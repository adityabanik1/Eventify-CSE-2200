package com.eventify;

import com.eventify.Models.Event;
import com.eventify.Models.Holiday;
import com.google.gson.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class JsonService {

    private static final int MAX_JSON_CHARACTERS = 2_000_000;

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private JsonService() {
    }

    public static List<Holiday> fetchHolidays(
            String suppliedCountry,
            String suppliedYear
    ) throws Exception {

        String country = suppliedCountry.trim()
                .toUpperCase(Locale.ROOT);

        if (!country.matches("[A-Z]{2}")) {
            throw new IllegalArgumentException(
                    "Country must be a two-letter code, such as US or GB."
            );
        }

        int year;

        try {
            year = Integer.parseInt(suppliedYear.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Enter a valid year.");
        }

        if (year < 1900 || year > 2100) {
            throw new IllegalArgumentException(
                    "Year must be between 1900 and 2100."
            );
        }

        URI uri = URI.create(
                "https://date.nager.at/api/v3/PublicHolidays/"
                        + year + "/" + country
        );

        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build()) {

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(
                            StandardCharsets.UTF_8
                    )
            );

            if (response.statusCode() != 200) {
                throw new IllegalArgumentException(
                        "Holiday API returned HTTP "
                                + response.statusCode()
                                + ". Check the country code or try again later."
                );
            }

            return parseHolidays(response.body());
        }
    }

    public static List<Holiday> readHolidays(Path file)
            throws Exception {

        if (Files.size(file) > 2_000_000) {
            throw new IllegalArgumentException(
                    "Choose a JSON file smaller than 2 MB."
            );
        }

        return parseHolidays(
                Files.readString(file, StandardCharsets.UTF_8)
        );
    }

    public static List<Holiday> parseHolidays(String json) {
        if (json == null || json.length() > MAX_JSON_CHARACTERS) {
            throw new IllegalArgumentException(
                    "The JSON response is missing or too large."
            );
        }

        try {
            JsonElement root = JsonParser.parseString(json);

            if (!root.isJsonArray()) {
                throw new IllegalArgumentException(
                        "Expected a JSON array of holidays."
                );
            }

            List<Holiday> holidays = new ArrayList<>();

            for (JsonElement element : root.getAsJsonArray()) {
                JsonObject object = element.getAsJsonObject();

                LocalDate date = LocalDate.parse(
                        requiredString(object, "date")
                );

                String name = requiredString(object, "name");
                String country = requiredString(object, "countryCode");

                String localName = object.has("localName")
                        && !object.get("localName").isJsonNull()
                        ? object.get("localName").getAsString()
                        : name;

                holidays.add(new Holiday(
                        date,
                        localName,
                        name,
                        country
                ));
            }

            holidays.sort(Comparator.comparing(Holiday::date));

            return holidays;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(
                    "Invalid holiday JSON. Each item needs date, name "
                            + "and countryCode; date must use yyyy-MM-dd.",
                    e
            );
        }
    }

    private static String requiredString(
            JsonObject object,
            String field
    ) {
        if (!object.has(field) || object.get(field).isJsonNull()) {
            throw new IllegalArgumentException(
                    "Missing JSON field: " + field
            );
        }

        String result = object.get(field).getAsString();

        if (result.isBlank()) {
            throw new IllegalArgumentException(
                    "Empty JSON field: " + field
            );
        }

        return result;
    }

    public static void exportEvents(
            Path file,
            List<Event> events
    ) throws Exception {

        JsonArray array = new JsonArray();

        for (Event event : events) {
            JsonObject object = new JsonObject();

            object.addProperty("id", event.id());
            object.addProperty("title", event.title());
            object.addProperty("description", event.description());
            object.addProperty("venue", event.venue());
            object.addProperty("date", event.date().toString());
            object.addProperty("time", event.time());
            object.addProperty("capacity", event.capacity());

            array.add(object);
        }

        Files.writeString(
                file,
                GSON.toJson(array),
                StandardCharsets.UTF_8
        );
    }
}
