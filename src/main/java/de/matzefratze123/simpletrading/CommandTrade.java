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

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import de.matzefratze123.simpletrading.config.MessageConfiguration;
import de.matzefratze123.simpletrading.config.Messages;

public class CommandTrade implements CommandExecutor {

	private SimpleTrading main;
	
	public CommandTrade(SimpleTrading main) {
		this.main = main;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!cmd.getName().equalsIgnoreCase("trade")) {
			// This command doesn't belong to us
			return true;
		}
		
		if (!(sender instanceof Player)) {
			sender.sendMessage("The console cannot initiate a trade");
			return true;
		}
		
		Player player = (Player) sender;
		TradeFactory factory = main.getFactory();
		MessageConfiguration messageConfig = main.getMessageConfiguration();
		
		if (args.length < 1) {
			player.sendMessage(ChatColor.RED + "Usage: /trade <player>");
			return true;
		}
		
		if (args[0].equalsIgnoreCase("accept")) {
			if (!player.hasPermission("trade.allowtrade")) {
				player.sendMessage(ChatColor.RED + "You don't have permission!");
				return true;
			}
			
			Trade trade = factory.getTrade(player);
			if (trade == null || trade.getPartner().getPlayer() != player) {
				player.sendMessage(messageConfig.getMessage(Messages.NO_PENDING_REQUESTS, player.getName()));
				return true;
			}
			
			Player other = trade.getInitiator().getPlayer() == player ? trade.getPartner().getPlayer() : trade.getInitiator().getPlayer();
			
			int maxDistance = main.getConfiguration().getMaximumTradeDistance();
			if (player.getWorld() != other.getWorld() || player.getLocation().distanceSquared(other.getLocation()) > maxDistance * 2) {
				player.sendMessage(ChatColor.RED + "Your partner is too far away!");
				return true;
			}
			
			factory.acceptTrade(player);
		} else if (args[0].equalsIgnoreCase("decline")) {
			Trade trade = factory.getTrade(player);
			if (trade == null || trade.getPartner().getPlayer() != player) {
				player.sendMessage(messageConfig.getMessage(Messages.NO_PENDING_REQUESTS, player.getName()));
				return true;
			}
			
			factory.declineTrade(player);
			trade.getInitiator().getPlayer().sendMessage(messageConfig.getMessage(Messages.DECLINE_REQUEST_MESSAGE, player.getName()));
			player.sendMessage(messageConfig.getMessage(Messages.DECLINE_MESSAGE, trade.getInitiator().getName()));
		} else if (args[0].equalsIgnoreCase("reload")) {
			if (!player.hasPermission("trade.reload")) {
				player.sendMessage(ChatColor.RED + "You don't have permission!");
				return true;
			}
			
			main.reload();
			player.sendMessage(ChatColor.GRAY + "Plugin configurations reloaded!");
		} else {
			if (!player.hasPermission("trade.allowtrade")) {
				player.sendMessage(ChatColor.RED + "You don't have permission!");
				return true;
			}
			
			String partnerName = args[0];
			Player tradePartner = Bukkit.getPlayer(partnerName);
			if (tradePartner == null) {
				player.sendMessage(ChatColor.RED + "Player with name \'" + partnerName + "\' could not be found.");
				return true;
			}
			
			if (tradePartner == player) {
				player.sendMessage(ChatColor.RED + "You cannot trade with yourself!");
				return true;
			}
			
			if (!main.getConfiguration().allowsCreativeTrading() && (player.getGameMode() == GameMode.CREATIVE || tradePartner.getGameMode() == GameMode.CREATIVE)) {
				player.sendMessage(messageConfig.getMessage(Messages.CREATIVE, tradePartner.getName()));
				return true;
			}
			
			int maxDistance = main.getConfiguration().getMaximumTradeDistance();
			if (player.getWorld() != tradePartner.getWorld() || player.getLocation().distanceSquared(tradePartner.getLocation()) > maxDistance * 2 ) {
				player.sendMessage(ChatColor.RED + "Your partner is too far away!");
				return true;
			}
			
			factory.initiateTrade(player, tradePartner);
		}
		
		return true;
	}

}
