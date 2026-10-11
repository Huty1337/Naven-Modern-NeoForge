package client.nilore.modules.impl.misc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import client.nilore.event.EventTarget;
import client.nilore.event.impl.PacketEvent;
import client.nilore.event.impl.TickEvent;
import client.nilore.event.impl.WorldChangeEvent;
import client.nilore.manager.ConfigManager;
import client.nilore.modules.Category;
import client.nilore.modules.Module;
import client.nilore.settings.impl.ModeSetting;
import client.nilore.settings.impl.NumberSetting;
import client.nilore.utils.misc.ChatUtil;

public class KillSay extends Module {
    public final ModeSetting fileStyle = new ModeSetting("Style", "naven-style", "southside-style").withDefault("naven-style");
    public final NumberSetting delay = new NumberSetting("Delay", 30, 0, 120, 1);
    private final List<String> messages = new ArrayList<>();
    private final Map<UUID, String> attacked = new HashMap<>();
    private final Queue<String> sendQueue = new LinkedList<>();
    private int ticks = 0;
    private int index = 0;
    private final File file = new File(ConfigManager.CONFIG_DIR, "killsay.txt");

    public KillSay() {
        super("KillSay", Category.MISC);
    }

    @Override
    public void onEnable() {
        loadFile();
    }

    @Override
    public void onDisable() {
        sendQueue.clear();
        attacked.clear();
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (mc.player == null || mc.level == null) return;
        if (!e.isIncoming() && e.getPacket() instanceof ServerboundInteractPacket packet) {
            packet.dispatch(new ServerboundInteractPacket.Handler() {
                @Override
                public void onAttack() {
                    if (mc.crosshairPickEntity instanceof Player playerTarget && playerTarget != mc.player) {
                        attacked.putIfAbsent(playerTarget.getUUID(), playerTarget.getName().getString());
                    }
                }

                @Override public void onInteraction(InteractionHand hand) {}
                @Override public void onInteraction(InteractionHand hand, Vec3 location) {}
            });
        }
    }

    @EventTarget
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.level == null) {
            attacked.clear();
            sendQueue.clear();
            return;
        }

        Iterator<Map.Entry<UUID, String>> it = attacked.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, String> entry = it.next();
            UUID uuid = entry.getKey();
            String name = entry.getValue();

            boolean found = false;
            for (Player player : mc.level.players()) {
                if (player.getUUID().equals(uuid)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                if (!name.equals(mc.player.getName().getString())) {
                    String msg = messages.get(index);
                    index++;
                    if (index >= messages.size()) index = 0;
                    sendQueue.add(format(msg, name));
                }
                it.remove();
            }
        }

        ticks++;
        int currentDelay = delay.getValue().intValue();
        if (currentDelay < 1) currentDelay = 1;
        if (!sendQueue.isEmpty() && ticks >= currentDelay) {
            var connection = mc.getConnection();
            if (connection != null) {
                String msg = sendQueue.poll();
                if (msg != null) {
                    connection.sendChat(msg);
                    ticks = 0;
                }
            }
        }
    }

    @EventTarget
    public void onWorldChange(WorldChangeEvent e) {
        sendQueue.clear();
        attacked.clear();
    }

    private String format(String msg, String name) {
        return switch (fileStyle.getValue()) {
            case "naven-style" -> msg.replace("%s", name);
            case "southside-style" -> msg.replace("{name}", name);
            default -> msg;
        };
    }

    private void loadFile() {
        try {
            if (!file.exists()) {
                ChatUtil.print("No killsay file found, creating....");
                createDefault();
            }
            messages.clear();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    messages.add(line);
                }
            }
        } catch (Exception e) {
            ChatUtil.print("Error while loading killsay file");
            setEnabled(false);
        }
    }

    private void createDefault() throws Exception {
        file.getParentFile().mkdirs();
        file.createNewFile();
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            bw.write("# KillSay config\n");
            bw.write("%s L\n");
            bw.write("%s fw\n");
            bw.write("%s 我喜欢你\n");
            bw.write("%s 我喜欢你♥\n");
            bw.write("%s 兄弟你好香\n");
            bw.write("%s 你好可爱\n");
            bw.write("%s 别急\n");
            bw.write("%s 你已被清朝杀手陈安健害死！快使用Hack Lunar！\n");
            bw.write("%s 你已被狂笑的蛇陈安健本人害死，快使用我编写的Hack Lunar端！\n");
        }
    }
}