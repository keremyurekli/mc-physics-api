package com.keremyurekli.physicsApi.util;

import com.keremyurekli.physicsApi.events.BodyCollisionEvent;
import org.bukkit.Bukkit;
import org.bukkit.SoundGroup;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ExampleCollisionEventListener implements Listener {


    @EventHandler
    public void onBodyCollision(BodyCollisionEvent event) {
        if (event.getApproachSpeed() > 1.5f) {
            List<B3Object> collisionObjects = new ArrayList<>();

            collisionObjects.add(event.getObjectA());
            collisionObjects.add(event.getObjectB());

            collisionObjects.forEach(obj -> {
                if (obj.material() != null) {
                    float volume = Math.min(1.0f, event.getApproachSpeed() / 10.0f);
                    SoundGroup group = obj.material().createBlockData().getSoundGroup();

                    float minPitch = 0.8f;
                    float maxPitch = 1.2f;
                    float randomPitch = ThreadLocalRandom.current().nextFloat() * (maxPitch - minPitch) + minPitch;

                    event.getWorld().playSound(event.getPoint().toLocation(event.getWorld()), group.getHitSound(), volume, randomPitch);
                }
            });
        }
    }
}
