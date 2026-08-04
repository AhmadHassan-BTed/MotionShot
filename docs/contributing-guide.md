# Developer Onboarding & Contributing Guide

Maintainer: **Ahmad Hassan (B-Ted)**

---

## 🛠️ Local Environment Setup

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/AhmadHassan-BTed/MotionShot.git
   cd MotionShot
   ```

2. **Copy Environment Template**:
   ```bash
   cp .env.example .env
   ```

3. **Open in Android Studio**:
   - Open Android Studio (Ladybug / Jellyfish or newer).
   - Sync Gradle project.

4. **Run Unit Tests**:
   ```bash
   ./gradlew test
   ```

5. **Build Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```

---

## 📐 Architecture Conventions

- **State Management**: Jetpack Compose `StateFlow<MotionShotUiState>`.
- **Concurrency**: Coroutines with `Dispatchers.Default` for pixel math processing.
- **Memory**: Always call `Bitmap.recycle()` on intermediate overlays.
