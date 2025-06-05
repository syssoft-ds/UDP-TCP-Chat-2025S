package udp;
import java.io.IOException;
import udp.Endpoint.Endpoint;

public class Main_UDP
{
    public static void main(String[] args) throws IOException 
    {
        if(args.length != 1)
        {
            System.out.println("usage: Main_UDP <name>");
            return;
        }
        String name = args[0];
        Endpoint endpoint = new Endpoint(name);
        endpoint.run();
    }
}