package bullet;

import java.util.*;

import org.joml.*;

import tage.*;

import tage.shapes.*;

public class GhostManager
{
    private MyGame game;
    private Vector<GhostAvatar> ghostAvs = new Vector<>();

    public GhostManager(MyGame g)
    {
        game = g;
    }

    public void createGhost(UUID id, Vector3f pos, int avatarSelection, float yaw)
    {
        GhostAvatar existingGhost = findAvatar(id);
        if (existingGhost != null)
        {
            System.out.println("Ghost already exists: " + id);
            existingGhost.setPosition(pos);
            existingGhost.setTextureImage(game.getRobotTexture(avatarSelection));
            existingGhost.setLocalRotation(new Matrix4f().rotationY(yaw));
            return;
        }

        ObjShape s = game.getGhostShape();
        TextureImage t = game.getRobotTexture(avatarSelection);

        GhostAvatar ghost = new GhostAvatar(id, s, t, pos);

        Matrix4f scale = new Matrix4f().scaling(game.getPlayerScale());
        ghost.setLocalScale(scale);

        ghost.setLocalRotation(new Matrix4f().rotationY(yaw));

        ghostAvs.add(ghost);

        System.out.println("Ghost created: " + id + " avatar=" + avatarSelection);
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

    public void updateGhostAvatar(UUID id, Vector3f pos, int avatarSelection, float yaw)
    {
        GhostAvatar g = findAvatar(id);
        if (g != null)
        {
            g.setPosition(pos);
            g.setTextureImage(game.getRobotTexture(avatarSelection));
            g.setLocalRotation(new Matrix4f().rotationY(yaw + (float)java.lang.Math.toRadians(270.0f)));
        }
        else
        {
            System.out.println("Move arrived before create for ghost" + id + "; creating ghost from move packet.");
            createGhost(id, pos, avatarSelection, yaw);
        }
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
