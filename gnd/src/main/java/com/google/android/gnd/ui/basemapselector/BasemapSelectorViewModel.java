package com.google.android.gnd.ui.basemapselector;

import android.util.Log;
import android.util.Pair;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gnd.model.basemap.tile.Tile;
import com.google.android.gnd.repository.DataRepository;
import com.google.android.gnd.ui.map.gms.ExtentSelectionState;
import com.google.android.gnd.workers.FileDownloadWorkManager;
import com.google.common.collect.ImmutableSet;

import java.util.HashSet;

import javax.inject.Inject;

import static com.google.android.gnd.util.ImmutableSetCollector.toImmutableSet;
import static java8.util.stream.StreamSupport.stream;

/**
 * This view model is responsible for managing state for the {@link BasemapSelectorFragment}.
 * Together, they constitute a basemap selector that users can interact with to select portions of a
 * basemap for offline viewing. Among other things, this view model is responsible for receiving
 * requests to download basemap files and for scheduling those requests with an {@link
 * com.google.android.gnd.workers.FileDownloadWorker}.
 */
public class BasemapSelectorViewModel extends ViewModel {
  private static final String TAG = BasemapSelectorViewModel.class.getName();
  private final LiveData<ImmutableSet<Tile>> tiles;
  private final LiveData<ImmutableSet<Tile>> downloadedAndPendingTiles;
  private HashSet<String> pendingTileIds = new HashSet<>();
  private final DataRepository dataRepository;
  private final FileDownloadWorkManager downloadWorkManager;

  @Inject
  BasemapSelectorViewModel(DataRepository dataRepository, FileDownloadWorkManager downloadWorkManager) {
    this.dataRepository = dataRepository;
    this.downloadWorkManager = downloadWorkManager;

    this.tiles = LiveDataReactiveStreams.fromPublisher(dataRepository.getTilesOnceAndStream());
    this.downloadedAndPendingTiles =
        LiveDataReactiveStreams.fromPublisher(
            this.dataRepository
                .getTilesOnceAndStream()
                .map(
                    tiles ->
                        stream(tiles)
                            .filter(
                                tile ->
                                    tile.getState() == Tile.State.DOWNLOADED
                                        || tile.getState() == Tile.State.PENDING)
                            .collect(toImmutableSet())));
  }

  public LiveData<ImmutableSet<Tile>> getTiles() {
    return tiles;
  }

  public void updatePendingTiles(Pair<String, ExtentSelectionState> geoJsonClick) {
    Log.d(TAG, "Click state:" + geoJsonClick.second);

    switch (geoJsonClick.second) {
      case SELECTED:
        Log.d(TAG, "Adding selected json feature: " + geoJsonClick.first);
        pendingTileIds.add(geoJsonClick.first);
        downloadTiles();
        break;
      case UNSELECTED:
        Log.d(TAG, "Removing unselected json feature: " + geoJsonClick.first);
        pendingTileIds.remove(geoJsonClick.first);
        break;
      default:
    }
  }

  public LiveData<ImmutableSet<Tile>> getDownloadedAndPendingTiles() {
    return downloadedAndPendingTiles;
  }

  /**
   * Download selected tiles.
   */
  private void downloadTiles() {
    for (String tileId : pendingTileIds) {
      Log.d(TAG, "Downloading: " + tileId);
      downloadWorkManager.enqueueFileDownloadWorker(tileId).subscribe(() -> Log.d(TAG, "worker complete"));
    }
  }
  // TODO: Implement view model.
}
