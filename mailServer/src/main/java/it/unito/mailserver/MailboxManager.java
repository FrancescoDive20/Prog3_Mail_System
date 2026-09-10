package it.unito.mailserver;

import it.unito.shared.Email;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Gestore centralizzato per la persistenza delle email su file system (formato JSON).
 * Implementa il pattern Singleton e garantisce l'accesso concorrente thread-safe
 * alle risorse condivise tramite meccanismi di mutua esclusione.
 */
public class MailboxManager {

    private static final MailboxManager INSTANCE = new MailboxManager();
    private static final String STORAGE_DIR = "server_storage";

    /**
     * Mappa concorrente che associa a ciascun utente un lock specifico.
     * Questo approccio (Lock Striping) massimizza il parallelismo, evitando di
     * bloccare l'intero sistema quando si accede a caselle postali differenti.
     */
    private final ConcurrentHashMap<String, ReentrantReadWriteLock> userLocks = new ConcurrentHashMap<>();
    private final ObjectMapper mapper;

    private MailboxManager() {
        this.mapper = new ObjectMapper();
        this.mapper.findAndRegisterModules();
        new File(STORAGE_DIR).mkdirs();
    }

    /** @return L'istanza Singleton del MailboxManager. */
    public static MailboxManager getInstance() {
        return INSTANCE;
    }

    private ReentrantReadWriteLock getLockForUser(String user) {
        return userLocks.computeIfAbsent(user, k -> new ReentrantReadWriteLock());
    }

    /**
     * Salva un'email nelle caselle di posta dei destinatari.
     * Costituisce una sezione critica protetta da un WriteLock per garantire la
     * scrittura atomica ed evitare race conditions durante modifiche simultanee.
     *
     * @param email L'oggetto Email da persistere.
     * @return {@code true} se il salvataggio ha avuto successo per tutti i destinatari validi.
     */
    public boolean saveEmailToRecipients(Email email) {
        boolean allSuccess = true;
        for (String recipient : email.getRecipients()) {
            File mailbox = new File(STORAGE_DIR, recipient + "_inbox.json");

            if (!mailbox.exists()) {
                MailServerApp.logInfo("Errore: tentativo di invio a destinatario inesistente (" + recipient + ")");
                allSuccess = false;
                continue;
            }

            ReentrantReadWriteLock lock = getLockForUser(recipient);
            lock.writeLock().lock();
            try {
                List<Email> emails = loadEmailsInternal(mailbox);
                emails.add(email);
                mapper.writeValue(mailbox, emails);
                MailServerApp.logInfo("Email salvata per: " + recipient);
            } catch (IOException e) {
                MailServerApp.logInfo("Errore salvataggio per " + recipient + ": " + e.getMessage());
                allSuccess = false;
            } finally {
                // Il rilascio del lock nel blocco finally previene situazioni di deadlock
                // in caso di eccezioni durante l'I/O.
                lock.writeLock().unlock();
            }
        }
        return allSuccess;
    }

    /**
     * Carica in memoria le email di un utente.
     * Utilizza un ReadLock che consente letture simultanee da parte di più thread,
     * bloccando l'accesso solo in presenza di un'operazione di scrittura attiva.
     *
     * @param user L'indirizzo email dell'utente.
     * @return La lista delle email presenti nella casella di posta.
     */
    public List<Email> loadEmailsForUser(String user) {
        ReentrantReadWriteLock lock = getLockForUser(user);
        lock.readLock().lock();
        try {
            File mailbox = new File(STORAGE_DIR, user + "_inbox.json");
            return loadEmailsInternal(mailbox);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Rimuove logicamente e fisicamente un'email dal file JSON dell'utente.
     * Operazione protetta da WriteLock per garantire l'atomicità della cancellazione.
     *
     * @param user L'utente proprietario della casella.
     * @param emailId L'identificatore univoco del messaggio da eliminare.
     * @return {@code true} se il messaggio è stato trovato e rimosso con successo.
     */
    public boolean deleteEmailForUser(String user, String emailId) {
        ReentrantReadWriteLock lock = getLockForUser(user);
        lock.writeLock().lock();
        try {
            File mailbox = new File(STORAGE_DIR, user + "_inbox.json");
            List<Email> emails = loadEmailsInternal(mailbox);
            boolean removed = emails.removeIf(e -> e.getId().equals(emailId));
            if (removed) {
                mapper.writeValue(mailbox, emails);
                MailServerApp.logInfo("Email cancellata per: " + user);
            }
            return removed;
        } catch (IOException e) {
            MailServerApp.logInfo("Errore cancellazione per " + user + ": " + e.getMessage());
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private List<Email> loadEmailsInternal(File file) {
        if (!file.exists()) return new ArrayList<>();
        try {
            return mapper.readValue(file, new TypeReference<List<Email>>() {});
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }
}