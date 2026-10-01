package io.quarkiverse.fx.showcase.pages.fxml;

import io.quarkus.runtime.annotations.RegisterForReflection;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Table model instantiated by FXML (no-arg constructor and setters through BeanAdapter) and read by
 * {@link javafx.scene.control.cell.PropertyValueFactory} (reflective lookup of {@code xxxProperty()}).
 */
@RegisterForReflection
public class Person {

    private final StringProperty name = new SimpleStringProperty(this, "name", "");
    private final IntegerProperty age = new SimpleIntegerProperty(this, "age");
    private final StringProperty role = new SimpleStringProperty(this, "role", "");

    public Person() {
    }

    public StringProperty nameProperty() {
        return name;
    }

    public String getName() {
        return name.get();
    }

    public void setName(String value) {
        name.set(value);
    }

    public IntegerProperty ageProperty() {
        return age;
    }

    public int getAge() {
        return age.get();
    }

    public void setAge(int value) {
        age.set(value);
    }

    public StringProperty roleProperty() {
        return role;
    }

    public String getRole() {
        return role.get();
    }

    public void setRole(String value) {
        role.set(value);
    }

    @Override
    public String toString() {
        return getName() + " (" + getAge() + ", " + getRole() + ")";
    }
}
