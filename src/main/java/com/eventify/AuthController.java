package com.eventify;

import com.eventify.Models.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class AuthController {

    @FXML
    private BorderPane root;

    @FXML
    private ImageView eventLogoView;

    @FXML
    private Label eventBadge;

    @FXML
    private Label subtitleLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private VBox participantBox;

    @FXML
    private TextField adminEmail;

    @FXML
    private PasswordField adminPassword;

    @FXML
    private TabPane participantTabPane;

    @FXML
    private Tab signUpTab;

    @FXML
    private TextField participantLoginEmail;

    @FXML
    private PasswordField participantLoginPassword;

    @FXML
    private TextField registerName;

    @FXML
    private TextField registerEmail;

    @FXML
    private PasswordField registerPassword;

    @FXML
    private PasswordField confirmPassword;

    @FXML
    public void initialize() {
        String event = App.getActiveMainEvent();
        if (event != null && !event.isBlank()) {
            eventBadge.setText(event);
            subtitleLabel.setText("Selected Event: " + event);

            String themeClass = switch (event.toLowerCase()) {
                case "bitfest" -> "theme-bitfest";
                case "calibration" -> "theme-calibration";
                case "ignition" -> "theme-ignition";
                default -> "theme-default";
            };
            if (root != null) {
                root.getStyleClass().removeAll("theme-bitfest", "theme-calibration", "theme-ignition", "theme-default");
                root.getStyleClass().add(themeClass);
            }

            if (eventLogoView != null) {
                String logoFile = switch (event.toLowerCase()) {
                    case "bitfest" -> "images/bitfest.png";
                    case "calibration" -> "images/calibration.png";
                    case "ignition" -> "images/ignition.png";
                    default -> null;
                };
                if (logoFile != null) {
                    var url = App.class.getResource(logoFile);
                    if (url != null) {
                        eventLogoView.setImage(new Image(url.toExternalForm(), 200, 90, true, true));
                    }
                }
            }
        } else {
            eventBadge.setText("General");
            subtitleLabel.setText("Event Management & Planning");
        }
    }

    @FXML
    public void onBackToHome() {
        App.showHome();
    }

    @FXML
    private void onAdminLogin() {
        String email = adminEmail.getText();
        String password = adminPassword.getText();

        App.run(
                root,
                statusLabel,
                () -> {
                    User user = Database.login(email, password);
                    if (!user.isAdmin()) {
                        throw new SecurityException(
                                "This account is a participant account. Please sign in via the Participant Portal."
                        );
                    }
                    return user;
                },
                App::showMain
        );
    }

    @FXML
    private void onParticipantLogin() {
        String email = participantLoginEmail.getText();
        String password = participantLoginPassword.getText();

        App.run(
                root,
                statusLabel,
                () -> Database.login(email, password),
                App::showMain
        );
    }

    @FXML
    private void onRegister() {
        String name = registerName.getText();
        String email = registerEmail.getText();
        String password = registerPassword.getText();
        String confirmation = confirmPassword.getText();

        if (!password.equals(confirmation)) {
            Forms.info("The passwords do not match.");
            return;
        }

        App.run(
                root,
                statusLabel,
                () -> {
                    Database.createParticipant(name, email, password);
                    return null;
                },
                ignored -> {
                    registerName.clear();
                    registerEmail.clear();
                    registerPassword.clear();
                    confirmPassword.clear();

                    participantLoginEmail.setText(email);
                    participantLoginPassword.clear();

                    if (participantTabPane != null && !participantTabPane.getTabs().isEmpty()) {
                        participantTabPane.getSelectionModel().select(0);
                    }

                    statusLabel.setText(
                            "Account created. Open Sign in to continue."
                    );

                    Forms.info(
                            "Participant account created successfully!\n"
                                    + "You can now log in with your email and password."
                    );
                }
        );
    }
}
