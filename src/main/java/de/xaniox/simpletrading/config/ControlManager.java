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

public abstract class ControlManager<T> {

    private ControlMode mode;

    public ControlManager(ControlMode mode) {
        this.mode = mode;
    }

    public ControlMode getMode() {
        return mode;
    }

    protected void setMode(ControlMode mode) {
        this.mode = mode;
    }

    public boolean isAllowed(T item) {
        return (mode == ControlMode.BLACKLIST) == isAllowedBlacklist(item);
    }

    protected abstract boolean isAllowedBlacklist(T item);

    public abstract void updateValues(TradeConfiguration config);

}
