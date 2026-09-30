---
title: "Genuine Tame Activity"
published: true
draft: false
---
# Genuine tame activities

Tamework 4.2.1 exposes `TAME_ACQUISITION_ACTIVITY` when its Activity API V2 feed is
open. Subscribe to `ActivityDomain.TAMING` and select `ActivityIds.TAME_ACQUIRED`.
The payload is `TameAcquiredActivityView`: its header carries the operation ID,
time and feed sequence; the remaining fields identify the resulting role, owner
and companion.

The action follows a successful `Tame` interaction. Before mutation, the animal
must be untamed and unowned. After mutation, it must be tamed and owned by the
interacting player. It does not require a managed-content profile, population
group or admission provider.

Claiming an already-tamed animal, capturing or releasing it, loading a profile,
and administrative state writes do not emit this action. Existing mapped
`tamework:tame_success` activities retain their behavior, including legacy claims
and supported capture-and-tame operations. Integrations that need genuine
interaction tames should select the new action rather than count both.

The feed is process-local and best effort. Consumers should deduplicate by
operation ID and must not treat it as a durable replay log. Listener failures do
not undo a completed tame. A reload closes old subscriptions; register again
against the current API and check the capability before enabling this feature.
