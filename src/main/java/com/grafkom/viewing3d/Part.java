package com.grafkom.viewing3d;

public class Part {
    public final String name;
    public final Mesh mesh;
    public final Texture2D texture;
    public final float specStrength, shininess;
    public final boolean castsShadow;

    public Part(PartData d, Texture2D texture) {
        this.name = d.name;
        this.mesh = new Mesh(d.geometry);
        this.texture = texture;
        this.specStrength = d.specStrength;
        this.shininess = d.shininess;
        this.castsShadow = d.castsShadow;
    }
}
