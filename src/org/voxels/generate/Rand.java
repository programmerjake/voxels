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

import java.io.*;
import java.util.*;

import org.voxels.*;
import org.voxels.Vector;
import org.voxels.generate.Tree.TreeType;

/** Land generator
 * 
 * @author jacob */
public final class Rand
{
    /** @author jacob */
    public static final class Settings
    {
        /**
         * 
         */
        public boolean isSuperflat;
        private static final int version = 0;

        /**
         * 
         */
        public Settings()
        {
            this.isSuperflat = false;
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
        }

        private void
            internalReadVer0(final DataInput i, final int curVersion) throws IOException
        {
            if(curVersion != 0)
                throw new IOException("invalid Rand.Settings version");
            this.isSuperflat = i.readBoolean();
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
            Settings retval = new Settings();
            int curVersion = i.readInt();
            retval.internalReadVer0(i, curVersion);
            return retval;
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
            this.settings = new Settings();
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

        private static Matrix drawBackground_t1 = new Matrix();
        private static Matrix drawBackground_t2 = new Matrix();

        @Override
        protected void drawBackground(final Matrix tform)
        {
            super.drawBackground(tform);
            String str = "New World Settings";
            float xScale = 2f / 40f;
            Text.draw(Matrix.setToScale(drawBackground_t1,
                                        xScale,
                                        2f / 40f,
                                        1.0f)
                            .concatAndSet(Matrix.setToTranslate(drawBackground_t2,
                                                                -xScale
                                                                        / 2f
                                                                        * Text.sizeW(str)
                                                                        / Text.sizeW("A"),
                                                                0.7f,
                                                                0))
                            .concatAndSet(tform),
                      Color.RGB(0, 0, 0),
                      str);
        }
    }

    private final int seed;
    /**
     * 
     */
    public final boolean isSuperflat;

    private Rand(final int seed, final Settings settings)
    {
        this.seed = seed;
        this.isSuperflat = settings.isSuperflat;
        this.treeChunkHashTableSync = new Object[hashPrime];
        for(int i = 0; i < hashPrime; i++)
            this.treeChunkHashTableSync[i] = new Object();
    }

    private Rand(final Settings settings)
    {
        this.seed = new Random().nextInt();
        this.isSuperflat = settings.isSuperflat;
        this.treeChunkHashTableSync = new Object[hashPrime];
        for(int i = 0; i < hashPrime; i++)
            this.treeChunkHashTableSync[i] = new Object();
    }

    /** @param settings
     *            the land generator settings
     * @return new land generator */
    public static Rand create(final Settings settings)
    {
        Settings s = settings;
        if(s == null)
            s = new Settings();
        Rand retval = null;
        int rockHeight;
        do
        {
            retval = new Rand(s);
            rockHeight = retval.getRockHeight(0, 0);
        }
        while(rockHeight < WaterHeight || retval.isInCave(0, rockHeight, 0)
                || retval.getTree(0, 0, true) != null);
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
            s = new Settings();
        return new Rand(seed, s);
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
        Tree,
        Lava,
        OreType,
        Cave,
        CaveDecoration,
        CaveDecorationChest,
        BiomeTemperature,
        BiomeRainfall,
        BiomeHeight,
        Vect/*
             * Vect
             * must
             * be
             * last
             */
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

    private int genHash(final int x, final int y, final int z, final int rc)
    {
        long retval = x + 9L * (y + 9L * (z + 9L * rc));
        retval %= hashPrime;
        if(retval < 0)
            retval += hashPrime;
        return (int)retval;
    }

    private Object genRandSyncObj = new Object();

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
                this.hashTable[hash] = new Node();
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
            public float getTreeProb(final TreeType tt)
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
            public float getTreeProb(final TreeType tt)
            {
                if(tt == TreeType.Oak)
                    return 0.002f;
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
                return 0.7f;
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
            public float getTreeProb(final TreeType tt)
            {
                switch(tt)
                {
                case Birch:
                    return 0.0f;
                case Jungle:
                    return 0.0f;
                case Oak:
                    return 0.0f;
                case Spruce:
                    return 0.05f;
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
            public float getTreeProb(final TreeType tt)
            {
                if(tt == TreeType.Oak)
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
            public float getTreeProb(final TreeType tt)
            {
                if(tt == TreeType.Oak)
                    return 1 / 20f;
                if(tt == TreeType.Birch)
                    return 1 / 30f;
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
                return retval * 8.0f;
            }

            @Override
            public float getTreeProb(final TreeType tt)
            {
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
            public float getTreeProb(final TreeType tt)
            {
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
            public float getTreeProb(final TreeType tt)
            {
                if(tt == TreeType.Jungle)
                    return 1 / 20f;
                if(tt == TreeType.Oak)
                    return 1 / 60f;
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

        public abstract float getTreeProb(Tree.TreeType tt);

        public abstract String getName();

        public static final Biome[] values = values();

        public abstract float getSnow();

        public abstract Block getSurfaceBlock();

        public abstract Block getSubSurfaceBlock();
    }

    private static final class BiomeFactorsChunk
    {
        public static final int size = 16; // must be power of 2
        public final float[][] biomeFactors = new float[size * size][];
        public final int cx, cz;

        public BiomeFactorsChunk(final int cx, final int cz)
        {
            this.cx = cx;
            this.cz = cz;
            for(int i = 0; i < size * size; i++)
            {
                this.biomeFactors[i] = null;
            }
        }

        public float[] get(final int x, final int z)
        {
            return this.biomeFactors[x + size * z];
        }

        public void set(final int x, final int z, final float[] v)
        {
            this.biomeFactors[x + size * z] = v;
        }
    }

    private void initBiomeFactorsChunk(final BiomeFactorsChunk bfc)
    {
        Integer[] indirArray = new Integer[Biome.values.length];
        for(int x = 0; x < BiomeFactorsChunk.size; x++)
        {
            for(int z = 0; z < BiomeFactorsChunk.size; z++)
            {
                final float[] biomeFactors = new float[Biome.values.length];
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
                for(int i = 0; i < biomeFactors.length; i++)
                {
                    indirArray[i] = Integer.valueOf(i);
                }
                Arrays.sort(indirArray, new Comparator<Integer>()
                {
                    @Override
                    public int compare(final Integer o1, final Integer o2) // sort
                                                                           // in
                    // descending
                    // order
                    {
                        int v1 = o1.intValue(), v2 = o2.intValue();
                        float f1 = biomeFactors[v1], f2 = biomeFactors[v2];
                        if(f1 < f2)
                            return 1;
                        if(f1 == f2)
                            return 0;
                        return -1;
                    }
                });
                final int keepCount = 20000;
                for(int i = keepCount; i < indirArray.length; i++)
                {
                    biomeFactors[indirArray[i].intValue()] = 0.0f;
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
                bfc = new BiomeFactorsChunk(cx, cz);
                initBiomeFactorsChunk(bfc);
                this.biomeFactorsHashTable[hash] = bfc; // TODO fix
            }
            return bfc.get(x - cx, z - cz);
        }
    }

    /** @param x
     *            the x coordinate
     * @param z
     *            the z coordinate
     * @return the biome temperature */
    @SuppressWarnings("unused")
    private float getBiomeTemperature(final int x, final int z)
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

    private static final class XZPosition
    {
        public int x, z;

        public XZPosition(final int x, final int z)
        {
            this.x = x;
            this.z = z;
        }

        public XZPosition(final XZPosition rt)
        {
            this.x = rt.x;
            this.z = rt.z;
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
    }

    private Map<XZPosition, String> biomeNameMap = new HashMap<XZPosition, String>();

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

    private XZPosition getBiomeName_t1 = new XZPosition(0, 0);

    /** @param x
     *            the x coordinate
     * @param z
     *            the z coordinate
     * @return the biome name */
    public String getBiomeName(final int x, final int z)
    {
        synchronized(this.biomeNameMap)
        {
            XZPosition p = this.getBiomeName_t1;
            p.x = x;
            p.z = z;
            String retval = this.biomeNameMap.get(p);
            if(retval == null)
            {
                retval = internalGetBiomeName(x, z);
                this.biomeNameMap.put(new XZPosition(p), retval);
            }
            return retval;
        }
    }

    /** @param x
     *            the x coordinate
     * @param z
     *            the z coordinate
     * @return the biome rain fall */
    @SuppressWarnings("unused")
    private float getBiomeRainfall(final int x, final int z)
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

    private float getRockHeightNoise(final int x, final int z)
    {
        float retval = 0.0f;
        float frequency = getBiomeHeightFrequency(x, z);
        float amplitude = 1.0f;
        float max = 0.0f;
        float roughness;
        roughness = getBiomeRoughness(x, z);
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
        retval = Math.max(0, Math.min(1, retval));
        retval = retval * retval * (3.0f - 2.0f * retval);
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

        public int getY(final int x, final int z)
        {
            return this.y[x + size * z];
        }

        public void setY(final int x, final int z, final int newY)
        {
            this.y[x + size * z] = newY;
        }
    }

    private RockChunk[] rockChunkHashTable = new RockChunk[hashPrime];

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
                retval = new RockChunk();
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

    private class LavaNode
    {
        public static final int size = 16;
        public static final int minLakeSize = 10;
        public static final int maxLakeSize = 20;
        public static final int maxHeight = 10 - World.Depth;
        public static final int minHeight = 1 - World.Depth;
        private int lakeSize[] = new int[size * size];
        public int cx, cz;

        public void setLakeSize(final int x, final int z, final int v)
        {
            this.lakeSize[x + size * z] = v;
        }

        public int getLakeSize(final int x, final int z)
        {
            return this.lakeSize[x + size * z];
        }

        public LavaNode(final int cx, final int cz)
        {
            this.cx = cx;
            this.cz = cz;
        }
    }

    private LavaNode makeLavaNode(final int cx, final int cz)
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

    private synchronized LavaNode getLavaNode(final int cx, final int cz)
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
        public static final int size = 4;
        public int cx, cz;
        private CaveType caves[] = new CaveType[size * size];
        private int y[] = new int[size * size];
        private int r[] = new int[size * size];
        private Vector dir[] = new Vector[size * size];

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

        public void setDir(final int x, final int z, final Vector dir)
        {
            this.dir[x + size * z] = Vector.allocate(dir);
        }

        public CaveChunk()
        {
        }
    }

    private CaveChunk caveChunkHashTable[] = new CaveChunk[hashPrime];
    private static final int caveMaxSize = 80;

    void fillCaveChunk(final CaveChunk cc)
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
        node = new CaveChunk();
        node.cx = cx;
        node.cz = cz;
        this.caveChunkHashTable[hash] = node;
        fillCaveChunk(node);
        return node;
    }

    private static class InCaveChunk
    {
        public static final int size = 4;
        private boolean v[] = new boolean[size * size * size];
        public int cx, cy, cz;

        private void setInCave(final int x,
                               final int y,
                               final int z,
                               final boolean v)
        {
            this.v[x + size * (y + size * z)] = v;
        }

        private static Vector InCaveChunk_t1 = Vector.allocate();

        public InCaveChunk(final int cx,
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
                                    dir = InCaveChunk_t1.set(dir);
                                    dir.setY(1 / 3f);
                                    dir = dir.normalizeAndSet();
                                    Vector p = dir.mulAndSet(0.95f * dir.dot(dx,
                                                                             dy,
                                                                             dz))
                                                  .subAndSet(dx, dy, dz);
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

        public boolean isInCave(final int x, final int y, final int z)
        {
            return this.v[x + size * (y + size * z)];
        }
    }

    private InCaveChunk inCaveChunkHashTable[] = new InCaveChunk[hashPrime];

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
        node = new InCaveChunk(cx, cy, cz, this);
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

    private static class TreeChunk
    {
        public static final int size = 4;
        public int cx, cz;
        private Tree tree[] = new Tree[size * size];
        public TreeChunk next;

        public TreeChunk(final int cx, final int cz)
        {
            this.cx = cx;
            this.cz = cz;
            for(int i = size * size - 1; i >= 0; i--)
                this.tree[i] = null;
            this.next = null;
        }

        public Tree getTree(final int x, final int z)
        {
            return this.tree[x + z * size];
        }

        public void setTree(final int x, final int z, final Tree v)
        {
            this.tree[x + z * size] = v;
        }
    }

    private synchronized float[] getTreeCount(final int x,
                                              final int z,
                                              final float[] retval)
    {
        synchronized(getBiomeFactorsSynchronizeObject(x, z))
        {
            float[] biomeFactors = getBiomeFactors(x, z);
            for(int i = 0; i < retval.length; i++)
            {
                retval[i] = 0;
                TreeType tt = TreeType.values()[i];
                for(int bi = 0; bi < biomeFactors.length; bi++)
                {
                    retval[i] += biomeFactors[bi]
                            * Biome.values[bi].getTreeProb(tt);
                }
                retval[i] *= TreeChunk.size * TreeChunk.size;
            }
        }
        return retval;
    }

    private synchronized TreeChunk makeTreeChunk(final int cx, final int cz)
    {
        float[] treeCount = new float[Tree.TreeType.values().length];
        TreeChunk tc = new TreeChunk(cx, cz);
        for(int x = 0; x < TreeChunk.size; x++)
        {
            for(int z = 0; z < TreeChunk.size; z++)
            {
                tc.setTree(x, z, null);
            }
        }
        getTreeCount(cx + TreeChunk.size / 2,
                     cz + TreeChunk.size / 2,
                     treeCount);
        int totalTreeCount = 0;
        Random rand = new Random(Float.valueOf(genRand(cx,
                                                       0,
                                                       cz,
                                                       RandClass.Tree))
                                      .hashCode());
        for(int i = 0; i < treeCount.length; i++)
        {
            treeCount[i] = (float)Math.floor(treeCount[i] + rand.nextFloat());
            totalTreeCount += (int)treeCount[i];
        }
        while(totalTreeCount > 0)
        {
            int x = rand.nextInt(TreeChunk.size);
            int z = rand.nextInt(TreeChunk.size);
            int RockHeight = getRockHeight(x + cx, z + cz);
            if(RockHeight >= WaterHeight
                    && !isInCave(x + cx, RockHeight, z + cz)
                    && !waterInArea(x, WaterHeight - 1, z))
            {
                float probFactor = 1.0f;
                final int searchDist = 5;
                for(int dx = -searchDist; dx <= searchDist; dx++)
                {
                    for(int dz = -searchDist; dz <= searchDist; dz++)
                    {
                        if(getTree(x + cx + dx, z + cz + dz, false) != null)
                        {
                            probFactor *= 1.0f - 1.0f / (1.0f + dx * dx + dz
                                    * dz);
                        }
                    }
                }
                int treeKind = rand.nextInt(treeCount.length);
                if(treeCount[treeKind] > 0.5f && rand.nextFloat() < probFactor)
                {
                    tc.setTree(x, z, new Tree(Tree.TreeType.values()[treeKind],
                                              rand.nextFloat()));
                    treeCount[treeKind] -= 1.0f;
                    totalTreeCount--;
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
                        if(getTree(x + cx + dx, z + cz + dz, false) != null)
                        {
                            probFactor *= 1.0f - 1.0f / (1.0f + dx * dx + dz
                                    * dz);
                        }
                    }
                }
                int treeKind = rand.nextInt(treeCount.length);
                if(treeCount[treeKind] > 0.5f && rand.nextFloat() < probFactor)
                {
                    treeCount[treeKind] -= 1.0f;
                    totalTreeCount--;
                }
            }
        }
        return tc;
    }

    private static int getChunkHash(final int cx, final int cz)
    {
        int retval = (int)((cx + 3L * cz) % hashPrime);
        if(retval < 0)
            retval += hashPrime;
        return retval;
    }

    private TreeChunk[] treeChunkHashTable = new TreeChunk[hashPrime];
    private Object[] treeChunkHashTableSync;

    private Tree getTree(final int x, final int z, final boolean make)
    {
        int cx = x - (x % TreeChunk.size + TreeChunk.size) % TreeChunk.size;
        int cz = z - (z % TreeChunk.size + TreeChunk.size) % TreeChunk.size;
        int hash = getChunkHash(cx, cz);
        synchronized(this.treeChunkHashTableSync[hash])
        {
            TreeChunk node = this.treeChunkHashTable[hash], parent = null;
            while(node != null)
            {
                if(node.cx == cx && node.cz == cz)
                {
                    if(parent != null)
                    {
                        parent.next = node.next;
                        node.next = this.treeChunkHashTable[hash];
                        this.treeChunkHashTable[hash] = node;
                    }
                    return node.getTree(x - cx, z - cz);
                }
                parent = node;
                node = node.next;
            }
            if(!make)
                return null;
            node = makeTreeChunk(cx, cz);
            node.next = this.treeChunkHashTable[hash];
            this.treeChunkHashTable[hash] = node;
            return node.getTree(x - cx, z - cz);
        }
    }

    @SuppressWarnings("unused")
    private synchronized Block internalGetTreeBlockKind(final int x,
                                                        final int y,
                                                        final int z)
    {
        final int searchDist = Tree.maxXZExtent;
        Block retval = null;
        for(int dx = -searchDist; dx <= searchDist; dx++)
        {
            for(int dz = -searchDist; dz <= searchDist; dz++)
            {
                int cx = dx + x, cz = dz + z;
                Tree tree = getTree(cx, cz, true);
                if(tree != null)
                {
                    int rockHeight = getRockHeight(cx, cz);
                    Block b = tree.getBlock(-dx, y - rockHeight, -dz);
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

    private static class TreeBlockKindChunk
    {
        public static final int size = 4;
        public int cx, cy, cz;
        private Block v[] = new Block[size * size * size];

        public TreeBlockKindChunk(final int cx, final int cy, final int cz)
        {
            this.cx = cx;
            this.cy = cy;
            this.cz = cz;
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
            this.v[x + size * (y + size * z)] = v;
        }
    }

    private TreeBlockKindChunk treeBlockKindChunkHashTable[] = new TreeBlockKindChunk[hashPrime];

    private synchronized void
        fillTreeBlockKindChunk(final TreeBlockKindChunk c)
    {
        final int searchDist = Tree.maxXZExtent;
        for(int dx = -searchDist; dx <= searchDist + TreeBlockKindChunk.size; dx++)
        {
            for(int dz = -searchDist; dz <= searchDist
                    + TreeBlockKindChunk.size; dz++)
            {
                int cx = dx + c.cx, cz = dz + c.cz;
                Tree tree = getTree(cx, cz, true);
                if(tree != null)
                {
                    int rockHeight = getRockHeight(cx, cz);
                    for(int x = c.cx; x < c.cx + TreeBlockKindChunk.size; x++)
                    {
                        for(int y = c.cy; y < c.cy + TreeBlockKindChunk.size; y++)
                        {
                            for(int z = c.cz; z < c.cz
                                    + TreeBlockKindChunk.size; z++)
                            {
                                Block retval = c.get(x, y, z);
                                Block b = tree.getBlock(x - cx,
                                                        y - rockHeight,
                                                        z - cz);
                                if(b == null)
                                    continue;
                                if(retval == null)
                                    retval = b;
                                else if(retval.getReplaceability(b.getType()) == BlockType.Replaceability.Replace)
                                    retval = b;
                                c.put(x, y, z, retval);
                            }
                        }
                    }
                }
            }
        }
    }

    private synchronized TreeBlockKindChunk getTreeBlockKindChunk(final int cx,
                                                                  final int cy,
                                                                  final int cz)
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

    private synchronized Block getTreeBlockKind(final int x,
                                                final int y,
                                                final int z)
    {
        int cx = x - (x % TreeBlockKindChunk.size + TreeBlockKindChunk.size)
                % TreeBlockKindChunk.size;
        int cy = y - (y % TreeBlockKindChunk.size + TreeBlockKindChunk.size)
                % TreeBlockKindChunk.size;
        int cz = z - (z % TreeBlockKindChunk.size + TreeBlockKindChunk.size)
                % TreeBlockKindChunk.size;
        TreeBlockKindChunk c = getTreeBlockKindChunk(cx, cy, cz);
        Block retval = c.get(x, y, z);
        return retval;
    }

    enum DecorationType
    {
        Torch, Chest, Cobweb, Last;
        public static final int count = Last.ordinal();
    }

    private Block getCaveDecoration(final int x, final int y, final int z)
    {
        final float genBlockProb = 0.1f;
        float ftype = genRand(x, y, z, RandClass.CaveDecoration)
                * DecorationType.count / genBlockProb;
        int type = (int)Math.floor(ftype);
        if(type >= DecorationType.count)
        {
            return new Block();
        }
        switch(DecorationType.values()[type])
        {
        case Cobweb:
        {
            int againstWallCount = 0;
            for(int o = 0; o < 6; o++)
                if(!isInCave(x + Block.getOrientationDX(o),
                             y + Block.getOrientationDY(o),
                             z + Block.getOrientationDZ(o)))
                    againstWallCount++;
            if(againstWallCount >= 2)
                return Block.NewCobweb();
            return new Block();
        }
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
            int i = 0;
            for(BlockType.AddedBlockDescriptor d : BlockType.getAllCaveChestBlocks(y))
            {
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
        case Last:
        default:
            return new Block();
        }
    }

    private static boolean didSetPos = false;
    private static Vector genChunk_t1 = Vector.allocate();

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
     * @return the generated chunk */
    @SuppressWarnings("unused")
    public GeneratedChunk genChunk(final int cx,
                                   final int cy,
                                   final int cz,
                                   final int chunkSize)
    {
        GeneratedChunk generatedChunk = new GeneratedChunk(chunkSize,
                                                           cx,
                                                           cy,
                                                           cz);
        if(false)
        {
            if(!didSetPos)
            {
                players.front().setPosition(genChunk_t1.set(0.5f, 1.0f, 0.5f));
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
                        Block tb = getTreeBlockKind(x, y, z);
                        if(tb == null)
                            block = getCaveDecoration(x, y, z);
                        else
                            block = tb;
                    }
                    else if(isLava(x, y, z))
                        block = Block.NewStationaryLava();
                    // else if(isOverLava(x, z) && y <= 5 + LavaNode.maxHeight)
                    // block = new Block();
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
                        block = Block.NewStationaryWater();
                    else
                    {
                        Block tb = getTreeBlockKind(x, y, z);
                        if(tb == null)
                            tb = new Block();
                        if(tb.getType() == BlockType.BTEmpty)
                        {
                            Block tb2 = getTreeBlockKind(x, y - 1, z);
                            if(tb2 != null
                                    && tb2.getType() == BlockType.BTEmpty)
                                tb2 = null;
                            if((tb2 != null || (y == rockHeight + 1 && !isInCave(x,
                                                                                 y - 1,
                                                                                 z)))
                                    && getBiomeSnow(x, z) > 0.5f)
                                tb = Block.NewSnow(1);
                        }
                        block = tb;
                    }
                    generatedChunk.setBlock(x, y, z, block);
                }
            }
        }
        return generatedChunk;
    }
}
