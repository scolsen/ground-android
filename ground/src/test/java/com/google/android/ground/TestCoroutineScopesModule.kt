/*
 * Copyright 2023 Google LLC
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
package com.google.android.ground

import com.google.android.ground.coroutines.ApplicationScope
import com.google.android.ground.coroutines.CoroutinesScopesModule
import com.google.android.ground.coroutines.DefaultDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [CoroutinesScopesModule::class])
object TestCoroutineScopesModule {
  @OptIn(ExperimentalCoroutinesApi::class)
  @ApplicationScope
  @Singleton
  @Provides
  fun provideCoroutineScope(
    @DefaultDispatcher testDispatcher: CoroutineDispatcher
  ): CoroutineScope = TestScope(testDispatcher)
}