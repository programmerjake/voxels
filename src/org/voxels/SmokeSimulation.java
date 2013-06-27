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

import static org.voxels.Color.V;
import static org.voxels.Color.VA;

/** @author jacob */
public class SmokeSimulation extends FireSimulation
{
    /** create a new SmokeSimulation */
    public SmokeSimulation()
    {
        super(0.8f, 10);
    }

    @Override
    protected Color getValueToColor(float v)
    {
        if(v > fRand(0.1f, 1.1f))
            return V((1.0f - v) / 2.0f);
        return VA(0, 1.0f);
    }

    @Override
    protected float getShrinkTerm(int y)
    {
        return Math.max(0, Height - y)
                / fRand(Height * 4.0f * 35.0f / 32.0f,
                        Height * 4.0f * 45.0f / 32.0f);
    }
}
