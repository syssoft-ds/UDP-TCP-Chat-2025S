# VR-Project

## Aufgabe 1: *Protokoll-Header*

- Folgendes IPv4-Packet aus [IPv4-Mitschnitt](./Dateien_Aufgabe_1/IPv4_Package.pcapng) mit Paket Nr.1:
  
  ```
  b0 f2 08 94 57 fd e0 d5 5e 23 be 54 08 00 45 00  
  00 3c 18 13 00 00 80 01 00 00 c0 a8 b2 28 08 08  
  08 08 08 00 4d 52 00 01 00 09 61 62 63 64 65 66  
  67 68 69 6a 6b 6c 6d 6e 6f 70 71 72 73 74 75 76  
  77 61 62 63 64 65 66 67 68 69  
  ```

  | Bytes | Feld & Bedeutung |
  | ------|------|
  | b0 f2 08 94 57 fd e0 d5 5e 23 be 54 08 00 | Ethernet Header |
  | 45 | Version --> 4 && IHL --> 5; ==> 20 Bytes Header-Länge |
  | 00 | Type of Service --> Standard-Dienst |
  | 00 3c | Gesamtlänge --> 60 Bytes |
  | 18 13 | Identifikation --> Paket ID |
  | 00 00 | keine Fragmentierung |
  | 80 | Time to Live --> TTL = 128 |
  | 01 | ICMP Protokoll |
  | 00 00 | Header-Prüfsumme; hier leer |
  | c0 a8 b2 28 | Source-IP --> 192.168.178.40, also lokale IP |
  | 08 08 08 08 | 8.8.8.8 Google-DNS (Ping-Request) Zieladresse|

<br><br>

- Folgendes UDP-Paket aus [UDP-Mitschnitt](./Dateien_Aufgabe_1/UDP_Package.pcapng) mit Paket Nr.1:  

  ```
  02 00 00 00 45 00 00 36 a5 c5 00 00 80 11 00 00  
  c0 a8 b2 28 c0 a8 b2 28 13 89 13 88 00 22 a5 8a  
  4d 53 47 20 42 6f 62 20 4d 6f 69 6e 73 65 6e 2c  
  20 77 61 73 20 67 65 68 74 3f  
  ```
  
  | Bytes | Feld & Bedeutung |
  | ----- | ---------------- |
  | 02 00 00 00 | Dummy-Daten von der Loopback-Schnittstelle |
  | 45 00 00 36 a5 c5 00 00 80 11 00 00 c0 a8 b2 28 c0 a8 b2 28 | IPv4 Header |
  | 13 89 | Source Port --> 5001 |
  | 13 88 | Dest. Port --> 5000 |
  | 00 22 | Länge --> 34 Bytes |
  | a5 8a | UDP-Prüfsumme zur Fehlererkennung | 
  | 4d 53 47 20 42 6f 62 20 4d 6f 69 6e 73 65 6e 2c 20 77 61 73 20 67 65 68 74 3f | UDP-Payload --> ASCII Darstellung: MSG Bob Moinsen, was geht? |

  **Nach IPv4 Header sind das die Bestandteile des UDP-Headers bis zum UDP-Payload**  

<br><br>

- Folgendes TCP-Paket aus [TCP-Mitschnitt](./Dateien_Aufgabe_1/TCP_Package.pcapng) mit Paket Nr.1:  

  ```
  02 00 00 00 45 00 00 47 d3 ef 40 00 80 06 00 00  
  7f 00 00 01 7f 00 00 01 c5 79 13 88 d1 52 f9 52  
  00 15 fc 37 50 18 27 f9 84 c2 00 00 73 65 6e 64  
  20 41 6c 69 63 65 20 4d 6f 69 6e 73 65 6e 2c 20  
  77 61 73 20 67 65 68 74 3f 0d 0a  
  ```
  
  | Bytes | Feld & Bedeutung |
  | ----- | ---------------- |
  | 02 00 00 00 | Dummy-Daten von der Loopback-Schnittstelle |
  | 45 00 00 47 d3 ef 40 00 80 06 00 00 7f 00 00 01 7f 00 00 01 | IPv4 Header |
  | c5 79 | Source Port --> lokaler zufälliger Port |
  | 13 88 | Dest. Port --> 5000 (Port des TCP-Servers) |
  | d1 52 f9 52 | Sequence --> Start der TCP-Nutzdaten |
  | 00 15 fc 37 | Erwarteter Bytewert vom Empfänger |
  | 50 18 | Data Offset & Flags --> Headerlänge + Flags: ACK + PSH |
  | 27 f9 | Größe des Empfangpuffers |
  | 84 c2 | TCP-Prüfsumme zur Fehlererkennung |
  | 00 00 | irgendein Pointer nicht gesetzt |
  | 73 65 6e 64 20 41 6c 69 63 65 20 4d 6f 69 6e 73 65 6e 2c 20 77 61 73 20 67 65 68 74 3f 0d 0a | TCP-Payload |

  **Nach IPv4 Header bilden die Bestandteile den TCP-Header bis zum TCP-Payload**  

<br><br>

## Aufgabe 2: *CIDR*  

gegeben:  103.161.122.83/18  

--> 103.161.122.83 bildet hiervon die IPv4-Adresse, aus 4 Teilen mit je 8 Bits zusammengesetzt (je Teil 0-255 darstellbar) --> insgesamt 32 Bits lang  

/18 ist die Präfix- bzw. Netzmaskenlänge. Hier gehören die ersten 18 Bits der Adresse zum Nertzwerkanteil. Die restlichen Bits (32-18 = 14), also 14 verbleibend, gehören dann zum Hostanteil bzw. sind für diesen reserviert.  

1. *Subnetzmaske*
Ein /18-Netz bedeutet, dass die ersten 18 Bits der 32 auf 1 gesetzt werden:
    --> 11111111.11111111.11000000.00000000
    in Dezimal ergibt das dann: 255.255.192.0

2. *Netzwerkadresse*
Erst IP-Adresse und Subnetzmaske in Binärform aufschreiben:
    103.161.11.83 --> 01100111.10100001.01111010.01010011
    255.255.192.0 --> 11111111.11111111.11000000.00000000

Jetzt Bitweises-UND anwenden (also neue Adresse wird überall 1 gesetzt wo bei IP UND Subnetzmaske 1 gesetzt sind)  
```
IP:        01100111.10100001.01111010.01010011
Maske:     11111111.11111111.11000000.00000000
----------------------------------------------
Ergebnis:  01100111.10100001.01000000.00000000
```
    In Binär folgt dann --> 103.192.64.0/18

3. *Broadcastadresse*
Hier werden nun der Hostanteil also die letzten 14 Bits der Netzwerkadresse also Hostbits auf 1 gesetzt

    01100111.10100001.01000000.00000000 --> 01100111.10100001.01111111.11111111
    In Binär folgt --> 103.161.127.255

4. *Subnetzbereich* (Gültige Hosts im Netz)
Netzwerkadresse + 1: um 1 Bit erhöht --> 103.192.64.1
                    
Subnetzmaske - 1: 103.161.127.255 - 1 --> 103.161.127.254


5. *Liegt die 103.161.122.83/18 im selben Netz wie 103.161.193.83/18?*

Netzwerkadresse von 103.161.193.83/18:
    - 103.161.193.83 in Binär --> 01100111.10100001.11000001.01010011
    - /18 also gleiche Subnetzmaske wie vorhin
    - Bitweises-UND für Netzwerkadresse:
    
    IP:         01100111.10100001.11000001.01010011
    Maske:      11111111.11111111.11000000.00000000
    -----------------------------------------------
    Ergebnis:   01100111.10100001.11000000.00000000
    

  in Binär folgt --> 103.161.192.0

  Wenn wir diese mit der Netzwerkadresse der anderen 103.161.64.0/18 vergleichen, fällt auf dass diese also nicht im selben Netz liegen.



## Aufgabe 3: *Kommunikation zwischen Implementationen*

  Beim Versuch der Kommunikation unser unterschiedlichen Implementierungen ist zunächst kein erfolgreicher Nachrichtenaustausch Zustande gekommen. Bei beiden Protokollen also TCP und UDP war gleichermaßen das Lesen bzw. Parsen der empfangen Nachricht das Problem, während der andere Kommilitone die Nachricht normal mit "send <targetName> <message>" versand, hatte ich zunächst anstatt send ein MSG angehangen und beim Empfangen auf eine Nachricht die mit MSG anfängt gewartet. Diese grundlegende Problematik war letztlich auch das Schlüsselelement für Aufgabe 4, dass man es allgemein kompatibel macht und sich auf eine einheitlich Syntax hier einigen müsste. Ein weiterer kleiner Aspekt war zusätzlich beim TCPChat, dass bei unseren jeweiligen Server auf eine bestimmte Nachricht gewartet wurde und wir eben durch unsere unterschiedlichen Implementierungen auch nicht die korrekten Nachrichten an die jeweiligen Server schickten und diese somit die Clients nicht registrierte. Lösung war hier eigentlich gleich, dass man sich auf eine festgelegte Nachricht oder so einigt, die der Server erwartet.
