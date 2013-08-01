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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/** @author jacob */
public class Vector {
	/** x coordinate */
	private float x;
	/** y coordinate */
	private float y;
	/** z coordinate */
	private float z;

	/** @return the x */
	public float getX() {
		return this.x;
	}

	/**
	 * @param x
	 *            the x to set
	 */
	public void setX(final float x) {
		this.x = x;
	}

	/** @return the y */
	public float getY() {
		return this.y;
	}

	/**
	 * @param y
	 *            the y to set
	 */
	public void setY(final float y) {
		this.y = y;
	}

	/** @return the z */
	public float getZ() {
		return this.z;
	}

	/**
	 * @param z
	 *            the z to set
	 */
	public void setZ(final float z) {
		this.z = z;
	}

	/** create a vector */
	@Deprecated
	public Vector() {
		this.x = 0;
		this.y = 0;
		this.z = 0;
	}

	/**
	 * create a vector &lt;<code>v</code>, <code>v</code>, <code>v</code>&gt;
	 * 
	 * @param v
	 *            the coordinate value
	 */
	@Deprecated
	public Vector(final float v) {
		this.x = v;
		this.y = v;
		this.z = v;
	}

	/**
	 * create a vector &lt;<code>x</code>, <code>y</code>, <code>z</code>&gt;
	 * 
	 * @param x
	 *            the x coordinate
	 * @param y
	 *            the y coordinate
	 * @param z
	 *            the z coordinate
	 */
	@Deprecated
	public Vector(final float x, final float y, final float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/**
	 * copy a vector
	 * 
	 * @param rt
	 *            the vector to copy
	 */
	@Deprecated
	public Vector(final Vector rt) {
		this(rt.x, rt.y, rt.z);
	}

	/**
	 * vector add
	 * 
	 * @param rt
	 *            the vector to add
	 * @return <code>this</code> + <code>rt</code>
	 */
	@Deprecated
	public Vector add(final Vector rt) {
		return new Vector(this.x + rt.x, this.y + rt.y, this.z + rt.z);
	}

	@Deprecated
	public Vector add(final float rx, final float ry, final float rz) {
		return new Vector(this.x + rx, this.y + ry, this.z + rz);
	}

	public static Vector add(final Vector dest, final Vector l, final Vector r) {
		dest.x = l.x + r.x;
		dest.y = l.y + r.y;
		dest.z = l.z + r.z;
		return dest;
	}

	public static Vector add(final Vector dest, final Vector l, final float rx,
			final float ry, final float rz) {
		if (dest instanceof ImmutableVector)
			((ImmutableVector) dest).unsupportedOp();
		dest.x = l.x + rx;
		dest.y = l.y + ry;
		dest.z = l.z + rz;
		return dest;
	}

	public Vector addAndSet(final Vector rt) {
		this.x += rt.x;
		this.y += rt.y;
		this.z += rt.z;
		return this;
	}

	public Vector addAndSet(final float rx, final float ry, final float rz) {
		this.x += rx;
		this.y += ry;
		this.z += rz;
		return this;
	}

	/**
	 * vector subtract
	 * 
	 * @param rt
	 *            the vector to add
	 * @return <code>this</code> - <code>rt</code>
	 */
	@Deprecated
	public Vector sub(final Vector rt) {
		return new Vector(this.x - rt.x, this.y - rt.y, this.z - rt.z);
	}

	@Deprecated
	public Vector sub(final float rx, final float ry, final float rz) {
		return new Vector(this.x - rx, this.y - ry, this.z - rz);
	}

	public static Vector sub(final Vector dest, final Vector l, final Vector r) {
		if (dest instanceof ImmutableVector)
			((ImmutableVector) dest).unsupportedOp();
		dest.x = l.x - r.x;
		dest.y = l.y - r.y;
		dest.z = l.z - r.z;
		return dest;
	}

	public static Vector sub(final Vector dest, final Vector l, final float rx,
			final float ry, final float rz) {
		if (dest instanceof ImmutableVector)
			((ImmutableVector) dest).unsupportedOp();
		dest.x = l.x - rx;
		dest.y = l.y - ry;
		dest.z = l.z - rz;
		return dest;
	}

	public static Vector sub(final Vector dest, final float lx, final float ly,
			final float lz, final Vector r) {
		if (dest instanceof ImmutableVector)
			((ImmutableVector) dest).unsupportedOp();
		dest.x = lx - r.x;
		dest.y = ly - r.y;
		dest.z = lz - r.z;
		return dest;
	}

	public Vector subAndSet(final Vector rt) {
		this.x -= rt.x;
		this.y -= rt.y;
		this.z -= rt.z;
		return this;
	}

	public Vector subAndSet(final float rx, final float ry, final float rz) {
		this.x -= rx;
		this.y -= ry;
		this.z -= rz;
		return this;
	}

	public Vector rsubAndSet(final Vector rt) {
		this.x = rt.x - this.x;
		this.y = rt.y - this.y;
		this.z = rt.z - this.z;
		return this;
	}

	public Vector rsubAndSet(final float rx, final float ry, final float rz) {
		this.x = rx - this.x;
		this.y = ry - this.y;
		this.z = rz - this.z;
		return this;
	}

	public Vector rdivAndSet(final Vector rt) {
		this.x = rt.x / this.x;
		this.y = rt.y / this.y;
		this.z = rt.z / this.z;
		return this;
	}

	public Vector rdivAndSet(final float rx, final float ry, final float rz) {
		this.x = rx / this.x;
		this.y = ry / this.y;
		this.z = rz / this.z;
		return this;
	}

	/**
	 * vector negate
	 * 
	 * @return -<code>this</code>
	 */
	@Deprecated
	public Vector neg() {
		return new Vector(-this.x, -this.y, -this.z);
	}

	public Vector negAndSet() {
		this.x = -this.x;
		this.y = -this.y;
		this.z = -this.z;
		return this;
	}

	public static Vector neg(final Vector dest, final Vector src) {
		if (dest instanceof ImmutableVector)
			((ImmutableVector) dest).unsupportedOp();
		dest.x = -src.x;
		dest.y = -src.y;
		dest.z = -src.z;
		return dest;
	}

	public static Vector set(final Vector dest, final float x, final float y,
			final float z) {
		if (dest instanceof ImmutableVector)
			((ImmutableVector) dest).unsupportedOp();
		dest.x = x;
		dest.y = y;
		dest.z = z;
		return dest;
	}

	public Vector set(final float x, final float y, final float z) {
		return set(this, x, y, z);
	}

	/**
	 * component-wise multiply
	 * 
	 * @param rt
	 *            the vector to multiply
	 * @return &lt;<code>this.x</code> &times; <code>rt.x</code>,
	 *         <code>this.y</code> &times; <code>rt.y</code>,
	 *         <code>this.z</code> &times; <code>rt.z</code>&gt;
	 */
	@Deprecated
	public Vector mul(final Vector rt) {
		return new Vector(this.x * rt.x, this.y * rt.y, this.z * rt.z);
	}

	@Deprecated
	public Vector mul(final float rx, final float ry, final float rz) {
		return new Vector(this.x * rx, this.y * ry, this.z * rz);
	}

	public Vector mulAndSet(final Vector rt) {
		this.x *= rt.x;
		this.y *= rt.y;
		this.z *= rt.z;
		return this;
	}

	public Vector mulAndSet(final float rx, final float ry, final float rz) {
		this.x *= rx;
		this.y *= ry;
		this.z *= rz;
		return this;
	}

	public Vector mulAndSet(final float rt) {
		this.x *= rt;
		this.y *= rt;
		this.z *= rt;
		return this;
	}

	public static Vector mul(final Vector dest, final Vector l, final Vector r) {
		if (dest instanceof ImmutableVector)
			((ImmutableVector) dest).unsupportedOp();
		dest.x = l.x * r.x;
		dest.y = l.y * r.y;
		dest.z = l.z * r.z;
		return dest;
	}

	public static Vector mul(final Vector dest, final Vector l, final float rx,
			final float ry, final float rz) {
		if (dest instanceof ImmutableVector)
			((ImmutableVector) dest).unsupportedOp();
		dest.x = l.x * rx;
		dest.y = l.y * ry;
		dest.z = l.z * rz;
		return dest;
	}

	public static Vector mul(final Vector dest, final Vector l, final float r) {
		if (dest instanceof ImmutableVector)
			((ImmutableVector) dest).unsupportedOp();
		dest.x = l.x * r;
		dest.y = l.y * r;
		dest.z = l.z * r;
		return dest;
	}

	public Vector divAndSet(final Vector rt) {
		this.x /= rt.x;
		this.y /= rt.y;
		this.z /= rt.z;
		return this;
	}

	public Vector divAndSet(final float rx, final float ry, final float rz) {
		this.x /= rx;
		this.y /= ry;
		this.z /= rz;
		return this;
	}

	public Vector divAndSet(final float rt) {
		this.x /= rt;
		this.y /= rt;
		this.z /= rt;
		return this;
	}

	public static Vector div(final Vector dest, final Vector l, final Vector r) {
		if (dest instanceof ImmutableVector)
			((ImmutableVector) dest).unsupportedOp();
		dest.x = l.x / r.x;
		dest.y = l.y / r.y;
		dest.z = l.z / r.z;
		return dest;
	}

	public static Vector div(final Vector dest, final float lx, final float ly,
			final float lz, final Vector r) {
		if (dest instanceof ImmutableVector)
			((ImmutableVector) dest).unsupportedOp();
		dest.x = lx / r.x;
		dest.y = ly / r.y;
		dest.z = lz / r.z;
		return dest;
	}

	public static Vector div(final Vector dest, final Vector l, final float rx,
			final float ry, final float rz) {
		if (dest instanceof ImmutableVector)
			((ImmutableVector) dest).unsupportedOp();
		dest.x = l.x / rx;
		dest.y = l.y / ry;
		dest.z = l.z / rz;
		return dest;
	}

	public static Vector div(final Vector dest, final Vector l, final float r) {
		if (dest instanceof ImmutableVector)
			((ImmutableVector) dest).unsupportedOp();
		dest.x = l.x / r;
		dest.y = l.y / r;
		dest.z = l.z / r;
		return dest;
	}

	/**
	 * component-wise divide
	 * 
	 * @param rt
	 *            the vector to multiply
	 * @return &lt;<code>this.x</code> &divide; <code>rt.x</code>,
	 *         <code>this.y</code> &divide; <code>rt.y</code>,
	 *         <code>this.z</code> &divide; <code>rt.z</code>&gt;
	 */
	@Deprecated
	public Vector div(final Vector rt) {
		return new Vector(this.x / rt.x, this.y / rt.y, this.z / rt.z);
	}

	@Deprecated
	public Vector div(final float rx, final float ry, final float rz) {
		return new Vector(this.x / rx, this.y / ry, this.z / rz);
	}

	/**
	 * multiply by scalar
	 * 
	 * @param rt
	 *            scalar to multiply by
	 * @return <code>this</code> &times; <code>rt</code>
	 */
	@Deprecated
	public Vector mul(final float rt) {
		return new Vector(this.x * rt, this.y * rt, this.z * rt);
	}

	/**
	 * divide by scalar
	 * 
	 * @param rt
	 *            scalar to divide by
	 * @return <code>this</code> &divide; <code>rt</code>
	 */
	@Deprecated
	public Vector div(final float rt) {
		return new Vector(this.x / rt, this.y / rt, this.z / rt);
	}

	/**
	 * dot product
	 * 
	 * @param rt
	 *            vector to dot product with
	 * @return <code>this</code> &middot; <code>rt</code>
	 * @see #cross(Vector rt)
	 */
	public float dot(final Vector rt) {
		return this.x * rt.x + this.y * rt.y + this.z * rt.z;
	}

	public float dot(float x, float y, float z) {
		return this.x * x + this.y * y + this.z * z;
	}

	/**
	 * absolute value squared
	 * 
	 * @return |<code>this</code>|&sup2;
	 * @see #abs()
	 */
	public float abs_squared() {
		return dot(this);
	}

	/**
	 * cross product
	 * 
	 * @param rt
	 *            vector to cross product with
	 * @return <code>this</code> &times; <code>rt</code>
	 * @see #dot(Vector rt)
	 */
	@Deprecated
	public Vector cross(final Vector rt) {
		return new Vector(this.y * rt.z - this.z * rt.y, this.z * rt.x - this.x
				* rt.z, this.x * rt.y - this.y * rt.x);
	}

	@Deprecated
	public Vector cross(final float rx, final float ry, final float rz) {
		return new Vector(this.y * rz - this.z * ry, this.z * rx - this.x * rz,
				this.x * ry - this.y * rx);
	}

	public Vector crossAndSet(final Vector rt) {
		float newX = this.y * rt.z - this.z * rt.y;
		float newY = this.z * rt.x - this.x * rt.z;
		float newZ = this.x * rt.y - this.y * rt.x;
		this.x = newX;
		this.y = newY;
		this.z = newZ;
		return this;
	}

	public Vector crossAndSet(final float rx, final float ry, final float rz) {
		float newX = this.y * rz - this.z * ry;
		float newY = this.z * rx - this.x * rz;
		float newZ = this.x * ry - this.y * rx;
		this.x = newX;
		this.y = newY;
		this.z = newZ;
		return this;
	}

	public Vector rcrossAndSet(final Vector rt) {
		float newX = rt.y * this.z - rt.z * this.y;
		float newY = rt.z * this.x - rt.x * this.z;
		float newZ = rt.x * this.y - rt.y * this.x;
		this.x = newX;
		this.y = newY;
		this.z = newZ;
		return this;
	}

	public Vector rcrossAndSet(final float rx, final float ry, final float rz) {
		float newX = ry * this.z - rz * this.y;
		float newY = rz * this.x - rx * this.z;
		float newZ = rx * this.y - ry * this.x;
		this.x = newX;
		this.y = newY;
		this.z = newZ;
		return this;
	}

	public Vector set(final Vector rt) {
		this.x = rt.x;
		this.y = rt.y;
		this.z = rt.z;
		return this;
	}

	public static Vector cross(final Vector dest, final Vector l, final Vector r) {
		if (dest instanceof ImmutableVector)
			((ImmutableVector) dest).unsupportedOp();
		float newX = l.y * r.z - l.z * r.y;
		float newY = l.z * r.x - l.x * r.z;
		float newZ = l.x * r.y - l.y * r.x;
		dest.x = newX;
		dest.y = newY;
		dest.z = newZ;
		return dest;
	}

	public static Vector cross(final Vector dest, final Vector l,
			final float rx, final float ry, final float rz) {
		if (dest instanceof ImmutableVector)
			((ImmutableVector) dest).unsupportedOp();
		float newX = l.y * rz - l.z * ry;
		float newY = l.z * rx - l.x * rz;
		float newZ = l.x * ry - l.y * rx;
		dest.x = newX;
		dest.y = newY;
		dest.z = newZ;
		return dest;
	}

	public static Vector cross(final Vector dest, final float lx,
			final float ly, final float lz, final Vector r) {
		if (dest instanceof ImmutableVector)
			((ImmutableVector) dest).unsupportedOp();
		float newX = ly * r.z - lz * r.y;
		float newY = lz * r.x - lx * r.z;
		float newZ = lx * r.y - ly * r.x;
		dest.x = newX;
		dest.y = newY;
		dest.z = newZ;
		return dest;
	}

	/**
	 * checks for equality
	 * 
	 * @param rt
	 *            vector to compare to
	 * @return true if <code>this</code> == <code>rt</code>
	 */
	public boolean equals(final Vector rt) {
		return this.x == rt.x && this.y == rt.y && this.z == rt.z;
	}

	public boolean equals(final float rx, final float ry, final float rz) {
		return this.x == rx && this.y == ry && this.z == rz;
	}

	/**
	 * absolute value
	 * 
	 * @return |<code>this</code>|
	 * @see #abs_squared()
	 */
	public float abs() {
		return (float) Math.sqrt(abs_squared());
	}

	/**
	 * normalize
	 * 
	 * @return this vector normalized or &lt;0, 0, 0&gt; if <code>this</code> ==
	 *         &lt;0, 0, 0&gt;
	 */
	@Deprecated
	public Vector normalize() {
		float a = abs();
		if (a == 0)
			a = 1;
		return this.div(a);
	}

	public static Vector normalize(final float x, final float y, final float z) {
		float a = (float) Math.sqrt(x * x + y * y + z * z);
		if (a == 0)
			a = 1;
		return new Vector(x / a, y / a, z / a);
	}

	public Vector normalizeAndSet() {
		float a = abs();
		if (a == 0)
			a = 1;
		return divAndSet(a);
	}

	public static Vector normalize(final Vector dest, final Vector src) {
		float a = src.abs();
		if (a == 0)
			a = 1;
		return div(dest, src, a);
	}

	public static Vector normalize(final Vector dest, final float x,
			final float y, final float z) {
		float a = (float) Math.sqrt(x * x + y * y + z * z);
		if (a == 0)
			a = 1;
		return set(dest, x / a, y / a, z / a);
	}

	/**
	 * calls glVertex3f with <code>v</code>
	 * 
	 * @param v
	 *            the vector to call glVertex3f with
	 */
	public static void glVertex(final Vector v) {
		Main.opengl.glVertex3f(v.x, v.y, v.z);
	}

	@Override
	public String toString() {
		return "<" + Float.toString(this.x) + ", " + Float.toString(this.y)
				+ ", " + Float.toString(this.z) + ">";
	}

	private static void validateFloat(final float f) throws IOException {
		if (Float.isInfinite(f) || Float.isNaN(f))
			throw new IOException("out of range");
	}

	/**
	 * read from a <code>DataInput</code>
	 * 
	 * @param i
	 *            <code>DataInput</code> to read from
	 * @return the read <code>Vector</code>
	 * @throws IOException
	 *             the exception thrown
	 */
	public static Vector read(final DataInput i) throws IOException {
		Vector retval = new Vector();
		retval.x = i.readFloat();
		validateFloat(retval.x);
		retval.y = i.readFloat();
		validateFloat(retval.y);
		retval.z = i.readFloat();
		validateFloat(retval.z);
		return retval;
	}

	/**
	 * write to a <code>DataOutput</code>
	 * 
	 * @param o
	 *            <code>OutputStream</code> to write to
	 * @throws IOException
	 *             the exception thrown
	 */
	public void write(final DataOutput o) throws IOException {
		o.writeFloat(this.x);
		o.writeFloat(this.y);
		o.writeFloat(this.z);
	}

	private static class ImmutableVector extends Vector {
		public ImmutableVector(final Vector v) {
			super(v);
		}

		Vector unsupportedOp() {
			throw new UnsupportedOperationException(
					"can't modify a immutable vector");
		}

		@Override
		public void setX(final float x) {
			unsupportedOp();
		}

		@Override
		public void setY(final float y) {
			unsupportedOp();
		}

		@Override
		public void setZ(final float z) {
			unsupportedOp();
		}

		@Override
		public Vector addAndSet(final Vector rt) {
			return unsupportedOp();
		}

		@Override
		public Vector addAndSet(final float rx, final float ry, final float rz) {
			return unsupportedOp();
		}

		@Override
		public Vector subAndSet(final Vector rt) {
			return unsupportedOp();
		}

		@Override
		public Vector subAndSet(final float rx, final float ry, final float rz) {
			return unsupportedOp();
		}

		@Override
		public Vector rsubAndSet(final Vector rt) {
			return unsupportedOp();
		}

		@Override
		public Vector rsubAndSet(final float rx, final float ry, final float rz) {
			return unsupportedOp();
		}

		@Override
		public Vector rdivAndSet(final Vector rt) {
			return unsupportedOp();
		}

		@Override
		public Vector rdivAndSet(final float rx, final float ry, final float rz) {
			return unsupportedOp();
		}

		@Override
		public Vector negAndSet() {
			return unsupportedOp();
		}

		@Override
		public Vector set(final float x, final float y, final float z) {
			return unsupportedOp();
		}

		@Override
		public Vector mulAndSet(final Vector rt) {
			return unsupportedOp();
		}

		@Override
		public Vector mulAndSet(final float rx, final float ry, final float rz) {
			return unsupportedOp();
		}

		@Override
		public Vector mulAndSet(final float rt) {
			return unsupportedOp();
		}

		@Override
		public Vector divAndSet(final Vector rt) {
			return unsupportedOp();
		}

		@Override
		public Vector divAndSet(final float rx, final float ry, final float rz) {
			return unsupportedOp();
		}

		@Override
		public Vector divAndSet(final float rt) {
			return unsupportedOp();
		}

		@Override
		public Vector crossAndSet(final Vector rt) {
			return unsupportedOp();
		}

		@Override
		public Vector crossAndSet(final float rx, final float ry, final float rz) {
			return unsupportedOp();
		}

		@Override
		public Vector rcrossAndSet(final Vector rt) {
			return unsupportedOp();
		}

		@Override
		public Vector rcrossAndSet(final float rx, final float ry,
				final float rz) {
			return unsupportedOp();
		}

		@Override
		public Vector set(final Vector rt) {
			return unsupportedOp();
		}

		@Override
		public Vector normalizeAndSet() {
			return unsupportedOp();
		}

		@Override
		public Vector getImmutable() {
			return this;
		}

		@Override
		public void free() {
		}
	}

	public Vector getImmutable() {
		return new ImmutableVector(this);
	}

	public static final Vector ZERO = new Vector(0, 0, 0).getImmutable();
	public static final Vector X = new Vector(1, 0, 0).getImmutable();
	public static final Vector Y = new Vector(0, 1, 0).getImmutable();
	public static final Vector Z = new Vector(0, 0, 1).getImmutable();
	public static final Vector XY = new Vector(1, 1, 0).getImmutable();
	public static final Vector YZ = new Vector(0, 1, 1).getImmutable();
	public static final Vector XZ = new Vector(1, 0, 1).getImmutable();
	public static final Vector XYZ = new Vector(1, 1, 1).getImmutable();
	public static final Vector NX = new Vector(-1, 0, 0).getImmutable();
	public static final Vector NY = new Vector(0, -1, 0).getImmutable();
	public static final Vector NZ = new Vector(0, 0, -1).getImmutable();
	public static final Vector NXY = new Vector(-1, 1, 0).getImmutable();
	public static final Vector NYZ = new Vector(0, -1, 1).getImmutable();
	public static final Vector NXZ = new Vector(-1, 0, 1).getImmutable();
	public static final Vector NXYZ = new Vector(-1, 1, 1).getImmutable();
	public static final Vector XNY = new Vector(1, -1, 0).getImmutable();
	public static final Vector YNZ = new Vector(0, 1, -1).getImmutable();
	public static final Vector XNZ = new Vector(1, 0, -1).getImmutable();
	public static final Vector NXNY = new Vector(-1, -1, 0).getImmutable();
	public static final Vector NYNZ = new Vector(0, -1, -1).getImmutable();
	public static final Vector NXNZ = new Vector(-1, 0, -1).getImmutable();
	public static final Vector XNYZ = new Vector(1, -1, 1).getImmutable();
	public static final Vector NXNYZ = new Vector(-1, -1, 1).getImmutable();
	public static final Vector XYNZ = new Vector(1, 1, -1).getImmutable();
	public static final Vector NXYNZ = new Vector(-1, 1, -1).getImmutable();
	public static final Vector XNYNZ = new Vector(1, -1, -1).getImmutable();
	public static final Vector NXNYNZ = new Vector(-1, -1, -1).getImmutable();

	public static Vector set(final Vector dest, final Vector src) {
		return set(dest, src.x, src.y, src.z);
	}

	private static Vector[] freeVectors = null;
	private static int freeVectorCount = 0;
	private static Object freeVectorPoolSyncObject = new Object();

	public static Vector allocate(float x, float y, float z) {
		synchronized (freeVectorPoolSyncObject) {
			if (freeVectorCount <= 0)
				return new Vector(x, y, z);
			Vector retval = freeVectors[--freeVectorCount];
			freeVectors[freeVectorCount] = null;
			return set(retval, x, y, z);
		}
	}

	public static Vector allocate(Vector rt) {
		return allocate(rt.x, rt.y, rt.z);
	}

	public static Vector allocate(float v) {
		return allocate(v, v, v);
	}

	public static Vector allocate() {
		return allocate(0, 0, 0);
	}

	private Vector[] expandArray(Vector[] array, int newSize) {
		Vector[] retval = new Vector[newSize];
		if (array != null)
			System.arraycopy(array, 0, retval, 0, array.length);
		return retval;
	}

	public void free() {
		synchronized (freeVectorPoolSyncObject) {
			if (freeVectors == null || freeVectorCount >= freeVectors.length) {
				freeVectors = expandArray(freeVectors, (freeVectors == null ? 0
						: freeVectors.length) + 1024);
			}
			freeVectors[freeVectorCount++] = this;
		}
	}
}
