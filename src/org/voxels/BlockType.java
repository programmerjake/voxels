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

/**
 * @author jacob
 * 
 */
public enum BlockType
{
	/**
	 * empty block
	 */
	BTEmpty(0, false, BlockDrawType.BDTNone, new Image[] {})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * stone block
	 */
	BTStone(1, true, BlockDrawType.BDTSolid, new Image[]
	{
		new Image("stone.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * cobblestone block
	 */
	BTCobblestone(2, true, BlockDrawType.BDTSolid, new Image[]
	{
		new Image("cobblestone.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * dirt block with grass on top
	 */
	BTGrass(3, true, BlockDrawType.BDTSolid, new Image[]
	{
		new Image("grass.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * dirt block
	 */
	BTDirt(4, true, BlockDrawType.BDTSolid, new Image[]
	{
		new Image("dirt.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * sapling block
	 */
	BTSapling(5, false, BlockDrawType.BDTCustom, new Image[]
	{
		new Image("sapling.png")
	})
	{
		@Override
		public double getGrowTime()
		{
			return 60.0;
		}

		@Override
		public Block make(int orientation)
		{
			return Block.NewSapling();
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
	},
	/**
	 * bedrock block
	 */
	BTBedrock(6, true, BlockDrawType.BDTSolid, new Image[]
	{
		new Image("bedrock.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * water block
	 */
	BTWater(7, false, BlockDrawType.BDTLiquid, new Image[]
	{
	    new Image("water.png"), new Image("opaquewater.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	},
	/**
	 * lava block
	 */
	BTLava(8, false, BlockDrawType.BDTLiquid, new Image[]
	{
		new Image("lava.png")
	})
	{
		@Override
		public Block make(int orientation)
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
			return 100;
		}
	},
	/**
	 * sand block
	 */
	BTSand(9, true, BlockDrawType.BDTSolid, new Image[]
	{
		new Image("sand.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * gravel block
	 */
	BTGravel(10, true, BlockDrawType.BDTSolid, new Image[]
	{
		new Image("gravel.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * wood block
	 */
	BTWood(11, true, BlockDrawType.BDTCustom, new Image[]
	{
		new Image("wood.png")
	})
	{
		@Override
		public Block make(int orientation)
		{
			return Block.NewWood();
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
	},
	/**
	 * leaves block
	 */
	BTLeaves(12, true, BlockDrawType.BDTCustom, new Image[]
	{
		new Image("leaves.png")
	})
	{
		@Override
		public Block make(int orientation)
		{
			return Block.NewLeaves();
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
	/**
	 * glass block
	 */
	BTGlass(13, false, BlockDrawType.BDTSolid, new Image[]
	{
		new Image("glass.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * chest
	 */
	BTChest(14, true, BlockDrawType.BDTSolid, new Image[]
	{
		new Image("chest.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * workbench
	 */
	BTWorkbench(15, true, BlockDrawType.BDTSolid, new Image[]
	{
		new Image("workbenchblock.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * furnace
	 */
	BTFurnace(16, true, BlockDrawType.BDTCustom, new Image[]
	{
	    new Image("furnace0.png"), new Image("furnace1.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * plank
	 */
	BTPlank(17, false, BlockDrawType.BDTItem, new Image[]
	{
		new Image("plank.png")
	})
	{
		@Override
		public Block make(int orientation)
		{
			return Block.NewPlank();
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
	},
	/**
	 * stick
	 */
	BTStick(18, false, BlockDrawType.BDTItem, new Image[]
	{
		new Image("stick.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * wood pick
	 */
	BTWoodPick(19, false, BlockDrawType.BDTItem, new Image[]
	{
		new Image("woodpick.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	},
	/**
	 * stone pick
	 */
	BTStonePick(20, false, BlockDrawType.BDTItem, new Image[]
	{
		new Image("stonepick.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	},
	/**
	 * wood shovel
	 */
	BTWoodShovel(21, false, BlockDrawType.BDTItem, new Image[]
	{
		new Image("woodshovel.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	},
	/**
	 * stone shovel
	 */
	BTStoneShovel(22, false, BlockDrawType.BDTItem, new Image[]
	{
		new Image("stoneshovel.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	},
	/**
	 * unpowered redstone dust
	 */
	BTRedstoneDustOff(23, false, BlockDrawType.BDTCustom, new Image[]
	{
	    new Image("redstoneoff0.png"),
	    new Image("redstoneoff1.png"),
	    new Image("redstoneoff2.png"),
	    new Image("redstoneoff3.png"),
	    new Image("redstoneoff4.png"),
	    new Image("redstoneoff5.png"),
	    new Image("redstoneoff6.png"),
	    new Image("redstoneoff7.png"),
	    new Image("redstoneoff8.png"),
	    new Image("redstoneoff9.png"),
	    new Image("redstoneoffA.png"),
	    new Image("redstoneoffB.png"),
	    new Image("redstoneoffC.png"),
	    new Image("redstoneoffD.png"),
	    new Image("redstoneoffE.png"),
	    new Image("redstoneoffF.png"),
	    new Image("redstoneoff.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * powered redstone dust
	 */
	BTRedstoneDustOn(24, false, BlockDrawType.BDTCustom, new Image[]
	{
	    new Image("redstoneon0.png"),
	    new Image("redstoneon1.png"),
	    new Image("redstoneon2.png"),
	    new Image("redstoneon3.png"),
	    new Image("redstoneon4.png"),
	    new Image("redstoneon5.png"),
	    new Image("redstoneon6.png"),
	    new Image("redstoneon7.png"),
	    new Image("redstoneon8.png"),
	    new Image("redstoneon9.png"),
	    new Image("redstoneonA.png"),
	    new Image("redstoneonB.png"),
	    new Image("redstoneonC.png"),
	    new Image("redstoneonD.png"),
	    new Image("redstoneonE.png"),
	    new Image("redstoneonF.png"),
	    new Image("redstoneon.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * redstone ore block
	 */
	BTRedstoneOre(25, true, BlockDrawType.BDTSolid, new Image[]
	{
		new Image("redstoneore.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * redstone block
	 */
	BTRedstoneBlock(26, true, BlockDrawType.BDTSolid, new Image[]
	{
		new Image("redstoneblock.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * off redstone torch
	 */
	BTRedstoneTorchOff(27, false, BlockDrawType.BDTTorch, new Image[]
	{
		new Image("redstonetorchoff.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * on redstone torch
	 */
	BTRedstoneTorchOn(28, false, BlockDrawType.BDTTorch, new Image[]
	{
		new Image("redstonetorchon.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * stone button
	 */
	BTStoneButton(29, false, BlockDrawType.BDTButton, new Image[]
	{
		new Image("stonebutton.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * wood button
	 */
	BTWoodButton(30, false, BlockDrawType.BDTButton, new Image[]
	{
		new Image("woodbutton.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * coal
	 */
	BTCoal(31, false, BlockDrawType.BDTItem, new Image[]
	{
		new Image("coal.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * coal ore block
	 */
	BTCoalOre(32, true, BlockDrawType.BDTSolid, new Image[]
	{
		new Image("coalore.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * iron ingot
	 */
	BTIronIngot(33, false, BlockDrawType.BDTItem, new Image[]
	{
		new Image("ironingot.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * iron ore block
	 */
	BTIronOre(34, true, BlockDrawType.BDTSolid, new Image[]
	{
		new Image("ironore.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * lapis lazuli shard
	 */
	BTLapisLazuli(35, false, BlockDrawType.BDTItem, new Image[]
	{
		new Image("lapislazuli.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	},
	/**
	 * lapis lazuli ore block
	 */
	BTLapisLazuliOre(36, true, BlockDrawType.BDTSolid, new Image[]
	{
		new Image("lapislazuliore.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * gold ingot
	 */
	BTGoldIngot(37, false, BlockDrawType.BDTItem, new Image[]
	{
		new Image("goldingot.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * gold ore block
	 */
	BTGoldOre(38, true, BlockDrawType.BDTSolid, new Image[]
	{
		new Image("goldore.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * diamond
	 */
	BTDiamond(39, false, BlockDrawType.BDTItem, new Image[]
	{
		new Image("diamond.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * diamond ore block
	 */
	BTDiamondOre(40, true, BlockDrawType.BDTSolid, new Image[]
	{
		new Image("diamondore.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * emerald
	 */
	BTEmerald(41, false, BlockDrawType.BDTItem, new Image[]
	{
		new Image("emerald.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * emerald ore block
	 */
	BTEmeraldOre(42, true, BlockDrawType.BDTSolid, new Image[]
	{
		new Image("emeraldore.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * torch
	 */
	BTTorch(43, false, BlockDrawType.BDTTorch, new Image[]
	{
		new Image("torch.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * iron pick
	 */
	BTIronPick(44, false, BlockDrawType.BDTItem, new Image[]
	{
		new Image("ironpick.png")
	})
	{
		public Block make(int orientation)
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
	},
	/**
	 * iron shovel
	 */
	BTIronShovel(45, false, BlockDrawType.BDTItem, new Image[]
	{
		new Image("ironshovel.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	},
	/**
	 * gold pick
	 */
	BTGoldPick(46, false, BlockDrawType.BDTItem, new Image[]
	{
		new Image("goldpick.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	},
	/**
	 * gold shovel
	 */
	BTGoldShovel(47, false, BlockDrawType.BDTItem, new Image[]
	{
		new Image("goldshovel.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	},
	/**
	 * diamond pick
	 */
	BTDiamondPick(48, false, BlockDrawType.BDTItem, new Image[]
	{
		new Image("diamondpick.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	},
	/**
	 * diamond shovel
	 */
	BTDiamondShovel(49, false, BlockDrawType.BDTItem, new Image[]
	{
		new Image("diamondshovel.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	},
	/**
	 * ladder
	 */
	BTLadder(50, false, BlockDrawType.BDTCustom, new Image[]
	{
		new Image("ladder.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	},
	/**
	 * redstone repeater off
	 */
	BTRedstoneRepeaterOff(51, false, BlockDrawType.BDTCustom, new Image[]
	{
	    new Image("redstonerepeateroff.png"),
	    new Image("redstonerepeaterlatch.png")
	})
	{
		@Override
		public Block make(int orientation)
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
		public Block make(int orientation, int vieworientation)
		{
			if(Block.getOrientationDX(orientation) != 0
			        || Block.getOrientationDZ(orientation) != 0)
				return make(orientation);
			return make(vieworientation);
		}
	},
	/**
	 * redstone repeater on
	 */
	BTRedstoneRepeaterOn(52, false, BlockDrawType.BDTCustom, new Image[]
	{
	    new Image("redstonerepeateron.png"),
	    new Image("redstonerepeaterlatch.png")
	})
	{
		@Override
		public Block make(int orientation)
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
		public Block make(int orientation, int vieworientation)
		{
			if(Block.getOrientationDX(orientation) != 0
			        || Block.getOrientationDZ(orientation) != 0)
				return make(orientation);
			return make(vieworientation);
		}
	},
	/**
	 * lever
	 */
	BTLever(53, false, BlockDrawType.BDTCustom, new Image[]
	{
	    new Image("lever.png"), new Image("leverhandle.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * obsidian block
	 */
	BTObsidian(54, true, BlockDrawType.BDTSolid, new Image[]
	{
		new Image("obsidian.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * piston
	 */
	BTPiston(55, false, BlockDrawType.BDTCustom, new Image[]
	{
	    new Image("piston.png"), new Image("extendedpistonbase.png")
	})
	{
		@Override
		public Block make(int orientation)
		{
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
		public Block make(int orientation, int vieworientation)
		{
			return make(vieworientation);
		}

		@Override
		public boolean use3DOrientation()
		{
			return true;
		}
	},
	/**
	 * sticky piston
	 */
	BTStickyPiston(56, false, BlockDrawType.BDTCustom, new Image[]
	{
	    new Image("stickypiston.png"), new Image("extendedpistonbase.png")
	})
	{
		@Override
		public Block make(int orientation)
		{
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
		public Block make(int orientation, int vieworientation)
		{
			return make(vieworientation);
		}

		@Override
		public boolean use3DOrientation()
		{
			return true;
		}
	},
	/**
	 * piston head
	 */
	BTPistonHead(57, false, BlockDrawType.BDTCustom, new Image[]
	{
	    new Image("extendedpistonhead.png"), new Image("pistonshaft.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * sticky piston
	 */
	BTStickyPistonHead(58, false, BlockDrawType.BDTCustom, new Image[]
	{
	    new Image("extendedstickypistonhead.png"), new Image("pistonshaft.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * slime
	 */
	BTSlime(59, false, BlockDrawType.BDTItem, new Image[]
	{
		new Image("slime.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * gunpowder
	 */
	BTGunpowder(60, false, BlockDrawType.BDTItem, new Image[]
	{
		new Image("gunpowder.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * TNT
	 */
	BTTNT(61, false, BlockDrawType.BDTSolid, new Image[]
	{
		new Image("tnt.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * blaze rod
	 */
	BTBlazeRod(62, false, BlockDrawType.BDTItem, new Image[]
	{
		new Image("blazerod.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * blaze powder
	 */
	BTBlazePowder(63, false, BlockDrawType.BDTItem, new Image[]
	{
		new Image("blazepowder.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * last block value, used to get <code>BlockType.Count</code>
	 */
	BTLast(64, false, BlockDrawType.BDTNone, null)
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * sun block, used for drawing sun
	 */
	BTSun(-1, true, BlockDrawType.BDTSolidAllSides, new Image[]
	{
		new Image("sun.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * moon block, used for drawing moon
	 */
	BTMoon(-2, true, BlockDrawType.BDTSolidAllSides, new Image[]
	{
		new Image("moon.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	/**
	 * delete block, used for drawing dig block animation
	 */
	BTDeleteBlock(-3, false, BlockDrawType.BDTCustom, new Image[]
	{
	    new Image("delete0.png"),
	    new Image("delete1.png"),
	    new Image("delete2.png"),
	    new Image("delete3.png"),
	    new Image("delete4.png"),
	    new Image("delete5.png"),
	    new Image("delete6.png"),
	    new Image("delete7.png")
	})
	{
		@Override
		public Block make(int orientation)
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
	public Image[] textures;
	/**
	 * 
	 */
	public final boolean isOpaque;
	/**
	 * 
	 */
	public final BlockDrawType drawType;

	/**
	 * @param orientation
	 *            the orientation for the new block, or -1 if none
	 * @return new block or null
	 */
	public abstract Block make(int orientation);

	/**
	 * @param orientation
	 *            the orientation for the side of the block clicked on
	 * @param vieworientation
	 *            the orientation for the direction the player is facing
	 * @return new block or null
	 */
	public Block make(int orientation, int vieworientation)
	{
		return make(orientation);
	}

	/**
	 * @return the amount of light emitted by this block or -1 if that must be
	 *         computed
	 */
	public abstract int getLight();

	/**
	 * @return true if this block needs to be drawn using double sided polygons
	 */
	public abstract boolean isDoubleSided();

	private BlockType(int newvalue,
	                  boolean newIsOpaque,
	                  BlockDrawType newDrawType,
	                  Image[] t)
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

	/**
	 * @param value
	 *            value of block to return
	 * @return <code>BlockType</code> of block specified by <code>value</code>
	 */
	public static BlockType toBlockType(int value)
	{
		if(value < 0 || value >= Count)
			return null;
		return blocks[value];
	}

	/**
	 * @return the on duration for buttons
	 */
	public int getOnTime()
	{
		return 0;
	}

	/**
	 * @return true if this block generates particles
	 */
	public abstract boolean isParticleGenerate();

	/**
	 * @return the block type generated by smelting this block
	 */
	public abstract BlockType getSmeltResult();

	/**
	 * @return the length of time that this block will fuel a furnace
	 */
	public abstract int getBurnTime();

	/**
	 * @return true if this block can be smelted
	 */
	public boolean isSmeltable()
	{
		if(getSmeltResult() == BlockType.BTEmpty)
			return false;
		return true;
	}

	/**
	 * @param needBreakToDig
	 *            true if the block being dug needs to be broken to dig it out
	 * @return the ability of this tool to dig
	 */
	public float getDigAbility(boolean needBreakToDig)
	{
		switch(this)
		{
		case BTDeleteBlock:
		case BTSun:
		case BTMoon:
		case BTLast:
			return -1;
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
		case BTStick:
			return -1;
		case BTWoodPick:
			return 1.5f;
		case BTStonePick:
			return 3;
		case BTWoodShovel:
			if(needBreakToDig)
				return 1.1f;
			return 2.2f;
		case BTStoneShovel:
			if(needBreakToDig)
				return 2;
			return 4;
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
			return -1;
		case BTIronPick:
			return 6;
		case BTIronShovel:
			if(needBreakToDig)
				return 4;
			return 8;
		case BTGoldPick:
			return 15;
		case BTGoldShovel:
			if(needBreakToDig)
				return 10;
			return 20;
		case BTDiamondPick:
			return 12;
		case BTDiamondShovel:
			if(needBreakToDig)
				return 8;
			return 16;
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
			return -1;
		}
		return -1;
	}

	/**
	 * @return true if this block needs to be broken to dig it out
	 */
	public boolean getNeedBreakToDig()
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
			return true;
		case BTGrass:
		case BTDirt:
			return false;
		case BTSapling:
		case BTBedrock:
			return true;
		case BTWater:
		case BTLava:
		case BTSand:
		case BTGravel:
			return false;
		case BTWood:
			return true;
		case BTLeaves:
			return false;
		case BTGlass:
		case BTChest:
		case BTWorkbench:
		case BTFurnace:
			return true;
		case BTPlank:
		case BTStick:
		case BTWoodPick:
		case BTStonePick:
		case BTWoodShovel:
		case BTStoneShovel:
		case BTRedstoneDustOff:
		case BTRedstoneDustOn:
			return false;
		case BTRedstoneOre:
			return true;
		case BTRedstoneBlock:
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
			return false;
		case BTLapisLazuli:
			return false;
		case BTLapisLazuliOre:
			return true;
		case BTGoldIngot:
			return false;
		case BTGoldOre:
			return false;
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
			return false;
		}
		return false;
	}

	/**
	 * @return the hardness of this block
	 */
	public int getHardness()
	{
		switch(this)
		{
		case BTDeleteBlock:
		case BTSun:
		case BTMoon:
		case BTLast:
			return -1;
		case BTEmpty:
			return -1;
		case BTStone:
			return 80;
		case BTCobblestone:
			return 40;
		case BTGrass:
		case BTDirt:
			return 20;
		case BTSapling:
			return 5;
		case BTBedrock:
			return -1;
		case BTWater:
			return 5;
		case BTLava:
			return 5;
		case BTSand:
		case BTGravel:
			return 10;
		case BTWood:
			return 30;
		case BTLeaves:
			return 5;
		case BTGlass:
			return 40;
		case BTChest:
			return 30;
		case BTWorkbench:
			return 30;
		case BTFurnace:
			return 50;
		case BTPlank:
		case BTStick:
		case BTWoodPick:
		case BTStonePick:
		case BTWoodShovel:
		case BTStoneShovel:
		case BTRedstoneDustOff:
		case BTRedstoneDustOn:
			return 5;
		case BTRedstoneOre:
			return 50;
		case BTRedstoneBlock:
			return 20;
		case BTRedstoneTorchOff:
		case BTRedstoneTorchOn:
		case BTStoneButton:
		case BTWoodButton:
			return 15;
		case BTCoal:
			return 5;
		case BTCoalOre:
			return 25;
		case BTIronIngot:
			return 20;
		case BTIronOre:
			return 30;
		case BTLapisLazuli:
			return 30;
		case BTLapisLazuliOre:
			return 30;
		case BTGoldIngot:
			return 10;
		case BTGoldOre:
			return 25;
		case BTDiamond:
			return 40;
		case BTDiamondOre:
			return 60;
		case BTEmerald:
			return 30;
		case BTEmeraldOre:
			return 30;
		case BTTorch:
			return 15;
		case BTIronPick:
		case BTIronShovel:
		case BTGoldPick:
		case BTGoldShovel:
		case BTDiamondPick:
		case BTDiamondShovel:
		case BTLadder:
			return 5;
		case BTRedstoneRepeaterOff:
		case BTRedstoneRepeaterOn:
			return 20;
		case BTLever:
			return 15;
		case BTObsidian:
			return 200;
		case BTPiston:
		case BTStickyPiston:
		case BTPistonHead:
		case BTStickyPistonHead:
			return 20;
		case BTSlime:
		case BTGunpowder:
			return 5;
		case BTTNT:
			return 15;
		case BTBlazeRod:
		case BTBlazePowder:
			return 5;
		}
		return -1;
	}

	/**
	 * get the maximum number of blocks to put in generated cave chests
	 * 
	 * @param y
	 *            the height of the new chest
	 * @return the maximum number of blocks to put in generated cave chests
	 */
	public int getChestGenCount(int y)
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
			return 2;
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
			return 5;
		case BTGunpowder:
			return 5;
		case BTTNT:
			return 0;
		case BTBlazeRod:
			return 1;
		case BTBlazePowder:
			return 0;
		}
		return 0;
	}

	/**
	 * @return true if this is an item
	 */
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
			return false;
		case BTPlank:
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
		}
		return false;
	}

	/**
	 * @return true if this block can support entities
	 */
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
			return true;
		case BTPlank:
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
			return false;
		}
		return false;
	}

	/**
	 * @return the growing time for this sapling
	 */
	public double getGrowTime()
	{
		return -1.0;
	}

	/**
	 * @return true if this block can be placed while the player is inside of it
	 */
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
			return true;
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
			return false;
		}
		return false;
	}

	/**
	 * write to a <code>DataOutput</code>
	 * 
	 * @param o
	 *            <code>OutputStream</code> to write to
	 * @throws IOException
	 *             the exception thrown
	 */
	public void write(DataOutput o) throws IOException
	{
		if(this.value < 0 || this.value >= Count)
			throw new IOException("tried to write special BlockType");
		o.writeShort(this.value);
	}

	/**
	 * read from a <code>DataInput</code>
	 * 
	 * @param i
	 *            <code>DataInput</code> to read from
	 * @return the read <code>BlockType</code>
	 * @throws IOException
	 *             the exception thrown
	 */
	public static BlockType read(DataInput i) throws IOException
	{
		int value = i.readUnsignedShort();
		if(value < 0 || value >= Count)
			throw new IOException("BlockType.value out of range");
		return toBlockType(value);
	}

	/**
	 * @return true if when making this block, you should use height to get the
	 *         view orientation
	 */
	public boolean use3DOrientation()
	{
		return false;
	}

	/**
	 * @return true if this block is explodable
	 */
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
			return true;
		case BTWater:
			return false;
		case BTWoodButton:
		case BTWoodPick:
		case BTWoodShovel:
		case BTLadder:
		case BTLever:
		case BTSlime:
		case BTLeaves:
			return true;
		case BTLava:
			return false;
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
			return true;
		case BTObsidian:
			return false;
		case BTPiston:
		case BTStickyPiston:
		case BTPistonHead:
		case BTStickyPistonHead:
		case BTGunpowder:
		case BTTNT:
		case BTBlazeRod:
		case BTBlazePowder:
			return true;
		}
		return false;
	}
}
