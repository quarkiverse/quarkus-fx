package io.quarkiverse.fx.showcase.pages.platform;

import io.quarkus.runtime.annotations.RegisterForReflection;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Second step of the {@code Bindings.select} chain.
 */
@RegisterForReflection
public class Address {

    private final StringProperty city = new SimpleStringProperty(this, "city");
    private final IntegerProperty zip = new SimpleIntegerProperty(this, "zip");

    public Address(String city, int zip) {
        setCity(city);
        setZip(zip);
    }

    public StringProperty cityProperty() {
        return city;
    }

    public String getCity() {
        return city.get();
    }

    public void setCity(String value) {
        city.set(value);
    }

    public IntegerProperty zipProperty() {
        return zip;
    }

    public int getZip() {
        return zip.get();
    }

    public void setZip(int value) {
        zip.set(value);
    }
}
