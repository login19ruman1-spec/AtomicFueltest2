package com.atomicfuel.command;

import com.atomicfuel.Main;
import com.atomicfuel.manager.FuelManager;
import com.atomicfuel.manager.RadiationManager;
import com.atomicfuel.manager.ReactorManager;
import com.atomicfuel.model.ReactorState;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public final class CommandHandler implements CommandExecutor, TabCompleter {
    private final Main plugin; private final FuelManager fuel; private final ReactorManager reactors; private final RadiationManager radiation;
    public CommandHandler(Main plugin,FuelManager fuel,ReactorManager reactors,RadiationManager radiation){this.plugin=plugin;this.fuel=fuel;this.reactors=reactors;this.radiation=radiation;}
    @Override public boolean onCommand(CommandSender s,Command c,String label,String[] a){
        if(a.length==0){if(admin(s))help(s);return true;}String sub=a[0].toLowerCase(Locale.ROOT);
        switch(sub){
            case "give"->{if(!admin(s))return true;if(a.length<3){s.sendMessage(Main.PREFIX+"§7/atomicfuel give <player> <type> [amount]");return true;}Player target=Bukkit.getPlayerExact(a[1]);FuelManager.Type t=fuel.parse(a[2]);if(target==null||t==null){s.sendMessage(Main.PREFIX+"§cUnknown player or type.");return true;}int n=Math.max(1,parseInt(a.length>3?a[3]:"1",1));fuel.give(target,t,n);s.sendMessage(Main.PREFIX+"§aGiven §f"+n+" "+t+"§a.");}
            case "status"->{if(!admin(s))return true;if(!(s instanceof Player p)){s.sendMessage(Main.PREFIX+"§cPlayers only.");return true;}ReactorState r=reactors.nearest(p.getLocation(),64);if(r==null){s.sendMessage(Main.PREFIX+"§7No reactor within 64 blocks.");return true;}s.sendMessage(Main.PREFIX+"§bReactor §7T="+(int)r.temperature()+" §7Stability="+(int)r.stability()+" §7Enrichment="+r.enrichment()+" §7Control="+r.controlRod()+" §7Coolant="+r.coolantCells()+" §7Reflectors="+r.reflectors()+" §7Countdown="+(r.countdownStart()>0));}
            case "stabilize"->{if(!admin(s)||!(s instanceof Player p))return true;ReactorState r=reactors.nearest(p.getLocation(),64);if(r!=null&&reactors.stabilize(r))s.sendMessage(Main.PREFIX+"§aNearest reactor stabilized.");else s.sendMessage(Main.PREFIX+"§cNo reactor nearby.");}
            case "cleanup"->{if(!admin(s)||!(s instanceof Player p))return true;double radius=a.length>1?parseDouble(a[1],16):16;s.sendMessage(Main.PREFIX+"§aRemoved §f"+radiation.cleanup(p.getLocation(),Math.max(1,radius))+" §aradiation zone(s).");}
            case "reload"->{if(!admin(s))return true;plugin.reloadPlugin();s.sendMessage(Main.PREFIX+"§aReloaded configuration.");}
            default->{if(admin(s))help(s);}
        }return true;
    }
    private boolean admin(CommandSender s){if(!(s instanceof Player p) || !p.isOp() || !s.hasPermission("atomicfuel.admin")){s.sendMessage(Main.PREFIX+"§cAdmin/OP only.");return false;}return true;}
    private void help(CommandSender s){s.sendMessage(Main.PREFIX+"§7/atomicfuel give <player> <type> [amount]");s.sendMessage(Main.PREFIX+"§7/atomicfuel status");s.sendMessage(Main.PREFIX+"§7/atomicfuel stabilize");s.sendMessage(Main.PREFIX+"§7/atomicfuel cleanup <radius>");s.sendMessage(Main.PREFIX+"§7/atomicfuel reload");}
    private int parseInt(String s,int d){try{return Integer.parseInt(s);}catch(Exception e){return d;}}private double parseDouble(String s,double d){try{return Double.parseDouble(s);}catch(Exception e){return d;}}
    @Override public List<String> onTabComplete(CommandSender s,Command c,String alias,List<String> args){
        if(!s.hasPermission("atomicfuel.admin") || !(s instanceof Player p) || !p.isOp()) return Collections.emptyList();
        if(args.size()==1)return filter(List.of("give","status","stabilize","cleanup","reload"),args.get(0));
        if(args.size()==2&&args.get(0).equalsIgnoreCase("give")){List<String>x=new ArrayList<>();for(Player p:Bukkit.getOnlinePlayers())x.add(p.getName());return filter(x,args.get(1));}
        if(args.size()==3&&args.get(0).equalsIgnoreCase("give")){List<String>x=new ArrayList<>();for(FuelManager.Type t:FuelManager.Type.values())x.add(t.name().toLowerCase(Locale.ROOT));return filter(x,args.get(2));}
        return Collections.emptyList();
    }
    private List<String> filter(List<String>x,String q){return x.stream().filter(v->v.toLowerCase(Locale.ROOT).startsWith(q.toLowerCase(Locale.ROOT))).toList();}
}
