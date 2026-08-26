package it.unito.mailclient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unito.shared.Email;
import it.unito.shared.Protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestisce le operazioni di I/O di rete verso il server.
 * Implementa un approccio "apri-invia-ricevi-chiudi" per ogni richiesta.
 */
public class NetworkClient {

    private final String serverHost;
    private final int serverPort;
    private final ObjectMapper mapper;

    public NetworkClient(String host, int port) {
        this.serverHost = host;
        this.serverPort = port;
        this.mapper = new ObjectMapper();
        this.mapper.findAndRegisterModules(); // Supporto per LocalDateTime
    }

    /**
     * Verifica se un utente esiste sul server.
     */
    public boolean checkUser(String emailAddress) throws IOException {
        try (Socket socket = new Socket(serverHost, serverPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Invia richiesta costruita tramite il protocollo [cite: 136]
            out.println(Protocol.build(Protocol.CMD_CHECK_USER, emailAddress));
            String response = in.readLine();

            // Verifica la risposta [cite: 111]
            return Protocol.RES_USER_EXISTS.equals(response);
        }
    }

    /**
     * Invia un'email al server.
     */
    public boolean sendEmail(Email email) throws IOException {
        try (Socket socket = new Socket(serverHost, serverPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Comando principale [cite: 98]
            out.println(Protocol.build(Protocol.CMD_SEND_EMAIL));
            // Payload JSON [cite: 82]
            out.println(mapper.writeValueAsString(email));

            String response = in.readLine();
            return Protocol.RES_OK.equals(response); // [cite: 108]
        }
    }

    /**
     * Richiede nuovi messaggi per l'utente, opzionalmente fornendo l'ID dell'ultimo messaggio noto.
     */
    public List<Email> fetchNewMessages(String userEmail, String lastKnownMessageId) throws IOException {
        try (Socket socket = new Socket(serverHost, serverPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            String cmd = lastKnownMessageId == null ?
                    Protocol.build(Protocol.CMD_FETCH_NEW, userEmail) :
                    Protocol.build(Protocol.CMD_FETCH_NEW, userEmail, lastKnownMessageId);

            out.println(cmd); // [cite: 96]

            String response = in.readLine();

            if (Protocol.RES_NEW_MESSAGES.equals(response)) { // [cite: 115]
                // Il server ha risposto affermativamente, leggiamo il JSON successivo
                String jsonPayload = in.readLine();
                if (jsonPayload != null) {
                    return mapper.readValue(jsonPayload, new TypeReference<List<Email>>() {});
                }
            }
            return new ArrayList<>(); // Nessun nuovo messaggio o errore [cite: 117]
        }
    }
}