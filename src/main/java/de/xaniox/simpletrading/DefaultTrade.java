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
import de.xaniox.simpletrading.config.ItemControlManager;
import de.xaniox.simpletrading.config.TradeConfiguration;
import de.xaniox.simpletrading.i18n.I18N;
import de.xaniox.simpletrading.i18n.I18NManager;
import de.xaniox.simpletrading.i18n.Messages;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

@SuppressWarnings("deprecation")
public class DefaultTrade implements Trade {

	private static final int INVENTORY_SIZE = 6 * 9;
	private static final int[] SEPERATOR_INDEXES = { 0, 1, 7, 8, 9, 13, 17, 22, 31, 40, 49 };
    private static final String[] LEVEL_UP_SEARCH = {"LEVEL_UP", "PLAYER_LEVELUP"};
    private static final String[] CLICK_SEARCH = {"UI_BUTTON_CLICK", "CLICK"};
	
	private static final MaterialData UNCONFIRMED_STATUS_MATERIAL_DATA = new MaterialData(Material.STAINED_GLASS, (byte) 14);
	private static final MaterialData CONFIRMED_STATUS_MATERIAL_DATA = new MaterialData(Material.STAINED_GLASS, (byte) 5);
	
	private static final int EXP_INFO_INDEX = 6;
	private static final int ACCEPT_TRADE_INDEX = 3;
	private static final int CONFIRMATION_INFO_INDEX = 4;
	private static final int DECLINE_TRADE_INDEX = 5;
	private static final int MONEY_INFO_INDEX = 2;
	private static final int ADD_MONEY_1_INDEX = 10;
	private static final int ADD_MONEY_2_INDEX = 11;
	private static final int ADD_MONEY_3_INDEX = 12;
	private static final int ADD_EXP_1_INDEX = 14;
    private static final int ADD_EXP_2_INDEX = 15;
    private static final int ADD_EXP_3_INDEX = 16;
	
	private static final float ADD_PITCH = 1.5F;
	private static final float REMOVE_PITCH = 1.0F;
    private static final DecimalFormat LEVEL_FORMAT = new DecimalFormat("0");

    static {
        LEVEL_FORMAT.setPositivePrefix("+");
    }
	
	private final TradePlayer initiator;
	private final TradePlayer partner;
	
	private final SimpleTrading plugin;
	private final TradeConfiguration config;
	private final I18N i18n = I18NManager.getGlobal();
	private final Economy econ;
	private final ItemControlManager controlManager;
	
	private StateChangedListener listener;
	private TradeState state;
	
	public DefaultTrade(Player initiator, Player partner, TradeConfiguration config, Economy econ,
			ItemControlManager controlManager, SimpleTrading plugin) {
		this.plugin = plugin;
		this.initiator = new TradePlayer(initiator);
		this.partner = new TradePlayer(partner);
		this.config = config;
		this.econ = econ;
		this.controlManager = controlManager;
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
		
		ItemStack expInfoItemStack;
		
		if (config.usesXpTrading()) {
			expInfoItemStack = config.getXpStatusBlockData().newItemStack();
			ItemMeta expInfoMeta = expInfoItemStack.getItemMeta();
			expInfoMeta.setDisplayName(i18n.getString(Messages.Inventory.EXP_INFO_TITLE));
			List<String> expLore = Lists.newArrayList();
			expLore.add(i18n.getVarString(Messages.Inventory.OFFER_LORE)
                    .setVariable("player", initiator.getName())
                    .setVariable("offer", "0")
                    .toString());
			expLore.add(i18n.getVarString(Messages.Inventory.OFFER_LORE)
                    .setVariable("player", partner.getName())
                    .setVariable("offer", "0")
                    .toString());
			expInfoMeta.setLore(expLore);
			expInfoItemStack.setItemMeta(expInfoMeta);
		} else {
			expInfoItemStack = seperator;
		}
		
		ItemStack acceptItemStack = config.getAcceptBlockData().newItemStack(1);
		ItemMeta acceptMeta = acceptItemStack.getItemMeta();
		acceptMeta.setDisplayName(i18n.getString(Messages.Inventory.ACCEPT_TRADE_TITLE));
		acceptItemStack.setItemMeta(acceptMeta);
		
		ItemStack unconfirmedStatusItemStack = UNCONFIRMED_STATUS_MATERIAL_DATA.toItemStack(1);
		ItemMeta unconfirmedStatusMeta = unconfirmedStatusItemStack.getItemMeta();
		unconfirmedStatusMeta.setDisplayName(i18n.getString(Messages.Inventory.TRADE_STATUS_TITLE));
		unconfirmedStatusMeta.setLore(Lists.newArrayList(i18n.getString(Messages.Inventory.WAITING_FOR_OTHER_PLAYER_LORE)));
		unconfirmedStatusItemStack.setItemMeta(unconfirmedStatusMeta);
		
		ItemStack declineItemStack = config.getDeclineBlockData().newItemStack(1);
		ItemMeta declineMeta = declineItemStack.getItemMeta();
		declineMeta.setDisplayName(i18n.getString(Messages.Inventory.DECLINE_TRADE_TITLE));
		declineItemStack.setItemMeta(declineMeta);
		
		ItemStack moneyInfoItemStack = seperator;
		ItemStack addMoney1ItemStack = seperator;
		ItemStack addMoney2ItemStack = seperator;
		ItemStack addMoney3ItemStack = seperator;

        int money1Value = config.getMoneyValue1();
        int money2Value = config.getMoneyValue2();
        int money3Value = config.getMoneyValue3();
		
		boolean usesVault = plugin.usesVault();
        boolean usesMoneyTrading = config.usesMoneyTrading();

		if (usesVault && usesMoneyTrading) {
			moneyInfoItemStack = config.getMoneyStatusBlockData().newItemStack();
			ItemMeta moneyMeta = moneyInfoItemStack.getItemMeta();
			moneyMeta.setDisplayName(i18n.getString(Messages.Inventory.MONEY_INFO_TITLE));
			List<String> moneyLore = Lists.newArrayList();
			moneyLore.add(i18n.getVarString(Messages.Inventory.OFFER_LORE)
                    .setVariable("player", initiator.getName())
                    .setVariable("offer", econ.format(0))
                    .toString());
			moneyLore.add(i18n.getVarString(Messages.Inventory.OFFER_LORE)
                    .setVariable("player", partner.getName())
                    .setVariable("offer", econ.format(0))
                    .toString());
			moneyMeta.setLore(moneyLore);
			moneyInfoItemStack.setItemMeta(moneyMeta);
			
			List<String> addMoneyLore = Lists.newArrayList(i18n
                .getString(Messages.Inventory.ADD_MONEY_LORE)
                .split("\n"));

			addMoney1ItemStack = config.getMoneyAddRemoveBlockData().newItemStack();
			ItemMeta metaMoney1 = addMoney1ItemStack.getItemMeta();
			metaMoney1.setDisplayName(i18n.getVarString(Messages.Inventory.ADD_REMOVE_MONEY_LORE)
                    .setVariable("money", econ.format(money1Value))
                    .toString());
			metaMoney1.setLore(addMoneyLore);
			addMoney1ItemStack.setItemMeta(metaMoney1);
			
			addMoney2ItemStack = config.getMoneyAddRemoveBlockData().newItemStack();
			ItemMeta metaMoney2 = addMoney2ItemStack.getItemMeta();
			metaMoney2.setDisplayName(i18n.getVarString(Messages.Inventory.ADD_REMOVE_MONEY_LORE)
                    .setVariable("money", econ.format(money2Value))
                    .toString());
			metaMoney2.setLore(addMoneyLore);
			addMoney2ItemStack.setItemMeta(metaMoney2);
			
			addMoney3ItemStack = config.getMoneyAddRemoveBlockData().newItemStack();
			ItemMeta metaMoney3 = addMoney3ItemStack.getItemMeta();
			metaMoney3.setDisplayName(i18n.getVarString(Messages.Inventory.ADD_REMOVE_MONEY_LORE)
                    .setVariable("money", econ.format(money3Value))
                    .toString());
			metaMoney3.setLore(addMoneyLore);
			addMoney3ItemStack.setItemMeta(metaMoney3);
		}
		
		ItemStack addExp1ItemStack;
        ItemStack addExp2ItemStack;
        ItemStack addExp3ItemStack;

        int expValue1 = config.getExpValue1();
        int expValue2 = config.getExpValue2();
        int expValue3 = config.getExpValue3();

		if (config.usesXpTrading()) {
			addExp1ItemStack = config.getXpAddRemoveBlockData().newItemStack();
            addExp2ItemStack = config.getXpAddRemoveBlockData().newItemStack();
            addExp3ItemStack = config.getXpAddRemoveBlockData().newItemStack();

			ItemMeta addExpLevelMeta = addExp1ItemStack.getItemMeta();
			addExpLevelMeta.setDisplayName(i18n.getVarString(Messages.Inventory.ADD_EXP_TITLE)
                .setVariable("exp", String.valueOf(expValue1))
                .toString());
			List<String> addExpLevelLore = Lists.newArrayList(i18n.getVarString(Messages.Inventory.ADD_EXP_LORE)
                    .setVariable("exp", String.valueOf(expValue1))
                    .toString()
                    .split("\n"));
			addExpLevelMeta.setLore(addExpLevelLore);
			addExp1ItemStack.setItemMeta(addExpLevelMeta);

            addExpLevelMeta = addExp2ItemStack.getItemMeta();
            addExpLevelMeta.setDisplayName(i18n.getVarString(Messages.Inventory.ADD_EXP_TITLE)
                    .setVariable("exp", String.valueOf(expValue2))
                    .toString());
            addExpLevelLore = Lists.newArrayList(i18n.getVarString(Messages.Inventory.ADD_EXP_LORE)
                    .setVariable("exp", String.valueOf(expValue2))
                    .toString()
                    .split("\n"));
            addExpLevelMeta.setLore(addExpLevelLore);
            addExp2ItemStack.setItemMeta(addExpLevelMeta);

            addExpLevelMeta = addExp3ItemStack.getItemMeta();
            addExpLevelMeta.setDisplayName(i18n.getVarString(Messages.Inventory.ADD_EXP_TITLE)
                    .setVariable("exp", String.valueOf(expValue3))
                    .toString());
            addExpLevelLore = Lists.newArrayList(i18n.getVarString(Messages.Inventory.ADD_EXP_LORE)
                    .setVariable("exp", String.valueOf(expValue3))
                    .toString()
                    .split("\n"));
            addExpLevelMeta.setLore(addExpLevelLore);
            addExp3ItemStack.setItemMeta(addExpLevelMeta);
		} else {
			addExp1ItemStack = seperator;
            addExp2ItemStack = seperator;
            addExp3ItemStack = seperator;
		}
		
		inv.setItem(EXP_INFO_INDEX, expInfoItemStack);
		inv.setItem(ACCEPT_TRADE_INDEX, acceptItemStack);
		inv.setItem(CONFIRMATION_INFO_INDEX, unconfirmedStatusItemStack);
		inv.setItem(DECLINE_TRADE_INDEX, declineItemStack);
		inv.setItem(MONEY_INFO_INDEX, moneyInfoItemStack);
		inv.setItem(ADD_MONEY_1_INDEX, addMoney1ItemStack);
		inv.setItem(ADD_MONEY_2_INDEX, addMoney2ItemStack);
		inv.setItem(ADD_MONEY_3_INDEX, addMoney3ItemStack);
		inv.setItem(ADD_EXP_1_INDEX, addExp1ItemStack);
        inv.setItem(ADD_EXP_2_INDEX, addExp2ItemStack);
        inv.setItem(ADD_EXP_3_INDEX, addExp3ItemStack);
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

			if (!plugin.isDisabling()) {
				Bukkit.getScheduler().runTask(plugin, new Runnable() {

					@Override
					public void run() {
						initiator.getPlayer().updateInventory();
						partner.getPlayer().updateInventory();
					}
				});
			}
		}
		
		
		switch (cause) {
		case DEATH:
			other.getPlayer().sendMessage(i18n.getVarString(Messages.General.CANCEL_TRADE_DEATH)
                .setVariable("player", who.getName())
                .toString());
			break;
		case INVENTORY_CLOSE:
		case DECLINE:
			other.getPlayer().sendMessage(i18n.getVarString(Messages.General.CANCEL_TRADE_CANCEL)
                .setVariable("player", who.getName())
                .toString());
			who.getPlayer().sendMessage(i18n.getVarString(Messages.General.CANCEL_TRADE_DECLINE)
                .setVariable("player", other.getName())
                .toString());
			break;
		case LEFT_WORLD:
			other.getPlayer().sendMessage(i18n.getVarString(Messages.General.CANCEL_TRADE_LEFT_WORLD)
                .setVariable("player", who.getName())
                .toString());
			break;
		case MOVE:
			other.getPlayer().sendMessage(i18n.getVarString(Messages.General.CANCEL_TRADE_MOVED_AWAY)
                .setVariable("player", who.getName())
                .toString());
			break;
		case QUIT:
			other.getPlayer().sendMessage(i18n.getVarString(Messages.General.CANCEL_TRADE_PLAYER_LEFT)
                .setVariable("player", who.getName())
                .toString());
			break;
		case TIMEOUT:
			who.getPlayer().sendMessage(i18n.getVarString(Messages.General.CANCEL_TRADE_TIMEOUT)
                .setVariable("player", other.getName())
                .toString());
			other.getPlayer().sendMessage(i18n.getVarString(Messages.General.CANCEL_TRADE_TIMEOUT)
                .setVariable("player", who.getName())
                .toString());
			break;
		case SERVER_SHUTDOWN:
			who.getPlayer().sendMessage(i18n.getString(Messages.General.CANCEL_SERVER_SHUTDOWN));
			other.getPlayer().sendMessage(i18n.getString(Messages.General.CANCEL_SERVER_SHUTDOWN));
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
		Inventory inventory;
		int rawSlot = event.getRawSlot();
		
		if (view.getTopInventory() != null && rawSlot < view.getTopInventory().getSize()) {
			inventory = view.getTopInventory();
		} else {
			inventory = view.getBottomInventory();
		}
				
		TradePlayer tradePlayer = initiator.getPlayer() == player ? initiator : partner.getPlayer() == player ? partner : null;
        if (tradePlayer == null) {
            //This event does not belong to this trade
            return;
        }

		boolean isPlayerInventory = inventory.getType() == InventoryType.PLAYER;
		boolean usesVault = plugin.usesVault();
		
		event.setCancelled(true);
		
		ClickType clickType = event.getClick();
		int slot = event.getSlot();
		TradeAction action = TradeAction.NOTHING;
		
		int moneyAdding = 0;
        int expAdding = 0;
		
		if (isPlayerInventory) {
			action = TradeAction.MOVE_ITEM_TO_TRADE_INVENTORY;
		} else {
            int moneyValue1 = config.getMoneyValue1();
            int moneyValue2 = config.getMoneyValue2();
            int moneyValue3 = config.getMoneyValue3();

            int expValue1 = config.getExpValue1();
            int expValue2 = config.getExpValue2();
            int expValue3 = config.getExpValue3();

			if (slot == ADD_MONEY_1_INDEX) {
				moneyAdding = moneyValue1;
				action = TradeAction.ADD_MONEY;
			} else if (slot == ADD_MONEY_2_INDEX) {
				moneyAdding = moneyValue2;
				action = TradeAction.ADD_MONEY;
			} else if (slot == ADD_MONEY_3_INDEX) {
                moneyAdding = moneyValue3;
                action = TradeAction.ADD_MONEY;
            } else if (slot == ADD_EXP_1_INDEX && config.usesXpTrading()) {
                expAdding = expValue1;
                action = TradeAction.ADD_EXP;
            } else if (slot == ADD_EXP_2_INDEX && config.usesXpTrading()) {
                expAdding = expValue2;
                action = TradeAction.ADD_EXP;
            } else if (slot == ADD_EXP_3_INDEX && config.usesXpTrading()) {
                expAdding = expValue3;
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

        Sound clickSound = getSoundEnumType(CLICK_SEARCH);

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

			if (config.isAbortOnDecline()) {
				plugin.getFactory().stopTrade(this, StopCause.DECLINE, player);
			}
			break;
		case ADD_EXP:
			int newExpOffer = tradePlayer.getExpOffer();

			if (clickType == ClickType.LEFT) {
				newExpOffer += expAdding;
				
				if (newExpOffer > SetExpFix.getTotalExperience(player.getPlayer())) {
					// Too few exp
					player.sendMessage(i18n.getString(Messages.General.NOT_ENOUGH_XP));
					return;
				}

                if (clickSound != null) {
                    player.playSound(player.getLocation(), clickSound, 1.0F, ADD_PITCH);
                }
			} else if (clickType == ClickType.RIGHT) {
				newExpOffer -= expAdding;
				
				if (newExpOffer < 0) {
					player.sendMessage(i18n.getString(Messages.General.NO_XP_OFFER));
					return;
				}

                if (clickSound != null) {
                    player.playSound(player.getLocation(), clickSound, 1.0F, REMOVE_PITCH);
                }
			}

			tradePlayer.setExpOffer(newExpOffer);
			declineAll();
			break;
		case ADD_MONEY:
			if (!usesVault || !config.usesMoneyTrading()) {
				return;
			}

			int newMoneyOffer = tradePlayer.getMoneyOffer();
            int maxMoneyAmount = config.getMaxMoneyTrading();

            if (clickType == ClickType.LEFT) {
				newMoneyOffer += moneyAdding;
				
				if (newMoneyOffer > econ.getBalance(player)) {
					// Not enough money
					player.sendMessage(i18n.getString(Messages.General.NOT_ENOUGH_MONEY));
					return;
				}

                if (newMoneyOffer > maxMoneyAmount && maxMoneyAmount != -1) {
                    // Reached the limit
                    player.sendMessage(i18n.getString(Messages.General.MAX_TRADE_AMOUNT_REACHED));
                    return;
                }

                if (clickSound != null) {
                    player.playSound(player.getLocation(), clickSound, 1.0F, ADD_PITCH);
                }
			} else if (clickType == ClickType.RIGHT) {
				newMoneyOffer -= moneyAdding;
				
				if (newMoneyOffer < 0) {
					player.sendMessage(i18n.getString(Messages.General.NO_NEGATIVE_MONEY_OFFER));
					return;
				}

                if (clickSound != null) {
                    player.playSound(player.getLocation(), clickSound, 1.0F, REMOVE_PITCH);
                }
			}
			
			tradePlayer.setMoneyOffer(newMoneyOffer);
			declineAll();
			break;
		default:
			break;
		}
		
		if (action == TradeAction.MOVE_ITEM_TO_PLAYER_INVENTORY || action == TradeAction.MOVE_ITEM_TO_TRADE_INVENTORY) {
			ItemStack stack = event.getCurrentItem();
			
			if (!controlManager.isAllowed(stack)) {
				// This item is not tradeable
				player.sendMessage(i18n.getString(Messages.General.CANNOT_TRADE_ITEM));
				return;
			}

			if (stack == null || stack.getType() == Material.AIR) {
				return;
			}

			ItemStack stackClone = stack.clone();
			
			int newStackAmount;
			
			if (clickType == ClickType.LEFT) {
				stackClone.setAmount(stack.getAmount());
				newStackAmount = 0;
			} else if (clickType == ClickType.RIGHT) {
				stackClone.setAmount(1);
				
				if (stack.getAmount() == 1) {
					newStackAmount = 0;
				} else {
					newStackAmount = stack.getAmount() - 1;
				}
			} else {
				return;
			}
			
			if (action == TradeAction.MOVE_ITEM_TO_TRADE_INVENTORY) {
				int untransferred = addToTradeInventory(tradePlayer, stackClone);
				if (untransferred != 0) {
					stack.setAmount(newStackAmount + untransferred);
				} else {
					stack.setAmount(newStackAmount);
				}
			} else {
				Inventory inv = player.getInventory();
				Map<Integer, ItemStack> untransferred = inv.addItem(stackClone);
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

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Player player = (Player) event.getWhoClicked();

        TradePlayer tradePlayer = initiator.getPlayer() == player ? initiator : partner.getPlayer() == player ? partner : null;
        if (tradePlayer == null) {
            //This event does not belong to this trade
            return;
        }

        event.setCancelled(true);
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
            SetExpFix.setTotalExperience(partnerPlayer, SetExpFix.getTotalExperience(partnerPlayer) + initiator.getExpOffer());
            SetExpFix.setTotalExperience(initiatorPlayer, SetExpFix.getTotalExperience(initiatorPlayer) - initiator.getExpOffer());
		}
		
		if (partner.getExpOffer() > 0) {
            SetExpFix.setTotalExperience(partnerPlayer, SetExpFix.getTotalExperience(partnerPlayer) - partner.getExpOffer());
            SetExpFix.setTotalExperience(initiatorPlayer, SetExpFix.getTotalExperience(initiatorPlayer) + partner.getExpOffer());
		}
		
		setState(TradeState.CONTRACTED);
		
		initiatorPlayer.closeInventory();
		partnerPlayer.closeInventory();
		
		transferTradeItems(initiator, partner);
		transferTradeItems(partner, initiator);
		
		initiatorPlayer.updateInventory();
		partnerPlayer.updateInventory();

        Sound levelUpSound = getSoundEnumType(LEVEL_UP_SEARCH);
        if (levelUpSound != null) {
            initiatorPlayer.playSound(initiatorPlayer.getLocation(), levelUpSound, 1.0F, 1.0F);
            partnerPlayer.playSound(partnerPlayer.getLocation(), levelUpSound, 1.0F, 1.0F);
        }
		
		initiatorPlayer.sendMessage(i18n.getVarString(Messages.General.TRADE_CONFIRMED)
                .setVariable("player", partnerPlayer.getName())
                .toString());
		partnerPlayer.sendMessage(i18n.getVarString(Messages.General.TRADE_CONFIRMED)
                .setVariable("player", initiator.getName())
                .toString());
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
			to.getPlayer().sendMessage(i18n.getString(Messages.General.INVENTORY_FULL_ITEMS_DROPPED));
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
		int maxStackSize = Math.max(stack.getAmount(), stack.getMaxStackSize());

		for (int y = 2; y < 6; y++) {
			for (int x = 0; x < 4; x++) {
				int slot = y * 9 + x;
				
				ItemStack current = inv.getItem(slot);
				int amount;
				if (current != null && !current.isSimilar(stack)) {
					//Totally not the itemstack we're searching for
					continue;
				} else if (current == null) {
					//There is no itemstack in this slot
					current = stack.clone();
					amount = 0;
				} else if (stack.getAmount() <= stack.getMaxStackSize()) {
					//This itemstack is similar to our requested
					//Only add up on this itemstack
					amount = current.getAmount();

					if (amount >= stack.getMaxStackSize()) {
						//We can't add anything on top of this itemstack
						continue;
					}
				} else {
					continue;
				}
				
				int newAmount = amount + stack.getAmount();
				if (newAmount > maxStackSize) {
					newAmount = maxStackSize;
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
		ItemStack statusStack;
		String loreLine;
		boolean isConfirmed;
		boolean usesVault = plugin.usesVault();

		if (initiator.hasAccepted() || partner.hasAccepted()) {
			statusStack = CONFIRMED_STATUS_MATERIAL_DATA.toItemStack(1);
			loreLine = i18n.getString(Messages.Inventory.ONE_PLAYER_ACCEPTED);
			isConfirmed = true;
		} else {
			statusStack = UNCONFIRMED_STATUS_MATERIAL_DATA.toItemStack(1);
			loreLine = i18n.getString(Messages.Inventory.WAITING_FOR_OTHER_PLAYER_LORE);
			isConfirmed = false;
		}

		ItemMeta meta = statusStack.getItemMeta();
		meta.setDisplayName(i18n.getVarString(Messages.Inventory.TRADE_STATUS_TITLE)
                .setVariable("color", String.valueOf(isConfirmed ? ChatColor.GREEN : ChatColor.RED))
                .toString());
		meta.setLore(Lists.newArrayList(ChatColor.WHITE + loreLine));
		statusStack.setItemMeta(meta);

        int initiatorDiff = getLevelDiff(initiator.getPlayer().getLevel(), SetExpFix.getTotalExperience(initiator.getPlayer()),
                partner.getExpOffer() - initiator.getExpOffer());
        int partnerDiff = getLevelDiff(partner.getPlayer().getLevel(), SetExpFix.getTotalExperience(partner.getPlayer()),
                initiator.getExpOffer() - partner.getExpOffer());

		ItemStack expInfoInitiator = config.getXpStatusBlockData().newItemStack();
        ItemStack expInfoPartner = config.getXpStatusBlockData().newItemStack();

		ItemMeta expInfoMetaInitiator = expInfoInitiator.getItemMeta();
        ItemMeta expInfoMetaPartner = expInfoPartner.getItemMeta();

        expInfoMetaInitiator.setDisplayName(i18n.getString(Messages.Inventory.EXP_INFO_TITLE));
        expInfoMetaPartner.setDisplayName(i18n.getString(Messages.Inventory.EXP_INFO_TITLE));

		List<String> expInfoLore = Lists.newArrayList();
		expInfoLore.add(i18n.getVarString(Messages.Inventory.OFFER_LORE)
                .setVariable("player", initiator.getName())
                .setVariable("offer", String.valueOf(initiator.getExpOffer()) + " XP")
                .toString());
		expInfoLore.add(i18n.getVarString(Messages.Inventory.OFFER_LORE)
                .setVariable("player", partner.getName())
                .setVariable("offer", String.valueOf(partner.getExpOffer()) + " XP")
                .toString());
        expInfoLore.add("");

        List<String> expInfoLoreInitiator = Lists.newArrayList(expInfoLore);
        List<String> expInfoLorePartner = Lists.newArrayList(expInfoLore);

        expInfoLoreInitiator.add(i18n.getVarString(Messages.Inventory.LEVEL_INFO)
            .setVariable("level-diff", LEVEL_FORMAT.format(initiatorDiff))
            .toString());
        expInfoLorePartner.add(i18n.getVarString(Messages.Inventory.LEVEL_INFO)
            .setVariable("level-diff", LEVEL_FORMAT.format(partnerDiff))
            .toString());

		expInfoMetaInitiator.setLore(expInfoLoreInitiator);
        expInfoMetaPartner.setLore(expInfoLorePartner);

		expInfoInitiator.setItemMeta(expInfoMetaInitiator);
        expInfoPartner.setItemMeta(expInfoMetaPartner);

		if (usesVault && config.usesMoneyTrading()) {
			ItemStack moneyInfo = config.getMoneyStatusBlockData().newItemStack();
			ItemMeta moneyInfoMeta = moneyInfo.getItemMeta();
			moneyInfoMeta.setDisplayName(i18n.getString(Messages.Inventory.MONEY_INFO_TITLE));
			List<String> moneyInfoLore = Lists.newArrayList();
			moneyInfoLore.add(i18n.getVarString(Messages.Inventory.OFFER_LORE)
                    .setVariable("player", initiator.getName())
                    .setVariable("offer", econ.format(initiator.getMoneyOffer()))
                    .toString());
			moneyInfoLore.add(i18n.getVarString(Messages.Inventory.OFFER_LORE)
                    .setVariable("player", partner.getName())
                    .setVariable("offer", econ.format(partner.getMoneyOffer()))
                    .toString());
			moneyInfoMeta.setLore(moneyInfoLore);
			moneyInfo.setItemMeta(moneyInfoMeta);

			initiator.getInventory().setItem(MONEY_INFO_INDEX, moneyInfo);
			partner.getInventory().setItem(MONEY_INFO_INDEX, moneyInfo);
		}

		initiator.getInventory().setItem(CONFIRMATION_INFO_INDEX, statusStack);
		partner.getInventory().setItem(CONFIRMATION_INFO_INDEX, statusStack);

		if (config.usesXpTrading()) {
			initiator.getInventory().setItem(EXP_INFO_INDEX, expInfoInitiator);
			partner.getInventory().setItem(EXP_INFO_INDEX, expInfoPartner);
		}
	}

    private int getLevelDiff(int levelBase, int expBase, int expDiff) {
        int totalExp = expBase + expDiff;
        int level;

        if (expDiff == 0) {
            return 0;
        }

        //Some magic number stuff, based on solving the equations found at
        //http://minecraft.gamepedia.com/Experience for the level
        if (totalExp < 394) {
            //Level 0 - 16
            level = (int) (Math.sqrt(totalExp + 9) - 3);
        } else if (totalExp >= 394 && totalExp < 1628) {
            //Level 17 - 31
            level = (int) (0.632456 * (Math.sqrt(totalExp - 195.975) + 12.8072));
        } else {
            //Level 32+
            level = (int) (0.471405 * (Math.sqrt(totalExp - 752.986) + 38.3016));
        }

        return level - levelBase;
    }

    public static Sound getSoundEnumType(String... searchStrings) {
        Sound[] sounds = Sound.values();
        for (Sound sound : sounds) {
            for (String search : searchStrings) {
                if (sound.name().toUpperCase().contains(search)) {
                    return sound;
                }
            }
        }

        return null;
    }

	private enum TradeAction {
		
		ADD_MONEY,
		ADD_EXP,
		ACCEPT,
		DECLINE,
		MOVE_ITEM_TO_PLAYER_INVENTORY,
		MOVE_ITEM_TO_TRADE_INVENTORY,
		NOTHING
		
	}
	
	public interface StateChangedListener {
		
		public void onStateChanged(Trade trade, TradeState newState);
		
	}

}
