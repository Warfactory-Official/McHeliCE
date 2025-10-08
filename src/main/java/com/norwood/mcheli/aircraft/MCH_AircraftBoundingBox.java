package com.norwood.mcheli.aircraft;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

public class MCH_AircraftBoundingBox extends AxisAlignedBB {
    private final MCH_EntityAircraft ac;

    protected MCH_AircraftBoundingBox(MCH_EntityAircraft ac) {
        this(ac, ac.getEntityBoundingBox());
    }

    public MCH_AircraftBoundingBox(MCH_EntityAircraft ac, AxisAlignedBB aabb) {
        super(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
        this.ac = ac;
    }

    public AxisAlignedBB NewAABB(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new MCH_AircraftBoundingBox(this.ac, new AxisAlignedBB(x1, y1, z1, x2, y2, z2));
    }

    public double getDistanceSquareBetween  (AxisAlignedBB box1, AxisAlignedBB box2) {
        double centerX1 = (box1.minX + box1.maxX) / 2.0;
        double centerY1 = (box1.minY + box1.maxY) / 2.0;
        double centerZ1 = (box1.minZ + box1.maxZ) / 2.0;

        double centerX2 = (box2.minX + box2.maxX) / 2.0;
        double centerY2 = (box2.minY + box2.maxY) / 2.0;
        double centerZ2 = (box2.minZ + box2.maxZ) / 2.0;

        double dx = centerX1 - centerX2;
        double dy = centerY1 - centerY2;
        double dz = centerZ1 - centerZ2;

        return dx * dx + dy * dy + dz * dz;
    }


    public boolean intersects(@NotNull AxisAlignedBB aabb) {

        boolean ret = false;
        double dist = 1.0E7D;
        this.ac.lastBBDamageFactor = 1.0F;
        this.ac.lastBBName = null;

        // Still use the overall bounding box for a quick preliminary check
        if (super.intersects(aabb)) {
            dist = this.getDistanceSquareBetween(aabb, this);
            ret = true;
        }

        // Iterate through each component's bounding box
        for (MCH_BoundingBox bb : this.ac.extraBoundingBox) {
            // 先用部件的轴对齐外包围盒做快速过滤
            if (bb.boundingBox.intersects(aabb)) {
                // 用完整的 OBB-AABB 判定代替原来的 corners 判定
                if (bb.intersectsAABB(aabb)) {
                    double dist2 = this.getDistanceSquareBetween(aabb, this);
                    if (dist2 < dist) {
                        dist = dist2;
                        this.ac.lastBBDamageFactor = bb.damageFactor;
                        this.ac.lastBBName = bb.name;
                    }
                    ret = true;
                }
            }
        }
        return ret;
    }
    public @NotNull AxisAlignedBB grow(double x, double y, double z) {
        double newMinX = this.minX - x;
        double newMinY = this.minY - y;
        double newMinZ = this.minZ - z;
        double newMaxX = this.maxX + x;
        double newMaxY = this.maxY + y;
        double newMaxZ = this.maxZ + z;

        return this.NewAABB(newMinX, newMinY, newMinZ, newMaxX, newMaxY, newMaxZ);
    }


    public @NotNull AxisAlignedBB union(AxisAlignedBB other) {
        double d0 = Math.min(this.minX, other.minX);
        double d1 = Math.min(this.minY, other.minY);
        double d2 = Math.min(this.minZ, other.minZ);
        double d3 = Math.max(this.maxX, other.maxX);
        double d4 = Math.max(this.maxY, other.maxY);
        double d5 = Math.max(this.maxZ, other.maxZ);
        return this.NewAABB(d0, d1, d2, d3, d4, d5);
    }

    public @NotNull AxisAlignedBB expand(double x, double y, double z) {
        double minX = this.minX + Math.min(0.0, x);
        double maxX = this.maxX + Math.max(0.0, x);
        double minY = this.minY + Math.min(0.0, y);
        double maxY = this.maxY + Math.max(0.0, y);
        double minZ = this.minZ + Math.min(0.0, z);
        double maxZ = this.maxZ + Math.max(0.0, z);

        return this.NewAABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public @NotNull AxisAlignedBB contract(double x, double y, double z) {
        double d3 = this.minX + x;
        double d4 = this.minY + y;
        double d5 = this.minZ + z;
        double d6 = this.maxX - x;
        double d7 = this.maxY - y;
        double d8 = this.maxZ - z;
        return this.NewAABB(d3, d4, d5, d6, d7, d8);
    }

    public AxisAlignedBB copy() {
        return this.NewAABB(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
    }

    public AxisAlignedBB getOffsetBoundingBox(double x, double y, double z) {
        return this.NewAABB(this.minX + x, this.minY + y, this.minZ + z, this.maxX + x, this.maxY + y, this.maxZ + z);
    }

    /**
     * Returns the intersection of the bounding box with a ray.
     * If a component bounding box is closer, it will use the component instead.
     */
    public RayTraceResult calculateIntercept(Vec3d start, Vec3d end) {
        this.ac.lastBBDamageFactor = 1.0F;
        this.ac.lastBBName = null;

        // First check intersection with the main aircraft bounding box
        RayTraceResult bestResult = super.calculateIntercept(start, end);
        double bestDist = (bestResult != null) ? start.distanceTo(bestResult.hitVec) : Double.MAX_VALUE;

        // Iterate through the rotated component bounding boxes and perform OBB ray tracing
        for (MCH_BoundingBox bb : this.ac.extraBoundingBox) {
            // Transform the ray into the local coordinate system of the component box
            Vec3d dir = end.subtract(start);

            // Compute the ray components along the component box axes
            double dirX = dir.dotProduct(bb.axisX);
            double dirY = dir.dotProduct(bb.axisY);
            double dirZ = dir.dotProduct(bb.axisZ);

            // Vector from ray start to the component center
            Vec3d relStart = start.subtract(bb.center);
            double startX = relStart.dotProduct(bb.axisX);
            double startY = relStart.dotProduct(bb.axisY);
            double startZ = relStart.dotProduct(bb.axisZ);

            // Slab method: compute intersection t parameters for each axis
            double tMin = 0.0D;
            double tMax = 1.0D;
            boolean skip = false;

            // X axis
            if (Math.abs(dirX) > 1e-7) {
                double invDx = 1.0D / dirX;
                double t1 = (-bb.halfWidth - startX) * invDx;
                double t2 = (bb.halfWidth - startX) * invDx;
                if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
                tMin = Math.max(tMin, t1);
                tMax = Math.min(tMax, t2);
            } else if (Math.abs(startX) > bb.halfWidth + 1e-6) {
                skip = true;
            }

            // Y axis
            if (!skip) {
                if (Math.abs(dirY) > 1e-7) {
                    double invDy = 1.0D / dirY;
                    double t1 = (-bb.halfHeight - startY) * invDy;
                    double t2 = (bb.halfHeight - startY) * invDy;
                    if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
                    tMin = Math.max(tMin, t1);
                    tMax = Math.min(tMax, t2);
                } else if (Math.abs(startY) > bb.halfHeight + 1e-6) {
                    skip = true;
                }
            }

            // Z axis
            if (!skip) {
                if (Math.abs(dirZ) > 1e-7) {
                    double invDz = 1.0D / dirZ;
                    double t1 = (-bb.halfDepth - startZ) * invDz;
                    double t2 = (bb.halfDepth - startZ) * invDz;
                    if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
                    tMin = Math.max(tMin, t1);
                    tMax = Math.min(tMax, t2);
                } else if (Math.abs(startZ) > bb.halfDepth + 1e-6) {
                    skip = true;
                }
            }

            // Check if the ray intersects this component box
            if (!skip && tMax >= tMin && tMin <= 1.0D && tMax >= 0.0D) {
                double tHit = (tMin < 0.0D) ? tMax : tMin;

                // Compute intersection point
                Vec3d hit = start.add(dir.scale(tHit));
                double dist = start.distanceTo(hit);

                // If this hit is closer than the previous closest, update result
                if (dist < bestDist) {

                    bestDist = dist;
                    Vec3d hitVec = new Vec3d(hit.x, hit.y, hit.z);
                    bestResult = new RayTraceResult(hitVec, EnumFacing.DOWN, new BlockPos(hitVec) ); //Yes, the facing is set to DOWN (0), no I don't know if its a good idea
                    this.ac.lastBBDamageFactor = bb.damageFactor;
                    this.ac.lastBBName = bb.name;
                }
            }
        }

        return bestResult;
    }

}
