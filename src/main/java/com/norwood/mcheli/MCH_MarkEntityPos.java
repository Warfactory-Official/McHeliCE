package com.norwood.mcheli;

import com.norwood.mcheli.helper.entity.ITargetMarkerObject;
import org.lwjgl.BufferUtils;

import javax.annotation.Nullable;
import java.nio.FloatBuffer;

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
