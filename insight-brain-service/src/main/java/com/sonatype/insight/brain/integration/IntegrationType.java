/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import java.util.Locale;

/**
 * @since 1.69
 */
public enum IntegrationType
{
  CI,
  CLI,
  RM;

  @Override
  public String toString() {
    return name().toLowerCase(Locale.ENGLISH);
  }

  public static IntegrationType fromString(String name) {
    if (name == null) {
      return null;
    }

    return valueOf(name.toUpperCase(Locale.ENGLISH));
  }
}
