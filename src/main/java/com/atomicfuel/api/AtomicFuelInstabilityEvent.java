package com.atomicfuel.api;
import org.bukkit.event.*; import com.atomicfuel.model.ReactorState;
public final class AtomicFuelInstabilityEvent extends Event implements Cancellable {
 private static final HandlerList H=new HandlerList(); private final ReactorState reactor; private boolean cancelled;
 public AtomicFuelInstabilityEvent(ReactorState r){reactor=r;} public ReactorState getReactor(){return reactor;} public boolean isCancelled(){return cancelled;} public void setCancelled(boolean c){cancelled=c;}
 public static HandlerList getHandlerList(){return H;} public HandlerList getHandlers(){return H;}
}
