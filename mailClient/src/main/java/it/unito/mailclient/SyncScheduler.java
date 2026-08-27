package it.unito.mailclient;

import it.unito.shared.Email;
import javafx.application.Platform;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Gestisce il polling periodico per scaricare le nuove email.
 * Lavora interamente in background.
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
            t.setDaemon(true); // Termina con l'applicazione
            return t;
        });
    }

    public void startPolling() {
        if (isRunning) return;
        isRunning = true;

        // Esegue il task ogni 5 secondi
        scheduler.scheduleAtFixedRate(this::fetchTask, 0, 5, TimeUnit.SECONDS);
    }

    public void stopPolling() {
        isRunning = false;
        scheduler.shutdownNow();
    }

    private void fetchTask() {
        String user = dataModel.getCurrentUser();
        if (user == null || user.isBlank()) return;

        try {
            // Ottiene l'ID dell'ultima email (se presente) per un fetch incrementale
            String lastId = null;
            if (!dataModel.getInbox().isEmpty()) {
                // Presumendo che l'ultima ricevuta sia in fondo (o in cima, dipenda dall'ordinamento)
                lastId = dataModel.getInbox().get(dataModel.getInbox().size() - 1).getId();
            }

            // Operazione I/O bloccante eseguita sul thread dello scheduler
            List<Email> newEmails = networkClient.fetchNewMessages(user, lastId);

            // Vincolo: aggiornamento del Modello TASSATIVAMENTE sul JavaFX Thread
            if (!newEmails.isEmpty()) {
                Platform.runLater(() -> {
                    dataModel.addEmails(newEmails);
                    dataModel.setConnectionStatus("Sincronizzato: " + newEmails.size() + " nuovi messaggi.");
                });
            } else {
                Platform.runLater(() -> dataModel.setConnectionStatus("Sincronizzato: nessun nuovo messaggio."));
            }

        } catch (IOException e) {
            Platform.runLater(() -> dataModel.setConnectionStatus("Errore di rete durante la sincronizzazione."));
        }
    }
}