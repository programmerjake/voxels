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
