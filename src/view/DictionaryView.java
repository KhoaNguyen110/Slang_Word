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
import src.controller.SlangController.AddOption;
import src.controller.SlangController.AddResult;
import src.model.SlangWord;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SearchView (improved performance):
 * - Use ListView<SlangWord> with a custom ListCell (virtualized).
 * - Edit/Delete buttons are only shown when mouse hovers the cell (btn.setVisible / setManaged).
 * - Interacts with SlangController (MVC).
 */
public class DictionaryView {
    private final SlangController controller = SlangController.getInstance();
    private final ObservableList<SlangWord> items = FXCollections.observableArrayList();
    private ListView<SlangWord> listView;

    public DictionaryView() {
        // backup snapshot once loaded to allow reset
        controller.backupOriginal();
    }

    public Scene getScene() {
        Label title = new Label("🔍 Search Slang Word");
        TextField input = new TextField();
        Button btnSearch = new Button("Search");
        Button btnShow = new Button("Show all Slang Words");
        Button btnBack = new Button("← Back");
        Button btnAdd = new Button("Add");
        Button btnRandom = new Button("Random Slang");
        Button btnReset = new Button("Reset to Original");

        listView = new ListView<>(items);
        listView.setCellFactory(lv -> new SlangListCell());
        listView.setPrefHeight(320);

        // Handlers
        btnSearch.setOnAction(e -> {
            Alert searcAlert = new Alert(Alert.AlertType.INFORMATION);
            searcAlert.setHeaderText("NOT FOUND");
            searcAlert.setTitle("Search Result");
            String q = input.getText().trim();
            if (q.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Please enter search keyword.");
                return;
            }
            // Build result list: word match first, then definition matches (avoid duplicates)
            Set<String> added = new HashSet<>();
            List<SlangWord> results = new ArrayList<>();
            SlangWord sw = controller.searchByWord(q);
            if (sw != null) {
                results.add(sw);
                added.add(sw.getWord());
            }
            List<SlangWord> byDef = controller.searchByDefinition(q);
            for (SlangWord s : byDef) {
                if (!added.contains(s.getWord())) {
                    results.add(s);
                    added.add(s.getWord());
                }
            }

            if (results.isEmpty()) {
               searcAlert.setContentText("No results found for \"" + q + "\".");
               searcAlert.showAndWait();
               return;
            }
            items.setAll(results);
        });

        btnShow.setOnAction(e -> refreshList());

        btnBack.setOnAction(e -> ViewManager.getInstance().switchScene(new MainMenuView().getScene()));

        btnAdd.setOnAction(e -> openAddDialog());

        btnRandom.setOnAction(e -> {
            Alert randomeAlert = new Alert(Alert.AlertType.INFORMATION);
            randomeAlert.setHeaderText("ON THIS DAY SLANG WORD");
            randomeAlert.setTitle("Random slang word");
            randomeAlert.setContentText("ON THIS DAY SLANG WORD");
            SlangWord r = controller.getRandomSlang();
            if (r == null) {
                randomeAlert.setContentText("ON THIS DAY SLANG WORD");
                return;
            }
            randomeAlert.setContentText(r.toString());
            randomeAlert.showAndWait();
            // Refresh and scroll to the random item
            refreshList();
            Platform.runLater(() -> {
                int idx = findIndexByWord(r.getWord());
                if (idx >= 0) {
                    listView.scrollTo(idx);
                    listView.getSelectionModel().select(idx);
                }
            });
        });

        btnReset.setOnAction(e -> {
            Alert resetAlert = new Alert(Alert.AlertType.INFORMATION);
            resetAlert.setHeaderText("RESET SUCCESSFUL");
            resetAlert.setTitle("Reset to Original");
            controller.resetToOriginal();
            refreshList();
            resetAlert.setContentText("Reset to original snapshot.");
            resetAlert.showAndWait();
        });

        HBox topRow = new HBox(8, title, input, btnSearch, btnAdd, btnRandom, btnReset);
        topRow.setPadding(new Insets(8));

        HBox bottomRow = new HBox(8, btnShow, btnBack);
        bottomRow.setPadding(new Insets(8));

        VBox root = new VBox(10, topRow, listView, bottomRow);
        root.setPadding(new Insets(12));
        root.setPrefSize(700, 520);

        // Init list
        refreshList();

        return new Scene(root);
    }

    // Refresh list from controller
    private void refreshList() {
        Map<String, SlangWord> all = controller.getAllSlang();
        List<SlangWord> sorted = all.values().stream()
                .sorted(Comparator.comparing(SlangWord::getWord, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
        items.setAll(sorted);
    }

    private int findIndexByWord(String word) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getWord().equals(word)) return i;
        }
        return -1;
    }

    // Add dialog uses controller.addSlang
    private void openAddDialog() {
        TextField fldWord = new TextField();
        TextArea fldDefs = new TextArea();
        fldDefs.setPromptText("Separate multiple definitions with '|' or newline");
        fldDefs.setPrefRowCount(4);

        VBox content = new VBox(8, new Label("Word:"), fldWord, new Label("Definitions:"), fldDefs);
        content.setPadding(new Insets(8));

        ButtonType ok = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.getDialogPane().getButtonTypes().addAll(ok, cancel);
        dialog.getDialogPane().setContent(content);
        dialog.setTitle("Add Slang");

        dialog.showAndWait().ifPresent(res -> {
            if (res == ok) {
                String word = fldWord.getText().trim();
                String defsRaw = fldDefs.getText().trim();
                if (word.isEmpty() || defsRaw.isEmpty()) {
                    showAlert(Alert.AlertType.WARNING, "Word and definition cannot be empty.");
                    return;
                }

                AddResult r = controller.addSlang(word, defsRaw);
                if (r == AddResult.ADDED) {
                    showAlert(Alert.AlertType.INFORMATION, "Added successfully.");
                    refreshList();
                } else if (r == AddResult.EXISTS) {
                    // ask Overwrite / Duplicate / Cancel
                    Alert a = new Alert(Alert.AlertType.CONFIRMATION);
                    a.setTitle("Duplicate detected");
                    a.setHeaderText("Word \"" + word + "\" already exists.");
                    ButtonType btOverwrite = new ButtonType("Overwrite");
                    ButtonType btDuplicate = new ButtonType("Duplicate (append)");
                    ButtonType btCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
                    a.getButtonTypes().setAll(btOverwrite, btDuplicate, btCancel);
                    a.showAndWait().ifPresent(choice -> {
                        if (choice == btOverwrite) {
                            AddResult res2 = controller.addSlang(word, defsRaw, AddOption.OVERWRITE);
                            if (res2 == AddResult.OVERWRITTEN) showAlert(Alert.AlertType.INFORMATION, "Updated successfully.");
                            else showAlert(Alert.AlertType.ERROR, "Overwrite failed.");
                        } else if (choice == btDuplicate) {
                            AddResult res2 = controller.addSlang(word, defsRaw, AddOption.DUPLICATE);
                            if (res2 == AddResult.DUPLICATED) showAlert(Alert.AlertType.INFORMATION, "Added duplicate definition.");
                            else showAlert(Alert.AlertType.ERROR, "Duplicate append failed.");
                        }
                        refreshList();
                    });
                } else {
                    showAlert(Alert.AlertType.ERROR, "Add failed.");
                }
            }
        });
    }

    // Edit dialog calling controller.editSlang
    private void openEditDialog(SlangWord sw) {
        String oldWord = sw.getWord();
        TextField fldWord = new TextField(sw.getWord());
        String currentDefs = String.join(" | ", sw.getDefinitions());
        TextArea fldDefs = new TextArea(currentDefs);
        fldDefs.setPrefRowCount(4);

        VBox content = new VBox(8, new Label("Word:"), fldWord, new Label("Definitions (separate by '|' or newline):"), fldDefs);
        content.setPadding(new Insets(8));

        ButtonType ok = new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.getDialogPane().getButtonTypes().addAll(ok, cancel);
        dialog.getDialogPane().setContent(content);
        dialog.setTitle("Edit Slang");

        dialog.showAndWait().ifPresent(res -> {
            if (res == ok) {
                String newWord = fldWord.getText().trim();
                String defsRaw = fldDefs.getText().trim();
                if (newWord.isEmpty() || defsRaw.isEmpty()) {
                    showAlert(Alert.AlertType.WARNING, "Word and definition cannot be empty.");
                    return;
                }
                boolean okEdit = controller.editSlang(oldWord, newWord, defsRaw);
                if (okEdit) {
                    showAlert(Alert.AlertType.INFORMATION, "Update successful.");
                    refreshList();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Update failed.");
                }
            }
        });
    }

    // Custom ListCell: builds HBox with label + buttons; buttons are shown only on hover.
    private class SlangListCell extends ListCell<SlangWord> {
        private final HBox container = new HBox(8);
        private final Label lbl = new Label();
        private final Button btnEdit = new Button("Edit");
        private final Button btnDelete = new Button("Delete");
        private final Region spacer = new Region();

        SlangListCell() {
            super();
            lbl.setWrapText(true);
            HBox.setHgrow(lbl, Priority.ALWAYS);
            HBox.setHgrow(spacer, Priority.ALWAYS);

            // Initially hide buttons (not visible and not managed to avoid layout cost)
            btnEdit.setVisible(false);
            btnEdit.setManaged(false);
            btnDelete.setVisible(false);
            btnDelete.setManaged(false);

            container.setPadding(new Insets(6));
            container.getChildren().addAll(lbl, spacer, btnEdit, btnDelete);

            // Show buttons on hover of the cell (mouse enter/exit)
            container.addEventFilter(MouseEvent.MOUSE_ENTERED, ev -> {
                btnEdit.setVisible(true);
                btnEdit.setManaged(true);
                btnDelete.setVisible(true);
                btnDelete.setManaged(true);
            });
            container.addEventFilter(MouseEvent.MOUSE_EXITED, ev -> {
                btnEdit.setVisible(false);
                btnEdit.setManaged(false);
                btnDelete.setVisible(false);
                btnDelete.setManaged(false);
            });

            // Button actions: use controller, then refresh list
            btnEdit.setOnAction(e -> {
                SlangWord sw = getItem();
                if (sw != null) openEditDialog(sw);
            });

            btnDelete.setOnAction(e -> {
                SlangWord sw = getItem();
                if (sw == null) return;
                Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete \"" + sw.getWord() + "\"?", ButtonType.YES, ButtonType.NO);
                a.showAndWait().ifPresent(bt -> {
                    if (bt == ButtonType.YES) {
                        boolean deleted = controller.deleteSlang(sw.getWord());
                        if (deleted) {
                            refreshList();
                            showAlert(Alert.AlertType.INFORMATION, "Deleted.");
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Delete failed.");
                        }
                    }
                });
            });
        }

        @Override
        protected void updateItem(SlangWord sw, boolean empty) {
            super.updateItem(sw, empty);
            if (empty || sw == null) {
                setGraphic(null);
                setText(null);
            } else {
                lbl.setText(sw.toString());
                setGraphic(container);
            }
        }
    }

    // Helpers
    private void showAlert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.showAndWait();
    }
}