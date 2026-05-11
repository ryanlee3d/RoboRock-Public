package bullet.actions;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;
import org.joml.*;
import tage.Camera;
import bullet.MyGame;

public class FwdAction extends AbstractInputAction
{
    private MyGame game;
    private Camera cam;
    private float direction, z;

    private static final float MOVE_SPEED = 8.0f;

    public FwdAction(MyGame g, float dir, Camera c)
    {
        game = g;
        direction = dir;
        cam = c;
    }

    @Override
    public void performAction(float time, Event e)
    {
        z = e.getValue();
        if (z > -0.2f && z < 0.2f)
        {
            game.stopPlayerHorizontalMotion();
            return;
        }

        Vector3f camN = cam.getN();
        Vector3f forward = new Vector3f(camN.x, 0f, camN.z);

        if (forward.lengthSquared() < 0.000001f) return;
        forward.normalize();

        // flip sign as needed for your camera convention
        forward.mul(-direction * z);

        game.movePlayerPhysics(forward, MOVE_SPEED);

        if (game.getProtocolClient() != null && game.getAvatar() != null)
            game.getProtocolClient().sendMoveMessage(game.getAvatar().getWorldLocation());
    }
}