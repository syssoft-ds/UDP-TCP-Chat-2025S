import socket
import sys
import threading
import time

known_clients = {}
my_name = ""
my_port = 0

# Vordefinierte Fragen und automatische Antworten
auto_responses = {
    "Was ist deine MAC-Adresse?": "Meine MAC-Adresse ist geheim",
    "Sind Kartoffeln eine richtige Mahlzeit?": "Kartoffeln sind eine echte Mahlzeit!",
    "Wie spät ist es?": lambda: f"Aktuelle Uhrzeit: {time.strftime('%H:%M:%S')}",
    "Was ist die aktuelle Rechnernetze Hausaufgabe?": "HA4",
}

def receive_loop():
    global known_clients
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s_sock:
        s_sock.bind(('0.0.0.0', my_port))
        print(f"[System] {my_name} hört auf UDP-Port {my_port}", flush=True)
        while True:
            data, addr = s_sock.recvfrom(4096)
            msg = data.decode().strip()
            print(f"\n[Empfangen] Von {addr}: {msg}", flush=True)

            # Unterstützung für Java-Registrierungsmeldung
            if msg.startswith("Hello, this is "):
                try:
                    parts = msg.split(", ")
                    name = parts[1].split(" ")[2]
                    ip = parts[2].split(" ")[5]
                    port = int(parts[3].split(" ")[5])
                    if name != my_name:
                        known_clients[name] = (ip, port)
                        print(f"[System] {name} (Java-Client) registriert unter {ip}:{port}", flush=True)
                except Exception as e:
                    print(f"[Fehler] Konnte Java-Registrierung nicht verarbeiten: {e}", flush=True)

            elif msg.startswith("register "):
                parts = msg.split()
                if len(parts) == 4:
                    _, name, ip, port = parts
                    port = int(port)
                    if name != my_name:
                        known_clients[name] = (ip, port)
                        print(f"[System] {name} registriert unter {ip}:{port}", flush=True)
                else:
                    print(f"[Fehler] Falsches register-Format: {msg}", flush=True)

            elif msg.startswith("send "):
                parts = msg.split(" ", 2)
                if len(parts) == 3:
                    _, sender, message = parts
                    print(f"[Nachricht von {sender}]: {message}", flush=True)

                    # Automatisch neuen Client registrieren, falls unbekannt
                    if sender != my_name and sender not in known_clients:
                        known_clients[sender] = addr
                        print(f"[System] Neuer Client {sender} automatisch registriert mit {addr}", flush=True)

                    # Automatische Antwort bei vordefinierten Fragen
                    if message in auto_responses:
                        antwort = auto_responses[message]
                        if callable(antwort):
                            antwort = antwort()
                        ip_port = known_clients.get(sender)
                        if ip_port:
                            ip, port = ip_port
                            antwort_msg = f"send {my_name} {antwort}"
                            with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as reply_sock:
                                reply_sock.sendto(antwort_msg.encode(), (ip, port))
                            print(f"[System] Automatische Antwort an {sender} gesendet.", flush=True)
                        else:
                            print(f"[Warnung] Sender {sender} nicht in known_clients, keine automatische Antwort gesendet.", flush=True)
                else:
                    print(f"[Fehler] Falsches send-Format: {msg}", flush=True)

            else:
                # Nachricht von Java oder sonstiger freier Text
                print(f"[Nachricht] {msg}", flush=True)

def send_loop():
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

                    # Nachricht an Java-Client senden
                    # Hinweis: Java erwartet freien Text, nicht 'register ...'
                    message = f"Hello, this is {my_name}, my IPv4 address is {socket.gethostbyname(socket.gethostname())}, my port number is {my_port}, and I am thrilled to talk to you."
                    s_sock.sendto(message.encode(), (ip, port))
                    print(f"[Info] Java-kompatible Registrierungsnachricht an {name} ({ip}:{port}) gesendet.", flush=True)

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

            elif line.startswith("sendall "):
                message = line[len("sendall "):].strip()
                if not known_clients:
                    print("[Fehler] Keine bekannten Clients vorhanden.", flush=True)
                    continue

                send_msg = f"send {my_name} {message}"
                for name, (ip, port) in known_clients.items():
                    s_sock.sendto(send_msg.encode(), (ip, port))
                print("[Info] Nachricht an alle bekannten Clients gesendet.", flush=True)

            else:
                print("[Info] Befehle: register <Name> <IP> <Port>, send <Name> <Nachricht>, sendall <Nachricht>, list, stop", flush=True)

def main():
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