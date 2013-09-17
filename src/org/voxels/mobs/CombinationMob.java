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

abstract class CombinationMob extends MobType
{
    private final MobType mob1, mob2;

    protected CombinationMob(final MobType mob1, final MobType mob2)
    {
        super(mob1.getName());
        this.mob1 = mob1;
        this.mob2 = mob2;
    }

    @Override
    public boolean canGenerateMob(final int bx,
                                  final int by,
                                  final int bz,
                                  final boolean isMobSpawner)
    {
        return this.mob1.canGenerateMob(bx, by, bz, isMobSpawner)
                && this.mob2.canGenerateMob(bx, by, bz, isMobSpawner);
    }
}