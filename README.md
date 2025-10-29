# BuildNotes

A lightweight in-game notetaking and build tracking mod for Minecraft, built for the Fabric loader. BuildNotes provides a seamless way to document your ideas, track build progress, and share information without ever leaving the game. It works in singleplayer and can be installed on a server for shared, collaborative notes.

![Screenshot Placeholder]()

## Features

BuildNotes is designed to be a flexible tool for players and server administrators alike. All notes and builds are categorized by "Scope," which determines where they are saved and who can see them.

#### **Note & Build Scopes**

*   **World Scope:** The default for single-player. Notes and builds are saved within a specific world's folder, keeping them tied to that world.
*   **Global Scope:** Notes and builds are saved to your global Minecraft config folder. This makes them accessible across all your worlds and even on servers, acting as a personal, persistent in-game notepad.
*   **Server Scope:** When both the client and a dedicated server have the mod, notes and builds can be saved to the server itself. These are visible to all connected players who also have the mod, making it a perfect tool for community noticeboards, server tours, or collaborative project planning.

#### **Build Tracker**

The build tracker is more than just a simple note. It includes dedicated fields for:
*   Build Name
*   Coordinates & Dimension
*   Description & Designer Credits
*   **Image Gallery:** Upload multiple screenshots for each build to visually track its progress or show it off.
*   **Custom Fields:** Add any additional fields you need for tracking materials, farm rates, or other project-specific data.

#### **Server Administration & Permissions**

When installed on a Fabric server, BuildNotes offers a complete permission system to control who can create, edit, and delete server-scoped notes.

*   **Operator Access:** Server operators can edit server notes by default.
*   **Permission Commands:** A robust command system allows OPs to grant or revoke editing permissions for specific players.
*   **Allow All Mode:** A simple command toggle to allow every player on the server to edit notes, perfect for smaller, trusted communities.

**Admin Commands:**
```
/buildnotes allow <player>
/buildnotes disallow <player>
/buildnotes list
/buildnotes allow_all <true|false>
```

## Screenshots

| Main Screen | Note Editor | Build Editor |
| :---: | :---: | :---: |
| ![Main Screen]() | ![Note Editor]() | ![Build Editor]() |


## Download

You can download the latest version of BuildNotes from:

*   **[Modrinth (Link Here)]**
*   **[CurseForge (Link Here)]**


## Dependencies

*   **Fabric API:** Required for all features.

## Future Plans

-   An in-game configuration screen for clientside settings.
-   The ability to categorize or tag notes for better organization.
-   Potential integrations with other mods (e.g., world map mods for waypoint creation).

## License
This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.