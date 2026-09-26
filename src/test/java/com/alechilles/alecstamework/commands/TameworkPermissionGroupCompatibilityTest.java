package com.alechilles.alecstamework.commands;

import com.hypixel.hytale.server.core.command.system.CommandOwner;
import com.hypixel.hytale.server.core.permissions.PermissionHolder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TameworkPermissionGroupCompatibilityTest {
    @Test
    void commandTreeCoexistsWithLowercaseAdminVirtualGroup() {
        TameworkCommandRoot root = new TameworkCommandRoot();
        root.setOwner(new CommandOwner() {
            @Override public String getName() { return "Alechilles:Alec's Tamework!"; }
        });
        var groups = new HashMap<>(root.getPermissionGroupsRecursive());
        groups.computeIfAbsent("admin", ignored -> new HashSet<>()).add("endlesslink.admin.fixture");
        // LuckPerms lowercases virtual group keys and rejects duplicate normalized keys.
        assertDoesNotThrow(() -> groups.keySet().stream().collect(Collectors.toMap(
                group -> group.toLowerCase(Locale.ROOT), Function.identity())));
        assertTrue(groups.get("admin").contains(TameworkCommandRoot.ROOT_PERMISSION));
        assertTrue(groups.get("admin").contains(TameworkApiTestPermission.NODE));
        assertTrue(groups.containsKey("hytale:Admin"));
        assertTrue(groups.containsKey("OP"));
        assertTrue(groups.containsKey("Operator"));
    }

    @Test
    void legacyPermissionNodesRemainAcceptedWithoutGrantingOrdinaryPlayersAccess() {
        for (String granted : Set.of("tamework.config", "admin", "Admin", "op", "OP", "operator", "Operator")) {
            assertTrue(TameworkConfigPermission.hasAccess(holder(granted)), granted);
        }
        assertFalse(TameworkConfigPermission.hasAccess(holder("unrelated.permission")));
    }

    private static PermissionHolder holder(String granted) {
        return new PermissionHolder() {
            @Override public boolean hasPermission(String id) { return id.equals(granted); }
            @Override public boolean hasPermission(String id, boolean fallback) { return hasPermission(id); }
        };
    }
}
