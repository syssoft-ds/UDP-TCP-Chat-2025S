package tcp.Server;

import java.io.IOException;
import java.net.Socket;

public class AcceptingThread extends Thread
{
    private Server server;
        
    public AcceptingThread(Server server)
    {
        this.server = server;                
    }

    public void run()
    {
        while (true)
        {
            try
            {
                Socket socket = server.socket.accept();
                Thread listenThread = new ListenThread(this.server, socket);
                listenThread.start();
            }
            catch (IOException e)
            {
                System.out.println(e.getMessage());
            }
        }
    }
}
