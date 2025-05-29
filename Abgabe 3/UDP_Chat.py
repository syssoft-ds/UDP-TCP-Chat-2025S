import socket
import sys
import threading

#"Hallo, hier ist Marvin, meine IP-Adresse ist die 192.168.0.42 und du kannst mich unter Port-Nummer 31337 erreichen."
def receive_lines(socket):
    global contacts, is_adding_contact

    while True:
        try:
            line, c_address = socket.recvfrom(4096)
            line = line.decode().rstrip()
            print(f'\n\nMessage <{repr(line)}> received from client {c_address}')

            if c_address not in contacts.values() and line.lower().startswith("hallo"):
                with contact_added:  # Synchronisation starten
                    is_adding_contact = True
                    print("achricht von unbekanntem Kontakt: Enter zum bestätigen")
                    answer = input("Soll ein neuer Kontakt hinzugefügt werden? (ja/nein): ")

                    if answer.lower() == 'ja':
                        name = input("Unter welchem Namen soll der Kontakt gespeichert werden? ")
                        contacts[name] = c_address
                        print("Kontakt gespeichert!")
                    else:
                        print("Kontakt wurde nicht gespeichert!")

                    is_adding_contact = False
                    contact_added.notify_all()  # Main-Thread benachrichtigen

            return line, c_address
        except Exception as e:
            print(f"Fehler im Empfangsthread: {e}")
            break
    return None


def send_lines(socket, to_ip, to_port, message):
    message = message.rstrip()
    socket.sendto(message.encode(), (to_ip, to_port))


def check_command(line):
    global commands

    for command in commands:
        if line.lower().startswith(command):
            return command
    return None


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
    message = input("Nachricht: ")
    return to_ip, to_port, message


def receive_thread(socket):
    while True:
        try:
            message = receive_lines(socket)
            #print(message)
            if message[0].lower() == 'stop':
                print("Empfangs-Thread beendet.")
                break
        except Exception as e:
            print(f"Fehler im Empfangsthread: {e}")
            break


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


def main(port):
    global is_adding_contact
    host = get_local_ip()
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
        s.bind((host, port))

        # Starte den Empfangs-Thread
        recv_thread = threading.Thread(target=receive_thread, args=(s,), daemon=True)
        recv_thread.start()

        # Hauptthread für die Eingabe
        while True:

            # Warte auf Benutzereingabe, aber pausiere, wenn ein Kontakt hinzugefügt wird
            with contact_added:
                while is_adding_contact:
                    contact_added.wait()  # Blockiert, bis `notify_all()` aufgerufen wird

            # Normale Benutzereingabe verarbeiten
            user_input = input("Befehl eingeben ('send hello', 'send to', 'my address', 'my contacts', 'stop'): ").strip()
            #print("Befehl eingeben ('Send hello', 'Send to', 'stop'): ")

            if user_input.lower() == 'stop':
                return

            command = check_command(user_input)
            if command == "send hello":
                to_ip, to_port, message = hello_generator(host, port)
                send_lines(s, to_ip, to_port, message)

            elif command == "my address":
                print(f"Adresse: {host} : {port}")

            elif command == "my contacts":
                if len(contacts.keys()) == 0:
                    print("Du verfügst aktuell über keine Kontakte!")

                else:
                    for key in contacts.keys():
                        print(f"Cantact: {key} : {contacts[key]}")

            elif command == "send to":
                message_packet = message_generator()
                if message_packet is not None:
                    to_ip, to_port, message = message_packet
                else:
                    print("Du verfügst aktuell über keine Kontakte!")

                if to_ip is not None:  # Nur senden, wenn gültige Daten zurückgegeben wurden
                    send_lines(s, to_ip, to_port, message)


def start():
    global username
    if len(sys.argv) != 3:
        name = sys.argv[0]
        print(f"Usage: \"{name} -username <port>")
        sys.exit()

    port = int(sys.argv[2])
    username = sys.argv[1].removeprefix("-")
    print(username)
    main(port)


if __name__ == '__main__':
    contact_lock = threading.Lock()
    contact_added = threading.Condition(contact_lock)  # Signalisiert, wenn ein Kontakt hinzugefügt wurde
    is_adding_contact = False

    username = ""
    commands = ["send to", "send hello", "my address", "my contacts"]
    contacts = {}
    start()
