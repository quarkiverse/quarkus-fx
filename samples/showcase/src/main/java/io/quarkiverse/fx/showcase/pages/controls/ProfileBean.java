package io.quarkiverse.fx.showcase.pages.controls;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * A plain JavaBean (getters, setters and bound properties through PropertyChangeSupport), adapted to JavaFX properties
 * by the javafx.beans.property.adapter builders : they find the accessors and the listener registration methods by
 * reflection, hence the registration.
 */
@RegisterForReflection
public class ProfileBean {

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    private String name = "Ada Lovelace";
    private int age = 36;
    private boolean subscribed;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        String old = this.name;
        this.name = name;
        support.firePropertyChange("name", old, name);
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        int old = this.age;
        this.age = age;
        support.firePropertyChange("age", old, age);
    }

    public boolean isSubscribed() {
        return subscribed;
    }

    public void setSubscribed(boolean subscribed) {
        boolean old = this.subscribed;
        this.subscribed = subscribed;
        support.firePropertyChange("subscribed", old, subscribed);
    }

    public void addPropertyChangeListener(String property, PropertyChangeListener listener) {
        support.addPropertyChangeListener(property, listener);
    }

    public void removePropertyChangeListener(String property, PropertyChangeListener listener) {
        support.removePropertyChangeListener(property, listener);
    }

    @Override
    public String toString() {
        return name + ", " + age + (subscribed ? ", subscribed" : "");
    }
}
