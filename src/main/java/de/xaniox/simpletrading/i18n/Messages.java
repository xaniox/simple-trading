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
package de.xaniox.simpletrading.i18n;

public interface Messages {

    public interface General {

        public static final String PREFIX = "general.";

        public static final String CANCEL_TRADE_DEATH = PREFIX + "cancel-trade-death";
        public static final String CANCEL_TRADE_CANCEL = PREFIX + "cancel-trade-cancel";
        public static final String CANCEL_TRADE_DECLINE = PREFIX + "cancel-trade-decline";
        public static final String CANCEL_TRADE_LEFT_WORLD = PREFIX + "cancel-trade-left-world";
        public static final String CANCEL_TRADE_MOVED_AWAY = PREFIX + "cancel-trade-moved-away";
        public static final String CANCEL_TRADE_PLAYER_LEFT = PREFIX + "cancel-trade-player-left";
        public static final String CANCEL_TRADE_TIMEOUT = PREFIX + "cancel-trade-timeout";
        public static final String CANCEL_SERVER_SHUTDOWN = PREFIX + "cancel-server-shutdown";
        public static final String NOT_ENOUGH_XP = PREFIX + "not-enough-xp";
        public static final String NO_XP_OFFER = PREFIX + "no-xp-offer";
        public static final String NOT_ENOUGH_MONEY = PREFIX + "not-enough-money";
        public static final String NO_NEGATIVE_MONEY_OFFER = PREFIX + "no-negative-money-offer";
        public static final String CANNOT_TRADE_ITEM = PREFIX + "cannot-trade-item";
        public static final String INVENTORY_FULL_ITEMS_DROPPED = PREFIX + "inventory-full-items-dropped";
        public static final String TRADE_REQUESTED = PREFIX + "trade-requested";
        public static final String TRADE_REQUEST_RECEIVED = PREFIX + "trade-request-received";
        public static final String PARTNER_ALREADY_INVOLVED = PREFIX + "partner-already-involved";
        public static final String NO_PENDING_REQUESTS = PREFIX + "no-pending-requests";
        public static final String REQUEST_TIMED_OUT = PREFIX + "request-timed-out";
        public static final String TRADE_ACCEPTED = PREFIX + "trade-accepted";
        public static final String PARTNER_IN_CREATIVE = PREFIX + "partner-in-creative";
        public static final String TRADE_CONFIRMED = PREFIX + "trade-confirmed";
        public static final String TRADE_REQUEST_DECLINED = PREFIX + "trade-request-declined";
        public static final String TRADE_DECLINED = PREFIX + "trade-declined";

    }

    public interface Inventory {

        public static final String PREFIX = "inventory.";

        public static final String EXP_INFO_TITLE = PREFIX + "exp-info-title";
        public static final String MONEY_INFO_TITLE = PREFIX + "money-info-title";
        public static final String ACCEPT_TRADE_TITLE = PREFIX + "accept-trade-title";
        public static final String DECLINE_TRADE_TITLE = PREFIX + "decline-trade-title";
        public static final String TRADE_STATUS_TITLE = PREFIX + "trade-status-title";
        public static final String ONE_PLAYER_ACCEPTED = PREFIX + "one-player-accepted";
        public static final String WAITING_FOR_OTHER_PLAYER_LORE = PREFIX + "waiting-for-other-player-lore";
        public static final String OFFER_LORE = PREFIX + "offer-lore";
        public static final String ADD_MONEY_LORE = PREFIX + "add-money-lore";
        public static final String ADD_EXP_LORE = PREFIX + "add-exp-lore";
        public static final String ADD_EXP_TITLE = PREFIX + "add-exp-title";
        public static final String ADD_REMOVE_MONEY_LORE = PREFIX + "add-remove-money-lore";

    }

    public interface Command {

        public static final String PREFIX = "command.";

        public static final String ONLY_PLAYER = PREFIX + "only-player";
        public static final String NOT_A_NUMBER = PREFIX + "not-a-number";
        public static final String USAGE = PREFIX + "usage";
        public static final String INSUFFICIENT_PERMISSION = PREFIX + "insufficient-permission";
        public static final String PARTNER_TOO_FAR_AWAY = PREFIX + "partner-too-far-away";
        public static final String CONFIGURATIONS_RELOADED = PREFIX + "configurations-reloaded";
        public static final String NO_LORE_WITH_NUMBER = PREFIX + "no-lore-with-number";
        public static final String NO_ITEM_IN_HAND = PREFIX + "no-item-in-hand";
        public static final String LORE_APPLIED = PREFIX + "lore-applied";
        public static final String PLAYER_NOT_FOUND = PREFIX + "player-not-found";
        public static final String NO_SELF_TRADE = PREFIX + "no-self-trade";

    }
	
}