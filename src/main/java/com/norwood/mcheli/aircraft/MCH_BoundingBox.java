package com.norwood.mcheli.aircraft;

import com.norwood.mcheli.MCH_Lib;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

/**
 * Rotatable bounding box class that stores orientation and local axis vectors
 * for fine-grained collision detection.
 */
public class MCH_BoundingBox {

    /** Current axis-aligned bounding box (used for broad-phase intersection tests) */
    @Getter
    public AxisAlignedBB boundingBox;

    /** Previous frame's bounding box (kept for historical state) */
    public AxisAlignedBB backupBoundingBox;

    /** Original local offset */
    public double offsetX;
    public double offsetY;
    public double offsetZ;

    /** Width (X axis), height (Y axis), and depth (Z axis) */
    public float width;
    public float widthZ;
    public float height;

    /** Half-widths for internal calculations */
    public float halfWidth;
    public float halfHeight;
    public float halfDepth;

    /** Offset vector after rotation (local origin rotated to world position) */
    public Vec3d rotatedOffset;

    /** Current and previous center positions in world coordinates */
    public Vec3d nowPos;
    public Vec3d prevPos;

    /** Damage factor when this bounding box is hit */
    @Getter
    public float damageFactor;

    /** Type of bounding box (unused, reserved for extensions) */
    @Setter
    public EnumBoundingBoxType boundingBoxType = EnumBoundingBoxType.DEFAULT;

    /** Optional name for this bounding box */
    @Setter
    public String name = "";

    // === Orientation data ===
    /** Current rotation angles (in degrees) */
    public float rotationYaw = 0.0F;
    public float rotationPitch = 0.0F;
    public float rotationRoll = 0.0F;

    /** Local axis vectors in world space */
    public Vec3d axisX = new Vec3d(1.0D, 0.0D, 0.0D);
    public Vec3d axisY = new Vec3d(0.0D, 1.0D, 0.0D);
    public Vec3d axisZ = new Vec3d(0.0D, 0.0D, 1.0D);

    /** Center position in world coordinates */
    public Vec3d center;

    /** Local rotation offset for turrets or parts */
    public float localRotYaw = 0.0F;
    public float localRotPitch = 0.0F;
    public float localRotRoll = 0.0F;

    // ===== Constructors =====

    public MCH_BoundingBox(double x, double y, double z, float w, float h, float df) {
        this(x, y, z, w, h, w, df);
    }

    public MCH_BoundingBox(double posX, double posY, double posZ,
                           float widthX, float height, float widthZ, float df) {
        this.offsetX = posX;
        this.offsetY = posY;
        this.offsetZ = posZ;
        this.width = widthX;
        this.widthZ = widthZ;
        this.height = height;

        this.halfWidth = widthX / 2.0F;
        this.halfHeight = height / 2.0F;
        this.halfDepth = widthZ / 2.0F;

        this.damageFactor = df;

        this.center = new Vec3d(posX, posY, posZ);
        this.nowPos = new Vec3d(posX, posY, posZ);
        this.prevPos = new Vec3d(posX, posY, posZ);

        this.boundingBox = new AxisAlignedBB(
                posX - halfWidth, posY - halfHeight, posZ - halfDepth,
                posX + halfWidth, posY + halfHeight, posZ + halfDepth);
        this.backupBoundingBox = copyAABB(this.boundingBox);
    }

    /**
     * Creates a deep copy of this MCH_BoundingBox instance.
     */
    public MCH_BoundingBox copy() {
        MCH_BoundingBox bb = new MCH_BoundingBox(this.offsetX, this.offsetY, this.offsetZ,
                this.width, this.height, this.widthZ, this.damageFactor);
        bb.rotationYaw = this.rotationYaw;
        bb.rotationPitch = this.rotationPitch;
        bb.rotationRoll = this.rotationRoll;

        bb.axisX = new Vec3d(this.axisX.x, this.axisX.y, this.axisX.z);
        bb.axisY = new Vec3d(this.axisY.x, this.axisY.y, this.axisY.z);
        bb.axisZ = new Vec3d(this.axisZ.x, this.axisZ.y, this.axisZ.z);

        bb.center = new Vec3d(this.center.x, this.center.y, this.center.z);

        bb.halfWidth = this.halfWidth;
        bb.halfHeight = this.halfHeight;
        bb.halfDepth = this.halfDepth;

        if (this.rotatedOffset != null) {
            bb.rotatedOffset = new Vec3d(
                    this.rotatedOffset.x,
                    this.rotatedOffset.y,
                    this.rotatedOffset.z);
        }

        bb.nowPos = new Vec3d(this.nowPos.x, this.nowPos.y, this.nowPos.z);
        bb.prevPos = new Vec3d(this.prevPos.x, this.prevPos.y, this.prevPos.z);

        bb.boundingBox = copyAABB(this.boundingBox);
        bb.backupBoundingBox = copyAABB(this.backupBoundingBox);

        bb.boundingBoxType = this.boundingBoxType;
        bb.name = this.name;
        return bb;
    }

    /**
     * Updates the bounding box's state based on the entity's world position and orientation.
     * yaw, pitch, and roll are in degrees, using the same rotation order as MCH_Lib.RotVec3().
     */
    public void updatePosition(double posX, double posY, double posZ,
                               float yaw, float pitch, float roll) {
        this.rotationYaw = yaw;
        this.rotationPitch = pitch;
        this.rotationRoll = roll;

        float extraYaw = yaw;
        float extraPitch = pitch;
        float extraRoll = roll;

        if (this.boundingBoxType == EnumBoundingBoxType.TURRET) {
            extraYaw += localRotYaw;
            extraPitch += localRotPitch;
            extraRoll += localRotRoll;
        }

        // Compute rotated offset
        Vec3d localOffset = new Vec3d(offsetX, offsetY, offsetZ);
        rotatedOffset = MCH_Lib.RotVec3(localOffset, -extraYaw, -extraPitch, -extraRoll);

        // Update world center
        double cx = posX + rotatedOffset.x;
        double cy = posY + rotatedOffset.y;
        double cz = posZ + rotatedOffset.z;

        prevPos = nowPos;

        nowPos = new Vec3d(cx, cy, cz);

        center = new Vec3d(cx, cy, cz);

        // Update local axis unit vectors in world coordinates
        axisX = MCH_Lib.RotVec3(new Vec3d(1.0D, 0.0D, 0.0D), -extraYaw, -extraPitch, -extraRoll);
        axisY = MCH_Lib.RotVec3(new Vec3d(0.0D, 1.0D, 0.0D), -extraYaw, -extraPitch, -extraRoll);
        axisZ = MCH_Lib.RotVec3(new Vec3d(0.0D, 0.0D, 1.0D), -extraYaw, -extraPitch, -extraRoll);

        // Compute enclosing AABB for fast checks
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;

        for (int xi = -1; xi <= 1; xi += 2) {
            for (int yi = -1; yi <= 1; yi += 2) {
                for (int zi = -1; zi <= 1; zi += 2) {
                    Vec3d cornerLocal = new Vec3d(
                            offsetX + xi * halfWidth,
                            offsetY + yi * halfHeight,
                            offsetZ + zi * halfDepth);
                    Vec3d cornerWorld = MCH_Lib.RotVec3(cornerLocal, -extraYaw, -extraPitch, -extraRoll);
                    double px = posX + cornerWorld.x;
                    double py = posY + cornerWorld.y;
                    double pz = posZ + cornerWorld.z;

                    if (px < minX) minX = px;
                    if (py < minY) minY = py;
                    if (pz < minZ) minZ = pz;
                    if (px > maxX) maxX = px;
                    if (py > maxY) maxY = py;
                    if (pz > maxZ) maxZ = pz;
                }
            }
        }

        // Update AABB
        backupBoundingBox = copyAABB(boundingBox);
        boundingBox = new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Full OBB-OBB intersection check using the Separating Axis Theorem (SAT).
     */
    public boolean intersectsOBB(MCH_BoundingBox other) {
        Vec3d[] A = new Vec3d[] { this.axisX, this.axisY, this.axisZ };
        Vec3d[] B = new Vec3d[] { other.axisX, other.axisY, other.axisZ };
        double[] a = new double[] { this.halfWidth, this.halfHeight, this.halfDepth };
        double[] b = new double[] { other.halfWidth, other.halfHeight, other.halfDepth };

        double[][] R = new double[3][3];
        double[][] absR = new double[3][3];
        double EPS = 1.0E-6;

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                R[i][j] = A[i].dotProduct(B[j]);
                absR[i][j] = Math.abs(R[i][j]) + EPS;
            }
        }

        Vec3d d = other.center.subtract(this.center);
        double[] t = new double[] {
                d.dotProduct(A[0]),
                d.dotProduct(A[1]),
                d.dotProduct(A[2])
        };

        double ra, rb;

        // Test A's axes
        for (int i = 0; i < 3; i++) {
            ra = a[i];
            rb = b[0] * absR[i][0] + b[1] * absR[i][1] + b[2] * absR[i][2];
            if (Math.abs(t[i]) > ra + rb) return false;
        }

        // Test B's axes
        for (int j = 0; j < 3; j++) {
            ra = a[0] * absR[0][j] + a[1] * absR[1][j] + a[2] * absR[2][j];
            rb = b[j];
            double tProj = Math.abs(t[0] * R[0][j] + t[1] * R[1][j] + t[2] * R[2][j]);
            if (tProj > ra + rb) return false;
        }

        // Test cross products of A and B axes (A_i × B_j)
        // The 9 cross-axes are tested individually below...

        // A0 x B0
        ra = a[1] * absR[2][0] + a[2] * absR[1][0];
        rb = b[1] * absR[0][2] + b[2] * absR[0][1];
        if (Math.abs(t[2] * R[1][0] - t[1] * R[2][0]) > ra + rb) return false;

        // A0 x B1
        ra = a[1] * absR[2][1] + a[2] * absR[1][1];
        rb = b[0] * absR[0][2] + b[2] * absR[0][0];
        if (Math.abs(t[2] * R[1][1] - t[1] * R[2][1]) > ra + rb) return false;

        // A0 x B2
        ra = a[1] * absR[2][2] + a[2] * absR[1][2];
        rb = b[0] * absR[0][1] + b[1] * absR[0][0];
        if (Math.abs(t[2] * R[1][2] - t[1] * R[2][2]) > ra + rb) return false;

        // A1 x B0
        ra = a[0] * absR[2][0] + a[2] * absR[0][0];
        rb = b[1] * absR[1][2] + b[2] * absR[1][1];
        if (Math.abs(t[0] * R[2][0] - t[2] * R[0][0]) > ra + rb) return false;

        // A1 x B1
        ra = a[0] * absR[2][1] + a[2] * absR[0][1];
        rb = b[0] * absR[1][2] + b[2] * absR[1][0];
        if (Math.abs(t[0] * R[2][1] - t[2] * R[0][1]) > ra + rb) return false;

        // A1 x B2
        ra = a[0] * absR[2][2] + a[2] * absR[0][2];
        rb = b[0] * absR[1][1] + b[1] * absR[1][0];
        if (Math.abs(t[0] * R[2][2] - t[2] * R[0][2]) > ra + rb) return false;

        // A2 x B0
        ra = a[0] * absR[1][0] + a[1] * absR[0][0];
        rb = b[1] * absR[2][2] + b[2] * absR[2][1];
        if (Math.abs(t[1] * R[0][0] - t[0] * R[1][0]) > ra + rb) return false;

        // A2 x B1
        ra = a[0] * absR[1][1] + a[1] * absR[0][1];
        rb = b[0] * absR[2][2] + b[2] * absR[2][0];
        if (Math.abs(t[1] * R[0][1] - t[0] * R[1][1]) > ra + rb) return false;

        // A2 x B2
        ra = a[0] * absR[1][2] + a[1] * absR[0][2];
        rb = b[0] * absR[2][1] + b[1] * absR[2][0];
        if (Math.abs(t[1] * R[0][2] - t[0] * R[1][2]) > ra + rb) return false;

        return true; // No separating axis found; boxes intersect.
    }

    /**
     * Tests intersection between this OBB and an axis-aligned AABB using full SAT.
     */
    public boolean intersectsAABB(AxisAlignedBB aabb) {
        double cx = (aabb.minX + aabb.maxX) * 0.5;
        double cy = (aabb.minY + aabb.maxY) * 0.5;
        double cz = (aabb.minZ + aabb.maxZ) * 0.5;
        double hx = (aabb.maxX - aabb.minX) * 0.5;
        double hy = (aabb.maxY - aabb.minY) * 0.5;
        double hz = (aabb.maxZ - aabb.minZ) * 0.5;

        MCH_BoundingBox tmp = new MCH_BoundingBox(cx, cy, cz, (float) (hx * 2.0), (float) (hy * 2.0),
                (float) (hz * 2.0), 1.0F);
        tmp.center = new Vec3d(cx, cy, cz);
        tmp.halfWidth = (float) hx;
        tmp.halfHeight = (float) hy;
        tmp.halfDepth = (float) hz;
        tmp.axisX = new Vec3d(1.0D, 0.0D, 0.0D);
        tmp.axisY = new Vec3d(0.0D, 1.0D, 0.0D);
        tmp.axisZ = new Vec3d(0.0D, 0.0D, 1.0D);

        return this.intersectsOBB(tmp);
    }

    @Override
    public String toString() {
        return "MCH_BoundingBox{" +
                "boundingBox=" + boundingBox +
                ", backupBoundingBox=" + backupBoundingBox +
                ", offsetX=" + offsetX +
                ", offsetY=" + offsetY +
                ", offsetZ=" + offsetZ +
                ", width=" + width +
                ", widthZ=" + widthZ +
                ", height=" + height +
                ", halfWidth=" + halfWidth +
                ", halfHeight=" + halfHeight +
                ", halfDepth=" + halfDepth +
                ", rotatedOffset=" + rotatedOffset +
                ", nowPos=" + nowPos +
                ", prevPos=" + prevPos +
                ", damageFactor=" + damageFactor +
                ", boundingBoxType=" + boundingBoxType +
                ", name='" + name + '\'' +
                ", rotationYaw=" + rotationYaw +
                ", rotationPitch=" + rotationPitch +
                ", rotationRoll=" + rotationRoll +
                ", axisX=" + axisX +
                ", axisY=" + axisY +
                ", axisZ=" + axisZ +
                ", center=" + center +
                ", localRotYaw=" + localRotYaw +
                ", localRotPitch=" + localRotPitch +
                ", localRotRoll=" + localRotRoll +
                '}';
    }

    public static enum EnumBoundingBoxType {
        DEFAULT,
        ENGINE,
        TURRET
    }

    public static AxisAlignedBB copyAABB(AxisAlignedBB bb) {
        if (bb == null) {
            return null;
        }
        return new AxisAlignedBB(
                bb.minX, bb.minY, bb.minZ,
                bb.maxX, bb.maxY, bb.maxZ);
    }
}
