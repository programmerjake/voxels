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

import org.voxels.Block;
import org.voxels.BlockType;
import org.voxels.Vector;
import org.voxels.World;

/** @author jacob */
public final class Tree {
	/** @author jacob */
	public static enum TreeType {
		/**
         * 
         */
		Oak, /**
         * 
         */
		Birch, /**
         * 
         */
		Spruce, /**
         * 
         */
		Jungle
	}

	private final TreeType treeType;
	/**
     * 
     */
	public static final int maxXZExtent = 10;
	/**
     * 
     */
	public static final int maxYExtent = 32;
	/**
	 * the x and z extents <BR/>
	 * -<code>XZExtent</code> &le; x &le; <code>XZExtent</code> <BR/>
	 * -<code>XZExtent</code> &le; z &le; <code>XZExtent</code> <BR/>
	 */
	public final int XZExtent;
	/**
	 * the y extent<br/>
	 * 0 &le; y &le; <code>YExtent</code>
	 */
	public final int YExtent;
	private BlockType blocks[];

	private BlockType getBlockTypeInternal(int x_in, int y, int z_in) {
		int x = x_in, z = z_in;
		x += this.XZExtent;
		z += this.XZExtent;
		if (x < 0 || x >= this.XZExtent * 2 + 1 || y < 0
				|| y >= this.YExtent + 1 || z < 0 || z >= this.XZExtent * 2 + 1)
			return BlockType.BTEmpty;
		BlockType retval = this.blocks[x + (this.XZExtent * 2 + 1)
				* (y + (this.YExtent + 1) * z)];
		if (retval == null)
			return BlockType.BTEmpty;
		return retval;
	}

	/**
	 * @param x
	 *            the x coordinate relative to the base of the tree
	 * @param y
	 *            the y coordinate relative to the base of the tree
	 * @param z
	 *            the z coordinate relative to the base of the tree
	 * @return the block type
	 */
	public BlockType getBlockType(int x, int y, int z) {
		return getBlockTypeInternal(x, y, z);
	}

	private int getWoodOrientation(int x, int y, int z) {
		if (getBlockType(x, y - 1, z) == BlockType.BTWood
				|| getBlockType(x, y + 1, z) == BlockType.BTWood)
			return 0; // ±y sides without bark
		if (getBlockType(x - 1, y, z) == BlockType.BTWood
				|| getBlockType(x + 1, y, z) == BlockType.BTWood)
			return 1; // ±x sides without bark
		if (getBlockType(x, y, z - 1) == BlockType.BTWood
				|| getBlockType(x, y, z + 1) == BlockType.BTWood)
			return 2; // ±z sides without bark
		return 3; // all sides with bark
	}

	private int getVinesOrientation(int x, int y, int z) {
		if (getBlockType(x - 1, y, z).isSolid())
			return Block.getOrientationFromVector(Vector.NX);
		if (getBlockType(x + 1, y, z).isSolid())
			return Block.getOrientationFromVector(Vector.X);
		if (getBlockType(x, y, z - 1).isSolid())
			return Block.getOrientationFromVector(Vector.NZ);
		if (getBlockType(x, y, z + 1).isSolid())
			return Block.getOrientationFromVector(Vector.Z);
		if (getBlockType(x, y + 1, z) == BlockType.BTVines)
			return getVinesOrientation(x, y + 1, z);
		return 0;
	}

	/**
	 * @param x
	 *            the block's x coordinate
	 * @param y
	 *            the block's y coordinate
	 * @param z
	 *            the block's z coordinate
	 * @return the block
	 */
	public Block getBlock(int x, int y, int z) {
		BlockType bt = getBlockType(x, y, z);
		if (bt == BlockType.BTEmpty)
			return null;
		if (bt == BlockType.BTWood)
			return Block.NewWood(this.treeType, getWoodOrientation(x, y, z));
		if (bt == BlockType.BTLeaves)
			return Block.NewLeaves(this.treeType, 0);
		if (bt == BlockType.BTVines)
			return Block.NewVines(getVinesOrientation(x, y, z));
		return bt.make(-1);
	}

	private boolean setBlockType(int x_in, int y, int z_in, BlockType bt) {
		int x = x_in, z = z_in;
		x += this.XZExtent;
		z += this.XZExtent;
		if (x < 0 || x >= this.XZExtent * 2 + 1 || y < 0
				|| y >= this.YExtent + 1 || z < 0 || z >= this.XZExtent * 2 + 1)
			return false;
		this.blocks[x + (this.XZExtent * 2 + 1) * (y + (this.YExtent + 1) * z)] = bt;
		return true;
	}

	private BlockType[] makeBlocksArray() {
		return new BlockType[(this.XZExtent * 2 + 1) * (this.YExtent + 1)
				* (this.XZExtent * 2 + 1)];
	}

	private void generateLeaves(int x, int y, int z) {
		if (this.treeType == TreeType.Jungle) {
			final int sz = 4;
			for (int dx = -sz; dx <= sz; dx++)
				for (int dy = 0; dy <= sz; dy++)
					for (int dz = -sz; dz <= sz; dz++)
						if (dx * dx + dy * dy * 2 * 2 + dz * dz < (sz + 1)
								* (sz + 1))
							if (getBlockType(x + dx, y + dy, z + dz) != BlockType.BTWood)
								setBlockType(x + dx, y + dy, z + dz,
										BlockType.BTLeaves);
			return;
		}
		final int sz = 1;
		for (int dx = -sz; dx <= sz; dx++)
			for (int dy = -sz; dy <= sz; dy++)
				for (int dz = -sz; dz <= sz; dz++)
					if (dx * dx + dy * dy + dz * dz < (sz + 1) * (sz + 1))
						if (getBlockType(x + dx, y + dy, z + dz) != BlockType.BTWood)
							setBlockType(x + dx, y + dy, z + dz,
									BlockType.BTLeaves);
	}

	private Vector generateTreeBranch_t1 = Vector.allocate();

	private void generateTreeBranch(int x_in, int y_in, int z_in, Random rand,
			int dx_in, int dz_in) {
		int x = x_in, y = y_in, z = z_in;
		int dx = dx_in, dz = dz_in;
		while (true) {
			if (getBlockType(x, y, z) == BlockType.BTWood)
				return;
			if (!setBlockType(x, y, z, BlockType.BTWood))
				return;
			generateLeaves(x, y, z);
			if (rand.nextInt(5) <= 2 || y >= this.YExtent - 1)
				return;
			dx += rand.nextInt(3) - 1;
			dz += rand.nextInt(3) - 1;
			int dir = Block.getOrientationFromVector(this.generateTreeBranch_t1
					.set(dx, 1.0f, dz));
			int ndx = Block.getOrientationDX(dir);
			int ndy = Block.getOrientationDY(dir);
			int ndz = Block.getOrientationDZ(dir);
			x += ndx;
			y += ndy;
			z += ndz;
		}
	}

	private void generateLargeBranchedTree(Random rand, boolean isBigTrunk) {
		for (int y = 0; y <= this.YExtent - 2; y++) {
			if (isBigTrunk) {
				if ((y >= this.YExtent / 3 && y >= 5 && rand.nextInt(8) == 0)
						|| y == this.YExtent - 2)
					generateTreeBranch(0, y, 0, rand, -1, -1);
				else
					setBlockType(0, y, 0, BlockType.BTWood);
				if ((y >= this.YExtent / 3 && y >= 5 && rand.nextInt(8) == 0)
						|| y == this.YExtent - 2)
					generateTreeBranch(1, y, 0, rand, 1, -1);
				else
					setBlockType(1, y, 0, BlockType.BTWood);
				if ((y >= this.YExtent / 3 && y >= 5 && rand.nextInt(8) == 0)
						|| y == this.YExtent - 2)
					generateTreeBranch(1, y, 1, rand, 1, 1);
				else
					setBlockType(1, y, 1, BlockType.BTWood);
				if ((y >= this.YExtent / 3 && y >= 5 && rand.nextInt(8) == 0)
						|| y == this.YExtent - 2)
					generateTreeBranch(0, y, 1, rand, -1, 1);
				else
					setBlockType(0, y, 1, BlockType.BTWood);
			} else if ((y >= this.YExtent / 3 && y >= 5 && rand.nextInt(3) == 0)
					|| y == this.YExtent - 2)
				generateTreeBranch(0, y, 0, rand, 0, 0);
			else
				setBlockType(0, y, 0, BlockType.BTWood);
		}
	}

	private void makeLeafRing(int y, int r) {
		for (int x = -r; x <= r; x++) {
			for (int z = -r; z <= r; z++) {
				if (x == 0 && z == 0)
					setBlockType(x, y, z, BlockType.BTWood);
				else if (x * x + z * z < (r + 0.25f) * (r + 0.25f))
					if (getBlockType(x, y, z) != BlockType.BTWood)
						setBlockType(x, y, z, BlockType.BTLeaves);
			}
		}
	}

	private void makeVines(Random rand) {
		for (int x = -this.XZExtent; x <= this.XZExtent; x++) {
			for (int y = 0; y <= this.YExtent; y++) {
				for (int z = -this.XZExtent; z <= this.XZExtent; z++) {
					if (getBlockType(x, y, z) != BlockType.BTEmpty)
						continue;
					if (rand.nextFloat() > 0.1f)
						continue;
					boolean doAdd = false;
					if (getBlockType(x + 1, y, z).isSolid())
						doAdd = true;
					else if (getBlockType(x - 1, y, z).isSolid())
						doAdd = true;
					else if (getBlockType(x, y, z + 1).isSolid())
						doAdd = true;
					else if (getBlockType(x, y, z - 1).isSolid())
						doAdd = true;
					if (doAdd) {
						int length = rand.nextInt(3) + 1;
						for (int dy = 0; dy > -length; dy--) {
							setBlockType(x, y + dy, z, BlockType.BTVines);
						}
					}
				}
			}
		}
	}

	/**
	 * @param treeType
	 *            the tree type
	 * @param t
	 *            the random parameter
	 */
	public Tree(TreeType treeType, float t) {
		this.treeType = treeType;
		Random rand = new Random((long) (t * Math.PI * 100000.0));
		switch (treeType) {
		case Birch:
			this.XZExtent = 2;
			this.YExtent = 6 + rand.nextInt(8 - 6 + 1);
			this.blocks = makeBlocksArray();
			for (int y = 0; y <= this.YExtent; y++) {
				switch (this.YExtent - y) {
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
					if (rand.nextBoolean())
						setBlockType(-1, y, -1, BlockType.BTLeaves);
					if (rand.nextBoolean())
						setBlockType(1, y, -1, BlockType.BTLeaves);
					if (rand.nextBoolean())
						setBlockType(1, y, 1, BlockType.BTLeaves);
					if (rand.nextBoolean())
						setBlockType(-1, y, 1, BlockType.BTLeaves);
					break;
				case 2:
				case 3:
					for (int x = -2; x <= 2; x++) {
						for (int z = -2; z <= 2; z++) {
							if (Math.abs(x) == 2 && Math.abs(z) == 2
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
		case Jungle:
			if (rand.nextFloat() >= 0.1) {
				this.XZExtent = 5;
				this.YExtent = 7 + rand.nextInt(10 - 7 + 1);
				this.blocks = makeBlocksArray();
				generateLargeBranchedTree(rand, false);
			} else {
				this.XZExtent = maxXZExtent;
				this.YExtent = maxYExtent;
				this.blocks = makeBlocksArray();
				generateLargeBranchedTree(rand, true);
			}
			makeVines(rand);
			break;
		case Spruce:
			switch (rand.nextInt(10)) {
			case 0: {
				this.XZExtent = 1;
				this.YExtent = 10 + rand.nextInt(13 - 10 + 1);
				this.blocks = makeBlocksArray();
				int leavesHeight = 2 + rand.nextInt(4);
				for (int y = 0; y <= this.YExtent; y++) {
					if (this.YExtent - y == 0) {
						setBlockType(0, y, 0, BlockType.BTLeaves);
						setBlockType(-1, y, 0, BlockType.BTLeaves);
						setBlockType(0, y, -1, BlockType.BTLeaves);
						setBlockType(1, y, 0, BlockType.BTLeaves);
						setBlockType(0, y, 1, BlockType.BTLeaves);
					} else if (this.YExtent - y < leavesHeight) {
						setBlockType(0, y, 0, BlockType.BTWood);
						setBlockType(-1, y, 0, BlockType.BTLeaves);
						setBlockType(0, y, -1, BlockType.BTLeaves);
						setBlockType(1, y, 0, BlockType.BTLeaves);
						setBlockType(0, y, 1, BlockType.BTLeaves);
					} else {
						setBlockType(0, y, 0, BlockType.BTWood);
					}
				}
				break;
			}
			case 1: {
				this.XZExtent = 5;
				this.YExtent = 10 + rand.nextInt(13 - 10 + 1);
				this.blocks = makeBlocksArray();
				int leavesHeight = this.YExtent - 2 - rand.nextInt(2);
				for (int y = 0; y <= this.YExtent; y++) {
					if (this.YExtent - y == 0) {
						setBlockType(0, y, 0, BlockType.BTLeaves);
						setBlockType(-1, y, 0, BlockType.BTLeaves);
						setBlockType(0, y, -1, BlockType.BTLeaves);
						setBlockType(1, y, 0, BlockType.BTLeaves);
						setBlockType(0, y, 1, BlockType.BTLeaves);
					} else if (this.YExtent - y < leavesHeight) {
						makeLeafRing(y, (this.YExtent - y) % 2 == 0 ? 1 : 2);
					} else {
						setBlockType(0, y, 0, BlockType.BTWood);
					}
				}
				break;
			}
			default: {
				this.XZExtent = 5;
				this.YExtent = 7 + rand.nextInt(9 - 7 + 1);
				this.blocks = makeBlocksArray();
				int leavesHeight = this.YExtent - 2 - rand.nextInt(2);
				for (int y = 0; y <= this.YExtent; y++) {
					if (this.YExtent - y == 0) {
						setBlockType(0, y, 0, BlockType.BTLeaves);
						setBlockType(-1, y, 0, BlockType.BTLeaves);
						setBlockType(0, y, -1, BlockType.BTLeaves);
						setBlockType(1, y, 0, BlockType.BTLeaves);
						setBlockType(0, y, 1, BlockType.BTLeaves);
					} else if (this.YExtent - y < leavesHeight) {
						makeLeafRing(y, ((this.YExtent - y) % 2 == 0) ? 1 : 2);
					} else {
						setBlockType(0, y, 0, BlockType.BTWood);
					}
				}
				break;
			}
			}
			break;
		case Oak:
		default: {
			if (rand.nextFloat() >= 0.1) {
				this.XZExtent = 3;
				this.YExtent = 7 + rand.nextInt(9 - 7 + 1);
				this.blocks = makeBlocksArray();
				for (int y = 0; y <= this.YExtent - 2; y++) {
					setBlockType(0, y, 0, BlockType.BTWood);
				}
				for (int y = this.YExtent - 4; y <= this.YExtent; y++) {
					int dy = y - (this.YExtent - 3);
					int sz = 4 - Math.abs(dy);
					for (int x = -sz; x <= sz; x++)
						for (int z = -sz; z <= sz; z++)
							if (getBlockType(x, y, z) == BlockType.BTEmpty)
								setBlockType(x, y, z, BlockType.BTLeaves);
				}
			} else {
				this.XZExtent = maxXZExtent;
				this.YExtent = 15;
				this.blocks = makeBlocksArray();
				generateLargeBranchedTree(rand, false);
			}
		}
		}
	}

	/**
	 * generate a tree
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
	 * @return if this tree generated
	 */
	public static boolean generate(Block type, int tx, int ty, int tz, float t) {
		Tree tree = new Tree(type.treeGetTreeType(), t);
		for (int dx = -tree.XZExtent; dx <= tree.XZExtent; dx++) {
			for (int dy = 0; dy <= tree.YExtent; dy++) {
				for (int dz = -tree.XZExtent; dz <= tree.XZExtent; dz++) {
					int x = tx + dx, y = ty + dy, z = tz + dz;
					Block b = world.getBlockEval(x, y, z);
					BlockType blockType = tree.getBlockType(dx, dy, dz);
					if (blockType == BlockType.BTEmpty)
						continue;
					if (b == null)
						return false;
					BlockType.Replaceability replaceability = b
							.getReplaceability(blockType);
					if (replaceability == BlockType.Replaceability.CanNotGrow)
						return false;
				}
			}
		}
		for (int dx = -tree.XZExtent; dx <= tree.XZExtent; dx++) {
			for (int dy = 0; dy <= tree.YExtent; dy++) {
				for (int dz = -tree.XZExtent; dz <= tree.XZExtent; dz++) {
					int x = tx + dx, y = ty + dy, z = tz + dz;
					Block b = world.getBlockEval(x, y, z);
					BlockType blockType = tree.getBlockType(dx, dy, dz);
					if (blockType == BlockType.BTEmpty)
						continue;
					if (b == null)
						return false;
					BlockType.Replaceability replaceability = b
							.getReplaceability(blockType);
					if (replaceability == BlockType.Replaceability.CanNotGrow)
						return false;
					if (replaceability == BlockType.Replaceability.Replace) {
						b = tree.getBlock(dx, dy, dz);
						if (b != null)
							world.setBlock(x, y, z, b);
					}
				}
			}
		}
		return true;
	}

	/**
	 * generate a tree with a random height
	 * 
	 * @param type
	 *            the sapling generating this tree
	 * @param tx
	 *            the tree's x coordinate
	 * @param ty
	 *            the tree's y coordinate
	 * @param tz
	 *            the tree's z coordinate
	 */
	public static void generate(Block type, int tx, int ty, int tz) {
		if (!generate(type, tx, ty, tz, World.fRand(0, 1)))
			world.setBlock(tx, ty, tz, type);
	}
}
