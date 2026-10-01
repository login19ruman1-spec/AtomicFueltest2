package com.atomicfuel.model;

import org.bukkit.Location;

/** Runtime + persisted state for one fictional reactor. */
public final class ReactorState {
    private final Location center;
    private double temperature;
    private double stability = 100.0;
    private int enrichment = 1;
    private long lastByproduct;
    private long countdownStart = -1L;
    private boolean active;
    private int controlRod;
    private int coolantCells;
    private int reflectors;
    private long lastWaterCharge;

    public ReactorState(Location center) { this.center = center.getBlock().getLocation(); }
    public Location center() { return center.clone(); }
    public double temperature() { return temperature; }
    public void temperature(double v) { temperature = Math.max(0, v); }
    public double stability() { return stability; }
    public void stability(double v) { stability = Math.max(0, Math.min(100, v)); }
    public int enrichment() { return enrichment; }
    public void enrichment(int v) { enrichment = Math.max(1, v); }
    public long lastByproduct() { return lastByproduct; }
    public void lastByproduct(long v) { lastByproduct = v; }
    public long countdownStart() { return countdownStart; }
    public void countdownStart(long v) { countdownStart = v; }
    public boolean active() { return active; }
    public void active(boolean v) { active = v; }
    public int controlRod() { return controlRod; }
    public void controlRod(int v) { controlRod = Math.max(0, v); }
    public int coolantCells() { return coolantCells; }
    public void coolantCells(int v) { coolantCells = Math.max(0, v); }
    public int reflectors() { return reflectors; }
    public void reflectors(int v) { reflectors = Math.max(0, v); }
    public long lastWaterCharge() { return lastWaterCharge; }
    public void lastWaterCharge(long v) { lastWaterCharge = v; }
    public boolean meltdown(double threshold) { return temperature >= threshold; }
}
