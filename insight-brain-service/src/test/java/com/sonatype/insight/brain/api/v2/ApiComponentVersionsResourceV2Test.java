/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiComponentVersionsServiceV2;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiComponentVersionsResourceV2Test
    extends AbstractResourceTest
{
  @Test
  public void testGetComponentVersions() throws Exception {
    List<String> hdsResult = Arrays.asList("v1", "v2", "v3", "v4");
    hdsRespondWith(hdsResult).atUri(ApiComponentVersionsServiceV2.HDS_COMPONENT_VERSIONS_LIST_PATH);

    ApiComponentIdentifierDTOV2 request = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"));

    HttpResponse response = restRequest().path(PublicApiPaths.COMPONENT_VERSIONS_PATH_V2).body(request).post();
    assertResponseStatus(200, response);

    List<String> result = response.getBodyList();
    assertThat(result).containsExactly("v1", "v2", "v3", "v4");
  }

  @Test
  public void testGetComponentVersions_Purl() throws Exception {
    List<String> hdsResult = Arrays.asList("v1", "v2", "v3", "v4");
    hdsRespondWith(hdsResult).atUri(ApiComponentVersionsServiceV2.HDS_COMPONENT_VERSIONS_LIST_PATH);

    String packageUrl = "pkg:maven/g1/a1@v1?classifier=c1&type=e1";
    Map<String, String> request = ImmutableMap.of("packageUrl", packageUrl);

    HttpResponse response = restRequest().path(PublicApiPaths.COMPONENT_VERSIONS_PATH_V2).body(request).post();
    assertResponseStatus(200, response);

    List<String> result = response.getBodyList();
    assertThat(result).containsExactly("v1", "v2", "v3", "v4");
  }
}
