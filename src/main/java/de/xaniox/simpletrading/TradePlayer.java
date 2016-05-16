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
package de.xaniox.simpletrading;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class TradePlayer {
	
	private final Player player;
	private Inventory inventory;
	private int expOffer;
	private int moneyOffer;
	private boolean accepted;
	
	public TradePlayer(Player player) {
		this.player = player;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public String getName() {
		return player.getName();
	}
	
	public Inventory getInventory() {
		return inventory;
	}
	
	public void setInventory(Inventory inventory) {
		this.inventory = inventory;
	}
	
	public void setExpOffer(int expOffer) {
		this.expOffer = expOffer;
	}
	
	public void incrementExpOffer() {
		expOffer++;
	}
	
	public int getExpOffer() {
		return expOffer;
	}
	
	public void setMoneyOffer(int moneyOffer) {
		this.moneyOffer = moneyOffer;
	}
	
	public void addMoneyToOffer(int money) {
		this.moneyOffer += money;
	}
	
	public int getMoneyOffer() {
		return moneyOffer;
	}
	
	public void setAccepted(boolean accepted) {
		this.accepted = accepted;
	}
	
	public boolean hasAccepted() {
		return accepted;
	}
	
}
