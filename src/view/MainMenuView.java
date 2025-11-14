package src.view;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import src.controller.SlangController;

public class MainMenuView {
    private SlangController slangController = new SlangController();
    
    public Scene getScene() {
        Label banner = new Label("📘 SLANG DICTIONARY");
        banner.setFont(new Font("Arial", 28));

        Label subtitle = new Label("Explore, Learn and Have Fun!");
        subtitle.setFont(new Font("Arial", 16));

        VBox header = new VBox(banner, subtitle);
        header.setAlignment(Pos.CENTER);
        header.setSpacing(10);

        // Các nút chức năng
        Button btnSearch = new Button("🔍 Dictionary");
        Button btnQuiz = new Button("🎯 Quiz Game");
        Button btnHistory = new Button("📜 History");
        Button btnExit = new Button("❌ Exit");

        VBox menu = new VBox(btnSearch, btnQuiz, btnHistory, btnExit);
        menu.setSpacing(15);
        menu.setAlignment(Pos.CENTER);

        VBox root = new VBox(header, menu);
        root.setAlignment(Pos.CENTER);
        root.setSpacing(40);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #f9f9f9, #e0e0e0);"
                    + "-fx-padding: 50;");

        Scene scene = new Scene(root, 800, 600);



        // Xử lý chuyển cảnh
        btnSearch.setOnAction(e -> {
            ViewManager.getInstance().switchScene(new DictionaryView().getScene());
        });

        btnQuiz.setOnAction(e -> {
            ViewManager.getInstance().switchScene(new QuizMenuView().getScene());
        });

        btnHistory.setOnAction(e -> {
            ViewManager.getInstance().switchScene(new HistoryView().getScene());
        });

        btnExit.setOnAction(e -> System.exit(0));

        return scene;
    }
}
