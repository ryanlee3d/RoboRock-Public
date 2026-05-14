package bullet;

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

                ghostManager.createGhost(ghostID, pos, avatarSelection, yaw);
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

                ghostManager.updateGhostAvatar(ghostID, pos, avatarSelection, yaw);
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
                    "," + game.getPlayerYaw();

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
                    "," + game.getPlayerYaw();

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
