import socket
import sys
import threading

clients = {}  # name -> (conn, addr)
clients_lock = threading.Lock()

def server(port):#Erstellt TCP-Socket und bindet ihn an port
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s_sock:
        s_sock.bind(('0.0.0.0', port))
        s_sock.listen()
        print(f"[Server] TCP-Server läuft auf Port {port} ...", flush=True)
        while True:
            conn, addr = s_sock.accept()
            t = threading.Thread(target=serve_client, args=(conn, addr), daemon=True)
            t.start()
#Empfangsschleife: verarbeitet Eingaben des Clients; register <Name>: Speichert den Namen und die Verbindung in clients, wenn nicht vergeben
#send <Name> <Nachricht>: Prüft, ob Sender registriert ist; Sucht das Ziel in clients; Leitet die Nachricht an das Ziel weiter; Bestätigt dem Absender
#stop Schließt Verbindung und entfernt Client aus der clients-Liste
def serve_client(conn, addr):
    name = None
    try:
        with conn:
            while True:
                data = conn.recv(1024).decode().strip()
                if not data:
                    print(f"[Server] Verbindung von {addr} geschlossen", flush=True)
                    break

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
#Erstellt TCP-Socket und verbindet sich mit dem Server
#Startet Thread zum Empfangen von Nachrichten
#Empfangene Nachrichten werden nicht-blockierend angezeigt, sodass die Eingabezeile (> ) immer sichtbar bleibt
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
                    # Damit eingabe nicht unterbrochen wird, Ausgabe mit Zeilenumbruch und prompt wieder anzeigen
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
#Startet Server; Startet Client
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