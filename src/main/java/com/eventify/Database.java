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

    public static final Path FILE = Path.of(
            System.getProperty("user.home"),
            ".eventify",
            "eventify.db"
    );

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

            boolean firstRun = count(
                    connection,
                    "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'"
            ) == 0;

            if (firstRun) {
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

                    update(
                            connection,
                            """
                            INSERT INTO events
                                (title, description, venue,
                                 event_date, event_time, capacity)
                            VALUES (?, ?, ?, ?, ?, ?)
                            """,
                            "Campus Tech Fest",
                            "Technology talks, demonstrations and teamwork.",
                            "Main Auditorium",
                            LocalDate.now().plusDays(7).toString(),
                            "09:00",
                            100
                    );

                    update(
                            connection,
                            """
                            INSERT INTO events
                                (title, description, venue,
                                 event_date, event_time, capacity)
                            VALUES (?, ?, ?, ?, ?, ?)
                            """,
                            "Programming Challenge",
                            "A friendly programming competition.",
                            "Computer Lab",
                            LocalDate.now().plusDays(14).toString(),
                            "10:00",
                            40
                    );

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
}
