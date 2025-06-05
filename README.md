# Hausaufgabe

## A1

Ich habe das folgende Paket aufgezeichnet.

```[]
Internet Protocol Version 4, Src: 136.199.189.116, Dst: 192.168.0.21
    0100 .... = Version: 4
    .... 0101 = Header Length: 20 bytes (5)
    Differentiated Services Field: 0x00 (DSCP: CS0, ECN: Not-ECT)
        0000 00.. = Differentiated Services Codepoint: Default (0)
        .... ..00 = Explicit Congestion Notification: Not ECN-Capable Transport (0)
    Total Length: 40
    Identification: 0xd4a7 (54439)
    010. .... = Flags: 0x2, Don't fragment
        0... .... = Reserved bit: Not set
        .1.. .... = Don't fragment: Set
        ..0. .... = More fragments: Not set
    ...0 0000 0000 0000 = Fragment Offset: 0
    Time to Live: 111
    Protocol: TCP (6)
    Header Checksum: 0x302f [validation disabled]
    [Header checksum status: Unverified]
    Source Address: 136.199.189.116
    Destination Address: 192.168.0.21
    [Stream index: 0]
```

Daraus ergibt sich den entsprechenden Feldern des IPv4 Headers zugeordnet:

<table>
  <tr>
    <td style="text-align: center;">Version = 4</td>
    <td style="text-align: center;">Header length = 20 bytes</td>
    <td style="text-align: center;">tos = 0x0</td>
    <td style="text-align: center;" colspan="2"> Total Length = 40 bytes</td>
  </tr>
  <tr>
    <td style="text-align: center;" colspan="3">identification = 0xd4a7</td>
    <td style="text-align: center;">f = 0x2</td>
    <td style="text-align: center;">fragment offset = 0</td>
  </tr>
  <tr>
    <td style="text-align: center;">Time to live = 111</td>
    <td style="text-align: center;">protocol = TCP</td>
    <td style="text-align: center;">Header checksum = 0x302f</td>
  </tr>
  <tr>
    <td style="text-align: center;" colspan ="5"> source adress = 136.199.189.116</td>
  </tr>
  <tr>
    <td style="text-align: center;" colspan="5"> destination adress = 198.168.0.21</td>
  </tr>
</table>

Der Header dieses tcp-Packets sah dann wie folgt aus:

```[]
Transmission Control Protocol, Src Port: 993, Dst Port: 41394, Seq: 1, Ack: 2, Len: 0
    Source Port: 993
    Destination Port: 41394
    [Stream index: 0]
    [Stream Packet Number: 2]
    [Conversation completeness: Incomplete (12)]
        ..0. .... = RST: Absent
        ...0 .... = FIN: Absent
        .... 1... = Data: Present
        .... .1.. = ACK: Present
        .... ..0. = SYN-ACK: Absent
        .... ...0 = SYN: Absent
        [Completeness Flags: ··DA··]
    [TCP Segment Len: 0]
    Sequence Number: 1    (relative sequence number)
    Sequence Number (raw): 3446330184
    [Next Sequence Number: 1    (relative sequence number)]
    Acknowledgment Number: 2    (relative ack number)
    Acknowledgment number (raw): 2205970108
    0101 .... = Header Length: 20 bytes (5)
    Flags: 0x010 (ACK)
        000. .... .... = Reserved: Not set
        ...0 .... .... = Accurate ECN: Not set
        .... 0... .... = Congestion Window Reduced: Not set
        .... .0.. .... = ECN-Echo: Not set
        .... ..0. .... = Urgent: Not set
        .... ...1 .... = Acknowledgment: Set
        .... .... 0... = Push: Not set
        .... .... .0.. = Reset: Not set
        .... .... ..0. = Syn: Not set
        .... .... ...0 = Fin: Not set
        [TCP Flags: ·······A····]
    Window: 65526
    [Calculated window size: 65526]
    [Window size scaling factor: -1 (unknown)]
    Checksum: 0x7064 [unverified]
    [Checksum Status: Unverified]
    Urgent Pointer: 0
    [Timestamps]
        [Time since first frame in this TCP stream: 0.033011521 seconds]
        [Time since previous frame in this TCP stream: 0.033011521 seconds]
    [SEQ/ACK analysis]
        [TCP Analysis Flags]
            [Expert Info (Warning/Sequence): ACKed segment that wasn't captured (common at capture start)]
                [ACKed segment that wasn't captured (common at capture start)]
                [Severity level: Warning]
                [Group: Sequence]

```

Hieraus ergibt sich der folgende [TCP-Header](https://en.wikipedia.org/wiki/Transmission_Control_Protocol)

<table>
  <tr>
    <td style="text-align: center;" colspan="2">Source Port = 993</td>
    <td style="text-align: center;" colspan="2">Destination Port = 41391</td>
  </tr>
  <tr>
    <td style="text-align: center;" colspan="4">sequence Number = 3446330184</td>
  </tr>
  <tr>
    <td style="text-align: center;" colspan="4">Ackknowlegment Number = 2205970108</td>
  </tr>
  <tr>
    <td style="text-align: center;" colspan="2">Data Offset = 5 Reserved = 0 Flags=00010000</td>
    <td style="text-align: center;" colspan="2">window = 65526</td>
  </tr>
  <tr>
    <td style="text-align: center;" colspan="2">Checksum = 0x7064</td>
    <td style="text-align: center;" colspan="2">URG Pointer = 0</td>
  </tr>
</table>

Weiter habe ich auch noch ein udp-Packet aufgezeichnet

```[]
User Datagram Protocol, Src Port: 50647, Dst Port: 3702
    Source Port: 50647
    Destination Port: 3702
    Length: 615
    Checksum: 0xb330 [unverified]
    [Checksum Status: Unverified]
    [Stream index: 62]
    [Stream Packet Number: 1]
    [Timestamps]
        [Time since first frame: 0.000000000 seconds]
        [Time since previous frame: 0.000000000 seconds]
    UDP payload (607 bytes)
```

woraus sich der folgende [UDP-Header](https://en.wikipedia.org/wiki/User_Datagram_Protocol) ergibt:

<table>
  <tr>
    <td style="text-align: center;">Source Port = 50647</td>
    <td style="text-align: center;">Destination Port = 3702</td>
  </tr>
  <tr>
    <td style="text-align: center;">Length = 615</td>
    <td style="text-align: center;">Checksum = 0xb330</td>
  </tr>
</table>

## A2

`103.161.122.83` ist die IPv4-Adresse und die `18` gibt die Subnetzmaske an.

- Die Subnetzmaske ist eine 32 Bit Zahl, mit 18 führenden Einsen und 14 darauf folgenden Nullen.
- Die Netzwerkadresse ist die Bitweise Verundung der Subnetzmaske und der IPv4-Adresse.
- Die Broadcast-Adresse ist die Bitweise Veroderung der IPv4-Adresse mit der Negierung der Subnetzmaske.

| Beschreibung       | 32-Bit Darstellung                    | Dezimaldarstellung |
|--------------------|---------------------------------------|--------------------|
| IPv4-Adresse       | `01100111.10100001.01111010.01010011` | `103.161.122.83`   |
| Subnetzmaske       | `11111111.11111111.11000000.00000000` | `255.255.192.000`  |
| Netzadresse        | `01100111.10100001.01000000.00000000` | `103.161.64.0`     |
| Broadcast-Adresse  | `01100111.10100001.01111111.11111111` | `103.161.127.255`  |

Um herauszufinden, ob die IPv4-Adresse `103.161.193.83/18` im selben Netz liegt, berechnen wir die Netzadresse dieser Adresse und vergleichen sie mir der oben berechneten Adresse.

| Beschreibung      | 32-Bit Darstellung                    | Dezimaldarstellung |
|-------------------|---------------------------------------|--------------------|
| IPv4-Adresse      | `01100111.10100001.11000001.01010011` | `103.161.193.83`   |
| Subnetzmaske      | `11111111.11111111.11000000.00000000` | `255.255.192.0`    |
| Netzadresse       | `01100111.10100001.11000000.00000000` | `103.161.192.0`    |

Da die beiden Netzadressen unterschiedlich sind folgt, dass die beiden Adressen nicht im selben Netzwerk liegen.

## A3

Bei der Kommunikation von unterschiedlichen Implementierungen kommt es zu Problemen, da unterschiedliche Implementierungen den Payload der Pakete unterschiedlich interpretieren.
Zum Beispiel ist es wichtig sich auf eine Zeichenkodierung zu einigen.
Auch welche Typen von Nachrichten versendet werden können muss entsprechend einheitlich sein.
Zu guter Letzt muss auch noch für jeden Typ einer Nachricht die Reihenfolge der jeweiligen Bestandteile einer Nachricht festgelegt werden.
Bei TCP kommt erschwerend hinzu, dass die Kommunikation quasi "doppelt" standardisiert werden muss.
Es muss einmal die Richtung von Client zu Server und die Richtung von Server zu Client standardisiert werden.
Um diese Probleme zu beheben, werde ich zunächst einen Protokoll Header implementieren.
Anschließend werde ich eine Liste der Nachrichtentypen und ihrer Formatierung erstellen.

## A4

### Main_UDP ausführen

Um das Programm unter Linux zu starten, führen sie den Befehl

```bash!
./gradlew runMain_udp --args="<name>" --console=plain
```

oder unter Windows

```bash
./gradlew.bat runMain_udp --args="<name>" --console=plain
```

im Root-Verzeichnis des Projekts aus.
Eine Übersicht der verfügbaren Befehle kann durch das Drücken der Entertaste eingesehen werden. Instanzen müssen sich erst bei einer anderen Instanz registrieren, bevor sie miteinander kommunizieren können.

### Main_TCP ausführen

Um den `Server` unter Linux zu starten, führen sie den folgenden Befehl aus.

```bash!
./gradlew runMain_tcp --console=plain
```

Um den `Client` zu starten, führen Sie den folgenden Befehl aus.

```bash
./gradlew runMain_tcp --args="<name> <server_ip> <server_port>"
```

Analog mit Windows, starten sie den `Server` mit dem folgenden Befehl.

```bash
./gradlew.bat runMain_tcp --pconsole=plain
```

Der Client kann unter Windows mit dem folgenden Befehl gestartet werden.

```bash
./gradlew.bat runMain_tcp --args="<name> <server_ip> <server_port>"
```

Clients behalten eine dauerhafte Verbindung zum Server.
Diese wird genutzt, um alle Nachrichten zu versenden.
Der Server verarbeitet die Nachrichten und leitet diese an die adressierten Clients weiter.

### Nachrichten Protokoll

Ich verwende einen 5 Byte langen Header.
Die UDP-Implementierung nutzt einen 4096 Byte großen Buffer die gesamte Länge der Nachricht ist also auf 4096 Byte begrenzt.
Eine Nachricht sieht dann wie folgt aus. Die unterschiedlichen Daten im Payload werden durch ein Komma getrennt.

| Bytes     | 0   | 1-4                                        | 5 - (4096 - 5) |
|-----------|-----|--------------------------------------------|----------------|
| 0 - 4096  | Typ | Länge der Nachricht, big endian 32-Bit Zahl| Payload, UTF_8 |

Die entsprechenden Nachrichtentypen und ihre Formate für udp sind:

| Typ | Bezeichnung   | Absender      | Empfänger     | Payload Format                            |
|-----|---------------| --------------|---------------|-------------------------------------------|
|2    | Registrierung | Endpoint      | Endpoint      | $endpoint.name,endpoint.ip,endpoint.port$ |
|3    | Nachricht     | Endpoint      | Endpoint      | $sender.name,message$                     |
|6    | Frage         | Endpoint      | Endpoint      | $sender.name,question$                    |

Die entsprechenden Nachrichtentypen und ihre Formate für tcp sind:

| Typ | Bezeichnung       | Absender      | Empfänger     | Payload Format                                                |
|-----|-------------------|---------------|---------------|---------------------------------------------------------------|
|1    | Connection        | Client        | Server        | $sender.name$                                                 |
|2    | Registrierung     | Client        | Server        | $receiver.name,sender.name$                                   |
|2    | Registrierung     | Server        | Client        | $sender.name$                                                 |
|3    | Nachricht         | Client        | Server        | $receiver.name,sender.name,message$                           |
|3    | Nachricht         | Server        | Client        | $sender.name,message$                                         |
|4    | Nachricht an alle | Client        | Server        | $n,receiver_1.name,\dots,receiver_n.name,sender.name,message$ |
|5    | Client Liste      | Client        | Server        | $\lambda$ (leere Nachricht)                                   |
|5    | Client Liste      | Server        | Client        | $client_1.name, client_n.name$                                |
|6    | Frage             | Client        | Server        | $receiver.name,sender.name,question$                          |
|6    | Frage             | Server        | Client        | $sender.name,question$                                        |

Es ist empfehlenswert eine Nachrichtenklasse zu schreiben, die diese Details hinter Abstraktion verbirgt.
Das Protokoll ist so designt, dass die Nachrichtenklasse sowohl für udp als auch für tcp genutzt werden kann.
