name: Bug Report
description: Create a report to help reproduce and fix a bug in MotionShot
title: '[BUG] '
labels: ['bug']
assignees: []

body:
  - type: textarea
    id: description
    attributes:
      label: Bug Description
      description: Clear and concise description of what the bug is.
    validations:
      required: true
  - type: textarea
    id: steps
    attributes:
      label: Steps to Reproduce
      description: Steps to reproduce the behavior.
      placeholder: |
        1. Open app
        2. Set Timer to 2s
        3. Tap capture button
        4. See error
    validations:
      required: true
  - type: input
    id: environment
    attributes:
      label: Device Environment
      description: Android Version, Device Model (e.g. Pixel 7, Android 14)
    validations:
      required: true
