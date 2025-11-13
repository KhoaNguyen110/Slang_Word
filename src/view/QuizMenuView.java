package src.view;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

public class QuizMenuView {
    public Scene getScene() {
        Label title = new Label("🎯 Quiz Game");
        title.setFont(new Font("Arial", 32));

        Button btnTypeA = new Button("A — Guess Definition from Slang");
        Button btnTypeB = new Button("B — Guess Slang from Definition");
        Button btnBack = new Button("← Back to Home");

        // CSS cho nút bự
        btnTypeA.setStyle("-fx-font-size: 18px; -fx-padding: 15 30;");
        btnTypeB.setStyle("-fx-font-size: 18px; -fx-padding: 15 30;");
        btnBack.setStyle("-fx-font-size: 14px;");

        VBox root = new VBox(25, title, btnTypeA, btnTypeB, btnBack);
        root.setAlignment(Pos.CENTER);

        Scene scene = new Scene(root, 800, 600);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff, #e3f2fd);");

        btnTypeA.setOnAction(e -> ViewManager.getInstance().switchScene(new QuizGameView(true).getScene()));
        btnTypeB.setOnAction(e -> ViewManager.getInstance().switchScene(new QuizGameView(false).getScene()));
        btnBack.setOnAction(e -> ViewManager.getInstance().switchScene(new MainMenuView().getScene()));

        return scene;
    }
}
