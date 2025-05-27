import socket
import sys
import re
import threading


def get_local_ip():
    try:
        # Erstellt ein temporäres Socket-Objekt, um die richtige IP zu ermitteln
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))  # Google DNS
        local_ip = s.getsockname()[0]
        s.close()
        return local_ip
    except Exception as e:
        print(f"Fehler beim Ermitteln der IP: {e}")
        return "127.0.0.1"  # Fallback auf localhost


def hello_generator(my_ip, my_port):
    global username
    to_ip, to_port = input("Wen willst du anschreiben? (Form: IP-Adresse Port)\n").split()
    return to_ip, int(to_port), f"Hallo, hier ist {username}, meine IP-Adresse ist die {my_ip} und du kannst mich unter Port-Nummer {my_port} erreichen."


def message_generator():
    names = [name for name in contacts.keys()]
    print(contacts)

    if len(names) == 0:
        return None

    while True:
        receiver = input(f"An wen willst du eine Nachricht schreiben? ({names})\n")
        if receiver in names:
            break

    to_ip, to_port = contacts[receiver][0], contacts[receiver][1]
    print(to_ip, to_port)
    message = input("Nachricht: ")
    return to_ip, to_port, message


def check_command(line):
    global commands
    for command in commands:
        if line.lower() == command:
            return command
    return None


def receive_thread(my_server_sock):
    my_server_sock.listen()
    while True:
        try:
            client_sock, c_address = my_server_sock.accept()

            t = threading.Thread(target=serve_client, args=(client_sock, c_address))
            t.start()
        except Exception as e:
            print(f"Fehler im Empfangsthread: {e}")
            break


def server(port):
    global is_adding_contact
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as my_server_sock:
        host = get_local_ip()
        my_server_sock.bind((host,port))


        recv_thread = threading.Thread(target=receive_thread, args=(my_server_sock,), daemon=True)
        recv_thread.start()
        while True:

            with contact_added:
                while is_adding_contact:
                    contact_added.wait()  # Blockiert, bis `notify_all()` aufgerufen wird

            # Normale Benutzereingabe verarbeiten
            user_input = input("Befehl eingeben ('send hello', 'send to', 'my address', 'my contacts', 'stop'): ").strip()
            # print("Befehl eingeben ('Send hello', 'Send to', 'stop'): ")
            print(user_input)
            if user_input.lower() == 'stop':
                return

            command = check_command(user_input)
            if command == "send hello":
                to_ip, to_port, message = hello_generator(host, port)
                send_lines(to_ip, to_port, message)

            elif command == "my address":
                print(f"Adresse: {host} : {port}")

            elif command == "my contacts":
                for key in contacts.keys():
                    print(f"Cantact: {key} : {contacts[key]}")

            elif command == "send to":
                message_packet = message_generator()
                if message_packet is not None:
                    to_ip, to_port, message = message_packet
                else:
                    print("Du verfügst aktuell über keine Kontakte!")

                if to_ip is not None:  # Nur senden, wenn gültige Daten zurückgegeben wurden
                    send_lines(to_ip, to_port, message)


def serve_client(c_sock, c_address):
    global contacts, is_adding_contact
    with c_sock:
        while True:
            line = c_sock.recv(1024).decode().rstrip()

            if not line.lower().startswith("$"):
                return

            line = line[1:]
            print(f'Message <{repr(line)}> received from client {c_address}')

            if line.lower() == 'stop':
                break

            match = re.match(pattern, line)
            if c_address not in contacts.values() and match:
                with contact_added:  # Synchronisation starten
                    is_adding_contact = True
                    print("Anfrage von unbekanntem Kontakt: Enter zum bestätigen")
                    answer = input("Soll ein neuer Kontakt hinzugefügt werden? (ja/nein): ")

                    if answer.lower() == 'ja':
                        name = input("Unter welchem Namen soll der Kontakt gespeichert werden? ")
                        contacts[name] = (c_address[0], int(match.group("port"))) #notwendig, da sonst der Port von send-client gespeichert wird
                        print("Kontakt gespeichert!")
                    else:
                        print("Kontakt wurde nicht gespeichert!")

                    is_adding_contact = False
                    contact_added.notify_all()


def send_lines(to_ip, to_port, message):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as c_sock:
        c_sock.connect((to_ip,to_port))

        message = "$" + message.rstrip()
        c_sock.send(message.encode())


def start():
    global username
    if len(sys.argv) != 3:
        name = sys.argv[0]
        print(len(sys.argv), sys.argv)
        print(f"Usage: \"{name} -username <port>\"")
        sys.exit()

    port = int(sys.argv[2])
    username = sys.argv[1].removeprefix("-")

    server(port)


if __name__ == '__main__':
    contact_lock = threading.Lock()
    contact_added = threading.Condition(contact_lock)  # Signalisiert, wenn ein Kontakt hinzugefügt wurde
    is_adding_contact = False
    pattern = r"^Hallo, hier ist (?P<username>[^,]+), meine IP-Adresse ist die (?P<ip>[\d\.]+) und du kannst mich unter Port-Nummer (?P<port>\d+) erreichen\.$"

    username = ""
    commands = ["send to", "send hello", "my address", "my contacts"]
    contacts = {}
    start()
