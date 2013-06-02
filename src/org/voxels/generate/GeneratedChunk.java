package org.voxels.generate;

import org.voxels.Block;

/**
 * @author jacob
 * 
 */
public class GeneratedChunk
{
	/**
	 * chunk size
	 */
	public final int size;
	/**
	 * chunk x coordinate
	 */
	public final int cx;
	/**
	 * chunk y coordinate
	 */
	public final int cy;
	/**
	 * chunk z coordinate
	 */
	public final int cz;
	private Block[] blocks;

	/**
	 * creates a <code>GeneratedChunk</code>
	 * 
	 * @param size
	 *            size of resulting chunk
	 * @param cx
	 *            chunk x coordinate
	 * @param cy
	 *            chunk y coordinate
	 * @param cz
	 *            chunk z coordinate
	 */
	public GeneratedChunk(int size, int cx, int cy, int cz)
	{
		this.size = size;
		this.blocks = new Block[size * size * size];
		this.cx = cx;
		this.cy = cy;
		this.cz = cz;
	}

	/**
	 * sets a block
	 * 
	 * @param x
	 *            block x coordinate (not relative to origin)
	 * @param y
	 *            block y coordinate (not relative to origin)
	 * @param z
	 *            block z coordinate (not relative to origin)
	 * @param b
	 *            block to set to
	 */
	public void setBlock(int x, int y, int z, Block b)
	{
		assert x >= this.cx && x < this.cx + this.size && y >= this.cy
		        && y < this.cy + this.size && z >= this.cz
		        && z < this.cz + this.size : "array index out of bounds";
		this.blocks[(x - this.cx) + this.size
		        * ((y - this.cy) + this.size * (z - this.cz))] = b;
	}

	/**
	 * @param x
	 *            block x coordinate (not relative to origin)
	 * @param y
	 *            block y coordinate (not relative to origin)
	 * @param z
	 *            block z coordinate (not relative to origin)
	 * @return the block at (<code>x</code>, <code>y</code>, <code>z</code>)
	 */
	public Block getBlock(int x, int y, int z)
	{
		return this.blocks[(x - this.cx) + this.size
		        * ((y - this.cy) + this.size * (z - this.cz))];
	}
}
