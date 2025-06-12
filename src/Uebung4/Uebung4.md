Abgabe von Maxim Smirnov

# Aufgabe 1: Protokoll-Header

Mit dem Filter 'ip' in Wireshark habe ich ein zufälliges IPv4-Paket ausgewählt und den IPv4-Header analysiert.
Die Daten des Pakets sehen wie folgt aus:

![IPv4](PNG/IPv4.PNG)

v = 4                                    // Version des Protokolls (IPv4)  
hl = 20 bytes                            // Header Length  
tos = 0x00 = 0                           // Type of Service, hier als Differentiated Services gelistet  
Total Length = 40 bytes                  // Gesamtlänge des Pakets  
identification = 0xdbb9 = 56249          // Identifikation des Pakets  
f = 0x2 = 2                              
Fragment offset(13) = 0     
Time to live = 128                       // TTL, wie lange das Paket im Netzwerk verbleiben darf  
protocol = TCP (6)                       // Protokoll, hier TCP  
Header checksum = 0x0000 = 0             // Prüfsumme des Headers  
source address = 192.168.178.21          // Absenderadresse  
destination address = 140.82.121.6       // Empfängeradresse  

Jetzt ein Paket mit Analyse des UDP-Headers:  
 
![UDPHeader](PNG/UDPHeader.PNG)  

Source Port = 63316                      // Absenderport  
Destination Port = 443                   // Empfängerport  
Length = 547 bytes                       // Länge des UDP-Pakets  
Checksum = 0x6112 = 24850                // Prüfsumme des UDP-Headers  




und ein Paket mit Analyse des TCP-Headers:  

![TCPHeader](PNG/TCPHeader.PNG)  

Source Port = 2576                  // Absenderport  
Destination Port = 443              // Empfängerport  
Sequence Number = 0                 // Sequenznummer  
Acknowledgment Number = 0           // Bestätigungsnummer  
Flags = 0x002 = 2                   
Window = 65535                     
Checksum = 0x783c = 30780           // Prüfsumme des TCP-Headers  
Urgent Pointer = 0                



# Aufgabe 2: CIDR

103.161.122.83 ist die IP-Adresse, und die 18 zeigt an wie viele Bits für das Netzwerk verwendet werden -> CIDR-Präfix  

Wenn man 103.161.122.83 in binär umwandelt, erhält man 01100111.10100001.01111010.01010011.  
Die ersten 18 Bits (01100111.10100001.01) repräsentieren das Netzwerk, und die restlichen 14 Bits (111010.01010011) repräsentieren die Hosts im Netzwerk.  

Für die Subnetzmaske muss man die ersten 18 Bits auf 1 setzen und die restlichen 14 Bits auf 0 setzen. Das ergibt die Subnetzmaske:  
11111111.11111111.11000000.00000000 = 255.255.192.0

Für die Broadcast-Adresse setzt man die Host-Bits auf 1 in der IP-Adresse. Das ergibt die Broadcast-Adresse:  
01100111.10100001.01111111.11111111 = 103.161.127.255  

Für die Netzwerkadresse muss man die IP-Adresse mit der Subnetzmaske bitweise mit einer logischen UND-Verknüpfung verknüpfen.  
Das ergibt die Netzwerkadresse: 01100111.10100001.01000000.00000000 =  103.161.64.0  

Um zu überprüfen, ob die IP-Adresse 103.161.193.83/18 im selben Netzwerk liegt, muss die Netzwerkadresse der beiden IP-Adressen gleich sein.  

103.161.193.83 in binär: 01100111.10100001.11000001.01010011  
Die Subnetzmaske bleibt gleich: 11111111.11111111.11000000.00000000  
Die Netzwerkadresse ist: 01100111.10100001.11000000.00000000 = 103.161.192.0 was nicht gleich 103.161.64.0 ist. Also liegen die beiden IP-Adressen nicht im selben Netzwerk.  

# Aufgabe 3: Kommunikation zwischen Implementationen

Ich habe die Implementationen von mir mit der Musterlösung verglichen.
Bei UDP-Chat war sofort das Problem, dass ich mich nicht richtig zwischen den beiden Implementationen verbinden konnte,
da die Musterlösung auf Englisch die register Funktion verwendet, während meine auf Deutsch ist. 

Bei der TCP-Chat Implementierung hab ich meinen Server gestartet und dann die Clients aus der Musterlösung verbunden.
Dort traten ähnliche Probleme auf, da die Register-Funktion bei mir anders umgesetzt ist, wodurch sich die Clients nicht auf meinem Server registrieren konnten.

Fazit ist, dass verschiedene Implementationen nicht miteinander funktionieren können, wenn man sich nicht auf Regeln einigt.
Man muss die Nachrichten die zwischen den Programmen ausgetauscht werden, standardisieren, damit die Kommunikation funktionieren kann.

# Aufgabe 4: Programmierung

Ich habe die Anforderungen in die Musterlösung aus der dritten Übung eingebaut, da diese Implementierung kompakter war als meine eigene. Ein weiterer Grund war, dass in der Musterlösung der TCP-Server und der TCP-Client in zwei getrennten Klassen umgesetzt sind. In meiner ursprünglichen Lösung waren beide in einer einzigen Klasse kombiniert, was die Struktur unübersichtlich machte.

Um die Kommunikation mit anderen Implementierungen zu ermöglichen, habe ich die Spezifikationen von Paul Simon verwendet, die er in dem Google-Dokument geteilt hat. Die Spezifikationen sind unter folgendem Link zu finden: https://github.com/paul99simon/UDP-TCP-Chat-2025S. Ich habe außerdem seinen Code geklont und getestet, ob die Kommunikation zwischen seiner und meiner Implementierung funktioniert.

Zunächst habe ich versucht, die UDP-Chat-Implementierung anzupassen. Laut Spezifikation soll die Registrierung über ein bestimmtes Payload-Format erfolgen, nämlich in der Form endpoint.name,endpoint.ip,endpoint.port. Zusätzlich wird ein 5-Bit-Header verwendet, der sowohl den Nachrichtentyp als auch die Länge der Nachricht enthält. Da ich mir bei der Umsetzung des Headers unsicher war, habe ich ChatGPT um Hilfe gebeten. Auf dieser Grundlage entstand die register()-Funktion, die ich in die UDP-Chat-Implementierung eingebaut habe.

Diese Funktion habe ich als Erstes getestet, und sie hat wie erwartet funktioniert.

Anschließend habe ich die restlichen Spezifikationen umgesetzt, die geforderten Methoden aus der Aufgabenstellung ergänzt und alles funktionierte. Dann hab ich auch die TCP-Chat-Implementierung angepasst.

Ein Problem ergab sich dadurch, dass Pauls TCP-Chat Implementierung in einigen Punkten anders aufgebaut ist. Nach meinem Verständnis registrieren sich seine Clients explizit bei anderen Clients. Zusätzlich müssen sie zunächst die Kontaktliste vom Server abrufen, bevor sie Nachrichten verschicken können.

Ich habe mich hingegen an der Logik der Musterlösung orientiert. In dieser Variante kennt man die Kontakte bereits und kann direkt Nachrichten versenden, also die Kontakte werden auf dem Server für alle gespeichert. Die Registrierung erfolgt automatisch, sobald sich ein Client mit dem Server verbindet.

Das bedeutet, dass in meiner Implementierung entsprechend der dritten Musterlösung nur die Verbindung hergestellt wird. Die zusätzlichen Registrierungsnachrichten habe ich weggelassen. Mein Server kann die Anfragen von Pauls Clients verarbeiten, aber meine eigene Implementierung regelt die Registrierung automatisch.

Außerdem habe ich für die vordefinierten Fragen diesselben wie Paul verwendet und eigene Antworten bei mir eingebaut. Also folgende Fragen:

Sind Kartoffeln ein Gericht?

Macht diese Aufgabe Spaß?

Ist Rhababerkuchen lecker?

Was ist deine MAC-Adresse?

Beispiel Konversation mit meinem Server und meinem Client (Ohne irrelevante Debug-Ausgaben):

| Zeit | Server                              | Client 1  (Charlie)             | Client 2   (Charlie42)                                                                      |
|------|-------------------------------------|---------------------------------|---------------------------------------------------------------------------------------------|
| 0    | Server started on IP 192.168.178.45 on Port 1444. <br/>Use "quit" to exit program.  | |                                                                                             |
| 1    |                                     | Registered as Charlie.          |                                                                                             |
| 2    |      |                                 | Registered as Charlie.                                                                      |
| 3    |           |                                 | Eingegeben: sendall hi                                                                      |
| 4    |    Empfangene Nachricht: Typ=4, Länge=30, Nachricht=2,Charlie,Charlie42,Charlie,hi       |   |                                                                                             |
| 5    |         | Nachricht von Charlie42: hi     | Nachricht von Charlie42: hi                                                                 |
| 6    |         |                                 | Eingegeben: ask Charlie Sind Kartoffeln ein Gericht?                                        |
| 7    |         | Frage von Charlie42: Sind Kartoffeln ein Gericht? |                                                                                             |
| 8    |         |                                 | Nachricht von Charlie: Ja, Kartoffeln können als Beilage oder Hauptgericht serviert werden. |


Beispiel Konversation mit Paul's Server auf meinem PC, meinem Client und einem Client von Paul (Ohne irrelevante Debug-Ausgaben):

| Zeit |   Paul's Server                | Client 1  (Charlie)    Mein Client                                               | Client 2   (Bob)    Paul's Client                                             |
|------|--------------------------------|----------------------------------------------------------------------------------|-------------------------------------------------------------------------------|
| 0    | Server Adress is: 192.168.178.45 5948 |                                                                                  |                                                                               |
| 1    |                                | Registered as Charlie.                                                           |                                                                               |
| 2    |                                |                                                                                  | Eingegeben: register Bob                                                      |
| 3    |                                |                                                                                  | Eingegeben: fetch                                                             |
| 4    |                                |                                                                                  | Eingegeben: list                                                              |
| 5    |                                |                                                                                  | Bob <br/> Charlie                                                             |
| 6    |                                |                                                                                  | Eingegeben: all Hallo                                                         |
| 7    |                                | Nachricht von Bob: Hallo                                                         | Bob: Hallo                                                                    |
| 8    |                                |                                                                                  | Eingegeben: ask Charlie Sind Kartoffeln ein Gericht?                          |
| 9    |                                | Frage von Bob: Sind Kartoffeln ein Gericht?                                      |                                                                               |
| 10   |                                |                                                                                  | Charlie: Ja, Kartoffeln können als Beilage oder Hauptgericht serviert werden. |
| 11   |                                | Eingegeben : ask Bob Was ist deine MAC-Adresse?                                  |                                                                               |
| 12   |                                | Nachricht von Bob: Interface: ethernet_0 <br/> MAC Adress: 74-5D-22-A8-CE-43 ... |                                                                               |


Das hat auch funktioniert, als ich meinen Server zusammen mit Pauls Client verwendet habe. Die Kommunikation zwischen den Implementierungen funktioniert also.

Mein Fazit ist, dass man mit klaren Regeln die Kommunikation zwischen verschiedenen Implementierungen ermöglichen kann.

Ich hätte die Aufgabe wahrscheinlich einfach über Strings gelöst, anstatt einen Header wie Paul zu verwenden. Trotzdem war es interessant, Pauls Protokoll zu implementieren, auch wenn es für mich etwas kompliziert war.
Ein Grund dafür ist, dass in meiner Implementierung des TCP-Clients nicht jeder Client eine Liste der anderen Clients speichert. Stattdessen verwaltet der Server diese Liste, und die Clients registrieren sich bei ihm.

Deshalb ist die Client-Liste für jeden Benutzer in meinem Ansatz eigentlich nicht notwendig. Damit es jedoch mit Pauls Implementierung funktioniert, musste ich sie trotzdem einbauen.
Sein Protokoll setzt das indirekt voraus, was mir am Anfang nicht bewusst war. Ein Beispiel dafür ist der Nachrichtentyp „Nachricht an alle“, der auf die Liste der Clients zugreift.

Wenn ich mehr Zeit gehabt hätte, hätte ich vielleicht versucht, das Protokoll gemeinsam mit ihm zu vereinfachen. Das hätte allerdings bedeutet, dass er seine Implementierung hätte anpassen müssen, was ich vermeiden wollte, da die Zeit knapp war.

Unten ist noch das Protokoll als Kopie von Paul.  


# Nachrichtenprotokoll

## Header
Das Nachrichtenprotokoll verwendet einen 5 Byte langen Header. Die UDP-Implementierung nutzt einen 4096 Byte großen Buffer, wodurch die gesamte Länge der Nachricht auf 4096 Byte begrenzt ist. Eine Nachricht ist wie folgt aufgebaut:

| Bytes         | Beschreibung                          |
|---------------|---------------------------------------|
| 0             | Typ                                   |
| 1-4           | Länge der Nachricht (big endian, 32-Bit Zahl) |
| 5 - (4096 - 5)| Payload (UTF-8)                       |

Die unterschiedlichen Daten im Payload werden durch ein Komma getrennt.

---

## Nachrichtentypen und Formate für UDP

| Typ | Bezeichnung   | Absender  | Empfänger | Payload Format                                                                 |
|-----|---------------|-----------|-----------|--------------------------------------------------------------------------------|
| 2   | Registrierung | Endpoint  | Endpoint  | `endpoint.name,endpoint.ip,endpoint.port`                                      |
| 3   | Nachricht     | Endpoint  | Endpoint  | `sender.name,message`                                                          |
| 6   | Frage         | Endpoint  | Endpoint  | `sender.name,question`                                                         |

---

## Nachrichtentypen und Formate für TCP

| Typ | Bezeichnung         | Absender | Empfänger | Payload Format                                                                 | Hinweis                                                       |
|-----|---------------------|----------|-----------|--------------------------------------------------------------------------------|---------------------------------------------------------------|
| 1   | Connection          | Client   | Server    | `sender.name`                                                                  | Nicht richtig verwendet, da Registrierung automatisch erfolgt |
| 2   | Registrierung       | Client   | Server    | `receiver.name,sender.name`                                                    | Nicht richtig verwendet, da Registrierung automatisch erfolgt |
| 2   | Registrierung       | Server   | Client    | `sender.name`                                                                  | Nicht richtig verwendet, da Registrierung automatisch erfolgt |
| 3   | Nachricht           | Client   | Server    | `receiver.name,sender.name,message`                                            |                                                               |
| 3   | Nachricht           | Server   | Client    | `sender.name,message`                                                          |                                                               |
| 4   | Nachricht an alle   | Client   | Server    | `n,receiver1.name,…,receivern.name,sender.name,message`                        |                                                               |
| 5   | Client Liste        | Client   | Server    | `λ` (leere Nachricht)                                                          |                                                               |
| 5   | Client Liste        | Server   | Client    | `client1.name,clientn.name`                                                    |                                                               |
| 6   | Frage               | Client   | Server    | `receiver.name,sender.name,question`                                           |                                                               |
| 6   | Frage               | Server   | Client    | `sender.name,question`                                                         |                                                               |

---
