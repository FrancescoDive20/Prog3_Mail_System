package it.unito.mailclient;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

/**
 * Entry point dell'applicazione JavaFX.
 * Istanzia il grafo della Scena a partire dal file dichiarativo FXML,
 * stabilendo formalmente il collegamento tra View e Controller.
 */
public class MailClientApp extends Application {

    private MailClientController controller;

    @Override
    public void start(Stage primaryStage) throws Exception {
        URL fxmlLocation = getClass().getResource("/fxml/MailClientView.fxml");
        if (fxmlLocation == null) {
            System.err.println("File FXML non trovato. Verifica il path delle risorse.");
            System.exit(1);
        }

        FXMLLoader loader = new FXMLLoader(fxmlLocation);
        Parent root = loader.load();

        controller = loader.getController();

        primaryStage.setTitle("Mail Client - JavaFX");
        primaryStage.setScene(new Scene(root, 800, 600));

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