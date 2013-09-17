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

import static org.voxels.World.world;

import org.voxels.*;

/** @author jacob */
final class Squid extends MobType
{
    public Squid()
    {
        super("Squid");
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
        if(by + World.Depth < 45 || by + World.Depth > 62)
            return false;
        Block b = world.getBlock(bx, by, bz);
        if(b == null)
            return false;
        if(b.getType().drawType != BlockDrawType.BDTLiquid)
            return false;
        if(World.fRand(0, 1) < 0.001f)
            return true;
        return false;
    }

    @Override
    public void generateMobDrops(final int bx, final int by, final int bz)
    {
        int count = (int)Math.floor(World.fRand(1, 3 + 1));
        for(int i = 0; i < count; i++)
        {
            generateItem(Block.NewInkSac(), bx, by, bz);
        }
    }
}
