package net.atif.buildnotes.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.atif.buildnotes.Buildnotes;
import net.fabricmc.loader.api.FabricLoader;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir();
    private static final String NOTES_FILE = "buildnotes_notes.json";
    private static final String BUILDS_FILE = "buildnotes_builds.json";

    private static DataManager instance;

    private List<Note> notes;
    private List<Build> builds;

    private DataManager() {
        loadNotes();
        loadBuilds();
    }

    public static DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }

    // --- Note Methods ---
    public List<Note> getNotes() { return this.notes; }

    public void saveNotes() {
        try (FileWriter writer = new FileWriter(CONFIG_PATH.resolve(NOTES_FILE).toFile())) {
            GSON.toJson(this.notes, writer);
        } catch (IOException e) {
            Buildnotes.LOGGER.error("Could not save notes!", e);
        }
    }

    private void loadNotes() {
        try (FileReader reader = new FileReader(CONFIG_PATH.resolve(NOTES_FILE).toFile())) {
            Type type = new TypeToken<ArrayList<Note>>() {}.getType();
            this.notes = GSON.fromJson(reader, type);
        } catch (IOException e) {
            Buildnotes.LOGGER.warn("Could not find notes file, creating new list...");
            this.notes = new ArrayList<>();
            // Add some default data for demonstration purposes
            addDefaultNotes();
            saveNotes();
        }
        if (this.notes == null) {
            this.notes = new ArrayList<>();
        }
    }

    // --- Build Methods ---
    public List<Build> getBuilds() { return this.builds; }

    public void saveBuilds() {
        try (FileWriter writer = new FileWriter(CONFIG_PATH.resolve(BUILDS_FILE).toFile())) {
            GSON.toJson(this.builds, writer);
        } catch (IOException e) {
            Buildnotes.LOGGER.error("Could not save builds!", e);
        }
    }

    private void loadBuilds() {
        try (FileReader reader = new FileReader(CONFIG_PATH.resolve(BUILDS_FILE).toFile())) {
            Type type = new TypeToken<ArrayList<Build>>() {}.getType();
            this.builds = GSON.fromJson(reader, type);
        } catch (IOException e) {
            Buildnotes.LOGGER.warn("Could not find builds file, creating new list...");
            this.builds = new ArrayList<>();
            addDefaultBuilds();
            saveBuilds();
        }
        if (this.builds == null) {
            this.builds = new ArrayList<>();
        }
    }

    private void addDefaultBuilds() {
        Build starterBase = new Build("Starter Base", "100, 64, -250", "Overworld", "A small oak and cobblestone house.", "Player");
        starterBase.getCustomFields().add(new CustomField("Resources Needed", "5 stacks of wood, 10 stacks of cobblestone."));
        starterBase.getCustomFields().add(new CustomField("Resources Needed", "5 stacks of wood, 10 stacks of cobblestone."));
        starterBase.getCustomFields().add(new CustomField("Resources Needed", "5 stacks of wood, 10 stacks of cobblestone."));
        starterBase.getCustomFields().add(new CustomField("Resousdfrces Needed", "5 stacks of wood, 10 stacks of cobblestone."));
        starterBase.getCustomFields().add(new CustomField("Resourcdsfses Needed", "5 stacks of wood, 10 stacks of cobblestone."));
        starterBase.getCustomFields().add(new CustomField("Resources Needed", "5 stacks of wood, 10 stacks of cobblestone."));
        starterBase.getCustomFields().add(new CustomField("Resoussdfdfsrces Needed", "5 stacks of wood, 10 stacks of cobblestone."));
        starterBase.getCustomFields().add(new CustomField("Resources Needed", "5 stacks of wood, 10 stacks of cobblestone."));
        starterBase.getCustomFields().add(new CustomField("sda Needed", "5 stacks of wood, 10 stacks of cobblestone."));
        starterBase.getCustomFields().add(new CustomField("Resources Needed", "5 stacks of wood, 10 stacks of cobblestone."));
        starterBase.getCustomFields().add(new CustomField("Resources Needed", "5 stacks of wood, 10 stacks of cobblestone."));
        starterBase.getCustomFields().add(new CustomField("Resources fdsfNeeded", "5 stacks of wood, 10 stacks of cobblestone."));
        starterBase.getCustomFields().add(new CustomField("Resources Needed", "5 stacks of wood, 10 stacks of cobblestone."));
        starterBase.getCustomFields().add(new CustomField("sdfded", "5 stacks of wood, 10 stacks of cobblestone."));
        starterBase.getCustomFields().add(new CustomField("Resourdfces Needed", "5 stacks of wood, 10 stacks of cobblestone."));
        starterBase.getCustomFields().add(new CustomField("Resosdfurces Needed", "5 stacks of wood, 10 stacks of cobblestone."));
        this.builds.add(starterBase);

        Build netherHub = new Build("Nether Hub", "12, 80, -31", "The Nether", "Main hub for all nether portals.", "Player");
        this.builds.add(netherHub);
    }

    private void addDefaultNotes() {
        this.notes.add(new Note("Welcome!", "This is your first note.\nYou can edit or delete it."));
        this.notes.add(new Note("Shopping List", "- 1 Stack of Cobblestone\n- 32 Oak Logs\n- 8 Diamonds"));
        this.notes.add(new Note("Project: Castle Walls", "The east wall needs to be extended by 30 blocks. Use stone bricks and add mossy variants for detail."));
        this.notes.add(new Note("Test", "Content"));
        this.notes.add(new Note("Test", "Content"));
        this.notes.add(new Note("Test", "Content"));
        this.notes.add(new Note("Test", "Content"));
        this.notes.add(new Note("Test", "Content"));
        this.notes.add(new Note("Test", "Content"));
        this.notes.add(new Note("Test", "Content"));
        this.notes.add(new Note("Test", "Content"));
        this.notes.add(new Note("Test", "Content"));
        this.notes.add(new Note("Test", "Content"));
        this.notes.add(new Note("Test", "Content"));
        this.notes.add(new Note("Test", "Content"));
        this.notes.add(new Note("Test", "Content"));
        this.notes.add(new Note("Test", "Content"));
        this.notes.add(new Note("Test", "Content"));

    }
}