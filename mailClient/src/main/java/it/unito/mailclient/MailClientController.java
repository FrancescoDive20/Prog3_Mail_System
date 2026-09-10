package it.unito.mailclient;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
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

/**
 * Controller dell'architettura MVC basato sul paradigma Event-Driven.
 * Intercetta le azioni dell'utente sulla View (Event Handlers), demanda l'I/O di rete
 * a un Thread Pool dedicato per non bloccare la GUI, e aggiorna il DataModel.
 */
public class MailClientController {

    @FXML private TextField loginEmailField;
    @FXML private Button loginButton;
    @FXML private VBox mainBox;
    @FXML private Label statusLabel;
    @FXML private Label userLabel;
    @FXML private ListView<Email> inboxListView;
    @FXML private TextArea readArea;

    @FXML private TextField toField;
    @FXML private TextField subjectField;
    @FXML private TextArea bodyArea;
    @FXML private Button sendButton;

    private DataModel dataModel;
    private NetworkClient networkClient;
    private SyncScheduler syncScheduler;

    /** Gestore dei thread delegato alle richieste spot (login, invio, eliminazione). */
    private ExecutorService actionExecutor;

    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    /**
     * Metodo di callback invocato dall'FXMLLoader al termine dell'iniezione delle dipendenze (@FXML).
     * Inizializza il Model, il layer di rete e stabilisce i vincoli di binding (Observer Pattern).
     */
    public void initialize() {

        FadeTransition fadeIn = new FadeTransition(Duration.millis(800), mainBox);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        dataModel = new DataModel();
        networkClient = new NetworkClient("127.0.0.1", 8081);
        syncScheduler = new SyncScheduler(networkClient, dataModel);
        actionExecutor = Executors.newCachedThreadPool();

        userLabel.textProperty().bind(dataModel.currentUserProperty());
        statusLabel.textProperty().bind(dataModel.connectionStatusProperty());
        inboxListView.setItems(dataModel.getInbox());

        inboxListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Email item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getSender() + " - " + item.getSubject());
                }
            }
        });

        inboxListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                readArea.setText("Da: " + newSelection.getSender() + "\nA: " + String.join(", ", newSelection.getRecipients()) +
                        "\nData: " + newSelection.getTimestamp() + "\n\n" + newSelection.getBody());
            } else {
                readArea.clear();
            }
        });

        mainBox.setDisable(true);
    }

    /**
     * Gestisce la procedura di login.
     * Effettua la validazione formale tramite espressioni regolari (Regex) prima
     * di interrogare asincronamente il server, minimizzando il traffico di rete inutile.
     */
    @FXML
    private void handleLogin(ActionEvent event) {
        String email = loginEmailField.getText().trim();
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showAlert(Alert.AlertType.ERROR, "Errore di Validazione", "Indirizzo email non valido.");
            return;
        }
        loginButton.setDisable(true);
        dataModel.setConnectionStatus("Verifica in corso...");

        actionExecutor.submit(() -> {
            try {
                boolean exists = networkClient.checkUser(email);
                Platform.runLater(() -> {
                    if (exists) {
                        dataModel.setCurrentUser(email);
                        dataModel.setConnectionStatus("Connesso come " + email);
                        mainBox.setDisable(false);
                        loginEmailField.setDisable(true);
                        syncScheduler.startPolling();
                    } else {
                        dataModel.setConnectionStatus("Utente non trovato.");
                        showAlert(Alert.AlertType.WARNING, "Login Fallito", "Utente inesistente.");
                        loginButton.setDisable(false);
                    }
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    dataModel.setConnectionStatus("Errore di connessione.");
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
            showAlert(Alert.AlertType.WARNING, "Campi vuoti", "Destinatario e Oggetto obbligatori.");
            return;
        }

        List<String> recipients = Arrays.stream(recipientsRaw.split("[,;]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        for (String rec : recipients) {
            if (!EMAIL_PATTERN.matcher(rec).matches()) {
                showAlert(Alert.AlertType.ERROR, "Errore", "Indirizzo destinatario non valido: " + rec);
                return;
            }
        }

        sendButton.setDisable(true);
        Email newEmail = new Email(UUID.randomUUID().toString(), dataModel.getCurrentUser(), recipients, subject, body, LocalDateTime.now());

        actionExecutor.submit(() -> {
            try {
                boolean success = networkClient.sendEmail(newEmail);
                Platform.runLater(() -> {
                    if (success) {
                        clearComposeFields();
                        dataModel.setConnectionStatus("Messaggio inviato.");
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Errore Server", "Invio rifiutato dal server.");
                    }
                });
            } catch (IOException e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Errore di Rete", "Server disconnesso. Invio fallito."));
            } finally {
                Platform.runLater(() -> sendButton.setDisable(false));
            }
        });
    }

    @FXML
    private void handleReply() {
        Email selected = inboxListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            toField.setText(selected.getSender());
            subjectField.setText("Re: " + selected.getSubject());
            bodyArea.setText("\n\n--- Messaggio Originale ---\n" + selected.getBody());
        }
    }

    @FXML
    private void handleReplyAll() {
        Email selected = inboxListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            String allRecipients = selected.getSender() + ", " + String.join(", ", selected.getRecipients());
            toField.setText(allRecipients.replace(dataModel.getCurrentUser() + ", ", "").replace(", " + dataModel.getCurrentUser(), ""));
            subjectField.setText("Re: " + selected.getSubject());
            bodyArea.setText("\n\n--- Messaggio Originale ---\n" + selected.getBody());
        }
    }

    @FXML
    private void handleForward() {
        Email selected = inboxListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            toField.clear();
            subjectField.setText("Fwd: " + selected.getSubject());
            bodyArea.setText("\n\n--- Messaggio Inoltrato da " + selected.getSender() + " ---\n" + selected.getBody());
        }
    }

    /**
     * Rimuove il messaggio sia in locale che sul server remoto.
     * Operazione asincrona che garantisce la responsività dell'interfaccia utente.
     */
    @FXML
    private void handleDelete() {
        Email selected = inboxListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            actionExecutor.submit(() -> {
                try {
                    boolean success = networkClient.deleteEmail(dataModel.getCurrentUser(), selected.getId());
                    if (success) {
                        Platform.runLater(() -> {
                            dataModel.getInbox().remove(selected);
                            readArea.clear();
                            dataModel.setConnectionStatus("Email eliminata.");
                        });
                    } else {
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Errore Server", "Impossibile rimuovere l'email dal server."));
                    }
                } catch (IOException e) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Errore Rete", "Server offline. Cancellazione fallita."));
                }
            });
        }
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

    /** Esegue un arresto controllato (graceful shutdown) dei thread in background. */
    public void shutdown() {
        if (syncScheduler != null) syncScheduler.stopPolling();
        if (actionExecutor != null) actionExecutor.shutdownNow();
    }
}