package src.view;

import javafx.scene.Scene;
import javafx.stage.Stage;

public class ViewManager {
    private static ViewManager instance;
    private Stage primaryStage;

    private ViewManager() {}

    public static ViewManager getInstance() {
        if (instance == null) instance = new ViewManager();
        return instance;
    }

    public void setStage(Stage stage) {
        this.primaryStage = stage;
    }

    public void switchScene(Scene scene) {
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
