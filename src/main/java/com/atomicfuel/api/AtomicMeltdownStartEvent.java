package com.atomicfuel.api;
import org.bukkit.event.*; import com.atomicfuel.model.ReactorState;
public final class AtomicMeltdownStartEvent extends Event { private static final HandlerList H=new HandlerList(); private final ReactorState reactor;
 public AtomicMeltdownStartEvent(ReactorState r){reactor=r;} public ReactorState getReactor(){return reactor;} public static HandlerList getHandlerList(){return H;} public HandlerList getHandlers(){return H;} }
