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

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import de.matzefratze123.simpletrading.config.TradeConfiguration;
import de.matzefratze123.simpletrading.config.TradeConfiguration.ItemStackData;

public class ItemControlManager {
	
	private ItemControlMode mode;
	private List<ItemStackData> items;
	private List<String> lores;
	
	public ItemControlManager(ItemControlMode mode, List<ItemStackData> items, List<String> lores) {
		this.mode = mode;
		this.items = items;
		this.lores = lores;
	}
	
	public ItemControlManager(TradeConfiguration config) {
		updateValues(config);
	}
	
	public void updateValues(TradeConfiguration config) {
		this.mode = config.getItemControlMode();
		this.items = config.getItemControlList();
		this.lores = config.getItemControlLoreList();
	}

	public boolean isTradeable(ItemStack stack) {
		boolean allowedBlacklist = isAllowedBlacklist(stack);
		
		return mode == ItemControlMode.BLACKLIST ? allowedBlacklist : !allowedBlacklist;
	}
	
	@SuppressWarnings("deprecation")
	private boolean isAllowedBlacklist(ItemStack stack) {
		for (ItemStackData data : items) {
			if (data.getMaterial() == stack.getType() && data.getData() == stack.getData().getData()) {
				return false;
			}
		}
		
		ItemMeta meta = stack.getItemMeta();
		List<String> lore = meta.getLore();
		
		if (lore != null) {
			for (String loreStr : lores) {	
				loreStr = ChatColor.stripColor(loreStr);
				
				for (String itemLoreStr : lore) {
					itemLoreStr = ChatColor.stripColor(itemLoreStr);
					
					if (loreStr.equals(itemLoreStr)) {
						return false;
					}
				}
			}
		}
		
		return true;
	}

	public enum ItemControlMode {
		
		BLACKLIST,
		WHITELIST;
		
		public static ItemControlMode getMode(String str, ItemControlMode def) {
			str = str.toUpperCase();
			
			for (ItemControlMode mode : values()) {
				if (mode.name().equals(str)) {
					return mode;
				}
			}
			
			if (def != null) {
				return def;
			} else throw new IllegalArgumentException("No enum constant \"" + str + "\" defined");
		}
		
	}
	
}
