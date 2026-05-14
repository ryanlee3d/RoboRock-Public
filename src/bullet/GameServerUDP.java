package bullet;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import java.util.UUID;

import org.joml.Vector3f;

import tage.networking.server.GameConnectionServer;
import tage.networking.server.IClientInfo;
import tage.networking.IGameConnection.ProtocolType;

public class GameServerUDP extends GameConnectionServer<UUID>
{
    // Tracks the latest known position for each connected client so late joiners
    // can be sent ghosts for players who were already in the session.
    private Map<UUID, Vector3f> clientPositions;
    private Map<UUID, Integer> clientAvatarSelections;
    public GameServerUDP(int localPort) throws IOException
    {
        super(localPort, ProtocolType.UDP);
        clientPositions = new java.util.concurrent.ConcurrentHashMap<>();
        clientAvatarSelections = new java.util.concurrent.ConcurrentHashMap<>();
        System.out.println("UDP server started on port " + localPort);
    }

    @Override
    public void processPacket(Object o, InetAddress senderIP, int sndPort)
    {
        String message = (String) o;
        String[] msgTokens = message.split(",");

        if (msgTokens.length > 0)
        {
            String command = msgTokens[0];

            // JOIN
            if (command.equals("join"))
            {
                try
                {
                    IClientInfo ci = getServerSocket().createClientInfo(senderIP, sndPort);
                    UUID clientID = UUID.fromString(msgTokens[1]);

                    addClient(ci, clientID);
                    sendJoinedMessage(clientID, true);
                    // Backfill any already-connected players to the newly joined client.
                    sendDetailsMessages(clientID);

                    System.out.println("Client joined: " + clientID);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }

            // CREATE
            if (command.equals("create"))
            {
                UUID clientID = UUID.fromString(msgTokens[1]);
                String[] pos = { msgTokens[2], msgTokens[3], msgTokens[4] };
                int avatarSelection = 0;

                if (msgTokens.length > 5)
                    avatarSelection = Integer.parseInt(msgTokens[5]);

                storeClientPosition(clientID, pos);
                clientAvatarSelections.put(clientID, avatarSelection);
                sendCreateMessages(clientID, pos, avatarSelection);
            }

            // MOVE
            if (command.equals("move"))
            {
                UUID clientID = UUID.fromString(msgTokens[1]);
                String[] pos = { msgTokens[2], msgTokens[3], msgTokens[4] };
                int avatarSelection = clientAvatarSelections.getOrDefault(clientID, 0);

                if (msgTokens.length > 5)
                    avatarSelection = Integer.parseInt(msgTokens[5]);

                storeClientPosition(clientID, pos);
                clientAvatarSelections.put(clientID, avatarSelection);
                sendMoveMessages(clientID, pos, avatarSelection);

            }

            // BYE
            if (command.equals("bye"))
            {
                UUID clientID = UUID.fromString(msgTokens[1]);

                clientPositions.remove(clientID);
                clientAvatarSelections.remove(clientID);
                sendByeMessages(clientID);
                removeClient(clientID);

                System.out.println("Client left: " + clientID);
            }
        }
    }

    private void storeClientPosition(UUID clientID, String[] pos)
    {
        clientPositions.put(clientID, new Vector3f(
                Float.parseFloat(pos[0]),
                Float.parseFloat(pos[1]),
                Float.parseFloat(pos[2])
        ));
    }

    public void sendJoinedMessage(UUID clientID, boolean success)
    {
        try
        {
            String message = "join," + (success ? "success" : "failure");
            sendPacket(message, clientID);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void sendCreateMessages(UUID clientID, String[] position, int avatarSelection)
    {
        try
        {
            String message = "create," + clientID +
                    "," + position[0] +
                    "," + position[1] +
                    "," + position[2] +
                    "," + avatarSelection;

            forwardPacketToAll(message, clientID);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void sendDetailsMessages(UUID clientID)
    {
        for (Map.Entry<UUID, Vector3f> entry : clientPositions.entrySet())
        {
            UUID remoteID = entry.getKey();
            if (remoteID.equals(clientID))
                continue;

            Vector3f pos = entry.getValue();
            try
            {
                // "GhostDetails" is used to handle ghosts that
                // existed before this client joined.
                int avatarSelection = clientAvatarSelections.getOrDefault(remoteID, 0);

                String message = "ghostDetails," + remoteID +
                        "," + pos.x() +
                        "," + pos.y() +
                        "," + pos.z() +
                        "," + avatarSelection;
                sendPacket(message, clientID);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public void sendMoveMessages(UUID clientID, String[] position, int avatarSelection)
    {
        try
        {
            String message = "move," + clientID +
                    "," + position[0] +
                    "," + position[1] +
                    "," + position[2] +
                    "," + avatarSelection;

            forwardPacketToAll(message, clientID);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void sendByeMessages(UUID clientID)
    {
        try
        {
            String message = "bye," + clientID;
            forwardPacketToAll(message, clientID);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
	
	public static void main(String[] args)
	{
		if (args.length < 1)
		{
			System.out.println("Usage: java bullet.GameServerUDP <port>");
			return;
		}

		int port = Integer.parseInt(args[0]);

		try
		{
			new GameServerUDP(port);
			System.out.println("Server running on port " + port);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
