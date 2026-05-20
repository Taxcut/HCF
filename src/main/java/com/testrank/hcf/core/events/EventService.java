package com.testrank.hcf.core.events;

import com.testrank.hcf.core.api.HCFService;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EventService implements HCFService {
    private final Map<UUID, HCFEvent> events = new ConcurrentHashMap<>();

    public HCFEvent create(HCFEventType type, String name) {
        HCFEvent event = new HCFEvent(type, name);
        events.put(event.id(), event);
        return event;
    }

    public Optional<HCFEvent> active(HCFEventType type) {
        return events.values().stream().filter(event -> event.type() == type && event.active()).findFirst();
    }

    public Collection<HCFEvent> events() {
        return events.values();
    }
}
