/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

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
  protected HttpRequest summaryRequest() {
    return restRequest().path(ArtifactoryRepositoryResource.SUMMARY_PATH);
  }

  @Override
  protected HttpRequest quarantineRequest() {
    return restRequest().path(ArtifactoryRepositoryResource.QUARANTINE_PATH);
  }

  @Override
  protected HttpRequest enableRequest() {
    return restRequest().path(ArtifactoryRepositoryResource.ENABLE_PATH);
  }

  @Override
  protected HttpRequest quarantinedComponentReportUrlRequest() {
    return restRequest().path(ArtifactoryRepositoryResource.QUARANTINED_COMPONENT_REPORT_URL_PATH);
  }
}
