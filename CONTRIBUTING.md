# Contributing to MotionShot

Contributions to MotionShot are welcomed and appreciated. Please take a moment to review the guidelines below before submitting pull requests or opening issues.

---

## Code of Conduct

Participation in this project is governed by the [Code of Conduct](CODE_OF_CONDUCT.md). Please adhere to these standards in all interactions.

---

## How to Contribute

### 1. Reporting Bugs
- Search existing issues to ensure the bug has not already been reported.
- Open a new issue using the **Bug Report** template.
- Include device specifications (Android version, device model), reproduction steps, and relevant logcat output.

### 2. Suggesting Features
- Open an issue using the **Feature Request** template.
- Describe the feature clearly, explain the use case, and discuss potential implementation strategies.

### 3. Submitting Pull Requests
1. Fork the repository and create a feature branch off `main`:
   ```bash
   git checkout -b feature/your-feature-name
   ```
2. Follow Kotlin coding conventions and Jetpack Compose best practices.
3. Ensure unit tests pass and code builds cleanly:
   ```bash
   ./gradlew test assembleDebug
   ```
4. Commit changes using clear, descriptive commit messages.
5. Push your branch to your fork and submit a Pull Request to `main`.

---

## Code Style & Standards

- **Formatting**: Adhere to `.editorconfig` rules.
- **Architecture**: Follow the clean module boundaries:
  - `domain`: Core abstractions, interfaces, and contracts.
  - `processing`: Pure algorithmic processing logic.
  - `engine`: Engine implementations.
  - `ui` & `viewmodel`: MVI / Jetpack Compose presentation layer.
- **Memory Safety**: Ensure all intermediate Bitmaps are explicitly recycled using `Bitmap.recycle()` to avoid heap leaks.

---

## Maintainer Information

This project is maintained by **Ahmad Hassan (B-Ted)**.
