package com.atomicfuel;

import com.atomicfuel.command.CommandHandler;
import com.atomicfuel.config.ConfigManager;
import com.atomicfuel.listener.Listeners;
import com.atomicfuel.manager.*;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
    public static final String PREFIX="§8[§bAtomicFuel§8] §f";
    private ConfigManager config; private StateStore store; private FuelManager fuel; private RadiationManager radiation; private CoolingManager cooling; private ExplosionManager explosions; private BuffManager buffs; private ReactorManager reactors;
    @Override public void onEnable(){
        saveDefaultConfig(); config=new ConfigManager(this); store=new StateStore(this); fuel=new FuelManager(this,store);
        radiation=new RadiationManager(this,config,fuel,store); cooling=new CoolingManager(this,config,fuel); explosions=new ExplosionManager(this,config,radiation,fuel); buffs=new BuffManager(this,config);
        reactors=new ReactorManager(this,config,fuel,cooling,radiation,explosions,buffs,store); new RecipeManager(this,fuel).registerAll();
        radiation.start(); getServer().getPluginManager().registerEvents(new Listeners(this,fuel,reactors,cooling,radiation),this);
        CommandHandler handler=new CommandHandler(this,fuel,reactors,radiation);getCommand("atomicfuel").setExecutor(handler);getCommand("atomicfuel").setTabCompleter(handler);reactors.start();
        getLogger().info("AtomicFuel 1.21.4 enabled.");
    }
    @Override public void onDisable(){if(reactors!=null)reactors.shutdown();if(radiation!=null)radiation.shutdown();}
    public void reloadPlugin(){reloadConfig();config.reload();}
    public ConfigManager config(){return config;} public StateStore store(){return store;} public FuelManager fuel(){return fuel;} public ReactorManager reactors(){return reactors;} public CoolingManager cooling(){return cooling;} public RadiationManager radiation(){return radiation;} public ExplosionManager explosions(){return explosions;} public BuffManager buffs(){return buffs;}
    public NamespacedKey key(String value){return new NamespacedKey(this,value.toLowerCase().replaceAll("[^a-z0-9._-]","_"));}
}
