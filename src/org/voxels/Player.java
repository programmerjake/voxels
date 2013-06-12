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
import static org.voxels.Matrix.glLoadMatrix;
import static org.voxels.PlayerList.players;
import static org.voxels.Vector.glVertex;
import static org.voxels.World.world;

import java.io.*;

/**
 * @author jacob
 * 
 */
public class Player implements GameObject
{
	/**
	 * 
	 */
	public static final float PlayerHeight = 2.0f;
	private Vector position = new Vector(0.5f, 1.5f, 0.5f);
	private Vector velocity = new Vector(0);
	private float viewTheta = 0.0f;
	private float viewPhi = 0.0f;
	private static final float selectionDist = 8.0f;
	private boolean paused = false, wasPaused = true;
	private int blockCount[] = new int[BlockType.Count];
	private int curBlockType = 0;
	private boolean isShiftDown = false;
	private int lastMouseX = -1, lastMouseY = -1;
	private static final float distLimit = 0.2f;
	private int furnaceSelection = -1;

	private enum State
	{
		Normal, Workbench, Chest, Furnace
	}

	private State state = State.Normal;
	private static final int workbenchMaxSize = 3;
	private int workbenchSize = 2;
	private int workbenchSelX = 1;
	private int workbenchSelY = 1;
	private int blockX = 0;
	private int blockY = 0;
	private int blockZ = 0;
	private int blockOrientation = -1;
	private int chestCurBlockType = 0;
	private BlockType workbench[] = new BlockType[workbenchMaxSize
	        * workbenchMaxSize];
	private float mouseDownTime = 0;
	private float deleteAnimTime = 0;

	/**
	 * 
	 */
	public Player()
	{
		this.position.y = Math.max(this.position.y,
		                           this.position.y
		                                   + world.getLandHeight((int)Math.floor(this.position.x),
		                                                         (int)Math.floor(this.position.z))
		                                   + 2);
	}

	/**
	 * @return true if this player is in normal(3D) state
	 */
	public boolean isNormalState()
	{
		if(this.state == State.Normal)
			return true;
		return false;
	}

	/**
	 * @return true if this player is paused
	 */
	public boolean isPaused()
	{
		return this.paused;
	}

	/**
	 * @return the transformation that converts world coordinates to this
	 *         player's camera coordinates
	 */
	public Matrix getWorldToCamera()
	{
		return Matrix.translate(this.position.neg())
		             .concat(Matrix.rotatey(this.viewTheta))
		             .concat(Matrix.rotatex(this.viewPhi));
	}

	/**
	 * @return the vector pointing in the direction this player is looking
	 */
	public Vector getForwardVector()
	{
		return Matrix.rotatex(-this.viewPhi)
		             .concat(Matrix.rotatey(-this.viewTheta))
		             .apply(new Vector(0.0f, 0.0f, -1.0f));
	}

	/**
	 * @return the vector pointing in the direction this player is facing
	 */
	public Vector getMoveForwardVector()
	{
		return Matrix.rotatey(-this.viewTheta).apply(new Vector(0.0f,
		                                                        0.0f,
		                                                        -1.0f));
	}

	/**
	 * @return this player's position
	 */
	public Vector getPosition()
	{
		return this.position;
	}

	private Block getSelectedBlock()
	{
		World.BlockHitDescriptor bhd = world.getPointedAtBlock(getWorldToCamera(),
		                                                       selectionDist,
		                                                       this.isShiftDown);
		this.blockX = bhd.x;
		this.blockY = bhd.y;
		this.blockZ = bhd.z;
		this.blockOrientation = bhd.orientation;
		return bhd.b;
	}

	private void internalDrawSelectedBlockH(float minX,
	                                        float maxX,
	                                        float minY,
	                                        float maxY,
	                                        float minZ,
	                                        float maxZ)
	{
		glVertex3f(minX, minY, minZ);
		glVertex3f(maxX, minY, minZ);
		glVertex3f(minX, minY, minZ);
		glVertex3f(minX, maxY, minZ);
		glVertex3f(maxX, maxY, minZ);
		glVertex3f(maxX, minY, minZ);
		glVertex3f(maxX, maxY, minZ);
		glVertex3f(minX, maxY, minZ);
		glVertex3f(minX, minY, maxZ);
		glVertex3f(maxX, minY, maxZ);
		glVertex3f(minX, minY, maxZ);
		glVertex3f(minX, maxY, maxZ);
		glVertex3f(maxX, maxY, maxZ);
		glVertex3f(maxX, minY, maxZ);
		glVertex3f(maxX, maxY, maxZ);
		glVertex3f(minX, maxY, maxZ);
		glVertex3f(minX, minY, minZ);
		glVertex3f(minX, minY, maxZ);
		glVertex3f(maxX, minY, minZ);
		glVertex3f(maxX, minY, maxZ);
		glVertex3f(maxX, maxY, minZ);
		glVertex3f(maxX, maxY, maxZ);
		glVertex3f(minX, maxY, minZ);
		glVertex3f(minX, maxY, maxZ);
	}

	void internalDrawSelectedBlock(float minX,
	                               float maxX,
	                               float minY,
	                               float maxY,
	                               float minZ,
	                               float maxZ)
	{
		final float selectionExpand = 0.05f;
		glColor4f(0.75f, 0.75f, 0.75f, 0);
		Image.unselectTexture();
		glBegin(GL_LINES);
		for(int dx = -1; dx <= 1; dx += 2)
			for(int dy = -1; dy <= 1; dy += 2)
				for(int dz = -1; dz <= 1; dz += 2)
					internalDrawSelectedBlockH(minX - selectionExpand * dx,
					                           maxX + selectionExpand * dx,
					                           minY - selectionExpand * dy,
					                           maxY + selectionExpand * dy,
					                           minZ - selectionExpand * dz,
					                           maxZ + selectionExpand * dz);
		glEnd();
	}

	private void drawBlockSelection(Matrix world2Camera, int x, int y, int z)
	{
		glMatrixMode(GL_MODELVIEW);
		glPushMatrix();
		glLoadMatrix(world2Camera);
		internalDrawSelectedBlock(x, x + 1, y, y + 1, z, z + 1);
		glMatrixMode(GL_MODELVIEW);
		glPopMatrix();
	}

	private static Image workbenchImg = new Image("workbench.png");
	private static Image furnaceImg = new Image("furnace.png");
	private static final float workbenchZDist = -10.0f;
	private static final float workbenchScale = 2.0f;
	private static final float workbenchResultX = 6.5f;
	private static final float furnaceZDist = -10.0f;
	private static final float furnaceScale = 2.0f;
	private static final float furnaceFireX = 0.0f, furnaceFireY = 0.0f;
	private static final float furnaceDestX = 5.0f, furnaceDestY = 0.0f;
	private static final float furnaceSrcX = 0, furnaceSrcY = 3;

	/**
	 * draw everything from this player's perspective
	 */
	public void drawAll()
	{
		Matrix worldToCamera = getWorldToCamera();
		world.draw(worldToCamera); // must call draw world first
		players.drawPlayers(worldToCamera);
		if(this.state == State.Normal)
		{
			Block b = getSelectedBlock();
			if(b != null)
				drawBlockSelection(worldToCamera,
				                   this.blockX,
				                   this.blockY,
				                   this.blockZ);
			if(this.deleteAnimTime >= 0)
			{
				b = Block.NewDeleteAnim(this.deleteAnimTime);
				b.draw(Matrix.translate(this.blockX, this.blockY, this.blockZ)
				             .concat(worldToCamera));
			}
		}
		glClear(GL_DEPTH_BUFFER_BIT);
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();
		Image.unselectTexture();
		glColor4f(1, 1, 1, 0);
		glBegin(GL_LINES);
		glVertex3f(-1, 0, -100);
		glVertex3f(1, 0, -100);
		glVertex3f(1, 0, -100);
		glVertex3f(-1, 0, -100);
		glVertex3f(0, -1, -100);
		glVertex3f(0, 1, -100);
		glVertex3f(0, 1, -100);
		glVertex3f(0, -1, -100);
		glEnd();
		switch(this.state)
		{
		case Normal:
		{
			break;
		}
		case Workbench:
		{
			glMatrixMode(GL_MODELVIEW);
			glLoadIdentity();
			workbenchImg.selectTexture();
			glColor3f(1, 1, 1);
			glBegin(GL_QUADS);
			glTexCoord2f(0, 0);
			glVertex3f(-8, -8, workbenchZDist - 0.1f);
			glTexCoord2f(1, 0);
			glVertex3f(8, -8, workbenchZDist - 0.1f);
			glTexCoord2f(1, 1);
			glVertex3f(8, 8, workbenchZDist - 0.1f);
			glTexCoord2f(0, 1);
			glVertex3f(-8, 8, workbenchZDist - 0.1f);
			glEnd();
			glClear(GL_DEPTH_BUFFER_BIT);
			for(int x = 0; x <= this.workbenchSize; x++)
			{
				for(int y = 0; y <= this.workbenchSize; y++)
				{
					Matrix blocktransform = Matrix.translate(x
					                                                 - this.workbenchSize
					                                                 / 2.0f,
					                                         y
					                                                 - this.workbenchSize
					                                                 / 2.0f,
					                                         0.0f)
					                              .concat(Matrix.scale(workbenchScale))
					                              .concat(Matrix.translate(0,
					                                                       0,
					                                                       workbenchZDist));
					if(x < this.workbenchSize)
					{
						Image.unselectTexture();
						glColor4f(1, 1, 1, 0);
						glBegin(GL_LINES);
						glVertex(blocktransform.apply(new Vector(0, 0, 0)));
						glVertex(blocktransform.apply(new Vector(1, 0, 0)));
						glEnd();
					}
					if(y < this.workbenchSize)
					{
						Image.unselectTexture();
						glColor4f(1, 1, 1, 0);
						glBegin(GL_LINES);
						glVertex(blocktransform.apply(new Vector(0, 0, 0)));
						glVertex(blocktransform.apply(new Vector(0, 1, 0)));
						glEnd();
					}
					if(x >= this.workbenchSize || y >= this.workbenchSize)
						continue;
					Block b = Block.make(this.workbench[x + this.workbenchSize
					        * y]);
					if(b != null)
					{
						b.drawAsItem(blocktransform);
					}
					if(x == this.workbenchSelX && y == this.workbenchSelY)
					{
						Vector minv = blocktransform.apply(new Vector(0));
						Vector maxv = blocktransform.apply(new Vector(1));
						internalDrawSelectedBlock(minv.x,
						                          maxv.x,
						                          minv.y,
						                          maxv.y,
						                          minv.z,
						                          maxv.z);
					}
				}
			}
			int count = 0;
			Block b = null;
			Block.ReduceDescriptor rd = Block.reduce(this.workbench,
			                                         this.workbenchSize);
			if(!rd.isEmpty())
			{
				b = Block.make(rd.b);
				count = rd.count;
			}
			if(b != null)
			{
				Matrix blockTransform = Matrix.translate(-0.5f, -0.5f, 0.0f)
				                              .concat(Matrix.scale(workbenchScale))
				                              .concat(Matrix.translate(workbenchResultX,
				                                                       0.0f,
				                                                       workbenchZDist));
				b.drawAsItem(blockTransform);
				blockTransform = Matrix.translate(0.5f, -0.5f, 0.0f)
				                       .concat(blockTransform);
				blockTransform = Matrix.scale(0.5f / workbenchScale)
				                       .concat(blockTransform);
				blockTransform = Matrix.translate(-(float)Text.sizeW(Integer.toString(count))
				                                          / Text.sizeW("A")
				                                          / 2.0f,
				                                  0.0f,
				                                  0.0f)
				                       .concat(blockTransform);
				Text.draw(blockTransform, Integer.toString(count));
			}
			break;
		}
		case Chest:
		{
			glMatrixMode(GL_MODELVIEW);
			glLoadIdentity();
			Image.unselectTexture();
			glColor4f(0.8f, 0.51f, 0.21f, 0.0f);
			final float minx = -2.0f, maxx = 2.0f, miny = -6.5f, maxy = 7.5f, zdist = -9.9f;
			glBegin(GL_QUADS);
			glVertex3f(minx, miny, zdist);
			glVertex3f(maxx, miny, zdist);
			glVertex3f(maxx, maxy + 1, zdist);
			glVertex3f(minx, maxy + 1, zdist);
			glEnd();
			glClear(GL_DEPTH_BUFFER_BIT);
			final String title = "Chest";
			final float titleScale = 0.5f;
			Text.draw(Matrix.scale(titleScale)
			                .concat(Matrix.translate(-titleScale
			                                                 * Text.sizeW(title)
			                                                 / Text.sizeW("A")
			                                                 / 2.0f,
			                                         maxy,
			                                         zdist + 0.01f)),
			          title);
			Block chest = world.getBlock(this.blockX, this.blockY, this.blockZ);
			if(chest == null || chest.getType() != BlockType.BTChest)
			{
				chest = Block.NewChest();
				this.state = State.Normal;
			}
			int blocktypecount = 0, blocktypeindex = -1;
			Main.addToFrameText("chestCurBlockType = " + this.chestCurBlockType
			        + "\n");
			for(int i = 1; i < BlockType.Count; i++)
			{
				if(chest.chestGetBlockTypeCount(BlockType.toBlockType(i)) > 0)
				{
					if(i == this.chestCurBlockType)
						blocktypeindex = blocktypecount;
					blocktypecount++;
				}
			}
			int startpos = blocktypeindex - 5;
			if(startpos < -1)
				startpos = -1;
			int endpos = blocktypeindex + 5;
			if(endpos > blocktypecount - 1)
				endpos = blocktypecount - 1;
			for(int i = startpos; i <= endpos; i++)
			{
				float y = i - blocktypeindex;
				Block b = null;
				int count = 0;
				for(int j = 1, k = 0; j < BlockType.Count; j++)
				{
					if(chest.chestGetBlockTypeCount(BlockType.toBlockType(j)) > 0)
					{
						if(k == i)
						{
							b = Block.make(BlockType.toBlockType(j));
							count = chest.chestGetBlockTypeCount(BlockType.toBlockType(j));
							break;
						}
						k++;
					}
				}
				if(b != null)
				{
					float scaleFactor = 0.8f;
					if(y < 0)
						y -= 0.5f;
					if(y > 0)
						y += 0.5f;
					if(y == 0)
						scaleFactor = 1.6f;
					Matrix blocktform = Matrix.translate(-0.5f, -0.5f, 0.0f)
					                          .concat(Matrix.scale(scaleFactor))
					                          .concat(Matrix.translate((minx + maxx) / 2.0f,
					                                                   (miny + maxy)
					                                                           / 2.0f
					                                                           + y,
					                                                   zdist + 0.01f));
					b.drawAsItem(blocktform);
					blocktform = Matrix.translate(1.5f, 0.5f, 0.0f)
					                   .concat(blocktform);
					blocktform = Matrix.scale(0.5f / scaleFactor)
					                   .concat(blocktform);
					blocktform = Matrix.translate(-(float)Text.sizeW(Integer.toString(count))
					                                      / Text.sizeW("A")
					                                      / 2.0f,
					                              0,
					                              0)
					                   .concat(blocktform);
					Text.draw(blocktform, Integer.toString(count));
				}
			}
			break;
		}
		case Furnace:
		{
			glMatrixMode(GL_MODELVIEW);
			glLoadIdentity();
			furnaceImg.selectTexture();
			glColor3f(1, 1, 1);
			glBegin(GL_QUADS);
			glTexCoord2f(0, 0);
			glVertex3f(-8, -8, furnaceZDist - 0.1f);
			glTexCoord2f(1, 0);
			glVertex3f(8, -8, furnaceZDist - 0.1f);
			glTexCoord2f(1, 1);
			glVertex3f(8, 8, furnaceZDist - 0.1f);
			glTexCoord2f(0, 1);
			glVertex3f(-8, 8, furnaceZDist - 0.1f);
			glEnd();
			glClear(GL_DEPTH_BUFFER_BIT);
			Block furnace = world.getBlock(this.blockX,
			                               this.blockY,
			                               this.blockZ);
			if(furnace == null || furnace.getType() != BlockType.BTFurnace)
				furnace = Block.NewFurnace();
			int count = furnace.furnaceGetDestBlockCount();
			Block b = Block.make(furnace.furnaceGetDestBlockType());
			if(b != null)
			{
				Matrix blockTransform = Matrix.translate(-0.5f, -0.5f, 0.0f)
				                              .concat(Matrix.scale(furnaceScale))
				                              .concat(Matrix.translate(furnaceDestX,
				                                                       furnaceDestY,
				                                                       furnaceZDist));
				if(this.furnaceSelection == 2)
				{
					Vector minv = blockTransform.apply(new Vector(0));
					Vector maxv = blockTransform.apply(new Vector(1));
					internalDrawSelectedBlock(minv.x,
					                          maxv.x,
					                          minv.y,
					                          maxv.y,
					                          minv.z,
					                          maxv.z);
				}
				b.drawAsItem(blockTransform);
				blockTransform = Matrix.translate(0.5f, -0.5f, 0.0f)
				                       .concat(blockTransform);
				blockTransform = Matrix.scale(0.5f / furnaceScale)
				                       .concat(blockTransform);
				blockTransform = Matrix.translate(-(float)Text.sizeW(Integer.toString(count))
				                                          / Text.sizeW("A")
				                                          / 2.0f,
				                                  0.0f,
				                                  0.0f)
				                       .concat(blockTransform);
				Text.draw(blockTransform, Integer.toString(count));
			}
			count = furnace.furnaceGetSrcBlockCount();
			b = Block.make(furnace.furnaceGetSrcBlockType());
			{
				Matrix blockTransform = Matrix.translate(-0.5f, -0.5f, 0.0f)
				                              .concat(Matrix.scale(furnaceScale))
				                              .concat(Matrix.translate(furnaceSrcX,
				                                                       furnaceSrcY,
				                                                       furnaceZDist));
				if(this.furnaceSelection == 1)
				{
					Vector minv = blockTransform.apply(new Vector(0));
					Vector maxv = blockTransform.apply(new Vector(1));
					internalDrawSelectedBlock(minv.x,
					                          maxv.x,
					                          minv.y,
					                          maxv.y,
					                          minv.z,
					                          maxv.z);
				}
				if(b != null)
				{
					b.drawAsItem(blockTransform);
					blockTransform = Matrix.translate(0.5f, -0.5f, 0.0f)
					                       .concat(blockTransform);
					blockTransform = Matrix.scale(0.5f / furnaceScale)
					                       .concat(blockTransform);
					blockTransform = Matrix.translate(-(float)Text.sizeW(Integer.toString(count))
					                                          / Text.sizeW("A")
					                                          / 2.0f,
					                                  0.0f,
					                                  0.0f)
					                       .concat(blockTransform);
					Text.draw(blockTransform, Integer.toString(count));
				}
			}
			count = furnace.furnaceGetFuelLeft();
			{
				Matrix blockTransform = Matrix.translate(-0.5f, -0.5f, 0.0f)
				                              .concat(Matrix.scale(furnaceScale))
				                              .concat(Matrix.translate(furnaceFireX,
				                                                       furnaceFireY,
				                                                       furnaceZDist));
				if(this.furnaceSelection == 0)
				{
					Vector minv = blockTransform.apply(new Vector(0));
					Vector maxv = blockTransform.apply(new Vector(1));
					internalDrawSelectedBlock(minv.x,
					                          maxv.x,
					                          minv.y,
					                          maxv.y,
					                          minv.z,
					                          maxv.z);
				}
				blockTransform = Matrix.translate(0.5f, -0.5f, 0.0f)
				                       .concat(blockTransform);
				blockTransform = Matrix.scale(0.5f / furnaceScale)
				                       .concat(blockTransform);
				blockTransform = Matrix.translate(-(float)Text.sizeW(Integer.toString(count))
				                                          / Text.sizeW("A")
				                                          / 2.0f,
				                                  0.0f,
				                                  0.0f)
				                       .concat(blockTransform);
				Text.draw(blockTransform, Integer.toString(count));
			}
			break;
		}
		}
		glClear(GL_DEPTH_BUFFER_BIT);
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();
		Image.unselectTexture();
		glColor4f(1.0f, 1.0f, 1.0f, 0.25f);
		final float minx = -7, maxx = 7, miny = -9.5f, maxy = -7, zdist = -9.9f;
		glBegin(GL_QUADS);
		glVertex3f(minx, miny, zdist);
		glVertex3f(maxx, miny, zdist);
		glVertex3f(maxx, maxy, zdist);
		glVertex3f(minx, maxy, zdist);
		glEnd();
		glClear(GL_DEPTH_BUFFER_BIT);
		int blocktypecount = 0, blocktypeindex = -1;
		for(int i = 1; i < BlockType.Count; i++)
		{
			if(this.blockCount[i] > 0)
			{
				if(i == this.curBlockType)
					blocktypeindex = blocktypecount;
				blocktypecount++;
			}
		}
		int startpos = blocktypeindex - 5;
		if(startpos < -1)
			startpos = -1;
		int endpos = blocktypeindex + 5;
		if(endpos > blocktypecount - 1)
			endpos = blocktypecount - 1;
		for(int i = startpos; i <= endpos; i++)
		{
			float x = i - blocktypeindex;
			Block b = null;
			int count = 0;
			for(int j = 1, k = 0; j < BlockType.Count; j++)
			{
				if(this.blockCount[j] > 0)
				{
					if(k == i)
					{
						b = Block.make(BlockType.toBlockType(j));
						count = this.blockCount[j];
						break;
					}
					k++;
				}
			}
			if(b != null)
			{
				float scaleFactor = 0.8f;
				if(x < 0)
					x -= 0.5f;
				if(x > 0)
					x += 0.5f;
				if(x == 0)
					scaleFactor = 1.6f;
				Matrix blocktform = Matrix.translate(-0.5f, -0.5f, 0)
				                          .concat(Matrix.scale(scaleFactor))
				                          .concat(Matrix.translate(x,
				                                                   (miny + (maxy - 1.0f)) / 2.0f,
				                                                   zdist + 0.01f));
				b.drawAsItem(blocktform);
				blocktform = Matrix.translate(0.5f, 1.2f, 0.0f)
				                   .concat(blocktform);
				float textSize = (float)Text.sizeW(Integer.toString(count))
				        / Text.sizeW("A"), textScale = 0.8f / textSize;
				if(x == 0 && textScale < 0.5f)
					textScale = 0.5f;
				blocktform = Matrix.scale(textScale / scaleFactor)
				                   .concat(blocktform);
				blocktform = Matrix.translate(-textSize * textScale / 2.0f,
				                              0.0f,
				                              0.0f).concat(blocktform);
				Text.draw(blocktform,
				          Color.RGB(0.5f, 0.5f, 0.5f),
				          Integer.toString(count));
			}
		}
		if(this.paused)
		{
			glMatrixMode(GL_MODELVIEW);
			glLoadIdentity();
			Image.unselectTexture();
			glColor4f(0.5f, 0.5f, 0.5f, 0.125f);
			glBegin(GL_QUADS);
			glVertex3f(-1, -1, -1);
			glVertex3f(1, -1, -1);
			glVertex3f(1, 1, -1);
			glVertex3f(-1, 1, -1);
			glEnd();
		}
	}

	@Override
	public void draw(Matrix worldToCamera)
	{
		// TODO Auto-generated method stub
	}

	/**
	 * set this player's position
	 * 
	 * @param pos
	 *            the new position
	 */
	public void setPosition(Vector pos)
	{
		this.position = new Vector(pos);
		this.velocity = new Vector(0);
	}

	private boolean mousePosToSelPosWorkbench(int mouseX, int mouseY)
	{
		float x = (float)mouseX / Main.ScreenXRes * 2.0f - 1.0f;
		float y = 1.0f - (float)mouseY / Main.ScreenYRes * 2.0f;
		x *= -workbenchZDist / workbenchScale;
		y *= -workbenchZDist / workbenchScale;
		x += this.workbenchSize / 2.0;
		y += this.workbenchSize / 2.0;
		x = (float)Math.floor(x);
		y = (float)Math.floor(y);
		if(x >= 0 && y >= 0 && x < this.workbenchSize && y < this.workbenchSize)
		{
			this.workbenchSelX = (int)x;
			this.workbenchSelY = (int)y;
			return true;
		}
		return false;
	}

	private boolean mouseIsInResultWorkbench(int mouseX, int mouseY)
	{
		float x = (float)mouseX / Main.ScreenXRes * 2.0f - 1.0f;
		float y = 1.0f - (float)mouseY / Main.ScreenYRes * 2.0f;
		x *= -workbenchZDist;
		y *= -workbenchZDist;
		x -= workbenchResultX;
		x /= workbenchScale;
		y /= workbenchScale;
		if(x < -0.5 || x > 0.5 || y < -0.5 || y > 0.5)
			return false;
		return true;
	}

	private boolean mouseIsInResultFurnace(int mouseX, int mouseY)
	{
		float x = (float)mouseX / Main.ScreenXRes * 2.0f - 1.0f;
		float y = 1.0f - (float)mouseY / Main.ScreenYRes * 2.0f;
		x *= -furnaceZDist;
		y *= -furnaceZDist;
		x -= furnaceDestX;
		y -= furnaceDestY;
		x /= furnaceScale;
		y /= furnaceScale;
		if(x < -0.5 || x > 0.5 || y < -0.5 || y > 0.5)
			return false;
		return true;
	}

	private boolean mouseIsInFireFurnace(int mouseX, int mouseY)
	{
		float x = (float)mouseX / Main.ScreenXRes * 2.0f - 1.0f;
		float y = 1.0f - (float)mouseY / Main.ScreenYRes * 2.0f;
		x *= -furnaceZDist;
		y *= -furnaceZDist;
		x -= furnaceFireX;
		y -= furnaceFireY;
		x /= furnaceScale;
		y /= furnaceScale;
		if(x < -0.5 || x > 0.5 || y < -0.5 || y > 0.5)
			return false;
		return true;
	}

	private boolean mouseIsInSourceFurnace(int mouseX, int mouseY)
	{
		float x = (float)mouseX / Main.ScreenXRes * 2.0f - 1.0f;
		float y = 1.0f - (float)mouseY / Main.ScreenYRes * 2.0f;
		x *= -furnaceZDist;
		y *= -furnaceZDist;
		x -= furnaceSrcX;
		y -= furnaceSrcY;
		x /= furnaceScale;
		y /= furnaceScale;
		if(x < -0.5 || x > 0.5 || y < -0.5 || y > 0.5)
			return false;
		return true;
	}

	/**
	 * 
	 * @param mouseX
	 *            mouse x position
	 * @param mouseY
	 *            mouse y position
	 * @param mouseLButton
	 *            if the mouse's left button is pressed
	 * @return true to move mouse to center
	 */
	public boolean
	    handleMouseMove(int mouseX, int mouseY, boolean mouseLButton)
	{
		if(this.paused)
		{
			this.wasPaused = true;
			this.deleteAnimTime = -1;
			return false;
		}
		switch(this.state)
		{
		case Normal:
		{
			if(this.wasPaused)
			{
				this.wasPaused = false;
				this.deleteAnimTime = -1;
				return true;
			}
			this.wasPaused = false;
			this.viewPhi += (mouseY - (Main.ScreenYRes / 2)) / 100.0;
			if(this.viewPhi < -Math.PI / 2)
				this.viewPhi = -(float)Math.PI / 2;
			else if(this.viewPhi > Math.PI / 2)
				this.viewPhi = (float)Math.PI / 2;
			this.viewTheta += (mouseX - (Main.ScreenXRes / 2)) / 100.0;
			this.viewTheta %= (float)(Math.PI * 2);
			if(mouseLButton)
			{
				int x = this.blockX, y = this.blockY, z = this.blockZ;
				Block b = getSelectedBlock();
				boolean isSameBlock = true;
				if(this.blockX != x)
					isSameBlock = false;
				if(this.blockY != y)
					isSameBlock = false;
				if(this.blockZ != z)
					isSameBlock = false;
				int deleteTime = -1;
				float timeDivisor = -1;
				if(this.curBlockType > 0 && b != null)
					timeDivisor = BlockType.toBlockType(this.curBlockType)
					                       .getDigAbility(b.getNeedBreakToDig());
				if(timeDivisor <= 0)
					timeDivisor = 1;
				if(b != null)
					deleteTime = b.getHardness();
				float deletePeriod = 0.05f * deleteTime / timeDivisor;
				if(deletePeriod < 0.25)
					deletePeriod = 0.25f;
				this.mouseDownTime += Main.getFrameDuration();
				if(b == null || !b.canDig() || !isSameBlock || deleteTime == -1
				        || deletePeriod > 5)
				{
					this.mouseDownTime = 0;
					this.deleteAnimTime = -1;
					return true;
				}
				this.deleteAnimTime = this.mouseDownTime / deletePeriod;
				if(this.mouseDownTime >= deletePeriod)
				{
					this.deleteAnimTime = -1;
					this.mouseDownTime = 0;
					// TODO finish
					// world.addModNode(blockX, blockY, blockZ, new Block());
					world.setBlock(this.blockX,
					               this.blockY,
					               this.blockZ,
					               new Block());
					b.digBlock(this.blockX, this.blockY, this.blockZ);
					Main.play(Main.destructAudio);
				}
			}
			return true;
		}
		case Workbench:
		{
			this.wasPaused = true;
			this.deleteAnimTime = -1;
			if(mouseX != this.lastMouseX || mouseY != this.lastMouseY)
			{
				this.lastMouseX = mouseX;
				this.lastMouseY = mouseY;
				mousePosToSelPosWorkbench(mouseX, mouseY);
			}
			return false;
		}
		case Chest:
		{
			this.wasPaused = true;
			this.deleteAnimTime = -1;
			return false;
		}
		case Furnace:
		{
			this.wasPaused = true;
			this.deleteAnimTime = -1;
			this.furnaceSelection = -1;
			if(mouseIsInFireFurnace(mouseX, mouseY))
			{
				this.furnaceSelection = 0;
			}
			else if(mouseIsInSourceFurnace(mouseX, mouseY))
			{
				this.furnaceSelection = 1;
			}
			else if(mouseIsInResultFurnace(mouseX, mouseY))
			{
				this.furnaceSelection = 2;
			}
			return false;
		}
		}
		return false;
	}

	private void nextCurBlockType()
	{
		if(this.curBlockType > 0)
		{
			for(int i = this.curBlockType + 1; i < BlockType.Count; i++)
			{
				if(this.blockCount[i] > 0)
				{
					this.curBlockType = i;
					return;
				}
			}
			this.curBlockType = 0;
			return;
		}
		for(int i = 1; i < BlockType.Count; i++)
		{
			if(this.blockCount[i] > 0)
			{
				this.curBlockType = i;
				return;
			}
		}
	}

	private void prevCurBlockType()
	{
		if(this.curBlockType > 0)
		{
			for(int i = this.curBlockType - 1; i > 0; i--)
			{
				if(this.blockCount[i] > 0)
				{
					this.curBlockType = i;
					return;
				}
			}
			this.curBlockType = 0;
			return;
		}
		for(int i = BlockType.Count - 1; i > 0; i--)
		{
			if(this.blockCount[i] > 0)
			{
				this.curBlockType = i;
				return;
			}
		}
	}

	/**
	 * give this player a block
	 * 
	 * @param bt
	 *            the type of block to give
	 */
	public void giveBlock(BlockType bt)
	{
		giveBlock(bt, true);
	}

	/**
	 * give this player a block
	 * 
	 * @param bt
	 *            the type of block to give
	 * @param setCurrentBlock
	 *            if <code>bt</code> should be set as the players currently
	 *            selected block
	 */
	public void giveBlock(BlockType bt, boolean setCurrentBlock)
	{
		if(bt == BlockType.BTEmpty)
		{
			if(setCurrentBlock)
				this.curBlockType = 0;
			return;
		}
		int index = bt.ordinal();
		if(index < 1 || index >= BlockType.Count)
		{
			if(setCurrentBlock)
				this.curBlockType = 0;
			return;
		}
		this.blockCount[index]++;
		if(setCurrentBlock)
			this.curBlockType = index;
	}

	/**
	 * @return the block taken from this player or
	 *         <code>BlockType.BTEmpty</code>
	 */
	public BlockType takeBlock()
	{
		BlockType retval = BlockType.BTEmpty;
		if(this.curBlockType <= 0)
		{
			return BlockType.BTEmpty;
		}
		retval = BlockType.toBlockType(this.curBlockType);
		if(this.blockCount[this.curBlockType] > 1)
		{
			this.blockCount[this.curBlockType]--;
		}
		else
		{
			this.blockCount[this.curBlockType] = 0;
			for(int i = this.curBlockType; i < BlockType.Count; i++)
			{
				if(this.blockCount[i] > 0)
				{
					this.curBlockType = i;
					return retval;
				}
			}
			this.curBlockType = 0;
			nextCurBlockType();
		}
		return retval;
	}

	/**
	 * @param bt
	 *            the kind of block to take
	 * @return the block taken from this player or
	 *         <code>BlockType.BTEmpty</code>
	 */
	public BlockType takeBlock(BlockType bt)
	{
		if(bt == BlockType.BTEmpty)
			return BlockType.BTEmpty;
		int index = bt.ordinal();
		if(index < 1 || index >= BlockType.Count)
			return BlockType.BTEmpty;
		if(index == this.curBlockType)
			return takeBlock();
		if(this.blockCount[index] > 0)
			this.blockCount[index]--;
		else
			return BlockType.BTEmpty;
		return bt;
	}

	private void runWorkbenchReduce()
	{
		int count;
		Block.ReduceDescriptor rd = Block.reduce(this.workbench,
		                                         this.workbenchSize);
		BlockType b = rd.b;
		count = rd.count;
		if(!rd.isEmpty())
		{
			for(int x = 0; x < this.workbenchSize; x++)
			{
				for(int y = 0; y < this.workbenchSize; y++)
				{
					this.workbench[x + this.workbenchSize * y] = takeBlock(this.workbench[x
					        + this.workbenchSize * y]);
				}
			}
			for(int i = 0; i < count; i++)
			{
				giveBlock(b, true);
			}
		}
	}

	private boolean canPlaceBlock(Block selb, int bx, int by, int bz)
	{
		if(selb == null || !this.isShiftDown || this.state != State.Normal
		        || this.curBlockType <= 0
		        || BlockType.toBlockType(this.curBlockType) == null
		        || BlockType.toBlockType(this.curBlockType).isItem())
			return false;
		if(Math.floor(this.position.x) == bx
		        && Math.floor(this.position.y) == by
		        && Math.floor(this.position.z) == bz
		        && !BlockType.toBlockType(this.curBlockType)
		                     .isPlaceableWhileInside())
			return false;
		if(selb.getType() != BlockType.BTEmpty
		        && selb.getType() != BlockType.BTWater)
			return false;
		Vector relpos = this.position.sub(new Vector(bx, by, bz));
		if(selb.adjustPlayerPosition(relpos, distLimit) == null)
			return false;
		return true;
	}

	private void internalSetPositionH(Vector pos)
	{
		Vector newpos = new Vector(pos);
		int x = (int)Math.floor(newpos.x), y = (int)Math.floor(newpos.y), z = (int)Math.floor(newpos.z);
		{
			Block b1 = world.getBlockEval(x, y, z);
			Block b2 = world.getBlockEval(x, y - 1, z);
			boolean setPosition = false;
			while((b1 != null && b1.adjustPlayerPosition(newpos.sub(new Vector(x,
			                                                                   y,
			                                                                   z)),
			                                             distLimit) == null)
			        || (b2 != null && b2.adjustPlayerPosition(newpos.sub(new Vector(x,
			                                                                        y - 1,
			                                                                        z)),
			                                                  distLimit) == null))
			{
				y++;
				newpos.y = y + distLimit + 1e-3f - (PlayerHeight - 1.0f)
				        + b2.getHeight();
				b1 = world.getBlockEval(x, y, z);
				b2 = world.getBlockEval(x, y - 1, z);
				setPosition = true;
			}
			if(setPosition)
			{
				this.position = new Vector(newpos);
				this.velocity = new Vector(0);
			}
		}
		for(int dx = -1; dx <= 1; dx++)
		{
			for(int dy = -2; dy <= 1; dy++)
			{
				for(int dz = -1; dz <= 1; dz++)
				{
					for(int i = 0; i < 1; i++)
					{
						Block b = world.getBlock(x + dx, y + dy - i, z + dz);
						if(b == null)
							return;
						Vector bpos = new Vector(x + dx, y + dy - i, z + dz);
						Vector relpos = newpos.sub(bpos);
						relpos = b.adjustPlayerPosition(relpos, distLimit);
						if(relpos == null)
							return;
						newpos = relpos.add(bpos);
					}
				}
			}
		}
		for(int dx = -1; dx <= 1; dx++)
		{
			for(int dy = -2; dy <= 1; dy++)
			{
				for(int dz = -1; dz <= 1; dz++)
				{
					for(int i = 0; i < 1; i++)
					{
						Block b = world.getBlock(x + dx, y + dy - i, z + dz);
						if(b == null)
							return;
						Vector bpos = new Vector(x + dx, y + dy - i, z + dz);
						Vector relpos = newpos.sub(bpos);
						relpos = b.adjustPlayerPosition(relpos, distLimit);
						if(relpos == null)
							return;
						newpos = relpos.add(bpos);
					}
				}
			}
		}
		for(int dx = -1; dx <= 1; dx++)
		{
			for(int dy = -2; dy <= 1; dy++)
			{
				for(int dz = -1; dz <= 1; dz++)
				{
					for(int i = 0; i < 1; i++)
					{
						Block b = world.getBlock(x + dx, y + dy - i, z + dz);
						if(b == null)
							return;
						Vector bpos = new Vector(x + dx, y + dy - i, z + dz);
						Vector relpos = newpos.sub(bpos);
						relpos = b.adjustPlayerPosition(relpos, distLimit);
						if(relpos == null)
							return;
						newpos = relpos.add(bpos);
					}
				}
			}
		}
		this.position = newpos;
	}

	private void internalSetPosition(Vector pos)
	{
		Vector deltapos = pos.sub(this.position);
		final int count = 100;
		deltapos = deltapos.mul(1.0f / count);
		for(int i = 0; i < count; i++)
		{
			internalSetPositionH(this.position.add(deltapos));
		}
	}

	private void chestNextCurBlockType()
	{
		Block chest = world.getBlock(this.blockX, this.blockY, this.blockZ);
		if(chest == null || chest.getType() != BlockType.BTChest)
		{
			chest = Block.NewChest();
			this.state = State.Normal;
			return;
		}
		if(this.chestCurBlockType > 0)
		{
			for(int i = this.chestCurBlockType + 1; i < BlockType.Count; i++)
			{
				if(chest.chestGetBlockTypeCount(BlockType.toBlockType(i)) > 0)
				{
					this.chestCurBlockType = i;
					return;
				}
			}
			this.chestCurBlockType = 0;
			return;
		}
		for(int i = 1; i < BlockType.Count; i++)
		{
			if(chest.chestGetBlockTypeCount(BlockType.toBlockType(i)) > 0)
			{
				this.chestCurBlockType = i;
				return;
			}
		}
	}

	private void chestPrevCurBlockType()
	{
		Block chest = world.getBlock(this.blockX, this.blockY, this.blockZ);
		if(chest == null || chest.getType() != BlockType.BTChest)
		{
			chest = Block.NewChest();
			this.state = State.Normal;
			return;
		}
		if(this.chestCurBlockType > 0)
		{
			for(int i = this.chestCurBlockType - 1; i > 0; i--)
			{
				if(chest.chestGetBlockTypeCount(BlockType.toBlockType(i)) > 0)
				{
					this.chestCurBlockType = i;
					return;
				}
			}
			this.chestCurBlockType = 0;
			return;
		}
		for(int i = BlockType.Count - 1; i > 0; i--)
		{
			if(chest.chestGetBlockTypeCount(BlockType.toBlockType(i)) > 0)
			{
				this.chestCurBlockType = i;
				return;
			}
		}
	}

	private boolean chestGiveBlock(BlockType bt, boolean setCurrentBlock)
	{
		if(bt == BlockType.BTEmpty)
		{
			if(setCurrentBlock)
				this.chestCurBlockType = 0;
			return true;
		}
		Block chest = world.getBlock(this.blockX, this.blockY, this.blockZ);
		if(chest == null || chest.getType() != BlockType.BTChest)
		{
			chest = Block.NewChest();
			this.state = State.Normal;
			return false;
		}
		chest = new Block(chest);
		chest.chestAddBlock(bt);
		// world.addModNode(blockX, blockY, blockZ, chest);
		// TODO finish
		world.setBlock(this.blockX, this.blockY, this.blockZ, chest);
		int index = bt.ordinal();
		if(setCurrentBlock)
			this.chestCurBlockType = index;
		return true;
	}

	private BlockType chestTakeBlock()
	{
		Block chest = world.getBlock(this.blockX, this.blockY, this.blockZ);
		if(chest == null || chest.getType() != BlockType.BTChest)
		{
			chest = Block.NewChest();
			this.state = State.Normal;
			return BlockType.BTEmpty;
		}
		chest = new Block(chest);
		BlockType retval = BlockType.BTEmpty;
		retval = BlockType.toBlockType(this.chestCurBlockType);
		if(this.chestCurBlockType <= 0
		        || chest.chestRemoveBlock(retval) == BlockType.BTEmpty)
		{
			this.chestCurBlockType = -1;
			return BlockType.BTEmpty;
		}
		if(chest.chestGetBlockTypeCount(retval) > 0)
		{
			// world.addModNode(blockX, blockY, blockZ, chest);
			// TODO finish
			world.setBlock(this.blockX, this.blockY, this.blockZ, chest);
		}
		else
		{
			// world.addModNode(blockX, blockY, blockZ, chest);
			// TODO finish
			world.setBlock(this.blockX, this.blockY, this.blockZ, chest);
			for(int i = this.chestCurBlockType; i < BlockType.Count; i++)
			{
				if(chest.chestGetBlockTypeCount(BlockType.toBlockType(i)) > 0)
				{
					this.chestCurBlockType = i;
					return retval;
				}
			}
			this.chestCurBlockType = 0;
			chestNextCurBlockType();
		}
		return retval;
	}

	@SuppressWarnings("unused")
	private BlockType chestTakeBlock(BlockType bt)
	{
		if(bt == BlockType.BTEmpty)
			return BlockType.BTEmpty;
		int index = bt.ordinal();
		if(index < 1 || index >= BlockType.Count)
			return BlockType.BTEmpty;
		if(index == this.chestCurBlockType)
			return chestTakeBlock();
		Block chest = world.getBlock(this.blockX, this.blockY, this.blockZ);
		if(chest == null || chest.getType() != BlockType.BTChest)
		{
			chest = Block.NewChest();
			this.state = State.Normal;
			return BlockType.BTEmpty;
		}
		chest = new Block(chest);
		BlockType retval = chest.chestRemoveBlock(bt);
		// world.addModNode(blockX, blockY, blockZ, chest);
		// TODO finish
		world.setBlock(this.blockX, this.blockY, this.blockZ, chest);
		return retval;
	}

	/**
	 * @param event
	 *            the event
	 */
	public void handleMouseUpDown(final Main.MouseEvent event)
	{
		if(this.paused)
			return;
		switch(this.state)
		{
		case Normal:
		{
			this.deleteAnimTime = -1;
			if(event.isDown && event.button == Main.MouseEvent.LEFT)
			{
				this.mouseDownTime = 0;
			}
			else if(event.isDown && event.button == Main.MouseEvent.RIGHT)
			{
				this.isShiftDown = true;
				Block oldb = getSelectedBlock();
				if(canPlaceBlock(oldb, this.blockX, this.blockY, this.blockZ))
				{
					BlockType bt = takeBlock();
					Block newb = Block.make(bt,
					                        this.blockOrientation,
					                        Block.getOrientationFromVector(bt.use3DOrientation() ? getForwardVector()
					                                : getMoveForwardVector()));
					if(newb != null)
					{
						// world.AddModNode(blockX, blockY, blockZ, newb);
						// TODO finish
						world.setBlock(this.blockX,
						               this.blockY,
						               this.blockZ,
						               newb);
						Main.play(Main.popAudio);
					}
					internalSetPosition(this.position);
					this.deleteAnimTime = -1;
				}
				this.isShiftDown = false;
			}
			else if(event.dWheel > 0)
			{
				nextCurBlockType();
			}
			else if(event.dWheel < 0)
			{
				prevCurBlockType();
			}
			else if(!event.isDown && event.button == Main.MouseEvent.LEFT)
			{
				Block b = getSelectedBlock();
				boolean didAction = false;
				if(b != null && b.getType() == BlockType.BTChest)
				{
					this.state = State.Chest;
					this.chestCurBlockType = 0;
					didAction = true;
				}
				else if(b != null && b.getType() == BlockType.BTWorkbench)
				{
					this.workbenchSize = 3;
					this.state = State.Workbench;
					for(int x = 0; x < this.workbenchSize; x++)
						for(int y = 0; y < this.workbenchSize; y++)
							this.workbench[x + y * this.workbenchSize] = BlockType.BTEmpty;
					this.lastMouseX = -1;
					this.lastMouseY = -1;
					didAction = true;
				}
				else if(b != null && b.getType() == BlockType.BTFurnace)
				{
					this.state = State.Furnace;
					didAction = true;
				}
				else if(b != null && b.getType() == BlockType.BTStoneButton)
				{
					b = Block.NewStoneButton(b.getType().getOnTime(),
					                         b.getOrientation());
					// world.addModNode(blockX, blockY, blockZ, b);
					// TODO finish
					world.setBlock(this.blockX, this.blockY, this.blockZ, b);
					didAction = true;
				}
				else if(b != null && b.getType() == BlockType.BTWoodButton)
				{
					b = Block.NewWoodButton(b.getType().getOnTime(),
					                        b.getOrientation());
					// world.addModNode(blockX, blockY, blockZ, b);
					// TODO finish
					world.setBlock(this.blockX, this.blockY, this.blockZ, b);
					didAction = true;
				}
				else if(b != null
				        && (b.getType() == BlockType.BTRedstoneRepeaterOff || b.getType() == BlockType.BTRedstoneRepeaterOn))
				{
					b = new Block(b);
					b.redstoneRepeaterStepDelay();
					// world.addModNode(blockX, blockY, blockZ, b);
					// TODO finish
					world.setBlock(this.blockX, this.blockY, this.blockZ, b);
					didAction = true;
				}
				else if(b != null && b.getType() == BlockType.BTLever)
				{
					b = new Block(b);
					b.leverToggle();
					// world.addModNode(blockX, blockY, blockZ, b);
					// TODO finish
					world.setBlock(this.blockX, this.blockY, this.blockZ, b);
					didAction = true;
				}
				else if(b != null && b.getType() == BlockType.BTTNT)
				{
					// TODO finish
				}
				if(didAction)
					Main.play(Main.clickAudio);
			}
			break;
		}
		case Workbench:
		{
			this.deleteAnimTime = -1;
			if(event.isDown && event.button == Main.MouseEvent.LEFT)
			{
				if(mousePosToSelPosWorkbench(event.mouseX, event.mouseY))
				{
					if(this.workbench[this.workbenchSelX + this.workbenchSize
					        * this.workbenchSelY] == BlockType.BTEmpty)
					{
						this.workbench[this.workbenchSelX + this.workbenchSize
						        * this.workbenchSelY] = takeBlock();
					}
					else
					{
						giveBlock(this.workbench[this.workbenchSelX
						                  + this.workbenchSize
						                  * this.workbenchSelY],
						          true);
						this.workbench[this.workbenchSelX + this.workbenchSize
						        * this.workbenchSelY] = BlockType.BTEmpty;
					}
				}
				else if(mouseIsInResultWorkbench(event.mouseX, event.mouseY))
				{
					runWorkbenchReduce();
				}
			}
			else if(event.dWheel > 0)
			{
				nextCurBlockType();
			}
			else if(event.dWheel < 0)
			{
				prevCurBlockType();
			}
			else if(!event.isDown)
			{
			}
			break;
		}
		case Chest:
		{
			this.deleteAnimTime = -1;
			if(event.dWheel > 0)
			{
				nextCurBlockType();
			}
			else if(event.dWheel < 0)
			{
				prevCurBlockType();
			}
			else if(event.isDown && event.button == Main.MouseEvent.LEFT)
			{
				int moveCount = 1;
				/*if(event.keysym.mod & KMOD_CTRL)
				    moveCount = 5;*/
				for(int i = 0; i < moveCount; i++)
					giveBlock(chestTakeBlock(), true);
			}
			else if(event.isDown && event.button == Main.MouseEvent.RIGHT)
			{
				int moveCount = 1;
				/*if(event.keysym.mod & KMOD_CTRL)
				    moveCount = 5;*/
				for(int i = 0; i < moveCount; i++)
					if(!chestGiveBlock(takeBlock(), true))
						return;
			}
			break;
		}
		case Furnace:
		{
			this.deleteAnimTime = -1;
			if(event.dWheel > 0)
			{
				nextCurBlockType();
			}
			else if(event.dWheel < 0)
			{
				prevCurBlockType();
			}
			else if(event.isDown && event.button == Main.MouseEvent.LEFT)
			{
				if(mouseIsInFireFurnace(event.mouseX, event.mouseY))
				{
					Block b = world.getBlock(this.blockX,
					                         this.blockY,
					                         this.blockZ);
					if(b != null && b.getType() == BlockType.BTFurnace)
						b = new Block(b);
					else
					{
						b = Block.NewFurnace();
						this.state = State.Normal;
						return;
					}
					BlockType bt = takeBlock();
					if(bt.getBurnTime() > 0)
					{
						b.furnaceAddFire(bt);
					}
					else
					{
						giveBlock(bt, true);
					}
					// world.addModNode(blockX, blockY, blockZ, b);
					// TODO finish
					world.setBlock(this.blockX, this.blockY, this.blockZ, b);
				}
				else if(mouseIsInSourceFurnace(event.mouseX, event.mouseY))
				{
					Block b = world.getBlock(this.blockX,
					                         this.blockY,
					                         this.blockZ);
					if(b != null && b.getType() == BlockType.BTFurnace)
						b = new Block(b);
					else
					{
						b = Block.NewFurnace();
						this.state = State.Normal;
						return;
					}
					BlockType bt = takeBlock();
					if(bt.isSmeltable())
					{
						if(!b.furnaceAddBlock(bt))
							giveBlock(bt, true);
					}
					else
					{
						giveBlock(bt, true);
					}
					// world.addModNode(blockX, blockY, blockZ, b);
					// TODO finish
					world.setBlock(this.blockX, this.blockY, this.blockZ, b);
				}
				else if(mouseIsInResultFurnace(event.mouseX, event.mouseY))
				{
					Block b = world.getBlock(this.blockX,
					                         this.blockY,
					                         this.blockZ);
					if(b != null && b.getType() == BlockType.BTFurnace)
						b = new Block(b);
					else
					{
						b = Block.NewFurnace();
						this.state = State.Normal;
						return;
					}
					BlockType bt = b.furnaceRemoveBlock();
					if(bt != BlockType.BTEmpty)
					{
						giveBlock(bt, true);
					}
					// world.addModNode(blockX, blockY, blockZ, b);
					// TODO finish
					world.setBlock(this.blockX, this.blockY, this.blockZ, b);
				}
			}
			break;
		}
		}
	}

	private boolean isOnGround()
	{
		Vector newpos = this.position.sub(new Vector(0, 0.01f + distLimit, 0));
		int x = (int)Math.floor(newpos.x), y = (int)Math.floor(newpos.y), z = (int)Math.floor(newpos.z);
		Block b1 = world.getBlockEval(x, y, z);
		Block b2 = world.getBlockEval(x, y - 1, z);
		if((b1 != null && b1.adjustPlayerPosition(newpos.sub(new Vector(x, y, z)),
		                                          distLimit) == null)
		        || (b2 != null && b2.adjustPlayerPosition(newpos.sub(new Vector(x,
		                                                                        y - 1,
		                                                                        z)),
		                                                  distLimit) == null))
		{
			return true;
		}
		else if(b1 == null || b2 == null)
		{
			this.velocity = new Vector(0);
			return false;
		}
		return false;
	}

	private boolean isInWater()
	{
		Vector newpos = this.position.sub(new Vector(0, 0.01f + distLimit, 0));
		int x = (int)Math.floor(newpos.x), y = (int)Math.floor(newpos.y), z = (int)Math.floor(newpos.z);
		Block b1 = world.getBlockEval(x, y, z);
		Block b2 = world.getBlockEval(x, y - 1, z);
		if(b1 != null && b1.getType() == BlockType.BTWater)
			return true;
		if(b2 != null && b2.getType() == BlockType.BTWater)
			return true;
		return false;
	}

	private boolean isInLadder()
	{
		Vector newpos = this.position.add(new Vector(0, 0.25f, 0));
		int x = (int)Math.floor(newpos.x), y = (int)Math.floor(newpos.y), z = (int)Math.floor(newpos.z);
		Block b = world.getBlockEval(x, y - 1, z);
		if(b != null && b.getType() == BlockType.BTLadder)
			return true;
		return false;
	}

	private Block getLadder()
	{
		Vector newpos = this.position.add(new Vector(0, 0.25f, 0));
		int x = (int)Math.floor(newpos.x), y = (int)Math.floor(newpos.y), z = (int)Math.floor(newpos.z);
		return world.getBlockEval(x, y - 1, z);
	}

	@Override
	public void move()
	{
		internalSetPosition(this.position);
		if(this.paused)
		{
			this.isShiftDown = false;
			return;
		}
		switch(this.state)
		{
		case Normal:
		{
			boolean inWater = isInWater();
			boolean inLadder = isInLadder();
			Vector origvelocity = new Vector(this.velocity);
			if(inWater)
			{
				Vector acc = this.velocity.mul(-(float)Main.getFrameDuration());
				Vector newvel = this.velocity.add(acc);
				if(this.velocity.abs_squared() <= acc.abs_squared()
				        || this.velocity.dot(newvel) <= 0
				        || this.velocity.abs_squared() < 1e-4 * 1e-4)
					this.velocity = new Vector(0);
				else
					this.velocity = newvel;
			}
			else if(inLadder)
			{
				this.velocity = new Vector(0);
				if(!Main.isKeyDown(Main.KEY_SHIFT))
				{
					this.velocity = new Vector(0, -1.5f, 0);
				}
			}
			else
			{
				this.velocity.y -= World.GravityAcceleration
				        * (float)Main.getFrameDuration();
			}
			Vector v = this.velocity.mul((float)Main.getFrameDuration());
			if(v.abs() > 1.0f)
			{
				v = v.normalize();
				if(this.velocity.abs_squared() > origvelocity.abs_squared())
					this.velocity = origvelocity;
			}
			internalSetPosition(this.position.add(v));
			if(isOnGround())
			{
				this.velocity = new Vector(0);
			}
			Vector forwardVec;
			if(inWater)
				forwardVec = getForwardVector();
			else if(inLadder)
			{
				forwardVec = getMoveForwardVector();
				Block ladder = getLadder();
				Vector pos = this.position.sub(new Vector((float)Math.floor(this.position.x),
				                                          0,
				                                          (float)Math.floor(this.position.z)));
				pos.y = 0.5f;
				if(ladder.ladderIsPlayerPushingIntoLadder(pos, forwardVec))
				{
					forwardVec = new Vector(0, 1, 0);
				}
			}
			else
				forwardVec = getMoveForwardVector();
			boolean isMoving = false;
			if(!Main.isKeyDown(Main.KEY_W) || !Main.isKeyDown(Main.KEY_S))
			{
				if(Main.isKeyDown(Main.KEY_W))
				{
					internalSetPosition(this.position.add(forwardVec.mul(3.5f * (float)Main.getFrameDuration())));
					isMoving = true;
				}
				if(Main.isKeyDown(Main.KEY_S))
				{
					internalSetPosition(this.position.sub(forwardVec.mul(3.5f * (float)Main.getFrameDuration())));
					isMoving = true;
				}
			}
			if(inWater && !isMoving)
			{
				this.velocity.y -= World.GravityAcceleration
				        * (float)Main.getFrameDuration() * 0.25f;
			}
			if(Main.isKeyDown(Main.KEY_SPACE))
			{
				if(isOnGround())
				{
					this.velocity = new Vector(0,
					                           2.0f * (float)Math.sqrt(World.GravityAcceleration),
					                           0);
				}
			}
			break;
		}
		case Workbench:
		{
			this.isShiftDown = false;
			break;
		}
		case Chest:
		{
			this.isShiftDown = false;
			break;
		}
		case Furnace:
		{
			this.isShiftDown = false;
			break;
		}
		}
	}

	/**
	 * @param paused
	 *            if this player should be paused
	 */
	public void setPaused(boolean paused)
	{
		if(this.paused || paused)
			this.wasPaused = true;
		this.paused = paused;
	}

	/**
	 * @param event
	 *            the event to handle
	 */
	public void handleKeyboardEvent(Main.KeyboardEvent event)
	{
		if(event.isDown)
		{
			if(event.key == Main.KEY_F2)
			{
				Main.saveAll();
				return;
			}
			if(event.key == Main.KEY_P)
			{
				setPaused(!this.paused);
				if(this.paused)
					this.wasPaused = true;
				return;
			}
			if(this.paused)
				return;
			switch(this.state)
			{
			case Normal:
			{
				if(event.key == Main.KEY_E)
				{
					this.deleteAnimTime = -1;
					this.workbenchSize = 2;
					this.state = State.Workbench;
					for(int x = 0; x < this.workbenchSize; x++)
						for(int y = 0; y < this.workbenchSize; y++)
							this.workbench[x + y * this.workbenchSize] = BlockType.BTEmpty;
					this.lastMouseX = -1;
					this.lastMouseY = -1;
				}
				break;
			}
			case Workbench:
			{
				this.deleteAnimTime = -1;
				if(event.key == Main.KEY_ESCAPE || event.key == Main.KEY_Q)
				{
					this.state = State.Normal;
					for(int x = 0; x < this.workbenchSize; x++)
					{
						for(int y = 0; y < this.workbenchSize; y++)
						{
							if(this.workbench[x + this.workbenchSize * y] == BlockType.BTEmpty)
								continue;
							giveBlock(this.workbench[x + this.workbenchSize * y],
							          false);
							this.workbench[x + this.workbenchSize * y] = BlockType.BTEmpty;
						}
					}
					this.wasPaused = true;
				}
				else if(event.key == Main.KEY_RETURN)
				{
					runWorkbenchReduce();
				}
				break;
			}
			case Chest:
			{
				this.deleteAnimTime = -1;
				if(event.key == Main.KEY_ESCAPE || event.key == Main.KEY_Q)
				{
					this.state = State.Normal;
					this.wasPaused = true;
				}
				else if(event.key == Main.KEY_LEFT || event.key == Main.KEY_A)
				{
					prevCurBlockType();
				}
				else if(event.key == Main.KEY_RIGHT || event.key == Main.KEY_D)
				{
					nextCurBlockType();
				}
				else if(event.key == Main.KEY_UP || event.key == Main.KEY_W)
				{
					chestNextCurBlockType();
				}
				else if(event.key == Main.KEY_DOWN || event.key == Main.KEY_S)
				{
					chestPrevCurBlockType();
				}
				else if(event.key == Main.KEY_SPACE)
				{
					int moveCount = 1;
					if(Main.isKeyDown(Main.KEY_CTRL))
						moveCount = 5;
					for(int i = 0; i < moveCount; i++)
						chestGiveBlock(takeBlock(), true);
				}
				else if(event.key == Main.KEY_DELETE)
				{
					int moveCount = 1;
					if(Main.isKeyDown(Main.KEY_CTRL))
						moveCount = 5;
					for(int i = 0; i < moveCount; i++)
						giveBlock(chestTakeBlock(), true);
				}
				break;
			}
			case Furnace:
			{
				this.deleteAnimTime = -1;
				if(event.key == Main.KEY_ESCAPE || event.key == Main.KEY_Q)
				{
					this.state = State.Normal;
					this.wasPaused = true;
				}
				else if(event.key == Main.KEY_LEFT || event.key == Main.KEY_A)
				{
					prevCurBlockType();
				}
				else if(event.key == Main.KEY_RIGHT || event.key == Main.KEY_D)
				{
					nextCurBlockType();
				}
				break;
			}
			}
		}
		else if(!event.isDown)
		{
			if(this.paused)
				return;
		}
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
		this.position.write(o);
		this.velocity.write(o);
		o.writeFloat(this.viewPhi);
		o.writeFloat(this.viewTheta);
		int count = 0;
		for(int i = 1; i < BlockType.Count; i++)
		{
			BlockType bt = BlockType.toBlockType(i);
			int bcount = this.blockCount[i];
			if(this.state == State.Workbench)
			{
				for(int x = 0; x < this.workbenchSize; x++)
				{
					for(int y = 0; y < this.workbenchSize; y++)
					{
						if(this.workbench[x + this.workbenchSize * y] == bt)
						{
							bcount++;
						}
					}
				}
			}
			if(bcount > 0)
				count++;
		}
		o.writeShort(count);
		for(int i = 1; i < BlockType.Count; i++)
		{
			BlockType bt = BlockType.toBlockType(i);
			int bcount = this.blockCount[i];
			if(this.state == State.Workbench)
			{
				for(int x = 0; x < this.workbenchSize; x++)
				{
					for(int y = 0; y < this.workbenchSize; y++)
					{
						if(this.workbench[x + this.workbenchSize * y] == bt)
						{
							bcount++;
						}
					}
				}
			}
			if(bcount <= 0)
				continue;
			bt.write(o);
			o.writeInt(bcount);
		}
	}

	/**
	 * read from a <code>DataInput</code>
	 * 
	 * @param i
	 *            <code>DataInput</code> to read from
	 * @return the read <code>Player</code>
	 * @throws IOException
	 *             the exception thrown
	 */
	public static Player read(DataInput i) throws IOException
	{
		Player retval = new Player();
		retval.position = Vector.read(i);
		retval.velocity = Vector.read(i);
		retval.viewPhi = i.readFloat();
		if(Float.isInfinite(retval.viewPhi) || Float.isNaN(retval.viewPhi)
		        || Math.abs(retval.viewPhi) > 1e-4 + Math.PI / 2)
			throw new IOException("view phi out of range");
		retval.viewTheta = i.readFloat();
		if(Float.isInfinite(retval.viewTheta) || Float.isNaN(retval.viewTheta)
		        || Math.abs(retval.viewTheta) > 1e-4 + Math.PI * 2)
			throw new IOException("theta out of range");
		int count = i.readUnsignedShort();
		if(count > BlockType.Count)
			throw new IOException("block type count is too big");
		while(count-- > 0)
		{
			int index = BlockType.read(i).value;
			if(retval.blockCount[index] > 0)
				throw new IOException("block type is duplicate");
			int value = i.readInt();
			if(value <= 0 || value >= 1000000000)
				throw new IOException("value is out of range");
			retval.blockCount[index] = value;
		}
		return retval;
	}

	/**
	 * push this player if it's inside of &lt;<code>bx</code>, <code>by</code>,
	 * <code>bz</code>&gt; out in the direction &lt;<code>dx</code>,
	 * <code>dy</code>, <code>dz</code>&gt;
	 * 
	 * @param bx
	 *            x coordinate of the block to push out of
	 * @param by
	 *            y coordinate of the block to push out of
	 * @param bz
	 *            z coordinate of the block to push out of
	 * @param dx
	 *            x coordinate of the direction to push
	 * @param dy
	 *            y coordinate of the direction to push
	 * @param dz
	 *            z coordinate of the direction to push
	 */
	public void push(int bx, int by, int bz, int dx, int dy, int dz)
	{
		boolean doPush = false;
		for(int pdy = -1; pdy <= 0; pdy++)
		{
			Vector p = this.position.add(new Vector(0, pdy, 0));
			int x = (int)Math.floor(p.x);
			int y = (int)Math.floor(p.y);
			int z = (int)Math.floor(p.z);
			if(x == bx && y == by && z == bz)
			{
				doPush = true;
				break;
			}
		}
		if(!doPush)
			return;
		internalSetPosition(this.position.add(new Vector(dx, dy, dz)));
	}
}
