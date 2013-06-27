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

import java.nio.FloatBuffer;
import java.util.*;

import org.lwjgl.opengl.GL11;

/** @author jacob */
public class RenderingStream
{
    /* if <code>RenderingStream</code> should use vertex arrays and the texture
     * atlas */
    private static final boolean USE_VERTEX_ARRAY_AND_TEXTURE_ATLAS = true;
    private static final boolean USE_TEXTURE_ATLAS = true;

    /** @author jacob */
    public static final class Polygon
    {
        /** the polygon's texture */
        public final TextureAtlas.TextureHandle texture;
        /***/
        public static final int VERT_SIZE = 3 + 2 + 4;
        /***/
        public static final int VERT_X = 0;
        /***/
        public static final int VERT_Y = 1;
        /***/
        public static final int VERT_Z = 2;
        /***/
        public static final int VERT_U = 3;
        /***/
        public static final int VERT_V = 4;
        /***/
        public static final int VERT_R = 5;
        /***/
        public static final int VERT_G = 6;
        /***/
        public static final int VERT_B = 7;
        /***/
        public static final int VERT_A = 8;
        /***/
        public static final int VERT_COUNT = 3;
        /***/
        public static final int POLY_COMMAND = GL11.GL_TRIANGLES;
        /** the polygon's values with each vertex's values in the order pos.x,
         * pos.y, pos.z, u, v, clr.r, clr.g, clr.b, clr.a */
        public final float[] values = new float[VERT_SIZE * VERT_COUNT];
        private int index = 0;

        /** @param pos
         *            the vertex's position
         * @param u
         *            the vertex's U texture coordinate
         * @param v
         *            the vertex's V texture coordinate
         * @param clr
         *            the vertex's color
         * @return <code>this</code> */
        public Polygon addVertex(Vector pos, float u, float v, Color clr)
        {
            this.values[this.index++] = pos.x;
            this.values[this.index++] = pos.y;
            this.values[this.index++] = pos.z;
            this.values[this.index++] = u;
            this.values[this.index++] = v;
            this.values[this.index++] = Color.GetRValue(clr) / 255.0f;
            this.values[this.index++] = Color.GetGValue(clr) / 255.0f;
            this.values[this.index++] = Color.GetBValue(clr) / 255.0f;
            this.values[this.index++] = Color.GetAValue(clr) / 255.0f;
            return this;
        }

        /** @param texture
         *            the polygon's texture */
        public Polygon(TextureAtlas.TextureHandle texture)
        {
            this.texture = texture;
        }

        /** construct a copy of a polygon
         * 
         * @param rt
         *            the polygon to copy */
        public Polygon(Polygon rt)
        {
            this.texture = rt.texture;
            this.index = rt.index;
            for(int i = 0; i < this.index; i++)
                this.values[i] = rt.values[i];
        }

        /** @param index
         *            the vertex index
         * @return the point */
        public Vector getPoint(int index)
        {
            return new Vector(this.values[index * VERT_SIZE + VERT_X],
                              this.values[index * VERT_SIZE + VERT_Y],
                              this.values[index * VERT_SIZE + VERT_Z]);
        }

        /** @param index
         *            the vertex index
         * @param v
         *            the new point */
        public void setPoint(int index, Vector v)
        {
            this.values[index * VERT_SIZE + VERT_X] = v.x;
            this.values[index * VERT_SIZE + VERT_Y] = v.y;
            this.values[index * VERT_SIZE + VERT_Z] = v.z;
        }

        /** @param tform
         *            the transformation
         * @return new transformed polygon */
        public Polygon transform(Matrix tform)
        {
            Polygon retval = new Polygon(this);
            for(int i = 0; i < VERT_COUNT; i++)
                retval.setPoint(i, tform.apply(getPoint(i)));
            return retval;
        }
    }

    private final ArrayList<Polygon> polys;
    private final Deque<Matrix> matrixStack;

    /**
	 * 
	 */
    public RenderingStream()
    {
        this.polys = new ArrayList<RenderingStream.Polygon>();
        this.matrixStack = new LinkedList<Matrix>();
        this.matrixStack.addFirst(Matrix.identity());
    }

    /** @param p
     *            the polygon to add
     * @return <code>this</code> */
    public RenderingStream add(Polygon p)
    {
        if(p == null)
            throw new NullPointerException();
        this.polys.add(p.transform(this.matrixStack.getFirst()));
        return this;
    }

    /**
	 * 
	 */
    public void pushMatrixStack()
    {
        this.matrixStack.addFirst(new Matrix(this.matrixStack.getFirst()));
    }

    /** @param mat
     *            the matrix to set to */
    public void setMatrix(Matrix mat)
    {
        if(mat == null)
            throw new NullPointerException();
        this.matrixStack.removeFirst();
        this.matrixStack.addFirst(mat);
    }

    /** @param mat
     *            the matrix to concat to */
    public void concatMatrix(Matrix mat)
    {
        if(mat == null)
            throw new NullPointerException();
        Matrix addMat = mat.concat(this.matrixStack.removeFirst());
        this.matrixStack.addFirst(addMat);
    }

    /**
	 * 
	 */
    public void popMatrixStack()
    {
        this.matrixStack.removeFirst();
    }

    /** @param rs
     *            the rendering stream
     * @return <code>this</code> */
    public RenderingStream add(RenderingStream rs)
    {
        if(rs == null)
            throw new NullPointerException();
        assert rs != this;
        for(Polygon p : rs.polys)
            add(p);
        return this;
    }

    /**
	 * 
	 */
    public void render()
    {
        if(USE_VERTEX_ARRAY_AND_TEXTURE_ATLAS)
        {
            FloatBuffer vertexArray = org.lwjgl.BufferUtils.createFloatBuffer(Polygon.VERT_COUNT
                    * 3 * this.polys.size());
            FloatBuffer textureArray = org.lwjgl.BufferUtils.createFloatBuffer(Polygon.VERT_COUNT
                    * 2 * this.polys.size());
            FloatBuffer colorArray = org.lwjgl.BufferUtils.createFloatBuffer(Polygon.VERT_COUNT
                    * 4 * this.polys.size());
            for(Polygon p : this.polys)
            {
                for(int i = 0; i < Polygon.VERT_COUNT; i++)
                {
                    vertexArray.put(p.values[i * Polygon.VERT_SIZE
                            + Polygon.VERT_X]);
                    vertexArray.put(p.values[i * Polygon.VERT_SIZE
                            + Polygon.VERT_Y]);
                    vertexArray.put(p.values[i * Polygon.VERT_SIZE
                            + Polygon.VERT_Z]);
                    colorArray.put(p.values[i * Polygon.VERT_SIZE
                            + Polygon.VERT_R]);
                    colorArray.put(p.values[i * Polygon.VERT_SIZE
                            + Polygon.VERT_G]);
                    colorArray.put(p.values[i * Polygon.VERT_SIZE
                            + Polygon.VERT_B]);
                    colorArray.put(p.values[i * Polygon.VERT_SIZE
                            + Polygon.VERT_A]);
                }
            }
            Image texImage = TextureAtlas.transformTextureCoords(this.polys,
                                                                 textureArray);
            vertexArray.flip();
            textureArray.flip();
            colorArray.flip();
            GL11.glFlush();
            GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
            GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            GL11.glVertexPointer(3, 0, vertexArray);
            GL11.glTexCoordPointer(2, 0, textureArray);
            GL11.glColorPointer(4, 0, colorArray);
            texImage.selectTexture();
            GL11.glDrawArrays(Polygon.POLY_COMMAND, 0, Polygon.VERT_COUNT
                    * this.polys.size());
            GL11.glFlush();
            GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
            GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        }
        else if(USE_TEXTURE_ATLAS)
        {
            FloatBuffer textureArray = FloatBuffer.allocate(Polygon.VERT_COUNT
                    * 2 * this.polys.size());
            Image texImage = TextureAtlas.transformTextureCoords(this.polys,
                                                                 textureArray);
            textureArray.flip();
            texImage.selectTexture();
            GL11.glBegin(Polygon.POLY_COMMAND);
            for(Polygon p : this.polys)
            {
                for(int vertex = 0; vertex < Polygon.VERT_COUNT; vertex++)
                {
                    GL11.glColor4f(p.values[vertex * Polygon.VERT_SIZE
                                           + Polygon.VERT_R],
                                   p.values[vertex * Polygon.VERT_SIZE
                                           + Polygon.VERT_G],
                                   p.values[vertex * Polygon.VERT_SIZE
                                           + Polygon.VERT_B],
                                   p.values[vertex * Polygon.VERT_SIZE
                                           + Polygon.VERT_A]);
                    float u = textureArray.get();
                    float v = textureArray.get();
                    GL11.glTexCoord2f(u, v);
                    GL11.glVertex3f(p.values[vertex * Polygon.VERT_SIZE
                                            + Polygon.VERT_X],
                                    p.values[vertex * Polygon.VERT_SIZE
                                            + Polygon.VERT_Y],
                                    p.values[vertex * Polygon.VERT_SIZE
                                            + Polygon.VERT_Z]);
                }
            }
            GL11.glEnd();
        }
        else
        {
            boolean insideBeginEnd = false;
            for(Polygon p : this.polys)
            {
                if(!p.texture.getImage().isSelected())
                {
                    if(insideBeginEnd)
                    {
                        GL11.glEnd();
                        insideBeginEnd = false;
                    }
                    p.texture.getImage().selectTexture();
                }
                if(!insideBeginEnd)
                {
                    GL11.glBegin(Polygon.POLY_COMMAND);
                    insideBeginEnd = true;
                }
                for(int vertex = 0; vertex < Polygon.VERT_COUNT; vertex++)
                {
                    GL11.glColor4f(p.values[vertex * Polygon.VERT_SIZE
                                           + Polygon.VERT_R],
                                   p.values[vertex * Polygon.VERT_SIZE
                                           + Polygon.VERT_G],
                                   p.values[vertex * Polygon.VERT_SIZE
                                           + Polygon.VERT_B],
                                   p.values[vertex * Polygon.VERT_SIZE
                                           + Polygon.VERT_A]);
                    GL11.glTexCoord2f(p.values[vertex * Polygon.VERT_SIZE
                            + Polygon.VERT_U], p.values[vertex
                            * Polygon.VERT_SIZE + Polygon.VERT_V]);
                    GL11.glVertex3f(p.values[vertex * Polygon.VERT_SIZE
                                            + Polygon.VERT_X],
                                    p.values[vertex * Polygon.VERT_SIZE
                                            + Polygon.VERT_Y],
                                    p.values[vertex * Polygon.VERT_SIZE
                                            + Polygon.VERT_Z]);
                }
            }
            if(insideBeginEnd)
                GL11.glEnd();
        }
    }
}
