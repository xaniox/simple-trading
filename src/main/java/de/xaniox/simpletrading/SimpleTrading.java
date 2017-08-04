/*
 * This file is part of SimpleTrading.
 * Copyright (c) 2015-2016 Matthias Werning
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
package de.xaniox.simpletrading;

import de.xaniox.simpletrading.Trade.StopCause;
import de.xaniox.simpletrading.config.ItemControlManager;
import de.xaniox.simpletrading.config.TradeConfiguration;
import de.xaniox.simpletrading.config.WorldControlManager;
import de.xaniox.simpletrading.i18n.I18N;
import de.xaniox.simpletrading.i18n.I18NBuilder;
import de.xaniox.simpletrading.i18n.I18NManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.mcstats.Metrics;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;

public class SimpleTrading extends JavaPlugin {

    private static final String I18N_CLASSPATH_FOLDER = "i18n/";
	private static final String VAULT_PLUGIN_NAME = "Vault";
	
	private TradeConfiguration config;
	private I18NManager i18nManager;
	private TradeFactory factory;
	private BukkitTask movementTask;
	private ItemControlManager itemControlManager;
    private WorldControlManager worldControlManager;
	
	private boolean usingVault;
	private Economy econ;
	private boolean isDisabling;

	public void onEnable() {
		File configFile = new File(this.getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            saveDefaultConfig();
        }

        Path dataFolder = getDataFolder().toPath();
        Configuration checkConfig = YamlConfiguration.loadConfiguration(configFile);
        checkConfigVersions(checkConfig, dataFolder);

        File i18nFolder = new File(getDataFolder(), "i18n");
        i18nFolder.mkdirs();

		config = new TradeConfiguration(getConfig());
        I18NManager.setGlobalBuilder(I18NBuilder.builder()
            .setFileSystemFolder(i18nFolder)
            .setClasspathFolder(I18N_CLASSPATH_FOLDER)
            .setLoadingMode(I18N.LoadingMode.FILE_SYSTEM)
            .setLocale(config.getLocale())
            .setLogger(getLogger()));
        i18nManager = new I18NManager();
		
		initVaultHook();
		
		itemControlManager = new ItemControlManager(config);
        worldControlManager = new WorldControlManager(config);
		
		factory = new TradeFactory(this, config, econ, itemControlManager);
		
		getCommand("trade").setExecutor(new CommandTrade(this));
		movementTask = getServer().getScheduler().runTaskTimer(this, new MoveCheckerRunnable(factory, config), 20L, 30L);

		try {
			Metrics metrics = new Metrics(this);
			metrics.start();
		} catch (IOException e) {
			getLogger().warning("Could not start metrics service: " + e);
		}

        PluginDescriptionFile pdf = getDescription();
        String version = pdf.getVersion();

        getLogger().info("SimpleTrading v" + version + " enabled!");
	}
	
	@Override
	public void onDisable() {
		isDisabling = true;
		HandlerList.unregisterAll(this);
		
		if (movementTask != null) {
			movementTask.cancel();
		}
		
		if (factory != null) {
			factory.stopAllTrades(StopCause.SERVER_SHUTDOWN);
		}
	}

    private void checkConfigVersions(Configuration config, Path dataFolder) {
        if (config.getInt("config-version", 0) < TradeConfiguration.CURRENT_CONFIG_VERSION) {
            Path configSource = dataFolder.resolve(TradeConfiguration.DESTINATION_FILE_NAME);
            Path configTarget = dataFolder.resolve("config_old.yml");

            try {
                Files.move(configSource, configTarget, StandardCopyOption.REPLACE_EXISTING);
                URL configResource = getClass().getResource(TradeConfiguration.CLASSPATH_RESOURCE_NAME);

                copyResource(configResource, configSource.toFile());

                ConsoleCommandSender sender = Bukkit.getConsoleSender();
                sender.sendMessage(ChatColor.RED + "Due to a SimpleTrading update your old configuration has been renamed");
                sender.sendMessage(ChatColor.RED + "to config_old.yml and a new one has been generated. Make sure to");
                sender.sendMessage(ChatColor.RED + "apply your old changes to the new config!");
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Could not create updated configuration due to an IOException", e);
            }
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
        i18nManager.reloadAll(config.getLocale());

		itemControlManager.updateValues(config);
        worldControlManager.updateValues(config);
	}

    public static void copyResource(URL resourceUrl, File destination) throws IOException {
        URLConnection connection = resourceUrl.openConnection();

        if (!destination.exists()) {
            destination.getParentFile().mkdirs();
            destination.createNewFile();
        }

        final int bufferSize = 1024;

        try (InputStream inStream = connection.getInputStream();
             FileOutputStream outStream = new FileOutputStream(destination)) {
            byte[] buffer = new byte[bufferSize];

            int read;
            while ((read = inStream.read(buffer)) > 0) {
                outStream.write(buffer, 0, read);
            }
        }
    }
	
	public boolean usesVault() {
		return usingVault;
	}
	
	public Economy getEconomy() {
		return econ;
	}

	public boolean isDisabling() {
		return isDisabling;
	}

	public TradeFactory getFactory() {
		return factory;
	}
	
	public TradeConfiguration getConfiguration() {
		return config;
	}

    public WorldControlManager getWorldControlManager() {
        return worldControlManager;
    }
}
