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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RepositoryResourceTest
    extends AbstractRepositoryResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(RepositoryResource.RESOURCE_PATH);
  }

  @Override
  protected HttpRequest summaryRequest() {
    return restRequest().path(RepositoryResource.SUMMARY_PATH);
  }

  @Override
  protected HttpRequest quarantineRequest() {
    return restRequest().path(RepositoryResource.QUARANTINE_PATH);
  }

  @Override
  protected HttpRequest enableRequest() {
    return restRequest().path(RepositoryResource.ENABLE_PATH);
  }

  @Test
  public void testGetIgnorePatterns() throws Exception {
    // Prepare request and mock the HDS request
    FirewallIgnorePatterns hdsResult = new FirewallIgnorePatterns();
    hdsResult.regexpsByRepositoryFormat = new HashMap<>();
    hdsResult.regexpsByRepositoryFormat.put("foo", Collections.singletonList("bar"));
    hdsRespondWith(hdsResult).atUri(AbstractRepositoryService.HDS_IGNORE_PATTERNS_PATH);

    HttpResponse response = restRequest().path(RepositoryResource.IGNORE_PATTERNS_PATH).get();
    assertResponseStatus(200, response);
    assertThat(response.getBody(FirewallIgnorePatterns.class).regexpsByRepositoryFormat)
        .isEqualTo(hdsResult.regexpsByRepositoryFormat);
  }
}
