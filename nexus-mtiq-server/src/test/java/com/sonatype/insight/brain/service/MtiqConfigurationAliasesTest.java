/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.sonatype.insight.brain.security.DefaultEncryptionKeyStore;
import com.sonatype.insight.brain.security.EncryptionKeyStore;
import com.sonatype.insight.brain.security.MultiTenantEncryptionKeyStore;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public class MtiqConfigurationAliasesTest
{
  @Test
  public void shouldResolveMultiTenantInsightConfigAliasWhenInsightConfigIsMultiTenant() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(ValidAliasBeans.class, MtiqConfigurationAliases.class);
      context.addBeanFactoryPostProcessor(markBeansAsLazy());
      context.refresh();

      assertThat(context.getBean("multiTenantInsightConfig", MultiTenantInsightConfig.class))
          .isSameAs(context.getBean("insightConfig"));
    }
  }

  @Test
  public void shouldFailWithActionableErrorWhenSingleTenantConfigIsUsedForMtiqAliases() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(InvalidAliasBeans.class, MtiqConfigurationAliases.class);
      context.addBeanFactoryPostProcessor(markBeansAsLazy());
      context.refresh();

      assertThatThrownBy(() -> context.getBean("multiTenantInsightConfig", MultiTenantInsightConfig.class))
          .rootCause()
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("MtiqConfigurationAliases")
          .hasMessageContaining(MultiTenantInsightConfig.class.getName())
          .hasMessageContaining(InsightConfig.class.getName())
          .hasMessageContaining("config.class=" + MultiTenantInsightConfig.class.getName());
    }
  }

  @Test
  public void shouldNotExposeMultiTenantEncryptionKeyStoreWhenUsingDefaultEncryptionKeyStore() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(DefaultEncryptionKeyStoreAliasBeans.class, MtiqConfigurationAliases.class);
      context.addBeanFactoryPostProcessor(markBeansAsLazy());
      context.refresh();

      assertThat(context.getBean(EncryptionKeyStore.class)).isSameAs(context.getBean(DefaultEncryptionKeyStore.class));
      assertThat(context.getBeansOfType(MultiTenantEncryptionKeyStore.class)).isEmpty();
    }
  }

  @Test
  public void shouldResolveMultiTenantEncryptionKeyStoreWhenUsingDedicatedEncryptionKeyStore() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(MultiTenantEncryptionKeyStoreAliasBeans.class, MtiqConfigurationAliases.class);
      context.addBeanFactoryPostProcessor(markBeansAsLazy());
      context.refresh();

      assertThat(context.getBean(EncryptionKeyStore.class))
          .isSameAs(context.getBean(MultiTenantEncryptionKeyStore.class));
      assertThat(context.getBean(EncryptionKeyStore.class))
          .isNotSameAs(context.getBean(DefaultEncryptionKeyStore.class));
      assertThat(context.getBeansOfType(MultiTenantEncryptionKeyStore.class))
          .containsKeys("encryptionKeyStore", "multiTenantEncryptionKeyStore")
          .allSatisfy((beanName, bean) -> assertThat(bean).isSameAs(context.getBean(EncryptionKeyStore.class)));
    }
  }

  private BeanFactoryPostProcessor markBeansAsLazy() {
    return beanFactory -> {
      for (String beanName : beanFactory.getBeanDefinitionNames()) {
        beanFactory.getBeanDefinition(beanName).setLazyInit(true);
      }
    };
  }

  @Configuration
  static class ValidAliasBeans
  {
    @Bean
    InsightConfig insightConfig() {
      return new MultiTenantInsightConfig();
    }
  }

  @Configuration
  static class InvalidAliasBeans
  {
    @Bean
    InsightConfig insightConfig() {
      return new InsightConfig();
    }
  }

  @Configuration
  static class DefaultEncryptionKeyStoreAliasBeans
  {
    @Bean
    InsightConfig insightConfig() {
      MultiTenantInsightConfig config = new MultiTenantInsightConfig();
      config.setUsingDefaultEncryptionKeyStore(true);
      return config;
    }

    @Bean
    DefaultEncryptionKeyStore defaultEncryptionKeyStore() {
      return mock(DefaultEncryptionKeyStore.class);
    }

    // Note: MultiTenantEncryptionKeyStore is NOT registered here.
    // In production, @ConditionalOnProperty prevents its registration
    // when using the default encryption keystore.
  }

  @Configuration
  static class MultiTenantEncryptionKeyStoreAliasBeans
  {
    @Bean
    InsightConfig insightConfig() {
      MultiTenantInsightConfig config = new MultiTenantInsightConfig();
      config.setUsingDefaultEncryptionKeyStore(false);
      return config;
    }

    @Bean
    DefaultEncryptionKeyStore defaultEncryptionKeyStore() {
      return mock(DefaultEncryptionKeyStore.class);
    }

    @Bean
    MultiTenantEncryptionKeyStore multiTenantEncryptionKeyStore() {
      return mock(MultiTenantEncryptionKeyStore.class);
    }
  }
}
