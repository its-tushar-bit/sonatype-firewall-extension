/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.artifactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;

import com.sonatype.insight.brain.artifactory.client.ArtifactoryChecksumSearchErrors;
import com.sonatype.insight.brain.artifactory.client.ArtifactoryChecksumSearchResults;
import com.sonatype.insight.brain.artifactory.client.ArtifactoryClient;
import com.sonatype.insight.brain.artifactory.client.ArtifactoryQueryLanguageUtils;
import com.sonatype.insight.brain.artifactory.client.ChecksumType;
import com.sonatype.insight.brain.report.RepositoryMatcher;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.error.exception.NotAuthenticatedException;
import com.sonatype.insight.test.LogOutput;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class DefaultArtifactoryClientTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(DefaultArtifactoryClient.class);

  @Rule
  public ArtifactoryMockServerRule artifactoryMockServer = new ArtifactoryMockServerRule();

  @Inject
  private ArtifactoryClientFactory artifactoryClientFactory;

  @Test
  public void testInitialize() {
    Configuration configuration = new Configuration();
    configuration.setServerUrl(artifactoryMockServer.getUrl());
    assertThat(new DefaultArtifactoryClient(configuration)).isInstanceOf(ArtifactoryClient.class);
  }

  @Test
  public void testSearchByChecksum_sha256() throws Exception {
    ArtifactoryClient artifactoryClient =
        artifactoryClientFactory.create().forArtifactory(artifactoryMockServer.getUrl(), null, null);
    String sha256 = "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941";
    ArtifactoryChecksumSearchResults expectedResults = ArtifactoryChecksumSearchResults.create("uri1", "uri2");
    artifactoryMockServer.mockSearchChecksum(ChecksumType.SHA256, sha256, expectedResults);

    ArtifactoryChecksumSearchResults results = artifactoryClient.searchByChecksum(ChecksumType.SHA256, sha256);

    assertThat(results).usingRecursiveComparison().isEqualTo(expectedResults);
    assertThat(logOutput).atDebugLevel().contains("Artifactory checksum search response status: HTTP/1.1 200 OK");
  }

  @Test
  public void testSearchByChecksum_sha256_NoMatches() throws Exception {
    ArtifactoryClient artifactoryClient =
        artifactoryClientFactory.create().forArtifactory(artifactoryMockServer.getUrl(), null, null);
    String sha256 = "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941";
    ArtifactoryChecksumSearchResults expectedResults = ArtifactoryChecksumSearchResults.create();
    artifactoryMockServer.mockSearchChecksum(ChecksumType.SHA256, sha256, expectedResults);

    ArtifactoryChecksumSearchResults results = artifactoryClient.searchByChecksum(ChecksumType.SHA256, sha256);

    assertThat(results).usingRecursiveComparison().isEqualTo(expectedResults);
    assertThat(logOutput).atDebugLevel().contains("Artifactory checksum search response status: HTTP/1.1 200 OK");
  }

  @Test
  public void testSearchByChecksum_sha256_WithCredentials() throws Exception {
    String username = "admin";
    char[] password = "admin123".toCharArray();
    ArtifactoryClient artifactoryClient =
        artifactoryClientFactory.create().forArtifactory(artifactoryMockServer.getUrl(), username, password);
    String sha256 = "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941";
    ArtifactoryChecksumSearchResults expectedResults = ArtifactoryChecksumSearchResults.create("uri1", "uri2");
    artifactoryMockServer.mockSearchChecksum(username, password, ChecksumType.SHA256, sha256, expectedResults);

    ArtifactoryChecksumSearchResults results = artifactoryClient.searchByChecksum(ChecksumType.SHA256, sha256);

    assertThat(results).usingRecursiveComparison().isEqualTo(expectedResults);
    assertThat(logOutput).atDebugLevel().contains("Artifactory checksum search response status: HTTP/1.1 200 OK");
  }

  @Test
  public void testSearchByChecksum_sha256_NotAuthenticatedException() {
    ArtifactoryClient artifactoryClient =
        artifactoryClientFactory.create().forArtifactory(artifactoryMockServer.getUrl(), null, null);
    String sha256 = "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941";
    ArtifactoryChecksumSearchErrors errors = ArtifactoryChecksumSearchErrors.create(401, "auth error");
    artifactoryMockServer.mockSearchChecksumError(ChecksumType.SHA256, sha256, errors);

    assertThatExceptionOfType(NotAuthenticatedException.class)
        .isThrownBy(() -> artifactoryClient.searchByChecksum(ChecksumType.SHA256, sha256))
        .withMessageContaining(errors.errors.get(0).message);
    assertThat(logOutput).atErrorLevel()
        .contains("Artifactory error raw response")
        .contains("\"status\" : " + errors.errors.get(0).status)
        .contains("\"message\" : \"" + errors.errors.get(0).message + "\"");
  }

  @Test
  public void testSearchByChecksum_sha256_BadGatewayException() {
    ArtifactoryClient artifactoryClient =
        artifactoryClientFactory.create().forArtifactory(artifactoryMockServer.getUrl(), null, null);
    String sha256 = "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941";
    ArtifactoryChecksumSearchErrors errors = ArtifactoryChecksumSearchErrors.create(500, "some error");
    artifactoryMockServer.mockSearchChecksumError(ChecksumType.SHA256, sha256, errors);

    assertThatExceptionOfType(BadGatewayException.class)
        .isThrownBy(() -> artifactoryClient.searchByChecksum(ChecksumType.SHA256, sha256))
        .withMessageContaining(errors.errors.get(0).message);
    assertThat(logOutput).atErrorLevel()
        .contains("Artifactory error raw response")
        .contains("\"status\" : " + errors.errors.get(0).status)
        .contains("\"message\" : \"" + errors.errors.get(0).message + "\"");
  }

  @Test
  public void testGetServerStatus_ViaQueryParam() throws Exception {
    ArtifactoryClient artifactoryClient =
        artifactoryClientFactory.create().forArtifactory(artifactoryMockServer.getUrl(), null, null);
    artifactoryMockServer.mockSearchChecksum(ChecksumType.SHA256, DefaultArtifactoryClient.TEST_SHA256,
        new ArtifactoryChecksumSearchResults());

    StatusType status = artifactoryClient.getServerStatusViaQueryParam();

    assertThat(status).isEqualTo(Status.fromStatusCode(200));
    assertThat(logOutput).atDebugLevel().containsPattern("Artifactory server header [-:\\w]+, status HTTP/1.1 200 OK");
  }

  @Test
  public void testGetServerStatus_ViaAQL() throws Exception {
    String username = "admin";
    char[] password = "admin123".toCharArray();
    ArtifactoryClient artifactoryClient =
        artifactoryClientFactory.create().forArtifactory(artifactoryMockServer.getUrl(), username, password);
    artifactoryMockServer.mockSearchByChecksumsUsingAQL(
        username,
        password,
        ChecksumType.SHA256,
        Collections.singleton(DefaultArtifactoryClient.TEST_SHA256)
    );

    StatusType status = artifactoryClient.getServerStatusViaAQL();

    assertThat(status).isEqualTo(Status.fromStatusCode(200));
    assertThat(logOutput).atDebugLevel().containsPattern("Artifactory server header [-:\\w]+, status HTTP/1.1 200 OK");
  }

  @Test
  public void testGetServerStatus_Error() throws Exception {
    ArtifactoryClient artifactoryClient = artifactoryClientFactory.create()
        .forArtifactory(artifactoryMockServer.getUrl(), null, null);
    testGetServerStatus_Error_ViaQueryParam(artifactoryClient);

    artifactoryClient = artifactoryClientFactory.create()
        .forArtifactory(artifactoryMockServer.getUrl(), "admin", "admin123".toCharArray());
    testGetServerStatus_Error_ViaAQL(artifactoryClient);
  }

  private void testGetServerStatus_Error_ViaQueryParam(ArtifactoryClient artifactoryClient) throws Exception {
    artifactoryMockServer.mockSearchChecksumError(ChecksumType.SHA256, DefaultArtifactoryClient.TEST_SHA256, 500);
    StatusType status = artifactoryClient.getServerStatusViaQueryParam();

    assertThat(status).isEqualTo(Status.fromStatusCode(500));
    assertThat(logOutput).atDebugLevel()
        .containsPattern("Artifactory server header [-:\\w]+, status HTTP/1.1 500 Server Error");
  }

  private void testGetServerStatus_Error_ViaAQL(ArtifactoryClient artifactoryClient) throws Exception {
    artifactoryMockServer.mockSearchByChecksumsUsingAQLError(
        "admin",
        "admin123".toCharArray(),
        ChecksumType.SHA256,
        Collections.singleton(DefaultArtifactoryClient.TEST_SHA256),
        500,
        "");
    StatusType status = artifactoryClient.getServerStatusViaAQL();

    assertThat(status).isEqualTo(Status.fromStatusCode(500));
    assertThat(logOutput).atDebugLevel()
        .containsPattern("Artifactory server header [-:\\w]+, status HTTP/1.1 500 Server Error");
  }

  @Test
  public void testGetServerStatus_MissingHeader() throws Exception {
    ArtifactoryClient artifactoryClient = artifactoryClientFactory.create()
        .forArtifactory(artifactoryMockServer.getUrl(), null, null);
    testGetServerStatus_MissingHeader_ViaQueryParam(artifactoryClient);

    artifactoryClient = artifactoryClientFactory.create()
        .forArtifactory(artifactoryMockServer.getUrl(), "admin", "admin123".toCharArray());
    testGetServerStatus_MissingHeader_ViaAQL(artifactoryClient);
  }

  private void testGetServerStatus_MissingHeader_ViaQueryParam(ArtifactoryClient artifactoryClient) throws Exception {
    artifactoryMockServer.getWireMockServer().stubFor(get(urlPathMatching(
        artifactoryMockServer.getRelativePath(DefaultArtifactoryClient.CHECKSUM_SEARCH_PATH))).withQueryParam(
            ChecksumType.SHA256.name().toLowerCase(Locale.ROOT), equalTo(DefaultArtifactoryClient.TEST_SHA256))
        .willReturn(aResponse().withStatus(200)));

    StatusType status = artifactoryClient.getServerStatusViaQueryParam();

    assertThat(status).isNotNull();
    assertThat(status.getStatusCode()).isEqualTo(Status.BAD_REQUEST.getStatusCode());
    assertThat(status.getFamily()).isEqualTo(Status.BAD_REQUEST.getFamily());
    assertThat(status.getReasonPhrase()).isEqualTo("Bad Request. Not a valid Artifactory server.");
    assertThat(logOutput).atDebugLevel()
        .containsPattern("Artifactory server header null, status HTTP/1.1 200 OK");
  }

  private void testGetServerStatus_MissingHeader_ViaAQL(ArtifactoryClient artifactoryClient) throws Exception {
    artifactoryMockServer.getWireMockServer().stubFor(post(urlPathMatching(
        artifactoryMockServer.getRelativePath(DefaultArtifactoryClient.AQL_SEARCH_PATH)))
        .withBasicAuth("admin", String.valueOf("admin123".toCharArray()))
        .withRequestBody(
            equalTo(
                ArtifactoryQueryLanguageUtils.createChecksumSearch(
                    ChecksumType.SHA256,
                    Collections.singleton(DefaultArtifactoryClient.TEST_SHA256))))
        .willReturn(aResponse().withStatus(200)));
    StatusType status = artifactoryClient.getServerStatusViaAQL();

    assertThat(status).isNotNull();
    assertThat(status.getStatusCode()).isEqualTo(Status.BAD_REQUEST.getStatusCode());
    assertThat(status.getFamily()).isEqualTo(Status.BAD_REQUEST.getFamily());
    assertThat(status.getReasonPhrase()).isEqualTo("Bad Request. Not a valid Artifactory server.");
    assertThat(logOutput).atDebugLevel()
        .containsPattern("Artifactory server header null, status HTTP/1.1 200 OK");
  }

  @Test
  public void testGetServerStatus_MissingHeaderValue() throws Exception {
    ArtifactoryClient artifactoryClient = artifactoryClientFactory.create()
        .forArtifactory(artifactoryMockServer.getUrl(), null, null);
    testGetServerStatus_MissingHeaderValue_ViaQueryParam(artifactoryClient);

    artifactoryClient = artifactoryClientFactory.create()
        .forArtifactory(artifactoryMockServer.getUrl(), "admin", "admin123".toCharArray());
    testGetServerStatus_MissingHeaderValue_ViaAQL(artifactoryClient);

  }

  private void testGetServerStatus_MissingHeaderValue_ViaQueryParam(ArtifactoryClient artifactoryClient)
      throws Exception
  {
    artifactoryMockServer.getWireMockServer().stubFor(get(urlPathMatching(
        artifactoryMockServer.getRelativePath(DefaultArtifactoryClient.CHECKSUM_SEARCH_PATH))).withQueryParam(
            ChecksumType.SHA256.name().toLowerCase(Locale.ROOT), equalTo(DefaultArtifactoryClient.TEST_SHA256))
        .willReturn(aResponse().withHeader(DefaultArtifactoryClient.ARTIFACTORY_ID_HEADER_NAME, "").withStatus(200)));

    StatusType status = artifactoryClient.getServerStatusViaQueryParam();

    assertThat(status).isNotNull();
    assertThat(status.getStatusCode()).isEqualTo(Status.BAD_REQUEST.getStatusCode());
    assertThat(status.getFamily()).isEqualTo(Status.BAD_REQUEST.getFamily());
    assertThat(status.getReasonPhrase()).isEqualTo("Bad Request. Not a valid Artifactory server.");
    assertThat(logOutput).atDebugLevel()
        .containsPattern("Artifactory server header , status HTTP/1.1 200 OK");
  }

  private void testGetServerStatus_MissingHeaderValue_ViaAQL(ArtifactoryClient artifactoryClient) throws Exception {
    artifactoryMockServer.getWireMockServer().stubFor(post(urlPathMatching(
        artifactoryMockServer.getRelativePath(DefaultArtifactoryClient.AQL_SEARCH_PATH)))
        .withBasicAuth("admin", String.valueOf("admin123".toCharArray()))
        .withRequestBody(
            equalTo(
                ArtifactoryQueryLanguageUtils.createChecksumSearch(
                    ChecksumType.SHA256,
                    Collections.singleton(DefaultArtifactoryClient.TEST_SHA256))))
        .willReturn(aResponse().withHeader(DefaultArtifactoryClient.ARTIFACTORY_ID_HEADER_NAME, "").withStatus(200)));

    StatusType status = artifactoryClient.getServerStatusViaAQL();

    assertThat(status).isNotNull();
    assertThat(status.getStatusCode()).isEqualTo(Status.BAD_REQUEST.getStatusCode());
    assertThat(status.getFamily()).isEqualTo(Status.BAD_REQUEST.getFamily());
    assertThat(status.getReasonPhrase()).isEqualTo("Bad Request. Not a valid Artifactory server.");
    assertThat(logOutput).atDebugLevel()
        .containsPattern("Artifactory server header , status HTTP/1.1 200 OK");
  }

  @Test
  public void testSearchByChecksum_WithCredentials_WithPath() throws Exception {
    String path = "/artifactory";
    artifactoryMockServer.setPath(path);
    String username = "admin";
    char[] password = "admin123".toCharArray();
    ArtifactoryClient artifactoryClient =
        artifactoryClientFactory.create().forArtifactory(artifactoryMockServer.getUrl(), username, password);
    String sha256 = "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941";
    ArtifactoryChecksumSearchResults expectedResults = ArtifactoryChecksumSearchResults.create("uri1", "uri2");
    artifactoryMockServer.mockSearchChecksum(username, password, ChecksumType.SHA256, sha256, expectedResults);

    ArtifactoryChecksumSearchResults results = artifactoryClient.searchByChecksum(ChecksumType.SHA256, sha256);

    assertThat(results).usingRecursiveComparison().isEqualTo(expectedResults);
    assertThat(logOutput).atDebugLevel().contains("Artifactory checksum search response status: HTTP/1.1 200 OK");
  }

  @Test
  public void testSearchByChecksum_WithPath() throws Exception {
    String path = "/artifactory";
    artifactoryMockServer.setPath(path);
    ArtifactoryClient artifactoryClient =
        artifactoryClientFactory.create().forArtifactory(artifactoryMockServer.getUrl(), null, null);
    String sha256 = "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941";
    ArtifactoryChecksumSearchResults expectedResults = ArtifactoryChecksumSearchResults.create("uri1", "uri2");
    artifactoryMockServer.mockSearchChecksum(ChecksumType.SHA256, sha256, expectedResults);

    ArtifactoryChecksumSearchResults results = artifactoryClient.searchByChecksum(ChecksumType.SHA256, sha256);

    assertThat(results).usingRecursiveComparison().isEqualTo(expectedResults);
    assertThat(logOutput).atDebugLevel().contains("Artifactory checksum search response status: HTTP/1.1 200 OK");
  }

  @Test
  public void testCreateChecksumSearch_Empty() {
    ChecksumType checksumType = ChecksumType.SHA256;
    Set<String> checksums = new LinkedHashSet<>();

    String checksumSearch = ArtifactoryQueryLanguageUtils.createChecksumSearch(checksumType, checksums);

    assertThat(checksumSearch).isEqualTo("items.find({\"$or\":[]}).include(\"sha256\",\"repo\",\"path\",\"name\")");
  }

  @Test
  public void testCreateChecksumSearch() {
    ChecksumType checksumType = ChecksumType.SHA256;
    Set<String> checksums = new LinkedHashSet<>(Arrays.asList(
        "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941",
        "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f942",
        "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f943"
    ));

    String checksumSearch = ArtifactoryQueryLanguageUtils.createChecksumSearch(checksumType, checksums);

    assertThat(checksumSearch).isEqualTo("items.find({\"$or\":[" +
        "{\"sha256\":\"eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941\"}," +
        "{\"sha256\":\"eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f942\"}," +
        "{\"sha256\":\"eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f943\"}]" +
        "}).include(\"sha256\",\"repo\",\"path\",\"name\")");
  }

  @Test
  public void testSearchByChecksumsUsingAQL_Null() throws Exception {
    ArtifactoryClient artifactoryClient = artifactoryClientFactory.create()
        .forArtifactory(artifactoryMockServer.getUrl(), "admin", "admin123".toCharArray());

    assertThat(artifactoryClient.searchByChecksumsUsingAQL(null, null)).isEmpty();
    assertThat(logOutput).atDebugLevel().contains("No checksums provided for AQL call, returning empty result.");
  }

  @Test
  public void testSearchByChecksumsUsingAQL_Empty() throws Exception {
    ArtifactoryClient artifactoryClient = artifactoryClientFactory.create()
        .forArtifactory(artifactoryMockServer.getUrl(), "admin", "admin123".toCharArray());

    assertThat(artifactoryClient.searchByChecksumsUsingAQL(null, Collections.emptySet())).isEmpty();
    assertThat(logOutput).atDebugLevel().contains("No checksums provided for AQL call, returning empty result.");
  }

  @Test
  public void testSearchByChecksumsUsingAQL() throws Exception {
    String username = "admin";
    char[] password = "admin123".toCharArray();
    String path = "/artifactory";
    artifactoryMockServer.setPath(path);
    ArtifactoryClient artifactoryClient =
        artifactoryClientFactory.create().forArtifactory(artifactoryMockServer.getUrl(), username, password);
    JsonNode[] nodes = new JsonNode[]{
        ArtifactoryMockServerRule.createAQLResult(ChecksumType.SHA256,
            "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941", "repo1a", "path1a", "name1a"),
        ArtifactoryMockServerRule.createAQLResult(ChecksumType.SHA256,
            "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941", "repo1b", "path1b", "name1b"),
        ArtifactoryMockServerRule.createAQLResult(ChecksumType.SHA256,
            "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f942", "repo2", "path2", "name2"),
        ArtifactoryMockServerRule.createAQLResult(ChecksumType.SHA256,
            "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f943", "", "path3", "name3"),
        ArtifactoryMockServerRule.createAQLResult(ChecksumType.SHA256,
            "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f943", "repo3", "", "name3"),
        ArtifactoryMockServerRule.createAQLResult(ChecksumType.SHA256,
            "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f943", "repo3", "path3", ""),
        ArtifactoryMockServerRule.createAQLResult(ChecksumType.SHA256, "", "repo3", "path3", "name3"),
        ArtifactoryMockServerRule.createAQLResult(ChecksumType.SHA256, "unexpected", "repo4", "path4", "name4")
    };
    List<String> sha256s = Arrays.stream(nodes)
        .map(n -> n.get(ChecksumType.SHA256.name().toLowerCase(Locale.ROOT)).asText())
        .distinct()
        .collect(Collectors.toList());
    Set<String> sha256sSet = new LinkedHashSet<>(Arrays.asList(sha256s.get(0), sha256s.get(1), sha256s.get(2)));
    artifactoryMockServer.mockSearchByChecksumsUsingAQL(username, password, ChecksumType.SHA256, sha256sSet, nodes);

    Map<String, ArtifactoryChecksumSearchResults> results =
        artifactoryClient.searchByChecksumsUsingAQL(ChecksumType.SHA256, sha256sSet);

    String expectedUriPrefix = artifactoryMockServer.getUrl() + RepositoryMatcher.API_STORAGE_PREFIX;
    assertThat(results).containsOnlyKeys(sha256s.get(0), sha256s.get(1));
    assertThat(results.get(sha256s.get(0))).usingRecursiveComparison().isEqualTo(
        ArtifactoryChecksumSearchResults.create(expectedUriPrefix + "repo1a/path1a/name1a",
            expectedUriPrefix + "repo1b/path1b/name1b"));
    assertThat(results.get(sha256s.get(1))).usingRecursiveComparison().isEqualTo(
        ArtifactoryChecksumSearchResults.create(expectedUriPrefix + "repo2/path2/name2"));

    assertThat(logOutput).atDebugLevel().contains("Artifactory AQL checksums search response status: HTTP/1.1 200 OK");
  }

  @Test
  public void testSearchByChecksumsUsingAQL_NoMatches() throws Exception {
    String username = "admin";
    char[] password = "admin123".toCharArray();
    String path = "/artifactory";
    artifactoryMockServer.setPath(path);
    ArtifactoryClient artifactoryClient =
        artifactoryClientFactory.create().forArtifactory(artifactoryMockServer.getUrl(), username, password);
    List<String> sha256s =
        Collections.singletonList("eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941");
    Set<String> sha256sSet = new LinkedHashSet<>(Collections.singletonList(sha256s.get(0)));
    artifactoryMockServer.mockSearchByChecksumsUsingAQL(username, password, ChecksumType.SHA256, sha256sSet);

    Map<String, ArtifactoryChecksumSearchResults> results =
        artifactoryClient.searchByChecksumsUsingAQL(ChecksumType.SHA256, sha256sSet);

    assertThat(results).isEmpty();

    assertThat(logOutput).atDebugLevel().contains("Artifactory AQL checksums search response status: HTTP/1.1 200 OK");
  }

  @Test
  public void testSearchByChecksumsUsingAQL_NotAuthenticatedException() {
    String username = "admin";
    char[] password = "admin123".toCharArray();
    ArtifactoryClient artifactoryClient =
        artifactoryClientFactory.create().forArtifactory(artifactoryMockServer.getUrl(), username, password);
    Set<String> checksums = new HashSet<>(Arrays.asList(
        "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941",
        "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f942",
        "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f943"
    ));
    artifactoryMockServer.mockSearchByChecksumsUsingAQLError(username, password, ChecksumType.SHA256, checksums, 401,
        "error");

    assertThatExceptionOfType(NotAuthenticatedException.class)
        .isThrownBy(() -> artifactoryClient.searchByChecksumsUsingAQL(ChecksumType.SHA256, checksums))
        .withMessageContaining("error");
  }

  @Test
  public void testSearchByChecksumsUsingAQL_BadGatewayException() {
    String username = "admin";
    char[] password = "admin123".toCharArray();
    ArtifactoryClient artifactoryClient =
        artifactoryClientFactory.create().forArtifactory(artifactoryMockServer.getUrl(), username, password);
    Set<String> checksums = new HashSet<>(Arrays.asList(
        "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941",
        "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f942",
        "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f943"
    ));
    artifactoryMockServer.mockSearchByChecksumsUsingAQLError(username, password, ChecksumType.SHA256, checksums, 500,
        "error");

    assertThatExceptionOfType(BadGatewayException.class)
        .isThrownBy(() -> artifactoryClient.searchByChecksumsUsingAQL(ChecksumType.SHA256, checksums))
        .withMessageContaining("error");
  }
}
