package no.ntnu.datakomm.chat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Class representing the main Graphical User Interface (GUI). JavaFX interface.
 */
public class App extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * This method is called automatically by JavaFX when the application is
     * launched
     *
     * @param primaryStage The main "stage" where the GUI will be rendered
     */
    @Override
    public void start(Stage primaryStage) {
        URL fxmlUrl = getClass().getResource("layout.fxml");
        URL cssUrl = getClass().getResource("styles/style.css");
        URL iconUrl = getClass().getResource("styles/ntnu.png");
        Parent root = null;
        boolean loaded = false;
        if (fxmlUrl != null && cssUrl != null && iconUrl != null) {
            try {
                root = FXMLLoader.load(fxmlUrl);
                Scene scene = new Scene(root, 600, 400);
                scene.getStylesheets().add(cssUrl.toURI().toString());
                primaryStage.setTitle("NTNU Ålesund - ChatClient");
                primaryStage.setScene(scene);
                Image anotherIcon = null;
                anotherIcon = new Image(iconUrl.toURI().toString());
                primaryStage.getIcons().add(anotherIcon);
                primaryStage.show();
                loaded = true;
            } catch (URISyntaxException | IOException e) {
                System.out.println("Error while loading FXML: " + e.getMessage());
            }
        }
        if (!loaded) {
            if (fxmlUrl == null) {
                System.out.println("FXML file not found!");
            }
            if (cssUrl == null) {
                System.out.println("CSS file not found!");
            }
            if (iconUrl == null) {
                System.out.println("Icon file not found!");
            }
            Platform.exit();
        }
    }
}