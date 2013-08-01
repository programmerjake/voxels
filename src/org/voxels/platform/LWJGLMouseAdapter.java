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

import org.lwjgl.opengl.Display;

/** @author jacob */
public class LWJGLMouseAdapter implements Mouse
{
    private boolean lastEventWasLeftButtonDown = false;
    private boolean isLeftTranslatedToRight = false;

    @Override
    public float getEventX()
    {
        return org.lwjgl.input.Mouse.getEventX();
    }

    @Override
    public float getEventY()
    {
        return Display.getHeight() - org.lwjgl.input.Mouse.getEventY() - 1;
    }

    @Override
    public int getEventButtonIndex()
    {
        int button = org.lwjgl.input.Mouse.getEventButton();
        if(button == BUTTON_LEFT && this.isLeftTranslatedToRight)
            return BUTTON_RIGHT;
        return button;
    }

    @Override
    public boolean isEventButtonDown()
    {
        return org.lwjgl.input.Mouse.getEventButtonState();
    }

    @Override
    public int getEventDWheel()
    {
        return org.lwjgl.input.Mouse.getEventDWheel();
    }

    @Override
    public float getX()
    {
        return org.lwjgl.input.Mouse.getX();
    }

    @Override
    public float getY()
    {
        return Display.getHeight() - org.lwjgl.input.Mouse.getY() - 1;
    }

    @Override
    public boolean isButtonDown(final int bIndex)
    {
        if(this.isLeftTranslatedToRight)
        {
            if(bIndex == BUTTON_RIGHT)
                return org.lwjgl.input.Mouse.isButtonDown(BUTTON_LEFT);
            if(bIndex == BUTTON_LEFT)
                return false;
        }
        return org.lwjgl.input.Mouse.isButtonDown(bIndex);
    }

    @Override
    public boolean nextEvent()
    {
        if(!org.lwjgl.input.Mouse.next())
        {
            return false;
        }
        this.isLeftTranslatedToRight = false;
        int button = org.lwjgl.input.Mouse.getEventButton();
        boolean isDown = org.lwjgl.input.Mouse.getEventButtonState();
        boolean eventWasLeftButtonDown = false;
        if(button == BUTTON_LEFT
                && (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LMENU) || org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LMENU))
                && (!this.lastEventWasLeftButtonDown || isDown))
            this.isLeftTranslatedToRight = true;
        else if(button == BUTTON_LEFT && isDown)
            eventWasLeftButtonDown = true;
        this.lastEventWasLeftButtonDown = eventWasLeftButtonDown;
        return true;
    }

    @Override
    public void setPosition(final float x, final float y)
    {
        org.lwjgl.input.Mouse.setCursorPosition(Math.round(x),
                                                Display.getHeight()
                                                        - Math.round(y) - 1);
    }

    @Override
    public float getDragThreshold()
    {
        return 4;
    }
}
