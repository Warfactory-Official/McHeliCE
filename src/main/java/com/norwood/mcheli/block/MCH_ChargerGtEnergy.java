package com.norwood.mcheli.block;

import com.norwood.mcheli.compat.energy.MCH_EnergyCompat;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.Optional;

import gregtech.api.capability.IEnergyContainer;

/**
 * GregTech {@code IEnergyContainer} view over a {@link MCH_TileEntityCharger}'s buffer.
 *
 * <p>Kept separate from the tile entity because FE's {@code int getEnergyStored()} and GregTech's
 * {@code long getEnergyStored()} clash on a single class. Soft dependency: the whole interface is
 * stripped by Forge {@link Optional} when GregTech is absent, and this class is only instantiated
 * from a GregTech-guarded code path, so it never loads without GregTech.
 */
@Optional.Interface(iface = "gregtech.api.capability.IEnergyContainer", modid = MCH_EnergyCompat.MODID_GREGTECH)
public class MCH_ChargerGtEnergy implements IEnergyContainer {

    private final MCH_TileEntityCharger charger;

    public MCH_ChargerGtEnergy(MCH_TileEntityCharger charger) {
        this.charger = charger;
    }

    @Override
    public long acceptEnergyFromNetwork(EnumFacing side, long voltage, long amperage) {
        if (!this.charger.isActive(MCH_EnergyCompat.System.GT) || voltage <= 0 || amperage <= 0) return 0;
        long roomEu = MCH_EnergyCompat.internalToEu(this.charger.getCapacityInternal() - this.charger.getEnergyInternal());
        if (roomEu < voltage) return 0;
        long maxAmps = Math.min(amperage, Math.min(this.getInputAmperage(), roomEu / voltage));
        if (maxAmps <= 0) return 0;
        this.charger.insertInternal(MCH_EnergyCompat.euToInternal(voltage * maxAmps), false);
        return maxAmps;
    }

    @Override
    public boolean inputsEnergy(EnumFacing side) {
        return this.charger.isActive(MCH_EnergyCompat.System.GT);
    }

    @Override
    public long changeEnergy(long delta) {
        long beforeEu = MCH_EnergyCompat.internalToEu(this.charger.getEnergyInternal());
        long capEu = MCH_EnergyCompat.internalToEu(this.charger.getCapacityInternal());
        long target = Math.max(0, Math.min(capEu, beforeEu + delta));
        this.charger.setEnergyInternal(MCH_EnergyCompat.euToInternal(target));
        return target - beforeEu;
    }

    @Override
    public long getEnergyStored() {
        return MCH_EnergyCompat.internalToEu(this.charger.getEnergyInternal());
    }

    @Override
    public long getEnergyCapacity() {
        return MCH_EnergyCompat.internalToEu(this.charger.getCapacityInternal());
    }

    @Override
    public long getInputAmperage() {
        return 16L;
    }

    @Override
    public long getInputVoltage() {
        return 2_000_000_000L; // accept any practical voltage tier
    }
}
