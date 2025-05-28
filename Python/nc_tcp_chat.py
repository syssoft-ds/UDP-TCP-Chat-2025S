import socket
import sys
import threading

registered_clients = {}  # {name: (ip, port)}
lock = threading.Lock()

def server(port):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s_sock:
        s_sock.bind(('0.0.0.0', port))
        s_sock.listen()
        hostname = socket.gethostname()
        local_ip = socket.gethostbyname(hostname)
        print(f"Server läuft auf IP {local_ip}, Port {port}")

        while True:
            c_sock, c_address = s_sock.accept()
            t = threading.Thread(target=serveClient, args=(c_sock, c_address))
            t.start()

def serveClient(c_sock, c_address):
    with c_sock:
        name = None
        while True:
            try:
                line = c_sock.recv(1024).decode().rstrip()
                if not line:
                    break
                print(f'Message <{repr(line)}> received from {c_address}')

                if line.startswith("REGISTER:"):
                    name = line.split("REGISTER:")[1].strip()
                    with lock:
                        registered_clients[name] = c_sock  # Save the socket directly
                    print(f"Client '{name}' registriert mit Adresse {c_address}")
                    continue

                elif line.startswith("SEND:"):
                    parts = line.split(":", 2)
                    if len(parts) != 3:
                        c_sock.sendall("Fehlerhaftes SEND-Format.\n".encode())
                        continue
                    target_name, message = parts[1], parts[2]
                    with lock:
                        target_sock = registered_clients.get(target_name)
                    if target_sock:
                        try:
                            target_sock.sendall(f"[{name}] {message}\n".encode())
                        except Exception as e:
                            c_sock.sendall(f"Fehler beim Senden an {target_name}: {e}\n".encode())
                    else:
                        c_sock.sendall(f"Empfänger '{target_name}' nicht gefunden.\n".encode())
                    continue

                elif line.lower() == 'stop':
                    break

            except Exception as e:
                print(f"Fehler im Client-Thread: {e}")
                break

        if name:
            with lock:
                registered_clients.pop(name, None)
            print(f"Client '{name}' abgemeldet")

def client(host, port):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as c_sock:
        c_sock.connect((host, port))
        name = input("Bitte gib deinen Namen ein: ").strip()
        c_sock.sendall(f"REGISTER:{name}".encode())

        recv_thread = threading.Thread(target=receive_messages, args=(c_sock,))
        recv_thread.daemon = True
        recv_thread.start()

        for line in sys.stdin:
            line = line.rstrip()
            c_sock.sendall(line.encode())
            if line.lower() == 'stop':
                break

def receive_messages(c_sock):
    while True:
        try:
            msg = c_sock.recv(1024)
            if not msg:
                break
            print(msg.decode(), end="")
        except:
            break

def main():
    if len(sys.argv) != 3:
        name = sys.argv[0]
        print(f"Usage: \"{name} -l <port>\" or \"{name} <ip> <port>\"")
        sys.exit()
    port = int(sys.argv[2])
    if sys.argv[1].lower() == '-l':
        server(port)
    else:
        client(sys.argv[1], port)

if __name__ == '__main__':
    main()