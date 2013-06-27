/**
 * this file is part of Voxels<BR/><BR/>
 * 
 * Voxels is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.<BR/><BR/>
 *
 * Voxels is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.<BR/><BR/>
 *
 * You should have received a copy of the GNU General Public License
 * along with Voxels.  If not, see <http://www.gnu.org/licenses/>.<BR/><BR/>
 */
package org.voxels;

import static org.voxels.PlayerList.players;
import static org.voxels.World.world;

import java.io.*;

import org.voxels.TextureAtlas.TextureHandle;

/** block<BR/>
 * not thread safe
 * 
 * @author jacob */
public class Block implements GameObject
{
    private BlockType type;

    private static class Data
    {
        // public Image imageref = null;
        public int intdata = 0;
        public int step = 0;
        public int[] BlockCounts = null;
        public BlockType blockdata = BlockType.BTEmpty;
        public int srccount = 0;
        public int destcount = 0;
        public int orientation = -1;
        public double runTime = -1.0;

        public Data()
        {
        }

        public Data(Data rt)
        {
            this.intdata = rt.intdata;
            this.BlockCounts = rt.BlockCounts;
            if(this.BlockCounts != null)
            {
                this.BlockCounts = new int[BlockType.Count];
                for(int i = 1; i < BlockType.Count; i++)
                    this.BlockCounts[i] = rt.BlockCounts[i];
            }
            this.blockdata = rt.blockdata;
            this.srccount = rt.srccount;
            this.destcount = rt.destcount;
            this.orientation = rt.orientation;
            this.runTime = rt.runTime;
            this.step = rt.step;
        }
    }

    private Data data = new Data();
    private int sunlight = 0, scatteredSunlight = 0, light = 0;
    private int lighting[] = null;
    private int curSunlightFactor = 0;

    private Block(BlockType newtype)
    {
        this.type = newtype;
    }

    private static final int DMaskNX = 0x20;
    private static final int DMaskPX = 0x10;
    private static final int DMaskNY = 0x8;
    private static final int DMaskPY = 0x4;
    private static final int DMaskNZ = 0x2;
    private static final int DMaskPZ = 0x1;

    /** @return true if this block is opaque */
    public boolean isOpaque()
    {
        return this.type.isOpaque;
    }

    /** creates an empty block */
    public Block()
    {
        this(BlockType.BTEmpty);
    }

    /** creates a copy of <code>rt</code>
     * 
     * @param rt
     *            block to create a copy of */
    public Block(Block rt)
    {
        this.type = rt.type;
        this.data = new Data(rt.data);
        this.sunlight = rt.sunlight;
        this.scatteredSunlight = rt.scatteredSunlight;
        this.light = rt.light;
    }

    /** creates the block used to show the delete animation
     * 
     * @param frame
     *            the frame to set the block to show. limited from 0 to 7.
     * @return new delete animation
     * @see #NewDeleteAnim(float t) */
    public static Block NewDeleteBlock(int frame)
    {
        Block retval = new Block(BlockType.BTDeleteBlock);
        retval.data.intdata = Math.min(Math.max(frame, 0), 7);
        return retval;
    }

    /** @return new stone block */
    public static Block NewStone()
    {
        return new Block(BlockType.BTStone);
    }

    /** @param ontime
     *            the amount of time left that the button is pushed
     * @param orientation
     *            the orientation of the new button
     * @return new stone button */
    public static Block NewStoneButton(int ontime, int orientation)
    {
        Block retval = new Block(BlockType.BTStoneButton);
        int neworientation = 4;
        if(orientation >= 0 && orientation <= 4)
            neworientation = orientation;
        retval.data.intdata = Math.max(0,
                                       Math.min(retval.type.getOnTime(), ontime));
        retval.data.orientation = neworientation;
        return retval;
    }

    /** @param ontime
     *            the amount of time left that the button is pushed
     * @param orientation
     *            the orientation of the new button
     * @return new wood button */
    public static Block NewWoodButton(int ontime, int orientation)
    {
        Block retval = new Block(BlockType.BTWoodButton);
        int neworientation = 4;
        if(orientation >= 0 && orientation <= 4)
            neworientation = orientation;
        retval.data.intdata = Math.max(0,
                                       Math.min(retval.type.getOnTime(), ontime));
        retval.data.orientation = neworientation;
        return retval;
    }

    /** @return new cobblestone block */
    public static Block NewCobblestone()
    {
        return new Block(BlockType.BTCobblestone);
    }

    /** @return new furnace */
    public static Block NewFurnace()
    {
        return NewFurnace(0, BlockType.BTEmpty, 0, 0);
    }

    /** @param fuelleft
     *            the amount of fuel left
     * @param sourceblock
     *            the block that is currently being smelted
     * @param srccount
     *            the number of blocks left to smelt
     * @param destcount
     *            the number of blocks already smelted
     * @return new furnace */
    public static Block NewFurnace(int fuelleft,
                                   BlockType sourceblock,
                                   int srccount,
                                   int destcount)
    {
        Block retval = new Block(BlockType.BTFurnace);
        retval.data.intdata = fuelleft;
        retval.data.blockdata = sourceblock;
        retval.data.destcount = destcount;
        retval.data.srccount = srccount;
        retval.data.runTime = world.getCurTime() + 10.0f; // time when furnace
                                                          // is done smelting
        return retval;
    }

    /** @return new workbench block */
    public static Block NewWorkbench()
    {
        return new Block(BlockType.BTWorkbench);
    }

    /** @return new chest */
    public static Block NewChest()
    {
        Block retval = new Block(BlockType.BTChest);
        retval.data.BlockCounts = new int[BlockType.Count];
        for(int i = 1; i < BlockType.Count; i++)
            retval.data.BlockCounts[i] = 0;
        return retval;
    }

    /** @return new sapling */
    public static Block NewSapling()
    {
        Block retval = new Block(BlockType.BTSapling);
        retval.data.runTime = world.getCurTime()
                + BlockType.BTSapling.getGrowTime();
        return retval;
    }

    /** @return new sand block */
    public static Block NewSand()
    {
        return new Block(BlockType.BTSand);
    }

    /** @return new gravel block */
    public static Block NewGravel()
    {
        return new Block(BlockType.BTGravel);
    }

    /** @return new wood block */
    public static Block NewWood()
    {
        return new Block(BlockType.BTWood);
    }

    /** @return new leaves block */
    public static Block NewLeaves()
    {
        return new Block(BlockType.BTLeaves);
    }

    /** @return new plank */
    public static Block NewPlank()
    {
        return new Block(BlockType.BTPlank);
    }

    /** @return new stick */
    public static Block NewStick()
    {
        return new Block(BlockType.BTStick);
    }

    /** @return new dirt block */
    public static Block NewDirt()
    {
        return new Block(BlockType.BTDirt);
    }

    /** @return new dirt block with grass on top */
    public static Block NewGrass()
    {
        return new Block(BlockType.BTGrass);
    }

    /** @return new bedrock block */
    public static Block NewBedrock()
    {
        return new Block(BlockType.BTBedrock);
    }

    /** @param amount
     *            the new height :
     *            <ul>
     *            <li>-8 : lava spring that isn't supported</li>
     *            <li>-7 - -1 : lava that isn't supported</li>
     *            <li>0 : not used</li>
     *            <li>1 - 7 : lava that is supported</li>
     *            <li>8 : lava spring that is supported</li>
     *            </ul>
     * @return new lava block */
    public static Block NewLava(int amount)
    {
        Block retval = new Block(BlockType.BTLava);
        retval.data.intdata = Math.min(8, Math.max(-8, amount));
        return retval;
    }

    /** @return new supported lava spring block */
    public static Block NewStationaryLava()
    {
        return NewLava(8);
    }

    /** @param amount
     *            the new height :
     *            <ul>
     *            <li>-8 : water spring that isn't supported</li>
     *            <li>-7 - -1 : water that isn't supported</li>
     *            <li>0 : not used</li>
     *            <li>1 - 7 : water that is supported</li>
     *            <li>8 : water spring that is supported</li>
     *            </ul>
     * @return new water block */
    public static Block NewWater(int amount)
    {
        Block retval = new Block(BlockType.BTWater);
        retval.data.intdata = Math.min(8, Math.max(-8, amount));
        return retval;
    }

    /** @return new supported water spring block */
    public static Block NewStationaryWater()
    {
        return NewWater(8);
    }

    /** @return new glass block */
    public static Block NewGlass()
    {
        return new Block(BlockType.BTGlass);
    }

    /** block that is used to display the sun
     * 
     * @return new sun as a block */
    public static Block NewSun()
    {
        Block retval = new Block(BlockType.BTSun);
        retval.setLighting(15, 15, 15);
        return retval;
    }

    /** block that is used to display the moon
     * 
     * @return new moon as a block */
    public static Block NewMoon()
    {
        Block retval = new Block(BlockType.BTMoon);
        retval.setLighting(15, 15, 15);
        return retval;
    }

    /** creates the block used to show the delete animation
     * 
     * @param t
     *            the time in the animation to set the block to show. limited
     *            from 0 to 1.
     * @return new delete animation
     * @see #NewDeleteBlock(int amount) */
    public static Block NewDeleteAnim(float t)
    {
        return NewDeleteBlock((int)Math.floor(Math.max(0.0f, Math.min(1.0f, t)) * 8.0f));
    }

    /** @return new wood pick */
    public static Block NewWoodPick()
    {
        return new Block(BlockType.BTWoodPick);
    }

    /** @return new stone pick */
    public static Block NewStonePick()
    {
        return new Block(BlockType.BTStonePick);
    }

    /** @return new wood shovel */
    public static Block NewWoodShovel()
    {
        return new Block(BlockType.BTWoodShovel);
    }

    /** @return new stone shovel */
    public static Block NewStoneShovel()
    {
        return new Block(BlockType.BTStoneShovel);
    }

    /** @return new iron pick */
    public static Block NewIronPick()
    {
        return new Block(BlockType.BTIronPick);
    }

    /** @return new iron shovel */
    public static Block NewIronShovel()
    {
        return new Block(BlockType.BTIronShovel);
    }

    /** @return new gold pick */
    public static Block NewGoldPick()
    {
        return new Block(BlockType.BTGoldPick);
    }

    /** @return new gold shovel */
    public static Block NewGoldShovel()
    {
        return new Block(BlockType.BTGoldShovel);
    }

    /** @return new diamond pick */
    public static Block NewDiamondPick()
    {
        return new Block(BlockType.BTDiamondPick);
    }

    /** @return new diamond shovel */
    public static Block NewDiamondShovel()
    {
        return new Block(BlockType.BTDiamondShovel);
    }

    /** creates a new redstone dust block
     * 
     * @param power
     *            the amount of power for the new block. limited from 0 to 15.
     * @param orientation
     *            the orientation mask of the new block.
     * @return new redstone dust block */
    public static Block NewRedstoneDust(int power, int orientation)
    {
        int newOrientation = orientation;
        newOrientation &= 0xFF;
        if((newOrientation & 0x1) == 0)
            newOrientation &= ~0x10;
        if((newOrientation & 0x2) == 0)
            newOrientation &= ~0x20;
        if((newOrientation & 0x4) == 0)
            newOrientation &= ~0x40;
        if((newOrientation & 0x8) == 0)
            newOrientation &= ~0x80;
        Block retval = new Block((power > 0) ? BlockType.BTRedstoneDustOn
                : BlockType.BTRedstoneDustOff);
        retval.data.intdata = Math.max(0, Math.min(15, power));
        retval.data.orientation = newOrientation;
        return retval;
    }

    /** @return new redstone ore block */
    public static Block NewRedstoneOre()
    {
        return new Block(BlockType.BTRedstoneOre);
    }

    /** @return new redstone block */
    public static Block NewRedstoneBlock()
    {
        return new Block(BlockType.BTRedstoneBlock);
    }

    /** @param state
     *            power state of the new torch
     * @param orientation
     *            orientation of the new torch
     * @return new redstone torch */
    public static Block NewRedstoneTorch(boolean state, int orientation)
    {
        int newOrientation = 4;
        if(orientation >= 0 && orientation <= 4)
            newOrientation = orientation;
        Block retval = new Block(state ? BlockType.BTRedstoneTorchOn
                : BlockType.BTRedstoneTorchOff);
        retval.data.orientation = newOrientation;
        return retval;
    }

    /** @param orientation
     *            orientation of the new torch
     * @return new torch */
    public static Block NewTorch(int orientation)
    {
        int newOrientation = 4;
        if(orientation >= 0 && orientation <= 4)
            newOrientation = orientation;
        Block retval = new Block(BlockType.BTTorch);
        retval.data.orientation = newOrientation;
        return retval;
    }

    /** @return new coal */
    public static Block NewCoal()
    {
        return new Block(BlockType.BTCoal);
    }

    /** @return new coal ore block */
    public static Block NewCoalOre()
    {
        return new Block(BlockType.BTCoalOre);
    }

    /** @return new iron ingot */
    public static Block NewIronIngot()
    {
        return new Block(BlockType.BTIronIngot);
    }

    /** @return new iron ore block */
    public static Block NewIronOre()
    {
        return new Block(BlockType.BTIronOre);
    }

    /** @return new lapis lazuli shard */
    public static Block NewLapisLazuli()
    {
        return new Block(BlockType.BTLapisLazuli);
    }

    /** @return new lapis lazuli ore block */
    public static Block NewLapisLazuliOre()
    {
        return new Block(BlockType.BTLapisLazuliOre);
    }

    /** @return new gold ingot */
    public static Block NewGoldIngot()
    {
        return new Block(BlockType.BTGoldIngot);
    }

    /** @return new gold ore block */
    public static Block NewGoldOre()
    {
        return new Block(BlockType.BTGoldOre);
    }

    /** @return new diamond block */
    public static Block NewDiamond()
    {
        return new Block(BlockType.BTDiamond);
    }

    /** @return new diamond ore block */
    public static Block NewDiamondOre()
    {
        return new Block(BlockType.BTDiamondOre);
    }

    /** @return new emerald block */
    public static Block NewEmerald()
    {
        return new Block(BlockType.BTEmerald);
    }

    /** @param orientation
     *            orientation of the new ladder
     * @return new ladder */
    public static Block NewLadder(int orientation)
    {
        Block retval = new Block(BlockType.BTLadder);
        retval.data.orientation = Math.max(0, Math.min(3, orientation));
        return retval;
    }

    /** @return new emerald ore block */
    public static Block NewEmeraldOre()
    {
        return new Block(BlockType.BTEmeraldOre);
    }

    /** create a new redstone repeater
     * 
     * @param value
     *            if the repeater is on
     * @param stepsleft
     *            the number of steps left to change or 0 if not changing
     * @param delay
     *            the delay
     * @param orientation
     *            the orientation
     * @return the new redstone repeater */
    public static Block NewRedstoneRepeater(boolean value,
                                            int stepsleft,
                                            int delay,
                                            int orientation)
    {
        Block retval;
        if(value)
            retval = new Block(BlockType.BTRedstoneRepeaterOn);
        else
            retval = new Block(BlockType.BTRedstoneRepeaterOff);
        retval.data.orientation = Math.max(0, Math.min(3, orientation));
        retval.data.intdata = Math.max(1, Math.min(4, delay));
        retval.data.step = Math.max(0,
                                    Math.min(retval.data.intdata - 1, stepsleft));
        return retval;
    }

    /** create a new lever
     * 
     * @param value
     *            if the lever is on
     * @param orientation
     *            the new lever's orientation
     * @return the new lever */
    public static Block NewLever(boolean value, int orientation)
    {
        Block retval = new Block(BlockType.BTLever);
        retval.data.intdata = value ? 1 : 0;
        retval.data.orientation = Math.max(0, Math.min(5, orientation));
        return retval;
    }

    /** @return new obsidian block */
    public static Block NewObsidian()
    {
        return new Block(BlockType.BTObsidian);
    }

    /** @param orientation
     *            the new piston's orientation
     * @param extended
     *            if the new piston is extended
     * @return new piston */
    public static Block NewPiston(int orientation, boolean extended)
    {
        Block retval = new Block(BlockType.BTPiston);
        retval.data.orientation = Math.max(0, Math.min(5, orientation));
        retval.data.intdata = (extended ? 1 : 0);
        retval.data.step = (extended ? 1 : 0);
        return retval;
    }

    /** @param orientation
     *            the new sticky piston's orientation
     * @param extended
     *            if the new sticky piston is extended
     * @return new sticky piston */
    public static Block NewStickyPiston(int orientation, boolean extended)
    {
        Block retval = new Block(BlockType.BTStickyPiston);
        retval.data.orientation = Math.max(0, Math.min(5, orientation));
        retval.data.intdata = (extended ? 1 : 0);
        retval.data.step = (extended ? 1 : 0);
        return retval;
    }

    /** @param orientation
     *            the new piston's head's orientation
     * @return new piston's head */
    public static Block NewPistonHead(int orientation)
    {
        Block retval = new Block(BlockType.BTPistonHead);
        retval.data.orientation = Math.max(0, Math.min(5, orientation));
        return retval;
    }

    /** @param orientation
     *            the new sticky piston's head's orientation
     * @return new sticky piston's head */
    public static Block NewStickyPistonHead(int orientation)
    {
        Block retval = new Block(BlockType.BTStickyPistonHead);
        retval.data.orientation = Math.max(0, Math.min(5, orientation));
        return retval;
    }

    /** @return new slime */
    public static Block NewSlime()
    {
        return new Block(BlockType.BTSlime);
    }

    /** @return new gunpowder */
    public static Block NewGunpowder()
    {
        return new Block(BlockType.BTGunpowder);
    }

    /** @return new TNT */
    public static Block NewTNT()
    {
        return new Block(BlockType.BTTNT);
    }

    /** @return new blaze rod */
    public static Block NewBlazeRod()
    {
        return new Block(BlockType.BTBlazeRod);
    }

    /** @return new blaze powder */
    public static Block NewBlazePowder()
    {
        return new Block(BlockType.BTBlazePowder);
    }

    /** @return new stone pressure plate */
    public static Block NewStonePressurePlate()
    {
        return new Block(BlockType.BTStonePressurePlate);
    }

    /** @return new wood pressure plate */
    public static Block NewWoodPressurePlate()
    {
        return new Block(BlockType.BTWoodPressurePlate);
    }

    private static RenderingStream drawFace(RenderingStream rs,
                                            TextureHandle texture,
                                            Vector p1,
                                            Vector p2,
                                            Vector p3,
                                            Vector p4,
                                            float u1,
                                            float v1,
                                            float u2,
                                            float v2,
                                            float u3,
                                            float v3,
                                            float u4,
                                            float v4,
                                            int bx,
                                            int by,
                                            int bz,
                                            boolean doublesided,
                                            boolean isEntity,
                                            boolean isAsItem,
                                            boolean isItemGlowing)
    {
        float c1, c2, c3, c4;
        Vector normal = p2.sub(p1).cross(p3.sub(p1)).normalize();
        if(isAsItem)
        {
            c1 = normal.dot(new Vector(0, 1, 0));
            if(c1 < 0)
                c1 = 0;
            c1 += 0.3f;
            if(c1 > 1)
                c1 = 1;
            if(isItemGlowing || true)
                c1 = 1;
            c2 = c1;
            c3 = c1;
            c4 = c1;
        }
        else if(isEntity)
        {
            c1 = world.getLighting(p1);
            c2 = world.getLighting(p2);
            c3 = world.getLighting(p3);
            c4 = world.getLighting(p4);
        }
        else
        {
            c1 = world.getLighting(p1, bx, by, bz);
            c2 = world.getLighting(p2, bx, by, bz);
            c3 = world.getLighting(p3, bx, by, bz);
            c4 = world.getLighting(p4, bx, by, bz);
        }
        rs.add(new RenderingStream.Polygon(texture).addVertex(p1,
                                                              u1,
                                                              v1,
                                                              Color.VA(c1, 1.0f))
                                                   .addVertex(p2,
                                                              u2,
                                                              v2,
                                                              Color.VA(c2, 1.0f))
                                                   .addVertex(p3,
                                                              u3,
                                                              v3,
                                                              Color.VA(c3, 1.0f)));
        rs.add(new RenderingStream.Polygon(texture).addVertex(p3,
                                                              u3,
                                                              v3,
                                                              Color.VA(c3, 1.0f))
                                                   .addVertex(p4,
                                                              u4,
                                                              v4,
                                                              Color.VA(c4, 1.0f))
                                                   .addVertex(p1,
                                                              u1,
                                                              v1,
                                                              Color.VA(c1, 1.0f)));
        if(doublesided)
            drawFace(rs,
                     texture,
                     p1,
                     p4,
                     p3,
                     p2,
                     u1,
                     v1,
                     u4,
                     v4,
                     u3,
                     v3,
                     u2,
                     v2,
                     bx,
                     by,
                     bz,
                     false,
                     isEntity,
                     isAsItem,
                     isItemGlowing);
        return rs;
    }

    private static RenderingStream drawFace(RenderingStream rs,
                                            TextureAtlas.TextureHandle texture,
                                            Vector p1,
                                            Vector p2,
                                            Vector p3,
                                            float u1,
                                            float v1,
                                            float u2,
                                            float v2,
                                            float u3,
                                            float v3,
                                            int bx,
                                            int by,
                                            int bz,
                                            boolean doublesided,
                                            boolean isEntity,
                                            boolean isAsItem,
                                            boolean isItemGlowing)
    {
        float c1, c2, c3;
        Vector normal = p2.sub(p1).cross(p3.sub(p1)).normalize();
        if(isAsItem)
        {
            c1 = normal.dot(new Vector(0, 1, 0));
            if(c1 < 0)
                c1 = 0;
            c1 += 0.3f;
            if(c1 > 1)
                c1 = 1;
            if(isItemGlowing || true)
                c1 = 1;
            c2 = c1;
            c3 = c1;
        }
        else if(isEntity)
        {
            c1 = world.getLighting(p1);
            c2 = world.getLighting(p2);
            c3 = world.getLighting(p3);
        }
        else
        {
            c1 = world.getLighting(p1, bx, by, bz);
            c2 = world.getLighting(p2, bx, by, bz);
            c3 = world.getLighting(p3, bx, by, bz);
        }
        rs.add(new RenderingStream.Polygon(texture).addVertex(p1,
                                                              u1,
                                                              v1,
                                                              Color.VA(c1, 1.0f))
                                                   .addVertex(p2,
                                                              u2,
                                                              v2,
                                                              Color.VA(c2, 1.0f))
                                                   .addVertex(p3,
                                                              u3,
                                                              v3,
                                                              Color.VA(c3, 1.0f)));
        if(doublesided)
            drawFace(rs,
                     texture,
                     p1,
                     p3,
                     p2,
                     u1,
                     v1,
                     u3,
                     v3,
                     u2,
                     v2,
                     bx,
                     by,
                     bz,
                     false,
                     isEntity,
                     isAsItem,
                     isItemGlowing);
        return rs;
    }

    private static RenderingStream drawItem(RenderingStream rs,
                                            Matrix localToBlock,
                                            Matrix blockToWorld,
                                            int bx,
                                            int by,
                                            int bz,
                                            TextureHandle img,
                                            boolean isEntity,
                                            boolean isAsItem)
    {
        Matrix localToWorld = localToBlock.concat(blockToWorld);
        Vector p1 = localToWorld.apply(new Vector(0, 0, 0));
        Vector p2 = localToWorld.apply(new Vector(1, 0, 0));
        Vector p3 = localToWorld.apply(new Vector(0, 1, 0));
        Vector p4 = localToWorld.apply(new Vector(1, 1, 0));
        final float minu = 0, maxu = 1, minv = 0, maxv = 1;
        drawFace(rs,
                 img,
                 p1,
                 p2,
                 p4,
                 p3,
                 minu,
                 minv,
                 maxu,
                 minv,
                 maxu,
                 maxv,
                 minu,
                 maxv,
                 bx,
                 by,
                 bz,
                 true,
                 isEntity,
                 isAsItem,
                 false);
        return rs;
    }

    private static RenderingStream drawItem(RenderingStream rs,
                                            Matrix localToBlock,
                                            Matrix blockToWorld,
                                            int bx,
                                            int by,
                                            int bz,
                                            TextureHandle img,
                                            boolean isEntity,
                                            boolean isAsItem,
                                            float minu,
                                            float minv,
                                            float maxu,
                                            float maxv)
    {
        Matrix localToWorld = localToBlock.concat(blockToWorld);
        Vector p1 = localToWorld.apply(new Vector(0, 0, 0));
        Vector p2 = localToWorld.apply(new Vector(1, 0, 0));
        Vector p3 = localToWorld.apply(new Vector(0, 1, 0));
        Vector p4 = localToWorld.apply(new Vector(1, 1, 0));
        drawFace(rs,
                 img,
                 p1,
                 p2,
                 p4,
                 p3,
                 minu,
                 minv,
                 maxu,
                 minv,
                 maxu,
                 maxv,
                 minu,
                 maxv,
                 bx,
                 by,
                 bz,
                 true,
                 isEntity,
                 isAsItem,
                 false);
        return rs;
    }

    private RenderingStream internalDraw(RenderingStream rs,
                                         int drawMask,
                                         Matrix localToBlock,
                                         Matrix blockToWorld,
                                         int bx,
                                         int by,
                                         int bz,
                                         TextureHandle img,
                                         boolean doubleSided,
                                         boolean isEntity,
                                         boolean isAsItem)
    {
        if(drawMask == 0)
            return rs;
        Matrix localToWorld = localToBlock.concat(blockToWorld);
        Vector p1 = localToWorld.apply(new Vector(0, 0, 0));
        Vector p2 = localToWorld.apply(new Vector(1, 0, 0));
        Vector p3 = localToWorld.apply(new Vector(0, 1, 0));
        Vector p4 = localToWorld.apply(new Vector(1, 1, 0));
        Vector p5 = localToWorld.apply(new Vector(0, 0, 1));
        Vector p6 = localToWorld.apply(new Vector(1, 0, 1));
        Vector p7 = localToWorld.apply(new Vector(0, 1, 1));
        Vector p8 = localToWorld.apply(new Vector(1, 1, 1));
        if((drawMask & 0x20) != 0) // -X
        {
            final float minu = 0.0f, maxu = 0.25f, minv = 0.5f, maxv = 1.0f;
            // p1, p5, p7, p3
            drawFace(rs,
                     img,
                     p1,
                     p5,
                     p7,
                     p3,
                     minu,
                     minv,
                     maxu,
                     minv,
                     maxu,
                     maxv,
                     minu,
                     maxv,
                     bx,
                     by,
                     bz,
                     doubleSided,
                     isEntity,
                     isAsItem,
                     (this.type == BlockType.BTSun || this.type == BlockType.BTMoon));
        }
        if((drawMask & 0x10) != 0) // +X
        {
            final float minu = 0.0f, maxu = 0.25f, minv = 0.0f, maxv = 0.5f;
            // p2, p4, p8, p6
            drawFace(rs,
                     img,
                     p2,
                     p4,
                     p8,
                     p6,
                     maxu,
                     minv,
                     maxu,
                     maxv,
                     minu,
                     maxv,
                     minu,
                     minv,
                     bx,
                     by,
                     bz,
                     doubleSided,
                     isEntity,
                     isAsItem,
                     (this.type == BlockType.BTSun || this.type == BlockType.BTMoon));
        }
        if((drawMask & 0x8) != 0) // -Y
        {
            final float minu = 0.25f, maxu = 0.5f, minv = 0.5f, maxv = 1.0f;
            // p1, p2, p6, p5
            drawFace(rs,
                     img,
                     p1,
                     p2,
                     p6,
                     p5,
                     minu,
                     minv,
                     maxu,
                     minv,
                     maxu,
                     maxv,
                     minu,
                     maxv,
                     bx,
                     by,
                     bz,
                     doubleSided,
                     isEntity,
                     isAsItem,
                     (this.type == BlockType.BTSun || this.type == BlockType.BTMoon));
        }
        if((drawMask & 0x4) != 0) // +Y
        {
            final float minu = 0.25f, maxu = 0.5f, minv = 0.0f, maxv = 0.5f;
            // p3, p7, p8, p4
            drawFace(rs,
                     img,
                     p3,
                     p7,
                     p8,
                     p4,
                     minu,
                     maxv,
                     minu,
                     minv,
                     maxu,
                     minv,
                     maxu,
                     maxv,
                     bx,
                     by,
                     bz,
                     doubleSided,
                     isEntity,
                     isAsItem,
                     (this.type == BlockType.BTSun || this.type == BlockType.BTMoon));
        }
        if((drawMask & 0x2) != 0) // -Z
        {
            final float minu = 0.5f, maxu = 0.75f, minv = 0.5f, maxv = 1.0f;
            // p1, p3, p4, p2
            drawFace(rs,
                     img,
                     p1,
                     p3,
                     p4,
                     p2,
                     maxu,
                     minv,
                     maxu,
                     maxv,
                     minu,
                     maxv,
                     minu,
                     minv,
                     bx,
                     by,
                     bz,
                     doubleSided,
                     isEntity,
                     isAsItem,
                     (this.type == BlockType.BTSun || this.type == BlockType.BTMoon));
        }
        if((drawMask & 0x1) != 0) // +Z
        {
            final float minu = 0.5f, maxu = 0.75f, minv = 0.0f, maxv = 0.5f;
            // p5, p6, p8, p7
            drawFace(rs,
                     img,
                     p5,
                     p6,
                     p8,
                     p7,
                     minu,
                     minv,
                     maxu,
                     minv,
                     maxu,
                     maxv,
                     minu,
                     maxv,
                     bx,
                     by,
                     bz,
                     doubleSided,
                     isEntity,
                     isAsItem,
                     (this.type == BlockType.BTSun || this.type == BlockType.BTMoon));
        }
        return rs;
    }

    private static final boolean IGNORE_FALLING_FLUID = false;

    @SuppressWarnings("unused")
    private float getFluidHeight(int x, int y, int z)
    {
        if(Math.abs(this.data.intdata) >= 7)
            return 1.0f;
        float retval = getHeight();
        boolean gotAny = false;
        if(true)
        {
            for(int dx = -1; dx <= 0; dx++)
            {
                for(int dz = -1; dz <= 0; dz++)
                {
                    Block b = world.getBlock(x + dx, y, z + dz);
                    if(b == null || b.type != this.type
                            || (IGNORE_FALLING_FLUID && b.data.intdata < 0))
                    {
                        continue;
                    }
                    gotAny = true;
                    float height = b.getHeight();
                    if(height > retval)
                    {
                        retval = height;
                    }
                }
            }
            if(Math.abs(this.data.intdata) <= 1 && !gotAny)
                return 0.0f;
            return retval; // to suppress warning 'Unnecessary
                           // @SuppressWarnings("unused")'
        }
        return retval;
    }

    private float getFluidBottom(int x,
                                 int y,
                                 int z,
                                 int bx,
                                 int by,
                                 int bz,
                                 int dx,
                                 int dz,
                                 float maxV)
    {
        Block b = world.getBlock(bx + dx, by, bz + dz);
        if(b == null || b.type != this.type)
            return 0.0f;
        float h = b.getFluidHeight(x, y, z);
        return Math.min(maxV, h);
    }

    private float interpolate(float t, float a, float b)
    {
        return (b - a) * t + a;
    }

    private Vector interpolate(float t, Vector a, Vector b)
    {
        return b.sub(a).mul(t).add(a);
    }

    private RenderingStream drawFluidFace(RenderingStream rs,
                                          TextureHandle texture,
                                          Vector nunv,
                                          Vector punv,
                                          Vector pupv,
                                          Vector nupv,
                                          float minu,
                                          float maxu,
                                          float minv,
                                          float maxv,
                                          float tnu,
                                          float bnu,
                                          float tpu,
                                          float bpu,
                                          int bx,
                                          int by,
                                          int bz)
    {
        Vector nunv_p = interpolate(bnu, nunv, nupv);
        float nunv_u = minu;
        float nunv_v = interpolate(bnu, minv, maxv);
        Vector nupv_p = interpolate(tnu, nunv, nupv);
        float nupv_u = minu;
        float nupv_v = interpolate(tnu, minv, maxv);
        Vector punv_p = interpolate(bpu, punv, pupv);
        float punv_u = maxu;
        float punv_v = interpolate(bpu, minv, maxv);
        Vector pupv_p = interpolate(tpu, punv, pupv);
        float pupv_u = maxu;
        float pupv_v = interpolate(tpu, minv, maxv);
        drawFace(rs,
                 texture,
                 nunv_p,
                 punv_p,
                 pupv_p,
                 nupv_p,
                 nunv_u,
                 nunv_v,
                 punv_u,
                 punv_v,
                 pupv_u,
                 pupv_v,
                 nupv_u,
                 nupv_v,
                 bx,
                 by,
                 bz,
                 true,
                 false,
                 false,
                 (this.type == BlockType.BTSun || this.type == BlockType.BTMoon));
        return rs;
    }

    private RenderingStream drawFluid(RenderingStream rs,
                                      Matrix blockToWorld,
                                      int bx,
                                      int by,
                                      int bz,
                                      TextureHandle img)
    {
        Block nx = world.getBlock(bx - 1, by, bz);
        Block px = world.getBlock(bx + 1, by, bz);
        Block ny = world.getBlock(bx, by - 1, bz);
        Block py = world.getBlock(bx, by + 1, bz);
        Block nz = world.getBlock(bx, by, bz - 1);
        Block pz = world.getBlock(bx, by, bz + 1);
        int drawMask = 0;
        float height = getHeight();
        if(nx != null && !nx.isOpaque() && nx.type != this.type
                && nx.getHeight() >= height)
            drawMask |= DMaskNX;
        if(px != null && !px.isOpaque() && px.type != this.type
                && px.getHeight() >= height)
            drawMask |= DMaskPX;
        if(ny != null && !ny.isOpaque()
                && (ny.type != this.type || ny.getHeight() < 1.0f))
            drawMask |= DMaskNY;
        if(py != null && !py.isOpaque() && py.type != this.type)
            drawMask |= DMaskPY;
        if(nz != null && !nz.isOpaque() && nz.type != this.type
                && nz.getHeight() >= height)
            drawMask |= DMaskNZ;
        if(pz != null && !pz.isOpaque() && pz.type != this.type
                && pz.getHeight() >= height)
            drawMask |= DMaskPZ;
        if(height < 1.0f)
            drawMask |= DMaskPY;
        if(drawMask == 0)
            return rs;
        float t00 = getFluidHeight(bx, by, bz);
        float b00nx = getFluidBottom(bx, by, bz, bx, by, bz, -1, 0, t00);
        float b00nz = getFluidBottom(bx, by, bz, bx, by, bz, 0, -1, t00);
        float t01 = getFluidHeight(bx, by, bz + 1);
        float b01nx = getFluidBottom(bx, by, bz + 1, bx, by, bz, -1, 0, t01);
        float b01pz = getFluidBottom(bx, by, bz + 1, bx, by, bz, 0, 1, t01);
        float t10 = getFluidHeight(bx + 1, by, bz);
        float b10px = getFluidBottom(bx + 1, by, bz, bx, by, bz, 1, 0, t10);
        float b10nz = getFluidBottom(bx + 1, by, bz, bx, by, bz, 0, -1, t10);
        float t11 = getFluidHeight(bx + 1, by, bz + 1);
        float b11px = getFluidBottom(bx + 1, by, bz + 1, bx, by, bz, 1, 0, t11);
        float b11pz = getFluidBottom(bx + 1, by, bz + 1, bx, by, bz, 0, 1, t11);
        float avgt = (t00 + t01 + t10 + t11) / 4.0f;
        Vector p1 = blockToWorld.apply(new Vector(0, 0, 0));
        Vector p2 = blockToWorld.apply(new Vector(1, 0, 0));
        Vector p3 = blockToWorld.apply(new Vector(0, 1, 0));
        Vector p4 = blockToWorld.apply(new Vector(1, 1, 0));
        Vector p5 = blockToWorld.apply(new Vector(0, 0, 1));
        Vector p6 = blockToWorld.apply(new Vector(1, 0, 1));
        Vector p7 = blockToWorld.apply(new Vector(0, 1, 1));
        Vector p8 = blockToWorld.apply(new Vector(1, 1, 1));
        Vector pCenter = blockToWorld.apply(new Vector(0.5f, avgt, 0.5f));
        if((drawMask & 0x20) != 0) // -X
        {
            final float minu = 0.0f, maxu = 0.25f, minv = 0.5f, maxv = 1.0f;
            Vector nunv = p1, punv = p5, pupv = p7, nupv = p3;
            float bnu = b00nx, bpu = b01nx;
            float tnu = t00, tpu = t01;
            drawFluidFace(rs,
                          img,
                          nunv,
                          punv,
                          pupv,
                          nupv,
                          minu,
                          maxu,
                          minv,
                          maxv,
                          tnu,
                          bnu,
                          tpu,
                          bpu,
                          bx,
                          by,
                          bz);
        }
        if((drawMask & 0x10) != 0) // +X
        {
            final float minu = 0.0f, maxu = 0.25f, minv = 0.0f, maxv = 0.5f;
            Vector punv = p2, pupv = p4, nupv = p8, nunv = p6;
            float bnu = b11px, bpu = b10px;
            float tnu = t11, tpu = t10;
            drawFluidFace(rs,
                          img,
                          nunv,
                          punv,
                          pupv,
                          nupv,
                          minu,
                          maxu,
                          minv,
                          maxv,
                          tnu,
                          bnu,
                          tpu,
                          bpu,
                          bx,
                          by,
                          bz);
        }
        if((drawMask & 0x8) != 0) // -Y
        {
            final float minu = 0.25f, maxu = 0.5f, minv = 0.5f, maxv = 1.0f;
            // p1, p2, p6, p5
            drawFace(rs,
                     img,
                     p1,
                     p2,
                     p6,
                     p5,
                     minu,
                     minv,
                     maxu,
                     minv,
                     maxu,
                     maxv,
                     minu,
                     maxv,
                     bx,
                     by,
                     bz,
                     true,
                     false,
                     false,
                     (this.type == BlockType.BTSun || this.type == BlockType.BTMoon));
        }
        if((drawMask & 0x2) != 0) // -Z
        {
            final float minu = 0.5f, maxu = 0.75f, minv = 0.5f, maxv = 1.0f;
            Vector punv = p1, pupv = p3, nupv = p4, nunv = p2;
            float bnu = b10nz, bpu = b00nz;
            float tnu = t10, tpu = t00;
            drawFluidFace(rs,
                          img,
                          nunv,
                          punv,
                          pupv,
                          nupv,
                          minu,
                          maxu,
                          minv,
                          maxv,
                          tnu,
                          bnu,
                          tpu,
                          bpu,
                          bx,
                          by,
                          bz);
        }
        if((drawMask & 0x1) != 0) // +Z
        {
            final float minu = 0.5f, maxu = 0.75f, minv = 0.0f, maxv = 0.5f;
            Vector nunv = p5, punv = p6, pupv = p8, nupv = p7;
            float bnu = b01pz, bpu = b11pz;
            float tnu = t01, tpu = t11;
            drawFluidFace(rs,
                          img,
                          nunv,
                          punv,
                          pupv,
                          nupv,
                          minu,
                          maxu,
                          minv,
                          maxv,
                          tnu,
                          bnu,
                          tpu,
                          bpu,
                          bx,
                          by,
                          bz);
        }
        if((drawMask & 0x4) != 0) // +Y
        {
            final float minu = 0.25f, maxu = 0.5f, minv = 0.0f, maxv = 0.5f;
            final float cu = (minu + maxu) / 2.0f, cv = (minv + maxv) / 2.0f;
            // p3, p7, p8, p4
            Vector nupv_p = interpolate(t00, p1, p3);
            Vector nunv_p = interpolate(t01, p5, p7);
            Vector punv_p = interpolate(t11, p6, p8);
            Vector pupv_p = interpolate(t10, p2, p4);
            drawFace(rs,
                     img,
                     nupv_p,
                     nunv_p,
                     pCenter,
                     minu,
                     maxv,
                     minu,
                     minv,
                     cu,
                     cv,
                     bx,
                     by,
                     bz,
                     true,
                     false,
                     false,
                     (this.type == BlockType.BTSun || this.type == BlockType.BTMoon));
            drawFace(rs,
                     img,
                     nunv_p,
                     punv_p,
                     pCenter,
                     minu,
                     minv,
                     maxu,
                     minv,
                     cu,
                     cv,
                     bx,
                     by,
                     bz,
                     true,
                     false,
                     false,
                     (this.type == BlockType.BTSun || this.type == BlockType.BTMoon));
            drawFace(rs,
                     img,
                     punv_p,
                     pupv_p,
                     pCenter,
                     maxu,
                     minv,
                     maxu,
                     maxv,
                     cu,
                     cv,
                     bx,
                     by,
                     bz,
                     true,
                     false,
                     false,
                     (this.type == BlockType.BTSun || this.type == BlockType.BTMoon));
            drawFace(rs,
                     img,
                     pupv_p,
                     nupv_p,
                     pCenter,
                     maxu,
                     maxv,
                     minu,
                     maxv,
                     cu,
                     cv,
                     bx,
                     by,
                     bz,
                     true,
                     false,
                     false,
                     (this.type == BlockType.BTSun || this.type == BlockType.BTMoon));
        }
        return rs;
    }

    private RenderingStream drawSolid(RenderingStream rs,
                                      Matrix blockToWorld,
                                      int bx,
                                      int by,
                                      int bz,
                                      boolean drawAllSides,
                                      TextureHandle img,
                                      boolean isEntity,
                                      boolean isAsItem)
    {
        int drawMask;
        if(isAsItem || isEntity || drawAllSides)
        {
            drawMask = 0x3F;
        }
        else
        {
            Block nx = world.getBlock(bx - 1, by, bz);
            Block px = world.getBlock(bx + 1, by, bz);
            Block ny = world.getBlock(bx, by - 1, bz);
            Block py = world.getBlock(bx, by + 1, bz);
            Block nz = world.getBlock(bx, by, bz - 1);
            Block pz = world.getBlock(bx, by, bz + 1);
            drawMask = 0;
            if(nx != null && !nx.isOpaque())
                drawMask |= DMaskNX;
            if(px != null && !px.isOpaque())
                drawMask |= DMaskPX;
            if(ny != null && !ny.isOpaque())
                drawMask |= DMaskNY;
            if(py != null && !py.isOpaque())
                drawMask |= DMaskPY;
            if(nz != null && !nz.isOpaque())
                drawMask |= DMaskNZ;
            if(pz != null && !pz.isOpaque())
                drawMask |= DMaskPZ;
        }
        internalDraw(rs,
                     drawMask,
                     Matrix.identity(),
                     blockToWorld,
                     bx,
                     by,
                     bz,
                     img,
                     this.type.isDoubleSided(),
                     isEntity,
                     isAsItem);
        return rs;
    }

    private RenderingStream drawSolid(RenderingStream rs,
                                      Matrix blockToWorld,
                                      int bx,
                                      int by,
                                      int bz,
                                      boolean drawAllSides,
                                      boolean isEntity,
                                      boolean isAsItem)
    {
        return drawSolid(rs,
                         blockToWorld,
                         bx,
                         by,
                         bz,
                         drawAllSides,
                         this.type.textures[this.data.intdata],
                         isEntity,
                         isAsItem);
    }

    private RenderingStream drawSim3D(RenderingStream rs,
                                      Matrix localToBlock,
                                      Matrix blockToWorld,
                                      int bx,
                                      int by,
                                      int bz,
                                      boolean isEntity,
                                      boolean isAsItem,
                                      TextureHandle img)
    {
        Matrix localToWorld = localToBlock.concat(blockToWorld);
        {
            Vector p1 = localToWorld.apply(new Vector(0, 0, 0.5f));
            Vector p2 = localToWorld.apply(new Vector(1, 0, 0.5f));
            Vector p3 = localToWorld.apply(new Vector(0, 1, 0.5f));
            Vector p4 = localToWorld.apply(new Vector(1, 1, 0.5f));
            final float minu = 0, maxu = 0.5f, minv = 0, maxv = 1;
            drawFace(rs,
                     img,
                     p1,
                     p2,
                     p4,
                     p3,
                     minu,
                     minv,
                     maxu,
                     minv,
                     maxu,
                     maxv,
                     minu,
                     maxv,
                     bx,
                     by,
                     bz,
                     true,
                     isEntity,
                     isAsItem,
                     false);
        }
        {
            Vector p1 = localToWorld.apply(new Vector(0.5f, 0, 0));
            Vector p2 = localToWorld.apply(new Vector(0.5f, 0, 1));
            Vector p3 = localToWorld.apply(new Vector(0.5f, 1, 0));
            Vector p4 = localToWorld.apply(new Vector(0.5f, 1, 1));
            final float minu = 0.5f, maxu = 1, minv = 0, maxv = 1;
            drawFace(rs,
                     img,
                     p1,
                     p2,
                     p4,
                     p3,
                     minu,
                     minv,
                     maxu,
                     minv,
                     maxu,
                     maxv,
                     minu,
                     maxv,
                     bx,
                     by,
                     bz,
                     true,
                     isEntity,
                     isAsItem,
                     false);
        }
        return rs;
    }

    @Override
    public RenderingStream draw(RenderingStream rs, Matrix blockToWorld)
    {
        return draw(rs, blockToWorld, false, false);
    }

    private static Matrix getButtonTransform(int orientation, boolean pushed)
    {
        final float ButtonSize = 0.2f;
        final float ButtonDepth = 0.1f;
        final float ButtonPushedDepth = 0.05f;
        float depth = ButtonDepth;
        if(pushed)
            depth = ButtonPushedDepth;
        Matrix tform = Matrix.translate(-0.5f, 0.0f, -0.5f)
                             .concat(Matrix.scale(ButtonSize, depth, ButtonSize));
        tform = tform.concat(Matrix.translate(0.0f, -0.5f, 0.0f));
        switch(orientation)
        {
        case 0: // -X
            tform = tform.concat(Matrix.rotatez(-Math.PI / 2));
            break;
        case 1: // -Z
            tform = tform.concat(Matrix.rotatex(Math.PI / 2));
            break;
        case 2: // +X
            tform = tform.concat(Matrix.rotatez(Math.PI / 2));
            break;
        case 3: // +Z
            tform = tform.concat(Matrix.rotatex(-Math.PI / 2));
            break;
        // case 4: // -Y
        default:
            break;
        }
        return tform.concat(Matrix.translate(0.5f, 0.5f, 0.5f));
    }

    private static Matrix getTorchTransform(int orientation)
    {
        final float xzscale = 1 / 8f, yscale = 6 / 8f;
        Matrix tform = Matrix.translate(-0.5f, 0.0f, -0.5f)
                             .concat(Matrix.scale(xzscale, yscale, xzscale));
        tform = tform.concat(Matrix.translate(0.5f, 0.0f, 0.5f));
        final float distfromedge = 0.5f - xzscale / 2.0f;
        final float distfromtop = 1.0f - yscale;
        switch(orientation)
        {
        case 0: // -X
            tform = tform.concat(Matrix.translate(-distfromedge,
                                                  distfromtop / 2,
                                                  0));
            break;
        case 1: // -Z
            tform = tform.concat(Matrix.translate(0,
                                                  distfromtop / 2,
                                                  -distfromedge));
            break;
        case 2: // +X
            tform = tform.concat(Matrix.translate(distfromedge,
                                                  distfromtop / 2,
                                                  0));
            break;
        case 3: // +Z
            tform = tform.concat(Matrix.translate(0,
                                                  distfromtop / 2,
                                                  distfromedge));
            break;
        // case 4: // -Y
        default:
            break;
        }
        return tform;
    }

    private static Matrix getLeverTransform(int orientation)
    {
        Matrix tform = Matrix.translate(-0.5f, 0, -0.5f);
        tform = tform.concat(Matrix.scale(8.0f / 16, 3.0f / 16, 6.0f / 16));
        tform = tform.concat(Matrix.translate(0.0f, -0.5f, 0.0f));
        switch(orientation)
        {
        case 0: // -X
            tform = tform.concat(Matrix.rotatez(-Math.PI / 2));
            break;
        case 1: // -Z
            tform = tform.concat(Matrix.rotatex(Math.PI / 2));
            break;
        case 2: // +X
            tform = tform.concat(Matrix.rotatez(Math.PI / 2));
            break;
        case 3: // +Z
            tform = tform.concat(Matrix.rotatex(-Math.PI / 2));
            break;
        case 5: // +Y
            tform = tform.concat(Matrix.rotatex(-Math.PI));
            break;
        // case 4: // -Y
        default:
            break;
        }
        return tform.concat(Matrix.translate(0.5f, 0.5f, 0.5f));
    }

    private static Matrix
        getLeverHandleTransform(int orientation, boolean state)
    {
        Matrix tform = Matrix.translate(-0.5f, 0, -0.5f);
        tform = tform.concat(Matrix.scale(2.0f / 16, 8.0f / 16, 2.0f / 16));
        tform = tform.concat(Matrix.rotatez(state ? Math.PI / 6 : -Math.PI / 6));
        tform = tform.concat(Matrix.translate(0.0f, -0.5f, 0.0f));
        switch(orientation)
        {
        case 0: // -X
            tform = tform.concat(Matrix.rotatez(-Math.PI / 2));
            break;
        case 1: // -Z
            tform = tform.concat(Matrix.rotatex(Math.PI / 2));
            break;
        case 2: // +X
            tform = tform.concat(Matrix.rotatez(Math.PI / 2));
            break;
        case 3: // +Z
            tform = tform.concat(Matrix.rotatex(-Math.PI / 2));
            break;
        case 5: // +Y
            tform = tform.concat(Matrix.rotatex(-Math.PI));
            break;
        // case 4: // -Y
        default:
            break;
        }
        return tform.concat(Matrix.translate(0.5f, 0.5f, 0.5f));
    }

    private RenderingStream draw(RenderingStream rs,
                                 Matrix blockToWorld,
                                 boolean isEntity,
                                 boolean isAsItem)
    {
        int bx, by, bz;
        Vector pos = blockToWorld.apply(new Vector(0));
        bx = (int)Math.floor(pos.x + 0.5);
        by = (int)Math.floor(pos.y + 0.5);
        bz = (int)Math.floor(pos.z + 0.5);
        switch(this.type.drawType)
        {
        case BDTCustom:
            switch(this.type)
            {
            case BTFurnace:
                if(this.data.intdata > 0
                        && this.data.blockdata != BlockType.BTEmpty
                        && this.data.srccount > 0)
                    drawSolid(rs,
                              blockToWorld,
                              bx,
                              by,
                              bz,
                              false,
                              this.type.textures[1],
                              isEntity,
                              isAsItem);
                else
                    drawSolid(rs,
                              blockToWorld,
                              bx,
                              by,
                              bz,
                              false,
                              this.type.textures[0],
                              isEntity,
                              isAsItem);
                break;
            case BTRedstoneDustOff:
            case BTRedstoneDustOn:
            {
                TextureHandle ImgBase = this.type.textures[(this.data.orientation & 0xA)
                        | ((this.data.orientation << 2) & 0x4)
                        | ((this.data.orientation >> 2) & 0x1)];
                Matrix tform = Matrix.translate(-0.5f, -0.5f, -0.5f)
                                     .concat(Matrix.scale(0.98f))
                                     .concat(Matrix.translate(0.5f, 0.5f, 0.5f));
                drawItem(rs,
                         Matrix.rotatex(Math.PI / 2).concat(tform),
                         blockToWorld,
                         bx,
                         by,
                         bz,
                         ImgBase,
                         isEntity,
                         isAsItem);
                int drawmask = 0;
                if((this.data.orientation & 0x10) != 0)
                    drawmask |= 0x20;
                if((this.data.orientation & 0x40) != 0)
                    drawmask |= 0x10;
                if((this.data.orientation & 0x20) != 0)
                    drawmask |= 0x2;
                if((this.data.orientation & 0x80) != 0)
                    drawmask |= 0x1;
                internalDraw(rs,
                             drawmask,
                             tform,
                             blockToWorld,
                             bx,
                             by,
                             bz,
                             this.type.textures[16],
                             true,
                             isEntity,
                             isAsItem);
                break;
            }
            case BTSapling:
                drawSim3D(rs,
                          Matrix.identity(),
                          blockToWorld,
                          bx,
                          by,
                          bz,
                          isEntity,
                          isAsItem,
                          this.type.textures[this.data.intdata]);
                break;
            case BTDeleteBlock:
                internalDraw(rs,
                             0x3F,
                             Matrix.translate(-0.5f, -0.5f, -0.5f)
                                   .concat(Matrix.scale(1.05f))
                                   .concat(Matrix.translate(0.5f, 0.5f, 0.5f)),
                             blockToWorld,
                             bx,
                             by,
                             bz,
                             this.type.textures[this.data.intdata],
                             this.type.isDoubleSided(),
                             isEntity,
                             isAsItem);
                break;
            case BTWood:
            case BTLeaves:
                drawSolid(rs,
                          blockToWorld,
                          bx,
                          by,
                          bz,
                          false,
                          this.type.textures[0],
                          isEntity,
                          isAsItem);
                break;
            case BTLadder:
                drawItem(rs,
                         Matrix.translate(-0.5f, -0.5f, -0.49f)
                               .concat(Matrix.rotatey(Math.PI / 2.0
                                       * (1 - this.data.orientation)))
                               .concat(Matrix.translate(0.5f, 0.5f, 0.5f)),
                         blockToWorld,
                         bx,
                         by,
                         bz,
                         this.type.textures[this.data.intdata],
                         isEntity,
                         isAsItem);
                break;
            case BTRedstoneRepeaterOff:
            case BTRedstoneRepeaterOn:
            {
                Matrix rotateMat = Matrix.translate(-0.5f, -0.5f, -0.49f)
                                         .concat(Matrix.rotatey(Math.PI / 2.0
                                                 * (1 - this.data.orientation)))
                                         .concat(Matrix.translate(0.5f,
                                                                  0.5f,
                                                                  0.5f));
                internalDraw(rs,
                             0x3F,
                             Matrix.scale(1.0f, 1.0f / 8, 1.0f)
                                   .concat(rotateMat),
                             blockToWorld,
                             bx,
                             by,
                             bz,
                             this.type.textures[0],
                             false,
                             isEntity,
                             isAsItem);
                TextureHandle torchTexture;
                if(this.type == BlockType.BTRedstoneRepeaterOn)
                    torchTexture = BlockType.BTRedstoneTorchOn.textures[0];
                else
                    torchTexture = BlockType.BTRedstoneTorchOff.textures[0];
                Matrix tform;
                tform = getTorchTransform(4);
                tform = tform.concat(Matrix.scale(1.0f, 0.6f, 1.0f));
                tform = tform.concat(Matrix.translate(-0.5f, 0, -0.5f));
                tform = tform.concat(Matrix.translate(0.5f, 0, 3.0f / 16));
                tform = tform.concat(rotateMat);
                internalDraw(rs,
                             0x3F,
                             tform,
                             blockToWorld,
                             bx,
                             by,
                             bz,
                             torchTexture,
                             false,
                             isEntity,
                             isAsItem);
                if(!isEntity && !isAsItem
                        && redstoneRepeaterIsLatched(bx, by, bz))
                {
                    tform = Matrix.translate(-0.5f, -0.5f, -0.5f);
                    tform = tform.concat(Matrix.scale(1.0f, 1.0f / 8, 1.0f / 8));
                    tform = tform.concat(Matrix.translate(0.5f,
                                                          3.0f / 16,
                                                          7.0f / 16));
                    tform = tform.concat(rotateMat);
                    internalDraw(rs,
                                 0x3F,
                                 tform,
                                 blockToWorld,
                                 bx,
                                 by,
                                 bz,
                                 this.type.textures[1],
                                 false,
                                 isEntity,
                                 isAsItem);
                }
                else
                {
                    tform = getTorchTransform(4);
                    tform = tform.concat(Matrix.scale(1.0f, 0.6f, 1.0f));
                    tform = tform.concat(Matrix.translate(-0.5f, 0, -0.5f));
                    tform = tform.concat(Matrix.translate(0.5f,
                                                          0,
                                                          (5.0f + 2.0f * this.data.intdata) / 16));
                    tform = tform.concat(rotateMat);
                    internalDraw(rs,
                                 0x3F,
                                 tform,
                                 blockToWorld,
                                 bx,
                                 by,
                                 bz,
                                 torchTexture,
                                 false,
                                 isEntity,
                                 isAsItem);
                }
                break;
            }
            case BTLever:
            {
                internalDraw(rs,
                             0x3F,
                             getLeverTransform(this.data.orientation),
                             blockToWorld,
                             bx,
                             by,
                             bz,
                             this.type.textures[0],
                             false,
                             isEntity,
                             isAsItem);
                internalDraw(rs,
                             0x3F,
                             getLeverHandleTransform(this.data.orientation,
                                                     this.data.intdata != 0),
                             blockToWorld,
                             bx,
                             by,
                             bz,
                             this.type.textures[1],
                             false,
                             isEntity,
                             isAsItem);
                break;
            }
            case BTPiston:
            case BTStickyPiston:
            {
                Matrix rotateMat, tform = Matrix.translate(-0.5f, -0.5f, -0.5f);
                switch(getNegOrientation(this.data.orientation))
                {
                case 0: // -X
                    tform = tform.concat(Matrix.rotatez(-Math.PI / 2));
                    break;
                case 1: // -Z
                    tform = tform.concat(Matrix.rotatex(Math.PI / 2));
                    break;
                case 2: // +X
                    tform = tform.concat(Matrix.rotatez(Math.PI / 2));
                    break;
                case 3: // +Z
                    tform = tform.concat(Matrix.rotatex(-Math.PI / 2));
                    break;
                case 5: // +Y
                    tform = tform.concat(Matrix.rotatex(-Math.PI));
                    break;
                // case 4: // -Y
                default:
                    break;
                }
                rotateMat = tform.concat(Matrix.translate(0.5f, 0.5f, 0.5f));
                if(this.data.intdata == 0)
                {
                    internalDraw(rs,
                                 0x3F,
                                 rotateMat,
                                 blockToWorld,
                                 bx,
                                 by,
                                 bz,
                                 this.type.textures[0],
                                 false,
                                 isEntity,
                                 isAsItem);
                }
                else
                {
                    tform = Matrix.scale(1.0f, 0.75f, 1.0f).concat(rotateMat);
                    internalDraw(rs,
                                 0x3F,
                                 tform,
                                 blockToWorld,
                                 bx,
                                 by,
                                 bz,
                                 this.type.textures[1],
                                 false,
                                 isEntity,
                                 isAsItem);
                }
                break;
            }
            case BTPistonHead:
            case BTStickyPistonHead:
            {
                Matrix rotateMat, tform = Matrix.translate(-0.5f, -0.5f, -0.5f);
                switch(getNegOrientation(this.data.orientation))
                {
                case 0: // -X
                    tform = tform.concat(Matrix.rotatez(-Math.PI / 2));
                    break;
                case 1: // -Z
                    tform = tform.concat(Matrix.rotatex(Math.PI / 2));
                    break;
                case 2: // +X
                    tform = tform.concat(Matrix.rotatez(Math.PI / 2));
                    break;
                case 3: // +Z
                    tform = tform.concat(Matrix.rotatex(-Math.PI / 2));
                    break;
                case 5: // +Y
                    tform = tform.concat(Matrix.rotatex(-Math.PI));
                    break;
                // case 4: // -Y
                default:
                    break;
                }
                rotateMat = tform.concat(Matrix.translate(0.5f, 0.5f, 0.5f));
                tform = Matrix.scale(1.0f, 0.25f, 1.0f)
                              .concat(Matrix.translate(0, 0.75f, 0))
                              .concat(rotateMat);
                internalDraw(rs,
                             0x3F,
                             tform,
                             blockToWorld,
                             bx,
                             by,
                             bz,
                             this.type.textures[0],
                             false,
                             isEntity,
                             isAsItem);
                tform = Matrix.scale(2.0f / 16, 1.0f, 2.0f / 16)
                              .concat(Matrix.translate(-1.0f / 16 + 0.5f,
                                                       -0.25f,
                                                       -1.0f / 16 + 0.5f))
                              .concat(rotateMat);
                internalDraw(rs,
                             0x3F,
                             tform,
                             blockToWorld,
                             bx,
                             by,
                             bz,
                             this.type.textures[1],
                             false,
                             isEntity,
                             isAsItem);
                break;
            }
            case BTStonePressurePlate:
            case BTWoodPressurePlate:
            {
                internalDraw(rs,
                             0x3F,
                             Matrix.scale(1.0f, 1.0f / 16, 1.0f),
                             blockToWorld,
                             bx,
                             by,
                             bz,
                             this.type.textures[0],
                             false,
                             isEntity,
                             isAsItem);
                break;
            }
            default:
                break;
            }
            break;
        case BDTItem:
            drawItem(rs,
                     Matrix.identity(),
                     blockToWorld,
                     bx,
                     by,
                     bz,
                     this.type.textures[this.data.intdata],
                     isEntity,
                     isAsItem);
            break;
        case BDTSolid:
            drawSolid(rs, blockToWorld, bx, by, bz, false, isEntity, isAsItem);
            break;
        case BDTNone:
            break;
        case BDTTorch:
        {
            internalDraw(rs,
                         0x3F,
                         getTorchTransform(this.data.orientation),
                         blockToWorld,
                         bx,
                         by,
                         bz,
                         this.type.textures[this.data.intdata],
                         false,
                         isEntity,
                         isAsItem);
            break;
        }
        case BDTLiquid:
        {
            if(isEntity || isAsItem)
            {
                drawSolid(rs,
                          blockToWorld,
                          bx,
                          by,
                          bz,
                          true,
                          this.type.textures[this.type == BlockType.BTWater ? 1
                                  : 0],
                          isEntity,
                          isAsItem);
            }
            else
            {
                drawFluid(rs, blockToWorld, bx, by, bz, this.type.textures[0]);
            }
            break;
        }
        case BDTButton:
            internalDraw(rs,
                         0x3F,
                         getButtonTransform(this.data.orientation,
                                            this.data.intdata > 0),
                         blockToWorld,
                         bx,
                         by,
                         bz,
                         this.type.textures[0],
                         false,
                         isEntity,
                         isAsItem);
            break;
        case BDTSolidAllSides:
            drawSolid(rs, blockToWorld, bx, by, bz, true, isEntity, isAsItem);
            break;
        default:
            break;
        }
        return rs;
    }

    /** @return the type of the block */
    public BlockType getType()
    {
        return this.type;
    }

    /** @return the amount of direct sunlight. ranges from 0 to 15. */
    public int getSunlight()
    {
        return this.sunlight;
    }

    /** set the lighting of this block<BR/>
     * not thread safe
     * 
     * @param sunlight
     *            the amount of direct sunlight. limited from 0 to 15.
     * @param scatteredSunlight
     *            the amount of scattered sunlight. limited from the limited
     *            <code>sunlight</code> to 15.
     * @param light
     *            the amount of non-sun light. limited from
     *            <code>getEmitLight()</code> to 15.
     * @return true if the lighting of the block changed
     * @see #getEmitLight() */
    public boolean setLighting(int sunlight, int scatteredSunlight, int light)
    {
        int newSunlight = Math.max(0, Math.min(15, sunlight));
        int newScatteredSunlight = Math.max(newSunlight,
                                            Math.min(15, scatteredSunlight));
        int newLight = Math.max(0, Math.min(15, light));
        if(isOpaque())
        {
            newSunlight = 0;
            newScatteredSunlight = 0;
            newLight = 0;
        }
        else if(this.type == BlockType.BTWater)
        {
            newSunlight = newSunlight - 2;
            newScatteredSunlight = newScatteredSunlight - 1;
            newLight = newLight - 1;
        }
        boolean changed = false;
        newLight = Math.max(newLight, getEmitLight());
        if(this.sunlight != newSunlight
                || this.scatteredSunlight != newScatteredSunlight
                || this.light != newLight)
            changed = true;
        this.sunlight = newSunlight;
        this.scatteredSunlight = newScatteredSunlight;
        this.light = newLight;
        if(changed)
            this.lighting = null;
        return changed;
    }

    /** @return the amount of scattered sunlight. ranges from 0 to 15. */
    public int getScatteredSunlight()
    {
        return this.scatteredSunlight;
    }

    /** @return the amount of non-sun light. ranges from 0 to 15. */
    public int getLight()
    {
        return this.light;
    }

    /** @return the amount of light emitted by this block. ranges from 0 to 15. */
    public int getEmitLight()
    {
        int retval = this.type.getLight();
        if(retval < 0)
        {
            switch(this.type)
            {
            case BTFurnace:
                if(this.data.intdata > 0
                        && this.data.blockdata != BlockType.BTEmpty
                        && this.data.srccount > 0)
                    return 13;
                return 0;
            default:
                retval = 15;
                break;
            }
        }
        return retval;
    }

    /** copies the lighting of <code>rt</code> to this block.<BR/>
     * not thread safe
     * 
     * @param rt
     *            the block to copy the lighting from */
    public void copyLighting(Block rt)
    {
        setLighting(rt.sunlight, rt.scatteredSunlight, rt.light);
    }

    /** resets the lighting of this block */
    public void resetLighting()
    {
        setLighting(0, 0, 0);
    }

    /** @param sunlightFactor
     *            the amount that sunlight counts for. limited from 0 (none) to
     *            15 (full).
     * @return the amount of light in this block */
    public int getLighting(int sunlightFactor)
    {
        return Math.max((this.scatteredSunlight * sunlightFactor) / 15,
                        this.light);
    }

    /** gets the lighting array <BR/>
     * not thread safe
     * 
     * @param sunlightFactor
     *            used to check for the right array to return
     * @return the lighting array for <code>sunlightFactor</code> or null if
     *         there isn't one */
    public int[] getLightingArray(int sunlightFactor)
    {
        if(sunlightFactor != this.curSunlightFactor)
            return null;
        return this.lighting;
    }

    /** sets the lighting array <BR/>
     * not thread safe
     * 
     * @param lighting
     *            the new lighting array
     * @param sunlightFactor
     *            used to check for the right array to return in
     *            <code>getLightingArray</code>
     * @see #getLightingArray(int sunlightFactor) */
    public void setLightingArray(int lighting[], int sunlightFactor)
    {
        if(lighting != null && lighting.length != 8)
            throw new IllegalArgumentException("new lighting array is wrong length");
        this.lighting = lighting;
        this.curSunlightFactor = sunlightFactor;
    }

    /** @return true if this block generates particles
     * @see #generateParticles(int, int, int, double, double) */
    public boolean isParticleGenerate()
    {
        switch(this.type)
        {
        case BTFurnace:
            if(this.data.blockdata == BlockType.BTEmpty)
                return false;
            if(this.data.srccount <= 0)
                return false;
            if(this.data.intdata <= 0)
                return false;
            return true;
        default:
            return this.type.isParticleGenerate();
        }
    }

    /** called when this block needs to generate particles
     * 
     * @param bx
     *            block x coordinate
     * @param by
     *            block y coordinate
     * @param bz
     *            block z coordinate
     * @param lastTime
     *            time for last frame
     * @param curTime
     *            time for this frame
     * @see #isParticleGenerate() */
    public void generateParticles(int bx,
                                  int by,
                                  int bz,
                                  double lastTime,
                                  double curTime)
    {
        Vector blockOrigin = new Vector(bx, by, bz);
        switch(this.type)
        {
        case BTDeleteBlock:
        case BTSun:
        case BTMoon:
        case BTLast:
            return;
        case BTEmpty:
            return;
        case BTStone:
        case BTCobblestone:
        case BTGrass:
        case BTDirt:
        case BTSapling:
        case BTBedrock:
        case BTWater:
        case BTLava:
        case BTSand:
        case BTGravel:
        case BTWood:
        case BTLeaves:
        case BTGlass:
        case BTChest:
        case BTWorkbench:
            return;
        case BTFurnace:
        {
            if(this.data.blockdata == BlockType.BTEmpty)
                return;
            if(this.data.srccount <= 0)
                return;
            if(this.data.intdata <= 0)
                return;
            final float ParticlesPerSecond = 30;
            int count = (int)(Math.floor(curTime * ParticlesPerSecond) - Math.floor(lastTime
                    * ParticlesPerSecond));
            for(int i = 0; i < count; i++)
            {
                Entity p = Entity.NewParticle(blockOrigin.add(new Vector(World.fRand(0,
                                                                                     1),
                                                                         1.0f,
                                                                         World.fRand(0,
                                                                                     1))),
                                              ParticleType.SmokeAnim,
                                              World.vRand(1.0f));
                world.insertEntity(p);
            }
            return;
        }
        case BTPlank:
        case BTStick:
        case BTWoodPick:
        case BTStonePick:
        case BTWoodShovel:
        case BTStoneShovel:
        case BTRedstoneDustOff:
            return;
        case BTRedstoneDustOn:
        {
            Vector pos = blockOrigin.add(new Vector(0.5f, 0, 0.5f));
            final float ParticlesPerSecond = 3;
            int count = (int)(Math.floor(curTime * ParticlesPerSecond) - Math.floor(lastTime
                    * ParticlesPerSecond));
            for(int i = 0; i < count; i++)
            {
                Entity p = Entity.NewParticle(pos,
                                              ParticleType.RedstoneFire,
                                              World.vRand(0.1f));
                world.insertEntity(p);
            }
            return;
        }
        case BTRedstoneOre:
        case BTRedstoneBlock:
        case BTRedstoneTorchOff:
            return;
        case BTRedstoneTorchOn:
        {
            // Vector pos = getTorchTransform(this.data.orientation).apply(new
            // Vector(0.5f,
            // 1.0f,
            // 0.5f))
            // .add(blockOrigin);
            // final float ParticlesPerSecond = 3;
            // int count = (int)(Math.floor(curTime * ParticlesPerSecond) -
            // Math.floor(lastTime
            // * ParticlesPerSecond));
            // for(int i = 0; i < count; i++)
            // {
            // Entity p;
            // p = Entity.NewParticle(pos,
            // ParticleType.RedstoneFireAnim,
            // World.vRand(0.1f));
            // world.insertEntity(p);
            // p = Entity.NewParticle(pos,
            // ParticleType.SmokeAnim,
            // World.vRand(0.1f));
            // world.insertEntity(p);
            // }
            return;
        }
        case BTStoneButton:
        case BTWoodButton:
        case BTCoal:
        case BTIronIngot:
        case BTLapisLazuli:
        case BTGoldIngot:
        case BTDiamond:
        case BTEmerald:
        case BTCoalOre:
        case BTIronOre:
        case BTLapisLazuliOre:
        case BTGoldOre:
        case BTDiamondOre:
        case BTEmeraldOre:
            return;
        case BTTorch:
        {
            Vector pos = getTorchTransform(this.data.orientation).apply(new Vector(0.5f,
                                                                                   1.0f,
                                                                                   0.5f))
                                                                 .add(blockOrigin);
            {
                final float ParticlesPerSecond = 5;
                int count = (int)(Math.floor(curTime * ParticlesPerSecond) - Math.floor(lastTime
                        * ParticlesPerSecond));
                for(int i = 0; i < count; i++)
                {
                    Entity p;
                    p = Entity.NewParticle(pos,
                                           ParticleType.FireAnim,
                                           World.vRand(0.1f));
                    world.insertEntity(p);
                }
            }
            {
                final float ParticlesPerSecond = 0.5f;// was 5
                int count = (int)(Math.floor(curTime * ParticlesPerSecond) - Math.floor(lastTime
                        * ParticlesPerSecond));
                for(int i = 0; i < count; i++)
                {
                    Entity p;
                    p = Entity.NewParticle(pos,
                                           ParticleType.SmokeAnim,
                                           World.vRand(0.1f));
                    world.insertEntity(p);
                }
            }
            return;
        }
        case BTIronPick:
        case BTIronShovel:
        case BTGoldPick:
        case BTGoldShovel:
        case BTDiamondPick:
        case BTDiamondShovel:
        case BTLadder:
        case BTRedstoneRepeaterOff:
            return;
        case BTRedstoneRepeaterOn:
        {
            // Matrix rotateMat = Matrix.translate(-0.5f, -0.5f, -0.49f)
            // .concat(Matrix.rotatey(Math.PI / 2.0
            // * (1 - this.data.orientation)))
            // .concat(Matrix.translate(0.5f, 0.5f, 0.5f));
            // final float ParticlesPerSecond = 2;
            // int count = (int)(Math.floor(curTime * ParticlesPerSecond) -
            // Math.floor(lastTime
            // * ParticlesPerSecond));
            // {
            // Matrix tform = getTorchTransform(4);
            // tform = tform.concat(Matrix.scale(1.0f, 0.6f, 1.0f));
            // tform = tform.concat(Matrix.translate(-0.5f, 0, -0.5f));
            // tform = tform.concat(Matrix.translate(0.5f, 0, 3.0f / 16));
            // tform = tform.concat(rotateMat);
            // Vector pos = tform.apply(new Vector(0.5f, 1.0f, 0.5f))
            // .add(blockOrigin);
            // for(int i = 0; i < count; i++)
            // {
            // Entity p;
            // p = Entity.NewParticle(pos,
            // ParticleType.RedstoneFireAnim,
            // World.vRand(0.1f));
            // world.insertEntity(p);
            // p = Entity.NewParticle(pos,
            // ParticleType.SmokeAnim,
            // World.vRand(0.1f));
            // world.insertEntity(p);
            // }
            // }
            // {
            // Matrix tform = getTorchTransform(4);
            // tform = tform.concat(Matrix.scale(1.0f, 0.6f, 1.0f));
            // tform = tform.concat(Matrix.translate(-0.5f, 0, -0.5f));
            // tform = tform.concat(Matrix.translate(0.5f,
            // 0,
            // (5.0f + 2.0f * this.data.intdata) / 16));
            // tform = tform.concat(rotateMat);
            // Vector pos = tform.apply(new Vector(0.5f, 1.0f, 0.5f))
            // .add(blockOrigin);
            // for(int i = 0; i < count; i++)
            // {
            // Entity p;
            // p = Entity.NewParticle(pos,
            // ParticleType.RedstoneFireAnim,
            // World.vRand(0.1f));
            // world.insertEntity(p);
            // p = Entity.NewParticle(pos,
            // ParticleType.SmokeAnim,
            // World.vRand(0.1f));
            // world.insertEntity(p);
            // }
            // }
            return;
        }
        case BTLever:
        case BTObsidian:
        case BTPiston:
        case BTStickyPiston:
        case BTPistonHead:
        case BTStickyPistonHead:
        case BTSlime:
        case BTGunpowder:
        case BTTNT:
        case BTBlazeRod:
        case BTBlazePowder:
        case BTStonePressurePlate:
        case BTWoodPressurePlate:
            return;
        }
    }

    private boolean isLiquidSupported(int bx, int by, int bz)
    {
        Block ny = world.getBlockEval(bx, by - 1, bz);
        if(ny == null)
            return false;
        if(ny.getType() == BlockType.BTWater)
            return ny.data.intdata >= 8;
        if(ny.getType() == BlockType.BTLava)
            return ny.data.intdata >= 8;
        if(ny.getType() != BlockType.BTEmpty)
            return true;
        return false;
    }

    /** called to evaluate general moves
     * 
     * @param bx
     *            block x coordinate
     * @param by
     *            block y coordinate
     * @param bz
     *            block z coordinate
     * @return the block that this block changes to or null if it doesn't change */
    public Block move(int bx, int by, int bz)
    {
        switch(this.type)
        {
        case BTLast:
        case BTDeleteBlock:
        case BTMoon:
        case BTSun:
            return null;
        case BTBedrock:
            return null;
        case BTEmpty:
        {
            Block px = world.getBlockEval(bx + 1, by, bz);
            Block nx = world.getBlockEval(bx - 1, by, bz);
            Block py = world.getBlockEval(bx, by + 1, bz);
            // Block ny = world.getBlockEval(bx, by - 1, bz);
            Block pz = world.getBlockEval(bx, by, bz + 1);
            Block nz = world.getBlockEval(bx, by, bz - 1);
            int newSign = 1;
            if(!isLiquidSupported(bx, by, bz))
                newSign = -1;
            if(py != null && py.getType() == BlockType.BTWater)
            {
                return NewWater(7 * newSign);
            }
            if(py != null && py.getType() == BlockType.BTLava)
            {
                return NewLava(7 * newSign);
            }
            BlockType bt = BlockType.BTEmpty;
            int height = 1;
            if(nx != null
                    && (nx.getType() == BlockType.BTWater || nx.getType() == BlockType.BTLava)
                    && nx.data.intdata > height)
            {
                height = nx.data.intdata;
                bt = nx.getType();
            }
            if(px != null
                    && (px.getType() == BlockType.BTWater || px.getType() == BlockType.BTLava)
                    && px.data.intdata > height)
            {
                height = px.data.intdata;
                bt = px.getType();
            }
            if(nz != null
                    && (nz.getType() == BlockType.BTWater || nz.getType() == BlockType.BTLava)
                    && nz.data.intdata > height)
            {
                height = nz.data.intdata;
                bt = nz.getType();
            }
            if(pz != null
                    && (pz.getType() == BlockType.BTWater || pz.getType() == BlockType.BTLava)
                    && pz.data.intdata > height)
            {
                height = pz.data.intdata;
                bt = pz.getType();
            }
            height--;
            if(height > 6)
                height = 6;
            switch(bt)
            {
            case BTWater:
                return NewWater(height * newSign);
            case BTLava:
                return NewLava(height * newSign);
            default:
                return null;
            }
        }
        case BTChest:
        case BTCoal:
        case BTCoalOre:
        case BTCobblestone:
        case BTDiamond:
        case BTDiamondOre:
        case BTDiamondPick:
        case BTDiamondShovel:
        case BTDirt:
        case BTEmerald:
        case BTEmeraldOre:
            break;
        case BTFurnace:
        {
            int fuelleft = this.data.intdata;
            BlockType sourceblock = this.data.blockdata;
            int destcount = this.data.destcount;
            int srccount = this.data.srccount;
            if(fuelleft <= 0 || sourceblock == BlockType.BTEmpty
                    || srccount <= 0)
                return null;
            if(world.getCurTime() < this.data.runTime)
            {
                world.addTimedInvalidate(bx,
                                         by,
                                         bz,
                                         this.data.runTime - world.getCurTime());
                return null;
            }
            srccount--;
            destcount++;
            fuelleft--;
            return NewFurnace(fuelleft, sourceblock, srccount, destcount);
        }
        case BTGlass:
        case BTGoldIngot:
        case BTGoldOre:
        case BTGoldPick:
        case BTGoldShovel:
        case BTGrass:
        case BTGravel:
        case BTIronIngot:
        case BTIronOre:
        case BTIronPick:
        case BTIronShovel:
        case BTLapisLazuli:
        case BTLapisLazuliOre:
            break;
        case BTLava:
        case BTWater:
        {
            Block px = world.getBlockEval(bx + 1, by, bz);
            Block nx = world.getBlockEval(bx - 1, by, bz);
            Block py = world.getBlockEval(bx, by + 1, bz);
            // Block ny = world.getBlockEval(bx, by - 1, bz);
            Block pz = world.getBlockEval(bx, by, bz + 1);
            Block nz = world.getBlockEval(bx, by, bz - 1);
            int newSign = 1;
            if(!isLiquidSupported(bx, by, bz))
                newSign = -1;
            if(this.type == BlockType.BTWater
                    && Math.abs(this.data.intdata) >= 8)
            {
                if(py != null && py.getType() == BlockType.BTLava)
                    return NewStone();
                int value = 8 * newSign;
                if(value != this.data.intdata)
                    return NewWater(value);
                return null;
            }
            if(this.type == BlockType.BTLava
                    && Math.abs(this.data.intdata) >= 8)
            {
                if(py != null && py.getType() == BlockType.BTWater)
                    return NewObsidian();
                if(px != null && px.getType() == BlockType.BTWater
                        && px.getHeight() >= 1.0f)
                    return NewObsidian();
                if(nx != null && nx.getType() == BlockType.BTWater
                        && nx.getHeight() >= 1.0f)
                    return NewObsidian();
                if(pz != null && pz.getType() == BlockType.BTWater
                        && pz.getHeight() >= 1.0f)
                    return NewObsidian();
                if(nz != null && nz.getType() == BlockType.BTWater
                        && nz.getHeight() >= 1.0f)
                    return NewObsidian();
                int value = 8 * newSign;
                if(value != this.data.intdata)
                    return NewLava(value);
                return null;
            }
            if(this.type == BlockType.BTLava
                    && Math.abs(this.data.intdata) == 7)
            {
                if(nx != null && nx.getType() == BlockType.BTWater)
                    return NewCobblestone();
                if(px != null && px.getType() == BlockType.BTWater)
                    return NewCobblestone();
                if(nz != null && nz.getType() == BlockType.BTWater)
                    return NewCobblestone();
                if(pz != null && pz.getType() == BlockType.BTWater)
                    return NewCobblestone();
            }
            else if(this.type == BlockType.BTLava)
            {
                if(nx != null && nx.getType() == BlockType.BTWater
                        && nx.getHeight() > getHeight())
                    return NewCobblestone();
                if(px != null && px.getType() == BlockType.BTWater
                        && px.getHeight() > getHeight())
                    return NewCobblestone();
                if(nz != null && nz.getType() == BlockType.BTWater
                        && nz.getHeight() > getHeight())
                    return NewCobblestone();
                if(pz != null && pz.getType() == BlockType.BTWater
                        && pz.getHeight() > getHeight())
                    return NewCobblestone();
            }
            else if(this.type == BlockType.BTWater)
            {
                if(nx != null && nx.getType() == BlockType.BTLava
                        && nx.getHeight() > getHeight())
                    return NewStone();
                if(px != null && px.getType() == BlockType.BTLava
                        && px.getHeight() > getHeight())
                    return NewStone();
                if(nz != null && nz.getType() == BlockType.BTLava
                        && nz.getHeight() > getHeight())
                    return NewStone();
                if(pz != null && pz.getType() == BlockType.BTLava
                        && pz.getHeight() > getHeight())
                    return NewStone();
            }
            if(py != null && py.getType() == BlockType.BTWater)
            {
                if(this.type == BlockType.BTLava)
                {
                    return NewCobblestone();
                }
                if(this.type == py.getType()
                        && this.data.intdata == 7 * newSign)
                    return null;
                return NewWater(7 * newSign);
            }
            if(py != null && py.getType() == BlockType.BTLava)
            {
                if(this.type == BlockType.BTWater)
                {
                    return NewStone();
                }
                if(this.type == py.getType()
                        && this.data.intdata == 7 * newSign)
                    return null;
                return NewLava(7 * newSign);
            }
            BlockType bt = BlockType.BTEmpty;
            int height = 0;
            if(nx != null
                    && (nx.getType() == BlockType.BTWater || nx.getType() == BlockType.BTLava)
                    && nx.data.intdata > height)
            {
                height = nx.data.intdata;
                bt = nx.getType();
            }
            if(px != null
                    && (px.getType() == BlockType.BTWater || px.getType() == BlockType.BTLava)
                    && px.data.intdata > height)
            {
                height = px.data.intdata;
                bt = px.getType();
            }
            if(nz != null
                    && (nz.getType() == BlockType.BTWater || nz.getType() == BlockType.BTLava)
                    && nz.data.intdata > height)
            {
                height = nz.data.intdata;
                bt = nz.getType();
            }
            if(pz != null
                    && (pz.getType() == BlockType.BTWater || pz.getType() == BlockType.BTLava)
                    && pz.data.intdata > height)
            {
                height = pz.data.intdata;
                bt = pz.getType();
            }
            height--;
            if(height > 6)
                height = 6;
            if(height <= 0)
                bt = BlockType.BTEmpty;
            int value = height * newSign;
            if(this.type == BlockType.BTLava && bt == BlockType.BTWater)
            {
                return NewCobblestone();
            }
            if(this.type == BlockType.BTWater && bt == BlockType.BTStone)
            {
                return NewStone();
            }
            if(value == this.data.intdata && this.type == bt)
                return null;
            switch(bt)
            {
            case BTWater:
                return NewWater(height * newSign);
            case BTLava:
                return NewLava(height * newSign);
            default:
                return new Block();
            }
        }
        case BTLeaves:
        case BTPlank:
        case BTRedstoneBlock:
        case BTRedstoneDustOff:
        case BTRedstoneDustOn:
        case BTRedstoneOre:
        case BTRedstoneTorchOff:
        case BTRedstoneTorchOn:
        case BTSand:
            break;
        case BTSapling:
        {
            Block py = world.getBlockEval(bx, by + 1, bz);
            Block ny = world.getBlockEval(bx, by - 1, bz);
            if(ny.getType() != BlockType.BTDirt
                    && ny.getType() != BlockType.BTGrass)
            {
                if(this.data.runTime >= 0)
                {
                    Block retval = NewSapling();
                    retval.data.runTime = -1;
                    return retval;
                }
                return null;
            }
            if(py.getType() != BlockType.BTEmpty)
            {
                if(this.data.runTime >= 0)
                {
                    Block retval = NewSapling();
                    retval.data.runTime = -1;
                    return retval;
                }
                return null;
            }
            if(world.getLighting(bx + 0.5f, by + 0.5f, bz + 0.5f) <= 7 / 15.0)
            {
                if(this.data.runTime >= 0)
                {
                    Block retval = NewSapling();
                    retval.data.runTime = -1;
                    return retval;
                }
                world.addTimedInvalidate(bx, by, bz, 10);
                return null;
            }
            if(this.data.runTime < 0)
            {
                Block retval = NewSapling();
                return retval;
            }
            if(world.getCurTime() >= this.data.runTime)
            {
                Block retval = new Block();
                world.addNewTree(bx, by, bz, this);
                return retval;
            }
            world.addTimedInvalidate(bx, by, bz, world.getCurTime()
                    - this.data.runTime);
            return null;
        }
        case BTStick:
        case BTStone:
        case BTStoneButton:
        case BTStonePick:
        case BTStoneShovel:
        case BTTorch:
        case BTWood:
        case BTWoodButton:
        case BTWoodPick:
        case BTWoodShovel:
        case BTWorkbench:
        case BTLadder:
        case BTRedstoneRepeaterOff:
        case BTRedstoneRepeaterOn:
        case BTLever:
        case BTObsidian:
        case BTPiston:
        case BTStickyPiston:
        case BTPistonHead:
        case BTStickyPistonHead:
        case BTSlime:
        case BTGunpowder:
        case BTTNT:
        case BTBlazeRod:
        case BTBlazePowder:
        case BTStonePressurePlate:
        case BTWoodPressurePlate:
            break;
        }
        return null;
    }

    private boolean redstoneRepeaterIsLatched(int bx, int by, int bz)
    {
        for(int angle = -1; angle <= 1; angle += 2)
        {
            int o = getRotatedOrientation(5, angle, this.data.orientation);
            int dx = getOrientationDX(o);
            int dy = getOrientationDY(o);
            int dz = getOrientationDZ(o);
            Block b = world.getBlockEval(bx + dx, by + dy, bz + dz);
            if(b == null)
                continue;
            if(b.getType() != BlockType.BTRedstoneRepeaterOn)
                continue;
            if(getNegOrientation(b.data.orientation) != o)
                continue;
            return true;
        }
        return false;
    }

    /** explode this TNT block
     * 
     * @param bx
     *            x coordinate of this block
     * @param by
     *            y coordinate of this block
     * @param bz
     *            z coordinate of this block */
    public void TNTExplode(int bx, int by, int bz)
    {
        world.insertEntity(Entity.NewPrimedTNT(new Vector(bx, by, bz), 1));
        world.setBlock(bx, by, bz, new Block());
    }

    /** called to evaluate redstone moves
     * 
     * @param bx
     *            block x coordinate
     * @param by
     *            block y coordinate
     * @param bz
     *            block z coordinate
     * @return the block that this block changes to or null if it doesn't change */
    public Block redstoneMove(int bx, int by, int bz)
    {
        switch(this.type)
        {
        case BTLast:
        case BTDeleteBlock:
        case BTMoon:
        case BTSun:
            return null;
        case BTBedrock:
        case BTEmpty:
        case BTChest:
        case BTCoal:
        case BTCoalOre:
        case BTCobblestone:
        case BTDiamond:
        case BTDiamondOre:
        case BTDiamondPick:
        case BTDiamondShovel:
        case BTDirt:
        case BTEmerald:
        case BTEmeraldOre:
        case BTFurnace:
        case BTGlass:
        case BTGoldIngot:
        case BTGoldOre:
        case BTGoldPick:
        case BTGoldShovel:
        case BTGrass:
        case BTGravel:
        case BTIronIngot:
        case BTIronOre:
        case BTIronPick:
        case BTIronShovel:
        case BTLapisLazuli:
        case BTLapisLazuliOre:
        case BTLava:
        case BTWater:
        case BTLeaves:
        case BTPlank:
        case BTRedstoneBlock:
        case BTRedstoneDustOff:
        case BTRedstoneDustOn:
            return null;
        case BTWoodButton:
        case BTStoneButton:
            if(this.data.intdata > 0)
            {
                Block retval = new Block(this);
                retval.data.intdata--;
                return retval;
            }
            return null;
        case BTRedstoneTorchOff:
        case BTRedstoneTorchOn:
        {
            int input = getEvalRedstoneIOValue(bx
                                                       + getOrientationDX(this.data.orientation),
                                               by
                                                       + getOrientationDY(this.data.orientation),
                                               bz
                                                       + getOrientationDZ(this.data.orientation),
                                               getNegOrientation(this.data.orientation));
            if(input == REDSTONE_POWER_STRONG
                    || (input >= REDSTONE_POWER_WEAK_MIN && input <= REDSTONE_POWER_WEAK_MAX))
            {
                if(this.type == BlockType.BTRedstoneTorchOff)
                    return null;
                return NewRedstoneTorch(false, this.data.orientation);
            }
            if(this.type == BlockType.BTRedstoneTorchOn)
                return null;
            return NewRedstoneTorch(true, this.data.orientation);
        }
        case BTRedstoneOre:
        case BTSand:
        case BTSapling:
        case BTStick:
        case BTStone:
        case BTStonePick:
        case BTStoneShovel:
        case BTTorch:
        case BTWood:
        case BTWoodPick:
        case BTWoodShovel:
        case BTWorkbench:
        case BTLadder:
            return null;
        case BTRedstoneRepeaterOff:
        case BTRedstoneRepeaterOn:
        {
            boolean value = false;
            if(this.type == BlockType.BTRedstoneRepeaterOn)
                value = true;
            boolean changing = false;
            if(this.data.step > 0)
                changing = true;
            int input = getEvalRedstoneIOValue(bx
                                                       - getOrientationDX(this.data.orientation),
                                               by
                                                       - getOrientationDY(this.data.orientation),
                                               bz
                                                       - getOrientationDZ(this.data.orientation),
                                               this.data.orientation);
            boolean newvalue = false;
            if(input == REDSTONE_POWER_STRONG
                    || (input >= REDSTONE_POWER_WEAK_MIN && input <= REDSTONE_POWER_WEAK_MAX))
                newvalue = true;
            if(redstoneRepeaterIsLatched(bx, by, bz))
            {
                if(this.data.step == 0)
                    return null;
                return NewRedstoneRepeater(value,
                                           0,
                                           this.data.intdata,
                                           this.data.orientation);
            }
            if(value != newvalue)
                changing = true;
            if(!changing)
                return null;
            int newstep = this.data.step + 1;
            if(newstep >= this.data.intdata)
            {
                return NewRedstoneRepeater(!value,
                                           0,
                                           this.data.intdata,
                                           this.data.orientation);
            }
            return NewRedstoneRepeater(value,
                                       newstep,
                                       this.data.intdata,
                                       this.data.orientation);
        }
        case BTLever:
        case BTObsidian:
            return null;
        case BTPiston:
        case BTStickyPiston:
        {
            boolean isOn = false;
            for(int orientation = 0; orientation < 6; orientation++)
            {
                if(orientation == this.data.orientation)
                    continue;
                int dx = getOrientationDX(orientation);
                int dy = getOrientationDY(orientation);
                int dz = getOrientationDZ(orientation);
                int x = bx + dx, y = by + dy, z = bz + dz;
                int curPower = getEvalRedstoneIOValue(x,
                                                      y,
                                                      z,
                                                      getNegOrientation(orientation));
                if(curPower == REDSTONE_POWER_STRONG)
                {
                    isOn = true;
                    break;
                }
                if(curPower >= REDSTONE_POWER_WEAK_MIN
                        && curPower <= REDSTONE_POWER_WEAK_MAX)
                {
                    isOn = true;
                    break;
                }
            }
            if(isOn == (this.data.step != 0))
                return null;
            Block b = new Block(this);
            if(isOn)
                b.data.step = 1;
            else
                b.data.step = 0;
            return b;
        }
        case BTPistonHead:
        case BTStickyPistonHead:
        case BTSlime:
        case BTGunpowder:
            return null;
        case BTTNT:
        {
            boolean isOn = false;
            for(int orientation = 0; orientation < 6; orientation++)
            {
                int dx = getOrientationDX(orientation);
                int dy = getOrientationDY(orientation);
                int dz = getOrientationDZ(orientation);
                int x = bx + dx, y = by + dy, z = bz + dz;
                int curPower = getEvalRedstoneIOValue(x,
                                                      y,
                                                      z,
                                                      getNegOrientation(orientation));
                if(curPower == REDSTONE_POWER_STRONG)
                {
                    isOn = true;
                    break;
                }
                if(curPower >= REDSTONE_POWER_WEAK_MIN
                        && curPower <= REDSTONE_POWER_WEAK_MAX)
                {
                    isOn = true;
                    break;
                }
            }
            if(isOn)
            {
                world.insertEntity(Entity.NewPrimedTNT(new Vector(bx, by, bz),
                                                       1));
                return new Block();
            }
            return null;
        }
        case BTBlazeRod:
        case BTBlazePowder:
            return null;
        case BTStonePressurePlate:
        case BTWoodPressurePlate:
        {
            if(this.data.intdata != 0)
            {
                Block retval = new Block(this);
                retval.data.intdata = 0;
                return retval;
            }
        }
        }
        return null;
    }

    /** called to evaluate redstone dust moves
     * 
     * @param bx
     *            block x coordinate
     * @param by
     *            block y coordinate
     * @param bz
     *            block z coordinate
     * @return the block that this block changes to or null if it doesn't change */
    public Block redstoneDustMove(int bx, int by, int bz)
    {
        if(this.type != BlockType.BTRedstoneDustOff
                && this.type != BlockType.BTRedstoneDustOn)
            return null;
        int power = 0;
        int borientation = 0;
        for(int orientation = 0; orientation <= 5; orientation++)
        {
            int dx = getOrientationDX(orientation);
            int dy = getOrientationDY(orientation);
            int dz = getOrientationDZ(orientation);
            int x = bx + dx, y = by + dy, z = bz + dz;
            int curPower = getEvalRedstoneDustIOValue(x,
                                                      y,
                                                      z,
                                                      getNegOrientation(orientation));
            if(curPower == REDSTONE_POWER_STRONG)
                curPower = REDSTONE_POWER_WEAK_MAX;
            if(curPower >= REDSTONE_POWER_WEAK_MIN
                    && curPower <= REDSTONE_POWER_WEAK_MAX)
            {
                if(curPower > power)
                    power = curPower;
            }
            if(getOrientationDY(orientation) == 0
                    && curPower != REDSTONE_POWER_NONE)
            {
                borientation |= 1 << orientation;
            }
        }
        for(int orientation = 0; orientation <= 5; orientation++)
        {
            if(getOrientationDY(orientation) != 0)
                continue;
            int dx = getOrientationDX(orientation);
            int dz = getOrientationDZ(orientation);
            for(int dy = -1; dy <= 1; dy += 2)
            {
                int x = bx + dx, y = by + dy, z = bz + dz;
                Block cutBlock;
                if(dy < 0)
                    cutBlock = world.getBlockEval(x, by, z);
                else
                    cutBlock = world.getBlockEval(bx, by + 1, bz);
                Block b = world.getBlockEval(x, y, z);
                if(b == null)
                    continue;
                if(b.getType() != BlockType.BTRedstoneDustOff
                        && b.getType() != BlockType.BTRedstoneDustOn)
                    continue;
                if(cutBlock != null && cutBlock.getCutsRedstoneDust())
                    continue;
                if(dy < 0 && (borientation & (1 << orientation)) != 0)
                    continue;
                if(b.data.intdata > power)
                    power = b.data.intdata;
                if(dy > 0)
                    borientation |= 0x11 << orientation;
                else
                    borientation |= 1 << orientation;
            }
        }
        power--;
        if(power < 0)
            power = 0;
        if(power == this.data.intdata && this.data.orientation == borientation)
            return null;
        return NewRedstoneDust(power, borientation);
    }

    /** called to evaluate piston dust moves
     * 
     * @param bx
     *            block x coordinate
     * @param by
     *            block y coordinate
     * @param bz
     *            block z coordinate */
    public void pistonMove(int bx, int by, int bz)
    {
        if(this.type != BlockType.BTPiston
                && this.type != BlockType.BTStickyPiston
                && this.type != BlockType.BTPistonHead
                && this.type != BlockType.BTStickyPistonHead)
            return;
        boolean isOn = this.data.step != 0;
        if(isOn && this.data.intdata != 0)
            return;
        if(!isOn && this.data.intdata == 0)
            return;
        Block newBlock = new Block(this);
        newBlock.data.intdata = isOn ? 1 : 0;
        int dx = getOrientationDX(this.data.orientation);
        int dy = getOrientationDY(this.data.orientation);
        int dz = getOrientationDZ(this.data.orientation);
        int x = bx + dx, y = by + dy, z = bz + dz;
        if(!isOn) // if turning off
        {
            world.setBlock(bx, by, bz, newBlock);
            Block head = world.getBlockEval(x, y, z);
            if(head == null)
                return;
            Block pulledBlock = null;
            if(this.type == BlockType.BTStickyPiston)
            {
                pulledBlock = world.getBlockEval(x + dx, y + dy, z + dz);
                if(pulledBlock != null)
                {
                    PushType p = pulledBlock.getPushType();
                    if(p == PushType.Pushed)
                    {
                        world.setBlock(x + dx, y + dy, z + dz, new Block());
                    }
                    else
                    {
                        pulledBlock = null;
                    }
                }
            }
            if(pulledBlock == null)
                pulledBlock = new Block();
            world.setBlock(x, y, z, pulledBlock);
            return;
        }
        final int maxDist = 12;
        int pushDist = 0;
        while(true)
        {
            Block b = world.getBlockEval(x + dx * pushDist,
                                         y + dy * pushDist,
                                         z + dz * pushDist);
            PushType p = PushType.NonPushable;
            if(b != null)
                p = b.getPushType();
            if(p == PushType.DropAsEntity || p == PushType.Remove)
            {
                break;
            }
            else if(p != PushType.Pushed)
            {
                return;
            }
            if(++pushDist > maxDist)
                return;
        }
        world.setBlock(bx, by, bz, newBlock);
        Block nextBlock;
        if(this.type == BlockType.BTStickyPiston)
            nextBlock = NewStickyPistonHead(this.data.orientation);
        else
            nextBlock = NewPistonHead(this.data.orientation);
        for(int i = 0; i <= pushDist; i++, x += dx, y += dy, z += dz)
        {
            Block thisBlock = nextBlock;
            nextBlock = world.getBlockEval(x, y, z);
            if(i == pushDist)
            {
                PushType p = nextBlock.getPushType();
                if(p == PushType.DropAsEntity)
                    nextBlock.digBlock(x, y, z);
                players.push(x, y, z, dx, dy, dz);
            }
            world.setBlock(x, y, z, thisBlock);
        }
    }

    /** @author jacob how a pushed block responds */
    public static enum PushType
    {
        /** block can't be pushed */
        NonPushable, /** block is removed */
        Remove, /** block is dropped as entity (as if it had been dug) */
        DropAsEntity, /** block is pushed */
        Pushed;
    }

    /** @return what happens when this block is pushed */
    public PushType getPushType()
    {
        switch(this.type)
        {
        case BTDeleteBlock:
        case BTSun:
        case BTMoon:
        case BTLast:
            return PushType.NonPushable;
        case BTEmpty:
            return PushType.Remove;
        case BTStone:
        case BTCobblestone:
        case BTGrass:
        case BTDirt:
            return PushType.Pushed;
        case BTBedrock:
            return PushType.NonPushable;
        case BTSapling:
            return PushType.DropAsEntity;
        case BTWater:
        case BTLava:
        case BTLeaves:
            return PushType.Remove;
        case BTSand:
        case BTGravel:
        case BTWood:
        case BTGlass:
        case BTChest:
        case BTWorkbench:
        case BTFurnace:
            return PushType.Pushed;
        case BTPlank:
        case BTStick:
        case BTWoodPick:
        case BTStonePick:
        case BTWoodShovel:
        case BTStoneShovel:
        case BTRedstoneDustOff:
        case BTRedstoneDustOn:
            return PushType.DropAsEntity;
        case BTRedstoneOre:
        case BTRedstoneBlock:
        case BTCoalOre:
        case BTIronOre:
        case BTLapisLazuliOre:
        case BTGoldOre:
        case BTDiamondOre:
        case BTEmeraldOre:
            return PushType.Pushed;
        case BTRedstoneTorchOff:
        case BTRedstoneTorchOn:
        case BTStoneButton:
        case BTWoodButton:
        case BTTorch:
        case BTCoal:
        case BTIronIngot:
        case BTLapisLazuli:
        case BTGoldIngot:
        case BTDiamond:
        case BTEmerald:
        case BTIronPick:
        case BTIronShovel:
        case BTGoldPick:
        case BTGoldShovel:
        case BTDiamondPick:
        case BTDiamondShovel:
        case BTLadder:
        case BTRedstoneRepeaterOff:
        case BTRedstoneRepeaterOn:
        case BTLever:
            return PushType.DropAsEntity;
        case BTObsidian:
            return PushType.NonPushable;
        case BTPiston:
        case BTStickyPiston:
            if(this.data.intdata != 0)
                return PushType.NonPushable;
            return PushType.Pushed;
        case BTPistonHead:
        case BTStickyPistonHead:
            return PushType.NonPushable;
        case BTSlime:
        case BTGunpowder:
            return PushType.DropAsEntity;
        case BTTNT:
            return PushType.Pushed;
        case BTBlazeRod:
        case BTBlazePowder:
        case BTStonePressurePlate:
        case BTWoodPressurePlate:
            return PushType.DropAsEntity;
        }
        return PushType.NonPushable;
    }

    private boolean isBlockSupported(int bx, int by, int bz, int orientation)
    {
        Block px = world.getBlockEval(bx + 1, by, bz);
        Block nx = world.getBlockEval(bx - 1, by, bz);
        Block py = world.getBlockEval(bx, by + 1, bz);
        Block ny = world.getBlockEval(bx, by - 1, bz);
        Block pz = world.getBlockEval(bx, by, bz + 1);
        Block nz = world.getBlockEval(bx, by, bz - 1);
        Block supportingblock;
        switch(orientation)
        {
        case 0:
            supportingblock = nx;
            break;
        case 1:
            supportingblock = nz;
            break;
        case 2:
            supportingblock = px;
            break;
        case 3:
            supportingblock = pz;
            break;
        case 5:
            supportingblock = py;
            break;
        default:
            supportingblock = ny;
            break;
        }
        if(supportingblock != null && !supportingblock.isSupporting())
            return false;
        return true;
    }

    /** @param bx
     *            block x coordinate
     * @param by
     *            block y coordinate
     * @param bz
     *            block z coordinate
     * @return the entity that this block changes to or null if it doesn't
     *         change */
    public Entity evalBlockToEntity(int bx, int by, int bz)
    {
        Entity retval = null;
        switch(this.type)
        {
        case BTDeleteBlock:
        case BTSun:
        case BTMoon:
        case BTLast:
            break;
        case BTEmpty:
        case BTStone:
        case BTCobblestone:
        case BTGrass:
        case BTDirt:
        case BTSapling:
        case BTBedrock:
        case BTWater:
        case BTLava:
            return null;
        case BTSand:
        case BTGravel:
        {
            if(!isBlockSupported(bx, by, bz, 4))
                return Entity.NewFallingBlock(new Vector(bx, by, bz), this.type);
            return null;
        }
        case BTWood:
        case BTLeaves:
        case BTGlass:
        case BTChest:
        case BTWorkbench:
        case BTFurnace:
        case BTPlank:
        case BTStick:
        case BTWoodPick:
        case BTStonePick:
        case BTWoodShovel:
        case BTStoneShovel:
            return null;
        case BTRedstoneDustOff:
        case BTRedstoneDustOn:
        {
            if(!isBlockSupported(bx, by, bz, 4))
                return Entity.NewBlock(new Vector(0.5f + bx,
                                                  0.5f + by,
                                                  0.5f + bz),
                                       BlockType.BTRedstoneDustOff,
                                       World.vRand(0.1f));
            return null;
        }
        case BTRedstoneOre:
        case BTRedstoneBlock:
            return null;
        case BTRedstoneTorchOff:
        case BTRedstoneTorchOn:
        {
            if(!isBlockSupported(bx, by, bz, this.data.orientation))
                return Entity.NewBlock(new Vector(0.5f + bx,
                                                  0.5f + by,
                                                  0.5f + bz),
                                       BlockType.BTRedstoneTorchOff,
                                       World.vRand(0.1f));
            return null;
        }
        case BTStoneButton:
        case BTWoodButton:
        case BTTorch:
        {
            if(!isBlockSupported(bx, by, bz, this.data.orientation))
                return Entity.NewBlock(new Vector(0.5f + bx,
                                                  0.5f + by,
                                                  0.5f + bz),
                                       this.type,
                                       World.vRand(0.1f));
            return null;
        }
        case BTCoal:
        case BTCoalOre:
        case BTIronIngot:
        case BTIronOre:
        case BTLapisLazuli:
        case BTLapisLazuliOre:
        case BTGoldIngot:
        case BTGoldOre:
        case BTDiamond:
        case BTDiamondOre:
        case BTEmerald:
        case BTEmeraldOre:
        case BTIronPick:
        case BTIronShovel:
        case BTGoldPick:
        case BTGoldShovel:
        case BTDiamondPick:
        case BTDiamondShovel:
        case BTSlime:
        case BTGunpowder:
        case BTTNT:
        case BTBlazeRod:
        case BTBlazePowder:
            return null;
        case BTLadder:
        {
            if(!isBlockSupported(bx, by, bz, this.data.orientation))
                return Entity.NewBlock(new Vector(0.5f + bx,
                                                  0.5f + by,
                                                  0.5f + bz),
                                       this.type,
                                       World.vRand(0.1f));
            return null;
        }
        case BTRedstoneRepeaterOff:
        case BTRedstoneRepeaterOn:
        case BTStonePressurePlate:
        case BTWoodPressurePlate:
        {
            if(!isBlockSupported(bx, by, bz, 4))
                return Entity.NewBlock(new Vector(0.5f + bx,
                                                  0.5f + by,
                                                  0.5f + bz),
                                       this.type,
                                       World.vRand(0.1f));
            return null;
        }
        case BTLever:
        {
            if(!isBlockSupported(bx, by, bz, this.data.orientation))
                return Entity.NewBlock(new Vector(0.5f + bx,
                                                  0.5f + by,
                                                  0.5f + bz),
                                       this.type,
                                       World.vRand(0.1f));
            return null;
        }
        case BTObsidian:
        case BTPiston:
        case BTStickyPiston:
        case BTPistonHead:
        case BTStickyPistonHead:
            return null;
        }
        return retval;
    }

    @Override
    public void move()
    {
    }

    private float rayIntersectsBlock(Vector origin,
                                     Vector direction,
                                     Matrix tform)
    {
        Matrix invtform = tform.invert();
        Vector orig = invtform.apply(origin);
        Vector dir = invtform.apply(direction)
                             .sub(invtform.apply(new Vector(0)));
        final float eps = 1e-4f;
        if(Math.abs(dir.x) < eps)
            dir.x = eps;
        if(Math.abs(dir.y) < eps)
            dir.y = eps;
        if(Math.abs(dir.z) < eps)
            dir.z = eps;
        Vector invdir = new Vector(1.0f / dir.x, 1.0f / dir.y, 1.0f / dir.z);
        Vector destpos = new Vector(1.0f);
        if(dir.x < 0)
            destpos.x = 0;
        if(dir.y < 0)
            destpos.y = 0;
        if(dir.z < 0)
            destpos.z = 0;
        Vector vt = new Vector();
        vt.x = (destpos.x - orig.x) * invdir.x;
        vt.y = (destpos.y - orig.y) * invdir.y;
        vt.z = (destpos.z - orig.z) * invdir.z;
        Vector hx, hy, hz;
        hx = dir.mul(vt.x).add(orig);
        hy = dir.mul(vt.y).add(orig);
        hz = dir.mul(vt.z).add(orig);
        if(hx.x >= -eps && hx.x <= 1 + eps && hx.y >= -eps && hx.y <= 1 + eps
                && hx.z >= -eps && hx.z <= 1 + eps)
            return vt.x;
        if(hy.x >= -eps && hy.x <= 1 + eps && hy.y >= -eps && hy.y <= 1 + eps
                && hy.z >= -eps && hy.z <= 1 + eps)
            return vt.y;
        if(hz.x >= -eps && hz.x <= 1 + eps && hz.y >= -eps && hz.y <= 1 + eps
                && hz.z >= -eps && hz.z <= 1 + eps)
            return vt.z;
        return -1;
    }

    @SuppressWarnings("unused")
    private int getRayEnterSide(Vector origin, Vector direction)
    {
        Vector dir = new Vector(direction);
        Vector orig = new Vector(origin);
        final float eps = 1e-4f;
        if(Math.abs(dir.x) < eps)
            dir.x = eps;
        if(Math.abs(dir.y) < eps)
            dir.y = eps;
        if(Math.abs(dir.z) < eps)
            dir.z = eps;
        Vector invdir = new Vector(1.0f / dir.x, 1.0f / dir.y, 1.0f / dir.z);
        Vector destpos = new Vector(0.0f);
        if(dir.x < 0)
            destpos.x = 1;
        if(dir.y < 0)
            destpos.y = 1;
        if(dir.z < 0)
            destpos.z = 1;
        Vector vt = new Vector();
        vt.x = (destpos.x - orig.x) * invdir.x;
        vt.y = (destpos.y - orig.y) * invdir.y;
        vt.z = (destpos.z - orig.z) * invdir.z;
        Vector hx, hy, hz;
        hx = dir.mul(vt.x).add(orig);
        hy = dir.mul(vt.y).add(orig);
        hz = dir.mul(vt.z).add(orig);
        if(hx.x >= -eps && hx.x <= 1 + eps && hx.y >= -eps && hx.y <= 1 + eps
                && hx.z >= -eps && hx.z <= 1 + eps)
        {
            if(dir.x < 0)
                return 2;
            return 0;
        }
        if(hy.x >= -eps && hy.x <= 1 + eps && hy.y >= -eps && hy.y <= 1 + eps
                && hy.z >= -eps && hy.z <= 1 + eps)
        {
            if(dir.y < 0)
                return 5;
            return 4;
        }
        if(hz.x >= -eps && hz.x <= 1 + eps && hz.y >= -eps && hz.y <= 1 + eps
                && hz.z >= -eps && hz.z <= 1 + eps)
        {
            if(dir.z < 0)
                return 3;
            return 1;
        }
        return -1;
    }

    private int getRayExitSide(Vector origin, Vector direction)
    {
        Vector dir = new Vector(direction);
        Vector orig = new Vector(origin);
        final float eps = 1e-4f;
        if(Math.abs(dir.x) < eps)
            dir.x = eps;
        if(Math.abs(dir.y) < eps)
            dir.y = eps;
        if(Math.abs(dir.z) < eps)
            dir.z = eps;
        Vector invdir = new Vector(1.0f / dir.x, 1.0f / dir.y, 1.0f / dir.z);
        Vector destpos = new Vector(1.0f);
        if(dir.x < 0)
            destpos.x = 0;
        if(dir.y < 0)
            destpos.y = 0;
        if(dir.z < 0)
            destpos.z = 0;
        Vector vt = new Vector();
        vt.x = (destpos.x - orig.x) * invdir.x;
        vt.y = (destpos.y - orig.y) * invdir.y;
        vt.z = (destpos.z - orig.z) * invdir.z;
        Vector hx, hy, hz;
        hx = dir.mul(vt.x).add(orig);
        hy = dir.mul(vt.y).add(orig);
        hz = dir.mul(vt.z).add(orig);
        if(hx.x >= -eps && hx.x <= 1 + eps && hx.y >= -eps && hx.y <= 1 + eps
                && hx.z >= -eps && hx.z <= 1 + eps)
        {
            if(dir.x < 0)
                return 0;
            return 2;
        }
        if(hy.x >= -eps && hy.x <= 1 + eps && hy.y >= -eps && hy.y <= 1 + eps
                && hy.z >= -eps && hy.z <= 1 + eps)
        {
            if(dir.y < 0)
                return 4;
            return 5;
        }
        if(hz.x >= -eps && hz.x <= 1 + eps && hz.y >= -eps && hz.y <= 1 + eps
                && hz.z >= -eps && hz.z <= 1 + eps)
        {
            if(dir.z < 0)
                return 1;
            return 3;
        }
        return -1;
    }

    private float getRayExitDist(Vector origin, Vector direction)
    {
        Vector dir = new Vector(direction);
        Vector orig = new Vector(origin);
        final float eps = 1e-4f;
        if(Math.abs(dir.x) < eps)
            dir.x = eps;
        if(Math.abs(dir.y) < eps)
            dir.y = eps;
        if(Math.abs(dir.z) < eps)
            dir.z = eps;
        Vector invdir = new Vector(1.0f / dir.x, 1.0f / dir.y, 1.0f / dir.z);
        Vector destpos = new Vector(1.0f);
        if(dir.x < 0)
            destpos.x = 0;
        if(dir.y < 0)
            destpos.y = 0;
        if(dir.z < 0)
            destpos.z = 0;
        Vector vt = new Vector();
        vt.x = (destpos.x - orig.x) * invdir.x;
        vt.y = (destpos.y - orig.y) * invdir.y;
        vt.z = (destpos.z - orig.z) * invdir.z;
        Vector hx, hy, hz;
        hx = dir.mul(vt.x).add(orig);
        hy = dir.mul(vt.y).add(orig);
        hz = dir.mul(vt.z).add(orig);
        if(hx.x >= -eps && hx.x <= 1 + eps && hx.y >= -eps && hx.y <= 1 + eps
                && hx.z >= -eps && hx.z <= 1 + eps)
        {
            return vt.x;
        }
        if(hy.x >= -eps && hy.x <= 1 + eps && hy.y >= -eps && hy.y <= 1 + eps
                && hy.z >= -eps && hy.z <= 1 + eps)
        {
            return vt.y;
        }
        if(hz.x >= -eps && hz.x <= 1 + eps && hz.y >= -eps && hz.y <= 1 + eps
                && hz.z >= -eps && hz.z <= 1 + eps)
        {
            return vt.z;
        }
        return -1;
    }

    /** @return the block's height */
    public float getHeight()
    {
        switch(this.type)
        {
        case BTDeleteBlock:
        case BTSun:
        case BTMoon:
        case BTLast:
            return 0;
        case BTEmpty:
            return 1;
        case BTStone:
        case BTCobblestone:
        case BTGrass:
        case BTDirt:
        case BTSapling:
        case BTBedrock:
            return 1;
        case BTWater:
        case BTLava:
            if(this.data.intdata > 7 || this.data.intdata < -7)
                return 1;
            return (Math.abs(this.data.intdata) + 1) / 8.0f;
        case BTSand:
        case BTGravel:
        case BTWood:
        case BTLeaves:
        case BTGlass:
        case BTChest:
        case BTWorkbench:
        case BTFurnace:
        case BTPlank:
        case BTStick:
        case BTWoodPick:
        case BTStonePick:
        case BTWoodShovel:
        case BTStoneShovel:
        case BTRedstoneDustOff:
        case BTRedstoneDustOn:
        case BTRedstoneOre:
        case BTRedstoneBlock:
        case BTRedstoneTorchOff:
        case BTRedstoneTorchOn:
        case BTStoneButton:
        case BTWoodButton:
        case BTCoal:
        case BTCoalOre:
        case BTIronIngot:
        case BTIronOre:
        case BTLapisLazuli:
        case BTLapisLazuliOre:
        case BTGoldIngot:
        case BTGoldOre:
        case BTDiamond:
        case BTDiamondOre:
        case BTEmerald:
        case BTEmeraldOre:
        case BTTorch:
        case BTIronPick:
        case BTIronShovel:
        case BTGoldPick:
        case BTGoldShovel:
        case BTDiamondPick:
        case BTDiamondShovel:
        case BTLadder:
            return 1;
        case BTRedstoneRepeaterOff:
        case BTRedstoneRepeaterOn:
            return 1.0f / 8;
        case BTLever:
        case BTObsidian:
        case BTPiston:
        case BTStickyPiston:
        case BTPistonHead:
        case BTStickyPistonHead:
        case BTSlime:
        case BTGunpowder:
        case BTTNT:
        case BTBlazeRod:
        case BTBlazePowder:
        case BTStonePressurePlate:
        case BTWoodPressurePlate:
            return 1;
        }
        return 0;
    }

    /** checks if a ray intersects this block
     * 
     * @param dir
     *            the direction of the ray
     * @param invdir
     *            the component-wise inverse of the ray direction
     * @param pos
     *            the origin of the ray
     * @param hitpos
     *            the place where the ray hits the unit box this block is in
     * @return >= 0 if the ray intersects this block */
    public float rayIntersects(Vector dir,
                               Vector invdir,
                               Vector pos,
                               Vector hitpos)
    {
        switch(this.type)
        {
        case BTDeleteBlock:
        case BTSun:
        case BTMoon:
        case BTLast:
            return -1;
        case BTEmpty:
            return -1;
        case BTStone:
        case BTCobblestone:
        case BTGrass:
        case BTDirt:
            return 0;
        case BTSapling:
            return 0;
        case BTBedrock:
            return 0;
        case BTWater:
        case BTLava:
        {
            float height = getHeight();
            if(hitpos.y <= height)
                return 0;
            if(dir.y > 1)
                return -1;
            float t = (height - pos.y) * invdir.y;
            Vector p = pos.add(dir.mul(t));
            if(p.x < 0 || p.x > 1 || p.z < 0 || p.z > 1)
                return -1;
            return t;
        }
        case BTSand:
        case BTGravel:
        case BTWood:
        case BTLeaves:
        case BTGlass:
        case BTChest:
        case BTWorkbench:
        case BTFurnace:
        case BTPlank:
        case BTStick:
        case BTWoodPick:
        case BTStonePick:
        case BTWoodShovel:
        case BTStoneShovel:
            return 0;
        case BTRedstoneDustOff:
        case BTRedstoneDustOn:
        {
            int exitside = getRayExitSide(hitpos, dir);
            float exitDist = getRayExitDist(hitpos, dir);
            switch(exitside)
            {
            case 0:
                if((this.data.orientation & 0x10) != 0)
                    return exitDist;
                break;
            case 1:
                if((this.data.orientation & 0x20) != 0)
                    return exitDist;
                break;
            case 2:
                if((this.data.orientation & 0x40) != 0)
                    return exitDist;
                break;
            case 3:
                if((this.data.orientation & 0x80) != 0)
                    return exitDist;
                break;
            case 4:
                return exitDist;
            default:
                break;
            }
            return -1;
        }
        case BTRedstoneOre:
        case BTRedstoneBlock:
            return 0;
        case BTRedstoneTorchOff:
        case BTRedstoneTorchOn:
        case BTTorch:
        {
            return rayIntersectsBlock(hitpos,
                                      dir,
                                      getTorchTransform(this.data.orientation));
        }
        case BTStoneButton:
        case BTWoodButton:
        {
            return rayIntersectsBlock(hitpos,
                                      dir,
                                      getButtonTransform(this.data.orientation,
                                                         this.data.intdata > 0));
        }
        case BTCoal:
        case BTCoalOre:
        case BTIronIngot:
        case BTIronOre:
        case BTLapisLazuli:
        case BTLapisLazuliOre:
        case BTGoldIngot:
        case BTGoldOre:
        case BTDiamond:
        case BTDiamondOre:
        case BTEmerald:
        case BTEmeraldOre:
        case BTIronPick:
        case BTIronShovel:
        case BTGoldPick:
        case BTGoldShovel:
        case BTDiamondPick:
        case BTDiamondShovel:
            return 0;
        case BTLadder:
            if(getRayExitSide(hitpos, dir) == this.data.orientation)
                return getRayExitDist(hitpos, dir);
            return -1;
        case BTRedstoneRepeaterOff:
        case BTRedstoneRepeaterOn:
            return rayIntersectsBlock(hitpos,
                                      dir,
                                      Matrix.scale(1.0f, 1.0f / 8, 1.0f));
        case BTLever:
            return rayIntersectsBlock(hitpos,
                                      dir,
                                      getLeverTransform(this.data.orientation));
        case BTObsidian:
        case BTPiston:
        case BTStickyPiston:
        case BTPistonHead:
        case BTStickyPistonHead:
        case BTSlime:
        case BTGunpowder:
        case BTTNT:
        case BTBlazeRod:
        case BTBlazePowder:
            return 0;
        case BTStonePressurePlate:
        case BTWoodPressurePlate:
            if(getRayExitSide(hitpos, dir) == 4)
                return getRayExitDist(hitpos, dir);
            return -1;
        }
        return -1;
    }

    /** makes a new block from <code>bt</code>
     * 
     * @param bt
     *            the type of the new block
     * @param orientation
     *            the orientation of the new block
     * @return the new block or null if <code>bt == BlockType.BTEmpty</code> */
    public static Block make(BlockType bt, int orientation)
    {
        return bt.make(orientation);
    }

    /** makes a new block from <code>bt</code>
     * 
     * @param bt
     *            the type of the new block
     * @return the new block or null if <code>bt == BlockType.BTEmpty</code> */
    public static Block make(BlockType bt)
    {
        return make(bt, -1);
    }

    /** @param rs
     *            the rendering stream
     * @param blockToWorld
     *            matrix to transform block coordinates to world coordinates
     * @param img
     *            image to draw
     * @return <code>rs</code> */
    public static RenderingStream drawImgAsEntity(RenderingStream rs,
                                                  Matrix blockToWorld,
                                                  TextureHandle img)
    {
        return drawItem(rs,
                        Matrix.translate(0.0f, 0.0f, 0.5f),
                        blockToWorld,
                        0,
                        0,
                        0,
                        img,
                        true,
                        false);
    }

    /** @param rs
     *            the rendering stream
     * @param blockToWorld
     *            matrix to transform block coordinates to world coordinates
     * @param img
     *            image to draw
     * @param minu
     *            the minimum u texture coordinate
     * @param minv
     *            the minimum v texture coordinate
     * @param maxu
     *            the maximum u texture coordinate
     * @param maxv
     *            the maximum v texture coordinate
     * @return <code>rs</code> */
    public static RenderingStream drawImgAsEntity(RenderingStream rs,
                                                  Matrix blockToWorld,
                                                  TextureHandle img,
                                                  float minu,
                                                  float minv,
                                                  float maxu,
                                                  float maxv)
    {
        return drawItem(rs,
                        Matrix.translate(0.0f, 0.0f, 0.5f),
                        blockToWorld,
                        0,
                        0,
                        0,
                        img,
                        true,
                        false,
                        minu,
                        minv,
                        maxu,
                        maxv);
    }

    /** draw this block as an entity
     * 
     * @param rs
     *            the rendering stream
     * @param blockToWorld
     *            matrix to transform block coordinates to world coordinates
     * @return <code>rs</code> */
    public RenderingStream
        drawAsEntity(RenderingStream rs, Matrix blockToWorld)
    {
        switch(this.type)
        {
        case BTDeleteBlock:
        case BTSun:
        case BTMoon:
        case BTLast:
            return rs;
        case BTEmpty:
            return rs;
        case BTStone:
        case BTCobblestone:
        case BTGrass:
        case BTDirt:
            draw(rs, blockToWorld, true, false);
            return rs;
        case BTSapling:
            draw(rs, blockToWorld, true, false);
            return rs;
        case BTBedrock:
        case BTWater:
        case BTLava:
        case BTSand:
        case BTGravel:
        case BTWood:
        case BTLeaves:
        case BTGlass:
        case BTChest:
        case BTWorkbench:
        case BTFurnace:
            draw(rs, blockToWorld, true, false);
            return rs;
        case BTPlank:
        case BTStick:
        case BTWoodPick:
        case BTStonePick:
        case BTWoodShovel:
        case BTStoneShovel:
        {
            drawImgAsEntity(rs,
                            blockToWorld,
                            this.type.textures[this.data.intdata]);
            return rs;
        }
        case BTRedstoneDustOff:
        case BTRedstoneDustOn:
        {
            Matrix tform = blockToWorld;
            tform = Matrix.translate(0.5f, 0.5f, 0.5f).concat(tform);
            tform = Matrix.rotatex(Math.PI / 2).concat(tform);
            tform = Matrix.translate(-0.5f, -0.5f, -0.5f).concat(tform);
            Block b = NewRedstoneDust(0, 0);
            b.draw(rs, tform, true, false);
            return rs;
        }
        case BTRedstoneOre:
        case BTRedstoneBlock:
            draw(rs, blockToWorld, true, false);
            return rs;
        case BTRedstoneTorchOff:
        case BTRedstoneTorchOn:
        {
            Block b = NewRedstoneTorch(false, 1);
            b.draw(rs, Matrix.scale(1.42857f).concat(blockToWorld), true, false);
            return rs;
        }
        case BTStoneButton:
        case BTWoodButton:
        {
            Matrix tform = blockToWorld;
            tform = Matrix.translate(0.5f, 0.5f, 0.5f).concat(tform);
            tform = Matrix.rotatex(Math.PI / 2).concat(tform);
            tform = Matrix.translate(-0.5f, -0.5f, -0.5f).concat(tform);
            Block b = new Block(this);
            b.draw(rs, tform, true, false);
            return rs;
        }
        case BTCoal:
        case BTIronIngot:
        case BTLapisLazuli:
        case BTGoldIngot:
        case BTDiamond:
        case BTEmerald:
        {
            drawImgAsEntity(rs,
                            blockToWorld,
                            this.type.textures[this.data.intdata]);
            return rs;
        }
        case BTCoalOre:
        case BTIronOre:
        case BTLapisLazuliOre:
        case BTGoldOre:
        case BTDiamondOre:
        case BTEmeraldOre:
            draw(rs, blockToWorld, true, false);
            return rs;
        case BTTorch:
        {
            Block b = NewTorch(1);
            b.draw(rs, Matrix.scale(1.42857f).concat(blockToWorld), true, false);
            return rs;
        }
        case BTIronPick:
        case BTIronShovel:
        case BTGoldPick:
        case BTGoldShovel:
        case BTDiamondPick:
        case BTDiamondShovel:
        case BTLadder:
        case BTSlime:
        {
            drawImgAsEntity(rs,
                            blockToWorld,
                            this.type.textures[this.data.intdata]);
            return rs;
        }
        case BTRedstoneRepeaterOff:
        case BTRedstoneRepeaterOn:
        {
            Block b = NewRedstoneRepeater(false, 0, 1, 0);
            b.draw(rs,
                   Matrix.translate(0, 0.5f, 0).concat(blockToWorld),
                   true,
                   false);
            return rs;
        }
        case BTStonePressurePlate:
        case BTWoodPressurePlate:
        {
            draw(rs,
                 Matrix.translate(0, 0.5f, 0).concat(blockToWorld),
                 true,
                 false);
            return rs;
        }
        case BTLever:
        {
            Block b = NewLever(false, 4);
            b.draw(rs, blockToWorld, true, false);
            return rs;
        }
        case BTObsidian:
        case BTPiston:
        case BTStickyPiston:
        case BTPistonHead:
        case BTStickyPistonHead:
            draw(rs, blockToWorld, true, false);
            return rs;
        case BTGunpowder:
            drawImgAsEntity(rs,
                            blockToWorld,
                            this.type.textures[this.data.intdata]);
            return rs;
        case BTTNT:
            draw(rs, blockToWorld, true, false);
            return rs;
        case BTBlazeRod:
        case BTBlazePowder:
            drawImgAsEntity(rs,
                            blockToWorld,
                            this.type.textures[this.data.intdata]);
            return rs;
        }
        return rs;
    }

    /** draws as an item
     * 
     * @param rs
     *            the rendering stream
     * @param blockToWorld
     *            the matrix that transforms block coordinates to world
     *            coordinates
     * @return <code>rs</code> */
    public RenderingStream drawAsItem(RenderingStream rs, Matrix blockToWorld)
    {
        switch(this.type)
        {
        case BTDeleteBlock:
        case BTSun:
        case BTMoon:
            draw(rs, blockToWorld, false, true);
            return rs;
        case BTLast:
            return rs;
        case BTEmpty:
            return rs;
        case BTStone:
        case BTCobblestone:
        case BTGrass:
        case BTDirt:
        case BTSapling:
        case BTBedrock:
        case BTWater:
        case BTLava:
        case BTSand:
        case BTGravel:
        case BTWood:
        case BTLeaves:
        case BTGlass:
        case BTChest:
        case BTWorkbench:
        case BTFurnace:
        case BTPlank:
        case BTStick:
        case BTWoodPick:
        case BTStonePick:
        case BTWoodShovel:
        case BTStoneShovel:
        {
            Block b = new Block(this);
            b.setLighting(0, 0, 15);
            b.draw(rs, blockToWorld, false, true);
            return rs;
        }
        case BTRedstoneDustOff:
        case BTRedstoneDustOn:
        {
            Matrix tform = blockToWorld;
            tform = Matrix.translate(0.5f, 0.5f, 0.5f).concat(tform);
            tform = Matrix.rotatex(Math.PI / 2).concat(tform);
            tform = Matrix.translate(-0.5f, -0.5f, -0.5f).concat(tform);
            Block b = NewRedstoneDust(0, 0);
            b.setLighting(0, 0, 15);
            b.draw(rs, tform, false, true);
            return rs;
        }
        case BTRedstoneOre:
        case BTRedstoneBlock:
        {
            Block b = new Block(this);
            b.setLighting(0, 0, 15);
            b.draw(rs, blockToWorld, false, true);
            return rs;
        }
        case BTRedstoneTorchOff:
        case BTRedstoneTorchOn:
        {
            Block b = NewRedstoneTorch(false, 1);
            b.setLighting(0, 0, 15);
            b.draw(rs, blockToWorld, false, true);
            return rs;
        }
        case BTStoneButton:
        case BTWoodButton:
        {
            Matrix tform = blockToWorld;
            tform = Matrix.translate(0.5f, 0.5f, 0.5f).concat(tform);
            tform = Matrix.rotatex(Math.PI / 2).concat(tform);
            tform = Matrix.translate(-0.5f, -0.5f, -0.5f).concat(tform);
            Block b = new Block(this);
            b.setLighting(0, 0, 15);
            b.draw(rs, tform, false, true);
            return rs;
        }
        case BTCoal:
        case BTCoalOre:
        case BTIronIngot:
        case BTIronOre:
        case BTLapisLazuli:
        case BTLapisLazuliOre:
        case BTGoldIngot:
        case BTGoldOre:
        case BTDiamond:
        case BTDiamondOre:
        case BTEmerald:
        case BTEmeraldOre:
        {
            Block b = new Block(this);
            b.setLighting(0, 0, 15);
            b.draw(rs, blockToWorld, false, true);
            return rs;
        }
        case BTTorch:
        {
            Block b = NewTorch(1);
            b.setLighting(0, 0, 15);
            b.draw(rs, blockToWorld, false, true);
            return rs;
        }
        case BTIronPick:
        case BTIronShovel:
        case BTGoldPick:
        case BTGoldShovel:
        case BTDiamondPick:
        case BTDiamondShovel:
        {
            Block b = new Block(this);
            b.setLighting(0, 0, 15);
            b.draw(rs, blockToWorld, false, true);
            return rs;
        }
        case BTLadder:
        {
            Block b = NewLadder(1);
            b.setLighting(0, 0, 15);
            b.draw(rs, blockToWorld, false, true);
            return rs;
        }
        case BTRedstoneRepeaterOff:
        case BTRedstoneRepeaterOn:
        {
            Block b = NewRedstoneRepeater(false, 0, 1, 0);
            b.setLighting(0, 0, 15);
            Matrix tform = blockToWorld;
            tform = Matrix.translate(0.5f, 0.5f, 0.5f).concat(tform);
            tform = Matrix.rotatex(Math.PI / 2).concat(tform);
            tform = Matrix.translate(-0.5f, -0.5f, -0.5f).concat(tform);
            b.draw(rs, tform, false, true);
            return rs;
        }
        case BTStonePressurePlate:
        case BTWoodPressurePlate:
        {
            Block b = new Block(this);
            b.setLighting(0, 0, 15);
            Matrix tform = blockToWorld;
            tform = Matrix.translate(0.5f, 0.5f, 0.5f).concat(tform);
            tform = Matrix.rotatex(Math.PI / 2).concat(tform);
            tform = Matrix.translate(-0.5f, -0.5f, -0.5f).concat(tform);
            b.draw(rs, tform, false, true);
            return rs;
        }
        case BTLever:
        {
            Block b = NewLever(false, 1);
            b.setLighting(0, 0, 15);
            b.draw(rs, blockToWorld, false, true);
            return rs;
        }
        case BTObsidian:
        case BTPiston:
        case BTStickyPiston:
        case BTPistonHead:
        case BTStickyPistonHead:
        case BTSlime:
        case BTGunpowder:
        case BTTNT:
        case BTBlazeRod:
        case BTBlazePowder:
        {
            Block b = new Block(this);
            b.setLighting(0, 0, 15);
            b.draw(rs, blockToWorld, false, true);
            return rs;
        }
        }
        return rs;
    }

    /** a reduction returned by <code>Block.reduce</code>
     * 
     * @author jacob
     * @see Block#reduce(BlockType[] array, int arraySize) */
    public static final class ReduceDescriptor
    {
        /** the blocks' type */
        public final BlockType b;
        /** the blocks count */
        public final int count;

        /** @param b
         *            the new blocks' type
         * @param count
         *            the number of new blocks */
        public ReduceDescriptor(BlockType b, int count)
        {
            this.b = b;
            this.count = count;
        }

        /** creates an empty <code>ReduceDescriptor</code> */
        public ReduceDescriptor()
        {
            this(BlockType.BTEmpty, 0);
        }

        /** @return true if this reduction failed */
        public boolean isEmpty()
        {
            if(this.b == BlockType.BTEmpty)
                return true;
            return false;
        }
    }

    private static class ReduceStruct
    {
        public final BlockType array[];
        public final int size;
        public final ReduceDescriptor retval;

        public ReduceStruct(BlockType array[], int size, BlockType b, int count)
        {
            this.array = array;
            this.size = size;
            this.retval = new ReduceDescriptor(b, count);
        }
    }

    private static final ReduceStruct reduceArray[] = new ReduceStruct[]
    {
        new Block.ReduceStruct(new BlockType[]
        {
            BlockType.BTPlank,
            BlockType.BTPlank,
            BlockType.BTPlank,
            BlockType.BTPlank,
            BlockType.BTEmpty,
            BlockType.BTPlank,
            BlockType.BTPlank,
            BlockType.BTPlank,
            BlockType.BTPlank
        }, 3, BlockType.BTChest, 1),
        new ReduceStruct(new BlockType[]
        {
            BlockType.BTCobblestone,
            BlockType.BTCobblestone,
            BlockType.BTCobblestone,
            BlockType.BTCobblestone,
            BlockType.BTEmpty,
            BlockType.BTCobblestone,
            BlockType.BTCobblestone,
            BlockType.BTCobblestone,
            BlockType.BTCobblestone
        }, 3, BlockType.BTFurnace, 1),
        new ReduceStruct(new BlockType[]
        {
            BlockType.BTPlank,
            BlockType.BTPlank,
            BlockType.BTPlank,
            BlockType.BTEmpty,
            BlockType.BTStick,
            BlockType.BTEmpty,
            BlockType.BTEmpty,
            BlockType.BTStick,
            BlockType.BTEmpty
        }, 3, BlockType.BTWoodPick, 1),
        new ReduceStruct(new BlockType[]
        {
            BlockType.BTCobblestone,
            BlockType.BTCobblestone,
            BlockType.BTCobblestone,
            BlockType.BTEmpty,
            BlockType.BTStick,
            BlockType.BTEmpty,
            BlockType.BTEmpty,
            BlockType.BTStick,
            BlockType.BTEmpty
        }, 3, BlockType.BTStonePick, 1),
        new ReduceStruct(new BlockType[]
        {
            BlockType.BTPlank,
            BlockType.BTEmpty,
            BlockType.BTEmpty,
            BlockType.BTStick,
            BlockType.BTEmpty,
            BlockType.BTEmpty,
            BlockType.BTStick,
            BlockType.BTEmpty,
            BlockType.BTEmpty
        }, 3, BlockType.BTWoodShovel, 1),
        new ReduceStruct(new BlockType[]
        {
            BlockType.BTCobblestone,
            BlockType.BTEmpty,
            BlockType.BTEmpty,
            BlockType.BTStick,
            BlockType.BTEmpty,
            BlockType.BTEmpty,
            BlockType.BTStick,
            BlockType.BTEmpty,
            BlockType.BTEmpty
        }, 3, BlockType.BTStoneShovel, 1),
        new ReduceStruct(new BlockType[]
        {
            BlockType.BTPlank,
            BlockType.BTPlank,
            BlockType.BTPlank,
            BlockType.BTPlank
        }, 2, BlockType.BTWorkbench, 1),
        new ReduceStruct(new BlockType[]
        {
            BlockType.BTWood
        }, 1, BlockType.BTPlank, 4),
        new ReduceStruct(new BlockType[]
        {
            BlockType.BTPlank,
            BlockType.BTEmpty,
            BlockType.BTPlank,
            BlockType.BTEmpty
        }, 2, BlockType.BTStick, 4),
        new ReduceStruct(new BlockType[]
        {
            BlockType.BTRedstoneBlock
        }, 1, BlockType.BTRedstoneDustOff, 9),
        new ReduceStruct(new BlockType[]
        {
            BlockType.BTCobblestone
        }, 1, BlockType.BTStoneButton, 1),
        new ReduceStruct(new BlockType[]
        {
            BlockType.BTPlank
        }, 1, BlockType.BTWoodButton, 1),
        new ReduceStruct(new BlockType[]
        {
            BlockType.BTRedstoneDustOff,
            BlockType.BTRedstoneDustOff,
            BlockType.BTRedstoneDustOff,
            BlockType.BTRedstoneDustOff,
            BlockType.BTRedstoneDustOff,
            BlockType.BTRedstoneDustOff,
            BlockType.BTRedstoneDustOff,
            BlockType.BTRedstoneDustOff,
            BlockType.BTRedstoneDustOff
        }, 3, BlockType.BTRedstoneBlock, 1),
        new ReduceStruct(new BlockType[]
        {
            BlockType.BTRedstoneDustOff,
            BlockType.BTEmpty,
            BlockType.BTStick,
            BlockType.BTEmpty
        }, 2, BlockType.BTRedstoneTorchOff, 1),
        new ReduceStruct(new BlockType[]
        {
            BlockType.BTCoal,
            BlockType.BTEmpty,
            BlockType.BTStick,
            BlockType.BTEmpty
        }, 2, BlockType.BTTorch, 4),
        new ReduceStruct(new BlockType[]
        {
            BlockType.BTIronIngot,
            BlockType.BTIronIngot,
            BlockType.BTIronIngot,
            BlockType.BTEmpty,
            BlockType.BTStick,
            BlockType.BTEmpty,
            BlockType.BTEmpty,
            BlockType.BTStick,
            BlockType.BTEmpty
        }, 3, BlockType.BTIronPick, 1),
        new ReduceStruct(new BlockType[]
        {
            BlockType.BTIronIngot,
            BlockType.BTEmpty,
            BlockType.BTEmpty,
            BlockType.BTStick,
            BlockType.BTEmpty,
            BlockType.BTEmpty,
            BlockType.BTStick,
            BlockType.BTEmpty,
            BlockType.BTEmpty
        }, 3, BlockType.BTIronShovel, 1),
        new ReduceStruct(new BlockType[]
        {
            BlockType.BTGoldIngot,
            BlockType.BTGoldIngot,
            BlockType.BTGoldIngot,
            BlockType.BTEmpty,
            BlockType.BTStick,
            BlockType.BTEmpty,
            BlockType.BTEmpty,
            BlockType.BTStick,
            BlockType.BTEmpty
        }, 3, BlockType.BTGoldPick, 1),
        new ReduceStruct(new BlockType[]
        {
            BlockType.BTGoldIngot,
            BlockType.BTEmpty,
            BlockType.BTEmpty,
            BlockType.BTStick,
            BlockType.BTEmpty,
            BlockType.BTEmpty,
            BlockType.BTStick,
            BlockType.BTEmpty,
            BlockType.BTEmpty
        }, 3, BlockType.BTGoldShovel, 1),
        new ReduceStruct(new BlockType[]
        {
            BlockType.BTDiamond,
            BlockType.BTDiamond,
            BlockType.BTDiamond,
            BlockType.BTEmpty,
            BlockType.BTStick,
            BlockType.BTEmpty,
            BlockType.BTEmpty,
            BlockType.BTStick,
            BlockType.BTEmpty
        }, 3, BlockType.BTDiamondPick, 1),
        new ReduceStruct(new BlockType[]
        {
            BlockType.BTDiamond,
            BlockType.BTEmpty,
            BlockType.BTEmpty,
            BlockType.BTStick,
            BlockType.BTEmpty,
            BlockType.BTEmpty,
            BlockType.BTStick,
            BlockType.BTEmpty,
            BlockType.BTEmpty
        }, 3, BlockType.BTDiamondShovel, 1),
        new ReduceStruct(new BlockType[]
        {
            BlockType.BTStick,
            BlockType.BTEmpty,
            BlockType.BTStick,
            BlockType.BTStick,
            BlockType.BTStick,
            BlockType.BTStick,
            BlockType.BTStick,
            BlockType.BTEmpty,
            BlockType.BTStick
        }, 3, BlockType.BTLadder, 3),
        new ReduceStruct(new BlockType[]
        {
            BlockType.BTEmpty,
            BlockType.BTEmpty,
            BlockType.BTEmpty,
            BlockType.BTRedstoneTorchOff,
            BlockType.BTRedstoneDustOff,
            BlockType.BTRedstoneTorchOff,
            BlockType.BTStone,
            BlockType.BTStone,
            BlockType.BTStone
        }, 3, BlockType.BTRedstoneRepeaterOff, 1),
        new ReduceStruct(new BlockType[]
        {
            BlockType.BTPlank,
            BlockType.BTPlank,
            BlockType.BTPlank,
            BlockType.BTCobblestone,
            BlockType.BTIronIngot,
            BlockType.BTCobblestone,
            BlockType.BTCobblestone,
            BlockType.BTRedstoneDustOff,
            BlockType.BTCobblestone
        }, 3, BlockType.BTPiston, 1),
        new ReduceStruct(new BlockType[]
        {
            BlockType.BTStick,
            BlockType.BTEmpty,
            BlockType.BTCobblestone,
            BlockType.BTEmpty
        }, 2, BlockType.BTLever, 1),
        new ReduceStruct(new BlockType[]
        {
            BlockType.BTSlime,
            BlockType.BTEmpty,
            BlockType.BTPiston,
            BlockType.BTEmpty
        }, 2, BlockType.BTStickyPiston, 1),
        new ReduceStruct(new BlockType[]
        {
            BlockType.BTGunpowder,
            BlockType.BTSand,
            BlockType.BTGunpowder,
            BlockType.BTSand,
            BlockType.BTGunpowder,
            BlockType.BTSand,
            BlockType.BTGunpowder,
            BlockType.BTSand,
            BlockType.BTGunpowder
        }, 3, BlockType.BTTNT, 1),
        new ReduceStruct(new BlockType[]
        {
            BlockType.BTBlazeRod
        }, 1, BlockType.BTBlazePowder, 2),
        new ReduceStruct(new BlockType[]
        {
            BlockType.BTEmpty,
            BlockType.BTEmpty,
            BlockType.BTStone,
            BlockType.BTStone
        }, 2, BlockType.BTStonePressurePlate, 1),
        new ReduceStruct(new BlockType[]
        {
            BlockType.BTEmpty,
            BlockType.BTEmpty,
            BlockType.BTPlank,
            BlockType.BTPlank
        }, 2, BlockType.BTWoodPressurePlate, 1),
    };
    private static final int reduceCount = reduceArray.length;

    /** find a reduction
     * 
     * @param array
     *            the block array. <code>array.length</code> must be more than
     *            <code>arraySize * arraySize</code>
     * @param arraySize
     *            the block dimensions
     * @return the reduction results. never returns <code>null</code>.
     * @see ReduceDescriptor */
    public static ReduceDescriptor reduce(BlockType[] array, int arraySize)
    {
        int minx = arraySize, maxx = 0, miny = arraySize, maxy = 0;
        for(int x = 0; x < arraySize; x++)
        {
            for(int y = 0; y < arraySize; y++)
            {
                if(array[x + y * arraySize] != BlockType.BTEmpty)
                {
                    if(minx > x)
                        minx = x;
                    if(maxx < x)
                        maxx = x;
                    if(miny > y)
                        miny = y;
                    if(maxy < y)
                        maxy = y;
                }
            }
        }
        for(int i = 0; i < reduceCount; i++)
        {
            boolean matches = true;
            if(maxx - minx >= reduceArray[i].size)
                matches = false;
            if(maxy - miny >= reduceArray[i].size)
                matches = false;
            if(!matches)
                continue;
            for(int x = 0; x < reduceArray[i].size; x++)
            {
                for(int y = 0; y < reduceArray[i].size; y++)
                {
                    BlockType rb = reduceArray[i].array[x
                            + (reduceArray[i].size - y - 1)
                            * reduceArray[i].size];
                    if(rb != BlockType.BTEmpty)
                    {
                        if(x > maxx - minx || y > maxy - miny)
                        {
                            matches = false;
                            break;
                        }
                        if(array[(x + minx) + arraySize * (y + miny)] != rb)
                        {
                            matches = false;
                            break;
                        }
                    }
                    else
                    {
                        if(x <= maxx - minx && y <= maxy - miny)
                        {
                            if(array[(x + minx) + arraySize * (y + miny)] != BlockType.BTEmpty)
                            {
                                matches = false;
                                break;
                            }
                        }
                    }
                }
                if(!matches)
                    break;
            }
            if(matches)
            {
                return reduceArray[i].retval;
            }
        }
        return new ReduceDescriptor();
    }

    /** @param bt
     *            type to look up for
     * @return the number of blocks of type <code>bt</code> in this chest block */
    public int chestGetBlockTypeCount(BlockType bt)
    {
        assert this.type == BlockType.BTChest && this.data.BlockCounts != null
                && this.data.BlockCounts.length == BlockType.Count : "illegal block state";
        if(bt == BlockType.BTEmpty)
            return 0;
        int index = bt.value;
        if(index < 1 || index >= BlockType.Count)
            return 0;
        return this.data.BlockCounts[index];
    }

    /** add a block to this chest block<BR/>
     * not thread safe
     * 
     * @param bt
     *            the type of the block to add */
    public void chestAddBlock(BlockType bt)
    {
        assert this.type == BlockType.BTChest && this.data.BlockCounts != null
                && this.data.BlockCounts.length == BlockType.Count : "illegal block state";
        if(bt == BlockType.BTEmpty)
            return;
        int index = bt.value;
        if(index < 1 || index >= BlockType.Count)
            return;
        this.data.BlockCounts[index]++;
    }

    /** remove a block from this chest block <BR/>
     * not thread safe
     * 
     * @param bt
     *            the type to remove
     * @return the removed type or <code>BlockType.BTEmpty</code> if there's
     *         none left */
    public BlockType chestRemoveBlock(BlockType bt)
    {
        assert this.type == BlockType.BTChest && this.data.BlockCounts != null
                && this.data.BlockCounts.length == BlockType.Count : "illegal block state";
        if(bt == BlockType.BTEmpty)
            return BlockType.BTEmpty;
        int index = bt.value;
        if(index < 1 || index >= BlockType.Count)
            return BlockType.BTEmpty;
        if(this.data.BlockCounts[index] <= 0)
            return BlockType.BTEmpty;
        this.data.BlockCounts[index]--;
        return bt;
    }

    /** @return the number of blocks already smelted */
    public int furnaceGetDestBlockCount()
    {
        assert this.type == BlockType.BTFurnace : "illegal block state";
        return this.data.destcount;
    }

    /** @return the number of blocks left to smelt */
    public int furnaceGetSrcBlockCount()
    {
        return this.data.srccount;
    }

    /** @return the type of block being smelted */
    public BlockType furnaceGetSrcBlockType()
    {
        return this.data.blockdata;
    }

    /** @return the type of block being smelted into */
    public BlockType furnaceGetDestBlockType()
    {
        if(this.data.blockdata == BlockType.BTEmpty)
            return BlockType.BTEmpty;
        return this.data.blockdata.getSmeltResult();
    }

    /** @return the amount of fuel left in this furnace */
    public int furnaceGetFuelLeft()
    {
        return this.data.intdata;
    }

    /** add more fuel to furnace
     * 
     * @param bt
     *            block to add to this furnace */
    public void furnaceAddFire(BlockType bt)
    {
        this.data.intdata += bt.getBurnTime();
    }

    /** add a block to smelt to this furnace
     * 
     * @param bt
     *            the type of block to smelt
     * @return true if <code>bt</code> is smeltable */
    public boolean furnaceAddBlock(BlockType bt)
    {
        if(this.data.blockdata != BlockType.BTEmpty
                && bt != this.data.blockdata)
            return false;
        if(!bt.isSmeltable())
            return false;
        this.data.blockdata = bt;
        this.data.srccount++;
        return true;
    }

    /** remove a smelted block from this furnace
     * 
     * @return the type of the smelted block or <code>BlockType.BTEmpty</code>
     *         if this furnace is empty */
    public BlockType furnaceRemoveBlock()
    {
        if(this.data.destcount > 1)
        {
            this.data.destcount--;
            return this.data.blockdata.getSmeltResult();
        }
        else if(this.data.destcount == 1)
        {
            BlockType retval = this.data.blockdata.getSmeltResult();
            if(this.data.srccount <= 0)
                this.data.blockdata = BlockType.BTEmpty;
            this.data.destcount = 0;
            return retval;
        }
        return BlockType.BTEmpty;
    }

    /** @return if this furnace is burning */
    public boolean furnaceIsBurning()
    {
        if(this.data.blockdata == BlockType.BTEmpty)
            return false;
        if(this.data.srccount <= 0)
            return false;
        if(this.data.intdata <= 0)
            return false;
        return true;
    }

    /** @return true if this block needs to be broken to dig it out */
    public boolean getNeedBreakToDig()
    {
        return this.type.getNeedBreakToDig();
    }

    /** @return the hardness of this block */
    public int getHardness()
    {
        return this.type.getHardness();
    }

    /** @return true if this is an item */
    public boolean isItem()
    {
        return this.type.isItem();
    }

    private Vector solidAdjustPlayerPosition(Vector position_in,
                                             float height,
                                             float distLimit)
    {
        Vector position = new Vector(position_in);
        float playerMinY = position.y - (Player.PlayerHeight - 1.0f);
        if(position.x > -distLimit && position.x < 1 + distLimit
                && position.y > -distLimit && playerMinY < height + distLimit
                && position.z > -distLimit && position.z < 1 + distLimit)
            return null;
        return position;
    }

    /** adjust the player's position if it's too close to this block
     * 
     * @param position
     *            the position to adjust
     * @param distLimit
     *            the closest that <code>position</code> can be to this block
     * @return the new position or null if <code>position</code> is inside this
     *         block */
    public Vector adjustPlayerPosition(Vector position, float distLimit)
    {
        switch(this.type)
        {
        case BTDeleteBlock:
        case BTSun:
        case BTMoon:
        case BTLast:
            return null;
        case BTEmpty:
            return position;
        case BTStone:
        case BTCobblestone:
        case BTGrass:
        case BTDirt:
        case BTSapling:
        case BTBedrock:
            return solidAdjustPlayerPosition(position, 1, distLimit);
        case BTWater:
            return position;
        case BTLava:
            if(this.data.intdata > 7 || this.data.intdata < -7)
                return solidAdjustPlayerPosition(position, 1, distLimit);
            return solidAdjustPlayerPosition(position, getHeight(), distLimit);
        case BTSand:
        case BTGravel:
        case BTWood:
        case BTLeaves:
        case BTGlass:
        case BTChest:
        case BTWorkbench:
        case BTFurnace:
            return solidAdjustPlayerPosition(position, 1, distLimit);
        case BTPlank:
        case BTStick:
        case BTWoodPick:
        case BTStonePick:
        case BTWoodShovel:
        case BTStoneShovel:
        case BTRedstoneDustOff:
        case BTRedstoneDustOn:
            return position;
        case BTRedstoneOre:
        case BTRedstoneBlock:
            return solidAdjustPlayerPosition(position, 1, distLimit);
        case BTRedstoneTorchOff:
        case BTRedstoneTorchOn:
        case BTStoneButton:
        case BTWoodButton:
            return position;
        case BTCoal:
            return position;
        case BTCoalOre:
            return solidAdjustPlayerPosition(position, 1, distLimit);
        case BTIronIngot:
            return position;
        case BTIronOre:
            return solidAdjustPlayerPosition(position, 1, distLimit);
        case BTLapisLazuli:
            return position;
        case BTLapisLazuliOre:
            return solidAdjustPlayerPosition(position, 1, distLimit);
        case BTGoldIngot:
            return position;
        case BTGoldOre:
            return solidAdjustPlayerPosition(position, 1, distLimit);
        case BTDiamond:
            return position;
        case BTDiamondOre:
            return solidAdjustPlayerPosition(position, 1, distLimit);
        case BTEmerald:
            return position;
        case BTEmeraldOre:
            return solidAdjustPlayerPosition(position, 1, distLimit);
        case BTTorch:
        case BTIronPick:
        case BTIronShovel:
        case BTGoldPick:
        case BTGoldShovel:
        case BTDiamondPick:
        case BTDiamondShovel:
        case BTLadder:
            return position;
        case BTRedstoneRepeaterOff:
        case BTRedstoneRepeaterOn:
            return solidAdjustPlayerPosition(position, getHeight(), distLimit);
        case BTLever:
            return position;
        case BTObsidian:
        case BTPiston:
        case BTStickyPiston:
        case BTPistonHead:
        case BTStickyPistonHead:
            return solidAdjustPlayerPosition(position, getHeight(), distLimit);
        case BTSlime:
        case BTGunpowder:
            return position;
        case BTTNT:
            return solidAdjustPlayerPosition(position, getHeight(), distLimit);
        case BTBlazeRod:
        case BTBlazePowder:
        case BTStonePressurePlate:
        case BTWoodPressurePlate:
            return position;
        }
        return null;
    }

    /** @return this block's orientation */
    public int getOrientation()
    {
        return this.data.orientation;
    }

    /** @return if this block is translucent */
    public boolean isTranslucent()
    {
        if(this.type == BlockType.BTWater)
            return true;
        return false;
    }

    /** @return true if this block can support entities */
    public boolean isSupporting()
    {
        return this.type.isSupporting();
    }

    /** @return true if this block can be dug */
    public boolean canDig()
    {
        switch(this.type)
        {
        case BTDeleteBlock:
        case BTSun:
        case BTMoon:
        case BTLast:
            return false;
        case BTEmpty:
        case BTBedrock:
            return false;
        case BTChest:
        case BTCoalOre:
        case BTCoal:
        case BTCobblestone:
        case BTDiamondPick:
        case BTDiamond:
        case BTDiamondShovel:
        case BTDirt:
        case BTEmerald:
        case BTFurnace:
        case BTGlass:
        case BTGoldIngot:
        case BTGoldPick:
        case BTGoldShovel:
        case BTGoldOre:
        case BTGravel:
        case BTIronIngot:
        case BTIronOre:
        case BTIronPick:
        case BTIronShovel:
        case BTLapisLazuli:
        case BTLeaves:
        case BTPlank:
        case BTRedstoneBlock:
        case BTRedstoneDustOff:
        case BTRedstoneOre:
        case BTRedstoneTorchOff:
        case BTSand:
        case BTSapling:
        case BTStick:
        case BTStoneButton:
        case BTStonePick:
        case BTStoneShovel:
        case BTTorch:
        case BTWood:
        case BTWoodButton:
        case BTWoodPick:
        case BTWoodShovel:
        case BTWorkbench:
        case BTDiamondOre:
        case BTEmeraldOre:
        case BTGrass:
        case BTLapisLazuliOre:
        case BTWater:
        case BTLava:
        case BTRedstoneDustOn:
        case BTRedstoneTorchOn:
        case BTStone:
        case BTLadder:
        case BTRedstoneRepeaterOff:
        case BTRedstoneRepeaterOn:
        case BTLever:
        case BTObsidian:
        case BTPiston:
        case BTStickyPiston:
        case BTPistonHead:
        case BTStickyPistonHead:
        case BTSlime:
        case BTGunpowder:
        case BTTNT:
        case BTBlazeRod:
        case BTBlazePowder:
        case BTStonePressurePlate:
        case BTWoodPressurePlate:
            return true;
        }
        return false;
    }

    /** add entities for digging block
     * 
     * @param x
     *            x coordinate
     * @param y
     *            y coordinate
     * @param z
     *            z coordinate */
    public void digBlock(int x, int y, int z)
    {
        switch(this.type)
        {
        case BTDeleteBlock:
        case BTSun:
        case BTMoon:
        case BTLast:
            return;
        case BTEmpty:
        case BTBedrock:
            return;
        case BTFurnace:
            world.insertEntity(Entity.NewBlock(new Vector(x + 0.5f,
                                                          y + 0.5f,
                                                          z + 0.5f),
                                               this.type,
                                               World.vRand(0.1f)));
            if(this.data.blockdata == BlockType.BTEmpty)
                return;
            for(int i = 0; i < this.data.srccount; i++)
            {
                world.insertEntity(Entity.NewBlock(new Vector(x + 0.5f,
                                                              y + 0.5f,
                                                              z + 0.5f),
                                                   this.data.blockdata,
                                                   World.vRand(0.1f)));
            }
            if(this.data.blockdata.getSmeltResult() == BlockType.BTEmpty)
                return;
            for(int i = 0; i < this.data.destcount; i++)
            {
                world.insertEntity(Entity.NewBlock(new Vector(x + 0.5f,
                                                              y + 0.5f,
                                                              z + 0.5f),
                                                   this.data.blockdata.getSmeltResult(),
                                                   World.vRand(0.1f)));
            }
            return;
        case BTChest:
            world.insertEntity(Entity.NewBlock(new Vector(x + 0.5f,
                                                          y + 0.5f,
                                                          z + 0.5f),
                                               this.type,
                                               World.vRand(0.1f)));
            for(int i = 1; i < BlockType.Count; i++)
            {
                while(true)
                {
                    BlockType bt = chestRemoveBlock(BlockType.toBlockType(i));
                    if(bt == BlockType.BTEmpty)
                        break;
                    world.insertEntity(Entity.NewBlock(new Vector(x + 0.5f,
                                                                  y + 0.5f,
                                                                  z + 0.5f),
                                                       bt,
                                                       World.vRand(0.1f)));
                }
            }
            return;
        case BTCoal:
        case BTCobblestone:
        case BTDiamondPick:
        case BTDiamond:
        case BTDiamondShovel:
        case BTDirt:
        case BTEmerald:
        case BTGlass:
        case BTGoldIngot:
        case BTGoldPick:
        case BTGoldShovel:
        case BTGoldOre:
        case BTGravel:
        case BTIronIngot:
        case BTIronOre:
        case BTIronPick:
        case BTIronShovel:
        case BTLapisLazuli:
        case BTPlank:
        case BTRedstoneBlock:
        case BTRedstoneDustOff:
        case BTRedstoneTorchOff:
        case BTSand:
        case BTSapling:
        case BTStick:
        case BTStoneButton:
        case BTStonePick:
        case BTStoneShovel:
        case BTTorch:
        case BTWood:
        case BTWoodButton:
        case BTWoodPick:
        case BTWoodShovel:
        case BTWorkbench:
        case BTLadder:
        case BTRedstoneRepeaterOff:
        case BTLever:
        case BTObsidian:
        case BTSlime:
        case BTGunpowder:
        case BTTNT:
        case BTBlazeRod:
        case BTBlazePowder:
        case BTStonePressurePlate:
        case BTWoodPressurePlate:
            world.insertEntity(Entity.NewBlock(new Vector(x + 0.5f,
                                                          y + 0.5f,
                                                          z + 0.5f),
                                               this.type,
                                               World.vRand(0.1f)));
            return;
        case BTRedstoneOre:
        {
            int count = Math.round(World.fRand(4 - 0.5f, 5 + 0.5f));
            for(int i = 0; i < count; i++)
                world.insertEntity(Entity.NewBlock(new Vector(x + 0.5f,
                                                              y + 0.5f,
                                                              z + 0.5f),
                                                   BlockType.BTRedstoneDustOff,
                                                   World.vRand(0.1f)));
            return;
        }
        case BTCoalOre:
            world.insertEntity(Entity.NewBlock(new Vector(x + 0.5f,
                                                          y + 0.5f,
                                                          z + 0.5f),
                                               BlockType.BTCoal,
                                               World.vRand(0.1f)));
            return;
        case BTDiamondOre:
            world.insertEntity(Entity.NewBlock(new Vector(x + 0.5f,
                                                          y + 0.5f,
                                                          z + 0.5f),
                                               BlockType.BTDiamond,
                                               World.vRand(0.1f)));
            return;
        case BTEmeraldOre:
            world.insertEntity(Entity.NewBlock(new Vector(x + 0.5f,
                                                          y + 0.5f,
                                                          z + 0.5f),
                                               BlockType.BTEmerald,
                                               World.vRand(0.1f)));
            return;
        case BTGrass:
            world.insertEntity(Entity.NewBlock(new Vector(x + 0.5f,
                                                          y + 0.5f,
                                                          z + 0.5f),
                                               BlockType.BTDirt,
                                               World.vRand(0.1f)));
            return;
        case BTLapisLazuliOre:
        {
            int count = Math.round(World.fRand(4 - 0.5f, 8 + 0.5f));
            for(int i = 0; i < count; i++)
                world.insertEntity(Entity.NewBlock(new Vector(x + 0.5f,
                                                              y + 0.5f,
                                                              z + 0.5f),
                                                   BlockType.BTLapisLazuli,
                                                   World.vRand(0.1f)));
            return;
        }
        case BTWater:
        case BTLava:
        {
            if(Math.abs(this.data.intdata) >= 8)
                world.insertEntity(Entity.NewBlock(new Vector(x + 0.5f,
                                                              y + 0.5f,
                                                              z + 0.5f),
                                                   this.type,
                                                   World.vRand(0.1f)));
            return;
        }
        case BTRedstoneRepeaterOn:
            world.insertEntity(Entity.NewBlock(new Vector(x + 0.5f,
                                                          y + 0.5f,
                                                          z + 0.5f),
                                               BlockType.BTRedstoneRepeaterOff,
                                               World.vRand(0.1f)));
            return;
        case BTRedstoneDustOn:
            world.insertEntity(Entity.NewBlock(new Vector(x + 0.5f,
                                                          y + 0.5f,
                                                          z + 0.5f),
                                               BlockType.BTRedstoneDustOff,
                                               World.vRand(0.1f)));
            return;
        case BTRedstoneTorchOn:
            world.insertEntity(Entity.NewBlock(new Vector(x + 0.5f,
                                                          y + 0.5f,
                                                          z + 0.5f),
                                               BlockType.BTRedstoneTorchOff,
                                               World.vRand(0.1f)));
            return;
        case BTStone:
            world.insertEntity(Entity.NewBlock(new Vector(x + 0.5f,
                                                          y + 0.5f,
                                                          z + 0.5f),
                                               BlockType.BTCobblestone,
                                               World.vRand(0.1f)));
            return;
        case BTLeaves:
        {
            if(World.fRand(0.0f, 1.0f) < 1.0f / 20)
            {
                world.insertEntity(Entity.NewBlock(new Vector(x + 0.5f,
                                                              y + 0.5f,
                                                              z + 0.5f),
                                                   BlockType.BTSapling,
                                                   World.vRand(0.1f)));
            }
            return;
        }
        case BTPiston:
        case BTStickyPiston:
        {
            world.insertEntity(Entity.NewBlock(new Vector(x + 0.5f,
                                                          y + 0.5f,
                                                          z + 0.5f),
                                               this.type,
                                               World.vRand(0.1f)));
            if(this.data.intdata == 0)
                return;
            int hx = getOrientationDX(this.data.orientation) + x;
            int hy = getOrientationDY(this.data.orientation) + y;
            int hz = getOrientationDZ(this.data.orientation) + z;
            Block head = world.getBlockEval(hx, hy, hz);
            if(head == null)
                return;
            if(head.getType() != BlockType.BTPistonHead
                    && head.getType() != BlockType.BTStickyPistonHead)
                return;
            world.setBlock(hx, hy, hz, new Block());
            return;
        }
        case BTPistonHead:
        case BTStickyPistonHead:
        {
            int bx = x - getOrientationDX(this.data.orientation);
            int by = y - getOrientationDY(this.data.orientation);
            int bz = z - getOrientationDZ(this.data.orientation);
            world.insertEntity(Entity.NewBlock(new Vector(x + 0.5f,
                                                          y + 0.5f,
                                                          z + 0.5f),
                                               (this.type == BlockType.BTStickyPistonHead) ? BlockType.BTStickyPiston
                                                       : BlockType.BTPiston,
                                               World.vRand(0.1f)));
            Block body = world.getBlockEval(bx, by, bz);
            if(body == null)
                return;
            if(body.getType() != BlockType.BTPiston
                    && body.getType() != BlockType.BTStickyPiston)
                return;
            world.setBlock(bx, by, bz, new Block());
            return;
        }
        }
    }

    private static final int REDSTONE_POWER_INPUT = -2;
    private static final int REDSTONE_POWER_NONE = -1;
    private static final int REDSTONE_POWER_WEAK_OFF = 0;
    private static final int REDSTONE_POWER_WEAK_MIN = 1;
    private static final int REDSTONE_POWER_WEAK_MAX = 15;
    private static final int REDSTONE_POWER_STRONG_OFF = 16;
    private static final int REDSTONE_POWER_STRONG = 17;

    private int getRedstoneIOValue(int dir)
    {
        switch(this.type)
        {
        case BTSun:
        case BTMoon:
        case BTDeleteBlock:
        case BTLast:
            return REDSTONE_POWER_NONE;
        case BTEmpty:
        case BTBedrock:
        case BTChest:
        case BTCoal:
        case BTCoalOre:
        case BTCobblestone:
        case BTDiamond:
        case BTDiamondOre:
        case BTDiamondPick:
        case BTDiamondShovel:
        case BTDirt:
        case BTEmerald:
        case BTEmeraldOre:
        case BTFurnace:
        case BTGlass:
        case BTGoldIngot:
        case BTGoldOre:
        case BTGoldPick:
        case BTGoldShovel:
        case BTGrass:
        case BTGravel:
        case BTIronIngot:
        case BTIronOre:
        case BTIronPick:
        case BTIronShovel:
        case BTLapisLazuli:
        case BTLapisLazuliOre:
        case BTLava:
        case BTLeaves:
        case BTPlank:
            return REDSTONE_POWER_NONE;
        case BTRedstoneBlock:
            return REDSTONE_POWER_STRONG;
        case BTRedstoneDustOff:
            return REDSTONE_POWER_WEAK_OFF;
        case BTRedstoneDustOn:
            return this.data.intdata;
        case BTRedstoneOre:
            return REDSTONE_POWER_NONE;
        case BTRedstoneTorchOff:
            if(dir == this.data.orientation)
                return REDSTONE_POWER_INPUT;
            return REDSTONE_POWER_STRONG_OFF;
        case BTRedstoneTorchOn:
            if(dir == this.data.orientation)
                return REDSTONE_POWER_INPUT;
            return REDSTONE_POWER_STRONG;
        case BTSand:
        case BTSapling:
        case BTStick:
        case BTStone:
            return REDSTONE_POWER_NONE;
        case BTStoneButton:
            if(dir != this.data.orientation)
                return REDSTONE_POWER_NONE;
            if(this.data.intdata > 0)
                return REDSTONE_POWER_STRONG;
            return REDSTONE_POWER_STRONG_OFF;
        case BTStonePick:
        case BTStoneShovel:
        case BTTorch:
        case BTWater:
        case BTWood:
            return REDSTONE_POWER_NONE;
        case BTWoodButton:
            if(dir != this.data.orientation)
                return REDSTONE_POWER_NONE;
            if(this.data.intdata > 0)
                return REDSTONE_POWER_STRONG;
            return REDSTONE_POWER_STRONG_OFF;
        case BTWoodPick:
        case BTWoodShovel:
        case BTWorkbench:
        case BTLadder:
            return REDSTONE_POWER_NONE;
        case BTRedstoneRepeaterOff:
        case BTRedstoneRepeaterOn:
            if(dir == this.data.orientation)
            {
                if(this.type == BlockType.BTRedstoneRepeaterOn)
                    return REDSTONE_POWER_STRONG;
                return REDSTONE_POWER_STRONG_OFF;
            }
            if(dir == getNegOrientation(this.data.orientation))
                return REDSTONE_POWER_INPUT;
            return REDSTONE_POWER_NONE;
        case BTLever:
        case BTStonePressurePlate:
        case BTWoodPressurePlate:
            if(this.data.intdata != 0)
                return REDSTONE_POWER_STRONG;
            return REDSTONE_POWER_STRONG_OFF;
        case BTObsidian:
            return REDSTONE_POWER_NONE;
        case BTPiston:
        case BTStickyPiston:
            return REDSTONE_POWER_INPUT;
        case BTPistonHead:
        case BTStickyPistonHead:
        case BTSlime:
        case BTGunpowder:
            return REDSTONE_POWER_NONE;
        case BTTNT:
            return REDSTONE_POWER_INPUT;
        case BTBlazeRod:
        case BTBlazePowder:
            return REDSTONE_POWER_NONE;
        }
        return REDSTONE_POWER_NONE;
    }

    private static int getEvalRedstoneIOPowerH(int origValue,
                                               int bx,
                                               int by,
                                               int bz,
                                               int dir)
    {
        Block b = world.getBlockEval(bx, by, bz);
        if(b == null)
            return origValue;
        int v = b.getRedstoneIOValue(dir);
        if(v == REDSTONE_POWER_STRONG || origValue == REDSTONE_POWER_STRONG)
            return REDSTONE_POWER_STRONG;
        if(v >= REDSTONE_POWER_WEAK_MIN && v <= REDSTONE_POWER_WEAK_MAX)
            return REDSTONE_POWER_WEAK_MIN;
        if(origValue >= REDSTONE_POWER_WEAK_MIN
                && origValue <= REDSTONE_POWER_WEAK_MAX)
            return REDSTONE_POWER_WEAK_MIN;
        if(v == REDSTONE_POWER_STRONG_OFF)
            return REDSTONE_POWER_STRONG_OFF;
        if(origValue == REDSTONE_POWER_STRONG_OFF)
            return REDSTONE_POWER_STRONG_OFF;
        if(v == REDSTONE_POWER_WEAK_OFF)
            return REDSTONE_POWER_WEAK_OFF;
        if(origValue == REDSTONE_POWER_WEAK_OFF)
            return REDSTONE_POWER_WEAK_OFF;
        if(v == REDSTONE_POWER_INPUT)
            return REDSTONE_POWER_INPUT;
        return origValue;
    }

    private static int getEvalRedstoneDustIOPowerH(int origValue,
                                                   int bx,
                                                   int by,
                                                   int bz,
                                                   int dir)
    {
        Block b = world.getBlockEval(bx, by, bz);
        if(b == null)
            return origValue;
        int v = b.getRedstoneIOValue(dir);
        if(v == REDSTONE_POWER_STRONG || origValue == REDSTONE_POWER_STRONG)
            return REDSTONE_POWER_STRONG;
        if(v == REDSTONE_POWER_STRONG_OFF)
            return REDSTONE_POWER_STRONG_OFF;
        if(origValue == REDSTONE_POWER_STRONG_OFF)
            return REDSTONE_POWER_STRONG_OFF;
        if(v == REDSTONE_POWER_INPUT)
            return REDSTONE_POWER_INPUT;
        return origValue;
    }

    private boolean getPassesRedstonePower()
    {
        switch(this.type)
        {
        case BTSun:
        case BTMoon:
        case BTDeleteBlock:
        case BTLast:
            return false;
        case BTEmpty:
        case BTCoal:
        case BTDiamond:
        case BTDiamondPick:
        case BTDiamondShovel:
        case BTEmerald:
        case BTGlass:
        case BTGoldIngot:
        case BTGoldPick:
        case BTGoldShovel:
        case BTIronIngot:
        case BTIronPick:
        case BTIronShovel:
        case BTLapisLazuli:
        case BTLava:
        case BTLeaves:
        case BTPlank:
        case BTRedstoneBlock:
        case BTRedstoneDustOff:
        case BTRedstoneDustOn:
        case BTRedstoneTorchOff:
        case BTRedstoneTorchOn:
        case BTSapling:
        case BTStick:
        case BTStoneButton:
        case BTStonePick:
        case BTStoneShovel:
        case BTTorch:
        case BTWater:
        case BTWoodButton:
        case BTWoodPick:
        case BTWoodShovel:
        case BTLadder:
        case BTRedstoneRepeaterOff:
        case BTRedstoneRepeaterOn:
        case BTLever:
        case BTPiston:
        case BTStickyPiston:
        case BTPistonHead:
        case BTStickyPistonHead:
        case BTSlime:
        case BTGunpowder:
        case BTTNT:
        case BTBlazeRod:
        case BTBlazePowder:
        case BTStonePressurePlate:
        case BTWoodPressurePlate:
            return false;
        case BTBedrock:
        case BTChest:
        case BTCoalOre:
        case BTCobblestone:
        case BTDiamondOre:
        case BTDirt:
        case BTEmeraldOre:
        case BTFurnace:
        case BTGoldOre:
        case BTGrass:
        case BTGravel:
        case BTIronOre:
        case BTLapisLazuliOre:
        case BTRedstoneOre:
        case BTSand:
        case BTStone:
        case BTWood:
        case BTWorkbench:
        case BTObsidian:
            return true;
        }
        return false;
    }

    /** @param orientation
     *            the orientation
     * @return the orientation's x component */
    public static int getOrientationDX(int orientation)
    {
        switch(orientation)
        {
        case 0:
            return -1;
        case 2:
            return 1;
        case 1:
        case 3:
        case 5:
        default:
            return 0;
        }
    }

    /** @param orientation
     *            the orientation
     * @return the orientation's y component */
    public static int getOrientationDY(int orientation)
    {
        switch(orientation)
        {
        case 0:
        case 1:
        case 2:
        case 3:
            return 0;
        case 5:
            return 1;
        default:
            return -1;
        }
    }

    /** @param orientation
     *            the orientation
     * @return the orientation's z component */
    public static int getOrientationDZ(int orientation)
    {
        switch(orientation)
        {
        case 1:
            return -1;
        case 3:
            return 1;
        case 0:
        case 2:
        case 5:
        default:
            return 0;
        }
    }

    /** @param orientation
     *            the orientation to invert
     * @return the opposite orientation */
    public static int getNegOrientation(int orientation)
    {
        switch(orientation)
        {
        case 0:
            return 2;
        case 1:
            return 3;
        case 2:
            return 0;
        case 3:
            return 1;
        case 5:
            return 4;
        default:
            return 5;
        }
    }

    /** @param axis
     *            the axis to rotate around
     * @param angle
     *            the angle to rotate. for an angle x, <code>angle</code> = x
     *            &divide; 90&deg;
     * @param originalOrientation
     *            the orientation to rotate
     * @return the rotated orientation */
    public static int getRotatedOrientation(int axis,
                                            int angle,
                                            int originalOrientation)
    {
        Vector vAxis = new Vector(getOrientationDX(axis),
                                  getOrientationDY(axis),
                                  getOrientationDZ(axis));
        Vector vOriginalOrientation = new Vector(getOrientationDX(originalOrientation),
                                                 getOrientationDY(originalOrientation),
                                                 getOrientationDZ(originalOrientation));
        return getOrientationFromVector(Matrix.rotate(vAxis,
                                                      angle * Math.PI / 2)
                                              .apply(vOriginalOrientation));
    }

    /** @param dir
     *            the <code>Vector</code> to get an orientation from
     * @return the orientation that <code>dir</code> is closest to */
    public static int getOrientationFromVector(Vector dir)
    {
        Vector absv = new Vector(Math.abs(dir.x),
                                 Math.abs(dir.y),
                                 Math.abs(dir.z));
        if(absv.x >= absv.y)
        {
            if(absv.x >= absv.z)
            {
                if(dir.x <= 0)
                    return 0;
                return 2;
            }
            if(dir.z <= 0)
                return 1;
            return 3;
        }
        if(absv.y >= absv.z)
        {
            if(dir.y <= 0)
                return 4;
            return 5;
        }
        if(dir.z <= 0)
            return 1;
        return 3;
    }

    private static int getEvalRedstoneIOValue(int bx, int by, int bz, int dir)
    {
        Block b = world.getBlockEval(bx, by, bz);
        int retval = -1;
        if(b == null)
            return REDSTONE_POWER_NONE;
        retval = b.getRedstoneIOValue(dir);
        if(retval != REDSTONE_POWER_NONE)
            return retval;
        if(!b.getPassesRedstonePower())
            return REDSTONE_POWER_NONE;
        for(int orientation = 0; orientation <= 5; orientation++)
        {
            if(orientation == dir)
                continue;
            int dx = getOrientationDX(orientation);
            int dy = getOrientationDY(orientation);
            int dz = getOrientationDZ(orientation);
            int x = bx + dx, y = by + dy, z = bz + dz;
            retval = getEvalRedstoneIOPowerH(retval,
                                             x,
                                             y,
                                             z,
                                             getNegOrientation(orientation));
        }
        return retval;
    }

    private static int getEvalRedstoneDustIOValue(int bx,
                                                  int by,
                                                  int bz,
                                                  int dir)
    {
        Block b = world.getBlockEval(bx, by, bz);
        int retval = -1;
        if(b == null)
            return REDSTONE_POWER_NONE;
        if(dir == 4
                && (b.getType() == BlockType.BTRedstoneTorchOff || b.getType() == BlockType.BTRedstoneTorchOn))
        {
            return b.getRedstoneIOValue(dir);
        }
        else if(dir == 4)
            return REDSTONE_POWER_NONE;
        retval = b.getRedstoneIOValue(dir);
        if(retval != REDSTONE_POWER_NONE)
            return retval;
        if(!b.getPassesRedstonePower())
            return REDSTONE_POWER_NONE;
        for(int orientation = 0; orientation <= 5; orientation++)
        {
            if(orientation == dir)
                continue;
            int dx = getOrientationDX(orientation);
            int dy = getOrientationDY(orientation);
            int dz = getOrientationDZ(orientation);
            int x = bx + dx, y = by + dy, z = bz + dz;
            retval = getEvalRedstoneDustIOPowerH(retval,
                                                 x,
                                                 y,
                                                 z,
                                                 getNegOrientation(orientation));
        }
        return retval;
    }

    private boolean getCutsRedstoneDust()
    {
        switch(this.type)
        {
        case BTSun:
        case BTMoon:
        case BTDeleteBlock:
        case BTLast:
            return false;
        case BTEmpty:
        case BTCoal:
        case BTDiamond:
        case BTDiamondPick:
        case BTDiamondShovel:
        case BTEmerald:
        case BTGlass:
        case BTGoldIngot:
        case BTGoldPick:
        case BTGoldShovel:
        case BTIronIngot:
        case BTIronPick:
        case BTIronShovel:
        case BTLapisLazuli:
        case BTLeaves:
        case BTPlank:
        case BTSapling:
        case BTStick:
        case BTStoneButton:
        case BTStonePick:
        case BTStoneShovel:
        case BTTorch:
        case BTWoodButton:
        case BTWoodPick:
        case BTWoodShovel:
        case BTLadder:
        case BTSlime:
            return false;
        case BTRedstoneBlock:
        case BTRedstoneDustOff:
        case BTRedstoneDustOn:
        case BTRedstoneTorchOff:
        case BTRedstoneTorchOn:
        case BTWater:
        case BTLava:
        case BTBedrock:
        case BTChest:
        case BTCoalOre:
        case BTCobblestone:
        case BTDiamondOre:
        case BTDirt:
        case BTEmeraldOre:
        case BTFurnace:
        case BTGoldOre:
        case BTGrass:
        case BTGravel:
        case BTIronOre:
        case BTLapisLazuliOre:
        case BTRedstoneOre:
        case BTSand:
        case BTStone:
        case BTWood:
        case BTWorkbench:
        case BTRedstoneRepeaterOff:
        case BTRedstoneRepeaterOn:
        case BTLever:
        case BTObsidian:
        case BTPiston:
        case BTStickyPiston:
        case BTPistonHead:
        case BTStickyPistonHead:
        case BTGunpowder:
        case BTTNT:
        case BTBlazeRod:
        case BTBlazePowder:
        case BTStonePressurePlate:
        case BTWoodPressurePlate:
            return true;
        }
        return true;
    }

    /** @param pos
     *            the player's relative position
     * @param dir
     *            the direction the player wants to move
     * @return true if the player is pushing into this ladder */
    public boolean ladderIsPlayerPushingIntoLadder(Vector pos, Vector dir)
    {
        Vector ladderOrientation = new Vector(getOrientationDX(this.data.orientation),
                                              getOrientationDY(this.data.orientation),
                                              getOrientationDZ(this.data.orientation));
        if(pos.sub(new Vector(0.5f)).dot(ladderOrientation) <= 0)
            return false;
        if(getRayExitSide(pos, dir.normalize()) == this.data.orientation)
            return true;
        return false;
    }

    /** @return true if this block can be placed while the player is inside of it */
    public boolean isPlaceableWhileInside()
    {
        return this.type.isPlaceableWhileInside();
    }

    /** steps to the next delay for this redstone repeater */
    public void redstoneRepeaterStepDelay()
    {
        int delay = this.data.intdata;
        delay++;
        if(delay > 4)
            delay = 1;
        this.data.intdata = delay;
        this.data.step = 0;
    }

    /** @param bt
     *            the type of block to make
     * @param orientation
     *            the orientation for the side of the block clicked on
     * @param vieworientation
     *            the orientation for the direction the player is facing
     * @return new block or null */
    public static Block
        make(BlockType bt, int orientation, int vieworientation)
    {
        return bt.make(orientation, vieworientation);
    }

    /** toggles this lever */
    public void leverToggle()
    {
        if(this.data.intdata != 0)
            this.data.intdata = 0;
        else
            this.data.intdata = 1;
    }

    /** write to a <code>DataOutput</code>
     * 
     * @param o
     *            <code>DataOutput</code> to write to
     * @throws IOException
     *             the exception thrown */
    public void write(DataOutput o) throws IOException
    {
        this.type.write(o);
        o.writeByte(Math.max(0, Math.min(15, this.light)));
        o.writeByte(Math.max(0, Math.min(15, this.scatteredSunlight)));
        o.writeByte(Math.max(0, Math.min(15, this.sunlight)));
        switch(this.type)
        {
        case BTSun:
        case BTMoon:
        case BTLast:
        case BTDeleteBlock:
            return;
        case BTBedrock:
        case BTCoal:
        case BTCoalOre:
        case BTCobblestone:
        case BTDiamond:
        case BTDiamondOre:
        case BTDiamondPick:
        case BTDiamondShovel:
        case BTDirt:
        case BTEmerald:
        case BTEmeraldOre:
        case BTEmpty:
        case BTGlass:
        case BTGoldIngot:
        case BTGoldOre:
        case BTGoldPick:
        case BTGoldShovel:
        case BTGrass:
        case BTGravel:
        case BTIronIngot:
        case BTIronOre:
        case BTIronPick:
        case BTIronShovel:
        case BTLapisLazuli:
        case BTLapisLazuliOre:
        case BTLeaves:
        case BTPlank:
        case BTRedstoneBlock:
        case BTRedstoneOre:
        case BTSand:
        case BTStick:
        case BTStone:
        case BTStonePick:
        case BTStoneShovel:
        case BTWood:
        case BTWoodPick:
        case BTWoodShovel:
        case BTWorkbench:
        case BTObsidian:
            return;
        case BTChest:
        {
            int count = 0;
            for(int i = 1; i < BlockType.Count; i++)
            {
                if(this.data.BlockCounts[i] > 0)
                    count++;
            }
            o.writeShort(count);
            for(int i = 1; i < BlockType.Count; i++)
            {
                if(this.data.BlockCounts[i] > 0)
                {
                    BlockType.toBlockType(i).write(o);
                    o.writeInt(this.data.BlockCounts[i]);
                }
            }
            return;
        }
        case BTFurnace:
        {
            o.writeInt(this.data.intdata);
            o.writeInt(this.data.srccount);
            o.writeInt(this.data.destcount);
            this.data.blockdata.write(o);
            if(this.data.intdata > 0 && this.data.srccount > 0
                    && this.data.blockdata != BlockType.BTEmpty)
            {
                double reltime = this.data.runTime - world.getCurTime();
                o.writeDouble(reltime);
            }
            return;
        }
        case BTLadder:
        {
            o.writeByte(this.data.orientation);
            return;
        }
        case BTLava:
        case BTWater:
        case BTStonePressurePlate:
        case BTWoodPressurePlate:
        {
            o.writeByte(this.data.intdata);
            return;
        }
        case BTLever:
        {
            o.writeBoolean(this.data.intdata != 0);
            o.writeByte(this.data.orientation);
            return;
        }
        case BTRedstoneDustOff:
        {
            o.writeByte(this.data.orientation);
            return;
        }
        case BTRedstoneDustOn:
        {
            o.writeByte(this.data.intdata);
            o.writeByte(this.data.orientation);
            return;
        }
        case BTRedstoneRepeaterOff:
        case BTRedstoneRepeaterOn:
        {
            o.writeByte(this.data.intdata);
            o.writeByte(this.data.step);
            o.writeByte(this.data.orientation);
            return;
        }
        case BTRedstoneTorchOff:
        case BTRedstoneTorchOn:
        case BTTorch:
        {
            o.writeByte(this.data.orientation);
            return;
        }
        case BTSapling:
        {
            if(this.data.runTime < 0)
            {
                o.writeDouble(-1.0);
            }
            else
            {
                double reltime = this.data.runTime - world.getCurTime();
                o.writeDouble(reltime);
            }
            return;
        }
        case BTStoneButton:
        case BTWoodButton:
        {
            o.writeByte(this.data.intdata);
            o.writeByte(this.data.orientation);
            return;
        }
        case BTPiston:
        case BTStickyPiston:
        {
            o.writeBoolean(this.data.intdata != 0);
            o.writeBoolean(this.data.step != 0);
            o.writeByte(this.data.orientation);
            return;
        }
        case BTPistonHead:
        case BTStickyPistonHead:
        {
            o.writeByte(this.data.orientation);
            return;
        }
        case BTSlime:
        case BTGunpowder:
        case BTTNT:
        case BTBlazeRod:
        case BTBlazePowder:
            return;
        }
    }

    private void internalRead(DataInput i) throws IOException
    {
        switch(this.type)
        {
        case BTSun:
        case BTMoon:
        case BTLast:
        case BTDeleteBlock:
            return;
        case BTBedrock:
        case BTCoal:
        case BTCoalOre:
        case BTCobblestone:
        case BTDiamond:
        case BTDiamondOre:
        case BTDiamondPick:
        case BTDiamondShovel:
        case BTDirt:
        case BTEmerald:
        case BTEmeraldOre:
        case BTEmpty:
        case BTGlass:
        case BTGoldIngot:
        case BTGoldOre:
        case BTGoldPick:
        case BTGoldShovel:
        case BTGrass:
        case BTGravel:
        case BTIronIngot:
        case BTIronOre:
        case BTIronPick:
        case BTIronShovel:
        case BTLapisLazuli:
        case BTLapisLazuliOre:
        case BTLeaves:
        case BTPlank:
        case BTRedstoneBlock:
        case BTRedstoneOre:
        case BTSand:
        case BTStick:
        case BTStone:
        case BTStonePick:
        case BTStoneShovel:
        case BTWood:
        case BTWoodPick:
        case BTWoodShovel:
        case BTWorkbench:
        case BTObsidian:
            return;
        case BTChest:
        {
            this.data.BlockCounts = new int[BlockType.Count];
            int count = i.readUnsignedShort();
            if(count > BlockType.Count)
                throw new IOException("Chest block count is too big");
            while(count-- > 0)
            {
                int index = BlockType.read(i).value;
                if(this.data.BlockCounts[index] > 0)
                    throw new IOException("Chest block type is duplicate");
                int value = i.readInt();
                if(value <= 0 || value >= 1000000000)
                    throw new IOException("Chest value is out of range");
                this.data.BlockCounts[index] = value;
            }
            return;
        }
        case BTFurnace:
        {
            int fuel = i.readInt();
            if(fuel < 0 || fuel >= 1000000000)
                throw new IOException("Furnace fuel is out of range");
            int srccount = i.readInt();
            if(srccount < 0 || srccount >= 1000000000)
                throw new IOException("Furnace source count is out of range");
            int destcount = i.readInt();
            if(destcount < 0 || destcount >= 1000000000)
                throw new IOException("Furnace destination count is out of range");
            this.data.intdata = fuel;
            this.data.srccount = srccount;
            this.data.destcount = destcount;
            this.data.blockdata = BlockType.read(i);
            if(this.data.blockdata == BlockType.BTEmpty)
            {
                if(srccount != 0 || destcount != 0)
                    throw new IOException("Furnace block count is non-zero when the block type is empty");
            }
            else if(!this.data.blockdata.isSmeltable())
                throw new IOException("Furnace has illegal block type");
            else
            {
                if(srccount == 0 && destcount == 0)
                    throw new IOException("Furnace has block count of zero when the block type is non-empty");
            }
            if(this.data.intdata > 0 && this.data.srccount > 0
                    && this.data.blockdata != BlockType.BTEmpty)
            {
                double value = i.readDouble();
                if(Double.isInfinite(value) || Double.isNaN(value)
                        || value <= 0 || value > 10.0)
                    throw new IOException("furnace left to smelt is out of range");
                this.data.runTime = value + world.getCurTime();
            }
            return;
        }
        case BTLadder:
        {
            this.data.orientation = i.readByte();
            if(this.data.orientation < 0 || this.data.orientation > 3)
                throw new IOException("Ladder orientation is out of range");
            return;
        }
        case BTLava:
        case BTWater:
        {
            this.data.intdata = i.readByte();
            if(this.data.intdata == 0 || this.data.intdata < -8
                    || this.data.intdata > 8)
                throw new IOException("fluid height is out of range");
            return;
        }
        case BTLever:
        {
            if(i.readBoolean())
                this.data.intdata = 1;
            else
                this.data.intdata = 0;
            this.data.orientation = i.readByte();
            if(this.data.orientation < 0 || this.data.orientation > 5)
                throw new IOException("Lever orientation is out of range");
            return;
        }
        case BTRedstoneDustOff:
        {
            this.data.orientation = i.readUnsignedByte();
            if(((this.data.orientation >> 4) & ~this.data.orientation) != 0)
                throw new IOException("Redstone Dust Off orientation is not valid");
            return;
        }
        case BTRedstoneDustOn:
        {
            this.data.intdata = i.readUnsignedByte();
            if(this.data.intdata <= 0 || this.data.intdata >= 16)
                throw new IOException("Redstone Dust On value is out of range");
            this.data.orientation = i.readUnsignedByte();
            if(((this.data.orientation >> 4) & ~this.data.orientation) != 0)
                throw new IOException("Redstone Dust On orientation is not valid");
            return;
        }
        case BTRedstoneRepeaterOff:
        case BTRedstoneRepeaterOn:
        {
            this.data.intdata = i.readUnsignedByte();
            if(this.data.intdata < 1 || this.data.intdata > 4)
                throw new IOException("Redstone Repeater delay is out of range");
            this.data.step = i.readUnsignedByte();
            if(this.data.step < 0 || this.data.step >= this.data.intdata)
                throw new IOException("Redstone Repeater current step is out of range");
            this.data.orientation = i.readUnsignedByte();
            if(this.data.orientation < 0 || this.data.orientation > 3)
                throw new IOException("Redstone Repeater orientation is out of range");
            return;
        }
        case BTRedstoneTorchOff:
        case BTRedstoneTorchOn:
        case BTTorch:
        {
            this.data.orientation = i.readUnsignedByte();
            if(this.data.orientation < 0 || this.data.orientation > 4)
                throw new IOException("Torch orientation is out of range");
            return;
        }
        case BTSapling:
        {
            double value = i.readDouble();
            if(Double.isInfinite(value)
                    || Double.isNaN(value)
                    || (value != -1 && (value <= 0 || value > this.type.getGrowTime())))
                throw new IOException("Sapling time left to grow is out of range");
            this.data.runTime = value;
            if(this.data.runTime > 0)
                this.data.runTime += world.getCurTime();
            return;
        }
        case BTStoneButton:
        case BTWoodButton:
        {
            this.data.intdata = i.readUnsignedByte();
            if(this.data.intdata < 0
                    || this.data.intdata > this.type.getOnTime())
                throw new IOException("Button time left is out of range");
            this.data.orientation = i.readUnsignedByte();
            if(this.data.orientation < 0 || this.data.orientation > 4)
                throw new IOException("Button orientation is out of range");
            return;
        }
        case BTPiston:
        case BTStickyPiston:
        {
            this.data.intdata = i.readBoolean() ? 1 : 0;
            this.data.step = i.readBoolean() ? 1 : 0;
            this.data.orientation = i.readUnsignedByte();
            if(this.data.orientation < 0 || this.data.orientation > 5)
                throw new IOException("Piston orientation is out of range");
            return;
        }
        case BTPistonHead:
        case BTStickyPistonHead:
        {
            this.data.orientation = i.readUnsignedByte();
            if(this.data.orientation < 0 || this.data.orientation > 5)
                throw new IOException("Piston Head orientation is out of range");
            return;
        }
        case BTSlime:
        case BTGunpowder:
        case BTTNT:
        case BTBlazeRod:
        case BTBlazePowder:
            return;
        case BTStonePressurePlate:
        case BTWoodPressurePlate:
        {
            if(i.readBoolean())
                this.data.intdata = 1;
            else
                this.data.intdata = 0;
            return;
        }
        }
    }

    /** read from a <code>DataInput</code>
     * 
     * @param i
     *            <code>DataInput</code> to read from
     * @return the read <code>Block</code>
     * @throws IOException
     *             the exception thrown */
    public static Block read(DataInput i) throws IOException
    {
        Block retval = new Block(BlockType.read(i));
        retval.light = i.readUnsignedByte();
        if(retval.light < 0 || retval.light > 15)
            throw new IOException("light is out of range");
        retval.scatteredSunlight = i.readUnsignedByte();
        if(retval.scatteredSunlight < 0 || retval.scatteredSunlight > 15)
            throw new IOException("scatteredSunlight is out of range");
        retval.sunlight = i.readUnsignedByte();
        if(retval.sunlight < 0 || retval.sunlight > 15)
            throw new IOException("sunlight is out of range");
        retval.internalRead(i);
        return retval;
    }

    /** @return true if this block is explodable */
    public boolean isExplodable()
    {
        return this.type.isExplodable();
    }

    /** check if <code>this</code> located at &lt;<code>bx</code>,
     * <code>by</code>, <code>bz</code>&gt; draws identically to <code>b</code>
     * located at &lt;<code>bbx</code>, <code>bby</code>, <code>bbz</code>&gt;
     * 
     * @param bx
     *            this block's x coordinate
     * @param by
     *            this block's y coordinate
     * @param bz
     *            this block's z coordinate
     * @param b
     *            the other block
     * @param bbx
     *            <code>b</code>'s x coordinate
     * @param bby
     *            <code>b</code>'s y coordinate
     * @param bbz
     *            <code>b</code>'s z coordinate
     * @return if <code>this</code> located at &lt;<code>bx</code>,
     *         <code>by</code>, <code>bz</code>&gt; draws identically to
     *         <code>b</code> located at &lt;<code>bbx</code>, <code>bby</code>,
     *         <code>bbz</code>&gt; */
    public boolean drawsSame(int bx,
                             int by,
                             int bz,
                             Block b,
                             int bbx,
                             int bby,
                             int bbz)
    {
        if(this.type != b.type)
            return false;
        if(this.lighting == null || b.lighting == null
                || this.curSunlightFactor != b.curSunlightFactor)
            return false;
        for(int i = 0; i < this.lighting.length; i++)
        {
            if(this.lighting[i] != b.lighting[i])
                return false;
        }
        switch(this.type.drawType)
        {
        case BDTButton:
        case BDTItem:
        case BDTNone:
        case BDTSolidAllSides:
        case BDTTorch:
            break;
        case BDTCustom:
            break;
        case BDTLiquid:
            // TODO finish
            return false;
        case BDTSolid:
        {
            Block nx = world.getBlock(bx - 1, by, bz);
            Block px = world.getBlock(bx + 1, by, bz);
            Block ny = world.getBlock(bx, by - 1, bz);
            Block py = world.getBlock(bx, by + 1, bz);
            Block nz = world.getBlock(bx, by, bz - 1);
            Block pz = world.getBlock(bx, by, bz + 1);
            int drawMask = 0;
            if(nx != null && !nx.isOpaque())
                drawMask |= DMaskNX;
            if(px != null && !px.isOpaque())
                drawMask |= DMaskPX;
            if(ny != null && !ny.isOpaque())
                drawMask |= DMaskNY;
            if(py != null && !py.isOpaque())
                drawMask |= DMaskPY;
            if(nz != null && !nz.isOpaque())
                drawMask |= DMaskNZ;
            if(pz != null && !pz.isOpaque())
                drawMask |= DMaskPZ;
            Block nx2 = world.getBlock(bbx - 1, bby, bbz);
            Block px2 = world.getBlock(bbx + 1, bby, bbz);
            Block ny2 = world.getBlock(bbx, bby - 1, bbz);
            Block py2 = world.getBlock(bbx, bby + 1, bbz);
            Block nz2 = world.getBlock(bbx, bby, bbz - 1);
            Block pz2 = world.getBlock(bbx, bby, bbz + 1);
            int drawMask2 = 0;
            if(nx2 != null && !nx2.isOpaque())
                drawMask2 |= DMaskNX;
            if(px2 != null && !px2.isOpaque())
                drawMask2 |= DMaskPX;
            if(ny2 != null && !ny2.isOpaque())
                drawMask2 |= DMaskNY;
            if(py2 != null && !py2.isOpaque())
                drawMask2 |= DMaskPY;
            if(nz2 != null && !nz2.isOpaque())
                drawMask2 |= DMaskNZ;
            if(pz2 != null && !pz2.isOpaque())
                drawMask2 |= DMaskPZ;
            if(drawMask != drawMask2)
                return false;
        }
        }
        switch(this.type)
        {
        case BTLast:
        case BTSun:
        case BTMoon:
            return true;
        case BTDeleteBlock:
            return this.data.intdata == b.data.intdata;
        case BTBedrock:
        case BTBlazePowder:
        case BTBlazeRod:
        case BTChest:
        case BTCoal:
        case BTCoalOre:
        case BTCobblestone:
        case BTDiamond:
        case BTDiamondOre:
        case BTDiamondPick:
        case BTDiamondShovel:
        case BTDirt:
        case BTEmerald:
        case BTEmeraldOre:
        case BTEmpty:
        case BTGlass:
        case BTGoldIngot:
        case BTGoldOre:
        case BTGoldPick:
        case BTGoldShovel:
        case BTGrass:
        case BTGravel:
        case BTGunpowder:
        case BTIronIngot:
        case BTIronOre:
        case BTIronPick:
        case BTIronShovel:
        case BTLapisLazuli:
        case BTLapisLazuliOre:
        case BTObsidian:
        case BTPlank:
        case BTRedstoneBlock:
        case BTRedstoneOre:
        case BTSand:
        case BTSapling:
        case BTSlime:
        case BTStick:
        case BTStone:
        case BTStonePick:
        case BTStoneShovel:
        case BTTNT:
        case BTWoodPick:
        case BTWoodShovel:
        case BTWorkbench:
        case BTStonePressurePlate:
        case BTWoodPressurePlate:
            return true;
        case BTWood:
        case BTLeaves:
        {
            Block nx = world.getBlock(bx - 1, by, bz);
            Block px = world.getBlock(bx + 1, by, bz);
            Block ny = world.getBlock(bx, by - 1, bz);
            Block py = world.getBlock(bx, by + 1, bz);
            Block nz = world.getBlock(bx, by, bz - 1);
            Block pz = world.getBlock(bx, by, bz + 1);
            int drawMask = 0;
            if(nx != null && !nx.isOpaque())
                drawMask |= DMaskNX;
            if(px != null && !px.isOpaque())
                drawMask |= DMaskPX;
            if(ny != null && !ny.isOpaque())
                drawMask |= DMaskNY;
            if(py != null && !py.isOpaque())
                drawMask |= DMaskPY;
            if(nz != null && !nz.isOpaque())
                drawMask |= DMaskNZ;
            if(pz != null && !pz.isOpaque())
                drawMask |= DMaskPZ;
            Block nx2 = world.getBlock(bbx - 1, bby, bbz);
            Block px2 = world.getBlock(bbx + 1, bby, bbz);
            Block ny2 = world.getBlock(bbx, bby - 1, bbz);
            Block py2 = world.getBlock(bbx, bby + 1, bbz);
            Block nz2 = world.getBlock(bbx, bby, bbz - 1);
            Block pz2 = world.getBlock(bbx, bby, bbz + 1);
            int drawMask2 = 0;
            if(nx2 != null && !nx2.isOpaque())
                drawMask2 |= DMaskNX;
            if(px2 != null && !px2.isOpaque())
                drawMask2 |= DMaskPX;
            if(ny2 != null && !ny2.isOpaque())
                drawMask2 |= DMaskNY;
            if(py2 != null && !py2.isOpaque())
                drawMask2 |= DMaskPY;
            if(nz2 != null && !nz2.isOpaque())
                drawMask2 |= DMaskNZ;
            if(pz2 != null && !pz2.isOpaque())
                drawMask2 |= DMaskPZ;
            if(drawMask != drawMask2)
                return false;
            return true;
        }
        case BTFurnace:
        {
            Block nx = world.getBlock(bx - 1, by, bz);
            Block px = world.getBlock(bx + 1, by, bz);
            Block ny = world.getBlock(bx, by - 1, bz);
            Block py = world.getBlock(bx, by + 1, bz);
            Block nz = world.getBlock(bx, by, bz - 1);
            Block pz = world.getBlock(bx, by, bz + 1);
            int drawMask = 0;
            if(nx != null && !nx.isOpaque())
                drawMask |= DMaskNX;
            if(px != null && !px.isOpaque())
                drawMask |= DMaskPX;
            if(ny != null && !ny.isOpaque())
                drawMask |= DMaskNY;
            if(py != null && !py.isOpaque())
                drawMask |= DMaskPY;
            if(nz != null && !nz.isOpaque())
                drawMask |= DMaskNZ;
            if(pz != null && !pz.isOpaque())
                drawMask |= DMaskPZ;
            Block nx2 = world.getBlock(bbx - 1, bby, bbz);
            Block px2 = world.getBlock(bbx + 1, bby, bbz);
            Block ny2 = world.getBlock(bbx, bby - 1, bbz);
            Block py2 = world.getBlock(bbx, bby + 1, bbz);
            Block nz2 = world.getBlock(bbx, bby, bbz - 1);
            Block pz2 = world.getBlock(bbx, bby, bbz + 1);
            int drawMask2 = 0;
            if(nx2 != null && !nx2.isOpaque())
                drawMask2 |= DMaskNX;
            if(px2 != null && !px2.isOpaque())
                drawMask2 |= DMaskPX;
            if(ny2 != null && !ny2.isOpaque())
                drawMask2 |= DMaskNY;
            if(py2 != null && !py2.isOpaque())
                drawMask2 |= DMaskPY;
            if(nz2 != null && !nz2.isOpaque())
                drawMask2 |= DMaskNZ;
            if(pz2 != null && !pz2.isOpaque())
                drawMask2 |= DMaskPZ;
            if(drawMask != drawMask2)
                return false;
            return furnaceIsBurning() == b.furnaceIsBurning();
        }
        case BTLadder:
        case BTPistonHead:
        case BTStickyPistonHead:
        case BTRedstoneTorchOff:
        case BTRedstoneTorchOn:
        case BTTorch:
            return this.data.orientation == b.data.orientation;
        case BTLever:
        case BTPiston:
        case BTStickyPiston:
        case BTStoneButton:
        case BTWoodButton:
        case BTRedstoneDustOff:
        case BTRedstoneDustOn:
            return this.data.orientation == b.data.orientation
                    && this.data.intdata == b.data.intdata;
        case BTRedstoneRepeaterOff:
        case BTRedstoneRepeaterOn:
        {
            if(this.data.orientation != b.data.orientation)
                return false;
            boolean isThisLocked = redstoneRepeaterIsLatched(bx, by, bz);
            boolean isBLocked = redstoneRepeaterIsLatched(bbx, bby, bbz);
            if(isThisLocked != isBLocked)
                return false;
            if(isThisLocked)
                return true;
            return this.data.intdata == b.data.intdata;
        }
        case BTLava:
        case BTWater:
        {
            // TODO finish
            return false;
        }
        }
        return false;
    }

    private int concatHash(int oldHash, int newHash)
    {
        return oldHash + 37 * newHash;
    }

    /** @param bx
     *            this block's x coordinate
     * @param by
     *            this block's y coordinate
     * @param bz
     *            this block's z coordinate
     * @return the hash code for this block */
    public int getDrawHashcode(int bx, int by, int bz)
    {
        int hash = this.type.value;
        if(this.lighting != null)
        {
            for(int i = 0; i < this.lighting.length; i++)
            {
                hash = concatHash(hash, this.lighting[i]);
            }
        }
        switch(this.type.drawType)
        {
        case BDTButton:
        case BDTItem:
        case BDTNone:
        case BDTSolidAllSides:
        case BDTTorch:
            break;
        case BDTCustom:
            break;
        case BDTLiquid:
            // TODO finish
            break;
        case BDTSolid:
        {
            Block nx = world.getBlock(bx - 1, by, bz);
            Block px = world.getBlock(bx + 1, by, bz);
            Block ny = world.getBlock(bx, by - 1, bz);
            Block py = world.getBlock(bx, by + 1, bz);
            Block nz = world.getBlock(bx, by, bz - 1);
            Block pz = world.getBlock(bx, by, bz + 1);
            int drawMask = 0;
            if(nx != null && !nx.isOpaque())
                drawMask |= DMaskNX;
            if(px != null && !px.isOpaque())
                drawMask |= DMaskPX;
            if(ny != null && !ny.isOpaque())
                drawMask |= DMaskNY;
            if(py != null && !py.isOpaque())
                drawMask |= DMaskPY;
            if(nz != null && !nz.isOpaque())
                drawMask |= DMaskNZ;
            if(pz != null && !pz.isOpaque())
                drawMask |= DMaskPZ;
            hash = concatHash(hash, drawMask);
            break;
        }
        }
        switch(this.type)
        {
        case BTLast:
        case BTSun:
        case BTMoon:
            return hash;
        case BTDeleteBlock:
            hash = concatHash(hash, this.data.intdata);
            return hash;
        case BTBedrock:
        case BTBlazePowder:
        case BTBlazeRod:
        case BTChest:
        case BTCoal:
        case BTCoalOre:
        case BTCobblestone:
        case BTDiamond:
        case BTDiamondOre:
        case BTDiamondPick:
        case BTDiamondShovel:
        case BTDirt:
        case BTEmerald:
        case BTEmeraldOre:
        case BTEmpty:
        case BTGlass:
        case BTGoldIngot:
        case BTGoldOre:
        case BTGoldPick:
        case BTGoldShovel:
        case BTGrass:
        case BTGravel:
        case BTGunpowder:
        case BTIronIngot:
        case BTIronOre:
        case BTIronPick:
        case BTIronShovel:
        case BTLapisLazuli:
        case BTLapisLazuliOre:
        case BTObsidian:
        case BTPlank:
        case BTRedstoneBlock:
        case BTRedstoneOre:
        case BTSand:
        case BTSapling:
        case BTSlime:
        case BTStick:
        case BTStone:
        case BTStonePick:
        case BTStoneShovel:
        case BTTNT:
        case BTWoodPick:
        case BTWoodShovel:
        case BTWorkbench:
        case BTStonePressurePlate:
        case BTWoodPressurePlate:
            return hash;
        case BTWood:
        case BTLeaves:
        {
            Block nx = world.getBlock(bx - 1, by, bz);
            Block px = world.getBlock(bx + 1, by, bz);
            Block ny = world.getBlock(bx, by - 1, bz);
            Block py = world.getBlock(bx, by + 1, bz);
            Block nz = world.getBlock(bx, by, bz - 1);
            Block pz = world.getBlock(bx, by, bz + 1);
            int drawMask = 0;
            if(nx != null && !nx.isOpaque())
                drawMask |= DMaskNX;
            if(px != null && !px.isOpaque())
                drawMask |= DMaskPX;
            if(ny != null && !ny.isOpaque())
                drawMask |= DMaskNY;
            if(py != null && !py.isOpaque())
                drawMask |= DMaskPY;
            if(nz != null && !nz.isOpaque())
                drawMask |= DMaskNZ;
            if(pz != null && !pz.isOpaque())
                drawMask |= DMaskPZ;
            hash = concatHash(hash, drawMask);
            return hash;
        }
        case BTFurnace:
        {
            Block nx = world.getBlock(bx - 1, by, bz);
            Block px = world.getBlock(bx + 1, by, bz);
            Block ny = world.getBlock(bx, by - 1, bz);
            Block py = world.getBlock(bx, by + 1, bz);
            Block nz = world.getBlock(bx, by, bz - 1);
            Block pz = world.getBlock(bx, by, bz + 1);
            int drawMask = 0;
            if(nx != null && !nx.isOpaque())
                drawMask |= DMaskNX;
            if(px != null && !px.isOpaque())
                drawMask |= DMaskPX;
            if(ny != null && !ny.isOpaque())
                drawMask |= DMaskNY;
            if(py != null && !py.isOpaque())
                drawMask |= DMaskPY;
            if(nz != null && !nz.isOpaque())
                drawMask |= DMaskNZ;
            if(pz != null && !pz.isOpaque())
                drawMask |= DMaskPZ;
            hash = concatHash(hash, drawMask);
            hash = concatHash(hash, furnaceIsBurning() ? 1 : 0);
            return hash;
        }
        case BTLadder:
        case BTPistonHead:
        case BTStickyPistonHead:
        case BTRedstoneTorchOff:
        case BTRedstoneTorchOn:
        case BTTorch:
            hash = concatHash(hash, this.data.orientation);
            return hash;
        case BTLever:
        case BTPiston:
        case BTStickyPiston:
        case BTStoneButton:
        case BTWoodButton:
        case BTRedstoneDustOff:
        case BTRedstoneDustOn:
            hash = concatHash(hash, this.data.orientation);
            hash = concatHash(hash, this.data.intdata);
            return hash;
        case BTRedstoneRepeaterOff:
        case BTRedstoneRepeaterOn:
        {
            hash = concatHash(hash, this.data.orientation);
            boolean isThisLocked = redstoneRepeaterIsLatched(bx, by, bz);
            if(isThisLocked)
            {
                hash = concatHash(hash, 12345);
                return hash;
            }
            hash = concatHash(hash, this.data.intdata);
            return hash;
        }
        case BTLava:
        case BTWater:
        {
            // TODO finish
            hash = concatHash(hash, this.data.intdata);
            hash = concatHash(hash, bx);
            hash = concatHash(hash, by);
            hash = concatHash(hash, bz);
            return hash;
        }
        }
        hash = concatHash(hash, 67281763);
        return hash;
    }

    /** @param bx
     *            this block's x coordinate
     * @param by
     *            this block's y coordinate
     * @param bz
     *            this block's z coordinate
     * @return if this block is cacheable */
    public boolean isCacheable(int bx, int by, int bz)
    {
        if(this.lighting == null)
            return false;
        switch(this.type)
        {
        case BTLast:
        case BTSun:
        case BTMoon:
        case BTDeleteBlock:
            return true;
        case BTBedrock:
        case BTBlazePowder:
        case BTBlazeRod:
        case BTChest:
        case BTCoal:
        case BTCoalOre:
        case BTCobblestone:
        case BTDiamond:
        case BTDiamondOre:
        case BTDiamondPick:
        case BTDiamondShovel:
        case BTDirt:
        case BTEmerald:
        case BTEmeraldOre:
        case BTEmpty:
        case BTGlass:
        case BTGoldIngot:
        case BTGoldOre:
        case BTGoldPick:
        case BTGoldShovel:
        case BTGrass:
        case BTGravel:
        case BTGunpowder:
        case BTIronIngot:
        case BTIronOre:
        case BTIronPick:
        case BTIronShovel:
        case BTLapisLazuli:
        case BTLapisLazuliOre:
        case BTLeaves:
        case BTObsidian:
        case BTPlank:
        case BTRedstoneBlock:
        case BTRedstoneOre:
        case BTSand:
        case BTSapling:
        case BTSlime:
        case BTStick:
        case BTStone:
        case BTStonePick:
        case BTStoneShovel:
        case BTTNT:
        case BTWood:
        case BTWoodPick:
        case BTWoodShovel:
        case BTWorkbench:
        case BTFurnace:
        case BTLadder:
        case BTPistonHead:
        case BTStickyPistonHead:
        case BTRedstoneTorchOff:
        case BTRedstoneTorchOn:
        case BTTorch:
        case BTLever:
        case BTPiston:
        case BTStickyPiston:
        case BTStoneButton:
        case BTWoodButton:
        case BTRedstoneDustOff:
        case BTRedstoneDustOn:
        case BTRedstoneRepeaterOff:
        case BTRedstoneRepeaterOn:
        case BTStonePressurePlate:
        case BTWoodPressurePlate:
            return true;
        case BTLava:
        case BTWater:
            // TODO finish
            return false;
        }
        return false;
    }

    /**
     * 
     */
    public void pressurePlatePress()
    {
        this.data.intdata = 1;
    }
}
