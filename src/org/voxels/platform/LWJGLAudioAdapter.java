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

/** @author jacob */
public class LWJGLAudioAdapter implements Audio
{
    private final org.newdawn.slick.openal.Audio audio;

    @Override
    public void play(float volume, boolean loop)
    {
        this.audio.playAsSoundEffect(1.0f, volume, loop);
    }

    @Override
    public boolean isPlaying()
    {
        return this.audio.isPlaying();
    }

    @Override
    public void stop()
    {
        this.audio.stop();
    }

    LWJGLAudioAdapter(org.newdawn.slick.openal.Audio audio)
    {
        this.audio = audio;
    }
}
