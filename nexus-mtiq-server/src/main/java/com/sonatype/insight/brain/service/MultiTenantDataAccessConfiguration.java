/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.dashboard.H2ApplicationRiskService;
import com.sonatype.insight.brain.dashboard.H2ComponentRiskService;
import com.sonatype.insight.brain.dashboard.H2DashboardViolationRiskService;
import com.sonatype.insight.brain.dashboard.PostgresApplicationRiskService;
import com.sonatype.insight.brain.dashboard.PostgresComponentRiskService;
import com.sonatype.insight.brain.dashboard.PostgresDashboardViolationRiskService;
import com.sonatype.insight.brain.support.SupportResource;
import com.sonatype.insight.brain.support.SupportService;
import java.beans.Introspector;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Configuration;

/**
 * Applies MTIQ-specific bean definition adjustments after the shared Named-bean
 * registration path has populated the registry.
 */
@Configuration
public class MultiTenantDataAccessConfiguration
    implements BeanDefinitionRegistryPostProcessor
{
  @Override
  public void postProcessBeanDefinitionRegistry(final BeanDefinitionRegistry registry) throws BeansException {
    // Keep IQ-only support beans and the single-tenant tenant-managed initializer out of MTIQ startup even when
    // they are discovered by the shared Named-bean registration path.
    removeBeanDefinitionIfPresent(registry, SupportResource.class);
    removeBeanDefinitionIfPresent(registry, SupportService.class);
    removeBeanDefinitionIfPresent(registry, DefaultTenantManagedInitializer.class);
    preferPostgresRiskServices(registry);
  }

  @Override
  public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory) throws BeansException {
    if (beanFactory instanceof BeanDefinitionRegistry registry) {
      removeBeanDefinitionIfPresent(registry, SupportResource.class);
      removeBeanDefinitionIfPresent(registry, SupportService.class);
      removeBeanDefinitionIfPresent(registry, DefaultTenantManagedInitializer.class);
      preferPostgresRiskServices(registry);
    }
  }

  private void preferPostgresRiskServices(final BeanDefinitionRegistry registry) {
    // The single-tenant selector beans remain registered in MTIQ, but they must not stay primary once the
    // concrete Postgres implementations are forced for the multi-tenant runtime. Otherwise Spring sees two
    // primary candidates for the same interface and Jersey resource creation fails during startup.
    setPrimaryFlag(registry, "dashboardViolationRiskService", false);
    setPrimaryFlag(registry, H2DashboardViolationRiskService.class, false);
    setPrimaryFlag(registry, PostgresDashboardViolationRiskService.class, true);

    setPrimaryFlag(registry, "applicationRiskService", false);
    setPrimaryFlag(registry, H2ApplicationRiskService.class, false);
    setPrimaryFlag(registry, PostgresApplicationRiskService.class, true);

    setPrimaryFlag(registry, "dashboardComponentRiskService", false);
    setPrimaryFlag(registry, H2ComponentRiskService.class, false);
    setPrimaryFlag(registry, PostgresComponentRiskService.class, true);
  }

  private void setPrimaryFlag(
      final BeanDefinitionRegistry registry,
      final Class<?> beanType,
      final boolean primary)
  {
    setPrimaryFlag(registry, Introspector.decapitalize(beanType.getSimpleName()), primary);
  }

  private void setPrimaryFlag(
      final BeanDefinitionRegistry registry,
      final String beanName,
      final boolean primary)
  {
    if (registry.containsBeanDefinition(beanName)) {
      registry.getBeanDefinition(beanName).setPrimary(primary);
    }
  }

  private void removeBeanDefinitionIfPresent(final BeanDefinitionRegistry registry, final Class<?> beanType) {
    String beanName = Introspector.decapitalize(beanType.getSimpleName());
    if (registry.containsBeanDefinition(beanName)) {
      registry.removeBeanDefinition(beanName);
    }
  }
}
