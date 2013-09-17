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

/** @author jacob */
final class Slime extends TwoHighMob
{
    public Slime()
    {
        super("Slime");
    }

    @Override
    public boolean canGenerateMob(final int bx,
                                  final int by,
                                  final int bz,
                                  final boolean isMobSpawner)
    {
        if(!super.canGenerateMob(bx, by, bz, isMobSpawner))
            return false;
        if(isMobSpawner)
            return true;
        if(by > 40 - World.Depth)
            return false;
        return World.world.canSlimeGenerate(bx, bz);
    }

    private int getMobCount(final int size)
    {
        if(size <= 0)
            return (int)Math.floor(World.fRand(0, 2 + 1));
        int count = 2 + (int)Math.floor(World.fRand(0, 2 + 1));
        int retval = 0;
        for(int i = 0; i < count; i++)
        {
            retval += getMobCount(size - 1);
        }
        return retval;
    }

    @Override
    public void generateMobDrops(final int bx, final int by, final int bz)
    {
        int size = (int)Math.floor(World.fRand(1, 3 + 1));
        int count = getMobCount(size);
        for(int i = 0; i < count; i++)
        {
            generateItem(Block.NewSlime(), bx, by, bz);
        }
    }
}
