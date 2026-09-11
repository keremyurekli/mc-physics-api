package com.keremyurekli.physicsApi;

import com.keremyurekli.physicsApi.events.BodyCollisionEvent;
import com.keremyurekli.physicsApi.events.SensorEnterEvent;
import com.keremyurekli.physicsApi.events.SensorExitEvent;
import com.keremyurekli.physicsApi.helpers.Box3DBasicUtils;
import com.keremyurekli.physicsApi.helpers.Box3DChunkMesher;
import com.keremyurekli.physicsApi.helpers.Box3DCreationHelper;
import com.keremyurekli.physicsApi.helpers.Box3DRagdollHelper;
import com.keremyurekli.physicsApi.util.B3Object;
import com.keremyurekli.physicsApi.util.Box3DLoader;
import org.box3d.*;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Box3DBridge {


    public static final float gravity = -9.8f;
    public final Arena worldArena = Arena.ofShared();
//    public Box3DChunkUtils blockTranslator;
    public World world;

    public Box3DChunkMesher chunkMesher;
    public Box3DCreationHelper creationHelper;



    public Map<Vector, B3Object> staticBlocks = new HashMap<>();

    // if you have the shapeId
    // you can get the uuid by shapeId.get(ValueLayout.JAVA_LONG, 0);
    public Map<Long, B3Object> allShapes = new HashMap<>();
    public Map<Long, B3Object> dynamicBlocks = new HashMap<>();

    public List<B3Object> render = new ArrayList<>();

    public MemorySegment worldId;

    public Box3DRagdollHelper ragdollHelper;

    public void init(World world) {

        Box3DLoader.load();

        MemorySegment worldDef = Box3D.b3DefaultWorldDef(worldArena);

        MemorySegment gravity = b3WorldDef.gravity(worldDef);
        b3Vec3.x(gravity, 0.0f);
        b3Vec3.y(gravity, Box3DBridge.gravity);
        b3Vec3.z(gravity, 0.0f);


        this.worldId = Box3D.b3CreateWorld(worldArena, worldDef);
        this.world = world;

//        blockTranslator = new Box3DChunkUtils(this);
        ragdollHelper = new Box3DRagdollHelper(this);
        chunkMesher = new Box3DChunkMesher(this,world, PhysicsApi.PLUGIN);
        creationHelper = new Box3DCreationHelper(this,world,PhysicsApi.PLUGIN);
    }

    public void clearStatics() {
        new ArrayList<>(staticBlocks.values()).forEach(B3Object::destroy);
        staticBlocks.clear();
    }

    public void clearDynamics() {
        new ArrayList<>(dynamicBlocks.values()).forEach(B3Object::destroy);
        dynamicBlocks.clear();
    }

    public void cleanup() {
        // 1. Remove all visualizer display entities
        render.forEach(o -> {
            if (o.visualizer() != null && o.visualizer().isValid()) {
                o.visualizer().remove();
            }
        });
        render.clear();
        staticBlocks.clear();
        dynamicBlocks.clear();
        allShapes.clear();

        // 2. Destroy the Box3D world once (Box3D automatically frees all bodies in C)
        if (worldId != null) {
            Box3D.b3DestroyWorld(worldId);
            worldId = null;
        }

        // 3. Close world arena safely
        if (worldArena.scope().isAlive()) {
            worldArena.close();
        }
    }


    // this exists because after the distance specified in spigot.yml, displayblocks becomes invisible because the original entity position gets left behind while we update its transform
// every 20 seconds should be fine
    public void updateEntityPositions() {

        //REMOVED FOR NOW
//        dynamicBlocks.forEach(db -> {
//            //maybe i should've async teleport idk
////            if (db.visualizer().getViewRange())
//            if (db.visualizer().getTrackedBy().isEmpty() && !db.visualizer().getWorld().getPlayers().isEmpty()) {
//                Vector b3Pos = b3BasicUtils.b3BodyPos(db);
//                if (!db.visualizer().getLocation().toVector().equals(b3Pos)) {
//                    db.visualizer().teleport(b3Pos.toLocation(db.visualizer().getWorld()));
//                }
//            }
//        });
    }


    public void removeStaticBlock(Location loc) {
        try (Arena tickArena = Arena.ofConfined()) {
//            float x = (float) (loc.getX() + 0.5f);
//            float y = (float) (loc.getY() + 0.5F);
//            float z = (float) (loc.getZ() + 0.5f);

            Vector v = new Vector(loc.x(), loc.y(), loc.z());
            if (staticBlocks.containsKey(v.clone().add(new Vector(0.5f,0.5f,0.5f)))) {
                staticBlocks.get(v.clone().add(new Vector(0.5f,0.5f,0.5f))).destroy();
//                Box3D.b3DestroyBody(staticBlocks.get(v).bodyId());
//                staticBlocks.remove(v);
            }
        }
    }


    public void onServerTick() {
        if (worldId != null) {
            Box3D.b3World_Step(worldId, 1.0f / 20.0f, 4);

            calculateCollisions();
            calculateSensors();
        }
    }

    public void renderWork() {
        try (Arena tickArena = Arena.ofConfined()) {
            for (B3Object obj : render) {
                Display primaryDisplay = obj.visualizer();
                if (primaryDisplay == null || !primaryDisplay.isValid()) continue;

                // 1. Get Box3D Position (Current center of mass)
                MemorySegment pos = Box3D.b3Body_GetPosition(tickArena, obj.bodyId());
                float px = b3Vec3.x(pos);
                float py = b3Vec3.y(pos);
                float pz = b3Vec3.z(pos);

                // 2. Get Box3D Rotation (Quaternion)
                MemorySegment rot = Box3D.b3Body_GetRotation(tickArena, obj.bodyId());
                MemorySegment v = b3Quat.v(rot);
                float qx = b3Vec3.x(v);
                float qy = b3Vec3.y(v);
                float qz = b3Vec3.z(v);
                float qw = b3Quat.s(rot);
                Quaternionf leftRotation = new Quaternionf(qx, qy, qz, qw);

                // 3. Determine Scale (2 * halfExtent or extraSizeInfo)
                Vector3f scale;
                if (obj.extraSizeInfo != null) {
                    scale = obj.extraSizeInfo.toVector3f();
                } else {
                    scale = new Vector3f(obj.halfX() * 2f, obj.halfY() * 2f, obj.halfZ() * 2f);
                }

                // 4. Collect all displays to transform (Primary + any extra multi-plane displays)
                List<Display> displaysToRender = new ArrayList<>();
                displaysToRender.add(primaryDisplay);

                if (obj.extraDisplays != null) {
                    for (Display extra : obj.extraDisplays) {
                        if (extra != null && extra.isValid()) {
                            displaysToRender.add(extra);
                        }
                    }
                }

                // 5. Update transformations for all display layers
                for (int i = 0; i < displaysToRender.size(); i++) {
                    Display display = displaysToRender.get(i);

                    // Relative position to this specific display entity's base location
                    Location baseLoc = display.getLocation();
                    float relX = (float) (px - baseLoc.getX());
                    float relY = (float) (py - baseLoc.getY());
                    float relZ = (float) (pz - baseLoc.getZ());

                    // Calculate pivot offset based on display type
                    Vector3f localPivotOffset;
                    if (display instanceof ItemDisplay) {
                        // ItemDisplay is already center-anchored in Minecraft
                        localPivotOffset = (obj.extraPosInfo != null)
                                ? obj.extraPosInfo.toVector3f()
                                : new Vector3f(0f, 0f, 0f);
                    } else {
                        // BlockDisplay is corner-anchored, so shift by -halfExtent to center it
                        localPivotOffset = new Vector3f(-obj.halfX(), -obj.halfY(), -obj.halfZ());
                        if (obj.extraPosInfo != null) {
                            localPivotOffset.add(obj.extraPosInfo.toVector3f());
                        }
                    }

                    // Rotate local pivot offset with body rotation and add relative translation
                    Vector3f finalTranslation = new Vector3f(relX, relY, relZ).add(new Vector3f(localPivotOffset).rotate(leftRotation));

                    // Combine physics rotation with this layer's local angle (for 3D spheres/shells)
                    Quaternionf combinedRotation;
                    if (obj.displayLocalRotations != null && i < obj.displayLocalRotations.size()) {
                        combinedRotation = new Quaternionf(leftRotation).mul(obj.displayLocalRotations.get(i));
                    } else {
                        combinedRotation = leftRotation;
                    }

                    display.setInterpolationDuration(2);
                    display.setInterpolationDelay(0);
                    display.setTransformation(new Transformation(finalTranslation, combinedRotation, scale, new Quaternionf()));
                }
            }
        }
    }
//    public void renderWork() {
//        try (Arena tickArena = Arena.ofConfined()) {
//            for (B3Object obj : render) {
//                Display display = obj.visualizer();
//                if (display == null || !display.isValid()) continue;
//
//                // 1. Get Box3D Position (Current center of mass)
//                MemorySegment pos = Box3D.b3Body_GetPosition(tickArena, obj.bodyId());
//                float px = b3Vec3.x(pos);
//                float py = b3Vec3.y(pos);
//                float pz = b3Vec3.z(pos);
//
//                // 2. Get Box3D Rotation (Quaternion)
//                MemorySegment rot = Box3D.b3Body_GetRotation(tickArena, obj.bodyId());
//                MemorySegment v = b3Quat.v(rot);
//                float qx = b3Vec3.x(v);
//                float qy = b3Vec3.y(v);
//                float qz = b3Vec3.z(v);
//                float qw = b3Quat.s(rot);
//                Quaternionf leftRotation = new Quaternionf(qx, qy, qz, qw);
//
//                // 3. Entity's stationary base location
//                Location baseLoc = display.getLocation();
//                float relX = (float) (px - baseLoc.getX());
//                float relY = (float) (py - baseLoc.getY());
//                float relZ = (float) (pz - baseLoc.getZ());
//
//                // 4. Calculate Center Pivot Offset + extraPosInfo
//                // Local corner offset: (-halfX, -halfY, -halfZ) + extraPosInfo
//                Vector3f localPivotOffset = new Vector3f(-obj.halfX(), -obj.halfY(), -obj.halfZ());
//                if (obj.extraPosInfo != null) {
//                    localPivotOffset.add(obj.extraPosInfo.toVector3f());
//                }
//
//                // Rotate local offset with the body and add relative world position
//                Vector3f finalTranslation = new Vector3f(relX, relY, relZ).add(localPivotOffset.rotate(leftRotation));
//
//                // 5. Apply Transformation (Scale is 2 * halfExtent or extraSizeInfo)
//                Vector3f scale;
//                if (obj.extraSizeInfo != null) {
//                    scale = obj.extraSizeInfo.toVector3f();
//                } else {
//                    scale = new Vector3f(obj.halfX() * 2f, obj.halfY() * 2f, obj.halfZ() * 2f);
//                }
//
//                display.setInterpolationDuration(2);
//                display.setInterpolationDelay(0);
//                display.setTransformation(new Transformation(finalTranslation, leftRotation, scale, new Quaternionf()));
//            }
//        }
//    }


    private void calculateCollisions() {
        try (Arena tickArena = Arena.ofConfined()) {
            MemorySegment contactEvents = Box3D.b3World_GetContactEvents(tickArena, worldId);
            // =============================================================
            // 1. CONTACT HIT EVENTS (Impact with speed and point)
            // =============================================================
            int hitCount = b3ContactEvents.hitCount(contactEvents);
            if (hitCount > 0) {
                MemorySegment hitEventsArray = b3ContactEvents.hitEvents(contactEvents);
                for (int i = 0; i < hitCount; i++) {
                    MemorySegment event = b3ContactHitEvent.asSlice(hitEventsArray, i);
                    float speed = b3ContactHitEvent.approachSpeed(event);
                    MemorySegment pt = b3ContactHitEvent.point(event);
                    float x = b3Vec3.x(pt);
                    float y = b3Vec3.y(pt);
                    float z = b3Vec3.z(pt);
                    MemorySegment shapeA = b3ContactHitEvent.shapeIdA(event);
                    MemorySegment shapeB = b3ContactHitEvent.shapeIdB(event);
                    long keyA = shapeA.get(ValueLayout.JAVA_LONG, 0);
                    long keyB = shapeB.get(ValueLayout.JAVA_LONG, 0);
                    B3Object objectA = allShapes.get(keyA);
                    B3Object objectB = allShapes.get(keyB);
                    // Fallback to legacy map lookup if not in allShapes
                    if (objectA == null) {
                        objectA = dynamicBlocks.get(keyA);
                        if (objectA == null) {
                            MemorySegment bodyA = Box3D.b3Shape_GetBody(tickArena, shapeA);
                            objectA = staticBlocks.get(Box3DBasicUtils.pos(bodyA));
                        }
                    }
                    if (objectB == null) {
                        objectB = dynamicBlocks.get(keyB);
                        if (objectB == null) {
                            MemorySegment bodyB = Box3D.b3Shape_GetBody(tickArena, shapeB);
                            objectB = staticBlocks.get(Box3DBasicUtils.pos(bodyB));
                        }
                    }
                    if (objectA == null || objectB == null) {
                        continue;
                    }
                    BodyCollisionEvent collisionEvent = new BodyCollisionEvent(objectA, objectB, new Vector(x, y, z), speed, world);
                    collisionEvent.callEvent();
                }
            }
        }
    }

    private void calculateSensors() {
        try (Arena tickArena = Arena.ofConfined()) {
            MemorySegment sensorEvents = Box3D.b3World_GetSensorEvents(tickArena, worldId);

            // =============================================================
            // 1. SENSOR BEGIN TOUCH (Object ENTERED trigger zone)
            // =============================================================
            int beginCount = b3SensorEvents.beginCount(sensorEvents);
            if (beginCount > 0) {
                MemorySegment beginEventsArray = b3SensorEvents.beginEvents(sensorEvents);
                for (int i = 0; i < beginCount; i++) {
                    MemorySegment event = b3SensorBeginTouchEvent.asSlice(beginEventsArray, i);

                    MemorySegment sensorShape   = b3SensorBeginTouchEvent.sensorShapeId(event);
                    MemorySegment visitorShape  = b3SensorBeginTouchEvent.visitorShapeId(event);

                    long sensorKey  = sensorShape.get(ValueLayout.JAVA_LONG, 0);
                    long visitorKey = visitorShape.get(ValueLayout.JAVA_LONG, 0);

                    B3Object sensorObj  = allShapes.get(sensorKey);
                    B3Object visitorObj = allShapes.get(visitorKey);

                    if (sensorObj == null) {
                        MemorySegment body = Box3D.b3Shape_GetBody(tickArena, sensorShape);
                        sensorObj = staticBlocks.get(Box3DBasicUtils.pos(body));
                    }
                    if (visitorObj == null) {
                        visitorObj = dynamicBlocks.get(visitorKey);
                    }

                    if (sensorObj == null || visitorObj == null) {
                        continue;
                    }

                    SensorEnterEvent enterEvent = new SensorEnterEvent(sensorObj, visitorObj, world);
                    enterEvent.callEvent();
                }
            }

            // =============================================================
            // 2. SENSOR END TOUCH (Object EXITED trigger zone)
            // =============================================================
            int endCount = b3SensorEvents.endCount(sensorEvents);
            if (endCount > 0) {
                MemorySegment endEventsArray = b3SensorEvents.endEvents(sensorEvents);
                for (int i = 0; i < endCount; i++) {
                    MemorySegment event = b3SensorEndTouchEvent.asSlice(endEventsArray, i);

                    MemorySegment sensorShape   = b3SensorEndTouchEvent.sensorShapeId(event);
                    MemorySegment visitorShape  = b3SensorEndTouchEvent.visitorShapeId(event);

                    long sensorKey  = sensorShape.get(ValueLayout.JAVA_LONG, 0);
                    long visitorKey = visitorShape.get(ValueLayout.JAVA_LONG, 0);

                    B3Object sensorObj  = allShapes.get(sensorKey);
                    B3Object visitorObj = allShapes.get(visitorKey);

                    if (sensorObj == null) {
                        MemorySegment body = Box3D.b3Shape_GetBody(tickArena, sensorShape);
                        sensorObj = staticBlocks.get(Box3DBasicUtils.pos(body));
                    }
                    if (visitorObj == null) {
                        visitorObj = dynamicBlocks.get(visitorKey);
                    }

                    if (sensorObj == null || visitorObj == null) {
                        continue;
                    }

                    SensorExitEvent exitEvent = new SensorExitEvent(sensorObj, visitorObj, world);
                    exitEvent.callEvent();
                }
            }
        }
    }
}
