import socket
import sys
import threading
import json

# Global dictionary to store known clients: {name: (ip, port)}
known_clients = {}
my_name = ""
my_ip = ""
my_port = 0

#Handles incoming UDP messages, processing registration requests and chat messages.
def handle_incoming_messages(s_sock):
    while True:
        try:
            line, c_address = s_sock.recvfrom(4096)
            message = line.decode().rstrip()
            
            # Message format: {"type": "...", "data": {...}}
            msg_data = json.loads(message)
            msg_type = msg_data.get("type")

            if msg_type == "register":
                handle_message_register(s_sock, c_address, msg_data, message)

            elif msg_type == "register_ack":
                handle_message_register_ack(msg_data)

            elif msg_type == "chat":
                handle_message_chat(msg_data)
            
        except Exception as e:
            print(f"Error handling incoming message: {e}")

def handle_message_register(s_sock, c_address, msg_data, message):
    
    sender_name = msg_data.get("name")
    sender_ip = msg_data.get("ip")
    sender_port = msg_data.get("port")

    # If the sender's IP is 127.0.0.1 and we are not, replace with actual client IP
    if sender_ip == "127.0.0.1" and c_address[0] != "127.0.0.1":
            sender_ip = c_address[0]

    if sender_name not in known_clients:
        known_clients[sender_name] = (sender_ip, sender_port)
        print(f"Registered new client: {sender_name} at {sender_ip}:{sender_port}")
        
        #send registration acknowledgement
        response_msg = {"type": "register_ack", "name": my_name, "ip": my_ip, "port": my_port}
        s_sock.sendto(json.dumps(response_msg).encode(), c_address)

    else:
        print(f"Client {sender_name} already registered and up-to-date.")

def handle_message_register_ack(msg_data):

    ack_name = msg_data.get("name")
    ack_ip = msg_data.get("ip")
    ack_port = msg_data.get("port")

    if ack_name not in known_clients:
        known_clients[ack_name] = (ack_ip, ack_port)
        print(f"Received registration acknowledgment from: {ack_name} at {ack_ip}:{ack_port}")

    else:
        print(f"Received registration acknowledgment from: {ack_name} (already known).")

def handle_message_chat(msg_data):
    sender_name = msg_data.get("sender")
    chat_message = msg_data.get("message")
    print(f"[{sender_name}]: {chat_message}")
   


#Starts the UDP server to listen for incoming messages.
def start_server(name, port):

    global my_name, my_port, my_ip
    my_name = name
    my_port = port

    #use only on local device
    my_ip = "127.0.0.1"
    print(f"Starting chat instance '{my_name}' on {my_ip}:{my_port}")

    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s_sock:
        s_sock.bind(('0.0.0.0', port))
        
        # Start a thread to handle incoming messages
        threading.Thread(target=handle_incoming_messages, args=(s_sock,), daemon=True).start()

        # list all commands for user to know what to do
        print("Ready for commands (register <ip> <port>, send <name> <message>, list, stop):")

        while True:

            try:
                #read what user wants to do
                command_line = input()
                parts = command_line.split(maxsplit=2)
                command = parts[0].lower()

                #check what user wants to do and send message
                if command == "register":
                    send_registration(parts)

                elif command == "send":
                    send_message(parts)

                elif command == "list":
                    if known_clients:
                        print("Known clients:")
                        for name, (ip, port) in known_clients.items():
                            print(f"  - {name}: {ip}:{port}")
                    else:
                        print("No clients registered yet.")

                elif command == "stop":
                    print("Shutting down...")
                    break

                else:
                    print("Unknown command. Available commands: register <ip> <port>, send <name> <message>, list, stop")

            except Exception as e:
                print(f"Error: {e}")

def send_registration(parts):
    
    if len(parts) == 3:
        reg_ip = parts[1]
        reg_port = int(parts[2])
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as c_sock:
            # Send registration message
            reg_msg = {
                "type": "register",
                "name": my_name,
                "ip": my_ip,
                "port": my_port
            }
            c_sock.sendto(json.dumps(reg_msg).encode(), (reg_ip, reg_port))
            print(f"Sent registration request to {reg_ip}:{reg_port}")
    else:
        print("Usage: register <ip> <port>")

def send_message(parts):
    
    if len(parts) >= 3:
        target_name = parts[1]
        message = parts[2]
        if target_name in known_clients:
            target_ip, target_port = known_clients[target_name]
            with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as c_sock:
                chat_msg = {
                    "type": "chat",
                    "sender": my_name,
                    "message": message
                }
                c_sock.sendto(json.dumps(chat_msg).encode(), (target_ip, target_port))
                print(f"Message sent to {target_name}")
        else:
            print(f"Client '{target_name}' not found. Use 'list' to see known clients.")
    else:
        print("Usage: send <name> <message>")



#main, called using "python UDP.py Alex 4200" (at least two consoles with different names and ports)
def main():
    if len(sys.argv) != 3:
        name = sys.argv[0]
        print(f"Usage: \"{name} <your_name> <port>\"")
        sys.exit()
    
    instance_name = sys.argv[1]
    port = int(sys.argv[2])
    
    start_server(instance_name, port)

if __name__ == '__main__':
    main()

#1) Open console, "python UDP.py Alex 4200"
#2) Open console, "python UDP.py Ben 4201"
#3) Alex: "register [Ben's IP] 4201"
#3) Ben: "register [Alex's IP] 4200"
#4) Console 1/2: "send [Alex/Ben] Hello World"
#5) ...
#n) Console 1&2: "stop"