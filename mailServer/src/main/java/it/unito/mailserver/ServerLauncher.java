package it.unito.mailserver;

/**
 * Classe di avvio fittizia per bypassare le restrizioni del Module Path di JavaFX.
 */
public class ServerLauncher {
    public static void main(String[] args) {
        // Richiama il main della vera applicazione JavaFX
        MailServerApp.main(args);
    }
}