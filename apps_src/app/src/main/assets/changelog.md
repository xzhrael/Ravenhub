# RavenHub Changelog

## Version 1.1.0 (Latest Release)

### Backend Updates
- **Cloud Storage SAF & Sync Engine**: Storage Access Framework (SAF) integration for encrypted backup export and restore with Google Drive, OneDrive, Nextcloud, and Dropbox.
- **GitHub In-App Update Engine**: Re-engineered update checker with HTML redirect fallback to bypass GitHub REST API rate limits.
- **Activity Lifecycle & Session Management**: Fixed Get Started completion transition and master key auto-lock lifecycle to prevent process restarts.
- **Icon Vector Generation Engine**: High-density mipmap vector pipeline for adaptive launcher and themed monochrome icons (Android 13+).

## Version 1.0.0
- Native Rust Security Engine (`raven_security`) with hardware AES-256-GCM encryption for Planner, Notes, Vault, and Finance data.
