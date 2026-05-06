package bullet;

public class WeaponInventory
{
    private WeaponType currentWeapon = WeaponType.KNIFE;
    private boolean plasmaBurstMode = true;

    private final int[] magazineAmmo = new int[WeaponType.COUNT];
    private final int[] reserveAmmo = new int[WeaponType.COUNT];

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
        for (WeaponType weapon : WeaponType.values())
        {
            magazineAmmo[weapon.ordinal()] = weapon.getMagazineCapacity();
            reserveAmmo[weapon.ordinal()] = weapon.getInitialReserve();
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

    public void addAmmoPickupBundle()
    {
        for (WeaponType weapon : WeaponType.values())
        {
            if (weapon.usesBullets())
                addReserveAmmo(weapon, weapon.getPickupAmount());
        }
    }

    public void beginReload()
    {
        if (!currentUsesBullets()) return;
        if (reloading && reloadingWeapon == currentWeapon) return;
        if (magazineAmmo[currentWeapon.ordinal()] >= currentWeapon.getMagazineCapacity()) return;
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

    private void addReserveAmmo(WeaponType weapon, int amount)
    {
        if (!weapon.usesBullets() || amount <= 0) return;

        int weaponIndex = weapon.ordinal();
        reserveAmmo[weaponIndex] = java.lang.Math.min(
            weapon.getReserveCapacity(),
            reserveAmmo[weaponIndex] + amount
        );
    }

    private void finishReload()
    {
        if (reloadingWeapon == null || !reloadingWeapon.usesBullets())
        {
            cancelReload();
            return;
        }

        int weaponIndex = reloadingWeapon.ordinal();
        int ammoNeeded = reloadingWeapon.getMagazineCapacity() - magazineAmmo[weaponIndex];
        int ammoToLoad = java.lang.Math.min(ammoNeeded, reserveAmmo[weaponIndex]);

        magazineAmmo[weaponIndex] += ammoToLoad;
        reserveAmmo[weaponIndex] -= ammoToLoad;

        cancelReload();
    }
}
