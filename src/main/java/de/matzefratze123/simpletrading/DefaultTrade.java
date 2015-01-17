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
import java.util.Map;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.collect.Lists;

import de.matzefratze123.simpletrading.config.MessageConfiguration;
import de.matzefratze123.simpletrading.config.Messages;
import de.matzefratze123.simpletrading.config.TradeConfiguration;

@SuppressWarnings("deprecation")
public class DefaultTrade implements Trade {

	private static final int INVENTORY_SIZE = 6 * 9;
	private static final int[] SEPERATOR_INDEXES = { 0, 1, 7, 8, 9, 10, 11, 15, 16, 17, 31, 40, 49 };
	
	private static final MaterialData EXPERIENCE_MATERIAL_DATA = new MaterialData(Material.EXP_BOTTLE);
	private static final MaterialData MONEY_MATERIAL_DATA = new MaterialData(Material.GOLD_NUGGET);
	private static final MaterialData UNCONFIRMED_STATUS_MATERIAL_DATA = new MaterialData(Material.STAINED_GLASS, (byte) 14);
	private static final MaterialData CONFIRMED_STATUS_MATERIAL_DATA = new MaterialData(Material.STAINED_GLASS, (byte) 5);
	
	private static final int EXP_INFO_INDEX = 2;
	private static final int ACCEPT_TRADE_INDEX = 3;
	private static final int CONFIRMATION_INFO_INDEX = 4;
	private static final int DECLINE_TRADE_INDEX = 5;
	private static final int MONEY_INFO_INDEX = 6;
	private static final int ADD_50_INDEX = 12;
	private static final int ADD_100_INDEX = 13;
	private static final int ADD_500_INDEX = 14;
	private static final int ADD_EXP_LEVEL_INDEX = 22;
	
	private static final float ADD_PITCH = 1.5F;
	private static final float REMOVE_PITCH = 1.0F;
	
	private final TradePlayer initiator;
	private final TradePlayer partner;
	
	private final JavaPlugin plugin;
	private final TradeConfiguration config;
	private final MessageConfiguration messageConfig;
	private final Economy econ;
	
	private StateChangedListener listener;
	private TradeState state;
	
	public DefaultTrade(Player initiator, Player partner, TradeConfiguration config, MessageConfiguration messageConfig, Economy econ, JavaPlugin plugin) {
		this.plugin = plugin;
		this.initiator = new TradePlayer(initiator);
		this.partner = new TradePlayer(partner);
		this.config = config;
		this.messageConfig = messageConfig;
		this.econ = econ;
		this.state = TradeState.REQUESTED;
	}
	
	@Override
	public TradePlayer getInitiator() {
		return initiator;
	}

	@Override
	public TradePlayer getPartner() {
		return partner;
	}

	@Override
	public TradeState getState() {
		return state;
	}
	
	public void setListener(StateChangedListener listener) {
		this.listener = listener;
	}
	
	@Override
	public void setState(TradeState state) {
		this.state = state;
		
		if (listener != null) {
			listener.onStateChanged(this, state);
		}
	}
	
	@Override
	public void accept() {
		final int maxInvNameLength = 32;
		String inventoryTitleInitiator = config.getInventoryName(partner.getName());
		String inventoryTitlePartner = config.getInventoryName(initiator.getName());
		
		if (inventoryTitleInitiator.length() > maxInvNameLength) {
			inventoryTitleInitiator = inventoryTitleInitiator.substring(0, maxInvNameLength);
		}
		
		if (inventoryTitlePartner.length() > maxInvNameLength) {
			inventoryTitlePartner = inventoryTitlePartner.substring(0, maxInvNameLength);
		}
		
		Inventory initiatorInventory = Bukkit.createInventory(null, INVENTORY_SIZE, inventoryTitleInitiator);
		Inventory partnerInventory = Bukkit.createInventory(null, INVENTORY_SIZE, inventoryTitlePartner);
		
		initializeInventory(initiatorInventory);
		initializeInventory(partnerInventory);
		
		initiator.setInventory(initiatorInventory);
		partner.setInventory(partnerInventory);
		
		initiator.getPlayer().openInventory(initiatorInventory);
		partner.getPlayer().openInventory(partnerInventory);
		
		setState(TradeState.TRADING);
	}
	
	private void initializeInventory(Inventory inv) {
		ItemStack seperator = config.getSeperatorBlockData().newItemStack();
		ItemMeta seperatorMeta = seperator.getItemMeta();
		seperatorMeta.setDisplayName(ChatColor.ITALIC.toString());
		seperator.setItemMeta(seperatorMeta);
		
		for (int seperatorIndex : SEPERATOR_INDEXES) {
			inv.setItem(seperatorIndex, seperator);
		}
		
		ItemStack expInfoItemStack = EXPERIENCE_MATERIAL_DATA.toItemStack(1);
		ItemMeta expInfoMeta = expInfoItemStack.getItemMeta();
		expInfoMeta.setDisplayName(ChatColor.DARK_GREEN + "Experience Trades");
		List<String> expLore = Lists.newArrayList();
		expLore.add(getOfferLoreString(getGenitive(initiator.getName()), "0"));
		expLore.add(getOfferLoreString(getGenitive(partner.getName()), "0"));
		expInfoMeta.setLore(expLore);
		expInfoItemStack.setItemMeta(expInfoMeta);
		
		ItemStack acceptItemStack = config.getAcceptBlockData().newItemStack(1);
		ItemMeta acceptMeta = acceptItemStack.getItemMeta();
		acceptMeta.setDisplayName(ChatColor.GREEN + "Accept Trade");
		acceptItemStack.setItemMeta(acceptMeta);
		
		ItemStack unconfirmedStatusItemStack = UNCONFIRMED_STATUS_MATERIAL_DATA.toItemStack(1);
		ItemMeta unconfirmedStatusMeta = unconfirmedStatusItemStack.getItemMeta();
		unconfirmedStatusMeta.setDisplayName(ChatColor.RED + "Trade Status");
		unconfirmedStatusMeta.setLore(Lists.newArrayList(ChatColor.WHITE + "Waiting for other player to accept"));
		unconfirmedStatusItemStack.setItemMeta(unconfirmedStatusMeta);
		
		ItemStack declineItemStack = config.getDeclineBlockData().newItemStack(1);
		ItemMeta declineMeta = declineItemStack.getItemMeta();
		declineMeta.setDisplayName(ChatColor.RED + "Decline Trade");
		declineItemStack.setItemMeta(declineMeta);
		
		ItemStack moneyInfoItemStack = MONEY_MATERIAL_DATA.toItemStack(1);
		ItemMeta moneyMeta = moneyInfoItemStack.getItemMeta();
		moneyMeta.setDisplayName(ChatColor.GOLD + "Money Trades");
		List<String> moneyLore = Lists.newArrayList();
		moneyLore.add(getOfferLoreString(getGenitive(initiator.getName()), econ.format(0)));
		moneyLore.add(getOfferLoreString(getGenitive(partner.getName()), econ.format(0)));
		moneyMeta.setLore(moneyLore);
		moneyInfoItemStack.setItemMeta(moneyMeta);
		
		List<String> addMoneyLore = Lists.newArrayList();
		addMoneyLore.add(ChatColor.GRAY + "Left-Click to add money");
		addMoneyLore.add(ChatColor.GRAY + "Right-Click to remove money");
		
		ItemStack add50ItemStack = MONEY_MATERIAL_DATA.toItemStack(1);
		ItemMeta meta50 = add50ItemStack.getItemMeta();
		meta50.setDisplayName(ChatColor.WHITE + "Add/Remove " + econ.format(50));
		meta50.setLore(addMoneyLore);
		add50ItemStack.setItemMeta(meta50);
		
		ItemStack add100ItemStack = MONEY_MATERIAL_DATA.toItemStack(1);
		ItemMeta meta100 = add100ItemStack.getItemMeta();
		meta100.setDisplayName(ChatColor.WHITE + "Add/Remove " + econ.format(100));
		meta100.setLore(addMoneyLore);
		add100ItemStack.setItemMeta(meta100);
		
		ItemStack add500ItemStack = MONEY_MATERIAL_DATA.toItemStack(1);
		ItemMeta meta500 = add500ItemStack.getItemMeta();
		meta500.setDisplayName(ChatColor.WHITE + "Add/Remove " + econ.format(500));
		meta500.setLore(addMoneyLore);
		add500ItemStack.setItemMeta(meta500);
		
		ItemStack addExpLevelItemStack = EXPERIENCE_MATERIAL_DATA.toItemStack(1);
		ItemMeta addExpLevelMeta = addExpLevelItemStack.getItemMeta();
		addExpLevelMeta.setDisplayName(ChatColor.WHITE + "Add/Remove one exp level");
		List<String> addExpLevelLore = Lists.newArrayList();
		addExpLevelLore.add(ChatColor.GRAY + "Left-Click to add a level");
		addExpLevelLore.add(ChatColor.GRAY + "Right-Click to remove a level");
		addExpLevelMeta.setLore(addExpLevelLore);
		addExpLevelItemStack.setItemMeta(addExpLevelMeta);
		
		inv.setItem(EXP_INFO_INDEX, expInfoItemStack);
		inv.setItem(ACCEPT_TRADE_INDEX, acceptItemStack);
		inv.setItem(CONFIRMATION_INFO_INDEX, unconfirmedStatusItemStack);
		inv.setItem(DECLINE_TRADE_INDEX, declineItemStack);
		inv.setItem(MONEY_INFO_INDEX, moneyInfoItemStack);
		inv.setItem(ADD_50_INDEX, add50ItemStack);
		inv.setItem(ADD_100_INDEX, add100ItemStack);
		inv.setItem(ADD_500_INDEX, add500ItemStack);
		inv.setItem(ADD_EXP_LEVEL_INDEX, addExpLevelItemStack);
	}
	
	private String getGenitive(String name) {
		name += '\'';
		if (!name.endsWith("s")) {
			name += 's';
		}
		
		return name;
	}
	
	private String getOfferLoreString(String name, String value) {
		return ChatColor.DARK_GRAY + name + " offers: " + ChatColor.GRAY + ChatColor.BOLD + value;
	}
	
	@Override
	public void stop(StopCause cause, TradePlayer who) {
		TradePlayer other = who == initiator ? partner : initiator;
		
		TradeState state = getState();
		setState(TradeState.CANCELLED);
		
		if (state == TradeState.TRADING) {
			reclaimItems(initiator);
			reclaimItems(partner);
			
			initiator.getPlayer().closeInventory();
			partner.getPlayer().closeInventory();
			
			Bukkit.getScheduler().runTask(plugin, new Runnable() {
				
				@Override
				public void run() {
					initiator.getPlayer().updateInventory();
					partner.getPlayer().updateInventory();
				}
			});
		}
		
		
		switch (cause) {
		case DEATH:
			other.getPlayer().sendMessage(messageConfig.getMessage(Messages.DEATH_MESSAGE, who.getName()));
			break;
		case INVENTORY_CLOSE:
			other.getPlayer().sendMessage(messageConfig.getMessage(Messages.CANCEL_MESSAGE, who.getName()));
			who.getPlayer().sendMessage(messageConfig.getMessage(Messages.DECLINE_MESSAGE, other.getName()));
			break;
		case LEFT_WORLD:
			other.getPlayer().sendMessage(messageConfig.getMessage(Messages.LEFT_WORLD, who.getName()));
			break;
		case MOVE:
			other.getPlayer().sendMessage(messageConfig.getMessage(Messages.MOVED_AWAY, who.getName()));
			break;
		case QUIT:
			other.getPlayer().sendMessage(messageConfig.getMessage(Messages.LEFT_MESSAGE, who.getName()));
			break;
		case TIMEOUT:
			who.getPlayer().sendMessage(messageConfig.getMessage(Messages.REQUEST_TIMED_OUT, other.getName()));
			other.getPlayer().sendMessage(messageConfig.getMessage(Messages.REQUEST_TIMED_OUT, who.getName()));
			break;
		case SERVER_SHUTDOWN:
			who.getPlayer().sendMessage(ChatColor.RED + "Stopping trade as plugin is shutting down");
			other.getPlayer().sendMessage(ChatColor.RED + "Stopping trade as plugin is shutting down");
		default:
			break;
		}
	}
	
	private void reclaimItems(TradePlayer player) {
		Inventory inv = player.getInventory();
	
		for (int y = 2; y < 6; y++) {
			for (int x = 0; x < 4; x++) {
				int slot = y * 9 + x;
				
				ItemStack current = inv.getItem(slot);
				if (current != null) {
					player.getPlayer().getInventory().addItem(current);
				}
				
				inv.setItem(slot, null);
			}
		}
	}

	@Override
	public void onInventoryClick(InventoryClickEvent event) {
		Player player = (Player) event.getWhoClicked();
		SlotType type = event.getSlotType();

		if (type != SlotType.CONTAINER && type != SlotType.QUICKBAR) {
			return;
		}
		
		InventoryView view = event.getView();
		Inventory inventory = null;
		int rawSlot = event.getRawSlot();
		
		if (view.getTopInventory() != null && rawSlot < view.getTopInventory().getSize()) {
			inventory = view.getTopInventory();
		} else {
			inventory = view.getBottomInventory();
		}
				
		TradePlayer tradePlayer = initiator.getPlayer() == player ? initiator : partner;
		boolean isPlayerInventory = inventory.getType() == InventoryType.PLAYER;
		
		event.setCancelled(true);
		
		ClickType clickType = event.getClick();
		int slot = event.getSlot();
		TradeAction action = TradeAction.NOTHING;
		
		int moneyAdding = 0;
		
		if (isPlayerInventory) {
			action = TradeAction.MOVE_ITEM_TO_TRADE_INVENTORY;
		} else {
			if (slot == ADD_50_INDEX) {
				moneyAdding = 50;
				action = TradeAction.ADD_MONEY;
			} else if (slot == ADD_100_INDEX) {
				moneyAdding = 100;
				action = TradeAction.ADD_MONEY;
			} else if (slot == ADD_500_INDEX) {
				moneyAdding = 500;
				action = TradeAction.ADD_MONEY;
			} else if (slot == ADD_EXP_LEVEL_INDEX) {
				action = TradeAction.ADD_EXP;
			} else if (slot == ACCEPT_TRADE_INDEX) {
				action = TradeAction.ACCEPT;
			} else if (slot == DECLINE_TRADE_INDEX) {
				action = TradeAction.DECLINE;
			} else {
				if (slot % 9 < 4 && slot / 9 > 1) {
					action = TradeAction.MOVE_ITEM_TO_PLAYER_INVENTORY;
				}
			}
		}
				
		//Process the calculated data
		switch (action) {
		case ACCEPT:
			tradePlayer.setAccepted(true);
			
			if (initiator.hasAccepted() && partner.hasAccepted()) {
				contractTrade();
			}
			break;
		case DECLINE:
			tradePlayer.setAccepted(false);
			break;
		case ADD_EXP:
			int newExpOffer = tradePlayer.getExpOffer();
			if (clickType == ClickType.LEFT) {
				newExpOffer++;
				
				if (newExpOffer > player.getLevel()) {
					// Too few exp
					player.sendMessage(ChatColor.RED + "You do not have enough xp to do this!");
					return;
				}
				
				player.playSound(player.getLocation(), Sound.CLICK, 1.0F, ADD_PITCH);
			} else if (clickType == ClickType.RIGHT) {
				newExpOffer--;
				
				if (newExpOffer < 0) {
					player.sendMessage(ChatColor.RED + "You do not provide an exp offer!");
					return;
				}
				
				player.playSound(player.getLocation(), Sound.CLICK, 1.0F, REMOVE_PITCH);
			}
			
			tradePlayer.setExpOffer(newExpOffer);
			declineAll();
			break;
		case ADD_MONEY:
			int newMoneyOffer = tradePlayer.getMoneyOffer();
			if (clickType == ClickType.LEFT) {
				newMoneyOffer += moneyAdding;
				
				if (newMoneyOffer > econ.getBalance(player)) {
					// Not enough money
					player.sendMessage(ChatColor.RED + "You do not have sufficient funds to do this!");
					return;
				}
				
				player.playSound(player.getLocation(), Sound.CLICK, 1.0F, ADD_PITCH);
			} else if (clickType == ClickType.RIGHT) {
				newMoneyOffer -= moneyAdding;
				
				if (newMoneyOffer < 0) {
					player.sendMessage(ChatColor.RED + "Your money offer cannot be negative!");
					return;
				}
				
				player.playSound(player.getLocation(), Sound.CLICK, 1.0F, REMOVE_PITCH);
			}
			
			tradePlayer.setMoneyOffer(newMoneyOffer);
			declineAll();
			break;
		default:
			break;
		}
		
		if (action == TradeAction.MOVE_ITEM_TO_PLAYER_INVENTORY || action == TradeAction.MOVE_ITEM_TO_TRADE_INVENTORY) {
			ItemStack stack = event.getCurrentItem();
			ItemStack otherStack = stack.clone();
			
			int newStackAmount;
			
			if (clickType == ClickType.LEFT) {
				otherStack.setAmount(stack.getAmount());
				newStackAmount = 0;
			} else if (clickType == ClickType.RIGHT) {
				otherStack.setAmount(1);
				
				if (stack.getAmount() == 1) {
					newStackAmount = 0;
				} else {
					newStackAmount = stack.getAmount() - 1;
				}
			} else {
				return;
			}
			
			if (action == TradeAction.MOVE_ITEM_TO_TRADE_INVENTORY) {
				int untransferred = addToTradeInventory(tradePlayer, otherStack);
				if (untransferred != 0) {
					stack.setAmount(newStackAmount + untransferred);
				} else {
					stack.setAmount(newStackAmount);
				}
			} else {
				Inventory inv = player.getInventory();
				Map<Integer, ItemStack> untransferred = inv.addItem(otherStack);
				if (!untransferred.isEmpty()) {
					stack.setAmount(newStackAmount + untransferred.get(0).getAmount());
				} else {
					stack.setAmount(newStackAmount);
				}
			}
			
			if (stack.getAmount() == 0) {
				stack = null;
			}
			
			event.setCurrentItem(stack);
			
			declineAll();			
			reflectChanges(tradePlayer);
		}
		
		updateInventoryStatus();
	}
	
	private void declineAll() {
		initiator.setAccepted(false);
		partner.setAccepted(false);
	}
	
	private void contractTrade() {
		Player initiatorPlayer = initiator.getPlayer();
		Player partnerPlayer = partner.getPlayer();
		
		if (initiator.getMoneyOffer() > 0) {
			econ.withdrawPlayer(initiatorPlayer, initiator.getMoneyOffer());
			econ.depositPlayer(partnerPlayer, initiator.getMoneyOffer());
		}
		
		if (partner.getMoneyOffer() > 0) {
			econ.withdrawPlayer(partnerPlayer, partner.getMoneyOffer());
			econ.depositPlayer(initiatorPlayer, partner.getMoneyOffer());
		}
		
		if (initiator.getExpOffer() > 0) {
			partnerPlayer.setLevel(partnerPlayer.getLevel() + initiator.getExpOffer());
			initiatorPlayer.setLevel(initiatorPlayer.getLevel() - initiator.getExpOffer());
		}
		
		if (partner.getExpOffer() > 0) {
			initiatorPlayer.setLevel(initiatorPlayer.getLevel() + partner.getExpOffer());
			partnerPlayer.setLevel(partnerPlayer.getLevel() - partner.getExpOffer());
		}
		
		setState(TradeState.CONTRACTED);
		
		initiatorPlayer.closeInventory();
		partnerPlayer.closeInventory();
		
		transferTradeItems(initiator, partner);
		transferTradeItems(partner, initiator);
		
		initiatorPlayer.updateInventory();
		partnerPlayer.updateInventory();
		
		initiatorPlayer.playSound(initiatorPlayer.getLocation(), Sound.LEVEL_UP, 1.0F, 1.0F);
		partnerPlayer.playSound(partnerPlayer.getLocation(), Sound.LEVEL_UP, 1.0F, 1.0F);
		
		initiatorPlayer.sendMessage(messageConfig.getMessage(Messages.CONFIRMED, partner.getName()));
		partnerPlayer.sendMessage(messageConfig.getMessage(Messages.CONFIRMED, initiator.getName()));
	}
	
	private void transferTradeItems(TradePlayer from, TradePlayer to) {
		Inventory inv = from.getInventory();
		
		boolean hasUntransferredItems = false;
		for (int y = 2; y < 6; y++) {
			for (int x = 0; x < 4; x++) {
				int slot = y * 9 + x;
				
				ItemStack current = inv.getItem(slot);
				if (current == null) {
					continue;
				}
				
				Map<Integer, ItemStack> untransferred = to.getPlayer().getInventory().addItem(current);
				if (!untransferred.isEmpty()) {
					hasUntransferredItems = true;
					for (ItemStack stack : untransferred.values()) {
						to.getPlayer().getWorld().dropItem(to.getPlayer().getLocation(), stack);
					}
				}
			}
		}
		
		if (hasUntransferredItems) {
			to.getPlayer().sendMessage(ChatColor.RED + "Your inventory is full! The rest of the items were dropped on the ground");
		}
	}
	
	private void reflectChanges(TradePlayer player) {
		Inventory inv = player.getInventory();
		Inventory otherInv = player == initiator ? partner.getInventory() : initiator.getInventory();
		
		for (int y = 2; y < 6; y++) {
			for (int x = 0; x < 4; x++) {
				int slot = y * 9 + x;
				int reflectedSlot = y * 9 + (8 - x);
				
				ItemStack current = inv.getItem(slot);
				otherInv.setItem(reflectedSlot, current);
			}
		}
	}
	
	private int addToTradeInventory(TradePlayer player, ItemStack stack) {
		Inventory inv = player.getInventory();
		
		for (int y = 2; y < 6; y++) {
			for (int x = 0; x < 4; x++) {
				int slot = y * 9 + x;
				
				ItemStack current = inv.getItem(slot);
				int amount;
				if (current != null && !current.isSimilar(stack)) {
					continue;
				} else if (current == null) {
					current = stack.clone();
					amount = 0;
				} else {
					amount = current.getAmount();
				}
				
				int newAmount = amount + stack.getAmount();
				if (newAmount > current.getMaxStackSize()) {
					newAmount = current.getMaxStackSize();
				}
				
				stack.setAmount(stack.getAmount() - (newAmount - amount));
				current.setAmount(newAmount);
				
				inv.setItem(slot, current);
				
				if (stack.getAmount() == 0) {
					return 0;
				}
			}
		}
		
		return stack.getAmount();
	}
	
	private void updateInventoryStatus() {
		ItemStack statusStack = null;
		String loreLine;
		boolean isConfirmed;
		
		if (initiator.hasAccepted() || partner.hasAccepted()) {
			statusStack = CONFIRMED_STATUS_MATERIAL_DATA.toItemStack(1);
			loreLine = "One player has accepted the trade";
			isConfirmed = true;
		} else {
			statusStack = UNCONFIRMED_STATUS_MATERIAL_DATA.toItemStack(1);
			loreLine = "Waiting for other player to accept";
			isConfirmed = false;
		}
		
		ItemMeta meta = statusStack.getItemMeta();
		meta.setDisplayName((isConfirmed ? ChatColor.GREEN : ChatColor.RED) + "Trade Status");
		meta.setLore(Lists.newArrayList(ChatColor.WHITE + loreLine));
		statusStack.setItemMeta(meta);
		
		ItemStack expInfo = EXPERIENCE_MATERIAL_DATA.toItemStack(1);
		ItemMeta expInfoMeta = expInfo.getItemMeta();
		expInfoMeta.setDisplayName(ChatColor.DARK_GREEN + "Experience Trades");
		List<String> expInfoLore = Lists.newArrayList();
		expInfoLore.add(getOfferLoreString(getGenitive(initiator.getName()), String.valueOf(initiator.getExpOffer())));
		expInfoLore.add(getOfferLoreString(getGenitive(partner.getName()), String.valueOf(partner.getExpOffer())));
		expInfoMeta.setLore(expInfoLore);
		expInfo.setItemMeta(expInfoMeta);
		
		ItemStack moneyInfo = MONEY_MATERIAL_DATA.toItemStack(1);
		ItemMeta moneyInfoMeta = moneyInfo.getItemMeta();
		moneyInfoMeta.setDisplayName(ChatColor.GOLD + "Money Trades");
		List<String> moneyInfoLore = Lists.newArrayList();
		moneyInfoLore.add(getOfferLoreString(getGenitive(initiator.getName()), econ.format(initiator.getMoneyOffer())));
		moneyInfoLore.add(getOfferLoreString(getGenitive(partner.getName()), econ.format(partner.getMoneyOffer())));
		moneyInfoMeta.setLore(moneyInfoLore);
		moneyInfo.setItemMeta(moneyInfoMeta);
		
		initiator.getInventory().setItem(CONFIRMATION_INFO_INDEX, statusStack);
		partner.getInventory().setItem(CONFIRMATION_INFO_INDEX, statusStack);
		
		initiator.getInventory().setItem(EXP_INFO_INDEX, expInfo);
		partner.getInventory().setItem(EXP_INFO_INDEX, expInfo);
		
		initiator.getInventory().setItem(MONEY_INFO_INDEX, moneyInfo);
		partner.getInventory().setItem(MONEY_INFO_INDEX, moneyInfo);
	}

	private enum TradeAction {
		
		ADD_MONEY,
		ADD_EXP,
		ACCEPT,
		DECLINE,
		MOVE_ITEM_TO_PLAYER_INVENTORY,
		MOVE_ITEM_TO_TRADE_INVENTORY,
		NOTHING;
		
	}
	
	public interface StateChangedListener {
		
		public void onStateChanged(Trade trade, TradeState newState);
		
	}

}
