package com.atomicfuel.manager;

import com.atomicfuel.Main;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Locale;

/** Creates and identifies all custom AtomicFuel items. */
public final class FuelManager {
    public enum Type {
        ATOMIC_FUEL_ROD, FUEL_ROD_BLOCK, ACTIVE_FUEL_ROD_BLOCK, CONTROL_ROD, COOLANT_CELL,
        NEUTRON_REFLECTOR, ATOMIC_BATTERY, TRITIUM_DUST, ENRICHED_IRON, RARE_ISOTOPE,
        RADIOACTIVE_WATER_BUCKET, CHARGED_WATER, RADAWAY, GEIGER_COUNTER, RADIOACTIVE_CORE
    }

    private final Main plugin;
    private final StateStore store;
    public FuelManager(Main plugin, StateStore store) { this.plugin = plugin; this.store = store; }

    private ItemStack item(Type type, int amount, Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material, amount);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        if(lore.length > 0) meta.setLore(List.of(lore));
        meta.getPersistentDataContainer().set(plugin.key("item"), PersistentDataType.STRING, type.name());
        stack.setItemMeta(meta);
        return stack;
    }

    public ItemStack create(Type type, int amount) {
        return switch(type) {
            case ATOMIC_FUEL_ROD -> setEnrichment(item(type, amount, Material.BLAZE_ROD, "§bAtomicFuelRod", "§7Fictional reactor fuel", "§8Enrichment: 1"), 1);
            case FUEL_ROD_BLOCK -> blockItem(type, amount, Material.GOLD_BLOCK, "§6FuelRodBlock");
            case ACTIVE_FUEL_ROD_BLOCK -> blockItem(type, amount, Material.GOLD_BLOCK, "§eActiveFuelRodBlock");
            case CONTROL_ROD -> item(type, amount, Material.COPPER_INGOT, "§aControlRod", "§7Stabilizes and controls a fictional reactor");
            case COOLANT_CELL -> item(type, amount, Material.PACKED_ICE, "§bCoolantCell", "§7Extra fictional cooling");
            case NEUTRON_REFLECTOR -> item(type, amount, Material.IRON_BLOCK, "§dNeutronReflector", "§7Boosts fictional power output");
            case ATOMIC_BATTERY -> setBatteryEnergy(item(type, amount, Material.COPPER_BLOCK, "§bAtomicBattery"), 0);
            case TRITIUM_DUST -> item(type, amount, Material.GLOWSTONE_DUST, "§3Tritium Dust");
            case ENRICHED_IRON -> item(type, amount, Material.IRON_INGOT, "§fEnriched Iron", "§7Fictional reinforced material");
            case RARE_ISOTOPE -> item(type, amount, Material.AMETHYST_SHARD, "§dRare Isotope");
            case RADIOACTIVE_WATER_BUCKET -> item(type, amount, Material.WATER_BUCKET, "§9Radioactive Water Bucket", "§cRadiation: 30s", "§7Right-click a block to create a RadiationZone");
            case CHARGED_WATER -> blockItem(type, amount, Material.LIGHT_BLUE_STAINED_GLASS, "§9ChargedWater");
            case RADAWAY -> item(type, amount, Material.POTION, "§aRadAway", "§7Reduces radiation and clears negative effects");
            case GEIGER_COUNTER -> item(type, amount, Material.COMPASS, "§eGeigerCounter", "§7Right-click to inspect nearby radiation");
            case RADIOACTIVE_CORE -> blockItem(type, amount, Material.RESPAWN_ANCHOR, "§4RadioactiveCore");
        };
    }

    private ItemStack blockItem(Type type, int amount, Material mat, String name) { return item(type, amount, mat, name, "§8AtomicFuel custom block"); }

    public Type type(ItemStack stack) {
        if(stack == null || !stack.hasItemMeta()) return null;
        String s = stack.getItemMeta().getPersistentDataContainer().get(plugin.key("item"), PersistentDataType.STRING);
        try { return s == null ? null : Type.valueOf(s); } catch (IllegalArgumentException ex) { return null; }
    }
    public boolean is(ItemStack stack, Type type) { return type(stack) == type; }
    public boolean isBlock(org.bukkit.block.Block block, Type type) { return type.name().equals(store.blockType(block.getLocation())); }
    public void tagBlock(org.bukkit.block.Block block, Type type) { store.tagBlock(block.getLocation(), type.name()); }
    public void untagBlock(org.bukkit.block.Block block) { store.untagBlock(block.getLocation()); }

    public int enrichment(ItemStack stack) {
        if(!is(stack, Type.ATOMIC_FUEL_ROD) || !stack.hasItemMeta()) return 1;
        return stack.getItemMeta().getPersistentDataContainer().getOrDefault(plugin.key("enrichment"), PersistentDataType.INTEGER, 1);
    }
    public ItemStack setEnrichment(ItemStack stack, int value) {
        if(!is(stack, Type.ATOMIC_FUEL_ROD)) return stack;
        ItemMeta meta=stack.getItemMeta(); int e=Math.max(1, Math.min(plugin.config().i("reactor.max-enrichment"), value));
        meta.getPersistentDataContainer().set(plugin.key("enrichment"), PersistentDataType.INTEGER, e);
        meta.setLore(List.of("§7Fictional reactor fuel", "§8Enrichment: "+e)); stack.setItemMeta(meta); return stack;
    }

    public boolean isBattery(ItemStack s) { return is(s, Type.ATOMIC_BATTERY); }
    public long batteryEnergy(ItemStack s) { if(!isBattery(s)) return 0; return s.getItemMeta().getPersistentDataContainer().getOrDefault(plugin.key("energy"), PersistentDataType.LONG, 0L); }
    public ItemStack setBatteryEnergy(ItemStack s, long energy) {
        if(!isBattery(s)) return s;
        ItemMeta m=s.getItemMeta(); long cap=plugin.config().i("energy.battery-capacity"); long e=Math.max(0,Math.min(cap,energy));
        m.getPersistentDataContainer().set(plugin.key("energy"),PersistentDataType.LONG,e);
        m.setLore(List.of("§7Stores AtomicEnergy","§8Energy: "+e+" / "+cap)); s.setItemMeta(m); return s;
    }
    public long chargeBattery(ItemStack s,long amount){long current=batteryEnergy(s);long cap=plugin.config().i("energy.battery-capacity");long add=Math.min(Math.max(0,amount),cap-current);setBatteryEnergy(s,current+add);return add;}
    public void give(Player p,Type type,int amount){p.getInventory().addItem(create(type,amount));}
    public Type parse(String value){try{return Type.valueOf(value.toUpperCase(Locale.ROOT).replace('-','_'));}catch(Exception e){return null;}}
}
