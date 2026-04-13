package bullet;

import java.util.UUID;

import org.joml.*;

import tage.*;
import tage.shapes.*;

public class GhostAvatar extends GameObject
{
    private UUID id;
    private AnimatedShape animShape;
    private Vector3f lastPosition;
    private boolean isMoving = false;

    // time since last movement packet
    private float timeSinceLastMove = 0.0f;

    // tweak this if needed
    private final float idleDelay = 0.15f;

    public GhostAvatar(UUID id, ObjShape s, TextureImage t, Vector3f pos)
    {
        super(GameObject.root(), s, t);
        this.id = id;

        setLocalLocation(pos);

        if (s instanceof AnimatedShape)
        {
            animShape = (AnimatedShape) s;
            animShape.playAnimation("STAND", 0.5f, AnimatedShape.EndType.LOOP, 0);
        }

        lastPosition = new Vector3f(pos);
    }

    public UUID getID()
    {
        return id;
    }

    public void setPosition(Vector3f pos)
    {
        float dist = pos.distance(lastPosition);
        boolean nowMoving = dist > 0.001f;

        if (nowMoving)
        {
            timeSinceLastMove = 0.0f;

            if (animShape != null && !isMoving)
            {
                animShape.playAnimation("RUN", 0.3f, AnimatedShape.EndType.LOOP, 0);
            }
        }

        setLocalLocation(pos);
        lastPosition.set(pos);
        isMoving = nowMoving;
    }

    public void update(float dt)
    {
        if (animShape != null)
        {
            animShape.updateAnimation();
        }

        if (isMoving)
        {
            timeSinceLastMove += dt;

            if (timeSinceLastMove >= idleDelay)
            {
                isMoving = false;

                if (animShape != null)
                {
                    animShape.playAnimation("STAND", 0.5f, AnimatedShape.EndType.LOOP, 0);
                }
            }
        }
    }
}