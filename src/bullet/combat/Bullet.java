package bullet.combat;

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
    public int enemyDamage;
    public int brainDamage;

    public Bullet(GameObject obj, PhysicsObject phys, Vector3f velocity, float lifetime, boolean isPlasma, boolean fromEnemy)
    {
        this(obj, phys, velocity, lifetime, isPlasma, fromEnemy, null, 100, 20);
    }

    public Bullet(GameObject obj, PhysicsObject phys, Vector3f velocity, float lifetime, boolean isPlasma, boolean fromEnemy, UUID ownerID)
    {
        this(obj, phys, velocity, lifetime, isPlasma, fromEnemy, ownerID, 100, 20);
    }

    public Bullet(GameObject obj, PhysicsObject phys, Vector3f velocity, float lifetime, boolean isPlasma, boolean fromEnemy, UUID ownerID, int enemyDamage, int brainDamage)
    {
        this.obj = obj;
        this.phys = phys;
        this.velocity = velocity;
        this.lifetime = lifetime;
        this.isPlasma = isPlasma;
        this.fromEnemy = fromEnemy;
        this.ownerID = ownerID;
        this.enemyDamage = enemyDamage;
        this.brainDamage = brainDamage;
    }
}
