# Download Chunk

Aplicación Android nativa moderna (2026) construida con Kotlin y Jetpack Compose Material 3 para la descarga rápida, persistente y reconstrucción streaming de archivos fragmentados en almacenamiento público (`Download/Chunk`).

---

## 🚀 Características Principales

- **Diseño 2026 Minimalista:** Interfaz limpia con navegación segmentada tipo píldora, sin tecnicismos innecesarios ni elementos visuales distractores.
- **Validación Automática:** Decodificación Base64 URL-safe, descompresión zlib y verificación estricta de estructura y formato antes de descargar.
- **Descargas Persistentes con WorkManager:** Tareas en segundo plano con notificación persistente, progreso en tiempo real, velocidad (MB/s) y tiempo estimado.
- **Reanudación Determinista:** Conserva partes completadas automáticamente para no descargar bloques repetidos si la conexión se interrumpe.
- **Almacenamiento Público Directo:** Reconstruye y almacena los archivos completos en `Download/Chunk` mediante MediaStore con soporte nativo Android 10 a Android 15+.
- **Firma Persistente y Consistente:** Keystore único (`my-upload-key.jks`) configurado para que cada compilación en GitHub Actions genere APKs con la misma firma digital, permitiendo actualizar la aplicación instalada sin necesidad de desinstalar la versión anterior.

---

## 📦 Compilación y Flujo Automatizado en GitHub Actions

El repositorio incluye un flujo de trabajo CI/CD completamente automatizado en `.github/workflows/android-build-apk.yml`.

### Activación del Flujo
- **Automática:** Se ejecuta en cada `push` a las ramas `main` o `master`.
- **Manual:** Se puede ejecutar en cualquier momento desde la pestaña **Actions** en GitHub seleccionando **Build Signed Release APK** y pulsando **Run workflow**.

---

## 📲 Cómo Descargar e Instalar el APK en tu Teléfono Móvil

Sigue estos sencillos pasos para instalar o actualizar **Download Chunk** directamente desde tu teléfono móvil:

1. **Abrir GitHub en el navegador del teléfono:**
   - Ingresa al repositorio de tu proyecto en GitHub.
2. **Ir a la pestaña "Actions":**
   - Toca la pestaña **Actions** en la barra superior del repositorio.
3. **Seleccionar la última ejecución:**
   - Toca la ejecución más reciente del flujo de trabajo (con un círculo verde de éxito ✅ llamado **Build Signed Release APK**).
4. **Descargar el Artefacto:**
   - Desplázate hasta la sección inferior llamada **Artifacts** (Artefactos).
   - Toca el archivo **`app-release-apk`**. El navegador descargará un archivo comprimido `app-release-apk.zip`.
5. **Descomprimir e Instalar:**
   - Abre la aplicación de **Archivos** o **Descargas** de tu teléfono.
   - Toca `app-release-apk.zip` y pulsa **Extraer / Descomprimir**.
   - Encontrarás el archivo **`app-release.apk`**.
   - Toca el archivo `.apk` y selecciona **Instalar** (si el sistema solicita permiso para instalar aplicaciones de fuentes desconocidas para tu explorador, concédelo).
6. **Actualizaciones Futuras:**
   - Gracias a la configuración de firma fija (`my-upload-key.jks`), cuando descargues e instales una nueva versión generada por GitHub Actions, tu teléfono la actualizará directamente manteniendo tus datos e historial, **sin requerir desinstalar la versión previa**.

---

## 💻 Compilación Local

Para compilar el APK Release firmado localmente:
```bash
./gradlew :app:assembleRelease --no-daemon -Dkotlin.compiler.execution.strategy=daemon -Dksp.incremental=false -Dksp.incremental.intermodule=false
```
El APK firmado se generará en:
```text
app/build/outputs/apk/release/app-release.apk
```

Para ejecutar las pruebas:
```bash
gradle :app:testDebugUnitTest
```
