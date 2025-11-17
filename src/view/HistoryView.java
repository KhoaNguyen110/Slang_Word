package src.view;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import src.controller.SlangController;
import src.model.SearchHistoryEntry;

import java.util.List;
import java.util.stream.Collectors;

/**
 * HistoryView
 * - Hiển thị lịch sử tìm kiếm (không hiển thị thời gian).
 * - Không tạo SlangController mới; dùng chung instance được truyền vào.
 */
public class HistoryView {
    private final SlangController controller;
    private final ObservableList<SearchHistoryEntry> items = FXCollections.observableArrayList();
    private ListView<SearchHistoryEntry> listView;

    
    public HistoryView(SlangController controller) {
        this.controller = controller;
    }

    public Scene getScene() {
        Label title = new Label("History");
        Button btnBack = new Button("← Back");
        Button btnClearAll = new Button("Clear All");

        listView = new ListView<>(items);
        listView.setPlaceholder(new Label("No history available"));
        listView.setCellFactory(lv -> new HistoryListCell());
        listView.setPrefHeight(420);

        btnBack.setOnAction(e -> {
            // Quay lại menu chính nhưng tái sử dụng cùng controller
            ViewManager.getInstance().switchScene(new MainMenuView(controller).getScene());
        });

        btnClearAll.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                    "Are you sure you want to clear all search history?",
                    ButtonType.YES, ButtonType.NO);
            a.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.YES) {
                    controller.clearSearchHistory();
                    refreshList();
                }
            });
        });

        HBox top = new HBox(8, title, btnClearAll, btnBack);
        top.setPadding(new Insets(8));

        VBox root = new VBox(10, top, listView);
        root.setPadding(new Insets(12));
        root.setPrefSize(800, 600);

        refreshList();
        return new Scene(root);
    }

    private void refreshList() {
        List<SearchHistoryEntry> history = controller.getSearchHistory();
        items.setAll(history);
    }

    // Custom ListCell: hiển thị query/type/results (không timestamp), nút Remove hiện khi hover
    private class HistoryListCell extends ListCell<SearchHistoryEntry> {
        private final HBox container = new HBox(8);
        private final Label lbl = new Label();
        private final Button btnRemove = new Button("Remove");
        private final Region spacer = new Region();

        HistoryListCell() {
            lbl.setWrapText(true);
            HBox.setHgrow(lbl, Priority.ALWAYS);
            HBox.setHgrow(spacer, Priority.ALWAYS);

            btnRemove.setVisible(false);
            btnRemove.setManaged(false);

            container.setPadding(new Insets(6));
            container.getChildren().addAll(lbl, spacer, btnRemove);

            container.addEventFilter(MouseEvent.MOUSE_ENTERED, ev -> {
                btnRemove.setVisible(true);
                btnRemove.setManaged(true);
            });
            container.addEventFilter(MouseEvent.MOUSE_EXITED, ev -> {
                btnRemove.setVisible(false);
                btnRemove.setManaged(false);
            });

            btnRemove.setOnAction(e -> {
                SearchHistoryEntry entry = getItem();
                if (entry == null) return;
                List<SearchHistoryEntry> hist = controller.getSearchHistory();
                int idx = hist.indexOf(entry);
                if (idx >= 0) {
                    boolean ok = controller.deleteHistoryEntry(idx);
                    if (ok) {
                        refreshList();
                    } else {
                        Alert err = new Alert(Alert.AlertType.ERROR, "Remove failed.", ButtonType.OK);
                        err.showAndWait();
                    }
                } else {
                    refreshList();
                }
            });
        }

        @Override
        protected void updateItem(SearchHistoryEntry entry, boolean empty) {
            super.updateItem(entry, empty);
            if (empty || entry == null) {
                setGraphic(null);
                setText(null);
            } else {
                String results = (entry.getResultWords() == null || entry.getResultWords().isEmpty())
                        ? "(no results)"
                        : entry.getResultWords().stream().collect(Collectors.joining(", "));
                String text = String.format("%s: \"%s\" -> %s", entry.getType(), entry.getQuery(), results);
                lbl.setText(text);
                setGraphic(container);
            }
        }
    }
}