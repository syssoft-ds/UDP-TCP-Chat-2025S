package TCPChat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.NoSuchElementException;

public class Receiver extends Thread {

    private Socket s = null;
    private boolean isServer = false;
    private HashMap<Socket, String> userList = null;

    public Receiver(Socket s, boolean isServer, HashMap<Socket, String> userList) {
        this.s = s;
        this.isServer = isServer;
        this.userList = userList;
    }

    public Receiver(Socket s, boolean isServer) {
        this.s = s;
        this.isServer = isServer;
    }

    @Override
    public void run() {
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream()));
            String line;
            do {
                line = r.readLine();

                if (!this.isServer) {
                    continue;
                }

                if (line == null || line.isEmpty()) {
                    line = "Leere Nachricht";
                }

                System.out.println(line);

                String[] words = line.trim().split(" ", 3);

                if (words[0].equals("send") && words.length >= 3) {

                    try {
                        Socket dest = userList.entrySet().stream()
                                .filter(e -> e.getValue().equals(words[1]))
                                .findFirst().get().getKey();
                        String source = userList.get(s);

                        if (words[2].equals("Was ist deine IP-Adresse?")) {
                            sendMessage(s, dest.getInetAddress().getHostAddress());
                        } else if (words[2].equals("Wie viel Uhr haben wir?")) {
                            sendMessage(s, LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                        } else if (words[2].equals("Welche Rechnernetze HA war das?")) {
                            sendMessage(s,"4. HA, Aufgabe 4");
                        } else {
                            words[2] = "From " + source + ": " + words[2];
                            sendMessage(dest, words[2]);
                        }
                    } catch (NoSuchElementException e) {
                        sendMessage(s, "Benutzer nicht gefunden: " + words[1]);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        sendMessage(s, "Benutzer nicht gefunden: " + words[1]);
                    }
                }
                if (words[0].equals("list")) {
                    userList.forEach((k, v) -> {
                        try {
                            sendMessage(s, v);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
                if (words[0].equals("register")) {
                    if (userList.containsKey(s)) {
                        userList.replace(s, words[1]);
                    } else {
                        userList.put(s, words[1]);
                    }
                }
                if(words[0].equals("broadcast")) {
                    String source = userList.get(s);
                    String message = "Boradcast from " + source + ": " + words[1];
                    userList.forEach((k, v) -> {
                        try {
                            sendMessage(k, message);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }


            } while (!line.equalsIgnoreCase("stop"));
            userList.remove(s);
            s.close();
            this.interrupt();
        }
        catch (IOException e) {
            System.out.println("There was an IOException while receiving data ...");
            // System.exit(-1);
        }
    }

    private static void sendMessage (Socket s, String message) throws IOException {
        PrintWriter w = new PrintWriter(s.getOutputStream(),true);
        w.println(message);
    }

}
