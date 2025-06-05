package tcp.Server;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;

import common.Message;

import java.util.Map;

public class Server implements Runnable
{
    public ServerSocket socket;
    public Map<String, Socket> addrMap;

    public Server()
    {
        addrMap = new ConcurrentHashMap<>();
        try
        {
            socket = new ServerSocket(0);
            String ip = getIP();
            int port = socket.getLocalPort();
            System.out.println("Server Adress is: " +  ip  + " " + port);
        }
        catch (IOException e)
        {
            System.out.println(e.getMessage());
        }
    }
    
    public void run()
    {
        Thread acceptingThread = new AcceptingThread(this);
        acceptingThread.start();
    }

    private static String getIP()
    {
        try
        {
            Socket temp = new Socket();
            temp.connect(new InetSocketAddress("google.com", 80));
            String ip = temp.getLocalAddress().getHostAddress();   
            temp.close();
            return ip;
        }
        catch(IOException e)
        {
            System.out.println(e.getMessage());
        }
        return "127.0.0.1";
    }
        
    public void send(String name, Message message)
    {
        Socket s = addrMap.get(name);
        if(s == null) return;
        send(s, message);
    }

    public void send(Socket s, Message message)
    {
        try
        {
            OutputStream os = s.getOutputStream();
            os.write(message.getBytes());
        }
        catch(IOException e)
        {
            System.out.println(e.getMessage());
        }
    }

    public void sendAll(String[] names, Message message)
    {
        for(String s : names)
        {
            send(s, message);
        }
    }
}