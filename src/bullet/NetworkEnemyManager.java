package bullet;

import java.util.HashMap;
import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import tage.GameObject;
import tage.ObjShape;
import tage.TextureImage;
import tage.shapes.AnimatedShape;

public class NetworkEnemyManager
{
    private MyGame game;
    private Map<Integer, GameObject> enemies = new HashMap<>();
    private Map<Integer, Boolean> enemyDeadStates = new HashMap<>();
    private Map<Integer, Boolean> enemyMovingStates = new HashMap<>();
    private Map<Integer, Vector3f> enemyLastPositions = new HashMap<>();

    public NetworkEnemyManager(MyGame game)
    {
        this.game = game;
    }

    public void updateEnemy(int enemyID, String type, Vector3f pos, float yaw, int health, boolean dead)
    {
        if (!type.equals("APE"))
            return;

        ObjShape shape = game.getApeShape();
        TextureImage texture = game.getApeTexture();

        if (shape == null || texture == null)
            return;

        GameObject enemy = enemies.get(enemyID);
        Vector3f previousPos = enemyLastPositions.get(enemyID);
        boolean moving = previousPos == null || previousPos.distance(pos) > 0.02f;

        if (enemy == null)
        {
            enemy = new GameObject(GameObject.root(), shape, texture);
            enemy.setLocalScale(new Matrix4f().scaling(0.01f));

            enemies.put(enemyID, enemy);
            enemyDeadStates.put(enemyID, false);
            enemyMovingStates.put(enemyID, false);
        }

        boolean wasDead = enemyDeadStates.getOrDefault(enemyID, false);
        boolean wasMoving = enemyMovingStates.getOrDefault(enemyID, false);

        if (shape instanceof AnimatedShape)
        {
            AnimatedShape animShape = (AnimatedShape) shape;

            if (dead)
            {
                if (!wasDead)
                    animShape.playAnimation("DIE", 0.3f, AnimatedShape.EndType.STOP, 0);
            }
            else if (moving && (!wasMoving || wasDead))
            {
                animShape.playAnimation("RUN", 0.3f, AnimatedShape.EndType.LOOP, 0);
            }
        }

        enemy.setLocalTranslation(new Matrix4f().translation(pos));
        enemy.setLocalRotation(new Matrix4f().rotationY(yaw));

        if (dead)
        {
            enemy.getRenderStates().setModelOrientationCorrection(
                new Matrix4f()
                    .rotationX((float)java.lang.Math.toRadians(90.0f))
                    .rotateZ((float)java.lang.Math.toRadians(270.0f))
            );
        }
        else
        {
            enemy.getRenderStates().setModelOrientationCorrection(
                new Matrix4f()
                    .rotationX((float)java.lang.Math.toRadians(90.0f))
                    .rotateZ((float)java.lang.Math.toRadians(180.0f))
            );
        }

        enemyDeadStates.put(enemyID, dead);
        enemyMovingStates.put(enemyID, moving && !dead);
        enemyLastPositions.put(enemyID, new Vector3f(pos));
    }

    public void removeEnemy(int enemyID)
    {
        GameObject enemy = enemies.remove(enemyID);
        enemyDeadStates.remove(enemyID);
        enemyMovingStates.remove(enemyID);
        enemyLastPositions.remove(enemyID);

        if (enemy != null)
            game.getEngine().getSceneGraph().removeGameObject(enemy);
    }

    public void clear()
    {
        for (GameObject enemy : enemies.values())
        {
            if (enemy != null)
                game.getEngine().getSceneGraph().removeGameObject(enemy);
        }

        enemies.clear();
        enemyDeadStates.clear();
        enemyMovingStates.clear();
        enemyLastPositions.clear();
    }
}
