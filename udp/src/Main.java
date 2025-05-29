import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class Main {

    private static void fatal(String comment) {
        System.out.println(comment);
        System.exit(-1);
    }

    private static final int packetSize = 4096;
    private static final Map<String, InetSocketAddress> clients = new HashMap<>();  //Instanz für bekannte Kontakte
    private static DatagramSocket socket;
    private static String name;

    public static void main(String[] args) throws IOException {
        if (args.length < 2)
            fatal("Usage: \"<netcat> <name> -l <port>\" or \"netcat <name> <ip> <port>\"");
        name = args[0];

        if (args[1].equalsIgnoreCase("-l")) {  //Servermodus
            int port = Integer.parseInt(args[2]);
            socket = new DatagramSocket(port);

            String ip = InetAddress.getLocalHost().getHostAddress();
            System.out.println("Hallo, ich bin " + name + " mit IP " + ip + " und Port " + port);

            listenAndTalk();
        } else {                                         //Clientmodus (peer to peer)
            String ip = InetAddress.getByName(args[1]).getHostAddress();
            int port = Integer.parseInt(args[2]);
            socket = new DatagramSocket();

            sendRegistration(ip, port); //Registrierung beim Ziel
            listenAndTalk();
        }
    }

    // Starte Listener-Thread, der Benutzereingaben verarbeitet
    private static void listenAndTalk() throws IOException {
        Thread listener = new Thread(() -> {
            byte[] buffer = new byte[packetSize];
            while (true) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String msg = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
                    handleMessage(msg, packet.getAddress(), packet.getPort()); // Methode für die Bearbeitung der Nachrichten
                } catch (IOException e) {}
            }
        });
        listener.setDaemon(true);
        listener.start();

        System.out.println("Tippen register, send oder stop.");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = reader.readLine()) != null && !line.equalsIgnoreCase("stop")) {
            if (line.startsWith("register ")) {
                String[] parts = line.split(" ");
                if (parts.length == 3) {
                    sendRegistration(parts[1], Integer.parseInt(parts[2]));
                } else {
                    System.out.println("Befehl: register <ip> <port>");
                }
            } else if (line.startsWith("send ")) {
                String[] parts = line.split(" ", 3);
                if (parts.length == 3) {
                    String clientName = parts[1];
                    String message = parts[2];
                    InetSocketAddress adresse = clients.get(clientName);
                    if (adresse != null) {
                        sendMessage("msg:" + name + ":" + message, adresse.getAddress(), adresse.getPort());
                    } else {
                        System.out.println("Kontakt ist unbekannt: " + clientName);
                    }
                } else {
                    System.out.println("Befehl: send <name> <message>");
                }
            }
        }
        socket.close();
    }

    private static void handleMessage(String message, InetAddress addresse, int port) {
        if (message.startsWith("register:")) {
            String[] parts = message.split(":");
            if (parts.length == 4) {
                String name = parts[1];
                String ip = parts[2];
                int clientPort = Integer.parseInt(parts[3]);
                InetSocketAddress newClient = new InetSocketAddress(ip, clientPort);

                boolean isNew = !clients.containsKey(name);
                clients.put(name, newClient);

                if (isNew) {
                    System.out.println("Neuer Kontakt: " + name + " " + ip + ":" + clientPort);

                }
            }
        } else if (message.startsWith("msg:")) {
            String[] parts = message.split(":", 3);
            if (parts.length == 3) {
                System.out.println(parts[1] + ": " + parts[2]);
            }
        }
    }

    private static void sendRegistration(String ipAdresse, int port) throws IOException {
        InetAddress adresse = InetAddress.getByName(ipAdresse);
        String ip = InetAddress.getLocalHost().getHostAddress();
        int myPort = socket.getLocalPort();
        String message = "register:" + name + ":" + ip + ":" + myPort;

        sendMessage(message, adresse, port);
        System.out.println("Die Registrierung wurde verschickt: " + ipAdresse + ", " + port);
    }

    private static void sendMessage(String msg, InetAddress address, int port) throws IOException {
        byte[] data = msg.getBytes("UTF-8");
        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        socket.send(packet);
    }
}
