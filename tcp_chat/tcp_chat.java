import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.HashMap;

public class tcp_chat {

    private static HashMap<String, PrintWriter> registeredUsers = new HashMap<>();

    private static BufferedReader br = null;

    private static int localport = -1;
    private static boolean END_CHAT_FLAG = false;


    private static void fatal ( String comment ) {
        System.out.println(comment);
        System.exit(-1);
    }

    // ************************************************************************
    // MAIN
    // ************************************************************************

    public static void main(String[] args) throws IOException {
        
        // unterscheiden zwischen  server / client

        if (args.length > 1)
            fatal("Usage: \"<netcat> -l\" for server mode OR \"netcat\" for client mode");    
        
        if (args.length == 1 && args[0].equalsIgnoreCase("-l")){
            
            // server mode
            
            while (localport==-1){

                try{

                    System.out.println("\nPlease enter a portnumber for the server. (valid port numbers: 1023 - 65535)");
                    localport = Integer.parseInt(readString());

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

            Server(localport);
        
        }
        else{

            // client mode

            Client();
        }
    }

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
    // Server
    // ************************************************************************

    private static void Server ( int port ) throws IOException {

        // This method opens a ServerSocket and accepts incoming connections

        ServerSocket s = new ServerSocket(port);
        while (true) {
            Socket client = s.accept();
            Thread t = new Thread(() -> serveClient(client));
            t.start();
        }
    }

    private static void serveClient ( Socket clientConnection ) {

        // This method initialises a client's connection to the server

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientConnection.getInputStream()));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(clientConnection.getOutputStream()), true);
            String line;
            String username;

            // setting username

            while(true){

                out.println("Enter your username (alphanumerical) OR stop to close the connection.");

                username = in.readLine().toLowerCase();

                if(username.toLowerCase().equals("stop")){
                    in.close();
                    out.close();
                    clientConnection.close();
                    return;
                }

                if(username.matches("^[a-zA-Z0-9]+$"))
                    if(!registeredUsers.containsKey(username)){
                        registeredUsers.put(username, out);
                        break;
                    }
                    else
                        out.println("User already exists.");
                else
                    out.println("Invalid username.");
            
            }
            
            out.println("username " + username + " set.\n");
            out.println(greeter());

            // receiving messages

            do {
                try{

                    line = in.readLine();

                    String[] tokens = line.split("\\s+");

                    switch(tokens[0].toLowerCase()){
                        case "send":sendMessage(username, tokens[1].toLowerCase(), line); 
                                    break; //send message to user
                        case "stop":closeChat(); 
                                    break;
                        case "known":   listUsers(username); 
                                        break; // returns userlist 
                        case "isknown": isKnown(tokens[1], username); 
                                        break;
                        case "help":if(tokens.length < 2){   //returns list of supported commands
                                        help(username); 
                                        break;
                                    }
                                    help(tokens[1], username);
                                    break;
                        default: throw new MalformedInputException(tokens[0] + " is not a known command.");
                    }
                } catch(MalformedInputException e){

                    out.println(e.getMessage());
                
                }

            } while (!END_CHAT_FLAG);

            System.out.println("closing connection to user: " + username + "...");
            registeredUsers.remove(username);
            out.close();
            in.close();
            clientConnection.close();
            System.out.println("connection to user: " + username + " closed.");
        }
        catch (IOException e) {
            System.out.println("There was an IOException while receiving data ...");
            System.exit(-1);
        }
    }

    private static void sendMessage(String sender, String receiver, String input){

        // This method sends a message to a client

        try {
            if(!registeredUsers.containsKey(receiver))
                throw new UnknownUserException(receiver + " is not a known user on this server.");
            
            String message = "from "+ sender + ": " + input.replaceFirst("send " + receiver + " ", "");

            registeredUsers.get(receiver).println(message);

        } catch(UnknownUserException e){

            registeredUsers.get(sender).println(e.getMessage());
        }
    } 


    private static void listUsers(String sender){

        // This method returns a list of all currently connected users


        String userlist = "";

        for(HashMap.Entry<String, PrintWriter> entry: registeredUsers.entrySet())
            userlist += entry.getKey() + " ";
        
        userlist.trim();

        registeredUsers.get(sender).println(userlist);
    }

    private static void isKnown(String username, String sender){

        // This method returns to a user whether any user with a specific nickname is currently connected

        try{
            if(!registeredUsers.containsKey(username))
                throw new UnknownUserException(username + " is not known on this server.");

            registeredUsers.get(sender).println(username + " is known on this server.");
        } catch(UnknownUserException e){

            registeredUsers.get(sender).println(e.getMessage());

        }
    }


    private static void help(String sender){

        // This method returns a list of all available commands and their syntax

        registeredUsers.get(sender).println("\nLIST OF SUPPORTED COMMANDS:\n\nsend - SYNTAX: send <message>\nstop - SYNTAX: stop\nknown - SYNTAX: known\nisknown - SYNTAX: isknown <username>\nhelp - SYNTAX: help OR help <command>\n");
    }

    private static void help(String command, String sender){

        // This method returns detailed information about a specific command

        switch(command.toLowerCase()){
            case "send": registeredUsers.get(sender).println("SYNTAX: send <username> <message>\nSends a message to a user, if the user is registered with the server."); break;
            case "stop": registeredUsers.get(sender).println("SYNTAX: stop\nCloses the client's connection to the server."); break;
            case "known": registeredUsers.get(sender).println("SYNTAX: known\nReturns a list of users that registered with the server."); break;
            case "isknown": registeredUsers.get(sender).println("SYNTAX: isknown <username>\nReturns whether a username is registered with the server."); break;
            case "help": registeredUsers.get(sender).println("SYNTAX: help\nPrints a list of all available commands.\n\nSYNTAX: help <command>\nDisplays additional information for a given command."); break;
            default: registeredUsers.get(sender).println(command + " is not a known command on this server.");
        }
    }

    private static void closeChat(){

        // This method sets the END_OF_CHAT flag

        END_CHAT_FLAG = true;
        
    }

    private static String greeter(){

        // This method prints a simple greeting message when a user connects to the server

        return "Welcome to the server\n\nTo get a list of all available commands type: help\nTo get a list of all users currently connected to the server type: known\nTo send a message to another user type: send <nickname>\nTo disconnect from the server type: stop\n";
    }

    // ************************************************************************
    // Client
    // ************************************************************************

    private static void Client () throws IOException {

        // This method initialises the client connection

        Socket serverConnect = null;
        int serverPort = -1; 
        String serverHost = null;

        String line = null;
        
        do{
            try{
                System.out.println("Please enter the IP address of the server.");
                serverHost = readString();
                InetAddress serverAddress = InetAddress.getByName(serverHost);
                System.out.println("Please enter the port the server is listening on.");
                serverPort = Integer.parseInt(readString());
                if (serverPort < 0 || 65335 < serverPort)
                    throw new NumberFormatException();

                serverConnect = new Socket(serverAddress,serverPort);
            } catch(UnknownHostException e){
                System.out.println(serverHost + " is not a valid IP address.");
                continue;
            } catch (NumberFormatException e){
                System.out.println("Please enter an Integer between 0 and 65335");
            } catch (SocketTimeoutException e) {
                System.out.println("Connection to server at " + serverHost + " on port " + serverPort + " timed out.");
                serverConnect = null;
                continue;
            } catch(ConnectException e){
                System.out.println("No server at " + serverHost + " on port "+ serverPort+".");
                serverConnect = null;
                continue;
            }
        } while(serverConnect == null);
        
        BufferedReader in = new BufferedReader(new InputStreamReader(serverConnect.getInputStream()));
        PrintWriter out = new PrintWriter(serverConnect.getOutputStream(),true);
        
        Thread t = new Thread(()->  {try{
                                        receiver(in);
                                    } catch(IOException e){

                                    }});
        t.start();

        do {
            line = readString();
            out.println(line);
        } while (!line.equalsIgnoreCase("stop"));

        in.close();
        out.close();
        serverConnect.close();
    }

    private static String readString () {

        // This method reads user input

        boolean again = false;
        String input = null;
        do {
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

    private static void receiver ( BufferedReader in ) throws IOException  {

        // This method listens for incoming packets

        String input = null;

        do {

            input = in.readLine();
            if (input != null)
                System.out.println(input);

        } while (input != null);

    }

    static class UnknownUserException extends IOException {

        // This exception is thrown if a user is not connected to the server

        public UnknownUserException(String message){
            super(message);
        }
    }


    static class MalformedInputException extends IOException{

        // This exception is thrown if a user's input does not contain any known command

        public MalformedInputException(String message){
            super(message);
        }
    }

}
