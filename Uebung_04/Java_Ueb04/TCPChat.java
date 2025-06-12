import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class TCPChat {
    private static final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        if (args.length < 2 || args.length > 2) {
            System.out.println("Usage:\nServer: -l <port>\nClient: <ip> <port> <name>");
            System.exit(1);
        }

        if (args[0].equalsIgnoreCase("-l")) {
            int port = Integer.parseInt(args[1]);
            startServer(port);
        } else {
            String ip = args[0];
            int port = Integer.parseInt(args[1]);
            // String name = args[2];
            startClient(ip, port);
        }
    }

    // server
    private static void startServer(int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Server started on port " + port);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            ClientHandler handler = new ClientHandler(clientSocket);
            new Thread(handler).start();
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientName = null;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // register
                String reg = in.readLine();
				System.out.println(reg);
                if (reg == null || reg.trim().isEmpty()) {
                    out.println("Invalid registration. Send Yousername.");
                    socket.close();
                    return;
                }

                clientName = reg.trim();
                if (clients.containsKey(clientName)) {
                    out.println("Name already in use.");
                    socket.close();
                    return;
                }

                clients.put(clientName, this);
                out.println("Registered as " + clientName);
                System.out.println("Client '" + clientName + "' registered.");

                // receive message
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.equalsIgnoreCase("stop")) break;

                    else if (line.equalsIgnoreCase("list")) {
                        out.println("*connected clients: " + String.join(", ", clients.keySet()) + "*");
                        continue;
                    }
                    if (line.startsWith("send ")) {
                        String[] parts = line.split(" ", 3);
                        if (parts.length != 3) {
                            out.println("Invalid send format. Use: send <target> <message>");
                            continue;
                        }

                        String target = parts[1];
                        ClientHandler targetClient = clients.get(target);
                        String msg = parts[2];
                        if(checkPredefinedQuestions(msg, this)) continue;
                        

                        if (targetClient != null) {
                            targetClient.sendMessage(clientName + ": " + msg);
                        } else {
                            out.println("*User '" + target + "' not found.*");
                        }
                    } else if(line.startsWith("broadcast")) {
                        String msg = line.substring("broadcast".length()).trim();
                        if(checkPredefinedQuestions(msg, this)) continue;
                        broadcastMessage(clientName + " (broadcast): " + msg, this);
                        out.println("*send to following clients: " + String.join(", ", clients.keySet() + "*"));
                    } else {
                        out.println("*Unknown command.*");
                    }
                }
            } catch (IOException e) {
                System.out.println("Client connection error: " + e.getMessage());
            } finally {
                cleanup();
            }
        }

        // send message
        private void sendMessage(String message) {
            out.println(message);
        }

        // broadcast message
        private void broadcastMessage(String message, ClientHandler sender) {
            for (ClientHandler client : clients.values()) {
                if(client != sender) {
                    client.sendMessage(message);
                }
            }
        }

        // predefined questions
        private boolean checkPredefinedQuestions(String msg, ClientHandler sender) {
            String trimmed = msg.trim();
            if (trimmed.substring(0).equalsIgnoreCase("Was ist deine IP Adresse?")) {
                sender.sendMessage("*Predefined: " + "Meine IP-Adresse ist: " + socket.getLocalAddress() + ".*");
                return true;
            }
            if (trimmed.substring(0).equalsIgnoreCase("Welche Rechnernetze HA war das?")) {
                sender.sendMessage("*Predefined: " + "Das war Rechnernetze Hausaufgabe Nummer 4.*");
                return true;
            }
            if (trimmed.substring(0).equalsIgnoreCase("Wie viel Uhr haben wir?")) {
                sender.sendMessage("*Predefined: " + "Es ist " + java.time.LocalTime.now().withNano(0).toString() + ".*");
                return true;
            }
            return false;
        }

        // handle disconnect
        private void cleanup() {
            if (clientName != null) {
                clients.remove(clientName);
                System.out.println("Client '" + clientName + "' disconnected.");
            }
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    // client
    private static void startClient(String ip, int port) {
        try (Socket socket = new Socket(ip, port)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));

            // register
            String response;
            while(true) {
                System.out.print("Please enter your name: ");
                String name = userInput.readLine().trim();
                if (name.isEmpty()) {
                    System.out.println("Name cannot be empty. Please try again.");
                    continue;
                }
                out.println(name);
                response = in.readLine();
                System.out.println(response);
                if (response != null && response.startsWith("Registered")) break;
            }

            System.out.println("You can now send messages. Type 'stop' to exit and 'help' for commands.");

            // thread for handling incoming messages
            Thread listener = new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        System.out.println(serverMessage);
                    }
                } catch (IOException e) {
                    System.out.println("Connection lost.");
                }
            });
            listener.setDaemon(true);
            listener.start();

            String inputLine;
            while ((inputLine = userInput.readLine()) != null) {
                if (inputLine.equalsIgnoreCase("help")) {
                        System.out.println("Available commands:\n" +
                                "\t send <target_name> <message> - Send a message to a specific user.\n" +
                                "\t broadcast <message> - Broadcast a message to all known users.\n" +
                                "\t list - List of all connected users.\n" +
                                "\t stop - Disconnect from the server.\n" +
                                "\t help - Show this help message.");
                                continue;
                    }
                out.println(inputLine);
                if (inputLine.equalsIgnoreCase("stop")) break;
            }

        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }
}

