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

import static org.voxels.Matrix.glLoadMatrix;
import static org.voxels.PlayerList.players;
import static org.voxels.Vector.glVertex;
import static org.voxels.World.world;

import java.io.*;

import org.voxels.BlockType.ToolLevel;
import org.voxels.BlockType.ToolType;
import org.voxels.platform.Mouse;

/** @author jacob */
public class Player implements GameObject
{
    /**
	 * 
	 */
    public static final float PlayerHeight = 2.0f;
    private Vector position = Vector.allocate(0.5f, 1.5f, 0.5f);
    private Vector velocity = Vector.allocate(0);
    private float viewTheta = 0.0f;
    private float viewPhi = 0.0f;
    private static final float selectionDist = 8.0f;
    private boolean wasPaused = true;
    private int blockCount[] = new int[(Block.CHEST_ROWS + 1)
            * Block.CHEST_COLUMNS];
    private Block blockType[] = new Block[(Block.CHEST_ROWS + 1)
            * Block.CHEST_COLUMNS];
    private int selectionX = 0;
    private boolean isShiftDown = false;
    private float lastMouseX = -1, lastMouseY = -1;
    private static final float distLimit = 0.2f;
    private int dragCount = 0;
    private Block dragType = null;
    private float dragX = 0, dragY = 0;
    private int creativeOffset = 0;
    private float startMouseX = -1, startMouseY = -1;
    private boolean isDragOperation = false;
    private boolean isFlying = false, isSneaking = false;
    private float touchWaitTime = 0.0f;

    private static int getInventoryIndex(final int row, final int column)
    {
        return row * Block.CHEST_COLUMNS + column;
    }

    private enum State
    {
        Normal, Workbench, Chest, Furnace, DispenserDropper, Hopper
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
        Block b;
        int count = 0;
        final int yoffset = World.Depth - 5;
        this.position.setY(-yoffset);
        for(;;)
        {
            b = world.getBlockEval((int)Math.floor(this.position.getX()),
                                   (int)Math.floor(this.position.getY() + 1
                                           + yoffset),
                                   (int)Math.floor(this.position.getZ()));
            while(b == null)
            {
                world.flagGenerate((int)Math.floor(this.position.getX()),
                                   (int)Math.floor(this.position.getY() + 1
                                           + yoffset),
                                   (int)Math.floor(this.position.getZ()));
                world.generateChunks();
                b = world.getBlockEval((int)Math.floor(this.position.getX()),
                                       (int)Math.floor(this.position.getY() + 1
                                               + yoffset),
                                       (int)Math.floor(this.position.getZ()));
                if(b == null)
                    Thread.yield();
            }
            if(b.isPlaceableWhileInside())
            {
                if(++count >= 2 + yoffset)
                    break;
            }
            else
                count = 0;
            this.position.setY(this.position.getY() + 1.0f);
        }
    }

    private Player(final Vector position)
    {
        this.position = position;
    }

    /** @return true if this player is in normal(3D) state */
    public boolean isNormalState()
    {
        if(this.state == State.Normal)
            return true;
        return false;
    }

    private Matrix getWorldToCamera_retval = new Matrix();
    private Matrix getWorldToCamera_t1 = new Matrix();
    private Vector getWorldToCamera_t2 = Vector.allocate();

    /** @return the transformation that converts world coordinates to this
     *         player's camera coordinates */
    public Matrix getWorldToCamera()
    {
        return Matrix.setToTranslate(this.getWorldToCamera_retval,
                                     Vector.neg(this.getWorldToCamera_t2,
                                                this.position))
                     .concatAndSet(Matrix.setToRotateY(this.getWorldToCamera_t1,
                                                       this.viewTheta))
                     .concatAndSet(Matrix.setToRotateX(this.getWorldToCamera_t1,
                                                       this.viewPhi));
    }

    private Vector getForwardVector_retval = Vector.allocate();
    private Matrix getForwardVector_t1 = new Matrix();
    private Matrix getForwardVector_t2 = new Matrix();

    /** @return the vector pointing in the direction this player is looking */
    public Vector getForwardVector()
    {
        return Matrix.setToRotateX(this.getForwardVector_t1, -this.viewPhi)
                     .concatAndSet(Matrix.setToRotateY(this.getForwardVector_t2,
                                                       -this.viewTheta))
                     .apply(this.getForwardVector_retval,
                            Vector.set(this.getForwardVector_retval,
                                       0.0f,
                                       0.0f,
                                       -1.0f));
    }

    private Vector getMoveForwardVector_retval = Vector.allocate();
    private Matrix getMoveForwardVector_t1 = new Matrix();

    /** @return the vector pointing in the direction this player is facing */
    public Vector getMoveForwardVector()
    {
        return Matrix.setToRotateY(this.getMoveForwardVector_t1,
                                   -this.viewTheta)
                     .apply(this.getMoveForwardVector_retval,
                            Vector.set(this.getMoveForwardVector_retval,
                                       0.0f,
                                       0.0f,
                                       -1.0f));
    }

    private static Vector getPosition_retval = Vector.allocate();

    /** @return this player's position */
    public Vector getPosition()
    {
        return getPosition_retval.set(this.position);
    }

    private World.BlockHitDescriptor getSelectedBlock_t1 = new World.BlockHitDescriptor();

    private Block getSelectedBlock()
    {
        World.BlockHitDescriptor bhd = world.getPointedAtBlock(this.getSelectedBlock_t1,
                                                               getWorldToCamera(),
                                                               selectionDist,
                                                               this.isShiftDown);
        this.blockX = bhd.x;
        this.blockY = bhd.y;
        this.blockZ = bhd.z;
        this.blockOrientation = bhd.orientation;
        return bhd.b;
    }

    private void internalDrawSelectedBlockH(final float minX,
                                            final float maxX,
                                            final float minY,
                                            final float maxY,
                                            final float minZ,
                                            final float maxZ)
    {
        Main.opengl.glVertex3f(minX, minY, minZ);
        Main.opengl.glVertex3f(maxX, minY, minZ);
        Main.opengl.glVertex3f(minX, minY, minZ);
        Main.opengl.glVertex3f(minX, maxY, minZ);
        Main.opengl.glVertex3f(maxX, maxY, minZ);
        Main.opengl.glVertex3f(maxX, minY, minZ);
        Main.opengl.glVertex3f(maxX, maxY, minZ);
        Main.opengl.glVertex3f(minX, maxY, minZ);
        Main.opengl.glVertex3f(minX, minY, maxZ);
        Main.opengl.glVertex3f(maxX, minY, maxZ);
        Main.opengl.glVertex3f(minX, minY, maxZ);
        Main.opengl.glVertex3f(minX, maxY, maxZ);
        Main.opengl.glVertex3f(maxX, maxY, maxZ);
        Main.opengl.glVertex3f(maxX, minY, maxZ);
        Main.opengl.glVertex3f(maxX, maxY, maxZ);
        Main.opengl.glVertex3f(minX, maxY, maxZ);
        Main.opengl.glVertex3f(minX, minY, minZ);
        Main.opengl.glVertex3f(minX, minY, maxZ);
        Main.opengl.glVertex3f(maxX, minY, minZ);
        Main.opengl.glVertex3f(maxX, minY, maxZ);
        Main.opengl.glVertex3f(maxX, maxY, minZ);
        Main.opengl.glVertex3f(maxX, maxY, maxZ);
        Main.opengl.glVertex3f(minX, maxY, minZ);
        Main.opengl.glVertex3f(minX, maxY, maxZ);
    }

    void internalDrawSelectedBlock(final float minX,
                                   final float maxX,
                                   final float minY,
                                   final float maxY,
                                   final float minZ,
                                   final float maxZ)
    {
        final float selectionExpand = 0.05f;
        Main.opengl.glColor4f(0.75f, 0.75f, 0.75f, 0);
        Image.unselectTexture();
        Main.opengl.glBegin(Main.opengl.GL_LINES());
        for(int dx = -1; dx <= 1; dx += 2)
            for(int dy = -1; dy <= 1; dy += 2)
                for(int dz = -1; dz <= 1; dz += 2)
                    internalDrawSelectedBlockH(minX - selectionExpand * dx,
                                               maxX + selectionExpand * dx,
                                               minY - selectionExpand * dy,
                                               maxY + selectionExpand * dy,
                                               minZ - selectionExpand * dz,
                                               maxZ + selectionExpand * dz);
        Main.opengl.glEnd();
    }

    private void drawBlockSelection(final Matrix world2Camera,
                                    final int x,
                                    final int y,
                                    final int z)
    {
        Main.opengl.glMatrixMode(Main.opengl.GL_MODELVIEW());
        Main.opengl.glPushMatrix();
        glLoadMatrix(world2Camera);
        internalDrawSelectedBlock(x, x + 1, y, y + 1, z, z + 1);
        Main.opengl.glMatrixMode(Main.opengl.GL_MODELVIEW());
        Main.opengl.glPopMatrix();
    }

    private static Image workbenchImg = new Image("workbench.png");
    private static Image inventoryImg = new Image("inventory.png");
    private static Image creativeImg = new Image("creative.png");
    private static Image chestImg = new Image("chestedit.png");
    private static Image furnaceImg = new Image("furnace.png");
    private static Image hotbarBoxImg = new Image("hotbarbox.png");
    private static Image dispenserDropperImg = new Image("dispenserdropper.png");
    private static Image hopperImg = new Image("hopper.png");
    private static final float workbenchZDist = -1.0f;
    private static final float simScreenHeight = 240f;
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
    private static final float touchButtonWidth = 0.25f,
            touchButtonHeight = 0.25f;
    private static final int dispenserDropperLeft = 59,
            dispenserDropperBottom = 255 - 166;
    private static final int hopperLeft = 41;
    private static final int hopperBottom = 255 - 148;

    private float getPlaceButtonLeft()
    {
        return Main.aspectRatio() - touchButtonWidth;
    }

    private float getPlaceButtonBottom()
    {
        return -touchButtonHeight / 2;
    }

    private float getReturnButtonLeft()
    {
        return Main.aspectRatio() - touchButtonWidth;
    }

    private float getReturnButtonBottom()
    {
        return -touchButtonHeight / 2;
    }

    private float getPauseButtonLeft()
    {
        return Main.aspectRatio() - touchButtonWidth;
    }

    private float getPauseButtonBottom()
    {
        return 1.0f - touchButtonHeight;
    }

    private float getInventoryButtonLeft()
    {
        return Main.aspectRatio() - touchButtonWidth;
    }

    private float getInventoryButtonBottom()
    {
        return -1.0f;
    }

    private float getMoveLeftButtonLeft()
    {
        return -Main.aspectRatio();
    }

    private float getMoveLeftButtonBottom()
    {
        return -touchButtonHeight / 2;
    }

    private float getMoveUpButtonLeft()
    {
        return -Main.aspectRatio() + touchButtonWidth;
    }

    private float getMoveUpButtonBottom()
    {
        return -touchButtonHeight / 2 + touchButtonHeight;
    }

    private float getMoveDownButtonLeft()
    {
        return -Main.aspectRatio() + touchButtonWidth;
    }

    private float getMoveDownButtonBottom()
    {
        return -touchButtonHeight / 2 - touchButtonHeight;
    }

    private float getMoveRightButtonLeft()
    {
        return -Main.aspectRatio() + touchButtonWidth * 2;
    }

    private float getMoveRightButtonBottom()
    {
        return -touchButtonHeight / 2;
    }

    private float getJumpButtonLeft()
    {
        return -Main.aspectRatio() + touchButtonWidth;
    }

    private float getJumpButtonBottom()
    {
        return -touchButtonHeight / 2;
    }

    private float getFlyButtonLeft()
    {
        return -Main.aspectRatio();
    }

    private float getFlyButtonBottom()
    {
        return 1.0f - touchButtonHeight;
    }

    private float getSneakButtonLeft()
    {
        return -Main.aspectRatio();
    }

    private float getSneakButtonBottom()
    {
        return -1.0f;
    }

    private static Matrix drawButton_t1 = new Matrix();
    private static Matrix drawButton_t2 = new Matrix();

    private void drawButton(final String text,
                            final float left,
                            final float bottom,
                            final boolean selected)
    {
        Image.unselectTexture();
        final float scaleFactor = 1.5f;
        final float right = left + touchButtonWidth, top = bottom
                + touchButtonHeight;
        if(selected)
            Main.opengl.glColor4f(0f, 0f, 1.0f, 0.0f);
        else
            Main.opengl.glColor4f(0.75f, 0.75f, 0.75f, 0.0f);
        Main.opengl.glBegin(Main.opengl.GL_TRIANGLES());
        Main.opengl.glVertex3f(left * scaleFactor,
                               bottom * scaleFactor,
                               -scaleFactor);
        Main.opengl.glVertex3f(right * scaleFactor,
                               bottom * scaleFactor,
                               -scaleFactor);
        Main.opengl.glVertex3f(right * scaleFactor,
                               top * scaleFactor,
                               -scaleFactor);
        Main.opengl.glVertex3f(right * scaleFactor,
                               top * scaleFactor,
                               -scaleFactor);
        Main.opengl.glVertex3f(left * scaleFactor,
                               top * scaleFactor,
                               -scaleFactor);
        Main.opengl.glVertex3f(left * scaleFactor,
                               bottom * scaleFactor,
                               -scaleFactor);
        Main.opengl.glEnd();
        final float textScale = 0.5f;
        Matrix textTransform = Matrix.setToScale(drawButton_t1,
                                                 2.0f * Text.sizeH("A")
                                                         / simScreenHeight
                                                         * textScale)
                                     .concatAndSet(Matrix.setToTranslate(drawButton_t2,
                                                                         (left + right)
                                                                                 / 2.0f
                                                                                 - textScale
                                                                                 * Text.sizeW(text)
                                                                                 / simScreenHeight,
                                                                         (bottom + top)
                                                                                 / 2.0f
                                                                                 - textScale
                                                                                 * Text.sizeH(text)
                                                                                 / simScreenHeight,
                                                                         -1.0f));
        Text.draw(textTransform, Color.V(0.0f), text);
    }

    private static Matrix drawCenteredText_t1 = new Matrix();
    private static Matrix drawCenteredText_t2 = new Matrix();

    private void drawCenteredText(final String str,
                                  final float xCenter,
                                  final float bottom)
    {
        if(str.length() <= 0)
            return;
        final Matrix imgMat = getImageMat();
        Matrix textTransform = Matrix.setToScale(drawCenteredText_t1,
                                                 Text.sizeH("A"))
                                     .concatAndSet(Matrix.setToTranslate(drawCenteredText_t2,
                                                                         xCenter
                                                                                 - Text.sizeW(str)
                                                                                 / 2.0f,
                                                                         bottom,
                                                                         0))
                                     .concatAndSet(imgMat)
                                     .concatAndSet(Matrix.setToScale(drawCenteredText_t2,
                                                                     0.7f));
        Text.draw(textTransform, str);
    }

    private static Matrix drawCell_t1 = new Matrix();
    private static Matrix drawCell_t2 = new Matrix();

    private void drawCell(final Block b,
                          final int count,
                          final float cellLeft,
                          final float cellBottom,
                          final boolean drawIfEmpty)
    {
        if(count <= 0 && !drawIfEmpty)
            return;
        final Matrix imgMat = getImageMat();
        if(b != null)
        {
            Matrix blockTransform = Matrix.setToScale(drawCell_t1, 16f)
                                          .concatAndSet(Matrix.setToTranslate(drawCell_t2,
                                                                              cellLeft,
                                                                              cellBottom,
                                                                              0))
                                          .concatAndSet(imgMat);
            RenderingStream.free(b.drawAsItem(RenderingStream.allocate(),
                                              blockTransform).render());
        }
        if(count > 1 || drawIfEmpty)
        {
            String str = Integer.toString(count);
            Matrix textTransform = Matrix.setToScale(drawCell_t1,
                                                     Text.sizeH("A"))
                                         .concatAndSet(Matrix.setToTranslate(drawCell_t2,
                                                                             cellLeft
                                                                                     + 16
                                                                                     - Text.sizeW(str),
                                                                             cellBottom,
                                                                             0))
                                         .concatAndSet(imgMat)
                                         .concatAndSet(Matrix.setToScale(drawCell_t2,
                                                                         0.7f));
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

    private static Matrix getImageMat_retval = new Matrix();
    private static Matrix getImageMat_t1 = new Matrix();

    private static Matrix getImageMatInternal()
    {
        final float imgW = dialogW / simScreenHeight, imgH = dialogH
                / simScreenHeight;
        return Matrix.setToScale(getImageMat_retval, 2f / simScreenHeight)
                     .concatAndSet(Matrix.setToTranslate(getImageMat_t1,
                                                         -imgW,
                                                         -imgH,
                                                         workbenchZDist));
    }

    private static final Matrix imageMat = getImageMatInternal().getImmutable();

    private static Matrix getImageMat()
    {
        return imageMat;
    }

    /** @param index
     *            the index */
    private float getHotbarLeft(final int index)
    {
        final float height = 2f * 20f / simScreenHeight;
        final float width = height, left = (index - Block.CHEST_COLUMNS / 2f)
                * width;
        return left;
    }

    /** @param index
     *            the index */
    private float getHotbarBottom(final int index)
    {
        final float height = 2f * 20f / simScreenHeight;
        final float top = -0.80f, bottom = top - height;
        return bottom;
    }

    /** @param index
     *            the index */
    private float getHotbarRight(final int index)
    {
        final float height = 2f * 20f / simScreenHeight;
        final float width = height, left = (index - Block.CHEST_COLUMNS / 2f)
                * width, right = left + width;
        return right;
    }

    /** @param index
     *            the index */
    private float getHotbarTop(final int index)
    {
        final float top = -0.80f;
        return top;
    }

    private static Matrix drawAll_t1 = new Matrix();
    private static Matrix drawAll_t2 = new Matrix();
    private static Vector drawAll_t3 = Vector.allocate();

    /** draw everything from this player's perspective */
    public void drawAll()
    {
        if(Main.DEBUG)
        {
            Main.addToFrameText("position : " + this.position.toString() + "\n");
            Main.addToFrameText("biome : " + world.getBiomeName(this.position)
                    + "\n");
        }
        Matrix worldToCamera = getWorldToCamera();
        RenderingStream rs = RenderingStream.allocate();
        RenderingStream trs = RenderingStream.allocate();
        players.drawPlayers(rs, worldToCamera);
        if(this.state == State.Normal)
        {
            Block b = getSelectedBlock();
            if(this.deleteAnimTime >= 0)
            {
                b = Block.NewDeleteAnim(this.deleteAnimTime);
                b.draw(trs,
                       Matrix.setToTranslate(drawAll_t1,
                                             this.blockX,
                                             this.blockY,
                                             this.blockZ)
                             .concatAndSet(worldToCamera));
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
        Main.opengl.glClear(Main.opengl.GL_DEPTH_BUFFER_BIT());
        Main.opengl.glMatrixMode(Main.opengl.GL_MODELVIEW());
        Main.opengl.glLoadIdentity();
        Image.unselectTexture();
        Main.opengl.glColor4f(1, 1, 1, 0);
        Main.opengl.glBegin(Main.opengl.GL_LINES());
        Main.opengl.glVertex3f(-1, 0, -100);
        Main.opengl.glVertex3f(1, 0, -100);
        Main.opengl.glVertex3f(1, 0, -100);
        Main.opengl.glVertex3f(-1, 0, -100);
        Main.opengl.glVertex3f(0, -1, -100);
        Main.opengl.glVertex3f(0, 1, -100);
        Main.opengl.glVertex3f(0, 1, -100);
        Main.opengl.glVertex3f(0, -1, -100);
        Main.opengl.glEnd();
        RenderingStream hotbarRS = RenderingStream.allocate();
        hotbarBoxImg.selectTexture();
        Main.opengl.glBegin(Main.opengl.GL_TRIANGLES());
        final float blockHeight = 2f * 16f / simScreenHeight;
        for(int i = 0; i < Block.CHEST_COLUMNS; i++)
        {
            final float zDist = -1f;
            final float maxU = 20f / 32f, maxV = 20f / 32f;
            final float left = getHotbarLeft(i);
            final float right = getHotbarRight(i);
            final float top = getHotbarTop(i);
            final float bottom = getHotbarBottom(i);
            final float height = top - bottom;
            final float width = right - left;
            if(i == this.selectionX)
                Main.opengl.glColor4f(0, 1, 0, 1);
            else
                Main.opengl.glColor4f(1, 1, 1, 1);
            Main.opengl.glTexCoord2f(0, 0);
            Main.opengl.glVertex3f(left, bottom, zDist);
            Main.opengl.glTexCoord2f(maxU, 0);
            Main.opengl.glVertex3f(right, bottom, zDist);
            Main.opengl.glTexCoord2f(maxU, maxV);
            Main.opengl.glVertex3f(right, top, zDist);
            Main.opengl.glTexCoord2f(maxU, maxV);
            Main.opengl.glVertex3f(right, top, zDist);
            Main.opengl.glTexCoord2f(0, maxV);
            Main.opengl.glVertex3f(left, top, zDist);
            Main.opengl.glTexCoord2f(0, 0);
            Main.opengl.glVertex3f(left, bottom, zDist);
            if(this.blockCount[getInventoryIndex(Block.CHEST_ROWS, i)] > 0)
            {
                Matrix tform = Matrix.setToTranslate(drawAll_t1, left + width
                        / 2f, bottom + height / 2f, -1f);
                tform = Matrix.setToScale(drawAll_t2, blockHeight)
                              .concatAndSet(tform);
                tform = Matrix.setToTranslate(drawAll_t1, -0.5f, -0.5f, 0)
                              .concatAndSet(tform);
                this.blockType[getInventoryIndex(Block.CHEST_ROWS, i)].drawAsItem(hotbarRS,
                                                                                  tform);
            }
        }
        Main.opengl.glEnd();
        Main.opengl.glClear(Main.opengl.GL_DEPTH_BUFFER_BIT());
        hotbarRS.render();
        RenderingStream.free(hotbarRS);
        Main.opengl.glClear(Main.opengl.GL_DEPTH_BUFFER_BIT());
        if(this.state != State.Normal)
        {
            Main.opengl.glMatrixMode(Main.opengl.GL_MODELVIEW());
            Main.opengl.glLoadIdentity();
            Image.unselectTexture();
            Main.opengl.glColor4f(0.5f, 0.5f, 0.5f, 0.125f);
            Main.opengl.glBegin(Main.opengl.GL_TRIANGLES());
            Main.opengl.glVertex3f(-Main.aspectRatio(), -1, -1);
            Main.opengl.glVertex3f(Main.aspectRatio(), -1, -1);
            Main.opengl.glVertex3f(Main.aspectRatio(), 1, -1);
            Main.opengl.glVertex3f(Main.aspectRatio(), 1, -1);
            Main.opengl.glVertex3f(-Main.aspectRatio(), 1, -1);
            Main.opengl.glVertex3f(-Main.aspectRatio(), -1, -1);
            Main.opengl.glEnd();
            Main.opengl.glClear(Main.opengl.GL_DEPTH_BUFFER_BIT());
        }
        switch(this.state)
        {
        case Normal:
        {
            break;
        }
        case Workbench:
        {
            Main.opengl.glMatrixMode(Main.opengl.GL_MODELVIEW());
            Main.opengl.glLoadIdentity();
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
            Main.opengl.glColor3f(1, 1, 1);
            Main.opengl.glBegin(Main.opengl.GL_TRIANGLES());
            Main.opengl.glTexCoord2f(0, 0);
            glVertex(imgMat.apply(drawAll_t3, Vector.ZERO));
            Main.opengl.glTexCoord2f(maxU, 0);
            glVertex(imgMat.apply(drawAll_t3,
                                  Vector.set(drawAll_t3, dialogW, 0, 0)));
            Main.opengl.glTexCoord2f(maxU, maxV);
            glVertex(imgMat.apply(drawAll_t3,
                                  Vector.set(drawAll_t3, dialogW, dialogH, 0)));
            Main.opengl.glTexCoord2f(maxU, maxV);
            glVertex(imgMat.apply(drawAll_t3,
                                  Vector.set(drawAll_t3, dialogW, dialogH, 0)));
            Main.opengl.glTexCoord2f(0, maxV);
            glVertex(imgMat.apply(drawAll_t3,
                                  Vector.set(drawAll_t3, 0, dialogH, 0)));
            Main.opengl.glTexCoord2f(0, 0);
            glVertex(imgMat.apply(drawAll_t3, Vector.ZERO));
            Main.opengl.glEnd();
            Main.opengl.glClear(Main.opengl.GL_DEPTH_BUFFER_BIT());
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
            Main.opengl.glMatrixMode(Main.opengl.GL_MODELVIEW());
            Main.opengl.glLoadIdentity();
            chestImg.selectTexture();
            final float maxU = (float)dialogW / dialogTextureSize, maxV = (float)dialogH
                    / dialogTextureSize;
            Matrix imgMat = getImageMat();
            Main.opengl.glColor3f(1, 1, 1);
            Main.opengl.glBegin(Main.opengl.GL_TRIANGLES());
            Main.opengl.glTexCoord2f(0, 0);
            glVertex(imgMat.apply(drawAll_t3, Vector.ZERO));
            Main.opengl.glTexCoord2f(maxU, 0);
            glVertex(imgMat.apply(drawAll_t3,
                                  Vector.set(drawAll_t3, dialogW, 0, 0)));
            Main.opengl.glTexCoord2f(maxU, maxV);
            glVertex(imgMat.apply(drawAll_t3,
                                  Vector.set(drawAll_t3, dialogW, dialogH, 0)));
            Main.opengl.glTexCoord2f(maxU, maxV);
            glVertex(imgMat.apply(drawAll_t3,
                                  Vector.set(drawAll_t3, dialogW, dialogH, 0)));
            Main.opengl.glTexCoord2f(0, maxV);
            glVertex(imgMat.apply(drawAll_t3,
                                  Vector.set(drawAll_t3, 0, dialogH, 0)));
            Main.opengl.glTexCoord2f(0, 0);
            glVertex(imgMat.apply(drawAll_t3, Vector.ZERO));
            Main.opengl.glEnd();
            Main.opengl.glClear(Main.opengl.GL_DEPTH_BUFFER_BIT());
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
            Main.opengl.glMatrixMode(Main.opengl.GL_MODELVIEW());
            Main.opengl.glLoadIdentity();
            furnaceImg.selectTexture();
            final float maxU = (float)dialogW / dialogTextureSize, maxV = (float)dialogH
                    / dialogTextureSize;
            Matrix imgMat = getImageMat();
            Main.opengl.glColor3f(1, 1, 1);
            Main.opengl.glBegin(Main.opengl.GL_TRIANGLES());
            Main.opengl.glTexCoord2f(0, 0);
            glVertex(imgMat.apply(drawAll_t3, Vector.ZERO));
            Main.opengl.glTexCoord2f(maxU, 0);
            glVertex(imgMat.apply(drawAll_t3,
                                  Vector.set(drawAll_t3, dialogW, 0, 0)));
            Main.opengl.glTexCoord2f(maxU, maxV);
            glVertex(imgMat.apply(drawAll_t3,
                                  Vector.set(drawAll_t3, dialogW, dialogH, 0)));
            Main.opengl.glTexCoord2f(maxU, maxV);
            glVertex(imgMat.apply(drawAll_t3,
                                  Vector.set(drawAll_t3, dialogW, dialogH, 0)));
            Main.opengl.glTexCoord2f(0, maxV);
            glVertex(imgMat.apply(drawAll_t3,
                                  Vector.set(drawAll_t3, 0, dialogH, 0)));
            Main.opengl.glTexCoord2f(0, 0);
            glVertex(imgMat.apply(drawAll_t3, Vector.ZERO));
            Main.opengl.glEnd();
            Main.opengl.glClear(Main.opengl.GL_DEPTH_BUFFER_BIT());
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
        case DispenserDropper:
        {
            Main.opengl.glMatrixMode(Main.opengl.GL_MODELVIEW());
            Main.opengl.glLoadIdentity();
            dispenserDropperImg.selectTexture();
            final float maxU = (float)dialogW / dialogTextureSize, maxV = (float)dialogH
                    / dialogTextureSize;
            Matrix imgMat = getImageMat();
            Main.opengl.glColor3f(1, 1, 1);
            Main.opengl.glBegin(Main.opengl.GL_TRIANGLES());
            Main.opengl.glTexCoord2f(0, 0);
            glVertex(imgMat.apply(drawAll_t3, Vector.ZERO));
            Main.opengl.glTexCoord2f(maxU, 0);
            glVertex(imgMat.apply(drawAll_t3,
                                  Vector.set(drawAll_t3, dialogW, 0, 0)));
            Main.opengl.glTexCoord2f(maxU, maxV);
            glVertex(imgMat.apply(drawAll_t3,
                                  Vector.set(drawAll_t3, dialogW, dialogH, 0)));
            Main.opengl.glTexCoord2f(maxU, maxV);
            glVertex(imgMat.apply(drawAll_t3,
                                  Vector.set(drawAll_t3, dialogW, dialogH, 0)));
            Main.opengl.glTexCoord2f(0, maxV);
            glVertex(imgMat.apply(drawAll_t3,
                                  Vector.set(drawAll_t3, 0, dialogH, 0)));
            Main.opengl.glTexCoord2f(0, 0);
            glVertex(imgMat.apply(drawAll_t3, Vector.ZERO));
            Main.opengl.glEnd();
            Main.opengl.glClear(Main.opengl.GL_DEPTH_BUFFER_BIT());
            drawInventory();
            Block dispenserDropper = world.getBlock(this.blockX,
                                                    this.blockY,
                                                    this.blockZ);
            if(dispenserDropper == null
                    || (dispenserDropper.getType() != BlockType.BTDispenser && dispenserDropper.getType() != BlockType.BTDropper))
                dispenserDropper = Block.NewDropper(-1);
            for(int row = 0; row < Block.DISPENSER_DROPPER_ROWS; row++)
            {
                for(int column = 0; column < Block.DISPENSER_DROPPER_COLUMNS; column++)
                {
                    int count = dispenserDropper.dispenserDropperGetBlockCount(row,
                                                                               column);
                    Block b = dispenserDropper.dispenserDropperGetBlockType(row,
                                                                            column);
                    drawCell(b,
                             count,
                             dispenserDropperLeft + column * cellSize,
                             dispenserDropperBottom + row * cellSize,
                             false);
                }
            }
            break;
        }
        case Hopper:
        {
            Main.opengl.glMatrixMode(Main.opengl.GL_MODELVIEW());
            Main.opengl.glLoadIdentity();
            hopperImg.selectTexture();
            final float maxU = (float)dialogW / dialogTextureSize, maxV = (float)dialogH
                    / dialogTextureSize;
            Matrix imgMat = getImageMat();
            Main.opengl.glColor3f(1, 1, 1);
            Main.opengl.glBegin(Main.opengl.GL_TRIANGLES());
            Main.opengl.glTexCoord2f(0, 0);
            glVertex(imgMat.apply(drawAll_t3, Vector.ZERO));
            Main.opengl.glTexCoord2f(maxU, 0);
            glVertex(imgMat.apply(drawAll_t3,
                                  Vector.set(drawAll_t3, dialogW, 0, 0)));
            Main.opengl.glTexCoord2f(maxU, maxV);
            glVertex(imgMat.apply(drawAll_t3,
                                  Vector.set(drawAll_t3, dialogW, dialogH, 0)));
            Main.opengl.glTexCoord2f(maxU, maxV);
            glVertex(imgMat.apply(drawAll_t3,
                                  Vector.set(drawAll_t3, dialogW, dialogH, 0)));
            Main.opengl.glTexCoord2f(0, maxV);
            glVertex(imgMat.apply(drawAll_t3,
                                  Vector.set(drawAll_t3, 0, dialogH, 0)));
            Main.opengl.glTexCoord2f(0, 0);
            glVertex(imgMat.apply(drawAll_t3, Vector.ZERO));
            Main.opengl.glEnd();
            Main.opengl.glClear(Main.opengl.GL_DEPTH_BUFFER_BIT());
            drawInventory();
            Block hopper = world.getBlock(this.blockX, this.blockY, this.blockZ);
            if(hopper == null || hopper.getType() != BlockType.BTHopper)
                hopper = Block.NewHopper(-1);
            for(int slot = 0; slot < Block.HOPPER_SLOTS; slot++)
            {
                int count = hopper.hopperGetBlockCount(slot);
                Block b = hopper.hopperGetBlockType(slot);
                drawCell(b,
                         count,
                         hopperLeft + slot * cellSize,
                         hopperBottom,
                         false);
            }
            break;
        }
        }
        Main.opengl.glClear(Main.opengl.GL_DEPTH_BUFFER_BIT());
        if(this.state != State.Normal)
        {
            drawCell(this.dragType,
                     this.dragCount,
                     this.dragX - cellSize / 2f,
                     this.dragY - cellSize / 2f,
                     false);
        }
        if(Main.platform.isTouchScreen())
        {
            if(this.state == State.Normal)
            {
                drawButton("Place",
                           getPlaceButtonLeft(),
                           getPlaceButtonBottom(),
                           false);
                drawButton("Inven\ntory",
                           getInventoryButtonLeft(),
                           getInventoryButtonBottom(),
                           false);
                drawButton("\u25C4",
                           getMoveLeftButtonLeft(),
                           getMoveLeftButtonBottom(),
                           false);
                drawButton("\u25BA",
                           getMoveRightButtonLeft(),
                           getMoveRightButtonBottom(),
                           false);
                drawButton("\u25B2",
                           getMoveUpButtonLeft(),
                           getMoveUpButtonBottom(),
                           false);
                drawButton("\u25BC",
                           getMoveDownButtonLeft(),
                           getMoveDownButtonBottom(),
                           false);
                drawButton("Jump",
                           getJumpButtonLeft(),
                           getJumpButtonBottom(),
                           false);
                if(Main.isCreativeMode)
                {
                    drawButton("Fly",
                               getFlyButtonLeft(),
                               getFlyButtonBottom(),
                               this.isFlying);
                }
                else
                    this.isFlying = false;
                drawButton("Sneak",
                           getSneakButtonLeft(),
                           getSneakButtonBottom(),
                           this.isSneaking);
            }
            else
            {
                drawButton("Close",
                           getReturnButtonLeft(),
                           getReturnButtonBottom(),
                           false);
            }
            drawButton("Pause",
                       getPauseButtonLeft(),
                       getPauseButtonBottom(),
                       false);
        }
    }

    @Override
    public RenderingStream draw(final RenderingStream rs,
                                final Matrix worldToCamera)
    {
        // TODO Auto-generated method stub
        return rs;
    }

    /** set this player's position
     * 
     * @param pos
     *            the new position */
    public void setPosition(final Vector pos)
    {
        this.position.set(pos);
        this.velocity.set(Vector.ZERO);
    }

    private boolean mousePosToSelPosWorkbench(final float mouseX,
                                              final float mouseY)
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

    private float mouseGetSimX(final float mouseX)
    {
        float x = mouseX / Main.ScreenXRes() * 2.0f - 1.0f;
        x *= Main.aspectRatio();
        x += dialogW / simScreenHeight;
        x /= 2f / simScreenHeight;
        return x;
    }

    private float mouseGetSimY(final float mouseY)
    {
        float y = 1.0f - mouseY / Main.ScreenYRes() * 2.0f;
        y += dialogH / simScreenHeight;
        y /= 2f / simScreenHeight;
        return y;
    }

    private boolean mouseIsInResultWorkbench(final float mouseX,
                                             final float mouseY)
    {
        float x = mouseGetSimX(mouseX);
        float y = mouseGetSimY(mouseY);
        x -= getWorkbenchResultLeft();
        y -= getWorkbenchResultBottom();
        x += cellBorder;
        y += cellBorder;
        if(x < 0 || x >= cellSize || y < 0 || y >= cellSize)
            return false;
        return true;
    }

    private boolean mouseIsInResultFurnace(final float mouseX,
                                           final float mouseY)
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

    private boolean
        mouseIsInFireFurnace(final float mouseX, final float mouseY)
    {
        float x = mouseGetSimX(mouseX);
        float y = mouseGetSimY(mouseY);
        x -= furnaceFireXCenter;
        y -= furnaceFireBottom;
        if(x < -10 || x > 10 || y < 0 || y > 10)
            return false;
        return true;
    }

    private boolean mouseIsInSourceFurnace(final float mouseX,
                                           final float mouseY)
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

    private boolean mouseIsInChest(final float mouseX, final float mouseY)
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

    private int mouseGetChestRow(final float mouseX, final float mouseY)
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

    private int mouseGetChestColumn(final float mouseX, final float mouseY)
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

    private boolean mouseIsInDispenserDropper(final float mouseX,
                                              final float mouseY)
    {
        float x = mouseGetSimX(mouseX);
        float y = mouseGetSimY(mouseY);
        x -= dispenserDropperLeft;
        y -= dispenserDropperBottom;
        x += cellBorder;
        y += cellBorder;
        if(x >= 0 && x < cellSize * Block.DISPENSER_DROPPER_COLUMNS && y >= 0
                && y < cellSize * Block.DISPENSER_DROPPER_ROWS)
            return true;
        return false;
    }

    private int mouseGetDispenserDropperRow(final float mouseX,
                                            final float mouseY)
    {
        float x = mouseGetSimX(mouseX);
        float y = mouseGetSimY(mouseY);
        x -= dispenserDropperLeft;
        y -= dispenserDropperBottom;
        x += cellBorder;
        y += cellBorder;
        if(x >= 0 && x < cellSize * Block.DISPENSER_DROPPER_COLUMNS && y >= 0
                && y < cellSize * Block.DISPENSER_DROPPER_ROWS)
            return (int)Math.floor(y / cellSize);
        return -1;
    }

    private int mouseGetDispenserDropperColumn(final float mouseX,
                                               final float mouseY)
    {
        float x = mouseGetSimX(mouseX);
        float y = mouseGetSimY(mouseY);
        x -= dispenserDropperLeft;
        y -= dispenserDropperBottom;
        x += cellBorder;
        y += cellBorder;
        if(x >= 0 && x < cellSize * Block.DISPENSER_DROPPER_COLUMNS && y >= 0
                && y < cellSize * Block.DISPENSER_DROPPER_ROWS)
            return (int)Math.floor(x / cellSize);
        return -1;
    }

    private boolean mouseIsInHopper(final float mouseX, final float mouseY)
    {
        float x = mouseGetSimX(mouseX);
        float y = mouseGetSimY(mouseY);
        x -= hopperLeft;
        y -= hopperBottom;
        x += cellBorder;
        y += cellBorder;
        if(x >= 0 && x < cellSize * Block.HOPPER_SLOTS && y >= 0
                && y < cellSize)
            return true;
        return false;
    }

    private int mouseGetHopperSlot(final float mouseX, final float mouseY)
    {
        float x = mouseGetSimX(mouseX);
        float y = mouseGetSimY(mouseY);
        x -= hopperLeft;
        y -= hopperBottom;
        x += cellBorder;
        y += cellBorder;
        if(x >= 0 && x < cellSize * Block.HOPPER_SLOTS && y >= 0
                && y < cellSize)
            return (int)Math.floor(x / cellSize);
        return -1;
    }

    private boolean mouseIsInCreative(final float mouseX, final float mouseY)
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

    private int mouseGetCreativeY(final float mouseX, final float mouseY)
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

    private int mouseGetCreativeX(final float mouseX, final float mouseY)
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

    private boolean mouseIsInCreativeUpButton(final float mouseX,
                                              final float mouseY)
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

    private boolean mouseIsInCreativeDownButton(final float mouseX,
                                                final float mouseY)
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

    private boolean mouseIsInInventoryOrHotbar(final float mouseX,
                                               final float mouseY)
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

    private int mouseGetInventoryOrHotbarRow(final float mouseX,
                                             final float mouseY)
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

    private int mouseGetInventoryOrHotbarColumn(final float mouseX,
                                                final float mouseY)
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

    private boolean mouseIsInHotbar(final float mouseX, final float mouseY)
    {
        float x = mouseX / Main.ScreenXRes() * 2.0f - 1.0f;
        x *= Main.aspectRatio();
        float y = 1.0f - mouseY / Main.ScreenYRes() * 2.0f;
        for(int i = 0; i < Block.CHEST_COLUMNS; i++)
        {
            if(x >= getHotbarLeft(i) && x <= getHotbarRight(i)
                    && y >= getHotbarBottom(i) && y <= getHotbarTop(i))
                return true;
        }
        return false;
    }

    private int mouseGetHotbarIndex(final float mouseX, final float mouseY)
    {
        float x = mouseX / Main.ScreenXRes() * 2.0f - 1.0f;
        x *= Main.aspectRatio();
        float y = 1.0f - mouseY / Main.ScreenYRes() * 2.0f;
        for(int i = 0; i < Block.CHEST_COLUMNS; i++)
        {
            if(x >= getHotbarLeft(i) && x <= getHotbarRight(i)
                    && y >= getHotbarBottom(i) && y <= getHotbarTop(i))
                return i;
        }
        return -1;
    }

    private boolean mouseIsInButton(final float mouseX,
                                    final float mouseY,
                                    final float left,
                                    final float bottom)
    {
        float x = mouseX / Main.ScreenXRes() * 2.0f - 1.0f;
        x *= Main.aspectRatio();
        float y = 1.0f - mouseY / Main.ScreenYRes() * 2.0f;
        if(x >= left && x <= left + touchButtonWidth && y >= bottom
                && y <= bottom + touchButtonHeight)
            return true;
        return false;
    }

    private boolean
        mouseIsInPlaceButton(final float mouseX, final float mouseY)
    {
        return mouseIsInButton(mouseX,
                               mouseY,
                               getPlaceButtonLeft(),
                               getPlaceButtonBottom());
    }

    private boolean
        mouseIsInReturnButton(final float mouseX, final float mouseY)
    {
        return mouseIsInButton(mouseX,
                               mouseY,
                               getReturnButtonLeft(),
                               getReturnButtonBottom());
    }

    private boolean
        mouseIsInPauseButton(final float mouseX, final float mouseY)
    {
        return mouseIsInButton(mouseX,
                               mouseY,
                               getPauseButtonLeft(),
                               getPauseButtonBottom());
    }

    private boolean mouseIsInInventoryButton(final float mouseX,
                                             final float mouseY)
    {
        return mouseIsInButton(mouseX,
                               mouseY,
                               getInventoryButtonLeft(),
                               getInventoryButtonBottom());
    }

    private boolean mouseIsInJumpButton(final float mouseX, final float mouseY)
    {
        return mouseIsInButton(mouseX,
                               mouseY,
                               getJumpButtonLeft(),
                               getJumpButtonBottom());
    }

    private boolean mouseIsInFlyButton(final float mouseX, final float mouseY)
    {
        return mouseIsInButton(mouseX,
                               mouseY,
                               getFlyButtonLeft(),
                               getFlyButtonBottom());
    }

    private boolean mouseIsInMoveDownButton(final float mouseX,
                                            final float mouseY)
    {
        return mouseIsInButton(mouseX,
                               mouseY,
                               getMoveDownButtonLeft(),
                               getMoveDownButtonBottom());
    }

    private boolean
        mouseIsInMoveUpButton(final float mouseX, final float mouseY)
    {
        return mouseIsInButton(mouseX,
                               mouseY,
                               getMoveUpButtonLeft(),
                               getMoveUpButtonBottom());
    }

    private boolean mouseIsInMoveLeftButton(final float mouseX,
                                            final float mouseY)
    {
        return mouseIsInButton(mouseX,
                               mouseY,
                               getMoveLeftButtonLeft(),
                               getMoveLeftButtonBottom());
    }

    private boolean mouseIsInMoveRightButton(final float mouseX,
                                             final float mouseY)
    {
        return mouseIsInButton(mouseX,
                               mouseY,
                               getMoveRightButtonLeft(),
                               getMoveRightButtonBottom());
    }

    private boolean isMoveRightPressed()
    {
        if(this.state != State.Normal)
            return false;
        if(this.isDragOperation)
            return false;
        if(!Main.mouse.isButtonDown(Mouse.BUTTON_LEFT))
            return false;
        return mouseIsInMoveRightButton(Main.mouse.getX(), Main.mouse.getY());
    }

    private boolean isMoveUpPressed()
    {
        if(this.state != State.Normal)
            return false;
        if(this.isDragOperation)
            return false;
        if(!Main.mouse.isButtonDown(Mouse.BUTTON_LEFT))
            return false;
        return mouseIsInMoveUpButton(Main.mouse.getX(), Main.mouse.getY());
    }

    private boolean isMoveLeftPressed()
    {
        if(this.state != State.Normal)
            return false;
        if(this.isDragOperation)
            return false;
        if(!Main.mouse.isButtonDown(Mouse.BUTTON_LEFT))
            return false;
        return mouseIsInMoveLeftButton(Main.mouse.getX(), Main.mouse.getY());
    }

    private boolean isMoveDownPressed()
    {
        if(this.state != State.Normal)
            return false;
        if(this.isDragOperation)
            return false;
        if(!Main.mouse.isButtonDown(Mouse.BUTTON_LEFT))
            return false;
        return mouseIsInMoveDownButton(Main.mouse.getX(), Main.mouse.getY());
    }

    private boolean isJumpPressed()
    {
        if(this.state != State.Normal)
            return false;
        if(this.isDragOperation)
            return false;
        if(!Main.mouse.isButtonDown(Mouse.BUTTON_LEFT))
            return false;
        return mouseIsInJumpButton(Main.mouse.getX(), Main.mouse.getY());
    }

    private boolean
        mouseIsInSneakButton(final float mouseX, final float mouseY)
    {
        return mouseIsInButton(mouseX,
                               mouseY,
                               getSneakButtonLeft(),
                               getSneakButtonBottom());
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
    public MouseMoveKind handleMouseMove(final float mouseX,
                                         final float mouseY,
                                         final boolean mouseLButton)
    {
        if(Main.DEBUG && Main.platform.isTouchScreen())
        {
            float x = mouseX / Main.ScreenXRes() * 2.0f - 1.0f;
            x *= Main.aspectRatio();
            float y = 1.0f - mouseY / Main.ScreenYRes() * 2.0f;
            Main.addToFrameText("X : " + Float.toString(x) + " Y : "
                    + Float.toString(y) + "\n");
        }
        switch(this.state)
        {
        case Normal:
        {
            if(Main.platform.isTouchScreen())
            {
                if(mouseLButton)
                {
                    if(Math.hypot(mouseX - this.startMouseX, mouseY
                            - this.startMouseY) > Main.mouse.getDragThreshold())
                    {
                        this.isDragOperation = true;
                    }
                }
            }
            if(this.wasPaused && !Main.platform.isTouchScreen())
            {
                this.wasPaused = false;
                this.deleteAnimTime = -1;
                return MouseMoveKind.GrabbedAndCentered;
            }
            this.wasPaused = false;
            if(this.isDragOperation && Main.platform.isTouchScreen())
            {
                this.viewPhi -= (mouseY - this.lastMouseY) / 100.0;
                if(this.viewPhi < -Math.PI / 2)
                    this.viewPhi = -(float)Math.PI / 2;
                else if(this.viewPhi > Math.PI / 2)
                    this.viewPhi = (float)Math.PI / 2;
                this.viewTheta -= (mouseX - this.lastMouseX) / 100.0;
                this.viewTheta %= (float)(Math.PI * 2);
                this.lastMouseX = mouseX;
                this.lastMouseY = mouseY;
                this.mouseDownTime = 0;
                this.deleteAnimTime = -1;
            }
            else if(!Main.platform.isTouchScreen())
            {
                this.viewPhi += (mouseY - (Main.ScreenYRes() / 2)) / 100.0;
                if(this.viewPhi < -Math.PI / 2)
                    this.viewPhi = -(float)Math.PI / 2;
                else if(this.viewPhi > Math.PI / 2)
                    this.viewPhi = (float)Math.PI / 2;
                this.viewTheta += (mouseX - (Main.ScreenXRes() / 2)) / 100.0;
                this.viewTheta %= (float)(Math.PI * 2);
            }
            if(mouseLButton
                    && ((!this.isDragOperation
                            && !isMoveUpPressed()
                            && !isMoveDownPressed()
                            && !isMoveLeftPressed()
                            && !isMoveRightPressed()
                            && !isJumpPressed()
                            && (!mouseIsInFlyButton(mouseX, mouseY) || !Main.isCreativeMode)
                            && !mouseIsInSneakButton(mouseX, mouseY)
                            && !mouseIsInHotbar(mouseX, mouseY)
                            && !mouseIsInPauseButton(mouseX, mouseY) && !mouseIsInPlaceButton(mouseX,
                                                                                              mouseY)) || !Main.platform.isTouchScreen()))
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
                Block curHotbar = getCurrentHotbarBlock();
                BlockType.ToolType toolType = BlockType.ToolType.None;
                BlockType.ToolLevel toolLevel = BlockType.ToolLevel.Nothing;
                if(curHotbar != null && b != null)
                {
                    toolType = curHotbar.getToolType();
                    toolLevel = curHotbar.getToolLevel();
                }
                Block.BlockDigDescriptor bdd = null;
                if(b != null)
                    bdd = b.getDigDescriptor(toolType, toolLevel);
                float deletePeriod = -1;
                if(Main.isCreativeMode)
                {
                    bdd = null;
                    toolType = ToolType.None;
                    toolLevel = ToolLevel.Nothing;
                    if(b != null)
                    {
                        if(b.canDig())
                            bdd = new Block.BlockDigDescriptor(0.25f,
                                                               false,
                                                               false);
                    }
                }
                if(bdd != null)
                    deletePeriod = bdd.digTime;
                if(deletePeriod < 0.25)
                    deletePeriod = 0.25f;
                if(Main.platform.isTouchScreen() && this.touchWaitTime > 0)
                {
                    this.touchWaitTime -= Main.getFrameDuration();
                }
                else
                    this.mouseDownTime += Main.getFrameDuration();
                if(b == null || !b.canDig() || !isSameBlock || bdd == null)
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
                    world.setBlock(this.blockX,
                                   this.blockY,
                                   this.blockZ,
                                   new Block());
                    b.digBlock(this.blockX,
                               this.blockY,
                               this.blockZ,
                               bdd.makesBlock,
                               toolType);
                    Main.play(Main.destructAudio);
                    if(bdd.usesTool && toolType != BlockType.ToolType.None
                            && toolType != BlockType.ToolType.Hoe)
                    {
                        curHotbar = takeBlock();
                        curHotbar = new Block(curHotbar);
                        if(curHotbar.toolUseTool())
                            if(!giveBlock(curHotbar, true))
                                dropBlock(curHotbar);
                    }
                }
            }
            if(mouseLButton && mouseIsInHotbar(mouseX, mouseY))
            {
                float oldTime = this.mouseDownTime;
                this.mouseDownTime += Main.getFrameDuration();
                float newTime = this.mouseDownTime;
                if(newTime >= 0.5f)
                {
                    oldTime = Math.max(0.5f, oldTime);
                    int throwCount = (int)(Math.ceil(newTime * 3) - Math.ceil(oldTime * 3));
                    for(int i = 0; i < throwCount; i++)
                    {
                        throwBlock(takeBlock());
                    }
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
        case DispenserDropper:
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
        case Hopper:
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
    private int giveBlock(final Block b,
                          final int count,
                          final int row,
                          final int column)
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

    private boolean internalGiveBlock(final Block b,
                                      final boolean setCurrentBlock)
    {
        if(b == null || b.getType() == BlockType.BTEmpty)
            return true;
        if(giveBlock(b, 1, Block.CHEST_ROWS, this.selectionX) > 0)
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

    /** give this player a block
     * 
     * @param b
     *            the block to give
     * @param setCurrentBlock
     *            if <code>b</code> should be set as the players currently
     *            selected block
     * @return if the block can be given to this player */
    public boolean giveBlock(final Block b, final boolean setCurrentBlock)
    {
        if(Main.isCreativeMode)
            return true;
        return internalGiveBlock(b, setCurrentBlock);
    }

    /** @return the block taken from this player or <code>null</code> */
    public Block takeBlock()
    {
        int index = getInventoryIndex(Block.CHEST_ROWS, this.selectionX);
        if(this.blockCount[index] <= 0)
            return null;
        Block retval = this.blockType[index];
        if(Main.isCreativeMode)
            return retval;
        if(--this.blockCount[index] <= 0)
            this.blockType[index] = null;
        return retval;
    }

    private Block takeBlock(final Block type)
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
                if(!retval.equals(type))
                    continue;
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

    private static Vector canPlaceBlock_t1 = Vector.allocate();

    private boolean canPlaceBlock(final Block selb,
                                  final int bx,
                                  final int by,
                                  final int bz)
    {
        if(selb == null || this.state != State.Normal
                || getCurrentHotbarBlock() == null
                || getCurrentHotbarBlock().isItem())
            return false;
        if(Math.floor(this.position.getX()) == bx
                && Math.floor(this.position.getY()) == by
                && Math.floor(this.position.getZ()) == bz
                && !getCurrentHotbarBlock().isPlaceableWhileInside())
            return false;
        if(!selb.isReplaceable())
            return false;
        Vector relpos = Vector.sub(canPlaceBlock_t1, this.position, bx, by, bz);
        if(selb.adjustPlayerPosition(relpos, distLimit) == null)
            return false;
        return true;
    }

    private static Vector internalSetPositionH_newpos = Vector.allocate();
    private static Vector internalSetPositionH_t1 = Vector.allocate();
    private static Vector internalSetPositionH_bpos = Vector.allocate();
    private static Vector internalSetPositionH_relpos = Vector.allocate();

    private void internalSetPositionH(final Vector pos)
    {
        Vector newpos = Vector.set(internalSetPositionH_newpos, pos);
        int x = (int)Math.floor(newpos.getX()), y = (int)Math.floor(newpos.getY()), z = (int)Math.floor(newpos.getZ());
        {
            Block b1 = world.getBlockEval(x, y, z);
            Block b2 = world.getBlockEval(x, y - 1, z);
            boolean setPosition = false;
            while(b1 != null
                    && b2 != null
                    && (b1.adjustPlayerPosition(Vector.sub(internalSetPositionH_t1,
                                                           newpos,
                                                           x,
                                                           y,
                                                           z),
                                                distLimit) == null || b2.adjustPlayerPosition(Vector.sub(internalSetPositionH_t1,
                                                                                                         newpos,
                                                                                                         x,
                                                                                                         y - 1,
                                                                                                         z),
                                                                                              distLimit) == null))
            {
                y++;
                newpos.setY(y + distLimit + 1e-3f - (PlayerHeight - 1.0f)
                        + b2.getHeight());
                b1 = world.getBlockEval(x, y, z);
                b2 = world.getBlockEval(x, y - 1, z);
                setPosition = true;
            }
            if(setPosition)
            {
                this.position.set(newpos);
                this.velocity.set(Vector.ZERO);
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
                        Vector bpos = Vector.set(internalSetPositionH_bpos, x
                                + dx, y + dy - i, z + dz);
                        Vector relpos = Vector.sub(internalSetPositionH_relpos,
                                                   newpos,
                                                   bpos);
                        relpos = b.adjustPlayerPosition(relpos, distLimit);
                        if(relpos == null)
                            return;
                        Vector.add(newpos, relpos, bpos);
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
                        Vector bpos = Vector.set(internalSetPositionH_bpos, x
                                + dx, y + dy - i, z + dz);
                        Vector relpos = Vector.sub(internalSetPositionH_relpos,
                                                   newpos,
                                                   bpos);
                        relpos = b.adjustPlayerPosition(relpos, distLimit);
                        if(relpos == null)
                            return;
                        Vector.add(newpos, relpos, bpos);
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
                        Vector bpos = Vector.set(internalSetPositionH_bpos, x
                                + dx, y + dy - i, z + dz);
                        Vector relpos = Vector.sub(internalSetPositionH_relpos,
                                                   newpos,
                                                   bpos);
                        relpos = b.adjustPlayerPosition(relpos, distLimit);
                        if(relpos == null)
                            return;
                        Vector.add(newpos, relpos, bpos);
                    }
                }
            }
        }
        this.position.set(newpos);
    }

    private static Vector internalSetPosition_deltapos = Vector.allocate();
    private static Vector internalSetPosition_t1 = Vector.allocate();

    private void internalSetPosition(final Vector pos)
    {
        Vector deltapos = Vector.sub(internalSetPosition_deltapos,
                                     pos,
                                     this.position);
        final int count = 100;
        deltapos = deltapos.mulAndSet(1.0f / count);
        for(int i = 0; i < count; i++)
        {
            internalSetPositionH(Vector.add(internalSetPosition_t1,
                                            this.position,
                                            deltapos));
        }
    }

    private boolean handleInventoryOrHotbarClick(final Main.MouseEvent event)
    {
        if(!event.isDown && Main.platform.isTouchScreen()
                && mouseIsInReturnButton(event.mouseX, event.mouseY))
        {
            quitToNormal();
            return true;
        }
        if(!event.isDown && mouseIsInPauseButton(event.mouseX, event.mouseY))
        {
            Main.needPause = true;
            quitToNormal();
            return true;
        }
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

    private static Vector throwBlock_t1 = Vector.allocate();

    private void throwBlock(final Block b)
    {
        if(b == null || b.getType() == BlockType.BTEmpty)
            return;
        world.insertEntity(Entity.NewThrownBlock(this.position,
                                                 b,
                                                 Vector.mul(throwBlock_t1,
                                                            getForwardVector(),
                                                            8.0f)));
    }

    private void dropBlock(final Block b)
    {
        if(b == null || b.getType() == BlockType.BTEmpty)
            return;
        world.insertEntity(Entity.NewBlock(this.position, b, Vector.ZERO));
    }

    private Block getContainer()
    {
        Block c = world.getBlock(this.blockX, this.blockY, this.blockZ);
        if(c == null)
        {
            quitToNormal();
            return null;
        }
        switch(this.state)
        {
        case Chest:
            if(c.getType() == BlockType.BTChest)
                return c;
            break;
        case DispenserDropper:
            if(c.getType() == BlockType.BTDispenser)
                return c;
            if(c.getType() == BlockType.BTDropper)
                return c;
            break;
        case Furnace:
            break;
        case Hopper:
            if(c.getType() == BlockType.BTHopper)
                return c;
            break;
        case Normal:
            break;
        case Workbench:
            break;
        }
        quitToNormal();
        return null;
    }

    private Block getSelectedBlockType(final Block container,
                                       final int row,
                                       final int column)
    {
        Block b = container;
        if(b == null)
            return null;
        switch(this.state)
        {
        case Normal:
            return null;
        case Workbench:
            return null;
        case Furnace:
            return null;
        case Chest:
            if(b.getType() != BlockType.BTChest)
                return null;
            return b.chestGetBlockType(row, column);
        case DispenserDropper:
            if(b.getType() != BlockType.BTDispenser
                    && b.getType() != BlockType.BTDropper)
                return null;
            return b.dispenserDropperGetBlockType(row, column);
        case Hopper:
            if(b.getType() != BlockType.BTHopper)
                return null;
            return b.hopperGetBlockType(row);
        }
        return null;
    }

    private int getSelectedBlockCount(final Block container,
                                      final int row,
                                      final int column)
    {
        Block b = container;
        if(b == null)
            return 0;
        switch(this.state)
        {
        case Normal:
            return 0;
        case Workbench:
            return 0;
        case Furnace:
            return 0;
        case Chest:
            if(b.getType() != BlockType.BTChest)
                return 0;
            return b.chestGetBlockCount(row, column);
        case DispenserDropper:
            if(b.getType() != BlockType.BTDispenser
                    && b.getType() != BlockType.BTDropper)
                return 0;
            return b.dispenserDropperGetBlockCount(row, column);
        case Hopper:
            if(b.getType() != BlockType.BTHopper)
                return 0;
            return b.hopperGetBlockCount(row);
        }
        return 0;
    }

    private int addToSelectedBlock(final Block container,
                                   final int row,
                                   final int column,
                                   final Block blockToAdd,
                                   final int count)
    {
        Block b = container;
        if(b == null)
            return 0;
        switch(this.state)
        {
        case Normal:
            return 0;
        case Workbench:
            return 0;
        case Furnace:
            return 0;
        case Chest:
            if(b.getType() != BlockType.BTChest)
                return 0;
            return b.chestAddBlocks(blockToAdd, count, row, column);
        case DispenserDropper:
            if(b.getType() != BlockType.BTDispenser
                    && b.getType() != BlockType.BTDropper)
                return 0;
            return b.dispenserDropperAddBlocks(blockToAdd, count, row, column);
        case Hopper:
            if(b.getType() != BlockType.BTHopper)
                return 0;
            return b.hopperAddBlocks(blockToAdd, count, row);
        }
        return 0;
    }

    private int removeFromSelectedBlock(final Block container,
                                        final int row,
                                        final int column,
                                        final Block blockToRemove,
                                        final int count)
    {
        Block b = container;
        if(b == null)
            return 0;
        switch(this.state)
        {
        case Normal:
            return 0;
        case Workbench:
            return 0;
        case Furnace:
            return 0;
        case Chest:
            if(b.getType() != BlockType.BTChest)
                return 0;
            return b.chestRemoveBlocks(blockToRemove, count, row, column);
        case DispenserDropper:
            if(b.getType() != BlockType.BTDispenser
                    && b.getType() != BlockType.BTDropper)
                return 0;
            return b.dispenserDropperRemoveBlocks(blockToRemove,
                                                  count,
                                                  row,
                                                  column);
        case Hopper:
            if(b.getType() != BlockType.BTHopper)
                return 0;
            return b.hopperRemoveBlocks(blockToRemove, count, row);
        }
        return 0;
    }

    private void handleMouseDownOnContainer(final int row, final int column)
    {
        Block container = getContainer();
        if(container == null)
            return;
        container = new Block(container);
        if(this.dragCount <= 0)
        {
            if(getSelectedBlockCount(container, row, column) > 0)
            {
                this.dragType = getSelectedBlockType(container, row, column);
                this.dragCount = removeFromSelectedBlock(container,
                                                         row,
                                                         column,
                                                         this.dragType,
                                                         getSelectedBlockCount(container,
                                                                               row,
                                                                               column));
                if(Main.isCreativeMode)
                    this.dragCount = 0;
                if(this.dragCount <= 0)
                    this.dragType = null;
            }
        }
        else if(getSelectedBlockCount(container, row, column) <= 0
                || Main.isCreativeMode)
        {
            int addedCount = addToSelectedBlock(container,
                                                row,
                                                column,
                                                this.dragType,
                                                this.dragCount);
            if(!Main.isCreativeMode)
                this.dragCount -= addedCount;
            if(this.dragCount <= 0)
                this.dragType = null;
        }
        else
        // pick
        // up
        // more
        // blocks
        {
            int transferCount = Math.min(getSelectedBlockCount(container,
                                                               row,
                                                               column),
                                         Block.BLOCK_STACK_SIZE
                                                 - this.dragCount);
            transferCount = removeFromSelectedBlock(container,
                                                    row,
                                                    column,
                                                    this.dragType,
                                                    transferCount);
            this.dragCount += transferCount;
        }
        world.setBlock(this.blockX, this.blockY, this.blockZ, container);
    }

    private Vector handleMouseUpDown_t1 = Vector.allocate();

    /** @param event
     *            the event */
    public void handleMouseUpDown(final Main.MouseEvent event)
    {
        if(event.button == -1 && event.dWheel == 0)
            return;
        switch(this.state)
        {
        case Normal:
        {
            this.deleteAnimTime = -1;
            if(Main.platform.isTouchScreen())
            {
                if(event.isDown)
                {
                    this.startMouseX = event.mouseX;
                    this.startMouseY = event.mouseY;
                    this.isDragOperation = false;
                    this.lastMouseX = event.mouseX;
                    this.lastMouseY = event.mouseY;
                    this.mouseDownTime = 0;
                    this.touchWaitTime = 0.5f;
                    return;
                }
                if(!event.isDown && this.isDragOperation)
                {
                    this.mouseDownTime = 0;
                    this.isDragOperation = false;
                    return;
                }
                if(!event.isDown && mouseIsInHotbar(event.mouseX, event.mouseY)
                        && !this.isDragOperation)
                {
                    this.selectionX = mouseGetHotbarIndex(event.mouseX,
                                                          event.mouseY);
                    this.mouseDownTime = 0;
                    return;
                }
                if(!event.isDown
                        && mouseIsInPauseButton(event.mouseX, event.mouseY)
                        && !this.isDragOperation)
                {
                    Main.needPause = true;
                    this.mouseDownTime = 0;
                    return;
                }
                if(!event.isDown
                        && mouseIsInInventoryButton(event.mouseX, event.mouseY)
                        && !this.isDragOperation)
                {
                    this.deleteAnimTime = -1;
                    this.workbenchSize = 2;
                    this.state = State.Workbench;
                    for(int x = 0; x < this.workbenchSize; x++)
                        for(int y = 0; y < this.workbenchSize; y++)
                            this.workbench[x + y * this.workbenchSize] = null;
                    this.lastMouseX = -1;
                    this.lastMouseY = -1;
                    this.dragCount = 0;
                    this.dragType = null;
                    this.mouseDownTime = 0;
                    return;
                }
                if(!event.isDown
                        && mouseIsInFlyButton(event.mouseX, event.mouseY)
                        && !this.isDragOperation && Main.isCreativeMode)
                {
                    this.isFlying = !this.isFlying;
                    this.mouseDownTime = 0;
                    return;
                }
                if(!event.isDown
                        && mouseIsInSneakButton(event.mouseX, event.mouseY)
                        && !this.isDragOperation)
                {
                    this.isSneaking = !this.isSneaking;
                    this.mouseDownTime = 0;
                    return;
                }
                if(!event.isDown
                        && mouseIsInJumpButton(event.mouseX, event.mouseY)
                        && !this.isDragOperation)
                {
                    handleJump();
                    this.mouseDownTime = 0;
                    return;
                }
            }
            if(event.isDown && event.button == Main.MouseEvent.LEFT
                    && !Main.platform.isTouchScreen())
            {
                this.mouseDownTime = 0;
            }
            else if((!Main.platform.isTouchScreen() && event.isDown && event.button == Main.MouseEvent.RIGHT)
                    || (Main.platform.isTouchScreen() && !event.isDown
                            && !this.isDragOperation && mouseIsInPlaceButton(event.mouseX,
                                                                             event.mouseY)))
            {
                if(getCurrentHotbarBlock() == null)
                    return;
                if(getCurrentHotbarBlock().getType() == BlockType.BTSnow)
                {
                    Block b = takeBlock();
                    world.insertEntity(Entity.NewThrownBlock(this.position,
                                                             b,
                                                             Vector.mul(throwBlock_t1,
                                                                        getForwardVector(),
                                                                        15.0f)));
                    return;
                }
                Block oldb;
                {
                    boolean didAnything = false;
                    oldb = getSelectedBlock();
                    if(oldb == null)
                        return;
                    Block newb = takeBlock();
                    if(newb.getType() == BlockType.BTBucket)
                    {
                        if(oldb.isItemInBucket())
                        {
                            if(!giveBlock(oldb.getItemInBucket(), false))
                                dropBlock(oldb.getItemInBucket());
                            newb = new Block();
                            world.setBlock(this.blockX,
                                           this.blockY,
                                           this.blockZ,
                                           newb);
                            didAnything = true;
                        }
                    }
                    if(newb.getToolType() == ToolType.Hoe)
                    {
                        if(oldb.getType() == BlockType.BTGrass
                                || oldb.getType() == BlockType.BTDirt)
                        {
                            oldb = Block.NewFarmland(false);
                            newb = new Block(newb);
                            newb.toolUseTool();
                            world.setBlock(this.blockX,
                                           this.blockY,
                                           this.blockZ,
                                           oldb);
                            didAnything = true;
                        }
                    }
                    if(newb.getType() == BlockType.BTBoneMeal)
                    {
                        if(oldb.onUseBoneMeal(this.blockX,
                                              this.blockY,
                                              this.blockZ))
                        {
                            return;
                        }
                    }
                    if(!giveBlock(newb, true))
                        dropBlock(newb);
                    if(didAnything)
                        return;
                }
                Block tempBlock = null;
                int tempX = 0, tempY = 0, tempZ = 0;
                if(oldb != null && oldb.isReplaceable())
                {
                    tempBlock = oldb;
                    tempX = this.blockX;
                    tempY = this.blockY;
                    tempZ = this.blockZ;
                    world.setBlock(this.blockX,
                                   this.blockY,
                                   this.blockZ,
                                   new Block());
                }
                this.isShiftDown = true;
                oldb = getSelectedBlock();
                this.isShiftDown = false;
                if(tempBlock != null)
                {
                    int dist = Math.abs(tempX + this.blockX)
                            + Math.abs(tempY + this.blockY)
                            + Math.abs(tempZ + this.blockZ);
                    if(dist > 1)
                    {
                        world.setBlock(tempX, tempY, tempZ, tempBlock);
                        tempBlock = null;
                        oldb = getSelectedBlock();
                        if(oldb == null)
                            return;
                    }
                }
                if(canPlaceBlock(oldb, this.blockX, this.blockY, this.blockZ))
                {
                    Block newb = takeBlock();
                    boolean isItemInBucket = newb.isItemInBucket();
                    if(newb != null)
                        newb = newb.makePlacedBlock(this.blockOrientation,
                                                    Block.getOrientationFromVector(getForwardVector()),
                                                    Block.getOrientationFromVector(getMoveForwardVector()));
                    if(newb != null)
                    {
                        world.setBlock(this.blockX,
                                       this.blockY,
                                       this.blockZ,
                                       newb);
                        if(!Main.isCreativeMode && isItemInBucket)
                            if(!giveBlock(Block.NewBucket(), false))
                                dropBlock(Block.NewBucket());
                        Main.play(Main.popAudio);
                        internalSetPosition(this.handleMouseUpDown_t1.set(this.position));
                        this.deleteAnimTime = -1;
                        return;
                    }
                    internalSetPosition(this.handleMouseUpDown_t1.set(this.position));
                    this.deleteAnimTime = -1;
                }
                if(tempBlock != null)
                {
                    world.setBlock(tempX, tempY, tempZ, tempBlock);
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
            else if(!event.isDown
                    && ((event.button == Main.MouseEvent.LEFT && !Main.platform.isTouchScreen()) || (Main.platform.isTouchScreen() && !this.isDragOperation)))
            {
                Block b = getSelectedBlock();
                boolean didAction = false;
                if(b != null && b.getType() == BlockType.BTChest)
                {
                    this.state = State.Chest;
                    this.dragCount = 0;
                    this.dragType = null;
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
                else if(b != null
                        && (b.getType() == BlockType.BTDispenser || b.getType() == BlockType.BTDropper))
                {
                    this.state = State.DispenserDropper;
                    this.dragCount = 0;
                    this.dragType = null;
                    didAction = true;
                }
                else if(b != null && b.getType() == BlockType.BTStoneButton)
                {
                    b = Block.NewStoneButton(b.getType().getOnTime(),
                                             b.getOrientation());
                    world.setBlock(this.blockX, this.blockY, this.blockZ, b);
                    didAction = true;
                }
                else if(b != null && b.getType() == BlockType.BTWoodButton)
                {
                    b = Block.NewWoodButton(b.getType().getOnTime(),
                                            b.getOrientation());
                    world.setBlock(this.blockX, this.blockY, this.blockZ, b);
                    didAction = true;
                }
                else if(b != null
                        && (b.getType() == BlockType.BTRedstoneRepeaterOff || b.getType() == BlockType.BTRedstoneRepeaterOn))
                {
                    b = new Block(b);
                    b.redstoneRepeaterStepDelay();
                    world.setBlock(this.blockX, this.blockY, this.blockZ, b);
                    didAction = true;
                }
                else if(b != null && b.getType() == BlockType.BTLever)
                {
                    b = new Block(b);
                    b.leverToggle();
                    world.setBlock(this.blockX, this.blockY, this.blockZ, b);
                    didAction = true;
                }
                else if(b != null && b.getType() == BlockType.BTTNT)
                {
                    // TODO finish
                }
                else if(b != null
                        && b.getType() == BlockType.BTRedstoneComparator)
                {
                    b = new Block(b);
                    b.redstoneComparatorToggleSubtractMode();
                    world.setBlock(this.blockX, this.blockY, this.blockZ, b);
                    didAction = true;
                }
                else if(b != null && b.getType() == BlockType.BTHopper)
                {
                    this.state = State.Hopper;
                    this.dragCount = 0;
                    this.dragType = null;
                    didAction = true;
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
                                count = 1;
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
                            {
                                if(this.dragCount + 1 > Block.BLOCK_STACK_SIZE)
                                    this.dragCount = Block.BLOCK_STACK_SIZE;
                                else
                                    this.dragCount++;
                            }
                            else
                            {
                                this.dragCount = 0;
                                this.dragType = null;
                            }
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
                        handleMouseDownOnContainer(row, column);
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
                                quitToNormal();
                                return;
                            }
                            if(this.dragType.getBurnTime() > 0)
                            {
                                b.furnaceAddFire(this.dragType);
                                if(!Main.isCreativeMode)
                                {
                                    if(--this.dragCount <= 0)
                                    {
                                        if(this.dragType.isItemInBucket())
                                        {
                                            this.dragType = Block.NewBucket();
                                            this.dragCount = 1;
                                        }
                                        else
                                            this.dragType = null;
                                    }
                                    else
                                    {
                                        if(this.dragType.isItemInBucket()
                                                && !giveBlock(Block.NewBucket(),
                                                              false))
                                            dropBlock(Block.NewBucket());
                                    }
                                }
                            }
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
                                quitToNormal();
                                return;
                            }
                            if(b.furnaceAddBlock(this.dragType))
                            {
                                if(!Main.isCreativeMode)
                                {
                                    if(--this.dragCount <= 0)
                                        this.dragType = null;
                                }
                            }
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
                            quitToNormal();
                            return;
                        }
                        if((this.dragCount <= 0 || this.dragType.equals(b.furnaceGetDestBlock()))
                                && this.dragCount < Block.BLOCK_STACK_SIZE)
                        {
                            Block newB = b.furnaceRemoveBlock();
                            if(newB != null && !Main.isCreativeMode)
                            {
                                this.dragCount++;
                                this.dragType = newB;
                            }
                        }
                        world.setBlock(this.blockX, this.blockY, this.blockZ, b);
                    }
                }
            }
            break;
        }
        case DispenserDropper:
        {
            this.deleteAnimTime = -1;
            if(!handleInventoryOrHotbarClick(event))
            {
                if(event.isDown && event.button == Main.MouseEvent.LEFT)
                {
                    if(mouseIsInDispenserDropper(event.mouseX, event.mouseY))
                    {
                        int row = mouseGetDispenserDropperRow(event.mouseX,
                                                              event.mouseY);
                        int column = mouseGetDispenserDropperColumn(event.mouseX,
                                                                    event.mouseY);
                        handleMouseDownOnContainer(row, column);
                    }
                }
                else if(event.isDown && event.button == Main.MouseEvent.RIGHT)
                {
                }
            }
            break;
        }
        case Hopper:
        {
            this.deleteAnimTime = -1;
            if(!handleInventoryOrHotbarClick(event))
            {
                if(event.isDown && event.button == Main.MouseEvent.LEFT)
                {
                    if(mouseIsInHopper(event.mouseX, event.mouseY))
                    {
                        int slot = mouseGetHopperSlot(event.mouseX,
                                                      event.mouseY);
                        handleMouseDownOnContainer(slot, 0);
                    }
                }
                else if(event.isDown && event.button == Main.MouseEvent.RIGHT)
                {
                }
            }
            break;
        }
        }
    }

    private static Vector isOnGround_newpos = Vector.allocate();
    private static Vector isOnGround_t1 = Vector.allocate();

    private boolean isOnGround()
    {
        Vector newpos = Vector.sub(isOnGround_newpos,
                                   this.position,
                                   0,
                                   0.01f + distLimit,
                                   0);
        int x = (int)Math.floor(newpos.getX()), y = (int)Math.floor(newpos.getY()), z = (int)Math.floor(newpos.getZ());
        Block b1 = world.getBlockEval(x, y, z);
        Block b2 = world.getBlockEval(x, y - 1, z);
        if((b1 != null && b1.adjustPlayerPosition(Vector.sub(isOnGround_t1,
                                                             newpos,
                                                             x,
                                                             y,
                                                             z), distLimit) == null)
                || (b2 != null && b2.adjustPlayerPosition(Vector.sub(isOnGround_t1,
                                                                     newpos,
                                                                     x,
                                                                     y - 1,
                                                                     z),
                                                          distLimit) == null))
        {
            return true;
        }
        else if(b1 == null || b2 == null)
        {
            this.velocity.set(Vector.ZERO);
            return false;
        }
        return false;
    }

    private static Vector isInWater_newpos = Vector.allocate();

    private boolean isInWater()
    {
        Vector newpos = Vector.sub(isInWater_newpos,
                                   this.position,
                                   0,
                                   0.01f + distLimit,
                                   0);
        int x = (int)Math.floor(newpos.getX()), y = (int)Math.floor(newpos.getY()), z = (int)Math.floor(newpos.getZ());
        Block b1 = world.getBlockEval(x, y, z);
        Block b2 = world.getBlockEval(x, y - 1, z);
        if(b1 != null && b1.getType() == BlockType.BTWater)
            return true;
        if(b2 != null && b2.getType() == BlockType.BTWater)
            return true;
        return false;
    }

    private static Vector isInClimbableBlock_newpos = Vector.allocate();

    private boolean isInClimbableBlock()
    {
        Vector newpos = Vector.add(isInClimbableBlock_newpos,
                                   this.position,
                                   0,
                                   0.25f,
                                   0);
        int x = (int)Math.floor(newpos.getX()), y = (int)Math.floor(newpos.getY()), z = (int)Math.floor(newpos.getZ());
        Block b = world.getBlockEval(x, y - 1, z);
        if(b != null && b.getType().isClimbable())
            return true;
        return false;
    }

    private void handleJump()
    {
        if(isOnGround())
        {
            this.velocity.set(0,
                              1.6f * (float)Math.sqrt(World.GravityAcceleration),
                              0);
        }
    }

    private Vector checkForStandingOnPressurePlate_newpos = Vector.allocate();

    private void checkForStandingOnPressurePlate()
    {
        Vector newpos = Vector.add(this.checkForStandingOnPressurePlate_newpos,
                                   this.position,
                                   0,
                                   0.25f,
                                   0);
        int x = (int)Math.floor(newpos.getX()), y = (int)Math.floor(newpos.getY()), z = (int)Math.floor(newpos.getZ());
        Block b = world.getBlockEval(x, y - 1, z);
        if(b != null
                && (b.getType() == BlockType.BTWoodPressurePlate || b.getType() == BlockType.BTStonePressurePlate))
        {
            b = new Block(b);
            b.pressurePlatePress();
            world.setBlock(x, y - 1, z, b);
        }
    }

    private static Vector getLadder_newpos = Vector.allocate();

    private Block getLadder()
    {
        Vector newpos = Vector.add(getLadder_newpos, this.position, 0, 0.25f, 0);
        int x = (int)Math.floor(newpos.getX()), y = (int)Math.floor(newpos.getY()), z = (int)Math.floor(newpos.getZ());
        return world.getBlockEval(x, y - 1, z);
    }

    private static Vector move_acc = Vector.allocate();
    private static Vector move_newvel = Vector.allocate();
    private static Vector move_v = Vector.allocate();
    private static Vector move_startPos = Vector.allocate();
    private static Vector move_t1 = Vector.allocate();
    private static Vector move_t2 = Vector.allocate();
    private static Vector move_pos = Vector.allocate();
    private static Vector move_forwardVec = Vector.allocate();
    private static Vector move_newPos = Vector.allocate();

    @Override
    public void move()
    {
        internalSetPosition(move_t1.set(this.position));
        switch(this.state)
        {
        case Normal:
        {
            boolean isFlying = (Main.platform.isTouchScreen() ? this.isFlying
                    : Main.isKeyDown(Main.KEY_F)) && Main.isCreativeMode;
            boolean inWater = isInWater();
            boolean inClimbableBlock = isInClimbableBlock();
            boolean isSneaking = Main.platform.isTouchScreen() ? this.isSneaking
                    : Main.isKeyDown(Main.KEY_SHIFT);
            if(isFlying)
            {
                float newMag = this.velocity.abs() - 15
                        * (float)Main.getFrameDuration();
                if(newMag <= 0)
                    this.velocity.set(Vector.ZERO);
                else
                    this.velocity = this.velocity.normalizeAndSet()
                                                 .mulAndSet(newMag);
            }
            else if(inWater)
            {
                Vector acc = Vector.mul(move_acc,
                                        this.velocity,
                                        -(float)Main.getFrameDuration());
                Vector newvel = Vector.add(move_newvel, this.velocity, acc);
                if(this.velocity.abs_squared() <= acc.abs_squared()
                        || this.velocity.dot(newvel) <= 0
                        || this.velocity.abs_squared() < 1e-4 * 1e-4)
                    this.velocity.set(Vector.ZERO);
                else
                    this.velocity.set(newvel);
            }
            else if(inClimbableBlock)
            {
                if(!isSneaking)
                    this.velocity.setY(Math.max(-1.5f,
                                                this.velocity.getY()
                                                        - World.GravityAcceleration
                                                        / 2.0f
                                                        * (float)Main.getFrameDuration()));
                else
                    this.velocity.setY(Math.max(0,
                                                this.velocity.getY()
                                                        - World.GravityAcceleration
                                                        / 2.0f
                                                        * (float)Main.getFrameDuration()));
            }
            else
            {
                this.velocity.setY(this.velocity.getY()
                        - World.GravityAcceleration
                        * (float)Main.getFrameDuration());
            }
            Vector v = Vector.mul(move_v,
                                  this.velocity,
                                  (float)Main.getFrameDuration());
            {
                Vector startPos = move_startPos.set(this.position);
                internalSetPosition(Vector.add(move_t1, this.position, v));
                if(Vector.sub(move_t1, startPos, this.position).abs_squared() < 1e-1f * 1e-1f * v.abs_squared())
                    this.velocity = this.velocity.mulAndSet((float)Math.pow(0.03f,
                                                                            Main.getFrameDuration()));
            }
            if(isOnGround())
            {
                checkForStandingOnPressurePlate();
                this.velocity.set(Vector.ZERO);
            }
            Vector forwardVec;
            if(isFlying)
                forwardVec = getForwardVector();
            else if(inWater)
                forwardVec = getForwardVector();
            else if(inClimbableBlock)
            {
                forwardVec = getMoveForwardVector();
                Block ladder = getLadder();
                if(ladder.getType() == BlockType.BTLadder)
                {
                    Vector pos = Vector.sub(move_pos,
                                            this.position,
                                            (float)Math.floor(this.position.getX()),
                                            0,
                                            (float)Math.floor(this.position.getZ()));
                    pos.setY(0.5f);
                    if(ladder.climbableIsPlayerPushingIntoLadder(pos,
                                                                 forwardVec))
                    {
                        forwardVec = move_forwardVec.set(0, 1, 0);
                    }
                }
            }
            else
                forwardVec = getMoveForwardVector();
            boolean isMoving = false;
            Vector startPos = move_startPos.set(this.position);
            boolean isMovingForward = Main.platform.isTouchScreen() ? isMoveUpPressed()
                    : Main.isKeyDown(Main.KEY_W);
            boolean isMovingBackward = Main.platform.isTouchScreen() ? isMoveDownPressed()
                    : Main.isKeyDown(Main.KEY_S);
            if(!isMovingForward || !isMovingBackward)
            {
                if(isMovingForward)
                {
                    Vector newPos = Vector.add(move_newPos,
                                               this.position,
                                               Vector.mul(move_t1,
                                                          forwardVec,
                                                          3.5f * (float)Main.getFrameDuration()));
                    internalSetPosition(newPos);
                    if(!this.position.equals(newPos) && inClimbableBlock)
                    {
                        forwardVec = getMoveForwardVector();
                        Block ladder = getLadder();
                        Vector pos = Vector.sub(move_pos,
                                                this.position,
                                                (float)Math.floor(this.position.getX()),
                                                0,
                                                (float)Math.floor(this.position.getZ()));
                        pos.setY(0.5f);
                        if(ladder.climbableIsPlayerPushingIntoLadder(pos,
                                                                     forwardVec))
                        {
                            forwardVec = move_forwardVec.set(0, 1, 0);
                            internalSetPosition(Vector.add(move_t1,
                                                           this.position,
                                                           Vector.mul(move_t2,
                                                                      forwardVec,
                                                                      3.5f * (float)Main.getFrameDuration())));
                        }
                    }
                    isMoving = !startPos.equals(this.position);
                }
                if(isMovingBackward)
                {
                    internalSetPosition(Vector.sub(move_t1,
                                                   this.position,
                                                   Vector.mul(move_t2,
                                                              forwardVec,
                                                              3.5f * (float)Main.getFrameDuration())));
                    isMoving = !startPos.equals(this.position);
                }
            }
            if(!isFlying && inWater && !isMoving)
            {
                this.velocity.setY(this.velocity.getY()
                        - World.GravityAcceleration
                        * (float)Main.getFrameDuration() * 0.25f);
            }
            if(Main.platform.isTouchScreen() ? isJumpPressed()
                    : Main.isKeyDown(Main.KEY_SPACE))
            {
                handleJump();
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
        case DispenserDropper:
        {
            this.isShiftDown = false;
            break;
        }
        case Hopper:
        {
            this.isShiftDown = false;
            break;
        }
        }
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
                if(!internalGiveBlock(this.dragType, false))
                    dropBlock(this.dragType);
            this.state = State.Normal;
            for(int x = 0; x < this.workbenchSize; x++)
            {
                for(int y = 0; y < this.workbenchSize; y++)
                {
                    if(this.workbench[x + this.workbenchSize * y] == null)
                        continue;
                    if(!internalGiveBlock(this.workbench[x + this.workbenchSize
                            * y], false))
                        dropBlock(this.workbench[x + this.workbenchSize * y]);
                    this.workbench[x + this.workbenchSize * y] = null;
                }
            }
            this.wasPaused = true;
            break;
        }
        case Chest:
        case DispenserDropper:
        case Hopper:
        {
            this.deleteAnimTime = -1;
            for(int i = 0; i < this.dragCount; i++)
                if(!internalGiveBlock(this.dragType, false))
                    dropBlock(this.dragType);
            this.state = State.Normal;
            this.wasPaused = true;
            break;
        }
        case Furnace:
        {
            this.deleteAnimTime = -1;
            for(int i = 0; i < this.dragCount; i++)
                if(!internalGiveBlock(this.dragType, false))
                    dropBlock(this.dragType);
            this.state = State.Normal;
            this.wasPaused = true;
            break;
        }
        }
    }

    /** @param event
     *            the event to handle */
    public void handleKeyboardEvent(final Main.KeyboardEvent event)
    {
        if(Main.platform.isTouchScreen())
            return;
        if(event.isDown)
        {
            if(event.key == Main.KEY_F2)
            {
                Main.saveAll();
                return;
            }
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
                    this.dragCount = 0;
                    this.dragType = null;
                }
                if(event.key == Main.KEY_Q)
                {
                    throwBlock(takeBlock());
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
            case DispenserDropper:
            case Hopper:
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
        }
    }

    /** write to a <code>DataOutput</code>
     * 
     * @param o
     *            <code>OutputStream</code> to write to
     * @throws IOException
     *             the exception thrown */
    public void write(final DataOutput o) throws IOException
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
    public static Player read(final DataInput i) throws IOException
    {
        Player retval = new Player(Vector.read(i));
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

    private static Vector push_t1 = Vector.allocate();

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
    public void push(final int bx,
                     final int by,
                     final int bz,
                     final int dx,
                     final int dy,
                     final int dz)
    {
        boolean doPush = false;
        for(int pdy = -1; pdy <= 0; pdy++)
        {
            Vector p = Vector.add(push_t1, this.position, 0, pdy, 0);
            int x = (int)Math.floor(p.getX());
            int y = (int)Math.floor(p.getY());
            int z = (int)Math.floor(p.getZ());
            if(x == bx && y == by && z == bz)
            {
                doPush = true;
                break;
            }
        }
        if(!doPush)
            return;
        internalSetPosition(Vector.add(push_t1, this.position, dx, dy, dz));
    }
}
