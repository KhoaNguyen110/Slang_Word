package src.slangdictionary;

import javafx.application.Application;
import javafx.stage.Stage;
import src.view.*;

public class SlangDictionaryApp extends Application {
    @Override
    public void start(Stage stage) {
        ViewManager.getInstance().setStage(stage);
        stage.setTitle("Slang Dictionary");
        
        ViewManager.getInstance().switchScene(new MainMenuView().getScene());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
