package zmaster587.advancedRocketry.network;

import java.io.IOException;
import java.util.logging.Logger;

import zmaster587.advancedRocketry.api.stations.ISpaceObject;
import zmaster587.advancedRocketry.api.stations.SpaceObjectManager;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.dimension.DimensionProperties;
import zmaster587.advancedRocketry.stations.SpaceObject;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.util.ForgeDirection;

public class PacketSpaceStationInfo extends BasePacket {
	SpaceObject spaceObject;
	int stationNumber;

	public PacketSpaceStationInfo() {}

	public PacketSpaceStationInfo(int stationNumber, ISpaceObject spaceObject) {
		this.spaceObject = (SpaceObject)spaceObject;
		this.stationNumber = stationNumber;
	}

	@Override
	public void write(ByteBuf out) {
		NBTTagCompound nbt = new NBTTagCompound();
		out.writeInt(stationNumber);
		boolean flag = false; //TODO //dimProperties == null;
		
		if(!flag) {
			
			//Try to send the nbt data of the dimension to the client, if it fails(probably due to non existent Biome ids) then remove the dimension
			try {
				spaceObject.getProperties().writeToNBT(nbt);
				PacketBuffer packetBuffer = new PacketBuffer(out);
				out.writeBoolean(false);
				//TODO: error handling
				try {
					packetBuffer.writeNBTTagCompoundToBuffer(nbt);
					out.writeInt(spaceObject.getForwardDirection().ordinal());
				} catch (IOException e) {
					e.printStackTrace();
				}
			} catch(NullPointerException e) {
				out.writeBoolean(true);
				Logger.getLogger("advancedRocketry").warning("Dimension " + stationNumber + " has thrown an exception trying to write NBT, deleting!");
				DimensionManager.getInstance().deleteDimension(stationNumber);
			}

		}
		else
			out.writeBoolean(flag);

	}

	@Override
	public void readClient(ByteBuf in) {
		PacketBuffer packetBuffer = new PacketBuffer(in);
		NBTTagCompound nbt;
		stationNumber = in.readInt();

		//Is dimention being deleted
		if(in.readBoolean()) {
			if(DimensionManager.getInstance().isDimensionCreated(stationNumber)) {
				DimensionManager.getInstance().deleteDimension(stationNumber);
			}
		}
		else {
			//TODO: error handling
			int direction;
			try {
				nbt = packetBuffer.readNBTTagCompoundFromBuffer();
				
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			
			direction = in.readInt();
			
			ISpaceObject iObject = SpaceObjectManager.getSpaceManager().getSpaceStation(stationNumber);
			//TODO: interface
			spaceObject = (SpaceObject)iObject;
			
			//Station needs to be created
			if( iObject == null ) {
				SpaceObject object = new SpaceObject();
				object.setProperties(DimensionProperties.createFromNBT(stationNumber, nbt));
				
				object.setForwardDirection(ForgeDirection.getOrientation(direction));
				
				SpaceObjectManager.getSpaceManager().registerSpaceObjectClient(object, object.getOrbitingPlanetId(), stationNumber);
			}
			else {
				iObject.setProperties(DimensionProperties.createFromNBT(stationNumber, nbt));
				spaceObject.setForwardDirection(ForgeDirection.getOrientation(direction));
			}
		}
	}

	@Override
	public void read(ByteBuf in) {
		//Should never be read on the server!
	}

	@Override
	public void executeClient(EntityPlayer thePlayer) {}

	@Override
	public void executeServer(EntityPlayerMP player) {}

}