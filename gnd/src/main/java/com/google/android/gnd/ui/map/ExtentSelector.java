package com.google.android.gnd.ui.map;

import android.util.Pair;

import com.google.android.gnd.ui.map.gms.ExtentSelectionState;
import com.google.common.collect.ImmutableSet;

import io.reactivex.Observable;

public interface ExtentSelector extends MapProvider.MapAdapter {

  Observable<Pair<String, ExtentSelectionState>> getExtentSelections();

  void updateExtentSelections(
      ImmutableSet<String> extentIdentifiers, ExtentSelectionState selectionState);

  void renderExtentSelectionLayer();
}
