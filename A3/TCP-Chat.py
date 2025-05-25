import socket
import threading
import sys

clients = {}  # name -> socket
lock = threading.Lock()

# verarbeitet Kommunikation mit verbundem Client, wie Registrierung, hinzufügen und entfernen von Clients
def handle_client(c_sock, c_address):
    with c_sock:
        try:
            # Registrierung
            name = c_sock.recv(1024).decode().strip()
            if not name:
                return
            with lock:
                if name in clients:
                    c_sock.sendall(f"Name '{name}' already taken.\n".encode())
                    return
                clients[name] = c_sock
            print(f"[Server] {name} registered from {c_address}")  # Neue Zeile
            c_sock.sendall(f"Welcome {name}! You can now send messages using 'send <name> <message>'\n".encode())

            # Kommunikation
            while True:
                data = c_sock.recv(1024).decode().strip()
                if not data:
                    break
                if data.lower() == "stop":
                    print(f"[Server] {name} disconnected.")  # Neue Zeile
                    break
                if data.startswith("send "):
                    parts = data.split(" ", 2)
                    if len(parts) < 3:
                        c_sock.sendall(b"Invalid format. Use: send <name> <message>\n")
                        continue
                    target_name, message = parts[1], parts[2]
                    with lock:
                        target_sock = clients.get(target_name)
                    if target_sock:
                        full_message = f"{name}: {message}"
                        print(f"[Server] {name} -> {target_name}: {message}")  # Neue Zeile
                        try:
                            target_sock.sendall(f"{full_message}\n".encode())
                        except:
                            c_sock.sendall(f"Could not deliver message to {target_name}\n".encode())
                    else:
                        print(f"[Server] Failed delivery: {name} -> {target_name} (not found)")  # Neue Zeile
                        c_sock.sendall(f"User {target_name} not found.\n".encode())
                else:
                    c_sock.sendall(b"Unknown command. Use: send <name> <message>\n")
        finally:
            with lock:
                for key, val in list(clients.items()):
                    if val == c_sock:
                        del clients[key]
                        print(f"[Server] Removed client '{key}' from registry")  # Neue Zeile
                        break

# startet TCP-Server, öffnet einen Socket und wartet auf eingehende Verbindungen
def server(port):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s_sock:
        s_sock.bind(('0.0.0.0', port))
        s_sock.listen()
        print(f"[Server] Listening on port {port}")
        while True:
            c_sock, c_address = s_sock.accept()
            threading.Thread(target=handle_client, args=(c_sock, c_address), daemon=True).start()

# verbindet sich als Client mit dem Server und startet einen neuen Thread
def client(host, port):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as c_sock:
        c_sock.connect((host, port))
        name = input("Enter your name for registration: ").strip()
        c_sock.sendall(name.encode())
        response = c_sock.recv(1024).decode()
        print(response)
        if "already taken" in response:
            return

        def receive():
            while True:
                data = c_sock.recv(1024)
                if not data:
                    print("Disconnected from server.")
                    break
                print(data.decode(), end='')

        threading.Thread(target=receive, daemon=True).start()

        for line in sys.stdin:
            line = line.strip()
            if not line:
                continue
            c_sock.sendall(line.encode())
            if line.lower() == 'stop':
                break

# Main-Methode mit Servermodus und Clientmodus
def main():
    if len(sys.argv) != 3:
        print(f"Usage: \"{sys.argv[0]} -l <port>\" or \"{sys.argv[0]} <ip> <port>\"")
        sys.exit(1)

    port = int(sys.argv[2])
    if sys.argv[1] == "-l":
        server(port)
    else:
        client(sys.argv[1], port)

if __name__ == "__main__":
    main()
