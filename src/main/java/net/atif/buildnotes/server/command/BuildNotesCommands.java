package net.atif.buildnotes.server.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.atif.buildnotes.Buildnotes;
import net.atif.buildnotes.server.PermissionEntry;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import java.util.*;
import java.util.stream.Collectors;

public class BuildNotesCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            register(dispatcher);
        });
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("buildnotes")
                // Only server operators (or permission level 2+) can use these commands
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("allow")
                        .then(CommandManager.argument("players", GameProfileArgumentType.gameProfile())
                                .executes(BuildNotesCommands::allowPlayer)))
                .then(CommandManager.literal("disallow")
                        .then(CommandManager.argument("players", GameProfileArgumentType.gameProfile())
                                .executes(BuildNotesCommands::disallowPlayer)))
                .then(CommandManager.literal("list")
                        .executes(BuildNotesCommands::listPlayers))
                .then(CommandManager.literal("allow_all")
                        .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                .executes(BuildNotesCommands::allowAll)))
        );
    }

    private static int allowPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(context, "players");
        ServerCommandSource source = context.getSource();

        List<String> addedPlayers = new ArrayList<>();
        List<String> alreadyAllowedPlayers = new ArrayList<>();

        for (GameProfile profile : profiles) {
            if (Buildnotes.PERMISSION_MANAGER.addPlayer(profile)) {
                addedPlayers.add(profile.getName());
            } else {
                alreadyAllowedPlayers.add(profile.getName());
            }
        }

        // Report successfully added players
        if (!addedPlayers.isEmpty()) {
            source.sendFeedback(new LiteralText("Added ").append(new LiteralText(String.join(", ", addedPlayers)).formatted(Formatting.GREEN)).append(" to the BuildNotes editor list."), true);
        }

        // Report players who were already on the list
        if (!alreadyAllowedPlayers.isEmpty()) {
            source.sendError(new LiteralText(String.join(", ", alreadyAllowedPlayers) + " were already on the list."));
        }

        return addedPlayers.size();
    }

    private static int disallowPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(context, "players");
        ServerCommandSource source = context.getSource();

        List<String> removedPlayers = new ArrayList<>();
        List<String> notOnListPlayers = new ArrayList<>();

        for (GameProfile profile : profiles) {
            if (Buildnotes.PERMISSION_MANAGER.removePlayer(profile)) {
                removedPlayers.add(profile.getName());
            } else {
                notOnListPlayers.add(profile.getName());
            }
        }

        // Report successfully removed players
        if (!removedPlayers.isEmpty()) {
            source.sendFeedback(new LiteralText("Removed ").append(new LiteralText(String.join(", ", removedPlayers)).formatted(Formatting.RED)).append(" from the BuildNotes editor list."), true);
        }

        // Report players who were not on the list to begin with
        if (!notOnListPlayers.isEmpty()) {
            source.sendError(new LiteralText(String.join(", ", notOnListPlayers) + " were not on the list."));
        }

        return removedPlayers.size();
    }

    private static int listPlayers(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        boolean allowAll = Buildnotes.PERMISSION_MANAGER.getAllowAll();
        if (allowAll) {
            source.sendFeedback(new LiteralText("Note: 'allow_all' is currently TRUE. All players can edit.").formatted(Formatting.GOLD), false);
        }

        Set<PermissionEntry> allowedPlayers = Buildnotes.PERMISSION_MANAGER.getAllowedPlayers();

        if (allowedPlayers.isEmpty()) {
            source.sendFeedback(new LiteralText("There are no players on the BuildNotes editor list."), false);
            return 1;
        }

        String playerNames = allowedPlayers.stream()
                .map(PermissionEntry::getName)
                .collect(Collectors.joining(", "));

        source.sendFeedback(new LiteralText("BuildNotes Editors: ").formatted(Formatting.YELLOW).append(new LiteralText(playerNames)), false);
        return allowedPlayers.size();
    }

    private static int allowAll(CommandContext<ServerCommandSource> context) {
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        ServerCommandSource source = context.getSource();

        Buildnotes.PERMISSION_MANAGER.setAllowAll(enabled);

        if (enabled) {
            source.sendFeedback(new LiteralText("All players can now edit BuildNotes.").formatted(Formatting.GREEN), true);
        } else {
            source.sendFeedback(new LiteralText("Only players on the list (and OPs) can edit BuildNotes.").formatted(Formatting.RED), true);
        }
        return 1;
    }
}