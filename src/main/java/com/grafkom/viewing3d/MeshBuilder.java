package com.grafkom.viewing3d;

import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MeshBuilder {

    public final List<Vector3f> pos = new ArrayList<>();
    public final List<Vector2f> uv = new ArrayList<>();

    public int triangleCount() { return pos.size() / 3; }

    public void addTriangle(Vector3f a, Vector3f b, Vector3f c,
                            Vector2f ua, Vector2f ub, Vector2f uc, Vector3f interior) {
        Vector3f n = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a));
        if (interior != null) {
            Vector3f centroid = new Vector3f(a).add(b).add(c).div(3f);
            if (n.dot(centroid.sub(interior)) < 0) { 
                Vector3f t = b; b = c; c = t;
                Vector2f tu = ub; ub = uc; uc = tu;
            }
        }
        pos.add(new Vector3f(a)); pos.add(new Vector3f(b)); pos.add(new Vector3f(c));
        uv.add(new Vector2f(ua)); uv.add(new Vector2f(ub)); uv.add(new Vector2f(uc));
    }

    public void addQuad(Vector3f a, Vector3f b, Vector3f c, Vector3f d,
                        Vector2f ua, Vector2f ub, Vector2f uc, Vector2f ud, Vector3f interior) {
        addTriangle(a, b, c, ua, ub, uc, interior);
        addTriangle(a, c, d, ua, uc, ud, interior);
    }

    public void addQuadUnitUV(Vector3f a, Vector3f b, Vector3f c, Vector3f d, float tileU, float tileV,
                              Vector3f interior) {
        addQuad(a, b, c, d, new Vector2f(0, 0), new Vector2f(tileU, 0),
                new Vector2f(tileU, tileV), new Vector2f(0, tileV), interior);
    }

    
    public void addBox(Vector3f min, Vector3f max, float tileSize, boolean stretch,
                       boolean withBottom, boolean withTop) {
        Vector3f center = new Vector3f(min).add(max).mul(0.5f);
        float[] lo = {min.x, min.y, min.z}, hi = {max.x, max.y, max.z};
        int[] uAxis = {2, 0, 0}; 
        int[] vAxis = {1, 2, 1}; 
        for (int axis = 0; axis < 3; axis++) {
            int b = (axis + 1) % 3, c = (axis + 2) % 3;
            for (int s = 0; s < 2; s++) {
                if (axis == 1 && s == 0 && !withBottom) continue;
                if (axis == 1 && s == 1 && !withTop) continue;
                float[][] cr = {{lo[b], lo[c]}, {hi[b], lo[c]}, {hi[b], hi[c]}, {lo[b], hi[c]}};
                Vector3f[] p = new Vector3f[4];
                Vector2f[] t = new Vector2f[4];
                for (int i = 0; i < 4; i++) {
                    float[] v = new float[3];
                    v[axis] = s == 0 ? lo[axis] : hi[axis];
                    v[b] = cr[i][0]; v[c] = cr[i][1];
                    p[i] = new Vector3f(v[0], v[1], v[2]);
                    int ua = uAxis[axis], va = vAxis[axis];
                    if (stretch) {
                        t[i] = new Vector2f((v[ua] - lo[ua]) / (hi[ua] - lo[ua]),
                                            (v[va] - lo[va]) / (hi[va] - lo[va]));
                    } else {
                        t[i] = new Vector2f(v[ua] / tileSize, v[va] / tileSize);
                    }
                }
                addQuad(p[0], p[1], p[2], p[3], t[0], t[1], t[2], t[3], center);
            }
        }
    }

   
    public void addConvexSolid(Vector3f[] v, int[][] faces, float tileSize) {
        Vector3f center = new Vector3f();
        for (Vector3f p : v) center.add(p);
        center.div(v.length);
        for (int[] f : faces) {
            // proyeksikan UV pada bidang face: sumbu u = sisi pertama, v = tegak lurus di bidang
            Vector3f e1 = new Vector3f(v[f[1]]).sub(v[f[0]]).normalize();
            Vector3f nrm = new Vector3f(v[f[1]]).sub(v[f[0]]).cross(new Vector3f(v[f[2]]).sub(v[f[0]])).normalize();
            Vector3f e2 = new Vector3f(nrm).cross(e1).normalize();
            Vector2f[] t = new Vector2f[f.length];
            for (int i = 0; i < f.length; i++) {
                Vector3f d = new Vector3f(v[f[i]]).sub(v[f[0]]);
                t[i] = new Vector2f(d.dot(e1) / tileSize, d.dot(e2) / tileSize);
            }
            for (int i = 1; i + 1 < f.length; i++)
                addTriangle(v[f[0]], v[f[i]], v[f[i + 1]], t[0], t[i], t[i + 1], center);
        }
    }

    public void addCylinder(Vector3f base, float radius, float height, int seg, float tileSize) {
        Vector3f center = new Vector3f(base).add(0, height / 2f, 0);
        for (int i = 0; i < seg; i++) {
            float a0 = (float) (2 * Math.PI * i / seg), a1 = (float) (2 * Math.PI * (i + 1) / seg);
            Vector3f b0 = new Vector3f(base.x + radius * (float) Math.cos(a0), base.y, base.z + radius * (float) Math.sin(a0));
            Vector3f b1 = new Vector3f(base.x + radius * (float) Math.cos(a1), base.y, base.z + radius * (float) Math.sin(a1));
            Vector3f t0 = new Vector3f(b0).add(0, height, 0), t1 = new Vector3f(b1).add(0, height, 0);
            float u0 = (float) i / seg * 2f, u1 = (float) (i + 1) / seg * 2f, vh = height / tileSize;
            addQuad(b0, b1, t1, t0, new Vector2f(u0, 0), new Vector2f(u1, 0), new Vector2f(u1, vh), new Vector2f(u0, vh), center);
            Vector3f top = new Vector3f(base).add(0, height, 0);
            addTriangle(top, t0, t1, new Vector2f(.5f, .5f), new Vector2f(.5f + .5f * (float) Math.cos(a0), .5f + .5f * (float) Math.sin(a0)),
                    new Vector2f(.5f + .5f * (float) Math.cos(a1), .5f + .5f * (float) Math.sin(a1)), center);
            addTriangle(base, b0, b1, new Vector2f(.5f, .5f), new Vector2f(.5f + .5f * (float) Math.cos(a0), .5f + .5f * (float) Math.sin(a0)),
                    new Vector2f(.5f + .5f * (float) Math.cos(a1), .5f + .5f * (float) Math.sin(a1)), center);
        }
    }
    public void addSphere(Vector3f c, float r, int sectors, int stacks, float uRepeat, float vRepeat) {
        for (int i = 0; i < stacks; i++) {
            float p0 = (float) (Math.PI * i / stacks), p1 = (float) (Math.PI * (i + 1) / stacks);
            for (int j = 0; j < sectors; j++) {
                float t0 = (float) (2 * Math.PI * j / sectors), t1 = (float) (2 * Math.PI * (j + 1) / sectors);
                Vector3f a = sph(c, r, p0, t0), b = sph(c, r, p0, t1), d = sph(c, r, p1, t0), e = sph(c, r, p1, t1);
                Vector2f ua = new Vector2f(uRepeat * j / sectors, vRepeat * (1f - (float) i / stacks));
                Vector2f ub = new Vector2f(uRepeat * (j + 1) / sectors, vRepeat * (1f - (float) i / stacks));
                Vector2f ud = new Vector2f(uRepeat * j / sectors, vRepeat * (1f - (float) (i + 1) / stacks));
                Vector2f ue = new Vector2f(uRepeat * (j + 1) / sectors, vRepeat * (1f - (float) (i + 1) / stacks));
                if (i == 0) addTriangle(a, d, e, ua, ud, ue, c);          // kutub atas
                else if (i == stacks - 1) addTriangle(a, d, b, ua, ud, ub, c); // kutub bawah
                else addQuad(a, d, e, b, ua, ud, ue, ub, c);
            }
        }
    }

    private static Vector3f sph(Vector3f c, float r, float phi, float theta) {
        return new Vector3f(c.x + r * (float) (Math.sin(phi) * Math.cos(theta)),
                            c.y + r * (float) Math.cos(phi),
                            c.z + r * (float) (Math.sin(phi) * Math.sin(theta)));
    }
    public float[] flatNormals() {
        float[] out = new float[pos.size() * 3];
        for (int t = 0; t < triangleCount(); t++) {
            Vector3f n = faceNormal(t, false);
            for (int k = 0; k < 3; k++) put(out, t * 3 + k, n);
        }
        return out;
    }
    public float[] smoothNormals(float creaseDeg) {
        float cosCrease = (float) Math.cos(Math.toRadians(creaseDeg));
        Map<Long, List<Integer>> byPos = new HashMap<>();
        int tc = triangleCount();
        for (int t = 0; t < tc; t++)
            for (int k = 0; k < 3; k++)
                byPos.computeIfAbsent(key(pos.get(t * 3 + k)), x -> new ArrayList<>()).add(t);
        Vector3f[] unit = new Vector3f[tc], area = new Vector3f[tc];
        for (int t = 0; t < tc; t++) { area[t] = faceNormal(t, true); unit[t] = new Vector3f(area[t]).normalize(); }

        float[] out = new float[pos.size() * 3];
        for (int t = 0; t < tc; t++) {
            for (int k = 0; k < 3; k++) {
                Vector3f sum = new Vector3f();
                for (int f : byPos.get(key(pos.get(t * 3 + k))))
                    if (unit[f].dot(unit[t]) >= cosCrease - 1e-5f) sum.add(area[f]);
                if (sum.lengthSquared() < 1e-12f) sum.set(unit[t]);
                put(out, t * 3 + k, sum.normalize());
            }
        }
        return out;
    }

    public float[] positions() {
        float[] o = new float[pos.size() * 3];
        for (int i = 0; i < pos.size(); i++) put(o, i, pos.get(i));
        return o;
    }

    public float[] uvs() {
        float[] o = new float[uv.size() * 2];
        for (int i = 0; i < uv.size(); i++) { o[i * 2] = uv.get(i).x; o[i * 2 + 1] = uv.get(i).y; }
        return o;
    }

    private Vector3f faceNormal(int t, boolean areaWeighted) {
        Vector3f a = pos.get(t * 3), b = pos.get(t * 3 + 1), c = pos.get(t * 3 + 2);
        Vector3f n = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a));
        return areaWeighted ? n : n.normalize();
    }
    
    private static long key(Vector3f p) {
        long x = Math.round(p.x * 1e4) & 0x1FFFFF, y = Math.round(p.y * 1e4) & 0x1FFFFF, z = Math.round(p.z * 1e4) & 0x1FFFFF;
        return (x << 42) | (y << 21) | z;
    }

    private static void put(float[] a, int i, Vector3f v) { a[i * 3] = v.x; a[i * 3 + 1] = v.y; a[i * 3 + 2] = v.z; }
}
