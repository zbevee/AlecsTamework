package com.alechilles.alecstamework.api;

import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nonnull;

/** A wild, unowned NPC became tamed and owned through a successful Tame interaction. */
public record TameAcquiredActivityView(
        @Nonnull ActivityHeader header,
        @Nonnull String roleId,
        @Nonnull UUID ownerId,
        @Nonnull UUID companionId
) implements ActivityView {
    public TameAcquiredActivityView {
        header = Objects.requireNonNull(header, "header");
        roleId = Objects.requireNonNull(roleId, "roleId").trim();
        if (roleId.isEmpty()) {
            throw new IllegalArgumentException("roleId is required.");
        }
        ownerId = Objects.requireNonNull(ownerId, "ownerId");
        companionId = Objects.requireNonNull(companionId, "companionId");
    }

    @Override
    @Nonnull
    public ActivityDomain domain() {
        return ActivityDomain.TAMING;
    }

    @Override
    @Nonnull
    public TameAcquiredActivityView withHeader(@Nonnull ActivityHeader nextHeader) {
        return new TameAcquiredActivityView(nextHeader, roleId, ownerId, companionId);
    }
}
