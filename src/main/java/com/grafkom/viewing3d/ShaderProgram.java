package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;

/** Membungkus kompilasi, linking, dan uniform GLSL. Shader dibaca dari /shaders di classpath. */
public class ShaderProgram {
    private final int programId;
    private final Map<String, Integer> uniforms = new HashMap<>();

    public ShaderProgram(String vertexResource, String fragmentResource) {
        int vs = compile(GL_VERTEX_SHADER, load(vertexResource), vertexResource);
        int fs = compile(GL_FRAGMENT_SHADER, load(fragmentResource), fragmentResource);
        programId = glCreateProgram();
        glAttachShader(programId, vs);
        glAttachShader(programId, fs);
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == 0)
            throw new IllegalStateException("Link error: " + glGetProgramInfoLog(programId));
        glDeleteShader(vs);
        glDeleteShader(fs);
    }

    private static int compile(int type, String src, String name) {
        int id = glCreateShader(type);
        glShaderSource(id, src);
        glCompileShader(id);
        if (glGetShaderi(id, GL_COMPILE_STATUS) == 0)
            throw new IllegalStateException("Compile error in " + name + ":\n" + glGetShaderInfoLog(id));
        return id;
    }

    private static String load(String path) {
        try (InputStream in = ShaderProgram.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("Shader not found: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public void use() { glUseProgram(programId); }

    private int loc(String name) {
        return uniforms.computeIfAbsent(name, n -> glGetUniformLocation(programId, n));
    }

    public void setInt(String n, int v) { glUniform1i(loc(n), v); }
    public void setBool(String n, boolean v) { glUniform1i(loc(n), v ? 1 : 0); }
    public void setFloat(String n, float v) { glUniform1f(loc(n), v); }
    public void setVec3(String n, Vector3f v) { glUniform3f(loc(n), v.x, v.y, v.z); }
    public void setVec3(String n, float x, float y, float z) { glUniform3f(loc(n), x, y, z); }

    public void setMat4(String n, Matrix4f m) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(loc(n), false, m.get(stack.mallocFloat(16)));
        }
    }

    public void delete() { glDeleteProgram(programId); }
}
