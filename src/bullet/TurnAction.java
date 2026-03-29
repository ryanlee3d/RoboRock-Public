package bullet;

import tage.GameObject;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;
import tage.Camera;

/**
 * An input action that turns the avatar left/right
 */
public class TurnAction extends AbstractInputAction
{
    private MyGame game;
    private Camera cam;
    private GameObject av;
    private float direction, x;

    private static final float TURN_SPEED = 0.15f;

    public TurnAction(MyGame g, float dir, Camera c)
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

        float turnAmt = -TURN_SPEED * direction * x * time;

        av = game.getAvatar();
        if (av == null) return;

        av.globalYaw(turnAmt);

        if (game.getProtocolClient() != null)
            game.getProtocolClient().sendMoveMessage(av.getWorldLocation());
    }
}