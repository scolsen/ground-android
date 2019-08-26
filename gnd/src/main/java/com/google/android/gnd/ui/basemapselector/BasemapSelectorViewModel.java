package com.google.android.gnd.ui.basemapselector;

import android.util.Log;
import android.util.Pair;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gnd.model.basemap.tile.Tile;
import com.google.android.gnd.repository.DataRepository;
import com.google.android.gnd.ui.map.gms.GeoJsonSelectionState;
import com.google.common.collect.ImmutableSet;

import java.util.HashMap;
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
  private HashMap<String, Tile> pendingTiles = new HashMap<>();
  private final DataRepository dataRepository;

  @Inject
  BasemapSelectorViewModel(DataRepository dataRepository) {
    this.dataRepository = dataRepository;

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

  public void updatePendingTiles(Pair<String, GeoJsonSelectionState> geoJsonClick) {
    switch (geoJsonClick.second) {
      case SELECTED:
        Log.d(TAG, "Adding selected json feature: " + geoJsonClick.first);
        pendingTiles.put(
            geoJsonClick.first,
            Tile.newBuilder()
                .setId(geoJsonClick.first)
                .setPath(geoJsonClick.first + ".geojson")
                .setState(Tile.State.PENDING)
                .build());
      case UNSELECTED:
        Log.d(TAG, "Removing unselected json feature: " + geoJsonClick.first);
        pendingTiles.remove(geoJsonClick.first);
    }
  }

  public LiveData<ImmutableSet<Tile>> getDownloadedAndPendingTiles() {
    return downloadedAndPendingTiles;
  }
  // TODO: Implement view model.
}
