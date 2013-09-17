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

import static org.voxels.PlayerList.players;

import org.voxels.*;

public abstract class MobType
{
    private final String name;

    public boolean canGenerateMob(final int bx,
                                  final int by,
                                  final int bz,
                                  final boolean isMobSpawner)
    {
        if(isMobSpawner)
            return true;
        PlayerList.PlayerIterator iter = players.iterator();
        for(; !iter.isEnd(); iter.next())
        {
            Player p = iter.get();
            Vector v = Vector.allocate(bx + 0.5f, by + 0.5f, bz + 0.5f)
                             .subAndSet(p.getPosition());
            if(v.abs() <= 24)
            {
                v.free();
                iter.free();
                return false;
            }
            v.free();
        }
        iter.free();
        return true;
    }

    public final void generateMob(final int bx,
                                  final int by,
                                  final int bz,
                                  final boolean isMobSpawner)
    {
        if(canGenerateMob(bx, by, bz, isMobSpawner))
            generateMobDrops(bx, by, bz);
    }

    public abstract void generateMobDrops(final int bx,
                                          final int by,
                                          final int bz);

    public final String getName()
    {
        return this.name;
    }

    protected MobType(final String name)
    {
        this.name = name;
    }

    /** @param type
     *            the block type<br/>
     *            free() is called
     * @param bx
     * @param by
     * @param bz */
    protected final void generateItem(final Block type,
                                      final int bx,
                                      final int by,
                                      final int bz)
    {
        Vector pos = Vector.allocate(bx + 0.5f, by + 0.5f, bz + 0.5f);
        Vector velocity = World.vRand(Vector.allocate(), 0.1f);
        World.world.insertEntity(Entity.NewBlock(pos, type, velocity));
        pos.free();
        velocity.free();
        type.free();
    }
}