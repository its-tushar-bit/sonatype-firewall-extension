/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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
  private static final AtomicBoolean LISTENER_REGISTERED = new AtomicBoolean();

  private static final ThreadLocal<List<Object>> MOCKS = ThreadLocal.withInitial(ArrayList::new);

  private static final MockCreationListener MOCK_CREATION_LISTENER =
      (Object mock, @SuppressWarnings("rawtypes") MockCreationSettings settings) -> MOCKS.get().add(mock);

  @Override
  protected void before() {
    if (LISTENER_REGISTERED.compareAndSet(false, true)) {
      Mockito.framework().addListener(MOCK_CREATION_LISTENER);
    }
    MOCKS.get().clear();
  }

  @Override
  protected void after() {
    List<Object> mocks = MOCKS.get()
        .stream()
        .filter(mock -> Mockito.mockingDetails(mock).isMock())
        .toList();
    try {
      Mockito.reset(mocks.toArray());
    }
    finally {
      MOCKS.get().clear();
      MOCKS.remove();
    }
  }
}
