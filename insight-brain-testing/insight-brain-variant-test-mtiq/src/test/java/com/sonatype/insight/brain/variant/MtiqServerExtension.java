/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.List;

import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.db.rule.MultiTenantDatabaseContainerRule;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationTest;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;
import com.sonatype.insight.brain.testing.AbstractBaseIntegrationTest;
import com.sonatype.insight.brain.testing.InsightBrainServiceFactory;
import com.sonatype.insight.brain.testing.MultiTenantTestInsightBrainServiceFactory;
import com.sonatype.insight.test.SpringTestExecutionContext;

/**
 * Boots the multi-tenant IQ server ({@code MultiTenantInsightBrainService}) once for the MTIQ variant
 * and reuses it across every {@code @MtiqTest}. The DB comes from the shared multi-tenant
 * embedded-postgres cluster ({@link MultiTenantDatabaseContainerRule}); requests run under the GLOBAL
 * tenant, which is provisioned at server startup. {@code ui/links/*} is tenant-agnostic and
 * unlicensed, so it is reachable without per-test tenant provisioning.
 */
public class MtiqServerExtension
    extends AbstractMtiqServerExtension
{
  @PostgresTest
  private static final class MtiqFixtureMarker
  {
  }

  @Override
  protected String variantKey() {
    return "mtiq";
  }

  @Override
  protected DatabaseContainer provisionDatabase() {
    // The MTIQ bean graph eagerly builds an AWS SecretsManager client (for RotateEncryptionKeyTask).
    // CI provides AWS_REGION; locally we set a dummy region so the client constructs. It is never
    // called by the ui/links endpoints, so no AWS access occurs.
    if (System.getProperty("aws.region") == null && System.getenv("AWS_REGION") == null) {
      System.setProperty("aws.region", "us-east-1");
    }

    // Multi-tenant mode must be active before the container rule initialises, and requests default to
    // the global tenant (TestRestTenantUtil returns null slug -> global) so the spike needs no
    // per-test tenant provisioning.
    TenantTestHelper.initMultiTenantMode();
    TenantTestHelper.setGlobalTenant();
    SpringTestExecutionContext.setCurrentTestClass(MtiqFixtureMarker.class);
    MultiTenantDatabaseContainerRule rule = MultiTenantDatabaseContainerRule.getInstance();
    rule.setTestName("mtiq_spike");
    rule.ensureInitializedForSpringContext();
    return rule.getDatabaseContainer();
  }

  @Override
  protected InsightBrainServiceFactory serviceFactory() {
    return new MultiTenantTestInsightBrainServiceFactory();
  }

  @Override
  protected Configurator configurator() {
    return new AbstractMultiTenantBaseIntegrationTest.MtiqDatabaseConfigurator();
  }

  @Override
  protected List<Class<?>> testConfigurations() {
    return List.of(
        AbstractBaseIntegrationTest.BaseIntegrationTestConfigurationWithTestEncryptionKeyStore.class,
        AbstractMultiTenantBaseIntegrationTest.MtiqTestConfiguration.class,
        AbstractMultiTenantBaseIntegrationTest.MtiqTestConfigurationWithTestEncryptionKeyStore.class);
  }
}
