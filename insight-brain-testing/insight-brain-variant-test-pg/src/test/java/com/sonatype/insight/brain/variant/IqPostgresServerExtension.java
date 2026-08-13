/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.List;

import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.brain.testing.AbstractBaseIntegrationTest;
import com.sonatype.insight.brain.testing.DefaultInsightBrainServiceFactory;
import com.sonatype.insight.brain.testing.InsightBrainServiceFactory;

/**
 * Boots a single-tenant IQ server on a real PostgreSQL database. Uses the codebase's existing
 * embedded-postgres (zonky) test cluster via the {@code @PostgresTest} fixture — no new
 * Testcontainers dependency. Wired through {@link IqPostgresTest}.
 */
public class IqPostgresServerExtension
    extends AbstractIqServerExtension
{
  @PostgresTest
  private static final class PostgresFixtureMarker
  {
  }

  @Override
  protected String variantKey() {
    return "iq-postgres";
  }

  @Override
  protected Class<?> fixtureMarker() {
    return PostgresFixtureMarker.class;
  }

  @Override
  protected InsightBrainServiceFactory serviceFactory() {
    return new DefaultInsightBrainServiceFactory();
  }

  @Override
  protected Configurator configurator() {
    return SpikeSupport.REUSABLE_NOOP_CONFIGURATOR;
  }

  @Override
  protected List<Class<?>> testConfigurations() {
    return List.of(AbstractBaseIntegrationTest.BaseIntegrationTestConfigurationWithTestEncryptionKeyStore.class);
  }

}
