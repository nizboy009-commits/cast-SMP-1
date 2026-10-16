package com.pinak.castsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public final class CastSMPPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private NamespacedKey castKey;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private static final List<String> CASTS = List.of("adrenaline","nimbus","puppet","terra","anchor","plague","helix","lunar","solar","vital","resonant","rime","necro","abyssal");
    private final Map<UUID, String[]> slots = new HashMap<>();
    @Override public void onEnable() {
        saveDefaultConfig(); castKey = new NamespacedKey(this, "cast_type");
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("spellbook")).setExecutor(this);
        Objects.requireNonNull(getCommand("cast")).setExecutor(this);
        Objects.requireNonNull(getCommand("cast")).setTabCompleter(this);
        getLogger().info("CastSMP enabled. Resource pack textures require the included 26.2 pack.");
    }
    private ItemStack makeCast(String type) {
        Material base = switch(type) {
            case "adrenaline" -> Material.BLAZE_POWDER; case "nimbus" -> Material.FEATHER; case "puppet" -> Material.STRING;
            case "terra" -> Material.IRON_PICKAXE; case "anchor" -> Material.HEAVY_CORE; case "plague" -> Material.SPIDER_EYE;
            case "helix" -> Material.PRISMARINE_CRYSTALS; case "lunar" -> Material.AMETHYST_SHARD; case "solar" -> Material.BLAZE_ROD;
            case "vital" -> Material.GHAST_TEAR; case "resonant" -> Material.ECHO_SHARD; case "rime" -> Material.PACKED_ICE;
            case "necro" -> Material.BONE; default -> Material.ENDER_EYE;
        };
        ItemStack item = new ItemStack(base); ItemMeta meta = item.getItemMeta();
        String title = title(type);
        meta.displayName(Component.text(title + " Cast", NamedTextColor.LIGHT_PURPLE));
        meta.lore(List.of(Component.text("Cast SMP Ability", NamedTextColor.GRAY), Component.text("Right-click to activate", NamedTextColor.YELLOW)));
        meta.getPersistentDataContainer().set(castKey, PersistentDataType.STRING, type);
        // Model IDs are provided by the separate resource pack.
        meta.setItemModel(NamespacedKey.fromString("castsmp:" + type));
        item.setItemMeta(meta); return item;
    }
    private String title(String s) { return s.substring(0,1).toUpperCase(Locale.ROOT)+s.substring(1); }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("spellbook")) {
            if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
            Inventory inv = Bukkit.createInventory(null, 27, Component.text("Cast SMP Spellbook", NamedTextColor.DARK_PURPLE));
            for (int i=0;i<CASTS.size();i++) inv.setItem(i, makeCast(CASTS.get(i)));
            p.openInventory(inv); return true;
        }
        if (args.length == 0) { sender.sendMessage("/cast give <cast> [player] | /cast spells | /cast reload"); return true; }
        if (args[0].equalsIgnoreCase("reload")) { if (!sender.hasPermission("castsmp.admin")) return noPerm(sender); reloadConfig(); sender.sendMessage("CastSMP config reloaded."); return true; }
        if (args[0].equalsIgnoreCase("spells")) { if (sender instanceof Player p) { onCommand(sender, Bukkit.getPluginCommand("spellbook"), "spellbook", new String[0]); } else sender.sendMessage(String.join(", ", CASTS)); return true; }
        if (args[0].equalsIgnoreCase("give")) {
            if (!sender.hasPermission("castsmp.admin")) return noPerm(sender);
            if (args.length < 2 || !CASTS.contains(args[1].toLowerCase(Locale.ROOT))) { sender.sendMessage("Valid casts: " + String.join(", ", CASTS)); return true; }
            Player target = args.length >= 3 ? Bukkit.getPlayerExact(args[2]) : (sender instanceof Player p ? p : null);
            if (target == null) { sender.sendMessage("Player not found; use /cast give <cast> <online-player>"); return true; }
            target.getInventory().addItem(makeCast(args[1].toLowerCase(Locale.ROOT))).values().forEach(drop -> target.getWorld().dropItemNaturally(target.getLocation(), drop));
            sender.sendMessage("Gave " + args[1] + " cast to " + target.getName()); return true;
        }
        sender.sendMessage("Unknown subcommand."); return true;
    }
    private boolean noPerm(CommandSender s) { s.sendMessage("You do not have permission."); return true; }
    @Override public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args) {
        if (!c.getName().equalsIgnoreCase("cast")) return List.of();
        if (args.length == 1) return List.of("give","spells","reload").stream().filter(x->x.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) return CASTS.stream().filter(x->x.startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(x->x.toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT))).toList();
        return List.of();
    }
    @EventHandler public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || !Component.text("Cast SMP Spellbook", NamedTextColor.DARK_PURPLE).equals(e.getView().title())) return;
        e.setCancelled(true); ItemStack item=e.getCurrentItem(); if(item==null || !item.hasItemMeta()) return;
        String type=item.getItemMeta().getPersistentDataContainer().get(castKey, PersistentDataType.STRING);
        if(type!=null) { p.getInventory().addItem(makeCast(type)); p.sendMessage(Component.text("Added "+title(type)+" Cast to your inventory.", NamedTextColor.GREEN)); }
    }
    @EventHandler public void onUse(PlayerInteractEvent e) {
        if(e.getHand()!=EquipmentSlot.HAND || (e.getAction()!=Action.RIGHT_CLICK_AIR && e.getAction()!=Action.RIGHT_CLICK_BLOCK)) return;
        ItemStack item=e.getItem(); if(item==null || !item.hasItemMeta()) return;
        String type=item.getItemMeta().getPersistentDataContainer().get(castKey, PersistentDataType.STRING); if(type==null) return;
        e.setCancelled(true); Player p=e.getPlayer(); if(!p.hasPermission("castsmp.use")) return;
        long now=System.currentTimeMillis(); long until=cooldowns.getOrDefault(p.getUniqueId(), Map.of()).getOrDefault(type,0L);
        if(until>now) { p.sendMessage(Component.text("Cooldown: "+((until-now+999)/1000)+"s", NamedTextColor.RED)); return; }
        activate(p,type); long seconds=Math.max(1,getConfig().getLong("cooldowns."+type,60)); cooldowns.computeIfAbsent(p.getUniqueId(),k->new HashMap<>()).put(type,now+seconds*1000L);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.8f, 1.2f);
        p.sendMessage(Component.text(title(type)+" Cast activated!", NamedTextColor.LIGHT_PURPLE));
    }
    private void activate(Player p, String type) {
        Location loc=p.getLocation(); World w=p.getWorld();
        switch(type) {
            case "adrenaline" -> { p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,200,1)); p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,200,1)); Bukkit.getScheduler().runTaskLater(this,()->{if(p.isOnline())p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,160,0));},200L); }
            case "nimbus" -> { Vector v=loc.getDirection().multiply(1.0); v.setY(1.0); p.setVelocity(v); w.spawnParticle(Particle.CLOUD,loc,35,.5,.4,.5,.08); }
            case "puppet" -> { for(Entity en:p.getNearbyEntities(3,3,3)) if(en instanceof Player t && t!=p) { t.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,60,6)); t.damage(4,p); } }
            case "terra" -> { Block target=loc.getBlock().getRelative(loc.getBlock().getFace(loc.getBlock())); p.setVelocity(loc.getDirection().multiply(.35)); w.spawnParticle(Particle.BLOCK,loc,24,.5,.5,.5, Bukkit.createBlockData(Material.DIRT)); }
            case "anchor" -> { for(Entity en:p.getNearbyEntities(5,5,5)) if(en instanceof Player t && t!=p) t.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,160,4)); }
            case "plague" -> { for(Entity en:p.getNearbyEntities(4,3,4)) if(en instanceof Player t && t!=p) t.addPotionEffect(new PotionEffect(PotionEffectType.POISON,60,1)); }
            case "helix" -> { p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,500,0)); p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,200,0)); w.spawnParticle(Particle.END_ROD,loc,40,.7,.8,.7,.06); }
            case "lunar" -> { for(Entity en:p.getNearbyEntities(8,8,8)) if(en instanceof Player t && t!=p) t.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,100,0)); w.spawnParticle(Particle.SOUL,loc,70,2,1,2,.03); }
            case "solar" -> { for(Entity en:p.getNearbyEntities(8,5,8)) if(en instanceof Player t && t!=p && t.getLocation().distance(loc)<8 && p.hasLineOfSight(t)) t.damage(3,p); w.spawnParticle(Particle.FLAME,loc,50,1,1,1,.08); }
            case "vital" -> { for(Entity en:p.getNearbyEntities(5,5,5)) if(en instanceof Player t) t.setHealth(Math.min(t.getMaxHealth(),t.getHealth()+8)); p.setHealth(Math.min(p.getMaxHealth(),p.getHealth()+8)); p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION,200,1)); }
            case "resonant" -> { w.playSound(loc,Sound.BLOCK_SCULK_SHRIEKER_SHRIEK,1,1); w.spawnParticle(Particle.SONIC_BOOM,loc.add(0,1,0),1,0,0,0,0); for(Entity en:p.getNearbyEntities(10,5,10)) if(en instanceof Player t && t!=p) t.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,300,0)); }
            case "rime" -> { for(Entity en:p.getNearbyEntities(3,3,3)) if(en instanceof Player t && t!=p) { t.setFreezeTicks(Math.min(t.getMaxFreezeTicks(),t.getFreezeTicks()+100)); t.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,120,2)); } w.spawnParticle(Particle.SNOWFLAKE,loc,50,1,1,1,.02); }
            case "necro" -> { for(int i=0;i<3;i++) { Location at=loc.clone().add((i-1)*1.5,0,1); Skeleton s=w.spawn(at,Skeleton.class); s.setTarget(p.getTargetEntity() instanceof LivingEntity le ? le : null); s.setPersistent(false); Bukkit.getScheduler().runTaskLater(this,()->{if(s.isValid())s.remove();},400L); } }
            case "abyssal" -> { for(Entity en:p.getNearbyEntities(5,4,5)) if(en instanceof Player t && t!=p) { t.damage(5,p); t.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS,100,0)); } w.spawnParticle(Particle.PORTAL,loc,100,2,1,2,.1); }
        }
    }
}
