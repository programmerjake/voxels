package org.voxels;

import static org.lwjgl.opengl.GL11.*;
import static org.voxels.Color.*;
import static org.voxels.Vector.glVertex;

/**
 * @author jacob
 * 
 */
public final class Text
{
	private static Image font = new Image("font.png");
	private static final int fontW = 8, fontH = 8;

	private static boolean getCharPixel(int x_in, int y_in, char ch)
	{
		int x = x_in, y = y_in;
		if(x < 0 || x >= fontW || y < 0 || y >= fontH)
		{
			int c = ch;
			c &= 0xFF;
			x += fontW * (c % 16);
			y += fontH * (c / 16);
			Color clr = font.getPixel(x, y);
			if(GetRValue(clr) >= 128 && GetAValue(clr) < 64)
				return true;
		}
		return false;
	}

	/**
	 * draw text
	 * 
	 * @param tform
	 *            the text to camera transformation
	 * @param clr
	 *            the text color
	 * @param str
	 *            the text to draw
	 */
	public static void draw(Matrix tform, Color clr, String str)
	{
		if(str.length() < 1)
			return;
		font.selectTexture();
		glColor(clr);
		glBegin(GL_QUADS);
		final float du = 1.0f / 16.0f, dv = 1.0f / 16.0f;
		float x = 0.0f, y = 0.0f;
		for(int i = 0; i < str.length(); i++)
		{
			if((str.charAt(i) & 0xFF) == '\n')
			{
				x = 0.0f;
				y -= 1.0f;
				continue;
			}
			int ch = str.charAt(i) & 0xFF;
			float u, v;
			u = ch % 16 / 16.0f;
			v = 1.0f - (ch / 16 + 1) / 16.0f;
			glTexCoord2f(u, v);
			glVertex(tform.apply(new Vector(0 + x, 0 + y, 0)));
			glTexCoord2f(u + du, v);
			glVertex(tform.apply(new Vector(1 + x, 0 + y, 0)));
			glTexCoord2f(u + du, v + dv);
			glVertex(tform.apply(new Vector(1 + x, 1 + y, 0)));
			glTexCoord2f(u, v + dv);
			glVertex(tform.apply(new Vector(0 + x, 1 + y, 0)));
			x += 1.0f;
		}
		glEnd();
	}

	/**
	 * draw text in white
	 * 
	 * @param tform
	 *            the text to camera transformation
	 * @param str
	 *            the text to draw
	 */
	public static void draw(Matrix tform, String str)
	{
		draw(tform, RGB(0xFF, 0xFF, 0xFF), str);
	}

	/**
	 * draw text to an image
	 * 
	 * @param img
	 *            image to draw to
	 * @param x
	 *            the x coordinate of the start of the text
	 * @param y
	 *            the y coordinate of the start of the text
	 * @param clr
	 *            the color to draw in
	 * @param str
	 *            the text to draw
	 */
	public static void draw(Image img, int x, int y, Color clr, String str)
	{
		int xi = x, yi = y;
		if(str.length() < 1)
			return;
		int startx = xi;
		for(int i = 0; i < str.length(); i++)
		{
			if((str.charAt(i) & 0xFF) == '\n')
			{
				xi = startx;
				yi += fontH;
				continue;
			}
			for(int cy = 0; cy < fontH; cy++)
				for(int cx = 0; cx < fontW; cx++)
					if(getCharPixel(cx, cy, str.charAt(i)))
						img.setPixel(cx + xi, cy + yi, clr);
			xi += fontW;
		}
		glEnd();
	}

	/**
	 * @param str
	 *            text to get the size of
	 * @return the width in pixels of the text
	 */
	public static int sizeW(String str)
	{
		int maxx = 0;
		int curx = 0;
		for(int i = 0; i < str.length(); i++)
		{
			if((str.charAt(i) & 0xFF) == '\n')
			{
				curx = 0;
				continue;
			}
			curx += fontW;
			if(curx > maxx)
				maxx = curx;
		}
		return maxx;
	}

	/**
	 * @param str
	 *            text to get the size of
	 * @return the height in pixels of the text
	 */
	public static int sizeH(String str)
	{
		int maxy = 0;
		int cury = 0;
		for(int i = 0; i < str.length(); i++)
		{
			if((str.charAt(i) & 0xFF) == '\n')
			{
				cury += fontH;
			}
			if(cury + fontH > maxy)
				maxy = cury + fontH;
		}
		return maxy;
	}

	private Text()
	{
	}
}
