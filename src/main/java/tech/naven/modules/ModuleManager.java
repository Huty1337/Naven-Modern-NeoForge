package tech.naven.modules;

import tech.naven.Naven;
import tech.naven.events.api.EventTarget;
import tech.naven.events.impl.EventKey;
import tech.naven.events.impl.EventMouseClick;
import tech.naven.exceptions.NoSuchModuleException;
import tech.naven.modules.impl.combat.AimAssist;
import tech.naven.modules.impl.combat.AntiBots;
import tech.naven.modules.impl.combat.AttackCrystal;
import tech.naven.modules.impl.combat.Aura;
import tech.naven.modules.impl.combat.AutoClicker;
import tech.naven.modules.impl.combat.Velocity;
import tech.naven.modules.impl.misc.AntiFireball;
import tech.naven.modules.impl.misc.AutoTools;
import tech.naven.modules.impl.misc.ChestStealer;
import tech.naven.modules.impl.misc.ClientFriend;
import tech.naven.modules.impl.misc.Disabler;
import tech.naven.modules.impl.misc.FastPlace;
import tech.naven.modules.impl.misc.Helper;
import tech.naven.modules.impl.misc.InventoryCleaner;
import tech.naven.modules.impl.misc.ItemTracker;
import tech.naven.modules.impl.misc.KillSay;
import tech.naven.modules.impl.misc.Spammer;
import tech.naven.modules.impl.misc.Teams;
import tech.naven.modules.impl.move.AutoMLG;
import tech.naven.modules.impl.move.Blink;
import tech.naven.modules.impl.move.FastWeb;
import tech.naven.modules.impl.move.LongJump;
import tech.naven.modules.impl.move.NoJumpDelay;
import tech.naven.modules.impl.move.NoSlow;
import tech.naven.modules.impl.move.SafeWalk;
import tech.naven.modules.impl.move.Scaffold;
import tech.naven.modules.impl.move.Sprint;
import tech.naven.modules.impl.move.Stuck;
import tech.naven.modules.impl.render.AntiBlindness;
import tech.naven.modules.impl.render.AntiNausea;
import tech.naven.modules.impl.render.ChestESP;
import tech.naven.modules.impl.render.ClickGUIModule;
import tech.naven.modules.impl.render.Compass;
import tech.naven.modules.impl.render.EffectDisplay;
import tech.naven.modules.impl.render.FullBright;
import tech.naven.modules.impl.render.Glow;
import tech.naven.modules.impl.render.HUD;
import tech.naven.modules.impl.render.ItemTags;
import tech.naven.modules.impl.render.MotionBlur;
import tech.naven.modules.impl.render.NameProtect;
import tech.naven.modules.impl.render.NameTags;
import tech.naven.modules.impl.render.NoHurtCam;
import tech.naven.modules.impl.render.NoRender;
import tech.naven.modules.impl.render.PostProcess;
import tech.naven.modules.impl.render.Projectile;
import tech.naven.modules.impl.render.Scoreboard;
import tech.naven.modules.impl.render.ScoreboardSpoof;
import tech.naven.modules.impl.render.TimeChanger;
import tech.naven.modules.impl.render.ViewClip;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModuleManager {
   private static final Logger log = LogManager.getLogger(ModuleManager.class);
   private final List<Module> modules = new ArrayList<>();
   private final Map<Class<? extends Module>, Module> classMap = new HashMap<>();
   private final Map<String, Module> nameMap = new HashMap<>();

   public ModuleManager() {
      try {
         this.initModules();
         this.modules.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
      } catch (Exception var2) {
         log.error("Failed to initialize modules", var2);
         throw new RuntimeException(var2);
      }

      Naven.getInstance().getEventManager().register(this);
   }

   private void initModules() {
      this.registerModule(
         new Aura(),
         new HUD(),
         new Velocity(),
         new NameTags(),
         new ChestStealer(),
         new InventoryCleaner(),
         new Scaffold(),
         new AntiBots(),
         new Sprint(),
         new ChestESP(),
         new ClickGUIModule(),
         new Teams(),
         new Glow(),
         new ItemTracker(),
         new AutoMLG(),
         new ClientFriend(),
         new NoJumpDelay(),
         new FastPlace(),
         new AntiFireball(),
         new Stuck(),
         new ScoreboardSpoof(),
         new AutoTools(),
         new ViewClip(),
         new Disabler(),
         new Projectile(),
         new TimeChanger(),
         new FullBright(),
         new NameProtect(),
         new NoHurtCam(),
         new AutoClicker(),
         new AntiBlindness(),
         new AntiNausea(),
         new Scoreboard(),
         new Compass(),
         new Spammer(),
         new KillSay(),
         new Blink(),
         new FastWeb(),
         new PostProcess(),
         new AttackCrystal(),
         new EffectDisplay(),
         new NoRender(),
         new ItemTags(),
         new SafeWalk(),
         new AimAssist(),
         new MotionBlur(),
         new Helper(),
         new NoSlow(),
         new LongJump()
      );
   }

   private void registerModule(Module... modules) {
      for (Module module : modules) {
         this.registerModule(module);
      }
   }

   private void registerModule(Module module) {
      module.initModule();
      this.modules.add(module);
      this.classMap.put((Class<? extends Module>)module.getClass(), module);
      this.nameMap.put(module.getName().toLowerCase(), module);
   }

   public List<Module> getModulesByCategory(Category category) {
      List<Module> modules = new ArrayList<>();

      for (Module module : this.modules) {
         if (module.getCategory() == category) {
            modules.add(module);
         }
      }

      return modules;
   }

   public Module getModule(Class<? extends Module> clazz) {
      Module module = this.classMap.get(clazz);
      if (module == null) {
         throw new NoSuchModuleException();
      } else {
         return module;
      }
   }

   public Module getModule(String name) {
      Module module = this.nameMap.get(name.toLowerCase());
      if (module == null) {
         throw new NoSuchModuleException();
      } else {
         return module;
      }
   }

   @EventTarget
   public void onKey(EventKey e) {
      if (e.isState() && Minecraft.getInstance().screen == null) {
         for (Module module : this.modules) {
            if (module.getKey() == e.getKey()) {
               module.toggle();
            }
         }
      }
   }

   @EventTarget
   public void onKey(EventMouseClick e) {
      if (!e.isState() && (e.getKey() == 3 || e.getKey() == 4)) {
         for (Module module : this.modules) {
            if (module.getKey() == -e.getKey()) {
               module.toggle();
            }
         }
      }
   }

   public List<Module> getModules() {
      return this.modules;
   }
}
