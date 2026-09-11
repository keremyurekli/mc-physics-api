package com.keremyurekli.physicsApi.deprecated;

import com.keremyurekli.physicsApi.Box3DBridge;
import org.bukkit.World;

public class Box3DChunkUtils {



    /** Deprecated, use Box3DChunkMesher.java*/

    Box3DBridge minecraftInterface;

    World world;

    public Box3DChunkUtils(Box3DBridge minecraftInterface) {
        this.minecraftInterface = minecraftInterface;
        this.world = this.minecraftInterface.world;
    }

//    public void registerBlock(int x, int y, int z, Material mat) {
//        minecraftInterface.spawnStaticRectangle(x + 0.5f, y + 0.5f, z + 0.5f, 1, 1, 1, mat, false);//its a cube, we are in minecraft
//    }


//    public void snapshot(Chunk[] chunks) {
//        Map<Vector2i, ChunkSnapshot> snapshotMap = new HashMap<>();
//        for (Chunk chunk : chunks) {
//            snapshotMap.put(new Vector2i(chunk.getX(), chunk.getZ()), chunk.getChunkSnapshot(true, false, false));
//        }
//        int minY = world.getMinHeight();
//        int maxY = world.getMaxHeight();
//        // 2. Loop through each chunk snapshot
//        for (ChunkSnapshot snapshot : snapshotMap.values()) {
//            int cx = snapshot.getX();
//            int cz = snapshot.getZ();
//            int chunkWorldX = cx * 16;
//            int chunkWorldZ = cz * 16;
//            for (int x = 0; x < 16; x++) {
//                for (int z = 0; z < 16; z++) {
//                    int highestY = snapshot.getHighestBlockYAt(x, z);
//                    for (int y = minY; y <= highestY; y++) {
//                        Material material = snapshot.getBlockType(x, y, z);
//                        if (material.isAir()) continue;
//                        if (isBlockExposed(snapshot, snapshotMap, x, y, z, minY, maxY)) {
//                            registerBlock(chunkWorldX + x, y, chunkWorldZ + z, material);
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//
//    public static final BlockFace[] FACES_6 = {
//            BlockFace.UP,
//            BlockFace.DOWN,
//            BlockFace.NORTH,
//            BlockFace.SOUTH,
//            BlockFace.EAST,
//            BlockFace.WEST
//    };
//
//
//    private static boolean isPassable(Material mat) {
//        return !mat.isOccluding() || mat.isAir() || !mat.isSolid();
//    }
//
//    public static Block[] getDirectNeighbors(Block block) {
//        Block[] neighbors = new Block[6];
//        for (int i = 0; i < 6; i++) {
//            neighbors[i] = block.getRelative(FACES_6[i]);
//        }
//        return neighbors;
//    }
//
//    public static boolean isBlockExposed(
//            ChunkSnapshot currentSnapshot,
//            Map<Vector2i, ChunkSnapshot> snapshotMap,
//            int x, int y, int z,
//            int minY, int maxY
//    ) {
//        int cx = currentSnapshot.getX();
//        int cz = currentSnapshot.getZ();
//        for (BlockFace face : FACES_6) {
//            int ny = y + face.getModY();
//            if (ny < minY || ny >= maxY) {
//                return true; // sky or void
//            }
//            int nx = x + face.getModX();
//            int nz = z + face.getModZ();
//            int targetChunkX = cx;
//            int targetChunkZ = cz;
//            int localX = nx;
//            int localZ = nz;
//            // If we stepped out of the current chunk, move to the neighbor chunk
//            if (nx < 0) {
//                targetChunkX = cx - 1;
//                localX = 15; // Last block of left chunk
//            } else if (nx > 15) {
//                targetChunkX = cx + 1;
//                localX = 0;  // First block of right chunk
//            }
//            if (nz < 0) {
//                targetChunkZ = cz - 1;
//                localZ = 15; // Last block of bottom chunk
//            } else if (nz > 15) {
//                targetChunkZ = cz + 1;
//                localZ = 0;  // First block of top chunk
//            }
//
//            ChunkSnapshot targetSnapshot = snapshotMap.get(new Vector2i(targetChunkX, targetChunkZ));
//
//            if (targetSnapshot == null) {
//                continue;
//            }
//            Material neighborMat = targetSnapshot.getBlockType(localX, ny, localZ);
//            if (isPassable(neighborMat)) {
//                return true;
//            }
//        }
//        return false;
//    }
////    public void snapshotSingleChunk(Chunk chunk) {
////        ChunkSnapshot snapshot = chunk.getChunkSnapshot();
////        int minY = world.getMinHeight();
////        int maxY = world.getMaxHeight();
////
////        for (int x = 0; x < 16; x++) {
////            for (int z = 0; z < 16; z++) {
////                int highestY = snapshot.getHighestBlockYAt(x, z);
////                for (int y = minY; y <= highestY; y++) {
////                    Material material = snapshot.getBlockType(x, y, z);
////                    if (material.isAir()) continue;
////                    if (b3Utils.isBlockExposed(snapshot, x, y, z, minY, maxY)) {
////                        int worldX = (snapshot.getX() << 4) + x;
////                        int worldZ = (snapshot.getZ() << 4) + z;
////
////                        registerBlock(worldX,y,worldZ);
////
////                    }
////                }
////
////            }
////        }
////
////
////    }


}
