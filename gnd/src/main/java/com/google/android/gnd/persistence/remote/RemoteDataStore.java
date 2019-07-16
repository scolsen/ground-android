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

package com.google.android.gnd.persistence.remote;

import com.google.android.gnd.model.Mutation;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.observation.Record;
import com.google.android.gnd.system.AuthenticationManager.User;
import com.google.common.collect.ImmutableCollection;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import java.util.List;

/** Defines API for accessing data in a remote data store. */
public interface RemoteDataStore {
  Single<List<Project>> loadProjectSummaries(User user);

  Single<Project> loadProject(String projectId);

  /**
   * Returns all features in the specified project, then continues to emit any remote updates to the
   * set of features in the project until all subscribers have been disposed. Implementations must
   * ensure any network or other potentially long running operations are performed on a background
   * thread.
   */
  Flowable<RemoteDataEvent<Feature>> loadFeaturesOnceAndStreamChanges(Project project);

  Flowable<RemoteDataEvent<Record>> loadRecordSummariesOnceAndStreamChanges(Feature feature);

  /**
   * Applies the provided mutations to the remote data store in a single batched transaction. If one
   * update fails, none of the mutations will be applied.
   */
  Completable applyMutations(ImmutableCollection<Mutation> mutations);
}
