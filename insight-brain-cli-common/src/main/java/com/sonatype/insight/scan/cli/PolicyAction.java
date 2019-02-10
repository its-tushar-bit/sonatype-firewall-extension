/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

public enum PolicyAction
{
  NONE, WARN, FAIL;

  @Override
  public String toString() {
    switch (this) {
      case NONE:
        return "None";
      case WARN:
        return "Warning";
      case FAIL:
        return "Failure";
      default:
        return super.toString();
    }
  }

  public PolicyAction combine(PolicyAction that) {
    return (this.ordinal() < that.ordinal()) ? that : this;
  }
}
