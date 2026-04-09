package bullet;

import tage.GameObject;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;
import org.joml.*;
import tage.Camera;

/** Moves the avatar left/right relative to its facing direction */
public class StrafeAction extends AbstractInputAction
{
    private MyGame game;
    private Camera cam;
    private GameObject av;
    private Vector3f oldPosition, newPosition;
    private Vector4f strafeDirection;
    private float direction, x;

    private static final float MOVE_SPEED = 0.3f;

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
        if (x > -0.2f && x < 0.2f) return;

        float moveAmt = MOVE_SPEED * direction * x * time;

        av = game.getAvatar();
        if (av == null) return;

        oldPosition = new Vector3f(av.getWorldLocation());

        // local right vector
        strafeDirection = new Vector4f(1f, 0f, 0f, 0f);
        strafeDirection.mul(av.getWorldRotation());
        strafeDirection.mul(moveAmt);

        newPosition = new Vector3f(
            oldPosition.x() + strafeDirection.x(),
            oldPosition.y() + strafeDirection.y(),
            oldPosition.z() + strafeDirection.z()
        );
        
        if(game.canMoveOnTerrain(oldPosition, newPosition)){
        av.setLocalLocation(newPosition);
        }

        if (game.getProtocolClient() != null)
            game.getProtocolClient().sendMoveMessage(av.getWorldLocation());
    }
}