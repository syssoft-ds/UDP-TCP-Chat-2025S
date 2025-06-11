import socket
import sys
import threading

known_clients = {}  # name -> (ip, port)
local_name = ""
local_port = 0

def receive(port):
    global known_clients
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as sock:
        sock.bind(("0.0.0.0", port))
        while True:
            data, addr = sock.recvfrom(4096)
            msg = data.decode().strip()

            if msg.startswith("REGISTER:"):
                # Format: REGISTER:<name>:<ip>:<port>
                try:
                    _, name, ip, port = msg.split(":")
                    known_clients[name] = (ip, int(port))
                    print(f"[System] Registrierung von '{name}' ({ip}:{port}) erhalten.")
                except:
                    print("[Fehler] Ungültige Registrierungsnachricht.")
            elif msg.startswith("MSG:"):
                    # Format: MSG:<name>:<message>
                    try:
                        _, sender, message = msg.split(":", 2)
                        print(f"[{sender}] {message}")

                        # Automatische Antworten auf bestimmte Fragen
                        antworten = {
                            "was ist deine mac-adresse?": "Meine MAC-Adresse ist 00:11:22:33:44:55",
                            "sind kartoffeln eine richtige mahlzeit?": "Natürlich sind Kartoffeln eine richtige Mahlzeit!",
                        }

                        lower_msg = message.lower()
                        if lower_msg in antworten:
                            reply = f"MSG:{local_name}:{antworten[lower_msg]}"
                            sock.sendto(reply.encode(), addr)

                    except:
                        print("[Fehler] Ungültige Nachrichtenstruktur.")
            elif msg.lower() == "stop":
                print("[System] Verbindung beendet.")
                break

def send():
    global known_clients, local_name, local_port
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as sock:
        for line in sys.stdin:
            line = line.strip()
            if line.startswith("register "):
                # Format: register <name> <ip> <port>
                try:
                    _, name, ip, port = line.split()
                    msg = f"REGISTER:{local_name}:{socket.gethostbyname(socket.gethostname())}:{local_port}"
                    sock.sendto(msg.encode(), (ip, int(port)))
                    known_clients[name] = (ip, int(port))
                    print(f"[System] Registrierung an '{name}' gesendet.")
                except:
                    print("[Fehler] Ungültiger Registrierbefehl.")
            elif line.startswith("sendall "):
                message = line[len("sendall "):]
                msg = f"MSG:{local_name}:{message}"
                for name, (ip, port) in known_clients.items():
                    sock.sendto(msg.encode(), (ip, port))
                print("[System] Nachricht an alle bekannten Clients gesendet.")
            elif line.startswith("send "):
                # Format: send <name> <message>
                try:
                    _, target_name, message = line.split(" ", 2)
                    if target_name in known_clients:
                        ip, port = known_clients[target_name]
                        msg = f"MSG:{local_name}:{message}"
                        sock.sendto(msg.encode(), (ip, port))
                    else:
                        print(f"[Fehler] Empfänger '{target_name}' nicht bekannt.")
                except:
                    print("[Fehler] Ungültiger Sendebefehl.")
            elif line.lower() == "stop":
                break
            else:
                print("[Hinweis] Unbekannter Befehl.")

def main():
    global local_name, local_port
    if len(sys.argv) != 4:
        print(f"Usage: {sys.argv[0]} <name> -l <port>")
        sys.exit(1)

    local_name = sys.argv[1]
    if sys.argv[2] != "-l":
        print("Fehler: Zweites Argument muss -l sein.")
        sys.exit(1)

    local_port = int(sys.argv[3])

    threading.Thread(target=receive, args=(local_port,), daemon=True).start()
    send()

if __name__ == "__main__":
    main()
