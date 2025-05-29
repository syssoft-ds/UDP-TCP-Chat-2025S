# Nicht vollständig. Die Rücknachricht funktioniert nicht richtig.

import socket
import sys
import threading

# Datenstruktur zur Namensauflösung: name -> (ip, port)
registry = {}

# Jeder Client muss beim Start seinen Namen angeben
my_name = None

def receiveMessages(my_port):
    """Empfängt Nachrichten und verarbeitet Registrierungen oder normale Chatnachrichten"""
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s_sock:
        s_sock.bind(('0.0.0.0', my_port))
        print(f"[INFO] Listening on port {my_port} und IP: {socket.gethostbyname(socket.gethostname())}")

        while True:
            data, addr = s_sock.recvfrom(4096)
            message = data.decode().strip()
            print(f"[DEBUG] Received: {repr(message)} from {addr}")

            # REGISTER Nachricht
            if message.startswith("REGISTER "):
                parts = message.split()
                if len(parts) == 4:
                    name = parts[1]
                    ip = parts[2]
                    port = int(parts[3])
                    registry[name] = (ip, port)
                    print(f"[INFO] Registered {name} at {ip}:{port}")

                    # eigene IP holen und zurückschicken
                    my_ip = socket.gethostbyname(socket.gethostname())
                    reply = f"REGISTER_REPLY {my_name} {my_ip} {my_port}"
                    s_sock.sendto(reply.encode(), addr)
                else:
                    print("[WARN] Malformed REGISTER message.")

            # REGISTER_REPLY Nachricht
            elif message.startswith("REGISTER_REPLY "):
                parts = message.split()
                if len(parts) == 4:
                    name = parts[1]
                    ip = parts[2]
                    port = int(parts[3])
                    registry[name] = (ip, port)
                    print(f"[INFO] Got REGISTER_REPLY from {name} at {ip}:{port}")
                else:
                    print("[WARN] Malformed REGISTER_REPLY message.")

            # Normale Chatnachricht
            elif message.startswith("MSG "):
                # Extrahiere Name und Nachricht
                msg_content = message[4:]
                if ": " in msg_content:
                    sender_name, msg_text = msg_content.split(": ", 1)
                    print(f"[MESSAGE] {sender_name}: {msg_text}")

                    # Wenn der Name noch nicht im Registry ist, eintragen
                    if sender_name not in registry:
                        ip, port = addr
                        registry[sender_name] = (ip, port)
                        print(f"[INFO] Learned sender {sender_name} is at {ip}:{port}")
                else:
                    print("[WARN] Malformed MSG message")

            elif message.lower() == "stop":
                print("[INFO] Stop received. Exiting receiver.")
                break

            else:
                print(f"[INFO] Unknown command: {message}")


def sendRegistration(target_ip, target_port, my_ip, my_port):
    """Sendet eine Registrierungsnachricht an eine andere Instanz"""
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as sock:
        reg_msg = f"REGISTER {my_name} {my_ip} {my_port}"
        sock.sendto(reg_msg.encode(), (target_ip, target_port))
        print(f"[INFO] Sent registration to {target_ip}:{target_port}")


def sendMessages():
    """Liest Eingaben vom Benutzer und sendet Nachrichten an registrierte Instanzen"""
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as sock:
        for line in sys.stdin:
            line = line.strip()
            if line.lower() == "stop":
                break

            # Nachricht im Format: send <name> <message>
            if line.startswith("send "):
                parts = line.split(maxsplit=2)
                if len(parts) < 3:
                    print("[ERROR] Usage: send <name> <message>")
                    continue
                name, msg = parts[1], parts[2]
                if name not in registry:
                    print(f"[ERROR] Unknown name: {name}")
                    continue
                target = registry[name]
                full_msg = f"MSG {my_name}: {msg}"
                sock.sendto(full_msg.encode(), target)
                print(f"[SENT] To {name}: {msg}")
            else:
                print("[INFO] Unknown command.")

def main():
    global my_name

    if len(sys.argv) < 3:
        print(f"Usage: {sys.argv[0]} <your_name> <your_port> [<target_ip> <target_port>]")
        sys.exit(1)

    my_name = sys.argv[1]
    my_port = int(sys.argv[2])

    # Start Empfangsthread
    receiver_thread = threading.Thread(target=receiveMessages, args=(my_port,), daemon=True)
    receiver_thread.start()

    # Optional: Registrierung bei einer anderen Instanz
    if len(sys.argv) == 5:
        target_ip = sys.argv[3]
        target_port = int(sys.argv[4])
        # Lokale IP-Adresse ermitteln
        hostname = socket.gethostname()
        my_ip = socket.gethostbyname(hostname)
        sendRegistration(target_ip, target_port, my_ip, my_port)

    # Nachrichten versenden
    sendMessages()

if __name__ == "__main__":
    main()
