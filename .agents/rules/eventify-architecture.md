---
trigger: Always On
---
# Eventify Project Architecture & Context

- **Stack**: Java 21, JavaFX 21.0.6, SQLite (embedded), Maven, Gson.
- **Design Patterns**: MVC, DAO, background tasks to prevent UI freezing.
- **Database**: `users`, `events`, `registrations`, `schedule`, `tasks`. Pragmas: `foreign_keys = ON`.
- **Key Constraints**: 
  - Leaderboard is dynamically calculated (Attendance=10pts, Tasks=custom pts).
  - Passwords are salted and hashed using PBKDF2WithHmacSHA256.
- **UI Guidelines**: Use modern dark-tint palette, FXML for layouts, and `Forms.java` for modal dialogs.
