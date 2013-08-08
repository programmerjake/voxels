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

import org.voxels.Block;
import org.voxels.BlockType;
import org.voxels.generate.Tree.TreeType;

public abstract class Plant
{
    public static enum PlantType
    {
        OakTree
        {
            @Override
            public TreeType toTreeType()
            {
                return TreeType.Oak;
            }

            @Override
            public Plant make(final float randSeed)
            {
                return new Tree(toTreeType(), randSeed);
            }
        },
        BirchTree
        {
            @Override
            public TreeType toTreeType()
            {
                return TreeType.Birch;
            }

            @Override
            public Plant make(final float randSeed)
            {
                return new Tree(toTreeType(), randSeed);
            }
        },
        SpruceTree
        {
            @Override
            public TreeType toTreeType()
            {
                return TreeType.Spruce;
            }

            @Override
            public Plant make(final float randSeed)
            {
                return new Tree(toTreeType(), randSeed);
            }
        },
        JungleTree
        {
            @Override
            public TreeType toTreeType()
            {
                return TreeType.Jungle;
            }

            @Override
            public Plant make(final float randSeed)
            {
                return new Tree(toTreeType(), randSeed);
            }
        },
        Cactus
        {
            final class CactusImp extends Plant
            {
                public CactusImp(final float randSeed)
                {
                    super(0, 2, Cactus);
                    int height = 1 + (int)Math.floor(3 * randSeed);
                    for(int y = 0; y < height; y++)
                        setBlockType(0, y, 0, BlockType.BTCactus);
                }

                @Override
                public Block getBlock(final int x, final int y, final int z)
                {
                    if(getBlockType(x, y, z) == BlockType.BTCactus)
                        return Block.NewCactus();
                    return null;
                }
            }

            @Override
            public Plant make(final float randSeed)
            {
                return new CactusImp(randSeed);
            }
        },
        DeadBush
        {
            final class Implementation extends Plant
            {
                /** @param randSeed
                 *            the random seed */
                public Implementation(final float randSeed)
                {
                    super(0, 1, DeadBush);
                    setBlockType(0, 0, 0, BlockType.BTDeadBush);
                }

                @Override
                public Block getBlock(final int x, final int y, final int z)
                {
                    return getBlockType(x, y, z).make(-1);
                }
            }

            @Override
            public Plant make(final float randSeed)
            {
                return new Implementation(randSeed);
            }
        },
        Flower
        {
            final class Implementation extends Plant
            {
                /** @param randSeed
                 *            the random seed */
                public Implementation(final float randSeed)
                {
                    super(0, 1, Flower);
                    setBlockType(0,
                                 0,
                                 0,
                                 (randSeed > 0.3f) ? BlockType.BTDandelion
                                         : BlockType.BTRose);
                }

                @Override
                public Block getBlock(final int x, final int y, final int z)
                {
                    return getBlockType(x, y, z).make(-1);
                }
            }

            @Override
            public Plant make(final float randSeed)
            {
                return new Implementation(randSeed);
            }
        },
        Grass
        {
            final class Implementation extends Plant
            {
                /** @param randSeed
                 *            the random seed */
                public Implementation(final float randSeed)
                {
                    super(0, 1, Grass);
                    setBlockType(0, 0, 0, BlockType.BTTallGrass);
                }

                @Override
                public Block getBlock(final int x, final int y, final int z)
                {
                    return getBlockType(x, y, z).make(-1);
                }
            }

            @Override
            public Plant make(final float randSeed)
            {
                return new Implementation(randSeed);
            }
        },
        ;
        public TreeType toTreeType()
        {
            return null;
        }

        public static PlantType make(final TreeType tt)
        {
            if(tt == null)
                return null;
            switch(tt)
            {
            case Birch:
                return BirchTree;
            case Jungle:
                return JungleTree;
            case Oak:
                return OakTree;
            case Spruce:
                return SpruceTree;
            }
            return null;
        }

        /** @param randSeed
         *            the random seed <br/>
         *            <code>0 &le; randSeed &lt; 1</code>
         * @return the new <code>Plant</code> */
        public abstract Plant make(float randSeed);
    }

    /**
     * 
     */
    public static final int maxXZExtent = 10;
    /**
     * 
     */
    public static final int maxYExtent = 32;
    /** the x and z extents <BR/>
     * -<code>XZExtent</code> &le; x &le; <code>XZExtent</code> <BR/>
     * -<code>XZExtent</code> &le; z &le; <code>XZExtent</code> <BR/> */
    public final int XZExtent;
    /** the y extent<br/>
     * 0 &le; y &le; <code>YExtent</code> */
    public final int YExtent;
    private final BlockType blocks[];
    public final PlantType type;

    private BlockType getBlockTypeInternal(final int x_in,
                                           final int y,
                                           final int z_in)
    {
        int x = x_in, z = z_in;
        x += this.XZExtent;
        z += this.XZExtent;
        if(x < 0 || x >= this.XZExtent * 2 + 1 || y < 0
                || y >= this.YExtent + 1 || z < 0 || z >= this.XZExtent * 2 + 1)
            return BlockType.BTEmpty;
        BlockType retval = this.blocks[x + (this.XZExtent * 2 + 1)
                * (y + (this.YExtent + 1) * z)];
        if(retval == null)
            return BlockType.BTEmpty;
        return retval;
    }

    /** @param x
     *            the x coordinate relative to the base of the tree
     * @param y
     *            the y coordinate relative to the base of the tree
     * @param z
     *            the z coordinate relative to the base of the tree
     * @return the block type */
    final public BlockType getBlockType(final int x, final int y, final int z)
    {
        return getBlockTypeInternal(x, y, z);
    }

    final protected boolean setBlockType(final int x_in,
                                         final int y,
                                         final int z_in,
                                         final BlockType bt)
    {
        int x = x_in, z = z_in;
        x += this.XZExtent;
        z += this.XZExtent;
        if(x < 0 || x >= this.XZExtent * 2 + 1 || y < 0
                || y >= this.YExtent + 1 || z < 0 || z >= this.XZExtent * 2 + 1)
            return false;
        this.blocks[x + (this.XZExtent * 2 + 1) * (y + (this.YExtent + 1) * z)] = bt;
        return true;
    }

    public abstract Block getBlock(final int x, final int y, final int z);

    protected Plant(final int XZExtent, final int YExtent, final PlantType type)
    {
        this.XZExtent = XZExtent;
        this.YExtent = YExtent;
        this.type = type;
        this.blocks = new BlockType[(XZExtent * 2 + 1) * (YExtent + 1)
                * (XZExtent * 2 + 1)];
    }
}
