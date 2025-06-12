package tcp.Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import common.ChatProtocoll;
import common.Message;

public class SendThread extends Thread
{
    private Client client;

    public SendThread(Client client)
    {
        this.client = client;
    }

    public void run()
    { 
        try
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            //Connecting Request
            Message connectRequest = new Message(ChatProtocoll.CONNECT_REQUEST, client.name);
            client.send(connectRequest);
            while(true)
            {
                String input = reader.readLine();
                Message message = constructMessage(input);
                if(message == null) continue;
                client.send(message);
            }
        }
        catch (IOException e)
        {
            System.out.printf("Exception: %s\n",e.getMessage());
        }
    }
    
    private Message constructMessage(String input)
    {
        if(input.startsWith("register"))
        {
            String[] command = input.split(" ", 2);
            if(command.length != 2)
            {
                printHelp();
                return null;
            }
            String[] args = new String[2];
            args[0] = command[1];
            args[1] = client.name;
            return new Message(ChatProtocoll.REGISTRATION, args);
        }
        else if(input.startsWith("send"))
        {
            String[] command = input.split(" ", 3);
            if(command.length != 3)
            {
                printHelp();
                return null;
            }
            String targetName = command[1];
            if(!client.names.contains(targetName))
            {
                printHelp("target " + targetName + " not registered");
                return null;
            }
            String[] args = new String[3];
            args[0] = targetName;
            args[1] = client.name;
            args[2] = command[2];
            return new Message(ChatProtocoll.MESSAGE, args);
        }
        else if(input.startsWith("all"))
        {
            String[] command = input.split(" ", 2);
            if(command.length != 2)
            {
                printHelp();
                return null;
            }
            String[] args = new String[client.names.size() + 3];
            args[0] = Integer.toString(client.names.size());
            args[args.length-2] = client.name;
            args[args.length-1] = command [1];
            int i = 1;
            for(String n : client.names)
            {
                args[i++] = n;
            }
            return new Message(ChatProtocoll.MESSAGE_ALL, args);
        }
        else if(input.startsWith("fetch"))
        {
            return new Message(ChatProtocoll.FETCH_CLIENTS,"");
        }
        else if(input.startsWith("ask"))
        {
            String[] command = input.split(" ", 3);
            if(command.length != 3)
            {
                printHelp();
                return null;
            }
            String targetName = command[1];
            if(!client.names.contains(targetName))
            {
                printHelp("target: " + targetName + " not registered");
                return null;
            }
            String[] args = new String[3];
            args[0] = targetName;
            args[1] = client.name;
            args[2] = command[2];
            return new Message(ChatProtocoll.QUESTION, args);
        }
        else if (input.startsWith("list"))
        {
            client.names.forEach(System.out::println);
        }
        else 
        {
            printHelp();
            return null;
        }
        return null;
    }

    private void printHelp()
    {
        String help =   "available commands are:" + "\n" +
                        "   register <name>" + "\n" +
                        "   send <name> <message>" + "\n" +
                        "   all <message>" + "\n" +
                        "   fetch" + "\n" +
                        "   ask <name> <question>" + "\n" +
                        "   list";
        System.out.println(help);
    }

    private void printHelp(String message)
    {
        System.out.println(message);
    }
}
