/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import com.sonatype.insight.scan.model.ItemContentType;

public class ThirdPartyScanContent
{
  private final String path;

  private final ItemContentType itemContentType;

  private final String lastModified;

  private final String hash;

  private final String content;

  public ThirdPartyScanContent(
      final String path,
      final ItemContentType itemContentType,
      final String lastModified,
      final String hash,
      final String content)
  {
    this.path = path;
    this.itemContentType = itemContentType;
    this.lastModified = lastModified;
    this.hash = hash;
    this.content = content;
  }

  public String getPath() {
    return path;
  }

  public ItemContentType getItemContentType() {
    return itemContentType;
  }

  public String getLastModified() {
    return lastModified;
  }

  public String getHash() {
    return hash;
  }

  public String getContent() {
    return content;
  }
}
