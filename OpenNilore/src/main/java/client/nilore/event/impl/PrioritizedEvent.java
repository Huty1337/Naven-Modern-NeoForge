package client.nilore.event.impl;

import client.nilore.event.EventMarker;
import client.nilore.event.Prioritized;

public abstract class PrioritizedEvent
implements Prioritized,
EventMarker {
    private final byte priority;

    protected PrioritizedEvent(byte by) {
        this.priority = by;
    }

    @Override
    public byte getPriority() {
        return this.priority;
    }
}