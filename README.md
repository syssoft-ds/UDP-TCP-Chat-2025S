# Hausaufgabe

## A1

## A2

`103.161.122.83` ist die IPv4-Adresse und die `18` gibt die Subnetzmaske an.

- Die Subnetzmaske ist eine 32 bit Zahl, mit 18 führenden Einsen und 14 darauf folgenden Nullen.
- Die Netzwerkadresse ist die Bitweise Verundung der Subnetzmaske und der IPv4-Adresse
- Die Broadcastadresse ist die Bitweise Veroderung der IPv4-Adresse mit der Negierung der Subnetzmaske

| Beschreibung      | 32-Bit Darstellung | Dezimaldarstellung |
| ------------      | ------------------ | ------------------ |
| IPv4-Adresse      | `01100111.10100001.01111010.01010011` | `103.161.122.83`|
| Subnetzmaske      | `11111111.11111111.11000000.00000000` | `255.255.192.000`|
| Netzadresse       | `01100111.10100001.01000000.00000000` | `103.161.64.0` |
| Broadcastadresse  | `01100111.10100001.01111111.11111111` | `103.161.127.255` |

Um herauszufinden, ob die IPv4-Adresse `103.161.193.83/18` im selben Netz liegt, berechnen wir die Netzadresse dieser Adresse und vergleichen sie mir der oben berechneten Adresse.

| Beschreibung      | 32-Bit Darstellung | Dezimaldarstellung |
| ------------      | ------------------ | ------------------ |
| IPv4-Adresse      | `01100111.10100001.11000001.01010011` | `103.161.193.83`|
| Subnetzmaske      | `11111111.11111111.11000000.00000000` | `255.255.192.0`|
| Netzadresse       | `01100111.10100001.11000000.00000000` | `103.161.192.0`|

Da die beiden Netzadressen unterschiedlich sind folgt, dass die beiden Adressen nicht im selben Netzwerk liegen.
