package tage.nodeControllers;

import tage.*;
import org.joml.*;
import java.util.IdentityHashMap;

/**
 * JumpController: makes its targets "jump" when triggered.<br>
 * Uses a sine arc<br><br>
 *
 * Usage:<br>
 *   JumpController jc = new JumpController(height, duration);<br>
 *   jc.addTarget(object);<br>
 *   engine.getSceneGraph().addNodeController(jc);<br>
 *   bind it in an input action: jc.startJump();
 */
public class JumpController extends NodeController
{
    private boolean jumping = false;
    private float jumpHeight = 2.0f;
    private float jumpDuration = 0.6f;
    private float t = 0.0f;

    private final IdentityHashMap<GameObject, Float> baseY = new IdentityHashMap<>();

    public JumpController() { super(); }

    public JumpController(float height, float duration) {
        super();
        this.jumpHeight = height;
        this.jumpDuration = duration;
    }

    public void setJumpHeight(float h) { jumpHeight = h; }
    public void setJumpDuration(float d) { jumpDuration = d; }
    public boolean isJumping() { return jumping; }

    public void startJump()
    {
        if (jumping) return;
        jumping = true;
        t = 0.0f;
        baseY.clear();
    }

	@Override
	public void apply(GameObject go)
	{
		if (!jumping) return;
		float dtSec = super.getElapsedTime() / 1000.0f;
		
		if (dtSec <= 0.0f) return;
		t += dtSec;
		Vector3f loc = go.getLocalLocation();
		Float by = baseY.get(go);
		
		if (by == null) {
			by = loc.y;
			baseY.put(go, by);
		}
		float phase = t / jumpDuration;
		
		if (phase > 1.0f) phase = 1.0f;
		float yOffset = (float) java.lang.Math.sin(java.lang.Math.PI * phase) * jumpHeight;
		go.setLocalLocation(new Vector3f(loc.x, by + yOffset, loc.z));

		if (t >= jumpDuration) {
			Vector3f loc2 = go.getLocalLocation();
			go.setLocalLocation(new Vector3f(loc2.x, by, loc2.z));
			jumping = false;
			t = 0.0f;
			baseY.clear();
		}
	}
}