package com.shane.raspberryjuicereload.util;

import com.shane.raspberryjuicereload.RaspberryJuiceReload;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;

public class BlockUtil {
    private static class CuboidRegion {
        private final World world;
        private final int minX, maxX;
        private final int minY, maxY;
        private final int minZ, maxZ;

        public CuboidRegion(Location pos1, Location pos2) {
            if (pos1 == null || pos2 == null) {
                throw new IllegalArgumentException("位置不能為空");
            }
            if (pos1.getWorld() != pos2.getWorld()) {
                throw new IllegalArgumentException("兩個位置必須在同一個世界");
            }

            this.world = pos1.getWorld();
            this.minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
            this.maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
            this.minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
            this.maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
            this.minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
            this.maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        }
    }

    private static String blockDataInit(String blockData) {
        if (blockData == null) return null;
        return blockData.replace('|', ',');
    }

    public static void setCuboid(Location pos1, Location pos2, String blockType, String blockData) {
        CuboidRegion region = new CuboidRegion(pos1, pos2);
        for (int x = region.minX; x <= region.maxX; ++x) {
            for (int z = region.minZ; z <= region.maxZ; ++z) {
                for (int y = region.minY; y <= region.maxY; ++y) {
                    updateBlock(region.world, new Location(region.world, x, y, z), blockType, blockData);
                }
            }
        }
    }

    public static String getBlocks(Location pos1, Location pos2) {
        StringBuilder blockData = new StringBuilder();
        CuboidRegion region = new CuboidRegion(pos1, pos2);
        for (int x = region.minX; x <= region.maxX; ++x)
            for (int z = region.minZ; z <= region.maxZ; ++z)
                for (int y = region.minY; y <= region.maxY; ++y)
                    blockData.append(x).append(' ').append(y).append(' ').append(z).append(':').append(region.world.getBlockAt(x, y, z).getType()).append(';');
        return !blockData.isEmpty() ? blockData.substring(0, blockData.length() - 1) : "";
    }

    public static String getBlocksWithData(Location pos1, Location pos2) {
        StringBuilder builder = new StringBuilder();
        CuboidRegion region = new CuboidRegion(pos1, pos2);
        for (int x = region.minX; x <= region.maxX; ++x) {
            for (int z = region.minZ; z <= region.maxZ; ++z) {
                for (int y = region.minY; y <= region.maxY; ++y) {
                    BlockData block = region.world.getBlockData(x, y, z);
                    builder.append(x - region.minX).append(' ');
                    builder.append(y - region.minY).append(' ');
                    builder.append(z - region.minZ).append(": ");
                    builder.append(block.getMaterial()).append(", ");
                    builder.append(block.getAsString().replace(block.getMaterial().getKey().toString(), "")).append(';');
                }
            }
        }
        return !builder.isEmpty() ? builder.substring(0, builder.length() - 1) : "";
    }

    public static void updateBlock(World world, Location loc, String blockTypeStr, String blockDataStr) {
        Block thisBlock = world.getBlockAt(loc);
        Material blockType = Material.valueOf(blockTypeStr);
        if (thisBlock.getType() == blockType) return;
        thisBlock.setType(blockType);
        if (blockDataStr == null || blockDataStr.isEmpty()) return;
        BlockData data = Bukkit.createBlockData(blockType, blockDataInit(blockDataStr));
        thisBlock.setBlockData(data);
    }

    public static void replaceBlock(World world, Location loc, String oldBlock, String newBlock, String newBlockData) {
        Block thisBlock = world.getBlockAt(loc);
        Material oldBlockType = thisBlock.getType(), newBlockType = Material.valueOf(newBlock);
        if (oldBlockType != Material.valueOf(oldBlock)) return;
        thisBlock.setType(newBlockType);
        if (newBlockData.isEmpty()) return;
        BlockData data = Bukkit.createBlockData(newBlockType, newBlockData);
        thisBlock.setBlockData(data);
    }

    public static int blockFaceToNotch(BlockFace blockFace) {
        // 轉換BlockFace到數值
        return switch (blockFace) {
            case DOWN -> 0;
            case UP -> 1;
            case NORTH -> 2;
            case SOUTH -> 3;
            case WEST -> 4;
            case EAST -> 5;
            default -> 7;
        };
    }
}
