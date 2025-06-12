package Uebung3.Java;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class UDP_Chat {

    public String clientName = "Client";
    public String clientIP;
    public int clientPort;
    public List<ClientInfo> otherClients = new ArrayList<>();

    private static void fatal ( String comment ) {
        System.out.println(comment);
        System.exit(-1);
    }

    // ************************************************************************
    // MAIN
    // ************************************************************************
    public void main(String[] args) throws IOException {
        // "Usage: name"
        if (args.length != 2)
            fatal("Usage: <name> <port>");

        clientName = args[0];
        int port = Integer.parseInt(args[1]);

        try (DatagramSocket socket = new DatagramSocket(port)) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            clientIP = socket.getLocalAddress().getHostAddress();
            socket.disconnect();
            clientPort = socket.getLocalPort();
        }

        System.out.printf("UDP Chat client started with name '%s', IP '%s', Port '%d'\n", clientName, clientIP, clientPort);

        Thread t = new Thread(() -> send());
        t.start();
        Thread t2 = new Thread(() -> listen(port));
        t2.start();
    }

    private static final int packetSize = 4096;

    // ************************************************************************
    // listen
    // ************************************************************************


    private void listen(int port) {
        try {
            DatagramSocket s = new DatagramSocket(port);
            byte[] buffer = new byte[packetSize];
            String line;
            do {
                DatagramPacket p = new DatagramPacket(buffer, buffer.length);
                s.receive(p);
                line = new String(buffer, 0, p.getLength(), "UTF-8");
                System.out.println(line);

                if (line.startsWith("Hallo, hier ist ")) {
                    String[] parts = line.split(", ");
                    String name = parts[1].split(" ")[2];
                    String ip = parts[2].split(" ")[4];
                    int portNumber = Integer.parseInt(parts[2].split(" ")[11]);
                    addClient(name, ip, portNumber);
                    System.out.println("Connected to: " + name + " (" + ip + ":" + portNumber + ")");
                }

            } while (!line.equalsIgnoreCase("stop"));
            s.close();
        } catch (IOException e) {
            System.out.println("Error in listening: " + e.getMessage());
        }
    }


    // ************************************************************************
    // send
    // ************************************************************************
    private void sendConnectMessage ( String other_host, int other_port ) throws IOException {
        InetAddress other_address = InetAddress.getByName(other_host);
        DatagramSocket s = new DatagramSocket();
        byte[] buffer = new byte[packetSize];
        String line;
        line = String.format("Hallo, hier ist %s, meine IP-Adresse ist die %s und du kannst mich unter Port-Nummer %d erreichen.",
                clientName, clientIP, clientPort);
        buffer = line.getBytes("UTF-8");
        DatagramPacket p = new DatagramPacket(buffer,buffer.length,other_address,other_port);
        s.send(p);

        s.close();
    }

    private void send() {
        String console;
        do {
             console = readString();
            if (console.startsWith("connect")) {
                String[] parts = console.split(" ");
                if (parts.length < 3 || parts.length > 4)  {
                    System.out.println("Usage: connect <ip> <port> (optional: <name>)");
                    return;
                }
                String ip = parts[1];
                int port = Integer.parseInt(parts[2]);
                try {
                    sendConnectMessage(ip, port);
                    addClient(parts[3], ip, port);
                    System.out.println("Connected to " + parts[3] + " at " + ip + ":" + port);
                } catch (IOException e) {
                    System.out.println("Error connecting: " + e.getMessage());
                }
            } else if (console.startsWith("send")) {
                String[] parts = console.split(" ", 3);
                if (parts.length < 3) {
                    System.out.println("Usage: send <name> <message>");
                    return;
                }
                String targetName = parts[1];
                String message = parts[2];

                for (ClientInfo client : otherClients) {
                    if (client.name.equals(targetName)) {
                        try {
                            InetAddress other_address = InetAddress.getByName(client.ip);
                            DatagramSocket s = new DatagramSocket();
                            byte[] buffer = message.getBytes("UTF-8");
                            DatagramPacket p = new DatagramPacket(buffer, buffer.length, other_address, client.port);
                            s.send(p);
                            s.close();
                            System.out.println("Message sent to " + targetName);
                        } catch (IOException e) {
                            System.out.println("Error sending message to " + client.name + ": " + e.getMessage());
                        }
                        return;
                    }
                }
                System.out.println("Client with name " + targetName + " not found.");
            } else {
                System.out.println("Unknown command. Use 'connect <ip> <port>' to connect.");
            }
        }while (!console.equalsIgnoreCase("stop"));
    }

    private String readString() {
        BufferedReader br = null;
        boolean again = false;
        String input = null;
        do {
            // System.out.print("Input: ");
            try {
                if (br == null)
                    br = new BufferedReader(new InputStreamReader(System.in));
                input = br.readLine();
            }
            catch (Exception e) {
                System.out.printf("Exception: %s\n",e.getMessage());
                again = true;
            }
        } while (again);
        return input;
    }

    private BufferedReader br = null;


    private class ClientInfo {
        public String name;
        public String ip;
        public int port;

        public ClientInfo(String name, String ip, int port) {
            this.name = name;
            this.ip = ip;
            this.port = port;
        }

        @Override
        public String toString() {
            return "Name: " + name + ", IP: " + ip + ", Port: " + port;
        }
    }

    public void addClient(String name, String ip, int port) {
        otherClients.add(new ClientInfo(name, ip, port));
    }
}
