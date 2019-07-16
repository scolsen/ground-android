package com.google.android.gnd.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

public class FileDownloadWorker extends Worker {
  public static final String CONTENTS = "contents";
  public static final String TARGET_URL = "url";

  public FileDownloadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
    super(context, params);
  }

  private static final String TAG = FileDownloadWorker.class.getSimpleName();

  @NonNull
  @Override
  public Result doWork() {
    String url = getInputData().getString(TARGET_URL);


    try {
      InputStream is = new URL(url).openStream();
      BufferedReader buf = new BufferedReader(new InputStreamReader(is));
      String line = buf.readLine();
      StringBuilder sb = new StringBuilder();
      while (line != null) {
        sb.append(line).append("\n");
        line = buf.readLine();
      }
      return Result.success(new Data.Builder().putString(CONTENTS, sb.toString()).build());
    } catch (IOException e) {
      Log.d(TAG, e.getMessage());
    }
    return Result.failure();
  }
}
