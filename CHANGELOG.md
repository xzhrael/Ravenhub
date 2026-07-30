# RavenHub Changelog

All notable changes to RavenHub will be documented in this file.

---

## [1.0.0] - 2026-07-31

### Added
- Advance Todo Sub-Task Tree: Interactive sub-task step tree for Todos with collapsible UI rows.
- Backup to Cloud Storage: Native chooser intent supporting Google Drive, Dropbox, Nextcloud, OneDrive, and device apps.
- Change Security PIN: Security card option in Settings to update master PIN seamlessly.
- Device Wallpaper Preview: Live system wallpaper rendered in Settings device mockup frame.
- Offline-First Update Checker: UI update check that triggers only when an active internet connection is detected.

### Fixed & Improved
- Original File Export: Resolved SAF export verification so unencrypted exported files are written correctly without 0-byte corruptions.
- Gradient Alpha Syncing: Synchronized banner gradient opacity slider between Custom Theme screen and Home Page card.
- Navigation Bar Overlap: Increased bottom spacing across all primary screens so the last card item is fully visible above floating BotNav pills.
- Bottom Navigation Icon Sizing: Standardized active and inactive tab icon sizes on the floating bottom navigation bar.
- Dark Mode Dialog Contrast: Ensured dialog text in backup module selection remains legible with high contrast in dark mode.

### Build & Security
- Multi-ABI Releases: Split release binaries built for `arm64-v8a`, `armeabi-v7a`, `x86_64`, and `universal`.
- Sanitized Release Notes: Removed raw native bridge symbols and internal debug details from documentation to maintain reverse-engineering protection.
