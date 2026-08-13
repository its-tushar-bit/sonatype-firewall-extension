/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.test.SpringTestExecutionContext;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Postgres counterpart of {@link ComponentTestDbHarnessExtension}: the exact same ordered
 * DB/search/entity/Mockito Jupiter lifecycle, but the module's JVM-wide {@code DatabaseContainerRule}
 * is steered onto a real PostgreSQL fixture instead of the default in-memory H2.
 *
 * <p>
 * The steering is done the same way {@code IqPostgresServerExtension} boots the PG server variant:
 * a stable marker class carrying {@code @PostgresTest} is published as the "current test class" so
 * {@code DatabaseContainerRule.resolveFixtureAnnotation(...)} resolves the Postgres fixture. Because
 * the marker is the same class for every test in the module, the fixture-reuse "same class" check is
 * always satisfied — the embedded PostgreSQL cluster is provisioned exactly once and reused across
 * every class in {@code insight-brain-variant-test-component-pg}.
 *
 * <p>
 * <b>Timing matters.</b> {@code SpringExtension} builds (and caches) the {@code ApplicationContext}
 * — including the datasource beans wired from the {@code DatabaseContainerRule} — during
 * {@code postProcessTestInstance}, which runs BEFORE any {@link #beforeEach}. So the marker must be
 * published in {@link #beforeAll} (which runs before the context is ever built); publishing it only
 * in {@code beforeEach} would let the very first context build provision the default in-memory H2
 * fixture, and every subsequent test would then reuse that H2 fixture. {@code beforeEach} still
 * re-publishes the marker via {@link #fixtureClass} for each test, but {@code beforeAll} is what
 * guarantees the initial fixture is Postgres.
 *
 * <p>
 * Everything else (the outer aux rules, {@code @Mock} init, {@code SearchIndexRule},
 * {@code TemporaryEntity} per-test snapshot/restore, the base {@code @BeforeEach}/{@code @AfterEach}
 * chain, and the Shiro/authz path) is inherited unchanged from {@link ComponentTestDbHarnessExtension}.
 */
public class ComponentTestPgHarnessExtension
    extends ComponentTestDbHarnessExtension
    implements BeforeAllCallback
{
  /** Stable marker whose {@code @PostgresTest} annotation selects the Postgres fixture for the whole module. */
  @PostgresTest
  private static final class PostgresFixtureMarker
  {
  }

  @Override
  public void beforeAll(final ExtensionContext context) {
    // Publish the Postgres fixture marker BEFORE SpringExtension builds the cached context at
    // postProcessTestInstance, so the datasource beans are wired from a Postgres fixture rather than
    // the default in-memory H2 one.
    SpringTestExecutionContext.setCurrentTestClass(PostgresFixtureMarker.class);
  }

  @Override
  protected Class<?> fixtureClass(final Class<?> testClass) {
    return PostgresFixtureMarker.class;
  }
}
