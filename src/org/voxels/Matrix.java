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

/** 4x4 matrix for 3D transformation with last row always equal to [0 0 0 1]
 * 
 * @author jacob */
public class Matrix implements Allocatable
{
    protected float elements[] = new float[12];

    /** get the value at the position (<code>x</code>, <code>y</code>)
     * 
     * @param x
     *            the column to get from (0 to 3)
     * @param y
     *            the row to get from (0 to 2)
     * @return the value at the position (<code>x</code>, <code>y</code>) */
    public float get(final int x, final int y)
    {
        return this.elements[x + y * 4];
    }

    /** set the value at the position (<code>x</code>, <code>y</code>)
     * 
     * @param x
     *            the column to set (0 to 3)
     * @param y
     *            the row to set (0 to 2)
     * @param value
     *            the new value for the position (<code>x</code>, <code>y</code>
     *            ) */
    public void set(final int x, final int y, final float value)
    {
        this.elements[x + y * 4] = value;
    }

    public static Matrix set(final Matrix dest,
                             final float x00,
                             final float x10,
                             final float x20,
                             final float x30,
                             final float x01,
                             final float x11,
                             final float x21,
                             final float x31,
                             final float x02,
                             final float x12,
                             final float x22,
                             final float x32)
    {
        if(dest instanceof ImmutableMatrix)
            ((ImmutableMatrix)dest).unsupportedOp();
        dest.elements[0 + 0 * 4] = x00;
        dest.elements[1 + 0 * 4] = x10;
        dest.elements[2 + 0 * 4] = x20;
        dest.elements[3 + 0 * 4] = x30;
        dest.elements[0 + 1 * 4] = x01;
        dest.elements[1 + 1 * 4] = x11;
        dest.elements[2 + 1 * 4] = x21;
        dest.elements[3 + 1 * 4] = x31;
        dest.elements[0 + 2 * 4] = x02;
        dest.elements[1 + 2 * 4] = x12;
        dest.elements[2 + 2 * 4] = x22;
        dest.elements[3 + 2 * 4] = x32;
        return dest;
    }

    public static Matrix set(final Matrix dest, final Matrix src)
    {
        if(dest instanceof ImmutableMatrix)
            ((ImmutableMatrix)dest).unsupportedOp();
        for(int i = 0; i < dest.elements.length; i++)
            dest.elements[i] = src.elements[i];
        return dest;
    }

    /** creates the identity matrix
     * 
     * @return the new identity matrix */
    @Deprecated
    public static Matrix identity()
    {
        return allocate(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0);
    }

    public static Matrix setToIdentity(final Matrix dest)
    {
        return set(dest, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0);
    }

    /** creates a rotation matrix
     * 
     * @param axis
     *            axis to rotate around
     * @param angle
     *            angle to rotate in radians
     * @return the new rotation matrix
     * @see #rotateX(double angle)
     * @see #rotateY(double angle)
     * @see #rotateZ(double angle) */
    @Deprecated
    public static Matrix rotate(final Vector axis, final double angle)
    {
        return setToRotate(allocate(), axis, angle);
    }

    /** creates a rotation matrix
     * 
     * @param dest
     *            the destination matrix
     * @param axis
     *            axis to rotate around
     * @param angle
     *            angle to rotate in radians
     * @return the new rotation matrix
     * @see #rotateX(double angle)
     * @see #rotateY(double angle)
     * @see #rotateZ(double angle) */
    public static Matrix setToRotate(final Matrix dest,
                                     final Vector axis,
                                     final double angle)
    {
        float r = axis.abs();
        if(r == 0.0f)
            return setToIdentity(dest);
        Vector axisv = Vector.div(Vector.allocate(), axis, r);
        float c, s, v;
        c = (float)Math.cos(angle);
        s = (float)Math.sin(angle);
        v = 1.0f - c; // Versine
        float xx, xy, xz, yy, yz, zz;
        xx = axisv.getX() * axisv.getX();
        xy = axisv.getX() * axisv.getY();
        xz = axisv.getX() * axisv.getZ();
        yy = axisv.getY() * axisv.getY();
        yz = axisv.getY() * axisv.getZ();
        zz = axisv.getZ() * axisv.getZ();
        Matrix retval = set(dest,
                            xx + (1 - xx) * c,
                            xy * v - axisv.getZ() * s,
                            xz * v + axisv.getY() * s,
                            0,
                            xy * v + axisv.getZ() * s,
                            yy + (1 - yy) * c,
                            yz * v - axisv.getX() * s,
                            0,
                            xz * v - axisv.getY() * s,
                            yz * v + axisv.getX() * s,
                            zz + (1 - zz) * c,
                            0);
        axisv.free();
        return retval;
    }

    /** creates a rotation matrix<br/>
     * the same as <code>Matrix.rotate(new Vector(1, 0, 0), angle)</code>
     * 
     * @param angle
     *            angle to rotate around the x axis in radians
     * @return the new rotation matrix
     * @see #rotate(Vector axis, double angle)
     * @see #rotateY(double angle)
     * @see #rotateZ(double angle) */
    @Deprecated
    public static Matrix rotateX(final double angle)
    {
        return rotate(Vector.X, angle);
    }

    public static Matrix setToRotateX(final Matrix dest, final double angle)
    {
        return setToRotate(dest, Vector.X, angle);
    }

    /** creates a rotation matrix<br/>
     * the same as <code>Matrix.rotate(new Vector(0, 1, 0), angle)</code>
     * 
     * @param angle
     *            angle to rotate around the y axis in radians
     * @return the new rotation matrix
     * @see #rotate(Vector axis, double angle)
     * @see #rotateX(double angle)
     * @see #rotateZ(double angle) */
    @Deprecated
    public static Matrix rotateY(final double angle)
    {
        return rotate(Vector.Y, angle);
    }

    public static Matrix setToRotateY(final Matrix dest, final double angle)
    {
        return setToRotate(dest, Vector.Y, angle);
    }

    /** creates a rotation matrix<br/>
     * the same as <code>Matrix.rotate(new Vector(0, 0, 1), angle)</code>
     * 
     * @param angle
     *            angle to rotate around the z axis in radians
     * @return the new rotation matrix
     * @see #rotate(Vector axis, double angle)
     * @see #rotateX(double angle)
     * @see #rotateY(double angle) */
    @Deprecated
    public static Matrix rotateZ(final double angle)
    {
        return rotate(Vector.Z, angle);
    }

    public static Matrix setToRotateZ(final Matrix dest, final double angle)
    {
        return setToRotate(dest, Vector.Z, angle);
    }

    /** creates a translation matrix
     * 
     * @param position
     *            the position to translate (0, 0, 0) to
     * @return the new translation matrix */
    @Deprecated
    public static Matrix translate(final Vector position)
    {
        return allocate(1,
                        0,
                        0,
                        position.getX(),
                        0,
                        1,
                        0,
                        position.getY(),
                        0,
                        0,
                        1,
                        position.getZ());
    }

    public static Matrix
        setToTranslate(final Matrix dest, final Vector position)
    {
        return set(dest,
                   1,
                   0,
                   0,
                   position.getX(),
                   0,
                   1,
                   0,
                   position.getY(),
                   0,
                   0,
                   1,
                   position.getZ());
    }

    /** creates a translation matrix
     * 
     * @param x
     *            the x coordinate to translate (0, 0, 0) to
     * @param y
     *            the y coordinate to translate (0, 0, 0) to
     * @param z
     *            the z coordinate to translate (0, 0, 0) to
     * @return the new translation matrix */
    @Deprecated
    public static Matrix translate(final float x, final float y, final float z)
    {
        return allocate(1, 0, 0, x, 0, 1, 0, y, 0, 0, 1, z);
    }

    public static Matrix setToTranslate(final Matrix dest,
                                        final float x,
                                        final float y,
                                        final float z)
    {
        return set(dest, 1, 0, 0, x, 0, 1, 0, y, 0, 0, 1, z);
    }

    /** creates a scaling matrix
     * 
     * @param x
     *            the amount to scale the x coordinate by
     * @param y
     *            the amount to scale the y coordinate by
     * @param z
     *            the amount to scale the z coordinate by
     * @return the new scaling matrix */
    @Deprecated
    public static Matrix scale(final float x, final float y, final float z)
    {
        return allocate(x, 0, 0, 0, 0, y, 0, 0, 0, 0, z, 0);
    }

    public static Matrix setToScale(final Matrix dest,
                                    final float x,
                                    final float y,
                                    final float z)
    {
        return set(dest, x, 0, 0, 0, 0, y, 0, 0, 0, 0, z, 0);
    }

    /** creates a scaling matrix
     * 
     * @param s
     *            <code>s.x</code> is the amount to scale the x coordinate by.<br/>
     *            <code>s.y</code> is the amount to scale the y coordinate by.<br/>
     *            <code>s.z</code> is the amount to scale the z coordinate by.
     * @return the new scaling matrix */
    @Deprecated
    public static Matrix scale(final Vector s)
    {
        return scale(s.getX(), s.getY(), s.getZ());
    }

    public static Matrix setToScale(final Matrix dest, final Vector s)
    {
        return setToScale(dest, s.getX(), s.getY(), s.getZ());
    }

    /** creates a scaling matrix
     * 
     * @param s
     *            the amount to scale by
     * @return the new scaling matrix */
    @Deprecated
    public static Matrix scale(final float s)
    {
        return scale(s, s, s);
    }

    public static Matrix setToScale(final Matrix dest, final float s)
    {
        return setToScale(dest, s, s, s);
    }

    /** @return the determinant of this matrix */
    public float determinant()
    {
        return this.elements[0 + 0 * 4]
                * (this.elements[1 + 1 * 4] * this.elements[2 + 2 * 4] - this.elements[1 + 2 * 4]
                        * this.elements[2 + 1 * 4])
                + this.elements[1 + 0 * 4]
                * (this.elements[0 + 2 * 4] * this.elements[2 + 1 * 4] - this.elements[0 + 1 * 4]
                        * this.elements[2 + 2 * 4])
                + this.elements[2 + 0 * 4]
                * (this.elements[0 + 1 * 4] * this.elements[1 + 2 * 4] - this.elements[0 + 2 * 4]
                        * this.elements[1 + 1 * 4]);
    }

    /** @return the inverse of this matrix or the identity matrix if this matrix
     *         is singular (has a determinant of 0). */
    @Deprecated
    public Matrix invert()
    {
        float det = determinant();
        if(det == 0.0f)
            return identity();
        float factor = 1.0f / det;
        return allocate((this.elements[1 + 1 * 4] * this.elements[2 + 2 * 4] - this.elements[1 + 2 * 4]
                                * this.elements[2 + 1 * 4])
                                * factor,
                        (this.elements[1 + 2 * 4] * this.elements[2 + 0 * 4] - this.elements[1 + 0 * 4]
                                * this.elements[2 + 2 * 4])
                                * factor,
                        (this.elements[1 + 0 * 4] * this.elements[2 + 1 * 4] - this.elements[1 + 1 * 4]
                                * this.elements[2 + 0 * 4])
                                * factor,
                        (-this.elements[1 + 0 * 4] * this.elements[2 + 1 * 4]
                                * this.elements[3 + 2 * 4]
                                + this.elements[1 + 1 * 4]
                                * this.elements[2 + 0 * 4]
                                * this.elements[3 + 2 * 4]
                                + this.elements[1 + 0 * 4]
                                * this.elements[2 + 2 * 4]
                                * this.elements[3 + 1 * 4]
                                - this.elements[1 + 2 * 4]
                                * this.elements[2 + 0 * 4]
                                * this.elements[3 + 1 * 4]
                                - this.elements[1 + 1 * 4]
                                * this.elements[2 + 2 * 4]
                                * this.elements[3 + 0 * 4] + this.elements[1 + 2 * 4]
                                * this.elements[2 + 1 * 4]
                                * this.elements[3 + 0 * 4])
                                * factor,
                        (this.elements[0 + 2 * 4] * this.elements[2 + 1 * 4] - this.elements[0 + 1 * 4]
                                * this.elements[2 + 2 * 4])
                                * factor,
                        (this.elements[0 + 0 * 4] * this.elements[2 + 2 * 4] - this.elements[0 + 2 * 4]
                                * this.elements[2 + 0 * 4])
                                * factor,
                        (this.elements[0 + 1 * 4] * this.elements[2 + 0 * 4] - this.elements[0 + 0 * 4]
                                * this.elements[2 + 1 * 4])
                                * factor,
                        (this.elements[0 + 0 * 4] * this.elements[2 + 1 * 4]
                                * this.elements[3 + 2 * 4]
                                - this.elements[0 + 1 * 4]
                                * this.elements[2 + 0 * 4]
                                * this.elements[3 + 2 * 4]
                                - this.elements[0 + 0 * 4]
                                * this.elements[2 + 2 * 4]
                                * this.elements[3 + 1 * 4]
                                + this.elements[0 + 2 * 4]
                                * this.elements[2 + 0 * 4]
                                * this.elements[3 + 1 * 4]
                                + this.elements[0 + 1 * 4]
                                * this.elements[2 + 2 * 4]
                                * this.elements[3 + 0 * 4] - this.elements[0 + 2 * 4]
                                * this.elements[2 + 1 * 4]
                                * this.elements[3 + 0 * 4])
                                * factor,
                        (this.elements[0 + 1 * 4] * this.elements[1 + 2 * 4] - this.elements[0 + 2 * 4]
                                * this.elements[1 + 1 * 4])
                                * factor,
                        (this.elements[0 + 2 * 4] * this.elements[1 + 0 * 4] - this.elements[0 + 0 * 4]
                                * this.elements[1 + 2 * 4])
                                * factor,
                        (this.elements[0 + 0 * 4] * this.elements[1 + 1 * 4] - this.elements[0 + 1 * 4]
                                * this.elements[1 + 0 * 4])
                                * factor,
                        (-this.elements[0 + 0 * 4] * this.elements[1 + 1 * 4]
                                * this.elements[3 + 2 * 4]
                                + this.elements[0 + 1 * 4]
                                * this.elements[1 + 0 * 4]
                                * this.elements[3 + 2 * 4]
                                + this.elements[0 + 0 * 4]
                                * this.elements[1 + 2 * 4]
                                * this.elements[3 + 1 * 4]
                                - this.elements[0 + 2 * 4]
                                * this.elements[1 + 0 * 4]
                                * this.elements[3 + 1 * 4]
                                - this.elements[0 + 1 * 4]
                                * this.elements[1 + 2 * 4]
                                * this.elements[3 + 0 * 4] + this.elements[0 + 2 * 4]
                                * this.elements[1 + 1 * 4]
                                * this.elements[3 + 0 * 4])
                                * factor);
    }

    public Matrix invertAndSet()
    {
        float det = determinant();
        if(det == 0.0f)
            return setToIdentity(this);
        float factor = 1.0f / det;
        return set(this,
                   (this.elements[1 + 1 * 4] * this.elements[2 + 2 * 4] - this.elements[1 + 2 * 4]
                           * this.elements[2 + 1 * 4])
                           * factor,
                   (this.elements[1 + 2 * 4] * this.elements[2 + 0 * 4] - this.elements[1 + 0 * 4]
                           * this.elements[2 + 2 * 4])
                           * factor,
                   (this.elements[1 + 0 * 4] * this.elements[2 + 1 * 4] - this.elements[1 + 1 * 4]
                           * this.elements[2 + 0 * 4])
                           * factor,
                   (-this.elements[1 + 0 * 4] * this.elements[2 + 1 * 4]
                           * this.elements[3 + 2 * 4]
                           + this.elements[1 + 1 * 4]
                           * this.elements[2 + 0 * 4]
                           * this.elements[3 + 2 * 4]
                           + this.elements[1 + 0 * 4]
                           * this.elements[2 + 2 * 4]
                           * this.elements[3 + 1 * 4]
                           - this.elements[1 + 2 * 4]
                           * this.elements[2 + 0 * 4]
                           * this.elements[3 + 1 * 4]
                           - this.elements[1 + 1 * 4]
                           * this.elements[2 + 2 * 4]
                           * this.elements[3 + 0 * 4] + this.elements[1 + 2 * 4]
                           * this.elements[2 + 1 * 4]
                           * this.elements[3 + 0 * 4])
                           * factor,
                   (this.elements[0 + 2 * 4] * this.elements[2 + 1 * 4] - this.elements[0 + 1 * 4]
                           * this.elements[2 + 2 * 4])
                           * factor,
                   (this.elements[0 + 0 * 4] * this.elements[2 + 2 * 4] - this.elements[0 + 2 * 4]
                           * this.elements[2 + 0 * 4])
                           * factor,
                   (this.elements[0 + 1 * 4] * this.elements[2 + 0 * 4] - this.elements[0 + 0 * 4]
                           * this.elements[2 + 1 * 4])
                           * factor,
                   (this.elements[0 + 0 * 4] * this.elements[2 + 1 * 4]
                           * this.elements[3 + 2 * 4]
                           - this.elements[0 + 1 * 4]
                           * this.elements[2 + 0 * 4]
                           * this.elements[3 + 2 * 4]
                           - this.elements[0 + 0 * 4]
                           * this.elements[2 + 2 * 4]
                           * this.elements[3 + 1 * 4]
                           + this.elements[0 + 2 * 4]
                           * this.elements[2 + 0 * 4]
                           * this.elements[3 + 1 * 4]
                           + this.elements[0 + 1 * 4]
                           * this.elements[2 + 2 * 4]
                           * this.elements[3 + 0 * 4] - this.elements[0 + 2 * 4]
                           * this.elements[2 + 1 * 4]
                           * this.elements[3 + 0 * 4])
                           * factor,
                   (this.elements[0 + 1 * 4] * this.elements[1 + 2 * 4] - this.elements[0 + 2 * 4]
                           * this.elements[1 + 1 * 4])
                           * factor,
                   (this.elements[0 + 2 * 4] * this.elements[1 + 0 * 4] - this.elements[0 + 0 * 4]
                           * this.elements[1 + 2 * 4])
                           * factor,
                   (this.elements[0 + 0 * 4] * this.elements[1 + 1 * 4] - this.elements[0 + 1 * 4]
                           * this.elements[1 + 0 * 4])
                           * factor,
                   (-this.elements[0 + 0 * 4] * this.elements[1 + 1 * 4]
                           * this.elements[3 + 2 * 4]
                           + this.elements[0 + 1 * 4]
                           * this.elements[1 + 0 * 4]
                           * this.elements[3 + 2 * 4]
                           + this.elements[0 + 0 * 4]
                           * this.elements[1 + 2 * 4]
                           * this.elements[3 + 1 * 4]
                           - this.elements[0 + 2 * 4]
                           * this.elements[1 + 0 * 4]
                           * this.elements[3 + 1 * 4]
                           - this.elements[0 + 1 * 4]
                           * this.elements[1 + 2 * 4]
                           * this.elements[3 + 0 * 4] + this.elements[0 + 2 * 4]
                           * this.elements[1 + 1 * 4]
                           * this.elements[3 + 0 * 4])
                           * factor);
    }

    public static Matrix setToInverse(final Matrix dest, final Matrix src)
    {
        float det = src.determinant();
        if(det == 0.0f)
            return setToIdentity(dest);
        float factor = 1.0f / det;
        return set(dest,
                   (src.elements[1 + 1 * 4] * src.elements[2 + 2 * 4] - src.elements[1 + 2 * 4]
                           * src.elements[2 + 1 * 4])
                           * factor,
                   (src.elements[1 + 2 * 4] * src.elements[2 + 0 * 4] - src.elements[1 + 0 * 4]
                           * src.elements[2 + 2 * 4])
                           * factor,
                   (src.elements[1 + 0 * 4] * src.elements[2 + 1 * 4] - src.elements[1 + 1 * 4]
                           * src.elements[2 + 0 * 4])
                           * factor,
                   (-src.elements[1 + 0 * 4] * src.elements[2 + 1 * 4]
                           * src.elements[3 + 2 * 4] + src.elements[1 + 1 * 4]
                           * src.elements[2 + 0 * 4] * src.elements[3 + 2 * 4]
                           + src.elements[1 + 0 * 4] * src.elements[2 + 2 * 4]
                           * src.elements[3 + 1 * 4] - src.elements[1 + 2 * 4]
                           * src.elements[2 + 0 * 4] * src.elements[3 + 1 * 4]
                           - src.elements[1 + 1 * 4] * src.elements[2 + 2 * 4]
                           * src.elements[3 + 0 * 4] + src.elements[1 + 2 * 4]
                           * src.elements[2 + 1 * 4] * src.elements[3 + 0 * 4])
                           * factor,
                   (src.elements[0 + 2 * 4] * src.elements[2 + 1 * 4] - src.elements[0 + 1 * 4]
                           * src.elements[2 + 2 * 4])
                           * factor,
                   (src.elements[0 + 0 * 4] * src.elements[2 + 2 * 4] - src.elements[0 + 2 * 4]
                           * src.elements[2 + 0 * 4])
                           * factor,
                   (src.elements[0 + 1 * 4] * src.elements[2 + 0 * 4] - src.elements[0 + 0 * 4]
                           * src.elements[2 + 1 * 4])
                           * factor,
                   (src.elements[0 + 0 * 4] * src.elements[2 + 1 * 4]
                           * src.elements[3 + 2 * 4] - src.elements[0 + 1 * 4]
                           * src.elements[2 + 0 * 4] * src.elements[3 + 2 * 4]
                           - src.elements[0 + 0 * 4] * src.elements[2 + 2 * 4]
                           * src.elements[3 + 1 * 4] + src.elements[0 + 2 * 4]
                           * src.elements[2 + 0 * 4] * src.elements[3 + 1 * 4]
                           + src.elements[0 + 1 * 4] * src.elements[2 + 2 * 4]
                           * src.elements[3 + 0 * 4] - src.elements[0 + 2 * 4]
                           * src.elements[2 + 1 * 4] * src.elements[3 + 0 * 4])
                           * factor,
                   (src.elements[0 + 1 * 4] * src.elements[1 + 2 * 4] - src.elements[0 + 2 * 4]
                           * src.elements[1 + 1 * 4])
                           * factor,
                   (src.elements[0 + 2 * 4] * src.elements[1 + 0 * 4] - src.elements[0 + 0 * 4]
                           * src.elements[1 + 2 * 4])
                           * factor,
                   (src.elements[0 + 0 * 4] * src.elements[1 + 1 * 4] - src.elements[0 + 1 * 4]
                           * src.elements[1 + 0 * 4])
                           * factor,
                   (-src.elements[0 + 0 * 4] * src.elements[1 + 1 * 4]
                           * src.elements[3 + 2 * 4] + src.elements[0 + 1 * 4]
                           * src.elements[1 + 0 * 4] * src.elements[3 + 2 * 4]
                           + src.elements[0 + 0 * 4] * src.elements[1 + 2 * 4]
                           * src.elements[3 + 1 * 4] - src.elements[0 + 2 * 4]
                           * src.elements[1 + 0 * 4] * src.elements[3 + 1 * 4]
                           - src.elements[0 + 1 * 4] * src.elements[1 + 2 * 4]
                           * src.elements[3 + 0 * 4] + src.elements[0 + 2 * 4]
                           * src.elements[1 + 1 * 4] * src.elements[3 + 0 * 4])
                           * factor);
    }

    /** apply this transformation to the point <code>v</code>
     * 
     * @param v
     *            the point to transform
     * @return the transformed point */
    @Deprecated
    public Vector apply(final Vector v)
    {
        return Vector.allocate(v.getX() * this.elements[0 + 0 * 4] + v.getY()
                * this.elements[1 + 0 * 4] + v.getZ()
                * this.elements[2 + 0 * 4] + this.elements[3 + 0 * 4], v.getX()
                * this.elements[0 + 1 * 4] + v.getY()
                * this.elements[1 + 1 * 4] + v.getZ()
                * this.elements[2 + 1 * 4] + this.elements[3 + 1 * 4], v.getX()
                * this.elements[0 + 2 * 4] + v.getY()
                * this.elements[1 + 2 * 4] + v.getZ()
                * this.elements[2 + 2 * 4] + this.elements[3 + 2 * 4]);
    }

    public Vector apply(final Vector dest, final Vector v)
    {
        return Vector.set(dest, v.getX() * this.elements[0 + 0 * 4] + v.getY()
                * this.elements[1 + 0 * 4] + v.getZ()
                * this.elements[2 + 0 * 4] + this.elements[3 + 0 * 4], v.getX()
                * this.elements[0 + 1 * 4] + v.getY()
                * this.elements[1 + 1 * 4] + v.getZ()
                * this.elements[2 + 1 * 4] + this.elements[3 + 1 * 4], v.getX()
                * this.elements[0 + 2 * 4] + v.getY()
                * this.elements[1 + 2 * 4] + v.getZ()
                * this.elements[2 + 2 * 4] + this.elements[3 + 2 * 4]);
    }

    public Vector apply(final Vector dest,
                        final float x,
                        final float y,
                        final float z)
    {
        return Vector.set(dest, x * this.elements[0 + 0 * 4] + y
                * this.elements[1 + 0 * 4] + z * this.elements[2 + 0 * 4]
                + this.elements[3 + 0 * 4], x * this.elements[0 + 1 * 4] + y
                * this.elements[1 + 1 * 4] + z * this.elements[2 + 1 * 4]
                + this.elements[3 + 1 * 4], x * this.elements[0 + 2 * 4] + y
                * this.elements[1 + 2 * 4] + z * this.elements[2 + 2 * 4]
                + this.elements[3 + 2 * 4]);
    }

    /** apply this transformation to the normal vector <code>v</code>
     * 
     * @param v
     *            the normal vector to transform
     * @return the transformed normal vector */
    @Deprecated
    public Vector applyToNormal(final Vector v)
    {
        return this.apply(v)
                   .subAndSet(this.elements[3 + 0 * 4],
                              this.elements[3 + 1 * 4],
                              this.elements[3 + 2 * 4])
                   .normalizeAndSet();
    }

    public Vector applyToNormal(final Vector dest, final Vector v)
    {
        return this.apply(dest, v)
                   .subAndSet(this.elements[3 + 0 * 4],
                              this.elements[3 + 1 * 4],
                              this.elements[3 + 2 * 4])
                   .normalizeAndSet();
    }

    /** combine this transformation with <code>rt</code>, with this
     * transformation first.<br/>
     * (premultiply <code>this</code> with <code>rt</code>)
     * 
     * @param rt
     *            the transformation to combine after this transformation
     * @return the resulting transformation */
    @Deprecated
    public Matrix concat(final Matrix rt)
    {
        return allocate(this.elements[0 + 0 * 4] * rt.elements[0 + 0 * 4]
                                + this.elements[0 + 1 * 4]
                                * rt.elements[1 + 0 * 4]
                                + this.elements[0 + 2 * 4]
                                * rt.elements[2 + 0 * 4],
                        this.elements[1 + 0 * 4] * rt.elements[0 + 0 * 4]
                                + this.elements[1 + 1 * 4]
                                * rt.elements[1 + 0 * 4]
                                + this.elements[1 + 2 * 4]
                                * rt.elements[2 + 0 * 4],
                        this.elements[2 + 0 * 4] * rt.elements[0 + 0 * 4]
                                + this.elements[2 + 1 * 4]
                                * rt.elements[1 + 0 * 4]
                                + this.elements[2 + 2 * 4]
                                * rt.elements[2 + 0 * 4],
                        this.elements[3 + 0 * 4] * rt.elements[0 + 0 * 4]
                                + this.elements[3 + 1 * 4]
                                * rt.elements[1 + 0 * 4]
                                + this.elements[3 + 2 * 4]
                                * rt.elements[2 + 0 * 4]
                                + rt.elements[3 + 0 * 4],
                        this.elements[0 + 0 * 4] * rt.elements[0 + 1 * 4]
                                + this.elements[0 + 1 * 4]
                                * rt.elements[1 + 1 * 4]
                                + this.elements[0 + 2 * 4]
                                * rt.elements[2 + 1 * 4],
                        this.elements[1 + 0 * 4] * rt.elements[0 + 1 * 4]
                                + this.elements[1 + 1 * 4]
                                * rt.elements[1 + 1 * 4]
                                + this.elements[1 + 2 * 4]
                                * rt.elements[2 + 1 * 4],
                        this.elements[2 + 0 * 4] * rt.elements[0 + 1 * 4]
                                + this.elements[2 + 1 * 4]
                                * rt.elements[1 + 1 * 4]
                                + this.elements[2 + 2 * 4]
                                * rt.elements[2 + 1 * 4],
                        this.elements[3 + 0 * 4] * rt.elements[0 + 1 * 4]
                                + this.elements[3 + 1 * 4]
                                * rt.elements[1 + 1 * 4]
                                + this.elements[3 + 2 * 4]
                                * rt.elements[2 + 1 * 4]
                                + rt.elements[3 + 1 * 4],
                        this.elements[0 + 0 * 4] * rt.elements[0 + 2 * 4]
                                + this.elements[0 + 1 * 4]
                                * rt.elements[1 + 2 * 4]
                                + this.elements[0 + 2 * 4]
                                * rt.elements[2 + 2 * 4],
                        this.elements[1 + 0 * 4] * rt.elements[0 + 2 * 4]
                                + this.elements[1 + 1 * 4]
                                * rt.elements[1 + 2 * 4]
                                + this.elements[1 + 2 * 4]
                                * rt.elements[2 + 2 * 4],
                        this.elements[2 + 0 * 4] * rt.elements[0 + 2 * 4]
                                + this.elements[2 + 1 * 4]
                                * rt.elements[1 + 2 * 4]
                                + this.elements[2 + 2 * 4]
                                * rt.elements[2 + 2 * 4],
                        this.elements[3 + 0 * 4] * rt.elements[0 + 2 * 4]
                                + this.elements[3 + 1 * 4]
                                * rt.elements[1 + 2 * 4]
                                + this.elements[3 + 2 * 4]
                                * rt.elements[2 + 2 * 4]
                                + rt.elements[3 + 2 * 4]);
    }

    public Matrix concat(final Matrix dest, final Matrix rt)
    {
        return set(dest,
                   this.elements[0 + 0 * 4] * rt.elements[0 + 0 * 4]
                           + this.elements[0 + 1 * 4] * rt.elements[1 + 0 * 4]
                           + this.elements[0 + 2 * 4] * rt.elements[2 + 0 * 4],
                   this.elements[1 + 0 * 4] * rt.elements[0 + 0 * 4]
                           + this.elements[1 + 1 * 4] * rt.elements[1 + 0 * 4]
                           + this.elements[1 + 2 * 4] * rt.elements[2 + 0 * 4],
                   this.elements[2 + 0 * 4] * rt.elements[0 + 0 * 4]
                           + this.elements[2 + 1 * 4] * rt.elements[1 + 0 * 4]
                           + this.elements[2 + 2 * 4] * rt.elements[2 + 0 * 4],
                   this.elements[3 + 0 * 4] * rt.elements[0 + 0 * 4]
                           + this.elements[3 + 1 * 4] * rt.elements[1 + 0 * 4]
                           + this.elements[3 + 2 * 4] * rt.elements[2 + 0 * 4]
                           + rt.elements[3 + 0 * 4],
                   this.elements[0 + 0 * 4] * rt.elements[0 + 1 * 4]
                           + this.elements[0 + 1 * 4] * rt.elements[1 + 1 * 4]
                           + this.elements[0 + 2 * 4] * rt.elements[2 + 1 * 4],
                   this.elements[1 + 0 * 4] * rt.elements[0 + 1 * 4]
                           + this.elements[1 + 1 * 4] * rt.elements[1 + 1 * 4]
                           + this.elements[1 + 2 * 4] * rt.elements[2 + 1 * 4],
                   this.elements[2 + 0 * 4] * rt.elements[0 + 1 * 4]
                           + this.elements[2 + 1 * 4] * rt.elements[1 + 1 * 4]
                           + this.elements[2 + 2 * 4] * rt.elements[2 + 1 * 4],
                   this.elements[3 + 0 * 4] * rt.elements[0 + 1 * 4]
                           + this.elements[3 + 1 * 4] * rt.elements[1 + 1 * 4]
                           + this.elements[3 + 2 * 4] * rt.elements[2 + 1 * 4]
                           + rt.elements[3 + 1 * 4],
                   this.elements[0 + 0 * 4] * rt.elements[0 + 2 * 4]
                           + this.elements[0 + 1 * 4] * rt.elements[1 + 2 * 4]
                           + this.elements[0 + 2 * 4] * rt.elements[2 + 2 * 4],
                   this.elements[1 + 0 * 4] * rt.elements[0 + 2 * 4]
                           + this.elements[1 + 1 * 4] * rt.elements[1 + 2 * 4]
                           + this.elements[1 + 2 * 4] * rt.elements[2 + 2 * 4],
                   this.elements[2 + 0 * 4] * rt.elements[0 + 2 * 4]
                           + this.elements[2 + 1 * 4] * rt.elements[1 + 2 * 4]
                           + this.elements[2 + 2 * 4] * rt.elements[2 + 2 * 4],
                   this.elements[3 + 0 * 4] * rt.elements[0 + 2 * 4]
                           + this.elements[3 + 1 * 4] * rt.elements[1 + 2 * 4]
                           + this.elements[3 + 2 * 4] * rt.elements[2 + 2 * 4]
                           + rt.elements[3 + 2 * 4]);
    }

    public Matrix concatAndSet(final Matrix rt)
    {
        return set(this,
                   this.elements[0 + 0 * 4] * rt.elements[0 + 0 * 4]
                           + this.elements[0 + 1 * 4] * rt.elements[1 + 0 * 4]
                           + this.elements[0 + 2 * 4] * rt.elements[2 + 0 * 4],
                   this.elements[1 + 0 * 4] * rt.elements[0 + 0 * 4]
                           + this.elements[1 + 1 * 4] * rt.elements[1 + 0 * 4]
                           + this.elements[1 + 2 * 4] * rt.elements[2 + 0 * 4],
                   this.elements[2 + 0 * 4] * rt.elements[0 + 0 * 4]
                           + this.elements[2 + 1 * 4] * rt.elements[1 + 0 * 4]
                           + this.elements[2 + 2 * 4] * rt.elements[2 + 0 * 4],
                   this.elements[3 + 0 * 4] * rt.elements[0 + 0 * 4]
                           + this.elements[3 + 1 * 4] * rt.elements[1 + 0 * 4]
                           + this.elements[3 + 2 * 4] * rt.elements[2 + 0 * 4]
                           + rt.elements[3 + 0 * 4],
                   this.elements[0 + 0 * 4] * rt.elements[0 + 1 * 4]
                           + this.elements[0 + 1 * 4] * rt.elements[1 + 1 * 4]
                           + this.elements[0 + 2 * 4] * rt.elements[2 + 1 * 4],
                   this.elements[1 + 0 * 4] * rt.elements[0 + 1 * 4]
                           + this.elements[1 + 1 * 4] * rt.elements[1 + 1 * 4]
                           + this.elements[1 + 2 * 4] * rt.elements[2 + 1 * 4],
                   this.elements[2 + 0 * 4] * rt.elements[0 + 1 * 4]
                           + this.elements[2 + 1 * 4] * rt.elements[1 + 1 * 4]
                           + this.elements[2 + 2 * 4] * rt.elements[2 + 1 * 4],
                   this.elements[3 + 0 * 4] * rt.elements[0 + 1 * 4]
                           + this.elements[3 + 1 * 4] * rt.elements[1 + 1 * 4]
                           + this.elements[3 + 2 * 4] * rt.elements[2 + 1 * 4]
                           + rt.elements[3 + 1 * 4],
                   this.elements[0 + 0 * 4] * rt.elements[0 + 2 * 4]
                           + this.elements[0 + 1 * 4] * rt.elements[1 + 2 * 4]
                           + this.elements[0 + 2 * 4] * rt.elements[2 + 2 * 4],
                   this.elements[1 + 0 * 4] * rt.elements[0 + 2 * 4]
                           + this.elements[1 + 1 * 4] * rt.elements[1 + 2 * 4]
                           + this.elements[1 + 2 * 4] * rt.elements[2 + 2 * 4],
                   this.elements[2 + 0 * 4] * rt.elements[0 + 2 * 4]
                           + this.elements[2 + 1 * 4] * rt.elements[1 + 2 * 4]
                           + this.elements[2 + 2 * 4] * rt.elements[2 + 2 * 4],
                   this.elements[3 + 0 * 4] * rt.elements[0 + 2 * 4]
                           + this.elements[3 + 1 * 4] * rt.elements[1 + 2 * 4]
                           + this.elements[3 + 2 * 4] * rt.elements[2 + 2 * 4]
                           + rt.elements[3 + 2 * 4]);
    }

    public Matrix concatAndSetAndFreeArg(final Matrix rt)
    {
        set(this,
            this.elements[0 + 0 * 4] * rt.elements[0 + 0 * 4]
                    + this.elements[0 + 1 * 4] * rt.elements[1 + 0 * 4]
                    + this.elements[0 + 2 * 4] * rt.elements[2 + 0 * 4],
            this.elements[1 + 0 * 4] * rt.elements[0 + 0 * 4]
                    + this.elements[1 + 1 * 4] * rt.elements[1 + 0 * 4]
                    + this.elements[1 + 2 * 4] * rt.elements[2 + 0 * 4],
            this.elements[2 + 0 * 4] * rt.elements[0 + 0 * 4]
                    + this.elements[2 + 1 * 4] * rt.elements[1 + 0 * 4]
                    + this.elements[2 + 2 * 4] * rt.elements[2 + 0 * 4],
            this.elements[3 + 0 * 4] * rt.elements[0 + 0 * 4]
                    + this.elements[3 + 1 * 4] * rt.elements[1 + 0 * 4]
                    + this.elements[3 + 2 * 4] * rt.elements[2 + 0 * 4]
                    + rt.elements[3 + 0 * 4],
            this.elements[0 + 0 * 4] * rt.elements[0 + 1 * 4]
                    + this.elements[0 + 1 * 4] * rt.elements[1 + 1 * 4]
                    + this.elements[0 + 2 * 4] * rt.elements[2 + 1 * 4],
            this.elements[1 + 0 * 4] * rt.elements[0 + 1 * 4]
                    + this.elements[1 + 1 * 4] * rt.elements[1 + 1 * 4]
                    + this.elements[1 + 2 * 4] * rt.elements[2 + 1 * 4],
            this.elements[2 + 0 * 4] * rt.elements[0 + 1 * 4]
                    + this.elements[2 + 1 * 4] * rt.elements[1 + 1 * 4]
                    + this.elements[2 + 2 * 4] * rt.elements[2 + 1 * 4],
            this.elements[3 + 0 * 4] * rt.elements[0 + 1 * 4]
                    + this.elements[3 + 1 * 4] * rt.elements[1 + 1 * 4]
                    + this.elements[3 + 2 * 4] * rt.elements[2 + 1 * 4]
                    + rt.elements[3 + 1 * 4],
            this.elements[0 + 0 * 4] * rt.elements[0 + 2 * 4]
                    + this.elements[0 + 1 * 4] * rt.elements[1 + 2 * 4]
                    + this.elements[0 + 2 * 4] * rt.elements[2 + 2 * 4],
            this.elements[1 + 0 * 4] * rt.elements[0 + 2 * 4]
                    + this.elements[1 + 1 * 4] * rt.elements[1 + 2 * 4]
                    + this.elements[1 + 2 * 4] * rt.elements[2 + 2 * 4],
            this.elements[2 + 0 * 4] * rt.elements[0 + 2 * 4]
                    + this.elements[2 + 1 * 4] * rt.elements[1 + 2 * 4]
                    + this.elements[2 + 2 * 4] * rt.elements[2 + 2 * 4],
            this.elements[3 + 0 * 4] * rt.elements[0 + 2 * 4]
                    + this.elements[3 + 1 * 4] * rt.elements[1 + 2 * 4]
                    + this.elements[3 + 2 * 4] * rt.elements[2 + 2 * 4]
                    + rt.elements[3 + 2 * 4]);
        rt.free();
        return this;
    }

    public static Matrix removeTranslate(final Matrix dest, final Matrix src)
    {
        return set(dest,
                   src.elements[0 + 0 * 4],
                   src.elements[1 + 0 * 4],
                   src.elements[2 + 0 * 4],
                   0,
                   src.elements[0 + 1 * 4],
                   src.elements[1 + 1 * 4],
                   src.elements[2 + 1 * 4],
                   0,
                   src.elements[0 + 2 * 4],
                   src.elements[1 + 2 * 4],
                   src.elements[2 + 2 * 4],
                   0);
    }

    public Matrix removeTranslateAndSet()
    {
        return removeTranslate(this, this);
    }

    @Override
    public String toString()
    {
        StringBuilder s = new StringBuilder("Matrix");
        for(int y = 0; y < 3; y++)
        {
            s.append("\n[ ");
            for(int x = 0; x < 4; x++)
            {
                s.append(this.elements[x + y * 4]);
                s.append(" ");
            }
            s.append("]");
        }
        s.append("\n[ 0 0 0 1 ]");
        return s.toString();
    }

    @SuppressWarnings("unused")
    private void dump()
    {
        System.out.println(toString());
    }

    private static float[] gl_Matrix_mat = new float[16];
    private static FloatBuffer gl_Matrix_buf = null;

    /** load <code>m</code> as the current OpenGL matrix
     * 
     * @param m
     *            the matrix to load */
    public static void glLoadMatrix(final Matrix m)
    {
        float[] mat = gl_Matrix_mat;
        mat[0] = m.elements[0 + 0 * 4];
        mat[1] = m.elements[0 + 1 * 4];
        mat[2] = m.elements[0 + 2 * 4];
        mat[3] = 0;
        mat[4] = m.elements[1 + 0 * 4];
        mat[5] = m.elements[1 + 1 * 4];
        mat[6] = m.elements[1 + 2 * 4];
        mat[7] = 0;
        mat[8] = m.elements[2 + 0 * 4];
        mat[9] = m.elements[2 + 1 * 4];
        mat[10] = m.elements[2 + 2 * 4];
        mat[11] = 0;
        mat[12] = m.elements[3 + 0 * 4];
        mat[13] = m.elements[3 + 1 * 4];
        mat[14] = m.elements[3 + 2 * 4];
        mat[15] = 1;
        if(gl_Matrix_buf == null)
            gl_Matrix_buf = Main.platform.createFloatBuffer(16);
        FloatBuffer buf = gl_Matrix_buf;
        buf.clear();
        buf.put(mat);
        buf.flip();
        Main.opengl.glLoadMatrix(buf);
    }

    /** combine <code>m</code> with the current OpenGL matrix
     * 
     * @param m
     *            the matrix to combine */
    public static void glMultMatrix(final Matrix m)
    {
        float[] mat = gl_Matrix_mat;
        mat[0] = m.elements[0 + 0 * 4];
        mat[1] = m.elements[0 + 1 * 4];
        mat[2] = m.elements[0 + 2 * 4];
        mat[3] = 0;
        mat[4] = m.elements[1 + 0 * 4];
        mat[5] = m.elements[1 + 1 * 4];
        mat[6] = m.elements[1 + 2 * 4];
        mat[7] = 0;
        mat[8] = m.elements[2 + 0 * 4];
        mat[9] = m.elements[2 + 1 * 4];
        mat[10] = m.elements[2 + 2 * 4];
        mat[11] = 0;
        mat[12] = m.elements[3 + 0 * 4];
        mat[13] = m.elements[3 + 1 * 4];
        mat[14] = m.elements[3 + 2 * 4];
        mat[15] = 1;
        if(gl_Matrix_buf == null)
            gl_Matrix_buf = Main.platform.createFloatBuffer(16);
        FloatBuffer buf = gl_Matrix_buf;
        buf.clear();
        buf.put(mat);
        buf.flip();
        Main.opengl.glMultMatrix(buf);
    }

    private static final class ImmutableMatrix extends Matrix
    {
        public ImmutableMatrix(final Matrix m)
        {
            for(int i = 0; i < m.elements.length; i++)
                this.elements[i] = m.elements[i];
        }

        public void unsupportedOp()
        {
            throw new UnsupportedOperationException("can't modify a immutable matrix");
        }

        @Override
        public void set(final int x, final int y, final float value)
        {
            unsupportedOp();
        }

        @Override
        public Matrix getImmutable()
        {
            return this;
        }
    }

    public Matrix getImmutable()
    {
        return new ImmutableMatrix(this);
    }

    public Matrix getImmutableAndFree()
    {
        Matrix retval = new ImmutableMatrix(this);
        free();
        return retval;
    }

    private static final Allocator<Matrix> allocator = new Allocator<Matrix>()
    {
        @Override
        protected Matrix allocateInternal()
        {
            return new Matrix();
        }
    };

    public static Matrix allocate(final Matrix rt)
    {
        return set(allocator.allocate(), rt);
    }

    public static Matrix allocate(final float x00,
                                  final float x10,
                                  final float x20,
                                  final float x30,
                                  final float x01,
                                  final float x11,
                                  final float x21,
                                  final float x31,
                                  final float x02,
                                  final float x12,
                                  final float x22,
                                  final float x32)
    {
        return set(allocator.allocate(),
                   x00,
                   x10,
                   x20,
                   x30,
                   x01,
                   x11,
                   x21,
                   x31,
                   x02,
                   x12,
                   x22,
                   x32);
    }

    public static Matrix allocate()
    {
        return allocate(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0);
    }

    @Override
    public void free()
    {
        allocator.free(this);
    }

    public static final Matrix IDENTITY = setToIdentity(Matrix.allocate()).getImmutableAndFree();
    public static final Matrix ROTATEX_PI_2 = setToRotateX(Matrix.allocate(),
                                                           Math.PI / 2).getImmutableAndFree();
    public static final Matrix ROTATEX_PI = setToRotateX(Matrix.allocate(),
                                                         Math.PI).getImmutableAndFree();
    public static final Matrix ROTATEX_3_PI_2 = setToRotateX(Matrix.allocate(),
                                                             3 * Math.PI / 2).getImmutableAndFree();
    public static final Matrix ROTATEY_PI_2 = setToRotateY(Matrix.allocate(),
                                                           Math.PI / 2).getImmutableAndFree();
    public static final Matrix ROTATEY_PI = setToRotateY(Matrix.allocate(),
                                                         Math.PI).getImmutableAndFree();
    public static final Matrix ROTATEY_3_PI_2 = setToRotateY(Matrix.allocate(),
                                                             3 * Math.PI / 2).getImmutableAndFree();
    public static final Matrix ROTATEZ_PI_2 = setToRotateZ(Matrix.allocate(),
                                                           Math.PI / 2).getImmutableAndFree();
    public static final Matrix ROTATEZ_PI = setToRotateZ(Matrix.allocate(),
                                                         Math.PI).getImmutableAndFree();
    public static final Matrix ROTATEZ_3_PI_2 = setToRotateZ(Matrix.allocate(),
                                                             3 * Math.PI / 2).getImmutableAndFree();

    @Override
    public Allocatable dup()
    {
        return allocate(this);
    }

    public static Matrix setToThetaPhi(final Matrix retval,
                                       final double theta,
                                       final double phi)
    {
        Matrix t = Matrix.setToRotateX(Matrix.allocate(), -phi);
        return setToRotateY(retval, theta).concatAndSetAndFreeArg(t);
    }
}
