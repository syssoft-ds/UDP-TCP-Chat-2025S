import socket
import sys
import threading
clients = {}  # name -> (ip, port)

def receiveLines(port):
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s_sock:
        s_sock.bind(('0.0.0.0', port))
        print(f"[Server] Wartet auf Port {port}")
        while True:
            line, c_address = s_sock.recvfrom(4096)
            line = line.decode().rstrip()
            print(f"[Empfangen] {line} von {c_address}")

            if line.lower() == 'stop':
                print("[Server] Stop empfängt, beende.")
                break

            if line.startswith("register "):
                name = line.split()[1].lower()
                clients[name] = c_address
                print(f"[Server] Registriert: {name} → {c_address}")
                s_sock.sendto(f"[Server] Willkommen, {name}!".encode(), c_address)

            elif line.startswith("send "):
                parts = line.split(maxsplit=2)
                if len(parts) < 3:
                    s_sock.sendto("[Fehler] Ungültiger send-Befehl.".encode(), c_address)
                    continue

                target_name = parts[1].lower()
                message = parts[2]

                sender_name = None
                for name, addr in clients.items():
                    if addr == c_address:
                        sender_name = name
                        break

                if not sender_name:
                    s_sock.sendto("[Fehler] Bitte zuerst registrieren.".encode(), c_address)
                    continue

                if target_name in clients:
                    target_address = clients[target_name]
                    full_msg = f"{sender_name} sagt: {message}"
                    s_sock.sendto(full_msg.encode(), target_address)
                    print(clients[target_name])
                    print(f"[Weitergeleitet] {sender_name} → {target_name}: {message}")
                else:
                    s_sock.sendto(f"[Fehler] Ziel {target_name} nicht registriert.".encode(), c_address)

def listen_for_messages(sock):
    while True:
        try:
            msg, addr = sock.recvfrom(4096)
            print(f"\n[Nachricht von Server] {msg.decode()}\n> ", end="")
        except:
            break


def sendLines(host, port):
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as c_sock:
        # Start Empfangs-Thread
        listener = threading.Thread(target=listen_for_messages, args=(c_sock,), daemon=True)
        listener.start()

        print("[Client] Eingabe starten. Zum Beenden: 'stop'")
        for line in sys.stdin:
            line = line.rstrip()
            c_sock.sendto(line.encode(), (host, port))
            if line.lower() == 'stop':
                break

def main():
    if len(sys.argv) != 3:
        name = sys.argv[0]
        print(f"Usage: \"{name} -l <port>\" oder \"{name} <ip> <port>\"")
        sys.exit()
    port = int(sys.argv[2])
    if sys.argv[1].lower() == '-l':
        receiveLines(port)
    else:
        sendLines(sys.argv[1], port)

if __name__ == '__main__':
    main()
