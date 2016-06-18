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

import org.bukkit.World;

import java.util.List;

public class WorldControlManager extends ControlManager<World> {

    private List<String> worldList;

    public WorldControlManager(ControlMode mode, List<String> worldList) {
        super(mode);

        this.worldList = worldList;
    }

    public WorldControlManager(TradeConfiguration config) {
        super(config.getWorldControlMode());

        this.worldList = config.getWorldControlList();
    }

    @Override
    public void updateValues(TradeConfiguration config) {
        super.setMode(config.getWorldControlMode());
        this.worldList = config.getWorldControlList();
    }

    @Override
    protected boolean isAllowedBlacklist(World item) {
        for (String worldName : worldList) {
            if (worldName.equals(item.getName())) {
                return false;
            }
        }

        return true;
    }

}
