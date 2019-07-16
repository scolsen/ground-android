package com.google.android.gnd.ui.map;

import com.google.android.gnd.model.tile.Tile;
import com.google.common.collect.ImmutableSet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class TileDownloader {
  private ImmutableSet<Tile> tiles;

  private Tile download(Tile tile) throws IOException {
    File file = new File(tile.getPath());

    FileOutputStream fos = new FileOutputStream(file);
    InputStream is = null;

    try {
      is = tile.getUrl().openStream();
      byte[] byteChunk = new byte[4096];
      int n;
      while ((n = is.read(byteChunk)) > 0) {
        fos.write(byteChunk, 0, n);
      }
      return Tile.newBuilder().setState(Tile.State.DOWNLOADED).build();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      fos.close();
      if (is != null) {
        is.close();
      }
    }
    return Tile.newBuilder().setState(Tile.State.FAILED).build();
  }

  public void download() {
    for (Tile tile : tiles) {
      try {
        download(tile);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
