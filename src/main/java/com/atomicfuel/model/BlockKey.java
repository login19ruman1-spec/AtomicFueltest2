package com.atomicfuel.model;

import org.bukkit.Location;
import org.bukkit.World;

public record BlockKey(String world, int x, int y, int z) {
    public static BlockKey of(Location l) { return new BlockKey(l.getWorld().getUID().toString(), l.getBlockX(), l.getBlockY(), l.getBlockZ()); }
    public Location location(World world) { return new Location(world, x, y, z); }
}
