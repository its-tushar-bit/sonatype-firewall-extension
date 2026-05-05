/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.repository.ConfigureRepositoriesRequest;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Before;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class ArtifactoryRepositoryServiceAuthzTest
    extends AbstractRepositoryServiceAuthzTest
{
  @Inject
  TestProductLicenseManager licenseManager;

  @Inject
  private ArtifactoryRepositoryService repositoryService;

  @Override
  protected AbstractRepositoryService getRepositoryService() {
    return repositoryService;
  }

  @Before
  public void init() {
    licenseManager.setFeatures(LicensedFeature.FIREWALL_FOR_ARTIFACTORY);
  }

  @Override
  protected ConfigureRepositoriesRequest createConfigureRepositoriesRequest() {
    return new ConfigureRepositoriesRequest("JFrog Artifactory", "7.37.15", "http://localhost:8081", null /*
                                                                                                           * repositories
                                                                                                           */);
  }
}
