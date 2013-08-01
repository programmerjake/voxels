/**
 * this file is part of voxels
 * 
 * voxels is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * voxels is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with voxels.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.voxels.platform;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/** @author jacob */
public interface OpenGL {
	/** @return GL_TRIANGLES */
	public int GL_TRIANGLES();

	/** @return GL_DEPTH_BUFFER_BIT */
	public int GL_DEPTH_BUFFER_BIT();

	/** @return GL_DEPTH_TEST */
	public int GL_DEPTH_TEST();

	/**
	 * @param mask
	 *            mask
	 */
	public void glClear(int mask);

	/**
	 * @param cap
	 *            cap
	 */
	public void glEnable(int cap);

	/** @return GL_TEXTURE_2D */
	public int GL_TEXTURE_2D();

	/** @return GL_ALPHA_TEST */
	public int GL_ALPHA_TEST();

	/** @return GL_CULL_FACE */
	public int GL_CULL_FACE();

	/** @return GL_BLEND */
	public int GL_BLEND();

	/** @return GL_BACK */
	public int GL_BACK();

	/** @return GL_CCW */
	public int GL_CCW();

	/**
	 * @param mode
	 *            mode
	 */
	public void glCullFace(int mode);

	/**
	 * @param mode
	 *            mode
	 */
	public void glFrontFace(int mode);

	/** @return GL_LESS */
	public int GL_LESS();

	/** @return GL_ONE_MINUS_SRC_ALPHA */
	public int GL_ONE_MINUS_SRC_ALPHA();

	/** @return GL_SRC_ALPHA */
	public int GL_SRC_ALPHA();

	/** @return GL_TEXTURE_ENV */
	public int GL_TEXTURE_ENV();

	/** @return GL_TEXTURE_ENV_MODE */
	public int GL_TEXTURE_ENV_MODE();

	/** @return GL_MODULATE */
	public int GL_MODULATE();

	/**
	 * @param func
	 *            func
	 * @param ref
	 *            ref
	 */
	public void glAlphaFunc(int func, float ref);

	/**
	 * @param sfactor
	 *            sfactor
	 * @param dfactor
	 *            dfactor
	 */
	public void glBlendFunc(int sfactor, int dfactor);

	/**
	 * @param target
	 *            target
	 * @param pname
	 *            pname
	 * @param param
	 *            param
	 */
	public void glTexEnvi(int target, int pname, int param);

	/** @return GL_PERSPECTIVE_CORRECTION_HINT */
	public int GL_PERSPECTIVE_CORRECTION_HINT();

	/** @return GL_NICEST */
	public int GL_NICEST();

	/** @return GL_PROJECTION */
	public int GL_PROJECTION();

	/**
	 * @param target
	 *            target
	 * @param mode
	 *            mode
	 */
	public void glHint(int target, int mode);

	/**
	 * @param x
	 *            x
	 * @param y
	 *            y
	 * @param width
	 *            width
	 * @param height
	 *            height
	 */
	public void glViewport(int x, int y, int width, int height);

	/**
	 * @param mode
	 *            mode
	 */
	public void glMatrixMode(int mode);

	/**
     * 
     */
	public void glLoadIdentity();

	/**
	 * @param left
	 *            left
	 * @param right
	 *            right
	 * @param bottom
	 *            bottom
	 * @param top
	 *            top
	 * @param zNear
	 *            zNear
	 * @param zFar
	 *            zFar
	 */
	public void glFrustum(double left, double right, double bottom, double top,
			double zNear, double zFar);

	/** @return GL_COLOR_BUFFER_BIT */
	public int GL_COLOR_BUFFER_BIT();

	/** @return GL_MODELVIEW */
	public int GL_MODELVIEW();

	/**
     * 
     */
	public void glPushMatrix();

	/**
     * 
     */
	public void glPopMatrix();

	/**
     * 
     */
	public void glFinish();

	/**
	 * @param flag
	 *            flag
	 */
	public void glDepthMask(boolean flag);

	/**
	 * @param r
	 *            r
	 * @param g
	 *            g
	 * @param b
	 *            b
	 * @param a
	 *            a
	 */
	public void glColor4f(float r, float g, float b, float a);

	/**
	 * @param r
	 *            r
	 * @param g
	 *            g
	 * @param b
	 *            b
	 * @param a
	 *            a
	 */
	public void glClearColor(float r, float g, float b, float a);

	/**
	 * @param target
	 *            target
	 * @param texture
	 *            texture
	 */
	public void glBindTexture(int target, int texture);

	/**
	 * @param texture
	 *            texture
	 */
	public void glDeleteTextures(int texture);

	/** @return texture */
	public int glGenTextures();

	/** @return GL_TEXTURE_WRAP_S */
	public int GL_TEXTURE_WRAP_S();

	/** @return GL_TEXTURE_WRAP_T */
	public int GL_TEXTURE_WRAP_T();

	/** @return GL_REPEAT */
	public int GL_REPEAT();

	/**
	 * @param target
	 *            target
	 * @param pname
	 *            pname
	 * @param param
	 *            param
	 */
	public void glTexParameteri(int target, int pname, int param);

	/** @return GL_TEXTURE_MAG_FILTER */
	public int GL_TEXTURE_MAG_FILTER();

	/** @return GL_NEAREST */
	public int GL_NEAREST();

	/** @return GL_TEXTURE_MIN_FILTER */
	public int GL_TEXTURE_MIN_FILTER();

	/** @return GL_UNPACK_ALIGNMENT */
	public int GL_UNPACK_ALIGNMENT();

	/**
	 * @param pname
	 *            pname
	 * @param param
	 *            param
	 */
	public void glPixelStorei(int pname, int param);

	/** @return GL_RGBA */
	public int GL_RGBA();

	/** @return GL_UNSIGNED_BYTE */
	public int GL_UNSIGNED_BYTE();

	/**
	 * @param target
	 *            target
	 * @param level
	 *            level
	 * @param internalformat
	 *            internal format
	 * @param width
	 *            width
	 * @param height
	 *            height
	 * @param border
	 *            border
	 * @param format
	 *            format
	 * @param type
	 *            type
	 * @param pixels
	 *            pixels
	 */
	public void glTexImage2D(int target, int level, int internalformat,
			int width, int height, int border, int format, int type,
			ByteBuffer pixels);

	/**
	 * @param buf
	 *            buf
	 */
	public void glMultMatrix(FloatBuffer buf);

	/**
	 * @param buf
	 *            buf
	 */
	public void glLoadMatrix(FloatBuffer buf);

	/** @return GL_LINES */
	public int GL_LINES();

	/**
	 * @param mode
	 *            mode
	 */
	public void glBegin(int mode);

	/**
     * 
     */
	public void glEnd();

	/**
	 * @param x
	 *            x
	 * @param y
	 *            y
	 * @param z
	 *            z
	 */
	public void glVertex3f(float x, float y, float z);

	/**
	 * @param s
	 *            s
	 * @param t
	 *            t
	 */
	public void glTexCoord2f(float s, float t);

	/**
	 * @param r
	 *            r
	 * @param g
	 *            g
	 * @param b
	 *            b
	 */
	public void glColor3f(int r, int g, int b);

	/** @return GL_COLOR_ARRAY */
	public int GL_COLOR_ARRAY();

	/**
	 * @param cap
	 *            cap
	 */
	public void glEnableClientState(int cap);

	/** @return GL_TEXTURE_COORD_ARRAY */
	public int GL_TEXTURE_COORD_ARRAY();

	/** @return GL_VERTEX_ARRAY */
	public int GL_VERTEX_ARRAY();

	/**
	 * @param pointer
	 *            pointer
	 */
	public void glVertexPointer(java.nio.FloatBuffer pointer);

	/**
	 * @param pointer
	 *            pointer
	 */
	public void glTexCoordPointer(java.nio.FloatBuffer pointer);

	/**
	 * @param pointer
	 *            pointer
	 */
	public void glColorPointer(java.nio.FloatBuffer pointer);

	/**
	 * @param mode
	 *            mode
	 * @param first
	 *            first
	 * @param count
	 *            count
	 */
	public void glDrawArrays(int mode, int first, int count);

	/**
	 * @param cap
	 *            cap
	 */
	public void glDisableClientState(int cap);

	public void bindContext();

	public void releaseContext();
}
