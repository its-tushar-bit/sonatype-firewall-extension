/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

public class ApplicationIdUtils
{
  public static String normalizeApplicationPublicId(String applicationPublicId) {
    // [^\\p{L}0-9._-] matches any character that is NOT:
    // \\p{L} (any Unicode letter)
    // 0-9 (digits)
    // . (dot)
    // _ (underscore)
    // - (dash)
    return applicationPublicId.replaceAll("[^\\p{L}0-9._-]", "-");
  }
}
