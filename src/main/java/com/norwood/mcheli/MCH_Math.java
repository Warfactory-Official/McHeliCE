package com.norwood.mcheli;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import net.minecraft.util.math.MathHelper;

public class MCH_Math {

    public static final float PI = (float) Math.PI;
    public static final MCH_Math instance = new MCH_Math();

    public static MCH_Math.FVector3D newVec3D() {
        return instance.privateNewVec3D(0.0F, 0.0F, 0.0F);
    }

    public static MCH_Math.FVector3D newVec3D(float x, float y, float z) {
        return instance.privateNewVec3D(x, y, z);
    }

    public static MCH_Math.FQuat newQuat() {
        return instance.privateNewQuat();
    }

    public static MCH_Math.FMatrix newMatrix() {
        return instance.privateNewMatrix();
    }

    public static MCH_Math.FQuat EulerToQuatTestNG(float yaw, float pitch, float roll) {
        MCH_Math.FVector3D axis = newVec3D();
        float rot = VecNormalize(axis);
        MCH_Math.FQuat dqtn = newQuat();
        QuatRotation(dqtn, rot, axis.x, axis.y, axis.z);
        return dqtn;
    }

    public static MCH_Math.FMatrix EulerToMatrix(float yaw, float pitch, float roll) {
        MCH_Math.FMatrix m = newMatrix();
        MatTurnZ(m, roll / 180.0F * PI);
        MatTurnX(m, pitch / 180.0F * PI);
        MatTurnY(m, yaw / 180.0F * PI);
        return m;
    }

    public static MCH_Math.FQuat EulerToQuat(float yaw, float pitch, float roll) {
        MCH_Math.FQuat dqtn = newQuat();
        MatrixToQuat(dqtn, EulerToMatrix(yaw, pitch, roll));
        return dqtn;
    }

    public static MCH_Math.FVector3D QuatToEuler(MCH_Math.FQuat q) {
        MCH_Math.FMatrix m = QuatToMatrix(q);
        return MatrixToEuler(m);
    }

    public static MCH_Math.FVector3D MatrixToEuler(MCH_Math.FMatrix m) {
        float xx = m.m00;
        float xy = m.m01;
        float xz = m.m02;
        float yy = m.m11;
        float zx = m.m20;
        float zy = m.m21;
        float zz = m.m22;
        float b = (float) (-Math.asin(zy));
        float cosB = Cos(b);
        float a;
        float c;
        if (Math.abs(cosB) >= 1.0E-4) {
            c = Atan2(zx, zz);
            float xy_cos = xy / cosB;
            if (xy_cos > 1.0F) {
                xy_cos = 1.0F;
            } else if (xy_cos < -1.0F) {
                xy_cos = -1.0F;
            }

            a = (float) Math.asin(xy_cos);
            if (Float.isNaN(a)) {
                a = 0.0F;
            }
        } else {
            c = Atan2(-xz, xx);
            a = 0.0F;
        }

        a = (float) (a * (180.0 / PI));
        b = (float) (b * (180.0 / PI));
        c = (float) (c * (180.0 / PI));
        if (yy < 0.0F) {
            a = 180.0F - a;
        }

        return newVec3D(-b, -c, -a);
    }

    public static float SIGN(float x) {
        return x >= 0.0F ? 1.0F : -1.0F;
    }

    public static float NORM(float a, float b, float c, float d) {
        return (float) Math.sqrt(a * a + b * b + c * c + d * d);
    }

    public static void QuatNormalize(MCH_Math.FQuat q) {
        float r = NORM(q.w, q.x, q.y, q.z);
        if (MathHelper.abs(r) > 1.0E-4) {
            q.w /= r;
            q.x /= r;
            q.y /= r;
            q.z /= r;
        }
    }

    public static boolean MatrixToQuat(MCH_Math.FQuat q, MCH_Math.FMatrix m) {
        q.w = (m.m00 + m.m11 + m.m22 + 1.0F) / 4.0F;
        q.x = (m.m00 - m.m11 - m.m22 + 1.0F) / 4.0F;
        q.y = (-m.m00 + m.m11 - m.m22 + 1.0F) / 4.0F;
        q.z = (-m.m00 - m.m11 + m.m22 + 1.0F) / 4.0F;
        if (q.w < 0.0F) {
            q.w = 0.0F;
        }

        if (q.x < 0.0F) {
            q.x = 0.0F;
        }

        if (q.y < 0.0F) {
            q.y = 0.0F;
        }

        if (q.z < 0.0F) {
            q.z = 0.0F;
        }

        q.w = (float) Math.sqrt(q.w);
        q.x = (float) Math.sqrt(q.x);
        q.y = (float) Math.sqrt(q.y);
        q.z = (float) Math.sqrt(q.z);
        if (q.w >= q.x && q.w >= q.y && q.w >= q.z) {
            q.w *= 1.0F;
            q.x = q.x * SIGN(m.m21 - m.m12);
            q.y = q.y * SIGN(m.m02 - m.m20);
            q.z = q.z * SIGN(m.m10 - m.m01);
        } else if (q.x >= q.w && q.x >= q.y && q.x >= q.z) {
            q.w = q.w * SIGN(m.m21 - m.m12);
            q.x *= 1.0F;
            q.y = q.y * SIGN(m.m10 + m.m01);
            q.z = q.z * SIGN(m.m02 + m.m20);
        } else if (q.y >= q.w && q.y >= q.x && q.y >= q.z) {
            q.w = q.w * SIGN(m.m02 - m.m20);
            q.x = q.x * SIGN(m.m10 + m.m01);
            q.y *= 1.0F;
            q.z = q.z * SIGN(m.m21 + m.m12);
        } else {

            q.w = q.w * SIGN(m.m10 - m.m01);
            q.x = q.x * SIGN(m.m20 + m.m02);
            q.y = q.y * SIGN(m.m21 + m.m12);
            q.z *= 1.0F;
        }

        correctQuat(q);
        float r = NORM(q.w, q.x, q.y, q.z);
        q.w /= r;
        q.x /= r;
        q.y /= r;
        q.z /= r;
        correctQuat(q);
        return true;
    }

    public static void correctQuat(MCH_Math.FQuat q) {
        if (Float.isNaN(q.w) || Float.isInfinite(q.w)) {
            q.w = 0.0F;
        }

        if (Float.isNaN(q.x) || Float.isInfinite(q.x)) {
            q.x = 0.0F;
        }

        if (Float.isNaN(q.y) || Float.isInfinite(q.y)) {
            q.y = 0.0F;
        }

        if (Float.isNaN(q.z) || Float.isInfinite(q.z)) {
            q.z = 0.0F;
        }
    }

    public static MCH_Math.FQuat motionTest(int x, int y, MCH_Math.FQuat prevQtn) {
        MCH_Math.FVector3D axis = newVec3D();
        MCH_Math.FQuat dqtn = newQuat();
        axis.x = 2.0F * PI * y / 200.0F;
        axis.y = 2.0F * PI * x / 200.0F;
        axis.z = 0.0F;
        float rot = VecNormalize(axis);
        QuatRotation(dqtn, rot, axis.x, axis.y, axis.z);
        return QuatMult(dqtn, prevQtn);
    }

    public static float Sin(float rad) {
        return (float) Math.sin(rad);
    }

    public static float Cos(float rad) {
        return (float) Math.cos(rad);
    }

    public static float Tan(float rad) {
        return (float) Math.tan(rad);
    }

    public static float Floor(float x) {
        return (float) Math.floor(x);
    }

    public static float Atan(float x) {
        return (float) Math.atan(x);
    }

    public static float Atan2(float y, float x) {
        return (float) Math.atan2(y, x);
    }

    public static float Fabs(float x) {
        return x >= 0.0F ? x : -x;
    }

    public static float InvSqrt(float x) {
        return 1.0F / (float) Math.sqrt(x);
    }

    public static float Pow(float a, float b) {
        return (float) Math.pow(a, b);
    }

    public static float VecNormalize(MCH_Math.FVector3D lpV) {
        float len2 = lpV.x * lpV.x + lpV.y * lpV.y + lpV.z * lpV.z;
        float length = (float) Math.sqrt(len2);
        if (length == 0.0F) {
            return 0.0F;
        } else {
            float invLength = 1.0F / length;
            lpV.x *= invLength;
            lpV.y *= invLength;
            lpV.z *= invLength;
            return length;
        }
    }

    public static float Vec2DNormalize(MCH_Math.FVector2D lpV) {
        float len2 = lpV.x * lpV.x + lpV.y * lpV.y;
        float length = (float) Math.sqrt(len2);
        if (length == 0.0F) {
            return 0.0F;
        } else {
            float invLength = 1.0F / length;
            lpV.x *= invLength;
            lpV.y *= invLength;
            return length;
        }
    }

    public static MCH_Math.FVector3D MatVector(MCH_Math.FMatrix lpM, MCH_Math.FVector3D lpV) {
        MCH_Math.FVector3D lpS = newVec3D();
        float x = lpV.x;
        float y = lpV.y;
        float z = lpV.z;
        lpS.x = lpM.m00 * x + lpM.m01 * y + lpM.m02 * z + lpM.m03;
        lpS.y = lpM.m10 * x + lpM.m11 * y + lpM.m12 * z + lpM.m13;
        lpS.z = lpM.m20 * x + lpM.m21 * y + lpM.m22 * z + lpM.m23;
        return lpS;
    }

    public static MCH_Math.FVector3D MatDirection(MCH_Math.FMatrix lpM, MCH_Math.FVector3D lpDir) {
        MCH_Math.FVector3D lpSDir = newVec3D();
        float x = lpDir.x;
        float y = lpDir.y;
        float z = lpDir.z;
        lpSDir.x = lpM.m00 * x + lpM.m01 * y + lpM.m02 * z;
        lpSDir.y = lpM.m10 * x + lpM.m11 * y + lpM.m12 * z;
        lpSDir.z = lpM.m20 * x + lpM.m21 * y + lpM.m22 * z;
        return lpSDir;
    }

    public static void MatIdentity(MCH_Math.FMatrix lpM) {
        lpM.m01 = lpM.m02 = lpM.m03 = lpM.m10 = lpM.m12 = lpM.m13 = lpM.m20 = lpM.m21 = lpM.m23 = lpM.m30 = lpM.m31 = lpM.m32 = 0.0F;
        lpM.m00 = lpM.m11 = lpM.m22 = lpM.m33 = 1.0F;
    }

    public static void MatCopy(MCH_Math.FMatrix lpMa, MCH_Math.FMatrix lpMb) {
        lpMa.m00 = lpMb.m00;
        lpMa.m10 = lpMb.m10;
        lpMa.m20 = lpMb.m20;
        lpMa.m30 = lpMb.m30;
        lpMa.m01 = lpMb.m01;
        lpMa.m11 = lpMb.m11;
        lpMa.m21 = lpMb.m21;
        lpMa.m31 = lpMb.m31;
        lpMa.m02 = lpMb.m02;
        lpMa.m12 = lpMb.m12;
        lpMa.m22 = lpMb.m22;
        lpMa.m32 = lpMb.m32;
        lpMa.m03 = lpMb.m03;
        lpMa.m13 = lpMb.m13;
        lpMa.m23 = lpMb.m23;
        lpMa.m33 = lpMb.m33;
    }

    public static void MatTranslate(MCH_Math.FMatrix m, float x, float y, float z) {
        float m30 = m.m30;
        float m31 = m.m31;
        float m32 = m.m32;
        float m33 = m.m33;
        m.m00 += m30 * x;
        m.m01 += m31 * x;
        m.m02 += m32 * x;
        m.m03 += m33 * x;
        m.m10 += m30 * y;
        m.m11 += m31 * y;
        m.m12 += m32 * y;
        m.m13 += m33 * y;
        m.m20 += m30 * z;
        m.m21 += m31 * z;
        m.m22 += m32 * z;
        m.m23 += m33 * z;
    }

    public static void MatMove(MCH_Math.FMatrix m, float x, float y, float z) {
        m.m03 = m.m03 + (m.m00 * x + m.m01 * y + m.m02 * z);
        m.m13 = m.m13 + (m.m10 * x + m.m11 * y + m.m12 * z);
        m.m23 = m.m23 + (m.m20 * x + m.m21 * y + m.m22 * z);
        m.m33 = m.m33 + (m.m30 * x + m.m31 * y + m.m32 * z);
    }

    public static void MatRotateX(MCH_Math.FMatrix m, float rad) {
        if (rad > 2.0F * PI || rad < -2.0F * PI) {
            rad -= 2.0F * PI * (int) (rad / (2.0F * PI));
        }

        float cosA = Cos(rad);
        float sinA = Sin(rad);
        float tmp1 = m.m10;
        float tmp2 = m.m20;
        m.m10 = cosA * tmp1 - sinA * tmp2;
        m.m20 = sinA * tmp1 + cosA * tmp2;
        tmp1 = m.m11;
        tmp2 = m.m21;
        m.m11 = cosA * tmp1 - sinA * tmp2;
        m.m21 = sinA * tmp1 + cosA * tmp2;
        tmp1 = m.m12;
        tmp2 = m.m22;
        m.m12 = cosA * tmp1 - sinA * tmp2;
        m.m22 = sinA * tmp1 + cosA * tmp2;
        tmp1 = m.m13;
        tmp2 = m.m23;
        m.m13 = cosA * tmp1 - sinA * tmp2;
        m.m23 = sinA * tmp1 + cosA * tmp2;
    }

    public static void MatRotateY(MCH_Math.FMatrix m, float rad) {
        if (rad > 2.0F * PI || rad < -2.0F * PI) {
            rad -= 2.0F * PI * (int) (rad / (2.0F * PI));
        }

        float cosA = Cos(rad);
        float sinA = Sin(rad);
        float tmp1 = m.m00;
        float tmp2 = m.m20;
        m.m00 = cosA * tmp1 + sinA * tmp2;
        m.m20 = -sinA * tmp1 + cosA * tmp2;
        tmp1 = m.m01;
        tmp2 = m.m21;
        m.m01 = cosA * tmp1 + sinA * tmp2;
        m.m21 = -sinA * tmp1 + cosA * tmp2;
        tmp1 = m.m02;
        tmp2 = m.m22;
        m.m02 = cosA * tmp1 + sinA * tmp2;
        m.m22 = -sinA * tmp1 + cosA * tmp2;
        tmp1 = m.m03;
        tmp2 = m.m23;
        m.m03 = cosA * tmp1 + sinA * tmp2;
        m.m23 = -sinA * tmp1 + cosA * tmp2;
    }

    public static void MatRotateZ(MCH_Math.FMatrix m, float rad) {
        if (rad > 2.0F * PI || rad < -2.0F * PI) {
            rad -= 2.0F * PI * (int) (rad / (2.0F * PI));
        }

        float cosA = Cos(rad);
        float sinA = Sin(rad);
        float tmp1 = m.m00;
        float tmp2 = m.m10;
        m.m00 = cosA * tmp1 - sinA * tmp2;
        m.m10 = sinA * tmp1 + cosA * tmp2;
        tmp1 = m.m01;
        tmp2 = m.m11;
        m.m01 = cosA * tmp1 - sinA * tmp2;
        m.m11 = sinA * tmp1 + cosA * tmp2;
        tmp1 = m.m02;
        tmp2 = m.m12;
        m.m02 = cosA * tmp1 - sinA * tmp2;
        m.m12 = sinA * tmp1 + cosA * tmp2;
        tmp1 = m.m03;
        tmp2 = m.m13;
        m.m03 = cosA * tmp1 - sinA * tmp2;
        m.m13 = sinA * tmp1 + cosA * tmp2;
    }

    public static void MatTurnX(MCH_Math.FMatrix m, float rad) {
        if (rad > 2.0F * PI || rad < -2.0F * PI) {
            rad -= 2.0F * PI * (int) (rad / (2.0F * PI));
        }

        float cosA = Cos(rad);
        float sinA = Sin(rad);
        float tmp1 = m.m01;
        float tmp2 = m.m02;
        m.m01 = cosA * tmp1 + sinA * tmp2;
        m.m02 = -sinA * tmp1 + cosA * tmp2;
        tmp1 = m.m11;
        tmp2 = m.m12;
        m.m11 = cosA * tmp1 + sinA * tmp2;
        m.m12 = -sinA * tmp1 + cosA * tmp2;
        tmp1 = m.m21;
        tmp2 = m.m22;
        m.m21 = cosA * tmp1 + sinA * tmp2;
        m.m22 = -sinA * tmp1 + cosA * tmp2;
        tmp1 = m.m31;
        tmp2 = m.m32;
        m.m31 = cosA * tmp1 + sinA * tmp2;
        m.m32 = -sinA * tmp1 + cosA * tmp2;
    }

    public static void MatTurnY(MCH_Math.FMatrix m, float rad) {
        if (rad > 2.0F * PI || rad < -2.0F * PI) {
            rad -= 2.0F * PI * (int) (rad / (2.0F * PI));
        }

        float cosA = Cos(rad);
        float sinA = Sin(rad);
        float tmp1 = m.m00;
        float tmp2 = m.m02;
        m.m00 = cosA * tmp1 - sinA * tmp2;
        m.m02 = sinA * tmp1 + cosA * tmp2;
        tmp1 = m.m10;
        tmp2 = m.m12;
        m.m10 = cosA * tmp1 - sinA * tmp2;
        m.m12 = sinA * tmp1 + cosA * tmp2;
        tmp1 = m.m20;
        tmp2 = m.m22;
        m.m20 = cosA * tmp1 - sinA * tmp2;
        m.m22 = sinA * tmp1 + cosA * tmp2;
        tmp1 = m.m30;
        tmp2 = m.m32;
        m.m30 = cosA * tmp1 - sinA * tmp2;
        m.m32 = sinA * tmp1 + cosA * tmp2;
    }

    public static void MatTurnZ(MCH_Math.FMatrix m, float rad) {
        if (rad > 2.0F * PI || rad < -2.0F * PI) {
            rad -= 2.0F * PI * (int) (rad / (2.0F * PI));
        }

        float cosA = Cos(rad);
        float sinA = Sin(rad);
        float tmp1 = m.m00;
        float tmp2 = m.m01;
        m.m00 = cosA * tmp1 + sinA * tmp2;
        m.m01 = -sinA * tmp1 + cosA * tmp2;
        tmp1 = m.m10;
        tmp2 = m.m11;
        m.m10 = cosA * tmp1 + sinA * tmp2;
        m.m11 = -sinA * tmp1 + cosA * tmp2;
        tmp1 = m.m20;
        tmp2 = m.m21;
        m.m20 = cosA * tmp1 + sinA * tmp2;
        m.m21 = -sinA * tmp1 + cosA * tmp2;
        tmp1 = m.m30;
        tmp2 = m.m31;
        m.m30 = cosA * tmp1 + sinA * tmp2;
        m.m31 = -sinA * tmp1 + cosA * tmp2;
    }

    public static void MatScale(MCH_Math.FMatrix lpM, float scalex, float scaley, float scalez) {
        lpM.m00 = scalex * lpM.m00;
        lpM.m01 = scalex * lpM.m01;
        lpM.m02 = scalex * lpM.m02;
        lpM.m03 = scalex * lpM.m03;
        lpM.m10 = scaley * lpM.m10;
        lpM.m11 = scaley * lpM.m11;
        lpM.m12 = scaley * lpM.m12;
        lpM.m13 = scaley * lpM.m13;
        lpM.m20 = scalez * lpM.m20;
        lpM.m21 = scalez * lpM.m21;
        lpM.m22 = scalez * lpM.m22;
        lpM.m23 = scalez * lpM.m23;
    }

    public static void MatSize(MCH_Math.FMatrix lpM, float scalex, float scaley, float scalez) {
        lpM.m00 = scalex * lpM.m00;
        lpM.m01 = scaley * lpM.m01;
        lpM.m02 = scalez * lpM.m02;
        lpM.m10 = scalex * lpM.m10;
        lpM.m11 = scaley * lpM.m11;
        lpM.m12 = scalez * lpM.m12;
        lpM.m20 = scalex * lpM.m20;
        lpM.m21 = scaley * lpM.m21;
        lpM.m22 = scalez * lpM.m22;
        lpM.m30 = scalex * lpM.m30;
        lpM.m31 = scaley * lpM.m31;
        lpM.m32 = scalez * lpM.m32;
    }

    public static MCH_Math.FQuat QuatMult(MCH_Math.FQuat lpP, MCH_Math.FQuat lpQ) {
        MCH_Math.FQuat lpR = newQuat();
        float pw = lpP.w;
        float px = lpP.x;
        float py = lpP.y;
        float pz = lpP.z;
        float qw = lpQ.w;
        float qx = lpQ.x;
        float qy = lpQ.y;
        float qz = lpQ.z;
        lpR.w = pw * qw - px * qx - py * qy - pz * qz;
        lpR.x = pw * qx + px * qw + py * qz - pz * qy;
        lpR.y = pw * qy - px * qz + py * qw + pz * qx;
        lpR.z = pw * qz + px * qy - py * qx + pz * qw;
        return lpR;
    }

    public static void QuatAdd(MCH_Math.FQuat q_out, MCH_Math.FQuat q) {
        q_out.w = q_out.w + q.w;
        q_out.x = q_out.x + q.x;
        q_out.y = q_out.y + q.y;
        q_out.z = q_out.z + q.z;
    }

    public static MCH_Math.FMatrix QuatToMatrix(MCH_Math.FQuat lpQ) {
        MCH_Math.FMatrix lpM = newMatrix();
        float qw = lpQ.w;
        float qx = lpQ.x;
        float qy = lpQ.y;
        float qz = lpQ.z;
        float x2 = 2.0F * qx * qx;
        float y2 = 2.0F * qy * qy;
        float z2 = 2.0F * qz * qz;
        float xy = 2.0F * qx * qy;
        float yz = 2.0F * qy * qz;
        float zx = 2.0F * qz * qx;
        float wx = 2.0F * qw * qx;
        float wy = 2.0F * qw * qy;
        float wz = 2.0F * qw * qz;
        lpM.m00 = 1.0F - y2 - z2;
        lpM.m01 = xy - wz;
        lpM.m02 = zx + wy;
        lpM.m03 = 0.0F;
        lpM.m10 = xy + wz;
        lpM.m11 = 1.0F - z2 - x2;
        lpM.m12 = yz - wx;
        lpM.m13 = 0.0F;
        lpM.m20 = zx - wy;
        lpM.m21 = yz + wx;
        lpM.m22 = 1.0F - x2 - y2;
        lpM.m23 = 0.0F;
        lpM.m30 = lpM.m31 = lpM.m32 = 0.0F;
        lpM.m33 = 1.0F;
        return lpM;
    }

    public static void QuatRotation(MCH_Math.FQuat lpQ, float rad, float ax, float ay, float az) {
        float hrad = 0.5F * rad;
        float s = Sin(hrad);
        lpQ.w = Cos(hrad);
        lpQ.x = s * ax;
        lpQ.y = s * ay;
        lpQ.z = s * az;
    }

    public static void QuatIdentity(MCH_Math.FQuat lpQ) {
        lpQ.w = 1.0F;
        lpQ.x = 0.0F;
        lpQ.y = 0.0F;
        lpQ.z = 0.0F;
    }

    public static void QuatCopy(MCH_Math.FQuat lpTo, MCH_Math.FQuat lpFrom) {
        lpTo.w = lpFrom.w;
        lpTo.x = lpFrom.x;
        lpTo.y = lpFrom.y;
        lpTo.z = lpFrom.z;
    }

    public MCH_Math.FVector3D privateNewVec3D(float x, float y, float z) {
        MCH_Math.FVector3D v = new FVector3D(this);
        v.x = x;
        v.y = y;
        v.z = z;
        return v;
    }

    private MCH_Math.FQuat privateNewQuat() {
        MCH_Math.FQuat q = new FQuat(this);
        QuatIdentity(q);
        return new FQuat(this);
    }

    private MCH_Math.FMatrix privateNewMatrix() {
        MCH_Math.FMatrix m = new FMatrix(this);
        MatIdentity(m);
        return m;
    }

    public static class FMatrix {

        float m00;
        float m10;
        float m20;
        float m30;
        float m01;
        float m11;
        float m21;
        float m31;
        float m02;
        float m12;
        float m22;
        float m32;
        float m03;
        float m13;
        float m23;
        float m33;

        public FMatrix(MCH_Math paramMCH_Math) {}

        public FloatBuffer toFloatBuffer() {
            ByteBuffer bb = ByteBuffer.allocateDirect(64);
            FloatBuffer fb = bb.asFloatBuffer();
            fb.put(this.m00);
            fb.put(this.m10);
            fb.put(this.m20);
            fb.put(this.m30);
            fb.put(this.m01);
            fb.put(this.m11);
            fb.put(this.m21);
            fb.put(this.m31);
            fb.put(this.m02);
            fb.put(this.m12);
            fb.put(this.m22);
            fb.put(this.m32);
            fb.put(this.m03);
            fb.put(this.m13);
            fb.put(this.m23);
            fb.put(this.m33);
            float f = fb.get(0);
            f = fb.get(1);
            fb.position(0);
            return fb;
        }
    }

    public static class FQuat {

        public float w;
        public float x;
        public float y;
        public float z;

        public FQuat(MCH_Math paramMCH_Math) {}
    }

    public static class FVector2D {

        public float x;
        public float y;

        public FVector2D(MCH_Math paramMCH_Math) {}
    }

    public static class FVector3D {

        public float x;
        public float y;
        public float z;

        public FVector3D(MCH_Math paramMCH_Math) {}
    }
}
