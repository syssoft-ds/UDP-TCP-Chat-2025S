package common;

import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ChatProtocoll {

    private ChatProtocoll(){}

    public static final Charset CHARSET = StandardCharsets.UTF_8;
    public static final ByteOrder BYTE_ORDER = ByteOrder.BIG_ENDIAN;
    public static final String SEP = ",";

    public static final int CONNECT_REQUEST = 0x01;
    public static final int REGISTRATION = 0x02;
    public static final int MESSAGE = 0x03;
    public static final int MESSAGE_ALL = 0x04;
    public static final int FETCH_CLIENTS = 0x05;
    public static final int QUESTION = 0x06;

}
