/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import org.apache.commons.codec.digest.DigestUtils;

public class ContentHashUtil
{
  private ContentHashUtil() {
    //Static util class
  }

  public static String getContentHash(String content) {
    return DigestUtils.sha256Hex(content);
  }
}
