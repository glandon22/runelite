package net.runelite.client.plugins.statussocket.data.player;

// Packet including most relevant information related to the client player.
// Used both for inventory & combat packets (attacking&defending)
public class PlayerDataPacket
{
	public String packetType;
	public String playerName;
	public int tick;
	public int runEnergy;
	public int specialAttack;



	public InventoryPacket[] inventory;

	public SkillPacket[] skills;

	public LocalPointPacket localPoint;
	public WorldPointPacket worldPoint;
	public CameraPacket camera;

}
