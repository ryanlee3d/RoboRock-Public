package bullet;

import net.java.games.input.Event;
import org.joml.Vector3f;
import tage.Camera;
import tage.CameraOrbit3D;
import tage.input.action.AbstractInputAction;

public class OverheadCameraController
{
    private static final float MIN_HEIGHT = 6.0f;
    private static final float MAX_HEIGHT = 60.0f;
    private static final float PAN_STEP = 1.0f;

    private float height = 32.0f;
    private float panX = 0.0f;
    private float panZ = 0.0f;
    private CameraOrbit3D orbitCam;

    public void setOrbitCam(CameraOrbit3D orbitCam)
    {
        this.orbitCam = orbitCam;
    }

    public float getHeight()
    {
        return height;
    }

    public void applyTo(Camera camera, Vector3f playerPosition)
    {
        if (camera == null || playerPosition == null) return;

        camera.setLocation(new Vector3f(playerPosition.x + panX, height, playerPosition.z + panZ));
        camera.setU(new Vector3f(1, 0, 0));
        camera.setV(new Vector3f(0, 0, -1));
        camera.setN(new Vector3f(0, -1, 0));
    }

    public AbstractInputAction createZoomInAction()
    {
        return new AbstractInputAction()
        {
            @Override
            public void performAction(float time, Event e)
            {
                height -= 1.0f;
                if (height < MIN_HEIGHT) height = MIN_HEIGHT;
            }
        };
    }

    public AbstractInputAction createZoomOutAction()
    {
        return new AbstractInputAction()
        {
            @Override
            public void performAction(float time, Event e)
            {
                height += 1.0f;
                if (height > MAX_HEIGHT) height = MAX_HEIGHT;
            }
        };
    }

    public AbstractInputAction createElevationUpAction()
    {
        return new AbstractInputAction()
        {
            @Override
            public void performAction(float time, Event e)
            {
                if (orbitCam != null)
                    orbitCam.addElevation(2.0f);
            }
        };
    }

    public AbstractInputAction createElevationDownAction()
    {
        return new AbstractInputAction()
        {
            @Override
            public void performAction(float time, Event e)
            {
                if (orbitCam != null)
                    orbitCam.addElevation(-2.0f);
            }
        };
    }

    public AbstractInputAction createPanUpAction()
    {
        return new AbstractInputAction()
        {
            @Override
            public void performAction(float time, Event e)
            {
                panZ += PAN_STEP;
            }
        };
    }

    public AbstractInputAction createPanDownAction()
    {
        return new AbstractInputAction()
        {
            @Override
            public void performAction(float time, Event e)
            {
                panZ -= PAN_STEP;
            }
        };
    }

    public AbstractInputAction createPanLeftAction()
    {
        return new AbstractInputAction()
        {
            @Override
            public void performAction(float time, Event e)
            {
                panX -= PAN_STEP;
            }
        };
    }

    public AbstractInputAction createPanRightAction()
    {
        return new AbstractInputAction()
        {
            @Override
            public void performAction(float time, Event e)
            {
                panX += PAN_STEP;
            }
        };
    }

    public AbstractInputAction createRecenterAction()
    {
        return new AbstractInputAction()
        {
            @Override
            public void performAction(float time, Event e)
            {
                panX = 0.0f;
                panZ = 0.0f;
            }
        };
    }
}
