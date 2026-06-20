package com.norwood.mcheli.compat.energy;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.compat.ModCompatManager;
import net.minecraft.item.ItemStack;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.Optional;

import gregtech.api.capability.GregtechCapabilities;
import gregtech.api.capability.IElectricItem;
import com.hbm.api.energymk2.IBatteryItem;

/**
 * Bridges MCheli's internal "energy units" (a plain integer, stored in the aircraft's FUEL field) to
 * whichever external energy system the server configures: Forge Energy (FE), GregTech EU, or HBM HE.
 *
 * <p>The internal unit is FE-equivalent (1 internal = 1 FE). EU and HE are converted using the
 * configurable ratios in {@link MCH_Config} ({@code EnergyRatioFEperEU}, {@code EnergyRatioFEperHE}).
 *
 * <p>GregTech and HBM are <em>soft</em> dependencies: every method that touches their types is
 * annotated {@link Optional.Method} so Forge strips it (returning a harmless default) when the mod is
 * absent. FE is part of Forge and always available.
 */
public final class MCH_EnergyCompat {

    public static final String MODID_GREGTECH = "gregtech";
    public static final String MODID_HBM = "hbm";

    /** Tier high enough to bypass GregTech voltage-tier gating on charge/discharge. */
    private static final int UNLIMITED_TIER = Integer.MAX_VALUE;

    public enum System { FE, GT, HBM }

    private MCH_EnergyCompat() {}

    public static boolean isGregtechLoaded() {
        return ModCompatManager.isLoaded(MODID_GREGTECH);
    }

    public static boolean isHbmLoaded() {
        return ModCompatManager.isLoaded(MODID_HBM);
    }

    /**
     * The active energy system. Falls back to FE when the configured mod isn't installed, which also
     * guarantees the GregTech/HBM ({@link Optional.Method}) code paths are never entered without the
     * mod present (their methods are stripped at runtime when absent).
     */
    public static System active() {
        String s = MCH_Config.EnergySystem != null ? MCH_Config.EnergySystem.prmString : "FE";
        if (s != null) {
            s = s.trim();
            if ((s.equalsIgnoreCase("GT") || s.equalsIgnoreCase("gregtech") || s.equalsIgnoreCase("EU"))
                    && isGregtechLoaded()) return System.GT;
            if ((s.equalsIgnoreCase("HBM") || s.equalsIgnoreCase("HE") || s.equalsIgnoreCase("ntm"))
                    && isHbmLoaded()) return System.HBM;
        }
        return System.FE;
    }

    /* ---------------- unit conversion (internal == FE) ---------------- */

    private static double euRatio() {
        double r = MCH_Config.EnergyRatioEU != null ? MCH_Config.EnergyRatioEU.prmDouble : 4.0;
        return r > 0 ? r : 4.0;
    }

    private static double heRatio() {
        double r = MCH_Config.EnergyRatioHE != null ? MCH_Config.EnergyRatioHE.prmDouble : 1.0;
        return r > 0 ? r : 1.0;
    }

    public static int euToInternal(long eu) {
        return (int) Math.min(Integer.MAX_VALUE, Math.round(eu * euRatio()));
    }

    public static long internalToEu(int internal) {
        return (long) Math.floor(internal / euRatio());
    }

    public static int heToInternal(long he) {
        return (int) Math.min(Integer.MAX_VALUE, Math.round(he * heRatio()));
    }

    public static long internalToHe(int internal) {
        return (long) Math.floor(internal / heRatio());
    }

    /* ---------------- battery items (in the fuel/charge slot) ---------------- */

    /** True when the stack can store/provide energy for the active system (FE is always accepted). */
    public static boolean isBattery(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (hasFe(stack)) return true;
        return switch (active()) {
            case GT -> isGtElectric(stack);
            case HBM -> isHbmBattery(stack);
            default -> false;
        };
    }

    /**
     * Pulls up to {@code maxInternal} internal units OUT of a battery item (to top up an aircraft).
     *
     * @return internal units actually obtained.
     */
    public static int dischargeFromItem(ItemStack stack, int maxInternal, boolean simulate) {
        if (stack == null || stack.isEmpty() || maxInternal <= 0) return 0;
        if (hasFe(stack)) return feDischarge(stack, maxInternal, simulate);
        return switch (active()) {
            case GT -> gtDischarge(stack, maxInternal, simulate);
            case HBM -> hbmDischarge(stack, maxInternal, simulate);
            default -> 0;
        };
    }

    /**
     * Pushes up to {@code maxInternal} internal units INTO a battery item (overflow storage).
     *
     * @return internal units actually stored.
     */
    public static int chargeToItem(ItemStack stack, int maxInternal, boolean simulate) {
        if (stack == null || stack.isEmpty() || maxInternal <= 0) return 0;
        if (hasFe(stack)) return feCharge(stack, maxInternal, simulate);
        return switch (active()) {
            case GT -> gtCharge(stack, maxInternal, simulate);
            case HBM -> hbmCharge(stack, maxInternal, simulate);
            default -> 0;
        };
    }

    /* ---------------- Forge Energy (always available) ---------------- */

    private static boolean hasFe(ItemStack stack) {
        return stack.hasCapability(CapabilityEnergy.ENERGY, null);
    }

    private static int feDischarge(ItemStack stack, int maxInternal, boolean simulate) {
        IEnergyStorage es = stack.getCapability(CapabilityEnergy.ENERGY, null);
        if (es == null || !es.canExtract()) return 0;
        return Math.max(0, es.extractEnergy(maxInternal, simulate));
    }

    private static int feCharge(ItemStack stack, int maxInternal, boolean simulate) {
        IEnergyStorage es = stack.getCapability(CapabilityEnergy.ENERGY, null);
        if (es == null || !es.canReceive()) return 0;
        return Math.max(0, es.receiveEnergy(maxInternal, simulate));
    }

    /* ---------------- GregTech EU (soft dependency) ---------------- */

    @Optional.Method(modid = MODID_GREGTECH)
    private static boolean isGtElectric(ItemStack stack) {
        return stack.hasCapability(GregtechCapabilities.CAPABILITY_ELECTRIC_ITEM, null);
    }

    @Optional.Method(modid = MODID_GREGTECH)
    private static int gtDischarge(ItemStack stack, int maxInternal, boolean simulate) {
        IElectricItem it = stack.getCapability(GregtechCapabilities.CAPABILITY_ELECTRIC_ITEM, null);
        if (it == null || !it.canProvideChargeExternally()) return 0;
        long maxEu = internalToEu(maxInternal);
        if (maxEu <= 0) return 0;
        long euPulled = it.discharge(maxEu, UNLIMITED_TIER, true, true, simulate);
        return euToInternal(euPulled);
    }

    @Optional.Method(modid = MODID_GREGTECH)
    private static int gtCharge(ItemStack stack, int maxInternal, boolean simulate) {
        IElectricItem it = stack.getCapability(GregtechCapabilities.CAPABILITY_ELECTRIC_ITEM, null);
        if (it == null || !it.chargeable()) return 0;
        long maxEu = internalToEu(maxInternal);
        if (maxEu <= 0) return 0;
        long euStored = it.charge(maxEu, UNLIMITED_TIER, true, simulate);
        return euToInternal(euStored);
    }

    /* ---------------- HBM HE (soft dependency) ---------------- */

    @Optional.Method(modid = MODID_HBM)
    private static boolean isHbmBattery(ItemStack stack) {
        return stack.getItem() instanceof IBatteryItem;
    }

    @Optional.Method(modid = MODID_HBM)
    private static int hbmDischarge(ItemStack stack, int maxInternal, boolean simulate) {
        if (!(stack.getItem() instanceof IBatteryItem bat)) return 0;
        long maxHe = internalToHe(maxInternal);
        long avail = Math.min(maxHe, Math.min(bat.getCharge(stack), bat.getDischargeRate(stack)));
        if (avail <= 0) return 0;
        if (!simulate) bat.setCharge(stack, bat.getCharge(stack) - avail);
        return heToInternal(avail);
    }

    @Optional.Method(modid = MODID_HBM)
    private static int hbmCharge(ItemStack stack, int maxInternal, boolean simulate) {
        if (!(stack.getItem() instanceof IBatteryItem bat)) return 0;
        long maxHe = internalToHe(maxInternal);
        long room = Math.min(maxHe, Math.min(bat.getMaxCharge(stack) - bat.getCharge(stack), bat.getChargeRate(stack)));
        if (room <= 0) return 0;
        if (!simulate) bat.setCharge(stack, bat.getCharge(stack) + room);
        return heToInternal(room);
    }
}
