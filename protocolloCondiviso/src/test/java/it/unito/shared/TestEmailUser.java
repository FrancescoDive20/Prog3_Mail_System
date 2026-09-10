package it.unito.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suite di test per la validazione della sintassi di rete (Protocol)
 * e la correttezza delle espressioni regolari (Regex) lato client/server.
 */
class TestEmailUser {

    // Regex per la validazione formale degli indirizzi email
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    @Test
    @DisplayName("Verifica l'impacchettamento dei comandi del Protocollo (metodo build)")
    void testProtocolBuild() {
        // Arrange
        String command = Protocol.CMD_CHECK_USER;
        String param = "alice@mia.mail.com";
        String expectedMessage = command + ";" + param;

        // Act
        String actualMessage = Protocol.build(command, param);

        // Assert
        assertEquals(expectedMessage, actualMessage,
                "Il payload generato non rispetta il formato delimitato atteso.");
    }

    @Test
    @DisplayName("Verifica l'estrazione dei token del Protocollo (metodo parse)")
    void testProtocolParse() {
        // Arrange
        String rawMessage = "SEND_EMAIL;alice@mia.mail.com;bob@mia.mail.com;Test;Messaggio";

        // Act
        String[] parsedData = Protocol.parse(rawMessage);

        // Assert
        assertNotNull(parsedData, "Il parser ha restituito un riferimento nullo.");
        assertEquals(5, parsedData.length, "Il numero di token estratti non è corretto.");
        assertEquals(Protocol.CMD_SEND_EMAIL, parsedData[0]);
        assertEquals("alice@mia.mail.com", parsedData[1]);
        assertEquals("Messaggio", parsedData[4]);
    }

    @Test
    @DisplayName("Test Regex Email: Validazione Casi Nominali")
    void testEmailRegexValid() {
        // Arrange: Insieme di input conformi (Boundary Analysis)
        String[] validEmails = {
                "alice@mia.mail.com",
                "nome.cognome@unito.it",
                "studente_123+tag@cs.domain.org"
        };

        // Act & Assert
        for (String email : validEmails) {
            Matcher matcher = EMAIL_PATTERN.matcher(email);
            assertTrue(matcher.matches(),
                    () -> "Falso Negativo: L'indirizzo valido '" + email + "' è stato rigettato.");
        }
    }

    @Test
    @DisplayName("Test Regex Email: Validazione Anomalie ed Edge Cases")
    void testEmailRegexInvalid() {
        // Arrange: Fault injection per testare la robustezza dell'espressione
        String[] invalidEmails = {
                "",                     // Stringa vuota
                "alice_at_mia.com",     // Mancanza del separatore @
                "@unito.it",            // Mancanza della local-part
                "bob@.com",             // Dominio primario assente
                "bob@dominio",          // Mancanza del Top Level Domain (TLD)
                "bob@dominio.c"         // TLD insufficiente
        };

        // Act & Assert
        for (String email : invalidEmails) {
            Matcher matcher = EMAIL_PATTERN.matcher(email);
            assertFalse(matcher.matches(),
                    () -> "Falso Positivo: L'indirizzo invalido '" + email + "' ha superato i controlli.");
        }
    }
}