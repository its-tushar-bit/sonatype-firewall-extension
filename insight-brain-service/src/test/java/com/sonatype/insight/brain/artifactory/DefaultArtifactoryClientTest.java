/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.artifactory;

import java.util.Locale;

import javax.inject.Inject;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;

import com.sonatype.insight.brain.artifactory.client.ArtifactoryChecksumSearchErrors;
import com.sonatype.insight.brain.artifactory.client.ArtifactoryChecksumSearchResults;
import com.sonatype.insight.brain.artifactory.client.ArtifactoryClient;
import com.sonatype.insight.brain.artifactory.client.ChecksumType;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.error.exception.NotAuthenticatedException;

import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class DefaultArtifactoryClientTest
    extends AbstractComponentTest
{
  @Rule
  public ArtifactoryMockServerRule artifactoryMockServer = new ArtifactoryMockServerRule();

  @Inject
  private ArtifactoryClientFactory artifactoryClientFactory;

  @Test
  public void testInitialize() {
    Configuration configuration = new Configuration();
    configuration.setServerUrl(artifactoryMockServer.getBaseUrl());
    assertThat(new DefaultArtifactoryClient(configuration)).isInstanceOf(ArtifactoryClient.class);
  }

  @Test
  public void testSearchByChecksum_sha256() throws Exception {
    ArtifactoryClient artifactoryClient =
        artifactoryClientFactory.create().forArtifactory(artifactoryMockServer.getBaseUrl(), null, null);
    String sha256 = "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941";
    ArtifactoryChecksumSearchResults expectedResults = ArtifactoryChecksumSearchResults.create("uri1", "uri2");
    artifactoryMockServer.mockSearchChecksum(ChecksumType.SHA256, sha256, expectedResults);

    ArtifactoryChecksumSearchResults results = artifactoryClient.searchByChecksum(ChecksumType.SHA256, sha256);

    assertThat(results).usingRecursiveComparison().isEqualTo(expectedResults);
  }

  @Test
  public void testSearchByChecksum_sha256_WithCredentials() throws Exception {
    String username = "admin";
    char[] password = "admin123".toCharArray();
    ArtifactoryClient artifactoryClient =
        artifactoryClientFactory.create().forArtifactory(artifactoryMockServer.getBaseUrl(), username, password);
    String sha256 = "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941";
    ArtifactoryChecksumSearchResults expectedResults = ArtifactoryChecksumSearchResults.create("uri1", "uri2");
    artifactoryMockServer.mockSearchChecksum(username, password, ChecksumType.SHA256, sha256, expectedResults);

    ArtifactoryChecksumSearchResults results = artifactoryClient.searchByChecksum(ChecksumType.SHA256, sha256);

    assertThat(results).usingRecursiveComparison().isEqualTo(expectedResults);
  }

  @Test
  public void testSearchByChecksum_sha256_NotAuthenticatedException() {
    ArtifactoryClient artifactoryClient =
        artifactoryClientFactory.create().forArtifactory(artifactoryMockServer.getBaseUrl(), null, null);
    String sha256 = "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941";
    ArtifactoryChecksumSearchErrors errors = ArtifactoryChecksumSearchErrors.create(401, "auth error");
    artifactoryMockServer.mockSearchChecksumError(ChecksumType.SHA256, sha256, errors);

    assertThatExceptionOfType(NotAuthenticatedException.class).isThrownBy(
        () -> artifactoryClient.searchByChecksum(ChecksumType.SHA256, sha256))
        .withMessageContaining(errors.errors.get(0).message);
  }

  @Test
  public void testSearchByChecksum_sha256_BadGatewayException() {
    ArtifactoryClient artifactoryClient =
        artifactoryClientFactory.create().forArtifactory(artifactoryMockServer.getBaseUrl(), null, null);
    String sha256 = "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941";
    ArtifactoryChecksumSearchErrors errors = ArtifactoryChecksumSearchErrors.create(500, "some error");
    artifactoryMockServer.mockSearchChecksumError(ChecksumType.SHA256, sha256, errors);

    assertThatExceptionOfType(BadGatewayException.class).isThrownBy(
        () -> artifactoryClient.searchByChecksum(ChecksumType.SHA256, sha256))
        .withMessageContaining(errors.errors.get(0).message);
  }

  @Test
  public void testGetServerStatus() throws Exception {
    ArtifactoryClient artifactoryClient =
        artifactoryClientFactory.create().forArtifactory(artifactoryMockServer.getBaseUrl(), null, null);
    artifactoryMockServer.mockSearchChecksum(ChecksumType.SHA256, DefaultArtifactoryClient.TEST_SHA256,
        new ArtifactoryChecksumSearchResults());

    StatusType status = artifactoryClient.getServerStatus();

    assertThat(status).isEqualTo(Status.fromStatusCode(200));
  }

  @Test
  public void testGetServerStatus_WithCredentials() throws Exception {
    String username = "admin";
    char[] password = "admin123".toCharArray();
    ArtifactoryClient artifactoryClient =
        artifactoryClientFactory.create().forArtifactory(artifactoryMockServer.getBaseUrl(), username, password);
    artifactoryMockServer.mockSearchChecksum(username, password, ChecksumType.SHA256,
        DefaultArtifactoryClient.TEST_SHA256, new ArtifactoryChecksumSearchResults());

    StatusType status = artifactoryClient.getServerStatus();

    assertThat(status).isEqualTo(Status.fromStatusCode(200));
  }

  @Test
  public void testGetServerStatus_Error() throws Exception {
    ArtifactoryClient artifactoryClient =
        artifactoryClientFactory.create().forArtifactory(artifactoryMockServer.getBaseUrl(), null, null);
    artifactoryMockServer.mockSearchChecksumError(ChecksumType.SHA256, DefaultArtifactoryClient.TEST_SHA256, 500);

    StatusType status = artifactoryClient.getServerStatus();

    assertThat(status).isEqualTo(Status.fromStatusCode(500));
  }

  @Test
  public void testGetServerStatus_MissingHeader() throws Exception {
    ArtifactoryClient artifactoryClient =
        artifactoryClientFactory.create().forArtifactory(artifactoryMockServer.getBaseUrl(), null, null);
    artifactoryMockServer.getWireMockServer().stubFor(get(urlPathMatching(
        artifactoryMockServer.getUrlPath(DefaultArtifactoryClient.CHECKSUM_SEARCH_PATH))).withQueryParam(
            ChecksumType.SHA256.name().toLowerCase(Locale.ROOT), equalTo(DefaultArtifactoryClient.TEST_SHA256))
        .willReturn(aResponse().withStatus(200)));

    StatusType status = artifactoryClient.getServerStatus();

    assertThat(status).isNotNull();
    assertThat(status.getStatusCode()).isEqualTo(Status.BAD_REQUEST.getStatusCode());
    assertThat(status.getFamily()).isEqualTo(Status.BAD_REQUEST.getFamily());
    assertThat(status.getReasonPhrase()).isEqualTo("Bad Request. Not a valid Artifactory server.");
  }

  @Test
  public void testGetServerStatus_MissingHeaderValue() throws Exception {
    ArtifactoryClient artifactoryClient =
        artifactoryClientFactory.create().forArtifactory(artifactoryMockServer.getBaseUrl(), null, null);
    artifactoryMockServer.getWireMockServer().stubFor(get(urlPathMatching(
        artifactoryMockServer.getUrlPath(DefaultArtifactoryClient.CHECKSUM_SEARCH_PATH))).withQueryParam(
            ChecksumType.SHA256.name().toLowerCase(Locale.ROOT), equalTo(DefaultArtifactoryClient.TEST_SHA256))
        .willReturn(aResponse().withHeader(DefaultArtifactoryClient.ARTIFACTORY_ID_HEADER_NAME, "").withStatus(200)));

    StatusType status = artifactoryClient.getServerStatus();

    assertThat(status).isNotNull();
    assertThat(status.getStatusCode()).isEqualTo(Status.BAD_REQUEST.getStatusCode());
    assertThat(status.getFamily()).isEqualTo(Status.BAD_REQUEST.getFamily());
    assertThat(status.getReasonPhrase()).isEqualTo("Bad Request. Not a valid Artifactory server.");
  }

  @Test
  public void testSearchByChecksum_WithCredentials_WithPath() throws Exception {
    String path = "/artifactory";
    artifactoryMockServer.setPath(path);
    String username = "admin";
    char[] password = "admin123".toCharArray();
    ArtifactoryClient artifactoryClient =
        artifactoryClientFactory.create().forArtifactory(artifactoryMockServer.getBaseUrl() + path, username, password);
    String sha256 = "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941";
    ArtifactoryChecksumSearchResults expectedResults = ArtifactoryChecksumSearchResults.create("uri1", "uri2");
    artifactoryMockServer.mockSearchChecksum(username, password, ChecksumType.SHA256, sha256, expectedResults);

    ArtifactoryChecksumSearchResults results = artifactoryClient.searchByChecksum(ChecksumType.SHA256, sha256);

    assertThat(results).usingRecursiveComparison().isEqualTo(expectedResults);
  }

  @Test
  public void testSearchByChecksum_WithPath() throws Exception {
    String path = "/artifactory";
    artifactoryMockServer.setPath(path);
    ArtifactoryClient artifactoryClient =
        artifactoryClientFactory.create().forArtifactory(artifactoryMockServer.getBaseUrl() + path, null, null);
    String sha256 = "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941";
    ArtifactoryChecksumSearchResults expectedResults = ArtifactoryChecksumSearchResults.create("uri1", "uri2");
    artifactoryMockServer.mockSearchChecksum(ChecksumType.SHA256, sha256, expectedResults);

    ArtifactoryChecksumSearchResults results = artifactoryClient.searchByChecksum(ChecksumType.SHA256, sha256);

    assertThat(results).usingRecursiveComparison().isEqualTo(expectedResults);
  }
}
