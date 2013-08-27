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
public final class Color implements Allocatable
{
    private static final Allocator<Color> allocator = new Allocator<Color>()
    {
        @Override
        protected Color allocateInternal()
        {
            return new Color();
        }
    };
    /** red intensity */
    public byte r;
    /** green intensity */
    public byte g;
    /** blue intensity */
    public byte b;
    /** opacity */
    public byte a;

    Color()
    {
    }

    /** @param nr
     *            red intensity
     * @param ng
     *            green intensity
     * @param nb
     *            blue intensity
     * @param na
     *            transparency
     * @return the new Color */
    public static Color allocate(final byte nr,
                                 final byte ng,
                                 final byte nb,
                                 final byte na)
    {
        Color retval = allocator.allocate();
        retval.r = nr;
        retval.g = ng;
        retval.b = nb;
        retval.a = na;
        return retval;
    }

    /** @param nr
     *            red intensity
     * @param ng
     *            green intensity
     * @param nb
     *            blue intensity
     * @return the new Color */
    public static Color allocate(final byte nr, final byte ng, final byte nb)
    {
        return allocate(nr, ng, nb, (byte)0);
    }

    /** @param nr
     *            red intensity
     * @param ng
     *            green intensity
     * @param nb
     *            blue intensity
     * @param na
     *            transparency
     * @return the new Color */
    public static Color allocate(final int nr,
                                 final int ng,
                                 final int nb,
                                 final int na)
    {
        return allocate((byte)Math.max(Math.min(nr, 0xFF), 0),
                        (byte)Math.max(Math.min(ng, 0xFF), 0),
                        (byte)Math.max(Math.min(nb, 0xFF), 0),
                        (byte)Math.max(Math.min(na, 0xFF), 0));
    }

    /** @param nr
     *            red intensity
     * @param ng
     *            green intensity
     * @param nb
     *            blue intensity
     * @return the new Color */
    public static Color allocate(final int nr, final int ng, final int nb)
    {
        return allocate((byte)Math.max(Math.min(nr, 0xFF), 0),
                        (byte)Math.max(Math.min(ng, 0xFF), 0),
                        (byte)Math.max(Math.min(nb, 0xFF), 0),
                        (byte)0);
    }

    /** @param rt
     *            the color to copy
     * @return the new Color */
    public static Color allocate(final Color rt)
    {
        return allocate(rt.r, rt.g, rt.b, rt.a);
    }

    /** @param r
     *            red intensity
     * @param g
     *            green intensity
     * @param b
     *            blue intensity
     * @return the created <code>Color</code> */
    public static Color RGB(final int r, final int g, final int b)
    {
        return allocate(r, g, b);
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
    public static Color
        RGBA(final int r, final int g, final int b, final int a)
    {
        return allocate(r, g, b, a);
    }

    /** @param r
     *            red intensity
     * @param g
     *            green intensity
     * @param b
     *            blue intensity
     * @return the created <code>Color</code> */
    public static Color RGB(final float r, final float g, final float b)
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
    public static Color RGBA(final float r,
                             final float g,
                             final float b,
                             final float a)
    {
        return RGBA((int)Math.floor(r * 256.0),
                    (int)Math.floor(g * 256.0),
                    (int)Math.floor(b * 256.0),
                    (int)Math.floor(a * 256.0));
    }

    /** @param v
     *            intensity
     * @return the created <code>Color</code> */
    public static Color V(final float v)
    {
        return RGB(v, v, v);
    }

    /** @param v
     *            intensity
     * @param a
     *            transparency
     * @return the created <code>Color</code> */
    public static Color VA(final float v, final float a)
    {
        return RGBA(v, v, v, a);
    }

    /** @param c
     *            color
     * @return red intensity */
    public static int GetRValue(final Color c)
    {
        return c.r & 0xFF;
    }

    /** @param c
     *            color
     * @return green intensity */
    public static int GetGValue(final Color c)
    {
        return c.g & 0xFF;
    }

    /** @param c
     *            color
     * @return blue intensity */
    public static int GetBValue(final Color c)
    {
        return c.b & 0xFF;
    }

    /** @param c
     *            color
     * @return transparency */
    public static int GetAValue(final Color c)
    {
        return c.a & 0xFF;
    }

    /** @param c
     *            color
     * @return red intensity */
    public static float GetRValueF(final Color c)
    {
        return (float)(c.r & 0xFF) / 0xFF;
    }

    /** @param c
     *            color
     * @return green intensity */
    public static float GetGValueF(final Color c)
    {
        return (float)(c.g & 0xFF) / 0xFF;
    }

    /** @param c
     *            color
     * @return blue intensity */
    public static float GetBValueF(final Color c)
    {
        return (float)(c.b & 0xFF) / 0xFF;
    }

    /** @param c
     *            color
     * @return transparency */
    public static float GetAValueF(final Color c)
    {
        return (float)(c.a & 0xFF) / 0xFF;
    }

    /** call glColor3* <BR/>
     * ignores <code>c</code>'s transparency
     * 
     * @param c
     *            color */
    public static void glColor(final Color c)
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
    public static void glClearColor(final Color c)
    {
        Main.opengl.glClearColor(GetRValue(c) / 255.0f,
                                 GetGValue(c) / 255.0f,
                                 GetBValue(c) / 255.0f,
                                 0.0f);
    }

    public Color compose(final Color bkgnd)
    {
        int foregroundA = GetAValue(this);
        return RGBA((GetRValue(this) * (255 - foregroundA) + GetRValue(bkgnd)
                            * foregroundA) / 255,
                    (GetGValue(this) * (255 - foregroundA) + GetGValue(bkgnd)
                            * foregroundA) / 255,
                    (GetBValue(this) * (255 - foregroundA) + GetBValue(bkgnd)
                            * foregroundA) / 255,
                    foregroundA * GetAValue(bkgnd) / 255);
    }

    @Override
    public void free()
    {
        allocator.free(this);
    }

    @Override
    public Color dup()
    {
        return allocate(this);
    }
}
