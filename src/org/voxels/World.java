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
package org.voxels;

import static org.lwjgl.opengl.GL11.*;
import static org.voxels.Color.RGB;
import static org.voxels.Color.glClearColor;
import static org.voxels.Matrix.glLoadMatrix;
import static org.voxels.PlayerList.players;

import java.io.*;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import org.voxels.generate.*;

/**
 * @author jacob
 * 
 */
public class World
{
	/**
	 * the program's world
	 */
	public static World world = new World();
	/**
	 * the maximum height<br/>
	 * <code>-Depth &lt;= y &lt; Height</code>
	 * 
	 * @see #Depth
	 */
	public static final int Height = 64;
	/**
	 * the maximum depth<br/>
	 * <code>-Depth &lt;= y &lt; Height</code>
	 * 
	 * @see #Height
	 */
	public static final int Depth = 64;
	/**
	 * gravitational acceleration
	 */
	public static final float GravityAcceleration = 9.8f;
	private static long randSeed = new Random().nextLong();
	private static final int viewDist = 10;
	private long displayListValidTag = 0;
	private Rand landGenerator = Rand.create();
	private static final int generatedChunkScale = 1 << 0; // must be power of 2

	/**
	 * generate a random <code>float</code>
	 * 
	 * @param min
	 *            minimum value
	 * @param max
	 *            maximum value
	 * @return a random float in the range [<code>min</code>, <code>max</code>)
	 */
	public synchronized static float fRand(float min, float max)
	{
		randSeed = (randSeed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
		return ((int)(randSeed >>> (48 - 24)) / ((float)(1 << 24)))
		        * (max - min) + min;
	}

	/**
	 * generate a random <code>Vector</code>
	 * 
	 * @param magnitude
	 *            the magnitude of the resulting vector
	 * @return a random vector
	 * @see #vRand()
	 */
	public static Vector vRand(float magnitude)
	{
		Vector retval = new Vector();
		do
		{
			retval.x = fRand(-1.0f, 1.0f);
			retval.y = fRand(-1.0f, 1.0f);
			retval.z = fRand(-1.0f, 1.0f);
		}
		while(retval.abs_squared() >= 1.0f
		        || retval.abs_squared() < 1e-3f * 1e-3f);
		return retval.normalize().mul(magnitude);
	}

	/**
	 * generate a random unit <code>Vector</code>
	 * 
	 * @return a random vector
	 * @see #vRand(float magnitude)
	 */
	public static Vector vRand()
	{
		Vector retval = new Vector();
		do
		{
			retval.x = fRand(-1.0f, 1.0f);
			retval.y = fRand(-1.0f, 1.0f);
			retval.z = fRand(-1.0f, 1.0f);
		}
		while(retval.abs_squared() >= 1.0f
		        || retval.abs_squared() < 1e-3f * 1e-3f);
		return retval.normalize();
	}

	private static class EntityNode
	{
		public EntityNode()
		{
		}

		public EntityNode hashnext;
		public EntityNode hashprev;
		public EntityNode next;
		@SuppressWarnings("unused")
		public EntityNode prev;
		public Entity e;
	}

	@SuppressWarnings("unused")
	private EntityNode entityHead = null, entityTail = null;

	private static class Chunk
	{
		public static final int size = 4; // must be power of 2
		public static final int generatedChunkSize = size; // must be power of 2
		                                                   // that is less than
		                                                   // or equal to
		                                                   // Chunk.size
		public static final int generatedChunksPerChunk = Math.max(1, size
		        / generatedChunkSize);
		public final int orgx, orgy, orgz;
		private boolean generated[] = new boolean[generatedChunksPerChunk
		        * generatedChunksPerChunk * generatedChunksPerChunk];
		private Block[] blocks = new Block[size * size * size];
		public Chunk next, listnext;
		public static final int drawPhaseCount = 2;
		public int displayList[] = new int[drawPhaseCount];
		public long displayListValidTag[] = new long[drawPhaseCount];
		@SuppressWarnings("unused")
		public EntityNode head = null, tail = null;

		public Chunk(int ox, int oy, int oz)
		{
			this.orgx = ox;
			this.orgy = oy;
			this.orgz = oz;
			for(int i = 0; i < drawPhaseCount; i++)
			{
				this.displayListValidTag[i] = -1;
				this.displayList[i] = 0;
			}
		}

		@Override
		protected void finalize() throws Throwable
		{
			for(int i = 0; i < drawPhaseCount; i++)
				if(this.displayList[i] != 0)
					glDeleteLists(this.displayList[i], 1);
			super.finalize();
		}

		public Block getBlock(int cx, int cy, int cz)
		{
			int index = cx + size * (cy + size * cz);
			return this.blocks[index];
		}

		public void setBlock(int cx, int cy, int cz, Block b)
		{
			int index = cx + size * (cy + size * cz);
			this.blocks[index] = b;
		}

		public boolean isGenerated(int cx_in, int cy_in, int cz_in)
		{
			int cx = cx_in;
			int cy = cy_in;
			int cz = cz_in;
			cx -= this.orgx;
			cy -= this.orgy;
			cz -= this.orgz;
			cx /= generatedChunkSize;
			cy /= generatedChunkSize;
			cz /= generatedChunkSize;
			return this.generated[cx + generatedChunksPerChunk
			        * (cy + generatedChunksPerChunk * cz)];
		}

		public void setGenerated(int cx_in, int cy_in, int cz_in, boolean g)
		{
			int cx = cx_in;
			int cy = cy_in;
			int cz = cz_in;
			cx -= this.orgx;
			cy -= this.orgy;
			cz -= this.orgz;
			cx /= generatedChunkSize;
			cy /= generatedChunkSize;
			cz /= generatedChunkSize;
			this.generated[cx + generatedChunksPerChunk
			        * (cy + generatedChunksPerChunk * cz)] = g;
		}
	}

	private Chunk lastChunk = null;

	private void insertEntity(EntityNode node)
	{
		Vector pos = node.e.getPosition();
		int x = (int)Math.floor(pos.x);
		int y = (int)Math.floor(pos.y);
		int z = (int)Math.floor(pos.z);
		node.next = this.entityHead;
		node.prev = null;
		if(this.entityHead != null)
			this.entityHead.prev = node;
		else
			this.entityTail = node;
		this.entityHead = node;
		Chunk c = findOrInsert(getChunkX(x), getChunkY(y), getChunkZ(z));
		node.hashnext = c.head;
		node.hashprev = null;
		if(c.head != null)
			c.head.hashprev = node;
		else
			c.tail = node;
		c.head = node;
	}

	private EntityNode removeAllEntities()
	{
		EntityNode retval = this.entityHead;
		this.entityHead = null;
		this.entityTail = null;
		for(EntityNode node = retval; node != null; node = node.next)
		{
			Chunk c = null;
			if(node.hashnext == null || node.hashprev == null)
			{
				Vector pos = node.e.getPosition();
				int x = (int)Math.floor(pos.x);
				int y = (int)Math.floor(pos.y);
				int z = (int)Math.floor(pos.z);
				c = find(getChunkX(x), getChunkY(y), getChunkZ(z));
				if(c == null)
					continue;
			}
			if(node.hashnext != null)
				node.hashnext.hashprev = node.hashprev;
			else
				c.tail = node.hashprev;
			if(node.hashprev != null)
				node.hashprev.hashnext = node.hashnext;
			else
				c.head = node.hashnext;
			node.hashnext = null;
			node.hashprev = null;
		}
		return retval;
	}

	private static class EvalNode
	{
		public EvalNode()
		{
		}

		public EvalNode next = null;
		public Block b = null;
		public int x, y, z;
	}

	private enum EvalType
	{
		General, Redstone, RedstoneFirst, Lighting, Particles, Pistons, Last
	}

	private static final int EvalTypeCount = EvalType.Last.ordinal();

	private static int getChunkX(int v)
	{
		return v - (v % Chunk.size + Chunk.size) % Chunk.size;
	}

	private static int getChunkY(int v)
	{
		return v - (v % Chunk.size + Chunk.size) % Chunk.size;
	}

	private static int getChunkZ(int v)
	{
		return v - (v % Chunk.size + Chunk.size) % Chunk.size;
	}

	private static int getGeneratedChunkX(int v)
	{
		return v - (v % Chunk.generatedChunkSize + Chunk.generatedChunkSize)
		        % Chunk.generatedChunkSize;
	}

	private static int getGeneratedChunkY(int v)
	{
		return v - (v % Chunk.generatedChunkSize + Chunk.generatedChunkSize)
		        % Chunk.generatedChunkSize;
	}

	private static int getGeneratedChunkZ(int v)
	{
		return v - (v % Chunk.generatedChunkSize + Chunk.generatedChunkSize)
		        % Chunk.generatedChunkSize;
	}

	private static final int WorldHashPrimePowOf2 = 17;
	private static final int WorldHashPrime = (1 << WorldHashPrimePowOf2) - 1;

	private int ModWorldHashPrime(int v_in)
	{
		int v = v_in;
		v = (v >>> WorldHashPrimePowOf2) + (v & WorldHashPrime);
		if(v >= WorldHashPrime)
			v = (v >>> WorldHashPrimePowOf2) + (v & WorldHashPrime);
		if(v >= WorldHashPrime)
			v -= WorldHashPrime;
		return v;
	}

	private int hashPos(int x, int y, int z)
	{
		return ModWorldHashPrime((x * 9 + y) * 9 + z);
	}

	private int hashChunkPos(int cx, int cy, int cz)
	{
		return hashPos(cx, cy, cz);
	}

	private Chunk[] hashTable = new Chunk[WorldHashPrime];

	private EvalNode[][] genEvalNodeHashTable()
	{
		EvalNode[][] retval = new EvalNode[EvalType.Last.ordinal()][];
		for(int i = 0; i < EvalType.Last.ordinal(); i++)
		{
			retval[i] = new EvalNode[WorldHashPrime];
		}
		return retval;
	}

	private EvalNode[][] evalNodeHashTable = genEvalNodeHashTable();

	private void insertEvalNode(EvalType et, EvalNode newnode)
	{
		int x = newnode.x;
		int y = newnode.y;
		int z = newnode.z;
		int hash = hashPos(x, y, z);
		EvalNode lastNode = null;
		int eti = et.ordinal();
		EvalNode node = this.evalNodeHashTable[eti][hash];
		while(node != null)
		{
			if(node.x == x
			        && node.y == y
			        && node.z == z
			        && ((node.b == null && newnode.b == null) || (node.b != null && newnode.b != null)))
			{
				if(lastNode != null)
				{
					lastNode.next = node.next;
					node.next = this.evalNodeHashTable[eti][hash];
					this.evalNodeHashTable[eti][hash] = node;
				}
				node.b = newnode.b;
				return;
			}
			lastNode = node;
			node = node.next;
		}
		node = newnode;
		node.next = this.evalNodeHashTable[eti][hash];
		this.evalNodeHashTable[eti][hash] = node;
	}

	private void insertEvalNode(EvalType et, int x, int y, int z, Block b)
	{
		int hash = hashPos(x, y, z);
		EvalNode lastNode = null;
		int eti = et.ordinal();
		EvalNode node = this.evalNodeHashTable[eti][hash];
		while(node != null)
		{
			if(node.x == x
			        && node.y == y
			        && node.z == z
			        && ((node.b == null && b == null) || (node.b != null && b != null)))
			{
				if(lastNode != null)
				{
					lastNode.next = node.next;
					node.next = this.evalNodeHashTable[eti][hash];
					this.evalNodeHashTable[eti][hash] = node;
				}
				node.b = b;
				return;
			}
			lastNode = node;
			node = node.next;
		}
		node = new EvalNode();
		node.next = this.evalNodeHashTable[eti][hash];
		this.evalNodeHashTable[eti][hash] = node;
		node.x = x;
		node.y = y;
		node.z = z;
		node.b = b;
	}

	private EvalNode removeAllEvalNodes(EvalType et)
	{
		int eti = et.ordinal();
		assert eti >= 0 && eti < EvalTypeCount;
		EvalNode retval = null;
		EvalNode retvaltail = null;
		for(int hash = 0; hash < WorldHashPrime; hash++)
		{
			if(this.evalNodeHashTable[eti][hash] == null)
				continue;
			if(retval == null)
			{
				retval = this.evalNodeHashTable[eti][hash];
				retvaltail = this.evalNodeHashTable[eti][hash];
			}
			else
			{
				retvaltail.next = this.evalNodeHashTable[eti][hash];
			}
			this.evalNodeHashTable[eti][hash] = null;
			while(retvaltail.next != null)
				retvaltail = retvaltail.next;
		}
		return retval;
	}

	private void invalidateChunk(int cx, int cy, int cz)
	{
		Chunk c = find(cx, cy, cz);
		if(c == null)
			return;
		for(int i = 0; i < Chunk.drawPhaseCount; i++)
			c.displayListValidTag[i] = -1;
	}

	private void insertEvalNode(EvalType et, int x, int y, int z)
	{
		insertEvalNode(et, x, y, z, null);
	}

	private void invalidate(int x, int y, int z)
	{
		if(y < -Depth || y >= Height)
			return;
		invalidateChunk(getChunkX(x), getChunkY(y), getChunkZ(z));
		for(EvalType i : EvalType.values())
		{
			if(i == EvalType.Last || i == EvalType.Particles)
				continue;
			insertEvalNode(i, x, y, z);
		}
	}

	private static class TimedInvalidate
	{
		public final int x, y, z;
		public double timeLeft;
		public TimedInvalidate next;

		public TimedInvalidate(int x, int y, int z, double timeLeft)
		{
			this.x = x;
			this.y = y;
			this.z = z;
			this.timeLeft = timeLeft;
		}

		public boolean isReady()
		{
			return this.timeLeft <= 0.0;
		}

		public void advanceTime(double deltatime)
		{
			this.timeLeft -= deltatime;
			if(this.timeLeft < 0.0)
				this.timeLeft = 0.0;
		}
	}

	private TimedInvalidate timedInvalidateHead = null;

	/**
	 * add a block invalidate in the future
	 * 
	 * @param x
	 *            x coordinate
	 * @param y
	 *            y coordinate
	 * @param z
	 *            z coordinate
	 * @param seconds
	 *            relative time to run
	 */
	public void addTimedInvalidate(int x, int y, int z, double seconds)
	{
		TimedInvalidate i = new TimedInvalidate(x, y, z, seconds);
		i.next = this.timedInvalidateHead;
		this.timedInvalidateHead = i;
	}

	private void checkAllTimedInvalidates()
	{
		double deltatime = Main.getFrameDuration();
		TimedInvalidate head = null;
		for(TimedInvalidate i = this.timedInvalidateHead; i != null; i = i.next)
		{
			i.advanceTime(deltatime);
			if(i.isReady())
			{
				invalidate(i.x, i.y, i.z);
			}
			else
			{
				i.next = head;
				head = i;
			}
		}
		this.timedInvalidateHead = head;
	}

	private void addParticleGen(int x, int y, int z)
	{
		insertEvalNode(EvalType.Particles, x, y, z);
	}

	private Chunk find(int cx, int cy, int cz)
	{
		if(this.lastChunk != null && this.lastChunk.orgx == cx
		        && this.lastChunk.orgy == cy && this.lastChunk.orgz == cz)
			return this.lastChunk;
		int hash = hashChunkPos(cx, cy, cz);
		Chunk lastNode = null;
		Chunk node = this.hashTable[hash];
		while(node != null)
		{
			if(node.orgx == cx && node.orgy == cy && node.orgz == cz)
			{
				if(lastNode != null)
				{
					lastNode.next = node.next;
					node.next = this.hashTable[hash];
					this.hashTable[hash] = node;
				}
				this.lastChunk = node;
				return node;
			}
			lastNode = node;
			node = node.next;
		}
		return null;
	}

	private Chunk chunksHead = null;

	private Chunk findOrInsert(int cx, int cy, int cz)
	{
		if(this.lastChunk != null && this.lastChunk.orgx == cx
		        && this.lastChunk.orgy == cy && this.lastChunk.orgz == cz)
			return this.lastChunk;
		int hash = hashChunkPos(cx, cy, cz);
		Chunk lastNode = null;
		Chunk node = this.hashTable[hash];
		while(node != null)
		{
			if(node.orgx == cx && node.orgy == cy && node.orgz == cz)
			{
				if(lastNode != null)
				{
					lastNode.next = node.next;
					node.next = this.hashTable[hash];
					this.hashTable[hash] = node;
				}
				this.lastChunk = node;
				return node;
			}
			lastNode = node;
			node = node.next;
		}
		node = new Chunk(cx, cy, cz);
		node.next = this.hashTable[hash];
		this.hashTable[hash] = node;
		node.listnext = this.chunksHead;
		this.chunksHead = node;
		this.lastChunk = node;
		return node;
	}

	private boolean isGenerated(int cx, int cy, int cz)
	{
		Chunk c = find(getChunkX(getGeneratedChunkX(cx)),
		               getChunkY(getGeneratedChunkY(cy)),
		               getChunkZ(getGeneratedChunkZ(cz)));
		if(c == null)
			return false;
		return c.isGenerated(cx, cy, cz);
	}

	private void setGenerated(int cx, int cy, int cz, boolean g)
	{
		Chunk c = find(getChunkX(getGeneratedChunkX(cx)),
		               getChunkY(getGeneratedChunkY(cy)),
		               getChunkZ(getGeneratedChunkZ(cz)));
		if(c == null)
			return;
		c.setGenerated(cx, cy, cz, g);
	}

	/**
	 * gets the block at (<code>x</code>, <code>y</code>, <code>z</code>)
	 * 
	 * @param x
	 *            the x coordinate of the block to get
	 * @param y
	 *            the y coordinate of the block to get
	 * @param z
	 *            the z coordinate of the block to get
	 * @return the block at (<code>x</code>, <code>y</code>, <code>z</code>) or
	 *         <code>null</code>
	 * @see #getBlockEval(int x, int y, int z)
	 * @see #setBlock(int x, int y, int z, Block block)
	 */
	public Block getBlock(int x, int y, int z)
	{
		int cx = getChunkX(x);
		int cy = getChunkY(y);
		int cz = getChunkZ(z);
		Chunk c = find(cx, cy, cz);
		if(c == null)
			return null;
		return c.getBlock(x - cx, y - cy, z - cz);
	}

	private void internalSetBlock(int x, int y, int z, Block b)
	{
		int cx = getChunkX(x);
		int cy = getChunkY(y);
		int cz = getChunkZ(z);
		Chunk c = findOrInsert(cx, cy, cz);
		c.setBlock(x - cx, y - cy, z - cz, b);
	}

	private void resetLightingArrays(int x, int y, int z)
	{
		for(int dx = -1; dx <= 1; dx++)
		{
			for(int dy = -1; dy <= 1; dy++)
			{
				for(int dz = -1; dz <= 1; dz++)
				{
					Block b = getBlock(x + dx, y + dy, z + dz);
					if(b == null)
						continue;
					b.setLightingArray(null, this.sunlightFactor);
					internalSetBlock(x + dx, y + dy, z + dz, b);
				}
			}
		}
	}

	private void addGeneratedChunk(GeneratedChunk c)
	{
		assert c.size == Chunk.generatedChunkSize * generatedChunkScale;
		assert c.cx % c.size == 0;
		assert c.cy % c.size == 0;
		assert c.cz % c.size == 0;
		for(int cx = c.cx; cx < c.cx + c.size; cx += Chunk.generatedChunkSize)
		{
			for(int cy = c.cy; cy < c.cy + c.size; cy += Chunk.generatedChunkSize)
			{
				for(int cz = c.cz; cz < c.cz + c.size; cz += Chunk.generatedChunkSize)
				{
					for(int x = cx; x < cx + Chunk.generatedChunkSize; x++)
					{
						for(int y = cy; y < cy + Chunk.generatedChunkSize; y++)
						{
							for(int z = cz; z < cz + Chunk.generatedChunkSize; z++)
							{
								setBlock(x, y, z, c.getBlock(x, y, z));
							}
						}
					}
					setGenerated(cx, cy, cz, true);
				}
			}
		}
	}

	/**
	 * sets the block at (<code>x</code>, <code>y</code>, <code>z</code>)
	 * 
	 * @param x
	 *            the x coordinate of the block to get
	 * @param y
	 *            the y coordinate of the block to get
	 * @param z
	 *            the z coordinate of the block to get
	 * @param block
	 *            the new block
	 * @see #getBlock(int x, int y, int z)
	 */
	public void setBlock(int x, int y, int z, Block block)
	{
		if(y < -Depth || y >= Height)
			return;
		Block b = new Block(block);
		Block oldb = getBlock(x, y, z);
		if(oldb != null)
			b.copyLighting(oldb);
		else
			b.setLighting(0, 0, 0);
		if(oldb == null || oldb.getEmitLight() != b.getEmitLight())
			b.resetLighting();
		internalSetBlock(x, y, z, b);
		resetLightingArrays(x, y, z);
		for(int dx = -2; dx <= 2; dx++)
		{
			for(int dy = -2; dy <= 2; dy++)
			{
				for(int dz = -2; dz <= 2; dz++)
				{
					int td = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
					if(td > 2)
						continue;
					invalidate(x + dx, y + dy, z + dz);
				}
			}
		}
		for(int orientation = 0; orientation < 6; orientation++)
		{
			int dx = Block.getOrientationDX(orientation);
			int dy = Block.getOrientationDY(orientation);
			int dz = Block.getOrientationDZ(orientation);
			for(int i = 3; i < 13; i++)
			{
				invalidate(x + dx * i, y + dy * i, z + dz * i);
			}
		}
	}

	/**
	 * gets the block at (<code>x</code>, <code>y</code>, <code>z</code>) for
	 * evaluation<br/>
	 * returns <code>null</code> if the block wasn't generated yet
	 * 
	 * @param x
	 *            the x coordinate of the block to get
	 * @param y
	 *            the y coordinate of the block to get
	 * @param z
	 *            the z coordinate of the block to get
	 * @return the block at (<code>x</code>, <code>y</code>, <code>z</code>) or
	 *         <code>null</code>
	 * @see #getBlock(int x, int y, int z)
	 */
	public Block getBlockEval(int x, int y, int z)
	{
		if(!isGenerated(getGeneratedChunkX(x),
		                getGeneratedChunkY(y),
		                getGeneratedChunkZ(z)))
			return null;
		return getBlock(x, y, z);
	}

	private int GetSunlight(int x, int y, int z)
	{
		if(y < -Depth)
			return 0;
		if(y >= Height)
			return 15;
		Block b = getBlock(x, y, z);
		if(b == null)
		{
			if(y > getLandHeight(x, z))
				return Math.min(15, 15 + (y - Rand.WaterHeight) * 2);
			return 0;
		}
		return b.getSunlight();
	}

	/**
	 * gets the original height of the land at (<code>x</code>, <code>z</code>)
	 * 
	 * @param x
	 *            the x coordinate
	 * @param z
	 *            the z coordinate
	 * @return the original height of the land at (<code>x</code>,
	 *         <code>z</code>)
	 */
	public int getLandHeight(int x, int z)
	{
		return this.landGenerator.getRockHeight(x, z);
	}

	int GetScatteredSunlight(int x, int y, int z)
	{
		if(y < -Depth)
			return 0;
		if(y >= Height)
			return 15;
		Block b = getBlock(x, y, z);
		if(b == null)
		{
			if(y > getLandHeight(x, z))
				return Math.min(15, 15 + (y - Rand.WaterHeight) * 2);
			return 0;
		}
		return b.getScatteredSunlight();
	}

	int GetLight(int x, int y, int z)
	{
		if(y < -Depth)
			return 0;
		if(y >= Height)
			return 0;
		Block b = getBlock(x, y, z);
		if(b == null)
		{
			return 0;
		}
		return b.getLight();
	}

	private void updateLight()
	{
		for(EvalNode node = removeAllEvalNodes(EvalType.Lighting); node != null; node = removeAllEvalNodes(EvalType.Lighting))
		{
			while(node != null)
			{
				int x = node.x, y = node.y, z = node.z;
				Block b = getBlock(x, y, z);
				if(PlayerList.PlayerCount > 0 && !Main.isServer)
					b = null;
				if(b == null)
				{
					node = node.next;
					continue;
				}
				int newlight = b.getEmitLight();
				int newsunlight = GetSunlight(x, y + 1, z);
				int newscatteredsunlight = 0;
				int light, scatteredsunlight;
				light = GetLight(x - 1, y, z);
				scatteredsunlight = GetScatteredSunlight(x - 1, y, z);
				if(newlight < light)
					newlight = light;
				if(newscatteredsunlight < scatteredsunlight)
					newscatteredsunlight = scatteredsunlight;
				light = GetLight(x + 1, y, z);
				scatteredsunlight = GetScatteredSunlight(x + 1, y, z);
				if(newlight < light)
					newlight = light;
				if(newscatteredsunlight < scatteredsunlight)
					newscatteredsunlight = scatteredsunlight;
				light = GetLight(x, y - 1, z);
				scatteredsunlight = GetScatteredSunlight(x, y - 1, z);
				if(newlight < light)
					newlight = light;
				if(newscatteredsunlight < scatteredsunlight)
					newscatteredsunlight = scatteredsunlight;
				light = GetLight(x, y + 1, z);
				scatteredsunlight = GetScatteredSunlight(x, y + 1, z);
				if(newlight < light)
					newlight = light;
				if(newscatteredsunlight < scatteredsunlight)
					newscatteredsunlight = scatteredsunlight;
				light = GetLight(x, y, z - 1);
				scatteredsunlight = GetScatteredSunlight(x, y, z - 1);
				if(newlight < light)
					newlight = light;
				if(newscatteredsunlight < scatteredsunlight)
					newscatteredsunlight = scatteredsunlight;
				light = GetLight(x, y, z + 1);
				scatteredsunlight = GetScatteredSunlight(x, y, z + 1);
				if(newlight < light)
					newlight = light;
				if(newscatteredsunlight < scatteredsunlight)
					newscatteredsunlight = scatteredsunlight;
				newlight--;
				if(newlight < 0)
					newlight = 0;
				newscatteredsunlight--;
				if(newscatteredsunlight < 0)
					newscatteredsunlight = 0;
				b = new Block(b);
				if(b.setLighting(newsunlight, newscatteredsunlight, newlight))
				{
					internalSetBlock(x, y, z, b);
					resetLightingArrays(x, y, z);
					invalidate(x - 1, y, z);
					invalidate(x + 1, y, z);
					invalidate(x, y - 1, z);
					invalidate(x, y + 1, z);
					invalidate(x, y, z - 1);
					invalidate(x, y, z + 1);
					invalidate(x, y, z);
				}
				node = node.next;
			}
		}
	}

	private int sunlightFactor = 15; // integer between 0 and 15
	private float timeOfDay = 0.5f;

	private static Color getBackgroundColor(float timeOfDay)
	{
		float intensity = 0.7f - 0.8f * (float)Math.cos(timeOfDay * 2.0
		        * Math.PI);
		if(intensity < 0.0f)
			intensity = 0.0f;
		if(intensity > 1.0f)
			return RGB(intensity - 1.0f, intensity - 1.0f, 1.0f);
		return RGB(0.0f, 0.0f, intensity);
	}

	private Color backgroundColor = getBackgroundColor(0.5f);

	private void setSunlightFactor()
	{
		float seconds = 20.0f * 60.0f * this.timeOfDay;
		final float secondsPerLightlevel = 10.0f;
		final int nightLight = 4, dayLight = 15;
		final float secondsForDawn = (dayLight - nightLight)
		        * secondsPerLightlevel;
		final float secondsForDusk = (dayLight - nightLight)
		        * secondsPerLightlevel;
		if(seconds < 5.0f * 60.0f - secondsForDawn)
		{
			this.sunlightFactor = 4;
		}
		else if(seconds < 5.0f * 60.0f)
		{
			float brightness = (secondsForDawn + seconds - 5.0f * 60.0f)
			        / secondsPerLightlevel;
			this.sunlightFactor = Math.round(brightness) + nightLight;
		}
		else if(seconds < 15.0f * 60.0f)
		{
			this.sunlightFactor = 15;
		}
		else if(seconds < 15.0f * 60.0f + secondsForDusk)
		{
			float brightness = (secondsForDusk + 15.0f * 60.0f - seconds)
			        / secondsPerLightlevel;
			this.sunlightFactor = Math.round(brightness) + nightLight;
		}
		else
			this.sunlightFactor = 4;
		this.displayListValidTag++;
	}

	private void setBackgroundColor()
	{
		this.backgroundColor = getBackgroundColor(this.timeOfDay);
	}

	private Vector sunPosition = new Vector(0);
	private Vector moonPosition = new Vector(0);

	private void setSunMoonPosition(Vector sunPosition,
	                                float sunIntensity_in,
	                                Vector moonPosition,
	                                float moonIntensity_in)
	{
		float sunIntensity = sunIntensity_in;
		float moonIntensity = moonIntensity_in;
		if(sunIntensity < 0)
			sunIntensity = 0;
		if(moonIntensity < 0)
			moonIntensity = 0;
		this.sunPosition = sunPosition.normalize().mul(sunIntensity);
		this.moonPosition = moonPosition.normalize().mul(moonIntensity);
	}

	/**
	 * sets the time of day
	 * 
	 * @param timeOfDay
	 *            the new time of day
	 */
	public void setTimeOfDay(float timeOfDay)
	{
		this.timeOfDay = timeOfDay - (float)Math.floor(timeOfDay);
		setSunlightFactor();
		setBackgroundColor();
		Vector sunvec = new Vector(-(float)Math.sin(this.timeOfDay * 2.0
		                                   * Math.PI),
		                           -(float)Math.cos(this.timeOfDay * 2.0
		                                   * Math.PI),
		                           0.0f);
		float sunStrength = 0.25f - 0.75f * (float)Math.cos(this.timeOfDay
		        * 2.0 * Math.PI);
		if(sunStrength < 0)
			sunStrength = 0;
		float moonStrength = 0.25f + 0.75f * (float)Math.cos(this.timeOfDay
		        * 2.0 * Math.PI);
		if(moonStrength < 0)
			moonStrength = 0;
		setSunMoonPosition(sunvec,
		                   sunStrength,
		                   sunvec.neg(),
		                   0.25f * moonStrength);
	}

	private int genChunkX = 0, genChunkY = 0, genChunkZ = 0;
	private float genChunkDistance = -1.0f;

	private void addGenChunk(int cx, int cy, int cz, float distance)
	{
		if(this.genChunkDistance < 0.0f || distance < this.genChunkDistance)
		{
			this.genChunkX = cx;
			this.genChunkY = cy;
			this.genChunkZ = cz;
			this.genChunkDistance = distance;
		}
	}

	private static final float chunkGenScale = 1.5f;

	/**
	 * @param p
	 * @param a
	 * @param b
	 * @param c
	 * @param d
	 * @return true if any p is inside p.x * a + p.y * b + p.z * c + d <= 0
	 */
	private boolean chunkPassClipPlane(Vector p[],
	                                   float a,
	                                   float b,
	                                   float c,
	                                   float d)
	{
		for(int i = 0; i < p.length; i++)
		{
			if(p[i].dot(new Vector(a, b, c)) + d <= 0)
				return true;
		}
		if(p.length <= 0)
			return true;
		return false;
	}

	private boolean chunkVisible(int cx, int cy, int cz, Matrix worldToCamera)
	{
		Vector p[] = new Vector[8];
		for(int i = 0; i < 8; i++)
		{
			Vector v = new Vector(cx, cy, cz);
			if((i & 1) != 0)
				v.x += Chunk.size;
			if((i & 2) != 0)
				v.y += Chunk.size;
			if((i & 4) != 0)
				v.z += Chunk.size;
			p[i] = worldToCamera.apply(v);
		}
		if(!chunkPassClipPlane(p, 0, 0, 1, 0))
			return false;
		if(!chunkPassClipPlane(p, -1, 0, 1, 0))
			return false;
		if(!chunkPassClipPlane(p, 1, 0, 1, 0))
			return false;
		if(!chunkPassClipPlane(p, 0, -1, 1, 0))
			return false;
		if(!chunkPassClipPlane(p, 0, 1, 1, 0))
			return false;
		return true;
	}

	private void drawBlock(int x, int y, int z, int drawPhase)
	{
		Block b = getBlock(x, y, z);
		if(b == null)
			return;
		if(b.isTranslucent() && drawPhase != 1)
			return;
		if(!b.isTranslucent() && drawPhase != 0)
			return;
		b.draw(Matrix.translate(x, y, z));
	}

	private void drawChunk(int cx, int cy, int cz, int drawPhase)
	{
		final boolean USE_DISPLAY_LIST = true;
		Chunk pnode = find(cx, cy, cz);
		if(pnode == null)
			return;
		EntityNode e = pnode.head;
		while(e != null)
		{
			e.e.draw(Matrix.identity());
			e = e.hashnext;
		}
		if(USE_DISPLAY_LIST)
		{
			if(pnode.displayListValidTag[drawPhase] == this.displayListValidTag)
			{
				glCallList(pnode.displayList[drawPhase]);
				return;
			}
			if(pnode.displayList[drawPhase] == 0)
				pnode.displayList[drawPhase] = glGenLists(1);
			glNewList(pnode.displayList[drawPhase], GL_COMPILE_AND_EXECUTE);
			Image.onListStart();
		}
		for(int x = 0; x < Chunk.size; x++)
		{
			for(int y = 0; y < Chunk.size; y++)
			{
				for(int z = 0; z < Chunk.size; z++)
				{
					drawBlock(x + cx, y + cy, z + cz, drawPhase);
				}
			}
		}
		if(USE_DISPLAY_LIST)
		{
			glEndList();
			pnode.displayListValidTag[drawPhase] = this.displayListValidTag;
		}
	}

	/**
	 * draw the world
	 * 
	 * @param worldToCamera
	 *            the transformation from world coordinates to camera
	 *            coordinates
	 */
	public void draw(Matrix worldToCamera)
	{
		Vector cameraPos = worldToCamera.invert().apply(new Vector(0));
		int cameraX = (int)Math.floor(cameraPos.x);
		int cameraY = (int)Math.floor(cameraPos.y);
		int cameraZ = (int)Math.floor(cameraPos.z);
		glClearColor(this.backgroundColor);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		glMatrixMode(GL_MODELVIEW);
		glPushMatrix();
		glLoadMatrix(worldToCamera);
		Block sunb = Block.NewSun();
		Block moonb = Block.NewMoon();
		if(!this.sunPosition.equals(new Vector(0)))
			sunb.drawAsItem(Matrix.translate(cameraPos.add(new Vector(-0.5f,
			                                                          -0.5f,
			                                                          -0.5f))
			                                          .add(this.sunPosition.normalize()
			                                                               .mul(10.0f))));
		if(!this.moonPosition.equals(new Vector(0)))
			moonb.drawAsItem(Matrix.translate(cameraPos.add(new Vector(-0.5f,
			                                                           -0.5f,
			                                                           -0.5f))
			                                           .add(this.moonPosition.normalize()
			                                                                 .mul(10.0f))));
		glClear(GL_DEPTH_BUFFER_BIT);
		int minDrawX = getChunkX(cameraX - viewDist);
		int maxDrawX = getChunkX(cameraX + viewDist);
		int minDrawY = getChunkY(cameraY - viewDist);
		int maxDrawY = getChunkY(cameraY + viewDist);
		int minDrawZ = getChunkZ(cameraZ - viewDist);
		int maxDrawZ = getChunkZ(cameraZ + viewDist);
		for(int drawPhase = 0; drawPhase < Chunk.drawPhaseCount; drawPhase++)
		{
			for(int cx = getChunkX(Math.round(cameraX - viewDist
			        * chunkGenScale)); cx <= getChunkX(Math.round(cameraX
			        + viewDist * chunkGenScale)); cx += Chunk.size)
			{
				for(int cy = getChunkY(Math.round(cameraY - viewDist
				        * chunkGenScale)); cy <= getChunkY(Math.round(cameraY
				        + viewDist * chunkGenScale)); cy += Chunk.size)
				{
					for(int cz = getChunkZ(Math.round(cameraZ - viewDist
					        * chunkGenScale)); cz <= getChunkZ(Math.round(cameraZ
					        + viewDist * chunkGenScale)); cz += Chunk.size)
					{
						boolean isVisible = false;
						if(chunkVisible(cx, cy, cz, worldToCamera))
						{
							isVisible = true;
							if(cx >= minDrawX && cx <= maxDrawX
							        && cy >= minDrawY && cy <= maxDrawY
							        && cz >= minDrawZ && cz <= maxDrawZ)
								drawChunk(cx, cy, cz, drawPhase);
						}
						else if(drawPhase > 0)
							continue;
						Chunk c = find(cx, cy, cz);
						if(c == null)
						{
							for(int gcx = cx; gcx < cx + Chunk.size; gcx += Chunk.generatedChunkSize)
							{
								for(int gcy = cy; gcy < cy + Chunk.size; gcy += Chunk.generatedChunkSize)
								{
									for(int gcz = cz; gcz < cz + Chunk.size; gcz += Chunk.generatedChunkSize)
									{
										Vector chunkCenter = new Vector(gcx
										        + Chunk.generatedChunkSize
										        / 2.0f, gcy
										        + Chunk.generatedChunkSize
										        / 2.0f, gcz
										        + Chunk.generatedChunkSize
										        / 2.0f);
										float distance = chunkCenter.sub(cameraPos)
										                            .abs();
										if(isVisible)
											distance /= 2;
										addGenChunk(gcx, gcy, gcz, distance);
									}
								}
							}
						}
						else
						{
							for(int gcx = cx; gcx < cx + Chunk.size; gcx += Chunk.generatedChunkSize)
							{
								for(int gcy = cy; gcy < cy + Chunk.size; gcy += Chunk.generatedChunkSize)
								{
									for(int gcz = cz; gcz < cz + Chunk.size; gcz += Chunk.generatedChunkSize)
									{
										if(!isGenerated(gcx, gcy, gcz))
										{
											Vector chunkCenter = new Vector(gcx
											        + Chunk.generatedChunkSize
											        / 2.0f, gcy
											        + Chunk.generatedChunkSize
											        / 2.0f, gcz
											        + Chunk.generatedChunkSize
											        / 2.0f);
											float distance = chunkCenter.sub(cameraPos)
											                            .abs();
											if(isVisible)
												distance /= 2;
											addGenChunk(gcx, gcy, gcz, distance);
											continue;
										}
										for(int x = gcx; x < gcx
										        + Chunk.generatedChunkSize; x++)
										{
											for(int y = gcy; y < gcy
											        + Chunk.generatedChunkSize; y++)
											{
												for(int z = gcz; z < gcz
												        + Chunk.generatedChunkSize; z++)
												{
													Block b = getBlock(x, y, z);
													if(b != null
													        && b.isParticleGenerate())
													{
														addParticleGen(x, y, z);
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
			switch(drawPhase)
			{
			case 0:
				glFinish();
				glDepthMask(false);
				break;
			case 1:
				glFinish();
				glDepthMask(true);
				break;
			}
		}
		glPopMatrix();
	}

	int[] getLightingArray(int bx, int by, int bz)
	{
		Block b = getBlock(bx, by, bz);
		if(b == null)
			return new int[]
			{
			    0, 0, 0, 0, 0, 0, 0, 0
			};
		if(b.getLightingArray(this.sunlightFactor) != null)
			return b.getLightingArray(this.sunlightFactor);
		int l[] = new int[3 * 3 * 3];
		boolean o[] = new boolean[3 * 3 * 3];
		for(int dx = 0; dx < 3; dx++)
		{
			for(int dy = 0; dy < 3; dy++)
			{
				for(int dz = 0; dz < 3; dz++)
				{
					Block block = getBlock(bx + dx - 1, by + dy - 1, bz + dz
					        - 1);
					if(block == null)
					{
						l[dx + 3 * (dy + 3 * dz)] = 0;
						o[dx + 3 * (dy + 3 * dz)] = true;
						continue;
					}
					l[dx + 3 * (dy + 3 * dz)] = block.getLighting(this.sunlightFactor);
					o[dx + 3 * (dy + 3 * dz)] = block.isOpaque();
				}
			}
		}
		for(int dx = 0; dx < 3; dx++)
		{
			for(int dy = 0; dy < 3; dy++)
			{
				for(int dz = 0; dz < 3; dz++)
				{
					int dcount = 0;
					if(dx != 1)
						dcount++;
					if(dy != 1)
						dcount++;
					if(dz != 1)
						dcount++;
					if(dcount <= 1)
						continue;
					if(dx != 1 && !o[dx + 3 * (1 + 3 * 1)])
						dcount--;
					if(dy != 1 && !o[1 + 3 * (dy + 3 * 1)])
						dcount--;
					if(dz != 1 && !o[1 + 3 * (1 + 3 * dz)])
						dcount--;
					if(dcount <= 0)
						l[dx + 3 * (dy + 3 * dz)] = 0;
				}
			}
		}
		int fl[] = new int[2 * 2 * 2];
		for(int x = 0; x < 2; x++)
		{
			for(int y = 0; y < 2; y++)
			{
				for(int z = 0; z < 2; z++)
				{
					int v = 0;
					for(int dx = 0; dx < 2; dx++)
					{
						for(int dy = 0; dy < 2; dy++)
						{
							for(int dz = 0; dz < 2; dz++)
							{
								int cx = x + dx;
								int cy = y + dy;
								int cz = z + dz;
								v = Math.max(v, l[cx + 3 * (cy + 3 * cz)]);
							}
						}
					}
					fl[x + 2 * (y + 2 * z)] = v;
				}
			}
		}
		b.setLightingArray(fl, this.sunlightFactor);
		return fl;
	}

	float
	    getLighting(float x_in, float y_in, float z_in, int bx, int by, int bz)
	{
		int l[] = getLightingArray(bx, by, bz);
		float x = x_in - bx;
		float y = y_in - by;
		float z = z_in - bz;
		float nx = 1 - x, ny = 1 - y, nz = 1 - z;
		float l00 = nz * l[0 + 2 * (0 + 2 * 0)] + z * l[0 + 2 * (0 + 2 * 1)];
		float l10 = nz * l[1 + 2 * (0 + 2 * 0)] + z * l[1 + 2 * (0 + 2 * 1)];
		float l01 = nz * l[0 + 2 * (1 + 2 * 0)] + z * l[0 + 2 * (1 + 2 * 1)];
		float l11 = nz * l[1 + 2 * (1 + 2 * 0)] + z * l[1 + 2 * (1 + 2 * 1)];
		float l0 = ny * l00 + y * l01;
		float l1 = ny * l10 + y * l11;
		return (nx * l0 + x * l1) / 15.0f;
	}

	float getLighting(Vector p, int bx, int by, int bz)
	{
		return getLighting(p.x, p.y, p.z, bx, by, bz);
	}

	float getLighting(float x, float y, float z)
	{
		return getLighting(x,
		                   y,
		                   z,
		                   (int)Math.floor(x),
		                   (int)Math.floor(y),
		                   (int)Math.floor(z));
	}

	float getLighting(Vector p)
	{
		return getLighting(p.x, p.y, p.z);
	}

	/**
	 * set the seed for this world<br/>
	 * should be called before calling anything else
	 * 
	 * @param newSeed
	 *            the new seed
	 */
	public void setSeed(int newSeed)
	{
		this.landGenerator = Rand.create(newSeed);
	}

	private static class ChunkGenerator implements Runnable
	{
		public int cx, cy, cz;
		public GeneratedChunk newChunk = null;
		public boolean generated = false;
		public AtomicBoolean busy = new AtomicBoolean(false);
		public Rand landGenerator = null;
		public Thread curThread = null;

		public ChunkGenerator()
		{
		}

		@Override
		public void run()
		{
			this.newChunk = this.landGenerator.genChunk(this.cx,
			                                            this.cy,
			                                            this.cz,
			                                            Chunk.generatedChunkSize
			                                                    * generatedChunkScale);
			this.generated = true;
			this.busy.set(false);
		}
	}

	private ChunkGenerator chunkGenerator = new ChunkGenerator();

	/**
	 * generate chunks
	 */
	public void generateChunks()
	{
		if(this.chunkGenerator.busy.get())
			return;
		if(this.chunkGenerator.generated)
		{
			addGeneratedChunk(this.chunkGenerator.newChunk);
			this.chunkGenerator.generated = false;
			this.chunkGenerator.newChunk = null;
		}
		if(this.genChunkDistance < 0)
			return;
		if(isGenerated(this.genChunkX, this.genChunkY, this.genChunkZ))
		{
			this.genChunkDistance = -1;
			return;
		}
		final int generateSize = Chunk.generatedChunkSize * generatedChunkScale;
		this.chunkGenerator.cx = this.genChunkX
		        - (this.genChunkX % generateSize + generateSize) % generateSize;
		this.chunkGenerator.cy = this.genChunkY
		        - (this.genChunkY % generateSize + generateSize) % generateSize;
		this.chunkGenerator.cz = this.genChunkZ
		        - (this.genChunkZ % generateSize + generateSize) % generateSize;
		this.chunkGenerator.landGenerator = this.landGenerator;
		this.chunkGenerator.busy.set(true);
		this.chunkGenerator.curThread = new Thread(this.chunkGenerator);
		this.chunkGenerator.curThread.start();
		this.genChunkDistance = -1;
	}

	/**
	 * insert a new entity into this world
	 * 
	 * @param e
	 *            the entity to insert
	 */
	public void insertEntity(Entity e)
	{
		if(e == null || e.isEmpty())
			return;
		EntityNode node = new EntityNode();
		node.e = new Entity(e);
		insertEntity(node);
	}

	private void moveEntities()
	{
		for(EntityNode node = removeAllEntities(), nextNode = (node != null ? node.next
		        : null); node != null; node = nextNode, nextNode = (node != null ? node.next
		        : null))
		{
			node.e.move();
			if(!node.e.isEmpty())
				insertEntity(node);
		}
		players.entityCheckHitPlayers();
	}

	void checkHitPlayer(Player p)
	{
		for(EntityNode node = removeAllEntities(), nextNode = (node != null ? node.next
		        : null); node != null; node = nextNode, nextNode = (node != null ? node.next
		        : null))
		{
			node.e.checkHitPlayer(p);
			if(!node.e.isEmpty())
				insertEntity(node);
		}
	}

	private void moveRedstone()
	{
		for(EvalNode node = removeAllEvalNodes(EvalType.RedstoneFirst); node != null; node = node.next)
		{
			Block b = getBlockEval(node.x, node.y, node.z);
			if(b != null)
				insertEvalNode(EvalType.RedstoneFirst,
				               node.x,
				               node.y,
				               node.z,
				               b.redstoneMove(node.x, node.y, node.z));
		}
		for(EvalNode node = removeAllEvalNodes(EvalType.RedstoneFirst); node != null; node = node.next)
		{
			if(node.b == null)
				continue;
			setBlock(node.x, node.y, node.z, node.b);
		}
		for(int i = 0; i < 16; i++)
		{
			for(EvalNode node = removeAllEvalNodes(EvalType.Redstone); node != null; node = node.next)
			{
				Block b = getBlockEval(node.x, node.y, node.z);
				if(b != null)
					insertEvalNode(EvalType.Redstone,
					               node.x,
					               node.y,
					               node.z,
					               b.redstoneDustMove(node.x, node.y, node.z));
			}
			for(EvalNode node = removeAllEvalNodes(EvalType.Redstone); node != null; node = node.next)
			{
				if(node.b == null)
					continue;
				setBlock(node.x, node.y, node.z, node.b);
			}
		}
	}

	private void moveGeneral()
	{
		for(EvalNode node = removeAllEvalNodes(EvalType.General); node != null; node = node.next)
		{
			Block b = getBlockEval(node.x, node.y, node.z);
			if(b != null)
			{
				Entity e = b.evalBlockToEntity(node.x, node.y, node.z);
				if(e != null)
				{
					insertEvalNode(EvalType.General,
					               node.x,
					               node.y,
					               node.z,
					               new Block());
					insertEntity(e);
				}
				else
				{
					insertEvalNode(EvalType.General,
					               node.x,
					               node.y,
					               node.z,
					               b.move(node.x, node.y, node.z));
				}
			}
		}
		for(EvalNode node = removeAllEvalNodes(EvalType.General); node != null; node = node.next)
		{
			if(node.b == null)
				continue;
			setBlock(node.x, node.y, node.z, node.b);
		}
	}

	private void movePistons()
	{
		EvalNode node = removeAllEvalNodes(EvalType.Pistons);
		while(node != null)
		{
			Block b = getBlockEval(node.x, node.y, node.z);
			if(b != null)
				b.pistonMove(node.x, node.y, node.z);
			node = node.next;
		}
	}

	private static final float redstoneMovePeriod = 0.1f;
	private static final float generalMovePeriod = 0.25f;
	private float redstoneMoveTimeLeft = redstoneMovePeriod;
	private float generalMoveTimeLeft = generalMovePeriod;

	private void moveAllBlocks()
	{
		this.generalMoveTimeLeft -= (float)Main.getFrameDuration();
		if(this.generalMoveTimeLeft <= 0)
		{
			this.generalMoveTimeLeft += generalMovePeriod;
			moveGeneral();
		}
		this.redstoneMoveTimeLeft -= (float)Main.getFrameDuration();
		if(this.redstoneMoveTimeLeft <= 0)
		{
			this.redstoneMoveTimeLeft += redstoneMovePeriod;
			moveRedstone();
			movePistons();
		}
	}

	private double particleGenTime = 0;

	private void addParticles()
	{
		double lastTime = this.particleGenTime;
		this.particleGenTime += Main.getFrameDuration();
		double curTime = this.particleGenTime;
		EvalNode node = removeAllEvalNodes(EvalType.Particles);
		while(node != null)
		{
			Block b = getBlock(node.x, node.y, node.z);
			if(b != null)
				b.generateParticles(node.x, node.y, node.z, lastTime, curTime);
			node = node.next;
		}
	}

	private double curTime = 0.0;

	/**
	 * moves everything in this world except the players
	 */
	public void move()
	{
		this.curTime += Main.getFrameDuration();
		final float dayDuration = 20.0f * 60.0f;
		setTimeOfDay(this.timeOfDay + (float)Main.getFrameDuration()
		        / dayDuration);
		addParticles();
		moveEntities();
		checkAllTimedInvalidates();
		moveAllBlocks();
		generateAllTrees();
		updateLight();
	}

	/**
	 * @return the current game time
	 */
	public double getCurTime()
	{
		return this.curTime;
	}

	static final class BlockHitDescriptor
	{
		public Block b;
		public int x, y, z, orientation;

		public BlockHitDescriptor(int x, int y, int z, int orientation, Block b)
		{
			this.b = b;
			this.x = x;
			this.y = y;
			this.z = z;
			this.orientation = orientation;
		}

		public BlockHitDescriptor()
		{
			this(0, 0, 0, -1, null);
		}
	}

	private BlockHitDescriptor
	    internalGetPointedAtBlock(final Vector pos_in,
	                              final Vector dir_in,
	                              float maxDist,
	                              boolean getBlockRightBefore)
	{
		int finishx = 0, finishy = 0, finishz = 0, orientation = -1;
		Vector pos = new Vector(pos_in);
		Vector dir = new Vector(dir_in);
		final float eps = 1e-4f;
		if(Math.abs(dir.x) < eps)
			dir.x = eps;
		if(Math.abs(dir.y) < eps)
			dir.y = eps;
		if(Math.abs(dir.z) < eps)
			dir.z = eps;
		Vector invdir = new Vector(1.0f / dir.x, 1.0f / dir.y, 1.0f / dir.z);
		int ix = (int)Math.floor(pos.x);
		int iy = (int)Math.floor(pos.y);
		int iz = (int)Math.floor(pos.z);
		int previx = 0, previy = 0, previz = 0;
		boolean hasprev = false;
		int lasthit = -1;
		Block prevb = null;
		Block b = getBlock(ix, iy, iz);
		if(b == null)
			return new BlockHitDescriptor();
		boolean passthruwater = false;
		if(b.getType() == BlockType.BTWater)
			passthruwater = true;
		float totalt = 0.0f;
		Vector nextxinc = new Vector((dir.x < 0) ? -1 : 1, 0, 0);
		Vector nextyinc = new Vector(0, (dir.y < 0) ? -1 : 1, 0);
		Vector nextzinc = new Vector(0, 0, (dir.z < 0) ? -1 : 1);
		int fixx = 0, fixy = 0, fixz = 0;
		if(dir.x < 0)
			fixx = -1;
		if(dir.y < 0)
			fixy = -1;
		if(dir.z < 0)
			fixz = -1;
		Vector vt = new Vector();
		Vector nextx = new Vector(), nexty = new Vector(), nextz = new Vector();
		nextx.x = ((dir.x < 0) ? (float)Math.ceil(pos.x) - 1
		        : (float)Math.floor(pos.x) + 1);
		nexty.y = ((dir.y < 0) ? (float)Math.ceil(pos.y) - 1
		        : (float)Math.floor(pos.y) + 1);
		nextz.z = ((dir.z < 0) ? (float)Math.ceil(pos.z) - 1
		        : (float)Math.floor(pos.z) + 1);
		vt.x = (nextx.x - pos.x) * invdir.x;
		vt.y = (nexty.y - pos.y) * invdir.y;
		vt.z = (nextz.z - pos.z) * invdir.z;
		nextx.y = vt.x * dir.y + pos.y;
		nextx.z = vt.x * dir.z + pos.z;
		nexty.x = vt.y * dir.x + pos.x;
		nexty.z = vt.y * dir.z + pos.z;
		nextz.x = vt.z * dir.x + pos.x;
		nextz.y = vt.z * dir.y + pos.y;
		Vector vtinc = new Vector(Math.abs(invdir.x),
		                          Math.abs(invdir.y),
		                          Math.abs(invdir.z));
		nextxinc.y = vtinc.x * dir.y;
		nextxinc.z = vtinc.x * dir.z;
		nextyinc.x = vtinc.y * dir.x;
		nextyinc.z = vtinc.y * dir.z;
		nextzinc.x = vtinc.z * dir.x;
		nextzinc.y = vtinc.z * dir.y;
		// int i = 0;
		while(b != null
		        && (b.getType() == BlockType.BTEmpty
		                || (b.getType() == BlockType.BTWater && passthruwater) || !b.rayIntersects(dir,
		                                                                                           invdir,
		                                                                                           pos.sub(new Vector(ix,
		                                                                                                              iy,
		                                                                                                              iz)),
		                                                                                           pos.sub(new Vector(ix,
		                                                                                                              iy,
		                                                                                                              iz))))/* && i++ < 1*/)
		{
			hasprev = true;
			previx = ix;
			previy = iy;
			previz = iz;
			prevb = b;
			float t;
			Vector newpos;
			if(vt.x < vt.y)
			{
				if(vt.x < vt.z)
				{
					t = vt.x;
					newpos = nextx;
					ix = (int)Math.floor(newpos.x);
					iy = (int)Math.floor(newpos.y);
					iz = (int)Math.floor(newpos.z);
					ix += fixx;
					vt = vt.sub(new Vector(t));
					nextx = nextx.add(nextxinc);
					vt.x = vtinc.x;
					lasthit = 0;
				}
				else
				{
					t = vt.z;
					newpos = nextz;
					ix = (int)Math.floor(newpos.x);
					iy = (int)Math.floor(newpos.y);
					iz = (int)Math.floor(newpos.z);
					iz += fixz;
					vt = vt.sub(new Vector(t));
					nextz = nextz.add(nextzinc);
					vt.z = vtinc.z;
					lasthit = 1;
				}
			}
			else
			{
				if(vt.y < vt.z)
				{
					t = vt.y;
					newpos = nexty;
					ix = (int)Math.floor(newpos.x);
					iy = (int)Math.floor(newpos.y);
					iz = (int)Math.floor(newpos.z);
					iy += fixy;
					vt = vt.sub(new Vector(t));
					nexty = nexty.add(nextyinc);
					vt.y = vtinc.y;
					lasthit = 4;
				}
				else
				{
					t = vt.z;
					newpos = nextz;
					ix = (int)Math.floor(newpos.x);
					iy = (int)Math.floor(newpos.y);
					iz = (int)Math.floor(newpos.z);
					iz += fixz;
					vt = vt.sub(new Vector(t));
					nextz = nextz.add(nextzinc);
					vt.z = vtinc.z;
					lasthit = 1;
				}
			}
			pos = newpos;
			totalt += t;
			if(totalt > maxDist)
				return new BlockHitDescriptor();
			b = getBlock(ix, iy, iz);
		}
		if(b != null)
		{
			if(getBlockRightBefore)
			{
				if(!hasprev)
				{
					return new BlockHitDescriptor();
				}
				finishx = previx;
				finishy = previy;
				finishz = previz;
				b = prevb;
				dir = dir.neg(); // swap lasthit
			}
			else
			{
				finishx = ix;
				finishy = iy;
				finishz = iz;
			}
			switch(lasthit)
			{
			case -1:
				break;
			case 0:
				if(dir.x < 0)
					lasthit = 2;
				break;
			case 1:
				if(dir.z < 0)
					lasthit = 3;
				break;
			// case 4:
			default:
				if(dir.y < 0)
					lasthit = 5;
				break;
			}
			orientation = lasthit;
		}
		return new BlockHitDescriptor(finishx, finishy, finishz, orientation, b);
	}

	BlockHitDescriptor getPointedAtBlock(Matrix worldToCamera,
	                                     float maxDist,
	                                     boolean getBlockRightBefore)
	{
		Vector org = new Vector(0, 0, 0);
		Vector dir = new Vector(0, 0, -1);
		Matrix cameraToWorld = worldToCamera.invert();
		org = cameraToWorld.apply(org);
		dir = cameraToWorld.apply(dir).sub(org).normalize();
		return internalGetPointedAtBlock(org, dir, maxDist, getBlockRightBefore);
	}

	private static final class TreeGenerateLocation
	{
		public final int x, y, z;
		public final Block startingSapling;
		public TreeGenerateLocation next;

		public TreeGenerateLocation(int x,
		                            int y,
		                            int z,
		                            Block b,
		                            TreeGenerateLocation next)
		{
			this.x = x;
			this.y = y;
			this.z = z;
			this.startingSapling = b;
			this.next = next;
		}
	}

	private TreeGenerateLocation treeGenerateListHead = null;

	private void generateAllTrees()
	{
		while(this.treeGenerateListHead != null)
		{
			TreeGenerateLocation next = this.treeGenerateListHead.next;
			this.treeGenerateListHead.next = null;
			Tree.generate(this.treeGenerateListHead.startingSapling,
			              this.treeGenerateListHead.x,
			              this.treeGenerateListHead.y,
			              this.treeGenerateListHead.z);
			this.treeGenerateListHead = next;
		}
	}

	/**
	 * add a new tree
	 * 
	 * @param x
	 *            new tree's x coordinate
	 * @param y
	 *            new tree's y coordinate
	 * @param z
	 *            new tree's z coordinate
	 * @param startingSapling
	 *            the sapling that created this tree
	 */
	public void addNewTree(int x, int y, int z, Block startingSapling)
	{
		this.treeGenerateListHead = new TreeGenerateLocation(x,
		                                                     y,
		                                                     z,
		                                                     startingSapling,
		                                                     this.treeGenerateListHead);
	}

	private static final int fileVersion = 1;

	/**
	 * write to a <code>DataOutput</code>
	 * 
	 * @param o
	 *            <code>OutputStream</code> to write to
	 * @throws IOException
	 *             the exception thrown
	 */
	public static void write(DataOutput o) throws IOException
	{
		o.writeInt(fileVersion);
		o.writeInt(world.landGenerator.getSeed());
		o.writeLong(randSeed);
		o.writeFloat(world.timeOfDay);
		int chunkcount = 0;
		for(Chunk c = world.chunksHead; c != null; c = c.listnext)
		{
			if(!world.isGenerated(c.orgx, c.orgy, c.orgz))
				continue;
			chunkcount++;
		}
		o.writeInt(chunkcount);
		Main.pushProgress(0.0f, 0.9f);
		if(chunkcount > 0)
		{
			Main.pushProgress(0, 1.0f / chunkcount);
			int curChunkCount = 0;
			for(Chunk c = world.chunksHead; c != null; c = c.listnext, curChunkCount++)
			{
				if(!world.isGenerated(c.orgx, c.orgy, c.orgz))
					continue;
				Main.pushProgress(curChunkCount, 1.0f / Chunk.size);
				o.writeInt(c.orgx);
				o.writeInt(c.orgy);
				o.writeInt(c.orgz);
				for(int x = 0; x < Chunk.size; x++)
				{
					Main.setProgress(x);
					for(int y = 0; y < Chunk.size; y++)
					{
						for(int z = 0; z < Chunk.size; z++)
						{
							c.getBlock(x, y, z).write(o);
						}
					}
				}
				Main.popProgress();
			}
			Main.popProgress();
		}
		Main.popProgress();
		Main.pushProgress(0.9f, 0.05f);
		int entitycount = 0;
		for(EntityNode node = world.entityHead; node != null; node = node.next)
		{
			entitycount++;
		}
		o.writeInt(entitycount);
		if(world.entityHead != null)
		{
			Main.pushProgress(0, 1.0f / entitycount);
			int progress = 0;
			for(EntityNode node = world.entityHead; node != null; node = node.next)
			{
				node.e.write(o);
				Main.setProgress(progress++);
			}
			Main.popProgress();
		}
		Main.popProgress();
		Main.pushProgress(0.95f, 0.05f);
		o.writeShort(EvalTypeCount);
		Main.pushProgress(0, 1.0f / (EvalTypeCount + 1));
		for(int evalTypei = 0; evalTypei < EvalTypeCount; evalTypei++)
		{
			EvalType evalType = EvalType.values()[evalTypei];
			EvalNode head = world.removeAllEvalNodes(evalType);
			int evalNodeCount = 0;
			for(EvalNode n = head; n != null; n = n.next)
			{
				evalNodeCount++;
			}
			o.writeInt(evalNodeCount);
			if(evalNodeCount > 0)
			{
				Main.pushProgress(evalTypei, 1.0f / evalNodeCount);
				int progress = 0;
				for(EvalNode n = head, nextNode = (head != null ? head.next
				        : null); n != null; n = nextNode, nextNode = (n != null ? n.next
				        : null))
				{
					o.writeInt(n.x);
					o.writeInt(n.y);
					o.writeInt(n.z);
					o.writeBoolean(n.b != null);
					if(n.b != null)
						n.b.write(o);
					world.insertEvalNode(evalType, n);
					Main.setProgress(progress++);
				}
				Main.popProgress();
			}
		}
		int timedInvalidateCount = 0;
		for(TimedInvalidate ti = world.timedInvalidateHead; ti != null; ti = ti.next)
		{
			timedInvalidateCount++;
		}
		o.writeInt(timedInvalidateCount);
		if(timedInvalidateCount > 0)
		{
			Main.pushProgress(EvalTypeCount, 1.0f / timedInvalidateCount);
			int progress = 0;
			for(TimedInvalidate ti = world.timedInvalidateHead; ti != null; ti = ti.next)
			{
				o.writeInt(ti.x);
				o.writeInt(ti.y);
				o.writeInt(ti.z);
				o.writeDouble(ti.timeLeft);
				Main.setProgress(progress++);
			}
			Main.popProgress();
		}
		Main.popProgress();
		Main.popProgress();
	}

	/**
	 * read from a <code>DataInput</code>
	 * 
	 * @param i
	 *            <code>DataInput</code> to read from
	 * @throws IOException
	 *             the exception thrown
	 */
	public static void read(DataInput i) throws IOException
	{
		int v = i.readInt();
		if(v != fileVersion)
		{
			readVer0(i, v);
			return;
		}
		int seed = i.readInt();
		clear(seed);
		randSeed = i.readLong();
		float timeOfDay = i.readFloat();
		if(Float.isInfinite(timeOfDay) || Float.isNaN(timeOfDay)
		        || timeOfDay < 0 || timeOfDay >= 1)
			throw new IOException("time of day is out of range");
		world.setTimeOfDay(timeOfDay);
		int chunkcount = i.readInt();
		if(chunkcount < 0)
			throw new IOException("chunk count out of range");
		if(chunkcount > 0)
		{
			Main.pushProgress(0, 0.9f);
			Main.pushProgress(0, 1.0f / chunkcount);
			int progress = 0;
			while(chunkcount-- > 0)
			{
				int cx = i.readInt();
				int cy = i.readInt();
				int cz = i.readInt();
				if(cx != getChunkX(cx) || cy != getChunkY(cy)
				        || cz != getChunkZ(cz))
					throw new IOException("chunk origin not valid");
				Main.pushProgress(progress++, 1.0f / Chunk.size);
				for(int x = cx; x < cx + Chunk.size; x++)
				{
					for(int y = cy; y < cy + Chunk.size; y++)
					{
						for(int z = cz; z < cz + Chunk.size; z++)
						{
							world.internalSetBlock(x, y, z, Block.read(i));
						}
					}
					Main.setProgress(x);
				}
				world.setGenerated(cx, cy, cz, true);
				Main.popProgress();
			}
			Main.popProgress();
			Main.popProgress();
		}
		int entitycount = i.readInt();
		if(entitycount < 0)
			throw new IOException("entity count out of range");
		if(entitycount > 0)
		{
			Main.pushProgress(0.9f, 0.05f);
			int progress = 0;
			while(entitycount-- > 0)
			{
				world.insertEntity(Entity.read(i));
				Main.setProgress(progress++);
			}
			Main.popProgress();
		}
		int evalTypeCount = i.readUnsignedShort();
		if(evalTypeCount > EvalTypeCount)
			throw new IOException("EvalTypeCount is too big");
		Main.pushProgress(0.95f, 0.05f);
		Main.pushProgress(0, 1.0f / (evalTypeCount + 1));
		for(int evalTypei = 0; evalTypei < evalTypeCount; evalTypei++)
		{
			EvalType evalType = EvalType.values()[evalTypei];
			int evalNodeCount = i.readInt();
			if(evalNodeCount < 0)
				throw new IOException("invalid eval node count");
			if(evalNodeCount > 0)
			{
				Main.pushProgress(evalTypei, 1.0f / evalNodeCount);
				int progress = 0;
				while(evalNodeCount-- > 0)
				{
					int x = i.readInt();
					int y = i.readInt();
					int z = i.readInt();
					boolean hasBlock = i.readBoolean();
					if(hasBlock)
						world.insertEvalNode(evalType, x, y, z, Block.read(i));
					else
						world.insertEvalNode(evalType, x, y, z);
					Main.setProgress(progress++);
				}
				Main.popProgress();
			}
		}
		int timedInvalidateCount = i.readInt();
		if(timedInvalidateCount < 0)
			throw new IOException("invalid timed invalidate count");
		if(timedInvalidateCount > 0)
		{
			Main.pushProgress(evalTypeCount, 1.0f / timedInvalidateCount);
			int progress = 0;
			while(timedInvalidateCount-- > 0)
			{
				int x = i.readInt();
				int y = i.readInt();
				int z = i.readInt();
				double timeLeft = i.readDouble();
				if(Double.isNaN(timeLeft) || Double.isInfinite(timeLeft)
				        || timeLeft < 0)
					throw new IOException("invalid timed invalidate time left");
				world.addTimedInvalidate(x, y, z, timeLeft);
				Main.setProgress(progress++);
			}
			Main.popProgress();
		}
		Main.popProgress();
		Main.popProgress();
	}

	private static void readVer0(DataInput i, int v) throws IOException
	{
		if(v != 0)
			throw new IOException("file version doesn't match");
		int seed = i.readInt();
		clear(seed);
		randSeed = i.readLong();
		float timeOfDay = i.readFloat();
		if(Float.isInfinite(timeOfDay) || Float.isNaN(timeOfDay)
		        || timeOfDay < 0 || timeOfDay >= 1)
			throw new IOException("time of day is out of range");
		world.setTimeOfDay(timeOfDay);
		int chunkcount = i.readInt();
		if(chunkcount < 0)
			throw new IOException("chunk count out of range");
		while(chunkcount-- > 0)
		{
			int cx = i.readInt();
			int cy = i.readInt();
			int cz = i.readInt();
			if(cx != getChunkX(cx) || cy != getChunkY(cy)
			        || cz != getChunkZ(cz))
				throw new IOException("chunk origin not valid");
			for(int x = cx; x < cx + Chunk.size; x++)
			{
				for(int y = cy; y < cy + Chunk.size; y++)
				{
					for(int z = cz; z < cz + Chunk.size; z++)
					{
						world.setBlock(x, y, z, Block.read(i));
					}
				}
			}
			world.setGenerated(cx, cy, cz, true);
		}
		int entitycount = i.readInt();
		if(entitycount < 0)
			throw new IOException("entity count out of range");
		while(entitycount-- > 0)
		{
			world.insertEntity(Entity.read(i));
		}
	}

	/**
	 * clears <code>world</code> to a new world
	 * 
	 * @param seed
	 *            the new seed
	 */
	public static void clear(int seed)
	{
		world = new World();
		world.setSeed(seed);
	}

	/**
	 * clears <code>world</code> to a new world
	 */
	public static void clear()
	{
		world = new World();
	}
}
