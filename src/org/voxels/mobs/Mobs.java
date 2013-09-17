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

import java.io.*;

/** @author jacob */
public final class Mobs
{
    private Mobs()
    {
        throw new UnsupportedOperationException("can't create instance of Mobs");
    }

    private static final MobType[] mobs = new MobType[]
    {
        new Creeper(), new Slime(), new Sheep(), new Squid(), new Skeleton(),
    };

    public static int getMobCount()
    {
        return mobs.length;
    }

    public static MobType getMob(final int index)
    {
        return mobs[index];
    }

    public static MobType getMobFromName(final String mobName)
    {
        for(int i = 0; i < mobs.length; i++)
            if(mobs[i].getName().equals(mobName))
                return mobs[i];
        return null;
    }

    public static void
        write(final MobType mobType, final DataOutput o) throws IOException
    {
        String name = mobType.getName();
        o.writeInt(name.length());
        o.writeChars(name);
    }

    public static MobType read(final DataInput i) throws IOException
    {
        int len = i.readInt();
        if(len < 0)
            throw new IOException("mob name length is out of range");
        StringBuilder sb = new StringBuilder(len);
        for(int j = 0; j < len; j++)
            sb.append(i.readChar());
        String name = sb.toString();
        for(int j = 0; j < mobs.length; j++)
        {
            if(mobs[j].equals(name))
                return mobs[j];
        }
        throw new IOException("invalid mob name");
    }
}
