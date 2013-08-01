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


/** @author jacob */
public final class Color
{
    /** red intensity */
    public byte r;
    /** green intensity */
    public byte g;
    /** blue intensity */
    public byte b;
    /** opacity */
    public byte a;

    /** @param nr
     *            red intensity
     * @param ng
     *            green intensity
     * @param nb
     *            blue intensity
     * @param na
     *            transparency */
    public Color(byte nr, byte ng, byte nb, byte na)
    {
        this.r = nr;
        this.g = ng;
        this.b = nb;
        this.a = na;
    }

    /** @param nr
     *            red intensity
     * @param ng
     *            green intensity
     * @param nb
     *            blue intensity */
    public Color(byte nr, byte ng, byte nb)
    {
        this(nr, ng, nb, (byte)0);
    }

    /** @param nr
     *            red intensity
     * @param ng
     *            green intensity
     * @param nb
     *            blue intensity
     * @param na
     *            transparency */
    public Color(int nr, int ng, int nb, int na)
    {
        this((byte)Math.max(Math.min(nr, 0xFF), 0),
             (byte)Math.max(Math.min(ng, 0xFF), 0),
             (byte)Math.max(Math.min(nb, 0xFF), 0),
             (byte)Math.max(Math.min(na, 0xFF), 0));
    }

    /** @param nr
     *            red intensity
     * @param ng
     *            green intensity
     * @param nb
     *            blue intensity */
    public Color(int nr, int ng, int nb)
    {
        this((byte)Math.max(Math.min(nr, 0xFF), 0),
             (byte)Math.max(Math.min(ng, 0xFF), 0),
             (byte)Math.max(Math.min(nb, 0xFF), 0),
             (byte)0);
    }

    /** @param rt
     *            the color to copy */
    public Color(Color rt)
    {
        this(rt.r, rt.g, rt.b, rt.a);
    }

    /** @param r
     *            red intensity
     * @param g
     *            green intensity
     * @param b
     *            blue intensity
     * @return the created <code>Color</code> */
    public static Color RGB(int r, int g, int b)
    {
        return new Color(r, g, b);
    }

    /** @param r
     *            red intensity
     * @param g
     *            green intensity
     * @param b
     *            blue intensity
     * @param a
     *            transparency
     * @return the created <code>Color</code> */
    public static Color RGBA(int r, int g, int b, int a)
    {
        return new Color(r, g, b, a);
    }

    /** @param r
     *            red intensity
     * @param g
     *            green intensity
     * @param b
     *            blue intensity
     * @return the created <code>Color</code> */
    public static Color RGB(float r, float g, float b)
    {
        return RGB((int)Math.floor(r * 256.0),
                   (int)Math.floor(g * 256.0),
                   (int)Math.floor(b * 256.0));
    }

    /** @param r
     *            red intensity
     * @param g
     *            green intensity
     * @param b
     *            blue intensity
     * @param a
     *            transparency
     * @return the created <code>Color</code> */
    public static Color RGBA(float r, float g, float b, float a)
    {
        return RGBA((int)Math.floor(r * 256.0),
                    (int)Math.floor(g * 256.0),
                    (int)Math.floor(b * 256.0),
                    (int)Math.floor(a * 256.0));
    }

    /** @param v
     *            intensity
     * @return the created <code>Color</code> */
    public static Color V(float v)
    {
        return RGB(v, v, v);
    }

    /** @param v
     *            intensity
     * @param a
     *            transparency
     * @return the created <code>Color</code> */
    public static Color VA(float v, float a)
    {
        return RGBA(v, v, v, a);
    }

    /** @param c
     *            color
     * @return red intensity */
    public static int GetRValue(Color c)
    {
        return c.r & 0xFF;
    }

    /** @param c
     *            color
     * @return green intensity */
    public static int GetGValue(Color c)
    {
        return c.g & 0xFF;
    }

    /** @param c
     *            color
     * @return blue intensity */
    public static int GetBValue(Color c)
    {
        return c.b & 0xFF;
    }

    /** @param c
     *            color
     * @return transparency */
    public static int GetAValue(Color c)
    {
        return c.a & 0xFF;
    }

    /** call glColor3* <BR/>
     * ignores <code>c</code>'s transparency
     * 
     * @param c
     *            color */
    public static void glColor(Color c)
    {
        Main.opengl.glColor4f(GetRValue(c) / 255.0f,
                              GetGValue(c) / 255.0f,
                              GetBValue(c) / 255.0f,
                              1.0f);
    }

    /** call glClearColor <BR/>
     * ignores <code>c</code>'s transparency
     * 
     * @param c
     *            color */
    public static void glClearColor(Color c)
    {
        Main.opengl.glClearColor(GetRValue(c) / 255.0f,
                                 GetGValue(c) / 255.0f,
                                 GetBValue(c) / 255.0f,
                                 0.0f);
    }
}
