package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.PlaylistItem
import com.example.ui.DiziUiState
import com.example.ui.DiziViewModel
import com.example.ui.components.VideoPlayer
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.CinemaAmber
import com.example.ui.theme.CinemaSurface
import com.example.ui.theme.CinemaTextSecondary

class MainActivity : ComponentActivity() {
    private val viewModel: DiziViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val selectedVideo by viewModel.selectedVideo.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(if (selectedVideo == null) innerPadding else PaddingValues(0.dp))
                    ) {
                        AnimatedContent(
                            targetState = selectedVideo,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                            label = "ScreenTransition"
                        ) { activeVideo ->
                            if (activeVideo != null) {
                                // Full Screen ExoPlayer View (locks to landscape, hides system bars, sets keep screen on)
                                VideoPlayer(
                                    videoUrl = activeVideo.url,
                                    videoTitle = activeVideo.name,
                                    onBack = { viewModel.selectedVideo.value = null },
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                // Main Browser List interface
                                DiziBrowserScreen(
                                    viewModel = viewModel,
                                    onPlayDizi = { viewModel.selectedVideo.value = it }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiziBrowserScreen(
    viewModel: DiziViewModel,
    onPlayDizi: (PlaylistItem) -> Unit
) {
    val uiState by viewModel.filteredState.collectAsState()
    val categories by viewModel.availableGroups.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedGroup.collectAsState()
    
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Header & Branding Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = "App Logo",
                            tint = CinemaAmber,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(id = R.string.app_name),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    
                    IconButton(
                        onClick = { viewModel.fetchDiziler() },
                        modifier = Modifier
                            .background(CinemaSurface, shape = CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Yeniləyin",
                            tint = CinemaAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Modern Search Field with amber hints
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = {
                        Text(
                            "Dizi, film və ya veriliş axtarın...",
                            color = CinemaTextSecondary.copy(alpha = 0.8f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Axtarış",
                            tint = CinemaAmber
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Təmizləyin",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CinemaAmber,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = CinemaAmber,
                        unfocusedLabelColor = CinemaTextSecondary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_field")
                )
            }
        }

        // Horizontal Categories List
        if (categories.size > 1) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isSelected = category == selectedCategory
                    DiziCategoryChip(
                        selected = isSelected,
                        label = category,
                        onClick = { viewModel.selectedGroup.value = category }
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Main listings container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (val state = uiState) {
                is DiziUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = CinemaAmber)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Kanallar yüklənir...",
                            color = CinemaTextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }

                is DiziUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Network error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Xəta Baş Verdi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            color = CinemaTextSecondary,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.fetchDiziler() },
                            colors = ButtonDefaults.buttonColors(containerColor = CinemaAmber)
                        ) {
                            Text("Yenidən Sına", color = Color.Black)
                        }
                    }
                }

                is DiziUiState.Success -> {
                    val list = state.items
                    if (list.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.MovieFilter,
                                contentDescription = "Siyahı boşdur",
                                tint = CinemaTextSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Uyğun dizi tapılmadı",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CinemaTextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Axtarış sözünü dəyişməyi və ya fərqli kateqoriya seçməyi sınayın.",
                                color = CinemaTextSecondary.copy(alpha = 0.7f),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    } else {
                        // "alta alta sadə şəkildə yixsin dizi" -> clean top-to-bottom lists
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Text(
                                    text = "Dizilər (${list.size})",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CinemaTextSecondary,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                            items(list) { dizi ->
                                val isFav = viewModel.isFavorite(dizi)
                                DiziItemRow(
                                    item = dizi,
                                    isFavorite = isFav,
                                    onPlay = { onPlayDizi(dizi) },
                                    onToggleFav = { viewModel.toggleFavorite(dizi) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiziItemRow(
    item: PlaylistItem,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onToggleFav: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = CinemaSurface
        ),
        shape = RoundedCornerShape(20.dp), // Premium 20dp corners matching rounded-2xl
        modifier = modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .testTag("dizi_card_${item.name.replace(" ", "_")}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Elegant 16:9 scale widescreen preview thumbnail (96dp x 56dp corresponds precisely to w-24 h-14 design mockup)
            if (!item.logo.isNullOrEmpty()) {
                AsyncImage(
                    model = item.logo,
                    contentDescription = item.name,
                    modifier = Modifier
                        .width(96.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1C1B1F)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "M3U",
                        color = CinemaAmber.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Info (Name & Group Category)
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                Text(
                    text = item.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Kateqoriya",
                        tint = CinemaTextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.group ?: "Dizi",
                        fontSize = 12.sp,
                        color = CinemaTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Favorites quick-bookmark toggle
            IconButton(
                onClick = onToggleFav,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isFavorite) CinemaAmber.copy(alpha = 0.15f) else Color.Transparent,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Seçilmiş",
                    tint = if (isFavorite) CinemaAmber else CinemaTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Action Circular play indicator
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(CinemaAmber, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Oynat",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun DiziCategoryChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) CinemaAmber else CinemaSurface)
            .border(
                width = 1.dp,
                color = if (selected) CinemaAmber else Color(0xFF2E2E36),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.Black else CinemaTextSecondary
        )
    }
}
