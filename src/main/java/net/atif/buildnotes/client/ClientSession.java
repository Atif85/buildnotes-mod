package net.atif.buildnotes.client;

import net.atif.buildnotes.data.PermissionLevel;

public class ClientSession {
    private static boolean onServer = false;
    private static PermissionLevel permissionLevel = PermissionLevel.VIEW_ONLY;

    public static void joinServer(PermissionLevel perms) {
        onServer = true;
        permissionLevel = perms;
        System.out.println("BuildNotes: Joined a compatible server with permission level: " + perms.toString());
    }

    public static void leaveServer() {
        onServer = false;
        permissionLevel = PermissionLevel.VIEW_ONLY;
        System.out.println("BuildNotes: Disconnected from server.");
    }

    public static boolean isOnServer() {
        return onServer;
    }

    public static PermissionLevel getPermissionLevel() {
        return permissionLevel;
    }

    public static boolean hasEditPermission() {
        return permissionLevel == PermissionLevel.CAN_EDIT;
    }
}