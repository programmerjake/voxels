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

public final class AllocatorHashMap<K extends Allocatable, V extends Allocatable> implements
    Allocatable
{
    private static final Allocator<AllocatorHashMap<Allocatable, Allocatable>> allocator = new Allocator<AllocatorHashMap<Allocatable, Allocatable>>()
    {
        @SuppressWarnings(
        {
            "rawtypes", "unchecked"
        })
        // because we can't use new AllocatorHashMap<K, V>() or new
        // AllocatorHashMap<?, ?>()
        @Override
        protected AllocatorHashMap<Allocatable, Allocatable>
            allocateInternal()
        {
            return new AllocatorHashMap();
        }
    };
    private static final int hashPrime = 8191;

    private static final class Node
    {
        public Node next;
        public Allocatable key;
        public Allocatable value;

        public Node()
        {
        }
    }

    private static final Allocator<Node> nodeAllocator = new Allocator<AllocatorHashMap.Node>()
    {
        @Override
        protected Node allocateInternal()
        {
            return new Node();
        }
    };

    private static boolean compareKeys(final Allocatable key1,
                                       final Allocatable key2)
    {
        if(key1 == null)
            return key2 == null;
        return key1.equals(key2);
    }

    private static int hashKey(final Allocatable key)
    {
        if(key == null)
            return 0;
        int retval = key.hashCode() % hashPrime;
        if(retval < 0)
            return retval + hashPrime;
        return retval;
    }

    private final Node[] table = new Node[hashPrime];

    /** the constructor shouldn't be called directly; use allocate() instead */
    AllocatorHashMap()
    {
    }

    public static AllocatorHashMap<? extends Allocatable, ? extends Allocatable>
        allocate()
    {
        return allocator.allocate();
    }

    public static AllocatorHashMap<? extends Allocatable, ? extends Allocatable>
        allocate(final AllocatorHashMap<? extends Allocatable, ? extends Allocatable> rt)
    {
        AllocatorHashMap<Allocatable, Allocatable> retval = allocator.allocate();
        Iterator<? extends Allocatable, ? extends Allocatable> i = rt.iterator();
        for(; !i.isEnd(); i.next())
        {
            retval.put(i.getKey(), i.getValue());
        }
        i.free();
        return retval;
    }

    private int entryCount = 0;

    public final void clear()
    {
        for(int i = 0; i < hashPrime; i++)
        {
            Node node = this.table[i];
            this.table[i] = null;
            while(node != null)
            {
                Node freeMe = node;
                node = node.next;
                if(freeMe.key != null)
                    freeMe.key.free();
                freeMe.key = null;
                freeMe.next = null;
                if(freeMe.value != null)
                    freeMe.value.free();
                freeMe.value = null;
                nodeAllocator.free(freeMe);
            }
        }
        this.entryCount = 0;
    }

    public final boolean containsKey(final Allocatable key)
    {
        int hash = hashKey(key);
        Node node = this.table[hash], parent = null;
        while(node != null)
        {
            if(compareKeys(key, node.key))
            {
                if(parent != null)
                {
                    parent.next = node.next;
                    node.next = this.table[hash];
                    this.table[hash] = node;
                }
                return true;
            }
            parent = node;
            node = node.next;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public final V get(final Allocatable key)
    {
        int hash = hashKey(key);
        Node node = this.table[hash], parent = null;
        while(node != null)
        {
            if(compareKeys(key, node.key))
            {
                if(parent != null)
                {
                    parent.next = node.next;
                    node.next = this.table[hash];
                    this.table[hash] = node;
                }
                return (V)node.value;
            }
            parent = node;
            node = node.next;
        }
        return null;
    }

    public final boolean isEmpty()
    {
        return size() == 0;
    }

    public final void put(final K key, final V value)
    {
        int hash = hashKey(key);
        Node node = this.table[hash], parent = null;
        while(node != null)
        {
            if(compareKeys(key, node.key))
            {
                if(parent != null)
                {
                    parent.next = node.next;
                    node.next = this.table[hash];
                    this.table[hash] = node;
                }
                if(node.value != null)
                    node.value.free();
                node.value = value;
                if(value != null)
                    node.value = value.dup();
                return;
            }
            parent = node;
            node = node.next;
        }
        node = nodeAllocator.allocate();
        node.key = key;
        if(key != null)
            node.key = key.dup();
        node.next = this.table[hash];
        this.table[hash] = node;
        node.value = value;
        if(value != null)
            node.value = value.dup();
        this.entryCount++;
    }

    public final void remove(final Allocatable key)
    {
        int hash = hashKey(key);
        Node node = this.table[hash], parent = null;
        while(node != null)
        {
            if(compareKeys(key, node.key))
            {
                if(parent != null)
                    parent.next = node.next;
                else
                    this.table[hash] = node.next;
                if(node.key != null)
                    node.key.free();
                if(node.value != null)
                    node.value.free();
                node.key = null;
                node.value = null;
                node.next = null;
                nodeAllocator.free(node);
                this.entryCount--;
                return;
            }
            parent = node;
            node = node.next;
        }
    }

    public final int size()
    {
        return this.entryCount;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final void free()
    {
        clear();
        allocator.free((AllocatorHashMap<Allocatable, Allocatable>)this);
    }

    @Override
    public final Allocatable dup()
    {
        return allocate(this);
    }

    public static abstract class Iterator<K, V>
    {
        public abstract boolean hasNext();

        public abstract boolean isEnd();

        public abstract boolean next();

        public abstract K getKey();

        public abstract V getValue();

        public abstract void free();
    }

    private static final class IteratorImp<K, V> extends Iterator<K, V>
    {
        int index;
        Node node;
        @SuppressWarnings("rawtypes")
        AllocatorHashMap allocatorHashMap = null;

        public IteratorImp()
        {
        }

        private static final Allocator<IteratorImp<?, ?>> allocator = new Allocator<AllocatorHashMap.IteratorImp<?, ?>>()
        {
            @SuppressWarnings("rawtypes")
            @Override
            protected IteratorImp<?, ?> allocateInternal()
            {
                return new IteratorImp();
            }
        };

        @SuppressWarnings("synthetic-access")
        private IteratorImp<K, V>
            init(final AllocatorHashMap<?, ?> allocatorHashMap)
        {
            this.allocatorHashMap = allocatorHashMap;
            if(allocatorHashMap == null)
            {
                this.index = 0;
                this.node = null;
                return this;
            }
            this.index = 0;
            this.node = allocatorHashMap.table[0];
            if(this.node == null)
                advance(true);
            return this;
        }

        @SuppressWarnings("synthetic-access")
        private boolean advance(final boolean setToNext)
        {
            if(this.allocatorHashMap == null)
                return false;
            Node node = this.node;
            int index = this.index;
            if(node != null)
                node = node.next;
            while(node == null)
            {
                if(index >= hashPrime - 1)
                {
                    if(setToNext)
                    {
                        this.index = 0;
                        this.node = null;
                        this.allocatorHashMap = null;
                    }
                    return false;
                }
                index++;
                node = this.allocatorHashMap.table[index];
            }
            if(setToNext)
            {
                this.node = node;
                this.index = index;
            }
            return true;
        }

        @Override
        public boolean hasNext()
        {
            return advance(false);
        }

        @Override
        public boolean next()
        {
            return advance(true);
        }

        @SuppressWarnings("unchecked")
        @Override
        public K getKey()
        {
            if(this.allocatorHashMap == null)
                return null;
            return (K)this.node.key;
        }

        @SuppressWarnings("unchecked")
        @Override
        public V getValue()
        {
            if(this.allocatorHashMap == null)
                return null;
            return (V)this.node.value;
        }

        @Override
        public void free()
        {
            this.index = 0;
            this.allocatorHashMap = null;
            this.node = null;
            allocator.free(this);
        }

        @Override
        public boolean isEnd()
        {
            return this.allocatorHashMap == null;
        }

        public static IteratorImp<?, ?>
            allocate(final AllocatorHashMap<?, ?> allocatorHashMap)
        {
            return allocator.allocate().init(allocatorHashMap);
        }
    }

    @SuppressWarnings("unchecked")
    public final Iterator<K, V> iterator()
    {
        return (Iterator<K, V>)IteratorImp.allocate(this);
    }
}
