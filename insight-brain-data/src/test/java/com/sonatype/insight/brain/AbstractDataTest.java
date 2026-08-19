/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain;

import com.sonatype.insight.brain.dataaccess.ConditionTypesTestHelper;
import com.sonatype.insight.brain.dataaccess.DAOFactory;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.TestDAOFactory;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManagerProvider;
import com.sonatype.insight.brain.db.AbstractDatabaseTest;
import com.sonatype.insight.brain.db.rule.DatabaseRule;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;

import org.junit.Before;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * This base class is intended to be used <strong>ONLY</strong> when you need to test using a relational DB and you want
 * to leverage the TemporaryEntity to populate the DB on your tests.
 * <p>
 * This base test class will ensure a DB is provisioned and ready to be used in your tests. Check
 * {@link DatabaseRule} to see how to set up a DB depending on your needs.
 */
public abstract class AbstractDataTest
    extends AbstractDatabaseTest
{
  protected ClusterLockManager clusterLockManager;

  protected DAOFactory daoFactory;

  @Rule(order = 2)
  public TemporaryEntity tempEntity = createTemporaryEntity();

  @Before
  @BeforeEach
  public void initialize() {
    SystemConfigurationPropertyDAO.invalidateEntireCache();
    daoFactory = new TestDAOFactory(databaseRule);
    SystemConfigurationPropertyFeature.injectDependencies(daoFactory.createSystemConfigurationPropertyDAO());
    clusterLockManager = new ClusterLockManagerProvider(
        databaseRule.getOperationalDataStore(),
        daoFactory.createPostgresAdvisoryLockDAO()).get();

    // Re-inject classes that have static dependencies
    ConditionTypesTestHelper.initConditionTypes(daoFactory);
    ConditionTypesTestHelper.initConditionValueTypes(daoFactory);
  }

  // JUnit 5 (Jupiter): drive the @Rule(order=2) TemporaryEntity.before() (inert under Vintage). Runs before the
  // subclass @BeforeEach setup(). initialize() is dual-annotated (@Before + @BeforeEach) above so Jupiter invokes
  // it exactly once and a subclass override replaces it — do NOT call it explicitly here, or an overriding
  // @BeforeEach setUp would run twice (double DB inserts -> PK violations).
  @BeforeEach
  public void jupiterInitTempEntity() {
    tempEntity.before();
  }

  @AfterEach
  public void jupiterCleanupData() {
    tempEntity.after();
  }

  /**
   * Creates a new instance of {@link TemporaryEntity}. This method is protected to allow subclasses to override the
   * creation logic if needed, for example, to insert any dependencies or configurations used by the
   * {@link TemporaryEntity}
   *
   * @return a new instance of {@link TemporaryEntity}.
   */
  protected TemporaryEntity createTemporaryEntity() {
    return new TemporaryEntity(databaseRule);
  }
}
