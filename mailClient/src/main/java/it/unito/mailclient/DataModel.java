package it.unito.mailclient;

import it.unito.shared.Email;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Modello MVC dell'applicazione client.
 * Gestisce lo stato in modo reattivo tramite JavaFX Properties.
 */
public class DataModel {

    // L'utente attualmente loggato. Una StringProperty permette alla GUI di reagire ai cambiamenti.
    private final StringProperty currentUser = new SimpleStringProperty("");

    // Stato della connessione (es. "Connesso", "Errore di rete").
    private final StringProperty connectionStatus = new SimpleStringProperty("Disconnesso");

    // La lista delle email. Essendo Observable, la ListView (o TableView) si aggiornerà automaticamente.
    private final ObservableList<Email> inbox = FXCollections.observableArrayList();

    public String getCurrentUser() {
        return currentUser.get();
    }

    public void setCurrentUser(String user) {
        this.currentUser.set(user);
    }

    public StringProperty currentUserProperty() {
        return currentUser;
    }

    public String getConnectionStatus() {
        return connectionStatus.get();
    }

    public void setConnectionStatus(String status) {
        this.connectionStatus.set(status);
    }

    public StringProperty connectionStatusProperty() {
        return connectionStatus;
    }

    public ObservableList<Email> getInbox() {
        return inbox;
    }

    /**
     * Aggiunge nuove email alla inbox in modo sicuro.
     * Questo metodo DEVE essere chiamato solo dal JavaFX Application Thread.
     */
    public void addEmails(Iterable<Email> newEmails) {
        for (Email e : newEmails) {
            if (!inbox.contains(e)) { // Evita duplicati (l'equals di Email si basa sull'ID)
                inbox.add(e);
            }
        }
    }
}