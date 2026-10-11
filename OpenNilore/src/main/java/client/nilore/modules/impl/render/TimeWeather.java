package client.nilore.modules.impl.render;

import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import client.nilore.event.impl.GameTickEvent;
import client.nilore.event.impl.ReceivePacketEvent;
import client.nilore.modules.Category;
import client.nilore.modules.Module;
import client.nilore.settings.impl.ModeSetting;
import client.nilore.settings.impl.NumberSetting;
import client.nilore.event.EventTarget;

public class TimeWeather extends Module {

    private final ModeSetting timeMode = new ModeSetting("Time", "Day", "Sunset", "Night", "Custom", "Vanilla")
            .withDefault("Vanilla");
    private final NumberSetting customTime = new NumberSetting("Custom Time", 6000, 0, 24000, 100,
            () -> timeMode.is("Custom"));

    private final ModeSetting weatherMode = new ModeSetting("Weather", "Clear", "Rain", "Thunder", "Vanilla")
            .withDefault("Clear");

    public TimeWeather() {
        super("TimeWeather", Category.RENDER);
    }

    @EventTarget
    public void onGameTick(GameTickEvent event) {
        if (mc.level == null) return;

        // 设置时间
        String time = timeMode.getValue();
        if (!"Vanilla".equals(time)) {
            long dayTime = switch (time) {
                case "Day" -> 1000L;
                case "Sunset" -> 13000L;
                case "Night" -> 18000L;
                case "Custom" -> customTime.getValue().longValue();
                default -> 0L;
            };
            mc.level.setDayTime(dayTime);
        }

        // 设置天气
        String weather = weatherMode.getValue();
        if (!"Vanilla".equals(weather)) {
            switch (weather) {
                case "Clear" -> {
                    mc.level.rainLevel = 0.0f;
                    mc.level.oRainLevel = 0.0f;
                    mc.level.thunderLevel = 0.0f;
                    mc.level.oThunderLevel = 0.0f;
                }
                case "Rain" -> {
                    mc.level.rainLevel = 1.0f;
                    mc.level.oRainLevel = 1.0f;
                    mc.level.thunderLevel = 0.0f;
                    mc.level.oThunderLevel = 0.0f;
                }
                case "Thunder" -> {
                    mc.level.rainLevel = 1.0f;
                    mc.level.oRainLevel = 1.0f;
                    mc.level.thunderLevel = 1.0f;
                    mc.level.oThunderLevel = 1.0f;
                }
            }
        }
    }

    @EventTarget
    public void onReceivePacket(ReceivePacketEvent event) {
        if (mc.level == null) return;

        // 拦截服务端时间同步包，防止覆盖我们的时间设置
        if (event.getPacket() instanceof ClientboundSetTimePacket
                && !"Vanilla".equals(timeMode.getValue())) {
            event.setCancelled(true);
            return;
        }

        // 只拦截天气相关的事件包，不影响其他游戏事件（模式切换、重生等）
        if (event.getPacket() instanceof ClientboundGameEventPacket gameEvent
                && !"Vanilla".equals(weatherMode.getValue())) {
            ClientboundGameEventPacket.Type type = gameEvent.getEvent();
            if (type == ClientboundGameEventPacket.START_RAINING
                    || type == ClientboundGameEventPacket.STOP_RAINING
                    || type == ClientboundGameEventPacket.RAIN_LEVEL_CHANGE
                    || type == ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE) {
                event.setCancelled(true);
            }
        }
    }
}