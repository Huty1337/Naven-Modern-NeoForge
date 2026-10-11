package client.nilore.event.impl;

import lombok.*;
import client.nilore.event.EventMarker;

@Data
@AllArgsConstructor
public class FallFlyingEvent
implements EventMarker {
    @Getter @Setter
    private float pitch;
}