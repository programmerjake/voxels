/**
 * this file is part of voxels
 * 
 * voxels is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * voxels is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with voxels.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.voxels.platform;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.voxels.MenuScreen;

/** @author jacob */
public interface Platform extends Closeable
{
    /** @return if the platform uses a touch screen */
    public boolean isTouchScreen();

    /** @param size
     *            size
     * @return a new byte buffer */
    public ByteBuffer createByteBuffer(int size);

    /** @param size
     *            size
     * @return a new float buffer */
    public FloatBuffer createFloatBuffer(int size);

    /** @param icon
     *            the icon */
    public void setIcon(ByteBuffer[] icon);

    /** @return the user settings directory */
    public File getUserSettingsDir();

    /** @return the OpenGL */
    public OpenGL getOpenGL();

    /** @return the mouse */
    public Mouse getMouse();

    /** @return the keyboard or null */
    public Keyboard getKeyboard();

    /** @param visible
     *            if the mouse is visible */
    public void setMouseVisible(boolean visible);

    /** @return if the mouse is visible */
    public boolean isMouseVisible();

    /** @return the change screen resolution menu or null */
    public MenuScreen getChangeScreenResolutionMenu();

    /** @return if this has a change screen resolution menu */
    public boolean hasChangeScreenResolutionMenu();

    /** @return if this is fullscreen */
    public boolean isFullscreen();

    /** @param fullscreen
     *            if this is fullscreen */
    public void setFullscreen(boolean fullscreen);

    /** @param vsync
     *            if VSync is enabled */
    public void setVSyncEnabled(boolean vsync);

    /**
     * 
     */
    public void update();

    /**
     * 
     */
    public void waitForNextFrame();

    /** @return if the close button was pressed */
    public boolean isCloseRequested();

    /**
     * 
     */
    public void init();

    @Override
    public void close();

    /** @param name
     *            the file name
     * @return the InputStream
     * @throws FileNotFoundException
     *             if the file couldn't be found */
    public InputStream
        getFileInputStream(String name) throws FileNotFoundException;

    /** @param is
     *            the InputStream
     * @return the loaded Audio or null
     * @throws IOException
     *             the IOException */
    public Audio loadAudio(InputStream is) throws IOException;

    /** @param name
     *            the file name
     * @return the loaded Audio or null
     * @throws IOException
     *             the IOException */
    public Audio loadAudioStream(String name) throws IOException;

    /** @return the current time in seconds */
    public double Timer();

    /** @return the screen width */
    public int getScreenWidth();

    /** @return the screen height */
    public int getScreenHeight();
}
