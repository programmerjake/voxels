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
import static org.voxels.World.world;

import java.io.*;
import java.net.Socket;

/** @author jacob */
public final class PlayerList
{
    /** the global player list */
    public static final PlayerList players = new PlayerList();
    /** the number of players in multiplayer */
    public static int PlayerCount = 0;
    private static final int hashPrime = 8191;

    private static class Node
    {
        public Node hashnext;
        @SuppressWarnings("unused")
        public String password;
        public Socket connection;
        public Player p;
        public Node next, prev;

        public Node(final String new_name,
                    final Player new_p,
                    final String new_password,
                    final Socket new_connection)
        {
            new_p.setName(new_name);
            this.password = new_password;
            this.p = new_p;
            this.connection = new_connection;
        }
    }

    private Node[] hashTable = new Node[hashPrime];
    private Node head, tail;

    private void destroy()
    {
        for(int i = 0; i < hashPrime; i++)
            this.hashTable[i] = null;
    }

    private static int hashString(final String str)
    {
        int retval = 0;
        for(int i = 0; i < str.length(); i++)
        {
            char ch = str.charAt(i);
            retval *= 0x100;
            retval += ch;
            retval %= hashPrime;
        }
        if(retval < 0)
            retval += hashPrime;
        return retval;
    }

    private void init()
    {
        for(int i = 0; i < hashPrime; i++)
            this.hashTable[i] = null;
        this.head = null;
        this.tail = null;
    }

    private Node insertPlayer(final Player p,
                              final Socket connection,
                              final String name,
                              final String password)
    {
        int hash = hashString(name);
        Node curNode = this.hashTable[hash];
        Node prevNode = null;
        while(curNode != null)
        {
            if(curNode.p.getName().equals(name))
            {
                if(prevNode != null)
                {
                    prevNode.hashnext = curNode.hashnext;
                    curNode.hashnext = this.hashTable[hash];
                    this.hashTable[hash] = curNode;
                }
                curNode.p = p;
                curNode.connection = connection;
                curNode.password = password;
                return curNode;
            }
            prevNode = curNode;
            curNode = curNode.hashnext;
        }
        curNode = new Node(name, p, password, connection);
        curNode.hashnext = this.hashTable[hash];
        this.hashTable[hash] = curNode;
        curNode.next = null;
        curNode.prev = this.tail;
        if(this.tail != null)
            this.tail.next = curNode;
        else
            this.head = curNode;
        this.tail = curNode;
        return curNode;
    }

    @SuppressWarnings("unused")
    private Player internalRemovePlayer(final String name)
    {
        int hash = hashString(name);
        Node curNode = this.hashTable[hash];
        Node prevNode = null;
        while(curNode != null)
        {
            if(curNode.p.getName().equals(name))
            {
                if(prevNode != null)
                    prevNode.hashnext = curNode.hashnext;
                else
                    this.hashTable[hash] = curNode.hashnext;
                Player retval = curNode.p;
                if(curNode.next != null)
                    curNode.next.prev = curNode.prev;
                else
                    this.tail = curNode.prev;
                if(curNode.prev != null)
                    curNode.prev.next = curNode.next;
                else
                    this.head = curNode.next;
                return retval;
            }
            prevNode = curNode;
            curNode = curNode.hashnext;
        }
        return null;
    }

    private Node findPlayer(final String name)
    {
        int hash = hashString(name);
        Node curNode = this.hashTable[hash];
        Node prevNode = null;
        while(curNode != null)
        {
            if(curNode.p.getName().equals(name))
            {
                if(prevNode != null)
                {
                    prevNode.hashnext = curNode.hashnext;
                    curNode.hashnext = this.hashTable[hash];
                    this.hashTable[hash] = curNode;
                }
                return curNode;
            }
            prevNode = curNode;
            curNode = curNode.hashnext;
        }
        return null;
    }

    /**
	 * 
	 */
    public PlayerList()
    {
        init();
    }

    /**
	 * 
	 */
    public void clear()
    {
        destroy();
        init();
    }

    /** @return the front player */
    public Player front()
    {
        if(this.head == null)
            return null;
        return this.head.p;
    }

    /** @return the front player's name */
    public String getFrontName()
    {
        if(this.head == null)
            return "";
        return this.head.p.getName();
    }

    private static Matrix draw_t1 = Matrix.allocate();

    /** draw from the perspective of the front player */
    public void draw()
    {
        if(this.head == null)
        {
            Main.opengl.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
            Main.opengl.glClear(Main.opengl.GL_DEPTH_BUFFER_BIT()
                    | Main.opengl.GL_COLOR_BUFFER_BIT());
            Main.opengl.glMatrixMode(Main.opengl.GL_MODELVIEW());
            Main.opengl.glLoadIdentity();
            String errormsg = "No Player Loaded!!!";
            float textwidth = Text.sizeW(errormsg) / Text.sizeW("A"), textheight = 1;
            Text.draw(Matrix.setToTranslate(draw_t1,
                                            -textwidth / 2,
                                            -textheight / 2,
                                            -20), RGB(255, 0, 0), errormsg);
        }
        else
        {
            this.head.p.drawAll();
        }
    }

    /** draw all the players
     * 
     * @param rs
     *            the rendering stream
     * @param worldToCamera
     *            Matrix that transforms world coordinates to camera coordinates
     * @return <code>rs</code> */
    public RenderingStream drawPlayers(final RenderingStream rs,
                                       final Matrix worldToCamera)
    {
        for(Node pnode = this.head; pnode != null; pnode = pnode.next)
        {
            pnode.p.draw(rs, worldToCamera);
        }
        return rs;
    }

    /** check for all entities hitting player */
    public void entityCheckHitPlayers()
    {
        if(PlayerCount > 0 && !Main.isServer)
            return;
        for(Node pnode = this.head; pnode != null; pnode = pnode.next)
        {
            if(PlayerCount > 0 && pnode.connection == null)
                continue;
            world.checkHitPlayer(pnode.p);
        }
    }

    /** get a player by name
     * 
     * @param name
     *            the name to look for
     * @return the player found or <code>null</code> if no player matches */
    public Player getPlayer(final String name)
    {
        Node node = findPlayer(name);
        if(node == null)
            return null;
        return node.p;
    }

    /** add a default player with no name and no password */
    public void addDefaultPlayer()
    {
        insertPlayer(Player.allocate(), null, "", "");
    }

    /** @param mouseX
     *            mouse x position
     * @param mouseY
     *            mouse y position
     * @param mouseLButton
     *            if the mouse's left button is pressed
     * @return the mouse move kind */
    public Player.MouseMoveKind handleMouseMove(final float mouseX,
                                                final float mouseY,
                                                final boolean mouseLButton)
    {
        if(this.head == null)
            return Player.MouseMoveKind.Normal;
        return front().handleMouseMove(mouseX, mouseY, mouseLButton);
    }

    /** @param event
     *            the event to handle */
    public void handleMouseUpDown(final Main.MouseEvent event)
    {
        if(this.head == null)
            return;
        front().handleMouseUpDown(event);
    }

    /** move all the players */
    public void move()
    {
        if(this.head == null)
            return;
        front().move();
    }

    /** @param event
     *            the event to handle */
    public void handleKeyboardEvent(final Main.KeyboardEvent event)
    {
        if(this.head == null)
            return;
        front().handleKeyboardEvent(event);
    }

    /** write to a <code>DataOutput</code>
     * 
     * @param o
     *            <code>DataOutput</code> to write to
     * @throws IOException
     *             the exception thrown */
    public void write(final DataOutput o) throws IOException
    {
        if(this.head == null)
            throw new IOException("no player to write");
        front().write(o);
    }

    /** read from a <code>DataInput</code>
     * 
     * @param i
     *            <code>DataInput</code> to read from
     * @param fileVersion
     *            the file version
     * @throws IOException
     *             the exception thrown */
    public static void
        read(final DataInput i, final int fileVersion) throws IOException
    {
        players.clear();
        players.insertPlayer(Player.read(i, fileVersion), null, "", "");
    }

    /** push all players inside of &lt;<code>bx</code>, <code>by</code>,
     * <code>bz</code>&gt; out in the direction &lt;<code>dx</code>,
     * <code>dy</code>, <code>dz</code>&gt;
     * 
     * @param bx
     *            x coordinate of the block to push out of
     * @param by
     *            y coordinate of the block to push out of
     * @param bz
     *            z coordinate of the block to push out of
     * @param dx
     *            x coordinate of the direction to push
     * @param dy
     *            y coordinate of the direction to push
     * @param dz
     *            z coordinate of the direction to push */
    public void push(final int bx,
                     final int by,
                     final int bz,
                     final int dx,
                     final int dy,
                     final int dz)
    {
        if(PlayerCount > 0 && !Main.isServer)
            return;
        for(Node pnode = this.head; pnode != null; pnode = pnode.next)
        {
            pnode.p.push(bx, by, bz, dx, dy, dz);
        }
    }

    public void handleEntityRemove(final Entity e)
    {
        if(PlayerCount > 0 && !Main.isServer)
            return;
        for(Node pnode = this.head; pnode != null; pnode = pnode.next)
        {
            pnode.p.handleEntityRemove(e);
        }
    }

    public static final class PlayerIterator
    {
        private Node node;

        private PlayerIterator()
        {
        }

        private static final Allocator<PlayerIterator> allocator = new Allocator<PlayerList.PlayerIterator>()
        {
            @SuppressWarnings("synthetic-access")
            @Override
            protected PlayerIterator allocateInternal()
            {
                return new PlayerIterator();
            }
        };

        public void free()
        {
            this.node = null;
            allocator.free(this);
        }

        public void next()
        {
            if(this.node != null)
            {
                this.node = this.node.next;
            }
        }

        public boolean isEnd()
        {
            return this.node == null;
        }

        public Player get()
        {
            return this.node.p;
        }

        public static PlayerIterator allocate(final Node node)
        {
            PlayerIterator retval = allocator.allocate();
            retval.node = node;
            return retval;
        }
    }

    public PlayerIterator iterator()
    {
        return PlayerIterator.allocate(this.head);
    }
}
