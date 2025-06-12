import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

public class TCP_Chat_Server {
    private static final int port = 1444;
    private static final Map<String, ClientInfo> clients = new HashMap<>();
    private static final Map<String, String> commonQuestions = new HashMap<>();

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

            loadCommonQuestions();
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
            ClientInfo clientInfo = new ClientInfo(clientSocket);
            String clientName = null;

            String line;
            while ((line = clientInfo.in.readLine()) != null) {
                String[] parts = line.split(" ", 3);
                if (parts[0].equalsIgnoreCase("register") && parts.length == 2) {
                    clientName = parts[1];
                    if (clients.containsKey(clientName)) {
                        clientName = clientName + (int) (Math.random() * 1000); // add random number if name is taken
                    }
                    synchronized (clients) {
                        clients.put(clientName, clientInfo);
                    }
                    System.out.println(clientName + " registered.");
                } else if (parts[0].equalsIgnoreCase("send") && parts.length == 3) {
                    String recipient = parts[1];
                    String message = parts[2];
                    sendMessage(clientName, recipient, message);
                }
                // (b) Anfrage, Nachricht an alle bekannten Clients zu schicken (Implementierung Serverseite)
                else if (parts[0].equalsIgnoreCase("broadcast")) {
                    if (clients.size() == 1) {clientInfo.out.println("No other clients connected.");}
                    if (clients.size() > 1) {broadcast(clientName, line.replaceFirst("broadcast ", ""));}
                }
                // (c) Sendung der List der bekannten Clients
                else if (parts[0].equalsIgnoreCase("list") && parts.length == 1) {
                    if (clients.size() == 1) {clientInfo.out.println("No other clients connected"); }
                    for (var client : clients.entrySet()) {
                        clientInfo.out.println(client.getKey());
                    }
                    clientInfo.out.println();
                }
                // (e) Vordefinierte Fragen beantworten
                else if ( commonQuestions.containsKey(line)){
                    clientInfo.out.println(commonQuestions.get(line));
                } else {
                    clientInfo.out.println("Unknown command.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    private static void sendMessage(String sender, String recipient, String message) {
        synchronized (clients) {
            ClientInfo recipientInfo = clients.get(recipient);
            if (recipientInfo != null) {
                recipientInfo.out.println("Message from " + sender + ": " + message);
            } else {
                System.out.println("Client " + recipient + " not found.");
            }
        }
    }

    // (a) Sende Nachricht an alle bekannten Clients
    private static void broadcast(String sender, String message) {

        for (var client : clients.entrySet() ) {
            sendMessage(sender, client.getKey(), message);
        }
    }

    // (e) Bestimmte, vordefinierte Fragen hinzufügen
    private static void loadCommonQuestions() {
        try {
            commonQuestions.put("Was ist deine IP-Addresse?", InetAddress.getLocalHost().getHostAddress());
            commonQuestions.put("Sind Kartoffeln eine richtige Mahlzeit?", "Ungekocht nicht. Zubereitet schon.");
            commonQuestions.put("Wie viel Uhr haben wir?", String.valueOf(LocalTime.now()));
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

}
