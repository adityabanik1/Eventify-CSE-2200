# 📖 Eventify — Quick Operation Manual

> **Plan. Coordinate. Celebrate.**  
> A desktop event management system built with JavaFX, SQLite, and Java 21.

---

## 🔑 1. Organizer (Admin) Portal

### 🔐 Login Credentials
| Field | Value |
| :--- | :--- |
| **Email** | `admin@eventify.com` |
| **Password** | `Admin@12345` |

### 🚀 How to Access Admin Panel
1. Launch the application (run `Launcher.java` or `mvn javafx:run`).
2. On the **Sign in** tab, enter the credentials above and click **Sign in**.
3. The header will display: `Eventify Organizer | Organizer`.

### 🛠️ Organizer Features & How to Use
* **Dashboard Tab:** View overall workspace statistics (total events, upcoming events, registrations, pending tasks).
* **Events Tab:**
  * **Create Event:** Click `Create event` ➔ Fill Title, Venue, Date, Time (HH:mm), and Capacity ➔ `OK`.
  * **Edit / Delete:** Select an event row ➔ Click `Edit selected` or `Delete selected`.
* **Registrations Tab:**
  * View all registered attendees across events.
  * **Toggle Attendance:** Select a participant ➔ Click `Toggle attendance` to award **10 attendance points**.
* **Schedule Tab:**
  * **Add Session:** Click `Add session` ➔ Set Start/End times (24-hr format) and Speaker ➔ Overlap detection is automatic.
* **Tasks Tab:**
  * **Assign Task:** Click `Assign task` ➔ Pick a registered attendee ➔ Set title, due date, and point value (e.g. 20 pts).
  * **Edit / Delete:** Modify or remove assigned tasks.
* **Leaderboard Tab:**
  * Dynamically calculated live rankings (`Total = 10 pts Attendance + Task Points`). Ties automatically share ranks.
* **Planning & JSON Tab:**
  * **Fetch Holidays:** Enter Country code (e.g., `US`, `GB`) and Year ➔ Click `Fetch holidays` to check for scheduling conflicts.
  * **Load Holiday JSON:** Load offline holiday data from `sample-holidays.json`.
  * **Export Events:** Export the current event list to a `.json` file.

---

## 👤 2. Participant Portal

### 📝 How to Create an Account
1. On the authentication screen, click the **Create participant account** tab.
2. Enter:
   * **Full Name** (e.g., `Ayesha Rahman`)
   * **Email Address** (e.g., `ayesha@example.com`)
   * **Password** (minimum 8 characters)
   * **Confirm Password**
3. Click **Create account**.
4. Switch back to the **Sign in** tab and log in with your new email and password.

### 🎯 Participant Features & How to Use
* **Dashboard Tab:** View your personal overview and upcoming schedules for registered events.
* **Events Tab:** Browse available events, check dates, venues, and remaining capacities.
* **Registrations Tab:**
  * **Register:** Select an event from the top dropdown ➔ Click `Register for selected event`.
  * **Cancel:** Click `Cancel registration` to drop out (removes any tasks assigned to you for that event).
  * *Note: Only your own registration is visible to you.*
* **Schedule Tab:** View all agenda sessions and speakers for the selected event.
* **Tasks Tab:**
  * View tasks specifically assigned to you by the organizer.
  * **Complete / Reopen:** Select a task ➔ Click `Complete / reopen selected` to toggle its status and update your score on the leaderboard.
* **Leaderboard Tab:** Check live scores and rankings for the event.

---

## ⚡ 3. Quick 5-Step Demo Workflow

```
[1. Admin] Create Event ➔ [2. Admin] Add Schedule ➔ [3. Participant] Sign up & Register
                              │
[5. Leaderboard Updated] ◄────┴── [4. Admin] Mark Attendance (+10 pts) & Assign Task (+pts)
                                  [4. Participant] Complete Task
```

1. **Log in as Admin** (`admin@eventify.com` / `Admin@12345`).
2. Go to **Events** ➔ Click `Create event` ➔ Name it (e.g., "Tech Fest 2026").
3. Click **Sign out** ➔ Go to **Create participant account** ➔ Register a test participant and log in.
4. Go to **Registrations** ➔ Click `Register for selected event`.
5. **Sign back in as Admin**:
   * Go to **Registrations** ➔ Select the participant ➔ Click `Toggle attendance` (+10 pts).
   * Go to **Tasks** ➔ Click `Assign task` ➔ Assign 20 points.
6. **Sign in as Participant**:
   * Go to **Tasks** ➔ Select task ➔ Click `Complete / reopen selected`.
7. **Check Leaderboard**:
   * Open **Leaderboard** ➔ The participant now has **30 points** (10 attendance + 20 task points) and Rank #1!
