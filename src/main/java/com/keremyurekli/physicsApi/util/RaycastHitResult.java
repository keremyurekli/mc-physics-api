package com.keremyurekli.physicsApi.util;

import org.bukkit.util.Vector;

import java.lang.foreign.MemorySegment;

public class RaycastHitResult {
        public MemorySegment hitShapeId = null;
        public float closestFraction = 1.0f;
        public boolean dynamic = false;
        public B3Object object;
        public Vector point;
    }