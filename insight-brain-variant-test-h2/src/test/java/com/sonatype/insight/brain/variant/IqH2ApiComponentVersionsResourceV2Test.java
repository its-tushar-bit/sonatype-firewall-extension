/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiComponentVersionsServiceV2;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2ApiComponentVersionsResourceV2Test
{
  private IqTestContext ctx;

  @Test
  void testGetComponentVersions() throws Exception {
    List<String> hdsResult = Arrays.asList("v1", "v2", "v3", "v4");
    ctx.hdsRespondWith(hdsResult).atUri(ApiComponentVersionsServiceV2.HDS_COMPONENT_VERSIONS_LIST_PATH);

    ApiComponentIdentifierDTOV2 request = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"));

    HttpResponse response =
        ctx.restRequest().path(PublicApiPaths.COMPONENT_VERSIONS_PATH_V2).body(request).post();
    ctx.assertResponseStatus(200, response);

    List<String> result = response.getBodyList();
    assertThat(result).containsExactly("v1", "v2", "v3", "v4");
  }

  @Test
  void testGetComponentVersions_Purl() throws Exception {
    List<String> hdsResult = Arrays.asList("v1", "v2", "v3", "v4");
    ctx.hdsRespondWith(hdsResult).atUri(ApiComponentVersionsServiceV2.HDS_COMPONENT_VERSIONS_LIST_PATH);

    String packageUrl = "pkg:maven/g1/a1@v1?classifier=c1&type=e1";
    Map<String, String> request = ImmutableMap.of("packageUrl", packageUrl);

    HttpResponse response =
        ctx.restRequest().path(PublicApiPaths.COMPONENT_VERSIONS_PATH_V2).body(request).post();
    ctx.assertResponseStatus(200, response);

    List<String> result = response.getBodyList();
    assertThat(result).containsExactly("v1", "v2", "v3", "v4");
  }
}
