/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration;

/**
 *
 * @since 1.172
 */
public enum CallFlowAlgorithm
{
  CLASS_HIERARCHY_ANALYSIS("CHA");

  private final String name;

  CallFlowAlgorithm(final String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
