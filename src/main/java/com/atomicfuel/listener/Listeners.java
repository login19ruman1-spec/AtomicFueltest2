package com.atomicfuel.listener;

import com.atomicfuel.Main;
import com.atomicfuel.manager.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

public final class Listeners implements Listener {
    private final Main plugin; private final FuelManager fuel; private final ReactorManager reactors; private final CoolingManager cooling; private final RadiationManager radiation;
    public Listeners(Main plugin,FuelManager fuel,ReactorManager reactors,CoolingManager cooling,RadiationManager radiation){this.plugin=plugin;this.fuel=fuel;this.reactors=reactors;this.cooling=cooling;this.radiation=radiation;}

    @EventHandler public void craft(PrepareItemCraftEvent e){
        if(!(e.getView().getPlayer() instanceof Player p))return;
        FuelManager.Type t=fuel.type(e.getInventory().getResult());
        if(t!=null&&!p.hasPermission("atomicfuel.craft"))e.getInventory().setResult(null);
    }

    @EventHandler public void place(BlockPlaceEvent e){
        FuelManager.Type t=fuel.type(e.getItemInHand());if(t==null)return;
        if(t==FuelManager.Type.FUEL_ROD_BLOCK||t==FuelManager.Type.ACTIVE_FUEL_ROD_BLOCK||t==FuelManager.Type.RADIOACTIVE_CORE){
            fuel.tagBlock(e.getBlock(),t);
            if(t==FuelManager.Type.ACTIVE_FUEL_ROD_BLOCK && !reactors.activate(e.getBlock())) { fuel.untagBlock(e.getBlock()); e.setCancelled(true); }
        }
    }

    @EventHandler public void breakBlock(BlockBreakEvent e){
        Block b=e.getBlock();
        if(fuel.isBlock(b,FuelManager.Type.ACTIVE_FUEL_ROD_BLOCK)){e.setDropItems(false);reactors.onBlockBreak(b);b.setType(Material.AIR,false);e.getPlayer().getInventory().addItem(fuel.create(FuelManager.Type.FUEL_ROD_BLOCK,1));e.getPlayer().sendMessage(Main.PREFIX+"§cReactor removed.");return;}
        if(fuel.isBlock(b,FuelManager.Type.FUEL_ROD_BLOCK)){e.setDropItems(false);fuel.untagBlock(b);e.getPlayer().getInventory().addItem(fuel.create(FuelManager.Type.FUEL_ROD_BLOCK,1));return;}
        if(fuel.isBlock(b,FuelManager.Type.RADIOACTIVE_CORE)){e.setDropItems(false);fuel.untagBlock(b);b.setType(Material.AIR,false);return;}
        if(fuel.isBlock(b,FuelManager.Type.CHARGED_WATER)){e.setDropItems(false);fuel.untagBlock(b);b.setType(Material.AIR,false);return;}
        reactors.onBlockBreak(b);
    }

    @EventHandler public void interact(PlayerInteractEvent e){
        if(e.getHand()!=EquipmentSlot.HAND||e.getClickedBlock()==null)return;
        Player p=e.getPlayer();Block b=e.getClickedBlock();ItemStack in=e.getItem();FuelManager.Type t=fuel.type(in);
        if(t==FuelManager.Type.FUEL_ROD_BLOCK && e.getAction().isRightClick()) { e.setCancelled(true); return; }

        if(fuel.isBlock(b,FuelManager.Type.FUEL_ROD_BLOCK)&&t==FuelManager.Type.CONTROL_ROD){
            if(!p.hasPermission("atomicfuel.use"))return;e.setCancelled(true);
            if(reactors.activate(b)){consume(in);p.sendMessage(Main.PREFIX+"§aReactor activated.");}
            else p.sendMessage(Main.PREFIX+"§cActivation failed: build the exact 3x3 frame and a full 3x3 water layer above it.");return;
        }
        if(fuel.isBlock(b,FuelManager.Type.ACTIVE_FUEL_ROD_BLOCK)&&t!=null && (t==FuelManager.Type.CONTROL_ROD||t==FuelManager.Type.COOLANT_CELL||t==FuelManager.Type.NEUTRON_REFLECTOR||t==FuelManager.Type.ATOMIC_FUEL_ROD)){
            if(!p.hasPermission("atomicfuel.use"))return;e.setCancelled(true);
            if(reactors.install(b,t,in)){consume(in);p.sendMessage(Main.PREFIX+"§aInstalled §f"+t.name()+"§a.");}else p.sendMessage(Main.PREFIX+"§eNo change: the reactor may already be at the configured limit.");return;
        }
        if(fuel.isBlock(b,FuelManager.Type.ACTIVE_FUEL_ROD_BLOCK)&&fuel.isBattery(in)){
            e.setCancelled(true);long pulled=reactors.extractEnergyNear(b.getLocation(),1000);long used=fuel.chargeBattery(in,pulled);if(used<pulled) { // return unused energy to the nearest buffer
                Block n=findBuffer(b); if(n!=null) reactors.storeEnergyPublic(n,pulled-used);
            }
            p.sendMessage(Main.PREFIX+"§bBattery +"+used+" AtomicEnergy.");return;
        }
        if(fuel.isBattery(in)&&b.getType()==Material.REDSTONE_BLOCK&&e.getAction().isRightClick()){
            e.setCancelled(true);long pulled=reactors.extractEnergy(b,1000);long used=fuel.chargeBattery(in,pulled);if(used<pulled)reactors.storeEnergyPublic(b,pulled-used);p.sendActionBar("§bAtomicEnergy: §f"+fuel.batteryEnergy(in)+" / "+plugin.config().i("energy.battery-capacity"));return;
        }
        if(fuel.isBlock(b,FuelManager.Type.RADIOACTIVE_CORE)){e.setCancelled(true);p.sendMessage(Main.PREFIX+"§cRadioactiveCore: radiation §f"+(int)radiation.levelAt(b.getLocation())+"§c.");return;}
        if(fuel.isBlock(b,FuelManager.Type.CHARGED_WATER)&&e.getAction().isRightClick()&&in!=null&&in.getType()==Material.BUCKET){
            e.setCancelled(true);b.setType(Material.WATER,false);fuel.untagBlock(b);replaceHand(p,fuel.create(FuelManager.Type.RADIOACTIVE_WATER_BUCKET,1));p.addPotionEffect(new org.bukkit.potion.PotionEffect(PotionEffectType.WITHER,600,0));p.sendMessage(Main.PREFIX+"§cYou collected radioactive water.");return;
        }
        if(t==FuelManager.Type.RADIOACTIVE_WATER_BUCKET&&e.getAction().isRightClick()){
            e.setCancelled(true);Location l=b.getRelative(e.getBlockFace()).getLocation();radiation.create(l,plugin.config().i("radiation.zone-radius-water"),false);consume(in);if(in.getAmount()<=0)replaceHand(p,new ItemStack(Material.BUCKET));return;
        }
        if(t==FuelManager.Type.RADAWAY&&e.getAction().isRightClick()){
            e.setCancelled(true);boolean changed=radiation.reduceAt(p.getLocation(),plugin.config().d("radiation.radaway-reduction"));clearRadiationEffects(p);consume(in);p.sendMessage(Main.PREFIX+(changed?"§aRadiation reduced.":"§7No nearby radiation zone."));return;
        }
        if(t==FuelManager.Type.GEIGER_COUNTER&&e.getAction().isRightClick()){e.setCancelled(true);p.sendActionBar("§eGeigerCounter §7radiation: §c"+(int)radiation.levelAt(p.getLocation()));}
    }

    @EventHandler public void waterBucket(PlayerBucketEmptyEvent e){
        if(e.getBucket()!=Material.WATER_BUCKET)return;double level=radiation.levelAt(e.getBlock().getLocation());if(level>0)plugin.getServer().getScheduler().runTask(plugin,()->radiation.reduceAt(e.getBlock().getLocation(),plugin.config().d("radiation.water-cleanup-reduction")));
    }

    @EventHandler public void fluid(BlockFromToEvent e){
        if(e.getBlock().getType()!=Material.WATER)return;
        for(var z:radiation.zones())if(z.center().getWorld().equals(e.getToBlock().getWorld())&&z.center().distance(e.getToBlock().getLocation())<=z.radius()){
            e.setCancelled(true);Block target=e.getToBlock();target.setType(Material.LIGHT_BLUE_STAINED_GLASS,false);fuel.tagBlock(target,FuelManager.Type.CHARGED_WATER);target.getWorld().spawnParticle(Particle.BUBBLE_POP,target.getLocation().add(.5,.5,.5),3,.2,.2,.2);break;
        }
    }

    private void consume(ItemStack item){if(item!=null)item.setAmount(item.getAmount()-1);}
    private void replaceHand(Player p,ItemStack item){p.getInventory().setItemInMainHand(item);}
    private void clearRadiationEffects(Player p){p.removePotionEffect(PotionEffectType.WITHER);p.removePotionEffect(PotionEffectType.POISON);p.removePotionEffect(PotionEffectType.NAUSEA);p.removePotionEffect(PotionEffectType.WEAKNESS);}
    private Block findBuffer(Block b){for(Block n:new Block[]{b.getRelative(1,0,0),b.getRelative(-1,0,0),b.getRelative(0,1,0),b.getRelative(0,-1,0),b.getRelative(0,0,1),b.getRelative(0,0,-1)})if(n.getType()==Material.REDSTONE_BLOCK)return n;return null;}
}
