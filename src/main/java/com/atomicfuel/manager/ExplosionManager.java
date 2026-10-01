package com.atomicfuel.manager;

import com.atomicfuel.Main;
import com.atomicfuel.api.AtomicExplosionEvent;
import com.atomicfuel.config.ConfigManager;
import com.atomicfuel.model.ReactorState;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

/** Handles the fictional Minecraft meltdown explosion and aftermath. */
public final class ExplosionManager {
    private final Main plugin; private final ConfigManager cfg; private final RadiationManager radiation; private final FuelManager fuel;
    public ExplosionManager(Main plugin,ConfigManager cfg,RadiationManager radiation,FuelManager fuel){this.plugin=plugin;this.cfg=cfg;this.radiation=radiation;this.fuel=fuel;}
    public boolean meltdown(ReactorState r){
        AtomicExplosionEvent ev=new AtomicExplosionEvent(r);plugin.getServer().getPluginManager().callEvent(ev);if(ev.isCancelled()){r.countdownStart(-1);r.temperature(700);return false;}
        Location l=r.center().clone().add(.5,.5,.5);World w=l.getWorld();
        w.playSound(l,Sound.ENTITY_GENERIC_EXPLODE,4,.5f);w.spawnParticle(Particle.EXPLOSION_EMITTER,l,1);
        double push=cfg.d("explosion.knockback");double rr=cfg.i("radiation.zone-radius-explosion")+r.reflectors()*2.0;
        for(Entity e:w.getNearbyEntities(l,rr,rr,rr)) if(e instanceof org.bukkit.entity.LivingEntity le){Vector v=e.getLocation().toVector().subtract(l.toVector());if(v.lengthSquared()<.01)v=new Vector(0,1,0);le.setVelocity(v.normalize().multiply(push));}
        float power=(float)(cfg.d("explosion.power")+r.reflectors()*0.5);w.createExplosion(l.getX(),l.getY(),l.getZ(),power,cfg.b("explosion.fire"),cfg.b("explosion.destroy-blocks"));
        radiation.create(l,rr);
        Block core=l.getBlock();core.setType(Material.RESPAWN_ANCHOR,false);fuel.tagBlock(core,FuelManager.Type.RADIOACTIVE_CORE);
        int crater=cfg.i("explosion.crater-radius");for(int x=-crater;x<=crater;x++)for(int z=-crater;z<=crater;z++){if(x*x+z*z>crater*crater)continue;Block b=w.getBlockAt(core.getX()+x,core.getY(),core.getZ()+z);if(b.equals(core))continue;if(Math.random()<.55)b.setType(Material.MYCELIUM,false);else if(Math.random()<.35)b.setType(Material.DEAD_BUSH,false);}
        r.active(false);r.countdownStart(-1);return true;
    }
}
