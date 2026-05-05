/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.util.Collections;

import com.sonatype.clm.dto.model.repository.ConfigureRepositoriesRequest;
import com.sonatype.clm.dto.model.repository.RepositoryDTO;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Before;

public class ArtifactoryRepositoryResourceTest
    extends AbstractRepositoryResourceTest
{
  @Before
  public void init() {
    getTestProductLicenseManager().setFeatures(LicensedFeature.FIREWALL_FOR_ARTIFACTORY);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(ArtifactoryRepositoryResource.RESOURCE_PATH);
  }

  @Override
  protected String getUserAgent() {
    return "Firewall_For_Jfrog_Artifactory/2.3.1 (; Linux; 5.10.109-104.500.amzn2.x86_64; amd64; 11.0.13; Jfrog"
        + " Artifactory 7.37.15)";
  }

  @Override
  protected ConfigureRepositoriesRequest createConfigureRepositoriesRequest(RepositoryDTO repositoryDTO) {
    return new ConfigureRepositoriesRequest("JFrog Artifactory", "7.37.15", "http://localhost:8081",
        Collections.singletonList(repositoryDTO));
  }
}
