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

import java.util.Random;

/** @author jacob */
public class FireSimulation {
	private Image image;
	private float[][][] values;
	protected static final int Width = 32, Height = 32;
	private int stepCount = 0;
	private final int seedCount;
	private Random rand;

	protected float fRand(float min, float max) {
		return this.rand.nextFloat() * (max - min) + min;
	}

	/** create new fire simulation */
	public FireSimulation() {
		this(0.1f, 10);
		for (int i = 0; i < 200; i++)
			step();
	}

	protected FireSimulation(float mixFactor, int seedCount) {
		this.mixFactor = mixFactor;
		this.seedCount = seedCount;
		this.image = new Image(Width, Height);
		this.values = new float[2][][];
		for (int i = 0; i < 2; i++) {
			this.values[i] = new float[Height][];
			for (int y = 0; y < Height; y++) {
				this.values[i][y] = new float[Width];
				for (int x = 0; x < Width; x++)
					this.values[i][y][x] = 0.0f;
			}
		}
		this.rand = new Random(0);
	}

	protected Color getValueToColor(float v) {
		float value = v * 3.0f;
		if (value <= 0.2f)
			return VA(0.0f, 1.0f);
		if (value < 1.0f)
			return RGB(value, 0.0f, 0.0f);
		if (value < 2.0f)
			return RGB(1.0f, value - 1.0f, 0.0f);
		return RGB(1.0f, 1.0f, value - 2.0f);
	}

	protected float getShrinkTerm(int y) {
		return Math.max(0, Height - y)
				/ fRand(Height * 4.0f * 15.0f / 32.0f,
						Height * 4.0f * 25.0f / 32.0f);
	}

	protected final float mixFactor;

	/**
	 * step the simulation
	 * 
	 * @return <code>this</code>
	 */
	public FireSimulation step() {
		final float invMixFactor = 1.0f - this.mixFactor;
		for (int x = 0; x < Width; x++) {
			for (int y = 0; y < Height; y++) {
				float newValue;
				if (y == Height - 1) {
					if (Math.abs(x - Width / 2) <= 1
							&& this.stepCount < this.seedCount)
						newValue = 1.0f;
					else
						newValue = 0.0f;
				} else {
					float oldValue = this.values[0][y][x];
					int dx = (int) Math.floor(fRand(-1.0f, 2.0f)); // integer
																	// from
																	// -1
																	// to 1
					if (fRand(0.0f, 1.0f) > 0.5f)
						dx = 0;
					int dy = (int) Math.floor(fRand(0.0f, 3.0f)); // integer
																	// from 0
																	// to
																	// 2
					float mixValue = 0.0f;
					if (x + dx >= 0 && x + dx < Width && y + dy >= 0
							&& y + dy < Height)
						mixValue = this.values[0][y + dy][x + dx];
					newValue = oldValue * invMixFactor + mixValue
							* this.mixFactor;
					newValue = (newValue * 3.0f + newValue * newValue) / 3.0f;
					if (newValue > 1.0f)
						newValue = 1.0f;
					newValue -= getShrinkTerm(y);
					if (newValue < 0.0f)
						newValue = 0.0f;
				}
				this.values[1][y][x] = newValue;
				this.image.setPixel(x, y, getValueToColor(newValue));
			}
		}
		float[][] temp = this.values[0];
		this.values[0] = this.values[1];
		this.values[1] = temp;
		this.stepCount++;
		return this;
	}

	/**
	 * get the current Image
	 * 
	 * @return the current Image
	 */
	public Image getImage() {
		return this.image;
	}
}
