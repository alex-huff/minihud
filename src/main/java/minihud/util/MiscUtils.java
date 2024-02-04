package minihud.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import malilib.util.StringUtils;
import malilib.util.game.wrap.EntityWrap;
import malilib.util.game.wrap.GameWrap;
import malilib.util.game.wrap.ItemWrap;
import malilib.util.game.wrap.NbtWrap;
import malilib.util.game.wrap.RegistryUtils;
import malilib.util.nbt.PrettyNbtStringifier;
import malilib.util.nbt.SimpleNbtStringifier;
import malilib.util.position.BlockPos;
import malilib.util.position.ChunkPos;
import malilib.util.position.IntBoundingBox;
import minihud.MiniHud;
import minihud.config.Configs;
import minihud.data.DroppedChunks;
import minihud.mixin.info_lines.ChunkProviderServerMixin;

public class MiscUtils
{
    private static final Random RAND = new Random();

    public static long bytesToMb(long bytes)
    {
        return bytes / 1024L / 1024L;
    }

    public static double intAverage(int[] values)
    {
        long sum = 0L;

        for (int value : values)
        {
            sum += value;
        }

        return (double) sum / (double) values.length;
    }

    public static boolean canSlimeSpawnAt(int posX, int posZ, long worldSeed)
    {
        return canSlimeSpawnInChunk(posX >> 4, posZ >> 4, worldSeed);
    }

    public static boolean canSlimeSpawnInChunk(int chunkX, int chunkZ, long worldSeed)
    {
        long slimeSeed = 987234911L;
        long rngSeed = worldSeed +
                       (long) (chunkX * chunkX *  4987142) +
                       (long) (chunkX * 5947611) +
                       (long) (chunkZ * chunkZ) * 4392871L +
                       (long) (chunkZ * 389711) ^ slimeSeed;

        RAND.setSeed(rngSeed);

        return RAND.nextInt(10) == 0;
    }

    public static int getChunkUnloadBucket(int chunkX, int chunkZ)
    {
        if (Configs.Generic.CHUNK_UNLOAD_BUCKET_HASH_SIZE.getBooleanValue())
        {
            return getChunkOrder(chunkX, chunkZ, DroppedChunks.getDroppedChunksHashSize());
        }
        // The old simple calculation, without knowledge of the HashSet size
        else
        {
            int longHash = Long.valueOf(ChunkPos.asLong(chunkX, chunkZ)).hashCode();
            return (longHash ^ (longHash >>> 16)) & 0xFFFF;
        }
    }

    /**
     * This method has been taken from the Carpet mod, by gnembon
     */
    public static int getCurrentHashSize(WorldServer world)
    {
        ChunkProviderServerMixin provider = (ChunkProviderServerMixin) world.getChunkProvider();

        try
        {
            Set<Long> droppedChunks = provider.minihud_getDroppedChunks();
            Field field = droppedChunks.getClass().getDeclaredField("map");
            field.setAccessible(true);

            @SuppressWarnings("unchecked")
            HashMap<Object, Object> map = (HashMap<Object, Object>) field.get(droppedChunks);
            field = map.getClass().getDeclaredField("table");
            field.setAccessible(true);

            Object[] table = (Object []) field.get(map);

            if (table == null)
            {
                return 2;
            }

            return table.length;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return -1;
    }


    /**
     * This method has been taken from the Carpet mod, by gnembon
     */
    public static int getChunkOrder(int chunkX, int chunkZ, int hashSize)
    {
        try
        {
            Method method = HashMap.class.getDeclaredMethod("hash", Object.class);
            method.setAccessible(true);

            return (Integer) method.invoke(null, Long.hashCode(ChunkPos.asLong(chunkX, chunkZ))) & (hashSize - 1);
        }
        catch (Exception e)
        {
            MiniHud.LOGGER.error("Error while trying to get the chunk unload order");
            return -1;
        }
    }

    public static int getSpawnableChunksCount(WorldServer world)
    {
        Set<ChunkPos> eligibleChunksForSpawning = new HashSet<>();
        int chunkCount = 0;

        for (EntityPlayer player : world.playerEntities)
        {
            if (player.isSpectator() == false)
            {
                int cx = EntityWrap.getChunkX(player);
                int cz = EntityWrap.getChunkZ(player);
                int chunkRadius = 8;

                for (int cxOff = -chunkRadius; cxOff <= chunkRadius; ++cxOff)
                {
                    for (int czOff = -chunkRadius; czOff <= chunkRadius; ++czOff)
                    {
                        boolean edgeChunk = cxOff == -chunkRadius || cxOff == chunkRadius || czOff == -chunkRadius || czOff == chunkRadius;
                        ChunkPos chunkPos = new ChunkPos(cxOff + cx, czOff + cz);

                        if (eligibleChunksForSpawning.contains(chunkPos) == false)
                        {
                            ++chunkCount;

                            if (edgeChunk == false && world.getWorldBorder().contains(chunkPos))
                            {
                                PlayerChunkMapEntry playerchunkmapentry = world.getPlayerChunkMap().getEntry(chunkPos.x, chunkPos.z);

                                if (playerchunkmapentry != null && playerchunkmapentry.isSentToPlayers())
                                {
                                    eligibleChunksForSpawning.add(chunkPos);
                                }
                            }
                        }
                    }
                }
            }
        }

        return chunkCount;
    }

    public static void getItemTooltip(ItemStack stack, List<String> lines)
    {
        boolean showPretty = Configs.Hotkeys.ITEM_NBT_KEY_PRETTY.isHeld();
        boolean showString = Configs.Hotkeys.ITEM_NBT_KEY_STRING.isHeld();
        NBTTagCompound tag = ItemWrap.getTag(stack);

        // If the vanilla advanced tooltips are disabled, add them here, when showing a tooltip
        if (GameWrap.getClient().gameSettings.advancedItemTooltips == false && (showPretty || showString))
        {
            if (stack.isItemDamaged())
            {
                lines.add(StringUtils.translate("item.durability", stack.getMaxDamage() - stack.getItemDamage(), stack.getMaxDamage()));
            }

            String regName = RegistryUtils.getItemIdStr(stack.getItem());
            lines.add(StringUtils.translate("minihud.tooltip.item.registry_name", regName));

            if (stack.hasTagCompound())
            {
                lines.add(StringUtils.translate("minihud.tooltip.item.nbt", NbtWrap.getKeys(tag).size()));
            }
        }

        if (tag != null)
        {
            String color = StringUtils.translate("minihud.tooltip.item.stringified_nbt_base_color");

            if (showPretty)
            {
                lines.addAll((new PrettyNbtStringifier(color)).getNbtLines(tag));
            }

            if (showString)
            {
                String str = (new SimpleNbtStringifier(color)).getNbtString(tag);
                StringUtils.splitTextToLines(lines, str, 240);
            }
        }
    }

    public static boolean isStructureWithinRange(IntBoundingBox bb, BlockPos playerPos, int maxRange)
    {
        return playerPos.getX() >= (bb.minX - maxRange) &&
               playerPos.getX() <= (bb.maxX + maxRange) &&
               playerPos.getZ() >= (bb.minZ - maxRange) &&
               playerPos.getZ() <= (bb.maxZ + maxRange);
    }

    public static boolean isStructureWithinRange(StructureBoundingBox bb, BlockPos playerPos, int maxRange)
    {
        return playerPos.getX() >= (bb.minX - maxRange) &&
               playerPos.getX() <= (bb.maxX + maxRange) &&
               playerPos.getZ() >= (bb.minZ - maxRange) &&
               playerPos.getZ() <= (bb.maxZ + maxRange);
    }
}
