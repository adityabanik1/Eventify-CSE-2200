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

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.setTitle("Eventify");

        Label loading = new Label("Preparing Eventify...");
        StackPane root = new StackPane(loading);

        stage.setScene(new Scene(root, 520, 420));
        stage.show();

        run(
                root,
                loading,
                () -> {
                    Database.initialize();
                    return null;
                },
                ignored -> showLogin()
        );
    }

    public static void showLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    App.class.getResource("auth.fxml")
            );

            Parent root = loader.load();

            stage.setMinWidth(480);
            stage.setMinHeight(570);
            stage.setTitle("Eventify — Sign in");
            stage.setScene(new Scene(root, 520, 600));
            stage.centerOnScreen();
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

            stage.setMinWidth(1000);
            stage.setMinHeight(680);
            stage.setTitle("Eventify — " + user.name());
            stage.setScene(new Scene(root, 1200, 780));
            stage.centerOnScreen();

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
