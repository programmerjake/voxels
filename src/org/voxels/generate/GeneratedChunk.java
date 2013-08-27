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
package org.voxels.generate;

import org.voxels.*;

/** @author jacob */
public class GeneratedChunk
{
    private static final Allocator<GeneratedChunk> allocator = new Allocator<GeneratedChunk>()
    {
        @Override
        protected GeneratedChunk allocateInternal()
        {
            return new GeneratedChunk();
        }
    };
    /** chunk size */
    public static final int size = World.generatedChunkSize;
    /** chunk x coordinate */
    public int cx;
    /** chunk y coordinate */
    public int cy;
    /** chunk z coordinate */
    public int cz;
    private final Block[] blocks = new Block[size * size * size];

    GeneratedChunk()
    {
    }

    /** creates a <code>GeneratedChunk</code>
     * 
     * @param size
     *            size of resulting chunk
     * @param cx
     *            chunk x coordinate
     * @param cy
     *            chunk y coordinate
     * @param cz
     *            chunk z coordinate
     * @return the new <code>GeneratedChunk</code> */
    public static GeneratedChunk allocate(final int size,
                                          final int cx,
                                          final int cy,
                                          final int cz)
    {
        assert GeneratedChunk.size == size;
        GeneratedChunk retval = allocator.allocate();
        retval.cx = cx;
        retval.cy = cy;
        retval.cz = cz;
        return retval;
    }

    /** sets a block
     * 
     * @param x
     *            block x coordinate (not relative to origin)
     * @param y
     *            block y coordinate (not relative to origin)
     * @param z
     *            block z coordinate (not relative to origin)
     * @param b
     *            block to set to (ownership is transferred to this
     *            GeneratedChunk) */
    public void setBlock(final int x, final int y, final int z, final Block b)
    {
        assert x >= this.cx && x < this.cx + GeneratedChunk.size
                && y >= this.cy && y < this.cy + GeneratedChunk.size
                && z >= this.cz && z < this.cz + GeneratedChunk.size : "array index out of bounds";
        final int i = (x - this.cx) + GeneratedChunk.size
                * ((y - this.cy) + GeneratedChunk.size * (z - this.cz));
        if(this.blocks[i] != null)
            this.blocks[i].free();
        this.blocks[i] = b;
    }

    /** @param x
     *            block x coordinate (not relative to origin)
     * @param y
     *            block y coordinate (not relative to origin)
     * @param z
     *            block z coordinate (not relative to origin)
     * @return the block at (<code>x</code>, <code>y</code>, <code>z</code>) */
    public Block getBlock(final int x, final int y, final int z)
    {
        return this.blocks[(x - this.cx) + GeneratedChunk.size
                * ((y - this.cy) + GeneratedChunk.size * (z - this.cz))];
    }

    public void free()
    {
        for(int i = 0; i < this.blocks.length; i++)
        {
            if(this.blocks[i] != null)
                this.blocks[i].free();
            this.blocks[i] = null;
        }
        allocator.free(this);
    }
}
