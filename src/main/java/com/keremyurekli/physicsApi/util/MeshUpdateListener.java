package com.keremyurekli.physicsApi.util;

import com.keremyurekli.physicsApi.PhysicsApi;
import com.keremyurekli.physicsApi.helpers.Box3DChunkMesher;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.util.Vector;

public class MeshUpdateListener implements Listener {


//    @EventHandler
//    public void onChunkLoad(ChunkLoadEvent event)
//    {
//        if (PhysicsApi.bridges.containsKey(event.getWorld())) {
//            PhysicsApi.bridges.get(event.getWorld()).chunkMesher.snapshotAsync(new Chunk[]{event.getChunk()});
//        }
//
////    / /        MinecraftBox3d.interfaces.get(event.getWorld())
//    }
//
//
//    @EventHandler
//    public void onBlockBreak(BlockBreakEvent event) {
//        if (PhysicsApi.bridges.containsKey(event.getBlock().getWorld())) {
//            PhysicsApi.bridges.get(event.getBlock().getWorld()).removeStaticBlock(event.getBlock().getLocation());
//
//
//            for (Block neighbor : Box3DChunkMesher.getDirectNeighbors(event.getBlock())) {
//                if (Box3DChunkMesher.isPassable(neighbor.getType())) continue;
//                PhysicsApi.bridges.get(neighbor.getWorld()).creationHelper.spawnStaticBlockRectangle(new Vector((float) neighbor.getLocation().getX() + 0.5f,
//                        (float) neighbor.getLocation().getY() + 0.5f
//                        , (float) neighbor.getLocation().getZ() + 0.5f), new Vector(1, 1, 1), 0.8f, neighbor.getType(), false);
//            }
//        }
//    }
//
//
//    @EventHandler
//    public void onBlockPlace(BlockPlaceEvent event) {
//        if (PhysicsApi.bridges.containsKey(event.getBlock().getWorld())) {
////            if (event.getBlock().isPassable() || event.getBlock().isLiquid()) return;
//            if (Box3DChunkMesher.isPassable(event.getBlock().getType())) return;
//            PhysicsApi.bridges.get(event.getBlock().getWorld()).creationHelper.spawnStaticBlockRectangle(new Vector((float) event.getBlock().getLocation().getX() + 0.5f,
//                    (float) event.getBlock().getLocation().getY() + 0.5f
//                    , (float) event.getBlock().getLocation().getZ() + 0.5f), new Vector(1, 1, 1), 0.8f, event.getBlock().getType(), false);
//
////            for (Block neighbor : Box3DChunkUtils.getDirectNeighbors(event.getBlock())) {
////                if (!neighbor.isPassable() && !neighbor.isLiquid()) continue;
////                PhysicsApi.bridges.get(neighbor.getWorld()).removeStaticBlock(neighbor.getLocation());
////            }
//        }
//    }




}
