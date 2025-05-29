import socket
import sys
import threading

# Globale Registry auf Server-Seite
registry = {}  # name -> (socket, address)

def server(port):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s_sock:
        s_sock.bind(('0.0.0.0', port))
        s_sock.listen()
        print(f"[INFO] Server listening on port {port}")
        while True:
            c_sock, c_address = s_sock.accept()
            t = threading.Thread(target=serveClient, args=(c_sock, c_address))
            t.start()

def serveClient(c_sock, c_address):
    """
    Funktion zur Behandlung eines einzelnen Clients
    """
    with c_sock:
        name = None
        while True:
            try:
                data = c_sock.recv(4096)
                if not data:
                    break  # Verbindung geschlossen
                line = data.decode().rstrip()
                print(f"[DEBUG] Received from {c_address}: {line}")

                # Registrierung
                if line.startswith("REGISTER "):
                    parts = line.split(maxsplit=1)
                    if len(parts) == 2:
                        name = parts[1]
                        registry[name] = (c_sock, c_address)
                        c_sock.sendall(f"[INFO] Registered as {name}\n".encode())
                        print(f"[INFO] Registered client {name} at {c_address}")
                    else:
                        c_sock.sendall(b"[ERROR] Invalid REGISTER command\n")

                # Sende Nachricht an anderen Client
                elif line.startswith("SEND "):
                    # Erwartet: SEND <name> <message>
                    parts = line.split(maxsplit=2)
                    if len(parts) < 3:
                        c_sock.sendall(b"[ERROR] Invalid SEND command\n")
                        continue
                    dest_name = parts[1]
                    message = parts[2]
                    if dest_name in registry:
                        dest_sock, _ = registry[dest_name]
                        try:
                            dest_sock.sendall(f"MSG from {name}: {message}\n".encode())
                            c_sock.sendall(f"[SENT] To {dest_name}: {message}\n".encode())
                        except Exception as e:
                            c_sock.sendall(f"[ERROR] Could not send to {dest_name}: {e}\n".encode())
                    else:
                        c_sock.sendall(f"[ERROR] Unknown name: {dest_name}\n".encode())

                # Stoppen
                elif line.lower() == "stop":
                    c_sock.sendall(b"[INFO] Goodbye\n")
                    break

                else:
                    c_sock.sendall(b"[ERROR] Unknown command\n")
            except ConnectionResetError:
                print(f"[WARN] Connection reset by {c_address}")
                break

        # Bei Verbindungsende entfernen wir den Client aus Registry
        if name and name in registry:
            del registry[name]
            print(f"[INFO] Client {name} disconnected and deregistered")

def client(host, port, my_name):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as c_sock:
        c_sock.connect((host, port))
        print(f"[INFO] Connected to server {host}:{port}")

        # Registrierung beim Server
        c_sock.sendall(f"REGISTER {my_name}\n".encode())

        # Thread für Empfang von Nachrichten vom Server
        def listen_server():
            while True:
                try:
                    data = c_sock.recv(4096)
                    if not data:
                        print("[INFO] Server closed connection")
                        break
                    print(data.decode(), end='')  # Ausgeben ohne doppeltes newline
                except Exception:
                    break

        listener = threading.Thread(target=listen_server, daemon=True)
        listener.start()

        # Hauptschleife: Eingabe vom User und senden an Server
        for line in sys.stdin:
            line = line.rstrip()
            # Umwandlung von "send name message" zu "SEND name message"
            if line.startswith("send "):
                parts = line.split(maxsplit=2)
                if len(parts) < 3:
                    print("[ERROR] Usage: send <name> <message>")
                    continue
                # Umwandeln und senden
                send_line = f"SEND {parts[1]} {parts[2]}"
            else:
                send_line = line

            c_sock.sendall((send_line + "\n").encode())
            if line.lower() == "stop":
                break

def main():
    if len(sys.argv) < 3:
        name = sys.argv[0]
        print(f"Usage server: {name} -l <port>")
        print(f"Usage client: {name} <host> <port> <name>")
        sys.exit()

    if sys.argv[1].lower() == "-l":
        port = int(sys.argv[2])
        server(port)
    else:
        if len(sys.argv) != 4:
            print(f"Usage client: {sys.argv[0]} <host> <port> <name>")
            sys.exit()
        host = sys.argv[1]
        port = int(sys.argv[2])
        my_name = sys.argv[3]
        client(host, port, my_name)

if __name__ == "__main__":
    main()
