/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.List;

import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.brain.testing.AbstractBaseIntegrationTest;
import com.sonatype.insight.brain.testing.DefaultInsightBrainServiceFactory;
import com.sonatype.insight.brain.testing.InsightBrainServiceFactory;

/**
 * Boots a single-tenant IQ server on an embedded (disk) H2 database. Wired through {@link IqH2Test}.
 */
public class IqH2ServerExtension
    extends AbstractIqServerExtension
{
  @H2DiskTest
  private static final class H2FixtureMarker
  {
  }

  @Override
  protected String variantKey() {
    return "iq-h2";
  }

  @Override
  protected Class<?> fixtureMarker() {
    return H2FixtureMarker.class;
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
