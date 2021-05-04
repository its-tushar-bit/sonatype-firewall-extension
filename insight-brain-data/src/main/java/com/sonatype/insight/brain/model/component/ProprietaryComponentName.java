/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.component;

public class ProprietaryComponentName
{
  private final String proprietaryNamePattern;

  private final String repositoryManagerInstanceId;

  private final String repositoryPublicId;

  public ProprietaryComponentName(
      String proprietaryNamePattern,
      String repositoryManagerInstanceId,
      String repositoryPublicId)
  {
    this.proprietaryNamePattern = proprietaryNamePattern;
    this.repositoryManagerInstanceId = repositoryManagerInstanceId;
    this.repositoryPublicId = repositoryPublicId;
  }

  public String getProprietaryNamePattern() {
    return proprietaryNamePattern;
  }

  public String getRepositoryManagerInstanceId() {
    return repositoryManagerInstanceId;
  }

  public String getRepositoryPublicId() {
    return repositoryPublicId;
  }

  @Override
  public String toString() {
    return proprietaryNamePattern + " - " + repositoryManagerInstanceId + ":" + repositoryPublicId;
  }
}
