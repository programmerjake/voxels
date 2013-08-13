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

import static org.voxels.Color.*;
import static org.voxels.Matrix.glLoadMatrix;
import static org.voxels.PlayerList.players;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.voxels.BlockType.ToolType;
import org.voxels.generate.*;

/** @author jacob */
public class World
{
    /** the program's world */
    public static World world = new World();
    /** the maximum height<br/>
     * <code>-Depth &lt;= y &lt; Height</code>
     * 
     * @see #Depth */
    public static final int Height = 10000;
    /** the maximum depth<br/>
     * <code>-Depth &lt;= y &lt; Height</code>
     * 
     * @see #Height */
    public static final int Depth = 64;
    /** gravitational acceleration */
    public static final float GravityAcceleration = 9.8f;
    private static long randSeed = new Random().nextLong();
    static int viewDist = 10;
    private long displayListValidTag = 0;
    private Rand.Settings landGeneratorSettings = null;
    private Rand landGenerator = Rand.create(this.landGeneratorSettings);
    private static final int generatedChunkScale = 1 << 0; // must be power of 2

    /** @param landGeneratorSettings
     *            the new land generator settings */
    public void
        setLandGeneratorSettings(final Rand.Settings landGeneratorSettings)
    {
        this.landGeneratorSettings = landGeneratorSettings;
        this.landGenerator = Rand.create(this.landGeneratorSettings);
    }

    /** generate a random <code>float</code>
     * 
     * @param min
     *            minimum value
     * @param max
     *            maximum value
     * @return a random float in the range [<code>min</code>, <code>max</code>) */
    public synchronized static float fRand(final float min, final float max)
    {
        randSeed = (randSeed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
        return ((int)(randSeed >>> (48 - 24)) / ((float)(1 << 24)))
                * (max - min) + min;
    }

    /** generate a random <code>Vector</code>
     * 
     * @param magnitude
     *            the magnitude of the resulting vector
     * @return a random vector
     * @see #vRand() */
    @Deprecated
    public static Vector vRand(final float magnitude)
    {
        Vector retval = Vector.allocate();
        do
        {
            retval.setX(fRand(-1.0f, 1.0f));
            retval.setY(fRand(-1.0f, 1.0f));
            retval.setZ(fRand(-1.0f, 1.0f));
        }
        while(retval.abs_squared() >= 1.0f
                || retval.abs_squared() < 1e-3f * 1e-3f);
        return retval.normalizeAndSet().mulAndSet(magnitude);
    }

    /** generate a random unit <code>Vector</code>
     * 
     * @return a random vector
     * @see #vRand(float magnitude) */
    @Deprecated
    public static Vector vRand()
    {
        Vector retval = Vector.allocate();
        do
        {
            retval.setX(fRand(-1.0f, 1.0f));
            retval.setY(fRand(-1.0f, 1.0f));
            retval.setZ(fRand(-1.0f, 1.0f));
        }
        while(retval.abs_squared() >= 1.0f
                || retval.abs_squared() < 1e-3f * 1e-3f);
        return retval.normalizeAndSet();
    }

    /** generate a random <code>Vector</code>
     * 
     * @param dest
     *            the destination vector
     * @param magnitude
     *            the magnitude of the resulting vector
     * @return a random vector
     * @see #vRand() */
    public static Vector vRand(final Vector dest, final float magnitude)
    {
        Vector retval = dest;
        do
        {
            retval.setX(fRand(-1.0f, 1.0f));
            retval.setY(fRand(-1.0f, 1.0f));
            retval.setZ(fRand(-1.0f, 1.0f));
        }
        while(retval.abs_squared() >= 1.0f
                || retval.abs_squared() < 1e-3f * 1e-3f);
        return retval.normalizeAndSet().mulAndSet(magnitude);
    }

    /** generate a random unit <code>Vector</code>
     * 
     * @param dest
     *            the destination vector
     * @return a random vector
     * @see #vRand(float magnitude) */
    public static Vector vRand(final Vector dest)
    {
        Vector retval = dest;
        do
        {
            retval.setX(fRand(-1.0f, 1.0f));
            retval.setY(fRand(-1.0f, 1.0f));
            retval.setZ(fRand(-1.0f, 1.0f));
        }
        while(retval.abs_squared() >= 1.0f
                || retval.abs_squared() < 1e-3f * 1e-3f);
        return retval.normalizeAndSet();
    }

    private static class EntityNode
    {
        private EntityNode()
        {
        }

        public EntityNode hashnext;
        public EntityNode hashprev;
        public EntityNode next;
        public EntityNode prev;
        public Entity e;

        public void free()
        {
            this.hashnext = null;
            this.hashprev = null;
            this.prev = null;
            if(this.e != null)
                this.e.free();
            this.e = null;
            this.next = freeEntityNodeHead;
            freeEntityNodeHead = this;
        }

        private static EntityNode freeEntityNodeHead = null;

        public static EntityNode allocate()
        {
            if(freeEntityNodeHead == null)
                return new EntityNode();
            EntityNode retval = freeEntityNodeHead;
            freeEntityNodeHead = retval.next;
            return retval;
        }
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
        public long displayListValidTag[] = new long[drawPhaseCount];
        @SuppressWarnings("unused")
        public EntityNode head = null, tail = null;
        public boolean drawsAnything = true;
        public long drawsAnythingValidTag = -1;

        public Chunk(final int ox, final int oy, final int oz)
        {
            this.orgx = ox;
            this.orgy = oy;
            this.orgz = oz;
            for(int i = 0; i < drawPhaseCount; i++)
            {
                this.displayListValidTag[i] = -1;
            }
        }

        public Block getBlock(final int cx, final int cy, final int cz)
        {
            int index = cx + size * (cy + size * cz);
            return this.blocks[index];
        }

        public void setBlock(final int cx,
                             final int cy,
                             final int cz,
                             final Block b)
        {
            int index = cx + size * (cy + size * cz);
            this.blocks[index] = b;
        }

        public boolean isGenerated(final int cx_in,
                                   final int cy_in,
                                   final int cz_in)
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

        public void setGenerated(final int cx_in,
                                 final int cy_in,
                                 final int cz_in,
                                 final boolean g)
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

        public void invalidate()
        {
            this.drawsAnythingValidTag = -1;
            this.drawsAnything = true;
            for(int i = 0; i < drawPhaseCount; i++)
            {
                this.displayListValidTag[i] = -1;
            }
        }
    }

    private Chunk lastChunk = null;

    private void insertEntity(final EntityNode node)
    {
        Vector pos = node.e.getPosition();
        int x = (int)Math.floor(pos.getX());
        int y = (int)Math.floor(pos.getY());
        int z = (int)Math.floor(pos.getZ());
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
                int x = (int)Math.floor(pos.getX());
                int y = (int)Math.floor(pos.getY());
                int z = (int)Math.floor(pos.getZ());
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
        private EvalNode(final int x, final int y, final int z, final int hash)
        {
            this.x = x;
            this.y = y;
            this.z = z;
            this.hash = hash;
        }

        public EvalNode hashnext = null;
        public EvalNode listnext = null;
        public Block b = null;
        public int x, y, z;
        public int hash;
        private static EvalNode freeNodeHead = null;

        public void free()
        {
            this.hashnext = null;
            this.listnext = freeNodeHead;
            this.b = null;
            freeNodeHead = this;
        }

        public static EvalNode allocate(final int x,
                                        final int y,
                                        final int z,
                                        final int hash)
        {
            if(freeNodeHead == null)
                return new EvalNode(x, y, z, hash);
            EvalNode retval = freeNodeHead;
            freeNodeHead = retval.listnext;
            retval.x = x;
            retval.y = y;
            retval.z = z;
            retval.hash = hash;
            return retval;
        }
    }

    private enum EvalType
    {
        General, Redstone, RedstoneFirst, Lighting, Particles, Pistons, Last;
        public static final EvalType[] values = values();
    }

    private static final int EvalTypeCount = EvalType.Last.ordinal();

    private static int getChunkX(final int v)
    {
        return v - (v % Chunk.size + Chunk.size) % Chunk.size;
    }

    private static int getChunkY(final int v)
    {
        return v - (v % Chunk.size + Chunk.size) % Chunk.size;
    }

    private static int getChunkZ(final int v)
    {
        return v - (v % Chunk.size + Chunk.size) % Chunk.size;
    }

    private static int getGeneratedChunkX(final int v)
    {
        return v - (v % Chunk.generatedChunkSize + Chunk.generatedChunkSize)
                % Chunk.generatedChunkSize;
    }

    private static int getGeneratedChunkY(final int v)
    {
        return v - (v % Chunk.generatedChunkSize + Chunk.generatedChunkSize)
                % Chunk.generatedChunkSize;
    }

    private static int getGeneratedChunkZ(final int v)
    {
        return v - (v % Chunk.generatedChunkSize + Chunk.generatedChunkSize)
                % Chunk.generatedChunkSize;
    }

    private static final int WorldHashPrimePowOf2 = 17;
    private static final int WorldHashPrime = (1 << WorldHashPrimePowOf2) - 1;

    private static int ModWorldHashPrime(final int v_in)
    {
        int v = v_in;
        v = (v >>> WorldHashPrimePowOf2) + (v & WorldHashPrime);
        if(v >= WorldHashPrime)
            v = (v >>> WorldHashPrimePowOf2) + (v & WorldHashPrime);
        if(v >= WorldHashPrime)
            v -= WorldHashPrime;
        return v;
    }

    private static int hashPos(final int x, final int y, final int z)
    {
        return ModWorldHashPrime((x * 9 + y) * 9 + z);
    }

    private int hashChunkPos(final int cx, final int cy, final int cz)
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
    private EvalNode[] evalNodeListHead = new EvalNode[EvalType.Last.ordinal()];

    private void insertEvalNode(final EvalType et, final EvalNode newnode)
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
                    lastNode.hashnext = node.hashnext;
                    node.hashnext = this.evalNodeHashTable[eti][hash];
                    this.evalNodeHashTable[eti][hash] = node;
                }
                node.b = newnode.b;
                return;
            }
            lastNode = node;
            node = node.hashnext;
        }
        node = newnode;
        node.hashnext = this.evalNodeHashTable[eti][hash];
        this.evalNodeHashTable[eti][hash] = node;
        node.listnext = this.evalNodeListHead[eti];
        this.evalNodeListHead[eti] = node;
    }

    private void insertEvalNode(final EvalType et,
                                final int x,
                                final int y,
                                final int z,
                                final Block b)
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
                    lastNode.hashnext = node.hashnext;
                    node.hashnext = this.evalNodeHashTable[eti][hash];
                    this.evalNodeHashTable[eti][hash] = node;
                }
                node.b = b;
                return;
            }
            lastNode = node;
            node = node.hashnext;
        }
        node = EvalNode.allocate(x, y, z, hash);
        node.hashnext = this.evalNodeHashTable[eti][hash];
        this.evalNodeHashTable[eti][hash] = node;
        node.b = b;
        node.listnext = this.evalNodeListHead[eti];
        this.evalNodeListHead[eti] = node;
    }

    private EvalNode removeAllEvalNodes(final EvalType et)
    {
        int eti = et.ordinal();
        assert eti >= 0 && eti < EvalTypeCount;
        EvalNode retval = this.evalNodeListHead[eti];
        this.evalNodeListHead[eti] = null;
        for(EvalNode node = retval; node != null; node = node.listnext)
        {
            this.evalNodeHashTable[eti][node.hash] = null;
        }
        return retval;
    }

    private void invalidateChunk(final int cx, final int cy, final int cz)
    {
        Chunk c = find(cx, cy, cz);
        if(c == null)
            return;
        c.invalidate();
    }

    private void insertEvalNode(final EvalType et,
                                final int x,
                                final int y,
                                final int z)
    {
        insertEvalNode(et, x, y, z, null);
    }

    private void invalidate(final int x, final int y, final int z)
    {
        if(y < -Depth || y >= Height)
            return;
        invalidateChunk(getChunkX(x), getChunkY(y), getChunkZ(z));
        for(int i = 0; i < EvalType.values.length; i++)
        {
            EvalType et = EvalType.values[i];
            if(et == EvalType.Last || et == EvalType.Particles)
                continue;
            insertEvalNode(et, x, y, z);
        }
    }

    private static class TimedInvalidate
    {
        public final int x, y, z;
        public double timeLeft;
        public TimedInvalidate next;

        public TimedInvalidate(final int x,
                               final int y,
                               final int z,
                               final double timeLeft)
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

        public void advanceTime(final double deltatime)
        {
            this.timeLeft -= deltatime;
            if(this.timeLeft < 0.0)
                this.timeLeft = 0.0;
        }
    }

    private TimedInvalidate timedInvalidateHead = null;

    /** add a block invalidate in the future
     * 
     * @param x
     *            x coordinate
     * @param y
     *            y coordinate
     * @param z
     *            z coordinate
     * @param seconds
     *            relative time to run */
    public void addTimedInvalidate(final int x,
                                   final int y,
                                   final int z,
                                   final double seconds)
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

    private void addParticleGen(final int x, final int y, final int z)
    {
        insertEvalNode(EvalType.Particles, x, y, z);
    }

    private Chunk find(final int cx, final int cy, final int cz)
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

    private Chunk findOrInsert(final int cx, final int cy, final int cz)
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

    private boolean isGenerated(final int cx, final int cy, final int cz)
    {
        Chunk c = find(getChunkX(getGeneratedChunkX(cx)),
                       getChunkY(getGeneratedChunkY(cy)),
                       getChunkZ(getGeneratedChunkZ(cz)));
        if(c == null)
            return false;
        return c.isGenerated(cx, cy, cz);
    }

    private void setGenerated(final int cx,
                              final int cy,
                              final int cz,
                              final boolean g)
    {
        Chunk c = find(getChunkX(getGeneratedChunkX(cx)),
                       getChunkY(getGeneratedChunkY(cy)),
                       getChunkZ(getGeneratedChunkZ(cz)));
        if(c == null)
            return;
        c.setGenerated(cx, cy, cz, g);
    }

    /** gets the block at (<code>x</code>, <code>y</code>, <code>z</code>)
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
     * @see #setBlock(int x, int y, int z, Block block) */
    public Block getBlock(final int x, final int y, final int z)
    {
        int cx = getChunkX(x);
        int cy = getChunkY(y);
        int cz = getChunkZ(z);
        Chunk c = find(cx, cy, cz);
        if(c == null)
            return null;
        return c.getBlock(x - cx, y - cy, z - cz);
    }

    private void internalSetBlock(final int x,
                                  final int y,
                                  final int z,
                                  final Block b)
    {
        int cx = getChunkX(x);
        int cy = getChunkY(y);
        int cz = getChunkZ(z);
        Chunk c = findOrInsert(cx, cy, cz);
        c.setBlock(x - cx, y - cy, z - cz, b);
    }

    private void resetLightingArrays(final int x, final int y, final int z)
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

    @SuppressWarnings("unused")
    private void addGeneratedChunk(final GeneratedChunk c)
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
                                // TODO finish
                                if(true)
                                    setBlock(x, y, z, c.getBlock(x, y, z));
                                else
                                    internalSetBlock(x,
                                                     y,
                                                     z,
                                                     c.getBlock(x, y, z));
                            }
                        }
                    }
                    setGenerated(cx, cy, cz, true);
                }
            }
        }
    }

    /** sets the block at (<code>x</code>, <code>y</code>, <code>z</code>)
     * 
     * @param x
     *            the x coordinate of the block to get
     * @param y
     *            the y coordinate of the block to get
     * @param z
     *            the z coordinate of the block to get
     * @param block
     *            the new block
     * @see #getBlock(int x, int y, int z) */
    public void setBlock(final int x,
                         final int y,
                         final int z,
                         final Block block)
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

    /** gets the block at (<code>x</code>, <code>y</code>, <code>z</code>) for
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
     * @see #getBlock(int x, int y, int z) */
    public Block getBlockEval(final int x, final int y, final int z)
    {
        if(!isGenerated(getGeneratedChunkX(x),
                        getGeneratedChunkY(y),
                        getGeneratedChunkZ(z)))
            return null;
        return getBlock(x, y, z);
    }

    private int GetSunlight(final int x, final int y, final int z)
    {
        if(y < -Depth)
            return 0;
        if(y >= Height)
            return 15;
        Block b = getBlock(x, y, z);
        if(b == null)
        {
            if(y > this.landGenerator.getRockHeight(x, z))
                return Math.min(15, 15 + (y - Rand.WaterHeight) * 3);
            return 0;
        }
        return b.getSunlight();
    }

    int GetScatteredSunlight(final int x, final int y, final int z)
    {
        if(y < -Depth)
            return 0;
        if(y >= Height)
            return 15;
        Block b = getBlock(x, y, z);
        if(b == null)
        {
            if(y > this.landGenerator.getRockHeight(x, z))
                return Math.min(15, 15 + (y - Rand.WaterHeight) * 3);
            return 0;
        }
        return b.getScatteredSunlight();
    }

    int GetLight(final int x, final int y, final int z)
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
                    node = node.listnext;
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
                EvalNode freeMe = node;
                node = node.listnext;
                freeMe.free();
            }
        }
    }

    private int sunlightFactor = 15; // integer between 0 and 15
    private float timeOfDay = 0.3f;

    private static Color getBackgroundColor(final float timeOfDay)
    {
        float seconds = 20.0f * 60.0f * timeOfDay;
        final float secondsPerLightlevel = 10.0f;
        final int nightLight = 4, dayLight = 15;
        final float secondsForDawn = (dayLight - nightLight)
                * secondsPerLightlevel;
        final float secondsForDusk = (dayLight - nightLight)
                * secondsPerLightlevel;
        float sunlightFactor;
        if(seconds < 5.0f * 60.0f - secondsForDawn)
        {
            sunlightFactor = nightLight;
        }
        else if(seconds < 5.0f * 60.0f)
        {
            float brightness = (secondsForDawn + seconds - 5.0f * 60.0f)
                    / secondsPerLightlevel;
            sunlightFactor = brightness + nightLight;
        }
        else if(seconds < 15.0f * 60.0f)
        {
            sunlightFactor = 15;
        }
        else if(seconds < 15.0f * 60.0f + secondsForDusk)
        {
            float brightness = (secondsForDusk + 15.0f * 60.0f - seconds)
                    / secondsPerLightlevel;
            sunlightFactor = brightness + nightLight;
        }
        else
            sunlightFactor = nightLight;
        float intensity = 1.5f * (sunlightFactor - nightLight)
                / (dayLight - nightLight);
        if(intensity < 0.0f)
            intensity = 0.0f;
        if(intensity > 1.5f)
            intensity = 1.5f;
        if(intensity > 1.0f)
            return RGB(intensity - 1.0f, intensity - 1.0f, 1.0f);
        return RGB(0.0f, 0.0f, intensity);
    }

    private Color backgroundColor = getBackgroundColor(0.3f);

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

    private Vector sunPosition = Vector.allocate();
    private Vector moonPosition = Vector.allocate();

    private void setSunMoonPosition(final Vector sunPosition,
                                    final float sunIntensity_in,
                                    final Vector moonPosition,
                                    final float moonIntensity_in)
    {
        float sunIntensity = sunIntensity_in;
        float moonIntensity = moonIntensity_in;
        if(sunIntensity < 0)
            sunIntensity = 0;
        if(moonIntensity < 0)
            moonIntensity = 0;
        this.sunPosition = this.sunPosition.set(sunPosition)
                                           .normalizeAndSet()
                                           .mulAndSet(sunIntensity);
        this.moonPosition = this.moonPosition.set(moonPosition)
                                             .normalizeAndSet()
                                             .mulAndSet(moonIntensity);
    }

    /** sets the time of day
     * 
     * @param timeOfDay
     *            the new time of day */
    public void setTimeOfDay(final float timeOfDay)
    {
        this.timeOfDay = timeOfDay - (float)Math.floor(timeOfDay);
        setSunlightFactor();
        setBackgroundColor();
        Vector sunvec = Vector.allocate(-(float)Math.sin(this.timeOfDay * 2.0
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
        Vector nsunvec = Vector.neg(Vector.allocate(), sunvec);
        setSunMoonPosition(sunvec, sunStrength, nsunvec, 0.25f * moonStrength);
        sunvec.free();
        nsunvec.free();
    }

    private int genChunkX = 0, genChunkY = 0, genChunkZ = 0;
    private float genChunkDistance = -1.0f;

    private void addGenChunk(final int cx,
                             final int cy,
                             final int cz,
                             final float distance)
    {
        if(this.genChunkDistance < 0.0f || distance < this.genChunkDistance)
        {
            this.genChunkX = cx;
            this.genChunkY = cy;
            this.genChunkZ = cz;
            this.genChunkDistance = distance;
        }
    }

    /** @param x
     *            the x coordinate
     * @param y
     *            the y coordinate
     * @param z
     *            the z coordinate */
    public void flagGenerate(final int x, final int y, final int z)
    {
        int cx = x - (x % Chunk.generatedChunkSize + Chunk.generatedChunkSize)
                % Chunk.generatedChunkSize;
        int cy = y - (y % Chunk.generatedChunkSize + Chunk.generatedChunkSize)
                % Chunk.generatedChunkSize;
        int cz = z - (z % Chunk.generatedChunkSize + Chunk.generatedChunkSize)
                % Chunk.generatedChunkSize;
        if(isGenerated(cx, cy, cz))
            return;
        this.genChunkX = cx;
        this.genChunkY = cy;
        this.genChunkZ = cz;
        this.genChunkDistance = 0.0f;
    }

    private static final float chunkGenScale = 1.5f;
    private Vector chunkPassClipPlane_t1 = Vector.allocate();

    /** @param p
     * @param a
     * @param b
     * @param c
     * @param d
     * @return true if any p is inside p.getX() * a + p.getY() * b + p.getZ() *
     *         c + d <= 0 */
    private boolean chunkPassClipPlane(final Vector p[],
                                       final float a,
                                       final float b,
                                       final float c,
                                       final float d)
    {
        for(int i = 0; i < p.length; i++)
        {
            if(p[i].dot(Vector.set(this.chunkPassClipPlane_t1, a, b, c)) + d <= 0)
                return true;
        }
        if(p.length <= 0)
            return true;
        return false;
    }

    private final Vector[] chunkVisible_t1;
    {
        this.chunkVisible_t1 = new Vector[8];
        for(int i = 0; i < 8; i++)
            this.chunkVisible_t1[i] = Vector.allocate();
    }

    private boolean chunkVisible(final int cx,
                                 final int cy,
                                 final int cz,
                                 final Matrix worldToCamera)
    {
        Vector p[] = this.chunkVisible_t1;
        for(int i = 0; i < 8; i++)
        {
            Vector v = Vector.set(p[i], cx, cy, cz);
            if((i & 1) != 0)
                v.setX(v.getX() + Chunk.size);
            if((i & 2) != 0)
                v.setY(v.getY() + Chunk.size);
            if((i & 4) != 0)
                v.setZ(v.getZ() + Chunk.size);
            worldToCamera.apply(p[i], v);
        }
        if(!chunkPassClipPlane(p, 0, 0, 1, 0))
            return false;
        if(!chunkPassClipPlane(p, -1 / Main.aspectRatio(), 0, 1, 0))
            return false;
        if(!chunkPassClipPlane(p, 1 / Main.aspectRatio(), 0, 1, 0))
            return false;
        if(!chunkPassClipPlane(p, 0, -1, 1, 0))
            return false;
        if(!chunkPassClipPlane(p, 0, 1, 1, 0))
            return false;
        return true;
    }

    private Matrix drawChunk_t1 = new Matrix();

    private void drawChunk(final RenderingStream rs,
                           final int cx,
                           final int cy,
                           final int cz,
                           final int drawPhase)
    {
        Chunk pnode = find(cx, cy, cz);
        if(pnode == null)
            return;
        if(drawPhase == 0)
        {
            EntityNode e = pnode.head;
            while(e != null)
            {
                e.e.draw(rs, Matrix.IDENTITY);
                e = e.hashnext;
            }
        }
        if(pnode.drawsAnythingValidTag != this.displayListValidTag)
        {
            boolean drawsAnything = false;
            outerloop: for(int x = 0; x < Chunk.size; x++)
            {
                for(int y = 0; y < Chunk.size; y++)
                {
                    for(int z = 0; z < Chunk.size; z++)
                    {
                        Block b = pnode.getBlock(x, y, z);
                        if(b == null)
                            continue;
                        if(b.drawsAnything(x + cx, y + cy, z + cz))
                        {
                            drawsAnything = true;
                            break outerloop;
                        }
                    }
                }
            }
            pnode.drawsAnything = drawsAnything;
            pnode.drawsAnythingValidTag = this.displayListValidTag;
        }
        if(pnode.drawsAnything)
        {
            for(int x = 0; x < Chunk.size; x++)
            {
                for(int y = 0; y < Chunk.size; y++)
                {
                    for(int z = 0; z < Chunk.size; z++)
                    {
                        Block b = pnode.getBlock(x, y, z);
                        if(b == null)
                            continue;
                        if(b.isTranslucent() && drawPhase != 1)
                            continue;
                        if(!b.isTranslucent() && drawPhase != 0)
                            continue;
                        getLightingArray(x + cx, y + cy, z + cz);
                        b = pnode.getBlock(x, y, z);
                        b.draw(rs, Matrix.setToTranslate(this.drawChunk_t1, x
                                + cx, y + cy, z + cz));
                    }
                }
            }
        }
    }

    private static List<Vector> makeStars()
    {
        final int starCount = 200;
        List<Vector> stars = new ArrayList<Vector>(starCount);
        Random rand = new Random(0);
        Vector v = Vector.allocate();
        for(int i = 0; i < starCount; i++)
        {
            do
            {
                v.set(rand.nextFloat() * 2 - 1,
                      rand.nextFloat() * 2 - 1,
                      rand.nextFloat() * 2 - 1);
            }
            while(v.abs_squared() > 1 || v.abs_squared() < 0.0001f);
            v.normalizeAndSet();
            stars.add(v.getImmutable());
        }
        v.free();
        return Collections.unmodifiableList(stars);
    }

    private static final List<Vector> stars = makeStars();
    private Vector draw_t1 = Vector.allocate();
    private Vector draw_t2 = Vector.allocate();
    private Vector draw_cameraPos = Vector.allocate();
    private Matrix draw_t3 = new Matrix();
    private Matrix draw_t4 = new Matrix();
    private Vector draw_chunkCenter = Vector.allocate();
    private static TextureAtlas.TextureHandle starImg = TextureAtlas.addImage(new Image("star.png"));
    private static TextureAtlas.TextureHandle sunsetGlow = TextureAtlas.addImage(new Image("sunsetglow.png"));

    /** draw the world
     * 
     * @param renderingStream
     *            the opaque rendering stream to render
     * @param transparentRenderingStream
     *            the transparent rendering stream to render
     * @param worldToCamera
     *            the transformation from world coordinates to camera
     *            coordinates */
    public void draw(final RenderingStream renderingStream,
                     final RenderingStream transparentRenderingStream,
                     final Matrix worldToCamera)
    {
        RenderingStream rs[] = new RenderingStream[Chunk.drawPhaseCount];
        rs[0] = renderingStream;
        rs[1] = transparentRenderingStream;
        for(int i = 2; i < Chunk.drawPhaseCount; i++)
            rs[i] = RenderingStream.allocate();
        Vector cameraPos = Matrix.setToInverse(this.draw_t3, worldToCamera)
                                 .apply(this.draw_cameraPos, Vector.ZERO);
        int cameraX = (int)Math.floor(cameraPos.getX());
        int cameraY = (int)Math.floor(cameraPos.getY());
        int cameraZ = (int)Math.floor(cameraPos.getZ());
        glClearColor(this.backgroundColor);
        Main.opengl.glClear(Main.opengl.GL_COLOR_BUFFER_BIT()
                | Main.opengl.GL_DEPTH_BUFFER_BIT());
        Main.opengl.glMatrixMode(Main.opengl.GL_MODELVIEW());
        Main.opengl.glPushMatrix();
        glLoadMatrix(worldToCamera);
        Block sunb = Block.NewSun();
        Block moonb = Block.NewMoon();
        if(!this.sunPosition.equals(Vector.ZERO))
        {
            Vector p = Vector.add(this.draw_t1, cameraPos, -0.5f, -0.5f, -0.5f);
            p.addAndSet(Vector.normalize(this.draw_t2, this.sunPosition)
                              .mulAndSet(10.0f));
            RenderingStream.free(sunb.drawAsEntity(RenderingStream.allocate(),
                                                   Matrix.setToTranslate(this.draw_t3,
                                                                         p))
                                     .render());
        }
        if(!this.moonPosition.equals(Vector.ZERO))
        {
            Vector p = Vector.add(this.draw_t1, cameraPos, -0.5f, -0.5f, -0.5f);
            p.addAndSet(Vector.normalize(this.draw_t2, this.moonPosition)
                              .mulAndSet(10.0f));
            RenderingStream.free(moonb.drawAsEntity(RenderingStream.allocate(),
                                                    Matrix.setToTranslate(this.draw_t3,
                                                                          p))
                                      .render());
        }
        Main.opengl.glPopMatrix();
        if(getSunAtHorizonFactor() > 0)
        {
            float t = getSunAtHorizonFactor();
            float nt = 1 - t;
            float glowR = GetRValueF(this.backgroundColor) * nt + 205 / 255f
                    * t;
            float glowG = GetGValueF(this.backgroundColor) * nt + 98 / 255f * t;
            float glowB = GetBValueF(this.backgroundColor) * nt + 62 / 255f * t;
            Matrix tform = Matrix.setToRotateZ(this.draw_t3, Math.PI
                    * (0.5 + Math.floor(2 * this.timeOfDay)));
            RenderingStream.free(Block.drawImgAsBlock(RenderingStream.allocate()
                                                                     .concatMatrix(Matrix.setToScale(this.draw_t4,
                                                                                                     60))
                                                                     .concatMatrix(Matrix.removeTranslate(this.draw_t4,
                                                                                                          worldToCamera)),
                                                      Matrix.setToTranslate(this.draw_t4,
                                                                            -0.5f,
                                                                            -0.5f,
                                                                            -0.5f)
                                                            .concatAndSet(tform),
                                                      sunsetGlow,
                                                      true,
                                                      true,
                                                      glowR,
                                                      glowG,
                                                      glowB)
                                      .render());
        }
        float starFactor = 1 - Math.min(1,
                                        GetBValueF(this.backgroundColor) * 2f);
        float nStarFactor = 1 - starFactor;
        float starR = GetRValueF(this.backgroundColor) * nStarFactor
                + starFactor;
        float starG = GetGValueF(this.backgroundColor) * nStarFactor
                + starFactor;
        float starB = GetBValueF(this.backgroundColor) * nStarFactor
                + starFactor;
        if(starFactor > 0)
        {
            RenderingStream starRenderingStream = RenderingStream.allocate();
            for(Vector v : stars)
            {
                Vector starPos = Vector.allocate(v);
                Matrix.setToRotateZ(this.draw_t3, -Math.PI * 2 * this.timeOfDay)
                      .apply(starPos, starPos);
                Vector p = worldToCamera.applyToNormal(this.draw_t1, starPos)
                                        .mulAndSet(50f);
                Block.drawImgAsEntity(starRenderingStream,
                                      Matrix.setToTranslate(this.draw_t4, p)
                                            .concatAndSet(Matrix.setToScale(this.draw_t3,
                                                                            20f / 50f)),
                                      starImg,
                                      true,
                                      starR,
                                      starG,
                                      starB);
                starPos.free();
            }
            starRenderingStream.render();
            RenderingStream.free(starRenderingStream);
        }
        Main.opengl.glClear(Main.opengl.GL_DEPTH_BUFFER_BIT());
        for(int i = 0; i < Chunk.drawPhaseCount; i++)
            rs[i].setMatrix(worldToCamera);
        int minDrawX = getChunkX(cameraX - viewDist);
        int maxDrawX = getChunkX(cameraX + viewDist);
        int minDrawY = getChunkY(cameraY - viewDist);
        int maxDrawY = getChunkY(cameraY + viewDist);
        int minDrawZ = getChunkZ(cameraZ - viewDist);
        int maxDrawZ = getChunkZ(cameraZ + viewDist);
        for(int cx = getChunkX(Math.round(cameraX - viewDist * chunkGenScale)); cx <= getChunkX(Math.round(cameraX
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
                        if(cx >= minDrawX && cx <= maxDrawX && cy >= minDrawY
                                && cy <= maxDrawY && cz >= minDrawZ
                                && cz <= maxDrawZ)
                        {
                            for(int drawPhase = 0; drawPhase < Chunk.drawPhaseCount; drawPhase++)
                            {
                                drawChunk(rs[drawPhase], cx, cy, cz, drawPhase);
                            }
                        }
                    }
                    Chunk c = find(cx, cy, cz);
                    if(c == null)
                    {
                        for(int gcx = cx; gcx < cx + Chunk.size; gcx += Chunk.generatedChunkSize)
                        {
                            for(int gcy = cy; gcy < cy + Chunk.size; gcy += Chunk.generatedChunkSize)
                            {
                                for(int gcz = cz; gcz < cz + Chunk.size; gcz += Chunk.generatedChunkSize)
                                {
                                    Vector chunkCenter = this.draw_chunkCenter.set(gcx
                                                                                           + Chunk.generatedChunkSize
                                                                                           / 2.0f,
                                                                                   gcy
                                                                                           + Chunk.generatedChunkSize
                                                                                           / 2.0f,
                                                                                   gcz
                                                                                           + Chunk.generatedChunkSize
                                                                                           / 2.0f);
                                    float distance = chunkCenter.subAndSet(cameraPos)
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
                                        Vector chunkCenter = this.draw_chunkCenter.set(gcx
                                                                                               + Chunk.generatedChunkSize
                                                                                               / 2.0f,
                                                                                       gcy
                                                                                               + Chunk.generatedChunkSize
                                                                                               / 2.0f,
                                                                                       gcz
                                                                                               + Chunk.generatedChunkSize
                                                                                               / 2.0f);
                                        float distance = chunkCenter.subAndSet(cameraPos)
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
        for(int drawPhase = 0; drawPhase < Chunk.drawPhaseCount; drawPhase++)
        {
            switch(drawPhase)
            {
            case 0:
                rs[drawPhase].render();
                Main.opengl.glFinish();
                Main.opengl.glDepthMask(false);
                break;
            case 1:
                rs[drawPhase].render();
                Main.opengl.glFinish();
                Main.opengl.glDepthMask(true);
                break;
            }
            RenderingStream.free(rs[drawPhase]);
        }
    }

    private int[] getLightingArray_l = new int[3 * 3 * 3];
    private boolean[] getLightingArray_o = new boolean[3 * 3 * 3];

    int[] getLightingArray(final int bx, final int by, final int bz)
    {
        Block b = getBlock(bx, by, bz);
        if(b == null)
            return new int[]
            {
                0, 0, 0, 0, 0, 0, 0, 0
            };
        if(b.getLightingArray(this.sunlightFactor) != null)
            return b.getLightingArray(this.sunlightFactor);
        int l[] = this.getLightingArray_l;
        boolean o[] = this.getLightingArray_o;
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
        for(int i = 2; i <= 3; i++)
        {
            for(int x = 0; x < 3; x++)
            {
                for(int y = 0; y < 3; y++)
                {
                    for(int z = 0; z < 3; z++)
                    {
                        if(o[x + 3 * (y + 3 * z)])
                            continue;
                        int dist = Math.abs(x - 1) + Math.abs(y - 1)
                                + Math.abs(z - 1);
                        if(dist != i)
                            continue;
                        boolean value = true;
                        for(int orientation = 0; orientation < 6; orientation++)
                        {
                            int px = x + Block.getOrientationDX(orientation);
                            int py = y + Block.getOrientationDY(orientation);
                            int pz = z + Block.getOrientationDZ(orientation);
                            int curdist = Math.abs(px - 1) + Math.abs(py - 1)
                                    + Math.abs(pz - 1);
                            if(curdist != i - 1)
                                continue;
                            if(px < 0 || px >= 3)
                                continue;
                            if(py < 0 || py >= 3)
                                continue;
                            if(pz < 0 || pz >= 3)
                                continue;
                            if(!o[px + 3 * (py + 3 * pz)])
                            {
                                value = false;
                                break;
                            }
                        }
                        o[x + 3 * (y + 3 * z)] = value;
                    }
                }
            }
        }
        for(int i = 0; i < 3 * 3 * 3; i++)
        {
            if(o[i])
                l[i] = 0;
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

    float getLighting(final float x_in,
                      final float y_in,
                      final float z_in,
                      final int bx,
                      final int by,
                      final int bz)
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

    float getLighting(final Vector p, final int bx, final int by, final int bz)
    {
        return getLighting(p.getX(), p.getY(), p.getZ(), bx, by, bz);
    }

    float getLighting(final float x, final float y, final float z)
    {
        return getLighting(x,
                           y,
                           z,
                           (int)Math.floor(x),
                           (int)Math.floor(y),
                           (int)Math.floor(z));
    }

    float getLighting(final Vector p)
    {
        return getLighting(p.getX(), p.getY(), p.getZ());
    }

    /** set the seed for this world<br/>
     * should be called before calling anything else
     * 
     * @param newSeed
     *            the new seed */
    public void setSeed(final int newSeed)
    {
        this.landGenerator = Rand.create(newSeed, this.landGeneratorSettings);
    }

    private static class ChunkGenerator implements Runnable
    {
        public int cx, cy, cz;
        public GeneratedChunk newChunk = null;
        public boolean generated = false;
        public AtomicBoolean busy = new AtomicBoolean(false);
        public Rand landGenerator = null;
        public Rand.Settings landGeneratorSettings = null;
        private final Thread curThread = new Thread(this, "Chunk Generator");
        public AtomicBoolean needStart = new AtomicBoolean(false);

        public ChunkGenerator()
        {
            this.curThread.setDaemon(true);
            this.curThread.start();
        }

        @Override
        public void run()
        {
            while(true)
            {
                synchronized(this.needStart)
                {
                    while(!this.needStart.get())
                    {
                        try
                        {
                            this.needStart.wait();
                        }
                        catch(InterruptedException e)
                        {
                        }
                    }
                    this.needStart.set(false);
                }
                this.newChunk = this.landGenerator.genChunk(this.cx,
                                                            this.cy,
                                                            this.cz,
                                                            Chunk.generatedChunkSize
                                                                    * generatedChunkScale);
                this.generated = true;
                this.busy.set(false);
            }
        }
    }

    private ChunkGenerator chunkGenerator = new ChunkGenerator();

    /** generate chunks */
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
        if(this.chunkGenerator.landGenerator == null
                || this.chunkGenerator.landGenerator.getSeed() != this.landGenerator.getSeed()
                || this.chunkGenerator.landGeneratorSettings == null
                || !this.chunkGenerator.landGeneratorSettings.equals(this.landGeneratorSettings))
        {
            this.chunkGenerator.landGeneratorSettings = new Rand.Settings(this.landGeneratorSettings);
            this.chunkGenerator.landGenerator = Rand.create(this.landGenerator.getSeed(),
                                                            this.chunkGenerator.landGeneratorSettings);
        }
        synchronized(this.chunkGenerator.needStart)
        {
            this.chunkGenerator.busy.set(true);
            this.chunkGenerator.needStart.set(true);
            this.chunkGenerator.needStart.notifyAll();
        }
        this.genChunkDistance = -1;
    }

    /** insert a new entity into this world
     * 
     * @param e
     *            the entity to insert */
    public void insertEntity(final Entity e)
    {
        if(e == null || e.isEmpty())
            return;
        EntityNode node = EntityNode.allocate();
        node.e = Entity.allocate(e);
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
            else
                node.free();
        }
        players.entityCheckHitPlayers();
    }

    private void removeEntityNode(final EntityNode node)
    {
        Vector pos = node.e.getPosition();
        int x = (int)Math.floor(pos.getX());
        int y = (int)Math.floor(pos.getY());
        int z = (int)Math.floor(pos.getZ());
        if(node.prev == null)
            this.entityHead = node.next;
        else
            node.prev.next = node.next;
        if(node.next == null)
            this.entityTail = node.prev;
        else
            node.next.prev = node.prev;
        node.next = null;
        node.prev = null;
        Chunk c = find(getChunkX(x), getChunkY(y), getChunkZ(z));
        if(c == null)
            return;
        if(node.hashprev == null)
            c.head = node.hashnext;
        else
            node.hashprev.hashnext = node.hashnext;
        if(node.hashnext == null)
            c.tail = node.hashprev;
        else
            node.hashnext.hashprev = node.hashprev;
        node.hashnext = null;
        node.hashprev = null;
    }

    private void explodeEntities(final Vector pos, final float strength)
    {
        final int maxRadius = (int)Math.ceil(strength * 2);
        final int minChunkX = getChunkX((int)Math.floor(pos.getX()) - maxRadius);
        final int maxChunkX = getChunkX((int)Math.ceil(pos.getX()) + maxRadius
                + Chunk.size - 1);
        final int minChunkY = getChunkY((int)Math.floor(pos.getY()) - maxRadius);
        final int maxChunkY = getChunkY((int)Math.ceil(pos.getY()) + maxRadius
                + Chunk.size - 1);
        final int minChunkZ = getChunkZ((int)Math.floor(pos.getZ()) - maxRadius);
        final int maxChunkZ = getChunkZ((int)Math.ceil(pos.getZ()) + maxRadius
                + Chunk.size - 1);
        EntityNode head = null;
        for(int cx = minChunkX; cx <= maxChunkX; cx += Chunk.size)
        {
            for(int cy = minChunkY; cy <= maxChunkY; cy += Chunk.size)
            {
                for(int cz = minChunkZ; cz <= maxChunkZ; cz += Chunk.size)
                {
                    Chunk c = find(cx, cy, cz);
                    if(c == null)
                        continue;
                    EntityNode node = c.head;
                    while(node != null)
                    {
                        removeEntityNode(node);
                        node.next = head;
                        head = node;
                        node = c.head;
                    }
                }
            }
        }
        for(EntityNode node = head, nextNode = (node != null ? node.next : null); node != null; node = nextNode, nextNode = (node != null ? node.next
                : null))
        {
            node.e.explode(pos, strength);
            if(!node.e.isEmpty())
                insertEntity(node);
            else
                node.free();
        }
    }

    void checkHitPlayer(final Player p)
    {
        for(EntityNode node = removeAllEntities(), nextNode = (node != null ? node.next
                : null); node != null; node = nextNode, nextNode = (node != null ? node.next
                : null))
        {
            node.e.checkHitPlayer(p);
            if(!node.e.isEmpty())
                insertEntity(node);
            else
                node.free();
        }
    }

    private void moveRedstone()
    {
        EvalNode freeMe;
        for(EvalNode node = removeAllEvalNodes(EvalType.RedstoneFirst); node != null; freeMe = node, node = node.listnext, freeMe.free())
        {
            Block b = getBlockEval(node.x, node.y, node.z);
            if(b != null)
                insertEvalNode(EvalType.RedstoneFirst,
                               node.x,
                               node.y,
                               node.z,
                               b.redstoneMove(node.x, node.y, node.z));
        }
        for(EvalNode node = removeAllEvalNodes(EvalType.RedstoneFirst); node != null; freeMe = node, node = node.listnext, freeMe.free())
        {
            if(node.b == null)
                continue;
            setBlock(node.x, node.y, node.z, node.b);
        }
        for(int i = 0; i < 16; i++)
        {
            for(EvalNode node = removeAllEvalNodes(EvalType.Redstone); node != null; freeMe = node, node = node.listnext, freeMe.free())
            {
                Block b = getBlockEval(node.x, node.y, node.z);
                if(b != null)
                    insertEvalNode(EvalType.Redstone,
                                   node.x,
                                   node.y,
                                   node.z,
                                   b.redstoneDustMove(node.x, node.y, node.z));
            }
            for(EvalNode node = removeAllEvalNodes(EvalType.Redstone); node != null; freeMe = node, node = node.listnext, freeMe.free())
            {
                if(node.b == null)
                    continue;
                setBlock(node.x, node.y, node.z, node.b);
            }
        }
    }

    private void moveGeneral()
    {
        EvalNode freeMe;
        for(EvalNode node = removeAllEvalNodes(EvalType.General); node != null; freeMe = node, node = node.listnext, freeMe.free())
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
        for(EvalNode node = removeAllEvalNodes(EvalType.General); node != null; freeMe = node, node = node.listnext, freeMe.free())
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
            EvalNode freeMe = node;
            node = node.listnext;
            freeMe.free();
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
            EvalNode freeMe = node;
            node = node.listnext;
            freeMe.free();
        }
    }

    private void runRandomMove()
    {
        for(Chunk node = this.chunksHead; node != null; node = node.listnext)
        {
            int count;
            count = (int)Math.floor(Chunk.size * Chunk.size * Chunk.size * 3f
                    / 16f / 16f / 16f + fRand(0.0f, 1.0f));
            for(int i = 0; i < count; i++)
            {
                int x = node.orgx + (int)Math.floor(fRand(0.0f, Chunk.size));
                int y = node.orgy + (int)Math.floor(fRand(0.0f, Chunk.size));
                int z = node.orgz + (int)Math.floor(fRand(0.0f, Chunk.size));
                Block b = getBlockEval(x, y, z);
                if(b != null)
                {
                    b = b.moveRandom(x, y, z);
                    if(b != null)
                        setBlock(x, y, z, b);
                }
            }
        }
    }

    private double curTime = 0.0;

    /** moves everything in this world except the players */
    public void move()
    {
        this.curTime += Main.getFrameDuration();
        final float dayDuration = 20.0f * 60.0f;
        setTimeOfDay(this.timeOfDay + (float)Main.getFrameDuration()
                / dayDuration * (Main.DEBUG ? 1 : 1));// TODO finish
        addParticles();
        moveEntities();
        checkAllTimedInvalidates();
        moveAllBlocks();
        runRandomMove();
        generateAllTrees();
        runAllExplosions();
        updateLight();
    }

    /** @return the current game time */
    public double getCurTime()
    {
        return this.curTime;
    }

    static final class BlockHitDescriptor
    {
        public Block b;
        public int x, y, z, orientation;
        public float distance;
        public boolean hitUnloadedChunk;

        public BlockHitDescriptor(final int x,
                                  final int y,
                                  final int z,
                                  final int orientation,
                                  final float distance,
                                  final Block b)
        {
            this.b = b;
            this.x = x;
            this.y = y;
            this.z = z;
            this.orientation = orientation;
            this.hitUnloadedChunk = (b != null) ? false : true;
            this.distance = distance;
        }

        public BlockHitDescriptor()
        {
            this(0, 0, 0, -1, -1, null);
            this.hitUnloadedChunk = false;
        }

        public BlockHitDescriptor init(final int x,
                                       final int y,
                                       final int z,
                                       final int orientation,
                                       final float distance,
                                       final Block b)
        {
            this.b = b;
            this.x = x;
            this.y = y;
            this.z = z;
            this.orientation = orientation;
            this.hitUnloadedChunk = (b != null) ? false : true;
            this.distance = distance;
            return this;
        }

        public BlockHitDescriptor init()
        {
            init(0, 0, 0, -1, -1, null);
            this.hitUnloadedChunk = false;
            return this;
        }
    }

    private Vector internalGetPointedAtBlock_pos = Vector.allocate();
    private Vector internalGetPointedAtBlock_dir = Vector.allocate();
    private Vector internalGetPointedAtBlock_invdir = Vector.allocate();
    private Vector internalGetPointedAtBlock_nextxinc = Vector.allocate();
    private Vector internalGetPointedAtBlock_nextyinc = Vector.allocate();
    private Vector internalGetPointedAtBlock_nextzinc = Vector.allocate();
    private Vector internalGetPointedAtBlock_vt = Vector.allocate();
    private Vector internalGetPointedAtBlock_nextx = Vector.allocate();
    private Vector internalGetPointedAtBlock_nexty = Vector.allocate();
    private Vector internalGetPointedAtBlock_nextz = Vector.allocate();
    private Vector internalGetPointedAtBlock_vtinc = Vector.allocate();
    private Vector internalGetPointedAtBlock_t1 = Vector.allocate();
    private Vector internalGetPointedAtBlock_t2 = Vector.allocate();
    private Vector internalGetPointedAtBlock_newpos = Vector.allocate();

    private BlockHitDescriptor
        internalGetPointedAtBlock(final BlockHitDescriptor retval,
                                  final Vector pos_in,
                                  final Vector dir_in,
                                  final float maxDist_in,
                                  final boolean getBlockRightBefore,
                                  final boolean calcPassThruWater,
                                  final boolean passThruWater_in)
    {
        final float maxDist = Float.isNaN(maxDist_in) ? 128
                : Math.max(0, Math.min(128, maxDist_in));
        int finishx = 0, finishy = 0, finishz = 0, orientation = -1;
        Vector pos = Vector.set(this.internalGetPointedAtBlock_pos, pos_in);
        Vector dir = Vector.set(this.internalGetPointedAtBlock_dir, dir_in);
        final float eps = 1e-4f;
        if(Math.abs(dir.getX()) < eps)
            dir.setX(eps);
        if(Math.abs(dir.getY()) < eps)
            dir.setY(eps);
        if(Math.abs(dir.getZ()) < eps)
            dir.setZ(eps);
        Vector invdir = Vector.set(this.internalGetPointedAtBlock_invdir,
                                   1.0f / dir.getX(),
                                   1.0f / dir.getY(),
                                   1.0f / dir.getZ());
        int ix = (int)Math.floor(pos.getX());
        int iy = (int)Math.floor(pos.getY());
        int iz = (int)Math.floor(pos.getZ());
        int previx = 0, previy = 0, previz = 0;
        boolean hasprev = false;
        int lasthit = -1;
        Block prevb = null;
        Block b = getBlock(ix, iy, iz);
        if(b == null)
            return retval.init();
        boolean passthruwater;
        if(calcPassThruWater)
        {
            passthruwater = false;
            if(b.getType() == BlockType.BTWater)
                passthruwater = true;
        }
        else
            passthruwater = passThruWater_in;
        float totalt = 0.0f;
        Vector nextxinc = Vector.set(this.internalGetPointedAtBlock_nextxinc,
                                     (dir.getX() < 0) ? -1 : 1,
                                     0,
                                     0);
        Vector nextyinc = Vector.set(this.internalGetPointedAtBlock_nextyinc,
                                     0,
                                     (dir.getY() < 0) ? -1 : 1,
                                     0);
        Vector nextzinc = Vector.set(this.internalGetPointedAtBlock_nextzinc,
                                     0,
                                     0,
                                     (dir.getZ() < 0) ? -1 : 1);
        int fixx = 0, fixy = 0, fixz = 0;
        if(dir.getX() < 0)
            fixx = -1;
        if(dir.getY() < 0)
            fixy = -1;
        if(dir.getZ() < 0)
            fixz = -1;
        Vector vt = this.internalGetPointedAtBlock_vt;
        Vector nextx = this.internalGetPointedAtBlock_nextx, nexty = this.internalGetPointedAtBlock_nexty, nextz = this.internalGetPointedAtBlock_nextz;
        nextx.setX((dir.getX() < 0) ? (float)Math.ceil(pos.getX()) - 1
                : (float)Math.floor(pos.getX()) + 1);
        nexty.setY((dir.getY() < 0) ? (float)Math.ceil(pos.getY()) - 1
                : (float)Math.floor(pos.getY()) + 1);
        nextz.setZ((dir.getZ() < 0) ? (float)Math.ceil(pos.getZ()) - 1
                : (float)Math.floor(pos.getZ()) + 1);
        vt.setX((nextx.getX() - pos.getX()) * invdir.getX());
        vt.setY((nexty.getY() - pos.getY()) * invdir.getY());
        vt.setZ((nextz.getZ() - pos.getZ()) * invdir.getZ());
        nextx.setY(vt.getX() * dir.getY() + pos.getY());
        nextx.setZ(vt.getX() * dir.getZ() + pos.getZ());
        nexty.setX(vt.getY() * dir.getX() + pos.getX());
        nexty.setZ(vt.getY() * dir.getZ() + pos.getZ());
        nextz.setX(vt.getZ() * dir.getX() + pos.getX());
        nextz.setY(vt.getZ() * dir.getY() + pos.getY());
        Vector vtinc = Vector.set(this.internalGetPointedAtBlock_vtinc,
                                  Math.abs(invdir.getX()),
                                  Math.abs(invdir.getY()),
                                  Math.abs(invdir.getZ()));
        nextxinc.setY(vtinc.getX() * dir.getY());
        nextxinc.setZ(vtinc.getX() * dir.getZ());
        nextyinc.setX(vtinc.getY() * dir.getX());
        nextyinc.setZ(vtinc.getY() * dir.getZ());
        nextzinc.setX(vtinc.getZ() * dir.getX());
        nextzinc.setY(vtinc.getZ() * dir.getY());
        float rayIntersectsRetval = -1;
        if(b != null)
            rayIntersectsRetval = b.rayIntersects(dir,
                                                  invdir,
                                                  Vector.sub(this.internalGetPointedAtBlock_t1,
                                                             pos,
                                                             ix,
                                                             iy,
                                                             iz),
                                                  Vector.sub(this.internalGetPointedAtBlock_t2,
                                                             pos,
                                                             ix,
                                                             iy,
                                                             iz));
        // int i = 0;
        while(b != null
                && (b.getType() == BlockType.BTEmpty
                        || (b.getType() == BlockType.BTWater && passthruwater) || rayIntersectsRetval == -1)/*
                                                                                                             * &&
                                                                                                             * i
                                                                                                             * ++
                                                                                                             * <
                                                                                                             * 1
                                                                                                             */)
        {
            hasprev = true;
            previx = ix;
            previy = iy;
            previz = iz;
            prevb = b;
            float t;
            Vector newpos;
            if(vt.getX() < vt.getY())
            {
                if(vt.getX() < vt.getZ())
                {
                    t = vt.getX();
                    newpos = Vector.set(this.internalGetPointedAtBlock_newpos,
                                        nextx);
                    ix = (int)Math.floor(newpos.getX());
                    iy = (int)Math.floor(newpos.getY());
                    iz = (int)Math.floor(newpos.getZ());
                    ix += fixx;
                    vt = vt.subAndSet(t, t, t);
                    nextx = nextx.addAndSet(nextxinc);
                    vt.setX(vtinc.getX());
                    lasthit = 0;
                }
                else
                {
                    t = vt.getZ();
                    newpos = Vector.set(this.internalGetPointedAtBlock_newpos,
                                        nextz);
                    ix = (int)Math.floor(newpos.getX());
                    iy = (int)Math.floor(newpos.getY());
                    iz = (int)Math.floor(newpos.getZ());
                    iz += fixz;
                    vt = vt.subAndSet(t, t, t);
                    nextz = nextz.addAndSet(nextzinc);
                    vt.setZ(vtinc.getZ());
                    lasthit = 1;
                }
            }
            else
            {
                if(vt.getY() < vt.getZ())
                {
                    t = vt.getY();
                    newpos = Vector.set(this.internalGetPointedAtBlock_newpos,
                                        nexty);
                    ix = (int)Math.floor(newpos.getX());
                    iy = (int)Math.floor(newpos.getY());
                    iz = (int)Math.floor(newpos.getZ());
                    iy += fixy;
                    vt = vt.subAndSet(t, t, t);
                    nexty = nexty.addAndSet(nextyinc);
                    vt.setY(vtinc.getY());
                    lasthit = 4;
                }
                else
                {
                    t = vt.getZ();
                    newpos = Vector.set(this.internalGetPointedAtBlock_newpos,
                                        nextz);
                    ix = (int)Math.floor(newpos.getX());
                    iy = (int)Math.floor(newpos.getY());
                    iz = (int)Math.floor(newpos.getZ());
                    iz += fixz;
                    vt = vt.subAndSet(t, t, t);
                    nextz = nextz.addAndSet(nextzinc);
                    vt.setZ(vtinc.getZ());
                    lasthit = 1;
                }
            }
            pos = Vector.set(this.internalGetPointedAtBlock_pos, newpos);
            totalt += t;
            if(totalt > maxDist)
                return new BlockHitDescriptor();
            b = getBlock(ix, iy, iz);
            rayIntersectsRetval = -1;
            if(b != null)
                rayIntersectsRetval = b.rayIntersects(dir,
                                                      invdir,
                                                      Vector.sub(this.internalGetPointedAtBlock_t1,
                                                                 pos,
                                                                 ix,
                                                                 iy,
                                                                 iz),
                                                      Vector.sub(this.internalGetPointedAtBlock_t2,
                                                                 pos,
                                                                 ix,
                                                                 iy,
                                                                 iz));
        }
        totalt += Math.max(0, rayIntersectsRetval);
        if(b != null)
        {
            if(getBlockRightBefore)
            {
                if(!hasprev)
                {
                    return retval.init();
                }
                finishx = previx;
                finishy = previy;
                finishz = previz;
                b = prevb;
                dir = dir.negAndSet(); // swap lasthit
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
                if(dir.getX() < 0)
                    lasthit = 2;
                break;
            case 1:
                if(dir.getZ() < 0)
                    lasthit = 3;
                break;
            // case 4:
            default:
                if(dir.getY() < 0)
                    lasthit = 5;
                break;
            }
            orientation = lasthit;
        }
        return retval.init(finishx, finishy, finishz, orientation, totalt, b);
    }

    BlockHitDescriptor getPointedAtBlock(final BlockHitDescriptor retval,
                                         final Vector org,
                                         final Vector dir,
                                         final float maxDist,
                                         final boolean getBlockRightBefore,
                                         final boolean passThruWater)
    {
        return internalGetPointedAtBlock(retval,
                                         org,
                                         dir,
                                         maxDist,
                                         getBlockRightBefore,
                                         false,
                                         passThruWater);
    }

    private Matrix getPointedAtBlock_cameraToWorld = new Matrix();
    private Vector getPointedAtBlock_org = Vector.allocate();
    private Vector getPointedAtBlock_dir = Vector.allocate();

    BlockHitDescriptor getPointedAtBlock(final BlockHitDescriptor retval,
                                         final Matrix worldToCamera,
                                         final float maxDist,
                                         final boolean getBlockRightBefore)
    {
        Vector org = Vector.set(this.getPointedAtBlock_org, 0, 0, 0);
        Vector dir = Vector.set(this.getPointedAtBlock_dir, 0, 0, -1);
        Matrix cameraToWorld = Matrix.setToInverse(this.getPointedAtBlock_cameraToWorld,
                                                   worldToCamera);
        org = cameraToWorld.apply(org, org);
        dir = cameraToWorld.apply(dir, dir).subAndSet(org).normalizeAndSet();
        return internalGetPointedAtBlock(retval,
                                         org,
                                         dir,
                                         maxDist,
                                         getBlockRightBefore,
                                         true,
                                         false);
    }

    private static final class TreeGenerateLocation
    {
        public final int x, y, z;
        public final Block startingSapling;
        public TreeGenerateLocation next;

        public TreeGenerateLocation(final int x,
                                    final int y,
                                    final int z,
                                    final Block b,
                                    final TreeGenerateLocation next)
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

    /** add a new tree
     * 
     * @param x
     *            new tree's x coordinate
     * @param y
     *            new tree's y coordinate
     * @param z
     *            new tree's z coordinate
     * @param startingSapling
     *            the sapling that created this tree */
    public void addNewTree(final int x,
                           final int y,
                           final int z,
                           final Block startingSapling)
    {
        this.treeGenerateListHead = new TreeGenerateLocation(x,
                                                             y,
                                                             z,
                                                             startingSapling,
                                                             this.treeGenerateListHead);
    }

    private static final int fileVersion = 2;

    /** write to a <code>DataOutput</code>
     * 
     * @param o
     *            <code>OutputStream</code> to write to
     * @throws IOException
     *             the exception thrown */
    public static void write(final DataOutput o) throws IOException
    {
        o.writeInt(fileVersion);
        o.writeInt(world.landGenerator.getSeed());
        world.landGeneratorSettings.write(o);
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
            EvalType evalType = EvalType.values[evalTypei];
            EvalNode head = world.removeAllEvalNodes(evalType);
            int evalNodeCount = 0;
            for(EvalNode n = head; n != null; n = n.listnext)
            {
                evalNodeCount++;
            }
            o.writeInt(evalNodeCount);
            if(evalNodeCount > 0)
            {
                Main.pushProgress(evalTypei, 1.0f / evalNodeCount);
                int progress = 0;
                for(EvalNode n = head, nextNode = (head != null ? head.listnext
                        : null); n != null; n = nextNode, nextNode = (n != null ? n.listnext
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

    /** read from a <code>DataInput</code>
     * 
     * @param i
     *            <code>DataInput</code> to read from
     * @throws IOException
     *             the exception thrown */
    public static void read(final DataInput i) throws IOException
    {
        int v = i.readInt();
        if(v != fileVersion)
        {
            readVer1(i, v);
            return;
        }
        int seed = i.readInt();
        clear(seed);
        world.setLandGeneratorSettings(Rand.Settings.read(i));
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
            EvalType evalType = EvalType.values[evalTypei];
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

    private static void
        readVer1(final DataInput i, final int v) throws IOException
    {
        if(v != 1)
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
            EvalType evalType = EvalType.values[evalTypei];
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

    private static void
        readVer0(final DataInput i, final int v) throws IOException
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

    /** clears <code>world</code> to a new world
     * 
     * @param seed
     *            the new seed */
    public static void clear(final int seed)
    {
        world = new World();
        world.setSeed(seed);
    }

    /** clears <code>world</code> to a new world */
    public static void clear()
    {
        world = new World();
    }

    private static class ExplosionNode
    {
        public final int x, y, z;
        public float strength;
        public ExplosionNode next;

        public ExplosionNode(final int x,
                             final int y,
                             final int z,
                             final float strength,
                             final ExplosionNode next)
        {
            this.x = x;
            this.y = y;
            this.z = z;
            this.strength = strength;
            this.next = next;
        }
    }

    private ExplosionNode explosionList = null;

    /** creates an explosion at &lt;<code>x</code>, <code>y</code>,
     * <code>z</code>&gt;
     * 
     * @param x
     *            the x coordinate of the new explosion
     * @param y
     *            the y coordinate of the new explosion
     * @param z
     *            the z coordinate of the new explosion
     * @param strength
     *            the strength of the new explosion */
    public void addExplosion(final int x,
                             final int y,
                             final int z,
                             final float strength)
    {
        this.explosionList = new ExplosionNode(x,
                                               y,
                                               z,
                                               strength,
                                               this.explosionList);
    }

    private float getExplosionStrength(final int x,
                                       final int y,
                                       final int z,
                                       final float strength)
    {
        Block b = getBlockEval(x, y, z);
        if(b == null)
            return 0.0f;
        if(!b.isExplodable())
            return 0.0f;
        return Math.max(0, strength - (b.getBlastResistance() / 5 + 0.3f)
                * 0.3f);
    }

    private static class BlockLoc
    {
        public final int x, y, z;

        public BlockLoc(final int x, final int y, final int z)
        {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(final Object obj)
        {
            if(obj == null || !(obj instanceof BlockLoc))
                return false;
            BlockLoc rt = (BlockLoc)obj;
            return rt.x == this.x && rt.y == this.y && rt.z == this.z;
        }

        @Override
        public int hashCode()
        {
            return this.x + 1234787 * this.y + 1263486782 * this.z;
        }
    }

    private static Vector runExplosionRay_step = Vector.allocate();

    private void runExplosionRay(final Set<BlockLoc> destroyedBlocks,
                                 final Vector init_pos,
                                 final Vector dir,
                                 final float init_strength)
    {
        Vector step = Vector.normalize(runExplosionRay_step, dir)
                            .mulAndSet(0.3f);
        float strength = init_strength;
        Vector pos = init_pos;
        int lastBlockX = (int)Math.floor(pos.getX());
        int lastBlockY = (int)Math.floor(pos.getY());
        int lastBlockZ = (int)Math.floor(pos.getZ());
        boolean isFirst = true;
        while(strength > 0)
        {
            int curBlockX = (int)Math.floor(pos.getX());
            int curBlockY = (int)Math.floor(pos.getY());
            int curBlockZ = (int)Math.floor(pos.getZ());
            if(curBlockX != lastBlockX || curBlockY != lastBlockY
                    || curBlockZ != lastBlockZ)
            {
                destroyedBlocks.add(new BlockLoc(lastBlockX,
                                                 lastBlockY,
                                                 lastBlockZ));
            }
            lastBlockX = curBlockX;
            lastBlockY = curBlockY;
            lastBlockZ = curBlockZ;
            if(isFirst)
                isFirst = false;
            else
                strength = Math.max(0, strength - 0.3f * 0.75f);
            strength = getExplosionStrength(curBlockX,
                                            curBlockY,
                                            curBlockZ,
                                            strength);
            pos = pos.addAndSet(step);
        }
    }

    private static Vector runExplosion_t1 = Vector.allocate();

    private void runExplosion(final int x, final int y, final int z)
    {
        Block b = getBlockEval(x, y, z);
        if(b == null)
            return;
        if(b.getType() == BlockType.BTEmpty)
            return;
        if(!b.isExplodable())
            return;
        if(b.getType() == BlockType.BTTNT)
        {
            insertEntity(Entity.NewPrimedTNT(runExplosion_t1.set(x, y, z),
                                             World.fRand(0, 1)));
        }
        else if(World.fRand(0, 5) < 1 && b.canDig())
            b.digBlock(x, y, z, true, ToolType.None);
        else
            b.digBlock(x, y, z, false, ToolType.None);
        setBlock(x, y, z, new Block());
    }

    private static Vector runExplosion_dir = Vector.allocate();
    private static Vector runExplosion_pos = Vector.allocate();

    private void runExplosion(final ExplosionNode explosion)
    {
        explodeEntities(runExplosion_t1.set(explosion.x + 0.5f,
                                            explosion.y + 0.5f,
                                            explosion.z + 0.5f),
                        explosion.strength);
        Set<BlockLoc> destroyedBlocks = new HashSet<World.BlockLoc>();
        final int count = 16;
        for(int x = 0; x < count; x++)
        {
            for(int y = 0; y < count; y++)
            {
                for(int z = 0; z < count; z++)
                {
                    if(x != 0 && x != count - 1 && y != 0 && y != count - 1
                            && z != 0 && z != count - 1)
                        z = count - 1;
                    Vector dir = runExplosion_dir.set((float)x * 2
                                                              / (count - 1) - 1,
                                                      (float)y * 2
                                                              / (count - 1) - 1,
                                                      (float)z * 2
                                                              / (count - 1) - 1)
                                                 .normalizeAndSet();
                    Vector pos = runExplosion_pos.set(explosion.x + 0.5f,
                                                      explosion.y + 0.5f,
                                                      explosion.z + 0.5f);
                    runExplosionRay(destroyedBlocks,
                                    pos,
                                    dir,
                                    fRand(0.7f, 1.3f) * explosion.strength);
                }
            }
        }
        for(BlockLoc i : destroyedBlocks)
        {
            runExplosion(i.x, i.y, i.z);
        }
    }

    private void runAllExplosions()
    {
        ExplosionNode allExplosions = this.explosionList;
        if(allExplosions == null)
            return;
        this.explosionList = null;
        ExplosionNode node = allExplosions;
        while(node != null)
        {
            runExplosion(node);
            node = node.next;
        }
    }

    /** @param x
     *            the x coordinate
     * @param z
     *            the z coordinate
     * @return the biome name */
    public String getBiomeName(final int x, final int z)
    {
        return this.landGenerator.getBiomeName(x, z);
    }

    /** @param position
     *            the position
     * @return the biome name */
    public String getBiomeName(final Vector position)
    {
        return getBiomeName((int)Math.floor(position.getX()),
                            (int)Math.floor(position.getZ()));
    }

    public float getBiomeGrassColorR(final int x, final int z)
    {
        return this.landGenerator.getBiomeGrassColorR(x, z);
    }

    public float getBiomeGrassColorG(final int x, final int z)
    {
        return this.landGenerator.getBiomeGrassColorG(x, z);
    }

    public float getBiomeGrassColorB(final int x, final int z)
    {
        return this.landGenerator.getBiomeGrassColorB(x, z);
    }

    public float getBiomeWaterColorR(final int x, final int z)
    {
        return this.landGenerator.getBiomeWaterColorR(x, z);
    }

    public float getBiomeWaterColorG(final int x, final int z)
    {
        return this.landGenerator.getBiomeWaterColorG(x, z);
    }

    public float getBiomeWaterColorB(final int x, final int z)
    {
        return this.landGenerator.getBiomeWaterColorB(x, z);
    }

    public float getBiomeFoliageColorR(final int x, final int z)
    {
        return this.landGenerator.getBiomeFoliageColorR(x, z);
    }

    public float getBiomeFoliageColorG(final int x, final int z)
    {
        return this.landGenerator.getBiomeFoliageColorG(x, z);
    }

    public float getBiomeFoliageColorB(final int x, final int z)
    {
        return this.landGenerator.getBiomeFoliageColorB(x, z);
    }

    public float getTimeOfDay()
    {
        return this.timeOfDay;
    }

    public boolean isNightTime()
    {
        if(this.timeOfDay < 0.3f || this.timeOfDay > 0.7f)
            return true;
        return false;
    }

    public boolean isDayTime()
    {
        return !isNightTime();
    }

    public void setToDawn()
    {
        setTimeOfDay(0.3f);
    }

    public float getSunAtHorizonFactor()
    {
        float v = 2.0f * (float)Math.abs(-0.1f
                + Math.cos(Math.PI * 2 * this.timeOfDay));
        if(v >= 1)
            return 0;
        return 1 - v;
    }

    public int getLandHeight(final int x, final int z)
    {
        return this.landGenerator.getRockHeight(x, z);
    }
}
