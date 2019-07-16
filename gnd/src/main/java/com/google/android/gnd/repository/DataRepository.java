/*
 * Copyright 2018 Google LLC
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

package com.google.android.gnd.repository;

import android.util.Log;
import com.google.android.gnd.model.Mutation;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.FeatureMutation;
import com.google.android.gnd.model.observation.Record;
import com.google.android.gnd.model.observation.RecordMutation;
import com.google.android.gnd.persistence.local.LocalDataStore;
import com.google.android.gnd.persistence.local.LocalValueStore;
import com.google.android.gnd.persistence.remote.RemoteDataEvent;
import com.google.android.gnd.persistence.remote.RemoteDataStore;
import com.google.android.gnd.persistence.remote.firestore.DocumentNotFoundException;
import com.google.android.gnd.persistence.sync.DataSyncWorkManager;
import com.google.android.gnd.persistence.uuid.OfflineUuidGenerator;
import com.google.android.gnd.system.AuthenticationManager.User;
import com.google.android.gnd.system.NetworkManager;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.processors.BehaviorProcessor;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.schedulers.Schedulers;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java8.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DataRepository {
  private static final String TAG = DataRepository.class.getSimpleName();
  private static final long GET_REMOTE_RECORDS_TIMEOUT_SECS = 5;

  private final InMemoryCache cache;
  private final LocalDataStore localDataStore;
  private final RemoteDataStore remoteDataStore;
  private final DataSyncWorkManager dataSyncWorkManager;
  private final FlowableProcessor<Persistable<Project>> activeProject;
  private final OfflineUuidGenerator uuidGenerator;
  private final NetworkManager networkManager;
  private final LocalValueStore localValueStore;

  @Inject
  public DataRepository(
      LocalDataStore localDataStore,
      RemoteDataStore remoteDataStore,
      DataSyncWorkManager dataSyncWorkManager,
      InMemoryCache cache,
      OfflineUuidGenerator uuidGenerator,
      NetworkManager networkManager,
      LocalValueStore localValueStore) {
    this.localDataStore = localDataStore;
    this.remoteDataStore = remoteDataStore;
    this.dataSyncWorkManager = dataSyncWorkManager;
    this.cache = cache;
    this.activeProject = BehaviorProcessor.create();
    this.uuidGenerator = uuidGenerator;
    this.networkManager = networkManager;
    this.localValueStore = localValueStore;

    streamFeaturesToLocalDb(remoteDataStore);
  }

  /**
   * Mirrors features in the current project from the remote db into the local db when the network
   * is available. When invoked, will first attempt to resync all features from the remote db,
   * subsequently syncing only remote changes.
   */
  private void streamFeaturesToLocalDb(RemoteDataStore remoteDataStore) {
    // TODO: Move to Application or background service.
    activeProject
        .compose(Persistable::values)
        .switchMap(p -> remoteDataStore.loadFeaturesOnceAndStreamChanges(p))
        .switchMap(event -> updateLocalFeature(event).toFlowable())
        .subscribe();
  }

  private Completable updateLocalFeature(RemoteDataEvent<Feature> event) {
    switch (event.getEventType()) {
      case ENTITY_LOADED:
      case ENTITY_MODIFIED:
        return event
            .value()
            .map(localDataStore::mergeFeature)
            .orElse(Completable.complete())
            .subscribeOn(Schedulers.io());
      case ENTITY_REMOVED:
        // TODO: Delete features:
        // localDataStore.removeFeature(event.getEntityId());
        return Completable.complete();
      case ERROR:
        return Completable.error(event.error().get());
      default:
        return Completable.error(
            new UnsupportedOperationException("Event type: " + event.getEventType()));
    }
  }

  /**
   * Returns a stream that emits the latest project activation state, and continues to emits changes
   * to that state until all subscriptions are disposed.
   */
  public Flowable<Persistable<Project>> getActiveProjectOnceAndStream() {
    return activeProject;
  }

  public Single<Project> activateProject(String projectId) {
    Log.d(TAG, " Activating project " + projectId);
    return remoteDataStore
        .loadProject(projectId)
        .doOnSubscribe(__ -> activeProject.onNext(Persistable.loading()))
        .doOnSuccess(this::onProjectLoaded);
  }

  private void onProjectLoaded(Project project) {
    cache.setActiveProject(project);
    activeProject.onNext(Persistable.loaded(project));
    localValueStore.setLastActiveProjectId(project.getId());
  }

  public Observable<Persistable<List<Project>>> getProjectSummaries(User user) {
    // TODO: Get from load db if network connection not available or remote times out.
    return remoteDataStore
        .loadProjectSummaries(user)
        .map(Persistable::loaded)
        .onErrorReturn(Persistable::error)
        .toObservable()
        .startWith(Persistable.loading());
  }

  // TODO: Only return feature fields needed to render features on map.
  // TODO(#127): Decouple from Project and accept id instead.
  public Flowable<ImmutableSet<Feature>> getFeaturesOnceAndStream(Project project) {
    return localDataStore.getFeaturesOnceAndStream(project);
  }

  /**
   * Retrieves the record summaries for the specified project, feature, and form, streaming
   * successive changes ad infinitum. If the network is available on subscribe, will attempt to sync
   * remote record changes to the local data store before returning the first set. If the network is
   * not available, relevant records will be returned directly from the local db. After the first
   * set is emitted, will continue to monitor the remote db for changes, writing updates to records
   * to the local db and emitting a new set on each change.
   */
  public Flowable<ImmutableList<Record>> getRecordSummariesOnceAndStream(
      String projectId, String featureId, String formId) {
    // TODO: Only fetch first n fields.
    // TODO: Also load from db.
    // TODO(#127): Decouple feature from record so that we don't need to fetch record here.
    return getFeature(projectId, featureId)
        .switchIfEmpty(Single.error(new DocumentNotFoundException()))
        .flatMapPublisher(
            feature -> {
              Flowable<RemoteDataEvent<Record>> remoteChanges =
                  remoteDataStore
                      .loadRecordSummariesOnceAndStreamChanges(feature)
                      .subscribeOn(Schedulers.io());
              Flowable<ImmutableList<Record>> localChanges =
                  localDataStore
                      .getRecordsOnceAndStream(feature, formId)
                      .subscribeOn(Schedulers.io());
              return maybeSyncFirst(remoteChanges)
                  .toFlowable()
                  .map(o -> (ImmutableList<Record>) o)
                  .concatWith(
                      // This will rewrite the first update in remoteChanges to the local db
                      // even if already successful. We allow this in case the first update
                      // timed out or failed or the network wasn't available.
                      localChanges.mergeWith(
                          remoteChanges.flatMapCompletable(this::mergeRemoteRecordChange)));
            });
  }

  /**
   * If network is available, waits for records from remote db, merging them into the local db
   * before completing. Completes immediately if the network isn't available or if record sync fails
   * or times out.
   */
  private Completable maybeSyncFirst(Flowable<RemoteDataEvent<Record>> remoteChanges) {
    if (networkManager.isNetworkAvailable()) {
      Log.d(TAG, "Has network; syncing records before showing");
      return remoteChanges
          .firstElement()
          .flatMapCompletable(this::mergeRemoteRecordChange)
          .timeout(GET_REMOTE_RECORDS_TIMEOUT_SECS, TimeUnit.SECONDS)
          // TODO: Propagate this to the user so we can show "network unavailable" message also
          // when network became unavailable mid-fetch .
          .doOnError(t -> Log.d(TAG, "Record sync timed out"))
          .onErrorComplete();
    } else {
      Log.d(TAG, "No network; skipping remote sync");
      return Completable.complete();
    }
  }

  private Completable mergeRemoteRecordChange(RemoteDataEvent<Record> event) {
    switch (event.getEventType()) {
      case ENTITY_LOADED:
      case ENTITY_MODIFIED:
        return event
            .value()
            .map(record -> localDataStore.mergeRecord(record).subscribeOn(Schedulers.io()))
            .orElse(Completable.never());
      case ENTITY_REMOVED:
        // TODO: Delete record from local db.
        break;
      case ERROR:
        event.error().ifPresent(t -> Log.e(TAG, "Error in remote record update", t));
        break;
      default:
        Log.e(TAG, "Unknown event type: " + event.getEventType());
    }
    return Completable.never();
  }
  // TODO(#127): Decouple Project from Feature and remove projectId.
  // TODO: Replace with Single and treat missing id as error.
  private Maybe<Feature> getFeature(String projectId, String featureId) {
    return getProject(projectId)
        .flatMapMaybe(project -> localDataStore.getFeature(project, featureId));
  }

  public Single<Record> getRecord(String projectId, String featureId, String recordId) {
    // TODO: Store and retrieve latest edits from cache and/or db.
    // TODO(#127): Decouple feature from record so that we don't need to fetch feature here.
    return getFeature(projectId, featureId)
        .switchIfEmpty(Single.error(new DocumentNotFoundException()))
        .flatMap(
            feature ->
                localDataStore
                    .getRecord(feature, recordId)
                    .switchIfEmpty(Single.error(new DocumentNotFoundException())));
  }

  public Single<Record> createRecord(String projectId, String featureId, String formId) {
    // TODO: Handle invalid formId.
    // TODO(#127): Decouple feature from record so that we don't need to fetch feature here.
    return getFeature(projectId, featureId)
        .switchIfEmpty(Single.error(new DocumentNotFoundException()))
        .map(
            feature ->
                Record.newBuilder()
                    .setId(uuidGenerator.generateUuid())
                    .setProject(feature.getProject())
                    .setFeature(feature)
                    .setForm(feature.getFeatureType().getForm(formId).get())
                    .build());
  }

  private Single<Project> getProject(String projectId) {
    // TODO: Try to load from db if network not available or times out.
    return Maybe.fromCallable(() -> cache.getActiveProject())
        .filter(p -> projectId.equals(p.getId()))
        .switchIfEmpty(remoteDataStore.loadProject(projectId));
  }

  public Completable applyAndEnqueue(RecordMutation mutation) {
    // TODO(#101): Store user id and timestamp on save.
    return localDataStore
        .applyAndEnqueue(mutation)
        .andThen(dataSyncWorkManager.enqueueSyncWorker(mutation.getFeatureId()));
  }

  public Completable saveFeature(Feature feature) {
    // TODO(#79): Assign owner and timestamps when creating new feature.
    // TODO(#80): Update UI to provide FeatureMutations instead of Features here.
    return localDataStore
        .applyAndEnqueue(
            FeatureMutation.builder()
                .setType(Mutation.Type.CREATE)
                .setProjectId(feature.getProject().getId())
                .setFeatureId(feature.getId())
                .setFeatureTypeId(feature.getFeatureType().getId())
                .setNewLocation(Optional.of(feature.getPoint()))
                // TODO(#101): Attach real credentials.
                .setUserId("")
                .build())
        .andThen(dataSyncWorkManager.enqueueSyncWorker(feature.getId()));
  }

  /**
   * Reactivates the last active project, emitting true once loaded, or false if no project was
   * previously activated.
   */
  public Single<Boolean> reactivateLastProject() {
    return Maybe.fromCallable(() -> localValueStore.getLastActiveProjectId())
        .flatMap(id -> activateProject(id).toMaybe())
        .doOnComplete(() -> Log.v(TAG, "No previous project found to reactivate"))
        .doOnSuccess(project -> Log.v(TAG, "Reactivated project " + project.getId()))
        .map(__ -> true)
        .toSingle(false);
  }

  /** Clears the currently active project from cache and from local localValueStore. */
  public void clearActiveProject() {
    cache.clearActiveProject();
    localValueStore.clearLastActiveProjectId();
    activeProject.onNext(Persistable.notLoaded());
  }
}
