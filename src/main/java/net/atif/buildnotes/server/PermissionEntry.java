package net.atif.buildnotes.server;

import java.util.Objects;
import java.util.UUID;

/**
 * A simple data class representing a player with permission.
 * Used for serializing to and from buildnotes_permissions.json.
 */
public class PermissionEntry {
    private final String uuid;
    private final String name;

    private PermissionEntry() {
        this.uuid = null;
        this.name = null;
    }

    public PermissionEntry(UUID uuid, String name) {
        this.uuid = uuid.toString();
        this.name = name;
    }

    public UUID getUuid() {
        return UUID.fromString(this.uuid);
    }

    public String getName() {
        return this.name;
    }

    // We define equality based on the UUID only, to prevent duplicate entries for the same player.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PermissionEntry that = (PermissionEntry) o;
        return Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}