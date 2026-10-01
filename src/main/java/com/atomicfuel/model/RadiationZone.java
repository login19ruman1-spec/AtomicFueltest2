package com.atomicfuel.model;

import org.bukkit.Location;

public final class RadiationZone {
    private final Location center;
    private double radius;
    private final long expiresAt;
    private long nextEffect;
    private long nextExpansion;
    private final boolean expandable;
    public RadiationZone(Location center,double radius,long expiresAt,long now){this(center,radius,expiresAt,now,false);}
    public RadiationZone(Location center,double radius,long expiresAt,long now,boolean expandable){this.center=center.getBlock().getLocation();this.radius=Math.max(1,radius);this.expiresAt=expiresAt;this.nextEffect=now;this.nextExpansion=expandable?now:Long.MAX_VALUE;this.expandable=expandable;}
    public Location center(){return center.clone();} public double radius(){return radius;} public void radius(double r){radius=Math.max(0,r);} public long expiresAt(){return expiresAt;} public long nextEffect(){return nextEffect;} public void nextEffect(long v){nextEffect=v;} public long nextExpansion(){return nextExpansion;} public void nextExpansion(long v){nextExpansion=v;} public boolean expandable(){return expandable;}
}
