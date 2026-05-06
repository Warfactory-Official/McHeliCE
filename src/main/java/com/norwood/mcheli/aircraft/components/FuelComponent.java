package com.norwood.mcheli.aircraft.components;

import com.norwood.mcheli.aircraft.*;
import com.norwood.mcheli.helper.MCH_CriteriaTriggers;
import com.norwood.mcheli.helper.MCH_Logger;
import com.norwood.mcheli.wrapper.W_Entity;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.norwood.mcheli.aircraft.components.NetworkSyncComponent.FUEL;
import static com.norwood.mcheli.aircraft.components.NetworkSyncComponent.FUEL_FF;

public class FuelComponent implements IAircraftComponent {


    @Getter
    private final MCH_EntityAircraft parent;
    private double fuelConsumption;
    private int fuelSuppliedCount;


    public FuelComponent(MCH_EntityAircraft parent) {
        this.parent = parent;
        this.fuelConsumption = 0.0;
    }

    @Override
    public MCH_EntityAircraft getParent() {
        return parent;
    }

    @Override
    public void init() {

    }

    @Override
    public void onUpdate() {

        this.supplyFuel();
        this.updateFuel();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        this.setFuel(nbt.getInteger("AcFuel"));
        this.setFuelType(nbt.hasKey("AcFuelType") ? nbt.getString("AcFuelType") : "mch_fuel"); //Default to the default mcheli fuel
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("AcFuel", this.getFuel());
        nbt.setString("AcFuelType", this.getFuelType());
    }

    public boolean isInfinityFuel(Entity player, boolean checkOtherSeet) {
        if (!this.parent.isCreative(player) && !this.parent.networkSync.getCommonStatus(4)) {
            if (checkOtherSeet) {
                for (MCH_EntitySeat seat : parent.seatManager.getSeats()) {
                    if (seat != null && this.parent.isCreative(seat.getRiddenByEntity())) {
                        return true;
                    }
                }
            }

            return false;
        } else {
            return true;
        }
    }

    public float getFuelPercentage() {
        int m = this.getMaxFuel();
        return m == 0 ? 0.0F : (float) this.getFuel() / m;
    }

    public int getFuel() {
        return parent.getDataManager().get(FUEL);
    }

    public void setFuel(int fuel) {
        if (parent.world.isRemote) return;
        if (fuel < 0) fuel = 0;
        if (fuel > this.getMaxFuel()) fuel = this.getMaxFuel();
        if (fuel != this.getFuel()) parent.getDataManager().set(FUEL, fuel);
    }

    public boolean canUseFuel(boolean checkOtherSeet) {
        return this.getMaxFuel() <= 0 || this.getFuel() > 1 || this.isInfinityFuel(parent.getRiddenByEntity(), checkOtherSeet);
    }

    public boolean canUseFuel() {
        return this.canUseFuel(false);
    }

    public int getMaxFuel() {
        return parent.getAcInfo() != null ? parent.getAcInfo().maxFuel : 0;
    }

    public void dumpFuel() {
        if (parent.world.isRemote) return;
        parent.getDataManager().set(FUEL, 0);
        parent.getDataManager().set(FUEL_FF, "");
    }

    public float getFuelConsumptionFactor() {
        var ac = parent.getAcInfo();
        if (ac == null) return 1F;
        return ac.getFuelConsumption(this.getFuelType());
    }

    public void supplyFuel() {
        MCH_AircraftInfo info = parent.getAcInfo();
        if (info == null || info.fuelSupplyRange <= 0.0F) return;
        if (parent.world.isRemote || parent.getCountOnUpdate() % 10 != 0) return;

        Fluid fluid;
        if (info.fuelSupplyInfinite) {
            fluid = FluidRegistry.getFluid(info.fuelSupplyType);
        } else {
            fluid = this.getFuelFluid();
        }

        if (fluid == null) return;
        if (!info.fuelSupplyInfinite && this.getFuel() <= 0) return;

        float range = info.fuelSupplyRange;
        List<MCH_EntityAircraft> list = parent.world.getEntitiesWithinAABB(
                MCH_EntityAircraft.class,
                parent.getCollisionBoundingBox().grow(range, range, range)
        );

        for (MCH_EntityAircraft ac : list) {
            if (W_Entity.isEqual(parent, ac)) continue;

            if (this.canAcceptFuel(fluid)) {
                if ((!parent.onGround || ac.canSupply()) && getFuel() < getMaxFuel()) {
                    int amount = Math.min(getMaxFuel() - getFuel(), 30);

                    if (getFuel() <= 0) {
                        setFuelType(fluid);
                    }

                    setFuel(getFuel() + amount);

                    if (!info.fuelSupplyInfinite) {
                        this.setFuel(Math.max(0, this.getFuel() - amount));
                    }
                }
                fuelSuppliedCount = 40;
            }
        }
    }

    public boolean canAcceptFuel(Fluid fluid) {
        var info = parent.getAcInfo();
        if (info == null || fluid == null) return false;

        if (!info.isFuelValid(new FluidStack(fluid, 1))) return false;

        Fluid currentType = getFuelFluid();
        return getFuel() <= 0 || currentType == null || currentType == fluid;
    }

    public void updateFuel() {
        if (getMaxFuel() == 0 || parent.isDestroyed() || parent.world.isRemote) return;

        if (fuelSuppliedCount > 0) fuelSuppliedCount--;

        //Consumption Logic
        if (parent.getCountOnUpdate() % 20 == 0 && getFuel() > 1 && parent.getThrottle() > 0.0 && fuelSuppliedCount <= 0) {
            var acInfo = parent.getAcInfo();
            if (acInfo != null) {
                double throttleFactor = Math.min(parent.getThrottle() * 1.4, 1.0);
                fuelConsumption += throttleFactor * acInfo.fuelConsumption * getFuelConsumptionFactor();

                if (fuelConsumption > 1.0) {
                    int toConsume = (int) fuelConsumption;
                    fuelConsumption -= toConsume;
                    setFuel(getFuel() - toConsume);
                }
            }
        }

        //Refueling Logic
        int currentFuel = getFuel();
        int maxFuel = getMaxFuel();

        if (parent.canSupply() && parent.getCountOnUpdate() % 10 == 0 && currentFuel < maxFuel) {
            MCH_AircraftInventory inventory = parent.getGuiInventory();
            ItemStack inputStack = inventory.getFuelSlotItemStack(MCH_AircraftInventory.SLOT_FUEL_IN);
            if (inputStack.isEmpty()) return;

            if (inputStack.getItem() instanceof MCH_ItemFuel) {
                handleItemFuelRefuel(inventory, inputStack, currentFuel, maxFuel);
                return;
            }

            IFluidHandlerItem handler = inputStack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
            if (handler == null) return;

            IFluidTankProperties tank = Arrays.stream(handler.getTankProperties())
                    .filter(p -> p.canDrain() && p.getContents() != null && canAcceptFuel(p.getContents().getFluid()))
                    .findFirst().orElse(null);

            if (tank == null) return;

            int fuelNeeded = maxFuel - currentFuel;
            Fluid fluid = Objects.requireNonNull(tank.getContents()).getFluid();

            FluidStack drained = handler.drain(new FluidStack(fluid, fuelNeeded), true);
            if (drained == null || drained.amount <= 0) return;

            ItemStack emptyContainer = handler.getContainer();
            ItemStack outputStack = inventory.getFuelSlotItemStack(MCH_AircraftInventory.SLOT_FUEL_OUT);

            if (manageOutputSlot(inventory, outputStack, emptyContainer)) {
                inputStack.shrink(1);
                setFuelType(fluid);
                setFuel(currentFuel + drained.amount);
                triggerRefuelCriteria();
            }
        }
    }

    @Nullable
    public FluidStack getFuelStack() {
        if (this.getFuel() == 0 || getFuelType().isEmpty()) return null;

        if (!FluidRegistry.isFluidRegistered(getFuelType())) {
            MCH_Logger.warn("Fluid: {} does not exist, make sure required fluids do exist. Fluid will be cleared");
            return null;
        }
        return new FluidStack(FluidRegistry.getFluid(getFuelType()), getFuel());
    }

    @Nullable
    public Fluid getFuelFluid() {
        if (getFuel() < 0)
            return null;
        String fluidName = getFuelType();

        var fluid = fluidName.isBlank() ? null : FluidRegistry.getFluid(fluidName);
        if (fluid == null) {
            MCH_Logger.warn("Retrieved fluid: {} does not exist, make sure required fluids do exist.", fluidName);
        }

        return fluid;
    }

    public String getFuelType() {
        return parent.getDataManager().get(FUEL_FF);
    }

    public void setFuelType(Fluid fluid) {
        if (fluid == null) return;
        parent.getDataManager().set(FUEL_FF, fluid.getName());
    }

    public void setFuelType(String name) {
        if (!name.isBlank() && FluidRegistry.getFluid(name) == null) {
            MCH_Logger.warn("Set fluid: {} does not exist, make sure required fluids do exist. Fluid will be set to default", name);
            parent.getDataManager().set(FUEL_FF, "mch_fuel");
        } else parent.getDataManager().set(FUEL_FF, name);
    }

    private void handleItemFuelRefuel(MCH_AircraftInventory inventory, ItemStack stack, int current, int max) {
        Fluid fuelType = FluidRegistry.getFluid("mch_fuel");
        if (!canAcceptFuel(fuelType)) return;

        int fuelInItem = stack.getMaxDamage() - stack.getItemDamage();
        if (fuelInItem <= 0) return;

        int needed = max - current;
        int toTransfer = Math.min(needed, fuelInItem);

        stack.setItemDamage(stack.getItemDamage() + toTransfer);
        setFuelType(fuelType);
        setFuel(current + toTransfer);

        if (stack.getItemDamage() >= stack.getMaxDamage()) {
            ItemStack empty = new ItemStack(stack.getItem());
            empty.setItemDamage(stack.getMaxDamage());

            ItemStack outputStack = inventory.getFuelSlotItemStack(MCH_AircraftInventory.SLOT_FUEL_OUT);
            if (manageOutputSlot(inventory, outputStack, empty)) {
                stack.shrink(1);
            }
        }
        triggerRefuelCriteria();
    }

    private void triggerRefuelCriteria() {
        if (parent.getRiddenByEntity() instanceof EntityPlayerMP player) {
            MCH_CriteriaTriggers.SUPPLY_FUEL.trigger(player);
        }
    }

    private boolean manageOutputSlot(MCH_AircraftInventory inv, ItemStack out, ItemStack container) {
        if (out.isEmpty()) {
            inv.getItemHandler().setStackInSlot(MCH_AircraftInventory.SLOT_FUEL_OUT, container.copy());
            return true;
        }
        if (ItemStack.areItemsEqual(out, container) && ItemStack.areItemStackTagsEqual(out, container)
                && out.getCount() < out.getMaxStackSize()) {
            out.grow(container.getCount());
            return true;
        }
        return false;
    }

    @Nullable
    public FluidStack drain(int maxDrain, boolean doDrain) {
        if (maxDrain <= 0) return null;

        int contents = getFuel();
        if (contents <= 0) return null;

        int drained = Math.min(maxDrain, contents);

        if (doDrain) {
            parent.getDataManager().set(FUEL, contents - drained);
        }

        Fluid fluid = FluidRegistry.getFluid(getFuelType());
        if (fluid == null) return null;

        return new FluidStack(fluid, drained);
    }

    @Nullable
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        if (resource == null || resource.amount <= 0) return null;

        int contents = getFuel();
        if (contents <= 0) return null;

        String type = getFuelType();
        String fluidName = resource.getFluid().getName();

        if (!fluidName.equalsIgnoreCase(type)) return null;

        int drained = Math.min(resource.amount, contents);

        if (doDrain) {
            parent.getDataManager().set(FUEL, contents - drained);
        }

        return new FluidStack(resource.getFluid(), drained);
    }

    public int fill(FluidStack resource, boolean doFill) {
        var ac = parent.getAcInfo();
        if (ac == null) return 0;
        int maxFill = getMaxFuel();
        int contents = getFuel();
        String type = getFuelType();
        String fluidName = resource.getFluid().getName();
        boolean sameFuel = fluidName.equalsIgnoreCase(type);

        //can't overfill or mix fuels
        if (contents >= maxFill || (contents > 0 && !sameFuel)) return 0;

        int accepted = Math.min(resource.amount, maxFill - contents);
        if (doFill) {
            parent.getDataManager().set(FUEL, contents + accepted);
        }
        return accepted;
    }


}
