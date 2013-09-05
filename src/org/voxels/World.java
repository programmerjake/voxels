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
import java.util.concurrent.atomic.AtomicInteger;

import org.voxels.BlockType.ToolType;
import org.voxels.generate.*;

//FIXME change move entity to move entities then remove them in separate loops so that Entity.move can find other entities 
/** @author jacob */
public final class World
{
    private static final Allocator<World> allocator = new Allocator<World>()
    {
        @Override
        protected World allocateInternal()
        {
            return new World();
        }
    };
    /** the program's world */
    public static World world = allocator.allocate().init();
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
    private Rand landGenerator;
    private static final int generatedChunkScale = 1 << 0; // must be power of 2

    /** @param landGeneratorSettings
     *            the new land generator settings
     * @param useSeed
     *            if the current seed should be used */
    public void
        setLandGeneratorSettings(final Rand.Settings landGeneratorSettings,
                                 final boolean useSeed)
    {
        if(this.landGeneratorSettings != null)
            this.landGeneratorSettings.free();
        this.landGeneratorSettings = landGeneratorSettings;
        if(this.landGeneratorSettings != null)
            this.landGeneratorSettings = this.landGeneratorSettings.dup();
        int seed = this.landGenerator.getSeed();
        this.landGenerator.free();
        if(useSeed)
            this.landGenerator = Rand.create(seed, this.landGeneratorSettings);
        else
            this.landGenerator = Rand.create(this.landGeneratorSettings);
    }

    private World init()
    {
        this.landGenerator = Rand.create(this.landGeneratorSettings);
        return this;
    }

    private void free()
    {
        clearChunkGenerator();
        this.landGenerator.free();
        this.landGenerator = null;
        if(this.landGeneratorSettings != null)
            this.landGeneratorSettings.free();
        this.landGeneratorSettings = null;
        clearEntities();
        clearHashTable();
        clearEvalNodes();
        clearTimedInvalidates();
        clearBackgroundColor();
        clearGenChunk();
        clearMoveAllBlocks();
        clearParticleGenTime();
        clearCurTime();
        clearTreeGenerateList();
        clearDisplayListValidTag();
        clearTimeOfDay();
        allocator.free(this);
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
        boolean isFree = false;
        public boolean isInList = false;

        public void free()
        {
            if(this.isFree)
                throw new RuntimeException("double free");
            if(this.isInList)
                throw new RuntimeException("free called on node in list");
            this.hashnext = null;
            this.hashprev = null;
            this.prev = null;
            if(this.e != null)
                this.e.free();
            this.e = null;
            this.isFree = true;
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
            retval.isFree = false;
            return retval;
        }
    }

    private EntityNode entityHead = null, entityTail = null;
    private long entityCount = 0;

    private void clearEntities()
    {
        EntityNode head = removeAllEntities();
        while(head != null)
        {
            EntityNode freeMe = head;
            head = head.next;
            freeMe.free();
        }
        this.entityCount = 0;
    }

    private static class Chunk
    {
        private static final Allocator<Chunk> allocator = new Allocator<World.Chunk>()
        {
            @Override
            protected Chunk allocateInternal()
            {
                return new Chunk();
            }
        };
        public static final int size = 4; // must be power of 2
        public static final int generatedChunkSize = size; // must be power of 2
                                                           // that is less than
                                                           // or equal to
                                                           // Chunk.size
        public static final int generatedChunksPerChunk = Math.max(1, size
                / generatedChunkSize);
        public int orgx, orgy, orgz;
        private final boolean generated[] = new boolean[generatedChunksPerChunk
                * generatedChunksPerChunk * generatedChunksPerChunk];
        private final Block[] blocks = new Block[size * size * size];
        public Chunk next, listnext;
        public static final int drawPhaseCount = 2;
        public final long displayListValidTag[] = new long[drawPhaseCount];
        @SuppressWarnings("unused")
        public EntityNode head = null, tail = null;
        public boolean drawsAnything = true;
        public long drawsAnythingValidTag = -1;
        public int fireCount = 0;

        Chunk()
        {
        }

        public static Chunk allocate(final int ox, final int oy, final int oz)
        {
            Chunk retval = allocator.allocate();
            retval.orgx = ox;
            retval.orgy = oy;
            retval.orgz = oz;
            for(int i = 0; i < drawPhaseCount; i++)
            {
                retval.displayListValidTag[i] = -1;
            }
            retval.next = null;
            retval.listnext = null;
            retval.head = null;
            retval.tail = null;
            retval.fireCount = 0;
            return retval;
        }

        public void free()
        {
            for(int i = 0; i < this.blocks.length; i++)
            {
                if(this.blocks[i] != null)
                {
                    this.blocks[i].isInWorld = false;
                    this.blocks[i].free();
                }
                this.blocks[i] = null;
            }
            this.next = null;
            this.listnext = null;
            this.head = null;
            this.tail = null;
            allocator.free(this);
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
            if(this.blocks[index] != null
                    && this.blocks[index].getType() == BlockType.BTFire)
                this.fireCount--;
            this.blocks[index] = b;
            if(b != null && b.getType() == BlockType.BTFire)
                this.fireCount++;
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

    private void clearLastChunk()
    {
        this.lastChunk = null;
    }

    private void insertEntity(final EntityNode node)
    {
        if(node.isFree || node.isInList || node == this.entityHead
                || node == this.entityTail)
            throw new RuntimeException("can't insert currently used entity");
        node.isInList = true;
        if(node.e.isEmpty())
        {
            node.next = this.entityHead;
            node.prev = null;
            node.hashnext = null;
            node.hashprev = null;
            if(this.entityHead != null)
                this.entityHead.prev = node;
            else
                this.entityTail = node;
            this.entityHead = node;
            this.entityCount++;
            return;
        }
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
        this.entityCount++;
    }

    private EntityNode removeAllEntities()
    {
        EntityNode retval = this.entityHead;
        this.entityHead = null;
        this.entityTail = null;
        for(EntityNode node = retval; node != null; node = node.next)
        {
            node.isInList = false;
            Chunk c = null;
            if(!node.e.isEmpty())
            {
                Vector pos = node.e.getPosition();
                int x = (int)Math.floor(pos.getX());
                int y = (int)Math.floor(pos.getY());
                int z = (int)Math.floor(pos.getZ());
                c = find(getChunkX(x), getChunkY(y), getChunkZ(z));
            }
            if(c != null)
            {
                c.head = null;
                c.tail = null;
            }
            node.hashnext = null;
            node.hashprev = null;
        }
        this.entityCount = 0;
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
            if(this.b != null)
                this.b.free();
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
        General,
        Redstone,
        RedstoneFirst,
        Lighting,
        Particles,
        Pistons,
        Fire,
        Last;
        public static final EvalType[] values = values();
    }

    private static final int EvalTypeCount = EvalType.Last.ordinal();

    private static int getChunkX(final int v)
    {
        return v - (v & (Chunk.size - 1));
    }

    private static int getChunkY(final int v)
    {
        return v - (v & (Chunk.size - 1));
    }

    private static int getChunkZ(final int v)
    {
        return v - (v & (Chunk.size - 1));
    }

    private static int getGeneratedChunkX(final int v)
    {
        return v - (v & (Chunk.generatedChunkSize - 1));
    }

    private static int getGeneratedChunkY(final int v)
    {
        return v - (v & (Chunk.generatedChunkSize - 1));
    }

    private static int getGeneratedChunkZ(final int v)
    {
        return v - (v & (Chunk.generatedChunkSize - 1));
    }

    private static final int WorldHashPrime = 22051;

    private static int hashPos(final int x, final int y, final int z)
    {
        int retval = (x * 20903 + y * 35363 + z) % WorldHashPrime;
        if(retval < 0)
            return retval + WorldHashPrime;
        return retval;
    }

    private int hashChunkPos(final int cx, final int cy, final int cz)
    {
        return hashPos(cx, cy, cz);
    }

    private final Chunk[] hashTable = new Chunk[WorldHashPrime];
    private long chunkCount = 0;
    private int maxBucketSize = 0;

    public long getChunkCount()
    {
        return this.chunkCount;
    }

    public int getMaxBucketSize()
    {
        return this.maxBucketSize;
    }

    private void clearHashTable()
    {
        clearLastChunk();
        for(int i = 0; i < WorldHashPrime; i++)
        {
            Chunk head = this.hashTable[i];
            this.hashTable[i] = null;
            while(head != null)
            {
                Chunk freeMe = head;
                head = head.next;
                freeMe.free();
            }
        }
        this.chunksHead = null;
        this.chunkCount = 0;
        this.maxBucketSize = 0;
    }

    private EvalNode[][] genEvalNodeHashTable()
    {
        EvalNode[][] retval = new EvalNode[EvalType.Last.ordinal()][];
        for(int i = 0; i < EvalType.Last.ordinal(); i++)
        {
            retval[i] = new EvalNode[WorldHashPrime];
        }
        return retval;
    }

    private final EvalNode[][] evalNodeHashTable = genEvalNodeHashTable();
    private final EvalNode[] evalNodeListHead = new EvalNode[EvalType.Last.ordinal()];

    private void clearEvalNodes()
    {
        for(EvalType et : EvalType.values)
        {
            if(et == EvalType.Last)
                continue;
            EvalNode head = removeAllEvalNodes(et);
            while(head != null)
            {
                EvalNode freeMe = head;
                head = head.listnext;
                freeMe.free();
            }
        }
    }

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
                newnode.b = null;
                newnode.free();
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
                if(node.b != null)
                    node.b.free();
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

    @SuppressWarnings("unused")
    private EvalNode removeAllEvalNodes(final EvalType et)
    {
        int eti = et.ordinal();
        assert eti >= 0 && eti < EvalTypeCount;
        EvalNode retval = this.evalNodeListHead[eti];
        this.evalNodeListHead[eti] = null;
        int count = 0;
        for(EvalNode node = retval; node != null; node = node.listnext)
        {
            this.evalNodeHashTable[eti][node.hash] = null;
            count++;
        }
        if(Main.DEBUG && false)
            Main.addToFrameText("EvalNode remove count : " + count + "\n");
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

    public void invalidate(final int x, final int y, final int z)
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
        private static final Allocator<TimedInvalidate> allocator = new Allocator<World.TimedInvalidate>()
        {
            @Override
            protected TimedInvalidate allocateInternal()
            {
                return new TimedInvalidate();
            }
        };
        public int x, y, z;
        public double timeLeft;
        public TimedInvalidate next;

        TimedInvalidate()
        {
        }

        public static TimedInvalidate allocate(final int x,
                                               final int y,
                                               final int z,
                                               final double timeLeft)
        {
            return allocator.allocate().init(x, y, z, timeLeft);
        }

        private TimedInvalidate init(final int x,
                                     final int y,
                                     final int z,
                                     final double timeLeft)
        {
            this.x = x;
            this.y = y;
            this.z = z;
            this.timeLeft = timeLeft;
            return this;
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

        public void free()
        {
            this.next = null;
            allocator.free(this);
        }
    }

    private TimedInvalidate timedInvalidateHead = null;

    private void clearTimedInvalidates()
    {
        while(this.timedInvalidateHead != null)
        {
            TimedInvalidate freeMe = this.timedInvalidateHead;
            this.timedInvalidateHead = this.timedInvalidateHead.next;
            freeMe.free();
        }
    }

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
        TimedInvalidate i = TimedInvalidate.allocate(x, y, z, seconds);
        i.next = this.timedInvalidateHead;
        this.timedInvalidateHead = i;
    }

    private void checkAllTimedInvalidates()
    {
        double deltatime = Main.getFrameDuration();
        {
            TimedInvalidate head = null;
            for(TimedInvalidate i = this.timedInvalidateHead; i != null; i = i.next)
            {
                i.advanceTime(deltatime);
                if(i.isReady())
                {
                    invalidate(i.x, i.y, i.z);
                    i.free();
                }
                else
                {
                    i.next = head;
                    head = i;
                }
            }
            this.timedInvalidateHead = head;
        }
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
        int bucketSize = 0;
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
            bucketSize++;
        }
        node = Chunk.allocate(cx, cy, cz);
        this.chunkCount++;
        bucketSize++;
        this.maxBucketSize = Math.max(this.maxBucketSize, bucketSize);
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
        Block b = c.getBlock(x - cx, y - cy, z - cz);
        if(b != null)
            b.checkForBeingAllocated();
        return b;
    }

    private void internalSetBlock(final int x,
                                  final int y,
                                  final int z,
                                  final Block b)
    {
        b.checkForBeingAllocated();
        int cx = getChunkX(x);
        int cy = getChunkY(y);
        int cz = getChunkZ(z);
        Chunk c = findOrInsert(cx, cy, cz);
        Block oldb = c.getBlock(x - cx, y - cy, z - cz);
        if(oldb != null)
            oldb.isInWorld = false;
        c.setBlock(x - cx, y - cy, z - cz, b);
        b.isInWorld = true;
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
                    b.setLightingArray(null,
                                       this.sunlightFactor,
                                       this.displayListValidTag);
                    internalSetBlock(x + dx, y + dy, z + dz, b);
                }
            }
        }
    }

    @SuppressWarnings("unused")
    private void addGeneratedChunk(final GeneratedChunk c)
    {
        if(c == null)
            return;
        assert c.cx % GeneratedChunk.size == 0;
        assert c.cy % GeneratedChunk.size == 0;
        assert c.cz % GeneratedChunk.size == 0;
        for(int cx = c.cx; cx < c.cx + GeneratedChunk.size; cx += Chunk.generatedChunkSize)
        {
            for(int cy = c.cy; cy < c.cy + GeneratedChunk.size; cy += Chunk.generatedChunkSize)
            {
                for(int cz = c.cz; cz < c.cz + GeneratedChunk.size; cz += Chunk.generatedChunkSize)
                {
                    for(int x = cx; x < cx + Chunk.generatedChunkSize; x++)
                    {
                        for(int y = cy; y < cy + Chunk.generatedChunkSize; y++)
                        {
                            for(int z = cz; z < cz + Chunk.generatedChunkSize; z++)
                            {
                                Block temp = getBlock(x, y, z);
                                // TODO finish
                                if(true)
                                    setBlock(x, y, z, c.getBlock(x, y, z).dup());
                                else
                                    internalSetBlock(x,
                                                     y,
                                                     z,
                                                     c.getBlock(x, y, z).dup());
                                if(temp != null)
                                    temp.free();
                            }
                        }
                    }
                    setGenerated(cx, cy, cz, true);
                }
            }
        }
        c.free();
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
        Block b = block;
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
        int count = 100;
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
                    EvalNode freeMe = node;
                    node = node.listnext;
                    freeMe.free();
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
                Block freeMe2 = b;
                b = b.dup();
                if(b.setLighting(newsunlight, newscatteredsunlight, newlight))
                {
                    internalSetBlock(x, y, z, b);
                    freeMe2.free();
                    resetLightingArrays(x, y, z);
                    invalidate(x - 1, y, z);
                    invalidate(x + 1, y, z);
                    invalidate(x, y - 1, z);
                    invalidate(x, y + 1, z);
                    invalidate(x, y, z - 1);
                    invalidate(x, y, z + 1);
                    invalidate(x, y, z);
                }
                else
                    b.free();
                EvalNode freeMe = node;
                node = node.listnext;
                freeMe.free();
            }
            if(count-- <= 0)
                return;
        }
    }

    private void clearTimeOfDay()
    {
        this.sunlightFactor = 15;
        this.timeOfDay = 0.3f;
    }

    private int sunlightFactor = 15; // integer between 0 and 15
    private float timeOfDay = 0.3f;

    /** @param timeOfDay
     *            the time of day
     * @return the new Color */
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

    private void clearBackgroundColor()
    {
        this.backgroundColor.free();
        this.backgroundColor = getBackgroundColor(0.3f);
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
        this.backgroundColor.free();
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

    private void clearGenChunk()
    {
        this.genChunkX = 0;
        this.genChunkY = 0;
        this.genChunkZ = 0;
        this.genChunkDistance = -1f;
    }

    private int genChunkX = 0, genChunkY = 0, genChunkZ = 0;
    private float genChunkDistance = -1.0f;

    private void addGenChunk(final int cx,
                             final int cy,
                             final int cz,
                             final float distance)
    {
        for(int i = 0; i < chunkGeneratorCount; i++)
        {
            if(this.chunkGenerator[i].busy.get()
                    || this.chunkGenerator[i].generated)
            {
                if(cx == this.chunkGenerator[i].cx
                        && cy == this.chunkGenerator[i].cy
                        && cz == this.chunkGenerator[i].cz)
                    return;
            }
        }
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
        int cx = x - (x & (Chunk.generatedChunkSize - 1));
        int cy = y - (y & (Chunk.generatedChunkSize - 1));
        int cz = z - (z & (Chunk.generatedChunkSize - 1));
        if(isGenerated(cx, cy, cz))
            return;
        this.genChunkX = cx;
        this.genChunkY = cy;
        this.genChunkZ = cz;
        this.genChunkDistance = 0.0f;
    }

    private static final float chunkGenScale = 1.5f;
    private static Vector chunkPassClipPlane_t1 = Vector.allocate();

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
            if(p[i].dot(Vector.set(World.chunkPassClipPlane_t1, a, b, c)) + d <= 0)
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

    private static Matrix drawChunk_t1 = Matrix.allocate();

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
                        b.draw(rs, Matrix.setToTranslate(World.drawChunk_t1, x
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
    private static Vector draw_t1 = Vector.allocate();
    private static Vector draw_t2 = Vector.allocate();
    private static Vector draw_cameraPos = Vector.allocate();
    private static Matrix draw_t3 = Matrix.allocate();
    private static Matrix draw_t4 = Matrix.allocate();
    private static Vector draw_chunkCenter = Vector.allocate();
    private static TextureAtlas.TextureHandle starImg = TextureAtlas.addImage(new Image("star.png"));
    private static TextureAtlas.TextureHandle sunsetGlow = TextureAtlas.addImage(new Image("sunsetglow.png"));
    private static final RenderingStream[] draw_rs = new RenderingStream[Chunk.drawPhaseCount];

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
        if(Main.DEBUG)
            Main.addToFrameText("Chunk Count : " + this.chunkCount
                    + "\nEach bucket has "
                    + ((float)this.chunkCount / WorldHashPrime)
                    + " chunks on average.\nMaximum bucket size : "
                    + this.maxBucketSize + "\nEntity Count : "
                    + this.entityCount + "\n");
        RenderingStream rs[] = draw_rs;
        rs[0] = renderingStream;
        rs[1] = transparentRenderingStream;
        for(int i = 2; i < Chunk.drawPhaseCount; i++)
            rs[i] = RenderingStream.allocate();
        Vector cameraPos = Matrix.setToInverse(World.draw_t3, worldToCamera)
                                 .apply(World.draw_cameraPos, Vector.ZERO);
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
            Vector p = Vector.add(World.draw_t1, cameraPos, -0.5f, -0.5f, -0.5f);
            p.addAndSet(Vector.normalize(World.draw_t2, this.sunPosition)
                              .mulAndSet(10.0f));
            RenderingStream.free(sunb.drawAsEntity(RenderingStream.allocate(),
                                                   Matrix.setToTranslate(World.draw_t3,
                                                                         p))
                                     .render());
        }
        if(!this.moonPosition.equals(Vector.ZERO))
        {
            Vector p = Vector.add(World.draw_t1, cameraPos, -0.5f, -0.5f, -0.5f);
            p.addAndSet(Vector.normalize(World.draw_t2, this.moonPosition)
                              .mulAndSet(10.0f));
            RenderingStream.free(moonb.drawAsEntity(RenderingStream.allocate(),
                                                    Matrix.setToTranslate(World.draw_t3,
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
            Matrix tform = Matrix.setToRotateZ(World.draw_t3, Math.PI
                    * (0.5 + Math.floor(2 * this.timeOfDay)));
            RenderingStream.free(Block.drawImgAsBlock(RenderingStream.allocate()
                                                                     .concatMatrix(Matrix.setToScale(World.draw_t4,
                                                                                                     60))
                                                                     .concatMatrix(Matrix.removeTranslate(World.draw_t4,
                                                                                                          worldToCamera)),
                                                      Matrix.setToTranslate(World.draw_t4,
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
                Matrix.setToRotateZ(World.draw_t3,
                                    -Math.PI * 2 * this.timeOfDay)
                      .apply(starPos, starPos);
                Vector p = worldToCamera.applyToNormal(World.draw_t1, starPos)
                                        .mulAndSet(50f);
                Block.drawImgAsEntity(starRenderingStream,
                                      Matrix.setToTranslate(World.draw_t4, p)
                                            .concatAndSet(Matrix.setToScale(World.draw_t3,
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
                                    Vector chunkCenter = World.draw_chunkCenter.set(gcx
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
                        if(Math.abs(cameraX - cx) < 20
                                && Math.abs(cameraY - cy) < 20
                                && Math.abs(cameraZ - cz) < 20
                                && c.fireCount > 0)
                            Main.needFireBurnAudio = true;
                        for(int gcx = cx; gcx < cx + Chunk.size; gcx += Chunk.generatedChunkSize)
                        {
                            for(int gcy = cy; gcy < cy + Chunk.size; gcy += Chunk.generatedChunkSize)
                            {
                                for(int gcz = cz; gcz < cz + Chunk.size; gcz += Chunk.generatedChunkSize)
                                {
                                    if(!isGenerated(gcx, gcy, gcz))
                                    {
                                        Vector chunkCenter = World.draw_chunkCenter.set(gcx
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
            // TODO finish
            // rs[drawPhase].sortByTexture();
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
            rs[drawPhase] = null;
        }
    }

    private int getLightingArrayIndex(final int xOff,
                                      final int yOff,
                                      final int zOff,
                                      final int x_in,
                                      final int y_in,
                                      final int z_in)
    {
        int x = x_in, y = y_in, z = z_in;
        if(xOff > 0)
            x = 1 - x;
        if(yOff > 0)
            y = 1 - y;
        if(zOff > 0)
            z = 1 - z;
        x += xOff;
        y += yOff;
        z += zOff;
        return x + 3 * (y + 3 * z);
    }

    @SuppressWarnings("unused")
    private int getDarkeningFactor(final int xOff,
                                   final int yOff,
                                   final int zOff,
                                   final boolean[] o)
    {
        if(true)
            return 0;
        if(o[getLightingArrayIndex(xOff, yOff, zOff, 1, 1, 1)])
        {
            if(o[getLightingArrayIndex(xOff, yOff, zOff, 0, 0, 0)]
                    && o[getLightingArrayIndex(xOff, yOff, zOff, 1, 1, 0)]
                    && o[getLightingArrayIndex(xOff, yOff, zOff, 1, 0, 1)])
                return 2;
            if(o[getLightingArrayIndex(xOff, yOff, zOff, 1, 1, 0)]
                    && o[getLightingArrayIndex(xOff, yOff, zOff, 0, 0, 0)]
                    && o[getLightingArrayIndex(xOff, yOff, zOff, 0, 1, 1)])
                return 2;
            if(o[getLightingArrayIndex(xOff, yOff, zOff, 1, 0, 1)]
                    && o[getLightingArrayIndex(xOff, yOff, zOff, 0, 1, 1)]
                    && o[getLightingArrayIndex(xOff, yOff, zOff, 0, 0, 0)])
                return 2;
            return 0;
        }
        if(o[getLightingArrayIndex(xOff, yOff, zOff, 1, 0, 0)]
                && o[getLightingArrayIndex(xOff, yOff, zOff, 0, 1, 0)]
                && o[getLightingArrayIndex(xOff, yOff, zOff, 0, 0, 1)])
            return 2;
        // TODO finish
        return 0;
    }

    private final int[] getLightingArray_l = new int[3 * 3 * 3];
    private final boolean[] getLightingArray_o = new boolean[3 * 3 * 3];
    private static final int[] getLightingArray_empty = new int[]
    {
        0, 0, 0, 0, 0, 0, 0, 0
    };

    int[] getLightingArray(final int bx, final int by, final int bz)
    {
        Block b = getBlock(bx, by, bz);
        if(b == null)
        {
            return getLightingArray_empty;
        }
        if(b.getLightingArray(this.sunlightFactor, this.displayListValidTag) != null)
            return b.getLightingArray(this.sunlightFactor,
                                      this.displayListValidTag);
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
        int fl[] = Block.allocateLightingArray();
        assert fl.length == 2 * 2 * 2;
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
                    v = Math.max(0, v - getDarkeningFactor(x, y, z, o) * 3);
                    fl[x + 2 * (y + 2 * z)] = v;
                }
            }
        }
        b.setLightingArray(fl, this.sunlightFactor, this.displayListValidTag);
        return fl;
    }

    static float getLighting(final float x,
                             final float y,
                             final float z,
                             final int l[])
    {
        float nx = 1 - x, ny = 1 - y, nz = 1 - z;
        float l00 = nz * l[0 + 2 * (0 + 2 * 0)] + z * l[0 + 2 * (0 + 2 * 1)];
        float l10 = nz * l[1 + 2 * (0 + 2 * 0)] + z * l[1 + 2 * (0 + 2 * 1)];
        float l01 = nz * l[0 + 2 * (1 + 2 * 0)] + z * l[0 + 2 * (1 + 2 * 1)];
        float l11 = nz * l[1 + 2 * (1 + 2 * 0)] + z * l[1 + 2 * (1 + 2 * 1)];
        float l0 = ny * l00 + y * l01;
        float l1 = ny * l10 + y * l11;
        return (nx * l0 + x * l1) / 15.0f;
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
        return getLighting(x, y, z, l);
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
        this.landGenerator.free();
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
        public final AtomicInteger busyCount;

        public ChunkGenerator(final AtomicInteger busyCount)
        {
            this.busyCount = busyCount;
            this.curThread.setDaemon(true);
            this.curThread.setPriority(Thread.MIN_PRIORITY);
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
                this.busyCount.incrementAndGet();
                this.newChunk = this.landGenerator.genChunk(this.cx,
                                                            this.cy,
                                                            this.cz,
                                                            Chunk.generatedChunkSize
                                                                    * generatedChunkScale);
                this.generated = true;
                synchronized(this.busyCount)
                {
                    this.busy.set(false);
                    this.busyCount.decrementAndGet();
                    this.busyCount.notifyAll();
                }
            }
        }
    }

    public static final int generatedChunkSize = Chunk.generatedChunkSize
            * generatedChunkScale;
    private static final int chunkGeneratorCount = 5;
    private final AtomicInteger chunkGeneratorBusyCount = new AtomicInteger(0);
    private final ChunkGenerator[] chunkGenerator = new ChunkGenerator[chunkGeneratorCount];
    {
        for(int i = 0; i < chunkGeneratorCount; i++)
            this.chunkGenerator[i] = new ChunkGenerator(this.chunkGeneratorBusyCount);
    }

    private void clearChunkGenerator()
    {
        for(int i = 0; i < chunkGeneratorCount; i++)
        {
            synchronized(this.chunkGeneratorBusyCount)
            {
                while(this.chunkGenerator[i].busy.get())
                {
                    try
                    {
                        this.chunkGeneratorBusyCount.wait();
                    }
                    catch(InterruptedException e)
                    {
                    }
                }
            }
            if(this.chunkGenerator[i].landGenerator != null)
                this.chunkGenerator[i].landGenerator.free();
            this.chunkGenerator[i].landGenerator = null;
            if(this.chunkGenerator[i].landGeneratorSettings != null)
                this.chunkGenerator[i].landGeneratorSettings.free();
            this.chunkGenerator[i].landGeneratorSettings = null;
            if(this.chunkGenerator[i].newChunk != null)
                this.chunkGenerator[i].newChunk.free();
            this.chunkGenerator[i].newChunk = null;
        }
    }

    /** generate chunks */
    public void generateChunks()
    {
        for(int i = 0; i < chunkGeneratorCount; i++)
        {
            if(this.chunkGenerator[i].busy.get())
                return;
            if(this.chunkGenerator[i].generated)
            {
                addGeneratedChunk(this.chunkGenerator[i].newChunk);
                this.chunkGenerator[i].generated = false;
                this.chunkGenerator[i].newChunk = null;
            }
            if(this.genChunkDistance < 0)
                return;
            if(isGenerated(this.genChunkX, this.genChunkY, this.genChunkZ))
            {
                this.genChunkDistance = -1;
                return;
            }
            final int generateSize = Chunk.generatedChunkSize
                    * generatedChunkScale;
            this.chunkGenerator[i].cx = this.genChunkX
                    - (this.genChunkX & (generateSize - 1));
            this.chunkGenerator[i].cy = this.genChunkY
                    - (this.genChunkY & (generateSize - 1));
            this.chunkGenerator[i].cz = this.genChunkZ
                    - (this.genChunkZ & (generateSize - 1));
            if(this.chunkGenerator[i].landGenerator == null
                    || this.chunkGenerator[i].landGenerator.getSeed() != this.landGenerator.getSeed()
                    || this.chunkGenerator[i].landGeneratorSettings == null
                    || !this.chunkGenerator[i].landGeneratorSettings.equals(this.landGeneratorSettings))
            {
                if(this.chunkGenerator[i].landGeneratorSettings != null)
                    this.chunkGenerator[i].landGeneratorSettings.free();
                this.chunkGenerator[i].landGeneratorSettings = Rand.Settings.allocate(this.landGeneratorSettings);
                if(this.chunkGenerator[i].landGenerator != null)
                    this.chunkGenerator[i].landGenerator.free();
                this.chunkGenerator[i].landGenerator = Rand.create(this.landGenerator.getSeed(),
                                                                   this.chunkGenerator[i].landGeneratorSettings);
            }
            synchronized(this.chunkGenerator[i].needStart)
            {
                this.chunkGenerator[i].busy.set(true);
                this.chunkGenerator[i].needStart.set(true);
                this.chunkGenerator[i].needStart.notifyAll();
            }
            this.genChunkDistance = -1;
        }
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

    private void removeAllClearEntities()
    {
        for(EntityNode node = this.entityHead; node != null; node = node.next)
        {
            if(node.e.isEmpty())
                players.handleEntityRemove(node.e);
        }
        for(EntityNode node = removeAllEntities(), nextNode = (node != null ? node.next
                : null); node != null; node = nextNode, nextNode = (node != null ? node.next
                : null))
        {
            if(!node.e.isEmpty())
                insertEntity(node);
            else
                node.free();
        }
    }

    private static final class EntityListNode
    {
        public EntityListNode next;
        public EntityNode node;

        private EntityListNode()
        {
        }

        private static final Allocator<EntityListNode> allocator = new Allocator<World.EntityListNode>()
        {
            @SuppressWarnings("synthetic-access")
            @Override
            protected EntityListNode allocateInternal()
            {
                return new EntityListNode();
            }
        };

        public void free()
        {
            this.next = null;
            this.node = null;
            allocator.free(this);
        }

        public static EntityListNode allocate()
        {
            return allocator.allocate();
        }
    }

    private EntityListNode makeEntityList()
    {
        EntityListNode retval = null;
        for(EntityNode node = this.entityHead; node != null; node = node.next)
        {
            EntityListNode newNode = EntityListNode.allocate();
            newNode.next = retval;
            newNode.node = node;
            retval = newNode;
        }
        return retval;
    }

    private void moveEntities()
    {
        for(EntityListNode node = makeEntityList(), freeMe = node; node != null; node = node.next, freeMe.free(), freeMe = node)
        {
            removeEntityNode(node.node);
            node.node.e.move();
            insertEntity(node.node);
        }
        players.entityCheckHitPlayers();
        removeAllClearEntities();
    }

    private void removeEntityNode(final EntityNode node)
    {
        node.isInList = false;
        if(node.e.isEmpty())
        {
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
            node.hashnext = null;
            node.hashprev = null;
            if(node.isFree || node.isInList || node == this.entityHead
                    || node == this.entityTail)
                throw new RuntimeException("can't insert currently used entity");
            this.entityCount--;
            return;
        }
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
        if(node.isFree || node.isInList || node == this.entityHead
                || node == this.entityTail)
            throw new RuntimeException("can't insert currently used entity");
        this.entityCount--;
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
            {
                players.handleEntityRemove(node.e);
                node.free();
            }
        }
    }

    void checkHitPlayer(final Player p)
    {
        for(EntityListNode node = makeEntityList(), freeMe = node; node != null; node = node.next, freeMe.free(), freeMe = node)
        {
            removeEntityNode(node.node);
            node.node.e.checkHitPlayer(p);
            insertEntity(node.node);
        }
    }

    private void moveRedstone()
    {
        EvalNode freeMe;
        for(EvalNode node = removeAllEvalNodes(EvalType.RedstoneFirst); node != null; freeMe = node, node = node.listnext, freeMe.free())
        {
            Block b = getBlockEval(node.x, node.y, node.z);
            if(b != null)
            {
                b = b.redstoneMove(node.x, node.y, node.z);
                if(b != null)
                    insertEvalNode(EvalType.RedstoneFirst,
                                   node.x,
                                   node.y,
                                   node.z,
                                   b);
            }
        }
        for(EvalNode node = removeAllEvalNodes(EvalType.RedstoneFirst); node != null; freeMe = node, node = node.listnext, freeMe.free())
        {
            if(node.b == null)
                continue;
            Block temp = getBlock(node.x, node.y, node.z);
            setBlock(node.x, node.y, node.z, node.b.dup());
            temp.free();
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
                Block temp = getBlock(node.x, node.y, node.z);
                setBlock(node.x, node.y, node.z, node.b.dup());
                temp.free();
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
                                   Block.NewEmpty());
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
            Block temp = getBlock(node.x, node.y, node.z);
            setBlock(node.x, node.y, node.z, node.b.dup());
            temp.free();
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

    private void moveFire()
    {
        EvalNode node = removeAllEvalNodes(EvalType.Fire);
        while(node != null)
        {
            Block b = getBlockEval(node.x, node.y, node.z);
            if(b != null)
                b.moveFire(node.x, node.y, node.z);
            EvalNode freeMe = node;
            node = node.listnext;
            freeMe.free();
        }
    }

    private static final float redstoneMovePeriod = 0.1f;
    private static final float generalMovePeriod = 0.25f;

    private void clearMoveAllBlocks()
    {
        this.redstoneMoveTimeLeft = redstoneMovePeriod;
        this.generalMoveTimeLeft = generalMovePeriod;
    }

    private float redstoneMoveTimeLeft = redstoneMovePeriod;
    private float generalMoveTimeLeft = generalMovePeriod;

    private void moveAllBlocks()
    {
        this.generalMoveTimeLeft -= (float)Main.getFrameDuration();
        if(this.generalMoveTimeLeft <= 0)
        {
            this.generalMoveTimeLeft += generalMovePeriod;
            moveGeneral();
            moveFire();
        }
        this.redstoneMoveTimeLeft -= (float)Main.getFrameDuration();
        if(this.redstoneMoveTimeLeft <= 0)
        {
            this.redstoneMoveTimeLeft += redstoneMovePeriod;
            moveRedstone();
            movePistons();
        }
    }

    private void clearParticleGenTime()
    {
        this.particleGenTime = 0;
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
        if(this.chunkCount == 0)
            return;
        int count = (int)Math.floor(Chunk.size * Chunk.size * Chunk.size * 3
                / 16f / 16f / 16f * this.chunkCount + fRand(0, 1) % 1f);
        for(int i = 0; i < count; i++)
        {
            int hash = (int)Math.floor(fRand(0, WorldHashPrime))
                    % WorldHashPrime;
            while(this.hashTable[hash] == null)
            {
                hash++;
                hash %= WorldHashPrime;
            }
            int bucketSize = 0;
            for(Chunk node = this.hashTable[hash]; node != null; node = node.next)
                bucketSize++;
            int index = (int)Math.floor(fRand(0, bucketSize));
            if(index >= bucketSize)
                index = bucketSize - 1;
            for(Chunk node = this.hashTable[hash]; node != null; node = node.next)
            {
                if(index-- <= 0)
                {
                    int x = node.orgx
                            + (int)Math.floor(fRand(0.0f, Chunk.size));
                    int y = node.orgy
                            + (int)Math.floor(fRand(0.0f, Chunk.size));
                    int z = node.orgz
                            + (int)Math.floor(fRand(0.0f, Chunk.size));
                    Block b = getBlockEval(x, y, z);
                    if(b != null)
                    {
                        Block destB = b.moveRandom(x, y, z);
                        if(destB != null)
                        {
                            setBlock(x, y, z, destB);
                            b.free();
                        }
                    }
                    break;
                }
            }
        }
    }

    private void clearCurTime()
    {
        this.curTime = 0;
    }

    private double curTime = 0.0;
    public static boolean useFastTime = false;

    /** moves everything in this world except the players */
    public void move()
    {
        this.curTime += Main.getFrameDuration();
        final float dayDuration = 20.0f * 60.0f;
        setTimeOfDay(this.timeOfDay + (float)Main.getFrameDuration()
                / dayDuration * ((Main.DEBUG && useFastTime) ? 20 : 1));
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

    public static final class BlockHitDescriptor
    {
        public Block b;
        public Entity e;
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
            this.e = null;
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

        public BlockHitDescriptor(final Entity e, final float distance)
        {
            this.b = null;
            this.e = e;
            this.x = 0;
            this.y = 0;
            this.z = 0;
            this.orientation = -1;
            this.hitUnloadedChunk = false;
            this.distance = distance;
        }

        public BlockHitDescriptor init(final int x,
                                       final int y,
                                       final int z,
                                       final int orientation,
                                       final float distance,
                                       final Block b)
        {
            this.b = b;
            this.e = null;
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

        public BlockHitDescriptor init(final Entity e, final float distance)
        {
            this.b = null;
            this.e = e;
            this.x = 0;
            this.y = 0;
            this.z = 0;
            this.orientation = -1;
            this.hitUnloadedChunk = false;
            this.distance = distance;
            return this;
        }
    }

    private static Vector internalGetPointedAtBlock_pos = Vector.allocate();
    private static Vector internalGetPointedAtBlock_dir = Vector.allocate();
    private static Vector internalGetPointedAtBlock_invdir = Vector.allocate();
    private static Vector internalGetPointedAtBlock_nextxinc = Vector.allocate();
    private static Vector internalGetPointedAtBlock_nextyinc = Vector.allocate();
    private static Vector internalGetPointedAtBlock_nextzinc = Vector.allocate();
    private static Vector internalGetPointedAtBlock_vt = Vector.allocate();
    private static Vector internalGetPointedAtBlock_nextx = Vector.allocate();
    private static Vector internalGetPointedAtBlock_nexty = Vector.allocate();
    private static Vector internalGetPointedAtBlock_nextz = Vector.allocate();
    private static Vector internalGetPointedAtBlock_vtinc = Vector.allocate();
    private static Vector internalGetPointedAtBlock_t1 = Vector.allocate();
    private static Vector internalGetPointedAtBlock_t2 = Vector.allocate();
    private static Vector internalGetPointedAtBlock_newpos = Vector.allocate();

    private BlockHitDescriptor
        internalGetPointedAtBlock(final BlockHitDescriptor retval,
                                  final Vector pos_in,
                                  final Vector dir_in,
                                  final float maxDist_in,
                                  final boolean getBlockRightBefore,
                                  final boolean calcPassThruWater,
                                  final boolean passThruWater_in)
    {
        float maxDist = Float.isNaN(maxDist_in) ? 128
                : Math.max(0, Math.min(128, maxDist_in));
        int finishx = 0, finishy = 0, finishz = 0, orientation = -1;
        Vector pos = Vector.set(World.internalGetPointedAtBlock_pos, pos_in);
        Vector dir = Vector.set(World.internalGetPointedAtBlock_dir, dir_in);
        final float eps = 1e-4f;
        if(Math.abs(dir.getX()) < eps)
            dir.setX(eps);
        if(Math.abs(dir.getY()) < eps)
            dir.setY(eps);
        if(Math.abs(dir.getZ()) < eps)
            dir.setZ(eps);
        Vector invdir = Vector.set(World.internalGetPointedAtBlock_invdir,
                                   1.0f / dir.getX(),
                                   1.0f / dir.getY(),
                                   1.0f / dir.getZ());
        int ix = (int)Math.floor(pos.getX());
        int iy = (int)Math.floor(pos.getY());
        int iz = (int)Math.floor(pos.getZ());
        int previx = 0, previy = 0, previz = 0;
        int cx, cy, cz;
        cx = getChunkX(ix);
        cy = getChunkY(iy);
        cz = getChunkZ(iz);
        int lastcx = 0x80000000, lastcy = 0, lastcz = 0;
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
        Vector nextxinc = Vector.set(World.internalGetPointedAtBlock_nextxinc,
                                     (dir.getX() < 0) ? -1 : 1,
                                     0,
                                     0);
        Vector nextyinc = Vector.set(World.internalGetPointedAtBlock_nextyinc,
                                     0,
                                     (dir.getY() < 0) ? -1 : 1,
                                     0);
        Vector nextzinc = Vector.set(World.internalGetPointedAtBlock_nextzinc,
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
        Vector vt = World.internalGetPointedAtBlock_vt;
        Vector nextx = World.internalGetPointedAtBlock_nextx, nexty = World.internalGetPointedAtBlock_nexty, nextz = World.internalGetPointedAtBlock_nextz;
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
        Vector vtinc = Vector.set(World.internalGetPointedAtBlock_vtinc,
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
                                                  Vector.sub(World.internalGetPointedAtBlock_t1,
                                                             pos,
                                                             ix,
                                                             iy,
                                                             iz),
                                                  Vector.sub(World.internalGetPointedAtBlock_t2,
                                                             pos,
                                                             ix,
                                                             iy,
                                                             iz),
                                                  ix,
                                                  iy,
                                                  iz);
        Entity hitEntity = null;
        cx = getChunkX(ix);
        cy = getChunkY(iy);
        cz = getChunkZ(iz);
        if(cx != lastcx || cy != lastcy || cz != lastcz)
        {
            lastcx = cx;
            lastcy = cy;
            lastcz = cz;
            for(int dx = -Chunk.size; dx <= Chunk.size; dx += Chunk.size)
            {
                for(int dy = -Chunk.size; dy <= Chunk.size; dy += Chunk.size)
                {
                    for(int dz = -Chunk.size; dz <= Chunk.size; dz += Chunk.size)
                    {
                        Chunk c = find(cx + dx, cy + dy, cz + dz);
                        if(c != null)
                        {
                            EntityNode node = c.head;
                            while(node != null)
                            {
                                if(node.e != null && !node.e.isEmpty())
                                {
                                    float t = node.e.rayHitEntity(pos_in, dir);
                                    if(t >= 0 && t <= maxDist)
                                    {
                                        hitEntity = node.e;
                                        maxDist = t;
                                    }
                                }
                                node = node.hashnext;
                            }
                        }
                    }
                }
            }
        }
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
            {
                float t;
                Vector newpos;
                if(vt.getX() < vt.getY())
                {
                    if(vt.getX() < vt.getZ())
                    {
                        t = vt.getX();
                        newpos = Vector.set(World.internalGetPointedAtBlock_newpos,
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
                        newpos = Vector.set(World.internalGetPointedAtBlock_newpos,
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
                        newpos = Vector.set(World.internalGetPointedAtBlock_newpos,
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
                        newpos = Vector.set(World.internalGetPointedAtBlock_newpos,
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
                pos = Vector.set(World.internalGetPointedAtBlock_pos, newpos);
                totalt += t;
            }
            if(totalt > maxDist)
            {
                if(hitEntity != null)
                    return retval.init(hitEntity, maxDist);
                return retval.init();
            }
            b = getBlock(ix, iy, iz);
            rayIntersectsRetval = -1;
            if(b != null)
                rayIntersectsRetval = b.rayIntersects(dir,
                                                      invdir,
                                                      Vector.sub(World.internalGetPointedAtBlock_t1,
                                                                 pos,
                                                                 ix,
                                                                 iy,
                                                                 iz),
                                                      Vector.sub(World.internalGetPointedAtBlock_t2,
                                                                 pos,
                                                                 ix,
                                                                 iy,
                                                                 iz),
                                                      ix,
                                                      iy,
                                                      iz);
            cx = getChunkX(ix);
            cy = getChunkY(iy);
            cz = getChunkZ(iz);
            if(cx != lastcx || cy != lastcy || cz != lastcz)
            {
                lastcx = cx;
                lastcy = cy;
                lastcz = cz;
                for(int dx = -Chunk.size; dx <= Chunk.size; dx += Chunk.size)
                {
                    for(int dy = -Chunk.size; dy <= Chunk.size; dy += Chunk.size)
                    {
                        for(int dz = -Chunk.size; dz <= Chunk.size; dz += Chunk.size)
                        {
                            Chunk c = find(cx + dx, cy + dy, cz + dz);
                            if(c != null)
                            {
                                EntityNode node = c.head;
                                while(node != null)
                                {
                                    if(node.e != null)
                                    {
                                        float t = node.e.rayHitEntity(pos_in,
                                                                      dir);
                                        if(t >= 0 && t <= maxDist)
                                        {
                                            hitEntity = node.e;
                                            maxDist = t;
                                        }
                                    }
                                    node = node.hashnext;
                                }
                            }
                        }
                    }
                }
            }
        }
        totalt += Math.max(0, rayIntersectsRetval);
        if(totalt > maxDist)
        {
            if(hitEntity != null)
                return retval.init(hitEntity, maxDist);
            return retval.init();
        }
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

    public BlockHitDescriptor
        getPointedAtBlock(final BlockHitDescriptor retval,
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

    private static Matrix getPointedAtBlock_cameraToWorld = Matrix.allocate();
    private static Vector getPointedAtBlock_org = Vector.allocate();
    private static Vector getPointedAtBlock_dir = Vector.allocate();

    public BlockHitDescriptor
        getPointedAtBlock(final BlockHitDescriptor retval,
                          final Matrix worldToCamera,
                          final float maxDist,
                          final boolean getBlockRightBefore)
    {
        Vector org = Vector.set(World.getPointedAtBlock_org, 0, 0, 0);
        Vector dir = Vector.set(World.getPointedAtBlock_dir, 0, 0, -1);
        Matrix cameraToWorld = Matrix.setToInverse(World.getPointedAtBlock_cameraToWorld,
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
        private static final Allocator<TreeGenerateLocation> allocator = new Allocator<TreeGenerateLocation>()
        {
            @Override
            protected TreeGenerateLocation allocateInternal()
            {
                return new TreeGenerateLocation();
            }
        };
        public int x, y, z;
        public Block startingSapling;
        public TreeGenerateLocation next;

        TreeGenerateLocation()
        {
        }

        public static TreeGenerateLocation
            allocate(final int x,
                     final int y,
                     final int z,
                     final Block b,
                     final TreeGenerateLocation next)
        {
            return allocator.allocate().init(x, y, z, b, next);
        }

        private TreeGenerateLocation init(final int x,
                                          final int y,
                                          final int z,
                                          final Block b,
                                          final TreeGenerateLocation next)
        {
            this.x = x;
            this.y = y;
            this.z = z;
            this.startingSapling = b.dup();
            this.next = next;
            return this;
        }

        public void free()
        {
            this.startingSapling.free();
            this.next = null;
            allocator.free(this);
        }
    }

    private TreeGenerateLocation treeGenerateListHead = null;

    private void clearTreeGenerateList()
    {
        while(this.treeGenerateListHead != null)
        {
            TreeGenerateLocation freeMe = this.treeGenerateListHead;
            this.treeGenerateListHead = this.treeGenerateListHead.next;
            freeMe.free();
        }
    }

    private void generateAllTrees()
    {
        while(this.treeGenerateListHead != null)
        {
            TreeGenerateLocation next = this.treeGenerateListHead.next;
            Tree.generate(this.treeGenerateListHead.startingSapling.dup(),
                          this.treeGenerateListHead.x,
                          this.treeGenerateListHead.y,
                          this.treeGenerateListHead.z);
            TreeGenerateLocation freeMe = this.treeGenerateListHead;
            this.treeGenerateListHead = next;
            freeMe.free();
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
     *            the sapling that created this tree (duplicated) */
    public void addNewTree(final int x,
                           final int y,
                           final int z,
                           final Block startingSapling)
    {
        this.treeGenerateListHead = TreeGenerateLocation.allocate(x,
                                                                  y,
                                                                  z,
                                                                  startingSapling,
                                                                  this.treeGenerateListHead);
    }

    private static final int fileVersion = 3;

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
            if(!node.e.isEmpty())
                entitycount++;
        }
        o.writeInt(entitycount);
        if(world.entityHead != null)
        {
            Main.pushProgress(0, 1.0f / entitycount);
            int progress = 0;
            for(EntityNode node = world.entityHead; node != null; node = node.next)
            {
                if(node.e.isEmpty())
                    continue;
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
            Main.pushProgress(EvalTypeCount, 0.5f / timedInvalidateCount);
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
     * @return the file version
     * @throws IOException
     *             the exception thrown */
    public static int read(final DataInput i) throws IOException
    {
        int v = i.readInt();
        readVer3(i, v);
        return v;
    }

    private static void
        readVer3(final DataInput i, final int v) throws IOException
    {
        if(v == 3)
            readVer2(i, 2);
        else
            readVer2(i, v);
    }

    private static void
        readVer2(final DataInput i, final int v) throws IOException
    {
        if(v != 2)
        {
            readVer1(i, v);
            return;
        }
        int seed = i.readInt();
        clear(seed);
        world.setLandGeneratorSettings(Rand.Settings.read(i), true);
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
        world.free();
        world = allocator.allocate().init();
        world.setSeed(seed);
    }

    /** clears <code>world</code> to a new world */
    public static void clear()
    {
        world.free();
        world = allocator.allocate().init();
    }

    private static class ExplosionNode
    {
        private static final Allocator<ExplosionNode> allocator = new Allocator<World.ExplosionNode>()
        {
            @Override
            protected ExplosionNode allocateInternal()
            {
                return new ExplosionNode();
            }
        };
        public int x, y, z;
        public float strength;
        public ExplosionNode next;
        public boolean ignoreRails;

        ExplosionNode()
        {
        }

        public static ExplosionNode allocate(final int x,
                                             final int y,
                                             final int z,
                                             final float strength,
                                             final ExplosionNode next,
                                             final boolean ignoreRails)
        {
            return allocator.allocate().init(x,
                                             y,
                                             z,
                                             strength,
                                             next,
                                             ignoreRails);
        }

        private ExplosionNode init(final int x,
                                   final int y,
                                   final int z,
                                   final float strength,
                                   final ExplosionNode next,
                                   final boolean ignoreRails)
        {
            this.x = x;
            this.y = y;
            this.z = z;
            this.strength = strength;
            this.next = next;
            this.ignoreRails = ignoreRails;
            return this;
        }

        public void free()
        {
            this.next = null;
            allocator.free(this);
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
     *            the strength of the new explosion
     * @param ignoreRails
     *            if rails and supporting blocks should be ignored */
    public void addExplosion(final int x,
                             final int y,
                             final int z,
                             final float strength,
                             final boolean ignoreRails)
    {
        this.explosionList = ExplosionNode.allocate(x,
                                                    y,
                                                    z,
                                                    strength,
                                                    this.explosionList,
                                                    ignoreRails);
        int particleCount = Math.max(1,
                                     (int)Math.floor(strength * strength
                                             * strength / 8));
        float maxDist = strength / 2;
        for(int i = 0; i < particleCount; i++)
        {
            Vector p = vRand(Vector.allocate(),
                             (float)Math.pow(fRand(0, maxDist), 2)).addAndSet(x + 0.5f,
                                                                              y + 0.5f,
                                                                              z + 0.5f);
            insertEntity(Entity.NewParticle(p,
                                            ParticleType.Explosion,
                                            Vector.ZERO));
            p.free();
        }
    }

    private static final Block getExplosionStrength_empty = Block.NewEmpty();

    private float getExplosionStrength(final int x,
                                       final int y,
                                       final int z,
                                       final float strength,
                                       final boolean ignoreRails)
    {
        Block b = getBlockEval(x, y, z);
        if(b == null)
            return 0.0f;
        if(ignoreRails && Block.isRailOrSupportingRails(x, y, z))
            b = getExplosionStrength_empty;
        if(!b.isExplodable())
            return 0.0f;
        return Math.max(0, strength - (b.getBlastResistance() / 5 + 0.3f)
                * 0.3f);
    }

    private static class BlockLoc implements Allocatable
    {
        private static final Allocator<BlockLoc> allocator = new Allocator<World.BlockLoc>()
        {
            @Override
            protected BlockLoc allocateInternal()
            {
                return new BlockLoc();
            }
        };
        public int x, y, z;

        BlockLoc()
        {
        }

        private BlockLoc init(final int x, final int y, final int z)
        {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }

        public static BlockLoc allocate(final int x, final int y, final int z)
        {
            return allocator.allocate().init(x, y, z);
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

        @Override
        public void free()
        {
            allocator.free(this);
        }

        @Override
        public BlockLoc dup()
        {
            return allocate(this.x, this.y, this.z);
        }
    }

    private static Vector runExplosionRay_step = Vector.allocate();

    private void
        runExplosionRay(final AllocatorHashMap<BlockLoc, BlockLoc> destroyedBlocks,
                        final Vector init_pos,
                        final Vector dir,
                        final float init_strength,
                        final boolean ignoreRails)
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
                BlockLoc position = BlockLoc.allocate(lastBlockX,
                                                      lastBlockY,
                                                      lastBlockZ);
                destroyedBlocks.put(position, null);
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
                                            strength,
                                            ignoreRails);
            pos = pos.addAndSet(step);
        }
    }

    private static Vector runExplosion_t1 = Vector.allocate();

    private void runExplosion(final int x,
                              final int y,
                              final int z,
                              final boolean ignoreRails)
    {
        Block b = getBlockEval(x, y, z);
        if(b == null)
            return;
        if(ignoreRails && Block.isRailOrSupportingRails(x, y, z))
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
        setBlock(x, y, z, Block.NewEmpty());
        b.free();
    }

    private static Vector runExplosion_dir = Vector.allocate();
    private static Vector runExplosion_pos = Vector.allocate();

    private void runExplosion(final ExplosionNode explosion)
    {
        explodeEntities(runExplosion_t1.set(explosion.x + 0.5f,
                                            explosion.y + 0.5f,
                                            explosion.z + 0.5f),
                        explosion.strength);
        @SuppressWarnings("unchecked")
        AllocatorHashMap<BlockLoc, BlockLoc> destroyedBlocks = (AllocatorHashMap<BlockLoc, BlockLoc>)AllocatorHashMap.allocate();
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
                                    fRand(0.7f, 1.3f) * explosion.strength,
                                    explosion.ignoreRails);
                }
            }
        }
        AllocatorHashMap.Iterator<BlockLoc, BlockLoc> iter = destroyedBlocks.iterator();
        for(; !iter.isEnd(); iter.next())
        {
            BlockLoc i = iter.getKey();
            runExplosion(i.x, i.y, i.z, explosion.ignoreRails);
        }
        iter.free();
        destroyedBlocks.free();
    }

    private void runAllExplosions()
    {
        ExplosionNode allExplosions = this.explosionList;
        if(allExplosions == null)
            return;
        this.explosionList = null;
        ExplosionNode node = allExplosions;
        if(node != null)
        {
            Main.play(Main.explodeAudio);
        }
        while(node != null)
        {
            runExplosion(node);
            ExplosionNode freeMe = node;
            node = node.next;
            freeMe.free();
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

    public void invalidateLightingArrays()
    {
        this.displayListValidTag++;
    }

    private void clearDisplayListValidTag()
    {
        this.displayListValidTag = 0;
    }

    public static final class EntityIterator
    {
        public static final class ListNode
        {
            public ListNode next;
            public Entity e;
            private static final Allocator<ListNode> allocator = new Allocator<ListNode>()
            {
                @SuppressWarnings("synthetic-access")
                @Override
                protected ListNode allocateInternal()
                {
                    return new ListNode();
                }
            };

            private ListNode()
            {
            }

            public void free()
            {
                this.next = null;
                this.e = null;
                allocator.free(this);
            }

            public static ListNode
                allocate(final ListNode next, final Entity e)
            {
                ListNode retval = allocator.allocate();
                retval.next = next;
                retval.e = e;
                return retval;
            }
        }

        private ListNode list = null;

        private EntityIterator()
        {
        }

        private static final Allocator<EntityIterator> allocator = new Allocator<EntityIterator>()
        {
            @SuppressWarnings("synthetic-access")
            @Override
            protected EntityIterator allocateInternal()
            {
                return new EntityIterator();
            }
        };

        private EntityIterator init(final ListNode list)
        {
            this.list = list;
            return this;
        }

        public static EntityIterator allocate(final ListNode list)
        {
            return allocator.allocate().init(list);
        }

        public boolean hasNext()
        {
            return this.list != null;
        }

        public Entity next()
        {
            if(this.list == null)
                return null;
            Entity retval = this.list.e;
            ListNode freeMe = this.list;
            this.list = this.list.next;
            freeMe.free();
            return retval;
        }

        public void free()
        {
            while(hasNext())
                next();
            this.list = null;
            allocator.free(this);
        }
    }

    public EntityIterator getEntityList(final float minx,
                                        final float maxx,
                                        final float miny,
                                        final float maxy,
                                        final float minz,
                                        final float maxz)
    {
        EntityIterator.ListNode head = null;
        int mincx = getChunkX((int)Math.floor(minx));
        int maxcx = getChunkX((int)Math.floor(maxx));
        int mincy = getChunkY((int)Math.floor(miny));
        int maxcy = getChunkY((int)Math.floor(maxy));
        int mincz = getChunkZ((int)Math.floor(minz));
        int maxcz = getChunkZ((int)Math.floor(maxz));
        for(int cx = mincx; cx <= maxcx; cx += Chunk.size)
        {
            for(int cy = mincy; cy <= maxcy; cy += Chunk.size)
            {
                for(int cz = mincz; cz <= maxcz; cz += Chunk.size)
                {
                    Chunk c = find(cx, cy, cz);
                    if(c == null)
                        continue;
                    for(EntityNode node = c.head; node != null; node = node.hashnext)
                    {
                        Entity e = node.e;
                        if(e.isEmpty())
                            continue;
                        Vector pos = e.getPosition();
                        if(pos.getX() < minx || pos.getX() >= maxx)
                            continue;
                        if(pos.getY() < miny || pos.getY() >= maxy)
                            continue;
                        if(pos.getZ() < minz || pos.getZ() >= maxz)
                            continue;
                        head = EntityIterator.ListNode.allocate(head, e);
                    }
                }
            }
        }
        return EntityIterator.allocate(head);
    }

    public EntityIterator getBlockEntityList(final int bx,
                                             final int by,
                                             final int bz)
    {
        return getEntityList(bx, bx + 1, by, by + 1, bz, bz + 1);
    }
}
