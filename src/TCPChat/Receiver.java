package TCPChat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;

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
                System.out.println(line);
                if (!this.isServer) {
                    continue;
                }

                String[] words = line.trim().split(" ", 3);

                if (words[0].equals("send")) {
                    Socket dest = userList.entrySet().stream().filter(e -> e.getValue().equals(words[1])).findFirst().get().getKey();
                    String source = userList.get(s);
                    words[2] = "From " + source + ": " + words[2];
                    sendMessage(dest, words[2]);
                }
                if (words[0].equals("show")) {
                    userList.forEach((k, v) -> {
                        try {
                            sendMessage(s, v);
                            System.out.println("test");
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


            } while (!line.equalsIgnoreCase("stop"));
            s.close();
        }
        catch (IOException e) {
            System.out.println("There was an IOException while receiving data ...");
            System.exit(-1);
        }
    }

    private static void sendMessage (Socket s, String message) throws IOException {
        PrintWriter w = new PrintWriter(s.getOutputStream(),true);
        w.println(message);
    }

}
