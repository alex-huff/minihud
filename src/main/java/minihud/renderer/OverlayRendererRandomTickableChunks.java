package minihud.renderer;

import javax.annotation.Nullable;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.minecraft.entity.Entity;

import malilib.util.data.Color4f;
import malilib.util.data.json.JsonUtils;
import malilib.util.game.wrap.EntityWrap;
import malilib.util.position.ChunkPos;
import malilib.util.position.Direction;
import malilib.util.position.PositionUtils;
import malilib.util.position.Vec3d;
import minihud.config.Configs;
import minihud.config.RendererToggle;

public class OverlayRendererRandomTickableChunks extends MiniHudOverlayRenderer
{
    protected final RendererToggle toggle;
    protected Vec3d pos = Vec3d.ZERO;
    @Nullable protected Vec3d newPos;

    public OverlayRendererRandomTickableChunks(RendererToggle toggle)
    {
        this.toggle = toggle;
    }

    @Override
    public void onEnabled()
    {
        super.onEnabled();

        if (this.toggle == RendererToggle.RANDOM_TICKS_FIXED && this.shouldRender())
        {
            this.newPos = EntityWrap.getCameraEntityPosition();
        }
    }

    @Override
    public boolean shouldRender()
    {
        return this.toggle.isRendererEnabled();
    }

    @Override
    public boolean needsUpdate(Entity entity)
    {
        if (this.toggle == RendererToggle.RANDOM_TICKS_FIXED)
        {
            return this.newPos != null;
        }
        // Player-following renderer
        else if (this.toggle == RendererToggle.RANDOM_TICKS_PLAYER)
        {
            return EntityWrap.getX(entity) != this.pos.x ||
                   EntityWrap.getZ(entity) != this.pos.z;
        }

        return false;
    }

    @Override
    public void update(Vec3d cameraPos, Entity entity)
    {
        if (this.toggle == RendererToggle.RANDOM_TICKS_PLAYER)
        {
            this.pos = EntityWrap.getEntityPos(entity);
        }
        else if (this.newPos != null)
        {
            this.pos = this.newPos;
            this.newPos = null;
        }

        final Color4f color = this.toggle == RendererToggle.RANDOM_TICKS_PLAYER ?
                Configs.Colors.RANDOM_TICKS_PLAYER_OVERLAY_COLOR.getColor() :
                Configs.Colors.RANDOM_TICKS_FIXED_OVERLAY_COLOR.getColor();

        LongOpenHashSet chunks = this.getRandomTickableChunks(this.pos);

        this.startBuffers();

        for (long posLong : chunks)
        {
            this.renderChunkEdgesIfApplicable(posLong, cameraPos, chunks, color);
        }

        this.uploadBuffers();
    }

    protected LongOpenHashSet getRandomTickableChunks(Vec3d posCenter)
    {
        LongOpenHashSet set = new LongOpenHashSet();
        final int centerChunkX = ((int) Math.floor(posCenter.x)) >> 4;
        final int centerChunkZ = ((int) Math.floor(posCenter.z)) >> 4;
        final double maxRange = 128.0 * 128.0;
        final int r = 9;

        for (int cz = centerChunkZ - r; cz <= centerChunkZ + r; ++cz)
        {
            for (int cx = centerChunkX - r; cx <= centerChunkX + r; ++cx)
            {
                double dx = (double) (cx * 16 + 8) - posCenter.x;
                double dz = (double) (cz * 16 + 8) - posCenter.z;

                if ((dx * dx + dz * dz) < maxRange)
                {
                    set.add(ChunkPos.asLong(cx, cz));
                }
            }
        }

        return set;
    }

    protected void renderChunkEdgesIfApplicable(long chunkPosLong, Vec3d cameraPos, LongOpenHashSet chunks, Color4f color)
    {
        for (Direction side : Direction.HORIZONTAL_DIRECTIONS)
        {
            int cx = PositionUtils.getChunkPosX(chunkPosLong);
            int cz = PositionUtils.getChunkPosZ(chunkPosLong);
            long posTmp = ChunkPos.asLong(cx + side.getXOffset(), cz + side.getZOffset());

            if (chunks.contains(posTmp) == false)
            {
                this.renderChunkEdge(chunkPosLong, side, cameraPos, color);
            }
        }
    }

    private void renderChunkEdge(long chunkPosLong, Direction side, Vec3d cameraPos, Color4f color)
    {
        int cx = PositionUtils.getChunkPosX(chunkPosLong);
        int cz = PositionUtils.getChunkPosZ(chunkPosLong);
        double minX, minZ, maxX, maxZ;

        switch (side)
        {
            case NORTH:
                minX = (cx << 4);
                minZ = (cz << 4);
                maxX = (cx << 4) + 16.0;
                maxZ = (cz << 4);
                break;
            case SOUTH:
                minX = (cx << 4);
                minZ = (cz << 4) + 16.0;
                maxX = (cx << 4) + 16.0;
                maxZ = (cz << 4) + 16.0;
                break;
            case WEST:
                minX = (cx << 4);
                minZ = (cz << 4);
                maxX = (cx << 4);
                maxZ = (cz << 4) + 16.0;
                break;
            case EAST:
                minX = (cx << 4) + 16.0;
                minZ = (cz << 4);
                maxX = (cx << 4) + 16.0;
                maxZ = (cz << 4) + 16.0;
                break;
            default:
                return;
        }

        RenderUtils.renderWallWithLines(minX, 0, minZ, maxX, 256, maxZ, 16, 16, true, cameraPos, color, this.quadBuilder, this.lineBuilder);
    }

    @Override
    public String getSaveId()
    {
        return this.toggle == RendererToggle.RANDOM_TICKS_FIXED ? "random_tickable_chunks" : "";
    }

    @Nullable
    @Override
    public JsonObject toJson()
    {
        JsonObject obj = super.toJson();
        obj.add("pos", JsonUtils.vec3dToJson(this.pos));
        return obj;
    }

    @Override
    public void fromJson(JsonObject obj)
    {
        super.fromJson(obj);

        Vec3d pos = JsonUtils.getVec3d(obj, "pos");

        if (pos != null && this.toggle == RendererToggle.RANDOM_TICKS_FIXED)
        {
            this.newPos = pos;
        }
    }
}
