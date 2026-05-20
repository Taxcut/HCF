package com.testrank.hcf.core.conquest;

import com.testrank.hcf.core.api.HCFService;
import com.testrank.hcf.core.events.EventService;
import com.testrank.hcf.core.events.HCFEvent;
import com.testrank.hcf.core.events.HCFEventType;

import java.util.UUID;

public final class ConquestService implements HCFService {
    private final EventService events;

    public ConquestService(EventService events) {
        this.events = events;
    }

    public HCFEvent start(long durationMillis) {
        HCFEvent event = events.create(HCFEventType.CONQUEST, "Conquest");
        event.start(durationMillis);
        return event;
    }

    public void score(UUID teamId, int points) {
        events.active(HCFEventType.CONQUEST).ifPresent(event -> event.addScore(teamId, points));
    }
}
