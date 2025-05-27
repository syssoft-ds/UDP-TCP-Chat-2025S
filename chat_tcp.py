import socket
import sys
import threading

# Globale Struktur für den Server: registrierte Clients
# Format: { "Name": socket }
registered_clients = {}

# Funktion für den TCP-Server
def start_server(port):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as server_sock:
        server_sock.bind(('0.0.0.0', port))
        server_sock.listen()
        print(f"[Server] Listening on port {port}...")

        while True:
            client_sock, addr = server_sock.accept()
            print(f"[Server] Connection from {addr}")
            t = threading.Thread(target=handle_client, args=(client_sock,))
            t.start()

# Server: Behandlung eines einzelnen Clients
def handle_client(client_sock):
    name = None
    try:
        with client_sock:
            while True:
                data = client_sock.recv(1024).decode().strip()
                if not data:
                    break

                if data.lower().startswith("register "):
                    # Registrierung: register <Name>
                    name = data.split(" ", 1)[1]
                    registered_clients[name] = client_sock
                    print(f"[Server] Registered client: {name}")
                    client_sock.send(f"[System] Registered as {name}\n".encode())

                elif data.lower().startswith("send "):
                    # Nachricht versenden: send <Empfänger> <Nachricht>
                    parts = data.split(" ", 2)
                    if len(parts) < 3:
                        client_sock.send(b"[Error] Usage: send <recipient> <message>\n")
                        continue
                    recipient, message = parts[1], parts[2]

                    if recipient not in registered_clients:
                        client_sock.send(f"[Error] Unknown recipient: {recipient}\n".encode())
                    else:
                        target_sock = registered_clients[recipient]
                        target_sock.send(f"[{name}] {message}\n".encode())
                else:
                    client_sock.send(b"[Error] Unknown command.\n")
    except Exception as e:
        print(f"[Server] Error with client {name or 'Unknown'}: {e}")
    finally:
        if name in registered_clients:
            print(f"[Server] {name} disconnected.")
            del registered_clients[name]

# Funktion für den Client
def start_client(server_ip, server_port, name):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as client_sock:
        client_sock.connect((server_ip, server_port))
        print(f"[Client] Connected to server at {server_ip}:{server_port}")

        # Registrierung
        client_sock.send(f"register {name}\n".encode())

        # Thread zum Empfang von Nachrichten vom Server
        def receive_messages():
            while True:
                data = client_sock.recv(1024).decode()
                if not data:
                    break
                print(data.strip())

        threading.Thread(target=receive_messages, daemon=True).start()

        # Eingabe-Loop zum Senden von Nachrichten
        try:
            while True:
                line = sys.stdin.readline()
                if not line:
                    break
                line = line.strip()
                client_sock.send((line + "\n").encode())
                if line.lower() == "quit":
                    break
        except KeyboardInterrupt:
            pass

# Main-Funktion zum Auswerten der Kommandozeile
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
