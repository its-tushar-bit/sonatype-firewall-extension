/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.component;

public class ProprietaryComponentName
{
  private final String proprietaryNamePattern;

  private final String repositoryId;

  public ProprietaryComponentName(String proprietaryNamePattern, String repositoryId) {
    this.proprietaryNamePattern = proprietaryNamePattern;
    this.repositoryId = repositoryId;
  }

  public String getProprietaryNamePattern() {
    return proprietaryNamePattern;
  }

  public String getRepositoryId() {
    return repositoryId;
  }

  @Override
  public String toString() {
    return proprietaryNamePattern + " - " + repositoryId;
  }
}
