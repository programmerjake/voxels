package org.voxels;

import java.io.*;

/**
 * @author jacob
 * 
 */
public enum EntityType
{
	/**
	 * the type of an empty entity
	 */
	Nothing,
	/**
	 * a block entity
	 */
	Block,
	/**
	 * a particle
	 */
	Particle,
	/**
	 * a falling block
	 */
	FallingBlock,
	/**
	 * used to get <code>EntityType.count</code>
	 */
	Last;
	/**
	 * the number of valid entity types
	 */
	public static final int count = Last.ordinal();

	/**
	 * read from a <code>DataInput</code>
	 * 
	 * @param i
	 *            <code>DataInput</code> to read from
	 * @return the read <code>EntityType</code>
	 * @throws IOException
	 *             the exception thrown
	 */
	public static EntityType read(DataInput i) throws IOException
	{
		int value = i.readUnsignedByte();
		if(value < 0 || value >= count)
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
