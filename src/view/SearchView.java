package src.view;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import src.controller.SlangController;
import java.util.List;
import src.model.SlangWord;

public class SearchView {
    private SlangController controller = new SlangController();

    public Scene getScene() {
        Label title = new Label("🔍 Search Slang Word");
        TextField input = new TextField();
        Button btnSearch = new Button("Search");
        Button btnShow = new Button("Show all Slang Words");    
        Button btnBack = new Button("← Back");
        TextArea result = new TextArea();

        btnSearch.setOnAction(e -> {
            SlangWord sw = controller.searchByWord(input.getText());
            List<SlangWord> defs = controller.searchByDefinition(input.getText());
            if (defs != null && !defs.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (SlangWord defSw : defs) {
                    sb.append(defSw.toString()).append("\n");
                }
                result.setText(sb.toString());
            } else
            result.setText(sw == null ? "Not found" : sw.toString());
        });

        btnBack.setOnAction(e -> {
            ViewManager.getInstance().switchScene(new MainMenuView().getScene());
        });

        btnShow.setOnAction(e -> {
            StringBuilder allWords = new StringBuilder();
            for (SlangWord sw : controller.getAllSlang().values()) {
                allWords.append(sw.toString()).append("\n");
            }
            result.setText(allWords.toString());
        });

        VBox vbox = new VBox(title, input, btnSearch, btnShow, result, btnBack);
        VBox.setVgrow(result, Priority.ALWAYS);
        vbox.setSpacing(15);
        vbox.setStyle("-fx-padding: 20;");
        return new Scene(vbox, 600, 400);
    }
}
