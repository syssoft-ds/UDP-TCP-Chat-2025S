import socket
import sys
import threading

clients = {}  # name -> socket
clients_lock = threading.Lock()
shutdown_flag = threading.Event()

def broadcast_client_list():
    with clients_lock:
        names = 'CLIENTS: ' + ' '.join(clients.keys())
        for sock in clients.values():
            try:
                sock.sendall(names.encode() + b'\n')
            except:
                pass

def handle_message(name, msg):
    parts = msg.split(' ', 2)
    if len(parts) < 2:
        return

    command = parts[0].lower()

    if command == 'send' and len(parts) == 3:
        target_name, message = parts[1], parts[2]
        with clients_lock:
            target = clients.get(target_name)
            if target:
                try:
                    target.sendall(f'FROM {name}: {message}\n'.encode())
                except:
                    pass
    elif command == 'all' and parts[1] == 'send' and len(parts) == 3:
        message = parts[2]
        with clients_lock:
            for cname, sock in clients.items():
                if cname != name:
                    try:
                        sock.sendall(f'FROM {name} (broadcast): {message}\n'.encode())
                    except:
                        pass
    elif msg.strip() == 'STOP SERVER':
        shutdown_flag.set()

def serveClient(c_sock, c_address):
    name = None
    try:
        with c_sock:
            line = c_sock.recv(1024).decode().rstrip()
            if not line.startswith("NAME "):
                c_sock.sendall(b'ERROR: First message must be NAME <yourname>\n')
                return

            name = line[5:].strip()
            with clients_lock:
                if name in clients:
                    c_sock.sendall(b'ERROR: Name already taken\n')
                    return
                clients[name] = c_sock
            broadcast_client_list()

            while not shutdown_flag.is_set():
                data = c_sock.recv(1024)
                if not data:
                    break
                line = data.decode().rstrip()
                print(f'Message <{repr(line)}> from {name}@{c_address}')
                handle_message(name, line)

    except Exception as e:
        print(f"Error with client {name}@{c_address}: {e}")
    finally:
        if name:
            with clients_lock:
                if name in clients:
                    del clients[name]
            broadcast_client_list()
        print(f"Connection to {name}@{c_address} closed")

def server(port):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s_sock:
        s_sock.bind(('0.0.0.0', port))
        s_sock.listen()
        print(f"Server started on port {port}")
        while not shutdown_flag.is_set():
            try:
                s_sock.settimeout(1.0)
                c_sock, c_address = s_sock.accept()
                t = threading.Thread(target=serveClient, args=(c_sock, c_address), daemon=True)
                t.start()
            except socket.timeout:
                continue
        print("Server shutting down...")

def client(host, port):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as c_sock:
        c_sock.connect((host, port))

        def recv_thread():
            while True:
                data = c_sock.recv(1024)
                if not data:
                    break
                print(data.decode().rstrip())

        t = threading.Thread(target=recv_thread, daemon=True)
        t.start()

        for line in sys.stdin:
            line = line.rstrip()
            c_sock.sendall(line.encode() + b'\n')
            if line == 'STOP SERVER':
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
