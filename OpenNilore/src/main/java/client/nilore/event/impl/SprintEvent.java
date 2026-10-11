package client.nilore.event.impl;

import client.nilore.event.EventMarker;

public class SprintEvent implements EventMarker {
    private boolean sprint;
    private Source source;

    public SprintEvent() {
        this(true, Source.INPUT);
    }

    public SprintEvent(boolean sprint, Source source) {
        this.sprint = sprint;
        this.source = source;
    }

    public boolean isSprint() {
        return sprint;
    }

    public void setSprint(boolean sprint) {
        this.sprint = sprint;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public enum Source {
        INPUT
    }
}
