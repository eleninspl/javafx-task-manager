package com.example.storage;

import com.example.model.Category;
import com.example.model.Priority;
import com.example.model.Task;
import com.example.model.Reminder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;

// Handles saving and loading application data in JSON format
public class JsonStorage {

    // Directory and file paths for JSON storage
    private static final String DIRECTORY = "medialab";
    private static final String CATEGORIES_FILE = DIRECTORY + "/categories.json";
    private static final String PRIORITIES_FILE = DIRECTORY + "/priorities.json";
    private static final String TASKS_FILE = DIRECTORY + "/tasks.json";
    private static final String REMINDERS_FILE = DIRECTORY + "/reminders.json";

    private Gson gson; // Gson instance for JSON conversion

    public JsonStorage() {
        // Build a Gson instance with pretty printing and a custom LocalDate adapter
        gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(java.time.LocalDate.class, new LocalDateAdapter())
                .create();
    }

    // Save categories to file
    public void saveCategories(Collection<Category> categories) {
        saveToFile(CATEGORIES_FILE, categories);
    }

    // Save priorities to file
    public void savePriorities(Collection<Priority> priorities) {
        saveToFile(PRIORITIES_FILE, priorities);
    }

    // Save tasks to file
    public void saveTasks(Collection<Task> tasks) {
        saveToFile(TASKS_FILE, tasks);
    }

    // Save reminders to file
    public void saveReminders(Collection<Reminder> reminders) {
        saveToFile(REMINDERS_FILE, reminders);
    }

    // Load categories from file
    public Collection<Category> loadCategories() {
        Type collectionType = new TypeToken<Collection<Category>>() {}.getType();
        return loadFromFile(CATEGORIES_FILE, collectionType);
    }

    // Load priorities from file
    public Collection<Priority> loadPriorities() {
        Type collectionType = new TypeToken<Collection<Priority>>() {}.getType();
        return loadFromFile(PRIORITIES_FILE, collectionType);
    }

    // Load tasks from file
    public Collection<Task> loadTasks() {
        Type collectionType = new TypeToken<Collection<Task>>() {}.getType();
        return loadFromFile(TASKS_FILE, collectionType);
    }

    // Load reminders from file
    public Collection<Reminder> loadReminders() {
        Type collectionType = new TypeToken<Collection<Reminder>>() {}.getType();
        return loadFromFile(REMINDERS_FILE, collectionType);
    }

    // Write the JSON representation of data to the specified file
    private void saveToFile(String filePath, Object data) {
        try {
            Files.createDirectories(Paths.get(DIRECTORY)); // Ensure directory exists
            String json = gson.toJson(data);               // Convert data to JSON
            Files.write(Paths.get(filePath), json.getBytes()); // Write JSON to file
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Read JSON data from the specified file and convert it to the given type
    @SuppressWarnings("unchecked")
    private <T> T loadFromFile(String filePath, Type type) {
        try {
            if (Files.exists(Paths.get(filePath))) {
                String json = new String(Files.readAllBytes(Paths.get(filePath)));
                return gson.fromJson(json, type);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return (T) Collections.emptyList(); // Return empty list if file doesn't exist or error occurs
    }
}
