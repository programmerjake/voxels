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

import java.io.*;
import java.util.*;

import org.voxels.generate.Tree;
import org.voxels.generate.Tree.TreeType;

/** @author jacob */
public enum BlockType
{
    /** empty block */
    BTEmpty(0, false, BlockDrawType.BDTNone,
            new TextureAtlas.TextureHandle[] {})
    {
        @Override
        public Block make(final int orientation)
        {
            return null;
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public boolean isReplaceable()
        {
            return true;
        }
    },
    /** stone block */
    BTStone(1, true, BlockDrawType.BDTSolid, new TextureAtlas.TextureHandle[]
    {
        TextureAtlas.addImage(new Image("stone.png"))
    })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewStone();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** cobblestone block */
    BTCobblestone(2, true, BlockDrawType.BDTSolid,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("cobblestone.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewCobblestone();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTStone;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** dirt block with grass on top */
    BTGrass(3, true, BlockDrawType.BDTCustom, new TextureAtlas.TextureHandle[]
    {
        TextureAtlas.addImage(new Image("grass.png")),
        TextureAtlas.addImage(new Image("snowgrass.png")),
        TextureAtlas.addImage(new Image("grassmask.png")),
        TextureAtlas.addImage(new Image("dirtmask.png"))
    })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewGrass();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** dirt block */
    BTDirt(4, true, BlockDrawType.BDTSolid, new TextureAtlas.TextureHandle[]
    {
        TextureAtlas.addImage(new Image("dirt.png"))
    })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewDirt();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** sapling block */
    BTSapling(5, false, BlockDrawType.BDTSim3D, makeSaplingTextures())
    {
        @Override
        public double getGrowTime()
        {
            return 60.0;
        }

        @Override
        public Block make(final int orientation)
        {
            return Block.NewSapling(TreeType.Oak);
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return true;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 3;
        }

        @Override
        protected void addToCreativeModeBlockList(final List<Block> list)
        {
            for(int i = 0; i < Tree.TreeType.values().length; i++)
                list.add(Block.NewSapling(Tree.TreeType.values()[i]));
        }
    },
    /** bedrock block */
    BTBedrock(6, true, BlockDrawType.BDTSolid, new TextureAtlas.TextureHandle[]
    {
        TextureAtlas.addImage(new Image("bedrock.png"))
    })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewBedrock();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** water block */
    BTWater(7, false, BlockDrawType.BDTLiquid, new TextureAtlas.TextureHandle[]
    {
        TextureAtlas.addImage(new Image("water.png")),
        TextureAtlas.addImage(new Image("opaquewater.png")),
        TextureAtlas.addImage(new Image("waterbucket.png")),
    })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewWater(-8);
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return true;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public boolean isItemInBucket()
        {
            return true;
        }

        @Override
        public boolean isReplaceable()
        {
            return true;
        }
    },
    /** lava block */
    BTLava(8, false, BlockDrawType.BDTLiquid, new TextureAtlas.TextureHandle[]
    {
        TextureAtlas.addImage(new Image("lava.png")),
        TextureAtlas.addImage(new Image("lavabucket.png")),
    })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewLava(-8);
        }

        @Override
        public int getLight()
        {
            return 15;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return true;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 100;
        }

        @Override
        public boolean isItemInBucket()
        {
            return true;
        }

        @Override
        public boolean isReplaceable()
        {
            return true;
        }
    },
    /** sand block */
    BTSand(9, true, BlockDrawType.BDTSolid, new TextureAtlas.TextureHandle[]
    {
        TextureAtlas.addImage(new Image("sand.png"))
    })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewSand();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTGlass;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** gravel block */
    BTGravel(10, true, BlockDrawType.BDTSolid, new TextureAtlas.TextureHandle[]
    {
        TextureAtlas.addImage(new Image("gravel.png"))
    })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewGravel();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** wood block */
    BTWood(11, true, BlockDrawType.BDTCustom, makeWoodTextures())
    {
        @Override
        public Block make(final int orientation)
        {
            int o;
            if(orientation < 0 || orientation >= 6)
                o = 0;
            else if(Block.getOrientationDX(orientation) != 0)
                o = 1;
            else if(Block.getOrientationDY(orientation) != 0)
                o = 0;
            else
                o = 2;
            return Block.NewWood(TreeType.Oak, o);
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 50;
        }

        @Override
        protected void addToCreativeModeBlockList(final List<Block> list)
        {
            for(int i = 0; i < Tree.TreeType.values().length; i++)
                list.add(Block.NewWood(Tree.TreeType.values()[i], 0));
        }
    },
    /** leaves block */
    BTLeaves(12, false, BlockDrawType.BDTCustom, makeLeavesTextures())
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewLeaves(TreeType.Oak);
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 20;
        }

        @Override
        protected void addToCreativeModeBlockList(final List<Block> list)
        {
            for(int i = 0; i < Tree.TreeType.values().length; i++)
                list.add(Block.NewLeaves(Tree.TreeType.values()[i]));
        }

        @Override
        public boolean isOpaque()
        {
            return !Main.FancyGraphics;
        }
    },
    /** glass block */
    BTGlass(13, false, BlockDrawType.BDTSolid, new TextureAtlas.TextureHandle[]
    {
        TextureAtlas.addImage(new Image("glass.png"))
    })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewGlass();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return true;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** chest */
    BTChest(14, true, BlockDrawType.BDTSolid, new TextureAtlas.TextureHandle[]
    {
        TextureAtlas.addImage(new Image("chest.png"))
    })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewChest();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 80;
        }
    },
    /** workbench */
    BTWorkbench(15, true, BlockDrawType.BDTSolid,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("workbenchblock.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewWorkbench();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 60;
        }
    },
    /** furnace */
    BTFurnace(16, true, BlockDrawType.BDTCustom,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("furnace0.png")),
                TextureAtlas.addImage(new Image("furnace1.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewFurnace();
        }

        @Override
        public int getLight()
        {
            return -1;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** plank */
    BTPlank(17, true, BlockDrawType.BDTSolid, new TextureAtlas.TextureHandle[]
    {
        TextureAtlas.addImage(new Image("plank.png")),
        TextureAtlas.addImage(new Image("birchplank.png")),
        TextureAtlas.addImage(new Image("spruceplank.png")),
        TextureAtlas.addImage(new Image("jungleplank.png")),
    })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewPlank(Tree.TreeType.Oak);
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTCoal;
        }

        @Override
        public int getBurnTime()
        {
            return 12;
        }

        @Override
        protected void addToCreativeModeBlockList(final List<Block> list)
        {
            for(int i = 0; i < Tree.TreeType.values().length; i++)
                list.add(Block.NewPlank(Tree.TreeType.values()[i]));
        }
    },
    /** stick */
    BTStick(18, false, BlockDrawType.BDTItem, new TextureAtlas.TextureHandle[]
    {
        TextureAtlas.addImage(new Image("stick.png"))
    })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewStick();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 3;
        }
    },
    /** wood pick */
    BTWoodPick(19, false, BlockDrawType.BDTTool,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("woodpick.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewWoodPick();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 20;
        }

        @Override
        public ToolType getToolType()
        {
            return ToolType.Pickaxe;
        }

        @Override
        public ToolLevel getToolLevel()
        {
            return ToolLevel.Wood;
        }
    },
    /** stone pick */
    BTStonePick(20, false, BlockDrawType.BDTTool,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("stonepick.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewStonePick();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public ToolType getToolType()
        {
            return ToolType.Pickaxe;
        }

        @Override
        public ToolLevel getToolLevel()
        {
            return ToolLevel.Stone;
        }
    },
    /** wood shovel */
    BTWoodShovel(21, false, BlockDrawType.BDTTool,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("woodshovel.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewWoodShovel();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 20;
        }

        @Override
        public ToolType getToolType()
        {
            return ToolType.Shovel;
        }

        @Override
        public ToolLevel getToolLevel()
        {
            return ToolLevel.Wood;
        }
    },
    /** stone shovel */
    BTStoneShovel(22, false, BlockDrawType.BDTTool,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("stoneshovel.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewStoneShovel();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public ToolType getToolType()
        {
            return ToolType.Shovel;
        }

        @Override
        public ToolLevel getToolLevel()
        {
            return ToolLevel.Stone;
        }
    },
    /** unpowered redstone dust */
    BTRedstoneDustOff(23, false, BlockDrawType.BDTCustom,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("redstoneoff0.png")),
                TextureAtlas.addImage(new Image("redstoneoff1.png")),
                TextureAtlas.addImage(new Image("redstoneoff2.png")),
                TextureAtlas.addImage(new Image("redstoneoff3.png")),
                TextureAtlas.addImage(new Image("redstoneoff4.png")),
                TextureAtlas.addImage(new Image("redstoneoff5.png")),
                TextureAtlas.addImage(new Image("redstoneoff6.png")),
                TextureAtlas.addImage(new Image("redstoneoff7.png")),
                TextureAtlas.addImage(new Image("redstoneoff8.png")),
                TextureAtlas.addImage(new Image("redstoneoff9.png")),
                TextureAtlas.addImage(new Image("redstoneoffa.png")),
                TextureAtlas.addImage(new Image("redstoneoffb.png")),
                TextureAtlas.addImage(new Image("redstoneoffc.png")),
                TextureAtlas.addImage(new Image("redstoneoffd.png")),
                TextureAtlas.addImage(new Image("redstoneoffe.png")),
                TextureAtlas.addImage(new Image("redstoneofff.png")),
                TextureAtlas.addImage(new Image("redstoneoff.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewRedstoneDust(0, 0);
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** powered redstone dust */
    BTRedstoneDustOn(24, false, BlockDrawType.BDTCustom,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("redstoneon0.png")),
                TextureAtlas.addImage(new Image("redstoneon1.png")),
                TextureAtlas.addImage(new Image("redstoneon2.png")),
                TextureAtlas.addImage(new Image("redstoneon3.png")),
                TextureAtlas.addImage(new Image("redstoneon4.png")),
                TextureAtlas.addImage(new Image("redstoneon5.png")),
                TextureAtlas.addImage(new Image("redstoneon6.png")),
                TextureAtlas.addImage(new Image("redstoneon7.png")),
                TextureAtlas.addImage(new Image("redstoneon8.png")),
                TextureAtlas.addImage(new Image("redstoneon9.png")),
                TextureAtlas.addImage(new Image("redstoneona.png")),
                TextureAtlas.addImage(new Image("redstoneonb.png")),
                TextureAtlas.addImage(new Image("redstoneonc.png")),
                TextureAtlas.addImage(new Image("redstoneond.png")),
                TextureAtlas.addImage(new Image("redstoneone.png")),
                TextureAtlas.addImage(new Image("redstoneonf.png")),
                TextureAtlas.addImage(new Image("redstoneon.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewRedstoneDust(0, 0);
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return true;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** redstone ore block */
    BTRedstoneOre(25, true, BlockDrawType.BDTSolid,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("redstoneore.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewRedstoneOre();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTRedstoneDustOff;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** redstone block */
    BTRedstoneBlock(26, true, BlockDrawType.BDTSolid,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("redstoneblock.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewRedstoneBlock();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** off redstone torch */
    BTRedstoneTorchOff(27, false, BlockDrawType.BDTTorch,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("redstonetorchoff.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewRedstoneTorch(false, orientation);
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** on redstone torch */
    BTRedstoneTorchOn(28, false, BlockDrawType.BDTTorch,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("redstonetorchon.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewRedstoneTorch(false, orientation);
        }

        @Override
        public int getLight()
        {
            return 7;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return true;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** stone button */
    BTStoneButton(29, false, BlockDrawType.BDTButton,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("stonebutton.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewStoneButton(0, orientation);
        }

        @Override
        public int getOnTime()
        {
            return 10;
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** wood button */
    BTWoodButton(30, false, BlockDrawType.BDTButton,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("woodbutton.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewWoodButton(0, orientation);
        }

        @Override
        public int getOnTime()
        {
            return 15;
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 15;
        }
    },
    /** coal */
    BTCoal(31, false, BlockDrawType.BDTItem, new TextureAtlas.TextureHandle[]
    {
        TextureAtlas.addImage(new Image("coal.png"))
    })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewCoal();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 40;
        }
    },
    /** coal ore block */
    BTCoalOre(32, true, BlockDrawType.BDTSolid,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("coalore.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewCoalOre();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTCoal;
        }

        @Override
        public int getBurnTime()
        {
            return 40;
        }
    },
    /** iron ingot */
    BTIronIngot(33, false, BlockDrawType.BDTItem,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("ironingot.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewIronIngot();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** iron ore block */
    BTIronOre(34, true, BlockDrawType.BDTSolid,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("ironore.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewIronOre();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTIronIngot;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** lapis lazuli shard */
    BTLapisLazuli(35, false, BlockDrawType.BDTItem,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("lapislazuli.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewLapisLazuli();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public DyeColor getDyeColor()
        {
            return DyeColor.LapisLazuli;
        }
    },
    /** lapis lazuli ore block */
    BTLapisLazuliOre(36, true, BlockDrawType.BDTSolid,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("lapislazuliore.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewLapisLazuliOre();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTLapisLazuli;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** gold ingot */
    BTGoldIngot(37, false, BlockDrawType.BDTItem,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("goldingot.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewGoldIngot();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** gold ore block */
    BTGoldOre(38, true, BlockDrawType.BDTSolid,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("goldore.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewGoldOre();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTGoldIngot;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** diamond */
    BTDiamond(39, false, BlockDrawType.BDTItem,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("diamond.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewDiamond();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** diamond ore block */
    BTDiamondOre(40, true, BlockDrawType.BDTSolid,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("diamondore.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewDiamondOre();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTDiamond;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** emerald */
    BTEmerald(41, false, BlockDrawType.BDTItem,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("emerald.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewEmerald();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** emerald ore block */
    BTEmeraldOre(42, true, BlockDrawType.BDTSolid,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("emeraldore.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewEmeraldOre();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmerald;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** torch */
    BTTorch(43, false, BlockDrawType.BDTTorch, new TextureAtlas.TextureHandle[]
    {
        TextureAtlas.addImage(new Image("torch.png"))
    })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewTorch(orientation);
        }

        @Override
        public int getLight()
        {
            return 14;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return true;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 20;
        }
    },
    /** iron pick */
    BTIronPick(44, false, BlockDrawType.BDTTool,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("ironpick.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewIronPick();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public ToolType getToolType()
        {
            return ToolType.Pickaxe;
        }

        @Override
        public ToolLevel getToolLevel()
        {
            return ToolLevel.Iron;
        }
    },
    /** iron shovel */
    BTIronShovel(45, false, BlockDrawType.BDTTool,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("ironshovel.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewIronShovel();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public ToolType getToolType()
        {
            return ToolType.Shovel;
        }

        @Override
        public ToolLevel getToolLevel()
        {
            return ToolLevel.Iron;
        }
    },
    /** gold pick */
    BTGoldPick(46, false, BlockDrawType.BDTTool,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("goldpick.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewGoldPick();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public ToolType getToolType()
        {
            return ToolType.Pickaxe;
        }

        @Override
        public ToolLevel getToolLevel()
        {
            return ToolLevel.Gold;
        }
    },
    /** gold shovel */
    BTGoldShovel(47, false, BlockDrawType.BDTTool,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("goldshovel.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewGoldShovel();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public ToolType getToolType()
        {
            return ToolType.Shovel;
        }

        @Override
        public ToolLevel getToolLevel()
        {
            return ToolLevel.Gold;
        }
    },
    /** diamond pick */
    BTDiamondPick(48, false, BlockDrawType.BDTTool,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("diamondpick.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewDiamondPick();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public ToolType getToolType()
        {
            return ToolType.Pickaxe;
        }

        @Override
        public ToolLevel getToolLevel()
        {
            return ToolLevel.Diamond;
        }
    },
    /** diamond shovel */
    BTDiamondShovel(49, false, BlockDrawType.BDTTool,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("diamondshovel.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewDiamondShovel();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public ToolType getToolType()
        {
            return ToolType.Shovel;
        }

        @Override
        public ToolLevel getToolLevel()
        {
            return ToolLevel.Diamond;
        }
    },
    /** ladder */
    BTLadder(50, false, BlockDrawType.BDTCustom,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("ladder.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewLadder(orientation);
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 20;
        }

        @Override
        public boolean isClimbable()
        {
            return true;
        }
    },
    /** redstone repeater off */
    BTRedstoneRepeaterOff(51, false, BlockDrawType.BDTCustom,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("redstonerepeateroff.png")),
                TextureAtlas.addImage(new Image("redstonerepeaterlatch.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewRedstoneRepeater(false, 0, 1, orientation);
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public Block make(final int orientation, final int vieworientation)
        {
            if(Block.getOrientationDX(orientation) != 0
                    || Block.getOrientationDZ(orientation) != 0)
                return make(orientation);
            return make(vieworientation);
        }
    },
    /** redstone repeater on */
    BTRedstoneRepeaterOn(52, false, BlockDrawType.BDTCustom,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("redstonerepeateron.png")),
                TextureAtlas.addImage(new Image("redstonerepeaterlatch.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewRedstoneRepeater(false, 0, 1, orientation);
        }

        @Override
        public int getLight()
        {
            return 9;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return true;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public Block make(final int orientation, final int vieworientation)
        {
            if(Block.getOrientationDX(orientation) != 0
                    || Block.getOrientationDZ(orientation) != 0)
                return make(orientation);
            return make(vieworientation);
        }
    },
    /** lever */
    BTLever(53, false, BlockDrawType.BDTCustom,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("lever.png")),
                TextureAtlas.addImage(new Image("leverhandle.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewLever(false, orientation);
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** obsidian block */
    BTObsidian(54, true, BlockDrawType.BDTSolid,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("obsidian.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewObsidian();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** piston */
    BTPiston(55, true, BlockDrawType.BDTCustom,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("piston.png")),
                TextureAtlas.addImage(new Image("extendedpistonbase.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            if(orientation == -1)
                return Block.NewPiston(orientation, false);
            return Block.NewPiston(Block.getNegOrientation(orientation), false);
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public Block make(final int orientation, final int vieworientation)
        {
            return make(vieworientation);
        }

        @Override
        public boolean use3DOrientation()
        {
            return true;
        }
    },
    /** sticky piston */
    BTStickyPiston(56, true, BlockDrawType.BDTCustom,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("stickypiston.png")),
                TextureAtlas.addImage(new Image("extendedpistonbase.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            if(orientation == -1)
                return Block.NewStickyPiston(orientation, false);
            return Block.NewStickyPiston(Block.getNegOrientation(orientation),
                                         false);
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public Block make(final int orientation, final int vieworientation)
        {
            return make(vieworientation);
        }

        @Override
        public boolean use3DOrientation()
        {
            return true;
        }
    },
    /** piston head */
    BTPistonHead(57, false, BlockDrawType.BDTCustom,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("extendedpistonhead.png")),
                TextureAtlas.addImage(new Image("pistonshaft.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewPistonHead(orientation);
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** sticky piston */
    BTStickyPistonHead(
            58,
            false,
            BlockDrawType.BDTCustom,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("extendedstickypistonhead.png")),
                TextureAtlas.addImage(new Image("pistonshaft.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewStickyPistonHead(orientation);
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** slime */
    BTSlime(59, false, BlockDrawType.BDTItem, new TextureAtlas.TextureHandle[]
    {
        TextureAtlas.addImage(new Image("slime.png"))
    })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewSlime();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** gunpowder */
    BTGunpowder(60, false, BlockDrawType.BDTItem,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("gunpowder.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewGunpowder();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** TNT */
    BTTNT(61, true, BlockDrawType.BDTSolid, new TextureAtlas.TextureHandle[]
    {
        TextureAtlas.addImage(new Image("tnt.png"))
    })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewTNT();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** blaze rod */
    BTBlazeRod(62, false, BlockDrawType.BDTItem,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("blazerod.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewBlazeRod();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** blaze powder */
    BTBlazePowder(63, false, BlockDrawType.BDTItem,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("blazepowder.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewBlazePowder();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** stone pressure plate */
    BTStonePressurePlate(64, false, BlockDrawType.BDTCustom,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("stonepressureplate.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewStonePressurePlate();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** wood pressure plate */
    BTWoodPressurePlate(65, false, BlockDrawType.BDTCustom,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("woodpressureplate.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewWoodPressurePlate();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 20;
        }
    },
    /** Snow block */
    BTSnow(66, false, BlockDrawType.BDTCustom, new TextureAtlas.TextureHandle[]
    {
        TextureAtlas.addImage(new Image("snow.png")),
        TextureAtlas.addImage(new Image("snowball.png"))
    })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewSnow(1);
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public boolean isReplaceable()
        {
            return true;
        }
    },
    /** vines */
    BTVines(67, false, BlockDrawType.BDTCustom,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("vines.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewVines(orientation);
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public boolean isClimbable()
        {
            return true;
        }

        @Override
        public boolean isReplaceable()
        {
            return true;
        }
    },
    /** wood axe */
    BTWoodAxe(68, false, BlockDrawType.BDTTool,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("woodaxe.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewWoodAxe();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public ToolType getToolType()
        {
            return ToolType.Axe;
        }

        @Override
        public ToolLevel getToolLevel()
        {
            return ToolLevel.Wood;
        }
    },
    /** stone axe */
    BTStoneAxe(69, false, BlockDrawType.BDTTool,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("stoneaxe.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewStoneAxe();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public ToolType getToolType()
        {
            return ToolType.Axe;
        }

        @Override
        public ToolLevel getToolLevel()
        {
            return ToolLevel.Stone;
        }
    },
    /** Iron Axe */
    BTIronAxe(70, false, BlockDrawType.BDTTool,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("ironaxe.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewIronAxe();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public ToolType getToolType()
        {
            return ToolType.Axe;
        }

        @Override
        public ToolLevel getToolLevel()
        {
            return ToolLevel.Iron;
        }
    },
    /** gold axe */
    BTGoldAxe(71, false, BlockDrawType.BDTTool,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("goldaxe.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewGoldAxe();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public ToolType getToolType()
        {
            return ToolType.Axe;
        }

        @Override
        public ToolLevel getToolLevel()
        {
            return ToolLevel.Gold;
        }
    },
    /** diamond axe */
    BTDiamondAxe(72, false, BlockDrawType.BDTTool,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("diamondaxe.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewDiamondAxe();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public ToolType getToolType()
        {
            return ToolType.Axe;
        }

        @Override
        public ToolLevel getToolLevel()
        {
            return ToolLevel.Diamond;
        }
    },
    /** bucket */
    BTBucket(73, false, BlockDrawType.BDTItem, new TextureAtlas.TextureHandle[]
    {
        TextureAtlas.addImage(new Image("bucket.png"))
    })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewBucket();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** shears */
    BTShears(74, false, BlockDrawType.BDTTool, new TextureAtlas.TextureHandle[]
    {
        TextureAtlas.addImage(new Image("shears.png"))
    })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewShears();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public ToolType getToolType()
        {
            return ToolType.Shears;
        }

        @Override
        public ToolLevel getToolLevel()
        {
            return ToolLevel.Iron;
        }

        @Override
        public int getDurability()
        {
            return 238;
        }
    },
    /** redstone comparator */
    BTRedstoneComparator(75, false, BlockDrawType.BDTCustom,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("redstonecomparatoroff.png")),
                TextureAtlas.addImage(new Image("redstonecomparatoron.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewRedstoneComparator(false, 0, orientation);
        }

        @Override
        public int getLight()
        {
            return -1;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public Block make(final int orientation, final int vieworientation)
        {
            if(Block.getOrientationDX(orientation) != 0
                    || Block.getOrientationDZ(orientation) != 0)
                return make(orientation);
            return make(vieworientation);
        }
    },
    /** quartz */
    BTQuartz(76, false, BlockDrawType.BDTItem, new TextureAtlas.TextureHandle[]
    {
        TextureAtlas.addImage(new Image("quartz.png"))
    })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewQuartz();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** dispenser */
    BTDispenser(77, true, BlockDrawType.BDTCustom,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("dispenser.png")),
                TextureAtlas.addImage(new Image("dropperdispenserframe.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            if(orientation == -1)
                return Block.NewDispenser(-1);
            return Block.NewDispenser(Block.getNegOrientation(orientation));
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public Block make(final int orientation, final int vieworientation)
        {
            return make(vieworientation);
        }

        @Override
        public boolean use3DOrientation()
        {
            return true;
        }
    },
    /** dropper */
    BTDropper(78, true, BlockDrawType.BDTCustom,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("dropper.png")),
                TextureAtlas.addImage(new Image("dropperdispenserframe.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            if(orientation == -1)
                return Block.NewDropper(-1);
            return Block.NewDropper(Block.getNegOrientation(orientation));
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public Block make(final int orientation, final int vieworientation)
        {
            return make(vieworientation);
        }

        @Override
        public boolean use3DOrientation()
        {
            return true;
        }
    },
    /** cobweb block */
    BTCobweb(79, false, BlockDrawType.BDTSim3D,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("web.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewCobweb();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return true;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** string */
    BTString(80, false, BlockDrawType.BDTItem, new TextureAtlas.TextureHandle[]
    {
        TextureAtlas.addImage(new Image("string.png"))
    })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewString();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** bow */
    BTBow(81, false, BlockDrawType.BDTItem, new TextureAtlas.TextureHandle[]
    {
        TextureAtlas.addImage(new Image("bow.png"))
    })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewBow();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** hopper */
    BTHopper(82, false, BlockDrawType.BDTCustom,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("hoppertop.png")),
                TextureAtlas.addImage(new Image("hoppermiddle.png")),
                TextureAtlas.addImage(new Image("hopperend.png")),
                TextureAtlas.addImage(new Image("hoppericon.png")),
            })
    {
        @Override
        public Block make(final int orientation)
        {
            if(orientation == -1)
                return Block.NewHopper(-1);
            return Block.NewHopper(orientation);
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** cactus */
    BTCactus(83, false, BlockDrawType.BDTSolid,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("cactus.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewCactus();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTCactusGreen;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** red mushroom */
    BTRedMushroom(84, false, BlockDrawType.BDTSim3D,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("redmushroom.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewRedMushroom();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return true;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** brown mushroom */
    BTBrownMushroom(85, false, BlockDrawType.BDTSim3D,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("brownmushroom.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewBrownMushroom();
        }

        @Override
        public int getLight()
        {
            return 1;
        }

        @Override
        public boolean isDoubleSided()
        {
            return true;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** dead bush */
    BTDeadBush(86, false, BlockDrawType.BDTSim3D,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("deadbush.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewDeadBush();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return true;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** dandelion */
    BTDandelion(87, false, BlockDrawType.BDTSim3D,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("dandelion.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewDandelion();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return true;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** rose */
    BTRose(88, false, BlockDrawType.BDTSim3D, new TextureAtlas.TextureHandle[]
    {
        TextureAtlas.addImage(new Image("rose.png"))
    })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewRose();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return true;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** tall grass */
    BTTallGrass(89, false, BlockDrawType.BDTSim3D,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("tallgrass.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewTallGrass();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return true;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** seeds */
    BTSeeds(90, false, BlockDrawType.BDTCustom,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("wheat0.png")),
                TextureAtlas.addImage(new Image("wheat1.png")),
                TextureAtlas.addImage(new Image("wheat2.png")),
                TextureAtlas.addImage(new Image("wheat3.png")),
                TextureAtlas.addImage(new Image("wheat4.png")),
                TextureAtlas.addImage(new Image("wheat5.png")),
                TextureAtlas.addImage(new Image("wheat6.png")),
                TextureAtlas.addImage(new Image("wheat7.png")),
                TextureAtlas.addImage(new Image("seeds.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewSeeds(0);
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return true;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** wheat */
    BTWheat(91, false, BlockDrawType.BDTItem, new TextureAtlas.TextureHandle[]
    {
        TextureAtlas.addImage(new Image("wheat.png"))
    })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewWheat();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** farmland */
    BTFarmland(92, false, BlockDrawType.BDTCustom,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("farmland.png")),
                TextureAtlas.addImage(new Image("wetfarmland.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewFarmland(false);
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return true;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** wood hoe */
    BTWoodHoe(93, false, BlockDrawType.BDTTool,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("woodhoe.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewWoodHoe();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public ToolType getToolType()
        {
            return ToolType.Hoe;
        }

        @Override
        public ToolLevel getToolLevel()
        {
            return ToolLevel.Wood;
        }
    },
    /** stone hoe */
    BTStoneHoe(94, false, BlockDrawType.BDTTool,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("stonehoe.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewStoneHoe();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public ToolType getToolType()
        {
            return ToolType.Hoe;
        }

        @Override
        public ToolLevel getToolLevel()
        {
            return ToolLevel.Stone;
        }
    },
    /** Iron Hoe */
    BTIronHoe(95, false, BlockDrawType.BDTTool,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("ironhoe.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewIronHoe();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public ToolType getToolType()
        {
            return ToolType.Hoe;
        }

        @Override
        public ToolLevel getToolLevel()
        {
            return ToolLevel.Iron;
        }
    },
    /** gold hoe */
    BTGoldHoe(96, false, BlockDrawType.BDTTool,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("goldhoe.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewGoldHoe();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public ToolType getToolType()
        {
            return ToolType.Hoe;
        }

        @Override
        public ToolLevel getToolLevel()
        {
            return ToolLevel.Gold;
        }
    },
    BTCocoa(97, false, BlockDrawType.BDTCustom,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("cocoasmall.png")),
                TextureAtlas.addImage(new Image("cocoamedium.png")),
                TextureAtlas.addImage(new Image("cocoalarge.png")),
                TextureAtlas.addImage(new Image("cocoabeans.png")),
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewCocoa(0, orientation);
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public DyeColor getDyeColor()
        {
            return DyeColor.CocoaBeans;
        }
    },
    /** ink sac */
    BTInkSac(98, false, BlockDrawType.BDTItem, new TextureAtlas.TextureHandle[]
    {
        TextureAtlas.addImage(new Image("inksac.png"))
    })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewInkSac();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public DyeColor getDyeColor()
        {
            return DyeColor.InkSac;
        }
    },
    /** rose red */
    BTRoseRed(99, false, BlockDrawType.BDTItem,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("rosered.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewRoseRed();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public DyeColor getDyeColor()
        {
            return DyeColor.RoseRed;
        }
    },
    /** cactus green */
    BTCactusGreen(100, false, BlockDrawType.BDTItem,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("cactusgreen.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewCactusGreen();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public DyeColor getDyeColor()
        {
            return DyeColor.CactusGreen;
        }
    },
    /** purple dye */
    BTPurpleDye(101, false, BlockDrawType.BDTItem,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("purpledye.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewPurpleDye();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public DyeColor getDyeColor()
        {
            return DyeColor.Purple;
        }
    },
    /** cyan dye */
    BTCyanDye(102, false, BlockDrawType.BDTItem,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("cyandye.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewCyanDye();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public DyeColor getDyeColor()
        {
            return DyeColor.Cyan;
        }
    },
    /** light gray dye */
    BTLightGrayDye(103, false, BlockDrawType.BDTItem,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("lightgraydye.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewLightGrayDye();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public DyeColor getDyeColor()
        {
            return DyeColor.LightGray;
        }
    },
    /** gray dye */
    BTGrayDye(104, false, BlockDrawType.BDTItem,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("graydye.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewGrayDye();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public DyeColor getDyeColor()
        {
            return DyeColor.Gray;
        }
    },
    /** pink dye */
    BTPinkDye(105, false, BlockDrawType.BDTItem,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("pinkdye.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewPinkDye();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public DyeColor getDyeColor()
        {
            return DyeColor.Pink;
        }
    },
    /** lime dye */
    BTLimeDye(106, false, BlockDrawType.BDTItem,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("limedye.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewLimeDye();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public DyeColor getDyeColor()
        {
            return DyeColor.Lime;
        }
    },
    /** dandelion yellow */
    BTDandelionYellow(107, false, BlockDrawType.BDTItem,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("dandelionyellow.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewDandelionYellow();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public DyeColor getDyeColor()
        {
            return DyeColor.DandelionYellow;
        }
    },
    /** light blue dye */
    BTLightBlueDye(108, false, BlockDrawType.BDTItem,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("lightbluedye.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewLightBlueDye();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public DyeColor getDyeColor()
        {
            return DyeColor.LightBlue;
        }
    },
    /** magenta dye */
    BTMagentaDye(109, false, BlockDrawType.BDTItem,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("magentadye.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewMagentaDye();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public DyeColor getDyeColor()
        {
            return DyeColor.Magenta;
        }
    },
    /** orange dye */
    BTOrangeDye(110, false, BlockDrawType.BDTItem,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("orangedye.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewOrangeDye();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public DyeColor getDyeColor()
        {
            return DyeColor.Orange;
        }
    },
    /** bone meal */
    BTBoneMeal(111, false, BlockDrawType.BDTItem,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("bonemeal.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewBoneMeal();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public DyeColor getDyeColor()
        {
            return DyeColor.BoneMeal;
        }
    },
    /** bone */
    BTBone(112, false, BlockDrawType.BDTItem, new TextureAtlas.TextureHandle[]
    {
        TextureAtlas.addImage(new Image("bone.png"))
    })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewBone();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** wool */
    BTWool(113, true, BlockDrawType.BDTCustom, new TextureAtlas.TextureHandle[]
    {
        TextureAtlas.addImage(new Image("wool.png"))
    })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewWool(DyeColor.BoneMeal);
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        protected void addToCreativeModeBlockList(final List<Block> list)
        {
            for(int i = 0; i < DyeColor.values.length; i++)
            {
                if(DyeColor.values[i] == DyeColor.None)
                    continue;
                list.add(Block.NewWool(DyeColor.values[i]));
            }
        }
    },
    /** bed */
    BTBed(114, false, BlockDrawType.BDTCustom, new TextureAtlas.TextureHandle[]
    {
        TextureAtlas.addImage(new Image("bedhead.png")),
        TextureAtlas.addImage(new Image("beditem.png"))
    })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewBed(orientation);
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public Block make(final int orientation, final int vieworientation)
        {
            if(Block.getOrientationDX(orientation) != 0
                    || Block.getOrientationDZ(orientation) != 0)
                return make(orientation);
            return make(vieworientation);
        }
    },
    /** bed foot */
    BTBedFoot(115, false, BlockDrawType.BDTCustom,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("bedend.png")),
                TextureAtlas.addImage(new Image("beditem.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewBed(orientation);
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public Block make(final int orientation, final int vieworientation)
        {
            if(Block.getOrientationDX(orientation) != 0
                    || Block.getOrientationDZ(orientation) != 0)
                return make(orientation);
            return make(vieworientation);
        }
    },
    /** fire */
    BTFire(116, false, BlockDrawType.BDTCustom,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("fire0.png")),
                TextureAtlas.addImage(new Image("fire1.png")),
                TextureAtlas.addImage(new Image("fire2.png")),
                TextureAtlas.addImage(new Image("fire3.png")),
                TextureAtlas.addImage(new Image("fire4.png")),
                TextureAtlas.addImage(new Image("fire5.png")),
                TextureAtlas.addImage(new Image("fire6.png")),
                TextureAtlas.addImage(new Image("fire7.png")),
                TextureAtlas.addImage(new Image("fireblock0.png")),
                TextureAtlas.addImage(new Image("fireblock1.png")),
                TextureAtlas.addImage(new Image("fireblock2.png")),
                TextureAtlas.addImage(new Image("fireblock3.png")),
                TextureAtlas.addImage(new Image("fireblock4.png")),
                TextureAtlas.addImage(new Image("fireblock5.png")),
                TextureAtlas.addImage(new Image("fireblock6.png")),
                TextureAtlas.addImage(new Image("fireblock7.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewFire(0);
        }

        @Override
        public int getLight()
        {
            return 15;
        }

        @Override
        public boolean isDoubleSided()
        {
            return true;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return true;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** flint */
    BTFlint(117, false, BlockDrawType.BDTItem, new TextureAtlas.TextureHandle[]
    {
        TextureAtlas.addImage(new Image("flint.png"))
    })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewFlint();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** flint and steel */
    BTFlintAndSteel(118, false, BlockDrawType.BDTTool,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("flintandsteel.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewFlintAndSteel();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public ToolType getToolType()
        {
            return ToolType.FlintAndSteel;
        }

        @Override
        public ToolLevel getToolLevel()
        {
            return ToolLevel.Wood; // has the closest durability to 65
        }
    },
    /** diamond hoe */
    BTDiamondHoe(119, false, BlockDrawType.BDTTool,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("diamondhoe.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewDiamondHoe();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public ToolType getToolType()
        {
            return ToolType.Hoe;
        }

        @Override
        public ToolLevel getToolLevel()
        {
            return ToolLevel.Diamond;
        }
    },
    /** Rail */
    BTRail(120, false, BlockDrawType.BDTRail, new TextureAtlas.TextureHandle[]
    {
        TextureAtlas.addImage(new Image("rail.png")),
        TextureAtlas.addImage(new Image("railcurve.png")),
    })
    {
        @Override
        public Block make(final int orientation)
        {
            if(orientation == -1)
                return Block.NewRail(0);
            if(Block.getOrientationDZ(orientation) != 0)
                return Block.NewRail(0);
            return Block.NewRail(1);
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return true;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public Block make(final int orientation, final int vieworientation)
        {
            if(Block.getOrientationDX(orientation) != 0
                    || Block.getOrientationDZ(orientation) != 0)
                return make(orientation);
            return make(vieworientation);
        }
    },
    /** Detector Rail */
    BTDetectorRail(121, false, BlockDrawType.BDTRail,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("detectorrailoff.png")),
                TextureAtlas.addImage(new Image("detectorrailon.png")),
            })
    {
        @Override
        public Block make(final int orientation)
        {
            if(orientation == -1)
                return Block.NewDetectorRail(0, 0);
            if(Block.getOrientationDZ(orientation) != 0)
                return Block.NewDetectorRail(0, 0);
            return Block.NewDetectorRail(1, 0);
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return true;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public Block make(final int orientation, final int vieworientation)
        {
            if(Block.getOrientationDX(orientation) != 0
                    || Block.getOrientationDZ(orientation) != 0)
                return make(orientation);
            return make(vieworientation);
        }
    },
    /** Activator Rail */
    BTActivatorRail(122, false, BlockDrawType.BDTRail,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("activatorrailoff.png")),
                TextureAtlas.addImage(new Image("activatorrailon.png")),
            })
    {
        @Override
        public Block make(final int orientation)
        {
            if(orientation == -1)
                return Block.NewActivatorRail(0, 0);
            if(Block.getOrientationDZ(orientation) != 0)
                return Block.NewActivatorRail(0, 0);
            return Block.NewActivatorRail(1, 0);
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return true;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public Block make(final int orientation, final int vieworientation)
        {
            if(Block.getOrientationDX(orientation) != 0
                    || Block.getOrientationDZ(orientation) != 0)
                return make(orientation);
            return make(vieworientation);
        }
    },
    /** Powered Rail */
    BTPoweredRail(123, false, BlockDrawType.BDTRail,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("poweredrailoff.png")),
                TextureAtlas.addImage(new Image("poweredrailon.png")),
            })
    {
        @Override
        public Block make(final int orientation)
        {
            if(orientation == -1)
                return Block.NewPoweredRail(0, 0);
            if(Block.getOrientationDZ(orientation) != 0)
                return Block.NewPoweredRail(0, 0);
            return Block.NewPoweredRail(1, 0);
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return true;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public Block make(final int orientation, final int vieworientation)
        {
            if(Block.getOrientationDX(orientation) != 0
                    || Block.getOrientationDZ(orientation) != 0)
                return make(orientation);
            return make(vieworientation);
        }
    },
    /** mine cart */
    BTMineCart(124, false, BlockDrawType.BDTCustom,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("minecartoutside.png")),
                TextureAtlas.addImage(new Image("minecartinside.png")),
                TextureAtlas.addImage(new Image("minecartitem.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewMinecart(0, true);
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return true;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }

        @Override
        public Block make(final int orientation, final int vieworientation)
        {
            return make(vieworientation);
        }
    },
    /** mine cart with chest */
    BTMineCartWithChest(125, false, BlockDrawType.BDTItem,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("minecartwithchest.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return Block.NewMinecartWithChest();
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return true;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** last block value, used to get <code>BlockType.Count</code> */
    BTLast(126, false, BlockDrawType.BDTNone, null)
    {
        @Override
        public Block make(final int orientation)
        {
            return null;
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** sun block, used for drawing sun */
    BTSun(-1, true, BlockDrawType.BDTSolidAllSides,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("sun.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return null;
        }

        @Override
        public int getLight()
        {
            return 15;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** moon block, used for drawing moon */
    BTMoon(-2, true, BlockDrawType.BDTSolidAllSides,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("moon.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return null;
        }

        @Override
        public int getLight()
        {
            return 15;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    },
    /** delete block, used for drawing dig block animation */
    BTDeleteBlock(-3, false, BlockDrawType.BDTCustom,
            new TextureAtlas.TextureHandle[]
            {
                TextureAtlas.addImage(new Image("delete0.png")),
                TextureAtlas.addImage(new Image("delete1.png")),
                TextureAtlas.addImage(new Image("delete2.png")),
                TextureAtlas.addImage(new Image("delete3.png")),
                TextureAtlas.addImage(new Image("delete4.png")),
                TextureAtlas.addImage(new Image("delete5.png")),
                TextureAtlas.addImage(new Image("delete6.png")),
                TextureAtlas.addImage(new Image("delete7.png"))
            })
    {
        @Override
        public Block make(final int orientation)
        {
            return null;
        }

        @Override
        public int getLight()
        {
            return 0;
        }

        @Override
        public boolean isDoubleSided()
        {
            return false;
        }

        @Override
        public boolean isParticleGenerate()
        {
            return false;
        }

        @Override
        public BlockType getSmeltResult()
        {
            return BTEmpty;
        }

        @Override
        public int getBurnTime()
        {
            return 0;
        }
    };
    /**
	 * 
	 */
    public final int value;
    /**
	 * 
	 */
    public static final int Count = BTLast.value;
    /**
	 * 
	 */
    public TextureAtlas.TextureHandle[] textures;
    private final boolean isOpaque;
    /**
	 * 
	 */
    public final BlockDrawType drawType;

    /** @param orientation
     *            the orientation for the new block, or -1 if none
     * @return new block or null */
    public abstract Block make(int orientation);

    /** @param orientation
     *            the orientation for the side of the block clicked on
     * @param vieworientation
     *            the orientation for the direction the player is facing
     * @return new block or null */
    public Block make(final int orientation, final int vieworientation)
    {
        return make(orientation);
    }

    /** @return the amount of light emitted by this block or -1 if that must be
     *         computed */
    public abstract int getLight();

    /** @return true if this block needs to be drawn using double sided polygons */
    public abstract boolean isDoubleSided();

    /** @author jacob */
    public static final class AddedBlockDescriptor
    {
        /**
         * 
         */
        public final int maxCount;
        /**
         * 
         */
        public final Block b;

        /** @param maxCount
         *            the initial <code>maxCount</code>
         * @param b
         *            the initial <code>b</code> */
        public AddedBlockDescriptor(final int maxCount, final Block b)
        {
            this.maxCount = maxCount;
            this.b = b;
        }
    }

    protected List<AddedBlockDescriptor> getCaveChestBlocks(final int y)
    {
        int count = getChestGenCount(y);
        ArrayList<AddedBlockDescriptor> retval = new ArrayList<BlockType.AddedBlockDescriptor>();
        if(count > 0)
            retval.add(new AddedBlockDescriptor(count, make(-1)));
        return retval;
    }

    private static Map<Integer, ArrayList<AddedBlockDescriptor>> caveChestBlocksMap = new HashMap<Integer, ArrayList<AddedBlockDescriptor>>();

    private static ArrayList<AddedBlockDescriptor>
        getAllCaveChestBlocks(final int y)
    {
        ArrayList<AddedBlockDescriptor> v = caveChestBlocksMap.get(Integer.valueOf(y));
        if(v != null)
            return v;
        ArrayList<AddedBlockDescriptor> retval = new ArrayList<BlockType.AddedBlockDescriptor>();
        for(int i = 0; i < BlockType.Count; i++)
            retval.addAll(toBlockType(i).getCaveChestBlocks(y));
        caveChestBlocksMap.put(Integer.valueOf(y), retval);
        return retval;
    }

    public static int getAllCaveChestBlocksSize(final int y)
    {
        return getAllCaveChestBlocks(y).size();
    }

    public static AddedBlockDescriptor
        getAllCaveChestBlocksItem(final int y, final int index)
    {
        return getAllCaveChestBlocks(y).get(index);
    }

    private static ArrayList<Block> creativeModeBlockList = null;

    protected void addToCreativeModeBlockList(final List<Block> list)
    {
        if(isInCreativeInventory())
            list.add(make(-1));
    }

    private static void makeCreativeModeInventory()
    {
        if(creativeModeBlockList == null)
        {
            creativeModeBlockList = new ArrayList<Block>();
            for(int i = 0; i < BlockType.Count; i++)
                toBlockType(i).addToCreativeModeBlockList(creativeModeBlockList);
        }
    }

    /** @return the creative mode inventory size */
    public static int getCreativeModeInventorySize()
    {
        makeCreativeModeInventory();
        return creativeModeBlockList.size();
    }

    /** @param index
     *            the index
     * @return the block at <code>index</code> in the creative mode inventory */
    public static Block getCreativeModeInventoryBlock(final int index)
    {
        makeCreativeModeInventory();
        return creativeModeBlockList.get(index);
    }

    private BlockType(final int newvalue,
                      final boolean newIsOpaque,
                      final BlockDrawType newDrawType,
                      final TextureAtlas.TextureHandle[] t)
    {
        this.value = newvalue;
        this.isOpaque = newIsOpaque;
        this.textures = t;
        this.drawType = newDrawType;
    }

    private static BlockType[] makeBlocksArray()
    {
        BlockType[] blocks = new BlockType[Count];
        BlockType[] vals = values();
        for(int i = 0; i < vals.length; i++)
        {
            int newvalue = vals[i].value;
            if(newvalue >= 0 && newvalue < Count)
                blocks[newvalue] = vals[i];
        }
        return blocks;
    }

    private static BlockType[] blocks = makeBlocksArray();

    /** @param value
     *            value of block to return
     * @return <code>BlockType</code> of block specified by <code>value</code> */
    public static BlockType toBlockType(final int value)
    {
        if(value < 0 || value >= Count)
            return null;
        return blocks[value];
    }

    /** @return the on duration for buttons */
    public int getOnTime()
    {
        return 0;
    }

    /** @return true if this block generates particles */
    public abstract boolean isParticleGenerate();

    /** @return the block type generated by smelting this block */
    public abstract BlockType getSmeltResult();

    /** @return the length of time that this block will fuel a furnace */
    public abstract int getBurnTime();

    /** @return true if this block can be smelted */
    public boolean isSmeltable()
    {
        if(getSmeltResult() == BlockType.BTEmpty)
            return false;
        return true;
    }

    /** get the maximum number of blocks to put in generated cave chests
     * 
     * @param y
     *            the height of the new chest
     * @return the maximum number of blocks to put in generated cave chests */
    public int getChestGenCount(final int y)
    {
        switch(this)
        {
        case BTDeleteBlock:
        case BTSun:
        case BTMoon:
        case BTLast:
            return 0;
        case BTEmpty:
            return 0;
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
        case BTRedstoneDustOff:
        case BTRedstoneDustOn:
        case BTRedstoneOre:
        case BTRedstoneBlock:
        case BTRedstoneTorchOff:
        case BTRedstoneTorchOn:
        case BTStoneButton:
        case BTWoodButton:
            return 0;
        case BTCoal:
            if(y < 50 - World.Depth)
                return 5;
            return 0;
        case BTCoalOre:
            return 0;
        case BTIronIngot:
            if(y < 50 - World.Depth)
                return 2;
            return 0;
        case BTIronOre:
            return 0;
        case BTLapisLazuli:
            if(y < 20 - World.Depth)
                return 4;
            return 0;
        case BTLapisLazuliOre:
            return 0;
        case BTGoldIngot:
            if(y < 25 - World.Depth)
                return 2;
            return 0;
        case BTGoldOre:
            return 0;
        case BTDiamond:
            if(y < 20 - World.Depth)
                return 2;
            return 0;
        case BTDiamondOre:
            return 0;
        case BTEmerald:
            if(y < 20 - World.Depth)
                return 2;
            return 0;
        case BTEmeraldOre:
            return 0;
        case BTTorch:
            return 1;
        case BTIronPick:
        case BTIronShovel:
        case BTGoldPick:
        case BTGoldShovel:
        case BTDiamondPick:
        case BTDiamondShovel:
            return 0;
        case BTLadder:
            return Math.min(2, Math.max(7, 2 - y / 7));
        case BTRedstoneRepeaterOff:
        case BTRedstoneRepeaterOn:
        case BTLever:
        case BTObsidian:
        case BTPiston:
        case BTStickyPiston:
        case BTPistonHead:
        case BTStickyPistonHead:
            return 0;
        case BTSlime:
            if(y < 30 - World.Depth)
                return 5;
            return 0;
        case BTGunpowder:
            if(y > 20 - World.Depth)
                return 5;
            return 0;
        case BTTNT:
            return 0;
        case BTBlazeRod:
        case BTQuartz:
            if(y < 50 - World.Depth)
                return 2;
            return 0;
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
        case BTDandelion:
        case BTRose:
        case BTFarmland:
        case BTSeeds:
        case BTTallGrass:
        case BTWheat:
        case BTWoodHoe:
        case BTStoneHoe:
        case BTIronHoe:
        case BTGoldHoe:
        case BTDiamondHoe:
        case BTCocoa:
            return 0;
        case BTInkSac:
            return 2;
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
            return 0;
        case BTBone:
            return 1;
        case BTWool:
            return 0;
        case BTBed:
        case BTBedFoot:
        case BTFire:
        case BTFlint:
        case BTFlintAndSteel:
            return 0;
        case BTRail:
        case BTDetectorRail:
        case BTActivatorRail:
        case BTPoweredRail:
        case BTMineCart:
        case BTMineCartWithChest:
            return 0;
        }
        return 0;
    }

    /** @return true if this is an item */
    public boolean isItem()
    {
        switch(this)
        {
        case BTDeleteBlock:
        case BTSun:
        case BTMoon:
        case BTLast:
            return false;
        case BTEmpty:
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
            return false;
        case BTStick:
        case BTWoodPick:
        case BTStonePick:
        case BTWoodShovel:
        case BTStoneShovel:
            return true;
        case BTRedstoneDustOff:
        case BTRedstoneDustOn:
        case BTRedstoneOre:
        case BTRedstoneBlock:
        case BTRedstoneTorchOff:
        case BTRedstoneTorchOn:
        case BTStoneButton:
        case BTWoodButton:
            return false;
        case BTCoal:
            return true;
        case BTCoalOre:
            return false;
        case BTIronIngot:
            return true;
        case BTIronOre:
            return false;
        case BTLapisLazuli:
            return true;
        case BTLapisLazuliOre:
            return false;
        case BTGoldIngot:
            return true;
        case BTGoldOre:
            return false;
        case BTDiamond:
            return true;
        case BTDiamondOre:
            return false;
        case BTEmerald:
            return true;
        case BTEmeraldOre:
        case BTTorch:
            return false;
        case BTIronPick:
        case BTIronShovel:
        case BTGoldPick:
        case BTGoldShovel:
        case BTDiamondPick:
        case BTDiamondShovel:
            return true;
        case BTLadder:
        case BTRedstoneRepeaterOff:
        case BTRedstoneRepeaterOn:
        case BTLever:
        case BTObsidian:
        case BTPiston:
        case BTStickyPiston:
        case BTPistonHead:
        case BTStickyPistonHead:
            return false;
        case BTSlime:
        case BTGunpowder:
            return true;
        case BTTNT:
            return false;
        case BTBlazeRod:
        case BTBlazePowder:
            return true;
        case BTStonePressurePlate:
        case BTWoodPressurePlate:
        case BTSnow:
        case BTVines:
            return false;
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
            return true;
        case BTRedstoneComparator:
        case BTDispenser:
        case BTDropper:
        case BTCobweb:
        case BTHopper:
        case BTCactus:
        case BTRedMushroom:
        case BTBrownMushroom:
        case BTDeadBush:
        case BTDandelion:
        case BTRose:
        case BTFarmland:
        case BTSeeds:
        case BTTallGrass:
        case BTCocoa:
        case BTWool:
        case BTBed:
        case BTBedFoot:
        case BTFire:
            return false;
        case BTWheat:
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
        case BTFlint:
        case BTFlintAndSteel:
        case BTMineCart:
        case BTMineCartWithChest:
            return true;
        case BTRail:
        case BTDetectorRail:
        case BTActivatorRail:
        case BTPoweredRail:
            return false;
        }
        return false;
    }

    /** @return true if this block can support entities */
    public boolean isSupporting()
    {
        switch(this)
        {
        case BTDeleteBlock:
        case BTSun:
        case BTMoon:
        case BTLast:
            return false;
        case BTEmpty:
            return false;
        case BTStone:
        case BTCobblestone:
        case BTGrass:
        case BTDirt:
            return true;
        case BTSapling:
            return false;
        case BTBedrock:
            return true;
        case BTWater:
        case BTLava:
            return false;
        case BTSand:
        case BTGravel:
        case BTWood:
        case BTLeaves:
        case BTGlass:
        case BTChest:
        case BTWorkbench:
        case BTFurnace:
        case BTPlank:
            return true;
        case BTStick:
        case BTWoodPick:
        case BTStonePick:
        case BTWoodShovel:
        case BTStoneShovel:
        case BTRedstoneDustOff:
        case BTRedstoneDustOn:
            return false;
        case BTRedstoneOre:
        case BTRedstoneBlock:
            return true;
        case BTRedstoneTorchOff:
        case BTRedstoneTorchOn:
        case BTStoneButton:
        case BTWoodButton:
            return false;
        case BTCoal:
            return false;
        case BTCoalOre:
            return true;
        case BTIronIngot:
            return false;
        case BTIronOre:
            return true;
        case BTLapisLazuli:
            return false;
        case BTLapisLazuliOre:
            return true;
        case BTGoldIngot:
            return false;
        case BTGoldOre:
            return true;
        case BTDiamond:
            return false;
        case BTDiamondOre:
            return true;
        case BTEmerald:
            return false;
        case BTEmeraldOre:
            return true;
        case BTTorch:
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
            return false;
        case BTObsidian:
        case BTPiston:
        case BTStickyPiston:
            return true;
        case BTPistonHead:
        case BTStickyPistonHead:
        case BTSlime:
        case BTGunpowder:
            return false;
        case BTTNT:
            return true;
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
        case BTRedMushroom:
        case BTBrownMushroom:
        case BTDeadBush:
        case BTDandelion:
        case BTRose:
        case BTSeeds:
        case BTTallGrass:
        case BTWheat:
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
        case BTBed:
        case BTBedFoot:
        case BTFire:
        case BTFlint:
        case BTFlintAndSteel:
        case BTRail:
        case BTDetectorRail:
        case BTActivatorRail:
        case BTPoweredRail:
        case BTMineCart:
        case BTMineCartWithChest:
            return false;
        case BTDispenser:
        case BTDropper:
        case BTHopper:
        case BTCactus:
        case BTFarmland:
        case BTWool:
            return true;
        }
        return false;
    }

    /** @return true if this block is solid */
    public boolean isSolid()
    {
        switch(this)
        {
        case BTDeleteBlock:
        case BTSun:
        case BTMoon:
        case BTLast:
            return false;
        case BTEmpty:
            return false;
        case BTStone:
        case BTCobblestone:
        case BTGrass:
        case BTDirt:
            return true;
        case BTSapling:
            return false;
        case BTBedrock:
            return true;
        case BTWater:
        case BTLava:
            return false;
        case BTSand:
        case BTGravel:
        case BTWood:
        case BTLeaves:
        case BTGlass:
        case BTChest:
        case BTWorkbench:
        case BTFurnace:
        case BTPlank:
            return true;
        case BTStick:
        case BTWoodPick:
        case BTStonePick:
        case BTWoodShovel:
        case BTStoneShovel:
        case BTRedstoneDustOff:
        case BTRedstoneDustOn:
            return false;
        case BTRedstoneOre:
        case BTRedstoneBlock:
            return true;
        case BTRedstoneTorchOff:
        case BTRedstoneTorchOn:
        case BTStoneButton:
        case BTWoodButton:
            return false;
        case BTCoal:
            return false;
        case BTCoalOre:
            return true;
        case BTIronIngot:
            return false;
        case BTIronOre:
            return true;
        case BTLapisLazuli:
            return false;
        case BTLapisLazuliOre:
            return true;
        case BTGoldIngot:
            return false;
        case BTGoldOre:
            return true;
        case BTDiamond:
            return false;
        case BTDiamondOre:
            return true;
        case BTEmerald:
            return false;
        case BTEmeraldOre:
            return true;
        case BTTorch:
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
            return false;
        case BTObsidian:
        case BTPiston:
        case BTStickyPiston:
            return true;
        case BTPistonHead:
        case BTStickyPistonHead:
        case BTSlime:
        case BTGunpowder:
            return false;
        case BTTNT:
            return true;
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
        case BTRedMushroom:
        case BTBrownMushroom:
        case BTDeadBush:
        case BTDandelion:
        case BTRose:
        case BTSeeds:
        case BTTallGrass:
        case BTWheat:
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
        case BTFire:
        case BTFlint:
        case BTFlintAndSteel:
        case BTRail:
        case BTDetectorRail:
        case BTActivatorRail:
        case BTPoweredRail:
        case BTMineCart:
        case BTMineCartWithChest:
            return false;
        case BTDispenser:
        case BTDropper:
        case BTHopper:
        case BTCactus:
        case BTFarmland:
        case BTWool:
        case BTBed:
        case BTBedFoot:
            return true;
        }
        return false;
    }

    /** @return the growing time for this sapling */
    public double getGrowTime()
    {
        return -1.0;
    }

    /** @return true if this block can be placed while the player is inside of it */
    public boolean isPlaceableWhileInside()
    {
        switch(this)
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
        case BTGoldIngot:
        case BTGoldPick:
        case BTGoldShovel:
        case BTIronIngot:
        case BTIronPick:
        case BTIronShovel:
        case BTLapisLazuli:
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
        case BTLever:
        case BTSlime:
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
        case BTDandelion:
        case BTRose:
        case BTSeeds:
        case BTTallGrass:
        case BTWheat:
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
        case BTFire:
        case BTFlint:
        case BTFlintAndSteel:
        case BTRail:
        case BTDetectorRail:
        case BTActivatorRail:
        case BTPoweredRail:
        case BTMineCart:
        case BTMineCartWithChest:
            return true;
        case BTPlank:
        case BTLeaves:
        case BTLava:
        case BTGlass:
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
        case BTObsidian:
        case BTPiston:
        case BTStickyPiston:
        case BTPistonHead:
        case BTStickyPistonHead:
        case BTGunpowder:
        case BTTNT:
        case BTBlazeRod:
        case BTBlazePowder:
        case BTSnow:
        case BTRedstoneComparator:
        case BTDispenser:
        case BTDropper:
        case BTHopper:
        case BTCactus:
        case BTFarmland:
        case BTWool:
        case BTBed:
        case BTBedFoot:
            return false;
        }
        return false;
    }

    /** @return true if this block is in the creative mode inventory */
    public boolean isInCreativeInventory()
    {
        switch(this)
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
        case BTGoldIngot:
        case BTGoldPick:
        case BTGoldShovel:
        case BTIronIngot:
        case BTIronPick:
        case BTIronShovel:
        case BTLapisLazuli:
        case BTPlank:
        case BTRedstoneBlock:
        case BTRedstoneDustOff:
        case BTRedstoneTorchOff:
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
        case BTLever:
        case BTSlime:
        case BTStonePressurePlate:
        case BTWoodPressurePlate:
        case BTLava:
        case BTGlass:
        case BTChest:
        case BTCobblestone:
        case BTDirt:
        case BTFurnace:
        case BTGoldOre:
        case BTGravel:
        case BTIronOre:
        case BTSand:
        case BTStone:
        case BTWood:
        case BTWorkbench:
        case BTRedstoneRepeaterOff:
        case BTObsidian:
        case BTPiston:
        case BTStickyPiston:
        case BTGunpowder:
        case BTTNT:
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
        case BTRedstoneComparator:
        case BTQuartz:
        case BTGrass:
        case BTCoalOre:
        case BTDiamondOre:
        case BTEmeraldOre:
        case BTLapisLazuliOre:
        case BTRedstoneOre:
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
        case BTDandelion:
        case BTRose:
        case BTFarmland:
        case BTSeeds:
        case BTTallGrass:
        case BTWheat:
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
        case BTBed:
        case BTFire:
        case BTFlint:
        case BTFlintAndSteel:
        case BTRail:
        case BTDetectorRail:
        case BTActivatorRail:
        case BTPoweredRail:
        case BTMineCart:
        case BTMineCartWithChest:
            return true;
        case BTLeaves:
        case BTBedrock:
        case BTRedstoneRepeaterOn:
        case BTPistonHead:
        case BTStickyPistonHead:
        case BTRedstoneDustOn:
        case BTRedstoneTorchOn:
        case BTBedFoot:
            return false;
        }
        return false;
    }

    /** write to a <code>DataOutput</code>
     * 
     * @param o
     *            <code>OutputStream</code> to write to
     * @throws IOException
     *             the exception thrown */
    public void write(final DataOutput o) throws IOException
    {
        if(this.value < 0 || this.value >= Count)
            throw new IOException("tried to write special BlockType");
        o.writeShort(this.value);
    }

    /** read from a <code>DataInput</code>
     * 
     * @param i
     *            <code>DataInput</code> to read from
     * @return the read <code>BlockType</code>
     * @throws IOException
     *             the exception thrown */
    public static BlockType read(final DataInput i) throws IOException
    {
        int value = i.readUnsignedShort();
        if(value < 0 || value >= Count)
            throw new IOException("BlockType.value out of range");
        return toBlockType(value);
    }

    /** @return true if when making this block, you should use height to get the
     *         view orientation */
    public boolean use3DOrientation()
    {
        return false;
    }

    /** @return true if this block is explodable */
    public boolean isExplodable()
    {
        switch(this)
        {
        case BTSun:
        case BTMoon:
        case BTDeleteBlock:
        case BTLast:
            return false;
        case BTEmpty:
            return true;
        case BTCoal:
        case BTDiamond:
        case BTDiamondPick:
        case BTDiamondShovel:
        case BTEmerald:
        case BTGoldIngot:
        case BTGoldPick:
        case BTGoldShovel:
        case BTIronIngot:
        case BTIronPick:
        case BTIronShovel:
        case BTLapisLazuli:
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
        case BTLever:
        case BTSlime:
        case BTLeaves:
        case BTLava:
        case BTGlass:
            return true;
        case BTBedrock:
            return false;
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
        case BTDandelion:
        case BTRose:
        case BTFarmland:
        case BTSeeds:
        case BTTallGrass:
        case BTWheat:
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
        case BTBed:
        case BTBedFoot:
        case BTFire:
        case BTFlint:
        case BTFlintAndSteel:
        case BTRail:
        case BTDetectorRail:
        case BTActivatorRail:
        case BTPoweredRail:
        case BTMineCart:
        case BTMineCartWithChest:
            return true;
        }
        return false;
    }

    /** @author jacob */
    public static enum Replaceability
    {
        /**
         * 
         */
        Replace,
        /**
         * 
         */
        GrowAround,
        /**
         * 
         */
        CanNotGrow
    }

    /** @param replacingBlock
     *            the replacing block
     * @return the replaceability of this block */
    public Replaceability getReplaceability(final BlockType replacingBlock)
    {
        switch(this)
        {
        case BTSun:
        case BTMoon:
        case BTDeleteBlock:
        case BTLast:
            return Replaceability.CanNotGrow;
        case BTEmpty:
            return Replaceability.Replace;
        case BTCoal:
        case BTDiamond:
        case BTDiamondPick:
        case BTDiamondShovel:
        case BTEmerald:
        case BTGoldIngot:
        case BTGoldPick:
        case BTGoldShovel:
        case BTIronIngot:
        case BTIronPick:
        case BTIronShovel:
        case BTLapisLazuli:
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
            return Replaceability.Replace;
        case BTWater:
        case BTRedstoneBlock:
            return Replaceability.CanNotGrow;
        case BTWoodButton:
        case BTWoodPick:
        case BTWoodShovel:
        case BTLadder:
        case BTLever:
        case BTSlime:
        case BTLeaves:
            if(replacingBlock == BTVines || replacingBlock == BTCocoa)
                return Replaceability.GrowAround;
            return Replaceability.Replace;
        case BTLava:
        case BTGlass:
        case BTBedrock:
            return Replaceability.CanNotGrow;
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
            return Replaceability.CanNotGrow;
        case BTWood:
        case BTPlank:
            if(replacingBlock == BTWood)
                return Replaceability.CanNotGrow;
            return Replaceability.GrowAround;
        case BTWorkbench:
        case BTRedstoneRepeaterOff:
        case BTRedstoneRepeaterOn:
        case BTRedstoneComparator:
            return Replaceability.CanNotGrow;
        case BTObsidian:
            return Replaceability.CanNotGrow;
        case BTPiston:
        case BTStickyPiston:
        case BTPistonHead:
        case BTStickyPistonHead:
            return Replaceability.CanNotGrow;
        case BTGunpowder:
            return Replaceability.Replace;
        case BTTNT:
        case BTStonePressurePlate:
        case BTWoodPressurePlate:
            return Replaceability.CanNotGrow;
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
        case BTRedMushroom:
        case BTBrownMushroom:
        case BTDeadBush:
        case BTDandelion:
        case BTRose:
        case BTSeeds:
        case BTTallGrass:
        case BTWheat:
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
        case BTFire:
        case BTFlint:
        case BTFlintAndSteel:
        case BTMineCart:
        case BTMineCartWithChest:
            return Replaceability.Replace;
        case BTDispenser:
        case BTDropper:
        case BTHopper:
        case BTFarmland:
        case BTWool:
        case BTBed:
        case BTBedFoot:
        case BTRail:
        case BTDetectorRail:
        case BTActivatorRail:
        case BTPoweredRail:
            return Replaceability.CanNotGrow;
        case BTCactus:
            if(replacingBlock == BTWood)
                return Replaceability.CanNotGrow;
            return Replaceability.GrowAround;
        }
        return Replaceability.CanNotGrow;
    }

    /** @return if this block is opaque */
    public boolean isOpaque()
    {
        return this.isOpaque;
    }

    /** @return if this block is climbable */
    public boolean isClimbable()
    {
        return false;
    }

    /** @author jacob */
    public static enum ToolType
    {
        /***/
        None
        {
            @Override
            public boolean diggingUsesTool()
            {
                return false;
            }
        },
        /***/
        Axe
        {
            @Override
            public boolean diggingUsesTool()
            {
                return true;
            }
        },
        /***/
        Pickaxe
        {
            @Override
            public boolean diggingUsesTool()
            {
                return true;
            }
        },
        /***/
        Shovel
        {
            @Override
            public boolean diggingUsesTool()
            {
                return true;
            }
        },
        /***/
        Hoe
        {
            @Override
            public boolean diggingUsesTool()
            {
                return false;
            }
        },
        /***/
        Shears
        {
            @Override
            public boolean diggingUsesTool()
            {
                return true;
            }
        },
        /***/
        FlintAndSteel
        {
            @Override
            public boolean diggingUsesTool()
            {
                return false;
            }
        };
        public abstract boolean diggingUsesTool();
    }

    /** @return the tool type */
    public ToolType getToolType()
    {
        return ToolType.None;
    }

    /** @author jacob */
    public static enum ToolLevel
    {
        /***/
        Nothing,
        /***/
        Wood,
        /***/
        Stone,
        /***/
        Iron,
        /***/
        Gold,
        /***/
        Diamond
    }

    /** @return the tool level */
    public ToolLevel getToolLevel()
    {
        return ToolLevel.Nothing;
    }

    /** @return the block's blast resistance */
    public float getBlastResistance()
    {
        switch(this)
        {
        case BTBedrock:
        case BTDeleteBlock:
        case BTLast:
        case BTMoon:
        case BTSun:
            return 1e10f;
        case BTBlazePowder:
        case BTBlazeRod:
        case BTCoal:
        case BTDiamond:
        case BTDiamondPick:
        case BTDiamondShovel:
        case BTEmerald:
        case BTEmpty:
        case BTGoldIngot:
        case BTGoldPick:
        case BTGoldShovel:
        case BTGunpowder:
        case BTIronIngot:
        case BTIronPick:
        case BTIronShovel:
        case BTLapisLazuli:
        case BTSlime:
        case BTStick:
        case BTStonePick:
        case BTStoneShovel:
        case BTWoodPick:
        case BTWoodShovel:
        case BTString:
        case BTBow:
            return 0;
        case BTChest:
            return 12.5f;
        case BTCoalOre:
            return 15f;
        case BTCobblestone:
            return 30f;
        case BTDiamondOre:
            return 15f;
        case BTDirt:
            return 2.5f;
        case BTEmeraldOre:
            return 15f;
        case BTFurnace:
            return 17.5f;
        case BTGlass:
            return 1.5f;
        case BTGoldOre:
            return 15f;
        case BTGrass:
            return 3f;
        case BTGravel:
            return 3f;
        case BTIronOre:
            return 15f;
        case BTLadder:
            return 2f;
        case BTLapisLazuliOre:
            return 15f;
        case BTLava:
            return 500f;
        case BTLeaves:
            return 1f;
        case BTLever:
            return 2.5f;
        case BTObsidian:
            return 6000f;
        case BTPiston:
            return 2.5f;
        case BTPistonHead:
            return 2.5f;
        case BTPlank:
            return 15f;
        case BTRedstoneBlock:
            return 30f;
        case BTRedstoneDustOff:
            return 0f;
        case BTRedstoneDustOn:
            return 0f;
        case BTRedstoneOre:
            return 15f;
        case BTRedstoneRepeaterOff:
            return 0f;
        case BTRedstoneRepeaterOn:
            return 0f;
        case BTRedstoneTorchOff:
            return 0f;
        case BTRedstoneTorchOn:
            return 0f;
        case BTSand:
            return 2.5f;
        case BTSapling:
            return 0f;
        case BTSnow:
            return 0.5f;
        case BTStickyPiston:
            return 2.5f;
        case BTStickyPistonHead:
            return 2.5f;
        case BTStone:
            return 30f;
        case BTStoneButton:
            return 2.5f;
        case BTStonePressurePlate:
            return 2.5f;
        case BTTNT:
            return 0f;
        case BTTorch:
            return 0f;
        case BTVines:
            return 1f;
        case BTWater:
            return 500f;
        case BTWood:
            return 10f;
        case BTWoodButton:
            return 2.5f;
        case BTWoodPressurePlate:
            return 2.5f;
        case BTWorkbench:
            return 12.5f;
        case BTWoodAxe:
        case BTStoneAxe:
        case BTIronAxe:
        case BTGoldAxe:
        case BTDiamondAxe:
        case BTShears:
        case BTBucket:
        case BTRedstoneComparator:
        case BTQuartz:
            return 0f;
        case BTDispenser:
        case BTDropper:
            return 17.5f;
        case BTCobweb:
            return 20;
        case BTHopper:
        case BTCocoa:
            return 15;
        case BTCactus:
            return 2;
        case BTRedMushroom:
        case BTBrownMushroom:
        case BTDeadBush:
        case BTDandelion:
        case BTRose:
            return 0;
        case BTFarmland:
            return 3;
        case BTSeeds:
        case BTTallGrass:
        case BTWheat:
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
        case BTFire:
            return 0;
        case BTWool:
            return 4;
        case BTBed:
        case BTBedFoot:
            return 1;
        case BTFlint:
        case BTFlintAndSteel:
        case BTMineCart:
        case BTMineCartWithChest:
            return 0;
        case BTRail:
        case BTDetectorRail:
        case BTActivatorRail:
        case BTPoweredRail:
            return 3.5f;
        }
        return 1e10f;
    }

    /** @return if this item is in a bucket */
    public boolean isItemInBucket()
    {
        return false;
    }

    /** @return if this block is replaceable */
    public boolean isReplaceable()
    {
        return false;
    }

    /** @return this tool's durability */
    public int getDurability()
    {
        switch(getToolLevel())
        {
        case Diamond:
            return 1562;
        case Gold:
            return 33;
        case Iron:
            return 251;
        case Nothing:
            return 0;
        case Stone:
            return 132;
        case Wood:
            return 60;
        }
        return 0;
    }

    protected static TextureAtlas.TextureHandle[] makeLeavesTextures()
    {
        Tree.TreeType[] values = Tree.TreeType.values();
        TextureAtlas.TextureHandle[] retval = new TextureAtlas.TextureHandle[values.length * 2];
        for(int i = 0; i < values.length; i++)
        {
            Image fancyImg = new Image(values[i].leavesImgName);
            retval[i + values.length] = TextureAtlas.addImage(fancyImg);
            Image fastImg = new Image(fancyImg);
            fastImg.setSolidBackground(Color.V(0.25f));
            retval[i] = TextureAtlas.addImage(fastImg);
        }
        return retval;
    }

    protected static TextureAtlas.TextureHandle[] makeWoodTextures()
    {
        Tree.TreeType[] values = Tree.TreeType.values();
        TextureAtlas.TextureHandle[] retval = new TextureAtlas.TextureHandle[values.length];
        for(int i = 0; i < values.length; i++)
        {
            Image img = new Image(values[i].woodImgName);
            retval[i] = TextureAtlas.addImage(img);
        }
        return retval;
    }

    protected static TextureAtlas.TextureHandle[] makeSaplingTextures()
    {
        Tree.TreeType[] values = Tree.TreeType.values();
        TextureAtlas.TextureHandle[] retval = new TextureAtlas.TextureHandle[values.length];
        for(int i = 0; i < values.length; i++)
        {
            Image img = new Image(values[i].saplingImgName);
            retval[i] = TextureAtlas.addImage(img);
        }
        return retval;
    }

    public enum DyeColor
    {
        InkSac(0x19, 0x19, 0x19),
        RoseRed(0x99, 0x33, 0x33),
        CactusGreen(0x66, 0x7F, 0x33),
        CocoaBeans(0x66, 0x4C, 0x33),
        LapisLazuli(0x33, 0x4C, 0xB2),
        Purple(0x7F, 0x3F, 0xB2),
        Cyan(0x4C, 0x7F, 0x99),
        LightGray(0x99, 0x99, 0x99),
        Gray(0x4C, 0x4C, 0x4C),
        Pink(0xF2, 0x7F, 0xA5),
        Lime(0x7F, 0xCC, 0x19),
        DandelionYellow(0xE5, 0xE5, 0x33),
        LightBlue(0x66, 0x99, 0xD8),
        Magenta(0xB2, 0x4C, 0xD8),
        Orange(0xD8, 0x7F, 0x33),
        BoneMeal(0xFF, 0xFF, 0xFF),
        None(0, 0, 0);
        public final float r, g, b;
        public static final DyeColor[] values = values();

        private DyeColor(final int r, final int g, final int b)
        {
            this.r = (float)r / 0xFF;
            this.g = (float)g / 0xFF;
            this.b = (float)b / 0xFF;
        }

        public void write(final DataOutput o) throws IOException
        {
            o.writeByte(ordinal());
        }

        public static DyeColor read(final DataInput i) throws IOException
        {
            int v = i.readUnsignedByte();
            if(v >= values.length)
                throw new IOException("DyeColor is out of range");
            return values[v];
        }
    }

    public DyeColor getDyeColor()
    {
        return DyeColor.None;
    }

    public enum Flammability
    {
        NotFlammable, BurnUp, BurnForever
    }

    /** @param isTopBurning
     *            if the top is burning
     * @return the flammability of this block */
    public Flammability getFlammability(final boolean isTopBurning)
    {
        switch(this)
        {
        case BTDeleteBlock:
        case BTLast:
        case BTMoon:
        case BTSun:
            return Flammability.NotFlammable;
        case BTBedrock:
            return Flammability.NotFlammable;
        case BTBed:
        case BTBedFoot:
        case BTBlazePowder:
        case BTBlazeRod:
        case BTBone:
        case BTBoneMeal:
        case BTBow:
        case BTBrownMushroom:
        case BTBucket:
        case BTCactus:
        case BTCactusGreen:
        case BTChest:
        case BTCoal:
        case BTCoalOre:
        case BTCobblestone:
        case BTCobweb:
        case BTCocoa:
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
        case BTEmpty:
        case BTFarmland:
        case BTFire:
        case BTFurnace:
        case BTGlass:
        case BTGoldAxe:
        case BTGoldHoe:
        case BTGoldIngot:
        case BTGoldOre:
        case BTGoldPick:
        case BTGoldShovel:
        case BTGrass:
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
        case BTPurpleDye:
        case BTQuartz:
        case BTRedMushroom:
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
        case BTSapling:
        case BTSeeds:
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
        case BTTorch:
        case BTWater:
        case BTWheat:
        case BTWoodAxe:
        case BTWoodButton:
        case BTWoodHoe:
        case BTWoodPick:
        case BTWoodPressurePlate:
        case BTWoodShovel:
        case BTWorkbench:
        case BTFlint:
        case BTFlintAndSteel:
        case BTRail:
        case BTDetectorRail:
        case BTActivatorRail:
        case BTPoweredRail:
        case BTMineCart:
        case BTMineCartWithChest:
            return Flammability.NotFlammable;
        case BTLeaves:
        case BTPlank:
        case BTTallGrass:
        case BTVines:
        case BTWood:
        case BTWool:
            return Flammability.BurnUp;
        case BTTNT:
            return Flammability.BurnForever;
        }
        return Flammability.NotFlammable;
    }
}
