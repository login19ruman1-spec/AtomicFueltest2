package com.atomicfuel.api;
import org.bukkit.event.*; import com.atomicfuel.model.RadiationZone;
public final class RadiationZoneExpireEvent extends Event { private static final HandlerList H=new HandlerList(); private final RadiationZone zone;
 public RadiationZoneExpireEvent(RadiationZone z){zone=z;} public RadiationZone getZone(){return zone;} public static HandlerList getHandlerList(){return H;} public HandlerList getHandlers(){return H;} }
