package oxoo2a;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Main {

    private static final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            fatal("Usage: java Main -l <port> OR java Main <server_ip> <port>");
        }

        if (args[0].equalsIgnoreCase("-l")) {
            int port = Integer.parseInt(args[1]);
            startServer(port);
        } else {
            String serverIp = args[0];
            int port = Integer.parseInt(args[1]);
            startClient(serverIp, port);
        }
    }

    private static void fatal(String comment) {
        System.out.println(comment);
        System.exit(-1);
    }

    // -------- SERVER --------
    private static void startServer(int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Server läuft auf Port " + port);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            ClientHandler handler = new ClientHandler(clientSocket);
            new Thread(handler).start();
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private String clientName = null;
        private BufferedReader in;
        private PrintWriter out;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                out.println("Bitte registriere dich mit: register <dein_name>");

                while (true) {
                    String line = in.readLine();
                    if (line == null) return;
                    line = line.trim();

                    if (line.startsWith("register ")) {
                        String requestedName = line.substring(9).trim();
                        if (requestedName.isEmpty()) {
                            out.println("Name darf nicht leer sein. Versuche erneut.");
                        } else if (clients.containsKey(requestedName)) {
                            out.println("Name schon vergeben. Bitte anderen Namen wählen.");
                        } else {
                            clientName = requestedName;
                            clients.put(clientName, this);
                            out.println("Registrierung erfolgreich als " + clientName);
                            broadcast("[Server]: " + clientName + " ist dem Chat beigetreten.", this);
                            break;
                        }
                    } else {
                        out.println("Bitte zuerst registrieren: register <dein_name>");
                    }
                }

                String msg;
                while ((msg = in.readLine()) != null) {
                    msg = msg.trim();
                    if (msg.equalsIgnoreCase("stop")) {
                        break;
                    } else if (msg.startsWith("send ")) {
                        String[] parts = msg.split(" ", 3);
                        if (parts.length < 3) {
                            out.println("Fehler: Verwendung send <name> <nachricht>");
                            continue;
                        }
                        String targetName = parts[1];
                        String message = parts[2];

                        ClientHandler targetClient = clients.get(targetName);
                        if (targetClient != null) {
                            targetClient.out.println("[" + clientName + "]: " + message);
                        } else {
                            out.println("Empfänger " + targetName + " nicht gefunden.");
                        }

                    } else if (msg.startsWith("broadcast ")) {
                        String message = msg.substring(10).trim();
                        broadcast("[" + clientName + "]: " + message, this);
                    } else if (msg.equalsIgnoreCase("list")) {
                        out.println("Bekannte Clients:");
                        for (String name : clients.keySet()) {
                            out.println("- " + name);
                        }
                    } else if (isKnownQuestion(msg)) {
                        out.println(answerFor(msg));
                    } else {
                        out.println("Unbekannter Befehl.");
                    }
                }

            } catch (IOException e) {
                System.out.println("Verbindung zu " + clientName + " verloren.");
            } finally {
                if (clientName != null) {
                    clients.remove(clientName);
                    broadcast("[Server]: " + clientName + " hat den Chat verlassen.", this);
                }
                try {
                    socket.close();
                } catch (IOException ignored) {}
            }
        }

        private void broadcast(String message, ClientHandler except) {
            for (ClientHandler c : clients.values()) {
                if (c != except) {
                    c.out.println(message);
                }
            }
        }

        private boolean isKnownQuestion(String msg) {
            return msg.equalsIgnoreCase("Was ist deine MAC-Adresse?") ||
                    msg.equalsIgnoreCase("Sind Kartoffeln eine richtige Mahlzeit?");
        }

        private String answerFor(String question) {
            String lower = question.trim().toLowerCase();
            switch (question) {
                case "was ist deine mac-adresse?":
                    return "Meine MAC-Adresse ist geheim!";
                case "sind kartoffeln eine richtige mahlzeit?":
                    return "Aber natürlich, besonders mit Quark!";
                default:
                    return "Keine Antwort verfügbar.";
            }
        }
    }

    // -------- CLIENT --------
    private static void startClient(String serverIp, int port) throws IOException {
        Socket socket = new Socket(serverIp, port);
        BufferedReader serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter serverOut = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in));

        Thread listener = new Thread(() -> {
            try {
                String line;
                while ((line = serverIn.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (IOException e) {
                System.out.println("Verbindung zum Server unterbrochen.");
            }
        });
        listener.start();

        String input;
        while ((input = userIn.readLine()) != null) {
            serverOut.println(input);
            if ("stop".equalsIgnoreCase(input.trim())) {
                break;
            }
        }

        socket.close();
        System.exit(0);
    }
}
