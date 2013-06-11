/**
 * this file is part of Voxels
 * 
 * Voxels is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Voxels is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Voxels.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.voxels;

import java.io.*;

/**
 * @author jacob
 * 
 */
public enum ParticleType
{
	/**
	 * 
	 */
	Fire, /**
	 * 
	 */
	RedstoneFire, /**
	 * 
	 */
	Smoke, /**
	 * 
	 */
	FireAnim, /**
	 * 
	 */
	RedstoneFireAnim, /**
	 * 
	 */
	SmokeAnim, /**
	 * 
	 */
	Last;
	/**
	 * 
	 */
	public static final int Count = Last.ordinal();

	/**
	 * read from a <code>DataInput</code>
	 * 
	 * @param i
	 *            <code>DataInput</code> to read from
	 * @return the read <code>ParticleType</code>
	 * @throws IOException
	 *             the exception thrown
	 */
	public static ParticleType read(DataInput i) throws IOException
	{
		int value = i.readUnsignedByte();
		if(value < 0 || value >= Count)
			throw new IOException("value out of range");
		return values()[value];
	}

	/**
	 * write to a <code>DataOutput</code>
	 * 
	 * @param o
	 *            <code>OutputStream</code> to write to
	 * @throws IOException
	 *             the exception thrown
	 */
	public void write(DataOutput o) throws IOException
	{
		o.writeByte(this.ordinal());
	}
}
