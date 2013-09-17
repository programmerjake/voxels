/**
 * this file is part of voxels
 * 
 * voxels is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * voxels is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with voxels.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.voxels.mobs;

import org.voxels.World;

abstract class NightMob extends MobType
{
    protected NightMob(final String name)
    {
        super(name);
    }

    @Override
    public boolean canGenerateMob(final int bx,
                                  final int by,
                                  final int bz,
                                  final boolean isMobSpawner)
    {
        if(!super.canGenerateMob(bx, by, bz, isMobSpawner))
            return false;
        float spawnChance = 7 / 15f - World.world.getLighting(bx + 0.5f,
                                                              by + 0.5f,
                                                              bz + 0.5f);
        if(spawnChance <= 0)
            return false;
        if(World.fRand(0, 1) + spawnChance >= 1)
            return true;
        return false;
    }
}