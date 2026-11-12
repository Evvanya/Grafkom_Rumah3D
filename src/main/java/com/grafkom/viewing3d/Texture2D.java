package com.grafkom.viewing3d;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.*;

public class Texture2D {

    public enum Filter { NEAREST, BILINEAR_MIPMAP }

    private final int textureID;
    private final String name;

    public Texture2D(String resourcePath) {
        this.name = resourcePath;
        textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        stbi_set_flip_vertically_on_load(true);   
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1), h = stack.mallocInt(1), ch = stack.mallocInt(1);
            ByteBuffer raw = readResource(resourcePath);
            ByteBuffer data = stbi_load_from_memory(raw, w, h, ch, 4);
            if (data != null) {
                glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w.get(0), h.get(0), 0, GL_RGBA, GL_UNSIGNED_BYTE, data);
                glGenerateMipmap(GL_TEXTURE_2D);  
                stbi_image_free(data);
            } else {
                System.err.println("Failed to load texture: " + resourcePath + " -> " + stbi_failure_reason());
            }
        } catch (IOException e) {
            System.err.println("Failed to read texture resource: " + resourcePath);
        }
        setFilter(Filter.BILINEAR_MIPMAP);
    }

    public void setFilter(Filter f) {
        glBindTexture(GL_TEXTURE_2D, textureID);
        if (f == Filter.NEAREST) {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        } else {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        }
    }

    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureID);
    }

    public void delete() { glDeleteTextures(textureID); }

    public String getName() { return name; }

    private static ByteBuffer readResource(String path) throws IOException {
        try (InputStream in = Texture2D.class.getResourceAsStream(path)) {
            if (in == null) throw new IOException("Resource not found: " + path);
            byte[] bytes = in.readAllBytes();
            ByteBuffer buf = BufferUtils.createByteBuffer(bytes.length);
            buf.put(bytes).flip();
            return buf;
        }
    }
}
