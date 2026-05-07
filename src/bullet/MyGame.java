package bullet;

import tage.*;
import tage.shapes.*;
import tage.input.*;
import tage.input.action.*;

import java.awt.*;
import java.awt.event.*;
import org.joml.*;
import org.joml.Math;

// networking imports
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import tage.networking.IGameConnection.ProtocolType;

// physics imports
import tage.physics.PhysicsEngine;
import tage.physics.PhysicsObject;

// behavior tree imports
import tage.ai.behaviortrees.*;

public class MyGame extends VariableFrameRateGame implements MouseMotionListener, MouseListener
{
    private static Engine engine;

    private boolean isShuttingDown = false;
    private GameState gameState = GameState.MENU;
    private int menuSelection = 0;
    private final MainMenu menu = new MainMenu();

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

    private double lastFrameTime, currFrameTime, elapsTime;
    private IAction restartGame;

    // game objects
    private GameObject player, skinny, ape, knife, pistol, plasmaRifle, rifle, shotGun, apePlasmaRifle, terr, centerBuilding;

    // instances of game objects for repeat use
    private GameObject[] smallBuildings = new GameObject[8];
    private GameObject[] smallBuildings2 = new GameObject[8];

    // shapes for animated objects
    private AnimatedShape playerS, skinnyS, apeS;

    // player animation values
    private boolean isMoving = false;
    private boolean wasMoving = false;
    private Vector3f prevPlayerPos = new Vector3f(0, 0, 0);

    // player stats
    private int pHealth = 100;

    private final int pHealthMin = 0;
    private final int pHealthMax = 150;
    private int playerCredits = 0;

    private final WeaponInventory weaponInventory = new WeaponInventory();

    // shapes and textures for game objects
    private ObjShape ammoS, terrS, healthS, plasmaRifleS, rifleS, shotGunS, knifeS, pistolS, smallBuildingS, smallBuilding2S, centerBuildingS, ufoS;

    private TextureImage playerTx, terrTxMap0, terrTxMap1, ammoTx, healthTx, plasmaRifleTx, rifleTx, shotGunTx, knifeTx, pistolTx,
        heightMap0, heightMap1, skinnyTx, apeTx, smallBuildingTx, smallBuilding2Tx, centerBuildingTx, ufoTx;

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

    // lighting
    private Light mainLight;

    // physics
    private PhysicsEngine physicsEngine;
    private PhysicsObject playerP, terrainP;
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
    private boolean isClientConnected = false;

    // skyboxes
    private int spaceSkyBox, islandSkyBox, lushSkyBox, plainsSkyBox;

    // map selection
    private int mapSelection = 0;

    // ghost rendering
    private AnimatedShape ghostS;
    private TextureImage ghostT;

    // bullet system
    private ObjShape bulletSphereS;
    private TextureImage bulletYellowTx;
    private TextureImage bulletBlueTx;
    private final BulletManager bulletManager = new BulletManager(this);

    private boolean isFiring = false;

    // apes
    private java.util.ArrayList<GameObject> activeApes = new java.util.ArrayList<>();
    private java.util.ArrayList<PhysicsObject> activeApePhysics = new java.util.ArrayList<>();
    private java.util.ArrayList<Integer> activeApeHealth = new java.util.ArrayList<>();
    private java.util.ArrayList<Boolean> activeApeDead = new java.util.ArrayList<>();
    private java.util.ArrayList<Float> activeApeDeathTimers = new java.util.ArrayList<>();
    private java.util.ArrayList<Float> activeApeThinkTimers = new java.util.ArrayList<>();
    private java.util.ArrayList<Float> activeApeFireCooldowns = new java.util.ArrayList<>();
    private java.util.ArrayList<Float> activeApeStrafeDirs = new java.util.ArrayList<>();
    private java.util.ArrayList<BehaviorTree> activeApeTrees = new java.util.ArrayList<>();

    private float apeThinkTimer = 0.0f;
    private final float apeThinkInterval = 0.25f;

    private final float worldGravity = -9.8f;
    private final float shotgunSpread = 0.12f;
    private final int shotgunPelletCount = 6;

    public MyGame(String serverAddress, int serverPort, String protocol)
    {
        super();
        gm = new GhostManager(this);
        ufoWaveManager = new UfoWaveManager(this::spawnApe);
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        serverProtocol = protocol.toUpperCase().compareTo("TCP") == 0 ? ProtocolType.TCP : ProtocolType.UDP;
    }

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
            System.out.println("missing protocol host");
        else
            protClient.sendJoinMessage();
    }

    protected void processNetworking(float elapsTime)
    {
        if (protClient != null)
            protClient.processPackets();
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
        if (terr == null) return;

        if (centerBuilding != null) snapObjectToTerrain(centerBuilding, 0.0f);

        for (int i = 0; i < smallBuildings.length; i++)
            if (smallBuildings[i] != null) snapObjectToTerrain(smallBuildings[i], 0.0f);

        for (int i = 0; i < smallBuildings2.length; i++)
            if (smallBuildings2[i] != null) snapObjectToTerrain(smallBuildings2[i], 0.0f);

        if (skinny != null) snapObjectToTerrain(skinny, 0.0f);
        if (ape != null) snapObjectToTerrain(ape, 0.0f);
    }

    private void updatePlayerVisibilityForCameraMode()
    {
        if (player == null) return;

        if (firstPersonMode)
            player.setLocalScale(new Matrix4f().scaling(0.0001f));
        else
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
        GameObject ape = activeApes.get(index);
        PhysicsObject apeP = activeApePhysics.get(index);

        if (ape != null)
            ape.setLocalScale(new Matrix4f().scaling(0.0001f));

        if (apeP != null)
            physicsEngine.removeObject(apeP.getUID());

        activeApes.remove(index);
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

    private void updateApeAI(float dt)
    {
        if (player == null) return;

        Vector3f playerPos = player.getWorldLocation();

        for (int i = 0; i < activeApes.size(); i++)
        {
            if (activeApeDead.get(i)) continue;

            GameObject apeObj = activeApes.get(i);
            PhysicsObject apePhys = activeApePhysics.get(i);

            if (apeObj == null || apePhys == null) continue;

            Vector3f apePos = apePhys.getLocation();
            Vector3f toPlayer = new Vector3f(playerPos).sub(apePos);
            float dist = toPlayer.length();

            if (dist < 0.001f) continue;
            toPlayer.normalize();

            // face player
            float yaw = (float)java.lang.Math.atan2(toPlayer.x, toPlayer.z);
            apeObj.setLocalRotation(new Matrix4f().rotationY(yaw));

            // circle player while staying in combat range
            float preferredMin = 8.0f;
            float preferredMax = 18.0f;
            float moveSpeed = 3.0f;
            float strafeDir = activeApeStrafeDirs.get(i);

            Vector3f moveDir = new Vector3f();

            // move toward player
            if (dist > preferredMax)
            {
                moveDir.set(toPlayer.x, 0.0f, toPlayer.z);
            }
            // back away a bit
            else if (dist < preferredMin)
            {
                moveDir.set(-toPlayer.x, 0.0f, -toPlayer.z);
            }
            // strafe around player
            else
            {
                moveDir.set(-toPlayer.z * strafeDir, 0.0f, toPlayer.x * strafeDir);
            }

            if (moveDir.lengthSquared() > 0.0001f)
            {
                moveDir.normalize();
                apePhys.setLinearVelocity(new float[]
                {
                    moveDir.x * moveSpeed,
                    apePhys.getLinearVelocity()[1],
                    moveDir.z * moveSpeed
                });
            }

            // occasionally change strafe direction
            float think = activeApeThinkTimers.get(i) + dt;
            if (think >= 1.0f)
            {
                think = 0.0f;
                if (Math.random() < 0.25)
                {
                    activeApeStrafeDirs.set(i, -activeApeStrafeDirs.get(i));
                }
            }
            activeApeThinkTimers.set(i, think);

            // fire cooldown
            float fireCd = activeApeFireCooldowns.get(i) - dt;
            if (fireCd <= 0.0f && dist <= 25.0f)
            {
                Vector3f fireDir = new Vector3f(playerPos)
                    .add(0.0f, 1.0f, 0.0f)
                    .sub(apePos.x, apePos.y + 1.5f, apePos.z)
                    .normalize();

                Vector3f muzzlePos = new Vector3f(apePos.x, apePos.y + 1.5f, apePos.z)
                    .add(new Vector3f(fireDir).mul(1.2f));

                spawnEnemyBullet(muzzlePos, fireDir, true);
                fireCd = 1.25f;   // ape fire rate
            }
            activeApeFireCooldowns.set(i, fireCd);
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

    private class PlayerAliveCondition extends BTCondition
    {
        public PlayerAliveCondition()
        {
            super(false);
        }

        protected boolean check()
        {
            return player != null && pHealth > 0;
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
            if (player == null || apeObj == null || apePhys == null)
                return BTStatus.BH_FAILURE;

            Vector3f playerPos = player.getWorldLocation();
            Vector3f apePos = apePhys.getLocation();

            Vector3f toPlayer = new Vector3f(playerPos).sub(apePos);
            if (toPlayer.length() < 0.001f)
                return BTStatus.BH_FAILURE;

            toPlayer.normalize();

            float yaw = (float)java.lang.Math.atan2(toPlayer.x, toPlayer.z);
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
            if (i < 0 || player == null || apePhys == null)
                return BTStatus.BH_FAILURE;

            Vector3f playerPos = player.getWorldLocation();
            Vector3f apePos = apePhys.getLocation();

            Vector3f toPlayer = new Vector3f(playerPos).sub(apePos);
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
            if (i < 0 || player == null || apePhys == null)
                return BTStatus.BH_FAILURE;

            Vector3f playerPos = player.getWorldLocation();
            Vector3f apePos = apePhys.getLocation();

            float dist = new Vector3f(playerPos).sub(apePos).length();

            float fireCd = activeApeFireCooldowns.get(i) - elapsedTime;

            if (fireCd <= 0.0f && dist <= 25.0f)
            {
                Vector3f fireDir = new Vector3f(playerPos)
                    .add(0.0f, 1.0f, 0.0f)
                    .sub(apePos.x, apePos.y + 1.5f, apePos.z)
                    .normalize();

                Vector3f muzzlePos = new Vector3f(apePos.x, apePos.y + 1.5f, apePos.z)
                    .add(new Vector3f(fireDir).mul(1.2f));

                spawnEnemyBullet(muzzlePos, fireDir, true);

                gameAudio.playApePlasma(muzzlePos);

                fireCd = 1.25f;
            }

            activeApeFireCooldowns.set(i, fireCd);

            return BTStatus.BH_SUCCESS;
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

        bulletYellowTx = new TextureImage("bullet.jpg");
        bulletBlueTx = new TextureImage("plasmaBullet.jpg");

        ufoTx = new TextureImage("ufo.png");
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
        apePlasmaRifle.setLocalTranslation(new Matrix4f().translation(-0.05f, 1.5f, 0.7f));
        apePlasmaRifle.setLocalRotation(new Matrix4f().rotationY((float)java.lang.Math.toRadians(0.0f)));
        apePlasmaRifle.setLocalScale(new Matrix4f().scaling(0.5f));
        apePlasmaRifle.setParent(ape);
        apePlasmaRifle.propagateTranslation(true);
        apePlasmaRifle.propagateRotation(true);
        apePlasmaRifle.propagateScale(true);
        apePlasmaRifle.applyParentRotationToPosition(true);

        pickupManager.buildObjects(ammoS, ammoTx, healthS, healthTx);

        knife = new GameObject(GameObject.root(), knifeS, knifeTx);
        knife.setLocalTranslation(new Matrix4f().translation(weaponPos.x, weaponPos.y, weaponPos.z));
        knife.setLocalRotation(new Matrix4f().rotationY((float)java.lang.Math.toRadians(weaponRotY)));
        knife.setLocalScale(new Matrix4f().scaling(weaponScale));
        attachWeaponToPlayer(knife);

        pistol = new GameObject(GameObject.root(), pistolS, pistolTx);
        pistol.setLocalTranslation(new Matrix4f().translation(weaponPos.x, weaponPos.y, weaponPos.z));
        pistol.setLocalRotation(new Matrix4f().rotationY((float)java.lang.Math.toRadians(weaponRotY)));
        pistol.setLocalScale(new Matrix4f().scaling(weaponScale));
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

        weaponInventory.reset();
        isFiring = false;
        updateWeaponVisibility();

        centerBuilding = new GameObject(GameObject.root(), centerBuildingS, centerBuildingTx);
        centerBuilding.setLocalTranslation(new Matrix4f().translation(0.0f, 0.0f, 0.0f));
        centerBuilding.setLocalScale(new Matrix4f().scaling(1.5f));

        float[][] sbPositions = {
            {-52.0f, 72.0f}, {48.0f, -65.0f}, {79.0f, -21.0f}, {-83.0f, 39.0f},
            {-18.0f, 40.0f}, {20.0f, -38.0f}, {24.0f, 5.0f}, {28.0f, 38.0f}
        };

        float[][] sb2Positions = {
            {78.0f, -37.0f}, {-79.0f, 16.0f}, {-50.0f, 51.0f}, {-40.0f, 22.0f},
            {-24.0f, -18.0f}, {-5.0f, 35.0f}, {10.0f, -42.0f}, {44.0f, -10.0f}
        };

        ufoWaveManager.buildObjects(ufoS, ufoTx);

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

		TextureImage activeHeightMap = (mapSelection == 1) ? heightMap1 : heightMap0;

		terrainP = (engine.getSceneGraph()).addPhysicsStaticTerrainMesh(
			loc, rot, activeHeightMap, 100.0f, 50.0f, 100
		);
		terrainP.setBounciness(0.0f);
		terrainP.setFriction(1.0f);
		terrainP.disableSleeping();
		terr.setPhysicsObject(terrainP);

        bulletManager.setPhysicsEngine(physicsEngine);

        engine.enableGraphicsWorldRender();
        if (physicsDebug)
            engine.enablePhysicsWorldRender();
        else
            engine.disablePhysicsWorldRender();
    }

    @Override
    public void initializeGame()
    {
        System.out.println("=== initializeGame() reached ===");

        createViewports();

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
        AbstractInputAction elevUp = overheadCameraController.createElevationUpAction();
        AbstractInputAction elevDown = overheadCameraController.createElevationDownAction();
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
        im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key._1, elevUp,
            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key._2, elevDown,
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

        setupNetworking();
        initAudio();
        setEarParameters();
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
            return;
        }

        lastFrameTime = currFrameTime;
        currFrameTime = System.currentTimeMillis();
        float dt = (float)((currFrameTime - lastFrameTime) / 1000.0);
        elapsTime += dt;

        cam = engine.getRenderSystem().getViewport("MAIN").getCamera();
        setEarParameters();
        
        currentMoveDir.set(0, 0, 0);
        im.update(dt);

        if (playerP != null)
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

        processNetworking(dt);

        weaponInventory.updateTimers(dt);
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

        updateApeBehaviorTrees(dt);

        updateApesFromPhysics();
        ufoWaveManager.update(dt);
        updateDeadApes(dt);

        bulletManager.update(dt);

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

        // if no apes alive, trigger next UFO
        if (!ufoWaveManager.isActive() && activeApes.size() == 0)
        {
            ufoWaveManager.startNextWave();
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

        engine.getHUDmanager().setHUD1("Health: " + pHealth + " | Credits: $" + playerCredits, new Vector3f(0, 1, 0), 15, 660);
        engine.getHUDmanager().setHUD2(weaponInventory.getHudText(), new Vector3f(1, 1, 1), 15, 630);
        engine.getHUDmanager().setHUD3(
            String.format("Player Pos: X(%.2f) Y(%.2f) Z(%.2f)", playerpos.x, playerpos.y, playerpos.z),
            new Vector3f(1, 1, 1), 15, 15
        );

        pickupManager.update(dt, terr);
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
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
                    if (menu.getSelectedIndex() == 1)
                    {
                        menu.previousMap();
                        setMapSelection(menu.getSelectedMapIndex());
                        applyMapSelection();
                    }
                    break;
                case KeyEvent.VK_RIGHT:
                    if (menu.getSelectedIndex() == 1)
                    {
                        menu.nextMap();
                        setMapSelection(menu.getSelectedMapIndex());
                        applyMapSelection();
                    }
                    break;
                case KeyEvent.VK_ENTER:
                    switch (menu.activateSelection())
                    {
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
                        case OPTIONS:
                            break;
                        case QUIT:
                            isShuttingDown = true;
                            mouseModeInitiated = false;
                            isRecentering = false;
                            gameAudio.releaseAll();
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

    @Override
    public void mouseReleased(MouseEvent e)
    {
        if (e.getButton() == MouseEvent.BUTTON1)
        {
            isFiring = false;
            gameAudio.stopRifleLoopSound();
        }
    }

    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override
    public void mouseDragged(MouseEvent e)
    {
        handleMouseLook(e);
    }

    void toggleFirstPersonMode()
    {
        firstPersonMode = !firstPersonMode;
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
        if (playerP == null) return;

        Vector3f dir = new Vector3f(moveDir.x, 0f, moveDir.z);
        if (dir.lengthSquared() < 0.000001f) return;
        dir.normalize();

        currentMoveDir.add(dir);
        currentMoveSpeed = speed;
    }

    public void stopPlayerHorizontalMotion(){};

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

        Vector3f forward = new Vector3f(cam.getN()).mul(-1.0f).normalize();
        Vector3f playerPos = player.getWorldLocation();
        Vector3f spawnPos = new Vector3f(playerPos)
            .add(0.0f, 1.5f, 0.0f)
            .add(new Vector3f(forward).mul(1.5f));

        switch (currentWeapon)
        {
            case PISTOL:
                bulletManager.spawnPlayerBullet(spawnPos, forward, false);
                weaponInventory.consumeCurrentRound();
                gameAudio.playPistolShot();
                break;

            case PLASMA_RIFLE:
                if (weaponInventory.isPlasmaBurstMode())
                {
                    int burstCount = java.lang.Math.min(3, weaponInventory.getCurrentMagazineAmmo());
                    for (int i = 0; i < burstCount; i++)
                    {
                    bulletManager.spawnPlayerBullet(spawnPos, forward, true);
                        weaponInventory.consumeCurrentRound();
                    }
                }
                else
                {
                bulletManager.spawnPlayerBullet(spawnPos, forward, true);
                    weaponInventory.consumeCurrentRound();
                }

                gameAudio.playPlasmaRifle();
                break;

            case RIFLE:
                bulletManager.spawnPlayerBullet(spawnPos, forward, false);
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

                bulletManager.spawnPlayerBullet(spawnPos, spreadDir, false);
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

    public void spawnEnemyBullet(Vector3f spawnPos, Vector3f dir, boolean isPlasma)
    {
        bulletManager.spawnEnemyBullet(spawnPos, dir, isPlasma);
    }

    public boolean checkAndDamageApe(Vector3f loc)
    {
        for (int j = activeApes.size() - 1; j >= 0; j--)
        {
            GameObject ape = activeApes.get(j);
            Vector3f apePos = ape.getWorldLocation();

            if (loc.distance(apePos) < 1.0f)
            {
                if (!activeApeDead.get(j))
                {
                    int hp = activeApeHealth.get(j) - 100;
                    activeApeHealth.set(j, hp);

                    if (hp <= 0)
                    {
                        activeApeDead.set(j, true);
                        activeApeDeathTimers.set(j, 2.0f);

                        PhysicsObject apeP = activeApePhysics.get(j);
                        if (apeP != null)
                        {
                            apeP.setAngularFactor(1f);
                            apeP.applyTorque(0.0f, 0.0f, 35.0f);
                        }
                        addPlayerCredits(new Random().nextInt(10)); // Reward for kill
                    }
                    return true;
                }
            }
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

            apeObj.getRenderStates().setModelOrientationCorrection(
                new Matrix4f()
                    .rotationX((float)Math.toRadians(90.0f))
                    .rotateZ((float)Math.toRadians(180.0f))
            );
        }
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
                break;
            default:
                terr.setTextureImage(terrTxMap0);
                terr.setHeightMap(heightMap0);
                terr.getRenderStates().setTileFactor(10);
                break;
        }
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
        pHealth = java.lang.Math.max(pHealthMin, java.lang.Math.min(value, pHealthMax));
    }
    public void addPlayerHealth(int amount) { setPlayerHealth(pHealth + amount); }
    public int getPlayerHealth() { return pHealth; }
    public int getPlayerHealthMax() { return pHealthMax; }
    public int getPlayerAmmo() { return weaponInventory.getTotalAmmo(); }
    public void addPlayerCredits(int amount) { playerCredits += amount; }
    public int getPlayerCredits() { return playerCredits; }

    // --- Networking & Multiplayer ---
    public ProtocolClient getProtocolClient() { return protClient; }
    public void setIsConnected(boolean value) { isClientConnected = value; }
    public GhostManager getGhostManager() { return gm; }
    public ObjShape getGhostShape() { return ghostS; }
    public TextureImage getGhostTexture() { return ghostT; }
}
