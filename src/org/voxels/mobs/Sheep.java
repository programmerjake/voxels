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
import org.voxels.BlockType.DyeColor;

/** @author jacob */
final class Sheep extends PassiveAnimal
{
    public Sheep()
    {
        super("Sheep", 2);
    }

    @Override
    public void generateMobDrops(final int bx, final int by, final int bz)
    {
        int count = (int)Math.floor(World.fRand(1, 3 + 1));
        if(World.fRand(0, 4) >= 1)
            count = 1;
        DyeColor dyeColor;
        float t = World.fRand(0, 1);
        if(t < 0.81836f)
            dyeColor = DyeColor.BoneMeal;
        else
        {
            t -= 0.81836f;
            if(t < 0.05f)
                dyeColor = DyeColor.Gray;
            else if(t < 0.1f)
                dyeColor = DyeColor.LightGray;
            else if(t < 0.15f)
                dyeColor = DyeColor.InkSac;
            else if(t < 0.18f)
                dyeColor = DyeColor.CocoaBeans;
            else
                dyeColor = DyeColor.Pink;
        }
        for(int i = 0; i < count; i++)
            generateItem(Block.NewWool(dyeColor), bx, by, bz);
    }
}
