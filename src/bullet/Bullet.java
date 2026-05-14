package bullet;

import tage.GameObject;
import tage.physics.PhysicsObject;
import org.joml.Vector3f;
import java.util.UUID;

public class Bullet {
    public GameObject obj;
    public PhysicsObject phys;
    public Vector3f velocity;
    public float lifetime;
    public boolean isPlasma;
    public boolean fromEnemy;
    public UUID ownerID;

    public Bullet(GameObject obj, PhysicsObject phys, Vector3f velocity, float lifetime, boolean isPlasma, boolean fromEnemy)
    {
        this(obj, phys, velocity, lifetime, isPlasma, fromEnemy, null);
    }

    public Bullet(GameObject obj, PhysicsObject phys, Vector3f velocity, float lifetime, boolean isPlasma, boolean fromEnemy, UUID ownerID)
    {
        this.obj = obj;
        this.phys = phys;
        this.velocity = velocity;
        this.lifetime = lifetime;
        this.isPlasma = isPlasma;
        this.fromEnemy = fromEnemy;
        this.ownerID = ownerID;
    }
}