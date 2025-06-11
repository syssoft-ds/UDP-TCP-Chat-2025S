import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.net.SocketException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

public class udp_chat {

    private static final int packetSize = 4096;

    private static BufferedReader input = null;
    private static String nickname = null;
    private static InetAddress localHost = null;
    private static int localport = -1;

    private static DatagramSocket socket = null;

    private static HashMap<String, InetSocketAddress> registeredUsers = new HashMap<>();
    private static HashMap<String, String> registeredAddresses = new HashMap<>();

    private static boolean END_CHAT_FLAG = false;



    private static void fatal ( String comment ) {
        System.out.println(comment);
        System.exit(-1);
    }

    // ************************************************************************
    // MAIN
    // ************************************************************************

    public static void main(String[] args) throws IOException {


        if (args.length > 1)
            fatal("Use 'netcat' or 'netcat <nickname>'");

        input = new BufferedReader(new InputStreamReader(System.in));

        // enter nickname

        while(args.length == 0){
            try {
                System.out.println("\nPlease provide a Nickname (single word, alphanumerical characters).");
                nickname = input.readLine();

                if (nickname.contains(" "))
                    throw new IOException("Your nickname may not contain whitespaces.");
                
                if (!nickname.matches("[a-zA-Z0-9]+"))
                    throw new IOException("Your nickname may only contain alphanumerical characters.");

                break;
            } catch (IOException e){
                System.out.println(nickname + " is not a valid nickname. Try again.");
                continue;
            }
        }

        while(args.length == 1){
            try{
                if (nickname != null){
                    System.out.println("\nPlease provide a Nickname (single word, alphanumerical characters).");
                    nickname = input.readLine();
                }

                nickname = args[0];
                if (nickname.contains(" "))
                        throw new IOException("Your nickname may not contain whitespaces.");
                    
                    if (!nickname.matches("[a-zA-Z0-9]+"))
                        throw new IOException("Your nickname may only contain alphanumerical characters.");

                    break;
            } catch (IOException e){
                    System.out.println(nickname + " is not a valid nickname. Try again.");
                    continue;
            }
        }


        // add checks when user already exists
        // refactor: username per chat not global to avoid doubles

        while (localport==-1){

            try{

                System.out.println("\nPlease enter a portnumber for the program to listen at. (valid port numbers: 1023 - 65535)");
                localport = Integer.parseInt(input.readLine());

                if(localport < 0 || 65535 < localport)
                    throw new IOException("\nport is out of range.");
                
                if(!freePort(localport))
                    throw new IOException("\nport is already in use.");
                break;

            } catch (IOException e){
                System.out.println(e.getMessage());
                localport = -1;
            } catch (NumberFormatException e){
                System.out.println("\nPlease input an Integer.");
                localport = -1;
            }

        }
        // get local ip

        try {
        
            localHost = InetAddress.getLocalHost();
        
        }catch(UnknownHostException e){
        
            System.out.println("Unable to determine local IP address.");
        
        }

        try{

            socket = new DatagramSocket(localport);

            Thread t = new Thread(() -> { try {
                                            receiver(socket);
                                        } catch (IOException e){

                                            System.out.println("receiver shutdown.");
                                        
                                        }});

            t.start();

            sender(socket);

        } catch(IOException e){

            // add meaningful exception handling

        } finally{

            input.close();
            socket.close();
        }

    }

    /*******************
     * Network Utility *
     *******************/

    private static boolean freePort(int port){

        // This method checks whether a port is available

        try(DatagramSocket socket = new DatagramSocket(port)){
            socket.close();
            return true;
        } catch (IOException e){
            // port is already in use

            System.out.println(port + " is already in use.");

            return false;
        }
    }


    // ************************************************************************
    // receiver
    // ************************************************************************


    private static void receiver ( DatagramSocket socket ) throws IOException  {

        // This method listens for incoming packets

        byte[] buffer = new byte[packetSize];
        try{
            do {
                DatagramPacket packet = new DatagramPacket(buffer,buffer.length);
                socket.receive(packet);

                //check whether host is known

                String lookup = packet.getAddress().getHostAddress();
                
                String input = new String(buffer,0,packet.getLength(),"UTF-8");

                String[] tokens = input.split("\\s+");

                if (registeredAddresses.get(lookup) == null && tokens[0].equalsIgnoreCase("register"))
                    
                    // if host is not known

                    registerHost(packet, buffer);

                else if (registeredAddresses.get(lookup) != null){

                    // if host is known

                    receiveMessage(input, buffer, socket, packet);

                }
            } while (!END_CHAT_FLAG);

        } catch(SocketException e){
            System.out.println("Socket closed, receiver shutting down.");
        }
    }

    private static void receiveMessage(String input, byte[] buffer, DatagramSocket socket, DatagramPacket packet){

        // This method displays a new message

        try{

            //String input = new String(buffer,0,packet.getLength(),"UTF-8");
            String user = registeredAddresses.get(packet.getAddress().getHostAddress());

            String[] tokens = input.split("\\s+");

            // extracting the message
            if (tokens[0].toLowerCase().matches("send"))
                input = input.replaceFirst(tokens[0] + " " + tokens[1], "").trim();
            else
                input = input.replaceFirst(tokens[0], "").trim();

            // outputting the message
            System.out.println(user +": " + input);

            // setting the prefix for predefined answers
            String prefix = "send " + user + " ";

            // predefined answers

            if(input.toLowerCase().startsWith("wie viel uhr haben wir?")){
                ZonedDateTime zdt = ZonedDateTime.now();
                String formatted = zdt.format(DateTimeFormatter.RFC_1123_DATE_TIME);
                sendMessage(user, prefix + formatted, socket, buffer);
            }
            if(input.toLowerCase().startsWith("was ist deine ip-adresse?")){
                try{
                    InetAddress localHost = InetAddress.getLocalHost();
                    String localIp = localHost.getHostAddress();
                    sendMessage(user, prefix + localIp, socket, buffer);
                } catch (UnknownHostException e) {
                    // pass
                }
            }
            if(input.toLowerCase().startsWith("welche rechnernetze ha war das?")){
                sendMessage(user, prefix + "4. HA, Aufgabe 4", socket, buffer);
            }

        } catch(Exception e){

            // add proper exception handling
        }

    }

    /*******************
     * Utility methods *
     *******************/

    private static void registerHost(DatagramPacket packet, byte[] buffer){
        
        // This method registers a new user with the local host

        try{

            String input = new String(buffer,0,packet.getLength(),"UTF-8");   
            String[] token = input.split("\\s+");
            
            if(token[0].equalsIgnoreCase("register"));
                if (token[1].matches("^[a-zA-Z0-9]+$")){
                    
                    String newUser = token[1];
                    InetSocketAddress newAddress = new InetSocketAddress(packet.getAddress(), packet.getPort());
                    
                    registeredUsers.put(newUser, newAddress);
                    registeredAddresses.put(newAddress.getAddress().getHostAddress(), newUser);

                    System.out.println(newUser + " (IP: " + packet.getAddress().getHostAddress() + ", PORT: " + packet.getPort() +") registered with you.");
                } 
        } catch (Exception e){

            // add proper exception handling
        
        }
    }


    private static void unregisterHost(String nickname){

        // this method removes a users nickname & address from known users and addresses

        if(registeredUsers.containsKey(nickname)){
            String address = registeredUsers.get(nickname).getAddress().getHostAddress();
            registeredUsers.remove(nickname);
            registeredAddresses.remove(address);
        }
        else{
            System.out.println(nickname + " is not known.");
        }

    }

    private static void sender(DatagramSocket socket){

        // This method reads and parses user input

        greeter();

        byte[] buffer = new byte[packetSize];
        String line;
        do {
            line = readString();

            // check whether line is empty
            try{
                String[] tokens = line.split("\\s+");

                switch(tokens[0].toLowerCase()){
                    case "send": if (tokens.length < 2) 
                                        throw new MalformedInputException("SEND requires a <nickname>, but none was supplied"); 
                    
                                sendMessage(tokens[1], line, socket, buffer); 
                                break;

                    case "exit":closeChat(); 
                                break;

                    case "send_all": broadcast(line, socket, buffer);
                                     break;

                    case "register":if(tokens.length < 3)
                                        throw new MalformedInputException("REGISTER requires 2 arguments: <target ip> <target port>"); 
                    
                                    registerWithHost(socket, tokens[1], Integer.parseInt(tokens[2])); 
                                    break;

                    
                    case "unregister":  if(tokens.length < 2)
                                            throw new MalformedInputException("UNREGISTER requires a <nickname>, but none was supplied"); 
                    
                                        unregisterHost(tokens[1]); 
                                        break;
                    case "help":if(tokens.length < 2){ 
                                    help();
                                    break;
                                }
                                help(tokens[1]);
                                break;

                    case "whois":   if(tokens.length < 2)
                                        throw new MalformedInputException("WHOIS requires a <nickname>, but none was supplied.");
                                    
                                    whois(tokens[1]); 
                                    break;
                                    
                    case "whoami":  whoami(); 
                                    break;

                    case "peers":   displayUsers();
                                    break;

                    default: throw new MalformedInputException(tokens[0] + " unknown command. Typ <help> for a list of known commands.");
                }
            } catch(MalformedInputException e){
                System.out.println(e.getMessage());
            }

        } while (!END_CHAT_FLAG);
    }

    private static void sendMessage(String nickname, String line, DatagramSocket socket, byte[] buffer){

        // This method sends messages to other users

        try{

            if(!registeredUsers.containsKey(nickname))
                throw new NameException(nickname + " is not a known user.");

            //String message = line.replaceFirst("send " + nickname + " ", "");
            String message = line;
            buffer = message.getBytes("UTF-8");

            InetSocketAddress otherHost = registeredUsers.get(nickname);
            InetAddress otherHostAddress = otherHost.getAddress();
            int otherHostPort = otherHost.getPort();
            DatagramPacket p = new DatagramPacket(buffer,buffer.length,otherHostAddress,otherHostPort);
            socket.send(p);

            }catch (NameException e){
            
            System.out.println(e.getMessage());
            
            }catch(IOException e){

                System.out.println("\nMessage could not be send.");

            } 
    }

    private static String readString () {

        // This method reads console input

        BufferedReader br = null;
        boolean again = false;
        String input = null;
        do {
            // System.out.print("Input: ");
            try {
                if (br == null)
                    br = new BufferedReader(new InputStreamReader(System.in));
                input = br.readLine();
                if(again == true)
                    again = false;
            }
            catch (Exception e) {
                System.out.printf("Exception: %s\n",e.getMessage());
                again = true;
            }
        } while (again);
        return input;
    }

    
    private static void broadcast(String message, DatagramSocket socket, byte[] buffer){
        // This method broadcasts a message to all registered users

        message.replaceFirst("broadcast", "");
        message.trim();

        for (HashMap.Entry<String, InetSocketAddress> entry : registeredUsers.entrySet()) {
            sendMessage(entry.getKey(), message, socket, buffer);
        }

    }

    private static void registerWithHost(DatagramSocket socket, String targetAddress, int targetPort){
        
        // This method registers the local user with a new remote host

        try {
            byte[] buffer = new byte[packetSize];
            InetAddress targetHost = InetAddress.getByName(targetAddress);

            String line = "register " + nickname + " " + localHost.getHostAddress() + " " + localport;
            buffer = line.getBytes("UTF-8");
            DatagramPacket p = new DatagramPacket(buffer, buffer.length, targetHost, targetPort);
            socket.send(p);
        }
            catch(Exception e){

                // add proper exception handling
        }
    } 

    private static void whoami(){

        // This method returns a users nickname as well as the ip address and port of the local client instance

        System.out.println("NICKNAME: " + nickname + ", IP: " + localHost.getHostAddress() + ", PORT: " + localport);
    }

    private static void whois(String nickname){

        // This method returns the nickname, ip and port for a specific registered user

        if(registeredUsers.containsKey(nickname)){
            InetSocketAddress address = registeredUsers.get(nickname);
            System.out.println("NICKNAME: " + nickname + ", IP: " + address.getAddress().getHostAddress() + ", PORT: " + address.getPort());
        }
        else
            System.out.println(nickname + " is not known.");
    }

    private static void greeter(){

        // This method displays a greeting message after initialisation

        System.out.println("To send a message to another user type: send <username>\n\nYou can only send message to another user after you have registered with them and they have registered with you by typing: register <target ip> <target port>\n\nTo see a list of all available commands type: help (help <command> for more details about a specific command)\n");
    }

    private static void help(){

        // This method returns a list of available commands + syntax

        System.out.println("\nLIST OF KNOWN COMMANDS:\n\nregister - SYNTAX: register <target ip> <target port>\n\nunregister - SYNTAX: unregister <nickname>\n\nsend - SYNTAX: send <nickname> <message>\n\nexit - SYNTAX: exit\n\nwhoami - SYNTAX: whoami\n\nwhois - SYNTAX: whois <nickname>\n\nPeers - SYNTAX: Peers\n\nhelp - SYNTAX: help OR help <command>\n");
    }

    private static void help (String command){

        // This method returns detailed information about a specific command

        switch(command.toLowerCase()){
            case "send": System.out.println("\nSYNTAX: send <username> <message>\n\nSends a message to a user. The message is only displayed to the user if you are registered with them and they are registered with you.\n"); break;
            case "Exit": System.out.println("\nSYNTAX: exit\n\n Closes the client.\n"); break;
            case "register": System.out.println("\nSYNTAX: register <target ip> <target port>\n\nSend your name to another user to register your client with them.\n"); break;
            case "unregister": System.out.println("\nSYNTAX: unregister <username>\n\nUnregisters a user with your client preventing their messages from being deisplayed to you.\n"); break;
            case "whoami": System.out.println("\nSYNTAX: whoami\n\nDisplays your username, ip and the port your client is running on.\n"); break;
            case "whois": System.out.println("\nSYNTAX: whois <username>\n\nDisplays the ip and the port the client of another is running on.\n"); break;
            case "Peers": System.out.println("\nSYNTAX: peers\n\nPrints a list of all users that registered with your client.\n"); break;
            case "help": System.out.println("\nSYNTAX: help\n\nPrints a list of all available commands.\n\nSYNTAX: help <command>\n\nDisplays additional information for a given command.\n"); break;
            default: System.out.println(command + " is not a known command\n.");
        }
    }

    private static void displayUsers(){

        // This method prints the names, ip addresses and ports of all registered users

        System.out.println("\nLIST OF KNOWN USERS:");
        for(HashMap.Entry<String, InetSocketAddress> entry : registeredUsers.entrySet()){
            System.out.println(entry.getKey() + " " + entry.getValue().getAddress().getHostAddress() + " " + entry.getValue().getPort());
        System.out.println();
        }
    }

    private static void closeChat(){

        // sets flag to close down the program

        END_CHAT_FLAG = true;

        if (socket != null)
            socket.close();
    }

    /*********************
     * Custom Exceptions *
     *********************/

    static class NameException extends IOException{

        // This exception is thrown if the input username does not conform to the specifications

        public NameException(String message){
            super(message);
        }
    }

    static class MalformedInputException extends IOException{

        // This exception is thrown if a users input does not contain a known command

        public MalformedInputException(String message){
            super(message);
        }
    }

}
