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

import static org.voxels.World.GravityAcceleration;
import static org.voxels.World.world;

import java.io.*;

import org.voxels.TextureAtlas.TextureHandle;

/** @author jacob */
public class Entity implements GameObject
{
    private Vector position = new Vector(0);
    private EntityType type;
    private static TextureHandle imgFire = TextureAtlas.addImage(new Image("particlefire.png"));
    private static TextureHandle imgRedstoneFire = TextureAtlas.addImage(new Image("particleredstonefire.png"));
    private static TextureHandle imgSmoke = TextureAtlas.addImage(new Image("particlesmoke.png"));
    private static final int SimulationAnimationFrameCount = 128;
    private static final float SimulationAnimationFrameRate = 30.0f;
    private static final float SmokeSimulationAnimationFrameRate = 15.0f;
    private static TextureHandle[] imgSmokeAnimation = null;
    private static TextureHandle[] imgFireAnimation = null;
    private static TextureHandle[] imgRedstoneFireAnimation = null;

    private static TextureHandle[] genAnimation(FireSimulation sim,
                                                int stepCount)
    {
        TextureHandle[] retval = new TextureHandle[SimulationAnimationFrameCount];
        Main.pushProgress(0, 1.0f / SimulationAnimationFrameCount);
        for(int frame = 0; frame < SimulationAnimationFrameCount; frame++)
        {
            for(int i = 0; i < stepCount; i++)
                sim.step();
            retval[frame] = TextureAtlas.addImage(new Image(sim.getImage()));
            Main.setProgress(frame);
        }
        Main.popProgress();
        return retval;
    }

    static void init()
    {
        Main.pushProgress(0.0f / 3, 1.0f / 3);
        imgSmokeAnimation = genAnimation(new SmokeSimulation(), 1);
        Main.popProgress();
        Main.pushProgress(1.0f / 3, 1.0f / 3);
        imgFireAnimation = genAnimation(new FireSimulation(), 2);
        Main.popProgress();
        Main.pushProgress(2.0f / 3, 1.0f / 3);
        imgRedstoneFireAnimation = genAnimation(new RedstoneFireSimulation(), 2);
        Main.popProgress();
    }

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
        public float frame;
    }

    Data data;

    /** create an empty entity */
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

    /** create a copy of an entity
     * 
     * @param rt
     *            entity to create a copy of */
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
            this.data.frame = rt.data.frame;
            break;
        case FallingBlock:
            this.data.blocktype = rt.data.blocktype;
            this.data.velocity = rt.data.velocity;
            break;
        case PrimedTNT:
            this.data.velocity = rt.data.velocity;
            this.data.existduration = rt.data.existduration;
            break;
        }
    }

    private void clear()
    {
        this.type = EntityType.Nothing;
        this.data = null;
    }

    @Override
    public RenderingStream draw(RenderingStream rs, Matrix worldToCamera)
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
                rs.pushMatrixStack();
                rs.concatMatrix(worldToCamera);
                b.drawAsEntity(rs, tform);
                rs.popMatrixStack();
            }
            break;
        }
        case Particle:
        {
            TextureHandle img = null;
            boolean isAnim = false;
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
            case FireAnim:
            {
                isAnim = true;
                int frame = (int)Math.floor(this.data.frame)
                        % SimulationAnimationFrameCount;
                if(frame < 0)
                    frame += SimulationAnimationFrameCount;
                img = imgFireAnimation[frame];
                break;
            }
            case RedstoneFireAnim:
            {
                isAnim = true;
                int frame = (int)Math.floor(this.data.frame)
                        % SimulationAnimationFrameCount;
                if(frame < 0)
                    frame += SimulationAnimationFrameCount;
                img = imgRedstoneFireAnimation[frame];
                break;
            }
            case SmokeAnim:
            {
                isAnim = true;
                int frame = (int)Math.floor(this.data.frame)
                        % SimulationAnimationFrameCount;
                if(frame < 0)
                    frame += SimulationAnimationFrameCount;
                img = imgSmokeAnimation[frame];
                break;
            }
            }
            if(img != null)
            {
                Matrix tform = Matrix.translate(-0.5f,
                                                isAnim ? 0.0f : -0.5f,
                                                -0.5f);
                tform = tform.concat(Matrix.rotatex(this.data.phi));
                tform = tform.concat(Matrix.rotatey(this.data.theta));
                if(!isAnim)
                    tform = tform.concat(Matrix.scale(0.125f));
                tform = tform.concat(Matrix.translate(this.position));
                rs.pushMatrixStack();
                rs.concatMatrix(worldToCamera);
                Block.drawImgAsEntity(rs, tform, img);
                rs.popMatrixStack();
            }
            break;
        }
        case FallingBlock:
        {
            Block b = Block.make(this.data.blocktype);
            if(b != null)
            {
                rs.pushMatrixStack();
                rs.concatMatrix(worldToCamera);
                b.drawAsEntity(rs, Matrix.translate(this.position));
                rs.popMatrixStack();
            }
            break;
        }
        case PrimedTNT:
        {
            Block b = Block.NewTNT();
            rs.pushMatrixStack();
            rs.concatMatrix(worldToCamera);
            b.drawAsEntity(rs, Matrix.translate(this.position));
            rs.popMatrixStack();
            break;
        }
        }
        return rs;
    }

    private Vector hitObstruction(Vector newPos)
    {
        Vector dir = newPos.sub(this.position);
        float dist = dir.abs();
        if(dist == 0)
            return null;
        dir = dir.div(dist);
        World.BlockHitDescriptor blockHitDescriptor = world.getPointedAtBlock(this.position,
                                                                              dir,
                                                                              dist,
                                                                              false,
                                                                              true);
        if(blockHitDescriptor.b != null || blockHitDescriptor.hitUnloadedChunk)
            return this.position.add(dir.mul(blockHitDescriptor.distance));
        return null;
    }

    private static final float TNTStrength = 5 * 6 * 6 * 7 * 7 * 7;

    private boolean canBeInBlock(Block b)
    {
        if(b == null)
            return false;
        return !b.isSolid();
    }

    /** @param deltaPos
     *            the value that you add to <code>this.position</code> to get
     *            the actual position
     * @param size
     *            the size of this entity
     * @return if this entity can move to any block */
    private boolean moveToNearestEmptySpace(Vector deltaPos, float size)
    {
        int px = (int)Math.floor(this.position.x + deltaPos.x);
        int py = (int)Math.floor(this.position.y + deltaPos.y);
        int pz = (int)Math.floor(this.position.z + deltaPos.z);
        if(canBeInBlock(world.getBlockEval(px, py, pz)))
            return true;
        for(int dist = 1; dist < 64; dist++)
        {
            for(int dx = -dist; dx <= dist; dx++)
            {
                for(int dy = -dist; dy <= dist; dy++)
                {
                    for(int dz = -dist; dz <= dist; dz++)
                    {
                        if(Math.max(Math.max(Math.abs(dx), Math.abs(dy)),
                                    Math.abs(dz)) != dist)
                            dz = dist;
                        int x = px + dx, y = py + dy, z = pz + dz;
                        if(canBeInBlock(world.getBlockEval(x, y, z)))
                        {
                            Vector pos = new Vector(x + 0.5f,
                                                    y + 0.5f,
                                                    z + 0.5f);
                            Vector dir = this.position.add(deltaPos).sub(pos);
                            float magnitude = Math.max(dir.x,
                                                       Math.max(dir.y, dir.z));
                            if(magnitude == 0)
                                magnitude = 1;
                            dir = dir.mul(Math.max(0, 1.0f - size) / magnitude);
                            this.position = pos.add(dir).sub(deltaPos);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
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
                    Vector newPos = this.position.add(this.data.velocity.mul((float)Main.getFrameDuration()));
                    Vector hitObstructionRetval = hitObstruction(newPos);
                    if(hitObstructionRetval == null)
                        this.position = newPos;
                    else
                    {
                        this.position = hitObstructionRetval;
                        this.data.velocity = new Vector(0);
                    }
                    if(!moveToNearestEmptySpace(new Vector(0), 1.0f / 7))
                    {
                        clear();
                        return;
                    }
                    if(this.position.y < miny)
                    {
                        this.position.y = miny;
                        this.data.velocity.y = 0;
                    }
                    b = world.getBlock((int)Math.floor(this.position.x),
                                       (int)Math.floor(this.position.y),
                                       (int)Math.floor(this.position.z));
                    if(this.position.y < miny + 0.2f && b != null
                            && b.getType() == BlockType.BTWoodPressurePlate)
                    {
                        b = new Block(b);
                        b.pressurePlatePress();
                        world.setBlock((int)Math.floor(this.position.x),
                                       (int)Math.floor(this.position.y),
                                       (int)Math.floor(this.position.z),
                                       b);
                    }
                }
                else
                {
                    this.data.velocity = this.data.velocity.add(new Vector(0.0f,
                                                                           -GravityAcceleration,
                                                                           0.0f).mul((float)Main.getFrameDuration()));
                    if(this.data.velocity.y < -15.0f)
                        this.data.velocity.y = -15.0f;
                    Vector newPos = this.position.add(this.data.velocity.mul((float)Main.getFrameDuration()));
                    Vector hitObstructionRetval = hitObstruction(newPos);
                    if(hitObstructionRetval == null)
                        this.position = newPos;
                    else
                    {
                        this.position = hitObstructionRetval;
                        this.data.velocity = new Vector(0);
                    }
                    if(!moveToNearestEmptySpace(new Vector(0), 1.0f / 7))
                    {
                        clear();
                        return;
                    }
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
            case FireAnim:
            case RedstoneFireAnim:
                this.data.velocity = new Vector(0);
                this.data.frame += SimulationAnimationFrameRate
                        * (float)Main.getFrameDuration();
                this.data.frame %= SimulationAnimationFrameCount;
                break;
            case SmokeAnim:
                this.data.velocity = new Vector(0);
                this.data.frame += SmokeSimulationAnimationFrameRate
                        * (float)Main.getFrameDuration();
                this.data.frame %= SimulationAnimationFrameCount;
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
        case PrimedTNT:
        {
            int x = Math.round(this.position.x);
            int y = Math.round(this.position.y);
            int z = Math.round(this.position.z);
            final float ParticlesPerSecond = 15;
            int startcount = (int)Math.floor(this.data.existduration
                    * ParticlesPerSecond);
            this.data.existduration -= Main.getFrameDuration();
            int endcount = (int)Math.floor(this.data.existduration
                    * ParticlesPerSecond);
            int count = startcount - endcount;
            for(int i = 0; i < count; i++)
            {
                world.insertEntity(NewParticle(this.position.add(new Vector(World.fRand(0,
                                                                                        1),
                                                                            1.0f,
                                                                            World.fRand(0,
                                                                                        1))),
                                               ParticleType.SmokeAnim,
                                               new Vector(0)));
            }
            if(this.data.existduration <= 0)
            {
                world.addExplosion(x, y, z, TNTStrength);
                clear();
                return;
            }
            Block b = world.getBlock(x, y - 1, z);
            if(b == null || b.isSupporting())
            {
                this.data.velocity = new Vector(0);
                break;
            }
            Vector deltapos = this.data.velocity.mul((float)Main.getFrameDuration());
            if(deltapos.abs_squared() > 1)
                deltapos = deltapos.normalize();
            this.position = this.position.add(deltapos);
            Vector newPos = this.position.add(deltapos);
            Vector hitObstructionRetval = hitObstruction(newPos);
            if(hitObstructionRetval == null)
                this.position = newPos;
            else
            {
                this.position = hitObstructionRetval;
                this.data.velocity = new Vector(0);
            }
            if(!moveToNearestEmptySpace(new Vector(0.5f), 1.0f))
            {
                world.addExplosion(x, y, z, TNTStrength);
                clear();
                return;
            }
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

    /** check for hitting player <code>p</code> and run the corresponding action
     * if <code>p</code> got hit
     * 
     * @param p
     *            player to check for */
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
        case PrimedTNT:
            break;
        }
    }

    /** @return this entity's position */
    public Vector getPosition()
    {
        return this.position;
    }

    /** @return true if this entity is empty */
    public boolean isEmpty()
    {
        return this.type == EntityType.Nothing;
    }

    /** check for moving from an explosion
     * 
     * @param pos
     *            the explosion position
     * @param strength
     *            the explosion strength */
    public void explode(Vector pos, float strength)
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
                return;
            }
            float actualStrength = strength;
            actualStrength -= 5 * this.position.sub(pos).abs();
            if(actualStrength <= 0)
                return;
            Vector dir = this.position.sub(pos)
                                      .normalize()
                                      .mul(actualStrength * 10 / TNTStrength);
            this.data.velocity = this.data.velocity.add(dir);
            break;
        }
        case Particle:
        {
            break;
        }
        case FallingBlock:
        {
            break;
        }
        case PrimedTNT:
        {
            float actualStrength = strength;
            actualStrength -= 5 * this.position.sub(pos)
                                               .add(new Vector(0.5f))
                                               .abs();
            if(actualStrength <= 0)
                return;
            Vector dir = this.position.sub(pos)
                                      .add(new Vector(0.5f))
                                      .normalize()
                                      .mul(actualStrength * 10 / TNTStrength);
            this.data.velocity = this.data.velocity.add(dir);
            break;
        }
        }
    }

    /** create a new block entity
     * 
     * @param position
     *            the initial position to create it at
     * @param bt
     *            the block type of the entity
     * @param velocity
     *            the initial velocity of the created entity
     * @return the new block entity */
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

    /** create a new particle
     * 
     * @param position
     *            the initial position of the particle
     * @param pt
     *            the type of particle
     * @param velocity
     *            the initial velocity of the particle
     * @return the new particle */
    public static Entity NewParticle(Vector position,
                                     ParticleType pt,
                                     Vector velocity)
    {
        Entity retval = new Entity(new Vector(position), EntityType.Particle);
        retval.data.velocity = new Vector(velocity);
        retval.data.particletype = pt;
        retval.data.phi = World.fRand(-(float)Math.PI / 2, (float)Math.PI / 2);
        retval.data.theta = World.fRand(0.0f, 2 * (float)Math.PI);
        retval.data.frame = 0;
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
        case FireAnim:
        case RedstoneFireAnim:
            retval.data.existduration = 1.5f;
            retval.data.phi = 0.0f;
            break;
        case SmokeAnim:
            retval.data.existduration = SimulationAnimationFrameCount
                    / SmokeSimulationAnimationFrameRate;
            retval.data.phi = 0.0f;
            break;
        }
        return retval;
    }

    /** create a new falling block
     * 
     * @param position
     *            the initial position of the falling block
     * @param bt
     *            the type of block
     * @return the new falling block */
    public static Entity NewFallingBlock(Vector position, BlockType bt)
    {
        Entity retval = new Entity(new Vector(position),
                                   EntityType.FallingBlock);
        retval.data.blocktype = bt;
        retval.data.velocity = new Vector(0);
        return retval;
    }

    /** creates a new primed TNT entity
     * 
     * @param position
     *            the new entity's position
     * @param timeFactor
     *            the new amount of time scaled to 0-1
     * @return the new primed TNT entity */
    public static Entity NewPrimedTNT(Vector position, double timeFactor)
    {
        Entity retval = new Entity(new Vector(position), EntityType.PrimedTNT);
        retval.data.velocity = new Vector(0);
        retval.data.existduration = timeFactor * 2.5f + 1.5f;
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
            case FireAnim:
            case RedstoneFireAnim:
            case SmokeAnim:
                this.data.existduration = i.readDouble();
                if(Double.isInfinite(this.data.existduration)
                        || Double.isNaN(this.data.existduration)
                        || this.data.existduration < 0
                        || this.data.existduration > 1e5)
                    throw new IOException("exist duration out of range");
                this.data.frame = i.readFloat();
                if(Double.isInfinite(this.data.frame)
                        || Double.isNaN(this.data.frame)
                        || this.data.frame < -1 - SimulationAnimationFrameCount
                        || this.data.existduration > 1 + SimulationAnimationFrameCount)
                    throw new IOException("frame out of range");
                break;
            }
            return;
        }
        case PrimedTNT:
        {
            this.data.existduration = i.readDouble();
            if(Double.isInfinite(this.data.existduration)
                    || Double.isNaN(this.data.existduration)
                    || this.data.existduration < 0
                    || this.data.existduration > 100)
                throw new IOException("exist duration out of range");
            this.data.velocity = Vector.read(i);
            return;
        }
        }
    }

    /** read from a <code>DataInput</code>
     * 
     * @param i
     *            <code>DataInput</code> to read from
     * @return the read <code>Entity</code>
     * @throws IOException
     *             the exception thrown */
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

    /** write to a <code>DataOutput</code>
     * 
     * @param o
     *            <code>OutputStream</code> to write to
     * @throws IOException
     *             the exception thrown */
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
            case FireAnim:
            case RedstoneFireAnim:
            case SmokeAnim:
                o.writeDouble(this.data.existduration);
                o.writeFloat(this.data.frame);
                break;
            }
            return;
        }
        case PrimedTNT:
        {
            o.writeDouble(this.data.existduration);
            this.data.velocity.write(o);
            return;
        }
        }
    }
}
