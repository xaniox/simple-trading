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
package de.matzefratze123.simpletrading.config;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import com.google.common.collect.Lists;

import de.matzefratze123.simpletrading.ItemControlManager.ItemControlMode;

public class TradeConfiguration { 
	
	private static final String BLOCKDATA_SEPERATOR = ":";
	private static final String PLAYERNAME_PLACEHOLDER = "@p";
	
	private ItemStackData acceptBlockData;
	private ItemStackData declineBlockData;
	private ItemStackData seperatorBlockData;
	private String inventoryName;
	private int maximumTradeDistance;
	private boolean allowCreativeTrading;
	private int timeout;
	private boolean useXpTrading;
	private ItemControlMode controlMode;
	private List<ItemStackData> controlItems;
	private List<String> controlLores;
	
	public TradeConfiguration(Configuration config) {
		loadByConfiguration(config);
	}
	
	public void loadByConfiguration(Configuration config) {
		ConfigurationSection blockSection = config.getConfigurationSection("blocks");
		acceptBlockData = ItemStackData.fromConfigString(blockSection.getString("accept", "ink_sack:10"), BLOCKDATA_SEPERATOR);
		declineBlockData = ItemStackData.fromConfigString(blockSection.getString("decline", "ink_sack:1"), BLOCKDATA_SEPERATOR);
		seperatorBlockData = ItemStackData.fromConfigString(blockSection.getString("seperator", "iron_fence"), BLOCKDATA_SEPERATOR);
		
		ConfigurationSection inventorySection = config.getConfigurationSection("inventory");
		inventoryName = inventorySection.getString("name", "SimpleTrading - @");
		
		ConfigurationSection globalSection = config.getConfigurationSection("global");
		maximumTradeDistance = globalSection.getInt("max-distance", 15);
		allowCreativeTrading = globalSection.getBoolean("creative-trading", true);
		timeout = globalSection.getInt("timeout", 60);
		useXpTrading = globalSection.getBoolean("use-xp-trading", true);
		
		ConfigurationSection itemControlSection = config.getConfigurationSection("item-control");
		controlMode = ItemControlMode.getMode(itemControlSection.getString("control-mode"), ItemControlMode.BLACKLIST);
		List<String> controlItemStringList = itemControlSection.getStringList("item-list");
		controlItems = Lists.newArrayList();
		
		for (String controlItemString : controlItemStringList) {
			controlItems.add(ItemStackData.fromConfigString(controlItemString, BLOCKDATA_SEPERATOR));
		}
		
		controlLores = itemControlSection.getStringList("item-lore");
	}
	
	public ItemStackData getAcceptBlockData() {
		return acceptBlockData;
	}
	
	public ItemStackData getDeclineBlockData() {
		return declineBlockData;
	}
	
	public ItemStackData getSeperatorBlockData() {
		return seperatorBlockData;
	}
	
	public String getInventoryName(String nameReplacement) {
		return inventoryName.replace(PLAYERNAME_PLACEHOLDER, nameReplacement);
	}
	
	public int getMaximumTradeDistance() {
		return maximumTradeDistance;
	}
	
	public boolean allowsCreativeTrading() {
		return allowCreativeTrading;
	}
	
	public int getTimeout() {
		return timeout;
	}
	
	public boolean usesXpTrading() {
		return useXpTrading;
	}
	
	public ItemControlMode getItemControlMode() {
		return controlMode;
	}
	
	public List<ItemStackData> getItemControlList() {
		return controlItems;
	}
	
	public List<String> getItemControlLoreList() {
		return controlLores;
	}
	
	public static class ItemStackData {
		
		private Material material;
		private byte data;
		
		public ItemStackData(Material material, byte data) {
			this.material = material;
			this.data = data;
		}
		
		@SuppressWarnings("deprecation")
		public static ItemStackData fromConfigString(String configStr, String seperator) {
			String[] components = configStr.split(seperator);
			String materialString = components[0];
			Material material = null;
			byte data = 0;
			
			for (Material mat : Material.values()) {
				if (mat.name().equalsIgnoreCase(materialString) ||
					mat.name().replace("_", "").equalsIgnoreCase(materialString)) {
					material = mat;
				}
			}
			
			if (material == null) {
				try {
					int legacyId = Integer.parseInt(materialString);
					material = Material.getMaterial(legacyId);
				} catch (NumberFormatException nfe) {
					// Give up
					throw new IllegalArgumentException("Config-String \"" + configStr + "\" material/block-id is invalid");
				}
			}
			
			if (components.length > 1) {
				try {
					int legacyData = Integer.parseInt(components[1]);
					data = (byte) legacyData;
				} catch (NumberFormatException nfe) {
					throw new IllegalArgumentException("Config-String \"" + configStr + "\" is invalid: Illegal block data");
				}
			}
			
			return new ItemStackData(material, data);
		}
		
		public Material getMaterial() {
			return material;
		}
		
		public byte getData() {
			return data;
		}

		public ItemStack newItemStack() {
			return newItemStack(1);
		}
		
		@SuppressWarnings("deprecation")
		public ItemStack newItemStack(int amount) {
			ItemStack stack = new ItemStack(material.getId(), amount, data);
			return stack;
		}
		
	}
	
}
