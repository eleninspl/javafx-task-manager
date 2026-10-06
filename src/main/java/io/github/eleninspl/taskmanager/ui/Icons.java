package io.github.eleninspl.taskmanager.ui;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.SVGPath;

/**
 * A small set of line icons drawn on a 24-unit grid and rendered at 16px.
 * Stroke color and width come from the "icon" style class, so icons follow the theme.
 */
public final class Icons {

    public static final String PLUS = "M12 5v14M5 12h14";
    public static final String SEARCH = "M10.5 4a6.5 6.5 0 1 0 0 13a6.5 6.5 0 1 0 0-13zM20 20l-4.8-4.8";
    public static final String ALL = "M4 6h16M4 12h16M4 18h10";
    public static final String OVERDUE = "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18zM12 7.5v5M12 16.2v.3";
    public static final String WEEK = "M4 6h16v14H4zM4 10.5h16M8.5 3v5M15.5 3v5";
    public static final String FOLDER = "M3.5 6.5h6l2 2h9v10.5h-17z";
    public static final String BELL = "M6.5 16.5v-5a5.5 5.5 0 0 1 11 0v5l1.5 1.5h-14zM10 21h4";
    public static final String FLAG = "M5.5 21V4M5.5 4.5h11l-2.2 4l2.2 4h-11";
    public static final String CLOSE = "M7 7l10 10M17 7L7 17";
    public static final String LOCK = "M7 11V8a5 5 0 0 1 10 0v3M5.5 11h13v9h-13z";

    private Icons() {}

    /**
     * Creates a 16px icon node.
     *
     * @param path SVG path data on a 24-unit grid
     * @return the icon node
     */
    public static Node icon(String path) {
        return icon(path, 16);
    }

    /**
     * Creates an icon node at the given size.
     *
     * @param path SVG path data on a 24-unit grid
     * @param size rendered size in pixels
     * @return the icon node
     */
    public static Node icon(String path, double size) {
        SVGPath svg = new SVGPath();
        svg.setContent(path);
        svg.getStyleClass().add("icon");
        double scale = size / 24.0;
        svg.setScaleX(scale);
        svg.setScaleY(scale);
        // Wrap in a Group so layout uses the scaled bounds
        Group group = new Group(svg);
        group.setFocusTraversable(false);
        group.setMouseTransparent(true);
        return group;
    }
}
