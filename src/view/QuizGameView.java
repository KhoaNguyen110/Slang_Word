package src.view;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.util.Duration;
import src.model.*;
import java.util.*;

public class QuizGameView {
    private SlangDictionary dict = SlangDictionary.getInstance();
    private boolean typeA; // true = slang→definition, false = definition→slang
    private int questionCount = 0;
    private int correctCount = 0;
    private int timeLeft = 10;
    private Timeline timer;

    private Label lblQuestion;
    private Label lblTimer;
    private List<Button> answerButtons;
    private VBox root;

    public QuizGameView(boolean typeA) {
        this.typeA = typeA;
    }

    public Scene getScene() {
        lblQuestion = new Label();
        lblQuestion.setFont(new Font("Arial", 20));

        lblTimer = new Label("⏰ 10s");
        lblTimer.setFont(new Font("Arial", 16));

        // 4 nút đáp án
        answerButtons = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Button b = new Button();
            b.setMaxWidth(Double.MAX_VALUE);
            b.setStyle("-fx-font-size: 16px; -fx-padding: 10 20;");
            answerButtons.add(b);
        }

        VBox answersBox = new VBox(10);
        answersBox.getChildren().addAll(answerButtons);
        answersBox.setAlignment(Pos.CENTER);

        root = new VBox(20, lblQuestion, lblTimer, answersBox);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-padding: 40; -fx-background-color: #fafafa;");

        Scene scene = new Scene(root, 800, 600);

        loadNextQuestion();
        return scene;
    }

    private void loadNextQuestion() {
        if (questionCount >= 4) {
            showResult();
            return;
        }

        // Reset timer
        timeLeft = 10;
        lblTimer.setText("⏰ 10s");
        if (timer != null) timer.stop();
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateTimer()));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();

        questionCount++;

        // Random câu hỏi
        List<SlangWord> all = new ArrayList<>(dict.getAll().values());
        Collections.shuffle(all);
        System.out.println("Size: " + all.size());

        SlangWord correct = all.get(0);
        Set<String> options = new HashSet<>();
        options.add(typeA ? correct.getDefinitions().get(0) : correct.getWord());
        while (options.size() < 4) {
            SlangWord rand = all.get(new Random().nextInt(all.size()));
            options.add(typeA ? rand.getDefinitions().get(0) : rand.getWord());
        }

        List<String> opts = new ArrayList<>(options);
        Collections.shuffle(opts);

        lblQuestion.setText(typeA
                ? "What is the meaning of: " + correct.getWord()
                : "Which slang means: " + correct.getDefinitions().get(0));

        for (int i = 0; i < 4; i++) {
            String text = opts.get(i);
            Button btn = answerButtons.get(i);
            btn.setText(text);
            btn.setOnAction(e -> checkAnswer(text, correct));
        }
    }

    private void updateTimer() {
        timeLeft--;
        lblTimer.setText("⏰ " + timeLeft + "s");
        if (timeLeft <= 0) {
            timer.stop();
            showResult();
        }
    }

    private void checkAnswer(String chosen, SlangWord correct) {
        boolean isCorrect = (typeA && correct.getDefinitions().contains(chosen))
                         || (!typeA && correct.getWord().equals(chosen));

        if (isCorrect) correctCount++;
        loadNextQuestion();
    }

    private void showResult() {
        if (timer != null) timer.stop();
        String message = correctCount >= 3
                ? "🎉 Congratulations! You won with " + correctCount + "/4 correct!"
                : "😢 You lost! Score: " + correctCount + "/4";

        Label lblResult = new Label(message);
        lblResult.setFont(new Font("Arial", 22));

        Button btnHome = new Button("🏠 Back to Home");
        Button btnRetry = new Button("🔁 Try Again");

        btnHome.setOnAction(e -> ViewManager.getInstance().switchScene(new MainMenuView().getScene()));
        btnRetry.setOnAction(e -> ViewManager.getInstance().switchScene(new QuizMenuView().getScene()));

        VBox box = new VBox(20, lblResult, btnHome, btnRetry);
        box.setAlignment(Pos.CENTER);
        root.getChildren().setAll(box);
    }
}
