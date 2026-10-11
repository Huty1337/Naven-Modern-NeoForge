package tech.naven.modules.impl.misc;

import tech.naven.Naven;
import tech.naven.modules.Category;
import tech.naven.modules.Module;
import tech.naven.modules.ModuleInfo;
import tech.naven.ui.notification.Notification;
import tech.naven.ui.notification.NotificationLevel;
import tech.naven.utils.TimeHelper;

@ModuleInfo(
   name = "ClientFriend",
   description = "Treat other users as friend!",
   category = Category.MISC
)
public class ClientFriend extends Module {
   public static TimeHelper attackTimer = new TimeHelper();

   @Override
   public void onDisable() {
      attackTimer.reset();
      Notification notification = new Notification(NotificationLevel.INFO, "You can attack other players after 15 seconds.", 15000L);
      Naven.getInstance().getNotificationManager().addNotification(notification);
   }
}
