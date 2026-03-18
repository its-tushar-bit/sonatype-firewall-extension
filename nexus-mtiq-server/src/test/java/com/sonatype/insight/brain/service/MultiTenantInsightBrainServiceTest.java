/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.List;
import java.util.Set;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.dashboard.ApplicationRiskService;
import com.sonatype.insight.brain.dashboard.DashboardComponentRiskService;
import com.sonatype.insight.brain.dashboard.DashboardViolationRiskService;
import com.sonatype.insight.brain.dashboard.PostgresApplicationRiskService;
import com.sonatype.insight.brain.dashboard.PostgresComponentRiskService;
import com.sonatype.insight.brain.dashboard.PostgresDashboardViolationRiskService;
import com.sonatype.insight.brain.security.DefaultEncryptionKeyStore;
import com.sonatype.insight.brain.security.EncryptionKeyStore;
import com.sonatype.insight.brain.security.MultiTenantEncryptionKeyStore;
import com.sonatype.insight.brain.tenancy.MultiTenantTenantManagedInitializer;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import com.sonatype.insight.brain.tenancy.TenantUtil;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class MultiTenantInsightBrainServiceTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  @Inject
  private Set<TenantManaged> tenantLifecycles;

  @Test
  public void shouldExcludeDefaultTenantManagedInitializer() {
    assertThat(getCLMServer().getInstance(TenantManagedInitializer.class))
        .isInstanceOf(MultiTenantTenantManagedInitializer.class);

    assertThat(getCLMServer().getInstance(TenantManagedInitializer.class))
        .isNotInstanceOf(DefaultTenantManagedInitializer.class);
  }

  @Test
  @ManualIqServerInit
  public void shouldNotBindAwsRelatedClasses_WhenUsingDefaultEncryptionKeyStore() throws Exception {
    startIqTestServer(new MtiqDatabaseConfigurator()
    {
      @Override
      public void configure(InsightConfig config) {
        super.configure(config);
        ((MultiTenantInsightConfig) config).setUsingDefaultEncryptionKeyStore(true);
      }
    });

    assertThat(getCLMServer().getInstance(EncryptionKeyStore.class)).isInstanceOf(DefaultEncryptionKeyStore.class);
    getCLMServer().getInjector().injectMembers(this);
    assertThat(tenantLifecycles.stream().anyMatch(t -> t instanceof Configuration)).isTrue();
    assertThat(tenantLifecycles.stream().anyMatch(t -> t instanceof MultiTenantEncryptionKeyStore)).isFalse();
  }

  @Test
  @ManualIqServerInit
  public void shouldBindAwsRelatedClasses_WhenNotUsingDefaultEncryptionKeyStore() throws Exception {
    startIqTestServer(new MtiqDatabaseConfigurator()
    {
      @Override
      public void configure(InsightConfig config) {
        super.configure(config);
        ((MultiTenantInsightConfig) config).setUsingDefaultEncryptionKeyStore(false);
      }
    });

    assertThat(getCLMServer().getInstance(EncryptionKeyStore.class)).isInstanceOf(MultiTenantEncryptionKeyStore.class);
    getCLMServer().getInjector().injectMembers(this);
    assertThat(tenantLifecycles.stream().anyMatch(t -> t instanceof Configuration)).isTrue();
    assertThat(tenantLifecycles.stream().anyMatch(t -> t instanceof MultiTenantEncryptionKeyStore)).isTrue();
  }

  @Test
  public void testInitialize_correctDbBasedInstancesInjected() {
    assertThat(getCLMServer().getInstance(DashboardViolationRiskService.class)).isInstanceOf(
        PostgresDashboardViolationRiskService.class);
    assertThat(getCLMServer().getInstance(ApplicationRiskService.class))
        .isInstanceOf(PostgresApplicationRiskService.class);
    assertThat(getCLMServer().getInstance(DashboardComponentRiskService.class))
        .isInstanceOf(PostgresComponentRiskService.class);
  }

  @Override
  protected List<Module> getBrainModules() {
    List<Module> modules = super.getBrainModules();
    modules.add(new AbstractModule()
    {
      @Override
      protected void configure() {
        bind(TenantUtil.class).toInstance(tenantUtil);
        bind(MultiTenantInsightBrainServiceTest.class);
      }
    });
    return modules;
  }

  @Override
  protected boolean shouldBindTestEncryptionKeyStore() {
    // This test manages its own EncryptionKeyStore binding
    return false;
  }
}
