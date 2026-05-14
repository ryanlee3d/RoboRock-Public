package bullet.managers;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import tage.Engine;
import tage.GameObject;
import tage.Light;
import tage.ObjShape;
import tage.TextureImage;
import bullet.MyGame;
import bullet.audio.GameAudio;
import bullet.combat.WeaponInventory;

public class PickupManager
{
    private static final float RESPAWN_TIME = 30.0f;
    private static final float COLLISION_RANGE = 1.5f;
    private static final float HIDDEN_SCALE = 0.0001f;
    private static final float AMMO_SCALE = 0.3f;
    private static final float HEALTH_SCALE = 0.3f;

    private static final Vector3f[] AMMO_SPAWN_POSITIONS =
    {
        new Vector3f(-46.0f, 0.0f, -13.0f),
        new Vector3f(-91.0f, 0.0f,  56.0f),
        new Vector3f(-58.0f, 0.0f,  89.0f),
        new Vector3f(-36.0f, 0.0f,  76.0f),
        new Vector3f( 60.0f, 0.0f, -19.0f),
        new Vector3f( 94.0f, 0.0f, -36.0f),
        new Vector3f( 68.0f, 0.0f, -81.0f),
        new Vector3f( 37.0f, 0.0f,   4.0f)
    };

    private static final Vector3f[] HEALTH_SPAWN_POSITIONS =
    {
        new Vector3f(-44.0f, 0.0f, -11.0f),
        new Vector3f(-89.0f, 0.0f,  58.0f),
        new Vector3f(-56.0f, 0.0f,  91.0f),
        new Vector3f(-29.0f, 0.0f,  30.0f),
        new Vector3f( 62.0f, 0.0f, -17.0f),
        new Vector3f( 96.0f, 0.0f, -34.0f),
        new Vector3f( 70.0f, 0.0f, -79.0f),
        new Vector3f( 26.0f, 0.0f, -90.0f)
    };

    private final MyGame game;

    private GameObject[] ammoPickups;
    private GameObject[] healthPickups;
    private boolean[] ammoActive;
    private boolean[] healthActive;
    private float[] ammoRespawnTimers;
    private float[] healthRespawnTimers;
    private Light[] ammoLights;
    private Light[] healthLights;
    private float ammoBobTime = 0.0f;
    private float healthSpin = 0.0f;

    public PickupManager(MyGame game)
    {
        this.game = game;
    }

    public void buildObjects(ObjShape ammoShape, TextureImage ammoTexture, ObjShape healthShape, TextureImage healthTexture)
    {
        ammoPickups = new GameObject[AMMO_SPAWN_POSITIONS.length];
        ammoActive = new boolean[AMMO_SPAWN_POSITIONS.length];
        ammoRespawnTimers = new float[AMMO_SPAWN_POSITIONS.length];
        for (int i = 0; i < AMMO_SPAWN_POSITIONS.length; i++)
        {
            Vector3f pos = AMMO_SPAWN_POSITIONS[i];
            ammoPickups[i] = new GameObject(GameObject.root(), ammoShape, ammoTexture);
            ammoPickups[i].setLocalTranslation(new Matrix4f().translation(pos.x, 0.0f, pos.z));
            ammoPickups[i].setLocalScale(new Matrix4f().scaling(AMMO_SCALE));
            ammoActive[i] = true;
        }

        healthPickups = new GameObject[HEALTH_SPAWN_POSITIONS.length];
        healthActive = new boolean[HEALTH_SPAWN_POSITIONS.length];
        healthRespawnTimers = new float[HEALTH_SPAWN_POSITIONS.length];
        for (int i = 0; i < HEALTH_SPAWN_POSITIONS.length; i++)
        {
            Vector3f pos = HEALTH_SPAWN_POSITIONS[i];
            healthPickups[i] = new GameObject(GameObject.root(), healthShape, healthTexture);
            healthPickups[i].setLocalTranslation(new Matrix4f().translation(pos.x, 0.0f, pos.z));
            healthPickups[i].setLocalScale(new Matrix4f().scaling(HEALTH_SCALE));
            healthPickups[i].getRenderStates().setModelOrientationCorrection(
                new Matrix4f().rotationX((float)java.lang.Math.toRadians(90.0f))
            );
            healthActive[i] = true;
        }
    }

    public void initializeLights(Engine engine)
    {
        ammoLights = new Light[AMMO_SPAWN_POSITIONS.length];
        for (int i = 0; i < ammoLights.length; i++)
        {
            ammoLights[i] = new Light();
            ammoLights[i].setDiffuse(0.2f, 1.0f, 0.2f);
            ammoLights[i].setSpecular(0.2f, 0.6f, 1.0f);
            ammoLights[i].setAmbient(0.05f, 0.1f, 0.2f);
            ammoLights[i].setType(Light.LightType.SPOTLIGHT);
            ammoLights[i].setDirection(new Vector3f(0.0f, -1.0f, 0.0f));
            ammoLights[i].setCutoffAngle(20.0f);
            ammoLights[i].setOffAxisExponent(10.0f);
            engine.getSceneGraph().addLight(ammoLights[i]);
        }

        healthLights = new Light[HEALTH_SPAWN_POSITIONS.length];
        for (int i = 0; i < healthLights.length; i++)
        {
            healthLights[i] = new Light();
            healthLights[i].setDiffuse(0.2f, 0.6f, 1.0f);
            healthLights[i].setSpecular(0.2f, 1.0f, 0.2f);
            healthLights[i].setAmbient(0.05f, 0.2f, 0.05f);
            healthLights[i].setType(Light.LightType.SPOTLIGHT);
            healthLights[i].setDirection(new Vector3f(0.0f, -1.0f, 0.0f));
            healthLights[i].setCutoffAngle(20.0f);
            healthLights[i].setOffAxisExponent(10.0f);
            engine.getSceneGraph().addLight(healthLights[i]);
        }
    }

    public void handleCollisions(float dt, GameObject player, WeaponInventory weapons, GameAudio audio)
    {
        if (player == null || ammoPickups == null || healthPickups == null) return;

        Vector3f playerPos = player.getWorldLocation();

        for (int i = 0; i < ammoPickups.length; i++)
        {
            if (!ammoActive[i])
            {
                ammoRespawnTimers[i] -= dt;
                if (ammoRespawnTimers[i] <= 0.0f)
                {
                    ammoRespawnTimers[i] = 0.0f;
                    showAmmoPickup(i);
                }
            }

            if (ammoActive[i] && ammoPickups[i] != null && playerPos.distance(ammoPickups[i].getWorldLocation()) <= COLLISION_RANGE)
            {
                weapons.addAmmoPickupBundle();
                audio.playAmmoPickup(ammoPickups[i].getWorldLocation());
                hideAmmoPickup(i);
            }
        }

        for (int i = 0; i < healthPickups.length; i++)
        {
            if (!healthActive[i])
            {
                healthRespawnTimers[i] -= dt;
                if (healthRespawnTimers[i] <= 0.0f)
                {
                    healthRespawnTimers[i] = 0.0f;
                    showHealthPickup(i);
                }
            }

            if (healthActive[i] && healthPickups[i] != null && playerPos.distance(healthPickups[i].getWorldLocation()) <= COLLISION_RANGE)
            {
                game.setPlayerHealth(game.getPlayerHealthMax());
                audio.playHealthPickup(healthPickups[i].getWorldLocation());
                hideHealthPickup(i);
            }
        }
    }

    public void update(float dt, GameObject terrain)
    {
        if (terrain == null || ammoPickups == null || healthPickups == null) return;

        ammoBobTime += dt;
        float bobOffset = (float)java.lang.Math.sin(ammoBobTime * 2.0f) * 0.25f;
        for (int i = 0; i < ammoPickups.length; i++)
        {
            Vector3f pos = AMMO_SPAWN_POSITIONS[i];
            float terrainY = terrain.getHeight(pos.x, pos.z);
            ammoPickups[i].setLocalTranslation(new Matrix4f().translation(pos.x, terrainY + 0.75f + bobOffset, pos.z));
        }

        healthSpin += dt * 45.0f;
        for (int i = 0; i < healthPickups.length; i++)
        {
            Vector3f pos = HEALTH_SPAWN_POSITIONS[i];
            float terrainY = terrain.getHeight(pos.x, pos.z);
            healthPickups[i].setLocalRotation(new Matrix4f().rotationY((float)java.lang.Math.toRadians(healthSpin)));
            healthPickups[i].setLocalTranslation(new Matrix4f().translation(pos.x, terrainY + 0.75f, pos.z));
        }

        updateLightPositions();
    }

    private void hideAmmoPickup(int index)
    {
        ammoActive[index] = false;
        ammoRespawnTimers[index] = RESPAWN_TIME;
        if (ammoPickups[index] != null)
            ammoPickups[index].setLocalScale(new Matrix4f().scaling(HIDDEN_SCALE));
    }

    private void showAmmoPickup(int index)
    {
        ammoActive[index] = true;
        if (ammoPickups[index] != null)
            ammoPickups[index].setLocalScale(new Matrix4f().scaling(AMMO_SCALE));
    }

    private void hideHealthPickup(int index)
    {
        healthActive[index] = false;
        healthRespawnTimers[index] = RESPAWN_TIME;
        if (healthPickups[index] != null)
            healthPickups[index].setLocalScale(new Matrix4f().scaling(HIDDEN_SCALE));
    }

    private void showHealthPickup(int index)
    {
        healthActive[index] = true;
        if (healthPickups[index] != null)
            healthPickups[index].setLocalScale(new Matrix4f().scaling(HEALTH_SCALE));
    }

    private void updateLightPositions()
    {
        if (ammoLights != null)
        {
            for (int i = 0; i < ammoPickups.length; i++)
            {
                if (ammoPickups[i] == null || ammoLights[i] == null) continue;

                if (ammoActive[i])
                {
                    ammoLights[i].setLocation(ammoPickups[i].getWorldLocation());
                }
                else
                {
                    ammoLights[i].setLocation(new Vector3f(0.0f, -10000.0f, 0.0f));
                }
            }
        }

        if (healthLights != null)
        {
            for (int i = 0; i < healthPickups.length; i++)
            {
                if (healthPickups[i] == null || healthLights[i] == null) continue;

                if (healthActive[i])
                {
                    healthLights[i].setLocation(healthPickups[i].getWorldLocation());
                }
                else
                {
                    healthLights[i].setLocation(new Vector3f(0.0f, -10000.0f, 0.0f));
                }
            }
        }
    }
}
