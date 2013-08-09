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

import org.voxels.BlockType.DyeColor;
import org.voxels.BlockType.ToolLevel;
import org.voxels.BlockType.ToolType;
import org.voxels.TextureAtlas.TextureHandle;
import org.voxels.generate.Tree;
import org.voxels.generate.Tree.TreeType;

/** block<BR/>
 * not thread safe
 * 
 * @author jacob */
public class Block implements GameObject
{
    private BlockType type;
    /***/
    public static final int CHEST_ROWS = 3;
    /***/
    public static final int CHEST_COLUMNS = 9;

    private static class Data
    {
        // public Image imageref = null;
        public int intdata = 0;
        public int step = 0;
        public int[] BlockCounts = null;
        public Block[] BlockTypes = null;
        public Block blockdata = null;
        public int srccount = 0;
        public int destcount = 0;
        public int orientation = -1;
        public double runTime = -1.0;
        public BlockType.DyeColor dyeColor = DyeColor.None;

        public Data()
        {
        }

        public Data(final Data rt)
        {
            this.intdata = rt.intdata;
            this.BlockCounts = rt.BlockCounts;
            this.BlockTypes = null;
            if(this.BlockCounts != null)
            {
                this.BlockCounts = new int[rt.BlockCounts.length];
                this.BlockTypes = new Block[rt.BlockCounts.length];
                for(int i = 0; i < rt.BlockCounts.length; i++)
                {
                    this.BlockCounts[i] = rt.BlockCounts[i];
                    this.BlockTypes[i] = rt.BlockTypes[i];
                }
            }
            this.blockdata = rt.blockdata;
            this.srccount = rt.srccount;
            this.destcount = rt.destcount;
            this.orientation = rt.orientation;
            this.runTime = rt.runTime;
            this.step = rt.step;
            this.dyeColor = rt.dyeColor;
        }
    }

    private Data data = new Data();
    private int sunlight = 0, scatteredSunlight = 0, light = 0;
    private int lighting[] = null;
    private int curSunlightFactor = 0;

    private Block(final BlockType newtype)
    {
        this.type = newtype;
    }

    private static final int DMaskNX = 0x20;
    private static final int DMaskPX = 0x10;
    private static final int DMaskNY = 0x8;
    private static final int DMaskPY = 0x4;
    private static final int DMaskNZ = 0x2;
    private static final int DMaskPZ = 0x1;

    /** @return if this block is opaque */
    public boolean isOpaque()
    {
        if(this.type == BlockType.BTPiston
                || this.type == BlockType.BTStickyPiston)
        {
            if(this.data.intdata == 0)
                return true;
            return false;
        }
        return this.type.isOpaque();
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
    public Block(final Block rt)
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
    public static Block NewDeleteBlock(final int frame)
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
    public static Block NewStoneButton(final int ontime, final int orientation)
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
    public static Block NewWoodButton(final int ontime, final int orientation)
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
        return NewFurnace(0, null, 0, 0);
    }

    public static final float FURNACE_SMELT_TIME = 10.0f;

    /** @param fuelleft
     *            the amount of fuel left
     * @param sourceblock
     *            the block that is currently being smelted
     * @param srccount
     *            the number of blocks left to smelt
     * @param destcount
     *            the number of blocks already smelted
     * @return new furnace */
    public static Block NewFurnace(final int fuelleft,
                                   final Block sourceblock,
                                   final int srccount,
                                   final int destcount)
    {
        Block retval = new Block(BlockType.BTFurnace);
        retval.data.intdata = fuelleft;
        retval.data.blockdata = sourceblock;
        retval.data.destcount = destcount;
        retval.data.srccount = srccount;
        retval.data.runTime = -1;
        if(srccount > 0 && fuelleft > 0)
            retval.data.runTime = world.getCurTime() + FURNACE_SMELT_TIME; // time
                                                                           // when
        // furnace
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
        retval.data.BlockCounts = new int[CHEST_ROWS * CHEST_COLUMNS];
        retval.data.BlockTypes = new Block[CHEST_ROWS * CHEST_COLUMNS];
        return retval;
    }

    /** @param treeType
     *            the tree type
     * @return new sapling */
    public static Block NewSapling(final TreeType treeType)
    {
        Block retval = new Block(BlockType.BTSapling);
        retval.data.intdata = treeType.ordinal();
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

    /** @param treeType
     *            the tree type
     * @param orientation
     *            the block orientation : <br/>
     *            <ul>
     *            <li>0 : ±y sides without bark</li>
     *            <li>1 : ±x sides without bark</li>
     *            <li>2 : ±z sides without bark</li>
     *            <li>3 : all sides with bark</li>
     *            </ul>
     * @return new wood block */
    public static Block NewWood(final Tree.TreeType treeType,
                                final int orientation)
    {
        Block retval = new Block(BlockType.BTWood);
        retval.data.intdata = treeType.ordinal();
        retval.data.orientation = Math.max(0, Math.min(3, orientation));
        return retval;
    }

    /**
     * 
     */
    public static final int MAX_LEAVES_DISTANCE = 4;

    /** @param treeType
     *            the tree type
     * @param distFromWood
     *            the distance from wood
     * @return new leaves block that can decay */
    public static Block NewLeaves(final Tree.TreeType treeType,
                                  final int distFromWood)
    {
        Block retval = new Block(BlockType.BTLeaves);
        retval.data.intdata = treeType.ordinal();
        retval.data.step = Math.max(0, Math.min(MAX_LEAVES_DISTANCE + 1,
                                                distFromWood));
        return retval;
    }

    /** @param treeType
     *            the tree type
     * @return new leaves block that can't decay */
    public static Block NewLeaves(final Tree.TreeType treeType)
    {
        Block retval = new Block(BlockType.BTLeaves);
        retval.data.intdata = treeType.ordinal();
        retval.data.step = -1;
        return retval;
    }

    /** @param treeType
     *            the tree type
     * @return new plank */
    public static Block NewPlank(final Tree.TreeType treeType)
    {
        Block retval = new Block(BlockType.BTPlank);
        retval.data.intdata = treeType.ordinal();
        return retval;
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
    public static Block NewLava(final int amount)
    {
        Block retval = new Block(BlockType.BTLava);
        retval.data.intdata = Math.min(8, Math.max(-8, amount));
        return retval;
    }

    /** @return new supported lava spring block */
    public static Block NewStationaryLava()
    {
        return NewLava(-8);
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
    public static Block NewWater(final int amount)
    {
        Block retval = new Block(BlockType.BTWater);
        retval.data.intdata = Math.min(8, Math.max(-8, amount));
        return retval;
    }

    /** @return new supported water spring block */
    public static Block NewStationaryWater()
    {
        return NewWater(-8);
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
    public static Block NewDeleteAnim(final float t)
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
    public static Block NewRedstoneDust(final int power, final int orientation)
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
    public static Block NewRedstoneTorch(final boolean state,
                                         final int orientation)
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
    public static Block NewTorch(final int orientation)
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
    public static Block NewLadder(final int orientation)
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
    public static Block NewRedstoneRepeater(final boolean value,
                                            final int stepsleft,
                                            final int delay,
                                            final int orientation)
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

    /** create a new redstone repeater
     * 
     * @return the new redstone repeater */
    public static Block NewRedstoneRepeater()
    {
        return NewRedstoneRepeater(false, 0, 1, -1);
    }

    /** create a new lever
     * 
     * @param value
     *            if the lever is on
     * @param orientation
     *            the new lever's orientation
     * @return the new lever */
    public static Block NewLever(final boolean value, final int orientation)
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
    public static Block
        NewPiston(final int orientation, final boolean extended)
    {
        Block retval = new Block(BlockType.BTPiston);
        if(orientation == -1)
            retval.data.orientation = 5;
        else
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
    public static Block NewStickyPiston(final int orientation,
                                        final boolean extended)
    {
        Block retval = new Block(BlockType.BTStickyPiston);
        if(orientation == -1)
            retval.data.orientation = 5;
        else
            retval.data.orientation = Math.max(0, Math.min(5, orientation));
        retval.data.intdata = (extended ? 1 : 0);
        retval.data.step = (extended ? 1 : 0);
        return retval;
    }

    /** @param orientation
     *            the new piston's head's orientation
     * @return new piston's head */
    public static Block NewPistonHead(final int orientation)
    {
        Block retval = new Block(BlockType.BTPistonHead);
        retval.data.orientation = Math.max(0, Math.min(5, orientation));
        return retval;
    }

    /** @param orientation
     *            the new sticky piston's head's orientation
     * @return new sticky piston's head */
    public static Block NewStickyPistonHead(final int orientation)
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

    /** @param depth
     *            the depth (1 - 8)
     * @return new snow */
    public static Block NewSnow(final int depth)
    {
        Block retval = new Block(BlockType.BTSnow);
        retval.data.intdata = Math.max(1, Math.min(8, depth));
        return retval;
    }

    /** @param orientation
     *            orientation of the new vines
     * @return new vines */
    public static Block NewVines(final int orientation)
    {
        Block retval = new Block(BlockType.BTVines);
        retval.data.orientation = Math.max(0, Math.min(3, orientation));
        return retval;
    }

    /** @return new wood axe */
    public static Block NewWoodAxe()
    {
        return new Block(BlockType.BTWoodAxe);
    }

    /** @return new stone axe */
    public static Block NewStoneAxe()
    {
        return new Block(BlockType.BTStoneAxe);
    }

    /** @return new iron axe */
    public static Block NewIronAxe()
    {
        return new Block(BlockType.BTIronAxe);
    }

    /** @return new gold axe */
    public static Block NewGoldAxe()
    {
        return new Block(BlockType.BTGoldAxe);
    }

    /** @return new diamond axe */
    public static Block NewDiamondAxe()
    {
        return new Block(BlockType.BTDiamondAxe);
    }

    /** @return new bucket */
    public static Block NewBucket()
    {
        return new Block(BlockType.BTBucket);
    }

    /** @return new shears */
    public static Block NewShears()
    {
        return new Block(BlockType.BTShears);
    }

    /** create a new redstone comparator
     * 
     * @param subtractMode
     *            if the comparator is in subtract mode
     * @param currentOutput
     *            the output strength
     * @param orientation
     *            the orientation
     * @return the new redstone comparator */
    public static Block NewRedstoneComparator(final boolean subtractMode,
                                              final int currentOutput,
                                              final int orientation)
    {
        Block retval = new Block(BlockType.BTRedstoneComparator);
        retval.data.orientation = Math.max(0, Math.min(3, orientation));
        retval.data.intdata = Math.max(0, Math.min(15, currentOutput));
        retval.data.step = subtractMode ? 1 : 0;
        return retval;
    }

    public static Block NewQuartz()
    {
        return new Block(BlockType.BTQuartz);
    }

    public static Block NewDispenser(final int orientation)
    {
        Block retval = new Block(BlockType.BTDispenser);
        retval.data.intdata = orientation;
        retval.data.BlockCounts = new int[DISPENSER_DROPPER_ROWS
                * DISPENSER_DROPPER_COLUMNS];
        retval.data.BlockTypes = new Block[DISPENSER_DROPPER_ROWS
                * DISPENSER_DROPPER_COLUMNS];
        if(orientation == -1)
            retval.data.orientation = 2;
        else
            retval.data.orientation = Math.max(0, Math.min(5, orientation));
        return retval;
    }

    public static Block NewDropper(final int orientation)
    {
        Block retval = new Block(BlockType.BTDropper);
        retval.data.intdata = orientation;
        retval.data.BlockCounts = new int[DISPENSER_DROPPER_ROWS
                * DISPENSER_DROPPER_COLUMNS];
        retval.data.BlockTypes = new Block[DISPENSER_DROPPER_ROWS
                * DISPENSER_DROPPER_COLUMNS];
        if(orientation == -1)
            retval.data.orientation = 2;
        else
            retval.data.orientation = Math.max(0, Math.min(5, orientation));
        return retval;
    }

    public static Block NewCobweb()
    {
        return new Block(BlockType.BTCobweb);
    }

    public static Block NewString()
    {
        return new Block(BlockType.BTString);
    }

    public static Block NewBow()
    {
        return new Block(BlockType.BTBow);
    }

    public static final int HOPPER_SLOTS = 5;

    public static Block NewHopper(final int orientation)
    {
        Block retval = new Block(BlockType.BTHopper);
        retval.data.intdata = 0;
        retval.data.BlockCounts = new int[HOPPER_SLOTS];
        retval.data.BlockTypes = new Block[HOPPER_SLOTS];
        if(orientation < 0 || orientation > 4)
            retval.data.orientation = 4;
        else
            retval.data.orientation = orientation;
        return retval;
    }

    public static Block NewCactus()
    {
        return new Block(BlockType.BTCactus);
    }

    public static Block NewRedMushroom()
    {
        return new Block(BlockType.BTRedMushroom);
    }

    public static Block NewBrownMushroom()
    {
        return new Block(BlockType.BTBrownMushroom);
    }

    public static Block NewDeadBush()
    {
        return new Block(BlockType.BTDeadBush);
    }

    public static Block NewDandelion()
    {
        return new Block(BlockType.BTDandelion);
    }

    public static Block NewRose()
    {
        return new Block(BlockType.BTRose);
    }

    public static Block NewTallGrass()
    {
        return new Block(BlockType.BTTallGrass);
    }

    public static Block NewSeeds(final int growthLevel)
    {
        Block retval = new Block(BlockType.BTSeeds);
        retval.data.intdata = Math.max(0, Math.min(7, growthLevel));
        return retval;
    }

    public static Block NewWheat()
    {
        return new Block(BlockType.BTWheat);
    }

    public static Block NewFarmland(final boolean isWet)
    {
        Block retval = new Block(BlockType.BTFarmland);
        retval.data.intdata = isWet ? 1 : 0;
        return retval;
    }

    public static Block NewWoodHoe()
    {
        return new Block(BlockType.BTWoodHoe);
    }

    public static Block NewStoneHoe()
    {
        return new Block(BlockType.BTStoneHoe);
    }

    public static Block NewIronHoe()
    {
        return new Block(BlockType.BTIronHoe);
    }

    public static Block NewGoldHoe()
    {
        return new Block(BlockType.BTGoldHoe);
    }

    public static Block NewDiamondHoe()
    {
        return new Block(BlockType.BTDiamondHoe);
    }

    public static Block NewCocoa(final int stage, final int orientation)
    {
        Block retval = new Block(BlockType.BTCocoa);
        retval.data.intdata = Math.max(0, Math.min(2, stage));
        retval.data.orientation = Math.max(0, Math.min(3, orientation));
        return retval;
    }

    public static Block NewInkSac()
    {
        return new Block(BlockType.BTInkSac);
    }

    public static Block NewRoseRed()
    {
        return new Block(BlockType.BTRoseRed);
    }

    public static Block NewCactusGreen()
    {
        return new Block(BlockType.BTCactusGreen);
    }

    public static Block NewPurpleDye()
    {
        return new Block(BlockType.BTPurpleDye);
    }

    public static Block NewCyanDye()
    {
        return new Block(BlockType.BTCyanDye);
    }

    public static Block NewLightGrayDye()
    {
        return new Block(BlockType.BTLightGrayDye);
    }

    public static Block NewGrayDye()
    {
        return new Block(BlockType.BTGrayDye);
    }

    public static Block NewPinkDye()
    {
        return new Block(BlockType.BTPinkDye);
    }

    public static Block NewLimeDye()
    {
        return new Block(BlockType.BTLimeDye);
    }

    public static Block NewDandelionYellow()
    {
        return new Block(BlockType.BTDandelionYellow);
    }

    public static Block NewLightBlueDye()
    {
        return new Block(BlockType.BTLightBlueDye);
    }

    public static Block NewMagentaDye()
    {
        return new Block(BlockType.BTMagentaDye);
    }

    public static Block NewOrangeDye()
    {
        return new Block(BlockType.BTOrangeDye);
    }

    public static Block NewBoneMeal()
    {
        return new Block(BlockType.BTBoneMeal);
    }

    public static Block NewBone()
    {
        return new Block(BlockType.BTBone);
    }

    public static Block NewWool(final BlockType.DyeColor color)
    {
        Block retval = new Block(BlockType.BTWool);
        if(color == DyeColor.None || color == null)
            retval.data.dyeColor = DyeColor.BoneMeal;
        else
            retval.data.dyeColor = color;
        return retval;
    }

    private static Vector drawFace_t1 = Vector.allocate();
    private static Vector drawFace_t2 = Vector.allocate();

    private static RenderingStream drawFace(final RenderingStream rs,
                                            final TextureHandle texture,
                                            final Vector p1,
                                            final Vector p2,
                                            final Vector p3,
                                            final Vector p4,
                                            final float u1,
                                            final float v1,
                                            final float u2,
                                            final float v2,
                                            final float u3,
                                            final float v3,
                                            final float u4,
                                            final float v4,
                                            final int bx,
                                            final int by,
                                            final int bz,
                                            final boolean doublesided,
                                            final boolean isEntity,
                                            final boolean isAsItem,
                                            final boolean isItemGlowing)
    {
        float c1, c2, c3, c4;
        Vector normal = Vector.sub(drawFace_t1, p2, p1)
                              .crossAndSet(Vector.sub(drawFace_t2, p3, p1))
                              .normalizeAndSet();
        if(isAsItem || isItemGlowing)
        {
            c1 = normal.dot(Vector.Y);
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
        rs.beginTriangle(texture);
        rs.vertex(p1, u1, v1, c1, c1, c1, 1.0f);
        rs.vertex(p2, u2, v2, c2, c2, c2, 1.0f);
        rs.vertex(p3, u3, v3, c3, c3, c3, 1.0f);
        rs.endTriangle();
        rs.beginTriangle(texture);
        rs.vertex(p3, u3, v3, c3, c3, c3, 1.0f);
        rs.vertex(p4, u4, v4, c4, c4, c4, 1.0f);
        rs.vertex(p1, u1, v1, c1, c1, c1, 1.0f);
        rs.endTriangle();
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

    @SuppressWarnings("unused")
    private static RenderingStream
        drawFace(final RenderingStream rs,
                 final TextureAtlas.TextureHandle texture,
                 final Vector p1,
                 final Vector p2,
                 final Vector p3,
                 final float u1,
                 final float v1,
                 final float u2,
                 final float v2,
                 final float u3,
                 final float v3,
                 final int bx,
                 final int by,
                 final int bz,
                 final boolean doublesided,
                 final boolean isEntity,
                 final boolean isAsItem,
                 final boolean isItemGlowing)
    {
        float c1, c2, c3;
        Vector normal = Vector.sub(drawFace_t1, p2, p1)
                              .crossAndSet(Vector.sub(drawFace_t2, p3, p1))
                              .normalizeAndSet();
        if(isAsItem || isItemGlowing)
        {
            c1 = normal.dot(Vector.Y);
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
        rs.beginTriangle(texture);
        rs.vertex(p1, u1, v1, c1, c1, c1, 1.0f);
        rs.vertex(p2, u2, v2, c2, c2, c2, 1.0f);
        rs.vertex(p3, u3, v3, c3, c3, c3, 1.0f);
        rs.endTriangle();
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

    private static RenderingStream drawFace(final RenderingStream rs,
                                            final TextureHandle texture,
                                            final Vector p1,
                                            final Vector p2,
                                            final Vector p3,
                                            final Vector p4,
                                            final float u1,
                                            final float v1,
                                            final float u2,
                                            final float v2,
                                            final float u3,
                                            final float v3,
                                            final float u4,
                                            final float v4,
                                            final int bx,
                                            final int by,
                                            final int bz,
                                            final boolean doublesided,
                                            final boolean isEntity,
                                            final boolean isAsItem,
                                            final boolean isItemGlowing,
                                            final float r,
                                            final float g,
                                            final float b)
    {
        float c1, c2, c3, c4;
        Vector normal = Vector.sub(drawFace_t1, p2, p1)
                              .crossAndSet(Vector.sub(drawFace_t2, p3, p1))
                              .normalizeAndSet();
        if(isAsItem || isItemGlowing)
        {
            c1 = normal.dot(Vector.Y);
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
        rs.beginTriangle(texture);
        rs.vertex(p1, u1, v1, c1 * r, c1 * g, c1 * b, 1.0f);
        rs.vertex(p2, u2, v2, c2 * r, c2 * g, c2 * b, 1.0f);
        rs.vertex(p3, u3, v3, c3 * r, c3 * g, c3 * b, 1.0f);
        rs.endTriangle();
        rs.beginTriangle(texture);
        rs.vertex(p3, u3, v3, c3 * r, c3 * g, c3 * b, 1.0f);
        rs.vertex(p4, u4, v4, c4 * r, c4 * g, c4 * b, 1.0f);
        rs.vertex(p1, u1, v1, c1 * r, c1 * g, c1 * b, 1.0f);
        rs.endTriangle();
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
                     isItemGlowing,
                     r,
                     g,
                     b);
        return rs;
    }

    private static RenderingStream
        drawFace(final RenderingStream rs,
                 final TextureAtlas.TextureHandle texture,
                 final Vector p1,
                 final Vector p2,
                 final Vector p3,
                 final float u1,
                 final float v1,
                 final float u2,
                 final float v2,
                 final float u3,
                 final float v3,
                 final int bx,
                 final int by,
                 final int bz,
                 final boolean doublesided,
                 final boolean isEntity,
                 final boolean isAsItem,
                 final boolean isItemGlowing,
                 final float r,
                 final float g,
                 final float b)
    {
        float c1, c2, c3;
        Vector normal = Vector.sub(drawFace_t1, p2, p1)
                              .crossAndSet(Vector.sub(drawFace_t2, p3, p1))
                              .normalizeAndSet();
        if(isAsItem || isItemGlowing)
        {
            c1 = normal.dot(Vector.Y);
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
        rs.beginTriangle(texture);
        rs.vertex(p1, u1, v1, c1 * r, c1 * g, c1 * b, 1.0f);
        rs.vertex(p2, u2, v2, c2 * r, c2 * g, c2 * b, 1.0f);
        rs.vertex(p3, u3, v3, c3 * r, c3 * g, c3 * b, 1.0f);
        rs.endTriangle();
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
                     isItemGlowing,
                     r,
                     g,
                     b);
        return rs;
    }

    private static Matrix drawItem_localToWorld = new Matrix();
    private static Vector drawItem_p1 = Vector.allocate();
    private static Vector drawItem_p2 = Vector.allocate();
    private static Vector drawItem_p3 = Vector.allocate();
    private static Vector drawItem_p4 = Vector.allocate();

    private static RenderingStream drawItem(final RenderingStream rs,
                                            final Matrix localToBlock,
                                            final Matrix blockToWorld,
                                            final int bx,
                                            final int by,
                                            final int bz,
                                            final TextureHandle img,
                                            final boolean isEntity,
                                            final boolean isAsItem)
    {
        Matrix localToWorld = localToBlock.concat(drawItem_localToWorld,
                                                  blockToWorld);
        Vector p1 = localToWorld.apply(drawItem_p1, Vector.ZERO);
        Vector p2 = localToWorld.apply(drawItem_p2, Vector.X);
        Vector p3 = localToWorld.apply(drawItem_p3, Vector.Y);
        Vector p4 = localToWorld.apply(drawItem_p4, Vector.XY);
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

    private static RenderingStream drawItem(final RenderingStream rs,
                                            final Matrix localToBlock,
                                            final Matrix blockToWorld,
                                            final int bx,
                                            final int by,
                                            final int bz,
                                            final TextureHandle img,
                                            final boolean isEntity,
                                            final boolean isAsItem,
                                            final float minu,
                                            final float minv,
                                            final float maxu,
                                            final float maxv)
    {
        Matrix localToWorld = localToBlock.concat(drawItem_localToWorld,
                                                  blockToWorld);
        Vector p1 = localToWorld.apply(drawItem_p1, Vector.ZERO);
        Vector p2 = localToWorld.apply(drawItem_p2, Vector.X);
        Vector p3 = localToWorld.apply(drawItem_p3, Vector.Y);
        Vector p4 = localToWorld.apply(drawItem_p4, Vector.XY);
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

    private static RenderingStream drawItem(final RenderingStream rs,
                                            final Matrix localToBlock,
                                            final Matrix blockToWorld,
                                            final int bx,
                                            final int by,
                                            final int bz,
                                            final TextureHandle img,
                                            final boolean isEntity,
                                            final boolean isAsItem,
                                            final float r,
                                            final float g,
                                            final float b)
    {
        Matrix localToWorld = localToBlock.concat(drawItem_localToWorld,
                                                  blockToWorld);
        Vector p1 = localToWorld.apply(drawItem_p1, Vector.ZERO);
        Vector p2 = localToWorld.apply(drawItem_p2, Vector.X);
        Vector p3 = localToWorld.apply(drawItem_p3, Vector.Y);
        Vector p4 = localToWorld.apply(drawItem_p4, Vector.XY);
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
                 false,
                 r,
                 g,
                 b);
        return rs;
    }

    @SuppressWarnings("unused")
    private static RenderingStream drawItem(final RenderingStream rs,
                                            final Matrix localToBlock,
                                            final Matrix blockToWorld,
                                            final int bx,
                                            final int by,
                                            final int bz,
                                            final TextureHandle img,
                                            final boolean isEntity,
                                            final boolean isAsItem,
                                            final float minu,
                                            final float minv,
                                            final float maxu,
                                            final float maxv,
                                            final float r,
                                            final float g,
                                            final float b)
    {
        Matrix localToWorld = localToBlock.concat(drawItem_localToWorld,
                                                  blockToWorld);
        Vector p1 = localToWorld.apply(drawItem_p1, Vector.ZERO);
        Vector p2 = localToWorld.apply(drawItem_p2, Vector.X);
        Vector p3 = localToWorld.apply(drawItem_p3, Vector.Y);
        Vector p4 = localToWorld.apply(drawItem_p4, Vector.XY);
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
                 false,
                 r,
                 g,
                 b);
        return rs;
    }

    private static Vector internalDraw_p1 = Vector.allocate();
    private static Vector internalDraw_p2 = Vector.allocate();
    private static Vector internalDraw_p3 = Vector.allocate();
    private static Vector internalDraw_p4 = Vector.allocate();
    private static Vector internalDraw_p5 = Vector.allocate();
    private static Vector internalDraw_p6 = Vector.allocate();
    private static Vector internalDraw_p7 = Vector.allocate();
    private static Vector internalDraw_p8 = Vector.allocate();
    private static Matrix internalDraw_localToWorld = new Matrix();

    private RenderingStream internalDraw(final RenderingStream rs,
                                         final int drawMask,
                                         final Matrix localToBlock,
                                         final Matrix blockToWorld,
                                         final int bx,
                                         final int by,
                                         final int bz,
                                         final TextureHandle img,
                                         final boolean doubleSided,
                                         final boolean isEntity,
                                         final boolean isAsItem)
    {
        if(drawMask == 0)
            return rs;
        Matrix localToWorld = localToBlock.concat(internalDraw_localToWorld,
                                                  blockToWorld);
        Vector p1 = localToWorld.apply(internalDraw_p1, Vector.ZERO);
        Vector p2 = localToWorld.apply(internalDraw_p2, Vector.X);
        Vector p3 = localToWorld.apply(internalDraw_p3, Vector.Y);
        Vector p4 = localToWorld.apply(internalDraw_p4, Vector.XY);
        Vector p5 = localToWorld.apply(internalDraw_p5, Vector.Z);
        Vector p6 = localToWorld.apply(internalDraw_p6, Vector.XZ);
        Vector p7 = localToWorld.apply(internalDraw_p7, Vector.YZ);
        Vector p8 = localToWorld.apply(internalDraw_p8, Vector.XYZ);
        if((drawMask & DMaskNX) != 0)
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
        if((drawMask & DMaskPX) != 0)
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
        if((drawMask & DMaskNY) != 0)
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
        if((drawMask & DMaskPY) != 0)
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
        if((drawMask & DMaskNZ) != 0)
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
        if((drawMask & DMaskPZ) != 0)
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

    private RenderingStream internalDraw(final RenderingStream rs,
                                         final int drawMask,
                                         final Matrix localToBlock,
                                         final Matrix blockToWorld,
                                         final int bx,
                                         final int by,
                                         final int bz,
                                         final TextureHandle img,
                                         final boolean doubleSided,
                                         final boolean isEntity,
                                         final boolean isAsItem,
                                         final float r,
                                         final float g,
                                         final float b)
    {
        if(drawMask == 0)
            return rs;
        Matrix localToWorld = localToBlock.concat(internalDraw_localToWorld,
                                                  blockToWorld);
        Vector p1 = localToWorld.apply(internalDraw_p1, Vector.ZERO);
        Vector p2 = localToWorld.apply(internalDraw_p2, Vector.X);
        Vector p3 = localToWorld.apply(internalDraw_p3, Vector.Y);
        Vector p4 = localToWorld.apply(internalDraw_p4, Vector.XY);
        Vector p5 = localToWorld.apply(internalDraw_p5, Vector.Z);
        Vector p6 = localToWorld.apply(internalDraw_p6, Vector.XZ);
        Vector p7 = localToWorld.apply(internalDraw_p7, Vector.YZ);
        Vector p8 = localToWorld.apply(internalDraw_p8, Vector.XYZ);
        if((drawMask & DMaskNX) != 0)
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
                     (this.type == BlockType.BTSun || this.type == BlockType.BTMoon),
                     r,
                     g,
                     b);
        }
        if((drawMask & DMaskPX) != 0)
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
                     (this.type == BlockType.BTSun || this.type == BlockType.BTMoon),
                     r,
                     g,
                     b);
        }
        if((drawMask & DMaskNY) != 0)
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
                     (this.type == BlockType.BTSun || this.type == BlockType.BTMoon),
                     r,
                     g,
                     b);
        }
        if((drawMask & DMaskPY) != 0)
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
                     (this.type == BlockType.BTSun || this.type == BlockType.BTMoon),
                     r,
                     g,
                     b);
        }
        if((drawMask & DMaskNZ) != 0)
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
                     (this.type == BlockType.BTSun || this.type == BlockType.BTMoon),
                     r,
                     g,
                     b);
        }
        if((drawMask & DMaskPZ) != 0)
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
                     (this.type == BlockType.BTSun || this.type == BlockType.BTMoon),
                     r,
                     g,
                     b);
        }
        return rs;
    }

    private static final boolean IGNORE_FALLING_FLUID = false;

    @SuppressWarnings("unused")
    private float getFluidHeight(final int x, final int y, final int z)
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

    private float getFluidBottom(final int x,
                                 final int y,
                                 final int z,
                                 final int bx,
                                 final int by,
                                 final int bz,
                                 final int dx,
                                 final int dz,
                                 final float maxV)
    {
        Block b = world.getBlock(bx + dx, by, bz + dz);
        if(b == null || b.type != this.type)
            return 0.0f;
        float h = b.getFluidHeight(x, y, z);
        return Math.min(maxV, h);
    }

    private float interpolate(final float t, final float a, final float b)
    {
        return (b - a) * t + a;
    }

    private Vector interpolate(final Vector dest,
                               final float t,
                               final Vector a,
                               final Vector b)
    {
        return Vector.sub(dest, b, a).mulAndSet(t).addAndSet(a);
    }

    private static Vector drawFluidFace_nunv_p = Vector.allocate();
    private static Vector drawFluidFace_nupv_p = Vector.allocate();
    private static Vector drawFluidFace_punv_p = Vector.allocate();
    private static Vector drawFluidFace_pupv_p = Vector.allocate();

    private RenderingStream drawFluidFace(final RenderingStream rs,
                                          final TextureHandle texture,
                                          final Vector nunv,
                                          final Vector punv,
                                          final Vector pupv,
                                          final Vector nupv,
                                          final float minu,
                                          final float maxu,
                                          final float minv,
                                          final float maxv,
                                          final float tnu,
                                          final float bnu,
                                          final float tpu,
                                          final float bpu,
                                          final int bx,
                                          final int by,
                                          final int bz,
                                          final float r,
                                          final float g,
                                          final float b)
    {
        Vector nunv_p = interpolate(drawFluidFace_nunv_p, bnu, nunv, nupv);
        float nunv_u = minu;
        float nunv_v = interpolate(bnu, minv, maxv);
        Vector nupv_p = interpolate(drawFluidFace_nupv_p, tnu, nunv, nupv);
        float nupv_u = minu;
        float nupv_v = interpolate(tnu, minv, maxv);
        Vector punv_p = interpolate(drawFluidFace_punv_p, bpu, punv, pupv);
        float punv_u = maxu;
        float punv_v = interpolate(bpu, minv, maxv);
        Vector pupv_p = interpolate(drawFluidFace_pupv_p, tpu, punv, pupv);
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
                 (this.type == BlockType.BTSun || this.type == BlockType.BTMoon),
                 r,
                 g,
                 b);
        return rs;
    }

    private boolean drawFluidDrawsFace(final Block b, final float myHeight)
    {
        if(b == null)
            return false;
        if(b.isOpaque())
            return false;
        if(b.getType() != this.type)
            return true;
        if(b.getHeight() >= myHeight)
            return false;
        return true;
    }

    private static Vector drawFluid_p1 = Vector.allocate();
    private static Vector drawFluid_p2 = Vector.allocate();
    private static Vector drawFluid_p3 = Vector.allocate();
    private static Vector drawFluid_p4 = Vector.allocate();
    private static Vector drawFluid_p5 = Vector.allocate();
    private static Vector drawFluid_p6 = Vector.allocate();
    private static Vector drawFluid_p7 = Vector.allocate();
    private static Vector drawFluid_p8 = Vector.allocate();
    private static Vector drawFluid_pCenter = Vector.allocate();
    private static Vector drawFluid_nunv_p = Vector.allocate();
    private static Vector drawFluid_nupv_p = Vector.allocate();
    private static Vector drawFluid_punv_p = Vector.allocate();
    private static Vector drawFluid_pupv_p = Vector.allocate();

    private RenderingStream drawFluid(final RenderingStream rs,
                                      final Matrix blockToWorld,
                                      final int bx,
                                      final int by,
                                      final int bz,
                                      final TextureHandle img)
    {
        Block nx = world.getBlock(bx - 1, by, bz);
        Block px = world.getBlock(bx + 1, by, bz);
        Block ny = world.getBlock(bx, by - 1, bz);
        Block py = world.getBlock(bx, by + 1, bz);
        Block nz = world.getBlock(bx, by, bz - 1);
        Block pz = world.getBlock(bx, by, bz + 1);
        int drawMask = 0;
        float height = getHeight();
        if(drawFluidDrawsFace(nx, height))
            drawMask |= DMaskNX;
        if(drawFluidDrawsFace(px, height))
            drawMask |= DMaskPX;
        if(ny != null && !ny.isOpaque()
                && (ny.type != this.type || ny.getHeight() < 1.0f))
            drawMask |= DMaskNY;
        if(py != null && !py.isOpaque() && py.type != this.type)
            drawMask |= DMaskPY;
        if(drawFluidDrawsFace(nz, height))
            drawMask |= DMaskNZ;
        if(drawFluidDrawsFace(pz, height))
            drawMask |= DMaskPZ;
        if(height < 1.0f)
            drawMask |= DMaskPY;
        if(drawMask == 0)
            return rs;
        float r = 1, g = 1, b = 1;
        if(this.type == BlockType.BTWater)
        {
            r = world.getBiomeWaterColorR(bx, bz);
            g = world.getBiomeWaterColorG(bx, bz);
            b = world.getBiomeWaterColorB(bx, bz);
        }
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
        Vector p1 = blockToWorld.apply(drawFluid_p1, Vector.ZERO);
        Vector p2 = blockToWorld.apply(drawFluid_p2, Vector.X);
        Vector p3 = blockToWorld.apply(drawFluid_p3, Vector.Y);
        Vector p4 = blockToWorld.apply(drawFluid_p4, Vector.XY);
        Vector p5 = blockToWorld.apply(drawFluid_p5, Vector.Z);
        Vector p6 = blockToWorld.apply(drawFluid_p6, Vector.XZ);
        Vector p7 = blockToWorld.apply(drawFluid_p7, Vector.YZ);
        Vector p8 = blockToWorld.apply(drawFluid_p8, Vector.XYZ);
        Vector pCenter = blockToWorld.apply(drawFluid_pCenter,
                                            Vector.set(drawFluid_pCenter,
                                                       0.5f,
                                                       avgt,
                                                       0.5f));
        if((drawMask & DMaskNX) != 0)
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
                          bz,
                          r,
                          g,
                          b);
        }
        if((drawMask & DMaskPX) != 0)
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
                          bz,
                          r,
                          g,
                          b);
        }
        if((drawMask & DMaskNY) != 0)
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
                     (this.type == BlockType.BTSun || this.type == BlockType.BTMoon),
                     r,
                     g,
                     b);
        }
        if((drawMask & DMaskNZ) != 0)
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
                          bz,
                          r,
                          g,
                          b);
        }
        if((drawMask & DMaskPZ) != 0)
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
                          bz,
                          r,
                          g,
                          b);
        }
        if((drawMask & 0x4) != 0) // +Y
        {
            final float minu = 0.25f, maxu = 0.5f, minv = 0.0f, maxv = 0.5f;
            final float cu = (minu + maxu) / 2.0f, cv = (minv + maxv) / 2.0f;
            // p3, p7, p8, p4
            Vector nupv_p = interpolate(drawFluid_nupv_p, t00, p1, p3);
            Vector nunv_p = interpolate(drawFluid_nunv_p, t01, p5, p7);
            Vector punv_p = interpolate(drawFluid_punv_p, t11, p6, p8);
            Vector pupv_p = interpolate(drawFluid_pupv_p, t10, p2, p4);
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
                     (this.type == BlockType.BTSun || this.type == BlockType.BTMoon),
                     r,
                     g,
                     b);
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
                     (this.type == BlockType.BTSun || this.type == BlockType.BTMoon),
                     r,
                     g,
                     b);
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
                     (this.type == BlockType.BTSun || this.type == BlockType.BTMoon),
                     r,
                     g,
                     b);
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
                     (this.type == BlockType.BTSun || this.type == BlockType.BTMoon),
                     r,
                     g,
                     b);
        }
        return rs;
    }

    private RenderingStream drawSolid(final RenderingStream rs,
                                      final Matrix blockToWorld,
                                      final int bx,
                                      final int by,
                                      final int bz,
                                      final boolean drawAllSides,
                                      final TextureHandle img,
                                      final boolean isEntity,
                                      final boolean isAsItem)
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
                     Matrix.IDENTITY,
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

    private RenderingStream drawSolid(final RenderingStream rs,
                                      final Matrix blockToWorld,
                                      final int bx,
                                      final int by,
                                      final int bz,
                                      final boolean drawAllSides,
                                      final boolean isEntity,
                                      final boolean isAsItem)
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

    private RenderingStream drawSolid(final RenderingStream rs,
                                      final Matrix blockToWorld,
                                      final int bx,
                                      final int by,
                                      final int bz,
                                      final boolean drawAllSides,
                                      final TextureHandle img,
                                      final boolean isEntity,
                                      final boolean isAsItem,
                                      final float r,
                                      final float g,
                                      final float b)
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
                     Matrix.IDENTITY,
                     blockToWorld,
                     bx,
                     by,
                     bz,
                     img,
                     this.type.isDoubleSided(),
                     isEntity,
                     isAsItem,
                     r,
                     g,
                     b);
        return rs;
    }

    private RenderingStream drawSolid(final RenderingStream rs,
                                      final Matrix blockToWorld,
                                      final int bx,
                                      final int by,
                                      final int bz,
                                      final boolean drawAllSides,
                                      final boolean isEntity,
                                      final boolean isAsItem,
                                      final float r,
                                      final float g,
                                      final float b)
    {
        return drawSolid(rs,
                         blockToWorld,
                         bx,
                         by,
                         bz,
                         drawAllSides,
                         this.type.textures[this.data.intdata],
                         isEntity,
                         isAsItem,
                         r,
                         g,
                         b);
    }

    private static Vector drawSim3D_p1 = Vector.allocate();
    private static Vector drawSim3D_p2 = Vector.allocate();
    private static Vector drawSim3D_p3 = Vector.allocate();
    private static Vector drawSim3D_p4 = Vector.allocate();
    private static Matrix drawSim3D_localToWorld = new Matrix();

    private RenderingStream drawSim3D(final RenderingStream rs,
                                      final Matrix localToBlock,
                                      final Matrix blockToWorld,
                                      final int bx,
                                      final int by,
                                      final int bz,
                                      final boolean isEntity,
                                      final boolean isAsItem,
                                      final TextureHandle img)
    {
        Matrix localToWorld = localToBlock.concat(drawSim3D_localToWorld,
                                                  blockToWorld);
        if(isAsItem)
        {
            Vector p1 = localToWorld.apply(drawSim3D_p1,
                                           Vector.set(drawSim3D_p1, 0, 0, 0.5f));
            Vector p2 = localToWorld.apply(drawSim3D_p2,
                                           Vector.set(drawSim3D_p2, 1, 0, 0.5f));
            Vector p3 = localToWorld.apply(drawSim3D_p3,
                                           Vector.set(drawSim3D_p3, 0, 1, 0.5f));
            Vector p4 = localToWorld.apply(drawSim3D_p4,
                                           Vector.set(drawSim3D_p4, 1, 1, 0.5f));
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
            return rs;
        }
        {
            Vector p1 = localToWorld.apply(drawSim3D_p1,
                                           Vector.set(drawSim3D_p1, 1, 0, 0));
            Vector p2 = localToWorld.apply(drawSim3D_p2,
                                           Vector.set(drawSim3D_p2, 0, 0, 1));
            Vector p3 = localToWorld.apply(drawSim3D_p3,
                                           Vector.set(drawSim3D_p3, 1, 1, 0));
            Vector p4 = localToWorld.apply(drawSim3D_p4,
                                           Vector.set(drawSim3D_p4, 0, 1, 1));
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
        {
            Vector p1 = localToWorld.apply(drawSim3D_p1,
                                           Vector.set(drawSim3D_p1, 0, 0, 0));
            Vector p2 = localToWorld.apply(drawSim3D_p2,
                                           Vector.set(drawSim3D_p2, 1, 0, 1f));
            Vector p3 = localToWorld.apply(drawSim3D_p3,
                                           Vector.set(drawSim3D_p3, 0, 1, 0f));
            Vector p4 = localToWorld.apply(drawSim3D_p4,
                                           Vector.set(drawSim3D_p4, 1, 1, 1f));
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
        return rs;
    }

    @Override
    public RenderingStream draw(final RenderingStream rs,
                                final Matrix blockToWorld)
    {
        return draw(rs, blockToWorld, false, false);
    }

    private static Matrix getButtonTransformInternal_t1 = new Matrix();
    private static Matrix getButtonTransformInternal_t2 = new Matrix();

    private static Matrix getButtonTransformInternal(final int orientation,
                                                     final boolean pushed)
    {
        final float ButtonSize = 0.2f;
        final float ButtonDepth = 0.1f;
        final float ButtonPushedDepth = 0.05f;
        float depth = ButtonDepth;
        if(pushed)
            depth = ButtonPushedDepth;
        Matrix tform = Matrix.setToTranslate(getButtonTransformInternal_t1,
                                             -0.5f,
                                             0.0f,
                                             -0.5f)
                             .concatAndSet(Matrix.setToScale(getButtonTransformInternal_t2,
                                                             ButtonSize,
                                                             depth,
                                                             ButtonSize));
        tform = tform.concatAndSet(Matrix.setToTranslate(getButtonTransformInternal_t2,
                                                         0.0f,
                                                         -0.5f,
                                                         0.0f));
        switch(orientation)
        {
        case 0: // -X
            tform = tform.concatAndSet(Matrix.setToRotateZ(getButtonTransformInternal_t2,
                                                           -Math.PI / 2));
            break;
        case 1: // -Z
            tform = tform.concatAndSet(Matrix.setToRotateX(getButtonTransformInternal_t2,
                                                           Math.PI / 2));
            break;
        case 2: // +X
            tform = tform.concatAndSet(Matrix.setToRotateZ(getButtonTransformInternal_t2,
                                                           Math.PI / 2));
            break;
        case 3: // +Z
            tform = tform.concatAndSet(Matrix.setToRotateX(getButtonTransformInternal_t2,
                                                           -Math.PI / 2));
            break;
        // case 4: // -Y
        default:
            break;
        }
        return tform.concatAndSet(Matrix.setToTranslate(getButtonTransformInternal_t2,
                                                        0.5f,
                                                        0.5f,
                                                        0.5f));
    }

    private static final Matrix[][] buttonTransforms;
    static
    {
        buttonTransforms = new Matrix[5][];
        for(int i = 0; i < 5; i++)
        {
            buttonTransforms[i] = new Matrix[2];
            buttonTransforms[i][0] = getButtonTransformInternal(i, false).getImmutable();
            buttonTransforms[i][1] = getButtonTransformInternal(i, true).getImmutable();
        }
    }

    private static Matrix getButtonTransform(final int orientation,
                                             final boolean pushed)
    {
        int bIndex = pushed ? 1 : 0;
        if(orientation < 0 || orientation >= 5)
            return buttonTransforms[4][bIndex];
        return buttonTransforms[orientation][bIndex];
    }

    private static Matrix getTorchTransfromInternal_t1 = new Matrix();
    private static Matrix getTorchTransfromInternal_t2 = new Matrix();

    private static Matrix getTorchTransformInternal(final int orientation)
    {
        final float xzscale = 1 / 8f, yscale = 6 / 8f;
        Matrix tform = Matrix.setToTranslate(getTorchTransfromInternal_t1,
                                             -0.5f,
                                             0.0f,
                                             -0.5f)
                             .concatAndSet(Matrix.setToScale(getTorchTransfromInternal_t2,
                                                             xzscale,
                                                             yscale,
                                                             xzscale));
        tform = tform.concatAndSet(Matrix.setToTranslate(getTorchTransfromInternal_t2,
                                                         0.5f,
                                                         0.0f,
                                                         0.5f));
        final float distfromedge = 0.5f - xzscale / 2.0f;
        final float distfromtop = 1.0f - yscale;
        switch(orientation)
        {
        case 0: // -X
            tform = tform.concatAndSet(Matrix.setToTranslate(getTorchTransfromInternal_t2,
                                                             -distfromedge,
                                                             distfromtop / 2,
                                                             0));
            break;
        case 1: // -Z
            tform = tform.concatAndSet(Matrix.setToTranslate(getTorchTransfromInternal_t2,
                                                             0,
                                                             distfromtop / 2,
                                                             -distfromedge));
            break;
        case 2: // +X
            tform = tform.concatAndSet(Matrix.setToTranslate(getTorchTransfromInternal_t2,
                                                             distfromedge,
                                                             distfromtop / 2,
                                                             0));
            break;
        case 3: // +Z
            tform = tform.concatAndSet(Matrix.setToTranslate(getTorchTransfromInternal_t2,
                                                             0,
                                                             distfromtop / 2,
                                                             distfromedge));
            break;
        // case 4: // -Y
        default:
            break;
        }
        return tform.getImmutable();
    }

    private static final Matrix[] torchTransforms;
    static
    {
        torchTransforms = new Matrix[5];
        for(int i = 0; i < 5; i++)
            torchTransforms[i] = getTorchTransformInternal(i);
    }

    private static Matrix getTorchTransform(final int orientation)
    {
        if(orientation < 0 || orientation >= 5)
            return torchTransforms[4];
        return torchTransforms[orientation];
    }

    private static Matrix getLeverTransformInternal_t1 = new Matrix();
    private static Matrix getLeverTransformInternal_t2 = new Matrix();

    private static Matrix getLeverTransformInternal(final int orientation)
    {
        Matrix tform = Matrix.setToTranslate(getLeverTransformInternal_t1,
                                             -0.5f,
                                             0,
                                             -0.5f);
        tform = tform.concatAndSet(Matrix.setToScale(getLeverTransformInternal_t2,
                                                     8.0f / 16,
                                                     3.0f / 16,
                                                     6.0f / 16));
        tform = tform.concatAndSet(Matrix.setToTranslate(getLeverTransformInternal_t2,
                                                         0.0f,
                                                         -0.5f,
                                                         0.0f));
        switch(orientation)
        {
        case 0: // -X
            tform = tform.concatAndSet(Matrix.setToRotateZ(getLeverTransformInternal_t2,
                                                           -Math.PI / 2));
            break;
        case 1: // -Z
            tform = tform.concatAndSet(Matrix.setToRotateX(getLeverTransformInternal_t2,
                                                           Math.PI / 2));
            break;
        case 2: // +X
            tform = tform.concatAndSet(Matrix.setToRotateZ(getLeverTransformInternal_t2,
                                                           Math.PI / 2));
            break;
        case 3: // +Z
            tform = tform.concatAndSet(Matrix.setToRotateX(getLeverTransformInternal_t2,
                                                           -Math.PI / 2));
            break;
        case 5: // +Y
            tform = tform.concatAndSet(Matrix.setToRotateX(getLeverTransformInternal_t2,
                                                           -Math.PI));
            break;
        // case 4: // -Y
        default:
            break;
        }
        return tform.concatAndSet(Matrix.setToTranslate(getLeverTransformInternal_t2,
                                                        0.5f,
                                                        0.5f,
                                                        0.5f))
                    .getImmutable();
    }

    private static final Matrix[] leverTransforms;
    static
    {
        leverTransforms = new Matrix[6];
        for(int i = 0; i < 6; i++)
            leverTransforms[i] = getLeverTransformInternal(i);
    }

    private static Matrix getLeverTransform(final int orientation)
    {
        if(orientation < 0 || orientation >= 6)
            return leverTransforms[4];
        return leverTransforms[orientation];
    }

    private static Matrix getLeverHandleTransformInternal_t1 = new Matrix();
    private static Matrix getLeverHandleTransformInternal_t2 = new Matrix();

    private static Matrix
        getLeverHandleTransformInternal(final int orientation,
                                        final boolean state)
    {
        Matrix tform = Matrix.setToTranslate(getLeverHandleTransformInternal_t1,
                                             -0.5f,
                                             0,
                                             -0.5f);
        tform = tform.concatAndSet(Matrix.setToScale(getLeverHandleTransformInternal_t2,
                                                     2.0f / 16,
                                                     8.0f / 16,
                                                     2.0f / 16));
        tform = tform.concatAndSet(Matrix.setToRotateZ(getLeverHandleTransformInternal_t2,
                                                       state ? Math.PI / 6
                                                               : -Math.PI / 6));
        tform = tform.concatAndSet(Matrix.setToTranslate(getLeverHandleTransformInternal_t2,
                                                         0.0f,
                                                         -0.5f,
                                                         0.0f));
        switch(orientation)
        {
        case 0: // -X
            tform = tform.concatAndSet(Matrix.setToRotateZ(getLeverHandleTransformInternal_t2,
                                                           -Math.PI / 2));
            break;
        case 1: // -Z
            tform = tform.concatAndSet(Matrix.setToRotateX(getLeverHandleTransformInternal_t2,
                                                           Math.PI / 2));
            break;
        case 2: // +X
            tform = tform.concatAndSet(Matrix.setToRotateZ(getLeverHandleTransformInternal_t2,
                                                           Math.PI / 2));
            break;
        case 3: // +Z
            tform = tform.concatAndSet(Matrix.setToRotateX(getLeverHandleTransformInternal_t2,
                                                           -Math.PI / 2));
            break;
        case 5: // +Y
            tform = tform.concatAndSet(Matrix.setToRotateX(getLeverHandleTransformInternal_t2,
                                                           -Math.PI));
            break;
        // case 4: // -Y
        default:
            break;
        }
        return tform.concatAndSet(Matrix.setToTranslate(getLeverHandleTransformInternal_t2,
                                                        0.5f,
                                                        0.5f,
                                                        0.5f))
                    .getImmutable();
    }

    private static final Matrix[][] leverHandleTransforms;
    static
    {
        leverHandleTransforms = new Matrix[6][];
        for(int i = 0; i < 6; i++)
        {
            leverHandleTransforms[i] = new Matrix[2];
            leverHandleTransforms[i][0] = getLeverHandleTransformInternal(i,
                                                                          false);
            leverHandleTransforms[i][1] = getLeverHandleTransformInternal(i,
                                                                          true);
        }
    }

    private static Matrix getLeverHandleTransform(final int orientation,
                                                  final boolean state)
    {
        if(orientation < 0 || orientation >= 6)
            return leverHandleTransforms[4][state ? 1 : 0];
        return leverHandleTransforms[orientation][state ? 1 : 0];
    }

    /** @return the use count for this tool */
    public int toolGetUseCount()
    {
        return this.data.intdata;
    }

    /** @return the maximum use count for this tool */
    public int toolGetMaxUseCount()
    {
        return this.type.getDurability();
    }

    private static final TextureHandle toolUsageBackground = TextureAtlas.addImage(new Image(Color.V(0.4f)));
    private static final TextureHandle[] toolUsageForeground = new TextureHandle[]
    {
        TextureAtlas.addImage(new Image(Color.RGB(1.0f, 0.5f, 0.0f))),
        TextureAtlas.addImage(new Image(Color.RGB(1.0f, 1.0f, 0.0f))),
        TextureAtlas.addImage(new Image(Color.RGB(0.0f, 1.0f, 0.0f))),
    };
    private static Vector drawToolUsage_t1 = Vector.allocate();
    private static Vector drawToolUsage_t2 = Vector.allocate();
    private static Vector drawToolUsage_t3 = Vector.allocate();
    private static Vector drawToolUsage_t4 = Vector.allocate();

    private RenderingStream drawToolUsage(final RenderingStream rs,
                                          final Matrix blockToWorld)
    {
        if(toolGetUseCount() <= 0)
            return rs;
        float relativeUseLeft = 1.0f - (float)toolGetUseCount()
                / toolGetMaxUseCount();
        float dividerPos = 1 / 16f + 14 / 16f * relativeUseLeft;
        rs.pushMatrixStack();
        rs.concatMatrix(blockToWorld);
        drawFace(rs,
                 toolUsageBackground,
                 Vector.set(drawToolUsage_t1, 15 / 16f, 3 / 16f, 0.05f),
                 Vector.set(drawToolUsage_t2, 15 / 16f, 1 / 16f, 0.05f),
                 Vector.set(drawToolUsage_t3, dividerPos, 1 / 16f, 0.05f),
                 Vector.set(drawToolUsage_t4, dividerPos, 3 / 16f, 0.05f),
                 0,
                 0,
                 0,
                 1,
                 1,
                 1,
                 1,
                 0,
                 0,
                 0,
                 0,
                 true,
                 false,
                 true,
                 false);
        drawFace(rs,
                 toolUsageForeground[Math.max(0,
                                              Math.min(toolUsageForeground.length - 1,
                                                       (int)Math.floor(relativeUseLeft
                                                               * toolUsageForeground.length)))],
                 Vector.set(drawToolUsage_t1, dividerPos, 1 / 16f, 0.05f),
                 Vector.set(drawToolUsage_t2, dividerPos, 3 / 16f, 0.05f),
                 Vector.set(drawToolUsage_t3, 1 / 16f, 3 / 16f, 0.05f),
                 Vector.set(drawToolUsage_t4, 1 / 16f, 1 / 16f, 0.05f),
                 0,
                 0,
                 0,
                 1,
                 1,
                 1,
                 1,
                 0,
                 0,
                 0,
                 0,
                 true,
                 false,
                 true,
                 false);
        rs.popMatrixStack();
        return rs;
    }

    private static Vector draw_pos = Vector.allocate();
    private static Matrix draw_t1 = new Matrix();
    private static Matrix draw_t2 = new Matrix();
    private static Matrix draw_rotateMat = new Matrix();

    private RenderingStream draw(final RenderingStream rs,
                                 final Matrix blockToWorld,
                                 final boolean isEntity,
                                 final boolean isAsItem)
    {
        int bx, by, bz;
        Vector pos = blockToWorld.apply(draw_pos, Vector.ZERO);
        bx = (int)Math.floor(pos.getX() + 0.5);
        by = (int)Math.floor(pos.getY() + 0.5);
        bz = (int)Math.floor(pos.getZ() + 0.5);
        switch(this.type.drawType)
        {
        case BDTCustom:
            switch(this.type)
            {
            case BTFurnace:
                if(this.data.intdata > 0 && this.data.blockdata != null
                        && this.data.srccount > 0
                        && this.data.destcount < BLOCK_STACK_SIZE)
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
                Matrix tform = Matrix.setToTranslate(draw_t1,
                                                     -0.5f,
                                                     -0.5f,
                                                     -0.5f)
                                     .concatAndSet(Matrix.setToScale(draw_t2,
                                                                     0.98f))
                                     .concatAndSet(Matrix.setToTranslate(draw_t2,
                                                                         0.5f,
                                                                         0.5f,
                                                                         0.5f));
                drawItem(rs,
                         Matrix.setToRotateX(draw_t2, Math.PI / 2)
                               .concatAndSet(tform),
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
            case BTDeleteBlock:
                internalDraw(rs,
                             0x3F,
                             Matrix.setToTranslate(draw_t1, -0.5f, -0.5f, -0.5f)
                                   .concatAndSet(Matrix.setToScale(draw_t2,
                                                                   1.05f))
                                   .concatAndSet(Matrix.setToTranslate(draw_t2,
                                                                       0.5f,
                                                                       0.5f,
                                                                       0.5f)),
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
                switch(this.data.orientation)
                {
                case 0:
                    internalDraw(rs,
                                 0x3F,
                                 Matrix.IDENTITY,
                                 blockToWorld,
                                 bx,
                                 by,
                                 bz,
                                 this.type.textures[this.data.intdata],
                                 false,
                                 isEntity,
                                 isAsItem);
                    break;
                case 1:
                    internalDraw(rs,
                                 0x3F,
                                 Matrix.setToTranslate(draw_t1,
                                                       -0.5f,
                                                       -0.5f,
                                                       -0.5f)
                                       .concatAndSet(Matrix.setToRotateZ(draw_t2,
                                                                         Math.PI / 2))
                                       .concatAndSet(Matrix.setToTranslate(draw_t2,
                                                                           0.5f,
                                                                           0.5f,
                                                                           0.5f)),
                                 blockToWorld,
                                 bx,
                                 by,
                                 bz,
                                 this.type.textures[this.data.intdata],
                                 false,
                                 isEntity,
                                 isAsItem);
                    break;
                case 2:
                    internalDraw(rs,
                                 0x3F,
                                 Matrix.setToTranslate(draw_t1,
                                                       -0.5f,
                                                       -0.5f,
                                                       -0.5f)
                                       .concatAndSet(Matrix.setToRotateX(draw_t2,
                                                                         Math.PI / 2))
                                       .concatAndSet(Matrix.setToTranslate(draw_t2,
                                                                           0.5f,
                                                                           0.5f,
                                                                           0.5f)),
                                 blockToWorld,
                                 bx,
                                 by,
                                 bz,
                                 this.type.textures[this.data.intdata],
                                 false,
                                 isEntity,
                                 isAsItem);
                    break;
                case 3:
                    internalDraw(rs,
                                 DMaskNX | DMaskNZ | DMaskPX | DMaskPZ,
                                 Matrix.IDENTITY,
                                 blockToWorld,
                                 bx,
                                 by,
                                 bz,
                                 this.type.textures[this.data.intdata],
                                 false,
                                 isEntity,
                                 isAsItem);
                    internalDraw(rs,
                                 DMaskNZ | DMaskPZ,
                                 Matrix.setToTranslate(draw_t1,
                                                       -0.5f,
                                                       -0.5f,
                                                       -0.5f)
                                       .concatAndSet(Matrix.setToRotateX(draw_t2,
                                                                         Math.PI / 2))
                                       .concatAndSet(Matrix.setToTranslate(draw_t2,
                                                                           0.5f,
                                                                           0.5f,
                                                                           0.5f)),
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
                break;
            case BTLeaves:
            {
                float r = Tree.defaultBiomeColorR;
                float g = Tree.defaultBiomeColorG;
                float b = Tree.defaultBiomeColorB;
                if(!isAsItem && !isEntity)
                {
                    r = world.getBiomeFoliageColorR(bx, bz);
                    g = world.getBiomeFoliageColorG(bx, bz);
                    b = world.getBiomeFoliageColorB(bx, bz);
                }
                drawSolid(rs,
                          blockToWorld,
                          bx,
                          by,
                          bz,
                          Main.FancyGraphics,
                          this.type.textures[Main.FancyGraphics ? this.type.textures.length
                                  / 2 + this.data.intdata
                                  : this.data.intdata],
                          isEntity,
                          isAsItem,
                          r,
                          g,
                          b);
                break;
            }
            case BTLadder:
                drawItem(rs,
                         Matrix.setToTranslate(draw_t1, -0.5f, -0.5f, -0.49f)
                               .concatAndSet(Matrix.setToRotateY(draw_t2,
                                                                 Math.PI
                                                                         / 2.0
                                                                         * (1 - this.data.orientation)))
                               .concatAndSet(Matrix.setToTranslate(draw_t2,
                                                                   0.5f,
                                                                   0.5f,
                                                                   0.5f)),
                         blockToWorld,
                         bx,
                         by,
                         bz,
                         this.type.textures[this.data.intdata],
                         isEntity,
                         isAsItem);
                break;
            case BTVines:
            {
                float r = Tree.defaultBiomeColorR;
                float g = Tree.defaultBiomeColorG;
                float b = Tree.defaultBiomeColorB;
                if(!isAsItem && !isEntity)
                {
                    r = world.getBiomeFoliageColorR(bx, bz);
                    g = world.getBiomeFoliageColorG(bx, bz);
                    b = world.getBiomeFoliageColorB(bx, bz);
                }
                drawItem(rs,
                         Matrix.setToTranslate(draw_t1, -0.5f, -0.5f, -0.49f)
                               .concatAndSet(Matrix.setToRotateY(draw_t2,
                                                                 Math.PI
                                                                         / 2.0
                                                                         * (1 - this.data.orientation)))
                               .concatAndSet(Matrix.setToTranslate(draw_t2,
                                                                   0.5f,
                                                                   0.5f,
                                                                   0.5f)),
                         blockToWorld,
                         bx,
                         by,
                         bz,
                         this.type.textures[this.data.intdata],
                         isEntity,
                         isAsItem,
                         r,
                         g,
                         b);
                break;
            }
            case BTRedstoneRepeaterOff:
            case BTRedstoneRepeaterOn:
            {
                Matrix rotateMat = Matrix.setToTranslate(draw_rotateMat,
                                                         -0.5f,
                                                         -0.5f,
                                                         -0.49f)
                                         .concatAndSet(Matrix.setToRotateY(draw_t2,
                                                                           Math.PI
                                                                                   / 2.0
                                                                                   * (1 - this.data.orientation)))
                                         .concatAndSet(Matrix.setToTranslate(draw_t2,
                                                                             0.5f,
                                                                             0.5f,
                                                                             0.5f));
                internalDraw(rs,
                             0x3F,
                             Matrix.setToScale(draw_t2, 1.0f, 1.0f / 8, 1.0f)
                                   .concatAndSet(rotateMat),
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
                tform = tform.concat(draw_t1, Matrix.setToScale(draw_t2,
                                                                1.0f,
                                                                0.6f,
                                                                1.0f));
                tform = tform.concatAndSet(Matrix.setToTranslate(draw_t2,
                                                                 -0.5f,
                                                                 0,
                                                                 -0.5f));
                tform = tform.concatAndSet(Matrix.setToTranslate(draw_t2,
                                                                 0.5f,
                                                                 0,
                                                                 3.0f / 16));
                tform = tform.concatAndSet(rotateMat);
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
                    tform = Matrix.setToTranslate(draw_t1, -0.5f, -0.5f, -0.5f);
                    tform = tform.concatAndSet(Matrix.setToScale(draw_t2,
                                                                 1.0f,
                                                                 1.0f / 8,
                                                                 1.0f / 8));
                    tform = tform.concatAndSet(Matrix.setToTranslate(draw_t2,
                                                                     0.5f,
                                                                     3.0f / 16,
                                                                     7.0f / 16));
                    tform = tform.concatAndSet(rotateMat);
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
                    tform = tform.concat(draw_t1, Matrix.setToScale(draw_t2,
                                                                    1.0f,
                                                                    0.6f,
                                                                    1.0f));
                    tform = tform.concatAndSet(Matrix.setToTranslate(draw_t2,
                                                                     0,
                                                                     0,
                                                                     (5.0f + 2.0f * this.data.intdata) / 16 - 0.5f));
                    tform = tform.concatAndSet(rotateMat);
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
                Matrix rotateMat, tform = Matrix.setToTranslate(draw_t1,
                                                                -0.5f,
                                                                -0.5f,
                                                                -0.5f);
                switch(getNegOrientation(this.data.orientation))
                {
                case 0: // -X
                    tform = tform.concatAndSet(Matrix.setToRotateZ(draw_t2,
                                                                   -Math.PI / 2));
                    break;
                case 1: // -Z
                    tform = tform.concatAndSet(Matrix.setToRotateX(draw_t2,
                                                                   Math.PI / 2));
                    break;
                case 2: // +X
                    tform = tform.concatAndSet(Matrix.setToRotateZ(draw_t2,
                                                                   Math.PI / 2));
                    break;
                case 3: // +Z
                    tform = tform.concatAndSet(Matrix.setToRotateX(draw_t2,
                                                                   -Math.PI / 2));
                    break;
                case 5: // +Y
                    tform = tform.concatAndSet(Matrix.setToRotateX(draw_t2,
                                                                   -Math.PI));
                    break;
                // case 4: // -Y
                default:
                    break;
                }
                rotateMat = tform.concat(draw_rotateMat,
                                         Matrix.setToTranslate(draw_t2,
                                                               0.5f,
                                                               0.5f,
                                                               0.5f));
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
                    tform = Matrix.setToScale(draw_t1, 1.0f, 0.75f, 1.0f)
                                  .concatAndSet(rotateMat);
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
                Matrix rotateMat, tform = Matrix.setToTranslate(draw_t1,
                                                                -0.5f,
                                                                -0.5f,
                                                                -0.5f);
                switch(getNegOrientation(this.data.orientation))
                {
                case 0: // -X
                    tform = tform.concatAndSet(Matrix.setToRotateZ(draw_t2,
                                                                   -Math.PI / 2));
                    break;
                case 1: // -Z
                    tform = tform.concatAndSet(Matrix.setToRotateX(draw_t2,
                                                                   Math.PI / 2));
                    break;
                case 2: // +X
                    tform = tform.concatAndSet(Matrix.setToRotateZ(draw_t2,
                                                                   Math.PI / 2));
                    break;
                case 3: // +Z
                    tform = tform.concatAndSet(Matrix.setToRotateX(draw_t2,
                                                                   -Math.PI / 2));
                    break;
                case 5: // +Y
                    tform = tform.concatAndSet(Matrix.setToRotateX(draw_t2,
                                                                   -Math.PI));
                    break;
                // case 4: // -Y
                default:
                    break;
                }
                rotateMat = tform.concat(draw_rotateMat,
                                         Matrix.setToTranslate(draw_t2,
                                                               0.5f,
                                                               0.5f,
                                                               0.5f));
                tform = Matrix.setToScale(draw_t1, 1.0f, 0.25f, 1.0f)
                              .concatAndSet(Matrix.setToTranslate(draw_t2,
                                                                  0,
                                                                  0.75f,
                                                                  0))
                              .concatAndSet(rotateMat);
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
                tform = Matrix.setToScale(draw_t1, 2.0f / 16, 1.0f, 2.0f / 16)
                              .concatAndSet(Matrix.setToTranslate(draw_t2,
                                                                  -1.0f / 16 + 0.5f,
                                                                  -0.25f,
                                                                  -1.0f / 16 + 0.5f))
                              .concatAndSet(rotateMat);
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
                             Matrix.setToScale(draw_t1, 1.0f, 1.0f / 16, 1.0f),
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
            case BTSnow:
            {
                internalDraw(rs,
                             0x3F,
                             Matrix.setToScale(draw_t1,
                                               1.0f,
                                               this.data.intdata / 8.0f,
                                               1.0f),
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
            case BTGrass:
            {
                boolean isSnowGrass = false;
                if(!isEntity && !isAsItem)
                {
                    Block py = world.getBlockEval(bx, by + 1, bz);
                    if(py != null && py.getType() == BlockType.BTSnow)
                        isSnowGrass = true;
                }
                if(isEntity || isAsItem || isSnowGrass)
                {
                    drawSolid(rs,
                              blockToWorld,
                              bx,
                              by,
                              bz,
                              false,
                              this.type.textures[isSnowGrass ? 1 : 0],
                              isEntity,
                              isAsItem);
                }
                else
                {
                    drawSolid(rs,
                              blockToWorld,
                              bx,
                              by,
                              bz,
                              false,
                              this.type.textures[3],
                              isEntity,
                              isAsItem);
                    drawSolid(rs,
                              blockToWorld,
                              bx,
                              by,
                              bz,
                              false,
                              this.type.textures[2],
                              isEntity,
                              isAsItem,
                              world.getBiomeGrassColorR(bx, bz),
                              world.getBiomeGrassColorG(bx, bz),
                              world.getBiomeGrassColorB(bx, bz));
                }
                break;
            }
            case BTRedstoneComparator:
            {
                Matrix rotateMat = Matrix.setToTranslate(draw_rotateMat,
                                                         -0.5f,
                                                         -0.5f,
                                                         -0.5f)
                                         .concatAndSet(Matrix.setToRotateY(draw_t2,
                                                                           Math.PI
                                                                                   / 2.0
                                                                                   * (1 - this.data.orientation)))
                                         .concatAndSet(Matrix.setToTranslate(draw_t2,
                                                                             0.5f,
                                                                             0.5f,
                                                                             0.5f));
                internalDraw(rs,
                             0x3F,
                             Matrix.setToScale(draw_t1, 1.0f, 1.0f / 8, 1.0f)
                                   .concatAndSet(rotateMat),
                             blockToWorld,
                             bx,
                             by,
                             bz,
                             this.type.textures[this.data.intdata > 0 ? 1 : 0],
                             false,
                             isEntity,
                             isAsItem);
                TextureHandle torchTexture;
                if(this.data.step != 0)
                    torchTexture = BlockType.BTRedstoneTorchOn.textures[0];
                else
                    torchTexture = BlockType.BTRedstoneTorchOff.textures[0];
                Matrix tform;
                tform = getTorchTransform(4);
                tform = tform.concat(draw_t1, Matrix.setToScale(draw_t2,
                                                                1.0f,
                                                                0.6f,
                                                                1.0f));
                tform = tform.concatAndSet(Matrix.setToTranslate(draw_t2,
                                                                 -0.5f,
                                                                 0,
                                                                 -0.5f));
                tform = tform.concatAndSet(Matrix.setToTranslate(draw_t2,
                                                                 0.5f,
                                                                 0,
                                                                 3.0f / 16));
                tform = tform.concatAndSet(rotateMat);
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
                if(this.data.intdata > 0)
                    torchTexture = BlockType.BTRedstoneTorchOn.textures[0];
                else
                    torchTexture = BlockType.BTRedstoneTorchOff.textures[0];
                tform = getTorchTransform(4);
                tform = tform.concat(draw_t1, Matrix.setToScale(draw_t2,
                                                                1.0f,
                                                                0.6f,
                                                                1.0f));
                tform = tform.concatAndSet(Matrix.setToTranslate(draw_t2,
                                                                 3.0f / 16,
                                                                 0,
                                                                 12f / 16 - 0.5f));
                tform = tform.concatAndSet(rotateMat);
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
                tform = getTorchTransform(4);
                tform = tform.concat(draw_t1, Matrix.setToScale(draw_t2,
                                                                1.0f,
                                                                0.6f,
                                                                1.0f));
                tform = tform.concatAndSet(Matrix.setToTranslate(draw_t2,
                                                                 -3.0f / 16,
                                                                 0,
                                                                 12f / 16 - 0.5f));
                tform = tform.concatAndSet(rotateMat);
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
                break;
            }
            case BTDispenser:
            case BTDropper:
            {
                int drawMask;
                if(isAsItem || isEntity)
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
                int drawMaskFrame = drawMask;
                int drawMaskOpening = drawMask;
                int mask;
                switch(this.data.orientation)
                {
                case 0:
                    mask = DMaskNX;
                    break;
                case 1:
                    mask = DMaskNZ;
                    break;
                case 2:
                    mask = DMaskPX;
                    break;
                case 3:
                    mask = DMaskPZ;
                    break;
                case 4:
                    mask = DMaskNY;
                    break;
                case 5:
                default:
                    mask = DMaskPY;
                    break;
                }
                drawMaskFrame = drawMaskFrame & ~mask;
                drawMaskOpening = drawMaskOpening & mask;
                internalDraw(rs,
                             drawMaskFrame,
                             Matrix.IDENTITY,
                             blockToWorld,
                             bx,
                             by,
                             bz,
                             this.type.textures[1],
                             false,
                             isEntity,
                             isAsItem);
                internalDraw(rs,
                             drawMaskOpening,
                             Matrix.IDENTITY,
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
            case BTHopper:
            {
                if(isAsItem || isEntity)
                {
                    drawItem(rs,
                             Matrix.IDENTITY,
                             blockToWorld,
                             bx,
                             by,
                             bz,
                             this.type.textures[3],
                             isEntity,
                             isAsItem);
                }
                else
                {
                    internalDraw(rs,
                                 0x3F,
                                 Matrix.setToScale(draw_t1, 1, 0.5f, 1)
                                       .concatAndSet(Matrix.setToTranslate(draw_t2,
                                                                           0,
                                                                           0.5f,
                                                                           0)),
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
                                 Matrix.setToScale(draw_t1, 0.5f, 0.5f, 0.5f)
                                       .concatAndSet(Matrix.setToTranslate(draw_t2,
                                                                           0.25f,
                                                                           0.125f,
                                                                           0.25f)),
                                 blockToWorld,
                                 bx,
                                 by,
                                 bz,
                                 this.type.textures[1],
                                 false,
                                 isEntity,
                                 isAsItem);
                    switch(this.data.orientation)
                    {
                    case 0:
                        internalDraw(rs,
                                     0x3F,
                                     Matrix.setToScale(draw_t1,
                                                       1 / 4f,
                                                       1 / 2f,
                                                       1 / 4f)
                                           .concatAndSet(Matrix.setToTranslate(draw_t2,
                                                                               -1 / 8f,
                                                                               -1 / 2f,
                                                                               -1 / 8f))
                                           .concatAndSet(Matrix.setToRotateZ(draw_t2,
                                                                             -Math.PI / 2))
                                           .concatAndSet(Matrix.setToTranslate(draw_t2,
                                                                               1 / 2f,
                                                                               5 / 16f,
                                                                               1 / 2f)),
                                     blockToWorld,
                                     bx,
                                     by,
                                     bz,
                                     this.type.textures[2],
                                     false,
                                     isEntity,
                                     isAsItem);
                        break;
                    case 1:
                        internalDraw(rs,
                                     0x3F,
                                     Matrix.setToScale(draw_t1,
                                                       1 / 4f,
                                                       1 / 2f,
                                                       1 / 4f)
                                           .concatAndSet(Matrix.setToTranslate(draw_t2,
                                                                               -1 / 8f,
                                                                               -1 / 2f,
                                                                               -1 / 8f))
                                           .concatAndSet(Matrix.setToRotateX(draw_t2,
                                                                             Math.PI / 2))
                                           .concatAndSet(Matrix.setToTranslate(draw_t2,
                                                                               1 / 2f,
                                                                               5 / 16f,
                                                                               1 / 2f)),
                                     blockToWorld,
                                     bx,
                                     by,
                                     bz,
                                     this.type.textures[2],
                                     false,
                                     isEntity,
                                     isAsItem);
                        break;
                    case 2:
                        internalDraw(rs,
                                     0x3F,
                                     Matrix.setToScale(draw_t1,
                                                       1 / 4f,
                                                       1 / 2f,
                                                       1 / 4f)
                                           .concatAndSet(Matrix.setToTranslate(draw_t2,
                                                                               -1 / 8f,
                                                                               -1 / 2f,
                                                                               -1 / 8f))
                                           .concatAndSet(Matrix.setToRotateZ(draw_t2,
                                                                             Math.PI / 2))
                                           .concatAndSet(Matrix.setToTranslate(draw_t2,
                                                                               1 / 2f,
                                                                               5 / 16f,
                                                                               1 / 2f)),
                                     blockToWorld,
                                     bx,
                                     by,
                                     bz,
                                     this.type.textures[2],
                                     false,
                                     isEntity,
                                     isAsItem);
                        break;
                    case 3:
                        internalDraw(rs,
                                     0x3F,
                                     Matrix.setToScale(draw_t1,
                                                       1 / 4f,
                                                       1 / 2f,
                                                       1 / 4f)
                                           .concatAndSet(Matrix.setToTranslate(draw_t2,
                                                                               -1 / 8f,
                                                                               -1 / 2f,
                                                                               -1 / 8f))
                                           .concatAndSet(Matrix.setToRotateX(draw_t2,
                                                                             -Math.PI / 2))
                                           .concatAndSet(Matrix.setToTranslate(draw_t2,
                                                                               1 / 2f,
                                                                               5 / 16f,
                                                                               1 / 2f)),
                                     blockToWorld,
                                     bx,
                                     by,
                                     bz,
                                     this.type.textures[2],
                                     false,
                                     isEntity,
                                     isAsItem);
                        break;
                    case 4:
                    default:
                        internalDraw(rs,
                                     0x3F,
                                     Matrix.setToScale(draw_t1,
                                                       1 / 4f,
                                                       1 / 2f,
                                                       1 / 4f)
                                           .concatAndSet(Matrix.setToTranslate(draw_t2,
                                                                               3 / 8f,
                                                                               0,
                                                                               3 / 8f)),
                                     blockToWorld,
                                     bx,
                                     by,
                                     bz,
                                     this.type.textures[2],
                                     false,
                                     isEntity,
                                     isAsItem);
                        break;
                    }
                }
                break;
            }
            case BTFarmland:
            {
                internalDraw(rs,
                             DMaskNX | DMaskPX | DMaskNY | DMaskNZ | DMaskPZ,
                             Matrix.IDENTITY,
                             blockToWorld,
                             bx,
                             by,
                             bz,
                             this.type.textures[this.data.intdata],
                             false,
                             isEntity,
                             isAsItem);
                internalDraw(rs,
                             DMaskPY,
                             Matrix.setToTranslate(draw_t1, 0, -1 / 16f, 0),
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
            case BTSeeds:
            {
                if(isAsItem)
                    drawItem(rs,
                             Matrix.IDENTITY,
                             blockToWorld,
                             bx,
                             by,
                             bz,
                             this.type.textures[8],
                             isEntity,
                             isAsItem);
                else if(isEntity)
                    drawImgAsEntity(rs, blockToWorld, this.type.textures[8]);
                else
                    drawSim3D(rs,
                              Matrix.setToTranslate(draw_t1, 0, -1 / 16f, 0),
                              blockToWorld,
                              bx,
                              by,
                              bz,
                              isEntity,
                              isAsItem,
                              this.type.textures[this.data.intdata]);
                break;
            }
            case BTCocoa:
            {
                if(isEntity)
                    drawImgAsEntity(rs, blockToWorld, this.type.textures[3]);
                else if(isAsItem)
                    drawItem(rs,
                             Matrix.IDENTITY,
                             blockToWorld,
                             bx,
                             by,
                             bz,
                             this.type.textures[3],
                             isEntity,
                             isAsItem);
                else
                {
                    Matrix rotateMat = Matrix.setToTranslate(draw_rotateMat,
                                                             -0.5f,
                                                             -0.5f,
                                                             -0.5f)
                                             .concatAndSet(Matrix.setToRotateY(draw_t2,
                                                                               Math.PI
                                                                                       / 2.0
                                                                                       * (1 - this.data.orientation)))
                                             .concatAndSet(Matrix.setToTranslate(draw_t2,
                                                                                 0.5f,
                                                                                 0.5f,
                                                                                 0.5f));
                    TextureAtlas.TextureHandle texture = this.type.textures[this.data.intdata];
                    drawItem(rs,
                             Matrix.setToTranslate(draw_t1, -0.5f, -0.5f, 0)
                                   .concatAndSet(Matrix.setToRotateY(draw_t2,
                                                                     Math.PI / 2))
                                   .concatAndSet(Matrix.setToTranslate(draw_t2,
                                                                       0.5f,
                                                                       0.5f,
                                                                       0.5f))
                                   .concatAndSet(rotateMat),
                             blockToWorld,
                             bx,
                             by,
                             bz,
                             texture,
                             isEntity,
                             isAsItem,
                             3 / 4f,
                             1 / 2f,
                             1f,
                             1f);
                    final float xzSize = (4 + 2 * this.data.intdata) / 16f, ySize = (5 + 2 * this.data.intdata) / 16f;
                    internalDraw(rs,
                                 DMaskNX,
                                 Matrix.setToTranslate(draw_t1,
                                                       0.5f - xzSize / 2f,
                                                       0,
                                                       0)
                                       .concatAndSet(rotateMat),
                                 blockToWorld,
                                 bx,
                                 by,
                                 bz,
                                 texture,
                                 false,
                                 isEntity,
                                 isAsItem);
                    internalDraw(rs,
                                 DMaskPX,
                                 Matrix.setToTranslate(draw_t1,
                                                       -0.5f + xzSize / 2f,
                                                       0,
                                                       0)
                                       .concatAndSet(rotateMat),
                                 blockToWorld,
                                 bx,
                                 by,
                                 bz,
                                 texture,
                                 false,
                                 isEntity,
                                 isAsItem);
                    internalDraw(rs,
                                 DMaskNZ,
                                 Matrix.setToTranslate(draw_t1, 0, 0, 1 / 16f)
                                       .concatAndSet(rotateMat),
                                 blockToWorld,
                                 bx,
                                 by,
                                 bz,
                                 texture,
                                 false,
                                 isEntity,
                                 isAsItem);
                    internalDraw(rs,
                                 DMaskPZ,
                                 Matrix.setToTranslate(draw_t1,
                                                       0,
                                                       0,
                                                       -15 / 16f + xzSize)
                                       .concatAndSet(rotateMat),
                                 blockToWorld,
                                 bx,
                                 by,
                                 bz,
                                 texture,
                                 false,
                                 isEntity,
                                 isAsItem);
                    internalDraw(rs,
                                 DMaskPY,
                                 Matrix.setToTranslate(draw_t1, 0, -4 / 16f, 0)
                                       .concatAndSet(rotateMat),
                                 blockToWorld,
                                 bx,
                                 by,
                                 bz,
                                 texture,
                                 false,
                                 isEntity,
                                 isAsItem);
                    internalDraw(rs,
                                 DMaskNY,
                                 Matrix.setToTranslate(draw_t1,
                                                       0,
                                                       12 / 16f - ySize,
                                                       0)
                                       .concatAndSet(rotateMat),
                                 blockToWorld,
                                 bx,
                                 by,
                                 bz,
                                 texture,
                                 false,
                                 isEntity,
                                 isAsItem);
                }
                break;
            }
            case BTWool:
                drawSolid(rs,
                          blockToWorld,
                          bx,
                          by,
                          bz,
                          false,
                          isEntity,
                          isAsItem,
                          this.data.dyeColor.r,
                          this.data.dyeColor.g,
                          this.data.dyeColor.b);
                break;
            default:
                break;
            }
            break;
        case BDTItem:
            drawItem(rs,
                     Matrix.IDENTITY,
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
        case BDTTool:
            drawItem(rs,
                     Matrix.IDENTITY,
                     blockToWorld,
                     bx,
                     by,
                     bz,
                     this.type.textures[0],
                     isEntity,
                     isAsItem);
            drawToolUsage(rs, blockToWorld);
            break;
        case BDTSim3D:
            drawSim3D(rs,
                      Matrix.IDENTITY,
                      blockToWorld,
                      bx,
                      by,
                      bz,
                      isEntity,
                      isAsItem,
                      this.type.textures[this.data.intdata]);
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
    public boolean setLighting(final int sunlight,
                               final int scatteredSunlight,
                               final int light)
    {
        int newSunlight = Math.max(0, Math.min(15, sunlight));
        int newScatteredSunlight = Math.max(newSunlight,
                                            Math.min(15, scatteredSunlight));
        int newLight = Math.max(0, Math.min(15, light));
        if(this.type == BlockType.BTLeaves)
        {
            newSunlight = 0;
        }
        else if(this.type == BlockType.BTPiston
                || this.type == BlockType.BTStickyPiston)
        {
            newSunlight = 0;
            newScatteredSunlight = 0;
            newLight = 0;
        }
        else if(isOpaque())
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
                if(this.data.intdata > 0 && this.data.blockdata != null
                        && this.data.srccount > 0
                        && this.data.destcount < BLOCK_STACK_SIZE)
                    return 13;
                return 0;
            case BTRedstoneComparator:
                if(this.data.intdata > 0)
                    return 9;
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
    public void copyLighting(final Block rt)
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
    public int getLighting(final int sunlightFactor)
    {
        return Math.max(this.scatteredSunlight + sunlightFactor - 15,
                        this.light);
    }

    /** gets the lighting array <BR/>
     * not thread safe
     * 
     * @param sunlightFactor
     *            used to check for the right array to return
     * @return the lighting array for <code>sunlightFactor</code> or null if
     *         there isn't one */
    public int[] getLightingArray(final int sunlightFactor)
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
    public void
        setLightingArray(final int lighting[], final int sunlightFactor)
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
            if(this.data.blockdata == null)
                return false;
            if(this.data.srccount <= 0)
                return false;
            if(this.data.intdata <= 0)
                return false;
            if(this.data.destcount >= BLOCK_STACK_SIZE)
                return false;
            return true;
        default:
            return this.type.isParticleGenerate();
        }
    }

    private static Vector generateParticles_t1 = Vector.allocate();
    private static Vector generateParticles_t2 = Vector.allocate();
    private static Vector generateParticles_blockOrigin = Vector.allocate();

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
    public void generateParticles(final int bx,
                                  final int by,
                                  final int bz,
                                  final double lastTime,
                                  final double curTime)
    {
        Vector blockOrigin = Vector.set(generateParticles_blockOrigin,
                                        bx,
                                        by,
                                        bz);
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
            if(this.data.blockdata == null)
                return;
            if(this.data.srccount <= 0)
                return;
            if(this.data.intdata <= 0)
                return;
            if(this.data.destcount >= BLOCK_STACK_SIZE)
                return;
            final float ParticlesPerSecond = 30;
            int count = (int)(Math.floor(curTime * ParticlesPerSecond) - Math.floor(lastTime
                    * ParticlesPerSecond));
            for(int i = 0; i < count; i++)
            {
                Entity p = Entity.NewParticle(Vector.add(generateParticles_t1,
                                                         blockOrigin,
                                                         World.fRand(0, 1),
                                                         1.0f,
                                                         World.fRand(0, 1)),
                                              ParticleType.SmokeAnim,
                                              World.vRand(generateParticles_t2,
                                                          1.0f));
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
            Vector pos = Vector.add(generateParticles_t1,
                                    blockOrigin,
                                    0.5f,
                                    0,
                                    0.5f);
            final float ParticlesPerSecond = 3;
            int count = (int)(Math.floor(curTime * ParticlesPerSecond) - Math.floor(lastTime
                    * ParticlesPerSecond));
            for(int i = 0; i < count; i++)
            {
                Entity p = Entity.NewParticle(pos,
                                              ParticleType.RedstoneFire,
                                              World.vRand(generateParticles_t2,
                                                          0.1f));
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
            Vector pos = getTorchTransform(this.data.orientation).apply(generateParticles_t1,
                                                                        Vector.set(generateParticles_t2,
                                                                                   0.5f,
                                                                                   1.0f,
                                                                                   0.5f))
                                                                 .addAndSet(blockOrigin);
            {
                final float ParticlesPerSecond = 5;
                int count = (int)(Math.floor(curTime * ParticlesPerSecond) - Math.floor(lastTime
                        * ParticlesPerSecond));
                for(int i = 0; i < count; i++)
                {
                    Entity p;
                    p = Entity.NewParticle(pos,
                                           ParticleType.FireAnim,
                                           World.vRand(generateParticles_t2,
                                                       0.1f));
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
                                           World.vRand(generateParticles_t2,
                                                       0.1f));
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
            // .concat(Matrix.rotateY(Math.PI / 2.0
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
        case BTSnow:
        case BTVines:
        case BTWoodAxe:
        case BTStoneAxe:
        case BTIronAxe:
        case BTGoldAxe:
        case BTDiamondAxe:
        case BTShears:
        case BTBucket:
        case BTRedstoneComparator:
        case BTQuartz:
        case BTDispenser:
        case BTDropper:
        case BTCobweb:
        case BTString:
        case BTBow:
        case BTHopper:
        case BTCactus:
        case BTRedMushroom:
        case BTBrownMushroom:
        case BTDeadBush:
        case BTWoodHoe:
        case BTStoneHoe:
        case BTIronHoe:
        case BTGoldHoe:
        case BTDiamondHoe:
        case BTDandelion:
        case BTRose:
        case BTTallGrass:
        case BTWheat:
        case BTFarmland:
        case BTSeeds:
        case BTCocoa:
        case BTInkSac:
        case BTRoseRed:
        case BTCactusGreen:
        case BTPurpleDye:
        case BTCyanDye:
        case BTLightGrayDye:
        case BTGrayDye:
        case BTPinkDye:
        case BTLimeDye:
        case BTDandelionYellow:
        case BTLightBlueDye:
        case BTMagentaDye:
        case BTOrangeDye:
        case BTBoneMeal:
        case BTBone:
        case BTWool:
            return;
        }
    }

    private boolean isLiquidSupported(final int bx, final int by, final int bz)
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

    /** @return this tree block's tree type */
    public Tree.TreeType treeGetTreeType()
    {
        Tree.TreeType[] values = Tree.TreeType.values();
        return values[this.data.intdata];
    }

    private Block moveHandleEmptySpaceChangeToFluid(final int bx,
                                                    final int by,
                                                    final int bz)
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

    private static Vector move_t1 = Vector.allocate();
    private static Vector move_t2 = Vector.allocate();

    /** called to evaluate general moves
     * 
     * @param bx
     *            block x coordinate
     * @param by
     *            block y coordinate
     * @param bz
     *            block z coordinate
     * @return the block that this block changes to or null if it doesn't change */
    public Block move(final int bx, final int by, final int bz)
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
        case BTStoneButton:
        case BTWoodButton:
        case BTLever:
            return moveHandleEmptySpaceChangeToFluid(bx, by, bz);
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
            Block sourceblock = this.data.blockdata;
            int destcount = this.data.destcount;
            int srccount = this.data.srccount;
            if(fuelleft <= 0 || sourceblock == null || srccount <= 0
                    || destcount >= BLOCK_STACK_SIZE)
                return null;
            Block retval = null;
            if(this.data.runTime < 0)
                retval = NewFurnace(fuelleft, sourceblock, srccount, destcount);
            double runTime = this.data.runTime;
            if(retval != null)
                runTime = retval.data.runTime;
            if(world.getCurTime() < runTime)
            {
                world.addTimedInvalidate(bx,
                                         by,
                                         bz,
                                         runTime - world.getCurTime());
                return retval;
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
        {
            int v = MAX_LEAVES_DISTANCE;
            for(int dir = 0; dir < 6; dir++)
            {
                Block b = world.getBlockEval(bx + getOrientationDX(dir), by
                        + getOrientationDY(dir), bz + getOrientationDZ(dir));
                if(b == null)
                    return null;
                if(b.getType() == BlockType.BTLeaves)
                {
                    v = Math.min(v, b.data.step);
                }
                else if(b.getType() == BlockType.BTWood)
                {
                    if(this.data.step != 0)
                        return NewLeaves(treeGetTreeType(), 0);
                    return null;
                }
            }
            v++;
            if(v != this.data.step)
                return NewLeaves(treeGetTreeType(), v);
            return null;
        }
        case BTRedstoneTorchOff:
        case BTRedstoneTorchOn:
        case BTRedstoneDustOff:
        case BTRedstoneDustOn:
        case BTSapling:
        case BTTorch:
        case BTRedMushroom:
        case BTBrownMushroom:
        case BTRose:
        case BTDandelion:
        {
            Block retval = moveHandleEmptySpaceChangeToFluid(bx, by, bz);
            if(retval == null)
                return null;
            world.insertEntity(Entity.NewBlock(Vector.set(move_t1,
                                                          bx + 0.5f,
                                                          by + 0.5f,
                                                          bz + 0.5f),
                                               this.type.make(-1),
                                               World.vRand(move_t2, 0.1f)));
            return retval;
        }
        case BTCocoa:
        {
            Block retval = moveHandleEmptySpaceChangeToFluid(bx, by, bz);
            if(retval == null)
            {
                Block b = world.getBlockEval(bx
                        + getOrientationDX(this.data.orientation), by
                        + getOrientationDY(this.data.orientation), bz
                        + getOrientationDZ(this.data.orientation));
                if(b != null
                        && (b.getType() != BlockType.BTWood || b.treeGetTreeType() != TreeType.Jungle))
                    retval = new Block();
            }
            if(retval == null)
                return null;
            digBlock(bx, by, bz, true, ToolType.None);
            return retval;
        }
        case BTTallGrass:
        {
            Block retval = moveHandleEmptySpaceChangeToFluid(bx, by, bz);
            if(retval == null
                    && ((this.light < 8 && this.scatteredSunlight < 8) || !isBlockSupported(bx,
                                                                                            by,
                                                                                            bz,
                                                                                            4)))
                retval = new Block();
            if(retval == null)
                return null;
            if(World.fRand(0, 8) <= 1)
                world.insertEntity(Entity.NewBlock(Vector.set(move_t1,
                                                              bx + 0.5f,
                                                              by + 0.5f,
                                                              bz + 0.5f),
                                                   NewSeeds(0),
                                                   World.vRand(move_t2, 0.1f)));
            return retval;
        }
        case BTSeeds:
        {
            Block ny = world.getBlockEval(bx, by - 1, bz);
            Block retval = moveHandleEmptySpaceChangeToFluid(bx, by, bz);
            if(retval == null
                    && ((this.light < 8 && this.scatteredSunlight < 8) || (ny != null && ny.getType() != BlockType.BTFarmland)))
                retval = new Block();
            if(retval == null)
                return null;
            int count = Math.min(2, (int)Math.floor(World.fRand(0, 2 + 1)));
            for(int i = 0; i < count; i++)
                world.insertEntity(Entity.NewBlock(Vector.set(move_t1,
                                                              bx + 0.5f,
                                                              by + 0.5f,
                                                              bz + 0.5f),
                                                   this.type.make(-1),
                                                   World.vRand(move_t2, 0.1f)));
            if(this.data.intdata >= 7)
                world.insertEntity(Entity.NewBlock(Vector.set(move_t1,
                                                              bx + 0.5f,
                                                              by + 0.5f,
                                                              bz + 0.5f),
                                                   NewWheat(),
                                                   World.vRand(move_t2, 0.1f)));
            return retval;
        }
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
        case BTLadder:
        case BTRedstoneRepeaterOff:
        case BTRedstoneRepeaterOn:
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
        case BTWoodAxe:
        case BTStoneAxe:
        case BTIronAxe:
        case BTGoldAxe:
        case BTDiamondAxe:
        case BTShears:
        case BTBucket:
        case BTRedstoneComparator:
        case BTQuartz:
        case BTDispenser:
        case BTDropper:
        case BTCobweb:
        case BTString:
        case BTBow:
        case BTHopper:
        case BTCactus:
        case BTWoodHoe:
        case BTStoneHoe:
        case BTIronHoe:
        case BTGoldHoe:
        case BTDiamondHoe:
            break;
        case BTDeadBush:
        case BTSnow:
        {
            Block retval = moveHandleEmptySpaceChangeToFluid(bx, by, bz);
            if(retval != null)
                return retval;
            if(!isBlockSupported(bx, by, bz, 4))
                return new Block();
            return null;
        }
        case BTVines:
        {
            Block retval = moveHandleEmptySpaceChangeToFluid(bx, by, bz);
            if(retval != null)
                return retval;
            if(isBlockSupported(bx, by, bz, this.data.orientation))
                return null;
            Block py = world.getBlockEval(bx, by + 1, bz);
            if(py == null)
                return null;
            if(py.getType() == BlockType.BTVines
                    && py.data.orientation == this.data.orientation)
                return null;
            return new Block();
        }
        case BTWheat:
            return null;
        case BTFarmland:
        {
            Block py = world.getBlockEval(bx, by + 1, bz);
            if(py == null)
                return null;
            if(py.isSolid())
                return NewDirt();
            if(py.getType() == BlockType.BTPiston
                    || py.getType() == BlockType.BTStickyPiston)
                return NewDirt();
            return null;
        }
        case BTInkSac:
        case BTRoseRed:
        case BTCactusGreen:
        case BTPurpleDye:
        case BTCyanDye:
        case BTLightGrayDye:
        case BTGrayDye:
        case BTPinkDye:
        case BTLimeDye:
        case BTDandelionYellow:
        case BTLightBlueDye:
        case BTMagentaDye:
        case BTOrangeDye:
        case BTBoneMeal:
        case BTBone:
        case BTWool:
            return null;
        }
        return null;
    }

    /** called to evaluate random moves
     * 
     * @param bx
     *            block x coordinate
     * @param by
     *            block y coordinate
     * @param bz
     *            block z coordinate
     * @return the block that this block changes to or null if it doesn't change */
    public Block moveRandom(final int bx, final int by, final int bz)
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
            Block py = world.getBlockEval(bx, by + 1, bz);
            Block ny = world.getBlockEval(bx, by - 1, bz);
            if(py != null)
            {
                if(py.getType() == BlockType.BTVines)
                {
                    return new Block(py);
                }
            }
            if(ny != null)
            {
                if(ny.getType() == BlockType.BTCactus)
                {
                    int stackHeight = 0;
                    Block b;
                    int i;
                    for(i = 1, b = ny; b != null
                            && b.getType() == BlockType.BTCactus; i++, b = world.getBlockEval(bx,
                                                                                              by
                                                                                                      - i,
                                                                                              bz))
                    {
                        stackHeight++;
                    }
                    if(b != null && b.getType() == BlockType.BTSand
                            && stackHeight < 3)
                        return NewCactus();
                }
            }
            int redMushroomCount = 0, brownMushroomCount = 0;
            for(int dx = -3; dx <= 3; dx++)
            {
                for(int dy = -3; dy <= 3; dy++)
                {
                    for(int dz = -3; dz <= 3; dz++)
                    {
                        Block b = world.getBlockEval(dx + bx, dy + by, dz + bz);
                        if(b != null)
                        {
                            if(b.getType() == BlockType.BTRedMushroom)
                            {
                                if(World.fRand(0, dy == 0 ? 2 : 4) < 1)
                                    redMushroomCount++;
                            }
                            else if(b.getType() == BlockType.BTBrownMushroom)
                            {
                                if(World.fRand(0, dy == 0 ? 2 : 4) < 1)
                                    brownMushroomCount++;
                            }
                        }
                    }
                }
            }
            if(this.light <= 12 && this.scatteredSunlight <= 12 && ny != null
                    && ny.isOpaque() && World.fRand(0, 1) <= 0.5f
                    && redMushroomCount + brownMushroomCount <= 3)
            {
                if(redMushroomCount > 0 && brownMushroomCount > 0)
                {
                    if(World.fRand(0, 1) <= (float)brownMushroomCount
                            / redMushroomCount)
                        return NewBrownMushroom();
                    return NewRedMushroom();
                }
                else if(redMushroomCount > 0)
                    return NewRedMushroom();
                else if(brownMushroomCount > 0)
                    return NewBrownMushroom();
            }
            return null;
        }
        case BTChest:
        case BTCoal:
        case BTCoalOre:
        case BTCobblestone:
        case BTDiamond:
        case BTDiamondOre:
        case BTDiamondPick:
        case BTDiamondShovel:
        case BTEmerald:
        case BTEmeraldOre:
        case BTWheat:
        case BTTallGrass:
        case BTDandelion:
        case BTRose:
            break;
        case BTDirt:
        {
            {
                Block py = world.getBlockEval(bx, by + 1, bz);
                if(py == null)
                    break;
                if(py.isOpaque())
                    break;
                if(Math.max(py.light, py.scatteredSunlight) < 4)
                    break;
            }
            final int dist = 3;
            for(int dx = -dist; dx <= dist; dx++)
            {
                for(int dy = -dist; dy <= dist; dy++)
                {
                    for(int dz = -dist; dz <= dist; dz++)
                    {
                        Block b = world.getBlockEval(bx + dx, by + dy, bz + dz);
                        if(b != null && b.getType() == BlockType.BTGrass)
                        {
                            return NewGrass();
                        }
                    }
                }
            }
            break;
        }
        case BTGrass:
        {
            Block py = world.getBlockEval(bx, by + 1, bz);
            if(py == null)
                break;
            if(py.isOpaque())
                return NewDirt();
            if(Math.max(py.light, py.scatteredSunlight) < 4)
                return NewDirt();
            break;
        }
        case BTFurnace:
        case BTGlass:
        case BTGoldIngot:
        case BTGoldOre:
        case BTGoldPick:
        case BTGoldShovel:
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
            break;
        case BTLeaves:
        {
            if(this.data.step > MAX_LEAVES_DISTANCE)
            {
                digBlock(bx, by, bz, true, ToolType.None);
                return new Block();
            }
            return null;
        }
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
                return null;
            if(py.getType() != BlockType.BTEmpty)
                return null;
            if(world.getLighting(bx + 0.5f, by + 0.5f, bz + 0.5f) <= 7 / 15.0)
                return null;
            Block retval = new Block();
            world.addNewTree(bx, by, bz, this);
            return retval;
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
        case BTSnow:
        {
            if(this.light > 11)
                return new Block();
            break;
        }
        case BTVines:
        case BTWoodAxe:
        case BTStoneAxe:
        case BTIronAxe:
        case BTGoldAxe:
        case BTDiamondAxe:
        case BTShears:
        case BTBucket:
        case BTRedstoneComparator:
        case BTQuartz:
        case BTDispenser:
        case BTDropper:
        case BTCobweb:
        case BTString:
        case BTBow:
        case BTHopper:
        case BTCactus:
        case BTRedMushroom:
        case BTBrownMushroom:
        case BTDeadBush:
        case BTWoodHoe:
        case BTStoneHoe:
        case BTIronHoe:
        case BTGoldHoe:
        case BTDiamondHoe:
            break;
        case BTSeeds:
        {
            Block ny = world.getBlockEval(bx, by - 1, bz);
            if(ny != null && ny.getType() == BlockType.BTFarmland
                    && ny.data.intdata == 0 && World.fRand(0, 2) > 1)
                return null;
            if(this.data.intdata < 7)
            {
                Block retval = new Block(this);
                retval.data.intdata++;
                return retval;
            }
            return null;
        }
        case BTFarmland:
        {
            boolean isWet = false;
            findWaterLoop: for(int dy = 0; dy <= 1; dy++)
            {
                for(int dx = -4; dx <= 4; dx++)
                {
                    for(int dz = -4; dz <= 4; dz++)
                    {
                        Block b = world.getBlockEval(bx + dx, by + dy, bz + dz);
                        if(b == null)
                            return null;
                        if(b.getType() == BlockType.BTWater)
                        {
                            isWet = true;
                            break findWaterLoop;
                        }
                    }
                }
            }
            if(this.data.intdata != 0 && isWet)
            {
                return null;
            }
            if(this.data.intdata == 0 && !isWet)
            {
                return NewDirt();
            }
            return NewFarmland(isWet);
        }
        case BTCocoa:
        {
            if(this.data.intdata < 2)
            {
                Block retval = new Block(this);
                retval.data.intdata++;
                return retval;
            }
            return null;
        }
        case BTInkSac:
        case BTRoseRed:
        case BTCactusGreen:
        case BTPurpleDye:
        case BTCyanDye:
        case BTLightGrayDye:
        case BTGrayDye:
        case BTPinkDye:
        case BTLimeDye:
        case BTDandelionYellow:
        case BTLightBlueDye:
        case BTMagentaDye:
        case BTOrangeDye:
        case BTBoneMeal:
        case BTBone:
        case BTWool:
            return null;
        }
        return null;
    }

    private boolean redstoneRepeaterIsLatched(final int bx,
                                              final int by,
                                              final int bz)
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

    private static Vector TNTExplode_t1 = Vector.allocate();

    /** explode this TNT block
     * 
     * @param bx
     *            x coordinate of this block
     * @param by
     *            y coordinate of this block
     * @param bz
     *            z coordinate of this block */
    public void TNTExplode(final int bx, final int by, final int bz)
    {
        world.insertEntity(Entity.NewPrimedTNT(Vector.set(TNTExplode_t1,
                                                          bx,
                                                          by,
                                                          bz), 1));
        world.setBlock(bx, by, bz, new Block());
    }

    private static Vector redstoneMove_t1 = Vector.allocate();

    /** called to evaluate redstone moves
     * 
     * @param bx
     *            block x coordinate
     * @param by
     *            block y coordinate
     * @param bz
     *            block z coordinate
     * @return the block that this block changes to or null if it doesn't change */
    public Block redstoneMove(final int bx, final int by, final int bz)
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
        case BTSnow:
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
                world.insertEntity(Entity.NewPrimedTNT(Vector.set(redstoneMove_t1,
                                                                  bx,
                                                                  by,
                                                                  bz),
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
            if(this.data.intdata > 0)
            {
                Block retval = new Block(this);
                retval.data.intdata--;
                return retval;
            }
            return null;
        }
        case BTVines:
        case BTWoodAxe:
        case BTStoneAxe:
        case BTIronAxe:
        case BTGoldAxe:
        case BTDiamondAxe:
        case BTShears:
        case BTBucket:
        case BTQuartz:
        case BTCobweb:
        case BTString:
        case BTBow:
        case BTWoodHoe:
        case BTStoneHoe:
        case BTIronHoe:
        case BTGoldHoe:
        case BTDiamondHoe:
            return null;
        case BTRedstoneComparator:
        {
            int backInput = getEvalRedstoneIOValue(bx
                                                           - getOrientationDX(this.data.orientation),
                                                   by
                                                           - getOrientationDY(this.data.orientation),
                                                   bz
                                                           - getOrientationDZ(this.data.orientation),
                                                   this.data.orientation);
            int backInputLevel = 0;
            if(backInput == REDSTONE_POWER_STRONG)
                backInputLevel = 15;
            else if(backInput >= REDSTONE_POWER_WEAK_MIN
                    && backInput <= REDSTONE_POWER_WEAK_MAX)
                backInputLevel = 1 + backInput - REDSTONE_POWER_WEAK_MIN;
            if(backInputLevel == 0)
            {
                Block b = world.getBlockEval(bx
                        - getOrientationDX(this.data.orientation), by
                        - getOrientationDY(this.data.orientation), bz
                        - getOrientationDZ(this.data.orientation));
                if(b != null)
                {
                    int containerLevel = b.getContainerLevel();
                    if(containerLevel >= 0)
                        backInputLevel = containerLevel;
                    else if(b.isSolid())
                    {
                        b = world.getBlockEval(bx
                                                       - 2
                                                       * getOrientationDX(this.data.orientation),
                                               by
                                                       - 2
                                                       * getOrientationDY(this.data.orientation),
                                               bz
                                                       - 2
                                                       * getOrientationDZ(this.data.orientation));
                        if(b != null)
                        {
                            containerLevel = b.getContainerLevel();
                            if(containerLevel >= 0)
                                backInputLevel = containerLevel;
                        }
                    }
                }
            }
            int sideInputLevel = 0;
            {
                int o = getRotatedOrientation(5, 1, this.data.orientation);
                int sideInput = getEvalRedstoneIOValue(bx - getOrientationDX(o),
                                                       by - getOrientationDY(o),
                                                       bz - getOrientationDZ(o),
                                                       o);
                int v = 0;
                if(sideInput == REDSTONE_POWER_STRONG)
                    v = 15;
                else if(sideInput >= REDSTONE_POWER_WEAK_MIN
                        && sideInput <= REDSTONE_POWER_WEAK_MAX)
                    v = 1 + sideInput - REDSTONE_POWER_WEAK_MIN;
                sideInputLevel = Math.max(sideInputLevel, v);
            }
            {
                int o = getRotatedOrientation(5, -1, this.data.orientation);
                int sideInput = getEvalRedstoneIOValue(bx - getOrientationDX(o),
                                                       by - getOrientationDY(o),
                                                       bz - getOrientationDZ(o),
                                                       o);
                int v = 0;
                if(sideInput == REDSTONE_POWER_STRONG)
                    v = 15;
                else if(sideInput >= REDSTONE_POWER_WEAK_MIN
                        && sideInput <= REDSTONE_POWER_WEAK_MAX)
                    v = 1 + sideInput - REDSTONE_POWER_WEAK_MIN;
                sideInputLevel = Math.max(sideInputLevel, v);
            }
            int newOutputLevel;
            if(this.data.step != 0) // is subtract mode
            {
                newOutputLevel = Math.max(0, backInputLevel - sideInputLevel);
            }
            else
            {
                if(backInputLevel >= sideInputLevel)
                    newOutputLevel = backInputLevel;
                else
                    newOutputLevel = 0;
            }
            if(this.data.intdata != newOutputLevel)
                return NewRedstoneComparator(this.data.step != 0,
                                             newOutputLevel,
                                             this.data.orientation);
            return null;
        }
        case BTDispenser:
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
            if(this.data.intdata == 0 && !isOn)
                return null;
            if(this.data.intdata != 0 && isOn)
                return null;
            Block retval = new Block(this);
            retval.data.intdata = isOn ? 1 : 0;
            if(isOn)
                retval.dispenserDispenseBlock(bx, by, bz);
            return retval;
        }
        case BTDropper:
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
            if(this.data.intdata == 0 && !isOn)
                return null;
            if(this.data.intdata != 0 && isOn)
                return null;
            Block retval = new Block(this);
            retval.data.intdata = isOn ? 1 : 0;
            if(isOn)
                retval.dropperDropBlock(bx, by, bz);
            return retval;
        }
        case BTHopper:
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
            if(this.data.intdata == 0 && !isOn)
            {
                Block removeFromBlock = world.getBlockEval(bx, by + 1, bz);
                boolean canTransfer = false;
                int step = this.data.step;
                if(removeFromBlock != null)
                {
                    int removeDescriptor = removeFromBlock.makeRemoveBlockFromContainerDescriptor(4);
                    if(removeFromBlock.getRemovedBlockFromContainer(4,
                                                                    removeDescriptor) != null)
                    {
                        if(!canTransfer)
                            step++;
                        canTransfer = true;
                        if(step >= HOPPER_TRANSFER_STEP_COUNT)
                        {
                            world.insertEntity(Entity.NewTransferItem(bx,
                                                                      by + 1,
                                                                      bz,
                                                                      bx,
                                                                      by,
                                                                      bz));
                        }
                    }
                }
                int destX = bx + getOrientationDX(this.data.orientation);
                int destY = by + getOrientationDY(this.data.orientation);
                int destZ = bz + getOrientationDZ(this.data.orientation);
                Block addToBlock = world.getBlockEval(destX, destY, destZ);
                if(addToBlock != null && addToBlock.isContainer())
                {
                    if(!canTransfer)
                        step++;
                    canTransfer = true;
                    if(step >= HOPPER_TRANSFER_STEP_COUNT)
                    {
                        world.insertEntity(Entity.NewTransferItem(bx,
                                                                  by,
                                                                  bz,
                                                                  destX,
                                                                  destY,
                                                                  destZ));
                    }
                }
                if(!canTransfer || step >= HOPPER_TRANSFER_STEP_COUNT)
                    step = 0;
                if(step != this.data.step)
                {
                    Block retval = new Block(this);
                    retval.data.step = step;
                    return retval;
                }
                return null;
            }
            if(this.data.intdata != 0 && isOn)
                return null;
            Block retval = new Block(this);
            retval.data.intdata = isOn ? 1 : 0;
            retval.data.step = 0;
            return retval;
        }
        case BTCactus:
        case BTRedMushroom:
        case BTBrownMushroom:
        case BTDeadBush:
        case BTTallGrass:
        case BTWheat:
        case BTSeeds:
        case BTDandelion:
        case BTRose:
        case BTFarmland:
        case BTCocoa:
        case BTInkSac:
        case BTRoseRed:
        case BTCactusGreen:
        case BTPurpleDye:
        case BTCyanDye:
        case BTLightGrayDye:
        case BTGrayDye:
        case BTPinkDye:
        case BTLimeDye:
        case BTDandelionYellow:
        case BTLightBlueDye:
        case BTMagentaDye:
        case BTOrangeDye:
        case BTBoneMeal:
        case BTBone:
        case BTWool:
            return null;
        }
        return null;
    }

    public static final int HOPPER_TRANSFER_STEP_COUNT = 4;
    public static final int DISPENSER_DROPPER_ROWS = 3;
    public static final int DISPENSER_DROPPER_COLUMNS = 3;

    private int getDispenserDropperSlotIndex(final int row, final int column)
    {
        return row + DISPENSER_DROPPER_ROWS * column;
    }

    private int pickDispenserDropperRandomSlot()
    {
        int count = 0;
        for(int row = 0; row < DISPENSER_DROPPER_ROWS; row++)
        {
            for(int column = 0; column < DISPENSER_DROPPER_COLUMNS; column++)
            {
                int index = getDispenserDropperSlotIndex(row, column);
                if(this.data.BlockCounts[index] > 0)
                    count++;
            }
        }
        if(count <= 0)
            return -1;
        int pos = (int)Math.floor(World.fRand(0, count));
        if(pos >= count)
            pos = count - 1;
        count = 0;
        for(int row = 0; row < DISPENSER_DROPPER_ROWS; row++)
        {
            for(int column = 0; column < DISPENSER_DROPPER_COLUMNS; column++)
            {
                int index = getDispenserDropperSlotIndex(row, column);
                if(this.data.BlockCounts[index] > 0)
                {
                    if(pos == count)
                        return index;
                    count++;
                }
            }
        }
        return -1; // shouldn't ever get here
    }

    private void dropperDropBlock(final int bx, final int by, final int bz)
    {
        int index = pickDispenserDropperRandomSlot();
        if(index == -1)
            return;
        Block b = this.data.BlockTypes[index];
        int dx = getOrientationDX(this.data.orientation);
        int dy = getOrientationDY(this.data.orientation);
        int dz = getOrientationDZ(this.data.orientation);
        Vector dir = Vector.allocate(dx, dy, dz);
        if(!b.onDrop(bx, by, bz, bx + dx, by + dy, bz + dz, dir))
        {
            dir.free();
            return;
        }
        dir.free();
        this.data.BlockCounts[index]--;
        if(this.data.BlockCounts[index] <= 0)
            this.data.BlockTypes[index] = null;
    }

    private static Vector runTransferItem_t1 = Vector.allocate();

    public void runTransferItem(final int srcX,
                                final int srcY,
                                final int srcZ,
                                final int destX,
                                final int destY,
                                final int destZ)
    {
        if(!isContainer())
            return;
        Block dest = new Block(world.getBlockEval(destX, destY, destZ));
        if(dest == null || !dest.isContainer())
            return;
        Block src = new Block(this);
        final int o = getOrientationFromVector(runTransferItem_t1.set(destX,
                                                                      destY,
                                                                      destZ)
                                                                 .subAndSet(srcX,
                                                                            srcY,
                                                                            srcZ));
        final int descriptor = src.makeRemoveBlockFromContainerDescriptor(o);
        if(descriptor == -1)
            return;
        final Block b = src.getRemovedBlockFromContainer(o, descriptor);
        if(b == null)
            return;
        if(dest.addBlockToContainer(b, getNegOrientation(o)))
        {
            src.removeBlockFromContainer(o, descriptor);
            world.setBlock(srcX, srcY, srcZ, src);
            world.setBlock(destX, destY, destZ, dest);
        }
    }

    private static Vector onDrop_t1 = Vector.allocate();
    private static Vector onDrop_t2 = Vector.allocate();

    private boolean onDrop(final int srcX,
                           final int srcY,
                           final int srcZ,
                           final int destX,
                           final int destY,
                           final int destZ,
                           final Vector dir)
    {
        if(this.type == BlockType.BTEmpty)
            return false;
        Block b = world.getBlockEval(destX, destY, destZ);
        if(b != null && b.isContainer())
        {
            world.insertEntity(Entity.NewTransferItem(srcX,
                                                      srcY,
                                                      srcZ,
                                                      destX,
                                                      destY,
                                                      destZ));
            return false;
        }
        world.insertEntity(Entity.NewBlock(onDrop_t1.set(dir)
                                                    .mulAndSet(-(0.5f - 0.25f + 0.05f))
                                                    .addAndSet(destX + 0.5f,
                                                               destY + 0.5f,
                                                               destZ + 0.5f),
                                           this,
                                           World.vRand(onDrop_t2, 0.2f)
                                                .addAndSet(dir)
                                                .mulAndSet(5f)));
        return true;
    }

    private void
        dispenserDispenseBlock(final int bx, final int by, final int bz)
    {
        Block b;
        {
            int index = pickDispenserDropperRandomSlot();
            if(index == -1)
                return;
            b = this.data.BlockTypes[index];
            this.data.BlockCounts[index]--;
            if(this.data.BlockCounts[index] <= 0)
                this.data.BlockTypes[index] = null;
        }
        int dx = getOrientationDX(this.data.orientation);
        int dy = getOrientationDY(this.data.orientation);
        int dz = getOrientationDZ(this.data.orientation);
        Vector dir = Vector.allocate(dx, dy, dz);
        b = b.onDispense(bx, by, bz, bx + dx, by + dy, bz + dz, dir);
        if(b != null && b.getType() != BlockType.BTEmpty)
        {
            findEmptySlotLoop: for(int row = 0; row < DISPENSER_DROPPER_ROWS; row++)
            {
                for(int column = 0; column < DISPENSER_DROPPER_COLUMNS; column++)
                {
                    int index = getDispenserDropperSlotIndex(row, column);
                    if(this.data.BlockCounts[index] <= 0)
                    {
                        this.data.BlockCounts[index]++;
                        this.data.BlockTypes[index] = b;
                        b = null;
                        break findEmptySlotLoop;
                    }
                    if(b.equals(this.data.BlockTypes[index]))
                    {
                        if(this.data.BlockCounts[index] < BLOCK_STACK_SIZE)
                        {
                            this.data.BlockCounts[index]++;
                            b = null;
                            break findEmptySlotLoop;
                        }
                    }
                }
            }
            if(b != null)
            {
                b.onDrop(bx, by, bz, bx + dx, by + dy, bz + dz, dir);
            }
        }
        dir.free();
    }

    private static Vector onDispense_t1 = Vector.allocate();
    private static Vector onDispense_t2 = Vector.allocate();

    private Block onDispense(final int srcX,
                             final int srcY,
                             final int srcZ,
                             final int destX,
                             final int destY,
                             final int destZ,
                             final Vector dir)
    {
        switch(this.type)
        {
        case BTDeleteBlock:
        case BTLast:
        case BTMoon:
        case BTSun:
            return null;
        case BTEmpty:
            return null;
        case BTBucket:
        {
            Block b = world.getBlockEval(destX, destY, destZ);
            if(b == null || !b.isItemInBucket())
            {
                world.insertEntity(Entity.NewBlock(onDispense_t1.set(dir)
                                                                .mulAndSet(-(0.5f - 0.25f + 0.05f))
                                                                .addAndSet(destX + 0.5f,
                                                                           destY + 0.5f,
                                                                           destZ + 0.5f),
                                                   this,
                                                   World.vRand(onDispense_t2,
                                                               0.2f)
                                                        .addAndSet(dir)
                                                        .mulAndSet(5f)));
                return null;
            }
            world.insertEntity(Entity.NewRemoveBlockIfEqual(onDispense_t1.set(destX,
                                                                              destY,
                                                                              destZ),
                                                            b));
            return b.getItemInBucket();
        }
        case BTLava:
        case BTWater:
        {
            Block b = world.getBlockEval(destX, destY, destZ);
            if(b == null || !b.isReplaceable())
                return this;
            world.insertEntity(Entity.NewPlaceBlockIfReplaceable(onDispense_t1.set(destX,
                                                                                   destY,
                                                                                   destZ),
                                                                 this));
            return NewBucket();
        }
        case BTSnow:
            world.insertEntity(Entity.NewBlock(onDispense_t1.set(dir)
                                                            .mulAndSet(-(0.5f - 0.25f + 0.01f))
                                                            .addAndSet(destX + 0.5f,
                                                                       destY + 0.5f,
                                                                       destZ + 0.5f),
                                               this,
                                               World.vRand(onDispense_t2, 0.2f)
                                                    .addAndSet(dir)
                                                    .mulAndSet(15f)));
            return null;
        case BTTNT:
            world.insertEntity(Entity.NewPrimedTNT(onDispense_t1.set(destX,
                                                                     destY,
                                                                     destZ), 1));
            return null;
        case BTBoneMeal:
            world.insertEntity(Entity.NewApplyBoneMealOrPutBackInContainer(destX,
                                                                           destY,
                                                                           destZ,
                                                                           srcX,
                                                                           srcY,
                                                                           srcZ));
            return null;
        case BTBedrock:
        case BTBlazePowder:
        case BTBlazeRod:
        case BTChest:
        case BTCoal:
        case BTCoalOre:
        case BTCobblestone:
        case BTDiamond:
        case BTDiamondAxe:
        case BTDiamondOre:
        case BTDiamondPick:
        case BTDiamondShovel:
        case BTDirt:
        case BTDispenser:
        case BTDropper:
        case BTEmerald:
        case BTEmeraldOre:
        case BTFurnace:
        case BTGlass:
        case BTGoldAxe:
        case BTGoldIngot:
        case BTGoldOre:
        case BTGoldPick:
        case BTGoldShovel:
        case BTGrass:
        case BTGravel:
        case BTGunpowder:
        case BTIronAxe:
        case BTIronIngot:
        case BTIronOre:
        case BTIronPick:
        case BTIronShovel:
        case BTLadder:
        case BTLapisLazuli:
        case BTLapisLazuliOre:
        case BTLeaves:
        case BTLever:
        case BTObsidian:
        case BTPiston:
        case BTPistonHead:
        case BTPlank:
        case BTQuartz:
        case BTRedstoneBlock:
        case BTRedstoneComparator:
        case BTRedstoneDustOff:
        case BTRedstoneDustOn:
        case BTRedstoneOre:
        case BTRedstoneRepeaterOff:
        case BTRedstoneRepeaterOn:
        case BTRedstoneTorchOff:
        case BTRedstoneTorchOn:
        case BTSand:
        case BTSapling:
        case BTShears:
        case BTSlime:
        case BTStick:
        case BTStickyPiston:
        case BTStickyPistonHead:
        case BTStone:
        case BTStoneAxe:
        case BTStoneButton:
        case BTStonePick:
        case BTStonePressurePlate:
        case BTStoneShovel:
        case BTTorch:
        case BTVines:
        case BTWood:
        case BTWoodAxe:
        case BTWoodButton:
        case BTWoodPick:
        case BTWoodPressurePlate:
        case BTWoodShovel:
        case BTWorkbench:
        case BTCobweb:
        case BTString:
        case BTBow:
        case BTHopper:
        case BTCactus:
        case BTRedMushroom:
        case BTBrownMushroom:
        case BTDeadBush:
        case BTWoodHoe:
        case BTStoneHoe:
        case BTIronHoe:
        case BTGoldHoe:
        case BTDiamondHoe:
        case BTDandelion:
        case BTRose:
        case BTFarmland:
        case BTWheat:
        case BTSeeds:
        case BTTallGrass:
        case BTCocoa:
        case BTInkSac:
        case BTRoseRed:
        case BTCactusGreen:
        case BTPurpleDye:
        case BTCyanDye:
        case BTLightGrayDye:
        case BTGrayDye:
        case BTPinkDye:
        case BTLimeDye:
        case BTDandelionYellow:
        case BTLightBlueDye:
        case BTMagentaDye:
        case BTOrangeDye:
        case BTBone:
        case BTWool:
            world.insertEntity(Entity.NewBlock(onDispense_t1.set(dir)
                                                            .mulAndSet(-(0.5f - 0.25f + 0.05f))
                                                            .addAndSet(destX + 0.5f,
                                                                       destY + 0.5f,
                                                                       destZ + 0.5f),
                                               this,
                                               World.vRand(onDispense_t2, 0.2f)
                                                    .addAndSet(dir)
                                                    .mulAndSet(5f)));
            return null;
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
    public Block redstoneDustMove(final int bx, final int by, final int bz)
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
    public void pistonMove(final int bx, final int by, final int bz)
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
                    nextBlock.digBlock(x, y, z, true, ToolType.None);
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
        case BTPlank:
            return PushType.Pushed;
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
        case BTWoodAxe:
        case BTStoneAxe:
        case BTIronAxe:
        case BTGoldAxe:
        case BTDiamondAxe:
        case BTShears:
        case BTBucket:
        case BTCobweb:
            return PushType.DropAsEntity;
        case BTSnow:
        case BTVines:
        case BTDeadBush:
            return PushType.Remove;
        case BTRedstoneComparator:
        case BTQuartz:
        case BTString:
        case BTBow:
        case BTCactus:
        case BTRedMushroom:
        case BTBrownMushroom:
        case BTWoodHoe:
        case BTStoneHoe:
        case BTIronHoe:
        case BTGoldHoe:
        case BTDiamondHoe:
        case BTDandelion:
        case BTRose:
        case BTWheat:
        case BTSeeds:
        case BTTallGrass:
        case BTCocoa:
        case BTInkSac:
        case BTRoseRed:
        case BTCactusGreen:
        case BTPurpleDye:
        case BTCyanDye:
        case BTLightGrayDye:
        case BTGrayDye:
        case BTPinkDye:
        case BTLimeDye:
        case BTDandelionYellow:
        case BTLightBlueDye:
        case BTMagentaDye:
        case BTOrangeDye:
        case BTBoneMeal:
        case BTBone:
            return PushType.DropAsEntity;
        case BTDispenser:
        case BTDropper:
        case BTHopper:
        case BTFarmland:
        case BTWool:
            return PushType.Pushed;
        }
        return PushType.NonPushable;
    }

    private boolean isBlockSupported(final int bx,
                                     final int by,
                                     final int bz,
                                     final int orientation)
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

    private static Vector evalBlockToEntity_t1 = Vector.allocate();
    private static Vector evalBlockToEntity_t2 = Vector.allocate();

    /** @param bx
     *            block x coordinate
     * @param by
     *            block y coordinate
     * @param bz
     *            block z coordinate
     * @return the entity that this block changes to or null if it doesn't
     *         change */
    public Entity evalBlockToEntity(final int bx, final int by, final int bz)
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
        case BTBedrock:
        case BTWater:
        case BTLava:
            return null;
        case BTSapling:
        {
            if((this.light < 8 && this.scatteredSunlight < 8)
                    || !isBlockSupported(bx, by, bz, 4))
                return Entity.NewBlock(Vector.set(evalBlockToEntity_t1,
                                                  0.5f + bx,
                                                  0.5f + by,
                                                  0.5f + bz),
                                       NewSapling(treeGetTreeType()),
                                       World.vRand(evalBlockToEntity_t2, 0.1f));
            return null;
        }
        case BTRose:
        case BTDandelion:
        {
            if((this.light < 8 && this.scatteredSunlight < 8)
                    || !isBlockSupported(bx, by, bz, 4))
                return Entity.NewBlock(Vector.set(evalBlockToEntity_t1,
                                                  0.5f + bx,
                                                  0.5f + by,
                                                  0.5f + bz),
                                       this.type.make(-1),
                                       World.vRand(evalBlockToEntity_t2, 0.1f));
            return null;
        }
        case BTTallGrass:
        case BTSeeds:
            return null;
        case BTSand:
        case BTGravel:
        {
            Block ny = world.getBlockEval(bx, by - 1, bz);
            if(ny != null && ny.isReplaceable())
                return Entity.NewFallingBlock(Vector.set(evalBlockToEntity_t1,
                                                         bx,
                                                         by,
                                                         bz), new Block(this));
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
                return Entity.NewBlock(Vector.set(evalBlockToEntity_t1,
                                                  0.5f + bx,
                                                  0.5f + by,
                                                  0.5f + bz),
                                       BlockType.BTRedstoneDustOff.make(-1),
                                       World.vRand(evalBlockToEntity_t2, 0.1f));
            return null;
        }
        case BTRedstoneOre:
        case BTRedstoneBlock:
            return null;
        case BTRedstoneTorchOff:
        case BTRedstoneTorchOn:
        {
            if(!isBlockSupported(bx, by, bz, this.data.orientation))
                return Entity.NewBlock(Vector.set(evalBlockToEntity_t1,
                                                  0.5f + bx,
                                                  0.5f + by,
                                                  0.5f + bz),
                                       BlockType.BTRedstoneTorchOff.make(-1),
                                       World.vRand(evalBlockToEntity_t2, 0.1f));
            return null;
        }
        case BTStoneButton:
        case BTWoodButton:
        case BTTorch:
        {
            if(!isBlockSupported(bx, by, bz, this.data.orientation))
                return Entity.NewBlock(Vector.set(evalBlockToEntity_t1,
                                                  0.5f + bx,
                                                  0.5f + by,
                                                  0.5f + bz),
                                       this.type.make(-1),
                                       World.vRand(evalBlockToEntity_t2, 0.1f));
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
        case BTDeadBush:
            return null;
        case BTLadder:
        {
            if(!isBlockSupported(bx, by, bz, this.data.orientation))
                return Entity.NewBlock(Vector.set(evalBlockToEntity_t1,
                                                  0.5f + bx,
                                                  0.5f + by,
                                                  0.5f + bz),
                                       this.type.make(-1),
                                       World.vRand(evalBlockToEntity_t2, 0.1f));
            return null;
        }
        case BTRedstoneRepeaterOff:
        case BTRedstoneRepeaterOn:
        case BTStonePressurePlate:
        case BTWoodPressurePlate:
        case BTRedstoneComparator:
        {
            if(!isBlockSupported(bx, by, bz, 4))
                return Entity.NewBlock(Vector.set(evalBlockToEntity_t1,
                                                  0.5f + bx,
                                                  0.5f + by,
                                                  0.5f + bz),
                                       this.type.make(-1),
                                       World.vRand(evalBlockToEntity_t2, 0.1f));
            return null;
        }
        case BTCactus:
        {
            boolean doDestroy = false;
            if(!isBlockSupported(bx, by, bz, 4))
                doDestroy = true;
            for(int o = 0; o <= 3 && !doDestroy; o++)
            {
                Block b = world.getBlockEval(bx + getOrientationDX(o), by
                        + getOrientationDY(o), bz + getOrientationDZ(o));
                if(b != null)
                {
                    if(b.isSolid() || b.isSupporting())
                        doDestroy = true;
                }
            }
            if(doDestroy)
                return Entity.NewBlock(Vector.set(evalBlockToEntity_t1,
                                                  0.5f + bx,
                                                  0.5f + by,
                                                  0.5f + bz),
                                       this.type.make(-1),
                                       World.vRand(evalBlockToEntity_t2, 0.1f));
            return null;
        }
        case BTLever:
        {
            if(!isBlockSupported(bx, by, bz, this.data.orientation))
                return Entity.NewBlock(Vector.set(evalBlockToEntity_t1,
                                                  0.5f + bx,
                                                  0.5f + by,
                                                  0.5f + bz),
                                       this.type.make(-1),
                                       World.vRand(evalBlockToEntity_t2, 0.1f));
            return null;
        }
        case BTObsidian:
        case BTPiston:
        case BTStickyPiston:
        case BTPistonHead:
        case BTStickyPistonHead:
        case BTSnow:
        case BTVines:
        case BTWoodAxe:
        case BTStoneAxe:
        case BTIronAxe:
        case BTGoldAxe:
        case BTDiamondAxe:
        case BTShears:
        case BTBucket:
        case BTQuartz:
        case BTDispenser:
        case BTDropper:
        case BTCobweb:
        case BTString:
        case BTBow:
        case BTHopper:
        case BTWoodHoe:
        case BTStoneHoe:
        case BTIronHoe:
        case BTGoldHoe:
        case BTDiamondHoe:
        case BTFarmland:
        case BTWheat:
        case BTCocoa:
        case BTInkSac:
        case BTRoseRed:
        case BTCactusGreen:
        case BTPurpleDye:
        case BTCyanDye:
        case BTLightGrayDye:
        case BTGrayDye:
        case BTPinkDye:
        case BTLimeDye:
        case BTDandelionYellow:
        case BTLightBlueDye:
        case BTMagentaDye:
        case BTOrangeDye:
        case BTBoneMeal:
        case BTBone:
        case BTWool:
            return null;
        case BTRedMushroom:
        case BTBrownMushroom:
        {
            if(this.light > 12 || this.scatteredSunlight > 12)
                return Entity.NewBlock(Vector.set(evalBlockToEntity_t1,
                                                  0.5f + bx,
                                                  0.5f + by,
                                                  0.5f + bz),
                                       this.type.make(-1),
                                       World.vRand(evalBlockToEntity_t2, 0.1f));
            if(!isBlockSupported(bx, by, bz, this.data.orientation))
                return Entity.NewBlock(Vector.set(evalBlockToEntity_t1,
                                                  0.5f + bx,
                                                  0.5f + by,
                                                  0.5f + bz),
                                       this.type.make(-1),
                                       World.vRand(evalBlockToEntity_t2, 0.1f));
            return null;
        }
        }
        return retval;
    }

    @Override
    public void move()
    {
    }

    private static Matrix rayIntersectsBlock_invtform = new Matrix();
    private static Vector rayIntersectsBlock_orig = Vector.allocate();
    private static Vector rayIntersectsBlock_dir = Vector.allocate();
    private static Vector rayIntersectsBlock_invdir = Vector.allocate();
    private static Vector rayIntersectsBlock_destpos = Vector.allocate();
    private static Vector rayIntersectsBlock_vt = Vector.allocate();
    private static Vector rayIntersectsBlock_hx = Vector.allocate();
    private static Vector rayIntersectsBlock_hy = Vector.allocate();
    private static Vector rayIntersectsBlock_hz = Vector.allocate();
    private static Vector rayIntersectsBlock_t1 = Vector.allocate();

    private float rayIntersectsBlock(final Vector origin,
                                     final Vector direction,
                                     final Matrix tform)
    {
        Matrix invtform = Matrix.setToInverse(rayIntersectsBlock_invtform,
                                              tform);
        Vector orig = invtform.apply(rayIntersectsBlock_orig, origin);
        Vector dir = invtform.apply(rayIntersectsBlock_dir, direction)
                             .subAndSet(invtform.apply(rayIntersectsBlock_t1,
                                                       Vector.ZERO));
        final float eps = 1e-4f;
        if(Math.abs(dir.getX()) < eps)
            dir.setX(eps);
        if(Math.abs(dir.getY()) < eps)
            dir.setY(eps);
        if(Math.abs(dir.getZ()) < eps)
            dir.setZ(eps);
        Vector invdir = Vector.div(rayIntersectsBlock_invdir, 1, 1, 1, dir);
        Vector destpos = Vector.set(rayIntersectsBlock_destpos, 1, 1, 1);
        if(dir.getX() < 0)
            destpos.setX(0);
        if(dir.getY() < 0)
            destpos.setY(0);
        if(dir.getZ() < 0)
            destpos.setZ(0);
        Vector vt = rayIntersectsBlock_vt;
        vt.setX((destpos.getX() - orig.getX()) * invdir.getX());
        vt.setY((destpos.getY() - orig.getY()) * invdir.getY());
        vt.setZ((destpos.getZ() - orig.getZ()) * invdir.getZ());
        Vector hx, hy, hz;
        hx = Vector.mul(rayIntersectsBlock_hx, dir, vt.getX()).addAndSet(orig);
        hy = Vector.mul(rayIntersectsBlock_hy, dir, vt.getY()).addAndSet(orig);
        hz = Vector.mul(rayIntersectsBlock_hz, dir, vt.getZ()).addAndSet(orig);
        if(hx.getX() >= -eps && hx.getX() <= 1 + eps && hx.getY() >= -eps
                && hx.getY() <= 1 + eps && hx.getZ() >= -eps
                && hx.getZ() <= 1 + eps)
            return vt.getX();
        if(hy.getX() >= -eps && hy.getX() <= 1 + eps && hy.getY() >= -eps
                && hy.getY() <= 1 + eps && hy.getZ() >= -eps
                && hy.getZ() <= 1 + eps)
            return vt.getY();
        if(hz.getX() >= -eps && hz.getX() <= 1 + eps && hz.getY() >= -eps
                && hz.getY() <= 1 + eps && hz.getZ() >= -eps
                && hz.getZ() <= 1 + eps)
            return vt.getZ();
        return -1;
    }

    private static Vector getRayEnterSide_dir = Vector.allocate();
    private static Vector getRayEnterSide_orig = Vector.allocate();
    private static Vector getRayEnterSide_invdir = Vector.allocate();
    private static Vector getRayEnterSide_destpos = Vector.allocate();
    private static Vector getRayEnterSide_vt = Vector.allocate();
    private static Vector getRayEnterSide_hx = Vector.allocate();
    private static Vector getRayEnterSide_hy = Vector.allocate();
    private static Vector getRayEnterSide_hz = Vector.allocate();

    private int getRayEnterSide(final Vector origin, final Vector direction)
    {
        Vector dir = Vector.set(getRayEnterSide_dir, direction);
        Vector orig = Vector.set(getRayEnterSide_orig, origin);
        final float eps = 1e-4f;
        if(Math.abs(dir.getX()) < eps)
            dir.setX(eps);
        if(Math.abs(dir.getY()) < eps)
            dir.setY(eps);
        if(Math.abs(dir.getZ()) < eps)
            dir.setZ(eps);
        Vector invdir = Vector.div(getRayEnterSide_invdir, 1, 1, 1, dir);
        Vector destpos = Vector.set(getRayEnterSide_destpos, 0, 0, 0);
        if(dir.getX() < 0)
            destpos.setX(1);
        if(dir.getY() < 0)
            destpos.setY(1);
        if(dir.getZ() < 0)
            destpos.setZ(1);
        Vector vt = getRayEnterSide_vt;
        vt.setX((destpos.getX() - orig.getX()) * invdir.getX());
        vt.setY((destpos.getY() - orig.getY()) * invdir.getY());
        vt.setZ((destpos.getZ() - orig.getZ()) * invdir.getZ());
        Vector hx, hy, hz;
        hx = Vector.mul(getRayEnterSide_hx, dir, vt.getX()).addAndSet(orig);
        hy = Vector.mul(getRayEnterSide_hy, dir, vt.getY()).addAndSet(orig);
        hz = Vector.mul(getRayEnterSide_hz, dir, vt.getZ()).addAndSet(orig);
        if(hx.getX() >= -eps && hx.getX() <= 1 + eps && hx.getY() >= -eps
                && hx.getY() <= 1 + eps && hx.getZ() >= -eps
                && hx.getZ() <= 1 + eps && vt.getX() >= -eps)
        {
            if(dir.getX() < 0)
                return 2;
            return 0;
        }
        if(hy.getX() >= -eps && hy.getX() <= 1 + eps && hy.getY() >= -eps
                && hy.getY() <= 1 + eps && hy.getZ() >= -eps
                && hy.getZ() <= 1 + eps && vt.getY() >= -eps)
        {
            if(dir.getY() < 0)
                return 5;
            return 4;
        }
        if(hz.getX() >= -eps && hz.getX() <= 1 + eps && hz.getY() >= -eps
                && hz.getY() <= 1 + eps && hz.getZ() >= -eps
                && hz.getZ() <= 1 + eps && vt.getZ() >= -eps)
        {
            if(dir.getZ() < 0)
                return 3;
            return 1;
        }
        return -1;
    }

    private int getRayExitSide(final Vector origin, final Vector direction)
    {
        Vector dir = Vector.set(getRayEnterSide_dir, direction);
        Vector orig = Vector.set(getRayEnterSide_orig, origin);
        final float eps = 1e-4f;
        if(Math.abs(dir.getX()) < eps)
            dir.setX(eps);
        if(Math.abs(dir.getY()) < eps)
            dir.setY(eps);
        if(Math.abs(dir.getZ()) < eps)
            dir.setZ(eps);
        Vector invdir = Vector.div(getRayEnterSide_invdir, 1, 1, 1, dir);
        Vector destpos = Vector.set(getRayEnterSide_destpos, 1, 1, 1);
        if(dir.getX() < 0)
            destpos.setX(0);
        if(dir.getY() < 0)
            destpos.setY(0);
        if(dir.getZ() < 0)
            destpos.setZ(0);
        Vector vt = getRayEnterSide_vt;
        vt.setX((destpos.getX() - orig.getX()) * invdir.getX());
        vt.setY((destpos.getY() - orig.getY()) * invdir.getY());
        vt.setZ((destpos.getZ() - orig.getZ()) * invdir.getZ());
        Vector hx, hy, hz;
        hx = Vector.mul(getRayEnterSide_hx, dir, vt.getX()).addAndSet(orig);
        hy = Vector.mul(getRayEnterSide_hy, dir, vt.getY()).addAndSet(orig);
        hz = Vector.mul(getRayEnterSide_hz, dir, vt.getZ()).addAndSet(orig);
        if(hx.getX() >= -eps && hx.getX() <= 1 + eps && hx.getY() >= -eps
                && hx.getY() <= 1 + eps && hx.getZ() >= -eps
                && hx.getZ() <= 1 + eps)
        {
            if(dir.getX() < 0)
                return 0;
            return 2;
        }
        if(hy.getX() >= -eps && hy.getX() <= 1 + eps && hy.getY() >= -eps
                && hy.getY() <= 1 + eps && hy.getZ() >= -eps
                && hy.getZ() <= 1 + eps)
        {
            if(dir.getY() < 0)
                return 4;
            return 5;
        }
        if(hz.getX() >= -eps && hz.getX() <= 1 + eps && hz.getY() >= -eps
                && hz.getY() <= 1 + eps && hz.getZ() >= -eps
                && hz.getZ() <= 1 + eps)
        {
            if(dir.getZ() < 0)
                return 1;
            return 3;
        }
        return -1;
    }

    private float getRayExitDist(final Vector origin, final Vector direction)
    {
        Vector dir = Vector.set(getRayEnterSide_dir, direction);
        Vector orig = Vector.set(getRayEnterSide_orig, origin);
        final float eps = 1e-4f;
        if(Math.abs(dir.getX()) < eps)
            dir.setX(eps);
        if(Math.abs(dir.getY()) < eps)
            dir.setY(eps);
        if(Math.abs(dir.getZ()) < eps)
            dir.setZ(eps);
        Vector invdir = Vector.div(getRayEnterSide_invdir, 1, 1, 1, dir);
        Vector destpos = Vector.set(getRayEnterSide_destpos, 1, 1, 1);
        if(dir.getX() < 0)
            destpos.setX(0);
        if(dir.getY() < 0)
            destpos.setY(0);
        if(dir.getZ() < 0)
            destpos.setZ(0);
        Vector vt = getRayEnterSide_vt;
        vt.setX((destpos.getX() - orig.getX()) * invdir.getX());
        vt.setY((destpos.getY() - orig.getY()) * invdir.getY());
        vt.setZ((destpos.getZ() - orig.getZ()) * invdir.getZ());
        Vector hx, hy, hz;
        hx = Vector.mul(getRayEnterSide_hx, dir, vt.getX()).addAndSet(orig);
        hy = Vector.mul(getRayEnterSide_hy, dir, vt.getY()).addAndSet(orig);
        hz = Vector.mul(getRayEnterSide_hz, dir, vt.getZ()).addAndSet(orig);
        if(hx.getX() >= -eps && hx.getX() <= 1 + eps && hx.getY() >= -eps
                && hx.getY() <= 1 + eps && hx.getZ() >= -eps
                && hx.getZ() <= 1 + eps)
        {
            return vt.getX();
        }
        if(hy.getX() >= -eps && hy.getX() <= 1 + eps && hy.getY() >= -eps
                && hy.getY() <= 1 + eps && hy.getZ() >= -eps
                && hy.getZ() <= 1 + eps)
        {
            return vt.getY();
        }
        if(hz.getX() >= -eps && hz.getX() <= 1 + eps && hz.getY() >= -eps
                && hz.getY() <= 1 + eps && hz.getZ() >= -eps
                && hz.getZ() <= 1 + eps)
        {
            return vt.getZ();
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
        case BTRedstoneComparator:
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
        case BTSnow:
            return this.data.intdata / 8.0f;
        case BTFarmland:
            return 15 / 16f;
        case BTSeeds:
            return (this.data.intdata + 1) / 8.0f;
        case BTVines:
        case BTWoodAxe:
        case BTStoneAxe:
        case BTIronAxe:
        case BTGoldAxe:
        case BTDiamondAxe:
        case BTShears:
        case BTBucket:
        case BTQuartz:
        case BTDispenser:
        case BTDropper:
        case BTCobweb:
        case BTString:
        case BTBow:
        case BTHopper:
        case BTCactus:
        case BTRedMushroom:
        case BTBrownMushroom:
        case BTDeadBush:
        case BTWoodHoe:
        case BTStoneHoe:
        case BTIronHoe:
        case BTGoldHoe:
        case BTDiamondHoe:
        case BTDandelion:
        case BTRose:
        case BTTallGrass:
        case BTWheat:
        case BTCocoa:
        case BTInkSac:
        case BTRoseRed:
        case BTCactusGreen:
        case BTPurpleDye:
        case BTCyanDye:
        case BTLightGrayDye:
        case BTGrayDye:
        case BTPinkDye:
        case BTLimeDye:
        case BTDandelionYellow:
        case BTLightBlueDye:
        case BTMagentaDye:
        case BTOrangeDye:
        case BTBoneMeal:
        case BTBone:
        case BTWool:
            return 1;
        }
        return 0;
    }

    private static Vector rayIntersects_t1 = Vector.allocate();
    private static Vector rayIntersects_t2 = Vector.allocate();
    private static Matrix rayIntersects_t3 = new Matrix();
    private static Matrix rayIntersects_t4 = new Matrix();

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
    public float rayIntersects(final Vector dir,
                               final Vector invdir,
                               final Vector pos,
                               final Vector hitpos)
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
        case BTSnow:
        case BTFarmland:
        case BTSeeds:
        {
            float height = getHeight();
            if(hitpos.getY() <= height)
                return 0;
            if(dir.getY() > 1)
                return -1;
            float t = (height - pos.getY()) * invdir.getY();
            Vector p = Vector.add(rayIntersects_t1,
                                  pos,
                                  Vector.mul(rayIntersects_t2, dir, t));
            if(p.getX() < 0 || p.getX() > 1 || p.getZ() < 0 || p.getZ() > 1)
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
        case BTVines:
            if(getRayExitSide(hitpos, dir) == this.data.orientation)
                return getRayExitDist(hitpos, dir);
            if(getRayEnterSide(hitpos, dir) == this.data.orientation)
                return 0;
            return -1;
        case BTRedstoneRepeaterOff:
        case BTRedstoneRepeaterOn:
        case BTRedstoneComparator:
            return rayIntersectsBlock(hitpos,
                                      dir,
                                      Matrix.setToScale(rayIntersects_t3,
                                                        1.0f,
                                                        1.0f / 8,
                                                        1.0f));
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
        case BTWoodAxe:
        case BTStoneAxe:
        case BTIronAxe:
        case BTGoldAxe:
        case BTDiamondAxe:
        case BTShears:
        case BTBucket:
        case BTQuartz:
        case BTDispenser:
        case BTDropper:
        case BTCobweb:
        case BTString:
        case BTBow:
        case BTHopper:
        case BTCactus:
        case BTDeadBush:
        case BTWoodHoe:
        case BTStoneHoe:
        case BTIronHoe:
        case BTGoldHoe:
        case BTDiamondHoe:
        case BTWheat:
        case BTTallGrass:
        case BTDandelion:
        case BTRose:
        case BTInkSac:
        case BTRoseRed:
        case BTCactusGreen:
        case BTPurpleDye:
        case BTCyanDye:
        case BTLightGrayDye:
        case BTGrayDye:
        case BTPinkDye:
        case BTLimeDye:
        case BTDandelionYellow:
        case BTLightBlueDye:
        case BTMagentaDye:
        case BTOrangeDye:
        case BTBoneMeal:
        case BTBone:
        case BTWool:
            return 0;
        case BTCocoa:
        {
            final float xzSize = (4 + 2 * this.data.intdata) / 16f, ySize = (5 + 2 * this.data.intdata) / 16f;
            final float minX = 0.5f - xzSize / 2f;
            final float maxX = 0.5f + xzSize / 2f;
            final float minZ = 1 / 16f;
            final float maxZ = 1 / 16f + xzSize;
            final float minY = 12 / 16f - ySize;
            final float maxY = 12 / 16f;
            return rayIntersectsBlock(hitpos,
                                      dir,
                                      Matrix.setToScale(rayIntersects_t3,
                                                        maxX - minX,
                                                        maxY - minY,
                                                        maxZ - minZ)
                                            .concatAndSet(Matrix.setToTranslate(rayIntersects_t4,
                                                                                minX,
                                                                                minY,
                                                                                minZ))
                                            .concatAndSet(Matrix.setToTranslate(rayIntersects_t4,
                                                                                -0.5f,
                                                                                -0.5f,
                                                                                -0.5f))
                                            .concatAndSet(Matrix.setToRotateY(rayIntersects_t4,
                                                                              Math.PI
                                                                                      / 2.0
                                                                                      * (1 - this.data.orientation)))
                                            .concatAndSet(Matrix.setToTranslate(rayIntersects_t4,
                                                                                0.5f,
                                                                                0.5f,
                                                                                0.5f)));
        }
        case BTRedMushroom:
        case BTBrownMushroom:
            return rayIntersectsBlock(hitpos,
                                      dir,
                                      Matrix.setToScale(rayIntersects_t3, 0.25f)
                                            .concatAndSet(Matrix.setToTranslate(rayIntersects_t4,
                                                                                3 / 8f,
                                                                                0,
                                                                                3 / 8f)));
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
    public static Block make(final BlockType bt, final int orientation)
    {
        return bt.make(orientation);
    }

    /** makes a new block from <code>bt</code>
     * 
     * @param bt
     *            the type of the new block
     * @return the new block or null if <code>bt == BlockType.BTEmpty</code> */
    public static Block make(final BlockType bt)
    {
        return make(bt, -1);
    }

    private static final Matrix drawImgAsEntity_mat = Matrix.setToTranslate(new Matrix(),
                                                                            0,
                                                                            0,
                                                                            0.5f)
                                                            .getImmutable();

    /** @param rs
     *            the rendering stream
     * @param blockToWorld
     *            matrix to transform block coordinates to world coordinates
     * @param img
     *            image to draw
     * @return <code>rs</code> */
    public static RenderingStream drawImgAsEntity(final RenderingStream rs,
                                                  final Matrix blockToWorld,
                                                  final TextureHandle img)
    {
        return drawItem(rs,
                        drawImgAsEntity_mat,
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
    public static RenderingStream drawImgAsEntity(final RenderingStream rs,
                                                  final Matrix blockToWorld,
                                                  final TextureHandle img,
                                                  final float minu,
                                                  final float minv,
                                                  final float maxu,
                                                  final float maxv)
    {
        return drawItem(rs,
                        drawImgAsEntity_mat,
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

    private static Matrix drawAsEntity_t1 = new Matrix();
    private static Matrix drawAsEntity_t2 = new Matrix();
    private static Block drawAsEntity_redstoneDust = NewRedstoneDust(0, 0);
    private static Block drawAsEntity_redstoneTorch = NewRedstoneTorch(false, 4);
    private static Block drawAsEntity_torch = NewTorch(4);
    private static Block drawAsEntity_redstoneRepeater = NewRedstoneRepeater(false,
                                                                             0,
                                                                             1,
                                                                             0);
    private static Block drawAsEntity_redstoneComparator = NewRedstoneComparator(false,
                                                                                 0,
                                                                                 0);
    private static Block drawAsEntity_lever = NewLever(false, 4);
    private static Block drawAsEntity_vines = NewVines(1);

    /** draw this block as an entity
     * 
     * @param rs
     *            the rendering stream
     * @param blockToWorld
     *            matrix to transform block coordinates to world coordinates
     * @return <code>rs</code> */
    public RenderingStream drawAsEntity(final RenderingStream rs,
                                        final Matrix blockToWorld)
    {
        switch(this.type)
        {
        case BTDeleteBlock:
        case BTSun:
        case BTMoon:
        case BTLast:
            draw(rs, blockToWorld, true, false);
            return rs;
        case BTEmpty:
            return rs;
        case BTStone:
        case BTCobblestone:
        case BTGrass:
        case BTDirt:
        case BTCocoa:
        case BTSapling:
        case BTCobweb:
        case BTHopper:
        case BTRedMushroom:
        case BTBrownMushroom:
        case BTDeadBush:
        case BTWheat:
        case BTSeeds:
        case BTRose:
        case BTDandelion:
        case BTTallGrass:
        case BTBedrock:
        case BTSand:
        case BTGravel:
        case BTWood:
        case BTLeaves:
        case BTGlass:
        case BTChest:
        case BTWorkbench:
        case BTFurnace:
        case BTPlank:
            draw(rs, blockToWorld, true, false);
            return rs;
        case BTStick:
        case BTWoodPick:
        case BTStonePick:
        case BTWoodShovel:
        case BTStoneShovel:
        {
            drawImgAsEntity(rs, blockToWorld, this.type.textures[0]);
            return rs;
        }
        case BTWater:
        {
            drawImgAsEntity(rs, blockToWorld, this.type.textures[2]);
            return rs;
        }
        case BTLava:
        {
            drawImgAsEntity(rs, blockToWorld, this.type.textures[1]);
            return rs;
        }
        case BTRedstoneDustOff:
        case BTRedstoneDustOn:
        {
            Matrix tform = blockToWorld;
            tform = Matrix.setToTranslate(drawAsEntity_t1, 0.5f, 0.5f, 0.5f)
                          .concatAndSet(tform);
            tform = Matrix.setToRotateX(drawAsEntity_t2, Math.PI / 2)
                          .concatAndSet(tform);
            tform = Matrix.setToTranslate(drawAsEntity_t1, -0.5f, -0.5f, -0.5f)
                          .concatAndSet(tform);
            Block b = drawAsEntity_redstoneDust;
            b.draw(rs, tform, true, false);
            return rs;
        }
        case BTRedstoneOre:
        case BTRedstoneBlock:
        case BTFarmland:
            draw(rs, blockToWorld, true, false);
            return rs;
        case BTRedstoneTorchOff:
        case BTRedstoneTorchOn:
        {
            Block b = drawAsEntity_redstoneTorch;
            b.draw(rs,
                   Matrix.setToTranslate(drawAsEntity_t1, -0.5f, 0, -0.5f)
                         .concatAndSet(Matrix.setToScale(drawAsEntity_t2, 2.0f))
                         .concatAndSet(Matrix.setToTranslate(drawAsEntity_t2,
                                                             0.5f,
                                                             0,
                                                             0.5f))
                         .concatAndSet(blockToWorld),
                   true,
                   false);
            return rs;
        }
        case BTStoneButton:
        case BTWoodButton:
        {
            Matrix tform = blockToWorld;
            tform = Matrix.setToTranslate(drawAsEntity_t1, 0.5f, 0.5f, 0.5f)
                          .concatAndSet(tform);
            tform = Matrix.setToRotateX(drawAsEntity_t2, Math.PI / 2)
                          .concatAndSet(tform);
            tform = Matrix.setToTranslate(drawAsEntity_t1, -0.5f, -0.5f, -0.5f)
                          .concatAndSet(tform);
            draw(rs, tform, true, false);
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
        case BTWool:
            draw(rs, blockToWorld, true, false);
            return rs;
        case BTTorch:
        {
            Block b = drawAsEntity_torch;
            b.draw(rs,
                   Matrix.setToTranslate(drawAsEntity_t1, -0.5f, 0, -0.5f)
                         .concatAndSet(Matrix.setToScale(drawAsEntity_t2, 2.0f))
                         .concatAndSet(Matrix.setToTranslate(drawAsEntity_t2,
                                                             0.5f,
                                                             0,
                                                             0.5f))
                         .concatAndSet(blockToWorld),
                   true,
                   false);
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
        case BTWoodAxe:
        case BTStoneAxe:
        case BTIronAxe:
        case BTGoldAxe:
        case BTDiamondAxe:
        case BTShears:
        case BTBucket:
        case BTWoodHoe:
        case BTStoneHoe:
        case BTIronHoe:
        case BTGoldHoe:
        case BTDiamondHoe:
        case BTInkSac:
        case BTRoseRed:
        case BTCactusGreen:
        case BTPurpleDye:
        case BTCyanDye:
        case BTLightGrayDye:
        case BTGrayDye:
        case BTPinkDye:
        case BTLimeDye:
        case BTDandelionYellow:
        case BTLightBlueDye:
        case BTMagentaDye:
        case BTOrangeDye:
        case BTBoneMeal:
        case BTBone:
        {
            drawImgAsEntity(rs, blockToWorld, this.type.textures[0]);
            return rs;
        }
        case BTVines:
        {
            drawAsEntity_vines.draw(rs,
                                    Matrix.setToTranslate(drawAsEntity_t1,
                                                          0,
                                                          0,
                                                          0.5f)
                                          .concatAndSet(blockToWorld),
                                    true,
                                    false);
            return rs;
        }
        case BTRedstoneRepeaterOff:
        case BTRedstoneRepeaterOn:
        {
            Block b = drawAsEntity_redstoneRepeater;
            b.draw(rs, Matrix.setToTranslate(drawAsEntity_t1, 0, 0.5f, 0)
                             .concatAndSet(blockToWorld), true, false);
            return rs;
        }
        case BTRedstoneComparator:
        {
            Block b = drawAsEntity_redstoneComparator;
            b.draw(rs, Matrix.setToTranslate(drawAsEntity_t1, 0, 0.5f, 0)
                             .concatAndSet(blockToWorld), true, false);
            return rs;
        }
        case BTStonePressurePlate:
        case BTWoodPressurePlate:
        {
            draw(rs, Matrix.setToTranslate(drawAsEntity_t1, 0, 0.5f, 0)
                           .concatAndSet(blockToWorld), true, false);
            return rs;
        }
        case BTSnow:
            drawImgAsEntity(rs, blockToWorld, this.type.textures[1]);
            return rs;
        case BTLever:
        {
            Block b = drawAsEntity_lever;
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
        case BTDispenser:
        case BTDropper:
        case BTCactus:
            draw(rs, blockToWorld, true, false);
            return rs;
        case BTBlazeRod:
        case BTBlazePowder:
        case BTQuartz:
        case BTString:
        case BTBow:
            drawImgAsEntity(rs,
                            blockToWorld,
                            this.type.textures[this.data.intdata]);
            return rs;
        }
        return rs;
    }

    private static final float SQRT_3 = (float)Math.sqrt(3);
    private static Matrix drawBlockAsItem_t1 = new Matrix();
    private static Matrix drawBlockAsItem_t2 = new Matrix();

    private RenderingStream drawBlockAsItem(final RenderingStream rs,
                                            final Matrix blockToWorld)
    {
        Matrix tform = Matrix.setToTranslate(drawBlockAsItem_t1,
                                             -0.5f,
                                             -0.5f,
                                             -0.5f)
                             .concatAndSet(Matrix.setToRotateY(drawBlockAsItem_t2,
                                                               -Math.PI / 4))
                             .concatAndSet(Matrix.setToRotateX(drawBlockAsItem_t2,
                                                               Math.PI / 6))
                             .concatAndSet(Matrix.setToScale(drawBlockAsItem_t2,
                                                             0.8f / SQRT_3,
                                                             0.8f / SQRT_3,
                                                             0.1f / SQRT_3))
                             .concatAndSet(Matrix.setToTranslate(drawBlockAsItem_t2,
                                                                 0.5f,
                                                                 0.5f,
                                                                 0.1f))
                             .concatAndSet(blockToWorld);
        draw(rs, tform, false, true);
        return rs;
    }

    private static Matrix drawAsItem_t1 = new Matrix();
    private static Matrix drawAsItem_t2 = new Matrix();
    private static Block drawAsItem_lever = NewLever(false, 1);
    private static Block drawAsItem_redstoneTorch = NewRedstoneTorch(false, 1);
    private static Block drawAsItem_torch = NewTorch(1);
    private static Block drawAsItem_vines = NewVines(1);

    /** draws as an item
     * 
     * @param rs
     *            the rendering stream
     * @param blockToWorld
     *            the matrix that transforms block coordinates to world
     *            coordinates
     * @return <code>rs</code> */
    public RenderingStream drawAsItem(final RenderingStream rs,
                                      final Matrix blockToWorld)
    {
        switch(this.type)
        {
        case BTDeleteBlock:
        case BTSun:
        case BTMoon:
            drawBlockAsItem(rs, blockToWorld);
            return rs;
        case BTLast:
            return rs;
        case BTEmpty:
            return rs;
        case BTStone:
        case BTCobblestone:
        case BTGrass:
        case BTDirt:
        case BTBedrock:
        case BTSand:
        case BTGravel:
        case BTWood:
        case BTLeaves:
        case BTGlass:
        case BTChest:
        case BTWorkbench:
        case BTFurnace:
        case BTRedstoneOre:
        case BTRedstoneBlock:
        case BTCoalOre:
        case BTIronOre:
        case BTLapisLazuliOre:
        case BTGoldOre:
        case BTDiamondOre:
        case BTEmeraldOre:
        case BTObsidian:
        case BTPiston:
        case BTStickyPiston:
        case BTPistonHead:
        case BTStickyPistonHead:
        case BTTNT:
        case BTPlank:
        case BTDispenser:
        case BTDropper:
        case BTCactus:
        case BTFarmland:
        case BTWool:
            drawBlockAsItem(rs, blockToWorld);
            return rs;
        case BTSapling:
        case BTCobweb:
        case BTRedMushroom:
        case BTBrownMushroom:
        case BTDeadBush:
        case BTRose:
        case BTDandelion:
        case BTTallGrass:
            draw(rs, Matrix.setToTranslate(drawAsItem_t1, 0, 0, -0.5f)
                           .concatAndSet(blockToWorld), false, true);
            return rs;
        case BTStick:
        case BTWoodPick:
        case BTStonePick:
        case BTWoodShovel:
        case BTStoneShovel:
        case BTShears:
        case BTBucket:
        case BTHopper:
        case BTCocoa:
        case BTInkSac:
        case BTRoseRed:
        case BTCactusGreen:
        case BTPurpleDye:
        case BTCyanDye:
        case BTLightGrayDye:
        case BTGrayDye:
        case BTPinkDye:
        case BTLimeDye:
        case BTDandelionYellow:
        case BTLightBlueDye:
        case BTMagentaDye:
        case BTOrangeDye:
        case BTBoneMeal:
        case BTBone:
        {
            draw(rs, blockToWorld, false, true);
            return rs;
        }
        case BTRedstoneDustOff:
        case BTRedstoneDustOn:
        {
            Matrix tform = blockToWorld;
            tform = Matrix.setToTranslate(drawAsItem_t1, 0.5f, 0.5f, 0.5f)
                          .concatAndSet(tform);
            tform = Matrix.setToRotateX(drawAsItem_t2, Math.PI / 2)
                          .concatAndSet(tform);
            tform = Matrix.setToTranslate(drawAsItem_t1, -0.5f, -0.5f, -0.5f)
                          .concatAndSet(tform);
            Block b = drawAsEntity_redstoneDust;
            b.draw(rs, tform, false, true);
            return rs;
        }
        case BTRedstoneTorchOff:
        case BTRedstoneTorchOn:
        {
            Block b = drawAsItem_redstoneTorch;
            b.draw(rs, blockToWorld, false, true);
            return rs;
        }
        case BTStoneButton:
        case BTWoodButton:
        {
            Matrix tform = blockToWorld;
            tform = Matrix.setToTranslate(drawAsItem_t1, 0.5f, 0.5f, 0.5f)
                          .concatAndSet(tform);
            tform = Matrix.setToRotateX(drawAsItem_t2, Math.PI / 2)
                          .concatAndSet(tform);
            tform = Matrix.setToTranslate(drawAsItem_t1, -0.5f, -0.5f, -0.5f)
                          .concatAndSet(tform);
            draw(rs, tform, false, true);
            return rs;
        }
        case BTCoal:
        case BTIronIngot:
        case BTLapisLazuli:
        case BTGoldIngot:
        case BTDiamond:
        case BTEmerald:
        {
            draw(rs, blockToWorld, false, true);
            return rs;
        }
        case BTTorch:
        {
            Block b = drawAsItem_torch;
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
            draw(rs, blockToWorld, false, true);
            return rs;
        }
        case BTVines:
            drawAsItem_vines.draw(rs, blockToWorld, false, true);
            return rs;
        case BTLadder:
        {
            drawItem(rs,
                     Matrix.IDENTITY,
                     blockToWorld,
                     0,
                     0,
                     0,
                     this.type.textures[0],
                     false,
                     true);
            return rs;
        }
        case BTRedstoneRepeaterOff:
        case BTRedstoneRepeaterOn:
        {
            Block b = drawAsEntity_redstoneRepeater;
            Matrix tform = blockToWorld;
            tform = Matrix.setToTranslate(drawAsItem_t1, 0.5f, 0.5f, 0.5f)
                          .concatAndSet(tform);
            tform = Matrix.setToRotateX(drawAsItem_t2, Math.PI / 2)
                          .concatAndSet(tform);
            tform = Matrix.setToTranslate(drawAsItem_t1, -0.5f, -0.5f, -0.5f)
                          .concatAndSet(tform);
            b.draw(rs, tform, false, true);
            return rs;
        }
        case BTRedstoneComparator:
        {
            Block b = drawAsEntity_redstoneComparator;
            Matrix tform = blockToWorld;
            tform = Matrix.setToTranslate(drawAsItem_t1, 0.5f, 0.5f, 0.5f)
                          .concatAndSet(tform);
            tform = Matrix.setToRotateX(drawAsItem_t2, Math.PI / 2)
                          .concatAndSet(tform);
            tform = Matrix.setToTranslate(drawAsItem_t1, -0.5f, -0.5f, -0.5f)
                          .concatAndSet(tform);
            b.draw(rs, tform, false, true);
            return rs;
        }
        case BTStonePressurePlate:
        case BTWoodPressurePlate:
        {
            drawBlockAsItem(rs, blockToWorld);
            return rs;
        }
        case BTLever:
        {
            Block b = drawAsItem_lever;
            b.draw(rs, blockToWorld, false, true);
            return rs;
        }
        case BTSlime:
        case BTGunpowder:
        case BTBlazeRod:
        case BTBlazePowder:
        case BTWoodAxe:
        case BTStoneAxe:
        case BTIronAxe:
        case BTGoldAxe:
        case BTDiamondAxe:
        case BTQuartz:
        case BTString:
        case BTBow:
        case BTWoodHoe:
        case BTStoneHoe:
        case BTIronHoe:
        case BTGoldHoe:
        case BTDiamondHoe:
        case BTSeeds:
        case BTWheat:
        {
            draw(rs, blockToWorld, false, true);
            return rs;
        }
        case BTSnow:
        {
            drawItem(rs,
                     Matrix.IDENTITY,
                     blockToWorld,
                     0,
                     0,
                     0,
                     this.type.textures[1],
                     false,
                     true);
            return rs;
        }
        case BTLava:
        {
            drawItem(rs,
                     Matrix.IDENTITY,
                     blockToWorld,
                     0,
                     0,
                     0,
                     this.type.textures[1],
                     false,
                     true);
            return rs;
        }
        case BTWater:
        {
            drawItem(rs,
                     Matrix.IDENTITY,
                     blockToWorld,
                     0,
                     0,
                     0,
                     this.type.textures[2],
                     false,
                     true);
            return rs;
        }
        }
        return rs;
    }

    /** a reduction returned by <code>Block.reduce</code>
     * 
     * @author jacob
     * @see Block#reduce(Block[] array, int arraySize) */
    public static final class ReduceDescriptor
    {
        /** the block */
        public final Block b;
        /** the blocks count */
        public final int count;

        /** @param b
         *            the new block
         * @param count
         *            the number of new blocks */
        public ReduceDescriptor(final Block b, final int count)
        {
            this.b = b;
            this.count = count;
        }

        /** creates an empty <code>ReduceDescriptor</code> */
        public ReduceDescriptor()
        {
            this(null, 0);
        }

        /** @return true if this reduction failed */
        public boolean isEmpty()
        {
            if(this.b == null)
                return true;
            return false;
        }
    }

    private static abstract class BlockDescriptor
    {
        public abstract boolean matches(Block b);

        public abstract boolean isEmpty();

        public BlockDescriptor()
        {
        }
    }

    private static class BlockDescriptorBlockType extends BlockDescriptor
    {
        private final BlockType bt;

        @Override
        public boolean matches(final Block b)
        {
            if(b == null)
                return isEmpty();
            return b.getType() == this.bt;
        }

        public BlockDescriptorBlockType(final BlockType bt)
        {
            this.bt = bt;
        }

        @Override
        public boolean isEmpty()
        {
            return this.bt == BlockType.BTEmpty;
        }
    }

    private static class BlockDescriptorTreeType extends
        BlockDescriptorBlockType
    {
        private final Tree.TreeType tt;

        @Override
        public boolean matches(final Block b)
        {
            if(b == null)
                return isEmpty();
            if(!super.matches(b))
                return false;
            return b.treeGetTreeType() == this.tt;
        }

        public BlockDescriptorTreeType(final BlockType bt,
                                       final Tree.TreeType tt)
        {
            super(bt);
            this.tt = tt;
        }
    }

    private static class BlockDescriptorDyeColor extends
        BlockDescriptorBlockType
    {
        private final DyeColor color;

        @Override
        public boolean matches(final Block b)
        {
            if(b == null)
                return isEmpty();
            if(!super.matches(b))
                return false;
            return b.dyedGetDyeColor() == this.color;
        }

        public BlockDescriptorDyeColor(final BlockType bt, final DyeColor color)
        {
            super(bt);
            this.color = color;
        }
    }

    private static class ReduceStruct
    {
        public final BlockDescriptor array[];
        public final int size;
        public final ReduceDescriptor retval;

        public ReduceStruct(final BlockDescriptor array[],
                            final int size,
                            final Block b,
                            final int count)
        {
            this.array = array;
            this.size = size;
            this.retval = new ReduceDescriptor(b, count);
        }

        public boolean isShapeless()
        {
            return false;
        }
    }

    private static class ShapelessReduceStruct extends ReduceStruct
    {
        public ShapelessReduceStruct(final BlockDescriptor array[],
                                     final int size,
                                     final Block b,
                                     final int count)
        {
            super(array, size, b, count);
        }

        @Override
        public boolean isShapeless()
        {
            return true;
        }
    }

    private static final ReduceStruct reduceArray[] = new ReduceStruct[]
    {
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTPlank),
            new BlockDescriptorBlockType(BlockType.BTPlank),
            new BlockDescriptorBlockType(BlockType.BTPlank),
            new BlockDescriptorBlockType(BlockType.BTPlank),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTPlank),
            new BlockDescriptorBlockType(BlockType.BTPlank),
            new BlockDescriptorBlockType(BlockType.BTPlank),
            new BlockDescriptorBlockType(BlockType.BTPlank)
        }, 3, NewChest(), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTCobblestone),
            new BlockDescriptorBlockType(BlockType.BTCobblestone),
            new BlockDescriptorBlockType(BlockType.BTCobblestone),
            new BlockDescriptorBlockType(BlockType.BTCobblestone),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTCobblestone),
            new BlockDescriptorBlockType(BlockType.BTCobblestone),
            new BlockDescriptorBlockType(BlockType.BTCobblestone),
            new BlockDescriptorBlockType(BlockType.BTCobblestone)
        }, 3, NewFurnace(), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTPlank),
            new BlockDescriptorBlockType(BlockType.BTPlank),
            new BlockDescriptorBlockType(BlockType.BTPlank),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty)
        }, 3, NewWoodPick(), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTCobblestone),
            new BlockDescriptorBlockType(BlockType.BTCobblestone),
            new BlockDescriptorBlockType(BlockType.BTCobblestone),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty)
        }, 3, NewStonePick(), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTPlank),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty)
        }, 3, NewWoodShovel(), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTCobblestone),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty)
        }, 3, NewStoneShovel(), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTPlank),
            new BlockDescriptorBlockType(BlockType.BTPlank),
            new BlockDescriptorBlockType(BlockType.BTPlank),
            new BlockDescriptorBlockType(BlockType.BTPlank)
        }, 2, NewWorkbench(), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorTreeType(BlockType.BTWood, TreeType.Oak)
        }, 1, NewPlank(TreeType.Oak), 4),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorTreeType(BlockType.BTWood, TreeType.Spruce)
        }, 1, NewPlank(TreeType.Spruce), 4),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorTreeType(BlockType.BTWood, TreeType.Birch)
        }, 1, NewPlank(TreeType.Birch), 4),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorTreeType(BlockType.BTWood, TreeType.Jungle)
        }, 1, NewPlank(TreeType.Jungle), 4),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTPlank),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTPlank),
            new BlockDescriptorBlockType(BlockType.BTEmpty)
        }, 2, NewStick(), 4),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTRedstoneBlock)
        }, 1, NewRedstoneDust(0, 0), 9),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTCobblestone)
        }, 1, NewStoneButton(0, 0), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTPlank)
        }, 1, NewWoodButton(0, 0), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTRedstoneDustOff),
            new BlockDescriptorBlockType(BlockType.BTRedstoneDustOff),
            new BlockDescriptorBlockType(BlockType.BTRedstoneDustOff),
            new BlockDescriptorBlockType(BlockType.BTRedstoneDustOff),
            new BlockDescriptorBlockType(BlockType.BTRedstoneDustOff),
            new BlockDescriptorBlockType(BlockType.BTRedstoneDustOff),
            new BlockDescriptorBlockType(BlockType.BTRedstoneDustOff),
            new BlockDescriptorBlockType(BlockType.BTRedstoneDustOff),
            new BlockDescriptorBlockType(BlockType.BTRedstoneDustOff)
        }, 3, NewRedstoneBlock(), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTRedstoneDustOff),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty)
        }, 2, NewRedstoneTorch(false, -1), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTCoal),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty)
        }, 2, NewTorch(-1), 4),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTIronIngot),
            new BlockDescriptorBlockType(BlockType.BTIronIngot),
            new BlockDescriptorBlockType(BlockType.BTIronIngot),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty)
        }, 3, NewIronPick(), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTIronIngot),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty)
        }, 3, NewIronShovel(), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTGoldIngot),
            new BlockDescriptorBlockType(BlockType.BTGoldIngot),
            new BlockDescriptorBlockType(BlockType.BTGoldIngot),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty)
        }, 3, NewGoldPick(), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTGoldIngot),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty)
        }, 3, NewGoldShovel(), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTDiamond),
            new BlockDescriptorBlockType(BlockType.BTDiamond),
            new BlockDescriptorBlockType(BlockType.BTDiamond),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty)
        }, 3, NewDiamondPick(), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTDiamond),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty)
        }, 3, NewDiamondShovel(), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick)
        }, 3, NewLadder(-1), 3),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTRedstoneTorchOff),
            new BlockDescriptorBlockType(BlockType.BTRedstoneDustOff),
            new BlockDescriptorBlockType(BlockType.BTRedstoneTorchOff),
            new BlockDescriptorBlockType(BlockType.BTStone),
            new BlockDescriptorBlockType(BlockType.BTStone),
            new BlockDescriptorBlockType(BlockType.BTStone)
        }, 3, NewRedstoneRepeater(), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTPlank),
            new BlockDescriptorBlockType(BlockType.BTPlank),
            new BlockDescriptorBlockType(BlockType.BTPlank),
            new BlockDescriptorBlockType(BlockType.BTCobblestone),
            new BlockDescriptorBlockType(BlockType.BTIronIngot),
            new BlockDescriptorBlockType(BlockType.BTCobblestone),
            new BlockDescriptorBlockType(BlockType.BTCobblestone),
            new BlockDescriptorBlockType(BlockType.BTRedstoneDustOff),
            new BlockDescriptorBlockType(BlockType.BTCobblestone)
        }, 3, NewPiston(-1, false), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTCobblestone),
            new BlockDescriptorBlockType(BlockType.BTEmpty)
        }, 2, NewLever(false, -1), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTSlime),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTPiston),
            new BlockDescriptorBlockType(BlockType.BTEmpty)
        }, 2, NewStickyPiston(-1, false), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTGunpowder),
            new BlockDescriptorBlockType(BlockType.BTSand),
            new BlockDescriptorBlockType(BlockType.BTGunpowder),
            new BlockDescriptorBlockType(BlockType.BTSand),
            new BlockDescriptorBlockType(BlockType.BTGunpowder),
            new BlockDescriptorBlockType(BlockType.BTSand),
            new BlockDescriptorBlockType(BlockType.BTGunpowder),
            new BlockDescriptorBlockType(BlockType.BTSand),
            new BlockDescriptorBlockType(BlockType.BTGunpowder)
        }, 3, NewTNT(), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTBlazeRod)
        }, 1, NewBlazePowder(), 2),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStone),
            new BlockDescriptorBlockType(BlockType.BTStone)
        }, 2, NewStonePressurePlate(), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTPlank),
            new BlockDescriptorBlockType(BlockType.BTPlank)
        }, 2, NewWoodPressurePlate(), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTPlank),
            new BlockDescriptorBlockType(BlockType.BTPlank),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTPlank),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty)
        }, 3, NewWoodAxe(), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTCobblestone),
            new BlockDescriptorBlockType(BlockType.BTCobblestone),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTCobblestone),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty)
        }, 3, NewStoneAxe(), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTIronIngot),
            new BlockDescriptorBlockType(BlockType.BTIronIngot),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTIronIngot),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty)
        }, 3, NewIronAxe(), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTGoldIngot),
            new BlockDescriptorBlockType(BlockType.BTGoldIngot),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTGoldIngot),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty)
        }, 3, NewGoldAxe(), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTDiamond),
            new BlockDescriptorBlockType(BlockType.BTDiamond),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTDiamond),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty)
        }, 3, NewDiamondAxe(), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTPlank),
            new BlockDescriptorBlockType(BlockType.BTPlank),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty)
        }, 3, NewWoodHoe(), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTCobblestone),
            new BlockDescriptorBlockType(BlockType.BTCobblestone),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty)
        }, 3, NewStoneHoe(), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTIronIngot),
            new BlockDescriptorBlockType(BlockType.BTIronIngot),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty)
        }, 3, NewIronHoe(), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTGoldIngot),
            new BlockDescriptorBlockType(BlockType.BTGoldIngot),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty)
        }, 3, NewGoldHoe(), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTDiamond),
            new BlockDescriptorBlockType(BlockType.BTDiamond),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty)
        }, 3, NewDiamondHoe(), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTIronIngot),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTIronIngot),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTIronIngot),
            new BlockDescriptorBlockType(BlockType.BTEmpty)
        }, 3, NewBucket(), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTIronIngot),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTIronIngot)
        }, 2, NewShears(), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTIronIngot),
            new BlockDescriptorBlockType(BlockType.BTIronIngot),
            new BlockDescriptorBlockType(BlockType.BTEmpty)
        }, 2, NewShears(), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTRedstoneTorchOff),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTRedstoneTorchOff),
            new BlockDescriptorBlockType(BlockType.BTQuartz),
            new BlockDescriptorBlockType(BlockType.BTRedstoneTorchOff),
            new BlockDescriptorBlockType(BlockType.BTStone),
            new BlockDescriptorBlockType(BlockType.BTStone),
            new BlockDescriptorBlockType(BlockType.BTStone)
        }, 3, NewRedstoneComparator(false, 0, -1), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTString),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTString),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTStick),
            new BlockDescriptorBlockType(BlockType.BTString)
        }, 3, NewBow(), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTCobblestone),
            new BlockDescriptorBlockType(BlockType.BTCobblestone),
            new BlockDescriptorBlockType(BlockType.BTCobblestone),
            new BlockDescriptorBlockType(BlockType.BTCobblestone),
            new BlockDescriptorBlockType(BlockType.BTEmpty),
            new BlockDescriptorBlockType(BlockType.BTCobblestone),
            new BlockDescriptorBlockType(BlockType.BTCobblestone),
            new BlockDescriptorBlockType(BlockType.BTRedstoneDustOff),
            new BlockDescriptorBlockType(BlockType.BTCobblestone)
        }, 3, NewDropper(-1), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTCobblestone),
            new BlockDescriptorBlockType(BlockType.BTCobblestone),
            new BlockDescriptorBlockType(BlockType.BTCobblestone),
            new BlockDescriptorBlockType(BlockType.BTCobblestone),
            new BlockDescriptorBlockType(BlockType.BTBow),
            new BlockDescriptorBlockType(BlockType.BTCobblestone),
            new BlockDescriptorBlockType(BlockType.BTCobblestone),
            new BlockDescriptorBlockType(BlockType.BTRedstoneDustOff),
            new BlockDescriptorBlockType(BlockType.BTCobblestone)
        }, 3, NewDispenser(-1), 1),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTRose)
        }, 1, NewRoseRed(), 2),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTDandelion)
        }, 1, NewDandelionYellow(), 2),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTBone)
        }, 1, NewBoneMeal(), 2),
        new ShapelessReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTRoseRed),
            new BlockDescriptorBlockType(BlockType.BTDandelionYellow),
        }, 2, NewOrangeDye(), 2),
        new ShapelessReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTLapisLazuli),
            new BlockDescriptorBlockType(BlockType.BTCactusGreen),
        }, 2, NewCyanDye(), 2),
        new ShapelessReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTLapisLazuli),
            new BlockDescriptorBlockType(BlockType.BTRoseRed),
        }, 2, NewPurpleDye(), 2),
        new ShapelessReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTBoneMeal),
            new BlockDescriptorBlockType(BlockType.BTInkSac),
        }, 2, NewGrayDye(), 2),
        new ShapelessReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTBoneMeal),
            new BlockDescriptorBlockType(BlockType.BTLapisLazuli),
        }, 2, NewLightBlueDye(), 2),
        new ShapelessReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTBoneMeal),
            new BlockDescriptorBlockType(BlockType.BTRoseRed),
        }, 2, NewPinkDye(), 2),
        new ShapelessReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTBoneMeal),
            new BlockDescriptorBlockType(BlockType.BTCactusGreen),
        }, 2, NewLimeDye(), 2),
        new ShapelessReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTPurpleDye),
            new BlockDescriptorBlockType(BlockType.BTPinkDye),
        }, 2, NewMagentaDye(), 2),
        new ShapelessReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTLapisLazuli),
            new BlockDescriptorBlockType(BlockType.BTBoneMeal),
            new BlockDescriptorBlockType(BlockType.BTRoseRed),
            new BlockDescriptorBlockType(BlockType.BTRoseRed),
        }, 4, NewMagentaDye(), 4),
        new ShapelessReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTLapisLazuli),
            new BlockDescriptorBlockType(BlockType.BTPinkDye),
            new BlockDescriptorBlockType(BlockType.BTRoseRed),
        }, 3, NewMagentaDye(), 3),
        new ShapelessReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTBoneMeal),
            new BlockDescriptorBlockType(BlockType.BTBoneMeal),
            new BlockDescriptorBlockType(BlockType.BTInkSac),
        }, 3, NewLightGrayDye(), 3),
        new ShapelessReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTBoneMeal),
            new BlockDescriptorBlockType(BlockType.BTGrayDye),
        }, 2, NewLightGrayDye(), 2),
        new ReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTString),
            new BlockDescriptorBlockType(BlockType.BTString),
            new BlockDescriptorBlockType(BlockType.BTString),
            new BlockDescriptorBlockType(BlockType.BTString)
        }, 2, NewWool(DyeColor.BoneMeal), 1),
        new ShapelessReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTInkSac),
            new BlockDescriptorDyeColor(BlockType.BTWool, DyeColor.BoneMeal),
        }, 2, NewWool(DyeColor.InkSac), 2),
        new ShapelessReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTRoseRed),
            new BlockDescriptorDyeColor(BlockType.BTWool, DyeColor.BoneMeal),
        }, 2, NewWool(DyeColor.RoseRed), 2),
        new ShapelessReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTCactusGreen),
            new BlockDescriptorDyeColor(BlockType.BTWool, DyeColor.BoneMeal),
        }, 2, NewWool(DyeColor.CactusGreen), 2),
        new ShapelessReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTCocoa),
            new BlockDescriptorDyeColor(BlockType.BTWool, DyeColor.BoneMeal),
        }, 2, NewWool(DyeColor.CocoaBeans), 2),
        new ShapelessReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTLapisLazuli),
            new BlockDescriptorDyeColor(BlockType.BTWool, DyeColor.BoneMeal),
        }, 2, NewWool(DyeColor.LapisLazuli), 2),
        new ShapelessReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTPurpleDye),
            new BlockDescriptorDyeColor(BlockType.BTWool, DyeColor.BoneMeal),
        }, 2, NewWool(DyeColor.Purple), 2),
        new ShapelessReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTCyanDye),
            new BlockDescriptorDyeColor(BlockType.BTWool, DyeColor.BoneMeal),
        }, 2, NewWool(DyeColor.Cyan), 2),
        new ShapelessReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTLightGrayDye),
            new BlockDescriptorDyeColor(BlockType.BTWool, DyeColor.BoneMeal),
        }, 2, NewWool(DyeColor.LightGray), 2),
        new ShapelessReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTGrayDye),
            new BlockDescriptorDyeColor(BlockType.BTWool, DyeColor.BoneMeal),
        }, 2, NewWool(DyeColor.Gray), 2),
        new ShapelessReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTPinkDye),
            new BlockDescriptorDyeColor(BlockType.BTWool, DyeColor.BoneMeal),
        }, 2, NewWool(DyeColor.Pink), 2),
        new ShapelessReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTLimeDye),
            new BlockDescriptorDyeColor(BlockType.BTWool, DyeColor.BoneMeal),
        }, 2, NewWool(DyeColor.Lime), 2),
        new ShapelessReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTDandelionYellow),
            new BlockDescriptorDyeColor(BlockType.BTWool, DyeColor.BoneMeal),
        }, 2, NewWool(DyeColor.DandelionYellow), 2),
        new ShapelessReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTLightBlueDye),
            new BlockDescriptorDyeColor(BlockType.BTWool, DyeColor.BoneMeal),
        }, 2, NewWool(DyeColor.LightBlue), 2),
        new ShapelessReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTMagentaDye),
            new BlockDescriptorDyeColor(BlockType.BTWool, DyeColor.BoneMeal),
        }, 2, NewWool(DyeColor.Magenta), 2),
        new ShapelessReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTOrangeDye),
            new BlockDescriptorDyeColor(BlockType.BTWool, DyeColor.BoneMeal),
        }, 2, NewWool(DyeColor.Orange), 2),
        new ShapelessReduceStruct(new BlockDescriptor[]
        {
            new BlockDescriptorBlockType(BlockType.BTBoneMeal),
            new BlockDescriptorDyeColor(BlockType.BTWool, DyeColor.BoneMeal),
        }, 2, NewWool(DyeColor.BoneMeal), 2),
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
    public static ReduceDescriptor reduce(final Block[] array,
                                          final int arraySize)
    {
        int minx = arraySize, maxx = 0, miny = arraySize, maxy = 0;
        for(int x = 0; x < arraySize; x++)
        {
            for(int y = 0; y < arraySize; y++)
            {
                if(array[x + y * arraySize] != null)
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
        final boolean[] slotUsed = new boolean[arraySize * arraySize];
        for(int i = 0; i < reduceCount; i++)
        {
            boolean matches = true;
            if(reduceArray[i].isShapeless())
            {
                for(int j = 0; j < slotUsed.length; j++)
                    slotUsed[j] = (array[j] == null);
                for(int j = 0; j < reduceArray[i].size; j++)
                {
                    boolean slotMatches = false;
                    for(int x = minx; x <= maxx; x++)
                    {
                        for(int y = miny; y <= maxy; y++)
                        {
                            if(!slotUsed[x + y * arraySize]
                                    && reduceArray[i].array[j].matches(array[x
                                            + y * arraySize]))
                            {
                                slotMatches = true;
                                slotUsed[x + y * arraySize] = true;
                                break;
                            }
                        }
                        if(slotMatches)
                            break;
                    }
                    if(!slotMatches)
                    {
                        matches = false;
                        break;
                    }
                }
                if(matches == true)
                {
                    for(int j = 0; j < slotUsed.length; j++)
                    {
                        if(!slotUsed[j])
                        {
                            matches = false;
                            break;
                        }
                    }
                }
            }
            else
            {
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
                        BlockDescriptor bd = reduceArray[i].array[x
                                + (reduceArray[i].size - y - 1)
                                * reduceArray[i].size];
                        if(!bd.isEmpty())
                        {
                            if(x > maxx - minx || y > maxy - miny)
                            {
                                matches = false;
                                break;
                            }
                            if(!bd.matches(array[(x + minx) + arraySize
                                    * (y + miny)]))
                            {
                                matches = false;
                                break;
                            }
                        }
                        else
                        {
                            if(x <= maxx - minx && y <= maxy - miny)
                            {
                                if(array[(x + minx) + arraySize * (y + miny)] != null)
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
            }
            if(matches)
            {
                return reduceArray[i].retval;
            }
        }
        return new ReduceDescriptor();
    }

    private static int chestGetSlotIndex(final int row, final int column)
    {
        return row + CHEST_ROWS * column;
    }

    /** @param row
     *            the slot's row
     * @param column
     *            the slot's column
     * @return the number of blocks in this chest slot */
    public int chestGetBlockCount(final int row, final int column)
    {
        assert this.type == BlockType.BTChest && this.data.BlockCounts != null
                && this.data.BlockCounts.length == CHEST_ROWS * CHEST_COLUMNS
                && this.data.BlockTypes.length == CHEST_ROWS * CHEST_COLUMNS : "illegal block state";
        if(row < 0 || row >= CHEST_ROWS || column < 0
                || column >= CHEST_COLUMNS)
            return 0;
        return this.data.BlockCounts[chestGetSlotIndex(row, column)];
    }

    /** @param row
     *            the slot's row
     * @param column
     *            the slot's column
     * @return the kind of block in this chest slot */
    public Block chestGetBlockType(final int row, final int column)
    {
        assert this.type == BlockType.BTChest && this.data.BlockCounts != null
                && this.data.BlockCounts.length == CHEST_ROWS * CHEST_COLUMNS
                && this.data.BlockTypes.length == CHEST_ROWS * CHEST_COLUMNS : "illegal block state";
        if(row < 0 || row >= CHEST_ROWS || column < 0
                || column >= CHEST_COLUMNS)
            return null;
        return this.data.BlockTypes[chestGetSlotIndex(row, column)];
    }

    /**
     * 
     */
    public static final int BLOCK_STACK_SIZE = 64;

    /** add some blocks to this chest block<BR/>
     * not thread safe
     * 
     * @param b
     *            the type of blocks to add
     * @param count
     *            the number of blocks to add
     * @param row
     *            the slot's row
     * @param column
     *            the slot's column
     * @return the number of blocks successfully added */
    public int chestAddBlocks(final Block b,
                              final int count,
                              final int row,
                              final int column)
    {
        assert this.type == BlockType.BTChest && this.data.BlockCounts != null
                && this.data.BlockCounts.length == CHEST_ROWS * CHEST_COLUMNS
                && this.data.BlockTypes.length == CHEST_ROWS * CHEST_COLUMNS : "illegal block state";
        if(b == null || b.getType() == BlockType.BTEmpty)
            return count;
        if(row < 0 || row >= CHEST_ROWS || column < 0
                || column >= CHEST_COLUMNS || count <= 0)
            return 0;
        int index = chestGetSlotIndex(row, column);
        if(this.data.BlockCounts[index] >= BLOCK_STACK_SIZE)
            return 0;
        if(this.data.BlockCounts[index] <= 0)
        {
            this.data.BlockTypes[index] = b;
            if(count > BLOCK_STACK_SIZE)
            {
                this.data.BlockCounts[index] = BLOCK_STACK_SIZE;
                return BLOCK_STACK_SIZE;
            }
            this.data.BlockCounts[index] = count;
            return count;
        }
        if(!this.data.BlockTypes[index].equals(b))
            return 0;
        if(this.data.BlockCounts[index] + count > BLOCK_STACK_SIZE)
        {
            int retval = BLOCK_STACK_SIZE - this.data.BlockCounts[index];
            this.data.BlockCounts[index] = BLOCK_STACK_SIZE;
            return retval;
        }
        this.data.BlockCounts[index] += count;
        return count;
    }

    /** remove some blocks from this chest block <BR/>
     * not thread safe
     * 
     * @param b
     *            the type of blocks to remove
     * @param count
     *            the number of blocks to remove
     * @param row
     *            the slot's row
     * @param column
     *            the slot's column
     * @return the number of blocks removed */
    public int chestRemoveBlocks(final Block b,
                                 final int count,
                                 final int row,
                                 final int column)
    {
        assert this.type == BlockType.BTChest && this.data.BlockCounts != null
                && this.data.BlockCounts.length == CHEST_ROWS * CHEST_COLUMNS
                && this.data.BlockTypes.length == CHEST_ROWS * CHEST_COLUMNS : "illegal block state";
        if(b == null || b.getType() == BlockType.BTEmpty)
            return count;
        if(row < 0 || row >= CHEST_ROWS || column < 0
                || column >= CHEST_COLUMNS || count <= 0)
            return 0;
        int index = chestGetSlotIndex(row, column);
        if(this.data.BlockCounts[index] <= 0)
            return 0;
        if(!this.data.BlockTypes[index].equals(b))
            return 0;
        if(this.data.BlockCounts[index] <= count)
        {
            int retval = this.data.BlockCounts[index];
            this.data.BlockCounts[index] = 0;
            this.data.BlockTypes[index] = null;
            return retval;
        }
        this.data.BlockCounts[index] -= count;
        return count;
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
    public Block furnaceGetSrcBlock()
    {
        return this.data.blockdata;
    }

    /** @return the block resulting from smelting this block or <code>null</code> */
    public Block getSmeltResult()
    {
        return getType().getSmeltResult().make(-1);
    }

    /** @return if this block is smeltable */
    public boolean isSmeltable()
    {
        return getType().isSmeltable();
    }

    /** @return the type of block being smelted into */
    public Block furnaceGetDestBlock()
    {
        if(this.data.blockdata == null)
            return null;
        return this.data.blockdata.getSmeltResult();
    }

    /** @return the amount of fuel left in this furnace */
    public int furnaceGetFuelLeft()
    {
        return this.data.intdata;
    }

    /** add more fuel to furnace
     * 
     * @param b
     *            block to add to this furnace */
    public void furnaceAddFire(final Block b)
    {
        this.data.intdata += b.getBurnTime();
    }

    /** add a block to smelt to this furnace
     * 
     * @param b
     *            the type of block to smelt
     * @return true if <code>b</code> can be added */
    public boolean furnaceAddBlock(final Block b)
    {
        if(b == null || b.getType() == BlockType.BTEmpty)
            return true;
        if(this.data.blockdata != null && !b.equals(this.data.blockdata))
            return false;
        if(!b.isSmeltable())
            return false;
        if(this.data.srccount >= BLOCK_STACK_SIZE)
            return false;
        this.data.blockdata = b;
        this.data.srccount++;
        return true;
    }

    /** remove a smelted block from this furnace
     * 
     * @return the smelted block or <code>null</code> if this furnace is empty */
    public Block furnaceRemoveBlock()
    {
        if(this.data.destcount > 1)
        {
            this.data.destcount--;
            return this.data.blockdata.getSmeltResult();
        }
        else if(this.data.destcount == 1)
        {
            Block retval = this.data.blockdata.getSmeltResult();
            if(this.data.srccount <= 0)
                this.data.blockdata = null;
            this.data.destcount = 0;
            return retval;
        }
        return null;
    }

    /** @return if this furnace is burning */
    public boolean furnaceIsBurning()
    {
        if(this.data.blockdata == null)
            return false;
        if(this.data.srccount <= 0)
            return false;
        if(this.data.intdata <= 0)
            return false;
        return true;
    }

    /** @return true if this is an item */
    public boolean isItem()
    {
        return this.type.isItem();
    }

    private static Vector solidAdjustPlayerPosition_position = Vector.allocate();

    private Vector solidAdjustPlayerPosition(final Vector position_in,
                                             final float height,
                                             final float distLimit)
    {
        Vector position = Vector.set(solidAdjustPlayerPosition_position,
                                     position_in);
        float playerMinY = position.getY() - (Player.PlayerHeight - 1.0f);
        if(position.getX() > -distLimit && position.getX() < 1 + distLimit
                && position.getY() > -distLimit
                && playerMinY < height + distLimit
                && position.getZ() > -distLimit
                && position.getZ() < 1 + distLimit)
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
    public Vector adjustPlayerPosition(final Vector position,
                                       final float distLimit)
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
        case BTBedrock:
            return solidAdjustPlayerPosition(position, 1, distLimit);
        case BTSapling:
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
        case BTPlank:
            return solidAdjustPlayerPosition(position, 1, distLimit);
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
        case BTSnow:
        case BTRedstoneComparator:
        case BTFarmland:
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
        case BTDispenser:
        case BTDropper:
        case BTHopper:
        case BTCactus:
        case BTWool:
            return solidAdjustPlayerPosition(position, getHeight(), distLimit);
        case BTBlazeRod:
        case BTBlazePowder:
        case BTStonePressurePlate:
        case BTWoodPressurePlate:
        case BTVines:
        case BTWoodAxe:
        case BTStoneAxe:
        case BTIronAxe:
        case BTGoldAxe:
        case BTDiamondAxe:
        case BTShears:
        case BTBucket:
        case BTQuartz:
        case BTCobweb:
        case BTString:
        case BTBow:
        case BTRedMushroom:
        case BTBrownMushroom:
        case BTDeadBush:
        case BTWoodHoe:
        case BTStoneHoe:
        case BTIronHoe:
        case BTGoldHoe:
        case BTDiamondHoe:
        case BTWheat:
        case BTDandelion:
        case BTRose:
        case BTSeeds:
        case BTTallGrass:
        case BTCocoa:
        case BTInkSac:
        case BTRoseRed:
        case BTCactusGreen:
        case BTPurpleDye:
        case BTCyanDye:
        case BTLightGrayDye:
        case BTGrayDye:
        case BTPinkDye:
        case BTLimeDye:
        case BTDandelionYellow:
        case BTLightBlueDye:
        case BTMagentaDye:
        case BTOrangeDye:
        case BTBoneMeal:
        case BTBone:
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

    /** @return true if this block is solid */
    public boolean isSolid()
    {
        return this.type.isSolid();
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
        case BTSnow:
        case BTVines:
        case BTWoodAxe:
        case BTStoneAxe:
        case BTIronAxe:
        case BTGoldAxe:
        case BTDiamondAxe:
        case BTShears:
        case BTBucket:
        case BTRedstoneComparator:
        case BTQuartz:
        case BTDispenser:
        case BTDropper:
        case BTCobweb:
        case BTString:
        case BTBow:
        case BTHopper:
        case BTCactus:
        case BTRedMushroom:
        case BTBrownMushroom:
        case BTDeadBush:
        case BTWoodHoe:
        case BTStoneHoe:
        case BTIronHoe:
        case BTGoldHoe:
        case BTDiamondHoe:
        case BTFarmland:
        case BTRose:
        case BTDandelion:
        case BTWheat:
        case BTSeeds:
        case BTTallGrass:
        case BTCocoa:
        case BTInkSac:
        case BTRoseRed:
        case BTCactusGreen:
        case BTPurpleDye:
        case BTCyanDye:
        case BTLightGrayDye:
        case BTGrayDye:
        case BTPinkDye:
        case BTLimeDye:
        case BTDandelionYellow:
        case BTLightBlueDye:
        case BTMagentaDye:
        case BTOrangeDye:
        case BTBoneMeal:
        case BTBone:
        case BTWool:
            return true;
        }
        return false;
    }

    private static Vector digBlock_t1 = Vector.allocate();
    private static Vector digBlock_t2 = Vector.allocate();

    /** add entities for digging block
     * 
     * @param x
     *            x coordinate
     * @param y
     *            y coordinate
     * @param z
     *            z coordinate
     * @param dropItems
     *            if this block should drop items
     * @param toolType
     *            the tool type */
    public void digBlock(final int x,
                         final int y,
                         final int z,
                         final boolean dropItems,
                         final BlockType.ToolType toolType)
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
            if(dropItems)
            {
                world.insertEntity(Entity.NewBlock(Vector.set(digBlock_t1,
                                                              x + 0.5f,
                                                              y + 0.5f,
                                                              z + 0.5f),
                                                   this.type.make(-1),
                                                   World.vRand(digBlock_t2,
                                                               0.1f)));
                if(this.data.blockdata == null)
                    return;
                for(int i = 0; i < this.data.srccount; i++)
                {
                    world.insertEntity(Entity.NewBlock(Vector.set(digBlock_t1,
                                                                  x + 0.5f,
                                                                  y + 0.5f,
                                                                  z + 0.5f),
                                                       this.data.blockdata,
                                                       World.vRand(digBlock_t2,
                                                                   0.1f)));
                }
                if(this.data.blockdata.getSmeltResult() == null)
                    return;
                for(int i = 0; i < this.data.destcount; i++)
                {
                    world.insertEntity(Entity.NewBlock(Vector.set(digBlock_t1,
                                                                  x + 0.5f,
                                                                  y + 0.5f,
                                                                  z + 0.5f),
                                                       this.data.blockdata.getSmeltResult(),
                                                       World.vRand(digBlock_t2,
                                                                   0.1f)));
                }
            }
            return;
        case BTChest:
            if(dropItems)
            {
                world.insertEntity(Entity.NewBlock(Vector.set(digBlock_t1,
                                                              x + 0.5f,
                                                              y + 0.5f,
                                                              z + 0.5f),
                                                   this.type.make(-1),
                                                   World.vRand(digBlock_t2,
                                                               0.1f)));
                for(int i = 0; i < CHEST_COLUMNS * CHEST_ROWS; i++)
                    for(int j = 0; j < this.data.BlockCounts[i]; j++)
                        world.insertEntity(Entity.NewBlock(Vector.set(digBlock_t1,
                                                                      x + 0.5f,
                                                                      y + 0.5f,
                                                                      z + 0.5f),
                                                           this.data.BlockTypes[i],
                                                           World.vRand(digBlock_t2,
                                                                       0.1f)));
            }
            return;
        case BTDispenser:
        case BTDropper:
            if(dropItems)
            {
                world.insertEntity(Entity.NewBlock(Vector.set(digBlock_t1,
                                                              x + 0.5f,
                                                              y + 0.5f,
                                                              z + 0.5f),
                                                   this.type.make(-1),
                                                   World.vRand(digBlock_t2,
                                                               0.1f)));
                for(int row = 0; row < DISPENSER_DROPPER_ROWS; row++)
                    for(int column = 0, index = getDispenserDropperSlotIndex(row,
                                                                             column); column < DISPENSER_DROPPER_COLUMNS; column++, index = getDispenserDropperSlotIndex(row,
                                                                                                                                                                         column))
                        for(int j = 0; j < this.data.BlockCounts[index]; j++)
                            world.insertEntity(Entity.NewBlock(Vector.set(digBlock_t1,
                                                                          x + 0.5f,
                                                                          y + 0.5f,
                                                                          z + 0.5f),
                                                               this.data.BlockTypes[index],
                                                               World.vRand(digBlock_t2,
                                                                           0.1f)));
            }
            return;
        case BTHopper:
            if(dropItems)
            {
                world.insertEntity(Entity.NewBlock(Vector.set(digBlock_t1,
                                                              x + 0.5f,
                                                              y + 0.5f,
                                                              z + 0.5f),
                                                   this.type.make(-1),
                                                   World.vRand(digBlock_t2,
                                                               0.1f)));
                for(int slot = 0; slot < HOPPER_SLOTS; slot++)
                    for(int j = 0; j < this.data.BlockCounts[slot]; j++)
                        world.insertEntity(Entity.NewBlock(Vector.set(digBlock_t1,
                                                                      x + 0.5f,
                                                                      y + 0.5f,
                                                                      z + 0.5f),
                                                           this.data.BlockTypes[slot],
                                                           World.vRand(digBlock_t2,
                                                                       0.1f)));
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
        case BTRedstoneBlock:
        case BTRedstoneDustOff:
        case BTRedstoneTorchOff:
        case BTSand:
        case BTStick:
        case BTStoneButton:
        case BTStonePick:
        case BTStoneShovel:
        case BTTorch:
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
        case BTSnow:
        case BTWoodAxe:
        case BTStoneAxe:
        case BTIronAxe:
        case BTGoldAxe:
        case BTDiamondAxe:
        case BTShears:
        case BTBucket:
        case BTRedstoneComparator:
        case BTQuartz:
        case BTString:
        case BTBow:
        case BTCactus:
        case BTRedMushroom:
        case BTBrownMushroom:
        case BTDeadBush:
        case BTWoodHoe:
        case BTStoneHoe:
        case BTIronHoe:
        case BTGoldHoe:
        case BTDiamondHoe:
        case BTDandelion:
        case BTRose:
        case BTWheat:
        case BTInkSac:
        case BTRoseRed:
        case BTCactusGreen:
        case BTPurpleDye:
        case BTCyanDye:
        case BTLightGrayDye:
        case BTGrayDye:
        case BTPinkDye:
        case BTLimeDye:
        case BTDandelionYellow:
        case BTLightBlueDye:
        case BTMagentaDye:
        case BTOrangeDye:
        case BTBoneMeal:
        case BTBone:
            if(dropItems)
                world.insertEntity(Entity.NewBlock(Vector.set(digBlock_t1,
                                                              x + 0.5f,
                                                              y + 0.5f,
                                                              z + 0.5f),
                                                   this.type.make(-1),
                                                   World.vRand(digBlock_t2,
                                                               0.1f)));
            return;
        case BTWool:
            if(dropItems)
                world.insertEntity(Entity.NewBlock(Vector.set(digBlock_t1,
                                                              x + 0.5f,
                                                              y + 0.5f,
                                                              z + 0.5f),
                                                   NewWool(this.data.dyeColor),
                                                   World.vRand(digBlock_t2,
                                                               0.1f)));
            return;
        case BTSapling:
            if(dropItems)
                world.insertEntity(Entity.NewBlock(Vector.set(digBlock_t1,
                                                              x + 0.5f,
                                                              y + 0.5f,
                                                              z + 0.5f),
                                                   NewSapling(treeGetTreeType()),
                                                   World.vRand(digBlock_t2,
                                                               0.1f)));
            return;
        case BTPlank:
            if(dropItems)
                world.insertEntity(Entity.NewBlock(Vector.set(digBlock_t1,
                                                              x + 0.5f,
                                                              y + 0.5f,
                                                              z + 0.5f),
                                                   NewPlank(treeGetTreeType()),
                                                   World.vRand(digBlock_t2,
                                                               0.1f)));
            return;
        case BTWood:
            if(dropItems)
                world.insertEntity(Entity.NewBlock(Vector.set(digBlock_t1,
                                                              x + 0.5f,
                                                              y + 0.5f,
                                                              z + 0.5f),
                                                   NewWood(treeGetTreeType(), 0),
                                                   World.vRand(digBlock_t2,
                                                               0.1f)));
            return;
        case BTRedstoneOre:
        {
            if(dropItems)
            {
                int count = Math.round(World.fRand(4 - 0.5f, 5 + 0.5f));
                for(int i = 0; i < count; i++)
                    world.insertEntity(Entity.NewBlock(Vector.set(digBlock_t1,
                                                                  x + 0.5f,
                                                                  y + 0.5f,
                                                                  z + 0.5f),
                                                       NewRedstoneDust(0, 0),
                                                       World.vRand(digBlock_t2,
                                                                   0.1f)));
            }
            return;
        }
        case BTCoalOre:
            if(dropItems)
                world.insertEntity(Entity.NewBlock(Vector.set(digBlock_t1,
                                                              x + 0.5f,
                                                              y + 0.5f,
                                                              z + 0.5f),
                                                   NewCoal(),
                                                   World.vRand(digBlock_t2,
                                                               0.1f)));
            return;
        case BTDiamondOre:
            if(dropItems)
                world.insertEntity(Entity.NewBlock(Vector.set(digBlock_t1,
                                                              x + 0.5f,
                                                              y + 0.5f,
                                                              z + 0.5f),
                                                   NewDiamond(),
                                                   World.vRand(digBlock_t2,
                                                               0.1f)));
            return;
        case BTEmeraldOre:
            if(dropItems)
                world.insertEntity(Entity.NewBlock(Vector.set(digBlock_t1,
                                                              x + 0.5f,
                                                              y + 0.5f,
                                                              z + 0.5f),
                                                   NewEmerald(),
                                                   World.vRand(digBlock_t2,
                                                               0.1f)));
            return;
        case BTGrass:
        case BTFarmland:
            if(dropItems)
                world.insertEntity(Entity.NewBlock(Vector.set(digBlock_t1,
                                                              x + 0.5f,
                                                              y + 0.5f,
                                                              z + 0.5f),
                                                   NewDirt(),
                                                   World.vRand(digBlock_t2,
                                                               0.1f)));
            return;
        case BTLapisLazuliOre:
        {
            if(dropItems)
            {
                int count = Math.round(World.fRand(4 - 0.5f, 8 + 0.5f));
                for(int i = 0; i < count; i++)
                    world.insertEntity(Entity.NewBlock(Vector.set(digBlock_t1,
                                                                  x + 0.5f,
                                                                  y + 0.5f,
                                                                  z + 0.5f),
                                                       NewLapisLazuli(),
                                                       World.vRand(digBlock_t2,
                                                                   0.1f)));
            }
            return;
        }
        case BTWater:
        case BTLava:
        {
            if(dropItems)
                if(Math.abs(this.data.intdata) >= 8)
                    world.insertEntity(Entity.NewBlock(Vector.set(digBlock_t1,
                                                                  x + 0.5f,
                                                                  y + 0.5f,
                                                                  z + 0.5f),
                                                       this.type.make(-1),
                                                       World.vRand(digBlock_t2,
                                                                   0.1f)));
            return;
        }
        case BTRedstoneRepeaterOn:
            if(dropItems)
                world.insertEntity(Entity.NewBlock(Vector.set(digBlock_t1,
                                                              x + 0.5f,
                                                              y + 0.5f,
                                                              z + 0.5f),
                                                   BlockType.BTRedstoneRepeaterOff.make(-1),
                                                   World.vRand(digBlock_t2,
                                                               0.1f)));
            return;
        case BTRedstoneDustOn:
            if(dropItems)
                world.insertEntity(Entity.NewBlock(Vector.set(digBlock_t1,
                                                              x + 0.5f,
                                                              y + 0.5f,
                                                              z + 0.5f),
                                                   BlockType.BTRedstoneDustOff.make(-1),
                                                   World.vRand(digBlock_t2,
                                                               0.1f)));
            return;
        case BTRedstoneTorchOn:
            if(dropItems)
                world.insertEntity(Entity.NewBlock(Vector.set(digBlock_t1,
                                                              x + 0.5f,
                                                              y + 0.5f,
                                                              z + 0.5f),
                                                   BlockType.BTRedstoneTorchOff.make(-1),
                                                   World.vRand(digBlock_t2,
                                                               0.1f)));
            return;
        case BTStone:
            if(dropItems)
                world.insertEntity(Entity.NewBlock(Vector.set(digBlock_t1,
                                                              x + 0.5f,
                                                              y + 0.5f,
                                                              z + 0.5f),
                                                   NewCobblestone(),
                                                   World.vRand(digBlock_t2,
                                                               0.1f)));
            return;
        case BTLeaves:
        {
            if(dropItems)
            {
                if(toolType == ToolType.Shears)
                {
                    world.insertEntity(Entity.NewBlock(Vector.set(digBlock_t1,
                                                                  x + 0.5f,
                                                                  y + 0.5f,
                                                                  z + 0.5f),
                                                       NewLeaves(treeGetTreeType()),
                                                       World.vRand(digBlock_t2,
                                                                   0.1f)));
                }
                else if(treeGetTreeType() == TreeType.Jungle)
                {
                    if(World.fRand(0.0f, 1.0f) < 1.0f / 40)
                    {
                        world.insertEntity(Entity.NewBlock(Vector.set(digBlock_t1,
                                                                      x + 0.5f,
                                                                      y + 0.5f,
                                                                      z + 0.5f),
                                                           NewSapling(treeGetTreeType()),
                                                           World.vRand(digBlock_t2,
                                                                       0.1f)));
                    }
                }
                else if(World.fRand(0.0f, 1.0f) < 1.0f / 20)
                {
                    world.insertEntity(Entity.NewBlock(Vector.set(digBlock_t1,
                                                                  x + 0.5f,
                                                                  y + 0.5f,
                                                                  z + 0.5f),
                                                       NewSapling(treeGetTreeType()),
                                                       World.vRand(digBlock_t2,
                                                                   0.1f)));
                }
            }
            return;
        }
        case BTVines:
        {
            if(dropItems)
            {
                if(toolType == ToolType.Shears)
                {
                    world.insertEntity(Entity.NewBlock(Vector.set(digBlock_t1,
                                                                  x + 0.5f,
                                                                  y + 0.5f,
                                                                  z + 0.5f),
                                                       NewVines(-1),
                                                       World.vRand(digBlock_t2,
                                                                   0.1f)));
                }
            }
            return;
        }
        case BTTallGrass:
        {
            if(dropItems)
            {
                if(toolType == ToolType.Shears)
                {
                    world.insertEntity(Entity.NewBlock(Vector.set(digBlock_t1,
                                                                  x + 0.5f,
                                                                  y + 0.5f,
                                                                  z + 0.5f),
                                                       new Block(this),
                                                       World.vRand(digBlock_t2,
                                                                   0.1f)));
                }
                else
                {
                    if(World.fRand(0, 8) <= 1)
                    {
                        world.insertEntity(Entity.NewBlock(Vector.set(digBlock_t1,
                                                                      x + 0.5f,
                                                                      y + 0.5f,
                                                                      z + 0.5f),
                                                           NewSeeds(0),
                                                           World.vRand(digBlock_t2,
                                                                       0.1f)));
                    }
                }
            }
            break;
        }
        case BTCocoa:
        {
            if(dropItems)
            {
                int count = 1;
                if(this.data.intdata >= 2)
                {
                    count = 2;
                    if(World.fRand(0, 1) <= 0.5f)
                        count = 3;
                }
                for(int i = 0; i < count; i++)
                {
                    world.insertEntity(Entity.NewBlock(Vector.set(digBlock_t1,
                                                                  x + 0.5f,
                                                                  y + 0.5f,
                                                                  z + 0.5f),
                                                       NewCocoa(0, -1),
                                                       World.vRand(digBlock_t2,
                                                                   0.1f)));
                }
            }
            break;
        }
        case BTSeeds:
        {
            if(dropItems)
            {
                if(this.data.intdata >= 7)
                {
                    world.insertEntity(Entity.NewBlock(Vector.set(digBlock_t1,
                                                                  x + 0.5f,
                                                                  y + 0.5f,
                                                                  z + 0.5f),
                                                       NewWheat(),
                                                       World.vRand(digBlock_t2,
                                                                   0.1f)));
                }
                int count = Math.min(2, (int)Math.floor(World.fRand(0, 2 + 1)));
                for(int i = 0; i < count; i++)
                {
                    world.insertEntity(Entity.NewBlock(Vector.set(digBlock_t1,
                                                                  x + 0.5f,
                                                                  y + 0.5f,
                                                                  z + 0.5f),
                                                       NewSeeds(0),
                                                       World.vRand(digBlock_t2,
                                                                   0.1f)));
                }
            }
            break;
        }
        case BTPiston:
        case BTStickyPiston:
        {
            if(dropItems)
                world.insertEntity(Entity.NewBlock(Vector.set(digBlock_t1,
                                                              x + 0.5f,
                                                              y + 0.5f,
                                                              z + 0.5f),
                                                   this.type.make(-1),
                                                   World.vRand(digBlock_t2,
                                                               0.1f)));
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
            if(dropItems)
                world.insertEntity(Entity.NewBlock(Vector.set(digBlock_t1,
                                                              x + 0.5f,
                                                              y + 0.5f,
                                                              z + 0.5f),
                                                   (this.type == BlockType.BTStickyPistonHead) ? NewStickyPiston(-1,
                                                                                                                 false)
                                                           : NewPiston(-1,
                                                                       false),
                                                   World.vRand(digBlock_t2,
                                                               0.1f)));
            Block body = world.getBlockEval(bx, by, bz);
            if(body == null)
                return;
            if(body.getType() != BlockType.BTPiston
                    && body.getType() != BlockType.BTStickyPiston)
                return;
            world.setBlock(bx, by, bz, new Block());
            return;
        }
        case BTCobweb:
        {
            if(dropItems)
                world.insertEntity(Entity.NewBlock(Vector.set(digBlock_t1,
                                                              x + 0.5f,
                                                              y + 0.5f,
                                                              z + 0.5f),
                                                   NewString(),
                                                   World.vRand(digBlock_t2,
                                                               0.1f)));
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

    private int getRedstoneIOValue(final int dir)
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
        case BTDispenser:
        case BTDropper:
        case BTHopper:
            return REDSTONE_POWER_INPUT;
        case BTBlazeRod:
        case BTBlazePowder:
        case BTSnow:
        case BTVines:
        case BTWoodAxe:
        case BTStoneAxe:
        case BTIronAxe:
        case BTGoldAxe:
        case BTDiamondAxe:
        case BTShears:
        case BTBucket:
        case BTQuartz:
        case BTCobweb:
        case BTString:
        case BTBow:
        case BTCactus:
        case BTRedMushroom:
        case BTBrownMushroom:
        case BTDeadBush:
        case BTWoodHoe:
        case BTStoneHoe:
        case BTIronHoe:
        case BTGoldHoe:
        case BTDiamondHoe:
        case BTDandelion:
        case BTRose:
        case BTSeeds:
        case BTFarmland:
        case BTWheat:
        case BTTallGrass:
        case BTCocoa:
        case BTInkSac:
        case BTRoseRed:
        case BTCactusGreen:
        case BTPurpleDye:
        case BTCyanDye:
        case BTLightGrayDye:
        case BTGrayDye:
        case BTPinkDye:
        case BTLimeDye:
        case BTDandelionYellow:
        case BTLightBlueDye:
        case BTMagentaDye:
        case BTOrangeDye:
        case BTBoneMeal:
        case BTBone:
        case BTWool:
            return REDSTONE_POWER_NONE;
        case BTRedstoneComparator:
        {
            if(dir == this.data.orientation)
            {
                if(this.data.intdata == 0)
                    return REDSTONE_POWER_WEAK_OFF;
                return REDSTONE_POWER_WEAK_MIN + this.data.intdata - 1;
            }
            else if(getOrientationDY(dir) == 0)
                return REDSTONE_POWER_INPUT;
            return REDSTONE_POWER_NONE;
        }
        }
        return REDSTONE_POWER_NONE;
    }

    private static int getEvalRedstoneIOPowerH(final int origValue,
                                               final int bx,
                                               final int by,
                                               final int bz,
                                               final int dir)
    {
        Block b = world.getBlockEval(bx, by, bz);
        if(b == null)
            return origValue;
        int v = b.getRedstoneIOValue(dir);
        if(v == REDSTONE_POWER_STRONG || origValue == REDSTONE_POWER_STRONG)
            return REDSTONE_POWER_STRONG;
        if(v >= REDSTONE_POWER_WEAK_MIN && v <= REDSTONE_POWER_WEAK_MAX)
        {
            if(origValue >= REDSTONE_POWER_WEAK_MIN
                    && origValue <= REDSTONE_POWER_WEAK_MAX)
                return Math.max(v, origValue);
            return v;
        }
        if(origValue >= REDSTONE_POWER_WEAK_MIN
                && origValue <= REDSTONE_POWER_WEAK_MAX)
            return origValue;
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

    private static int getEvalRedstoneDustIOPowerH(final int origValue,
                                                   final int bx,
                                                   final int by,
                                                   final int bz,
                                                   final int dir)
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
        case BTSnow:
        case BTVines:
        case BTWoodAxe:
        case BTStoneAxe:
        case BTIronAxe:
        case BTGoldAxe:
        case BTDiamondAxe:
        case BTShears:
        case BTBucket:
        case BTRedstoneComparator:
        case BTQuartz:
        case BTCobweb:
        case BTString:
        case BTBow:
        case BTHopper:
        case BTCactus:
        case BTRedMushroom:
        case BTBrownMushroom:
        case BTDeadBush:
        case BTWoodHoe:
        case BTStoneHoe:
        case BTIronHoe:
        case BTGoldHoe:
        case BTDiamondHoe:
        case BTDandelion:
        case BTRose:
        case BTSeeds:
        case BTFarmland:
        case BTWheat:
        case BTTallGrass:
        case BTCocoa:
        case BTInkSac:
        case BTRoseRed:
        case BTCactusGreen:
        case BTPurpleDye:
        case BTCyanDye:
        case BTLightGrayDye:
        case BTGrayDye:
        case BTPinkDye:
        case BTLimeDye:
        case BTDandelionYellow:
        case BTLightBlueDye:
        case BTMagentaDye:
        case BTOrangeDye:
        case BTBoneMeal:
        case BTBone:
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
        case BTDispenser:
        case BTDropper:
        case BTWool:
            return true;
        }
        return false;
    }

    /** @param orientation
     *            the orientation
     * @return the orientation's x component */
    public static int getOrientationDX(final int orientation)
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
    public static int getOrientationDY(final int orientation)
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
    public static int getOrientationDZ(final int orientation)
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
    public static int getNegOrientation(final int orientation)
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

    private static Vector getRotatedOrientation_vAxis = Vector.allocate();
    private static Vector getRotatedOrientation_vOrigionalOrientation = Vector.allocate();
    private static Vector getRotatedOrientation_t1 = Vector.allocate();
    private static Matrix getRotatedOrientation_t2 = new Matrix();

    /** @param axis
     *            the axis to rotate around
     * @param angle
     *            the angle to rotate. for an angle x, <code>angle</code> = x
     *            &divide; 90&deg;
     * @param originalOrientation
     *            the orientation to rotate
     * @return the rotated orientation */
    public static int getRotatedOrientation(final int axis,
                                            final int angle,
                                            final int originalOrientation)
    {
        Vector vAxis = Vector.set(getRotatedOrientation_vAxis,
                                  getOrientationDX(axis),
                                  getOrientationDY(axis),
                                  getOrientationDZ(axis));
        Vector vOriginalOrientation = Vector.set(getRotatedOrientation_vOrigionalOrientation,
                                                 getOrientationDX(originalOrientation),
                                                 getOrientationDY(originalOrientation),
                                                 getOrientationDZ(originalOrientation));
        return getOrientationFromVector(Matrix.setToRotate(getRotatedOrientation_t2,
                                                           vAxis,
                                                           angle * Math.PI / 2)
                                              .apply(getRotatedOrientation_t1,
                                                     vOriginalOrientation));
    }

    private static Vector getOrientationFromVector_t1 = Vector.allocate();

    /** @param dir
     *            the <code>Vector</code> to get an orientation from
     * @return the orientation that <code>dir</code> is closest to */
    public static int getOrientationFromVector(final Vector dir)
    {
        Vector absv = Vector.set(getOrientationFromVector_t1,
                                 Math.abs(dir.getX()),
                                 Math.abs(dir.getY()),
                                 Math.abs(dir.getZ()));
        if(absv.getX() >= absv.getY())
        {
            if(absv.getX() >= absv.getZ())
            {
                if(dir.getX() <= 0)
                    return 0;
                return 2;
            }
            if(dir.getZ() <= 0)
                return 1;
            return 3;
        }
        if(absv.getY() >= absv.getZ())
        {
            if(dir.getY() <= 0)
                return 4;
            return 5;
        }
        if(dir.getZ() <= 0)
            return 1;
        return 3;
    }

    private static int getEvalRedstoneIOValue(final int bx,
                                              final int by,
                                              final int bz,
                                              final int dir)
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

    private static int getEvalRedstoneDustIOValue(final int bx,
                                                  final int by,
                                                  final int bz,
                                                  final int dir)
    {
        Block b = world.getBlockEval(bx, by, bz);
        int retval = -1;
        if(b == null)
            return REDSTONE_POWER_NONE;
        // if(dir == 4
        // && (b.getType() == BlockType.BTRedstoneTorchOff || b.getType() ==
        // BlockType.BTRedstoneTorchOn))
        // {
        // return b.getRedstoneIOValue(dir);
        // }
        // else if(dir == 4)
        // return REDSTONE_POWER_NONE;
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
        case BTVines:
        case BTWoodAxe:
        case BTStoneAxe:
        case BTIronAxe:
        case BTGoldAxe:
        case BTDiamondAxe:
        case BTShears:
        case BTBucket:
        case BTQuartz:
        case BTCobweb:
        case BTString:
        case BTBow:
        case BTCactus:
        case BTRedMushroom:
        case BTBrownMushroom:
        case BTDeadBush:
        case BTDandelion:
        case BTRose:
        case BTSeeds:
        case BTWheat:
        case BTTallGrass:
        case BTWoodHoe:
        case BTStoneHoe:
        case BTIronHoe:
        case BTGoldHoe:
        case BTDiamondHoe:
        case BTCocoa:
        case BTInkSac:
        case BTRoseRed:
        case BTCactusGreen:
        case BTPurpleDye:
        case BTCyanDye:
        case BTLightGrayDye:
        case BTGrayDye:
        case BTPinkDye:
        case BTLimeDye:
        case BTDandelionYellow:
        case BTLightBlueDye:
        case BTMagentaDye:
        case BTOrangeDye:
        case BTBoneMeal:
        case BTBone:
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
        case BTSnow:
        case BTRedstoneComparator:
        case BTDispenser:
        case BTDropper:
        case BTHopper:
        case BTFarmland:
        case BTWool:
            return true;
        }
        return true;
    }

    private static Vector climbableIsPlayerPushingIntoLadder_ladderOrientation = Vector.allocate();
    private static Vector climbableIsPlayerPushingIntoLadder_t1 = Vector.allocate();
    private static Vector climbableIsPlayerPushingIntoLadder_t2 = Vector.allocate();

    /** @param pos
     *            the player's relative position
     * @param dir
     *            the direction the player wants to move
     * @return true if the player is pushing into this climbable block */
    public boolean climbableIsPlayerPushingIntoLadder(final Vector pos,
                                                      final Vector dir)
    {
        Vector ladderOrientation = Vector.set(climbableIsPlayerPushingIntoLadder_ladderOrientation,
                                              getOrientationDX(this.data.orientation),
                                              getOrientationDY(this.data.orientation),
                                              getOrientationDZ(this.data.orientation));
        if(Vector.sub(climbableIsPlayerPushingIntoLadder_t1,
                      pos,
                      Vector.set(climbableIsPlayerPushingIntoLadder_t2,
                                 0.5f,
                                 0.5f,
                                 0.5f)).dot(ladderOrientation) <= 0)
            return false;
        if(getRayExitSide(pos,
                          Vector.normalize(climbableIsPlayerPushingIntoLadder_t1,
                                           dir)) == this.data.orientation)
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
    public static Block make(final BlockType bt,
                             final int orientation,
                             final int vieworientation)
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
    public void write(final DataOutput o) throws IOException
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
        case BTDirt:
        case BTEmerald:
        case BTEmeraldOre:
        case BTEmpty:
        case BTGlass:
        case BTGoldIngot:
        case BTGoldOre:
        case BTGrass:
        case BTGravel:
        case BTIronIngot:
        case BTIronOre:
        case BTLapisLazuli:
        case BTLapisLazuliOre:
        case BTRedstoneBlock:
        case BTRedstoneOre:
        case BTSand:
        case BTStick:
        case BTStone:
        case BTBucket:
        case BTWorkbench:
        case BTObsidian:
        case BTCobweb:
        case BTString:
        case BTBow:
        case BTCactus:
        case BTRedMushroom:
        case BTBrownMushroom:
        case BTDeadBush:
        case BTDandelion:
        case BTRose:
        case BTWheat:
        case BTTallGrass:
        case BTInkSac:
        case BTRoseRed:
        case BTCactusGreen:
        case BTPurpleDye:
        case BTCyanDye:
        case BTLightGrayDye:
        case BTGrayDye:
        case BTPinkDye:
        case BTLimeDye:
        case BTDandelionYellow:
        case BTLightBlueDye:
        case BTMagentaDye:
        case BTOrangeDye:
        case BTBoneMeal:
        case BTBone:
            return;
        case BTDiamondPick:
        case BTDiamondShovel:
        case BTGoldPick:
        case BTGoldShovel:
        case BTIronPick:
        case BTIronShovel:
        case BTStonePick:
        case BTStoneShovel:
        case BTWoodPick:
        case BTWoodShovel:
        case BTWoodAxe:
        case BTStoneAxe:
        case BTIronAxe:
        case BTGoldAxe:
        case BTDiamondAxe:
        case BTShears:
        case BTWoodHoe:
        case BTStoneHoe:
        case BTIronHoe:
        case BTGoldHoe:
        case BTDiamondHoe:
            o.writeShort(this.data.intdata);
            return;
        case BTPlank:
        case BTSapling:
        case BTSeeds:
        case BTFarmland:
            o.writeByte(this.data.intdata);
            return;
        case BTLeaves:
            o.writeByte(this.data.intdata);
            o.writeByte(this.data.step);
            return;
        case BTWood:
        case BTCocoa:
            o.writeByte(this.data.intdata);
            o.writeByte(this.data.orientation);
            return;
        case BTChest:
        {
            for(int i = 0; i < CHEST_ROWS * CHEST_COLUMNS; i++)
            {
                o.writeInt(this.data.BlockCounts[i]);
                if(this.data.BlockCounts[i] > 0)
                {
                    this.data.BlockTypes[i].write(o);
                }
            }
            return;
        }
        case BTFurnace:
        {
            o.writeInt(this.data.intdata);
            o.writeInt(this.data.srccount);
            o.writeInt(this.data.destcount);
            o.writeBoolean(this.data.blockdata != null);
            if(this.data.blockdata != null)
                this.data.blockdata.write(o);
            if(this.data.intdata > 0 && this.data.srccount > 0
                    && this.data.blockdata != null)
            {
                double reltime = this.data.runTime - world.getCurTime();
                o.writeDouble(reltime);
            }
            return;
        }
        case BTHopper:
        {
            o.writeBoolean(this.data.intdata != 0);
            o.writeByte(this.data.step);
            o.writeByte(this.data.orientation);
            for(int slot = 0; slot < HOPPER_SLOTS; slot++)
            {
                o.writeInt(this.data.BlockCounts[slot]);
                if(this.data.BlockCounts[slot] > 0)
                    this.data.BlockTypes[slot].write(o);
            }
            return;
        }
        case BTLadder:
        case BTVines:
        {
            o.writeByte(this.data.orientation);
            return;
        }
        case BTLava:
        case BTWater:
        case BTStonePressurePlate:
        case BTWoodPressurePlate:
        case BTSnow:
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
        case BTRedstoneComparator:
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
        case BTQuartz:
            return;
        case BTDispenser:
        case BTDropper:
        {
            o.writeByte(this.data.orientation);
            o.writeBoolean(this.data.intdata != 0);
            for(int i = 0; i < DISPENSER_DROPPER_ROWS * DISPENSER_DROPPER_ROWS; i++)
            {
                o.writeInt(this.data.BlockCounts[i]);
                if(this.data.BlockCounts[i] > 0)
                {
                    this.data.BlockTypes[i].write(o);
                }
            }
            return;
        }
        case BTWool:
        {
            this.data.dyeColor.write(o);
            return;
        }
        }
    }

    private void internalRead(final DataInput i) throws IOException
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
        case BTDirt:
        case BTEmerald:
        case BTEmeraldOre:
        case BTEmpty:
        case BTGlass:
        case BTGoldIngot:
        case BTGoldOre:
        case BTGrass:
        case BTGravel:
        case BTIronIngot:
        case BTIronOre:
        case BTLapisLazuli:
        case BTLapisLazuliOre:
        case BTRedstoneBlock:
        case BTRedstoneOre:
        case BTSand:
        case BTStick:
        case BTStone:
        case BTBucket:
        case BTWorkbench:
        case BTObsidian:
        case BTCobweb:
        case BTString:
        case BTBow:
        case BTCactus:
        case BTRedMushroom:
        case BTBrownMushroom:
        case BTDeadBush:
        case BTDandelion:
        case BTRose:
        case BTWheat:
        case BTTallGrass:
        case BTInkSac:
        case BTRoseRed:
        case BTCactusGreen:
        case BTPurpleDye:
        case BTCyanDye:
        case BTLightGrayDye:
        case BTGrayDye:
        case BTPinkDye:
        case BTLimeDye:
        case BTDandelionYellow:
        case BTLightBlueDye:
        case BTMagentaDye:
        case BTOrangeDye:
        case BTBoneMeal:
        case BTBone:
            return;
        case BTDiamondPick:
        case BTDiamondShovel:
        case BTGoldPick:
        case BTGoldShovel:
        case BTIronPick:
        case BTIronShovel:
        case BTStonePick:
        case BTStoneShovel:
        case BTWoodPick:
        case BTWoodShovel:
        case BTWoodAxe:
        case BTStoneAxe:
        case BTIronAxe:
        case BTGoldAxe:
        case BTDiamondAxe:
        case BTShears:
        case BTWoodHoe:
        case BTStoneHoe:
        case BTIronHoe:
        case BTGoldHoe:
        case BTDiamondHoe:
            this.data.intdata = i.readUnsignedShort();
            if(this.data.intdata >= this.type.getDurability())
                throw new IOException("tool use count out of range");
            return;
        case BTSapling:
            this.data.intdata = i.readUnsignedByte();
            if(this.data.intdata >= Tree.TreeType.values().length)
                throw new IOException("sapling tree type out of range");
            return;
        case BTPlank:
            this.data.intdata = i.readUnsignedByte();
            if(this.data.intdata >= Tree.TreeType.values().length)
                throw new IOException("plank tree type out of range");
            return;
        case BTLeaves:
            this.data.intdata = i.readUnsignedByte();
            if(this.data.intdata >= Tree.TreeType.values().length)
                throw new IOException("leaves tree type out of range");
            this.data.step = i.readByte();
            if(this.data.step != -1
                    && (this.data.step < 0 || this.data.step > MAX_LEAVES_DISTANCE + 1))
                throw new IOException("leaves distance from wood out of range");
            return;
        case BTWood:
            this.data.intdata = i.readUnsignedByte();
            if(this.data.intdata >= Tree.TreeType.values().length)
                throw new IOException("wood tree type out of range");
            this.data.orientation = i.readUnsignedByte();
            if(this.data.intdata > 3)
                throw new IOException("wood orientation out of range");
            return;
        case BTChest:
        {
            this.data.BlockCounts = new int[CHEST_ROWS * CHEST_COLUMNS];
            this.data.BlockTypes = new Block[CHEST_ROWS * CHEST_COLUMNS];
            for(int index = 0; index < CHEST_ROWS * CHEST_COLUMNS; index++)
            {
                int value = i.readInt();
                if(value < 0 || value > BLOCK_STACK_SIZE)
                    throw new IOException("Chest slot block count is out of range");
                this.data.BlockCounts[index] = value;
                if(value > 0)
                    this.data.BlockTypes[index] = Block.read(i);
                else
                    this.data.BlockTypes[index] = null;
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
            if(i.readBoolean())
                this.data.blockdata = Block.read(i);
            else
                this.data.blockdata = null;
            if(this.data.blockdata == null)
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
                    && this.data.blockdata != null)
            {
                double value = i.readDouble();
                if(Double.isInfinite(value) || Double.isNaN(value)
                        || value <= 0 || value > 10.0)
                    throw new IOException("furnace left to smelt is out of range");
                this.data.runTime = value + world.getCurTime();
            }
            return;
        }
        case BTHopper:
        {
            this.data.intdata = i.readBoolean() ? 1 : 0;
            this.data.step = i.readUnsignedByte();
            if(this.data.step >= HOPPER_TRANSFER_STEP_COUNT
                    || (this.data.intdata != 0 && this.data.step != 0))
                throw new IOException("hopper step is out of range");
            this.data.orientation = i.readUnsignedByte();
            if(this.data.orientation > 4)
                throw new IOException("hopper orientation is out of range");
            this.data.BlockCounts = new int[HOPPER_SLOTS];
            this.data.BlockTypes = new Block[HOPPER_SLOTS];
            for(int slot = 0; slot < HOPPER_SLOTS; slot++)
            {
                this.data.BlockCounts[slot] = i.readInt();
                if(this.data.BlockCounts[slot] < 0
                        || this.data.BlockCounts[slot] > BLOCK_STACK_SIZE)
                    throw new IOException("hopper slot's item count is out of range");
                if(this.data.BlockCounts[slot] > 0)
                    this.data.BlockTypes[slot] = Block.read(i);
                else
                    this.data.BlockTypes[slot] = null;
            }
            return;
        }
        case BTLadder:
        case BTVines:
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
        case BTRedstoneComparator:
        {
            this.data.intdata = i.readUnsignedByte();
            if(this.data.intdata < 0 || this.data.intdata >= 16)
                throw new IOException("Redstone Comparator output power is out of range");
            this.data.step = i.readBoolean() ? 1 : 0;
            this.data.orientation = i.readUnsignedByte();
            if(this.data.orientation < 0 || this.data.orientation > 3)
                throw new IOException("Redstone Comparator orientation is out of range");
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
        case BTCocoa:
        {
            this.data.intdata = i.readUnsignedByte();
            if(this.data.intdata < 0 || this.data.intdata > 2)
                throw new IOException("cocoa growth stage is out of range");
            this.data.orientation = i.readUnsignedByte();
            if(this.data.orientation < 0 || this.data.orientation > 4)
                throw new IOException("cocoa orientation is out of range");
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
        case BTQuartz:
            return;
        case BTStonePressurePlate:
        case BTWoodPressurePlate:
        case BTSnow:
        {
            this.data.intdata = i.readUnsignedByte();
            return;
        }
        case BTDispenser:
        case BTDropper:
        {
            this.data.BlockCounts = new int[DISPENSER_DROPPER_ROWS
                    * DISPENSER_DROPPER_COLUMNS];
            this.data.BlockTypes = new Block[DISPENSER_DROPPER_ROWS
                    * DISPENSER_DROPPER_COLUMNS];
            this.data.orientation = i.readUnsignedByte();
            if(this.data.orientation < 0 || this.data.orientation > 5)
                throw new IOException("dispenser or dropper orientation is out of range");
            this.data.intdata = i.readBoolean() ? 1 : 0;
            for(int index = 0; index < DISPENSER_DROPPER_ROWS
                    * DISPENSER_DROPPER_ROWS; index++)
            {
                int value = i.readInt();
                if(value < 0 || value > BLOCK_STACK_SIZE)
                    throw new IOException("dispenser or dropper slot block count is out of range");
                this.data.BlockCounts[index] = value;
                if(value > 0)
                    this.data.BlockTypes[index] = Block.read(i);
                else
                    this.data.BlockTypes[index] = null;
            }
            return;
        }
        case BTSeeds:
        {
            this.data.intdata = i.readUnsignedByte();
            if(this.data.intdata < 0 || this.data.intdata > 7)
                throw new IOException("seeds growth stage is out of range");
            return;
        }
        case BTFarmland:
        {
            this.data.intdata = i.readBoolean() ? 1 : 0;
            return;
        }
        case BTWool:
        {
            this.data.dyeColor = DyeColor.read(i);
            if(this.data.dyeColor == DyeColor.None)
                throw new IOException("wool dye color is invalid");
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
    public static Block read(final DataInput i) throws IOException
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

    // /** check if <code>this</code> located at &lt;<code>bx</code>,
    // * <code>by</code>, <code>bz</code>&gt; draws identically to
    // <code>b</code>
    // * located at &lt;<code>bbx</code>, <code>bby</code>, <code>bbz</code>&gt;
    // *
    // * @param bx
    // * this block's x coordinate
    // * @param by
    // * this block's y coordinate
    // * @param bz
    // * this block's z coordinate
    // * @param b
    // * the other block
    // * @param bbx
    // * <code>b</code>'s x coordinate
    // * @param bby
    // * <code>b</code>'s y coordinate
    // * @param bbz
    // * <code>b</code>'s z coordinate
    // * @return if <code>this</code> located at &lt;<code>bx</code>,
    // * <code>by</code>, <code>bz</code>&gt; draws identically to
    // * <code>b</code> located at &lt;<code>bbx</code>, <code>bby</code>,
    // * <code>bbz</code>&gt; */
    // public boolean drawsSame(int bx,
    // int by,
    // int bz,
    // Block b,
    // int bbx,
    // int bby,
    // int bbz)
    // {
    // if(this.type != b.type)
    // return false;
    // if(this.lighting == null || b.lighting == null
    // || this.curSunlightFactor != b.curSunlightFactor)
    // return false;
    // for(int i = 0; i < this.lighting.length; i++)
    // {
    // if(this.lighting[i] != b.lighting[i])
    // return false;
    // }
    // switch(this.type.drawType)
    // {
    // case BDTButton:
    // case BDTItem:
    // case BDTNone:
    // case BDTSolidAllSides:
    // case BDTTorch:
    // break;
    // case BDTCustom:
    // break;
    // case BDTLiquid:
    // // TO DO finish
    // return false;
    // case BDTSolid:
    // {
    // Block nx = world.getBlock(bx - 1, by, bz);
    // Block px = world.getBlock(bx + 1, by, bz);
    // Block ny = world.getBlock(bx, by - 1, bz);
    // Block py = world.getBlock(bx, by + 1, bz);
    // Block nz = world.getBlock(bx, by, bz - 1);
    // Block pz = world.getBlock(bx, by, bz + 1);
    // int drawMask = 0;
    // if(nx != null && !nx.isOpaque())
    // drawMask |= DMaskNX;
    // if(px != null && !px.isOpaque())
    // drawMask |= DMaskPX;
    // if(ny != null && !ny.isOpaque())
    // drawMask |= DMaskNY;
    // if(py != null && !py.isOpaque())
    // drawMask |= DMaskPY;
    // if(nz != null && !nz.isOpaque())
    // drawMask |= DMaskNZ;
    // if(pz != null && !pz.isOpaque())
    // drawMask |= DMaskPZ;
    // Block nx2 = world.getBlock(bbx - 1, bby, bbz);
    // Block px2 = world.getBlock(bbx + 1, bby, bbz);
    // Block ny2 = world.getBlock(bbx, bby - 1, bbz);
    // Block py2 = world.getBlock(bbx, bby + 1, bbz);
    // Block nz2 = world.getBlock(bbx, bby, bbz - 1);
    // Block pz2 = world.getBlock(bbx, bby, bbz + 1);
    // int drawMask2 = 0;
    // if(nx2 != null && !nx2.isOpaque())
    // drawMask2 |= DMaskNX;
    // if(px2 != null && !px2.isOpaque())
    // drawMask2 |= DMaskPX;
    // if(ny2 != null && !ny2.isOpaque())
    // drawMask2 |= DMaskNY;
    // if(py2 != null && !py2.isOpaque())
    // drawMask2 |= DMaskPY;
    // if(nz2 != null && !nz2.isOpaque())
    // drawMask2 |= DMaskNZ;
    // if(pz2 != null && !pz2.isOpaque())
    // drawMask2 |= DMaskPZ;
    // if(drawMask != drawMask2)
    // return false;
    // }
    // }
    // switch(this.type)
    // {
    // case BTLast:
    // case BTSun:
    // case BTMoon:
    // return true;
    // case BTDeleteBlock:
    // return this.data.intdata == b.data.intdata;
    // case BTBedrock:
    // case BTBlazePowder:
    // case BTBlazeRod:
    // case BTChest:
    // case BTCoal:
    // case BTCoalOre:
    // case BTCobblestone:
    // case BTDiamond:
    // case BTDiamondOre:
    // case BTDiamondPick:
    // case BTDiamondShovel:
    // case BTDirt:
    // case BTEmerald:
    // case BTEmeraldOre:
    // case BTEmpty:
    // case BTGlass:
    // case BTGoldIngot:
    // case BTGoldOre:
    // case BTGoldPick:
    // case BTGoldShovel:
    // case BTGrass:
    // case BTGravel:
    // case BTGunpowder:
    // case BTIronIngot:
    // case BTIronOre:
    // case BTIronPick:
    // case BTIronShovel:
    // case BTLapisLazuli:
    // case BTLapisLazuliOre:
    // case BTObsidian:
    // case BTPlank:
    // case BTRedstoneBlock:
    // case BTRedstoneOre:
    // case BTSand:
    // case BTSapling:
    // case BTSlime:
    // case BTStick:
    // case BTStone:
    // case BTStonePick:
    // case BTStoneShovel:
    // case BTTNT:
    // case BTWoodPick:
    // case BTWoodShovel:
    // case BTWorkbench:
    // case BTStonePressurePlate:
    // case BTWoodPressurePlate:
    // return true;
    // case BTWood:
    // case BTLeaves:
    // {
    // // TO DO finish
    // Block nx = world.getBlock(bx - 1, by, bz);
    // Block px = world.getBlock(bx + 1, by, bz);
    // Block ny = world.getBlock(bx, by - 1, bz);
    // Block py = world.getBlock(bx, by + 1, bz);
    // Block nz = world.getBlock(bx, by, bz - 1);
    // Block pz = world.getBlock(bx, by, bz + 1);
    // int drawMask = 0;
    // if(nx != null && !nx.isOpaque())
    // drawMask |= DMaskNX;
    // if(px != null && !px.isOpaque())
    // drawMask |= DMaskPX;
    // if(ny != null && !ny.isOpaque())
    // drawMask |= DMaskNY;
    // if(py != null && !py.isOpaque())
    // drawMask |= DMaskPY;
    // if(nz != null && !nz.isOpaque())
    // drawMask |= DMaskNZ;
    // if(pz != null && !pz.isOpaque())
    // drawMask |= DMaskPZ;
    // Block nx2 = world.getBlock(bbx - 1, bby, bbz);
    // Block px2 = world.getBlock(bbx + 1, bby, bbz);
    // Block ny2 = world.getBlock(bbx, bby - 1, bbz);
    // Block py2 = world.getBlock(bbx, bby + 1, bbz);
    // Block nz2 = world.getBlock(bbx, bby, bbz - 1);
    // Block pz2 = world.getBlock(bbx, bby, bbz + 1);
    // int drawMask2 = 0;
    // if(nx2 != null && !nx2.isOpaque())
    // drawMask2 |= DMaskNX;
    // if(px2 != null && !px2.isOpaque())
    // drawMask2 |= DMaskPX;
    // if(ny2 != null && !ny2.isOpaque())
    // drawMask2 |= DMaskNY;
    // if(py2 != null && !py2.isOpaque())
    // drawMask2 |= DMaskPY;
    // if(nz2 != null && !nz2.isOpaque())
    // drawMask2 |= DMaskNZ;
    // if(pz2 != null && !pz2.isOpaque())
    // drawMask2 |= DMaskPZ;
    // if(drawMask != drawMask2)
    // return false;
    // return true;
    // }
    // case BTFurnace:
    // {
    // Block nx = world.getBlock(bx - 1, by, bz);
    // Block px = world.getBlock(bx + 1, by, bz);
    // Block ny = world.getBlock(bx, by - 1, bz);
    // Block py = world.getBlock(bx, by + 1, bz);
    // Block nz = world.getBlock(bx, by, bz - 1);
    // Block pz = world.getBlock(bx, by, bz + 1);
    // int drawMask = 0;
    // if(nx != null && !nx.isOpaque())
    // drawMask |= DMaskNX;
    // if(px != null && !px.isOpaque())
    // drawMask |= DMaskPX;
    // if(ny != null && !ny.isOpaque())
    // drawMask |= DMaskNY;
    // if(py != null && !py.isOpaque())
    // drawMask |= DMaskPY;
    // if(nz != null && !nz.isOpaque())
    // drawMask |= DMaskNZ;
    // if(pz != null && !pz.isOpaque())
    // drawMask |= DMaskPZ;
    // Block nx2 = world.getBlock(bbx - 1, bby, bbz);
    // Block px2 = world.getBlock(bbx + 1, bby, bbz);
    // Block ny2 = world.getBlock(bbx, bby - 1, bbz);
    // Block py2 = world.getBlock(bbx, bby + 1, bbz);
    // Block nz2 = world.getBlock(bbx, bby, bbz - 1);
    // Block pz2 = world.getBlock(bbx, bby, bbz + 1);
    // int drawMask2 = 0;
    // if(nx2 != null && !nx2.isOpaque())
    // drawMask2 |= DMaskNX;
    // if(px2 != null && !px2.isOpaque())
    // drawMask2 |= DMaskPX;
    // if(ny2 != null && !ny2.isOpaque())
    // drawMask2 |= DMaskNY;
    // if(py2 != null && !py2.isOpaque())
    // drawMask2 |= DMaskPY;
    // if(nz2 != null && !nz2.isOpaque())
    // drawMask2 |= DMaskNZ;
    // if(pz2 != null && !pz2.isOpaque())
    // drawMask2 |= DMaskPZ;
    // if(drawMask != drawMask2)
    // return false;
    // return furnaceIsBurning() == b.furnaceIsBurning();
    // }
    // case BTLadder:
    // case BTPistonHead:
    // case BTStickyPistonHead:
    // case BTRedstoneTorchOff:
    // case BTRedstoneTorchOn:
    // case BTTorch:
    // return this.data.orientation == b.data.orientation;
    // case BTLever:
    // case BTPiston:
    // case BTStickyPiston:
    // case BTStoneButton:
    // case BTWoodButton:
    // case BTRedstoneDustOff:
    // case BTRedstoneDustOn:
    // return this.data.orientation == b.data.orientation
    // && this.data.intdata == b.data.intdata;
    // case BTRedstoneRepeaterOff:
    // case BTRedstoneRepeaterOn:
    // {
    // if(this.data.orientation != b.data.orientation)
    // return false;
    // boolean isThisLocked = redstoneRepeaterIsLatched(bx, by, bz);
    // boolean isBLocked = redstoneRepeaterIsLatched(bbx, bby, bbz);
    // if(isThisLocked != isBLocked)
    // return false;
    // if(isThisLocked)
    // return true;
    // return this.data.intdata == b.data.intdata;
    // }
    // case BTLava:
    // case BTWater:
    // {
    // // TO DO finish
    // return false;
    // }
    // }
    // return false;
    // }
    //
    // private int concatHash(int oldHash, int newHash)
    // {
    // return oldHash + 37 * newHash;
    // }
    //
    // /** @param bx
    // * this block's x coordinate
    // * @param by
    // * this block's y coordinate
    // * @param bz
    // * this block's z coordinate
    // * @return the hash code for this block */
    // public int getDrawHashcode(int bx, int by, int bz)
    // {
    // int hash = this.type.value;
    // if(this.lighting != null)
    // {
    // for(int i = 0; i < this.lighting.length; i++)
    // {
    // hash = concatHash(hash, this.lighting[i]);
    // }
    // }
    // switch(this.type.drawType)
    // {
    // case BDTButton:
    // case BDTItem:
    // case BDTNone:
    // case BDTSolidAllSides:
    // case BDTTorch:
    // break;
    // case BDTCustom:
    // break;
    // case BDTLiquid:
    // // TO DO finish
    // break;
    // case BDTSolid:
    // {
    // Block nx = world.getBlock(bx - 1, by, bz);
    // Block px = world.getBlock(bx + 1, by, bz);
    // Block ny = world.getBlock(bx, by - 1, bz);
    // Block py = world.getBlock(bx, by + 1, bz);
    // Block nz = world.getBlock(bx, by, bz - 1);
    // Block pz = world.getBlock(bx, by, bz + 1);
    // int drawMask = 0;
    // if(nx != null && !nx.isOpaque())
    // drawMask |= DMaskNX;
    // if(px != null && !px.isOpaque())
    // drawMask |= DMaskPX;
    // if(ny != null && !ny.isOpaque())
    // drawMask |= DMaskNY;
    // if(py != null && !py.isOpaque())
    // drawMask |= DMaskPY;
    // if(nz != null && !nz.isOpaque())
    // drawMask |= DMaskNZ;
    // if(pz != null && !pz.isOpaque())
    // drawMask |= DMaskPZ;
    // hash = concatHash(hash, drawMask);
    // break;
    // }
    // }
    // switch(this.type)
    // {
    // case BTLast:
    // case BTSun:
    // case BTMoon:
    // return hash;
    // case BTDeleteBlock:
    // hash = concatHash(hash, this.data.intdata);
    // return hash;
    // case BTBedrock:
    // case BTBlazePowder:
    // case BTBlazeRod:
    // case BTChest:
    // case BTCoal:
    // case BTCoalOre:
    // case BTCobblestone:
    // case BTDiamond:
    // case BTDiamondOre:
    // case BTDiamondPick:
    // case BTDiamondShovel:
    // case BTDirt:
    // case BTEmerald:
    // case BTEmeraldOre:
    // case BTEmpty:
    // case BTGlass:
    // case BTGoldIngot:
    // case BTGoldOre:
    // case BTGoldPick:
    // case BTGoldShovel:
    // case BTGrass:
    // case BTGravel:
    // case BTGunpowder:
    // case BTIronIngot:
    // case BTIronOre:
    // case BTIronPick:
    // case BTIronShovel:
    // case BTLapisLazuli:
    // case BTLapisLazuliOre:
    // case BTObsidian:
    // case BTPlank:
    // case BTRedstoneBlock:
    // case BTRedstoneOre:
    // case BTSand:
    // case BTSapling:
    // case BTSlime:
    // case BTStick:
    // case BTStone:
    // case BTStonePick:
    // case BTStoneShovel:
    // case BTTNT:
    // case BTWoodPick:
    // case BTWoodShovel:
    // case BTWorkbench:
    // case BTStonePressurePlate:
    // case BTWoodPressurePlate:
    // return hash;
    // case BTWood:
    // case BTLeaves:
    // {
    // // TO DO finish
    // Block nx = world.getBlock(bx - 1, by, bz);
    // Block px = world.getBlock(bx + 1, by, bz);
    // Block ny = world.getBlock(bx, by - 1, bz);
    // Block py = world.getBlock(bx, by + 1, bz);
    // Block nz = world.getBlock(bx, by, bz - 1);
    // Block pz = world.getBlock(bx, by, bz + 1);
    // int drawMask = 0;
    // if(nx != null && !nx.isOpaque())
    // drawMask |= DMaskNX;
    // if(px != null && !px.isOpaque())
    // drawMask |= DMaskPX;
    // if(ny != null && !ny.isOpaque())
    // drawMask |= DMaskNY;
    // if(py != null && !py.isOpaque())
    // drawMask |= DMaskPY;
    // if(nz != null && !nz.isOpaque())
    // drawMask |= DMaskNZ;
    // if(pz != null && !pz.isOpaque())
    // drawMask |= DMaskPZ;
    // hash = concatHash(hash, drawMask);
    // return hash;
    // }
    // case BTFurnace:
    // {
    // Block nx = world.getBlock(bx - 1, by, bz);
    // Block px = world.getBlock(bx + 1, by, bz);
    // Block ny = world.getBlock(bx, by - 1, bz);
    // Block py = world.getBlock(bx, by + 1, bz);
    // Block nz = world.getBlock(bx, by, bz - 1);
    // Block pz = world.getBlock(bx, by, bz + 1);
    // int drawMask = 0;
    // if(nx != null && !nx.isOpaque())
    // drawMask |= DMaskNX;
    // if(px != null && !px.isOpaque())
    // drawMask |= DMaskPX;
    // if(ny != null && !ny.isOpaque())
    // drawMask |= DMaskNY;
    // if(py != null && !py.isOpaque())
    // drawMask |= DMaskPY;
    // if(nz != null && !nz.isOpaque())
    // drawMask |= DMaskNZ;
    // if(pz != null && !pz.isOpaque())
    // drawMask |= DMaskPZ;
    // hash = concatHash(hash, drawMask);
    // hash = concatHash(hash, furnaceIsBurning() ? 1 : 0);
    // return hash;
    // }
    // case BTLadder:
    // case BTPistonHead:
    // case BTStickyPistonHead:
    // case BTRedstoneTorchOff:
    // case BTRedstoneTorchOn:
    // case BTTorch:
    // hash = concatHash(hash, this.data.orientation);
    // return hash;
    // case BTLever:
    // case BTPiston:
    // case BTStickyPiston:
    // case BTStoneButton:
    // case BTWoodButton:
    // case BTRedstoneDustOff:
    // case BTRedstoneDustOn:
    // hash = concatHash(hash, this.data.orientation);
    // hash = concatHash(hash, this.data.intdata);
    // return hash;
    // case BTRedstoneRepeaterOff:
    // case BTRedstoneRepeaterOn:
    // {
    // hash = concatHash(hash, this.data.orientation);
    // boolean isThisLocked = redstoneRepeaterIsLatched(bx, by, bz);
    // if(isThisLocked)
    // {
    // hash = concatHash(hash, 12345);
    // return hash;
    // }
    // hash = concatHash(hash, this.data.intdata);
    // return hash;
    // }
    // case BTLava:
    // case BTWater:
    // {
    // // TO DO finish
    // hash = concatHash(hash, this.data.intdata);
    // hash = concatHash(hash, bx);
    // hash = concatHash(hash, by);
    // hash = concatHash(hash, bz);
    // return hash;
    // }
    // }
    // hash = concatHash(hash, 67281763);
    // return hash;
    // }
    //
    // /** @param bx
    // * this block's x coordinate
    // * @param by
    // * this block's y coordinate
    // * @param bz
    // * this block's z coordinate
    // * @return if this block is cacheable */
    // public boolean isCacheable(int bx, int by, int bz)
    // {
    // if(this.lighting == null)
    // return false;
    // switch(this.type)
    // {
    // case BTLast:
    // case BTSun:
    // case BTMoon:
    // case BTDeleteBlock:
    // return true;
    // case BTBedrock:
    // case BTBlazePowder:
    // case BTBlazeRod:
    // case BTChest:
    // case BTCoal:
    // case BTCoalOre:
    // case BTCobblestone:
    // case BTDiamond:
    // case BTDiamondOre:
    // case BTDiamondPick:
    // case BTDiamondShovel:
    // case BTDirt:
    // case BTEmerald:
    // case BTEmeraldOre:
    // case BTEmpty:
    // case BTGlass:
    // case BTGoldIngot:
    // case BTGoldOre:
    // case BTGoldPick:
    // case BTGoldShovel:
    // case BTGrass:
    // case BTGravel:
    // case BTGunpowder:
    // case BTIronIngot:
    // case BTIronOre:
    // case BTIronPick:
    // case BTIronShovel:
    // case BTLapisLazuli:
    // case BTLapisLazuliOre:
    // case BTLeaves:
    // case BTObsidian:
    // case BTPlank:
    // case BTRedstoneBlock:
    // case BTRedstoneOre:
    // case BTSand:
    // case BTSapling:
    // case BTSlime:
    // case BTStick:
    // case BTStone:
    // case BTStonePick:
    // case BTStoneShovel:
    // case BTTNT:
    // case BTWood:
    // case BTWoodPick:
    // case BTWoodShovel:
    // case BTWorkbench:
    // case BTFurnace:
    // case BTLadder:
    // case BTPistonHead:
    // case BTStickyPistonHead:
    // case BTRedstoneTorchOff:
    // case BTRedstoneTorchOn:
    // case BTTorch:
    // case BTLever:
    // case BTPiston:
    // case BTStickyPiston:
    // case BTStoneButton:
    // case BTWoodButton:
    // case BTRedstoneDustOff:
    // case BTRedstoneDustOn:
    // case BTRedstoneRepeaterOff:
    // case BTRedstoneRepeaterOn:
    // case BTStonePressurePlate:
    // case BTWoodPressurePlate:
    // return true;
    // case BTLava:
    // case BTWater:
    // // TO DO finish
    // return false;
    // }
    // return false;
    // }
    /**
     * 
     */
    public void pressurePlatePress()
    {
        this.data.intdata = 4;
    }

    /** @param replacingBlock
     *            the replacing block
     * @return the replaceability of this block */
    public BlockType.Replaceability
        getReplaceability(final BlockType replacingBlock)
    {
        return this.type.getReplaceability(replacingBlock);
    }

    @Override
    public boolean equals(final Object rt_in)
    {
        if(rt_in == null)
            return this.type == BlockType.BTEmpty;
        if(!(rt_in instanceof Block))
            return false;
        Block rt = (Block)rt_in;
        if(this.type != rt.type)
            return false;
        switch(this.type)
        {
        case BTLast:
        case BTSun:
        case BTMoon:
        case BTDeleteBlock:
            return true;
        case BTBedrock:
        case BTEmpty:
            return true;
        case BTBlazePowder:
        case BTBlazeRod:
        case BTCoal:
        case BTCoalOre:
        case BTCobblestone:
        case BTDiamond:
        case BTDiamondOre:
        case BTDirt:
        case BTEmerald:
        case BTEmeraldOre:
        case BTGlass:
        case BTGoldIngot:
        case BTGoldOre:
        case BTGrass:
        case BTGravel:
        case BTGunpowder:
        case BTIronIngot:
        case BTIronOre:
        case BTLapisLazuli:
        case BTLapisLazuliOre:
        case BTObsidian:
        case BTPlank:
        case BTRedstoneBlock:
        case BTRedstoneOre:
        case BTSand:
        case BTSlime:
        case BTStick:
        case BTStone:
        case BTStonePick:
        case BTStoneShovel:
        case BTTNT:
        case BTBucket:
        case BTWorkbench:
        case BTQuartz:
        case BTCobweb:
        case BTString:
        case BTBow:
        case BTCactus:
        case BTRedMushroom:
        case BTBrownMushroom:
        case BTDeadBush:
        case BTDandelion:
        case BTRose:
        case BTWheat:
        case BTTallGrass:
        case BTInkSac:
        case BTRoseRed:
        case BTCactusGreen:
        case BTPurpleDye:
        case BTCyanDye:
        case BTLightGrayDye:
        case BTGrayDye:
        case BTPinkDye:
        case BTLimeDye:
        case BTDandelionYellow:
        case BTLightBlueDye:
        case BTMagentaDye:
        case BTOrangeDye:
        case BTBoneMeal:
        case BTBone:
            return true;
        case BTDiamondPick:
        case BTDiamondShovel:
        case BTGoldPick:
        case BTGoldShovel:
        case BTIronPick:
        case BTIronShovel:
        case BTWoodPick:
        case BTWoodShovel:
        case BTWoodAxe:
        case BTStoneAxe:
        case BTIronAxe:
        case BTGoldAxe:
        case BTDiamondAxe:
        case BTShears:
        case BTWoodHoe:
        case BTStoneHoe:
        case BTIronHoe:
        case BTGoldHoe:
        case BTDiamondHoe:
            return toolGetUseCount() == rt.toolGetUseCount();
        case BTChest:
        {
            for(int i = 0; i < CHEST_ROWS * CHEST_COLUMNS; i++)
            {
                if(this.data.BlockCounts[i] != rt.data.BlockCounts[i])
                    return false;
                if(this.data.BlockCounts[i] > 0
                        && !this.data.BlockTypes[i].equals(rt.data.BlockTypes[i]))
                    return false;
            }
            return true;
        }
        case BTFurnace:
            if(this.data.intdata != rt.data.intdata
                    || this.data.srccount != rt.data.srccount
                    || this.data.destcount != rt.data.destcount)
                return false;
            if(this.data.blockdata == null && rt.data.blockdata != null
                    && !rt.data.blockdata.equals(this.data.blockdata))
                return false;
            if(this.data.blockdata != null
                    && !this.data.blockdata.equals(rt.data.blockdata))
                return false;
            return true;
        case BTHopper:
            if(this.data.intdata != rt.data.intdata
                    || this.data.step != rt.data.step
                    || this.data.orientation != rt.data.orientation)
                return false;
            for(int slot = 0; slot < HOPPER_SLOTS; slot++)
            {
                if(this.data.BlockCounts[slot] != rt.data.BlockCounts[slot])
                    return false;
                if(this.data.BlockCounts[slot] > 0
                        && !this.data.BlockTypes[slot].equals(rt.data.BlockTypes[slot]))
                    return false;
            }
            return true;
        case BTLadder:
        case BTVines:
            return this.data.orientation == rt.data.orientation;
        case BTLava:
        case BTWater:
        case BTSeeds:
        case BTFarmland:
            return this.data.intdata == rt.data.intdata;
        case BTSapling:
            if(this.data.intdata != rt.data.intdata)
                return false;
            return true;
        case BTLeaves:
            return this.data.intdata == rt.data.intdata;
        case BTWood:
        case BTCocoa:
            if(this.data.orientation != rt.data.orientation)
                return false;
            return this.data.intdata == rt.data.intdata;
        case BTLever:
            if(this.data.orientation != rt.data.orientation)
                return false;
            return this.data.intdata == rt.data.intdata;
        case BTPiston:
        case BTStickyPiston:
            if(this.data.orientation != rt.data.orientation)
                return false;
            return this.data.intdata == rt.data.intdata;
        case BTPistonHead:
        case BTStickyPistonHead:
            return this.data.orientation == rt.data.orientation;
        case BTRedstoneDustOff:
        case BTRedstoneDustOn:
            if(this.data.orientation != rt.data.orientation)
                return false;
            return this.data.intdata == rt.data.intdata;
        case BTRedstoneRepeaterOff:
        case BTRedstoneRepeaterOn:
            if(this.data.orientation != rt.data.orientation)
                return false;
            return this.data.intdata == rt.data.intdata;
        case BTRedstoneTorchOff:
        case BTRedstoneTorchOn:
            return this.data.orientation == rt.data.orientation;
        case BTStoneButton:
        case BTWoodButton:
            if(this.data.orientation != rt.data.orientation)
                return false;
            return this.data.intdata == rt.data.intdata;
        case BTStonePressurePlate:
        case BTWoodPressurePlate:
        case BTSnow:
            return this.data.intdata == rt.data.intdata;
        case BTTorch:
            return this.data.orientation == rt.data.orientation;
        case BTRedstoneComparator:
            if(this.data.orientation != rt.data.orientation)
                return false;
            return this.data.step == rt.data.step;
        case BTDispenser:
        case BTDropper:
            if(this.data.orientation != rt.data.orientation)
                return false;
            if(this.data.intdata != rt.data.intdata)
                return false;
            for(int i = 0; i < DISPENSER_DROPPER_ROWS
                    * DISPENSER_DROPPER_COLUMNS; i++)
            {
                if(this.data.BlockCounts[i] != rt.data.BlockCounts[i])
                    return false;
                if(this.data.BlockCounts[i] > 0
                        && !this.data.BlockTypes[i].equals(rt.data.BlockTypes[i]))
                    return false;
            }
            return true;
        case BTWool:
            return this.data.dyeColor == rt.data.dyeColor;
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode()
    {
        int hash = this.type.value;
        switch(this.type)
        {
        case BTLast:
        case BTSun:
        case BTMoon:
        case BTDeleteBlock:
            return hash;
        case BTBedrock:
        case BTEmpty:
            return hash;
        case BTBlazePowder:
        case BTBlazeRod:
        case BTCoal:
        case BTCoalOre:
        case BTCobblestone:
        case BTDiamond:
        case BTDiamondOre:
        case BTDirt:
        case BTEmerald:
        case BTEmeraldOre:
        case BTGlass:
        case BTGoldIngot:
        case BTGoldOre:
        case BTGrass:
        case BTGravel:
        case BTGunpowder:
        case BTIronIngot:
        case BTIronOre:
        case BTLapisLazuli:
        case BTLapisLazuliOre:
        case BTObsidian:
        case BTPlank:
        case BTRedstoneBlock:
        case BTRedstoneOre:
        case BTSand:
        case BTSlime:
        case BTStick:
        case BTStone:
        case BTStonePick:
        case BTStoneShovel:
        case BTTNT:
        case BTBucket:
        case BTWorkbench:
        case BTQuartz:
        case BTCobweb:
        case BTString:
        case BTBow:
        case BTCactus:
        case BTRedMushroom:
        case BTBrownMushroom:
        case BTDeadBush:
        case BTDandelion:
        case BTRose:
        case BTWheat:
        case BTTallGrass:
        case BTInkSac:
        case BTRoseRed:
        case BTCactusGreen:
        case BTPurpleDye:
        case BTCyanDye:
        case BTLightGrayDye:
        case BTGrayDye:
        case BTPinkDye:
        case BTLimeDye:
        case BTDandelionYellow:
        case BTLightBlueDye:
        case BTMagentaDye:
        case BTOrangeDye:
        case BTBoneMeal:
        case BTBone:
            return hash;
        case BTDiamondPick:
        case BTDiamondShovel:
        case BTGoldPick:
        case BTGoldShovel:
        case BTIronPick:
        case BTIronShovel:
        case BTWoodPick:
        case BTWoodShovel:
        case BTWoodAxe:
        case BTStoneAxe:
        case BTIronAxe:
        case BTGoldAxe:
        case BTDiamondAxe:
        case BTShears:
        case BTWoodHoe:
        case BTStoneHoe:
        case BTIronHoe:
        case BTGoldHoe:
        case BTDiamondHoe:
            return hash + 21743678 * toolGetUseCount();
        case BTChest:
        {
            for(int i = 0; i < CHEST_ROWS * CHEST_COLUMNS; i++)
            {
                hash += 1273648 * this.data.BlockCounts[i];
                if(this.data.BlockCounts[i] > 0)
                    hash += 3 * this.data.BlockTypes[i].hashCode();
            }
            return hash;
        }
        case BTFurnace:
            hash += 2176438 * this.data.intdata + 127364 * this.data.srccount
                    + 61873268 * this.data.destcount;
            if(this.data.blockdata != null)
                hash += 3 * this.data.blockdata.hashCode();
            return hash;
        case BTHopper:
            hash += this.data.intdata * 234879 + this.data.step * 1279747
                    + this.data.orientation * 17492;
            for(int slot = 0; slot < HOPPER_SLOTS; slot++)
            {
                hash += 759234 * this.data.BlockCounts[slot];
                if(this.data.BlockCounts[slot] > 0)
                    hash += this.data.BlockTypes[slot].hashCode() * 129123;
            }
            return hash;
        case BTLadder:
        case BTVines:
            hash += 126364 * this.data.orientation;
            return hash;
        case BTLava:
        case BTWater:
        case BTSeeds:
        case BTFarmland:
            return hash + 12643 * this.data.intdata;
        case BTSapling:
        {
            hash += this.data.intdata * 7219843;
            long v = Double.doubleToLongBits(this.data.runTime);
            hash += 2183746 * (int)(v ^ (v >>> 32));
            return hash;
        }
        case BTLeaves:
            return hash + 162873468 * this.data.intdata;
        case BTWood:
        case BTCocoa:
            hash += 126364 * this.data.orientation;
            return hash + 162873468 * this.data.intdata;
        case BTLever:
            hash += 126364 * this.data.orientation;
            return hash + 162873468 * this.data.intdata;
        case BTPiston:
        case BTStickyPiston:
            hash += 126364 * this.data.orientation;
            return hash + 162873468 * this.data.intdata;
        case BTPistonHead:
        case BTStickyPistonHead:
            hash += 126364 * this.data.orientation;
            return hash;
        case BTRedstoneDustOff:
        case BTRedstoneDustOn:
            hash += 126364 * this.data.orientation;
            return hash + 162873468 * this.data.intdata;
        case BTRedstoneRepeaterOff:
        case BTRedstoneRepeaterOn:
            hash += 126364 * this.data.orientation;
            return hash + 162873468 * this.data.intdata;
        case BTRedstoneTorchOff:
        case BTRedstoneTorchOn:
            hash += 126364 * this.data.orientation;
            return hash;
        case BTStoneButton:
        case BTWoodButton:
            hash += 126364 * this.data.orientation;
            return hash + 162873468 * this.data.intdata;
        case BTStonePressurePlate:
        case BTWoodPressurePlate:
        case BTSnow:
            return hash + 162873468 * this.data.intdata;
        case BTTorch:
            hash += 126364 * this.data.orientation;
            return hash;
        case BTRedstoneComparator:
            hash += 7321493 * this.data.orientation;
            return hash + 2793489 * this.data.step;
        case BTDispenser:
        case BTDropper:
            hash += 126364 * this.data.orientation;
            hash += 162873468 * this.data.intdata;
            for(int i = 0; i < DISPENSER_DROPPER_ROWS
                    * DISPENSER_DROPPER_COLUMNS; i++)
            {
                hash += 1273648 * this.data.BlockCounts[i];
                if(this.data.BlockCounts[i] > 0)
                    hash += 3 * this.data.BlockTypes[i].hashCode();
            }
            return hash;
        case BTWool:
            return hash + 129347 * this.data.dyeColor.hashCode();
        }
        throw new UnsupportedOperationException();
    }

    /** @return the length of time that this block will fuel a furnace */
    public int getBurnTime()
    {
        return this.type.getBurnTime();
    }

    /** @param orientation
     *            the orientation for the side of the block clicked on
     * @param vieworientation
     *            the orientation for the direction the player is looking
     * @param forwardorientation
     *            the orientation for the direction the player is faceing
     * @return new block or null */
    public Block makePlacedBlock(final int orientation,
                                 final int vieworientation,
                                 final int forwardorientation)
    {
        switch(this.type)
        {
        case BTWood:
            return NewWood(treeGetTreeType(), orientation);
        case BTLeaves:
            return NewLeaves(treeGetTreeType());
        case BTPlank:
            return NewPlank(treeGetTreeType());
        case BTSapling:
            return NewSapling(treeGetTreeType());
        case BTRedstoneRepeaterOff:
        case BTRedstoneRepeaterOn:
            return BlockType.BTRedstoneRepeaterOff.make(orientation,
                                                        forwardorientation);
        case BTRedstoneComparator:
            return BlockType.BTRedstoneComparator.make(orientation,
                                                       forwardorientation);
        case BTWool:
            return NewWool(dyedGetDyeColor());
        default:
            return this.type.make(orientation, vieworientation);
        }
    }

    private boolean drawSolidDrawsAnything(final int bx,
                                           final int by,
                                           final int bz)
    {
        Block nx = world.getBlock(bx - 1, by, bz);
        if(nx != null && !nx.isOpaque())
            return true;
        Block px = world.getBlock(bx + 1, by, bz);
        if(px != null && !px.isOpaque())
            return true;
        Block ny = world.getBlock(bx, by - 1, bz);
        if(ny != null && !ny.isOpaque())
            return true;
        Block py = world.getBlock(bx, by + 1, bz);
        if(py != null && !py.isOpaque())
            return true;
        Block nz = world.getBlock(bx, by, bz - 1);
        if(nz != null && !nz.isOpaque())
            return true;
        Block pz = world.getBlock(bx, by, bz + 1);
        if(pz != null && !pz.isOpaque())
            return true;
        return false;
    }

    private boolean drawFluidDrawsAnything(final int bx,
                                           final int by,
                                           final int bz)
    {
        Block nx = world.getBlock(bx - 1, by, bz);
        Block px = world.getBlock(bx + 1, by, bz);
        Block ny = world.getBlock(bx, by - 1, bz);
        Block py = world.getBlock(bx, by + 1, bz);
        Block nz = world.getBlock(bx, by, bz - 1);
        Block pz = world.getBlock(bx, by, bz + 1);
        int drawMask = 0;
        float height = getHeight();
        if(drawFluidDrawsFace(nx, height))
            drawMask |= DMaskNX;
        if(drawFluidDrawsFace(px, height))
            drawMask |= DMaskPX;
        if(ny != null && !ny.isOpaque()
                && (ny.type != this.type || ny.getHeight() < 1.0f))
            drawMask |= DMaskNY;
        if(py != null && !py.isOpaque() && py.type != this.type)
            drawMask |= DMaskPY;
        if(drawFluidDrawsFace(nz, height))
            drawMask |= DMaskNZ;
        if(drawFluidDrawsFace(pz, height))
            drawMask |= DMaskPZ;
        if(height < 1.0f)
            drawMask |= DMaskPY;
        if(drawMask != 0)
            return true;
        return false;
    }

    /** @param bx
     *            the block's x coordinate
     * @param by
     *            the block's y coordinate
     * @param bz
     *            the block's z coordinate
     * @return if this block draws anything when draw is called */
    public boolean drawsAnything(final int bx, final int by, final int bz)
    {
        switch(this.type.drawType)
        {
        case BDTCustom:
            switch(this.type)
            {
            case BTFurnace:
                return drawSolidDrawsAnything(bx, by, bz);
            case BTRedstoneDustOff:
            case BTRedstoneDustOn:
            case BTDeleteBlock:
                return true;
            case BTWood:
                return true;// TODO finish
            case BTLeaves:
                if(Main.FancyGraphics)
                    return true;
                return drawSolidDrawsAnything(bx, by, bz);
            case BTLadder:
            case BTVines:
            case BTRedstoneRepeaterOff:
            case BTRedstoneRepeaterOn:
            case BTLever:
            case BTPiston:
            case BTStickyPiston:
            case BTPistonHead:
            case BTStickyPistonHead:
            case BTStonePressurePlate:
            case BTWoodPressurePlate:
            case BTSnow:
            case BTCocoa:
            case BTHopper:
                return true;
            case BTGrass:
            case BTDispenser:
            case BTDropper:
            case BTWool:
                return drawSolidDrawsAnything(bx, by, bz);
            default:
                return true;
            }
        case BDTItem:
            return true;
        case BDTSolid:
            return drawSolidDrawsAnything(bx, by, bz);
        case BDTNone:
            return false;
        case BDTTorch:
            return true;
        case BDTLiquid:
            return drawFluidDrawsAnything(bx, by, bz);
        case BDTButton:
        case BDTSolidAllSides:
        case BDTTool:
        case BDTSim3D:
            return true;
        }
        return true;
    }

    /** @author jacob */
    public static final class BlockDigDescriptor
    {
        /**
         * 
         */
        public final float digTime;
        /**
         * 
         */
        public final boolean makesBlock;
        /**
         * 
         */
        public final boolean usesTool;

        /** @param digTime
         *            the new <code>digTime</code>
         * @param makesBlock
         *            the new <code>makesBlock</code>
         * @param usesTool
         *            if the tool is used up */
        public BlockDigDescriptor(final float digTime,
                                  final boolean makesBlock,
                                  final boolean usesTool)
        {
            this.digTime = digTime;
            this.makesBlock = makesBlock;
            this.usesTool = usesTool;
        }

        /** @param digTime
         *            the new <code>digTime</code> */
        public BlockDigDescriptor(final float digTime)
        {
            this(digTime, true, true);
        }

        /** @param digTime
         *            the new <code>digTime</code>
         * @param makesBlock
         *            the new <code>makesBlock</code> */
        public BlockDigDescriptor(final float digTime, final boolean makesBlock)
        {
            this(digTime, makesBlock, true);
        }
    }

    /** @param toolType
     *            the tool type
     * @param toolLevel
     *            the tool level
     * @return the <code>BlockDigDescriptor</code> or <code>null</code> if this
     *         block can't be dug */
    public BlockDigDescriptor
        getDigDescriptor(final BlockType.ToolType toolType,
                         final BlockType.ToolLevel toolLevel)
    {
        switch(this.type)
        {
        case BTBedrock:
        case BTDeleteBlock:
        case BTEmpty:
        case BTLast:
        case BTMoon:
        case BTSun:
            return null;
        case BTBlazePowder:
        case BTBlazeRod:
        case BTCoal:
        case BTDiamond:
        case BTDiamondPick:
        case BTDiamondShovel:
        case BTEmerald:
        case BTGoldIngot:
        case BTGoldPick:
        case BTGoldShovel:
        case BTGunpowder:
        case BTIronIngot:
        case BTIronPick:
        case BTIronShovel:
        case BTLapisLazuli:
        case BTStonePick:
        case BTStoneShovel:
        case BTWoodPick:
        case BTWoodShovel:
        case BTStick:
        case BTSlime:
        case BTWoodAxe:
        case BTStoneAxe:
        case BTIronAxe:
        case BTGoldAxe:
        case BTDiamondAxe:
        case BTShears:
        case BTBucket:
        case BTQuartz:
        case BTString:
        case BTBow:
        case BTCactus:
        case BTRedMushroom:
        case BTBrownMushroom:
        case BTDandelion:
        case BTRose:
        case BTWheat:
        case BTTallGrass:
        case BTSeeds:
        case BTWoodHoe:
        case BTStoneHoe:
        case BTIronHoe:
        case BTGoldHoe:
        case BTDiamondHoe:
        case BTCocoa:
        case BTInkSac:
        case BTRoseRed:
        case BTCactusGreen:
        case BTPurpleDye:
        case BTCyanDye:
        case BTLightGrayDye:
        case BTGrayDye:
        case BTPinkDye:
        case BTLimeDye:
        case BTDandelionYellow:
        case BTLightBlueDye:
        case BTMagentaDye:
        case BTOrangeDye:
        case BTBoneMeal:
        case BTBone:
            return new BlockDigDescriptor(0.0f, true, true);
        case BTChest:
        case BTWorkbench:
            if(toolType == ToolType.Axe)
            {
                switch(toolLevel)
                {
                case Wood:
                    return new BlockDigDescriptor(1.9f);
                case Stone:
                    return new BlockDigDescriptor(0.95f);
                case Iron:
                    return new BlockDigDescriptor(0.65f);
                case Diamond:
                    return new BlockDigDescriptor(0.5f);
                case Gold:
                    return new BlockDigDescriptor(0.35f);
                default:
                    break;
                }
            }
            return new BlockDigDescriptor(3.75f, true, true);
        case BTCoalOre:
            if(toolType == ToolType.Pickaxe)
            {
                switch(toolLevel)
                {
                case Wood:
                    return new BlockDigDescriptor(2.25f);
                case Stone:
                    return new BlockDigDescriptor(1.15f);
                case Iron:
                    return new BlockDigDescriptor(0.75f);
                case Diamond:
                    return new BlockDigDescriptor(0.6f);
                case Gold:
                    return new BlockDigDescriptor(0.4f);
                default:
                    break;
                }
            }
            return new BlockDigDescriptor(15f, false, true);
        case BTCobblestone:
            if(toolType == ToolType.Pickaxe)
            {
                switch(toolLevel)
                {
                case Wood:
                    return new BlockDigDescriptor(1.5f);
                case Stone:
                    return new BlockDigDescriptor(0.75f);
                case Iron:
                    return new BlockDigDescriptor(0.5f);
                case Diamond:
                    return new BlockDigDescriptor(0.4f);
                case Gold:
                    return new BlockDigDescriptor(0.25f);
                default:
                    break;
                }
            }
            return new BlockDigDescriptor(10f, false, true);
        case BTDiamondOre:
        case BTEmeraldOre:
        case BTGoldOre:
        case BTRedstoneOre:
            if(toolType == ToolType.Pickaxe)
            {
                switch(toolLevel)
                {
                case Wood:
                    return new BlockDigDescriptor(15f, false, true);
                case Stone:
                    return new BlockDigDescriptor(15f, false, true);
                case Iron:
                    return new BlockDigDescriptor(0.75f);
                case Diamond:
                    return new BlockDigDescriptor(0.6f);
                case Gold:
                    return new BlockDigDescriptor(15f, false, true);
                default:
                    break;
                }
            }
            return new BlockDigDescriptor(15f, false, true);
        case BTHopper:
            if(toolType == ToolType.Pickaxe)
            {
                switch(toolLevel)
                {
                case Wood:
                    return new BlockDigDescriptor(2.25f, false, true);
                case Stone:
                    return new BlockDigDescriptor(1.15f, false, true);
                case Iron:
                    return new BlockDigDescriptor(0.75f);
                case Diamond:
                    return new BlockDigDescriptor(0.6f);
                case Gold:
                    return new BlockDigDescriptor(0.4f, false, true);
                default:
                    break;
                }
            }
            return new BlockDigDescriptor(15f, false, true);
        case BTDirt:
        case BTSand:
        case BTFarmland:
            if(toolType == ToolType.Shovel)
            {
                switch(toolLevel)
                {
                case Wood:
                    return new BlockDigDescriptor(0.4f);
                case Stone:
                    return new BlockDigDescriptor(0.2f);
                case Iron:
                    return new BlockDigDescriptor(0.15f);
                case Diamond:
                    return new BlockDigDescriptor(0.1f);
                case Gold:
                    return new BlockDigDescriptor(0.1f);
                default:
                    break;
                }
            }
            return new BlockDigDescriptor(0.75f, true, true);
        case BTGrass:
        case BTGravel:
            if(toolType == ToolType.Shovel)
            {
                switch(toolLevel)
                {
                case Wood:
                    return new BlockDigDescriptor(0.45f);
                case Stone:
                    return new BlockDigDescriptor(0.25f);
                case Iron:
                    return new BlockDigDescriptor(0.15f);
                case Diamond:
                    return new BlockDigDescriptor(0.15f);
                case Gold:
                    return new BlockDigDescriptor(0.1f);
                default:
                    break;
                }
            }
            return new BlockDigDescriptor(0.9f, true, true);
        case BTFurnace:
        case BTDispenser:
        case BTDropper:
            if(toolType == ToolType.Pickaxe)
            {
                switch(toolLevel)
                {
                case Wood:
                    return new BlockDigDescriptor(2.65f);
                case Stone:
                    return new BlockDigDescriptor(1.35f);
                case Iron:
                    return new BlockDigDescriptor(0.9f);
                case Diamond:
                    return new BlockDigDescriptor(0.7f);
                case Gold:
                    return new BlockDigDescriptor(0.45f);
                default:
                    break;
                }
            }
            return new BlockDigDescriptor(17.5f, true, true);
        case BTGlass:
            return new BlockDigDescriptor(0.3f, true, true);
        case BTIronOre:
        case BTLapisLazuliOre:
            if(toolType == ToolType.Pickaxe)
            {
                switch(toolLevel)
                {
                case Wood:
                    return new BlockDigDescriptor(15f, false, true);
                case Stone:
                    return new BlockDigDescriptor(1.15f);
                case Iron:
                    return new BlockDigDescriptor(0.75f);
                case Diamond:
                    return new BlockDigDescriptor(0.6f);
                case Gold:
                    return new BlockDigDescriptor(0.4f);
                default:
                    break;
                }
            }
            return new BlockDigDescriptor(15f, false, true);
        case BTLadder:
            return new BlockDigDescriptor(0.5f, true, true);
        case BTLava:
        case BTWater:
            return null;
        case BTCobweb:
            if(toolType == ToolType.Shears)
                return new BlockDigDescriptor(0.4f);
            return new BlockDigDescriptor(20f, false, true);
        case BTLeaves:
            if(toolType == ToolType.Shears)
            {
                return new BlockDigDescriptor(0.05f);
            }
            return new BlockDigDescriptor(0.3f, true, true);
        case BTLever:
            return new BlockDigDescriptor(0.5f, true, true);
        case BTObsidian:
            if(toolType == ToolType.Pickaxe)
            {
                switch(toolLevel)
                {
                case Wood:
                    return new BlockDigDescriptor(250f, false, true);
                case Stone:
                    return new BlockDigDescriptor(250f, false, true);
                case Iron:
                    return new BlockDigDescriptor(250f, false, true);
                case Diamond:
                    return new BlockDigDescriptor(9.4f);
                case Gold:
                    return new BlockDigDescriptor(250f, false, true);
                default:
                    break;
                }
            }
            return new BlockDigDescriptor(250f, false, true);
        case BTPiston:
        case BTStickyPiston:
            return new BlockDigDescriptor(0.5f, true, true);
        case BTPistonHead:
        case BTStickyPistonHead:
            return new BlockDigDescriptor(0.5f, true, true);
        case BTRedstoneBlock:
            if(toolType == ToolType.Pickaxe)
            {
                switch(toolLevel)
                {
                case Wood:
                    return new BlockDigDescriptor(3.75f);
                case Stone:
                    return new BlockDigDescriptor(1.9f);
                case Iron:
                    return new BlockDigDescriptor(1.25f);
                case Diamond:
                    return new BlockDigDescriptor(0.95f);
                case Gold:
                    return new BlockDigDescriptor(0.65f);
                default:
                    break;
                }
            }
            return new BlockDigDescriptor(25f, false, true);
        case BTRedstoneDustOff:
        case BTRedstoneDustOn:
            return new BlockDigDescriptor(0.0f, true, true);
        case BTRedstoneRepeaterOff:
        case BTRedstoneRepeaterOn:
        case BTRedstoneComparator:
            return new BlockDigDescriptor(0.5f, true, true);
        case BTRedstoneTorchOff:
        case BTRedstoneTorchOn:
            return new BlockDigDescriptor(0.0f, true, true);
        case BTSapling:
            return new BlockDigDescriptor(0.0f, true, true);
        case BTSnow:
            if(toolType == ToolType.Shovel)
            {
                switch(toolLevel)
                {
                case Wood:
                    return new BlockDigDescriptor(0.1f);
                case Stone:
                    return new BlockDigDescriptor(0.05f);
                case Iron:
                    return new BlockDigDescriptor(0.05f);
                case Diamond:
                    return new BlockDigDescriptor(0.05f);
                case Gold:
                    return new BlockDigDescriptor(0.05f);
                default:
                    break;
                }
            }
            return new BlockDigDescriptor(0.5f, false, true);
        case BTStone:
            if(toolType == ToolType.Pickaxe)
            {
                switch(toolLevel)
                {
                case Wood:
                    return new BlockDigDescriptor(1.15f);
                case Stone:
                    return new BlockDigDescriptor(0.6f);
                case Iron:
                    return new BlockDigDescriptor(0.4f);
                case Diamond:
                    return new BlockDigDescriptor(0.3f);
                case Gold:
                    return new BlockDigDescriptor(0.2f);
                default:
                    break;
                }
            }
            return new BlockDigDescriptor(7.5f, false, true);
        case BTStoneButton:
        case BTWoodButton:
            return new BlockDigDescriptor(0.5f, true, true);
        case BTStonePressurePlate:
            if(toolType == ToolType.Pickaxe)
            {
                switch(toolLevel)
                {
                case Wood:
                    return new BlockDigDescriptor(0.4f);
                case Stone:
                    return new BlockDigDescriptor(0.2f);
                case Iron:
                    return new BlockDigDescriptor(0.15f);
                case Diamond:
                    return new BlockDigDescriptor(0.1f);
                case Gold:
                    return new BlockDigDescriptor(0.1f);
                default:
                    break;
                }
            }
            return new BlockDigDescriptor(2.5f, false, true);
        case BTTNT:
        case BTTorch:
            return new BlockDigDescriptor(0.0f, true, true);
        case BTVines:
            if(toolType == ToolType.Axe)
            {
                switch(toolLevel)
                {
                case Wood:
                    return new BlockDigDescriptor(0.15f, true);
                case Stone:
                    return new BlockDigDescriptor(0.1f, true);
                case Iron:
                    return new BlockDigDescriptor(0.05f, true);
                case Diamond:
                    return new BlockDigDescriptor(0.05f, true);
                case Gold:
                    return new BlockDigDescriptor(0.05f, true);
                default:
                    break;
                }
            }
            else if(toolType == ToolType.Shears)
            {
                return new BlockDigDescriptor(0.3f);
            }
            return new BlockDigDescriptor(0.3f, false, true);
        case BTDeadBush:
            if(toolType == ToolType.Shears)
                return new BlockDigDescriptor(0);
            return new BlockDigDescriptor(0, false, true);
        case BTWood:
        case BTPlank:
            if(toolType == ToolType.Axe)
            {
                switch(toolLevel)
                {
                case Wood:
                    return new BlockDigDescriptor(1.5f);
                case Stone:
                    return new BlockDigDescriptor(0.75f);
                case Iron:
                    return new BlockDigDescriptor(0.5f);
                case Diamond:
                    return new BlockDigDescriptor(0.4f);
                case Gold:
                    return new BlockDigDescriptor(0.25f);
                default:
                    break;
                }
            }
            return new BlockDigDescriptor(3.0f, true, true);
        case BTWoodPressurePlate:
            if(toolType == ToolType.Axe)
            {
                switch(toolLevel)
                {
                case Wood:
                    return new BlockDigDescriptor(0.4f);
                case Stone:
                    return new BlockDigDescriptor(0.2f);
                case Iron:
                    return new BlockDigDescriptor(0.15f);
                case Diamond:
                    return new BlockDigDescriptor(0.1f);
                case Gold:
                    return new BlockDigDescriptor(0.1f);
                default:
                    break;
                }
            }
            return new BlockDigDescriptor(0.75f, true, true);
        case BTWool:
            if(toolType == ToolType.Shears)
                return new BlockDigDescriptor(0.25f, true, false);
            return new BlockDigDescriptor(1.2f);
        }
        return null;
    }

    /** @return the block's blast resistance */
    public float getBlastResistance()
    {
        if(this.type == BlockType.BTLava)
        {
            if(Math.abs(this.data.intdata) >= 8)
                return 500f;
            return 0f;
        }
        return this.type.getBlastResistance();
    }

    /** @return the tool type */
    public ToolType getToolType()
    {
        return this.type.getToolType();
    }

    /** @return the tool level */
    public ToolLevel getToolLevel()
    {
        return this.type.getToolLevel();
    }

    /** @return if this tool has any durability points left */
    public boolean toolUseTool()
    {
        this.data.intdata++;
        final int durability = toolGetMaxUseCount();
        if(this.data.intdata >= durability)
        {
            this.data.intdata = durability - 1;
            return false;
        }
        return true;
    }

    /** @return if this item is in a bucket */
    public boolean isItemInBucket()
    {
        return this.type.isItemInBucket();
    }

    /** @return if this block is replaceable */
    public boolean isReplaceable()
    {
        return this.type.isReplaceable();
    }

    public Block getItemInBucket()
    {
        if(this.type == BlockType.BTWater)
            return NewStationaryWater();
        if(this.type == BlockType.BTLava)
            return NewStationaryLava();
        return null;
    }

    private boolean
        checkItemHitSolidBlock(final float size, final Vector relpos)
    {
        float min = -size, max = 1 + size;
        if(relpos.getX() >= min && relpos.getX() <= max && relpos.getY() >= min
                && relpos.getY() <= max && relpos.getZ() >= min
                && relpos.getZ() <= max)
            return true;
        return false;
    }

    public boolean checkItemHit(final float size, final Vector relpos)
    {
        if(isSolid())
        {
            return checkItemHitSolidBlock(size, relpos);
        }
        return false;
    }

    public boolean pressurePlateIsItemPressing(final float size,
                                               final Vector relpos)
    {
        float min = -size, max = 1 + size;
        float ymax = 1 / 16f + size;
        if(relpos.getX() >= min && relpos.getX() <= max && relpos.getY() >= min
                && relpos.getY() <= ymax && relpos.getZ() >= min
                && relpos.getZ() <= max)
            return true;
        return false;
    }

    public int dispenserDropperGetBlockCount(final int row, final int column)
    {
        assert this.type == BlockType.BTDispenser
                || this.type == BlockType.BTDropper;
        if(row < 0 || row >= DISPENSER_DROPPER_ROWS)
            return 0;
        if(column < 0 || column >= DISPENSER_DROPPER_COLUMNS)
            return 0;
        int index = getDispenserDropperSlotIndex(row, column);
        return this.data.BlockCounts[index];
    }

    public Block dispenserDropperGetBlockType(final int row, final int column)
    {
        assert this.type == BlockType.BTDispenser
                || this.type == BlockType.BTDropper;
        if(row < 0 || row >= DISPENSER_DROPPER_ROWS)
            return null;
        if(column < 0 || column >= DISPENSER_DROPPER_COLUMNS)
            return null;
        int index = getDispenserDropperSlotIndex(row, column);
        return this.data.BlockTypes[index];
    }

    public int hopperGetBlockCount(final int slot)
    {
        assert this.type == BlockType.BTHopper;
        if(slot < 0 || slot >= HOPPER_SLOTS)
            return 0;
        return this.data.BlockCounts[slot];
    }

    public Block hopperGetBlockType(final int slot)
    {
        assert this.type == BlockType.BTHopper;
        if(slot < 0 || slot >= HOPPER_SLOTS)
            return null;
        return this.data.BlockTypes[slot];
    }

    public int getContainerLevel()
    {
        switch(this.type)
        {
        case BTChest:
        {
            float slotsTaken = 0;
            for(int row = 0; row < CHEST_ROWS; row++)
            {
                for(int column = 0; column < CHEST_COLUMNS; column++)
                {
                    Block b = chestGetBlockType(row, column);
                    if(b != null)
                    {
                        if(b.getToolType() != BlockType.ToolType.None
                                && b.toolGetUseCount() > 0)
                            slotsTaken += 1;
                        else
                            slotsTaken += (float)chestGetBlockCount(row, column)
                                    / BLOCK_STACK_SIZE;
                    }
                }
            }
            float v = slotsTaken / (CHEST_ROWS * CHEST_COLUMNS);
            if(v > 0)
                return Math.min((int)Math.floor(Math.max(1, v * 16)), 15);
            return 0;
        }
        case BTFurnace:
        {
            float v = (float)(this.data.destcount + this.data.srccount)
                    / BLOCK_STACK_SIZE / 2;
            if(v > 0)
                return Math.min((int)Math.floor(Math.max(1, v * 16)), 15);
            return 0;
        }
        case BTDispenser:
        case BTDropper:
        {
            float slotsTaken = 0;
            for(int row = 0; row < DISPENSER_DROPPER_ROWS; row++)
            {
                for(int column = 0; column < DISPENSER_DROPPER_COLUMNS; column++)
                {
                    Block b = dispenserDropperGetBlockType(row, column);
                    if(b != null)
                    {
                        if(b.getToolType() != BlockType.ToolType.None
                                && b.toolGetUseCount() > 0)
                            slotsTaken += 1;
                        else
                            slotsTaken += (float)dispenserDropperGetBlockCount(row,
                                                                               column)
                                    / BLOCK_STACK_SIZE;
                    }
                }
            }
            float v = slotsTaken
                    / (DISPENSER_DROPPER_ROWS * DISPENSER_DROPPER_COLUMNS);
            if(v > 0)
                return Math.min((int)Math.floor(Math.max(1, v * 16)), 15);
            return 0;
        }
        case BTHopper:
        {
            float slotsTaken = 0;
            for(int slot = 0; slot < HOPPER_SLOTS; slot++)
            {
                if(this.data.BlockCounts[slot] > 0)
                {
                    Block b = this.data.BlockTypes[slot];
                    if(b.getToolType() != BlockType.ToolType.None
                            && b.toolGetUseCount() > 0)
                        slotsTaken += 1;
                    else
                        slotsTaken += (float)this.data.BlockCounts[slot]
                                / BLOCK_STACK_SIZE;
                }
            }
            float v = slotsTaken / (HOPPER_SLOTS);
            if(v > 0)
                return Math.min((int)Math.floor(Math.max(1, v * 16)), 15);
            return 0;
        }
        case BTBedrock:
        case BTBlazePowder:
        case BTBlazeRod:
        case BTBucket:
        case BTCoal:
        case BTCoalOre:
        case BTCobblestone:
        case BTDeleteBlock:
        case BTDiamond:
        case BTDiamondAxe:
        case BTDiamondOre:
        case BTDiamondPick:
        case BTDiamondShovel:
        case BTDirt:
        case BTEmerald:
        case BTEmeraldOre:
        case BTEmpty:
        case BTGlass:
        case BTGoldAxe:
        case BTGoldIngot:
        case BTGoldOre:
        case BTGoldPick:
        case BTGoldShovel:
        case BTGrass:
        case BTGravel:
        case BTGunpowder:
        case BTIronAxe:
        case BTIronIngot:
        case BTIronOre:
        case BTIronPick:
        case BTIronShovel:
        case BTLadder:
        case BTLapisLazuli:
        case BTLapisLazuliOre:
        case BTLast:
        case BTLava:
        case BTLeaves:
        case BTLever:
        case BTMoon:
        case BTObsidian:
        case BTPiston:
        case BTPistonHead:
        case BTPlank:
        case BTRedstoneBlock:
        case BTRedstoneComparator:
        case BTRedstoneDustOff:
        case BTRedstoneDustOn:
        case BTRedstoneOre:
        case BTRedstoneRepeaterOff:
        case BTRedstoneRepeaterOn:
        case BTRedstoneTorchOff:
        case BTRedstoneTorchOn:
        case BTSand:
        case BTSapling:
        case BTShears:
        case BTSlime:
        case BTSnow:
        case BTStick:
        case BTStickyPiston:
        case BTStickyPistonHead:
        case BTStone:
        case BTStoneAxe:
        case BTStoneButton:
        case BTStonePick:
        case BTStonePressurePlate:
        case BTStoneShovel:
        case BTSun:
        case BTTNT:
        case BTTorch:
        case BTVines:
        case BTWater:
        case BTWood:
        case BTWoodAxe:
        case BTWoodButton:
        case BTWoodPick:
        case BTWoodPressurePlate:
        case BTWoodShovel:
        case BTWorkbench:
        case BTQuartz:
        case BTCobweb:
        case BTString:
        case BTBow:
        case BTCactus:
        case BTRedMushroom:
        case BTBrownMushroom:
        case BTDeadBush:
        case BTDandelion:
        case BTRose:
        case BTSeeds:
        case BTFarmland:
        case BTWheat:
        case BTTallGrass:
        case BTWoodHoe:
        case BTStoneHoe:
        case BTIronHoe:
        case BTGoldHoe:
        case BTDiamondHoe:
        case BTCocoa:
        case BTInkSac:
        case BTRoseRed:
        case BTCactusGreen:
        case BTPurpleDye:
        case BTCyanDye:
        case BTLightGrayDye:
        case BTGrayDye:
        case BTPinkDye:
        case BTLimeDye:
        case BTDandelionYellow:
        case BTLightBlueDye:
        case BTMagentaDye:
        case BTOrangeDye:
        case BTBoneMeal:
        case BTBone:
        case BTWool:
            return -1;
        }
        return -1;
    }

    public void redstoneComparatorToggleSubtractMode()
    {
        this.data.step = this.data.step != 0 ? 0 : 1;
    }

    public int dispenserDropperAddBlocks(final Block b,
                                         final int count,
                                         final int row,
                                         final int column)
    {
        assert (this.type == BlockType.BTDispenser || this.type == BlockType.BTDropper)
                && this.data.BlockCounts != null : "illegal block state";
        if(b == null || b.getType() == BlockType.BTEmpty)
            return count;
        if(row < 0 || row >= DISPENSER_DROPPER_ROWS || column < 0
                || column >= DISPENSER_DROPPER_COLUMNS || count <= 0)
            return 0;
        int index = getDispenserDropperSlotIndex(row, column);
        if(this.data.BlockCounts[index] >= BLOCK_STACK_SIZE)
            return 0;
        if(this.data.BlockCounts[index] <= 0)
        {
            this.data.BlockTypes[index] = b;
            if(count > BLOCK_STACK_SIZE)
            {
                this.data.BlockCounts[index] = BLOCK_STACK_SIZE;
                return BLOCK_STACK_SIZE;
            }
            this.data.BlockCounts[index] = count;
            return count;
        }
        if(!this.data.BlockTypes[index].equals(b))
            return 0;
        if(this.data.BlockCounts[index] + count > BLOCK_STACK_SIZE)
        {
            int retval = BLOCK_STACK_SIZE - this.data.BlockCounts[index];
            this.data.BlockCounts[index] = BLOCK_STACK_SIZE;
            return retval;
        }
        this.data.BlockCounts[index] += count;
        return count;
    }

    public int dispenserDropperRemoveBlocks(final Block b,
                                            final int count,
                                            final int row,
                                            final int column)
    {
        assert (this.type == BlockType.BTDispenser || this.type == BlockType.BTDropper)
                && this.data.BlockCounts != null : "illegal block state";
        if(b == null || b.getType() == BlockType.BTEmpty)
            return count;
        if(row < 0 || row >= DISPENSER_DROPPER_ROWS || column < 0
                || column >= DISPENSER_DROPPER_COLUMNS || count <= 0)
            return 0;
        int index = getDispenserDropperSlotIndex(row, column);
        if(this.data.BlockCounts[index] <= 0)
            return 0;
        if(!this.data.BlockTypes[index].equals(b))
            return 0;
        if(this.data.BlockCounts[index] <= count)
        {
            int retval = this.data.BlockCounts[index];
            this.data.BlockCounts[index] = 0;
            this.data.BlockTypes[index] = null;
            return retval;
        }
        this.data.BlockCounts[index] -= count;
        return count;
    }

    public int
        hopperRemoveBlocks(final Block b, final int count, final int slot)
    {
        assert this.type == BlockType.BTHopper && this.data.BlockCounts != null : "illegal block state";
        if(b == null || b.getType() == BlockType.BTEmpty)
            return count;
        if(slot < 0 || slot >= HOPPER_SLOTS)
            return 0;
        int index = slot;
        if(this.data.BlockCounts[index] <= 0)
            return 0;
        if(!this.data.BlockTypes[index].equals(b))
            return 0;
        if(this.data.BlockCounts[index] <= count)
        {
            int retval = this.data.BlockCounts[index];
            this.data.BlockCounts[index] = 0;
            this.data.BlockTypes[index] = null;
            return retval;
        }
        this.data.BlockCounts[index] -= count;
        return count;
    }

    public boolean isContainer()
    {
        switch(this.type)
        {
        case BTDeleteBlock:
        case BTLast:
        case BTMoon:
        case BTSun:
            return false;
        case BTEmpty:
        case BTBedrock:
            return false;
        case BTBlazePowder:
        case BTBlazeRod:
        case BTBow:
        case BTBucket:
        case BTCoal:
        case BTCoalOre:
        case BTCobblestone:
        case BTCobweb:
        case BTDiamond:
        case BTDiamondAxe:
        case BTDiamondOre:
        case BTDiamondPick:
        case BTDiamondShovel:
        case BTDirt:
        case BTEmerald:
        case BTEmeraldOre:
        case BTGlass:
        case BTGoldAxe:
        case BTGoldIngot:
        case BTGoldOre:
        case BTGoldPick:
        case BTGoldShovel:
        case BTGrass:
        case BTGravel:
        case BTGunpowder:
        case BTIronAxe:
        case BTIronIngot:
        case BTIronOre:
        case BTIronPick:
        case BTIronShovel:
        case BTLadder:
        case BTLapisLazuli:
        case BTLapisLazuliOre:
        case BTLava:
        case BTLeaves:
        case BTLever:
        case BTObsidian:
        case BTPiston:
        case BTPistonHead:
        case BTPlank:
        case BTQuartz:
        case BTRedstoneBlock:
        case BTRedstoneComparator:
        case BTRedstoneDustOff:
        case BTRedstoneDustOn:
        case BTRedstoneOre:
        case BTRedstoneRepeaterOff:
        case BTRedstoneRepeaterOn:
        case BTRedstoneTorchOff:
        case BTRedstoneTorchOn:
        case BTSand:
        case BTSapling:
        case BTShears:
        case BTSlime:
        case BTSnow:
        case BTStick:
        case BTStickyPiston:
        case BTStickyPistonHead:
        case BTStone:
        case BTStoneAxe:
        case BTStoneButton:
        case BTStonePick:
        case BTStonePressurePlate:
        case BTStoneShovel:
        case BTString:
        case BTTNT:
        case BTTorch:
        case BTVines:
        case BTWater:
        case BTWood:
        case BTWoodAxe:
        case BTWoodButton:
        case BTWoodPick:
        case BTWoodPressurePlate:
        case BTWoodShovel:
        case BTWorkbench:
        case BTCactus:
        case BTRedMushroom:
        case BTBrownMushroom:
        case BTDeadBush:
        case BTWoodHoe:
        case BTStoneHoe:
        case BTIronHoe:
        case BTGoldHoe:
        case BTDiamondHoe:
        case BTDandelion:
        case BTRose:
        case BTSeeds:
        case BTFarmland:
        case BTWheat:
        case BTTallGrass:
        case BTCocoa:
        case BTInkSac:
        case BTRoseRed:
        case BTCactusGreen:
        case BTPurpleDye:
        case BTCyanDye:
        case BTLightGrayDye:
        case BTGrayDye:
        case BTPinkDye:
        case BTLimeDye:
        case BTDandelionYellow:
        case BTLightBlueDye:
        case BTMagentaDye:
        case BTOrangeDye:
        case BTBoneMeal:
        case BTBone:
        case BTWool:
            return false;
        case BTChest:
        case BTDispenser:
        case BTDropper:
        case BTFurnace:
        case BTHopper:
            return true;
        }
        return false;
    }

    /** @param b
     *            the block to add
     * @param addFromOrientation
     *            the side that the block is added from
     * @return if the block can be added */
    public boolean addBlockToContainer(final Block b,
                                       final int addFromOrientation)
    {
        if(b == null || b.getType() == BlockType.BTEmpty)
            return false;
        switch(this.type)
        {
        case BTDeleteBlock:
        case BTLast:
        case BTMoon:
        case BTSun:
            return false;
        case BTEmpty:
        case BTBedrock:
            return false;
        case BTBlazePowder:
        case BTBlazeRod:
        case BTBow:
        case BTBucket:
        case BTCoal:
        case BTCoalOre:
        case BTCobblestone:
        case BTCobweb:
        case BTDiamond:
        case BTDiamondAxe:
        case BTDiamondOre:
        case BTDiamondPick:
        case BTDiamondShovel:
        case BTDirt:
        case BTEmerald:
        case BTEmeraldOre:
        case BTGlass:
        case BTGoldAxe:
        case BTGoldIngot:
        case BTGoldOre:
        case BTGoldPick:
        case BTGoldShovel:
        case BTGrass:
        case BTGravel:
        case BTGunpowder:
        case BTIronAxe:
        case BTIronIngot:
        case BTIronOre:
        case BTIronPick:
        case BTIronShovel:
        case BTLadder:
        case BTLapisLazuli:
        case BTLapisLazuliOre:
        case BTLava:
        case BTLeaves:
        case BTLever:
        case BTObsidian:
        case BTPiston:
        case BTPistonHead:
        case BTPlank:
        case BTQuartz:
        case BTRedstoneBlock:
        case BTRedstoneComparator:
        case BTRedstoneDustOff:
        case BTRedstoneDustOn:
        case BTRedstoneOre:
        case BTRedstoneRepeaterOff:
        case BTRedstoneRepeaterOn:
        case BTRedstoneTorchOff:
        case BTRedstoneTorchOn:
        case BTSand:
        case BTSapling:
        case BTShears:
        case BTSlime:
        case BTSnow:
        case BTStick:
        case BTStickyPiston:
        case BTStickyPistonHead:
        case BTStone:
        case BTStoneAxe:
        case BTStoneButton:
        case BTStonePick:
        case BTStonePressurePlate:
        case BTStoneShovel:
        case BTString:
        case BTTNT:
        case BTTorch:
        case BTVines:
        case BTWater:
        case BTWood:
        case BTWoodAxe:
        case BTWoodButton:
        case BTWoodPick:
        case BTWoodPressurePlate:
        case BTWoodShovel:
        case BTWorkbench:
        case BTCactus:
        case BTRedMushroom:
        case BTBrownMushroom:
        case BTDeadBush:
        case BTWoodHoe:
        case BTStoneHoe:
        case BTIronHoe:
        case BTGoldHoe:
        case BTDiamondHoe:
        case BTDandelion:
        case BTRose:
        case BTSeeds:
        case BTFarmland:
        case BTWheat:
        case BTTallGrass:
        case BTCocoa:
        case BTInkSac:
        case BTRoseRed:
        case BTCactusGreen:
        case BTPurpleDye:
        case BTCyanDye:
        case BTLightGrayDye:
        case BTGrayDye:
        case BTPinkDye:
        case BTLimeDye:
        case BTDandelionYellow:
        case BTLightBlueDye:
        case BTMagentaDye:
        case BTOrangeDye:
        case BTBoneMeal:
        case BTBone:
        case BTWool:
            return false;
        case BTChest:
        {
            for(int row = 0; row < CHEST_ROWS; row++)
            {
                for(int column = 0; column < CHEST_COLUMNS; column++)
                {
                    if(chestAddBlocks(b, 1, row, column) > 0)
                        return true;
                }
            }
            return false;
        }
        case BTDispenser:
        case BTDropper:
        {
            for(int row = 0; row < DISPENSER_DROPPER_ROWS; row++)
            {
                for(int column = 0; column < DISPENSER_DROPPER_COLUMNS; column++)
                {
                    if(dispenserDropperAddBlocks(b, 1, row, column) > 0)
                        return true;
                }
            }
            return false;
        }
        case BTFurnace:
        {
            switch(Block.getOrientationDY(addFromOrientation))
            {
            case 0:
            {
                int burnTime = b.getBurnTime();
                if(burnTime <= 0)
                    return false;
                furnaceAddFire(b);
                return true;
            }
            case 1:
                return furnaceAddBlock(b);
            case -1:
            default:
                return false;
            }
        }
        case BTHopper:
        {
            for(int slot = 0; slot < HOPPER_SLOTS; slot++)
                if(hopperAddBlocks(b, 1, slot) > 0)
                    return true;
            return false;
        }
        }
        return false;
    }

    public int hopperAddBlocks(final Block b, final int count, final int slot)
    {
        assert this.type == BlockType.BTHopper;
        if(slot < 0 || slot >= HOPPER_SLOTS || b == null || count <= 0)
            return 0;
        final int index = slot;
        if(this.data.BlockCounts[index] >= BLOCK_STACK_SIZE)
            return 0;
        if(this.data.BlockCounts[index] <= 0)
        {
            this.data.BlockTypes[index] = b;
            if(count > BLOCK_STACK_SIZE)
            {
                this.data.BlockCounts[index] = BLOCK_STACK_SIZE;
                return BLOCK_STACK_SIZE;
            }
            this.data.BlockCounts[index] = count;
            return count;
        }
        if(!this.data.BlockTypes[index].equals(b))
            return 0;
        if(this.data.BlockCounts[index] + count > BLOCK_STACK_SIZE)
        {
            int retval = BLOCK_STACK_SIZE - this.data.BlockCounts[index];
            this.data.BlockCounts[index] = BLOCK_STACK_SIZE;
            return retval;
        }
        this.data.BlockCounts[index] += count;
        return count;
    }

    /** @param removeFromOrientation
     *            the side that the block is removed from
     * @return the block removal descriptor */
    public int
        makeRemoveBlockFromContainerDescriptor(final int removeFromOrientation)
    {
        switch(this.type)
        {
        case BTDeleteBlock:
        case BTLast:
        case BTMoon:
        case BTSun:
            return -1;
        case BTEmpty:
        case BTBedrock:
            return -1;
        case BTBlazePowder:
        case BTBlazeRod:
        case BTBow:
        case BTBucket:
        case BTCoal:
        case BTCoalOre:
        case BTCobblestone:
        case BTCobweb:
        case BTDiamond:
        case BTDiamondAxe:
        case BTDiamondOre:
        case BTDiamondPick:
        case BTDiamondShovel:
        case BTDirt:
        case BTEmerald:
        case BTEmeraldOre:
        case BTGlass:
        case BTGoldAxe:
        case BTGoldIngot:
        case BTGoldOre:
        case BTGoldPick:
        case BTGoldShovel:
        case BTGrass:
        case BTGravel:
        case BTGunpowder:
        case BTIronAxe:
        case BTIronIngot:
        case BTIronOre:
        case BTIronPick:
        case BTIronShovel:
        case BTLadder:
        case BTLapisLazuli:
        case BTLapisLazuliOre:
        case BTLava:
        case BTLeaves:
        case BTLever:
        case BTObsidian:
        case BTPiston:
        case BTPistonHead:
        case BTPlank:
        case BTQuartz:
        case BTRedstoneBlock:
        case BTRedstoneComparator:
        case BTRedstoneDustOff:
        case BTRedstoneDustOn:
        case BTRedstoneOre:
        case BTRedstoneRepeaterOff:
        case BTRedstoneRepeaterOn:
        case BTRedstoneTorchOff:
        case BTRedstoneTorchOn:
        case BTSand:
        case BTSapling:
        case BTShears:
        case BTSlime:
        case BTSnow:
        case BTStick:
        case BTStickyPiston:
        case BTStickyPistonHead:
        case BTStone:
        case BTStoneAxe:
        case BTStoneButton:
        case BTStonePick:
        case BTStonePressurePlate:
        case BTStoneShovel:
        case BTString:
        case BTTNT:
        case BTTorch:
        case BTVines:
        case BTWater:
        case BTWood:
        case BTWoodAxe:
        case BTWoodButton:
        case BTWoodPick:
        case BTWoodPressurePlate:
        case BTWoodShovel:
        case BTWorkbench:
        case BTCactus:
        case BTRedMushroom:
        case BTBrownMushroom:
        case BTDeadBush:
        case BTWoodHoe:
        case BTStoneHoe:
        case BTIronHoe:
        case BTGoldHoe:
        case BTDiamondHoe:
        case BTDandelion:
        case BTRose:
        case BTSeeds:
        case BTFarmland:
        case BTWheat:
        case BTTallGrass:
        case BTCocoa:
        case BTInkSac:
        case BTRoseRed:
        case BTCactusGreen:
        case BTPurpleDye:
        case BTCyanDye:
        case BTLightGrayDye:
        case BTGrayDye:
        case BTPinkDye:
        case BTLimeDye:
        case BTDandelionYellow:
        case BTLightBlueDye:
        case BTMagentaDye:
        case BTOrangeDye:
        case BTBoneMeal:
        case BTBone:
        case BTWool:
            return -1;
        case BTChest:
        {
            int occupiedCount = 0;
            for(int row = 0; row < CHEST_ROWS; row++)
            {
                for(int column = 0; column < CHEST_COLUMNS; column++)
                {
                    if(chestGetBlockCount(row, column) > 0)
                        occupiedCount++;
                }
            }
            if(occupiedCount <= 0)
                return -1;
            int removePos = Math.min(occupiedCount - 1,
                                     (int)Math.floor(World.fRand(0,
                                                                 occupiedCount)));
            return removePos;
        }
        case BTDispenser:
        case BTDropper:
        {
            int occupiedCount = 0;
            for(int row = 0; row < DISPENSER_DROPPER_ROWS; row++)
            {
                for(int column = 0; column < DISPENSER_DROPPER_COLUMNS; column++)
                {
                    if(dispenserDropperGetBlockCount(row, column) > 0)
                        occupiedCount++;
                }
            }
            if(occupiedCount <= 0)
                return -1;
            return Math.min(occupiedCount - 1,
                            (int)Math.floor(World.fRand(0, occupiedCount)));
        }
        case BTFurnace:
        {
            if(Block.getOrientationDY(removeFromOrientation) != -1)
                return -1;
            if(furnaceGetDestBlockCount() > 0)
                return 0;
            return -1;
        }
        case BTHopper:
        {
            int occupiedCount = 0;
            for(int slot = 0; slot < HOPPER_SLOTS; slot++)
            {
                if(hopperGetBlockCount(slot) > 0)
                    occupiedCount++;
            }
            if(occupiedCount <= 0)
                return -1;
            return Math.min(occupiedCount - 1,
                            (int)Math.floor(World.fRand(0, occupiedCount)));
        }
        }
        return -1;
    }

    /** @param removeFromOrientation
     *            the side that the block is removed from
     * @param removeDescriptor
     *            the block removal descriptor
     * @return the removed block or null */
    public Block getRemovedBlockFromContainer(final int removeFromOrientation,
                                              final int removeDescriptor)
    {
        if(removeDescriptor < 0)
            return null;
        switch(this.type)
        {
        case BTDeleteBlock:
        case BTLast:
        case BTMoon:
        case BTSun:
            return null;
        case BTEmpty:
        case BTBedrock:
            return null;
        case BTBlazePowder:
        case BTBlazeRod:
        case BTBow:
        case BTBucket:
        case BTCoal:
        case BTCoalOre:
        case BTCobblestone:
        case BTCobweb:
        case BTDiamond:
        case BTDiamondAxe:
        case BTDiamondOre:
        case BTDiamondPick:
        case BTDiamondShovel:
        case BTDirt:
        case BTEmerald:
        case BTEmeraldOre:
        case BTGlass:
        case BTGoldAxe:
        case BTGoldIngot:
        case BTGoldOre:
        case BTGoldPick:
        case BTGoldShovel:
        case BTGrass:
        case BTGravel:
        case BTGunpowder:
        case BTIronAxe:
        case BTIronIngot:
        case BTIronOre:
        case BTIronPick:
        case BTIronShovel:
        case BTLadder:
        case BTLapisLazuli:
        case BTLapisLazuliOre:
        case BTLava:
        case BTLeaves:
        case BTLever:
        case BTObsidian:
        case BTPiston:
        case BTPistonHead:
        case BTPlank:
        case BTQuartz:
        case BTRedstoneBlock:
        case BTRedstoneComparator:
        case BTRedstoneDustOff:
        case BTRedstoneDustOn:
        case BTRedstoneOre:
        case BTRedstoneRepeaterOff:
        case BTRedstoneRepeaterOn:
        case BTRedstoneTorchOff:
        case BTRedstoneTorchOn:
        case BTSand:
        case BTSapling:
        case BTShears:
        case BTSlime:
        case BTSnow:
        case BTStick:
        case BTStickyPiston:
        case BTStickyPistonHead:
        case BTStone:
        case BTStoneAxe:
        case BTStoneButton:
        case BTStonePick:
        case BTStonePressurePlate:
        case BTStoneShovel:
        case BTString:
        case BTTNT:
        case BTTorch:
        case BTVines:
        case BTWater:
        case BTWood:
        case BTWoodAxe:
        case BTWoodButton:
        case BTWoodPick:
        case BTWoodPressurePlate:
        case BTWoodShovel:
        case BTWorkbench:
        case BTCactus:
        case BTRedMushroom:
        case BTBrownMushroom:
        case BTDeadBush:
        case BTWoodHoe:
        case BTStoneHoe:
        case BTIronHoe:
        case BTGoldHoe:
        case BTDiamondHoe:
        case BTDandelion:
        case BTRose:
        case BTSeeds:
        case BTFarmland:
        case BTWheat:
        case BTTallGrass:
        case BTCocoa:
        case BTInkSac:
        case BTRoseRed:
        case BTCactusGreen:
        case BTPurpleDye:
        case BTCyanDye:
        case BTLightGrayDye:
        case BTGrayDye:
        case BTPinkDye:
        case BTLimeDye:
        case BTDandelionYellow:
        case BTLightBlueDye:
        case BTMagentaDye:
        case BTOrangeDye:
        case BTBoneMeal:
        case BTBone:
        case BTWool:
            return null;
        case BTChest:
        {
            int occupiedCount = 0;
            for(int row = 0; row < CHEST_ROWS; row++)
            {
                for(int column = 0; column < CHEST_COLUMNS; column++)
                {
                    if(chestGetBlockCount(row, column) > 0)
                        occupiedCount++;
                }
            }
            if(occupiedCount <= 0)
                return null;
            if(removeDescriptor >= occupiedCount)
                return null;
            int removePos = removeDescriptor;
            int index = 0;
            for(int row = 0; row < CHEST_ROWS; row++)
            {
                for(int column = 0; column < CHEST_COLUMNS; column++)
                {
                    if(chestGetBlockCount(row, column) > 0)
                    {
                        if(index == removePos)
                        {
                            Block retval = chestGetBlockType(row, column);
                            return retval;
                        }
                        index++;
                    }
                }
            }
            return null;
        }
        case BTDispenser:
        case BTDropper:
        {
            int occupiedCount = 0;
            for(int row = 0; row < DISPENSER_DROPPER_ROWS; row++)
            {
                for(int column = 0; column < DISPENSER_DROPPER_COLUMNS; column++)
                {
                    if(dispenserDropperGetBlockCount(row, column) > 0)
                        occupiedCount++;
                }
            }
            if(occupiedCount <= 0)
                return null;
            if(removeDescriptor >= occupiedCount)
                return null;
            int removePos = removeDescriptor;
            int index = 0;
            for(int row = 0; row < DISPENSER_DROPPER_ROWS; row++)
            {
                for(int column = 0; column < DISPENSER_DROPPER_COLUMNS; column++)
                {
                    if(dispenserDropperGetBlockCount(row, column) > 0)
                    {
                        if(index == removePos)
                        {
                            Block retval = dispenserDropperGetBlockType(row,
                                                                        column);
                            return retval;
                        }
                        index++;
                    }
                }
            }
            return null;
        }
        case BTFurnace:
        {
            if(Block.getOrientationDY(removeFromOrientation) != -1)
                return null;
            if(furnaceGetDestBlockCount() > 0)
                return furnaceGetDestBlock();
            return null;
        }
        case BTHopper:
        {
            int occupiedCount = 0;
            for(int slot = 0; slot < HOPPER_SLOTS; slot++)
            {
                if(hopperGetBlockCount(slot) > 0)
                    occupiedCount++;
            }
            if(occupiedCount <= 0)
                return null;
            if(removeDescriptor >= occupiedCount)
                return null;
            int removePos = removeDescriptor;
            int index = 0;
            for(int slot = 0; slot < HOPPER_SLOTS; slot++)
            {
                if(hopperGetBlockCount(slot) > 0)
                {
                    if(index == removePos)
                    {
                        Block retval = hopperGetBlockType(slot);
                        return retval;
                    }
                    index++;
                }
            }
            return null;
        }
        }
        return null;
    }

    /** @param removeFromOrientation
     *            the side that the block is removed from
     * @param removeDescriptor
     *            the block removal descriptor
     * @return the removed block or null */
    public Block removeBlockFromContainer(final int removeFromOrientation,
                                          final int removeDescriptor)
    {
        if(removeDescriptor < 0)
            return null;
        switch(this.type)
        {
        case BTDeleteBlock:
        case BTLast:
        case BTMoon:
        case BTSun:
            return null;
        case BTEmpty:
        case BTBedrock:
            return null;
        case BTBlazePowder:
        case BTBlazeRod:
        case BTBow:
        case BTBucket:
        case BTCoal:
        case BTCoalOre:
        case BTCobblestone:
        case BTCobweb:
        case BTDiamond:
        case BTDiamondAxe:
        case BTDiamondOre:
        case BTDiamondPick:
        case BTDiamondShovel:
        case BTDirt:
        case BTEmerald:
        case BTEmeraldOre:
        case BTGlass:
        case BTGoldAxe:
        case BTGoldIngot:
        case BTGoldOre:
        case BTGoldPick:
        case BTGoldShovel:
        case BTGrass:
        case BTGravel:
        case BTGunpowder:
        case BTIronAxe:
        case BTIronIngot:
        case BTIronOre:
        case BTIronPick:
        case BTIronShovel:
        case BTLadder:
        case BTLapisLazuli:
        case BTLapisLazuliOre:
        case BTLava:
        case BTLeaves:
        case BTLever:
        case BTObsidian:
        case BTPiston:
        case BTPistonHead:
        case BTPlank:
        case BTQuartz:
        case BTRedstoneBlock:
        case BTRedstoneComparator:
        case BTRedstoneDustOff:
        case BTRedstoneDustOn:
        case BTRedstoneOre:
        case BTRedstoneRepeaterOff:
        case BTRedstoneRepeaterOn:
        case BTRedstoneTorchOff:
        case BTRedstoneTorchOn:
        case BTSand:
        case BTSapling:
        case BTShears:
        case BTSlime:
        case BTSnow:
        case BTStick:
        case BTStickyPiston:
        case BTStickyPistonHead:
        case BTStone:
        case BTStoneAxe:
        case BTStoneButton:
        case BTStonePick:
        case BTStonePressurePlate:
        case BTStoneShovel:
        case BTString:
        case BTTNT:
        case BTTorch:
        case BTVines:
        case BTWater:
        case BTWood:
        case BTWoodAxe:
        case BTWoodButton:
        case BTWoodPick:
        case BTWoodPressurePlate:
        case BTWoodShovel:
        case BTWorkbench:
        case BTCactus:
        case BTRedMushroom:
        case BTBrownMushroom:
        case BTDeadBush:
        case BTWoodHoe:
        case BTStoneHoe:
        case BTIronHoe:
        case BTGoldHoe:
        case BTDiamondHoe:
        case BTDandelion:
        case BTRose:
        case BTSeeds:
        case BTFarmland:
        case BTWheat:
        case BTTallGrass:
        case BTCocoa:
        case BTInkSac:
        case BTRoseRed:
        case BTCactusGreen:
        case BTPurpleDye:
        case BTCyanDye:
        case BTLightGrayDye:
        case BTGrayDye:
        case BTPinkDye:
        case BTLimeDye:
        case BTDandelionYellow:
        case BTLightBlueDye:
        case BTMagentaDye:
        case BTOrangeDye:
        case BTBoneMeal:
        case BTBone:
        case BTWool:
            return null;
        case BTChest:
        {
            int occupiedCount = 0;
            for(int row = 0; row < CHEST_ROWS; row++)
            {
                for(int column = 0; column < CHEST_COLUMNS; column++)
                {
                    if(chestGetBlockCount(row, column) > 0)
                        occupiedCount++;
                }
            }
            if(occupiedCount <= 0)
                return null;
            if(removeDescriptor >= occupiedCount)
                return null;
            int removePos = removeDescriptor;
            int index = 0;
            for(int row = 0; row < CHEST_ROWS; row++)
            {
                for(int column = 0; column < CHEST_COLUMNS; column++)
                {
                    if(chestGetBlockCount(row, column) > 0)
                    {
                        if(index == removePos)
                        {
                            Block retval = chestGetBlockType(row, column);
                            if(chestRemoveBlocks(retval, 1, row, column) > 0)
                                return retval;
                            break;
                        }
                        index++;
                    }
                }
            }
            return null;
        }
        case BTDispenser:
        case BTDropper:
        {
            int occupiedCount = 0;
            for(int row = 0; row < DISPENSER_DROPPER_ROWS; row++)
            {
                for(int column = 0; column < DISPENSER_DROPPER_COLUMNS; column++)
                {
                    if(dispenserDropperGetBlockCount(row, column) > 0)
                        occupiedCount++;
                }
            }
            if(occupiedCount <= 0)
                return null;
            if(removeDescriptor >= occupiedCount)
                return null;
            int removePos = removeDescriptor;
            int index = 0;
            for(int row = 0; row < DISPENSER_DROPPER_ROWS; row++)
            {
                for(int column = 0; column < DISPENSER_DROPPER_COLUMNS; column++)
                {
                    if(dispenserDropperGetBlockCount(row, column) > 0)
                    {
                        if(index == removePos)
                        {
                            Block retval = dispenserDropperGetBlockType(row,
                                                                        column);
                            if(dispenserDropperRemoveBlocks(retval,
                                                            1,
                                                            row,
                                                            column) > 0)
                                return retval;
                            break;
                        }
                        index++;
                    }
                }
            }
            return null;
        }
        case BTFurnace:
        {
            if(Block.getOrientationDY(removeFromOrientation) != -1)
                return null;
            return furnaceRemoveBlock();
        }
        case BTHopper:
        {
            int occupiedCount = 0;
            for(int slot = 0; slot < HOPPER_SLOTS; slot++)
            {
                if(hopperGetBlockCount(slot) > 0)
                    occupiedCount++;
            }
            if(occupiedCount <= 0)
                return null;
            if(removeDescriptor >= occupiedCount)
                return null;
            int removePos = removeDescriptor;
            int index = 0;
            for(int slot = 0; slot < HOPPER_SLOTS; slot++)
            {
                if(hopperGetBlockCount(slot) > 0)
                {
                    if(index == removePos)
                    {
                        Block retval = hopperGetBlockType(slot);
                        if(hopperRemoveBlocks(retval, 1, slot) > 0)
                            return retval;
                        break;
                    }
                    index++;
                }
            }
            return null;
        }
        }
        return null;
    }

    public boolean hopperIsActive()
    {
        return this.data.intdata == 0;
    }

    public boolean onUseBoneMeal(final int bx, final int by, final int bz)
    {
        switch(this.type)
        {
        case BTDeleteBlock:
        case BTSun:
        case BTMoon:
        case BTLast:
            break;
        case BTBedrock:
        case BTEmpty:
            break;
        case BTBlazePowder:
        case BTBlazeRod:
        case BTBone:
        case BTBoneMeal:
        case BTBow:
            break;
        case BTBrownMushroom:
        case BTRedMushroom:
            // TODO finish
            break;
        case BTBucket:
        case BTCactus:
        case BTCactusGreen:
        case BTChest:
        case BTCoal:
        case BTCoalOre:
        case BTCobblestone:
        case BTCobweb:
            break;
        case BTCocoa:
        {
            Block b = new Block(this);
            b.data.intdata = 2;
            world.setBlock(bx, by, bz, b);
            return true;
        }
        case BTCyanDye:
        case BTDandelion:
        case BTDandelionYellow:
        case BTDeadBush:
        case BTDiamond:
        case BTDiamondAxe:
        case BTDiamondHoe:
        case BTDiamondOre:
        case BTDiamondPick:
        case BTDiamondShovel:
        case BTDirt:
        case BTDispenser:
        case BTDropper:
        case BTEmerald:
        case BTEmeraldOre:
        case BTFarmland:
        case BTFurnace:
        case BTGlass:
            break;
        case BTGoldAxe:
        case BTGoldHoe:
        case BTGoldIngot:
        case BTGoldOre:
        case BTGoldPick:
        case BTGoldShovel:
            break;
        case BTGrass:
        {
            for(int dx = -5; dx <= 5; dx++)
            {
                for(int dy = -3; dy <= 3; dy++)
                {
                    for(int dz = -5; dz <= 5; dz++)
                    {
                        int x = bx + dx, y = by + dy, z = bz + dz;
                        Block b = world.getBlockEval(x, y - 1, z);
                        if(b != null && b.getType() == BlockType.BTGrass)
                        {
                            b = world.getBlockEval(x, y, z);
                            if(b != null && b.getType() == BlockType.BTEmpty)
                            {
                                final float tallGrassProb = 8 * 7 / 300f;
                                final float roseProb = 4 / 300f;
                                final float dandelionProb = 2 / 300f;
                                float randV = World.fRand(0, 1);
                                if(randV <= roseProb)
                                    world.setBlock(x, y, z, NewRose());
                                else if(randV <= roseProb + dandelionProb)
                                    world.setBlock(x, y, z, NewDandelion());
                                else if(randV <= roseProb + dandelionProb
                                        + tallGrassProb)
                                    world.setBlock(x, y, z, NewTallGrass());
                            }
                        }
                    }
                }
            }
            return true;
        }
        case BTGravel:
        case BTGrayDye:
        case BTGunpowder:
        case BTHopper:
        case BTInkSac:
        case BTIronAxe:
        case BTIronHoe:
        case BTIronIngot:
        case BTIronOre:
        case BTIronPick:
        case BTIronShovel:
        case BTLadder:
        case BTLapisLazuli:
        case BTLapisLazuliOre:
        case BTLava:
        case BTLeaves:
        case BTLever:
        case BTLightBlueDye:
        case BTLightGrayDye:
        case BTLimeDye:
        case BTMagentaDye:
        case BTObsidian:
        case BTOrangeDye:
        case BTPinkDye:
        case BTPiston:
        case BTPistonHead:
        case BTPlank:
        case BTPurpleDye:
        case BTQuartz:
        case BTRedstoneBlock:
        case BTRedstoneComparator:
        case BTRedstoneDustOff:
        case BTRedstoneDustOn:
        case BTRedstoneOre:
        case BTRedstoneRepeaterOff:
        case BTRedstoneRepeaterOn:
        case BTRedstoneTorchOff:
        case BTRedstoneTorchOn:
        case BTRose:
        case BTRoseRed:
        case BTSand:
            break;
        case BTSapling:
        {
            Block b = moveRandom(bx, by, bz);
            if(b != null)
                world.setBlock(bx, by, bz, b);
            return true;
        }
        case BTSeeds:
        {
            Block b = moveRandom(bx, by, bz);
            if(b != null)
                world.setBlock(bx, by, bz, b);
            return true;
        }
        case BTShears:
        case BTSlime:
        case BTSnow:
        case BTStick:
        case BTStickyPiston:
        case BTStickyPistonHead:
        case BTStone:
        case BTStoneAxe:
        case BTStoneButton:
        case BTStoneHoe:
        case BTStonePick:
        case BTStonePressurePlate:
        case BTStoneShovel:
        case BTString:
        case BTTNT:
        case BTTallGrass:
        case BTTorch:
        case BTVines:
        case BTWater:
        case BTWheat:
        case BTWood:
        case BTWoodAxe:
        case BTWoodButton:
        case BTWoodHoe:
        case BTWoodPick:
        case BTWoodPressurePlate:
        case BTWoodShovel:
        case BTWool:
        case BTWorkbench:
            break;
        }
        return false;
    }

    public DyeColor dyedGetDyeColor()
    {
        switch(this.type)
        {
        case BTInkSac:
        case BTRoseRed:
        case BTCactusGreen:
        case BTCocoa:
        case BTLapisLazuli:
        case BTPurpleDye:
        case BTCyanDye:
        case BTLightGrayDye:
        case BTGrayDye:
        case BTPinkDye:
        case BTLimeDye:
        case BTDandelionYellow:
        case BTLightBlueDye:
        case BTMagentaDye:
        case BTOrangeDye:
        case BTBoneMeal:
            return this.type.getDyeColor();
        default:
            return this.data.dyeColor;
        }
    }
}
