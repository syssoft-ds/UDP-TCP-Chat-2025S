package common;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Message {
    
    public int header;
    public int length;
    public String payload;
    
    public Message(int header, String payload)
    {
        this.header = header;
        this.length = payload.getBytes(ChatProtocoll.CHARSET).length;
        this.payload = payload;
    }

    public Message(int header, String[] payload)
    {
        StringBuilder builder = new StringBuilder();

        for(int i = 0; i < payload.length; i++)
        {
            builder.append(payload[i]).append(ChatProtocoll.SEP);
        }

        builder.deleteCharAt(builder.length()-1);
        this.header = header;
        this.payload = builder.toString();
        this.length = this.payload.getBytes(ChatProtocoll.CHARSET).length;
    }

    public Message(byte[] payload)
    {
        this.header = payload[0];
        this.length = payload.length - 5;
        this.payload = new String(Arrays.copyOfRange(payload, 4, payload.length));
    }

    public Message(InputStream in) throws IOException, SocketException
    {
        this.header = in.read();
        if(header == -1) throw new SocketException("Connection was closed");
        {
            this.length = ByteBuffer.wrap(in.readNBytes(4)).order(ChatProtocoll.BYTE_ORDER).getInt();
            this.payload = new String(in.readNBytes(length), ChatProtocoll.CHARSET);
        }
    }

    public byte[] getBytes()
    {
        byte[] arr = new byte[length + 5];
        arr[0] = (byte) header;
        ByteBuffer buffer = ByteBuffer.allocate(4).order(ChatProtocoll.BYTE_ORDER).putInt(length);
        arr[1] = buffer.get(0);
        arr[2] = buffer.get(1);
        arr[3] = buffer.get(2);
        arr[4] = buffer.get(3);

        byte[] messageBytes = payload.getBytes(ChatProtocoll.CHARSET);
        int j = 5;
        for(int i = 0; i < messageBytes.length; i++)
        {
            arr[j++] = messageBytes[i];
        }
        return arr;
    }

    @Override
    public String toString()
    {
        return header+ ":" + length + ":" + payload;
    }
}