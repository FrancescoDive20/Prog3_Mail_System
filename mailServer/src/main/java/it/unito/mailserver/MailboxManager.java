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
 * Singleton per la gestione della persistenza su file JSON.
 * Garantisce la mutua esclusione a livello di singola casella postale.
 */
public class MailboxManager {

    private static final MailboxManager INSTANCE = new MailboxManager();
    private static final String STORAGE_DIR = "server_storage";

    // Mappa per mantenere un lock distinto per ogni utente, ottimizzando la concorrenza
    private final ConcurrentHashMap<String, ReentrantReadWriteLock> userLocks = new ConcurrentHashMap<>();
    private final ObjectMapper mapper;

    private MailboxManager() {
        this.mapper = new ObjectMapper();
        this.mapper.findAndRegisterModules();
        new File(STORAGE_DIR).mkdirs();
    }

    public static MailboxManager getInstance() {
        return INSTANCE;
    }

    private ReentrantReadWriteLock getLockForUser(String user) {
        return userLocks.computeIfAbsent(user, k -> new ReentrantReadWriteLock());
    }

    /**
     * Salva un'email nella casella del destinatario garantendo mutua esclusione.
     */
    public boolean saveEmailToRecipients(Email email) {
        boolean allSuccess = true;
        for (String recipient : email.getRecipients()) {
            ReentrantReadWriteLock lock = getLockForUser(recipient);
            // Acquisizione del lock in SCRITTURA (bloccante per altre letture/scritture)
            lock.writeLock().lock();
            try {
                File mailbox = new File(STORAGE_DIR, recipient + "_inbox.json");
                List<Email> emails = loadEmailsInternal(mailbox);
                emails.add(email);
                mapper.writeValue(mailbox, emails);
                MailServerApp.logInfo("Email salvata su disco per: " + recipient);
            } catch (IOException e) {
                MailServerApp.logInfo("Errore di I/O per " + recipient + ": " + e.getMessage());
                allSuccess = false;
            } finally {
                // Rilascio tassativo del lock nel finally
                lock.writeLock().unlock();
            }
        }
        return allSuccess;
    }

    /**
     * Legge le email di un utente garantendo la concorrenza in lettura.
     */
    public List<Email> loadEmailsForUser(String user) {
        ReentrantReadWriteLock lock = getLockForUser(user);
        // Acquisizione del lock in LETTURA (permette altre letture simultanee)
        lock.readLock().lock();
        try {
            File mailbox = new File(STORAGE_DIR, user + "_inbox.json");
            return loadEmailsInternal(mailbox);
        } finally {
            lock.readLock().unlock();
        }
    }

    private List<Email> loadEmailsInternal(File file) {
        if (!file.exists()) return new ArrayList<>();
        try {
            return mapper.readValue(file, new TypeReference<List<Email>>() {});
        } catch (IOException e) {
            MailServerApp.logInfo("Errore durante la deserializzazione: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}