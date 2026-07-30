use aes_gcm::{
    aead::{Aead, KeyInit},
    Aes256Gcm, Nonce, Key
};
use rand::{RngCore, rngs::OsRng};
use std::fs::File;
use std::io::{Read, Write, BufReader, BufWriter, ErrorKind};

uniffi::include_scaffolding!("raven_security");

#[derive(Debug, thiserror::Error)]
pub enum SecurityError {
    #[error("Encryption failed")]
    EncryptionFailed,
    #[error("Decryption failed")]
    DecryptionFailed,
    #[error("Invalid key length")]
    InvalidKeyLength,
    #[error("IO error: {0}")]
    IoError(String),
}

const CHUNK_SIZE: usize = 64 * 1024; // 64KB chunk streaming

pub fn encrypt_bytes(key: Vec<u8>, plaintext: Vec<u8>) -> Result<Vec<u8>, SecurityError> {
    if key.len() != 32 {
        return Err(SecurityError::InvalidKeyLength);
    }
    let cipher = Aes256Gcm::new(Key::<Aes256Gcm>::from_slice(&key));
    let mut nonce_bytes = [0u8; 12];
    OsRng.fill_bytes(&mut nonce_bytes);
    let nonce = Nonce::from_slice(&nonce_bytes);

    let ciphertext = cipher
        .encrypt(nonce, plaintext.as_ref())
        .map_err(|_| SecurityError::EncryptionFailed)?;

    let mut result = Vec::with_capacity(12 + ciphertext.len());
    result.extend_from_slice(&nonce_bytes);
    result.extend_from_slice(&ciphertext);
    Ok(result)
}

pub fn decrypt_bytes(key: Vec<u8>, ciphertext: Vec<u8>) -> Result<Vec<u8>, SecurityError> {
    if key.len() != 32 {
        return Err(SecurityError::InvalidKeyLength);
    }
    if ciphertext.len() < 12 {
        return Err(SecurityError::DecryptionFailed);
    }

    let (nonce_bytes, payload) = ciphertext.split_at(12);
    let cipher = Aes256Gcm::new(Key::<Aes256Gcm>::from_slice(&key));
    let nonce = Nonce::from_slice(nonce_bytes);

    cipher
        .decrypt(nonce, payload)
        .map_err(|_| SecurityError::DecryptionFailed)
}

pub fn encrypt_file_chunked(input_path: String, output_path: String, key: Vec<u8>) -> Result<(), SecurityError> {
    if key.len() != 32 {
        return Err(SecurityError::InvalidKeyLength);
    }
    let input_file = File::open(&input_path).map_err(|e| SecurityError::IoError(e.to_string()))?;
    let output_file = File::create(&output_path).map_err(|e| SecurityError::IoError(e.to_string()))?;
    let mut reader = BufReader::new(input_file);
    let mut writer = BufWriter::new(output_file);

    let cipher = Aes256Gcm::new(Key::<Aes256Gcm>::from_slice(&key));
    let mut buffer = vec![0u8; CHUNK_SIZE];

    loop {
        let bytes_read = reader.read(&mut buffer).map_err(|e| SecurityError::IoError(e.to_string()))?;
        if bytes_read == 0 {
            break;
        }

        let mut nonce_bytes = [0u8; 12];
        OsRng.fill_bytes(&mut nonce_bytes);
        let nonce = Nonce::from_slice(&nonce_bytes);

        let encrypted_chunk = cipher
            .encrypt(nonce, &buffer[..bytes_read])
            .map_err(|_| SecurityError::EncryptionFailed)?;

        let chunk_len = (encrypted_chunk.len() as u32).to_le_bytes();
        writer.write_all(&chunk_len).map_err(|e| SecurityError::IoError(e.to_string()))?;
        writer.write_all(&nonce_bytes).map_err(|e| SecurityError::IoError(e.to_string()))?;
        writer.write_all(&encrypted_chunk).map_err(|e| SecurityError::IoError(e.to_string()))?;
    }

    writer.flush().map_err(|e| SecurityError::IoError(e.to_string()))?;
    Ok(())
}

pub fn decrypt_file_chunked(input_path: String, output_path: String, key: Vec<u8>) -> Result<(), SecurityError> {
    if key.len() != 32 {
        return Err(SecurityError::InvalidKeyLength);
    }
    let input_file = File::open(&input_path).map_err(|e| SecurityError::IoError(e.to_string()))?;
    let output_file = File::create(&output_path).map_err(|e| SecurityError::IoError(e.to_string()))?;
    let mut reader = BufReader::new(input_file);
    let mut writer = BufWriter::new(output_file);

    let cipher = Aes256Gcm::new(Key::<Aes256Gcm>::from_slice(&key));

    loop {
        let mut len_bytes = [0u8; 4];
        match reader.read_exact(&mut len_bytes) {
            Ok(_) => {},
            Err(e) if e.kind() == ErrorKind::UnexpectedEof => break,
            Err(e) => return Err(SecurityError::IoError(e.to_string())),
        }
        let chunk_len = u32::from_le_bytes(len_bytes) as usize;

        let mut nonce_bytes = [0u8; 12];
        reader.read_exact(&mut nonce_bytes).map_err(|e| SecurityError::IoError(e.to_string()))?;

        let mut encrypted_chunk = vec![0u8; chunk_len];
        reader.read_exact(&mut encrypted_chunk).map_err(|e| SecurityError::IoError(e.to_string()))?;

        let nonce = Nonce::from_slice(&nonce_bytes);
        let decrypted_chunk = cipher
            .decrypt(nonce, encrypted_chunk.as_ref())
            .map_err(|_| SecurityError::DecryptionFailed)?;

        writer.write_all(&decrypted_chunk).map_err(|e| SecurityError::IoError(e.to_string()))?;
    }

    writer.flush().map_err(|e| SecurityError::IoError(e.to_string()))?;
    Ok(())
}

pub fn derive_pbkdf2_key(password: Vec<u8>, salt: Vec<u8>, iterations: u32) -> Vec<u8> {
    let mut out = [0u8; 32];
    pbkdf2::pbkdf2::<hmac::Hmac<sha2::Sha256>>(&password, &salt, iterations, &mut out)
        .expect("PBKDF2 calculation failed");
    out.to_vec()
}
