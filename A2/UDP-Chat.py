import socket
import sys
import threading
import json

class ChatClient:
    # Initialisierung von Client mit Namen und Port
    def __init__(self, name, listen_port):
        self.name = name
        self.listen_port = listen_port
        self.known_peers = {}
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.socket.bind(('0.0.0.0', self.listen_port))
        self.running = True

    # sendet Registierungsnachricht
    def register(self, target_host, target_port):
        registration_message = {
            'type': 'register',
            'name': self.name,
            'host': self.get_local_ip(),
            'port': self.listen_port
        }
        try:
            self.send_message(target_host, target_port, json.dumps(registration_message).encode())
            print(f"Registrierungsanfrage an {target_host}:{target_port} gesendet.")
        except Exception as e:
            print(f"Fehler beim Senden der Registrierungsanfrage: {e}")

    # warten auf eingehende UDP-Pakete
    def process_incoming(self):
        while self.running:
            try:
                data, address = self.socket.recvfrom(4096)
                message = json.loads(data.decode())
                if message['type'] == 'register':
                    name = message['name']
                    host = message['host']
                    port = message['port']
                    if name != self.name and name not in self.known_peers:
                        self.known_peers[name] = (host, port)
                        print(f"'{name}' hat sich registriert: {host}:{port}")
                        # Sende dem neuen Peer unsere Informationen
                        self_info = {
                            'type': 'register',
                            'name': self.name,
                            'host': self.get_local_ip(),
                            'port': self.listen_port
                        }
                        self.send_message(host, port, json.dumps(self_info).encode())
                elif message['type'] == 'message':
                    sender_name = message.get('sender')
                    content = message.get('content')
                    print(f"Nachricht von '{sender_name}': {content}")
            except json.JSONDecodeError:
                print(f"Ungültiges JSON von {address}: {data.decode()}")
            except socket.error as e:
                if self.running:
                    print(f"Fehler beim Empfangen: {e}")
                    break
    
    # sendet Daten per UDP
    def send_message(self, host, port, data):
        self.socket.sendto(data, (host, port))

    # sucht Empfänger und sendet Nachricht
    def send_chat_message(self, recipient_name, message_content):
        if recipient_name in self.known_peers:
            host, port = self.known_peers[recipient_name]
            chat_message = {
                'type': 'message',
                'sender': self.name,
                'content': message_content
            }
            try:
                self.send_message(host, port, json.dumps(chat_message).encode())
                print(f"Nachricht an '{recipient_name}' gesendet: {message_content}")
            except Exception as e:
                print(f"Fehler beim Senden der Nachricht an '{recipient_name}': {e}")
        else:
            print(f"'{recipient_name}' ist nicht bekannt.")

    # ermittelt lokale IP des Rechners
    def get_local_ip(self):
        try:
            # Erstelle einen Socket, der keine Verbindung aufbaut
            s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            # Sende eine leere Nachricht an einen öffentlichen Server
            s.connect(("8.8.8.8", 80))
            local_ip = s.getsockname()[0]
            s.close()
            return local_ip
        except socket.error:
            return "127.0.0.1" # Fallback

    # Kommandozeile Schnittstelle für Interaktion im Chat
    def command_line_interface(self):
        while self.running:
            command = input("> ").split()
            if not command:
                continue
            action = command[0].lower()

            if action == 'register' and len(command) == 3:
                target_host = command[1]
                try:
                    target_port = int(command[2])
                    self.register(target_host, target_port)
                except ValueError:
                    print("Ungültiger Port.")
            elif action == 'send' and len(command) >= 3:
                recipient_name = command[1]
                message = " ".join(command[2:])
                self.send_chat_message(recipient_name, message)
            elif action == 'peers':
                print("Bekannte Peers:")
                for name, address in self.known_peers.items():
                    print(f"- {name}: {address[0]}:{address[1]}")
            elif action == 'exit':
                self.running = False
                print("Beende Chat.")
                break
            else:
                print("Ungültiger Befehl. Verfügbare Befehle: register <ip> <port>, send <name> <message>, peers, exit")

# Main-Methode mit Servermodus und Clientmodus
def main():
    if len(sys.argv) != 3:
        name = sys.argv[0]
        print(f"Usage: \"python {name} <eigener_name> <eigener_port>\"")
        sys.exit()

    client_name = sys.argv[1]
    try:
        listen_port = int(sys.argv[2])
    except ValueError:
        print("Ungültiger Port.")
        sys.exit()

    chat_client = ChatClient(client_name, listen_port)

    # Starte den Thread für den Nachrichtenempfang
    receive_thread = threading.Thread(target=chat_client.process_incoming)
    receive_thread.daemon = True
    receive_thread.start()

    # Starte die Kommandozeilen-Schnittstelle im Haupt-Thread
    chat_client.command_line_interface()

    # Warte auf das Beenden des Empfangsthreads
    receive_thread.join()
    chat_client.socket.close()

if __name__ == '__main__':
    main()