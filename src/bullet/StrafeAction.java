package bullet;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;
import org.joml.*;
import tage.Camera;

public class StrafeAction extends AbstractInputAction
{
    private MyGame game;
    private Camera cam;
    private float direction, x;

    private static final float MOVE_SPEED = 8.0f;

    public StrafeAction(MyGame g, float dir, Camera c)
    {
        game = g;
        direction = dir;
        cam = c;
    }

    @Override
    public void performAction(float time, Event e)
    {
        x = e.getValue();
        if (x > -0.2f && x < 0.2f)
        {
            game.stopPlayerHorizontalMotion();
            return;
        }

        Vector3f camN = cam.getN();
        Vector3f forward = new Vector3f(camN.x, 0f, camN.z);

        if (forward.lengthSquared() < 0.000001f) return;
        forward.normalize();

        Vector3f right = new Vector3f(forward.z, 0f, -forward.x);
        right.normalize();

        right.mul(direction * x);

        game.movePlayerPhysics(right, MOVE_SPEED);

        if (game.getProtocolClient() != null && game.getAvatar() != null)
            game.getProtocolClient().sendMoveMessage(game.getAvatar().getWorldLocation());
    }
}