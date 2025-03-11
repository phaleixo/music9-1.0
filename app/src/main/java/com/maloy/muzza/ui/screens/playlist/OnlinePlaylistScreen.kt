package com.maloy.muzza.ui.screens.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.maloy.innertube.models.SongItem
import com.maloy.innertube.models.WatchEndpoint
import com.maloy.muzza.LocalDatabase
import com.maloy.muzza.LocalPlayerAwareWindowInsets
import com.maloy.muzza.LocalPlayerConnection
import com.maloy.muzza.LocalSyncUtils
import com.maloy.muzza.R
import com.maloy.muzza.constants.AlbumThumbnailSize
import com.maloy.muzza.constants.HideExplicitKey
import com.maloy.muzza.constants.ThumbnailCornerRadius
import com.maloy.muzza.db.entities.PlaylistEntity
import com.maloy.muzza.db.entities.PlaylistSongMap
import com.maloy.muzza.db.entities.SongEntity
import com.maloy.muzza.extensions.toMediaItem
import com.maloy.muzza.extensions.togglePlayPause
import com.maloy.muzza.models.toMediaMetadata
import com.maloy.muzza.playback.ExoDownloadService
import com.maloy.muzza.playback.queues.YouTubeQueue
import com.maloy.muzza.ui.component.AutoResizeText
import com.maloy.muzza.ui.component.DefaultDialog
import com.maloy.muzza.ui.component.FontSizeRange
import com.maloy.muzza.ui.component.IconButton
import com.maloy.muzza.ui.component.LocalMenuState
import com.maloy.muzza.ui.component.YouTubeListItem
import com.maloy.muzza.ui.component.shimmer.ButtonPlaceholder
import com.maloy.muzza.ui.component.shimmer.ListItemPlaceHolder
import com.maloy.muzza.ui.component.shimmer.ShimmerHost
import com.maloy.muzza.ui.component.shimmer.TextPlaceholder
import com.maloy.muzza.ui.menu.YouTubePlaylistMenu
import com.maloy.muzza.ui.menu.YouTubeSongMenu
import com.maloy.muzza.ui.menu.YouTubeSongSelectionMenu
import com.maloy.muzza.ui.utils.backToMain
import com.maloy.muzza.utils.rememberPreference
import com.maloy.muzza.viewmodels.OnlinePlaylistViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OnlinePlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: OnlinePlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.playlistSongs.collectAsState()
    val dbPlaylist by viewModel.dbPlaylist.collectAsState()

    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val syncUtils = LocalSyncUtils.current

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    val filteredSongs = remember(songs, query) {
        if (query.text.isEmpty()) songs.mapIndexed { index, song -> index to song }
        else songs
            .mapIndexed { index, song -> index to song }
            .filter { (_, song) ->
                song.title.contains(query.text, ignoreCase = true) ||
                        song.artists.fastAny { it.name.contains(query.text, ignoreCase = true) }
            }
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }
    if (isSearching) {
        BackHandler {
            isSearching = false
            query = TextFieldValue()
        }
    }

    val showTopBarTitle by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0
        }
    }

    var downloadState by remember {
        mutableStateOf(Download.STATE_STOPPED)
    }

    var showRemoveDownloadDialog by remember {
        mutableStateOf(false)
    }

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.remove_download_playlist_confirm, playlist?.title!!),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            },
            buttons = {
                TextButton(
                    onClick = { showRemoveDownloadDialog = false }
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        database.transaction {
                            dbPlaylist?.id?.let { clearPlaylist(it) }
                        }

                        songs.forEach { song ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.id,
                                false
                            )
                        }
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        )
    }

    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection = rememberSaveable(
        saver = listSaver<MutableList<Int>, Int>(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) { mutableStateListOf() }
    val onExitSelectionMode = {
        inSelectMode = false
        selection.clear()
    }
    if (inSelectMode) {
        BackHandler(onBack = onExitSelectionMode)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues()
        ) {
            playlist.let { playlist ->
                if (playlist != null) {
                    if (!isSearching) {
                        item {
                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .animateItem()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = playlist.thumbnail,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(AlbumThumbnailSize)
                                            .clip(RoundedCornerShape(ThumbnailCornerRadius))
                                    )

                                    Spacer(Modifier.width(16.dp))

                                    Column(
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        AutoResizeText(
                                            text = playlist.title,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            fontSizeRange = FontSizeRange(16.sp, 22.sp)
                                        )

                                        playlist.author?.let { artist ->
                                            Text(buildAnnotatedString {
                                                withStyle(
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Normal,
                                                        color = MaterialTheme.colorScheme.onBackground
                                                    ).toSpanStyle()
                                                ) {
                                                    if (artist.id != null) {
                                                        val link = LinkAnnotation.Clickable(artist.id!!) {
                                                            navController.navigate("artist/${artist.id!!}")
                                                        }
                                                        withLink(link) {
                                                            append(artist.name)
                                                        }
                                                    } else {
                                                        append(artist.name)
                                                    }
                                                }
                                            })
                                        }

                                        playlist.songCountText?.let { songCountText ->
                                            Text(
                                                text = songCountText,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Normal
                                            )
                                        }

                                        Row {
                                            if (playlist.id != "LM") {
                                                IconButton(
                                                    onClick = {
                                                        if (dbPlaylist?.playlist == null) {
                                                            database.transaction {
                                                                val playlistEntity = PlaylistEntity(
                                                                    name = playlist.title,
                                                                    browseId = playlist.id,
                                                                    isEditable = playlist.isEditable,
                                                                ).toggleLike()
                                                                insert(playlistEntity)
                                                                songs.map(SongItem::toMediaMetadata)
                                                                    .onEach(::insert)
                                                                    .mapIndexed { index, song ->
                                                                        PlaylistSongMap(
                                                                            songId = song.id,
                                                                            playlistId = playlistEntity.id,
                                                                            position = index
                                                                        )
                                                                    }
                                                                    .forEach(::insert)
                                                            }
                                                            } else {
                                                                database.transaction {
                                                                    update(dbPlaylist!!.playlist.toggleLike())
                                                                }
                                                        }
                                                    }
                                                        ) {
                                                        Icon(
                                                            painter = painterResource(
                                                                if (dbPlaylist?.playlist?.bookmarkedAt != null) R.drawable.favorite else R.drawable.favorite_border
                                                            ),
                                                            contentDescription = null,
                                                            tint = if (dbPlaylist?.playlist?.bookmarkedAt != null) MaterialTheme.colorScheme.error else LocalContentColor.current
                                                        )
                                                }
                                            }

                                            if (dbPlaylist != null) {
                                                when (downloadState) {
                                                    Download.STATE_COMPLETED -> {
                                                        IconButton(
                                                            onClick = {
                                                                showRemoveDownloadDialog = true
                                                            }
                                                        ) {
                                                            Icon(
                                                                Icons.Rounded.OfflinePin,
                                                                contentDescription = null
                                                            )
                                                        }
                                                    }

                                                    Download.STATE_DOWNLOADING -> {
                                                        IconButton(
                                                            onClick = {
                                                                songs.forEach { song ->
                                                                    DownloadService.sendRemoveDownload(
                                                                        context,
                                                                        ExoDownloadService::class.java,
                                                                        song.id,
                                                                        false
                                                                    )
                                                                }
                                                            }
                                                        ) {
                                                            CircularProgressIndicator(
                                                                strokeWidth = 2.dp,
                                                                modifier = Modifier.size(24.dp)
                                                            )
                                                        }
                                                    }

                                                    else -> {
                                                        IconButton(
                                                            onClick = {
                                                                viewModel.viewModelScope.launch(
                                                                    Dispatchers.IO
                                                                ) {
                                                                    syncUtils.syncPlaylist(
                                                                        playlist.id,
                                                                        dbPlaylist!!.id
                                                                    )
                                                                }

                                                                songs.forEach { song ->
                                                                    val downloadRequest =
                                                                        DownloadRequest.Builder(
                                                                            song.id,
                                                                            song.id.toUri()
                                                                        )
                                                                            .setCustomCacheKey(song.id)
                                                                            .setData(song.title.toByteArray())
                                                                            .build()
                                                                    DownloadService.sendAddDownload(
                                                                        context,
                                                                        ExoDownloadService::class.java,
                                                                        downloadRequest,
                                                                        false
                                                                    )
                                                                }
                                                            }
                                                        ) {
                                                            Icon(
                                                                Icons.Rounded.Download,
                                                                contentDescription = null
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            IconButton(
                                                onClick = {
                                                    playerConnection.addToQueue(
                                                        items = songs.map { it.toMediaItem() }
                                                    )
                                                }
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.queue_music),
                                                    contentDescription = null
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    menuState.show {
                                                        YouTubePlaylistMenu(
                                                            playlist = playlist,
                                                            songs = songs,
                                                            coroutineScope = coroutineScope,
                                                            onDismiss = menuState::dismiss
                                                        )
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.more_vert),
                                                    contentDescription = null
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(
                                        onClick = {
                                            playerConnection.playQueue(YouTubeQueue(playlist.shuffleEndpoint))
                                        },
                                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.shuffle),
                                            contentDescription = null,
                                            modifier = Modifier.size(ButtonDefaults.IconSize)
                                        )
                                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                        Text(stringResource(R.string.shuffle))
                                    }

                                    playlist.radioEndpoint?.let { radioEndpoint ->
                                        OutlinedButton(
                                            onClick = {
                                                playerConnection.playQueue(YouTubeQueue(radioEndpoint))
                                            },
                                            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.radio),
                                                contentDescription = null,
                                                modifier = Modifier.size(ButtonDefaults.IconSize)
                                            )
                                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                            Text(stringResource(R.string.radio))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    items(
                        items = filteredSongs,
                        key = { (index, _) -> index }
                    ) { (index, song) ->
                        val onCheckedChange: (Boolean) -> Unit = {
                            if (it) {
                                selection.add(index)
                            } else {
                                selection.remove(index)
                            }
                        }

                        YouTubeListItem(
                            item = song,
                            isActive = mediaMetadata?.id == song.id,
                            isPlaying = isPlaying,
                            trailingContent = {
                                if (inSelectMode) {
                                    Checkbox(
                                        checked = index in selection,
                                        onCheckedChange = onCheckedChange
                                    )
                                } else {
                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                YouTubeSongMenu(
                                                    song = song,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.more_vert),
                                            contentDescription = null
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .combinedClickable(
                                    enabled = !hideExplicit || !song.explicit,
                                    onClick = {
                                        if (inSelectMode) {
                                            onCheckedChange(index !in selection)
                                        } else if (song.id == mediaMetadata?.id) {
                                            playerConnection.player.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                YouTubeQueue(
                                                    song.endpoint ?: WatchEndpoint(videoId = song.id),
                                                    song.toMediaMetadata()
                                                )
                                            )
                                        }
                                    },
                                    onLongClick = {
                                        if (!inSelectMode) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            inSelectMode = true
                                            onCheckedChange(true)
                                        }
                                    }
                                )
                                .alpha(if (hideExplicit && song.explicit) 0.3f else 1f)
                                .animateItem()
                        )
                    }
                } else {
                    item {
                        ShimmerHost {
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Spacer(
                                        modifier = Modifier
                                            .size(AlbumThumbnailSize)
                                            .clip(RoundedCornerShape(ThumbnailCornerRadius))
                                            .background(MaterialTheme.colorScheme.onSurface)
                                    )

                                    Spacer(Modifier.width(16.dp))

                                    Column(
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        TextPlaceholder()
                                        TextPlaceholder()
                                        TextPlaceholder()
                                    }
                                }

                                Spacer(Modifier.padding(8.dp))

                                Row {
                                    ButtonPlaceholder(Modifier.weight(1f))

                                    Spacer(Modifier.width(12.dp))

                                    ButtonPlaceholder(Modifier.weight(1f))
                                }
                            }

                            repeat(6) {
                                ListItemPlaceHolder()
                            }
                        }
                    }
                }
            }
        }

        TopAppBar(
            title = {
                if (inSelectMode) {
                    Text(pluralStringResource(R.plurals.n_selected, selection.size, selection.size))
                } else if (isSearching) {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search),
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleLarge,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                } else if (showTopBarTitle) {
                    Text(playlist?.title.orEmpty())
                }
            },
            navigationIcon = {
                if (inSelectMode) {
                    IconButton(onClick = onExitSelectionMode) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (isSearching) {
                                isSearching = false
                                query = TextFieldValue()
                            } else {
                                navController.navigateUp()
                            }
                        },
                        onLongClick = {
                            if (!isSearching) {
                                navController.backToMain()
                            }
                        }
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null
                        )
                    }
                }
            },
            actions = {
                if (inSelectMode) {
                    Checkbox(
                        checked = selection.size == songs.size,
                        onCheckedChange = {
                            if (selection.size == songs.size) {
                                selection.clear()
                            } else {
                                selection.clear()
                                selection.addAll(songs.mapIndexedNotNull { index, song ->
                                    if (hideExplicit && song.explicit) null
                                    else index
                                })
                            }
                        }
                    )
                    IconButton(
                        enabled = selection.isNotEmpty(),
                        onClick = {
                            menuState.show {
                                YouTubeSongSelectionMenu(
                                    selection = selection.mapNotNull { songs.getOrNull(it) },
                                    onDismiss = menuState::dismiss,
                                    onExitSelectionMode = onExitSelectionMode
                                )
                            }
                        }
                    ) {
                        Icon(
                            painterResource(R.drawable.more_vert),
                            contentDescription = null
                        )
                    }
                } else if (!isSearching) {
                    IconButton(
                        onClick = { isSearching = true }
                    ) {
                        Icon(
                            painterResource(R.drawable.search),
                            contentDescription = null
                        )
                    }
                }
            },
            scrollBehavior = scrollBehavior
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime))
                .align(Alignment.BottomCenter)
        )
    }
}
