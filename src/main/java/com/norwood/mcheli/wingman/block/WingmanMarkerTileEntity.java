package com.norwood.mcheli.wingman.block;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

public class WingmanMarkerTileEntity extends TileEntity {

    private MarkerType markerType    = MarkerType.PARKING;
    private String markerId          = "";   // ユーザー定義ID（滑走路・駐機場の識別子）
    private String baseId            = "";   // 所属ベースのID（BASE種別自身は空文字）
    /** PARKING 種別のみ有効: 駐機方位 -1=任意, 0=北, 1=東, 2=南, 3=西 */
    private int    parkingHeading    = -1;

    public MarkerType getMarkerType()    { return markerType; }
    public String getMarkerId()          { return markerId; }
    public String getBaseId()            { return baseId; }
    public int    getParkingHeading()    { return parkingHeading; }

    public void setMarkerType(MarkerType t) { markerType = t; markDirty(); sync(); }
    public void setMarkerId(String id)      { markerId = id;   markDirty(); sync(); }
    public void setBaseId(String id)        { baseId   = id;   markDirty(); sync(); }
    public void setParkingHeading(int h)    { parkingHeading = h; markDirty(); sync(); }

    // ─── NBT ────────────────────────────────────────────────────────────────

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setString("markerType", markerType.name());
        tag.setString("markerId",   markerId);
        tag.setString("baseId",     baseId);
        tag.setInteger("parkingHeading", parkingHeading);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        try { markerType = MarkerType.valueOf(tag.getString("markerType")); }
        catch (Exception ignored) { markerType = MarkerType.PARKING; }
        markerId       = tag.getString("markerId");
        baseId         = tag.getString("baseId");
        parkingHeading = tag.hasKey("parkingHeading") ? tag.getInteger("parkingHeading") : -1;
    }

    // ─── クライアント同期（右クリックGUI のために必要） ──────────────────

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, writeToNBT(new NBTTagCompound()));
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    /** サーバー側の変更をクライアントへブロードキャスト */
    private void sync() {
        if (world != null && !world.isRemote) {
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        }
    }
}
