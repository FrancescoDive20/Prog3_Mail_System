package it.unito.mailclient;

import it.unito.shared.Email;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Rappresenta il Model nell'architettura MVC dell'applicazione client.
 * Incapsula lo stato dell'interfaccia e i dati di dominio.
 * Sfrutta le primitive di JavaFX (Properties e ObservableList) per implementare
 * nativamente il pattern Observer-Observable senza l'ausilio di classi deprecate.
 */
public class DataModel {

    private final StringProperty currentUser = new SimpleStringProperty("");
    private final StringProperty connectionStatus = new SimpleStringProperty("Disconnesso");

    /**
     * Struttura dati osservabile. Qualsiasi modifica a questa lista notificherà
     * automaticamente gli observer registrati (es. la ListView della GUI),
     * garantendo il disaccoppiamento tra logica e presentazione.
     */
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
     * Inserisce le nuove email nel Model.
     * Per prevenire violazioni di concorrenza, l'invocazione di questo metodo
     * deve avvenire rigorosamente all'interno del JavaFX Application Thread.
     *
     * @param newEmails Collezione iterabile di nuove email da aggiungere.
     */
    public void addEmails(Iterable<Email> newEmails) {
        for (Email e : newEmails) {
            if (!inbox.contains(e)) {
                inbox.add(e);
            }
        }
    }
}