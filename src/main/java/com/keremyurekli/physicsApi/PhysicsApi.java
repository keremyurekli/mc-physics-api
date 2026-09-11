package com.keremyurekli.physicsApi;

import com.keremyurekli.physicsApi.util.*;
import org.bukkit.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PhysicsApi extends JavaPlugin {
    public static Plugin PLUGIN;

    public static BukkitTask box3dTask;
    public static BukkitTask renderUpdateTask;
    public static BukkitTask updateEntitiesTask;
    public static NamespacedKey gravityGunKey;
    public static HashMap<World, Box3DBridge> bridges = new HashMap<>();
    private static Logger logger;


    public static Box3DBridge getBridge(World world) {
        if (world == null) return null;
        return bridges.computeIfAbsent(world, w -> {
            Box3DBridge bridge = new Box3DBridge();
            bridge.init(w);
            return bridge;
        });
    }

    public static void log(Level level, String s) {
        logger.log(level, s);
    }

    @Override
    public void onEnable() {
        PLUGIN = this;
        logger = getLogger();
        //§
        log(Level.INFO, "Trying to load box3d native!");
        Box3DLoader.load();


//        log(Level.INFO, "Registering events!");

//        getServer().getPluginManager().registerEvents(new MeshUpdateListener(), this);

        setupBox3d();

        //fun part
//        gravityGunKey = new NamespacedKey(this, "gravitygun");
//        getServer().getPluginManager().registerEvents(new GravityGunEvents(), this);
//        getServer().getPluginManager().registerEvents(new ExampleCollisionEventListener(), this);

    }


    public static void setupBox3d() {


        log(Level.INFO, "Initializing the bridges!");


        Bukkit.getWorlds().forEach(world -> {
            Box3DBridge bridge = new Box3DBridge();
            bridge.init(world);
            bridges.put(world, bridge);
        });


        //maybe per world tasking??
        box3dTask = Bukkit.getScheduler().runTaskTimer(PLUGIN, () -> {
            bridges.keySet().forEach(w -> {
                bridges.get(w).onServerTick();
            });
        }, 0L, 1L);

        renderUpdateTask = Bukkit.getScheduler().runTaskTimer(PLUGIN, () -> {
            bridges.keySet().forEach(w -> {
                bridges.get(w).renderWork();
            });
        }, 0L, 1L);

        updateEntitiesTask = Bukkit.getScheduler().runTaskTimer(PLUGIN, () -> {
            bridges.keySet().forEach(w -> {
                bridges.get(w).updateEntityPositions();
            });
        }, 100L, 100L);

//        GravityGunEvents.startTask();
    }

    public static void killBox3d() {
        B3Object.stopAllVisualizers();
//        GravityGunEvents.cleanup();
        if (box3dTask != null) box3dTask.cancel();
        if (renderUpdateTask != null) renderUpdateTask.cancel();
        if (updateEntitiesTask != null) updateEntitiesTask.cancel();
        bridges.values().forEach(Box3DBridge::cleanup);
        bridges.clear();
    }

    @Override
    public void onDisable() {
        killBox3d();
    }

    //fun part
//    public static ItemStack createGravityGun() {
//        ItemStack item = new ItemStack(Material.ECHO_SHARD);
//
//        ItemMeta meta = item.getItemMeta();
//        if (meta != null) {
//            meta.setDisplayName("§b§lGravity Gun");
//
//            meta.setLore(List.of(
//                    "§8▸ §3[RMB] §fHold to Grab",
//                    "§8▸ §3[LMB] §fLaunch / Throw",
//                    "§8▸ §3[Shift + Scroll] §7Change Distance"
//            ));
//
//
//            PersistentDataContainer data = meta.getPersistentDataContainer();
//            data.set(gravityGunKey, PersistentDataType.BOOLEAN, true);
//
//            item.setItemMeta(meta);
//        }
//
//        return item;
//    }
}
