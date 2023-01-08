package com.May2Beez.modules.world;

import com.May2Beez.May2BeezQoL;
import com.May2Beez.modules.Module;
import com.May2Beez.utils.LocationUtils;
import com.May2Beez.utils.ReflectionUtils;
import com.May2Beez.utils.RenderUtils;
import com.google.common.collect.Lists;
import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockStone;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.crash.CrashReport;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ReportedException;
import net.minecraft.util.Vec3;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class WorldScanner extends Module {

    public WorldScanner() {
        super("WorldScanner");
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    public static class World {
        private final ConcurrentHashMap<String, BlockPos> crystalWaypoints;
        private final ConcurrentHashMap<String, BlockPos> mobSpotWaypoints;
        private final ConcurrentLinkedQueue<BlockPos> fairyGrottosWaypoints;
        private final ConcurrentLinkedQueue<BlockPos> wormFishingWaypoints;
        public World() {
            this.crystalWaypoints = new ConcurrentHashMap<>();
            this.mobSpotWaypoints = new ConcurrentHashMap<>();
            this.fairyGrottosWaypoints = new ConcurrentLinkedQueue<>();
            this.wormFishingWaypoints = new ConcurrentLinkedQueue<>();
        }

        public void updateCrystalWaypoints(String name, BlockPos blockPos) {
            this.crystalWaypoints.put(name, blockPos);
        }

        public ConcurrentHashMap<String, BlockPos> getCrystalWaypoints() {
            return crystalWaypoints;
        }

        public void updateMobSpotWaypoints(String name, BlockPos blockPos) {
            this.mobSpotWaypoints.put(name, blockPos);
        }

        public ConcurrentHashMap<String, BlockPos> getMobSpotWaypoints() {
            return mobSpotWaypoints;
        }

        public void updateFairyGrottos(BlockPos blockPos) {
            this.fairyGrottosWaypoints.add(blockPos);
        }

        public ConcurrentLinkedQueue<BlockPos> getFairyGrottos() {
            return this.fairyGrottosWaypoints;
        }

        public void updateWormFishing(BlockPos blockPos) {
            this.wormFishingWaypoints.add(blockPos);
        }

        public ConcurrentLinkedQueue<BlockPos> getWormFishing() {
            return this.wormFishingWaypoints;
        }
    }

    public static final HashMap<String, World> worlds = new HashMap<>();
    private static int cooldown = 100;
    private static long lastScan = 0;

    private static final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (!May2BeezQoL.config.worldScanner || mc.theWorld == null || mc.thePlayer == null) return;
        if (May2BeezQoL.config.worldScannerScanMode == 1) return;
        if (cooldown != 0) return;
        World currentWorld = worlds.get(LocationUtils.serverName);
        if (currentWorld == null) return;
        CompletableFuture.runAsync(() -> handleChunkLoad(event.getChunk(), currentWorld));
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;
        if (!May2BeezQoL.config.worldScanner) return;
        if (event.phase == TickEvent.Phase.START) return;
        if (cooldown > 0) {
            cooldown--;
        }
        if (cooldown == 1 && !worlds.containsKey(LocationUtils.serverName)) {
            worlds.put(LocationUtils.serverName, new World());
        }
        if (cooldown == 0) {
            if (May2BeezQoL.config.worldScannerScanMode == 0) return;
            World currentWorld = worlds.get(LocationUtils.serverName);
            if (currentWorld == null) return;
            if (System.currentTimeMillis() - lastScan > May2BeezQoL.config.worldScannerScanFrequency * 1000L) {
                lastScan = System.currentTimeMillis();
                Object object = ReflectionUtils.field(mc.theWorld.getChunkProvider(), "chunkListing");
                if (object != null && object.getClass() == Lists.newArrayList().getClass()) {
                    for (Chunk chunk : (List<Chunk>) object) {
                        CompletableFuture.runAsync(() -> handleChunkLoad(chunk, currentWorld));
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        cooldown = 100;
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        if (!May2BeezQoL.config.worldScanner || mc.theWorld == null || mc.thePlayer == null) return;
        if (cooldown != 0) return;
        if (!LocationUtils.onSkyblock) return;
        World currentWorld = worlds.get(LocationUtils.serverName);
        if (currentWorld == null) return;
        if (May2BeezQoL.config.worldScannerCHCrystals) {
            for (Map.Entry<String, BlockPos> entry : currentWorld.getCrystalWaypoints().entrySet()) {
                BlockPos blockPos = entry.getValue();
                if (May2BeezQoL.config.espHighlight)
                    RenderUtils.drawBlockBox(blockPos, Color.WHITE, 3);
                if (May2BeezQoL.config.espWaypointText)
                    RenderUtils.drawText(entry.getKey(), blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
                if (May2BeezQoL.config.espBeacon)
                    RenderUtils.renderBeacon(new Vec3(blockPos.getX(), blockPos.getY(), blockPos.getZ()), Color.PINK, event.partialTicks);
            }
        }
        if (May2BeezQoL.config.worldScannerCHMobSpots) {
            for (Map.Entry<String, BlockPos> entry : currentWorld.getMobSpotWaypoints().entrySet()) {
                BlockPos blockPos = entry.getValue();
                if (May2BeezQoL.config.espHighlight)
                    RenderUtils.drawBlockBox(blockPos, Color.RED, 3);
                if (May2BeezQoL.config.espWaypointText)
                    RenderUtils.drawText(entry.getKey(), blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
                if (May2BeezQoL.config.espBeacon)
                    RenderUtils.renderBeacon(new Vec3(blockPos.getX(), blockPos.getY(), blockPos.getZ()), Color.PINK, event.partialTicks);
            }
        }
        if (May2BeezQoL.config.worldScannerCHFairyGrottos) {
            for (BlockPos blockPos : currentWorld.getFairyGrottos()) {
                if (May2BeezQoL.config.espHighlight)
                    RenderUtils.drawBlockBox(blockPos, Color.PINK, 3);
                if (May2BeezQoL.config.espWaypointText)
                    RenderUtils.drawText("§dFairy Grotto", blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
                if (May2BeezQoL.config.espBeacon)
                    RenderUtils.renderBeacon(new Vec3(blockPos.getX(), blockPos.getY(), blockPos.getZ()), Color.PINK, event.partialTicks);
            }
        }
        if (May2BeezQoL.config.worldScannerCHWormFishing) {
            for (BlockPos blockPos : currentWorld.getWormFishing()) {
                if (May2BeezQoL.config.espHighlight)
                    RenderUtils.drawBlockBox(blockPos, Color.ORANGE, 3);
                if (May2BeezQoL.config.espWaypointText)
                    RenderUtils.drawText("§6Worm Fishing", blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
                if (May2BeezQoL.config.espBeacon)
                    RenderUtils.renderBeacon(new Vec3(blockPos.getX(), blockPos.getY(), blockPos.getZ()), Color.PINK, event.partialTicks);
            }
        }
    }

    public static void handleChunkLoad(Chunk chunk, World currentWorld) {
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 256; y++) {
                for (int z = 0; z < 16; z++) {
                    if (May2BeezQoL.config.worldScannerCHCrystals && LocationUtils.currentIsland == LocationUtils.Island.CRYSTAL_HOLLOWS) {
                        // queen
                        if (chunk.getBlock(x, y, z) == Blocks.stone &&
                                chunk.getBlock(x, y + 1, z) == Blocks.log2 &&
                                chunk.getBlock(x, y + 2, z) == Blocks.log2 &&
                                chunk.getBlock(x, y + 3, z) == Blocks.log2 &&
                                chunk.getBlock(x, y + 4, z) == Blocks.log2 &&
                                chunk.getBlock(x, y + 5, z) == Blocks.cauldron) {
                            currentWorld.updateCrystalWaypoints("§6Queen", new BlockPos(chunk.xPosition * 16 + x, y + 5, chunk.zPosition * 16 + z));
                            return;
                        }
                        // divan
                        if (chunk.getBlock(x, y, z) == Blocks.quartz_block && // pillar
                                chunk.getBlock(x, y + 1, z) == Blocks.quartz_stairs &&
                                chunk.getBlock(x, y + 2, z) == Blocks.stone_brick_stairs &&
                                chunk.getBlock(x, y + 3, z) == Blocks.stonebrick) { // chiseled
                            currentWorld.updateCrystalWaypoints("§2Divan", new BlockPos(chunk.xPosition * 16 + x, y + 5, chunk.zPosition * 16 + z));
                            return;
                        }
                        // temple
                        if (chunk.getBlock(x, y, z) == Blocks.bedrock &&
                                chunk.getBlock(x, y + 1, z) == Blocks.clay &&
                                chunk.getBlock(x, y + 2, z) == Blocks.clay &&
                                chunk.getBlock(x, y + 3, z) == Blocks.stained_hardened_clay && // color lime
                                chunk.getBlock(x, y + 4, z) == Blocks.wool && // color green
                                chunk.getBlock(x, y + 5, z) == Blocks.leaves &&
                                chunk.getBlock(x, y + 6, z) == Blocks.leaves) { // oak leaves
                            currentWorld.updateCrystalWaypoints("§5Temple Crystal", new BlockPos(chunk.xPosition * 16 + x + 9, y + 2, chunk.zPosition * 16 + z));
                            currentWorld.updateCrystalWaypoints("§5Temple Door Guardian", new BlockPos(chunk.xPosition * 16 + x - 45, y + 47, chunk.zPosition * 16 + z - 18));
                            return;
                        }
                        // city
                        if (chunk.getBlock(x, y, z) == Blocks.cobblestone &&
                                chunk.getBlock(x, y + 1, z) == Blocks.cobblestone &&
                                chunk.getBlock(x, y + 2, z) == Blocks.cobblestone &&
                                chunk.getBlock(x, y + 3, z) == Blocks.cobblestone &&
                                chunk.getBlock(x, y + 4, z) == Blocks.stone_stairs &&
                                chunk.getBlock(x, y + 5, z) == Blocks.stone && getBlockState(chunk, x, y + 5, z).getValue(BlockStone.VARIANT) == BlockStone.EnumType.ANDESITE_SMOOTH  &&  // smooth andesite
                                chunk.getBlock(x, y + 6, z) == Blocks.stone && getBlockState(chunk, x, y + 5, z).getValue(BlockStone.VARIANT) == BlockStone.EnumType.ANDESITE_SMOOTH  &&  // smooth andesite
                                chunk.getBlock(x, y + 7, z) == Blocks.dark_oak_stairs) {
                            currentWorld.updateCrystalWaypoints("§bCity", new BlockPos(chunk.xPosition * 16 + x + 24, y, chunk.zPosition * 16 + z - 17));
                            return;
                        }
                        // king
                        if (chunk.getBlock(x, y, z) == Blocks.wool && // color red
                                chunk.getBlock(x, y + 1, z) == Blocks.dark_oak_stairs &&
                                chunk.getBlock(x, y + 2, z) == Blocks.dark_oak_stairs &&
                                chunk.getBlock(x, y + 3, z) == Blocks.dark_oak_stairs) {
                            currentWorld.updateCrystalWaypoints("§6King", new BlockPos(chunk.xPosition * 16 + x + 1, y - 1, chunk.zPosition * 16 + z + 2));
                            return;
                        }
                        // balls
                        if (y < 80 && chunk.getBlock(x, y, z) == Blocks.barrier &&
                                chunk.getBlock(x, y + 1, z) == Blocks.barrier &&
                                chunk.getBlock(x, y + 2, z) == Blocks.barrier &&
                                chunk.getBlock(x, y + 3, z) == Blocks.barrier &&
                                chunk.getBlock(x, y + 4, z) == Blocks.barrier &&
                                chunk.getBlock(x, y + 5, z) == Blocks.barrier &&
                                chunk.getBlock(x, y + 6, z) == Blocks.barrier &&
                                chunk.getBlock(x, y + 7, z) == Blocks.barrier &&
                                chunk.getBlock(x, y + 8, z) == Blocks.barrier &&
                                chunk.getBlock(x, y + 9, z) == Blocks.barrier &&
                                chunk.getBlock(x, y + 10, z) == Blocks.barrier &&
                                currentWorld.getCrystalWaypoints().size() == 6) {
                            currentWorld.updateCrystalWaypoints("§6Bal", new BlockPos(chunk.xPosition * 16 + x + 1, y - 1, chunk.zPosition * 16 + z + 2));
                            return;
                        }
                    }

                    if (May2BeezQoL.config.worldScannerCHMobSpots && LocationUtils.currentIsland == LocationUtils.Island.CRYSTAL_HOLLOWS) {
                        // goblin hall
                        if (chunk.getBlock(x, y, z) == Blocks.planks && getBlockState(chunk, x, y, z).getValue(BlockPlanks.VARIANT) == BlockPlanks.EnumType.SPRUCE && // spruce
                                chunk.getBlock(x, y + 2, z) == Blocks.spruce_stairs &&
                                chunk.getBlock(x, y + 3, z) == Blocks.spruce_stairs &&
                                chunk.getBlock(x, y + 6, z) == Blocks.spruce_stairs &&
                                chunk.getBlock(x, y + 7, z) == Blocks.spruce_stairs &&
                                chunk.getBlock(x, y + 10, z) == Blocks.spruce_stairs &&
                                chunk.getBlock(x, y + 11, z) == Blocks.spruce_stairs &&
                                chunk.getBlock(x, y + 13, z) == Blocks.planks && getBlockState(chunk, x, y + 13, z).getValue(BlockPlanks.VARIANT) == BlockPlanks.EnumType.SPRUCE) { // spruce
                            currentWorld.updateMobSpotWaypoints("§6Goblin Hall", new BlockPos(chunk.xPosition * 16 + x, y + 7, chunk.zPosition * 16 + z));
                            return;
                        }
                        // grunt bridge
                        if (chunk.getBlock(x, y, z) == Blocks.stone_brick_stairs &&
                                chunk.getBlock(x, y + 5, z) == Blocks.stonebrick &&
                                chunk.getBlock(x, y + 6, z) == Blocks.stonebrick &&
                                chunk.getBlock(x, y + 8, z) == Blocks.stone_slab && // stone brick slab
                                chunk.getBlock(x, y + 9, z) == Blocks.stonebrick &&
                                chunk.getBlock(x, y + 13, z) == Blocks.stonebrick &&
                                chunk.getBlock(x, y + 14, z) == Blocks.stone_slab) { // stone brick slab
                            currentWorld.updateMobSpotWaypoints("§bGrunt Bridge", new BlockPos(chunk.xPosition * 16 + x, y - 1, chunk.zPosition * 16 + z - 45));
                            return;
                        }
                    }

                    if (May2BeezQoL.config.worldScannerCHFairyGrottos && LocationUtils.currentIsland == LocationUtils.Island.CRYSTAL_HOLLOWS) {
                        if (chunk.getBlock(x, y, z) == Blocks.stained_glass && getBlockState(chunk, x, y, z).getValue(BlockColored.COLOR) == EnumDyeColor.MAGENTA) {
                            currentWorld.updateFairyGrottos(new BlockPos(chunk.xPosition * 16 + x, y, chunk.zPosition * 16 + z));
                            return;
                        }
                    }

                    if (May2BeezQoL.config.worldScannerCHWormFishing && LocationUtils.currentIsland == LocationUtils.Island.CRYSTAL_HOLLOWS) {
                        if ((chunk.xPosition * 16 + x >= 564 && chunk.zPosition * 16 + z >= 513) || (chunk.xPosition * 16 + x >= 513 && chunk.zPosition * 16 + z >= 564)) {
                            if (y > 63 &&
                                    (chunk.getBlock(x, y, z) == Blocks.lava || chunk.getBlock(x, y, z) == Blocks.flowing_lava) &&
                                    (chunk.getBlock(x, y + 1, z) != Blocks.lava && chunk.getBlock(x, y + 1, z) != Blocks.flowing_lava)) {
                                currentWorld.updateWormFishing(new BlockPos(chunk.xPosition * 16 + x, y, chunk.zPosition * 16 + z));
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private static IBlockState getBlockState(Chunk chunk, int x, int y, int z) {
        ExtendedBlockStorage extendedblockstorage;
        IBlockState iBlockState = Blocks.air.getDefaultState();
        if (y >= 0 && y >> 4 < chunk.getBlockStorageArray().length && (extendedblockstorage = chunk.getBlockStorageArray()[y >> 4]) != null) {
            try {
                iBlockState = extendedblockstorage.get(x, y & 0xF, z);
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Getting block");
                throw new ReportedException(crashreport);
            }
        }
        return iBlockState;
    }
}
