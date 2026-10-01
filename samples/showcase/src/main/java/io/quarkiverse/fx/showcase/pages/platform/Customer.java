package io.quarkiverse.fx.showcase.pages.platform;

import io.quarkus.runtime.annotations.RegisterForReflection;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * JavaFX bean model navigated by {@code Bindings.select(customer, "address", "city")} : the select binding looks up the
 * {@code xxxProperty()} methods reflectively.
 */
@RegisterForReflection
public class Customer {

    private final StringProperty name = new SimpleStringProperty(this, "name");
    private final ObjectProperty<Address> address = new SimpleObjectProperty<>(this, "address");

    public Customer(String name, Address address) {
        setName(name);
        setAddress(address);
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

    public ObjectProperty<Address> addressProperty() {
        return address;
    }

    public Address getAddress() {
        return address.get();
    }

    public void setAddress(Address value) {
        address.set(value);
    }
}
