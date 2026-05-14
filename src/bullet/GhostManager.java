package bullet;

import java.util.*;

import org.joml.*;

import tage.*;

import tage.shapes.*;

public class GhostManager
{
    private MyGame game;
    private Vector<GhostAvatar> ghostAvs = new Vector<>();
    private Map<UUID, GameObject> ghostWeapons = new HashMap<>();
    private Map<UUID, Integer> ghostWeaponIndexes = new HashMap<>();

    public GhostManager(MyGame g)
    {
        game = g;
    }

    private void updateGhostWeapon(GhostAvatar ghost, UUID id, int weaponIndex)
    {
        if (ghost == null) return;

        if (weaponIndex < 0 || weaponIndex >= WeaponType.COUNT)
            weaponIndex = 0;

        Integer currentWeapon = ghostWeaponIndexes.get(id);
        GameObject weapon = ghostWeapons.get(id);

        // Only recreate the weapon model if the selected weapon changed.
        if (weapon == null || currentWeapon == null || currentWeapon.intValue() != weaponIndex)
        {
            if (weapon != null)
            {
                game.getEngine().getSceneGraph().removeGameObject(weapon);
                ghostWeapons.remove(id);
            }

            ObjShape weaponShape = game.getWeaponShape(weaponIndex);
            TextureImage weaponTexture = game.getWeaponTexture(weaponIndex);

            if (weaponShape == null || weaponTexture == null)
                return;

            weapon = new GameObject(GameObject.root(), weaponShape, weaponTexture);

            weapon.setParent(ghost);
            weapon.propagateTranslation(true);
            weapon.propagateRotation(true);
            weapon.propagateScale(true);
            weapon.applyParentRotationToPosition(true);

            ghostWeapons.put(id, weapon);
            ghostWeaponIndexes.put(id, weaponIndex);
        }

        // Always reapply placement every update.
        Vector3f offset = game.getGhostWeaponOffset(weaponIndex);

        weapon.setLocalTranslation(new Matrix4f().translation(offset));
        weapon.setLocalRotation(new Matrix4f().rotationY((float)java.lang.Math.toRadians(90.0f)));
        weapon.setLocalScale(new Matrix4f().scaling(game.getGhostWeaponScale(weaponIndex)));

        weapon.getRenderStates().setModelOrientationCorrection(
            game.getGhostWeaponOrientationCorrection(weaponIndex)
        );
    }

    public void createGhost(UUID id, Vector3f pos, int avatarSelection, float yaw, int weaponIndex)
    {
        GhostAvatar existingGhost = findAvatar(id);
        if (existingGhost != null)
        {
            System.out.println("Ghost already exists: " + id);
            existingGhost.setPosition(pos);
            existingGhost.setTextureImage(game.getRobotTexture(avatarSelection));
            existingGhost.setLocalRotation(new Matrix4f().rotationY(yaw));
            updateGhostWeapon(existingGhost, id, weaponIndex);
            return;
        }

        ObjShape s = game.getGhostShape();
        TextureImage t = game.getRobotTexture(avatarSelection);

        GhostAvatar ghost = new GhostAvatar(id, s, t, pos);

        Matrix4f scale = new Matrix4f().scaling(game.getPlayerScale());
        ghost.setLocalScale(scale);

        ghost.setLocalRotation(new Matrix4f().rotationY(yaw));

        updateGhostWeapon(ghost, id, weaponIndex);

        ghostAvs.add(ghost);

        System.out.println("Ghost created: " + id + " avatar=" + avatarSelection);
    }

    public void createGhost(UUID id, Vector3f pos, int avatarSelection, float yaw)
    {
        createGhost(id, pos, avatarSelection, yaw, 0);
    }

    public void createGhost(UUID id, Vector3f pos, int avatarSelection)
    {
        createGhost(id, pos, avatarSelection, 0.0f);
    }

    public void createGhost(UUID id, Vector3f pos)
    {
        createGhost(id, pos, 0);
    }

    public void removeGhostAvatar(UUID id)
    {
        GameObject weapon = ghostWeapons.remove(id);

        if (weapon != null)
            game.getEngine().getSceneGraph().removeGameObject(weapon);

        ghostWeaponIndexes.remove(id);

        GhostAvatar g = findAvatar(id);

        if (g != null)
        {
            game.getEngine().getSceneGraph().removeGameObject(g);
            ghostAvs.remove(g);
        }
        else
        {
            System.out.println("Ghost not found");
        }
    }

    private GhostAvatar findAvatar(UUID id)
    {
        for (GhostAvatar g : ghostAvs)
        {
            if (g.getID().equals(id))
                return g;
        }
        return null;
    }

    public void updateGhostAvatar(UUID id, Vector3f pos, int avatarSelection, float yaw, int weaponIndex)
    {
        GhostAvatar g = findAvatar(id);
        if (g != null)
        {
            g.setPosition(pos);
            g.setTextureImage(game.getRobotTexture(avatarSelection));
            g.setLocalRotation(new Matrix4f().rotationY(yaw + (float)java.lang.Math.toRadians(270.0f)));
            updateGhostWeapon(g, id, weaponIndex);
        }
        else
        {
            System.out.println("Move arrived before create for ghost" + id + "; creating ghost from move packet.");
            createGhost(id, pos, avatarSelection, yaw, weaponIndex);
        }
    }

    public void updateGhostAvatar(UUID id, Vector3f pos, int avatarSelection, float yaw)
    {
        updateGhostAvatar(id, pos, avatarSelection, yaw, 0);
    }

    public void updateGhostAvatar(UUID id, Vector3f pos, int avatarSelection)
    {
        updateGhostAvatar(id, pos, avatarSelection, 0.0f);
    }

    public void updateGhostAvatar(UUID id, Vector3f pos)
    {
        updateGhostAvatar(id, pos, 0);
    }

    public void updateGhostAnimations(float dt)
    {
        for (GhostAvatar g : ghostAvs)
        {
            g.update(dt);
        }
    }
}
