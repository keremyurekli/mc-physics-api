package com.keremyurekli.physicsApi.helpers;

import com.keremyurekli.physicsApi.util.B3Object;
import com.keremyurekli.physicsApi.Box3DBridge;
import org.box3d.*;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.joml.Quaternionf;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;

public class Box3DCreationHelper {

    private final Box3DBridge bridge;
    private final World world;
    private final Plugin plugin;

    private final Arena worldArena;
    private final MemorySegment worldId;

    public Box3DCreationHelper(Box3DBridge bridge, World world, Plugin plugin) {
        this.bridge = bridge;
        this.world = world;
        this.plugin = plugin;
        this.worldArena = bridge.worldArena;
        this.worldId = bridge.worldId;
    }

//
//    public B3Object spawnStaticRectangle(float x, float y, float z, float sizex, float sizey, float sizez, @Nullable Material material, boolean visualize) {
//        Vector ps = new Vector(x, y, z);
//        if (bridge.staticBlocks.containsKey(ps)) {
//            return null;
//        }
//        try (Arena tempArena = Arena.ofConfined()) {
//            MemorySegment bodyDef = Box3D.b3DefaultBodyDef(tempArena);
//            b3BodyDef.type(bodyDef, Box3D.b3_staticBody());
//
//            MemorySegment pos = b3BodyDef.position(bodyDef);
////            b3Vec3.x(pos, x + 0.5f);
////            b3Vec3.y(pos, y + 0.5f);
////            b3Vec3.z(pos, z + 0.5f);
//
//            b3Vec3.x(pos, x);
//            b3Vec3.y(pos, y);
//            b3Vec3.z(pos, z);
//            MemorySegment bodyId = Box3D.b3CreateBody(worldArena, worldId, bodyDef);
//
//            float halfX = sizex * 0.5f;
//            float halfY = sizey * 0.5f;
//            float halfZ = sizez * 0.5f;
//
//            MemorySegment hull = Box3D.b3MakeBoxHull(tempArena, halfX, halfY, halfZ);
//            MemorySegment shapeDef = Box3D.b3DefaultShapeDef(tempArena);
//
//            MemorySegment shapeId = Box3D.b3CreateHullShape(worldArena, bodyId, shapeDef, hull);
//            Box3D.b3Shape_EnableHitEvents(shapeId,true);
//
//            Display display = null;
//            if (material != null && visualize) {
//                display = Box3DBasicUtils.spawnDynamicBlockVisualizer(world, new Location(world, x, y, z), sizex, sizey, sizez, material);
//            }
//
//            B3Object b3obj = new B3Object(bodyId, shapeId, halfX, halfY, halfZ, display, material, world);
//            bridge.staticBlocks.put(ps, b3obj);
//
//            return b3obj;
//        }
//    }

    /**
     * Spawns a static, unmovable physical box collider in the physics world (Center-Oriented).
     *
     * @param pos The center position (X, Y, Z) of the static box in world coordinates
     * @param size The full dimensions (sizeX, sizeY, sizeZ) of the box in blocks/meters
     * @param friction Surface friction (e.g. 0.6 for normal blocks, 0.1 for ice)
     * @param material Optional Bukkit Material for sound effects and visuals
     * @param visualize If {@code true}, spawns a client-side BlockDisplay entity for this static box
     * @return The created {@link B3Object}, or {@code null} if a static box already exists at this exact position
     */
    public B3Object spawnStaticBlockRectangle(
            Vector pos,
            Vector size,
            float friction,
            @Nullable Material material,
            boolean visualize
    ) {
        if (bridge.staticBlocks.containsKey(pos)) {
            return null; // Prevent duplicate static colliders at the same position
        }
        try (Arena tempArena = Arena.ofConfined()) {
            // 1. Create Static Body
            MemorySegment bodyDef = Box3D.b3DefaultBodyDef(tempArena);
            b3BodyDef.type(bodyDef, Box3D.b3_staticBody());
            MemorySegment bodyPos = b3BodyDef.position(bodyDef);
            b3Vec3.x(bodyPos, (float) pos.getX());
            b3Vec3.y(bodyPos, (float) pos.getY());
            b3Vec3.z(bodyPos, (float) pos.getZ());
            MemorySegment bodyId = Box3D.b3CreateBody(bridge.worldArena, bridge.worldId, bodyDef);
            // 2. Create Box Hull
            float halfX = (float) (size.getX() * 0.5);
            float halfY = (float) (size.getY() * 0.5);
            float halfZ = (float) (size.getZ() * 0.5);
            MemorySegment hull = Box3D.b3MakeBoxHull(tempArena, halfX, halfY, halfZ);
            MemorySegment shapeDef = Box3D.b3DefaultShapeDef(tempArena);
            b3ShapeDef.enableHitEvents(shapeDef, true);
//            b3ShapeDef.enableContactEvents(shapeDef, true);
            MemorySegment shapeId = Box3D.b3CreateHullShape(bridge.worldArena, bodyId, shapeDef, hull);
            Box3D.b3Shape_SetFriction(shapeId, friction);
            Box3D.b3Shape_EnableHitEvents(shapeId, true);
//            Box3D.b3Shape_EnableContactEvents(shapeId, true);
            // 3. Optional Visualizer
            Display display = null;
            if (material != null && visualize) {
                display = Box3DBasicUtils.spawnDynamicBlockVisualizer(
                        world,
                        pos.toLocation(world),
                        (float) size.getX(),
                        (float) size.getY(),
                        (float) size.getZ(),
                        material
                );
            }
            // 4. Create Object & Store
            B3Object b3obj = new B3Object(bodyId, shapeId, halfX, halfY, halfZ, display, material, world);

            bridge.staticBlocks.put(pos, b3obj);
            bridge.allShapes.put(b3obj.shapeKey(), b3obj);

            if (material != null && visualize) {
                bridge.render.add(b3obj);
            }
            return b3obj;
        }
    }

    /**
     * Spawns a dynamic, physics-simulated movable rigid body in the world (Center-Oriented).
     *
     * @param pos The initial center position (X, Y, Z) of the dynamic body in world coordinates
     * @param size The full dimensions (sizeX, sizeY, sizeZ) of the box in blocks/meters
     * @param density Physical density (affects mass and inertia, e.g. 1.0 for wood, 8.0 for iron)
     * @param friction Surface friction (e.g. 0.6 for standard grip, 0.1 for ice)
     * @param material Optional Bukkit Material for sound effects and BlockDisplay visualizer
     * @return The created {@link B3Object}
     */
    public B3Object spawnDynamicBlockRectangle(
            Vector pos,
            Vector size,
            float density,
            float friction,
            @Nullable Material material
    ) {
        try (Arena tempArena = Arena.ofConfined()) {
            // 1. Create Dynamic Body Def with Air & Angular Damping
            MemorySegment bodyDef = Box3D.b3DefaultBodyDef(tempArena);
            b3BodyDef.type(bodyDef, Box3D.b3_dynamicBody());
            MemorySegment bodyPos = b3BodyDef.position(bodyDef);
            b3Vec3.x(bodyPos, (float) pos.getX());
            b3Vec3.y(bodyPos, (float) pos.getY());
            b3Vec3.z(bodyPos, (float) pos.getZ());
            // Rotational & linear friction so it settles naturally
            b3BodyDef.linearDamping(bodyDef, 0.2f);
            b3BodyDef.angularDamping(bodyDef, 1.0f);
            MemorySegment bodyId = Box3D.b3CreateBody(bridge.worldArena, bridge.worldId, bodyDef);
            // 2. Create Box Hull
            float halfX = (float) (size.getX() * 0.5);
            float halfY = (float) (size.getY() * 0.5);
            float halfZ = (float) (size.getZ() * 0.5);
            MemorySegment hull = Box3D.b3MakeBoxHull(tempArena, halfX, halfY, halfZ);
            MemorySegment shapeDef = Box3D.b3DefaultShapeDef(tempArena);
            b3ShapeDef.enableSensorEvents(shapeDef, true);
            b3ShapeDef.enableHitEvents(shapeDef, true);
//            b3ShapeDef.enableContactEvents(shapeDef, true);
            MemorySegment shapeId = Box3D.b3CreateHullShape(bridge.worldArena, bodyId, shapeDef, hull);
            Box3D.b3Shape_SetDensity(shapeId, density, true);
            Box3D.b3Shape_SetFriction(shapeId, friction);
            Box3D.b3Shape_EnableHitEvents(shapeId, true);
//            Box3D.b3Shape_EnableContactEvents(shapeId, true);
            Box3D.b3Shape_EnableSensorEvents(shapeId, true);
            // 3. Optional Visualizer
            Display display = null;
            if (material != null) {
                display = Box3DBasicUtils.spawnDynamicBlockVisualizer(
                        world,
                        pos.toLocation(world),
                        (float) size.getX(),
                        (float) size.getY(),
                        (float) size.getZ(),
                        material
                );
            }
            // 4. Create Object & Register
            B3Object b3obj = new B3Object(bodyId, shapeId, halfX, halfY, halfZ, display, material, world);
            bridge.dynamicBlocks.put(b3obj.shapeKey(), b3obj);
            bridge.allShapes.put(b3obj.shapeKey(), b3obj); // Instant O(1) collision key!
            if (display != null) {
                bridge.render.add(b3obj);
            }
            return b3obj;
        }
    }

    /**
     * Spawns a phantom, non-colliding Sensor (Trigger Zone) in the physics world.
     * <p>
     * Sensors do <b>not</b> physically block or bounce objects. Instead, objects pass right through
     * them freely like air, while automatically firing {@link com.keremyurekli.physicsApi.events.SensorEnterEvent} and {@link com.keremyurekli.physicsApi.events.SensorExitEvent}
     * the exact moment an object enters or leaves the trigger zone.
     * @param pos The center position (X, Y, Z) of the trigger volume in world coordinates
     * @param size The full dimensions (width, height, depth) of the sensor box in blocks/meters
     * @param material Optional Bukkit Material (e.g. {@code Material.LIGHT_BLUE_STAINED_GLASS} or {@code null} for invisible)
     * @param visualize If {@code true}, spawns a visualizer for this trigger zone
     * @return The created {@link B3Object} representing the sensor volume
     */
    public B3Object spawnStaticSensor(
            Vector pos,
            Vector size,
            @Nullable Material material,
            boolean visualize
    ) {
        try (Arena tempArena = Arena.ofConfined()) {
            // 1. Create Static Body (Zero-mass, locked in space)
            MemorySegment bodyDef = Box3D.b3DefaultBodyDef(tempArena);
            b3BodyDef.type(bodyDef, Box3D.b3_staticBody());

            MemorySegment bodyPos = b3BodyDef.position(bodyDef);
            b3Vec3.x(bodyPos, (float) pos.getX());
            b3Vec3.y(bodyPos, (float) pos.getY());
            b3Vec3.z(bodyPos, (float) pos.getZ());

            MemorySegment bodyId = Box3D.b3CreateBody(bridge.worldArena, bridge.worldId, bodyDef);

            // 2. Create Box Hull
            float halfX = (float) (size.getX() * 0.5);
            float halfY = (float) (size.getY() * 0.5);
            float halfZ = (float) (size.getZ() * 0.5);

            MemorySegment hull = Box3D.b3MakeBoxHull(tempArena, halfX, halfY, halfZ);
            MemorySegment shapeDef = Box3D.b3DefaultShapeDef(tempArena);

            // ⭐ 3. ENABLE SENSOR FLAGS (Pass-through ghost collider)
            b3ShapeDef.isSensor(shapeDef, true);
            b3ShapeDef.enableSensorEvents(shapeDef, true);

            MemorySegment shapeId = Box3D.b3CreateHullShape(bridge.worldArena, bodyId, shapeDef, hull);
            Box3D.b3Shape_EnableSensorEvents(shapeId, true);

            // 4. Optional Visualizer (e.g. Glowing glass or invisible)
            Display display = null;
            if (material != null && visualize) {
                display = Box3DBasicUtils.spawnDynamicBlockVisualizer(
                        world,
                        pos.toLocation(world),
                        (float) size.getX(),
                        (float) size.getY(),
                        (float) size.getZ(),
                        material
                );
            }

            // 5. Create & Register
            B3Object b3obj = new B3Object(bodyId, shapeId, halfX, halfY, halfZ, display, material, world);

            bridge.staticBlocks.put(pos, b3obj);
            bridge.allShapes.put(b3obj.shapeKey(), b3obj); // Instant O(1) sensor event key!

            if (material != null && visualize) {
                bridge.render.add(b3obj);
            }

            return b3obj;
        }
    }

    public B3Object spawnDynamicItemRectangle(
            Vector pos,
            Vector size,
            @Nullable Vector extraPos,
            @Nullable Vector extraSize,
            float density,
            float friction,
            @Nullable ItemStack is,
            ItemDisplay.ItemDisplayTransform ts
    ) {
        try (Arena tempArena = Arena.ofConfined()) {
            // 1. Create Dynamic Body Def with Air & Angular Damping
            MemorySegment bodyDef = Box3D.b3DefaultBodyDef(tempArena);
            b3BodyDef.type(bodyDef, Box3D.b3_dynamicBody());
            MemorySegment bodyPos = b3BodyDef.position(bodyDef);
            b3Vec3.x(bodyPos, (float) pos.getX());
            b3Vec3.y(bodyPos, (float) pos.getY());
            b3Vec3.z(bodyPos, (float) pos.getZ());
            // Rotational & linear friction so it settles naturally
            b3BodyDef.linearDamping(bodyDef, 0.2f);
            b3BodyDef.angularDamping(bodyDef, 1.0f);
            MemorySegment bodyId = Box3D.b3CreateBody(bridge.worldArena, bridge.worldId, bodyDef);
            // 2. Create Box Hull
            float halfX = (float) (size.getX() * 0.5);
            float halfY = (float) (size.getY() * 0.5);
            float halfZ = (float) (size.getZ() * 0.5);
            MemorySegment hull = Box3D.b3MakeBoxHull(tempArena, halfX, halfY, halfZ);
            MemorySegment shapeDef = Box3D.b3DefaultShapeDef(tempArena);
            b3ShapeDef.enableSensorEvents(shapeDef, true);
            b3ShapeDef.enableHitEvents(shapeDef, true);
//            b3ShapeDef.enableContactEvents(shapeDef, true);
            MemorySegment shapeId = Box3D.b3CreateHullShape(bridge.worldArena, bodyId, shapeDef, hull);
            Box3D.b3Shape_SetDensity(shapeId, density, true);
            Box3D.b3Shape_SetFriction(shapeId, friction);
            Box3D.b3Shape_EnableHitEvents(shapeId, true);
//            Box3D.b3Shape_EnableContactEvents(shapeId, true);
            Box3D.b3Shape_EnableSensorEvents(shapeId, true);
            // 3. Optional Visualizer
            Display display = null;
            if (is != null) {
                display = Box3DBasicUtils.spawnDynamicItemVisualizer(
                        world,
                        pos.toLocation(world),
                        (float) size.getX(),
                        (float) size.getY(),
                        (float) size.getZ(),
                        is,
                        ts
                );
            }
            // 4. Create Object & Register
            B3Object b3obj = new B3Object(bodyId, shapeId, halfX, halfY, halfZ, display, null, world);
            b3obj.extraSizeInfo = extraSize;
            b3obj.extraPosInfo = extraPos;
            bridge.dynamicBlocks.put(b3obj.shapeKey(), b3obj);
            bridge.allShapes.put(b3obj.shapeKey(), b3obj); // Instant O(1) collision key!
            if (display != null) {
                bridge.render.add(b3obj);
            }
            return b3obj;
        }
    }

    public B3Object spawnDynamicSphere(
            Vector pos,
            float rad,
            @Nullable Vector extraPos,
            @Nullable Vector extraSize,
            float density,
            float friction,
            @Nullable ItemStack is
    ) {
        try (Arena tempArena = Arena.ofConfined()) {
            // 1. Create Dynamic Body
            MemorySegment bodyDef = Box3D.b3DefaultBodyDef(tempArena);
            b3BodyDef.type(bodyDef, Box3D.b3_dynamicBody());
            MemorySegment bodyPos = b3BodyDef.position(bodyDef);
            b3Vec3.x(bodyPos, (float) pos.getX());
            b3Vec3.y(bodyPos, (float) pos.getY());
            b3Vec3.z(bodyPos, (float) pos.getZ());

            b3BodyDef.linearDamping(bodyDef, 0.2f);
            b3BodyDef.angularDamping(bodyDef, 1.0f);
            MemorySegment bodyId = Box3D.b3CreateBody(bridge.worldArena, bridge.worldId, bodyDef);

            // 2. Create Sphere Collider
            MemorySegment sphere = b3Sphere.allocate(tempArena);
            b3Sphere.radius(sphere, rad);
            MemorySegment sphereCenter = b3Sphere.center(sphere);
            b3Vec3.x(sphereCenter, 0.0f);
            b3Vec3.y(sphereCenter, 0.0f);
            b3Vec3.z(sphereCenter, 0.0f);

            MemorySegment shapeDef = Box3D.b3DefaultShapeDef(tempArena);
            b3ShapeDef.enableSensorEvents(shapeDef, true);
            b3ShapeDef.enableHitEvents(shapeDef, true);
            b3ShapeDef.enableContactEvents(shapeDef, true);

            MemorySegment shapeId = Box3D.b3CreateSphereShape(bridge.worldArena, bodyId, shapeDef, sphere);
            Box3D.b3Shape_SetDensity(shapeId, density, true);
            Box3D.b3Shape_SetFriction(shapeId, friction);
            Box3D.b3Shape_EnableHitEvents(shapeId, true);
            Box3D.b3Shape_EnableContactEvents(shapeId, true);
            Box3D.b3Shape_EnableSensorEvents(shapeId, true);

            // 3. Multi-angle 3D Slimeball Sphere Visualizers
            float diameter = rad * 2f;
            float visualX = extraSize != null ? (float) extraSize.getX() : diameter;
            float visualY = extraSize != null ? (float) extraSize.getY() : diameter;
            float visualZ = extraSize != null ? (float) extraSize.getZ() : diameter;

            Display primaryDisplay = null;
            List<Display> extraDisplaysList = new ArrayList<>();
            List<Quaternionf> displayRotationsList = new ArrayList<>();

            if (is != null) {
                // Slices: 4 vertical slices rotated around Y (0°, 45°, 90°, 135°)
                // + 1 horizontal equator (Pitch 90°) + 2 diagonal tilts
                Quaternionf[] sphereAngles = new Quaternionf[]{
                        new Quaternionf(),                                                  // Front (0°)
                        new Quaternionf().rotateY((float) Math.toRadians(45)),              // 45° Y
                        new Quaternionf().rotateY((float) Math.toRadians(90)),              // 90° Y
                        new Quaternionf().rotateY((float) Math.toRadians(135)),             // 135° Y
                        new Quaternionf().rotateX((float) Math.toRadians(90)),              // Flat horizontal
                        new Quaternionf().rotateX((float) Math.toRadians(45)),              // 45° X tilt
                        new Quaternionf().rotateZ((float) Math.toRadians(45))               // 45° Z tilt
                };

                for (int i = 0; i < sphereAngles.length; i++) {
                    Display d = Box3DBasicUtils.spawnDynamicItemVisualizer(
                            world,
                            pos.toLocation(world),
                            visualX,
                            visualY,
                            visualZ,
                            is,
                            ItemDisplay.ItemDisplayTransform.FIXED
                    );

                    if (i == 0) {
                        primaryDisplay = d;
                    } else {
                        extraDisplaysList.add(d);
                    }
                    displayRotationsList.add(sphereAngles[i]);
                }
            }

            // 4. Create Object & Register
            B3Object b3obj = new B3Object(bodyId, shapeId, rad, rad, rad, primaryDisplay, null, world);
            b3obj.extraSizeInfo = extraSize;
            b3obj.extraPosInfo = extraPos;
            b3obj.extraDisplays = extraDisplaysList;
            b3obj.displayLocalRotations = displayRotationsList;

            bridge.dynamicBlocks.put(b3obj.shapeKey(), b3obj);
            bridge.allShapes.put(b3obj.shapeKey(), b3obj);

            if (primaryDisplay != null) {
                bridge.render.add(b3obj);
            }

            return b3obj;
        }
    }
}
