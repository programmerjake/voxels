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

import static org.voxels.World.world;

import java.util.Random;

import org.voxels.*;

/** @author jacob */
public final class Tree extends Plant
{
    public static final float defaultBiomeColorR = 91f / 255f;
    public static final float defaultBiomeColorG = 171f / 255f;
    public static final float defaultBiomeColorB = 71f / 255f;

    /** @author jacob */
    public static enum TreeType
    {
        /**
         * 
         */
        Oak("leaves.png", "wood.png", "sapling.png"), /**
         * 
         */
        Birch("birchleaves.png", "birchwood.png", "birchsapling.png"), /**
         * 
         */
        Spruce("spruceleaves.png", "sprucewood.png", "sprucesapling.png"), /**
         * 
         */
        Jungle("jungleleaves.png", "junglewood.png", "junglesapling.png");
        public final String leavesImgName;
        public final String woodImgName;
        public final String saplingImgName;

        private TreeType(final String leavesImgName,
                         final String woodImgName,
                         final String saplingImgName)
        {
            this.leavesImgName = leavesImgName;
            this.woodImgName = woodImgName;
            this.saplingImgName = saplingImgName;
        }
    }

    private final TreeType treeType;

    private int getWoodOrientation(final int x, final int y, final int z)
    {
        if(getBlockType(x, y - 1, z) == BlockType.BTWood
                || getBlockType(x, y + 1, z) == BlockType.BTWood)
            return 0; // ±y sides without bark
        if(getBlockType(x - 1, y, z) == BlockType.BTWood
                || getBlockType(x + 1, y, z) == BlockType.BTWood)
            return 1; // ±x sides without bark
        if(getBlockType(x, y, z - 1) == BlockType.BTWood
                || getBlockType(x, y, z + 1) == BlockType.BTWood)
            return 2; // ±z sides without bark
        return 3; // all sides with bark
    }

    private int getVinesOrientation(final int x, final int y, final int z)
    {
        if(getBlockType(x - 1, y, z).isSolid())
            return Block.getOrientationFromVector(Vector.NX);
        if(getBlockType(x + 1, y, z).isSolid())
            return Block.getOrientationFromVector(Vector.X);
        if(getBlockType(x, y, z - 1).isSolid())
            return Block.getOrientationFromVector(Vector.NZ);
        if(getBlockType(x, y, z + 1).isSolid())
            return Block.getOrientationFromVector(Vector.Z);
        if(getBlockType(x, y + 1, z) == BlockType.BTVines)
            return getVinesOrientation(x, y + 1, z);
        return 0;
    }

    private final int positionalRandSeed;

    /** return a random integer x in the range 0 &le; x &lt; <code>limit</code>
     * 
     * @param x
     *            the x coordinate
     * @param y
     *            the y coordinate
     * @param z
     *            the z coordinate
     * @param limit
     *            one more than the maximum return value
     * @return the random integer */
    private int positionalRand(final int x,
                               final int y,
                               final int z,
                               final int limit)
    {
        if(limit <= 1)
            return 0;
        int hash = x * 2304912 + y * 734872381 + z * 842301830
                + this.positionalRandSeed;
        final long mask = (1L << 48) - 1;
        final long multiplier = 0x5DEECE66DL;
        long v = (hash ^ multiplier) & mask;
        for(int i = 0; i < 5; i++)
        {
            v *= multiplier;
            v += 0xB;
            v &= mask;
        }
        if(Integer.bitCount(limit) == 1)
        {
            v >>= 48 - Integer.numberOfTrailingZeros(limit);
            v &= limit + 1;
        }
        else
        {
            v %= limit;
            if(v < 0)
                v += limit;
        }
        return (int)v;
    }

    private int getCocoaOrientation(final int x, final int y, final int z)
    {
        final BlockType nx, px, nz, pz;
        nx = getBlockType(x - 1, y, z);
        px = getBlockType(x + 1, y, z);
        nz = getBlockType(x, y, z - 1);
        pz = getBlockType(x, y, z + 1);
        int oCount = 0;
        if(nx == BlockType.BTWood)
            oCount++;
        if(nz == BlockType.BTWood)
            oCount++;
        if(px == BlockType.BTWood)
            oCount++;
        if(pz == BlockType.BTWood)
            oCount++;
        int oIndex = positionalRand(x, y, z, oCount);
        if(nx == BlockType.BTWood)
        {
            if(oIndex-- == 0)
                return 0;
        }
        if(nz == BlockType.BTWood)
        {
            if(oIndex-- == 0)
                return 1;
        }
        if(px == BlockType.BTWood)
        {
            if(oIndex-- == 0)
                return 2;
        }
        if(pz == BlockType.BTWood)
        {
            if(oIndex-- == 0)
                return 3;
        }
        return -1;
    }

    /** @param x
     *            the block's x coordinate
     * @param y
     *            the block's y coordinate
     * @param z
     *            the block's z coordinate
     * @return the block */
    @Override
    public Block getBlock(final int x, final int y, final int z)
    {
        BlockType bt = getBlockType(x, y, z);
        if(bt == BlockType.BTEmpty)
            return null;
        if(bt == BlockType.BTWood)
            return Block.NewWood(this.treeType, getWoodOrientation(x, y, z));
        if(bt == BlockType.BTLeaves)
            return Block.NewLeaves(this.treeType, 0);
        if(bt == BlockType.BTVines)
            return Block.NewVines(getVinesOrientation(x, y, z));
        if(bt == BlockType.BTCocoa)
            return Block.NewCocoa(positionalRand(x, y, z, 3),
                                  getCocoaOrientation(x, y, z));
        return bt.make(-1);
    }

    private void generateLeaves(final int x, final int y, final int z)
    {
        if(this.treeType == TreeType.Jungle)
        {
            final int sz = 4;
            for(int dx = -sz; dx <= sz; dx++)
                for(int dy = 0; dy <= sz; dy++)
                    for(int dz = -sz; dz <= sz; dz++)
                        if(dx * dx + dy * dy * 2 * 2 + dz * dz < (sz + 1)
                                * (sz + 1))
                            if(getBlockType(x + dx, y + dy, z + dz) != BlockType.BTWood)
                                setBlockType(x + dx,
                                             y + dy,
                                             z + dz,
                                             BlockType.BTLeaves);
            return;
        }
        final int sz = 1;
        for(int dx = -sz; dx <= sz; dx++)
            for(int dy = -sz; dy <= sz; dy++)
                for(int dz = -sz; dz <= sz; dz++)
                    if(dx * dx + dy * dy + dz * dz < (sz + 1) * (sz + 1))
                        if(getBlockType(x + dx, y + dy, z + dz) != BlockType.BTWood)
                            setBlockType(x + dx,
                                         y + dy,
                                         z + dz,
                                         BlockType.BTLeaves);
    }

    private Vector generateTreeBranch_t1 = Vector.allocate();

    private void generateTreeBranch(final int x_in,
                                    final int y_in,
                                    final int z_in,
                                    final Random rand,
                                    final int dx_in,
                                    final int dz_in)
    {
        int x = x_in, y = y_in, z = z_in;
        int dx = dx_in, dz = dz_in;
        while(true)
        {
            if(getBlockType(x, y, z) == BlockType.BTWood)
                return;
            if(!setBlockType(x, y, z, BlockType.BTWood))
                return;
            generateLeaves(x, y, z);
            if(rand.nextInt(5) <= 2 || y >= this.YExtent - 1)
                return;
            dx += rand.nextInt(3) - 1;
            dz += rand.nextInt(3) - 1;
            int dir = Block.getOrientationFromVector(this.generateTreeBranch_t1.set(dx,
                                                                                    1.0f,
                                                                                    dz));
            int ndx = Block.getOrientationDX(dir);
            int ndy = Block.getOrientationDY(dir);
            int ndz = Block.getOrientationDZ(dir);
            x += ndx;
            y += ndy;
            z += ndz;
        }
    }

    private void generateLargeBranchedTree(final Random rand,
                                           final boolean isBigTrunk,
                                           final int ysz)
    {
        for(int y = 0; y <= ysz - 2; y++)
        {
            if(isBigTrunk)
            {
                if((y >= ysz / 3 && y >= 5 && rand.nextInt(8) == 0)
                        || y == ysz - 2)
                    generateTreeBranch(0, y, 0, rand, -1, -1);
                else
                    setBlockType(0, y, 0, BlockType.BTWood);
                if((y >= ysz / 3 && y >= 5 && rand.nextInt(8) == 0)
                        || y == ysz - 2)
                    generateTreeBranch(1, y, 0, rand, 1, -1);
                else
                    setBlockType(1, y, 0, BlockType.BTWood);
                if((y >= ysz / 3 && y >= 5 && rand.nextInt(8) == 0)
                        || y == ysz - 2)
                    generateTreeBranch(1, y, 1, rand, 1, 1);
                else
                    setBlockType(1, y, 1, BlockType.BTWood);
                if((y >= ysz / 3 && y >= 5 && rand.nextInt(8) == 0)
                        || y == ysz - 2)
                    generateTreeBranch(0, y, 1, rand, -1, 1);
                else
                    setBlockType(0, y, 1, BlockType.BTWood);
            }
            else if((y >= ysz / 3 && y >= 5 && rand.nextInt(3) == 0)
                    || y == ysz - 2)
                generateTreeBranch(0, y, 0, rand, 0, 0);
            else
                setBlockType(0, y, 0, BlockType.BTWood);
        }
    }

    private void makeLeafRing(final int y, final int r)
    {
        for(int x = -r; x <= r; x++)
        {
            for(int z = -r; z <= r; z++)
            {
                if(x == 0 && z == 0)
                    setBlockType(x, y, z, BlockType.BTWood);
                else if(x * x + z * z < (r + 0.25f) * (r + 0.25f))
                    if(getBlockType(x, y, z) != BlockType.BTWood)
                        setBlockType(x, y, z, BlockType.BTLeaves);
            }
        }
    }

    private void makeVines(final Random rand)
    {
        for(int x = -this.XZExtent; x <= this.XZExtent; x++)
        {
            for(int y = 0; y <= this.YExtent; y++)
            {
                for(int z = -this.XZExtent; z <= this.XZExtent; z++)
                {
                    if(getBlockType(x, y, z) != BlockType.BTEmpty)
                        continue;
                    if(rand.nextFloat() > 0.1f)
                        continue;
                    boolean doAdd = false;
                    if(getBlockType(x + 1, y, z).isSolid())
                        doAdd = true;
                    else if(getBlockType(x - 1, y, z).isSolid())
                        doAdd = true;
                    else if(getBlockType(x, y, z + 1).isSolid())
                        doAdd = true;
                    else if(getBlockType(x, y, z - 1).isSolid())
                        doAdd = true;
                    if(doAdd)
                    {
                        int length = rand.nextInt(3) + 1;
                        for(int dy = 0; dy > -length; dy--)
                        {
                            setBlockType(x, y + dy, z, BlockType.BTVines);
                        }
                    }
                }
            }
        }
    }

    private void makeCocoa(final Random rand)
    {
        for(int x = -this.XZExtent; x <= this.XZExtent; x++)
        {
            for(int y = 0; y <= this.YExtent; y++)
            {
                for(int z = -this.XZExtent; z <= this.XZExtent; z++)
                {
                    if(getBlockType(x, y, z) != BlockType.BTEmpty)
                        continue;
                    if(rand.nextFloat() > 0.2f)
                        continue;
                    boolean doAdd = false;
                    if(getBlockType(x + 1, y, z) == BlockType.BTWood)
                        doAdd = true;
                    else if(getBlockType(x - 1, y, z) == BlockType.BTWood)
                        doAdd = true;
                    else if(getBlockType(x, y, z + 1) == BlockType.BTWood)
                        doAdd = true;
                    else if(getBlockType(x, y, z - 1) == BlockType.BTWood)
                        doAdd = true;
                    if(doAdd)
                    {
                        setBlockType(x, y, z, BlockType.BTCocoa);
                    }
                }
            }
        }
    }

    /** @param treeType
     *            the tree type
     * @param t
     *            the random parameter */
    public Tree(final TreeType treeType, final float t)
    {
        super(maxXZExtent, maxYExtent, PlantType.make(treeType));
        this.treeType = treeType;
        Random rand = new Random((long)(t * Math.PI * 100000.0));
        switch(treeType)
        {
        case Birch:
        {
            final int ysz = 5 + rand.nextInt(7 - 5 + 1);
            for(int y = 0; y <= ysz; y++)
            {
                switch(ysz - y)
                {
                case 0:
                    setBlockType(0, y, 0, BlockType.BTLeaves);
                    setBlockType(-1, y, 0, BlockType.BTLeaves);
                    setBlockType(0, y, -1, BlockType.BTLeaves);
                    setBlockType(1, y, 0, BlockType.BTLeaves);
                    setBlockType(0, y, 1, BlockType.BTLeaves);
                    break;
                case 1:
                    setBlockType(0, y, 0, BlockType.BTWood);
                    setBlockType(-1, y, 0, BlockType.BTLeaves);
                    setBlockType(0, y, -1, BlockType.BTLeaves);
                    setBlockType(1, y, 0, BlockType.BTLeaves);
                    setBlockType(0, y, 1, BlockType.BTLeaves);
                    if(rand.nextBoolean())
                        setBlockType(-1, y, -1, BlockType.BTLeaves);
                    if(rand.nextBoolean())
                        setBlockType(1, y, -1, BlockType.BTLeaves);
                    if(rand.nextBoolean())
                        setBlockType(1, y, 1, BlockType.BTLeaves);
                    if(rand.nextBoolean())
                        setBlockType(-1, y, 1, BlockType.BTLeaves);
                    break;
                case 2:
                case 3:
                    for(int x = -2; x <= 2; x++)
                    {
                        for(int z = -2; z <= 2; z++)
                        {
                            if(Math.abs(x) == 2 && Math.abs(z) == 2
                                    && rand.nextBoolean())
                                continue;
                            setBlockType(x, y, z, BlockType.BTLeaves);
                        }
                    }
                    setBlockType(0, y, 0, BlockType.BTWood);
                    break;
                default:
                    setBlockType(0, y, 0, BlockType.BTWood);
                    break;
                }
            }
            break;
        }
        case Jungle:
            if(rand.nextFloat() >= 0.1)
            {
                final int ysz = 6 + rand.nextInt(9 - 6 + 1);
                generateLargeBranchedTree(rand, false, ysz);
            }
            else
            {
                final int ysz = maxYExtent;
                generateLargeBranchedTree(rand, true, ysz);
            }
            makeVines(rand);
            makeCocoa(rand);
            break;
        case Spruce:
            switch(rand.nextInt(10))
            {
            case 0:
            {
                final int ysz = 9 + rand.nextInt(12 - 9 + 1);
                int leavesHeight = 2 + rand.nextInt(4);
                for(int y = 0; y <= ysz; y++)
                {
                    if(ysz - y == 0)
                    {
                        setBlockType(0, y, 0, BlockType.BTLeaves);
                        setBlockType(-1, y, 0, BlockType.BTLeaves);
                        setBlockType(0, y, -1, BlockType.BTLeaves);
                        setBlockType(1, y, 0, BlockType.BTLeaves);
                        setBlockType(0, y, 1, BlockType.BTLeaves);
                    }
                    else if(ysz - y < leavesHeight)
                    {
                        setBlockType(0, y, 0, BlockType.BTWood);
                        setBlockType(-1, y, 0, BlockType.BTLeaves);
                        setBlockType(0, y, -1, BlockType.BTLeaves);
                        setBlockType(1, y, 0, BlockType.BTLeaves);
                        setBlockType(0, y, 1, BlockType.BTLeaves);
                    }
                    else
                    {
                        setBlockType(0, y, 0, BlockType.BTWood);
                    }
                }
                break;
            }
            case 1:
            {
                final int ysz = 9 + rand.nextInt(12 - 9 + 1);
                int leavesHeight = ysz - 2 - rand.nextInt(2);
                for(int y = 0; y <= ysz; y++)
                {
                    if(ysz - y == 0)
                    {
                        setBlockType(0, y, 0, BlockType.BTLeaves);
                        setBlockType(-1, y, 0, BlockType.BTLeaves);
                        setBlockType(0, y, -1, BlockType.BTLeaves);
                        setBlockType(1, y, 0, BlockType.BTLeaves);
                        setBlockType(0, y, 1, BlockType.BTLeaves);
                    }
                    else if(ysz - y < leavesHeight)
                    {
                        makeLeafRing(y, (ysz - y) % 2 == 0 ? 1 : 2);
                    }
                    else
                    {
                        setBlockType(0, y, 0, BlockType.BTWood);
                    }
                }
                break;
            }
            default:
            {
                final int ysz = 6 + rand.nextInt(8 - 6 + 1);
                int leavesHeight = ysz - 2 - rand.nextInt(2);
                for(int y = 0; y <= ysz; y++)
                {
                    if(ysz - y == 0)
                    {
                        setBlockType(0, y, 0, BlockType.BTLeaves);
                        setBlockType(-1, y, 0, BlockType.BTLeaves);
                        setBlockType(0, y, -1, BlockType.BTLeaves);
                        setBlockType(1, y, 0, BlockType.BTLeaves);
                        setBlockType(0, y, 1, BlockType.BTLeaves);
                    }
                    else if(ysz - y < leavesHeight)
                    {
                        makeLeafRing(y, ((ysz - y) % 2 == 0) ? 1 : 2);
                    }
                    else
                    {
                        setBlockType(0, y, 0, BlockType.BTWood);
                    }
                }
                break;
            }
            }
            break;
        case Oak:
        default:
        {
            if(rand.nextFloat() >= 0.1)
            {
                final int ysz = 6 + rand.nextInt(8 - 6 + 1);
                for(int y = 0; y <= ysz - 2; y++)
                {
                    setBlockType(0, y, 0, BlockType.BTWood);
                }
                for(int y = ysz - 4; y <= ysz; y++)
                {
                    int dy = y - (ysz - 3);
                    int sz = 4 - Math.abs(dy);
                    for(int x = -sz; x <= sz; x++)
                        for(int z = -sz; z <= sz; z++)
                            if(getBlockType(x, y, z) == BlockType.BTEmpty)
                                setBlockType(x, y, z, BlockType.BTLeaves);
                }
            }
            else
            {
                final int ysz = 14;
                generateLargeBranchedTree(rand, false, ysz);
            }
        }
        }
        this.positionalRandSeed = rand.nextInt();
    }

    /** generate a tree
     * 
     * @param type
     *            the sapling generating this tree
     * @param tx
     *            the tree's x coordinate
     * @param ty
     *            the tree's y coordinate
     * @param tz
     *            the tree's z coordinate
     * @param t
     *            the tree's random parameter
     * @return if this tree generated */
    public static boolean generate(final Block type,
                                   final int tx,
                                   final int ty,
                                   final int tz,
                                   final float t)
    {
        Tree tree = new Tree(type.treeGetTreeType(), t);
        for(int dx = -tree.XZExtent; dx <= tree.XZExtent; dx++)
        {
            for(int dy = 0; dy <= tree.YExtent; dy++)
            {
                for(int dz = -tree.XZExtent; dz <= tree.XZExtent; dz++)
                {
                    int x = tx + dx, y = ty + dy, z = tz + dz;
                    Block b = world.getBlockEval(x, y, z);
                    BlockType blockType = tree.getBlockType(dx, dy, dz);
                    if(blockType == BlockType.BTEmpty)
                        continue;
                    if(b == null)
                        return false;
                    BlockType.Replaceability replaceability = b.getReplaceability(blockType);
                    if(replaceability == BlockType.Replaceability.CanNotGrow)
                        return false;
                }
            }
        }
        for(int dx = -tree.XZExtent; dx <= tree.XZExtent; dx++)
        {
            for(int dy = 0; dy <= tree.YExtent; dy++)
            {
                for(int dz = -tree.XZExtent; dz <= tree.XZExtent; dz++)
                {
                    int x = tx + dx, y = ty + dy, z = tz + dz;
                    Block b = world.getBlockEval(x, y, z);
                    BlockType blockType = tree.getBlockType(dx, dy, dz);
                    if(blockType == BlockType.BTEmpty)
                        continue;
                    if(b == null)
                        return false;
                    BlockType.Replaceability replaceability = b.getReplaceability(blockType);
                    if(replaceability == BlockType.Replaceability.CanNotGrow)
                        return false;
                    if(replaceability == BlockType.Replaceability.Replace)
                    {
                        b = tree.getBlock(dx, dy, dz);
                        if(b != null)
                            world.setBlock(x, y, z, b);
                    }
                }
            }
        }
        return true;
    }

    /** generate a tree with a random height
     * 
     * @param type
     *            the sapling generating this tree
     * @param tx
     *            the tree's x coordinate
     * @param ty
     *            the tree's y coordinate
     * @param tz
     *            the tree's z coordinate */
    public static void generate(final Block type,
                                final int tx,
                                final int ty,
                                final int tz)
    {
        if(!generate(type, tx, ty, tz, World.fRand(0, 1)))
            world.setBlock(tx, ty, tz, type);
    }
}
