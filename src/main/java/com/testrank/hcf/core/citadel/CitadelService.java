package com.testrank.hcf.core.citadel;

import com.testrank.hcf.core.api.HCFService;
import com.testrank.hcf.core.events.EventService;
import com.testrank.hcf.core.events.HCFEvent;
import com.testrank.hcf.core.events.HCFEventType;

public final class CitadelService implements HCFService {
    private final EventService events;

    public CitadelService(EventService events) {
        this.events = events;
    }

    public HCFEvent start(long durationMillis) {
        HCFEvent event = events.create(HCFEventType.CITADEL, "Citadel");
        event.start(durationMillis);
        return event;
    }
}
