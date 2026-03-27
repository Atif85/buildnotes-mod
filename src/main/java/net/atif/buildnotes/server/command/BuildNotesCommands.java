package net.atif.buildnotes.server.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.atif.buildnotes.Buildnotes;
import net.atif.buildnotes.server.PermissionEntry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands; 
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.players.NameAndId;

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

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("buildnotes")
                // Only server operators (or permission level 2+) can use these commands
                // NEW
                .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                .then(Commands.literal("allow")
                        .then(Commands.argument("players", GameProfileArgument.gameProfile())
                                .executes(BuildNotesCommands::allowPlayer)))
                .then(Commands.literal("disallow")
                        .then(Commands.argument("players", GameProfileArgument.gameProfile())
                                .executes(BuildNotesCommands::disallowPlayer)))
                .then(Commands.literal("list")
                        .executes(BuildNotesCommands::listPlayers))
                .then(Commands.literal("allow_all")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(BuildNotesCommands::allowAll)))
        );
    }

    private static int allowPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<NameAndId> entries = GameProfileArgument.getGameProfiles(context, "players");
        CommandSourceStack source = context.getSource();

        List<String> addedPlayers = new ArrayList<>();
        List<String> alreadyAllowedPlayers = new ArrayList<>();

        for (NameAndId entry : entries) {
            GameProfile profile = new GameProfile(entry.id(), entry.name());
            if (Buildnotes.PERMISSION_MANAGER.addPlayer(profile)) {
                addedPlayers.add(profile.name());
            } else {
                alreadyAllowedPlayers.add(profile.name());
            }
        }

        // Report successfully added players
        if (!addedPlayers.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Added ")
                    .append(Component.literal(String.join(", ", addedPlayers)).withStyle(ChatFormatting.GREEN))
                    .append(" to the BuildNotes editor list."), true);
        }

        // Report players who were already on the list
        if (!alreadyAllowedPlayers.isEmpty()) {
            source.sendFailure(Component.literal(String.join(", ", alreadyAllowedPlayers) + " were already on the list."));
        }

        return addedPlayers.size();
    }

    private static int disallowPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<NameAndId> entries = GameProfileArgument.getGameProfiles(context, "players");
        CommandSourceStack source = context.getSource();

        List<String> removedPlayers = new ArrayList<>();
        List<String> notOnListPlayers = new ArrayList<>();

        for (NameAndId entry : entries) {
            GameProfile profile = new GameProfile(entry.id(), entry.name());
            if (Buildnotes.PERMISSION_MANAGER.removePlayer(profile)) {
                removedPlayers.add(profile.name());
            } else {
                notOnListPlayers.add(profile.name());
            }
        }

        // Report successfully removed players
        if (!removedPlayers.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Removed ")
                    .append(Component.literal(String.join(", ", removedPlayers)).withStyle(ChatFormatting.RED))
                    .append(" from the BuildNotes editor list."), true);
        }

        // Report players who were not on the list to begin with
        if (!notOnListPlayers.isEmpty()) {
            source.sendFailure(Component.literal(String.join(", ", notOnListPlayers) + " were not on the list."));
        }

        return removedPlayers.size();
    }

    private static int listPlayers(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        boolean allowAll = Buildnotes.PERMISSION_MANAGER.getAllowAll();
        if (allowAll) {
            source.sendSuccess(() -> Component.literal("Note: 'allow_all' is currently TRUE. All players can edit.").withStyle(ChatFormatting.GOLD), false);
        }

        Set<PermissionEntry> allowedPlayers = Buildnotes.PERMISSION_MANAGER.getAllowedPlayers();

        if (allowedPlayers.isEmpty()) {
            source.sendSuccess(() -> Component.literal("There are no players on the BuildNotes editor list."), false);
            return 1;
        }

        String playerNames = allowedPlayers.stream()
                .map(PermissionEntry::getName)
                .collect(Collectors.joining(", "));

        source.sendSuccess(() -> Component.literal("BuildNotes Editors: ").withStyle(ChatFormatting.YELLOW).append(Component.literal(playerNames)), false);
        return allowedPlayers.size();
    }

    private static int allowAll(CommandContext<CommandSourceStack> context) {
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        CommandSourceStack source = context.getSource();

        Buildnotes.PERMISSION_MANAGER.setAllowAll(enabled);

        if (enabled) {
            source.sendSuccess(() -> Component.literal("All players can now edit BuildNotes.").withStyle(ChatFormatting.GREEN), true);
        } else {
            source.sendSuccess(() -> Component.literal("Only players on the list (and OPs) can edit BuildNotes.").withStyle(ChatFormatting.RED), true);
        }
        return 1;
    }
}