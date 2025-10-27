package net.atif.buildnotes.data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Build extends BaseEntry {
    private String name;
    private String coordinates;
    private String dimension;
    private String description;
    private String credits;
    private final List<CustomField> customFields;
    private List<String> imageFileNames;

    public Build(String name, String coordinates, String dimension, String description, String credits) {
        super();
        this.name = name;
        this.coordinates = coordinates;
        this.dimension = dimension;
        this.description = description;
        this.credits = credits;
        this.customFields = new ArrayList<>();
        this.imageFileNames = new ArrayList<>();
    }

    // Optional protected no-arg constructor for deserializers (safe)
    protected Build() {
        super();
        this.customFields = new ArrayList<>();
        this.imageFileNames = new ArrayList<>();
    }

    // --- Getters ---
    public UUID getId() { return super.getId(); } // kept for symmetry with old code (optional)
    public String getName() { return name; }
    public String getCoordinates() { return coordinates; }
    public String getDimension() { return dimension; }
    public String getDescription() { return description; }
    public String getCredits() { return credits; }
    public List<CustomField> getCustomFields() { return customFields; }

    public List<String> getImageFileNames() {
        if (this.imageFileNames == null) {
            // Backward-compatibility for older JSON where this may be null
            this.imageFileNames = new ArrayList<>();
        }
        return this.imageFileNames;
    }

    public long getLastModified() { return super.getLastModified(); }

    // --- Setters ---
    public void setName(String name) { this.name = name; }
    public void setCoordinates(String coordinates) { this.coordinates = coordinates; }
    public void setDimension(String dimension) { this.dimension = dimension; }
    public void setDescription(String description) { this.description = description; }
    public void setCredits(String credits) { this.credits = credits; }
}
