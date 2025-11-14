package src.view;

import javafx.application.Platform;
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
 * - Nếu trống thì hiện "No history available" (ListView placeholder).
 * - Mỗi entry có nút Remove (hiện khi hover).
 * - Có nút "Clear All" để xóa toàn bộ lịch sử.
 *
 * Ghi chú:
 * - Gọi controller.getSearchHistory(), controller.clearSearchHistory()
 * - Gọi controller.deleteHistoryEntry(index) để xóa entry đơn lẻ.
 *   Nếu controller của bạn dùng signature khác (ví dụ deleteHistoryEntry(SearchHistoryEntry)),
 *   thay dòng gọi tương ứng.
 */
public class HistoryView {
    private final SlangController controller = new SlangController();
    private final ObservableList<SearchHistoryEntry> items = FXCollections.observableArrayList();
    private ListView<SearchHistoryEntry> listView;

    public Scene getScene() {
        Label title = new Label("History");
        Button btnBack = new Button("← Back");
        Button btnClearAll = new Button("Clear All");

        listView = new ListView<>(items);
        listView.setPlaceholder(new Label("No history available"));
        listView.setCellFactory(lv -> new HistoryListCell());
        listView.setPrefHeight(420);

        btnBack.setOnAction(e -> ViewManager.getInstance().switchScene(new MainMenuView().getScene()));

        btnClearAll.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to clear all search history?", ButtonType.YES, ButtonType.NO);
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
        // controller returns newest-first; keep that order (or sort if needed)
        items.setAll(history);
    }

    // Custom ListCell: show query/type/results (no timestamp), remove button appears on hover
    private class HistoryListCell extends ListCell<SearchHistoryEntry> {
        private final HBox container = new HBox(8);
        private final Label lbl = new Label();
        private final Button btnRemove = new Button("Remove");
        private final Region spacer = new Region();

        HistoryListCell() {
            super();
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
                // find index in controller list (history stored newest-first)
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
                    // fallback: refresh to reflect current state
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
                // Display WITHOUT timestamp as requested
                String text = String.format("%s: \"%s\" -> %s", entry.getType(), entry.getQuery(), results);
                lbl.setText(text);
                setGraphic(container);
            }
        }
    }
}