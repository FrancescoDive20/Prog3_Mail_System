package it.unito.mailclient;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class MailClientApp extends Application {

    private MailClientController controller;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Assicurati che il file FXML sia presente nella cartella resources appropriata
        URL fxmlLocation = getClass().getResource("/fxml/MailClientView.fxml");
        if (fxmlLocation == null) {
            System.err.println("File FXML non trovato. Verifica il percorso delle resources.");
            System.exit(1);
        }

        FXMLLoader loader = new FXMLLoader(fxmlLocation);
        Parent root = loader.load();

        // Recupero il controller per poter invocare il metodo di shutdown
        controller = loader.getController();

        primaryStage.setTitle("Mail Client - JavaFX");
        primaryStage.setScene(new Scene(root, 800, 600));

        // Gestione pulita della chiusura
        primaryStage.setOnCloseRequest(event -> {
            if (controller != null) {
                controller.shutdown();
            }
            System.exit(0);
        });

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}