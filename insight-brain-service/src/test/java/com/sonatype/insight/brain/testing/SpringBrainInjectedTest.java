/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing;

import com.sonatype.insight.brain.StaticInjectionTestHelper;
import com.sonatype.insight.brain.dataaccess.DAOFactory;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.TestDAOFactory;
import com.sonatype.insight.brain.db.rule.DatabaseContainerRule;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.search.SearchIndexRule;
import com.sonatype.insight.test.SpringInjectedTest;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.springframework.test.context.ContextConfiguration;

/**
 * Spring-based test base class that replaces BrainInjectedTest.
 * Provides database and search test infrastructure with Spring dependency injection.
 *
 * <p>
 * This class handles creation of the four data store classes for tests.
 * The {@link DatabaseContainerRule} is a junit rule to create the instances
 * and inject them into the data stores.
 * </p>
 *
 * <p>
 * Tests extending this class will have access to:
 * <ul>
 * <li>{@link #daoFactory} - For creating DAOs before tests start</li>
 * <li>{@link #tempEntity} - For creating temporary test entities</li>
 * <li>{@link #databaseContainerRule} - For database access</li>
 * <li>{@link #searchIndexRule} - For search index access</li>
 * </ul>
 * </p>
 */
@ContextConfiguration(
    classes = SpringTestConfiguration.class,
    initializers = SearchTestPropertyInitializer.class)
public abstract class SpringBrainInjectedTest
    extends SpringInjectedTest
{
  /**
   * Note: As this will be the child class of the test, the database rule must be executed first.
   * This is very important as we need the data stores initialized first, in particular ahead of
   * other rules like {@link TemporaryEntity}
   */
  @Rule(order = 1)
  public DatabaseContainerRule databaseContainerRule =
      DatabaseContainerRule.getInstance(SpringBrainInjectedTest.class);

  @Rule(order = 2)
  public SearchIndexRule searchIndexRule = createSearchIndexRule();

  @Rule(order = 3)
  public TemporaryEntity tempEntity = createTemporaryEntity();

  /**
   * You should only use this `daoFactory` when you override the `configure` method and you need
   * to create DAOs there. Otherwise, always prefer the use of the @Inject annotation to inject
   * the DAOs you need for your test.
   */
  protected DAOFactory daoFactory;

  @BeforeClass
  public static void disableWaitToCloseOldClients() {
    HdsClient.waitToCloseOldClients = false;
  }

  @Before
  public final void initializeSpringBrainInjectedTestHarness() {
    // Re-inject classes that have static dependencies before any subclass @Before methods run.
    daoFactory = new TestDAOFactory(databaseContainerRule);
    StaticInjectionTestHelper.inject(daoFactory);

    // Spring context will inject fields automatically via @Inject
  }

  /**
   * Legacy extension hook retained for subclasses that still override {@code setUp()} and call
   * {@code super.setUp()}. The harness initialization now happens in
   * {@link #initializeSpringBrainInjectedTestHarness()} so overriding this method can no longer
   * interfere with base fixture setup ordering.
   */
  protected void setUp() throws Exception {
    // legacy no-op
  }

  /**
   * Creates a new instance of {@link TemporaryEntity}. This method is protected to allow
   * subclasses to override the creation logic if needed, for example, to insert any
   * dependencies or configurations used by the {@link TemporaryEntity}.
   *
   * @return a new instance of {@link TemporaryEntity}.
   */
  protected TemporaryEntity createTemporaryEntity() {
    return new TemporaryEntity(databaseContainerRule);
  }

  /**
   * Allows subclasses to opt out of the shared search fixture when they do not exercise search functionality.
   */
  protected SearchIndexRule createSearchIndexRule() {
    return SearchIndexRule.getInstance(SpringBrainInjectedTest.class);
  }
}
