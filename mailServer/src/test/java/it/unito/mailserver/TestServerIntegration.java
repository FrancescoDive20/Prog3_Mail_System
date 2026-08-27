package it.unito.mailserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unito.shared.Email;
import it.unito.shared.Protocol;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Suite di collaudo dinamico per l'integrazione di rete e la concorrenza del Mail Server.
 * ATTENZIONE: Il MailServerMain deve essere in esecuzione prima di lanciare questi test.
 */
public class TestServerIntegration {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8081;

    @Test
    public void testCheckUserCommand() {
        // Test di base: verifica la connessione e la corretta interpretazione di un comando semplice
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Invia il comando testuale
            String command = Protocol.build(Protocol.CMD_CHECK_USER, "test@unito.it");
            out.println(command);

            // Legge la risposta
            String response = in.readLine();

            // Asserisce che il server abbia risposto come da protocollo
            Assertions.assertEquals(Protocol.RES_USER_EXISTS, response,
                    "Il server non ha risposto correttamente al comando CHECK_USER");

        } catch (Exception e) {
            Assertions.fail("Errore di rete durante il test: " + e.getMessage());
        }
    }

    @Test
    public void testConcurrentEmailSending() throws InterruptedException {
        // STRESS TEST: Verifichiamo la sezione critica del MailboxManager
        int numberOfConcurrentClients = 50;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfConcurrentClients);

        // Il CountDownLatch serve a bloccare tutti i thread finché non sono tutti pronti,
        // per poi farli scattare esattamente nello stesso millisecondo, massimizzando la probabilità di Race Condition.
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numberOfConcurrentClients);

        AtomicInteger successfulDeliveries = new AtomicInteger(0);
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();

        // Creiamo l'email da inviare
        Email testEmail = new Email(
                "ID_STRESS_TEST",
                "stress.tester@unito.it",
                List.of("target.concorrenza@unito.it"),
                "Stress Test",
                "Test di invio simultaneo",
                LocalDateTime.now()
        );

        // Prepariamo i 50 thread client
        for (int i = 0; i < numberOfConcurrentClients; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Attende il segnale di via

                    try (Socket socket = new Socket(HOST, PORT);
                         PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                        // Invia il comando SEND_EMAIL
                        out.println(Protocol.build(Protocol.CMD_SEND_EMAIL));

                        // Invia il payload JSON nella riga successiva (come si aspetta ClientHandler)
                        out.println(mapper.writeValueAsString(testEmail));

                        // Legge l'esito
                        String response = in.readLine();
                        if (Protocol.RES_OK.equals(response)) {
                            successfulDeliveries.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Errore nel thread client: " + e.getMessage());
                } finally {
                    endLatch.countDown(); // Segnala che questo client ha finito
                }
            });
        }

        // SCATENA LA RACE CONDITION: Dà il via libera a tutti i thread simultaneamente
        startLatch.countDown();

        // Attende che tutti i 50 thread abbiano finito
        endLatch.await();
        executor.shutdown();

        // VALIDAZIONE ACCADEMICA
        // Ci aspettiamo che, grazie ai ReentrantReadWriteLock, nessuna scrittura sia fallita.
        Assertions.assertEquals(numberOfConcurrentClients, successfulDeliveries.get(),
                "Alcuni invii sono falliti a causa di violazioni della mutua esclusione sui file.");
    }
}