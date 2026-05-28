/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.AdminApiPaths;
import com.sonatype.insight.brain.api.admin.TenantResource;
import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesResource;
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
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@Category(SlowTest.class)
public class MultiTenantInsightBrainServiceTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  @Test
  public void shouldExcludeDefaultTenantManagedInitializer() {
    assertThat(getCLMServer().getInstance(TenantManagedInitializer.class))
        .isInstanceOf(MultiTenantTenantManagedInitializer.class);

    assertThat(getCLMServer().getInstance(TenantManagedInitializer.class))
        .isNotInstanceOf(DefaultTenantManagedInitializer.class);
  }

  @Test
  @ManualIqServerInit
  public void shouldNotBindAwsRelatedClasses_WhenUsingDefaultEncryptionKeyStore() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(EncryptionKeyStoreAliasTestFixtures.DefaultEncryptionKeyStoreAliasBeans.class,
          EncryptionKeyStoreAliasTestFixtures.TenantManagedBeans.class,
          MtiqConfigurationAliases.class);
      context.refresh();

      assertThat(context.getBean("multiTenantInsightConfig", MultiTenantInsightConfig.class)
          .isUsingDefaultEncryptionKeyStore()).isTrue();
      assertThat(context.getBean(EncryptionKeyStore.class)).isSameAs(context.getBean(DefaultEncryptionKeyStore.class));
      assertThat(context.getBeansOfType(MultiTenantEncryptionKeyStore.class)).isEmpty();

      assertThat(context.getBeansOfType(Configuration.class)).isNotEmpty();
      assertThat(context.getBeansOfType(MultiTenantEncryptionKeyStore.class)).isEmpty();
    }
  }

  @Test
  @ManualIqServerInit
  public void shouldBindAwsRelatedClasses_WhenNotUsingDefaultEncryptionKeyStore() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(EncryptionKeyStoreAliasTestFixtures.MultiTenantEncryptionKeyStoreAliasBeans.class,
          EncryptionKeyStoreAliasTestFixtures.TenantManagedBeans.class,
          MtiqConfigurationAliases.class);
      context.refresh();

      assertThat(context.getBean("multiTenantInsightConfig", MultiTenantInsightConfig.class)
          .isUsingDefaultEncryptionKeyStore()).isFalse();
      assertThat(context.getBean(EncryptionKeyStore.class))
          .isSameAs(context.getBean(MultiTenantEncryptionKeyStore.class));
      assertThat(context.getBeansOfType(MultiTenantEncryptionKeyStore.class))
          .containsKeys("encryptionKeyStore", "multiTenantEncryptionKeyStore");

      assertThat(context.getBeansOfType(Configuration.class)).isNotEmpty();
      assertThat(context.getBeansOfType(MultiTenantEncryptionKeyStore.class)).isNotEmpty();
    }
  }

  @Test
  public void shouldExcludeIqOnlyResourcesFromMtiqMainResourceConfig() {
    ResourceConfig mainResourceConfig = getCLMServer()
        .getApplicationContext()
        .getBean("mtiqMainResourceConfig", ResourceConfig.class);

    assertThat(getCLMServer().getApplicationContext().containsBean("resourceConfig")).isFalse();
    assertThat(mainResourceConfig.getInstances())
        .extracting(instance -> instance.getClass().getName())
        .doesNotContain(ApiConfigFeaturesResource.class.getName());
  }

  @Test
  public void shouldCollectAdminResourcesIntoAdminBundle() {
    AdminResourceBundle adminResourceBundle = getCLMServer().getInstance(AdminResourceBundle.class);

    assertThat(adminResourceBundle).isNotNull();
    assertThat(adminResourceBundle.getRegisteredResources())
        .anyMatch(resource -> resource.getClass().equals(TenantResource.class));
  }

  @Test
  public void shouldExposeAdminResourcesOnAdminSurface() throws Exception {
    HttpResponse response = adminRestRequest(AdminApiPaths.ADMIN_PATH + TenantResource.LIST_TENANTS)
        .query("tenant=global")
        .get();

    assertResponseStatus(200, response);
    assertThat(response.getBodyList()).contains(getTestTenant().tenantSlug);
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
  protected boolean shouldBindTestEncryptionKeyStore() {
    return true;
  }
}
