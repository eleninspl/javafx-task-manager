package com.example.controller.dialog;

import com.example.model.Priority;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;

public class PriorityDialog extends Dialog<Priority> {

    private TextField nameField; // Text field for priority name

    public PriorityDialog(Priority existingPri) {
        // Set dialog title and add OK/Cancel buttons
        setTitle("Priority Dialog");
        ButtonType okBtn = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(okBtn, cancelBtn);

        // Create grid layout for input
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        // Initialize the text field with prompt text
        nameField = new TextField();
        nameField.setPromptText("Priority Name");

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        getDialogPane().setContent(grid);

        // Pre-populate if editing an existing priority
        if (existingPri != null) {
            nameField.setText(existingPri.getName());
        }

        // Convert the result when OK is clicked; create a new Priority with the entered name
        setResultConverter(new Callback<ButtonType, Priority>() {
            @Override
            public Priority call(ButtonType b) {
                if (b == okBtn) {
                    return new Priority(nameField.getText().trim());
                }
                return null;
            }
        });
    }
}
