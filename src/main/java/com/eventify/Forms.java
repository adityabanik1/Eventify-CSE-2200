package com.eventify;

import com.eventify.Models.*;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

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
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setStyle("-fx-padding: 20;");
        grid.setPrefWidth(480);

        for (int i = 0; i < controls.length; i++) {
            Label label = new Label(labels[i]);
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

        TextArea description = new TextArea(
                existing == null ? "" : existing.description()
        );
        description.setPrefRowCount(3);
        description.setWrapText(true);

        TextField venue = new TextField(
                existing == null ? "" : existing.venue()
        );

        DatePicker date = new DatePicker(
                existing == null
                        ? LocalDate.now().plusDays(1)
                        : existing.date()
        );
        date.setEditable(false);

        TextField time = new TextField(
                existing == null ? "09:00" : existing.time()
        );
        time.setPromptText("HH:mm");

        Spinner<Integer> capacity = new Spinner<>(
                1,
                100_000,
                existing == null ? 100 : existing.capacity()
        );
        capacity.setEditable(false);

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

        TextField start = new TextField(
                existing == null ? event.time() : existing.start()
        );

        TextField end = new TextField(
                existing == null ? "" : existing.end()
        );
        end.setPromptText("HH:mm, later than start");

        TextField speaker = new TextField(
                existing == null ? "" : existing.speaker()
        );

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
        points.setEditable(false);

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
