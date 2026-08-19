/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

/**
 * Summary information about a repository
 *
 * @since 1.144
 */
public class RepositorySummary
{
  public String id;

  public String name;

  // For json deserialization
  public RepositorySummary() {
  }

  public RepositorySummary(String id, String name) {
    this.id = id;
    this.name = name;
  }
}
