package org.logistix.engine.action;

import org.logistix.domain.action.ActionAuditEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory thread-safe store recording complete Action lifecycle audit entries.
 */
public class InMemoryActionAuditStore {

    private final List<ActionAuditEntry> entries = new CopyOnWriteArrayList<>();

    public void record(ActionAuditEntry entry) {
        if (entry != null) {
            entries.add(entry);
        }
    }

    public List<ActionAuditEntry> getEntries() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public Optional<ActionAuditEntry> findByActionId(String actionId) {
        return entries.stream()
                .filter(e -> e.actionId().equals(actionId))
                .findFirst();
    }

    public Optional<ActionAuditEntry> findLatestByActionId(String actionId) {
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (entries.get(i).actionId().equals(actionId)) {
                return Optional.of(entries.get(i));
            }
        }
        return Optional.empty();
    }

    public List<ActionAuditEntry> findAllByActionId(String actionId) {
        return entries.stream()
                .filter(e -> e.actionId().equals(actionId))
                .toList();
    }

    public Optional<ActionAuditEntry> findByCorrelationId(String correlationId) {
        return entries.stream()
                .filter(e -> e.correlationId().equals(correlationId))
                .findFirst();
    }

    public void clear() {
        entries.clear();
    }
}
