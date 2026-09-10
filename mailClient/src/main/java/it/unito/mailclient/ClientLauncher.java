package it.unito.mailclient;

/**
 * Classe wrapper per l'avvio dell'applicazione.
 * Risolve architetturalmente i conflitti di inizializzazione legati al Module Path di JavaFX
 * quando l'applicazione viene pacchettizzata o avviata esternamente al plugin Maven.
 */
public class ClientLauncher {
    public static void main(String[] args) {
        MailClientApp.main(args);
    }
}