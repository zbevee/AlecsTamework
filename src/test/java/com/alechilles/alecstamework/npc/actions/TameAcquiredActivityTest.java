package com.alechilles.alecstamework.npc.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alechilles.alecstamework.Tamework;
import com.alechilles.alecstamework.activity.ActivityRuntime;
import com.alechilles.alecstamework.api.ActivityView;
import com.alechilles.alecstamework.api.TameAcquiredActivityView;
import com.alechilles.alecstamework.config.managed.ManagedActivityConfigRegistry;
import com.alechilles.alecstamework.config.population.PopulationGroupConfigRegistry;
import com.alechilles.alecstamework.damage.SimpleClaimsDamageHytaleFixture;
import com.alechilles.alecstamework.npc.TamedStateResolver;
import com.alechilles.alecstamework.npc.components.TameworkOwnerComponent;
import com.alechilles.alecstamework.npc.components.TameworkTamedComponent;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.TestEntityComponentStore;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

/** Exercises the pre-mutation wild snapshot and the final successful-tame publication gate. */
class TameAcquiredActivityTest {
    private static final UUID OWNER = UUID.randomUUID();
    private static final UUID COMPANION = UUID.randomUUID();
    private final List<ActivityView> published = new ArrayList<>();

    @AfterEach
    void clearRuntime() {
        ActivityRuntime.clear();
    }

    @Test
    void firstWildTamePublishesAfterOwnershipAndTamedStateCommit() throws Exception {
        try (Fixture fixture = new Fixture()) {
            assertFalse(TamedStateResolver.isTamed(fixture.npcRef, fixture.store));
            assertNull(fixture.owner());
            fixture.tame(fixture.player);
            assertEquals(OWNER, fixture.owner());
            assertTrue(TamedStateResolver.isTamed(fixture.npcRef, fixture.store));
            assertEquals(1, published.size());
            TameAcquiredActivityView activity = assertInstanceOf(
                    TameAcquiredActivityView.class, published.getFirst());
            assertEquals(OWNER, activity.ownerId());
            assertEquals(COMPANION, activity.companionId());
            assertEquals("Tamed_Test", activity.roleId());

            fixture.tame(fixture.player);
            assertEquals(1, published.size(), "A second attempt must not acquire the same tame again");
        }
    }

    @Test
    void alreadyTamedButUnownedIsNotNewTame() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.store.put(fixture.npcRef, fixture.tamedType, new TameworkTamedComponent(true));
            fixture.tame(fixture.player);
            assertTrue(published.isEmpty());
            assertEquals(OWNER, fixture.owner(), "Existing ownership behavior stays intact");
        }
    }

    @Test
    void legacyTamedRoleWithoutComponentIsNotNewTame() throws Exception {
        try (Fixture fixture = new Fixture()) {
            NPCEntity npc = new NPCEntity();
            npc.setRoleName("Tamed_Test");
            fixture.store.put(fixture.npcRef, NPCEntity.getComponentType(), npc);
            fixture.tame(fixture.player);
            assertTrue(published.isEmpty());
        }
    }

    @Test
    void previouslyOwnedUntamedNpcIsNotNewTame() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.store.put(fixture.npcRef, TameworkOwnerComponent.getComponentType(),
                    new TameworkOwnerComponent(UUID.randomUUID(), null));
            fixture.tame(fixture.player);
            assertTrue(published.isEmpty());
        }
    }

    @Test
    void missingActorCannotPublish() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.tame(null);
            assertTrue(published.isEmpty());
        }
    }

    @Test
    void finalStateMustStillBeTamedAndOwnedByTheAcquiringActor() {
        installRuntime();
        InteractionExecutor.publishTameIfCommitted(UUID.randomUUID(), false,
                OWNER, OWNER, true, "Tamed_Test", COMPANION);
        InteractionExecutor.publishTameIfCommitted(UUID.randomUUID(), true,
                OWNER, OWNER, false, "Tamed_Test", COMPANION);
        InteractionExecutor.publishTameIfCommitted(UUID.randomUUID(), true,
                OWNER, UUID.randomUUID(), true, "Tamed_Test", COMPANION);
        InteractionExecutor.publishTameIfCommitted(UUID.randomUUID(), true,
                null, OWNER, true, "Tamed_Test", COMPANION);
        assertTrue(published.isEmpty());
    }

    private void installRuntime() {
        ActivityRuntime.install(published::add,
                new ManagedActivityConfigRegistry(new PopulationGroupConfigRegistry()));
    }

    private final class Fixture implements AutoCloseable {
        private final SimpleClaimsDamageHytaleFixture.HytaleModuleScope modules =
                SimpleClaimsDamageHytaleFixture.HytaleModuleScope.install();
        private final ComponentType<EntityStore, TameworkTamedComponent> tamedType = new ComponentType<>();
        private final TestEntityComponentStore store = new TestEntityComponentStore(new EntityStore(null));
        private final Ref<EntityStore> npcRef = store.createReference();
        private final Player player;
        private final Field statsInstance;
        private final Object previousStatsInstance;
        private final Field tranquilizerIndex;
        private final int previousTranquilizerIndex;

        Fixture() throws Exception {
            Field tamed = Tamework.class.getDeclaredField("tamedComponentType");
            tamed.setAccessible(true);
            tamed.set(Tamework.getInstance(), tamedType);
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Unsafe unsafe = (Unsafe) unsafeField.get(null);
            statsInstance = EntityStatsModule.class.getDeclaredField("instance");
            statsInstance.setAccessible(true);
            previousStatsInstance = statsInstance.get(null);
            statsInstance.set(null, unsafe.allocateInstance(EntityStatsModule.class));
            player = (Player) unsafe.allocateInstance(Player.class);
            player.setLegacyUUID(OWNER);
            tranquilizerIndex = InteractionStateEffects.class.getDeclaredField("tranquilizerEffectIndex");
            tranquilizerIndex.setAccessible(true);
            previousTranquilizerIndex = tranquilizerIndex.getInt(null);
            tranquilizerIndex.setInt(null, -1);
            installRuntime();
        }

        UUID owner() {
            TameworkOwnerComponent component = store.getComponent(
                    npcRef, TameworkOwnerComponent.getComponentType());
            return component == null ? null : component.getOwnerId();
        }

        void tame(Player actor) {
            assertTrue(new InteractionStateEffects().applyStartTaming(
                    npcRef, store, actor, (ref, liveStore, liveActor, acquired) ->
                            InteractionExecutor.publishTameIfCommitted(UUID.randomUUID(), acquired,
                                    liveActor == null ? null : liveActor.getUuid(), owner(),
                                    TamedStateResolver.isTamed(ref, liveStore), "Tamed_Test", COMPANION)));
        }

        @Override
        public void close() throws Exception {
            statsInstance.set(null, previousStatsInstance);
            tranquilizerIndex.setInt(null, previousTranquilizerIndex);
            store.close();
            modules.close();
        }
    }
}
