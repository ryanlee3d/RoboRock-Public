package bullet.states;

import java.awt.event.KeyEvent;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import bullet.MyGame;
import bullet.combat.WeaponInventory;
import tage.Engine;
import tage.GameObject;
import tage.ObjShape;
import tage.TextureImage;
import tage.physics.PhysicsObject;
import tage.shapes.Cube;

public class ShopState
{
    public static final float VENDING_TERRAIN_Y_OFFSET = 1.25f;

    private static final float VENDING_WINDOW_DURATION = 15.0f;
    private static final float INTERACTION_DISTANCE = 5.0f;
    private static final float HIDDEN_SCALE = 0.0001f;

    private ObjShape vendingShape;
    private GameObject vendingMachine;

    private float shopTimer = 0.0f;
    private boolean vendingActive = false;
    private boolean shopOpen = false;

    public void loadShape()
    {
        vendingShape = new Cube();
    }

    public void buildObjects(TextureImage vendingTexture)
    {
        if (vendingShape == null)
            loadShape();

        vendingMachine = new GameObject(GameObject.root(), vendingShape, vendingTexture);
        hideVendingMachine();
    }

    public GameObject getVendingMachine()
    {
        return vendingMachine;
    }

    public boolean isActive()
    {
        return vendingActive;
    }

    public boolean isShopOpen()
    {
        return shopOpen;
    }

    public void startWindow(Vector3f pos, GameObject terrain)
    {
        if (vendingMachine == null || pos == null)
            return;

        float y = pos.y;

        if (terrain != null)
            y = terrain.getHeight(pos.x, pos.z) + VENDING_TERRAIN_Y_OFFSET;

        vendingMachine.setLocalTranslation(
            new Matrix4f().translation(pos.x, y, pos.z)
        );

        vendingMachine.setLocalScale(
            new Matrix4f().scaling(1.5f, 2.5f, 1.5f)
        );

        shopTimer = VENDING_WINDOW_DURATION;
        vendingActive = true;
        shopOpen = false;
    }

    public boolean update(float dt)
    {
        if (!vendingActive)
            return false;

        shopTimer -= dt;

        if (shopTimer <= 0.0f)
        {
            shopTimer = 0.0f;
            hideVendingMachine();
            return true;
        }

        return false;
    }

    public boolean tryToggleShop(GameObject player, PhysicsObject playerPhysics, Vector3f currentMoveDir)
    {
        if (!vendingActive || vendingMachine == null || player == null)
            return false;

        if (player.getWorldLocation().distance(vendingMachine.getWorldLocation()) > INTERACTION_DISTANCE)
            return false;

        shopOpen = true;

        if (shopOpen)
        {
            if (playerPhysics != null)
                playerPhysics.setLinearVelocity(new float[] { 0f, playerPhysics.getLinearVelocity()[1], 0f });

            if (currentMoveDir != null)
                currentMoveDir.set(0, 0, 0);
        }

        return true;
    }

    public void closeShop()
    {
        shopOpen = false;
    }

    public void handleShopKey(int keyCode, MyGame game, WeaponInventory weapons)
    {
        switch (keyCode)
        {
            case KeyEvent.VK_E:
            case KeyEvent.VK_ESCAPE:
                closeShop();
                break;

            case KeyEvent.VK_1:
                buyFullHeal(game);
                break;

            case KeyEvent.VK_2:
                buyFullAmmo(game, weapons);
                break;

            default:
                break;
        }
    }

    public void renderHud(Engine engine, int playerHealth, int playerCredits)
    {
        if (shopOpen)
        {
            renderShopHud(engine, playerCredits);
            return;
        }

        engine.getHUDmanager().setHUD1(
            "Vending Machine available: " + (int)shopTimer + "s",
            new Vector3f(0, 1, 1),
            15,
            660
        );

        engine.getHUDmanager().setHUD2(
            "Press E near the vending machine to shop",
            new Vector3f(1, 1, 1),
            15,
            630
        );

        engine.getHUDmanager().setHUD3(
            "Health: " + playerHealth + " | Credits: $" + playerCredits,
            new Vector3f(0, 1, 0),
            15,
            600
        );
    }

    private void renderShopHud(Engine engine, int playerCredits)
    {
        engine.getHUDmanager().setHUD1(
            "--- VENDING MACHINE SHOP --- Credits: $" + playerCredits,
            new Vector3f(1, 1, 0),
            15,
            660
        );

        engine.getHUDmanager().setHUD2(
            "[1] Full Heal ($50)  |  [2] Full Ammo ($150)",
            new Vector3f(1, 1, 1),
            15,
            630
        );

        engine.getHUDmanager().setHUD3(
            "Press E or ESC to exit shop",
            new Vector3f(1, 0, 0),
            15,
            600
        );
    }

    private void buyFullHeal(MyGame game)
    {
        if (game.getPlayerHealth() < game.getPlayerHealthMax() && game.spendPlayerCredits(50))
            game.setPlayerHealth(game.getPlayerHealthMax());
    }

    private void buyFullAmmo(MyGame game, WeaponInventory weapons)
    {
        if (game.spendPlayerCredits(150))
            weapons.fillAllAmmo();
    }

    private void hideVendingMachine()
    {
        vendingActive = false;
        shopOpen = false;

        if (vendingMachine != null)
        {
            vendingMachine.setLocalScale(new Matrix4f().scaling(HIDDEN_SCALE));
            vendingMachine.setLocalTranslation(new Matrix4f().translation(9999.0f, 9999.0f, 9999.0f));
        }
    }
}