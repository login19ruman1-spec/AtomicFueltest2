package com.atomicfuel.manager;

import com.atomicfuel.Main;
import com.atomicfuel.api.RadiationZoneCreateEvent;
import com.atomicfuel.api.RadiationZoneExpireEvent;
import com.atomicfuel.config.ConfigManager;
import com.atomicfuel.model.RadiationZone;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/** Persistent fictional radiation zones, effects and environmental corruption. */
public final class RadiationManager {
    private final Main plugin; private final ConfigManager cfg; private final FuelManager fuel; private final StateStore store;
    private final List<RadiationZone> zones=new ArrayList<>(); private BukkitTask task;
    public RadiationManager(Main plugin,ConfigManager cfg,FuelManager fuel,StateStore store){this.plugin=plugin;this.cfg=cfg;this.fuel=fuel;this.store=store;loadPersisted();}
    public void start(){task=plugin.getServer().getScheduler().runTaskTimer(plugin,this::tick,20L,20L);}
    public void shutdown(){if(task!=null)task.cancel();saveAll();}
    private void loadPersisted(){for(World w:plugin.getServer().getWorlds())zones.addAll(store.loadZones(w));}
    private void saveAll(){for(World w:plugin.getServer().getWorlds())store.saveZones(w,zones);}
    public List<RadiationZone> zones(){return Collections.unmodifiableList(zones);}
    public RadiationZone create(Location l,double radius){return create(l,radius,false);}
    public RadiationZone create(Location l,double radius,boolean expandable){
        long now=System.currentTimeMillis(); RadiationZone z=new RadiationZone(l,radius,now+cfg.i("radiation.duration-seconds")*1000L,now,expandable); if(expandable) z.nextExpansion(now+cfg.i("radiation.expand-every-seconds")*1000L);
        zones.add(z);store.saveZones(l.getWorld(),zones);plugin.getServer().getPluginManager().callEvent(new RadiationZoneCreateEvent(z));
        l.getWorld().playSound(l,Sound.BLOCK_BEACON_DEACTIVATE,1,.6f);return z;
    }
    private void tick(){
        long now=System.currentTimeMillis();boolean changed=false;Iterator<RadiationZone> it=zones.iterator();
        while(it.hasNext()){
            RadiationZone z=it.next();
            if(now>=z.expiresAt()){plugin.getServer().getPluginManager().callEvent(new RadiationZoneExpireEvent(z));it.remove();changed=true;continue;}
            visualize(z);if(now>=z.nextEffect()){effectsAndTerrain(z);z.nextEffect(now+cfg.i("radiation.effect-tick-interval")*50L);changed=true;}
            if(z.expandable() && now>=z.nextExpansion() && z.radius()<cfg.i("radiation.zone-radius-explosion")){z.radius(z.radius()+1);z.nextExpansion(now+cfg.i("radiation.expand-every-seconds")*1000L);changed=true;}
        }
        if(changed)saveAll();
    }
    private void visualize(RadiationZone z){World w=z.center().getWorld();for(int i=0;i<14;i++){double a=Math.random()*Math.PI*2,rr=Math.sqrt(Math.random())*z.radius();Location l=z.center().clone().add(Math.cos(a)*rr,.2+Math.random()*1.8,Math.sin(a)*rr);w.spawnParticle(Particle.SPORE_BLOSSOM_AIR,l,1,0,0,0,0);w.spawnParticle(Particle.WARPED_SPORE,l,1,.1,.1,.1,0);}}
    private void effectsAndTerrain(RadiationZone z){
        World w=z.center().getWorld();double rr=z.radius()*z.radius();
        for(Player p:w.getPlayers())if(p.getLocation().distanceSquared(z.center())<=rr){p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER,80,0));p.addPotionEffect(new PotionEffect(PotionEffectType.POISON,80,0));p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA,100,0));p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,80,0));p.playSound(p.getLocation(),Sound.BLOCK_BEACON_AMBIENT,.25f,.5f);}
        int r=(int)Math.ceil(z.radius());int cx=z.center().getBlockX(),cz=z.center().getBlockZ(),cy=z.center().getBlockY();
        for(int x=-r;x<=r;x++)for(int zz=-r;zz<=r;zz++){
            if(x*x+zz*zz>rr)continue;Block b=w.getBlockAt(cx+x,cy,cz+zz);
            if(b.getType()==Material.WATER){b.setType(Material.LIGHT_BLUE_STAINED_GLASS,false);fuel.tagBlock(b,FuelManager.Type.CHARGED_WATER);continue;}
            if(b.getType()==Material.SHORT_GRASS||b.getType()==Material.TALL_GRASS||b.getType().name().endsWith("LEAVES")){b.setType(Math.random()<0.7?Material.DEAD_BUSH:Material.AIR,false);fuel.untagBlock(b);continue;}
            if(b.getType()==Material.GRASS_BLOCK||b.getType()==Material.DIRT||b.getType()==Material.PODZOL||b.getType()==Material.COARSE_DIRT){b.setType(Math.random()<0.15?Material.MYCELIUM:Material.COARSE_DIRT,false);}
        }
    }
    public boolean reduceAt(Location l,double amount){boolean changed=false;for(RadiationZone z:zones)if(z.center().getWorld().equals(l.getWorld())&&z.center().distance(l)<=z.radius()){z.radius(Math.max(0,z.radius()-amount));changed=true;}zones.removeIf(z->z.radius()<1);if(changed)saveAll();return changed;}
    public int cleanup(Location l,double radius){int n=0;Iterator<RadiationZone>it=zones.iterator();while(it.hasNext()){RadiationZone z=it.next();if(z.center().getWorld().equals(l.getWorld())&&z.center().distance(l)<=radius){it.remove();n++;}}if(n>0)saveAll();return n;}
    public double levelAt(Location l){double max=0;for(RadiationZone z:zones)if(z.center().getWorld().equals(l.getWorld())){double d=z.center().distance(l);if(d<=z.radius())max=Math.max(max,(z.radius()-d)/Math.max(1,z.radius())*100);}return max;}
}
