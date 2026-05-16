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
import tage.shapes.ImportedModel;

public class ShopState
{
    public static final float VENDING_TERRAIN_Y_OFFSET = 0.5f;

    private static final float VENDING_WINDOW_DURATION = 15.0f;
    private static final float INTERACTION_DISTANCE = 5.0f;
    private static final float HIDDEN_SCALE = 0.0001f;
    private static final int FULL_HEAL_COST = 50;
    private static final int FULL_AMMO_COST = 150;
    private static final int DAMAGE_UPGRADE_BASE_COST = 250;
    private static final int DAMAGE_UPGRADE_COST_STEP = 150;
    private static final int AMMO_UPGRADE_BASE_COST = 200;
    private static final int AMMO_UPGRADE_COST_STEP = 100;

    private ObjShape vendingShape;
    private TextureImage vendingTexture;
    private GameObject vendingMachine;

    private float shopTimer = 0.0f;
    private boolean vendingActive = false;
    private boolean shopOpen = false;

    public void loadTexture()
    {
        vendingTexture = new TextureImage("uv_checker_material_uv_grid_2048x2048_Roug.png");
    }

    public void loadShape()
    {
        vendingShape = new ImportedModel("Terminal UV_1.obj");
    }

    public void buildObjects(TextureImage vendingTexture)
    {
        if (vendingShape == null)
            loadShape();

        if (this.vendingTexture == null)
            loadTexture();

        TextureImage texture = this.vendingTexture != null ? this.vendingTexture : vendingTexture;
        vendingMachine = new GameObject(GameObject.root(), vendingShape, texture);
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
            new Matrix4f().scaling(1f, 1f, 1f)
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

            case KeyEvent.VK_3:
                buyDamageUpgrade(game, weapons);
                break;

            case KeyEvent.VK_4:
                buyAmmoUpgrade(game, weapons);
                break;

            default:
                break;
        }
    }

    public void renderHud(Engine engine, int playerHealth, int playerCredits, WeaponInventory weapons)
    {
        if (shopOpen)
        {
            renderShopHud(engine, playerCredits, weapons);
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

    private void renderShopHud(Engine engine, int playerCredits, WeaponInventory weapons)
    {
        engine.getHUDmanager().setHUD1(
            "--- VENDING MACHINE SHOP --- Credits: $" + playerCredits,
            new Vector3f(1, 1, 0),
            15,
            660
        );

        engine.getHUDmanager().setHUD2(
            "[1] Full Heal ($" + FULL_HEAL_COST + ")  |  [2] Full Ammo ($" + FULL_AMMO_COST + ")",
            new Vector3f(1, 1, 1),
            15,
            630
        );

        engine.getHUDmanager().setHUD3(
            "[3] " + getDamageUpgradeText(weapons) + "  |  [4] " + getAmmoUpgradeText(weapons) + "  |  E/ESC Exit",
            new Vector3f(1, 0, 0),
            15,
            600
        );
    }

    private void buyFullHeal(MyGame game)
    {
        if (game.getPlayerHealth() < game.getPlayerHealthMax() && game.spendPlayerCredits(FULL_HEAL_COST))
            game.setPlayerHealth(game.getPlayerHealthMax());
    }

    private void buyFullAmmo(MyGame game, WeaponInventory weapons)
    {
        if (game.spendPlayerCredits(FULL_AMMO_COST))
            weapons.fillAllAmmo();
    }

    private void buyDamageUpgrade(MyGame game, WeaponInventory weapons)
    {
        if (weapons == null || weapons.isDamageUpgradeMaxed())
            return;

        if (game.spendPlayerCredits(getDamageUpgradeCost(weapons)))
            weapons.upgradeDamageMultiplier();
    }

    private void buyAmmoUpgrade(MyGame game, WeaponInventory weapons)
    {
        if (weapons == null || weapons.isAmmoUpgradeMaxed())
            return;

        if (game.spendPlayerCredits(getAmmoUpgradeCost(weapons)))
            weapons.upgradeAmmoMultiplier();
    }

    private String getDamageUpgradeText(WeaponInventory weapons)
    {
        if (weapons == null)
            return "Damage Upgrade";

        String levelText = "Damage x" + formatMultiplier(weapons.getDamageMultiplier()) +
            " L" + weapons.getDamageUpgradeLevel() + "/" + WeaponInventory.MAX_DAMAGE_UPGRADE_LEVEL;

        if (weapons.isDamageUpgradeMaxed())
            return levelText + " MAX";

        return levelText + " ($" + getDamageUpgradeCost(weapons) + ")";
    }

    private String getAmmoUpgradeText(WeaponInventory weapons)
    {
        if (weapons == null)
            return "Ammo Upgrade";

        String levelText = "Ammo x" + formatMultiplier(weapons.getAmmoMultiplier()) +
            " L" + weapons.getAmmoUpgradeLevel() + "/" + WeaponInventory.MAX_AMMO_UPGRADE_LEVEL;

        if (weapons.isAmmoUpgradeMaxed())
            return levelText + " MAX";

        return levelText + " ($" + getAmmoUpgradeCost(weapons) + ")";
    }

    private int getDamageUpgradeCost(WeaponInventory weapons)
    {
        return DAMAGE_UPGRADE_BASE_COST + (weapons.getDamageUpgradeLevel() * DAMAGE_UPGRADE_COST_STEP);
    }

    private int getAmmoUpgradeCost(WeaponInventory weapons)
    {
        return AMMO_UPGRADE_BASE_COST + (weapons.getAmmoUpgradeLevel() * AMMO_UPGRADE_COST_STEP);
    }

    private String formatMultiplier(float value)
    {
        return String.format(java.util.Locale.US, "%.2f", value);
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
