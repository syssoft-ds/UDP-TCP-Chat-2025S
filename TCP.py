import socket
import sys
import threading
import json
import datetime

#Global dictionary to store known clients: {name: client_socket}
#This will store the actual socket connection for each registered client.
known_clients = {}
#A lock to protect access to the known_clients dictionary from multiple threads
clients_lock = threading.Lock()


#Called when program executed as server using "python TCP.py -l <port>"
def server(port):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s_sock:
        s_sock.bind(('0.0.0.0', port))
        s_sock.listen() #Waits for messages
        print(f"TCP chat server listening on port {port}...")
        #Checking for messages until termination
        while True:
            #New client-socket
            c_sock, c_address = s_sock.accept()
            print(f"New connection from {c_address}")
            #New thread executing serveClient with c_sock, c_address as arguments
            t = threading.Thread(target=serveClient, args=(c_sock, c_address))
            t.daemon = True #Allow main program to exit even if threads are running
            t.start()

#Handles incoming TCP messages from a connected client.
def serveClient(c_sock, c_address):
    global known_clients
    client_name = None #To store the name of the client once registered

    with c_sock:
        while True:
            try:
                #Receive messages from the client
                line = c_sock.recv(4096).decode().rstrip()
                if not line: #Client disconnected
                    break

                msg_data = json.loads(line)
                msg_type = msg_data.get("type")

                if msg_type == "register":
                    client_name = handle_message_registration(c_sock, c_address, msg_data)
                    #connection is closed when no client is returned
                    if client_name == "":
                        break
                    
                elif msg_type == "chat":
                    handle_message_chat(c_sock, c_address, client_name, msg_data)
                
                elif msg_type == "broadcast":
                    handle_message_broadcast(c_sock, c_address, client_name, msg_data)
                    
                elif msg_type == "list":
                    #Handle request to list known clients
                    if client_name:
                        with clients_lock:
                            client_names = list(known_clients.keys())
                            list_msg = {"type": "client_list", "clients": client_names}
                            c_sock.sendall(json.dumps(list_msg).encode())
                            print(f"Sent client list to {client_name}.")
                    else:
                        print(f"Unregistered client {c_address} requested client list.")

                elif msg_type == "stop":
                    print(f'Client {client_name if client_name else c_address} sent stop message. Disconnecting.')
                    break
                
            except Exception as e:
                print(f"Error serving client {client_name if client_name else c_address}: {e}")
                break #Disconnect on other errors

    #Clean up client from known_clients when connection closes
    if client_name:
        with clients_lock:
            if client_name in known_clients:
                del known_clients[client_name]
                print(f"Client {client_name} disconnected.")
    else:
        print(f"Unregistered client {c_address} disconnected.")

def handle_message_registration(c_sock, c_address, msg_data):

    sender_name = msg_data.get("name")

    with clients_lock:

        #check if user is new (cause should not be registered otherwise)
        if sender_name not in known_clients:

            #add client
            known_clients[sender_name] = c_sock
            client_name = sender_name
            print(f"Registered new client: {sender_name} from {c_address}")

            #Send registration acknowledgement back to the client
            response_msg = {"type": "register_ack", "status": "success", "message": f"Welcome, {sender_name}!"}
            c_sock.sendall(json.dumps(response_msg).encode())
            return client_name

        else:
            response_msg = {"type": "register_ack", "status": "error", "message": f"Name '{sender_name}' already taken."}
            c_sock.sendall(json.dumps(response_msg).encode())
            print(f"Client {c_address} tried to register with existing name: {sender_name}")
            
            return ""
        
def handle_message_chat(c_sock, c_address, client_name, msg_data):

    #Ensure client is registered before sending chat
    if client_name:

        target_name = msg_data.get("recipient")
        chat_message = msg_data.get("message")

        with clients_lock:
            target_sock = known_clients.get(target_name)
            if target_sock:
                #Forward the message to the target client
                forward_msg = {"type": "chat", "sender": client_name, "message": chat_message}
                target_sock.sendall(json.dumps(forward_msg).encode())
                print(f"Message from {client_name} to {target_name} forwarded.")
            else:
                print(f"Client '{target_name}' not found for message from {client_name}.")
                #Inform sender that recipient is not found
                error_msg = {"type": "error", "message": f"Client '{target_name}' not found."}
                c_sock.sendall(json.dumps(error_msg).encode())
        
    else:
        print(f"Unregistered client {c_address} sent chat message.")

def handle_message_broadcast(c_sock, c_address, client_name, msg_data):

    if client_name:

        broadcast_message = msg_data.get("message")

        with clients_lock:

            for name, sock in known_clients.items():

                if name != client_name: #Don't send broadcast back to sender
                    
                    forward_msg = {"type": "chat", "sender": f"BROADCAST from {client_name}", "message": broadcast_message}
                    
                    try:
                        sock.sendall(json.dumps(forward_msg).encode())
                        print(f"Broadcast from {client_name} forwarded to {name}.")
                    except Exception as e:
                        print(f"Error sending broadcast to {name}: {e}")
            
            #Send an acknowledgment to the sender that the broadcast was processed
            ack_msg = {"type": "broadcast_ack", "status": "success", "message": "Broadcast sent to all known clients."}
            c_sock.sendall(json.dumps(ack_msg).encode())

    else:
        print(f"Unregistered client {c_address} sent broadcast message.")


#Called when program executed as client using "python TCP.py <server_ip> <server_port> <your_name>"
def client(host, port, my_name):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as c_sock:
        try:
            c_sock.connect((host, port))
            print(f"Connected to server at {host}:{port}")

            #Start a thread to handle incoming messages from the server
            threading.Thread(target=client_receive_handler, args=(c_sock, my_name), daemon=True).start()

            #Send initial registration message
            reg_msg = {"type": "register", "name": my_name}
            c_sock.sendall(json.dumps(reg_msg).encode())
            print(f"Sent registration request as '{my_name}' to server.")

            print("Ready for commands (send <name> <message>, broadcast <message>, list, stop):")

            for line in sys.stdin:
                line = line.rstrip()
                parts = line.split(maxsplit=2)
                command = parts[0].lower()

                if command == "send":
                    if len(parts) >= 3:
                        target_name = parts[1]
                        message_content = parts[2]
                        chat_msg = {
                            "type": "chat",
                            "recipient": target_name,
                            "message": message_content
                        }
                        c_sock.sendall(json.dumps(chat_msg).encode())
                        print(f"Requested to send message to {target_name}.")
                    else:
                        print("Usage: send <name> <message>")
                
                elif command == "broadcast":
                    if len(parts) >= 2:
                        message_content = parts[1]
                        broadcast_msg = {
                            "type": "broadcast",
                            "message": message_content
                        }
                        c_sock.sendall(json.dumps(broadcast_msg).encode())
                        print(f"Requested to broadcast message: {message_content}.")
                    else:
                        print("Usage: broadcast <message>")

                elif command == "list":
                    list_req_msg = {"type": "list"}
                    c_sock.sendall(json.dumps(list_req_msg).encode())
                    print("Requested client list from server.")

                elif command == "stop":
                    stop_msg = {"type": "stop"}
                    c_sock.sendall(json.dumps(stop_msg).encode())
                    print("Sent stop message to server. Shutting down...")
                    break #Exit the input loop and close the client socket

                else:
                    print("Unknown command. Available commands: send <name> <message>, broadcast <message>, list, stop")

        except Exception as e:
            print(f"Client error: {e}")
            print("Client shutting down.")

#Handles incoming messages for the client from the server
def client_receive_handler(c_sock, my_name):
    while True:
        try:
            message = c_sock.recv(4096).decode().rstrip()
            if not message:
                print("Server disconnected.")
                break
            
            msg_data = json.loads(message)
            msg_type = msg_data.get("type")

            #check what type of message is received
            if msg_type == "register_ack":
                status = msg_data.get("status")
                ack_message = msg_data.get("message")
                print(f"Registration Acknowledgment: {status.upper()} - {ack_message}")
                if status == "error":
                    print("Exiting due to registration error. Please try another name or port.")
                    sys.exit(1) #Exit if registration failed

            elif msg_type == "chat":
                sender_name = msg_data.get("sender")
                chat_message = msg_data.get("message")
                print(f"[{sender_name}]: {chat_message}")

                if sender_name != my_name: #Avoid replying to self if broadcasted to self (not supposed to happen with current server logic)
                    response_message = None
                    if "Was ist deine IP-Adresse?" in chat_message:
                        ip_addr = "127.0.0.1"
                        response_message = f"Meine IP-Adresse ist: {ip_addr}"
                    elif "Wie viel Uhr haben wir?" in chat_message:
                        current_time = datetime.datetime.now().strftime("%H:%M:%S")
                        response_message = f"Es ist {current_time} Uhr."
                    elif "Welche Rechnernetze HA war das?" in chat_message:
                        response_message = "4. HA, Aufgabe 4"

                    if response_message:
                        #Send the automatic response back to the sender
                        response_msg_data = {
                            "type": "chat",
                            "recipient": sender_name,
                            "message": response_message
                        }
                        c_sock.sendall(json.dumps(response_msg_data).encode())
                        print(f"Sent automatic response to {sender_name}: {response_message}")


            elif msg_type == "client_list":
                clients = msg_data.get("clients", [])
                if clients:
                    print("Known clients:")
                    for name in clients:
                        print(f"  - {name}")
                else:
                    print("No clients registered yet on the server.")
            
            elif msg_type == "broadcast_ack":
                status = msg_data.get("status")
                ack_message = msg_data.get("message")
                print(f"Broadcast Acknowledgment: {status.upper()} - {ack_message}")

            elif msg_type == "error":
                error_message = msg_data.get("message")
                print(f"Server Error: {error_message}")

        except Exception as e:
            print(f"Error in client receive thread: {e}")
            break



def main():
    if len(sys.argv) < 3 or len(sys.argv) > 4:
        name = sys.argv[0]
        print(f"Usage for server: \"{name} -l <port>\"")
        print(f"Usage for client: \"{name} <server_ip> <server_port> <your_name>\"")
        sys.exit()

    if sys.argv[1].lower() == '-l':
        if len(sys.argv) != 3:
            print(f"Usage for server: \"{sys.argv[0]} -l <port>\"")
            sys.exit(1)
        port = int(sys.argv[2])
        server(port)

    else:
        if len(sys.argv) != 4:
            print(f"Usage for client: \"{sys.argv[0]} <server_ip> <server_port> <your_name>\"")
            sys.exit(1)
        host = sys.argv[1]
        port = int(sys.argv[2])
        my_name = sys.argv[3]
        client(host, port, my_name)

if __name__ == '__main__':
    main()



#Program did not work again, cause the following error appeared (most likely due to my firewall):
#Es konnte keine Verbindung hergestellt werden, da der Zielcomputer die Verbindung verweigerte