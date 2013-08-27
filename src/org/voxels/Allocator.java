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
package org.voxels;

/** @author jacob
 * @param <T>
 *            the type */
public abstract class Allocator<T>
{
    private Object[] pool = null;
    private int count = 0;
    private final int expansionAmount;

    private static final int arraySize(final Object[] array)
    {
        if(array == null)
            return 0;
        return array.length;
    }

    private final Object[] expandArray(final Object[] array)
    {
        int newSize = arraySize(array) + this.expansionAmount;
        Object[] retval = new Object[newSize];
        if(array != null)
            System.arraycopy(array, 0, retval, 0, array.length);
        return retval;
    }

    protected abstract T allocateInternal();

    @SuppressWarnings("unchecked")
    // because we can't create an array with new T[...]
    public synchronized final T
        allocate()
    {
        if(this.count <= 0)
            return allocateInternal();
        Object retval = this.pool[--this.count];
        this.pool[this.count] = null;
        if(retval == null)
            throw new NullPointerException();
        return (T)retval;
    }

    public synchronized final void free(final T o)
    {
        if(o == null)
            return;
        if(this.count >= arraySize(this.pool))
        {
            this.pool = expandArray(this.pool);
        }
        this.pool[this.count++] = o;
    }

    /** @param expansionAmount
     *            the amount to expand the free pool by */
    public Allocator(final int expansionAmount)
    {
        this.expansionAmount = expansionAmount;
    }

    public Allocator()
    {
        this(1024);
    }
}
