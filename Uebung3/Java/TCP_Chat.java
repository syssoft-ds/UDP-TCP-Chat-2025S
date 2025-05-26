import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class TCP_Chat {

    public String clientName = "Client";
    public String clientIP;
    public int clientPort;
    public List<ClientInfo> registeredClients = new ArrayList<>();

    private static void fatal ( String comment ) {
        System.out.println(comment);
        System.exit(-1);
    }

    // ************************************************************************
    // MAIN
    // ************************************************************************
    public void main(String[] args) throws IOException {
        if (args.length < 2 || args.length > 3)
            fatal("Usage: \"-l <port>\" or \"<ip> <port> <name>\" ");

        int port = Integer.parseInt(args[1]);

        if (args[0].equalsIgnoreCase("-l")) {
            try (DatagramSocket socket = new DatagramSocket(port)) {
                socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
                clientIP = socket.getLocalAddress().getHostAddress();
                socket.disconnect();
                clientPort = socket.getLocalPort();
            }

            System.out.printf("TCP Chat Server started on IP '%s', Port '%d'\n", clientIP, clientPort);
            Server(port);
        }else {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
                clientIP = socket.getLocalAddress().getHostAddress();
                socket.disconnect();
                clientPort = socket.getLocalPort();
            }
            clientName = args[2];
            System.out.printf("TCP Chat Client started with name '%s', IP '%s', Port '%d'\n", clientName, clientIP, clientPort);
            Client(args[0], port);

        }


    }

    // ************************************************************************
    // Server
    // ************************************************************************
    private void Server ( int port ) throws IOException {
        ServerSocket s = new ServerSocket(port);
        while (true) {
            Socket client = s.accept();
            Thread t = new Thread(() -> serveClient(client));
            t.start();
        }
    }

    private void serveClient(Socket clientConnection) {
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(clientConnection.getInputStream()));
            PrintWriter w = new PrintWriter(clientConnection.getOutputStream(), true);

            // Registrierung des Clients
            String registration = r.readLine();
            String[] regParts = registration.split(" ");
            if (regParts.length == 3 && regParts[0].equals("register")) {
                String name = regParts[1];
                int port = Integer.parseInt(regParts[2]);
                registeredClients.add(new ClientInfo(name, clientConnection.getInetAddress().getHostAddress(), port, clientConnection));
                w.println("Registered as " + name);
                System.out.printf("Registered Client as " + name +" and added to the list.\n");
            }

            // Nachrichtenverarbeitung
            String line;
            while ((line = r.readLine()) != null && !line.equals("stop")) {
                String[] parts = line.split(" ", 3);
                if (parts.length == 3 && parts[0].equals("send")) {
                    String targetName = parts[1];
                    String message = parts[2];
                    forwardMessage(targetName, message, w);
                }
            }
            clientConnection.close();
        } catch (IOException e) {
            System.out.println("Error handling client: " + e.getMessage());
        }
    }

    private void forwardMessage(String targetName, String message, PrintWriter senderWriter) {
        for (ClientInfo client : registeredClients) {
            if (client.name.equals(targetName)) {
                try {
                    PrintWriter targetWriter = new PrintWriter(client.socket.getOutputStream(), true);
                    targetWriter.println(message);
                    senderWriter.println("Message sent to " + targetName);
                    System.out.printf("Message sent to %s\n", targetName);
                    return;
                } catch (IOException e) {
                    senderWriter.println("Error sending message to " + targetName);
                    System.out.printf("Error sending message to %s: %s\n", targetName, e.getMessage());
                }
            }
        }
        senderWriter.println("Client with name " + targetName + " not found.");
        System.out.printf("Client with name %s not found.\n", targetName);
    }

    // ************************************************************************
    // Client
    // ************************************************************************
    private void Client(String serverHost, int serverPort) throws IOException {
        InetAddress serverAddress = InetAddress.getByName(serverHost);
        Socket serverConnect = new Socket(serverAddress, serverPort);
        PrintWriter w = new PrintWriter(serverConnect.getOutputStream(), true);
        BufferedReader r = new BufferedReader(new InputStreamReader(serverConnect.getInputStream()));

        // Registrierung
        w.println("register " + clientName + " " + serverConnect.getLocalPort());
        System.out.println(r.readLine());

        // Thread zum Empfangen von Nachrichten
        Thread receiveThread = new Thread(() -> {
            try {
                String incomingMessage;
                while ((incomingMessage = r.readLine()) != null) {
                    System.out.println(incomingMessage); // Zeigt die Nachricht mit Absender an
                }
            } catch (IOException e) {
                System.out.println("Error receiving messages: " + e.getMessage());
            }
        });
        receiveThread.start();

        // Nachrichten senden
        String line;
        do {
            line = readString();
            w.println(line);
        } while (!line.equalsIgnoreCase("stop"));
        serverConnect.close();
    }

    private String readString() {
        boolean again = false;
        String input = null;
        do {
            // System.out.print("Input: ");
            try {
                if (br == null)
                    br = new BufferedReader(new InputStreamReader(System.in));
                input = br.readLine();
            }
            catch (Exception e) {
                System.out.printf("Exception: %s\n",e.getMessage());
                again = true;
            }
        } while (again);
        return input;
    }

    private BufferedReader br = null;

    public class ClientInfo {
        public String name;
        public String ip;
        public int port;
        public Socket socket;

        public ClientInfo(String name, String ip, int port, Socket socket) {
            this.name = name;
            this.ip = ip;
            this.port = port;
            this.socket = socket;
        }

        @Override
        public String toString() {
            return "Name: " + name + ", IP: " + ip + ", Port: " + port;
        }
    }

    public void addClient(String name, String ip, int port, Socket socket) {
        registeredClients.add(new ClientInfo(name, ip, port, socket));
    }
}
