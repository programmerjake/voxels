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

import static org.voxels.Color.RGB;
import static org.voxels.Color.VA;

/**
 * @author jacob
 * 
 */
public class RedstoneFireSimulation extends FireSimulation
{
	/**
	 * create a new RedstoneFireSimulation
	 */
	public RedstoneFireSimulation()
	{
		super();
	}

	@Override
	protected Color getValueToColor(float v)
	{
		if(v > 0.05f)
			return RGB(v, 0, 0);
		return VA(0.0f, 1.0f);
	}
}
