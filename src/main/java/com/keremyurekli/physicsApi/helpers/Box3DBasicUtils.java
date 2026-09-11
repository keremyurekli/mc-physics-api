package com.keremyurekli.physicsApi.helpers;

import com.keremyurekli.physicsApi.util.B3Object;
import com.keremyurekli.physicsApi.Box3DBridge;
import com.keremyurekli.physicsApi.PhysicsApi;
import com.keremyurekli.physicsApi.util.RaycastHitResult;
import org.box3d.Box3D;
import org.box3d.b3CastResultFcn;
import org.box3d.b3QueryFilter;
import org.box3d.b3Vec3;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

public class Box3DBasicUtils {


    public static BlockDisplay spawnDynamicBlockVisualizer(World world, Location location, float sizex, float sizey, float sizez, Material material) {
        return world.spawn(
                location,
                BlockDisplay.class,
                entity -> {
                    entity.setBlock(
                            material.createBlockData()
                    );

                    entity.setPersistent(true);
//                    entity.setViewRange(5000);
                    entity.setRotation(0, 0);

                    entity.setViewRange(5.0f);
                    entity.setDisplayWidth(200.0f);
                    entity.setDisplayHeight(200.0f);
                    entity.setShadowRadius(0.0f);

                    entity.setTransformation(
                            new Transformation(
                                    new Vector3f((float) location.x(), (float) location.y(), (float) location.z()),
                                    new AxisAngle4f(),
                                    new Vector3f(
                                            sizex,
                                            sizey,
                                            sizez
                                    ),
                                    new AxisAngle4f()
                            )
                    );
                }
        );
    }

    public static ItemDisplay spawnDynamicItemVisualizer(World world, Location location, float sizex, float sizey, float sizez, ItemStack is, ItemDisplay.ItemDisplayTransform ts) {
        return world.spawn(
                location,
                ItemDisplay.class,
                entity -> {
                    entity.setItemStack(is);
                    entity.setPersistent(true);
                    entity.setRotation(0, 0);
                    entity.setViewRange(5.0f);
                    entity.setDisplayWidth(200.0f);
                    entity.setDisplayHeight(200.0f);
                    entity.setShadowRadius(0.0f);
                    entity.setItemDisplayTransform(ts);
                    entity.setTransformation(
                            new Transformation(
                                    new Vector3f(0f, 0f, 0f), // Relative offset starts at (0, 0, 0)
                                    new AxisAngle4f(),
                                    new Vector3f(sizex, sizey, sizez),
                                    new AxisAngle4f()
                            )
                    );
                }
        );
    }
//    public static ItemDisplay spawnDynamicItemVisualizer(World world, Location location, float sizex, float sizey, float sizez, ItemStack is, ItemDisplay.ItemDisplayTransform ts) {
//        return world.spawn(
//                location,
//                ItemDisplay.class,
//                entity -> {
//                    entity.setItemStack(
//                            is
//                    );
//
//                    entity.setPersistent(true);
////                    entity.setViewRange(5000);
//                    entity.setRotation(0, 0);
//
//                    entity.setViewRange(5.0f);
//                    entity.setDisplayWidth(200.0f);
//                    entity.setDisplayHeight(200.0f);
//                    entity.setShadowRadius(0.0f);
//
//                    entity.setItemDisplayTransform(ts);
//                    entity.setTransformation(
//                            new Transformation(
//                                    new Vector3f((float) location.x(), (float) location.y(), (float) location.z()),
//                                    new AxisAngle4f(),
//                                    new Vector3f(
//                                            sizex,
//                                            sizey,
//                                            sizez
//                                    ),
//                                    new AxisAngle4f()
//                            )
//                    );
//                }
//        );
//    }
    public static Vector pos(B3Object obj) {
        try (Arena tempArena = Arena.ofConfined()) {

            MemorySegment pos = Box3D.b3Body_GetPosition(tempArena, obj.bodyId());
            float bx = b3Vec3.x(pos);
            float by = b3Vec3.y(pos);
            float bz = b3Vec3.z(pos);
            return new Vector(bx, by, bz);
        }
    }
    public static Vector pos(MemorySegment bodyId) {
        try (Arena tempArena = Arena.ofConfined()) {

            MemorySegment pos = Box3D.b3Body_GetPosition(tempArena, bodyId);
            float bx = b3Vec3.x(pos);
            float by = b3Vec3.y(pos);
            float bz = b3Vec3.z(pos);
            return new Vector(bx, by, bz);
        }
    }

    public static MemorySegment shapeIdToBodyId(Arena tickArena, MemorySegment shapeId) {
        return Box3D.b3Shape_GetBody(tickArena, shapeId);
    }

    public static float getDistanceToPlayer(Player player, B3Object obj, boolean eye) {
        try (Arena tempArena = Arena.ofConfined()) {

            MemorySegment pos = Box3D.b3Body_GetPosition(tempArena, obj.bodyId());
            float bx = b3Vec3.x(pos);
            float by = b3Vec3.y(pos);
            float bz = b3Vec3.z(pos);

            Location p = eye ? player.getEyeLocation() : player.getLocation();
            float dx = bx - (float) p.getX();
            float dy = by - (float) p.getY();
            float dz = bz - (float) p.getZ();

            return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }

    public static void velocity(B3Object obj, float addX, float addY, float addZ, boolean set) {
        try (Arena tempArena = Arena.ofConfined()) {

            MemorySegment newVel = b3Vec3.allocate(tempArena);



            if (set) {
                MemorySegment currentVel = Box3D.b3Body_GetLinearVelocity(tempArena, obj.bodyId());
                float curX = b3Vec3.x(currentVel);
                float curY = b3Vec3.y(currentVel);
                float curZ = b3Vec3.z(currentVel);

                b3Vec3.x(newVel, curX + addX);
                b3Vec3.y(newVel, curY + addY);
                b3Vec3.z(newVel, curZ + addZ);

            } else {
                b3Vec3.x(newVel, addX);
                b3Vec3.y(newVel, addY);
                b3Vec3.z(newVel, addZ);
            }


            Box3D.b3Body_SetLinearVelocity(obj.bodyId(), newVel);
        }
    }


    public static RaycastHitResult raycastWorldNative(
            World world,
            Vector start,
            Vector dir,
            float maxDistance
    ) {

        Box3DBridge bridge = PhysicsApi.getBridge(world);
        Vector3f translation = new Vector3f(
                (float) (dir.getX() * maxDistance),
                (float) (dir.getY() * maxDistance),
                (float) (dir.getZ() * maxDistance)
        );
        try (Arena rayArena = Arena.ofConfined()) {
            MemorySegment origin = b3Vec3.allocate(rayArena);
            b3Vec3.x(origin, (float) start.getX());
            b3Vec3.y(origin, (float) start.getY());
            b3Vec3.z(origin, (float) start.getZ());
            MemorySegment trans = b3Vec3.allocate(rayArena);
            b3Vec3.x(trans, translation.x);
            b3Vec3.y(trans, translation.y);
            b3Vec3.z(trans, translation.z);

            MemorySegment filter = Box3D.b3DefaultQueryFilter(rayArena);
            b3QueryFilter.categoryBits(filter, -1L);
            b3QueryFilter.maskBits(filter, -1L);
            RaycastHitResult result = new RaycastHitResult();
            b3CastResultFcn.Function callback = (shapeId, point, normal, fraction,
                                                 materialId, triangleIndex, childIndex, context) -> {
                if (fraction < result.closestFraction) {
                    result.closestFraction = fraction;

                    MemorySegment savedShape = rayArena.allocate(shapeId.byteSize());
                    savedShape.copyFrom(shapeId);
                    result.hitShapeId = savedShape;

                    result.point = new Vector(b3Vec3.x(point),b3Vec3.y(point),b3Vec3.z(point));
                }
                return fraction;
            };
            MemorySegment callbackStub = b3CastResultFcn.allocate(callback, rayArena);
            Box3D.b3World_CastRay(rayArena,bridge.worldId, origin, trans, filter, callbackStub, MemorySegment.NULL);
            if (result.hitShapeId != null) {



                MemorySegment bodyId = Box3D.b3Shape_GetBody(rayArena, result.hitShapeId);

                MemorySegment pos = Box3D.b3Body_GetPosition(rayArena, bodyId);
                float bodyX = b3Vec3.x(pos);
                float bodyY = b3Vec3.y(pos);
                float bodyZ = b3Vec3.z(pos);

                int bodyType = Box3D.b3Body_GetType(bodyId);
                boolean isDynamic = (bodyType == Box3D.b3_dynamicBody());


                if (isDynamic) {
                    if (bridge.dynamicBlocks.containsKey(result.hitShapeId.get(ValueLayout.JAVA_LONG, 0))) {
                        result.object = bridge.dynamicBlocks.get(result.hitShapeId.get(ValueLayout.JAVA_LONG, 0));
                        result.dynamic = true;
                    }
                } else {
                    if (bridge.staticBlocks.containsKey(new Vector(bodyX,bodyY,bodyZ))) {
                        result.object = bridge.staticBlocks.get(new Vector(bodyX,bodyY,bodyZ));
                    }
                }


                return result;
            }
            return null;
        }
    }

}
