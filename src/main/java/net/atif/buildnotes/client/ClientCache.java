package net.atif.buildnotes.client;

import net.atif.buildnotes.data.Build;
import net.atif.buildnotes.data.Note;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A client-side cache for holding notes and builds received from a server.
 * This ensures that network-synced data is handled separately from local file I/O.
 */
public class ClientCache {

    // Using CopyOnWriteArrayList to prevent issues when iterating and modifying from different threads.
    private static final List<Note> serverNotes = new CopyOnWriteArrayList<>();
    private static final List<Build> serverBuilds = new CopyOnWriteArrayList<>();

    public static List<Note> getNotes() {
        return serverNotes;
    }

    public static List<Build> getBuilds() {
        return serverBuilds;
    }

    /**
     * Replaces the entire list of server notes with a new list from the server.
     * To be called on initial sync.
     */
    public static void setNotes(List<Note> notes) {
        serverNotes.clear();
        serverNotes.addAll(notes);
    }

    /**
     * Replaces the entire list of server builds with a new list from the server.
     * To be called on initial sync.
     */
    public static void setBuilds(List<Build> builds) {
        serverBuilds.clear();
        serverBuilds.addAll(builds);
    }

    /**
     * Adds a new entry or updates an existing one.
     */
    public static void addOrUpdateNote(Note note) {
        serverNotes.removeIf(n -> n.getId().equals(note.getId()));
        serverNotes.add(note);
    }

    public static void addOrUpdateBuild(Build build) {
        serverBuilds.removeIf(b -> b.getId().equals(build.getId()));
        serverBuilds.add(build);
    }

    /**
     * Deletes an entry by its ID.
     */
    public static void removeNote(Note note) {
        serverNotes.removeIf(n -> n.getId().equals(note.getId()));
    }

    public static void removeBuild(Build build) {
        serverBuilds.removeIf(b -> b.getId().equals(build.getId()));
    }

    // More specific removal methods
    public static void removeNoteById(UUID id) {
        serverNotes.removeIf(n -> n.getId().equals(id));
    }

    public static void removeBuildById(UUID id) {
        serverBuilds.removeIf(b -> b.getId().equals(id));
    }

    /**
     * Clears all cached data. To be called when disconnecting from a server.
     */
    public static void clear() {
        serverNotes.clear();
        serverBuilds.clear();
    }
}