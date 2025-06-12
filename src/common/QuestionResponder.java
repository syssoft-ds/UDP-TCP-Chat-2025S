package common;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Map;
import java.util.function.Supplier;

public class QuestionResponder
{
    public static Map<String, Supplier<String>> questionDict = Map.of
    (
        "Sind Kartoffeln ein Gericht?", () -> "Ja",
        "Macht diese Aufgabe Spaß?", () -> "Nein",
        "Ist Rhababerkuchen lecker?", () -> "Ja", 
        "Was ist deine MAC-Adresse?", () -> getMacAdress()
    );

    private static String getMacAdress()
    {
        byte[] hardwareAddress;
        StringBuilder builder = new StringBuilder();
        try
        {
            Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces();
            while(networks.hasMoreElements())
            {
                NetworkInterface ni = networks.nextElement();
                if(ni.isLoopback() || ni.isVirtual() || !ni.isUp()) continue;
                
                hardwareAddress = ni.getHardwareAddress();
                
                if(hardwareAddress == null) continue;
                builder.append("Interface: " + ni.getName() + "\n");
                builder.append("MAC Adress: ");
                for (int i = 0; i < hardwareAddress.length; i++) {
                    builder.append(String.format("%02X%s", hardwareAddress[i], (i < hardwareAddress.length - 1) ? "-" : ""));
                }
                builder.append("\n");
            }
        }
        catch(SocketException e){System.out.println(e.getMessage());}
        return builder.toString();
    }
}