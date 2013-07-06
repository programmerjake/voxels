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

/** @author jacob */
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
    private int blockCount[] = new int[(Block.CHEST_ROWS + 1)
            * Block.CHEST_COLUMNS];
    private Block blockType[] = new Block[(Block.CHEST_ROWS + 1)
            * Block.CHEST_COLUMNS];
    private int selectionX = 0;
    private boolean isShiftDown = false;
    private int lastMouseX = -1, lastMouseY = -1;
    private static final float distLimit = 0.2f;
    private int dragCount = 0;
    private Block dragType = null;
    private float dragX = 0, dragY = 0;
    private int creativeOffset = 0;

    private static int getInventoryIndex(int row, int column)
    {
        return row * Block.CHEST_COLUMNS + column;
    }

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
    private final Block workbench[] = new Block[workbenchMaxSize
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

    /** @return true if this player is in normal(3D) state */
    public boolean isNormalState()
    {
        if(this.state == State.Normal)
            return true;
        return false;
    }

    /** @return true if this player is paused */
    public boolean isPaused()
    {
        return this.paused;
    }

    /** @return the transformation that converts world coordinates to this
     *         player's camera coordinates */
    public Matrix getWorldToCamera()
    {
        return Matrix.translate(this.position.neg())
                     .concat(Matrix.rotatey(this.viewTheta))
                     .concat(Matrix.rotatex(this.viewPhi));
    }

    /** @return the vector pointing in the direction this player is looking */
    public Vector getForwardVector()
    {
        return Matrix.rotatex(-this.viewPhi)
                     .concat(Matrix.rotatey(-this.viewTheta))
                     .apply(new Vector(0.0f, 0.0f, -1.0f));
    }

    /** @return the vector pointing in the direction this player is facing */
    public Vector getMoveForwardVector()
    {
        return Matrix.rotatey(-this.viewTheta).apply(new Vector(0.0f,
                                                                0.0f,
                                                                -1.0f));
    }

    /** @return this player's position */
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
    private static Image inventoryImg = new Image("inventory.png");
    private static Image creativeImg = new Image("creative.png");
    private static Image chestImg = new Image("chestedit.png");
    private static Image furnaceImg = new Image("furnace.png");
    private static Image hotbarBoxImg = new Image("hotbarbox.png");
    private static final float workbenchZDist = -1.0f;
    private static final float simScreenHeight = 480f;
    private static final int dialogW = 170, dialogH = 151,
            dialogTextureSize = 256;
    private static final int inventoryLeft = 5, inventoryBottom = 28;
    private static final int hotbarLeft = 5, hotbarBottom = 5;
    private static final int chestLeft = 5, chestBottom = 255 - 161;
    private static final int creativeLeft = 5, creativeBottom = 255 - 161;
    private static final int CREATIVE_COLUMNS = 8;
    private static final int creativeUpLeft = 151,
            creativeUpBottom = 255 - 125;
    private static final int creativeDownLeft = 151,
            creativeDownBottom = 255 - 159;
    private static final int creativeButtonSize = 14;
    private static final int cellSize = 18, cellBorder = 1;
    private static final int furnaceFireXCenter = 66,
            furnaceFireBottom = 255 - 155;
    private static final int furnaceSrcLeft = 59, furnaceSrcBottom = 255 - 132;
    private static final int furnaceDestLeft = 101,
            furnaceDestBottom = 255 - 146;

    private void drawCenteredText(String str, float xCenter, float bottom)
    {
        if(str.length() <= 0)
            return;
        final Matrix imgMat = getImageMat();
        Matrix textTransform = Matrix.scale(Text.sizeH("A"))
                                     .concat(Matrix.translate(xCenter
                                                                      - Text.sizeW(str)
                                                                      / 2.0f,
                                                              bottom,
                                                              0))
                                     .concat(imgMat)
                                     .concat(Matrix.scale(0.7f));
        Text.draw(textTransform, str);
    }

    private void drawCell(Block b,
                          int count,
                          float cellLeft,
                          float cellBottom,
                          boolean drawIfEmpty)
    {
        if(count <= 0 && !drawIfEmpty)
            return;
        final Matrix imgMat = getImageMat();
        if(b != null)
        {
            Matrix blockTransform = Matrix.scale(16f)
                                          .concat(Matrix.translate(cellLeft,
                                                                   cellBottom,
                                                                   0))
                                          .concat(imgMat);
            b.drawAsItem(new RenderingStream(), blockTransform).render();
        }
        if(count > 1 || drawIfEmpty)
        {
            String str = Integer.toString(count);
            Matrix textTransform = Matrix.scale(Text.sizeH("A"))
                                         .concat(Matrix.translate(cellLeft
                                                                          + 16
                                                                          - Text.sizeW(str),
                                                                  cellBottom,
                                                                  0))
                                         .concat(imgMat)
                                         .concat(Matrix.scale(0.7f));
            Text.draw(textTransform, str);
        }
    }

    private void drawInventory()
    {
        for(int x = 0; x < Block.CHEST_COLUMNS; x++)
        {
            for(int y = 0; y < Block.CHEST_ROWS; y++)
            {
                int count = this.blockCount[getInventoryIndex(y, x)];
                Block b = this.blockType[getInventoryIndex(y, x)];
                drawCell(b,
                         count,
                         inventoryLeft + cellSize * x,
                         inventoryBottom + cellSize * y,
                         false);
            }
        }
        for(int x = 0; x < Block.CHEST_COLUMNS; x++)
        {
            int count = this.blockCount[getInventoryIndex(Block.CHEST_ROWS, x)];
            Block b = this.blockType[getInventoryIndex(Block.CHEST_ROWS, x)];
            drawCell(b, count, hotbarLeft + cellSize * x, hotbarBottom, false);
        }
    }

    private int getWorkbenchLeft()
    {
        if(this.workbenchSize == 2)
            return 48;
        return 40;
    }

    private int getWorkbenchBottom()
    {
        if(this.workbenchSize == 2)
            return 99;
        return 89;
    }

    private int getWorkbenchResultLeft()
    {
        if(this.workbenchSize == 2)
            return 104;
        return 114;
    }

    private int getWorkbenchResultBottom()
    {
        if(this.workbenchSize == 2)
            return 255 - 148;
        return 255 - 148;
    }

    private Matrix getImageMat()
    {
        final float imgW = dialogW / simScreenHeight, imgH = dialogH
                / simScreenHeight;
        return Matrix.scale(2f / simScreenHeight)
                     .concat(Matrix.translate(-imgW, -imgH, workbenchZDist));
    }

    /** draw everything from this player's perspective */
    public void drawAll()
    {
        Matrix worldToCamera = getWorldToCamera();
        RenderingStream rs = new RenderingStream();
        RenderingStream trs = new RenderingStream();
        players.drawPlayers(rs, worldToCamera);
        if(this.state == State.Normal)
        {
            Block b = getSelectedBlock();
            if(this.deleteAnimTime >= 0)
            {
                b = Block.NewDeleteAnim(this.deleteAnimTime);
                b.draw(trs,
                       Matrix.translate(this.blockX, this.blockY, this.blockZ)
                             .concat(worldToCamera));
            }
        }
        world.draw(rs, trs, worldToCamera); // must call draw world first
        if(this.state == State.Normal)
        {
            Block b = getSelectedBlock();
            if(b != null)
                drawBlockSelection(worldToCamera,
                                   this.blockX,
                                   this.blockY,
                                   this.blockZ);
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
        RenderingStream hotbarRS = new RenderingStream();
        hotbarBoxImg.selectTexture();
        glBegin(GL_QUADS);
        final float blockHeight = 2f * 16f / simScreenHeight;
        for(int i = 0; i < Block.CHEST_COLUMNS; i++)
        {
            final float zDist = -1f;
            final float maxU = 20f / 32f, maxV = 20f / 32f;
            final float height = 2f * 20f / simScreenHeight;
            final float top = -0.85f, bottom = top - height;
            final float width = height, left = (i - Block.CHEST_COLUMNS / 2f)
                    * width, right = left + width;
            if(i == this.selectionX)
                glColor4f(0, 1, 0, 1);
            else
                glColor4f(1, 1, 1, 1);
            glTexCoord2f(0, 0);
            glVertex3f(left, bottom, zDist);
            glTexCoord2f(maxU, 0);
            glVertex3f(right, bottom, zDist);
            glTexCoord2f(maxU, maxV);
            glVertex3f(right, top, zDist);
            glTexCoord2f(0, maxV);
            glVertex3f(left, top, zDist);
            if(this.blockCount[getInventoryIndex(Block.CHEST_ROWS, i)] > 0)
            {
                Matrix tform = Matrix.translate(left + width / 2f, bottom
                        + height / 2f, -1f);
                tform = Matrix.scale(blockHeight).concat(tform);
                tform = Matrix.translate(-0.5f, -0.5f, 0).concat(tform);
                this.blockType[getInventoryIndex(Block.CHEST_ROWS, i)].drawAsItem(hotbarRS,
                                                                                  tform);
            }
        }
        glEnd();
        glClear(GL_DEPTH_BUFFER_BIT);
        hotbarRS.render();
        glClear(GL_DEPTH_BUFFER_BIT);
        if(this.state != State.Normal)
        {
            glMatrixMode(GL_MODELVIEW);
            glLoadIdentity();
            Image.unselectTexture();
            glColor4f(0.5f, 0.5f, 0.5f, 0.125f);
            glBegin(GL_QUADS);
            glVertex3f(-Main.aspectRatio, -1, -1);
            glVertex3f(Main.aspectRatio, -1, -1);
            glVertex3f(Main.aspectRatio, 1, -1);
            glVertex3f(-Main.aspectRatio, 1, -1);
            glEnd();
            glClear(GL_DEPTH_BUFFER_BIT);
        }
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
            final int workbenchLeft = getWorkbenchLeft(), workbenchBottom = getWorkbenchBottom(), resultLeft = getWorkbenchResultLeft(), resultBottom = getWorkbenchResultBottom();
            boolean drawCreative = false;
            if(this.workbenchSize == 2)
            {
                if(Main.isCreativeMode)
                {
                    creativeImg.selectTexture();
                    drawCreative = true;
                }
                else
                    inventoryImg.selectTexture();
            }
            else
                workbenchImg.selectTexture();
            final float maxU = (float)dialogW / dialogTextureSize, maxV = (float)dialogH
                    / dialogTextureSize;
            Matrix imgMat = getImageMat();
            glColor3f(1, 1, 1);
            glBegin(GL_QUADS);
            glTexCoord2f(0, 0);
            glVertex(imgMat.apply(new Vector(0, 0, 0)));
            glTexCoord2f(maxU, 0);
            glVertex(imgMat.apply(new Vector(dialogW, 0, 0)));
            glTexCoord2f(maxU, maxV);
            glVertex(imgMat.apply(new Vector(dialogW, dialogH, 0)));
            glTexCoord2f(0, maxV);
            glVertex(imgMat.apply(new Vector(0, dialogH, 0)));
            glEnd();
            glClear(GL_DEPTH_BUFFER_BIT);
            drawInventory();
            if(drawCreative)
            {
                for(int y = 0; y < Block.CHEST_ROWS; y++)
                {
                    for(int x = 0; x < CREATIVE_COLUMNS; x++)
                    {
                        Block b = null;
                        int index = (y + this.creativeOffset)
                                * CREATIVE_COLUMNS + x;
                        if(index < BlockType.getCreativeModeInventorySize())
                            b = BlockType.getCreativeModeInventoryBlock(index);
                        if(b != null)
                        {
                            drawCell(b,
                                     1,
                                     creativeLeft + cellSize * x,
                                     creativeBottom + cellSize
                                             * (Block.CHEST_ROWS - y - 1),
                                     false);
                        }
                    }
                }
            }
            else
            {
                for(int x = 0; x < this.workbenchSize; x++)
                {
                    for(int y = 0; y < this.workbenchSize; y++)
                    {
                        Block b = this.workbench[x + this.workbenchSize * y];
                        if(b != null)
                        {
                            drawCell(b,
                                     1,
                                     workbenchLeft + cellSize * x,
                                     workbenchBottom + cellSize * y,
                                     false);
                        }
                    }
                }
                int count = 0;
                Block b = null;
                Block.ReduceDescriptor rd = Block.reduce(this.workbench,
                                                         this.workbenchSize);
                if(!rd.isEmpty())
                {
                    b = rd.b;
                    count = rd.count;
                }
                drawCell(b, count, resultLeft, resultBottom, false);
            }
            break;
        }
        case Chest:
        {
            glMatrixMode(GL_MODELVIEW);
            glLoadIdentity();
            chestImg.selectTexture();
            final float maxU = (float)dialogW / dialogTextureSize, maxV = (float)dialogH
                    / dialogTextureSize;
            Matrix imgMat = getImageMat();
            glColor3f(1, 1, 1);
            glBegin(GL_QUADS);
            glTexCoord2f(0, 0);
            glVertex(imgMat.apply(new Vector(0, 0, 0)));
            glTexCoord2f(maxU, 0);
            glVertex(imgMat.apply(new Vector(dialogW, 0, 0)));
            glTexCoord2f(maxU, maxV);
            glVertex(imgMat.apply(new Vector(dialogW, dialogH, 0)));
            glTexCoord2f(0, maxV);
            glVertex(imgMat.apply(new Vector(0, dialogH, 0)));
            glEnd();
            glClear(GL_DEPTH_BUFFER_BIT);
            drawInventory();
            Block chest = world.getBlock(this.blockX, this.blockY, this.blockZ);
            if(chest == null || chest.getType() != BlockType.BTChest)
                chest = Block.NewChest();
            for(int row = 0; row < Block.CHEST_ROWS; row++)
            {
                for(int column = 0; column < Block.CHEST_COLUMNS; column++)
                {
                    int count = chest.chestGetBlockCount(row, column);
                    Block b = chest.chestGetBlockType(row, column);
                    drawCell(b,
                             count,
                             chestLeft + column * cellSize,
                             chestBottom + row * cellSize,
                             false);
                }
            }
            break;
        }
        case Furnace:
        {
            glMatrixMode(GL_MODELVIEW);
            glLoadIdentity();
            furnaceImg.selectTexture();
            final float maxU = (float)dialogW / dialogTextureSize, maxV = (float)dialogH
                    / dialogTextureSize;
            Matrix imgMat = getImageMat();
            glColor3f(1, 1, 1);
            glBegin(GL_QUADS);
            glTexCoord2f(0, 0);
            glVertex(imgMat.apply(new Vector(0, 0, 0)));
            glTexCoord2f(maxU, 0);
            glVertex(imgMat.apply(new Vector(dialogW, 0, 0)));
            glTexCoord2f(maxU, maxV);
            glVertex(imgMat.apply(new Vector(dialogW, dialogH, 0)));
            glTexCoord2f(0, maxV);
            glVertex(imgMat.apply(new Vector(0, dialogH, 0)));
            glEnd();
            glClear(GL_DEPTH_BUFFER_BIT);
            drawInventory();
            Block furnace = world.getBlock(this.blockX,
                                           this.blockY,
                                           this.blockZ);
            if(furnace == null || furnace.getType() != BlockType.BTFurnace)
                furnace = Block.NewFurnace();
            drawCell(furnace.furnaceGetSrcBlock(),
                     furnace.furnaceGetSrcBlockCount(),
                     furnaceSrcLeft,
                     furnaceSrcBottom,
                     false);
            drawCell(furnace.furnaceGetDestBlock(),
                     furnace.furnaceGetDestBlockCount(),
                     furnaceDestLeft,
                     furnaceDestBottom,
                     true);
            drawCenteredText(Integer.toString(furnace.furnaceGetFuelLeft()),
                             furnaceFireXCenter,
                             furnaceFireBottom);
            break;
        }
        }
        glClear(GL_DEPTH_BUFFER_BIT);
        if(this.state != State.Normal)
        {
            drawCell(this.dragType,
                     this.dragCount,
                     this.dragX - cellSize / 2f,
                     this.dragY - cellSize / 2f,
                     false);
        }
        if(this.paused)
        {
            glClear(GL_DEPTH_BUFFER_BIT);
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
    public RenderingStream draw(RenderingStream rs, Matrix worldToCamera)
    {
        // TODO Auto-generated method stub
        return rs;
    }

    /** set this player's position
     * 
     * @param pos
     *            the new position */
    public void setPosition(Vector pos)
    {
        this.position = new Vector(pos);
        this.velocity = new Vector(0);
    }

    private boolean mousePosToSelPosWorkbench(int mouseX, int mouseY)
    {
        float x = mouseGetSimX(mouseX);
        float y = mouseGetSimY(mouseY);
        x -= getWorkbenchLeft();
        y -= getWorkbenchBottom();
        x += cellBorder;
        y += cellBorder;
        x /= cellSize;
        y /= cellSize;
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

    private float mouseGetSimX(int mouseX)
    {
        float x = (float)mouseX / Main.ScreenXRes * 2.0f - 1.0f;
        x *= Main.aspectRatio;
        x += dialogW / simScreenHeight;
        x /= 2f / simScreenHeight;
        return x;
    }

    private float mouseGetSimY(int mouseY)
    {
        float y = 1.0f - (float)mouseY / Main.ScreenYRes * 2.0f;
        y += dialogH / simScreenHeight;
        y /= 2f / simScreenHeight;
        return y;
    }

    private boolean mouseIsInResultWorkbench(int mouseX, int mouseY)
    {
        float x = mouseGetSimX(mouseX);
        float y = mouseGetSimY(mouseY);
        x -= getWorkbenchLeft();
        y -= getWorkbenchBottom();
        x += cellBorder;
        y += cellBorder;
        if(x < 0 || x >= cellSize || y < 0 || y >= cellSize)
            return false;
        return true;
    }

    private boolean mouseIsInResultFurnace(int mouseX, int mouseY)
    {
        float x = mouseGetSimX(mouseX);
        float y = mouseGetSimY(mouseY);
        x -= furnaceDestLeft;
        y -= furnaceDestBottom;
        x += cellBorder;
        y += cellBorder;
        if(x < 0 || x >= cellSize || y < 0 || y >= cellSize)
            return false;
        return true;
    }

    private boolean mouseIsInFireFurnace(int mouseX, int mouseY)
    {
        float x = mouseGetSimX(mouseX);
        float y = mouseGetSimY(mouseY);
        x -= furnaceFireXCenter;
        y -= furnaceFireBottom;
        if(x < -10 || x > 10 || y < 0 || y > 10)
            return false;
        return true;
    }

    private boolean mouseIsInSourceFurnace(int mouseX, int mouseY)
    {
        float x = mouseGetSimX(mouseX);
        float y = mouseGetSimY(mouseY);
        x -= furnaceSrcLeft;
        y -= furnaceSrcBottom;
        x += cellBorder;
        y += cellBorder;
        if(x < 0 || x >= cellSize || y < 0 || y >= cellSize)
            return false;
        return true;
    }

    private boolean mouseIsInChest(int mouseX, int mouseY)
    {
        float x = mouseGetSimX(mouseX);
        float y = mouseGetSimY(mouseY);
        x -= chestLeft;
        y -= chestBottom;
        x += cellBorder;
        y += cellBorder;
        if(x >= 0 && x < cellSize * Block.CHEST_COLUMNS && y >= 0
                && y < cellSize * Block.CHEST_ROWS)
            return true;
        return false;
    }

    private int mouseGetChestRow(int mouseX, int mouseY)
    {
        float x = mouseGetSimX(mouseX);
        float y = mouseGetSimY(mouseY);
        x -= chestLeft;
        y -= chestBottom;
        x += cellBorder;
        y += cellBorder;
        if(x >= 0 && x < cellSize * Block.CHEST_COLUMNS && y >= 0
                && y < cellSize * Block.CHEST_ROWS)
            return (int)Math.floor(y / cellSize);
        return -1;
    }

    private int mouseGetChestColumn(int mouseX, int mouseY)
    {
        float x = mouseGetSimX(mouseX);
        float y = mouseGetSimY(mouseY);
        x -= chestLeft;
        y -= chestBottom;
        x += cellBorder;
        y += cellBorder;
        if(x >= 0 && x < cellSize * Block.CHEST_COLUMNS && y >= 0
                && y < cellSize * Block.CHEST_ROWS)
            return (int)Math.floor(x / cellSize);
        return -1;
    }

    private boolean mouseIsInCreative(int mouseX, int mouseY)
    {
        float x = mouseGetSimX(mouseX);
        float y = mouseGetSimY(mouseY);
        x -= creativeLeft;
        y -= creativeBottom;
        x += cellBorder;
        y += cellBorder;
        if(x >= 0 && x < cellSize * CREATIVE_COLUMNS && y >= 0
                && y < cellSize * Block.CHEST_ROWS)
            return true;
        return false;
    }

    private int mouseGetCreativeY(int mouseX, int mouseY)
    {
        float x = mouseGetSimX(mouseX);
        float y = mouseGetSimY(mouseY);
        x -= creativeLeft;
        y -= creativeBottom;
        x += cellBorder;
        y += cellBorder;
        if(x >= 0 && x < cellSize * CREATIVE_COLUMNS && y >= 0
                && y < cellSize * Block.CHEST_ROWS)
            return Block.CHEST_ROWS - (int)Math.floor(y / cellSize) - 1;
        return -1;
    }

    private int mouseGetCreativeX(int mouseX, int mouseY)
    {
        float x = mouseGetSimX(mouseX);
        float y = mouseGetSimY(mouseY);
        x -= creativeLeft;
        y -= creativeBottom;
        x += cellBorder;
        y += cellBorder;
        if(x >= 0 && x < cellSize * CREATIVE_COLUMNS && y >= 0
                && y < cellSize * Block.CHEST_ROWS)
            return (int)Math.floor(x / cellSize);
        return -1;
    }

    private boolean mouseIsInCreativeUpButton(int mouseX, int mouseY)
    {
        float x = mouseGetSimX(mouseX);
        float y = mouseGetSimY(mouseY);
        x -= creativeUpLeft;
        y -= creativeUpBottom;
        if(x >= 0 && x < creativeButtonSize && y >= 0
                && y < creativeButtonSize * Block.CHEST_ROWS)
            return true;
        return false;
    }

    private boolean mouseIsInCreativeDownButton(int mouseX, int mouseY)
    {
        float x = mouseGetSimX(mouseX);
        float y = mouseGetSimY(mouseY);
        x -= creativeDownLeft;
        y -= creativeDownBottom;
        if(x >= 0 && x < creativeButtonSize && y >= 0
                && y < creativeButtonSize * Block.CHEST_ROWS)
            return true;
        return false;
    }

    private boolean mouseIsInInventoryOrHotbar(int mouseX, int mouseY)
    {
        float mx = mouseGetSimX(mouseX);
        float my = mouseGetSimY(mouseY);
        float x = mx, y = my;
        x -= inventoryLeft;
        y -= inventoryBottom;
        x += cellBorder;
        y += cellBorder;
        if(x >= 0 && x < cellSize * Block.CHEST_COLUMNS && y >= 0
                && y < cellSize * Block.CHEST_ROWS)
            return true;
        x = mx - hotbarLeft;
        y = my - hotbarBottom;
        x += cellBorder;
        y += cellBorder;
        if(x >= 0 && x < cellSize * Block.CHEST_COLUMNS && y >= 0
                && y < cellSize)
            return true;
        return false;
    }

    private int mouseGetInventoryOrHotbarRow(int mouseX, int mouseY)
    {
        float mx = mouseGetSimX(mouseX);
        float my = mouseGetSimY(mouseY);
        float x = mx, y = my;
        x -= inventoryLeft;
        y -= inventoryBottom;
        x += cellBorder;
        y += cellBorder;
        if(x >= 0 && x < cellSize * Block.CHEST_COLUMNS && y >= 0
                && y < cellSize * Block.CHEST_ROWS)
            return (int)Math.floor(y / cellSize);
        x = mx - hotbarLeft;
        y = my - hotbarBottom;
        x += cellBorder;
        y += cellBorder;
        if(x >= 0 && x < cellSize * Block.CHEST_COLUMNS && y >= 0
                && y < cellSize)
            return Block.CHEST_ROWS;
        return -1;
    }

    private int mouseGetInventoryOrHotbarColumn(int mouseX, int mouseY)
    {
        float mx = mouseGetSimX(mouseX);
        float my = mouseGetSimY(mouseY);
        float x = mx, y = my;
        x -= inventoryLeft;
        y -= inventoryBottom;
        x += cellBorder;
        y += cellBorder;
        if(x >= 0 && x < cellSize * Block.CHEST_COLUMNS && y >= 0
                && y < cellSize * Block.CHEST_ROWS)
            return (int)Math.floor(x / cellSize);
        x = mx - hotbarLeft;
        y = my - hotbarBottom;
        x += cellBorder;
        y += cellBorder;
        if(x >= 0 && x < cellSize * Block.CHEST_COLUMNS && y >= 0
                && y < cellSize)
            return (int)Math.floor(x / cellSize);
        return -1;
    }

    private Block getCurrentHotbarBlock()
    {
        return this.blockType[getInventoryIndex(Block.CHEST_ROWS,
                                                this.selectionX)];
    }

    /** @author jacob */
    public static enum MouseMoveKind
    {
        /**
         * 
         */
        Normal,
        /**
         * 
         */
        Grabbed,
        /**
         * 
         */
        GrabbedAndCentered
    }

    /** @param mouseX
     *            mouse x position
     * @param mouseY
     *            mouse y position
     * @param mouseLButton
     *            if the mouse's left button is pressed
     * @return the mouse move kind */
    public MouseMoveKind handleMouseMove(int mouseX,
                                         int mouseY,
                                         boolean mouseLButton)
    {
        if(this.paused)
        {
            this.wasPaused = true;
            this.deleteAnimTime = -1;
            return MouseMoveKind.Normal;
        }
        switch(this.state)
        {
        case Normal:
        {
            if(this.wasPaused)
            {
                this.wasPaused = false;
                this.deleteAnimTime = -1;
                return MouseMoveKind.GrabbedAndCentered;
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
                Block curHotbar = getCurrentHotbarBlock();
                if(curHotbar != null && b != null)
                    timeDivisor = curHotbar.getDigAbility(b.getNeedBreakToDig());
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
                    return MouseMoveKind.GrabbedAndCentered;
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
            return MouseMoveKind.GrabbedAndCentered;
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
                this.dragX = mouseGetSimX(mouseX);
                this.dragY = mouseGetSimY(mouseY);
            }
            return (this.dragCount > 0) ? MouseMoveKind.Grabbed
                    : MouseMoveKind.Normal;
        }
        case Chest:
        {
            this.wasPaused = true;
            this.deleteAnimTime = -1;
            if(mouseX != this.lastMouseX || mouseY != this.lastMouseY)
            {
                this.lastMouseX = mouseX;
                this.lastMouseY = mouseY;
                this.dragX = mouseGetSimX(mouseX);
                this.dragY = mouseGetSimY(mouseY);
            }
            return (this.dragCount > 0) ? MouseMoveKind.Grabbed
                    : MouseMoveKind.Normal;
        }
        case Furnace:
        {
            this.wasPaused = true;
            this.deleteAnimTime = -1;
            if(mouseX != this.lastMouseX || mouseY != this.lastMouseY)
            {
                this.lastMouseX = mouseX;
                this.lastMouseY = mouseY;
                this.dragX = mouseGetSimX(mouseX);
                this.dragY = mouseGetSimY(mouseY);
            }
            return (this.dragCount > 0) ? MouseMoveKind.Grabbed
                    : MouseMoveKind.Normal;
        }
        }
        return MouseMoveKind.Normal;
    }

    private void nextCurBlockType()
    {
        this.selectionX = (this.selectionX + 1) % Block.CHEST_COLUMNS;
    }

    private void prevCurBlockType()
    {
        this.selectionX = (this.selectionX + Block.CHEST_COLUMNS - 1)
                % Block.CHEST_COLUMNS;
    }

    // returns the number of block given
    private int giveBlock(Block b, int count, int row, int column)
    {
        if(count <= 0)
            return 0;
        int index = getInventoryIndex(row, column);
        if(this.blockCount[index] <= 0)
        {
            this.blockCount[index] = Math.min(count, Block.BLOCK_STACK_SIZE);
            this.blockType[index] = b;
            return this.blockCount[index];
        }
        if(this.blockType[index].equals(b))
        {
            int origCount = this.blockCount[index];
            this.blockCount[index] = Math.min(count + origCount,
                                              Block.BLOCK_STACK_SIZE);
            return this.blockCount[index] - origCount;
        }
        return 0;
    }

    /** give this player a block
     * 
     * @param b
     *            the block to give
     * @param setCurrentBlock
     *            if <code>b</code> should be set as the players currently
     *            selected block
     * @return if the block can be given to this player */
    public boolean giveBlock(Block b, boolean setCurrentBlock)
    {
        if(b == null || b.getType() == BlockType.BTEmpty)
            return true;
        for(int row = Block.CHEST_ROWS; row >= 0; row--)
        {
            for(int column = 0; column < Block.CHEST_COLUMNS; column++)
            {
                if(giveBlock(b, 1, row, column) > 0)
                {
                    if(setCurrentBlock)
                    {
                        if(row == Block.CHEST_ROWS)
                            this.selectionX = column;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /** @return the block taken from this player or <code>null</code> */
    public Block takeBlock()
    {
        int index = getInventoryIndex(Block.CHEST_ROWS, this.selectionX);
        if(this.blockCount[index] <= 0)
            return null;
        Block retval = this.blockType[index];
        if(--this.blockCount[index] <= 0)
            this.blockType[index] = null;
        return retval;
    }

    private Block takeBlock(Block type)
    {
        if(type == null || type.getType() == BlockType.BTEmpty)
            return null;
        for(int row = Block.CHEST_ROWS; row >= 0; row--)
        {
            for(int column = 0; column < Block.CHEST_COLUMNS; column++)
            {
                int index = getInventoryIndex(row, column);
                if(this.blockCount[index] <= 0)
                    continue;
                Block retval = this.blockType[index];
                if(--this.blockCount[index] <= 0)
                    this.blockType[index] = null;
                return retval;
            }
        }
        return null;
    }

    private void runWorkbenchReduce()
    {
        int count;
        Block.ReduceDescriptor rd = Block.reduce(this.workbench,
                                                 this.workbenchSize);
        Block b = rd.b;
        count = rd.count;
        if(rd.isEmpty())
            return;
        if((this.dragCount <= 0 || this.dragType.equals(b))
                && this.dragCount + count <= Block.BLOCK_STACK_SIZE)
        {
            for(int x = 0; x < this.workbenchSize; x++)
            {
                for(int y = 0; y < this.workbenchSize; y++)
                {
                    this.workbench[x + this.workbenchSize * y] = takeBlock(this.workbench[x
                            + this.workbenchSize * y]);
                }
            }
            this.dragCount += count;
            this.dragType = b;
        }
    }

    private boolean canPlaceBlock(Block selb, int bx, int by, int bz)
    {
        if(selb == null || !this.isShiftDown || this.state != State.Normal
                || getCurrentHotbarBlock() == null
                || getCurrentHotbarBlock().isItem())
            return false;
        if(Math.floor(this.position.x) == bx
                && Math.floor(this.position.y) == by
                && Math.floor(this.position.z) == bz
                && !getCurrentHotbarBlock().isPlaceableWhileInside())
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
            while(b1 != null
                    && b2 != null
                    && (b1.adjustPlayerPosition(newpos.sub(new Vector(x, y, z)),
                                                distLimit) == null || b2.adjustPlayerPosition(newpos.sub(new Vector(x,
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

    private boolean handleInventoryOrHotbarClick(final Main.MouseEvent event)
    {
        if(!event.isDown || event.button != Main.MouseEvent.LEFT)
            return false;
        if(mouseIsInInventoryOrHotbar(event.mouseX, event.mouseY))
        {
            int row = mouseGetInventoryOrHotbarRow(event.mouseX, event.mouseY);
            int column = mouseGetInventoryOrHotbarColumn(event.mouseX,
                                                         event.mouseY);
            int index = getInventoryIndex(row, column);
            if(this.dragCount <= 0)
            {
                if(this.blockCount[index] > 0)
                {
                    this.dragCount = this.blockCount[index];
                    this.dragType = this.blockType[index];
                    this.blockCount[index] = 0;
                    this.blockType[index] = null;
                }
            }
            else if(this.blockCount[index] <= 0)
            {
                this.blockCount[index] = this.dragCount;
                this.blockType[index] = this.dragType;
                this.dragCount = 0;
                this.dragType = null;
            }
            else if(this.blockType[index].equals(this.dragType)) // pick
                                                                 // up
                                                                 // more
                                                                 // blocks
            {
                int transferCount = Math.min(this.blockCount[index],
                                             Block.BLOCK_STACK_SIZE
                                                     - this.dragCount);
                this.dragCount += transferCount;
                this.blockCount[index] -= transferCount;
                if(this.blockCount[index] <= 0)
                    this.blockType[index] = null;
            }
            return true;
        }
        return false;
    }

    private static int getMaxCreativeOffset()
    {
        return Math.max(0, (BlockType.getCreativeModeInventorySize()
                + CREATIVE_COLUMNS - 1)
                / CREATIVE_COLUMNS - Block.CHEST_ROWS);
    }

    /** @param event
     *            the event */
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
                    Block newb = takeBlock();
                    if(newb != null)
                        newb = newb.makePlacedBlock(this.blockOrientation,
                                                    Block.getOrientationFromVector(getForwardVector()));
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
                    didAction = true;
                }
                else if(b != null && b.getType() == BlockType.BTWorkbench)
                {
                    this.workbenchSize = 3;
                    this.state = State.Workbench;
                    for(int x = 0; x < this.workbenchSize; x++)
                        for(int y = 0; y < this.workbenchSize; y++)
                            this.workbench[x + y * this.workbenchSize] = null;
                    this.lastMouseX = -1;
                    this.lastMouseY = -1;
                    this.dragCount = 0;
                    this.dragType = null;
                    didAction = true;
                }
                else if(b != null && b.getType() == BlockType.BTFurnace)
                {
                    this.state = State.Furnace;
                    this.dragCount = 0;
                    this.dragType = null;
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
            if(!handleInventoryOrHotbarClick(event))
            {
                if(event.isDown && event.button == Main.MouseEvent.LEFT)
                {
                    if(this.workbenchSize == 2 && Main.isCreativeMode)
                    {
                        if(mouseIsInCreative(event.mouseX, event.mouseY))
                        {
                            int x = mouseGetCreativeX(event.mouseX,
                                                      event.mouseY);
                            int y = mouseGetCreativeY(event.mouseX,
                                                      event.mouseY);
                            int index = (y + this.creativeOffset)
                                    * CREATIVE_COLUMNS + x;
                            final Block b;
                            if(index < BlockType.getCreativeModeInventorySize())
                                b = BlockType.getCreativeModeInventoryBlock(index);
                            else
                                b = null;
                            final int count;
                            if(b == null)
                                count = 0;
                            else
                                count = Block.BLOCK_STACK_SIZE;
                            if(this.dragCount <= 0)
                            {
                                if(count > 0)
                                {
                                    this.dragCount = count;
                                    this.dragType = b;
                                }
                            }
                            else if(count <= 0)
                            {
                                this.dragCount = 0;
                                this.dragType = null;
                            }
                            else if(b.equals(this.dragType))
                                this.dragCount = Block.BLOCK_STACK_SIZE;
                        }
                        else if(mouseIsInCreativeUpButton(event.mouseX,
                                                          event.mouseY))
                        {
                            if(this.creativeOffset > 0)
                                this.creativeOffset--;
                        }
                        else if(mouseIsInCreativeDownButton(event.mouseX,
                                                            event.mouseY))
                        {
                            if(this.creativeOffset < getMaxCreativeOffset())
                                this.creativeOffset++;
                        }
                    }
                    else if(mousePosToSelPosWorkbench(event.mouseX,
                                                      event.mouseY))
                    {
                        if(this.workbench[this.workbenchSelX
                                + this.workbenchSize * this.workbenchSelY] == null)
                        {
                            if(this.dragCount > 0)
                            {
                                this.workbench[this.workbenchSelX
                                        + this.workbenchSize
                                        * this.workbenchSelY] = this.dragType;
                                if(--this.dragCount <= 0)
                                    this.dragType = null;
                            }
                        }
                        else
                        {
                            if((this.dragCount <= 0 || this.dragType.equals(this.workbench[this.workbenchSelX
                                    + this.workbenchSize * this.workbenchSelY]))
                                    && this.dragCount < Block.BLOCK_STACK_SIZE)
                            {
                                this.dragCount++;
                                this.dragType = this.workbench[this.workbenchSelX
                                        + this.workbenchSize
                                        * this.workbenchSelY];
                                this.workbench[this.workbenchSelX
                                        + this.workbenchSize
                                        * this.workbenchSelY] = null;
                            }
                        }
                    }
                    else if(mouseIsInResultWorkbench(event.mouseX, event.mouseY))
                    {
                        runWorkbenchReduce();
                    }
                }
                else if(!event.isDown)
                {
                }
            }
            break;
        }
        case Chest:
        {
            this.deleteAnimTime = -1;
            if(!handleInventoryOrHotbarClick(event))
            {
                if(event.isDown && event.button == Main.MouseEvent.LEFT)
                {
                    if(mouseIsInChest(event.mouseX, event.mouseY))
                    {
                        int row = mouseGetChestRow(event.mouseX, event.mouseY);
                        int column = mouseGetChestColumn(event.mouseX,
                                                         event.mouseY);
                        Block chest = world.getBlock(this.blockX,
                                                     this.blockY,
                                                     this.blockZ);
                        if(chest == null
                                || chest.getType() != BlockType.BTChest)
                        {
                            for(int i = 0; i < this.dragCount; i++)
                                giveBlock(this.dragType, false);
                            this.state = State.Normal;
                            return;
                        }
                        chest = new Block(chest);
                        if(this.dragCount <= 0)
                        {
                            if(chest.chestGetBlockCount(row, column) > 0)
                            {
                                this.dragType = chest.chestGetBlockType(row,
                                                                        column);
                                this.dragCount = chest.chestRemoveBlocks(this.dragType,
                                                                         chest.chestGetBlockCount(row,
                                                                                                  column),
                                                                         row,
                                                                         column);
                                if(this.dragCount <= 0)
                                    this.dragType = null;
                            }
                        }
                        else if(chest.chestGetBlockCount(row, column) <= 0)
                        {
                            this.dragCount -= chest.chestAddBlocks(this.dragType,
                                                                   this.dragCount,
                                                                   row,
                                                                   column);
                            if(this.dragCount <= 0)
                                this.dragType = null;
                        }
                        else
                        // pick
                        // up
                        // more
                        // blocks
                        {
                            int transferCount = Math.min(chest.chestGetBlockCount(row,
                                                                                  column),
                                                         Block.BLOCK_STACK_SIZE
                                                                 - this.dragCount);
                            transferCount = chest.chestRemoveBlocks(this.dragType,
                                                                    transferCount,
                                                                    row,
                                                                    column);
                            this.dragCount += transferCount;
                        }
                        // world.addModNode(blockX, blockY, blockZ, chest);
                        // TODO finish
                        world.setBlock(this.blockX,
                                       this.blockY,
                                       this.blockZ,
                                       chest);
                    }
                }
                else if(event.isDown && event.button == Main.MouseEvent.RIGHT)
                {
                }
            }
            break;
        }
        case Furnace:
        {
            this.deleteAnimTime = -1;
            if(!handleInventoryOrHotbarClick(event))
            {
                if(event.isDown && event.button == Main.MouseEvent.LEFT)
                {
                    if(mouseIsInFireFurnace(event.mouseX, event.mouseY))
                    {
                        if(this.dragCount > 0)
                        {
                            Block b = world.getBlock(this.blockX,
                                                     this.blockY,
                                                     this.blockZ);
                            if(b != null && b.getType() == BlockType.BTFurnace)
                                b = new Block(b);
                            else
                            {
                                for(int i = 0; i < this.dragCount; i++)
                                    giveBlock(this.dragType, false);
                                this.state = State.Normal;
                                return;
                            }
                            if(this.dragType.getBurnTime() > 0)
                            {
                                b.furnaceAddFire(this.dragType);
                                if(--this.dragCount <= 0)
                                    this.dragType = null;
                            }
                            // world.addModNode(blockX, blockY, blockZ, b);
                            // TODO finish
                            world.setBlock(this.blockX,
                                           this.blockY,
                                           this.blockZ,
                                           b);
                        }
                    }
                    else if(mouseIsInSourceFurnace(event.mouseX, event.mouseY))
                    {
                        if(this.dragCount > 0)
                        {
                            Block b = world.getBlock(this.blockX,
                                                     this.blockY,
                                                     this.blockZ);
                            if(b != null && b.getType() == BlockType.BTFurnace)
                                b = new Block(b);
                            else
                            {
                                for(int i = 0; i < this.dragCount; i++)
                                    giveBlock(this.dragType, false);
                                this.state = State.Normal;
                                return;
                            }
                            if(b.furnaceAddBlock(this.dragType))
                                if(--this.dragCount <= 0)
                                    this.dragType = null;
                            // world.addModNode(blockX, blockY, blockZ, b);
                            // TODO finish
                            world.setBlock(this.blockX,
                                           this.blockY,
                                           this.blockZ,
                                           b);
                        }
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
                            for(int i = 0; i < this.dragCount; i++)
                                giveBlock(this.dragType, false);
                            this.state = State.Normal;
                            return;
                        }
                        if((this.dragCount <= 0 || this.dragType.equals(b.furnaceGetDestBlock()))
                                && this.dragCount < Block.BLOCK_STACK_SIZE)
                        {
                            Block newB = b.furnaceRemoveBlock();
                            if(newB != null)
                            {
                                this.dragCount++;
                                this.dragType = newB;
                            }
                        }
                        // world.addModNode(blockX, blockY, blockZ, b);
                        // TODO finish
                        world.setBlock(this.blockX, this.blockY, this.blockZ, b);
                    }
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

    private void checkForStandingOnPressurePlate()
    {
        Vector newpos = this.position.add(new Vector(0, 0.25f, 0));
        int x = (int)Math.floor(newpos.x), y = (int)Math.floor(newpos.y), z = (int)Math.floor(newpos.z);
        Block b = world.getBlockEval(x, y - 1, z);
        if(b != null
                && (b.getType() == BlockType.BTWoodPressurePlate || b.getType() == BlockType.BTStonePressurePlate))
        {
            b = new Block(b);
            b.pressurePlatePress();
            world.setBlock(x, y - 1, z, b);
        }
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
            boolean isFlying = Main.isKeyDown(Main.KEY_F)
                    && Main.isCreativeMode;
            boolean inWater = isInWater();
            boolean inLadder = isInLadder();
            Vector origvelocity = new Vector(this.velocity);
            if(isFlying)
            {
                float newMag = this.velocity.abs() - 15
                        * (float)Main.getFrameDuration();
                if(newMag <= 0)
                    this.velocity = new Vector(0);
                else
                    this.velocity = this.velocity.normalize().mul(newMag);
            }
            else if(inWater)
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
                checkForStandingOnPressurePlate();
                this.velocity = new Vector(0);
            }
            Vector forwardVec;
            if(isFlying)
                forwardVec = getForwardVector();
            else if(inWater)
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
            if(!isFlying && inWater && !isMoving)
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

    /** @param paused
     *            if this player should be paused */
    public void setPaused(boolean paused)
    {
        if(this.paused || paused)
            this.wasPaused = true;
        this.paused = paused;
    }

    private void quitToNormal()
    {
        switch(this.state)
        {
        case Normal:
            return;
        case Workbench:
        {
            this.deleteAnimTime = -1;
            for(int i = 0; i < this.dragCount; i++)
                giveBlock(this.dragType, false);
            this.state = State.Normal;
            for(int x = 0; x < this.workbenchSize; x++)
            {
                for(int y = 0; y < this.workbenchSize; y++)
                {
                    if(this.workbench[x + this.workbenchSize * y] == null)
                        continue;
                    giveBlock(this.workbench[x + this.workbenchSize * y], false);
                    this.workbench[x + this.workbenchSize * y] = null;
                }
            }
            this.wasPaused = true;
            break;
        }
        case Chest:
        {
            this.deleteAnimTime = -1;
            for(int i = 0; i < this.dragCount; i++)
                giveBlock(this.dragType, false);
            this.state = State.Normal;
            this.wasPaused = true;
            break;
        }
        case Furnace:
        {
            this.deleteAnimTime = -1;
            for(int i = 0; i < this.dragCount; i++)
                giveBlock(this.dragType, false);
            this.state = State.Normal;
            this.wasPaused = true;
            break;
        }
        }
    }

    /** @param event
     *            the event to handle */
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
                            this.workbench[x + y * this.workbenchSize] = null;
                    this.lastMouseX = -1;
                    this.lastMouseY = -1;
                }
                break;
            }
            case Workbench:
            {
                this.deleteAnimTime = -1;
                if(event.key == Main.KEY_ESCAPE || event.key == Main.KEY_Q)
                    quitToNormal();
                break;
            }
            case Chest:
            {
                this.deleteAnimTime = -1;
                if(event.key == Main.KEY_ESCAPE || event.key == Main.KEY_Q)
                    quitToNormal();
                break;
            }
            case Furnace:
            {
                this.deleteAnimTime = -1;
                if(event.key == Main.KEY_ESCAPE || event.key == Main.KEY_Q)
                    quitToNormal();
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

    /** write to a <code>DataOutput</code>
     * 
     * @param o
     *            <code>OutputStream</code> to write to
     * @throws IOException
     *             the exception thrown */
    public void write(DataOutput o) throws IOException
    {
        this.position.write(o);
        this.velocity.write(o);
        o.writeFloat(this.viewPhi);
        o.writeFloat(this.viewTheta);
        if(this.state != State.Normal)
        {
            quitToNormal();
        }
        for(int i = 0; i < (Block.CHEST_ROWS + 1) * Block.CHEST_COLUMNS; i++)
        {
            o.writeInt(this.blockCount[i]);
            if(this.blockCount[i] > 0)
            {
                this.blockType[i].write(o);
            }
        }
    }

    /** read from a <code>DataInput</code>
     * 
     * @param i
     *            <code>DataInput</code> to read from
     * @return the read <code>Player</code>
     * @throws IOException
     *             the exception thrown */
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
        for(int index = 0; index < (Block.CHEST_ROWS + 1) * Block.CHEST_COLUMNS; index++)
        {
            retval.blockCount[index] = i.readInt();
            if(retval.blockCount[index] < 0
                    || retval.blockCount[index] > Block.BLOCK_STACK_SIZE)
                throw new IOException("inventory cell block count out of range");
            if(retval.blockCount[index] > 0)
                retval.blockType[index] = Block.read(i);
            else
                retval.blockType[index] = null;
        }
        return retval;
    }

    /** push this player if it's inside of &lt;<code>bx</code>, <code>by</code>,
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
     *            z coordinate of the direction to push */
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
