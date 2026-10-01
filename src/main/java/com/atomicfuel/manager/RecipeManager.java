package com.atomicfuel.manager;

import com.atomicfuel.Main;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;

import java.util.HashMap;
import java.util.Map;

/** Registers all requested vanilla-only crafting recipes. */
public final class RecipeManager {
    private final Main plugin; private final FuelManager fuel;
    public RecipeManager(Main plugin,FuelManager fuel){this.plugin=plugin;this.fuel=fuel;}
    public void registerAll(){
        custom("atomic_fuel_rod",FuelManager.Type.ATOMIC_FUEL_ROD,new String[]{"BPB","PGP","BPB"},mat('B',Material.BLAZE_POWDER,'P',Material.GUNPOWDER,'G',Material.NETHERITE_SCRAP),1);
        customExact("fuel_rod_block",FuelManager.Type.FUEL_ROD_BLOCK,new String[]{"FFF","FFF","FFF"},'F',FuelManager.Type.ATOMIC_FUEL_ROD,1);
        custom("control_rod",FuelManager.Type.CONTROL_ROD,new String[]{"CRC","RIR","CRC"},mat('C',Material.COPPER_INGOT,'R',Material.REDSTONE,'I',Material.IRON_INGOT),1);
        custom("coolant_cell",FuelManager.Type.COOLANT_CELL,new String[]{"SIS","IWI","SIS"},mat('S',Material.SNOWBALL,'I',Material.ICE,'W',Material.WATER_BUCKET),1);
        custom("neutron_reflector",FuelManager.Type.NEUTRON_REFLECTOR,new String[]{"ICI","CDC","ICI"},mat('I',Material.IRON_INGOT,'C',Material.COAL_BLOCK,'D',Material.DIAMOND),1);
        custom("atomic_battery",FuelManager.Type.ATOMIC_BATTERY,new String[]{"CRC","RGR","CRC"},mat('C',Material.COPPER_INGOT,'R',Material.REDSTONE,'G',Material.GOLD_INGOT),1);
        custom("radaway",FuelManager.Type.RADAWAY,new String[]{"MSG","SBS","GSG"},mat('M',Material.MILK_BUCKET,'S',Material.SUGAR,'G',Material.GOLD_INGOT,'B',Material.GLASS_BOTTLE),2);
        custom("geiger_counter",FuelManager.Type.GEIGER_COUNTER,new String[]{"IRI","RGR","IRI"},mat('I',Material.IRON_INGOT,'R',Material.REDSTONE,'G',Material.GLASS),1);
    }
    private Map<Character,Material> mat(Object... a){Map<Character,Material>m=new HashMap<>();for(int i=0;i<a.length;i+=2)m.put((Character)a[i],(Material)a[i+1]);return m;}
    private void custom(String id,FuelManager.Type out,String[] shape,Map<Character,Material> mats,int amount){ShapedRecipe r=new ShapedRecipe(new NamespacedKey(plugin,id),fuel.create(out,amount));r.shape(shape);mats.forEach(r::setIngredient);plugin.getServer().addRecipe(r);}
    private void customExact(String id,FuelManager.Type out,String[] shape,char key,FuelManager.Type ingredient,int amount){ShapedRecipe r=new ShapedRecipe(new NamespacedKey(plugin,id),fuel.create(out,amount));r.shape(shape);r.setIngredient(key,new RecipeChoice.ExactChoice(fuel.create(ingredient,1)));plugin.getServer().addRecipe(r);}
}
