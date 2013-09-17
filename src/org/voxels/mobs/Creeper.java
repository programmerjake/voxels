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

import org.voxels.Block;
import org.voxels.World;

final class Creeper extends CombinationMob
{
    public Creeper()
    {
        super(new EmptyTwoHighMob("Creeper"), new EmptyNightMob("Creeper"));
    }

    @Override
    public void generateMobDrops(final int bx, final int by, final int bz)
    {
        int count = (int)Math.floor(World.fRand(0, 2 + 1));
        for(int i = 0; i < count; i++)
            generateItem(Block.NewGunpowder(), bx, by, bz);
    }
}