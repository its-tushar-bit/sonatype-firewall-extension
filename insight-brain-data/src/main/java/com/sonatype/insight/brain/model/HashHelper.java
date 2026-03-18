/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

public class HashHelper
{
  public static final int MAX_LENGTH = 20;

  /**
   * We use only the first 10 bytes of the hashes, so we have to truncate to the first 20 chars in the string
   * representation of a hash.
   *
   * @param hash a hash that may need truncation
   * @return a new hash truncated to the correct length, or the original hash if no truncation is needed
   */
  public static String truncateHash(final String hash) {
    if (hash != null && hash.length() > MAX_LENGTH) {
      return hash.substring(0, MAX_LENGTH);
    }
    else {
      return hash;
    }
  }
}
