package bullet.combat;

public class WeaponInventory
{
    public static final int MAX_DAMAGE_UPGRADE_LEVEL = 4;
    public static final int MAX_AMMO_UPGRADE_LEVEL = 4;

    private static final float DAMAGE_MULTIPLIER_STEP = 0.25f;
    private static final float AMMO_MULTIPLIER_STEP = 0.25f;
    private static final int BASE_ENEMY_BULLET_DAMAGE = 100;
    private static final int BASE_BRAIN_BULLET_DAMAGE = 20;

    private WeaponType currentWeapon = WeaponType.KNIFE;
    private boolean plasmaBurstMode = true;

    private final int[] magazineAmmo = new int[WeaponType.COUNT];
    private final int[] reserveAmmo = new int[WeaponType.COUNT];

    private int damageUpgradeLevel = 0;
    private int ammoUpgradeLevel = 0;

    private boolean reloading = false;
    private WeaponType reloadingWeapon = null;
    private float reloadTimer = 0.0f;
    private float fireCooldown = 0.0f;

    public WeaponInventory()
    {
        reset();
    }

    public void reset()
    {
        damageUpgradeLevel = 0;
        ammoUpgradeLevel = 0;

        for (WeaponType weapon : WeaponType.values())
        {
            magazineAmmo[weapon.ordinal()] = getMagazineCapacity(weapon);
            reserveAmmo[weapon.ordinal()] = getInitialReserve(weapon);
        }

        currentWeapon = WeaponType.KNIFE;
        plasmaBurstMode = true;
        reloading = false;
        reloadingWeapon = null;
        reloadTimer = 0.0f;
        fireCooldown = 0.0f;
    }

    public void updateTimers(float dt)
    {
        if (fireCooldown > 0.0f)
        {
            fireCooldown -= dt;
            if (fireCooldown < 0.0f)
                fireCooldown = 0.0f;
        }

        if (reloading)
        {
            reloadTimer -= dt;
            if (reloadTimer <= 0.0f)
                finishReload();
        }
    }

    public WeaponType getCurrentWeapon()
    {
        return currentWeapon;
    }

    public void selectNext()
    {
        currentWeapon = currentWeapon.next();
        cancelReload();
    }

    public void selectPrevious()
    {
        currentWeapon = currentWeapon.previous();
        cancelReload();
    }

    public boolean currentUsesBullets()
    {
        return currentWeapon.usesBullets();
    }

    public boolean isReloading()
    {
        return reloading;
    }

    public boolean isCoolingDown()
    {
        return fireCooldown > 0.0f;
    }

    public boolean isAutomaticWeapon()
    {
        return currentWeapon == WeaponType.RIFLE ||
            (currentWeapon == WeaponType.PLASMA_RIFLE && !plasmaBurstMode);
    }

    public boolean isPlasmaBurstMode()
    {
        return plasmaBurstMode;
    }

    public boolean togglePlasmaFireMode()
    {
        if (currentWeapon != WeaponType.PLASMA_RIFLE)
            return false;

        plasmaBurstMode = !plasmaBurstMode;
        return true;
    }

    public int getCurrentMagazineAmmo()
    {
        return currentUsesBullets() ? magazineAmmo[currentWeapon.ordinal()] : 0;
    }

    public int getCurrentReserveAmmo()
    {
        return currentUsesBullets() ? reserveAmmo[currentWeapon.ordinal()] : 0;
    }

    public int getTotalAmmo()
    {
        return getCurrentMagazineAmmo() + getCurrentReserveAmmo();
    }

    public int getDamageUpgradeLevel()
    {
        return damageUpgradeLevel;
    }

    public int getAmmoUpgradeLevel()
    {
        return ammoUpgradeLevel;
    }

    public boolean isDamageUpgradeMaxed()
    {
        return damageUpgradeLevel >= MAX_DAMAGE_UPGRADE_LEVEL;
    }

    public boolean isAmmoUpgradeMaxed()
    {
        return ammoUpgradeLevel >= MAX_AMMO_UPGRADE_LEVEL;
    }

    public float getDamageMultiplier()
    {
        return 1.0f + (damageUpgradeLevel * DAMAGE_MULTIPLIER_STEP);
    }

    public float getAmmoMultiplier()
    {
        return 1.0f + (ammoUpgradeLevel * AMMO_MULTIPLIER_STEP);
    }

    public boolean upgradeDamageMultiplier()
    {
        if (isDamageUpgradeMaxed())
            return false;

        damageUpgradeLevel++;
        return true;
    }

    public boolean upgradeAmmoMultiplier()
    {
        if (isAmmoUpgradeMaxed())
            return false;

        float oldMultiplier = getAmmoMultiplier();
        ammoUpgradeLevel++;
        float newMultiplier = getAmmoMultiplier();

        for (WeaponType weapon : WeaponType.values())
        {
            if (!weapon.usesBullets())
                continue;

            int weaponIndex = weapon.ordinal();
            int oldMagazineCapacity = getScaledAmount(weapon.getMagazineCapacity(), oldMultiplier);
            int oldReserveCapacity = getScaledAmount(weapon.getReserveCapacity(), oldMultiplier);
            int newMagazineCapacity = getScaledAmount(weapon.getMagazineCapacity(), newMultiplier);
            int newReserveCapacity = getScaledAmount(weapon.getReserveCapacity(), newMultiplier);

            magazineAmmo[weaponIndex] = java.lang.Math.min(
                newMagazineCapacity,
                magazineAmmo[weaponIndex] + (newMagazineCapacity - oldMagazineCapacity)
            );
            reserveAmmo[weaponIndex] = java.lang.Math.min(
                newReserveCapacity,
                reserveAmmo[weaponIndex] + (newReserveCapacity - oldReserveCapacity)
            );
        }

        return true;
    }

    public int scaleDamage(int baseDamage)
    {
        return java.lang.Math.max(1, java.lang.Math.round(baseDamage * getDamageMultiplier()));
    }

    public int getCurrentEnemyBulletDamage()
    {
        return scaleDamage(BASE_ENEMY_BULLET_DAMAGE);
    }

    public int getCurrentBrainBulletDamage()
    {
        return scaleDamage(BASE_BRAIN_BULLET_DAMAGE);
    }

    public void addAmmoPickupBundle()
    {
        for (WeaponType weapon : WeaponType.values())
        {
            if (weapon.usesBullets())
                addReserveAmmo(weapon, getPickupAmount(weapon));
        }
    }

    public void beginReload()
    {
        if (!currentUsesBullets()) return;
        if (reloading && reloadingWeapon == currentWeapon) return;
        if (magazineAmmo[currentWeapon.ordinal()] >= getMagazineCapacity(currentWeapon)) return;
        if (reserveAmmo[currentWeapon.ordinal()] <= 0) return;

        reloading = true;
        reloadingWeapon = currentWeapon;
        reloadTimer = currentWeapon.getReloadTime();
    }

    public void cancelReload()
    {
        reloading = false;
        reloadingWeapon = null;
        reloadTimer = 0.0f;
    }

    public void consumeCurrentRound()
    {
        if (!currentUsesBullets()) return;

        int weaponIndex = currentWeapon.ordinal();
        magazineAmmo[weaponIndex] = java.lang.Math.max(0, magazineAmmo[weaponIndex] - 1);
    }

    public void startFireCooldown()
    {
        fireCooldown = currentWeapon.getFireDelay();
    }

    public String getHudText()
    {
        if (!currentUsesBullets())
            return "Weapon: " + currentWeapon.getDisplayName();

        String status = reloading ? "  Reloading..." : "";
        String modeText = "";

        if (currentWeapon == WeaponType.PLASMA_RIFLE)
            modeText = plasmaBurstMode ? "  [BURST]" : "  [AUTO]";

        return currentWeapon.getDisplayName() + " Ammo: " +
            getCurrentMagazineAmmo() + "/" + getCurrentReserveAmmo() + modeText + status;
    }

    public void addReserveAmmo(WeaponType weapon, int amount)
    {
        if (!weapon.usesBullets() || amount <= 0) return;

        int weaponIndex = weapon.ordinal();
        reserveAmmo[weaponIndex] = java.lang.Math.min(
            getReserveCapacity(weapon),
            reserveAmmo[weaponIndex] + amount
        );
    }

    public void fillAllAmmo()
    {
        for (WeaponType weapon : WeaponType.values())
        {
            if (!weapon.usesBullets())
                continue;

            int weaponIndex = weapon.ordinal();
            magazineAmmo[weaponIndex] = getMagazineCapacity(weapon);
            reserveAmmo[weaponIndex] = getReserveCapacity(weapon);
        }

        cancelReload();
    }

    private void finishReload()
    {
        if (reloadingWeapon == null || !reloadingWeapon.usesBullets())
        {
            cancelReload();
            return;
        }

        int weaponIndex = reloadingWeapon.ordinal();
        int ammoNeeded = getMagazineCapacity(reloadingWeapon) - magazineAmmo[weaponIndex];
        int ammoToLoad = java.lang.Math.min(ammoNeeded, reserveAmmo[weaponIndex]);

        magazineAmmo[weaponIndex] += ammoToLoad;
        reserveAmmo[weaponIndex] -= ammoToLoad;

        cancelReload();
    }

    private int getMagazineCapacity(WeaponType weapon)
    {
        return getScaledAmount(weapon.getMagazineCapacity(), getAmmoMultiplier());
    }

    private int getReserveCapacity(WeaponType weapon)
    {
        return getScaledAmount(weapon.getReserveCapacity(), getAmmoMultiplier());
    }

    private int getInitialReserve(WeaponType weapon)
    {
        return getScaledAmount(weapon.getInitialReserve(), getAmmoMultiplier());
    }

    private int getPickupAmount(WeaponType weapon)
    {
        return getScaledAmount(weapon.getPickupAmount(), getAmmoMultiplier());
    }

    private int getScaledAmount(int baseAmount, float multiplier)
    {
        if (baseAmount <= 0)
            return 0;

        return java.lang.Math.max(1, java.lang.Math.round(baseAmount * multiplier));
    }
}
