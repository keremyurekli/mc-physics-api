package com.keremyurekli.physicsApi.events;

import com.keremyurekli.physicsApi.util.B3Object;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SensorEnterEvent extends Event {


    private static final HandlerList HANDLER_LIST = new HandlerList();
    private World world;
    private B3Object objectA;
    private B3Object objectB;

    public SensorEnterEvent(B3Object objectA, B3Object objectB, World world) {
        this.objectA = objectA;
        this.objectB= objectB;
        this.world = world;
    }
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
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
