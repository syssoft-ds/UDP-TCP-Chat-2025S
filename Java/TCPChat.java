import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class TCPChat {
    private static final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        if (args.length < 2 || args.length > 3) {
            System.out.println("Usage:\nServer: -l <port>\nClient: <ip> <port> <name>");
            System.exit(1);
        }

        if (args[0].equalsIgnoreCase("-l")) {
            int port = Integer.parseInt(args[1]);
            startServer(port);
        } else {
            String ip = args[0];
            int port = Integer.parseInt(args[1]);
            String name = args[2];
            startClient(ip, port, name);
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
                if (reg == null || !reg.startsWith("register ")) {
                    out.println("Invalid registration.");
                    socket.close();
                    return;
                }

                String[] regParts = reg.split(" ");
                if (regParts.length != 2) {
                    out.println("Usage: register <name>");
                    socket.close();
                    return;
                }

                clientName = regParts[1];
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

                    if (line.startsWith("send ")) {
                        String[] parts = line.split(" ", 3);
                        if (parts.length != 3) {
                            out.println("Invalid send format. Use: send <target> <message>");
                            continue;
                        }

                        String target = parts[1];
                        String msg = parts[2];

                        ClientHandler targetClient = clients.get(target);
                        if (targetClient != null) {
                            targetClient.sendMessage(clientName + ": " + msg);
                            out.println("Message sent to " + target);
                        } else {
                            out.println("User '" + target + "' not found.");
                        }
                    } else {
                        out.println("Unknown command.");
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
    private static void startClient(String ip, int port, String name) {
        try (Socket socket = new Socket(ip, port)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));

            // register
            out.println("register " + name);
            String response = in.readLine();
            System.out.println(response);
            if (!response.startsWith("Registered")) return;

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
                out.println(inputLine);
                if (inputLine.equalsIgnoreCase("stop")) break;
            }

        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }
}

