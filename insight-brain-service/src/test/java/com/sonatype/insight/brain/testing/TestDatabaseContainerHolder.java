/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing;

import com.sonatype.insight.brain.db.DatabaseContainer;

/**
 * ThreadLocal holder for the test's DatabaseContainer.
 * <p>
 * This bridges the timing gap between when
 * {@link SpringTestInsightBrainService#setDatabaseContainer(DatabaseContainer)}
 * is called (before Spring starts) and when Spring creates beans that need the DatabaseContainer.
 * <p>
 * Flow:
 * <ol>
 * <li>JUnit @Before creates DatabaseContainer via DatabaseContainerRule</li>
 * <li>Test calls setDatabaseContainer() which stores in this holder</li>
 * <li>Test calls start() which boots Spring</li>
 * <li>TestDatabaseConfiguration.beanContainer() retrieves from holder</li>
 * <li>Other beans (Quartz, Configuration) successfully inject DatabaseContainer</li>
 * </ol>
 */
public final class TestDatabaseContainerHolder
{
  private static final ThreadLocal<DatabaseContainer> HOLDER = new ThreadLocal<>();

  private TestDatabaseContainerHolder() {
    // Utility class - no instantiation
  }

  /**
   * Store the DatabaseContainer for the current thread.
   */
  public static void set(DatabaseContainer container) {
    HOLDER.set(container);
  }

  /**
   * Retrieve the DatabaseContainer for the current thread.
   *
   * @return the stored DatabaseContainer, or null if not set
   */
  public static DatabaseContainer get() {
    return HOLDER.get();
  }

  /**
   * Clear the stored DatabaseContainer for the current thread.
   * Should be called in test teardown to prevent memory leaks.
   */
  public static void clear() {
    HOLDER.remove();
  }
}
