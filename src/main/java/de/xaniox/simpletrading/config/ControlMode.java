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
package de.xaniox.simpletrading.config;

public enum ControlMode {

    BLACKLIST,
    WHITELIST;

    public static ControlMode getMode(String str, ControlMode def) {
        str = str.toUpperCase();

        for (ControlMode mode : values()) {
            if (mode.name().equals(str)) {
                return mode;
            }
        }

        if (def != null) {
            return def;
        } else throw new IllegalArgumentException("No enum constant \"" + str + "\" defined");
    }

}
