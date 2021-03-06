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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.voxels.PNGDecoder.Format;

/** @author jacob */
public class Image
{
    private byte[] data;
    private int w, h;
    private boolean topToBottom;
    private boolean alphaInvert;
    private static final int BytesPerPixel = 4; // RGBA
    protected int texture;
    protected boolean validTexture;

    private void SwapRows(final int y1, final int y2)
    {
        int i1 = y1 * this.w * BytesPerPixel;
        int i2 = y2 * this.w * BytesPerPixel;
        for(int i = 0; i < this.w * BytesPerPixel; i++)
        {
            byte t = this.data[i1];
            this.data[i1] = this.data[i2];
            this.data[i2] = t;
            i1++;
            i2++;
        }
    }

    private void setRowOrder(final boolean new_topToBottom)
    {
        if(!this.topToBottom && !new_topToBottom)
            return;
        if(this.topToBottom && new_topToBottom)
            return;
        this.topToBottom = new_topToBottom;
        if(this.data == null)
            return;
        for(int y1 = 0, y2 = this.h - 1; y1 < y2; y1++, y2--)
        {
            SwapRows(y1, y2);
        }
    }

    private void setAlphaInvert(final boolean isInverted)
    {
        boolean doinvert = false;
        if(isInverted && !this.alphaInvert)
            doinvert = true;
        if(!isInverted && this.alphaInvert)
            doinvert = true;
        this.alphaInvert = isInverted;
        if(!doinvert || this.data == null)
            return;
        for(int i = this.w * this.h - 1, pos = 3; i >= 0; i--, pos += 4)
        {
            this.data[pos] = (byte)(~this.data[pos]);
        }
    }

    /** create an invalid <code>Image</code> */
    public Image()
    {
        this.data = null;
        this.w = 0;
        this.h = 0;
        this.topToBottom = false;
        this.alphaInvert = false;
        this.texture = 0;
        this.validTexture = false;
    }

    /** load an image from <code>filename</code><br/>
     * only supports loading from PNG images
     * 
     * @param filename
     *            the name of the image to load */
    @SuppressWarnings("resource")
    public Image(final String filename)
    {
        this.texture = 0;
        this.validTexture = false;
        InputStream in = null;
        try
        {
            in = Main.platform.getFileInputStream(filename);
            PNGDecoder decoder = new PNGDecoder(in);
            this.w = decoder.getWidth();
            this.h = decoder.getHeight();
            if(Integer.bitCount(this.w) != 1 || Integer.bitCount(this.h) != 1) // must
                                                                               // be
                                                                               // power
                                                                               // of
                                                                               // two
                                                                               // sizes
                throw new IOException();
            ByteBuffer buf = ByteBuffer.wrap(new byte[4 * decoder.getWidth()
                    * decoder.getHeight()]);
            decoder.decode(buf, decoder.getWidth() * 4, Format.RGBA);
            buf.flip();
            this.data = buf.array();
            this.topToBottom = true;
            this.alphaInvert = true;
            if(this.data == null)
            {
                this.data = null;
                this.w = 0;
                this.h = 0;
                this.topToBottom = false;
                this.alphaInvert = false;
            }
        }
        catch(IOException e)
        {
            this.data = null;
            this.w = 0;
            this.h = 0;
            this.topToBottom = false;
            this.alphaInvert = false;
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
            if(this.data == null)
                throw new RuntimeException("can't load image : " + filename);
        }
    }

    /** create a transparent image
     * 
     * @param width
     *            the width of the new image
     * @param height
     *            the height of the new image */
    public Image(final int width, final int height)
    {
        if(Integer.bitCount(width) != 1 || Integer.bitCount(height) != 1) // must
                                                                          // be
                                                                          // a
                                                                          // power
                                                                          // of
                                                                          // 2
                                                                          // size
        {
            this.data = null;
            this.w = 0;
            this.h = 0;
            this.topToBottom = false;
            this.alphaInvert = false;
            this.texture = 0;
            this.validTexture = false;
            return;
        }
        this.texture = 0;
        this.validTexture = false;
        this.topToBottom = false;
        this.alphaInvert = false;
        this.w = width;
        this.h = height;
        this.data = new byte[this.w * this.h * BytesPerPixel];
        for(int i = 0; i < this.w * this.h * BytesPerPixel; i++)
            this.data[i] = (byte)0xFF;
    }

    /** create a single color image
     * 
     * @param color
     *            the color */
    public Image(final Color color)
    {
        this.texture = 0;
        this.validTexture = false;
        this.topToBottom = false;
        this.alphaInvert = false;
        this.w = 1;
        this.h = 1;
        this.data = new byte[this.w * this.h * BytesPerPixel];
        for(int i = 0; i < this.w * this.h * BytesPerPixel; i++)
            this.data[i] = (byte)0xFF;
        setPixel(0, 0, color);
    }

    /** create a copy of <code>rt</code>
     * 
     * @param rt
     *            the image to copy */
    public Image(final Image rt)
    {
        this.texture = 0;
        this.validTexture = false;
        this.topToBottom = rt.topToBottom;
        this.alphaInvert = rt.alphaInvert;
        this.w = rt.w;
        this.h = rt.h;
        if(rt.data != null)
        {
            this.data = new byte[this.w * this.h * BytesPerPixel];
            for(int i = 0; i < this.w * this.h * BytesPerPixel; i++)
                this.data[i] = rt.data[i];
        }
        else
            this.data = null;
    }

    /** @return true if this image is valid */
    public boolean isValid()
    {
        if(this.data != null)
            return true;
        return false;
    }

    private static int CurrentTexture = 0;

    /** select the default (blank) texture */
    public static void unselectTexture()
    {
        Main.opengl.glBindTexture(Main.opengl.GL_TEXTURE_2D(), 0);
        CurrentTexture = 0;
    }

    /** call after calling glNewList */
    public static void onListStart()
    {
        CurrentTexture = 0;
    }

    private void genTexture()
    {
        if(this.data == null)
        {
            if(this.texture != 0)
            {
                if(CurrentTexture == this.texture)
                {
                    CurrentTexture = 0;
                    Main.opengl.glBindTexture(Main.opengl.GL_TEXTURE_2D(), 0);
                }
                Main.opengl.glDeleteTextures(this.texture);
            }
            this.texture = 0;
            this.validTexture = true;
            return;
        }
        setRowOrder(false);
        setAlphaInvert(false);
        if(this.texture == 0)
            this.texture = Main.opengl.glGenTextures();
        if(CurrentTexture != this.texture)
            Main.opengl.glBindTexture(Main.opengl.GL_TEXTURE_2D(), this.texture);
        CurrentTexture = this.texture;
        Main.opengl.glTexParameteri(Main.opengl.GL_TEXTURE_2D(),
                                    Main.opengl.GL_TEXTURE_WRAP_S(),
                                    Main.opengl.GL_REPEAT());
        Main.opengl.glTexParameteri(Main.opengl.GL_TEXTURE_2D(),
                                    Main.opengl.GL_TEXTURE_WRAP_T(),
                                    Main.opengl.GL_REPEAT());
        Main.opengl.glTexParameteri(Main.opengl.GL_TEXTURE_2D(),
                                    Main.opengl.GL_TEXTURE_MAG_FILTER(),
                                    Main.opengl.GL_NEAREST());
        Main.opengl.glTexParameteri(Main.opengl.GL_TEXTURE_2D(),
                                    Main.opengl.GL_TEXTURE_MIN_FILTER(),
                                    Main.opengl.GL_NEAREST());
        Main.opengl.glPixelStorei(Main.opengl.GL_UNPACK_ALIGNMENT(), 1);
        ByteBuffer buf = Main.platform.createByteBuffer(this.data.length);
        buf.put(this.data);
        buf.flip();
        Main.opengl.glTexImage2D(Main.opengl.GL_TEXTURE_2D(),
                                 0,
                                 Main.opengl.GL_RGBA(),
                                 this.w,
                                 this.h,
                                 0,
                                 Main.opengl.GL_RGBA(),
                                 Main.opengl.GL_UNSIGNED_BYTE(),
                                 buf);
        this.validTexture = true;
    }

    /** selects this image as the current texture */
    public void selectTexture()
    {
        if(!this.validTexture)
            genTexture();
        if(this.texture != CurrentTexture)
        {
            Main.opengl.glBindTexture(Main.opengl.GL_TEXTURE_2D(), this.texture);
            CurrentTexture = this.texture;
        }
    }

    /** @return true if this is selected as the current texture */
    public boolean isSelected()
    {
        if(!this.validTexture)
            return false;
        return this.texture == CurrentTexture;
    }

    /** the size that an image used as an icon must be */
    public final int IconSize = 32;

    /** sets this image as the window's icon */
    public void setWindowIcon()
    {
        if(this.data == null)
            return;
        assert this.w == this.IconSize && this.h == this.IconSize : "Wrong Sized Icon";
        ByteBuffer buf = Main.platform.createByteBuffer(this.data.length);
        buf.put(this.data);
        buf.flip();
        Main.platform.setIcon(new ByteBuffer[]
        {
            buf
        });
    }

    /** get the color of a pixel
     * 
     * @param x
     *            x coordinate from left
     * @param y
     *            y coordinate from top
     * @return the color of the pixel or transparent white if (<code>x</code>,
     *         <code>y</code>) is outside of the image */
    public Color getPixel(final int x, final int y)
    {
        if(this.data == null || x < 0 || x >= this.w || y < 0 || y >= this.h)
            return Color.RGBA(0xFF, 0xFF, 0xFF, 0xFF);
        setAlphaInvert(false);
        int yp = y;
        if(!this.topToBottom)
            yp = this.h - yp - 1;
        this.validTexture = false;
        int index = (x + yp * this.w) * BytesPerPixel;
        return Color.allocate(this.data[index],
                              this.data[index + 1],
                              this.data[index + 2],
                              this.data[index + 3]);
    }

    /** set the color of a pixel
     * 
     * @param x
     *            x coordinate from left
     * @param y
     *            y coordinate from top
     * @param c
     *            the new color of the pixel */
    public void setPixel(final int x, final int y, final Color c)
    {
        if(this.data == null || x < 0 || x >= this.w || y < 0 || y >= this.h)
            return;
        setAlphaInvert(false);
        int yp = y;
        if(!this.topToBottom)
            yp = this.h - yp - 1;
        this.validTexture = false;
        int index = (x + yp * this.w) * BytesPerPixel;
        this.data[index] = c.r;
        this.data[index + 1] = c.g;
        this.data[index + 2] = c.b;
        this.data[index + 3] = c.a;
    }

    /** destroy this image */
    public void destroy()
    {
        this.data = null;
        this.w = 0;
        this.h = 0;
        this.topToBottom = true;
        this.alphaInvert = false;
        if(CurrentTexture == this.texture)
        {
            CurrentTexture = 0;
            Main.opengl.glBindTexture(Main.opengl.GL_TEXTURE_2D(), 0);
        }
        Main.opengl.glDeleteTextures(this.texture);
        this.validTexture = false;
        this.texture = 0;
    }

    /** @return this image's width */
    public int getWidth()
    {
        return this.w;
    }

    /** @return this image's height */
    public int getHeight()
    {
        return this.h;
    }

    /** change all transparent areas to <code>c</code>
     * 
     * @param c
     *            the new background color */
    public void setSolidBackground(final Color c)
    {
        for(int y = 0; y < getHeight(); y++)
        {
            for(int x = 0; x < getWidth(); x++)
            {
                setPixel(x, y, getPixel(x, y).compose(c));
            }
        }
    }

    private static class ConstantImage extends Image
    {
        public ConstantImage(final Image rt)
        {
            super(rt);
        }

        @Override
        public void setPixel(final int x, final int y, final Color c)
        {
        }

        @Override
        public void destroy()
        {
            if(isSelected())
                unselectTexture();
            Main.opengl.glDeleteTextures(this.texture);
            this.validTexture = false;
            this.texture = 0;
        }

        @Override
        public boolean isModifiable()
        {
            return false;
        }
    }

    /** @return true if this image is modifiable */
    public boolean isModifiable()
    {
        return true;
    }

    /** makes a unmodifiable copy of an image
     * 
     * @param img
     *            the image to copy
     * @return the unmodifiable copy of <code>img</code> */
    public static Image unmodifiable(final Image img)
    {
        if(img != null && img.isModifiable())
            return new ConstantImage(img);
        return img;
    }
}
