package io.quarkiverse.fx.showcase.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Helpers to run checks, attach them to a page node and display them.
 */
public final class Checks {

    private static final String KEY = "showcase.checks";

    private Checks() {
    }

    /**
     * Runs {@code action} : passed with the returned value, or failed with the exception.
     */
    public static Check run(String name, Callable<?> action) {
        try {
            return Check.pass(name, action.call());
        } catch (Throwable t) {
            return Check.fail(name, describe(t));
        }
    }

    /**
     * Runs {@code action} : passed if the returned value equals {@code expected}.
     */
    public static Check expect(String name, Object expected, Callable<?> action) {
        try {
            Object actual = action.call();
            boolean ok = expected == null ? actual == null : expected.equals(actual);
            return Check.of(name, ok, ok ? actual : "expected " + expected + " but got " + actual);
        } catch (Throwable t) {
            return Check.fail(name, describe(t));
        }
    }

    public static String describe(Throwable t) {
        StringBuilder sb = new StringBuilder(t.getClass().getName());
        if (t.getMessage() != null) {
            sb.append(": ").append(t.getMessage());
        }
        Throwable cause = t.getCause();
        int depth = 0;
        while (cause != null && cause != t && depth++ < 5) {
            sb.append(" <- ").append(cause.getClass().getName());
            if (cause.getMessage() != null) {
                sb.append(": ").append(cause.getMessage());
            }
            t = cause;
            cause = cause.getCause();
        }
        return sb.toString();
    }

    public static void attach(Node node, List<Check> checks) {
        node.getProperties().put(KEY, List.copyOf(checks));
    }

    @SuppressWarnings("unchecked")
    public static List<Check> of(Node node) {
        Object checks = node.getProperties().get(KEY);
        return checks instanceof List<?> list ? (List<Check>) list : List.of();
    }

    /**
     * A table of checks (name, value, status), with the checks attached to it.
     */
    public static VBox view(String title, List<Check> checks) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("checks-grid");
        grid.setHgap(12);
        grid.setVgap(4);
        ColumnConstraints status = new ColumnConstraints(24);
        status.setHalignment(HPos.CENTER);
        ColumnConstraints name = new ColumnConstraints(260);
        ColumnConstraints value = new ColumnConstraints();
        value.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(status, name, value);

        int row = 0;
        for (Check check : checks) {
            Label statusLabel = new Label(check.ok() == null ? "•" : check.ok() ? "✔" : "✘");
            statusLabel.getStyleClass().add(check.ok() == null ? "check-info" : check.ok() ? "check-pass" : "check-fail");
            Label nameLabel = new Label(check.name());
            nameLabel.getStyleClass().add("check-name");
            Label valueLabel = new Label(check.value());
            valueLabel.getStyleClass().add("check-value");
            valueLabel.setWrapText(true);
            grid.addRow(row++, statusLabel, nameLabel, valueLabel);
        }

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("section-title");
        VBox box = new VBox(8, titleLabel, grid);
        box.setPadding(new Insets(4));
        attach(box, checks);
        return box;
    }

    /**
     * Collects the checks attached to {@code root} and to all its descendants.
     */
    public static List<Check> collect(Node root) {
        List<Check> all = new ArrayList<>(of(root));
        if (root instanceof javafx.scene.Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                all.addAll(collect(child));
            }
        }
        return all;
    }
}
