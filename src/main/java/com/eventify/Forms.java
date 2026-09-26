package com.eventify;

import com.eventify.Models.*;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.time.LocalDate;
import java.util.List;

public final class Forms {

    private Forms() {
    }

    private static boolean show(
            String title,
            String[] labels,
            Control... controls
    ) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(App.window());
        dialog.setTitle(title);
        dialog.setHeaderText(title);

        dialog.getDialogPane().getButtonTypes().addAll(
                ButtonType.OK,
                ButtonType.CANCEL
        );

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);
        grid.setStyle("-fx-padding: 20;");
        grid.setPrefWidth(540);

        // Explicit column constraints prevent labels from collapsing into "..."
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(110);
        col1.setPrefWidth(125);
        col1.setHgrow(Priority.NEVER);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(col1, col2);

        for (int i = 0; i < controls.length; i++) {
            Label label = new Label(labels[i]);
            label.setMinWidth(Region.USE_PREF_SIZE); // Never truncate label text
            label.setStyle("-fx-font-weight: bold;");

            Control control = controls[i];
            control.setMaxWidth(Double.MAX_VALUE);

            grid.add(label, 0, i);
            grid.add(control, 1, i);
            GridPane.setHgrow(control, Priority.ALWAYS);
        }

        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().getStylesheets().add(
                Forms.class.getResource("style.css").toExternalForm()
        );

        return dialog.showAndWait().orElse(ButtonType.CANCEL)
                == ButtonType.OK;
    }

    public static EventDraft event(Event existing) {
        TextField title = new TextField(
                existing == null ? "" : existing.title()
        );
        title.setPromptText("e.g. Campus Tech Fest");

        TextArea description = new TextArea(
                existing == null ? "" : existing.description()
        );
        description.setPromptText("Enter event description, agenda or notes...");
        description.setPrefRowCount(3);
        description.setWrapText(true);

        TextField venue = new TextField(
                existing == null ? "" : existing.venue()
        );
        venue.setPromptText("e.g. Main Auditorium / Room 401");

        DatePicker date = new DatePicker(
                existing == null
                        ? LocalDate.now().plusDays(1)
                        : existing.date()
        );
        date.setEditable(false);

        TextField time = new TextField(
                existing == null ? "09:00" : existing.time()
        );
        time.setPromptText("HH:mm (e.g. 09:30 or 14:00)");

        Spinner<Integer> capacity = new Spinner<>(
                1,
                100_000,
                existing == null ? 100 : existing.capacity()
        );
        capacity.setEditable(true);

        boolean accepted = show(
                existing == null ? "Create event" : "Edit event",
                new String[]{
                        "Title",
                        "Description",
                        "Venue",
                        "Date",
                        "Start time",
                        "Capacity"
                },
                title,
                description,
                venue,
                date,
                time,
                capacity
        );

        if (!accepted) {
            return null;
        }

        return new EventDraft(
                title.getText(),
                description.getText(),
                venue.getText(),
                date.getValue(),
                time.getText(),
                capacity.getValue()
        );
    }

    public static SlotDraft slot(Slot existing, Event event) {
        TextField title = new TextField(
                existing == null ? "" : existing.title()
        );
        title.setPromptText("e.g. Keynote Presentation");

        TextField start = new TextField(
                existing == null ? event.time() : existing.start()
        );
        start.setPromptText("HH:mm (e.g. 09:30)");

        TextField end = new TextField(
                existing == null ? "" : existing.end()
        );
        end.setPromptText("HH:mm (later than start, e.g. 11:00)");

        TextField speaker = new TextField(
                existing == null ? "" : existing.speaker()
        );
        speaker.setPromptText("e.g. Guest Speaker / Organizing Team");

        boolean accepted = show(
                existing == null ? "Add schedule item" : "Edit schedule item",
                new String[]{
                        "Session title",
                        "Start time",
                        "End time",
                        "Speaker / host"
                },
                title,
                start,
                end,
                speaker
        );

        if (!accepted) {
            return null;
        }

        return new SlotDraft(
                title.getText(),
                start.getText(),
                end.getText(),
                speaker.getText()
        );
    }

    public static JobDraft job(
            Job existing,
            List<Registration> registrations,
            Event event
    ) {
        TextField title = new TextField(
                existing == null ? "" : existing.title()
        );
        title.setPromptText("e.g. Manage registration desk");

        ComboBox<Registration> assignee = new ComboBox<>(
                FXCollections.observableArrayList(registrations)
        );

        if (existing == null) {
            assignee.getSelectionModel().selectFirst();
        } else {
            registrations.stream()
                    .filter(r -> r.userId() == existing.assigneeId())
                    .findFirst()
                    .ifPresent(assignee::setValue);
        }

        DatePicker due = new DatePicker(
                existing == null ? event.date() : existing.due()
        );
        due.setEditable(false);

        Spinner<Integer> points = new Spinner<>(
                1,
                1000,
                existing == null ? 20 : existing.points()
        );
        points.setEditable(true);

        boolean accepted = show(
                existing == null ? "Assign task" : "Edit task",
                new String[]{
                        "Task title",
                        "Assign to",
                        "Due date",
                        "Points"
                },
                title,
                assignee,
                due,
                points
        );

        if (!accepted) {
            return null;
        }

        return new JobDraft(
                title.getText(),
                assignee.getValue() == null
                        ? -1
                        : assignee.getValue().userId(),
                due.getValue(),
                points.getValue()
        );
    }

    public static boolean showPaymentForm(Event event) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.initOwner(App.window());
        dialog.setTitle("Event Registration");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL);

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(15);
        content.setStyle("-fx-padding: 24; -fx-pref-width: 440;");

        javafx.scene.layout.HBox headerBox = new javafx.scene.layout.HBox(10);
        headerBox.setAlignment(javafx.geometry.Pos.CENTER);
        
        String activeEvent = App.getActiveMainEvent();
        String logoFile = "images/bitfest.png";
        if (activeEvent != null) {
            switch (activeEvent.toLowerCase()) {
                case "calibration" -> logoFile = "images/calibration.png";
                case "ignition" -> logoFile = "images/ignition.png";
                default -> logoFile = "images/bitfest.png";
            }
        }
        
        java.net.URL url = App.class.getResource(logoFile);
        if (url != null) {
            javafx.scene.image.ImageView logo = new javafx.scene.image.ImageView(new javafx.scene.image.Image(url.toExternalForm()));
            logo.setFitHeight(50);
            logo.setPreserveRatio(true);
            headerBox.getChildren().add(logo);
        }

        javafx.scene.layout.VBox titleBox = new javafx.scene.layout.VBox(4);
        Label titleLabel = new Label("Secure Registration");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Label subtitle = new Label(event.title());
        titleBox.getChildren().addAll(titleLabel, subtitle);
        headerBox.getChildren().add(titleBox);

        TextField nameField = new TextField();
        nameField.setPromptText("Participant Full Name");
        TextField teamField = new TextField();
        teamField.setPromptText("Team Name (Optional)");
        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone Number");

        Button payNowBtn = new Button("Pay Now 💳");
        payNowBtn.setStyle("-fx-background-color: #4f46e5; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 20; -fx-cursor: hand;");
        payNowBtn.setMaxWidth(Double.MAX_VALUE);

        javafx.scene.layout.HBox paymentOptions = new javafx.scene.layout.HBox(12);
        paymentOptions.setAlignment(javafx.geometry.Pos.CENTER);
        paymentOptions.setVisible(false);
        paymentOptions.setManaged(false);

        Button bkashBtn = new Button();
        try {
            javafx.scene.image.ImageView bkashLogo = new javafx.scene.image.ImageView(new javafx.scene.image.Image(App.class.getResource("images/bkash.png").toExternalForm()));
            bkashLogo.setFitHeight(30);
            bkashLogo.setPreserveRatio(true);
            bkashBtn.setGraphic(bkashLogo);
        } catch (Exception ex) {
            bkashBtn.setText("bKash");
        }
        bkashBtn.setStyle("-fx-background-color: white; -fx-cursor: hand; -fx-padding: 5 15; -fx-border-color: #e2136e; -fx-border-radius: 4; -fx-background-radius: 4;");
        
        Button cardBtn = new Button();
        try {
            javafx.scene.image.ImageView cardLogo = new javafx.scene.image.ImageView(new javafx.scene.image.Image(App.class.getResource("images/card.png").toExternalForm()));
            cardLogo.setFitHeight(30);
            cardLogo.setPreserveRatio(true);
            cardBtn.setGraphic(cardLogo);
        } catch (Exception ex) {
            cardBtn.setText("Card (Debit/Credit)");
        }
        cardBtn.setStyle("-fx-background-color: white; -fx-cursor: hand; -fx-padding: 5 15; -fx-border-color: #334155; -fx-border-radius: 4; -fx-background-radius: 4;");

        Button spotBtn = new Button("Spot Registration");
        spotBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 10 15; -fx-border-radius: 4; -fx-background-radius: 4;");

        paymentOptions.getChildren().addAll(bkashBtn, cardBtn, spotBtn);

        payNowBtn.setOnAction(e -> {
            if (nameField.getText().isBlank() || phoneField.getText().isBlank()) {
                Forms.info("Name and Phone Number are required.");
                return;
            }
            payNowBtn.setVisible(false);
            payNowBtn.setManaged(false);
            paymentOptions.setVisible(true);
            paymentOptions.setManaged(true);
        });

        bkashBtn.setOnAction(e -> { dialog.setResult(true); dialog.close(); });
        cardBtn.setOnAction(e -> { dialog.setResult(true); dialog.close(); });
        spotBtn.setOnAction(e -> { dialog.setResult(true); dialog.close(); });

        content.getChildren().addAll(
                headerBox,
                new javafx.scene.layout.Region(),
                new Label("Participant Details:"), nameField, teamField, phoneField,
                new javafx.scene.layout.Region(),
                payNowBtn,
                paymentOptions
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getStylesheets().add(Forms.class.getResource("style.css").toExternalForm());

        return dialog.showAndWait().orElse(false);
    }

    public static boolean confirm(String message) {
        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION,
                message,
                ButtonType.YES,
                ButtonType.NO
        );

        alert.initOwner(App.window());
        alert.setTitle("Confirm action");
        alert.setHeaderText("Are you sure?");

        return alert.showAndWait().orElse(ButtonType.NO)
                == ButtonType.YES;
    }

    public static void info(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(App.window());
        alert.setTitle("Eventify");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
