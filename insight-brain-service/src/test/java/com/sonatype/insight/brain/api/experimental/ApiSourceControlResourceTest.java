/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.util.HashMap;
import java.util.Map;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.experimental.dto.ApiOwnerUserRateLimitsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiOwnerDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.github.dto.GithubUser;

import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class ApiSourceControlResourceTest
    extends AbstractResourceTest
{
  @Rule
  public WireMockRule mockScmServer = new WireMockRule(wireMockConfig().dynamicPort());

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.SOURCE_CONTROL_PATH_EXPERIMENTAL_PATH).auth();
  }

  @Before
  public void before() {
    Map<String, Object> githubRateLimitsResponse = new HashMap<>();
    Map<String, Object> githubRateLimitResponse = new HashMap<>();
    githubRateLimitResponse.put("remaining", 4);
    githubRateLimitResponse.put("limit", 10);
    githubRateLimitResponse.put("reset", 4444L);
    githubRateLimitResponse.put("used", 6);
    Map<String, Map<String, Object>> githubRateLimitResponseMap = new HashMap<>();
    githubRateLimitResponseMap.put("core", githubRateLimitResponse);
    githubRateLimitsResponse.put("resources", githubRateLimitResponseMap);
    mockScmServer.stubFor(get("/api/v3/rate_limit").withHeader("Authorization", matching("token token"))
        .willReturn(aResponse().withStatus(200).withBody(JsonUtils.format(githubRateLimitsResponse))));
    GithubUser githubUser = new GithubUser();
    githubUser.setGlobalId("userId");
    mockScmServer.stubFor(get("/api/v3/user").withHeader("Authorization", matching("token token"))
        .willReturn(aResponse().withStatus(200).withBody(JsonUtils.format(githubUser))));
  }

  @Test
  public void testGetRateLimits() throws Exception {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    tempEntity.newSourceControl(application.getId(), mockScmServer.baseUrl() + "/orgName/repoName",
        new DefaultPlexusCipher().encrypt("token", "CMMDwoV"), SourceControlProvider.GITHUB);

    HttpResponse response = restRequest().path(ApiSourceControlResource.RATE_LIMITS_PATH)
        .parameter(OwnerType.ORGANIZATION, application.getOrganizationId()).get();

    assertResponseStatus(200, response);
    ApiOwnerUserRateLimitsDTO result = response.getBody(ApiOwnerUserRateLimitsDTO.class);
    assertThat(result).isNotNull();
    assertThat(result.ownerType).isEqualTo(OwnerType.ORGANIZATION.toString());
    assertThat(result.ownerId).isEqualTo(application.getOrganizationId());
    assertThat(result.ownerPublicId).isEqualTo(application.getOrganizationId());
    assertThat(result.ownerName).isEqualTo(organization.getName());
    assertThat(result.userRateLimits).hasSize(1);
    assertThat(result.userRateLimits.get(0).user).isEqualTo("userId");
    assertThat(result.userRateLimits.get(0).provider).isEqualTo(SourceControlProvider.GITHUB);
    assertThat(result.userRateLimits.get(0).definingOwners).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(ApiOwnerDTO.fromOwner(application));
    assertThat(result.userRateLimits.get(0).associatedApplications).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(ApiOwnerDTO.fromOwner(application));
    assertThat(result.userRateLimits.get(0).rateLimits).hasSize(1);
    assertThat(result.userRateLimits.get(0).rateLimits.get(0).category).isEqualTo("core");
    assertThat(result.userRateLimits.get(0).rateLimits.get(0).remaining).isEqualTo(4);
    assertThat(result.userRateLimits.get(0).rateLimits.get(0).limit).isEqualTo(10);
    assertThat(result.userRateLimits.get(0).rateLimits.get(0).resetEpochTime).isEqualTo(4444);
  }
}
