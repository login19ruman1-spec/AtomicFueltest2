package com.atomicfuel.manager;

import com.atomicfuel.Main;
import com.atomicfuel.config.ConfigManager;
import com.atomicfuel.model.ReactorState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;

/** Validates the exact 3x3 water cooling layer and charges coolant water over time. */
public final class CoolingManager {
    private final Main plugin; private final ConfigManager cfg; private final FuelManager fuel;
    public CoolingManager(Main plugin, ConfigManager cfg, FuelManager fuel){this.plugin=plugin;this.cfg=cfg;this.fuel=fuel;}

    public boolean hasCoolingWater(ReactorState r) {
        for(Block b : waterLayer(r)) if(b.getType()!=Material.WATER) return false;
        return true;
    }
    public boolean hasAnyChargedWater(ReactorState r) {
        for(Block b:waterLayer(r)) if(fuel.isBlock(b,FuelManager.Type.CHARGED_WATER)) return true;
        return false;
    }
    public void tick(ReactorState r) {
        long now=System.currentTimeMillis();
        if(now-r.lastWaterCharge() < cfg.i("cooling.charge-water-every-seconds")*1000L) return;
        boolean changed=false;
        for(Block b:waterLayer(r)) {
            if(b.getType()==Material.WATER) {
                b.setType(Material.LIGHT_BLUE_STAINED_GLASS,false); fuel.tagBlock(b,FuelManager.Type.CHARGED_WATER); changed=true;
                b.getWorld().spawnParticle(Particle.BUBBLE_POP,b.getLocation().add(.5,.5,.5),5,.2,.2,.2);
            }
        }
        if(changed) r.lastWaterCharge(now);
    }
    public Block[] waterLayer(ReactorState r) {
        Block center=r.center().getBlock(); Block[] out=new Block[9]; int i=0;
        for(int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) out[i++]=center.getRelative(x,1,z);
        return out;
    }
    public boolean isCorrectFrame(ReactorState r) {
        Block c=r.center().getBlock();
        for(int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) {
            if(x==0&&z==0) continue;
            Material expected=((Math.abs(x)+Math.abs(z))==1)?Material.REDSTONE_BLOCK:Material.IRON_BLOCK;
            if(c.getRelative(x,0,z).getType()!=expected) return false;
        }
        return true;
    }
}
