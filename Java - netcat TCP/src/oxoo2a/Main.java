package oxoo2a;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Main {

    // Map von Client-Namen zu ClientHandler-Objekten
    private static final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();

    private static void fatal(String comment) {
        System.out.println(comment);
        System.exit(-1);
    }

    // ************************************************************************
    // MAIN
    // ************************************************************************
    public static void main(String[] args) throws IOException {
        if (args.length != 2)
            fatal("Usage: \"<netcat> -l <port>\" or \"netcat <ip> <port>\"");
        int port = Integer.parseInt(args[1]);

        if (args[0].equalsIgnoreCase("-l"))
            startServer(port);
        else
            startClient(args[0], port);
    }

    // ************************************************************************
    // SERVER
    // ************************************************************************
    private static void startServer(int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Server gestartet auf Port " + port);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            ClientHandler handler = new ClientHandler(clientSocket);
            handler.start();
        }
    }

    private static class ClientHandler extends Thread {
        private final Socket socket;
        private String clientName;
        private BufferedReader in;
        private PrintWriter out;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Registrierung
                out.println("Bitte registriere dich mit: register <deinName>");
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("register ")) {
                        String requestedName = line.substring(9).trim();
                        if (requestedName.isEmpty()) {
                            out.println("Name darf nicht leer sein.");
                        } else if (clients.containsKey(requestedName)) {
                            out.println("Name bereits vergeben, bitte anderen Namen wählen.");
                        } else {
                            clientName = requestedName;
                            clients.put(clientName, this);
                            out.println("Registrierung erfolgreich als '" + clientName + "'");
                            System.out.println(clientName + " registriert.");
                            break;
                        }
                    } else {
                        out.println("Bitte zuerst registrieren: register <deinName>");
                    }
                }

                if (clientName == null) {
                    closeConnection();
                    return;
                }

                // Haupt-Kommunikation
                while ((line = in.readLine()) != null) {
                    if (line.equalsIgnoreCase("stop")) {
                        out.println("Verbindung wird geschlossen.");
                        break;
                    }

                    // send <Name> <Message>
                    if (line.startsWith("send ")) {
                        String[] parts = line.split(" ", 3);
                        if (parts.length < 3) {
                            out.println("Ungültiger Befehl. Format: send <Name> <Nachricht>");
                            continue;
                        }
                        String recipient = parts[1];
                        String message = parts[2];
                        ClientHandler recipientHandler = clients.get(recipient);
                        if (recipientHandler == null) {
                            out.println("Empfänger '" + recipient + "' nicht gefunden.");
                        } else {
                            recipientHandler.sendMessage("Nachricht von " + clientName + ": " + message);
                        }

                        // Auto-Antworten
                        if (isAutoQuestion(message)) {
                            String reply = getAutoReply(message);
                            this.sendMessage("Auto-Antwort an " + clientName + ": " + reply);
                        }

                        // sendall <Message>
                    } else if (line.startsWith("sendall ")) {
                        String message = line.substring(8);

                        // Auto-Antwort an den Sender, falls es eine Frage ist
                        if (isAutoQuestion(message)) {
                            String reply = getAutoReply(message);
                            this.sendMessage("Auto-Antwort an " + clientName + ": " + reply);
                        }

                        for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
                            if (!entry.getKey().equals(clientName)) { // nicht an sich selbst
                                entry.getValue().sendMessage("Broadcast von " + clientName + ": " + message);
                            }
                        }
                    }
                    else if (line.equalsIgnoreCase("clients")) {
                        out.println("Bekannte Clients:");
                        for (String name : clients.keySet()) {
                            out.println("- " + name);
                        }

                    } else {
                        out.println("Unbekannter Befehl. Benutze: send <Name> <Nachricht>, sendall <Nachricht>, clients oder stop");
                    }
                }

            } catch (IOException e) {
                System.out.println("Fehler bei Client " + clientName + ": " + e.getMessage());
            } finally {
                closeConnection();
            }
        }

        private boolean isAutoQuestion(String msg) {
            return msg.equalsIgnoreCase("Was ist deine MAC-Adresse?") ||
                    msg.equalsIgnoreCase("Sind Kartoffeln eine richtige Mahlzeit?");
        }

        private String getAutoReply(String msg) {
            if (msg.equalsIgnoreCase("Was ist deine MAC-Adresse?")) {
                return "Meine MAC-Adresse ist 00:11:22:33:44:55";
            }
            if (msg.equalsIgnoreCase("Sind Kartoffeln eine richtige Mahlzeit?")) {
                return "Absolut! Kartoffeln sind lecker UND nahrhaft!";
            }
            return "Ich weiß darauf leider keine Antwort.";
        }


        private void sendMessage(String message) {
            out.println(message);
        }

        private void closeConnection() {
            try {
                if (clientName != null) {
                    clients.remove(clientName);
                    System.out.println(clientName + " hat die Verbindung getrennt.");
                }
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    // ************************************************************************
    // CLIENT
    // ************************************************************************
    private static void startClient(String serverHost, int serverPort) throws IOException {
        Socket socket = new Socket(serverHost, serverPort);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));

        // Thread zum Lesen von Servernachrichten
        Thread readerThread = new Thread(() -> {
            try {
                String serverLine;
                while ((serverLine = in.readLine()) != null) {
                    System.out.println(serverLine);
                }
            } catch (IOException e) {
                System.out.println("Verbindung beendet.");
            }
        });
        readerThread.start();

        // Registrierung und Nachrichten senden
        String line;
        while ((line = userInput.readLine()) != null) {
            out.println(line);
            if (line.equalsIgnoreCase("stop")) {
                break;
            }
        }

        socket.close();
    }
}
