import socket 
import sys
import threading
import time
from datetime import datetime

known_clients = {}
my_name = ""
my_port = 0
my_ip = ""
sock = None

def receive_loop():
    global known_clients, sock
    while True:
        data, addr = sock.recvfrom(4096)
        msg = data.decode().strip()
        print(f"\n[Empfangen] Von {addr}: {msg}", flush=True)

        if msg == "request_name":
            response = f"register {my_name} {my_ip} {my_port}"
            sock.sendto(response.encode(), addr)
            print(f"[System] Antwort an {addr}: {response}", flush=True)

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

                # Automatische Antwort auf bestimmte Fragen
                antwort = None
                frage = message.lower()

                if frage == "was ist deine ip-adresse?":
                    antwort = f"Meine IP-Adresse ist {my_ip}"
                elif frage == "wie viel uhr haben wir?":
                    jetzt = datetime.now().strftime("%H:%M:%S")
                    antwort = f"Aktuelle Systemzeit ist {jetzt}"
                elif frage == "welche rechnernetze ha war das?":
                    antwort = "4. HA, Aufgabe 4"

                if antwort:
                    # Antwort nur an den Fragesteller senden
                    if sender in known_clients:
                        ip, port = known_clients[sender]
                        send_msg = f"send {my_name} {antwort}"
                        sock.sendto(send_msg.encode(), (ip, port))
                        print(f"[Automatisch an {sender} gesendet]: {antwort}", flush=True)
                    else:
                        print(f"[Warnung] Fragesteller {sender} nicht bekannt, keine automatische Antwort gesendet.", flush=True)

            else:
                print(f"[Fehler] Falsches send-Format: {msg}", flush=True)

        else:
            print(f"[Unbekannt] Nachricht: {msg}", flush=True)

# (Restlicher Code bleibt gleich, send_loop und main unverändert)

def send_loop():
    global known_clients, sock
    while True:
        try:
            line = input("> ").strip()
        except EOFError:
            break

        if line.lower() == "exit":
            print("[System] Programm wird beendet.", flush=True)
            break

        elif line.lower() == "peers":
            if known_clients:
                print("[System] Registrierte Kontakte:", flush=True)
                for name, (ip, port) in known_clients.items():
                    print(f"  {name} -> {ip}:{port}", flush=True)
            else:
                print("[System] Keine Kontakte registriert.", flush=True)

        elif line.startswith("register "):
            parts = line.split()
            if len(parts) == 3:
                _, ip, port_str = parts
                try:
                    port = int(port_str)
                except ValueError:
                    print("[Fehler] Port muss eine Zahl sein.", flush=True)
                    continue

                try:
                    sock.sendto("request_name".encode(), (ip, port))
                    print(f"[Info] Anfrage zur Namensregistrierung an {ip}:{port} gesendet.", flush=True)
                except Exception as e:
                    print(f"[Fehler] Konnte Nachricht nicht senden: {e}", flush=True)

                time.sleep(0.5)

            elif len(parts) == 4:
                _, name, ip, port = parts
                try:
                    port = int(port)
                    known_clients[name] = (ip, port)
                    print(f"[System] {name} lokal registriert unter {ip}:{port}", flush=True)
                    sock.sendto(line.encode(), (ip, port))
                    print(f"[Info] Registrierungsanfrage an {name} ({ip}:{port}) gesendet.", flush=True)
                except:
                    print("[Fehler] Ungültiges Format.", flush=True)
            else:
                print("[Fehler] Bitte benutze: register <IP> <Port>", flush=True)

        elif line.startswith("send_all "):
            message = line[len("send_all "):].strip()
            if not known_clients:
                print("[Fehler] Keine bekannten Clients zum Senden.", flush=True)
                continue
            for name, (ip, port) in known_clients.items():
                send_msg = f"send {my_name} {message}"
                try:
                    sock.sendto(send_msg.encode(), (ip, port))
                except Exception as e:
                    print(f"[Fehler] Nachricht an {name} konnte nicht gesendet werden: {e}", flush=True)
            print("[Info] Nachricht an alle gesendet.", flush=True)

        elif line.startswith("send "):
            parts = line.split(" ", 2)
            if len(parts) == 3:
                _, name, message = parts
                if name not in known_clients:
                    print(f"[Fehler] Ziel '{name}' nicht bekannt. Bitte registriere zuerst.", flush=True)
                    continue
                ip, port = known_clients[name]
                send_msg = f"send {my_name} {message}"
                try:
                    sock.sendto(send_msg.encode(), (ip, port))
                    print(f"[Info] Nachricht an {name} gesendet.", flush=True)
                except Exception as e:
                    print(f"[Fehler] Nachricht konnte nicht gesendet werden: {e}", flush=True)
            else:
                print("[Fehler] Bitte benutze: send <Name> <Nachricht>", flush=True)

        else:
            print("[Info] Befehle: register <IP> <Port>, send <Name> <Nachricht>, send_all <Nachricht>, peers, exit", flush=True)

def main():
    global my_name, my_port, my_ip, sock
    if len(sys.argv) != 4:
        print("Verwendung: python chat_udp.py <Name> <Bind-IP> <Port>", flush=True)
        sys.exit(1)

    my_name = sys.argv[1]
    my_ip = sys.argv[2]
    my_port = int(sys.argv[3])

    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.bind((my_ip, my_port))

    print(f"[System] Starte {my_name} auf {my_ip}:{my_port} ...", flush=True)

    threading.Thread(target=receive_loop, daemon=True).start()
    send_loop()

if __name__ == "__main__":
    main()