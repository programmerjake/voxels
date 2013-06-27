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

/** @author jacob */
public interface GameObject
{
	/** draw this object
	 * 
	 * @param rs
	 *            the rendering stream to draw to
	 * @param worldToCamera
	 *            Matrix that transforms world coordinates to camera coordinates
	 * @return <code>rs</code> */
	public RenderingStream draw(RenderingStream rs, Matrix worldToCamera);

	/** move this object */
	public void move();
}
