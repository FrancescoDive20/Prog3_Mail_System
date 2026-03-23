package it.unito.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class TestEmailUser {

    // Regex per validazione formale robusta lato client e server
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
                "Il payload generato non rispetta lo standard delimitato da punto e virgola");
    }

    @Test
    @DisplayName("Verifica l'estrazione dei token del Protocollo (metodo parse)")
    void testProtocolParse() {
        // Arrange
        String rawMessage = "SEND_EMAIL;alice@mia.mail.com;bob@mia.mail.com;Test;Messaggio";

        // Act
        // N.B: Il test assume che Protocol.parse() restituisca correttamente String[] e non String come erroneamente indicato in PDF
        String[] parsedData = Protocol.parse(rawMessage);

        // Assert
        assertNotNull(parsedData, "Il parser non deve mai restituire un reference nullo");
        assertEquals(5, parsedData.length, "Il numero di token estratti è discordante");
        assertEquals(Protocol.CMD_SEND_EMAIL, parsedData[0]);
        assertEquals("alice@mia.mail.com", parsedData[1]);
        assertEquals("Messaggio", parsedData[4]);
    }

    @Test
    @DisplayName("Test Metodologico Regex Email: Esito Positivo")
    void testEmailRegexValid() {
        // Arrange: Boundary analysis dei casi nominali
        String[] validEmails = {
                "alice@mia.mail.com",
                "nome.cognome@unito.it",
                "studente_123+tag@cs.domain.org"
        };

        // Act & Assert
        for (String email : validEmails) {
            Matcher matcher = EMAIL_PATTERN.matcher(email);
            assertTrue(matcher.matches(),
                    () -> "Falso Negativo: L'email valida '" + email + "' è stata rigettata.");
        }
    }

    @Test
    @DisplayName("Test Metodologico Regex Email: Esito Negativo")
    void testEmailRegexInvalid() {
        // Arrange: Fault injection ed edge cases
        String[] invalidEmails = {
                "",                     // E-01: Stringa vuota
                "alice_at_mia.com",     // E-02: Mancanza del separatore @
                "@unito.it",            // E-03: Mancanza della local-part
                "bob@.com",             // E-04: Dominio primario assente
                "bob@dominio",          // E-05: Mancanza del Top Level Domain (TLD)
                "bob@dominio.c"         // E-06: TLD insufficiente (minimo 2 caratteri)
        };

        // Act & Assert
        for (String email : invalidEmails) {
            Matcher matcher = EMAIL_PATTERN.matcher(email);
            assertFalse(matcher.matches(),
                    () -> "Falso Positivo: L'email invalida '" + email + "' ha superato i controlli di validazione.");
        }
    }
}