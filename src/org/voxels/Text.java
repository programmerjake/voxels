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
import static org.voxels.Color.*;
import static org.voxels.Vector.glVertex;

import java.util.*;

/** @author jacob */
public final class Text
{
    private static Image font = new Image("font.png");
    private static TextureAtlas.TextureHandle fontTexture = TextureAtlas.addImage(font);
    private static final int fontW = 8, fontH = 8;

    private static boolean getCharPixel(int x_in, int y_in, int ch)
    {
        int x = x_in, y = y_in;
        if(x < 0 || x >= fontW || y < 0 || y >= fontH)
        {
            int c = ch;
            c &= 0xFF;
            x += fontW * (c % 16);
            y += fontH * (c / 16);
            Color clr = font.getPixel(x, y);
            if(GetRValue(clr) >= 128 && GetAValue(clr) < 64)
                return true;
        }
        return false;
    }

    private static final int[] topPageTranslations = new int[]
    {
        0x00C7,
        0x00FC,
        0x00E9,
        0x00E2,
        0x00E4,
        0x00E0,
        0x00E5,
        0x00E7,
        0x00EA,
        0x00EB,
        0x00E8,
        0x00EF,
        0x00EE,
        0x00EC,
        0x00C4,
        0x00C5,
        0x00C9,
        0x00E6,
        0x00C6,
        0x00F4,
        0x00F6,
        0x00F2,
        0x00FB,
        0x00F9,
        0x00FF,
        0x00D6,
        0x00DC,
        0x00A2,
        0x00A3,
        0x00A5,
        0x20A7,
        0x0192,
        0x00E1,
        0x00ED,
        0x00F3,
        0x00FA,
        0x00F1,
        0x00D1,
        0x00AA,
        0x00BA,
        0x00BF,
        0x2310,
        0x00AC,
        0x00BD,
        0x00BC,
        0x00A1,
        0x00AB,
        0x00BB,
        0x2591,
        0x2592,
        0x2593,
        0x2502,
        0x2524,
        0x2561,
        0x2562,
        0x2556,
        0x2555,
        0x2563,
        0x2551,
        0x2557,
        0x255D,
        0x255C,
        0x255B,
        0x2510,
        0x2514,
        0x2534,
        0x252C,
        0x251C,
        0x2500,
        0x253C,
        0x255E,
        0x255F,
        0x255A,
        0x2554,
        0x2569,
        0x2566,
        0x2560,
        0x2550,
        0x256C,
        0x2567,
        0x2568,
        0x2564,
        0x2565,
        0x2559,
        0x2558,
        0x2552,
        0x2553,
        0x256B,
        0x256A,
        0x2518,
        0x250C,
        0x2588,
        0x2584,
        0x258C,
        0x2590,
        0x2580,
        0x03B1,
        0x00DF,
        0x0393,
        0x03C0,
        0x03A3,
        0x03C3,
        0x00B5,
        0x03C4,
        0x03A6,
        0x0398,
        0x03A9,
        0x03B4,
        0x221E,
        0x03C6,
        0x03B5,
        0x2229,
        0x2261,
        0x00B1,
        0x2265,
        0x2264,
        0x2320,
        0x2321,
        0x00F7,
        0x2248,
        0x00B0,
        0x2219,
        0x00B7,
        0x221A,
        0x207F,
        0x00B2,
        0x25A0,
        0x00A0
    };

    private static Map<Integer, Integer> makeTPTMap()
    {
        Map<Integer, Integer> retval = new HashMap<Integer, Integer>();
        for(int i = 0; i < 0x80; i++)
            retval.put(new Integer(topPageTranslations[i]),
                       new Integer(i + 0x80));
        return Collections.unmodifiableMap(retval);
    }

    private static final Map<Integer, Integer> topPageTranslationsMap = makeTPTMap();

    private static int translateToCodePage437(char ch)
    {
        int character = ch;
        if(character == '\0')
            return character;
        if(character >= 0x20 && character <= 0x7E)
            return character;
        switch(character)
        {
        case '\u263A':
            return 0x01;
        case '\u263B':
            return 0x02;
        case '\u2665':
            return 0x03;
        case '\u2666':
            return 0x04;
        case '\u2663':
            return 0x05;
        case '\u2660':
            return 0x06;
        case '\u2022':
            return 0x07;
        case '\u25D8':
            return 0x08;
        case '\u25CB':
            return 0x09;
        case '\u25D9':
            return 0x0A;
        case '\u2642':
            return 0x0B;
        case '\u2640':
            return 0x0C;
        case '\u266A':
            return 0x0D;
        case '\u266B':
            return 0x0E;
        case '\u263C':
            return 0x0F;
        case '\u25BA':
            return 0x10;
        case '\u25C4':
            return 0x11;
        case '\u2195':
            return 0x12;
        case '\u203C':
            return 0x13;
        case '\u00B6':
            return 0x14;
        case '\u00A7':
            return 0x15;
        case '\u25AC':
            return 0x16;
        case '\u21A8':
            return 0x17;
        case '\u2191':
            return 0x18;
        case '\u2193':
            return 0x19;
        case '\u2192':
            return 0x1A;
        case '\u2190':
            return 0x1B;
        case '\u221F':
            return 0x1C;
        case '\u2194':
            return 0x1D;
        case '\u25B2':
            return 0x1E;
        case '\u25BC':
            return 0x1F;
        case '\u2302':
            return 0x7F;
        case '\u03B2':
            return 0xE1;
        case '\u03A0':
        case '\u220F':
            return 0xE3;
        case '\u2211':
            return 0xE4;
        case '\u03BC':
            return 0xE6;
        case '\u2126':
            return 0xEA;
        case '\u00F0':
        case '\u2202':
            return 0xEB;
        case '\u2205':
        case '\u03D5':
        case '\u2300':
        case '\u00F8':
            return 0xED;
        case '\u2208':
        case '\u20AC':
            return 0xEE;
        default:
        {
            Integer result = topPageTranslationsMap.get(new Integer(character));
            if(result != null)
                return result.intValue();
            return '?';
        }
        }
    }

    /** draw text
     * 
     * @param tform
     *            the text to camera transformation
     * @param clr
     *            the text color
     * @param str
     *            the text to draw */
    public static void draw(Matrix tform, Color clr, String str)
    {
        if(str.length() < 1)
            return;
        font.selectTexture();
        glColor(clr);
        glBegin(GL_QUADS);
        final float du = 1.0f / 16.0f, dv = 1.0f / 16.0f;
        float x = 0.0f, y = 0.0f;
        for(int i = 0; i < str.length(); i++)
        {
            if(str.charAt(i) == '\n')
            {
                x = 0.0f;
                y -= 1.0f;
                continue;
            }
            int ch = translateToCodePage437(str.charAt(i));
            float u, v;
            u = ch % 16 / 16.0f;
            v = 1.0f - (ch / 16 + 1) / 16.0f;
            glTexCoord2f(u, v);
            glVertex(tform.apply(new Vector(0 + x, 0 + y, 0)));
            glTexCoord2f(u + du, v);
            glVertex(tform.apply(new Vector(1 + x, 0 + y, 0)));
            glTexCoord2f(u + du, v + dv);
            glVertex(tform.apply(new Vector(1 + x, 1 + y, 0)));
            glTexCoord2f(u, v + dv);
            glVertex(tform.apply(new Vector(0 + x, 1 + y, 0)));
            x += 1.0f;
        }
        glEnd();
    }

    /** draw text in white
     * 
     * @param tform
     *            the text to camera transformation
     * @param str
     *            the text to draw */
    public static void draw(Matrix tform, String str)
    {
        draw(tform, RGB(0xFF, 0xFF, 0xFF), str);
    }

    /** draw text
     * 
     * @param rs
     *            the rendering stream
     * @param tform
     *            the text to camera transformation
     * @param clr
     *            the text color
     * @param str
     *            the text to draw
     * @return <code>rs</code> */
    public static RenderingStream draw(RenderingStream rs,
                                       Matrix tform,
                                       Color clr,
                                       String str)
    {
        if(str.length() < 1)
            return rs;
        final float du = 1.0f / 16.0f, dv = 1.0f / 16.0f;
        float x = 0.0f, y = 0.0f;
        for(int i = 0; i < str.length(); i++)
        {
            if(str.charAt(i) == '\n')
            {
                x = 0.0f;
                y -= 1.0f;
                continue;
            }
            int ch = translateToCodePage437(str.charAt(i));
            float u, v;
            u = ch % 16 / 16.0f;
            v = 1.0f - (ch / 16 + 1) / 16.0f;
            RenderingStream.Polygon p = new RenderingStream.Polygon(fontTexture);
            p.addVertex(tform.apply(new Vector(0 + x, 0 + y, 0)), u, v, clr);
            p.addVertex(tform.apply(new Vector(1 + x, 0 + y, 0)),
                        u + du,
                        v,
                        clr);
            p.addVertex(tform.apply(new Vector(1 + x, 1 + y, 0)), u + du, v
                    + dv, clr);
            rs.add(p);
            p = new RenderingStream.Polygon(fontTexture);
            p.addVertex(tform.apply(new Vector(1 + x, 1 + y, 0)), u + du, v
                    + dv, clr);
            p.addVertex(tform.apply(new Vector(0 + x, 1 + y, 0)),
                        u,
                        v + dv,
                        clr);
            p.addVertex(tform.apply(new Vector(0 + x, 0 + y, 0)), u, v, clr);
            rs.add(p);
            x += 1.0f;
        }
        glEnd();
        return rs;
    }

    /** draw text in white
     * 
     * @param rs
     *            the rendering stream
     * @param tform
     *            the text to camera transformation
     * @param str
     *            the text to draw
     * @return <code>rs</code> */
    public static RenderingStream draw(RenderingStream rs,
                                       Matrix tform,
                                       String str)
    {
        return draw(rs, tform, RGB(0xFF, 0xFF, 0xFF), str);
    }

    /** draw text to an image
     * 
     * @param img
     *            image to draw to
     * @param x
     *            the x coordinate of the start of the text
     * @param y
     *            the y coordinate of the start of the text
     * @param clr
     *            the color to draw in
     * @param str
     *            the text to draw */
    public static void draw(Image img, int x, int y, Color clr, String str)
    {
        int xi = x, yi = y;
        if(str.length() < 1)
            return;
        int startx = xi;
        for(int i = 0; i < str.length(); i++)
        {
            if(str.charAt(i) == '\n')
            {
                xi = startx;
                yi += fontH;
                continue;
            }
            for(int cy = 0; cy < fontH; cy++)
                for(int cx = 0; cx < fontW; cx++)
                    if(getCharPixel(cx,
                                    cy,
                                    translateToCodePage437(str.charAt(i))))
                        img.setPixel(cx + xi, cy + yi, clr);
            xi += fontW;
        }
        glEnd();
    }

    /** @param str
     *            text to get the size of
     * @return the width in pixels of the text */
    public static int sizeW(String str)
    {
        int maxx = 0;
        int curx = 0;
        for(int i = 0; i < str.length(); i++)
        {
            if(str.charAt(i) == '\n')
            {
                curx = 0;
                continue;
            }
            curx += fontW;
            if(curx > maxx)
                maxx = curx;
        }
        return maxx;
    }

    /** @param str
     *            text to get the size of
     * @return the height in pixels of the text */
    public static int sizeH(String str)
    {
        int maxy = 0;
        int cury = 0;
        for(int i = 0; i < str.length(); i++)
        {
            if(str.charAt(i) == '\n')
            {
                cury += fontH;
            }
            if(cury + fontH > maxy)
                maxy = cury + fontH;
        }
        return maxy;
    }

    private Text()
    {
    }
}
