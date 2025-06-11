import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class Main {

    private static void fatal ( String comment ) {
        System.out.println(comment);
        System.exit(-1);
    }

    //  Server speichert die Namen der Benutzer
    private static ConcurrentHashMap<String, PrintWriter> clients = new ConcurrentHashMap<>();

    //Liste der Fragen mit den automatischen Antworten
    private static final Map<String, Supplier<String>> autoAnswers = new HashMap<>();
    static {
        autoAnswers.put("Was ist deine MAC-Adresse?", () -> "Meine MAC-Adresse ist geheim.");
        autoAnswers.put("Sind Kartoffeln eine richtige Mahlzeit?", () -> "Vielleicht...");
        autoAnswers.put("Wie viel Uhr haben wir?", () -> "Es ist jetzt " + java.time.LocalTime.now().withNano(0));
    }


    // ************************************************************************
    // MAIN
    // ************************************************************************
    public static void main(String[] args) throws IOException {
        if (args.length != 2)
            fatal("Usage: \"<netcat> -l <port>\" or \"netcat <ip> <port>\"");
        int port = Integer.parseInt(args[1]);
        if (args[0].equalsIgnoreCase("-l"))
            Server(port);
        else
            Client(args[0],port);
    }

    // ************************************************************************
    // Server
    // ************************************************************************
    private static void Server ( int port ) throws IOException {
        ServerSocket s = new ServerSocket(port);

        // Thread für Server-Konsole zur Eingabe der Nachrichten
        new Thread(() -> {
            try (BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in))) {
                String line;
                while ((line = consoleReader.readLine()) != null) {
                    if (line.equalsIgnoreCase("stop")) {
                        System.out.println("Server wird gestoppt.");
                        System.exit(0);
                    }
                    // Nachricht an alle Clients senden
                    for (PrintWriter clientOut : clients.values()) {
                        clientOut.println("Server (Systemnachricht): " + line);
                    }
                    System.out.println("Nachricht an alle Clients gesendet.");
                }
            } catch (IOException e) {
                System.out.println("Fehler beim Lesen von der Konsole.");
            }
        }).start();

        while (true) {
            Socket client = s.accept();
            Thread t = new Thread(() -> serveClient(client));
            t.start();
        }
    }

    private static void serveClient ( Socket clientConnection ) {

        String name = null;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(clientConnection.getInputStream()));
             PrintWriter w = new PrintWriter(clientConnection.getOutputStream(), true))
        {
            // Abfrage für Namen
            while (true) {
                w.println("Geben den Namen ein:");
                name = r.readLine().trim();

                if (name.isEmpty()) {
                    w.println("Der Name darf nicht leer sein.");
                } else if (name.matches("\\d+")) {
                    w.println("Der Name darf nicht nur aus Ziffern bestehen.");
                } else if (clients.containsKey(name)) {
                    w.println("Bereits verwendeter Name.");
                } else {
                    break;
                }
            }

            final String currentName = name;
            clients.put(currentName, w); // Client registrieren
            w.println("Befehle: send <name> <message> | sendall <message> | clients | ask <question> | asklist | stop");

            String line;
            while ((line = r.readLine()) != null) {
                if (line.equalsIgnoreCase("stop")) {
                    break;
                }

                if (line.toLowerCase().startsWith("send ")) {
                    String[] parts = line.split(" ", 3);
                    if (parts.length < 3) {
                        w.println("Befehle: send <name> <message> | sendall <message> | clients | ask <question> | asklist | stop");
                        continue;
                    }

                    String clientName = parts[1];
                    String message = parts[2];

                    if (clientName.equals(name)) {
                        w.println("Man kann keine Nachrichten an sich selbst senden.");
                        continue;
                    }

                    PrintWriter recipientOut = clients.get(clientName);
                    if (recipientOut != null) {
                        recipientOut.println(name + ": " + message);
                    } else {
                        w.println("Benutzer '" + clientName + "' wurde nicht gefunden.");
                    }
                } else if (line.toLowerCase().startsWith("sendall ")) {
                    String message = line.substring("sendall ".length());
                    for (String clientName : clients.keySet()) {
                        if (!clientName.equals(name)) {
                            PrintWriter recipientOut = clients.get(clientName);
                            if (recipientOut != null) {
                                recipientOut.println(name + ": " + message);
                            }
                        }
                    }
                    w.println("Nachricht wurde an alle gesendet.");
                } else if (line.equalsIgnoreCase("clients")) {
                    // Liste der Clients anzeigen
                    //String clientList = String.join(", ", clients.keySet());
                    String clientList = clients.keySet().stream()
                            .filter(n -> !n.equals(currentName))  // aktuellen Benutzer ausfiltern
                            .sorted()
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("(keine anderen Clients)");
                    w.println("Bekannte Clients: " + clientList);

                    continue;
                } else if (line.toLowerCase().startsWith("ask ")) {
                    String frage = line.substring("ask ".length()).trim();
                    Supplier<String> antwort = autoAnswers.get(frage);
                    if (antwort != null) {
                        w.println("Antwort: " + antwort.get());
                    } else {
                        w.println("Unbekannte Frage. Mit 'asklist' kann man alle Fragen sehen.");
                    }
                }
                else if (line.equalsIgnoreCase("asklist")) {
                    if (autoAnswers.isEmpty()) {
                        w.println("Es gibt keine vordefinierten Fragen.");
                    } else {
                        w.println("Liste der Fragen:");
                        for (String q : autoAnswers.keySet()) {
                            w.println("- " + q);
                        }
                    }
                } else {
                    w.println("Befehle: send <name> <message> | sendall <message> | clients | ask <question> | asklist | stop");
                }
            }
        } catch (IOException e) {
        } finally {
            try {
                clientConnection.close();
            } catch (IOException e) {}

            if (name != null) {
                clients.remove(name);
                System.out.println("Benutzer '" + name + "' hat die Verbindung getrennt.");
            }
        }
    }

    // ************************************************************************
    // Client
    // ************************************************************************
    private static void Client ( String serverHost, int serverPort ) throws IOException {

        Socket serverConnect = new Socket(serverHost, serverPort);
        BufferedReader r = new BufferedReader(new InputStreamReader(serverConnect.getInputStream())); // Eingabe vom Server
        PrintWriter w = new PrintWriter(serverConnect.getOutputStream(), true); // Ausgabe zum Server
        BufferedReader client_reader = new BufferedReader(new InputStreamReader(System.in)); // Eingabe vom Benutzer

        // Thread zum Empfangen von Nachrichten vom Server
        new Thread(() -> {
            try {
                String message;
                while ((message = r.readLine()) != null) {
                    System.out.println(message);
                }
            } catch (IOException e) {
                System.out.println("Verbindung zum Server verloren.");
                System.exit(0);
            }
        }).start();

        // Sende Nachrichten
        String userInput;
        while ((userInput = client_reader.readLine()) != null) {
            w.println(userInput);
            if (userInput.equalsIgnoreCase("stop")) break;
        }

        serverConnect.close();
    }
}