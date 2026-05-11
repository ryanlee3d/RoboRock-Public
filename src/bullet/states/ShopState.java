package bullet.states;

import java.awt.event.KeyEvent;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import bullet.MyGame;
import bullet.combat.WeaponInventory;
import bullet.combat.WeaponType;
import bullet.managers.UfoWaveManager;
import tage.Engine;
import tage.GameObject;
import tage.ObjShape;
import tage.TextureImage;
import tage.physics.PhysicsObject;
import tage.shapes.Cube;

public class ShopState
{
    public static final float VENDING_TERRAIN_Y_OFFSET = 1.25f;

    private static final float BETWEEN_WAVE_DURATION = 15.0f;
    private static final float INTERACTION_DISTANCE = 5.0f;
    private static final float VENDING_X = -55.0f;
    private static final float VENDING_Y = 1.5f;
    private static final float VENDING_Z = 90.0f;

    private ObjShape vendingShape;
    private GameObject vendingMachine;
    private float shopTimer = 0.0f;

    public void loadShape()
    {
        vendingShape = new Cube();
    }

    public void buildObjects(TextureImage vendingTexture)
    {
        if (vendingShape == null)
            loadShape();

        vendingMachine = new GameObject(GameObject.root(), vendingShape, vendingTexture);
        vendingMachine.setLocalTranslation(new Matrix4f().translation(VENDING_X, VENDING_Y, VENDING_Z));
        vendingMachine.setLocalScale(new Matrix4f().scaling(1.5f, 2.5f, 1.5f));
    }

    public GameObject getVendingMachine()
    {
        return vendingMachine;
    }

    public GameState updateWaveTimer(float dt, GameState gameState, boolean waveActive, int activeApeCount, UfoWaveManager waveManager)
    {
        if (gameState == GameState.PLAYING && !waveActive && activeApeCount == 0)
        {
            shopTimer = BETWEEN_WAVE_DURATION;
            return GameState.BETWEEN_WAVES;
        }

        if (gameState == GameState.BETWEEN_WAVES)
        {
            shopTimer -= dt;
            if (shopTimer <= 0.0f)
            {
                shopTimer = 0.0f;
                waveManager.startNextWave();
                return GameState.PLAYING;
            }
        }

        return gameState;
    }

    public void renderShopHud(Engine engine, int playerCredits)
    {
        engine.getHUDmanager().setHUD1("--- VENDING MACHINE SHOP --- Credits: $" + playerCredits, new Vector3f(1, 1, 0), 15, 660);
        engine.getHUDmanager().setHUD2("[1] Full Heal (50c)  |  [2] Rifle Ammo (100c)  |  [3] Shotgun Ammo (150c)", new Vector3f(1, 1, 1), 15, 630);
        engine.getHUDmanager().setHUD3("Press 'E' to Exit Shop", new Vector3f(1, 0, 0), 15, 600);
    }

    public void renderBetweenWaveHud(Engine engine, int playerHealth, int playerCredits)
    {
        engine.getHUDmanager().setHUD1("Next Wave in: " + (int)shopTimer + "s", new Vector3f(0, 1, 1), 15, 660);
        engine.getHUDmanager().setHUD2("Find the Vending Machine to buy upgrades!", new Vector3f(1, 1, 1), 15, 630);
        engine.getHUDmanager().setHUD3("Health: " + playerHealth + " | Credits: $" + playerCredits, new Vector3f(0, 1, 0), 15, 600);
    }

    public GameState handleWorldKey(int keyCode, GameState gameState, GameObject player, PhysicsObject playerPhysics, Vector3f currentMoveDir)
    {
        if (keyCode != KeyEvent.VK_E || vendingMachine == null || player == null)
            return gameState;

        if (player.getWorldLocation().distance(vendingMachine.getWorldLocation()) >= INTERACTION_DISTANCE)
            return gameState;

        if (playerPhysics != null)
            playerPhysics.setLinearVelocity(new float[] { 0f, playerPhysics.getLinearVelocity()[1], 0f });

        currentMoveDir.set(0, 0, 0);
        return GameState.SHOP;
    }

    public GameState handleShopKey(int keyCode, GameState gameState, MyGame game, WeaponInventory weapons)
    {
        switch (keyCode)
        {
            case KeyEvent.VK_E:
            case KeyEvent.VK_ESCAPE:
                return getResumeState();
            case KeyEvent.VK_1:
                buyFullHeal(game);
                break;
            case KeyEvent.VK_2:
                buyAmmo(game, weapons, WeaponType.RIFLE, 100, 60);
                break;
            case KeyEvent.VK_3:
                buyAmmo(game, weapons, WeaponType.SHOTGUN, 150, 20);
                break;
            case KeyEvent.VK_4:
                buyPowerUp(game, weapons, WeaponType.SHOTGUN, 200);
                break;
            default:
                break;
        }

        return gameState;
    }

    public GameState getResumeState()
    {
        return (shopTimer > 0.0f) ? GameState.BETWEEN_WAVES : GameState.PLAYING;
    }

    private void buyFullHeal(MyGame game)
    {
        if (game.getPlayerHealth() < game.getPlayerHealthMax() && game.spendPlayerCredits(50))
            game.setPlayerHealth(game.getPlayerHealthMax());
    }

    private void buyAmmo(MyGame game, WeaponInventory weapons, WeaponType weapon, int cost, int amount)
    {
        if (game.spendPlayerCredits(cost))
            weapons.addReserveAmmo(weapon, amount);
    }

    private void buyPowerUp(MyGame game, WeaponInventory weapons, WeaponType weapon, int cost)
    {
    }
}
