/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ide;

/**
 * @since 1.9.2
 */
public interface AssetPathAdjuster
{
  /**
   * Conditionally change the path based on it and the user agent requesting it.
   * @param path
   * @param userAgent
   * @return potentially adjusted path, or the original path
   */
  String adjustPath(String path, String userAgent);
}
