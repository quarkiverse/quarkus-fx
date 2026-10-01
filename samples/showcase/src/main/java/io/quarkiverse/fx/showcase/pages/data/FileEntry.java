package io.quarkiverse.fx.showcase.pages.data;

import io.quarkus.runtime.annotations.RegisterForReflection;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * TreeTableView model : read reflectively by TreeItemPropertyValueFactory.
 */
@RegisterForReflection
public class FileEntry {

    private final StringProperty name = new SimpleStringProperty(this, "name");
    private final StringProperty type = new SimpleStringProperty(this, "type");
    private final LongProperty size = new SimpleLongProperty(this, "size");
    private final StringProperty modified = new SimpleStringProperty(this, "modified");

    public FileEntry(String name, String type, long size, String modified) {
        this.name.set(name);
        this.type.set(type);
        this.size.set(size);
        this.modified.set(modified);
    }

    public StringProperty nameProperty() {
        return name;
    }

    public String getName() {
        return name.get();
    }

    public StringProperty typeProperty() {
        return type;
    }

    public String getType() {
        return type.get();
    }

    public LongProperty sizeProperty() {
        return size;
    }

    public long getSize() {
        return size.get();
    }

    public StringProperty modifiedProperty() {
        return modified;
    }

    public String getModified() {
        return modified.get();
    }

    public boolean isDirectory() {
        return "Folder".equals(getType());
    }

    @Override
    public String toString() {
        return getName();
    }
}
