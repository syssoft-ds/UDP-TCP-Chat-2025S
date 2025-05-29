import socket
import sys
import threading
import time

known_clients = {}  # Speicherung bekannter Clients
my_name = ""#Lokaler Benutzername und Port
my_port = 0
#Erstellt einen UDP-Socket
#register <Name> <IP> <Port>: Wird empfangen, wenn sich jemand registrieren will
#send <Name> <Nachricht>: Eine Textnachricht von einem bekannten Client
def receive_loop():
    global known_clients
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s_sock:
        s_sock.bind(('0.0.0.0', my_port))
        print(f"[System] {my_name} hört auf UDP-Port {my_port}", flush=True)
        while True:
            data, addr = s_sock.recvfrom(4096)
            msg = data.decode().strip()
            print(f"\n[Empfangen] Von {addr}: {msg}", flush=True)

            if msg.startswith("register "):
                parts = msg.split()
                if len(parts) == 4:
                    _, name, ip, port = parts
                    port = int(port)

                    if name != my_name:  # nicht sich selbst speichern
                        known_clients[name] = (ip, port)
                        print(f"[System] {name} registriert unter {ip}:{port}", flush=True)
                else:
                    print(f"[Fehler] Falsches register-Format: {msg}", flush=True)

            elif msg.startswith("send "):
                parts = msg.split(" ", 2)
                if len(parts) == 3:
                    _, sender, message = parts
                    print(f"[Nachricht von {sender}]: {message}", flush=True)
                else:
                    print(f"[Fehler] Falsches send-Format: {msg}", flush=True)

            else:
                print(f"[Unbekannt] Nachricht: {msg}", flush=True)

def send_loop():#UDP-Socket zum Senden; stop: Beendet das Programm; list: Zeigt bekannte Clients (known_clients)
    global known_clients
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s_sock:
        while True:
            try:
                line = input("> ").strip()
            except EOFError:
                break

            if line.lower() == "stop":
                print("[System] Programm wird beendet.", flush=True)
                break

            elif line.lower() == "list":
                if known_clients:
                    print("[System] Registrierte Kontakte:", flush=True)
                    for name, (ip, port) in known_clients.items():
                        print(f"  {name} -> {ip}:{port}", flush=True)
                else:
                    print("[System] Keine Kontakte registriert.", flush=True)

            elif line.startswith("register "):
                parts = line.split()
                if len(parts) == 4:
                    _, name, ip, port = parts
                    try:
                        port = int(port)
                    except ValueError:
                        print("[Fehler] Port muss eine Zahl sein.", flush=True)
                        continue

                    if name == my_name:
                        print("[Fehler] Du kannst dich nicht selbst registrieren.", flush=True)
                        continue

                    # Lokal speichern
                    known_clients[name] = (ip, port)
                    print(f"[System] {name} lokal registriert unter {ip}:{port}", flush=True)

                    # Nachricht an den Partner senden
                    s_sock.sendto(line.encode(), (ip, port))
                    print(f"[Info] Registrierungsanfrage an {name} ({ip}:{port}) gesendet.", flush=True)

                    time.sleep(0.5)
                else:
                    print("[Fehler] Bitte benutze: register <Name> <IP> <Port>", flush=True)

            elif line.startswith("send "):
                parts = line.split(" ", 2)
                if len(parts) == 3:
                    _, name, message = parts
                    if name not in known_clients:
                        print(f"[Fehler] Ziel '{name}' nicht bekannt. Bitte registriere zuerst.", flush=True)
                        continue
                    ip, port = known_clients[name]
                    send_msg = f"send {my_name} {message}"
                    s_sock.sendto(send_msg.encode(), (ip, port))
                    print(f"[Info] Nachricht an {name} gesendet.", flush=True)
                else:
                    print("[Fehler] Bitte benutze: send <Name> <Nachricht>", flush=True)

            else:
                print("[Info] Befehle: register <Name> <IP> <Port>, send <Name> <Nachricht>, list, stop", flush=True)

def main():#Erwartet 3 Argumente; Speichert Name und Port; Startet receive_loop() in einem Thread; startet send_loop() im Hauptthread
    global my_name, my_port
    if len(sys.argv) != 4 or sys.argv[2] != "-l":
        print("Verwendung: python chat_nc_udp.py <Name> -l <Port>", flush=True)
        sys.exit(1)

    my_name = sys.argv[1]
    my_port = int(sys.argv[3])

    print(f"[System] Starte {my_name} auf Port {my_port} ...", flush=True)

    threading.Thread(target=receive_loop, daemon=True).start()

    send_loop()

if __name__ == "__main__":
    main()