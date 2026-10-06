package com.example.controller;

import com.example.model.Category;
import com.example.model.Priority;
import com.example.model.Task;
import com.example.service.CategoryService;
import com.example.service.PriorityService;
import com.example.service.TaskService;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import java.util.List;
import java.util.stream.Collectors;

public class SearchController {

    private TaskService taskService;       // Manages tasks
    private CategoryService categoryService; // Manages categories
    private PriorityService priorityService; // Manages priorities

    // Main container for search UI
    private VBox view;                     
    private TextField titleField;          // Input for title search
    private VBox categoryCheckBoxContainer; // Container for category filter checkboxes
    private VBox priorityCheckBoxContainer; // Container for priority filter checkboxes
    private Button searchBtn;              // Button to perform search
    private TableView<Task> resultTable;   // Table to display search results

    public SearchController(TaskService taskService, CategoryService categoryService, PriorityService priorityService) {
        this.taskService = taskService;
        this.categoryService = categoryService;
        this.priorityService = priorityService;
        createView();
        attachListeners();
    }

    public VBox getView() {
        return view;
    }

    // Build the entire search UI
    private void createView() {
        view = new VBox(10);
        view.setPadding(new Insets(10));

        Label header = createHeader();
        titleField = createTitleField();
        categoryCheckBoxContainer = createCategoryCheckBoxContainer();
        priorityCheckBoxContainer = createPriorityCheckBoxContainer();
        searchBtn = createSearchButton();
        resultTable = createResultTable();

        view.getChildren().addAll(header, new Label("Title:"), titleField,
                categoryCheckBoxContainer, priorityCheckBoxContainer,
                searchBtn, new Label("Results:"), resultTable);
    }

    // Create header label
    private Label createHeader() {
        Label header = new Label("Search Tasks");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        return header;
    }

    // Create the title search text field
    private TextField createTitleField() {
        TextField tf = new TextField();
        tf.setPromptText("Enter title text...");
        return tf;
    }

    // Create container for category checkboxes and initialize them
    private VBox createCategoryCheckBoxContainer() {
        VBox vbox = new VBox(5);
        vbox.setPadding(new Insets(5));
        Label catLabel = new Label("Filter by Categories:");
        vbox.getChildren().add(catLabel);
        updateCategoryCheckboxes(vbox);
        return vbox;
    }

    // Create container for priority checkboxes and initialize them
    private VBox createPriorityCheckBoxContainer() {
        VBox vbox = new VBox(5);
        vbox.setPadding(new Insets(5));
        Label priLabel = new Label("Filter by Priorities:");
        vbox.getChildren().add(priLabel);
        updatePriorityCheckboxes(vbox);
        return vbox;
    }

    // Create the search button and set its action
    private Button createSearchButton() {
        Button btn = new Button("Search");
        btn.setOnAction(e -> performSearch());
        return btn;
    }

    // Create the TableView for search results with its columns
    private TableView<Task> createResultTable() {
        TableView<Task> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(createTitleColumn());
        table.getColumns().add(createPriorityColumn());
        table.getColumns().add(createCategoryColumn());
        table.getColumns().add(createDueDateColumn());
        return table;
    }

    // Create Title column
    private TableColumn<Task, String> createTitleColumn() {
        TableColumn<Task, String> col = new TableColumn<>("Title");
        col.setCellValueFactory(new PropertyValueFactory<>("title"));
        return col;
    }

    // Create Priority column
    private TableColumn<Task, String> createPriorityColumn() {
        TableColumn<Task, String> col = new TableColumn<>("Priority");
        col.setCellValueFactory(cellData -> {
            Priority pri = cellData.getValue().getPriority();
            return new SimpleStringProperty(pri != null ? pri.getName() : "");
        });
        return col;
    }

    // Create Category column
    private TableColumn<Task, String> createCategoryColumn() {
        TableColumn<Task, String> col = new TableColumn<>("Category");
        col.setCellValueFactory(cellData -> {
            Category cat = cellData.getValue().getCategory();
            return new SimpleStringProperty(cat != null ? cat.getName() : "No Category");
        });
        return col;
    }

    // Create Due Date column
    private TableColumn<Task, String> createDueDateColumn() {
        TableColumn<Task, String> col = new TableColumn<>("Due Date");
        col.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getDueDate() != null ? cellData.getValue().getDueDate().toString() : "")
        );
        return col;
    }

    // Update category checkboxes in the given container
    private void updateCategoryCheckboxes(VBox container) {
        container.getChildren().removeIf(node -> node instanceof CheckBox);
        ObservableList<Category> categories = categoryService.getCategories();
        for (Category cat : categories) {
            if (cat.getName().equalsIgnoreCase("No Category")) continue;
            CheckBox cb = new CheckBox(cat.getName());
            cb.setUserData(cat.getId());
            container.getChildren().add(cb);
        }
    }

    // Update priority checkboxes in the given container
    private void updatePriorityCheckboxes(VBox container) {
        container.getChildren().removeIf(node -> node instanceof CheckBox);
        ObservableList<Priority> priorities = priorityService.getPriorities();
        for (Priority pri : priorities) {
            CheckBox cb = new CheckBox(pri.getName());
            cb.setUserData(pri.getId());
            container.getChildren().add(cb);
        }
    }

    // Attach listeners to refresh checkboxes when underlying lists change
    private void attachListeners() {
        categoryService.getCategories().addListener((ListChangeListener<Category>) change -> 
            updateCategoryCheckboxes(categoryCheckBoxContainer)
        );
        priorityService.getPriorities().addListener((ListChangeListener<Priority>) change -> 
            updatePriorityCheckboxes(priorityCheckBoxContainer)
        );
    }

    // Execute search based on title and selected filters
    private void performSearch() {
        String title = titleField.getText().trim();
        List<String> selectedCategoryIds = categoryCheckBoxContainer.getChildren().stream()
                .filter(node -> node instanceof CheckBox)
                .map(node -> (CheckBox) node)
                .filter(CheckBox::isSelected)
                .map(cb -> (String) cb.getUserData())
                .collect(Collectors.toList());
        List<String> selectedPriorityIds = priorityCheckBoxContainer.getChildren().stream()
                .filter(node -> node instanceof CheckBox)
                .map(node -> (CheckBox) node)
                .filter(CheckBox::isSelected)
                .map(cb -> (String) cb.getUserData())
                .collect(Collectors.toList());

        List<Task> filteredTasks = taskService.getTasks().stream().filter(task -> {
            boolean matches = true;
            if (!title.isEmpty()) {
                matches &= task.getTitle().toLowerCase().contains(title.toLowerCase());
            }
            if (!selectedCategoryIds.isEmpty()) {
                matches &= (task.getCategory() != null && selectedCategoryIds.contains(task.getCategory().getId()));
            }
            if (!selectedPriorityIds.isEmpty()) {
                matches &= (task.getPriority() != null && selectedPriorityIds.contains(task.getPriority().getId()));
            }
            return matches;
        }).collect(Collectors.toList());

        resultTable.setItems(FXCollections.observableArrayList(filteredTasks));
    }
}
