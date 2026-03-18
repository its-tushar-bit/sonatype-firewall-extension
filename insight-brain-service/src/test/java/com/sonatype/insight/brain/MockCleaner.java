/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain;

import java.util.ArrayList;
import java.util.List;

import org.junit.rules.ExternalResource;
import org.mockito.Mockito;
import org.mockito.listeners.MockCreationListener;
import org.mockito.mock.MockCreationSettings;

/**
 * When a mock method is called with some parameters, mockito holds strong references to all parameter instances
 * (because it might need to use them to compare parameter values when it verifies invocations).
 * The parameter strong references are not released if the test does not explicitly verify all mock invocations.
 * In practice, it's next to impossible to verify all invocations and many tests don't do that because not all
 * invocations are important for all tests.
 *
 * This causes memory leaks and when they accumulate, OutOfMemoryErrors.
 *
 * This class registers a listener for mockito mock creations and records all mocks created by a test.
 * After the test, it clears all invocations for all mocks, which cause mockito to release the strong references to all
 * invocation parameters.
 */
public class MockCleaner
    extends ExternalResource
{
  private List<Object> mocks = new ArrayList<>();

  private MockCreationListener mockCreationListener = new MockCreationListener()
  {
    @Override
    public void onMockCreated(Object mock, @SuppressWarnings("rawtypes") MockCreationSettings settings) {
      mocks.add(mock);
    }
  };

  @Override
  protected void before() {
    Mockito.framework().addListener(mockCreationListener);
  }

  @Override
  protected void after() {
    Mockito.reset(mocks.toArray());
    Mockito.framework().removeListener(mockCreationListener);
    mocks.clear();
  }
}
