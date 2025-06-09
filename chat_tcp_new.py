### Erweiterte Version von chat_tcp.py mit "broadcast", "list", Autoresponses ###

import socket
import sys
import threading
from datetime import datetime

registered_clients = {}  # name -> socket
client_addresses = {}    # name -> (ip, port)

def start_server(port):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as server_sock:
        server_sock.bind(('0.0.0.0', port))
        server_sock.listen()
        print(f"[Server] Listening on port {port}...")

        while True:
            client_sock, addr = server_sock.accept()
            t = threading.Thread(target=handle_client, args=(client_sock, addr))
            t.start()

def handle_client(client_sock, addr):
    name = None
    try:
        with client_sock:
            while True:
                data = client_sock.recv(1024).decode().strip()
                if not data:
                    break

                if data.lower().startswith("register"):
                    ### Registrierung erweitern: Adresse speichern für Autoresponses ###
                    name = data.split(" ", 1)[1]
                    registered_clients[name] = client_sock
                    client_addresses[name] = addr
                    print(f"[Server] Registered client: {name}")
                    client_sock.send(f"[System] Registered as {name}\n".encode())

                elif data.lower().startswith("send"):
                    ### Erweiterung: Autoresponse bei vordefinierten Fragen ###
                    parts = data.split(" ", 2)
                    if len(parts) < 3:
                        client_sock.send(b"[Error] Usage: send <recipient> <message>\n")
                        continue
                    recipient, message = parts[1], parts[2]
                    if recipient not in registered_clients:
                        client_sock.send(f"[Error] Unknown recipient: {recipient}\n".encode())
                    else:
                        reply = handle_autoresponse(message, recipient)
                        target_sock = registered_clients[recipient]
                        target_sock.send(f"[{name}] {message}\n".encode())
                        if reply:
                            client_sock.send(f"[{recipient}] {reply}\n".encode())

                elif data.lower().startswith("broadcast"):
                    ### Neue Funktion: Nachricht an alle registrierten Clients senden ###
                    msg = data.partition(" ")[2]
                    for other_name, sock in registered_clients.items():
                        if other_name != name:
                            sock.send(f"[{name} -> all] {msg}\n".encode())

                elif data.lower() == "list":
                    ### Neue Funktion: Liste aller registrierten Clients zurücksenden ###
                    names = ", ".join(registered_clients.keys())
                    client_sock.send(f"[System] Registered clients: {names}\n".encode())

                elif data.lower() == "list fragen":
                    ### Neue Funktion: Liste der unterstützten Autoresponse-Fragen anzeigen ###
                    antworten = [
                        "- Was ist deine IP-Adresse?",
                        "- Wie viel Uhr haben wir?",
                        "- Welche Rechnernetze HA war das?"
                    ]
                    client_sock.send("[System] Unterstützte automatische Antworten:\n".encode())
                    client_sock.send(("\n".join(antworten) + "\n").encode())

                elif data.lower() == "stop":
                    break

                else:
                    client_sock.send(b"[Error] Unknown command.\n")

    except Exception as e:
        print(f"[Server] Error with client {name or 'Unknown'}: {e}")
    finally:
        if name in registered_clients:
            print(f"[Server] {name} disconnected.")
            del registered_clients[name]
            del client_addresses[name]

def handle_autoresponse(message, recipient):
    ### Neue Funktion: Reagiere automatisch auf bestimmte Fragen ###
    message = message.strip()
    if message.startswith("Was ist deine IP-Adresse?"):
        ip, _ = client_addresses.get(recipient, ("unknown", 0))
        return f"Meine IP-Adresse ist {ip}"
    elif message.startswith("Wie viel Uhr haben wir?"):
        return f"Es ist {datetime.now().strftime('%H:%M:%S')}"
    elif message.startswith("Welche Rechnernetze HA war das?"):
        return "4. HA, Aufgabe 4"
    return None

def start_client(server_ip, server_port, name):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as client_sock:
        client_sock.connect((server_ip, server_port))
        print(f"[Client] Connected to server at {server_ip}:{server_port}")

        client_sock.send(f"register {name}\n".encode())

        def receive_messages():
            while True:
                data = client_sock.recv(1024).decode()
                if not data:
                    break
                print(data.strip())

        threading.Thread(target=receive_messages, daemon=True).start()

        try:
            while True:
                line = sys.stdin.readline()
                if not line:
                    break
                line = line.strip()
                client_sock.send((line + "\n").encode())
                if line.lower() == "stop":
                    break
        except KeyboardInterrupt:
            pass

def main():
    if len(sys.argv) < 3:
        print("Usage for server: chat_tcp.py -l <port>")
        print("Usage for client: chat_tcp.py <server_ip> <port> <name>")
        sys.exit(1)

    if sys.argv[1] == "-l":
        port = int(sys.argv[2])
        start_server(port)
    else:
        if len(sys.argv) != 4:
            print("Usage: chat_tcp.py <server_ip> <port> <name>")
            sys.exit(1)
        server_ip = sys.argv[1]
        port = int(sys.argv[2])
        name = sys.argv[3]
        start_client(server_ip, port, name)

if __name__ == '__main__':
    main()
