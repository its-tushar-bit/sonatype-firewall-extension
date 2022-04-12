/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.artifactory.ArtifactoryMockServerRule;
import com.sonatype.insight.brain.artifactory.DefaultArtifactoryClient;
import com.sonatype.insight.brain.artifactory.client.ArtifactoryChecksumSearchResult;
import com.sonatype.insight.brain.artifactory.client.ArtifactoryChecksumSearchResults;
import com.sonatype.insight.brain.artifactory.client.ChecksumType;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.artifactory.ArtifactoryConnectionDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.artifactory.ArtifactoryConnection;
import com.sonatype.insight.brain.security.PasswordHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

public class RepositoryMatcherTest
    extends InjectedTest
{
  @Rule
  public ArtifactoryMockServerRule artifactoryMockServer = new ArtifactoryMockServerRule();

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  @Inject
  private PasswordHandler passwordHandler;

  @Inject
  private RepositoryMatcher matcher;

  private ArtifactoryConnection artifactoryConnection;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Before
  public void before() {
    artifactoryConnection = tempEntity.newArtifactoryConnection(Organization.ROOT_ORGANIZATION_ID,
        artifactoryMockServer.getBaseUrl(), "artifactoryUser",
        passwordHandler.encryptPassword("password".toCharArray()));
  }

  @Test
  public void testIdentify_FilterOnlySha256() throws Exception {
    ComponentIdentifier identifier =
        ComponentIdentifier.createMavenCoordinates("g.org", "a", "1.1-SNAPSHOT", null, "jar");
    mockArtifactoryResponse();

    Map<ComponentIdentifier, ObjectNode> sha256Matches = matcher.identify(readJsonFile("match-sha256/bom.json"));

    assertThat(sha256Matches).hasSize(1).containsOnlyKeys(identifier);
    artifactoryMockServer.getWireMockServer()
        .verify(1, anyRequestedFor(urlPathEqualTo(DefaultArtifactoryClient.CHECKSUM_SEARCH_PATH)));
  }

  @Test
  public void testIdentify_FilterCorrectMatchState() throws Exception {
    ComponentIdentifier id1 =
        ComponentIdentifier.createMavenCoordinates("g.org", "a", "1.1-SNAPSHOT", null, "jar");
    mockArtifactoryResponse();
    ComponentIdentifier id2 =
        ComponentIdentifier.createMavenCoordinates("g2.org", "a2", "5.0", null, "jar");
    mockArtifactoryResponse("8c7ac48903b2a4382561321e5c25913960007a65d0f5e4a167a80d41984092c9",
        "http://localhost/artifactory/api/storage/reponame2/g2/org/a2/5.0/a2-5.0.jar"
    );

    Map<ComponentIdentifier, ObjectNode> sha256Matches = matcher.identify(readJsonFile("match-state/bom.json"));

    assertThat(sha256Matches).hasSize(2).containsOnlyKeys(id1, id2);
  }

  @Test
  public void testIdentify_FilterCorrectExtension() throws Exception {
    ComponentIdentifier identifier =
        ComponentIdentifier.createMavenCoordinates("g.org", "a", "1.1-SNAPSHOT", null, "jar");
    mockArtifactoryResponse();

    Map<ComponentIdentifier, ObjectNode> sha256Matches = matcher.identify(readJsonFile("match-extension/bom.json"));

    assertThat(sha256Matches).hasSize(1).containsOnlyKeys(identifier);
    artifactoryMockServer.getWireMockServer()
        .verify(1, anyRequestedFor(urlPathEqualTo(DefaultArtifactoryClient.CHECKSUM_SEARCH_PATH)));
  }

  @Test
  public void testIdentify_FilterNonProprietary() throws Exception {
    ComponentIdentifier identifier =
        ComponentIdentifier.createMavenCoordinates("g.org", "a", "1.1-SNAPSHOT", null, "jar");
    mockArtifactoryResponse();

    Map<ComponentIdentifier, ObjectNode> sha256Matches = matcher.identify(readJsonFile("match-proprietary/bom.json"));

    assertThat(sha256Matches).hasSize(1).containsOnlyKeys(identifier);
    artifactoryMockServer.getWireMockServer()
        .verify(1, anyRequestedFor(urlPathEqualTo(DefaultArtifactoryClient.CHECKSUM_SEARCH_PATH)));
  }

  @Test
  public void testIdentify_HandleMultipleUris() throws Exception {
    ComponentIdentifier identifier =
        ComponentIdentifier.createMavenCoordinates("g.org", "a", "1.1-SNAPSHOT", null, "jar");
    ArtifactoryChecksumSearchResult result1 = new ArtifactoryChecksumSearchResult();
    result1.uri = "invalid uri";
    ArtifactoryChecksumSearchResult result2 = new ArtifactoryChecksumSearchResult();
    result2.uri = "http://localhost/artifactory/api/storage/reponame/g/org/a/1.1-SNAPSHOT/a-1.1-SNAPSHOT.jar";
    ArtifactoryChecksumSearchResults mockResult = new ArtifactoryChecksumSearchResults();
    mockResult.results = Arrays.asList(result1, result2);
    artifactoryMockServer.mockSearchChecksum(ChecksumType.SHA256,
        "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941", mockResult);

    Map<ComponentIdentifier, ObjectNode> sha256Matches = matcher.identify(readJsonFile("match-proprietary/bom.json"));

    assertThat(sha256Matches).hasSize(1).containsOnlyKeys(identifier);
    artifactoryMockServer.getWireMockServer()
        .verify(1, anyRequestedFor(urlPathEqualTo(DefaultArtifactoryClient.CHECKSUM_SEARCH_PATH)));
  }

  @Test
  public void testIdentify_NoRecognizablResults() throws Exception {
    ArtifactoryChecksumSearchResult result = new ArtifactoryChecksumSearchResult();
    result.uri = "invalid uri";
    ArtifactoryChecksumSearchResults mockResult = new ArtifactoryChecksumSearchResults();
    mockResult.results = Collections.singletonList(result);
    artifactoryMockServer.mockSearchChecksum(ChecksumType.SHA256,
        "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941", mockResult);

    Map<ComponentIdentifier, ObjectNode> sha256Matches = matcher.identify(readJsonFile("match-proprietary/bom.json"));

    assertThat(sha256Matches).hasSize(0);
    artifactoryMockServer.getWireMockServer()
        .verify(1, anyRequestedFor(urlPathEqualTo(DefaultArtifactoryClient.CHECKSUM_SEARCH_PATH)));
  }

  @Test
  public void testIdentify_ArtifactoryError() throws Exception {
    artifactoryMockServer.mockSearchChecksumError(ChecksumType.SHA256,
        "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941", 502);

    Map<ComponentIdentifier, ObjectNode> sha256Matches = matcher.identify(readJsonFile("match-state/bom.json"));

    assertThat(sha256Matches).hasSize(0);
    //although 2 matching components, only 1 search is attempted due to connection error
    artifactoryMockServer.getWireMockServer()
        .verify(1, anyRequestedFor(urlPathEqualTo(DefaultArtifactoryClient.CHECKSUM_SEARCH_PATH)));
  }

  @Test
  public void testIdentify_NoArtifactoryMatches() throws Exception {
    ArtifactoryChecksumSearchResults mockResult = new ArtifactoryChecksumSearchResults(); //empty result
    artifactoryMockServer.mockSearchChecksum(ChecksumType.SHA256,
        "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941", mockResult);

    Map<ComponentIdentifier, ObjectNode> sha256Matches = matcher.identify(readJsonFile("match-proprietary/bom.json"));

    assertThat(sha256Matches).hasSize(0);
  }

  @Test
  public void testIdentify_NoBomNodesToMatch() throws Exception {
    Map<ComponentIdentifier, ObjectNode> sha256Matches = matcher.identify(readJsonFile("no-match/bom.json"));
    assertThat(sha256Matches).hasSize(0);
  }

  @Test
  public void testIdentify_NoConfiguredArtifactoryConnections() throws Exception {
    new ArtifactoryConnectionDAO().delete(artifactoryConnection);
    Map<ComponentIdentifier, ObjectNode> sha256Matches = matcher.identify(readJsonFile("match-proprietary/bom.json"));

    assertThat(sha256Matches).hasSize(0);
  }

  @Test
  public void testResolveComponentIdentifierFromUri_maven() throws Exception {
    ComponentIdentifier identifier = RepositoryMatcher.resolveComponentIdentifierFromUri("http://localhost/" +
        "artifactory/api/storage/reponame/org/apache/struts/struts2-core/2.3.4/struts2-core-2.3.4.jar");
    ComponentIdentifier expectedId =
        ComponentIdentifier.createMavenCoordinates("org.apache.struts", "struts2-core", "2.3.4", null, "jar");
    assertThat(identifier).isEqualTo(expectedId);
  }

  @Test
  public void testResolveComponentIdentifierFromUri_InvalidUri() throws Exception {
    ComponentIdentifier identifier = RepositoryMatcher.resolveComponentIdentifierFromUri("not a uri");
    assertThat(identifier).isNull();
  }

  @Test
  public void testResolveComponentIdentifierFromUri_InvalidUri_MissingRequiredCoordinates() throws Exception {
    ComponentIdentifier identifier = RepositoryMatcher.resolveComponentIdentifierFromUri(
        "http://localhost/artifactory/api/storage/reponame/1.1-SNAPSHOT/a-1.1-SNAPSHOT.jar");
    assertThat(identifier).isNull();
  }

  @Test
  public void testResolveComponentIdentifierFromUri_InvalidUri_MissingExtension() throws Exception {
    ComponentIdentifier expectedId = ComponentIdentifier.createMavenCoordinates("g.org", "a", "1.1-SNAPSHOT", null, "");
    ComponentIdentifier identifier = RepositoryMatcher.resolveComponentIdentifierFromUri(
        "http://localhost/artifactory/api/storage/reponame/g/org/a/1.1-SNAPSHOT/a-1/");
    assertThat(identifier).isEqualTo(expectedId);
  }

  private void mockArtifactoryResponse() {
    mockArtifactoryResponse("eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941",
        "http://localhost/artifactory/api/storage/reponame/g/org/a/1.1-SNAPSHOT/a-1.1-SNAPSHOT.jar"
    );
  }

  private void mockArtifactoryResponse(String sha256, String... uris) {
    artifactoryMockServer.mockSearchChecksum(ChecksumType.SHA256, sha256,
        ArtifactoryChecksumSearchResults.create(uris));
  }

  private JsonNode readJsonFile(String path) throws IOException {
    return objectMapper.readTree(getClass().getResource("/RepositoryMatcherTest/" + path));
  }
}
