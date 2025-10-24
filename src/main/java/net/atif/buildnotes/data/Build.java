package net.atif.buildnotes.data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Build {
    private final UUID id;
    private String name;
    private String coordinates;
    private String dimension;
    private String description;
    private String credits;
    private final List<CustomField> customFields;
    private long lastModified;

    public Build(String name, String coordinates, String dimension, String description, String credits) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.coordinates = coordinates;
        this.dimension = dimension;
        this.description = description;
        this.credits = credits;
        this.customFields = new ArrayList<>();
        this.updateTimestamp();
    }

    public void updateTimestamp() {
        this.lastModified = Instant.now().getEpochSecond();
    }

    // --- Getters ---
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getCoordinates() { return coordinates; }
    public String getDimension() { return dimension; }
    public String getDescription() { return description; }
    public String getCredits() { return credits; }
    public List<CustomField> getCustomFields() { return customFields; }
    public long getLastModified() { return lastModified; }

    // --- Setters ---
    public void setName(String name) { this.name = name; }
    public void setCoordinates(String coordinates) { this.coordinates = coordinates; }
    public void setDimension(String dimension) { this.dimension = dimension; }
    public void setDescription(String description) { this.description = description; }
    public void setcredits(String credits) { this.credits = credits; }
}
