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

import org.voxels.*;

/** @author jacob */
abstract class PassiveAnimal extends DayMob
{
    protected final int height;

    public PassiveAnimal(final String name, final int height)
    {
        super(name);
        this.height = height;
    }

    @Override
    public boolean canGenerateMob(final int bx,
                                  final int by,
                                  final int bz,
                                  final boolean isMobSpawner)
    {
        if(!super.canGenerateMob(bx, by, bz, isMobSpawner))
            return false;
        Block b = World.world.getBlockEval(bx, by, bz);
        if(b == null)
            return false;
        if(b.getType() != BlockType.BTEmpty)
            return false;
        b = World.world.getBlockEval(bx, by - 1, bz);
        if(b == null)
            return false;
        if(b.getType() != BlockType.BTGrass)
            return false;
        for(int i = 1; i < this.height; i++)
        {
            b = World.world.getBlockEval(bx, by + i, bz);
            if(b == null)
                return false;
            if(b.isOpaque() || b.getType().drawType == BlockDrawType.BDTLiquid)
                return false;
        }
        return true;
    }
}
