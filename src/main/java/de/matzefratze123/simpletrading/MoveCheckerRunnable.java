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

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.google.common.collect.Maps;

import de.matzefratze123.simpletrading.Trade.StopCause;
import de.matzefratze123.simpletrading.config.TradeConfiguration;

public class MoveCheckerRunnable implements Runnable {
	
	private final TradeConfiguration config;
	private final TradeFactory factory;
	private final Map<Player, Location> lastLocationMap;
	
	public MoveCheckerRunnable(TradeFactory factory, TradeConfiguration config) {
		this.factory = factory;
		this.lastLocationMap = Maps.newHashMap();
		this.config = config;
	}

	@Override
	public void run() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			Trade trade = factory.getTrade(player);
			
			if (trade == null || trade.getState() != TradeState.TRADING) {
				if (lastLocationMap.containsKey(player)) {
					lastLocationMap.remove(player);
				}
				
				continue;
			}
			
			if (!lastLocationMap.containsKey(player)) {
				lastLocationMap.put(player, player.getLocation());
			} else {
				Location last = lastLocationMap.get(player);
				Location now = player.getLocation();
				
				if (last.getWorld() != now.getWorld()) {
					factory.stopTrade(trade, StopCause.LEFT_WORLD, player);
				} else {
					double distanceSquared = trade.getInitiator().getPlayer().getLocation()
							.distanceSquared(trade.getPartner().getPlayer().getLocation());
					
					final int maxDistance = config.getMaximumTradeDistance();
					if (distanceSquared > Math.pow(maxDistance, 2)) {
						factory.stopTrade(trade, StopCause.MOVE, player);
					}
				}
			}
		}
	}
	
}
