package bullet;

import tage.*;
import tage.shapes.*;
import tage.input.*;
import tage.input.action.*;

import java.awt.*;
import java.awt.event.*;
import org.joml.*;

//networking imports
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import tage.networking.IGameConnection.ProtocolType;

public class MyGame extends VariableFrameRateGame implements MouseMotionListener
{
	private static Engine engine;
	
	private enum GameState
    {
        PLAYING,
        PAUSED,
        GAME_OVER
    }

    private GameState gameState = GameState.PLAYING;

	private InputManager im;
	private CameraOrbit3D orbitCam;
	private Camera cam;
	private Camera camOver;

	private double lastFrameTime, currFrameTime, elapsTime;

	private GameObject dol;
	private GameObject cube;

	private ObjShape dolS;
	private ObjShape cubeS;

	private TextureImage doltx;
	private TextureImage ancient;

	private Light mainLight;

	//mouselook
	private Robot robot;
	private boolean mouseModeInitiated = false;
	private boolean isRecentering = false;
	private float centerX, centerY;
	private float prevMouseX, prevMouseY;
	private float curMouseX, curMouseY;
	
	//controlling the hud cam
	private float ohHeight = 32.0f;
	private float ohMinH = 6.0f;
	private float ohMaxH = 60.0f;
	private float ohPanX = 0.0f;
	private float ohPanZ = 0.0f;
	private float ohPanStep = 1.0f;
	
	//networking fields
	private GhostManager gm;
	private String serverAddress;
	private int serverPort;
	private ProtocolType serverProtocol;
	private ProtocolClient protClient;
	private boolean isClientConnected = false;
	
	//skyboxes
	private int cloudTest;
	
	//getter functions
	public GameObject getAvatar() { return dol; }
	public Camera getCamera() { return cam; }
	public ObjShape getGhostShape() { return ghostS; }
	public TextureImage getGhostTexture() { return ghostT; }
	public GhostManager getGhostManager() { return gm; }
	public Engine getEngine() { return engine; }
	
	public Vector3f getPlayerPosition()
	{
		if (dol == null) return new Vector3f(0,0,0);
		return dol.getWorldLocation();
	}
	
	public ProtocolClient getProtocolClient(){return protClient; }
	
	public void setIsConnected(boolean value){isClientConnected = value; }

	// ghost rendering
	private ObjShape ghostS;
	private TextureImage ghostT;
	
	//networking logic
	private void setupNetworking()
	{
		isClientConnected = false;

		try
		{
			protClient = new ProtocolClient(
				InetAddress.getByName(serverAddress),
				serverPort,
				serverProtocol,
				this
			);
		}
		catch (UnknownHostException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		if (protClient == null)
		{
			System.out.println("missing protocol host");
		}
		else
		{
			protClient.sendJoinMessage();
		}
	}

	protected void processNetworking(float elapsTime)
	{
		if (protClient != null)
			protClient.processPackets();
	}
	
	//intializes mouselook
	private void initMouseMode()
	{
		mouseModeInitiated = true;

		RenderSystem rs = engine.getRenderSystem();
		Viewport vw = rs.getViewport("MAIN");
		float left = vw.getActualLeft();
		float bottom = vw.getActualBottom();
		float width = vw.getActualWidth();
		float height = vw.getActualHeight();

		centerX = (int)(left + width / 2.0f);
		centerY = (int)(bottom - height / 2.0f);

		isRecentering = false;

		try { robot = new Robot(); }
		catch (AWTException ex) { throw new RuntimeException("Couldn't create Robot!"); }

		recenterMouse();
		prevMouseX = centerX;
		prevMouseY = centerY;
	}

	public MyGame(String serverAddress, int serverPort, String protocol)
	{
		super();
		gm = new GhostManager(this);
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;

		if (protocol.toUpperCase().compareTo("TCP") == 0)
			serverProtocol = ProtocolType.TCP;
		else
			serverProtocol = ProtocolType.UDP;
	}
	
	@Override
	public void loadSkyBoxes()
	{
		cloudTest = (engine.getSceneGraph()).loadCubeMap("fluffyClouds"); //make sure the images are .jpg
		//add same as above here save xp xn yp yn zp zn in assets/skyboxes/"   "
		
		(engine.getSceneGraph()).setActiveSkyBoxTexture(cloudTest); //sets the scene to this skybox
		(engine.getSceneGraph()).setSkyBoxEnabled(true);
	}

	public static void main(String[] args)
	{
		if (args.length < 3)
		{
			System.out.println("Usage: java bullet.MyGame <serverIP> <port> <TCP|UDP>");
			System.exit(0);
		}

		MyGame game = new MyGame(args[0], Integer.parseInt(args[1]), args[2]);
		engine = new Engine(game);
		engine.initializeSystem();
		game.buildGame();
		game.startGame();
	}

	@Override
	public void loadShapes()
	{
		dolS = new ImportedModel("dolphinHighPoly.obj");
		ghostS = dolS;
	}

	@Override
	public void loadTextures()
	{
		doltx = new TextureImage("Dolphin_HighPolyUV.jpg");
		ghostT = doltx;
	}

	@Override
	public void buildObjects()
	{
		Matrix4f initialTranslation, initialScale;

		dol = new GameObject(GameObject.root(), dolS, doltx);
		initialTranslation = new Matrix4f().translation(0.0f, 0.75f, 0.0f);
		initialScale = new Matrix4f().scaling(3.0f);
		dol.setLocalTranslation(initialTranslation);
		dol.setLocalScale(initialScale);
	}

	@Override
	public void initializeLights()
	{
		Light.setGlobalAmbient(0.5f, 0.5f, 0.5f);

		mainLight = new Light();
		mainLight.setLocation(new Vector3f(0.0f, 0.0f, 0.0f));
		engine.getSceneGraph().addLight(mainLight);
	}

	@Override
	public void createViewports()
	{
		engine.getRenderSystem().addViewport("MAIN", 0, 0, 1f, 1f);
		engine.getRenderSystem().addViewport("OVERHEAD", .75f, 0, .25f, .25f);

		Viewport mainVp = engine.getRenderSystem().getViewport("MAIN");
		Viewport overVp = engine.getRenderSystem().getViewport("OVERHEAD");

		Camera mainCamera = mainVp.getCamera();
		Camera overCamera = overVp.getCamera();

		overVp.setHasBorder(true);
		overVp.setBorderWidth(4);
		overVp.setBorderColor(0.0f, 1.0f, 0.0f);

		// Main camera
		mainCamera.setLocation(new Vector3f(0.0f, 8.0f, 12.0f));
		mainCamera.setU(new Vector3f(1, 0, 0));
		mainCamera.setV(new Vector3f(0, 1, 0));
		mainCamera.setN(new Vector3f(0, 0, -1));

		// Overhead camera
		overCamera.setLocation(new Vector3f(0, ohHeight, 0));
		overCamera.setU(new Vector3f(1, 0, 0));
		overCamera.setV(new Vector3f(0, 0, -1));
		overCamera.setN(new Vector3f(0, -1, 0));
	}

	@Override
	public void initializeGame()
	{
		System.out.println("=== initializeGame() reached ===");

		createViewports();

		cam = engine.getRenderSystem().getViewport("MAIN").getCamera();
		camOver = engine.getRenderSystem().getViewport("OVERHEAD").getCamera();

		orbitCam = new CameraOrbit3D(cam, dol);

		lastFrameTime = System.currentTimeMillis();
		currFrameTime = System.currentTimeMillis();
		elapsTime = 0.0;

		engine.getRenderSystem().setWindowDimensions(1280, 720);

		// CHANGE FOR FPS VIEW
		cam.setLocation(new Vector3f(0.0f, 8.0f, 12.0f));

		im = engine.getInputManager();

		FwdAction fwdA = new FwdAction(this, -25.0f, cam);
		FwdAction backA = new FwdAction(this, 25.0f, cam);
		TurnAction leftT = new TurnAction(this, -25.0f, cam);
		TurnAction rightT = new TurnAction(this, 25.0f, cam);

		AbstractInputAction orbitAPad = new OrbitAzimuthAction(-1.0f);
		AbstractInputAction zoomIn = new OverheadZoomInAction();
		AbstractInputAction zoomOut = new OverheadZoomOutAction();
		AbstractInputAction elevUp = new ElevationUp();
		AbstractInputAction elevDown = new ElevationDown();
		AbstractInputAction panUp = new OhPUA();
		AbstractInputAction panDown = new OhPDA();
		AbstractInputAction panLeft = new OhPLA();
		AbstractInputAction panRight = new OhPRA();
		AbstractInputAction recenter = new OhRecenter();

		//gamepad bindings
		im.associateActionWithAllGamepads(
			net.java.games.input.Component.Identifier.Axis.Y, fwdA,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllGamepads(
			net.java.games.input.Component.Identifier.Axis.X, rightT,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllGamepads(
			net.java.games.input.Component.Identifier.Axis.RX, orbitAPad,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllGamepads(
			net.java.games.input.Component.Identifier.Button._4, zoomOut,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllGamepads(
			net.java.games.input.Component.Identifier.Button._5, zoomIn,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllGamepads(
			net.java.games.input.Component.Identifier.Button._8, elevUp,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllGamepads(
			net.java.games.input.Component.Identifier.Button._9, elevDown,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		//keyboard bindings
		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.W, backA,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.S, fwdA,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.A, leftT,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.D, rightT,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.ADD, zoomIn,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.SUBTRACT, zoomOut,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key._1, elevUp,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key._2, elevDown,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.DOWN, panUp,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.UP, panDown,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.LEFT, panLeft,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.RIGHT, panRight,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.R, recenter,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
			
		//setup networking
		setupNetworking();
	}

	@Override
	public void update()
	{
		lastFrameTime = currFrameTime;
		currFrameTime = System.currentTimeMillis();
		float dt = (float)((currFrameTime - lastFrameTime) / 1000.0);
		elapsTime += dt;

		cam = engine.getRenderSystem().getViewport("MAIN").getCamera();
		im.update(dt);
		processNetworking(dt); // updates the network connection

		if (!mouseModeInitiated) initMouseMode();
		orbitCam.updateCameraPosition();

		Vector3f dpos = dol.getWorldLocation();

		// Overhead camera follows dolphin from previous assignment and needs to be changed - was going to work on setting up the multiplayer network first
		camOver.setLocation(new Vector3f(dpos.x + ohPanX, ohHeight, dpos.z + ohPanZ));
		camOver.setU(new Vector3f(1, 0, 0));
		camOver.setV(new Vector3f(0, 0, -1));
		camOver.setN(new Vector3f(0, -1, 0));

		// HUD that was brought over from A2
		String posStr = String.format("Dolphin Pos: X(%.2f) Y(%.2f) Z(%.2f)", dpos.x, dpos.y, dpos.z);
		Vector3f hudColor = new Vector3f(1, 1, 1);
		engine.getHUDmanager().setHUD1(posStr, hudColor, 15, 15);
	}

	@Override
	public void mouseMoved(MouseEvent e)
	{
		if (!mouseModeInitiated) return;

		if (isRecentering &&
			e.getXOnScreen() == (int)centerX &&
			e.getYOnScreen() == (int)centerY)
		{
			isRecentering = false;
			return;
		}

		curMouseX = e.getXOnScreen();
		curMouseY = e.getYOnScreen();

		float mouseDeltaX = prevMouseX - curMouseX;
		orbitCam.addAzimuth(mouseDeltaX * 0.25f);

		prevMouseX = curMouseX;
		prevMouseY = curMouseY;

		recenterMouse();
		prevMouseX = centerX;
		prevMouseY = centerY;
	}

	private void recenterMouse()
	{
		RenderSystem rs = engine.getRenderSystem();
		Viewport vw = rs.getViewport("MAIN");
		float left = vw.getActualLeft();
		float bottom = vw.getActualBottom();
		float width = vw.getActualWidth();
		float height = vw.getActualHeight();

		centerX = (int)(left + width / 2.0f);
		centerY = (int)(bottom - height / 2.0f);

		isRecentering = true;
		robot.mouseMove((int)centerX, (int)centerY);
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e)
	{
		int clicks = e.getWheelRotation();
		orbitCam.addRadius(clicks * 0.25f);
	}

	private class OrbitAzimuthAction extends AbstractInputAction
	{
		private final float dir;

		private OrbitAzimuthAction(float dir)
		{
			this.dir = dir;
		}

		@Override
		public void performAction(float time, net.java.games.input.Event e)
		{
			float v = e.getValue() * dir;

			float rotAmount;
			if (v < -0.2f) rotAmount = -0.8f;
			else if (v > 0.2f) rotAmount = 0.8f;
			else rotAmount = 0.0f;

			orbitCam.addAzimuth(rotAmount);
		}
	}

	private class OverheadZoomInAction extends AbstractInputAction
	{
		@Override
		public void performAction(float time, net.java.games.input.Event e)
		{
			ohHeight -= 1.0f;
			if (ohHeight < ohMinH) ohHeight = ohMinH;
		}
	}

	private class OverheadZoomOutAction extends AbstractInputAction
	{
		@Override
		public void performAction(float time, net.java.games.input.Event e)
		{
			ohHeight += 1.0f;
			if (ohHeight > ohMaxH) ohHeight = ohMaxH;
		}
	}

	private class ElevationUp extends AbstractInputAction
	{
		@Override
		public void performAction(float time, net.java.games.input.Event e)
		{
			orbitCam.addElevation(2.0f);
		}
	}

	private class ElevationDown extends AbstractInputAction
	{
		@Override
		public void performAction(float time, net.java.games.input.Event e)
		{
			orbitCam.addElevation(-2.0f);
		}
	}

	private class OhPUA extends AbstractInputAction
	{
		@Override
		public void performAction(float time, net.java.games.input.Event e)
		{
			ohPanZ += ohPanStep;
		}
	}

	private class OhPDA extends AbstractInputAction
	{
		@Override
		public void performAction(float time, net.java.games.input.Event e)
		{
			ohPanZ -= ohPanStep;
		}
	}

	private class OhPLA extends AbstractInputAction
	{
		@Override
		public void performAction(float time, net.java.games.input.Event e)
		{
			ohPanX -= ohPanStep;
		}
	}

	private class OhPRA extends AbstractInputAction
	{
		@Override
		public void performAction(float time, net.java.games.input.Event e)
		{
			ohPanX += ohPanStep;
		}
	}

	private class OhRecenter extends AbstractInputAction
	{
		@Override
		public void performAction(float time, net.java.games.input.Event e)
		{
			ohPanX = 0.0f;
			ohPanZ = 0.0f;
		}
	}
}