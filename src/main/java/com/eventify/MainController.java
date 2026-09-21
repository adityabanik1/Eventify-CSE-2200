package com.eventify;

import com.eventify.Models.*;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Function;

public class MainController {

    @FXML
    private BorderPane root;

    @FXML
    private Label userLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private ComboBox<Event> eventBox;

    @FXML
    private Label eventCountLabel;

    @FXML
    private Label upcomingCountLabel;

    @FXML
    private Label registrationCountLabel;

    @FXML
    private Label pendingCountLabel;

    @FXML
    private Label selectedEventLabel;

    @FXML
    private TextArea upcomingArea;

    @FXML
    private TableView<Event> eventsTable;

    @FXML
    private TableView<Registration> registrationsTable;

    @FXML
    private TableView<Slot> scheduleTable;

    @FXML
    private TableView<Job> tasksTable;

    @FXML
    private TableView<Score> leaderboardTable;

    @FXML
    private TableView<Holiday> holidaysTable;

    @FXML
    private HBox eventAdminBar;

    @FXML
    private HBox scheduleAdminBar;

    @FXML
    private HBox taskAdminBar;

    @FXML
    private Button registerButton;

    @FXML
    private Button attendanceButton;

    @FXML
    private Label registrationHint;

    @FXML
    private Label taskHint;

    @FXML
    private TextField countryField;

    @FXML
    private TextField yearField;

    private User user;
    private Snapshot snapshot;
    private boolean applyingSnapshot;

    @FunctionalInterface
    private interface Action {
        void run() throws Exception;
    }

    @FXML
    private void initialize() {
        yearField.setText(String.valueOf(LocalDate.now().getYear()));

        column(eventsTable, "ID", 60, Event::id);
        column(eventsTable, "Title", 260, Event::title);
        column(eventsTable, "Date", 120, Event::date);
        column(eventsTable, "Time", 90, Event::time);
        column(eventsTable, "Venue", 240, Event::venue);
        column(eventsTable, "Capacity", 100, Event::capacity);

        column(registrationsTable, "User ID", 90, Registration::userId);
        column(registrationsTable, "Name", 260, Registration::name);
        column(registrationsTable, "Email", 330, Registration::email);
        column(
                registrationsTable,
                "Attended",
                130,
                r -> r.attended() ? "Yes" : "No"
        );

        column(scheduleTable, "ID", 70, Slot::id);
        column(scheduleTable, "Session", 360, Slot::title);
        column(scheduleTable, "Start", 100, Slot::start);
        column(scheduleTable, "End", 100, Slot::end);
        column(scheduleTable, "Speaker / host", 280, Slot::speaker);

        column(tasksTable, "ID", 60, Job::id);
        column(tasksTable, "Task", 300, Job::title);
        column(tasksTable, "Assigned to", 220, Job::assigneeName);
        column(tasksTable, "Due date", 120, Job::due);
        column(tasksTable, "Points", 90, Job::points);
        column(
                tasksTable,
                "Status",
                130,
                j -> j.done() ? "Completed" : "Pending"
        );

        column(leaderboardTable, "Rank", 90, Score::rank);
        column(leaderboardTable, "Participant", 320, Score::name);
        column(
                leaderboardTable,
                "Attendance points",
                180,
                Score::attendancePoints
        );
        column(
                leaderboardTable,
                "Task points",
                160,
                Score::taskPoints
        );
        column(leaderboardTable, "Total score", 160, Score::total);

        column(holidaysTable, "Date", 130, Holiday::date);
        column(holidaysTable, "Local name", 320, Holiday::localName);
        column(holidaysTable, "English name", 360, Holiday::name);
        column(holidaysTable, "Country", 100, Holiday::countryCode);
    }

    private static <T> void column(
            TableView<T> table,
            String title,
            double width,
            Function<T, ?> getter
    ) {
        TableColumn<T, Object> column = new TableColumn<>(title);

        column.setCellValueFactory(
                cell -> new ReadOnlyObjectWrapper<>(
                        getter.apply(cell.getValue())
                )
        );

        column.setPrefWidth(width);
        table.getColumns().add(column);
        table.setPlaceholder(new Label("No records to display."));
    }

    public void setUser(User user) {
        this.user = user;

        userLabel.setText(
                user.name() + " | "
                        + (user.isAdmin() ? "Organizer" : "Participant")
        );

        visible(eventAdminBar, user.isAdmin());
        visible(scheduleAdminBar, user.isAdmin());
        visible(taskAdminBar, user.isAdmin());
        visible(attendanceButton, user.isAdmin());
        visible(registerButton, !user.isAdmin());

        registrationHint.setText(
                user.isAdmin()
                        ? "All registrations for the selected event. "
                            + "Select a participant to manage attendance or cancel."
                        : "Only your registration is shown here. "
                            + "Use Register to join the selected event."
        );

        taskHint.setText(
                user.isAdmin()
                        ? "Assign tasks to registered participants. "
                            + "Completed tasks contribute points to the leaderboard."
                        : "Only your assigned tasks are shown. "
                            + "Select a task to complete or reopen it."
        );

        refresh(null);
    }

    private static void visible(Node node, boolean value) {
        node.setVisible(value);
        node.setManaged(value);
    }

    private Integer selectedEventId() {
        Event selected = eventBox.getValue();
        return selected == null ? null : selected.id();
    }

    private Event requireEvent() {
        Event selected = eventBox.getValue();

        if (selected == null) {
            Forms.info("Create or select an event first.");
        }

        return selected;
    }

    private <T> T selected(TableView<T> table) {
        T row = table.getSelectionModel().getSelectedItem();

        if (row == null) {
            Forms.info("Select a row in the table first.");
        }

        return row;
    }

    private void refresh(Integer eventId) {
        App.run(
                root,
                statusLabel,
                () -> Database.snapshot(user, eventId),
                this::apply
        );
    }

    private void change(Action action) {
        Integer eventId = selectedEventId();

        App.run(
                root,
                statusLabel,
                () -> {
                    action.run();
                    return Database.snapshot(user, eventId);
                },
                this::apply
        );
    }

    private void apply(Snapshot result) {
        snapshot = result;
        applyingSnapshot = true;

        try {
            eventBox.getItems().setAll(result.events());
            eventBox.setValue(result.activeEvent());

            eventsTable.getItems().setAll(result.events());
            registrationsTable.getItems().setAll(
                    result.registrations()
            );
            scheduleTable.getItems().setAll(result.slots());
            tasksTable.getItems().setAll(result.jobs());
            leaderboardTable.getItems().setAll(result.scores());
        } finally {
            applyingSnapshot = false;
        }

        eventCountLabel.setText(
                String.valueOf(result.events().size())
        );

        long upcomingEvents = result.events().stream()
                .filter(event -> LocalDateTime.of(
                        event.date(),
                        LocalTime.parse(event.time())
                ).isAfter(LocalDateTime.now()))
                .count();

        upcomingCountLabel.setText(
                String.valueOf(upcomingEvents)
        );

        registrationCountLabel.setText(
                String.valueOf(result.totalRegistrations())
        );

        pendingCountLabel.setText(
                String.valueOf(result.pendingTasks())
        );

        Event active = result.activeEvent();

        if (active == null) {
            selectedEventLabel.setText(
                    "No events yet. An organizer can create one in the Events tab."
            );
        } else {
            selectedEventLabel.setText(
                    active.title()
                            + "\n" + active.date()
                            + " at " + active.time()
                            + " | " + active.venue()
                            + " | Capacity: " + active.capacity()
                            + "\n" + active.description()
            );
        }

        upcomingArea.setText(
                result.upcoming().isEmpty()
                        ? "No upcoming schedule items. "
                            + "An organizer can add sessions in the Schedule tab."
                        : String.join("\n\n", result.upcoming())
        );

        statusLabel.setText(
                "Ready | "
                        + (active == null
                            ? "No event selected"
                            : active.title())
        );
    }

    @FXML
    private void onEventChanged() {
        if (!applyingSnapshot && user != null) {
            refresh(selectedEventId());
        }
    }

    @FXML
    private void onRefresh() {
        refresh(selectedEventId());
    }

    @FXML
    private void onLogout() {
        App.showLogin();
    }

    @FXML
    private void onViewEvent() {
        Event event = selected(eventsTable);

        if (event == null) {
            return;
        }

        Forms.info(
                event.title()
                        + "\n\nDate: " + event.date()
                        + "\nTime: " + event.time()
                        + "\nVenue: " + event.venue()
                        + "\nCapacity: " + event.capacity()
                        + "\n\n" + event.description()
        );
    }

    @FXML
    private void onAddEvent() {
        EventDraft draft = Forms.event(null);

        if (draft != null) {
            change(() -> Database.saveEvent(user, 0, draft));
        }
    }

    @FXML
    private void onEditEvent() {
        Event event = selected(eventsTable);

        if (event == null) {
            return;
        }

        EventDraft draft = Forms.event(event);

        if (draft != null) {
            change(() -> Database.saveEvent(
                    user,
                    event.id(),
                    draft
            ));
        }
    }

    @FXML
    private void onDeleteEvent() {
        Event event = selected(eventsTable);

        if (event != null && Forms.confirm(
                "Delete \"" + event.title() + "\"?\n\n"
                        + "Its registrations, schedule and tasks "
                        + "will also be deleted."
        )) {
            change(() -> Database.deleteEvent(user, event.id()));
        }
    }

    @FXML
    private void onRegisterEvent() {
        Event event = requireEvent();

        if (event != null) {
            change(() -> Database.register(user, event.id()));
        }
    }

    @FXML
    private void onCancelRegistration() {
        Event event = requireEvent();

        if (event == null) {
            return;
        }

        int participantId;

        if (user.isAdmin()) {
            Registration registration = selected(registrationsTable);

            if (registration == null) {
                return;
            }

            participantId = registration.userId();
        } else {
            if (snapshot.registrations().isEmpty()) {
                Forms.info("You are not registered for this event.");
                return;
            }

            participantId = user.id();
        }

        if (Forms.confirm(
                "Cancel this registration?\n\n"
                        + "Assigned tasks for this participant in this event "
                        + "will also be deleted."
        )) {
            change(() -> Database.cancelRegistration(
                    user,
                    event.id(),
                    participantId
            ));
        }
    }

    @FXML
    private void onToggleAttendance() {
        Event event = requireEvent();

        if (event == null) {
            return;
        }

        Registration registration = selected(registrationsTable);

        if (registration != null) {
            change(() -> Database.setAttendance(
                    user,
                    event.id(),
                    registration.userId(),
                    !registration.attended()
            ));
        }
    }

    @FXML
    private void onAddSlot() {
        Event event = requireEvent();

        if (event == null) {
            return;
        }

        SlotDraft draft = Forms.slot(null, event);

        if (draft != null) {
            change(() -> Database.saveSlot(
                    user,
                    event.id(),
                    0,
                    draft
            ));
        }
    }

    @FXML
    private void onEditSlot() {
        Event event = requireEvent();

        if (event == null) {
            return;
        }

        Slot slot = selected(scheduleTable);

        if (slot == null) {
            return;
        }

        SlotDraft draft = Forms.slot(slot, event);

        if (draft != null) {
            change(() -> Database.saveSlot(
                    user,
                    event.id(),
                    slot.id(),
                    draft
            ));
        }
    }

    @FXML
    private void onDeleteSlot() {
        Event event = requireEvent();

        if (event == null) {
            return;
        }

        Slot slot = selected(scheduleTable);

        if (slot != null && Forms.confirm(
                "Delete schedule item \"" + slot.title() + "\"?"
        )) {
            change(() -> Database.deleteSlot(
                    user,
                    event.id(),
                    slot.id()
            ));
        }
    }

    @FXML
    private void onAddJob() {
        Event event = requireEvent();

        if (event == null) {
            return;
        }

        if (snapshot.registrations().isEmpty()) {
            Forms.info(
                    "No participants are registered for this event.\n"
                            + "Register a participant before assigning tasks."
            );
            return;
        }

        JobDraft draft = Forms.job(
                null,
                snapshot.registrations(),
                event
        );

        if (draft != null) {
            change(() -> Database.saveJob(
                    user,
                    event.id(),
                    0,
                    draft
            ));
        }
    }

    @FXML
    private void onEditJob() {
        Event event = requireEvent();

        if (event == null) {
            return;
        }

        Job job = selected(tasksTable);

        if (job == null) {
            return;
        }

        JobDraft draft = Forms.job(
                job,
                snapshot.registrations(),
                event
        );

        if (draft != null) {
            change(() -> Database.saveJob(
                    user,
                    event.id(),
                    job.id(),
                    draft
            ));
        }
    }

    @FXML
    private void onToggleJob() {
        Event event = requireEvent();

        if (event == null) {
            return;
        }

        Job job = selected(tasksTable);

        if (job != null) {
            change(() -> Database.setJobDone(
                    user,
                    event.id(),
                    job.id(),
                    !job.done()
            ));
        }
    }

    @FXML
    private void onDeleteJob() {
        Event event = requireEvent();

        if (event == null) {
            return;
        }

        Job job = selected(tasksTable);

        if (job != null && Forms.confirm(
                "Delete task \"" + job.title() + "\"?\n"
                        + "Any points from this task will disappear."
        )) {
            change(() -> Database.deleteJob(
                    user,
                    event.id(),
                    job.id()
            ));
        }
    }

    @FXML
    private void onFetchHolidays() {
        String country = countryField.getText();
        String year = yearField.getText();

        App.run(
                root,
                statusLabel,
                () -> JsonService.fetchHolidays(country, year),
                holidays -> {
                    holidaysTable.getItems().setAll(holidays);

                    statusLabel.setText(
                            "Loaded " + holidays.size()
                                    + " holidays from Nager.Date."
                    );
                }
        );
    }

    private FileChooser jsonChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);

        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "JSON files",
                        "*.json"
                )
        );

        return chooser;
    }

    @FXML
    private void onLoadHolidays() {
        File file = jsonChooser("Open holiday JSON")
                .showOpenDialog(App.window());

        if (file == null) {
            return;
        }

        App.run(
                root,
                statusLabel,
                () -> JsonService.readHolidays(file.toPath()),
                holidays -> {
                    holidaysTable.getItems().setAll(holidays);

                    statusLabel.setText(
                            "Loaded " + holidays.size()
                                    + " holidays from " + file.getName()
                    );
                }
        );
    }

    @FXML
    private void onExportEvents() {
        FileChooser chooser = jsonChooser("Export events");
        chooser.setInitialFileName("eventify-events.json");

        File file = chooser.showSaveDialog(App.window());

        if (file == null) {
            return;
        }

        List<Event> events = List.copyOf(snapshot.events());

        App.run(
                root,
                statusLabel,
                () -> {
                    JsonService.exportEvents(file.toPath(), events);
                    return null;
                },
                ignored -> {
                    statusLabel.setText(
                            "Events exported to " + file.getAbsolutePath()
                    );

                    Forms.info("Events exported successfully.");
                }
        );
    }
}
