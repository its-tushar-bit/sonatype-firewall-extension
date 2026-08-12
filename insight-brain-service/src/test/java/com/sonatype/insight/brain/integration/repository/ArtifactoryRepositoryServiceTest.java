/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.repository.ConfigureRepositoriesRequest;
import com.sonatype.clm.dto.model.repository.RepositoryDTO;

public class ArtifactoryRepositoryServiceTest
    extends AbstractRepositoryServiceTest
{
  @Inject
  private ArtifactoryRepositoryService repositoryService;

  @Override
  protected AbstractRepositoryService getRepositoryService() {
    return repositoryService;
  }

  @Override
  protected String getUserAgent() {
    return "Firewall_For_Jfrog_Artifactory/2.3.1 (; Linux; 5.10.109-104.500.amzn2.x86_64; amd64; 11.0.13; Jfrog"
        + " Artifactory 7.37.15)";
  }

  @Override
  protected ConfigureRepositoriesRequest createConfigureRepositoriesRequest(List<RepositoryDTO> repositoryDTOs) {
    return new ConfigureRepositoriesRequest("JFrog Artifactory", "7.37.15", "http://localhost:8081", repositoryDTOs);
  }
}
