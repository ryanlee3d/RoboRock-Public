package bullet;

import java.io.IOException;
import java.util.*;

import org.joml.*;

import tage.*;

public class GhostManager
{
    private MyGame game;
    private Vector<GhostAvatar> ghostAvs = new Vector<>();

    public GhostManager(MyGame g)
    {
        game = g;
    }

    public void createGhost(UUID id, Vector3f pos) throws IOException
    {
        ObjShape s = game.getGhostShape();
        TextureImage t = game.getGhostTexture();

        GhostAvatar ghost = new GhostAvatar(id, s, t, pos);

        Matrix4f scale = new Matrix4f().scaling(0.25f);
        ghost.setLocalScale(scale);

        ghostAvs.add(ghost);

        System.out.println("Ghost created: " + id);
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

    public void updateGhostAvatar(UUID id, Vector3f pos)
    {
        GhostAvatar g = findAvatar(id);

        if (g != null)
        {
            g.setPosition(pos);
        }
        else
        {
            System.out.println("Ghost not found for update");
        }
    }
}