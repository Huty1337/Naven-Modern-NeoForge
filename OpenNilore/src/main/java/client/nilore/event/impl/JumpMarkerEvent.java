package client.nilore.event.impl;

import lombok.AllArgsConstructor;
import lombok.Data;
import client.nilore.event.EventMarker;

@Data
@AllArgsConstructor
public class JumpMarkerEvent
implements EventMarker {
    private float yaw;
}