/*
 * This file is part of SimpleTrading.
 * Copyright (c) 2015 matzefratze123
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.matzefratze123.simpletrading;

import java.io.File;
import java.io.IOException;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.mcstats.Metrics;

import de.matzefratze123.simpletrading.Trade.StopCause;
import de.matzefratze123.simpletrading.config.MessageConfiguration;
import de.matzefratze123.simpletrading.config.TradeConfiguration;

public class SimpleTrading extends JavaPlugin {
	
	private static final String VAULT_PLUGIN_NAME = "Vault";
	
	private File messageConfigFile;
	private TradeConfiguration config;
	private MessageConfiguration messageConfig;
	private TradeFactory factory;
	private BukkitTask movementTask;
	private ItemControlManager controlManager;
	
	private boolean usingVault;
	private Economy econ;

	public void onEnable() {
		File configFile = new File(this.getDataFolder(), "config.yml");
		messageConfigFile = new File(this.getDataFolder(), "messages.yml");
		
		if (!configFile.exists()) {
			saveDefaultConfig();
		}
		
		if (!messageConfigFile.exists()) {
			saveResource("messages.yml", false);
		} 
		
		config = new TradeConfiguration(getConfig());
		messageConfig = new MessageConfiguration(YamlConfiguration.loadConfiguration(messageConfigFile));
		
		initVaultHook();
		
		controlManager = new ItemControlManager(config);
		
		factory = new TradeFactory(this, messageConfig, config, econ, controlManager);
		
		getCommand("trade").setExecutor(new CommandTrade(this));
		movementTask = getServer().getScheduler().runTaskTimer(this, new MoveCheckerRunnable(factory, config), 20L, 30L);
		
		try {
			Metrics metrics = new Metrics(this);
			metrics.start();
		} catch (IOException e) {
			getLogger().warning("Could not start metrics service: " + e);
		}
		
		getLogger().info("Plugin successfully enabled");
	}
	
	@Override
	public void onDisable() {
		HandlerList.unregisterAll(this);
		
		if (movementTask != null) {
			movementTask.cancel();
		}
		
		if (factory != null) {
			factory.stopAllTrades(StopCause.SERVER_SHUTDOWN);
		}
	}

	public void initVaultHook() {
		PluginManager pluginManager = getServer().getPluginManager();
		if (!pluginManager.isPluginEnabled(VAULT_PLUGIN_NAME)) {
			return;
		}
		
		// Vault seems to exist so retrieve an provider instance of Economy.class
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return;
		}
		
		usingVault = true;
		econ = rsp.getProvider();
	}
	
	public void reload() {
		reloadConfig();
		config.loadByConfiguration(getConfig());
		
		Configuration messageConfiguration = YamlConfiguration.loadConfiguration(messageConfigFile);
		messageConfig.loadByConfiguration(messageConfiguration);
		
		controlManager.updateValues(config);
	}
	
	public boolean usesVault() {
		return usingVault;
	}
	
	public Economy getEconomy() {
		return econ;
	}
	
	public TradeFactory getFactory() {
		return factory;
	}
	
	public TradeConfiguration getConfiguration() {
		return config;
	}
	
	public MessageConfiguration getMessageConfiguration() {
		return messageConfig;
	}
	
}
