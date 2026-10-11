package tech.naven.events.api.events;

public interface Cancellable {
   boolean isCancelled();

   void setCancelled(boolean var1);
}
