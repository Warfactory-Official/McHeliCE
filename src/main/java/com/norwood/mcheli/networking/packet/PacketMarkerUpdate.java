package com.norwood.mcheli.networking.packet;

import com.norwood.mcheli.wingman.block.MarkerType;
import com.norwood.mcheli.wingman.block.WingmanMarkerTileEntity;
import com.norwood.mcheli.wingman.registry.MarkerRegistry;
import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.ElegantPacket;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

/**
 * Client (marker GUI) → Server: apply marker configuration changes.
 */
@ElegantPacket
public class PacketMarkerUpdate implements ClientToServerPacket {

    public int    x, y, z;
    public String typeName = "";
    public String id = "";
    public String baseId = "";
    public int    parkingHeading = -1;

    public PacketMarkerUpdate() {}

    public PacketMarkerUpdate(BlockPos pos, MarkerType type, String id, String baseId, int parkingHeading) {
        this.x              = pos.getX();
        this.y              = pos.getY();
        this.z              = pos.getZ();
        this.typeName       = type.name();
        this.id             = id;
        this.baseId         = baseId;
        this.parkingHeading = parkingHeading;
    }

    @Override
    public void onReceive(EntityPlayerMP player) {
        WorldServer ws = player.getServerWorld();
        ws.addScheduledTask(() -> {
            BlockPos pos = new BlockPos(this.x, this.y, this.z);
            TileEntity te = ws.getTileEntity(pos);
            if (!(te instanceof WingmanMarkerTileEntity)) return;

            MarkerType type;
            try { type = MarkerType.valueOf(this.typeName); }
            catch (Exception e) { type = MarkerType.PARKING; }

            WingmanMarkerTileEntity wte = (WingmanMarkerTileEntity) te;
            wte.setMarkerType(type);
            wte.setMarkerId(this.id);
            wte.setBaseId(type == MarkerType.BASE ? "" : this.baseId);
            wte.setParkingHeading(type == MarkerType.PARKING ? this.parkingHeading : -1);

            MarkerRegistry.register(ws, pos, wte);
        });
    }
}
