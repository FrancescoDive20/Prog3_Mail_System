import com.fasterxml.jackson.databind.ObjectMapper;
import it.unito.protocol.Email;
import java.time.LocalDateTime;
import java.util.List;

public class TestEmail {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Email email = new Email(
                "123",
                "alice@mia.mail.com",
                List.of("bob@mia.mail.com"),
                "Hello",
                "Ciao Bob!",
                LocalDateTime.now()
        );

        // SERIALIZZAZIONE
        String json = mapper.writeValueAsString(email);
        System.out.println("JSON:");
        System.out.println(json);

        // DESERIALIZZAZIONE
        Email restored = mapper.readValue(json, Email.class);
        System.out.println("Restored:");
        System.out.println(restored);
    }
}
