package com.atomicfuel.api;
import org.bukkit.event.*; import com.atomicfuel.model.RadiationZone;
public final class RadiationZoneCreateEvent extends Event { private static final HandlerList H=new HandlerList(); private final RadiationZone zone;
 public RadiationZoneCreateEvent(RadiationZone z){zone=z;} public RadiationZone getZone(){return zone;} public static HandlerList getHandlerList(){return H;} public HandlerList getHandlers(){return H;} }
