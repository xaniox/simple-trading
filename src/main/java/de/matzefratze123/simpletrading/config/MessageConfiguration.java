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
package de.matzefratze123.simpletrading.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.configuration.Configuration;

public class MessageConfiguration {
	
	private static final String PLAYERNAME_PLACEHOLDER = "@p";
	
	private Map<String, String> messages;
	
	public MessageConfiguration(Configuration config) {
		messages = new LinkedHashMap<String, String>();
		
		loadByConfiguration(config);
	}
	
	public void loadByConfiguration(Configuration config) {
		Set<String> messageKeys = config.getKeys(false);
		for (String key : messageKeys) {
			String message = config.getString(key);
			
			messages.put(key, message);
		}
	}
	
	public String getMessage(String key, String nameReplacement) {
		String message = messages.get(key);
		if (message != null) {
			message = message.replace(PLAYERNAME_PLACEHOLDER, nameReplacement);
			message = ChatColor.translateAlternateColorCodes('&', message);
		}
		
		return message;
	}

}
