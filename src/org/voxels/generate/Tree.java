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

import org.voxels.*;

/**
 * @author jacob
 * 
 */
public final class Tree
{
	/**
	 * @author jacob
	 * 
	 */
	public static enum TreeBlockKind
	{
		/**
		 * 
		 */
		None, /**
		 * 
		 */
		Wood, /**
		 * 
		 */
		Leaves;
		/**
		 * @param a
		 *            first <code>TreeBlockKind</code> to combine
		 * @param b
		 *            second <code>TreeBlockKind</code> to combine
		 * @return the resulting <code>TreeBlockKind</code>
		 */
		public static TreeBlockKind combine(TreeBlockKind a, TreeBlockKind b)
		{
			if(a == None && b == None)
				return None;
			if(a == Wood || b == Wood)
				return Wood;
			return Leaves;
		}

		/**
		 * @param b
		 *            the original block
		 * @param tbk
		 *            the <code>TreeBlockKind</code>
		 * @return the resulting block
		 */
		public static Block combine(Block b, TreeBlockKind tbk)
		{
			if(tbk == None)
				return b;
			if(tbk == Leaves)
			{
				if(b.getType() == BlockType.BTWood
				        || b.getType() == BlockType.BTLeaves)
					return b;
				return Block.NewLeaves();
			}
			if(b.getType() == BlockType.BTWood)
				return b;
			return Block.NewWood();
		}
	}

	private static final float leavesScaleFactor = 0.7f;
	private static final int treeHeightMin = 5;
	private static final int treeHeightMax = 9;

	/**
	 * @param t
	 *            the random input. must be 0 &lt; <code>t</code> &lt; 1
	 * @return the resulting tree height
	 */
	public static int getTreeHeight(float t)
	{
		float retval = treeHeightMin + (treeHeightMax - treeHeightMin)
		        * Math.max(0, Math.min(1, t));
		return Math.round(retval);
	}

	private Tree()
	{
	}

	/**
	 * @param treeHeight
	 *            the tree height
	 * @return the tree's minimum x coordinate
	 */
	public static int getTreeMinX(int treeHeight)
	{
		return -Math.round(leavesScaleFactor * treeHeight);
	}

	/**
	 * @param treeHeight
	 *            the tree height
	 * @return the tree's maximum x coordinate
	 */
	public static int getTreeMaxX(int treeHeight)
	{
		return Math.round(leavesScaleFactor * treeHeight);
	}

	/**
	 * @param treeHeight
	 *            the tree height
	 * @return the tree's minimum y coordinate
	 */
	public static int getTreeMinY(int treeHeight)
	{
		return 0;
	}

	/**
	 * @param treeHeight
	 *            the tree height
	 * @return the tree's maximum y coordinate
	 */
	public static int getTreeMaxY(int treeHeight)
	{
		return Math.round(leavesScaleFactor * treeHeight) + treeHeight;
	}

	/**
	 * @param treeHeight
	 *            the tree height
	 * @return the tree's minimum z coordinate
	 */
	public static int getTreeMinZ(int treeHeight)
	{
		return -Math.round(leavesScaleFactor * treeHeight);
	}

	/**
	 * @param treeHeight
	 *            the tree height
	 * @return the tree's maximum z coordinate
	 */
	public static int getTreeMaxZ(int treeHeight)
	{
		return Math.round(leavesScaleFactor * treeHeight);
	}

	/**
	 * the maximum coordinate value
	 */
	public static final int maximumExtent = getTreeMaxY(treeHeightMax);

	/**
	 * @param dx
	 *            x coordinate
	 * @param dy
	 *            y coordinate
	 * @param dz
	 *            z coordinate
	 * @param treeHeight
	 *            tree height
	 * @return the <code>TreeBlockKind</code>
	 */
	public static TreeBlockKind getTreeShape(int dx,
	                                         int dy,
	                                         int dz,
	                                         int treeHeight)
	{
		int treeLeavesSize = Math.round(leavesScaleFactor * treeHeight);
		if(dx == 0 && dz == 0 && dy >= 0 && dy <= treeHeight + treeLeavesSize)
			return TreeBlockKind.Wood;
		int yp = dy - treeHeight - treeLeavesSize;
		if(dx * dx + yp * yp + dz * dz < treeLeavesSize * treeLeavesSize)
			return TreeBlockKind.Leaves;
		return TreeBlockKind.None;
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
	 * @param treeHeight
	 *            the tree's height
	 */
	public static void generate(Block type,
	                            int tx,
	                            int ty,
	                            int tz,
	                            int treeHeight)
	{
		for(int dx = getTreeMinX(treeHeight); dx <= getTreeMaxX(treeHeight); dx++)
		{
			for(int dy = getTreeMinY(treeHeight); dy <= getTreeMaxY(treeHeight); dy++)
			{
				for(int dz = getTreeMinZ(treeHeight); dz <= getTreeMaxZ(treeHeight); dz++)
				{
					int x = tx + dx, y = ty + dy, z = tz + dz;
					Block b = world.getBlockEval(x, y, z);
					if(b == null)
						continue;
					b = TreeBlockKind.combine(b,
					                          getTreeShape(dx,
					                                       dy,
					                                       dz,
					                                       treeHeight));
					world.setBlock(x, y, z, b);
				}
			}
		}
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
	public static void generate(Block type, int tx, int ty, int tz)
	{
		generate(type, tx, ty, tz, getTreeHeight(World.fRand(0, 1)));
	}
}
