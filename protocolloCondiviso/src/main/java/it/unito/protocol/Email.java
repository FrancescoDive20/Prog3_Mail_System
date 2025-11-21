package it.unito.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Modello condiviso di email tra client e server.
 *
 * Questa classe è nel modulo condiviso per garantirne
 * identicità di struttura su entrambe le applicazioni.
 */
public class Email implements Serializable {

    private final String id;
    private final String sender;
    private final List<String> recipients;
    private final String subject;
    private final String body;
    private final LocalDateTime timestamp;

    @JsonCreator
    public Email(
            @JsonProperty("id") String id,
            @JsonProperty("sender") String sender,
            @JsonProperty("recipients") List<String> recipients,
            @JsonProperty("subject") String subject,
            @JsonProperty("body") String body,
            @JsonProperty("timestamp") LocalDateTime timestamp
    ) {
        this.id = id;
        this.sender = sender;
        this.recipients = recipients;
        this.subject = subject;
        this.body = body;
        this.timestamp = timestamp;
    }

    /* GETTER NECESSARY PER JACKSON E PER IL CLIENT/SERVER */

    public String getId() {
        return id;
    }

    public String getSender() {
        return sender;
    }

    public List<String> getRecipients() {
        return recipients;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /* UTILITY */

    @Override
    public String toString() {
        return "Email{" +
                "id='" + id + '\'' +
                ", sender='" + sender + '\'' +
                ", recipients=" + recipients +
                ", subject='" + subject + '\'' +
                ", body='" + body + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Email)) return false;
        Email email = (Email) o;
        return Objects.equals(id, email.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
