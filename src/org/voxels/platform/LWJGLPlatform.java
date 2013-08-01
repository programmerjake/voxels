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
package org.voxels.platform;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.openal.AL;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.newdawn.slick.openal.AudioLoader;
import org.voxels.*;

/** @author jacob */
public class LWJGLPlatform implements Platform
{
    @Override
    public boolean isTouchScreen()
    {
        return false;
    }

    @Override
    public ByteBuffer createByteBuffer(final int size)
    {
        return org.lwjgl.BufferUtils.createByteBuffer(size);
    }

    @Override
    public FloatBuffer createFloatBuffer(final int size)
    {
        return org.lwjgl.BufferUtils.createFloatBuffer(size);
    }

    @Override
    public void setIcon(final ByteBuffer[] icon)
    {
        Display.setIcon(icon);
    }

    /**
     * 
     */
    public LWJGLPlatform()
    {
    }

    @Override
    public File getUserSettingsDir()
    {
        String homeDir = System.getProperty("user.home");
        return new File(homeDir);
    }

    private OpenGL opengl = new LWJGLOpenGLAdapter();

    @Override
    public OpenGL getOpenGL()
    {
        return this.opengl;
    }

    private Mouse mouse = new LWJGLMouseAdapter();

    @Override
    public Mouse getMouse()
    {
        return this.mouse;
    }

    private Keyboard keyboard = new LWJGLKeyboardAdapter();

    @Override
    public Keyboard getKeyboard()
    {
        return this.keyboard;
    }

    @Override
    public void setMouseVisible(final boolean visible)
    {
        org.lwjgl.input.Mouse.setGrabbed(!visible);
    }

    @Override
    public boolean isMouseVisible()
    {
        return !org.lwjgl.input.Mouse.isGrabbed();
    }

    @Override
    public MenuScreen getChangeScreenResolutionMenu()
    {
        return new MenuScreen(Color.V(0.75f))
        {
            public DisplayMode[] modes;
            private int selectedMode;

            @Override
            protected void drawBackground(final Matrix tform)
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

            public void setSelectedMode(final int selectedMode)
            {
                this.selectedMode = selectedMode;
                try
                {
                    Display.setDisplayMode(this.modes[this.selectedMode]);
                    Display.setVSyncEnabled(Main.isVSyncEnabled);
                }
                catch(LWJGLException e)
                {
                    StringWriter w = new StringWriter();
                    PrintWriter pw = new PrintWriter(w, true);
                    e.printStackTrace(pw);
                    Main.alert("Can't change display mode", w.toString());
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
                                           getBackgroundColor(),
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
                                     getBackgroundColor(),
                                     Color.RGB(0f, 0f, 0f),
                                     Color.RGB(0.0f, 0.0f, 1.0f),
                                     this)
                {
                    @Override
                    public void onMouseOver(final float mouseX,
                                            final float mouseY)
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
        };
    }

    @Override
    public boolean hasChangeScreenResolutionMenu()
    {
        return true;
    }

    @Override
    public boolean isFullscreen()
    {
        return Display.isFullscreen();
    }

    @Override
    public void setFullscreen(final boolean fullscreen)
    {
        try
        {
            Display.setFullscreen(fullscreen);
        }
        catch(LWJGLException e)
        {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void update()
    {
        Display.update();
    }

    @Override
    public void waitForNextFrame()
    {
        if(!Main.DEBUG)
            Display.sync(60);
    }

    @Override
    public boolean isCloseRequested()
    {
        return Display.isCloseRequested();
    }

    private static int getModeDist(final DisplayMode mode)
    {
        return Math.abs(mode.getWidth() - 640)
                + Math.abs(mode.getHeight() - 480);
    }

    @Override
    public void init()
    {
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
            Display.setTitle("Voxels " + Main.Version);
            Display.create();
            setVSyncEnabled(Main.isVSyncEnabled);
            org.lwjgl.input.Mouse.create();
            org.lwjgl.input.Keyboard.create();
            org.lwjgl.input.Keyboard.enableRepeatEvents(true);
            if(!AL.isCreated())
                AL.create();
        }
        catch(LWJGLException e)
        {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void close()
    {
        AL.destroy();
        Display.destroy();
        org.lwjgl.input.Mouse.destroy();
        org.lwjgl.input.Keyboard.destroy();
    }

    @Override
    public void setVSyncEnabled(final boolean vsync)
    {
        Display.setVSyncEnabled(vsync);
    }

    @Override
    public InputStream
        getFileInputStream(final String filename) throws FileNotFoundException
    {
        InputStream in = Main.class.getResourceAsStream(File.separator + "res"
                + File.separator + filename);
        if(in == null)
        {
            in = new FileInputStream("res" + File.separator + filename);
        }
        return in;
    }

    @Override
    public Audio loadAudio(final InputStream in) throws IOException
    {
        if(in == null)
            return null;
        org.newdawn.slick.openal.Audio retval = AudioLoader.getAudio("OGG", in);
        if(retval == null)
            return null;
        return new LWJGLAudioAdapter(retval);
    }

    @Override
    public double Timer()
    {
        return (double)Sys.getTime() / Sys.getTimerResolution();
    }

    @Override
    public int getScreenWidth()
    {
        return Display.getWidth();
    }

    @Override
    public int getScreenHeight()
    {
        return Display.getHeight();
    }
}
