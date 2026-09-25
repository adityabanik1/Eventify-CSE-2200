package com.eventify;

import com.eventify.Models.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Database {

    public static Path FILE = Path.of(
            System.getProperty("user.home"),
            ".eventify",
            "eventify.db"
    );

    private static String activeContext = "eventify";

    public static void setDatabaseContext(String contextName) {
        activeContext = contextName != null ? contextName : "eventify";
        FILE = Path.of(
                System.getProperty("user.home"),
                ".eventify",
                activeContext + ".db"
        );
    }

    private Database() {
    }

    @FunctionalInterface
    private interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    private static Connection connect() throws SQLException {
        Connection connection = DriverManager.getConnection(
                "jdbc:sqlite:" + FILE.toAbsolutePath()
        );

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
        } catch (SQLException e) {
            connection.close();
            throw e;
        }

        return connection;
    }

    private static void bind(
            PreparedStatement statement,
            Object... values
    ) throws SQLException {
        for (int i = 0; i < values.length; i++) {
            statement.setObject(i + 1, values[i]);
        }
    }

    private static int update(
            Connection connection,
            String sql,
            Object... values
    ) throws SQLException {
        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {
            bind(statement, values);
            return statement.executeUpdate();
        }
    }

    private static <T> List<T> query(
            Connection connection,
            String sql,
            RowMapper<T> mapper,
            Object... values
    ) throws SQLException {
        List<T> results = new ArrayList<>();
        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {
            bind(statement, values);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    results.add(mapper.map(rs));
                }
            }
        }
        return results;
    }

    private static int count(
            Connection connection,
            String sql,
            Object... values
    ) throws SQLException {
        return query(
                connection,
                sql,
                rs -> rs.getInt(1),
                values
        ).getFirst();
    }

    private static Event mapEvent(ResultSet rs) throws SQLException {
        return new Event(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("venue"),
                LocalDate.parse(rs.getString("event_date")),
                rs.getString("event_time"),
                rs.getInt("capacity")
        );
    }

    public static void initialize() throws Exception {
        Files.createDirectories(FILE.getParent());

        try (Connection connection = connect();
             Statement statement = connection.createStatement()) {

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        email TEXT NOT NULL COLLATE NOCASE UNIQUE,
                        password_hash TEXT NOT NULL,
                        role TEXT NOT NULL
                            CHECK(role IN ('ADMIN', 'PARTICIPANT'))
                    )
                    """);

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        venue TEXT NOT NULL,
                        event_date TEXT NOT NULL,
                        event_time TEXT NOT NULL,
                        capacity INTEGER NOT NULL CHECK(capacity > 0)
                    )
                    """);

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS registrations (
                        event_id INTEGER NOT NULL,
                        user_id INTEGER NOT NULL,
                        attended INTEGER NOT NULL DEFAULT 0
                            CHECK(attended IN (0, 1)),
                        PRIMARY KEY(event_id, user_id),
                        FOREIGN KEY(event_id)
                            REFERENCES events(id) ON DELETE CASCADE,
                        FOREIGN KEY(user_id)
                            REFERENCES users(id) ON DELETE CASCADE
                    )
                    """);

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS schedule (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        event_id INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        start_time TEXT NOT NULL,
                        end_time TEXT NOT NULL,
                        speaker TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(event_id)
                            REFERENCES events(id) ON DELETE CASCADE
                    )
                    """);

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS tasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        event_id INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        assignee_id INTEGER NOT NULL,
                        due_date TEXT NOT NULL,
                        points INTEGER NOT NULL CHECK(points > 0),
                        done INTEGER NOT NULL DEFAULT 0
                            CHECK(done IN (0, 1)),
                        FOREIGN KEY(event_id)
                            REFERENCES events(id) ON DELETE CASCADE,
                        FOREIGN KEY(event_id, assignee_id)
                            REFERENCES registrations(event_id, user_id)
                            ON DELETE CASCADE
                    )
                    """);

            statement.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_schedule_event
                    ON schedule(event_id)
                    """);

            statement.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_tasks_event
                    ON tasks(event_id)
                    """);

            boolean adminMissing = count(
                    connection,
                    "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'"
            ) == 0;

            if (adminMissing) {
                connection.setAutoCommit(false);
                try {
                    String adminPassword = System.getenv(
                            "EVENTIFY_ADMIN_PASSWORD"
                    );

                    if (adminPassword == null || adminPassword.isBlank()) {
                        adminPassword = "Admin@12345";
                    }

                    if (adminPassword.length() < 8) {
                        throw new IllegalArgumentException(
                                "EVENTIFY_ADMIN_PASSWORD must contain at least 8 characters."
                        );
                    }

                    update(
                            connection,
                            """
                            INSERT INTO users
                                (name, email, password_hash, role)
                            VALUES (?, ?, ?, 'ADMIN')
                            """,
                            "Eventify Organizer",
                            "admin@eventify.com",
                            Passwords.hash(adminPassword)
                    );

                    connection.commit();
                } catch (Exception e) {
                    connection.rollback();
                    throw e;
                }
            }

            // Verify if context-specific sub-events have been seeded
            boolean needsSeeding = false;
            if ("BitFest".equalsIgnoreCase(activeContext)) {
                needsSeeding = count(connection, "SELECT COUNT(*) FROM events") < 8;
            } else if ("Calibration".equalsIgnoreCase(activeContext)) {
                needsSeeding = count(connection, "SELECT COUNT(*) FROM events WHERE title LIKE '%Micromouse%' OR title LIKE '%CAD Contest%'") == 0;
            } else if ("Ignition".equalsIgnoreCase(activeContext)) {
                needsSeeding = count(connection, "SELECT COUNT(*) FROM events WHERE title LIKE '%3 Minute Thesis%' OR title LIKE '%Mechanics Olympiad%'") == 0;
            } else {
                needsSeeding = count(connection, "SELECT COUNT(*) FROM events") == 0;
            }

            if (needsSeeding) {
                connection.setAutoCommit(false);
                try {
                    // Clean cascade wipe of old sample data in this specific database
                    update(connection, "DELETE FROM tasks");
                    update(connection, "DELETE FROM schedule");
                    update(connection, "DELETE FROM registrations");
                    update(connection, "DELETE FROM events");

                    seedContextEvents(connection, activeContext);
                    connection.commit();
                } catch (Exception e) {
                    connection.rollback();
                    throw e;
                }
            }

            // Ensure template participant accounts exist for testing
            connection.setAutoCommit(false);
            try {
                String hash = Passwords.hash("Password@123");

                update(
                        connection,
                        "INSERT OR IGNORE INTO users (name, email, password_hash, role) VALUES (?, ?, ?, 'PARTICIPANT')",
                        "Alice Walker", "alice@example.com", hash
                );
                update(
                        connection,
                        "INSERT OR IGNORE INTO users (name, email, password_hash, role) VALUES (?, ?, ?, 'PARTICIPANT')",
                        "Bob Martin", "bob@example.com", hash
                );
                update(
                        connection,
                        "INSERT OR IGNORE INTO users (name, email, password_hash, role) VALUES (?, ?, ?, 'PARTICIPANT')",
                        "Charlie Davis", "charlie@example.com", hash
                );

                List<Integer> eventIds = query(connection, "SELECT id FROM events", rs -> rs.getInt(1));
                List<Integer> userIds = query(connection, "SELECT id FROM users WHERE role = 'PARTICIPANT' ORDER BY id", rs -> rs.getInt(1));

                if (!eventIds.isEmpty() && userIds.size() >= 2) {
                    int aliceId = userIds.get(0);
                    int bobId = userIds.get(1);

                    for (int eid : eventIds) {
                        int regCount = count(connection, "SELECT COUNT(*) FROM registrations WHERE event_id = ?", eid);
                        if (regCount == 0) {
                            // Register Alice (attended = 1) and Bob (attended = 0)
                            update(connection, "INSERT OR IGNORE INTO registrations (event_id, user_id, attended) VALUES (?, ?, 1)", eid, aliceId);
                            update(connection, "INSERT OR IGNORE INTO registrations (event_id, user_id, attended) VALUES (?, ?, 0)", eid, bobId);

                            // Template tasks for testing
                            update(connection,
                                    "INSERT INTO tasks (event_id, title, assignee_id, due_date, points, done) VALUES (?, ?, ?, ?, ?, 1)",
                                    eid, "Prepare welcome kits", aliceId, LocalDate.now().plusDays(2).toString(), 20);
                            update(connection,
                                    "INSERT INTO tasks (event_id, title, assignee_id, due_date, points, done) VALUES (?, ?, ?, ?, ?, 0)",
                                    eid, "Check projector setup", bobId, LocalDate.now().plusDays(3).toString(), 15);
                        }
                    }
                }

                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private static String required(
            String value,
            String field,
            int maximum
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required.");
        }

        String result = value.trim();

        if (result.length() > maximum) {
            throw new IllegalArgumentException(
                    field + " must not exceed " + maximum + " characters."
            );
        }

        return result;
    }

    private static String email(String value) {
        String result = required(value, "Email", 150)
                .toLowerCase(Locale.ROOT);

        if (!result.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException(
                    "Enter a valid email address."
            );
        }

        return result;
    }

    private static String time(String value) {
        String result = required(value, "Time", 5);

        if (!result.matches("\\d{2}:\\d{2}")) {
            throw new IllegalArgumentException(
                    "Use 24-hour time in HH:mm format, for example 09:30."
            );
        }

        try {
            LocalTime.parse(result);
            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid time. Use a value between 00:00 and 23:59."
            );
        }
    }

    private static void requireAdmin(User actor) {
        if (actor == null || !actor.isAdmin()) {
            throw new SecurityException(
                    "Only an organizer can perform this action."
            );
        }
    }

    private static Event event(
            Connection connection,
            int eventId
    ) throws SQLException {
        List<Event> results = query(
                connection,
                "SELECT * FROM events WHERE id = ?",
                Database::mapEvent,
                eventId
        );

        if (results.isEmpty()) {
            throw new IllegalArgumentException("Event no longer exists.");
        }

        return results.getFirst();
    }

    public static User login(
            String suppliedEmail,
            String password
    ) throws Exception {
        String normalizedEmail = email(suppliedEmail);

        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM users WHERE email = ?"
             )) {

            statement.setString(1, normalizedEmail);

            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()
                        || !Passwords.matches(
                                password,
                                rs.getString("password_hash")
                        )) {
                    throw new IllegalArgumentException(
                            "Incorrect email or password."
                    );
                }

                return new User(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("role")
                );
            }
        }
    }

    public static void createParticipant(
            String suppliedName,
            String suppliedEmail,
            String password
    ) throws Exception {
        String name = required(suppliedName, "Name", 100);
        String normalizedEmail = email(suppliedEmail);

        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException(
                    "Password must contain at least 8 characters."
            );
        }

        try (Connection connection = connect()) {
            if (count(
                    connection,
                    "SELECT COUNT(*) FROM users WHERE email = ?",
                    normalizedEmail
            ) > 0) {
                throw new IllegalArgumentException(
                        "An account with this email already exists."
                );
            }

            update(
                    connection,
                    """
                    INSERT INTO users
                        (name, email, password_hash, role)
                    VALUES (?, ?, ?, 'PARTICIPANT')
                    """,
                    name,
                    normalizedEmail,
                    Passwords.hash(password)
            );
        }
    }

    public static void saveEvent(
            User actor,
            int id,
            EventDraft draft
    ) throws Exception {
        requireAdmin(actor);

        String title = required(draft.title(), "Title", 120);
        String venue = required(draft.venue(), "Venue", 150);
        String eventTime = time(draft.time());

        if (draft.date() == null) {
            throw new IllegalArgumentException("Select an event date.");
        }

        if (draft.capacity() < 1 || draft.capacity() > 100_000) {
            throw new IllegalArgumentException(
                    "Capacity must be between 1 and 100000."
            );
        }

        String description = draft.description() == null
                ? ""
                : draft.description().trim();

        if (description.length() > 3000) {
            throw new IllegalArgumentException(
                    "Description must not exceed 3000 characters."
            );
        }

        try (Connection connection = connect()) {
            connection.setAutoCommit(false);

            try {
                if (id == 0) {
                    update(
                            connection,
                            """
                            INSERT INTO events
                                (title, description, venue,
                                 event_date, event_time, capacity)
                            VALUES (?, ?, ?, ?, ?, ?)
                            """,
                            title,
                            description,
                            venue,
                            draft.date().toString(),
                            eventTime,
                            draft.capacity()
                    );
                } else {
                    event(connection, id);

                    int registered = count(
                            connection,
                            """
                            SELECT COUNT(*) FROM registrations
                            WHERE event_id = ?
                            """,
                            id
                    );

                    if (draft.capacity() < registered) {
                        throw new IllegalArgumentException(
                                "Capacity cannot be lower than the current "
                                        + registered + " registrations."
                        );
                    }

                    int earlierSlots = count(
                            connection,
                            """
                            SELECT COUNT(*) FROM schedule
                            WHERE event_id = ? AND start_time < ?
                            """,
                            id,
                            eventTime
                    );

                    if (earlierSlots > 0) {
                        throw new IllegalArgumentException(
                                "An existing schedule item starts before "
                                        + "the proposed event start time."
                        );
                    }

                    update(
                            connection,
                            """
                            UPDATE events
                            SET title = ?, description = ?, venue = ?,
                                event_date = ?, event_time = ?, capacity = ?
                            WHERE id = ?
                            """,
                            title,
                            description,
                            venue,
                            draft.date().toString(),
                            eventTime,
                            draft.capacity(),
                            id
                    );
                }

                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public static void deleteEvent(User actor, int id)
            throws Exception {
        requireAdmin(actor);

        try (Connection connection = connect()) {
            update(connection, "DELETE FROM events WHERE id = ?", id);
        }
    }

    public static void register(User actor, int eventId)
            throws Exception {
        if (actor == null || actor.isAdmin()) {
            throw new SecurityException(
                    "Use a participant account to register."
            );
        }

        try (Connection connection = connect()) {
            connection.setAutoCommit(false);

            try {
                Event selected = event(connection, eventId);

                LocalDateTime start = LocalDateTime.of(
                        selected.date(),
                        LocalTime.parse(selected.time())
                );

                if (!start.isAfter(LocalDateTime.now())) {
                    throw new IllegalArgumentException(
                            "Registration is closed because this event has started."
                    );
                }

                if (count(
                        connection,
                        """
                        SELECT COUNT(*) FROM registrations
                        WHERE event_id = ? AND user_id = ?
                        """,
                        eventId,
                        actor.id()
                ) > 0) {
                    throw new IllegalArgumentException(
                            "You are already registered for this event."
                    );
                }

                int registered = count(
                        connection,
                        """
                        SELECT COUNT(*) FROM registrations
                        WHERE event_id = ?
                        """,
                        eventId
                );

                if (registered >= selected.capacity()) {
                    throw new IllegalArgumentException("This event is full.");
                }

                update(
                        connection,
                        """
                        INSERT INTO registrations(event_id, user_id)
                        VALUES (?, ?)
                        """,
                        eventId,
                        actor.id()
                );

                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public static void cancelRegistration(
            User actor,
            int eventId,
            int participantId
    ) throws Exception {
        if (!actor.isAdmin() && actor.id() != participantId) {
            throw new SecurityException(
                    "You can cancel only your own registration."
            );
        }

        try (Connection connection = connect()) {
            update(
                    connection,
                    """
                    DELETE FROM registrations
                    WHERE event_id = ? AND user_id = ?
                    """,
                    eventId,
                    participantId
            );
        }
    }

    public static void setAttendance(
            User actor,
            int eventId,
            int participantId,
            boolean attended
    ) throws Exception {
        requireAdmin(actor);

        try (Connection connection = connect()) {
            update(
                    connection,
                    """
                    UPDATE registrations SET attended = ?
                    WHERE event_id = ? AND user_id = ?
                    """,
                    attended ? 1 : 0,
                    eventId,
                    participantId
            );
        }
    }

    public static void saveSlot(
            User actor,
            int eventId,
            int id,
            SlotDraft draft
    ) throws Exception {
        requireAdmin(actor);

        String title = required(draft.title(), "Session title", 120);
        String start = time(draft.start());
        String end = time(draft.end());
        String speaker = draft.speaker() == null
                ? ""
                : draft.speaker().trim();

        if (end.compareTo(start) <= 0) {
            throw new IllegalArgumentException(
                    "Session end time must be after its start time."
            );
        }

        try (Connection connection = connect()) {
            Event selected = event(connection, eventId);

            if (start.compareTo(selected.time()) < 0) {
                throw new IllegalArgumentException(
                        "A session cannot begin before the event start time."
                );
            }

            int overlaps = count(
                    connection,
                    """
                    SELECT COUNT(*) FROM schedule
                    WHERE event_id = ?
                      AND id <> ?
                      AND start_time < ?
                      AND end_time > ?
                    """,
                    eventId,
                    id,
                    end,
                    start
            );

            if (overlaps > 0) {
                throw new IllegalArgumentException(
                        "This session overlaps an existing session."
                );
            }

            if (id == 0) {
                update(
                        connection,
                        """
                        INSERT INTO schedule
                            (event_id, title, start_time, end_time, speaker)
                        VALUES (?, ?, ?, ?, ?)
                        """,
                        eventId,
                        title,
                        start,
                        end,
                        speaker
                );
            } else {
                update(
                        connection,
                        """
                        UPDATE schedule
                        SET title = ?, start_time = ?, end_time = ?, speaker = ?
                        WHERE id = ? AND event_id = ?
                        """,
                        title,
                        start,
                        end,
                        speaker,
                        id,
                        eventId
                );
            }
        }
    }

    public static void deleteSlot(
            User actor,
            int eventId,
            int id
    ) throws Exception {
        requireAdmin(actor);

        try (Connection connection = connect()) {
            update(
                    connection,
                    "DELETE FROM schedule WHERE id = ? AND event_id = ?",
                    id,
                    eventId
            );
        }
    }

    public static void saveJob(
            User actor,
            int eventId,
            int id,
            JobDraft draft
    ) throws Exception {
        requireAdmin(actor);

        String title = required(draft.title(), "Task title", 150);

        if (draft.due() == null) {
            throw new IllegalArgumentException("Select a task due date.");
        }

        if (draft.points() < 1 || draft.points() > 1000) {
            throw new IllegalArgumentException(
                    "Task points must be between 1 and 1000."
            );
        }

        try (Connection connection = connect()) {
            if (count(
                    connection,
                    """
                    SELECT COUNT(*) FROM registrations
                    WHERE event_id = ? AND user_id = ?
                    """,
                    eventId,
                    draft.assigneeId()
            ) == 0) {
                throw new IllegalArgumentException(
                        "The assignee must be registered for this event."
                );
            }

            if (id == 0) {
                update(
                        connection,
                        """
                        INSERT INTO tasks
                            (event_id, title, assignee_id, due_date, points)
                        VALUES (?, ?, ?, ?, ?)
                        """,
                        eventId,
                        title,
                        draft.assigneeId(),
                        draft.due().toString(),
                        draft.points()
                );
            } else {
                update(
                        connection,
                        """
                        UPDATE tasks
                        SET title = ?, assignee_id = ?, due_date = ?, points = ?
                        WHERE id = ? AND event_id = ?
                        """,
                        title,
                        draft.assigneeId(),
                        draft.due().toString(),
                        draft.points(),
                        id,
                        eventId
                );
            }
        }
    }

    public static void setJobDone(
            User actor,
            int eventId,
            int id,
            boolean done
    ) throws Exception {
        try (Connection connection = connect()) {
            List<Integer> assignees = query(
                    connection,
                    """
                    SELECT assignee_id FROM tasks
                    WHERE id = ? AND event_id = ?
                    """,
                    rs -> rs.getInt(1),
                    id,
                    eventId
            );

            if (assignees.isEmpty()) {
                throw new IllegalArgumentException("Task no longer exists.");
            }

            if (!actor.isAdmin() && assignees.getFirst() != actor.id()) {
                throw new SecurityException(
                        "You can update only your own tasks."
                );
            }

            update(
                    connection,
                    """
                    UPDATE tasks SET done = ?
                    WHERE id = ? AND event_id = ?
                    """,
                    done ? 1 : 0,
                    id,
                    eventId
            );
        }
    }

    public static void deleteJob(
            User actor,
            int eventId,
            int id
    ) throws Exception {
        requireAdmin(actor);

        try (Connection connection = connect()) {
            update(
                    connection,
                    "DELETE FROM tasks WHERE id = ? AND event_id = ?",
                    id,
                    eventId
            );
        }
    }

    public static Snapshot snapshot(
            User actor,
            Integer preferredEventId
    ) throws Exception {
        try (Connection connection = connect()) {
            List<Event> events = query(
                    connection,
                    """
                    SELECT * FROM events
                    ORDER BY event_date, event_time, id
                    """,
                    Database::mapEvent
            );

            Event active = null;

            if (preferredEventId != null) {
                active = events.stream()
                        .filter(e -> e.id() == preferredEventId)
                        .findFirst()
                        .orElse(null);
            }

            if (active == null && !events.isEmpty()) {
                active = events.getFirst();
            }

            int eventId = active == null ? -1 : active.id();

            // If the selected event has no registrations, auto-seed Alice & Bob so admin can test immediately
            if (eventId > 0) {
                int regCount = count(connection, "SELECT COUNT(*) FROM registrations WHERE event_id = ?", eventId);
                if (regCount == 0) {
                    List<Integer> userIds = query(connection, "SELECT id FROM users WHERE role = 'PARTICIPANT' ORDER BY id", rs -> rs.getInt(1));
                    if (userIds.size() >= 2) {
                        int aliceId = userIds.get(0);
                        int bobId = userIds.get(1);
                        update(connection, "INSERT OR IGNORE INTO registrations (event_id, user_id, attended) VALUES (?, ?, 1)", eventId, aliceId);
                        update(connection, "INSERT OR IGNORE INTO registrations (event_id, user_id, attended) VALUES (?, ?, 0)", eventId, bobId);

                        update(connection,
                                "INSERT INTO tasks (event_id, title, assignee_id, due_date, points, done) VALUES (?, ?, ?, ?, ?, 1)",
                                eventId, "Prepare welcome kits", aliceId, LocalDate.now().plusDays(2).toString(), 20);
                        update(connection,
                                "INSERT INTO tasks (event_id, title, assignee_id, due_date, points, done) VALUES (?, ?, ?, ?, ?, 0)",
                                eventId, "Check projector setup", bobId, LocalDate.now().plusDays(3).toString(), 15);
                    }
                }
            }

            List<Registration> registrations = query(
                    connection,
                    """
                    SELECT u.id, u.name, u.email, r.attended
                    FROM registrations r
                    JOIN users u ON u.id = r.user_id
                    WHERE r.event_id = ?
                      AND (? = 1 OR u.id = ?)
                    ORDER BY u.name, u.id
                    """,
                    rs -> new Registration(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getInt("attended") == 1
                    ),
                    eventId,
                    actor.isAdmin() ? 1 : 0,
                    actor.id()
            );

            List<Slot> slots = query(
                    connection,
                    """
                    SELECT * FROM schedule
                    WHERE event_id = ?
                    ORDER BY start_time, id
                    """,
                    rs -> new Slot(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("start_time"),
                            rs.getString("end_time"),
                            rs.getString("speaker")
                    ),
                    eventId
            );

            List<Job> jobs = query(
                    connection,
                    """
                    SELECT t.*, u.name AS assignee_name
                    FROM tasks t
                    JOIN users u ON u.id = t.assignee_id
                    WHERE t.event_id = ?
                      AND (? = 1 OR t.assignee_id = ?)
                    ORDER BY t.done, t.due_date, t.id
                    """,
                    rs -> new Job(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getInt("assignee_id"),
                            rs.getString("assignee_name"),
                            LocalDate.parse(rs.getString("due_date")),
                            rs.getInt("points"),
                            rs.getInt("done") == 1
                    ),
                    eventId,
                    actor.isAdmin() ? 1 : 0,
                    actor.id()
            );

            List<Score> unranked = query(
                    connection,
                    """
                    SELECT u.id, u.name,
                           CASE WHEN r.attended = 1
                                THEN 10 ELSE 0 END AS attendance_points,
                           COALESCE(SUM(
                               CASE WHEN t.done = 1
                                    THEN t.points ELSE 0 END
                           ), 0) AS task_points
                    FROM registrations r
                    JOIN users u ON u.id = r.user_id
                    LEFT JOIN tasks t
                      ON t.event_id = r.event_id
                     AND t.assignee_id = r.user_id
                    WHERE r.event_id = ?
                    GROUP BY u.id, u.name, r.attended
                    ORDER BY
                      (CASE WHEN r.attended = 1 THEN 10 ELSE 0 END
                       + COALESCE(SUM(
                           CASE WHEN t.done = 1 THEN t.points ELSE 0 END
                         ), 0)) DESC,
                      u.name, u.id
                    """,
                    rs -> {
                        int attendance = rs.getInt("attendance_points");
                        int taskPoints = rs.getInt("task_points");

                        return new Score(
                                0,
                                rs.getString("name"),
                                attendance,
                                taskPoints,
                                attendance + taskPoints
                        );
                    },
                    eventId
            );

            List<Score> scores = new ArrayList<>();
            int previousTotal = Integer.MIN_VALUE;
            int rank = 0;

            for (int i = 0; i < unranked.size(); i++) {
                Score row = unranked.get(i);

                if (row.total() != previousTotal) {
                    rank = i + 1;
                }

                scores.add(new Score(
                        rank,
                        row.name(),
                        row.attendancePoints(),
                        row.taskPoints(),
                        row.total()
                ));

                previousTotal = row.total();
            }

            int totalRegistrations = count(
                    connection,
                    "SELECT COUNT(*) FROM registrations"
            );

            int pendingTasks = count(
                    connection,
                    "SELECT COUNT(*) FROM tasks WHERE done = 0"
            );

            String now = LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
            );

            List<String> upcoming = query(
                    connection,
                    """
                    SELECT e.title AS event_title, e.event_date,
                           s.title AS session_title, s.start_time
                    FROM schedule s
                    JOIN events e ON e.id = s.event_id
                    WHERE (e.event_date || 'T' || s.start_time) >= ?
                    ORDER BY e.event_date, s.start_time
                    LIMIT 8
                    """,
                    rs -> rs.getString("event_date")
                            + " " + rs.getString("start_time")
                            + " | " + rs.getString("event_title")
                            + " | " + rs.getString("session_title"),
                    now
            );

            return new Snapshot(
                    events,
                    active,
                    registrations,
                    slots,
                    jobs,
                    scores,
                    totalRegistrations,
                    pendingTasks,
                    upcoming
            );
        }
    }

    private static SlotDraft slot(String title, String start, String end, String speaker) {
        return new SlotDraft(title, start, end, speaker);
    }

    private static void insertEventWithSchedule(
            Connection connection,
            String title,
            String description,
            String venue,
            LocalDate date,
            String time,
            int capacity,
            List<SlotDraft> slots
    ) throws SQLException {
        update(
                connection,
                """
                INSERT INTO events (title, description, venue, event_date, event_time, capacity)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                title, description, venue, date.toString(), time, capacity
        );

        int eventId = count(connection, "SELECT last_insert_rowid()");

        if (slots != null) {
            for (SlotDraft s : slots) {
                update(
                        connection,
                        """
                        INSERT INTO schedule (event_id, title, start_time, end_time, speaker)
                        VALUES (?, ?, ?, ?, ?)
                        """,
                        eventId, s.title(), s.start(), s.end(), s.speaker()
                );
            }
        }
    }

    private static void seedContextEvents(Connection connection, String context) throws Exception {
        LocalDate baseDate = LocalDate.now().plusDays(10);

        if ("BitFest".equalsIgnoreCase(context)) {
            // 3rd KUET CSE National Festival — BitFest 2025
            insertEventWithSchedule(connection, "Inter University Programming Contest (IUPC)",
                    "3rd KUET CSE National Festival premier competitive programming marathon for top universities.",
                    "CSE Computer Lab & Aud.", baseDate, "09:00", 60,
                    List.of(
                            slot("Reporting & Mock Contest", "09:00", "10:00", "Problemsetters Panel"),
                            slot("Main IUPC Contest (5 Hours)", "10:30", "15:30", "Contest Director"),
                            slot("Solution Discussion & Award Ceremony", "16:00", "17:30", "Chief Guest & HOD")
                    ));

            insertEventWithSchedule(connection, "Hackathon",
                    "24-hour sprint developing impactful software prototypes solving national problems.",
                    "Software Engineering Lab", baseDate.plusDays(1), "09:30", 50,
                    List.of(
                            slot("Problem Statement Reveal & Mentoring", "09:30", "10:30", "Industry Mentors"),
                            slot("Prototype Development & Checkpoint", "11:00", "16:00", "Technical Committee"),
                            slot("Final Demo & Pitch to Jury", "16:30", "18:00", "Startup Founders Panel")
                    ));

            insertEventWithSchedule(connection, "Datathon",
                    "Machine learning and data science analytics sprint on complex real-world data.",
                    "AI Research Lab", baseDate.plusDays(1), "10:00", 40,
                    List.of(
                            slot("Dataset Briefing & Evaluation Criteria", "10:00", "11:00", "Lead Data Scientist"),
                            slot("Modeling & Prediction Sprint", "11:30", "15:30", "Kaggle Grandmaster"),
                            slot("Insights Presentation & Defense", "16:00", "17:00", "Research Jury")
                    ));

            insertEventWithSchedule(connection, "Gaming Contest",
                    "Tactical high-intensity esports competition.",
                    "Student Center Arena", baseDate.plusDays(2), "10:00", 64,
                    List.of(
                            slot("Knockout Bracket Rounds", "10:00", "13:00", "Tournament Admins"),
                            slot("Semifinals Clash", "14:00", "16:00", "Casters Desk"),
                            slot("Grand Championship Finals", "16:30", "18:00", "Main Stage Host")
                    ));

            insertEventWithSchedule(connection, "Line Following Robot Competition",
                    "Autonomous line tracking robotics challenge across intricate curves and obstacles.",
                    "KUET Gymnasium", baseDate.plusDays(2), "09:30", 45,
                    List.of(
                            slot("Arena Calibration & Technical Inspection", "09:30", "10:30", "Technical Lead"),
                            slot("Time Trials (Round 1)", "11:00", "13:30", "Arena Referees"),
                            slot("Obstacle Course Finals", "14:30", "16:30", "Robotics Society")
                    ));

            insertEventWithSchedule(connection, "Soccer Bot",
                    "Remote-controlled vehicular robotics football battle in custom mini arena.",
                    "KUET Gymnasium Arena 2", baseDate.plusDays(2), "10:00", 40,
                    List.of(
                            slot("Group Stage League Matches", "10:00", "12:30", "Head Referee"),
                            slot("Quarter & Semi-Final Knockouts", "13:30", "15:00", "Arena Marshals"),
                            slot("Soccer Bot Championship Match", "15:30", "16:30", "Robotics Club President")
                    ));

            insertEventWithSchedule(connection, "Project Showcase",
                    "Showcase of innovative software, embedded systems, and hardware projects.",
                    "Central Exhibition Hall", baseDate.plusDays(3), "09:30", 80,
                    List.of(
                            slot("Public Exhibition & Demonstration", "09:30", "12:00", "Exhibitors"),
                            slot("Expert Jury Evaluation Rounds", "12:30", "15:00", "Faculty Panel"),
                            slot("Prize & Innovation Honors", "15:30", "16:30", "Dean of EEE")
                    ));

            insertEventWithSchedule(connection, "IT Business Case Competition",
                    "Strategic business problem solving combining tech viability and market strategy.",
                    "CSE Seminar Room", baseDate.plusDays(3), "09:00", 35,
                    List.of(
                            slot("Case Study Reveal & Team Huddle", "09:00", "10:00", "Case Author"),
                            slot("Strategy Formulation & Deck Build", "10:30", "13:30", "Mentors"),
                            slot("Boardroom Pitch to Judges", "14:00", "16:30", "Corporate Executives")
                    ));

        } else if ("Calibration".equalsIgnoreCase(context)) {
            // Calibration 2.0 (Department of Mechatronics Engineering, KUET)
            insertEventWithSchedule(connection, "CAD Contest",
                    "3D modeling and mechanical design challenge. Department of Mechatronics Engineering. Prize: BDT 30K.",
                    "CAD Design Studio", baseDate, "09:00", 50,
                    List.of(
                            slot("Briefing & Modeling Task Reveal", "09:00", "09:30", "Design Committee"),
                            slot("Solid Modeling & Rendering", "09:30", "12:30", "CAD Supervisors"),
                            slot("Model Evaluation & Defense", "13:30", "15:00", "Industry Jury")
                    ));

            insertEventWithSchedule(connection, "Soccer Bot",
                    "Fast-paced competitive robotic soccer tournament. Prize: BDT 50K.",
                    "Mechatronics Arena", baseDate, "09:30", 40,
                    List.of(
                            slot("Round of 16 & Group Matches", "09:30", "12:00", "Referee Committee"),
                            slot("Knockout Elimination Rounds", "13:00", "15:00", "Arena Marshals"),
                            slot("Championship Match & Awarding", "15:30", "16:30", "Faculty Advisor")
                    ));

            insertEventWithSchedule(connection, "Line Follower Robot (LFR)",
                    "High-speed autonomous navigation over complex tracks. Prize: BDT 50K.",
                    "Mechatronics Arena Track B", baseDate.plusDays(1), "10:00", 45,
                    List.of(
                            slot("Track Calibration & Testing", "10:00", "11:00", "Track In-Charge"),
                            slot("Qualifying Speed Heats", "11:30", "13:30", "Judges Panel"),
                            slot("Championship Fast Run", "14:30", "16:00", "Chief Judge")
                    ));

            insertEventWithSchedule(connection, "RC Speed Battle",
                    "Off-road radio-controlled high speed vehicular racing. Prize: BDT 40K.",
                    "KUET Central Field Track", baseDate.plusDays(1), "10:00", 35,
                    List.of(
                            slot("Vehicle Inspection & Time Trials", "10:00", "11:30", "Pit Crew Marshals"),
                            slot("Circuit Knockout Battles", "12:30", "14:30", "Race Director"),
                            slot("Podium Celebration", "15:00", "16:00", "Department Head")
                    ));

            insertEventWithSchedule(connection, "Micromouse Maze Solver",
                    "Autonomous maze-exploring algorithmic pathfinding contest. Prize: BDT 30K.",
                    "Robotics Lab", baseDate.plusDays(1), "09:30", 30,
                    List.of(
                            slot("Maze Exploration & Mapping Phase", "09:30", "11:30", "Algorithm Jury"),
                            slot("Fastest-Path Timed Runs", "12:30", "14:30", "Lead Arbiter"),
                            slot("Score Verification & Results", "15:00", "16:00", "Robotics Club")
                    ));

            insertEventWithSchedule(connection, "Ad-Making Contest",
                    "Creative advertising and promotional video production. Prize: BDT 25K.",
                    "Media Center Hall", baseDate.plusDays(2), "10:00", 40,
                    List.of(
                            slot("Commercial Screening Round", "10:00", "12:00", "Creative Director"),
                            slot("Pitch Defense & Q&A", "13:00", "14:30", "Ad Agency Panel")
                    ));

            insertEventWithSchedule(connection, "Business Case Study",
                    "Engineering commercialization and business model contest. Prize: BDT 60K.",
                    "Conference Hall A", baseDate.plusDays(2), "09:30", 40,
                    List.of(
                            slot("Case Presentation Round 1", "09:30", "12:30", "Case Analysts"),
                            slot("Final Presentation to Venture Panel", "13:30", "16:00", "Venture Capitalists")
                    ));

            insertEventWithSchedule(connection, "Robotics and IT Olympiad",
                    "Theoretical knowledge and problem-solving competition. Prize: BDT 16K.",
                    "Academic Block A Aud.", baseDate.plusDays(2), "10:00", 100,
                    List.of(
                            slot("Olympiad Written Exam", "10:00", "12:00", "Exam Controller"),
                            slot("Answer Key Discussion", "14:00", "15:30", "Faculty Panel")
                    ));

            insertEventWithSchedule(connection, "Project Showcasing",
                    "Exhibition of mechatronics, automation, and robotics innovations. Prize: BDT 32K.",
                    "Exhibition Center", baseDate.plusDays(3), "09:00", 60,
                    List.of(
                            slot("Project Demonstrations", "09:00", "12:00", "Technical Evaluators"),
                            slot("Jury Scoring & Interviews", "13:00", "15:00", "Academic Jury")
                    ));

            insertEventWithSchedule(connection, "Poster Presentation",
                    "Research and technical poster display. Prize: BDT 18K.",
                    "Gallery Hall", baseDate.plusDays(3), "10:00", 50,
                    List.of(
                            slot("Poster Display & Walkthrough", "10:00", "12:30", "Session Chairs"),
                            slot("Oral Defense & Evaluations", "13:30", "14:30", "Research Council")
                    ));

        } else if ("Ignition".equalsIgnoreCase(context)) {
            // Ignition 2026 (Department of Mechanical Engineering, KUET)
            insertEventWithSchedule(connection, "Line Follower Robot (LFR)",
                    "Precision robotics navigation competition. Prize: BDT 80K.",
                    "Mechanical Expo Ground", baseDate, "09:30", 50,
                    List.of(
                            slot("Qualifying Rounds", "09:30", "12:00", "Technical Stewards"),
                            slot("High-Speed Finals", "13:00", "15:30", "Head Referee")
                    ));

            insertEventWithSchedule(connection, "Soccer Bot",
                    "Thrilling robotic football tournament. Prize: BDT 90K.",
                    "Mechanical Expo Ground", baseDate, "10:00", 40,
                    List.of(
                            slot("Group Matches", "10:00", "12:30", "Referees"),
                            slot("Knockout Finals & Trophy", "13:30", "15:30", "ME Club President")
                    ));

            insertEventWithSchedule(connection, "Business Case Study",
                    "Engineering strategy and management case contest. Prize: BDT 100K.",
                    "ME Seminar Room", baseDate.plusDays(1), "09:00", 40,
                    List.of(
                            slot("Case Preparation Phase", "09:00", "12:00", "Case Facilitator"),
                            slot("Executive Board Presentations", "13:00", "16:00", "Corporate Panel")
                    ));

            insertEventWithSchedule(connection, "Ad-Making",
                    "Creative advertising showcasing engineering solutions. Prize: BDT 60K.",
                    "Audio-Visual Hall", baseDate.plusDays(1), "10:00", 40,
                    List.of(
                            slot("Ad Screenings", "10:00", "12:00", "Creative Panel"),
                            slot("Concept Defense", "13:00", "14:30", "Marketing Mentors")
                    ));

            insertEventWithSchedule(connection, "CAD Contest",
                    "Mechanical component modeling and drafting. Prize: BDT 60K.",
                    "Mechanical CAD Lab", baseDate.plusDays(1), "09:00", 50,
                    List.of(
                            slot("3D Part Modeling Sprint", "09:00", "12:00", "CAD Specialist"),
                            slot("FEA & Structural Feasibility Check", "13:00", "14:30", "Professor of ME")
                    ));

            insertEventWithSchedule(connection, "Project Showcasing",
                    "Innovative mechanical, energy, and automotive projects. Prize: BDT 50K.",
                    "Central Workshop Hall", baseDate.plusDays(2), "09:30", 60,
                    List.of(
                            slot("Live Prototype Demonstration", "09:30", "12:30", "Workshop Supervisors"),
                            slot("Jury Evaluation & Scoring", "13:30", "15:00", "Faculty Committee")
                    ));

            insertEventWithSchedule(connection, "Poster Presentation",
                    "Mechanical engineering research and innovative concepts. Prize: BDT 35K.",
                    "Workshop Gallery", baseDate.plusDays(2), "10:00", 50,
                    List.of(
                            slot("Poster Exhibition", "10:00", "12:00", "Session Judges"),
                            slot("Q&A and Defense", "13:00", "14:00", "Review Board")
                    ));

            insertEventWithSchedule(connection, "3 Minute Thesis",
                    "180-second rapid research presentation for engineering scholars. Prize: BDT 35K.",
                    "Main Auditorium", baseDate.plusDays(2), "10:30", 40,
                    List.of(
                            slot("Rapid Presentations Round", "10:30", "12:30", "Session Chairs"),
                            slot("Finalist Speeches & Awards", "13:30", "14:30", "Dean of ME")
                    ));

            insertEventWithSchedule(connection, "Mechanics Olympiad",
                    "Advanced classical mechanics and thermodynamics challenge. Prize: BDT 15K.",
                    "Lecture Theatre 1", baseDate.plusDays(3), "10:00", 80,
                    List.of(
                            slot("Written Exam Round", "10:00", "11:30", "Exam Invigilators"),
                            slot("Solution Walkthrough", "12:30", "13:30", "Academic Team")
                    ));

            insertEventWithSchedule(connection, "Automobile Olympiad",
                    "Automotive design, engines, and EV technology challenge. Prize: BDT 15K.",
                    "Lecture Theatre 2", baseDate.plusDays(3), "11:00", 80,
                    List.of(
                            slot("Automobile Challenge Paper", "11:00", "12:30", "SAE Coordinators"),
                            slot("Technical Discussion", "14:00", "15:00", "Automotive Engineers")
                    ));

            insertEventWithSchedule(connection, "Content Writing",
                    "Technical and scientific writing competition. Prize: BDT 10K.",
                    "ME Library Hall", baseDate.plusDays(3), "09:30", 50,
                    List.of(
                            slot("Article Writing Session", "09:30", "11:30", "Editorial Board"),
                            slot("Evaluation & Feedback", "12:30", "14:00", "Senior Faculty")
                    ));

            insertEventWithSchedule(connection, "Gaming Contest (FIFA, Valorant)",
                    "FIFA & Valorant competitive championship. Prize: BDT 50K.",
                    "E-Sports Gaming Lounge", baseDate.plusDays(3), "10:00", 64,
                    List.of(
                            slot("Knockout Stage", "10:00", "13:00", "Gaming Admins"),
                            slot("Championship Finals", "14:00", "16:30", "Shoutcasters")
                    ));
        } else {
            // Default fallback
            insertEventWithSchedule(connection, "Campus Tech Fest",
                    "Technology talks, demonstrations and teamwork.",
                    "Main Auditorium", baseDate, "09:00", 100,
                    List.of(slot("Opening Ceremony", "09:00", "10:00", "Dean")));
            insertEventWithSchedule(connection, "Programming Challenge",
                    "A friendly programming competition.",
                    "Computer Lab", baseDate.plusDays(7), "10:00", 40,
                    List.of(slot("Contest Round", "10:00", "13:00", "Lead Judge")));
        }
    }
}
