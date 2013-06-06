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

import static org.lwjgl.opengl.GL11.*;
import static org.voxels.Color.RGB;
import static org.voxels.World.world;

import java.io.*;
import java.net.Socket;

/**
 * @author jacob
 * 
 */
public final class PlayerList
{
	/**
	 * the global player list
	 */
	static public PlayerList players = new PlayerList();
	/**
	 * the number of players in multiplayer
	 */
	static public int PlayerCount = 0;
	private static final int hashPrime = 8191;

	private static class Node
	{
		public Node hashnext;
		public final String name;
		@SuppressWarnings("unused")
		public String password;
		public Socket connection;
		public Player p;
		public Node next, prev;

		public Node(String new_name,
		            Player new_p,
		            String new_password,
		            Socket new_connection)
		{
			this.name = new_name;
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

	private static int hashString(String str)
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

	private Node insertPlayer(Player p,
	                          Socket connection,
	                          String name,
	                          String password)
	{
		int hash = hashString(name);
		Node curNode = this.hashTable[hash];
		Node prevNode = null;
		while(curNode != null)
		{
			if(curNode.name.equals(name))
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
	private Player internalRemovePlayer(String name)
	{
		int hash = hashString(name);
		Node curNode = this.hashTable[hash];
		Node prevNode = null;
		while(curNode != null)
		{
			if(curNode.name.equals(name))
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

	private Node findPlayer(String name)
	{
		int hash = hashString(name);
		Node curNode = this.hashTable[hash];
		Node prevNode = null;
		while(curNode != null)
		{
			if(curNode.name.equals(name))
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

	/**
	 * @return the front player
	 */
	public Player front()
	{
		if(this.head == null)
			return null;
		return this.head.p;
	}

	/**
	 * @return the front player's name
	 */
	public String getFrontName()
	{
		if(this.head == null)
			return "";
		return this.head.name;
	}

	/**
	 * draw from the perspective of the front player
	 */
	public void draw()
	{
		if(this.head == null)
		{
			glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
			glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
			glMatrixMode(GL_MODELVIEW);
			glLoadIdentity();
			String errormsg = "No Player Loaded!!!";
			float textwidth = Text.sizeW(errormsg) / Text.sizeW("A"), textheight = 1;
			Text.draw(Matrix.translate(-textwidth / 2, -textheight / 2, -20),
			          RGB(255, 0, 0),
			          errormsg);
		}
		else
		{
			this.head.p.drawAll();
		}
	}

	/**
	 * draw all the players
	 * 
	 * @param worldToCamera
	 *            Matrix that transforms world coordinates to camera coordinates
	 */
	public void drawPlayers(Matrix worldToCamera)
	{
		for(Node pnode = this.head; pnode != null; pnode = pnode.next)
		{
			pnode.p.draw(worldToCamera);
		}
	}

	/**
	 * check for all entities hitting player
	 */
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

	/**
	 * get a player by name
	 * 
	 * @param name
	 *            the name to look for
	 * @return the player found or <code>null</code> if no player matches
	 */
	public Player getPlayer(String name)
	{
		Node node = findPlayer(name);
		if(node == null)
			return null;
		return node.p;
	}

	/**
	 * add a default player with no name and no password
	 */
	public void addDefaultPlayer()
	{
		insertPlayer(new Player(), null, "", "");
	}

	/**
	 * @param mouseX
	 *            mouse x position
	 * @param mouseY
	 *            mouse y position
	 * @param mouseLButton
	 *            if the mouse's left button is pressed
	 * @return true to move mouse to center
	 */
	public boolean
	    handleMouseMove(int mouseX, int mouseY, boolean mouseLButton)
	{
		if(this.head == null)
			return false;
		return front().handleMouseMove(mouseX, mouseY, mouseLButton);
	}

	/**
	 * @param event
	 *            the event to handle
	 */
	public void handleMouseUpDown(final Main.MouseEvent event)
	{
		if(this.head == null)
			return;
		front().handleMouseUpDown(event);
	}

	/**
	 * move all the players
	 */
	public void move()
	{
		if(this.head == null)
			return;
		front().move();
	}

	/**
	 * @param event
	 *            the event to handle
	 */
	public void handleKeyboardEvent(Main.KeyboardEvent event)
	{
		if(this.head == null)
			return;
		front().handleKeyboardEvent(event);
	}

	/**
	 * write to a <code>DataOutput</code>
	 * 
	 * @param o
	 *            <code>DataOutput</code> to write to
	 * @throws IOException
	 *             the exception thrown
	 */
	public void write(DataOutput o) throws IOException
	{
		if(this.head == null)
			throw new IOException("no player to write");
		front().write(o);
	}

	/**
	 * read from a <code>DataInput</code>
	 * 
	 * @param i
	 *            <code>DataInput</code> to read from
	 * @throws IOException
	 *             the exception thrown
	 */
	public static void read(DataInput i) throws IOException
	{
		players.clear();
		players.insertPlayer(Player.read(i), null, "", "");
	}

	/**
	 * push all players inside of &lt;<code>bx</code>, <code>by</code>,
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
	 *            z coordinate of the direction to push
	 */
	public void push(int bx, int by, int bz, int dx, int dy, int dz)
	{
		if(PlayerCount > 0 && !Main.isServer)
			return;
		for(Node pnode = this.head; pnode != null; pnode = pnode.next)
		{
			pnode.p.push(bx, by, bz, dx, dy, dz);
		}
	}
}
