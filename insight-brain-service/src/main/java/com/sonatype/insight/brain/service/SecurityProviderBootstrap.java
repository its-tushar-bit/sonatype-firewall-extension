/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.net.URL;
import java.net.URLClassLoader;
import java.security.Provider;
import java.security.Security;

import com.sonatype.insight.brain.security.FIPSModeDetector;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import static com.sonatype.insight.brain.security.FIPSProviderFactory.createFipsProvider;

public final class SecurityProviderBootstrap
{
  private SecurityProviderBootstrap() {
    // utility class
  }

  public static void ensureBouncyCastleProviderIsLowestPreference() {
    if (FIPSModeDetector.isEnabled()) {
      Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
      loadFipsProvider();
      return;
    }

    Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
    loadNonFipsProvider();
  }

  private static Provider createNonFipsProvider() {
    try {
      URL bouncyCastleJarUrl = BouncyCastleProvider.class.getProtectionDomain().getCodeSource().getLocation();
      URLClassLoader bouncyCastleClassLoader = new URLClassLoader(new URL[]{bouncyCastleJarUrl}, null);

      Class<?> providerClass = bouncyCastleClassLoader.loadClass("org.bouncycastle.jce.provider.BouncyCastleProvider");
      return (Provider) providerClass.getConstructor().newInstance();
    }
    catch (Exception e) {
      throw new IllegalStateException("Failed to create non-FIPS provider", e);
    }
  }

  private static void loadNonFipsProvider() {
    Security.addProvider(createNonFipsProvider());
  }

  private static void loadFipsProvider() {
    Security.addProvider(createFipsProvider());
  }
}
