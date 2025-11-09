package com.norwood.mcheli.aircraft;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import com.norwood.mcheli.MCH_ClientTickHandlerBase;
import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_Key;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.networking.packet.PacketSeatPlayerControl;
import com.norwood.mcheli.wrapper.W_Reflection;

public class MCH_ClientSeatTickHandler extends MCH_ClientTickHandlerBase {

    public MCH_Key KeySwitchNextSeat;
    public MCH_Key KeySwitchPrevSeat;
    public MCH_Key KeyParachuting;
    public MCH_Key KeyFreeLook;
    public MCH_Key KeyUnmountForce;
    public MCH_Key[] Keys;
    protected boolean isRiding = false;
    protected boolean isBeforeRiding = false;

    public MCH_ClientSeatTickHandler(Minecraft minecraft, MCH_Config config) {
        super(minecraft);
        this.updateKeybind(config);
    }

    @Override
    public void updateKeybind(MCH_Config config) {
        this.KeySwitchNextSeat = new MCH_Key(MCH_Config.KeyExtra.prmInt);
        this.KeySwitchPrevSeat = new MCH_Key(MCH_Config.KeyGUI.prmInt);
        this.KeyParachuting = new MCH_Key(MCH_Config.KeySwitchHovering.prmInt);
        this.KeyUnmountForce = new MCH_Key(42);
        this.KeyFreeLook = new MCH_Key(MCH_Config.KeyFreeLook.prmInt);
        this.Keys = new MCH_Key[] { this.KeySwitchNextSeat, this.KeySwitchPrevSeat, this.KeyParachuting,
                this.KeyUnmountForce, this.KeyFreeLook };
    }

    @Override
    protected void onTick(boolean inGUI) {
        for (MCH_Key k : this.Keys) {
            k.update();
        }

        this.isBeforeRiding = this.isRiding;
        EntityPlayer player = this.mc.player;
        MCH_EntityAircraft ac = null;
        if (player != null && player.getRidingEntity() instanceof MCH_EntitySeat seat) {
            if (seat.getParent() == null || seat.getParent().getAcInfo() == null) {
                return;
            }

            ac = seat.getParent();
            if (!inGUI && !ac.isDestroyed()) {
                this.playerControl(player, seat, ac);
            }

            this.isRiding = true;
        } else {
            this.isRiding = false;
        }

        if (this.isBeforeRiding != this.isRiding) {
            if (this.isRiding) {
                W_Reflection.setThirdPersonDistance(ac.thirdPersonDist);
            } else {
                if (player == null || !(player.getRidingEntity() instanceof MCH_EntityAircraft)) {
                    W_Reflection.restoreDefaultThirdPersonDistance();
                }

                MCH_Lib.setRenderViewEntity(player);
            }
        }
    }

    private void playerControl(EntityPlayer player, MCH_EntitySeat seat, MCH_EntityAircraft ac) {
        PacketSeatPlayerControl playerControl = new PacketSeatPlayerControl();
        boolean send = false;
        if (this.KeyFreeLook.isKeyDown() && ac.canSwitchGunnerFreeLook(player)) {
            ac.switchGunnerFreeLookMode();
        }

        if (this.KeyParachuting.isKeyDown()) {
            if (ac.canParachuting(player)) {
                playerControl.parachuting = true;
                send = true;
            } else if (ac.canRepelling(player)) {
                playerControl.parachuting = true;
                send = true;
            } else {
                playSoundNG();
            }
        }

        if (send) {
            playerControl.sendToServer();
        }
    }
}
