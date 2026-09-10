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
 * Gestore dell'I/O di rete.
 * Questa classe incapsula le logiche di connessione tramite Java Socket.
 * Adotta un approccio "Stateless": il socket viene aperto e chiuso ad ogni operazione.
 * Tale paradigma garantisce un elevato livello di scalabilità e resilienza ai guasti di rete.
 */
public class NetworkClient {

    private final String serverHost;
    private final int serverPort;
    private final ObjectMapper mapper;

    public NetworkClient(String host, int port) {
        this.serverHost = host;
        this.serverPort = port;
        this.mapper = new ObjectMapper();
        this.mapper.findAndRegisterModules();
    }

    /**
     * Interroga il server per verificare la presenza di un utente.
     *
     * @param emailAddress L'indirizzo da validare lato backend.
     * @return {@code true} se l'utente esiste.
     * @throws IOException In caso di irraggiungibilità del server.
     */
    public boolean checkUser(String emailAddress) throws IOException {
        try (Socket socket = new Socket(serverHost, serverPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println(Protocol.build(Protocol.CMD_CHECK_USER, emailAddress));
            String response = in.readLine();
            return Protocol.RES_USER_EXISTS.equals(response);
        }
    }

    /**
     * Invia un oggetto Email serializzato (marshalling JSON) al server.
     *
     * @param email L'istanza dell'email da trasmettere.
     * @return {@code true} se il salvataggio remoto avviene con successo.
     */
    public boolean sendEmail(Email email) throws IOException {
        try (Socket socket = new Socket(serverHost, serverPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println(Protocol.build(Protocol.CMD_SEND_EMAIL));
            out.println(mapper.writeValueAsString(email));
            String response = in.readLine();
            return Protocol.RES_OK.equals(response);
        }
    }

    /**
     * Esegue il recupero differenziale dei messaggi (delta sync).
     * Ottimizza l'utilizzo della banda passante inviando al server l'ID dell'ultima email nota.
     *
     * @param userEmail L'utente che richiede i messaggi.
     * @param lastKnownMessageId L'ID dell'ultimo messaggio ricevuto, oppure null al primo avvio.
     * @return La lista deserializzata delle nuove email.
     */
    public List<Email> fetchNewMessages(String userEmail, String lastKnownMessageId) throws IOException {
        try (Socket socket = new Socket(serverHost, serverPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            String cmd = lastKnownMessageId == null ?
                    Protocol.build(Protocol.CMD_FETCH_NEW, userEmail) :
                    Protocol.build(Protocol.CMD_FETCH_NEW, userEmail, lastKnownMessageId);

            out.println(cmd);
            String response = in.readLine();

            if (Protocol.RES_NEW_MESSAGES.equals(response)) {
                String jsonPayload = in.readLine();
                if (jsonPayload != null) {
                    return mapper.readValue(jsonPayload, new TypeReference<List<Email>>() {});
                }
            }
            return new ArrayList<>();
        }
    }

    /**
     * Trasmette il comando di eliminazione permanente di un messaggio.
     *
     * @param userEmail Il proprietario della mailbox.
     * @param emailId L'identificatore univoco del messaggio da eliminare.
     * @return {@code true} in caso di avvenuta cancellazione sul server.
     */
    public boolean deleteEmail(String userEmail, String emailId) throws IOException {
        try (Socket socket = new Socket(serverHost, serverPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println(Protocol.build(Protocol.CMD_DELETE_EMAIL, userEmail, emailId));
            return Protocol.RES_OK.equals(in.readLine());
        }
    }
}