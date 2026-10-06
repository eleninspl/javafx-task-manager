package io.github.eleninspl.taskmanager.controller.dialog;

import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;

/** Layout helpers shared by the dialogs. */
final class Forms {

    private Forms() {}

    /** A label stacked above its control; the label's mnemonic (e.g. "_Title") focuses the control. */
    static VBox field(String label, Control control) {
        Label l = new Label(label);
        l.setMnemonicParsing(true);
        l.setLabelFor(control);
        l.getStyleClass().add("field-label");
        control.setMaxWidth(Double.MAX_VALUE);
        VBox box = new VBox(6, l, control);
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    /** Two fields side by side, sharing the width equally. */
    static HBox row(Node left, Node right) {
        HBox row = new HBox(12, left, right);
        if (left instanceof Region r) r.setPrefWidth(1);
        if (right instanceof Region r) r.setPrefWidth(1);
        return row;
    }

    private static final java.time.format.DateTimeFormatter DISPLAY =
            java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.ENGLISH);
    private static final java.util.List<java.time.format.DateTimeFormatter> ACCEPTED = java.util.List.of(
            DISPLAY,
            java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy"),
            java.time.format.DateTimeFormatter.ofPattern("d-M-yyyy"),
            java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);

    /**
     * Shows dates as "4 Oct 2026" like the rest of the app, accepts day-first typing (4/10/2026),
     * and commits dates as they are typed, not only on Enter, so a typed date is never silently lost.
     */
    static void commitTypedDates(DatePicker picker) {
        picker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return date == null ? "" : date.format(DISPLAY);
            }

            @Override
            public LocalDate fromString(String text) {
                if (text == null || text.isBlank()) return null;
                for (java.time.format.DateTimeFormatter f : ACCEPTED) {
                    try {
                        return LocalDate.parse(text.trim(), f);
                    } catch (java.time.format.DateTimeParseException ignored) {
                        // try the next format
                    }
                }
                throw new IllegalArgumentException("Not a date: " + text);
            }
        });
        picker.setPromptText("e.g. 14 Oct 2026");
        picker.getEditor().textProperty().addListener((obs, old, text) -> {
            StringConverter<LocalDate> converter = picker.getConverter();
            try {
                LocalDate parsed = (text == null || text.isBlank()) ? null : converter.fromString(text);
                if (parsed != null && !parsed.equals(picker.getValue())) picker.setValue(parsed);
                if ((text == null || text.isBlank()) && picker.getValue() != null) picker.setValue(null);
            } catch (RuntimeException ignored) {
                // Incomplete date while typing; keep the last valid value
            }
        });
    }
}
