# Methode zum Zeichnen des Manchester-Codes
def manchester_code_console(binary_input):
    output = ""
    for bit in binary_input:
        if bit == '0':
            output += "/\\"  # Übergang von Hoch nach Niedrig
        elif bit == '1':
            output += "\\/"  # Übergang von Niedrig nach Hoch
        else:
            raise ValueError("Ungültige binäre Eingabe. Nur '0' und '1' sind erlaubt.")
    return output

if __name__ == "__main__":
    # Eingabe der Binärzahl
    binary_input = input("Gib eine binäre Zahl ein: ")
    try:
        console_output = manchester_code_console(binary_input)
        print("\nManchester-Code:")
        print("-" * (len(binary_input) * 2)) # Obere Begrenzung
        print(console_output)
        print("-" * (len(binary_input) * 2)) # Untere Begrenzung
        print("Legende: /\\ = '0', \\/ = '1'")
    except ValueError as e:
        print(f"Fehler: {e}")