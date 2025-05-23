
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.io.IOException;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

import org.javatuples.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class Main_TCP {

    private class Server
    {
        public ServerSocket serverSocket;
        public LinkedList<Socket> clientSockets;

        @JsonProperty
        public ConcurrentHashMap<String, Pair<String, Integer>> addrMap;

        public Server()
        {
            clientSockets = new LinkedList<Socket>();
            addrMap = new ConcurrentHashMap<>();
            try
            {
                serverSocket = new ServerSocket(0);
                String ip = getIP();
                int port = serverSocket.getLocalPort();
                System.out.println("Server Adress is: " +  ip  + " " + port);
            }
            catch (IOException e)
            {
                System.out.println(e.getMessage());
            }
            Thread ListeningThread = new ListeningThread();
            ListeningThread.start();
        }

        public class ListeningThread extends Thread
        {
            public void run()
            {
                while (true)
                {
                    try
                    {
                        Socket socket = serverSocket.accept();
                        clientSockets.add(socket);

                        BufferedReader r = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        String triplet_json = r.readLine();
                        Triplet<String, String, Integer> triplet = serializer.readValue(triplet_json, Triplet.class);
                        
                        addrMap.putIfAbsent(triplet.getValue0(), new Pair<String, Integer>(triplet.getValue1(), triplet.getValue2()));
                        
                        String addrMap_json = serializer.writeValueAsString(addrMap);

                        for(Socket s : clientSockets)
                        {
                            PrintWriter w = new PrintWriter(s.getOutputStream(),true);
                            w.println(addrMap_json);
                        }
                    }
                    catch (IOException e)
                    {
                        System.out.println(e.getMessage());
                    }
                }
            }
        }
    }
    
    private class Client
    {
        @JsonProperty
        Triplet<String, String, Integer> identity;
        
        public ConcurrentHashMap<String, Pair<String, Integer>> addrMap;

        ServerSocket serverSocket;
        Socket clientSocket;
        Socket receivingSocket;
        Socket sendSocket;

        public Client(String name, String addr, int port)
        {
            try
            {
                serverSocket = new ServerSocket(0);
                String my_ip = getIP();
                int my_port = serverSocket.getLocalPort();
                identity = new Triplet<String,String,Integer>(name, my_ip, my_port);

                clientSocket = new Socket(InetAddress.getByName(addr), port);

            } catch (IOException e)
            {
                System.out.println(e.getMessage());
            }
           
            Thread clienThread = new ClientThread();
            Thread listeningThread = new ListeningThread();
            Thread sendingThread = new SendThread();

            clienThread.start();
            listeningThread.start();
            sendingThread.start();
        }

        public class ClientThread extends Thread
        {
            public void run()
            {
                try
                {
                    String triple_json = serializer.writeValueAsString(identity);
                    PrintWriter w = new PrintWriter(clientSocket.getOutputStream(), true);
                    w.println(triple_json);

                    BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    while(true)
                    {
                        String addrMap_json = reader.readLine();
                        addrMap = serializer.readValue(addrMap_json, new TypeReference<ConcurrentHashMap<String, Pair<String, Integer>>>() {});                       
                    }
                }
                catch (IOException e)
                {
                    System.out.println(e.getMessage());    
                }
            }
        }

        public class ListeningThread extends Thread
        {
            public void run()
            {
                while (true)
                {
                    try
                    {
                        receivingSocket = serverSocket.accept();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(receivingSocket.getInputStream()));
                        System.out.println(reader.readLine());
                        receivingSocket.close();
                    }
                    catch(IOException e)
                    {
                        System.out.println(e.getMessage());
                    }
                }      
            }
        }

        public class SendThread extends Thread
        {
            public void run()
            {
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                while (true)
                {
                    try
                    {
                        String input = br.readLine();
                        if(input.startsWith("send"))
                        {
                            String[] args = input.split(" ", 3);
                            if(args.length != 3)
                            {
                                printHelp();
                                continue;
                            }
                            String targetName = args[1];
                            String message = args[2];
    
                            Pair<String, Integer> target = addrMap.get(targetName);
                            if(target == null)
                            {
                                System.out.println("target " + targetName + " not registered");
                                continue;
                            }
                            sendSocket = new Socket(target.getValue0(), target.getValue1());
                            PrintWriter w = new PrintWriter(sendSocket.getOutputStream(), true);
                            w.println(message);
                            sendSocket.close();
                        }
                        else if (input.startsWith("list"))
                        {
                            addrMap.forEach((k, v) -> 
                            {
                                System.out.println(k + ": " + v.getValue0() + ": " + v.getValue1());
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
        }

        private void printHelp()
        {
            String help =   "available commands are:" + "\n" +
                            "   send <name> <message>" + "\n" +
                            "   list";
            System.out.println(help);
        }
    }
    
    private class TripletDeserializer extends JsonDeserializer<Triplet<String, String, Integer>>
    {
        @Override
        public Triplet<String, String, Integer> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            String name = node.get("value0").asText();
            String addr = node.get("value1").asText();
            int port = node.get("value2").asInt();
            return Triplet.with(name, addr, port);
        }
    }

    private class PairDeserializer extends JsonDeserializer<Pair<String, Integer>>
    {
        @Override
        public Pair<String, Integer> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            String addr = node.get("value0").asText();
            int port = node.get("value1").asInt();
            return Pair.with(addr, port);
        }
    }

    public ObjectMapper serializer;

    public Main_TCP()
    {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Triplet.class, new TripletDeserializer());
        module.addDeserializer(Pair.class, new PairDeserializer());
        serializer = new ObjectMapper();
        serializer.registerModule(module);
        new Server();
    }

    public Main_TCP(String name, String addr, Integer port)
    {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Triplet.class, new TripletDeserializer());
        module.addDeserializer(Pair.class, new PairDeserializer());
        serializer = new ObjectMapper();
        serializer.registerModule(module);
        new Client(name, addr, port);
    }

    private static String getIP()
    {
        try
        {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress("google.com", 80));
            String ip = socket.getLocalAddress().getHostAddress();   
            socket.close();
            return ip;
        }
        catch(IOException e)
        {
            System.out.println(e.getMessage());
        }
        return "127.0.0.1";
    }

    private static void printHelp()
    {
        String help =   "Usage of Main_TCP:\n" + 
                        "   Main_TCP //start server\n" +
                        "   Main_TCP <server_ip> <server_port> //start client and register at server";
        System.out.println(help);
    }

    public static void main(String[] args) throws IOException {
        if(args.length == 0)
        {
            new Main_TCP();
        }
        else if(args.length == 3)
        {
            String name = args[0];
            String ip = args[1];
            int port = Integer.valueOf(args[2]);
            new Main_TCP(name, ip, port);
        }
        else
        {
            printHelp();
        }
    }
}