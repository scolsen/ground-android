/*
 * Copyright 2021 Google LLC
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

package com.google.android.ground.ui.home.featuredetails;

import static com.google.common.truth.Truth.assertThat;

import com.google.android.ground.TestObservers;
import com.google.android.ground.model.User;
import com.google.android.ground.model.locationofinterest.LocationOfInterest;
import dagger.hilt.android.testing.HiltAndroidTest;
import java.util.Arrays;
import java.util.Collection;
import java8.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.ParameterizedRobolectricTestRunner.Parameters;

@HiltAndroidTest
@RunWith(ParameterizedRobolectricTestRunner.class)
public class DeleteMenuVisibilityTest extends BaseMenuVisibilityTest {

  public DeleteMenuVisibilityTest(
      User user, LocationOfInterest locationOfInterest, boolean visible) {
    super(user, locationOfInterest, visible);
  }

  @Parameters
  public static Collection<Object[]> data() {
    Object[][] data = {

      // Point locationOfInterest created by some other user
      {TEST_USER_OWNER, createPointFeature(TEST_USER_UNKNOWN), true},
      {TEST_USER_MANAGER, createPointFeature(TEST_USER_UNKNOWN), true},
      {TEST_USER_CONTRIBUTOR, createPointFeature(TEST_USER_UNKNOWN), false},

      // Polygon locationOfInterest created by some other user
      {TEST_USER_OWNER, createPolygonFeature(TEST_USER_UNKNOWN), true},
      {TEST_USER_MANAGER, createPolygonFeature(TEST_USER_UNKNOWN), true},
      {TEST_USER_CONTRIBUTOR, createPolygonFeature(TEST_USER_UNKNOWN), false},

      // Current user created the selected locationOfInterest
      {TEST_USER_CONTRIBUTOR, createPointFeature(TEST_USER_CONTRIBUTOR), true},
      {TEST_USER_CONTRIBUTOR, createPolygonFeature(TEST_USER_CONTRIBUTOR), true},
    };
    return Arrays.asList(data);
  }

  @Test
  public void testDeleteMenuVisible() {
    fakeAuthenticationManager.setUser(user);
    viewModel.onLocationOfInterestSelected(Optional.of(locationOfInterest));

    TestObservers.observeUntilFirstChange(viewModel.isDeleteMenuOptionVisible());
    assertThat(viewModel.isDeleteMenuOptionVisible().getValue()).isEqualTo(visible);
  }
}
