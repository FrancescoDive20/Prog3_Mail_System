package it.unito.mailserver;

import it.unito.shared.Protocol;
import it.unito.shared.Email;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.io.IOException;

/**
 * Task eseguito dal Thread Pool per gestire una singola sessione client.
 */
public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final ObjectMapper mapper;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        this.mapper = new ObjectMapper();
        this.mapper.findAndRegisterModules(); // Necessario per LocalDateTime
    }

    @Override
    public void run() {
        // Vincolo: try-with-resources per chiusura tassativa di Socket, Reader e Writer
        try (
                clientSocket;
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String requestLine;
            while ((requestLine = in.readLine()) != null) {

                // Parsing conforme al protocollo condiviso
                String[] parsedCommand = Protocol.parse(requestLine);
                if (parsedCommand.length == 0) continue;

                String command = parsedCommand[0];
                MailServerApp.logInfo("Ricevuto comando: " + command);

                switch (command) {
                    case Protocol.CMD_CHECK_USER:
                        handleCheckUser(parsedCommand, out);
                        break;
                    case Protocol.CMD_FETCH_NEW:
                        handleFetchNew(parsedCommand, out);
                        break;
                    case Protocol.CMD_SEND_EMAIL:
                        handleSendEmail(parsedCommand, out, in);
                        break;
                    case Protocol.CMD_QUIT:
                        out.println(Protocol.RES_OK);
                        return; // Esce dal ciclo, innescando la chiusura automatica delle risorse
                    default:
                        out.println(Protocol.ERR_INVALID_CMD);
                }
            }
        } catch (IOException e) {
            MailServerApp.logInfo("Disconnessione anomala del client: " + e.getMessage());
        }
    }

    private void handleCheckUser(String[] params, PrintWriter out) {
        // Implementazione verifica utente
        out.println(Protocol.RES_USER_EXISTS);
    }

    private void handleFetchNew(String[] params, PrintWriter out) {
        // Esempio: recupero dal MailboxManager e invio JSON
        out.println(Protocol.RES_NO_NEW_MESSAGES);
    }

    private void handleSendEmail(String[] params, PrintWriter out, BufferedReader in) throws IOException {
        // Il client invierà il JSON dell'email nella riga successiva
        String jsonEmail = in.readLine();
        if (jsonEmail != null) {
            Email email = mapper.readValue(jsonEmail, Email.class);
            MailServerApp.logInfo("Elaborazione email per: " + email.getRecipients());

            // Salvataggio concorrente tramite MailboxManager
            boolean success = MailboxManager.getInstance().saveEmailToRecipients(email);
            out.println(success ? Protocol.RES_OK : Protocol.ERR_DELIVERY_FAILED);
        } else {
            out.println(Protocol.ERR_INVALID_EMAIL);
        }
    }
}