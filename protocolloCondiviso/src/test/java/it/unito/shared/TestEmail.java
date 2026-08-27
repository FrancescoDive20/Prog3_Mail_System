package it.unito.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestEmail {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        // Inizializzazione del mapper prima di ogni test (Principio di Isolamento)
        mapper = new ObjectMapper();
        // Registrazione dei moduli JSR-310 necessaria per mappare correttamente LocalDateTime
        mapper.findAndRegisterModules();
    }

    @Test
    @DisplayName("Verifica serializzazione e deserializzazione JSON completa dell'entità Email")
    void testSerializationAndDeserialization() throws Exception {
        // Arrange: Costruzione del mock di test
        // Troncamento ai millisecondi per evitare asimmetrie di precisione tra OS e JSON
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        Email originalEmail = new Email(
                "123",
                "alice@mia.mail.com",
                List.of("bob@mia.mail.com"),
                "Hello",
                "Ciao Bob!",
                now
        );

        // Act: Processo di marshalling e unmarshalling
        String json = mapper.writeValueAsString(originalEmail);
        Email restoredEmail = mapper.readValue(json, Email.class);

        // Assert: Validazione tramite Oracolo Automatico
        assertNotNull(restoredEmail, "L'oggetto deserializzato non deve essere nullo");

        // Giustificazione Ingegneristica: Poiché Email.equals() valuta solo l'ID,
        // asseriamo manualmente ogni singola proprietà di dominio per garantire
        // che l'integrità strutturale sia mantenuta al 100% post-deserializzazione.
        assertEquals(originalEmail.getId(), restoredEmail.getId(), "Mancata corrispondenza: ID");
        assertEquals(originalEmail.getSender(), restoredEmail.getSender(), "Mancata corrispondenza: Mittente");
        assertEquals(originalEmail.getRecipients(), restoredEmail.getRecipients(), "Mancata corrispondenza: Destinatari");
        assertEquals(originalEmail.getSubject(), restoredEmail.getSubject(), "Mancata corrispondenza: Oggetto");
        assertEquals(originalEmail.getBody(), restoredEmail.getBody(), "Mancata corrispondenza: Corpo del messaggio");
        assertEquals(originalEmail.getTimestamp(), restoredEmail.getTimestamp(), "Mancata corrispondenza: Timestamp");
    }
}