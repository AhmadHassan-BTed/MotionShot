# Changelog

All notable changes to the MotionShot project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2026-08-05

### Added
- Continuous logarithmic Shutter Speed slider (`Auto`, `1/30s` to `1/8000s`).
- Continuous ISO slider (`Auto`, `ISO 50` to `ISO 102400` boost).
- `PreferencesRepository` for saving and restoring user camera settings automatically.
- Fast Motion Slide preview & 15 FPS Auto-Play Flipbook in Result Gallery.
- Hard time-bounded capture engine guaranteeing zero timer overrun.
- Pluggable `MotionEngine` interface and architecture.
- 2D translation alignment engine (`ImageStabilizer`) for handheld jitter cancellation.

### Changed
- Refactored `FrameDifferencer` to use Normalized Chromaticity ($r, g$) + BT.601 Luminance separation.
- Updated preset selections to 1s, 2s, 5s timer and 5, 15, 25 frames.

---

## [1.0.0] - 2026-08-04

### Added
- Initial public open-source release by **Ahmad Hassan (B-Ted)**.
- CameraX live preview with Jetpack Compose UI.
- Sony Motion Shot stroboscopic compositing engine.
- Step 1 Raw Frame inspection gallery.
