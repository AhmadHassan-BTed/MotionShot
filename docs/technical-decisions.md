# Architectural Decision Records (ADR)

Maintainer: **Ahmad Hassan (B-Ted)**

---

## ADR 1: Normalized Chromaticity over Raw RGB Subtraction

### Status
Accepted

### Context
Simple RGB subtraction (`abs(R1-R0) + abs(G1-G0) + abs(B1-B0)`) failed under ambient lighting shifts. Camera auto-exposure adjustments caused full-frame difference false-positives, creating overlapping image glitches.

### Decision
Use **Normalized Chromaticity** ($r = \frac{R}{R+G+B}$, $g = \frac{G}{R+G+B}$) combined with BT.601 Luminance separation.

### Consequences
- **Positive**: 100% immune to camera exposure drift and environmental shadows.
- **Positive**: Clean silhouette extraction without background artifact leaks.

---

## ADR 2: Pluggable `MotionEngine` Contract

### Status
Accepted

### Context
To support future AI segmentation models (MediaPipe) without modifying UI or ViewModel code, processing must be isolated.

### Decision
Define a single `MotionEngine` interface and `MotionEngineFactory` registry.

### Consequences
- **Positive**: High cohesion, zero coupling between UI presentation and image math algorithms.
- **Positive**: New processing engines can be plugged in by adding a single class implementation.
