package io.quarkiverse.fx.showcase.pages.platform;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * A plain Java bean (getters, setters, bound properties through {@link PropertyChangeSupport}) adapted to JavaFX
 * properties by the {@code javafx.beans.property.adapter} builders, which access it reflectively.
 */
@RegisterForReflection
public class LegacyAccount {

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private String owner;
    private int balance;

    public LegacyAccount(String owner, int balance) {
        this.owner = owner;
        this.balance = balance;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        String old = this.owner;
        this.owner = owner;
        support.firePropertyChange("owner", old, owner);
    }

    public int getBalance() {
        return balance;
    }

    public void deposit(int amount) {
        int old = balance;
        balance += amount;
        support.firePropertyChange("balance", old, balance);
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
