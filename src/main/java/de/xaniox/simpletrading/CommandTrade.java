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

import com.google.common.collect.Lists;
import de.xaniox.simpletrading.config.TradeConfiguration;
import de.xaniox.simpletrading.config.WorldControlManager;
import de.xaniox.simpletrading.i18n.I18N;
import de.xaniox.simpletrading.i18n.I18NManager;
import de.xaniox.simpletrading.i18n.Messages;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginDescriptionFile;

import java.util.List;

public class CommandTrade implements CommandExecutor {

    private final I18N i18n = I18NManager.getGlobal();
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
			sender.sendMessage(i18n.getString(Messages.Command.ONLY_PLAYER));
			return true;
		}
		
		Player player = (Player) sender;
		TradeFactory factory = main.getFactory();
        WorldControlManager worldControlManager = main.getWorldControlManager();

		if (args.length < 1) {
            player.sendMessage(i18n.getVarString(Messages.Command.USAGE)
                .setVariable("usage", "/trade <player>")
                .toString());
			return true;
		}
		
		if (args[0].equalsIgnoreCase("accept") || args[0].equalsIgnoreCase("a")) {
			if (!player.hasPermission(Permissions.TRADE_ACCEPT.getPermission())) {
                player.sendMessage(i18n.getString(Messages.Command.INSUFFICIENT_PERMISSION));
				return true;
			}
			
			Trade trade = factory.getTrade(player);
			if (trade == null || trade.getPartner().getPlayer() != player) {
				player.sendMessage(i18n.getVarString(Messages.General.NO_PENDING_REQUESTS)
                    .setVariable("player", player.getName())
                    .toString());
				return true;
			}
			
			Player other = trade.getInitiator().getPlayer() == player ? trade.getPartner().getPlayer() : trade.getInitiator().getPlayer();
			
			int maxDistance = main.getConfiguration().getMaximumTradeDistance();
			if ((player.getWorld() != other.getWorld() || player.getLocation().distanceSquared(other.getLocation()) > maxDistance * 2)
                    && maxDistance != TradeConfiguration.NO_MAX_DISTANCE) {
                player.sendMessage(i18n.getString(Messages.Command.PARTNER_TOO_FAR_AWAY));
				return true;
			}

            if (!worldControlManager.isAllowed(player.getWorld())) {
                player.sendMessage(i18n.getString(Messages.Command.CANNOT_TRADE_IN_WORLD));
                return true;
            }

            if (!worldControlManager.isAllowed(other.getWorld())) {
                player.sendMessage(i18n.getString(Messages.Command.CANNOT_TRADE_IN_WORLD_PARTNER));
                return true;
            }
			
			factory.acceptTrade(player);
		} else if (args[0].equalsIgnoreCase("decline") || args[0].equalsIgnoreCase("d")) {
            if (!player.hasPermission(Permissions.TRADE_DENY.getPermission())) {
                player.sendMessage(i18n.getString(Messages.Command.INSUFFICIENT_PERMISSION));
                return true;
            }

			Trade trade = factory.getTrade(player);
			if (trade == null || trade.getPartner().getPlayer() != player) {
				player.sendMessage(i18n.getVarString(Messages.General.NO_PENDING_REQUESTS)
                    .setVariable("player", player.getName())
                    .toString());
				return true;
			}
			
			factory.declineTrade(player);
			trade.getInitiator().getPlayer().sendMessage(i18n.getVarString(Messages.General.TRADE_REQUEST_DECLINED)
                .setVariable("player", player.getName())
                .toString());
			player.sendMessage(i18n.getVarString(Messages.General.TRADE_DECLINED)
                .setVariable("player", trade.getInitiator().getName())
                .toString());
		} else if (args[0].equalsIgnoreCase("reload")) {
			if (!player.hasPermission(Permissions.RELOAD.getPermission())) {
                player.sendMessage(i18n.getString(Messages.Command.INSUFFICIENT_PERMISSION));
				return true;
			}
			
			main.reload();
            player.sendMessage(i18n.getString(Messages.Command.CONFIGURATIONS_RELOADED));
		} else if (args[0].equalsIgnoreCase("sign")) {
            if (!player.hasPermission(Permissions.SIGN.getPermission())) {
                player.sendMessage(i18n.getString(Messages.Command.INSUFFICIENT_PERMISSION));
                return true;
            }

            int loreIndex = 0;

            if (args.length > 1) {
                try {
                    loreIndex = Integer.parseInt(args[1]) - 1;
                } catch (NumberFormatException nfe) {
                    player.sendMessage(i18n.getVarString(Messages.Command.NOT_A_NUMBER)
                            .setVariable("number", args[1])
                            .toString());
                    return true;
                }
            }

            List<String> lores = main.getConfiguration().getItemControlLoreList();
            if (loreIndex >= lores.size()) {
                player.sendMessage(i18n.getVarString(Messages.Command.NO_LORE_WITH_NUMBER)
                        .setVariable("number", String.valueOf(loreIndex + 1))
                        .toString());
                return true;
            }

            String lore = lores.get(loreIndex);
            ItemStack stack = player.getItemInHand();

            if (stack == null) {
                player.sendMessage(i18n.getString(Messages.Command.NO_ITEM_IN_HAND));
                return true;
            }

            ItemMeta meta = stack.getItemMeta();
            List<String> itemLore = meta.getLore();
            if (itemLore == null) {
                itemLore = Lists.newArrayList();
            }

            itemLore.add(lore);
            meta.setLore(itemLore);

            stack.setItemMeta(meta);
            player.setItemInHand(stack);
            player.sendMessage(i18n.getString(Messages.Command.LORE_APPLIED));
        } else if (args[0].equalsIgnoreCase("version")) {
            PluginDescriptionFile desc = main.getDescription();

            player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "SimpleTrading " + ChatColor.GREEN + "v" + desc.getVersion()
                    + ChatColor.GRAY + " by " + ChatColor.GREEN + "Matze" + ChatColor.GRAY + " (@xaniox/@matzefratze123)");
		} else {
			if (!player.hasPermission(Permissions.TRADE_INITIATE.getPermission())) {
                player.sendMessage(i18n.getString(Messages.Command.INSUFFICIENT_PERMISSION));
                return true;
			}
			
			String partnerName = args[0];
			Player tradePartner = Bukkit.getPlayer(partnerName);
			if (tradePartner == null) {
                player.sendMessage(i18n.getVarString(Messages.Command.PLAYER_NOT_FOUND)
                    .setVariable("player", partnerName)
                    .toString());
				return true;
			}
			
			if (tradePartner == player) {
                player.sendMessage(i18n.getString(Messages.Command.NO_SELF_TRADE));
				return true;
			}
			
			if (!main.getConfiguration().allowsCreativeTrading() && (player.getGameMode() == GameMode.CREATIVE || tradePartner.getGameMode() == GameMode.CREATIVE)) {
                player.sendMessage(i18n.getVarString(Messages.General.PARTNER_IN_CREATIVE)
                    .setVariable("player", tradePartner.getName())
                    .toString());
				return true;
			}
			
			int maxDistance = main.getConfiguration().getMaximumTradeDistance();
			if ((player.getWorld() != tradePartner.getWorld() || player.getLocation().distanceSquared(tradePartner.getLocation()) > maxDistance * 2) &&
                    maxDistance != TradeConfiguration.NO_MAX_DISTANCE) {
                player.sendMessage(i18n.getString(Messages.Command.PARTNER_TOO_FAR_AWAY));
				return true;
			}

            if (!worldControlManager.isAllowed(player.getWorld())) {
                player.sendMessage(i18n.getString(Messages.Command.CANNOT_TRADE_IN_WORLD));
                return true;
            }

            if (!worldControlManager.isAllowed(tradePartner.getWorld())) {
                player.sendMessage(i18n.getString(Messages.Command.CANNOT_TRADE_IN_WORLD_PARTNER));
                return true;
            }
			
			factory.initiateTrade(player, tradePartner);
		}
		
		return true;
	}

}
