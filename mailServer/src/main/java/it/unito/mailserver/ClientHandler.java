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
 * Task (Runnable) delegato dal Thread Pool per la gestione concorrente di una singola
 * connessione client. Implementa la logica di parsing dei comandi del protocollo e
 * coordina l'interazione con il livello di persistenza (MailboxManager).
 */
public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final ObjectMapper mapper;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        this.mapper = new ObjectMapper();
        this.mapper.findAndRegisterModules();
    }

    /**
     * Esegue il ciclo di vita della richiesta.
     * Utilizza il costrutto try-with-resources per garantire la chiusura deterministica
     * degli stream e del socket al termine della comunicazione (approccio stateless).
     */
    @Override
    public void run() {
        try (
                clientSocket;
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String requestLine;
            while ((requestLine = in.readLine()) != null) {

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
                    case Protocol.CMD_DELETE_EMAIL:
                        handleDeleteEmail(parsedCommand, out);
                        break;
                    case Protocol.CMD_QUIT:
                        out.println(Protocol.RES_OK);
                        return;
                    default:
                        out.println(Protocol.ERR_INVALID_CMD);
                }
            }
        } catch (IOException e) {
            MailServerApp.logInfo("Disconnessione anomala del client: " + e.getMessage());
        }
    }

    private void handleCheckUser(String[] params, PrintWriter out) {
        if (params.length < 2) {
            out.println(Protocol.ERR_INVALID_CMD);
            return;
        }
        String email = params[1];
        java.io.File mailbox = new java.io.File("server_storage", email + "_inbox.json");

        MailServerApp.logInfo("Verifica utente: " + email);

        if (mailbox.exists()) {
            out.println(Protocol.RES_USER_EXISTS);
        } else {
            out.println(Protocol.RES_USER_NOT_FOUND);
        }
    }

    /**
     * Recupera i nuovi messaggi implementando un meccanismo di fetch differenziale (delta).
     * Riduce il carico di rete trasmettendo esclusivamente le email successive all'ultimo ID noto.
     */
    private void handleFetchNew(String[] params, PrintWriter out) {
        if (params.length < 2) {
            out.println(Protocol.ERR_INVALID_CMD);
            return;
        }
        String email = params[1];
        String lastId = params.length > 2 ? params[2] : null;

        try {
            java.util.List<it.unito.shared.Email> allEmails = MailboxManager.getInstance().loadEmailsForUser(email);
            java.util.List<it.unito.shared.Email> newEmails = new java.util.ArrayList<>();

            if (lastId == null || lastId.equals("null")) {
                newEmails.addAll(allEmails);
            } else {
                boolean found = false;
                for (it.unito.shared.Email e : allEmails) {
                    if (found) newEmails.add(e);
                    if (e.getId().equals(lastId)) found = true;
                }
                if (!found) newEmails.addAll(allEmails);
            }

            if (newEmails.isEmpty()) {
                out.println(Protocol.RES_NO_NEW_MESSAGES);
            } else {
                out.println(Protocol.RES_NEW_MESSAGES);
                out.println(mapper.writeValueAsString(newEmails));
            }
        } catch (Exception e) {
            out.println(Protocol.ERR_SERVER_INTERNAL);
        }
    }

    private void handleSendEmail(String[] params, PrintWriter out, BufferedReader in) throws IOException {
        String jsonEmail = in.readLine();
        if (jsonEmail != null) {
            Email email = mapper.readValue(jsonEmail, Email.class);
            MailServerApp.logInfo("Elaborazione email per: " + email.getRecipients());

            boolean success = MailboxManager.getInstance().saveEmailToRecipients(email);
            out.println(success ? Protocol.RES_OK : Protocol.ERR_DELIVERY_FAILED);
        } else {
            out.println(Protocol.ERR_INVALID_EMAIL);
        }
    }

    private void handleDeleteEmail(String[] params, PrintWriter out) {
        if (params.length < 3) {
            out.println(Protocol.ERR_INVALID_CMD);
            return;
        }
        String userEmail = params[1];
        String emailId = params[2];
        boolean deleted = MailboxManager.getInstance().deleteEmailForUser(userEmail, emailId);
        out.println(deleted ? Protocol.RES_OK : Protocol.ERR_SERVER_INTERNAL);
    }
}