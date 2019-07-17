package com.google.android.gnd.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

public class FileDownloadWorker extends Worker {
  public static final String CONTENTS = "contents";
  public static final String TARGET_URL = "url";
  public static final String FILENAME = "filename";

  public FileDownloadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
    super(context, params);
  }

  private static final String TAG = FileDownloadWorker.class.getSimpleName();

  @NonNull
  @Override
  public Result doWork() {
    String url = getInputData().getString(TARGET_URL);
    String filename = getInputData().getString(FILENAME);

    try {
      InputStream is = new URL(url).openStream();
      FileOutputStream fos = new FileOutputStream(filename);
      byte[] byteChunk = new byte[4096];
      int n;
      while ((n = is.read(byteChunk)) > 0) {
        fos.write(byteChunk, 0, n);
      }
      is.close();
      fos.close();
      return Result.success();
    } catch (IOException e) {
      Log.d(TAG, e.getMessage());
    }
    return Result.failure();
  }
}
