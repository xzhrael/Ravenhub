package com.ravenhub.app.data.vault

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class CredentialItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val username: String = "",
    val password: String = "",
    val category: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class VaultFileEntry(
    val id: String = UUID.randomUUID().toString(),
    val originalName: String,
    val encryptedFileName: String, // ponytail: stored as UUID.enc in app internal storage
    val sizeBytes: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class VaultData(
    val credentials: List<CredentialItem> = emptyList(),
    val files: List<VaultFileEntry> = emptyList()
)
