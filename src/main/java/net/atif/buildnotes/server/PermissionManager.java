package net.atif.buildnotes.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.mojang.authlib.GameProfile;
import net.atif.buildnotes.Buildnotes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// NOTE: This class is SERVER-ONLY
public class PermissionManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String PERMISSIONS_FILE = "buildnotes_permissions.json";
    private final Path configPath;

    private boolean allowAll = false;
    // The data structure now stores a PermissionEntry object, not just a UUID.
    private final Set<PermissionEntry> allowedPlayers = new HashSet<>();

    public PermissionManager(MinecraftServer server) {
        this.configPath = server.getSavePath(WorldSavePath.ROOT).resolve("buildnotes").resolve(PERMISSIONS_FILE);
        load();
    }

    private void load() {
        try {
            if (Files.notExists(configPath)) {
                save(); // Create a default empty file if it doesn't exist
                return;
            }
            try (FileReader reader = new FileReader(configPath.toFile())) {
                JsonElement element = JsonParser.parseReader(reader);
                if (!element.isJsonObject()) return; // Handle empty or malformed file

                JsonObject json = element.getAsJsonObject();
                this.allowAll = json.has("allowAll") && json.get("allowAll").getAsBoolean();

                // Deserialize into a Set of PermissionEntry objects
                if (json.has("allowedPlayers")) {
                    Set<PermissionEntry> loadedEntries = GSON.fromJson(json.get("allowedPlayers"), new TypeToken<HashSet<PermissionEntry>>(){}.getType());
                    if (loadedEntries != null) {
                        allowedPlayers.addAll(loadedEntries);
                    }
                }
            }
        } catch (Exception e) {
            Buildnotes.LOGGER.error("Failed to load BuildNotes permissions file!", e);
        }
    }

    private void save() {
        try {
            Files.createDirectories(configPath.getParent());
            try (FileWriter writer = new FileWriter(configPath.toFile())) {
                JsonObject json = new JsonObject();
                json.addProperty("allowAll", this.allowAll);
                // Serialize the Set of PermissionEntry objects
                json.add("allowedPlayers", GSON.toJsonTree(this.allowedPlayers));
                GSON.toJson(json, writer);
            }
        } catch (IOException e) {
            Buildnotes.LOGGER.error("Failed to save BuildNotes permissions file!", e);
        }
    }

    public boolean isAllowedToEdit(ServerPlayerEntity player) {
        if (this.allowAll) {
            return true;
        }
        if (player.getServer().getPlayerManager().isOperator(player.getGameProfile())) {
            return true;
        }
        // Check if any entry in the set matches the player's UUID
        return allowedPlayers.stream().anyMatch(entry -> entry.getUuid().equals(player.getUuid()));
    }

    // Now accepts a GameProfile to get both UUID and name
    public boolean addPlayer(GameProfile profile) {
        PermissionEntry newEntry = new PermissionEntry(profile.getId(), profile.getName());
        if (allowedPlayers.add(newEntry)) {
            save();
            return true;
        }
        return false;
    }

    // Now accepts a GameProfile to find the player to remove
    public boolean removePlayer(GameProfile profile) {
        // We can remove based on a dummy entry, since equals() only checks the UUID
        if (allowedPlayers.remove(new PermissionEntry(profile.getId(), ""))) {
            save();
            return true;
        }
        return false;
    }

    public void setAllowAll(boolean allow) {
        this.allowAll = allow;
        save();
    }

    public boolean getAllowAll() {
        return this.allowAll;
    }

    // Returns the full entries for the list command
    public Set<PermissionEntry> getAllowedPlayers() {
        return new HashSet<>(allowedPlayers);
    }
}