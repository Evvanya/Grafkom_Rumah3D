package com.grafkom.viewing3d;

public class PartData {
    public final String name;
    public final MeshBuilder geometry;
    public final String texture;        
    public final float specStrength;    
    public final float shininess;       
    public final boolean castsShadow;

    public PartData(String name, MeshBuilder geometry, String texture,
                    float specStrength, float shininess, boolean castsShadow) {
        this.name = name; this.geometry = geometry; this.texture = texture;
        this.specStrength = specStrength; this.shininess = shininess; this.castsShadow = castsShadow;
    }
}
