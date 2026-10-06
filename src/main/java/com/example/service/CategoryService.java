package com.example.service;

import com.example.model.Category;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.Collection;
import java.util.Optional;

public class CategoryService {

    // Observable list to hold all categories
    private final ObservableList<Category> categories = FXCollections.observableArrayList();
    // Constant for the default category name
    private final String NO_CATEGORY_NAME = "No Category";
    // The default category instance
    private final Category noCategory;

    public CategoryService() {
        // Create the default "No Category" item and add it to the list
        noCategory = new Category(NO_CATEGORY_NAME);
        categories.add(noCategory);
    }

    // Return the full observable list of categories
    public ObservableList<Category> getCategories() {
        return categories;
    }

    // Return only categories used for management (excluding the default)
    public ObservableList<Category> getCategoriesForManagement() {
        return categories.filtered(cat -> !cat.getName().equals(NO_CATEGORY_NAME));
    }

    // Load categories from a given collection, preserving the default
    public void setCategories(Collection<Category> loadedCategories) {
        categories.clear();
        categories.add(noCategory);
        for (Category c : loadedCategories) {
            if (c == null) continue;
            if (c.getName() == null || c.getName().trim().isEmpty()) continue;
            if (c.getName().equals(NO_CATEGORY_NAME)) continue;
            categories.add(c);
        }
    }

    // Add a new category; if the name is the default, return the default category
    public Category addCategory(String name) {
        if (name.equals(NO_CATEGORY_NAME)) {
            return noCategory;
        }
        Category category = new Category(name);
        categories.add(category);
        return category;
    }

    // Update the name of an existing category and inform TaskService to update its tasks
    public boolean updateCategory(String categoryId, String newName, TaskService taskService) {
        Optional<Category> opt = categories.stream()
                .filter(c -> c.getId().equals(categoryId))
                .findFirst();
        if (opt.isPresent()) {
            Category cat = opt.get();
            // Do not update the default category
            if (cat.getName().equals(NO_CATEGORY_NAME)) return false;
            cat.setName(newName);
            int index = categories.indexOf(cat);
            if (index != -1) {
                // Replace the category in the list to trigger UI updates
                categories.set(index, cat);
            }
            // Let TaskService update tasks associated with this category
            taskService.updateTasksForCategory(categoryId);
            return true;
        }
        return false;
    }

    // Delete a category and delete its tasks via TaskService
    public Category deleteCategory(String categoryId, TaskService taskService) {
        Optional<Category> opt = categories.stream()
                .filter(c -> c.getId().equals(categoryId))
                .findFirst();
        if (opt.isPresent()) {
            Category toRemove = opt.get();
            // Do not delete the default category
            if (toRemove.getName().equals(NO_CATEGORY_NAME)) return null;
            categories.remove(toRemove);
            taskService.deleteTasksByCategory(categoryId);
            return toRemove;
        }
        return null;
    }

    // Get a category by its ID
    public Category getCategoryById(String id) {
        return categories.stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    // Return the default "No Category" object
    public Category getNoCategory() {
        return noCategory;
    }
}
