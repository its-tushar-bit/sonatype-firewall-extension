/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonatype.insight.brain.spring.config.NamedBeanRegistrationConfiguration;
import java.util.Arrays;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.Import;

public class MultiTenantDataAccessConfigurationTest
{
  @Test
  public void shouldImportSharedNamedBeanRegistrationBeforeMtiqBeanDefinitionAdjustments() {
    Import importAnnotation = MultiTenantInsightBrainService.class.getAnnotation(Import.class);

    assertThat(Arrays.asList(importAnnotation.value()))
        .containsExactly(NamedBeanRegistrationConfiguration.class, MultiTenantDataAccessConfiguration.class);
  }

  @Test
  public void shouldNotRegisterNamedBeansWhileAdjustingMtiqBeanDefinitions() {
    CountingBeanFactory registry = new CountingBeanFactory();

    new MultiTenantDataAccessConfiguration().postProcessBeanDefinitionRegistry(registry);

    assertThat(registry.getRegistrationCount()).isZero();
  }

  private static final class CountingBeanFactory
      extends DefaultListableBeanFactory
  {
    private int registrationCount;

    @Override
    public void registerBeanDefinition(final String beanName, final BeanDefinition beanDefinition) {
      registrationCount++;
      super.registerBeanDefinition(beanName, beanDefinition);
    }

    int getRegistrationCount() {
      return registrationCount;
    }
  }
}
