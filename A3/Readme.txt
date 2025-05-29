1. Starte eine Instanz des Programms als Server und den Rest als Cleints in verschiedenen Terminals. 

	python TCP-Chat.py -l 12345 		(Server)
	python TCP-Chat.py <server_ip> 12345 	(Client)	

2. Beim Start wird jeder Client nach einem Namen gefragt. Dieser Name muss eindeutig sein und dient zur Identifikation im Chat.

3. Sende Nachrichten an einen registrierten Client:

	send <Name_des_Empfängers> <Nachricht>
	
	Zum Beispiel von PC an Laptop:
	send Laptop Hallo, wie geht es dir?

4. Beende Verbindung:

	stop