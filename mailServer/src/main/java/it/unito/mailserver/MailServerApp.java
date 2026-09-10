package it.unito.mailserver;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * Entry point dell'applicazione Server basata su JavaFX.
 * Inizializza l'interfaccia grafica per il logging e avvia il demone di rete
 * incaricato di ascoltare le connessioni in ingresso su un thread separato.
 */
public class MailServerApp extends Application {

    private static TextArea logArea;
    private Thread serverThread;

    @Override
    public void start(Stage primaryStage) {
        logArea = new TextArea();
        logArea.setEditable(false);

        BorderPane root = new BorderPane(logArea);
        Scene scene = new Scene(root, 600, 400);

        primaryStage.setTitle("Mail Server - Log Console");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> shutdownServer());
        primaryStage.show();

        logInfo("Avvio del server in corso...");

        // Delega l'ascolto su socket a un thread demone per non bloccare il JavaFX Application Thread
        ServerConnectionListener listener = new ServerConnectionListener(8081);
        serverThread = new Thread(listener);
        serverThread.setDaemon(true);
        serverThread.start();
    }

    /**
     * Metodo thread-safe per iniettare stringhe di log nella GUI da thread secondari.
     * Sfrutta {@link Platform#runLater} per schedulare l'aggiornamento sul thread grafico,
     * prevenendo eccezioni di concorrenza.
     *
     * @param message Il messaggio testuale da inserire nel log.
     */
    public static void logInfo(String message) {
        Platform.runLater(() -> logArea.appendText(message + "\n"));
    }

    private void shutdownServer() {
        logInfo("Spegnimento del server e rilascio delle risorse...");
        if (serverThread != null && serverThread.isAlive()) {
            serverThread.interrupt();
        }
        Platform.exit();
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}