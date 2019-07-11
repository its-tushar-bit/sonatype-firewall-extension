/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import javax.inject.Inject;

public class ArtifactoryRepositoryServiceTest
    extends AbstractRepositoryServiceTest
{
  @Inject
  private ArtifactoryRepositoryService repositoryService;

  @Override
  protected AbstractRepositoryService getRepositoryService() {
    return repositoryService;
  }
}
