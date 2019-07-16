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

package com.google.android.gnd.model.layer;

import static java8.util.stream.StreamSupport.stream;

import com.google.android.gnd.model.Timestamps;
import com.google.android.gnd.model.form.Form;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java8.util.Optional;
import javax.annotation.Nullable;

@AutoValue
public abstract class FeatureType {
  @Nullable
  public abstract String getId();

  @Nullable
  public abstract String getListHeading();

  @Nullable
  public abstract String getItemLabel();

  @Nullable
  public abstract String getIconId();

  @Nullable
  public abstract String getIconColor();

  public abstract ImmutableList<Form> getForms();

  @Nullable
  public abstract Timestamps getServerTimestamps();

  @Nullable
  public abstract Timestamps getClientTimestamps();

  public Optional<Form> getForm(String formId) {
    return stream(getForms()).filter(form -> form.getId().equals(formId)).findFirst();
  }

  public static Builder newBuilder() {
    return new AutoValue_FeatureType.Builder().setClientTimestamps(Timestamps.getDefaultInstance());
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setId(@Nullable String newId);

    public abstract Builder setListHeading(@Nullable String newListHeading);

    public abstract Builder setItemLabel(@Nullable String newItemLabel);

    public abstract Builder setIconId(@Nullable String newIconId);

    public abstract Builder setIconColor(@Nullable String newIconColor);

    public abstract ImmutableList.Builder<Form> formsBuilder();

    public Builder addForm(Form newForm) {
      formsBuilder().add(newForm);
      return this;
    }

    public abstract Builder setServerTimestamps(@Nullable Timestamps newServerTimestamps);

    public abstract Builder setClientTimestamps(@Nullable Timestamps newClientTimestamps);

    public abstract FeatureType build();
  }
}
