package fi.dy.masa.minihud.hotkeys;

import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.hotkeys.KeyCallbackToggleBooleanConfigWithMessage;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.event.RenderHandler;
import fi.dy.masa.minihud.renderer.OverlayRenderer;
import fi.dy.masa.minihud.renderer.OverlayRendererRandomTickableChunks;
import fi.dy.masa.minihud.renderer.OverlayRendererSlimeChunks;
import fi.dy.masa.minihud.renderer.OverlayRendererSpawnableChunks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;

public class KeyCallbackToggleRenderer extends KeyCallbackToggleBooleanConfigWithMessage
{
    protected final RendererToggle rendererConfig;

    public KeyCallbackToggleRenderer(RendererToggle config)
    {
        super(config);

        this.rendererConfig = config;
    }

    @Override
    public boolean onKeyAction(KeyAction action, IKeybind key)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc != null && mc.player != null && super.onKeyAction(action, key))
        {
            String green = TextFormatting.GREEN.toString();
            String red = TextFormatting.RED.toString();
            String rst = TextFormatting.RESET.toString();
            String statusOn = green + I18n.format("malilib.message.value.on") + rst;
            String statusOff = red + I18n.format("malilib.message.value.off") + rst;
            String strStatus = this.config.getBooleanValue() ? statusOn : statusOff;

            if (key == RendererToggle.OVERLAY_CHUNK_UNLOAD_BUCKET.getKeybind())
            {
                OverlayRenderer.chunkUnloadBucketOverlayY = mc.player.posY - 2;
            }
            else if (key == RendererToggle.OVERLAY_SLIME_CHUNKS_OVERLAY.getKeybind())
            {
                OverlayRendererSlimeChunks.overlayTopY = mc.player.posY;
            }
            else if (key == RendererToggle.OVERLAY_SPAWN_CHUNK_OVERLAY_REAL.getKeybind() && this.rendererConfig.getBooleanValue())
            {
                BlockPos spawn = mc.world.getSpawnPoint();
                RenderHandler.getInstance().getDataStorage().setWorldSpawn(spawn);
                String strPos = String.format("x: %d, y: %d, z: %d", spawn.getX(), spawn.getY(), spawn.getZ());
                String message = I18n.format("minihud.message.toggled_using_world_spawn", this.config.getPrettyName(), strStatus, strPos);

                StringUtils.printActionbarMessage(message);
            }
            else if (key == RendererToggle.OVERLAY_RANDOM_TICKS_FIXED.getKeybind() && this.rendererConfig.getBooleanValue())
            {
                Vec3d pos = mc.player.getPositionVector();
                OverlayRendererRandomTickableChunks.newPos = pos;
                String strPos = String.format("x: %.2f, y: %.2f, z: %.2f", pos.x, pos.y, pos.z);
                String message = I18n.format("minihud.message.toggled_using_position", this.config.getPrettyName(), strStatus, strPos);

                StringUtils.printActionbarMessage(message);
            }
            else if (key == RendererToggle.OVERLAY_SPAWNABLE_CHUNKS_PLAYER.getKeybind() && this.rendererConfig.getBooleanValue())
            {
                OverlayRendererSpawnableChunks.overlayTopY = mc.player.posY;
            }
            else if (key == RendererToggle.OVERLAY_SPAWNABLE_CHUNKS_FIXED.getKeybind() && this.rendererConfig.getBooleanValue())
            {
                BlockPos pos = new BlockPos(mc.player);
                OverlayRendererSpawnableChunks.newPos = pos;
                OverlayRendererSpawnableChunks.overlayTopY = mc.player.posY;
                String strPos = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                String message = I18n.format("minihud.message.toggled_using_position", this.config.getPrettyName(), strStatus, strPos);

                StringUtils.printActionbarMessage(message);

            }

            return true;
        }

        return false;
    }
}
