package com.atomicfuel.manager;

import com.atomicfuel.Main;
import com.atomicfuel.config.ConfigManager;
import com.atomicfuel.model.ReactorState;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class BuffManager {
    private final Main plugin; private final ConfigManager cfg;
    public BuffManager(Main plugin,ConfigManager cfg){this.plugin=plugin;this.cfg=cfg;}
    public void apply(ReactorState r){
        double radius=cfg.i("buff-zone.radius");
        for(Player p:r.center().getWorld().getPlayers()){
            if(p.getLocation().distanceSquared(r.center())>radius*radius)continue;
            boolean stable=r.stability()>=50&&r.temperature()<cfg.i("reactor.temperature-meltdown")&&r.countdownStart()<0;
            if(stable){effect(p,PotionEffectType.HASTE,60,0);effect(p,PotionEffectType.NIGHT_VISION,220,0);effect(p,PotionEffectType.REGENERATION,80,0);}
            else {effect(p,PotionEffectType.WITHER,60,0);effect(p,PotionEffectType.POISON,60,0);effect(p,PotionEffectType.NAUSEA,80,0);effect(p,PotionEffectType.WEAKNESS,60,0);}
        }
    }
    private void effect(Player p,PotionEffectType type,int duration,int amplifier){p.addPotionEffect(new PotionEffect(type,duration,amplifier,true,false,false));}
}
