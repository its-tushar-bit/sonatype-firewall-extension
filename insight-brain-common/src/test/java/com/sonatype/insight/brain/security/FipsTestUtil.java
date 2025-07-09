/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.net.URL;
import java.net.URLClassLoader;
import java.security.Provider;
import java.security.Security;

import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;

public class FipsTestUtil
{
  private FipsTestUtil() {
    // no op
  }

  public static void insertBouncyCastleFipsProvider() {
    if (Security.getProvider(BouncyCastleFipsProvider.PROVIDER_NAME) == null) {
      Security.addProvider(createFipsProvider());
    }
    if (Security.getProvider(BouncyCastleJsseProvider.PROVIDER_NAME) == null) {
      Security.addProvider(new BouncyCastleJsseProvider());
    }
  }

  public static void removeBouncyCastleFipsProvider() {
    Security.removeProvider(BouncyCastleFipsProvider.PROVIDER_NAME);
    Security.removeProvider(BouncyCastleJsseProvider.PROVIDER_NAME);
  }

  private static Provider createFipsProvider() {
    try {
      URL fipsJarUrl = BouncyCastleFipsProvider.class.getProtectionDomain().getCodeSource().getLocation();
      URLClassLoader fipsClassLoader = new URLClassLoader(new URL[]{fipsJarUrl}, null);

      Class<?> providerClass = fipsClassLoader.loadClass(
          "org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider");
      return (Provider) providerClass.getConstructor().newInstance();
    }
    catch (Exception e) {
      throw new IllegalStateException("Failed to create FIPS provider", e);
    }
  }
}
