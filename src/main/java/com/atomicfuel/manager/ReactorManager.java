package com.atomicfuel.manager;

import com.atomicfuel.Main;
import com.atomicfuel.api.*;
import com.atomicfuel.config.ConfigManager;
import com.atomicfuel.model.ReactorState;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/** Main fictional reactor simulation. */
public final class ReactorManager {
    private final Main plugin; private final ConfigManager cfg; private final FuelManager fuel; private final CoolingManager cooling;
    private final RadiationManager radiation; private final ExplosionManager explosion; private final BuffManager buffs; private final StateStore store;
    private final Map<String, ReactorState> reactors=new HashMap<>(); private BukkitTask task; private long tickCounter;

    public ReactorManager(Main plugin, ConfigManager cfg, FuelManager fuel, CoolingManager cooling, RadiationManager radiation,
                          ExplosionManager explosion, BuffManager buffs, StateStore store){
        this.plugin=plugin;this.cfg=cfg;this.fuel=fuel;this.cooling=cooling;this.radiation=radiation;this.explosion=explosion;this.buffs=buffs;this.store=store;
    }
    public void start(){loadPersisted();task=plugin.getServer().getScheduler().runTaskTimer(plugin,this::tick,1L,1L);}
    public void shutdown(){if(task!=null)task.cancel();saveAll();}
    private String key(Location l){return l.getWorld().getUID()+":"+l.getBlockX()+":"+l.getBlockY()+":"+l.getBlockZ();}
    public Collection<ReactorState> all(){return Collections.unmodifiableCollection(reactors.values());}
    public ReactorState nearest(Location loc,double max){ReactorState best=null;double bd=max*max;for(ReactorState r:reactors.values()){if(r.center().getWorld()==null||!r.center().getWorld().equals(loc.getWorld()))continue;double d=r.center().distanceSquared(loc);if(d<=bd){bd=d;best=r;}}return best;}
    public ReactorState get(Block b){return reactors.get(key(b.getLocation()));}

    private void loadPersisted(){
        for(World w:plugin.getServer().getWorlds()) for(String index:store.index(w,"reactors")) {
            Location l=store.decode(w,index); if(l==null) continue; ReactorState r=store.loadReactor(l);
            if(r!=null && r.active()) reactors.put(key(l),r);
        }
    }
    private void saveAll(){for(ReactorState r:reactors.values())store.saveReactor(r);}

    public boolean activate(Block b){
        if(!fuel.isBlock(b,FuelManager.Type.FUEL_ROD_BLOCK)) return false;
        ReactorState candidate=new ReactorState(b.getLocation());
        if(!cooling.isCorrectFrame(candidate) || !cooling.hasCoolingWater(candidate)) return false;
        b.setType(Material.GOLD_BLOCK,false);fuel.untagBlock(b);fuel.tagBlock(b,FuelManager.Type.ACTIVE_FUEL_ROD_BLOCK);
        candidate.active(true);candidate.lastByproduct(System.currentTimeMillis());candidate.lastWaterCharge(System.currentTimeMillis());
        reactors.put(key(b.getLocation()),candidate);store.saveReactor(candidate);
        b.getWorld().playSound(b.getLocation(),Sound.BLOCK_BEACON_ACTIVATE,1,1.2f);return true;
    }
    public boolean stabilize(ReactorState r){if(r==null||!r.active())return false;r.stability(100);r.temperature(Math.max(0,r.temperature()-cfg.i("reactor.coolant-reduction")));r.countdownStart(-1);plugin.getServer().getPluginManager().callEvent(new AtomicStabilizeEvent(r));store.saveReactor(r);return true;}

    private void tick(){
        tickCounter++;
        for(ReactorState r:new ArrayList<>(reactors.values())) tickReactor(r);
        if(tickCounter%20==0) saveAll();
    }
    private void tickReactor(ReactorState r){
        if(!r.active())return;
        Block center=r.center().getBlock();
        if(!fuel.isBlock(center,FuelManager.Type.ACTIVE_FUEL_ROD_BLOCK)){remove(r);return;}
        plugin.getServer().getPluginManager().callEvent(new AtomicFuelTickEvent(r));

        boolean frameOk=cooling.isCorrectFrame(r); boolean water=cooling.hasCoolingWater(r); boolean charged=cooling.hasAnyChargedWater(r);
        double heat=water ? 1.0-cfg.d("cooling.water-cool-per-tick") : 3.0;
        if(charged) heat += Math.abs(cfg.d("cooling.charged-water-heat-penalty"));
        heat -= r.coolantCells()*0.5;
        if(!frameOk) heat += 1.5;
        r.temperature(Math.min(cfg.i("reactor.temperature-max"),r.temperature()+Math.max(-20,heat)));

        if(!frameOk || !water || charged) r.stability(r.stability()-cfg.d("reactor.stability-decay-per-tick")*(charged?2:1));
        else r.stability(r.stability()-cfg.d("reactor.stability-decay-per-tick")*0.15);

        if(r.temperature()>=cfg.i("reactor.temperature-meltdown") && r.countdownStart()<0){
            AtomicFuelInstabilityEvent ev=new AtomicFuelInstabilityEvent(r);plugin.getServer().getPluginManager().callEvent(ev);
            if(!ev.isCancelled()){r.countdownStart(System.currentTimeMillis());plugin.getServer().getPluginManager().callEvent(new AtomicMeltdownStartEvent(r));}
        }
        if(r.countdownStart()>0){long elapsed=(System.currentTimeMillis()-r.countdownStart())/1000L;int left=Math.max(0,cfg.i("reactor.countdown-seconds")-(int)elapsed);siren(r,left);if(left<=0){if(explosion.meltdown(r)){remove(r);return;}r.countdownStart(-1);}}

        double energy=cfg.d("energy.per-tick-base")+cfg.d("energy.per-tick-per-enrichment")*r.enrichment();
        energy*=1.0+r.reflectors()*0.25; energy*=Math.max(0.0,r.stability()/100.0);
        if(!water) energy*=0.75;
        distributeEnergy(r,Math.max(0,Math.round(energy)));

        if(System.currentTimeMillis()-r.lastByproduct()>=cfg.i("byproduct.every-seconds")*1000L){giveByproduct(r);r.lastByproduct(System.currentTimeMillis());}
        buffs.apply(r); cooling.tick(r);
        if(r.temperature()>=cfg.i("reactor.temperature-max")) r.temperature(cfg.i("reactor.temperature-max"));
    }

    private void distributeEnergy(ReactorState r,long amount){
        if(amount<=0)return; Block c=r.center().getBlock();
        Set<Block> buffers=new LinkedHashSet<>();ArrayDeque<Block> q=new ArrayDeque<>();Set<String> seen=new HashSet<>();
        for(Block n:neighbors(c)){if(n.getType()==Material.REDSTONE_BLOCK)buffers.add(n);if(n.getType()==Material.COPPER_BLOCK)q.add(n);}
        while(!q.isEmpty()&&seen.size()<128){Block b=q.poll();String k=b.getX()+":"+b.getY()+":"+b.getZ();if(!seen.add(k))continue;for(Block n:neighbors(b)){if(n.getType()==Material.REDSTONE_BLOCK)buffers.add(n);else if(n.getType()==Material.COPPER_BLOCK)q.add(n);}}
        if(buffers.isEmpty())return;long each=Math.max(1,amount/buffers.size());for(Block b:buffers)storeEnergy(b,each);
    }
    private void storeEnergy(Block b,long amount){long now=storedEnergy(b),cap=cfg.i("energy.redstone-buffer-capacity");long add=Math.min(amount,Math.max(0,cap-now));b.getChunk().getPersistentDataContainer().set(plugin.key("energy_"+b.getX()+"_"+b.getY()+"_"+b.getZ()),org.bukkit.persistence.PersistentDataType.LONG,now+add);if(add>0)b.getWorld().spawnParticle(Particle.END_ROD,b.getLocation().add(.5,.5,.5),1,.15,.15,.15,0);}
    public void storeEnergyPublic(Block b,long amount){ if(b!=null && b.getType()==Material.REDSTONE_BLOCK) storeEnergy(b,amount); }
    public long storedEnergy(Block b){return b.getChunk().getPersistentDataContainer().getOrDefault(plugin.key("energy_"+b.getX()+"_"+b.getY()+"_"+b.getZ()),org.bukkit.persistence.PersistentDataType.LONG,0L);}
    public long extractEnergy(Block b,long amount){long have=storedEnergy(b),take=Math.min(have,Math.max(0,amount));b.getChunk().getPersistentDataContainer().set(plugin.key("energy_"+b.getX()+"_"+b.getY()+"_"+b.getZ()),org.bukkit.persistence.PersistentDataType.LONG,have-take);return take;}
    public long extractEnergyNear(Location l,long amount){for(Block b:neighbors(l.getBlock()))if(b.getType()==Material.REDSTONE_BLOCK&&storedEnergy(b)>0)return extractEnergy(b,amount);return 0;}

    private void giveByproduct(ReactorState r){
        Chest chest=null;for(Block b:neighbors(r.center().getBlock()))if(b.getState() instanceof Chest c){chest=c;break;}if(chest==null)return;
        double heat=Math.min(1.0,r.temperature()/cfg.i("reactor.temperature-max"));
        if(Math.random()<0.55+heat*0.3)chest.getBlockInventory().addItem(fuel.create(FuelManager.Type.TRITIUM_DUST,1));
        if(Math.random()<0.35+heat*0.4)chest.getBlockInventory().addItem(fuel.create(FuelManager.Type.ENRICHED_IRON,1));
        if(Math.random()<0.10+heat*0.25)chest.getBlockInventory().addItem(fuel.create(FuelManager.Type.RARE_ISOTOPE,1));
    }
    private void siren(ReactorState r,int seconds){World w=r.center().getWorld();w.playSound(r.center(),Sound.BLOCK_NOTE_BLOCK_BASS,0.7f,0.5f+Math.min(1.5f,seconds*.08f));w.spawnParticle(Particle.SMOKE,r.center().add(.5,1,.5),12,.4,.7,.4,.02);if(seconds<=3)for(Player p:WorldPlayers.near(r.center(),16))p.sendTitle("§cREACTOR UNSTABLE","§e"+seconds+" seconds",0,20,0);}
    private List<Block> neighbors(Block b){return List.of(b.getRelative(1,0,0),b.getRelative(-1,0,0),b.getRelative(0,1,0),b.getRelative(0,-1,0),b.getRelative(0,0,1),b.getRelative(0,0,-1));}

    public void remove(ReactorState r){reactors.remove(key(r.center()));fuel.untagBlock(r.center().getBlock());store.removeReactor(r);}
    public void onBlockBreak(Block b){
        ReactorState r=reactors.get(key(b.getLocation()));if(r!=null){remove(r);return;}
        for(ReactorState reactor:new ArrayList<>(reactors.values())){
            if(reactor.center().getWorld().equals(b.getWorld()) && Math.abs(reactor.center().getBlockX()-b.getX())<=1 && Math.abs(reactor.center().getBlockZ()-b.getZ())<=1 && reactor.center().getBlockY()==b.getY()){
                reactor.temperature(reactor.temperature()+cfg.i("reactor.heat-spike-on-remove"));reactor.stability(reactor.stability()-25);store.saveReactor(reactor);return;
            }
        }
    }
    public boolean install(Block b,FuelManager.Type t,ItemStack item){ReactorState r=get(b);if(r==null)return false;switch(t){case CONTROL_ROD->{r.controlRod(r.controlRod()+1);r.stability(r.stability()+cfg.i("reactor.control-rod-bonus"));}case COOLANT_CELL->r.coolantCells(r.coolantCells()+1);case NEUTRON_REFLECTOR->r.reflectors(r.reflectors()+1);case ATOMIC_FUEL_ROD->{int next=Math.min(cfg.i("reactor.max-enrichment"),r.enrichment()+fuel.enrichment(item));if(next==r.enrichment())return false;r.enrichment(next);}default-> {return false;}}store.saveReactor(r);return true;}
    static final class WorldPlayers{static List<Player> near(Location l,double d){List<Player>x=new ArrayList<>();for(Player p:l.getWorld().getPlayers())if(p.getLocation().distanceSquared(l)<=d*d)x.add(p);return x;}}
}
