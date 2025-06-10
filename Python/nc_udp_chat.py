import socket
import sys
import threading

def receiveAndRelay(port):
    clients = {}         # (ip, port) -> name
    name_to_addr = {}    # name -> (ip, port)
    server_running = True

    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s_sock:
        s_sock.bind(('0.0.0.0', port))
        print(f"Server listening on UDP port {port}...")

        def broadcastClientList():
            client_list = "\n".join([f"{name} ({ip}:{port})" for (ip, port), name in clients.items()])
            for client_addr in clients:
                try:
                    s_sock.sendto(f"[Client List]\n{client_list}".encode(), client_addr)
                except Exception as e:
                    print(f"Error sending client list to {client_addr}: {e}")

        while server_running:
            try:
                msg_raw, addr = s_sock.recvfrom(4096)
                msg = msg_raw.decode().strip()
                print(f'Message <{repr(msg)}> received from {addr}')

                # Stop entire server
                if msg.upper() == "STOP SERVER":
                    print(f"[!] Server shutdown requested by {addr}")
                    for client_addr in clients:
                        s_sock.sendto(b"[Server] Shutting down.", client_addr)
                    server_running = False
                    break

                # Name registration
                if msg.startswith("NAME "):
                    requested_name = msg[5:].strip()
                    if requested_name in name_to_addr:
                        s_sock.sendto(f"[Server] Name '{requested_name}' is already in use.".encode(), addr)
                        continue
                    clients[addr] = requested_name
                    name_to_addr[requested_name] = addr
                    print(f"Client {addr} registered as '{requested_name}'")
                    broadcastClientList()
                    continue

                if msg.startswith("Q "):
                    parts = msg.split(maxsplit=1)
                    if len(parts) < 2:
                        s_sock.sendto(b"[Server] Invalid question format. Use: Q <question number>", addr)
                        continue
                    answer = ''
                    question = parts[1]
                    if question == '1':
                        answer = 'JA'
                    elif question == '2':
                        answer = 'NEIN'
                    else:
                        answer = 'Unbekannte Frage'

                    content = answer
                    recipient = clients[addr]
                    forwarded = f"[ANSWER TO QUESTION: ] {content}"
                    if recipient in name_to_addr:
                        dest_addr = name_to_addr[recipient]
                        forwarded = f"[{recipient} → {recipient}] {content}"
                        s_sock.sendto(forwarded.encode(), dest_addr)
                    continue
                    

                # Client disconnects
                if msg.lower() == 'stop':
                    if addr in clients:
                        name = clients.pop(addr)
                        name_to_addr.pop(name, None)
                        print(f"Client {addr} ('{name}') disconnected")
                        broadcastClientList()
                    continue

                # Reject messages from unregistered clients
                if addr not in clients:
                    s_sock.sendto(b"[Server] Please register first using: NAME <yourname>", addr)
                    continue

                # Message to other client(s)
                parts = msg.split(maxsplit=1)
                if len(parts) < 2:
                    s_sock.sendto(b"[Server] Invalid message format. Use: <recipient> <message>", addr)
                    continue

                recipient, content = parts
                sender_name = clients[addr]

                if recipient.lower() == "all":
                    forwarded = f"[{sender_name} → all] {content}"
                    for target_addr in clients:
                        if target_addr != addr:
                            s_sock.sendto(forwarded.encode(), target_addr)
                    continue

                # Direct message
                if recipient in name_to_addr:
                    dest_addr = name_to_addr[recipient]
                    forwarded = f"[{sender_name} → {recipient}] {content}"
                    s_sock.sendto(forwarded.encode(), dest_addr)
                else:
                    s_sock.sendto(f"[Server] Unknown recipient: {recipient}".encode(), addr)

            except Exception as e:
                print(f"Error: {e}")
                break

        print("Server shutdown complete.")



def receiveFromServer(c_sock):
    while True:
        try:
            msg, _ = c_sock.recvfrom(4096)
            print(f"\n[Message from server] {msg.decode()}")
        except Exception as e:
            print(f"Receive error: {e}")
            break

def clientModeWithReceiving(server_host, server_port):
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as c_sock:
        # 🔧 Binde den Socket an einen lokalen Port (automatisch zugewiesen)
        c_sock.bind(('', 0))  # '' = alle Interfaces, 0 = zufälliger Port
        print(f"Client listening on {c_sock.getsockname()}")

        # 🎧 Empfangs-Thread starten
        receiver = threading.Thread(target=receiveFromServer, args=(c_sock,), daemon=True)
        receiver.start()

        # 📨 Sendeschleife
        while True:
            try:
                line = input()
                line = line.rstrip()
                c_sock.sendto(line.encode(), (server_host, server_port))
                if line.lower() == 'stop':
                    break
            except KeyboardInterrupt:
                break

def main():
    if len(sys.argv) != 3:
        name = sys.argv[0]
        print(f"Usage: \"{name} -l <port>\" or \"{name} <ip> <port>\"")
        sys.exit()
    port = int(sys.argv[2])
    if sys.argv[1].lower() == '-l':
        receiveAndRelay(port)
    else:
        clientModeWithReceiving(sys.argv[1], port)

if __name__ == '__main__':
    main()
