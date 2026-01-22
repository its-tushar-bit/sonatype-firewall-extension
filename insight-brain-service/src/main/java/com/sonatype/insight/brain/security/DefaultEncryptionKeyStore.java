/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.service.InsightConfig;

import ru.vyarus.dropwizard.guice.module.installer.scanner.InvisibleForScanner;

import static com.sonatype.insight.brain.security.keystore.KeyStoreFactory.getDefaultEncryptionKeyStoreKey;

@Named
@Singleton
@InvisibleForScanner
public class DefaultEncryptionKeyStore
    implements EncryptionKeyStore
{
  private final InsightConfig insightConfig;

  @Inject
  public DefaultEncryptionKeyStore(final InsightConfig insightConfig) {
    this.insightConfig = insightConfig;
  }

  @Override
  public String getKey() {
    try {
      return getDefaultEncryptionKeyStoreKey(insightConfig.getSonatypeWork());
    }
    catch (Exception e) {
      throw new IllegalStateException("Failed to get encryption key", e);
    }
  }
}
