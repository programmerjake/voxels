package org.voxels;

import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL11;

/**
 * 4x4 matrix for 3D transformation with last row always equal to [0 0 0 1]
 * 
 * @author jacob
 * 
 */
public class Matrix
{
	private float elements[] = new float[12];

	/**
	 * get the value at the position (<code>x</code>, <code>y</code>)
	 * 
	 * @param x
	 *            the column to get from (0 to 3)
	 * @param y
	 *            the row to get from (0 to 3)
	 * @return the value at the position (<code>x</code>, <code>y</code>)
	 * @throws ArrayIndexOutOfBoundsException
	 *             if <code>x</code> or <code>y</code> is out of range
	 */
	public float get(int x, int y) throws ArrayIndexOutOfBoundsException
	{
		if(x < 0 || x >= 4 || y < 0 || y >= 4)
			throw new ArrayIndexOutOfBoundsException();
		if(y >= 3)
		{
			if(x == y)
				return 1.0f;
			return 0.0f;
		}
		return this.elements[x + y * 4];
	}

	/**
	 * set the value at the position (<code>x</code>, <code>y</code>)
	 * 
	 * @param x
	 *            the column to set (0 to 3)
	 * @param y
	 *            the row to set (0 to 3)
	 * @param value
	 *            the new value for the position (<code>x</code>, <code>y</code>
	 *            )
	 * @throws ArrayIndexOutOfBoundsException
	 *             if <code>x</code> or <code>y</code> is out of range
	 */
	public void
	    set(int x, int y, float value) throws ArrayIndexOutOfBoundsException
	{
		if(x < 0 || x >= 4 || y < 0 || y >= 4)
			throw new ArrayIndexOutOfBoundsException();
		if(y < 3)
			this.elements[x + y * 4] = value;
	}

	/**
	 * creates the identity matrix<BR/>
	 * using <code>Matrix.identity()</code> is preferred
	 * 
	 * @see #identity()
	 */
	public Matrix()
	{
		this(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0);
	}

	/**
	 * creates the matrix<br/>
	 * <table>
	 * <tr>
	 * <td>/</td>
	 * <td></td>
	 * <td></td>
	 * <td></td>
	 * <td></td>
	 * <td>\</td>
	 * </tr>
	 * <tr>
	 * <td>|</td>
	 * <td><code>x00</code></td>
	 * <td><code>x10</code></td>
	 * <td><code>x20</code></td>
	 * <td><code>x30</code></td>
	 * <td>|</td>
	 * </tr>
	 * <tr>
	 * <td>|</td>
	 * <td><code>x01</code></td>
	 * <td><code>x11</code></td>
	 * <td><code>x21</code></td>
	 * <td><code>x31</code></td>
	 * <td>|</td>
	 * </tr>
	 * <tr>
	 * <td>|</td>
	 * <td><code>x02</code></td>
	 * <td><code>x12</code></td>
	 * <td><code>x22</code></td>
	 * <td><code>x32</code></td>
	 * <td>|</td>
	 * </tr>
	 * <tr>
	 * <td>|</td>
	 * <td><code>0.0</code></td>
	 * <td><code>0.0</code></td>
	 * <td><code>0.0</code></td>
	 * <td><code>1.0</code></td>
	 * <td>|</td>
	 * </tr>
	 * <tr>
	 * <td>\</td>
	 * <td></td>
	 * <td></td>
	 * <td></td>
	 * <td></td>
	 * <td>/</td>
	 * </tr>
	 * </table>
	 * 
	 * @param x00
	 *            value at (0, 0)
	 * @param x10
	 *            value at (1, 0)
	 * @param x20
	 *            value at (2, 0)
	 * @param x30
	 *            value at (3, 0)
	 * @param x01
	 *            value at (0, 1)
	 * @param x11
	 *            value at (1, 1)
	 * @param x21
	 *            value at (2, 1)
	 * @param x31
	 *            value at (3, 1)
	 * @param x02
	 *            value at (0, 2)
	 * @param x12
	 *            value at (1, 2)
	 * @param x22
	 *            value at (2, 2)
	 * @param x32
	 *            value at (3, 2)
	 */
	public Matrix(float x00,
	              float x10,
	              float x20,
	              float x30,
	              float x01,
	              float x11,
	              float x21,
	              float x31,
	              float x02,
	              float x12,
	              float x22,
	              float x32)
	{
		set(0, 0, x00);
		set(1, 0, x10);
		set(2, 0, x20);
		set(3, 0, x30);
		set(0, 1, x01);
		set(1, 1, x11);
		set(2, 1, x21);
		set(3, 1, x31);
		set(0, 2, x02);
		set(1, 2, x12);
		set(2, 2, x22);
		set(3, 2, x32);
	}

	/**
	 * creates the identity matrix
	 * 
	 * @return the new identity matrix
	 */
	public static Matrix identity()
	{
		return new Matrix(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0);
	}

	/**
	 * creates a rotation matrix
	 * 
	 * @param axis
	 *            axis to rotate around
	 * @param angle
	 *            angle to rotate in radians
	 * @return the new rotation matrix
	 * @see #rotatex(double angle)
	 * @see #rotatey(double angle)
	 * @see #rotatez(double angle)
	 */
	public static Matrix rotate(Vector axis, double angle)
	{
		float r = axis.abs();
		if(r == 0.0f)
			return identity();
		Vector axisv = axis.div(r);
		float c, s, v;
		c = (float)Math.cos(angle);
		s = (float)Math.sin(angle);
		v = 1.0f - c; // Versine
		float xx, xy, xz, yy, yz, zz;
		xx = axisv.x * axisv.x;
		xy = axisv.x * axisv.y;
		xz = axisv.x * axisv.z;
		yy = axisv.y * axisv.y;
		yz = axisv.y * axisv.z;
		zz = axisv.z * axisv.z;
		Matrix retval = new Matrix(xx + (1 - xx) * c,
		                           xy * v - axisv.z * s,
		                           xz * v + axisv.y * s,
		                           0,
		                           xy * v + axisv.z * s,
		                           yy + (1 - yy) * c,
		                           yz * v - axisv.x * s,
		                           0,
		                           xz * v - axisv.y * s,
		                           yz * v + axisv.x * s,
		                           zz + (1 - zz) * c,
		                           0);
		return retval;
	}

	/**
	 * creates a rotation matrix<br/>
	 * the same as <code>Matrix.rotate(new Vector(1, 0, 0), angle)</code>
	 * 
	 * @param angle
	 *            angle to rotate around the x axis in radians
	 * @return the new rotation matrix
	 * 
	 * @see #rotate(Vector axis, double angle)
	 * @see #rotatey(double angle)
	 * @see #rotatez(double angle)
	 */
	public static Matrix rotatex(double angle)
	{
		return rotate(new Vector(1, 0, 0), angle);
	}

	/**
	 * creates a rotation matrix<br/>
	 * the same as <code>Matrix.rotate(new Vector(0, 1, 0), angle)</code>
	 * 
	 * @param angle
	 *            angle to rotate around the y axis in radians
	 * @return the new rotation matrix
	 * 
	 * @see #rotate(Vector axis, double angle)
	 * @see #rotatex(double angle)
	 * @see #rotatez(double angle)
	 */
	public static Matrix rotatey(double angle)
	{
		return rotate(new Vector(0, 1, 0), angle);
	}

	/**
	 * creates a rotation matrix<br/>
	 * the same as <code>Matrix.rotate(new Vector(0, 0, 1), angle)</code>
	 * 
	 * @param angle
	 *            angle to rotate around the z axis in radians
	 * @return the new rotation matrix
	 * 
	 * @see #rotate(Vector axis, double angle)
	 * @see #rotatex(double angle)
	 * @see #rotatey(double angle)
	 */
	public static Matrix rotatez(double angle)
	{
		return rotate(new Vector(0, 0, 1), angle);
	}

	/**
	 * creates a translation matrix
	 * 
	 * @param position
	 *            the position to translate (0, 0, 0) to
	 * @return the new translation matrix
	 */
	public static Matrix translate(Vector position)
	{
		return new Matrix(1,
		                  0,
		                  0,
		                  position.x,
		                  0,
		                  1,
		                  0,
		                  position.y,
		                  0,
		                  0,
		                  1,
		                  position.z);
	}

	/**
	 * creates a translation matrix
	 * 
	 * @param x
	 *            the x coordinate to translate (0, 0, 0) to
	 * @param y
	 *            the y coordinate to translate (0, 0, 0) to
	 * @param z
	 *            the z coordinate to translate (0, 0, 0) to
	 * @return the new translation matrix
	 */
	public static Matrix translate(float x, float y, float z)
	{
		return translate(new Vector(x, y, z));
	}

	/**
	 * creates a scaling matrix
	 * 
	 * @param x
	 *            the amount to scale the x coordinate by
	 * @param y
	 *            the amount to scale the y coordinate by
	 * @param z
	 *            the amount to scale the z coordinate by
	 * @return the new scaling matrix
	 */
	public static Matrix scale(float x, float y, float z)
	{
		return new Matrix(x, 0, 0, 0, 0, y, 0, 0, 0, 0, z, 0);
	}

	/**
	 * creates a scaling matrix
	 * 
	 * @param s
	 *            <code>s.x</code> is the amount to scale the x coordinate by.<br/>
	 *            <code>s.y</code> is the amount to scale the y coordinate by.<br/>
	 *            <code>s.z</code> is the amount to scale the z coordinate by.
	 * @return the new scaling matrix
	 */
	public static Matrix scale(Vector s)
	{
		return scale(s.x, s.y, s.z);
	}

	/**
	 * creates a scaling matrix
	 * 
	 * @param s
	 *            the amount to scale by
	 * @return the new scaling matrix
	 */
	public static Matrix scale(float s)
	{
		return scale(s, s, s);
	}

	/**
	 * @return the determinant of this matrix
	 */
	public float determinant()
	{
		return get(0, 0) * (get(1, 1) * get(2, 2) - get(1, 2) * get(2, 1))
		        + get(1, 0) * (get(0, 2) * get(2, 1) - get(0, 1) * get(2, 2))
		        + get(2, 0) * (get(0, 1) * get(1, 2) - get(0, 2) * get(1, 1));
	}

	/**
	 * @return the inverse of this matrix or the identity matrix if this matrix
	 *         is singular (has a determinant of 0).
	 */
	public Matrix invert()
	{
		float det = this.determinant();
		if(det == 0.0f)
			return identity();
		float factor = 1.0f / det;
		return new Matrix((get(1, 1) * get(2, 2) - get(1, 2) * get(2, 1))
		        * factor, (get(1, 2) * get(2, 0) - get(1, 0) * get(2, 2))
		        * factor, (get(1, 0) * get(2, 1) - get(1, 1) * get(2, 0))
		        * factor, (-get(1, 0) * get(2, 1) * get(3, 2) + get(1, 1)
		        * get(2, 0) * get(3, 2) + get(1, 0) * get(2, 2) * get(3, 1)
		        - get(1, 2) * get(2, 0) * get(3, 1) - get(1, 1) * get(2, 2)
		        * get(3, 0) + get(1, 2) * get(2, 1) * get(3, 0))
		        * factor, (get(0, 2) * get(2, 1) - get(0, 1) * get(2, 2))
		        * factor, (get(0, 0) * get(2, 2) - get(0, 2) * get(2, 0))
		        * factor, (get(0, 1) * get(2, 0) - get(0, 0) * get(2, 1))
		        * factor, (get(0, 0) * get(2, 1) * get(3, 2) - get(0, 1)
		        * get(2, 0) * get(3, 2) - get(0, 0) * get(2, 2) * get(3, 1)
		        + get(0, 2) * get(2, 0) * get(3, 1) + get(0, 1) * get(2, 2)
		        * get(3, 0) - get(0, 2) * get(2, 1) * get(3, 0))
		        * factor, (get(0, 1) * get(1, 2) - get(0, 2) * get(1, 1))
		        * factor, (get(0, 2) * get(1, 0) - get(0, 0) * get(1, 2))
		        * factor, (get(0, 0) * get(1, 1) - get(0, 1) * get(1, 0))
		        * factor, (-get(0, 0) * get(1, 1) * get(3, 2) + get(0, 1)
		        * get(1, 0) * get(3, 2) + get(0, 0) * get(1, 2) * get(3, 1)
		        - get(0, 2) * get(1, 0) * get(3, 1) - get(0, 1) * get(1, 2)
		        * get(3, 0) + get(0, 2) * get(1, 1) * get(3, 0))
		        * factor);
	}

	/**
	 * apply this transformation to the point <code>v</code>
	 * 
	 * @param v
	 *            the point to transform
	 * @return the transformed point
	 */
	public Vector apply(Vector v)
	{
		return new Vector(v.x * get(0, 0) + v.y * get(1, 0) + v.z * get(2, 0)
		        + get(3, 0), v.x * get(0, 1) + v.y * get(1, 1) + v.z
		        * get(2, 1) + get(3, 1), v.x * get(0, 2) + v.y * get(1, 2)
		        + v.z * get(2, 2) + get(3, 2));
	}

	/**
	 * apply this transformation to the normal vector <code>v</code>
	 * 
	 * @param v
	 *            the normal vector to transform
	 * @return the transformed normal vector
	 */
	public Vector applyToNormal(Vector v)
	{
		Vector p1 = this.apply(new Vector(0));
		Vector p2 = this.apply(v);
		return p2.sub(p1).normalize();
	}

	/**
	 * combine this transformation with <code>rt</code>, with this
	 * transformation first.<br/>
	 * (premultiply <code>this</code> with <code>rt</code>)
	 * 
	 * @param rt
	 *            the transformation to combine after this transformation
	 * @return the resulting transformation
	 */
	public Matrix concat(Matrix rt)
	{
		// Matrix retval = new Matrix();
		// for(int i = 0; i < 4; i++)
		// {
		// for(int j = 0; j < 3; j++)
		// {
		// float sum = 0;
		// for(int k = 0; k < 4; k++)
		// sum += get(i, k) * rt.get(k, j);
		// retval.set(i, j, sum);
		// }
		// }
		// return retval;
		return new Matrix(get(0, 0) * rt.get(0, 0) + get(0, 1) * rt.get(1, 0)
		                          + get(0, 2) * rt.get(2, 0),
		                  get(1, 0) * rt.get(0, 0) + get(1, 1) * rt.get(1, 0)
		                          + get(1, 2) * rt.get(2, 0),
		                  get(2, 0) * rt.get(0, 0) + get(2, 1) * rt.get(1, 0)
		                          + get(2, 2) * rt.get(2, 0),
		                  get(3, 0) * rt.get(0, 0) + get(3, 1) * rt.get(1, 0)
		                          + get(3, 2) * rt.get(2, 0) + rt.get(3, 0),
		                  get(0, 0) * rt.get(0, 1) + get(0, 1) * rt.get(1, 1)
		                          + get(0, 2) * rt.get(2, 1),
		                  get(1, 0) * rt.get(0, 1) + get(1, 1) * rt.get(1, 1)
		                          + get(1, 2) * rt.get(2, 1),
		                  get(2, 0) * rt.get(0, 1) + get(2, 1) * rt.get(1, 1)
		                          + get(2, 2) * rt.get(2, 1),
		                  get(3, 0) * rt.get(0, 1) + get(3, 1) * rt.get(1, 1)
		                          + get(3, 2) * rt.get(2, 1) + rt.get(3, 1),
		                  get(0, 0) * rt.get(0, 2) + get(0, 1) * rt.get(1, 2)
		                          + get(0, 2) * rt.get(2, 2),
		                  get(1, 0) * rt.get(0, 2) + get(1, 1) * rt.get(1, 2)
		                          + get(1, 2) * rt.get(2, 2),
		                  get(2, 0) * rt.get(0, 2) + get(2, 1) * rt.get(1, 2)
		                          + get(2, 2) * rt.get(2, 2),
		                  get(3, 0) * rt.get(0, 2) + get(3, 1) * rt.get(1, 2)
		                          + get(3, 2) * rt.get(2, 2) + rt.get(3, 2));
	}

	@Override
	public String toString()
	{
		StringBuilder s = new StringBuilder("Matrix");
		for(int y = 0; y < 4; y++)
		{
			s.append("\n[ ");
			for(int x = 0; x < 4; x++)
			{
				s.append(get(x, y));
				s.append(" ");
			}
			s.append("]");
		}
		return s.toString();
	}

	@SuppressWarnings("unused")
	private void dump()
	{
		System.out.println(toString());
	}

	/**
	 * load <code>m</code> as the current OpenGL matrix
	 * 
	 * @param m
	 *            the matrix to load
	 */
	public static void glLoadMatrix(Matrix m)
	{
		float[] mat = new float[]
		{
		    m.get(0, 0),
		    m.get(0, 1),
		    m.get(0, 2),
		    m.get(0, 3),
		    m.get(1, 0),
		    m.get(1, 1),
		    m.get(1, 2),
		    m.get(1, 3),
		    m.get(2, 0),
		    m.get(2, 1),
		    m.get(2, 2),
		    m.get(2, 3),
		    m.get(3, 0),
		    m.get(3, 1),
		    m.get(3, 2),
		    m.get(3, 3)
		};
		FloatBuffer buf = org.lwjgl.BufferUtils.createFloatBuffer(16);
		buf.put(mat);
		buf.flip();
		GL11.glLoadMatrix(buf);
	}

	/**
	 * combine <code>m</code> with the current OpenGL matrix
	 * 
	 * @param m
	 *            the matrix to combine
	 */
	public static void glMultMatrix(Matrix m)
	{
		float[] mat = new float[]
		{
		    m.get(0, 0),
		    m.get(0, 1),
		    m.get(0, 2),
		    m.get(0, 3),
		    m.get(1, 0),
		    m.get(1, 1),
		    m.get(1, 2),
		    m.get(1, 3),
		    m.get(2, 0),
		    m.get(2, 1),
		    m.get(2, 2),
		    m.get(2, 3),
		    m.get(3, 0),
		    m.get(3, 1),
		    m.get(3, 2),
		    m.get(3, 3)
		};
		FloatBuffer buf = org.lwjgl.BufferUtils.createFloatBuffer(32);
		buf.put(mat);
		buf.flip();
		GL11.glMultMatrix(buf);
	}
}
