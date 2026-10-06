package io.github.eleninspl.taskmanager.controller;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** The title block at the top of each view, with optional actions on the right. */
final class ViewHeader {

    private ViewHeader() {}

    static HBox create(Label title, Label subtitle, Node... actions) {
        title.getStyleClass().add("view-title");
        subtitle.getStyleClass().add("view-subtitle");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(8, new VBox(2, title, subtitle), spacer);
        header.getChildren().addAll(actions);
        header.setAlignment(Pos.BOTTOM_LEFT);
        return header;
    }
}
