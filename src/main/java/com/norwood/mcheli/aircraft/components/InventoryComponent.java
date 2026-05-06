package com.norwood.mcheli.aircraft.components;

import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.aircraft.MCH_AircraftInventory;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_ItemFuel;
import com.norwood.mcheli.helper.MCH_CriteriaTriggers;
import com.norwood.mcheli.weapon.MCH_WeaponInfo;
import com.norwood.mcheli.weapon.MCH_WeaponSet;
import com.norwood.mcheli.wrapper.W_Entity;
import com.norwood.mcheli.wrapper.W_Item;
import lombok.Getter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class InventoryComponent implements IAircraftComponent {
    @Getter
    private final MCH_EntityAircraft parent;
    private final MCH_AircraftInventory inventory;

    private int supplyAmmoWait = 0;
    private boolean beforeSupplyAmmo = false;

    public InventoryComponent(MCH_EntityAircraft parent) {
        this.parent = parent;
        this.inventory = new MCH_AircraftInventory(parent);
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
        updateSupplyAmmo();
        supplyAmmoToOtherAircraft();
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        this.inventory.readFromNBT(compound);
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        this.inventory.writeToNBT(compound);
    }

    public MCH_AircraftInventory getGuiInventory() {
        return this.inventory;
    }



    private void updateSupplyAmmo() {
        if (!parent.world.isRemote) {
            boolean isReloading = parent.getRiddenByEntity() instanceof EntityPlayer && !parent.getRiddenByEntity().isDead;
            parent.networkSync.setCommonStatus(2, isReloading);
            if (!parent.isDestroyed() && this.beforeSupplyAmmo && !isReloading) {
                parent.weaponSystem.reloadAllWeapon();
            }
            this.beforeSupplyAmmo = isReloading;
        }
    }

    private void supplyAmmoToOtherAircraft() {
        float range = parent.getAcInfo() != null ? parent.getAcInfo().ammoSupplyRange : 0.0F;
        if (range > 0.0F && !parent.world.isRemote && parent.getCountOnUpdate() % 40 == 0) {
            List<MCH_EntityAircraft> list = parent.world.getEntitiesWithinAABB(MCH_EntityAircraft.class, parent.getCollisionBoundingBox().grow(range, range, range));
            for (MCH_EntityAircraft ac : list) {
                if (!W_Entity.isEqual(parent, ac) && ac.canSupply()) {
                    for (int wid = 0; wid < ac.weaponSystem.getWeaponNum(); wid++) {
                        MCH_WeaponSet ws = ac.weaponSystem.getWeapon(wid);
                        int num = ws.getRestAllAmmoNum() + ws.getAmmo();
                        if (num < ws.getMaxAmmo()) {
                            ws.setReserveAmmo(num + Math.max(1, ws.getMaxAmmo() / 10));
                        }
                    }
                }
            }
        }
    }
}
