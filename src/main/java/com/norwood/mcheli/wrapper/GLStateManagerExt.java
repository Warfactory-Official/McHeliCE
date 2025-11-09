package com.norwood.mcheli.wrapper;


import lombok.Getter;
import org.lwjgl.opengl.GL11;

/** Small extension to GlstateManager that keeps track of point size, decreasing gl calls
 *
 */
public class GLStateManagerExt {

    private static float prevPointSize;
    @Getter
    private static float pointSize;

    public static  void setPointSize(float size){
        if(size == pointSize) return;
        prevPointSize = pointSize;
        GL11.glPointSize(size);
        pointSize = size;
    }

    public static  void restorePointSize(){
        if(pointSize == prevPointSize) return;
        pointSize = prevPointSize;
        GL11.glPointSize(prevPointSize);

    }

    public static void pollSize(){
       pointSize = GL11.glGetInteger(2833);
    }

}
