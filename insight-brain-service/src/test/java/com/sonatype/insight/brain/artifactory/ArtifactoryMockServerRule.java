/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.artifactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import com.sonatype.insight.brain.artifactory.client.ArtifactoryChecksumSearchErrors;
import com.sonatype.insight.brain.artifactory.client.ArtifactoryChecksumSearchResults;
import com.sonatype.insight.brain.artifactory.client.ArtifactoryQueryLanguageUtils;
import com.sonatype.insight.brain.artifactory.client.ChecksumType;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.rules.ExternalResource;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class ArtifactoryMockServerRule
    extends ExternalResource
{
  public static final String ARTIFACTORY_ID_HEADER_MOCK_VALUE = "0986f3b9fd628e90:634d7e6:181a15d54ff:-8000";

  private WireMockServer artifactoryMockServer;

  private String path;

  @Override
  protected void before() {
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

  public String getUrl() {
    return getBaseUrl() + getPath();
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getRelativePath(String path) {
    return getPath() + path;
  }

  public void mockSearchByChecksumsUsingAQL(
      String username,
      char[] password,
      ChecksumType checksumType,
      Set<String> checksums,
      Set<String> repositories,
      JsonNode... results)
  {
    artifactoryMockServer.stubFor(
        post(urlPathMatching(getRelativePath(ArtifactoryClient.AQL_SEARCH_PATH)))
            .withBasicAuth(username, String.valueOf(password))
            .withRequestBody(equalTo(ArtifactoryQueryLanguageUtils
                .createChecksumSearch(checksumType, checksums, repositories)))
            .willReturn(aResponse().withHeader(ArtifactoryClient.ARTIFACTORY_ID_HEADER_NAME,
                ARTIFACTORY_ID_HEADER_MOCK_VALUE)
                .withBody(JsonUtils.format(createAQLResults(results)))
                .withStatus(200)));
  }

  public void mockSearchByChecksumsUsingAQLError(
      String username,
      char[] password,
      ChecksumType checksumType,
      Set<String> checksums,
      int status,
      String error)
  {
    artifactoryMockServer.stubFor(
        post(urlPathMatching(getRelativePath(ArtifactoryClient.AQL_SEARCH_PATH)))
            .withBasicAuth(username, String.valueOf(password))
            .withRequestBody(equalTo(ArtifactoryQueryLanguageUtils
                .createChecksumSearch(checksumType, checksums, Collections.emptySet())))
            .willReturn(aResponse().withHeader(ArtifactoryClient.ARTIFACTORY_ID_HEADER_NAME,
                ARTIFACTORY_ID_HEADER_MOCK_VALUE)
                .withStatus(status)
                .withBody(error)));
  }

  public static JsonNode createAQLResult(
      ChecksumType checksumType,
      String checksum,
      String repo,
      String path,
      String name)
  {
    ObjectNode result = new ObjectMapper().createObjectNode();
    result.put(checksumType.name().toLowerCase(Locale.ROOT), checksum);
    result.put(ArtifactoryQueryLanguageUtils.FIELD_REPO, repo);
    result.put(ArtifactoryQueryLanguageUtils.FIELD_PATH, path);
    result.put(ArtifactoryQueryLanguageUtils.FIELD_NAME, name);
    return result;
  }

  private static JsonNode createAQLResults(JsonNode... nodes) {
    ObjectNode wrapper = new ObjectMapper().createObjectNode();
    ArrayNode results = wrapper.putArray("results");
    results.addAll(Arrays.asList(nodes));
    return wrapper;
  }

  public void mockSearchChecksum(ChecksumType checksumType, String checksum, ArtifactoryChecksumSearchResults results) {
    artifactoryMockServer.stubFor(
        get(urlPathMatching(getRelativePath(ArtifactoryClient.CHECKSUM_SEARCH_PATH)))
            .withQueryParam(checksumType.name().toLowerCase(Locale.ROOT), equalTo(checksum))
            .willReturn(aResponse().withHeader(ArtifactoryClient.ARTIFACTORY_ID_HEADER_NAME,
                ARTIFACTORY_ID_HEADER_MOCK_VALUE)
                .withBody(JsonUtils.format(results))
                .withStatus(200)));
  }

  public void mockSearchChecksum(
      String username,
      char[] password,
      ChecksumType checksumType,
      String checksum,
      String repos,
      ArtifactoryChecksumSearchResults results)
  {
    artifactoryMockServer.stubFor(
        get(urlPathMatching(getRelativePath(ArtifactoryClient.CHECKSUM_SEARCH_PATH)))
            .withBasicAuth(username, String.valueOf(password))
            .withQueryParam(checksumType.name().toLowerCase(Locale.ROOT), equalTo(checksum))
            .withQueryParam("repos", equalTo(repos))
            .willReturn(aResponse().withHeader(ArtifactoryClient.ARTIFACTORY_ID_HEADER_NAME,
                ARTIFACTORY_ID_HEADER_MOCK_VALUE)
                .withBody(JsonUtils.format(results))
                .withStatus(200)));
  }

  public void mockSearchChecksumError(
      ChecksumType checksumType,
      String checksum,
      ArtifactoryChecksumSearchErrors errors)
  {
    artifactoryMockServer.stubFor(
        get(urlPathMatching(getRelativePath(ArtifactoryClient.CHECKSUM_SEARCH_PATH)))
            .withQueryParam(checksumType.name().toLowerCase(Locale.ROOT), equalTo(checksum))
            .willReturn(aResponse().withHeader(ArtifactoryClient.ARTIFACTORY_ID_HEADER_NAME,
                ARTIFACTORY_ID_HEADER_MOCK_VALUE)
                .withBody(JsonUtils.format(errors))
                .withStatus(errors.errors.get(0).status)));
  }

  public void mockSearchChecksumError(ChecksumType checksumType, String checksum, int status) {
    artifactoryMockServer.stubFor(
        get(urlPathMatching(getRelativePath(ArtifactoryClient.CHECKSUM_SEARCH_PATH)))
            .withQueryParam(checksumType.name().toLowerCase(Locale.ROOT), equalTo(checksum))
            .willReturn(aResponse().withHeader(ArtifactoryClient.ARTIFACTORY_ID_HEADER_NAME,
                ARTIFACTORY_ID_HEADER_MOCK_VALUE).withStatus(status)));
  }
}
