import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class UDPChat {

    private static final int PACKET_SIZE = 4096;
    private static final Map<String, InetSocketAddress> contacts = new HashMap<>();
    private static String myName;
    private static DatagramSocket socket;
    private static final int DISCOVERY_PORT = 6000;

    private static void fatal ( String comment ) {
        System.out.println(comment);
        System.exit(-1);
    }


    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Usage: java UDPChat <yourName> <yourPort>");
            System.exit(1);
        }

        myName = args[0];
        int myPort = Integer.parseInt(args[1]);
        if(myPort == DISCOVERY_PORT) {
            fatal("Ungültige Port-Nummer. Gegebener Port wird bereits verwendet!");
        }

        socket = new DatagramSocket(myPort);


        // thread -> listening for messages
        Thread listenerThread = new Thread(() -> listen());
        listenerThread.start();

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            String input = br.readLine();
            if (input == null) continue;
            if (input.equalsIgnoreCase("exit")) break;

            if (input.startsWith("register ")) {
                // syntax -> register <name> <ip> <port>
                String[] parts = input.split(" ");
                if (parts.length == 4) {
                    String name = parts[1];
                    String ip = parts[2];
                    int port = Integer.parseInt(parts[3]);
                    InetSocketAddress addr = new InetSocketAddress(ip, port);
                    contacts.put(name, addr);

                    String msg = "REGISTER " + myName + " " + InetAddress.getLocalHost().getHostAddress() + " " + socket.getLocalPort();
                    sendMessage(addr, msg);
                    System.out.println("Registered with " + name);
                }
            } else if (input.startsWith("send ")) {
                // syntax -> send <name> <message>
                int firstSpace = input.indexOf(' ');
                int secondSpace = input.indexOf(' ', firstSpace + 1);
                if (secondSpace == -1) continue;

                String name = input.substring(firstSpace + 1, secondSpace);
                String msg = input.substring(secondSpace + 1);

                if (contacts.containsKey(name)) {
                    String formattedMsg = "MSG " + myName + " " + msg;
                    sendMessage(contacts.get(name), formattedMsg);
                } else {
                    System.out.println("Unknown contact: " + name);
                }
            }
        }

        socket.close();
        System.exit(0);
    }

    private static void listen() {
        byte[] buffer = new byte[PACKET_SIZE];

        while (true) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String msg = new String(packet.getData(), 0, packet.getLength(), "UTF-8");

                // Simple message parsing
                if (msg.startsWith("REGISTER ")) {
                    String[] parts = msg.split(" ");
                    if (parts.length == 4) {
                        String name = parts[1];
                        String ip = parts[2];
                        int port = Integer.parseInt(parts[3]);
                        contacts.put(name, new InetSocketAddress(ip, port));
                        System.out.println("Registered: " + name + " (" + ip + ":" + port + ")");
                    }
                } else if (msg.startsWith("MSG ")) {
                    String[] parts = msg.split(" ", 3);
                    if (parts.length == 3) {
                        System.out.println("\n" + parts[1] + ": " + parts[2]);
                    }
                } else {
                    // pass on package
                    InetAddress senderAddress = packet.getAddress();
                    int senderPort = packet.getPort();

                    if (!senderAddress.equals(InetAddress.getLocalHost()) || senderPort != socket.getLocalPort()) {
                        // pass on contacts
                        for (InetSocketAddress addr : contacts.values()) {
                            if (!addr.getAddress().equals(senderAddress) || addr.getPort() != senderPort) {
                                sendMessage(addr, msg);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Fehler im Listener: " + e.getMessage());
            }
        }
    }

    private static void sendMessage(InetSocketAddress address, String message) {
        try {
            byte[] data = message.getBytes("UTF-8");
            DatagramPacket packet = new DatagramPacket(data, data.length, address.getAddress(), address.getPort());
            socket.send(packet);
        } catch (IOException e) {
            System.out.println("Fehler beim Senden: " + e.getMessage());
        }
    }

}