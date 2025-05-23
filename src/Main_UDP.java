import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.net.SocketException;
import java.net.UnknownHostException;

import javafx.util.*;

public class Main_UDP implements Runnable{

    private static String name;
    private static int port;
    private static InetAddress address;
    private static DatagramSocket s;
    private static final int packetSize = 4096;

    private static ConcurrentMap<String, Pair<InetAddress, Integer>> addressDict = new ConcurrentHashMap<>();


    public void run()
    {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true)
        {
            try
            {
                String input = br.readLine();
                if(input.startsWith("register"))
                {
                    String[] args = input.split(" ", 3);
                    if(args.length != 3)
                    {
                        printHelp();
                        continue;
                    }
                    try {
                        InetAddress targetip = InetAddress.getByName(args[1]);
                        int targetPort = Integer.valueOf(args[2]);
                        registerMessage(targetip, targetPort);
                    }
                    catch(UnknownHostException e)
                    {
                        System.out.println(e.getMessage());
                    }
                    catch(NumberFormatException e)
                    {
                        System.out.println(e.getMessage());
                    }
                    
                }
                else if(input.startsWith("send"))
                {
                    String[] args = input.split(" ", 3);
                    if(args.length != 3)
                    {
                        printHelp();
                        continue;
                    }
                    String targetName = args[1];
                    String message = args[2];

                    Pair<InetAddress, Integer> target = addressDict.get(targetName);
                    if(target == null)
                    {
                        System.out.println("target " + targetName + " not registered");
                        continue;
                    }
                    sendMessage(target.getKey(), target.getValue(), message);
                }
                else if (input.startsWith("list"))
                {
                    addressDict.forEach((k, v) -> 
                    {
                        System.out.println(k + ": " + v.getKey().getHostAddress() + ": " + v.getValue());
                    });
                }
                else 
                {
                    printHelp();
                }
            }
            catch (IOException e)
            {
                System.out.printf("Exception: %s\n",e.getMessage());
            }
        }
    }

    private void registerMessage(InetAddress addr, int targetPort) throws IOException
    {
        String registerMessage = "REGISTER:" + Main_UDP.name + ":" + address.toString().substring(1) + ":" + port;
        //System.out.println(registerMessage);
        byte[] data = registerMessage.getBytes(StandardCharsets.UTF_8);   
        send(addr, targetPort, data);
    }

    private void sendMessage(InetAddress addr, int port, String message) throws IOException
    {
        String sendMessage = "MESSAGE:" + message;
        //System.out.println(sendMessage);
        byte[] data = sendMessage.getBytes(StandardCharsets.UTF_8);
        send(addr, port, data);
    }

    private void send(InetAddress addr, int port, byte[] data) throws IOException
    {
        DatagramPacket packet = new DatagramPacket(data, data.length, addr, port);
        s.send(packet);
    }

    private void printHelp()
    {
        String help =   "available commands are:" + "\n" +
                        "   send <name> <message>" + "\n" +
                        "   register <ip> <port>" + "\n" +
                        "   list";
        System.out.println(help);
    }

    public static void main(String[] args) throws IOException 
    {
        if(args.length != 1)
        {
            System.out.println("usage: Main_UDP <name>");
            return;
        }
        name = args[0];
        try
        {
            s = new DatagramSocket();
            s.connect(InetAddress.getByName("8.8.8.8"), 10002);
            address = s.getLocalAddress();
            s.disconnect();
            port = s.getLocalPort();
            System.out.printf("UDP endpoint is (%s %d)\n", address.getHostAddress(), port);
        }
        catch (SocketException e)
        {
            System.out.println(e.getMessage());
        }

        Thread inputThread = new Thread(new Main_UDP());
        inputThread.start();
        
        byte[] buffer = new byte[packetSize];
        String message;
        while (true)
        {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            s.receive(packet);
            message = new String(buffer, 0, packet.getLength(), "UTF-8");
            if(message.startsWith("REGISTER"))
            {
                String[] temp = message.split(":", 4);
                if(temp.length != 4)
                {
                    packetDataNotValid(message);
                    continue;
                }

                String name = temp[1];
                InetAddress addr = InetAddress.getByName(temp[2]);
                int port = Integer.valueOf(temp[3]);

                addressDict.putIfAbsent(name,new Pair<InetAddress,Integer>(addr, port));

            }
            else if(message.startsWith("MESSAGE"))
            {
                String[] temp = message.split(":", 2);
                if(temp.length != 2)
                {
                    packetDataNotValid(message);
                    continue;
                }
                System.out.println(temp[1]);
            }
            else
            {
                packetDataNotValid(message);
            }
        }
    }

    private static void packetDataNotValid(String message)
    {
        System.out.println("Data not valid. Message was: " + message);
    }

    // ************************************************************************
    // listenAndTalk
    // ************************************************************************
    /*private static void listenAndTalk ( int port ) throws IOException  {
        byte[] buffer = new byte[packetSize];
        String line;
        do {
            DatagramPacket p = new DatagramPacket(buffer,buffer.length);
            s.receive(p);
            line = new String(buffer,0,p.getLength(),"UTF-8");
            System.out.println(line);
        } while (!line.equalsIgnoreCase("stop"));
        s.close();
    }

    // ************************************************************************
    // connectAndTalk
    // ************************************************************************
    private static void connectAndTalk ( String other_host, int other_port ) throws IOException {
        InetAddress other_address = InetAddress.getByName(other_host);
        DatagramSocket s = new DatagramSocket();
        byte[] buffer = new byte[packetSize];
        String line;
        do {
            line = readString();
            buffer = line.getBytes("UTF-8");
            DatagramPacket p = new DatagramPacket(buffer,buffer.length,other_address,other_port);
            s.send(p);
        } while (!line.equalsIgnoreCase("stop"));
        s.close();
    }*/
}