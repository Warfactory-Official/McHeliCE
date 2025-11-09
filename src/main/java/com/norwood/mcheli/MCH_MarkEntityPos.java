package com.norwood.mcheli;

import java.nio.FloatBuffer;

import javax.annotation.Nullable;

import org.lwjgl.BufferUtils;

import com.norwood.mcheli.helper.entity.ITargetMarkerObject;

public class MCH_MarkEntityPos {

    public final FloatBuffer pos;
    public final int type;
    private final ITargetMarkerObject target;

    public MCH_MarkEntityPos(int type, ITargetMarkerObject target) {
        this.type = type;
        this.pos = BufferUtils.createFloatBuffer(3);
        this.target = target;
    }

    public MCH_MarkEntityPos(int type) {
        this(type, null);
    }

    @Nullable
    public ITargetMarkerObject getTarget() {
        return this.target;
    }
}
