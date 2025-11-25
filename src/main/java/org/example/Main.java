package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.util.WindowResizer;

public class Main extends Application {

    private org.example.controller.MainController mainController;

    @Override
    public void start(Stage stage) throws Exception {


        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/org/example/fxml/main-view.fxml"));

        Parent mainContent = fxmlLoader.load();
        mainController = fxmlLoader.getController();

        StackPane contentWrapper = new StackPane(mainContent);
        contentWrapper.getStyleClass().add("app-window");
        Rectangle clip = new Rectangle();
        clip.setArcWidth(28);
        clip.setArcHeight(28);
        clip.widthProperty().bind(contentWrapper.widthProperty());
        clip.heightProperty().bind(contentWrapper.heightProperty());
        contentWrapper.setClip(clip);

        StackPane windowRoot = new StackPane(contentWrapper);
        windowRoot.getStyleClass().add("app-window-shadow");

        Scene scene = new Scene(windowRoot, 1400, 1000);
        scene.setFill(Color.TRANSPARENT);

        ThemeManager.setScene(scene);

        ThemeManager.applyTheme("dark.css");

        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle("mejais");
        stage.setScene(scene);
        stage.setMinWidth(1100);
        stage.setMinHeight(720);
        stage.setResizable(true);
        WindowResizer.makeResizable(stage, windowRoot);
        stage.show();

    }

    @Override
    public void stop() {
        if (mainController != null) {
            mainController.stop();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}

