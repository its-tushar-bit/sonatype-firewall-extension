/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.security.Provider;
import java.security.Security;
import java.util.Arrays;

import com.sonatype.insight.brain.security.FIPSModeDetector;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SecurityProviderBootstrapTest
{
  private Provider[] originalProviders;

  @Before
  public void setUp() {
    originalProviders = Security.getProviders();
  }

  @After
  public void tearDown() {
    for (Provider provider : Security.getProviders()) {
      Security.removeProvider(provider.getName());
    }
    for (Provider provider : originalProviders) {
      Security.addProvider(provider);
    }
  }

  @Test
  public void shouldMoveBouncyCastleProviderToLowestPreferenceWhenFipsDisabled() {
    Assume.assumeFalse(FIPSModeDetector.isEnabled());

    Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
    Security.insertProviderAt(new BouncyCastleProvider(), 1);

    SecurityProviderBootstrap.ensureBouncyCastleProviderIsLowestPreference();

    Provider[] providers = Security.getProviders();
    assertThat(providers[providers.length - 1].getName()).isEqualTo(BouncyCastleProvider.PROVIDER_NAME);
    assertThat(Arrays.stream(providers)
        .filter(provider -> BouncyCastleProvider.PROVIDER_NAME.equals(provider.getName())))
            .hasSize(1);
  }
}
