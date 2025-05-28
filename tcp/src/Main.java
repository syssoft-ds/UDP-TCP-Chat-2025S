import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class Main {

    private static void fatal ( String comment ) {
        System.out.println(comment);
        System.exit(-1);
    }

    //  Server speichert die Namen der Benutzer
    private static ConcurrentHashMap<String, PrintWriter> clients = new ConcurrentHashMap<>();

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
        while (true) {
            Socket client = s.accept();
            Thread t = new Thread(() -> serveClient(client));
            t.start();
        }
    }

    private static void serveClient ( Socket clientConnection ) {
       /* try {
            BufferedReader r = new BufferedReader(new InputStreamReader(clientConnection.getInputStream()));
            String line;
            do {
                line = r.readLine();
                System.out.println(line);
            } while (!line.equalsIgnoreCase("stop"));
            clientConnection.close();
        }
        catch (IOException e) {
            System.out.println("There was an IOException while receiving data ...");
            System.exit(-1);
        } */

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

            clients.put(name, w); // Client registrieren
            w.println("Befehle: send <name> <message> | stop");

            String line;
            while ((line = r.readLine()) != null) {
                if (line.equalsIgnoreCase("stop")) {
                    break;
                }

                if (line.toLowerCase().startsWith("send ")) {
                    String[] parts = line.split(" ", 3);
                    if (parts.length < 3) {
                        w.println("Befehle: send <name> <message> | stop");
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
                } else {
                    w.println("Befehle: send <name> <message> | stop");
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
        /*InetAddress serverAddress = InetAddress.getByName(serverHost);
        Socket serverConnect = new Socket(serverAddress,serverPort);
        PrintWriter w = new PrintWriter(serverConnect.getOutputStream(),true);
        String line;
        do {
            line = readString();
            w.println(line);
        } while (!line.equalsIgnoreCase("stop"));
        serverConnect.close();*/

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