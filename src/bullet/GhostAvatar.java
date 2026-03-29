package bullet;

import java.util.UUID;

import org.joml.*;

import tage.*;

public class GhostAvatar extends GameObject
{
    private UUID id;

    public GhostAvatar(UUID id, ObjShape s, TextureImage t, Vector3f pos)
    {
        super(GameObject.root(), s, t);
        this.id = id;

        setLocalLocation(pos);
    }

    public UUID getID()
    {
        return id;
    }

    public void setPosition(Vector3f pos)
    {
        setLocalLocation(pos);
    }
}