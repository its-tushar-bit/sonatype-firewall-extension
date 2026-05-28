/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import static org.mockito.Mockito.mock;

import com.sonatype.insight.brain.security.DefaultEncryptionKeyStore;
import com.sonatype.insight.brain.security.MultiTenantEncryptionKeyStore;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

final class EncryptionKeyStoreAliasTestFixtures
{
  private EncryptionKeyStoreAliasTestFixtures() {
  }

  @TestConfiguration
  static class TenantManagedBeans
  {
    @Bean
    Configuration configuration() {
      return mock(Configuration.class);
    }
  }

  @TestConfiguration
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
  }

  @TestConfiguration
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
