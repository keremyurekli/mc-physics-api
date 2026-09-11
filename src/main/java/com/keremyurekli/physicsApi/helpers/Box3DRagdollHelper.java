package com.keremyurekli.physicsApi.helpers;

import com.keremyurekli.physicsApi.util.B3Object;
import com.keremyurekli.physicsApi.Box3DBridge;
import org.box3d.Box3D;
import org.box3d.b3BodyDef;
import org.box3d.b3ShapeDef;
import org.box3d.b3Vec3;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Display;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;

public class Box3DRagdollHelper {



    private final Box3DBridge ifc;
    private final MemorySegment worldId;
    private final Arena worldArena;
    public Box3DRagdollHelper(Box3DBridge ifc) {
        this.ifc = ifc;
        this.worldId = ifc.worldId;
        this.worldArena = ifc.worldArena;
    }
    public List<B3Object> exampleRagdoll(Location loc) {
        List<B3Object> ragdollLimbs = new ArrayList<>();
        World world = loc.getWorld();
        float spawnX = (float) loc.getX();
        float spawnY = (float) loc.getY();
        float spawnZ = (float) loc.getZ();
        // Half-extents (Half of full width, height, depth) in blocks
        float torsoHx = 0.25f, torsoHy = 0.35f, torsoHz = 0.15f; // Torso: 0.5 x 0.7 x 0.3
        float headH   = 0.20f;                                  // Head:  0.4 x 0.4 x 0.4
        float armHx   = 0.10f, armHy   = 0.30f, armHz   = 0.10f; // Arms:  0.2 x 0.6 x 0.2
        float legHx   = 0.12f, legHy   = 0.35f, legHz   = 0.12f; // Legs:  0.24 x 0.7 x 0.24
        // -------------------------------------------------------------
        // 1. Create the 6 Limbs
        // -------------------------------------------------------------
        // A. Torso (Center of the ragdoll)
        B3Object torso = createLimb(world, spawnX, spawnY, spawnZ, torsoHx, torsoHy, torsoHz, 1.2f, Material.CYAN_CONCRETE);
        ragdollLimbs.add(torso);
        // B. Head (Above torso)
        float headY = spawnY + torsoHy + headH + 0.05f;
        B3Object head = createLimb(world, spawnX, headY, spawnZ, headH, headH, headH, 0.8f, Material.WHITE_CONCRETE);
        ragdollLimbs.add(head);
        // C. Left Arm & Right Arm (Shoulders)
        float armY = spawnY + torsoHy - armHy;
        float leftArmX = spawnX - (torsoHx + armHx + 0.05f);
        float rightArmX = spawnX + (torsoHx + armHx + 0.05f);
        B3Object leftArm = createLimb(world, leftArmX, armY, spawnZ, armHx, armHy, armHz, 0.7f, Material.LIGHT_BLUE_CONCRETE);
        B3Object rightArm = createLimb(world, rightArmX, armY, spawnZ, armHx, armHy, armHz, 0.7f, Material.LIGHT_BLUE_CONCRETE);
        ragdollLimbs.add(leftArm);
        ragdollLimbs.add(rightArm);
        // D. Left Leg & Right Leg (Hips)
        float legY = spawnY - (torsoHy + legHy + 0.05f);
        float leftLegX = spawnX - (torsoHx * 0.5f);
        float rightLegX = spawnX + (torsoHx * 0.5f);
        B3Object leftLeg = createLimb(world, leftLegX, legY, spawnZ, legHx, legHy, legHz, 1.0f, Material.GRAY_CONCRETE);
        B3Object rightLeg = createLimb(world, rightLegX, legY, spawnZ, legHx, legHy, legHz, 1.0f, Material.GRAY_CONCRETE);
        ragdollLimbs.add(leftLeg);
        ragdollLimbs.add(rightLeg);
        // -------------------------------------------------------------
        // 2. Connect Limbs with Revolute Hinge Joints
        // -------------------------------------------------------------
        try (Arena jointArena = Arena.ofConfined()) {
            // Neck Joint: Torso top <-> Head bottom (Bends -30 to +30 degrees)
            Box3DJointHelper.createRevoluteJoint(
                    torso.bodyId(), head.bodyId(),
                    0.0f, torsoHy, 0.0f,
                    0.0f, -headH, 0.0f,
                    Box3DJointHelper.HingeAxis.ROLL_Z,
                    (float) Math.toRadians(-30.0), (float) Math.toRadians(30.0),
                    true,
                    worldId,worldArena);
            // Left Shoulder: Torso left <-> Left arm top (-90 to +90 degrees)
            Box3DJointHelper.createRevoluteJoint(
                    torso.bodyId(), leftArm.bodyId(),
                    -torsoHx, torsoHy, 0.0f,
                    armHx, armHy, 0.0f,
                    Box3DJointHelper.HingeAxis.ROLL_Z,
                    (float) Math.toRadians(-90.0), (float) Math.toRadians(90.0),
                    true,
                    worldId,worldArena);
            // Right Shoulder: Torso right <-> Right arm top (-90 to +90 degrees)
            Box3DJointHelper.createRevoluteJoint(
                    torso.bodyId(), rightArm.bodyId(),
                    torsoHx, torsoHy, 0.0f,
                    -armHx, armHy, 0.0f,
                    Box3DJointHelper.HingeAxis.ROLL_Z,
                    (float) Math.toRadians(-90.0), (float) Math.toRadians(90.0),
                    true,
                    worldId,worldArena);
            // Left Hip: Torso bottom left <-> Left leg top (-70 to +30 degrees)
            Box3DJointHelper.createRevoluteJoint(
                    torso.bodyId(), leftLeg.bodyId(),
                    -torsoHx * 0.5f, -torsoHy, 0.0f,
                    0.0f, legHy, 0.0f,
                    Box3DJointHelper.HingeAxis.ROLL_Z,
                    (float) Math.toRadians(-70.0), (float) Math.toRadians(30.0),
                    true,
                    worldId,worldArena);
            // Right Hip: Torso bottom right <-> Right leg top (-70 to +30 degrees)
            Box3DJointHelper.createRevoluteJoint(
                    torso.bodyId(), rightLeg.bodyId(),
                    torsoHx * 0.5f, -torsoHy, 0.0f,
                    0.0f, legHy, 0.0f,
                    Box3DJointHelper.HingeAxis.ROLL_Z,
                    (float) Math.toRadians(-70.0), (float) Math.toRadians(30.0),
                    true,
                    worldId,worldArena);
        }


        ragdollLimbs.forEach(b3 -> {
            ifc.dynamicBlocks.put(b3.shapeKey(),b3);
            ifc.render.add(b3);
        });
        return ragdollLimbs;
    }
    // Creates a single limb Box3D body and its Minecraft BlockDisplay
    private B3Object createLimb(World world, float x, float y, float z, float hx, float hy, float hz, float density, Material material) {
        try (Arena temp = Arena.ofConfined()) {
            MemorySegment bodyDef = Box3D.b3DefaultBodyDef(temp);
            b3BodyDef.type(bodyDef, Box3D.b3_dynamicBody());
            MemorySegment pos = b3BodyDef.position(bodyDef);
            b3Vec3.x(pos, x);
            b3Vec3.y(pos, y);
            b3Vec3.z(pos, z);
            MemorySegment bodyId = Box3D.b3CreateBody(worldArena, worldId, bodyDef);
            MemorySegment hull = Box3D.b3MakeBoxHull(temp, hx, hy, hz);
            MemorySegment shapeDef = Box3D.b3DefaultShapeDef(temp);
            b3ShapeDef.density(shapeDef, density);
            MemorySegment shapeId = Box3D.b3CreateHullShape(worldArena, bodyId, shapeDef, hull);
            Box3D.b3Shape_SetFriction(shapeId,0.6f);
            // Spawn BlockDisplay visualizer with matching dimensions
            Display display = Box3DBasicUtils.spawnDynamicBlockVisualizer(world, new Location(world, x, y, z), hx * 2, hy * 2, hz * 2, material);
            return new B3Object(bodyId, shapeId, hx, hy, hz, display, material, world);
        }
    }
}
