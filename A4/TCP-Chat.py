import socket
import threading
import sys
import datetime
import socket

clients = {}  # name -> socket
lock = threading.Lock()

# sendet Nachricht vom Server an alle Clients
def send_all_server(message):
    full_message = f"[Server]: {message}\n"
    print(full_message.strip())  # Ausgabe auf dem Server
    with lock:
        for name, sock in list(clients.items()):
            try:
                sock.sendall(full_message.encode())
            except:
                print(f"[Server] Failed to send message to {name}")

# sendet alle registrierten Clients
def broadcast_client_list():
    with lock:
        if not clients:
            message = "[Server]: No clients connected."
        else:
            names = ", ".join(clients.keys())
            message = f"[Server]: Connected users: {names}"
    send_all_server(message)

# ermitteln der eigenen IP-Adresse
def get_own_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        # Zieladresse ist irrelevant, nur um Interface zu bestimmen
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
    except Exception:
        ip = "IP unknown"
    finally:
        s.close()
    return ip

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
                        # Prüfe auf vordefinierte Frage und automatisiere Antwort
                        response = None
                        lower_msg = message.lower()
                        if lower_msg == "was ist deine ip-adresse?":
                            response = get_own_ip()
                        elif lower_msg == "wie viel uhr haben wir?":
                            response = datetime.datetime.now().strftime("%H:%M:%S")
                        elif lower_msg == "welche rechnernetze ha war das?":
                            response = "4. HA, Aufgabe 4."

                        if response is not None:
                        # automatische Antwort an den Sender
                            try:
                                reply = f"[Auto-Reply] {target_name} antwortet: {response}\n"
                                c_sock.sendall(reply.encode())
                            except:
                                print(f"[Server] Fehler beim Senden der automatischen Antwort an {name}")

                            # zustellen der Frage an Zielclient
                            try:
                                target_sock.sendall(f"{name} (fragt): {message}\n".encode())
                            except:
                                print(f"[Server] Fehler beim Senden der Frage an {target_name}")
                    
                        # Fall einer normalen Nachricht
                        else:
                            full_message = f"{name}: {message}"
                            print(f"[Server] {name} -> {target_name}: {message}")  # Neue Zeile
                            try:
                                target_sock.sendall(f"{full_message}\n".encode())
                            except:
                                c_sock.sendall(f"Could not deliver message to {target_name}\n".encode())
                    else:
                        print(f"[Server] Failed delivery: {name} -> {target_name} (not found)")  # Neue Zeile
                        c_sock.sendall(f"User {target_name} not found.\n".encode())
                        
                # Prüfung auf Anfrage eines Clients, eine Servernachricht zu senden
                elif data.startswith("broadcast "):
                    message = data[len("broadcast "):]
                    full_message = f"{name} (to all): {message}"
                    print(f"[Server] {name} broadcasted: {message}")
                    with lock:
                        for other_name, sock in clients.items():
                            if other_name != name:  # nicht an sich selbst
                                try:
                                    sock.sendall(f"{full_message}\n".encode())
                                except:
                                    print(f"[Server] Failed to deliver broadcast to {other_name}")
                    c_sock.sendall(b"[Server] Broadcast sent.\n")
                
                # Prüfung auf Anfrage eines Clients für die Liste aller registrierten Clients
                elif data.lower() == "list":
                    with lock:
                        if not clients:
                             message = "[Server]: No clients connected."
                        else:
                             names = ", ".join(clients.keys())
                             message = f"[Server]: Connected users: {names}"
                    c_sock.sendall((message + "\n").encode())
                
                else:
                    c_sock.sendall(b"Unknown command. Use: send <name> <message>, broadcast <message>, list or stop\n")
        finally:
            with lock:
                for key, val in list(clients.items()):
                    if val == c_sock:
                        del clients[key]
                        print(f"[Server] Removed client '{key}' from registry")  # Neue Zeile
                        break

# startet TCP-Server, öffnet einen Socket und wartet auf eingehende Verbindungen
def server(port):

    # Ergänzung um als Server Nachrichten an alle zu versenden
    def server_input_loop():
        while True:
            msg = input()
            if msg.strip().lower() == "list":
                broadcast_client_list()
            elif msg.strip().lower() == "stop":
                print("[Server] Shutdown not implemented – stop manually.")
            elif msg:
                send_all_server(msg)

    threading.Thread(target=server_input_loop, daemon=True).start()

    # Ausgabe des Servers bei neuer Registierung
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
