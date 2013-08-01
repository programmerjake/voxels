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
public interface Keyboard
{
    /***/
    public static final int KEY_A = 1;
    /***/
    public static final int KEY_B = 2;
    /***/
    public static final int KEY_C = 3;
    /***/
    public static final int KEY_D = 4;
    /***/
    public static final int KEY_E = 5;
    /***/
    public static final int KEY_F = 6;
    /***/
    public static final int KEY_G = 7;
    /***/
    public static final int KEY_H = 8;
    /***/
    public static final int KEY_I = 9;
    /***/
    public static final int KEY_J = 10;
    /***/
    public static final int KEY_K = 11;
    /***/
    public static final int KEY_L = 12;
    /***/
    public static final int KEY_M = 13;
    /***/
    public static final int KEY_N = 14;
    /***/
    public static final int KEY_O = 15;
    /***/
    public static final int KEY_P = 16;
    /***/
    public static final int KEY_Q = 17;
    /***/
    public static final int KEY_R = 18;
    /***/
    public static final int KEY_S = 19;
    /***/
    public static final int KEY_T = 20;
    /***/
    public static final int KEY_U = 21;
    /***/
    public static final int KEY_V = 22;
    /***/
    public static final int KEY_W = 23;
    /***/
    public static final int KEY_X = 24;
    /***/
    public static final int KEY_Y = 25;
    /***/
    public static final int KEY_Z = 26;
    /** the F1 key */
    public static final int KEY_F1 = 27;
    /** the F2 key */
    public static final int KEY_F2 = 28;
    /** the F3 key */
    public static final int KEY_F3 = 29;
    /** the F4 key */
    public static final int KEY_F4 = 30;
    /** the F5 key */
    public static final int KEY_F5 = 31;
    /** the F6 key */
    public static final int KEY_F6 = 32;
    /** the F7 key */
    public static final int KEY_F7 = 33;
    /** the F8 key */
    public static final int KEY_F8 = 34;
    /** the F9 key */
    public static final int KEY_F9 = 35;
    /** the F10 key */
    public static final int KEY_F10 = 36;
    /** the F11 key */
    public static final int KEY_F11 = 37;
    /** the F12 key */
    public static final int KEY_F12 = 38;
    /** the 0 key */
    public static final int KEY_0 = 39;
    /** the 1 key */
    public static final int KEY_1 = 40;
    /** the 2 key */
    public static final int KEY_2 = 41;
    /** the 3 key */
    public static final int KEY_3 = 42;
    /** the 4 key */
    public static final int KEY_4 = 43;
    /** the 5 key */
    public static final int KEY_5 = 44;
    /** the 6 key */
    public static final int KEY_6 = 45;
    /** the 7 key */
    public static final int KEY_7 = 46;
    /** the 8 key */
    public static final int KEY_8 = 47;
    /** the 9 key */
    public static final int KEY_9 = 48;
    /** the Apostrophe key */
    public static final int KEY_APOSTROPHE = 49;
    /** the up arrow key */
    public static final int KEY_UP = 50;
    /** the down arrow key */
    public static final int KEY_DOWN = 51;
    /** the left arrow key */
    public static final int KEY_LEFT = 52;
    /** the right arrow key */
    public static final int KEY_RIGHT = 53;
    /** the left shift key */
    public static final int KEY_LSHIFT = 54;
    /** the right shift key */
    public static final int KEY_RSHIFT = 55;
    /** the left control key */
    public static final int KEY_LCTRL = 56;
    /** the right control key */
    public static final int KEY_RCTRL = 57;
    /** the left alt key */
    public static final int KEY_LALT = 58;
    /** the right alt key */
    public static final int KEY_RALT = 59;
    /** the return (enter) key */
    public static final int KEY_RETURN = 60;
    /** the left or right shift key key */
    public static final int KEY_SHIFT = 61;
    /** the left or right Control key */
    public static final int KEY_CTRL = 62;
    /** the left or right Alt key */
    public static final int KEY_ALT = 63;
    /** the special value that means that no key corresponds to this character */
    public static final int KEY_NONE = 0;
    /** the Escape key */
    public static final int KEY_ESCAPE = 64;
    /** the Space key */
    public static final int KEY_SPACE = 65;
    /** the Delete key */
    public static final int KEY_DELETE = 66;
    /** the special value that means that no character was translated */
    public static final char CHAR_NONE = (char)0;

    /** @param key
     *            the key
     * @return if the key is down */
    public boolean isKeyDown(int key);

    /** @return the event's key */
    public int getEventKey();

    /** @return the event's character */
    public char getEventCharacter();

    /** @return if the event's key is pressed */
    public boolean getEventKeyDown();

    /** @return if the event is a repeat event */
    public boolean isRepeatEvent();

    /** @return if there is a next event */
    public boolean nextEvent();
}
