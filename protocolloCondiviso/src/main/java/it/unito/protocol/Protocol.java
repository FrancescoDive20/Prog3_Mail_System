package it.unito.protocol;

/**
 * Protocollo di comunicazione standardizzato tra mail-client e mail-server.
 *
 * Ogni comando e risposta è espresso come semplice stringa (testuale),
 * così da essere trasmesso facilmente su socket.
 *
 * Il protocollo è minimale, robusto e facilmente estendibile.
 */
public final class Protocol {

    private Protocol() {}

    /* ============================
       COMANDI INVIATI DAL CLIENT
       ============================ */

    // Client → Server: verifica esistenza utente
    public static final String CMD_CHECK_USER = "CHECK_USER";

    // Client → Server: ottieni nuovi messaggi
    public static final String CMD_FETCH_NEW = "FETCH_NEW";

    // Client → Server: invio email
    public static final String CMD_SEND_EMAIL = "SEND_EMAIL";

    // Client → Server: cancella un messaggio
    public static final String CMD_DELETE_EMAIL = "DELETE_EMAIL";

    // Client → Server: chiusura connessione
    public static final String CMD_QUIT = "QUIT";


    /* ============================
       RISPOSTE DEL SERVER
       ============================ */

    public static final String RES_OK = "OK";
    public static final String RES_ERROR = "ERROR";

    // Risposta: utente trovato
    public static final String RES_USER_EXISTS = "USER_EXISTS";

    // Risposta: utente inesistente
    public static final String RES_USER_NOT_FOUND = "USER_NOT_FOUND";

    // Risposta: messaggi nuovi disponibili
    public static final String RES_NEW_MESSAGES = "NEW_MESSAGES";

    // Risposta: nessun nuovo messaggio
    public static final String RES_NO_NEW_MESSAGES = "NO_NEW_MESSAGES";


    /* ============================
       ERRORI STANDARDIZZATI
       ============================ */

    public static final String ERR_INVALID_CMD = "ERR_INVALID_COMMAND";
    public static final String ERR_INVALID_EMAIL = "ERR_INVALID_EMAIL_FORMAT";
    public static final String ERR_UNKNOWN_USER = "ERR_UNKNOWN_USER";
    public static final String ERR_DELIVERY_FAILED = "ERR_DELIVERY_FAILED";
    public static final String ERR_SERVER_INTERNAL = "ERR_SERVER_INTERNAL";


    /* ============================
       METODI DI SUPPORTO
       ============================ */

    /**
     * Costruisce un messaggio protocollo come:
     * CMD;param1;param2;...
     */
    public static String build(String command, String... params) {
        if (params == null || params.length == 0) {
            return command;
        }

        StringBuilder sb = new StringBuilder(command);

        for (String p : params) {
            sb.append(";").append(p);
        }

        return sb.toString();
    }


    /**
     * Divide un messaggio di protocollo nei suoi campi.
     */
    public static String[] parse(String message) {
        if (message == null || message.isBlank()) {
            return new String[0];
        }
        return message.split(";");
    }
}