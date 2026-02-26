/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.metrics;

public enum ScmCommentOperation
{
  CREATE("create"),
  UPDATE("update");

  private final String value;

  ScmCommentOperation(final String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }
}
