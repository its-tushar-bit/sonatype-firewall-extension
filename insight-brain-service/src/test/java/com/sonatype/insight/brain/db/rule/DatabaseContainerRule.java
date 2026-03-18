/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.rule;

import com.sonatype.insight.brain.db.DatabaseConfigProvider;
import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.db.DatabaseName;
import com.sonatype.insight.brain.db.TestDatabaseContainer;
import com.sonatype.insight.brain.service.InsightBrainService;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.db.DatabaseEngine;

/**
 * <p>
 * Extends the {@link DatabaseRule} (see those javadocs) with the {@link DatabaseContainer} required for the main
 * application (i.e. {@link InsightBrainService}).
 *
 * <p>
 * Example:
 *
 * <pre>
 * {@code
 * @Rule(order = 1)
 * public DatabaseContainerRule databaseRule = DatabaseContainerRule.getInstance();
 * }
 * </pre>
 * </p>
 * <p>
 * See also the javadoc in {@link DatabaseRule}
 * </p>
 */
public class DatabaseContainerRule
    extends DatabaseRule
{
  private static final DatabaseContainerRule INSTANCE = new DatabaseContainerRule();

  private static Class<?> currentTestClassType;

  private TestDatabaseContainer databaseContainer;

  protected DatabaseContainerRule() {
    // private constructor for singleton enforcement
  }

  /**
   * Return the singleton {@link DatabaseContainer}
   *
   * @param baseTestClassType you should pass in the BASE test class type here. The value is tracked between subsequent
   *          tests and when the value changes it is considered as making the currently active database
   *          NOT reusable and therefore a fresh database will be automatically be re-provisioned
   */
  public static DatabaseContainerRule getInstance(Class<?> baseTestClassType) {
    if (currentTestClassType != baseTestClassType) {
      INSTANCE.markFixtureAsDirty();
      currentTestClassType = baseTestClassType;
    }

    return INSTANCE;
  }

  @Override
  protected void before() throws Throwable {
    super.before();

    if (hasFixtureTypeChanged() || !isFixtureReusable()) {
      this.databaseContainer = createTestDatabaseContainer();
    }
  }

  private TestDatabaseContainer createTestDatabaseContainer() {
    log.info("Creating new test DatabaseContainer");
    return new TestDatabaseContainer(getDataSourceProvider(), this);
  }

  public DatabaseContainer getDatabaseContainer() {
    return databaseContainer;
  }

  public DatabaseConfigProvider getDatabaseConfigProvider() {
    return new DatabaseConfigProvider()
    {
      @Override
      public DatabaseConfig getDatabaseConfig(final DatabaseName databaseName) {
        return DatabaseContainerRule.this.getDatabaseConfig(databaseName.name());
      }

      @Override
      public DatabaseEngine getDatabaseEngine() {
        return getDatabaseEngine();
      }
    };
  }

  public void resetMocks() {
    databaseContainer.resetMocks();
  }
}
