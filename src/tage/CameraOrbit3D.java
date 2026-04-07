package tage;

import tage.Camera;
import tage.GameObject;
import org.joml.Vector3f;
import java.lang.Math;

public class CameraOrbit3D
{
	private Camera camera;
	private GameObject avatar;
	private float cameraAzimuth;
	private float cameraElevation;
	private float cameraRadius;

	// offsets from avatar origin to where the weapon/gun will be held
	private float weaponAnchorHeight;
	private float weaponAnchorForward;

	public CameraOrbit3D(Camera cam, GameObject av, float avatarScale)
	{
		camera = cam;
		avatar = av;

		cameraAzimuth = 0.0f;
		cameraElevation = 20.0f;
		cameraRadius = 3.0f;

		// tune these if needed
		weaponAnchorHeight = avatarScale * 120.0f;
		weaponAnchorForward = avatarScale * 12.0f;

		updateCameraPosition();
	}

	public void updateCameraPosition()
	{
		Vector3f avatarLoc = avatar.getWorldLocation();

		// target point near neck / gun holding position
		Vector3f targetLoc = new Vector3f(
			avatarLoc.x(),
			avatarLoc.y() + weaponAnchorHeight,
			avatarLoc.z() + weaponAnchorForward
		);

		double theta = Math.toRadians(cameraAzimuth);
		double phi = Math.toRadians(cameraElevation);

		float x = cameraRadius * (float)(Math.cos(phi) * Math.sin(theta));
		float y = cameraRadius * (float)(Math.sin(phi));
		float z = cameraRadius * (float)(Math.cos(phi) * Math.cos(theta));

		camera.setLocation(new Vector3f(
			targetLoc.x() + x,
			targetLoc.y() + y,
			targetLoc.z() + z
		));

		camera.lookAt(targetLoc);
	}

	public void addRadius(float dr)
	{
		cameraRadius += dr;
		if (cameraRadius < 1.0f) cameraRadius = 1.0f;
		if (cameraRadius > 20.0f) cameraRadius = 20.0f;
		updateCameraPosition();
	}

	public void addElevation(float de)
	{
		cameraElevation += de;
		if (cameraElevation < -80.0f) cameraElevation = -80.0f;
		if (cameraElevation > 80.0f) cameraElevation = 80.0f;
		updateCameraPosition();
	}

	public void addAzimuth(float da)
	{
		cameraAzimuth = (cameraAzimuth + da) % 360.0f;
		if (cameraAzimuth < 0) cameraAzimuth += 360.0f;
		updateCameraPosition();
	}
}