package org.voxels;

import static org.lwjgl.opengl.GL11.glVertex3f;

import java.io.*;

/**
 * @author jacob
 * 
 */
public class Vector implements Serializable
{
	private static final long serialVersionUID = 6129521073951251607L;
	/**
	 * x coordinate
	 */
	public float x;
	/**
	 * y coordinate
	 */
	public float y;
	/**
	 * z coordinate
	 */
	public float z;

	/**
	 * create a vector
	 */
	public Vector()
	{
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
	public Vector(float v)
	{
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
	public Vector(float x, float y, float z)
	{
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
	public Vector(Vector rt)
	{
		this(rt.x, rt.y, rt.z);
	}

	/**
	 * vector add
	 * 
	 * @param rt
	 *            the vector to add
	 * @return <code>this</code> + <code>rt</code>
	 */
	public Vector add(Vector rt)
	{
		return new Vector(this.x + rt.x, this.y + rt.y, this.z + rt.z);
	}

	/**
	 * vector subtract
	 * 
	 * @param rt
	 *            the vector to add
	 * @return <code>this</code> - <code>rt</code>
	 */
	public Vector sub(Vector rt)
	{
		return new Vector(this.x - rt.x, this.y - rt.y, this.z - rt.z);
	}

	/**
	 * vector negate
	 * 
	 * @return -<code>this</code>
	 */
	public Vector neg()
	{
		return new Vector(-this.x, -this.y, -this.z);
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
	public Vector mul(Vector rt)
	{
		return new Vector(this.x * rt.x, this.y * rt.y, this.z * rt.z);
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
	public Vector div(Vector rt)
	{
		return new Vector(this.x / rt.x, this.y / rt.y, this.z / rt.z);
	}

	/**
	 * multiply by scalar
	 * 
	 * @param rt
	 *            scalar to multiply by
	 * @return <code>this</code> &times; <code>rt</code>
	 */
	public Vector mul(float rt)
	{
		return new Vector(this.x * rt, this.y * rt, this.z * rt);
	}

	/**
	 * divide by scalar
	 * 
	 * @param rt
	 *            scalar to divide by
	 * @return <code>this</code> &divide; <code>rt</code>
	 */
	public Vector div(float rt)
	{
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
	public float dot(Vector rt)
	{
		return this.x * rt.x + this.y * rt.y + this.z * rt.z;
	}

	/**
	 * absolute value squared
	 * 
	 * @return |<code>this</code>|&sup2;
	 * @see #abs()
	 */
	public float abs_squared()
	{
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
	public Vector cross(Vector rt)
	{
		return new Vector(this.y * rt.z - this.z * rt.y, this.z * rt.x - this.x
		        * rt.z, this.x * rt.y - this.y * rt.x);
	}

	/**
	 * checks for equality
	 * 
	 * @param rt
	 *            vector to compare to
	 * @return true if <code>this</code> == <code>rt</code>
	 */
	public boolean equals(Vector rt)
	{
		return this.x == rt.x && this.y == rt.y && this.z == rt.z;
	}

	/**
	 * absolute value
	 * 
	 * @return |<code>this</code>|
	 * @see #abs_squared()
	 */
	public float abs()
	{
		return (float)Math.sqrt(abs_squared());
	}

	/**
	 * normalize
	 * 
	 * @return this vector normalized or &lt;0, 0, 0&gt; if <code>this</code> ==
	 *         &lt;0, 0, 0&gt;
	 */
	public Vector normalize()
	{
		float a = this.abs();
		if(a == 0)
			a = 1;
		return this.div(a);
	}

	/**
	 * calls glVertex3f with <code>v</code>
	 * 
	 * @param v
	 *            the vector to call glVertex3f with
	 */
	public static void glVertex(Vector v)
	{
		glVertex3f(v.x, v.y, v.z);
	}

	@Override
	public String toString()
	{
		return "<" + Float.toString(this.x) + ", " + Float.toString(this.y)
		        + ", " + Float.toString(this.z) + ">";
	}

	private static void validateFloat(float f) throws IOException
	{
		if(Float.isInfinite(f) || Float.isNaN(f))
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
	public static Vector read(DataInput i) throws IOException
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

	/**
	 * write to a <code>DataOutput</code>
	 * 
	 * @param o
	 *            <code>OutputStream</code> to write to
	 * @throws IOException
	 *             the exception thrown
	 */
	public void write(DataOutput o) throws IOException
	{
		o.writeFloat(this.x);
		o.writeFloat(this.y);
		o.writeFloat(this.z);
	}
}
