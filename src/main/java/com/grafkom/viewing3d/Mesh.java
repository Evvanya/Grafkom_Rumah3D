package com.grafkom.viewing3d;

import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

public class Mesh {
    private static final float CREASE_DEG = 60f;
    private final int vaoFlat, vaoSmooth, vboFlat, vboSmooth, vertexCount;

    public Mesh(MeshBuilder mb) {
        float[] p = mb.positions(), t = mb.uvs();
        vertexCount = mb.pos.size();
        int[] flat = upload(p, mb.flatNormals(), t);
        int[] smooth = upload(p, mb.smoothNormals(CREASE_DEG), t);
        vaoFlat = flat[0]; vboFlat = flat[1];
        vaoSmooth = smooth[0]; vboSmooth = smooth[1];
    }

    private static int[] upload(float[] p, float[] n, float[] t) {
        int count = p.length / 3;
        FloatBuffer buf = MemoryUtil.memAllocFloat(count * 8);
        for (int i = 0; i < count; i++) {
            buf.put(p[i * 3]).put(p[i * 3 + 1]).put(p[i * 3 + 2]);
            buf.put(n[i * 3]).put(n[i * 3 + 1]).put(n[i * 3 + 2]);
            buf.put(t[i * 2]).put(t[i * 2 + 1]);
        }
        buf.flip();
        int vao = glGenVertexArrays(), vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, buf, GL_STATIC_DRAW);
        int stride = 8 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6L * Float.BYTES);
        glEnableVertexAttribArray(2);
        glBindVertexArray(0);
        MemoryUtil.memFree(buf);
        return new int[]{vao, vbo};
    }

    public void draw(boolean smooth) {
        glBindVertexArray(smooth ? vaoSmooth : vaoFlat);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);
    }

    public int triangleCount() { return vertexCount / 3; }

    public void delete() {
        glDeleteVertexArrays(vaoFlat); glDeleteVertexArrays(vaoSmooth);
        glDeleteBuffers(vboFlat); glDeleteBuffers(vboSmooth);
    }
}
