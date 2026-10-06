package com.example.controller.dialog;

import com.example.model.Category;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;

public class CategoryDialog extends Dialog<Category> {

    private TextField nameField; // Text field for category name

    public CategoryDialog(Category existingCat) {
        // Set dialog title and add OK and Cancel buttons
        setTitle("Category Dialog");
        ButtonType okBtn = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(okBtn, cancelBtn);

        // Create grid layout for input fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        // Initialize the name text field with a prompt
        nameField = new TextField();
        nameField.setPromptText("Category Name");

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        getDialogPane().setContent(grid);

        // If editing, pre-fill the field with the existing category name
        if (existingCat != null) {
            nameField.setText(existingCat.getName());
        }

        // Convert the result when OK is clicked; create a new Category with the entered name
        setResultConverter(new Callback<ButtonType, Category>() {
            @Override
            public Category call(ButtonType b) {
                if (b == okBtn) {
                    return new Category(nameField.getText().trim());
                }
                return null;
            }
        });
    }
}
