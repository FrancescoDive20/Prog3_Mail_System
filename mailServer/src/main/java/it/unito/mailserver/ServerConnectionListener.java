package it.unito.mailserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Accetta le connessioni in ingresso e le delega a un Thread Pool.
 */
public class ServerConnectionListener implements Runnable {

    private final int port;
    // Vincolo: uso di ExecutorService al posto di thread manuali
    private final ExecutorService threadPool;

    public ServerConnectionListener(int port) {
        this.port = port;
        // Pool dimensionato in base al carico previsto
        this.threadPool = Executors.newFixedThreadPool(10);
    }

    @Override
    public void run() {
        // Vincolo: chiusura sicura del ServerSocket tramite try-with-resources
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            MailServerApp.logInfo("Server in ascolto sulla porta " + port);

            while (!Thread.currentThread().isInterrupted()) {
                Socket clientSocket = serverSocket.accept();
                MailServerApp.logInfo("Nuova connessione accettata: " + clientSocket.getInetAddress());

                // Deleghiamo la gestione al pool
                threadPool.submit(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            MailServerApp.logInfo("Errore fatale del ServerSocket: " + e.getMessage());
        } finally {
            threadPool.shutdown();
        }
    }
}