package com.eventify;

import java.time.LocalDate;
import java.util.List;

public final class Models {

    private Models() {
    }

    public record User(
            int id,
            String name,
            String email,
            String role
    ) {
        public boolean isAdmin() {
            return "ADMIN".equals(role);
        }
    }

    public record Event(
            int id,
            String title,
            String description,
            String venue,
            LocalDate date,
            String time,
            int capacity
    ) {
        @Override
        public String toString() {
            return title + " — " + date;
        }
    }

    public record EventDraft(
            String title,
            String description,
            String venue,
            LocalDate date,
            String time,
            int capacity
    ) {
    }

    public record Registration(
            int userId,
            String name,
            String email,
            boolean attended
    ) {
        @Override
        public String toString() {
            return name + " (" + email + ")";
        }
    }

    public record Slot(
            int id,
            String title,
            String start,
            String end,
            String speaker
    ) {
    }

    public record SlotDraft(
            String title,
            String start,
            String end,
            String speaker
    ) {
    }

    public record Job(
            int id,
            String title,
            int assigneeId,
            String assigneeName,
            LocalDate due,
            int points,
            boolean done
    ) {
    }

    public record JobDraft(
            String title,
            int assigneeId,
            LocalDate due,
            int points
    ) {
    }

    public record Score(
            int rank,
            String name,
            int attendancePoints,
            int taskPoints,
            int total
    ) {
    }

    public record Holiday(
            LocalDate date,
            String localName,
            String name,
            String countryCode
    ) {
    }

    public record Snapshot(
            List<Event> events,
            Event activeEvent,
            List<Registration> registrations,
            List<Slot> slots,
            List<Job> jobs,
            List<Score> scores,
            int totalRegistrations,
            int pendingTasks,
            List<String> upcoming
    ) {
    }
}
