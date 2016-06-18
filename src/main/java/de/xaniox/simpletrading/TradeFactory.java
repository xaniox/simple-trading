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

import com.google.common.collect.Sets;
import de.xaniox.simpletrading.Trade.StopCause;
import de.xaniox.simpletrading.config.ItemControlManager;
import de.xaniox.simpletrading.config.TradeConfiguration;
import de.xaniox.simpletrading.config.WorldControlManager;
import de.xaniox.simpletrading.i18n.I18N;
import de.xaniox.simpletrading.i18n.I18NManager;
import de.xaniox.simpletrading.i18n.Messages;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.Set;

public class TradeFactory implements Listener {

    private final I18N i18n = I18NManager.getGlobal();
	private final SimpleTrading plugin;
	private final TradeConfiguration config;
	private final Set<Trade> trades;
	private final Economy econ;
	private final ItemControlManager controlManager;
	
	public TradeFactory(SimpleTrading plugin, TradeConfiguration config, Economy econ, ItemControlManager controlManager) {
		this.plugin = plugin;
		this.config = config;
		this.trades = Sets.newLinkedHashSet();
		this.econ = econ;
		this.controlManager = controlManager;
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	public Trade initiateTrade(Player initiator, Player partner) {
		Trade trade;
		
		if (isInvolvedInTrade(initiator)) {
			trade = null;
		} else if (isInvolvedInTrade(partner)) {
            initiator.sendMessage(i18n.getVarString(Messages.General.PARTNER_ALREADY_INVOLVED)
                .setVariable("player", partner.getName())
                .toString());
			trade = null;
		} else {
			DefaultTrade simpleTrade = new DefaultTrade(initiator, partner, config, econ, controlManager, plugin);
			
			int timeout = config.getTimeout();
			final BukkitTask timeoutTask = Bukkit.getScheduler().runTaskLater(plugin, new TimeoutRunnable(simpleTrade), timeout * 20L);
			
			simpleTrade.setListener(new DefaultTrade.StateChangedListener() {
				
				@Override
				public void onStateChanged(Trade trade, TradeState newState) {
					switch (newState) {
					case CANCELLED:
						//$FALL-THROUGH$
					case CONTRACTED:
						trades.remove(trade);
					case TRADING:
						int taskId = timeoutTask.getTaskId();
						BukkitScheduler scheduler = Bukkit.getScheduler();
						if (scheduler.isCurrentlyRunning(taskId) || scheduler.isQueued(taskId)) {
							timeoutTask.cancel();
						}
					default:
						break;
					}
				}
			});
			
			trades.add(simpleTrade);
			trade = simpleTrade;
			
			initiator.sendMessage(i18n.getVarString(Messages.General.TRADE_REQUESTED)
                    .setVariable("player", partner.getName())
                    .toString());
			partner.sendMessage(i18n.getVarString(Messages.General.TRADE_REQUEST_RECEIVED)
                    .setVariable("player", initiator.getName())
                    .toString());
		}
		
		return trade;
	}
	
	public void declineTrade(Player decliner) {
		Trade trade = getTrade(decliner);
		if (trade == null) {
			throw new IllegalStateException(decliner.getName() + " is not involved in any trade. Can not decline.");
		}
		
		if (trade.getState() != TradeState.REQUESTED) {
			return;
		}
		
		trade.setState(TradeState.CANCELLED);
		trades.remove(trade);
		
		Player initiator = trade.getInitiator().getPlayer();
        initiator.sendMessage(i18n.getVarString(Messages.General.TRADE_REQUEST_DECLINED)
            .setVariable("player", decliner.getName())
            .toString());
	}
	
	private void timeoutTrade(Trade trade) {
		if (trade.getState() != TradeState.REQUESTED) {
			return;
		}
		
		trades.remove(trade);
		trade.stop(StopCause.TIMEOUT, trade.getInitiator());
	}
	
	public void acceptTrade(Player accepter) {
		Trade trade = getTrade(accepter);
		if (trade == null) {
			throw new IllegalStateException(accepter.getName() + " is not involved in any trade. Can not accept.");
		}
		
		if (trade.getState() != TradeState.REQUESTED) {
			return;
		}
		
		Player initiator = trade.getInitiator().getPlayer();
        initiator.sendMessage(i18n.getVarString(Messages.General.TRADE_ACCEPTED)
            .setVariable("player", accepter.getName())
            .toString());
		
		trade.accept();
	}
	
	public void stopTrade(Trade trade, StopCause cause, Player who) {
		if (!trades.contains(trade)) {
			return;
		}
		
		TradePlayer tradePlayer;
		if (trade.getInitiator().getPlayer() == who) {
			tradePlayer = trade.getInitiator();
		} else if (trade.getPartner().getPlayer() == who) {
			tradePlayer = trade.getPartner();
		} else {
			throw new IllegalArgumentException(who.getName() + " is not affiliated in this trade");
		}
		
		trade.stop(cause, tradePlayer);
		trades.remove(trade);
	}
	
	public void stopAllTrades(StopCause cause) {
		for (Trade trade : trades) {
			trade.stop(cause, trade.getInitiator());
		}
	}
	
	public boolean isInvolvedInTrade(Player player) {
		for (Trade trade : trades) {
			if (trade.getInitiator().getPlayer() == player || trade.getPartner().getPlayer() == player) {
				return true;
			}
		}
		
		return false;
	}
	
	public Trade getTrade(Player player) {
		for (Trade trade : trades) {
			if (trade.getInitiator().getPlayer() == player || trade.getPartner().getPlayer() == player) {
				return trade;
			}
		}
		
		return null;
	}
	
	@EventHandler
	public void onPlayerInteractEntityEvent(PlayerInteractEntityEvent event) {
		Player player = event.getPlayer();
		if (!player.isSneaking()) {
			return;
		}
		
		Entity interacted = event.getRightClicked();
		if (!(interacted instanceof Player)) {
			return;
		}
		
		Player tradePartner = (Player) interacted;
		if (!config.allowsCreativeTrading() && (player.getGameMode() == GameMode.CREATIVE || tradePartner.getGameMode() == GameMode.CREATIVE)) {
            player.sendMessage(i18n.getVarString(Messages.General.PARTNER_IN_CREATIVE)
                .setVariable("player", tradePartner.getName())
                .toString());
			return;
		}

        WorldControlManager worldControlManager = plugin.getWorldControlManager();
        if (!worldControlManager.isAllowed(player.getWorld())) {
            player.sendMessage(i18n.getString(Messages.Command.CANNOT_TRADE_IN_WORLD));
            return;
        }
		
		if (!player.hasPermission(Permissions.TRADE_INITIATE_SHIFT.getPermission())) {
			player.sendMessage(ChatColor.RED + "You don't have permission to trade!");
			return;
		}
		
		Trade trade = getTrade(player);
		if (trade == null) {
			initiateTrade(player, tradePartner);
		} else if (trade.getPartner().getPlayer() == player) {
			acceptTrade(player);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerPickupItem(PlayerPickupItemEvent event) {
		Player player = event.getPlayer();
		Trade trade = getTrade(player);
		
		if (trade == null || trade.getState() != TradeState.TRADING) {
			return;
		}
		
		event.setCancelled(true);
	}
	
	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		HumanEntity entity = event.getPlayer();
		if (!(entity instanceof Player)) {
			return;
		}
		
		Player player = (Player) entity;
		Trade trade = getTrade(player);
		if (trade == null || trade.getState() != TradeState.TRADING) {
			return;
		}
		
		stopTrade(trade, StopCause.INVENTORY_CLOSE, player);
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		handleQuit(event);
	}
	
	@EventHandler
	public void onPlayerKick(PlayerKickEvent event) {
		handleQuit(event);
	}
	
	private void handleQuit(PlayerEvent event) {
		Player player = event.getPlayer();
		Trade trade = getTrade(player);
		
		if (trade == null) {
			return;
		}
		
		stopTrade(trade, StopCause.QUIT, player);
	}
	
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		Player player = event.getEntity();
		Trade trade = getTrade(player);
		
		if (trade == null || trade.getState() != TradeState.TRADING) {
			return;
		}
		
		stopTrade(trade, StopCause.DEATH, player);
	}
	
	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		HumanEntity entity = event.getWhoClicked();
		if (!(entity instanceof Player)) {
			return;
		}
		
		Player player = (Player) entity;
		Trade trade = getTrade(player);
		
		if (trade == null || trade.getState() != TradeState.TRADING) {
			return;
		}
		
		trade.onInventoryClick(event);
	}
	
	private class TimeoutRunnable implements Runnable {
		
		private Trade trade;
		
		public TimeoutRunnable(Trade trade) {
			this.trade = trade;
		}
		
		@Override
		public void run() {
			timeoutTrade(trade);
		}
		
	}
	
}
