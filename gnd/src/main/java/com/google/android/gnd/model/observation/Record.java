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

package com.google.android.gnd.model.observation;

import androidx.annotation.Nullable;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.Timestamps;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.form.Form;
import com.google.android.gnd.system.AuthenticationManager.User;
import com.google.auto.value.AutoValue;

/** Represents a single instance of data collected about a specific {@link Feature}. */
@AutoValue
public abstract class Record {
  @Nullable
  public abstract String getId();

  @Nullable
  public abstract Project getProject();

  @Nullable
  public abstract Feature getFeature();

  @Nullable
  public abstract Form getForm();

  @Nullable
  public abstract User getCreatedBy();

  @Nullable
  public abstract User getModifiedBy();

  @Nullable
  public abstract Timestamps getServerTimestamps();

  @Nullable
  public abstract Timestamps getClientTimestamps();

  public abstract ResponseMap getResponses();

  public static Builder newBuilder() {
    return new AutoValue_Record.Builder().setResponses(ResponseMap.builder().build());
  }

  public abstract Record.Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setId(@Nullable String newId);

    public abstract Builder setProject(@Nullable Project project);

    public abstract Builder setFeature(@Nullable Feature feature);

    public abstract Builder setForm(@Nullable Form form);

    public abstract Builder setCreatedBy(@Nullable User user);

    public abstract Builder setModifiedBy(@Nullable User user);

    public abstract Builder setServerTimestamps(@Nullable Timestamps newServerTimestamps);

    public abstract Builder setClientTimestamps(@Nullable Timestamps newClientTimestamps);

    public abstract Builder setResponses(ResponseMap responses);

    public abstract Record build();
  }
}
