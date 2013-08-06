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

import java.util.*;

/** @author jacob */
public class TextureAtlas
{
    private static class AtlasPart
    {
        public final byte[] data;
        public int x, y;
        public final int w, h;

        public AtlasPart(final byte[] data, final int w, final int h)
        {
            this.data = data;
            this.w = w;
            this.h = h;
        }

        public boolean intersectsWith(final AtlasPart ap)
        {
            if(ap.x >= this.x + this.w || ap.x + ap.w <= this.x)
                return false;
            if(ap.y >= this.y + this.h || ap.y + ap.h <= this.y)
                return false;
            return true;
        }
    }

    /** @author jacob */
    public interface TextureHandle
    {
        /** @return this texture's image */
        public Image getImage();
    }

    private static class TextureHandleImp implements TextureHandle
    {
        public final AtlasPart ap;
        public final Image image;

        public TextureHandleImp(final AtlasPart ap, final Image image)
        {
            this.ap = ap;
            this.image = image;
        }

        @Override
        public Image getImage()
        {
            return this.image;
        }
    }

    private static ArrayList<AtlasPart> parts = new ArrayList<AtlasPart>();
    private static int finalImageWidth = 0, finalImageHeight = 0;
    private static Image finalImage = null;
    private static boolean isValidPlacement = false;

    private static boolean isImageValid()
    {
        if(finalImage == null)
            return false;
        return true;
    }

    private static Object syncObject = new Object();

    private static boolean generateImagePlacementHelper()
    {
        Comparator<AtlasPart> comparator = new Comparator<TextureAtlas.AtlasPart>()
        {
            @Override
            public int compare(final AtlasPart o1, final AtlasPart o2)
            {
                int size1 = o1.w * o1.h;
                int size2 = o2.w * o2.h;
                if(size1 < size2) // if size1 is smaller then place later
                    return 1;
                if(size1 > size2) // if size1 is larger then place earlier
                    return -1;
                return 0;
            }
        };
        PriorityQueue<AtlasPart> partQueue = new PriorityQueue<AtlasPart>(parts.size(),
                                                                          comparator);
        partQueue.addAll(parts);
        ArrayList<AtlasPart> placedParts = new ArrayList<TextureAtlas.AtlasPart>(parts.size());
        int x = 0;
        for(AtlasPart ap = partQueue.poll(); ap != null; ap = partQueue.poll())
        {
            if(x + ap.w > finalImageWidth)
                x = 0;
            int y = 0;
            ap.x = x;
            for(AtlasPart i : placedParts)
            {
                ap.y = i.y;
                if(i.intersectsWith(ap))
                {
                    int curY = i.y + i.h;
                    if(curY > y)
                    {
                        y = curY;
                        if(y + ap.h > finalImageHeight)
                            return false;
                    }
                }
            }
            ap.y = y;
            placedParts.add(ap);
            x += ap.w;
        }
        return true;
    }

    private static void generateImagePlacement()
    {
        if(isValidPlacement)
            return;
        finalImageWidth = 32;
        finalImageHeight = 32;
        while(!generateImagePlacementHelper())
        {
            if(finalImageWidth > finalImageHeight)
                finalImageHeight *= 2;
            finalImageWidth *= 2;
        }
        isValidPlacement = true;
    }

    private static void generateImage()
    {
        if(isImageValid())
            return;
        generateImagePlacement();
        finalImage = new Image(finalImageWidth, finalImageHeight);
        Color color = new Color(0, 0, 0);
        for(AtlasPart ap : parts)
        {
            for(int index = 0, y = 0; y < ap.h; y++)
            {
                for(int x = 0; x < ap.w; x++)
                {
                    color.r = ap.data[index++];
                    color.g = ap.data[index++];
                    color.b = ap.data[index++];
                    color.a = ap.data[index++];
                    finalImage.setPixel(x + ap.x, finalImageHeight - (y + ap.y)
                            - 1, color);
                }
            }
        }
    }

    /** add an image to the global texture atlas
     * 
     * @param image
     *            the image to add
     * @return the texture handle or null */
    public static TextureHandle addImage(final Image image)
    {
        if(image == null || !image.isValid())
            return null;
        Image img = Image.unmodifiable(image);
        synchronized(syncObject)
        {
            int w = img.getWidth(), h = img.getHeight();
            AtlasPart ap = new AtlasPart(new byte[4 * w * h], w, h);
            for(int y = 0, index = 0; y < h; y++)
            {
                for(int x = 0; x < w; x++)
                {
                    Color color = img.getPixel(x, h - y - 1);
                    ap.data[index++] = color.r;
                    ap.data[index++] = color.g;
                    ap.data[index++] = color.b;
                    ap.data[index++] = color.a;
                }
            }
            parts.add(ap);
            finalImage = null;
            isValidPlacement = false;
            return new TextureHandleImp(ap, img);
        }
    }

    /** for each polygon in a list of polygons transform the texture coordinates
     * to correspond to that polygon's texture image
     * 
     * @param srcTextureArray
     *            the list of input texture coordinate pairs
     * @param texHandleArray
     *            the list of texture handles
     * @param count
     *            the number of coordinate pairs to transform
     * @param textureArray
     *            the list of output texture coordinate pairs
     * @return the atlas image */
    public static Image
        transformTextureCoords(final float[] srcTextureArray,
                               final TextureHandle[] texHandleArray,
                               final int count,
                               final float[] textureArray)
    {
        synchronized(syncObject)
        {
            generateImage();
            final float widthFactor = 1.0f / finalImageWidth;
            final float heightFactor = 1.0f / finalImageHeight;
            for(int ti = 0, thi = 0; thi < count; thi++)
            {
                if(texHandleArray[thi] == null
                        || !(texHandleArray[thi] instanceof TextureHandleImp))
                    throw new IllegalArgumentException("invalid texture handle");
                TextureHandleImp h = (TextureHandleImp)texHandleArray[thi];
                AtlasPart ap = h.ap;
                for(int vertex = 0; vertex < 3; vertex++)
                {
                    float u = srcTextureArray[ti];
                    float v = srcTextureArray[ti + 1];
                    u = (u * ap.w + ap.x) * widthFactor;
                    v = (v * ap.h + ap.y) * heightFactor;
                    textureArray[ti++] = u;
                    textureArray[ti++] = v;
                }
            }
            return finalImage;
        }
    }

    private TextureAtlas()
    {
    }
}
