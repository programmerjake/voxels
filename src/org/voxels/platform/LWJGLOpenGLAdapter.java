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

import org.lwjgl.opengl.GL11;

/** @author jacob */
public class LWJGLOpenGLAdapter implements OpenGL {
	/**
     * 
     */
	public LWJGLOpenGLAdapter() {
	}

	@Override
	public int GL_TRIANGLES() {
		return GL11.GL_TRIANGLES;
	}

	@Override
	public int GL_DEPTH_BUFFER_BIT() {
		return GL11.GL_DEPTH_BUFFER_BIT;
	}

	@Override
	public int GL_DEPTH_TEST() {
		return GL11.GL_DEPTH_TEST;
	}

	@Override
	public void glClear(int mask) {
		GL11.glClear(mask);
	}

	@Override
	public void glEnable(int cap) {
		GL11.glEnable(cap);
	}

	@Override
	public int GL_TEXTURE_2D() {
		return GL11.GL_TEXTURE_2D;
	}

	@Override
	public int GL_ALPHA_TEST() {
		return GL11.GL_ALPHA_TEST;
	}

	@Override
	public int GL_CULL_FACE() {
		return GL11.GL_CULL_FACE;
	}

	@Override
	public int GL_BLEND() {
		return GL11.GL_BLEND;
	}

	@Override
	public int GL_BACK() {
		return GL11.GL_BACK;
	}

	@Override
	public int GL_CCW() {
		return GL11.GL_CCW;
	}

	@Override
	public void glCullFace(int mode) {
		GL11.glCullFace(mode);
	}

	@Override
	public void glFrontFace(int mode) {
		GL11.glFrontFace(mode);
	}

	@Override
	public int GL_LESS() {
		return GL11.GL_LESS;
	}

	@Override
	public int GL_ONE_MINUS_SRC_ALPHA() {
		return GL11.GL_ONE_MINUS_SRC_ALPHA;
	}

	@Override
	public int GL_SRC_ALPHA() {
		return GL11.GL_SRC_ALPHA;
	}

	@Override
	public int GL_TEXTURE_ENV() {
		return GL11.GL_TEXTURE_ENV;
	}

	@Override
	public int GL_TEXTURE_ENV_MODE() {
		return GL11.GL_TEXTURE_ENV_MODE;
	}

	@Override
	public int GL_MODULATE() {
		return GL11.GL_MODULATE;
	}

	@Override
	public void glAlphaFunc(int func, float ref) {
		GL11.glAlphaFunc(func, ref);
	}

	@Override
	public void glBlendFunc(int sfactor, int dfactor) {
		GL11.glBlendFunc(sfactor, dfactor);
	}

	@Override
	public void glTexEnvi(int target, int pname, int param) {
		GL11.glTexEnvi(target, pname, param);
	}

	@Override
	public int GL_PERSPECTIVE_CORRECTION_HINT() {
		return GL11.GL_PERSPECTIVE_CORRECTION_HINT;
	}

	@Override
	public int GL_NICEST() {
		return GL11.GL_NICEST;
	}

	@Override
	public int GL_PROJECTION() {
		return GL11.GL_PROJECTION;
	}

	@Override
	public void glHint(int target, int mode) {
		GL11.glHint(target, mode);
	}

	@Override
	public void glViewport(int x, int y, int width, int height) {
		GL11.glViewport(x, y, width, height);
	}

	@Override
	public void glMatrixMode(int mode) {
		GL11.glMatrixMode(mode);
	}

	@Override
	public void glLoadIdentity() {
		GL11.glLoadIdentity();
	}

	@Override
	public void glFrustum(double left, double right, double bottom, double top,
			double zNear, double zFar) {
		GL11.glFrustum(left, right, bottom, top, zNear, zFar);
	}

	@Override
	public int GL_COLOR_BUFFER_BIT() {
		return GL11.GL_COLOR_BUFFER_BIT;
	}

	@Override
	public int GL_MODELVIEW() {
		return GL11.GL_MODELVIEW;
	}

	@Override
	public void glPushMatrix() {
		GL11.glPushMatrix();
	}

	@Override
	public void glPopMatrix() {
		GL11.glPopMatrix();
	}

	@Override
	public void glFinish() {
		GL11.glFinish();
	}

	@Override
	public void glDepthMask(boolean flag) {
		GL11.glDepthMask(flag);
	}

	@Override
	public void glColor4f(float r, float g, float b, float a) {
		GL11.glColor4f(r, g, b, a);
	}

	@Override
	public void glClearColor(float r, float g, float b, float a) {
		GL11.glClearColor(r, g, b, a);
	}

	@Override
	public void glBindTexture(int target, int texture) {
		GL11.glBindTexture(target, texture);
	}

	@Override
	public void glDeleteTextures(int texture) {
		GL11.glDeleteTextures(texture);
	}

	@Override
	public int glGenTextures() {
		return GL11.glGenTextures();
	}

	@Override
	public int GL_TEXTURE_WRAP_S() {
		return GL11.GL_TEXTURE_WRAP_S;
	}

	@Override
	public int GL_TEXTURE_WRAP_T() {
		return GL11.GL_TEXTURE_WRAP_T;
	}

	@Override
	public int GL_REPEAT() {
		return GL11.GL_REPEAT;
	}

	@Override
	public void glTexParameteri(int target, int pname, int param) {
		GL11.glTexParameteri(target, pname, param);
	}

	@Override
	public int GL_TEXTURE_MAG_FILTER() {
		return GL11.GL_TEXTURE_MAG_FILTER;
	}

	@Override
	public int GL_NEAREST() {
		return GL11.GL_NEAREST;
	}

	@Override
	public int GL_TEXTURE_MIN_FILTER() {
		return GL11.GL_TEXTURE_MIN_FILTER;
	}

	@Override
	public int GL_UNPACK_ALIGNMENT() {
		return GL11.GL_UNPACK_ALIGNMENT;
	}

	@Override
	public void glPixelStorei(int pname, int param) {
		GL11.glPixelStorei(pname, param);
	}

	@Override
	public int GL_RGBA() {
		return GL11.GL_RGBA;
	}

	@Override
	public int GL_UNSIGNED_BYTE() {
		return GL11.GL_UNSIGNED_BYTE;
	}

	@Override
	public void glTexImage2D(int target, int level, int internalformat,
			int width, int height, int border, int format, int type,
			ByteBuffer pixels) {
		GL11.glTexImage2D(target, level, internalformat, width, height, border,
				format, type, pixels);
	}

	@Override
	public void glMultMatrix(FloatBuffer buf) {
		GL11.glMultMatrix(buf);
	}

	@Override
	public void glLoadMatrix(FloatBuffer buf) {
		GL11.glLoadMatrix(buf);
	}

	@Override
	public int GL_LINES() {
		return GL11.GL_LINES;
	}

	@Override
	public void glBegin(int mode) {
		GL11.glBegin(mode);
	}

	@Override
	public void glEnd() {
		GL11.glEnd();
	}

	@Override
	public void glVertex3f(float x, float y, float z) {
		GL11.glVertex3f(x, y, z);
	}

	@Override
	public void glTexCoord2f(float s, float t) {
		GL11.glTexCoord2f(s, t);
	}

	@Override
	public void glColor3f(int r, int g, int b) {
		GL11.glColor3f(r, g, b);
	}

	@Override
	public int GL_COLOR_ARRAY() {
		return GL11.GL_COLOR_ARRAY;
	}

	@Override
	public void glEnableClientState(int cap) {
		GL11.glEnableClientState(cap);
	}

	@Override
	public int GL_TEXTURE_COORD_ARRAY() {
		return GL11.GL_TEXTURE_COORD_ARRAY;
	}

	@Override
	public int GL_VERTEX_ARRAY() {
		return GL11.GL_VERTEX_ARRAY;
	}

	@Override
	public void glVertexPointer(FloatBuffer pointer) {
		GL11.glVertexPointer(3, 0, pointer);
	}

	@Override
	public void glTexCoordPointer(FloatBuffer pointer) {
		GL11.glTexCoordPointer(2, 0, pointer);
	}

	@Override
	public void glColorPointer(FloatBuffer pointer) {
		GL11.glColorPointer(4, 0, pointer);
	}

	@Override
	public void glDrawArrays(int mode, int first, int count) {
		GL11.glDrawArrays(mode, first, count);
	}

	@Override
	public void glDisableClientState(int cap) {
		GL11.glDisableClientState(cap);
	}

	@Override
	public void bindContext() {
	}

	@Override
	public void releaseContext() {
	}
}
