package bullet;

public enum WeaponType
{
    KNIFE("Knife", 0, 0, 0, 0, 999.0f, 0.0f),
    PISTOL("Pistol", 12, 48, 12, 24, 0.25f, 1.20f),
    PLASMA_RIFLE("Plasma Rifle", 20, 80, 20, 40, 0.18f, 1.60f),
    RIFLE("Machine Gun", 30, 120, 30, 60, 0.10f, 1.40f),
    SHOTGUN("Shotgun", 5, 25, 5, 10, 0.55f, 1.80f);

    static final int COUNT = values().length;

    private final String displayName;
    private final int magazineCapacity;
    private final int reserveCapacity;
    private final int pickupAmount;
    private final int initialReserve;
    private final float fireDelay;
    private final float reloadTime;

    WeaponType(
        String displayName,
        int magazineCapacity,
        int reserveCapacity,
        int pickupAmount,
        int initialReserve,
        float fireDelay,
        float reloadTime)
    {
        this.displayName = displayName;
        this.magazineCapacity = magazineCapacity;
        this.reserveCapacity = reserveCapacity;
        this.pickupAmount = pickupAmount;
        this.initialReserve = initialReserve;
        this.fireDelay = fireDelay;
        this.reloadTime = reloadTime;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public int getMagazineCapacity()
    {
        return magazineCapacity;
    }

    public int getReserveCapacity()
    {
        return reserveCapacity;
    }

    public int getPickupAmount()
    {
        return pickupAmount;
    }

    public int getInitialReserve()
    {
        return initialReserve;
    }

    public float getFireDelay()
    {
        return fireDelay;
    }

    public float getReloadTime()
    {
        return reloadTime;
    }

    public boolean usesBullets()
    {
        return magazineCapacity > 0;
    }

    public WeaponType next()
    {
        WeaponType[] weapons = values();
        return weapons[(ordinal() + 1) % weapons.length];
    }

    public WeaponType previous()
    {
        WeaponType[] weapons = values();
        return weapons[(ordinal() - 1 + weapons.length) % weapons.length];
    }
}
