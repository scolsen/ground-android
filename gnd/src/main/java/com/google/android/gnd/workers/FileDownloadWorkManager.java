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

import androidx.lifecycle.LiveData;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/** Enqueues data sync work to be done in the background. */
public class FileDownloadWorkManager {
  /** Number of seconds to wait before retrying failed sync tasks. */
  private static final long RETRY_BACKOFF_SECS = 5;

  private final WorkManager workManager;

  public FileDownloadWorkManager() {
    this.workManager= WorkManager.getInstance();
  }

  /**
   * Enqueues a worker that sends changes made locally to the remote data store once a network
   * connection is available. The returned {@code Completable} completes immediately as soon as the
   * worker is added to the work queue (not once the sync job completes).
   */
  public LiveData<WorkInfo> enqueueFileDownloadWorker(String url, String filename) {
    return getWorkManager()
        .getWorkInfoByIdLiveData(enqueueFileDownloadWorkerInternal(url, filename).getId());
  }

  private OneTimeWorkRequest enqueueFileDownloadWorkerInternal(String url, String filename) {
    // Rather than having running workers monitor the queue for new mutations for their respective
    // featureId, we instead queue a new worker on each new mutation. This simplifies the worker
    // implementation and avoids race conditions in the rare event the worker finishes just when new
    // mutations are added to the db.
    OneTimeWorkRequest request = buildWorkerRequest(url, filename);

    getWorkManager()
        .enqueueUniqueWork(
            FileDownloadWorker.class.getName(),
            ExistingWorkPolicy.APPEND,
            request);

    return request;
  }

  private WorkManager getWorkManager() {
    return workManager;
  }

  private Constraints getWorkerConstraints() {
    // TODO: Make required NetworkType configurable.
    return new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
  }

  private OneTimeWorkRequest buildWorkerRequest(String url, String filename) {
    return new OneTimeWorkRequest.Builder(FileDownloadWorker.class)
        .setConstraints(getWorkerConstraints())
        .setBackoffCriteria(BackoffPolicy.LINEAR, RETRY_BACKOFF_SECS, TimeUnit.SECONDS)
        .setInputData(FileDownloadWorker.createInputData(url, filename))
        .build();
  }
}
