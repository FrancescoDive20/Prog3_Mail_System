package it.unito.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Modello dati condiviso che rappresenta un messaggio di posta elettronica.
 * La classe implementa {@link Serializable} ed è annotata per il supporto
 * alla serializzazione/deserializzazione JSON tramite la libreria Jackson.
 * Garantisce l'identità strutturale dell'entità di dominio tra il livello Client e il Server.
 */
public class Email implements Serializable {

    private final String id;
    private final String sender;
    private final List<String> recipients;
    private final String subject;
    private final String body;
    private final LocalDateTime timestamp;

    /**
     * Costruttore principale dell'entità Email.
     * Utilizza le annotazioni {@code @JsonCreator} e {@code @JsonProperty} per
     * permettere a Jackson di ricostruire oggetti immutabili durante l'unmarshalling.
     *
     * @param id L'identificatore univoco del messaggio (generalmente un UUID).
     * @param sender L'indirizzo email del mittente.
     * @param recipients La lista degli indirizzi email dei destinatari.
     * @param subject L'oggetto del messaggio.
     * @param body Il corpo testuale del messaggio.
     * @param timestamp La data e l'ora di invio del messaggio.
     */
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

    /** @return L'identificatore univoco dell'email. */
    public String getId() {
        return id;
    }

    /** @return L'indirizzo email del mittente. */
    public String getSender() {
        return sender;
    }

    /** @return La lista dei destinatari del messaggio. */
    public List<String> getRecipients() {
        return recipients;
    }

    /** @return L'oggetto del messaggio. */
    public String getSubject() {
        return subject;
    }

    /** @return Il corpo del messaggio. */
    public String getBody() {
        return body;
    }

    /** @return Il timestamp di creazione del messaggio. */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

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

    /**
     * Verifica l'uguaglianza tra due email basandosi esclusivamente sull'ID univoco.
     */
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