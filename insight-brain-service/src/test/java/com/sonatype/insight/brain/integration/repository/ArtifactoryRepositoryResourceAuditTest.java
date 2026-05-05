/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.util.Collections;

import com.sonatype.clm.dto.model.repository.ConfigureRepositoriesRequest;
import com.sonatype.clm.dto.model.repository.RepositoryDTO;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Before;

public class ArtifactoryRepositoryResourceAuditTest
    extends AbstractRepositoryResourceAuditTest
{
  @Before
  public void init() {
    getTestProductLicenseManager().setFeatures(LicensedFeature.FIREWALL_FOR_ARTIFACTORY);
  }

  @Override
  protected String getResourcePath() {
    return ArtifactoryRepositoryResource.RESOURCE_PATH;
  }

  @Override
  protected ConfigureRepositoriesRequest createConfigureRepositoriesRequest(RepositoryDTO repositoryDTO) {
    return new ConfigureRepositoriesRequest("JFrog Artifactory", "7.37.15", "http://localhost:8081",
        Collections.singletonList(repositoryDTO));
  }
}
