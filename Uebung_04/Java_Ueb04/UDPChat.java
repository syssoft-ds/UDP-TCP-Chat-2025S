import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
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
            if (input.equalsIgnoreCase("exit")) {
                // exit chat --> tell known contacts
                for (Map.Entry<String, InetSocketAddress> entry : contacts.entrySet()) {
                    String exitMessage = "exit " + myName;
                    sendMessage(entry.getValue(), exitMessage);
                }
                break;
            };

            if (input.startsWith("register ")) {
                // syntax -> register <name> <ip> <port>
                String[] parts = input.split(" ");
                if (parts.length == 4) {
                    String name = parts[1];
                    String ip = parts[2];
                    int port = Integer.parseInt(parts[3]);
                    InetSocketAddress addr = new InetSocketAddress(ip, port);
                    contacts.put(name, addr);

                    String msg = "register " + myName + " " + InetAddress.getLocalHost().getHostAddress() + " " + socket.getLocalPort();
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
                    String formattedMsg = "send " + myName + " " + msg;
                    sendMessage(contacts.get(name), formattedMsg);
                } else {
                    System.out.println("Unknown contact: " + name);
                }
            } else if (input.startsWith("send_all ")) {
                // syntax -> send_all <message>
                String msg = input.substring("send_all ".length());
                for (Map.Entry<String, InetSocketAddress> entry : contacts.entrySet()) {
                    String formattedMsg = "send " + myName + " " + msg;
                    sendMessage(entry.getValue(), formattedMsg);
                }
                System.out.println("*Message sent to all known contacts.*");
            } else if (input.startsWith("peers")) {
                // list of all known contacts
                System.out.println("Known contacts: ");
                for (Map.Entry<String, InetSocketAddress> entry : contacts.entrySet()) {
                    System.out.println("\t" + entry.getKey() + " -> " + entry.getValue().getAddress().getHostAddress() + ":" + entry.getValue().getPort());
                }
            } else if (input.equalsIgnoreCase("help")) {
                System.out.println("All commands: \n" +
                        "\t register <name> <ip> <port> - register a new contact.\n" +
                        "\t send <name> <message> - send a message to a specific contact (target).\n" +
                        "\t send_all <message> - send a message to all known contacts.\n" +
                        "\t peers - list of all known contacts.\n" +
                        "\t exit - exit the chat.\n" +
                        "\t help - show this help message.\n"
                    );
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
                if (msg.startsWith("register ")) {
                    String[] parts = msg.split(" ");
                    if (parts.length == 4) {
                        String name = parts[1];
                        // IP und Port aus dem Paket, nicht aus der Nachricht!
                        InetAddress realIp = packet.getAddress();
                        int realPort = packet.getPort();
                        contacts.put(name, new InetSocketAddress(realIp, realPort));
                        System.out.println("Registered: " + name + " (" + realIp.getHostAddress() + ":" + realPort + ")");
                    }
                } else if (msg.startsWith("exit ")) {
                    // syntax -> exit <name>
                    System.out.println("Exiting chat...");
                    String[] parts = msg.split(" ", 2);
                    if (parts.length == 2) {
                        String name = parts[1];
                        if (contacts.containsKey(name)) {
                            contacts.remove(name);
                            System.out.println("contact '" + name + "' left the chat and was erased from this world.");
                        }
                    }
                } else if (msg.startsWith("send ")) {
                    String[] parts = msg.split(" ", 3);
                    if (parts.length == 3) {
                        String senderName = null;
                        InetAddress senderAddr = packet.getAddress();
                        int senderPort = packet.getPort();
                        for (Map.Entry<String, InetSocketAddress> entry : contacts.entrySet()) {
                            InetSocketAddress addr = entry.getValue();
                            if (addr.getAddress().equals(senderAddr) && addr.getPort() == senderPort) {
                                senderName = entry.getKey();
                                break;
                            }
                        }
                        if (senderName == null) {
                            senderName = senderAddr.getHostAddress();
                        }
                        String message = parts[2];
                        if (checkPredefinedQuestions(message, packet.getAddress(), packet.getPort())) {
                            continue;
                        }
                        System.out.println("\n" + senderName + ": " + message);
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

    private static boolean checkPredefinedQuestions(String message, InetAddress addr, int port) throws UnknownHostException {
        String trimmed = message.trim();
        if (trimmed.substring(0).equalsIgnoreCase("Was ist deine IP Adresse?")) {
            String antwort = "Meine IP-Adresse ist: " + addr.getLocalHost().getHostAddress();
            sendMessage(new InetSocketAddress(addr, port), "send " + myName + " " + antwort);
            return true;
        }
        if (trimmed.substring(0).equalsIgnoreCase("Welche Rechnernetze HA war das?")) {
            String antwort = "4. HA, Aufgabe 4";
            sendMessage(new InetSocketAddress(addr, port), "send " + myName + " " + antwort);
            return true;
        }
        if (trimmed.substring(0).equalsIgnoreCase("Wie viel Uhr haben wir?")) {
            String antwort = "Es ist " + java.time.LocalTime.now().withNano(0).toString();
            sendMessage(new InetSocketAddress(addr, port), "send " + myName + " " + antwort);
            return true;
        }
        return false;
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