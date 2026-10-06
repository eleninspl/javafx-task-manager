package io.github.eleninspl.taskmanager.controller;

import io.github.eleninspl.taskmanager.controller.dialog.NameDialog;
import io.github.eleninspl.taskmanager.ui.Dialogs;
import io.github.eleninspl.taskmanager.ui.Icons;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * A screen for managing a list of named items (categories or priority levels): add, rename
 * and delete, with one built-in item that can't be changed.
 *
 * @param <T> the item type
 */
public abstract class ManagedListController<T> {

    private final ListView<T> list = new ListView<>();
    private final Label subtitle = new Label();
    private final String noun;
    private final String plural;
    private final ObservableList<T> items;
    private final ObservableList<?> usageSource;
    private final VBox view;

    /**
     * @param title       view title, e.g. "Categories"
     * @param noun        singular item name, e.g. "category"
     * @param plural      plural item name, e.g. "categories"
     * @param items       the live list of items
     * @param usageSource list whose changes affect the usage counts (the tasks)
     */
    protected ManagedListController(String title, String noun, String plural, ObservableList<T> items,
                                    ObservableList<?> usageSource) {
        this.noun = noun;
        this.plural = plural;
        Button add = new Button("New " + noun, Icons.icon(Icons.PLUS, 14));
        add.getStyleClass().add("primary");
        add.setOnAction(e -> add(noun));
        Button rename = new Button("Rename");
        rename.setOnAction(e -> rename(noun));
        Button delete = new Button("Delete");
        delete.setOnAction(e -> delete());
        var selected = list.getSelectionModel().selectedItemProperty();
        rename.disableProperty().bind(Bindings.createBooleanBinding(
                () -> selected.get() == null || isBuiltIn(selected.get()), selected));
        delete.disableProperty().bind(rename.disableProperty());

        Node header = ViewHeader.create(new Label(title), subtitle, rename, delete, add);

        list.setItems(items);
        list.getStyleClass().add("plain");
        list.setCellFactory(lv -> new ItemCell());
        list.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.F2) rename(noun);
            if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) delete();
        });
        StackPane panel = new StackPane(list);
        panel.getStyleClass().add("panel");
        VBox.setVgrow(panel, Priority.ALWAYS);

        Label note = new Label(builtInNote());
        note.getStyleClass().add("hint");
        note.setWrapText(true);

        view = new VBox(16, header, panel, note);
        view.setStyle("-fx-padding: 22 20 20 20;");

        this.items = items;
        this.usageSource = usageSource;
    }

    /**
     * Starts listening for changes and fills in the counts. Subclasses call this at the end of
     * their constructor, once the fields the abstract methods rely on are set.
     */
    protected final void start() {
        InvalidationListener update = obs -> {
            int custom = (int) items.stream().filter(i -> !isBuiltIn(i)).count();
            String builtIn = items.stream().filter(this::isBuiltIn).map(this::name).findFirst().orElse("");
            subtitle.setText((custom == 1 ? "1 " + noun : custom + " " + plural) + " plus \u201C" + builtIn + "\u201D");
            list.refresh();
        };
        items.addListener(update);
        usageSource.addListener(update);
        update.invalidated(null);
    }

    /** @return the view's root node */
    public Node getView() {
        return view;
    }

    /** @return display name of an item */
    protected abstract String name(T item);

    /** @return whether the item is the protected default */
    protected abstract boolean isBuiltIn(T item);

    /** @return how many tasks use the item */
    protected abstract long usage(T item);

    /** @return how many unfinished tasks use the item (the number the sidebar shows) */
    protected abstract long openUsage(T item);

    /** @return an error for an unusable name, or null; excluded is the item being renamed */
    protected abstract String nameError(String name, T excluded);

    /** Adds an item with a validated name. */
    protected abstract void create(String name);

    /** Renames an item to a validated name. */
    protected abstract void rename(T item, String name);

    /** Deletes an item and applies the consequence to its tasks. */
    protected abstract void remove(T item);

    /** @return what deleting the item does to its tasks, in plain words */
    protected abstract String deleteConsequence(T item, long usage);

    /** @return one line explaining the built-in item */
    protected abstract String builtInNote();

    private void add(String noun) {
        new NameDialog("New " + noun, "_Name", "", "Add " + noun, name -> nameError(name, null))
                .showAndWait().ifPresent(this::create);
    }

    private void rename(String noun) {
        T item = list.getSelectionModel().getSelectedItem();
        if (item == null || isBuiltIn(item)) return;
        new NameDialog("Rename " + noun, "_Name", name(item), "Rename", name -> nameError(name, item))
                .showAndWait().ifPresent(name -> rename(item, name));
    }

    private void delete() {
        T item = list.getSelectionModel().getSelectedItem();
        if (item == null || isBuiltIn(item)) return;
        if (Dialogs.confirm("Delete “" + name(item) + "”?", deleteConsequence(item, usage(item)),
                "Delete")) {
            remove(item);
        }
    }

    static String tasks(long n) {
        return n == 1 ? "1 task" : n + " tasks";
    }

    private class ItemCell extends ListCell<T> {
        ItemCell() {
            setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && getItem() != null
                        && !isBuiltIn(getItem())) {
                    list.getSelectionModel().select(getItem());
                    rename(noun);
                }
            });
        }

        @Override
        protected void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);
            setText(null);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }
            Label name = new Label(name(item));
            name.setStyle("-fx-font-weight: bold;");
            HBox left = new HBox(8, name);
            left.setAlignment(Pos.CENTER_LEFT);
            if (isBuiltIn(item)) {
                Label builtIn = new Label("Built in", Icons.icon(Icons.LOCK, 13));
                builtIn.getStyleClass().add("meta");
                builtIn.setGraphicTextGap(4);
                builtIn.setTooltip(new Tooltip("Can't be renamed or deleted"));
                left.getChildren().add(builtIn);
            }
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            long total = usage(item);
            long open = openUsage(item);
            String usageText = total == 0 ? "No tasks"
                    : open == total ? open + " open"
                    : open + " open \u00B7 " + (total - open) + " completed";
            Label count = new Label(usageText);
            count.getStyleClass().add("text-tertiary");
            HBox row = new HBox(left, spacer, count);
            row.setAlignment(Pos.CENTER_LEFT);
            setGraphic(row);
            setAccessibleText(name(item) + ", " + usageText + (isBuiltIn(item) ? ", built in" : ""));
        }
    }
}
