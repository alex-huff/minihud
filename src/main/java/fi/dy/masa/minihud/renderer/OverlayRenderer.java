package fi.dy.masa.minihud.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;
import net.minecraft.block.Blocks;
import net.minecraft.class_9801;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.entity.Entity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.util.math.Vec3d;
import fi.dy.masa.malilib.util.Color4f;
import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.minihud.config.RendererToggle;

public class OverlayRenderer
{
    private static long loginTime;
    private static boolean canRender;

    public static void resetRenderTimeout()
    {
        canRender = false;
        loginTime = System.currentTimeMillis();
    }

    public static void renderOverlays(Matrix4f matrix4f, Matrix4f projMatrix, MinecraftClient mc)
    {
        Entity entity = EntityUtils.getCameraEntity();

        if (entity == null)
        {
            return;
        }

        if (canRender == false)
        {
            // Don't render before the player has been placed in the actual proper position,
            // otherwise some of the renderers mess up.
            // The magic 8.5, 65, 8.5 comes from the WorldClient constructor
            if (System.currentTimeMillis() - loginTime >= 5000 || entity.getX() != 8.5 || entity.getY() != 65 || entity.getZ() != 8.5)
            {
                canRender = true;
            }
            else
            {
                return;
            }
        }

        if (RendererToggle.OVERLAY_BEACON_RANGE.getBooleanValue())
        {
            mc.getProfiler().push(() -> "BeaconRangeHeldItem");
            renderBeaconBoxForPlayerIfHoldingItem(entity, mc);
            mc.getProfiler().pop();
        }

        RenderContainer.INSTANCE.render(entity, matrix4f, projMatrix, mc);
    }


    public static void renderBeaconBoxForPlayerIfHoldingItem(Entity entity, MinecraftClient mc)
    {
        Item item = mc.player.getMainHandStack().getItem();

        if (item instanceof BlockItem && ((BlockItem) item).getBlock() == Blocks.BEACON)
        {
            renderBeaconBoxForPlayer(entity, mc);
            return;
        }

        item = mc.player.getMainHandStack().getItem();

        if (item instanceof BlockItem && ((BlockItem) item).getBlock() == Blocks.BEACON)
        {
            renderBeaconBoxForPlayer(entity, mc);
        }
    }

    private static void renderBeaconBoxForPlayer(Entity entity, MinecraftClient mc)
    {
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        double x = Math.floor(entity.getX()) - cameraPos.x;
        double y = Math.floor(entity.getY()) - cameraPos.y;
        double z = Math.floor(entity.getZ()) - cameraPos.z;
        // Use the slot number as the level if sneaking
        int level = mc.player.isSneaking() ? Math.min(4, mc.player.getInventory().selectedSlot + 1) : 4;
        float range = level * 10 + 10;
        float minX = (float) (x - range);
        float minY = (float) (y - range);
        float minZ = (float) (z - range);
        float maxX = (float) (x + range + 1);
        float maxY = (float) (y + 4);
        float maxZ = (float) (z + range + 1);
        Color4f color = OverlayRendererBeaconRange.getColorForLevel(level);

        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.polygonOffset(-3f, -3f);
        RenderSystem.enablePolygonOffset();
        fi.dy.masa.malilib.render.RenderUtils.setupBlend();
        fi.dy.masa.malilib.render.RenderUtils.color(1f, 1f, 1f, 1f);

        Tessellator tessellator = Tessellator.getInstance();
        //BufferBuilder buffer = tessellator.getBuffer();
        BufferBuilder buffer = tessellator.method_60827(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        // FIXME MeshData
        class_9801 meshData;

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.applyModelViewMatrix();
        //buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        fi.dy.masa.malilib.render.RenderUtils.drawBoxAllSidesBatchedQuads(minX, minY, minZ, maxX, maxY, maxZ, Color4f.fromColor(color, 0.3f), buffer);

        //tessellator.draw();
        meshData = buffer.method_60800();
        BufferRenderer.drawWithGlobalProgram(meshData);
        meshData.close();

        buffer = tessellator.method_60827(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        fi.dy.masa.malilib.render.RenderUtils.drawBoxAllEdgesBatchedLines(minX, minY, minZ, maxX, maxY, maxZ, Color4f.fromColor(color, 1f), buffer);

        //tessellator.draw();
        meshData = buffer.method_60800();
        BufferRenderer.drawWithGlobalProgram(meshData);
        meshData.close();

        RenderSystem.polygonOffset(0f, 0f);
        RenderSystem.disablePolygonOffset();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
