/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.util.Collections;
import java.util.HashMap;

import com.sonatype.clm.dto.model.component.FirewallIgnorePatterns;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.features.Feature;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ArtifactoryRepositoryResourceTest
    extends AbstractRepositoryResourceTest
{
  @Before
  public void init() {
    getTestProductLicenseManager().setFeatures(Feature.FIREWALL_FOR_ARTIFACTORY);
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
  
  @Test
  public void testGetIgnorePatterns() throws Exception {
    // Prepare request and mock the HDS request
    FirewallIgnorePatterns hdsResult = new FirewallIgnorePatterns();
    hdsResult.regexpsByRepositoryFormat = new HashMap<>();
    hdsResult.regexpsByRepositoryFormat.put("foo", Collections.singletonList("bar"));
    setHdsResponseForURI(AbstractRepositoryService.HDS_IGNORE_PATTERNS_PATH, hdsResult, 200);
  
    HttpResponse response = restRequest().path(ArtifactoryRepositoryResource.IGNORE_PATTERNS_PATH).get();
    assertResponseStatus(200, response);
    assertThat(response.getBody(FirewallIgnorePatterns.class).regexpsByRepositoryFormat)
        .isEqualTo(hdsResult.regexpsByRepositoryFormat);
  }
}
