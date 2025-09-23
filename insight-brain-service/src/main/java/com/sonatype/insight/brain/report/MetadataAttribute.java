/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

public enum MetadataAttribute
{
  LAST_MODIFIED_EPOCH_TIME("lastModifiedTime"),
  SIZE_IN_BYTES("size");

  private final String fileAttributeName;

  MetadataAttribute(final String fileAttributeName) {
    this.fileAttributeName = fileAttributeName;
  }

  public String getFileAttributeName() {
    return fileAttributeName;
  }
}
