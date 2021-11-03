/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.ws.rs.core.Response.Status;

import com.sonatype.insight.brain.dataaccess.configuration.RepositoryClientConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.RepositoryClientConfiguration;
import com.sonatype.insight.brain.repository.RepositoryAllVersionsResponse;
import com.sonatype.insight.brain.repository.RepositoryClient;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.error.exception.NotAuthenticatedException;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NexusRepository3ClientTest
{
  @Rule
  public WireMockRule nxrm3MockSever = new WireMockRule(wireMockConfig().dynamicPort());

  @Mock
  private RepositoryClientConfigurationDAO clientConfigurationDAO;

  private RepositoryClientFactory factory;

  private String baseUrl;

  @Before
  public void before() {
    when(clientConfigurationDAO.get()).thenReturn(newRepositoryClientConfiguration());
    factory = new RepositoryClientFactory(clientConfigurationDAO);
    baseUrl = nxrm3MockSever.baseUrl();
  }

  private RepositoryClientConfiguration newRepositoryClientConfiguration() {
    RepositoryClientConfiguration config = new RepositoryClientConfiguration();
    config.setConnectionTimeout(5);
    config.setSocketTimeout(10);
    return config;
  }

  @Test
  public void testGetAllVersions_Maven_NoPaging() throws Exception {
    nxrm3MockSever.stubFor(get(urlPathMatching("/service/rest/v1/search/assets"))
        .withBasicAuth("user", "pass")
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(getCannedResponse("maven_nopaging.json"))));
    RepositoryClient client = factory.create().forNexus3(baseUrl, "user", "pass");
    Map<String, String> params =
        ImmutableMap.of("group", "g1", "name", "n1", "maven.extension", "jar", "maven.classifier", "");

    RepositoryAllVersionsResponse response = client.getAllVersions(params);
    assertThat(response.getVersions()).hasSize(3);
    assertThat(response.getVersions().get(0).get("version")).isEqualTo("1.1.0");
    assertThat(response.getVersions().get(1).get("version")).isEqualTo("1.2.0");
    assertThat(response.getVersions().get(2).get("version")).isEqualTo("1.3.0");
  }

  @Test
  public void testGetAllVersions_Maven_WithPaging() throws Exception {
    nxrm3MockSever.stubFor(get(urlPathMatching("/service/rest/v1/search/assets"))
        .withBasicAuth("user", "pass")
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(getCannedResponse("maven_paging_1.json"))));

    nxrm3MockSever.stubFor(get(urlPathMatching("/service/rest/v1/search/assets"))
        .withQueryParam("continuationToken", new EqualToPattern("page2"))
        .withBasicAuth("user", "pass")
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(getCannedResponse("maven_paging_2.json"))));

    RepositoryClient client = factory.create().forNexus3(baseUrl, "user", "pass");
    Map<String, String> params =
        ImmutableMap.of("group", "g1", "name", "n1", "maven.extension", "jar", "maven.classifier", "");

    RepositoryAllVersionsResponse response = client.getAllVersions(params);
    assertThat(response.getVersions()).hasSize(4);
    assertThat(response.getVersions().get(0).get("version")).isEqualTo("1.0.0");
    assertThat(response.getVersions().get(1).get("version")).isEqualTo("1.1.0");
    assertThat(response.getVersions().get(2).get("version")).isEqualTo("1.2.0");
    assertThat(response.getVersions().get(3).get("version")).isEqualTo("1.3.0");
  }

  @Test
  public void testGetAllVersions_Npm_WithPaging() throws Exception {
    nxrm3MockSever.stubFor(get(urlPathMatching("/service/rest/v1/search/assets"))
        .withBasicAuth("user", "pass")
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(getCannedResponse("npm_page_1.json"))));

    nxrm3MockSever.stubFor(get(urlPathMatching("/service/rest/v1/search/assets"))
        .withQueryParam("continuationToken", new EqualToPattern("page2"))
        .withBasicAuth("user", "pass")
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(getCannedResponse("npm_page_2.json"))));

    RepositoryClient client = factory.create().forNexus3(baseUrl, "user", "pass");
    Map<String, String> params = ImmutableMap.of("name", "p1");

    RepositoryAllVersionsResponse response = client.getAllVersions(params);
    assertThat(response.getVersions()).hasSize(3);
    assertThat(response.getVersions().get(0).get("version")).isEqualTo("1.0.0");
    assertThat(response.getVersions().get(1).get("version")).isEqualTo("1.1.0");
    assertThat(response.getVersions().get(2).get("version")).isEqualTo("1.2.0");
  }

  @Test
  public void testGetAllVersions_Npm_NoPaging() throws Exception {
    nxrm3MockSever.stubFor(get(urlPathMatching("/service/rest/v1/search/assets"))
        .withBasicAuth("user", "pass")
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(getCannedResponse("npm_nopaging.json"))));
    RepositoryClient client = factory.create().forNexus3(baseUrl, "user", "pass");
    Map<String, String> params = ImmutableMap.of("name", "p1");

    RepositoryAllVersionsResponse response = client.getAllVersions(params);
    assertThat(response.getVersions()).hasSize(2);
    assertThat(response.getVersions().get(0).get("version")).isEqualTo("1.1.0");
    assertThat(response.getVersions().get(1).get("version")).isEqualTo("1.2.0");
  }

  @Test
  public void testGetAllVersions_HandleInvalidCoordinates() throws Exception {
    nxrm3MockSever.stubFor(get(urlPathMatching("/service/rest/v1/search/assets"))
        .withBasicAuth("user", "pass")
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(getCannedResponse("maven_invalid_coordinates.json"))));
    RepositoryClient client = factory.create().forNexus3(baseUrl, "user", "pass");
    Map<String, String> params =
        ImmutableMap.of("group", "g1", "name", "n1", "maven.extension", "jar", "maven.classifier", "");

    RepositoryAllVersionsResponse response = client.getAllVersions(params);
    assertThat(response.getVersions()).hasSize(1);
    assertThat(response.getVersions().get(0).get("version")).isEqualTo("1.1.0");
  }

  @Test
  public void testGetAllVersions_InvalidCredentials() {
    nxrm3MockSever.stubFor(get(urlPathMatching("/service/rest/v1/search/assets"))
        .willReturn(aResponse().withStatus(401)));

    RepositoryClient client = factory.create().forNexus3(baseUrl, "BLAH", "BLAH");
    Map<String, String> params = ImmutableMap.of("name", "p1");

    assertThatExceptionOfType(NotAuthenticatedException.class).isThrownBy(() -> client.getAllVersions(params))
        .withMessage("could not retrieve search component response from repository manager: Unauthorized");
  }

  @Test
  public void testGetAllVersions_ServerError() {
    nxrm3MockSever.stubFor(get(urlPathMatching("/service/rest/v1/search/assets"))
        .withBasicAuth("user", "pass")
        .willReturn(aResponse().withStatus(502)));

    RepositoryClient client = factory.create().forNexus3(baseUrl, "user", "pass");
    Map<String, String> params = ImmutableMap.of("name", "p1");

    assertThatExceptionOfType(BadGatewayException.class).isThrownBy(() -> client.getAllVersions(params))
        .withMessage("could not retrieve search component response from repository manager: Bad Gateway");
  }

  @Test
  public void testGetAllVersions_InvalidResponseContent() throws Exception {
    nxrm3MockSever.stubFor(get(urlPathMatching("/service/rest/v1/search/assets"))
        .withBasicAuth("user", "pass")
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(getCannedResponse("maven_invalid_content.json"))));
    RepositoryClient client = factory.create().forNexus3(baseUrl, "user", "pass");
    Map<String, String> params =
        ImmutableMap.of("group", "g1", "name", "n1", "maven.extension", "jar", "maven.classifier", "");

    RepositoryAllVersionsResponse response = client.getAllVersions(params);
    assertThat(response.getVersions()).isEmpty();
  }

  @Test
  public void testGetServerStatus_OK() throws Exception {
    nxrm3MockSever.stubFor(get(urlPathMatching("/service/rest/v1/status"))
        .withBasicAuth("user", "pass")
        .willReturn(aResponse().withStatus(200)));

    RepositoryClient client = factory.create().forNexus3(baseUrl, "user", "pass");
    assertThat(client.getServerStatus()).isEqualTo(Status.OK);
  }

  @Test
  public void testGetServerStatus_Unauthorized() throws Exception {
    nxrm3MockSever.stubFor(get(urlPathMatching("/service/rest/v1/status"))
        .willReturn(aResponse().withStatus(401)));

    RepositoryClient client = factory.create().forNexus3(baseUrl, "BLAH", "BLAH");
    assertThat(client.getServerStatus()).isEqualTo(Status.UNAUTHORIZED);
  }

  private String getCannedResponse(final String path) throws IOException {
    return IOUtils.toString(getClass().getResource("/NexusRepository3ClientTest/" + path),
        StandardCharsets.UTF_8);
  }
}
