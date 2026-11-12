package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_MULTISAMPLE;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Kontrol:
 *   B  Phong  <-> Blinn-Phong          (Indikator 1)
 *   F  Flat   <-> Smooth shading       (Indikator 2)
 *   Z  Z-Buffer Depth Test on/off      (Indikator 3)
 *   C  Back-Face Culling on/off        (Indikator 3)
 *   M  Texture filter Nearest <-> Bilinear+Mipmap (Indikator 4)
 *   T  Texture on/off (warna polos)    (Indikator 4)
 *   H  Shadow Mapping on/off           (Indikator 5)
 *   - / =  kurangi / tambah Shadow Bias (Indikator 5)
 *   SPACE  jeda/lanjut orbit cahaya,  LEFT/RIGHT geser cahaya manual
 *   P  wireframe (lihat struktur mesh)   R  reset kamera   ESC keluar
 *   Mouse: drag kiri = orbit kamera, scroll = zoom
 */
public class VisualRealismSuite {

    private static final int WIDTH = 1280, HEIGHT = 800;

    private long window;
    private int fbWidth = WIDTH, fbHeight = HEIGHT;

    // ---- state fitur
    private boolean blinnPhong = true;
    private boolean smooth = true;
    private boolean depthTest = true;
    private boolean cullFace = true;
    private boolean useTexture = true;
    private boolean mipmapFilter = true;
    private boolean shadows = true;
    private boolean wireframe = false;
    private boolean lightOrbit = true;
    private float shadowBias = 0.0015f;
    private float lightAngle = 40f;   // derajat
    private double lastMouseX, lastMouseY;
    private boolean dragging;

    private final Camera camera = new Camera();
    private ShaderProgram sceneShader, depthShader;
    private ShadowMapFBO shadowFBO;
    private final List<Part> parts = new ArrayList<>();
    private final Map<String, Texture2D> textures = new HashMap<>();
    private Mesh lightMarker;
    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Gagal inisialisasi GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);   
        glfwWindowHint(GLFW_SAMPLES, 4);                         

        window = glfwCreateWindow(WIDTH, HEIGHT, "3D Visual Realism Suite", NULL, NULL);
        if (window == NULL) throw new RuntimeException("Gagal membuat window GLFW");

        GLFWVidMode vm = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (vm != null) glfwSetWindowPos(window, (vm.width() - WIDTH) / 2, (vm.height() - HEIGHT) / 2);
        glfwMakeContextCurrent(window);
        glfwSwapInterval(0);         
        glfwShowWindow(window);
        GL.createCapabilities();

        int[] w = new int[1], h = new int[1];
        glfwGetFramebufferSize(window, w, h);
        fbWidth = w[0]; fbHeight = h[0];

        glfwSetFramebufferSizeCallback(window, (win, nw, nh) -> { fbWidth = Math.max(1, nw); fbHeight = Math.max(1, nh); });
        glfwSetKeyCallback(window, this::onKey);
        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                dragging = action == GLFW_PRESS;
                double[] x = new double[1], y = new double[1];
                glfwGetCursorPos(win, x, y);
                lastMouseX = x[0]; lastMouseY = y[0];
            }
        });
        glfwSetCursorPosCallback(window, (win, x, y) -> {
            if (dragging) { camera.orbit((float) (lastMouseX - x) * 0.4f, (float) (y - lastMouseY) * 0.4f); }
            lastMouseX = x; lastMouseY = y;
        });
        glfwSetScrollCallback(window, (win, dx, dy) -> camera.zoom((float) dy * 0.8f));

        glEnable(GL_MULTISAMPLE);
        HSRManager.setupDepthTest(depthTest);
        HSRManager.setupBackFaceCulling(cullFace);

        sceneShader = new ShaderProgram("/shaders/phong.vert", "/shaders/phong.frag");
        depthShader = new ShaderProgram("/shaders/depth.vert", "/shaders/depth.frag");
        shadowFBO = new ShadowMapFBO();

        for (PartData d : HouseModel.build()) {
            Texture2D tex = textures.computeIfAbsent(d.texture, n -> new Texture2D("/textures/" + n + ".png"));
            parts.add(new Part(d, tex));
        }
        lightMarker = new Mesh(HouseModel.lightMarker());

        int tris = 0;
        for (Part p : parts) tris += p.mesh.triangleCount();
        System.out.println("OpenGL  : " + glGetString(GL_VERSION));
        System.out.println("Renderer: " + glGetString(GL_RENDERER));
        System.out.println("Mesh    : " + parts.size() + " bagian, " + tris + " segitiga");
        printControls();
    }

    private void loop() {
        double lastTime = glfwGetTime(), fpsTimer = lastTime;
        int frames = 0;
        while (!glfwWindowShouldClose(window)) {
            double now = glfwGetTime();
            float dt = (float) (now - lastTime);
            lastTime = now;
            if (lightOrbit) lightAngle += 12f * dt;

            render();

            glfwSwapBuffers(window);
            glfwPollEvents();

            frames++;
            if (now - fpsTimer >= 0.5) {
                glfwSetWindowTitle(window, statusTitle(frames / (now - fpsTimer)));
                frames = 0; fpsTimer = now;
            }
        }
    }

    
    private Vector3f lightPosition() {
        double a = Math.toRadians(lightAngle);
        return new Vector3f(10f * (float) Math.cos(a), 11f, 10f * (float) Math.sin(a));
    }

    private void render() {
        Vector3f lightPos = lightPosition();
 
        Matrix4f lightProj = new Matrix4f().ortho(-12f, 12f, -12f, 12f, 1f, 40f);
        Matrix4f lightView = new Matrix4f().lookAt(lightPos, new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));
        Matrix4f lightSpace = new Matrix4f(lightProj).mul(lightView);
        Matrix4f model = new Matrix4f();   

        if (shadows) {
            shadowFBO.bindForWriting();
            glEnable(GL_DEPTH_TEST);
            glDisable(GL_CULL_FACE);                  
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
            glEnable(GL_POLYGON_OFFSET_FILL);
            glPolygonOffset(2.0f, 4.0f);              
            depthShader.use();
            depthShader.setMat4("lightSpaceMatrix", lightSpace);
            depthShader.setMat4("model", model);
            for (Part p : parts) if (p.castsShadow) p.mesh.draw(smooth);
            glDisable(GL_POLYGON_OFFSET_FILL);
            shadowFBO.unbind(fbWidth, fbHeight);
        }

        HSRManager.setupDepthTest(depthTest);
        HSRManager.setupBackFaceCulling(cullFace);
        glPolygonMode(GL_FRONT_AND_BACK, wireframe ? GL_LINE : GL_FILL);
        glClearColor(0.55f, 0.74f, 0.92f, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        float aspect = (float) fbWidth / fbHeight;
        sceneShader.use();
        sceneShader.setMat4("projection", camera.projection(aspect));
        sceneShader.setMat4("view", camera.view());
        sceneShader.setMat4("model", model);
        sceneShader.setMat4("lightSpaceMatrix", lightSpace);
        sceneShader.setVec3("lightPos", lightPos);
        sceneShader.setVec3("viewPos", camera.position());
        sceneShader.setVec3("lightColor", 1.0f, 0.97f, 0.9f);
        sceneShader.setVec3("objectColor", 0.78f, 0.74f, 0.68f);
        sceneShader.setBool("useBlinnPhong", blinnPhong);
        sceneShader.setBool("useShadow", shadows);
        sceneShader.setBool("unlit", false);
        sceneShader.setFloat("shadowBias", shadowBias);
        sceneShader.setInt("textureSampler", 0);
        sceneShader.setInt("shadowMap", 1);

        glActiveTexture(GL_TEXTURE0 + 1);
        glBindTexture(GL_TEXTURE_2D, shadowFBO.getDepthTextureID());

        for (Part p : parts) {
            sceneShader.setBool("useTexture", useTexture);
            sceneShader.setFloat("specStrength", p.specStrength);
            sceneShader.setFloat("shininess", p.shininess);
            p.texture.bind(0);
            p.mesh.draw(smooth);
        }


        sceneShader.setBool("unlit", true);
        sceneShader.setBool("useTexture", false);
        sceneShader.setVec3("objectColor", 1f, 0.92f, 0.45f);
        sceneShader.setMat4("model", new Matrix4f().translate(lightPos));
        lightMarker.draw(true);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
    }

    private void onKey(long win, int key, int scancode, int action, int mods) {
        if (action == GLFW_RELEASE) return;
        boolean press = action == GLFW_PRESS;
        switch (key) {
            case GLFW_KEY_ESCAPE -> glfwSetWindowShouldClose(win, true);
            case GLFW_KEY_LEFT -> lightAngle -= 4f;
            case GLFW_KEY_RIGHT -> lightAngle += 4f;
            case GLFW_KEY_MINUS -> { shadowBias = Math.max(0f, shadowBias - 0.0002f); log("Shadow bias = " + shadowBias); }
            case GLFW_KEY_EQUAL -> { shadowBias += 0.0002f; log("Shadow bias = " + shadowBias); }
            default -> { if (press) onToggle(key); }
        }
    }

    private void onToggle(int key) {
        switch (key) {
            case GLFW_KEY_B -> { blinnPhong = !blinnPhong; log("Shading model: " + (blinnPhong ? "Blinn-Phong" : "Phong")); }
            case GLFW_KEY_F -> { smooth = !smooth; log("Normal: " + (smooth ? "Smooth (vertex normal averaging)" : "Flat (per-face)")); }
            case GLFW_KEY_Z -> { depthTest = !depthTest; log("Z-Buffer depth test: " + onOff(depthTest)); }
            case GLFW_KEY_C -> { cullFace = !cullFace; log("Back-face culling: " + onOff(cullFace)); }
            case GLFW_KEY_T -> { useTexture = !useTexture; log("Texture: " + onOff(useTexture)); }
            case GLFW_KEY_M -> {
                mipmapFilter = !mipmapFilter;
                Texture2D.Filter f = mipmapFilter ? Texture2D.Filter.BILINEAR_MIPMAP : Texture2D.Filter.NEAREST;
                for (Texture2D t : textures.values()) t.setFilter(f);
                log("Texture filter: " + (mipmapFilter ? "Bilinear + Mipmap" : "Nearest"));
            }
            case GLFW_KEY_H -> { shadows = !shadows; log("Shadow mapping: " + onOff(shadows)); }
            case GLFW_KEY_P -> { wireframe = !wireframe; log("Wireframe: " + onOff(wireframe)); }
            case GLFW_KEY_SPACE -> { lightOrbit = !lightOrbit; log("Orbit cahaya: " + onOff(lightOrbit)); }
            case GLFW_KEY_R -> camera.reset();
            default -> { }
        }
    }

    private String statusTitle(double fps) {
        return String.format("3D Visual Realism Suite | %.0f FPS | %s | %s | Z-Buf:%s | Cull:%s | Tex:%s(%s) | Shadow:%s bias=%.4f",
                fps, blinnPhong ? "Blinn-Phong" : "Phong", smooth ? "Smooth" : "Flat",
                onOff(depthTest), onOff(cullFace), onOff(useTexture), mipmapFilter ? "Mipmap" : "Nearest",
                onOff(shadows), shadowBias);
    }

    private static String onOff(boolean b) { return b ? "ON" : "OFF"; }
    private static void log(String s) { System.out.println("[toggle] " + s); }

    private static void printControls() {
        System.out.println("""
                ===== KONTROL =====
                 B  Phong <-> Blinn-Phong        F  Flat <-> Smooth
                 Z  Z-Buffer on/off              C  Back-face culling on/off
                 M  Nearest <-> Bilinear+Mipmap  T  Texture on/off
                 H  Shadow mapping on/off        - / =  Shadow bias turun / naik
                 SPACE  jeda orbit cahaya        LEFT/RIGHT  geser cahaya
                 P  wireframe    R  reset kamera    Mouse drag = orbit, scroll = zoom
                """);
    }

    private void cleanup() {
        for (Part p : parts) p.mesh.delete();
        for (Texture2D t : textures.values()) t.delete();
        lightMarker.delete();
        sceneShader.delete();
        depthShader.delete();
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        GLFWErrorCallback cb = glfwSetErrorCallback(null);
        if (cb != null) cb.free();
    }

    public static void main(String[] args) {
        new VisualRealismSuite().run();
    }
}
