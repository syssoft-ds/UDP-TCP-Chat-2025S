package udp.Endpoint;

import common.Message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.javatuples.Pair;

public class Endpoint implements Runnable
{

    public Map<String, Pair<InetAddress, Integer>> addressDict;

    public String name;
    public int port;
    public InetAddress address;
    public DatagramSocket s;

    public Endpoint(String name)
    {
        this.name = name;
        addressDict = Collections.synchronizedMap(new HashMap<>());
        try
        {
            this.s = new DatagramSocket();
            this.s.connect(InetAddress.getByName("8.8.8.8"), 10002);
            this.address = s.getLocalAddress();
            this.s.disconnect();
            this.port = s.getLocalPort();
            System.out.printf("UDP endpoint is (%s %d)\n", address.getHostAddress(), port);
        }
        catch(UnknownHostException e)
        {
            System.out.println(e.getMessage());
        }
        catch (SocketException e)
        {
            System.out.println(e.getMessage());
        }
    }

    public void run()
    {
        Thread listenThread = new ListenThread(this);
        Thread sendThread = new SendThread(this);

        listenThread.start();
        sendThread.start();
    }
    
    public void send(InetAddress addr, int port, Message message) throws IOException
    {
        byte[] data = message.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, addr, port);
        s.send(packet);
    }

    public void sendAll(Message message) throws IOException
    {
        for(Pair<InetAddress, Integer> pair : addressDict.values())
        {
            send(pair.getValue0(), pair.getValue1(), message);
        }
    }
}