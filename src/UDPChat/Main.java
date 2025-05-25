package UDPChat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class Main {
    private static final int packetSize = 4096;
    private static ArrayList<User> userList = new ArrayList<User>();
    private static User user = null;

    public static void main(String[] args) throws IOException {
        System.out.println("Bitte Benutzernamen eingeben:");
        String userName = readString();
        System.out.println("Bitte Port eingeben:");
        int port = Integer.parseInt(readString());

        user = new User(userName, InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()), port);
        userList.add(user);

        DatagramSocket s = new DatagramSocket(port);
        Thread t = new Thread(() -> receiver(s));
        t.start();

        messageWorker(s);

    }


    private static void receiver ( DatagramSocket s ) {
        byte[] buffer = new byte[packetSize];
        String line;
        try {
            do {
                DatagramPacket p = new DatagramPacket(buffer,buffer.length);
                s.receive(p);
                line = new String(buffer,0,p.getLength(),"UTF-8");
                String[] words = line.trim().split("\\s+", 2);
                if(words[0].equals("sync")) {
                    User newUser = new User(words[1], p.getAddress(), p.getPort());
                    System.out.println(p.getAddress());
                    if (!userList.contains(newUser)) {
                        userList.add(newUser);
                    }
                    String syncAnswer = "syncresp";
                    for (User user : userList) {
                        syncAnswer = syncAnswer + " " + user.name + "-" + user.ip + "-" + user.port;
                    }
                    System.out.println(syncAnswer);
                    sendMessage(s,syncAnswer.getBytes("UTF-8"),p.getAddress(),p.getPort());
                    continue;
                }
                if (words[0].equals("syncresp")) {
                    String[] users = words[1].split(" ");
                    for (String user : users) {
                        String[] data = user.trim().split("-");
                        User newUser = new User(data[0], InetAddress.getByName(data[1].replace("/", "")), Integer.parseInt(data[2]));
                        if (!userList.contains(newUser)) {
                            userList.add(newUser);
                        }
                    }
                    continue;
                }
                System.out.println(line);
            } while (!line.equalsIgnoreCase("stop"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        s.close();
    }

    private static void messageWorker( DatagramSocket s ) throws IOException {
        while (true) {
            byte[] buffer = new byte[packetSize];
            String line = readString();
            String[] words = line.trim().split("\\s+", 3);

            if (words[0].equalsIgnoreCase("send")) {
                try {
                    User receiver = userList.stream().filter(user -> user.name.equals(words[1])).findFirst().get();
                    buffer = words[2].getBytes("UTF-8");
                    sendMessage(s,buffer,receiver.ip,receiver.port);
                } catch (Exception e) {
                    System.out.println("User not found");
                    continue;
                }
            }
            if (words[0].equalsIgnoreCase("stop")) {
                break;
            }
            if (words[0].equalsIgnoreCase("show")) {
                userList.forEach(System.out::println);
            }
            if (words[0].equalsIgnoreCase("sync")) {
                InetAddress inet = InetAddress.getByName(InetAddress.getLocalHost().getHostAddress());
                while (true) {
                    System.out.println("Bitte IP eines Chatpartners eingeben um einem Netzwerk beizutreten:");
                    String connectIP = readString();
                    try {
                        inet = InetAddress.getByName(connectIP);
                        break;
                    } catch (UnknownHostException e) {
                        System.out.println("Keine valide IP-Adresse");
                    }
                }

                System.out.println("Bitte Port eines Chatpartners eingeben:");
                int connectPort = Integer.parseInt(readString());

                String syncMessage = "sync " + user.name;
                buffer = syncMessage.getBytes("UTF-8");
                sendMessage(s,buffer, inet, connectPort);
            }
        }
    }

    private static void sendMessage ( DatagramSocket s, byte[] buffer, InetAddress receiverIP, int receiverPort ) throws IOException {
        DatagramPacket p = new DatagramPacket(buffer,buffer.length,receiverIP,receiverPort);
        s.send(p);
    }

    private static String readString () {
        BufferedReader br = null;
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

    static class User {

        public final String name;
        public final InetAddress ip;
        public final int port;

        public User(String name, InetAddress ip, int port) {
            this.name = name;
            this.ip = ip;
            this.port = port;
            System.out.println(ip);
        }

        @Override
        public String toString() {
            return "User{name='" + name + "', ip=" + ip + ", port=" + port + "}";
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            User other = (User) obj;
            return port == other.port &&
                    name.equals(other.name) &&
                    ip.equals(other.ip);
        }
    }
}
