package com.norwood.mcheli.factories;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.AbstractUIFactory;
import com.cleanroommc.modularui.factory.GuiManager;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.norwood.mcheli.aircraft.AircraftGui;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class AircraftGuiFactory extends AbstractUIFactory<AircraftGuiData> {

    public static AircraftGuiFactory INSTANCE = new AircraftGuiFactory();

    protected AircraftGuiFactory() {
        super("mcheli:aircraft");
    }

    private static <E extends MCH_EntityAircraft & IGuiHolder<AircraftGuiData>> void verifyEntity(EntityPlayer player, E entity) {
        Objects.requireNonNull(entity);
        if (!entity.isEntityAlive()) {
            throw new IllegalArgumentException("Can't open dead Entity GUI!");
        } else if (player.world != entity.world) {
            throw new IllegalArgumentException("Entity must be in same dimension as the player!");
        }
    }

    public <E extends MCH_EntityAircraft & IGuiHolder<AircraftGuiData>> void open(EntityPlayer player, E entity) {
        Objects.requireNonNull(player);
        verifyEntity(player, entity);
        GuiManager.open(this, new AircraftGuiData(player, entity, entity.getAcInfo()), (EntityPlayerMP) player);
    }

    @Override
    public @NotNull IGuiHolder<AircraftGuiData> getGuiHolder(AircraftGuiData guiData) {
        return Objects.requireNonNull(castGuiHolder(guiData.getGuiHolder()), "Found Entity is not a gui holder!");
    }

    @Override
    public void writeGuiData(AircraftGuiData guiData, PacketBuffer packetBuffer) {
        packetBuffer.writeInt(guiData.getGuiHolder().getEntityId());
    }

    @Override
    public @NotNull AircraftGuiData readGuiData(EntityPlayer entityPlayer, PacketBuffer packetBuffer) {
        var aircraft = (MCH_EntityAircraft) entityPlayer.world.getEntityByID(packetBuffer.readInt());
        return new AircraftGuiData(entityPlayer,
                aircraft,
                aircraft.getAcInfo()
        );
    }

    @Override
    public ModularScreen createScreen(AircraftGuiData guiData, ModularPanel mainPanel) {
        IGuiHolder<AircraftGuiData> guiHolder = Objects.requireNonNull(getGuiHolder(guiData), "Gui holder must not be null!");
        return guiHolder.createScreen(guiData, mainPanel);
    }



    @Override
    public boolean canInteractWith(EntityPlayer player, AircraftGuiData guiData) {
        Entity guiHolder = guiData.getGuiHolder();
        return super.canInteractWith(player, guiData) &&
                guiHolder != null &&
                player.getDistanceSq(guiHolder.posX, guiHolder.posY, guiHolder.posZ) <= 64 && //TODO: Make variable
                player.world == guiHolder.world &&
                guiHolder.isEntityAlive();
    }
}
