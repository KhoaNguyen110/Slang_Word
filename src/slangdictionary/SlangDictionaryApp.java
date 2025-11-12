package src.slangdictionary;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import src.view.MainView;

public class SlangDictionaryApp extends Application {
    @Override
    public void start(Stage stage) {
        stage.setTitle("Slang Dictionary");
        stage.setScene(new Scene(new MainView().getRoot(), 800, 600));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
