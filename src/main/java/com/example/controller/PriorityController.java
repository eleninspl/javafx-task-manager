package com.example.controller;

import com.example.model.Priority;
import com.example.service.PriorityService;
import com.example.service.TaskService;
import com.example.App;
import com.example.controller.dialog.PriorityDialog;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import java.util.Optional;

public class PriorityController {

    private PriorityService priorityService;  // Manages priorities
    private TaskService taskService;            // Used to update tasks using priorities
    private App app;                            // Reference to main app
    private BorderPane view;                    // Main UI container
    private ListView<Priority> priorityList;    // List view for priorities
    private Button newBtn, editBtn, deleteBtn;    // Buttons for operations

    public PriorityController(PriorityService priorityService, TaskService taskService, App app) {
        this.priorityService = priorityService;
        this.taskService = taskService;
        this.app = app;
        createView();
    }

    public BorderPane getView() {
        return view;
    }

    // Build the UI for priority management
    private void createView() {
        view = new BorderPane();
        view.setPadding(new Insets(10));

        priorityList = new ListView<>(priorityService.getPriorities());
        view.setCenter(priorityList);

        newBtn = new Button("New Priority");
        editBtn = new Button("Edit Priority");
        deleteBtn = new Button("Delete Priority");

        newBtn.setOnAction(e -> openPriorityDialog(null));
        editBtn.setOnAction(e -> handleEditPriority());
        deleteBtn.setOnAction(e -> handleDeletePriority());

        HBox buttonBox = new HBox(10, newBtn, editBtn, deleteBtn);
        buttonBox.setPadding(new Insets(10));
        view.setBottom(buttonBox);
    }

    // Handle editing a priority with selection and default check
    private void handleEditPriority() {
        Priority selected = getSelectedPriority();
        if (selected == null) {
            showInfoAlert("Please select a priority to edit.");
            return;
        }
        if (isDefaultPriority(selected)) {
            showInfoAlert("The Default priority cannot be renamed.");
            return;
        }
        openPriorityDialog(selected);
    }

    // Handle deleting a priority with selection and default check
    private void handleDeletePriority() {
        Priority selected = getSelectedPriority();
        if (selected == null) {
            showInfoAlert("Please select a priority to delete.");
            return;
        }
        if (isDefaultPriority(selected)) {
            showInfoAlert("The Default priority cannot be deleted.");
            return;
        }
        if (confirmAction("Warning: Delete Priority", "Deleting this priority will reassign its tasks to 'Default'.",
                "Do you really want to delete this priority?")) {
            Priority deleted = priorityService.deletePriority(selected.getId(), taskService);
            if (deleted == null) {
                showInfoAlert("Cannot delete the Default priority.");
            }
            priorityList.refresh();
            app.updateSummary();
        }
    }

    // Opens the PriorityDialog for creating or editing a priority
    private void openPriorityDialog(Priority existingPri) {
        PriorityDialog dialog = new PriorityDialog(existingPri);
        dialog.setTitle(existingPri == null ? "New Priority" : "Edit Priority");
        dialog.showAndWait().ifPresent(resultPri -> {
            if (existingPri == null) {
                priorityService.addPriority(resultPri.getName());
            } else {
                if (confirmAction("Warning: Rename Priority", 
                        "Renaming this priority will update its tasks.",
                        "Do you want to continue?")) {
                    boolean success = priorityService.updatePriority(existingPri.getId(), resultPri.getName(), taskService);
                    if (!success) {
                        showInfoAlert("Cannot rename the Default priority.");
                    }
                }
            }
            priorityList.refresh();
            app.updateSummary();
        });
    }

    // Helper: returns the currently selected priority (or null if none)
    private Priority getSelectedPriority() {
        return priorityList.getSelectionModel().getSelectedItem();
    }

    // Helper: returns true if the given priority is the default
    private boolean isDefaultPriority(Priority priority) {
        return priority.getName().equals("Default");
    }

    // Helper: shows an information alert with the specified message
    private void showInfoAlert(String message) {
        new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK).showAndWait();
    }

    // Helper: displays a confirmation alert and returns true if user confirmed
    private boolean confirmAction(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}
