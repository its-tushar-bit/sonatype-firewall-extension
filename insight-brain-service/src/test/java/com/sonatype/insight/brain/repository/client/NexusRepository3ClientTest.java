/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import jakarta.ws.rs.core.Response.Status;

import com.sonatype.insight.brain.dataaccess.configuration.RepositoryClientConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.RepositoryClientConfiguration;
import com.sonatype.insight.brain.repository.RepositoryAllVersionsResponse;
import com.sonatype.insight.brain.repository.RepositoryClient;
import com.sonatype.insight.brain.testsupport.wiremock.ReusableWireMockExtension;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.error.exception.NotAuthenticatedException;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class NexusRepository3ClientTest
{
  @RegisterExtension
  static ReusableWireMockExtension nxrm3MockServer = new ReusableWireMockExtension();

  public static final String NXRM_VERSION_HEADER_MOCK_VALUE = "Nexus/3.37.3-02 (PRO)";

  @Mock
  private RepositoryClientConfigurationDAO clientConfigurationDAO;

  private RepositoryClientFactory factory;

  private String baseUrl;

  @BeforeEach
  public void before() {
    when(clientConfigurationDAO.get()).thenReturn(newRepositoryClientConfiguration());
    factory = new RepositoryClientFactory(clientConfigurationDAO);
    baseUrl = nxrm3MockServer.baseUrl();
  }

  private RepositoryClientConfiguration newRepositoryClientConfiguration() {
    RepositoryClientConfiguration config = new RepositoryClientConfiguration();
    config.setConnectionTimeout(5);
    config.setSocketTimeout(10);
    return config;
  }

  @Test
  public void testGetAllVersions_Maven_NoPaging() throws Exception {
    Map<String, String> params =
        ImmutableMap.of("group", "g1", "name", "n1", "maven.extension", "jar", "maven.classifier", "");
    nxrm3MockServer.stubFor(get(urlPathMatching("/service/rest/v1/search/assets"))
        .withBasicAuth("user", "pass")
        .withQueryParams(ImmutableMap.of(
            "group", equalTo("g1"),
            "name", equalTo("n1"),
            "maven.extension", equalTo("jar"),
            "maven.classifier", equalTo("")))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(getCannedResponse("maven_nopaging.json"))));

    RepositoryClient client = factory.create().forNexus3(baseUrl, "user", "pass".toCharArray());
    RepositoryAllVersionsResponse response = client.getAllVersions(params);

    assertThat(response.getComponents()).hasSize(3);
    assertResultComponent(response, 0, "1.1.0", "g1-n1-110-sha1");
    assertResultComponent(response, 1, "1.2.0", "g1-n1-120-sha1");
    assertResultComponent(response, 2, "1.3.0", "g1-n1-130-sha1");
  }

  @Test
  public void testGetAllVersions_Maven_WithPaging() throws Exception {
    nxrm3MockServer.stubFor(get(urlPathMatching("/service/rest/v1/search/assets"))
        .withBasicAuth("user", "pass")
        .withQueryParams(ImmutableMap.of(
            "group", equalTo("g1"),
            "name", equalTo("n1"),
            "maven.extension", equalTo("jar"),
            "maven.classifier", equalTo("")))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(getCannedResponse("maven_paging_1.json"))));

    nxrm3MockServer.stubFor(get(urlPathMatching("/service/rest/v1/search/assets"))
        .withBasicAuth("user", "pass")
        .withQueryParams(ImmutableMap.of(
            "group", equalTo("g1"),
            "name", equalTo("n1"),
            "maven.extension", equalTo("jar"),
            "maven.classifier", equalTo("")))
        .withQueryParam("continuationToken", equalTo("page2"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(getCannedResponse("maven_paging_2.json"))));

    RepositoryClient client = factory.create().forNexus3(baseUrl, "user", "pass".toCharArray());
    Map<String, String> params =
        ImmutableMap.of("group", "g1", "name", "n1", "maven.extension", "jar", "maven.classifier", "");

    RepositoryAllVersionsResponse response = client.getAllVersions(params);
    assertThat(response.getComponents()).hasSize(4);
    assertResultComponent(response, 0, "1.0.0", "g1-n1-100-sha1");
    assertResultComponent(response, 1, "1.1.0", "g1-n1-110-sha1");
    assertResultComponent(response, 2, "1.2.0", "g1-n1-120-sha1");
    assertResultComponent(response, 3, "1.3.0", "g1-n1-130-sha1");
  }

  @Test
  public void testGetAllVersions_Npm_WithPaging() throws Exception {
    nxrm3MockServer.stubFor(get(urlPathMatching("/service/rest/v1/search/assets"))
        .withBasicAuth("user", "pass")
        .withQueryParam("name", equalTo("p1"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(getCannedResponse("npm_page_1.json"))));

    nxrm3MockServer.stubFor(get(urlPathMatching("/service/rest/v1/search/assets"))
        .withBasicAuth("user", "pass")
        .withQueryParam("name", equalTo("p1"))
        .withQueryParam("continuationToken", equalTo("page2"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(getCannedResponse("npm_page_2.json"))));

    RepositoryClient client = factory.create().forNexus3(baseUrl, "user", "pass".toCharArray());
    Map<String, String> params = ImmutableMap.of("name", "p1");

    RepositoryAllVersionsResponse response = client.getAllVersions(params);
    assertThat(response.getComponents()).hasSize(3);
    assertResultComponent(response, 0, "1.0.0", "p1-100-sha1");
    assertResultComponent(response, 1, "1.1.0", "p1-110-sha1");
    assertResultComponent(response, 2, "1.2.0", "p1-120-sha1");
  }

  @Test
  public void testGetAllVersions_Npm_NoPaging() throws Exception {
    nxrm3MockServer.stubFor(get(urlPathMatching("/service/rest/v1/search/assets"))
        .withBasicAuth("user", "pass")
        .withQueryParam("name", equalTo("p1"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(getCannedResponse("npm_nopaging.json"))));
    RepositoryClient client = factory.create().forNexus3(baseUrl, "user", "pass".toCharArray());
    Map<String, String> params = ImmutableMap.of("name", "p1");

    RepositoryAllVersionsResponse response = client.getAllVersions(params);
    assertThat(response.getComponents()).hasSize(2);
    assertResultComponent(response, 0, "1.1.0", "p1-110-sha1");
    assertResultComponent(response, 1, "1.2.0", "p1-120-sha1");
  }

  @Test
  public void testGetAllVersions_HandleInvalidCoordinates() throws Exception {
    nxrm3MockServer.stubFor(get(urlPathMatching("/service/rest/v1/search/assets"))
        .withBasicAuth("user", "pass")
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(getCannedResponse("maven_invalid_coordinates.json"))));
    RepositoryClient client = factory.create().forNexus3(baseUrl, "user", "pass".toCharArray());
    Map<String, String> params =
        ImmutableMap.of("group", "g1", "name", "n1", "maven.extension", "jar", "maven.classifier", "");

    RepositoryAllVersionsResponse response = client.getAllVersions(params);
    assertThat(response.getComponents()).hasSize(1);
    assertResultComponent(response, 0, "1.1.0", "g1-n1-sha1");
  }

  @Test
  public void testGetAllVersions_InvalidCredentials() {
    nxrm3MockServer.stubFor(get(urlPathMatching("/service/rest/v1/search/assets"))
        .willReturn(aResponse().withStatus(401)));

    RepositoryClient client = factory.create().forNexus3(baseUrl, "BLAH", "BLAH".toCharArray());
    Map<String, String> params = ImmutableMap.of("name", "p1");

    assertThatExceptionOfType(NotAuthenticatedException.class).isThrownBy(() -> client.getAllVersions(params))
        .withMessage("could not retrieve search component response from repository manager: Unauthorized");
  }

  @Test
  public void testGetAllVersions_ServerError() {
    nxrm3MockServer.stubFor(get(urlPathMatching("/service/rest/v1/search/assets"))
        .withBasicAuth("user", "pass")
        .willReturn(aResponse().withStatus(502)));

    RepositoryClient client = factory.create().forNexus3(baseUrl, "user", "pass".toCharArray());
    Map<String, String> params = ImmutableMap.of("name", "p1");

    assertThatExceptionOfType(BadGatewayException.class).isThrownBy(() -> client.getAllVersions(params))
        .withMessage("could not retrieve search component response from repository manager: Bad Gateway");
  }

  @Test
  public void testGetAllVersions_InvalidResponseContent() throws Exception {
    nxrm3MockServer.stubFor(get(urlPathMatching("/service/rest/v1/search/assets"))
        .withBasicAuth("user", "pass")
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(getCannedResponse("maven_invalid_content.json"))));
    RepositoryClient client = factory.create().forNexus3(baseUrl, "user", "pass".toCharArray());
    Map<String, String> params =
        ImmutableMap.of("group", "g1", "name", "n1", "maven.extension", "jar", "maven.classifier", "");

    RepositoryAllVersionsResponse response = client.getAllVersions(params);
    assertThat(response.getComponents()).isEmpty();
  }

  @Test
  public void testGetServerStatus_OK() throws Exception {
    nxrm3MockServer.stubFor(get(urlPathMatching("/service/rest/v1/status"))
        .withBasicAuth("user", "pass")
        .willReturn(aResponse().withHeader(NexusRepository3Client.NXRM_VERSION_HEADER_NAME,
            NexusRepository3ClientTest.NXRM_VERSION_HEADER_MOCK_VALUE).withStatus(200)));

    RepositoryClient client = factory.create().forNexus3(baseUrl, "user", "pass".toCharArray());
    assertThat(client.getServerStatus()).isEqualTo(Status.OK);
  }

  @Test
  public void testGetServerStatus_Unauthorized() throws Exception {
    nxrm3MockServer.stubFor(get(urlPathMatching("/service/rest/v1/status"))
        .willReturn(aResponse().withHeader(NexusRepository3Client.NXRM_VERSION_HEADER_NAME,
            NexusRepository3ClientTest.NXRM_VERSION_HEADER_MOCK_VALUE).withStatus(401)));

    RepositoryClient client = factory.create().forNexus3(baseUrl, "BLAH", "BLAH".toCharArray());
    assertThat(client.getServerStatus()).isEqualTo(Status.UNAUTHORIZED);
  }

  @Test
  public void testGetAllVersions_Maven_MissingSha1() throws Exception {
    nxrm3MockServer.stubFor(get(urlPathMatching("/service/rest/v1/search/assets"))
        .withBasicAuth("user", "pass")
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(getCannedResponse("maven_missing_sha1.json"))));
    RepositoryClient client = factory.create().forNexus3(baseUrl, "user", "pass".toCharArray());
    Map<String, String> params =
        ImmutableMap.of("group", "g1", "name", "n1", "maven.extension", "jar", "maven.classifier", "");

    RepositoryAllVersionsResponse response = client.getAllVersions(params);
    assertThat(response.getComponents()).hasSize(2);
    assertResultComponent(response, 0, "1.2.0", null);
    assertResultComponent(response, 1, "1.3.0", null);
  }

  @Test
  public void testGetAllVersions_Npm_MissingSha1() throws Exception {
    nxrm3MockServer.stubFor(get(urlPathMatching("/service/rest/v1/search/assets"))
        .withBasicAuth("user", "pass")
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(getCannedResponse("npm_missing_sha1.json"))));
    RepositoryClient client = factory.create().forNexus3(baseUrl, "user", "pass".toCharArray());
    Map<String, String> params = ImmutableMap.of("name", "p1");

    RepositoryAllVersionsResponse response = client.getAllVersions(params);
    assertThat(response.getComponents()).hasSize(2);
    assertResultComponent(response, 0, "1.1.0", null);
    assertResultComponent(response, 1, "1.2.0", null);
  }

  @Test
  public void testGetAllVersions_OrderedByComparableVersion() throws Exception {
    nxrm3MockServer.stubFor(get(urlPathMatching("/service/rest/v1/search/assets"))
        .withQueryParams(ImmutableMap.of(
            "group", equalTo("g"),
            "name", equalTo("a"),
            "maven.extension", equalTo("jar"),
            "maven.classifier", equalTo("")))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(getCannedResponse("ordering.json"))));
    RepositoryClient client = factory.create().forNexus3(baseUrl, null, null);
    Map<String, String> params =
        ImmutableMap.of("group", "g", "name", "a", "maven.extension", "jar", "maven.classifier", "");

    RepositoryAllVersionsResponse response = client.getAllVersions(params);

    assertThat(response.getComponents()).hasSize(5);
    assertResultComponent(response, 0, "1.1.0-01", "1");
    assertResultComponent(response, 1, "1.2.0-01", "2");
    assertResultComponent(response, 2, "1.2.0-20210730.075537-6", "3");
    assertResultComponent(response, 3, "1.2.0-20210730.075537-7", "4");
    assertResultComponent(response, 4, "1.3.0-01", "5");
  }

  @Test
  public void testGetAllVersions_RemovesDuplicates() throws Exception {
    nxrm3MockServer.stubFor(get(urlPathMatching("/service/rest/v1/search/assets"))
        .withQueryParams(ImmutableMap.of(
            "group", equalTo("g"),
            "name", equalTo("a"),
            "maven.extension", equalTo("jar"),
            "maven.classifier", equalTo("")))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(getCannedResponse("duplicates.json"))));
    RepositoryClient client = factory.create().forNexus3(baseUrl, null, null);
    Map<String, String> params =
        ImmutableMap.of("group", "g", "name", "a", "maven.extension", "jar", "maven.classifier", "");

    RepositoryAllVersionsResponse response = client.getAllVersions(params);

    assertThat(response.getComponents()).hasSize(2);
    assertResultComponent(response, 0, "1.1.0-01", "1");
    assertResultComponent(response, 1, "1.2.0-01", "2");
  }

  private String getCannedResponse(final String path) throws IOException {
    return IOUtils.toString(getClass().getResource("/NexusRepository3ClientTest/" + path),
        StandardCharsets.UTF_8);
  }

  private void assertResultComponent(
      final RepositoryAllVersionsResponse response,
      final int index,
      final String version,
      final String sha1)
  {
    assertThat(response.getComponents().get(index).getIdentifier().get("version")).isEqualTo(version);
    assertThat(response.getComponents().get(index).getSha1()).isEqualTo(sha1);
  }
}
