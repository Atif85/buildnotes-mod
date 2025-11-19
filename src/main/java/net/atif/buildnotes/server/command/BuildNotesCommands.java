package net.atif.buildnotes.server.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.atif.buildnotes.Buildnotes;
import net.atif.buildnotes.server.PermissionEntry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class BuildNotesCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> register(dispatcher)
        );
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
        Collection<PlayerConfigEntry> entries = GameProfileArgumentType.getProfileArgument(context, "players");
        ServerCommandSource source = context.getSource();

        List<String> addedPlayers = new ArrayList<>();
        List<String> alreadyAllowedPlayers = new ArrayList<>();

        for (PlayerConfigEntry entry : entries) {
            GameProfile profile = new GameProfile(entry.id(), entry.name());
            if (Buildnotes.PERMISSION_MANAGER.addPlayer(profile)) {
                addedPlayers.add(profile.name());
            } else {
                alreadyAllowedPlayers.add(profile.name());
            }
        }

        // Report successfully added players
        if (!addedPlayers.isEmpty()) {
            source.sendFeedback(() -> Text.literal("Added ").append(Text.literal(String.join(", ", addedPlayers)).formatted(Formatting.GREEN)).append(" to the BuildNotes editor list."), true);
        }

        // Report players who were already on the list
        if (!alreadyAllowedPlayers.isEmpty()) {
            source.sendError(Text.literal(String.join(", ", alreadyAllowedPlayers) + " were already on the list."));
        }

        return addedPlayers.size();
    }

    private static int disallowPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Collection<PlayerConfigEntry> entries = GameProfileArgumentType.getProfileArgument(context, "players");
        ServerCommandSource source = context.getSource();

        List<String> removedPlayers = new ArrayList<>();
        List<String> notOnListPlayers = new ArrayList<>();

        for (PlayerConfigEntry entry : entries) {
            GameProfile profile = new GameProfile(entry.id(), entry.name());
            if (Buildnotes.PERMISSION_MANAGER.removePlayer(profile)) {
                removedPlayers.add(profile.name());
            } else {
                notOnListPlayers.add(profile.name());
            }
        }

        // Report successfully removed players
        if (!removedPlayers.isEmpty()) {
            source.sendFeedback(() -> Text.literal("Removed ").append(Text.literal(String.join(", ", removedPlayers)).formatted(Formatting.RED)).append(" from the BuildNotes editor list."), true);
        }

        // Report players who were not on the list to begin with
        if (!notOnListPlayers.isEmpty()) {
            source.sendError(Text.literal(String.join(", ", notOnListPlayers) + " were not on the list."));
        }

        return removedPlayers.size();
    }

    private static int listPlayers(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        boolean allowAll = Buildnotes.PERMISSION_MANAGER.getAllowAll();
        if (allowAll) {
            source.sendFeedback(() -> Text.literal("Note: 'allow_all' is currently TRUE. All players can edit.").formatted(Formatting.GOLD), false);
        }

        Set<PermissionEntry> allowedPlayers = Buildnotes.PERMISSION_MANAGER.getAllowedPlayers();

        if (allowedPlayers.isEmpty()) {
            source.sendFeedback(() -> Text.literal("There are no players on the BuildNotes editor list."), false);
            return 1;
        }

        String playerNames = allowedPlayers.stream()
                .map(PermissionEntry::getName)
                .collect(Collectors.joining(", "));

        source.sendFeedback(() -> Text.literal("BuildNotes Editors: ").formatted(Formatting.YELLOW).append(Text.literal(playerNames)), false);
        return allowedPlayers.size();
    }

    private static int allowAll(CommandContext<ServerCommandSource> context) {
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        ServerCommandSource source = context.getSource();

        Buildnotes.PERMISSION_MANAGER.setAllowAll(enabled);

        if (enabled) {
            source.sendFeedback(() -> Text.literal("All players can now edit BuildNotes.").formatted(Formatting.GREEN), true);
        } else {
            source.sendFeedback(() -> Text.literal("Only players on the list (and OPs) can edit BuildNotes.").formatted(Formatting.RED), true);
        }
        return 1;
    }
}