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

package com.google.android.gnd.persistence.local.room;

import com.google.android.gnd.model.Mutation;

/**
 * Checked exception used internally to ensure we catch and properly handle errors in the Room local
 * db implementation.
 */
class LocalDataStoreException extends Exception {

  public LocalDataStoreException(String message) {
    super(message);
  }

  /**
   * Returns a new {@code LocalDataStoreException} whose message indicates the mutation type is
   * unknown.
   */
  public static LocalDataStoreException unknownMutationType(Mutation.Type type) {
    return new LocalDataStoreException("Unknown Mutation.Type." + type);
  }

  /**
   * Returns a new {@code LocalDataStoreException} whose message indicates the mutation type is
   * unknown.
   */
  public static LocalDataStoreException unknownMutationClass(Class clz) {
    return new LocalDataStoreException("Unknown mutation " + clz);
  }
}
