1. Starte mehrere Instanzen des Programms auf verschiedenen Terminals oder Rechnern. Jeder Instanz muss ein eindeutiger Name und ein eigener Port zugewiesen werden.

	python UDP-Chat.py <name> <port>

	Beispiel: User PC und Laptop
	python UDP-Chat.py PC 12345
	python UDP-Chat.py Laptop 54321

2. Registriere dich bei einem anderen Client von einer der Instanzen aus:

	> register <IP-Adresse_des_anderen_Clients> <Port_des_anderen_Clients>
	
	Beispiel von PC zu Laptop (192.168.2.100):
	> register 192.168.2.100 54321
	Laptop sollte dann eine Meldung erhalten, dass sich PC registriert hat, und umgekehrt.

3. Sende Nachrichten an einen registrierten Client:

	> send <Name_des_Empfängers> <Nachricht>
	
	Zum Beispiel von PC an Laptop:
	> send Laptop Hallo, wie geht es dir?

4. Sende Nachrichten an alle registrierten Clients:

	> send_all <Nachricht>

5. Beim Senden einer der drei vordefinierten Fragen erhält man folgende Antwort:

	> Was ist deine IP-Adresse? --> Empfänger sendet IP-Adresse
	> Wie viel Uhr haben wir? --> Empfänger sendet Systemzeit
	> Welche Rechnernetze HA war das? --> Empfängers sendet vordefinierte Nachricht: "4. HA, Aufgabe 4"

6. Zeige bekannte Peers an:

	> peers

7. Beende das Programm:

	> exit