package com.alechilles.alecstamework.metrics;

import com.alechilles.beacon.commands.TelemetryCommandRoot;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandManager;

/** Keeps this embedded Beacon copy compatible with case-insensitive virtual groups. */
final class BeaconPermissionCompatibility {
    private BeaconPermissionCompatibility() { }

    static void normalizeRegisteredCommand() {
        CommandManager manager = CommandManager.get();
        if (manager != null) normalize(manager.getCommandRegistration().get("beacon"));
    }

    static void normalize(AbstractCommand command) {
        // Other plugins may own /beacon. Change only this embedded runtime's command tree.
        if (command instanceof TelemetryCommandRoot) normalizeTree(command);
    }

    private static void normalizeTree(AbstractCommand command) {
        var groups = command.getPermissionGroups();
        if (groups != null) {
            for (int i = 0; i < groups.size(); i++) {
                if ("Admin".equals(groups.get(i))) groups.set(i, "admin");
            }
        }
        command.getSubCommands().values().forEach(BeaconPermissionCompatibility::normalizeTree);
    }
}
