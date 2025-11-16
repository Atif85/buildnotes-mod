package net.atif.buildnotes.data;

import net.minecraft.network.PacketByteBuf;

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

    private Build(UUID id, long lastModified, Scope scope, String name, String coords, String dim, String desc, String cred, List<String> images, List<CustomField> fields) {
        super(id, lastModified, scope);
        this.name = name;
        this.coordinates = coords;
        this.dimension = dim;
        this.description = desc;
        this.credits = cred;
        this.imageFileNames = images;
        this.customFields = fields;
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

    public void writeToBuf(PacketByteBuf buf) {
        buf.writeUuid(this.getId());
        buf.writeLong(this.getLastModified());
        buf.writeEnumConstant(this.getScope());
        buf.writeString(this.name);
        buf.writeString(this.coordinates);
        buf.writeString(this.dimension);
        buf.writeString(this.description);
        buf.writeString(this.credits);
        buf.writeCollection(this.imageFileNames, PacketByteBuf::writeString);
        buf.writeCollection(this.customFields, (b, field) -> field.writeToBuf(b));
    }

    public static Build fromBuf(PacketByteBuf buf) {
        UUID id = buf.readUuid();
        long lastModified = buf.readLong();
        Scope scope = buf.readEnumConstant(Scope.class);
        String name = buf.readString();
        String coords = buf.readString();
        String dim = buf.readString();
        String desc = buf.readString();
        String cred = buf.readString();
        List<String> images = buf.readList(PacketByteBuf::readString);
        List<CustomField> fields = buf.readList(CustomField::fromBuf);

        return new Build(id, lastModified, scope, name, coords, dim, desc, cred, images, fields);
    }
}
