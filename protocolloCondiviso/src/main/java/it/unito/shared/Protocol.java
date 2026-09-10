package it.unito.shared;

/**
 * Protocollo di comunicazione standardizzato tra il Mail Client e il Mail Server.
 * Definisce le costanti per i comandi e i codici di risposta, consentendo una
 * trasmissione testuale efficiente (stateless) tramite socket.
 */
public final class Protocol {

    private Protocol() {}

    /* ==================================
       COMANDI INVIATI DAL CLIENT (RICHIESTE)
       ================================== */

    /** Richiesta per la verifica dell'esistenza di un account utente sul server. */
    public static final String CMD_CHECK_USER = "CHECK_USER";

    /** Richiesta per il recupero differenziale o totale delle email. */
    public static final String CMD_FETCH_NEW = "FETCH_NEW";

    /** Richiesta di inoltro di una nuova email. */
    public static final String CMD_SEND_EMAIL = "SEND_EMAIL";

    /** Richiesta di eliminazione logica/fisica di un messaggio. */
    public static final String CMD_DELETE_EMAIL = "DELETE_EMAIL";

    /** Richiesta di chiusura formale della connessione socket. */
    public static final String CMD_QUIT = "QUIT";

    /* ==================================
       CODICI DI RISPOSTA DEL SERVER
       ================================== */

    /** Esito positivo generico. */
    public static final String RES_OK = "OK";
    /** Esito negativo generico. */
    public static final String RES_ERROR = "ERROR";

    /** L'utente specificato è registrato nel sistema. */
    public static final String RES_USER_EXISTS = "USER_EXISTS";
    /** L'utente specificato non è presente nel sistema. */
    public static final String RES_USER_NOT_FOUND = "USER_NOT_FOUND";

    /** Sono presenti nuovi messaggi per il client. */
    public static final String RES_NEW_MESSAGES = "NEW_MESSAGES";
    /** La casella di posta è già aggiornata. */
    public static final String RES_NO_NEW_MESSAGES = "NO_NEW_MESSAGES";

    /* ==================================
       CODICI DI ERRORE STANDARDIZZATI
       ================================== */

    /** Il comando inviato non rispetta le specifiche del protocollo. */
    public static final String ERR_INVALID_CMD = "ERR_INVALID_COMMAND";
    /** Il formato dell'indirizzo email non è valido o è malformato. */
    public static final String ERR_INVALID_EMAIL = "ERR_INVALID_EMAIL_FORMAT";
    /** L'utente mittente o destinatario è sconosciuto. */
    public static final String ERR_UNKNOWN_USER = "ERR_UNKNOWN_USER";
    /** Il server non è riuscito a recapitare il messaggio. */
    public static final String ERR_DELIVERY_FAILED = "ERR_DELIVERY_FAILED";
    /** Eccezione interna non gestita lato server. */
    public static final String ERR_SERVER_INTERNAL = "ERR_SERVER_INTERNAL";

    /* ==================================
       METODI DI SUPPORTO AL PROTOCOLLO
       ================================== */

    /**
     * Costruisce un payload testuale conforme al protocollo.
     * Il formato risultante è: {@code COMANDO;parametro1;parametro2;...}
     *
     * @param command Il comando principale da inviare (es. {@code CMD_CHECK_USER}).
     * @param params  I parametri opzionali associati al comando.
     * @return La stringa formattata, pronta per la trasmissione su stream di rete.
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
     * Esegue il parsing di un messaggio in ingresso estraendone i token.
     *
     * @param message La stringa grezza ricevuta dal socket.
     * @return Un array di stringhe contenente il comando (all'indice 0) e i relativi parametri.
     */
    public static String[] parse(String message) {
        if (message == null || message.isBlank()) {
            return new String[0];
        }
        return message.split(";");
    }
}