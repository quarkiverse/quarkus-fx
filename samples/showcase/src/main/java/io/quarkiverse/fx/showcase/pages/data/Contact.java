package io.quarkiverse.fx.showcase.pages.data;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Plain Java bean (no JavaFX property), adapted through the JavaBean property builders (reflective).
 */
@RegisterForReflection
public class Contact {

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private String name;
    private int rating;

    public Contact() {
    }

    public Contact(String name, int rating) {
        this.name = name;
        this.rating = rating;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        String old = this.name;
        this.name = name;
        support.firePropertyChange("name", old, name);
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        int old = this.rating;
        this.rating = rating;
        support.firePropertyChange("rating", old, rating);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String property, PropertyChangeListener listener) {
        support.addPropertyChangeListener(property, listener);
    }

    public void removePropertyChangeListener(String property, PropertyChangeListener listener) {
        support.removePropertyChangeListener(property, listener);
    }
}
