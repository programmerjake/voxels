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

import java.io.*;

/** @author jacob */
public class Vector implements Allocatable
{
    /** x coordinate */
    private float x;
    /** y coordinate */
    private float y;
    /** z coordinate */
    private float z;

    /** @return the x */
    public float getX()
    {
        return this.x;
    }

    /** @param x
     *            the x to set */
    public void setX(final float x)
    {
        this.x = x;
    }

    /** @return the y */
    public float getY()
    {
        return this.y;
    }

    /** @param y
     *            the y to set */
    public void setY(final float y)
    {
        this.y = y;
    }

    /** @return the z */
    public float getZ()
    {
        return this.z;
    }

    /** @param z
     *            the z to set */
    public void setZ(final float z)
    {
        this.z = z;
    }

    /** create a vector */
    @Deprecated
    Vector()
    {
        this.x = 0;
        this.y = 0;
        this.z = 0;
    }

    /** create a vector &lt;<code>x</code>, <code>y</code>, <code>z</code>&gt;
     * 
     * @param x
     *            the x coordinate
     * @param y
     *            the y coordinate
     * @param z
     *            the z coordinate */
    private Vector(final float x, final float y, final float z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /** copy a vector
     * 
     * @param rt
     *            the vector to copy */
    protected Vector(final Vector rt)
    {
        this(rt.x, rt.y, rt.z);
    }

    /** vector add
     * 
     * @param rt
     *            the vector to add
     * @return <code>this</code> + <code>rt</code> */
    @Deprecated
    public Vector add(final Vector rt)
    {
        return new Vector(this.x + rt.x, this.y + rt.y, this.z + rt.z);
    }

    @Deprecated
    public Vector add(final float rx, final float ry, final float rz)
    {
        return new Vector(this.x + rx, this.y + ry, this.z + rz);
    }

    public static Vector add(final Vector dest, final Vector l, final Vector r)
    {
        dest.x = l.x + r.x;
        dest.y = l.y + r.y;
        dest.z = l.z + r.z;
        return dest;
    }

    public static Vector add(final Vector dest,
                             final Vector l,
                             final float rx,
                             final float ry,
                             final float rz)
    {
        if(dest instanceof ImmutableVector)
            ((ImmutableVector)dest).unsupportedOp();
        dest.x = l.x + rx;
        dest.y = l.y + ry;
        dest.z = l.z + rz;
        return dest;
    }

    public Vector addAndSet(final Vector rt)
    {
        this.x += rt.x;
        this.y += rt.y;
        this.z += rt.z;
        return this;
    }

    public Vector addAndSet(final float rx, final float ry, final float rz)
    {
        this.x += rx;
        this.y += ry;
        this.z += rz;
        return this;
    }

    /** vector subtract
     * 
     * @param rt
     *            the vector to add
     * @return <code>this</code> - <code>rt</code> */
    @Deprecated
    public Vector sub(final Vector rt)
    {
        return new Vector(this.x - rt.x, this.y - rt.y, this.z - rt.z);
    }

    @Deprecated
    public Vector sub(final float rx, final float ry, final float rz)
    {
        return new Vector(this.x - rx, this.y - ry, this.z - rz);
    }

    public static Vector sub(final Vector dest, final Vector l, final Vector r)
    {
        if(dest instanceof ImmutableVector)
            ((ImmutableVector)dest).unsupportedOp();
        dest.x = l.x - r.x;
        dest.y = l.y - r.y;
        dest.z = l.z - r.z;
        return dest;
    }

    public static Vector sub(final Vector dest,
                             final Vector l,
                             final float rx,
                             final float ry,
                             final float rz)
    {
        if(dest instanceof ImmutableVector)
            ((ImmutableVector)dest).unsupportedOp();
        dest.x = l.x - rx;
        dest.y = l.y - ry;
        dest.z = l.z - rz;
        return dest;
    }

    public static Vector sub(final Vector dest,
                             final float lx,
                             final float ly,
                             final float lz,
                             final Vector r)
    {
        if(dest instanceof ImmutableVector)
            ((ImmutableVector)dest).unsupportedOp();
        dest.x = lx - r.x;
        dest.y = ly - r.y;
        dest.z = lz - r.z;
        return dest;
    }

    public Vector subAndSet(final Vector rt)
    {
        this.x -= rt.x;
        this.y -= rt.y;
        this.z -= rt.z;
        return this;
    }

    public Vector subAndSet(final float rx, final float ry, final float rz)
    {
        this.x -= rx;
        this.y -= ry;
        this.z -= rz;
        return this;
    }

    public Vector rsubAndSet(final Vector rt)
    {
        this.x = rt.x - this.x;
        this.y = rt.y - this.y;
        this.z = rt.z - this.z;
        return this;
    }

    public Vector rsubAndSet(final float rx, final float ry, final float rz)
    {
        this.x = rx - this.x;
        this.y = ry - this.y;
        this.z = rz - this.z;
        return this;
    }

    public Vector rdivAndSet(final Vector rt)
    {
        this.x = rt.x / this.x;
        this.y = rt.y / this.y;
        this.z = rt.z / this.z;
        return this;
    }

    public Vector rdivAndSet(final float rx, final float ry, final float rz)
    {
        this.x = rx / this.x;
        this.y = ry / this.y;
        this.z = rz / this.z;
        return this;
    }

    /** vector negate
     * 
     * @return -<code>this</code> */
    @Deprecated
    public Vector neg()
    {
        return new Vector(-this.x, -this.y, -this.z);
    }

    public Vector negAndSet()
    {
        this.x = -this.x;
        this.y = -this.y;
        this.z = -this.z;
        return this;
    }

    public static Vector neg(final Vector dest, final Vector src)
    {
        if(dest instanceof ImmutableVector)
            ((ImmutableVector)dest).unsupportedOp();
        dest.x = -src.x;
        dest.y = -src.y;
        dest.z = -src.z;
        return dest;
    }

    public static Vector set(final Vector dest,
                             final float x,
                             final float y,
                             final float z)
    {
        if(dest instanceof ImmutableVector)
            ((ImmutableVector)dest).unsupportedOp();
        dest.x = x;
        dest.y = y;
        dest.z = z;
        return dest;
    }

    public Vector set(final float x, final float y, final float z)
    {
        return set(this, x, y, z);
    }

    /** component-wise multiply
     * 
     * @param rt
     *            the vector to multiply
     * @return &lt;<code>this.x</code> &times; <code>rt.x</code>,
     *         <code>this.y</code> &times; <code>rt.y</code>,
     *         <code>this.z</code> &times; <code>rt.z</code>&gt; */
    @Deprecated
    public Vector mul(final Vector rt)
    {
        return new Vector(this.x * rt.x, this.y * rt.y, this.z * rt.z);
    }

    @Deprecated
    public Vector mul(final float rx, final float ry, final float rz)
    {
        return new Vector(this.x * rx, this.y * ry, this.z * rz);
    }

    public Vector mulAndSet(final Vector rt)
    {
        this.x *= rt.x;
        this.y *= rt.y;
        this.z *= rt.z;
        return this;
    }

    public Vector mulAndSet(final float rx, final float ry, final float rz)
    {
        this.x *= rx;
        this.y *= ry;
        this.z *= rz;
        return this;
    }

    public Vector mulAndSet(final float rt)
    {
        this.x *= rt;
        this.y *= rt;
        this.z *= rt;
        return this;
    }

    public static Vector mul(final Vector dest, final Vector l, final Vector r)
    {
        if(dest instanceof ImmutableVector)
            ((ImmutableVector)dest).unsupportedOp();
        dest.x = l.x * r.x;
        dest.y = l.y * r.y;
        dest.z = l.z * r.z;
        return dest;
    }

    public static Vector mul(final Vector dest,
                             final Vector l,
                             final float rx,
                             final float ry,
                             final float rz)
    {
        if(dest instanceof ImmutableVector)
            ((ImmutableVector)dest).unsupportedOp();
        dest.x = l.x * rx;
        dest.y = l.y * ry;
        dest.z = l.z * rz;
        return dest;
    }

    public static Vector mul(final Vector dest, final Vector l, final float r)
    {
        if(dest instanceof ImmutableVector)
            ((ImmutableVector)dest).unsupportedOp();
        dest.x = l.x * r;
        dest.y = l.y * r;
        dest.z = l.z * r;
        return dest;
    }

    public Vector divAndSet(final Vector rt)
    {
        this.x /= rt.x;
        this.y /= rt.y;
        this.z /= rt.z;
        return this;
    }

    public Vector divAndSet(final float rx, final float ry, final float rz)
    {
        this.x /= rx;
        this.y /= ry;
        this.z /= rz;
        return this;
    }

    public Vector divAndSet(final float rt)
    {
        this.x /= rt;
        this.y /= rt;
        this.z /= rt;
        return this;
    }

    public static Vector div(final Vector dest, final Vector l, final Vector r)
    {
        if(dest instanceof ImmutableVector)
            ((ImmutableVector)dest).unsupportedOp();
        dest.x = l.x / r.x;
        dest.y = l.y / r.y;
        dest.z = l.z / r.z;
        return dest;
    }

    public static Vector div(final Vector dest,
                             final float lx,
                             final float ly,
                             final float lz,
                             final Vector r)
    {
        if(dest instanceof ImmutableVector)
            ((ImmutableVector)dest).unsupportedOp();
        dest.x = lx / r.x;
        dest.y = ly / r.y;
        dest.z = lz / r.z;
        return dest;
    }

    public static Vector div(final Vector dest,
                             final Vector l,
                             final float rx,
                             final float ry,
                             final float rz)
    {
        if(dest instanceof ImmutableVector)
            ((ImmutableVector)dest).unsupportedOp();
        dest.x = l.x / rx;
        dest.y = l.y / ry;
        dest.z = l.z / rz;
        return dest;
    }

    public static Vector div(final Vector dest, final Vector l, final float r)
    {
        if(dest instanceof ImmutableVector)
            ((ImmutableVector)dest).unsupportedOp();
        dest.x = l.x / r;
        dest.y = l.y / r;
        dest.z = l.z / r;
        return dest;
    }

    /** component-wise divide
     * 
     * @param rt
     *            the vector to multiply
     * @return &lt;<code>this.x</code> &divide; <code>rt.x</code>,
     *         <code>this.y</code> &divide; <code>rt.y</code>,
     *         <code>this.z</code> &divide; <code>rt.z</code>&gt; */
    @Deprecated
    public Vector div(final Vector rt)
    {
        return new Vector(this.x / rt.x, this.y / rt.y, this.z / rt.z);
    }

    @Deprecated
    public Vector div(final float rx, final float ry, final float rz)
    {
        return new Vector(this.x / rx, this.y / ry, this.z / rz);
    }

    /** multiply by scalar
     * 
     * @param rt
     *            scalar to multiply by
     * @return <code>this</code> &times; <code>rt</code> */
    @Deprecated
    public Vector mul(final float rt)
    {
        return new Vector(this.x * rt, this.y * rt, this.z * rt);
    }

    /** divide by scalar
     * 
     * @param rt
     *            scalar to divide by
     * @return <code>this</code> &divide; <code>rt</code> */
    @Deprecated
    public Vector div(final float rt)
    {
        return new Vector(this.x / rt, this.y / rt, this.z / rt);
    }

    /** dot product
     * 
     * @param rt
     *            vector to dot product with
     * @return <code>this</code> &middot; <code>rt</code>
     * @see #cross(Vector rt) */
    public float dot(final Vector rt)
    {
        return this.x * rt.x + this.y * rt.y + this.z * rt.z;
    }

    public float dot(final float x, final float y, final float z)
    {
        return this.x * x + this.y * y + this.z * z;
    }

    /** absolute value squared
     * 
     * @return |<code>this</code>|&sup2;
     * @see #abs() */
    public float abs_squared()
    {
        return dot(this);
    }

    /** cross product
     * 
     * @param rt
     *            vector to cross product with
     * @return <code>this</code> &times; <code>rt</code>
     * @see #dot(Vector rt) */
    @Deprecated
    public Vector cross(final Vector rt)
    {
        return new Vector(this.y * rt.z - this.z * rt.y, this.z * rt.x - this.x
                * rt.z, this.x * rt.y - this.y * rt.x);
    }

    @Deprecated
    public Vector cross(final float rx, final float ry, final float rz)
    {
        return new Vector(this.y * rz - this.z * ry,
                          this.z * rx - this.x * rz,
                          this.x * ry - this.y * rx);
    }

    public Vector crossAndSet(final Vector rt)
    {
        float newX = this.y * rt.z - this.z * rt.y;
        float newY = this.z * rt.x - this.x * rt.z;
        float newZ = this.x * rt.y - this.y * rt.x;
        this.x = newX;
        this.y = newY;
        this.z = newZ;
        return this;
    }

    public Vector crossAndSet(final float rx, final float ry, final float rz)
    {
        float newX = this.y * rz - this.z * ry;
        float newY = this.z * rx - this.x * rz;
        float newZ = this.x * ry - this.y * rx;
        this.x = newX;
        this.y = newY;
        this.z = newZ;
        return this;
    }

    public Vector rcrossAndSet(final Vector rt)
    {
        float newX = rt.y * this.z - rt.z * this.y;
        float newY = rt.z * this.x - rt.x * this.z;
        float newZ = rt.x * this.y - rt.y * this.x;
        this.x = newX;
        this.y = newY;
        this.z = newZ;
        return this;
    }

    public Vector rcrossAndSet(final float rx, final float ry, final float rz)
    {
        float newX = ry * this.z - rz * this.y;
        float newY = rz * this.x - rx * this.z;
        float newZ = rx * this.y - ry * this.x;
        this.x = newX;
        this.y = newY;
        this.z = newZ;
        return this;
    }

    public Vector set(final Vector rt)
    {
        this.x = rt.x;
        this.y = rt.y;
        this.z = rt.z;
        return this;
    }

    public static Vector
        cross(final Vector dest, final Vector l, final Vector r)
    {
        if(dest instanceof ImmutableVector)
            ((ImmutableVector)dest).unsupportedOp();
        float newX = l.y * r.z - l.z * r.y;
        float newY = l.z * r.x - l.x * r.z;
        float newZ = l.x * r.y - l.y * r.x;
        dest.x = newX;
        dest.y = newY;
        dest.z = newZ;
        return dest;
    }

    public static Vector cross(final Vector dest,
                               final Vector l,
                               final float rx,
                               final float ry,
                               final float rz)
    {
        if(dest instanceof ImmutableVector)
            ((ImmutableVector)dest).unsupportedOp();
        float newX = l.y * rz - l.z * ry;
        float newY = l.z * rx - l.x * rz;
        float newZ = l.x * ry - l.y * rx;
        dest.x = newX;
        dest.y = newY;
        dest.z = newZ;
        return dest;
    }

    public static Vector cross(final Vector dest,
                               final float lx,
                               final float ly,
                               final float lz,
                               final Vector r)
    {
        if(dest instanceof ImmutableVector)
            ((ImmutableVector)dest).unsupportedOp();
        float newX = ly * r.z - lz * r.y;
        float newY = lz * r.x - lx * r.z;
        float newZ = lx * r.y - ly * r.x;
        dest.x = newX;
        dest.y = newY;
        dest.z = newZ;
        return dest;
    }

    /** checks for equality
     * 
     * @param rt
     *            vector to compare to
     * @return true if <code>this</code> == <code>rt</code> */
    public boolean equals(final Vector rt)
    {
        return this.x == rt.x && this.y == rt.y && this.z == rt.z;
    }

    public boolean equals(final float rx, final float ry, final float rz)
    {
        return this.x == rx && this.y == ry && this.z == rz;
    }

    /** absolute value
     * 
     * @return |<code>this</code>|
     * @see #abs_squared() */
    public float abs()
    {
        return (float)Math.sqrt(abs_squared());
    }

    /** normalize
     * 
     * @return this vector normalized or &lt;0, 0, 0&gt; if <code>this</code> ==
     *         &lt;0, 0, 0&gt; */
    @Deprecated
    public Vector normalize()
    {
        float a = abs();
        if(a == 0)
            a = 1;
        return this.div(a);
    }

    @Deprecated
    public static Vector normalize(final float x, final float y, final float z)
    {
        float a = (float)Math.sqrt(x * x + y * y + z * z);
        if(a == 0)
            a = 1;
        return allocate(x / a, y / a, z / a);
    }

    public Vector normalizeAndSet()
    {
        float a = abs();
        if(a == 0)
            a = 1;
        return divAndSet(a);
    }

    public static Vector normalize(final Vector dest, final Vector src)
    {
        float a = src.abs();
        if(a == 0)
            a = 1;
        return div(dest, src, a);
    }

    public static Vector normalize(final Vector dest,
                                   final float x,
                                   final float y,
                                   final float z)
    {
        float a = (float)Math.sqrt(x * x + y * y + z * z);
        if(a == 0)
            a = 1;
        return set(dest, x / a, y / a, z / a);
    }

    /** calls glVertex3f with <code>v</code>
     * 
     * @param v
     *            the vector to call glVertex3f with */
    public static void glVertex(final Vector v)
    {
        Main.opengl.glVertex3f(v.x, v.y, v.z);
    }

    @Override
    public String toString()
    {
        return "<" + Float.toString(this.x) + ", " + Float.toString(this.y)
                + ", " + Float.toString(this.z) + ">";
    }

    private static void validateFloat(final float f) throws IOException
    {
        if(Float.isInfinite(f) || Float.isNaN(f))
            throw new IOException("out of range");
    }

    /** read from a <code>DataInput</code>
     * 
     * @param i
     *            <code>DataInput</code> to read from
     * @return the read <code>Vector</code>
     * @throws IOException
     *             the exception thrown */
    public static Vector read(final DataInput i) throws IOException
    {
        Vector retval = new Vector();
        retval.x = i.readFloat();
        validateFloat(retval.x);
        retval.y = i.readFloat();
        validateFloat(retval.y);
        retval.z = i.readFloat();
        validateFloat(retval.z);
        return retval;
    }

    /** write to a <code>DataOutput</code>
     * 
     * @param o
     *            <code>OutputStream</code> to write to
     * @throws IOException
     *             the exception thrown */
    public void write(final DataOutput o) throws IOException
    {
        o.writeFloat(this.x);
        o.writeFloat(this.y);
        o.writeFloat(this.z);
    }

    private static class ImmutableVector extends Vector
    {
        public ImmutableVector(final Vector v)
        {
            super(v);
        }

        Vector unsupportedOp()
        {
            throw new UnsupportedOperationException("can't modify a immutable vector");
        }

        @Override
        public void setX(final float x)
        {
            unsupportedOp();
        }

        @Override
        public void setY(final float y)
        {
            unsupportedOp();
        }

        @Override
        public void setZ(final float z)
        {
            unsupportedOp();
        }

        @Override
        public Vector addAndSet(final Vector rt)
        {
            return unsupportedOp();
        }

        @Override
        public Vector addAndSet(final float rx, final float ry, final float rz)
        {
            return unsupportedOp();
        }

        @Override
        public Vector subAndSet(final Vector rt)
        {
            return unsupportedOp();
        }

        @Override
        public Vector subAndSet(final float rx, final float ry, final float rz)
        {
            return unsupportedOp();
        }

        @Override
        public Vector rsubAndSet(final Vector rt)
        {
            return unsupportedOp();
        }

        @Override
        public Vector
            rsubAndSet(final float rx, final float ry, final float rz)
        {
            return unsupportedOp();
        }

        @Override
        public Vector rdivAndSet(final Vector rt)
        {
            return unsupportedOp();
        }

        @Override
        public Vector
            rdivAndSet(final float rx, final float ry, final float rz)
        {
            return unsupportedOp();
        }

        @Override
        public Vector negAndSet()
        {
            return unsupportedOp();
        }

        @Override
        public Vector set(final float x, final float y, final float z)
        {
            return unsupportedOp();
        }

        @Override
        public Vector mulAndSet(final Vector rt)
        {
            return unsupportedOp();
        }

        @Override
        public Vector mulAndSet(final float rx, final float ry, final float rz)
        {
            return unsupportedOp();
        }

        @Override
        public Vector mulAndSet(final float rt)
        {
            return unsupportedOp();
        }

        @Override
        public Vector divAndSet(final Vector rt)
        {
            return unsupportedOp();
        }

        @Override
        public Vector divAndSet(final float rx, final float ry, final float rz)
        {
            return unsupportedOp();
        }

        @Override
        public Vector divAndSet(final float rt)
        {
            return unsupportedOp();
        }

        @Override
        public Vector crossAndSet(final Vector rt)
        {
            return unsupportedOp();
        }

        @Override
        public Vector
            crossAndSet(final float rx, final float ry, final float rz)
        {
            return unsupportedOp();
        }

        @Override
        public Vector rcrossAndSet(final Vector rt)
        {
            return unsupportedOp();
        }

        @Override
        public Vector rcrossAndSet(final float rx,
                                   final float ry,
                                   final float rz)
        {
            return unsupportedOp();
        }

        @Override
        public Vector set(final Vector rt)
        {
            return unsupportedOp();
        }

        @Override
        public Vector normalizeAndSet()
        {
            return unsupportedOp();
        }

        @Override
        public Vector getImmutable()
        {
            return this;
        }

        @Override
        public void free()
        {
        }
    }

    public Vector getImmutable()
    {
        return new ImmutableVector(this);
    }

    public Vector getImmutableAndFree()
    {
        Vector retval = new ImmutableVector(this);
        free();
        return retval;
    }

    private static final Allocator<Vector> allocator = new Allocator<Vector>()
    {
        @Override
        protected Vector allocateInternal()
        {
            return new Vector();
        }
    }; // must
       // be
       // before
       // all
       // calls
       // to
       // allocate
    public static final Vector ZERO = Vector.allocate(0, 0, 0)
                                            .getImmutableAndFree();
    public static final Vector X = Vector.allocate(1, 0, 0)
                                         .getImmutableAndFree();
    public static final Vector Y = Vector.allocate(0, 1, 0)
                                         .getImmutableAndFree();
    public static final Vector Z = Vector.allocate(0, 0, 1)
                                         .getImmutableAndFree();
    public static final Vector XY = Vector.allocate(1, 1, 0)
                                          .getImmutableAndFree();
    public static final Vector YZ = Vector.allocate(0, 1, 1)
                                          .getImmutableAndFree();
    public static final Vector XZ = Vector.allocate(1, 0, 1)
                                          .getImmutableAndFree();
    public static final Vector XYZ = Vector.allocate(1, 1, 1)
                                           .getImmutableAndFree();
    public static final Vector NX = Vector.allocate(-1, 0, 0)
                                          .getImmutableAndFree();
    public static final Vector NY = Vector.allocate(0, -1, 0)
                                          .getImmutableAndFree();
    public static final Vector NZ = Vector.allocate(0, 0, -1)
                                          .getImmutableAndFree();
    public static final Vector NXY = Vector.allocate(-1, 1, 0)
                                           .getImmutableAndFree();
    public static final Vector NYZ = Vector.allocate(0, -1, 1)
                                           .getImmutableAndFree();
    public static final Vector NXZ = Vector.allocate(-1, 0, 1)
                                           .getImmutableAndFree();
    public static final Vector NXYZ = Vector.allocate(-1, 1, 1)
                                            .getImmutableAndFree();
    public static final Vector XNY = Vector.allocate(1, -1, 0)
                                           .getImmutableAndFree();
    public static final Vector YNZ = Vector.allocate(0, 1, -1)
                                           .getImmutableAndFree();
    public static final Vector XNZ = Vector.allocate(1, 0, -1)
                                           .getImmutableAndFree();
    public static final Vector NXNY = Vector.allocate(-1, -1, 0)
                                            .getImmutableAndFree();
    public static final Vector NYNZ = Vector.allocate(0, -1, -1)
                                            .getImmutableAndFree();
    public static final Vector NXNZ = Vector.allocate(-1, 0, -1)
                                            .getImmutableAndFree();
    public static final Vector XNYZ = Vector.allocate(1, -1, 1)
                                            .getImmutableAndFree();
    public static final Vector NXNYZ = Vector.allocate(-1, -1, 1)
                                             .getImmutableAndFree();
    public static final Vector XYNZ = Vector.allocate(1, 1, -1)
                                            .getImmutableAndFree();
    public static final Vector NXYNZ = Vector.allocate(-1, 1, -1)
                                             .getImmutableAndFree();
    public static final Vector XNYNZ = Vector.allocate(1, -1, -1)
                                             .getImmutableAndFree();
    public static final Vector NXNYNZ = Vector.allocate(-1, -1, -1)
                                              .getImmutableAndFree();

    public static Vector set(final Vector dest, final Vector src)
    {
        return set(dest, src.x, src.y, src.z);
    }

    public static Vector allocate(final float x, final float y, final float z)
    {
        return allocator.allocate().set(x, y, z);
    }

    public static Vector allocate(final Vector rt)
    {
        return allocate(rt.x, rt.y, rt.z);
    }

    public static Vector allocate(final float v)
    {
        return allocate(v, v, v);
    }

    public static Vector allocate()
    {
        return allocate(0, 0, 0);
    }

    @Override
    public void free()
    {
        allocator.free(this);
    }

    @Override
    public Allocatable dup()
    {
        return allocate(this);
    }

    public float getPhi()
    {
        float v = this.y / abs();
        v = Math.max(-1, Math.min(1, v));
        return (float)Math.asin(v);
    }

    public float getTheta()
    {
        return (float)Math.atan2(this.x, this.z);
    }

    public float getSphericalR()
    {
        return abs();
    }

    public float getCylindricalR()
    {
        return (float)Math.sqrt(this.x * this.x + this.z * this.z);
    }

    public static Vector setToSphericalCoordinates(final Vector retval,
                                                   final float r,
                                                   final float theta,
                                                   final float phi)
    {
        float cosPhi = (float)Math.cos(phi);
        return set(retval,
                   r * (float)Math.sin(theta) * cosPhi,
                   r * (float)Math.sin(phi),
                   r * (float)Math.cos(theta) * cosPhi);
    }

    public Vector setToSphericalCoordinates(final float r,
                                            final float theta,
                                            final float phi)
    {
        float cosPhi = (float)Math.cos(phi);
        return set(r * (float)Math.sin(theta) * cosPhi,
                   r * (float)Math.sin(phi),
                   r * (float)Math.cos(theta) * cosPhi);
    }

    public static Vector setToCylindricalCoordinates(final Vector retval,
                                                     final float r,
                                                     final float theta,
                                                     final float y)
    {
        return set(retval,
                   r * (float)Math.sin(theta),
                   y,
                   r * (float)Math.cos(theta));
    }

    public Vector setToCylindricalCoordinates(final float r,
                                              final float theta,
                                              final float y)
    {
        return set(r * (float)Math.sin(theta), y, r * (float)Math.cos(theta));
    }

    public float maximumAbs()
    {
        return Math.max(Math.max(Math.abs(this.x), Math.abs(this.y)),
                        Math.abs(this.z));
    }

    public Vector maximumNormalizeAndSet()
    {
        float a = maximumAbs();
        if(a == 0)
            a = 1;
        return divAndSet(a);
    }

    public static Vector maximumNormalize(final Vector dest, final Vector src)
    {
        float a = src.maximumAbs();
        if(a == 0)
            a = 1;
        return div(dest, src, a);
    }

    public static Vector maximumNormalize(final Vector dest,
                                          final float x,
                                          final float y,
                                          final float z)
    {
        float a = Math.max(Math.max(Math.abs(x), Math.abs(y)), Math.abs(z));
        if(a == 0)
            a = 1;
        return set(dest, x / a, y / a, z / a);
    }

    public float cylindricalMaximumAbs()
    {
        return Math.max((float)Math.sqrt(this.x * this.x + this.z * this.z),
                        Math.abs(this.y));
    }

    public Vector cylindricalMaximumNormalizeAndSet()
    {
        float a = cylindricalMaximumAbs();
        if(a == 0)
            a = 1;
        return divAndSet(a);
    }

    public static Vector cylindricalMaximumNormalize(final Vector dest,
                                                     final Vector src)
    {
        float a = src.cylindricalMaximumAbs();
        if(a == 0)
            a = 1;
        return div(dest, src, a);
    }

    public static Vector cylindricalMaximumNormalize(final Vector dest,
                                                     final float x,
                                                     final float y,
                                                     final float z)
    {
        float a = Math.max((float)Math.sqrt(x * x + z * z), Math.abs(y));
        if(a == 0)
            a = 1;
        return set(dest, x / a, y / a, z / a);
    }
}
