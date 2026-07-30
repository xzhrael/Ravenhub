package com.ravenhub.app.ui.mainscreens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ravenhub.app.R
import com.ravenhub.app.data.finance.FinanceDataManager
import com.ravenhub.app.data.notes.NotesDataManager
import com.ravenhub.app.data.planner.PlannerDataManager
import com.ravenhub.app.data.vault.VaultDataManager
import com.ravenhub.app.security.MasterKeyManager

import com.ravenhub.app.ui.component.ExpressiveList
import com.ravenhub.app.ui.component.ExpressiveListItem
import com.ravenhub.app.ui.component.LocalAppHazeState
import com.ravenhub.app.ui.component.LocalBlurEnabled
import com.ravenhub.app.ui.component.MediaBannerRenderer
import com.ravenhub.app.ui.util.getHeaderImage
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val hazeState = LocalAppHazeState.current
    val isBlurEnabled = LocalBlurEnabled.current

    val plannerData by PlannerDataManager.data.collectAsState()
    val notesData by NotesDataManager.data.collectAsState()
    val vaultData by VaultDataManager.data.collectAsState()
    val financeData by FinanceDataManager.data.collectAsState()

    val todoCount = remember(plannerData) { plannerData.todos.count { !it.isCompleted } }
    val habitCount = remember(plannerData) { plannerData.habits.size }
    val notesCount = remember(notesData) { notesData.notes.size }
    val credCount = remember(vaultData) { vaultData.credentials.size }
    val fileCount = remember(vaultData) { vaultData.files.size }
    val totalExpenses = remember(financeData) {
        financeData.expenses.filter { it.type == com.ravenhub.app.data.finance.TransactionType.EXPENSE }.sumOf { it.amount }
    }

    LaunchedEffect(Unit) {
        if (MasterKeyManager.isUnlocked()) {
            PlannerDataManager.load(context)
            NotesDataManager.load(context)
            VaultDataManager.load(context)
            FinanceDataManager.load(context)
        }
    }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isBlurEnabled && hazeState != null) Modifier.hazeSource(state = hazeState) else Modifier)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(
                bottom = 110.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            )
        ) {
            item {
                Spacer(modifier = Modifier.statusBarsPadding().height(64.dp))
            }

            // --- Hero Banner ---
            item {
                HeroBannerCard()
            }

            // --- Quick Summary Section ---
            item {
                Text(
                    text = "Quick Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryMetricCard(
                        title = "Planner",
                        value = "$todoCount Pending",
                        subtitle = "$habitCount Habits",
                        icon = Icons.Rounded.CheckCircle,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        onIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("planner") }
                    )
                    SummaryMetricCard(
                        title = "Notes",
                        value = "$notesCount Notes",
                        subtitle = "Knowledge Base",
                        icon = Icons.Rounded.Description,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        onIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("notes") }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryMetricCard(
                        title = "Vault",
                        value = "$credCount Passwords",
                        subtitle = "$fileCount Secure Files",
                        icon = Icons.Rounded.Lock,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        onIconColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("vault") }
                    )
                    SummaryMetricCard(
                        title = "Finance",
                        value = currencyFormat.format(totalExpenses),
                        subtitle = "Total Spent",
                        icon = Icons.Rounded.AccountBalanceWallet,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        onIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("finance") }
                    )
                }
            }

            // --- Maintainer Card (Copied directly from RavenCore) ---
            item {
                Text(
                    text = "Maintainer",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp)
                )
            }
            item {
                ExpressiveList(
                    content = listOf {
                        ExpressiveListItem(
                            headlineContent = { Text(text = "Luca Azhrael", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface) },
                            supportingContent = { Text("Creator & Maintainer", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            leadingContent = {
                                Image(
                                    painter = painterResource(R.drawable.app_icon),
                                    contentDescription = "Luca Azhrael",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                )
                            },
                            trailingContent = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { uriHandler.openUri("https://github.com/xzhrael/Ravencore") }) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_github),
                                            contentDescription = "GitHub",
                                            modifier = Modifier.size(29.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = { uriHandler.openUri("https://t.me/lucaslounge") }) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_telegram),
                                            contentDescription = "Telegram",
                                            modifier = Modifier.size(26.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun HeroBannerCard() {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val customBannerUri = remember { context.getHeaderImage() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(20 / 9f),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            MediaBannerRenderer(
                uriString = customBannerUri,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, colorScheme.surfaceContainerLow.copy(alpha = 0.85f))
                        )
                    )
            )
        }
    }
}

@Composable
private fun SummaryMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onIconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = onIconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
