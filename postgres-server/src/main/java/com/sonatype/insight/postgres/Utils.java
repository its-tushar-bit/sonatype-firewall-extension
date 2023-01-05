/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.postgres;

import org.junit.AssumptionViolatedException;

public final class Utils
{
  private Utils() {
    throw new UnsupportedOperationException();
  }

  public static void assumeSupported() {
    if (Boolean.getBoolean("docker.optional")) {
      throw new AssumptionViolatedException("Docker unavailable.");
    }
  }

  public static String applyRegistry(String image) {
    String registry = System.getProperty("docker.registry", "");
    return (registry.isEmpty() ? "" : registry + '/') + image;
  }
}
