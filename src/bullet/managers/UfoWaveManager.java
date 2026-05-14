package bullet.managers;

import java.util.function.Consumer;
import java.util.function.Supplier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import tage.GameObject;
import tage.ObjShape;
import tage.TextureImage;

public class UfoWaveManager
{
    private static final Vector3f[] UFO_DROP_POSITIONS =
    {
        new Vector3f(-64.48f, 5.99f,  38.01f),
        new Vector3f(-76.82f, 7.92f,  -5.37f),
        new Vector3f( 21.98f, 5.14f, -15.00f),
        new Vector3f( 66.83f, 5.45f, -28.26f),
        new Vector3f( 39.35f, 4.94f, -67.59f),
        new Vector3f( 86.69f, 5.45f, -52.79f),
        new Vector3f(-27.04f, 5.65f,  11.53f),
        new Vector3f(-50.59f, 6.23f,  -6.37f)
    };

    private static final Vector3f LARGE_UFO_DROP_POSITION = new Vector3f(69.30f, 4.94f, -64.08f);

    private final Consumer<Vector3f> apeSpawner;
    private final Supplier<Vector3f> playerPositionSupplier;

    private GameObject[] ufos = new GameObject[UFO_DROP_POSITIONS.length];
    private boolean[] usedUfoDrops = new boolean[UFO_DROP_POSITIONS.length];

    private GameObject largeUfo;
    private GameObject activeUfo = null;
    private Vector3f activeUfoStart = new Vector3f();
    private Vector3f activeUfoTarget = new Vector3f();
    private float activeUfoTravelTime = 0.0f;
    private float activeUfoTravelDuration = 6.0f;
    private int activeUfoDropCount = 0;
    private boolean active = false;
    private boolean waveDropFinished = false;
    private int currentWave = 0;
    private boolean networkVisualOnly = false;
    private Vector3f lastWaveTarget = new Vector3f();
    private int lastWaveApeCount = 0;
    private int lastWaveUfoIndex = -1;

    public UfoWaveManager(Consumer<Vector3f> apeSpawner, Supplier<Vector3f> playerPositionSupplier)
    {
        this.apeSpawner = apeSpawner;
        this.playerPositionSupplier = playerPositionSupplier;
    }

    public void buildObjects(ObjShape ufoShape, TextureImage ufoTexture)
    {
        for (int i = 0; i < ufos.length; i++)
        {
            ufos[i] = new GameObject(GameObject.root(), ufoShape, ufoTexture);
            ufos[i].setLocalTranslation(new Matrix4f().translation(9999.0f, 9999.0f, 9999.0f));
            ufos[i].setLocalScale(new Matrix4f().scaling(0.05f));
        }

        largeUfo = new GameObject(GameObject.root(), ufoShape, ufoTexture);
        largeUfo.setLocalTranslation(new Matrix4f().translation(9999.0f, 9999.0f, 9999.0f));
        largeUfo.setLocalScale(new Matrix4f().scaling(0.25f));
    }

    public boolean isActive()
    {
        return active;
    }

    public void reset()
    {
        currentWave = 0;
        active = false;
        waveDropFinished = false;
    }

    public void startNextWave()
    {
        if (active)
            return;

        if (currentWave < UFO_DROP_POSITIONS.length)
        {
            int nextIndex = getClosestUnusedUfoIndex();

            if (nextIndex < 0)
                return;

            usedUfoDrops[nextIndex] = true;
            spawnWave(UFO_DROP_POSITIONS[nextIndex], 5, nextIndex);
        }
        else if (currentWave == UFO_DROP_POSITIONS.length)
        {
            spawnWave(LARGE_UFO_DROP_POSITION, 10, -1);
        }

        currentWave++;
    }

    public boolean isFinalWaveComplete()
    {
        return currentWave > UFO_DROP_POSITIONS.length && !active;
    }

    public Vector3f getLargeUfoPosition()
    {
        if (largeUfo == null)
            return new Vector3f(LARGE_UFO_DROP_POSITION.x, LARGE_UFO_DROP_POSITION.y + 18.0f, LARGE_UFO_DROP_POSITION.z);

        return largeUfo.getWorldLocation();
    }

    public void startNetworkWave(Vector3f pos, int apeCount, int ufoIndex)
    {
        if (active)
            return;

        networkVisualOnly = true;
        spawnWave(pos, apeCount, ufoIndex);
    }

    public Vector3f getLastWaveTarget()
    {
        return new Vector3f(lastWaveTarget);
    }

    public int getLastWaveApeCount()
    {
        return lastWaveApeCount;
    }

    public int getLastWaveUfoIndex()
    {
        return lastWaveUfoIndex;
    }

    public void update(float dt)
    {
        if (!active || activeUfo == null || waveDropFinished) return;

        activeUfoTravelTime += dt;
        float t = activeUfoTravelTime / activeUfoTravelDuration;
        if (t > 1.0f) t = 1.0f;

        Vector3f pos = new Vector3f(
            activeUfoStart.x + (activeUfoTarget.x - activeUfoStart.x) * t,
            activeUfoStart.y + (activeUfoTarget.y + 18.0f - activeUfoStart.y) * t,
            activeUfoStart.z + (activeUfoTarget.z - activeUfoStart.z) * t
        );

        float zigzag = (float)org.joml.Math.sin(t * 8.0f * org.joml.Math.PI) * 8.0f;
        pos.x += zigzag;

        activeUfo.setLocalTranslation(new Matrix4f().translation(pos));

        if (t >= 1.0f)
        {
            if (!networkVisualOnly)
                dropApes(activeUfoTarget, activeUfoDropCount);

            waveDropFinished = true;
            active = false;
            networkVisualOnly = false;
        }
    }

    private int getClosestUnusedUfoIndex()
    {
        Vector3f playerPos = null;

        if (playerPositionSupplier != null)
            playerPos = playerPositionSupplier.get();

        if (playerPos == null)
            playerPos = new Vector3f(0.0f, 0.0f, 0.0f);

        int bestIndex = -1;
        float bestDistSq = Float.MAX_VALUE;

        for (int i = 0; i < UFO_DROP_POSITIONS.length; i++)
        {
            if (usedUfoDrops[i])
                continue;

            Vector3f drop = UFO_DROP_POSITIONS[i];

            float dx = drop.x - playerPos.x;
            float dz = drop.z - playerPos.z;
            float distSq = dx * dx + dz * dz;

            if (distSq < bestDistSq)
            {
                bestDistSq = distSq;
                bestIndex = i;
            }
        }

        return bestIndex;
    }

    private void spawnWave(Vector3f pos, int apeCount, int ufoIndex)
    {
        active = true;
        waveDropFinished = false;
        activeUfoTarget.set(pos);
        activeUfoTravelTime = 0.0f;
        activeUfoDropCount = apeCount;
        lastWaveTarget.set(pos);
        lastWaveApeCount = apeCount;
        lastWaveUfoIndex = ufoIndex;

        if (apeCount == 10)
        {
            activeUfo = largeUfo;

            if (activeUfo != null)
                activeUfo.setLocalScale(new Matrix4f().scaling(0.25f));
        }
        else
        {
            if (ufoIndex < 0 || ufoIndex >= ufos.length)
            {
                active = false;
                activeUfo = null;
                return;
            }

            activeUfo = ufos[ufoIndex];

            if (activeUfo != null)
                activeUfo.setLocalScale(new Matrix4f().scaling(0.05f));
        }

        if (activeUfo == null)
        {
            active = false;
            return;
        }

        activeUfoStart.set(getStartPosition(pos));
        activeUfo.setLocalTranslation(new Matrix4f().translation(activeUfoStart));
    }

    private Vector3f getStartPosition(Vector3f target)
    {
        float startX = target.x < 0 ? -140.0f : 140.0f;
        float startZ = target.z < 0 ? -140.0f : 140.0f;
        return new Vector3f(startX, target.y + 25.0f, startZ);
    }

    private void dropApes(Vector3f dropPos, int apeCount)
    {
        for (int i = 0; i < apeCount; i++)
        {
            float offsetX = ((float)org.joml.Math.random() - 0.5f) * 8.0f;
            float offsetZ = ((float)org.joml.Math.random() - 0.5f) * 8.0f;

            apeSpawner.accept(new Vector3f(dropPos.x + offsetX, dropPos.y, dropPos.z + offsetZ));
        }
    }

    public void hideAllUfos()
    {
        active = false;
        activeUfo = null;
        waveDropFinished = false;

        if (largeUfo != null)
        {
            largeUfo.setLocalScale(new Matrix4f().scaling(0.0001f));
            largeUfo.setLocalTranslation(new Matrix4f().translation(9999.0f, 9999.0f, 9999.0f));
        }

        if (ufos != null)
        {
            for (GameObject ufo : ufos)
            {
                if (ufo != null)
                {
                    ufo.setLocalScale(new Matrix4f().scaling(0.0001f));
                    ufo.setLocalTranslation(new Matrix4f().translation(9999.0f, 9999.0f, 9999.0f));
                }
            }
        }
    }

    public void resetWaves()
    {
        active = false;
        activeUfo = null;
        waveDropFinished = false;
        activeUfoTravelTime = 0.0f;
        activeUfoDropCount = 0;
        currentWave = 0;

        for (int i = 0; i < usedUfoDrops.length; i++)
            usedUfoDrops[i] = false;
    }

    // DEBUG METHOD
    public void debugPlaceLargeUfo()
    {
        if (largeUfo == null) return;

        Vector3f pos = new Vector3f(
            LARGE_UFO_DROP_POSITION.x,
            LARGE_UFO_DROP_POSITION.y + 18.0f,
            LARGE_UFO_DROP_POSITION.z
        );

        largeUfo.setLocalScale(new Matrix4f().scaling(0.25f));
        largeUfo.setLocalTranslation(new Matrix4f().translation(pos));
    }
}
