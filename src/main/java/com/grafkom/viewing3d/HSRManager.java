package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL11.*;

public class HSRManager {
    private static boolean depthOn, cullOn;

    public static void setupDepthTest(boolean enable) {
        depthOn = enable;
        if (enable) {
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LESS);       
        } else {
            glDisable(GL_DEPTH_TEST);  
        }
    }

    public static void setupBackFaceCulling(boolean enable) {
        cullOn = enable;
        if (enable) {
            glEnable(GL_CULL_FACE);
            glCullFace(GL_BACK);       
            glFrontFace(GL_CCW);        
        } else {
            glDisable(GL_CULL_FACE);
        }
    }

    public static boolean isDepthOn() { return depthOn; }
    public static boolean isCullOn() { return cullOn; }
}
