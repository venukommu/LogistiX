package org.logistix.domain.action;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * In-process reference registry of trusted, authorized operational approver identities.
 * Enforces a strict configuration lifecycle: configure -> validate -> freeze -> runtime read-only.
 * Prevents unauthorized dynamic registration or runtime trust escalation.
 */
public class TrustedApproverRegistry {

    private final Map<String, Set<ActionType>> approverAuthorities = new ConcurrentHashMap<>();
    private final AtomicBoolean frozen = new AtomicBoolean(false);

    public static TrustedApproverRegistry withStandardLogisticsApprovers() {
        TrustedApproverRegistry registry = new TrustedApproverRegistry();

        Set<ActionType> allStandardActions = Set.of(
                ActionType.CHANGE_DELIVERY_APPOINTMENT,
                ActionType.ASSIGN_DRIVER,
                ActionType.UPDATE_SHIPMENT_STATUS
        );

        registry.registerApprover("SUPERVISOR-001", allStandardActions);
        registry.registerApprover("Supervisor-Jane", allStandardActions);
        registry.registerApprover("Supervisor-Bob", allStandardActions);
        registry.registerApprover("Manager-Alice", allStandardActions);
        registry.registerApprover("PHARMACY-APPROVER-001", Set.of(ActionType.CHANGE_DELIVERY_APPOINTMENT));
        registry.registerApprover("FLEET-MANAGER-001", Set.of(ActionType.ASSIGN_DRIVER));

        registry.freeze();
        return registry;
    }

    public static TrustedApproverRegistry empty() {
        return new TrustedApproverRegistry();
    }

    public synchronized void registerApprover(String approverId, Set<ActionType> allowedActionTypes) {
        if (frozen.get()) {
            throw new IllegalStateException("Security Guardrail: Cannot register approver [" + approverId +
                    "]. TrustedApproverRegistry is frozen and immutable.");
        }
        if (approverId == null || approverId.isBlank()) {
            throw new IllegalArgumentException("approverId must not be null or blank");
        }
        if (allowedActionTypes == null || allowedActionTypes.isEmpty()) {
            throw new IllegalArgumentException("allowedActionTypes must not be null or empty");
        }
        if (approverAuthorities.containsKey(approverId)) {
            throw new IllegalArgumentException("Approver [" + approverId + "] is already registered. Duplicate registrations are prohibited.");
        }
        approverAuthorities.put(approverId, Set.copyOf(allowedActionTypes));
    }

    public void freeze() {
        frozen.set(true);
    }

    public boolean isFrozen() {
        return frozen.get();
    }

    public boolean isAuthorizedApprover(String approverId) {
        if (approverId == null || approverId.isBlank()) return false;
        return approverAuthorities.containsKey(approverId);
    }

    public boolean isAuthorizedApprover(String approverId, ActionType actionType) {
        if (approverId == null || approverId.isBlank() || actionType == null) return false;
        Set<ActionType> allowed = approverAuthorities.get(approverId);
        if (allowed == null) return false;
        if (allowed.isEmpty()) return true;
        for (ActionType type : allowed) {
            if (type.equals(actionType) || type.code().equalsIgnoreCase(actionType.code())) {
                return true;
            }
        }
        return false;
    }

    public Set<String> getRegisteredApproverIds() {
        return Set.copyOf(approverAuthorities.keySet());
    }

    public Set<ActionType> getAllowedActionTypes(String approverId) {
        if (approverId == null) return Set.of();
        Set<ActionType> types = approverAuthorities.get(approverId);
        return types != null ? types : Set.of();
    }
}
