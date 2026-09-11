package com.keremyurekli.physicsApi.util;

import com.keremyurekli.physicsApi.Box3DBridge;
import com.keremyurekli.physicsApi.PhysicsApi;
import org.box3d.Box3D;
import org.box3d.b3Quat;
import org.box3d.b3Vec3;
import org.bukkit.*;
import org.bukkit.entity.Display;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class B3Object {

    private final MemorySegment bodyId;
    private final MemorySegment shapeId;
    private final float halfX;
    private final float halfY;
    private final float halfZ;
    private final @Nullable Display visualizer;
    private final @Nullable Material material;
    private final World world;

    public B3Object(
        MemorySegment bodyId,
        MemorySegment shapeId,
        float halfX,
        float halfY,
        float halfZ,
        @Nullable Display visualizer,
        @Nullable Material material,
        World world
    ) {
        this.bodyId = bodyId;
        this.shapeId = shapeId;
        this.halfX = halfX;
        this.halfY = halfY;
        this.halfZ = halfZ;
        this.visualizer = visualizer;
        this.material = material;
        this.world = world;
    }

    public Vector extraSizeInfo;
    public Vector extraPosInfo;

    public List<Display> extraDisplays = new ArrayList<>();
    public List<Quaternionf> displayLocalRotations = new ArrayList<>();

    public MemorySegment bodyId() {
        return bodyId;
    }

    public MemorySegment shapeId() {
        return shapeId;
    }

    public float halfX() {
        return halfX;
    }

    public float halfY() {
        return halfY;
    }

    public float halfZ() {
        return halfZ;
    }

    public @Nullable Display visualizer() {
        return visualizer;
    }

    public @Nullable Material material() {
        return material;
    }

    public World world() {
        return world;
    }

    public MemorySegment getBodyId() {
        return bodyId;
    }

    public MemorySegment getShapeId() {
        return shapeId;
    }

    public float getHalfX() {
        return halfX;
    }

    public float getHalfY() {
        return halfY;
    }

    public float getHalfZ() {
        return halfZ;
    }

    public @Nullable Display getVisualizer() {
        return visualizer;
    }

    public @Nullable Material getMaterial() {
        return material;
    }

    public World getWorld() {
        return world;
    }

    public long bodyKey() {
        return bodyId.get(java.lang.foreign.ValueLayout.JAVA_LONG, 0);
    }
    public long shapeKey() {
        return shapeId.get(java.lang.foreign.ValueLayout.JAVA_LONG, 0);
    }


    private static final Map<Long, BukkitTask> ACTIVE_TASKS = new ConcurrentHashMap<>();
    public static void stopAllVisualizers() {
        ACTIVE_TASKS.values().forEach(BukkitTask::cancel);
        ACTIVE_TASKS.clear();
    }
    public void startVisualizing() {
        stopVisualizing();
        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(0, 255, 255), 0.8f);
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(PhysicsApi.PLUGIN, () -> {
            // ⭐ SAFETY CHECK: Stop if world was unloaded or destroyed
            Box3DBridge bridge = PhysicsApi.bridges.get(world);
            if (bridge == null || bridge.worldId == null) {
                stopVisualizing();
                return;
            }
            try (Arena temp = Arena.ofConfined()) {
                MemorySegment pSeg = Box3D.b3Body_GetPosition(temp, bodyId);
                Vector3f center = new Vector3f(b3Vec3.x(pSeg), b3Vec3.y(pSeg), b3Vec3.z(pSeg));
                MemorySegment rSeg = Box3D.b3Body_GetRotation(temp, bodyId);
                MemorySegment v = b3Quat.v(rSeg);
                Quaternionf rot = new Quaternionf(b3Vec3.x(v), b3Vec3.y(v), b3Vec3.z(v), b3Quat.s(rSeg));
                Vector3f[] c = new Vector3f[8];
                int idx = 0;
                for (int sx : new int[]{-1, 1}) {
                    for (int sy : new int[]{-1, 1}) {
                        for (int sz : new int[]{-1, 1}) {
                            Vector3f corner = new Vector3f(sx * halfX, sy * halfY, sz * halfZ);
                            corner.rotate(rot);
                            corner.add(center);
                            c[idx++] = corner;
                        }
                    }
                }
                drawEdge(c[0], c[1], dust);
                drawEdge(c[0], c[2], dust);
                drawEdge(c[3], c[1], dust);
                drawEdge(c[3], c[2], dust);
                drawEdge(c[4], c[5], dust);
                drawEdge(c[4], c[6], dust);
                drawEdge(c[7], c[5], dust);
                drawEdge(c[7], c[6], dust);
                drawEdge(c[0], c[4], dust);
                drawEdge(c[1], c[5], dust);
                drawEdge(c[2], c[6], dust);
                drawEdge(c[3], c[7], dust);
            }
        }, 0L, 2L);
        ACTIVE_TASKS.put(bodyKey(), task);
    }
    /**
     * Helper to spawn interpolated particles along a line between point A and point B.
     */
    private void drawEdge(Vector3f p1, Vector3f p2, Particle.DustOptions dust) {
        float distance = p1.distance(p2);
        int steps = Math.max(2, (int) (distance * 3.5f)); // Density of particles
        for (int i = 0; i <= steps; i++) {
            float t = (float) i / steps;
            double x = p1.x + (p2.x - p1.x) * t;
            double y = p1.y + (p2.y - p1.y) * t;
            double z = p1.z + (p2.z - p1.z) * t;
            world.spawnParticle(Particle.DUST, x, y, z, 1, 0, 0, 0, 0, dust);
        }
    }
    /**
     * Stops the particle visualization task for this body.
     */
    public void stopVisualizing() {
        BukkitTask task = ACTIVE_TASKS.remove(bodyKey());
        if (task != null) {
            task.cancel();
        }
    }

    public Vector pos() {
        try (Arena tempArena = Arena.ofConfined()) {
            MemorySegment pos = Box3D.b3Body_GetPosition(tempArena, bodyId);
            float bx = b3Vec3.x(pos);
            float by = b3Vec3.y(pos);
            float bz = b3Vec3.z(pos);
            return new Vector(bx, by, bz);
        }
    }

    public void destroy() {
        stopVisualizing();

        if (visualizer != null && visualizer.isValid()) {
            visualizer.remove();
        }
        if (!extraDisplays.isEmpty()) {
            extraDisplays.forEach(display -> {
                display.remove();
            });
            extraDisplays.clear();
            displayLocalRotations.clear();
        }

        Box3DBridge bridge = PhysicsApi.getBridge(world);
        if (bridge != null) {
            bridge.render.remove(this);
            bridge.dynamicBlocks.remove(shapeKey());
            bridge.allShapes.remove(shapeKey());

            if (bridge.worldId != null && bridge.worldArena.scope().isAlive()) {
                Vector currentPos = pos(); // Read position BEFORE destroying body!
                bridge.staticBlocks.remove(currentPos);
                Box3D.b3DestroyBody(bodyId);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof B3Object b3Object)) return false;
        return bodyKey() == b3Object.bodyKey() && shapeKey() == b3Object.shapeKey();
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(bodyKey(), shapeKey());
    }

    @Override
    public String toString() {
        return "B3Object{" +
                "bodyKey=" + bodyKey() +
                ", shapeKey=" + shapeKey() +
                ", halfX=" + halfX +
                ", halfY=" + halfY +
                ", halfZ=" + halfZ +
                ", material=" + material +
                ", world=" + (world != null ? world.getName() : "null") +
                '}';
    }
}