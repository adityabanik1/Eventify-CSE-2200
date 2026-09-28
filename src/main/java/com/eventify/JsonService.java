package com.eventify;

import com.eventify.Models.Event;
import com.eventify.Models.Holiday;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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

    private static final ObjectMapper MAPPER = new ObjectMapper();

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
            JsonNode root = MAPPER.readTree(json);

            if (!root.isArray()) {
                throw new IllegalArgumentException(
                        "Expected a JSON array of holidays."
                );
            }

            List<Holiday> holidays = new ArrayList<>();

            for (JsonNode element : root) {
                LocalDate date = LocalDate.parse(
                        requiredString(element, "date")
                );

                String name = requiredString(element, "name");
                String country = requiredString(element, "countryCode");

                String localName = element.has("localName")
                        && !element.get("localName").isNull()
                        ? element.get("localName").asText()
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
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid holiday JSON. Each item needs date, name "
                            + "and countryCode; date must use yyyy-MM-dd.",
                    e
            );
        }
    }

    private static String requiredString(
            JsonNode object,
            String field
    ) {
        if (!object.has(field) || object.get(field).isNull()) {
            throw new IllegalArgumentException(
                    "Missing JSON field: " + field
            );
        }

        String result = object.get(field).asText();

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

        ArrayNode array = MAPPER.createArrayNode();

        for (Exportable exportable : events) {
            array.add(exportable.toJsonNode(MAPPER));
        }

        Files.writeString(
                file,
                MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(array),
                StandardCharsets.UTF_8
        );
    }
}
