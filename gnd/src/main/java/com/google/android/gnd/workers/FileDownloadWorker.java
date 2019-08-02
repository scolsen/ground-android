/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gnd.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gnd.model.basemap.tile.Tile;
import com.google.android.gnd.persistence.local.LocalDataStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * A worker that downloads files to the device in the background. The target URL and file name are
 * provided in a {@link Data} object. This worker should only run when the device has a network
 * connection.
 */
public class FileDownloadWorker extends Worker {
  private static final String TILE_ID = "tile_id";
  private static final int BUFFER_SIZE = 4096;
  private final Context context;
  private final LocalDataStore localDataStore;

  public FileDownloadWorker(
      @NonNull Context context, @NonNull WorkerParameters params, LocalDataStore localDataStore) {
    super(context, params);
    this.context = context;
    this.localDataStore = localDataStore;
  }

  private static final String TAG = FileDownloadWorker.class.getSimpleName();

  /** Creates input data for the FileDownloadWorker. */
  public static Data createInputData(String tilePrimaryKey) {
    return new Data.Builder().putString(TILE_ID, tilePrimaryKey).build();
  }

  private Result downloadTile(Tile tile) {
    localDataStore.insertOrUpdateTile(
        Tile.newBuilder()
            .setId(tile.getId())
            .setState(Tile.State.IN_PROGRESS)
            .setPath(tile.getPath())
            .setUrl(tile.getUrl())
            .build());
    try {
      InputStream is = new URL(tile.getUrl()).openStream();
      FileOutputStream fos = context.openFileOutput(tile.getPath(), Context.MODE_PRIVATE);
      byte[] byteChunk = new byte[BUFFER_SIZE];
      int n;
      while ((n = is.read(byteChunk)) > 0) {
        fos.write(byteChunk, 0, n);
      }
      is.close();
      fos.close();
      localDataStore.insertOrUpdateTile(
          Tile.newBuilder()
              .setId(tile.getId())
              .setState(Tile.State.DOWNLOADED)
              .setPath(tile.getPath())
              .setUrl(tile.getUrl())
              .build());
      return Result.success();
    } catch (IOException e) {
      Log.d(TAG, "Failed to download and write file.", e);
      localDataStore.insertOrUpdateTile(
          Tile.newBuilder()
              .setId(tile.getId())
              .setState(Tile.State.FAILED)
              .setPath(tile.getPath())
              .setUrl(tile.getUrl())
              .build());
      return Result.failure();
    }
  }

  private Result resumeTileDownload(Tile tile) {
    try {
      File existingTileFile = new File(context.getFilesDir(), tile.getPath());
      URL url = new URL(tile.getUrl());
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestProperty("Range", existingTileFile.length() + "-");
      connection.connect();
      InputStream is = connection.getInputStream();
      FileOutputStream fos = new FileOutputStream(existingTileFile);
      byte[] byteChunk = new byte[BUFFER_SIZE];
      int n;
      while ((n = is.read(byteChunk)) > 0) {
        fos.write(byteChunk, 0, n);
      }
      is.close();
      fos.close();
      localDataStore.insertOrUpdateTile(
          Tile.newBuilder()
              .setId(tile.getId())
              .setState(Tile.State.DOWNLOADED)
              .setPath(tile.getPath())
              .setUrl(tile.getUrl())
              .build());
      return Result.success();
    } catch (IOException e) {
      Log.d(TAG, "Failed to download and write file.", e);
      localDataStore.insertOrUpdateTile(
          Tile.newBuilder()
              .setId(tile.getId())
              .setState(Tile.State.FAILED)
              .setPath(tile.getPath())
              .setUrl(tile.getUrl())
              .build());
      return Result.failure();
    }
  }

  /**
   * Downloads a file from a given url and saves it to the application's file directory. The
   * directory is given by the provided context. Returns a successful result containing the filename
   * of the written file upon success.
   */
  @NonNull
  @Override
  public Result doWork() {
    Tile tile = localDataStore.getTile(TILE_ID).blockingGet();

    switch (tile.getState()) {
      case DOWNLOADED:
        return Result.success();
      case PENDING:
        return downloadTile(tile);
      case FAILED:
      case IN_PROGRESS:
        return resumeTileDownload(tile);
      default:
        return Result.failure();
    }
  }
}
