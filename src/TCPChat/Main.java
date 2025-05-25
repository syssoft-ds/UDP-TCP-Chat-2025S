package TCPChat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.HashMap;

public class Main {

    private static BufferedReader br = null;

    private static HashMap<Socket, String> userList = new HashMap<>();

    public static void main(String[] args) throws IOException {

        System.out.println("Als Server starten? [y/n]");
        String isServer = readString();

        if (isServer.equalsIgnoreCase("y")) {

            System.out.println("Bitte Port eingeben:");
            String portInput = readString();
            if (portInput.isEmpty()) portInput = "111";
            int port = Integer.parseInt(portInput);
            Server(port);

        } else {

            InetAddress inet = InetAddress.getByName(InetAddress.getLocalHost().getHostAddress());
            while (true) {
                System.out.println("Bitte IP eines Chatpartners eingeben um einem Netzwerk beizutreten:");
                String connectIP = readString();
                if (connectIP.isEmpty()) connectIP = "127.0.0.1";
                try {
                    inet = InetAddress.getByName(connectIP);
                    break;
                } catch (UnknownHostException e) {
                    System.out.println("Keine valide IP-Adresse");
                }
            }
            System.out.println("Bitte Server-Port eingeben:");
            String portInput = readString();
            if (portInput.isEmpty()) portInput = "111";
            int port = Integer.parseInt(portInput);

            Client(inet, port);
        }
    }

    // ************************************************************************
    // Server
    // ************************************************************************

    private static void Server ( int port ) throws IOException {
        ServerSocket s = new ServerSocket(port);
        while (true) {
            Socket client = s.accept();
            Receiver r = new Receiver(client, true, userList);
            r.start();
        }
    }

    // ************************************************************************
    // Client
    // ************************************************************************

    private static void Client (InetAddress serverAddress, int serverPort) throws IOException {
        Socket serverConnect = new Socket(serverAddress,serverPort);
        PrintWriter w = new PrintWriter(serverConnect.getOutputStream(),true);
        Receiver r = new Receiver(serverConnect, false);
        r.start();
        String line;
        do {
            line = readString();
            w.println(line);
        } while (!line.equalsIgnoreCase("stop"));
        serverConnect.close();
    }


    // ************************************************************************
    // Utils
    // ************************************************************************

    private static String readString () {
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

}
