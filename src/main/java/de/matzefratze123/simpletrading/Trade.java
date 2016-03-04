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

import org.bukkit.event.inventory.InventoryClickEvent;

public interface Trade {
	
	public TradePlayer getInitiator();
	
	public TradePlayer getPartner();
	
	public TradeState getState();
	
	public void setState(TradeState state);
	
	public void accept();
	
	public void stop(StopCause cause, TradePlayer who);
	
	public void onInventoryClick(InventoryClickEvent event);
	
	public enum StopCause {
		
		INVENTORY_CLOSE,
		QUIT,
		DEATH,
		MOVE,
		LEFT_WORLD, 
		TIMEOUT,
		SERVER_SHUTDOWN
		
	}

	
}
