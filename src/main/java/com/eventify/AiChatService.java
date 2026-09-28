package com.eventify;

import com.google.gson.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Service to communicate with the AI assistant strictly scoped
 * and guardrailed for the Eventify Festival & Event Platform.
 */
public final class AiChatService {

    private static final String AI_ENDPOINT = "https://text.pollinations.ai/";
    private static final Gson GSON = new GsonBuilder().create();

    public record ChatMessage(String role, String text) {}

    private static final List<ChatMessage> conversationHistory = new ArrayList<>();

    private AiChatService() {}

    public static boolean hasApiKey() {
        return true;
    }

    public static String getApiKey() {
        return "READY";
    }

    public static void setApiKey(String key) {
        // No-op: No key required!
    }

    public static List<ChatMessage> getHistory() {
        return new ArrayList<>(conversationHistory);
    }

    public static void clearHistory() {
        conversationHistory.clear();
    }

    /**
     * Creates the strict Eventify system prompt ensuring the AI exclusively assists with Eventify.
     */
    public static String buildSystemInstruction(String activeFestival, String activeSubEvent, String userRole) {
        return """
            You are Eventify AI, the exclusive intelligent assistant for the "Eventify" Festival and Event Management desktop platform developed for CSE-2200 Object-Oriented Programming at KUET.
            
            STRICT GUARDRAILS & SCOPE:
            1. You are strictly and exclusively an assistant for the Eventify platform.
            2. You ONLY answer questions related to Eventify, festival management, event coordination, KUET festivals (BitFest, Ignition, Calibration), scheduling, tasks, registrations, leaderboards, and Eventify troubleshooting.
            3. IF A USER ASKS ANYTHING UNRELATED TO EVENTIFY (such as general programming unrelated to this app, homework in other subjects, recipes, movies, politics, weather, math, general chit-chat, etc.):
               You MUST politely decline and reply ONLY with:
               "I am Eventify AI, an exclusive assistant for the Eventify Festival Management platform. I can only assist with Eventify events, registrations, schedules, tasks, leaderboards, and troubleshooting. How can I help you with Eventify?"
            
            EVENTIFY PLATFORM SPECIFICATIONS:
            - Target Domain: KUET Tech & Engineering Festivals:
              * BitFest 2025 (KUET CSE National Tech Festival — IUPC, Hackathon, Datathon, Gaming Contest, Line Following Robot, Soccer Bot, Project Showcase, IT Business Case).
              * Ignition 2026 (KUET Mechanical National Engineering Fest — Mechanical Olympiad, CAD Contest, Project Showcase).
              * Calibration 2.0 (KUET Mechatronics & Robotics Fest — Robotics, CAD, IoT, Tech Innovation).
            - User Roles:
              * Organizer (Admin): Full event CRUD, track registrations, toggle participant attendance (+10 pts), create schedules with overlap prevention, assign point-bearing tasks, view live calculated leaderboard, export JSON, fetch public holidays via Nager.Date API.
              * Participant: Browse events, self-register (before event date/time), cancel registration (cascades tasks), view schedule/agenda, complete/reopen own tasks, view live event leaderboard.
            - Leaderboard Scoring Rule:
              Total Score = (attended == 1 ? 10 : 0) + SUM(points of tasks marked done == 1).
              Equal scores share a dense rank.
            - Tech Stack: Java 21, JavaFX 21 (Cyberpunk & Neo-Tech Glow UI), SQLite embedded (ACID, foreign key cascades), Maven, PBKDF2 password hashing with salt.
            - Current Context:
              Active Festival: %s
              Active Sub-Event: %s
              Current User Role: %s
            
            FORMATTING & TONE:
            - Keep responses crisp, cyber-themed, polite, and well-structured with bullet points where appropriate.
            """.formatted(
                activeFestival != null ? activeFestival : "General",
                activeSubEvent != null ? activeSubEvent : "None selected",
                userRole != null ? userRole : "Participant"
            );
    }

    /**
     * Sends user query to AI service.
     * Falls back to offline intelligence if network is unavailable.
     */
    public static String sendMessage(String userQuery, String activeFestival, String activeSubEvent, String userRole) {
        conversationHistory.add(new ChatMessage("user", userQuery));

        try {
            String response = callAiEndpoint(activeFestival, activeSubEvent, userRole);
            if (response != null && !response.isBlank()) {
                conversationHistory.add(new ChatMessage("model", response));
                return response;
            }
        } catch (Exception e) {
            // Fallback to offline intelligence engine if network is unreachable
        }

        // Offline Fallback Engine
        String fallbackResponse = fallbackOfflineEngine(userQuery, activeFestival, activeSubEvent);
        conversationHistory.add(new ChatMessage("model", fallbackResponse));
        return fallbackResponse;
    }

    private static String callAiEndpoint(String activeFestival, String activeSubEvent, String userRole) throws Exception {
        JsonObject rootObj = new JsonObject();
        JsonArray messages = new JsonArray();

        // System Instruction
        JsonObject sysMsg = new JsonObject();
        sysMsg.addProperty("role", "system");
        sysMsg.addProperty("content", buildSystemInstruction(activeFestival, activeSubEvent, userRole));
        messages.add(sysMsg);

        // History
        for (ChatMessage msg : conversationHistory) {
            JsonObject obj = new JsonObject();
            obj.addProperty("role", "model".equals(msg.role()) ? "assistant" : "user");
            obj.addProperty("content", msg.text());
            messages.add(obj);
        }

        rootObj.add("messages", messages);
        String payload = GSON.toJson(rootObj);

        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build()) {

            HttpRequest request = HttpRequest.newBuilder(URI.create(AI_ENDPOINT))
                    .timeout(Duration.ofSeconds(12))
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/plain, application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() == 200 && response.body() != null && !response.body().isBlank()) {
                return response.body().trim();
            }
        }
        return null;
    }

    /**
     * Context-aware offline knowledge engine for zero-network environments.
     */
    private static String fallbackOfflineEngine(String query, String activeFestival, String activeSubEvent) {
        String lower = query.toLowerCase();

        boolean isRelevant = lower.contains("event") || lower.contains("fest") || lower.contains("kuet")
                || lower.contains("bitfest") || lower.contains("ignition") || lower.contains("calibration")
                || lower.contains("login") || lower.contains("sign") || lower.contains("account") || lower.contains("password")
                || lower.contains("user") || lower.contains("admin") || lower.contains("organizer") || lower.contains("participant")
                || lower.contains("score") || lower.contains("point") || lower.contains("rank") || lower.contains("leaderboard")
                || lower.contains("schedule") || lower.contains("session") || lower.contains("time") || lower.contains("timeline")
                || lower.contains("task") || lower.contains("register") || lower.contains("registration") || lower.contains("attend")
                || lower.contains("venue") || lower.contains("capacity") || lower.contains("export") || lower.contains("json")
                || lower.contains("help") || lower.contains("trouble") || lower.contains("problem") || lower.contains("bug")
                || lower.contains("hi") || lower.contains("hello") || lower.contains("hey") || lower.contains("eventify");

        if (!isRelevant) {
            return "I am Eventify AI, an exclusive assistant for the Eventify Festival Management platform. I can only assist with Eventify events, registrations, schedules, tasks, leaderboards, and troubleshooting. How can I help you with Eventify?";
        }

        if (lower.contains("login") || lower.contains("sign in") || lower.contains("account") || lower.contains("password")) {
            return getQuickFaqAnswer("LOGIN");
        }
        if (lower.contains("create") || lower.contains("add event") || lower.contains("edit event")) {
            return getQuickFaqAnswer("EVENT_CREATION");
        }
        if (lower.contains("score") || lower.contains("point") || lower.contains("leaderboard") || lower.contains("rank")) {
            return getQuickFaqAnswer("LEADERBOARD");
        }
        if (lower.contains("schedule") || lower.contains("time") || lower.contains("timeline") || lower.contains("overlap")) {
            return getQuickFaqAnswer("SCHEDULE");
        }
        if (lower.contains("bitfest")) {
            return """
                🚀 **BitFest 2025 (KUET CSE National Tech Festival)**
                
                • **Key Competitions**:
                  1. Inter-University Programming Contest (IUPC)
                  2. Hackathon & Datathon
                  3. Line Following Robot & Soccer Bot
                  4. Gaming Contest & IT Business Case
                  5. Project & Innovation Showcase
                • **How to join**: Click BitFest on the Home screen, log in as Participant, and click 'Register'!
                """;
        }
        if (lower.contains("calibration")) {
            return """
                ⚙️ **Calibration 2.0 (KUET Mechatronics & Robotics Fest)**
                
                • Focuses on robotics, CAD design, IoT innovation, and precision engineering.
                • Managed with dedicated real-time schedule tracking and team tasks.
                """;
        }
        if (lower.contains("ignition")) {
            return """
                🔥 **Ignition 2026 (KUET Mechanical National Engineering Fest)**
                
                • Features Mechanical Olympiad, CAD Modeling, and Engineering Innovation Showcase.
                • Supports role-based participant coordination and dynamic scoreboards.
                """;
        }

        // Generic Eventify answer
        return """
            ⚡ **Eventify AI Assistant**:
            I am here to assist you with the Eventify platform!
            
            • **Active Festival**: %s
            • **Sub-Event**: %s
            
            You can ask me about:
            - Competitions and sub-events for BitFest, Ignition, or Calibration
            - Participant self-registration and attendance tracking (+10 pts)
            - Schedule timeline rules and overlap checks
            - Task assignments and live leaderboard dense ranking!
            """.formatted(activeFestival != null ? activeFestival : "General", activeSubEvent != null ? activeSubEvent : "All");
    }

    /**
     * Built-in instant answers for common FAQs and troubleshooting questions.
     */
    public static String getQuickFaqAnswer(String topic) {
        return switch (topic) {
            case "LOGIN" -> """
                🔑 **Facing Problems with Login or Account Creation?**
                
                • **Default Organizer Account**:
                  - Email: `admin@eventify.com`
                  - Password: `Admin@12345`
                • **Pre-seeded Participant Accounts**:
                  - `alice@example.com` | Password: `Password@123`
                  - `bob@example.com`   | Password: `Password@123`
                  - `charlie@example.com`
                • **Self-Registration Rules**:
                  - Password must have at least 8 characters and include both letters and digits.
                  - Email must contain valid format (`user@domain.com`).
                • **Database Storage**:
                  - SQLite files are stored locally in your profile: `~/.eventify/`.
                """;

            case "EVENT_CREATION" -> """
                ➕ **Facing Problems Creating or Editing an Event?**
                
                • **Organizer Role Required**:
                  - Only users logged in as **Organizer (Admin)** have permission to create, edit, or delete events.
                • **Input Constraints**:
                  - **Date Format**: Must strictly be `YYYY-MM-DD` (e.g. `2026-10-15`).
                  - **Time Format**: Must be in 24-hour format `HH:mm` (e.g. `09:30`).
                  - **Capacity**: Must be an integer greater than 0.
                  - **Venue & Title**: Cannot be empty.
                """;

            case "LEADERBOARD" -> """
                🏆 **How is the Event Leaderboard Calculated?**
                
                • **Official Formula**:
                  `Total Score = (Attended == 1 ? 10 : 0) + SUM(Completed Task Points)`
                • **Attendance**:
                  - When the Organizer marks attendance for an attendee in the Registrations tab, **+10 points** are automatically added.
                • **Tasks**:
                  - Each task completed by the participant contributes its designated points (e.g., +15, +20).
                • **Ranking Logic**:
                  - Dense ranking is applied. Participants with equal total points share the same rank!
                """;

            case "SCHEDULE" -> """
                ⏱ **Schedule Conflicts & Session Timeline Rules**
                
                • **Overlap Prevention**:
                  - No two sessions in the same sub-event can overlap in time `[start_time, end_time)`.
                • **Chronological Requirement**:
                  - Session start time cannot be earlier than the sub-event start time.
                  - End time must be strictly after the start time (`HH:mm`).
                """;

            default -> "I am Eventify AI. How can I assist you with your festival operations today?";
        };
    }
}
