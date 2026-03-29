package tage.nodeControllers;

import tage.*;
import org.joml.*;

/**
 * A nodeController to set an objects visibility by
 * scaling it locally.
 *
 * <p>When visible, the controller sets the target's local scale to 1.0. When hidden,
 * it sets the target's local scale to 0.0001 so it is effectively not visible.</p>
 *
 * <p><b>Usage:</b>
 * <pre>
 * VisibilityController vc = new VisibilityController();
 * vc.addTarget(object);
 * engine.getSceneGraph().addNodeController(vc);
 *
 * vc.toggle();
 * vc.apply(object)
 * </pre>
 * </p>
 */
 
public class VisibilityController extends NodeController
{
    private boolean visible = true;
    private final Matrix4f visibleMat = new Matrix4f().scaling(1.0f);
    private final Matrix4f hiddenMat  = new Matrix4f().scaling(0.0001f);

    public void toggle() {
        visible = !visible;
    }

    public void setVisible(boolean v) {
        visible = v;
    }

    public boolean isVisible() { return visible; }

    @Override
    public void apply(GameObject go)
    {
        float s = visible ? 1.0f : 0.0001f;
        if (visible) go.setLocalScale(new Matrix4f(visibleMat));
        else         go.setLocalScale(new Matrix4f(hiddenMat));
    }
}