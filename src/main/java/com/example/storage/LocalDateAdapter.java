package com.example.storage;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

// Custom adapter for converting LocalDate objects to/from JSON
public class LocalDateAdapter extends TypeAdapter<LocalDate> {
    // Formatter for ISO local date format
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    public void write(JsonWriter out, LocalDate value) throws IOException {
        if (value == null) {
            out.nullValue(); // Write JSON null if value is null
            return;
        }
        // Write the LocalDate as a formatted string
        out.value(value.format(formatter));
    }

    @Override
    public LocalDate read(JsonReader in) throws IOException {
        // Read the next string and parse it into a LocalDate
        String s = in.nextString();
        return LocalDate.parse(s, formatter);
    }
}
