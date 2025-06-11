package Uebung4;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class UDP_Chat {

    private static int port;
    private static String name;
    private static final Map<String, ClientInfo> clients = new HashMap<>();

    private record ClientInfo(String ip, int port) { }

    private static void fatal(String input) {
        System.err.println(input);
        System.exit(-1);
    }

    public static boolean isIP(String ip) { // Checks if String is valid IPv4 address
        String[] parts = ip.split("\\."); // Split by dot
        if (parts.length != 4) { return false; } // Must be 4 chunks
        for (String p : parts) { // Check if numbers are valid
            try {
                int number = Integer.parseInt(p);
                if (number < 0 || number > 255) { return false; }
            } catch (NumberFormatException e) { return false; }
        }
        return true;
    }

    public static boolean isPort(String port) {
        try {
            int number = Integer.parseInt(port);
            if (number < 0 || number > 65535) { return false; }
        } catch (NumberFormatException e) { return false; }
        return true;
    }

    public static void main(String[] args) {

        // Handling arguments, checking validity
        if (args.length != 2) {
            fatal("Arguments: \"<port number> <client name>\"");
        }
        if (!isPort(args[0])) {
            fatal("Invalid port number");
        } else {
            port = Integer.parseInt(args[0]);
        }
        name = args[1];

        System.out.println(name + " (Port: " + port + ") is here, looking around.\nUse \"register <ip address> <port number>\" to contact another client.\nUse \"send <registered client name> <message>\" to message them.\nUse \"sendall\" to message all known clients.\nUse \"ask <registered client name> <question>\" to ask someone a question.\nUse \"questions\" to see all available questions.\nUse \"quit\" to exit program.");
        // Start a new thread to listen for messages
        new Thread(() -> receiveLines(port)).start();

        // Main thread continues to process user input
        try(BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) { // closes automatically
            String input;
            while (!(input = br.readLine()).equalsIgnoreCase("quit")) {
                String[] parts = input.split(" ");
                if (parts[0].equalsIgnoreCase("register") && parts.length == 3 && isPort(parts[2])) {
                    register(parts[1], Integer.parseInt(parts[2]));
                } else if (parts[0].equalsIgnoreCase("send")) {
                    String receiver = parts[1];
                    ClientInfo receiverInfo = clients.get(receiver);
                    if (receiverInfo != null) {
                        String message = input.substring(input.indexOf(receiver) + receiver.length()).trim();
                        sendLines(receiverInfo.ip, receiverInfo.port, message, (byte) 3);
                        System.out.println("Sent \"" + message + "\" to " + receiver + ".");
                    } else {
                        System.err.println("Unknown client \"" + receiver + "\".");
                    }
                }else if (parts[0].equalsIgnoreCase("sendall")) {
                    String message = input.substring(input.indexOf("sendall") + 7).trim();
                    sendLinesAll(message);
                    System.out.println("Sent \"" + message + "\" to " + " to all known clients.");
                } else if (parts[0].equalsIgnoreCase("ask")) {
                    String receiver = parts[1];
                    ClientInfo receiverInfo = clients.get(receiver);
                    if (receiverInfo != null) {
                        String message = input.substring(input.indexOf(receiver) + receiver.length()).trim();
                        sendLines(receiverInfo.ip, receiverInfo.port, message, (byte) 6);
                        System.out.println("Sent \"" + message + "\" to " + receiver + ".");
                    } else {
                        System.err.println("Unknown client \"" + receiver + "\".");
                    }
                }else if (parts[0].equalsIgnoreCase("questions")) {
                    System.out.println("Available questions: \n1. Sind Kartoffeln ein Gericht?\n2. Macht diese Aufgabe Spaß?\n3. Ist Rhababerkuchen lecker?\n4. Was ist deine MAC-Adresse?");
                } else {
                    System.err.println("Unknown command.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    private static final int packetSize = 4096;

    private static void receiveLines(int port) {
        try(DatagramSocket s = new DatagramSocket(port)) { // closes automatically
            byte[] buffer = new byte[packetSize];
            String payload;
            do {
                DatagramPacket p = new DatagramPacket(buffer, buffer.length);
                s.receive(p);
                ByteBuffer messageBuffer = ByteBuffer.wrap(p.getData(), 0, p.getLength());

                // Nachrichtentyp (1 Byte)
                byte type = messageBuffer.get();

                // Länge der Nachricht (4 Bytes, Big Endian)
                int length = messageBuffer.getInt();

                // Payload (Rest der Nachricht)
                byte[] payloadBytes = new byte[length];
                messageBuffer.get(payloadBytes);
                payload = new String(payloadBytes, StandardCharsets.UTF_8);

                if(type == 3) { // Message type
                    String[] parts = payload.split(",",2);
                    if (parts.length == 2) {
                        String sender = parts[0];
                        String message = parts[1];
                        System.out.println("Nachricht von " + sender + ": " + message);
                    } else {
                        System.err.println("Invalid message format.");
                    }
                } else if (type == 2) { // Registration type
                    String[] parts = payload.split(",");
                    if (parts.length == 3) {
                        String name = parts[0];
                        String ip = parts[1];
                        String portString = parts[2];
                        if (isIP(ip) && isPort(portString)) {
                            int clientPort = Integer.parseInt(portString);
                            clients.put(name, new ClientInfo(ip, clientPort));
                            System.out.println("Registered client: " + name + " at " + ip + ":" + clientPort);
                        } else {
                            System.err.println("Invalid registration data for " + name);
                        }
                    } else {
                        System.err.println("Invalid registration format.");
                    }
                } else if (type == 6) { // Question type
                    String[] parts = payload.split(",");
                    if (parts.length == 2) {
                        String sender = parts[0];
                        String question = parts[1];
                        System.out.println("Question from " + sender + ": " + question);

                        if( question.equalsIgnoreCase("Sind Kartoffeln ein Gericht?")) {
                            ClientInfo receiverInfo = clients.get(sender);
                            sendLines(receiverInfo.ip, receiverInfo.port, "Ja, Kartoffeln können als Beilage oder Hauptgericht serviert werden.", (byte) 3);
                        } else if (question.equalsIgnoreCase("Macht diese Aufgabe Spaß?")) {
                            ClientInfo receiverInfo = clients.get(sender);
                            sendLines(receiverInfo.ip, receiverInfo.port, "Ja, es ist eine interessante Aufgabe!", (byte) 3);
                        } else if (question.equalsIgnoreCase("Ist Rhababerkuchen lecker?")) {
                            ClientInfo receiverInfo = clients.get(sender);
                            sendLines(receiverInfo.ip, receiverInfo.port, "Ja, Rhababerkuchen ist sehr lecker!", (byte) 3);
                        } else if (question.equalsIgnoreCase("Was ist deine MAC-Adresse?")) {
                            try {
                                ClientInfo receiverInfo = clients.get(sender);
                                byte[] macBytes = NetworkInterface.getByInetAddress(InetAddress.getLocalHost()).getHardwareAddress();
                                if (macBytes != null) {
                                    StringBuilder macAddressBuilder = new StringBuilder();
                                    for (int i = 0; i < macBytes.length; i++) {
                                        macAddressBuilder.append(String.format("%02X", macBytes[i]));
                                        if (i < macBytes.length - 1) {
                                            macAddressBuilder.append(":");
                                        }
                                    }
                                    String macAddress = macAddressBuilder.toString();
                                    sendLines(receiverInfo.ip, receiverInfo.port, "Meine MAC-Adresse ist: " + macAddress, (byte) 3);
                                } else {
                                    System.err.println("MAC-Adresse konnte nicht abgerufen werden.");
                                }
                            } catch (SocketException | UnknownHostException e) {
                                System.err.println("Fehler beim Abrufen der MAC-Adresse: " + e.getMessage());
                            }
                        } else {
                            ClientInfo receiverInfo = clients.get(sender);
                            sendLines(receiverInfo.ip, receiverInfo.port, "Diese Frage kann ich nicht beantworten.", (byte) 3);
                        }
                    } else {
                        System.err.println("Invalid question format.");
                    }
                } else {
                    System.err.println("Unknown message type: " + type);
                }

            } while (!payload.equalsIgnoreCase("quit"));
        } catch (IOException e) {
            System.err.println("Unable to receive message on port \"" + port + "\".");
        }
    }

    private static void sendLines(String friend, int friends_port, String message, byte Typ) {
        try (DatagramSocket socket = new DatagramSocket()) {
            // Nachrichtentyp (1 Byte)
            byte typ = Typ;
            // Payload erstellen: endpoint.name,endpoint.ip,endpoint.port
            String payload = String.format("%s,%s", name, message);
            byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

            // Länge der Nachricht (4 Bytes, big endian)
            int messageLength =  1 + 4 + payloadBytes.length; // type + length + Payload
            byte[] lengthBytes = ByteBuffer.allocate(4).putInt(payloadBytes.length).array();

            // Nachricht zusammenstellen
            ByteBuffer messageBuffer = ByteBuffer.allocate(messageLength);
            messageBuffer.put(typ); // Typ
            messageBuffer.put(lengthBytes); // Länge
            messageBuffer.put(payloadBytes); // Payload

            // Nachricht senden
            InetAddress friendAddress = InetAddress.getByName(friend);
            DatagramPacket packet = new DatagramPacket(messageBuffer.array(), messageBuffer.array().length, friendAddress, friends_port);
            socket.send(packet);

            System.out.println("Message sent.");
        } catch (IOException e) {
            System.err.println("Unable to send message to \"" + friend + "\".");
        }
    }

    private static void sendLinesAll(String message) {
        for (Map.Entry<String, ClientInfo> entry : clients.entrySet()) {
            String clientName = entry.getKey();
            ClientInfo clientInfo = entry.getValue();

            try (DatagramSocket socket = new DatagramSocket()) {
                // Nachrichtentyp (1 Byte)
                byte type = 3;
                // Payload erstellen: endpoint.name,endpoint.ip,endpoint.port
                String payload = String.format("%s,%s", name, message);
                byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

                // Länge der Nachricht (4 Bytes, big endian)
                int messageLength = 1 + 4 + payloadBytes.length; // Typ (1 Byte) + Länge (4 Bytes) + Payload
                byte[] lengthBytes = ByteBuffer.allocate(4).putInt(payloadBytes.length).array();

                // Nachricht zusammenstellen
                ByteBuffer messageBuffer = ByteBuffer.allocate(messageLength);
                messageBuffer.put(type); // Typ
                messageBuffer.put(lengthBytes); // Länge
                messageBuffer.put(payloadBytes); // Payload

                // Nachricht senden
                InetAddress friendAddress = InetAddress.getByName(clientInfo.ip);
                DatagramPacket packet = new DatagramPacket(messageBuffer.array(), messageBuffer.array().length, friendAddress, clientInfo.port);
                socket.send(packet);

                System.out.println("Message sent.");
            } catch (IOException e) {
                System.err.println("Unable to send message to \"" + clientName + "\".");
            }
        }
    }


    private static void register(String friend, int friends_port) {
        try (DatagramSocket socket = new DatagramSocket()) {
            // Nachrichtentyp (1 Byte)
            byte type = 2;

            // Payload erstellen: endpoint.name,endpoint.ip,endpoint.port
            String payload = String.format("%s,%s,%d", name, InetAddress.getLocalHost().getHostAddress(), port);
            byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

            // Länge der Nachricht (4 Bytes, big endian)
            int messageLength = 1 + 4 + payloadBytes.length; // Typ (1 Byte) + Länge (4 Bytes) + Payload
            byte[] lengthBytes = ByteBuffer.allocate(4).putInt(payloadBytes.length).array();

            // Nachricht zusammenstellen
            ByteBuffer messageBuffer = ByteBuffer.allocate(messageLength);
            messageBuffer.put(type); // Typ
            messageBuffer.put(lengthBytes); // Länge
            messageBuffer.put(payloadBytes); // Payload

            // Nachricht senden
            InetAddress friendAddress = InetAddress.getByName(friend);
            DatagramPacket packet = new DatagramPacket(messageBuffer.array(), messageBuffer.array().length, friendAddress, friends_port);
            socket.send(packet);

            System.out.println("Registrierungsnachricht gesendet.");
        } catch (IOException e) {
            System.err.println("Fehler beim Senden der Registrierungsnachricht: " + e.getMessage());
        }
    }




}