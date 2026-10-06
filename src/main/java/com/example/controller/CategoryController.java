package com.example.controller;

import com.example.model.Category;
import com.example.service.CategoryService;
import com.example.service.TaskService;
import com.example.App;
import com.example.controller.dialog.CategoryDialog;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import java.util.Optional;

public class CategoryController {

    private CategoryService categoryService;  
    private TaskService taskService;          
    private App app;                         
    private BorderPane view;                  
    private ListView<Category> categoryList;  
    private Button newBtn, editBtn, deleteBtn;  

    public CategoryController(CategoryService categoryService, TaskService taskService, App app) {
        this.categoryService = categoryService;
        this.taskService = taskService;
        this.app = app;
        createView();
    }

    public BorderPane getView() {
        return view;
    }

    // Build the main view for category management
    private void createView() {
        view = new BorderPane();
        view.setPadding(new Insets(10));

        // Create list view with categories for management (excluding default)
        categoryList = new ListView<>(categoryService.getCategoriesForManagement());
        view.setCenter(categoryList);

        // Create buttons for new, edit, and delete
        newBtn = new Button("New Category");
        editBtn = new Button("Edit Category");
        deleteBtn = new Button("Delete Category");

        newBtn.setOnAction(e -> openCategoryDialog(null));
        editBtn.setOnAction(e -> handleEditCategory());
        deleteBtn.setOnAction(e -> handleDeleteCategory());

        HBox buttonBox = new HBox(10, newBtn, editBtn, deleteBtn);
        buttonBox.setPadding(new Insets(10));
        view.setBottom(buttonBox);
    }

    // Handle editing a category
    private void handleEditCategory() {
        Category selected = getSelectedCategory();
        if (selected == null) {
            showInfoAlert("Please select a category to edit.");
            return;
        }
        openCategoryDialog(selected);
    }

    // Handle deleting a category
    private void handleDeleteCategory() {
        Category selected = getSelectedCategory();
        if (selected == null) {
            showInfoAlert("Please select a category to delete.");
            return;
        }
        if (confirmAction("Warning: Delete Category",
                "Deleting this category will remove all its tasks.",
                "Do you really want to delete this category?")) {
            categoryService.deleteCategory(selected.getId(), taskService);
            categoryList.refresh();
            app.updateSummary();
        }
    }

    // Returns the currently selected category (or null if none)
    private Category getSelectedCategory() {
        return categoryList.getSelectionModel().getSelectedItem();
    }

    // Open dialog for creating or editing a category
    private void openCategoryDialog(Category selected) {
        CategoryDialog dialog = new CategoryDialog(selected);
        dialog.setTitle(selected == null ? "New Category" : "Edit Category");
        dialog.showAndWait().ifPresent(resultCat -> {
            if (selected == null) {
                categoryService.addCategory(resultCat.getName());
            } else {
                if (confirmAction("Warning: Rename Category",
                        "Renaming this category will update its tasks.",
                        "Do you want to continue?")) {
                    categoryService.updateCategory(selected.getId(), resultCat.getName(), taskService);
                }
            }
            categoryList.refresh();
            app.updateSummary();
        });
    }

    // Helper: Show an informational alert with a message
    private void showInfoAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.showAndWait();
    }

    // Helper: Show a confirmation alert and return true if the user confirms
    private boolean confirmAction(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}
