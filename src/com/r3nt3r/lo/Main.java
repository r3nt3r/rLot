package com.r3nt3r.lo;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.minecraft.server.DataWatcher;
import net.minecraft.server.EntityLiving;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

	public String logPrefix = "[rLot] "; 
	public Logger log = Logger.getLogger("Minecraft"); 
	public File pFolder = new File("plugins" + File.separator + "rLot"); 
	private static Economy econ = null;
	 private boolean setupEconomy() {
	        if (getServer().getPluginManager().getPlugin("Vault") == null) {
	            return false;
	        }
	        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
	        if (rsp == null) {
	            return false;
	        }
	        econ = rsp.getProvider();
	        return econ != null;
	    }
    
    public void loadConfiguration(){
    getConfig().options().copyDefaults(true); 
    saveConfig();
    }
	@Override
	public void onEnable() {
		this.log.info(this.logPrefix + "rLot is initializing");
		if (!setupEconomy() ) {
            log.info(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
		try {
		    Metrics metrics = new Metrics(this);
		    metrics.start();
		} catch (IOException e) {
		    
		}
		loadConfiguration();
		final boolean updateEnable = getConfig().getBoolean("auto-update");
		if (updateEnable) {
			this.log.info(this.logPrefix + "Auto updateing is turned ON!");
			final Updater updater = new Updater(this, "rlot", this.getFile(), Updater.UpdateType.DEFAULT, true);
		} else {
			this.log.info(this.logPrefix + "Auto updateing is turned OFF!");
			final Updater updater = new Updater(this, "rlot", this.getFile(), Updater.UpdateType.NO_DOWNLOAD, true);
		}
		reloadConfig();
		this.log.info(this.logPrefix + "rLot is finished initializing");
	}

	@Override
	public void onDisable() {

	}

	public void playPotionEffect(final Player player,
			final LivingEntity entity, final int color, final int duration) {
		final EntityLiving el = ((CraftLivingEntity) entity).getHandle();
		final DataWatcher dw = el.getDataWatcher();
		dw.watch(8, Integer.valueOf(color));

		final Main plugin = new Main();

		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			@Override
			public void run() {
				int c = 0;
				if (!el.effects.isEmpty()) {
					c = net.minecraft.server.PotionBrewer.a(el.effects.values());
				}
				dw.watch(8, Integer.valueOf(c));
			}
		}, duration);
	}

	@Override
	public boolean onCommand(final CommandSender sender, final Command cmd,
			final String commandLabel, final String[] orgs) {

		if (cmd.getName().equalsIgnoreCase("rlot")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage("This command can only be run by a player.");
			} else {
				
				final Player player = (Player) sender;
				
				EconomyResponse  r= econ.withdrawPlayer(player.getName(), getConfig().getDouble("withdraw"));
				if(r.transactionSuccess()) {
	                sender.sendMessage(ChatColor.GREEN + String.format("You were taken %s for lottery!", econ.format(r.amount), econ.format(r.balance)));
	            }
				
				final Random random = new Random();
				final int Chance = random.nextInt(getConfig().getInt("maxnumber"));
				if (Chance >= getConfig().getInt("chance")) {
					final Player players[] = Bukkit.getOnlinePlayers();

					for (int i = 0; i < players.length; i++) {
						if (players[i].getDisplayName() == sender.getName()) {
							playPotionEffect(players[i], players[i], 0xFFFF00,
									400);
						}
						;
					}
					EconomyResponse  r1= econ.depositPlayer(player.getName(), getConfig().getDouble("deposit"));
					
					
					sender.sendMessage(ChatColor.GREEN + "Your number is "
							+ Chance + ". You won the lottery and "
							+ econ.format(r1.amount));
				} else {
					sender.sendMessage(ChatColor.RED
							+ "Your number is "
							+ Chance
							+ ". Sorry, you don't won the lottery try next time! :)");
				}
			}
			return true;
		}
		return false;

	}
	

}