package it.unito.mailclient;

import it.unito.shared.Email;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MailClientController {

    @FXML private TextField loginEmailField;
    @FXML private Button loginButton;
    @FXML private VBox mainBox;
    @FXML private Label statusLabel;
    @FXML private Label userLabel;
    @FXML private ListView<Email> inboxListView;

    // Campi composizione
    @FXML private TextField toField;
    @FXML private TextField subjectField;
    @FXML private TextArea bodyArea;
    @FXML private Button sendButton;

    private DataModel dataModel;
    private NetworkClient networkClient;
    private SyncScheduler syncScheduler;
    private ExecutorService actionExecutor;

    // Validazione Regex robusta
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    public void initialize() {
        dataModel = new DataModel();
        networkClient = new NetworkClient("127.0.0.1", 8081);
        syncScheduler = new SyncScheduler(networkClient, dataModel);
        actionExecutor = Executors.newCachedThreadPool(); // Per azioni spot come login o invio

        // Binding reattivo tra Modello e View
        userLabel.textProperty().bind(dataModel.currentUserProperty());
        statusLabel.textProperty().bind(dataModel.connectionStatusProperty());
        inboxListView.setItems(dataModel.getInbox());

        // Personalizzazione celle ListView (opzionale ma consigliata)
        inboxListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Email item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getSender() + " - " + item.getSubject()); // [cite: 40, 46]
                }
            }
        });

        mainBox.setDisable(true); // Disabilita l'interfaccia principale fino al login
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String email = loginEmailField.getText().trim();
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showAlert(Alert.AlertType.ERROR, "Errore di Validazione", "Inserisci un indirizzo email valido.");
            return;
        }

        loginButton.setDisable(true);
        dataModel.setConnectionStatus("Verifica credenziali in corso...");

        // Operazione di rete in background per non bloccare la GUI
        actionExecutor.submit(() -> {
            try {
                boolean exists = networkClient.checkUser(email);

                Platform.runLater(() -> {
                    if (exists) {
                        dataModel.setCurrentUser(email);
                        dataModel.setConnectionStatus("Connesso come " + email);
                        mainBox.setDisable(false);
                        loginEmailField.setDisable(true);
                        syncScheduler.startPolling(); // Avvia il polling asincrono
                    } else {
                        dataModel.setConnectionStatus("Utente non trovato.");
                        showAlert(Alert.AlertType.WARNING, "Login Fallito", "L'utente non esiste sul server.");
                        loginButton.setDisable(false);
                    }
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    dataModel.setConnectionStatus("Errore di connessione.");
                    showAlert(Alert.AlertType.ERROR, "Errore di Rete", "Impossibile contattare il server.");
                    loginButton.setDisable(false);
                });
            }
        });
    }

    @FXML
    private void handleSendEmail(ActionEvent event) {
        String recipientsRaw = toField.getText().trim();
        String subject = subjectField.getText().trim();
        String body = bodyArea.getText();

        if (recipientsRaw.isEmpty() || subject.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Campi Obbligatori", "Destinatario e Oggetto non possono essere vuoti.");
            return;
        }

        // Parsing e validazione destinatari (separati da virgola o punto e virgola)
        List<String> recipients = Arrays.stream(recipientsRaw.split("[,;]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        for (String rec : recipients) {
            if (!EMAIL_PATTERN.matcher(rec).matches()) {
                showAlert(Alert.AlertType.ERROR, "Errore di Validazione", "Indirizzo destinatario non valido: " + rec);
                return;
            }
        }

        sendButton.setDisable(true);
        dataModel.setConnectionStatus("Invio messaggio in corso...");

        // Costruzione dell'oggetto Email [cite: 13, 21]
        Email newEmail = new Email(
                UUID.randomUUID().toString(),
                dataModel.getCurrentUser(),
                recipients,
                subject,
                body,
                LocalDateTime.now()
        );

        // Invio in background
        actionExecutor.submit(() -> {
            try {
                boolean success = networkClient.sendEmail(newEmail);
                Platform.runLater(() -> {
                    if (success) {
                        dataModel.setConnectionStatus("Messaggio inviato con successo.");
                        clearComposeFields();
                    } else {
                        dataModel.setConnectionStatus("Errore: invio fallito dal server.");
                        showAlert(Alert.AlertType.ERROR, "Errore Server", "Il server ha rifiutato l'invio.");
                    }
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    dataModel.setConnectionStatus("Errore di rete durante l'invio.");
                    showAlert(Alert.AlertType.ERROR, "Errore di Rete", "Impossibile inviare il messaggio.");
                });
            } finally {
                Platform.runLater(() -> sendButton.setDisable(false));
            }
        });
    }

    private void clearComposeFields() {
        toField.clear();
        subjectField.clear();
        bodyArea.clear();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Metodo da chiamare alla chiusura dell'app (dal MailClientApp)
    public void shutdown() {
        if (syncScheduler != null) syncScheduler.stopPolling();
        if (actionExecutor != null) actionExecutor.shutdownNow();
    }
}