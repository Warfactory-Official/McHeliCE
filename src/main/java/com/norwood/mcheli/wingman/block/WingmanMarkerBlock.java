package com.norwood.mcheli.wingman.block;

import com.norwood.mcheli.wingman.registry.MarkerRegistry;
import com.norwood.mcheli.wingman.registry.TaxiRouteRegistry;
import com.norwood.mcheli.wingman.mission.TaxiRoute;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class WingmanMarkerBlock extends Block {

    public static final PropertyEnum<MarkerType> TYPE =
        PropertyEnum.create("type", MarkerType.class);

    public WingmanMarkerBlock() {
        super(Material.IRON);
        // Registry name is assigned by MCH_Blocks.register(this, "wingman_marker") (mcheli domain).
        setTranslationKey("wingman_marker");
        setHardness(2.0f);
        setResistance(10.0f);
        setDefaultState(blockState.getBaseState().withProperty(TYPE, MarkerType.WAYPOINT));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, TYPE);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState();
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return 0;
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof WingmanMarkerTileEntity) {
            return state.withProperty(TYPE, ((WingmanMarkerTileEntity) te).getMarkerType());
        }
        return state;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) { return true; }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new WingmanMarkerTileEntity();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        WingmanMarkerTileEntity te = getTe(world, pos);
        if (te == null || !(player instanceof net.minecraft.entity.player.EntityPlayerMP)) return true;
        net.minecraft.entity.player.EntityPlayerMP mp = (net.minecraft.entity.player.EntityPlayerMP) player;

        if (te.getMarkerType() == MarkerType.BASE) {
            // BASE マーカー: 基地コンフィグGUI（タキシールート + ミッションプランナー）
            openBaseGui(world, pos, te, mp);
        } else {
            // その他のマーカー: 通常のマーカー設定GUI
            new com.norwood.mcheli.networking.packet.PacketOpenMarkerGui(
                    pos, te.getMarkerType(), te.getMarkerId(), te.getBaseId(), te.getParkingHeading())
                .sendToPlayer(mp);
        }
        return true;
    }

    // ─── BASE GUI ─────────────────────────────────────────────────────────────

    private static void openBaseGui(World world, BlockPos pos, WingmanMarkerTileEntity te,
                                    net.minecraft.entity.player.EntityPlayerMP player) {
        String baseId = te.getMarkerId(); // BASE マーカーの ID が基地 ID
        com.norwood.mcheli.networking.packet.PacketOpenBaseGui pkt =
            new com.norwood.mcheli.networking.packet.PacketOpenBaseGui(pos, baseId);

        // タキシールートを収集
        for (TaxiRoute r : TaxiRouteRegistry.getForBase(world, baseId)) {
            com.norwood.mcheli.networking.packet.PacketOpenBaseGui.RouteDto dto =
                new com.norwood.mcheli.networking.packet.PacketOpenBaseGui.RouteDto();
            dto.routeId     = r.routeId;
            dto.parkingId   = r.parkingId;
            dto.runwayId    = r.runwayId;
            dto.runwayBId   = r.runwayBId;
            dto.waypointIds.addAll(r.waypointIds);
            dto.arrivalWaypointIds.addAll(r.arrivalWaypointIds);
            dto.arrivalRunwayId = r.arrivalRunwayId;
            dto.parkingHeading = r.parkingHeading;
            pkt.routes.add(dto);
        }

        // 子マーカーを収集（baseId 一致）
        for (MarkerRegistry.MarkerInfo m : MarkerRegistry.findChildren(world, baseId)) {
            com.norwood.mcheli.networking.packet.PacketOpenBaseGui.MarkerDto dto =
                new com.norwood.mcheli.networking.packet.PacketOpenBaseGui.MarkerDto();
            dto.id = m.id;
            dto.x  = m.pos.getX();
            dto.y  = m.pos.getY();
            dto.z  = m.pos.getZ();
            if (m.type == MarkerType.PARKING)         pkt.parkingMarkers.add(dto);
            else if (m.type == MarkerType.WAYPOINT)  pkt.waypointMarkers.add(dto);
            else if (m.type == MarkerType.RUNWAY_A)  { pkt.runwayAId = m.id; pkt.runwayAMarkers.add(dto); }
            else if (m.type == MarkerType.RUNWAY_B)  { pkt.runwayBId = m.id; pkt.runwayBMarkers.add(dto); }
            else if (m.type == MarkerType.HELIPAD)   pkt.helipads.add(dto);
            else if (m.type == MarkerType.HELIPAD_B) pkt.helipadBMarkers.add(dto);
        }

        // WAYPOINT 追加収集: baseId 未設定（新規設置直後など）でも 500 ブロック以内なら表示する。
        // GUI の "Parent Base" 設定を忘れた場合でもルート策定画面に WP を出す。
        java.util.Set<String> addedWpIds = new java.util.HashSet<>();
        for (com.norwood.mcheli.networking.packet.PacketOpenBaseGui.MarkerDto d : pkt.waypointMarkers) {
            addedWpIds.add(d.id);
        }
        final double WP_RADIUS_SQ = 500.0 * 500.0;
        for (MarkerRegistry.MarkerInfo m : MarkerRegistry.snapshot(world)) {
            if (m.type != MarkerType.WAYPOINT) continue;
            if (addedWpIds.contains(m.id)) continue; // baseId 一致で既に追加済み
            double dx = m.pos.getX() - pos.getX();
            double dz = m.pos.getZ() - pos.getZ();
            if (dx * dx + dz * dz > WP_RADIUS_SQ) continue; // 500 ブロック超は除外
            com.norwood.mcheli.networking.packet.PacketOpenBaseGui.MarkerDto dto =
                new com.norwood.mcheli.networking.packet.PacketOpenBaseGui.MarkerDto();
            dto.id = m.id;
            dto.x  = m.pos.getX();
            dto.y  = m.pos.getY();
            dto.z  = m.pos.getZ();
            pkt.waypointMarkers.add(dto);
            com.norwood.mcheli.wingman.McHeliWingman.logger.info(
                "[OpenBaseGui] WP '{}' added via radius fallback (baseId='{}' unset)",
                m.id, m.baseId);
        }

        // 近くの McHeli 機体を収集（512ブロック以内）
        double bx = pos.getX(), bz = pos.getZ();
        for (net.minecraft.entity.Entity e : world.loadedEntityList) {
            if (!com.norwood.mcheli.wingman.util.McheliReflect.isAircraft(e)) continue;
            double dx = e.posX - bx, dz = e.posZ - bz;
            if (dx * dx + dz * dz > 512.0 * 512.0) continue;
            com.norwood.mcheli.networking.packet.PacketOpenBaseGui.AircraftDto a =
                new com.norwood.mcheli.networking.packet.PacketOpenBaseGui.AircraftDto();
            a.uuid = e.getUniqueID().toString();
            a.name = e.getName();
            pkt.nearbyAircraft.add(a);
        }

        com.norwood.mcheli.wingman.McHeliWingman.logger.info(
            "[OpenBaseGui] Sending to {}: baseId={} routes={} parkings={} runwayAs={} helipads={} aircraft={}",
            player.getName(), baseId, pkt.routes.size(), pkt.parkingMarkers.size(),
            pkt.runwayAMarkers.size(), pkt.helipads.size(), pkt.nearbyAircraft.size());
        pkt.sendToPlayer(player);
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) {
            WingmanMarkerTileEntity te = getTe(world, pos);
            if (te != null) {
                if (te.getMarkerId().isEmpty()) {
                    te.setMarkerId(autoId(world, te.getMarkerType()));
                    te.markDirty();
                }
                MarkerRegistry.register(world, pos, te);
                player_hint(world, pos, te);
            }
        }
    }

    public static String autoId(World world, MarkerType type) {
        String prefix;
        switch (type) {
            case PARKING:   prefix = "p";   break;
            case RUNWAY_A:  prefix = "ra";  break;
            case RUNWAY_B:  prefix = "rb";  break;
            case HELIPAD:   prefix = "hp";  break;
            case HELIPAD_B: prefix = "hpb"; break;
            default:        prefix = "wp";  break;
        }
        int n = MarkerRegistry.findByType(world, type).size() + 1;
        return prefix + "_" + n;
    }

    /** 設置したプレイヤーにIDを通知する（サーバー側でプレイヤーが近くにいれば）。 */
    private static void player_hint(World world, BlockPos pos, WingmanMarkerTileEntity te) {
        for (net.minecraft.entity.player.EntityPlayer p : world.playerEntities) {
            if (p.getDistanceSq(pos.getX(), pos.getY(), pos.getZ()) < 16 * 16) {
                p.sendMessage(new TextComponentString(
                    "§7Marker placed: " + te.getMarkerType().displayName()
                    + " §7id=§e" + te.getMarkerId()
                    + " §7(§f/wingman marker type§7 to change type)"));
                break;
            }
        }
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) MarkerRegistry.unregister(world, pos);
        super.breakBlock(world, pos, state);
    }

    private WingmanMarkerTileEntity getTe(World world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        return te instanceof WingmanMarkerTileEntity ? (WingmanMarkerTileEntity) te : null;
    }
}
