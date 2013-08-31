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

import static org.voxels.PlayerList.players;
import static org.voxels.World.GravityAcceleration;
import static org.voxels.World.world;

import java.io.*;

import org.voxels.TextureAtlas.TextureHandle;
import org.voxels.World.BlockHitDescriptor;

//TODO finish adding hopper moving to/from minecart with container
/** @author jacob */
public class Entity implements GameObject
{
    private Entity next = null; // for free entity list
    private Vector position = null;
    private EntityType type;
    private static Entity freeEntityListHead = null;
    private static final Object freeEntityListSyncObject = new Object();

    public static Entity allocate()
    {
        synchronized(freeEntityListSyncObject)
        {
            Entity retval = freeEntityListHead;
            if(retval == null)
                return new Entity();
            freeEntityListHead = retval.next;
            retval.type = EntityType.Nothing;
            retval.next = null;
            retval.position = null;
            return retval;
        }
    }

    public void free()
    {
        if(this.position != null)
        {
            this.position.free();
            this.position = null;
        }
        if(this.data != null)
        {
            this.data.free();
            this.data = null;
        }
        synchronized(freeEntityListSyncObject)
        {
            this.next = freeEntityListHead;
            freeEntityListHead = this;
        }
    }

    private static TextureHandle imgFire = TextureAtlas.addImage(new Image("particlefire.png"));
    private static TextureHandle imgRedstoneFire = TextureAtlas.addImage(new Image("particleredstonefire.png"));
    private static TextureHandle imgSmoke = TextureAtlas.addImage(new Image("particlesmoke.png"));
    private static final int SimulationAnimationFrameCount = 128;
    private static final float SimulationAnimationFrameRate = 30.0f;
    private static final float SmokeSimulationAnimationFrameRate = 15.0f;
    private static TextureHandle[] imgSmokeAnimation = null;
    private static TextureHandle[] imgFireAnimation = null;
    private static TextureHandle[] imgRedstoneFireAnimation = null;
    private static TextureHandle imgExplosion = TextureAtlas.addImage(new Image("explosion.png"));
    private static final int explosionFrameCount = 16;
    private static final float explosionFrameRate = 20f;

    private static TextureHandle[] genAnimation(final FireSimulation sim,
                                                final int stepCount)
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
        final boolean runSmokeAnimation = true, runFireAnimation = false, runRedstoneFireAnimation = false;
        int count = (runSmokeAnimation ? 1 : 0) + (runFireAnimation ? 1 : 0)
                + (runRedstoneFireAnimation ? 1 : 0);
        int index = 0;
        if(runSmokeAnimation)
        {
            Main.pushProgress((float)(index++) / count, 1.0f / count);
            imgSmokeAnimation = genAnimation(new SmokeSimulation(), 1);
            Main.popProgress();
        }
        if(runFireAnimation)
        {
            Main.pushProgress((float)(index++) / count, 1.0f / count);
            imgFireAnimation = genAnimation(new FireSimulation(), 2);
            Main.popProgress();
        }
        if(runRedstoneFireAnimation)
        {
            Main.pushProgress((float)(index++) / count, 1.0f / count);
            imgRedstoneFireAnimation = genAnimation(new RedstoneFireSimulation(),
                                                    2);
            Main.popProgress();
        }
    }

    private static class Data
    {
        private Data()
        {
        }

        public Block block;
        public float theta, phi;
        public Vector velocity;
        public double existduration;
        public boolean nearperson;
        public ParticleType particletype;
        public float frame;
        public float momentum;
        private Data next = null;
        private static final Object freePoolSyncObject = new Object();
        private static Data freePoolHead = null;
        public String ridingPlayerName = null;
        public Vector cornerVelocity = null;
        public boolean wasOverOnActivatorRail = false;

        public static Data allocate()
        {
            synchronized(freePoolSyncObject)
            {
                if(freePoolHead == null)
                    return new Data();
                Data retval = freePoolHead;
                freePoolHead = retval.next;
                retval.next = null;
                return retval;
            }
        }

        public void free()
        {
            if(this.block != null)
                this.block.free();
            this.block = null;
            if(this.velocity != null)
            {
                this.velocity.free();
                this.velocity = null;
            }
            if(this.cornerVelocity != null)
            {
                this.cornerVelocity.free();
                this.cornerVelocity = null;
            }
            synchronized(freePoolSyncObject)
            {
                this.next = freePoolHead;
                freePoolHead = this;
            }
        }
    }

    Data data;

    /** create an empty entity */
    private Entity()
    {
        this.type = EntityType.Nothing;
        this.data = null;
    }

    private static Entity
        allocate(final Vector position, final EntityType type)
    {
        Entity retval = allocate();
        retval.position = Vector.allocate(position);
        retval.type = type;
        retval.data = Data.allocate();
        return retval;
    }

    /** create a copy of an entity
     * 
     * @param rt
     *            entity to create a copy of
     * @return the new copy */
    public static Entity allocate(final Entity rt)
    {
        Entity retval = allocate();
        retval.type = rt.type;
        retval.position = Vector.allocate(rt.position);
        retval.data = Data.allocate();
        switch(retval.type)
        {
        case Last:
            break;
        case Nothing:
            break;
        case Block:
        case ThrownBlock:
            retval.data.block = rt.data.block.dup();
            retval.data.theta = rt.data.theta;
            retval.data.phi = rt.data.phi;
            retval.data.velocity = Vector.allocate(rt.data.velocity);
            retval.data.existduration = rt.data.existduration;
            retval.data.nearperson = rt.data.nearperson;
            break;
        case Particle:
            retval.data.velocity = Vector.allocate(rt.data.velocity);
            retval.data.particletype = rt.data.particletype;
            retval.data.theta = rt.data.theta;
            retval.data.phi = rt.data.phi;
            retval.data.existduration = rt.data.existduration;
            retval.data.frame = rt.data.frame;
            break;
        case FallingBlock:
            retval.data.block = rt.data.block.dup();
            retval.data.velocity = Vector.allocate(rt.data.velocity);
            break;
        case PrimedTNT:
            retval.data.velocity = Vector.allocate(rt.data.velocity);
            retval.data.existduration = rt.data.existduration;
            break;
        case PlaceBlockIfReplaceable:
            retval.data.block = rt.data.block.dup();
            break;
        case RemoveBlockIfEqual:
            retval.data.block = rt.data.block.dup();
            break;
        case TransferItem:
        case ApplyBoneMealOrPutBackInContainer:
            retval.data.velocity = Vector.allocate(rt.data.velocity);
            break;
        case MineCart:
            retval.data.velocity = Vector.allocate(rt.data.velocity);
            retval.data.block = Block.allocate(rt.data.block);
            retval.data.theta = rt.data.theta;
            retval.data.phi = rt.data.phi;
            retval.data.existduration = rt.data.existduration;
            retval.data.momentum = rt.data.momentum;
            retval.data.ridingPlayerName = rt.data.ridingPlayerName;
            retval.data.cornerVelocity = Vector.allocate(rt.data.cornerVelocity);
            retval.data.wasOverOnActivatorRail = rt.data.wasOverOnActivatorRail;
            break;
        }
        return retval;
    }

    private void clear()
    {
        this.type = EntityType.Nothing;
        if(this.position != null)
        {
            this.position.free();
            this.position = null;
        }
        if(this.data != null)
        {
            this.data.free();
            this.data = null;
        }
    }

    private static final float minecartScale = 0.98f;
    private static final Matrix getMinecartDrawMatrix_t1 = Matrix.allocate();
    private static final Matrix getMinecartDrawMatrix_t2 = Matrix.allocate();

    private Matrix getMinecartDrawMatrix()
    {
        Matrix retval = Matrix.setToTranslate(getMinecartDrawMatrix_t1,
                                              -0.5f,
                                              -0.5f,
                                              -0.5f);
        retval = retval.concatAndSet(Matrix.setToScale(getMinecartDrawMatrix_t2,
                                                       minecartScale));
        retval = retval.concatAndSet(Matrix.setToRotateX(getMinecartDrawMatrix_t2,
                                                         -this.data.phi));
        retval = retval.concatAndSet(Matrix.setToRotateY(getMinecartDrawMatrix_t2,
                                                         this.data.theta));
        retval = retval.concatAndSet(Matrix.setToTranslate(getMinecartDrawMatrix_t2,
                                                           this.position));
        return retval;
    }

    private static Matrix draw_t1 = Matrix.allocate();
    private static Matrix draw_t2 = Matrix.allocate();
    private static final Matrix minecartDrawBlockMatrix = Matrix.setToScale(Matrix.allocate(),
                                                                            10 / 16f)
                                                                .concatAndSetAndFreeArg(Matrix.setToTranslate(Matrix.allocate(),
                                                                                                              3 / 16f,
                                                                                                              3 / 16f,
                                                                                                              3 / 16f))
                                                                .getImmutableAndFree();

    @Override
    public RenderingStream draw(final RenderingStream rs,
                                final Matrix worldToCamera)
    {
        switch(this.type)
        {
        case Last:
            break;
        case Nothing:
            break;
        case ThrownBlock:
        case Block:
        {
            if(this.data.block != null)
            {
                Matrix tform = Matrix.setToTranslate(draw_t1,
                                                     -0.5f,
                                                     -0.5f,
                                                     -0.5f)
                                     .concatAndSet(Matrix.setToRotateX(draw_t2,
                                                                       this.data.phi))
                                     .concatAndSet(Matrix.setToRotateY(draw_t2,
                                                                       this.data.theta))
                                     .concatAndSet(Matrix.setToScale(draw_t2,
                                                                     0.25f))
                                     .concatAndSet(Matrix.setToTranslate(draw_t2,
                                                                         this.position));
                rs.pushMatrixStack();
                rs.concatMatrix(worldToCamera);
                this.data.block.drawAsEntity(rs, tform);
                rs.popMatrixStack();
            }
            break;
        }
        case Particle:
        {
            TextureHandle img = null;
            boolean isAnim = false;
            float minu = 0, maxu = 1, minv = 0, maxv = 1;
            boolean isGlowing = false;
            float scale = 1;
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
                if(imgFireAnimation == null)
                    return rs;
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
                if(imgRedstoneFireAnimation == null)
                    return rs;
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
                if(imgSmokeAnimation == null)
                    return rs;
                isAnim = true;
                int frame = (int)Math.floor(this.data.frame)
                        % SimulationAnimationFrameCount;
                if(frame < 0)
                    frame += SimulationAnimationFrameCount;
                img = imgSmokeAnimation[frame];
                break;
            }
            case Explosion:
            {
                int frame = (int)Math.floor(this.data.frame)
                        % explosionFrameCount;
                if(frame < 0)
                    frame += explosionFrameCount;
                img = imgExplosion;
                minu = (frame % 4) * 0.25f;
                maxu = minu + 0.25f;
                minv = (3 - frame / 4) * 0.25f;
                maxv = minv + 0.25f;
                scale = 10;
                break;
            }
            }
            if(img != null)
            {
                Matrix tform = Matrix.setToTranslate(draw_t1,
                                                     -0.5f,
                                                     isAnim ? 0.0f : -0.5f,
                                                     -0.5f);
                tform = tform.concatAndSet(Matrix.setToRotateX(draw_t2,
                                                               this.data.phi));
                tform = tform.concatAndSet(Matrix.setToRotateY(draw_t2,
                                                               this.data.theta));
                if(!isAnim)
                    tform = tform.concatAndSet(Matrix.setToScale(draw_t2,
                                                                 0.125f * scale));
                tform = tform.concatAndSet(Matrix.setToTranslate(draw_t2,
                                                                 this.position));
                rs.pushMatrixStack();
                rs.concatMatrix(worldToCamera);
                Block.drawImgAsEntity(rs,
                                      tform,
                                      img,
                                      isGlowing,
                                      minu,
                                      maxu,
                                      minv,
                                      maxv,
                                      1,
                                      1,
                                      1);
                rs.popMatrixStack();
            }
            break;
        }
        case FallingBlock:
        {
            if(this.data.block != null)
            {
                rs.pushMatrixStack();
                rs.concatMatrix(worldToCamera);
                this.data.block.drawAsEntity(rs,
                                             Matrix.setToTranslate(draw_t1,
                                                                   -0.5f,
                                                                   -0.5f,
                                                                   -0.5f)
                                                   .concatAndSet(Matrix.setToScale(draw_t2,
                                                                                   2 * tntItemSize))
                                                   .concatAndSet(Matrix.setToTranslate(draw_t2,
                                                                                       this.position))
                                                   .concatAndSet(Matrix.setToTranslate(draw_t2,
                                                                                       0.5f,
                                                                                       0.5f,
                                                                                       0.5f)));
                rs.popMatrixStack();
            }
            break;
        }
        case PrimedTNT:
        {
            Block b = Block.NewTNT();
            rs.pushMatrixStack();
            rs.concatMatrix(worldToCamera);
            b.drawAsEntity(rs,
                           Matrix.setToTranslate(draw_t1, -0.5f, -0.5f, -0.5f)
                                 .concatAndSet(Matrix.setToScale(draw_t2,
                                                                 2 * tntItemSize))
                                 .concatAndSet(Matrix.setToTranslate(draw_t2,
                                                                     this.position))
                                 .concatAndSet(Matrix.setToTranslate(draw_t2,
                                                                     0.5f,
                                                                     0.5f,
                                                                     0.5f)));
            b.free();
            rs.popMatrixStack();
            break;
        }
        case PlaceBlockIfReplaceable:
        case RemoveBlockIfEqual:
        case TransferItem:
        case ApplyBoneMealOrPutBackInContainer:
            return rs;
        case MineCart:
        {
            Block b = Block.NewMinecart(0, false);
            rs.pushMatrixStack();
            rs.concatMatrix(worldToCamera);
            b.drawAsEntity(rs, getMinecartDrawMatrix());
            if(this.data.block != null)
                this.data.block.drawAsEntity(rs,
                                             minecartDrawBlockMatrix.concat(draw_t1,
                                                                            getMinecartDrawMatrix()));
            rs.popMatrixStack();
            b.free();
            return rs;
        }
        }
        return rs;
    }

    private static final float TNTStrength = 4;

    private static boolean itemHits(final float size, final Vector pos)
    {
        Vector t = Vector.allocate();
        int x = (int)Math.floor(pos.getX());
        int y = (int)Math.floor(pos.getY());
        int z = (int)Math.floor(pos.getZ());
        int searchDist = (int)Math.floor(size) + 1;
        for(int dx = -searchDist; dx <= searchDist; dx++)
        {
            for(int dy = -searchDist; dy <= searchDist; dy++)
            {
                for(int dz = -searchDist; dz <= searchDist; dz++)
                {
                    Block b = world.getBlock(x + dx, y + dy, z + dz);
                    if(b == null)
                        b = bedrockBlock;
                    if(b.checkItemHit(size,
                                      Vector.sub(t, pos, x + dx, y + dy, z + dz)))
                    {
                        t.free();
                        return true;
                    }
                }
            }
        }
        t.free();
        return false;
    }

    private static float getNearestEmptySpotH(final float size,
                                              final Vector position,
                                              final Vector dir,
                                              final float maxDist,
                                              final float minDist)
    {
        if(maxDist - minDist < 1e-5)
            return maxDist;
        float avgDist = (maxDist + minDist) * 0.5f;
        Vector p = Vector.allocate(dir).mulAndSet(avgDist).addAndSet(position);
        if(!itemHits(size, p))
        {
            p.free();
            return getNearestEmptySpotH(size, position, dir, avgDist, minDist);
        }
        p.free();
        return getNearestEmptySpotH(size, position, dir, maxDist, avgDist);
    }

    /** @param size
     *            the item's size
     * @return a newly allocated Vector that is the nearest empty spot or null */
    private static Vector getNearestEmptySpot(final float size,
                                              final Vector position)
    {
        Vector retval = Vector.allocate();
        final float distFactor = 0.01f;
        for(int dist = 0; dist <= 1000; dist += 1 + (dist >> 1))
        {
            for(int dx = -dist; dx <= dist; dx += 1 + (dist >> 1))
            {
                for(int dy = -dist; dy <= dist; dy += 1 + (dist >> 1))
                {
                    int min = Math.abs(dx) + Math.abs(dy) - dist, max = -min;
                    int step = Math.max(1, max - min);
                    for(int dz = min; dz <= max; dz += step)
                    {
                        retval.set(position);
                        retval.addAndSet(dx * distFactor, dy * distFactor, dz
                                * distFactor);
                        if(!itemHits(size, retval))
                        {
                            if(dx == 0 && dy == 0 && dz == 0)
                                return retval;
                            Vector dir = Vector.allocate(dx * distFactor, dy
                                    * distFactor, dz * distFactor);
                            float r = dir.abs();
                            dir.divAndSet(r);
                            retval.set(dir)
                                  .mulAndSet(getNearestEmptySpotH(size,
                                                                  position,
                                                                  dir,
                                                                  r,
                                                                  Math.max(0,
                                                                           r
                                                                                   - 2.0f
                                                                                   * distFactor
                                                                                   * (1 + dist >> 1))))
                                  .addAndSet(position);
                            dir.free();
                            return retval;
                        }
                    }
                }
            }
        }
        retval.free();
        return null;
    }

    private static final float blockItemSize = 0.125f * (float)Math.sqrt(2);
    private static final float tntItemSize = 0.49f;
    private static Vector move_t1 = Vector.allocate();
    private static Vector move_t2 = Vector.allocate();
    private static Vector move_t3 = Vector.allocate();
    private static final Block bedrockBlock = Block.NewBedrock();
    private static final double MINECART_DELETE_TIME = 0.3f;

    private static final class BlockWithPos
    {
        public int bx, by, bz;
        public Block b;

        public BlockWithPos()
        {
        }
    }

    private static final BlockWithPos getRailFromPos_retval = new BlockWithPos();

    private static BlockWithPos getRailFromPos(final Vector pos)
    {
        int bx = (int)Math.floor(pos.getX());
        int by = (int)Math.floor(pos.getY() + 0.5f);
        int bz = (int)Math.floor(pos.getZ());
        Block b = world.getBlockEval(bx, by, bz);
        if(b == null || !b.isRail())
        {
            by--;
            b = world.getBlockEval(bx, by, bz);
        }
        if(b == null || !b.isRail())
        {
            by--;
            b = world.getBlockEval(bx, by, bz);
        }
        if(b == null || !b.isRail())
            return null;
        BlockWithPos retval = getRailFromPos_retval;
        retval.bx = bx;
        retval.by = by;
        retval.bz = bz;
        retval.b = b;
        return retval;
    }

    private static boolean minecartIsPosOnTrack(final Vector pos)
    {
        BlockWithPos bwp = getRailFromPos(pos);
        if(bwp == null)
            return false;
        Block b = bwp.b;
        int bx = bwp.bx, by = bwp.by, bz = bwp.bz;
        float railHeightAtPos = 0;
        if(b.railGetOrientation() >= 2 && b.railGetOrientation() <= 5)
        {
            float x = pos.getX() - bx;
            float z = pos.getZ() - bz;
            x -= 0.5f;
            z -= 0.5f;
            for(int i = 2; i < b.railGetOrientation(); i++)
            {
                float t = x;
                x = z;
                z = -t;
            }
            railHeightAtPos = 0.5f - x;
        }
        return pos.getY() - 0.5f <= by + railHeightAtPos + 0.05f;
    }

    private void minecartOnSetPosition()
    {
        if(this.data.ridingPlayerName == null)
            return;
        Player p = players.getPlayer(this.data.ridingPlayerName);
        if(p == null)
            return;
        if(!p.isRiding)
        {
            this.data.ridingPlayerName = null;
            return;
        }
        Vector pos = Vector.allocate(this.position).addAndSet(0, 1.0f, 0);
        p.setPosition(pos);
        pos.free();
    }

    private void minecartSetVelocity()
    {
        BlockWithPos bwp = getRailFromPos(this.position);
        this.data.cornerVelocity.set(Vector.ZERO);
        this.data.velocity.setToSphericalCoordinates(1,
                                                     this.data.theta,
                                                     this.data.phi)
                          .mulAndSet(Math.max(-8,
                                              Math.min(8, this.data.momentum)));
        if(bwp != null && bwp.b.railGetOrientation() >= 6) // is corner
        {
            this.data.velocity.mulAndSet((float)Math.sqrt(2));
            Vector t = Vector.allocate(this.data.velocity);
            t.setY(0);
            int velocityOrientation = Block.getOrientationFromVector(t);
            t.free();
            float velocityRotateAngle = (float)Math.PI / 4;
            int railOrientation = bwp.b.getOrientation() - 6;
            if(velocityOrientation == railOrientation
                    || velocityOrientation == (railOrientation + 3) % 4)
            {
                velocityRotateAngle = -velocityRotateAngle;
            }
            Matrix tform = Matrix.allocate();
            Matrix.setToRotateY(tform, -velocityRotateAngle)
                  .apply(this.data.cornerVelocity, this.data.velocity);
            Matrix.setToRotateY(tform, velocityRotateAngle)
                  .apply(this.data.velocity, this.data.velocity);
            tform.free();
            return;
        }
    }

    private static final Vector[] railOrigin = new Vector[]
    {
        Vector.allocate(0.5f, 0.5f, 0).getImmutableAndFree(),
        Vector.allocate(0, 0.5f, 0.5f).getImmutableAndFree(),
        Vector.allocate(1, 0.5f, 0.5f).getImmutableAndFree(),
        Vector.allocate(0.5f, 0.5f, 1).getImmutableAndFree(),
        Vector.allocate(0, 0.5f, 0.5f).getImmutableAndFree(),
        Vector.allocate(0.5f, 0.5f, 0).getImmutableAndFree(),
        Vector.allocate(0, 0.5f, 0.5f).getImmutableAndFree(),
        Vector.allocate(0.5f, 0.5f, 0).getImmutableAndFree(),
        Vector.allocate(1, 0.5f, 0.5f).getImmutableAndFree(),
        Vector.allocate(0.5f, 0.5f, 1).getImmutableAndFree(),
    };
    private static final Vector[] railDir = new Vector[]
    {
        Vector.allocate(0, 0, 1).normalizeAndSet().getImmutableAndFree(),
        Vector.allocate(1, 0, 0).normalizeAndSet().getImmutableAndFree(),
        Vector.allocate(-1, 1, 0).normalizeAndSet().getImmutableAndFree(),
        Vector.allocate(0, 1, -1).normalizeAndSet().getImmutableAndFree(),
        Vector.allocate(1, 1, 0).normalizeAndSet().getImmutableAndFree(),
        Vector.allocate(0, 1, 1).normalizeAndSet().getImmutableAndFree(),
        Vector.allocate(1, 0, -1).normalizeAndSet().getImmutableAndFree(),
        Vector.allocate(1, 0, 1).normalizeAndSet().getImmutableAndFree(),
        Vector.allocate(-1, 0, 1).normalizeAndSet().getImmutableAndFree(),
        Vector.allocate(-1, 0, -1).normalizeAndSet().getImmutableAndFree(),
    };
    private static final Vector minecartAttachToRail_t1 = Vector.allocate();

    private float minecartAttachToRail()
    {
        BlockWithPos b = getRailFromPos(this.position);
        if(b == null)
            return -this.data.momentum;
        if(b.b.getType() == BlockType.BTDetectorRail)
        {
            b.b.detectorRailActivate();
            world.setBlock(b.bx, b.by, b.bz, b.b);
        }
        int railOrientation = b.b.railGetOrientation();
        float t = minecartAttachToRail_t1.set(this.position)
                                         .subAndSet(b.bx, b.by, b.bz)
                                         .subAndSet(railOrigin[railOrientation])
                                         .dot(railDir[railOrientation]);
        this.position.set(railDir[railOrientation])
                     .mulAndSet(t)
                     .addAndSet(b.bx, b.by, b.bz)
                     .addAndSet(railOrigin[railOrientation]);
        minecartOnSetPosition();
        Vector facingDir = minecartAttachToRail_t1.setToSphericalCoordinates(1,
                                                                             this.data.theta,
                                                                             this.data.phi);
        facingDir.setY(facingDir.getY() / 2);
        t = facingDir.dot(railDir[railOrientation]);
        facingDir = minecartAttachToRail_t1.set(railDir[railOrientation]);
        if(t < 0)
            facingDir.negAndSet();
        this.data.theta = facingDir.getTheta();
        this.data.phi = facingDir.getPhi();
        minecartSetVelocity();
        float returnedAcceleration = 0;
        if(b.b.getType() == BlockType.BTActivatorRail)
        {
            this.data.wasOverOnActivatorRail = b.b.railIsPowered();
        }
        if(b.b.getType() == BlockType.BTPoweredRail)
        {
            if(b.b.railIsPowered())
            {
                returnedAcceleration = Math.signum(this.data.momentum) * 5;
            }
            else
            {
                this.data.momentum = 0;
                return 0;
            }
        }
        return facingDir.getY() > 0 ? returnedAcceleration - 1
                : (facingDir.getY() == 0 ? returnedAcceleration
                        : returnedAcceleration + 1);
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
        case ThrownBlock:
        case Block:
        {
            if(this.data.nearperson)
            {
                this.position = this.position.addAndSet(Vector.mul(move_t1,
                                                                   this.data.velocity,
                                                                   (float)Main.getFrameDuration()));
                this.data.nearperson = false;
                this.data.theta = (this.data.theta + (float)Main.getFrameDuration()
                        * 0.5f * (float)Math.PI)
                        % (float)(2 * Math.PI);
                break;
            }
            this.data.existduration += Main.getFrameDuration();
            if(this.data.existduration > 0.6f
                    && this.type == EntityType.ThrownBlock)
                this.type = EntityType.Block;
            if(this.data.existduration > 60.0 * 6) // 6 min
            {
                clear();
                return;
            }
            if(this.position.getY() < -World.Depth)
            {
                clear();
                return;
            }
            this.data.velocity = this.data.velocity.addAndSet(Vector.set(move_t1,
                                                                         0.0f,
                                                                         -GravityAcceleration,
                                                                         0.0f)
                                                                    .mulAndSet((float)Main.getFrameDuration()));
            Vector deltaPos = Vector.mul(move_t1,
                                         this.data.velocity,
                                         (float)Main.getFrameDuration());
            Vector newPos = move_t2.set(this.position);
            Vector lastPos = move_t3.set(newPos);
            final int count = (int)Math.floor(deltaPos.abs() * 100) + 1;
            deltaPos.divAndSet(count);
            for(int i = 0; i < count; i++)
            {
                if(itemHits(blockItemSize, newPos))
                {
                    newPos.set(lastPos);
                    break;
                }
                lastPos.set(newPos);
                newPos.addAndSet(deltaPos);
            }
            Vector adjustedNewPos = getNearestEmptySpot(blockItemSize, newPos);
            if(adjustedNewPos != null)
            {
                if(!newPos.equals(adjustedNewPos))
                    this.data.velocity.set(Vector.ZERO);
                this.position.set(adjustedNewPos);
                adjustedNewPos.free();
            }
            else
            {
                clear();
                return;
            }
            Block b = world.getBlockEval((int)Math.floor(this.position.getX()),
                                         (int)Math.floor(this.position.getY()),
                                         (int)Math.floor(this.position.getZ()));
            if(b != null)
            {
                if(b.getType() == BlockType.BTWoodPressurePlate
                        && b.pressurePlateIsItemPressing(blockItemSize,
                                                         Vector.sub(move_t1,
                                                                    this.position,
                                                                    (float)Math.floor(this.position.getX()),
                                                                    (float)Math.floor(this.position.getY()),
                                                                    (float)Math.floor(this.position.getZ()))))
                {
                    b.pressurePlatePress();
                    world.setBlock((int)Math.floor(this.position.getX()),
                                   (int)Math.floor(this.position.getY()),
                                   (int)Math.floor(this.position.getZ()),
                                   b);
                }
            }
            b = world.getBlockEval((int)Math.floor(this.position.getX()),
                                   (int)Math.floor(this.position.getY()) - 1,
                                   (int)Math.floor(this.position.getZ()));
            if(b != null)
            {
                if(b.getType() == BlockType.BTHopper && b.hopperIsActive())
                {
                    if(b.addBlockToContainer(this.data.block, 5))
                    {
                        world.setBlock((int)Math.floor(this.position.getX()),
                                       (int)Math.floor(this.position.getY()) - 1,
                                       (int)Math.floor(this.position.getZ()),
                                       b);
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
                this.data.velocity = this.data.velocity.addAndSet(Vector.set(move_t1,
                                                                             0,
                                                                             (float)Main.getFrameDuration(),
                                                                             0))
                                                       .mulAndSet((float)Math.pow(0.3f,
                                                                                  (float)Main.getFrameDuration()));
                break;
            case FireAnim:
            case RedstoneFireAnim:
                this.data.velocity.set(Vector.ZERO);
                this.data.frame += SimulationAnimationFrameRate
                        * (float)Main.getFrameDuration();
                this.data.frame %= SimulationAnimationFrameCount;
                break;
            case SmokeAnim:
                this.data.velocity.set(Vector.ZERO);
                this.data.frame += SmokeSimulationAnimationFrameRate
                        * (float)Main.getFrameDuration();
                this.data.frame %= SimulationAnimationFrameCount;
                break;
            case Explosion:
                this.data.velocity.set(Vector.ZERO);
                this.data.frame += explosionFrameRate
                        * (float)Main.getFrameDuration();
                if(Math.floor(this.data.frame) >= explosionFrameCount)
                {
                    clear();
                    return;
                }
                break;
            }
            this.position = this.position.addAndSet(this.data.velocity.mulAndSet((float)Main.getFrameDuration()));
            break;
        }
        case FallingBlock:
        {
            int x = Math.round(this.position.getX());
            int y = Math.round(this.position.getY());
            int z = Math.round(this.position.getZ());
            Block b = world.getBlock(x, y, z);
            if(b != null && !b.isReplaceable()
                    && b.getType() != BlockType.BTEmpty)
            {
                b = this.data.block;
                if(b == null)
                    b = Block.NewEmpty();
                else
                    b = b.dup();
                world.insertEntity(NewBlock(move_t1.set(this.position)
                                                   .addAndSet(0.5f, 0.5f, 0.5f),
                                            b,
                                            World.vRand(move_t2, 0.1f)));
                clear();
                return;
            }
            b = world.getBlock(x, y - 1, z);
            if(b == null || b.isSupporting())
            {
                b = this.data.block;
                if(b == null)
                    b = Block.NewEmpty();
                else
                    b = b.dup();
                Block prevBlock = world.getBlock(x, y, z);
                world.setBlock(x, y, z, b);
                if(prevBlock != null)
                    prevBlock.free();
                clear();
                return;
            }
            Vector deltapos = Vector.mul(move_t1,
                                         this.data.velocity,
                                         (float)Main.getFrameDuration());
            if(deltapos.abs_squared() > 1)
                deltapos = deltapos.normalizeAndSet();
            this.position = this.position.addAndSet(deltapos);
            this.data.velocity = this.data.velocity.addAndSet(Vector.set(move_t2,
                                                                         0,
                                                                         -GravityAcceleration
                                                                                 * (float)Main.getFrameDuration(),
                                                                         0));
            break;
        }
        case PrimedTNT:
        {
            int x = Math.round(this.position.getX());
            int y = Math.round(this.position.getY());
            int z = Math.round(this.position.getZ());
            {
                final float ParticlesPerSecond = 5;
                int startcount = (int)Math.floor(this.data.existduration
                        * ParticlesPerSecond);
                this.data.existduration -= Main.getFrameDuration();
                int endcount = (int)Math.floor(this.data.existduration
                        * ParticlesPerSecond);
                int count = startcount - endcount;
                for(int i = 0; i < count; i++)
                {
                    world.insertEntity(NewParticle(Vector.add(move_t1,
                                                              this.position,
                                                              Vector.set(move_t2,
                                                                         0.5f,
                                                                         1.0f,
                                                                         0.5f)),
                                                   ParticleType.SmokeAnim,
                                                   Vector.ZERO));
                }
            }
            if(this.data.existduration <= 0)
            {
                world.addExplosion(x, y, z, TNTStrength);
                clear();
                return;
            }
            this.data.velocity = this.data.velocity.addAndSet(Vector.set(move_t1,
                                                                         0,
                                                                         -GravityAcceleration
                                                                                 * (float)Main.getFrameDuration(),
                                                                         0));
            Vector deltaPos = Vector.mul(move_t1,
                                         this.data.velocity,
                                         (float)Main.getFrameDuration());
            Vector newPos = move_t2.set(this.position).addAndSet(0.5f,
                                                                 0.5f,
                                                                 0.5f);
            Vector lastPos = move_t3.set(newPos);
            final int count = (int)Math.floor(deltaPos.abs() * 100) + 1;
            deltaPos.divAndSet(count);
            for(int i = 0; i < count; i++)
            {
                if(itemHits(tntItemSize, newPos))
                {
                    newPos.set(lastPos);
                    break;
                }
                lastPos.set(newPos);
                newPos.addAndSet(deltaPos);
            }
            Vector adjustedNewPos = getNearestEmptySpot(tntItemSize, newPos);
            if(adjustedNewPos != null
                    && Vector.sub(move_t1, adjustedNewPos, newPos).abs() > 0.8f)
            {
                adjustedNewPos.free();
                adjustedNewPos = null;
            }
            if(adjustedNewPos == null)
            {
                world.addExplosion(x, y, z, TNTStrength);
                clear();
                return;
            }
            if(!adjustedNewPos.equals(newPos))
                this.data.velocity.set(Vector.ZERO);
            this.position.set(adjustedNewPos.subAndSet(0.5f, 0.5f, 0.5f));
            adjustedNewPos.free();
            break;
        }
        case PlaceBlockIfReplaceable:
        {
            int x = Math.round(this.position.getX());
            int y = Math.round(this.position.getY());
            int z = Math.round(this.position.getZ());
            Block b = world.getBlockEval(x, y, z);
            if(b != null && b.isReplaceable())
            {
                world.setBlock(x, y, z, this.data.block.dup());
                b.free();
            }
            clear();
            return;
        }
        case RemoveBlockIfEqual:
        {
            int x = Math.round(this.position.getX());
            int y = Math.round(this.position.getY());
            int z = Math.round(this.position.getZ());
            Block b = world.getBlockEval(x, y, z);
            if(b != null && b.equals(this.data.block))
            {
                world.setBlock(x, y, z, Block.NewEmpty());
                b.free();
            }
            clear();
            return;
        }
        case TransferItem:
        {
            int srcX = Math.round(this.position.getX());
            int srcY = Math.round(this.position.getY());
            int srcZ = Math.round(this.position.getZ());
            int destX = Math.round(this.data.velocity.getX());
            int destY = Math.round(this.data.velocity.getY());
            int destZ = Math.round(this.data.velocity.getZ());
            Block src = world.getBlockEval(srcX, srcY, srcZ);
            if(src != null)
                src.runTransferItem(srcX, srcY, srcZ, destX, destY, destZ);
            clear();
            return;
        }
        case ApplyBoneMealOrPutBackInContainer:
        {
            int x = Math.round(this.position.getX());
            int y = Math.round(this.position.getY());
            int z = Math.round(this.position.getZ());
            int cx = Math.round(this.data.velocity.getX());
            int cy = Math.round(this.data.velocity.getY());
            int cz = Math.round(this.data.velocity.getZ());
            Block b = world.getBlockEval(x, y, z);
            boolean isBoneMealUsed = false;
            if(b != null)
            {
                isBoneMealUsed = b.onUseBoneMeal(x, y, z);
            }
            if(!isBoneMealUsed)
            {
                b = world.getBlockEval(x, y - 1, z);
                if(b != null)
                {
                    isBoneMealUsed = b.onUseBoneMeal(x, y - 1, z);
                }
            }
            if(!isBoneMealUsed)
            {
                Block container = world.getBlockEval(cx, cy, cz);
                if(container != null)
                {
                    Vector orientationVector = Vector.allocate(x, y, z)
                                                     .subAndSet(cx, cy, cz);
                    final int orientation = Block.getOrientationFromVector(orientationVector);
                    orientationVector.free();
                    if(container.addBlockToContainer(Block.NewBoneMeal(),
                                                     orientation))
                    {
                        isBoneMealUsed = true;
                    }
                }
            }
            if(!isBoneMealUsed)
            {
                Vector t1 = Vector.allocate();
                Vector t2 = Vector.allocate();
                Vector dir = Vector.allocate(x, y, z).subAndSet(cx, cy, cz);
                world.insertEntity(Entity.NewBlock(t1.set(dir)
                                                     .mulAndSet(-(0.5f - 0.25f + 0.05f))
                                                     .addAndSet(x + 0.5f,
                                                                x + 0.5f,
                                                                x + 0.5f),
                                                   Block.NewBoneMeal(),
                                                   World.vRand(t2, 0.2f)
                                                        .addAndSet(dir)
                                                        .mulAndSet(5f)));
                t1.free();
                t2.free();
                dir.free();
            }
            clear();
            return;
        }
        case MineCart:
        {
            if(this.data.existduration <= 0)
            {
                minecartDropAllItems();
                clear();
                return;
            }
            this.data.existduration += 0.33 * Main.getFrameDuration();
            if(this.data.existduration > MINECART_DELETE_TIME)
                this.data.existduration = MINECART_DELETE_TIME;
            this.data.velocity.addAndSet(0, -World.GravityAcceleration
                    * (float)Main.getFrameDuration(), 0);
            if(minecartIsPosOnTrack(this.position))
            {
                minecartAttachToRail();
                int count = Math.max(1,
                                     (int)Math.floor(1000
                                             * Math.abs(this.data.momentum)
                                             * Main.getFrameDuration()));
                final float invCount = 1.0f / count;
                for(int i = 0; i < count; i++)
                {
                    this.position.addAndSet(move_t1.set(this.data.velocity)
                                                   .addAndSet(this.data.cornerVelocity)
                                                   .mulAndSet((float)Main.getFrameDuration()
                                                           * invCount));
                    minecartOnSetPosition();
                    this.data.momentum = Math.max(-20,
                                                  Math.min(20,
                                                           this.data.momentum
                                                                   + minecartAttachToRail()
                                                                   * (float)Main.getFrameDuration()
                                                                   * invCount));
                }
                if(Main.DEBUG)
                    Main.addToFrameText("Momentum : " + this.data.momentum
                            + "\n");
            }
            else
            {
                this.data.momentum *= (float)Math.pow(0.9,
                                                      Main.getFrameDuration());
                this.data.phi = 0;
                this.data.cornerVelocity.set(Vector.ZERO);
                Vector deltaPos = Vector.mul(move_t1,
                                             this.data.velocity,
                                             (float)Main.getFrameDuration());
                Vector newPos = move_t2.set(this.position);
                Vector lastPos = move_t3.set(newPos);
                final int count = (int)Math.floor(deltaPos.abs() * 100) + 1;
                deltaPos.divAndSet(count);
                for(int i = 0; i < count; i++)
                {
                    if(itemHits(minecartScale / 2, newPos))
                    {
                        this.data.velocity.set(Vector.ZERO);
                        newPos.set(lastPos);
                        break;
                    }
                    lastPos.set(newPos);
                    newPos.addAndSet(deltaPos);
                }
                Vector adjustedNewPos = getNearestEmptySpot(minecartScale / 2,
                                                            newPos);
                if(adjustedNewPos != null)
                {
                    if(!newPos.equals(adjustedNewPos))
                        this.data.velocity.set(Vector.ZERO);
                    this.position.set(adjustedNewPos);
                    minecartOnSetPosition();
                    adjustedNewPos.free();
                }
                else
                {
                    this.data.velocity.set(Vector.ZERO);
                    return;
                }
            }
            return;
        }
        }
    }

    private void minecartDropAllItems()
    {
        if(this.data.block != null)
        {
            Block block = this.data.block.dup();
            if(block.isContainer())
            {
                while(true)
                {
                    int removeDescriptor = block.makeRemoveBlockFromContainerDescriptor(-1);
                    Block b = block.removeBlockFromContainer(-1,
                                                             removeDescriptor);
                    if(b == null)
                        break;
                    world.insertEntity(NewBlock(this.position,
                                                b,
                                                World.vRand(move_t1, 0.1f)));
                }
            }
            world.insertEntity(NewBlock(this.position,
                                        block,
                                        World.vRand(move_t1, 0.1f)));
        }
        world.insertEntity(NewBlock(this.position,
                                    BlockType.BTMineCart.make(-1),
                                    World.vRand(move_t1, 0.1f)));
    }

    private static Vector checkHitPlayer_t1 = Vector.allocate();

    /** check for hitting player <code>p</code> and run the corresponding action
     * if <code>p</code> got hit
     * 
     * @param p
     *            player to check for */
    public void checkHitPlayer(final Player p)
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
            Vector disp = Vector.sub(checkHitPlayer_t1, ppos, this.position);
            if(disp.abs_squared() <= 0.3f * 0.3f
                    && p.giveBlock(this.data.block, false))
            {
                clear();
            }
            else if(disp.abs_squared() <= 3.0f * 3.0f)
            {
                float speed = this.data.velocity.abs();
                speed += 3 * Main.getFrameDuration()
                        * World.GravityAcceleration;
                if(speed > 15.0f)
                    speed = 15.0f;
                speed = Math.min(disp.abs() / (float)Main.getFrameDuration(),
                                 speed);
                this.data.velocity = Vector.sub(this.data.velocity,
                                                ppos,
                                                this.position)
                                           .normalizeAndSet()
                                           .mulAndSet(speed);
                this.data.nearperson = true;
            }
            break;
        }
        case PrimedTNT:
        {
            Vector ppos = p.getPosition();
            Vector disp = Vector.sub(checkHitPlayer_t1, ppos, this.position);
            if(disp.abs_squared() <= 15 * 15)
                Main.needFuseBurnAudio = true;
            break;
        }
        case Particle:
        case FallingBlock:
        case ThrownBlock:
        case PlaceBlockIfReplaceable:
        case RemoveBlockIfEqual:
        case TransferItem:
        case ApplyBoneMealOrPutBackInContainer:
            break;
        case MineCart:
        {
            // TODO finish
        }
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

    private Vector explode_t1 = Vector.allocate();
    private Vector explode_t2 = Vector.allocate();

    /** check for moving from an explosion
     * 
     * @param pos
     *            the explosion position
     * @param strength
     *            the explosion strength */
    public void explode(final Vector pos, final float strength)
    {
        final float explosionRadius = strength * 2.0f;
        switch(this.type)
        {
        case Last:
            break;
        case Nothing:
            break;
        case Block:
        case ThrownBlock:
        {
            if(this.data.nearperson)
            {
                return;
            }
            final float impact = 3
                    * (1.0f - Vector.sub(this.explode_t1, this.position, pos)
                                    .abs() / explosionRadius)
                    * getBoxExposure(Vector.sub(this.explode_t1,
                                                this.position,
                                                0.125f,
                                                0.125f,
                                                0.125f),
                                     Vector.add(this.explode_t2,
                                                this.position,
                                                0.125f,
                                                0.125f,
                                                0.125f),
                                     pos);
            if(impact <= 0)
                return;
            Vector dir = Vector.sub(this.explode_t1, this.position, pos)
                               .normalizeAndSet()
                               .mulAndSet(impact * GravityAcceleration);
            this.data.velocity.addAndSet(dir);
            break;
        }
        case Particle:
        {
            break;
        }
        case FallingBlock:
        {
            final float impact = 3
                    * (1.0f - Vector.sub(this.explode_t1, this.position, pos)
                                    .addAndSet(0.5f, 0.5f, 0.5f)
                                    .abs()
                            / explosionRadius)
                    * getBoxExposure(Vector.add(this.explode_t1,
                                                this.position,
                                                0.5f - tntItemSize,
                                                0.5f - tntItemSize,
                                                0.5f - tntItemSize),
                                     Vector.add(this.explode_t2,
                                                this.position,
                                                0.5f + tntItemSize,
                                                0.5f + tntItemSize,
                                                0.5f + tntItemSize),
                                     pos);
            if(impact <= 0)
                return;
            Vector dir = Vector.sub(this.explode_t1, this.position, pos)
                               .normalizeAndSet()
                               .mulAndSet(impact * GravityAcceleration);
            this.data.velocity.addAndSet(dir);
            break;
        }
        case PrimedTNT:
        {
            final float impact = 3
                    * (1.0f - Vector.sub(this.explode_t1, this.position, pos)
                                    .addAndSet(0.5f, 0.5f, 0.5f)
                                    .abs()
                            / explosionRadius)
                    * getBoxExposure(Vector.add(this.explode_t1,
                                                this.position,
                                                0.5f - tntItemSize,
                                                0.5f - tntItemSize,
                                                0.5f - tntItemSize),
                                     Vector.add(this.explode_t2,
                                                this.position,
                                                0.5f + tntItemSize,
                                                0.5f + tntItemSize,
                                                0.5f + tntItemSize),
                                     pos);
            if(impact <= 0)
                return;
            Vector dir = Vector.sub(this.explode_t1, this.position, pos)
                               .normalizeAndSet()
                               .mulAndSet(impact * GravityAcceleration);
            this.data.velocity.addAndSet(dir);
            break;
        }
        case PlaceBlockIfReplaceable:
        case RemoveBlockIfEqual:
        case TransferItem:
        case ApplyBoneMealOrPutBackInContainer:
        case MineCart:
            break;
        }
    }

    /** create a new block entity
     * 
     * @param position
     *            the initial position to create it at
     * @param b
     *            the block
     * @param velocity
     *            the initial velocity of the created entity
     * @return the new block entity */
    public static Entity NewBlock(final Vector position,
                                  final Block b,
                                  final Vector velocity)
    {
        Entity retval = allocate(position, EntityType.Block);
        retval.data.block = b.dup();
        retval.data.existduration = 0;
        retval.data.phi = 0;
        retval.data.theta = World.fRand(0.0f, 2 * (float)Math.PI);
        retval.data.velocity = Vector.allocate(velocity);
        retval.data.nearperson = false;
        return retval;
    }

    /** create a new thrown block entity
     * 
     * @param position
     *            the initial position to create it at
     * @param b
     *            the block
     * @param velocity
     *            the initial velocity of the created entity
     * @return the new thrown block entity */
    public static Entity NewThrownBlock(final Vector position,
                                        final Block b,
                                        final Vector velocity)
    {
        Entity retval = allocate(position, EntityType.ThrownBlock);
        retval.data.block = b.dup();
        retval.data.existduration = 0;
        retval.data.phi = 0;
        retval.data.theta = World.fRand(0.0f, 2 * (float)Math.PI);
        retval.data.velocity = Vector.allocate(velocity);
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
    public static Entity NewParticle(final Vector position,
                                     final ParticleType pt,
                                     final Vector velocity)
    {
        Entity retval = allocate(position, EntityType.Particle);
        retval.data.velocity = Vector.allocate(velocity);
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
        case Explosion:
            retval.data.phi = 0;
            retval.data.existduration = 5;
            break;
        }
        return retval;
    }

    /** create a new falling block
     * 
     * @param position
     *            the initial position of the falling block
     * @param b
     *            the block
     * @return the new falling block */
    public static Entity NewFallingBlock(final Vector position, final Block b)
    {
        Entity retval = allocate(position, EntityType.FallingBlock);
        retval.data.block = b.dup();
        retval.data.velocity = Vector.allocate(0);
        return retval;
    }

    /** creates a new primed TNT entity
     * 
     * @param position
     *            the new entity's position
     * @param timeFactor
     *            the new amount of time scaled to 0-1
     * @return the new primed TNT entity */
    public static Entity NewPrimedTNT(final Vector position,
                                      final double timeFactor)
    {
        Entity retval = allocate(position, EntityType.PrimedTNT);
        retval.data.velocity = Vector.allocate(0);
        retval.data.existduration = timeFactor * 2.5f + 1.5f;
        return retval;
    }

    /** create a new PlaceBlockIfReplaceable
     * 
     * @param position
     *            the position of the PlaceBlockIfReplaceable
     * @param b
     *            the block
     * @return the new PlaceBlockIfReplaceable */
    public static Entity NewPlaceBlockIfReplaceable(final Vector position,
                                                    final Block b)
    {
        Entity retval = allocate(position, EntityType.PlaceBlockIfReplaceable);
        retval.data.block = b.dup();
        return retval;
    }

    /** create a new RemoveBlockIfEqual
     * 
     * @param position
     *            the position of the RemoveBlockIfEqual
     * @param b
     *            the block
     * @return the new RemoveBlockIfEqual */
    public static Entity NewRemoveBlockIfEqual(final Vector position,
                                               final Block b)
    {
        Entity retval = allocate(position, EntityType.RemoveBlockIfEqual);
        retval.data.block = b.dup();
        return retval;
    }

    public static Entity NewTransferItem(final int srcX,
                                         final int srcY,
                                         final int srcZ,
                                         final int destX,
                                         final int destY,
                                         final int destZ)
    {
        Vector t = Vector.allocate(srcX, srcY, srcZ);
        Entity retval = allocate(t, EntityType.TransferItem);
        retval.data.velocity = t.set(destX, destY, destZ);
        return retval;
    }

    public static Entity
        NewApplyBoneMealOrPutBackInContainer(final int cropX,
                                             final int cropY,
                                             final int cropZ,
                                             final int containerX,
                                             final int containerY,
                                             final int containerZ)
    {
        Vector t = Vector.allocate(cropX, cropY, cropZ);
        Entity retval = allocate(t,
                                 EntityType.ApplyBoneMealOrPutBackInContainer);
        retval.data.velocity = t.set(containerX, containerY, containerZ);
        return retval;
    }

    public static Entity NewMineCart(final Vector position,
                                     final Block containedBlock)
    {
        Entity retval = allocate(position, EntityType.MineCart);
        retval.data.velocity = Vector.allocate(Vector.ZERO);
        retval.data.block = containedBlock;
        retval.data.phi = 0;
        retval.data.theta = 0;
        retval.data.existduration = MINECART_DELETE_TIME;
        retval.data.momentum = 0;
        retval.data.ridingPlayerName = null;
        retval.data.cornerVelocity = Vector.allocate(Vector.ZERO);
        retval.data.wasOverOnActivatorRail = false;
        return retval;
    }

    private void readPhiTheta(final DataInput i) throws IOException
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

    private void internalRead(final DataInput i) throws IOException
    {
        switch(this.type)
        {
        case Last:
        case Nothing:
            return;
        case ThrownBlock:
        case Block:
        {
            this.data.block = Block.read(i);
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
            this.data.block = Block.read(i);
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
                if(Float.isInfinite(this.data.frame)
                        || Float.isNaN(this.data.frame)
                        || this.data.frame < -1 - SimulationAnimationFrameCount
                        || this.data.frame > 1 + SimulationAnimationFrameCount)
                    throw new IOException("frame out of range");
                break;
            case Explosion:
                this.data.existduration = 5;
                this.data.frame = i.readFloat();
                if(Float.isInfinite(this.data.frame)
                        || Float.isNaN(this.data.frame)
                        || this.data.frame < -1 - explosionFrameCount
                        || this.data.frame > 1 + explosionFrameCount)
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
        case PlaceBlockIfReplaceable:
        case RemoveBlockIfEqual:
        {
            this.data.block = Block.read(i);
            return;
        }
        case TransferItem:
        case ApplyBoneMealOrPutBackInContainer:
        {
            this.data.velocity = Vector.read(i);
            return;
        }
        case MineCart:
        {
            this.data.velocity = Vector.read(i);
            if(i.readBoolean())
                this.data.block = Block.read(i);
            else
                this.data.block = null;
            readPhiTheta(i);
            this.data.existduration = i.readDouble();
            if(Double.isInfinite(this.data.existduration)
                    || Double.isNaN(this.data.existduration)
                    || this.data.existduration < -100
                    || this.data.existduration > 100)
                throw new IOException("exist duration out of range");
            this.data.momentum = i.readFloat();
            if(Float.isInfinite(this.data.momentum)
                    || Float.isNaN(this.data.momentum))
                throw new IOException("momentum out of range");
            int length = i.readInt();
            if(length == -1)
                this.data.ridingPlayerName = null;
            else
            {
                if(length < 0)
                    throw new IOException("riding player name length out of range");
                StringBuilder sb = new StringBuilder(length);
                for(int index = 0; index < length; index++)
                    sb.append(i.readChar());
                this.data.ridingPlayerName = sb.toString();
            }
            this.data.cornerVelocity = Vector.read(i);
            this.data.wasOverOnActivatorRail = i.readBoolean();
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
    public static Entity read(final DataInput i) throws IOException
    {
        EntityType type = EntityType.read(i);
        if(type == EntityType.Nothing)
            return allocate();
        Vector position = Vector.read(i);
        Entity retval = allocate(position, type);
        position.free();
        retval.internalRead(i);
        return retval;
    }

    /** write to a <code>DataOutput</code>
     * 
     * @param o
     *            <code>OutputStream</code> to write to
     * @throws IOException
     *             the exception thrown */
    public void write(final DataOutput o) throws IOException
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
        case ThrownBlock:
        case Block:
        {
            this.data.block.write(o);
            o.writeDouble(this.data.existduration);
            o.writeFloat(this.data.phi);
            o.writeFloat(this.data.theta);
            this.data.velocity.write(o);
            o.writeBoolean(this.data.nearperson);
            return;
        }
        case FallingBlock:
        {
            this.data.block.write(o);
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
            case Explosion:
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
        case PlaceBlockIfReplaceable:
        case RemoveBlockIfEqual:
        {
            this.data.block.write(o);
            return;
        }
        case TransferItem:
        case ApplyBoneMealOrPutBackInContainer:
        {
            this.data.velocity.write(o);
            return;
        }
        case MineCart:
        {
            this.data.velocity.write(o);
            o.writeBoolean(this.data.block != null);
            if(this.data.block != null)
                this.data.block.write(o);
            o.writeFloat(this.data.phi);
            o.writeFloat(this.data.theta);
            o.writeDouble(this.data.existduration);
            o.writeFloat(this.data.momentum);
            if(this.data.ridingPlayerName != null)
            {
                o.writeInt(this.data.ridingPlayerName.length());
                for(int index = 0; index < this.data.ridingPlayerName.length(); index++)
                    o.writeChar(this.data.ridingPlayerName.charAt(index));
            }
            else
                o.writeInt(-1);
            this.data.cornerVelocity.write(o);
            o.writeBoolean(this.data.wasOverOnActivatorRail);
            break;
        }
        }
    }

    private static Vector getBoxExposure_size = Vector.allocate();
    private static Vector getBoxExposure_t1 = Vector.allocate();
    private static Vector getBoxExposure_t2 = Vector.allocate();
    private static Vector getBoxExposure_t3 = Vector.allocate();
    private static World.BlockHitDescriptor getBoxExposure_t4 = new World.BlockHitDescriptor();

    private float getBoxExposure(final Vector min,
                                 final Vector max,
                                 final Vector origin)
    {
        int retval = 0, divisor = 0;
        final int count = 3;
        Vector size = Vector.sub(getBoxExposure_size, max, min);
        for(int x = 0; x < count; x++)
        {
            float fx = x / (count - 1);
            for(int y = 0; y < count; y++)
            {
                float fy = y / (count - 1);
                for(int z = 0; z < count; z++)
                {
                    float fz = z / (count - 1);
                    Vector disp = Vector.add(getBoxExposure_t1,
                                             min,
                                             Vector.mul(getBoxExposure_t2,
                                                        size,
                                                        Vector.set(getBoxExposure_t3,
                                                                   fx,
                                                                   fy,
                                                                   fz)))
                                        .subAndSet(origin);
                    float dist = disp.abs();
                    if(dist <= 0)
                    {
                        retval++;
                    }
                    else
                    {
                        Vector dir = Vector.div(getBoxExposure_t2, disp, dist);
                        BlockHitDescriptor bhd = world.getPointedAtBlock(getBoxExposure_t4,
                                                                         origin,
                                                                         dir,
                                                                         dist + 1e-3f,
                                                                         false,
                                                                         true);
                        if(!bhd.hitUnloadedChunk
                                && (bhd.b != null || bhd.e != null))
                            retval++;
                    }
                    divisor++;
                }
            }
        }
        return (float)retval / divisor;
    }

    private static final Vector rayHitEntity_t1 = Vector.allocate();
    private static final Vector rayHitEntity_t2 = Vector.allocate();
    private static final Matrix rayHitEntity_t3 = Matrix.allocate();
    private static final Matrix rayHitEntity_t4 = Matrix.allocate();

    public float rayHitEntity(final Vector origin, final Vector dir)
    {
        switch(this.type)
        {
        case ApplyBoneMealOrPutBackInContainer:
            break;
        case Block:
            break;
        case FallingBlock:
            break;
        case Last:
            break;
        case Nothing:
            break;
        case Particle:
            break;
        case PlaceBlockIfReplaceable:
            break;
        case PrimedTNT:
            break;
        case RemoveBlockIfEqual:
            break;
        case ThrownBlock:
            break;
        case TransferItem:
            break;
        case MineCart:
        {
            Matrix mat = Matrix.setToInverse(rayHitEntity_t3,
                                             getMinecartDrawMatrix());
            Matrix matNoTranslate = Matrix.removeTranslate(rayHitEntity_t4, mat);
            Vector dir2 = matNoTranslate.apply(rayHitEntity_t1, dir);
            Vector origin2 = mat.apply(rayHitEntity_t2, origin);
            return Block.minecartRayIntersects(origin2, dir2, this.data.block);
        }
        }
        return -1;
    }

    public void onPunch(final double punchTime)
    {
        switch(this.type)
        {
        case ApplyBoneMealOrPutBackInContainer:
            break;
        case Block:
            break;
        case FallingBlock:
            break;
        case Last:
            break;
        case Nothing:
            break;
        case Particle:
            break;
        case PlaceBlockIfReplaceable:
            break;
        case PrimedTNT:
            break;
        case RemoveBlockIfEqual:
            break;
        case ThrownBlock:
            break;
        case TransferItem:
            break;
        case MineCart:
        {
            this.data.existduration -= punchTime;
            break;
        }
        }
    }

    public void onUseButtonPress(final Player p)
    {
        switch(this.type)
        {
        case ApplyBoneMealOrPutBackInContainer:
            break;
        case Block:
            break;
        case FallingBlock:
            break;
        case Last:
            break;
        case Nothing:
            break;
        case Particle:
            break;
        case PlaceBlockIfReplaceable:
            break;
        case PrimedTNT:
            break;
        case RemoveBlockIfEqual:
            break;
        case ThrownBlock:
            break;
        case TransferItem:
            break;
        case MineCart:
        {
            if(this.data.block != null)
            {
                p.openContainerEntity(this);
                break;
            }
            this.data.ridingPlayerName = p.getName();
            p.isRiding = true;
            break;
        }
        }
    }

    public void minecartClearRiders()
    {
        this.data.ridingPlayerName = null;
    }

    public EntityType getType()
    {
        return this.type;
    }

    public Block minecartGetBlock()
    {
        return this.data.block;
    }
}
