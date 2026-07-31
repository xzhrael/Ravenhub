# RavenHub Changelog

All notable changes to RavenHub will be documented in this file.

---

## [1.1.0] - 2026-07-31

### Backend & Engine Updates
- **Cloud Storage SAF & Sync Engine**: Storage Access Framework (SAF) integration for encrypted backup export and restore with Google Drive, OneDrive, Nextcloud, and Dropbox.
- **GitHub In-App Update Engine**: Re-engineered `UpdateCheckerUtil.kt` with HTML redirect fallback to bypass GitHub REST API rate limits seamlessly.
- **Activity Lifecycle & Session Management**: Fixed Get Started completion transition and master key auto-lock lifecycle to prevent process restarts.
- **Icon Vector Generation Engine**: High-density mipmap vector pipeline for adaptive launcher and themed monochrome icons (Android 13+).

---

## [1.0.0] - 2026-07-31

### Backend & Core Infrastructure
- Native Rust Security Engine (`raven_security`) with hardware AES-256-GCM encryption.
- Multi-ABI release compilation (`arm64-v8a`, `armeabi-v7a`, `x86_64`, `universal`).
- Offline-first encrypted database management for Planner, Notes, Vault, and Finance modules.
