package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Kamera orbit (azimuth/elevation/jarak) mengelilingi titik target. */
public class Camera {
    public float azimuth = 35f, elevation = 22f, distance = 13f;
    public final Vector3f target = new Vector3f(0, 1.6f, 0);

    public void reset() { azimuth = 35f; elevation = 22f; distance = 13f; }

    public void orbit(float dAz, float dEl) {
        azimuth += dAz;
        elevation = Math.max(-5f, Math.min(85f, elevation + dEl));
    }

    public void zoom(float d) { distance = Math.max(3f, Math.min(35f, distance - d)); }

    public Vector3f position() {
        double az = Math.toRadians(azimuth), el = Math.toRadians(elevation);
        return new Vector3f(
                target.x + distance * (float) (Math.cos(el) * Math.sin(az)),
                target.y + distance * (float) Math.sin(el),
                target.z + distance * (float) (Math.cos(el) * Math.cos(az)));
    }

    public Matrix4f view() { return new Matrix4f().lookAt(position(), target, new Vector3f(0, 1, 0)); }

    public Matrix4f projection(float aspect) {
        return new Matrix4f().perspective((float) Math.toRadians(45), aspect, 0.5f, 100f);
    }
}
