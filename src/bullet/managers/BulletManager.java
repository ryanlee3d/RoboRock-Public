package bullet.managers;

import java.util.ArrayList;
import java.util.Iterator;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import tage.GameObject;
import tage.ObjShape;
import tage.TextureImage;
import tage.physics.PhysicsEngine;
import tage.physics.PhysicsObject;
import bullet.MyGame;
import bullet.combat.Bullet;
import java.util.UUID;

public class BulletManager {
    private final MyGame game;
    private PhysicsEngine physicsEngine;
    private final ArrayList<Bullet> activeBullets = new ArrayList<>();

    private ObjShape bulletSphereS;
    private TextureImage bulletYellowTx;
    private TextureImage bulletBlueTx;

    private final float bulletLifeMax = 10.0f;
    private final float bulletRadius = 0.02f; 
    private final float plasmaRadius = 0.30f;
    private final float bulletGravityScale = 0.01f;
    private final float bulletSpeed = 60.0f;
    private final float plasmaSpeed = 35.0f;
    private final float worldGravity = -9.8f;

    public BulletManager(MyGame game) {
        this.game = game;
    }

    public void setPhysicsEngine(PhysicsEngine pe) {
        this.physicsEngine = pe;
    }

    public void buildObjects(ObjShape sphereS, TextureImage yellowTx, TextureImage blueTx) {
        this.bulletSphereS = sphereS;
        this.bulletYellowTx = yellowTx;
        this.bulletBlueTx = blueTx;
    }

    public void spawnPlayerBullet(Vector3f spawnPos, Vector3f dir, boolean isPlasma) {
        spawnBullet(spawnPos, dir, isPlasma, false, null);
    }

    public void spawnNetworkPlayerBullet(UUID ownerID, Vector3f spawnPos, Vector3f dir, boolean isPlasma) {
        spawnBullet(spawnPos, dir, isPlasma, false, ownerID);
    }

    public void spawnEnemyBullet(Vector3f spawnPos, Vector3f dir, boolean isPlasma) {
        spawnBullet(spawnPos, dir, isPlasma, true, null);
    }

    private void spawnBullet(Vector3f spawnPos, Vector3f dir, boolean isPlasma, boolean fromEnemy, UUID ownerID) {
        GameObject bulletObj = new GameObject(GameObject.root(), bulletSphereS, isPlasma ? bulletBlueTx : bulletYellowTx);
        float scale = isPlasma ? plasmaRadius : bulletRadius;
        bulletObj.setLocalTranslation(new Matrix4f().translation(spawnPos.x, spawnPos.y, spawnPos.z));
        bulletObj.setLocalScale(new Matrix4f().scaling(scale));

        PhysicsObject bulletP = game.getEngine().getSceneGraph().addPhysicsSphere(
            isPlasma ? 2.0f : 1.0f, spawnPos, new Quaternionf(), scale);

        bulletP.setBounciness(0.0f);
        bulletP.setFriction(0.2f);
        bulletP.setDamping(0.0f, 0.0f);
        bulletP.setGravity(new float[] { 0f, worldGravity * bulletGravityScale, 0f });
        bulletP.disableSleeping();

        float speed = isPlasma ? plasmaSpeed : bulletSpeed;
        Vector3f velocity = new Vector3f(dir).mul(speed);
        bulletP.setLinearVelocity(new float[] { velocity.x, velocity.y, velocity.z });
        bulletObj.setPhysicsObject(bulletP);

        activeBullets.add(new Bullet(bulletObj, bulletP, velocity, bulletLifeMax, isPlasma, fromEnemy, ownerID));
    }

    public void update(float dt) {
        Iterator<Bullet> it = activeBullets.iterator();
        while (it.hasNext()) {
            Bullet b = it.next();
            if (b.obj == null || b.phys == null) {
                removeBullet(b);
                it.remove();
                continue;
            }

            Vector3f loc = b.phys.getLocation();
            b.obj.setLocalTranslation(new Matrix4f().translation(loc.x, loc.y, loc.z));
            boolean bulletRemoved = false;

            if (b.fromEnemy) {
                GameObject player = game.getAvatar();
                if (player != null && loc.distance(player.getWorldLocation()) < 1.0f) {
                    game.addPlayerHealth(-10);
                    bulletRemoved = true;
                }
            } else {
                if (game.checkAndDamageApe(loc, b.ownerID) ||
                    game.checkAndDamageSkinny(loc, b.ownerID) ||
                    game.checkAndDamageBrain(loc))
                    bulletRemoved = true;
            }

            b.lifetime -= dt;
            if (bulletRemoved || b.lifetime <= 0.0f || loc.y < -10.0f) {
                removeBullet(b);
                it.remove();
            }
        }
    }

    private void removeBullet(Bullet b) {
        if (b.obj != null) b.obj.setLocalScale(new Matrix4f().scaling(0.0001f));
        if (b.phys != null && physicsEngine != null) physicsEngine.removeObject(b.phys.getUID());
    }
}
