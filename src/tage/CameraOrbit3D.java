package tage;

import tage.Camera;
import tage.GameObject;
import org.joml.Vector3f;
import java.lang.Math;

public class CameraOrbit3D{ 
	private Camera camera; 
	private GameObject avatar; 
	private float cameraAzimuth; 
	private float cameraElevation;
	private float cameraRadius;
	public CameraOrbit3D(Camera cam, GameObject av){ 
		camera = cam;
		avatar = av;
		cameraAzimuth = 0.0f; 
		cameraElevation = 20.0f;
		cameraRadius = 3.0f; 
		updateCameraPosition();
	}
	public void updateCameraPosition(){ 	
		Vector3f avatarRot = avatar.getWorldForwardVector();
		double avatarAngle = Math.toDegrees((double)avatarRot.angleSigned(new Vector3f(0,0,-1), new Vector3f(0,1,0)));
		float totalAz = cameraAzimuth - (float)avatarAngle;
		double theta = Math.toRadians(totalAz);
		double phi = Math.toRadians(cameraElevation);
		float x = cameraRadius * (float)(Math.cos(phi) * Math.sin(theta));
		float y = cameraRadius * (float)(Math.sin(phi));
		float z = cameraRadius * (float)(Math.cos(phi) * Math.cos(theta));
		camera.setLocation(new
		Vector3f(x,y,z).add(avatar.getWorldLocation()));
		camera.lookAt(avatar);
	}
		public void addRadius(float dr) {
		cameraRadius += dr;
		if (cameraRadius < 1.0f) cameraRadius = 1.0f;
		if (cameraRadius > 20.0f) cameraRadius = 20.0f;
		updateCameraPosition();
	}

	public void addElevation(float de) {
		cameraElevation += de;
		if (cameraElevation < 5.0f) cameraElevation = 5.0f;
		if (cameraElevation > 80.0f) cameraElevation = 80.0f;
		updateCameraPosition();
	}
	
	public void addAzimuth(float da) {
		cameraAzimuth = (cameraAzimuth + da) % 360.0f;
		if (cameraAzimuth < 0) cameraAzimuth += 360.0f;
		updateCameraPosition();
	}
}