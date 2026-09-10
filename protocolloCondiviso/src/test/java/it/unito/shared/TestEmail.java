package it.unito.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suite di test per la validazione del processo di serializzazione
 * e deserializzazione (marshalling/unmarshalling) dell'entità di dominio.
 */
class TestEmail {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        mapper.findAndRegisterModules(); // Supporto JSR-310 per LocalDateTime
    }

    @Test
    @DisplayName("Verifica serializzazione e deserializzazione JSON dell'entità Email")
    void testSerializationAndDeserialization() throws Exception {
        // Arrange: Costruzione dell'oggetto di test.
        // Viene applicato un troncamento per prevenire artefatti di precisione temporale durante la serializzazione.
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        Email originalEmail = new Email(
                "123-UUID",
                "alice@mia.mail.com",
                List.of("bob@mia.mail.com"),
                "Oggetto di test",
                "Corpo del messaggio",
                now
        );

        // Act: Esecuzione delle operazioni di marshalling e unmarshalling
        String json = mapper.writeValueAsString(originalEmail);
        Email restoredEmail = mapper.readValue(json, Email.class);

        // Assert: Validazione strutturale post-deserializzazione
        assertNotNull(restoredEmail, "L'oggetto deserializzato risulta nullo.");

        // Poiché il metodo equals() valuta unicamente l'ID per convenzione di dominio,
        // si esegue un'asserzione esplicita su ogni campo per garantire la totale integrità dei dati.
        assertEquals(originalEmail.getId(), restoredEmail.getId(), "L'ID non coincide.");
        assertEquals(originalEmail.getSender(), restoredEmail.getSender(), "Il mittente non coincide.");
        assertEquals(originalEmail.getRecipients(), restoredEmail.getRecipients(), "I destinatari non coincidono.");
        assertEquals(originalEmail.getSubject(), restoredEmail.getSubject(), "L'oggetto non coincide.");
        assertEquals(originalEmail.getBody(), restoredEmail.getBody(), "Il corpo del messaggio non coincide.");
        assertEquals(originalEmail.getTimestamp(), restoredEmail.getTimestamp(), "Il timestamp non coincide.");
    }
}