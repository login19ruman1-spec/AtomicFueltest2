package com.atomicfuel.api;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import com.atomicfuel.model.ReactorState;
public final class AtomicFuelTickEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList(); private final ReactorState reactor;
    public AtomicFuelTickEvent(ReactorState reactor) { this.reactor = reactor; }
    public ReactorState getReactor() { return reactor; }
    public static HandlerList getHandlerList() { return HANDLERS; } @Override public HandlerList getHandlers(){return HANDLERS;}
}
