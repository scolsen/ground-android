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

package com.google.android.gnd.model;

import com.google.android.gnd.model.layer.FeatureType;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java8.util.Optional;
import javax.annotation.Nullable;

/** Configuration, schema, and ACLs for a single project. */
@AutoValue
public abstract class Project {
  @Nullable
  public abstract String getId();

  @Nullable
  public abstract String getTitle();

  @Nullable
  public abstract String getDescription();

  protected abstract ImmutableMap<String, FeatureType> getFeatureTypeMap();

  public ImmutableList<FeatureType> getFeatureTypes() {
    return getFeatureTypeMap().values().asList();
  }

  public Optional<FeatureType> getFeatureType(String featureTypeId) {
    return Optional.ofNullable(getFeatureTypeMap().get(featureTypeId));
  }

  public static Builder newBuilder() {
    return new AutoValue_Project.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setId(@Nullable String newId);

    public abstract Builder setTitle(@Nullable String newTitle);

    public abstract Builder setDescription(@Nullable String newDescription);

    public abstract ImmutableMap.Builder<String, FeatureType> featureTypeMapBuilder();

    public void putFeatureType(String id, FeatureType featureType) {
      featureTypeMapBuilder().put(id, featureType);
    }

    public abstract Project build();
  }
}
