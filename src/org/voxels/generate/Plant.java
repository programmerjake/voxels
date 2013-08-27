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
                return Tree.allocate(toTreeType(), randSeed);
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
                return Tree.allocate(toTreeType(), randSeed);
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
                return Tree.allocate(toTreeType(), randSeed);
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
                return Tree.allocate(toTreeType(), randSeed);
            }
        },
        Cactus
        {
            @Override
            public Plant make(final float randSeed)
            {
                return CactusImplementation.allocate(randSeed);
            }
        },
        DeadBush
        {
            @Override
            public Plant make(final float randSeed)
            {
                return DeadBushImplementation.allocate(randSeed);
            }
        },
        Flower
        {
            @Override
            public Plant make(final float randSeed)
            {
                return FlowerImplementation.allocate(randSeed);
            }
        },
        Grass
        {
            @Override
            public Plant make(final float randSeed)
            {
                return GrassImplementation.allocate(randSeed);
            }
        },
        ;
        protected static final class CactusImplementation extends Plant
        {
            private static final Allocator<CactusImplementation> allocator = new Allocator<CactusImplementation>()
            {
                @Override
                protected CactusImplementation allocateInternal()
                {
                    return new CactusImplementation();
                }
            };

            private CactusImplementation init(final float randSeed)
            {
                super.init(0, 2, Cactus);
                int height = 1 + (int)Math.floor(3 * randSeed);
                for(int y = 0; y < height; y++)
                    setBlockType(0, y, 0, BlockType.BTCactus);
                return this;
            }

            public static CactusImplementation allocate(final float randSeed)
            {
                return allocator.allocate().init(randSeed);
            }

            @Override
            public Block getBlock(final int x, final int y, final int z)
            {
                if(getBlockType(x, y, z) == BlockType.BTCactus)
                    return Block.NewCactus();
                return null;
            }

            @Override
            public void free()
            {
                allocator.free(this);
            }
        }

        protected static final class DeadBushImplementation extends Plant
        {
            private static final Allocator<DeadBushImplementation> allocator = new Allocator<Plant.PlantType.DeadBushImplementation>()
            {
                @Override
                protected DeadBushImplementation allocateInternal()
                {
                    return new DeadBushImplementation();
                }
            };

            /** @param randSeed
             *            the random seed */
            private DeadBushImplementation init(final float randSeed)
            {
                super.init(0, 1, DeadBush);
                setBlockType(0, 0, 0, BlockType.BTDeadBush);
                return this;
            }

            public static DeadBushImplementation allocate(final float randSeed)
            {
                return allocator.allocate().init(randSeed);
            }

            @Override
            public Block getBlock(final int x, final int y, final int z)
            {
                return getBlockType(x, y, z).make(-1);
            }

            @Override
            public void free()
            {
                allocator.free(this);
            }
        }

        protected static final class FlowerImplementation extends Plant
        {
            private static final Allocator<FlowerImplementation> allocator = new Allocator<Plant.PlantType.FlowerImplementation>()
            {
                @Override
                protected FlowerImplementation allocateInternal()
                {
                    return new FlowerImplementation();
                }
            };

            private FlowerImplementation init(final float randSeed)
            {
                super.init(0, 1, Flower);
                setBlockType(0,
                             0,
                             0,
                             (randSeed > 0.33333f) ? BlockType.BTDandelion
                                     : BlockType.BTRose);
                return this;
            }

            @Override
            public Block getBlock(final int x, final int y, final int z)
            {
                return getBlockType(x, y, z).make(-1);
            }

            public static FlowerImplementation allocate(final float randSeed)
            {
                return allocator.allocate().init(randSeed);
            }

            @Override
            public void free()
            {
                allocator.free(this);
            }
        }

        protected static final class GrassImplementation extends Plant
        {
            private static final Allocator<GrassImplementation> allocator = new Allocator<Plant.PlantType.GrassImplementation>()
            {
                @Override
                protected GrassImplementation allocateInternal()
                {
                    return new GrassImplementation();
                }
            };

            /** @param randSeed
             *            the random seed */
            private GrassImplementation init(final float randSeed)
            {
                super.init(0, 1, Grass);
                setBlockType(0, 0, 0, BlockType.BTTallGrass);
                return this;
            }

            public static GrassImplementation allocate(final float randSeed)
            {
                return allocator.allocate().init(randSeed);
            }

            @Override
            public Block getBlock(final int x, final int y, final int z)
            {
                return getBlockType(x, y, z).make(-1);
            }

            @Override
            public void free()
            {
                allocator.free(this);
            }
        }

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
    public int XZExtent;
    /** the y extent<br/>
     * 0 &le; y &le; <code>YExtent</code> */
    public int YExtent;
    private final BlockType blocks[] = new BlockType[(maxXZExtent * 2 + 1)
            * (maxYExtent + 1) * (maxXZExtent * 2 + 1)];
    public PlantType type;

    protected void clear()
    {
        for(int i = 0; i < this.blocks.length; i++)
            this.blocks[i] = BlockType.BTEmpty;
    }

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

    /** @param x
     *            the x coordinate
     * @param y
     *            the y coordinate
     * @param z
     *            the z coordinate
     * @return the new block */
    public abstract Block getBlock(final int x, final int y, final int z);

    protected Plant()
    {
    }

    protected Plant init(final int XZExtent,
                         final int YExtent,
                         final PlantType type)
    {
        clear();
        this.XZExtent = XZExtent;
        this.YExtent = YExtent;
        this.type = type;
        return this;
    }

    public abstract void free();
}
