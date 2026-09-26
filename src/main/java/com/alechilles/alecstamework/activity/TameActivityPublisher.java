package com.alechilles.alecstamework.activity;

import com.alechilles.alecstamework.api.ActivityHeader;
import com.alechilles.alecstamework.api.ActivityIds;
import com.alechilles.alecstamework.api.ActivityDomain;
import com.alechilles.alecstamework.api.TameAcquiredActivityView;
import com.alechilles.alecstamework.api.TameActivityView;
import com.alechilles.alecstamework.api.internal.LiveActivityFeed;
import com.alechilles.alecstamework.config.managed.ManagedActivityConfigRegistry;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Publishes committed wild-to-tamed acquisitions. */
public final class TameActivityPublisher {
    private final LiveActivityFeed.Publisher publisher;
    private final ManagedActivityConfigRegistry managedActivities;

    public TameActivityPublisher(
            @Nonnull LiveActivityFeed.Publisher publisher,
            @Nonnull ManagedActivityConfigRegistry managedActivities
    ) {
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.managedActivities = Objects.requireNonNull(
                managedActivities, "managedActivities");
    }

    /** Publishes an actual Tame interaction, then preserves the optional managed activity. */
    public void publishAcquired(
            @Nonnull UUID operationId,
            @Nullable String roleId,
            @Nullable UUID ownerId,
            @Nullable UUID companionId
    ) {
        if (operationId == null || roleId == null || roleId.isBlank()
                || ownerId == null || companionId == null) {
            return;
        }
        try {
            if (publisher.hasInterest(ActivityDomain.TAMING, ActivityIds.TAME_ACQUIRED)) {
                publisher.publish(new TameAcquiredActivityView(
                        new ActivityHeader(operationId, ActivityIds.TAME_ACQUIRED, Instant.now()),
                        roleId, ownerId, companionId));
            }
        } catch (RuntimeException | LinkageError ignored) {
            // A listener cannot undo taming or suppress the existing managed activity.
        }
        publish(operationId, roleId, ownerId, companionId);
    }

    /** Publishes one acquisition after owner and tame state commit. */
    public void publish(
            @Nonnull UUID operationId,
            @Nullable String roleId,
            @Nullable UUID ownerId,
            @Nullable UUID companionId
    ) {
        if (operationId == null || roleId == null || roleId.isBlank()
                || ownerId == null || companionId == null) {
            return;
        }
        ManagedActivityConfigRegistry.RoleResolution resolution =
                managedActivities.resolveRole(roleId.trim()).orElse(null);
        if (resolution == null || resolution.family() == null
                || resolution.profile().activities().tameSuccess() == null) {
            return;
        }
        try {
            publisher.publish(new TameActivityView(
                    new ActivityHeader(
                            operationId, ActivityIds.TAME_SUCCESS, Instant.now()),
                    resolution.profile().profileId(),
                    Set.of(resolution.family().groupId()),
                    resolution.roleId(),
                    ownerId,
                    companionId,
                    resolution.profile().activities().tameSuccess()
            ));
        } catch (RuntimeException | LinkageError ignored) {
            // Publication cannot undo the committed acquisition.
        }
    }
}
