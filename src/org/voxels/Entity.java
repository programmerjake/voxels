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
import static org.voxels.Matrix.glMultMatrix;
import static org.voxels.World.GravityAcceleration;
import static org.voxels.World.world;

import java.io.*;

/**
 * @author jacob
 * 
 */
public class Entity implements GameObject
{
	private Vector position = new Vector(0);
	private EntityType type;
	private static Image imgFire = new Image("particlefire.png");
	private static Image imgRedstoneFire = new Image("particleredstonefire.png");
	private static Image imgSmoke = new Image("particlesmoke.png");

	private static class Data
	{
		public Data()
		{
		}

		public BlockType blocktype;
		public float theta, phi;
		public Vector velocity;
		public double existduration;
		public boolean nearperson;
		public ParticleType particletype;
	}

	Data data;

	/**
	 * create an empty entity
	 */
	public Entity()
	{
		this.type = EntityType.Nothing;
		this.data = null;
	}

	private Entity(Vector position, EntityType type)
	{
		this.position = position;
		this.type = type;
		this.data = new Data();
	}

	/**
	 * create a copy of an entity
	 * 
	 * @param rt
	 *            entity to create a copy of
	 */
	public Entity(Entity rt)
	{
		this.type = rt.type;
		this.position = new Vector(rt.position);
		this.data = new Data();
		switch(this.type)
		{
		case Last:
			break;
		case Nothing:
			break;
		case Block:
			this.data.blocktype = rt.data.blocktype;
			this.data.theta = rt.data.theta;
			this.data.phi = rt.data.phi;
			this.data.velocity = rt.data.velocity;
			this.data.existduration = rt.data.existduration;
			this.data.nearperson = rt.data.nearperson;
			break;
		case Particle:
			this.data.velocity = rt.data.velocity;
			this.data.particletype = rt.data.particletype;
			this.data.theta = rt.data.theta;
			this.data.phi = rt.data.phi;
			this.data.existduration = rt.data.existduration;
			break;
		case FallingBlock:
			this.data.blocktype = rt.data.blocktype;
			this.data.velocity = rt.data.velocity;
			break;
		}
	}

	private void clear()
	{
		this.type = EntityType.Nothing;
		this.data = null;
	}

	@Override
	public void draw(Matrix worldToCamera)
	{
		switch(this.type)
		{
		case Last:
			break;
		case Nothing:
			break;
		case Block:
		{
			Block b = Block.make(this.data.blocktype);
			if(b != null)
			{
				Matrix tform = Matrix.translate(-0.5f, -0.5f, -0.5f)
				                     .concat(Matrix.rotatex(this.data.phi))
				                     .concat(Matrix.rotatey(this.data.theta))
				                     .concat(Matrix.scale(0.125f))
				                     .concat(Matrix.translate(this.position));
				glMatrixMode(GL_MODELVIEW);
				glPushMatrix();
				glMultMatrix(worldToCamera);
				b.drawAsEntity(tform);
				glPopMatrix();
			}
			break;
		}
		case Particle:
		{
			Image img = null;
			switch(this.data.particletype)
			{
			case Last:
				break;
			case Fire:
				img = imgFire;
				break;
			case RedstoneFire:
				img = imgRedstoneFire;
				break;
			case Smoke:
				img = imgSmoke;
				break;
			}
			if(img != null)
			{
				Matrix tform = Matrix.translate(-0.5f, -0.5f, -0.5f);
				tform = tform.concat(Matrix.rotatex(this.data.phi));
				tform = tform.concat(Matrix.rotatey(this.data.theta));
				tform = tform.concat(Matrix.scale(0.125f));
				tform = tform.concat(Matrix.translate(this.position));
				glMatrixMode(GL_MODELVIEW);
				glPushMatrix();
				glMultMatrix(worldToCamera);
				Block.drawImgAsEntity(tform, img);
				glPopMatrix();
			}
			break;
		}
		case FallingBlock:
		{
			Block b = Block.make(this.data.blocktype);
			if(b != null)
			{
				glMatrixMode(GL_MODELVIEW);
				glPushMatrix();
				glMultMatrix(worldToCamera);
				b.drawAsEntity(Matrix.translate(this.position));
				glPopMatrix();
			}
			break;
		}
		}
	}

	@Override
	public void move()
	{
		switch(this.type)
		{
		case Last:
			break;
		case Nothing:
			break;
		case Block:
		{
			if(this.data.nearperson)
			{
				this.position = this.position.add(this.data.velocity.mul((float)Main.getFrameDuration()));
				this.data.nearperson = false;
				this.data.theta = (this.data.theta + (float)Main.getFrameDuration()
				        * 0.5f * (float)Math.PI)
				        % (float)(2 * Math.PI);
				break;
			}
			this.data.existduration += Main.getFrameDuration();
			if(this.data.existduration > 60.0 * 6) // 6 min
			{
				clear();
				return;
			}
			if(this.position.y < -World.Depth)
			{
				clear();
				return;
			}
			Block b = world.getBlock((int)Math.floor(this.position.x),
			                         (int)Math.floor(this.position.y),
			                         (int)Math.floor(this.position.z));
			if(b == null || b.isSupporting())
			{
				this.data.velocity = new Vector(0.0f);
			}
			else
			{
				b = world.getBlock((int)Math.floor(this.position.x),
				                   (int)Math.floor(this.position.y) - 1,
				                   (int)Math.floor(this.position.z));
				if(b == null || b.isSupporting())
				{
					Vector newvelocity = this.data.velocity.div(1.5f)
					                                       .add(new Vector(0.0f,
					                                                       1.2f
					                                                               * GravityAcceleration
					                                                               * (this.position.y - (float)Math.floor(this.position.y)),
					                                                       0.0f))
					                                       .add(new Vector(0.0f,
					                                                       -GravityAcceleration,
					                                                       0.0f));
					this.data.velocity = this.data.velocity.add(newvelocity.sub(this.data.velocity)
					                                                       .mul((float)Main.getFrameDuration()));
					float miny = (float)Math.floor(this.position.y) + 0.1f;
					this.position = this.position.add(this.data.velocity.mul((float)Main.getFrameDuration()));
					if(this.position.y < miny)
					{
						this.position.y = miny;
						this.data.velocity.y = 0;
					}
				}
				else
				{
					this.data.velocity = this.data.velocity.add(new Vector(0.0f,
					                                                       -GravityAcceleration,
					                                                       0.0f).mul((float)Main.getFrameDuration()));
					if(this.data.velocity.y < -15.0f)
						this.data.velocity.y = -15.0f;
					this.position = this.position.add(this.data.velocity.mul((float)Main.getFrameDuration()));
				}
			}
			this.data.theta = (this.data.theta + (float)Main.getFrameDuration()
			        * 0.5f * (float)Math.PI)
			        % (float)(2 * Math.PI);
			break;
		}
		case Particle:
		{
			this.data.existduration -= Main.getFrameDuration();
			if(this.data.existduration <= 0)
			{
				clear();
				return;
			}
			switch(this.data.particletype)
			{
			case Last:
				break;
			case Fire:
			case RedstoneFire:
			case Smoke:
				this.data.velocity = this.data.velocity.add(new Vector(0,
				                                                       (float)Main.getFrameDuration(),
				                                                       0))
				                                       .mul((float)Math.pow(0.3f,
				                                                            (float)Main.getFrameDuration()));
				break;
			}
			this.position = this.position.add(this.data.velocity.mul((float)Main.getFrameDuration()));
			break;
		}
		case FallingBlock:
		{
			int x = Math.round(this.position.x);
			int y = Math.round(this.position.y);
			int z = Math.round(this.position.z);
			Block b = world.getBlock(x, y - 1, z);
			if(b == null || b.isSupporting())
			{
				b = Block.make(this.data.blocktype);
				if(b == null)
					b = new Block();
				world.setBlock(x, y, z, b);
				clear();
				return;
			}
			Vector deltapos = this.data.velocity.mul((float)Main.getFrameDuration());
			if(deltapos.abs_squared() > 1)
				deltapos = deltapos.normalize();
			this.position = this.position.add(deltapos);
			this.data.velocity = this.data.velocity.add(new Vector(0,
			                                                       -GravityAcceleration
			                                                               * (float)Main.getFrameDuration(),
			                                                       0));
			if(this.data.velocity.y < -15.0f)
				this.data.velocity.y = -15.0f;
			break;
		}
		}
	}

	/**
	 * check for hitting player <code>p</code> and run the corresponding action
	 * if <code>p</code> got hit
	 * 
	 * @param p
	 *            player to check for
	 */
	public void checkHitPlayer(Player p)
	{
		switch(this.type)
		{
		case Last:
			break;
		case Nothing:
			break;
		case Block:
		{
			Vector ppos = p.getPosition();
			Vector disp = ppos.sub(this.position);
			if(disp.abs_squared() <= 0.3f * 0.3f)
			{
				p.giveBlock(this.data.blocktype, false);
				clear();
			}
			else if(disp.abs_squared() <= 3.0f * 3.0f)
			{
				float speed = this.data.velocity.abs();
				speed += Main.getFrameDuration() * World.GravityAcceleration;
				if(speed > 15.0f)
					speed = 15.0f;
				this.data.velocity = p.getPosition()
				                      .sub(this.position)
				                      .normalize()
				                      .mul(speed);
				this.data.nearperson = true;
			}
			break;
		}
		case Particle:
			break;
		case FallingBlock:
			break;
		}
	}

	/**
	 * @return this entity's position
	 */
	public Vector getPosition()
	{
		return this.position;
	}

	/**
	 * @return true if this entity is empty
	 */
	public boolean isEmpty()
	{
		return this.type == EntityType.Nothing;
	}

	/**
	 * create a new block entity
	 * 
	 * @param position
	 *            the initial position to create it at
	 * @param bt
	 *            the block type of the entity
	 * @param velocity
	 *            the initial velocity of the created entity
	 * @return the new block entity
	 */
	public static Entity
	    NewBlock(Vector position, BlockType bt, Vector velocity)
	{
		Entity retval = new Entity(new Vector(position), EntityType.Block);
		retval.data.blocktype = bt;
		retval.data.existduration = 0;
		retval.data.phi = 0;
		retval.data.theta = World.fRand(0.0f, 2 * (float)Math.PI);
		retval.data.velocity = new Vector(velocity);
		retval.data.nearperson = false;
		return retval;
	}

	/**
	 * create a new particle
	 * 
	 * @param position
	 *            the initial position of the particle
	 * @param pt
	 *            the type of particle
	 * @param velocity
	 *            the initial velocity of the particle
	 * @return the new particle
	 */
	public static Entity NewParticle(Vector position,
	                                 ParticleType pt,
	                                 Vector velocity)
	{
		Entity retval = new Entity(new Vector(position), EntityType.Particle);
		retval.data.velocity = new Vector(velocity);
		retval.data.particletype = pt;
		retval.data.phi = World.fRand(-(float)Math.PI, (float)Math.PI);
		retval.data.theta = World.fRand(0.0f, 2 * (float)Math.PI);
		switch(pt)
		{
		case Last:
			break;
		case Fire:
		case RedstoneFire:
			retval.data.existduration = World.fRand(0.0f, 1.0f);
			break;
		case Smoke:
			retval.data.existduration = Math.pow(World.fRand(0.0f, 1.0f), 4.0) * 5.0;
			break;
		}
		return retval;
	}

	/**
	 * create a new falling block
	 * 
	 * @param position
	 *            the initial position of the falling block
	 * @param bt
	 *            the type of block
	 * @return the new falling block
	 */
	public static Entity NewFallingBlock(Vector position, BlockType bt)
	{
		Entity retval = new Entity(new Vector(position),
		                           EntityType.FallingBlock);
		retval.data.blocktype = bt;
		retval.data.velocity = new Vector(0);
		return retval;
	}

	private void readPhiTheta(DataInput i) throws IOException
	{
		this.data.phi = i.readFloat();
		if(Float.isInfinite(this.data.phi) || Float.isNaN(this.data.phi)
		        || Math.abs(this.data.phi) > 1e-4 + Math.PI / 2)
			throw new IOException("phi out of range");
		this.data.theta = i.readFloat();
		if(Float.isInfinite(this.data.theta) || Float.isNaN(this.data.theta)
		        || Math.abs(this.data.theta) > 1e-4 + Math.PI * 2)
			throw new IOException("theta out of range");
	}

	private void internalRead(DataInput i) throws IOException
	{
		switch(this.type)
		{
		case Last:
		case Nothing:
			return;
		case Block:
		{
			this.data.blocktype = BlockType.read(i);
			this.data.existduration = i.readDouble();
			if(Double.isInfinite(this.data.existduration)
			        || Double.isNaN(this.data.existduration)
			        || this.data.existduration < 0
			        || this.data.existduration > 1e5)
				throw new IOException("exist duration out of range");
			readPhiTheta(i);
			this.data.velocity = Vector.read(i);
			this.data.nearperson = i.readBoolean();
			return;
		}
		case FallingBlock:
		{
			this.data.blocktype = BlockType.read(i);
			this.data.velocity = Vector.read(i);
			return;
		}
		case Particle:
		{
			this.data.velocity = Vector.read(i);
			this.data.particletype = ParticleType.read(i);
			readPhiTheta(i);
			switch(this.data.particletype)
			{
			case Last:
				break;
			case Fire:
			case RedstoneFire:
			case Smoke:
				this.data.existduration = i.readDouble();
				if(Double.isInfinite(this.data.existduration)
				        || Double.isNaN(this.data.existduration)
				        || this.data.existduration < 0
				        || this.data.existduration > 1e5)
					throw new IOException("exist duration out of range");
				break;
			}
			return;
		}
		}
	}

	/**
	 * read from a <code>DataInput</code>
	 * 
	 * @param i
	 *            <code>DataInput</code> to read from
	 * @return the read <code>Entity</code>
	 * @throws IOException
	 *             the exception thrown
	 */
	public static Entity read(DataInput i) throws IOException
	{
		EntityType type = EntityType.read(i);
		if(type == EntityType.Nothing)
			return new Entity();
		Vector position = Vector.read(i);
		Entity retval = new Entity(position, type);
		retval.internalRead(i);
		return retval;
	}

	/**
	 * write to a <code>DataOutput</code>
	 * 
	 * @param o
	 *            <code>OutputStream</code> to write to
	 * @throws IOException
	 *             the exception thrown
	 */
	public void write(DataOutput o) throws IOException
	{
		this.type.write(o);
		if(this.type == EntityType.Nothing)
			return;
		this.position.write(o);
		switch(this.type)
		{
		case Last:
		case Nothing:
			return;
		case Block:
		{
			this.data.blocktype.write(o);
			o.writeDouble(this.data.existduration);
			o.writeFloat(this.data.phi);
			o.writeFloat(this.data.theta);
			this.data.velocity.write(o);
			o.writeBoolean(this.data.nearperson);
			return;
		}
		case FallingBlock:
		{
			this.data.blocktype.write(o);
			this.data.velocity.write(o);
			return;
		}
		case Particle:
		{
			this.data.velocity.write(o);
			this.data.particletype.write(o);
			o.writeFloat(this.data.phi);
			o.writeFloat(this.data.theta);
			switch(this.data.particletype)
			{
			case Last:
				break;
			case Fire:
			case RedstoneFire:
			case Smoke:
				o.writeDouble(this.data.existduration);
				break;
			}
			return;
		}
		}
	}
}
