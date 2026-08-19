/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import org.apache.commons.lang3.StringUtils;

/**
 * Where a scan or waiver request originated. Persisted as a string, so constant names are part of
 * the database contract and are also the accepted values of the {@code X-Scan-Source} header.
 */
public enum ScanSource
{
  FIREWALL_PROXY,
  BROWSER_EXTENSION,
  IDE,
  CI;

  /**
   * The value used when nothing more specific is known, including for legacy rows stored before this
   * column existed.
   */
  public static final ScanSource DEFAULT = FIREWALL_PROXY;

  /**
   * Parses an {@code X-Scan-Source} header value. Total: blank, absent, and unrecognized input all
   * yield {@link #DEFAULT} rather than throwing, because an unknown header from a future client must
   * not fail an otherwise-valid request.
   */
  public static ScanSource fromHeader(String value) {
    if (StringUtils.isBlank(value)) {
      return DEFAULT;
    }
    String trimmed = value.trim();
    for (ScanSource scanSource : values()) {
      if (scanSource.name().equalsIgnoreCase(trimmed)) {
        return scanSource;
      }
    }
    return DEFAULT;
  }
}
