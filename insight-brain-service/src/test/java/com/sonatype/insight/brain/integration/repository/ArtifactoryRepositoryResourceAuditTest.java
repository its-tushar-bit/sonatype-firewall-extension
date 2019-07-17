/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

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
  protected String getEnablePath() {
    return ArtifactoryRepositoryResource.ENABLE_PATH;
  }

  @Override
  protected String getResourcePath() {
    return ArtifactoryRepositoryResource.RESOURCE_PATH;
  }

  @Override
  protected String getEvaluateComponentsPath() {
    return ArtifactoryRepositoryResource.EVALUATE_COMPONENTS_PATH;
  }

  @Override
  protected String getQuarantinePath() {
    return ArtifactoryRepositoryResource.QUARANTINE_PATH;
  }

  @Override
  protected String getComponentsPath() {
    return ArtifactoryRepositoryResource.COMPONENTS_PATH;
  }

  @Override
  protected String getEvaluateComponentWithQuarantinePath() {
    return ArtifactoryRepositoryResource.EVALUATE_COMPONENT_WITH_QUARANTINE_PATH;
  }
}
