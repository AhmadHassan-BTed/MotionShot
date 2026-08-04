# System Design & Frame Lifecycle

Maintainer: **Ahmad Hassan (B-Ted)**

---

## Frame Acquisition & Scheduling Lifecycle

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant UI as CameraScreen / Controls
    participant VM as MotionShotViewModel
    participant Analyzer as FrameAnalyzer
    participant Camera as CameraX Engine
    participant Engine as MotionEngine

    User->>UI: Tap Capture Button
    UI->>VM: onCaptureToggle()
    VM->>VM: Start Timer (Total Window = T seconds)
    
    loop Every Interval Step (T / (N-1))
        VM->>Analyzer: shouldCapture.set(true)
        Camera->>Analyzer: analyze(ImageProxy)
        Analyzer->>VM: frameChannel.receive()
        VM->>VM: Increment framesCaptured
    end

    VM->>Engine: process(capturedFrames)
    Engine->>Engine: ImageStabilizer (2D Translation Alignment)
    Engine->>Engine: FrameDifferencer (Normalized Chromaticity)
    Engine->>Engine: MorphologyOps (3x3 Opening)
    Engine->>Engine: CanvasCompositor (PorterDuff SRC_OVER)
    Engine-->>VM: Return Composite Bitmap
    VM->>UI: Phase = Done (Show Result & Step 1 Gallery)
```

---

## Handheld 2D Translation Alignment

Camera micro-jitter is cancelled via translation vector estimation:

$$\Delta x, \Delta y = \arg\min_{dx, dy} \sum_{y, x} |\text{Base}(x, y) - \text{Curr}(x + dx, y + dy)|$$

```mermaid
flowchart LR
    A[Base Frame 0] --> C[ImageStabilizer Correlation Engine]
    B[Target Frame N] --> C
    C -->|Translation Offset (dx, dy)| D[Buffer Alignment & Shift]
    D --> E[Normalized Chromaticity Differencer]
    E --> F[3x3 Morphological Opening]
    F --> G[Native Canvas PorterDuff Stamping]
```
