package minihud.renderer;

import java.util.ArrayList;
import java.util.List;
import org.lwjgl.opengl.GL11;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.chunk.Chunk;

import malilib.config.option.ColorConfig;
import malilib.config.option.Vec2dConfig;
import malilib.render.buffer.VertexBuilder;
import malilib.render.overlay.VboRenderObject;
import malilib.util.data.Color4f;
import malilib.util.data.Identifier;
import malilib.util.game.wrap.EntityWrap;
import malilib.util.game.wrap.GameWrap;
import malilib.util.game.wrap.RenderWrap;
import malilib.util.position.BlockPos;
import malilib.util.position.Direction;
import malilib.util.position.Vec3d;
import minihud.Reference;
import minihud.config.Configs;
import minihud.config.RendererToggle;
import minihud.util.value.LightLevelMarkerMode;
import minihud.util.value.LightLevelNumberMode;

public class OverlayRendererLightLevel extends MiniHudOverlayRenderer
{
    private static final Identifier NUMBER_TEXTURE = new Identifier(Reference.MOD_ID, "textures/misc/light_level_numbers.png");

    private final List<LightLevelInfo> lightInfoList = new ArrayList<>();
    private Direction lastDirection = Direction.NORTH;

    public OverlayRendererLightLevel()
    {
        super(COLORED_TEXTURED_QUADS_BUILDER, COLORED_LINES_BUILDER);
    }

    @Override
    public boolean shouldRender()
    {
        return RendererToggle.LIGHT_LEVEL.isRendererEnabled();
    }

    @Override
    public boolean needsUpdate(Entity entity)
    {
        return this.needsUpdate || this.lastUpdatePos == null ||
               Math.abs(EntityWrap.getX(entity) - this.lastUpdatePos.getX()) > 4 ||
               Math.abs(EntityWrap.getY(entity) - this.lastUpdatePos.getY()) > 4 ||
               Math.abs(EntityWrap.getZ(entity) - this.lastUpdatePos.getZ()) > 4 ||
               (Configs.Generic.LIGHT_LEVEL_NUMBER_ROTATION.getBooleanValue() &&
                   this.lastDirection != EntityWrap.getClosestHorizontalLookingDirection(entity));
    }

    @Override
    public void update(Vec3d cameraPos, Entity entity)
    {
        this.startBuffers();

        BlockPos pos = EntityWrap.getEntityBlockPos(entity);
        //long pre = System.nanoTime();
        this.updateLightLevels(GameWrap.getClientWorld(), pos);
        //System.out.printf("LL markers: %d, time: %.3f s\n", LIGHT_INFOS.size(), (double) (System.nanoTime() - pre) / 1000000000D);
        this.renderLightLevels(cameraPos);

        this.uploadBuffers();
        this.lastDirection = EntityWrap.getClosestHorizontalLookingDirection(entity);
        this.needsUpdate = false;
    }

    @Override
    protected void preRender()
    {
        super.preRender();

        RenderWrap.bindTexture(NUMBER_TEXTURE);
    }

    @Override
    public void allocateGlResources()
    {
        this.quadRenderer = this.allocateBuffer(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR, VboRenderObject::setupArrayPointersPosUvColor);
        this.outlineRenderer = this.allocateBuffer(GL11.GL_LINES);
    }

    protected void renderLightLevels(Vec3d cameraPos)
    {
        final int count = this.lightInfoList.size();
        Entity entity = GameWrap.getCameraEntity();

        if (count > 0)
        {
            Direction numberFacing = Configs.Generic.LIGHT_LEVEL_NUMBER_ROTATION.getBooleanValue() ? EntityWrap.getClosestHorizontalLookingDirection(entity) : Direction.NORTH;
            LightLevelNumberMode numberMode = Configs.Generic.LIGHT_LEVEL_NUMBER_MODE.getValue();
            LightLevelMarkerMode markerMode = Configs.Generic.LIGHT_LEVEL_MARKER_MODE.getValue();
            boolean useColoredNumbers = Configs.Generic.LIGHT_LEVEL_COLORED_NUMBERS.getBooleanValue();
            int lightThreshold = Configs.Generic.LIGHT_LEVEL_THRESHOLD.getIntegerValue();

            if (numberMode == LightLevelNumberMode.BLOCK || numberMode == LightLevelNumberMode.BOTH)
            {
                this.renderNumbers(cameraPos, LightLevelNumberMode.BLOCK,
                                   Configs.Generic.LIGHT_LEVEL_NUMBER_OFFSET_BLOCK,
                                   Configs.Colors.LIGHT_LEVEL_NUMBER_BLOCK_LIT,
                                   Configs.Colors.LIGHT_LEVEL_NUMBER_BLOCK_DARK,
                                   useColoredNumbers, lightThreshold, numberFacing);
            }

            if (numberMode == LightLevelNumberMode.SKY || numberMode == LightLevelNumberMode.BOTH)
            {
                this.renderNumbers(cameraPos, LightLevelNumberMode.SKY,
                                   Configs.Generic.LIGHT_LEVEL_NUMBER_OFFSET_SKY,
                                   Configs.Colors.LIGHT_LEVEL_NUMBER_SKY_LIT,
                                   Configs.Colors.LIGHT_LEVEL_NUMBER_SKY_DARK,
                                   useColoredNumbers, lightThreshold, numberFacing);
            }

            if (markerMode == LightLevelMarkerMode.SQUARE)
            {
                this.renderMarkers(this::renderLightLevelSquare, cameraPos, lightThreshold);
            }
            else if (markerMode == LightLevelMarkerMode.CROSS)
            {
                this.renderMarkers(this::renderLightLevelCross, cameraPos, lightThreshold);
            }
        }
    }

    protected void renderNumbers(Vec3d cameraPos, LightLevelNumberMode mode, Vec2dConfig cfgOff,
                                 ColorConfig cfgColorLit, ColorConfig cfgColorDark, boolean useColoredNumbers,
                                 int lightThreshold, Direction numberFacing)
    {
        double ox = cfgOff.getValue().x;
        double oz = cfgOff.getValue().y;
        double tmpX, tmpZ;
        Color4f colorLit, colorDark;
        double offsetY = Configs.Generic.LIGHT_LEVEL_Z_OFFSET.getDoubleValue();

        switch (numberFacing)
        {
            case SOUTH: tmpX =  ox; tmpZ =  oz; break;
            case WEST:  tmpX = -oz; tmpZ =  ox; break;
            case EAST:  tmpX =  oz; tmpZ = -ox; break;
            case NORTH:
            default:    tmpX = -ox; tmpZ = -oz;
        }

        if (useColoredNumbers)
        {
            colorLit = cfgColorLit.getColor();
            colorDark = cfgColorDark.getColor();
        }
        else
        {
            colorLit = Color4f.WHITE;
            colorDark = Color4f.WHITE;
        }

        this.renderLightLevelNumbers(tmpX + cameraPos.x, cameraPos.y - offsetY, tmpZ + cameraPos.z,
                                     numberFacing, lightThreshold, mode, colorLit, colorDark);
    }

    protected void renderMarkers(IMarkerRenderer renderer, Vec3d cameraPos, int lightThreshold)
    {
        double markerSize = Configs.Generic.LIGHT_LEVEL_MARKER_SIZE.getDoubleValue();
        Color4f colorLit = Configs.Colors.LIGHT_LEVEL_MARKER_LIT.getColor();
        Color4f colorDark = Configs.Colors.LIGHT_LEVEL_MARKER_DARK.getColor();
        double offsetX = cameraPos.x;
        double offsetY = cameraPos.y - Configs.Generic.LIGHT_LEVEL_Z_OFFSET.getDoubleValue();
        double offsetZ = cameraPos.z;
        double offset1 = (1.0 - markerSize) / 2.0;
        double offset2 = (1.0 - offset1);
        VertexBuilder lineBuilder = this.lineBuilder;

        for (LightLevelInfo info : this.lightInfoList)
        {
            if (info.block < lightThreshold)
            {
                BlockPos pos = info.pos;
                Color4f color = info.sky >= lightThreshold ? colorLit : colorDark;
                renderer.render(pos.getX() - offsetX, pos.getY() - offsetY, pos.getZ() - offsetZ,
                                color, offset1, offset2, lineBuilder);
            }
        }
    }

    protected void renderLightLevelNumbers(double dx, double dy, double dz, Direction facing,
                                           int lightThreshold, LightLevelNumberMode numberMode,
                                           Color4f colorLit, Color4f colorDark)
    {
        for (LightLevelInfo info : this.lightInfoList)
        {
            int lightLevel = numberMode == LightLevelNumberMode.BLOCK ? info.block : info.sky;
            Color4f color = lightLevel >= lightThreshold ? colorLit : colorDark;
            BlockPos pos = info.pos;
            double x = pos.getX() - dx;
            double y = pos.getY() - dy;
            double z = pos.getZ() - dz;

            this.renderLightLevelTextureColor(x, y, z, facing, lightLevel, color, this.quadBuilder);
        }
    }

    protected void renderLightLevelTextureColor(double x, double y, double z, Direction facing,
                                                int lightLevel, Color4f color, VertexBuilder builder)
    {
        float w = 0.25f;
        float u = (lightLevel & 0x3) * w;
        float v = (lightLevel >> 2) * w;

        switch (facing)
        {
            case NORTH:
                builder.posUvColor(x    , y, z    , u    , v    , color);
                builder.posUvColor(x    , y, z + 1, u    , v + w, color);
                builder.posUvColor(x + 1, y, z + 1, u + w, v + w, color);
                builder.posUvColor(x + 1, y, z    , u + w, v    , color);
                break;

            case SOUTH:
                builder.posUvColor(x + 1, y, z + 1, u    , v    , color);
                builder.posUvColor(x + 1, y, z    , u    , v + w, color);
                builder.posUvColor(x    , y, z    , u + w, v + w, color);
                builder.posUvColor(x    , y, z + 1, u + w, v    , color);
                break;

            case EAST:
                builder.posUvColor(x + 1, y, z    , u    , v    , color);
                builder.posUvColor(x    , y, z    , u    , v + w, color);
                builder.posUvColor(x    , y, z + 1, u + w, v + w, color);
                builder.posUvColor(x + 1, y, z + 1, u + w, v    , color);
                break;

            case WEST:
                builder.posUvColor(x    , y, z + 1, u    , v    , color);
                builder.posUvColor(x + 1, y, z + 1, u    , v + w, color);
                builder.posUvColor(x + 1, y, z    , u + w, v + w, color);
                builder.posUvColor(x    , y, z    , u + w, v    , color);
                break;

            default:
        }
    }

    protected void renderLightLevelCross(double x, double y, double z, Color4f color,
                                         double offset1, double offset2, VertexBuilder builder)
    {
        builder.posColor(x + offset1, y, z + offset1, color);
        builder.posColor(x + offset2, y, z + offset2, color);

        builder.posColor(x + offset1, y, z + offset2, color);
        builder.posColor(x + offset2, y, z + offset1, color);
    }

    private void renderLightLevelSquare(double x, double y, double z, Color4f color,
                                        double offset1, double offset2, VertexBuilder builder)
    {
        builder.posColor(x + offset1, y, z + offset1, color);
        builder.posColor(x + offset1, y, z + offset2, color);

        builder.posColor(x + offset1, y, z + offset2, color);
        builder.posColor(x + offset2, y, z + offset2, color);

        builder.posColor(x + offset2, y, z + offset2, color);
        builder.posColor(x + offset2, y, z + offset1, color);

        builder.posColor(x + offset2, y, z + offset1, color);
        builder.posColor(x + offset1, y, z + offset1, color);
    }

    private void updateLightLevels(World world, BlockPos center)
    {
        this.lightInfoList.clear();

        int radius = Configs.Generic.LIGHT_LEVEL_RANGE.getIntegerValue();
        final int minX = center.getX() - radius;
        final int minY = center.getY() - radius;
        final int minZ = center.getZ() - radius;
        final int maxX = center.getX() + radius;
        final int maxY = center.getY() + radius;
        final int maxZ = center.getZ() + radius;
        final int minCX = (minX >> 4);
        final int minCZ = (minZ >> 4);
        final int maxCX = (maxX >> 4);
        final int maxCZ = (maxZ >> 4);

        for (int cx = minCX; cx <= maxCX; ++cx)
        {
            final int startX = Math.max( cx << 4      , minX);
            final int endX   = Math.min((cx << 4) + 15, maxX);

            for (int cz = minCZ; cz <= maxCZ; ++cz)
            {
                final int startZ = Math.max( cz << 4      , minZ);
                final int endZ   = Math.min((cz << 4) + 15, maxZ);
                Chunk chunk = world.getChunk(cx, cz);

                for (int x = startX; x <= endX; ++x)
                {
                    for (int z = startZ; z <= endZ; ++z)
                    {
                        final int startY = Math.max(minY, 0);
                        final int endY   = Math.min(maxY, chunk.getTopFilledSegment() + 15 + 1);
                        IBlockState stateDown = chunk.getBlockState(x, startY - 1, z);
                        IBlockState state    = chunk.getBlockState(x, startY, z);
                        IBlockState stateUp  = chunk.getBlockState(x, startY + 1, z);
                        IBlockState stateUp2 = chunk.getBlockState(x, startY + 2, z);

                        for (int y = startY; y <= endY; ++y)
                        {
                            if (canSpawnAt(stateDown, state, stateUp, stateUp2))
                            {
                                BlockPos pos = new BlockPos(x, y, z);
                                int block = y < 256 ? chunk.getLightFor(EnumSkyBlock.BLOCK, pos) : 0;
                                int sky   = y < 256 ? chunk.getLightFor(EnumSkyBlock.SKY, pos) : 15;

                                this.lightInfoList.add(new LightLevelInfo(pos, block, sky));

                                //y += 2; // if the spot is spawnable, that means the next spawnable spot can be the third block up
                            }

                            stateDown = state;
                            state = stateUp;
                            stateUp = stateUp2;
                            stateUp2 = chunk.getBlockState(x, y + 3, z);
                        }
                    }
                }
            }
        }
    }

    /**
     * This method mimics the one from WorldEntitySpawner, but takes in the Chunk to avoid that lookup
     */
    public static boolean canSpawnAt(IBlockState stateDown, IBlockState state, IBlockState stateUp, IBlockState stateUp2)
    {
        if (stateDown.isTopSolid() == false ||
            stateDown.getBlock() == Blocks.BEDROCK ||
            stateDown.getBlock() == Blocks.BARRIER)
        {
            return false;
        }
        else
        {
            if (state.getMaterial() == Material.WATER)
            {
                return stateUp.getMaterial() == Material.WATER &&
                       stateUp2.isNormalCube() == false;
            }

            return WorldEntitySpawner.isValidEmptySpawnBlock(state) &&
                   WorldEntitySpawner.isValidEmptySpawnBlock(stateUp);
        }
    }

    public static class LightLevelInfo
    {
        public final BlockPos pos;
        public final int block;
        public final int sky;

        public LightLevelInfo(BlockPos pos, int block, int sky)
        {
            this.pos = pos;
            this.block = block;
            this.sky = sky;
        }
    }

    private interface IMarkerRenderer
    {
        void render(double x, double y, double z, Color4f color, double offset1, double offset2, VertexBuilder builder);
    }
}
