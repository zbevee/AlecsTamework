package com.alechilles.alecstamework.metrics;

import com.alechilles.beacon.commands.TelemetryCommandRoot;
import com.alechilles.beacon.runtime.host.TelemetryCommandRuntime;
import com.hypixel.hytale.server.core.command.system.CommandOwner;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BeaconPermissionCompatibilityTest {
    @Test
    void doesNotTouchAnotherPluginsBeaconCommandOrMissingCommand() {
        var other = new OtherBeacon();
        BeaconPermissionCompatibility.normalize(other);
        BeaconPermissionCompatibility.normalize(null);
        assertEquals(java.util.List.of("Admin"), other.getPermissionGroups());
    }

    @Test
    void actualEmbeddedCommandTreeCoexistsWithLowercaseAdmin() {
        var root = command();
        Map<String, Set<String>> before = normalized(root.getPermissionGroupsRecursive());
        BeaconPermissionCompatibility.normalize(root);
        var groups = root.getPermissionGroupsRecursive();
        assertEquals(before, normalized(groups), "Every permission grant must be preserved");
        groups.computeIfAbsent("admin", ignored -> new HashSet<>()).add("endlesslink.admin.fixture");
        assertDoesNotThrow(() -> groups.keySet().stream().collect(Collectors.toMap(
                group -> group.toLowerCase(Locale.ROOT), Function.identity())));
        assertTrue(groups.get("admin").contains("beacon.command.beacon.server.verify"));
        assertTrue(groups.get("admin").contains("beacon.command.beacon.reports"));
        assertFalse(groups.containsKey("Admin"));
        BeaconPermissionCompatibility.normalize(root);
        assertEquals(before, normalized(root.getPermissionGroupsRecursive()), "Normalization is idempotent");
    }

    private static Map<String, Set<String>> normalized(Map<String, Set<String>> source) {
        Map<String, Set<String>> result = new HashMap<>();
        source.forEach((key, value) -> result.computeIfAbsent(key.toLowerCase(Locale.ROOT),
                ignored -> new HashSet<>()).addAll(value));
        return result;
    }

    private static TelemetryCommandRoot command() {
        var runtime = (TelemetryCommandRuntime) Proxy.newProxyInstance(
                TelemetryCommandRuntime.class.getClassLoader(), new Class<?>[]{TelemetryCommandRuntime.class},
                (proxy, method, args) -> { throw new AssertionError("Registration must not execute runtime actions"); });
        var root = new TelemetryCommandRoot(runtime);
        root.setOwner(new CommandOwner() {
            @Override public String getName() { return "Beacon"; }
        });
        return root;
    }

    private static final class OtherBeacon extends com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection {
        OtherBeacon() {
            super("beacon", "Other provider fixture");
            setPermissionGroups("Admin");
        }
    }
}
