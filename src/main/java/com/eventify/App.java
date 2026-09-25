package com.eventify;

import com.eventify.Models.User;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class App extends Application {

    private static Stage stage;

    private static final ExecutorService WORKER =
            Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(
                        runnable,
                        "eventify-background-worker"
                );
                thread.setDaemon(true);
                return thread;
            });

    private static String activeMainEvent = null;

    public static void setActiveMainEvent(String eventName) {
        activeMainEvent = eventName;
        Database.setDatabaseContext(eventName);
    }

    public static String getActiveMainEvent() {
        return activeMainEvent;
    }

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.setTitle("Eventify");

        showHome();
    }

    public static void selectMainEventAndLogin(String eventName, Parent currentRoot, Label statusLabel) {
        setActiveMainEvent(eventName);
        run(
                currentRoot,
                statusLabel,
                () -> {
                    Database.initialize();
                    return null;
                },
                ignored -> showLogin()
        );
    }

    private static void switchScene(
            Parent root,
            double defaultWidth,
            double defaultHeight,
            double minWidth,
            double minHeight,
            String title
    ) {
        stage.setTitle(title);
        stage.setMinWidth(minWidth);
        stage.setMinHeight(minHeight);

        Scene currentScene = stage.getScene();
        if (currentScene == null) {
            stage.setScene(new Scene(root, defaultWidth, defaultHeight));
            stage.centerOnScreen();
            stage.show();
        } else {
            boolean wasMaximized = stage.isMaximized();
            currentScene.setRoot(root);
            if (!wasMaximized) {
                stage.setWidth(defaultWidth);
                stage.setHeight(defaultHeight);
                stage.centerOnScreen();
            }
        }
    }

    public static void showHome() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    App.class.getResource("home.fxml")
            );

            Parent root = loader.load();

            switchScene(root, 900, 560, 680, 460, "Eventify — Select Event");
        } catch (IOException e) {
            showError(e);
        }
    }

    public static void showLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    App.class.getResource("auth.fxml")
            );

            Parent root = loader.load();

            String event = activeMainEvent != null ? activeMainEvent : "General";
            switchScene(root, 680, 680, 480, 420, "Eventify (" + event + ") — Portal Access");
        } catch (IOException e) {
            showError(e);
        }
    }

    public static void showMain(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    App.class.getResource("main.fxml")
            );

            Parent root = loader.load();

            MainController controller = loader.getController();

            String event = activeMainEvent != null ? activeMainEvent : "General";
            switchScene(root, 1180, 750, 720, 480, "Eventify (" + event + ") — " + user.name());

            controller.setUser(user);
        } catch (IOException e) {
            showError(e);
        }
    }

    public static Stage window() {
        return stage;
    }

    public static <T> void run(
            Parent root,
            Label status,
            Callable<T> work,
            Consumer<T> onSuccess
    ) {
        root.setDisable(true);
        status.setText("Working...");

        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };

        task.setOnSucceeded(event -> {
            root.setDisable(false);
            status.setText("Ready");

            try {
                onSuccess.accept(task.getValue());
            } catch (Exception e) {
                showError(e);
            }
        });

        task.setOnFailed(event -> {
            root.setDisable(false);
            status.setText("Operation failed");
            showError(task.getException());
        });

        task.setOnCancelled(event -> {
            root.setDisable(false);
            status.setText("Operation cancelled");
        });

        WORKER.submit(task);
    }

    public static void showError(Throwable error) {
        error.printStackTrace();

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(stage);
        alert.setTitle("Eventify");
        alert.setHeaderText("Could not complete the operation");
        alert.setContentText(
                Objects.requireNonNullElse(
                        error.getMessage(),
                        error.getClass().getSimpleName()
                )
        );

        alert.getDialogPane().setMinHeight(
                javafx.scene.layout.Region.USE_PREF_SIZE
        );

        alert.showAndWait();
    }

    @Override
    public void stop() {
        WORKER.shutdownNow();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
