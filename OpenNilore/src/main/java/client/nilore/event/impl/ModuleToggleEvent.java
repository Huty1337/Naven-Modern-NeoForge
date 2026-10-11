package client.nilore.event.impl;

import client.nilore.event.EventMarker;
import client.nilore.modules.Module;

public record ModuleToggleEvent(Module module, boolean enabled) implements EventMarker {
}
