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
 * Suite di collaudo per l'integrazione di rete e la valutazione delle performance
 * di concorrenza del Mail Server.
 * NOTA: L'applicativo Server deve essere in esecuzione per superare i test.
 */
public class TestServerIntegration {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8081;

    @Test
    public void testCheckUserCommand() {
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            String command = Protocol.build(Protocol.CMD_CHECK_USER, "test@unito.it");
            out.println(command);
            String response = in.readLine();

            Assertions.assertEquals(Protocol.RES_USER_EXISTS, response,
                    "Il server non ha risposto correttamente al comando CHECK_USER");

        } catch (Exception e) {
            Assertions.fail("Errore di rete durante il test: " + e.getMessage());
        }
    }

    /**
     * Stress Test per validare la sezione critica del MailboxManager.
     * Simula un elevato numero di client concorrenti che tentano di scrivere
     * simultaneamente nello stesso file JSON.
     */
    @Test
    public void testConcurrentEmailSending() throws InterruptedException {
        int numberOfConcurrentClients = 50;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfConcurrentClients);

        // CountDownLatch è impiegato come barriera di sincronizzazione per sospendere
        // l'esecuzione dei thread fino a quando non sono tutti inizializzati, massimizzando
        // così la probabilità di innescare una Race Condition.
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numberOfConcurrentClients);

        // Utilizzo di una variabile atomica per garantire un conteggio thread-safe dei successi
        AtomicInteger successfulDeliveries = new AtomicInteger(0);
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();

        Email testEmail = new Email(
                "ID_STRESS_TEST",
                "stress.tester@unito.it",
                List.of("target.concorrenza@unito.it"),
                "Stress Test",
                "Test di invio simultaneo",
                LocalDateTime.now()
        );

        for (int i = 0; i < numberOfConcurrentClients; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Attende il segnale di sblocco globale

                    try (Socket socket = new Socket(HOST, PORT);
                         PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                        out.println(Protocol.build(Protocol.CMD_SEND_EMAIL));
                        out.println(mapper.writeValueAsString(testEmail));

                        if (Protocol.RES_OK.equals(in.readLine())) {
                            successfulDeliveries.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Errore nel thread client: " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // Segnale di sblocco: tutti i thread partono nel medesimo istante
        startLatch.countDown();
        // Sospende il thread principale finché tutti i worker non hanno concluso
        endLatch.await();
        executor.shutdown();

        // Validazione finale: l'uso dei ReentrantReadWriteLock nel backend
        // deve aver impedito qualsiasi corruzione e garantito 50 scritture perfette.
        Assertions.assertEquals(numberOfConcurrentClients, successfulDeliveries.get(),
                "Violazione della mutua esclusione: alcuni invii sono falliti.");
    }
}