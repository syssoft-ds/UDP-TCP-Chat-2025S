package oxoo2a;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class Main {

    private static String myName = "NA";
    private static final Map<String, InetSocketAddress> contacts = new HashMap<>();
    private static final int packetSize = 4096;

    private static void fatal(String comment) {
        System.out.println(comment);
        System.exit(-1);
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3)
            fatal("Usage: <myName> -l <port> OR <myName> <ip> <port>");

        myName = args[0];

        if (args[1].equalsIgnoreCase("-l")) {
            int port = Integer.parseInt(args[2]);
            DatagramSocket sharedSocket = new DatagramSocket(port);

            new Thread(() -> {
                try {
                    listenAndTalk(sharedSocket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            startSendLoop(sharedSocket);

        } else {
            String targetIp = args[1];
            int port = Integer.parseInt(args[2]);
            connectAndTalk(targetIp, port);
        }
    }

    private static void listenAndTalk(DatagramSocket s) throws IOException {
        byte[] buffer = new byte[packetSize];
        String line;
        do {
            DatagramPacket p = new DatagramPacket(buffer, buffer.length);
            s.receive(p);
            line = new String(buffer, 0, p.getLength(), "UTF-8");

            if (line.startsWith("register ")) {
                String[] parts = line.split(" ");
                if (parts.length == 4) {
                    String name = parts[1];
                    String ip = parts[2];
                    int port = Integer.parseInt(parts[3]);
                    InetAddress addr = InetAddress.getByName(ip);
                    contacts.put(name, new InetSocketAddress(addr, port));
                    System.out.println(name + " registered from " + ip + ":" + port);
                } else {
                    System.out.println("Invalid register format. Use: register <name> <ip> <port>");
                }
            } else if (line.startsWith("chat:")) {
                String[] parts = line.split(":", 3);
                if (parts.length == 3) {
                    System.out.println("[" + parts[1] + "]: " + parts[2]);
                }
            } else {
                System.out.println(line);
            }

        } while (!line.equalsIgnoreCase("stop"));
        s.close();
    }

    private static void startSendLoop(DatagramSocket s) throws IOException {
        byte[] buffer = new byte[packetSize];
        String line;
        do {
            line = readString();

            if (line.startsWith("register ")) {
                String[] parts = line.split(" ");
                if (parts.length == 4) {
                    String name = parts[1];
                    String ip = parts[2];
                    int port = Integer.parseInt(parts[3]);
                    String regMsg = "register " + myName + " " + InetAddress.getLocalHost().getHostAddress() + " " + s.getLocalPort();
                    DatagramPacket p = new DatagramPacket(regMsg.getBytes("UTF-8"), regMsg.length(), InetAddress.getByName(ip), port);
                    s.send(p);
                    System.out.println("Registration message sent to " + ip + ":" + port);
                } else {
                    System.out.println("Usage: register <name> <ip> <port>");
                }
            } else if (line.startsWith("send ")) {
                String[] parts = line.split(" ", 3);
                if (parts.length == 3) {
                    String to = parts[1];
                    String msg = parts[2];
                    InetSocketAddress recipient = contacts.get(to);
                    if (recipient != null) {
                        String chatMsg = "chat:" + myName + ":" + msg;
                        DatagramPacket p = new DatagramPacket(chatMsg.getBytes("UTF-8"), chatMsg.length(), recipient);
                        s.send(p);
                    } else {
                        System.out.println("Unknown recipient: " + to);
                    }
                } else {
                    System.out.println("Usage: send <name> <message>");
                }
            }

        } while (!line.equalsIgnoreCase("stop"));
        s.close();
    }

    private static void connectAndTalk(String other_host, int other_port) throws IOException {
        DatagramSocket s = new DatagramSocket();
        byte[] buffer = new byte[packetSize];
        String line;
        InetAddress other_address = InetAddress.getByName(other_host);
        do {
            line = readString();
            buffer = line.getBytes("UTF-8");
            DatagramPacket p = new DatagramPacket(buffer, buffer.length, other_address, other_port);
            s.send(p);
        } while (!line.equalsIgnoreCase("stop"));
        s.close();
    }

    private static String readString() {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String input = null;
        try {
            input = br.readLine();
        } catch (Exception e) {
            System.out.println("Exception while reading input: " + e.getMessage());
        }
        return input;
    }
}
