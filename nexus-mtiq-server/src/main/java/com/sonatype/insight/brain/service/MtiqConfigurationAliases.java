/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.security.DefaultEncryptionKeyStore;
import com.sonatype.insight.brain.security.EncryptionKeyStore;
import com.sonatype.insight.brain.security.MultiTenantEncryptionKeyStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MtiqConfigurationAliases
{
  @Bean
  public MultiTenantInsightConfig multiTenantInsightConfig(InsightConfig insightConfig) {
    return MtiqConfigSupport.requireMultiTenantInsightConfig(
        insightConfig,
        "MtiqConfigurationAliases.multiTenantInsightConfig");
  }

  @Bean
  @Primary
  public EncryptionKeyStore encryptionKeyStore(
      @Qualifier("insightConfig") InsightConfig insightConfig,
      DefaultEncryptionKeyStore defaultEncryptionKeyStore,
      ObjectProvider<MultiTenantEncryptionKeyStore> multiTenantEncryptionKeyStoreProvider)
  {
    MultiTenantInsightConfig multiTenantInsightConfig = MtiqConfigSupport.requireMultiTenantInsightConfig(
        insightConfig,
        "MtiqConfigurationAliases.encryptionKeyStore");
    if (multiTenantInsightConfig.isUsingDefaultEncryptionKeyStore()) {
      return defaultEncryptionKeyStore;
    }

    MultiTenantEncryptionKeyStore multiTenantEncryptionKeyStore = multiTenantEncryptionKeyStoreProvider
        .getIfAvailable();
    if (multiTenantEncryptionKeyStore == null) {
      throw new IllegalStateException(
          "MultiTenantEncryptionKeyStore is required when usingDefaultEncryptionKeyStore=false");
    }
    return multiTenantEncryptionKeyStore;
  }
}
