package com.eventify;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class HomeController extends BaseController {

    @Override
    public void setupController() {
        // Implementation of BaseController's abstract method
        if (apiKeyField != null) {
            apiKeyField.setText(AiChatService.getApiKey());
        }
        initChat();
    }

    @FXML
    private VBox root;

    @FXML
    private Label statusLabel;

    @FXML
    private TextField searchField;

    @FXML
    private VBox bitfestCard;

    @FXML
    private VBox calibrationCard;

    @FXML
    private VBox ignitionCard;

    // Chat Overlay UI Elements on Home Page
    @FXML
    private Region chatBackdrop;

    @FXML
    private VBox chatOverlay;

    @FXML
    private Button floatingAiBtn;

    @FXML
    private Label fabIcon;

    @FXML
    private Button homeAiButton;

    @FXML
    private PasswordField apiKeyField;

    @FXML
    private ScrollPane chatScrollPane;

    @FXML
    private VBox chatMessagesBox;

    @FXML
    private TextField chatInputField;

    @FXML
    private Button chatSendButton;

    @FXML
    private Label chatStatusLabel;

    @FXML
    public void onSelectBitFest() {
        App.selectMainEventAndLogin("BitFest", root, statusLabel);
    }

    @FXML
    public void onSelectCalibration() {
        App.selectMainEventAndLogin("Calibration", root, statusLabel);
    }

    @FXML
    public void onSelectIgnition() {
        App.selectMainEventAndLogin("Ignition", root, statusLabel);
    }

    @FXML
    public void initialize() {
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                String query = newValue.toLowerCase().trim();

                boolean showBitFest = query.isEmpty() || "bitfest".contains(query) || "kuet cse".contains(query);
                boolean showCalibration = query.isEmpty() || "calibration".contains(query) || "mechatronics".contains(query);
                boolean showIgnition = query.isEmpty() || "ignition".contains(query) || "mechanical".contains(query);

                bitfestCard.setVisible(showBitFest);
                bitfestCard.setManaged(showBitFest);

                calibrationCard.setVisible(showCalibration);
                calibrationCard.setManaged(showCalibration);

                ignitionCard.setVisible(showIgnition);
                ignitionCard.setManaged(showIgnition);
            });
        }

        setupController();
    }

    @FXML
    private void onToggleChat() {
        if (chatOverlay == null) return;
        boolean show = !chatOverlay.isVisible();
        chatOverlay.setVisible(show);
        chatOverlay.setManaged(show);
        if (fabIcon != null) {
            fabIcon.setText(show ? "✕" : "🤖");
        }
        if (show) {
            if (apiKeyField != null) {
                apiKeyField.setText(AiChatService.getApiKey());
            }
            if (chatMessagesBox != null && chatMessagesBox.getChildren().isEmpty()) {
                initChat();
            }
            if (chatInputField != null) {
                javafx.application.Platform.runLater(() -> chatInputField.requestFocus());
            }
        }
    }

    @FXML
    private void onCloseChat() {
        if (chatOverlay == null) return;
        chatOverlay.setVisible(false);
        chatOverlay.setManaged(false);
        if (fabIcon != null) {
            fabIcon.setText("🤖");
        }
    }

    private void initChat() {
        if (chatMessagesBox == null) return;
        chatMessagesBox.getChildren().clear();
        appendBotMessage(
                "⚡ **Welcome to Eventify AI!**\n\n"
                        + "I am your intelligent assistant exclusively dedicated to the **Eventify** festival management platform.\n"
                        + "I can help you explore **BitFest**, **Ignition**, and **Calibration**, explain login and registration requirements, or troubleshoot system issues.\n\n"
                        + "Click any quick topic above or type your question below."
        );
    }

    @FXML
    private void onClearChat() {
        AiChatService.clearHistory();
        initChat();
        if (chatStatusLabel != null) {
            chatStatusLabel.setText("Chat cleared");
        }
    }

    @FXML
    private void onSendChatMessage() {
        if (chatInputField == null) return;
        String text = chatInputField.getText().trim();
        if (text.isBlank()) return;
        chatInputField.clear();
        processUserMessage(text);
    }

    @FXML
    private void onFaqLogin() {
        processFaq("Facing problems with login or account creation?", "LOGIN");
    }

    @FXML
    private void onFaqCreateEvent() {
        processFaq("Facing problems creating or editing an event?", "EVENT_CREATION");
    }

    @FXML
    private void onFaqLeaderboard() {
        processFaq("How is the event leaderboard score calculated?", "LEADERBOARD");
    }

    @FXML
    private void onFaqSchedule() {
        processFaq("What are the rules and constraints for event schedule sessions?", "SCHEDULE");
    }

    private void processFaq(String question, String faqKey) {
        appendUserMessage(question);
        sendToAi(question);
    }

    private void processUserMessage(String userText) {
        appendUserMessage(userText);
        sendToAi(userText);
    }

    private void sendToAi(String message) {
        if (chatStatusLabel != null) {
            chatStatusLabel.setText("🤖 Eventify AI is thinking...");
        }
        if (chatSendButton != null) {
            chatSendButton.setDisable(true);
        }

        App.runAsync(
                () -> AiChatService.sendMessage(message, "General / Pre-Login", "Festival Portal Selection", "Visitor / Potential Participant"),
                reply -> {
                    if (chatSendButton != null) chatSendButton.setDisable(false);
                    if (chatStatusLabel != null) chatStatusLabel.setText("Ready");
                    appendBotMessage(reply);
                },
                error -> {
                    if (chatSendButton != null) chatSendButton.setDisable(false);
                    if (chatStatusLabel != null) chatStatusLabel.setText("Ready");
                    appendBotMessage(AiChatService.getQuickFaqAnswer("GENERAL"));
                }
        );
    }

    private void appendUserMessage(String text) {
        if (chatMessagesBox == null) return;
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_RIGHT);

        VBox bubble = new VBox(4);
        bubble.getStyleClass().add("user-bubble");
        bubble.setMaxWidth(460);

        Label senderLabel = new Label("👤 You (Visitor)");
        senderLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #c7d2fe;");

        Label contentLabel = new Label(text);
        contentLabel.setWrapText(true);
        contentLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 13px;");

        bubble.getChildren().addAll(senderLabel, contentLabel);
        row.getChildren().add(bubble);

        chatMessagesBox.getChildren().add(row);
        scrollToBottom();
    }

    private void appendBotMessage(String text) {
        if (chatMessagesBox == null) return;
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);

        VBox bubble = new VBox(6);
        bubble.getStyleClass().add("bot-bubble");
        bubble.setMaxWidth(480);

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label avatar = new Label("🤖");
        avatar.setStyle("-fx-font-size: 15px;");
        Label senderLabel = new Label("Eventify AI");
        senderLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");
        Label scopeBadge = new Label("EXCLUSIVE");
        scopeBadge.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-background-color: rgba(6, 182, 212, 0.2); -fx-text-fill: #22d3ee; -fx-padding: 2px 6px; -fx-background-radius: 4px;");
        header.getChildren().addAll(avatar, senderLabel, scopeBadge);

        Label contentLabel = new Label(text);
        contentLabel.setWrapText(true);
        contentLabel.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 13px; -fx-line-spacing: 2px;");

        bubble.getChildren().addAll(header, contentLabel);
        row.getChildren().add(bubble);

        chatMessagesBox.getChildren().add(row);
        scrollToBottom();
    }

    private void scrollToBottom() {
        if (chatScrollPane != null) {
            javafx.application.Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
        }
    }
}
