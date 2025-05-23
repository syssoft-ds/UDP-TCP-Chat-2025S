import socket
import sys

#Server, started with "py UDP.py -l 4200"
def receiveLines(port):
    #creates a UDP-Socket
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s_sock:
        s_sock.bind(('0.0.0.0',port))
        #waits for messages and prints a message when received
        while True:
            line, c_address = s_sock.recvfrom(4096)
            line = line.decode().rstrip()
            print(f'Message <{repr(line)}> received from client {c_address}')
            #if message == "stop" the program terminates
            if line.lower() == 'stop':
                break

#client, started with "py UDP.py 127.0.0.1 4200"
def sendLines(host,port):
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as c_sock:
        for line in sys.stdin:
            line = line.rstrip()
            c_sock.sendto(line.encode(),(host,port))
            #if message == "stop" the program terminates
            if line.lower() == 'stop':
                break

def main():
    if len(sys.argv) != 3:
        name = sys.argv[0]
        print(f"Usage: \"{name} -l <port>\" or \"{name} <ip> <port>\"")
        sys.exit()
    port = int(sys.argv[2])
    #server
    if sys.argv[1].lower() == '-l':
        receiveLines(port)
    #client
    else:
        sendLines(sys.argv[1],port)

if __name__ == '__main__':
    main()

#1) Open console, init server with "py UDP.py -l 4200"
#2) Open console, init client with "py UDP.py 127.0.0.1 4200
#3) Type anything inside the client-console to send messages
#4) stop program by sending a "stop"-message