package net.atif.buildnotes.data;

import net.minecraft.network.FriendlyByteBuf;
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

    public void writeToBuf(FriendlyByteBuf buf) {
        buf.writeUUID(this.getId());
        buf.writeLong(this.getLastModified());
        buf.writeById(Scope::ordinal, this.getScope());
        buf.writeUtf(this.name);
        buf.writeUtf(this.coordinates);
        buf.writeUtf(this.dimension);
        buf.writeUtf(this.description);
        buf.writeUtf(this.credits);

        List<String> images = this.getImageFileNames();
        buf.writeVarInt(images.size());
        for (String image: images) {
            buf.writeUtf(image);
        }

        buf.writeVarInt(this.customFields.size());
        for (CustomField field : this.customFields) {
            field.writeToBuf(buf);
        }
    }

    public static Build fromBuf(FriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        long lastModified = buf.readLong();
        Scope scope = buf.readById(index -> Scope.values()[index]);
        String name = buf.readUtf();
        String coords = buf.readUtf();
        String dim = buf.readUtf();
        String desc = buf.readUtf();
        String cred = buf.readUtf();

        int imageCount = buf.readVarInt();
        List<String> images = new ArrayList<>(imageCount);
        for (int i = 0; i < imageCount; i++) {
            images.add(buf.readUtf());
        }

        int fieldCount = buf.readVarInt();
        List<CustomField> fields = new ArrayList<>(fieldCount);
        for (int i = 0; i < fieldCount; i++) {
            fields.add(CustomField.fromBuf(buf));
        }

        return new Build(id, lastModified, scope, name, coords, dim, desc, cred, images, fields);
    }
}
