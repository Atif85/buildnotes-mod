package net.atif.buildnotes.data.template;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BuildTemplate {
    private String id;
    private String name;
    private List<String> fieldTitles;
    private boolean isBuiltIn;

    // No-arg constructor for Gson deserialization
    public BuildTemplate() {
        this.fieldTitles = new ArrayList<>();
    }

    public BuildTemplate(String id, String name, List<String> fieldTitles, boolean isBuiltIn) {
        this.id = id;
        this.name = name;
        this.fieldTitles = fieldTitles != null ? new ArrayList<>(fieldTitles) : new ArrayList<>();
        this.isBuiltIn = isBuiltIn;
    }

    // Keep record-style getter method names so all existing code works without changes
    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public List<String> fieldTitles() {
        return fieldTitles;
    }

    public boolean isBuiltIn() {
        return isBuiltIn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BuildTemplate that = (BuildTemplate) o;
        return isBuiltIn == that.isBuiltIn &&
                Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(fieldTitles, that.fieldTitles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, fieldTitles, isBuiltIn);
    }
}