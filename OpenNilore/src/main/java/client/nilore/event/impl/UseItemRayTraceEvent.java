package client.nilore.event.impl;

import lombok.*;
import client.nilore.event.EventMarker;

@AllArgsConstructor
@Data
public class UseItemRayTraceEvent
implements EventMarker {
    @Getter @Setter
    private float yaw;
    @Getter @Setter
    private float pitch;
}