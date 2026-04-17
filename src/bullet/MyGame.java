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
	private GameObject player, skinny, ape, knife, pistol, plasmaRifle, rifle, shotGun, apePlasmaRifle, terr, centerBuilding;

	//instances of game objects for repeat use
	private GameObject[] smallBuildings = new GameObject[8];
	private GameObject[] smallBuildings2 = new GameObject[8];
	private GameObject[] ammoPickups;
	private GameObject[] healthPickups;

	private boolean[] ammoActive;
	private boolean[] healthActive;
	private float[] ammoRespawnTimers;
	private float[] healthRespawnTimers;

	// shapes for animated objects
	private AnimatedShape playerS, skinnyS, apeS;

	// player animation values
	private boolean isMoving = false;
	private boolean wasMoving = false;
	private Vector3f prevPlayerPos = new Vector3f(0,0,0);

	// player stats
	private int pHealth = 100;
	private int pAmmo = 10;

	private final int pHealthMin = 0;
	private final int pHealthMax = 150;
	private final int pAmmoMin = 0;
	private final int pAmmoMax = 30;

	// pickup respawn
	private final float pickupRespawnTime = 30.0f;

	// pickup collision tuning
	private final float pickupCollisionRange = 1.5f;
	private final float hiddenPickupScale = 0.0001f;

	// audio
	private tage.audio.IAudioManager audioMgr;
	private tage.audio.Sound hPsound;
	private tage.audio.Sound aPsound;

	// shapes and textures for game objects
	private ObjShape ammoS, terrS, healthS, plasmaRifleS, rifleS, shotGunS, knifeS, pistolS, smallBuildingS, smallBuilding2S, centerBuildingS;

	private TextureImage playerTx, terrTxMap0, terrTxMap1, ammoTx, healthTx, plasmaRifleTx, rifleTx, shotGunTx, knifeTx, pistolTx, heightMap0, heightMap1, skinnyTx, apeTx, smallBuildingTx, smallBuilding2Tx, centerBuildingTx;

	//pickup object animation values
	private float ammoBobTime = 0.0f;
	private float healthSpin = 0.0f;

	//pickup positions
	private final Vector3f[] ammoSpawnPositions = 
	{
		new Vector3f(-46.0f, 0.0f, -13.0f),
		new Vector3f(-91.0f, 0.0f,  56.0f),
		new Vector3f(-58.0f, 0.0f,  89.0f),
		new Vector3f(-36.0f, 0.0f,  76.0f),
		new Vector3f( 60.0f, 0.0f, -19.0f),
		new Vector3f( 94.0f, 0.0f, -36.0f),
		new Vector3f( 68.0f, 0.0f, -81.0f),
		new Vector3f( 37.0f, 0.0f,   4.0f)
	};

	private final Vector3f[] healthSpawnPositions = 
	{
		new Vector3f(-44.0f, 0.0f, -11.0f),
		new Vector3f(-89.0f, 0.0f,  58.0f),
		new Vector3f(-56.0f, 0.0f,  91.0f),
		new Vector3f(-29.0f, 0.0f,  30.0f),
		new Vector3f( 62.0f, 0.0f, -17.0f),
		new Vector3f( 96.0f, 0.0f, -34.0f),
		new Vector3f( 70.0f, 0.0f, -79.0f),
		new Vector3f( 26.0f, 0.0f, -90.0f)
	};

	//object init locations and scale
	private Vector3f playerStartPos = new Vector3f(0.0f, 0.75f, 0.0f);
	private float playerScale = 0.01f;
	private float ammoScale = 0.3f;
	private float healthScale = 0.3f;

	//Movement Variables
	private static final float maxClimbSlope = 1.2f;
	private static final float maxStepHeight = 0.5f;

	// weapon cycling
	private int currentWeaponIndex = 0;   // 0 = knife, 1 = pistol, 2 = plasma rifle, 3 = rifle, 4 = shotgun

	// shared first-pass weapon transform
	private Vector3f weaponPos = new Vector3f(-0.2f, 1.4f, 0.65f); // old hip vector: (0.18f, 1.10f, 0.28f);
	private float weaponScale = 0.5f;
	private float knifeWeaponScale = 6f;
	private float weaponRotY = 0.0f;

	// hidden scale for inactive weapons
	private final float hiddenWeaponScale = 0.0001f;

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
	private Light[] ammoLights;
	private Light[] healthLights;
	
	//map selection
	private int mapSelection = 0;
	
	//getter functions
	public GameObject getAvatar() { return player; }
	public Camera getCamera() { return cam; }
	public ObjShape getGhostShape() { return ghostS; }
	public TextureImage getGhostTexture() { return ghostT; }
	public GhostManager getGhostManager() { return gm; }
	public float getPlayerScale() { return playerScale; }
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
	private AnimatedShape ghostS;
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

	private void snapToTerrain(GameObject obj)
	{
		if (obj == null || terr == null) return;

		Vector3f pos = obj.getWorldLocation();
		float height = terr.getHeight(pos.x, pos.z);

		obj.setLocalLocation(new Vector3f(pos.x, height, pos.z));
	}

	private void attachWeaponToPlayer(GameObject weapon)
	{
		if (weapon == null || player == null) return;

		weapon.setParent(player);
		weapon.propagateTranslation(true);
		weapon.propagateRotation(true);
		weapon.propagateScale(true);
		weapon.applyParentRotationToPosition(true);
	}

	private void updateWeaponVisibility()
	{
		if (knife != null)
		{
			if (currentWeaponIndex == 0) {
				knife.setLocalScale(new Matrix4f().scaling(knifeWeaponScale));
				knife.getRenderStates().setModelOrientationCorrection((new Matrix4f())
    			.rotateY((float)java.lang.Math.toRadians(-90.0f)).rotateZ((float)java.lang.Math.toRadians(25.0f)));
			} else {
				knife.setLocalScale(new Matrix4f().scaling(hiddenWeaponScale));
			}
		}

		if (pistol != null)
		{
			if (currentWeaponIndex == 1)
				pistol.setLocalScale(new Matrix4f().scaling(weaponScale));
			else
				pistol.setLocalScale(new Matrix4f().scaling(hiddenWeaponScale));
		}

		if (plasmaRifle != null)
		{
			if (currentWeaponIndex == 2)
				plasmaRifle.setLocalScale(new Matrix4f().scaling(weaponScale));
			else
				plasmaRifle.setLocalScale(new Matrix4f().scaling(hiddenWeaponScale));
		}

		if (rifle != null)
		{
			if (currentWeaponIndex == 3)
				rifle.setLocalScale(new Matrix4f().scaling(weaponScale));
			else
				rifle.setLocalScale(new Matrix4f().scaling(hiddenWeaponScale));
		}

		if (shotGun != null)
		{
			if (currentWeaponIndex == 4)
				shotGun.setLocalScale(new Matrix4f().scaling(weaponScale + 0.8f));
			else
				shotGun.setLocalScale(new Matrix4f().scaling(hiddenWeaponScale));
		}
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
				
				ghostS = new AnimatedShape("Robot.rkm", "Robot.rks");
				ghostS.loadAnimation("RUN", "RobotRun.rka");
				ghostS.loadAnimation("STAND", "RobotStanding.rka");
				ghostS.loadAnimation("SWAP", "RobotSwapGun.rka");
				ghostS.loadAnimation("SWAPRUN", "RobotSwapGunRun.rka");

				skinnyS = new AnimatedShape("skinny.rkm", "skinny.rks");
				skinnyS.loadAnimation("WAVE", "wave.rka");

				apeS = new AnimatedShape("ape.rkm", "ape.rks");
				apeS.loadAnimation("RUN", "apeRun.rka");

				smallBuildingS = new ImportedModel("smallBuilding.obj");
				smallBuilding2S = new ImportedModel("smallBuilding2.obj");
				centerBuildingS = new ImportedModel("centerBuilding.obj");

				ammoS = new ImportedModel("ammo.obj");
				healthS = new ImportedModel("health.obj");
				knifeS = new ImportedModel("knife.obj");
				pistolS = new ImportedModel("pistol.obj");
				plasmaRifleS = new ImportedModel("plasmaRifle.obj");
				rifleS = new ImportedModel("rifle.obj");
				shotGunS = new ImportedModel("shotGun.obj");
				break;
			case 1:
				playerS = new AnimatedShape("Robot.rkm", "Robot.rks");
				playerS.loadAnimation("RUN", "RobotRun.rka");
				playerS.loadAnimation("SWAP", "RobotSwapGun.rka");
				playerS.loadAnimation("SWAPRUN", "RobotSwapGunRun.rka");

				ghostS = new AnimatedShape("Robot.rkm", "Robot.rks");
				ghostS.loadAnimation("RUN", "RobotRun.rka");
				ghostS.loadAnimation("STAND", "RobotStanding.rka");
				ghostS.loadAnimation("SWAP", "RobotSwapGun.rka");
				ghostS.loadAnimation("SWAPRUN", "RobotSwapGunRun.rka");

				ammoS = new ImportedModel("ammo.obj");
				healthS = new ImportedModel("health.obj");
				plasmaRifleS = new ImportedModel("plasmaRifle.obj");
				rifleS = new ImportedModel("rifle.obj");
				shotGunS = new ImportedModel("shotGun.obj");
				knifeS = new ImportedModel("knife.obj");
				pistolS = new ImportedModel("pistol.obj");
				break;
		}
	}

	@Override
	public void loadTextures()
	{
  		playerTx = new TextureImage("robot.jpg");
    	ghostT = playerTx;

		skinnyTx = new TextureImage("skinny.jpg");

		apeTx = new TextureImage("ape.jpg");

		smallBuildingTx = new TextureImage("smallBuilding.png");
		smallBuilding2Tx = new TextureImage("smallBuilding2.png");
		centerBuildingTx = new TextureImage("centerBuilding.jpg");

    	ammoTx = new TextureImage("ammo.jpg");
    	healthTx = new TextureImage("health.jpg");
    	plasmaRifleTx = new TextureImage("plasmaRifle.jpg");
		rifleTx = new TextureImage("rifle.jpg");
		shotGunTx = new TextureImage("shotGun.jpg");
		knifeTx = new TextureImage("knife.png");
		pistolTx = new TextureImage("pistol.jpg");
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

	ammoPickups = new GameObject[ammoSpawnPositions.length];
	ammoActive = new boolean[ammoSpawnPositions.length];
	ammoRespawnTimers = new float[ammoSpawnPositions.length];

	for (int i = 0; i < ammoSpawnPositions.length; i++)
	{
		ammoPickups[i] = new GameObject(GameObject.root(), ammoS, ammoTx);
		ammoPickups[i].setLocalTranslation(
			new Matrix4f().translation(ammoSpawnPositions[i].x, 0.0f, ammoSpawnPositions[i].z));
		ammoPickups[i].setLocalScale(new Matrix4f().scaling(ammoScale));
		ammoActive[i] = true;
	}

	healthPickups = new GameObject[healthSpawnPositions.length];
	healthActive = new boolean[healthSpawnPositions.length];
	healthRespawnTimers = new float[healthSpawnPositions.length];

	for (int i = 0; i < healthSpawnPositions.length; i++)
	{
		healthPickups[i] = new GameObject(GameObject.root(), healthS, healthTx);
		healthPickups[i].setLocalTranslation(
			new Matrix4f().translation(healthSpawnPositions[i].x, 0.0f, healthSpawnPositions[i].z));
		healthPickups[i].setLocalScale(new Matrix4f().scaling(healthScale));
		healthPickups[i].getRenderStates().setModelOrientationCorrection(
			(new Matrix4f()).rotationX((float)java.lang.Math.toRadians(90.0f)));
		healthActive[i] = true;
	}

	// knife
	knife = new GameObject(GameObject.root(), knifeS, knifeTx);
	knife.setLocalTranslation(new Matrix4f().translation(weaponPos.x, weaponPos.y, weaponPos.z));
	knife.setLocalRotation(new Matrix4f().rotationY((float)java.lang.Math.toRadians(weaponRotY)));
	knife.setLocalScale(new Matrix4f().scaling(weaponScale));
	attachWeaponToPlayer(knife);

	// pistol
	pistol = new GameObject(GameObject.root(), pistolS, pistolTx);
	pistol.setLocalTranslation(new Matrix4f().translation(weaponPos.x, weaponPos.y, weaponPos.z));
	pistol.setLocalRotation(new Matrix4f().rotationY((float)java.lang.Math.toRadians(weaponRotY)));
	pistol.setLocalScale(new Matrix4f().scaling(weaponScale));
	attachWeaponToPlayer(pistol);

	// plasma rifle
	plasmaRifle = new GameObject(GameObject.root(), plasmaRifleS, plasmaRifleTx);
	plasmaRifle.setLocalTranslation(new Matrix4f().translation(weaponPos.x, weaponPos.y, weaponPos.z));
	plasmaRifle.setLocalRotation(new Matrix4f().rotationY((float)java.lang.Math.toRadians(weaponRotY)));
	plasmaRifle.setLocalScale(new Matrix4f().scaling(weaponScale));
	attachWeaponToPlayer(plasmaRifle);

	// rifle
	rifle = new GameObject(GameObject.root(), rifleS, rifleTx);
	rifle.setLocalTranslation(new Matrix4f().translation(weaponPos.x, weaponPos.y, weaponPos.z));
	rifle.setLocalRotation(new Matrix4f().rotationY((float)java.lang.Math.toRadians(weaponRotY)));
	rifle.setLocalScale(new Matrix4f().scaling(weaponScale));
	rifle.getRenderStates().setModelOrientationCorrection((new Matrix4f())
	.rotateX((float)java.lang.Math.toRadians(90.0f)));
	attachWeaponToPlayer(rifle);

	// shotgun
	shotGun = new GameObject(GameObject.root(), shotGunS, shotGunTx);
	shotGun.setLocalTranslation(new Matrix4f().translation(weaponPos.x, weaponPos.y - 0.25f, weaponPos.z - 0.2f));
	shotGun.setLocalRotation(new Matrix4f().rotationY((float)java.lang.Math.toRadians(weaponRotY)));
	shotGun.setLocalScale(new Matrix4f().scaling(weaponScale));
	shotGun.getRenderStates().setModelOrientationCorrection((new Matrix4f())
	.rotateY((float)java.lang.Math.toRadians(90.0f))
    .rotateX((float)java.lang.Math.toRadians(90.0f)));
	attachWeaponToPlayer(shotGun);

	// start on knife
	currentWeaponIndex = 0;
	updateWeaponVisibility();

	// center building at terrain center
	centerBuilding = new GameObject(GameObject.root(), centerBuildingS, centerBuildingTx);
	centerBuilding.setLocalTranslation(new Matrix4f().translation(0.0f, 0.0f, 0.0f));
	centerBuilding.setLocalScale(new Matrix4f().scaling(1.5f));
	snapToTerrain(centerBuilding);

	float[][] sbPositions = {
		{-52.0f,  72.0f},
		{ 48.0f, -65.0f},
		{ 79.0f, -21.0f},
		{-83.0f,  39.0f},
		{-18.0f,  40.0f},
		{ 20.0f, -38.0f},
		{ 24.0f,   5.0f},
		{ 28.0f,  38.0f}
	};

	float[][] sb2Positions = {
		{ 78.0f, -37.0f},
		{-79.0f,  16.0f},
		{-50.0f,  51.0f},
		{-40.0f,  22.0f},
		{-24.0f, -18.0f},
		{ -5.0f,  35.0f},
		{ 10.0f, -42.0f},
		{ 44.0f, -10.0f}
	};

	// build 8 smallBuilding objects
	for (int i = 0; i < smallBuildings.length; i++)
	{
		smallBuildings[i] = new GameObject(GameObject.root(), smallBuildingS, smallBuildingTx);
		smallBuildings[i].setLocalTranslation(
		new Matrix4f().translation(sbPositions[i][0], 0.0f, sbPositions[i][1]));
		smallBuildings[i].setLocalScale(new Matrix4f().scaling(1.0f));
		snapToTerrain(smallBuildings[i]);
	}

	// build 8 smallBuilding2 objects
	for (int i = 0; i < smallBuildings2.length; i++)
	{
		smallBuildings2[i] = new GameObject(GameObject.root(), smallBuilding2S, smallBuilding2Tx);
		smallBuildings2[i].setLocalTranslation(
			new Matrix4f().translation(sb2Positions[i][0], 0.0f, sb2Positions[i][1]));
		smallBuildings2[i].setLocalScale(new Matrix4f().scaling(1.0f));
		snapToTerrain(smallBuildings2[i]);
	}

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

				ammoLights = new Light[ammoSpawnPositions.length];
				for (int i = 0; i < ammoLights.length; i++)
				{
					ammoLights[i] = new Light();
					ammoLights[i].setDiffuse(0.2f, 1.0f, 0.2f);
					ammoLights[i].setSpecular(0.2f, 0.6f, 1.0f);
					ammoLights[i].setAmbient(0.05f, 0.1f, 0.2f);
					ammoLights[i].setType(Light.LightType.SPOTLIGHT);
					ammoLights[i].setDirection(new Vector3f(0.0f, -1.0f, 0.0f));
					ammoLights[i].setCutoffAngle(20.0f);
					ammoLights[i].setOffAxisExponent(10.0f);

					engine.getSceneGraph().addLight(ammoLights[i]);
				}

				healthLights = new Light[healthSpawnPositions.length];
				for (int i = 0; i < healthLights.length; i++)
				{
					healthLights[i] = new Light();
					healthLights[i].setDiffuse(0.2f, 0.6f, 1.0f);
					healthLights[i].setSpecular(0.2f, 1.0f, 0.2f);
					healthLights[i].setAmbient(0.05f, 0.2f, 0.05f);
					healthLights[i].setType(Light.LightType.SPOTLIGHT);
					healthLights[i].setDirection(new Vector3f(0.0f, -1.0f, 0.0f));
					healthLights[i].setCutoffAngle(20.0f);
					healthLights[i].setOffAxisExponent(10.0f);

					engine.getSceneGraph().addLight(healthLights[i]);
				}
				break;
			case 1:
				Light.setGlobalAmbient(0.5f, 0.5f, 0.5f);

				mainLight = new Light();
				mainLight.setLocation(new Vector3f(0.0f, 0.0f, 0.0f));

				engine.getSceneGraph().addLight(mainLight);

				ammoLights = new Light[ammoSpawnPositions.length];
				for (int i = 0; i < ammoLights.length; i++)
				{
					ammoLights[i] = new Light();
					ammoLights[i].setDiffuse(0.2f, 1.0f, 0.2f);
					ammoLights[i].setSpecular(0.2f, 0.6f, 1.0f);
					ammoLights[i].setAmbient(0.05f, 0.1f, 0.2f);
					ammoLights[i].setType(Light.LightType.SPOTLIGHT);
					ammoLights[i].setDirection(new Vector3f(0.0f, -1.0f, 0.0f));
					ammoLights[i].setCutoffAngle(20.0f);
					ammoLights[i].setOffAxisExponent(10.0f);

					engine.getSceneGraph().addLight(ammoLights[i]);
				}

				healthLights = new Light[healthSpawnPositions.length];
				for (int i = 0; i < healthLights.length; i++)
				{
					healthLights[i] = new Light();
					healthLights[i].setDiffuse(0.2f, 0.6f, 1.0f);
					healthLights[i].setSpecular(0.2f, 1.0f, 0.2f);
					healthLights[i].setAmbient(0.05f, 0.2f, 0.05f);
					healthLights[i].setType(Light.LightType.SPOTLIGHT);
					healthLights[i].setDirection(new Vector3f(0.0f, -1.0f, 0.0f));
					healthLights[i].setCutoffAngle(20.0f);
					healthLights[i].setOffAxisExponent(10.0f);

					engine.getSceneGraph().addLight(healthLights[i]);
				}
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

	private void initAudio()
	{
		audioMgr = engine.getAudioManager();
		if (audioMgr == null)
		{
			System.out.println("Audio manager not available from engine.");
			return;
		}

		tage.audio.AudioResource healthRes =
			audioMgr.createAudioResource("healthPickup.wav", tage.audio.AudioResourceType.AUDIO_SAMPLE);

		tage.audio.AudioResource ammoRes =
			audioMgr.createAudioResource("ammoPickup.wav", tage.audio.AudioResourceType.AUDIO_SAMPLE);

		hPsound = new tage.audio.Sound(healthRes, tage.audio.SoundType.SOUND_EFFECT, 75, false);
		aPsound = new tage.audio.Sound(ammoRes, tage.audio.SoundType.SOUND_EFFECT, 75, false);

		hPsound.initialize(audioMgr);
		aPsound.initialize(audioMgr);
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

		//setup audio
		initAudio();
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
		if (gm != null) gm.updateGhostAnimations(dt);
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

		handlePickupCollisions(dt);

		// small threshold so tiny float changes do not count as movement
		isMoving = moveDist > 0.001f;

		// handle swap timing
		if (playerS != null)
		{
				if (isMoving && !wasMoving)
					playerS.playAnimation("RUN", 0.3f, AnimatedShape.EndType.LOOP, 0);
				else if (!isMoving && wasMoving)
					playerS.playAnimation("STAND", 0.5f, AnimatedShape.EndType.LOOP, 0);
		}

		wasMoving = isMoving;
		prevPlayerPos.set(currentPos);

		// Overhead camera follows dolphin from previous assignment and needs to be changed - was going to work on setting up the multiplayer network first
		camOver.setLocation(new Vector3f(playerpos.x + ohPanX, ohHeight, playerpos.z + ohPanZ));
		camOver.setU(new Vector3f(1, 0, 0));
		camOver.setV(new Vector3f(0, 0, -1));
		camOver.setN(new Vector3f(0, -1, 0));

		String healthStr = "Health: " + pHealth;
		String ammoStr = "Ammo: " + pAmmo;
		String posStr = String.format("Player Pos: X(%.2f) Y(%.2f) Z(%.2f)", playerpos.x, playerpos.y, playerpos.z);

		Vector3f healthColor = new Vector3f(0, 1, 0);   // green
		Vector3f ammoColor = new Vector3f(1, 1, 1);     // white
		Vector3f posColor = new Vector3f(1, 1, 1);      // white

		engine.getHUDmanager().setHUD1(healthStr, healthColor, 15, 660);
		engine.getHUDmanager().setHUD2(ammoStr, ammoColor, 15, 630);
		engine.getHUDmanager().setHUD3(posStr, posColor, 15, 15);

		ammoBobTime += dt;
		float bobOffset = (float)java.lang.Math.sin(ammoBobTime * 2.0f) * 0.25f;

		for (int i = 0; i < ammoPickups.length; i++)
		{
			float terrainY = terr.getHeight(ammoSpawnPositions[i].x, ammoSpawnPositions[i].z);
			ammoPickups[i].setLocalTranslation(
				new Matrix4f().translation(
					ammoSpawnPositions[i].x,
					terrainY + 0.75f + bobOffset,
					ammoSpawnPositions[i].z));
		}

		healthSpin += dt * 45.0f;

		for (int i = 0; i < healthPickups.length; i++)
		{
			float terrainY = terr.getHeight(healthSpawnPositions[i].x, healthSpawnPositions[i].z);
			healthPickups[i].setLocalRotation(
				new Matrix4f().rotationY((float)java.lang.Math.toRadians(healthSpin)));
			healthPickups[i].setLocalTranslation(
				new Matrix4f().translation(
					healthSpawnPositions[i].x,
					terrainY + 0.75f,
					healthSpawnPositions[i].z));
		}

		for (int i = 0; i < ammoPickups.length; i++)
		{
			if (ammoPickups[i] != null && ammoLights[i] != null)
			{
				ammoLights[i].setLocation(ammoPickups[i].getWorldLocation());
			}
		}

		for (int i = 0; i < healthPickups.length; i++)
		{
			if (healthPickups[i] != null && healthLights[i] != null)
			{
				healthLights[i].setLocation(healthPickups[i].getWorldLocation());
			}
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
					if (hPsound != null && audioMgr != null)
						hPsound.release(audioMgr);
					if (aPsound != null && audioMgr != null)
						aPsound.release(audioMgr);
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

		if (clicks > 0)
		{
			currentWeaponIndex = (currentWeaponIndex + 1) % 5;
		}
		else if (clicks < 0)
		{
			currentWeaponIndex = (currentWeaponIndex + 4) % 5; // same as -1 mod 5
		}

		updateWeaponVisibility();
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

	// Player health and ammo management
	public void setPlayerHealth(int value)
	{
		pHealth = java.lang.Math.max(pHealthMin, java.lang.Math.min(value, pHealthMax));
	}

	public void setPlayerAmmo(int value)
	{
		pAmmo = java.lang.Math.max(pAmmoMin, java.lang.Math.min(value, pAmmoMax));
	}

	public void addPlayerHealth(int amount)
	{
		setPlayerHealth(pHealth + amount);
	}

	public void addPlayerAmmo(int amount)
	{
		setPlayerAmmo(pAmmo + amount);
	}

	public int getPlayerHealth()
	{
		return pHealth;
	}

	public int getPlayerAmmo()
	{
		return pAmmo;
	}

	private void hideAmmoPickup(int index)
	{
		ammoActive[index] = false;
		ammoRespawnTimers[index] = pickupRespawnTime;

		if (ammoPickups[index] != null)
			ammoPickups[index].setLocalScale(new Matrix4f().scaling(hiddenPickupScale));
	}

	private void showAmmoPickup(int index)
	{
		ammoActive[index] = true;

		if (ammoPickups[index] != null)
			ammoPickups[index].setLocalScale(new Matrix4f().scaling(ammoScale));
	}

	private void hideHealthPickup(int index)
	{
		healthActive[index] = false;
		healthRespawnTimers[index] = pickupRespawnTime;

		if (healthPickups[index] != null)
			healthPickups[index].setLocalScale(new Matrix4f().scaling(hiddenPickupScale));
	}

	private void showHealthPickup(int index)
	{
		healthActive[index] = true;

		if (healthPickups[index] != null)
			healthPickups[index].setLocalScale(new Matrix4f().scaling(healthScale));
	}

	private void handlePickupCollisions(float dt)
	{
		if (player == null) return;

		Vector3f playerPos = player.getWorldLocation();

		for (int i = 0; i < ammoPickups.length; i++)
		{
			if (!ammoActive[i])
			{
				ammoRespawnTimers[i] -= dt;
				if (ammoRespawnTimers[i] <= 0.0f)
				{
					ammoRespawnTimers[i] = 0.0f;
					showAmmoPickup(i);
				}
			}

			if (ammoActive[i] && ammoPickups[i] != null &&
				playerPos.distance(ammoPickups[i].getWorldLocation()) <= pickupCollisionRange)
			{
				addPlayerAmmo(10);
				hideAmmoPickup(i);

				if (aPsound != null)
					aPsound.play();
			}
		}

		for (int i = 0; i < healthPickups.length; i++)
		{
			if (!healthActive[i])
			{
				healthRespawnTimers[i] -= dt;
				if (healthRespawnTimers[i] <= 0.0f)
				{
					healthRespawnTimers[i] = 0.0f;
					showHealthPickup(i);
				}
			}

			if (healthActive[i] && healthPickups[i] != null &&
				playerPos.distance(healthPickups[i].getWorldLocation()) <= pickupCollisionRange)
			{
				setPlayerHealth(pHealthMax);
				hideHealthPickup(i);

				if (hPsound != null)
					hPsound.play();
			}
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