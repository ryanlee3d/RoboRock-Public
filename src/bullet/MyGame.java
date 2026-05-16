package bullet;

import tage.*;
import tage.shapes.*;
import tage.input.*;
import tage.input.action.*;

import java.awt.*;
import java.awt.event.*;
import org.joml.*;
import org.joml.Math;
import java.util.Random;

// networking imports
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import tage.networking.IGameConnection.ProtocolType;
import java.util.Enumeration;
import java.util.UUID;

// physics imports
import tage.physics.PhysicsEngine;
import tage.physics.PhysicsObject;

// behavior tree imports
import tage.ai.behaviortrees.*;

import bullet.actions.*;
import bullet.audio.*;
import bullet.avatars.*;
import bullet.camera.*;
import bullet.combat.*;
import bullet.managers.*;
import bullet.network.*;
import bullet.rendering.*;
import bullet.states.*;
import bullet.ui.*;

public class MyGame extends VariableFrameRateGame implements MouseMotionListener, MouseListener, MouseWheelListener
{
    private static Engine engine;

    private boolean isShuttingDown = false;
    private boolean shutdownWindowHandlerInstalled = false;
    private GameState gameState = GameState.MENU;
    private final ShopState shopState = new ShopState();
    private int menuSelection = 0;
    private final MainMenu menu = new MainMenu();
    private String multiplayerStatusText = "";

    private int avatarSelection = 0;
    private final String[] avatarNames = {
        "Blue",
        "White",
        "Dark",
        "Grey"
    };

    private boolean physicsDebug = true;
    private final GameAudio gameAudio = new GameAudio();
    private final PickupManager pickupManager = new PickupManager(this);
    private final UfoWaveManager ufoWaveManager;

    private InputManager im;
    private CameraOrbit3D orbitCam;
    private Camera cam;
    private Camera camOver;

    private boolean firstPersonMode = false;
    private final OverheadCameraController overheadCameraController = new OverheadCameraController();

    // tweak these until the gun lines up: x = right, y = up, z = forward from player center
    private Vector3f fpOffset = new Vector3f(0.08f, 1.55f, -0.18f);

    private float sensitvity = 0.25f;

    // gamepad
    private static final float GAMEPAD_DEADZONE = 0.25f;
    private static final float GAMEPAD_LOOK_SPEED = 120.0f;
    private static final long GAMEPAD_MENU_REPEAT_MS = 180L;

    private float gamepadLookX = 0.0f;
    private float gamepadLookY = 0.0f;
    private boolean gamepadFireHeld = false;
    private boolean gamepadFireSeenThisFrame = false;
    private long lastGamepadMenuInputTime = 0L;

    private double lastFrameTime, currFrameTime, elapsTime;
    private IAction restartGame;

    // game objects
    private GameObject player, skinny, ape, knife, pistol, plasmaRifle, rifle, shotGun, apePlasmaRifle, terr, centerBuilding, brain, playerGrappleLine;

    // instances of game objects for repeat use
    private GameObject[] smallBuildings = new GameObject[8];
    private GameObject[] smallBuildings2 = new GameObject[8];

    // shapes for animated objects
    private AnimatedShape playerS, skinnyS, apeS, brainS;

    // player animation values
    private boolean isMoving = false;
    private boolean wasMoving = false;
    private Vector3f prevPlayerPos = new Vector3f(0, 0, 0);

    // player stats
    private int pHealth = 100;
    private boolean playerDeathScreenActive = false;

    private final int pHealthMin = 0;
    private final int pHealthMax = 150;
    private static final int MAX_RESPAWN_LIVES = 3;
    private int respawnLives = MAX_RESPAWN_LIVES;
    private int playerCredits = 0;

    private final WeaponInventory weaponInventory = new WeaponInventory();

    // shapes and textures for game objects
    private ObjShape ammoS, terrS, healthS, plasmaRifleS, rifleS, shotGunS, knifeS, pistolS, smallBuildingS, smallBuilding2S, centerBuildingS, ufoS, grappleGunS, skinnyGrappleLineS;

    private TextureImage playerTx, terrTxMap0, terrTxMap1, ammoTx, healthTx, plasmaRifleTx, rifleTx, shotGunTx, knifeTx, pistolTx,
        heightMap0, heightMap1, skinnyTx, apeTx, smallBuildingTx, smallBuilding2Tx, centerBuildingTx, ufoTx, brainTx, grappleGunTx;

    private TextureImage[] robotTextures = new TextureImage[4];

    //DEBUG
    private GameObject debugSkinny;
    private GameObject debugSkinnyPlasmaRifle;
    private GameObject debugSkinnyGrappleGun;

    // object init locations and scale
    private Vector3f playerStartPos = new Vector3f(-61.13f, 14.08f, 96.12f);
    private float playerScale = 0.01f;

    // player / terrain tuning
    private float playerCapsuleRadius = 0.5f;
    private float playerCapsuleHeight = 1.2f;
    private float playerVisualYOffset = 1.1f;      // move model upward/downward to align with physics capsule

    // Movement Variables
    private static final float maxClimbSlope = 1.2f;
    private static final float maxStepHeight = 0.5f;

    private Vector3f currentMoveDir = new Vector3f();
    private float currentMoveSpeed = 8.0f;

    // shared first-pass weapon transform
    private Vector3f weaponPos = new Vector3f(-0.2f, 1.4f, 0.65f);
    private float weaponScale = 0.5f;
    private float knifeWeaponScale = 6f;
    private float weaponRotY = 0.0f;  

    // hidden scale for inactive weapons
    private final float hiddenWeaponScale = 0.0001f;

    //grapple gun
    private GameObject grapplePickup;
    private boolean grapplePickupActive = false;
    private boolean playerHasGrapple = false;
    private boolean playerGrappling = false;
    private float grappleTimer = 0.0f;
    private final float grappleDuration = 0.75f;
    private final float grappleSpeed = 80.0f;
    private Vector3f grappleDir = new Vector3f();

    // lighting
    private Light mainLight;

    // physics
    private PhysicsEngine physicsEngine;
    private PhysicsObject playerP, terrainP, terrainP0, terrainP1;
    private boolean physicsRunning = true;

    // mouselook
    private Robot robot;
    private boolean mouseModeInitiated = false;
    private boolean isRecentering = false;
    private float centerX, centerY;
    private float prevMouseX, prevMouseY;
    private float curMouseX, curMouseY;
    private boolean cursorSet = false;

    // networking fields
    private GhostManager gm;
    private String serverAddress;
    private int serverPort;
    private ProtocolType serverProtocol;
    private ProtocolClient protClient;
    private GameServerUDP hostedServer;
    private DatagramSocket discoverySocket;
    private Thread discoveryThread;
    private volatile boolean discoveryRunning = false;
    private boolean isClientConnected = false;
    private boolean isHostClient = false;
    private float networkUpdateTimer = 0.0f;
    private static final float NETWORK_UPDATE_INTERVAL = 0.05f;
    private static final String DISCOVERY_REQUEST = "BULLET_DISCOVER_SERVER";
    private static final String DISCOVERY_RESPONSE = "BULLET_SERVER_HERE";
    private static final int DISCOVERY_TIMEOUT_MS = 1500;
    private NetworkEnemyManager networkEnemyManager;
    private float enemyNetworkUpdateTimer = 0.0f;
    private static final float ENEMY_NETWORK_UPDATE_INTERVAL = 0.10f;

    private boolean networkWaveHasSpawnedEnemies = false;
    private boolean ufoShopWindowUsedForCurrentWave = false;
    private boolean offlineMode = false;
    
    // skyboxes
    private int spaceSkyBox, islandSkyBox, lushSkyBox, plainsSkyBox;

    // map selection
    private int mapSelection = 0;
    private boolean pendingSkinnySpawn = false;
    private boolean pendingCaseOneStart = false;
    private boolean pendingDebugFinalUfoBeam = false;

    //floating brain
    private boolean brainFloating = false;

    // objective HUD state
    private boolean playerKnockedOffLevelTwo = false;
    private boolean brainDefeated = false;

    // brain boss AI
    private boolean brainActive = false;
    private float brainActionTimer = 3.0f;
    private float brainCircleAngle = 0.0f;
    private float brainCircleDir = 1.0f;
    private int brainAttackType = 0;
    private int brainShotsLeft = 0;
    private float brainShotTimer = 0.0f;
    private Vector3f brainRushTarget = new Vector3f();
    private boolean brainRushing = false;
    private int brainHealth = 5000;
    private final int brainMaxHealth = 5000;
    private static final int NETWORK_BRAIN_ID = -5000;

    // ghost rendering
    private AnimatedShape ghostS;
    private TextureImage ghostT;

    // bullet system
    private ObjShape bulletSphereS;
    private TextureImage bulletYellowTx;
    private TextureImage bulletBlueTx;
    private final BulletManager bulletManager = new BulletManager(this);

    private boolean isFiring = false;

    private float knifeStabTimer = 0.0f;
    private float knifeAttackCooldown = 0.0f;

    private static final float KNIFE_STAB_DURATION = 0.18f;
    private static final float KNIFE_ATTACK_COOLDOWN = 0.45f;
    private static final float KNIFE_RANGE = 2.5f;
    private static final int KNIFE_DAMAGE = 50;

    // plasma rifle burst fire control
    private int plasmaBurstShotsRemaining = 0;
    private float plasmaBurstTimer = 0.0f;
    private final float plasmaBurstDelay = 0.08f;

    // apes
    private java.util.ArrayList<GameObject> activeApes = new java.util.ArrayList<>();
    private java.util.ArrayList<Integer> activeApeNetworkIds = new java.util.ArrayList<>();
    private int nextNetworkEnemyId = 1;
    private java.util.ArrayList<PhysicsObject> activeApePhysics = new java.util.ArrayList<>();
    private java.util.ArrayList<Integer> activeApeHealth = new java.util.ArrayList<>();
    private java.util.ArrayList<Boolean> activeApeDead = new java.util.ArrayList<>();
    private java.util.ArrayList<Float> activeApeDeathTimers = new java.util.ArrayList<>();
    private java.util.ArrayList<Float> activeApeThinkTimers = new java.util.ArrayList<>();
    private java.util.ArrayList<Float> activeApeFireCooldowns = new java.util.ArrayList<>();
    private java.util.ArrayList<Float> activeApeStrafeDirs = new java.util.ArrayList<>();
    private java.util.ArrayList<BehaviorTree> activeApeTrees = new java.util.ArrayList<>();
    private java.util.ArrayList<PhysicsObject> buildingPhysics = new java.util.ArrayList<>();
    private java.util.ArrayList<GameObject> activeApeGuns = new java.util.ArrayList<>();

    private float apeThinkTimer = 0.0f;
    private final float apeThinkInterval = 0.25f;

    // skinwalker
    private java.util.ArrayList<GameObject> activeSkinnys = new java.util.ArrayList<>();
    private java.util.ArrayList<Integer> activeSkinnyNetworkIds = new java.util.ArrayList<>();
    private java.util.ArrayList<PhysicsObject> activeSkinnyPhysics = new java.util.ArrayList<>();
    private java.util.ArrayList<Integer> activeSkinnyHealth = new java.util.ArrayList<>();
    private java.util.ArrayList<Boolean> activeSkinnyDead = new java.util.ArrayList<>();
    private java.util.ArrayList<Float> activeSkinnyBounceTimers = new java.util.ArrayList<>();
    private java.util.ArrayList<GameObject> activeSkinnyPlasma = new java.util.ArrayList<>();
    private java.util.ArrayList<Float> activeSkinnyFireCooldowns = new java.util.ArrayList<>();
    private java.util.ArrayList<GameObject> activeSkinnyGrapple = new java.util.ArrayList<>();
    private java.util.ArrayList<GameObject> activeSkinnyGrappleLines = new java.util.ArrayList<>();
    private int skinnyKillCount = 0;



    // UFO tractor beam
    private boolean tractorBeamActive = false;
    private boolean playerInTractorBeam = false;
    private Light tractorBeamLight;
    // gameplay-only tractor beam: no physics collider, just X/Z distance + wave completion logic
    private Vector3f tractorBeamCenter = new Vector3f(0.0f, 0.0f, 0.0f);

    private final float tractorBeamRadius = 5.0f;
    private final float tractorBeamHeight = 35.0f;
    private final float tractorBeamLiftSpeed = 8.0f;
    private float tractorBeamTopY = 35.0f;

    // Level two
    private boolean levelTwoArrivalEventPending = false;
    private float levelTwoArrivalTimer = 0.0f;
    private float levelTwoThrowControlTimer = 0.0f;

    private final float levelTwoArrivalDelay = 1.0f;
    private final float levelTwoThrowBackSpeed = 30.0f;
    private final float levelTwoThrowUpSpeed = 6.0f;
    private final float levelTwoThrowControlDuration = 1.0f;

    private final Vector3f levelTwoThrowDir = new Vector3f(0.0f, 0.0f, 1.0f);
    // Background music control
    private boolean levelTwoMusicPending = false;
    private float levelTwoMusicTimer = 0.0f;
    private final float levelTwoMusicDelay = 2.0f; // 1 sec to roar, then 1 sec after roar
    private boolean bossMusicStarted = false;



    private final float worldGravity = -9.8f;
    private final float shotgunSpread = 0.12f;
    private final int shotgunPelletCount = 6;

    public MyGame(String serverAddress, int serverPort, String protocol)
    {
        super();
        gm = new GhostManager(this);
        networkEnemyManager = new NetworkEnemyManager(this);
        ufoWaveManager = new UfoWaveManager(this::spawnApe, this::getPlayerPosition);
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        serverProtocol = protocol.toUpperCase().compareTo("TCP") == 0 ? ProtocolType.TCP : ProtocolType.UDP;
    }

    private void enableOfflineHostMode()
    {
        if (offlineMode)
            return;

        offlineMode = true;
        isClientConnected = false;
        isHostClient = true;

        // Stop trying to send network messages when running solo.
        protClient = null;

        networkWaveHasSpawnedEnemies = false;
        ufoShopWindowUsedForCurrentWave = false;

        System.out.println("No server connection detected. Running local offline host mode.");
    }

    private boolean setupNetworking(String address, int port, boolean allowOfflineFallback)
    {
        isClientConnected = false;
        isHostClient = false;
        offlineMode = false;
        protClient = null;

        try
        {
            protClient = new ProtocolClient(
                InetAddress.getByName(address),
                port,
                serverProtocol,
                this
            );
            serverAddress = address;
            serverPort = port;
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
            if (allowOfflineFallback)
            {
                enableOfflineHostMode();
                return true;
            }

            multiplayerStatusText = "Could not connect to " + address + ":" + port;
            return false;
        }
        else
        {
            isClientConnected = true;
            protClient.sendJoinMessage();
            multiplayerStatusText = "Connected to " + address + ":" + port;
            return true;
        }
    }

    protected void processNetworking(float elapsTime)
    {
        if (protClient != null)
            protClient.processPackets();
    }

    @Override
    public void shutdown()
    {
        super.shutdown();
        shutdownNetworking();
    }

    private void shutdownNetworking()
    {
        stopDiscoveryResponder();

        if (protClient != null)
        {
            protClient.sendByeMessage();

            try
            {
                protClient.shutdown();
            }
            catch (IOException e)
            {
                System.out.println("Error shutting down client connection: " + e.getMessage());
            }

            protClient = null;
        }

        if (hostedServer != null)
        {
            try
            {
                hostedServer.shutdown();
            }
            catch (IOException e)
            {
                System.out.println("Error shutting down hosted server: " + e.getMessage());
            }

            hostedServer = null;
        }

        isClientConnected = false;
        isHostClient = false;
        offlineMode = false;
    }

    private void installShutdownWindowHandler()
    {
        if (shutdownWindowHandlerInstalled || engine == null || engine.getRenderSystem() == null)
            return;

        shutdownWindowHandlerInstalled = true;
        engine.getRenderSystem().setDefaultCloseOperation(javax.swing.JFrame.DO_NOTHING_ON_CLOSE);
        engine.getRenderSystem().addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                if (!isShuttingDown)
                {
                    isShuttingDown = true;
                    gameAudio.releaseAll();
                    shutdown();
                }

                System.exit(0);
            }
        });
    }

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

        setCrosshairCursor();
    }

    private void setCrosshairCursor()
    {
        if (cursorSet || engine == null) return;

        RenderSystem rs = engine.getRenderSystem();
        if (rs == null || rs.getGLCanvas() == null) return;

        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Image cursorImage = toolkit.getImage("./assets/textures/crosshairs.png");

        Point hotspot = new Point(16, 16); // for 32x32 image
        Cursor crosshairCursor =
            toolkit.createCustomCursor(cursorImage, hotspot, "Crosshair");

        rs.getGLCanvas().setCursor(crosshairCursor);
        cursorSet = true;
    }

    private void recenterMouse()
    {
        if (isShuttingDown || robot == null || engine == null) return;

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

    private void updateFirstPersonCamera()
    {
        if (cam == null || player == null) return;

        Vector3f playerPos = player.getWorldLocation();

        // use current camera basis so mouse-look still controls direction
        Vector3f camForward = new Vector3f(cam.getN()).mul(-1.0f).normalize();
        Vector3f worldUp = new Vector3f(0.0f, 1.0f, 0.0f);

        // compute right vector from current look direction
        Vector3f camRight = new Vector3f();
        worldUp.cross(camForward, camRight).normalize();

        // rebuild a clean orthonormal basis
        Vector3f camUp = new Vector3f();
        camForward.cross(camRight, camUp).normalize();

        // offset camera from player: right + up + slight forward
        Vector3f fpCamPos = new Vector3f(playerPos)
            .add(new Vector3f(camRight).mul(fpOffset.x))
            .add(new Vector3f(worldUp).mul(fpOffset.y))
            .add(new Vector3f(camForward).mul(fpOffset.z));

        cam.setLocation(fpCamPos);
        cam.setU(camRight);
        cam.setV(camUp);
        cam.setN(new Vector3f(camForward).mul(-1.0f));
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

    private void snapObjectToTerrain(GameObject obj, float yOffset)
    {
        if (obj == null || terr == null) return;
        Vector3f pos = obj.getWorldLocation();
        float height = terr.getHeight(pos.x, pos.z);
        obj.setLocalLocation(new Vector3f(pos.x, height + yOffset, pos.z));
    }

    private void syncGameObjectToPhysics(GameObject go)
    {
        if (go == null || go.getPhysicsObject() == null) return;

        Vector3f loc = go.getPhysicsObject().getLocation();
        if (go == player)
            loc = new Vector3f(loc.x, loc.y - playerVisualYOffset, loc.z);

        Matrix4f locMat = new Matrix4f();
        locMat.set(3, 0, loc.x);
        locMat.set(3, 1, loc.y);
        locMat.set(3, 2, loc.z);
        go.setLocalTranslation(locMat);

        Quaternionf rot = go.getPhysicsObject().getRotation();
        Matrix4f rotMat = new Matrix4f();
        rot.get(rotMat);
        go.setLocalRotation(rotMat);
    }

    private void addBuildingBoxCollider(GameObject building, float width, float height, float depth)
    {
        if (building == null || physicsEngine == null) return;

        Vector3f loc = building.getWorldLocation();
        Quaternionf rot = new Quaternionf();
        building.getWorldRotation().getNormalizedRotation(rot);

        PhysicsObject p = engine.getSceneGraph().addPhysicsBox(
            0.0f,
            new Vector3f(loc.x, loc.y + height / 2.0f, loc.z),
            rot,
            new float[] { width, height, depth }
        );

        p.setFriction(1.0f);
        p.setBounciness(0.0f);
        p.disableSleeping();

        buildingPhysics.add(p);
    }

    private void updateWeaponVisibility()
    {
        WeaponType currentWeapon = weaponInventory.getCurrentWeapon();

        if (knife != null)
        {
            if (currentWeapon == WeaponType.KNIFE)
            {
                knife.setLocalScale(new Matrix4f().scaling(knifeWeaponScale));
                knife.getRenderStates().setModelOrientationCorrection((new Matrix4f())
                    .rotateY((float)java.lang.Math.toRadians(-90.0f))
                    .rotateZ((float)java.lang.Math.toRadians(25.0f)));
            }
            else knife.setLocalScale(new Matrix4f().scaling(hiddenWeaponScale));
        }

        if (pistol != null)
            pistol.setLocalScale(new Matrix4f().scaling(currentWeapon == WeaponType.PISTOL ? weaponScale : hiddenWeaponScale));

        if (plasmaRifle != null)
            plasmaRifle.setLocalScale(new Matrix4f().scaling(currentWeapon == WeaponType.PLASMA_RIFLE ? weaponScale : hiddenWeaponScale));

        if (rifle != null)
            rifle.setLocalScale(new Matrix4f().scaling(currentWeapon == WeaponType.RIFLE ? weaponScale : hiddenWeaponScale));

        if (shotGun != null)
            shotGun.setLocalScale(new Matrix4f().scaling(currentWeapon == WeaponType.SHOTGUN ? weaponScale + 0.8f : hiddenWeaponScale));
    }

    private void updateStaticObjectsToTerrain()
    {
        if (terr == null || mapSelection == 1) return;

        if (centerBuilding != null) snapObjectToTerrain(centerBuilding, 0.0f);


        for (int i = 0; i < smallBuildings.length; i++)
            if (smallBuildings[i] != null) snapObjectToTerrain(smallBuildings[i], 0.0f);

        for (int i = 0; i < smallBuildings2.length; i++)
            if (smallBuildings2[i] != null) snapObjectToTerrain(smallBuildings2[i], 0.0f);
    }

    private void updatePlayerVisibilityForCameraMode()
    {
        if (player == null) return;

        player.setLocalScale(new Matrix4f().scaling(playerScale));
    }

    private void spawnApe(Vector3f dropPos)
    {
        float terrainY = terr.getHeight(dropPos.x, dropPos.z);

        Vector3f spawnPos = new Vector3f(
            dropPos.x,
            terrainY + 7.0f,   // 6–8 above terrain (you requested this)
            dropPos.z
        );

        GameObject newApe = new GameObject(GameObject.root(), apeS, apeTx);

        newApe.setLocalScale(new Matrix4f().scaling(0.01f));
        newApe.getRenderStates().setModelOrientationCorrection(
            new Matrix4f()
                .rotationX((float)Math.toRadians(90.0f))
                .rotateZ((float)Math.toRadians(180.0f))
        );

        if (apeS != null)
            apeS.playAnimation("RUN", 0.3f, AnimatedShape.EndType.LOOP, 0);        

        newApe.setLocalTranslation(new Matrix4f().translation(
            spawnPos.x, spawnPos.y, spawnPos.z));

        GameObject newApeGun = new GameObject(GameObject.root(), plasmaRifleS, plasmaRifleTx);
        newApeGun.setLocalTranslation(new Matrix4f().translation(-0.05f, 1.5f, 0.7f));
        newApeGun.setLocalRotation(new Matrix4f().rotationY((float)java.lang.Math.toRadians(0.0f)));
        newApeGun.setLocalScale(new Matrix4f().scaling(0.5f));

        newApeGun.setParent(newApe);
        newApeGun.propagateTranslation(true);
        newApeGun.propagateRotation(true);
        newApeGun.propagateScale(true);
        newApeGun.applyParentRotationToPosition(true);

        // physics capsule for ape
        Quaternionf rot = new Quaternionf();
        PhysicsObject apeP = engine.getSceneGraph().addPhysicsCapsule(
            1.0f,
            spawnPos,
            rot,
            1,
            0.5f,
            1.2f
        );

        apeP.setFriction(0.8f);
        apeP.setDamping(0.2f, 0.9f);
        apeP.setBounciness(0.0f);
        apeP.disableSleeping();
        apeP.setAngularFactor(0f);

        newApe.setPhysicsObject(apeP);

        activeApes.add(newApe);
        activeApeNetworkIds.add(nextNetworkEnemyId++);
        activeApeGuns.add(newApeGun);
        activeApePhysics.add(apeP);
        activeApeHealth.add(100); 
        activeApeDead.add(false);
        activeApeDeathTimers.add(0.0f);
        activeApeThinkTimers.add(0.0f);
        activeApeFireCooldowns.add((float)(Math.random() * 1.5f));
        activeApeStrafeDirs.add(Math.random() < 0.5 ? -1.0f : 1.0f);
        activeApeTrees.add(createApeBehaviorTree(newApe, apeP));
    }

    private void removeApe(int index)
    {
        int networkID = activeApeNetworkIds.get(index);

        if (protClient != null && isHostClient)
            protClient.sendEnemyRemove(networkID, "APE");

        GameObject ape = activeApes.get(index);
        PhysicsObject apeP = activeApePhysics.get(index);
        GameObject apeGun = activeApeGuns.get(index);

        if (ape != null)
            ape.setLocalScale(new Matrix4f().scaling(0.0001f));

        if (apeP != null)
            physicsEngine.removeObject(apeP.getUID());

        if (apeGun != null)
            apeGun.setLocalScale(new Matrix4f().scaling(0.0001f));

        activeApes.remove(index);
        activeApeNetworkIds.remove(index);
        activeApeGuns.remove(index);
        activeApePhysics.remove(index);
        activeApeHealth.remove(index);
        activeApeDead.remove(index);
        activeApeDeathTimers.remove(index);
        activeApeThinkTimers.remove(index);
        activeApeFireCooldowns.remove(index);
        activeApeStrafeDirs.remove(index);
        activeApeTrees.remove(index);
    }

    private void updateDeadApes(float dt)
    {
        for (int i = activeApes.size() - 1; i >= 0; i--)
        {
            if (!activeApeDead.get(i)) continue;

            float time = activeApeDeathTimers.get(i) - dt;
            activeApeDeathTimers.set(i, time);

            if (time <= 0.0f)
                removeApe(i);
        }
    }

    private void updateApeBehaviorTrees(float dt)
    {
        apeThinkTimer += dt;

        if (apeThinkTimer < apeThinkInterval)
            return;

        float thinkDt = apeThinkTimer;
        apeThinkTimer = 0.0f;

        for (int i = 0; i < activeApeTrees.size(); i++)
        {
            if (!activeApeDead.get(i))
                activeApeTrees.get(i).update(thinkDt);
        }
    }

    private BehaviorTree createApeBehaviorTree(GameObject apeObj, PhysicsObject apePhys)
    {
        BehaviorTree bt = new BehaviorTree(BTCompositeType.SEQUENCE);

        bt.insertAtRoot(new PlayerAliveCondition());
        bt.insertAtRoot(new ApeFacePlayerAction(apeObj, apePhys));
        bt.insertAtRoot(new ApeMoveByDistanceAction(apeObj, apePhys));
        bt.insertAtRoot(new ApeRandomStrafeAction(apeObj));
        bt.insertAtRoot(new ApeFireAction(apeObj, apePhys));

        return bt;
    }

    private int getApeIndex(GameObject apeObj)
    {
        return activeApes.indexOf(apeObj);
    }

    private Vector3f getClosestPlayerOrGhostTarget(Vector3f from)
    {
        if (from == null)
            return null;

        Vector3f bestTarget = null;
        float bestDistSq = Float.MAX_VALUE;

        if (player != null && pHealth > 0 && !playerDeathScreenActive)
        {
            Vector3f playerPos = player.getWorldLocation();

            float dx = playerPos.x - from.x;
            float dy = playerPos.y - from.y;
            float dz = playerPos.z - from.z;

            bestDistSq = dx * dx + dy * dy + dz * dz;
            bestTarget = new Vector3f(playerPos);
        }

        if (gm != null)
        {
            Vector3f ghostPos = gm.getClosestGhostPosition(from);

            if (ghostPos != null)
            {
                float dx = ghostPos.x - from.x;
                float dy = ghostPos.y - from.y;
                float dz = ghostPos.z - from.z;

                float ghostDistSq = dx * dx + dy * dy + dz * dz;

                if (bestTarget == null || ghostDistSq < bestDistSq)
                {
                    bestDistSq = ghostDistSq;
                    bestTarget = new Vector3f(ghostPos);
                }
            }
        }

        return bestTarget;
    }

    private boolean hasAnyLivingPlayerOrGhostTarget()
    {
        if (player != null && pHealth > 0 && !playerDeathScreenActive)
            return true;

        if (gm != null)
            return gm.getClosestGhostPosition(new Vector3f(0.0f, 0.0f, 0.0f)) != null;

        return false;
    }

    private class PlayerAliveCondition extends BTCondition
    {
        public PlayerAliveCondition()
        {
            super(false);
        }

        protected boolean check()
        {
            return hasAnyLivingPlayerOrGhostTarget();
        }
    }

    private class ApeFacePlayerAction extends BTAction
    {
        private GameObject apeObj;
        private PhysicsObject apePhys;

        public ApeFacePlayerAction(GameObject apeObj, PhysicsObject apePhys)
        {
            this.apeObj = apeObj;
            this.apePhys = apePhys;
        }

        protected BTStatus update(float elapsedTime)
        {
            if (apeObj == null || apePhys == null)
                return BTStatus.BH_FAILURE;

            Vector3f apePos = apePhys.getLocation();
            Vector3f targetPos = getClosestPlayerOrGhostTarget(apePos);

            if (targetPos == null)
                return BTStatus.BH_FAILURE;

            Vector3f toTarget = new Vector3f(targetPos).sub(apePos);

            if (toTarget.length() < 0.001f)
                return BTStatus.BH_FAILURE;

            toTarget.normalize();

            float yaw = (float)java.lang.Math.atan2(toTarget.x, toTarget.z);
            apeObj.setLocalRotation(new Matrix4f().rotationY(yaw));

            return BTStatus.BH_SUCCESS;
        }
    }

    private class ApeMoveByDistanceAction extends BTAction
    {
        private GameObject apeObj;
        private PhysicsObject apePhys;

        public ApeMoveByDistanceAction(GameObject apeObj, PhysicsObject apePhys)
        {
            this.apeObj = apeObj;
            this.apePhys = apePhys;
        }

        protected BTStatus update(float elapsedTime)
        {
            int i = getApeIndex(apeObj);
            if (i < 0 || apePhys == null)
                return BTStatus.BH_FAILURE;

            Vector3f apePos = apePhys.getLocation();
            Vector3f targetPos = getClosestPlayerOrGhostTarget(apePos);

            if (targetPos == null)
                return BTStatus.BH_FAILURE;

            Vector3f toPlayer = new Vector3f(targetPos).sub(apePos);
            float dist = toPlayer.length();

            if (dist < 0.001f)
                return BTStatus.BH_FAILURE;

            toPlayer.normalize();

            float preferredMin = 8.0f;
            float preferredMax = 18.0f;
            float moveSpeed = 3.0f;
            float strafeDir = activeApeStrafeDirs.get(i);

            Vector3f moveDir = new Vector3f();

            if (dist > preferredMax)
            {
                moveDir.set(toPlayer.x, 0.0f, toPlayer.z);
            }
            else if (dist < preferredMin)
            {
                moveDir.set(-toPlayer.x, 0.0f, -toPlayer.z);
            }
            else
            {
                moveDir.set(-toPlayer.z * strafeDir, 0.0f, toPlayer.x * strafeDir);
            }

            if (moveDir.lengthSquared() > 0.0001f)
            {
                moveDir.normalize();

                float[] vel = apePhys.getLinearVelocity();

                apePhys.setLinearVelocity(new float[]
                {
                    moveDir.x * moveSpeed,
                    vel[1],
                    moveDir.z * moveSpeed
                });
            }

            return BTStatus.BH_SUCCESS;
        }
    }

    private class ApeRandomStrafeAction extends BTAction
    {
        private GameObject apeObj;

        public ApeRandomStrafeAction(GameObject apeObj)
        {
            this.apeObj = apeObj;
        }

        protected BTStatus update(float elapsedTime)
        {
            int i = getApeIndex(apeObj);
            if (i < 0)
                return BTStatus.BH_FAILURE;

            float think = activeApeThinkTimers.get(i) + elapsedTime;

            if (think >= 1.0f)
            {
                think = 0.0f;

                if (Math.random() < 0.25)
                    activeApeStrafeDirs.set(i, -activeApeStrafeDirs.get(i));
            }

            activeApeThinkTimers.set(i, think);

            return BTStatus.BH_SUCCESS;
        }
    }

    private class ApeFireAction extends BTAction
    {
        private GameObject apeObj;
        private PhysicsObject apePhys;

        public ApeFireAction(GameObject apeObj, PhysicsObject apePhys)
        {
            this.apeObj = apeObj;
            this.apePhys = apePhys;
        }

        protected BTStatus update(float elapsedTime)
        {
            int i = getApeIndex(apeObj);
        if (i < 0 || apePhys == null)
            return BTStatus.BH_FAILURE;

        Vector3f apePos = apePhys.getLocation();
        Vector3f targetPos = getClosestPlayerOrGhostTarget(apePos);

        if (targetPos == null)
            return BTStatus.BH_FAILURE;

        float dist = new Vector3f(targetPos).sub(apePos).length();

            float fireCd = activeApeFireCooldowns.get(i) - elapsedTime;

            if (fireCd <= 0.0f && dist <= 25.0f)
            {
                Vector3f fireDir = new Vector3f(targetPos)
                    .add(0.0f, 1.0f, 0.0f)
                    .sub(apePos.x, apePos.y + 1.5f, apePos.z)
                    .normalize();

                Vector3f muzzlePos = new Vector3f(apePos.x, apePos.y + 1.5f, apePos.z)
                    .add(new Vector3f(fireDir).mul(1.2f));

                spawnEnemyBullet(muzzlePos, fireDir, true);

                if (protClient != null && isHostClient)
                protClient.sendEnemyBullet(muzzlePos, fireDir, true);

                gameAudio.playNpcPlasma(muzzlePos);

                fireCd = 1.25f;
            }

            activeApeFireCooldowns.set(i, fireCd);

            return BTStatus.BH_SUCCESS;
        }
    }

    private void spawnSkinny(Vector3f pos)
    {
        float terrainY = terr.getHeight(pos.x, pos.z);

        Vector3f spawnPos = new Vector3f(pos.x, terrainY + 2.0f, pos.z);

        GameObject s = new GameObject(GameObject.root(), skinnyS, skinnyTx);
        s.setLocalScale(new Matrix4f().scaling(0.8f));
        s.setLocalTranslation(new Matrix4f().translation(spawnPos));
        skinnyS.playAnimation("GRAPPLE", 0.3f, AnimatedShape.EndType.LOOP, 0);

        // === RIGHT HAND PLASMA ===
        GameObject plasma = new GameObject(GameObject.root(), plasmaRifleS, plasmaRifleTx);
        plasma.setParent(s);
        plasma.propagateTranslation(true);
        plasma.propagateRotation(true);
        plasma.propagateScale(true);
        plasma.applyParentRotationToPosition(true);

        plasma.setLocalTranslation(
            new Matrix4f().translation(-0.3f, 1.25f, 0.75f)
        );
        plasma.setLocalScale(new Matrix4f().scaling(weaponScale / 75.0f));
        plasma.setLocalRotation(
            new Matrix4f().rotationY(0.0f)
        );

        // === LEFT HAND GRAPPLE ===
        GameObject grapple = new GameObject(GameObject.root(), grappleGunS, grappleGunTx);
        grapple.setParent(s);
        grapple.propagateTranslation(true);
        grapple.propagateRotation(true);
        grapple.propagateScale(true);
        grapple.applyParentRotationToPosition(true);

        grapple.setLocalTranslation(
            new Matrix4f().translation(0.25f, 1.45f, 0.25f)
        );
        grapple.setLocalScale(new Matrix4f().scaling(0.03f));
        grapple.setLocalRotation(
            new Matrix4f().rotationX((float)Math.toRadians(45.0f))
        );

        /*
        // Alien grapple lines were too visually messy, so this is disabled.
        // The player grapple line still uses skinnyGrappleLineS separately.
        GameObject grappleLine = new GameObject(GameObject.root(), skinnyGrappleLineS);
        grappleLine.getRenderStates().setColor(new Vector3f(0.6f, 0.6f, 0.6f));
        grappleLine.setParent(s);
        grappleLine.propagateTranslation(true);
        grappleLine.propagateRotation(true);
        grappleLine.propagateScale(true);
        grappleLine.applyParentRotationToPosition(true);

        grappleLine.setLocalTranslation(
            new Matrix4f().translation(0.25f, 1.55f, 0.25f)
        );
        grappleLine.setLocalScale(new Matrix4f().scaling(0.0001f));
        grappleLine.setLocalRotation(
            new Matrix4f().rotationX((float)java.lang.Math.toRadians(45.0f))
        );

        activeSkinnyGrappleLines.add(grappleLine);
        */
        // store references
        activeSkinnyPlasma.add(plasma);
        activeSkinnyGrapple.add(grapple);

        Quaternionf rot = new Quaternionf();

        PhysicsObject p = engine.getSceneGraph().addPhysicsCapsule(
            1.0f,
            spawnPos,
            rot,
            1,
            0.4f,
            1.0f
        );

        p.setFriction(0.6f);
        p.setDamping(0.1f, 0.8f);
        p.setBounciness(0.0f);
        p.disableSleeping();

        s.setPhysicsObject(p);

        activeSkinnys.add(s);
        activeSkinnyNetworkIds.add(nextNetworkEnemyId++);
        activeSkinnyPhysics.add(p);
        activeSkinnyHealth.add(100);
        activeSkinnyDead.add(false);
        activeSkinnyBounceTimers.add((float)Math.random());
        activeSkinnyFireCooldowns.add((float)(Math.random() * 1.5f));
    }

    private void spawnSkinnyWave()
    {
        Vector3f[] points = {
            new Vector3f(-45.29f,0,-39.20f),
            new Vector3f(31.86f,0,-67.46f),
            new Vector3f(59.16f,0,35.07f),
            new Vector3f(2.05f,0,65.86f),
            new Vector3f(-62.13f,0,13.12f),
            new Vector3f(-60.20f,0,-64.62f),
            new Vector3f(-83.75f,0,28.96f)
        };

        for (Vector3f base : points)
        {
            for (int i = 0; i < 10; i++)
            {
                Vector3f offset = new Vector3f(
                    (float)(Math.random()*8 - 4),
                    0,
                    (float)(Math.random()*8 - 4)
                );

                spawnSkinny(new Vector3f(base).add(offset));
            }
        }

        System.out.println("Spawned " + (points.length * 10) + " skinnys");
    }

    private void updateSkinnys(float dt)
    {
        if (player == null) return;

        for (int i = 0; i < activeSkinnys.size(); i++)
        {
            if (activeSkinnyDead.get(i)) continue;

            PhysicsObject p = activeSkinnyPhysics.get(i);
            Vector3f pos = p.getLocation();

            Vector3f targetPos = getClosestPlayerOrGhostTarget(pos);

            if (targetPos == null)
                continue;

            float timer = activeSkinnyBounceTimers.get(i) - dt;
            
        if (timer <= 0.0f)
        {
            Vector3f dir = new Vector3f(targetPos).sub(pos);
            if (dir.length() > 0.01f) dir.normalize();

            p.setLinearVelocity(new float[]{
                dir.x * 4.0f,
                8.0f,
                dir.z * 4.0f
            });


            timer = 1.5f + (float)Math.random();
        }

        /*
        // Disabled alien grapple lines; they clutter the screen when many aliens jump.
        float[] vel = p.getLinearVelocity();

        if (i < activeSkinnyGrappleLines.size())
        {
            GameObject line = activeSkinnyGrappleLines.get(i);

            if (line != null)
            {
                if (vel[1] > 0.0f)
                {
                    float height = java.lang.Math.max(0.5f, p.getLocation().y() - 2.0f);

                    line.setLocalScale(
                        new Matrix4f().scaling(0.05f, height * 1.2f, 0.05f)
                    );

                    line.setLocalTranslation(
                        new Matrix4f().translation(0.25f, 1.55f + height * 0.6f, 0.25f)
                    );

                    line.setLocalRotation(
                        new Matrix4f().rotationX((float)java.lang.Math.toRadians(45.0f))
                    );
                }
                else
                {
                    line.setLocalScale(new Matrix4f().scaling(0.0001f));
                }
            }
        }
        */
        // === SKINNY SHOOTING ===
        float dist = new Vector3f(targetPos).sub(pos).length();

        float fireCd = activeSkinnyFireCooldowns.get(i) - dt;

        if (fireCd <= 0.0f && dist <= 20.0f)
        {
            GameObject skinnyGun = null;

            if (i < activeSkinnyPlasma.size())
                skinnyGun = activeSkinnyPlasma.get(i);

            Vector3f muzzleBase;

            if (skinnyGun != null)
                muzzleBase = skinnyGun.getWorldLocation();
            else
                muzzleBase = new Vector3f(pos.x, pos.y + 1.5f, pos.z);

            Vector3f fireDir = new Vector3f(targetPos)
                .add(0.0f, 1.0f, 0.0f)
                .sub(muzzleBase)
                .normalize();

            Vector3f muzzlePos = new Vector3f(muzzleBase)
                .add(new Vector3f(fireDir).mul(0.75f));
            spawnEnemyBullet(muzzlePos, fireDir, true);

            if (protClient != null && isHostClient)
                protClient.sendEnemyBullet(muzzlePos, fireDir, true);

            gameAudio.playNpcPlasma(muzzlePos);

            fireCd = 1.5f;
        }

        activeSkinnyFireCooldowns.set(i, fireCd);
        activeSkinnyBounceTimers.set(i, timer);
        }
    }

    @Override
    public void loadSkyBoxes()
    {
        spaceSkyBox = (engine.getSceneGraph()).loadCubeMap("blueSpace");
        islandSkyBox = (engine.getSceneGraph()).loadCubeMap("island");
        lushSkyBox = (engine.getSceneGraph()).loadCubeMap("lush");
        plainsSkyBox = (engine.getSceneGraph()).loadCubeMap("plains");
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
        shopState.loadShape();
        brainS = new AnimatedShape("brain.rkm", "brain.rks");
        brainS.loadAnimation("FLOAT", "brainFloat.rka");
        skinnyS = new AnimatedShape("skinny.rkm", "skinny.rks");
        skinnyS.loadAnimation("GRAPPLE", "skinnyGrapple.rka");
        grappleGunS = new ImportedModel("grapple.obj");
        skinnyGrappleLineS = new Cube();
        switch (mapSelection)
        {
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

                apeS = new AnimatedShape("ape.rkm", "ape.rks");
                apeS.loadAnimation("RUN", "apeRun.rka");
                apeS.loadAnimation("DIE", "apeDie.rka");

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

                bulletSphereS = new Sphere();

                ufoS = new ImportedModel("ufo.obj");
                
                break;

            case 1:
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

                ammoS = new ImportedModel("ammo.obj");
                healthS = new ImportedModel("health.obj");
                plasmaRifleS = new ImportedModel("plasmaRifle.obj");
                rifleS = new ImportedModel("rifle.obj");
                shotGunS = new ImportedModel("shotGun.obj");
                knifeS = new ImportedModel("knife.obj");
                pistolS = new ImportedModel("pistol.obj");

                bulletSphereS = new Sphere();
                break;
        }
    }

    private void setupCaseOneStart()
    {
        float playerY = terr.getHeight(0.0f, 0.0f) + playerVisualYOffset + 2.0f;

        playerP.setLocation(new float[] { 0.0f, playerY, 10.0f });
        playerP.setLinearVelocity(new float[] { 0.0f, 0.0f, 0.0f });

        player.setLocalTranslation(
            new Matrix4f().translation(0.0f, playerY - playerVisualYOffset, 0.0f)
        );

        float brainZ = -8.0f;
        float brainY = terr.getHeight(0.0f, brainZ) + 1.0f;

        if (brain != null)
        {
            brain.setLocalTranslation(new Matrix4f().translation(1.5f, brainY, brainZ));
            brain.setLocalScale(new Matrix4f().scaling(0.1f));
            brainActive = false;
            brainFloating = false;
            brainDefeated = false;
            brainHealth = brainMaxHealth;
            brainActionTimer = 3.0f + (float)(Math.random() * 2.0f);
            brainCircleAngle = 0.0f;
            brainCircleDir = 1.0f;
            brainRushing = false;
        }

        playerKnockedOffLevelTwo = false;
        playerHasGrapple = false;

        gameAudio.stopMusic();

        levelTwoMusicPending = true;
        levelTwoMusicTimer = levelTwoMusicDelay;
        bossMusicStarted = false;

        scheduleLevelTwoArrivalEvent();
    }

    private void enterVendingShopView()
    {
        firstPersonMode = false;
        updatePlayerVisibilityForCameraMode();

        currentMoveDir.set(0, 0, 0);
        stopPlayerHorizontalMotion();
        endFireInput();
    }

    private void exitVendingShopView()
    {
        shopState.closeShop();

        currentMoveDir.set(0, 0, 0);
        stopPlayerHorizontalMotion();
        endFireInput();

        firstPersonMode = true;
        updatePlayerVisibilityForCameraMode();

        recenterMouse();
    }

    private void updateBrainAnimation()
    {
        if (brain == null || brainS == null || mapSelection != 1)
            return;

        Vector3f targetPos = getClosestPlayerOrGhostTarget(brain.getWorldLocation());

        if (targetPos == null)
            return;

        float dist = targetPos.distance(brain.getWorldLocation());

        if (!brainFloating && dist <= 8.0f)
        {
            brainS.playAnimation("FLOAT", 0.3f, AnimatedShape.EndType.LOOP, 0);
            brainFloating = true;

            brainActive = true;
            startBossMusic();
            brainActionTimer = 3.0f + (float)(Math.random() * 2.0f);
            brainCircleAngle = 0.0f;
            brainCircleDir = 1.0f;
            brainRushing = false;
        }
    }

    private void startBossMusic()
    {
        if (bossMusicStarted)
            return;

        bossMusicStarted = true;
        levelTwoMusicPending = false;

        gameAudio.playBossMusic();
    }

    private void updateBrainBoss(float dt)
    {
        if (!brainActive || brain == null || player == null || mapSelection != 1)
            return;

        if (brainRushing)
        {
            updateBrainRush(dt);
            return;
        }

        updateBrainCircle(dt);

        if (brainShotsLeft > 0)
        {
            updateBrainShooting(dt);
            return;
        }

        brainActionTimer -= dt;

        if (brainActionTimer <= 0.0f)
        {
            startRandomBrainAction();
            brainActionTimer = 3.0f + (float)(Math.random() * 2.0f);
        }
    }

    private void updateBrainCircle(float dt)
    {
        Vector3f playerPos = getClosestPlayerOrGhostTarget(brain.getWorldLocation());

        if (playerPos == null)
            return;

        brainCircleAngle += brainCircleDir * dt * 0.8f;

        if (Math.random() < 0.005f)
            brainCircleDir *= -1.0f;

        float radius = 12.0f;
        float height = 8.0f;

        float x = playerPos.x + (float)java.lang.Math.cos(brainCircleAngle) * radius;
        float z = playerPos.z + (float)java.lang.Math.sin(brainCircleAngle) * radius;
        float y = playerPos.y + height + (float)java.lang.Math.sin(elapsTime * 2.0f) * 1.5f;

        brain.setLocalTranslation(new Matrix4f().translation(x, y, z));

        Vector3f toPlayer = new Vector3f(playerPos).sub(brain.getWorldLocation());

        if (toPlayer.lengthSquared() > 0.001f)
        {
            float yaw = (float)java.lang.Math.atan2(toPlayer.x, toPlayer.z);
            brain.setLocalRotation(new Matrix4f().rotationY(yaw));
        }
    }

    private void startRandomBrainAction()
    {
        brainAttackType = (int)(Math.random() * 4.0f);

        switch (brainAttackType)
        {
            case 0:
                // sine arc stream
                brainShotsLeft = 30;
                brainShotTimer = 0.0f;
                break;

            case 1:
                // 5x5 square burst
                brainSquareBurst();
                break;

            case 2:
                // fast random spread
                brainShotsLeft = 45;
                brainShotTimer = 0.0f;
                break;

            case 3:
                // rush attack
                Vector3f targetPos = getClosestPlayerOrGhostTarget(brain.getWorldLocation());

                if (targetPos != null)
                {
                    brainRushTarget.set(targetPos);
                    brainRushing = true;
                }
                break;
        }
    }

    private void updateBrainShooting(float dt)
    {
        brainShotTimer -= dt;

        float delay = brainAttackType == 2 ? 0.035f : 0.055f;

        if (brainShotTimer > 0.0f)
            return;

        if (brainAttackType == 0)
            brainSineShot();
        else if (brainAttackType == 2)
            brainRandomShot();

        brainShotsLeft--;
        brainShotTimer = delay;
    }

    private void brainSineShot()
    {
        Vector3f brainPos = brain.getWorldLocation();
        Vector3f targetPos = getClosestPlayerOrGhostTarget(brainPos);

        if (targetPos == null)
            return;

        Vector3f target = new Vector3f(targetPos).add(0.0f, 1.0f, 0.0f);

        Vector3f dir = new Vector3f(target).sub(brainPos).normalize();

        Vector3f side = new Vector3f(-dir.z, 0.0f, dir.x);
        if (side.lengthSquared() > 0.001f)
            side.normalize();

        float wave = (float)java.lang.Math.sin((30 - brainShotsLeft) * 0.55f) * 0.45f;

        dir.add(new Vector3f(side).mul(wave)).normalize();

        Vector3f muzzle = new Vector3f(brainPos).add(new Vector3f(dir).mul(1.5f));

        spawnEnemyBullet(muzzle, dir, true);
        if (protClient != null && isHostClient)
            protClient.sendEnemyBullet(muzzle, dir, true);
        gameAudio.playNpcPlasma(muzzle);
    }

    private void brainRandomShot()
    {
        Vector3f brainPos = brain.getWorldLocation();
        Vector3f targetPos = getClosestPlayerOrGhostTarget(brainPos);

        if (targetPos == null)
            return;

        Vector3f target = new Vector3f(targetPos).add(0.0f, 1.0f, 0.0f);

        Vector3f dir = new Vector3f(target).sub(brainPos).normalize();

        dir.add(
            ((float)Math.random() - 0.5f) * 0.7f,
            ((float)Math.random() - 0.5f) * 0.35f,
            ((float)Math.random() - 0.5f) * 0.7f
        ).normalize();

        Vector3f muzzle = new Vector3f(brainPos).add(new Vector3f(dir).mul(1.5f));

        spawnEnemyBullet(muzzle, dir, true);
        if (protClient != null && isHostClient)
            protClient.sendEnemyBullet(muzzle, dir, true);
        gameAudio.playNpcPlasma(muzzle);
    }

    private void brainSquareBurst()
    {
        Vector3f brainPos = brain.getWorldLocation();
        Vector3f targetPos = getClosestPlayerOrGhostTarget(brainPos);

        if (targetPos == null)
            return;

        Vector3f target = new Vector3f(targetPos).add(0.0f, 1.0f, 0.0f);

        Vector3f forward = new Vector3f(target).sub(brainPos).normalize();
        Vector3f right = new Vector3f(-forward.z, 0.0f, forward.x);

        if (right.lengthSquared() < 0.001f)
            right.set(1.0f, 0.0f, 0.0f);
        else
            right.normalize();

        Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);

        for (int x = -2; x <= 2; x++)
        {
            for (int y = -2; y <= 2; y++)
            {
                Vector3f dir = new Vector3f(forward)
                    .add(new Vector3f(right).mul(x * 0.15f))
                    .add(new Vector3f(up).mul(y * 0.15f))
                    .normalize();

                Vector3f muzzle = new Vector3f(brainPos).add(new Vector3f(dir).mul(1.5f));
                spawnEnemyBullet(muzzle, dir, true);

                if (protClient != null && isHostClient)
                    protClient.sendEnemyBullet(muzzle, dir, true);
            }
        }

        gameAudio.playNpcPlasma(brainPos);
    }

    private void updateBrainRush(float dt)
    {
        Vector3f brainPos = brain.getWorldLocation();
        Vector3f toTarget = new Vector3f(brainRushTarget).sub(brainPos);

        if (toTarget.length() <= 1.0f)
        {
            brainRushing = false;
            return;
        }

        toTarget.normalize();

        float rushSpeed = 22.0f;
        Vector3f newPos = new Vector3f(brainPos).add(toTarget.mul(rushSpeed * dt));

        brain.setLocalTranslation(new Matrix4f().translation(newPos));

        float yaw = (float)java.lang.Math.atan2(toTarget.x, toTarget.z);
        brain.setLocalRotation(new Matrix4f().rotationY(yaw));
    }

    private String getBrainHealthBar()
    {
        int bars = java.lang.Math.max(0, brainHealth / 250);

        StringBuilder sb = new StringBuilder();
        sb.append("Brain[");

        for (int i = 0; i < bars; i++)
            sb.append("=");

        for (int i = bars; i < 20; i++)
            sb.append(" ");

        sb.append("]");

        return sb.toString();
    }

    @Override
    public void loadTextures()
    {
        robotTextures[0] = new TextureImage("robot.jpg");
        robotTextures[1] = new TextureImage("robot1.jpg");
        robotTextures[2] = new TextureImage("robot2.jpg");
        robotTextures[3] = new TextureImage("robot3.jpg");

        playerTx = robotTextures[avatarSelection];
        ghostT = playerTx;

        skinnyTx = new TextureImage("skinny.jpg");
        apeTx = new TextureImage("ape.jpg");
        brainTx = new TextureImage("brain.jpg");

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

        bulletYellowTx = new TextureImage("bullet.jpg");
        bulletBlueTx = new TextureImage("plasmaBullet.jpg");

        ufoTx = new TextureImage("ufo.png");

        grappleGunTx = new TextureImage("grapple.png");
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

        pickupManager.buildObjects(ammoS, ammoTx, healthS, healthTx);

        knife = new GameObject(GameObject.root(), knifeS, knifeTx);
        knife.setLocalTranslation(new Matrix4f().translation(weaponPos.x, weaponPos.y, weaponPos.z));
        knife.setLocalRotation(new Matrix4f().rotationY((float)java.lang.Math.toRadians(weaponRotY)));
        knife.setLocalScale(new Matrix4f().scaling(weaponScale));
        attachWeaponToPlayer(knife);

        pistol = new GameObject(GameObject.root(), pistolS, pistolTx);
        pistol.setLocalTranslation(new Matrix4f().translation(weaponPos.x, weaponPos.y - 0.1f, weaponPos.z - 0.1f));
        pistol.setLocalRotation(new Matrix4f().rotationY((float)java.lang.Math.toRadians(weaponRotY)));
        pistol.setLocalScale(new Matrix4f().scaling(weaponScale + 0.01f));
        attachWeaponToPlayer(pistol);

        plasmaRifle = new GameObject(GameObject.root(), plasmaRifleS, plasmaRifleTx);
        plasmaRifle.setLocalTranslation(new Matrix4f().translation(weaponPos.x, weaponPos.y, weaponPos.z));
        plasmaRifle.setLocalRotation(new Matrix4f().rotationY((float)java.lang.Math.toRadians(weaponRotY)));
        plasmaRifle.setLocalScale(new Matrix4f().scaling(weaponScale));
        attachWeaponToPlayer(plasmaRifle);

        rifle = new GameObject(GameObject.root(), rifleS, rifleTx);
        rifle.setLocalTranslation(new Matrix4f().translation(weaponPos.x, weaponPos.y, weaponPos.z));
        rifle.setLocalRotation(new Matrix4f().rotationY((float)java.lang.Math.toRadians(weaponRotY)));
        rifle.setLocalScale(new Matrix4f().scaling(weaponScale));
        rifle.getRenderStates().setModelOrientationCorrection((new Matrix4f()).rotateX((float)java.lang.Math.toRadians(90.0f)));
        attachWeaponToPlayer(rifle);

        shotGun = new GameObject(GameObject.root(), shotGunS, shotGunTx);
        shotGun.setLocalTranslation(new Matrix4f().translation(weaponPos.x, weaponPos.y - 0.25f, weaponPos.z - 0.2f));
        shotGun.setLocalRotation(new Matrix4f().rotationY((float)java.lang.Math.toRadians(weaponRotY)));
        shotGun.setLocalScale(new Matrix4f().scaling(weaponScale));
        shotGun.getRenderStates().setModelOrientationCorrection((new Matrix4f())
            .rotateY((float)java.lang.Math.toRadians(90.0f))
            .rotateX((float)java.lang.Math.toRadians(90.0f)));
        attachWeaponToPlayer(shotGun);

        grapplePickup = new GameObject(GameObject.root(), grappleGunS, grappleGunTx);
        grapplePickup.setLocalScale(new Matrix4f().scaling(0.0001f));
        grapplePickup.setLocalTranslation(new Matrix4f().translation(0.0f, -10000.0f, 0.0f));

        playerGrappleLine = new GameObject(GameObject.root(), skinnyGrappleLineS);
        playerGrappleLine.getRenderStates().setColor(new Vector3f(0.6f, 0.6f, 0.6f));
        playerGrappleLine.setLocalScale(new Matrix4f().scaling(0.0001f));
        playerGrappleLine.setLocalTranslation(new Matrix4f().translation(0.0f, -10000.0f, 0.0f));

        weaponInventory.reset();
        isFiring = false;
        updateWeaponVisibility();

        centerBuilding = new GameObject(GameObject.root(), centerBuildingS, centerBuildingTx);
        centerBuilding.setLocalTranslation(new Matrix4f().translation(0.0f, 0.0f, 0.0f));
        centerBuilding.setLocalScale(new Matrix4f().scaling(4.0f));
        centerBuilding.getRenderStates().setModelOrientationCorrection(new Matrix4f().rotationX((float)Math.toRadians(90.0f)));
        float[][] sbPositions = {
            {-52.0f, 72.0f}, {48.0f, -65.0f}, {79.0f, -21.0f}, {-83.0f, 39.0f},
            {-18.0f, 40.0f}, {20.0f, -38.0f}, {24.0f, 5.0f}, {28.0f, 38.0f}
        };

        float[][] sb2Positions = {
            {78.0f, -37.0f}, {-79.0f, 16.0f}, {-50.0f, 51.0f}, {-40.0f, 22.0f},
            {-24.0f, -18.0f}, {-5.0f, 35.0f}, {10.0f, -42.0f}, {44.0f, -10.0f}
        };

        ufoWaveManager.buildObjects(ufoS, ufoTx);

        shopState.buildObjects(centerBuildingTx);

        for (int i = 0; i < smallBuildings.length; i++)
        {
            smallBuildings[i] = new GameObject(GameObject.root(), smallBuildingS, smallBuildingTx);
            smallBuildings[i].setLocalTranslation(new Matrix4f().translation(sbPositions[i][0], 0.0f, sbPositions[i][1]));
            smallBuildings[i].setLocalScale(new Matrix4f().scaling(0.01f));
        }

        for (int i = 0; i < smallBuildings2.length; i++)
        {
            smallBuildings2[i] = new GameObject(GameObject.root(), smallBuilding2S, smallBuilding2Tx);
            smallBuildings2[i].setLocalTranslation(new Matrix4f().translation(sb2Positions[i][0], 0.0f, sb2Positions[i][1]));
            smallBuildings2[i].setLocalScale(new Matrix4f().scaling(4.0f));
        }

        terr = new GameObject(GameObject.root(), terrS, terrTxMap0);
        terr.setLocalTranslation((new Matrix4f()).translation(0f, 0f, 0f));
        terr.setLocalScale((new Matrix4f()).scaling(100.0f, 50.0f, 100.0f));
        terr.getRenderStates().setTiling(1);

        brain = new GameObject(GameObject.root(), brainS, brainTx);
        brain.setLocalScale(new Matrix4f().scaling(0.1f));
        brain.setLocalTranslation(new Matrix4f().translation(0.0f, -10000.0f, 0.0f));
        brain.getRenderStates().setModelOrientationCorrection(new Matrix4f().rotationX((float)java.lang.Math.toRadians(180.0f)));

        bulletManager.buildObjects(bulletSphereS, bulletYellowTx, bulletBlueTx);

        applyMapSelection();
    }

    @Override
    public void initializeLights()
    {
        Light.setGlobalAmbient(0.5f, 0.5f, 0.5f);

        mainLight = new Light();
        mainLight.setLocation(new Vector3f(0.0f, 0.0f, 0.0f));
        engine.getSceneGraph().addLight(mainLight);

        pickupManager.initializeLights(engine);

        tractorBeamLight = new Light();
        tractorBeamLight.setType(Light.LightType.SPOTLIGHT);
        tractorBeamLight.setAmbient(0.0f, 0.2f, 0.3f);
        tractorBeamLight.setDiffuse(0.2f, 0.9f, 1.0f);
        tractorBeamLight.setSpecular(0.2f, 0.9f, 1.0f);
        tractorBeamLight.setDirection(new Vector3f(0.0f, -1.0f, 0.0f));
        tractorBeamLight.setCutoffAngle(25.0f);
        tractorBeamLight.setOffAxisExponent(1.5f);
        tractorBeamLight.setConstantAttenuation(1.0f);
        tractorBeamLight.setLinearAttenuation(0.01f);
        tractorBeamLight.setQuadraticAttenuation(0.001f);
        tractorBeamLight.setLocation(new Vector3f(0.0f, 35.0f, 0.0f));
        tractorBeamLight.disable();

        engine.getSceneGraph().addLight(tractorBeamLight);
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

        mainCamera.setLocation(new Vector3f(0.0f, 8.0f, 12.0f));
        mainCamera.setU(new Vector3f(1, 0, 0));
        mainCamera.setV(new Vector3f(0, 1, 0));
        mainCamera.setN(new Vector3f(0, 0, -1));

        overCamera.setLocation(new Vector3f(0, overheadCameraController.getHeight(), 0));
        overCamera.setU(new Vector3f(1, 0, 0));
        overCamera.setV(new Vector3f(0, 0, -1));
        overCamera.setN(new Vector3f(0, -1, 0));
    }

    private void initAudio()
    {
        gameAudio.initialize(engine);
        gameAudio.playLevel1Music();
    }

    private void setEarParameters()
    {
        gameAudio.setEarParameters(player, cam);
    }

    private boolean isShotgunPumping()
    {
        return gameAudio.isShotgunPumping();
    }

    private void updateWeaponAudio(float dt)
    {
        gameAudio.updateWeaponAudio(dt);
    }

    @Override
    public void initializePhysicsObjects()
    {
        float[] gravity = {0f, worldGravity, 0f};
        physicsEngine = engine.getSceneGraph().getPhysicsEngine();
        physicsEngine.setGravity(gravity);

        Vector3f loc = player.getWorldLocation();
        Quaternionf rot = new Quaternionf();
        player.getWorldRotation().getNormalizedRotation(rot);

        playerP = engine.getSceneGraph().addPhysicsCapsule(
            1.0f,
            loc,
            rot,
            1,
            playerCapsuleRadius,
            playerCapsuleHeight
        );
        playerP.setFriction(0.8f);
        playerP.setDamping(0.2f, 0.9f);
        playerP.setBounciness(0.0f);
        playerP.disableSleeping();
        playerP.setAngularFactor(0f);
        player.setPhysicsObject(playerP);

		loc = terr.getWorldLocation();
		rot = new Quaternionf();
		(terr.getWorldRotation()).getNormalizedRotation(rot);

        terrainP0 = engine.getSceneGraph().addPhysicsStaticTerrainMesh(
            loc, rot, heightMap0, 100.0f, 50.0f, 100
        );

        terrainP1 = engine.getSceneGraph().addPhysicsStaticTerrainMesh(
            new Vector3f(0.0f, -10000.0f, 0.0f), rot, heightMap1, 100.0f, 50.0f, 100
        );

        terrainP0.setBounciness(0.0f);
        terrainP0.setFriction(1.0f);
        terrainP0.disableSleeping();

        terrainP1.setBounciness(0.0f);
        terrainP1.setFriction(1.0f);
        terrainP1.disableSleeping();

        switchTerrainPhysics();

        addBuildingBoxCollider(centerBuilding, 18.0f, 18.0f, 18.0f);

        for (int i = 0; i < smallBuildings.length; i++)
            addBuildingBoxCollider(smallBuildings[i], 8.0f, 10.0f, 8.0f);

        for (int i = 0; i < smallBuildings2.length; i++)
            addBuildingBoxCollider(smallBuildings2[i], 12.0f, 12.0f, 12.0f);

        bulletManager.setPhysicsEngine(physicsEngine);

        engine.enableGraphicsWorldRender();
        if (physicsDebug)
            engine.enablePhysicsWorldRender();
        else
            engine.disablePhysicsWorldRender();
    }

    private boolean isButtonPressed(net.java.games.input.Event e)
    {
        return e != null && e.getValue() > 0.5f;
    }

    private boolean acceptGamepadMenuInput()
    {
        long now = System.currentTimeMillis();
        if (now - lastGamepadMenuInputTime < GAMEPAD_MENU_REPEAT_MS)
            return false;

        lastGamepadMenuInputTime = now;
        return true;
    }

    private void menuMoveUp()
    {
        menu.moveUp();
        menuSelection = menu.getSelectedIndex();
    }

    private void menuMoveDown()
    {
        menu.moveDown();
        menuSelection = menu.getSelectedIndex();
    }

    private void menuPreviousMap()
    {
        if (menu.activateSelection() == MainMenu.MenuAction.SELECT_MAP)
        {
            menu.previousMap();
            setMapSelection(menu.getSelectedMapIndex());
            applyMapSelection();
        }
    }

    private void menuNextMap()
    {
        if (menu.activateSelection() == MainMenu.MenuAction.SELECT_MAP)
        {
            menu.nextMap();
            setMapSelection(menu.getSelectedMapIndex());
            applyMapSelection();
        }
    }

    private void activateCurrentMenuSelection()
    {
        switch (menu.activateSelection())
        {
            case START_GAME:
                gameState = GameState.ROBOT_SELECT;
                applyAvatarSelectionTexture();
                break;

            case SELECT_MAP:
                menu.nextMap();
                setMapSelection(menu.getSelectedMapIndex());
                applyMapSelection();
                break;

            case MULTIPLAYER:
                gameState = GameState.MULTIPLAYER_MENU;
                multiplayerStatusText = "Choose Host Game or Join Game";
                break;

            case OPTIONS:
                break;

            case QUIT:
                isShuttingDown = true;
                mouseModeInitiated = false;
                isRecentering = false;
                gameAudio.releaseAll();
                shutdown();
                System.exit(0);
                break;

            default:
                System.out.println("Menu option not implemented yet: " + menu.getSelectedItem());
                break;
        }
    }

    private void beginHostGame()
    {
        multiplayerStatusText = "Hosting game...";
        serverProtocol = ProtocolType.UDP;
        startHostedServerIfNeeded();
        startDiscoveryResponder();

        if (setupNetworking("127.0.0.1", serverPort, true))
        {
            gameState = GameState.ROBOT_SELECT;
            applyAvatarSelectionTexture();
        }
    }

    private void beginJoinGame()
    {
        multiplayerStatusText = "Searching for hosted game...";

        ServerDiscoveryResult discoveredServer = discoverHostedServer();
        if (discoveredServer == null)
        {
            multiplayerStatusText = "No hosted game found on this network";
            gameState = GameState.MULTIPLAYER_MENU;
            return;
        }

        multiplayerStatusText = "Joining " + discoveredServer.address + ":" + discoveredServer.port;

        if (setupNetworking(discoveredServer.address, discoveredServer.port, false))
        {
            gameState = GameState.ROBOT_SELECT;
            applyAvatarSelectionTexture();
        }
        else
        {
            gameState = GameState.MULTIPLAYER_MENU;
        }
    }

    private void startHostedServerIfNeeded()
    {
        if (hostedServer != null || serverProtocol != ProtocolType.UDP)
            return;

        try
        {
            hostedServer = new GameServerUDP(serverPort);
            multiplayerStatusText = "Hosting on port " + serverPort;
        }
        catch (IOException e)
        {
            multiplayerStatusText = "Using existing server on port " + serverPort;
            System.out.println("Could not start local server, trying to connect instead: " + e.getMessage());
        }
    }

    private int getDiscoveryPort()
    {
        return serverPort + 1;
    }

    private void startDiscoveryResponder()
    {
        if (discoveryRunning)
            return;

        discoveryRunning = true;
        discoveryThread = new Thread(() -> runDiscoveryResponder(), "BulletServerDiscovery");
        discoveryThread.setDaemon(true);
        discoveryThread.start();
    }

    private void runDiscoveryResponder()
    {
        try
        {
            discoverySocket = new DatagramSocket(getDiscoveryPort());
            byte[] buffer = new byte[256];

            while (discoveryRunning)
            {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                discoverySocket.receive(request);

                String message = new String(
                    request.getData(),
                    request.getOffset(),
                    request.getLength(),
                    StandardCharsets.UTF_8
                );

                if (!DISCOVERY_REQUEST.equals(message))
                    continue;

                byte[] responseData = (DISCOVERY_RESPONSE + "," + serverPort).getBytes(StandardCharsets.UTF_8);
                DatagramPacket response = new DatagramPacket(
                    responseData,
                    responseData.length,
                    request.getAddress(),
                    request.getPort()
                );
                discoverySocket.send(response);
            }
        }
        catch (SocketException e)
        {
            if (discoveryRunning)
                System.out.println("Server discovery responder unavailable: " + e.getMessage());
        }
        catch (IOException e)
        {
            if (discoveryRunning)
                System.out.println("Server discovery responder stopped: " + e.getMessage());
        }
        finally
        {
            if (discoverySocket != null)
            {
                discoverySocket.close();
                discoverySocket = null;
            }

            discoveryRunning = false;
        }
    }

    private void stopDiscoveryResponder()
    {
        discoveryRunning = false;

        if (discoverySocket != null)
        {
            discoverySocket.close();
            discoverySocket = null;
        }

        discoveryThread = null;
    }

    private ServerDiscoveryResult discoverHostedServer()
    {
        try (DatagramSocket socket = new DatagramSocket())
        {
            socket.setBroadcast(true);
            socket.setSoTimeout(DISCOVERY_TIMEOUT_MS);

            byte[] requestData = DISCOVERY_REQUEST.getBytes(StandardCharsets.UTF_8);
            sendDiscoveryRequest(socket, requestData, InetAddress.getByName("255.255.255.255"));
            sendDiscoveryRequestsToNetworkBroadcasts(socket, requestData);

            byte[] buffer = new byte[256];
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            socket.receive(response);

            String message = new String(
                response.getData(),
                response.getOffset(),
                response.getLength(),
                StandardCharsets.UTF_8
            );

            String[] parts = message.split(",");
            if (parts.length < 2 || !DISCOVERY_RESPONSE.equals(parts[0]))
                return null;

            int discoveredPort = Integer.parseInt(parts[1]);
            return new ServerDiscoveryResult(response.getAddress().getHostAddress(), discoveredPort);
        }
        catch (SocketTimeoutException e)
        {
            return null;
        }
        catch (IOException | NumberFormatException e)
        {
            System.out.println("Server discovery failed: " + e.getMessage());
            return null;
        }
    }

    private void sendDiscoveryRequestsToNetworkBroadcasts(DatagramSocket socket, byte[] requestData)
    {
        try
        {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements())
            {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback())
                    continue;

                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses())
                {
                    InetAddress broadcast = interfaceAddress.getBroadcast();
                    if (broadcast != null)
                        sendDiscoveryRequest(socket, requestData, broadcast);
                }
            }
        }
        catch (SocketException e)
        {
            System.out.println("Could not enumerate broadcast addresses: " + e.getMessage());
        }
    }

    private void sendDiscoveryRequest(DatagramSocket socket, byte[] requestData, InetAddress address)
    {
        try
        {
            DatagramPacket request = new DatagramPacket(requestData, requestData.length, address, getDiscoveryPort());
            socket.send(request);
        }
        catch (IOException e)
        {
            System.out.println("Could not send discovery request to " + address.getHostAddress() + ": " + e.getMessage());
        }
    }

    private static class ServerDiscoveryResult
    {
        final String address;
        final int port;

        ServerDiscoveryResult(String address, int port)
        {
            this.address = address;
            this.port = port;
        }
    }

    private void beginFireInput()
    {
        if (shopState.isShopOpen())
            return;

        if (gameState != GameState.PLAYING) return;

        if (weaponInventory.getCurrentWeapon() == WeaponType.KNIFE)
        {
            performKnifeAttack();
            return;
        }

        if (!weaponInventory.currentUsesBullets()) return;
        if (weaponInventory.isReloading()) return;
        if (weaponInventory.getCurrentWeapon() == WeaponType.SHOTGUN && isShotgunPumping()) return;

        if (weaponInventory.getCurrentMagazineAmmo() <= 0)
        {
            weaponInventory.beginReload();
            return;
        }

        if (weaponInventory.isAutomaticWeapon())
        {
            isFiring = true;
            if (!weaponInventory.isCoolingDown())
                fireCurrentWeapon();
        }
        else
        {
            if (!weaponInventory.isCoolingDown())
                fireCurrentWeapon();
        }
    }

    private void endFireInput()
    {
        isFiring = false;
        gameAudio.stopRifleLoopSound();
    }

    private void markGamepadFireHeld()
    {
        gamepadFireSeenThisFrame = true;

        if (!gamepadFireHeld)
        {
            gamepadFireHeld = true;
            beginFireInput();
        }
    }

    private void finishGamepadFireIfReleased()
    {
        if (gamepadFireHeld && !gamepadFireSeenThisFrame)
        {
            gamepadFireHeld = false;
            endFireInput();
        }
    }

    private void applyGamepadLook(float dt)
    {
        if (gameState != GameState.PLAYING || orbitCam == null) return;

        if (java.lang.Math.abs(gamepadLookX) > GAMEPAD_DEADZONE)
            orbitCam.addAzimuth(-gamepadLookX * GAMEPAD_LOOK_SPEED * dt);

        if (java.lang.Math.abs(gamepadLookY) > GAMEPAD_DEADZONE)
            orbitCam.addElevation(gamepadLookY * GAMEPAD_LOOK_SPEED * dt);
    }

    private class GamepadLookXAction extends AbstractInputAction
    {
        @Override
        public void performAction(float time, net.java.games.input.Event e)
        {
            gamepadLookX = e.getValue();
        }
    }

    private class GamepadLookYAction extends AbstractInputAction
    {
        @Override
        public void performAction(float time, net.java.games.input.Event e)
        {
            gamepadLookY = e.getValue();
        }
    }

    private class GamepadFireAction extends AbstractInputAction
    {
        @Override
        public void performAction(float time, net.java.games.input.Event e)
        {
            if (e.getValue() > GAMEPAD_DEADZONE)
                markGamepadFireHeld();
        }
    }

    private class GamepadButtonFireAction extends AbstractInputAction
    {
        @Override
        public void performAction(float time, net.java.games.input.Event e)
        {
            markGamepadFireHeld();
        }
    }

    private class GamepadReloadAction extends AbstractInputAction
    {
        @Override
        public void performAction(float time, net.java.games.input.Event e)
        {
            if (isButtonPressed(e) && gameState == GameState.PLAYING)
                weaponInventory.beginReload();
        }
    }

    private class GamepadGrappleAction extends AbstractInputAction
    {
        @Override
        public void performAction(float time, net.java.games.input.Event e)
        {
            if (isButtonPressed(e) && gameState == GameState.PLAYING)
                startPlayerGrapple();
        }
    }

    private class GamepadNextWeaponAction extends AbstractInputAction
    {
        @Override
        public void performAction(float time, net.java.games.input.Event e)
        {
            if (isButtonPressed(e) && gameState == GameState.PLAYING)
            {
                weaponInventory.selectNext();
                updateWeaponVisibility();
            }
        }
    }

    private class GamepadPreviousWeaponAction extends AbstractInputAction
    {
        @Override
        public void performAction(float time, net.java.games.input.Event e)
        {
            if (isButtonPressed(e) && gameState == GameState.PLAYING)
            {
                weaponInventory.selectPrevious();
                updateWeaponVisibility();
            }
        }
    }

    private class GamepadAcceptAction extends AbstractInputAction
    {
        @Override
        public void performAction(float time, net.java.games.input.Event e)
        {
            if (!isButtonPressed(e)) return;

            if (playerDeathScreenActive)
            {
                continueAfterPlayerDeath();
                return;
            }

        if (gameState == GameState.MENU)
        {
            activateCurrentMenuSelection();
            return;
        }

        if (gameState == GameState.MULTIPLAYER_MENU)
        {
            if (menu.getSelectedMultiplayerIndex() == 0)
                beginHostGame();
            else
                beginJoinGame();

            return;
        }

        if (gameState == GameState.ROBOT_SELECT)
        {
            startSelectedGame();
            return;
        }
        }
    }

    private class GamepadLeftStickYAction extends AbstractInputAction
    {
        @Override
        public void performAction(float time, net.java.games.input.Event e)
        {
            float v = e.getValue();

            if (java.lang.Math.abs(v) < GAMEPAD_DEADZONE)
                return;

            if (gameState == GameState.MENU)
            {
                if (!acceptGamepadMenuInput()) return;

                if (v < 0.0f) menuMoveUp();
                else menuMoveDown();

                return;
            }

            if (gameState == GameState.MULTIPLAYER_MENU)
            {
                if (!acceptGamepadMenuInput()) return;

                if (v < 0.0f) menu.moveMultiplayerUp();
                else menu.moveMultiplayerDown();

                return;
            }

            if (gameState != GameState.PLAYING || cam == null)
                return;

            Vector3f camN = cam.getN();
            Vector3f forward = new Vector3f(camN.x, 0.0f, camN.z);

            if (forward.lengthSquared() < 0.000001f)
                return;

            forward.normalize();

            // stick up is usually negative, so this makes up = forward
            forward.mul(-v);

            movePlayerPhysics(forward, currentMoveSpeed);

            if (protClient != null && player != null)
                protClient.sendMoveMessage(player.getWorldLocation());
        }
    }

    private class GamepadLeftStickXAction extends AbstractInputAction
    {
        @Override
        public void performAction(float time, net.java.games.input.Event e)
        {
            float v = e.getValue();

            if (java.lang.Math.abs(v) < GAMEPAD_DEADZONE)
                return;

            if (gameState == GameState.MENU)
            {
                if (!acceptGamepadMenuInput()) return;

                if (v < 0.0f) menuPreviousMap();
                else menuNextMap();

                return;
            }

            if (gameState == GameState.ROBOT_SELECT)
            {
                if (java.lang.Math.abs(v) < GAMEPAD_DEADZONE) return;
                if (!acceptGamepadMenuInput()) return;

                if (v < 0.0f) previousAvatarSelection();
                else nextAvatarSelection();

                return;
            }

            if (gameState != GameState.PLAYING || cam == null)
                return;

            Vector3f camN = cam.getN();
            Vector3f forward = new Vector3f(camN.x, 0.0f, camN.z);

            if (forward.lengthSquared() < 0.000001f)
                return;

            forward.normalize();

            Vector3f right = new Vector3f(forward.z, 0.0f, -forward.x);
            right.normalize();

            // negate
            right.mul(-v);

            movePlayerPhysics(right, currentMoveSpeed);

            if (protClient != null && player != null)
                protClient.sendMoveMessage(player.getWorldLocation());
        }
    }

    private String getRobotSelectText()
    {
        StringBuilder sb = new StringBuilder("Pick your robot: ");

        for (int i = 0; i < avatarNames.length; i++)
        {
            if (i > 0) sb.append("   ");

            if (i == avatarSelection)
                sb.append("<").append(avatarNames[i]).append(">");
            else
                sb.append(avatarNames[i]);
        }

        return sb.toString();
    }

    private void previousAvatarSelection()
    {
        avatarSelection = (avatarSelection - 1 + avatarNames.length) % avatarNames.length;
        applyAvatarSelectionTexture();
    }

    private void nextAvatarSelection()
    {
        avatarSelection = (avatarSelection + 1) % avatarNames.length;
        applyAvatarSelectionTexture();
    }

    private void applyAvatarSelectionTexture()
    {
        if (robotTextures == null) return;
        if (avatarSelection < 0 || avatarSelection >= robotTextures.length) return;
        if (robotTextures[avatarSelection] == null) return;

        playerTx = robotTextures[avatarSelection];
        ghostT = playerTx;

        if (player != null)
            player.setTextureImage(playerTx);
    }

    private void startSelectedGame()
    {
        setMapSelection(menu.getSelectedMapIndex());
        applyMapSelection();
        switchTerrainPhysics();

        applyAvatarSelectionTexture();

        if (mapSelection == 1)
        {
            hideMapZeroBuildings();
            pendingCaseOneStart = true;
            pendingSkinnySpawn = true;
        }

        gameState = GameState.PLAYING;
        firstPersonMode = true;
        physicsDebug = false;
        respawnLives = MAX_RESPAWN_LIVES;
        pHealth = 100;
        playerDeathScreenActive = false;
        engine.disablePhysicsWorldRender();
        respawnPlayerAtCurrentMapStart();

        engine.getHUDmanager().setHUD1("", new Vector3f(1, 1, 1), 0, 0);
        engine.getHUDmanager().setHUD2("", new Vector3f(1, 1, 1), 0, 0);
        engine.getHUDmanager().setHUD3("", new Vector3f(1, 1, 1), 0, 0);
        engine.getHUDmanager().setHUD4("", new Vector3f(1, 1, 1), 0, 0);

        if (!isClientConnected && !isHostClient)
            enableOfflineHostMode();

        if (protClient != null)
            protClient.sendCreateMessage(getPlayerPosition());
    }

    @Override
    public void initializeGame()
    {
        System.out.println("=== initializeGame() reached ===");

        createViewports();
        installShutdownWindowHandler();

        cam = engine.getRenderSystem().getViewport("MAIN").getCamera();
        camOver = engine.getRenderSystem().getViewport("OVERHEAD").getCamera();
        orbitCam = new CameraOrbit3D(cam, player, playerScale);
        overheadCameraController.setOrbitCam(orbitCam);

        lastFrameTime = System.currentTimeMillis();
        currFrameTime = System.currentTimeMillis();
        elapsTime = 0.0;

        engine.getRenderSystem().setWindowDimensions(1280, 720);

        cam.setLocation(new Vector3f(0.0f, 8.0f, 12.0f));

        im = engine.getInputManager();

        FwdAction fwdA = new FwdAction(this, -25.0f, cam);
        FwdAction backA = new FwdAction(this, 25.0f, cam);
        StrafeAction leftS = new StrafeAction(this, 25.0f, cam);
        StrafeAction rightS = new StrafeAction(this, -25.0f, cam);

        AbstractInputAction zoomIn = overheadCameraController.createZoomInAction();
        AbstractInputAction zoomOut = overheadCameraController.createZoomOutAction();
        AbstractInputAction panUp = overheadCameraController.createPanUpAction();
        AbstractInputAction panDown = overheadCameraController.createPanDownAction();
        AbstractInputAction panLeft = overheadCameraController.createPanLeftAction();
        AbstractInputAction panRight = overheadCameraController.createPanRightAction();
        AbstractInputAction recenter = overheadCameraController.createRecenterAction();
        AbstractInputAction toggleFP = new AbstractInputAction() {
            @Override
            public void performAction(float time, net.java.games.input.Event e) {
                toggleFirstPersonMode();
            }
        };
        AbstractInputAction togglePhysics = new AbstractInputAction() {
            @Override
            public void performAction(float time, net.java.games.input.Event e) {
                togglePhysicsDebug();
            }
        };
        AbstractInputAction togglePlasmaFireMode = new AbstractInputAction() {
            @Override
            public void performAction(float time, net.java.games.input.Event e) {
                togglePlasmaFireMode();
            }
        };

        // corrected bindings: W forward, S backward
        im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.W, fwdA,
            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.S, backA,
            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.A, leftS,
            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.D, rightS,
            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.ADD, zoomIn,
            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.SUBTRACT, zoomOut,
            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.DOWN, panUp,
            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.UP, panDown,
            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.LEFT, panLeft,
            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.RIGHT, panRight,
            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.C, recenter,
            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.F,toggleFP,
            InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);          
        im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.P,togglePhysics,
            InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.B,togglePlasmaFireMode,
            InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

        // gamepad left stick: gameplay movement + menu navigation
        im.associateActionWithAllGamepads(net.java.games.input.Component.Identifier.Axis.Y, new GamepadLeftStickYAction(),
            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllGamepads(net.java.games.input.Component.Identifier.Axis.X, new GamepadLeftStickXAction(),
            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        // gamepad look: right stick
        im.associateActionWithAllGamepads(net.java.games.input.Component.Identifier.Axis.RX, new GamepadLookXAction(),
            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllGamepads(net.java.games.input.Component.Identifier.Axis.RY, new GamepadLookYAction(),
            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        // fire: right trigger if your controller reports it as Z or RZ
        im.associateActionWithAllGamepads(net.java.games.input.Component.Identifier.Axis.Z, new GamepadFireAction(),
            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllGamepads(net.java.games.input.Component.Identifier.Axis.RZ, new GamepadFireAction(),
            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        // backup fire: right bumper
        im.associateActionWithAllGamepads(net.java.games.input.Component.Identifier.Button._5, new GamepadButtonFireAction(),
            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        // buttons: A accept, B grapple, X reload, Y next weapon, LB previous weapon
        im.associateActionWithAllGamepads(net.java.games.input.Component.Identifier.Button._0, new GamepadAcceptAction(),
            InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllGamepads(net.java.games.input.Component.Identifier.Button._1, new GamepadGrappleAction(),
            InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllGamepads(net.java.games.input.Component.Identifier.Button._2, new GamepadReloadAction(),
            InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllGamepads(net.java.games.input.Component.Identifier.Button._3, new GamepadNextWeaponAction(),
            InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllGamepads(net.java.games.input.Component.Identifier.Button._4, new GamepadPreviousWeaponAction(),
            InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

        initAudio();
        setEarParameters();
    }

    private boolean areUfoWaveEnemiesCleared()
    {
        if (isHostClient)
            return activeApes.size() == 0;

        if (networkEnemyManager == null)
            return false;

        return networkWaveHasSpawnedEnemies && networkEnemyManager.getLivingEnemyCount() == 0;
    }

    private void startNextHostUfoWaveAndSync()
    {
        if (!isHostClient)
            return;

        ufoShopWindowUsedForCurrentWave = false;

        ufoWaveManager.startNextWave();

        if (protClient != null && ufoWaveManager.isActive())
        {
            protClient.sendUfoWaveStart(
                ufoWaveManager.getLastWaveTarget(),
                ufoWaveManager.getLastWaveApeCount(),
                ufoWaveManager.getLastWaveUfoIndex()
            );
        }
    }

    @Override
    public void update()
    {
        if (isShuttingDown || engine == null || engine.getRenderSystem() == null) return;

        if (gameState == GameState.MENU)
        {
            mouseModeInitiated = false;
            Vector3f titleColor = new Vector3f(0.95f, 0.8f, 0.45f);
            Vector3f bodyColor = new Vector3f(1.0f, 1.0f, 1.0f);
            Vector3f footerColor = new Vector3f(0.7f, 0.9f, 0.7f);

            engine.getHUDmanager().setHUD1(menu.getTitleText(), titleColor, 520, 620);
            engine.getHUDmanager().setHUD2(menu.getMenuText(), bodyColor, 120, 560);
            engine.getHUDmanager().setHUD3(menu.getFooterText(), footerColor, 420, 120);

            if (im != null)
                im.update(0.016f);
            return;
        }

        if (gameState == GameState.MULTIPLAYER_MENU)
        {
            mouseModeInitiated = false;
            engine.getHUDmanager().setHUD1("MULTIPLAYER", new Vector3f(0.95f, 0.8f, 0.45f), 500, 620);
            engine.getHUDmanager().setHUD2(menu.getMultiplayerText(), new Vector3f(1.0f, 1.0f, 1.0f), 250, 560);
            engine.getHUDmanager().setHUD3(menu.getMultiplayerFooterText(), new Vector3f(0.7f, 0.9f, 0.7f), 360, 120);
            engine.getHUDmanager().setHUD4(multiplayerStatusText, new Vector3f(0.7f, 0.9f, 1.0f), 350, 500);

            if (im != null)
                im.update(0.016f);
            return;
        }

        if (gameState == GameState.ROBOT_SELECT)
        {
            mouseModeInitiated = false;
            processNetworking(0.016f);

            engine.getHUDmanager().setHUD1(
                "ROBOT SELECT",
                new Vector3f(0.95f, 0.8f, 0.45f),
                500,
                620
            );

            engine.getHUDmanager().setHUD2(
                getRobotSelectText(),
                new Vector3f(1.0f, 1.0f, 1.0f),
                300,
                560
            );

            engine.getHUDmanager().setHUD3(
                "Use A/D or LEFT STICK X to choose, ENTER/A button to start",
                new Vector3f(0.7f, 0.9f, 0.7f),
                300,
                120
            );

            if (im != null)
                im.update(0.016f);

            return;
        }

        lastFrameTime = currFrameTime;
        currFrameTime = System.currentTimeMillis();
        float dt = (float)((currFrameTime - lastFrameTime) / 1000.0);
        elapsTime += dt;

        if (playerDeathScreenActive && !isHostClient)
        {
            processNetworking(dt);

            if (gm != null)
                gm.updateGhostAnimations(dt);

            showDeathContinueHud();
            return;
        }

        if (pendingDebugFinalUfoBeam)
        {
            pendingDebugFinalUfoBeam = false;
            debugStartFinalUfoBeam();
        }

        if (pendingCaseOneStart)
        {
            setupCaseOneStart();
            pendingCaseOneStart = false;
        }

        if (pendingSkinnySpawn)
        {
            if (isHostClient)
                spawnSkinnyWave();

            pendingSkinnySpawn = false;
        }

        cam = engine.getRenderSystem().getViewport("MAIN").getCamera();
        setEarParameters();
        
        currentMoveDir.set(0, 0, 0);
        gamepadFireSeenThisFrame = false;

        im.update(dt);

        applyGamepadLook(dt);
        finishGamepadFireIfReleased();

        if (shopState.isShopOpen())
        {
            currentMoveDir.set(0, 0, 0);
            stopPlayerHorizontalMotion();
        }

        updateLevelTwoArrivalEvent(dt);

        updateLevelTwoMusic(dt);

        if (playerP != null)
        {
            // While the tractor beam owns the player, do not let WASD overwrite
            // the upward beam velocity or add horizontal drift.
            boolean beamControllingPlayer = tractorBeamActive && playerInTractorBeam;
            boolean levelTwoThrowControllingPlayer = levelTwoThrowControlTimer > 0.0f;

            if (!beamControllingPlayer && !levelTwoThrowControllingPlayer)
            {
                float[] vel = playerP.getLinearVelocity();
                if (currentMoveDir.lengthSquared() > 0.0001f)
                {
                    currentMoveDir.normalize();
                    playerP.setLinearVelocity(new float[] {
                        currentMoveDir.x * currentMoveSpeed,
                        vel[1], // Preserve Y velocity for gravity/falling
                        currentMoveDir.z * currentMoveSpeed
                    });
                }
                else
                {
                    playerP.setLinearVelocity(new float[] { 0f, vel[1], 0f });
                }
            }
        }

        processNetworking(dt);

        weaponInventory.updateTimers(dt);

        updateKnifeStab(dt);

        updateWeaponAudio(dt);

        if (isFiring && weaponInventory.isAutomaticWeapon() && !weaponInventory.isReloading() && !weaponInventory.isCoolingDown())
        {
            if (weaponInventory.getCurrentMagazineAmmo() > 0)
                fireCurrentWeapon();
            else if (weaponInventory.getCurrentReserveAmmo() > 0)
                weaponInventory.beginReload();
            else
                isFiring = false;
        }

        if (physicsRunning && physicsEngine != null)
        {
            physicsEngine.update(dt);
            syncGameObjectToPhysics(player);
        }

        if (isHostClient)
            updateApeBehaviorTrees(dt);

        if (isHostClient)
            updateSkinnys(dt);

        checkGrapplePickup();

        updatePlayerGrapple(dt);

        if (isHostClient)
            updateApesFromPhysics();

        if (isHostClient)
            updateSkinnysFromPhysics();

        if (mapSelection == 0)
        {
            ufoWaveManager.update(dt);
        }

        if (isHostClient)
            updateDeadApes(dt);

        boolean wasShopping = shopState.isShopOpen();
        boolean vendingExpired = shopState.update(dt);

        if (vendingExpired && wasShopping)
        {
            exitVendingShopView();
        }

        if (vendingExpired && isHostClient && mapSelection == 0 && !ufoWaveManager.isActive() && !ufoWaveManager.isFinalWaveComplete())
        {
            startNextHostUfoWaveAndSync();
        }

        bulletManager.update(dt);

        updateTractorBeam(dt);
        
        // handle plasma burst firing
        if (plasmaBurstShotsRemaining > 0)
        {
            plasmaBurstTimer -= dt;

            if (plasmaBurstTimer <= 0.0f)
            {
                Vector3f forward = new Vector3f(cam.getN()).normalize();

                Vector3f spread = new Vector3f(
                    ((float)Math.random() - 0.5f) * 0.05f,
                    ((float)Math.random() - 0.5f) * 0.05f,
                    ((float)Math.random() - 0.5f) * 0.05f
                );

                Vector3f dir = new Vector3f(forward).add(spread).normalize();

                Vector3f spawnPos = new Vector3f(player.getWorldLocation())
                    .add(0.0f, 1.5f, 0.0f)
                    .add(new Vector3f(dir).mul(1.5f));

                int enemyDamage = weaponInventory.getCurrentEnemyBulletDamage();
                int brainDamage = weaponInventory.getCurrentBrainBulletDamage();

                bulletManager.spawnPlayerBullet(spawnPos, dir, true, enemyDamage, brainDamage);

                if (protClient != null)
                    protClient.sendPlayerBullet(spawnPos, dir, true, enemyDamage, brainDamage);

                weaponInventory.consumeCurrentRound();
                gameAudio.playPlasmaRifle();

                plasmaBurstShotsRemaining--;
                plasmaBurstTimer = plasmaBurstDelay;
            }
        }

        if (!mouseModeInitiated) initMouseMode();

        if (firstPersonMode)
            updateFirstPersonCamera();
        else
            orbitCam.updateCameraPosition();

        //updatePlayerVisibilityForCameraMode();

        if (playerS != null) playerS.updateAnimation();
        if (gm != null) gm.updateGhostAnimations(dt);
        if (skinnyS != null) skinnyS.updateAnimation();
        if (apeS != null) apeS.updateAnimation();
        if (brainS != null) brainS.updateAnimation();
        
        if (isHostClient)
        {
            updateBrainAnimation();
            updateBrainBoss(dt);
        }

        // if no apes alive, bring up shop, trigger next UFO, if last wave completed, trigger tractor beam
        if (mapSelection == 0 && !ufoWaveManager.isActive() && areUfoWaveEnemiesCleared())
        {
            if (isHostClient)
            {
                if (ufoWaveManager.isFinalWaveComplete())
                {
                    startTractorBeam();
                }
                else if (ufoWaveManager.getLastWaveApeCount() == 0)
                {
                    startNextHostUfoWaveAndSync();
                }
                else if (!shopState.isActive() && !ufoShopWindowUsedForCurrentWave)
                {
                    ufoShopWindowUsedForCurrentWave = true;
                    shopState.startWindow(ufoWaveManager.getLastWaveTarget(), terr);
                }
            }
            else
            {
                if (ufoWaveManager.getLastWaveApeCount() > 0
                    && !shopState.isActive()
                    && !ufoShopWindowUsedForCurrentWave)
                {
                    ufoShopWindowUsedForCurrentWave = true;
                    shopState.startWindow(ufoWaveManager.getLastWaveTarget(), terr);

                    // Prevent the non-host shop from reopening before the next host wave arrives.
                    networkWaveHasSpawnedEnemies = false;
                }
            }
        }

        updateStaticObjectsToTerrain();

        Vector3f camN = cam.getN();
        float flatX = camN.x;
        float flatZ = camN.z;
        if (java.lang.Math.abs(flatX) > 0.0001f || java.lang.Math.abs(flatZ) > 0.0001f)
        {
            float yaw = (float)java.lang.Math.atan2(flatX, flatZ);
            player.setLocalRotation(new Matrix4f().rotationY(yaw));
        }
        networkUpdateTimer += dt;

        if (networkUpdateTimer >= NETWORK_UPDATE_INTERVAL)
        {
            networkUpdateTimer = 0.0f;

            if (protClient != null && player != null)
                protClient.sendMoveMessage(player.getWorldLocation());
        }

        enemyNetworkUpdateTimer += dt;

        if (enemyNetworkUpdateTimer >= ENEMY_NETWORK_UPDATE_INTERVAL)
        {
            enemyNetworkUpdateTimer = 0.0f;
            sendApeNetworkUpdates();
            sendSkinnyNetworkUpdates();
            sendBrainNetworkUpdates();
        }

        Vector3f playerpos = player.getWorldLocation();

        pickupManager.handleCollisions(dt, player, weaponInventory, gameAudio);

        float vx = 0.0f;
        float vz = 0.0f;

        if (playerP != null)
        {
            float[] vel = playerP.getLinearVelocity();
            vx = vel[0];
            vz = vel[2];
        }

        float horizontalSpeed = (float)java.lang.Math.sqrt(vx * vx + vz * vz);
        isMoving = horizontalSpeed > 0.05f;

        if (playerS != null)
        {
            if (isMoving && !wasMoving)
                playerS.playAnimation("RUN", 0.3f, AnimatedShape.EndType.LOOP, 0);
            else if (!isMoving && wasMoving)
                playerS.playAnimation("STAND", 0.5f, AnimatedShape.EndType.LOOP, 0);
        }

        wasMoving = isMoving;
        prevPlayerPos.set(playerpos);

        overheadCameraController.applyTo(camOver, playerpos);

        if (playerDeathScreenActive)
        {
            showDeathContinueHud();
            return;
        }

        if (shopState.isActive())
        {
            shopState.renderHud(engine, pHealth, playerCredits, weaponInventory);
        }
        else
        {
            engine.getHUDmanager().setHUD1(
                "Health: " + pHealth + " | Lives: " + respawnLives + " | Credits: $" + playerCredits,
                new Vector3f(0, 1, 0),
                15,
                660
            );

            engine.getHUDmanager().setHUD2(
                weaponInventory.getHudText(),
                new Vector3f(1, 1, 1),
                15,
                630
            );

            engine.getHUDmanager().setHUD3(
                getObjectiveHudText(),
                new Vector3f(1, 1, 1),
                15,
                15
            );
        }
        if (brainActive && brainHealth > 0)
        {
            engine.getHUDmanager().setHUD4(
                getBrainHealthBar(),
                new Vector3f(1, 0, 0),
                440,
                650
            );
        }
        else
        {
            engine.getHUDmanager().setHUD4("", new Vector3f(1, 1, 1), 0, 0);
        }

        pickupManager.update(dt, terr);
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        if (playerDeathScreenActive)
        {
            if (e.getKeyCode() == KeyEvent.VK_ENTER)
                continueAfterPlayerDeath();

            return;
        }

        if (gameState == GameState.MENU)
        {
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
                    if (menu.activateSelection() == MainMenu.MenuAction.SELECT_MAP)
                    {
                        menu.previousMap();
                        setMapSelection(menu.getSelectedMapIndex());
                        applyMapSelection();
                    }
                    break;
                case KeyEvent.VK_RIGHT:
                    if (menu.activateSelection() == MainMenu.MenuAction.SELECT_MAP)
                    {
                        menu.nextMap();
                        setMapSelection(menu.getSelectedMapIndex());
                        applyMapSelection();
                    }
                    break;
                case KeyEvent.VK_ENTER:
                    activateCurrentMenuSelection();
                    break;
                default:
                    break;
            }

            super.keyPressed(e);
            return;
        }

        if (gameState == GameState.MULTIPLAYER_MENU)
        {
            switch (e.getKeyCode())
            {
                case KeyEvent.VK_UP:
                    menu.moveMultiplayerUp();
                    break;

                case KeyEvent.VK_DOWN:
                    menu.moveMultiplayerDown();
                    break;

                case KeyEvent.VK_ENTER:
                    if (menu.getSelectedMultiplayerIndex() == 0)
                        beginHostGame();
                    else
                        beginJoinGame();
                    break;

                case KeyEvent.VK_ESCAPE:
                    gameState = GameState.MENU;
                    multiplayerStatusText = "";
                    break;

                default:
                    break;
            }

            super.keyPressed(e);
            return;
        }

        if (gameState == GameState.ROBOT_SELECT)
        {
            switch (e.getKeyCode())
            {
                case KeyEvent.VK_A:
                case KeyEvent.VK_LEFT:
                    previousAvatarSelection();
                    break;

                case KeyEvent.VK_D:
                case KeyEvent.VK_RIGHT:
                    nextAvatarSelection();
                    break;

                case KeyEvent.VK_ENTER:
                    startSelectedGame();
                    break;

                case KeyEvent.VK_ESCAPE:
                    gameState = GameState.MENU;
                    break;

                default:
                    break;
            }

            super.keyPressed(e);
            return;
        }
        if (shopState.isShopOpen())
        {
            shopState.handleShopKey(e.getKeyCode(), this, weaponInventory);

            if (!shopState.isShopOpen())
                exitVendingShopView();

            return;
        }
        if (gameState == GameState.PLAYING)
        {
            switch (e.getKeyCode())
            {
                case KeyEvent.VK_ESCAPE:
                    gameState = GameState.PAUSED;
                    engine.getHUDmanager().setHUD1("PAUSED", new Vector3f(1, 1, 1), 600, 360);
                    break;
                case KeyEvent.VK_R:
                    weaponInventory.beginReload();
                    break;
                case KeyEvent.VK_E:
                    if (shopState.tryToggleShop(player, playerP, currentMoveDir))
                    {
                        enterVendingShopView();
                    }
                    else
                    {
                        startPlayerGrapple();
                    }
                    break;
                case KeyEvent.VK_BACK_SLASH:
                    restartGame = new RestartGame(this);
                    restartGame.performAction(0, null);
                    break;
                // DEBUG KEYS
                case KeyEvent.VK_T:
                    if (isHostClient)
                    {
                        pendingDebugFinalUfoBeam = true;
                        System.out.println("DEBUG: queued final UFO beam test");
                    }
                    else
                    {
                        System.out.println("DEBUG: T ignored on non-host; press T on host");
                    }
                    break;
                case KeyEvent.VK_Y:
                    spawnDebugSkinnyLoadout();
                    break;
                case KeyEvent.VK_U:
                    debugStartLevelTwoArrivalEvent();
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
        handleMouseLook(e);
    }

    @Override
    public void mouseDragged(MouseEvent e)
    {
        handleMouseLook(e);
    }

    private void handleMouseLook(MouseEvent e)
    {
        if (isShuttingDown || !mouseModeInitiated || orbitCam == null) return;

        if (isRecentering && e.getXOnScreen() == (int)centerX && e.getYOnScreen() == (int)centerY)
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

        recenterMouse();

        prevMouseX = centerX;
        prevMouseY = centerY;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e)
    {
        int clicks = e.getWheelRotation();
        if (clicks > 0) weaponInventory.selectNext();
        else if (clicks < 0) weaponInventory.selectPrevious();
        updateWeaponVisibility();
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        if (gameState != GameState.PLAYING) return;
        if (e.getButton() != MouseEvent.BUTTON1) return;

        recenterMouse();
        prevMouseX = centerX;
        prevMouseY = centerY;

        beginFireInput();
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        if (e.getButton() == MouseEvent.BUTTON1)
            endFireInput();
    }

    void toggleFirstPersonMode()
    {
        firstPersonMode = !firstPersonMode;
        updatePlayerVisibilityForCameraMode();
    }

    void togglePhysicsDebug()
    {
        physicsDebug = !physicsDebug;

        if (physicsDebug)
        {
            engine.enablePhysicsWorldRender();
            System.out.println("Physics Debug ON");
        }
        else
        {
            engine.disablePhysicsWorldRender();
            System.out.println("Physics Debug OFF");
        }
    }

    void togglePlasmaFireMode()
    {
        if (weaponInventory.togglePlasmaFireMode())
        {
            System.out.println("Plasma Rifle Mode: " + (weaponInventory.isPlasmaBurstMode() ? "BURST" : "FULL AUTO"));
        }
    }


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
        if (rise > maxStepHeight) return false;
        if (slope > maxClimbSlope) return false;
        return true;
    }

    public void movePlayerPhysics(Vector3f moveDir, float speed)
    {
        if (shopState.isShopOpen())
        {
            stopPlayerHorizontalMotion();
            return;
        }
        if (playerDeathScreenActive) return;
        if (gameState != GameState.PLAYING) return;
        if (playerP == null) return;

        Vector3f dir = new Vector3f(moveDir.x, 0f, moveDir.z);
        if (dir.lengthSquared() < 0.000001f) return;
        dir.normalize();

        currentMoveDir.add(dir);
        currentMoveSpeed = speed;
    }

    public void stopPlayerHorizontalMotion()
    {
        currentMoveDir.set(0, 0, 0);

        if (playerP != null)
        {
            float[] vel = playerP.getLinearVelocity();
            playerP.setLinearVelocity(new float[] { 0.0f, vel[1], 0.0f });
        }
    }

    private void fireCurrentWeapon()
    {
        if (!weaponInventory.currentUsesBullets()) return;
        if (weaponInventory.isReloading()) return;

        if (weaponInventory.getCurrentMagazineAmmo() <= 0)
        {
            weaponInventory.beginReload();
            return;
        }

        WeaponType currentWeapon = weaponInventory.getCurrentWeapon();
        if (currentWeapon == WeaponType.SHOTGUN && isShotgunPumping()) return;

        Vector3f forward = new Vector3f(cam.getN()).normalize();
        Vector3f playerPos = player.getWorldLocation();
        Vector3f spawnPos = new Vector3f(playerPos)
            .add(0.0f, 1.5f, 0.0f)
            .add(new Vector3f(forward).mul(1.5f));

        switch (currentWeapon)
        {
            case PISTOL:
                spawnAndSyncPlayerBullet(spawnPos, forward, false);

                weaponInventory.consumeCurrentRound();
                gameAudio.playPistolShot();
                break;

            case PLASMA_RIFLE:
                if (weaponInventory.isPlasmaBurstMode())
                {
                    plasmaBurstShotsRemaining = Math.min(3, weaponInventory.getCurrentMagazineAmmo());
                    plasmaBurstTimer = 0.0f;
                }
                else
                {
                    spawnAndSyncPlayerBullet(spawnPos, forward, true);

                    weaponInventory.consumeCurrentRound();
                    gameAudio.playPlasmaRifle();
                }
                break;

            case RIFLE:
                spawnAndSyncPlayerBullet(spawnPos, forward, false);

                weaponInventory.consumeCurrentRound();
                gameAudio.playRifleLoopSound();
                break;

            case SHOTGUN:
                for (int i = 0; i < shotgunPelletCount; i++)
                {
                    Vector3f spreadDir = new Vector3f(forward).add(
                        ((float)Math.random() - 0.5f) * shotgunSpread,
                        ((float)Math.random() - 0.5f) * shotgunSpread,
                        ((float)Math.random() - 0.5f) * shotgunSpread
                    ).normalize();

                    spawnAndSyncPlayerBullet(spawnPos, spreadDir, false);
                }
                weaponInventory.consumeCurrentRound();

                gameAudio.playShotgunShot();
                gameAudio.startShotgunPump(WeaponType.SHOTGUN.getFireDelay());
                break;

            default:
                return;
        }

        weaponInventory.startFireCooldown();
    }

    private void performKnifeAttack()
    {
        if (knifeAttackCooldown > 0.0f)
            return;

        if (player == null || cam == null)
            return;

        knifeAttackCooldown = KNIFE_ATTACK_COOLDOWN;
        knifeStabTimer = KNIFE_STAB_DURATION;

        Vector3f forward = new Vector3f(cam.getN()).normalize();

        Vector3f start = new Vector3f(player.getWorldLocation())
            .add(0.0f, 1.3f, 0.0f);

        for (float d = 0.75f; d <= KNIFE_RANGE; d += 0.35f)
        {
            Vector3f hitPoint = new Vector3f(start)
                .add(new Vector3f(forward).mul(d));

            int knifeDamage = weaponInventory.scaleDamage(KNIFE_DAMAGE);

            if (checkAndDamageApe(hitPoint, null, knifeDamage))
                return;

            if (checkAndDamageSkinny(hitPoint, null, knifeDamage))
                return;

            if (checkAndDamageBrain(hitPoint, knifeDamage))
                return;
        }
    }

    private void spawnAndSyncPlayerBullet(Vector3f spawnPos, Vector3f dir, boolean isPlasma)
    {
        int enemyDamage = weaponInventory.getCurrentEnemyBulletDamage();
        int brainDamage = weaponInventory.getCurrentBrainBulletDamage();

        bulletManager.spawnPlayerBullet(spawnPos, dir, isPlasma, enemyDamage, brainDamage);

        if (protClient != null)
            protClient.sendPlayerBullet(spawnPos, dir, isPlasma, enemyDamage, brainDamage);
    }

    private void updateKnifeStab(float dt)
    {
        if (knifeAttackCooldown > 0.0f)
            knifeAttackCooldown -= dt;

        if (knife == null)
            return;

        if (knifeStabTimer > 0.0f)
        {
            knifeStabTimer -= dt;

            float progress = 1.0f - java.lang.Math.max(0.0f, knifeStabTimer) / KNIFE_STAB_DURATION;

            float stabAmount;

            if (progress < 0.5f)
                stabAmount = progress * 2.0f * 0.75f;
            else
                stabAmount = (1.0f - progress) * 2.0f * 0.75f;

            knife.setLocalTranslation(
                new Matrix4f().translation(
                    weaponPos.x,
                    weaponPos.y,
                    weaponPos.z + stabAmount
                )
            );
        }
        else
        {
            knife.setLocalTranslation(
                new Matrix4f().translation(
                    weaponPos.x,
                    weaponPos.y,
                    weaponPos.z
                )
            );
        }
    }

    public void spawnEnemyBullet(Vector3f spawnPos, Vector3f dir, boolean isPlasma)
    {
        bulletManager.spawnEnemyBullet(spawnPos, dir, isPlasma);
    }

    public boolean checkAndDamageApe(Vector3f loc)
    {
        return checkAndDamageApe(loc, null, 100);
    }

    public boolean checkAndDamageApe(Vector3f loc, UUID shooterID)
    {
        return checkAndDamageApe(loc, shooterID, 100);
    }

    public boolean checkAndDamageApe(Vector3f loc, UUID shooterID, int damage)
    {
        for (int j = activeApes.size() - 1; j >= 0; j--)
        {
            GameObject ape = activeApes.get(j);
            Vector3f apePos = ape.getWorldLocation();

            if (loc.distance(apePos) < 1.0f)
            {
                if (!activeApeDead.get(j))
                {
                    int hp = activeApeHealth.get(j) - damage;
                    activeApeHealth.set(j, hp);

                    if (hp <= 0)
                    {
                        activeApeDead.set(j, true);
                        activeApeDeathTimers.set(j, 2.0f);

                        ape.setLocalRotation(new Matrix4f().rotationZ((float)Math.toRadians(90.0f)));

                        GameObject apeGun = activeApeGuns.get(j);
                        if (apeGun != null)
                            apeGun.setLocalScale(new Matrix4f().scaling(0.0001f));

                        gameAudio.playApeDie(ape.getWorldLocation());

                        PhysicsObject apeP = activeApePhysics.get(j);
                        if (apeP != null)
                        {
                            apeP.setAngularFactor(1f);
                            apeP.applyTorque(0.0f, 0.0f, 35.0f);
                        }

                        int creditAmount = 25 + new java.util.Random().nextInt(26);

                        if (shooterID == null)
                        {
                            addPlayerCredits(creditAmount);
                        }
                        else if (protClient != null && isHostClient)
                        {
                            protClient.sendCreditAward(shooterID, creditAmount);
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public boolean checkAndDamageSkinny(Vector3f loc)
    {
        return checkAndDamageSkinny(loc, null, 100);
    }

    public boolean checkAndDamageSkinny(Vector3f loc, UUID shooterID)
    {
        return checkAndDamageSkinny(loc, shooterID, 100);
    }

    public boolean checkAndDamageSkinny(Vector3f loc, UUID shooterID, int damage)
    {
        for (int i = activeSkinnys.size() - 1; i >= 0; i--)
        {
            if (activeSkinnyDead.get(i)) continue;

            GameObject s = activeSkinnys.get(i);

            if (loc.distance(s.getWorldLocation()) < 1.0f)
            {
                int hp = activeSkinnyHealth.get(i) - damage;
                activeSkinnyHealth.set(i, hp);

                if (hp <= 0)
                {
                    activeSkinnyDead.set(i, true);
                    skinnyKillCount++;

                    gameAudio.playAlienDie(s.getWorldLocation());

                    int creditAmount = 25 + new java.util.Random().nextInt(26);

                    if (shooterID == null)
                    {
                        addPlayerCredits(creditAmount);
                    }
                    else if (protClient != null && isHostClient)
                    {
                        protClient.sendCreditAward(shooterID, creditAmount);
                    }

                    if (skinnyKillCount % 10 == 0)
                    {
                        dropGrapplePickup(s.getWorldLocation());
                    }

                    s.setLocalScale(new Matrix4f().scaling(0.0001f));

                    GameObject plasma = activeSkinnyPlasma.get(i);
                    GameObject grapple = activeSkinnyGrapple.get(i);

                    if (plasma != null)
                        plasma.setLocalScale(new Matrix4f().scaling(0.0001f));

                    if (grapple != null)
                        grapple.setLocalScale(new Matrix4f().scaling(0.0001f));

                    /*
                    // Alien grapple lines are disabled, so there is no line visual to hide here.
                    if (i < activeSkinnyGrappleLines.size())
                    {
                        GameObject line = activeSkinnyGrappleLines.get(i);
                        if (line != null)
                            line.setLocalScale(new Matrix4f().scaling(0.0001f));
                    }
                    */

                    PhysicsObject p = activeSkinnyPhysics.get(i);
                    if (p != null)
                        physicsEngine.removeObject(p.getUID());
                }
                return true;
            }
        }
        return false;
    }

    public boolean checkAndDamageBrain(Vector3f loc)
    {
        return checkAndDamageBrain(loc, 20);
    }

    public boolean checkAndDamageBrain(Vector3f loc, int damage)
    {
        if (!isHostClient)
            return false;

        if (!brainActive || brain == null || brainHealth <= 0)
            return false;

        if (loc.distance(brain.getWorldLocation()) < 2.0f)
        {
            brainHealth -= damage;

            if (brainHealth <= 0)
            {
                brainHealth = 0;
                brainActive = false;
                brainDefeated = true;
                brain.setLocalScale(new Matrix4f().scaling(0.0001f));
                System.out.println("Brain defeated!");
            }

            return true;
        }

        return false;
    }

    private void updateApesFromPhysics()
    {
        for (int i = 0; i < activeApes.size(); i++)
        {
            GameObject apeObj = activeApes.get(i);
            PhysicsObject apePhys = activeApePhysics.get(i);

            if (apeObj == null || apePhys == null) continue;

            Vector3f loc = apePhys.getLocation();
            Matrix4f locMat = new Matrix4f().translation(loc.x, loc.y - 1.1f, loc.z);
            apeObj.setLocalTranslation(locMat);

            if (!activeApeDead.get(i))
            {
                apeObj.getRenderStates().setModelOrientationCorrection(
                    new Matrix4f()
                        .rotationX((float)Math.toRadians(90.0f))
                        .rotateZ((float)Math.toRadians(180.0f))
                );
            }
            else
            {
                apeObj.getRenderStates().setModelOrientationCorrection(
                    new Matrix4f()
                        .rotationX((float)Math.toRadians(90.0f))
                        .rotateZ((float)Math.toRadians(270.0f))
                );
            }
        }
    }

    private void updateSkinnysFromPhysics()
    {
        for (int i = 0; i < activeSkinnys.size(); i++)
        {
            if (activeSkinnyDead.get(i)) continue;

            GameObject skinnyObj = activeSkinnys.get(i);
            PhysicsObject skinnyPhys = activeSkinnyPhysics.get(i);

            if (skinnyObj == null || skinnyPhys == null) continue;

            Vector3f loc = skinnyPhys.getLocation();

            skinnyObj.setLocalTranslation(
                new Matrix4f().translation(loc.x, loc.y - 1.0f, loc.z)
            );

            Vector3f playerPos = player.getWorldLocation();
            Vector3f toPlayer = new Vector3f(playerPos).sub(loc);

            if (toPlayer.lengthSquared() > 0.001f)
            {
                float yaw = (float)java.lang.Math.atan2(toPlayer.x, toPlayer.z);
                skinnyObj.setLocalRotation(new Matrix4f().rotationY(yaw));
            }
        }
    }

    private void dropGrapplePickup(Vector3f pos)
    {
        showGrapplePickup(pos);

        if (protClient != null && isHostClient)
            protClient.sendGrappleDrop(pos);

        System.out.println("Grapple pickup dropped");
    }

    private void checkGrapplePickup()
    {
        if (!grapplePickupActive || grapplePickup == null || player == null) return;

        if (player.getWorldLocation().distance(grapplePickup.getWorldLocation()) < 2.0f)
        {
            playerHasGrapple = true;
            hideGrapplePickup();

            if (protClient != null)
                protClient.sendGrappleTaken();

            System.out.println("Player picked up grapple");
        }
    }

    private void showGrapplePickup(Vector3f pos)
    {
        if (grapplePickup == null) return;

        grapplePickup.setLocalTranslation(
            new Matrix4f().translation(pos.x, pos.y + 1.0f, pos.z)
        );

        grapplePickup.setLocalScale(new Matrix4f().scaling(0.08f));
        grapplePickupActive = true;
    }

    private void hideGrapplePickup()
    {
        grapplePickupActive = false;

        if (grapplePickup != null)
        {
            grapplePickup.setLocalScale(new Matrix4f().scaling(0.0001f));
            grapplePickup.setLocalTranslation(new Matrix4f().translation(0.0f, -10000.0f, 0.0f));
        }
    }

    private void startPlayerGrapple()
    {
        if (!playerHasGrapple || playerGrappling || cam == null || playerP == null) return;

        grappleDir.set(cam.getN()).normalize();

        playerGrappling = true;
        grappleTimer = grappleDuration;

        System.out.println("Player grapple fired");
        if (playerGrappleLine != null)
        {
            Vector3f start = new Vector3f(player.getWorldLocation()).add(0.0f, 1.5f, 0.0f);
            Vector3f end = new Vector3f(start).add(new Vector3f(grappleDir).mul(60.0f));
            Vector3f mid = new Vector3f(start).add(end).mul(0.5f);

            playerGrappleLine.setLocalTranslation(new Matrix4f().translation(mid));

            playerGrappleLine.setLocalScale(new Matrix4f().scaling(0.05f, 30.0f, 0.05f));

            float yaw = (float)java.lang.Math.atan2(grappleDir.x, grappleDir.z);
            float pitch = (float)java.lang.Math.asin(grappleDir.y);

            playerGrappleLine.setLocalRotation(
                new Matrix4f()
                    .rotationY(yaw)
                    .rotateX((float)java.lang.Math.toRadians(90.0f) - pitch)
            );
        }
    }

    private void updatePlayerGrapple(float dt)
    {
        if (!playerGrappling || playerP == null) return;

        grappleTimer -= dt;

        playerP.setLinearVelocity(new float[] {
            grappleDir.x * grappleSpeed,
            grappleDir.y * grappleSpeed,
            grappleDir.z * grappleSpeed
        });

        if (grappleTimer <= 0.0f)
        {
            playerGrappling = false;
            playerP.setLinearVelocity(new float[] { 0.0f, 0.0f, 0.0f });

            if (playerGrappleLine != null)
                playerGrappleLine.setLocalScale(new Matrix4f().scaling(0.0001f));
        }
    }

    private void scheduleLevelTwoArrivalEvent()
    {
        levelTwoArrivalEventPending = true;
        levelTwoArrivalTimer = levelTwoArrivalDelay;
        levelTwoThrowControlTimer = 0.0f;

        System.out.println("Level two arrival event scheduled: roar/throwback in 1 second");
    }

    private void updateLevelTwoArrivalEvent(float dt)
    {
        if (levelTwoThrowControlTimer > 0.0f)
        {
            levelTwoThrowControlTimer -= dt;

            if (levelTwoThrowControlTimer < 0.0f)
                levelTwoThrowControlTimer = 0.0f;
        }

        if (!levelTwoArrivalEventPending)
            return;

        levelTwoArrivalTimer -= dt;

        if (levelTwoArrivalTimer <= 0.0f)
            triggerLevelTwoArrivalEvent();
    }
    
    private void updateLevelTwoMusic(float dt)
    {
        if (!levelTwoMusicPending)
            return;

        levelTwoMusicTimer -= dt;

        if (levelTwoMusicTimer <= 0.0f)
        {
            levelTwoMusicPending = false;

            if (!bossMusicStarted)
                gameAudio.playLevel2Music();
        }
    }

    private void triggerLevelTwoArrivalEvent()
    {
        levelTwoArrivalEventPending = false;

        gameAudio.playRoar();
        throwPlayerBackFromLevelTwoSpawn();
        playerKnockedOffLevelTwo = true;

        System.out.println("Level two arrival event fired: TAGE roar + throwback");
    }

    private void throwPlayerBackFromLevelTwoSpawn()
    {
        if (playerP == null)
            return;

        Vector3f throwDir = new Vector3f(levelTwoThrowDir);

        if (throwDir.lengthSquared() < 0.0001f)
            throwDir.set(0.0f, 0.0f, 1.0f);
        else
            throwDir.normalize();

        playerP.setLinearVelocity(new float[] {
            throwDir.x * levelTwoThrowBackSpeed,
            levelTwoThrowUpSpeed,
            throwDir.z * levelTwoThrowBackSpeed
        });

        levelTwoThrowControlTimer = levelTwoThrowControlDuration;
    }

    private void startTractorBeam()
    {
        if (tractorBeamActive) return;

        tractorBeamActive = true;
        playerInTractorBeam = false;

        Vector3f ufoPos = ufoWaveManager.getLargeUfoPosition();

        if (protClient != null && isHostClient)
            protClient.sendTractorBeamStart(ufoPos);

        tractorBeamTopY = ufoPos.y;

        tractorBeamCenter.set(
            ufoPos.x,
            ufoPos.y - (tractorBeamHeight * 0.5f),
            ufoPos.z
        );

        if (tractorBeamLight != null)
        {
            tractorBeamLight.setLocation(new Vector3f(ufoPos.x, ufoPos.y - 1.0f, ufoPos.z));
            tractorBeamLight.setDirection(new Vector3f(0.0f, -1.0f, 0.0f));
            tractorBeamLight.enable();
        }

        System.out.println("Tractor beam active from large UFO (gameplay trigger, no physics collider)");
    }

    private void updateTractorBeam(float dt)
    {
        if (!tractorBeamActive || player == null || playerP == null)
            return;

        Vector3f playerPos = player.getWorldLocation();

        float dx = playerPos.x - tractorBeamCenter.x;
        float dz = playerPos.z - tractorBeamCenter.z;
        float horizontalDist = (float)Math.sqrt(dx * dx + dz * dz);

        // Pure gameplay trigger: no cylinder/collider.
        // The player is considered captured if their X/Z position is under the large UFO
        // after the final wave has enabled the beam.
        boolean underLargeUfo = horizontalDist <= tractorBeamRadius;
        boolean belowUfo = playerPos.y <= tractorBeamTopY;

        if (underLargeUfo && belowUfo)
        {
            playerInTractorBeam = true;

            // Center the lift so the player cannot fight the beam with movement input.
            playerP.setLinearVelocity(new float[] {
                0.0f,
                tractorBeamLiftSpeed,
                0.0f
            });

            if (playerPos.y >= tractorBeamTopY - 1.0f)
                swapToCaseOneTerrain();
        }
        else
        {
            playerInTractorBeam = false;
        }
    }

    private void handlePlayerDeath()
    {
        if (playerDeathScreenActive)
            return;

        respawnLives = java.lang.Math.max(0, respawnLives - 1);
        playerDeathScreenActive = true;
        gameState = GameState.PAUSED;
        physicsDebug = true;
        engine.enablePhysicsWorldRender();
        isFiring = false;
        plasmaBurstShotsRemaining = 0;
        playerGrappling = false;
        currentMoveDir.set(0.0f, 0.0f, 0.0f);

        gameAudio.stopRifleLoopSound();

        if (playerP != null)
            playerP.setLinearVelocity(new float[] { 0.0f, 0.0f, 0.0f });

        if (playerGrappleLine != null)
            playerGrappleLine.setLocalScale(new Matrix4f().scaling(0.0001f));

        Vector3f deathPos = player != null ? player.getWorldLocation() : new Vector3f(0.0f, 0.0f, 0.0f);
        gameAudio.playPlayerDie(deathPos);

        showDeathContinueHud();
    }

    private void showDeathContinueHud()
    {
        if (respawnLives <= 0)
        {
            engine.getHUDmanager().setHUD1(
                "GAME OVER",
                new Vector3f(1.0f, 0.1f, 0.1f),
                520,
                400
            );

            engine.getHUDmanager().setHUD2(
                "<press ENTER for menu>",
                new Vector3f(0.95f, 0.95f, 0.95f),
                460,
                350
            );

            engine.getHUDmanager().setHUD3("", new Vector3f(1.0f, 1.0f, 1.0f), 0, 0);
            engine.getHUDmanager().setHUD4("", new Vector3f(1.0f, 1.0f, 1.0f), 0, 0);
            return;
        }

        engine.getHUDmanager().setHUD1(
            "RESPAWN",
            new Vector3f(1.0f, 1.0f, 1.0f),
            520,
            400
        );

        engine.getHUDmanager().setHUD2(
            "Lives remaining: " + respawnLives + "  |  <press ENTER>",
            new Vector3f(0.95f, 0.95f, 0.95f),
            430,
            350
        );

        engine.getHUDmanager().setHUD3("", new Vector3f(1.0f, 1.0f, 1.0f), 0, 0);
        engine.getHUDmanager().setHUD4("", new Vector3f(1.0f, 1.0f, 1.0f), 0, 0);
    }

    private void continueAfterPlayerDeath()
    {
        if (respawnLives <= 0)
        {
            returnToMenuAfterGameOver();
            return;
        }

        playerDeathScreenActive = false;
        pHealth = 100;
        gameState = GameState.PLAYING;
        physicsDebug = false;
        engine.disablePhysicsWorldRender();
        isFiring = false;
        plasmaBurstShotsRemaining = 0;
        playerGrappling = false;
        currentMoveDir.set(0.0f, 0.0f, 0.0f);

        if (playerP != null)
            playerP.setLinearVelocity(new float[] { 0.0f, 0.0f, 0.0f });

        respawnPlayerAtCurrentMapStart();

        if (playerGrappleLine != null)
            playerGrappleLine.setLocalScale(new Matrix4f().scaling(0.0001f));

        engine.getHUDmanager().setHUD1("", new Vector3f(1.0f, 1.0f, 1.0f), 0, 0);
        engine.getHUDmanager().setHUD2("", new Vector3f(1.0f, 1.0f, 1.0f), 0, 0);
        engine.getHUDmanager().setHUD3("", new Vector3f(1.0f, 1.0f, 1.0f), 0, 0);
        engine.getHUDmanager().setHUD4("", new Vector3f(1.0f, 1.0f, 1.0f), 0, 0);

        System.out.println("Player continued after death");
    }

    private void respawnPlayerAtCurrentMapStart()
    {
        if (player == null)
            return;

        Vector3f respawnPos = getCurrentMapRespawnPosition();

        if (playerP != null)
        {
            playerP.setLocation(new float[] { respawnPos.x, respawnPos.y, respawnPos.z });
            playerP.setLinearVelocity(new float[] { 0.0f, 0.0f, 0.0f });
        }

        player.setLocalTranslation(
            new Matrix4f().translation(respawnPos.x, respawnPos.y - playerVisualYOffset, respawnPos.z)
        );
    }

    private Vector3f getCurrentMapRespawnPosition()
    {
        if (terr == null)
            return new Vector3f(playerStartPos);

        if (mapSelection == 1)
        {
            float y = terr.getHeight(0.0f, 10.0f) + playerVisualYOffset + 2.0f;
            return new Vector3f(0.0f, y, 10.0f);
        }

        return new Vector3f(playerStartPos);
    }

    private void returnToMenuAfterGameOver()
    {
        playerDeathScreenActive = false;
        gameState = GameState.MENU;
        physicsDebug = false;
        pHealth = 100;
        respawnLives = MAX_RESPAWN_LIVES;
        isFiring = false;
        plasmaBurstShotsRemaining = 0;
        playerGrappling = false;
        currentMoveDir.set(0.0f, 0.0f, 0.0f);
        gameAudio.stopRifleLoopSound();
        engine.disablePhysicsWorldRender();

        if (playerP != null)
            playerP.setLinearVelocity(new float[] { 0.0f, 0.0f, 0.0f });

        engine.getHUDmanager().setHUD1("", new Vector3f(1.0f, 1.0f, 1.0f), 0, 0);
        engine.getHUDmanager().setHUD2("", new Vector3f(1.0f, 1.0f, 1.0f), 0, 0);
        engine.getHUDmanager().setHUD3("", new Vector3f(1.0f, 1.0f, 1.0f), 0, 0);
        engine.getHUDmanager().setHUD4("", new Vector3f(1.0f, 1.0f, 1.0f), 0, 0);
    }

    private String getObjectiveHudText()
    {
        if (brainDefeated)
            return "YOU WIN!";

        if (brainActive && brainHealth > 0)
            return "Finish off the brain!";

        if (mapSelection == 1)
        {
            if (playerHasGrapple)
                return "One of those weird aliens dropped this grapple gun, press E to grapple! Get back up there!";

            if (playerKnockedOffLevelTwo)
                return "There's got to be a way to get back up there!";

            return "They left the tractor beam active, get in there and destroy the brain!";
        }

        if (tractorBeamActive)
            return "They left the tractor beam active, get in there and destroy the brain!";

        return "Defend Earth from the robot monkeys!";
    }

    private String getTractorBeamDebugText(Vector3f playerPos)
    {
        if (!tractorBeamActive)
            return "Beam: OFF";

        float dx = playerPos.x - tractorBeamCenter.x;
        float dz = playerPos.z - tractorBeamCenter.z;
        float horizontalDist = (float)Math.sqrt(dx * dx + dz * dz);

        return String.format(
            "Beam: ACTIVE | Under UFO: %s | Dist: %.2f/%.2f | TopY: %.2f",
            playerInTractorBeam ? "YES" : "NO",
            horizontalDist,
            tractorBeamRadius,
            tractorBeamTopY
        );
    }

    private void applyMapSelection()
    {
        if (terr == null) return;

        switch (mapSelection)
        {
            case 0:
                terr.setTextureImage(terrTxMap0);
                terr.setHeightMap(heightMap0);
                (engine.getSceneGraph()).setActiveSkyBoxTexture(islandSkyBox);
                terr.getRenderStates().setTileFactor(10);
                break;
            case 1:
                terr.setTextureImage(terrTxMap1);
                terr.setHeightMap(heightMap1);
                (engine.getSceneGraph()).setActiveSkyBoxTexture(spaceSkyBox);
                terr.getRenderStates().setTileFactor(100);
                hideMapZeroBuildings();
                break;
            default:
                terr.setTextureImage(terrTxMap0);
                terr.setHeightMap(heightMap0);
                terr.getRenderStates().setTileFactor(10);
                break;
        }
    }

    private void switchTerrainPhysics()
    {
        if (terrainP0 == null || terrainP1 == null) return;

        if (mapSelection == 0)
        {
            terrainP0.setLocation(new float[] { 0.0f, 0.0f, 0.0f });
            terrainP1.setLocation(new float[] { 0.0f, -10000.0f, 0.0f });
            terrainP = terrainP0;
        }
        else
        {
            terrainP0.setLocation(new float[] { 0.0f, -10000.0f, 0.0f });
            terrainP1.setLocation(new float[] { 0.0f, 0.0f, 0.0f });
            terrainP = terrainP1;
        }

        terr.setPhysicsObject(terrainP);
    }

    private void hideMapZeroBuildings()
    {
        if (centerBuilding != null)
            centerBuilding.setLocalScale(new Matrix4f().scaling(0.0001f));

        for (GameObject b : smallBuildings)
            if (b != null) b.setLocalScale(new Matrix4f().scaling(0.0001f));

        for (GameObject b : smallBuildings2)
            if (b != null) b.setLocalScale(new Matrix4f().scaling(0.0001f));

        for (PhysicsObject p : buildingPhysics)
            if (p != null && physicsEngine != null)
                physicsEngine.removeObject(p.getUID());

        buildingPhysics.clear();
    }

    private void hideLevelZeroUfos()
    {
        if (ufoWaveManager != null)
            ufoWaveManager.hideAllUfos();
    }

    private void swapToCaseOneTerrain()
    {
        tractorBeamActive = false;
        playerInTractorBeam = false;

        if (tractorBeamLight != null)
            tractorBeamLight.disable();

        hideMapZeroBuildings();
        hideLevelZeroUfos();

        mapSelection = 1;
        applyMapSelection();
        switchTerrainPhysics();
        pendingSkinnySpawn = true;

        setupCaseOneStart();

        System.out.println("Swapped to case 1 terrain");
    }

    private void sendApeNetworkUpdates()
    {
        if (!isHostClient || protClient == null)
            return;

        for (int i = 0; i < activeApes.size(); i++)
        {
            GameObject apeObj = activeApes.get(i);

            if (apeObj == null)
                continue;

            int enemyID = activeApeNetworkIds.get(i);
            Vector3f pos = apeObj.getWorldLocation();

            float yaw = 0.0f;

            Vector3f targetPos = getClosestPlayerOrGhostTarget(pos);

            if (targetPos != null)
            {
                Vector3f toTarget = new Vector3f(targetPos).sub(pos);

                if (toTarget.lengthSquared() > 0.001f)
                    yaw = (float)java.lang.Math.atan2(toTarget.x, toTarget.z);
            }

            int health = activeApeHealth.get(i);
            boolean dead = activeApeDead.get(i);

            protClient.sendEnemyUpdate(enemyID, "APE", pos, yaw, health, dead);
        }
    }

    private void sendSkinnyNetworkUpdates()
    {
        if (!isHostClient || protClient == null)
            return;

        for (int i = 0; i < activeSkinnys.size(); i++)
        {
            GameObject skinnyObj = activeSkinnys.get(i);

            if (skinnyObj == null)
                continue;

            int enemyID = activeSkinnyNetworkIds.get(i);
            Vector3f pos = skinnyObj.getWorldLocation();

            float yaw = 0.0f;

            Vector3f targetPos = getClosestPlayerOrGhostTarget(pos);

            if (targetPos != null)
            {
                Vector3f toTarget = new Vector3f(targetPos).sub(pos);

                if (toTarget.lengthSquared() > 0.001f)
                    yaw = (float)java.lang.Math.atan2(toTarget.x, toTarget.z);
            }

            int health = activeSkinnyHealth.get(i);
            boolean dead = activeSkinnyDead.get(i);

            protClient.sendEnemyUpdate(enemyID, "SKINNY", pos, yaw, health, dead);
        }
    }

    private void sendBrainNetworkUpdates()
    {
        if (!isHostClient || protClient == null || brain == null || mapSelection != 1)
            return;

        if (!brainActive && !brainDefeated)
            return;

        Vector3f pos = brain.getWorldLocation();

        float yaw = 0.0f;

        Vector3f targetPos = getClosestPlayerOrGhostTarget(pos);

        if (targetPos != null)
        {
            Vector3f toTarget = new Vector3f(targetPos).sub(pos);

            if (toTarget.lengthSquared() > 0.001f)
                yaw = (float)java.lang.Math.atan2(toTarget.x, toTarget.z);
        }

        boolean dead = brainDefeated || brainHealth <= 0;

        protClient.sendEnemyUpdate(
            NETWORK_BRAIN_ID,
            "BRAIN",
            pos,
            yaw,
            brainHealth,
            dead
        );
    }

    public void receiveNetworkEnemyUpdate(int enemyID, String enemyType, Vector3f pos, float yaw, int health, boolean dead)
    {
        if (isHostClient)
            return;

        if (enemyType.equals("BRAIN"))
        {
            receiveNetworkBrainUpdate(pos, yaw, health, dead);
            return;
        }

        if (enemyType.equals("APE"))
            networkWaveHasSpawnedEnemies = true;

        if (networkEnemyManager != null)
            networkEnemyManager.updateEnemy(enemyID, enemyType, pos, yaw, health, dead);
    }

    public void receiveNetworkEnemyRemove(int enemyID, String enemyType)
    {
        if (isHostClient)
            return;

        if (networkEnemyManager != null)
            networkEnemyManager.removeEnemy(enemyID);
    }

    public void receiveNetworkEnemyBullet(Vector3f pos, Vector3f dir, boolean isPlasma)
    {
        if (isHostClient)
            return;

        spawnEnemyBullet(pos, dir, isPlasma);
        gameAudio.playNpcPlasma(pos);
    }

    public void receiveNetworkPlayerBullet(UUID shooterID, Vector3f pos, Vector3f dir, boolean isPlasma, int enemyDamage, int brainDamage)
    {
        if (!isHostClient)
            return;

        bulletManager.spawnNetworkPlayerBullet(shooterID, pos, dir, isPlasma, enemyDamage, brainDamage);
    }
    
    public void receiveCreditAward(int amount)
    {
        addPlayerCredits(amount);
    }
    
    public void receiveNetworkUfoWaveStart(Vector3f pos, int apeCount, int ufoIndex)
    {
        if (isHostClient)
            return;

        networkWaveHasSpawnedEnemies = false;
        ufoShopWindowUsedForCurrentWave = false;

        if (ufoWaveManager != null)
            ufoWaveManager.startNetworkWave(pos, apeCount, ufoIndex);
    }

    public void receiveNetworkTractorBeamStart(Vector3f ufoPos)
    {
        if (isHostClient)
            return;

        tractorBeamActive = true;
        playerInTractorBeam = false;

        tractorBeamTopY = ufoPos.y;

        tractorBeamCenter.set(
            ufoPos.x,
            ufoPos.y - (tractorBeamHeight * 0.5f),
            ufoPos.z
        );

        if (tractorBeamLight != null)
        {
            tractorBeamLight.setLocation(new Vector3f(ufoPos.x, ufoPos.y - 1.0f, ufoPos.z));
            tractorBeamLight.setDirection(new Vector3f(0.0f, -1.0f, 0.0f));
            tractorBeamLight.enable();
        }
    }

    public void receiveNetworkGrappleDrop(Vector3f pos)
    {
        if (isHostClient)
            return;

        showGrapplePickup(pos);
    }

    public void receiveNetworkGrappleTaken()
    {
        hideGrapplePickup();
    }

    private void receiveNetworkBrainUpdate(Vector3f pos, float yaw, int health, boolean dead)
    {
        if (brain == null)
            return;

        brainHealth = health;

        if (dead || health <= 0)
        {
            brainHealth = 0;
            brainActive = false;
            brainFloating = false;
            brainDefeated = true;
            brain.setLocalScale(new Matrix4f().scaling(0.0001f));
            return;
        }

        boolean wasInactive = !brainActive;

        brainActive = true;
        brainFloating = true;
        brainDefeated = false;

        brain.setLocalTranslation(new Matrix4f().translation(pos));
        brain.setLocalRotation(new Matrix4f().rotationY(yaw));
        brain.setLocalScale(new Matrix4f().scaling(0.1f));

        if (brainS != null && wasInactive)
            brainS.playAnimation("FLOAT", 0.3f, AnimatedShape.EndType.LOOP, 0);

        if (wasInactive)
            startBossMusic();
    }

    // ========================================================
    // GETTERS & SETTERS
    // ========================================================

    // --- Engine & Core ---
    public Engine getEngine() { return engine; }
    public Camera getCamera() { return cam; }

    // --- Game State & Flow ---
    public void setGameState(String state) { gameState = GameState.valueOf(state); }
    public void setMapSelection(int selection) { mapSelection = selection; }

    // --- Player Stats & Info ---
    public GameObject getAvatar() { return player; }
    public float getPlayerScale() { return playerScale; }
    public Vector3f getPlayerPosition()
    {
        if (player == null) return new Vector3f(0, 0, 0);
        return player.getWorldLocation();
    }
    public void setPlayerHealth(int value)
    {
        int oldHealth = pHealth;
        pHealth = java.lang.Math.max(pHealthMin, java.lang.Math.min(value, pHealthMax));

        if (oldHealth > 0 && pHealth <= 0)
            handlePlayerDeath();
    }
    public void addPlayerHealth(int amount) { setPlayerHealth(pHealth + amount); }
    public int getPlayerHealth() { return pHealth; }
    public int getPlayerHealthMax() { return pHealthMax; }
    public int getPlayerAmmo() { return weaponInventory.getTotalAmmo(); }
    public void addPlayerCredits(int amount)
    {
        playerCredits += amount;
    }

    public boolean spendPlayerCredits(int amount)
    {
        if (amount <= 0)
            return true;

        if (playerCredits < amount)
            return false;

        playerCredits -= amount;
        return true;
    }

    public int getPlayerCredits()
    {
        return playerCredits;
    }

    // --- Networking & Multiplayer ---
    public ProtocolClient getProtocolClient() { return protClient; }
    public void setIsConnected(boolean value) { isClientConnected = value; }
    public void setIsHostClient(boolean value)
    {
        isHostClient = value;
    }

    public boolean isHostClient()
    {
        return isHostClient;
    }
    public GhostManager getGhostManager() { return gm; }
    public ObjShape getGhostShape() { return ghostS; }
    public TextureImage getGhostTexture() { return ghostT; }

    public ObjShape getApeShape()
    {
        return apeS;
    }

    public TextureImage getApeTexture()
    {
        return apeTx;
    }

    public ObjShape getSkinnyShape()
    {
        return skinnyS;
    }

    public TextureImage getSkinnyTexture()
    {
        return skinnyTx;
    }

    public int getAvatarSelection()
    {
        return avatarSelection;
    }

    public TextureImage getRobotTexture(int selection)
    {
        if (selection < 0 || selection >= robotTextures.length)
            selection = 0;

        if (robotTextures[selection] == null)
            return playerTx;

        return robotTextures[selection];
    }
    public float getPlayerYaw()
    {
        if (cam == null)
            return 0.0f;

        Vector3f camN = cam.getN();

        float flatX = camN.x;
        float flatZ = camN.z;

        if (java.lang.Math.abs(flatX) < 0.0001f && java.lang.Math.abs(flatZ) < 0.0001f)
            return 0.0f;

        return (float)java.lang.Math.atan2(flatX, flatZ);
    }
    public int getCurrentWeaponIndex()
    {
        return weaponInventory.getCurrentWeapon().ordinal();
    }

    public ObjShape getWeaponShape(int weaponIndex)
    {
        if (weaponIndex < 0 || weaponIndex >= WeaponType.COUNT)
            weaponIndex = 0;

        WeaponType weapon = WeaponType.values()[weaponIndex];

        switch (weapon)
        {
            case KNIFE:
                return knifeS;
            case PISTOL:
                return pistolS;
            case PLASMA_RIFLE:
                return plasmaRifleS;
            case RIFLE:
                return rifleS;
            case SHOTGUN:
                return shotGunS;
            default:
                return pistolS;
        }
    }

    public TextureImage getWeaponTexture(int weaponIndex)
    {
        if (weaponIndex < 0 || weaponIndex >= WeaponType.COUNT)
            weaponIndex = 0;

        WeaponType weapon = WeaponType.values()[weaponIndex];

        switch (weapon)
        {
            case KNIFE:
                return knifeTx;
            case PISTOL:
                return pistolTx;
            case PLASMA_RIFLE:
                return plasmaRifleTx;
            case RIFLE:
                return rifleTx;
            case SHOTGUN:
                return shotGunTx;
            default:
                return pistolTx;
        }
    }

    public Vector3f getGhostWeaponOffset(int weaponIndex)
    {
        if (weaponIndex < 0 || weaponIndex >= WeaponType.COUNT)
            weaponIndex = 0;

        WeaponType weapon = WeaponType.values()[weaponIndex];

        switch (weapon)
        {
            case KNIFE:
                return new Vector3f(
                    weaponPos.x,
                    weaponPos.y,
                    weaponPos.z
                );

            case PISTOL:
                return new Vector3f(
                    weaponPos.x,
                    weaponPos.y,
                    weaponPos.z
                );

            case PLASMA_RIFLE:
                return new Vector3f(
                    weaponPos.x,
                    weaponPos.y,
                    weaponPos.z
                );

            case RIFLE:
                return new Vector3f(
                    weaponPos.x,
                    weaponPos.y,
                    weaponPos.z
                );

            case SHOTGUN:
                return new Vector3f(
                    weaponPos.x,
                    weaponPos.y - 0.25f,
                    weaponPos.z - 0.20f
                );

            default:
                return new Vector3f(
                    weaponPos.x,
                    weaponPos.y,
                    weaponPos.z
                );
        }
    }

    public float getGhostWeaponScale(int weaponIndex)
    {
        if (weaponIndex < 0 || weaponIndex >= WeaponType.COUNT)
            weaponIndex = 0;

        WeaponType weapon = WeaponType.values()[weaponIndex];

        switch (weapon)
        {
            case KNIFE:
                return knifeWeaponScale;

            case PISTOL:
                return weaponScale;

            case PLASMA_RIFLE:
                return weaponScale;

            case RIFLE:
                return weaponScale;

            case SHOTGUN:
                return weaponScale + 0.8f;

            default:
                return weaponScale;
        }
    }

    public Matrix4f getGhostWeaponOrientationCorrection(int weaponIndex)
    {
        if (weaponIndex < 0 || weaponIndex >= WeaponType.COUNT)
            weaponIndex = 0;

        WeaponType weapon = WeaponType.values()[weaponIndex];

        switch (weapon)
        {
            case KNIFE:
                return new Matrix4f()
                    .rotateY((float)java.lang.Math.toRadians(-90.0f))
                    .rotateZ((float)java.lang.Math.toRadians(25.0f));

            case RIFLE:
                return new Matrix4f()
                    .rotateX((float)java.lang.Math.toRadians(90.0f));

            case SHOTGUN:
                return new Matrix4f()
                    .rotateY((float)java.lang.Math.toRadians(90.0f))
                    .rotateX((float)java.lang.Math.toRadians(90.0f));

            default:
                return new Matrix4f();
        }
    }

    //-------------------------------
    //DEBUGGING
    private void debugStartFinalUfoBeam()
    {
        if (terr == null || playerP == null || ufoWaveManager == null)
        {
            System.out.println("DEBUG: cannot start UFO beam test yet; missing terrain/player physics/UFO manager");
            return;
        }

        mapSelection = 0;
        applyMapSelection();
        switchTerrainPhysics();

        // remove any active apes so final condition is clean
        for (int i = activeApes.size() - 1; i >= 0; i--)
            removeApe(i);

        // force beam from large UFO
        ufoWaveManager.debugPlaceLargeUfo();
        startTractorBeam();

        Vector3f beamPos = tractorBeamCenter;
        float y = terr.getHeight(beamPos.x, beamPos.z) + playerVisualYOffset + 1.0f;

        playerP.setLocation(new float[] {
            beamPos.x,
            y,
            beamPos.z
        });

        playerP.setLinearVelocity(new float[] { 0.0f, 0.0f, 0.0f });

        System.out.println("DEBUG: player moved under gameplay tractor beam");
    }
    private void debugStartLevelTwoArrivalEvent()
    {
        mapSelection = 1;
        applyMapSelection();
        switchTerrainPhysics();
        hideMapZeroBuildings();

        setupCaseOneStart();

        System.out.println("DEBUG: forced level two arrival event");
    }

    private void spawnDebugSkinnyLoadout()
    {
        if (player == null || skinnyS == null || plasmaRifleS == null || grappleGunS == null)
            return;

        Vector3f playerPos = player.getWorldLocation();

        Vector3f forward = new Vector3f(cam.getN()).normalize();

        Vector3f spawnPos = new Vector3f(playerPos)
            .add(new Vector3f(forward).mul(6.0f));

        spawnPos.y = 52.0f;

        debugSkinny = new GameObject(GameObject.root(), skinnyS, skinnyTx);
        debugSkinny.setLocalTranslation(new Matrix4f().translation(spawnPos));
        debugSkinny.setLocalScale(new Matrix4f().scaling(0.8f));
        debugSkinny.setLocalRotation(new Matrix4f().rotationY((float)java.lang.Math.toRadians(180.0f)));

        skinnyS.playAnimation("GRAPPLE", 0.3f, AnimatedShape.EndType.LOOP, 0);

        // right hand plasma rifle
        debugSkinnyPlasmaRifle = new GameObject(GameObject.root(), plasmaRifleS, plasmaRifleTx);
        debugSkinnyPlasmaRifle.setParent(debugSkinny);
        debugSkinnyPlasmaRifle.propagateTranslation(true);
        debugSkinnyPlasmaRifle.propagateRotation(true);
        debugSkinnyPlasmaRifle.propagateScale(true);
        debugSkinnyPlasmaRifle.applyParentRotationToPosition(true);

        debugSkinnyPlasmaRifle.setLocalTranslation(
            new Matrix4f().translation(-0.3f, 1.25f, 0.75f)
        );
        debugSkinnyPlasmaRifle.setLocalScale(new Matrix4f().scaling(weaponScale/75.0f));
        debugSkinnyPlasmaRifle.setLocalRotation(
            new Matrix4f().rotationY((float)java.lang.Math.toRadians(0.0f))
        );

        // left hand grapple gun, pointed upward
        debugSkinnyGrappleGun = new GameObject(GameObject.root(), grappleGunS, grappleGunTx);
        debugSkinnyGrappleGun.setParent(debugSkinny);
        debugSkinnyGrappleGun.propagateTranslation(true);
        debugSkinnyGrappleGun.propagateRotation(true);
        debugSkinnyGrappleGun.propagateScale(true);
        debugSkinnyGrappleGun.applyParentRotationToPosition(true);

        debugSkinnyGrappleGun.setLocalTranslation(
            new Matrix4f().translation(0.25f, 1.45f, 0.25f)
        );
        debugSkinnyGrappleGun.setLocalScale(new Matrix4f().scaling(0.03f));
        debugSkinnyGrappleGun.setLocalRotation(
            new Matrix4f()
                .rotationX((float)java.lang.Math.toRadians(45.0f))
        );

        System.out.println("DEBUG: spawned skinny weapon fit test");
    }
}
