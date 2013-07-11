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
import static org.voxels.PlayerList.players;
import static org.voxels.World.world;

import java.awt.GridLayout;
import java.io.*;
import java.lang.reflect.InvocationTargetException;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;

import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.openal.AL;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.newdawn.slick.openal.Audio;
import org.newdawn.slick.openal.AudioLoader;
import org.voxels.generate.Rand;

/** @author jacob */
public final class Main
{
    /** the screen x resolution */
    public static int ScreenXRes = 640;
    /** the screen y resolution */
    public static int ScreenYRes = 480;
    /** the aspect ratio */
    public static float aspectRatio = (float)ScreenXRes / ScreenYRes;
    /** the program's version */
    public static final String Version = "0.2.2";
    /** true if this program is running as a server */
    public static boolean isServer = false;
    /** true if this program is the debug version */
    public static boolean DEBUG = false;
    /** if running in fancy graphics mode instead of fast graphics mode */
    public static boolean FancyGraphics = false;
    static boolean lastEventWasLeftButtonDown = false;

    /** @author jacob */
    public static class MouseEvent
    {
        /** the mouse x coordinate */
        public final int mouseX;
        /** the mouse y coordinate */
        public final int mouseY;
        /** the button this event is about */
        public final int button;
        /** the amount that the mouse wheel was moved */
        public final int dWheel;
        /** true if the button is pressed */
        public final boolean isDown;
        /** left mouse button */
        public static final int LEFT = 0;
        /** right mouse button */
        public static final int RIGHT = 1;
        /** middle mouse button */
        public static final int MIDDLE = 2;

        /**
		 * 
		 */
        public MouseEvent()
        {
            this.mouseX = Mouse.getEventX();
            this.mouseY = Display.getHeight() - Mouse.getEventY() - 1;
            int button = Mouse.getEventButton();
            this.isDown = Mouse.getEventButtonState();
            boolean eventWasLeftButtonDown = false;
            if(button == LEFT && Main.isKeyDown(Main.KEY_ALT)
                    && (!lastEventWasLeftButtonDown || this.isDown))
                button = RIGHT;
            else if(button == LEFT && this.isDown)
                eventWasLeftButtonDown = true;
            lastEventWasLeftButtonDown = eventWasLeftButtonDown;
            this.button = button;
            this.dWheel = Mouse.getEventDWheel();
        }
    }

    private static String frameText = "";

    /** adds a string to the current frame's overlaid text
     * 
     * @param str
     *            the string to add */
    public static void addToFrameText(String str)
    {
        frameText += str;
    }

    private static float fps = 30.0f;

    private static void renderFrame()
    {
        players.draw();
        glClear(GL_DEPTH_BUFFER_BIT);
        final float dist = 480f / Text.sizeH("A") / 2.0f;
        Text.draw(Matrix.translate(-dist * aspectRatio, dist - 1.0f, -dist),
                  Color.RGB(1.0f, 1.0f, 1.0f),
                  frameText);
        frameText = "";
        if(DEBUG)
        {
            String fpsStr = "?";
            if(getFrameDuration() > 0)
            {
                fps = fps * (1.0f - (float)getFrameDuration()) + 1.0f;
                fpsStr = "0000000" + Integer.toString((int)(fps * 100.0f));
                fpsStr = fpsStr.substring(0, fpsStr.length() - 2) + "."
                        + fpsStr.substring(fpsStr.length() - 2);
                while(fpsStr.charAt(0) == '0' && fpsStr.charAt(1) != '.')
                {
                    fpsStr = fpsStr.substring(1);
                }
            }
            addToFrameText("Voxels " + Version + "\n" + "FPS : " + fpsStr
                    + "\n");
        }
        else
        {
            addToFrameText("Voxels " + Version + "\n");
        }
    }

    private static void setFullscreen(boolean isFullscreen)
    {
        try
        {
            if(Display.isFullscreen() && !isFullscreen
                    || !Display.isFullscreen() && isFullscreen)
            {
                Display.setFullscreen(isFullscreen);
            }
        }
        catch(LWJGLException e)
        {
            e.printStackTrace();
            System.exit(1);
        }
        glEnable(GL_DEPTH_TEST);
        final double minGLdist = 1e-2;
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_ALPHA_TEST);
        glEnable(GL_CULL_FACE); // TODO fix
        glEnable(GL_BLEND);
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW);
        glAlphaFunc(GL_LESS, 0.85f);
        glBlendFunc(GL_ONE_MINUS_SRC_ALPHA, GL_SRC_ALPHA);
        glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
        glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);
        glViewport(0, 0, ScreenXRes, ScreenYRes);
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glFrustum(-minGLdist * aspectRatio,
                  minGLdist * aspectRatio,
                  -minGLdist,
                  minGLdist,
                  minGLdist,
                  150f);
    }

    /** @return the current time in seconds */
    public static double Timer()
    {
        return (double)Sys.getTime() / Sys.getTimerResolution();
    }

    private static double frameDuration = 0.05;

    /** @return the duration in seconds of the last frame */
    public static double getFrameDuration()
    {
        return frameDuration;
    }

    /** the A key */
    public static final int KEY_A = Keyboard.KEY_A;
    /** the B key */
    public static final int KEY_B = Keyboard.KEY_B;
    /** the C key */
    public static final int KEY_C = Keyboard.KEY_C;
    /** the D key */
    public static final int KEY_D = Keyboard.KEY_D;
    /** the E key */
    public static final int KEY_E = Keyboard.KEY_E;
    /** the F key */
    public static final int KEY_F = Keyboard.KEY_F;
    /** the G key */
    public static final int KEY_G = Keyboard.KEY_G;
    /** the H key */
    public static final int KEY_H = Keyboard.KEY_H;
    /** the I key */
    public static final int KEY_I = Keyboard.KEY_I;
    /** the J key */
    public static final int KEY_J = Keyboard.KEY_J;
    /** the K key */
    public static final int KEY_K = Keyboard.KEY_K;
    /** the L key */
    public static final int KEY_L = Keyboard.KEY_L;
    /** the M key */
    public static final int KEY_M = Keyboard.KEY_M;
    /** the N key */
    public static final int KEY_N = Keyboard.KEY_N;
    /** the O key */
    public static final int KEY_O = Keyboard.KEY_O;
    /** the P key */
    public static final int KEY_P = Keyboard.KEY_P;
    /** the Q key */
    public static final int KEY_Q = Keyboard.KEY_Q;
    /** the R key */
    public static final int KEY_R = Keyboard.KEY_R;
    /** the S key */
    public static final int KEY_S = Keyboard.KEY_S;
    /** the T key */
    public static final int KEY_T = Keyboard.KEY_T;
    /** the U key */
    public static final int KEY_U = Keyboard.KEY_U;
    /** the V key */
    public static final int KEY_V = Keyboard.KEY_V;
    /** the W key */
    public static final int KEY_W = Keyboard.KEY_W;
    /** the X key */
    public static final int KEY_X = Keyboard.KEY_X;
    /** the Y key */
    public static final int KEY_Y = Keyboard.KEY_Y;
    /** the Z key */
    public static final int KEY_Z = Keyboard.KEY_Z;
    /** the F1 key */
    public static final int KEY_F1 = Keyboard.KEY_F1;
    /** the F2 key */
    public static final int KEY_F2 = Keyboard.KEY_F2;
    /** the F3 key */
    public static final int KEY_F3 = Keyboard.KEY_F3;
    /** the F4 key */
    public static final int KEY_F4 = Keyboard.KEY_F4;
    /** the F5 key */
    public static final int KEY_F5 = Keyboard.KEY_F5;
    /** the F6 key */
    public static final int KEY_F6 = Keyboard.KEY_F6;
    /** the F7 key */
    public static final int KEY_F7 = Keyboard.KEY_F7;
    /** the F8 key */
    public static final int KEY_F8 = Keyboard.KEY_F8;
    /** the F9 key */
    public static final int KEY_F9 = Keyboard.KEY_F9;
    /** the F10 key */
    public static final int KEY_F10 = Keyboard.KEY_F10;
    /** the F11 key */
    public static final int KEY_F11 = Keyboard.KEY_F11;
    /** the F12 key */
    public static final int KEY_F12 = Keyboard.KEY_F12;
    /** the 0 key */
    public static final int KEY_0 = Keyboard.KEY_0;
    /** the 1 key */
    public static final int KEY_1 = Keyboard.KEY_1;
    /** the 2 key */
    public static final int KEY_2 = Keyboard.KEY_2;
    /** the 3 key */
    public static final int KEY_3 = Keyboard.KEY_3;
    /** the 4 key */
    public static final int KEY_4 = Keyboard.KEY_4;
    /** the 5 key */
    public static final int KEY_5 = Keyboard.KEY_5;
    /** the 6 key */
    public static final int KEY_6 = Keyboard.KEY_6;
    /** the 7 key */
    public static final int KEY_7 = Keyboard.KEY_7;
    /** the 8 key */
    public static final int KEY_8 = Keyboard.KEY_8;
    /** the 9 key */
    public static final int KEY_9 = Keyboard.KEY_9;
    /** the Apostrophe key */
    public static final int KEY_APOSTROPHE = Keyboard.KEY_APOSTROPHE;
    /** the up arrow key */
    public static final int KEY_UP = Keyboard.KEY_UP;
    /** the down arrow key */
    public static final int KEY_DOWN = Keyboard.KEY_DOWN;
    /** the left arrow key */
    public static final int KEY_LEFT = Keyboard.KEY_LEFT;
    /** the right arrow key */
    public static final int KEY_RIGHT = Keyboard.KEY_RIGHT;
    /** the left shift key */
    public static final int KEY_LSHIFT = Keyboard.KEY_LSHIFT;
    /** the right shift key */
    public static final int KEY_RSHIFT = Keyboard.KEY_RSHIFT;
    /** the left control key */
    public static final int KEY_LCTRL = Keyboard.KEY_LCONTROL;
    /** the right control key */
    public static final int KEY_RCTRL = Keyboard.KEY_RCONTROL;
    /** the left alt key */
    public static final int KEY_LALT = Keyboard.KEY_LMENU;
    /** the right alt key */
    public static final int KEY_RALT = Keyboard.KEY_RMENU;
    /** the return (enter) key */
    public static final int KEY_RETURN = Keyboard.KEY_RETURN;
    /** the left or right shift key key */
    public static final int KEY_SHIFT = -10000;
    /** the left or right Control key */
    public static final int KEY_CTRL = -10001;
    /** the left or right Alt key */
    public static final int KEY_ALT = -10002;
    /** the special value that means that no key corresponds to this character */
    public static final int KEY_NONE = Keyboard.KEY_NONE;
    /** the Escape key */
    public static final int KEY_ESCAPE = Keyboard.KEY_ESCAPE;
    /** the Space key */
    public static final int KEY_SPACE = Keyboard.KEY_SPACE;
    /** the Delete key */
    public static final int KEY_DELETE = Keyboard.KEY_DELETE;
    /** the special value that means that no character was translated */
    public static final char CHAR_NONE = (char)Keyboard.CHAR_NONE;

    /** @param key
     *            the key to check for
     * @return true if the key is pressed */
    public static boolean isKeyDown(int key)
    {
        if(key == KEY_SHIFT)
        {
            return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)
                    || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        }
        if(key == KEY_ALT)
        {
            return Keyboard.isKeyDown(Keyboard.KEY_LMENU)
                    || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
        }
        if(key == KEY_CTRL)
        {
            return Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)
                    || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
        }
        return Keyboard.isKeyDown(key);
    }

    /** @author jacob */
    public static class KeyboardEvent
    {
        /**
		 * 
		 */
        public final int key;
        /**
		 * 
		 */
        public final char character;
        /** if the key is pressed */
        public final boolean isDown;
        /** if this key press event is a repeat */
        public final boolean isRepeat;

        /**
		 * 
		 */
        public KeyboardEvent()
        {
            this.key = Keyboard.getEventKey();
            this.character = Keyboard.getEventCharacter();
            this.isDown = Keyboard.getEventKeyState();
            this.isRepeat = Keyboard.isRepeatEvent();
        }
    }

    private static int getModeDist(DisplayMode mode)
    {
        return Math.abs(mode.getWidth() - 640)
                + Math.abs(mode.getHeight() - 480);
    }

    private static Rand.Settings makeLandGeneratorSettings()
    {
        Rand.SettingsCreatorMenu menu = new Rand.SettingsCreatorMenu();
        runMenu(menu);
        return menu.settings;
    }

    private static void generateGame()
    {
        saveFile = null;
        World.clear();
        world.setLandGeneratorSettings(makeLandGeneratorSettings());
        players.clear();
        players.addDefaultPlayer();
        // if(DEBUG)
        // {
        // for(int i = 0; i < 10000; i++)
        // {
        // players.front().giveBlock(BlockType.BTChest);
        // players.front().giveBlock(BlockType.BTFurnace);
        // players.front().giveBlock(BlockType.BTSapling);
        // players.front().giveBlock(BlockType.BTTorch);
        // players.front().giveBlock(BlockType.BTWorkbench);
        // players.front().giveBlock(BlockType.BTLadder);
        // players.front().giveBlock(BlockType.BTLadder);
        // players.front().giveBlock(BlockType.BTLadder);
        // players.front().giveBlock(BlockType.BTLadder);
        // players.front().giveBlock(BlockType.BTLadder);
        // players.front().giveBlock(BlockType.BTLadder);
        // players.front().giveBlock(BlockType.BTRedstoneDustOff);
        // players.front().giveBlock(BlockType.BTRedstoneDustOff);
        // players.front().giveBlock(BlockType.BTRedstoneDustOff);
        // players.front().giveBlock(BlockType.BTRedstoneDustOff);
        // players.front().giveBlock(BlockType.BTRedstoneDustOff);
        // players.front().giveBlock(BlockType.BTRedstoneTorchOff);
        // players.front().giveBlock(BlockType.BTLever);
        // players.front().giveBlock(BlockType.BTStonePressurePlate);
        // players.front().giveBlock(BlockType.BTWoodPressurePlate);
        // players.front().giveBlock(BlockType.BTLava);
        // players.front().giveBlock(BlockType.BTWater);
        // players.front().giveBlock(BlockType.BTObsidian);
        // players.front().giveBlock(BlockType.BTStone);
        // players.front().giveBlock(BlockType.BTRedstoneRepeaterOff);
        // players.front().giveBlock(BlockType.BTSlime);
        // players.front().giveBlock(BlockType.BTSlime);
        // players.front().giveBlock(BlockType.BTPiston);
        // players.front().giveBlock(BlockType.BTPiston);
        // players.front().giveBlock(BlockType.BTSand);
        // players.front().giveBlock(BlockType.BTGunpowder);
        // players.front().giveBlock(BlockType.BTTNT);
        // players.front().giveBlock(BlockType.BTTNT);
        // players.front().giveBlock(BlockType.BTTNT);
        // players.front().giveBlock(BlockType.BTTNT);
        // players.front().giveBlock(BlockType.BTTNT);
        // players.front().giveBlock(BlockType.BTTNT);
        // players.front().giveBlock(BlockType.BTTNT);
        // players.front().giveBlock(BlockType.BTTNT);
        // players.front().giveBlock(BlockType.BTTNT);
        // players.front().giveBlock(BlockType.BTBlazeRod);
        // players.front().giveBlock(BlockType.BTDiamondPick);
        // players.front().giveBlock(BlockType.BTGoldPick);
        // }
        // }
        didLoad = true;
    }

    static boolean isFullscreen = false;
    private static double curTime;
    private static double lastFrameStartTime;
    /**
     * 
     */
    public static boolean isCreativeMode = false;

    /** @param menu
     *            the menu to run */
    public static void runMenu(MenuScreen menu)
    {
        Mouse.setGrabbed(false);
        boolean done = false;
        while(!done)
        {
            setFullscreen(isFullscreen);
            menu.draw();
            Display.update();
            {
                while(Mouse.next())
                {
                    MouseEvent event = new MouseEvent();
                    if(event.isDown)
                        menu.onClick((float)event.mouseX / ScreenXRes * 2f - 1f,
                                     1f - (float)event.mouseY / ScreenYRes * 2f);
                }
                while(Keyboard.next())
                {
                    KeyboardEvent event = new KeyboardEvent();
                    if(event.isDown && event.key == KEY_F4
                            && isKeyDown(KEY_ALT))
                    {
                        done = true;
                        continue;
                    }
                    if(event.isDown && event.key == KEY_F11)
                    {
                        isFullscreen = !isFullscreen;
                        continue;
                    }
                }
                int mouseX = Mouse.getX();
                int mouseY = Display.getHeight() - Mouse.getY() - 1;
                menu.onMouseOver((float)mouseX / ScreenXRes * 2f - 1f, 1f
                        - (float)mouseY / ScreenYRes * 2f);
            }
            Display.sync(60);
            curTime = Timer();
            frameDuration = curTime - lastFrameStartTime;
            internalSaveAll();
            lastFrameStartTime = curTime;
            if(menu.isDone())
                done = true;
            if(Display.isCloseRequested())
                done = true;
        }
    }

    /** @param title
     *            the title
     * @param message
     *            the message */
    public static void alert(final String title, final String message)
    {
        runMenu(new MenuScreen(Color.V(0.75f))
        {
            @Override
            protected void drawBackground(Matrix tform)
            {
                super.drawBackground(tform);
                String str = title;
                float xScale = 2f / 40f;
                Text.draw(Matrix.scale(xScale, 2f / 40f, 1.0f)
                                .concat(Matrix.translate(-xScale
                                                                 / 2f
                                                                 * Text.sizeW(str)
                                                                 / Text.sizeW("A"),
                                                         0.7f,
                                                         0))
                                .concat(tform),
                          Color.RGB(0, 0, 0),
                          str);
            }

            {
                add(new TextMenuItem(message,
                                     Color.RGB(0f, 0f, 0f),
                                     this.getBackgroundColor(),
                                     Color.RGB(0f, 0f, 0f),
                                     Color.RGB(0.0f, 0.0f, 1.0f),
                                     this)
                {
                    @Override
                    public void onMouseOver(float mouseX, float mouseY)
                    {
                    }

                    @Override
                    public void onClick(float mouseX, float mouseY)
                    {
                    }
                });
                add(new SpacerMenuItem(Color.V(0.0f), this));
                MenuItem okButton = new TextMenuItem("OK",
                                                     Color.RGB(0f, 0f, 0f),
                                                     this.getBackgroundColor(),
                                                     Color.RGB(0f, 0f, 0f),
                                                     Color.RGB(0.0f, 0.0f, 1.0f),
                                                     this)
                {
                    @Override
                    public void onMouseOver(float mouseX, float mouseY)
                    {
                        select();
                    }

                    @Override
                    public void onClick(float mouseX, float mouseY)
                    {
                        this.container.close();
                    }
                };
                add(okButton);
                okButton.select();
            }
        });
    }

    private static void runScreenResolutionMenu()
    {
        runMenu(new MenuScreen(Color.V(0.75f))
        {
            public DisplayMode[] modes;
            private int selectedMode;

            @Override
            protected void drawBackground(Matrix tform)
            {
                super.drawBackground(tform);
                String str = "Change Screen Resolution";
                float xScale = 2f / 40f;
                Text.draw(Matrix.scale(xScale, 2f / 40f, 1.0f)
                                .concat(Matrix.translate(-xScale
                                                                 / 2f
                                                                 * Text.sizeW(str)
                                                                 / Text.sizeW("A"),
                                                         0.7f,
                                                         0))
                                .concat(tform),
                          Color.RGB(0, 0, 0),
                          str);
            }

            public void setSelectedMode(int selectedMode)
            {
                this.selectedMode = selectedMode;
                try
                {
                    Display.setDisplayMode(this.modes[this.selectedMode]);
                    ScreenXRes = this.modes[this.selectedMode].getWidth();
                    ScreenYRes = this.modes[this.selectedMode].getHeight();
                    aspectRatio = (float)ScreenXRes / ScreenYRes;
                }
                catch(LWJGLException e)
                {
                    StringWriter w = new StringWriter();
                    PrintWriter pw = new PrintWriter(w, true);
                    e.printStackTrace(pw);
                    alert("Can't change display mode", w.toString());
                }
            }

            public int getSelectedMode()
            {
                return this.selectedMode;
            }

            {
                try
                {
                    this.modes = Display.getAvailableDisplayModes();
                }
                catch(LWJGLException e)
                {
                    e.printStackTrace();
                    System.exit(1);
                }
                this.selectedMode = -1;
                DisplayMode[] newModes = new DisplayMode[0];
                for(int i = 0; i < this.modes.length; i++)
                {
                    boolean found = false;
                    for(int j = 0; j < newModes.length; j++)
                    {
                        if(newModes[j].getWidth() == this.modes[i].getWidth()
                                && newModes[j].getHeight() == this.modes[i].getHeight())
                        {
                            found = true;
                            int oldDist = Math.abs(newModes[j].getBitsPerPixel() - 32)
                                    + Math.abs(newModes[j].getFrequency() - 60);
                            int newDist = Math.abs(this.modes[i].getBitsPerPixel() - 32)
                                    + Math.abs(this.modes[i].getFrequency() - 60);
                            if(newDist <= oldDist)
                                newModes[j] = this.modes[i];
                        }
                    }
                    if(!found)
                    {
                        DisplayMode[] temp = new DisplayMode[newModes.length + 1];
                        for(int j = 0; j < newModes.length; j++)
                            temp[j] = newModes[j];
                        temp[newModes.length] = this.modes[i];
                        newModes = temp;
                    }
                }
                this.modes = newModes;
                for(int i = 0; i < this.modes.length; i++)
                {
                    if(this.modes[i].getWidth() == Display.getDisplayMode()
                                                          .getWidth()
                            && this.modes[i].getHeight() == Display.getDisplayMode()
                                                                   .getHeight())
                        this.selectedMode = i;
                    final int index = i;
                    add(new OptionMenuItem(Integer.toString(this.modes[i].getWidth())
                                                   + "x"
                                                   + Integer.toString(this.modes[i].getHeight()),
                                           Color.RGB(0f, 0f, 0f),
                                           this.getBackgroundColor(),
                                           Color.RGB(0f, 0f, 0f),
                                           Color.RGB(0.0f, 0.0f, 1.0f),
                                           this)
                    {
                        @Override
                        public void pick()
                        {
                            setSelectedMode(index);
                        }

                        @Override
                        public boolean isPicked()
                        {
                            return index == getSelectedMode();
                        }
                    });
                }
                add(new SpacerMenuItem(Color.V(0), this));
                add(new TextMenuItem("OK",
                                     Color.RGB(0f, 0f, 0f),
                                     this.getBackgroundColor(),
                                     Color.RGB(0f, 0f, 0f),
                                     Color.RGB(0.0f, 0.0f, 1.0f),
                                     this)
                {
                    @Override
                    public void onMouseOver(float mouseX, float mouseY)
                    {
                        select();
                    }

                    @Override
                    public void onClick(float mouseX, float mouseY)
                    {
                        this.container.close();
                    }
                });
            }
        });
    }

    @SuppressWarnings("synthetic-access")
    private static void runOptionsMenu()
    {
        runMenu(new MenuScreen(Color.V(0.75f))
        {
            @Override
            protected void drawBackground(Matrix tform)
            {
                super.drawBackground(tform);
                String str = "Options";
                float xScale = 2f / 40f;
                Text.draw(Matrix.scale(xScale, 2f / 40f, 1.0f)
                                .concat(Matrix.translate(-xScale
                                                                 / 2f
                                                                 * Text.sizeW(str)
                                                                 / Text.sizeW("A"),
                                                         0.7f,
                                                         0))
                                .concat(tform),
                          Color.RGB(0, 0, 0),
                          str);
            }

            {
                add(new CheckMenuItem("Debug Mode",
                                      Color.RGB(0f, 0f, 0f),
                                      this.getBackgroundColor(),
                                      Color.RGB(0f, 0f, 0f),
                                      Color.RGB(0.0f, 0.0f, 1.0f),
                                      this)
                {
                    @Override
                    public void setChecked(boolean checked)
                    {
                        Main.DEBUG = checked;
                    }

                    @Override
                    public boolean isChecked()
                    {
                        return Main.DEBUG;
                    }
                });
                add(new CheckMenuItem("Use Vertex Arrays\nand a Texture Atlas",
                                      Color.RGB(0f, 0f, 0f),
                                      this.getBackgroundColor(),
                                      Color.RGB(0f, 0f, 0f),
                                      Color.RGB(0.0f, 0.0f, 1.0f),
                                      this)
                {
                    @Override
                    public void setChecked(boolean checked)
                    {
                        RenderingStream.USE_VERTEX_ARRAY_AND_TEXTURE_ATLAS = checked;
                    }

                    @Override
                    public boolean isChecked()
                    {
                        return RenderingStream.USE_VERTEX_ARRAY_AND_TEXTURE_ATLAS;
                    }
                });
                add(new CheckMenuItem("Creative Mode",
                                      Color.RGB(0f, 0f, 0f),
                                      this.getBackgroundColor(),
                                      Color.RGB(0f, 0f, 0f),
                                      Color.RGB(0.0f, 0.0f, 1.0f),
                                      this)
                {
                    @Override
                    public void setChecked(boolean checked)
                    {
                        Main.isCreativeMode = checked;
                    }

                    @Override
                    public boolean isChecked()
                    {
                        return Main.isCreativeMode;
                    }
                });
                if(Main.didLoad)
                {
                    add(new TextMenuItem("Change Time To Morning",
                                         Color.RGB(0.0f, 0.0f, 0.0f),
                                         this.getBackgroundColor(),
                                         Color.RGB(0.0f, 0.0f, 0.0f),
                                         Color.RGB(0.0f, 0.0f, 1.0f),
                                         this)
                    {
                        @Override
                        public void onMouseOver(float mouseX, float mouseY)
                        {
                            select();
                        }

                        @Override
                        public void onClick(float mouseX, float mouseY)
                        {
                            world.setTimeOfDay(0.3f);
                        }
                    });
                }
                add(new SpacerMenuItem(Color.V(0), this));
                add(new CheckMenuItem("Fancy Graphics",
                                      Color.RGB(0f, 0f, 0f),
                                      this.getBackgroundColor(),
                                      Color.RGB(0f, 0f, 0f),
                                      Color.RGB(0.0f, 0.0f, 1.0f),
                                      this)
                {
                    @Override
                    public void setChecked(boolean checked)
                    {
                        Main.FancyGraphics = checked;
                    }

                    @Override
                    public boolean isChecked()
                    {
                        return Main.FancyGraphics;
                    }
                });
                add(new SpacerMenuItem(Color.V(0), this));
                add(new OptionMenuItem("Render Distance : Far",
                                       Color.RGB(0f, 0f, 0f),
                                       this.getBackgroundColor(),
                                       Color.RGB(0f, 0f, 0f),
                                       Color.RGB(0.0f, 0.0f, 1.0f),
                                       this)
                {
                    @Override
                    public void pick()
                    {
                        World.viewDist = 32;
                    }

                    @Override
                    public boolean isPicked()
                    {
                        return World.viewDist == 32;
                    }
                });
                add(new OptionMenuItem("Render Distance : Medium",
                                       Color.RGB(0f, 0f, 0f),
                                       this.getBackgroundColor(),
                                       Color.RGB(0f, 0f, 0f),
                                       Color.RGB(0.0f, 0.0f, 1.0f),
                                       this)
                {
                    @Override
                    public void pick()
                    {
                        World.viewDist = 16;
                    }

                    @Override
                    public boolean isPicked()
                    {
                        return World.viewDist == 16;
                    }
                });
                add(new OptionMenuItem("Render Distance : Default",
                                       Color.RGB(0f, 0f, 0f),
                                       this.getBackgroundColor(),
                                       Color.RGB(0f, 0f, 0f),
                                       Color.RGB(0.0f, 0.0f, 1.0f),
                                       this)
                {
                    @Override
                    public void pick()
                    {
                        World.viewDist = 10;
                    }

                    @Override
                    public boolean isPicked()
                    {
                        return World.viewDist == 10;
                    }
                });
                add(new OptionMenuItem("Render Distance : Fast",
                                       Color.RGB(0f, 0f, 0f),
                                       this.getBackgroundColor(),
                                       Color.RGB(0f, 0f, 0f),
                                       Color.RGB(0.0f, 0.0f, 1.0f),
                                       this)
                {
                    @Override
                    public void pick()
                    {
                        World.viewDist = 8;
                    }

                    @Override
                    public boolean isPicked()
                    {
                        return World.viewDist == 8;
                    }
                });
                add(new SpacerMenuItem(Color.V(0), this));
                add(new TextMenuItem("Change Screen Resolution...",
                                     Color.RGB(0.0f, 0.0f, 0.0f),
                                     this.getBackgroundColor(),
                                     Color.RGB(0.0f, 0.0f, 0.0f),
                                     Color.RGB(0.0f, 0.0f, 1.0f),
                                     this)
                {
                    @Override
                    public void onMouseOver(float mouseX, float mouseY)
                    {
                        select();
                    }

                    @Override
                    public void onClick(float mouseX, float mouseY)
                    {
                        runScreenResolutionMenu();
                    }
                });
                add(new CheckMenuItem("Full Screen",
                                      Color.RGB(0f, 0f, 0f),
                                      this.getBackgroundColor(),
                                      Color.RGB(0f, 0f, 0f),
                                      Color.RGB(0.0f, 0.0f, 1.0f),
                                      this)
                {
                    @Override
                    public void setChecked(boolean checked)
                    {
                        Main.isFullscreen = checked;
                    }

                    @Override
                    public boolean isChecked()
                    {
                        return Main.isFullscreen;
                    }
                });
                add(new SpacerMenuItem(Color.V(0), this));
                add(new TextMenuItem("Return To Main Menu",
                                     Color.RGB(0.0f, 0.0f, 0.0f),
                                     this.getBackgroundColor(),
                                     Color.RGB(0.0f, 0.0f, 0.0f),
                                     Color.RGB(0.0f, 0.0f, 1.0f),
                                     this)
                {
                    @Override
                    public void onMouseOver(float mouseX, float mouseY)
                    {
                        select();
                    }

                    @Override
                    public void onClick(float mouseX, float mouseY)
                    {
                        this.container.close();
                    }
                });
            }
        });
    }

    @SuppressWarnings("synthetic-access")
    private static void runMainMenu()
    {
        runMenu(new MenuScreen(Color.RGB(0.75f, 0.75f, 0.75f))
        {
            @Override
            protected void drawBackground(Matrix tform)
            {
                super.drawBackground(tform);
                String str = "Main Menu";
                float xScale = 2f / 40f;
                Text.draw(Matrix.scale(xScale, 2f / 40f, 1.0f)
                                .concat(Matrix.translate(-xScale
                                                                 / 2f
                                                                 * Text.sizeW(str)
                                                                 / Text.sizeW("A"),
                                                         0.7f,
                                                         0))
                                .concat(tform),
                          Color.RGB(0, 0, 0),
                          str);
            }

            {
                add(new TextMenuItem("New Game",
                                     Color.RGB(0f, 0f, 0f),
                                     this.getBackgroundColor(),
                                     Color.RGB(0f, 0f, 0f),
                                     Color.RGB(0.0f, 0.0f, 1.0f),
                                     this)
                {
                    @Override
                    public void onMouseOver(float mouseX, float mouseY)
                    {
                        select();
                    }

                    @Override
                    public void onClick(float mouseX, float mouseY)
                    {
                        if(didLoad)
                        {
                            saveAll();
                            internalSaveAll();
                        }
                        generateGame();
                        this.container.close();
                    }
                });
                add(new TextMenuItem("Load Game",
                                     Color.RGB(0f, 0f, 0f),
                                     this.getBackgroundColor(),
                                     Color.RGB(0f, 0f, 0f),
                                     Color.RGB(0.0f, 0.0f, 1.0f),
                                     this)
                {
                    @Override
                    public void onMouseOver(float mouseX, float mouseY)
                    {
                        select();
                    }

                    @Override
                    public void onClick(float mouseX, float mouseY)
                    {
                        if(didLoad)
                        {
                            saveAll();
                            internalSaveAll();
                        }
                        didLoad = false;
                        loadAll();
                        if(didLoad)
                            this.container.close();
                    }
                });
                if(didLoad)
                {
                    add(new TextMenuItem("Save Game",
                                         Color.RGB(0.0f, 0.0f, 0.0f),
                                         this.getBackgroundColor(),
                                         Color.RGB(0.0f, 0.0f, 0.0f),
                                         Color.RGB(0.0f, 0.0f, 1.0f),
                                         this)
                    {
                        @Override
                        public void onMouseOver(float mouseX, float mouseY)
                        {
                            select();
                        }

                        @Override
                        public void onClick(float mouseX, float mouseY)
                        {
                            saveAll();
                        }
                    });
                    add(new SpacerMenuItem(Color.V(0), this));
                    add(new TextMenuItem("Return To Game",
                                         Color.RGB(0.0f, 0.0f, 0.0f),
                                         this.getBackgroundColor(),
                                         Color.RGB(0.0f, 0.0f, 0.0f),
                                         Color.RGB(0.0f, 0.0f, 1.0f),
                                         this)
                    {
                        @Override
                        public void onMouseOver(float mouseX, float mouseY)
                        {
                            select();
                        }

                        @Override
                        public void onClick(float mouseX, float mouseY)
                        {
                            this.container.close();
                        }
                    });
                }
                add(new SpacerMenuItem(Color.V(0), this));
                add(new TextMenuItem("Quit Game",
                                     Color.RGB(0.0f, 0.0f, 0.0f),
                                     this.getBackgroundColor(),
                                     Color.RGB(0.0f, 0.0f, 0.0f),
                                     Color.RGB(0.0f, 0.0f, 1.0f),
                                     this)
                {
                    @Override
                    public void onMouseOver(float mouseX, float mouseY)
                    {
                        select();
                    }

                    @Override
                    public void onClick(float mouseX, float mouseY)
                    {
                        if(Main.didLoad)
                        {
                            saveAll();
                            internalSaveAll();
                        }
                        AL.destroy();
                        Display.destroy();
                        System.exit(0);
                    }
                });
                add(new TextMenuItem("Options",
                                     Color.RGB(0.0f, 0.0f, 0.0f),
                                     this.getBackgroundColor(),
                                     Color.RGB(0.0f, 0.0f, 0.0f),
                                     Color.RGB(0.0f, 0.0f, 1.0f),
                                     this)
                {
                    @Override
                    public void onMouseOver(float mouseX, float mouseY)
                    {
                        select();
                    }

                    @Override
                    public void onClick(float mouseX, float mouseY)
                    {
                        runOptionsMenu();
                    }
                });
            }
        });
    }

    /** @param args
     *            command line arguments */
    public static void main(String[] args)
    {
        for(int i = 0; i < args.length; i++)
        {
            if(args[i].equals("--debug"))
                DEBUG = true;
        }
        init();
        try
        {
            DisplayMode[] modes = Display.getAvailableDisplayModes();
            int closestindex = -1;
            for(int i = 0; i < modes.length; i++)
            {
                if(closestindex == -1)
                    closestindex = 0;
                else if(getModeDist(modes[closestindex]) > getModeDist(modes[i]))
                    closestindex = i;
            }
            Display.setDisplayMode(modes[closestindex]);
            ScreenXRes = modes[closestindex].getWidth();
            ScreenYRes = modes[closestindex].getHeight();
            aspectRatio = (float)ScreenXRes / ScreenYRes;
            Display.setTitle("Voxels " + Version);
            Display.create();
            Mouse.create();
            Keyboard.create();
            Keyboard.enableRepeatEvents(true);
            if(!AL.isCreated())
                AL.create();
        }
        catch(LWJGLException e)
        {
            e.printStackTrace();
            System.exit(1);
        }
        isFullscreen = false;
        boolean done = false;
        lastFrameStartTime = Timer();
        curTime = Timer();
        runMainMenu();
        if(!didLoad)
        {
            AL.destroy();
            Display.destroy();
            System.exit(0);
        }
        while(!done)
        {
            setFullscreen(isFullscreen);
            renderFrame();
            Display.update();
            world.generateChunks();
            {
                while(Mouse.next())
                {
                    players.handleMouseUpDown(new MouseEvent());
                }
                while(Keyboard.next())
                {
                    KeyboardEvent event = new KeyboardEvent();
                    if(event.isDown && event.key == KEY_F4
                            && isKeyDown(KEY_ALT))
                    {
                        done = true;
                        continue;
                    }
                    if(event.isDown && event.key == KEY_F11)
                    {
                        isFullscreen = !isFullscreen;
                        continue;
                    }
                    if(event.isDown && event.key == KEY_P)
                    {
                        runMainMenu();
                        continue;
                    }
                    players.handleKeyboardEvent(event);
                }
                switch(players.handleMouseMove(Mouse.getX(),
                                               Display.getHeight()
                                                       - Mouse.getY() - 1,
                                               Mouse.isButtonDown(0)))
                {
                case Grabbed:
                    if(!Mouse.isGrabbed())
                    {
                        int x = Mouse.getX(), y = Mouse.getY();
                        Mouse.setGrabbed(true);
                        Mouse.setCursorPosition(x, y);
                    }
                    break;
                case GrabbedAndCentered:
                    if(!Mouse.isGrabbed())
                        Mouse.setGrabbed(true);
                    Mouse.setCursorPosition(ScreenXRes / 2, Display.getHeight()
                            - ScreenYRes / 2 - 1);
                    break;
                default:
                    if(Mouse.isGrabbed())
                    {
                        int x = Mouse.getX(), y = Mouse.getY();
                        Mouse.setGrabbed(false);
                        Mouse.setCursorPosition(x, y);
                    }
                    break;
                }
                players.move();
            }
            world.move();
            Display.sync(60);
            curTime = Timer();
            frameDuration = curTime - lastFrameStartTime;
            internalSaveAll();
            lastFrameStartTime = curTime;
            if(Display.isCloseRequested())
                done = true;
        }
        saveAll(); // signal to save
        internalSaveAll(); // run save
        AL.destroy();
        Display.destroy();
        System.exit(0);
    }

    static boolean isLoading = false;

    private static JFileChooser makeFileChooser()
    {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileFilter()
        {
            @Override
            public String getDescription()
            {
                return "Voxels World";
            }

            @Override
            public boolean accept(File f)
            {
                if(f.exists())
                {
                    if(f.isDirectory())
                        return true;
                    if(!f.isFile() || !f.canRead())
                        return false;
                    if(!isLoading && !f.canWrite())
                        return false;
                }
                if(f.getName().toLowerCase().endsWith(".vw"))
                    return true;
                return false;
            }
        });
        fileChooser.removeChoosableFileFilter(fileChooser.getAcceptAllFileFilter());
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        return fileChooser;
    }

    private static File saveFile = null;
    private static JFileChooser fileChooser = makeFileChooser();
    private static JProgressBar progressBar = new JProgressBar(0, 10000);
    static JLabel progressLabel = new JLabel("");

    private static JDialog genProgressDialog()
    {
        progressLabel.setHorizontalAlignment(SwingConstants.CENTER);
        JDialog progressDialog = new JDialog((JWindow)null, "Voxels " + Version);
        progressDialog.setLayout(new GridLayout(2, 1));
        progressDialog.add(progressLabel);
        progressDialog.add(progressBar);
        progressDialog.setSize(300, 75);
        return progressDialog;
    }

    static JDialog progressDialog = genProgressDialog();
    static String progressLabelText = "";

    private static void showProgressDialog(String labelText)
    {
        topOfProgressStack = null;
        progressLabelText = labelText;
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                progressLabel.setText(progressLabelText);
                progressDialog.setVisible(true);
            }
        });
    }

    private static void hideProgressDialog()
    {
        topOfProgressStack = null;
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                progressDialog.setVisible(false);
            }
        });
    }

    private static class SetProgressRunnable implements Runnable
    {
        private float progress;
        private JProgressBar progressBar;

        public SetProgressRunnable(float progress, JProgressBar progressBar)
        {
            this.progress = progress;
            this.progressBar = progressBar;
        }

        @Override
        public void run()
        {
            this.progressBar.setValue(Math.round(10000 * this.progress));
        }
    }

    private static void internalSetProgress(float progress)
    {
        try
        {
            SwingUtilities.invokeAndWait(new SetProgressRunnable(progress,
                                                                 progressBar));
        }
        catch(InterruptedException e)
        {
        }
        catch(InvocationTargetException e)
        {
        }
    }

    private static class ProgressStackNode
    {
        public float scale, offset;
        public ProgressStackNode next;

        public ProgressStackNode()
        {
        }
    }

    private static ProgressStackNode topOfProgressStack = null;

    private static float getProgressOffset()
    {
        if(topOfProgressStack != null)
            return topOfProgressStack.offset;
        return 0.0f;
    }

    private static float getProgressScale()
    {
        if(topOfProgressStack != null)
            return topOfProgressStack.scale;
        return 1.0f;
    }

    static void pushProgress(float offset, float scale)
    {
        ProgressStackNode n = new ProgressStackNode();
        n.next = topOfProgressStack;
        n.scale = scale * getProgressScale();
        n.offset = getProgressOffset() + offset * getProgressScale();
        topOfProgressStack = n;
    }

    private static double lastSetProgressTime = -1;

    static void setProgress(float progress)
    {
        final double updatePeriod = 0.1;
        final double curTime = Timer();
        if(curTime - lastSetProgressTime < updatePeriod)
            return;
        lastSetProgressTime = curTime;
        float finalProgress = progress * getProgressScale()
                + getProgressOffset();
        internalSetProgress(Math.max(0, Math.min(1, finalProgress)));
    }

    static void popProgress()
    {
        if(topOfProgressStack != null)
            topOfProgressStack = topOfProgressStack.next;
    }

    private static boolean getSaveFile()
    {
        if(Display.isFullscreen())
        {
            try
            {
                Display.setFullscreen(false);
            }
            catch(LWJGLException e)
            {
                e.printStackTrace();
            }
        }
        if(Mouse.isGrabbed())
            Mouse.setGrabbed(false);
        isLoading = false;
        if(JFileChooser.APPROVE_OPTION == fileChooser.showSaveDialog(null))
        {
            saveFile = fileChooser.getSelectedFile();
            if(!saveFile.getName().toLowerCase().endsWith(".vw"))
                saveFile = new File(saveFile.getParentFile(),
                                    saveFile.getName() + ".vw");
            return true;
        }
        return false;
    }

    private static boolean needSave = false; // true if saveAll() has been
                                             // called

    /** saves everything */
    public static void saveAll()
    {
        needSave = true;
    }

    private static void internalSaveAll()
    {
        if(!needSave)
            return;
        needSave = false;
        if(saveFile == null)
        {
            if(!getSaveFile())
                return;
        }
        showProgressDialog("Saving...");
        OutputStream fos = null;
        boolean needHide = true;
        try
        {
            fos = new BufferedOutputStream(new FileOutputStream(saveFile));
            DataOutputStream dos = new DataOutputStream(fos);
            World.write(dos);
            players.write(dos);
            dos.close();
            hideProgressDialog();
            needHide = false;
        }
        catch(IOException e)
        {
            hideProgressDialog();
            needHide = false;
            JOptionPane.showMessageDialog(null,
                                          "Can't save : " + e.getMessage(),
                                          "Voxels",
                                          JOptionPane.WARNING_MESSAGE);
        }
        finally
        {
            if(fos != null)
            {
                try
                {
                    fos.close();
                }
                catch(IOException e)
                {
                    System.err.println(e.getMessage());
                }
            }
            if(needHide)
                hideProgressDialog();
        }
    }

    private static boolean getLoadFile()
    {
        saveFile = null;
        isLoading = true;
        if(JFileChooser.APPROVE_OPTION == fileChooser.showOpenDialog(null))
        {
            saveFile = fileChooser.getSelectedFile();
            return true;
        }
        return false;
    }

    private static void init()
    {
        showProgressDialog("Initializing...");
        Entity.init();
        hideProgressDialog();
    }

    private static boolean didLoad = false;

    private static boolean loadAll()
    {
        boolean done = false;
        boolean retval = false;
        while(!done)
        {
            if(!getLoadFile())
                return false;
            showProgressDialog("Loading...");
            InputStream fis = null;
            boolean needHide = true;
            try
            {
                fis = new BufferedInputStream(new FileInputStream(saveFile));
                DataInputStream dis = new DataInputStream(fis);
                World.read(dis);
                PlayerList.read(dis);
                dis.close();
                hideProgressDialog();
                needHide = false;
                done = true;
                didLoad = true;
                retval = true;
            }
            catch(EOFException e)
            {
                hideProgressDialog();
                needHide = false;
                JOptionPane.showMessageDialog(null,
                                              "Can't load : unexpected EOF",
                                              "Voxels",
                                              JOptionPane.ERROR_MESSAGE);
            }
            catch(IOException e)
            {
                hideProgressDialog();
                needHide = false;
                JOptionPane.showMessageDialog(null,
                                              "Can't load : " + e.getMessage(),
                                              "Voxels",
                                              JOptionPane.ERROR_MESSAGE);
            }
            finally
            {
                if(fis != null)
                {
                    try
                    {
                        fis.close();
                    }
                    catch(IOException e)
                    {
                        System.err.println(e.getMessage());
                    }
                }
                if(needHide)
                    hideProgressDialog();
            }
        }
        return retval;
    }

    private Main()
    {
    }

    /** @param filename
     *            the file to open
     * @return the created <code>InputStream</code>
     * @throws FileNotFoundException
     *             if the file couldn't be opened */
    public static InputStream
        getInputStream(String filename) throws FileNotFoundException
    {
        InputStream in = Main.class.getResourceAsStream(File.separator + "res"
                + File.separator + filename);
        if(in == null)
        {
            in = new FileInputStream("res" + File.separator + filename);
        }
        return in;
    }

    /** load a audio file
     * 
     * @param filename
     *            the file to load
     * @return the loaded <code>Audio</code> or <code>null</code> */
    @SuppressWarnings("resource")
    public static Audio loadAudio(String filename)
    {
        InputStream in = null;
        Audio retval = null;
        try
        {
            in = getInputStream(filename);
            if(in == null)
                return null;
            retval = AudioLoader.getAudio("OGG", in);
        }
        catch(IOException e)
        {
        }
        finally
        {
            try
            {
                if(in != null)
                    in.close();
            }
            catch(IOException e)
            {
            }
        }
        return retval;
    }

    static void play(Audio a)
    {
        if(a != null)
            a.playAsSoundEffect(1.0f, 1.0f, false);
    }

    static Audio clickAudio = loadAudio("click.ogg");
    static Audio popAudio = loadAudio("pop.ogg");
    static Audio destructAudio = loadAudio("destruct.ogg");
}
