package com.google.android.gnd.persistence.local.room;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.android.gnd.model.basemap.tile.Tile;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;

@AutoValue
@Entity(tableName = "tile")
public abstract class TileEntity {
  @CopyAnnotations
  @NonNull
  @PrimaryKey
  @ColumnInfo(name = "id")
  public abstract String getId();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "path")
  public abstract String getPath();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "url")
  public abstract String getUrl();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "state")
  public abstract TileEntityState getState();

  public static Tile toTile(TileEntity tileEntity) {
    Tile.Builder tile =
        Tile.newBuilder()
            .setId(tileEntity.getId())
            .setPath(tileEntity.getPath())
            .setState(toTileState(tileEntity.getState()))
            .setUrl(tileEntity.getUrl());
    return tile.build();
  }

  private static Tile.State toTileState(TileEntityState state) {
    switch (state) {
      case PENDING:
        return Tile.State.PENDING;
      case IN_PROGRESS:
        return Tile.State.IN_PROGRESS;
      case DOWNLOADED:
        return Tile.State.DOWNLOADED;
      case FAILED:
        return Tile.State.FAILED;
      case REMOVED:
        return Tile.State.REMOVED;
      default:
        throw new IllegalArgumentException("Unknown tile state: " + state);
    }
  }

  public static TileEntity fromTile(Tile tile) {
    TileEntity.Builder entity =
        TileEntity.builder()
            .setId(tile.getId())
            .setPath(tile.getPath())
            .setState(toEntityState(tile.getState()))
            .setUrl(tile.getUrl());
    return entity.build();
  }

  private static TileEntityState toEntityState(Tile.State state) {
    switch (state) {
      case PENDING:
        return TileEntityState.PENDING;
      case IN_PROGRESS:
        return TileEntityState.IN_PROGRESS;
      case FAILED:
        return TileEntityState.FAILED;
      case DOWNLOADED:
        return TileEntityState.DOWNLOADED;
      case REMOVED:
        return TileEntityState.REMOVED;
      default:
        return TileEntityState.UNKNOWN;
    }
  }

  public static TileEntity create(String id, String path, TileEntityState state, String url) {
    return builder().setId(id).setState(state).setPath(path).setUrl(url).build();
  }

  public static Builder builder() {
    return new AutoValue_TileEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setId(String newId);

    public abstract Builder setPath(String newPath);

    public abstract Builder setState(TileEntityState newState);

    public abstract Builder setUrl(String url);

    public abstract TileEntity build();
  }
}

