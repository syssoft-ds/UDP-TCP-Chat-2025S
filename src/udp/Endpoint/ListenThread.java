package udp.Endpoint;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.function.Supplier;

import org.javatuples.Pair;

import common.ChatProtocoll;
import common.Message;
import common.QuestionResponder;

public class ListenThread extends Thread
{

    private Endpoint endpoint;
    private byte[] buffer;

    public ListenThread(Endpoint endpoint)
    {
        this.endpoint = endpoint;
        this.buffer = new byte[ChatProtocoll.PACKET_SIZE];
    }

    public void run()
    {
        while (true)
        {            
            try
            {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                endpoint.s.receive(packet);
                Message message = new Message(buffer);
                
                String[] split;            
                String peerName;
                InetAddress peerAddr;
                int peerPort;            

                switch(message.header)
                {
                    case ChatProtocoll.REGISTRATION:
                        split = message.payload.split(ChatProtocoll.SEP);
                        peerName = split[0];
                        peerAddr = InetAddress.getByName(split[1]);
                        peerPort = Integer.valueOf(split[2].trim());
                        endpoint.addressDict.putIfAbsent(peerName, new Pair<InetAddress,Integer>(peerAddr, peerPort));        
                        break;
                    case ChatProtocoll.MESSAGE:
                        split = message.payload.split(ChatProtocoll.SEP, 2);
                        System.out.println(split[0] + ": " + split[1]);
                        break;
                    case ChatProtocoll.QUESTION:
                        split = message.payload.split(ChatProtocoll.SEP, 2);
                        peerName = split[0];
                        Pair<InetAddress, Integer> peer = endpoint.addressDict.get(peerName);
                        
                        if(peer == null) continue;
                        
                        peerAddr = peer.getValue0();
                        peerPort = peer.getValue1();

                        Supplier<String> answerFunc = QuestionResponder.questionDict.get(split[1]);                   
                        Message answer;
                        if(answerFunc == null)
                        {
                            answer = new Message(ChatProtocoll.MESSAGE, endpoint.name + ChatProtocoll.SEP + "Diese Frage kann ich nicht beantworten");
                        }
                        else
                        {   
                            answer = new Message(ChatProtocoll.MESSAGE, endpoint.name  + ChatProtocoll.SEP + answerFunc.get());
                        }
                        {
                            endpoint.send(peerAddr, peerPort, answer);
                        }
                        break;
                }
            }
            catch(IOException e)
            {
                System.out.println(e.getMessage());
            }
        }
    }
}