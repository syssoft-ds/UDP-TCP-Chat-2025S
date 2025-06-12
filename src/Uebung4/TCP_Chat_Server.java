package Uebung4;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class TCP_Chat_Server {
    private static final int port = 1444;
    private static final Map<String, ClientInfo> clients = new HashMap<>();

    private static class ClientInfo {
        Socket socket;
        PrintWriter out;
        BufferedReader in;

        ClientInfo(Socket socket) {
            try {
                this.socket = socket;
                this.out = new PrintWriter(socket.getOutputStream(), true);
                this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on IP " + InetAddress.getLocalHost().getHostAddress() + " on Port " + port + ".\nUse \"quit\" to exit program.");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try {
            InputStream inputStream = clientSocket.getInputStream();
            ClientInfo clientInfo = new ClientInfo(clientSocket);
            String clientName = null;

            while (!clientSocket.isClosed()) {
                byte[] header = new byte[5]; // 1 Byte für Typ + 4 Bytes für Länge
                int bytesRead = inputStream.read(header);

                if (bytesRead == header.length) {
                    ByteBuffer buffer = ByteBuffer.wrap(header);
                    byte type = buffer.get(); // Typ (1 Byte)
                    int length = buffer.getInt(); // Länge (4 Bytes)

                    byte[] payload = new byte[length];
                    inputStream.read(payload); // Payload lesen

                    String message = new String(payload, StandardCharsets.UTF_8);
                    System.out.println("Empfangene Nachricht: Typ=" + type + ", Länge=" + length + ", Nachricht=" + message);

                    if(type == 1 || type == 2) {
                        clientName = message;
                        if (clients.containsKey(clientName)) {
                            clientName = clientName + (int) (Math.random() * 1000); // add random number if name is taken
                        }
                        synchronized (clients) {
                            clients.put(clientName, clientInfo);
                        }
                        System.out.println(clientName + " registered.");
                    }else if(type == 3 && clientName != null) {
                        String[] parts = message.split(",", 3);
                        if (parts.length == 3) {
                            String recipient = parts[0];
                            String msg = parts[2];
                            sendMessage(clientName, recipient, msg,3);
                        }
                    } else if(type == 4 && clientName != null) {
                        String[] parts = message.split(",", 2);
                        int n = Integer.parseInt(parts[0]); // Extrahiere die Anzahl der n-Werte
                        String msg = message.split(",", n + 3)[n + 2]; // Überspringe die n-Werte und hole die Nachricht

                        sendMessageToAllClients(clientName, msg);
                    } else if(type == 5 && clientName != null) {
                        if (message.isEmpty()) {
                            synchronized (clients) {
                                StringBuilder clientList = new StringBuilder();
                                for (String clientName2 : clients.keySet()) {
                                    clientList.append(clientName2).append(",");
                                }
                                if (clientList.length() > 0) {
                                    clientList.setLength(clientList.length() - 1); // Entfernt das letzte Komma
                                }
                                sendMessage(clientName, clientList.toString(),5);
                            }
                        }
                    } else if(type == 6 && clientName != null) {
                        String[] parts = message.split(",", 3);
                        if (parts.length == 3) {
                            String recipient = parts[0];
                            String msg = parts[2];
                            sendMessage(clientName, recipient, msg,6);
                        }
                    }else{
                        System.out.println("Ungültiger Header empfangen: Typ=" + type);
                    }


                } else if (bytesRead == -1) {
                    System.out.println("Client hat die Verbindung geschlossen.");
                    break;
                } else {
                    System.out.println("Ungültiger Header empfangen.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendMessage(String sender, String recipient, String message, int Type) {
        synchronized (clients) {
            ClientInfo recipientInfo = clients.get(recipient);
            if (recipientInfo != null) {
                try {
                    // Nachrichtentyp (1 Byte)
                    byte type = (byte) Type;

                    // Payload erstellen: sender,message
                    String payload = sender + "," + message;
                    byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

                    // Länge der Nachricht (4 Bytes, big endian)
                    byte[] lengthBytes = ByteBuffer.allocate(4).putInt(payloadBytes.length).array();

                    // Nachricht zusammenstellen
                    ByteBuffer messageBuffer = ByteBuffer.allocate(1 + 4 + payloadBytes.length);
                    messageBuffer.put(type); // Typ
                    messageBuffer.put(lengthBytes); // Länge
                    messageBuffer.put(payloadBytes); // Payload

                    // Nachricht senden
                    OutputStream out = recipientInfo.socket.getOutputStream();
                    out.write(messageBuffer.array());
                    out.flush();
                } catch (IOException e) {
                    System.err.println("Error sending message to " + recipient + ": " + e.getMessage());
                }
            } else {
                System.out.println("Client " + recipient + " not found.");
            }
        }
    }

    private static void sendMessage(String recipient, String message, int Type) {
        synchronized (clients) {
            ClientInfo recipientInfo = clients.get(recipient);
            if (recipientInfo != null) {
                try {
                    // Nachrichtentyp (1 Byte)
                    byte type = (byte) Type;

                    // Payload erstellen: sender,message
                    String payload =  message;
                    byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

                    // Länge der Nachricht (4 Bytes, big endian)
                    byte[] lengthBytes = ByteBuffer.allocate(4).putInt(payloadBytes.length).array();

                    // Nachricht zusammenstellen
                    ByteBuffer messageBuffer = ByteBuffer.allocate(1 + 4 + payloadBytes.length);
                    messageBuffer.put(type); // Typ
                    messageBuffer.put(lengthBytes); // Länge
                    messageBuffer.put(payloadBytes); // Payload

                    // Nachricht senden
                    OutputStream out = recipientInfo.socket.getOutputStream();
                    out.write(messageBuffer.array());
                    out.flush();
                } catch (IOException e) {
                    System.err.println("Error sending message to " + recipient + ": " + e.getMessage());
                }
            } else {
                System.out.println("Client " + recipient + " not found.");
            }
        }
    }

    private static void sendMessageToAllClients(String sender, String message) {
        synchronized (clients) {
            for (Map.Entry<String, ClientInfo> entry : clients.entrySet()) {
                ClientInfo recipientInfo = entry.getValue();
                try {
                    // Nachrichtentyp (1 Byte)
                    byte type = 3; // Typ 3 für Nachricht

                    // Payload erstellen: sender,message
                    String payload = sender + "," + message;
                    byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

                    // Länge der Nachricht (4 Bytes, big endian)
                    byte[] lengthBytes = ByteBuffer.allocate(4).putInt(payloadBytes.length).array();

                    // Nachricht zusammenstellen
                    ByteBuffer messageBuffer = ByteBuffer.allocate(1 + 4 + payloadBytes.length);
                    messageBuffer.put(type); // Typ
                    messageBuffer.put(lengthBytes); // Länge
                    messageBuffer.put(payloadBytes); // Payload

                    // Nachricht senden
                    OutputStream out = recipientInfo.socket.getOutputStream();
                    out.write(messageBuffer.array());
                    out.flush();
                } catch (IOException e) {
                    System.err.println("Error sending message to client: " + e.getMessage());
                }
            }
        }
    }
}