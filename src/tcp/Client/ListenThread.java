package tcp.Client;

import java.io.IOException;
import java.net.SocketException;
import java.util.Arrays;
import java.util.stream.Collectors;

import common.ChatProtocoll;
import common.Message;
import common.QuestionResponder;

import java.util.Set;
import java.util.function.Supplier;

public class ListenThread extends Thread
{
    private Client client;

    public ListenThread(Client client)
    {
        this.client = client;
        System.out.println(client.name);
    }

    public void run()
    {
        try
        {
            while(true)
            {
                Message message = new Message(client.socket.getInputStream());
                String[] split;
                switch (message.header) {
                    case ChatProtocoll.REGISTRATION:
                        client.names.add(message.payload);
                        break;
                    case ChatProtocoll.MESSAGE:
                        split = message.payload.split(ChatProtocoll.SEP, 2);

                        System.out.println(split[0] + ": " + split[1]);
                        break;
                    case ChatProtocoll.FETCH_CLIENTS:
                        split = message.payload.split(ChatProtocoll.SEP);
                        Set<String> newNames = Arrays.stream(split).filter(s -> !s.equals(client.name)).collect(Collectors.toSet());
                        client.names.addAll(newNames);
                        break;
                    case ChatProtocoll.QUESTION :
                        split = message.payload.split(ChatProtocoll.SEP);
                        Supplier<String> answerFunc = QuestionResponder.questionDict.get(split[1]);
                        if(answerFunc == null)
                        {
                            Message answer = new Message(ChatProtocoll.MESSAGE, split[0] + "," + client.name +  ",Diese Frage kann ich nicht beantworten");
                            client.send(answer);   
                        }
                        else
                        {
                            Message answer = new Message(ChatProtocoll.MESSAGE, split[0] + "," + client.name + "," + answerFunc.get());
                            client.send(answer);
                        }
                        break;
                }
            }
        }
        catch(SocketException e)
        {
            System.out.println(e.getMessage());
            System.out.println("Server closed Connection");
        }
        catch (IOException e)
        {
            System.out.println(e.getMessage());    
        }
        finally
        {
            try
            {
                client.names.clear();
                client.socket.close();
            }
            catch(IOException e)
            {
                System.out.println(e.getMessage());
            }
        }
    }
}