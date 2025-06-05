package tcp.Server;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;

import common.ChatProtocoll;
import common.Message;
public class ListenThread extends Thread
{
    Server server;
    Socket socket;
    private boolean running;
    private String name;

    public ListenThread(Server server,Socket socket)
    {
        this.server = server;
        this.socket = socket;
        this.name = "";
        this.running = true;
    }

    public void run()
    {
        try
        {
            InputStream in = socket.getInputStream();
            while(running)
            {
                String receiver;
                String[] split;
                Message relay;

                Message message = new Message(in);
                
                switch(message.header)
                {
                    case ChatProtocoll.CONNECT_REQUEST:
                        server.addrMap.putIfAbsent(message.payload, socket);
                        name = message.payload;
                        break;
                    case ChatProtocoll.REGISTRATION :
                        split = message.payload.split(ChatProtocoll.SEP);
                        receiver = split[0];
                        relay = new Message(ChatProtocoll.REGISTRATION, Arrays.copyOfRange(split, 1, split.length));
                        server.send(receiver, relay);
                        break;
                    case ChatProtocoll.MESSAGE :
                        split = message.payload.split(ChatProtocoll.SEP);
                        receiver = split[0];
                        relay = new Message(ChatProtocoll.MESSAGE, Arrays.copyOfRange(split, 1, split.length));
                        server.send(receiver, relay);
                        break;
                    case ChatProtocoll.MESSAGE_ALL:
                        split = message.payload.split(ChatProtocoll.SEP);
                        int count = Integer.valueOf(split[0]);
                        String[] names = Arrays.copyOfRange(split, 1, count+1);
                        String[] args = Arrays.copyOfRange(split, count+1, split.length);
                        relay = new Message(ChatProtocoll.MESSAGE, args);
                        server.sendAll(names, relay);
                        break;
                    case ChatProtocoll.FETCH_CLIENTS:
                        relay = new Message(ChatProtocoll.FETCH_CLIENTS, server.addrMap.keySet().toArray(new String[0]));
                        server.send(socket, relay);
                        break;
                    case ChatProtocoll.QUESTION:
                        split = message.payload.split(ChatProtocoll.SEP);
                        receiver = split[0];
                        relay = new Message(ChatProtocoll.QUESTION, Arrays.copyOfRange(split, 1, split.length));    
                        server.send(receiver, relay);
                        break;
                }
            }
        }
        catch(SocketException e)
        {
            System.err.println(e.getMessage());
            System.err.println("client " + name + " disconnected");;
        }
        catch(IOException e)
        {
        }
        finally
        {
            try
            {
                socket.close();
                server.addrMap.remove(name);
            }
            catch(IOException e)
            {
                System.out.println(e.getMessage()); 
            }
        }
    }
}