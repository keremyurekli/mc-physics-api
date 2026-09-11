package com.keremyurekli.physicsApi.helpers;

import com.keremyurekli.physicsApi.Box3DBridge;
import org.box3d.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

public class Box3DJointHelper {

    public enum HingeAxis {
        PITCH_X,
        YAW_Y,
        ROLL_Z
    }

    /**
     * Creates a powered, motorized Revolute Joint between two rigid bodies.
     * <p>
     * A Motor Joint applies continuous rotational torque to reach and maintain a target angular speed (RPM)
     * around a specified local axis. Angle limits are disabled by default so the joint can spin full 360-degree
     * rotations indefinitely.
     * @param bodyA The first rigid body (often a static base/anchor or vehicle chassis)
     * @param bodyB The second rigid body to be spun (e.g. blade, wheel, rotor)
     * @param aX The X offset of the pivot anchor in Body A's local space
     * @param aY The Y offset of the pivot anchor in Body A's local space
     * @param aZ The Z offset of the pivot anchor in Body A's local space
     * @param bX The X offset of the pivot anchor in Body B's local space (e.g. center of the blade)
     * @param bY The Y offset of the pivot anchor in Body B's local space
     * @param bZ The Z offset of the pivot anchor in Body B's local space
     * @param rpm Target angular speed in Revolutions Per Minute (e.g. 60.0 for 1 spin/sec, negative values spin in reverse)
     * @param maxTorque Maximum rotational force the motor can exert (higher values allow slicing through obstacles without slowing down)
     * @param axis The rotation axis orientation ({@link HingeAxis#YAW_Y} for horizontal blender/turntable, {@link HingeAxis#PITCH_X}/{@link HingeAxis#ROLL_Z} for vertical windmill/wheel)
     * @param worldId The native Box3D World ID (b3WorldId)
     * @param worldArena The persistent Arena managing the physics world's lifecycle
     * @return The created native Joint ID (MemorySegment of b3JointId). Can be used to change speed or toggle the motor at runtime.
     */
    public static MemorySegment createMotorJoint(
            MemorySegment bodyA, MemorySegment bodyB,
            float aX, float aY, float aZ,
            float bX, float bY, float bZ,
            float rpm,
            float maxTorque,
            HingeAxis axis,
            MemorySegment worldId,
            Arena worldArena
    ) {
        try (Arena tempArena = Arena.ofConfined()) {
            MemorySegment jointDef = Box3D.b3DefaultRevoluteJointDef(tempArena);
            MemorySegment base = b3RevoluteJointDef.base(jointDef);
            b3JointDef.bodyIdA(base, bodyA);
            b3JointDef.bodyIdB(base, bodyB);
            // Local Anchors
            MemorySegment frameA = b3JointDef.localFrameA(base);
            MemorySegment pA = b3Transform.p(frameA);
            b3Vec3.x(pA, aX); b3Vec3.y(pA, aY); b3Vec3.z(pA, aZ);
            MemorySegment frameB = b3JointDef.localFrameB(base);
            MemorySegment pB = b3Transform.p(frameB);
            b3Vec3.x(pB, bX); b3Vec3.y(pB, bY); b3Vec3.z(pB, bZ);
            // Orientations
            setHingeOrientation(b3Transform.q(frameA), axis);
            setHingeOrientation(b3Transform.q(frameB), axis);
            // Motor properties
            b3RevoluteJointDef.enableMotor(jointDef, true);
            float targetSpeedRad = (float) ((rpm * 2.0 * Math.PI) / 60.0);
            b3RevoluteJointDef.motorSpeed(jointDef, targetSpeedRad);
            b3RevoluteJointDef.maxMotorTorque(jointDef, maxTorque);
            b3JointDef.collideConnected(base, false);
            b3RevoluteJointDef.enableLimit(jointDef, false);
            return Box3D.b3CreateRevoluteJoint(worldArena, worldId, jointDef);
        }
    }

    /**
     * Creates a 1-DOF Rotational Hinge (Revolute Joint) between two rigid bodies.
     * <p>
     * A Revolute Joint pins two bodies together at a shared anchor point, allowing them to swing
     * freely around a single axis of rotation while locking all linear movement. Optional minimum
     * and maximum angle limits can be enforced to prevent limbs or doors from swinging past desired angles.
     * @param bodyA The first rigid body (e.g. doorframe, torso, ceiling anchor)
     * @param bodyB The second rigid body (e.g. door, limb, pendulum)
     * @param aX The X offset of the hinge pin in Body A's local space
     * @param aY The Y offset of the hinge pin in Body A's local space
     * @param aZ The Z offset of the hinge pin in Body A's local space
     * @param bX The X offset of the hinge pin in Body B's local space
     * @param bY The Y offset of the hinge pin in Body B's local space
     * @param bZ The Z offset of the hinge pin in Body B's local space
     * @param axis The rotation axis orientation ({@link HingeAxis#PITCH_X} for nod/walking swing, {@link HingeAxis#YAW_Y} for door swing, {@link HingeAxis#ROLL_Z} for clock face/tilt)
     * @param lowerLimit The minimum allowed rotation angle in radians (e.g. {@code (float) Math.toRadians(-90)})
     * @param upperLimit The maximum allowed rotation angle in radians (e.g. {@code (float) Math.toRadians(90)})
     * @param useLimits {@code true} to enforce lower/upper angle constraints, {@code false} to allow full 360-degree free swinging
     * @param worldId The native Box3D World ID (b3WorldId)
     * @param worldArena The persistent Arena managing the physics world's lifecycle
     * @return The created native Joint ID (MemorySegment of b3JointId)
     */
    public static MemorySegment createRevoluteJoint(
            MemorySegment bodyA, MemorySegment bodyB,
            float aX, float aY, float aZ,
            float bX, float bY, float bZ,
            HingeAxis axis,
            float lowerLimit, float upperLimit,
            boolean useLimits,
            MemorySegment worldId,
            Arena worldArena
    ) {
        try (Arena tempArena = Arena.ofConfined()) {
            MemorySegment jointDef = Box3D.b3DefaultRevoluteJointDef(tempArena);
            MemorySegment base = b3RevoluteJointDef.base(jointDef);
            b3JointDef.bodyIdA(base, bodyA);
            b3JointDef.bodyIdB(base, bodyB);
            // 1. Anchors (p)
            MemorySegment frameA = b3JointDef.localFrameA(base);
            MemorySegment pA = b3Transform.p(frameA);
            b3Vec3.x(pA, aX); b3Vec3.y(pA, aY); b3Vec3.z(pA, aZ);
            MemorySegment frameB = b3JointDef.localFrameB(base);
            MemorySegment pB = b3Transform.p(frameB);
            b3Vec3.x(pB, bX); b3Vec3.y(pB, bY); b3Vec3.z(pB, bZ);
            // 2. Rotation Axis (q)
            setHingeOrientation(b3Transform.q(frameA), axis);
            setHingeOrientation(b3Transform.q(frameB), axis);
            // 3. Flags and Limits
            b3JointDef.collideConnected(base, false);
            if (useLimits) {
                b3RevoluteJointDef.enableLimit(jointDef, true);
                b3RevoluteJointDef.lowerAngle(jointDef, lowerLimit);
                b3RevoluteJointDef.upperAngle(jointDef, upperLimit);
            }
            return Box3D.b3CreateRevoluteJoint(worldArena, worldId, jointDef);
        }
    }

    /**
     * Creates a Distance Joint between two rigid bodies.
     * <p>
     * A Distance Joint constrains the distance between two local anchor points.
     * @param bodyA The first rigid body (MemorySegment of b3BodyId)
     * @param bodyB The second rigid body (MemorySegment of b3BodyId)
     * @param aX The X offset of the anchor point in Body A's local space
     * @param aY The Y offset of the anchor point in Body A's local space
     * @param aZ The Z offset of the anchor point in Body A's local space
     * @param bX The X offset of the anchor point in Body B's local space
     * @param bY The Y offset of the anchor point in Body B's local space
     * @param bZ The Z offset of the anchor point in Body B's local space
     * @param length The target rest distance between the two anchor points (in meters/blocks)
     * @param spring {@code true} to enable spring elasticity, {@code false} for a stiff non-elastic cable/rod
     * @param hertz The spring oscillation frequency in Hertz (stiffness: e.g. 4.0 for soft bungee, 30.0 for stiff spring, ignored if spring is false)
     * @param dampingRatio The spring damping ratio (0.0 = bouncy oscillation, 1.0 = critical damping/no overshoot, ignored if spring is false)
     * @param worldId The native Box3D World ID (b3WorldId)
     * @param worldArena The persistent Arena managing the physics world's lifecycle
     * @return The created native Joint ID (MemorySegment of b3JointId)
     */
    public static MemorySegment createDistanceJoint(
            MemorySegment bodyA, MemorySegment bodyB,
            float aX, float aY, float aZ,
            float bX, float bY, float bZ,
            float length,
            boolean spring,
            float hertz, float dampingRatio,
            MemorySegment worldId,
            Arena worldArena
    ) {
        try (Arena tempArena = Arena.ofConfined()) {
            MemorySegment jointDef = Box3D.b3DefaultDistanceJointDef(tempArena);
            MemorySegment base = b3DistanceJointDef.base(jointDef);

            b3JointDef.bodyIdA(base, bodyA);
            b3JointDef.bodyIdB(base, bodyB);

            MemorySegment frameA = b3JointDef.localFrameA(base);
            b3Vec3.x(b3Transform.p(frameA), aX);
            b3Vec3.y(b3Transform.p(frameA), aY);
            b3Vec3.z(b3Transform.p(frameA), aZ);

            MemorySegment frameB = b3JointDef.localFrameB(base);
            b3Vec3.x(b3Transform.p(frameB), bX);
            b3Vec3.y(b3Transform.p(frameB), bY);
            b3Vec3.z(b3Transform.p(frameB), bZ);

            b3DistanceJointDef.length(jointDef, length);
            b3DistanceJointDef.enableSpring(jointDef, spring);
            if (spring) {
                b3DistanceJointDef.hertz(jointDef, hertz);
                b3DistanceJointDef.dampingRatio(jointDef, dampingRatio);
            }

            b3JointDef.collideConnected(base, false);
            return Box3D.b3CreateDistanceJoint(worldArena, worldId, jointDef);
        }
    }


    /**
     * Creates a 2-Axis Universal Joint (Gimbal / Flexible Neck / Shoulder)
     * Allows rotation in 2 independent directions with custom angle limits for each!
     *
     * @param bridge The Box3DBridge instance
     * @param bodyA The parent body (e.g. Torso)
     * @param bodyB The child body (e.g. Head)
     * @param pivotWorldX The world X coordinate of the neck pivot point
     * @param pivotWorldY The world Y coordinate of the neck pivot point
     * @param pivotWorldZ The world Z coordinate of the neck pivot point
     * @param aX, aY, aZ Local anchor on Body A (e.g. top of torso: 0, torsoHy, 0)
     * @param bX, bY, bZ Local anchor on Body B (e.g. bottom of head: 0, -headH, 0)
     * @param axis1 Primary rotation axis (e.g. PITCH_X for nodding forward/back)
     * @param lowerLimit1, upperLimit1 Angle limits for axis 1 (in radians)
     * @param axis2 Secondary rotation axis (e.g. ROLL_Z or YAW_Y for tilting/turning)
     * @param lowerLimit2, upperLimit2 Angle limits for axis 2 (in radians)
     */
    public static UniversalJointResult createUniversalJoint(
            Box3DBridge bridge,
            MemorySegment bodyA, MemorySegment bodyB,
            float pivotWorldX, float pivotWorldY, float pivotWorldZ,
            float aX, float aY, float aZ,
            float bX, float bY, float bZ,
            HingeAxis axis1, float lowerLimit1, float upperLimit1,
            HingeAxis axis2, float lowerLimit2, float upperLimit2,
            boolean useLimits
    ) {
        try (Arena tempArena = Arena.ofConfined()) {
            // 1. Create a tiny, invisible intermediate bone (0.02m, completely weightless)
            MemorySegment boneDef = Box3D.b3DefaultBodyDef(tempArena);
            b3BodyDef.type(boneDef, Box3D.b3_dynamicBody());
            MemorySegment bonePos = b3BodyDef.position(boneDef);
            b3Vec3.x(bonePos, pivotWorldX);
            b3Vec3.y(bonePos, pivotWorldY);
            b3Vec3.z(bonePos, pivotWorldZ);
            MemorySegment boneBody = Box3D.b3CreateBody(bridge.worldArena, bridge.worldId, boneDef);
            // Very small hull so it has valid non-zero inertia
            float boneHalf = 0.02f;
            MemorySegment hull = Box3D.b3MakeBoxHull(tempArena, boneHalf, boneHalf, boneHalf);
            MemorySegment shapeDef = Box3D.b3DefaultShapeDef(tempArena);
            b3ShapeDef.density(shapeDef, 0.1f);
            Box3D.b3CreateHullShape(bridge.worldArena, boneBody, shapeDef, hull);
            // 2. Hinge 1: Body A (Torso) <-> Intermediate Bone (Axis 1)
            MemorySegment joint1 = createRevoluteJoint(
                    bodyA, boneBody,
                    aX, aY, aZ,
                    0.0f, 0.0f, 0.0f,
                    axis1,
                    lowerLimit1, upperLimit1,
                    useLimits,
                    bridge.worldId, bridge.worldArena
            );
            // 3. Hinge 2: Intermediate Bone <-> Body B (Head) (Axis 2)
            MemorySegment joint2 = createRevoluteJoint(
                    boneBody, bodyB,
                    0.0f, 0.0f, 0.0f,
                    bX, bY, bZ,
                    axis2,
                    lowerLimit2, upperLimit2,
                    useLimits,
                    bridge.worldId, bridge.worldArena
            );
            return new UniversalJointResult(joint1, joint2, boneBody);
        }
    }


    public static void setHingeOrientation(MemorySegment qSeg, HingeAxis axis) {
        MemorySegment v = b3Quat.v(qSeg);
        switch (axis) {
            case YAW_Y -> {
                b3Vec3.x(v, 0.7071f);
                b3Vec3.y(v, 0.0f);
                b3Vec3.z(v, 0.0f);
                b3Quat.s(qSeg, 0.7071f);
            }
            case PITCH_X -> {
                b3Vec3.x(v, 0.0f);
                b3Vec3.y(v, 0.7071f);
                b3Vec3.z(v, 0.0f);
                b3Quat.s(qSeg, 0.7071f);
            }
            case ROLL_Z -> {
                b3Vec3.x(v, 0.0f);
                b3Vec3.y(v, 0.0f);
                b3Vec3.z(v, 0.0f);
                b3Quat.s(qSeg, 1.0f);
            }
        }
    }



    public record UniversalJointResult(
            MemorySegment joint1,
            MemorySegment joint2,
            MemorySegment intermediateBone
    ) {}
}