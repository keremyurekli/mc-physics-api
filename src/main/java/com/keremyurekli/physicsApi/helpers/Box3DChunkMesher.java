package com.keremyurekli.physicsApi.helpers;

import com.keremyurekli.physicsApi.Box3DBridge;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Box3DChunkMesher {

    private final Box3DBridge bridge;
    private final World world;
    private final Plugin plugin;

    // ⭐ Thread-safe set of packed chunk keys (cx, cz) that are already meshed
    private final Set<Long> meshedChunks = ConcurrentHashMap.newKeySet();

    public record ExposedBlock(int x, int y, int z, Material material) {}

    public Box3DChunkMesher(Box3DBridge bridge, World world, Plugin plugin) {
        this.bridge = bridge;
        this.world = world;
        this.plugin = plugin;
    }

    public static long packChunkKey(int cx, int cz) {
        return (((long) cx) << 32) | (cz & 0xFFFFFFFFL);
    }

    public boolean isChunkMeshed(int cx, int cz) {
        return meshedChunks.contains(packChunkKey(cx, cz));
    }

    public boolean isChunkMeshed(Chunk chunk) {
        return isChunkMeshed(chunk.getX(), chunk.getZ());
    }

    /**
     * Call this when a chunk unloads if you want to allow it to be re-meshed later
     */
    public void unmeshChunk(int cx, int cz) {
        meshedChunks.remove(packChunkKey(cx, cz));
    }

    public void unmeshChunk(Chunk chunk) {
        unmeshChunk(chunk.getX(), chunk.getZ());
    }

    public void clearMeshedChunks() {
        meshedChunks.clear();
    }

    /**
     * 1. Synchronous Mesh:
     */
    public void snapshotSync(Chunk[] chunks) {
        // Filter out already meshed chunks for processing, but keep all snapshots for neighbor borders
        List<Chunk> unmeshedChunks = new ArrayList<>();
        Map<Long, ChunkSnapshot> snapshotMap = new HashMap<>(chunks.length * 2);

        for (Chunk chunk : chunks) {
            long key = packChunkKey(chunk.getX(), chunk.getZ());
            snapshotMap.put(key, chunk.getChunkSnapshot(true, false, false));

            if (!meshedChunks.contains(key)) {
                unmeshedChunks.add(chunk);
                meshedChunks.add(key); // Mark as meshed
            }
        }

        if (unmeshedChunks.isEmpty()) {
            return; // All chunks already meshed!
        }

        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight();

        // 1. Scan only the unmeshed chunks in parallel
        List<ExposedBlock> exposedBlocks = CompletableFuture.supplyAsync(() -> {
            return unmeshedChunks.parallelStream()
                    .map(chunk -> snapshotMap.get(packChunkKey(chunk.getX(), chunk.getZ())))
                    .filter(Objects::nonNull)
                    .flatMap(snapshot -> scanChunk(snapshot, snapshotMap, minY, maxY).stream())
                    .collect(Collectors.toList());
        }).join();

        // 2. Register in Box3D safely on the Main Thread
        for (ExposedBlock b : exposedBlocks) {
            bridge.creationHelper.spawnStaticBlockRectangle(
                    new Vector(b.x + 0.5f, b.y + 0.5f, b.z + 0.5f),
                    new Vector(1.0f, 1.0f, 1.0f),
                    0.8f,
                    b.material,
                    false
            );
        }
    }

    /**
     * 2. Asynchronous Mesh:
     */
    public void snapshotAsync(Chunk[] chunks) {
        List<Chunk> unmeshedChunks = new ArrayList<>();
        Map<Long, ChunkSnapshot> snapshotMap = new HashMap<>(chunks.length * 2);

        for (Chunk chunk : chunks) {
            long key = packChunkKey(chunk.getX(), chunk.getZ());
            snapshotMap.put(key, chunk.getChunkSnapshot(true, false, false));

            if (!meshedChunks.contains(key)) {
                unmeshedChunks.add(chunk);
                meshedChunks.add(key); // Mark as meshed
            }
        }

        if (unmeshedChunks.isEmpty()) {
            return; // All chunks already meshed!
        }

        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight();

        // Step 1: Background thread scan
        CompletableFuture.supplyAsync(() -> {
            return unmeshedChunks.parallelStream()
                    .map(chunk -> snapshotMap.get(packChunkKey(chunk.getX(), chunk.getZ())))
                    .filter(Objects::nonNull)
                    .flatMap(snapshot -> scanChunk(snapshot, snapshotMap, minY, maxY).stream())
                    .collect(Collectors.toList());
        }).thenAccept(exposedBlocks -> {
            // ⭐ Step 2: POST BACK TO MAIN THREAD!
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (ExposedBlock b : exposedBlocks) {
                    bridge.creationHelper.spawnStaticBlockRectangle(
                            new Vector(b.x + 0.5f, b.y + 0.5f, b.z + 0.5f),
                            new Vector(1.0f, 1.0f, 1.0f),
                            0.8f,
                            b.material,
                            false
                    );
                }
            });
        });
    }

    private static List<ExposedBlock> scanChunk(
            ChunkSnapshot snapshot,
            Map<Long, ChunkSnapshot> snapshotMap,
            int minY, int maxY
    ) {
        List<ExposedBlock> exposed = new ArrayList<>(1024);
        int cx = snapshot.getX();
        int cz = snapshot.getZ();
        int chunkWorldX = cx * 16;
        int chunkWorldZ = cz * 16;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int highestY = snapshot.getHighestBlockYAt(x, z);

                for (int y = minY; y <= highestY; y++) {
                    Material mat = snapshot.getBlockType(x, y, z);
                    if (isPassable(mat)) {
                        continue;
                    }

                    if (isBlockExposedFast(snapshot, snapshotMap, cx, cz, x, y, z, minY, maxY)) {
                        exposed.add(new ExposedBlock(chunkWorldX + x, y, chunkWorldZ + z, mat));
                    }
                }
            }
        }
        return exposed;
    }

    private static boolean isBlockExposedFast(
            ChunkSnapshot currentSnapshot,
            Map<Long, ChunkSnapshot> snapshotMap,
            int cx, int cz,
            int x, int y, int z,
            int minY, int maxY
    ) {
        if (y + 1 >= maxY || isPassable(currentSnapshot.getBlockType(x, y + 1, z))) return true;
        if (y - 1 < minY  || isPassable(currentSnapshot.getBlockType(x, y - 1, z))) return true;

        if (x < 15) {
            if (isPassable(currentSnapshot.getBlockType(x + 1, y, z))) return true;
        } else {
            ChunkSnapshot east = snapshotMap.get(packChunkKey(cx + 1, cz));
            if (east != null && isPassable(east.getBlockType(0, y, z))) return true;
        }

        if (x > 0) {
            if (isPassable(currentSnapshot.getBlockType(x - 1, y, z))) return true;
        } else {
            ChunkSnapshot west = snapshotMap.get(packChunkKey(cx - 1, cz));
            if (west != null && isPassable(west.getBlockType(15, y, z))) return true;
        }

        if (z < 15) {
            if (isPassable(currentSnapshot.getBlockType(x, y, z + 1))) return true;
        } else {
            ChunkSnapshot south = snapshotMap.get(packChunkKey(cx, cz + 1));
            if (south != null && isPassable(south.getBlockType(x, y, 0))) return true;
        }

        if (z > 0) {
            if (isPassable(currentSnapshot.getBlockType(x, y, z - 1))) return true;
        } else {
            ChunkSnapshot north = snapshotMap.get(packChunkKey(cx, cz - 1));
            if (north != null && isPassable(north.getBlockType(x, y, 15))) return true;
        }

        return false;
    }

    public static boolean isPassable(Material mat) {
        return mat == null || mat.isAir() || mat == Material.WATER || mat == Material.LAVA ||
                mat == Material.SHORT_GRASS || mat == Material.TALL_GRASS ||
                mat == Material.SEAGRASS || mat == Material.SNOW || mat == Material.CAVE_AIR || mat == Material.VOID_AIR;
    }

    public static final BlockFace[] FACES_6 = {
            BlockFace.UP,
            BlockFace.DOWN,
            BlockFace.NORTH,
            BlockFace.SOUTH,
            BlockFace.EAST,
            BlockFace.WEST
    };

    public static Block[] getDirectNeighbors(Block block) {
        Block[] neighbors = new Block[6];
        for (int i = 0; i < 6; i++) {
            neighbors[i] = block.getRelative(FACES_6[i]);
        }
        return neighbors;
    }
}