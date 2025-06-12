package tcp.Client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import common.Message;

public class Client implements Runnable
{

    public Socket socket;
    public String name;
    public Set<String> names;

    public Client(String name, String addr, int port)
    {
        names = Collections.synchronizedSet(new HashSet<>());
        try
        {
            this.socket = new Socket(InetAddress.getByName(addr), port);
            this.name = name;

        } catch (IOException e)
        {
            System.out.println(e.getMessage());
        }
    
    }

    public void send(Message message) throws IOException
    {
        OutputStream os = socket.getOutputStream();
        os.write(message.getBytes());
    }
    
    public void run()
    {
        Thread listenThread = new ListenThread(this);
        Thread sendThread = new SendThread(this);

        listenThread.start();
        sendThread.start();
    }
}