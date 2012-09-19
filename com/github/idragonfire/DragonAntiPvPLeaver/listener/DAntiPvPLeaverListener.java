package com.github.idragonfire.DragonAntiPvPLeaver.listener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredListener;

import com.github.idragonfire.DragonAntiPvPLeaver.DAntiPvPLeaverPlugin;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.topcat.npclib.DragonAntiPvPListener.entity.HumanNPC;

public class DAntiPvPLeaverListener implements Listener {
    private DAntiPvPLeaverPlugin antiPvP;

    public DAntiPvPLeaverListener(DAntiPvPLeaverPlugin antiPvP) {
        this.antiPvP = antiPvP;
    }

    public static boolean canBypass(Player player) {
        return player.hasPermission("dragonantipvpleaver.bypass");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (canBypass(player) || (player.getGameMode().getValue() == 1)) {
            return;
        }
        String name = player.getName();
        if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            WorldGuardPlugin worldGuard = (WorldGuardPlugin) Bukkit
                    .getPluginManager().getPlugin("WorldGuard");
            LocalPlayer localPlayer = worldGuard.wrapPlayer(player);
            RegionManager regionManager = worldGuard.getRegionManager(player
                    .getWorld());
            ApplicableRegionSet set = regionManager.getApplicableRegions(player
                    .getLocation());
            if (!set.allows(DefaultFlag.PVP, localPlayer)) {
                return;
            }
        }
        if (!this.antiPvP.playersNearby(player)) {
            return;
        }
        this.antiPvP.spawnHumanNPC(player, player.getLocation(), name);
        String npcSpawned = this.antiPvP.getLang("npcSpawned");
        this.antiPvP.broadcastNearPlayer(player, ChatColor.RED
                + player.getName() + ChatColor.YELLOW + " " + npcSpawned);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (canBypass(player)) {
            return;
        }
        final String name = player.getName();
        this.antiPvP.despawnHumanByName(name);
        if (!this.antiPvP.isDead(name)) {
            return;
        }
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.sendMessage(ChatColor.RED + " "
                + this.antiPvP.getLang("yourNPCKilled"));
        this.antiPvP.removeDead(player.getName());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!this.antiPvP.isAntiPvpNPC(event.getEntity())) {
            return;
        }
        HumanNPC npc = (HumanNPC) this.antiPvP.getOneHumanNPCByName(event
                .getEntity().getName());
        this.antiPvP.addDead(npc.getName());
        Bukkit.broadcastMessage(ChatColor.RED
                + this.antiPvP.getLang("npcKilled").replace("<Player>",
                        npc.getName()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageEvent event) {
        if (event.isCancelled()) {
            Bukkit.broadcastMessage("Some plugin cancel AntiPvP");

        }
        RegisteredListener[] listener = EntityDamageEvent.getHandlerList()
                .getRegisteredListeners();
        for (int i = 0; i < listener.length; i++) {
            Bukkit.broadcastMessage(listener[i].getListener().toString());
        }
        try {
            if (!this.antiPvP.isAntiPvpNPC(event.getEntity())) {
                return;
            }
            event.setCancelled(false);
            Player npc = (Player) event.getEntity();
            this.antiPvP.npcFirstTimeAttacked(npc.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}