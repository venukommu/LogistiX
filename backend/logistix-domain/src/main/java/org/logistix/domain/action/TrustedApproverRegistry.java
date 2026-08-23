package org.logistix.domain.action;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-process reference registry of trusted, authorized operational approver identities.
 * Replaces unverified plain-string approver claims with verified approval authority validation.
 */
public class TrustedApproverRegistry {

    private final Map<String, Set<ActionType>> approverAuthorities = new ConcurrentHashMap<>();

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

        return registry;
    }

    public static TrustedApproverRegistry empty() {
        return new TrustedApproverRegistry();
    }

    public void registerApprover(String approverId, Set<ActionType> allowedActionTypes) {
        Objects.requireNonNull(approverId, "approverId must not be null");
        Objects.requireNonNull(allowedActionTypes, "allowedActionTypes must not be null");
        approverAuthorities.put(approverId, Collections.unmodifiableSet(Set.copyOf(allowedActionTypes)));
    }

    public boolean isAuthorizedApprover(String approverId) {
        if (approverId == null) return false;
        return approverAuthorities.containsKey(approverId);
    }

    public boolean isAuthorizedApprover(String approverId, ActionType actionType) {
        if (approverId == null || actionType == null) return false;
        Set<ActionType> allowed = approverAuthorities.get(approverId);
        if (allowed == null) return false;
        return allowed.isEmpty() || allowed.contains(actionType);
    }

    public Set<String> getRegisteredApproverIds() {
        return Collections.unmodifiableSet(approverAuthorities.keySet());
    }
}
