package bullet.managers;

import java.util.function.Consumer;
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

    private GameObject[] ufos = new GameObject[UFO_DROP_POSITIONS.length];
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

    public UfoWaveManager(Consumer<Vector3f> apeSpawner)
    {
        this.apeSpawner = apeSpawner;
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
        int maxWaves = UFO_DROP_POSITIONS.length;
        int dropIndex = currentWave % maxWaves;
        int apeCount = 5 + (currentWave * 2); // Scale difficulty with wave number

        if ((currentWave + 1) % 5 == 0) // Boss wave every 5th wave
            spawnWave(LARGE_UFO_DROP_POSITION, apeCount + 5, true);
        else
            spawnWave(UFO_DROP_POSITIONS[dropIndex], apeCount, false);

        currentWave++;
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
            dropApes(activeUfoTarget, activeUfoDropCount);
            waveDropFinished = true;
            active = false;
            activeUfo.setLocalTranslation(new Matrix4f().translation(9999.0f, 9999.0f, 9999.0f));
        }
    }

    private void spawnWave(Vector3f pos, int apeCount, boolean isLarge)
    {
        active = true;
        waveDropFinished = false;
        activeUfoTarget.set(pos);
        activeUfoTravelTime = 0.0f;
        activeUfoDropCount = apeCount;

        activeUfo = isLarge ? largeUfo : ufos[currentWave % ufos.length];
        activeUfo.setLocalScale(new Matrix4f().scaling(isLarge ? 0.25f : 0.05f));
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
}
