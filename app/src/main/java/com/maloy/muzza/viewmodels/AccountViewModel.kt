package com.maloy.muzza.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maloy.innertube.YouTube
import com.maloy.innertube.models.AlbumItem
import com.maloy.innertube.models.ArtistItem
import com.maloy.innertube.models.PlaylistItem
import com.maloy.muzza.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor() : ViewModel() {
    val playlists = MutableStateFlow<List<PlaylistItem>?>(null)
    val albums = MutableStateFlow<List<AlbumItem>?>(null)
    val artists = MutableStateFlow<List<ArtistItem>?>(null)

    init {
        viewModelScope.launch {
            YouTube.likedPlaylists().onSuccess {
                playlists.value = it
            }.onFailure {
                reportException(it)
            }
            YouTube.libraryAlbums().onSuccess {
                albums.value = it
            }.onFailure {
                reportException(it)
            }
            YouTube.libraryArtistsSubscriptions().onSuccess {
                artists.value = it
            }.onFailure {
                reportException(it)
            }
        }
    }
}
