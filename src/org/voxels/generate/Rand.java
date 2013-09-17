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

import java.io.*;
import java.util.Random;

import org.voxels.*;
import org.voxels.generate.Plant.PlantType;
import org.voxels.mobs.MobType;
import org.voxels.mobs.Mobs;

/** Land generator
 * 
 * @author jacob */
public final class Rand
{
    public static int getBiomeCount()
    {
        return Biome.values.length;
    }

    public static String getBiomeName(final int index)
    {
        return Biome.values[index].getName();
    }

    /** @author jacob */
    public static final class Settings implements Allocatable
    {
        private static final Allocator<Settings> allocator = new Allocator<Settings>()
        {
            @SuppressWarnings("synthetic-access")
            @Override
            protected Settings allocateInternal()
            {
                return new Settings();
            }
        };
        /**
         * 
         */
        public boolean isSuperflat;
        public String startingBiome;
        private static final int version = 1;

        private Settings()
        {
        }

        /**
         * 
         */
        private Settings init()
        {
            this.isSuperflat = false;
            this.startingBiome = "";
            return this;
        }

        private Settings init(final Settings rt)
        {
            this.isSuperflat = rt.isSuperflat;
            this.startingBiome = rt.startingBiome;
            return this;
        }

        public static Settings allocate()
        {
            return allocator.allocate().init();
        }

        public static Settings allocate(final Settings rt)
        {
            return allocator.allocate().init(rt);
        }

        public boolean isStartingBiomeValid()
        {
            for(int index = 0; index < Rand.getBiomeCount(); index++)
            {
                if(this.startingBiome.equals(Rand.getBiomeName(index)))
                    return true;
            }
            return this.startingBiome.equals("");
        }

        /** write to a <code>DataOutput</code>
         * 
         * @param o
         *            <code>OutputStream</code> to write to
         * @throws IOException
         *             the exception thrown */
        public void write(final DataOutput o) throws IOException
        {
            o.writeInt(version);
            o.writeBoolean(this.isSuperflat);
            o.writeInt(this.startingBiome.length());
            o.writeChars(this.startingBiome);
        }

        private void
            internalReadVer0(final DataInput i, final int curVersion) throws IOException
        {
            if(curVersion != 0)
                throw new IOException("invalid Rand.Settings version");
            this.isSuperflat = i.readBoolean();
        }

        private void
            internalReadVer1(final DataInput i, final int curVersion) throws IOException
        {
            if(curVersion != 1)
                internalReadVer0(i, curVersion);
            this.isSuperflat = i.readBoolean();
            int length = i.readInt();
            StringBuilder sb = new StringBuilder(length);
            for(int j = 0; j < length; j++)
                sb.append(i.readChar());
            this.startingBiome = sb.toString();
        }

        /** read from a <code>DataInput</code>
         * 
         * @param i
         *            <code>DataInput</code> to read from
         * @return the read <code>Settings</code>
         * @throws IOException
         *             the exception thrown */
        public static Settings read(final DataInput i) throws IOException
        {
            Settings retval = allocator.allocate().init();
            int curVersion = i.readInt();
            retval.internalReadVer1(i, curVersion);
            return retval;
        }

        @Override
        public boolean equals(final Object o)
        {
            if(o == null || !(o instanceof Settings))
                return false;
            Settings rt = (Settings)o;
            if(rt.isSuperflat != this.isSuperflat)
                return false;
            if(!rt.startingBiome.equals(this.startingBiome))
                return false;
            return true;
        }

        @Override
        public int hashCode()
        {
            return Boolean.valueOf(this.isSuperflat).hashCode() + 1271266283
                    * this.startingBiome.hashCode();
        }

        @Override
        public Settings dup()
        {
            return allocate(this);
        }

        @Override
        public void free()
        {
            allocator.free(this);
        }
    }

    /** @author jacob */
    public static final class SettingsCreatorMenu extends MenuScreen
    {
        /**
         * 
         */
        public final Settings settings;

        /**
         * 
         */
        public SettingsCreatorMenu()
        {
            super(Color.V(0.75f));
            this.settings = Settings.allocate();
            add(new CheckMenuItem("Superflat",
                                  Color.RGB(0f, 0f, 0f),
                                  getBackgroundColor(),
                                  Color.RGB(0f, 0f, 0f),
                                  Color.RGB(0.0f, 0.0f, 1.0f),
                                  this)
            {
                @Override
                public void setChecked(final boolean checked)
                {
                    SettingsCreatorMenu.this.settings.isSuperflat = checked;
                }

                @Override
                public boolean isChecked()
                {
                    return SettingsCreatorMenu.this.settings.isSuperflat;
                }
            });
            add(new SpacerMenuItem(Color.V(0), this));
            if(Main.DEBUG)
            {
                for(int index = 0; index < Rand.getBiomeCount(); index++)
                {
                    final String biomeName = Rand.getBiomeName(index);
                    add(new OptionMenuItem("Start In : " + biomeName,
                                           Color.RGB(0f, 0f, 0f),
                                           getBackgroundColor(),
                                           Color.RGB(0f, 0f, 0f),
                                           Color.RGB(0.0f, 0.0f, 1.0f),
                                           this)
                    {
                        @Override
                        public void pick()
                        {
                            SettingsCreatorMenu.this.settings.startingBiome = biomeName;
                        }

                        @Override
                        public boolean isPicked()
                        {
                            return SettingsCreatorMenu.this.settings.startingBiome.equals(biomeName);
                        }
                    });
                }
                add(new SpacerMenuItem(Color.V(0), this));
            }
            add(new TextMenuItem("Create World",
                                 Color.RGB(0.0f, 0.0f, 0.0f),
                                 getBackgroundColor(),
                                 Color.RGB(0.0f, 0.0f, 0.0f),
                                 Color.RGB(0.0f, 0.0f, 1.0f),
                                 this)
            {
                @Override
                public void onMouseOver(final float mouseX, final float mouseY)
                {
                    select();
                }

                @Override
                public void onClick(final float mouseX, final float mouseY)
                {
                    this.container.close();
                }
            });
        }

        @Override
        protected void drawBackground(final Matrix tform)
        {
            drawTextBackground("New World Settings", tform);
        }
    }

    Rand()
    {
        this.plantChunkHashTableSync = new Object[hashPrime];
        for(int i = 0; i < hashPrime; i++)
            this.plantChunkHashTableSync[i] = new Object();
    }

    private static final Allocator<Rand> allocator = new Allocator<Rand>(10)
    {
        @Override
        protected Rand allocateInternal()
        {
            return new Rand();
        }
    };
    private int seed;
    /**
     * 
     */
    public boolean isSuperflat;

    private Rand init(final int seed, final Settings settings)
    {
        this.seed = seed;
        this.isSuperflat = settings.isSuperflat;
        return this;
    }

    private static final Random init_rand = new Random();

    private Rand init(final Settings settings)
    {
        this.seed = init_rand.nextInt();
        this.isSuperflat = settings.isSuperflat;
        return this;
    }

    public static Rand allocate(final int seed, final Settings settings)
    {
        return allocator.allocate().init(seed, settings);
    }

    public static Rand allocate(final Settings settings)
    {
        return allocator.allocate().init(settings);
    }

    private void clear()
    {
        clearHashTable();
        clearBiomeFactorsHashTable();
        clearBiomeNameMap();
        clearRockChunkHashTable();
        clearLavaNodeHashTable();
        clearCaveChunkHashTable();
        clearInCaveChunkHashTable();
        clearPlantChunkHashTable();
        clearPlantBlockKindChunkHashTable();
    }

    public void free()
    {
        clear();
        allocator.free(this);
    }

    private static boolean biomePasses(final String biome,
                                       final String expectedBiome)
    {
        if(expectedBiome.equals(""))
            return true;
        return biome.equals(expectedBiome);
    }

    private static boolean isWaterBiome(final String biomeName)
    {
        for(Biome b : Biome.values)
        {
            if(b.getName().equals(biomeName))
            {
                return b.isWaterBiome();
            }
        }
        return false;
    }

    /** @param settings
     *            the land generator settings
     * @return new land generator */
    public static Rand create(final Settings settings)
    {
        Settings s = settings;
        if(s == null)
            s = Settings.allocate();
        else
            s = s.dup();
        boolean isWaterBiome = isWaterBiome(s.startingBiome);
        Rand retval = null;
        int rockHeight;
        do
        {
            if(retval != null)
                retval.free();
            retval = allocate(s);
            rockHeight = retval.getRockHeight(0, 0);
        }
        while((rockHeight < WaterHeight && !isWaterBiome)
                || !biomePasses(retval.getBiomeName(0, 0), s.startingBiome)
                || retval.isInCave(0, rockHeight, 0)
                || retval.getPlant(0, 0, true) != null);
        s.free();
        return retval;
    }

    /** @param seed
     *            seed for land generator
     * @param settings
     *            the land generator settings
     * @return new land generator */
    public static Rand create(final int seed, final Settings settings)
    {
        Settings s = settings;
        if(s == null)
            s = Settings.allocate();
        else
            s = s.dup();
        Rand retval = allocate(seed, s);
        s.free();
        return retval;
    }

    /** @return this land generator's seed */
    public int getSeed()
    {
        return this.seed;
    }

    private static final int hashPrime = 99991;

    private static enum RandClass
    {
        RockHeight,
        LakeBedType,
        Plant,
        Lava,
        OreType,
        Cave,
        CaveDecoration,
        CaveDecorationChest,
        BiomeTemperature,
        BiomeRainfall,
        BiomeHeight,
        Vect,
        Vect2,
        Vect3,
        Vect4,
        Vect5,
        Vect6,
        Slime,
        Mob
    }

    private static class Node
    {
        public int x, y, z, rc;
        public float value;

        public Node()
        {
        }
    }

    private final Node[] hashTable = new Node[hashPrime];
    private static final Allocator<Node> nodeAllocator = new Allocator<Node>()
    {
        @Override
        protected Node allocateInternal()
        {
            return new Node();
        }
    };

    private void clearHashTable()
    {
        for(int i = 0; i < this.hashTable.length; i++)
        {
            nodeAllocator.free(this.hashTable[i]);
            this.hashTable[i] = null;
        }
    }

    private int genHash(final int x, final int y, final int z, final int rc)
    {
        long retval = x + 9L * (y + 9L * (z + 9L * rc));
        retval %= hashPrime;
        if(retval < 0)
            retval += hashPrime;
        return (int)retval;
    }

    private final Object genRandSyncObj = new Object();

    private float genRand(final int x, final int y, final int z, final int w)
    {
        synchronized(this.genRandSyncObj)
        {
            final long mask = (1L << 48) - 1;
            final long multiplier = 0x5DEECE66DL;
            int hash = genHash(x, y, z, w);
            if(this.hashTable[hash] != null && this.hashTable[hash].x == x
                    && this.hashTable[hash].y == y
                    && this.hashTable[hash].z == z
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
            for(int i = 0; i < 5; i++)
            {
                randv *= multiplier;
                randv += 0xB;
                randv &= mask;
            }
            float retval = randv * (1.0f / (mask + 1));
            if(this.hashTable[hash] == null)
                this.hashTable[hash] = nodeAllocator.allocate();
            this.hashTable[hash].x = x;
            this.hashTable[hash].y = y;
            this.hashTable[hash].z = z;
            this.hashTable[hash].rc = w;
            this.hashTable[hash].value = retval;
            return retval;
        }
    }

    private float genRand(final int x,
                          final int y,
                          final int z,
                          final RandClass rc)
    {
        return genRand(x, y, z, rc.ordinal());
    }

    private Vector genRandV(final int x, final int y, final int z)
    {
        Vector retval = Vector.allocate();
        int w = RandClass.Vect.ordinal();
        do
        {
            retval.setX(genRand(x, y, z, w++) * 2.0f - 1.0f);
            retval.setY(genRand(x, y, z, w++) * 2.0f - 1.0f);
            retval.setZ(genRand(x, y, z, w++) * 2.0f - 1.0f);
        }
        while(retval.abs() > 1.0f || retval.abs() < 0.0001f);
        return retval;
    }

    @SuppressWarnings("unused")
    private float getNoise(final float x,
                           final float y,
                           final float z,
                           final RandClass rc)
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

    private float getFractalNoise(final float x_in,
                                  final float y_in,
                                  final float z_in,
                                  final RandClass rc,
                                  final int iterations,
                                  final float roughness)
    {
        float retval = 0;
        float factor = 1.0f;
        float x = x_in, y = y_in, z = z_in;
        for(int i = 0; i < iterations; i++)
        {
            int xmin = (int)Math.floor(x);
            int xmax = xmin + 1;
            int ymin = (int)Math.floor(y);
            int ymax = ymin + 1;
            int zmin = (int)Math.floor(z);
            int zmax = zmin + 1;
            float v000 = genRand(xmin, ymin + i * 1000, zmin, rc);
            float v100 = genRand(xmax, ymin + i * 1000, zmin, rc);
            float v010 = genRand(xmin, ymax + i * 1000, zmin, rc);
            float v110 = genRand(xmax, ymax + i * 1000, zmin, rc);
            float v001 = genRand(xmin, ymin + i * 1000, zmax, rc);
            float v101 = genRand(xmax, ymin + i * 1000, zmax, rc);
            float v011 = genRand(xmin, ymax + i * 1000, zmax, rc);
            float v111 = genRand(xmax, ymax + i * 1000, zmax, rc);
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
            retval += (v0 * nfx + v1 * fx) * factor;
            factor *= roughness;
            x *= 2;
            y *= 2;
            z *= 2;
        }
        return retval;
    }

    private final float biomeScale = 128f;

    private float getInternalBiomeTemperature(final int x, final int z)
    {
        return Math.max(Math.min(getFractalNoise(x / this.biomeScale,
                                                 0,
                                                 z / this.biomeScale,
                                                 RandClass.BiomeTemperature,
                                                 4,
                                                 0.2f),
                                 1.0f), 0.0f);
    }

    private float getInternalBiomeRainfall(final int x, final int z)
    {
        return Math.max(Math.min(getFractalNoise(x / this.biomeScale,
                                                 0,
                                                 z / this.biomeScale,
                                                 RandClass.BiomeRainfall,
                                                 4,
                                                 0.2f),
                                 1.0f), 0.0f);
    }

    private float getInternalBiomeHeight(final int x, final int z)
    {
        return Math.max(Math.min(getFractalNoise(x / this.biomeScale,
                                                 0,
                                                 z / this.biomeScale,
                                                 RandClass.BiomeHeight,
                                                 4,
                                                 0.2f),
                                 1.0f), 0.0f);
    }

    private static enum Biome
    {
        Ocean
        {
            @Override
            public float getRainfall()
            {
                return 0.5f;
            }

            @Override
            public float getTemperature()
            {
                return 0.5f;
            }

            @Override
            public float getHeight()
            {
                return -8f;
            }

            @Override
            public float getCorrespondence(final float rainfall,
                                           final float temperature,
                                           final float height)
            {
                return (1f - height) * 15f;
            }

            @Override
            public float getPlantProb(final PlantType t)
            {
                return 0f;
            }

            @Override
            public String getName()
            {
                return "Ocean";
            }

            @Override
            public float getRoughness()
            {
                return 0.2f;
            }

            @Override
            public float getHeightVariation()
            {
                return 0.05f;
            }

            @Override
            public float getHeightFrequency()
            {
                return 0.001f;
            }

            @Override
            public float getSnow()
            {
                return 0;
            }

            @Override
            public Block getSurfaceBlock()
            {
                return Block.NewSand();
            }

            @Override
            public Block getSubSurfaceBlock()
            {
                return Block.NewSand();
            }

            @Override
            public float getHeightExponent()
            {
                return 1;
            }

            @Override
            public boolean isWaterBiome()
            {
                return true;
            }

            @Override
            public float getMobProb(final MobType t)
            {
                if(t.getName().equals("Squid"))
                    return 0.001f;
                // TODO Auto-generated method stub
                return 0;
            }
        },
        ExtremeHills
        {
            @Override
            public float getRainfall()
            {
                return 0.5f;
            }

            @Override
            public float getTemperature()
            {
                return 0.5f;
            }

            @Override
            public float getHeight()
            {
                return 5f;
            }

            @Override
            public float getCorrespondence(final float rainfall,
                                           final float temperature,
                                           final float height)
            {
                float retval = height;
                retval *= retval;
                retval *= retval;
                retval *= retval;
                float v = 1.0f - temperature;
                retval *= 1.0f - v * v;
                return retval * 5.0f;
            }

            @Override
            public float getPlantProb(final PlantType t)
            {
                if(t == PlantType.OakTree)
                    return 0.002f;
                if(t == PlantType.Flower)
                    return 0.01f;
                return 0f;
            }

            @Override
            public String getName()
            {
                return "Extreme Hills";
            }

            @Override
            public float getRoughness()
            {
                return 0.4f;
            }

            @Override
            public float getHeightVariation()
            {
                return 0.3f;
            }

            @Override
            public float getHeightFrequency()
            {
                return 0.04f;
            }

            @Override
            public float getSnow()
            {
                return 0;
            }

            @Override
            public Block getSurfaceBlock()
            {
                return Block.NewGrass();
            }

            @Override
            public Block getSubSurfaceBlock()
            {
                return Block.NewDirt();
            }

            @Override
            public float getHeightExponent()
            {
                return 0.7f;
            }

            @Override
            public boolean isWaterBiome()
            {
                return false;
            }

            @Override
            public float getMobProb(final MobType t)
            {
                if(t.getName().equals("Sheep"))
                    return 0.001f;
                // TODO Auto-generated method stub
                return 0;
            }
        },
        Taiga
        {
            @Override
            public float getRainfall()
            {
                return 0.5f;
            }

            @Override
            public float getTemperature()
            {
                return 0.3f;
            }

            @Override
            public float getHeight()
            {
                return 5;
            }

            @Override
            public float getRoughness()
            {
                return 0.4f;
            }

            @Override
            public float getHeightVariation()
            {
                return 0.4f;
            }

            @Override
            public float getHeightFrequency()
            {
                return 0.01f;
            }

            @Override
            public float getCorrespondence(final float rainfall,
                                           final float temperature,
                                           final float height)
            {
                float retval = 1.0f - Math.abs(height - 0.5f);
                retval *= 1.0f - Math.abs(rainfall - getRainfall());
                retval *= 1.0f - Math.abs(temperature - getTemperature());
                return retval * 8.0f;
            }

            @Override
            public float getPlantProb(final PlantType t)
            {
                switch(t)
                {
                case BirchTree:
                    return 0.0f;
                case JungleTree:
                    return 0.0f;
                case OakTree:
                    return 0.0f;
                case SpruceTree:
                    return 0.05f;
                case Cactus:
                case DeadBush:
                case Grass:
                    return 0.0f;
                case Flower:
                    return 0.01f;
                }
                return 0.0f;
            }

            @Override
            public String getName()
            {
                return "Taiga";
            }

            @Override
            public float getSnow()
            {
                return 1;
            }

            @Override
            public Block getSurfaceBlock()
            {
                return Block.NewGrass();
            }

            @Override
            public Block getSubSurfaceBlock()
            {
                return Block.NewDirt();
            }

            @Override
            public float getHeightExponent()
            {
                return 1;
            }

            @Override
            public boolean isWaterBiome()
            {
                return false;
            }

            @Override
            public float getMobProb(final MobType t)
            {
                if(t.getName().equals("Sheep"))
                    return 0.001f;
                // TODO Auto-generated method stub
                return 0;
            }
        },
        Tundra
        {
            @Override
            public float getRainfall()
            {
                return 0;
            }

            @Override
            public float getTemperature()
            {
                return 0;
            }

            @Override
            public float getHeight()
            {
                return 0.75f;
            }

            @Override
            public float getRoughness()
            {
                return 0.4f;
            }

            @Override
            public float getHeightVariation()
            {
                return 0.05f;
            }

            @Override
            public float getHeightFrequency()
            {
                return 0.01f;
            }

            @Override
            public float getCorrespondence(final float rainfall,
                                           final float temperature,
                                           final float height)
            {
                float retval = 1.0f - Math.abs(height - getHeight());
                retval *= retval;
                retval *= 1.0f - temperature;
                return retval * 7.0f;
            }

            @Override
            public float getPlantProb(final PlantType t)
            {
                if(t == PlantType.OakTree)
                    return 0.005f;
                return 0;
            }

            @Override
            public String getName()
            {
                return "Tundra";
            }

            @Override
            public float getSnow()
            {
                return 1;
            }

            @Override
            public Block getSurfaceBlock()
            {
                return Block.NewGrass();
            }

            @Override
            public Block getSubSurfaceBlock()
            {
                return Block.NewDirt();
            }

            @Override
            public float getHeightExponent()
            {
                return 1;
            }

            @Override
            public boolean isWaterBiome()
            {
                return false;
            }

            @Override
            public float getMobProb(final MobType t)
            {
                // TODO Auto-generated method stub
                return 0;
            }
        },
        Forest
        {
            @Override
            public float getRainfall()
            {
                return 0.75f;
            }

            @Override
            public float getTemperature()
            {
                return 0.75f;
            }

            @Override
            public float getHeight()
            {
                return 5;
            }

            @Override
            public float getRoughness()
            {
                return 0.3f;
            }

            @Override
            public float getHeightVariation()
            {
                return 0.1f;
            }

            @Override
            public float getHeightFrequency()
            {
                return 0.05f;
            }

            @Override
            public float getCorrespondence(final float rainfall,
                                           final float temperature,
                                           final float height)
            {
                float retval = 1.0f - Math.abs(height - 0.4f);
                retval *= 1.0f - Math.abs(rainfall - getRainfall());
                retval *= 1.0f - Math.abs(temperature - getTemperature());
                return retval * 8.0f;
            }

            @Override
            public float getPlantProb(final PlantType t)
            {
                if(t == PlantType.OakTree)
                    return 1 / 20f;
                if(t == PlantType.BirchTree)
                    return 1 / 30f;
                if(t == PlantType.Flower)
                    return 1 / 50f;
                return 0;
            }

            @Override
            public String getName()
            {
                return "Forest";
            }

            @Override
            public float getSnow()
            {
                return 0;
            }

            @Override
            public Block getSurfaceBlock()
            {
                return Block.NewGrass();
            }

            @Override
            public Block getSubSurfaceBlock()
            {
                return Block.NewDirt();
            }

            @Override
            public float getHeightExponent()
            {
                return 1;
            }

            @Override
            public boolean isWaterBiome()
            {
                return false;
            }

            @Override
            public float getMobProb(final MobType t)
            {
                if(t.getName().equals("Sheep"))
                    return 0.002f;
                // TODO Auto-generated method stub
                return 0;
            }
        },
        Desert
        {
            @Override
            public float getRainfall()
            {
                return 0;
            }

            @Override
            public float getTemperature()
            {
                return 1;
            }

            @Override
            public float getHeight()
            {
                return 5;
            }

            @Override
            public float getRoughness()
            {
                return 0.2f;
            }

            @Override
            public float getHeightVariation()
            {
                return 0.2f;
            }

            @Override
            public float getHeightFrequency()
            {
                return 0.01f;
            }

            @Override
            public float getCorrespondence(final float rainfall,
                                           final float temperature,
                                           final float height)
            {
                float retval = 1.0f - Math.abs(height - 0.5f);
                retval *= 1.0f - Math.abs(rainfall - getRainfall());
                retval *= 1.0f - Math.abs(temperature - getTemperature());
                return retval * 12.0f;
            }

            @Override
            public float getPlantProb(final PlantType t)
            {
                if(t == PlantType.Cactus)
                    return 0.015f;
                else if(t == PlantType.DeadBush)
                    return 0.015f;
                return 0;
            }

            @Override
            public String getName()
            {
                return "Desert";
            }

            @Override
            public float getSnow()
            {
                return 0;
            }

            @Override
            public Block getSurfaceBlock()
            {
                return Block.NewSand();
            }

            @Override
            public Block getSubSurfaceBlock()
            {
                return Block.NewSand();
            }

            @Override
            public float getHeightExponent()
            {
                return 0.5f;
            }

            @Override
            public boolean isWaterBiome()
            {
                return false;
            }

            @Override
            public float getMobProb(final MobType t)
            {
                // TODO Auto-generated method stub
                return 0;
            }
        },
        Plains
        {
            @Override
            public float getRainfall()
            {
                return 0.33f;
            }

            @Override
            public float getTemperature()
            {
                return 0.66f;
            }

            @Override
            public float getHeight()
            {
                return 5;
            }

            @Override
            public float getRoughness()
            {
                return 0.3f;
            }

            @Override
            public float getHeightVariation()
            {
                return 0.05f;
            }

            @Override
            public float getHeightFrequency()
            {
                return 0.01f;
            }

            @Override
            public float getCorrespondence(final float rainfall,
                                           final float temperature,
                                           final float height)
            {
                float retval = 1.0f - Math.abs(height - 0.5f);
                retval *= 1.0f - Math.abs(rainfall - getRainfall());
                retval *= 1.0f - Math.abs(temperature - getTemperature());
                return retval * 8.0f;
            }

            @Override
            public float getPlantProb(final PlantType t)
            {
                if(t == PlantType.Flower)
                    return 1 / 25f;
                if(t == PlantType.Grass)
                    return 1 / 5f;
                return 0;
            }

            @Override
            public String getName()
            {
                return "Plains";
            }

            @Override
            public float getSnow()
            {
                return 0;
            }

            @Override
            public Block getSurfaceBlock()
            {
                return Block.NewGrass();
            }

            @Override
            public Block getSubSurfaceBlock()
            {
                return Block.NewDirt();
            }

            @Override
            public float getHeightExponent()
            {
                return 1;
            }

            @Override
            public boolean isWaterBiome()
            {
                return false;
            }

            @Override
            public float getMobProb(final MobType t)
            {
                if(t.getName().equals("Sheep"))
                    return 0.001f;
                // TODO Auto-generated method stub
                return 0;
            }
        },
        Jungle
        {
            @Override
            public float getRainfall()
            {
                return 1;
            }

            @Override
            public float getTemperature()
            {
                return 1;
            }

            @Override
            public float getHeight()
            {
                return 5;
            }

            @Override
            public float getRoughness()
            {
                return 0.3f;
            }

            @Override
            public float getHeightVariation()
            {
                return 0.05f;
            }

            @Override
            public float getHeightFrequency()
            {
                return 0.05f;
            }

            @Override
            public float getCorrespondence(final float rainfall,
                                           final float temperature,
                                           final float height)
            {
                float retval = 1.0f - Math.abs(height - 0.5f);
                retval *= 1.0f - Math.abs(rainfall - getRainfall());
                retval *= 1.0f - Math.abs(temperature - getTemperature());
                return retval * 8.0f;
            }

            @Override
            public float getPlantProb(final PlantType t)
            {
                if(t == PlantType.JungleTree)
                    return 1 / 20f;
                if(t == PlantType.OakTree)
                    return 1 / 60f;
                if(t == PlantType.Flower)
                    return 1 / 30f;
                return 0;
            }

            @Override
            public String getName()
            {
                return "Jungle";
            }

            @Override
            public float getSnow()
            {
                return 0;
            }

            @Override
            public Block getSurfaceBlock()
            {
                return Block.NewGrass();
            }

            @Override
            public Block getSubSurfaceBlock()
            {
                return Block.NewDirt();
            }

            @Override
            public float getHeightExponent()
            {
                return 1;
            }

            @Override
            public boolean isWaterBiome()
            {
                return false;
            }

            @Override
            public float getMobProb(final MobType t)
            {
                // TODO Auto-generated method stub
                return 0;
            }
        },
        ;
        public abstract float getRainfall();

        public abstract float getTemperature();

        public abstract float getHeight();

        public abstract float getRoughness();

        public abstract float getHeightVariation();

        public abstract float getHeightFrequency();

        public abstract float getCorrespondence(float rainfall,
                                                float temperature,
                                                float height);

        public abstract float getPlantProb(PlantType t);

        public abstract String getName();

        public static final Biome[] values = values();

        public abstract float getSnow();

        public abstract Block getSurfaceBlock();

        public abstract Block getSubSurfaceBlock();

        public abstract float getHeightExponent();

        public abstract boolean isWaterBiome();

        public abstract float getMobProb(MobType t);
    }

    private static final class BiomeFactorsChunk
    {
        public static final int size = 16; // must be power of 2
        private static final Allocator<BiomeFactorsChunk> allocator = new Allocator<Rand.BiomeFactorsChunk>()
        {
            @SuppressWarnings("synthetic-access")
            @Override
            protected BiomeFactorsChunk allocateInternal()
            {
                return new BiomeFactorsChunk();
            }
        };
        private static final Allocator<float[]> biomeFactorsAllocator = new Allocator<float[]>()
        {
            @Override
            protected float[] allocateInternal()
            {
                return new float[Biome.values.length];
            }
        };
        public final float[][] biomeFactors = new float[size * size][];
        public int cx, cz;

        private BiomeFactorsChunk()
        {
        }

        public static float[] allocateBiomeFactorsArray()
        {
            return biomeFactorsAllocator.allocate();
        }

        public static BiomeFactorsChunk allocate(final int cx, final int cz)
        {
            BiomeFactorsChunk retval = allocator.allocate();
            retval.cx = cx;
            retval.cz = cz;
            return retval;
        }

        public float[] get(final int x, final int z)
        {
            return this.biomeFactors[x + size * z];
        }

        public void set(final int x, final int z, final float[] v)
        {
            this.biomeFactors[x + size * z] = v;
        }

        public void free()
        {
            for(int i = 0; i < size * size; i++)
            {
                biomeFactorsAllocator.free(this.biomeFactors[i]);
                this.biomeFactors[i] = null;
            }
            allocator.free(this);
        }
    }

    private void initBiomeFactorsChunk(final BiomeFactorsChunk bfc)
    {
        for(int x = 0; x < BiomeFactorsChunk.size; x++)
        {
            for(int z = 0; z < BiomeFactorsChunk.size; z++)
            {
                final float[] biomeFactors = BiomeFactorsChunk.allocateBiomeFactorsArray();
                float rainfall = getInternalBiomeRainfall(bfc.cx + x, bfc.cz
                        + z);
                float temperature = getInternalBiomeTemperature(bfc.cx + x,
                                                                bfc.cz + z);
                float height = getInternalBiomeHeight(bfc.cx + x, bfc.cz + z);
                for(int i = 0; i < biomeFactors.length; i++)
                    biomeFactors[i] = Biome.values[i].getCorrespondence(rainfall,
                                                                        temperature,
                                                                        height);
                {
                    float sum = 0.0f;
                    for(int i = 0; i < biomeFactors.length; i++)
                        sum += biomeFactors[i];
                    for(int i = 0; i < biomeFactors.length; i++)
                        biomeFactors[i] /= sum;
                }
                for(int i = 0; i < biomeFactors.length; i++)
                {
                    biomeFactors[i] *= biomeFactors[i];
                    biomeFactors[i] *= biomeFactors[i];
                    biomeFactors[i] *= biomeFactors[i];
                    biomeFactors[i] *= biomeFactors[i];
                    biomeFactors[i] *= biomeFactors[i];
                }
                {
                    float sum = 0.0f;
                    for(int i = 0; i < biomeFactors.length; i++)
                        sum += biomeFactors[i];
                    for(int i = 0; i < biomeFactors.length; i++)
                        biomeFactors[i] /= sum;
                }
                bfc.set(x, z, biomeFactors);
            }
        }
    }

    private final BiomeFactorsChunk[] biomeFactorsHashTable = new BiomeFactorsChunk[hashPrime];

    private void clearBiomeFactorsHashTable()
    {
        for(int i = 0; i < hashPrime; i++)
        {
            if(this.biomeFactorsHashTable[i] != null)
                this.biomeFactorsHashTable[i].free();
            this.biomeFactorsHashTable[i] = null;
        }
    }

    private final Object[] biomeFactorsHashTableSync;
    {
        this.biomeFactorsHashTableSync = new Object[hashPrime];
        for(int i = 0; i < hashPrime; i++)
            this.biomeFactorsHashTableSync[i] = new Object();
    }

    private Object getBiomeFactorsSynchronizeObject(final int x, final int z)
    {
        int cx = x - (x & (BiomeFactorsChunk.size - 1));
        int cz = z - (z & (BiomeFactorsChunk.size - 1));
        int hash = getChunkHash(cx, cz);
        return this.biomeFactorsHashTableSync[hash];
    }

    // must synchronize over call and all accesses to the return value
    private float[] getBiomeFactors(final int x, final int z)
    {
        int cx = x - (x & (BiomeFactorsChunk.size - 1));
        int cz = z - (z & (BiomeFactorsChunk.size - 1));
        int hash = getChunkHash(cx, cz);
        synchronized(this.biomeFactorsHashTableSync[hash])
        {
            BiomeFactorsChunk bfc = this.biomeFactorsHashTable[hash];
            if(bfc == null || bfc.cx != cx || bfc.cz != cz)
            {
                bfc = BiomeFactorsChunk.allocate(cx, cz);
                initBiomeFactorsChunk(bfc);
                if(this.biomeFactorsHashTable[hash] != null)
                    this.biomeFactorsHashTable[hash].free();
                this.biomeFactorsHashTable[hash] = bfc;
            }
            float[] factors = bfc.get(x - cx, z - cz);
            return factors;
        }
    }

    /** @param x
     *            the x coordinate
     * @param z
     *            the z coordinate
     * @return the biome temperature */
    public float getBiomeTemperature(final int x, final int z)
    {
        float retval = 0.0f;
        synchronized(getBiomeFactorsSynchronizeObject(x, z))
        {
            float[] factors = getBiomeFactors(x, z);
            for(int i = 0; i < factors.length; i++)
            {
                retval += factors[i] * Biome.values[i].getTemperature();
            }
        }
        return Math.max(Math.min(retval, 1.0f), 0.0f);
    }

    private float getBiomeHeightExponent(final int x, final int z)
    {
        float retval = 0.0f;
        synchronized(getBiomeFactorsSynchronizeObject(x, z))
        {
            float[] factors = getBiomeFactors(x, z);
            for(int i = 0; i < factors.length; i++)
            {
                retval += factors[i] * Biome.values[i].getHeightExponent();
            }
        }
        return Math.max(Math.min(retval, 1.0f), 0.0f);
    }

    /** @param x
     *            the x coordinate
     * @param z
     *            the z coordinate
     * @return the amount of snow */
    private float getBiomeSnow(final int x, final int z)
    {
        float retval = 0.0f;
        synchronized(getBiomeFactorsSynchronizeObject(x, z))
        {
            float[] factors = getBiomeFactors(x, z);
            for(int i = 0; i < factors.length; i++)
            {
                retval += factors[i] * Biome.values[i].getSnow();
            }
        }
        return Math.max(Math.min(retval, 1.0f), 0.0f);
    }

    private static final class XZPosition implements Allocatable
    {
        public int x, z;
        private static final Allocator<XZPosition> allocator = new Allocator<Rand.XZPosition>()
        {
            @Override
            protected XZPosition allocateInternal()
            {
                return new XZPosition();
            }
        };

        XZPosition()
        {
        }

        public static XZPosition allocate(final int x, final int z)
        {
            XZPosition retval = allocator.allocate();
            retval.x = x;
            retval.z = z;
            return retval;
        }

        public static XZPosition allocate(final XZPosition rt)
        {
            return allocate(rt.x, rt.z);
        }

        @Override
        public void free()
        {
            allocator.free(this);
        }

        @Override
        public boolean equals(final Object obj)
        {
            if(obj == null || !(obj instanceof XZPosition))
                return false;
            XZPosition rt = (XZPosition)obj;
            return rt.x == this.x && rt.z == this.z;
        }

        @Override
        public int hashCode()
        {
            return this.x + 63485 * this.z;
        }

        @Override
        public Allocatable dup()
        {
            return allocate(this);
        }
    }

    private static final class StringWrapper implements Allocatable
    {
        private static final Allocator<StringWrapper> allocator = new Allocator<Rand.StringWrapper>()
        {
            @Override
            protected StringWrapper allocateInternal()
            {
                return new StringWrapper();
            }
        };
        public String value;

        public StringWrapper()
        {
        }

        @Override
        public void free()
        {
            this.value = null;
            allocator.free(this);
        }

        @Override
        public StringWrapper dup()
        {
            StringWrapper retval = allocator.allocate();
            retval.value = this.value;
            return retval;
        }

        public static StringWrapper allocate(final String value)
        {
            StringWrapper retval = allocator.allocate();
            retval.value = value;
            return retval;
        }
    }

    @SuppressWarnings("unchecked")
    private final AllocatorHashMap<XZPosition, StringWrapper> biomeNameMap = (AllocatorHashMap<XZPosition, StringWrapper>)AllocatorHashMap.allocate();

    private void clearBiomeNameMap()
    {
        this.biomeNameMap.clear();
    }

    private String internalGetBiomeName(final int x, final int z)
    {
        synchronized(getBiomeFactorsSynchronizeObject(x, z))
        {
            float[] factors = getBiomeFactors(x, z);
            String retval = Biome.values[0].getName();
            float v = factors[0];
            for(int i = 1; i < factors.length; i++)
            {
                if(factors[i] > v)
                {
                    retval = Biome.values[i].getName();
                    v = factors[i];
                }
            }
            return retval;
        }
    }

    private Block getBiomeSurfaceBlock(final int x, final int z)
    {
        synchronized(getBiomeFactorsSynchronizeObject(x, z))
        {
            float[] factors = getBiomeFactors(x, z);
            Biome retval = Biome.values[0];
            float v = factors[0];
            for(int i = 1; i < factors.length; i++)
            {
                if(factors[i] > v)
                {
                    retval = Biome.values[i];
                    v = factors[i];
                }
            }
            return retval.getSurfaceBlock();
        }
    }

    private Block getBiomeSubSurfaceBlock(final int x, final int z)
    {
        synchronized(getBiomeFactorsSynchronizeObject(x, z))
        {
            float[] factors = getBiomeFactors(x, z);
            Biome retval = Biome.values[0];
            float v = factors[0];
            for(int i = 1; i < factors.length; i++)
            {
                if(factors[i] > v)
                {
                    retval = Biome.values[i];
                    v = factors[i];
                }
            }
            return retval.getSubSurfaceBlock();
        }
    }

    /** @param x
     *            the x coordinate
     * @param z
     *            the z coordinate
     * @return the biome name */
    public String getBiomeName(final int x, final int z)
    {
        synchronized(this.biomeNameMap)
        {
            XZPosition p = XZPosition.allocate(x, z);
            StringWrapper sw = this.biomeNameMap.get(p);
            String retval = (sw == null ? null : sw.value);
            if(retval == null)
            {
                retval = internalGetBiomeName(x, z);
                sw = StringWrapper.allocate(retval);
                this.biomeNameMap.put(p, sw);
                sw.free();
            }
            p.free();
            return retval;
        }
    }

    /** @param x
     *            the x coordinate
     * @param z
     *            the z coordinate
     * @return the biome rain fall */
    public float getBiomeRainfall(final int x, final int z)
    {
        float retval = 0.0f;
        synchronized(getBiomeFactorsSynchronizeObject(x, z))
        {
            float[] factors = getBiomeFactors(x, z);
            for(int i = 0; i < factors.length; i++)
            {
                retval += factors[i] * Biome.values[i].getRainfall();
            }
        }
        return Math.max(Math.min(retval, 1.0f), 0.0f);
    }

    private float getBiomeMobProb(final int x, final int z, final MobType t)
    {
        float retval = 0.0f;
        synchronized(getBiomeFactorsSynchronizeObject(x, z))
        {
            float[] factors = getBiomeFactors(x, z);
            for(int i = 0; i < factors.length; i++)
            {
                retval += factors[i] * Biome.values[i].getMobProb(t);
            }
        }
        return Math.max(Math.min(retval, 1.0f), 0.0f);
    }

    private static float
        interpolate(final float t, final float a, final float b)
    {
        return a + t * (b - a);
    }

    public float getBiomeGrassColorR(final int x, final int z)
    {
        return interpolate(getBiomeTemperature(x, z),
                           129f / 255f,
                           interpolate(getBiomeRainfall(x, z),
                                       190f / 255f,
                                       20f / 255f));
    }

    public float getBiomeGrassColorG(final int x, final int z)
    {
        return interpolate(getBiomeTemperature(x, z),
                           180f / 255f,
                           interpolate(getBiomeRainfall(x, z),
                                       183f / 255f,
                                       220f / 255f));
    }

    public float getBiomeGrassColorB(final int x, final int z)
    {
        return interpolate(getBiomeTemperature(x, z),
                           149f / 255f,
                           interpolate(getBiomeRainfall(x, z),
                                       84f / 255f,
                                       0f / 255f));
    }

    public float getBiomeWaterColorR(final int x, final int z)
    {
        return interpolate(getBiomeTemperature(x, z),
                           0f / 255f,
                           interpolate(getBiomeRainfall(x, z),
                                       0f / 255f,
                                       0f / 255f));
    }

    public float getBiomeWaterColorG(final int x, final int z)
    {
        return interpolate(getBiomeTemperature(x, z),
                           4f / 255f,
                           interpolate(getBiomeRainfall(x, z),
                                       160f / 255f,
                                       228f / 255f));
    }

    public float getBiomeWaterColorB(final int x, final int z)
    {
        return interpolate(getBiomeTemperature(x, z),
                           253f / 255f,
                           interpolate(getBiomeRainfall(x, z),
                                       212f / 255f,
                                       194f / 255f));
    }

    public float getBiomeFoliageColorR(final int x, final int z)
    {
        return interpolate(getBiomeTemperature(x, z),
                           96f / 255f,
                           interpolate(getBiomeRainfall(x, z),
                                       174f / 255f,
                                       27f / 255f));
    }

    public float getBiomeFoliageColorG(final int x, final int z)
    {
        return interpolate(getBiomeTemperature(x, z),
                           161f / 255f,
                           interpolate(getBiomeRainfall(x, z),
                                       164f / 255f,
                                       191f / 255f));
    }

    public float getBiomeFoliageColorB(final int x, final int z)
    {
        return interpolate(getBiomeTemperature(x, z),
                           123f / 255f,
                           interpolate(getBiomeRainfall(x, z),
                                       42f / 255f,
                                       0f / 255f));
    }

    /** @param x
     *            the x coordinate
     * @param z
     *            the z coordinate
     * @return the biome height */
    private float getBiomeHeight(final int x, final int z)
    {
        float retval = 0.0f;
        synchronized(getBiomeFactorsSynchronizeObject(x, z))
        {
            float[] factors = getBiomeFactors(x, z);
            for(int i = 0; i < factors.length; i++)
            {
                retval += factors[i] * Biome.values[i].getHeight();
            }
        }
        return retval;
    }

    /** @param x
     *            the x coordinate
     * @param z
     *            the z coordinate
     * @return the biome roughness */
    private float getBiomeRoughness(final int x, final int z)
    {
        float retval = 0.0f;
        synchronized(getBiomeFactorsSynchronizeObject(x, z))
        {
            float[] factors = getBiomeFactors(x, z);
            for(int i = 0; i < factors.length; i++)
            {
                retval += factors[i] * Biome.values[i].getRoughness();
            }
        }
        return Math.max(Math.min(retval, 1.0f), 0.0f);
    }

    /** @param x
     *            the x coordinate
     * @param z
     *            the z coordinate
     * @return the biome height variation */
    private float getBiomeHeightVariation(final int x, final int z)
    {
        float retval = 0.0f;
        synchronized(getBiomeFactorsSynchronizeObject(x, z))
        {
            float[] factors = getBiomeFactors(x, z);
            for(int i = 0; i < factors.length; i++)
            {
                retval += factors[i] * Biome.values[i].getHeightVariation();
            }
        }
        return retval;
    }

    /** @param x
     *            the x coordinate
     * @param z
     *            the z coordinate
     * @return the biome height frequency */
    @SuppressWarnings("unused")
    private float getBiomeHeightFrequency(final int x, final int z)
    {
        float retval = 0.0f;
        synchronized(getBiomeFactorsSynchronizeObject(x, z))
        {
            float[] factors = getBiomeFactors(x, z);
            for(int i = 0; i < factors.length; i++)
            {
                retval += factors[i] * Biome.values[i].getHeightFrequency();
            }
        }
        return retval;
    }

    private float
        getRockHeightNoiseH(final float x, final float z, final int i)
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

    private final float[] getRockHeightNoise_factors = new float[Biome.values.length];

    private float getRockHeightNoise(final int x, final int z)
    {
        float retval = 0.0f;
        float max = 0.0f;
        float initRoughness = getBiomeRoughness(x, z);
        synchronized(this.getRockHeightNoise_factors)
        {
            float[] factors = this.getRockHeightNoise_factors;
            synchronized(getBiomeFactorsSynchronizeObject(x, z))
            {
                float[] f = getBiomeFactors(x, z);
                for(int i = 0; i < f.length; i++)
                    factors[i] = f[i];
            }
            for(int biomeIndex = 0; biomeIndex < factors.length; biomeIndex++)
            {
                float frequency = Biome.values[biomeIndex].getHeightFrequency();
                float amplitude = factors[biomeIndex];
                float roughness;
                roughness = initRoughness;
                roughness = 1.15f + 0.4f * roughness;
                for(int i = 0; amplitude >= 0.01f; i++)
                {
                    retval += getRockHeightNoiseH(x * frequency,
                                                  z * frequency,
                                                  i) * amplitude;
                    max += amplitude;
                    frequency *= roughness;
                    frequency = Math.min(frequency, 1.0f);
                    amplitude /= roughness;
                }
            }
        }
        retval /= max;
        retval -= 0.5f;
        retval *= 2;
        if(retval < 0)
            retval = -(float)Math.pow(-retval, getBiomeHeightExponent(x, z));
        else
            retval = (float)Math.pow(retval, getBiomeHeightExponent(x, z));
        retval += 0.5f;
        retval = Math.max(0, Math.min(1, retval));
        retval = retval * retval * (3.0f - 2.0f * retval);
        return retval;
    }

    private static final class RockChunk
    {
        public int cx, cz;
        public final static int size = 4;
        private final int y[] = new int[size * size];

        public RockChunk()
        {
        }

        public int getY(final int x, final int z)
        {
            return this.y[x + size * z];
        }

        public void setY(final int x, final int z, final int newY)
        {
            this.y[x + size * z] = newY;
        }
    }

    private static final Allocator<RockChunk> rockChunkAllocator = new Allocator<RockChunk>()
    {
        @Override
        protected RockChunk allocateInternal()
        {
            return new RockChunk();
        }
    };
    private final RockChunk[] rockChunkHashTable = new RockChunk[hashPrime];

    private void clearRockChunkHashTable()
    {
        for(int i = 0; i < hashPrime; i++)
        {
            rockChunkAllocator.free(this.rockChunkHashTable[i]);
            this.rockChunkHashTable[i] = null;
        }
    }

    private int internalGetRockHeight(final int x, final int z)
    {
        final int maxY = Math.min(World.Height, World.Depth) - 1, minY = -World.Depth;
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
        retval = (retval - 0.5f) * getBiomeHeightVariation(x, z) + 0.5f;
        retval = retval * (maxY - minY) + minY;
        retval /= 4;
        retval += getBiomeHeight(x, z);
        retval = (float)Math.floor(retval + 0.5f);
        if(retval > maxY)
            retval = maxY;
        else if(retval < minY)
            retval = minY;
        if(this.isSuperflat)
            retval = (retval + 1) / 4 - 1;
        return (int)retval;
    }

    /** @param x
     *            x coordinate
     * @param z
     *            z coordinate
     * @return height of land */
    public int getRockHeight(final int x, final int z)
    {
        int cx = x - (x % RockChunk.size + RockChunk.size) % RockChunk.size;
        int cz = z - (z % RockChunk.size + RockChunk.size) % RockChunk.size;
        int hash = getChunkHash(cx, cz);
        synchronized(this.rockChunkHashTable)
        {
            RockChunk retval = this.rockChunkHashTable[hash];
            if(retval == null || retval.cx != cx || retval.cz != cz)
            {
                rockChunkAllocator.free(retval);
                retval = rockChunkAllocator.allocate();
                this.rockChunkHashTable[hash] = retval;
                retval.cx = cx;
                retval.cz = cz;
                for(int px = 0; px < RockChunk.size; px++)
                {
                    for(int pz = 0; pz < RockChunk.size; pz++)
                    {
                        retval.setY(px,
                                    pz,
                                    internalGetRockHeight(px + cx, pz + cz));
                    }
                }
            }
            return retval.getY(x - cx, z - cz);
        }
    }

    /***/
    public static final int WaterHeight = 0;

    private boolean waterInArea(final int x, final int y, final int z)
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

    private static final class LavaNode
    {
        public static final int size = 16;
        public static final int minLakeSize = 10;
        public static final int maxLakeSize = 20;
        public static final int maxHeight = 10 - World.Depth;
        public static final int minHeight = 1 - World.Depth;
        private int lakeSize[] = new int[size * size];
        public int cx, cz;

        public LavaNode()
        {
        }

        public void setLakeSize(final int x, final int z, final int v)
        {
            this.lakeSize[x + size * z] = v;
        }

        public int getLakeSize(final int x, final int z)
        {
            return this.lakeSize[x + size * z];
        }

        public LavaNode init(final int cx, final int cz)
        {
            this.cx = cx;
            this.cz = cz;
            return this;
        }
    }

    private static final Allocator<LavaNode> lavaNodeAllocator = new Allocator<Rand.LavaNode>()
    {
        @Override
        protected LavaNode allocateInternal()
        {
            return new LavaNode();
        }
    };

    private LavaNode makeLavaNode(final int cx, final int cz)
    {
        LavaNode retval = lavaNodeAllocator.allocate().init(cx, cz);
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

    private void clearLavaNodeHashTable()
    {
        for(int i = 0; i < hashPrime; i++)
        {
            lavaNodeAllocator.free(this.lavaNodeHashTable[i]);
            this.lavaNodeHashTable[i] = null;
        }
    }

    private synchronized LavaNode getLavaNode(final int cx, final int cz)
    {
        int hash = getChunkHash(cx, cz);
        LavaNode node = this.lavaNodeHashTable[hash];
        if(node == null || node.cx != cx || node.cz != cz)
        {
            lavaNodeAllocator.free(node);
            node = makeLavaNode(cx, cz);
            this.lavaNodeHashTable[hash] = node;
        }
        return node;
    }

    private int getLavaLakeSize(final int x, final int z)
    {
        int cx = x - (x % LavaNode.size + LavaNode.size) % LavaNode.size;
        int cz = z - (z % LavaNode.size + LavaNode.size) % LavaNode.size;
        return getLavaNode(cx, cz).getLakeSize(x - cx, z - cz);
    }

    private int getLavaLakeHeight(final int x, final int z)
    {
        float t = genRand(x, 2, z, RandClass.Lava);
        float v = LavaNode.minHeight + t
                * (LavaNode.maxHeight - LavaNode.minHeight);
        return (int)Math.floor(v);
    }

    private boolean isLava(final int x, final int y, final int z)
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

    @SuppressWarnings("unused")
    private boolean isOverLava(final int x, final int z)
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
        private static final Allocator<CaveChunk> allocator = new Allocator<Rand.CaveChunk>()
        {
            @Override
            protected CaveChunk allocateInternal()
            {
                return new CaveChunk();
            }
        };
        public static final int size = 4;
        public int cx, cz;
        private final CaveType caves[] = new CaveType[size * size];
        private final int y[] = new int[size * size];
        private final int r[] = new int[size * size];
        private final Vector dir[] = new Vector[size * size];

        public CaveType getCave(final int x, final int z)
        {
            return this.caves[x + size * z];
        }

        public int getY(final int x, final int z)
        {
            return this.y[x + size * z];
        }

        public int getR(final int x, final int z)
        {
            return this.r[x + size * z];
        }

        public Vector getDir(final int x, final int z)
        {
            return this.dir[x + size * z];
        }

        public void setCave(final int x, final int z, final CaveType c)
        {
            this.caves[x + size * z] = c;
        }

        public void setY(final int x, final int z, final int y)
        {
            this.y[x + size * z] = y;
        }

        public void setR(final int x, final int z, final int r)
        {
            this.r[x + size * z] = r;
        }

        public void setDir(final int x, final int z, final Vector dir)// hand
                                                                      // over
                                                                      // dir to
                                                                      // this
        {
            if(this.dir[x + size * z] != null)
                this.dir[x + size * z].free();
            this.dir[x + size * z] = dir;
        }

        public CaveChunk()
        {
        }

        public static CaveChunk allocate()
        {
            return allocator.allocate();
        }

        public void free()
        {
            for(int i = 0; i < this.dir.length; i++)
            {
                if(this.dir[i] != null)
                    this.dir[i].free();
                this.dir[i] = null;
            }
            allocator.free(this);
        }
    }

    private final CaveChunk caveChunkHashTable[] = new CaveChunk[hashPrime];

    private void clearCaveChunkHashTable()
    {
        for(int i = 0; i < hashPrime; i++)
        {
            if(this.caveChunkHashTable[i] != null)
                this.caveChunkHashTable[i].free();
            this.caveChunkHashTable[i] = null;
        }
    }

    private static final int caveMaxSize = 80;

    private void fillCaveChunk(final CaveChunk cc)
    {
        // final float caveProb = 2.0f;
        final float caveProb = 10.0f;
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

    synchronized CaveChunk getCaveChunk(final int cx, final int cz)
    {
        int hash = getChunkHash(cx, cz);
        CaveChunk node = this.caveChunkHashTable[hash];
        if(node != null && node.cx == cx && node.cz == cz)
        {
            return node;
        }
        if(node != null)
            node.free();
        node = CaveChunk.allocate();
        node.cx = cx;
        node.cz = cz;
        this.caveChunkHashTable[hash] = node;
        fillCaveChunk(node);
        return node;
    }

    private static class InCaveChunk
    {
        private static final Allocator<InCaveChunk> allocator = new Allocator<Rand.InCaveChunk>()
        {
            @Override
            protected InCaveChunk allocateInternal()
            {
                return new InCaveChunk();
            }
        };
        public static final int size = 4;
        private final boolean v[] = new boolean[size * size * size];
        public int cx, cy, cz;

        private void setInCave(final int x,
                               final int y,
                               final int z,
                               final boolean v)
        {
            this.v[x + size * (y + size * z)] = v;
        }

        public InCaveChunk()
        {
        }

        private InCaveChunk init(final int cx,
                                 final int cy,
                                 final int cz,
                                 final Rand rand)
        {
            this.cx = cx;
            this.cy = cy;
            this.cz = cz;
            for(int cdx = -caveMaxSize; cdx <= caveMaxSize + size; cdx++)
            {
                for(int cdz = -caveMaxSize; cdz <= caveMaxSize + size; cdz++)
                {
                    int px = cx + cdx, pz = cz + cdz;
                    CaveChunk cc = rand.getCaveChunk(px
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
                                    dir = Vector.allocate(dir);
                                    dir.setY(1 / 3f);
                                    dir = dir.normalizeAndSet();
                                    Vector p = dir.mulAndSet(0.95f * dir.dot(dx,
                                                                             dy,
                                                                             dz))
                                                  .subAndSet(dx, dy, dz);
                                    if(p.abs() < r / 16.0f)
                                        newValue = true;
                                    dir.free();
                                    break;
                                }
                                }
                                setInCave(x - cx, y - cy, z - cz, newValue);
                            }
                        }
                    }
                }
            }
            return this;
        }

        public static InCaveChunk allocate(final int cx,
                                           final int cy,
                                           final int cz,
                                           final Rand rand)
        {
            return allocator.allocate().init(cx, cy, cz, rand);
        }

        public boolean isInCave(final int x, final int y, final int z)
        {
            return this.v[x + size * (y + size * z)];
        }

        public void free()
        {
            for(int i = 0; i < this.v.length; i++)
                this.v[i] = false;
            allocator.free(this);
        }
    }

    private final InCaveChunk inCaveChunkHashTable[] = new InCaveChunk[hashPrime];

    private void clearInCaveChunkHashTable()
    {
        for(int i = 0; i < hashPrime; i++)
        {
            if(this.inCaveChunkHashTable[i] != null)
                this.inCaveChunkHashTable[i].free();
            this.inCaveChunkHashTable[i] = null;
        }
    }

    private synchronized InCaveChunk getInCaveChunk(final int cx,
                                                    final int cy,
                                                    final int cz)
    {
        int hash = genHash(cx, cy, cz, 0);
        InCaveChunk node = this.inCaveChunkHashTable[hash];
        if(node != null && node.cx == cx && node.cy == cy && node.cz == cz)
        {
            return node;
        }
        if(node != null)
            node.free();
        node = InCaveChunk.allocate(cx, cy, cz, this);
        this.inCaveChunkHashTable[hash] = node;
        return node;
    }

    private synchronized boolean
        isInCave(final int x, final int y, final int z)
    {
        if(y > getRockHeight(x, z))
            return false;
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

    private static class PlantChunk
    {
        private static final Allocator<PlantChunk> allocator = new Allocator<Rand.PlantChunk>()
        {
            @Override
            protected PlantChunk allocateInternal()
            {
                return new PlantChunk();
            }
        };
        public static final int size = 4;
        public int cx, cz;
        private Plant plant[] = new Plant[size * size];
        public PlantChunk next;

        public PlantChunk()
        {
        }

        private PlantChunk init(final int cx, final int cz)
        {
            this.cx = cx;
            this.cz = cz;
            for(int i = size * size - 1; i >= 0; i--)
                this.plant[i] = null;
            this.next = null;
            return this;
        }

        public static PlantChunk allocate(final int cx, final int cz)
        {
            return allocator.allocate().init(cx, cz);
        }

        public Plant get(final int x, final int z)
        {
            return this.plant[x + z * size];
        }

        public void set(final int x, final int z, final Plant plant)
        {
            int i = x + z * size;
            if(this.plant[i] != null)
                this.plant[i].free();
            this.plant[i] = plant;
        }

        public void free()
        {
            for(int i = size * size - 1; i >= 0; i--)
            {
                if(this.plant[i] != null)
                    this.plant[i].free();
                this.plant[i] = null;
            }
            this.next = null;
            allocator.free(this);
        }
    }

    private synchronized float[] getPlantCount(final int x,
                                               final int z,
                                               final float[] retval)
    {
        synchronized(getBiomeFactorsSynchronizeObject(x, z))
        {
            float[] biomeFactors = getBiomeFactors(x, z);
            for(int i = 0; i < retval.length; i++)
            {
                retval[i] = 0;
                PlantType pt = PlantType.values()[i];
                for(int bi = 0; bi < biomeFactors.length; bi++)
                {
                    retval[i] += biomeFactors[bi]
                            * Biome.values[bi].getPlantProb(pt);
                }
                retval[i] *= PlantChunk.size * PlantChunk.size;
            }
        }
        return retval;
    }

    private synchronized PlantChunk makePlantChunk(final int cx, final int cz)
    {
        float[] plantCount = new float[PlantType.values().length];
        PlantChunk pc = PlantChunk.allocate(cx, cz);
        getPlantCount(cx + PlantChunk.size / 2,
                      cz + PlantChunk.size / 2,
                      plantCount);
        int totalPlantCount = 0;
        Random rand = new Random(Float.valueOf(genRand(cx,
                                                       0,
                                                       cz,
                                                       RandClass.Plant))
                                      .hashCode());
        for(int i = 0; i < plantCount.length; i++)
        {
            plantCount[i] = (float)Math.floor(plantCount[i] + rand.nextFloat());
            totalPlantCount += (int)plantCount[i];
        }
        while(totalPlantCount > 0)
        {
            int x = rand.nextInt(PlantChunk.size);
            int z = rand.nextInt(PlantChunk.size);
            int RockHeight = getRockHeight(x + cx, z + cz);
            if(RockHeight >= WaterHeight
                    && !isInCave(x + cx, RockHeight, z + cz)
                    && !waterInArea(x + cx, WaterHeight - 1, z + cz))
            {
                float probFactor = 1.0f;
                final int searchDist = 5;
                for(int dx = -searchDist; dx <= searchDist; dx++)
                {
                    for(int dz = -searchDist; dz <= searchDist; dz++)
                    {
                        if(getPlant(x + cx + dx, z + cz + dz, false) != null)
                        {
                            probFactor *= 1.0f - 1.0f / (1.0f + dx * dx + dz
                                    * dz);
                        }
                    }
                }
                int plantKind = rand.nextInt(plantCount.length);
                if(plantCount[plantKind] > 0.5f
                        && rand.nextFloat() < probFactor)
                {
                    pc.set(x,
                           z,
                           Plant.PlantType.values()[plantKind].make(rand.nextFloat()));
                    plantCount[plantKind] -= 1.0f;
                    totalPlantCount--;
                }
            }
            else
            {
                float probFactor = 1.0f;
                final int searchDist = 5;
                for(int dx = -searchDist; dx <= searchDist; dx++)
                {
                    for(int dz = -searchDist; dz <= searchDist; dz++)
                    {
                        if(getPlant(x + cx + dx, z + cz + dz, false) != null)
                        {
                            probFactor *= 1.0f - 1.0f / (1.0f + dx * dx + dz
                                    * dz);
                        }
                    }
                }
                int plantKind = rand.nextInt(plantCount.length);
                if(plantCount[plantKind] > 0.5f
                        && rand.nextFloat() < probFactor)
                {
                    plantCount[plantKind] -= 1.0f;
                    totalPlantCount--;
                }
            }
        }
        return pc;
    }

    private static int getChunkHash(final int cx, final int cz)
    {
        int retval = (int)((cx + 3L * cz) % hashPrime);
        if(retval < 0)
            retval += hashPrime;
        return retval;
    }

    private final PlantChunk[] plantChunkHashTable = new PlantChunk[hashPrime];

    private void clearPlantChunkHashTable()
    {
        for(int i = 0; i < hashPrime; i++)
        {
            if(this.plantChunkHashTable[i] != null)
                this.plantChunkHashTable[i].free();
            this.plantChunkHashTable[i] = null;
        }
    }

    private final Object[] plantChunkHashTableSync;

    private Plant getPlant(final int x, final int z, final boolean make)
    {
        int cx = x - (x % PlantChunk.size + PlantChunk.size) % PlantChunk.size;
        int cz = z - (z % PlantChunk.size + PlantChunk.size) % PlantChunk.size;
        int hash = getChunkHash(cx, cz);
        synchronized(this.plantChunkHashTableSync[hash])
        {
            PlantChunk node = this.plantChunkHashTable[hash], parent = null;
            while(node != null)
            {
                if(node.cx == cx && node.cz == cz)
                {
                    if(parent != null)
                    {
                        parent.next = node.next;
                        node.next = this.plantChunkHashTable[hash];
                        this.plantChunkHashTable[hash] = node;
                    }
                    return node.get(x - cx, z - cz);
                }
                parent = node;
                node = node.next;
            }
            if(!make)
                return null;
            node = makePlantChunk(cx, cz);
            node.next = this.plantChunkHashTable[hash];
            this.plantChunkHashTable[hash] = node;
            return node.get(x - cx, z - cz);
        }
    }

    @SuppressWarnings("unused")
    private synchronized Block internalGetPlantBlockKind(final int x,
                                                         final int y,
                                                         final int z)
    {
        final int searchDist = Plant.maxXZExtent;
        Block retval = null;
        for(int dx = -searchDist; dx <= searchDist; dx++)
        {
            for(int dz = -searchDist; dz <= searchDist; dz++)
            {
                int cx = dx + x, cz = dz + z;
                Plant plant = getPlant(cx, cz, true);
                if(plant != null)
                {
                    int rockHeight = getRockHeight(cx, cz);
                    Block b = plant.getBlock(-dx, y - rockHeight, -dz);
                    if(b == null)
                        continue;
                    if(retval == null)
                        retval = b;
                    else if(retval.getReplaceability(b.getType()) == BlockType.Replaceability.Replace)
                        retval = b;
                    if(retval.getReplaceability(BlockType.BTWood) != BlockType.Replaceability.Replace)
                        return retval;
                }
            }
        }
        return retval;
    }

    private static class PlantBlockKindChunk
    {
        private static final Allocator<PlantBlockKindChunk> allocator = new Allocator<Rand.PlantBlockKindChunk>()
        {
            @Override
            protected PlantBlockKindChunk allocateInternal()
            {
                return new PlantBlockKindChunk();
            }
        };
        public static final int size = 4;
        public int cx, cy, cz;
        private final Block v[] = new Block[size * size * size];

        public PlantBlockKindChunk()
        {
        }

        private PlantBlockKindChunk init(final int cx,
                                         final int cy,
                                         final int cz)
        {
            this.cx = cx;
            this.cy = cy;
            this.cz = cz;
            return this;
        }

        public static PlantBlockKindChunk allocate(final int cx,
                                                   final int cy,
                                                   final int cz)
        {
            return allocator.allocate().init(cx, cy, cz);
        }

        public Block get(final int x_in, final int y_in, final int z_in)
        {
            int x = x_in - this.cx;
            int y = y_in - this.cy;
            int z = z_in - this.cz;
            Block retval = this.v[x + size * (y + size * z)];
            return retval;
        }

        public void put(final int x_in,
                        final int y_in,
                        final int z_in,
                        final Block v)
        {
            int x = x_in - this.cx;
            int y = y_in - this.cy;
            int z = z_in - this.cz;
            int i = x + size * (y + size * z);
            if(this.v[i] != null)
                this.v[i].free();
            this.v[i] = v;
        }

        public void free()
        {
            for(int i = 0; i < this.v.length; i++)
            {
                if(this.v[i] != null)
                    this.v[i].free();
                this.v[i] = null;
            }
            allocator.free(this);
        }
    }

    private final PlantBlockKindChunk plantBlockKindChunkHashTable[] = new PlantBlockKindChunk[hashPrime];

    private void clearPlantBlockKindChunkHashTable()
    {
        for(int i = 0; i < hashPrime; i++)
        {
            if(this.plantBlockKindChunkHashTable[i] != null)
                this.plantBlockKindChunkHashTable[i].free();
            this.plantBlockKindChunkHashTable[i] = null;
        }
    }

    private synchronized void
        fillPlantBlockKindChunk(final PlantBlockKindChunk c)
    {
        final int searchDist = Plant.maxXZExtent;
        for(int dx = -searchDist; dx <= searchDist + PlantBlockKindChunk.size; dx++)
        {
            for(int dz = -searchDist; dz <= searchDist
                    + PlantBlockKindChunk.size; dz++)
            {
                int cx = dx + c.cx, cz = dz + c.cz;
                Plant plant = getPlant(cx, cz, true);
                if(plant != null)
                {
                    int rockHeight = getRockHeight(cx, cz);
                    for(int x = c.cx; x < c.cx + PlantBlockKindChunk.size; x++)
                    {
                        for(int y = c.cy; y < c.cy + PlantBlockKindChunk.size; y++)
                        {
                            for(int z = c.cz; z < c.cz
                                    + PlantBlockKindChunk.size; z++)
                            {
                                Block retval = c.get(x, y, z);
                                Block b = plant.getBlock(x - cx, y - rockHeight
                                        - 1, z - cz);
                                if(b == null)
                                    continue;
                                if(retval == null)
                                    retval = b;
                                else if(retval.getReplaceability(b.getType()) == BlockType.Replaceability.Replace)
                                {
                                    retval = b;
                                }
                                else
                                {
                                    b.free();
                                    retval = retval.dup();
                                }
                                c.put(x, y, z, retval);
                            }
                        }
                    }
                }
            }
        }
    }

    private synchronized PlantBlockKindChunk
        getPlantBlockKindChunk(final int cx, final int cy, final int cz)
    {
        int hash = genHash(cx, cy, cz, 0);
        PlantBlockKindChunk node = this.plantBlockKindChunkHashTable[hash];
        if(node != null && node.cx == cx && node.cy == cy && node.cz == cz)
        {
            return node;
        }
        if(node != null)
            node.free();
        node = PlantBlockKindChunk.allocate(cx, cy, cz);
        fillPlantBlockKindChunk(node);
        this.plantBlockKindChunkHashTable[hash] = node;
        return node;
    }

    /** @param x
     *            the x coordinate
     * @param y
     *            the y coordinate
     * @param z
     *            the z coordinate
     * @return new block or null */
    private synchronized Block getTreeBlockKind(final int x,
                                                final int y,
                                                final int z)
    {
        int cx = x - (x % PlantBlockKindChunk.size + PlantBlockKindChunk.size)
                % PlantBlockKindChunk.size;
        int cy = y - (y % PlantBlockKindChunk.size + PlantBlockKindChunk.size)
                % PlantBlockKindChunk.size;
        int cz = z - (z % PlantBlockKindChunk.size + PlantBlockKindChunk.size)
                % PlantBlockKindChunk.size;
        PlantBlockKindChunk c = getPlantBlockKindChunk(cx, cy, cz);
        Block retval = c.get(x, y, z);
        if(retval != null)
            return Block.allocate(retval);
        return retval;
    }

    enum DecorationType
    {
        Torch, Chest, Cobweb, Mushroom;
        public static final int count = values().length;
    }

    /** @param x
     *            the x coordinate
     * @param y
     *            the y coordinate
     * @param z
     *            the z coordinate
     * @return new block */
    private Block getCaveDecoration(final int x, final int y, final int z)
    {
        final float genBlockProb = 0.1f;
        float ftype = genRand(x, y, z, RandClass.CaveDecoration)
                * DecorationType.count / genBlockProb;
        int type = (int)Math.floor(ftype);
        if(type >= DecorationType.count)
        {
            return Block.NewEmpty();
        }
        switch(DecorationType.values()[type])
        {
        case Mushroom:
        {
            if(isInCave(x, y - 1, z))
            {
                return Block.NewEmpty();
            }
            if(ftype - type > 0.5f)
                return Block.NewEmpty();
            if(ftype - type > 0.25f)
                return Block.NewBrownMushroom();
            return Block.NewRedMushroom();
        }
        case Cobweb:
        {
            int againstWallCount = 0;
            for(int o = 0; o < 6; o++)
            {
                int tx = x + Block.getOrientationDX(o), ty = y
                        + Block.getOrientationDY(o), tz = z
                        + Block.getOrientationDZ(o);
                if(!isInCave(tx, ty, tz) && getRockHeight(tx, tz) >= ty)
                    againstWallCount++;
            }
            if(againstWallCount >= 2)
                return Block.NewCobweb();
            return Block.NewEmpty();
        }
        case Torch:
        {
            if(isInCave(x, y - 1, z))
            {
                return Block.NewEmpty();
            }
            if(ftype - type > 0.5f)
                return Block.NewEmpty();
            return Block.NewTorch(4);
        }
        case Chest:
        {
            if(isInCave(x, y - 1, z))
                return Block.NewEmpty();
            if(ftype - type > 0.25f)
                return Block.NewEmpty();
            Block retval = Block.NewChest();
            int i = 0;
            final int caveChestBlocksSize = BlockType.getAllCaveChestBlocksSize(y);
            for(int caveChestBlocksIndex = 0; caveChestBlocksIndex < caveChestBlocksSize; caveChestBlocksIndex++)
            {
                BlockType.AddedBlockDescriptor d = BlockType.getAllCaveChestBlocksItem(y,
                                                                                       caveChestBlocksIndex);
                int count = (int)Math.floor(genRand(x,
                                                    i++,
                                                    z,
                                                    RandClass.CaveDecorationChest)
                        * (d.maxCount + 1));
                for(int row = Block.CHEST_ROWS - 1; row >= 0; row--)
                {
                    if(count <= 0)
                        break;
                    for(int column = 0; column < Block.CHEST_COLUMNS; column++)
                    {
                        if(count <= 0)
                            break;
                        count -= retval.chestAddBlocks(d.b, count, row, column);
                    }
                }
            }
            return retval;
        }
        default:
            return Block.NewEmpty();
        }
    }

    /** generates a chunk
     * 
     * @param cx
     *            chunk x coordinate
     * @param cy
     *            chunk y coordinate
     * @param cz
     *            chunk z coordinate
     * @param chunkSize
     *            chunk size
     * @return the new generated chunk */
    public GeneratedChunk genChunk(final int cx,
                                   final int cy,
                                   final int cz,
                                   final int chunkSize)
    {
        GeneratedChunk generatedChunk = GeneratedChunk.allocate(chunkSize,
                                                                cx,
                                                                cy,
                                                                cz);
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
                        Block tb = getTreeBlockKind(x, y, z);
                        if(tb == null)
                            block = getCaveDecoration(x, y, z);
                        else
                            block = tb;
                    }
                    else if(isLava(x, y, z))
                        block = Block.NewStationaryLava();
                    // else if(isOverLava(x, z) && y <= 5 + LavaNode.maxHeight)
                    // block = Block.NewEmpty();
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
                        block = getBiomeSubSurfaceBlock(x, z);
                    else if(y == rockHeight)
                        block = getBiomeSurfaceBlock(x, z);
                    else if(y <= WaterHeight)
                    {
                        MobType squid = null;
                        for(int i = 0; i < Mobs.getMobCount(); i++)
                        {
                            if(Mobs.getMob(i).getName().equals("Squid"))
                            {
                                squid = Mobs.getMob(i);
                                break;
                            }
                        }
                        if(squid != null)
                        {
                            if(getBiomeMobProb(x, z, squid)
                                    + genRand(x, 1000 + y, z, RandClass.Mob) >= 1)
                            {
                                generatedChunk.addEntity(Entity.NewMob(x,
                                                                       y,
                                                                       z,
                                                                       squid));
                            }
                        }
                        block = Block.NewStationaryWater();
                    }
                    else
                    {
                        Block tb = getTreeBlockKind(x, y, z);
                        if(tb == null)
                            tb = Block.NewEmpty();
                        if(tb.getType() == BlockType.BTEmpty)
                        {
                            Block tb2 = getTreeBlockKind(x, y - 1, z);
                            if(tb2 != null
                                    && tb2.getType() == BlockType.BTEmpty)
                            {
                                tb2.free();
                                tb2 = null;
                            }
                            if(tb2 != null
                                    || (y == rockHeight + 1 && !isInCave(x,
                                                                         y - 1,
                                                                         z)))
                            {
                                if(getBiomeSnow(x, z) > 0.5f)
                                {
                                    if(tb != null)
                                        tb.free();
                                    tb = Block.NewSnow(1);
                                }
                                for(int mobIndex = 0; mobIndex < Mobs.getMobCount(); mobIndex++)
                                {
                                    if(getBiomeMobProb(x,
                                                       z,
                                                       Mobs.getMob(mobIndex))
                                            + genRand(x,
                                                      mobIndex,
                                                      z,
                                                      RandClass.Mob) >= 1)
                                    {
                                        generatedChunk.addEntity(Entity.NewMob(x,
                                                                               y,
                                                                               z,
                                                                               Mobs.getMob(mobIndex)));
                                    }
                                }
                            }
                            if(tb2 != null)
                                tb2.free();
                        }
                        block = tb;
                    }
                    generatedChunk.setBlock(x, y, z, block);
                }
            }
        }
        return generatedChunk;
    }

    public boolean canSlimeGenerate(final int x, final int z)
    {
        return genRand(x & ~0xF, 0, z & ~0xF, RandClass.Slime) < 0.1f;
    }
}
