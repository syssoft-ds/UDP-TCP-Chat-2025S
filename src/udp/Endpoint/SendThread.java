package udp.Endpoint;

import common.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import org.javatuples.Pair;
import common.ChatProtocoll;

public class SendThread extends Thread
{

    private Endpoint endpoint;

    public SendThread(Endpoint endpoint)
    {
        this.endpoint = endpoint;
    }

    public void run()
    {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        
        while (true)
        {
            try
            {
                String input = br.readLine();

                if(input.startsWith("register"))
                {
                    String[] command = input.split(" ", 3);
                    if(command.length != 3)
                    {
                        printHelp();
                    }
                    String[] args = new String[3];
                    args[0] = endpoint.name;
                    args[1] = endpoint.address.getHostAddress();
                    args[2] = Integer.toString(endpoint.port);
                    
                    InetAddress peerAddress = InetAddress.getByName(command[1]);
                    int peerPort = Integer.valueOf(command[2]);
                    Message registerMessage = new Message(ChatProtocoll.REGISTRATION, args);
                    endpoint.send(peerAddress, peerPort, registerMessage);
                }
                else if(input.startsWith("send"))
                {
                    String[] command = input.split(" ", 3);

                    if(command.length != 3) printHelp();
                    
                    String targetName = command[1];
                    Pair<InetAddress, Integer> peer = endpoint.addressDict.get(targetName);

                    if(peer == null)
                    {
                        printHelp("target: " + targetName + " not registered");
                        continue;
                    }
                    Message message = new Message(ChatProtocoll.MESSAGE, endpoint.name + ChatProtocoll.SEP + command[2]);
                    endpoint.send(peer.getValue0(), peer.getValue1(), message);
                }
                else if(input.startsWith("all"))
                {
                    String[] command = input.split(" ", 2);
                    
                    if(command.length != 2) printHelp();
                    
                    Message message = new Message(ChatProtocoll.MESSAGE, endpoint.name + ChatProtocoll.SEP + command[1]);
                    endpoint.sendAll(message);
                }
                else if(input.startsWith("ask"))
                {
                    String[] command = input.split(" ", 3);

                    if(command.length != 3) printHelp();

                    String targetName = command[1];
                    Pair<InetAddress, Integer> peer = endpoint.addressDict.get(targetName);

                    if(peer == null)
                    {
                        printHelp("target: " + targetName + " not registered");
                        continue;
                    }

                    Message question = new Message(ChatProtocoll.QUESTION, endpoint.name + ChatProtocoll.SEP + command[2]);
                    endpoint.send(peer.getValue0(), peer.getValue1(), question);
                }
                else if (input.startsWith("list"))
                {
                    endpoint.addressDict.keySet().forEach(System.out::println);
                }
                else 
                {
                    printHelp();
                }
            }
            catch (IOException e)
            {
                System.out.printf("Exception: %s\n",e.getMessage());
            }
        }
    }

    private void printHelp(String help)
    {
        System.out.println(help);
    }

    private void printHelp()
    {
        String help =   "available commands are:" + "\n" +
                        "   send <name> <message>" + "\n" +
                        "   register <ip> <port>" + "\n" +
                        "   all <message>" + "\n" +
                        "   ask <name> <question>" + "\n" +
                        "   list";
        System.out.println(help);
    }
}