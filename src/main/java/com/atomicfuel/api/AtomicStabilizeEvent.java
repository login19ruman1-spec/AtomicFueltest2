package com.atomicfuel.api;
import org.bukkit.event.*; import com.atomicfuel.model.ReactorState;
public final class AtomicStabilizeEvent extends Event { private static final HandlerList H=new HandlerList(); private final ReactorState reactor;
 public AtomicStabilizeEvent(ReactorState r){reactor=r;} public ReactorState getReactor(){return reactor;} public static HandlerList getHandlerList(){return H;} public HandlerList getHandlers(){return H;} }
