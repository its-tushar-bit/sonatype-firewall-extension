/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.util.Locale;

/**
 * @since 1.35
 */
public enum SupportFileType
{
  LOG,
  CLUSTER_LOG,
  INFO,
  CONFIG,
  DB;

  private final String dirName = name().toLowerCase(Locale.ENGLISH);

  public String getDirName() {
    return dirName;
  }
}
