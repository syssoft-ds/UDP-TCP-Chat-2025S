package Uebung4;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class TCP_Chat_Client {
    private static String name;
    private static String serverIP;
    private static int serverPort;
    private static String clientList;


    private static void fatal(String input) {
        System.err.println(input);
        System.exit(-1);
    }

    public static boolean isIP(String ip) { // Checks if String is valid IPv4 address
        return UDP_Chat.isIP(ip);
    }

    public static boolean isPort(String port) {
        return UDP_Chat.isPort(port);
    }

    public static void main(String[] args) {

        // Handling arguments, checking validity
        if (args.length != 3) {
            fatal("Arguments: \"<server ip address> <server port number> <client name>\"");
        }
        if (!isIP(args[0])) {
            fatal("Invalid IP address");
        } else {
            serverIP = args[0];
        }
        if (!isPort(args[1])) {
            fatal("Invalid port number");
        } else {
            serverPort = Integer.parseInt(args[1]);
        }
        name = args[2];

        try (Socket socket = new Socket(serverIP, serverPort);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in))) {
            // closes automatically

            // Verwendung der Methode
            new Thread(() -> handleIncomingMessages(socket)).start();

            register(socket);

            System.out.println(name + " is connected to Server at IP " + serverIP + " on port " + serverPort + ".\nUse \"send <client name> <message>\" to send a message to a client. \nUse \"sendall <message>\" to send a message to all clients.\nUse \"exit\" to close the connection. \nUse \"ask <client name>\" to ask a client a question.\nUse \"list\" to list all connected clients.");

            handleUserInput(socket, stdIn);

        } catch (UnknownHostException e) {
            fatal("Unknown Server with IP " + serverIP);
        } catch (IOException e) {
            fatal("Unable to send message.");
        }
        System.exit(0);
    }

    private static void handleUserInput(Socket socket, BufferedReader stdIn) {
        try {
            String userInput;
            while ((userInput = stdIn.readLine()) != null) {
                String[] parts = userInput.split(" ", 3);
                if (parts[0].equalsIgnoreCase("send") && parts.length == 3) {
                    send(userInput, socket, 3);
                } else if (parts[0].equalsIgnoreCase("sendall") && parts.length == 2) {
                    send("", socket, 5); //Clientliste aktualisieren
                    try {
                        Thread.sleep(1000); // Pause für 300 Millisekunden, damit Antworten auf "list" verarbeitet werden können
                    } catch (InterruptedException e) {
                        e.printStackTrace(); // Fehlerbehandlung, falls der Thread unterbrochen wird
                    }
                    String payload = clientList.split(",").length + "," + clientList + ","+ name + "," + parts[1];
                    send(payload, socket, 4);
                } else if (parts[0].equalsIgnoreCase("ask") && parts.length == 3) {
                    send(userInput, socket, 6);
                } else if (parts[0].equalsIgnoreCase("exit")) {
                    System.out.println("Closing connection...");
                    try {
                        socket.close();
                    } catch (IOException e) {
                        System.err.println("Error closing socket: " + e.getMessage());
                    }
                    System.out.println("Connection closed.");
                    break;
                } else if (parts[0].equalsIgnoreCase("list")) {
                    send("", socket, 5);
                } else {
                    System.err.println("Unknown command. Use \"send <client name> <message>\" to send a message to a client, \"sendall <message>\" to send a message to all clients, or \"ask <client name>\" to ask a client a question.");
                }
            }
        } catch (IOException e) {
            fatal("Unable to send message.");
        }
    }

    private static void handleIncomingMessages(Socket socket) {
        try {
            InputStream inputStream = socket.getInputStream();
            while (!socket.isClosed()) {
                byte[] header = new byte[5]; // 1 Byte für Typ + 4 Bytes für Länge
                int bytesRead = inputStream.read(header);

                if (bytesRead == header.length) {
                    ByteBuffer buffer = ByteBuffer.wrap(header);
                    byte type = buffer.get(); // Typ (1 Byte)
                    int length = buffer.getInt(); // Länge (4 Bytes)

                    byte[] payload = new byte[length];
                    inputStream.read(payload); // Payload lesen

                    String message = new String(payload, StandardCharsets.UTF_8);
                    processMessage(type, message, socket);
                } else if (bytesRead == -1) {
                    System.out.println("Server hat die Verbindung geschlossen.");
                    break;
                } else {
                    System.out.println("Ungültiger Header empfangen.");
                }
            }
        } catch (IOException e) {
            fatal("Unable to get message from Server.");
        }
    }

    private static void processMessage(byte type, String message, Socket socket) {
        if (type == 3 || type == 4) {
            String[] split = message.split(",", 2);
            if (split.length == 2) {
                System.out.println("Nachricht von " + split[0] + ": " + split[1]);
            } else {
                System.out.println("Nachricht: " + split[0]);
            }
        } else if (type == 5) {
            clientList = message;
            System.out.println("Alle Clients: " + clientList);
        } else if (type == 6) {
            String[] split = message.split(",", 2);
            System.out.println("Frage von " + split[0] + ": " + split[1]);
            String response = respondToQuestion(split[1]);
            send("send " + split[0] + " " + response, socket, 3);
        } else {
            System.out.println("Empfangene Nachricht: Typ=" + type + ", Länge=" + message.length() + ", Nachricht=" + message);
        }
    }


    private static void register(Socket socket) {
        try {
            // Nachrichtentyp (1 Byte)
            byte type = 1; // Typ 1 für Registrierung

            // Payload erstellen: endpoint.name,endpoint.ip,endpoint.port
            String payload = name;
            byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

            // Länge der Nachricht (4 Bytes, big endian)
            int messageLength = 1 + 4 + payloadBytes.length; // Typ (1 Byte) + Länge (4 Bytes) + Payload
            byte[] lengthBytes = ByteBuffer.allocate(4).putInt(payloadBytes.length).array();

            // Nachricht zusammenstellen
            ByteBuffer messageBuffer = ByteBuffer.allocate(messageLength);
            messageBuffer.put(type); // Typ
            messageBuffer.put(lengthBytes); // Länge
            messageBuffer.put(payloadBytes); // Payload

            // Bytes über OutputStream senden
            OutputStream out = socket.getOutputStream();
            out.write(messageBuffer.array());
            out.flush();

            System.out.println("Registered as " + name + ".");
        } catch (IOException e) {
            System.err.println("Error during registration: " + e.getMessage());
        }
    }

    private static void send(String userInput, Socket socket, int Type) {
        try {
            // Nachrichtentyp (1 Byte)
            byte type = (byte) Type; // Typ 3 für Message oder 4 für MessageAll

            // Payload erstellen: endpoint.name,endpoint.ip,endpoint.port
            String payload;
            if(type != 4){
                payload = userInput.substring(userInput.indexOf(" ") + 1).replaceFirst(" ", ",");
            }else{
                payload = userInput.substring(userInput.indexOf(" ") + 1);
            }

            if (type == 3 || type == 6) {
                int commaIndex = payload.indexOf(",");
                if (commaIndex != -1) {
                    payload = payload.substring(0, commaIndex + 1) + name + "," + payload.substring(commaIndex + 1);
                }
            }

            byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

            // Länge der Nachricht (4 Bytes, big endian)
            int messageLength = 1 + 4 + payloadBytes.length; // Typ (1 Byte) + Länge (4 Bytes) + Payload
            byte[] lengthBytes = ByteBuffer.allocate(4).putInt(payloadBytes.length).array();

            // Nachricht zusammenstellen
            ByteBuffer messageBuffer = ByteBuffer.allocate(messageLength);
            messageBuffer.put(type); // Typ
            messageBuffer.put(lengthBytes); // Länge
            messageBuffer.put(payloadBytes); // Payload

            // Bytes über OutputStream senden
            OutputStream out = socket.getOutputStream();
            out.write(messageBuffer.array());
            out.flush();
            System.out.println("Send Message");
        } catch (IOException e) {
            System.err.println("Error during sending Message: " + e.getMessage());
        }
    }

    private static String respondToQuestion(String payload) {
        if (payload.equalsIgnoreCase("Sind Kartoffeln ein Gericht?")) {
            return "Ja, Kartoffeln können als Beilage oder Hauptgericht serviert werden.";
        } else if (payload.equalsIgnoreCase("Macht diese Aufgabe Spaß?")) {
            return "Ja, es ist eine interessante Aufgabe!";
        } else if (payload.equalsIgnoreCase("Ist Rhababerkuchen lecker?")) {
            return "Ja, Rhababerkuchen ist sehr lecker!";
        } else if (payload.equalsIgnoreCase("Was ist deine MAC-Adresse?")) {
            try {
                byte[] macBytes = NetworkInterface.getByInetAddress(InetAddress.getLocalHost()).getHardwareAddress();
                if (macBytes != null) {
                    StringBuilder macAddressBuilder = new StringBuilder();
                    for (int i = 0; i < macBytes.length; i++) {
                        macAddressBuilder.append(String.format("%02X", macBytes[i]));
                        if (i < macBytes.length - 1) {
                            macAddressBuilder.append(":");
                        }
                    }
                    return "Meine MAC-Adresse ist: " + macAddressBuilder.toString();
                } else {
                    return "MAC-Adresse konnte nicht abgerufen werden.";
                }
            } catch (SocketException | UnknownHostException e) {
                return "Fehler beim Abrufen der MAC-Adresse: " + e.getMessage();
            }
        } else {
            return "Diese Frage kann ich nicht beantworten.";
        }
    }

}