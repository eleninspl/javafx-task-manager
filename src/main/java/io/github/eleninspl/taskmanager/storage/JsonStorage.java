package io.github.eleninspl.taskmanager.storage;

import io.github.eleninspl.taskmanager.model.Category;
import io.github.eleninspl.taskmanager.model.Priority;
import io.github.eleninspl.taskmanager.model.Task;
import io.github.eleninspl.taskmanager.model.Reminder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Collections;

// Handles saving and loading application data in JSON format
public class JsonStorage {

    // System property that overrides the data directory (useful for testing and demos)
    public static final String DATA_DIR_PROPERTY = "medialab.dataDir";

    private final Path directory;
    private final Gson gson; // Gson instance for JSON conversion

    // Store data in the directory given by -Dmedialab.dataDir, or ~/medialab by default
    // (the assignment requires the data folder to be named "medialab")
    public JsonStorage() {
        this(defaultDirectory());
    }

    public JsonStorage(Path directory) {
        this.directory = directory;
        // Build a Gson instance with pretty printing and a custom LocalDate adapter
        gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(java.time.LocalDate.class, new LocalDateAdapter())
                .create();
    }

    private static Path defaultDirectory() {
        String override = System.getProperty(DATA_DIR_PROPERTY);
        if (override != null && !override.isBlank()) {
            return Paths.get(override);
        }
        return Paths.get(System.getProperty("user.home"), "medialab");
    }

    public Path getDirectory() {
        return directory;
    }

    // Save categories to file
    public void saveCategories(Collection<Category> categories) {
        saveToFile("categories.json", categories);
    }

    // Save priorities to file
    public void savePriorities(Collection<Priority> priorities) {
        saveToFile("priorities.json", priorities);
    }

    // Save tasks to file
    public void saveTasks(Collection<Task> tasks) {
        saveToFile("tasks.json", tasks);
    }

    // Save reminders to file
    public void saveReminders(Collection<Reminder> reminders) {
        saveToFile("reminders.json", reminders);
    }

    // Load categories from file
    public Collection<Category> loadCategories() {
        Type collectionType = new TypeToken<Collection<Category>>() {}.getType();
        return loadFromFile("categories.json", collectionType);
    }

    // Load priorities from file
    public Collection<Priority> loadPriorities() {
        Type collectionType = new TypeToken<Collection<Priority>>() {}.getType();
        return loadFromFile("priorities.json", collectionType);
    }

    // Load tasks from file
    public Collection<Task> loadTasks() {
        Type collectionType = new TypeToken<Collection<Task>>() {}.getType();
        return loadFromFile("tasks.json", collectionType);
    }

    // Load reminders from file
    public Collection<Reminder> loadReminders() {
        Type collectionType = new TypeToken<Collection<Reminder>>() {}.getType();
        return loadFromFile("reminders.json", collectionType);
    }

    // Write the JSON to a temporary file first, then move it into place,
    // so a crash mid-write never leaves a half-written data file behind
    private void saveToFile(String fileName, Object data) {
        Path target = directory.resolve(fileName);
        Path temp = directory.resolve(fileName + ".tmp");
        try {
            Files.createDirectories(directory);
            Files.writeString(temp, gson.toJson(data), StandardCharsets.UTF_8);
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Read JSON data from the specified file and convert it to the given type.
    // A file that cannot be parsed is set aside (renamed to *.corrupt) instead of
    // being silently overwritten by the next save.
    private <T> Collection<T> loadFromFile(String fileName, Type type) {
        Path file = directory.resolve(fileName);
        if (!Files.exists(file)) {
            return Collections.emptyList();
        }
        try {
            Collection<T> data = gson.fromJson(Files.readString(file, StandardCharsets.UTF_8), type);
            return data != null ? data : Collections.emptyList();
        } catch (JsonParseException e) {
            System.err.println("Could not parse " + file + ": " + e.getMessage());
            try {
                Files.move(file, directory.resolve(fileName + ".corrupt"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException moveError) {
                moveError.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }
}
