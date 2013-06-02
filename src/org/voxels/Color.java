package org.voxels;

import org.lwjgl.opengl.GL11;

/**
 * @author jacob
 * 
 */
public final class Color
{
	/**
	 * red intensity
	 */
	public byte r;
	/**
	 * green intensity
	 */
	public byte g;
	/**
	 * blue intensity
	 */
	public byte b;
	/**
	 * opacity
	 */
	public byte a;

	/**
	 * @param nr
	 *            red intensity
	 * @param ng
	 *            green intensity
	 * @param nb
	 *            blue intensity
	 * @param na
	 *            opacity
	 */
	public Color(byte nr, byte ng, byte nb, byte na)
	{
		this.r = nr;
		this.g = ng;
		this.b = nb;
		this.a = na;
	}

	/**
	 * @param nr
	 *            red intensity
	 * @param ng
	 *            green intensity
	 * @param nb
	 *            blue intensity
	 */
	public Color(byte nr, byte ng, byte nb)
	{
		this(nr, ng, nb, (byte)0);
	}

	/**
	 * @param nr
	 *            red intensity
	 * @param ng
	 *            green intensity
	 * @param nb
	 *            blue intensity
	 * @param na
	 *            opacity
	 */
	public Color(int nr, int ng, int nb, int na)
	{
		this((byte)Math.max(Math.min(nr, 0xFF), 0),
		     (byte)Math.max(Math.min(ng, 0xFF), 0),
		     (byte)Math.max(Math.min(nb, 0xFF), 0),
		     (byte)Math.max(Math.min(na, 0xFF), 0));
	}

	/**
	 * @param nr
	 *            red intensity
	 * @param ng
	 *            green intensity
	 * @param nb
	 *            blue intensity
	 */
	public Color(int nr, int ng, int nb)
	{
		this((byte)Math.max(Math.min(nr, 0xFF), 0),
		     (byte)Math.max(Math.min(ng, 0xFF), 0),
		     (byte)Math.max(Math.min(nb, 0xFF), 0),
		     (byte)0);
	}

	/**
	 * @param r
	 *            red intensity
	 * @param g
	 *            green intensity
	 * @param b
	 *            blue intensity
	 * @return the created <code>Color</code>
	 */
	public static Color RGB(int r, int g, int b)
	{
		return new Color(r, g, b);
	}

	/**
	 * @param r
	 *            red intensity
	 * @param g
	 *            green intensity
	 * @param b
	 *            blue intensity
	 * @param a
	 *            opacity
	 * @return the created <code>Color</code>
	 */
	public static Color RGBA(int r, int g, int b, int a)
	{
		return new Color(r, g, b);
	}

	/**
	 * @param r
	 *            red intensity
	 * @param g
	 *            green intensity
	 * @param b
	 *            blue intensity
	 * @return the created <code>Color</code>
	 */
	public static Color RGB(float r, float g, float b)
	{
		return RGB((int)Math.floor(r * 256.0),
		           (int)Math.floor(g * 256.0),
		           (int)Math.floor(b * 256.0));
	}

	/**
	 * @param r
	 *            red intensity
	 * @param g
	 *            green intensity
	 * @param b
	 *            blue intensity
	 * @param a
	 *            opacity
	 * @return the created <code>Color</code>
	 */
	public static Color RGBA(float r, float g, float b, float a)
	{
		return RGBA((int)Math.floor(r * 256.0),
		            (int)Math.floor(g * 256.0),
		            (int)Math.floor(b * 256.0),
		            (int)Math.floor(a * 256.0));
	}

	/**
	 * @param v
	 *            intensity
	 * @return the created <code>Color</code>
	 */
	public static Color V(float v)
	{
		return RGB(v, v, v);
	}

	/**
	 * @param v
	 *            intensity
	 * @param a
	 *            opacity
	 * @return the created <code>Color</code>
	 */
	public static Color VA(float v, float a)
	{
		return RGBA(v, v, v, a);
	}

	/**
	 * @param c
	 *            color
	 * @return red intensity
	 */
	public static int GetRValue(Color c)
	{
		return c.r & 0xFF;
	}

	/**
	 * @param c
	 *            color
	 * @return green intensity
	 */
	public static int GetGValue(Color c)
	{
		return c.g & 0xFF;
	}

	/**
	 * @param c
	 *            color
	 * @return blue intensity
	 */
	public static int GetBValue(Color c)
	{
		return c.b & 0xFF;
	}

	/**
	 * @param c
	 *            color
	 * @return opacity
	 */
	public static int GetAValue(Color c)
	{
		return c.a & 0xFF;
	}

	/**
	 * call glColor3* <BR/>
	 * ignores <code>c</code>'s opacity
	 * 
	 * @param c
	 *            color
	 */
	public static void glColor(Color c)
	{
		GL11.glColor4f(GetRValue(c) / 255.0f,
		               GetGValue(c) / 255.0f,
		               GetBValue(c) / 255.0f,
		               1.0f);
	}

	/**
	 * call glClearColor <BR/>
	 * ignores <code>c</code>'s opacity
	 * 
	 * @param c
	 *            color
	 */
	public static void glClearColor(Color c)
	{
		GL11.glClearColor(GetRValue(c) / 255.0f,
		                  GetGValue(c) / 255.0f,
		                  GetBValue(c) / 255.0f,
		                  0.0f);
	}
}
