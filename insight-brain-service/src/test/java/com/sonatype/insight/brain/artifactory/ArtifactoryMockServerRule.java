/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.artifactory;

import java.util.Locale;

import com.sonatype.insight.brain.artifactory.client.ArtifactoryChecksumSearchErrors;
import com.sonatype.insight.brain.artifactory.client.ArtifactoryChecksumSearchResults;
import com.sonatype.insight.brain.artifactory.client.ChecksumType;
import com.sonatype.insight.json.store.JsonUtils;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.rules.ExternalResource;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class ArtifactoryMockServerRule
    extends ExternalResource
{
  public static final String ARTIFACTORY_ID_HEADER_MOCK_VALUE = "0986f3b9fd628e90:634d7e6:181a15d54ff:-8000";

  private WireMockServer artifactoryMockServer;

  private String path;

  @Override
  protected void before() throws Throwable {
    artifactoryMockServer = new WireMockServer(wireMockConfig().dynamicPort());
    artifactoryMockServer.start();
    path = "";
  }

  @Override
  protected void after() {
    artifactoryMockServer.stop();
  }

  public WireMockServer getWireMockServer() {
    return artifactoryMockServer;
  }

  public String getBaseUrl() {
    return artifactoryMockServer.baseUrl();
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getUrlPath(String path) {
    return this.path + path;
  }

  public void mockSearchChecksum(ChecksumType checksumType, String checksum, ArtifactoryChecksumSearchResults results) {
    artifactoryMockServer.stubFor(
        get(urlPathMatching(getUrlPath(DefaultArtifactoryClient.CHECKSUM_SEARCH_PATH)))
            .withQueryParam(checksumType.name().toLowerCase(Locale.ROOT), equalTo(checksum))
            .willReturn(aResponse().withHeader(DefaultArtifactoryClient.ARTIFACTORY_ID_HEADER_NAME,
                    ARTIFACTORY_ID_HEADER_MOCK_VALUE)
                .withBody(JsonUtils.format(results)).withStatus(200))
    );
  }

  public void mockSearchChecksum(
      String username,
      char[] password,
      ChecksumType checksumType,
      String checksum,
      ArtifactoryChecksumSearchResults results)
  {
    artifactoryMockServer.stubFor(
        get(urlPathMatching(getUrlPath(DefaultArtifactoryClient.CHECKSUM_SEARCH_PATH)))
            .withBasicAuth(username, String.valueOf(password))
            .withQueryParam(checksumType.name().toLowerCase(Locale.ROOT), equalTo(checksum))
            .willReturn(aResponse().withHeader(DefaultArtifactoryClient.ARTIFACTORY_ID_HEADER_NAME,
                    ARTIFACTORY_ID_HEADER_MOCK_VALUE)
                .withBody(JsonUtils.format(results)).withStatus(200))
    );
  }

  public void mockSearchChecksumError(
      ChecksumType checksumType,
      String checksum,
      ArtifactoryChecksumSearchErrors errors)
  {
    artifactoryMockServer.stubFor(
        get(urlPathMatching(getUrlPath(DefaultArtifactoryClient.CHECKSUM_SEARCH_PATH)))
            .withQueryParam(checksumType.name().toLowerCase(Locale.ROOT), equalTo(checksum))
            .willReturn(aResponse().withHeader(DefaultArtifactoryClient.ARTIFACTORY_ID_HEADER_NAME,
                    ARTIFACTORY_ID_HEADER_MOCK_VALUE)
                .withBody(JsonUtils.format(errors)).withStatus(errors.errors.get(0).status))
    );
  }

  public void mockSearchChecksumError(ChecksumType checksumType, String checksum, int status) {
    artifactoryMockServer.stubFor(
        get(urlPathMatching(getUrlPath(DefaultArtifactoryClient.CHECKSUM_SEARCH_PATH)))
            .withQueryParam(checksumType.name().toLowerCase(Locale.ROOT), equalTo(checksum))
            .willReturn(aResponse().withHeader(DefaultArtifactoryClient.ARTIFACTORY_ID_HEADER_NAME,
                ARTIFACTORY_ID_HEADER_MOCK_VALUE).withStatus(status))
    );
  }
}
