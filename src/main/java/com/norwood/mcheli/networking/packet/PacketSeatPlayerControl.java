package com.norwood.mcheli.networking.packet;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_EntitySeat;
import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.ElegantPacket;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumHand;

@ElegantPacket
public class PacketSeatPlayerControl extends PacketBase implements ClientToServerPacket {

    public PlayerControlState switchSeat = PlayerControlState.IDLE;
    public boolean parachuting;

    @Override
    public void onReceive(EntityPlayerMP player) {
        if (player.world.isRemote) return;
        MCH_EntityAircraft ac;
        if (player.getRidingEntity() instanceof MCH_EntitySeat seat) {
            ac = seat.getParent();
        } else {
            ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(player);
        }

        if (ac == null) return;

        switch (switchSeat) {
            case IDLE -> {
                break;
            }
            case DISMOUNT -> {
                player.dismountRidingEntity();
                ac.keepOnRideRotation = true;
                ac.processInitialInteract(player, true, EnumHand.MAIN_HAND);
            }
            case NEXT -> ac.switchNextSeat(player);
            case PREV -> ac.switchPrevSeat(player);
        }

        if (this.parachuting) {
            ac.unmount(player);

        }
    }

    public static enum PlayerControlState {
        IDLE,
        NEXT,
        PREV,
        DISMOUNT

    }
}
