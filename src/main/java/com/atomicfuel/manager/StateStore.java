package com.atomicfuel.manager;

import com.atomicfuel.Main;
import com.atomicfuel.model.ReactorState;
import com.atomicfuel.model.RadiationZone;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/** Persists reactor and radiation metadata in Bukkit/Paper PDCs. */
public final class StateStore {
    private final Main plugin;
    public StateStore(Main plugin) { this.plugin = plugin; }

    private String token(int v) { return v < 0 ? "m" + (-v) : "p" + v; }
    private String reactorKey(ReactorState r) { Location l = r.center(); return "r_" + token(l.getBlockX()) + "_" + token(l.getBlockY()) + "_" + token(l.getBlockZ()); }
    private String blockKey(Location l) { return "b_" + token(l.getBlockX()) + "_" + token(l.getBlockY()) + "_" + token(l.getBlockZ()); }

    public void saveReactor(ReactorState r) {
        PersistentDataContainer pdc = r.center().getWorld().getChunkAt(r.center()).getPersistentDataContainer();
        String value = String.join(",", String.valueOf(r.temperature()), String.valueOf(r.stability()), String.valueOf(r.enrichment()),
                String.valueOf(r.lastByproduct()), String.valueOf(r.countdownStart()), String.valueOf(r.active()),
                String.valueOf(r.controlRod()), String.valueOf(r.coolantCells()), String.valueOf(r.reflectors()), String.valueOf(r.lastWaterCharge()));
        pdc.set(plugin.key(reactorKey(r)), PersistentDataType.STRING, value);
        addWorldIndex(r.center().getWorld(), "reactors", reactorKey(r));
    }

    public ReactorState loadReactor(Location l) {
        PersistentDataContainer pdc = l.getWorld().getChunkAt(l).getPersistentDataContainer();
        String raw = pdc.get(plugin.key(reactorKey(new ReactorState(l))), PersistentDataType.STRING);
        if (raw == null) return null;
        try {
            String[] a = raw.split(",", -1);
            ReactorState r = new ReactorState(l);
            r.temperature(Double.parseDouble(a[0])); r.stability(Double.parseDouble(a[1])); r.enrichment(Integer.parseInt(a[2]));
            r.lastByproduct(Long.parseLong(a[3])); r.countdownStart(Long.parseLong(a[4])); r.active(Boolean.parseBoolean(a[5]));
            r.controlRod(Integer.parseInt(a[6])); r.coolantCells(Integer.parseInt(a[7])); r.reflectors(Integer.parseInt(a[8])); r.lastWaterCharge(Long.parseLong(a[9]));
            return r;
        } catch (RuntimeException ex) { return null; }
    }

    public void removeReactor(ReactorState r) {
        r.center().getWorld().getChunkAt(r.center()).getPersistentDataContainer().remove(plugin.key(reactorKey(r)));
        removeWorldIndex(r.center().getWorld(), "reactors", reactorKey(r));
    }

    public void tagBlock(Location l, String type) {
        l.getWorld().getChunkAt(l).getPersistentDataContainer().set(plugin.key(blockKey(l)), PersistentDataType.STRING, type);
    }
    public String blockType(Location l) {
        return l.getWorld().getChunkAt(l).getPersistentDataContainer().get(plugin.key(blockKey(l)), PersistentDataType.STRING);
    }
    public void untagBlock(Location l) { l.getWorld().getChunkAt(l).getPersistentDataContainer().remove(plugin.key(blockKey(l))); }

    private void addWorldIndex(World world, String name, String value) {
        PersistentDataContainer pdc = world.getPersistentDataContainer();
        Set<String> values = new LinkedHashSet<>(split(pdc.get(plugin.key(name), PersistentDataType.STRING)));
        values.add(value); pdc.set(plugin.key(name), PersistentDataType.STRING, String.join(";", values));
    }
    private void removeWorldIndex(World world, String name, String value) {
        PersistentDataContainer pdc = world.getPersistentDataContainer();
        Set<String> values = new LinkedHashSet<>(split(pdc.get(plugin.key(name), PersistentDataType.STRING)));
        values.remove(value); if(values.isEmpty()) pdc.remove(plugin.key(name)); else pdc.set(plugin.key(name), PersistentDataType.STRING, String.join(";", values));
    }
    private List<String> split(String raw) { return raw == null || raw.isBlank() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(raw.split(";"))); }
    public Set<String> index(World world, String name) { return new LinkedHashSet<>(split(world.getPersistentDataContainer().get(plugin.key(name), PersistentDataType.STRING))); }

    public Location decode(World world, String key) {
        String[] p = key.substring(key.indexOf('_') + 1).split("_");
        if(p.length != 3) return null;
        return new Location(world, decodeInt(p[0]), decodeInt(p[1]), decodeInt(p[2]));
    }
    private int decodeInt(String s) { return Integer.parseInt(s.substring(1)) * (s.charAt(0) == 'm' ? -1 : 1); }

    public void saveZones(World world, Collection<RadiationZone> zones) {
        List<String> rows = new ArrayList<>();
        for(RadiationZone z : zones) if(z.center().getWorld().equals(world)) {
            Location l=z.center(); rows.add(l.getBlockX()+","+l.getBlockY()+","+l.getBlockZ()+","+z.radius()+","+z.expiresAt()+","+z.expandable());
        }
        PersistentDataContainer pdc=world.getPersistentDataContainer();
        if(rows.isEmpty()) pdc.remove(plugin.key("radiation_zones")); else pdc.set(plugin.key("radiation_zones"),PersistentDataType.STRING,String.join(";",rows));
    }
    public List<RadiationZone> loadZones(World world) {
        String raw=world.getPersistentDataContainer().get(plugin.key("radiation_zones"),PersistentDataType.STRING);
        List<RadiationZone> out=new ArrayList<>(); if(raw==null||raw.isBlank()) return out;
        long now=System.currentTimeMillis();
        for(String row:raw.split(";")) try {String[] a=row.split(","); long expires=Long.parseLong(a[4]); if(expires>now){ boolean expandable=a.length>5&&Boolean.parseBoolean(a[5]); RadiationZone z=new RadiationZone(new Location(world,Integer.parseInt(a[0]),Integer.parseInt(a[1]),Integer.parseInt(a[2])),Double.parseDouble(a[3]),expires,now,expandable); if(expandable) z.nextExpansion(now+30000L); out.add(z); }} catch(Exception ignored) {}
        return out;
    }
}
