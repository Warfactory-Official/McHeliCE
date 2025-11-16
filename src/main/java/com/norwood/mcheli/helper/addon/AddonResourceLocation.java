package com.norwood.mcheli.helper.addon;

import com.google.common.base.Strings;
import com.norwood.mcheli.Tags;
import com.norwood.mcheli.helper.MCH_Utils;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class AddonResourceLocation extends ResourceLocation {

    public static final AddonResourceLocation EMPTY_LOCATION = new AddonResourceLocation();
    public static final String SHARE_DOMAIN = "<!mcheli_share_domain>";
    public static final char SEPARATOR = '|';
    private final String addonDomain;
    private final boolean isEmpty;

    private AddonResourceLocation() {
        super(0, "empty", "empty");
        this.addonDomain = "@empty";
        this.isEmpty = true;
    }

    protected AddonResourceLocation(int unused, String... resourceName) {
        super(unused, resourceName[0], resourceName[2]);
        this.addonDomain = resourceName[1];
        this.isEmpty = false;
    }

    public AddonResourceLocation(String resourceName) {
        this(0, parsePath(resourceName));
    }

    public AddonResourceLocation(String resourceName, String addonDomainIn) {
        this(0, parsePath(addonDomainIn, resourceName));
    }

    public AddonResourceLocation(ResourceLocation resourceLocation, String addonDomainIn) {
        this(resourceLocation.toString(), addonDomainIn);
    }

    public AddonResourceLocation(String resourceDomainIn, String addonDomainIn, String resourcePathIn) {
        this(0, parsePath(resourceDomainIn + ":" + addonDomainIn + '|' + resourcePathIn));
    }

    protected static String[] parsePath(String pathIn) {
        String[] spl = splitObjectName(pathIn);
        String[] ret = new String[] { spl[0], null, spl[1] };
        int i = ret[2].indexOf(124);
        if (i >= 0) {
            ret[1] = ret[2].substring(0, i);
            ret[2] = ret[2].substring(i + 1);
        }

        ret[1] = Strings.isNullOrEmpty(ret[1]) ? "<!mcheli_share_domain>" : ret[1].toLowerCase(Locale.ROOT);
        if (ret[0].equals("minecraft")) {
            ret[0] = Tags.MODID;
        } else if (!spl[0].equals(Tags.MODID)) {
            MCH_Utils.logger().warn("Invalid mod domain '{}', replace at '{}'. path:'{}'", ret[0], Tags.MODID, ret[2]);
            ret[0] = Tags.MODID;
        }

        return ret;
    }

    protected static String[] parsePath(String addonDomain, String pathIn) {
        String[] spl = splitObjectName(pathIn);
        return new String[] { spl[0], addonDomain, spl[1] };
    }

    public static AddonResourceLocation share(ResourceLocation location) {
        return share(location.getNamespace(), location.getPath());
    }

    public static AddonResourceLocation share(String modid, String location) {
        return new AddonResourceLocation(modid, "<!mcheli_share_domain>", location);
    }

    public String getAddonDomain() {
        return this.addonDomain;
    }

    public String getAddonLocation() {
        return this.addonDomain + '|' + this.path;
    }

    public ResourceLocation asVanillaLocation() {
        return new ResourceLocation(this.namespace, this.getAddonLocation());
    }

    public ResourceLocation asVanillaDomainPath() {
        return new ResourceLocation(this.namespace, this.path);
    }

    public boolean isShareDomain() {
        return this.addonDomain.equals("<!mcheli_share_domain>");
    }

    public boolean isEmptyLocation() {
        return this.isEmpty || this.equals(EMPTY_LOCATION);
    }

    public int hashCode() {
        return 31 * (31 * super.hashCode() + this.addonDomain.hashCode()) + Boolean.hashCode(this.isEmpty);
    }

    public boolean equals(Object p_equals_1_) {
        if (this == p_equals_1_) {
            return true;
        } else if (p_equals_1_ instanceof AddonResourceLocation location && super.equals(p_equals_1_)) {
            return location.addonDomain.equals(this.addonDomain) && location.isEmpty == this.isEmpty;
        } else {
            return false;
        }
    }

    public boolean equalsIgnore(ResourceLocation location) {
        if (this == location) {
            return true;
        } else if (super.equals(location)) {
            if (this.isShareDomain()) {
                return true;
            } else if (location instanceof AddonResourceLocation other) {
                return other.isShareDomain() || other.addonDomain.equals(this.addonDomain);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public int compareTo(ResourceLocation p_compareTo_1_) {
        int i = this.namespace.compareTo(p_compareTo_1_.getNamespace());
        if (i == 0 && p_compareTo_1_ instanceof AddonResourceLocation) {
            i = this.addonDomain.compareTo(((AddonResourceLocation) p_compareTo_1_).addonDomain);
        }

        if (i == 0) {
            i = this.path.compareTo(p_compareTo_1_.getPath());
        }

        return i;
    }

    public @NotNull String toString() {
        return this.namespace + ":" + this.addonDomain + '|' + this.path;
    }
}
