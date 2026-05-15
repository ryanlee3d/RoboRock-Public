package bullet.network;
import bullet.MyGame;
import bullet.managers.GhostManager;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

import org.joml.Vector3f;

import tage.networking.client.GameConnectionClient;
import tage.networking.IGameConnection.ProtocolType;

public class ProtocolClient extends GameConnectionClient
{
    private MyGame game;
    private UUID id;
    private GhostManager ghostManager;

    public ProtocolClient(InetAddress remAddr, int remPort,
                          ProtocolType pType, MyGame game) throws IOException
    {
        super(remAddr, remPort, pType);
        this.game = game;
        this.id = UUID.randomUUID();
        this.ghostManager = game.getGhostManager();
    }

    @Override
    protected void processPacket(Object msg)
    {
        String strMessage = (String) msg;
        String[] tokens = strMessage.split(",");

        if (tokens.length > 0)
        {
            String command = tokens[0];

            // JOIN RESPONSE
            if (command.equals("join"))
            {
                if (tokens[1].equals("success"))
                {
                    game.setIsConnected(true);
                    sendCreateMessage(game.getPlayerPosition());
                }
                else
                {
                    game.setIsConnected(false);
                }
            }

            // HOST ASSIGNMENT
            if (command.equals("host"))
            {
                boolean isHost = Boolean.parseBoolean(tokens[1]);
                game.setIsHostClient(isHost);
                System.out.println("Host client status: " + isHost);
            }

            // CREATE or DETAILS
            if (command.equals("create") || command.equals("ghostDetails") || command.equals("GhostDetails"))
            {
                UUID ghostID = UUID.fromString(tokens[1]);
                if (ghostID.equals(id)) return;

                Vector3f pos = new Vector3f(
                        Float.parseFloat(tokens[2]),
                        Float.parseFloat(tokens[3]),
                        Float.parseFloat(tokens[4])
                );

                int avatarSelection = 0;
                if (tokens.length > 5)
                    avatarSelection = Integer.parseInt(tokens[5]);
                float yaw = 0.0f;
                if (tokens.length > 6)
                    yaw = Float.parseFloat(tokens[6]);
                int weaponIndex = 0;
                if (tokens.length > 7)
                    weaponIndex = Integer.parseInt(tokens[7]);

                ghostManager.createGhost(ghostID, pos, avatarSelection, yaw, weaponIndex);
            }

            // MOVE
            if (command.equals("move"))
            {
                UUID ghostID = UUID.fromString(tokens[1]);
                if (ghostID.equals(id)) return;

                Vector3f pos = new Vector3f(
                        Float.parseFloat(tokens[2]),
                        Float.parseFloat(tokens[3]),
                        Float.parseFloat(tokens[4])
                );

                int avatarSelection = 0;
                if (tokens.length > 5)
                    avatarSelection = Integer.parseInt(tokens[5]);
                float yaw = 0.0f;
                if (tokens.length > 6)
                    yaw = Float.parseFloat(tokens[6]);
                int weaponIndex = 0;
                if (tokens.length > 7)
                    weaponIndex = Integer.parseInt(tokens[7]);

                ghostManager.updateGhostAvatar(ghostID, pos, avatarSelection, yaw, weaponIndex);
            }

            // ENEMY UPDATE
            if (command.equals("enemyUpdate"))
            {
                UUID senderID = UUID.fromString(tokens[1]);

                if (senderID.equals(id))
                    return;

                int enemyID = Integer.parseInt(tokens[2]);
                String enemyType = tokens[3];

                Vector3f pos = new Vector3f(
                        Float.parseFloat(tokens[4]),
                        Float.parseFloat(tokens[5]),
                        Float.parseFloat(tokens[6])
                );

                float yaw = Float.parseFloat(tokens[7]);
                int health = Integer.parseInt(tokens[8]);
                boolean dead = Boolean.parseBoolean(tokens[9]);

                game.receiveNetworkEnemyUpdate(enemyID, enemyType, pos, yaw, health, dead);
            }

            // ENEMY REMOVE
            if (command.equals("enemyRemove"))
            {
                UUID senderID = UUID.fromString(tokens[1]);

                if (senderID.equals(id))
                    return;

                int enemyID = Integer.parseInt(tokens[2]);
                String enemyType = tokens[3];

                game.receiveNetworkEnemyRemove(enemyID, enemyType);
            }

            // ENEMY BULLET
            if (command.equals("enemyBullet"))
            {
                UUID senderID = UUID.fromString(tokens[1]);

                if (senderID.equals(id))
                    return;

                Vector3f pos = new Vector3f(
                        Float.parseFloat(tokens[2]),
                        Float.parseFloat(tokens[3]),
                        Float.parseFloat(tokens[4])
                );

                Vector3f dir = new Vector3f(
                        Float.parseFloat(tokens[5]),
                        Float.parseFloat(tokens[6]),
                        Float.parseFloat(tokens[7])
                );

                boolean isPlasma = Boolean.parseBoolean(tokens[8]);
                game.receiveNetworkEnemyBullet(pos, dir, isPlasma);
            }

            // PLAYER BULLET
            if (command.equals("playerBullet"))
            {
                UUID senderID = UUID.fromString(tokens[1]);

                if (senderID.equals(id))
                    return;

                Vector3f pos = new Vector3f(
                        Float.parseFloat(tokens[2]),
                        Float.parseFloat(tokens[3]),
                        Float.parseFloat(tokens[4])
                );

                Vector3f dir = new Vector3f(
                        Float.parseFloat(tokens[5]),
                        Float.parseFloat(tokens[6]),
                        Float.parseFloat(tokens[7])
                );

                boolean isPlasma = Boolean.parseBoolean(tokens[8]);
                int enemyDamage = tokens.length > 9 ? Integer.parseInt(tokens[9]) : 100;
                int brainDamage = tokens.length > 10 ? Integer.parseInt(tokens[10]) : 20;
                game.receiveNetworkPlayerBullet(senderID, pos, dir, isPlasma, enemyDamage, brainDamage);
            }

            // CREDIT AWARD
            if (command.equals("creditAward"))
            {
                UUID targetID = UUID.fromString(tokens[2]);
                int amount = Integer.parseInt(tokens[3]);

                if (targetID.equals(id))
                    game.receiveCreditAward(amount);
            }

            // UFO WAVE START
            if (command.equals("ufoWaveStart"))
            {
                UUID senderID = UUID.fromString(tokens[1]);

                if (senderID.equals(id))
                    return;

                Vector3f pos = new Vector3f(
                        Float.parseFloat(tokens[2]),
                        Float.parseFloat(tokens[3]),
                        Float.parseFloat(tokens[4])
                );

                int apeCount = Integer.parseInt(tokens[5]);
                int ufoIndex = Integer.parseInt(tokens[6]);

                game.receiveNetworkUfoWaveStart(pos, apeCount, ufoIndex);
            }

            // TRACTOR BEAM START
            if (command.equals("tractorBeamStart"))
            {
                UUID senderID = UUID.fromString(tokens[1]);

                if (senderID.equals(id))
                    return;

                Vector3f ufoPos = new Vector3f(
                        Float.parseFloat(tokens[2]),
                        Float.parseFloat(tokens[3]),
                        Float.parseFloat(tokens[4])
                );

                game.receiveNetworkTractorBeamStart(ufoPos);
            }

            // GRAPPLE DROP
            if (command.equals("grappleDrop"))
            {
                UUID senderID = UUID.fromString(tokens[1]);

                if (senderID.equals(id))
                    return;

                Vector3f pos = new Vector3f(
                        Float.parseFloat(tokens[2]),
                        Float.parseFloat(tokens[3]),
                        Float.parseFloat(tokens[4])
                );

                game.receiveNetworkGrappleDrop(pos);
            }

            // GRAPPLE TAKEN
            if (command.equals("grappleTaken"))
            {
                UUID senderID = UUID.fromString(tokens[1]);

                if (senderID.equals(id))
                    return;

                game.receiveNetworkGrappleTaken();
            }

            // BYE
            if (command.equals("bye"))
            {
                UUID ghostID = UUID.fromString(tokens[1]);
                // Ignore our own bye because the local avatar is not managed as a ghost.
                if (ghostID.equals(id)) return;
                ghostManager.removeGhostAvatar(ghostID);
            }
        }
    }

    public void sendJoinMessage()
    {
        try
        {
            sendPacket("join," + id.toString());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void sendCreateMessage(Vector3f pos)
    {
        try
        {
            String msg = "create," + id +
                    "," + pos.x() +
                    "," + pos.y() +
                    "," + pos.z() +
                    "," + game.getAvatarSelection() +
                    "," + game.getPlayerYaw() +
                    "," + game.getCurrentWeaponIndex();

            sendPacket(msg);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void sendMoveMessage(Vector3f pos)
    {
        try
        {
            String msg = "move," + id +
                    "," + pos.x() +
                    "," + pos.y() +
                    "," + pos.z() +
                    "," + game.getAvatarSelection() +
                    "," + game.getPlayerYaw() +
                    "," + game.getCurrentWeaponIndex();

            sendPacket(msg);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void sendEnemyUpdate(int enemyID, String enemyType, Vector3f pos, float yaw, int health, boolean dead)
    {
        try
        {
            String msg = "enemyUpdate," + id +
                    "," + enemyID +
                    "," + enemyType +
                    "," + pos.x() +
                    "," + pos.y() +
                    "," + pos.z() +
                    "," + yaw +
                    "," + health +
                    "," + dead;

            sendPacket(msg);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void sendEnemyRemove(int enemyID, String enemyType)
    {
        try
        {
            String msg = "enemyRemove," + id +
                    "," + enemyID +
                    "," + enemyType;

            sendPacket(msg);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void sendEnemyBullet(Vector3f pos, Vector3f dir, boolean isPlasma)
    {
        try
        {
            String msg = "enemyBullet," + id +
                    "," + pos.x() +
                    "," + pos.y() +
                    "," + pos.z() +
                    "," + dir.x() +
                    "," + dir.y() +
                    "," + dir.z() +
                    "," + isPlasma;

            sendPacket(msg);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void sendPlayerBullet(Vector3f pos, Vector3f dir, boolean isPlasma)
    {
        sendPlayerBullet(pos, dir, isPlasma, 100, 20);
    }

    public void sendPlayerBullet(Vector3f pos, Vector3f dir, boolean isPlasma, int enemyDamage, int brainDamage)
    {
        try
        {
            String msg = "playerBullet," + id +
                    "," + pos.x() +
                    "," + pos.y() +
                    "," + pos.z() +
                    "," + dir.x() +
                    "," + dir.y() +
                    "," + dir.z() +
                    "," + isPlasma +
                    "," + enemyDamage +
                    "," + brainDamage;

            sendPacket(msg);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void sendCreditAward(UUID targetID, int amount)
    {
        try
        {
            String msg = "creditAward," + id +
                    "," + targetID +
                    "," + amount;

            sendPacket(msg);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void sendUfoWaveStart(Vector3f pos, int apeCount, int ufoIndex)
    {
        try
        {
            String msg = "ufoWaveStart," + id +
                    "," + pos.x() +
                    "," + pos.y() +
                    "," + pos.z() +
                    "," + apeCount +
                    "," + ufoIndex;

            sendPacket(msg);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void sendTractorBeamStart(Vector3f ufoPos)
    {
        try
        {
            String msg = "tractorBeamStart," + id +
                    "," + ufoPos.x() +
                    "," + ufoPos.y() +
                    "," + ufoPos.z();

            sendPacket(msg);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void sendGrappleDrop(Vector3f pos)
    {
        try
        {
            String msg = "grappleDrop," + id +
                    "," + pos.x() +
                    "," + pos.y() +
                    "," + pos.z();

            sendPacket(msg);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void sendGrappleTaken()
    {
        try
        {
            String msg = "grappleTaken," + id;
            sendPacket(msg);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void sendByeMessage()
    {
        try
        {
            sendPacket("bye," + id.toString());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
