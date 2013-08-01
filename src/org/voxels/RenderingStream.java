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

/** @author jacob */
public class RenderingStream
{
    private static class MatrixNode
    {
        public final Matrix mat;
        public MatrixNode next;

        public MatrixNode()
        {
            this.mat = Matrix.setToIdentity(new Matrix());
            this.next = null;
        }
    }

    private static MatrixNode[] freeMatrixHead = new MatrixNode[]
    {
        null
    };

    private static void freeMatrix(final MatrixNode m)
    {
        synchronized(freeMatrixHead)
        {
            m.next = freeMatrixHead[0];
            freeMatrixHead[0] = m;
        }
    }

    private static MatrixNode allocMatrix()
    {
        synchronized(freeMatrixHead)
        {
            MatrixNode retval = freeMatrixHead[0];
            if(retval != null)
            {
                freeMatrixHead[0] = retval.next;
                retval.next = null;
                Matrix.set(retval.mat, Matrix.IDENTITY);
                return retval;
            }
        }
        return new MatrixNode();
    }

    /** if <code>RenderingStream</code> should use vertex arrays and the texture
     * atlas */
    public static boolean USE_VERTEX_ARRAY_AND_TEXTURE_ATLAS = false;
    private static final boolean USE_TEXTURE_ATLAS = false;
    private static final TextureAtlas.TextureHandle whiteTexture = TextureAtlas.addImage(new Image(Color.V(1.0f)));
    /**
     * 
     */
    public static final TextureAtlas.TextureHandle NO_TEXTURE = null;
    private RenderingStream next = null;
    private static final RenderingStream[] pool = new RenderingStream[]
    {
        null
    };

    public static RenderingStream allocate()
    {
        synchronized(pool)
        {
            if(pool[0] == null)
                return new RenderingStream();
            RenderingStream retval = pool[0];
            pool[0] = retval.next;
            retval.clear();
            return retval;
        }
    }

    public static void free(final RenderingStream rs)
    {
        if(rs == null)
            return;
        synchronized(pool)
        {
            rs.next = pool[0];
            pool[0] = rs;
        }
    }

    private static float[] expandArray(final float[] array, final int newSize)
    {
        float[] retval = new float[newSize];
        if(array != null)
            System.arraycopy(array, 0, retval, 0, array.length);
        return retval;
    }

    private static TextureAtlas.TextureHandle[]
        expandArray(final TextureAtlas.TextureHandle[] array, final int newSize)
    {
        TextureAtlas.TextureHandle[] retval = new TextureAtlas.TextureHandle[newSize];
        System.arraycopy(array, 0, retval, 0, array.length);
        return retval;
    }

    private float[] vertexArray = null;
    private float[] colorArray = null;
    private float[] texCoordArray = null;
    private float[] transformedTexCoordArray = null;
    private TextureAtlas.TextureHandle[] textureArray = new TextureAtlas.TextureHandle[0];
    private int trianglesUsed = 0;
    private MatrixNode matrixStack = null;
    private int trianglePoint = -1;

    public void clear()
    {
        this.trianglesUsed = 0;
        this.next = null;
        while(this.matrixStack != null)
        {
            MatrixNode node = this.matrixStack;
            this.matrixStack = node.next;
            freeMatrix(node);
        }
        this.matrixStack = allocMatrix();
        this.matrixStack.next = null;
        this.trianglePoint = -1;
    }

    /**
	 * 
	 */
    private RenderingStream()
    {
        this.trianglesUsed = 0;
        this.next = null;
        this.matrixStack = allocMatrix();
        this.matrixStack.next = null;
        this.trianglePoint = -1;
    }

    public RenderingStream
        beginTriangle(final TextureAtlas.TextureHandle texture)
    {
        if(this.trianglePoint >= 0)
            throw new IllegalStateException("beginTriangle called twice without endTriangle call in between");
        this.trianglePoint = 0;
        if(this.trianglesUsed >= this.textureArray.length)
        {
            this.textureArray = expandArray(this.textureArray,
                                            this.textureArray.length + 256);
            this.vertexArray = expandArray(this.vertexArray,
                                           this.textureArray.length * 3 * 3);
            this.colorArray = expandArray(this.colorArray,
                                          this.textureArray.length * 4 * 3);
            this.texCoordArray = expandArray(this.texCoordArray,
                                             this.textureArray.length * 2 * 3);
            this.transformedTexCoordArray = expandArray(this.transformedTexCoordArray,
                                                        this.textureArray.length * 2 * 3);
        }
        if(texture == NO_TEXTURE)
            this.textureArray[this.trianglesUsed] = whiteTexture;
        else
            this.textureArray[this.trianglesUsed] = texture;
        return this;
    }

    public RenderingStream endTriangle()
    {
        if(this.trianglePoint != 3)
        {
            if(this.trianglePoint == -1)
                throw new IllegalStateException("endTriangle called without beginTriangle call before");
            throw new IllegalStateException("endTriangle called without three vertex calls before");
        }
        this.trianglePoint = -1;
        this.trianglesUsed++;
        return this;
    }

    private Vector vertex_t1 = Vector.allocate();

    public RenderingStream vertex(final float x,
                                  final float y,
                                  final float z,
                                  final float u,
                                  final float v,
                                  final float r,
                                  final float g,
                                  final float b,
                                  final float a)
    {
        if(this.trianglePoint == -1)
            throw new IllegalStateException("vertex called without beginTriangle call before");
        if(this.trianglePoint >= 3)
            throw new IllegalStateException("missing endTriangle call before");
        Vector p = this.matrixStack.mat.apply(this.vertex_t1,
                                              Vector.set(this.vertex_t1,
                                                         x,
                                                         y,
                                                         z));
        int vi = (this.trianglePoint + 3 * this.trianglesUsed) * 3;
        int ci = (this.trianglePoint + 3 * this.trianglesUsed) * 4;
        int ti = (this.trianglePoint + 3 * this.trianglesUsed) * 2;
        this.trianglePoint++;
        this.colorArray[ci++] = r;
        this.colorArray[ci++] = g;
        this.colorArray[ci++] = b;
        this.colorArray[ci] = a;
        this.vertexArray[vi++] = p.getX();
        this.vertexArray[vi++] = p.getY();
        this.vertexArray[vi] = p.getZ();
        this.texCoordArray[ti++] = u;
        this.texCoordArray[ti] = v;
        return this;
    }

    public RenderingStream vertex(final Vector p,
                                  final float u,
                                  final float v,
                                  final float r,
                                  final float g,
                                  final float b,
                                  final float a)
    {
        return vertex(p.getX(), p.getY(), p.getZ(), u, v, r, g, b, a);
    }

    public RenderingStream vertex(final Vector p,
                                  final float u,
                                  final float v,
                                  final Color c)
    {
        return vertex(p.getX(),
                      p.getY(),
                      p.getZ(),
                      u,
                      v,
                      Color.GetRValue(c) / 255f,
                      Color.GetGValue(c) / 255f,
                      Color.GetBValue(c) / 255f,
                      Color.GetAValue(c) / 255f);
    }

    public RenderingStream vertex(final float x,
                                  final float y,
                                  final float z,
                                  final float u,
                                  final float v,
                                  final Color c)
    {
        return vertex(x,
                      y,
                      z,
                      u,
                      v,
                      Color.GetRValue(c) / 255f,
                      Color.GetGValue(c) / 255f,
                      Color.GetBValue(c) / 255f,
                      Color.GetAValue(c) / 255f);
    }

    /** insert a new rectangle from &lt;<code>x1</code>, <code>y1</code>, 0&gt;
     * to &lt;<code>x2</code>, <code>y2</code>, 0&gt;
     * 
     * @param x1
     *            the first point's x coordinate
     * @param y1
     *            the first point's y coordinate
     * @param x2
     *            the second point's x coordinate
     * @param y2
     *            the second point's y coordinate
     * @param u1
     *            the first point's u coordinate
     * @param v1
     *            the first point's v coordinate
     * @param u2
     *            the second point's u coordinate
     * @param v2
     *            the second point's v coordinate
     * @param color
     *            the new rectangle's color
     * @param texture
     *            the texture or <code>Polygon.NO_TEXTURE</code>
     * @return <code>this</code> */
    public RenderingStream addRect(final float x1,
                                   final float y1,
                                   final float x2,
                                   final float y2,
                                   final float u1,
                                   final float v1,
                                   final float u2,
                                   final float v2,
                                   final Color color,
                                   final TextureAtlas.TextureHandle texture)
    {
        beginTriangle(texture);
        vertex(x1, y1, 0, u1, v1, color);
        vertex(x2, y1, 0, u2, v1, color);
        vertex(x2, y2, 0, u2, v2, color);
        endTriangle();
        beginTriangle(texture);
        vertex(x2, y2, 0, u2, v2, color);
        vertex(x1, y2, 0, u1, v2, color);
        vertex(x1, y1, 0, u1, v1, color);
        endTriangle();
        return this;
    }

    /** @return the current matrix */
    public Matrix getMatrix()
    {
        return this.matrixStack.mat;
    }

    /**
	 * 
	 */
    public void pushMatrixStack()
    {
        Matrix oldMat = this.matrixStack.mat;
        MatrixNode newNode = allocMatrix();
        Matrix.set(newNode.mat, oldMat);
        newNode.next = this.matrixStack;
        this.matrixStack = newNode;
    }

    /** @param mat
     *            the matrix to set to */
    public void setMatrix(final Matrix mat)
    {
        if(mat == null)
            throw new NullPointerException();
        Matrix.set(this.matrixStack.mat, mat);
    }

    /** @param mat
     *            the matrix to concat to */
    public void concatMatrix(final Matrix mat)
    {
        if(mat == null)
            throw new NullPointerException();
        mat.concat(this.matrixStack.mat, this.matrixStack.mat);
    }

    /**
	 * 
	 */
    public void popMatrixStack()
    {
        if(this.matrixStack.next == null)
            throw new IllegalStateException("can not pop the last matrix off the stack");
        MatrixNode node = this.matrixStack;
        this.matrixStack = this.matrixStack.next;
        freeMatrix(node);
    }

    /** @param rs
     *            the rendering stream
     * @return <code>this</code> */
    public RenderingStream add(final RenderingStream rs)
    {
        if(rs == null)
            throw new NullPointerException();
        assert rs != this;
        for(int tri = 0, vi = 0, ci = 0, ti = 0; tri < rs.trianglesUsed; tri++)
        {
            beginTriangle(rs.textureArray[tri]);
            for(int i = 0; i < 3; i++)
            {
                float x = rs.vertexArray[vi++];
                float y = rs.vertexArray[vi++];
                float z = rs.vertexArray[vi++];
                float u = rs.texCoordArray[ti++];
                float v = rs.texCoordArray[ti++];
                float r = rs.colorArray[ci++];
                float g = rs.colorArray[ci++];
                float b = rs.colorArray[ci++];
                float a = rs.colorArray[ci++];
                vertex(x, y, z, u, v, r, g, b, a);
            }
            endTriangle();
        }
        return this;
    }

    private FloatBuffer vertexBuffer = null;
    private FloatBuffer texCoordBuffer = null;
    private FloatBuffer colorBuffer = null;

    private FloatBuffer checkBufferLength(final FloatBuffer origBuffer,
                                          final int minLength)
    {
        if(origBuffer == null || origBuffer.capacity() < minLength)
            return Main.platform.createFloatBuffer(minLength);
        return origBuffer;
    }

    /** @return this */
    public RenderingStream render()
    {
        if(this.trianglePoint != -1)
            throw new IllegalStateException("render called between beginTriangle and endTriangle");
        if(this.trianglesUsed <= 0)
            return this;
        if(USE_VERTEX_ARRAY_AND_TEXTURE_ATLAS)
        {
            this.vertexBuffer = checkBufferLength(this.vertexBuffer,
                                                  this.vertexArray.length);
            this.texCoordBuffer = checkBufferLength(this.texCoordBuffer,
                                                    this.texCoordArray.length);
            this.colorBuffer = checkBufferLength(this.colorBuffer,
                                                 this.colorArray.length);
            Image texImage = TextureAtlas.transformTextureCoords(this.texCoordArray,
                                                                 this.textureArray,
                                                                 this.trianglesUsed,
                                                                 this.transformedTexCoordArray);
            Main.opengl.glFinish();
            Main.opengl.glEnableClientState(Main.opengl.GL_COLOR_ARRAY());
            Main.opengl.glEnableClientState(Main.opengl.GL_TEXTURE_COORD_ARRAY());
            Main.opengl.glEnableClientState(Main.opengl.GL_VERTEX_ARRAY());
            this.colorBuffer.clear();
            this.colorBuffer.put(this.colorArray, 0, this.trianglesUsed * 4 * 3);
            this.colorBuffer.flip();
            this.texCoordBuffer.clear();
            this.texCoordBuffer.put(this.transformedTexCoordArray,
                                    0,
                                    this.trianglesUsed * 2 * 3);
            this.vertexBuffer.clear();
            this.vertexBuffer.put(this.vertexArray,
                                  0,
                                  this.trianglesUsed * 3 * 3);
            this.vertexBuffer.flip();
            Main.opengl.glVertexPointer(this.vertexBuffer);
            Main.opengl.glTexCoordPointer(this.texCoordBuffer);
            Main.opengl.glColorPointer(this.colorBuffer);
            texImage.selectTexture();
            Main.opengl.glDrawArrays(Main.opengl.GL_TRIANGLES(),
                                     0,
                                     this.trianglesUsed * 3);
            Main.opengl.glFinish();
            Main.opengl.glDisableClientState(Main.opengl.GL_COLOR_ARRAY());
            Main.opengl.glDisableClientState(Main.opengl.GL_TEXTURE_COORD_ARRAY());
            Main.opengl.glDisableClientState(Main.opengl.GL_VERTEX_ARRAY());
        }
        else if(USE_TEXTURE_ATLAS)
        {
            Image texImage = TextureAtlas.transformTextureCoords(this.texCoordArray,
                                                                 this.textureArray,
                                                                 this.trianglesUsed,
                                                                 this.transformedTexCoordArray);
            texImage.selectTexture();
            Main.opengl.glBegin(Main.opengl.GL_TRIANGLES());
            int ti = 0, ci = 0, vi = 0;
            for(int tri = 0; tri < this.trianglesUsed; tri++)
            {
                for(int vertex = 0; vertex < 3; vertex++)
                {
                    float r = this.colorArray[ci++];
                    float g = this.colorArray[ci++];
                    float b = this.colorArray[ci++];
                    float a = this.colorArray[ci++];
                    Main.opengl.glColor4f(r, g, b, a);
                    float u = this.texCoordArray[ti++];
                    float v = this.texCoordArray[ti++];
                    Main.opengl.glTexCoord2f(u, v);
                    float x = this.vertexArray[vi++];
                    float y = this.vertexArray[vi++];
                    float z = this.vertexArray[vi++];
                    Main.opengl.glVertex3f(x, y, z);
                }
            }
            Main.opengl.glEnd();
        }
        else
        {
            boolean insideBeginEnd = false;
            int ti = 0, ci = 0, vi = 0;
            for(int tri = 0; tri < this.trianglesUsed; tri++)
            {
                if(!this.textureArray[tri].getImage().isSelected())
                {
                    if(insideBeginEnd)
                    {
                        Main.opengl.glEnd();
                        insideBeginEnd = false;
                    }
                    this.textureArray[tri].getImage().selectTexture();
                }
                if(!insideBeginEnd)
                {
                    Main.opengl.glBegin(Main.opengl.GL_TRIANGLES());
                    insideBeginEnd = true;
                }
                for(int vertex = 0; vertex < 3; vertex++)
                {
                    float r = this.colorArray[ci++];
                    float g = this.colorArray[ci++];
                    float b = this.colorArray[ci++];
                    float a = this.colorArray[ci++];
                    Main.opengl.glColor4f(r, g, b, a);
                    float u = this.texCoordArray[ti++];
                    float v = this.texCoordArray[ti++];
                    Main.opengl.glTexCoord2f(u, v);
                    float x = this.vertexArray[vi++];
                    float y = this.vertexArray[vi++];
                    float z = this.vertexArray[vi++];
                    Main.opengl.glVertex3f(x, y, z);
                }
            }
            if(insideBeginEnd)
                Main.opengl.glEnd();
        }
        return this;
    }
}
