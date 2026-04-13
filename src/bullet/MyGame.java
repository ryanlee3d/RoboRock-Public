package bullet;

import tage.*;
import tage.shapes.*;
import tage.input.*;
import tage.input.action.*;

import java.awt.*;
import java.awt.event.*;
import org.joml.*;
import org.joml.Math;

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
		MENU,
        PLAYING,
        PAUSED,
        GAME_OVER
    }
	private boolean isShuttingDown = false;
    private GameState gameState = GameState.MENU;
	private int menuSelection = 0;
	private final MainMenu menu = new MainMenu();

	private InputManager im;
	private CameraOrbit3D orbitCam;
	private Camera cam;
	private Camera camOver;

	private float sensitvity = 0.25f;

	private double lastFrameTime, currFrameTime, elapsTime;
	private IAction restartGame;

	//game objects
	private GameObject player, skinny, ape, ammoPickup, healthPickup, plasmaRifle, apePlasmaRifle, terr;

	// shapes for animated objects
	private AnimatedShape playerS, skinnyS, apeS;

	// player animation values
	private boolean isMoving = false;
	private boolean wasMoving = false;
	private boolean isSwapping = false;
	private Vector3f prevPlayerPos = new Vector3f(0,0,0);

	// adjust this if the swap animation is longer/shorter
	private float swapTimer = 0.0f;
	private final float swapDuration = 0.8f;

	// shapes and textures for game objects
	private ObjShape ammoS, terrS, healthS, plasmaRifleS;

	private TextureImage playerTx, terrTxMap0, terrTxMap1, ammoTx, healthTx, plasmaRifleTx, heightMap0, heightMap1, skinnyTx, apeTx;

	//pickup object animation values
	private float ammoBobTime = 0.0f;
	private float healthSpin = 0.0f;

	//object init locations and scale
	private Vector3f playerStartPos = new Vector3f(0.0f, 0.75f, 0.0f);
	private float playerScale = 0.01f;
	private Vector3f ammoBasePos = new Vector3f(3.0f, 1.0f, 0.0f);
	private float ammoScale = 0.3f;
	private Vector3f healthBasePos = new Vector3f(-3.0f, 1.0f, 0.0f);
	private float healthScale = 0.3f;

	//Movement Variables
	private static final float maxClimbSlope = 1.2f;
	private static final float maxStepHeight = 0.5f;

	// plasma rifle transform
	private Vector3f plasmaRiflePos = new Vector3f (-0.2f,  1.4f, 0.65f);// old hip vector: (0.18f, 1.10f, 0.28f);
	private float plasmaRifleScale = 0.5f;
	private float plasmaRifleRotY = 0.0f; // degrees

	//lighting
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
	private int spaceSkyBox, fluffySkyBox;

	//lights
	private Light ammoLight;
	private Light healthLight;
	
	//map selection
	private int mapSelection = 0;
	
	//getter functions
	public GameObject getAvatar() { return player; }
	public Camera getCamera() { return cam; }
	public ObjShape getGhostShape() { return ghostS; }
	public TextureImage getGhostTexture() { return ghostT; }
	public GhostManager getGhostManager() { return gm; }
	public Engine getEngine() { return engine; }
	
	//setter functions
	public void setMapSelection(int selection){mapSelection = selection;}
	public void setGameState(String state){gameState = GameState.valueOf(state);}
	
	public Vector3f getPlayerPosition()
	{
		if (player == null) return new Vector3f(0,0,0);
		return player.getWorldLocation();
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

	private void playSwapAnimation()
	{
		if (isShuttingDown) return;
		if (playerS == null) return;

		isSwapping = true;
		swapTimer = swapDuration;

		playerS.stopAnimation();

		if (isMoving)
			playerS.playAnimation("SWAPRUN", 1.0f, AnimatedShape.EndType.NONE, 1);
		else
			playerS.playAnimation("SWAP", 1.0f, AnimatedShape.EndType.NONE, 1);
	}

	private void snapToTerrain(GameObject obj)
	{
		if (obj == null || terr == null) return;

		Vector3f pos = obj.getWorldLocation();
		float height = terr.getHeight(pos.x, pos.z);

		obj.setLocalLocation(new Vector3f(pos.x, height, pos.z));
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
			spaceSkyBox = (engine.getSceneGraph()).loadCubeMap("blueSpace"); //make sure the images are .jpg
			//add same as above here save xp xn yp yn zp zn in assets/skyboxes/"   "
			fluffySkyBox = (engine.getSceneGraph()).loadCubeMap("fluffyClouds"); //make sure the images are .jpg
			//add same as above here save xp xn yp yn zp zn in assets/skyboxes/"   "
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
		terrS = new TerrainPlane(1000);
		switch (mapSelection) {
			case 0:
				playerS = new AnimatedShape("Robot.rkm", "Robot.rks");
				playerS.loadAnimation("RUN", "RobotRun.rka");
				playerS.loadAnimation("STAND", "RobotStanding.rka");
				playerS.loadAnimation("SWAP", "RobotSwapGun.rka");
				playerS.loadAnimation("SWAPRUN", "RobotSwapGunRun.rka");
				ghostS = playerS;

				skinnyS = new AnimatedShape("skinny.rkm", "skinny.rks");
				skinnyS.loadAnimation("WAVE", "wave.rka");

				apeS = new AnimatedShape("ape.rkm", "ape.rks");
				apeS.loadAnimation("RUN", "apeRun.rka");

				ammoS = new ImportedModel("ammo.obj");
				healthS = new ImportedModel("health.obj");
				plasmaRifleS = new ImportedModel("plasmaRifle.obj");
				break;
			case 1:
				playerS = new AnimatedShape("Robot.rkm", "Robot.rks");
				playerS.loadAnimation("RUN", "RobotRun.rka");
				playerS.loadAnimation("SWAP", "RobotSwapGun.rka");
				playerS.loadAnimation("SWAPRUN", "RobotSwapGunRun.rka");
				ghostS = playerS;

				ammoS = new ImportedModel("ammo.obj");
				healthS = new ImportedModel("health.obj");
				plasmaRifleS = new ImportedModel("plasmaRifle.obj");
				break;
		}
	}

	@Override
	public void loadTextures()
	{
  		playerTx = new TextureImage("robot.jpg");
    	ghostT = playerTx;

		skinnyTx = new TextureImage("skinny.jpg");

		apeTx = new TextureImage("ape.jpg"); // or whatever texture name

    	ammoTx = new TextureImage("ammo.jpg");
    	healthTx = new TextureImage("health.jpg");
    	plasmaRifleTx = new TextureImage("plasmaRifle.jpg");

    	terrTxMap0 = new TextureImage("coast_sand_rocks_02_diff_1k.jpg");
    	terrTxMap1 = new TextureImage("airbase_radar_panels.jpg");

   		heightMap0 = new TextureImage("map0hm.png");
    	heightMap1 = new TextureImage("map1hm.png");
}


@Override
public void buildObjects()
{
	player = new GameObject(GameObject.root(), playerS, playerTx);
	player.setLocalTranslation(new Matrix4f().translation(playerStartPos.x, playerStartPos.y, playerStartPos.z));
	player.setLocalScale(new Matrix4f().scaling(playerScale));
	prevPlayerPos.set(player.getWorldLocation());
	player.getRenderStates().setModelOrientationCorrection((new Matrix4f())
    .rotationY((float)java.lang.Math.toRadians(270.0f)));
	playerS.playAnimation("STAND", 0.5f, AnimatedShape.EndType.LOOP, 0);

	skinny = new GameObject(GameObject.root(), skinnyS, skinnyTx);
	skinny.setLocalTranslation(new Matrix4f().translation(0.0f, 1.0f, -5.0f));
	skinny.setLocalScale(new Matrix4f().scaling(1.0f));
	skinnyS.playAnimation("WAVE", 0.3f, AnimatedShape.EndType.LOOP, 0);

	ape = new GameObject(GameObject.root(), apeS, apeTx);
	ape.setLocalTranslation(new Matrix4f().translation(5.0f, 1.0f, -5.0f));
	ape.setLocalScale(new Matrix4f().scaling(0.01f));
	ape.getRenderStates().setModelOrientationCorrection((new Matrix4f())
    .rotationX((float)java.lang.Math.toRadians(90.0f))
    .rotateZ((float)java.lang.Math.toRadians(180.0f)));
	apeS.playAnimation("RUN", 0.25f, AnimatedShape.EndType.LOOP, 0);

	apePlasmaRifle = new GameObject(GameObject.root(), plasmaRifleS, plasmaRifleTx);
	apePlasmaRifle.setLocalTranslation(new Matrix4f().translation(-0.05f,  1.5f, 0.7f));
	apePlasmaRifle.setLocalRotation(new Matrix4f().rotationY((float)java.lang.Math.toRadians(0.0f)));
	apePlasmaRifle.setLocalScale(new Matrix4f().scaling(0.5f));

	apePlasmaRifle.setParent(ape);
	apePlasmaRifle.propagateTranslation(true);
	apePlasmaRifle.propagateRotation(true);
	apePlasmaRifle.propagateScale(true);
	apePlasmaRifle.applyParentRotationToPosition(true);

	ammoPickup = new GameObject(GameObject.root(), ammoS, ammoTx);
	ammoPickup.setLocalTranslation( new Matrix4f().translation(ammoBasePos.x, ammoBasePos.y, ammoBasePos.z));
	ammoPickup.setLocalScale(new Matrix4f().scaling(ammoScale));

	healthPickup = new GameObject(GameObject.root(), healthS, healthTx);
	healthPickup.setLocalTranslation( new Matrix4f().translation(healthBasePos.x, healthBasePos.y, healthBasePos.z));
	healthPickup.setLocalScale(new Matrix4f().scaling(healthScale));
	healthPickup.getRenderStates().setModelOrientationCorrection((new Matrix4f())
    .rotationX((float)java.lang.Math.toRadians(90.0f)));

	// ---------- plasma rifle attached to player ----------
	plasmaRifle = new GameObject(GameObject.root(), plasmaRifleS, plasmaRifleTx);
	plasmaRifle.setLocalTranslation(new Matrix4f().translation(plasmaRiflePos.x, plasmaRiflePos.y, plasmaRiflePos.z));
	plasmaRifle.setLocalRotation(new Matrix4f().rotationY((float)java.lang.Math.toRadians(plasmaRifleRotY)));
	plasmaRifle.setLocalScale(new Matrix4f().scaling(plasmaRifleScale));
	plasmaRifle.setParent(player);
	plasmaRifle.propagateTranslation(true);
	plasmaRifle.propagateRotation(true);
	plasmaRifle.propagateScale(true);
	plasmaRifle.applyParentRotationToPosition(true);

	// ---------- terrain ----------
	terr = new GameObject(GameObject.root(), terrS, terrTxMap0);
	Matrix4f initialTranslation = (new Matrix4f()).translation(0f, 0f, 0f);
	terr.setLocalTranslation(initialTranslation);
	Matrix4f initialScale = (new Matrix4f()).scaling(100.0f, 50.0f, 100.0f);
	terr.setLocalScale(initialScale);
	terr.getRenderStates().setTiling(1);

	applyMapSelection();
}

	@Override
	public void initializeLights()
	{
		switch(mapSelection){
			case 0:
				Light.setGlobalAmbient(0.5f, 0.5f, 0.5f);

				mainLight = new Light();
				mainLight.setLocation(new Vector3f(0.0f, 0.0f, 0.0f));

				engine.getSceneGraph().addLight(mainLight);

				ammoLight = new Light();
				ammoLight.setDiffuse(0.2f, 1.0f, 0.2f);
				ammoLight.setSpecular(0.2f, 0.6f, 1.0f);
				ammoLight.setAmbient(0.05f, 0.1f, 0.2f);

				ammoLight.setType(Light.LightType.SPOTLIGHT);
				ammoLight.setDirection(new Vector3f(0.0f, -1.0f, 0.0f));
				ammoLight.setCutoffAngle(20.0f);
				ammoLight.setOffAxisExponent(10.0f);

				engine.getSceneGraph().addLight(ammoLight);

				healthLight = new Light();
				healthLight.setDiffuse (0.2f, 0.6f, 1.0f);
				healthLight.setSpecular(0.2f, 1.0f, 0.2f);
				healthLight.setAmbient(0.05f, 0.2f, 0.05f);

				healthLight.setType(Light.LightType.SPOTLIGHT);
				healthLight.setDirection(new Vector3f(0.0f, -1.0f, 0.0f));
				healthLight.setCutoffAngle(20.0f);
				healthLight.setOffAxisExponent(10.0f);

				engine.getSceneGraph().addLight(healthLight);

				break;
			case 1:
				Light.setGlobalAmbient(0.5f, 0.5f, 0.5f);

				mainLight = new Light();
				mainLight.setLocation(new Vector3f(0.0f, 0.0f, 0.0f));

				engine.getSceneGraph().addLight(mainLight);

				ammoLight = new Light();
				ammoLight.setDiffuse(0.2f, 1.0f, 0.2f);
				ammoLight.setSpecular(0.2f, 0.6f, 1.0f);
				ammoLight.setAmbient(0.05f, 0.1f, 0.2f);

				ammoLight.setType(Light.LightType.SPOTLIGHT);
				ammoLight.setDirection(new Vector3f(0.0f, -1.0f, 0.0f));
				ammoLight.setCutoffAngle(20.0f);
				ammoLight.setOffAxisExponent(10.0f);

				engine.getSceneGraph().addLight(ammoLight);

				healthLight = new Light();
				healthLight.setDiffuse (0.2f, 0.6f, 1.0f);
				healthLight.setSpecular(0.2f, 1.0f, 0.2f);
				healthLight.setAmbient(0.05f, 0.2f, 0.05f);

				healthLight.setType(Light.LightType.SPOTLIGHT);
				healthLight.setDirection(new Vector3f(0.0f, -1.0f, 0.0f));
				healthLight.setCutoffAngle(20.0f);
				healthLight.setOffAxisExponent(10.0f);

				engine.getSceneGraph().addLight(healthLight);

				break;
		}
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

		orbitCam = new CameraOrbit3D(cam, player, playerScale);

		lastFrameTime = System.currentTimeMillis();
		currFrameTime = System.currentTimeMillis();
		elapsTime = 0.0;

		engine.getRenderSystem().setWindowDimensions(1280, 720);

		// CHANGE FOR FPS VIEW
		cam.setLocation(new Vector3f(0.0f, 8.0f, 12.0f));

		im = engine.getInputManager();

		FwdAction fwdA = new FwdAction(this, -25.0f, cam);
		FwdAction backA = new FwdAction(this, 25.0f, cam);
		StrafeAction leftS = new StrafeAction(this, 25.0f, cam);
		StrafeAction rightS = new StrafeAction(this, -25.0f, cam);

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
		/*im.associateActionWithAllGamepads(
			net.java.games.input.Component.Identifier.Axis.Y, fwdA,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllGamepads(
			net.java.games.input.Component.Identifier.Axis.X, rightS,
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
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);*/

		//keyboard bindings
		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.W, backA,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.S, fwdA,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.A, leftS,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.D, rightS,
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
		if (isShuttingDown) return;

    	if (engine == null) return;
    	if (engine.getRenderSystem() == null) return;
		if(gameState == GameState.MENU) {
			mouseModeInitiated = false;
			Vector3f titleColor = new Vector3f(0.95f, 0.8f, 0.45f);
			Vector3f bodyColor = new Vector3f(1.0f, 1.0f, 1.0f);
			Vector3f footerColor = new Vector3f(0.7f, 0.9f, 0.7f);

			engine.getHUDmanager().setHUD1(menu.getTitleText(), titleColor, 520, 620);
			engine.getHUDmanager().setHUD2(menu.getMenuText(), bodyColor, 120, 560);
			engine.getHUDmanager().setHUD3(menu.getFooterText(), footerColor, 420, 120);
			return;
		}
		lastFrameTime = currFrameTime;
		currFrameTime = System.currentTimeMillis();
		float dt = (float)((currFrameTime - lastFrameTime) / 1000.0);
		elapsTime += dt;

		cam = engine.getRenderSystem().getViewport("MAIN").getCamera();
		im.update(dt);
		processNetworking(dt); // updates the network connection

		if (!mouseModeInitiated) initMouseMode();
		orbitCam.updateCameraPosition();

		if (playerS != null) playerS.updateAnimation();
		if (skinnyS != null) skinnyS.updateAnimation();
		if (apeS != null) apeS.updateAnimation();

		//snap to terrain 
		snapToTerrain(skinny);
		snapToTerrain(ape);

		Vector3f camN = cam.getN();
		float flatX = camN.x;
		float flatZ = camN.z;

		if (java.lang.Math.abs(flatX) > 0.0001f || java.lang.Math.abs(flatZ) > 0.0001f)
		{
			float yaw = (float)java.lang.Math.atan2(flatX, flatZ);
			player.setLocalRotation(new Matrix4f().rotationY(yaw));
		}

		Vector3f playerpos = player.getWorldLocation();
		float height = terr.getHeight(playerpos.x, playerpos.z);
		player.setLocalLocation(new Vector3f(playerpos.x(), height, playerpos.z()));
		Vector3f currentPos = player.getWorldLocation();
		float moveDist = currentPos.distance(prevPlayerPos);

		// small threshold so tiny float changes do not count as movement
		isMoving = moveDist > 0.001f;

		// handle swap timing
		if (playerS != null)
		{
			if (isSwapping)
			{
				swapTimer -= dt;
				if (swapTimer <= 0.0f)
				{
					isSwapping = false;

					if (isMoving)
						playerS.playAnimation("RUN", 0.03f, AnimatedShape.EndType.LOOP, 0);
					else
						playerS.playAnimation("STAND", 0.5f, AnimatedShape.EndType.LOOP, 0);
				}
			}
			else
			{
				if (isMoving && !wasMoving)
					playerS.playAnimation("RUN", 0.3f, AnimatedShape.EndType.LOOP, 0);
				else if (!isMoving && wasMoving)
					playerS.playAnimation("STAND", 0.5f, AnimatedShape.EndType.LOOP, 0);
			}
		}

		wasMoving = isMoving;
		prevPlayerPos.set(currentPos);

		// Overhead camera follows dolphin from previous assignment and needs to be changed - was going to work on setting up the multiplayer network first
		camOver.setLocation(new Vector3f(playerpos.x + ohPanX, ohHeight, playerpos.z + ohPanZ));
		camOver.setU(new Vector3f(1, 0, 0));
		camOver.setV(new Vector3f(0, 0, -1));
		camOver.setN(new Vector3f(0, -1, 0));

		// HUD that was brought over from A2
		String posStr = String.format("Player Pos: X(%.2f) Y(%.2f) Z(%.2f)", playerpos.x, playerpos.y, playerpos.z);
		Vector3f hudColor = new Vector3f(1, 1, 1);
		engine.getHUDmanager().setHUD1(posStr, hudColor, 15, 15);

		// animate ammo pickup: bob up and down
		ammoBobTime += dt;
		float bobOffset = (float)java.lang.Math.sin(ammoBobTime * 2.0f) * 0.25f;
		float terrainY = terr.getHeight(ammoBasePos.x, ammoBasePos.z);
		ammoPickup.setLocalTranslation(
			new Matrix4f().translation(
				ammoBasePos.x,
				terrainY + 0.75f + bobOffset,
				ammoBasePos.z));

		// animate health pickup: rotate around Y axis
		healthSpin += dt * 45.0f; // degrees per second
		terrainY = terr.getHeight(healthBasePos.x, healthBasePos.z);
		healthPickup.setLocalRotation(
			new Matrix4f().rotationY((float)java.lang.Math.toRadians(healthSpin)));
		healthPickup.setLocalTranslation(
			new Matrix4f().translation(
				healthBasePos.x,
				terrainY + 0.75f,
				healthBasePos.z));

		if (ammoPickup != null && ammoLight != null) {
			ammoLight.setLocation(ammoPickup.getWorldLocation());
		}

		if (healthPickup != null && healthLight != null) {
			healthLight.setLocation(healthPickup.getWorldLocation());
		}
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		if (gameState == GameState.MENU){
    switch (e.getKeyCode())
    {
        case KeyEvent.VK_UP:
            menu.moveUp();
            menuSelection = menu.getSelectedIndex();
            break;
        case KeyEvent.VK_DOWN:
            menu.moveDown();
            menuSelection = menu.getSelectedIndex();
            break;
        case KeyEvent.VK_LEFT:
            if (menu.getSelectedIndex() == 1){
                menu.previousMap();
                setMapSelection(menu.getSelectedMapIndex());
                applyMapSelection();
            }
            break;
        case KeyEvent.VK_RIGHT:
            if (menu.getSelectedIndex() == 1)  {
                menu.nextMap();
                setMapSelection(menu.getSelectedMapIndex());
                applyMapSelection();
            }
            break;
        case KeyEvent.VK_ENTER:
            switch (menu.activateSelection())  {
                case START_GAME:
                    setMapSelection(menu.getSelectedMapIndex());
                    applyMapSelection();
                    gameState = GameState.PLAYING;
                    engine.getHUDmanager().setHUD1("", new Vector3f(1, 1, 1), 0, 0);
                    engine.getHUDmanager().setHUD2("", new Vector3f(1, 1, 1), 0, 0);
                    engine.getHUDmanager().setHUD3("", new Vector3f(1, 1, 1), 0, 0);
                    break;
                case SELECT_MAP:
                    menu.nextMap();
                    setMapSelection(menu.getSelectedMapIndex());
                    applyMapSelection();
                    break;
                case MULTIPLAYER:
                    break;
                case OPTIONS:
                    break;
                case QUIT:
                    isShuttingDown = true;
                    mouseModeInitiated = false;
                    isRecentering = false;
                    isSwapping = false;
                    shutdown();
                    System.exit(0);
                    return;
                default:
                    System.out.println("Menu option not implemented yet: " + menu.getSelectedItem());
                    break;
            }
            	break;
        	default:
       	    	break;
	}
	}	
		if (gameState == GameState.PLAYING){
			switch (e.getKeyCode())
			{
				case KeyEvent.VK_ESCAPE:
					gameState = GameState.PAUSED;
					engine.getHUDmanager().setHUD1("PAUSED", new Vector3f(1, 1, 1), 600, 360);
					break;
				case KeyEvent.VK_BACK_SLASH:
					restartGame = new RestartGame(this);
					restartGame.performAction(0, null);
					break;
				default:
					break;
			}
		}
	super.keyPressed(e);
}

	@Override
	public void mouseMoved(MouseEvent e)
	{
		if (isShuttingDown) return;
		if (!mouseModeInitiated) return;
		if (orbitCam == null) return;

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
		float mouseDeltaY = prevMouseY - curMouseY;

		float xSensitivity = mouseDeltaX * sensitvity;
		float ySensitivity = mouseDeltaY * -sensitvity;

		orbitCam.addAzimuth(xSensitivity);
		orbitCam.addElevation(ySensitivity);

		prevMouseX = curMouseX;
		prevMouseY = curMouseY;

		recenterMouse();
		prevMouseX = centerX;
		prevMouseY = centerY;
	}

	private void recenterMouse()
	{
		if (isShuttingDown) return;
		if (robot == null) return;
		if (engine == null) return;

		RenderSystem rs = engine.getRenderSystem();
		if (rs == null) return;

		Viewport vw = rs.getViewport("MAIN");
		if (vw == null) return;

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

		playSwapAnimation();
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
	
	//Calculates the angle of the next portion of terrain to ensure player cannot run up slopes that are too steep
	public boolean canMoveOnTerrain(Vector3f from, Vector3f to)
{
    if (terr == null) return true;

    float currentH = terr.getHeight(from.x(), from.z());
    float nextH = terr.getHeight(to.x(), to.z());

    float rise = nextH - currentH;
    float run = (float)Math.sqrt(
        (to.x() - from.x()) * (to.x() - from.x()) +
        (to.z() - from.z()) * (to.z() - from.z())
    );

    if (run < 0.0001f) return true;

    float slope = rise / run;

    if (rise > maxStepHeight) return false;      // too big a step upward
    if (slope > maxClimbSlope) return false; // too steep uphill

    return true;
}
private void applyMapSelection()
{
    if (terr == null) return;

    switch (mapSelection)
    {
        case 0:
            terr.setTextureImage(terrTxMap0);
            terr.setHeightMap(heightMap0);
			(engine.getSceneGraph()).setActiveSkyBoxTexture(fluffySkyBox); //sets the scene to this skybox
            terr.getRenderStates().setTileFactor(10);
            break;

        case 1:
            terr.setTextureImage(terrTxMap1);
            terr.setHeightMap(heightMap1);
			(engine.getSceneGraph()).setActiveSkyBoxTexture(spaceSkyBox); //sets the scene to this skybox
            terr.getRenderStates().setTileFactor(100);
            break;

        default:
            terr.setTextureImage(terrTxMap0);
            terr.setHeightMap(heightMap0);
            terr.getRenderStates().setTileFactor(10);
            break;
    }
}

}