package oxoo2a;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class Main {
    // Name dieser Chat-Instanz
    private static String myName;
    private static DatagramSocket s = null;


    // Liste bekannter Kontakte: Name -> Adresse
    private static final Map<String, InetSocketAddress> contacts = new HashMap<>();

    private static void fatal ( String comment ) {
        System.out.println(comment);
        System.exit(-1);
    }

    // ************************************************************************
    // MAIN
    // ************************************************************************
    public static void main(String[] args) throws IOException {
        if (args.length < 3)
            fatal("Usage:\n  <name> -l <port>\n  <name> <ip> <port>");
        myName = args[0];
        int port = Integer.parseInt(args[2]);
        if (args[1].equalsIgnoreCase("-l"))
            listenAndTalk(port);
        else
            connectAndTalk(args[1],port);
    }

    private static final int packetSize = 4096;

    // ************************************************************************
    // listenAndTalk
    // ************************************************************************
    private static void listenAndTalk ( int port ) throws IOException  {
        s = new DatagramSocket(port);
        // Thread zum Empfang von Nachrichten
        Thread receiver = new Thread(() -> {
            try {
                while (true) {
                    byte[] buffer = new byte[packetSize];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    s.receive(packet);
                    String message = new String(buffer, 0, packet.getLength(), "UTF-8");
                    processIncomingMessage(message, packet.getAddress(), packet.getPort());
                }
            } catch (IOException e) {
                System.out.println("Fehler beim Empfangen: " + e.getMessage());
            }
        });
        receiver.start();

        // Hauptthread liest Eingaben und sendet Nachrichten
        while (true) {
            String line = readString();

            if (line.equalsIgnoreCase("stop")) {
                s.close();
                break;
            }

            if (line.startsWith("send ")) {
                String[] parts = line.split(" ", 3);
                if (parts.length < 3) {
                    System.out.println("Ungültiger Befehl. Format: send <name> <message>");
                    continue;
                }

                String recipient = parts[1];
                String msg = parts[2];
                InetSocketAddress recipientAddress = contacts.get(recipient);

                if (recipientAddress == null) {
                    System.out.println("Empfänger '" + recipient + "' nicht bekannt.");
                    continue;
                }

                String formattedMessage = "msg " + myName + " " + msg;
                sendMessage(s, formattedMessage,
                        recipientAddress.getAddress(), recipientAddress.getPort());
            } else {
                System.out.println("Unbekannter Befehl. Benutze: send <name> <message>");
            }
        }
    }

    // Verarbeitung eingehender Nachrichten
    private static void processIncomingMessage(String message, InetAddress sender, int port) {
        if (message.startsWith("hello")) {
            // Format: hello <name> <ip> <port>
            String[] parts = message.split(" ");
            if (parts.length == 4) {
                String name = parts[1];
                String ip = parts[2];
                int theirPort = Integer.parseInt(parts[3]);
                InetSocketAddress newContact = new InetSocketAddress(ip, theirPort);

                // Nur hinzufügen und antworten, wenn Kontakt neu oder Adresse anders
                if (!contacts.containsKey(name) || !contacts.get(name).equals(newContact)) {
                    contacts.put(name, newContact);
                    System.out.println("Registriert: " + name + " @ " + ip + ":" + theirPort);

                    // Gegenseitige Registrierung (Antwort mit hello)
                    try {
                        String reply = "hello " + myName + " " +
                                InetAddress.getLocalHost().getHostAddress() + " " + s.getLocalPort();
                        sendMessage(s, reply, sender, port);
                    } catch (IOException e) {
                        System.out.println("Fehler beim Senden der Gegengruß-Nachricht: " + e.getMessage());
                    }
                }
                // Sonst: Kontakt bekannt, keine Antwort senden -> Endlosschleife vermeiden
            }
        } else if (message.startsWith("msg")) {
            // Format: msg <sender_name> <message>
            String[] parts = message.split(" ", 3);
            if (parts.length == 3) {
                String from = parts[1];
                String text = parts[2];
                System.out.println("Nachricht von " + from + ": " + text);
            }
        } else {
            System.out.println("Unbekannte Nachricht: " + message);
        }
    }


    // ************************************************************************
    // connectAndTalk
    // ************************************************************************
    private static void connectAndTalk ( String other_host, int other_port ) throws IOException {
        InetAddress other_address = InetAddress.getByName(other_host);
        s = new DatagramSocket();

        // Registrierung an den Partner senden
        String helloMessage = "hello " + myName + " " +
                InetAddress.getLocalHost().getHostAddress() + " " + s.getLocalPort();
        sendMessage(s, helloMessage, other_address, other_port);

        // Thread zum Empfang von Nachrichten
        Thread receiver = new Thread(() -> {
            byte[] buffer = new byte[packetSize];
            try {
                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    s.receive(packet);
                    String message = new String(buffer, 0, packet.getLength(), "UTF-8");
                    processIncomingMessage(message, packet.getAddress(), packet.getPort());
                }
            } catch (IOException e) {
                System.out.println("Fehler beim Empfangen: " + e.getMessage());
            }
        });
        receiver.start();

        // Benutzer-Eingaben lesen und Nachrichten senden
        while (true) {
            String line = readString();

            if (line.equalsIgnoreCase("stop")) {
                s.close();
                break;
            }

            if (line.startsWith("send ")) {
                String[] parts = line.split(" ", 3);
                if (parts.length < 3) {
                    System.out.println("Ungültiger Befehl. Format: send <name> <message>");
                    continue;
                }

                String recipient = parts[1];
                String msg = parts[2];
                InetSocketAddress recipientAddress = contacts.get(recipient);

                if (recipientAddress == null) {
                    System.out.println("Empfänger '" + recipient + "' nicht bekannt.");
                    continue;
                }

                String formattedMessage = "msg " + myName + " " + msg;
                sendMessage(s, formattedMessage,
                        recipientAddress.getAddress(), recipientAddress.getPort());
            } else {
                System.out.println("Unbekannter Befehl. Benutze: send <name> <message>");
            }
        }
    }


    // Nachricht als UDP-Paket senden
    private static void sendMessage(DatagramSocket socket, String message,
                                    InetAddress address, int port) throws IOException {
        byte[] buffer = message.getBytes("UTF-8");
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
        socket.send(packet);
    }


    private static String readString () {
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
}
