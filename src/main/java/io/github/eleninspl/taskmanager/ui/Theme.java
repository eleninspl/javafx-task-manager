package io.github.eleninspl.taskmanager.ui;

import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.scene.text.Font;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * Applies the application stylesheet to scenes and dialogs.
 * Due-date stamps use a monospaced face; JavaFX CSS cannot express a font fallback list,
 * so the first installed candidate is chosen at startup and added as a small generated stylesheet.
 */
public final class Theme {

    private static final String APP_CSS = Theme.class
            .getResource("/io/github/eleninspl/taskmanager/app.css").toExternalForm();

    private static final List<String> MONO_CANDIDATES = List.of(
            "SF Mono", "Menlo", "Cascadia Mono", "Consolas", "JetBrains Mono",
            "DejaVu Sans Mono", "Liberation Mono", "Ubuntu Mono", "Monospaced");

    private static final String MONO_CSS = buildMonoStylesheet();

    private Theme() {}

    /**
     * Adds the application styles to a scene.
     *
     * @param scene the scene to style
     */
    public static void apply(Scene scene) {
        scene.getStylesheets().addAll(APP_CSS, MONO_CSS);
    }

    /**
     * Adds the application styles to a dialog, which has its own scene.
     *
     * @param pane the dialog pane to style
     */
    public static void apply(DialogPane pane) {
        pane.getStylesheets().addAll(APP_CSS, MONO_CSS);
    }

    private static String buildMonoStylesheet() {
        List<String> installed = Font.getFamilies();
        String family = MONO_CANDIDATES.stream().filter(installed::contains).findFirst().orElse("Monospaced");
        String css = ".stamp, .mono { -fx-font-family: \"" + family + "\"; }";
        return "data:text/css;base64," + Base64.getEncoder().encodeToString(css.getBytes(StandardCharsets.UTF_8));
    }
}
