import socket
import sys
import threading

#called when program executed as server using "python TCP.py -l 4200"
def server(port):
    #creates TCP-Socket
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s_sock:
        s_sock.bind(('0.0.0.0',port))
        s_sock.listen() #waits for messages
        #checking for messages until termination
        while True:
            #new client-socket
            c_sock, c_address = s_sock.accept()
            #new thread executing serveClient with c_sock, c_adress as arugments
            t = threading.Thread(target=serveClient,args=(c_sock,c_address))
            t.start()

#own thread per client (to wait for its messages)
def serveClient(c_sock,c_address):
    with c_sock:
        #checks for messages and prints them until its stopped
        while True:
            line = c_sock.recv(1024).decode().rstrip()
            print(f'Message <{repr(line)}> received from client {c_address}')
            if line.lower() == 'stop':
                break

#called when program executed as client using "python TCP.py 127.0.0.1 4200"
def client(host,port):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as c_sock:
        #execute handshake to connect with host (via its port)
        c_sock.connect((host,port))
        for line in sys.stdin:
            line = line.rstrip()
            c_sock.sendall(line.encode()) #sends message to server
            if line.lower() == 'stop':
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
        client(sys.argv[1],port)

if __name__ == '__main__':
    main()