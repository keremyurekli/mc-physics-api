package com.keremyurekli.physicsApi.events;

import com.keremyurekli.physicsApi.util.B3Object;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.util.Vector;

public class BodyCollisionEvent extends Event {


    private static final HandlerList HANDLER_LIST = new HandlerList();
    private World world;
    private B3Object objectA;
    private B3Object objectB;
    private Vector point;
    private float approachSpeed;

    public BodyCollisionEvent(B3Object objectA, B3Object objectB, Vector point, float approachSpeed, World world) {
        this.objectA = objectA;
        this.objectB= objectB;
        this.point = point;
        this.approachSpeed = approachSpeed;
        this.world = world;
    }
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public Vector getPoint() {
        return point;
    }

    public float getApproachSpeed() {
        return approachSpeed;
    }

    public World getWorld() {
        return world;
    }

    public B3Object getObjectA() {
        return objectA;
    }

    public B3Object getObjectB() {
        return objectB;
    }
}
