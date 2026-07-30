## v1.0 (RavenHub Backend Security & Encrypted Engine Release)

### 🔒 Cryptography & Keystore Security Architecture
- **Hardware Keystore Master Key**: Isolated 256-bit Master Key generated inside hardware-backed Android KeyStore (`RavenHubAndroidKeystoreMasterKey`). Master Key is never derived directly from the user's PIN.
- **PBKDF2-HMAC-SHA256 Key Derivation**: PIN Key derived using PBKDF2-HMAC-SHA256 with 100,000 iterations and a device-unique salt encrypted in KeyStore.
- **PIN Lock Protocol**: 4–8 digit numeric PIN keyboard with cryptographic PBKDF2 verification against wrapped Master Key payloads.
- **Zero-Grace Period Auto-Lock**: Immediate re-locking of Master Key and zeroing of memory key buffers on every `onStop` lifecycle event, ensuring mandatory lock screen re-authentication on every app resume.
- **Root Detection & Self-Destruct Wipe**: Integrated `RootSecurityManager` checks for active root access and triggers immediate zeroization & local data purge if compromised.

### 🦀 Rust UniFFI Native Security Engine (`libraven_security.so`)
- **Native Encryption Engine**: All cryptographic operations across all modules (Planner, Finance, Vault, Notes) routed through Rust crate `raven_security` compiled via `cargo ndk` for `arm64-v8a` and `x86_64`.
- **UniFFI Kotlin Bindings**: Cross-language FFI bindings bridging Rust binary protocols directly to Android Kotlin (`RustSecurityBridge`).
- **Chunked File Streaming Encryption**: 64KB chunk-based AES-256-GCM file streaming encryption and decryption for large media and documents in Vault (`encrypt_file_chunked` / `decrypt_file_chunked`).
- **Unified Binary Protocol**: Standardized payload envelope across all persistence layers, ensuring consistent AES-256-GCM authentication tags and IV initialization vectors.

### 📦 Encrypted Selective Zip Backup & Restore
- **Selective Module Exporter**: Configurable `.zip` archive builder allowing users to check/uncheck specific modules (Planner, Finance, Vault, Notes) before export.
- **Transparent Encrypted Backup**: Archived module files remain 100% AES-256-GCM encrypted inside the `.zip` container.
- **Automatic Restore Decryption Engine**: Restoring backup archives extracts files safely into internal app storage and decrypts contents transparently upon master key unlock.
