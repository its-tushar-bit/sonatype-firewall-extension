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
import com.sonatype.insight.brain.spring.DropwizardConfigBootstrap;
import com.sonatype.insight.brain.spring.config.DropwizardConfigConfiguration;
import org.junit.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

public class SpringMultiTenantConfigBootstrapValidationTest
{
  @Test
  public void shouldFailWithActionableErrorWhenMtiqBootstrapUsesDefaultConfigClass() {
    SpringApplicationBuilder builder = new SpringApplicationBuilder(TestMtiqBootstrapApplication.class)
        .profiles("test")
        .properties(
            "spring.main.web-application-type=none",
            "spring.main.lazy-initialization=false",
            "spring.main.allow-bean-definition-overriding=true");
    DropwizardConfigBootstrap.configure(builder, TestInsightBrainService.DEFAULT_CONFIG_FILE_PATH);

    assertThatThrownBy(builder::run)
        .rootCause()
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("MultiTenantInsightConfig")
        .hasMessageContaining(InsightConfig.class.getName())
        .hasMessageContaining("config.class=" + MultiTenantInsightConfig.class.getName());
  }

  @Test
  public void shouldResolveMultiTenantEncryptionKeyStoreWhenMtiqBootstrapUsesMultiTenantConfigClass() {
    SpringApplicationBuilder builder = new SpringApplicationBuilder(TestMtiqBootstrapApplication.class)
        .profiles("test")
        .properties(
            "spring.main.web-application-type=none",
            "spring.main.lazy-initialization=false",
            "spring.main.allow-bean-definition-overriding=true");
    DropwizardConfigBootstrap.configure(
        builder,
        TestInsightBrainService.DEFAULT_CONFIG_FILE_PATH,
        MultiTenantInsightConfig.class);

    try (ConfigurableApplicationContext context = builder.run()) {
      assertThat(context.getBean(MultiTenantInsightConfig.class)).isSameAs(context.getBean(InsightConfig.class));
      assertThat(context.getBean(EncryptionKeyStore.class))
          .isSameAs(context.getBean(MultiTenantEncryptionKeyStore.class));
    }
  }

  @SpringBootConfiguration
  @EnableAutoConfiguration
  @Import({
    DropwizardConfigConfiguration.class,
    MtiqConfigurationAliases.class
  })
  static class TestMtiqBootstrapApplication
  {
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
