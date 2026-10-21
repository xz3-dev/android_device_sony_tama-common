Copyright (C) 2018 - The LineageOS Project

Common device tree for Sony tama devices
==============

## Branch `lineage-22.2-xz3`

Used for the Xperia XZ3 801SO build (xz3-dev/android_device_sony_akatsuki).
On top of `lineage-22.2`:

- `TARGET_USES_SONY_CXD224X_NFC := true` in a device makefile drops the NXP NFC
  HAL, its `libnfc-nci.conf` and the MIFARE Classic feature.
- `DOUBLE_TAP_TO_WAKE` in `powerhint.json` drives
  `/sys/devices/virtual/input/lge_touch/tap_to_wake` (Atmel touch driver with
  tap to wake in xz3-dev/android_kernel_sony_sdm845). Devices enable the Settings
  toggle with `config_supportDoubleTapWake`.
- `XperiaParts` (`parts/`): camera button page in Settings > System > Gestures
  (Lineage camera key settings) and a notification light helper that blinks the
  LED for apps whose channels do not request it.
