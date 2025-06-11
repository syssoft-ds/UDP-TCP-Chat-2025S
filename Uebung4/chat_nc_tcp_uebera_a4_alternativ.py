import socket  
import sys
import threading
import datetime

clients = {}  # name -> (conn, addr)
clients_lock = threading.Lock()

# Funktion zum Senden an alle Clients außer dem Sender
def send_message_to_all(sender_name, message):
    with clients_lock:
        for name, (conn, _) in clients.items():
            if name != sender_name:  # Nicht an sich selbst senden
                try:
                    conn.sendall(f"[Nachricht von {sender_name} an alle]: {message}\n".encode())
                except Exception as e:
                    print(f"[Server] Fehler beim Senden an {name}: {e}", flush=True)

def server(port):  # TCP-Socket und Bindung an Port
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s_sock:
        s_sock.bind(('0.0.0.0', port))
        s_sock.listen()
        print(f"[Server] TCP-Server läuft auf Port {port} ...", flush=True)
        while True:
            conn, addr = s_sock.accept()
            t = threading.Thread(target=serve_client, args=(conn, addr), daemon=True)
            t.start()

# Empfangsschleife, verarbeitet Eingaben der Clients
def serve_client(conn, addr):
    name = None

    predefined_answers = {
        "Was ist deine MAC-Adresse?": "Meine MAC-Adresse ist geheim",
        "Sind Kartoffeln eine richtige Mahlzeit?": "Kartoffeln sind eine echte Mahlzeit!",
    }

    try:
        with conn:
            while True:
                data = conn.recv(1024).decode().strip()
                if not data:
                    print(f"[Server] Verbindung von {addr} geschlossen", flush=True)
                    break

                # Dynamische automatische Antworten
                if data == "Was ist deine IP-Adresse?":
                    antwort = f"Meine IP-Adresse ist {addr[0]}"
                    conn.sendall(f"[Automatische Antwort] {antwort}\n".encode())
                    continue

                elif data == "Wie viel Uhr haben wir?":
                    jetzt = datetime.datetime.now().strftime("%H:%M:%S")
                    antwort = f"Aktuelle Systemzeit ist {jetzt}"
                    conn.sendall(f"[Automatische Antwort] {antwort}\n".encode())
                    continue

                elif data == "Welche Rechnernetze HA war das?":
                    antwort = "4. HA, Aufgabe 4"
                    conn.sendall(f"[Automatische Antwort] {antwort}\n".encode())
                    continue

                # Statische vordefinierte Antworten
                if data in predefined_answers:
                    antwort = predefined_answers[data]
                    conn.sendall(f"[Automatische Antwort] {antwort}\n".encode())
                    continue

                # Registrierung
                if data.startswith("register "):
                    requested_name = data[len("register "):].strip()
                    if not requested_name:
                        conn.sendall("[Fehler] Kein Name angegeben.\n".encode())
                        continue

                    with clients_lock:
                        if requested_name in clients:
                            conn.sendall(f"[Fehler] Name '{requested_name}' bereits vergeben.\n".encode())
                            continue
                        name = requested_name
                        clients[name] = (conn, addr)
                    print(f"[Server] {name} registriert von {addr}", flush=True)
                    conn.sendall(f"[System] Willkommen {name}!\n".encode())

                # Nachricht an bestimmten Client senden
                elif data.startswith("send "):
                    if not name:
                        conn.sendall("[Fehler] Bitte zuerst registrieren (register <Name>).\n".encode())
                        continue

                    parts = data.split(" ", 2)
                    if len(parts) < 3:
                        conn.sendall("[Fehler] Falsches Format. Nutze: send <Name> <Nachricht>\n".encode())
                        continue

                    target_name = parts[1]
                    message = parts[2]

                    with clients_lock:
                        if target_name not in clients:
                            conn.sendall(f"[Fehler] Ziel '{target_name}' nicht bekannt.\n".encode())
                            continue
                        target_conn, _ = clients[target_name]

                    try:
                        target_conn.sendall(f"[Nachricht von {name}]: {message}\n".encode())
                        conn.sendall(f"[System] Nachricht an {target_name} gesendet.\n".encode())
                    except Exception as e:
                        conn.sendall(f"[Fehler] Nachricht konnte nicht gesendet werden: {e}\n".encode())

                # Nachricht an alle senden (neuer Befehl: broadcast)
                elif data.startswith("broadcast "):
                    if not name:
                        conn.sendall("[Fehler] Bitte zuerst registrieren (register <Name>).\n".encode())
                        continue

                    message = data[len("broadcast "):].strip()
                    send_message_to_all(name, message)
                    conn.sendall("[System] Nachricht an alle gesendet.\n".encode())

                    # Prüfen, ob Nachricht eine vordefinierte Frage ist — Antwort nur an Absender
                    if message in predefined_answers:
                        antwort = predefined_answers[message]
                        conn.sendall(f"[Automatische Antwort] {antwort}\n".encode())

                # Liste der registrierten Clients senden
                elif data == "list":
                    with clients_lock:
                        if clients:
                            client_names = ", ".join(clients.keys())
                            conn.sendall(f"[System] Bekannte Clients: {client_names}\n".encode())
                        else:
                            conn.sendall("[System] Keine Clients registriert.\n".encode())

                # Verbindung beenden
                elif data == "stop":
                    conn.sendall("[System] Verbindung wird geschlossen.\n".encode())
                    break

                else:
                    conn.sendall("[System] Unbekannter Befehl.\n".encode())

    except Exception as e:
        print(f"[Server] Fehler mit Verbindung {addr}: {e}", flush=True)

    finally:
        if name:
            with clients_lock:
                if name in clients:
                    del clients[name]
            print(f"[Server] {name} hat die Verbindung getrennt.", flush=True)

# Client-Funktion (unverändert)
def client(host, port):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as c_sock:
        c_sock.connect((host, port))

        def receive():
            while True:
                try:
                    msg = c_sock.recv(1024).decode()
                    if not msg:
                        print("\n[System] Verbindung vom Server beendet.")
                        break
                    print("\n" + msg.strip())
                    print("> ", end="", flush=True)
                except Exception:
                    break

        threading.Thread(target=receive, daemon=True).start()

        while True:
            try:
                line = input("> ").strip()
                if not line:
                    continue
                c_sock.sendall((line + "\n").encode())
                if line.lower() == "stop":
                    break
            except EOFError:
                break

# Hauptfunktion zum Starten von Server oder Client
def main():
    if len(sys.argv) != 3:
        print(f"Verwendung:\n  Server: {sys.argv[0]} -l <port>\n  Client: {sys.argv[0]} <host> <port>")
        sys.exit(1)

    if sys.argv[1] == "-l":
        port = int(sys.argv[2])
        server(port)
    else:
        host = sys.argv[1]
        port = int(sys.argv[2])
        client(host, port)

if __name__ == "__main__":
    main()