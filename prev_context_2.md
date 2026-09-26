# Eventify - KUET Festival Management App (Context Handover)

## 🎯 Purpose and Overview
**Eventify** is a comprehensive JavaFX-based desktop application designed to manage the national-level tech festivals hosted by the CSE Department of KUET. The system supports multiple major festivals—specifically **BitFest**, **Ignition**, and **Calibration**—under a single unified platform. 

The primary goal is to provide a seamless, scalable, and visually immersive experience for managing event schedules, registrations, participants, and specific contest segments.

## 🏗️ Architecture & Tech Stack
- **Language**: Java 21
- **UI Framework**: JavaFX 21
- **Build Tool**: Maven (Bundled with IntelliJ IDEA)
- **Database**: SQLite (JDBC)
- **Design Pattern**: MVC (Model-View-Controller)

### Data Isolation
To keep data clean and segregated across different festivals, the app employs isolated SQLite databases:
- `BitFest.db`
- `Calibration.db`
- `Ignition.db`
- `General.db` (For shared/global data)
The active database is dynamically resolved by `Database.java` based on the current context.

### Context Switching
The app maintains state using a global context manager (`App.java`):
- `App.setActiveMainEvent(String eventName)` sets the current festival.
- `App.getActiveMainEvent()` retrieves it.
Controllers use this to dynamically adjust logic, queries, and UI themes.

## ✅ Updates & Progress Shared from Previous Session

### 1. UI/UX Glassmorphic Overhaul (Dark Theme)
Inspired by the "Eventora" dark UI reference, the application has been completely redesigned:
- **Glassmorphism**: Solid backgrounds on cards, tables, sidebars, and text areas were replaced with translucent dark glass (`rgba(17,24,39,0.78)`).
- **Dynamic Theming**: Added `.theme-bitfest`, `.theme-calibration`, and `.theme-ignition` CSS classes. The root `BorderPane` dynamically loads the appropriate theme, altering accent colors, glows, and hover effects depending on the active festival.
- **Dynamic Text/Branding**: Hero banners, sidebar text, and footer mottos automatically update to reflect the specific festival selected.

### 2. Authentic Transparent Watermarks
- A custom Java utility (`ProcessImages.java`) was executed to strip the opaque backgrounds (black, white, navy) from `ignition.png`, `calibration.png`, and `bitfest.png`.
- The `calibration.png` text was adjusted to silver-white for optimal dark-mode visibility.
- A centralized watermark (`620x620`, opacity `0.06`) is now embedded in `main.fxml` beneath the UI layer. Because the UI is glassmorphic, the active event's logo shines elegantly through the entire dashboard.

### 3. Build & Bug Fixes
- Resolved generic type inference errors (`java.lang.String[]` vs `String`).
- Recompiled and tested successfully using IntelliJ's bundled Maven.

## 🚀 Targets to Achieve & Necessary Directions (For the New Chat)

### 1. Implement BitFest 2025 Modules
The user has provided the schedule and event list for the **3rd KUET CSE National Festival (BitFest 2025)**. The following competition modules need to be integrated:
1. Inter University Programming Contest (IUPC)
2. Hackathon
3. Datathon
4. Gaming Contest
5. Line Following Robot Competition
6. Soccer Bot
7. Project Showcase
8. IT Business

**Action Item**: Create specific UI views, data models, and database tables (in `BitFest.db`) to handle registrations, scheduling, and scoring for these 8 segments.

### 2. GUI Responsiveness & Scrolling Fix
- **Known Issue**: When the application GUI is not in full-screen mode (e.g., half-screen or resized), mouse scrolling fails to work on certain Panes/ScrollPanes.
- **Action Item**: Investigate and fix the `ScrollPane` fit-to-width/height properties and ensure all nested tables and lists are responsive and scrollable regardless of the window dimensions.

### 3. Event Scheduling and Roster
- The user is preparing to provide a list of events and schedules. 
- **Action Item**: Develop a dynamic `ScheduleView` that reads from the active database and displays a timeline or calendar view of the events.

## 🛠️ Instructions for the Agent
1. **Always read the context**: Use `App.getActiveMainEvent()` to ensure you are modifying the correct database and UI state.
2. **Maintain the Glassmorphic Theme**: Any new components (cards, lists, modals) must follow the `rgba` background transparency established in `style.css` so the watermark remains visible.
3. **Maven Commands**: Standard `mvn` might not be on the Windows PATH. If you need to compile, use the absolute path to IntelliJ's bundled Maven: 
   `"C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.3\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" compile`
4. **Test UI Changes**: Ensure that any new layout handles window resizing gracefully and that scrollbars appear as needed.
