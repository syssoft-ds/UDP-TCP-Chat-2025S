package tcp;

import tcp.Server.Server;
import tcp.Client.Client;

import java.io.IOException;

public class Main_TCP {

    private static void printHelp()
    {
        String help =   "Usage of Main_TCP:\n" + 
                        "   Main_TCP //start server\n" +
                        "   Main_TCP <server_ip> <server_port> //start client and register at server";
        System.out.println(help);
    }

    public static void main(String[] args) throws IOException {
        if(args.length == 0)
        {
            Server server = new Server();
            server.run();
        }
        else if(args.length == 3)
        {
            String name = args[0];
            String ip = args[1];
            int port = Integer.valueOf(args[2]);
            Client client = new Client(name, ip, port);
            client.run();
        }
        else
        {
            printHelp();
        }
    }
}