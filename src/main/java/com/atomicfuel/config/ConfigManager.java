package com.atomicfuel.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class ConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration c;
    public ConfigManager(JavaPlugin plugin) { this.plugin = plugin; this.c = plugin.getConfig(); }
    public void reload() { plugin.reloadConfig(); c = plugin.getConfig(); }
    public double d(String path) { return c.getDouble(path); }
    public int i(String path) { return c.getInt(path); }
    public boolean b(String path) { return c.getBoolean(path); }
    public List<String> list(String path) { return c.getStringList(path); }
}
