package bullet;

import tage.GameObject;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;
import org.joml.*;
import tage.Camera;

/** Moves the avatar forward/backward */
public class FwdAction extends AbstractInputAction
{
    private MyGame game;
    private Camera cam;
    private GameObject av;
    private Vector3f oldPosition, newPosition;
    private Vector4f fwdDirection;
    private float direction, z;

    private static final float MOVE_SPEED = 0.3f;

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
        if (z > -0.2f && z < 0.2f) return;

        float moveAmt = MOVE_SPEED * direction * z * time;

        av = game.getAvatar();
        if (av == null) return;

        oldPosition = new Vector3f(av.getWorldLocation());

        fwdDirection = new Vector4f(0f, 0f, 1f, 0f);
        fwdDirection.mul(av.getWorldRotation());
        fwdDirection.mul(moveAmt);

        newPosition = new Vector3f(
            oldPosition.x() + fwdDirection.x(),
            oldPosition.y() + fwdDirection.y(),
            oldPosition.z() + fwdDirection.z()
        );

        av.setLocalLocation(newPosition);

        if (game.getProtocolClient() != null)
            game.getProtocolClient().sendMoveMessage(av.getWorldLocation());
    }
}