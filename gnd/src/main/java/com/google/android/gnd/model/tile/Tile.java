package com.google.android.gnd.model.tile;

import com.google.android.gnd.model.feature.Feature;
import com.google.auto.value.AutoValue;

import java.net.URL;

@AutoValue
public abstract class Tile {
    public enum State {
    PENDING,
    DOWNLOADED,
    FAILED
  }

  public abstract String getId();
  public abstract String getPath();
  public abstract URL getUrl();
  public abstract State getState();

  public static Builder newBuilder() {return new AutoValue_Tile.Builder();}

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setId(String id);
    public abstract Builder setPath(String path);
    public abstract Builder setUrl(URL url);
    public abstract Builder setState(State state);

    public abstract Tile build();
  }
}
