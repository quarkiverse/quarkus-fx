package io.quarkiverse.fx.showcase.pages.data;

import io.quarkus.runtime.annotations.RegisterForReflection;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * TableView model : read reflectively by PropertyValueFactory ({@code xxxProperty()} methods, and the
 * {@code getFullName()} getter fallback) and by Bindings.select.
 */
@RegisterForReflection
public class Person {

    private final StringProperty firstName = new SimpleStringProperty(this, "firstName");
    private final StringProperty lastName = new SimpleStringProperty(this, "lastName");
    private final StringProperty email = new SimpleStringProperty(this, "email");
    private final IntegerProperty age = new SimpleIntegerProperty(this, "age");
    private final StringProperty department = new SimpleStringProperty(this, "department");
    private final BooleanProperty active = new SimpleBooleanProperty(this, "active");
    private final DoubleProperty progress = new SimpleDoubleProperty(this, "progress");

    public Person(String firstName, String lastName, String email, int age, String department, boolean active,
            double progress) {
        this.firstName.set(firstName);
        this.lastName.set(lastName);
        this.email.set(email);
        this.age.set(age);
        this.department.set(department);
        this.active.set(active);
        this.progress.set(progress);
    }

    /**
     * Parses a {@code firstName,lastName,email,age,department,active,progress} line.
     */
    public static Person parse(String line) {
        String[] f = line.split(",");
        return new Person(f[0], f[1], f[2], Integer.parseInt(f[3]), f[4], Boolean.parseBoolean(f[5]),
                Double.parseDouble(f[6]));
    }

    public StringProperty firstNameProperty() {
        return firstName;
    }

    public String getFirstName() {
        return firstName.get();
    }

    public StringProperty lastNameProperty() {
        return lastName;
    }

    public String getLastName() {
        return lastName.get();
    }

    public StringProperty emailProperty() {
        return email;
    }

    public String getEmail() {
        return email.get();
    }

    public IntegerProperty ageProperty() {
        return age;
    }

    public int getAge() {
        return age.get();
    }

    public StringProperty departmentProperty() {
        return department;
    }

    public String getDepartment() {
        return department.get();
    }

    public BooleanProperty activeProperty() {
        return active;
    }

    public boolean isActive() {
        return active.get();
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public double getProgress() {
        return progress.get();
    }

    /**
     * No {@code fullNameProperty()} : PropertyValueFactory falls back to this getter.
     */
    public String getFullName() {
        return getFirstName() + " " + getLastName();
    }

    @Override
    public String toString() {
        return getFullName();
    }
}
