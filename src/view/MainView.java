package src.view;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import src.controller.*;
import src.model.SlangWord;

public class MainView {
    private BorderPane root;
    private SlangController controller;

    public MainView() {
        controller = new SlangController();
        root = new BorderPane();

        TextField searchField = new TextField();
        Button searchBtn = new Button("Search");
        TextArea resultArea = new TextArea();

        searchBtn.setOnAction(e -> {
            String query = searchField.getText();
            SlangWord result = controller.searchByWord(query);
            resultArea.setText(result == null ? "Not found" : result.toString());
        });

        VBox top = new VBox(new Label("Enter Slang Word:"), searchField, searchBtn);
        root.setTop(top);
        root.setCenter(resultArea);
    }

    public BorderPane getRoot() { return root; }
}
