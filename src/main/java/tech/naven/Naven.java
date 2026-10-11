package tech.naven;

import tech.naven.commands.CommandManager;
import tech.naven.events.api.EventManager;
import tech.naven.events.api.EventTarget;
import tech.naven.events.api.types.EventType;
import tech.naven.events.impl.EventRunTicks;
import tech.naven.events.impl.EventShutdown;
import tech.naven.files.FileManager;
import tech.naven.modules.ModuleManager;
import tech.naven.modules.impl.render.ClickGUIModule;
import tech.naven.ui.notification.NotificationManager;
import tech.naven.utils.EntityWatcher;
import tech.naven.utils.EventWrapper;
import tech.naven.utils.LogUtils;
import tech.naven.utils.NetworkUtils;
import tech.naven.utils.ServerUtils;
import tech.naven.utils.TickTimeHelper;
import tech.naven.utils.renderer.Fonts;
import tech.naven.utils.rotation.RotationManager;
import tech.naven.values.HasValueManager;
import tech.naven.values.ValueManager;
import java.awt.FontFormatException;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.neoforged.neoforge.common.NeoForge;

public class Naven {
   public static final String CLIENT_NAME = "Naven-Modern";
   public static final String CLIENT_DISPLAY_NAME = "Naven";
   private static Naven instance;
   private final EventManager eventManager;
   private final EventWrapper eventWrapper;
   private final ValueManager valueManager;
   private final HasValueManager hasValueManager;
   private final RotationManager rotationManager;
   public final ModuleManager moduleManager;
   private final CommandManager commandManager;
   private final FileManager fileManager;
   private final NotificationManager notificationManager;
   public static float TICK_TIMER = 1.0F;
   public static Queue<Runnable> skipTasks = new ConcurrentLinkedQueue<>();

   private Naven() {
      System.out.println("Naven Init");
      instance = this;
      this.eventManager = new EventManager();
      try {
         Fonts.loadFonts();
      } catch (IOException var2) {
         throw new RuntimeException(var2);
      } catch (FontFormatException var3) {
         throw new RuntimeException(var3);
      }

      this.eventWrapper = new EventWrapper();
      this.valueManager = new ValueManager();
      this.hasValueManager = new HasValueManager();
      this.moduleManager = new ModuleManager();
      this.rotationManager = new RotationManager();
      this.commandManager = new CommandManager();
      this.fileManager = new FileManager();
      this.notificationManager = new NotificationManager();
      this.fileManager.load();
      this.moduleManager.getModule(ClickGUIModule.class).setEnabled(false);
      this.eventManager.register(getInstance());
      this.eventManager.register(this.eventWrapper);
      this.eventManager.register(new RotationManager());
      this.eventManager.register(new NetworkUtils());
      this.eventManager.register(new ServerUtils());
      this.eventManager.register(new EntityWatcher());
      NeoForge.EVENT_BUS.register(this.eventWrapper);
   }

   public static void modRegister() {
      try {
         new Naven();
      } catch (Exception var1) {
         System.err.println("Failed to load client");
         var1.printStackTrace(System.err);
      }
   }

   @EventTarget
   public void onShutdown(EventShutdown e) {
      this.fileManager.save();
      LogUtils.close();
   }

   @EventTarget(0)
   public void onEarlyTick(EventRunTicks e) {
      if (e.getType() == EventType.PRE) {
         TickTimeHelper.update();
      }
   }

   public static Naven getInstance() {
      return instance;
   }

   public EventManager getEventManager() {
      return this.eventManager;
   }

   public EventWrapper getEventWrapper() {
      return this.eventWrapper;
   }

   public ValueManager getValueManager() {
      return this.valueManager;
   }

   public HasValueManager getHasValueManager() {
      return this.hasValueManager;
   }

   public RotationManager getRotationManager() {
      return this.rotationManager;
   }

   public ModuleManager getModuleManager() {
      return this.moduleManager;
   }

   public CommandManager getCommandManager() {
      return this.commandManager;
   }

   public FileManager getFileManager() {
      return this.fileManager;
   }

   public NotificationManager getNotificationManager() {
      return this.notificationManager;
   }
}
