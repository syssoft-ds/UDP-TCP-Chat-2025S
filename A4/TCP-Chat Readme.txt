1. Starte eine Instanz des Programms als Server und den Rest als Cleints in verschiedenen Terminals. 

	python TCP-Chat.py -l 12345 		(Server)
	python TCP-Chat.py <server_ip> 12345 	(Client)	


2. Beim Start wird jeder Client nach einem Namen gefragt. Dieser Name muss eindeutig sein und dient zur Identifikation im Chat.


3. Senden von Nachrichten
3.1 Client --> Client:

	send <Name_des_Empfängers> <Nachricht>	
	Zum Beispiel von PC an Laptop:
	send Laptop Hallo, wie geht es dir?

	
	Beim Senden einer der drei vordefinierten Fragen erhält man folgende Antwort:

	> Was ist deine IP-Adresse? --> Empfänger sendet IP-Adresse
	> Wie viel Uhr haben wir? --> Empfänger sendet Systemzeit
	> Welche Rechnernetze HA war das? --> Empfängers sendet vordefinierte Nachricht: "4. HA, Aufgabe 4"

3.2 Server --> alle Client:

	im Server-Terminal Text eingeben und Enter drücken für Nachricht an alle Clients
	<list> sendet eine Liste aller verbundenen Clients an alle Clients

3.3 Client --> Server:
	
	broadcast <Nachricht>	Senden eine Nachricht an alle Clients über Server + Bestätigung vom Server
	list			Erhalte die Liste aller verbunden Clients


4. Beende Verbindung:

	stop