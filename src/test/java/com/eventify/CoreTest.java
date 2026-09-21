package com.eventify;

import com.eventify.Models.Event;
import com.eventify.Models.Holiday;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CoreTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void correctPasswordMatches() throws Exception {
        String stored = Passwords.hash("Participant@123");

        assertTrue(
                Passwords.matches("Participant@123", stored)
        );

        assertFalse(
                Passwords.matches("WrongPassword", stored)
        );
    }

    @Test
    void samePasswordGetsDifferentSalt() throws Exception {
        String first = Passwords.hash("Participant@123");
        String second = Passwords.hash("Participant@123");

        assertNotEquals(first, second);
    }

    @Test
    void parsesHolidayArray() {
        String json = """
                [
                  {
                    "date": "2026-12-25",
                    "localName": "Christmas Day",
                    "name": "Christmas Day",
                    "countryCode": "US"
                  },
                  {
                    "date": "2026-01-01",
                    "name": "New Year's Day",
                    "countryCode": "US"
                  }
                ]
                """;

        List<Holiday> holidays = JsonService.parseHolidays(json);

        assertEquals(2, holidays.size());
        assertEquals(
                LocalDate.of(2026, 1, 1),
                holidays.getFirst().date()
        );

        assertEquals(
                "New Year's Day",
                holidays.getFirst().localName()
        );
    }

    @Test
    void rejectsInvalidHolidayJson() {
        assertThrows(
                IllegalArgumentException.class,
                () -> JsonService.parseHolidays(
                        """
                        [{"name":"Missing date","countryCode":"US"}]
                        """
                )
        );
    }

    @Test
    void exportsEventsAsJson() throws Exception {
        Event event = new Event(
                1,
                "Tech Fest",
                "A sample event",
                "Auditorium",
                LocalDate.of(2026, 8, 20),
                "09:00",
                100
        );

        Path file = temporaryDirectory.resolve("events.json");

        JsonService.exportEvents(file, List.of(event));

        var array = JsonParser.parseString(
                Files.readString(file)
        ).getAsJsonArray();

        assertEquals(1, array.size());

        assertEquals(
                "Tech Fest",
                array.get(0).getAsJsonObject()
                        .get("title").getAsString()
        );

        assertEquals(
                "2026-08-20",
                array.get(0).getAsJsonObject()
                        .get("date").getAsString()
        );
    }
}
