package it.unito.mailclient;

import it.unito.shared.Email;
import javafx.application.Platform;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Gestore del polling periodico per la sincronizzazione passiva dei messaggi.
 * Sfrutta uno {@link ScheduledExecutorService} per isolare i task temporizzati
 * su un Thread demone in background, prevenendo colli di bottiglia sull'interfaccia.
 * Garantisce inoltre la resilienza del sistema operando tentativi continui (retry)
 * in caso di inattività del server.
 */
public class SyncScheduler {

    private final ScheduledExecutorService scheduler;
    private final NetworkClient networkClient;
    private final DataModel dataModel;
    private boolean isRunning = false;

    public SyncScheduler(NetworkClient networkClient, DataModel dataModel) {
        this.networkClient = networkClient;
        this.dataModel = dataModel;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SyncScheduler-Thread");
            t.setDaemon(true);
            return t;
        });
    }

    /** Avvia o riprende l'esecuzione schedulata dei task di aggiornamento. */
    public void startPolling() {
        if (isRunning) return;
        isRunning = true;
        scheduler.scheduleAtFixedRate(this::fetchTask, 0, 5, TimeUnit.SECONDS);
    }

    /** Arresta forzatamente il servizio di polling in fase di shutdown. */
    public void stopPolling() {
        isRunning = false;
        scheduler.shutdownNow();
    }

    /**
     * Sezione critica di aggiornamento eseguita dal Thread dello scheduler.
     * Invoca operazioni bloccanti di I/O (fetchNewMessages) e, al termine, delega
     * le mutazioni di stato della GUI tramite {@code Platform.runLater}.
     */
    private void fetchTask() {
        String user = dataModel.getCurrentUser();
        if (user == null || user.isBlank()) return;

        try {
            String lastId = null;
            if (!dataModel.getInbox().isEmpty()) {
                lastId = dataModel.getInbox().get(dataModel.getInbox().size() - 1).getId();
            }

            List<Email> newEmails = networkClient.fetchNewMessages(user, lastId);

            if (!newEmails.isEmpty()) {
                Platform.runLater(() -> {
                    dataModel.addEmails(newEmails);
                    dataModel.setConnectionStatus("Sincronizzato: " + newEmails.size() + " nuovi messaggi.");
                });
            } else {
                Platform.runLater(() -> dataModel.setConnectionStatus("Sincronizzato: nessun nuovo messaggio."));
            }

        } catch (IOException e) {
            // Gestione Resilienza: Informa l'utente senza provocare il crash dell'applicativo.
            // Il task si riavvierà ciclicamente ristabilendo la connessione una volta risolto il disservizio.
            Platform.runLater(() -> dataModel.setConnectionStatus("Server offline. Attesa riconnessione..."));
        }
    }
}