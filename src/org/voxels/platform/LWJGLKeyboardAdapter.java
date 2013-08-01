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
public class LWJGLKeyboardAdapter implements Keyboard
{
    @Override
    public boolean isKeyDown(int key)
    {
        if(key == KEY_SHIFT)
        {
            return org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT)
                    || org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RSHIFT);
        }
        if(key == KEY_ALT)
        {
            return org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LMENU)
                    || org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RMENU);
        }
        if(key == KEY_CTRL)
        {
            return org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LCONTROL)
                    || org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RCONTROL);
        }
        int newKey = -1;
        switch(key)
        {
        case KEY_A:
            newKey = org.lwjgl.input.Keyboard.KEY_A;
            break;
        case KEY_B:
            newKey = org.lwjgl.input.Keyboard.KEY_B;
            break;
        case KEY_C:
            newKey = org.lwjgl.input.Keyboard.KEY_C;
            break;
        case KEY_D:
            newKey = org.lwjgl.input.Keyboard.KEY_D;
            break;
        case KEY_E:
            newKey = org.lwjgl.input.Keyboard.KEY_E;
            break;
        case KEY_F:
            newKey = org.lwjgl.input.Keyboard.KEY_F;
            break;
        case KEY_G:
            newKey = org.lwjgl.input.Keyboard.KEY_G;
            break;
        case KEY_H:
            newKey = org.lwjgl.input.Keyboard.KEY_H;
            break;
        case KEY_I:
            newKey = org.lwjgl.input.Keyboard.KEY_I;
            break;
        case KEY_J:
            newKey = org.lwjgl.input.Keyboard.KEY_J;
            break;
        case KEY_K:
            newKey = org.lwjgl.input.Keyboard.KEY_K;
            break;
        case KEY_L:
            newKey = org.lwjgl.input.Keyboard.KEY_L;
            break;
        case KEY_M:
            newKey = org.lwjgl.input.Keyboard.KEY_M;
            break;
        case KEY_N:
            newKey = org.lwjgl.input.Keyboard.KEY_N;
            break;
        case KEY_O:
            newKey = org.lwjgl.input.Keyboard.KEY_O;
            break;
        case KEY_P:
            newKey = org.lwjgl.input.Keyboard.KEY_P;
            break;
        case KEY_Q:
            newKey = org.lwjgl.input.Keyboard.KEY_Q;
            break;
        case KEY_R:
            newKey = org.lwjgl.input.Keyboard.KEY_R;
            break;
        case KEY_S:
            newKey = org.lwjgl.input.Keyboard.KEY_S;
            break;
        case KEY_T:
            newKey = org.lwjgl.input.Keyboard.KEY_T;
            break;
        case KEY_U:
            newKey = org.lwjgl.input.Keyboard.KEY_U;
            break;
        case KEY_V:
            newKey = org.lwjgl.input.Keyboard.KEY_V;
            break;
        case KEY_W:
            newKey = org.lwjgl.input.Keyboard.KEY_W;
            break;
        case KEY_X:
            newKey = org.lwjgl.input.Keyboard.KEY_X;
            break;
        case KEY_Y:
            newKey = org.lwjgl.input.Keyboard.KEY_Y;
            break;
        case KEY_Z:
            newKey = org.lwjgl.input.Keyboard.KEY_Z;
            break;
        case KEY_F1:
            newKey = org.lwjgl.input.Keyboard.KEY_F1;
            break;
        case KEY_F2:
            newKey = org.lwjgl.input.Keyboard.KEY_F2;
            break;
        case KEY_F3:
            newKey = org.lwjgl.input.Keyboard.KEY_F3;
            break;
        case KEY_F4:
            newKey = org.lwjgl.input.Keyboard.KEY_F4;
            break;
        case KEY_F5:
            newKey = org.lwjgl.input.Keyboard.KEY_F5;
            break;
        case KEY_F6:
            newKey = org.lwjgl.input.Keyboard.KEY_F6;
            break;
        case KEY_F7:
            newKey = org.lwjgl.input.Keyboard.KEY_F7;
            break;
        case KEY_F8:
            newKey = org.lwjgl.input.Keyboard.KEY_F8;
            break;
        case KEY_F9:
            newKey = org.lwjgl.input.Keyboard.KEY_F9;
            break;
        case KEY_F10:
            newKey = org.lwjgl.input.Keyboard.KEY_F10;
            break;
        case KEY_F11:
            newKey = org.lwjgl.input.Keyboard.KEY_F11;
            break;
        case KEY_F12:
            newKey = org.lwjgl.input.Keyboard.KEY_F12;
            break;
        case KEY_0:
            newKey = org.lwjgl.input.Keyboard.KEY_0;
            break;
        case KEY_1:
            newKey = org.lwjgl.input.Keyboard.KEY_1;
            break;
        case KEY_2:
            newKey = org.lwjgl.input.Keyboard.KEY_2;
            break;
        case KEY_3:
            newKey = org.lwjgl.input.Keyboard.KEY_3;
            break;
        case KEY_4:
            newKey = org.lwjgl.input.Keyboard.KEY_4;
            break;
        case KEY_5:
            newKey = org.lwjgl.input.Keyboard.KEY_5;
            break;
        case KEY_6:
            newKey = org.lwjgl.input.Keyboard.KEY_6;
            break;
        case KEY_7:
            newKey = org.lwjgl.input.Keyboard.KEY_7;
            break;
        case KEY_8:
            newKey = org.lwjgl.input.Keyboard.KEY_8;
            break;
        case KEY_9:
            newKey = org.lwjgl.input.Keyboard.KEY_9;
            break;
        case KEY_APOSTROPHE:
            newKey = org.lwjgl.input.Keyboard.KEY_APOSTROPHE;
            break;
        case KEY_UP:
            newKey = org.lwjgl.input.Keyboard.KEY_UP;
            break;
        case KEY_DOWN:
            newKey = org.lwjgl.input.Keyboard.KEY_DOWN;
            break;
        case KEY_LEFT:
            newKey = org.lwjgl.input.Keyboard.KEY_LEFT;
            break;
        case KEY_RIGHT:
            newKey = org.lwjgl.input.Keyboard.KEY_RIGHT;
            break;
        case KEY_LSHIFT:
            newKey = org.lwjgl.input.Keyboard.KEY_LSHIFT;
            break;
        case KEY_RSHIFT:
            newKey = org.lwjgl.input.Keyboard.KEY_RSHIFT;
            break;
        case KEY_LCTRL:
            newKey = org.lwjgl.input.Keyboard.KEY_LCONTROL;
            break;
        case KEY_RCTRL:
            newKey = org.lwjgl.input.Keyboard.KEY_RCONTROL;
            break;
        case KEY_LALT:
            newKey = org.lwjgl.input.Keyboard.KEY_LMENU;
            break;
        case KEY_RALT:
            newKey = org.lwjgl.input.Keyboard.KEY_RMENU;
            break;
        case KEY_RETURN:
            newKey = org.lwjgl.input.Keyboard.KEY_RETURN;
            break;
        case KEY_ESCAPE:
            newKey = org.lwjgl.input.Keyboard.KEY_ESCAPE;
            break;
        case KEY_SPACE:
            newKey = org.lwjgl.input.Keyboard.KEY_SPACE;
            break;
        case KEY_DELETE:
            newKey = org.lwjgl.input.Keyboard.KEY_DELETE;
            break;
        }
        if(newKey == -1)
            return false;
        return org.lwjgl.input.Keyboard.isKeyDown(newKey);
    }

    @Override
    public int getEventKey()
    {
        switch(org.lwjgl.input.Keyboard.getEventKey())
        {
        case org.lwjgl.input.Keyboard.KEY_A:
            return KEY_A;
        case org.lwjgl.input.Keyboard.KEY_B:
            return KEY_B;
        case org.lwjgl.input.Keyboard.KEY_C:
            return KEY_C;
        case org.lwjgl.input.Keyboard.KEY_D:
            return KEY_D;
        case org.lwjgl.input.Keyboard.KEY_E:
            return KEY_E;
        case org.lwjgl.input.Keyboard.KEY_F:
            return KEY_F;
        case org.lwjgl.input.Keyboard.KEY_G:
            return KEY_G;
        case org.lwjgl.input.Keyboard.KEY_H:
            return KEY_H;
        case org.lwjgl.input.Keyboard.KEY_I:
            return KEY_I;
        case org.lwjgl.input.Keyboard.KEY_J:
            return KEY_J;
        case org.lwjgl.input.Keyboard.KEY_K:
            return KEY_K;
        case org.lwjgl.input.Keyboard.KEY_L:
            return KEY_L;
        case org.lwjgl.input.Keyboard.KEY_M:
            return KEY_M;
        case org.lwjgl.input.Keyboard.KEY_N:
            return KEY_N;
        case org.lwjgl.input.Keyboard.KEY_O:
            return KEY_O;
        case org.lwjgl.input.Keyboard.KEY_P:
            return KEY_P;
        case org.lwjgl.input.Keyboard.KEY_Q:
            return KEY_Q;
        case org.lwjgl.input.Keyboard.KEY_R:
            return KEY_R;
        case org.lwjgl.input.Keyboard.KEY_S:
            return KEY_S;
        case org.lwjgl.input.Keyboard.KEY_T:
            return KEY_T;
        case org.lwjgl.input.Keyboard.KEY_U:
            return KEY_U;
        case org.lwjgl.input.Keyboard.KEY_V:
            return KEY_V;
        case org.lwjgl.input.Keyboard.KEY_W:
            return KEY_W;
        case org.lwjgl.input.Keyboard.KEY_X:
            return KEY_X;
        case org.lwjgl.input.Keyboard.KEY_Y:
            return KEY_Y;
        case org.lwjgl.input.Keyboard.KEY_Z:
            return KEY_Z;
        case org.lwjgl.input.Keyboard.KEY_F1:
            return KEY_F1;
        case org.lwjgl.input.Keyboard.KEY_F2:
            return KEY_F2;
        case org.lwjgl.input.Keyboard.KEY_F3:
            return KEY_F3;
        case org.lwjgl.input.Keyboard.KEY_F4:
            return KEY_F4;
        case org.lwjgl.input.Keyboard.KEY_F5:
            return KEY_F5;
        case org.lwjgl.input.Keyboard.KEY_F6:
            return KEY_F6;
        case org.lwjgl.input.Keyboard.KEY_F7:
            return KEY_F7;
        case org.lwjgl.input.Keyboard.KEY_F8:
            return KEY_F8;
        case org.lwjgl.input.Keyboard.KEY_F9:
            return KEY_F9;
        case org.lwjgl.input.Keyboard.KEY_F10:
            return KEY_F10;
        case org.lwjgl.input.Keyboard.KEY_F11:
            return KEY_F11;
        case org.lwjgl.input.Keyboard.KEY_F12:
            return KEY_F12;
        case org.lwjgl.input.Keyboard.KEY_0:
            return KEY_0;
        case org.lwjgl.input.Keyboard.KEY_1:
            return KEY_1;
        case org.lwjgl.input.Keyboard.KEY_2:
            return KEY_2;
        case org.lwjgl.input.Keyboard.KEY_3:
            return KEY_3;
        case org.lwjgl.input.Keyboard.KEY_4:
            return KEY_4;
        case org.lwjgl.input.Keyboard.KEY_5:
            return KEY_5;
        case org.lwjgl.input.Keyboard.KEY_6:
            return KEY_6;
        case org.lwjgl.input.Keyboard.KEY_7:
            return KEY_7;
        case org.lwjgl.input.Keyboard.KEY_8:
            return KEY_8;
        case org.lwjgl.input.Keyboard.KEY_9:
            return KEY_9;
        case org.lwjgl.input.Keyboard.KEY_APOSTROPHE:
            return KEY_APOSTROPHE;
        case org.lwjgl.input.Keyboard.KEY_UP:
            return KEY_UP;
        case org.lwjgl.input.Keyboard.KEY_DOWN:
            return KEY_DOWN;
        case org.lwjgl.input.Keyboard.KEY_LEFT:
            return KEY_LEFT;
        case org.lwjgl.input.Keyboard.KEY_RIGHT:
            return KEY_RIGHT;
        case org.lwjgl.input.Keyboard.KEY_LSHIFT:
            return KEY_LSHIFT;
        case org.lwjgl.input.Keyboard.KEY_RSHIFT:
            return KEY_RSHIFT;
        case org.lwjgl.input.Keyboard.KEY_LCONTROL:
            return KEY_LCTRL;
        case org.lwjgl.input.Keyboard.KEY_RCONTROL:
            return KEY_RCTRL;
        case org.lwjgl.input.Keyboard.KEY_LMENU:
            return KEY_LALT;
        case org.lwjgl.input.Keyboard.KEY_RMENU:
            return KEY_RALT;
        case org.lwjgl.input.Keyboard.KEY_RETURN:
            return KEY_RETURN;
        case org.lwjgl.input.Keyboard.KEY_ESCAPE:
            return KEY_ESCAPE;
        case org.lwjgl.input.Keyboard.KEY_SPACE:
            return KEY_SPACE;
        case org.lwjgl.input.Keyboard.KEY_DELETE:
            return KEY_DELETE;
        }
        return KEY_NONE;
    }

    @Override
    public char getEventCharacter()
    {
        char retval = org.lwjgl.input.Keyboard.getEventCharacter();
        if(retval == org.lwjgl.input.Keyboard.CHAR_NONE)
            return CHAR_NONE;
        return retval;
    }

    @Override
    public boolean getEventKeyDown()
    {
        return org.lwjgl.input.Keyboard.getEventKeyState();
    }

    @Override
    public boolean isRepeatEvent()
    {
        return org.lwjgl.input.Keyboard.isRepeatEvent();
    }

    @Override
    public boolean nextEvent()
    {
        return org.lwjgl.input.Keyboard.next();
    }
}
