package org.voxels;


/**
 * @author jacob
 * 
 */
public interface GameObject
{
	/**
	 * draw this object
	 * 
	 * @param worldToCamera
	 *            Matrix that transforms world coordinates to camera coordinates
	 */
	public void draw(Matrix worldToCamera);

	/**
	 * move this object
	 */
	public void move();
}
