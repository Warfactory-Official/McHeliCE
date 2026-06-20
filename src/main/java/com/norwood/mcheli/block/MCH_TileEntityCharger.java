package com.norwood.mcheli.block;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.aircraft.MCH_AircraftInventory;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.compat.energy.MCH_EnergyCompat;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import gregtech.api.capability.GregtechCapabilities;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.lib.ForgeDirection;

import java.util.List;

/**
 * A powered block that charges nearby electric MCheli aircraft (and battery items in their fuel slot).
 *
 * <p>It buffers energy in MCheli's internal unit (FE-equivalent) and accepts power from whichever
 * external system {@link MCH_Config#EnergySystem} selects:
 * <ul>
 *   <li><b>FE</b> – exposes Forge's {@link IEnergyStorage}; cables/generators push in.</li>
 *   <li><b>GT</b> – exposes GregTech's {@code IEnergyContainer} (via {@link MCH_ChargerGtEnergy}).</li>
 *   <li><b>HBM</b> – implements {@code IEnergyReceiverMK2} and subscribes to HBM power networks.</li>
 * </ul>
 * GregTech and HBM are soft dependencies: their interfaces/methods are stripped by Forge
 * {@link Optional} when the mod is absent, so this class still loads in a vanilla-Forge environment.
 *
 * <p>Note: FE's {@code int getEnergyStored()} and GregTech's {@code long getEnergyStored()} cannot
 * coexist on one class, so the GregTech container is delegated to {@link MCH_ChargerGtEnergy}.
 */
@Optional.Interface(iface = "com.hbm.api.energymk2.IEnergyReceiverMK2", modid = MCH_EnergyCompat.MODID_HBM)
public class MCH_TileEntityCharger extends TileEntity implements ITickable, IEnergyStorage, IEnergyReceiverMK2 {

    /** Internal energy buffer (FE-equivalent units). */
    private int energy;
    private int updateCounter;
    // Lazily-created GregTech view. Its type is one of OUR classes (loads fine without GregTech, whose
    // interface is stripped by @Optional); only the cast inside getGtCapability touches GregTech types.
    private MCH_ChargerGtEnergy gtContainer;

    int getEnergyInternal() {
        return this.energy;
    }

    int getCapacityInternal() {
        return Math.max(0, MCH_Config.ChargerCapacity.prmInt);
    }

    boolean isActive(MCH_EnergyCompat.System system) {
        return MCH_EnergyCompat.active() == system;
    }

    /** Adds internal energy to the buffer, capped at capacity. Returns the amount accepted. */
    int insertInternal(int amount, boolean simulate) {
        int accepted = Math.max(0, Math.min(amount, this.getCapacityInternal() - this.energy));
        if (accepted > 0 && !simulate) {
            this.energy += accepted;
            this.markDirty();
        }
        return accepted;
    }

    /** Directly sets the buffer (used by external-system setters), clamped to capacity. */
    void setEnergyInternal(int value) {
        this.energy = Math.max(0, Math.min(this.getCapacityInternal(), value));
        this.markDirty();
    }

    private int transferRate() {
        return Math.max(0, MCH_Config.ChargerTransferRate.prmInt);
    }

    /* ============================ tick ============================ */

    @Override
    public void update() {
        if (this.world == null || this.world.isRemote) return;

        if (this.isActive(MCH_EnergyCompat.System.HBM)) {
            this.hbmSubscribe();
        }

        if (++this.updateCounter < 10) return;
        this.updateCounter = 0;

        if (this.energy <= 0) return;
        this.distributeToAircraft();
    }

    private void distributeToAircraft() {
        double range = MCH_Config.ChargerRange.prmDouble;
        if (range <= 0.0) return;

        AxisAlignedBB box = new AxisAlignedBB(this.pos).grow(range);
        List<MCH_EntityAircraft> list = this.world.getEntitiesWithinAABB(MCH_EntityAircraft.class, box);

        int budget = this.transferRate();
        for (MCH_EntityAircraft ac : list) {
            if (this.energy <= 0 || budget <= 0) break;
            if (!ac.isElectric()) continue;

            // 1) top up the aircraft's internal buffer
            int want = ac.getMaxFuel() - ac.getFuel();
            if (want > 0) {
                int give = Math.min(Math.min(budget, this.energy), want);
                int accepted = ac.addEnergy(give, false);
                this.energy -= accepted;
                budget -= accepted;
            }

            // 2) overflow into a battery in the fuel slot (extended storage)
            if (this.energy > 0 && budget > 0 && ac.allowBattery()) {
                ItemStack bat = ac.getGuiInventory().getFuelSlotItemStack(MCH_AircraftInventory.SLOT_FUEL_IN);
                int room = Math.min(budget, this.energy);
                int stored = MCH_EnergyCompat.chargeToItem(bat, room, false);
                this.energy -= stored;
                budget -= stored;
            }
        }
        this.markDirty();
    }

    /* ============================ NBT ============================ */

    @Override
    public void readFromNBT(@NotNull NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.energy = tag.getInteger("Energy");
    }

    @Override
    public @NotNull NBTTagCompound writeToNBT(@NotNull NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("Energy", this.energy);
        return tag;
    }

    /* ============================ Forge Energy (FE) ============================ */

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!this.canReceive()) return 0;
        return this.insertInternal(maxReceive, simulate);
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0; // sink only
    }

    @Override
    public int getEnergyStored() {
        return this.energy;
    }

    @Override
    public int getMaxEnergyStored() {
        return this.getCapacityInternal();
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return this.isActive(MCH_EnergyCompat.System.FE);
    }

    /* ============================ capabilities ============================ */

    @Override
    public boolean hasCapability(@NotNull Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) return true;
        // Guarded so the GregTech-only method is never invoked when GregTech is absent (it is stripped).
        if (MCH_EnergyCompat.isGregtechLoaded() && this.hasGtCapability(capability)) return true;
        return super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(@NotNull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(this);
        }
        if (MCH_EnergyCompat.isGregtechLoaded()) {
            T gt = this.getGtCapability(capability);
            if (gt != null) return gt;
        }
        return super.getCapability(capability, facing);
    }

    @Optional.Method(modid = MCH_EnergyCompat.MODID_GREGTECH)
    private boolean hasGtCapability(Capability<?> capability) {
        return this.isActive(MCH_EnergyCompat.System.GT)
                && capability == GregtechCapabilities.CAPABILITY_ENERGY_CONTAINER;
    }

    @Optional.Method(modid = MCH_EnergyCompat.MODID_GREGTECH)
    private <T> T getGtCapability(Capability<T> capability) {
        if (this.isActive(MCH_EnergyCompat.System.GT)
                && capability == GregtechCapabilities.CAPABILITY_ENERGY_CONTAINER) {
            if (this.gtContainer == null) this.gtContainer = new MCH_ChargerGtEnergy(this);
            return GregtechCapabilities.CAPABILITY_ENERGY_CONTAINER.cast(this.gtContainer);
        }
        return null;
    }

    /* ============================ HBM HE ============================ */

    @Override
    public long getPower() {
        return MCH_EnergyCompat.internalToHe(this.energy);
    }

    @Override
    public void setPower(long power) {
        if (!this.isActive(MCH_EnergyCompat.System.HBM)) return;
        this.setEnergyInternal(MCH_EnergyCompat.heToInternal(Math.max(0, power)));
    }

    @Override
    public long getMaxPower() {
        if (!this.isActive(MCH_EnergyCompat.System.HBM)) return 0L; // reject HBM transfer when inactive
        return MCH_EnergyCompat.internalToHe(this.getCapacityInternal());
    }

    @Override
    public boolean isLoaded() {
        return !this.isInvalid();
    }

    @Optional.Method(modid = MCH_EnergyCompat.MODID_HBM)
    private void hbmSubscribe() {
        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            this.trySubscribe(this.world,
                    this.pos.getX() + dir.offsetX,
                    this.pos.getY() + dir.offsetY,
                    this.pos.getZ() + dir.offsetZ,
                    dir);
        }
    }
}
