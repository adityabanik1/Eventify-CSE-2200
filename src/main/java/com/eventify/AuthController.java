package com.eventify;

import com.eventify.Models.User;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public class AuthController {

    @FXML
    private BorderPane root;

    @FXML
    private ImageView eventLogoView;

    @FXML
    private StackPane titleStack;

    @FXML
    private HBox effectBox;

    @FXML
    private Label mainEventTitle;

    @FXML
    private Label eventifySubLabel;

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

    private final List<Timeline> activeAnimations = new ArrayList<>();

    @FXML
    public void initialize() {
        stopAnimations();

        String event = App.getActiveMainEvent();
        if (event != null && !event.isBlank()) {
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
                        eventLogoView.setImage(new Image(url.toExternalForm(), 220, 95, true, true));
                    }
                }
            }

            switch (event.toLowerCase()) {
                case "ignition" -> setupIgnitionRacingVibe();
                case "bitfest" -> setupBitFestTechVibe();
                case "calibration" -> setupCalibrationRoboticVibe();
                default -> setupDefaultVibe(event);
            }
        } else {
            setupDefaultVibe("Eventify");
        }
    }

    private void stopAnimations() {
        for (Timeline t : activeAnimations) {
            if (t != null) {
                t.stop();
            }
        }
        activeAnimations.clear();
        if (effectBox != null) {
            effectBox.getChildren().clear();
        }
    }

    private void setupIgnitionRacingVibe() {
        if (mainEventTitle != null) {
            mainEventTitle.setText("IGNITION 2026");
            mainEventTitle.setStyle(
                    "-fx-font-family: 'Orbitron'; " +
                    "-fx-font-size: 36px; " +
                    "-fx-font-weight: bold; " +
                    "-fx-font-style: italic; " +
                    "-fx-text-fill: linear-gradient(to right, #ea580c 0%, #f97316 45%, #fde047 100%);"
            );

            DropShadow flare = new DropShadow();
            flare.setColor(Color.web("#ea580c"));
            flare.setRadius(16);
            flare.setSpread(0.3);
            mainEventTitle.setEffect(flare);

            if (effectBox != null) {
                effectBox.getChildren().clear();
                Region flareStreak = new Region();
                flareStreak.setPrefHeight(3);
                flareStreak.setPrefWidth(280);
                flareStreak.setStyle(
                        "-fx-background-color: linear-gradient(to right, transparent, #ea580c, #f59e0b, transparent); " +
                        "-fx-effect: dropshadow(gaussian, #ff4500, 12, 0, 0, 0);"
                );
                effectBox.getChildren().add(flareStreak);

                Timeline flareTimeline = new Timeline(
                        new KeyFrame(Duration.ZERO,
                                new KeyValue(flare.radiusProperty(), 14),
                                new KeyValue(flare.spreadProperty(), 0.25),
                                new KeyValue(mainEventTitle.scaleXProperty(), 1.0),
                                new KeyValue(mainEventTitle.scaleYProperty(), 1.0),
                                new KeyValue(flareStreak.scaleXProperty(), 0.75),
                                new KeyValue(flareStreak.opacityProperty(), 0.5)
                        ),
                        new KeyFrame(Duration.millis(550),
                                new KeyValue(flare.radiusProperty(), 34),
                                new KeyValue(flare.spreadProperty(), 0.65),
                                new KeyValue(mainEventTitle.scaleXProperty(), 1.04),
                                new KeyValue(mainEventTitle.scaleYProperty(), 1.04),
                                new KeyValue(flareStreak.scaleXProperty(), 1.3),
                                new KeyValue(flareStreak.opacityProperty(), 1.0)
                        )
                );
                flareTimeline.setAutoReverse(true);
                flareTimeline.setCycleCount(Timeline.INDEFINITE);
                flareTimeline.play();
                activeAnimations.add(flareTimeline);
            }
        }

        if (eventifySubLabel != null) {
            eventifySubLabel.setText("EVENTIFY • KUET MECHANICAL ENGINEERING FEST");
            eventifySubLabel.setStyle("-fx-text-fill: #fb923c; -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 2.5px;");
        }
    }

    private void setupBitFestTechVibe() {
        if (mainEventTitle != null) {
            mainEventTitle.setText("BITFEST 2025");
            mainEventTitle.setStyle(
                    "-fx-font-family: 'Orbitron'; " +
                    "-fx-font-size: 36px; " +
                    "-fx-font-weight: bold; " +
                    "-fx-text-fill: linear-gradient(to right, #00f0ff 0%, #38bdf8 55%, #818cf8 100%);"
            );

            DropShadow cyberGlow = new DropShadow();
            cyberGlow.setColor(Color.web("#00f0ff"));
            cyberGlow.setRadius(18);
            cyberGlow.setSpread(0.35);
            mainEventTitle.setEffect(cyberGlow);

            if (effectBox != null) {
                effectBox.getChildren().clear();
                effectBox.setSpacing(14);
                List<Region> rays = new ArrayList<>();
                for (int i = 0; i < 9; i++) {
                    Region ray = new Region();
                    ray.setPrefWidth(2);
                    ray.setPrefHeight(i % 2 == 0 ? 52 : 38);
                    ray.setStyle(
                            "-fx-background-color: linear-gradient(to bottom, transparent, rgba(0, 240, 255, 0.85), transparent); " +
                            "-fx-effect: dropshadow(gaussian, #00f0ff, 9, 0, 0, 0);"
                    );
                    rays.add(ray);
                    effectBox.getChildren().add(ray);
                }

                Timeline glowTimeline = new Timeline(
                        new KeyFrame(Duration.ZERO,
                                new KeyValue(cyberGlow.radiusProperty(), 15),
                                new KeyValue(cyberGlow.spreadProperty(), 0.28)
                        ),
                        new KeyFrame(Duration.millis(750),
                                new KeyValue(cyberGlow.radiusProperty(), 32),
                                new KeyValue(cyberGlow.spreadProperty(), 0.6)
                        )
                );
                glowTimeline.setAutoReverse(true);
                glowTimeline.setCycleCount(Timeline.INDEFINITE);
                glowTimeline.play();
                activeAnimations.add(glowTimeline);

                for (int i = 0; i < rays.size(); i++) {
                    Region r = rays.get(i);
                    Timeline rayTimeline = new Timeline(
                            new KeyFrame(Duration.ZERO,
                                    new KeyValue(r.scaleYProperty(), 0.6),
                                    new KeyValue(r.opacityProperty(), 0.3)
                            ),
                            new KeyFrame(Duration.millis(350 + (i * 100)),
                                    new KeyValue(r.scaleYProperty(), 1.35),
                                    new KeyValue(r.opacityProperty(), 1.0)
                            )
                    );
                    rayTimeline.setAutoReverse(true);
                    rayTimeline.setCycleCount(Timeline.INDEFINITE);
                    rayTimeline.play();
                    activeAnimations.add(rayTimeline);
                }
            }
        }

        if (eventifySubLabel != null) {
            eventifySubLabel.setText("EVENTIFY • KUET CSE NATIONAL TECH FESTIVAL");
            eventifySubLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 2.5px;");
        }
    }

    private void setupCalibrationRoboticVibe() {
        if (mainEventTitle != null) {
            mainEventTitle.setText("[ CALIBRATION 2.0 ]");
            mainEventTitle.setStyle(
                    "-fx-font-family: 'Orbitron'; " +
                    "-fx-font-size: 33px; " +
                    "-fx-font-weight: bold; " +
                    "-fx-text-fill: linear-gradient(to right, #10b981 0%, #34d399 50%, #6ee7b7 100%);"
            );

            DropShadow robotGlow = new DropShadow();
            robotGlow.setColor(Color.web("#10b981"));
            robotGlow.setRadius(16);
            robotGlow.setSpread(0.3);
            mainEventTitle.setEffect(robotGlow);

            if (effectBox != null) {
                effectBox.getChildren().clear();
                effectBox.setSpacing(120);

                Region hudLeft = new Region();
                hudLeft.setPrefSize(16, 16);
                hudLeft.setStyle("-fx-border-color: #34d399 transparent transparent #34d399; -fx-border-width: 2.5px; -fx-effect: dropshadow(gaussian, #10b981, 8, 0, 0, 0);");

                Region hudRight = new Region();
                hudRight.setPrefSize(16, 16);
                hudRight.setStyle("-fx-border-color: transparent #34d399 #34d399 transparent; -fx-border-width: 2.5px; -fx-effect: dropshadow(gaussian, #10b981, 8, 0, 0, 0);");

                effectBox.getChildren().addAll(hudLeft, hudRight);

                Timeline robotTimeline = new Timeline(
                        new KeyFrame(Duration.ZERO,
                                new KeyValue(robotGlow.radiusProperty(), 12),
                                new KeyValue(hudLeft.translateYProperty(), -3),
                                new KeyValue(hudRight.translateYProperty(), 3),
                                new KeyValue(mainEventTitle.opacityProperty(), 0.9)
                        ),
                        new KeyFrame(Duration.millis(480),
                                new KeyValue(robotGlow.radiusProperty(), 28),
                                new KeyValue(hudLeft.translateYProperty(), 3),
                                new KeyValue(hudRight.translateYProperty(), -3),
                                new KeyValue(mainEventTitle.opacityProperty(), 1.0)
                        )
                );
                robotTimeline.setAutoReverse(true);
                robotTimeline.setCycleCount(Timeline.INDEFINITE);
                robotTimeline.play();
                activeAnimations.add(robotTimeline);
            }
        }

        if (eventifySubLabel != null) {
            eventifySubLabel.setText("EVENTIFY • KUET MECHATRONICS & ROBOTICS FEST");
            eventifySubLabel.setStyle("-fx-text-fill: #34d399; -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 2.5px;");
        }
    }

    private void setupDefaultVibe(String eventName) {
        if (mainEventTitle != null) {
            mainEventTitle.setText(eventName.toUpperCase());
            mainEventTitle.setStyle("-fx-font-family: 'Orbitron'; -fx-font-size: 34px; -fx-font-weight: bold; -fx-text-fill: #6366f1;");
        }
        if (eventifySubLabel != null) {
            eventifySubLabel.setText("EVENTIFY PLATFORM");
            eventifySubLabel.setStyle("-fx-text-fill: #818cf8; -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 2.5px;");
        }
    }

    @FXML
    public void onBackToHome() {
        stopAnimations();
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
                user -> {
                    stopAnimations();
                    App.showMain(user);
                }
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
                user -> {
                    stopAnimations();
                    App.showMain(user);
                }
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
