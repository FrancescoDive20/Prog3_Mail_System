package it.unito.mailserver;

/**
 * Classe wrapper necessaria per avviare l'applicazione JavaFX
 * bypassando le restrizioni modulari imposte dal Module Path di Java.
 */
public class ServerLauncher {
    public static void main(String[] args) {
        MailServerApp.main(args);
    }
}