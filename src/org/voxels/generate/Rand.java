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

import static org.voxels.PlayerList.players;

import java.util.Random;

import org.voxels.*;

/**
 * Land generator
 * 
 * @author jacob
 */
public class Rand
{
	private final int seed;

	private Rand(int seed)
	{
		this.seed = seed;
	}

	private Rand()
	{
		this.seed = new Random().nextInt();
	}

	/**
	 * @return new land generator
	 */
	public static Rand create()
	{
		Rand retval = null;
		int rockHeight;
		do
		{
			retval = new Rand();
			rockHeight = retval.getRockHeight(0, 0);
		}
		while(rockHeight < WaterHeight || retval.getHasTree(0, 0)
		        || retval.isInCave(0, rockHeight, 0));
		return retval;
	}

	/**
	 * @param seed
	 *            seed for land generator
	 * @return new land generator
	 */
	public static Rand create(int seed)
	{
		return new Rand(seed);
	}

	/**
	 * @return this land generator's seed
	 */
	public int getSeed()
	{
		return this.seed;
	}

	private static final int hashPrime = 99991;

	private static enum RandClass
	{
		RockHeight,
		LakeBedType,
		GenTree,
		TreeSize,
		Lava,
		OreType,
		Cave,
		CaveDecoration,
		CaveDecorationChest,
		Vect/* Vect must be last */
	}

	private static class Node
	{
		public int x, y, z, rc;
		public float value;

		public Node()
		{
		}
	}

	private Node[] hashTable = new Node[hashPrime];

	private int genHash(int x, int y, int z, int rc)
	{
		long retval = x + 9L * (y + 9L * (z + 9L * rc));
		retval %= hashPrime;
		if(retval < 0)
			retval += hashPrime;
		return (int)retval;
	}

	private synchronized float genRand(int x, int y, int z, int w)
	{
		final long mask = (1L << 48) - 1;
		final long multiplier = 0x5DEECE66DL;
		int hash = genHash(x, y, z, w);
		if(this.hashTable[hash] != null && this.hashTable[hash].x == x
		        && this.hashTable[hash].y == y && this.hashTable[hash].z == z
		        && this.hashTable[hash].rc == w)
			return this.hashTable[hash].value;
		long randv = x * 12345;
		randv &= mask;
		randv = 12345 * randv + y;
		randv &= mask;
		randv = 12345 * randv + z;
		randv &= mask;
		randv = 12345 * randv + w;
		randv &= mask;
		randv = 12345 * randv + this.seed;
		randv &= mask;
		randv = (randv ^ multiplier) & mask;
		for(int i = 0; i < 10; i++)
		{
			randv *= multiplier;
			randv += 0xB;
			randv &= mask;
		}
		float retval = randv * (1.0f / (mask + 1));
		if(this.hashTable[hash] == null)
			this.hashTable[hash] = new Node();
		this.hashTable[hash].x = x;
		this.hashTable[hash].y = y;
		this.hashTable[hash].z = z;
		this.hashTable[hash].rc = w;
		this.hashTable[hash].value = retval;
		return retval;
	}

	private float genRand(int x, int y, int z, RandClass rc)
	{
		return genRand(x, y, z, rc.ordinal());
	}

	private Vector genRandV(int x, int y, int z)
	{
		Vector retval = new Vector();
		int w = RandClass.Vect.ordinal();
		do
		{
			retval.x = genRand(x, y, z, w++) * 2.0f - 1.0f;
			retval.y = genRand(x, y, z, w++) * 2.0f - 1.0f;
			retval.z = genRand(x, y, z, w++) * 2.0f - 1.0f;
		}
		while(retval.abs() > 1.0f || retval.abs() < 0.0001f);
		return retval;
	}

	@SuppressWarnings("unused")
	private float getNoise(float x, float y, float z, RandClass rc)
	{
		int xmin = (int)Math.floor(x);
		int xmax = xmin + 1;
		int ymin = (int)Math.floor(y);
		int ymax = ymin + 1;
		int zmin = (int)Math.floor(z);
		int zmax = zmin + 1;
		float v000 = genRand(xmin, ymin, zmin, rc);
		float v100 = genRand(xmax, ymin, zmin, rc);
		float v010 = genRand(xmin, ymax, zmin, rc);
		float v110 = genRand(xmax, ymax, zmin, rc);
		float v001 = genRand(xmin, ymin, zmax, rc);
		float v101 = genRand(xmax, ymin, zmax, rc);
		float v011 = genRand(xmin, ymax, zmax, rc);
		float v111 = genRand(xmax, ymax, zmax, rc);
		float fx = x - xmin;
		float fy = y - ymin;
		float fz = z - zmin;
		float nfx = 1 - fx;
		float nfy = 1 - fy;
		float nfz = 1 - fz;
		float v00 = v000 * nfz + v001 * fz;
		float v10 = v100 * nfz + v101 * fz;
		float v01 = v010 * nfz + v011 * fz;
		float v11 = v110 * nfz + v111 * fz;
		float v0 = v00 * nfy + v01 * fy;
		float v1 = v10 * nfy + v11 * fy;
		return v0 * nfx + v1 * fx;
	}

	private float getRockHeightNoiseH(float x, float z, int i)
	{
		int xmin = (int)Math.floor(x);
		int xmax = xmin + 1;
		int zmin = (int)Math.floor(z);
		int zmax = zmin + 1;
		float y00 = genRand(xmin, i, zmin, RandClass.RockHeight);
		float y10 = genRand(xmax, i, zmin, RandClass.RockHeight);
		float y01 = genRand(xmin, i, zmax, RandClass.RockHeight);
		float y11 = genRand(xmax, i, zmax, RandClass.RockHeight);
		float fx = x - xmin;
		float fz = z - zmin;
		float nfx = 1 - fx;
		float nfz = 1 - fz;
		float y0 = y00 * nfz + y01 * fz;
		float y1 = y10 * nfz + y11 * fz;
		return y0 * nfx + y1 * fx;
	}

	@SuppressWarnings("unused")
	private float getRockHeightNoise(int x, int z)
	{
		float retval = 0.0f;
		float frequency = 0.02f;
		float amplitude = 1.0f;
		float max = 0.0f;
		float roughness;
		if(true)
		{
			roughness = getRockHeightNoiseH(x / 64.0f, z / 64.0f, -1);
		}
		else
		{
			roughness = (((x >>> 6) ^ (z >>> 6)) & 1);
		}
		roughness = 1.15f + 0.4f * roughness;
		for(int i = 0; amplitude >= 0.01f; i++)
		{
			retval += getRockHeightNoiseH(x * frequency, z * frequency, i)
			        * amplitude;
			max += amplitude;
			frequency *= roughness;
			frequency = Math.min(frequency, 1.0f);
			amplitude /= roughness;
		}
		retval /= max;
		retval -= 0.5f;
		retval *= 2;
		retval += 0.5f;
		return retval;
	}

	private static class RockChunk
	{
		public int cx, cz;
		public final static int size = 4;
		private int y[] = new int[size * size];

		public RockChunk()
		{
		}

		public int getY(int x, int z)
		{
			return this.y[x + size * z];
		}

		public void setY(int x, int z, int newY)
		{
			this.y[x + size * z] = newY;
		}
	}

	private RockChunk rockChunkHashTable[] = new RockChunk[hashPrime];

	private int internalGetRockHeight(int x, int z)
	{
		final int maxY = World.Height - 1, minY = -World.Depth;
		final boolean USE_NEW_METHOD = true;
		float retval;
		if(!USE_NEW_METHOD)
		{
			final int size = 32;
			int xmin = x - x % size;
			if(xmin > x)
				xmin -= size;
			int xmax = xmin + size;
			int zmin = z - z % size;
			if(zmin > z)
				zmin -= size;
			int zmax = zmin + size;
			float y00 = genRand(xmin, 0, zmin, RandClass.RockHeight);
			float y10 = genRand(xmax, 0, zmin, RandClass.RockHeight);
			float y01 = genRand(xmin, 0, zmax, RandClass.RockHeight);
			float y11 = genRand(xmax, 0, zmax, RandClass.RockHeight);
			float fx = (float)(x - xmin) / size;
			float fz = (float)(z - zmin) / size;
			float nfx = 1 - fx;
			float nfz = 1 - fz;
			float y0 = y00 * nfz + y01 * fz;
			float y1 = y10 * nfz + y11 * fz;
			float y = y0 * nfx + y1 * fx;
			retval = y;
		}
		else
		{
			retval = getRockHeightNoise(x, z);
		}
		retval = retval * (maxY - minY) + minY;
		retval /= 4;
		retval = (float)Math.floor(retval + 0.5f);
		if(retval > maxY)
			retval = maxY;
		else if(retval < minY)
			retval = minY;
		return (int)retval;
	}

	private synchronized RockChunk getRockChunk(int cx, int cz)
	{
		int hash = getChunkHash(cx, cz);
		RockChunk retval = this.rockChunkHashTable[hash];
		if(retval == null || retval.cx != cx || retval.cz != cz)
		{
			retval = new RockChunk();
			this.rockChunkHashTable[hash] = retval;
			retval.cx = cx;
			retval.cz = cz;
			for(int x = 0; x < RockChunk.size; x++)
			{
				for(int z = 0; z < RockChunk.size; z++)
				{
					retval.setY(x, z, internalGetRockHeight(x + cx, z + cz));
				}
			}
		}
		return retval;
	}

	/**
	 * @param x
	 *            x coordinate
	 * @param z
	 *            z coordinate
	 * @return height of land
	 */
	public synchronized int getRockHeight(int x, int z)
	{
		int cx = x - (x % RockChunk.size + RockChunk.size) % RockChunk.size;
		int cz = z - (z % RockChunk.size + RockChunk.size) % RockChunk.size;
		RockChunk c = getRockChunk(cx, cz);
		return c.getY(x - cx, z - cz);
	}

	/***/
	public static final int WaterHeight = 0;

	private boolean waterInArea(int x, int y, int z)
	{
		final int dist = 3;
		if(y > WaterHeight)
			return false;
		for(int dx = -dist; dx <= dist; dx++)
		{
			for(int dz = -dist; dz <= dist; dz++)
			{
				int RockHeight = getRockHeight(x + dx, z + dz);
				if(RockHeight >= WaterHeight)
					continue;
				if(y <= RockHeight - dist)
					continue;
				return true;
			}
		}
		return false;
	}

	private class LavaNode
	{
		public static final int size = 16;
		public static final int minLakeSize = 10;
		public static final int maxLakeSize = 20;
		public static final int maxHeight = 10 - World.Depth;
		public static final int minHeight = 1 - World.Depth;
		private int lakeSize[] = new int[size * size];
		public int cx, cz;

		public void setLakeSize(int x, int z, int v)
		{
			this.lakeSize[x + size * z] = v;
		}

		public int getLakeSize(int x, int z)
		{
			return this.lakeSize[x + size * z];
		}

		public LavaNode(int cx, int cz)
		{
			this.cx = cx;
			this.cz = cz;
		}
	}

	private LavaNode makeLavaNode(int cx, int cz)
	{
		LavaNode retval = new LavaNode(cx, cz);
		final float lakeProbability = 0.05f
		        / (LavaNode.maxLakeSize * LavaNode.maxLakeSize) * 16f
		        / (float)Math.PI;
		for(int x = 0; x < LavaNode.size; x++)
		{
			for(int z = 0; z < LavaNode.size; z++)
			{
				int v = 0;
				if(genRand(cx + x, 0, cz + z, RandClass.Lava) < lakeProbability)
					v = (int)Math.floor(genRand(cx + x,
					                            1,
					                            cz + z,
					                            RandClass.Lava)
					        * (LavaNode.maxLakeSize - LavaNode.minLakeSize)
					        + LavaNode.minLakeSize);
				retval.setLakeSize(x, z, v);
			}
		}
		return retval;
	}

	private LavaNode[] lavaNodeHashTable = new LavaNode[hashPrime];

	private synchronized LavaNode getLavaNode(int cx, int cz)
	{
		int hash = getChunkHash(cx, cz);
		LavaNode node = this.lavaNodeHashTable[hash];
		if(node == null || node.cx != cx || node.cz != cz)
		{
			node = makeLavaNode(cx, cz);
			this.lavaNodeHashTable[hash] = node;
		}
		return node;
	}

	private int getLavaLakeSize(int x, int z)
	{
		int cx = x - (x % LavaNode.size + LavaNode.size) % LavaNode.size;
		int cz = z - (z % LavaNode.size + LavaNode.size) % LavaNode.size;
		return getLavaNode(cx, cz).getLakeSize(x - cx, z - cz);
	}

	private int getLavaLakeHeight(int x, int z)
	{
		float t = genRand(x, 2, z, RandClass.Lava);
		float v = LavaNode.minHeight + t
		        * (LavaNode.maxHeight - LavaNode.minHeight);
		return (int)Math.floor(v);
	}

	private boolean isLava(int x, int y, int z)
	{
		if(y < LavaNode.minHeight || y > LavaNode.maxHeight)
			return false;
		for(int dx = -LavaNode.maxLakeSize; dx <= LavaNode.maxLakeSize; dx++)
		{
			for(int dz = -LavaNode.maxLakeSize; dz <= LavaNode.maxLakeSize; dz++)
			{
				int lakeSize = getLavaLakeSize(x + dx, z + dz);
				if(lakeSize <= 0)
					continue;
				int dy = y - getLavaLakeHeight(x + dx, z + dz);
				if(dy > 0)
					continue;
				if(dx * dx + dy * dy * 3 * 3 + dz * dz < lakeSize * lakeSize)
					return true;
			}
		}
		return false;
	}

	private boolean isOverLava(int x, int z)
	{
		for(int dx = -LavaNode.maxLakeSize; dx <= LavaNode.maxLakeSize; dx++)
		{
			for(int dz = -LavaNode.maxLakeSize; dz <= LavaNode.maxLakeSize; dz++)
			{
				int lakeSize = getLavaLakeSize(x + dx, z + dz);
				if(lakeSize <= 0)
					continue;
				if(dx * dx + dz * dz < lakeSize * lakeSize)
					return true;
			}
		}
		return false;
	}

	private enum CaveType
	{
		None, Sphere, Cylinder, Cylinder2, Cylinder3, Cylinder4, Last;
		public static final int Count = Last.ordinal();
	}

	private static class CaveChunk
	{
		public static final int size = 4;
		public int cx, cz;
		private CaveType caves[] = new CaveType[size * size];
		private int y[] = new int[size * size];
		private int r[] = new int[size * size];
		private Vector dir[] = new Vector[size * size];

		public CaveType getCave(int x, int z)
		{
			return this.caves[x + size * z];
		}

		public int getY(int x, int z)
		{
			return this.y[x + size * z];
		}

		public int getR(int x, int z)
		{
			return this.r[x + size * z];
		}

		public Vector getDir(int x, int z)
		{
			return this.dir[x + size * z];
		}

		public void setCave(int x, int z, CaveType c)
		{
			this.caves[x + size * z] = c;
		}

		public void setY(int x, int z, int y)
		{
			this.y[x + size * z] = y;
		}

		public void setR(int x, int z, int r)
		{
			this.r[x + size * z] = r;
		}

		public void setDir(int x, int z, Vector dir)
		{
			this.dir[x + size * z] = new Vector(dir);
		}

		public CaveChunk()
		{
		}
	}

	private CaveChunk caveChunkHashTable[] = new CaveChunk[hashPrime];
	private static final int caveMaxSize = 80;

	void fillCaveChunk(CaveChunk cc)
	{
		final float caveProb = 2.0f;
		for(int x = 0; x < CaveChunk.size; x++)
		{
			for(int z = 0; z < CaveChunk.size; z++)
			{
				float fv = genRand(x + cc.cx, 0, z + cc.cz, RandClass.Cave)
				        * caveMaxSize * caveMaxSize / caveProb
				        * (CaveType.Count - 1);
				if(fv > CaveType.Count)
					fv = CaveType.Count;
				int v = (int)Math.floor(fv);
				v++;
				if(v >= CaveType.Count)
					cc.setCave(x, z, CaveType.None);
				else
					cc.setCave(x, z, CaveType.values()[v]);
				if(cc.getCave(x, z) == CaveType.None)
					continue;
				int rockHeight = getRockHeight(x + cc.cx, z + cc.cz);
				float y = genRand(x + cc.cx, 100000, z + cc.cz, RandClass.Cave);
				cc.setY(x,
				        z,
				        (int)Math.floor(y * (rockHeight + World.Depth)
				                - World.Depth));
				cc.setR(x,
				        z,
				        (int)Math.floor(genRand(x + cc.cx,
				                                200000,
				                                z + cc.cz,
				                                RandClass.Cave) * caveMaxSize));
				cc.setDir(x, z, genRandV(x, 0, z));
			}
		}
	}

	synchronized CaveChunk getCaveChunk(int cx, int cz)
	{
		int hash = getChunkHash(cx, cz);
		CaveChunk node = this.caveChunkHashTable[hash];
		if(node != null && node.cx == cx && node.cz == cz)
		{
			return node;
		}
		node = new CaveChunk();
		node.cx = cx;
		node.cz = cz;
		this.caveChunkHashTable[hash] = node;
		fillCaveChunk(node);
		return node;
	}

	private class InCaveChunk
	{
		public static final int size = 4;
		private boolean v[] = new boolean[size * size * size];
		public int cx, cy, cz;

		private void setInCave(int x, int y, int z, boolean v)
		{
			this.v[x + size * (y + size * z)] = v;
		}

		public InCaveChunk(int cx, int cy, int cz)
		{
			this.cx = cx;
			this.cy = cy;
			this.cz = cz;
			for(int cdx = -caveMaxSize; cdx <= caveMaxSize + size; cdx++)
			{
				for(int cdz = -caveMaxSize; cdz <= caveMaxSize + size; cdz++)
				{
					int px = cx + cdx, pz = cz + cdz;
					CaveChunk cc = getCaveChunk(px
					        - (px % CaveChunk.size + CaveChunk.size)
					        % CaveChunk.size, pz
					        - (pz % CaveChunk.size + CaveChunk.size)
					        % CaveChunk.size);
					int ccx = px - cc.cx;
					int ccz = pz - cc.cz;
					CaveType type = cc.getCave(ccx, ccz);
					if(type == CaveType.None)
						continue;
					for(int x = cx; x < cx + size; x++)
					{
						for(int y = cy; y < cy + size; y++)
						{
							for(int z = cz; z < cz + size; z++)
							{
								boolean newValue = isInCave(x - cx, y - cy, z
								        - cz);
								int dx = x - px;
								int dz = z - pz;
								int dy = y - cc.getY(ccx, ccz);
								int r = cc.getR(ccx, ccz);
								Vector dir = cc.getDir(ccx, ccz);
								switch(type)
								{
								case Last:
								case None:
									break;
								case Sphere:
								{
									r /= 4;
									if(dx * dx + dy * dy + dz * dz < r * r)
										newValue = true;
									break;
								}
								case Cylinder:
								case Cylinder2:
								case Cylinder3:
								case Cylinder4:
								{
									dir.y *= 4;
									dir.y = Math.round(dir.y);
									dir.y /= 16;
									dir = dir.normalize();
									Vector p = dir.mul(0.95f * dir.dot(new Vector(dx,
									                                              dy,
									                                              dz)))
									              .sub(new Vector(dx, dy, dz));
									if(p.abs() < r / 16.0f)
										newValue = true;
									break;
								}
								}
								setInCave(x - cx, y - cy, z - cz, newValue);
							}
						}
					}
				}
			}
		}

		public boolean isInCave(int x, int y, int z)
		{
			return this.v[x + size * (y + size * z)];
		}
	}

	private InCaveChunk inCaveChunkHashTable[] = new InCaveChunk[hashPrime];

	private synchronized InCaveChunk getInCaveChunk(int cx, int cy, int cz)
	{
		int hash = genHash(cx, cy, cz, 0);
		InCaveChunk node = this.inCaveChunkHashTable[hash];
		if(node != null && node.cx == cx && node.cy == cy && node.cz == cz)
		{
			return node;
		}
		node = new InCaveChunk(cx, cy, cz);
		this.inCaveChunkHashTable[hash] = node;
		return node;
	}

	private synchronized boolean isInCave(int x, int y, int z)
	{
		int cx = x - (x % InCaveChunk.size + InCaveChunk.size)
		        % InCaveChunk.size;
		int cy = y - (y % InCaveChunk.size + InCaveChunk.size)
		        % InCaveChunk.size;
		int cz = z - (z % InCaveChunk.size + InCaveChunk.size)
		        % InCaveChunk.size;
		InCaveChunk c = getInCaveChunk(cx, cy, cz);
		boolean retval = c.isInCave(x - cx, y - cy, z - cz);
		return retval;
	}

	private static class TreeChunk
	{
		public static final int size = 4;
		public int cx, cz;
		private boolean hasTree[] = new boolean[size * size];

		public TreeChunk(int cx, int cz)
		{
			this.cx = cx;
			this.cz = cz;
		}

		public boolean getHasTree(int x, int z)
		{
			return this.hasTree[x + z * size];
		}

		public void setHasTree(int x, int z, boolean v)
		{
			this.hasTree[x + z * size] = v;
		}
	}

	private static final float makeTreeProb = 0.01f;

	private TreeChunk makeTreeChunk(int cx, int cz)
	{
		TreeChunk tc = new TreeChunk(cx, cz);
		for(int x = 0; x < TreeChunk.size; x++)
		{
			for(int z = 0; z < TreeChunk.size; z++)
			{
				tc.setHasTree(x, z, false);
				int RockHeight = getRockHeight(x + cx, z + cz);
				if(RockHeight > WaterHeight)
					if(genRand(x + cx, 0, z + cz, RandClass.GenTree) < makeTreeProb
					        && !isInCave(x + cx, RockHeight, z + cz))
						tc.setHasTree(x, z, true);
			}
		}
		return tc;
	}

	private static int getChunkHash(int cx, int cz)
	{
		int retval = (int)((cx + 3L * cz) % hashPrime);
		if(retval < 0)
			retval += hashPrime;
		return retval;
	}

	private TreeChunk[] treeChunkHashTable = new TreeChunk[hashPrime];

	private synchronized boolean getHasTree(int x, int z)
	{
		int cx = x - (x % TreeChunk.size + TreeChunk.size) % TreeChunk.size;
		int cz = z - (z % TreeChunk.size + TreeChunk.size) % TreeChunk.size;
		int hash = getChunkHash(cx, cz);
		if(this.treeChunkHashTable[hash] == null
		        || this.treeChunkHashTable[hash].cx != cx
		        || this.treeChunkHashTable[hash].cz != cz)
			this.treeChunkHashTable[hash] = makeTreeChunk(cx, cz);
		return this.treeChunkHashTable[hash].getHasTree(x - cx, z - cz);
	}

	@SuppressWarnings("unused")
	private synchronized Tree.TreeBlockKind internalGetTreeBlockKind(int x,
	                                                                 int y,
	                                                                 int z)
	{
		final int searchdist = Tree.maximumExtent;
		Tree.TreeBlockKind retval = Tree.TreeBlockKind.None;
		for(int dx = -searchdist; dx <= searchdist; dx++)
		{
			for(int dz = -searchdist; dz <= searchdist; dz++)
			{
				int cx = dx + x, cz = dz + z;
				if(getHasTree(cx, cz))
				{
					int rockHeight = getRockHeight(cx, cz);
					float treeHeightT = genRand(cx, 0, cz, RandClass.TreeSize);
					int treeHeight = Tree.getTreeHeight(treeHeightT);
					retval = Tree.TreeBlockKind.combine(retval,
					                                    Tree.getTreeShape(-dx,
					                                                      y
					                                                              - rockHeight,
					                                                      -dz,
					                                                      treeHeight));
					if(retval == Tree.TreeBlockKind.Wood)
						return retval;
				}
			}
		}
		return retval;
	}

	private static class TreeBlockKindChunk
	{
		public static final int size = 4;
		public int cx, cy, cz;
		private Tree.TreeBlockKind v[] = new Tree.TreeBlockKind[size * size
		        * size];

		public TreeBlockKindChunk(int cx, int cy, int cz)
		{
			this.cx = cx;
			this.cy = cy;
			this.cz = cz;
		}

		public Tree.TreeBlockKind get(int x_in, int y_in, int z_in)
		{
			int x = x_in - this.cx;
			int y = y_in - this.cy;
			int z = z_in - this.cz;
			Tree.TreeBlockKind retval = this.v[x + size * (y + size * z)];
			if(retval == null)
				return Tree.TreeBlockKind.None;
			return retval;
		}

		public void put(int x_in, int y_in, int z_in, Tree.TreeBlockKind v)
		{
			int x = x_in - this.cx;
			int y = y_in - this.cy;
			int z = z_in - this.cz;
			this.v[x + size * (y + size * z)] = v;
		}
	}

	private TreeBlockKindChunk treeBlockKindChunkHashTable[] = new TreeBlockKindChunk[hashPrime];

	private synchronized void fillTreeBlockKindChunk(TreeBlockKindChunk c)
	{
		final int searchdist = Tree.maximumExtent;
		for(int dx = -searchdist; dx <= searchdist + TreeBlockKindChunk.size; dx++)
		{
			for(int dz = -searchdist; dz <= searchdist
			        + TreeBlockKindChunk.size; dz++)
			{
				int cx = dx + c.cx, cz = dz + c.cz;
				if(getHasTree(cx, cz))
				{
					int rockHeight = getRockHeight(cx, cz);
					float treeHeightT = genRand(cx, 0, cz, RandClass.TreeSize);
					int treeHeight = Tree.getTreeHeight(treeHeightT);
					for(int x = c.cx; x < c.cx + TreeBlockKindChunk.size; x++)
					{
						for(int y = c.cy; y < c.cy + TreeBlockKindChunk.size; y++)
						{
							for(int z = c.cz; z < c.cz
							        + TreeBlockKindChunk.size; z++)
							{
								c.put(x,
								      y,
								      z,
								      Tree.TreeBlockKind.combine(c.get(x, y, z),
								                                 Tree.getTreeShape(x
								                                                           - cx,
								                                                   y
								                                                           - rockHeight,
								                                                   z
								                                                           - cz,
								                                                   treeHeight)));
							}
						}
					}
				}
			}
		}
	}

	private synchronized TreeBlockKindChunk getTreeBlockKindChunk(int cx,
	                                                              int cy,
	                                                              int cz)
	{
		int hash = genHash(cx, cy, cz, 0);
		TreeBlockKindChunk node = this.treeBlockKindChunkHashTable[hash];
		if(node != null && node.cx == cx && node.cy == cy && node.cz == cz)
		{
			return node;
		}
		node = new TreeBlockKindChunk(cx, cy, cz);
		fillTreeBlockKindChunk(node);
		this.treeBlockKindChunkHashTable[hash] = node;
		return node;
	}

	private synchronized Tree.TreeBlockKind
	    getTreeBlockKind(int x, int y, int z)
	{
		int cx = x - (x % TreeBlockKindChunk.size + TreeBlockKindChunk.size)
		        % TreeBlockKindChunk.size;
		int cy = y - (y % TreeBlockKindChunk.size + TreeBlockKindChunk.size)
		        % TreeBlockKindChunk.size;
		int cz = z - (z % TreeBlockKindChunk.size + TreeBlockKindChunk.size)
		        % TreeBlockKindChunk.size;
		TreeBlockKindChunk c = getTreeBlockKindChunk(cx, cy, cz);
		return c.get(x, y, z);
	}

	enum DecorationType
	{
		Torch, Chest, Last;
		public static final int count = Last.ordinal();
	}

	private Block getCaveDecoration(int x, int y, int z)
	{
		final float genBlockProb = 0.1f;
		float ftype = genRand(x, y, z, RandClass.CaveDecoration)
		        * DecorationType.count / genBlockProb;
		int type = (int)Math.floor(ftype);
		if(type >= DecorationType.count)
			return new Block();
		switch(DecorationType.values()[type])
		{
		case Torch:
		{
			if(isInCave(x, y - 1, z))
			{
				return new Block();
			}
			return Block.NewTorch(4);
		}
		case Chest:
		{
			if(isInCave(x, y - 1, z))
				return new Block();
			if(ftype - type > 0.25)
				return new Block();
			Block retval = Block.NewChest();
			for(int i = 1; i < BlockType.Count; i++)
			{
				BlockType bt = BlockType.toBlockType(i);
				int count = (int)Math.floor(genRand(x,
				                                    i,
				                                    z,
				                                    RandClass.CaveDecorationChest)
				        * (bt.getChestGenCount(y) + 1));
				for(int j = 0; j < count; j++)
					retval.chestAddBlock(bt);
			}
			return retval;
		}
		case Last:
		default:
			return new Block();
		}
	}

	private static boolean didSetPos = false;

	/**
	 * generates a chunk
	 * 
	 * @param cx
	 *            chunk x coordinate
	 * @param cy
	 *            chunk y coordinate
	 * @param cz
	 *            chunk z coordinate
	 * @param chunkSize
	 *            chunk size
	 * @return the generated chunk
	 */
	@SuppressWarnings("unused")
	public GeneratedChunk genChunk(int cx, int cy, int cz, int chunkSize)
	{
		GeneratedChunk generatedChunk = new GeneratedChunk(chunkSize,
		                                                   cx,
		                                                   cy,
		                                                   cz);
		if(false)
		{
			if(!didSetPos)
			{
				players.front().setPosition(new Vector(0.5f, 1.0f, 0.5f));
				didSetPos = true;
			}
			for(int x = cx; x < cx + chunkSize; x++)
			{
				for(int y = cy; y < cy + chunkSize; y++)
				{
					for(int z = cz; z < cz + chunkSize; z++)
					{
						if(x * x + y * y + z * z >= 5 * 5 && (x != 0 || z != 0))
							generatedChunk.setBlock(x, y, z, Block.NewStone());
						else if(x * x / 2 + y * y + z * z / 2 < 1 * 1
						        && (x != 0 || z != 0 || true))
							generatedChunk.setBlock(x,
							                        y,
							                        z,
							                        Block.NewWater(4 + x * z
							                                * 3));
						else
							generatedChunk.setBlock(x, y, z, new Block());
					}
				}
			}
			return generatedChunk;
		}
		for(int x = cx; x < cx + chunkSize; x++)
		{
			for(int z = cz; z < cz + chunkSize; z++)
			{
				int rockHeight = getRockHeight(x, z);
				for(int y = cy; y < cy + chunkSize; y++)
				{
					Block block = null;
					if(y == -World.Depth)
						block = Block.NewBedrock();
					else if(isInCave(x, y, z))
					{
						Tree.TreeBlockKind tbk = getTreeBlockKind(x, y, z);
						switch(tbk)
						{
						case Leaves:
							block = Block.NewLeaves();
							break;
						case None:
							block = getCaveDecoration(x, y, z);
							break;
						case Wood:
							block = Block.NewWood();
							break;
						}
					}
					else if(isLava(x, y, z))
						block = Block.NewStationaryLava();
					else if(isOverLava(x, z) && y <= 5 + LavaNode.maxHeight)
						block = new Block();
					else if(y <= rockHeight && waterInArea(x, y, z))
					{
						if(isInCave(x, y - 1, z))
							block = Block.NewStone();
						else if(genRand(x, y, z, RandClass.LakeBedType) >= 0.5f)
							block = Block.NewSand();
						else
							block = Block.NewGravel();
					}
					else if(y <= rockHeight - 5
					        || (rockHeight < WaterHeight && y <= rockHeight))
					{
						final int orecount = 2000;
						float randv = orecount
						        * genRand(x, y, z, RandClass.OreType);
						if(randv < 16) // 0.8%
						{
							if(y < 15 - World.Depth)
								block = Block.NewRedstoneOre();
							else
								block = Block.NewStone();
						}
						else if(randv < 36) // 1.0%
						{
							block = Block.NewCoalOre();
						}
						else if(randv < 48) // 0.6%
						{
							block = Block.NewIronOre();
						}
						else if(randv < 50) // 0.1%
						{
							if(y < 30 - World.Depth)
								block = Block.NewGoldOre();
							else
								block = Block.NewStone();
						}
						else if(randv < 52) // 0.1%
						{
							if(y < 15 - World.Depth)
								block = Block.NewDiamondOre();
							else
								block = Block.NewStone();
						}
						else if(randv < 53) // 0.05%
						{
							if(y < 32 - World.Depth)
								block = Block.NewLapisLazuliOre();
							else
								block = Block.NewStone();
						}
						else if(randv < 54) // 0.05%
						{
							if(y < 32 - World.Depth)
								block = Block.NewEmeraldOre();
							else
								block = Block.NewStone();
						}
						else
							block = Block.NewStone();
					}
					else if(y < rockHeight)
						block = Block.NewDirt();
					else if(y == rockHeight)
						block = Block.NewGrass();
					else if(y <= WaterHeight)
						block = Block.NewStationaryWater();
					else
					{
						Tree.TreeBlockKind tbk = getTreeBlockKind(x, y, z);
						switch(tbk)
						{
						case Leaves:
							block = Block.NewLeaves();
							break;
						case None:
							block = new Block();
							break;
						case Wood:
							block = Block.NewWood();
							break;
						}
					}
					generatedChunk.setBlock(x, y, z, block);
				}
			}
		}
		return generatedChunk;
	}
}
