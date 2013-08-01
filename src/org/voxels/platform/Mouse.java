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

/** @author jacob */
public interface Mouse
{
    /**
     * 
     */
    public static final int BUTTON_LEFT = 0;
    /**
     * 
     */
    public static final int BUTTON_RIGHT = 1;
    /**
     * 
     */
    public static final int BUTTON_MIDDLE = 2;

    /** @return the current event's x */
    public float getEventX();

    /** @return the current event's y */
    public float getEventY();

    /** @return the current event's button index */
    public int getEventButtonIndex();

    /** @return if the current event's button is pressed */
    public boolean isEventButtonDown();

    /** @return the current event's wheel rotation amount */
    public int getEventDWheel();

    /** @return the mouse's x coordinate */
    public float getX();

    /** @return the mouse's y coordinate */
    public float getY();

    /** @param bIndex
     *            the button index
     * @return if the button is pressed */
    public boolean isButtonDown(int bIndex);

    /** @return if there is a next event */
    public boolean nextEvent();

    /** @param x
     *            new mouse x
     * @param y
     *            new mouse y */
    public void setPosition(float x, float y);

    /** @return the drag threshold */
    public float getDragThreshold();
}
