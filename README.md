# 3D Visual Realism Suite — Mesh Rumah 3D (Grafika Komputer, Minggu 11)

Java 17 + LWJGL 3.3.3 + JOML + GLSL 330 core. Perluasan project *ThreeDViewing*.

## Menjalankan di NetBeans (disarankan)
1. Ekstrak ZIP. Di NetBeans: **File > Open Project…** lalu pilih folder `Grafkom_Minggu11_Rumah3D` (ikon Maven akan muncul).
2. Pastikan JDK 17+: klik kanan project > **Properties > Build > Compile > Java Platform**.
3. Tunggu NetBeans mengunduh dependency (LWJGL, JOML) — butuh internet pada run pertama. Native LWJGL dipilih otomatis sesuai OS.
4. Klik kanan project > **Clean and Build**, lalu tekan **Run Project (F6)**.
   Kalau gagal: klik kanan project > **Custom > Run Visual Realism Suite**.

## Menjalankan lewat terminal
```
mvn compile exec:exec
```
Perlu JDK 17+, Maven, dan GPU/driver OpenGL 3.3+. (macOS: flag `-XstartOnFirstThread` sudah diatur di pom.)

## Kontrol
| Tombol | Fungsi | Indikator |
|---|---|---|
| **B** | Phong ⇄ Blinn-Phong | 1 |
| **F** | Flat ⇄ Smooth shading | 2 |
| **Z** | Z-Buffer depth test on/off | 3 |
| **C** | Back-face culling on/off | 3 |
| **M** | Texture filter Nearest ⇄ Bilinear+Mipmap | 4 |
| **T** | Texture on/off | 4 |
| **H** | Shadow mapping on/off | 5 |
| **- / =** | Shadow bias turun / naik | 5 |
| SPACE, ←/→ | jeda orbit / geser cahaya | |
| P | wireframe (lihat struktur mesh) | |
| Mouse | drag = orbit, scroll = zoom; R = reset | |

Judul window menampilkan **FPS** dan status semua toggle (untuk bagian pembahasan laporan).

## Struktur
```
src/main/java/com/grafkom/viewing3d/
  VisualRealismSuite  main loop, input, 2-pass shadow rendering
  MeshBuilder         geometri + normal flat / smooth (vertex normal averaging) + UV   (tanpa OpenGL)
  HouseModel          merakit mesh rumah, pohon, semak, ground                         (tanpa OpenGL)
  Mesh / Part         VAO flat & smooth, material
  HSRManager          Z-Buffer + Back-Face Culling       (Praktikum 2)
  Texture2D           STBImage loader + filtering/mipmap (Praktikum 3)
  ShadowMapFBO        Depth FBO 2048x2048                (Praktikum 4)
  ShaderProgram, Camera
src/main/resources/shaders/  phong.vert/.frag (Praktikum 1), depth.vert/.frag
src/main/resources/textures/ brick, roof, wood, door, window, grass, concrete, leaves, bark (PNG)
model/rumah3d.obj            ekspor mesh (bisa dibuka di Blender)
docs/                        Laporan_Praktikum.docx + screenshot pengujian
```

## Mesh
10 bagian, 4.412 segitiga: pondasi+teras, dinding bata, cerobong, gable kayu, atap genteng (papan tebal),
pintu, 5 jendela, 2 pohon (silinder + UV-sphere), 2 semak, halaman. Semua face ber-winding CCW (menghadap keluar),
bernormal vertex dan berkoordinat UV; sudut tajam (>60°) dijaga tetap tegas pada smooth shading (crease angle).
