import socket
import sys
import threading
from datetime import datetime

name = None
port = None
known_clients = {}  ### name -> (ip, port)

# Vordefinierte Fragen und Antworten
predefined_faq = {
    "Was ist deine IP-Adresse?": "Meine IP ist <wird dynamisch gesendet>",
    "Wie viel Uhr haben wir?": "Es ist <wird dynamisch gesendet>",
    "Welche Rechnernetze HA war das?": "4. HA, Aufgabe 4"
}

def receive_loop():
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s_sock:
        s_sock.bind(('0.0.0.0', port))
        while True:
            data, addr = s_sock.recvfrom(4096)
            msg = data.decode().strip()
            parts = msg.split(" ", 2)

            if parts[0] == "REGISTER" and len(parts) == 3:
                client_name = parts[1]
                client_port = int(parts[2])
                known_clients[client_name] = (addr[0], client_port)
                print(f"[System] {client_name} registered from {addr[0]}:{client_port}")

            elif parts[0] == "MSG" and len(parts) == 3:
                sender = parts[1]
                message = parts[2]
                print(f"[{sender}] {message}")

            elif parts[0] == "PEERS":
                ### Empfangene Liste der bekannten Clients anzeigen
                peer_list = parts[1] if len(parts) > 1 else ""
                print("[System] Bekannte Clients:")
                for entry in peer_list.split(","):
                    if entry:
                        print(f"  {entry}")

            ### Erweiterte automatische Antworten auf vordefinierte Fragen
            elif msg == "Was ist deine IP-Adresse?":
                s_sock.sendto(f"MSG {name} Meine IP ist {addr[0]}".encode(), addr)

            elif msg == "Wie viel Uhr haben wir?":
                now = datetime.now().strftime("%H:%M:%S")
                s_sock.sendto(f"MSG {name} Es ist {now}".encode(), addr)

            elif msg == "Welche Rechnernetze HA war das?":
                s_sock.sendto(f"MSG {name} 4. HA, Aufgabe 4".encode(), addr)

            elif msg == "peers":
                ### Auf Anfrage Liste aller registrierten Clients senden
                peers_list = ",".join([f"{n}:{ip}:{p}" for n, (ip, p) in known_clients.items()])
                s_sock.sendto(f"PEERS {peers_list}".encode(), addr)

def send_loop():
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as c_sock:
        for line in sys.stdin:
            line = line.strip()

            if line.startswith("register"):
                ### register <your_name> <target_ip> <target_port>
                parts = line.split()
                if len(parts) != 4:
                    print("Usage: register <your_name> <target_ip> <target_port>")
                    continue
                my_name, target_ip, target_port = parts[1], parts[2], int(parts[3])
                msg = f"REGISTER {my_name} {port}"
                c_sock.sendto(msg.encode(), (target_ip, target_port))

            elif line.startswith("send_all"):
                ### Neu: Nachricht an alle bekannten Clients senden
                parts = line.split(" ", 1)
                if len(parts) != 2:
                    print("Usage: send_all <Nachricht>")
                    continue
                message = parts[1]
                for client_name, (client_ip, client_port) in known_clients.items():
                    msg = f"MSG {name} {message}"
                    c_sock.sendto(msg.encode(), (client_ip, client_port))

            elif line.startswith("send"):
                ### send <name> <message>
                parts = line.split(" ", 2)
                if len(parts) != 3:
                    print("Usage: send <name> <message>")
                    continue
                target_name = parts[1]
                message = parts[2]
                if target_name not in known_clients:
                    print(f"[Error] Unknown recipient: {target_name}")
                    continue
                target_ip, target_port = known_clients[target_name]
                msg = f"MSG {name} {message}"
                c_sock.sendto(msg.encode(), (target_ip, target_port))

            elif line == "list":
                ### Alle bekannten Clients anzeigen
                print("[Known Clients]")
                for n, (ip, p) in known_clients.items():
                    print(f"  {n}: {ip}:{p}")

            elif line == "peers":
                ### Neu: Anfrage an einen Client senden, um dessen Liste von bekannten Clients zu erhalten
                if not known_clients:
                    print("Keine bekannten Clients")
                    continue
                # Hier wird die Anfrage an den ersten registrierten Client geschickt
                target_ip, target_port = next(iter(known_clients.values()))
                c_sock.sendto("peers".encode(), (target_ip, target_port))

            elif line == "list fragen":
                ### Neu: Liste der vordefinierten Fragen mit Antworten anzeigen
                print("[System] Vordefinierte Fragen mit Antworten:")
                for question, answer in predefined_faq.items():
                    print(f"  \"{question}\" -> \"{answer}\"")

            elif line == "exit":
                print("[System] Bye!")
                break

def main():
    global name, port
    if len(sys.argv) != 3:
        print(f"Usage: python {sys.argv[0]} <your_name> <your_port>")
        sys.exit(1)
    name = sys.argv[1]
    port = int(sys.argv[2])

    threading.Thread(target=receive_loop, daemon=True).start()
    send_loop()

if __name__ == "__main__":
    main()
