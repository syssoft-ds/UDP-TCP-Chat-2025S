import socket
import sys
import threading

name = None
port = None
known_clients = {}  # name -> (ip, port)

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

def send_loop():
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as c_sock:
        for line in sys.stdin:
            line = line.strip()
            if line.startswith("register"):
                # register <name> <ip> <port>
                parts = line.split()
                if len(parts) != 4:
                    print("Usage: register <your_name> <target_ip> <target_port>")
                    continue
                my_name, target_ip, target_port = parts[1], parts[2], int(parts[3])
                msg = f"REGISTER {my_name} {port}"
                c_sock.sendto(msg.encode(), (target_ip, target_port))

            elif line.startswith("send"):
                # send <name> <message>
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
                print("[Known Clients]")
                for n, (ip, p) in known_clients.items():
                    print(f"  {n}: {ip}:{p}")

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
