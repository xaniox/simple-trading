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
package de.xaniox.simpletrading.config;

import de.xaniox.simpletrading.config.TradeConfiguration.ItemStackData;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ItemControlManager extends ControlManager<ItemStack> {
	
	private List<ItemStackData> items;
	private List<String> lores;
	
	public ItemControlManager(ControlMode mode, List<ItemStackData> items, List<String> lores) {
        super(mode);

        this.items = items;
		this.lores = lores;
	}
	
	public ItemControlManager(TradeConfiguration config) {
        super(config.getItemControlMode());

		updateValues(config);
	}

    @Override
	public void updateValues(TradeConfiguration config) {
		this.items = config.getItemControlList();
		this.lores = config.getItemControlLoreList();
	}
	
	@SuppressWarnings("deprecation")
    @Override
	protected boolean isAllowedBlacklist(ItemStack stack) {
		for (ItemStackData data : items) {
			if (data.getMaterial() == stack.getType() && data.getData() == stack.getData().getData()) {
				return false;
			}
		}
		
		ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return true;
        }

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
	
}
