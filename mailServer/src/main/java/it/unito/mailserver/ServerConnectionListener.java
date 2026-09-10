package it.unito.mailserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Listener di rete incaricato di accettare le connessioni TCP in ingresso.
 * Delega l'elaborazione di ciascuna sessione client a un Thread Pool per
 * ottimizzare l'uso delle risorse di sistema.
 */
public class ServerConnectionListener implements Runnable {

    private final int port;
    /** Struttura dati per la gestione efficiente dei thread (prevenzione dell'overhead di creazione). */
    private final ExecutorService threadPool;

    public ServerConnectionListener(int port) {
        this.port = port;
        // Istanzia un pool di thread a dimensione fissa adeguato al carico previsto
        this.threadPool = Executors.newFixedThreadPool(10);
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            MailServerApp.logInfo("Server in ascolto sulla porta " + port);

            // Ciclo infinito bloccante per l'accettazione delle richieste
            while (!Thread.currentThread().isInterrupted()) {
                Socket clientSocket = serverSocket.accept();
                MailServerApp.logInfo("Nuova connessione accettata: " + clientSocket.getInetAddress());

                // Affida l'esecuzione del Runnable (ClientHandler) a uno dei thread del pool
                threadPool.submit(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            MailServerApp.logInfo("Errore fatale del ServerSocket: " + e.getMessage());
        } finally {
            threadPool.shutdown();
        }
    }
}