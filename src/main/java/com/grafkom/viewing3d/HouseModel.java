package com.grafkom.viewing3d;

import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;


public class HouseModel {

    public static final float HALF_W = 2.0f;    
    public static final float HALF_D = 1.8f;    
    public static final float WALL_H = 2.4f;    
    public static final float RIDGE_H = 3.9f;   
    public static final float EAVE_OUT = 0.4f;  
    public static final float GABLE_OUT = 0.3f; 
    public static final float ROOF_T = 0.14f;   

    public static List<PartData> build() {
        List<PartData> parts = new ArrayList<>();

        MeshBuilder ground = new MeshBuilder();
        float g = 14f;
        ground.addQuad(new Vector3f(-g, 0, g), new Vector3f(g, 0, g), new Vector3f(g, 0, -g), new Vector3f(-g, 0, -g),
                new Vector2f(0, 0), new Vector2f(g * 2, 0), new Vector2f(g * 2, g * 2), new Vector2f(0, g * 2),
                new Vector3f(0, -1, 0));
        parts.add(new PartData("Halaman (ground plane)", ground, "grass", 0.05f, 8f, false));

        MeshBuilder base = new MeshBuilder();
        base.addBox(new Vector3f(-HALF_W - 0.08f, 0, -HALF_D - 0.08f),
                    new Vector3f(HALF_W + 0.08f, 0.25f, HALF_D + 0.08f), 1.0f, false, false, true);
        base.addBox(new Vector3f(-0.95f, 0, HALF_D), new Vector3f(0.95f, 0.12f, HALF_D + 0.65f), 1.0f, false, false, true);
        parts.add(new PartData("Pondasi & teras", base, "concrete", 0.15f, 16f, true));

        MeshBuilder walls = new MeshBuilder();
        walls.addBox(new Vector3f(-HALF_W, 0.0f, -HALF_D), new Vector3f(HALF_W, WALL_H, HALF_D), 1.0f, false, false, false);
        
        walls.addBox(new Vector3f(1.0f, 2.2f, -1.15f), new Vector3f(1.5f, 4.7f, -0.65f), 1.0f, false, false, true);
        parts.add(new PartData("Dinding bata & cerobong", walls, "brick", 0.10f, 12f, true));

        float roofZ = HALF_D + EAVE_OUT;                            
        float roofEaveY = WALL_H - 0.1f;                            
        float roofSlope = (RIDGE_H + 0.02f - roofEaveY) / roofZ;    
        float gableRidge = WALL_H + roofSlope * HALF_D;             
        MeshBuilder gable = new MeshBuilder();
        Vector3f[] gv = {
                new Vector3f(-HALF_W, WALL_H, -HALF_D), new Vector3f(-HALF_W, WALL_H, HALF_D), new Vector3f(-HALF_W, gableRidge, 0),
                new Vector3f(HALF_W, WALL_H, -HALF_D), new Vector3f(HALF_W, WALL_H, HALF_D), new Vector3f(HALF_W, gableRidge, 0)};
        gable.addConvexSolid(gv, new int[][]{
                {0, 1, 2},            
                {3, 4, 5},            
                {1, 4, 5, 2},         
                {0, 3, 5, 2}},        
                1.0f);
        parts.add(new PartData("Gable (dinding segitiga)", gable, "wood", 0.20f, 20f, true));

        MeshBuilder roof = new MeshBuilder();
        float x0 = -HALF_W - GABLE_OUT, x1 = HALF_W + GABLE_OUT;
        for (int side = -1; side <= 1; side += 2) {
            float zE = side * roofZ;
            Vector3f[] r = {
                    new Vector3f(x0, roofEaveY, zE), new Vector3f(x1, roofEaveY, zE),
                    new Vector3f(x1, RIDGE_H + 0.02f, 0), new Vector3f(x0, RIDGE_H + 0.02f, 0)};
            Vector3f[] v = new Vector3f[8];
            for (int i = 0; i < 4; i++) { v[i] = r[i]; v[i + 4] = new Vector3f(r[i]).sub(0, ROOF_T, 0); }
            roof.addConvexSolid(v, new int[][]{
                    {0, 1, 2, 3},   
                    {4, 5, 6, 7},   
                    {0, 1, 5, 4},   
                    {0, 3, 7, 4},   
                    {1, 2, 6, 5},   
                    {3, 2, 6, 7}},  
                    0.9f);
        }
        parts.add(new PartData("Atap genteng", roof, "roof", 0.25f, 24f, true));

        MeshBuilder door = new MeshBuilder();
        door.addBox(new Vector3f(-0.45f, 0.12f, HALF_D - 0.02f), new Vector3f(0.45f, 1.95f, HALF_D + 0.15f), 1f, true, false, true);
        parts.add(new PartData("Pintu", door, "door", 0.20f, 24f, true));

        MeshBuilder win = new MeshBuilder();
        for (float cx : new float[]{-1.2f, 1.2f})
            win.addBox(new Vector3f(cx - 0.4f, 1.0f, HALF_D - 0.02f), new Vector3f(cx + 0.4f, 1.9f, HALF_D + 0.06f), 1f, true, true, true);
        win.addBox(new Vector3f(-0.5f, 1.0f, -HALF_D - 0.06f), new Vector3f(0.5f, 1.9f, -HALF_D + 0.02f), 1f, true, true, true);
        for (float sx : new float[]{-1, 1}) {
            float a = sx * HALF_W, b = sx * (HALF_W + 0.06f);
            win.addBox(new Vector3f(Math.min(a - sx * 0.02f, b), 1.0f, -0.45f), new Vector3f(Math.max(a - sx * 0.02f, b), 1.9f, 0.45f), 1f, true, true, true);
        }
        parts.add(new PartData("Jendela", win, "window", 1.00f, 64f, true));

        MeshBuilder bark = new MeshBuilder(), leaves = new MeshBuilder();
        float[][] trees = {{-4.6f, 2.2f, 1.1f}, {4.8f, -2.6f, 0.9f}};
        for (float[] t : trees) {
            bark.addCylinder(new Vector3f(t[0], 0, t[1]), 0.17f, 1.9f * t[2], 20, 1f);
            leaves.addSphere(new Vector3f(t[0], 1.9f * t[2] + 0.55f * t[2], t[1]), 1.05f * t[2], 32, 20, 3f, 2f);
        }
        parts.add(new PartData("Batang pohon", bark, "bark", 0.05f, 4f, true));
        parts.add(new PartData("Tajuk pohon (sphere)", leaves, "leaves", 0.30f, 16f, true));

        MeshBuilder bush = new MeshBuilder();
        bush.addSphere(new Vector3f(-1.5f, 0.38f, HALF_D + 0.55f), 0.48f, 28, 16, 2f, 1.5f);
        bush.addSphere(new Vector3f(1.5f, 0.38f, HALF_D + 0.55f), 0.48f, 28, 16, 2f, 1.5f);
        parts.add(new PartData("Semak", bush, "leaves", 0.20f, 12f, true));

        return parts;
    }

    public static MeshBuilder lightMarker() {
        MeshBuilder m = new MeshBuilder();
        m.addSphere(new Vector3f(0, 0, 0), 0.35f, 16, 10, 1f, 1f);
        return m;
    }
}
