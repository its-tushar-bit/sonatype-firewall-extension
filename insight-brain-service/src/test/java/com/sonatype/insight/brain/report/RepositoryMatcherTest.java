/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.experimental.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.api.v2.service.AbstractApiComponentDetailsServiceV2;
import com.sonatype.insight.brain.api.v2.service.DefaultApiComponentDetailsServiceV2;
import com.sonatype.insight.brain.artifactory.ArtifactoryMockServerRule;
import com.sonatype.insight.brain.artifactory.DefaultArtifactoryClient;
import com.sonatype.insight.brain.artifactory.client.ArtifactoryChecksumSearchResults;
import com.sonatype.insight.brain.artifactory.client.ChecksumType;
import com.sonatype.insight.brain.dataaccess.artifactory.ArtifactoryConnectionDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.artifactory.ArtifactoryConnection;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadGatewayException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RepositoryMatcherTest
    extends AbstractComponentTest
{
  @Rule
  public ArtifactoryMockServerRule artifactoryMockServer = new ArtifactoryMockServerRule();

  @Inject
  private PasswordHandler passwordHandler;

  @Inject
  private RepositoryMatcher matcher;

  @Mock
  private DefaultApiComponentDetailsServiceV2 mockDefaultApiComponentDetailsServiceV2;

  private ArtifactoryConnection artifactoryConnection;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void configure(Binder binder) {
    binder.bind(DefaultApiComponentDetailsServiceV2.class).toInstance(mockDefaultApiComponentDetailsServiceV2);
    super.configure(binder);
  }

  @Before
  public void before() {
    artifactoryConnection = tempEntity.newArtifactoryConnection(Organization.ROOT_ORGANIZATION_ID,
        artifactoryMockServer.getBaseUrl(), "artifactoryUser",
        passwordHandler.encryptPassword("password".toCharArray()));
  }

  @Test
  public void testMatch() throws Exception {
    SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.setEnabled(true);
    ComponentIdentifier identifier =
        ComponentIdentifier.createMavenCoordinates("g.org", "a", "1.1-SNAPSHOT", null, "jar");
    mockArtifactoryResponse();
    List<ComponentEvaluationData> mockResult = new ArrayList<>();
    ComponentEvaluationData componentEvaluationData = new ComponentEvaluationData();
    mockResult.add(componentEvaluationData);
    when(mockDefaultApiComponentDetailsServiceV2.getComponentDetailsListFromHds(anyList(),
        eq(AbstractApiComponentDetailsServiceV2.PURPOSE_EVALUATION))).thenReturn(mockResult);

    matcher.match(readJsonFile("match-sha256/bom.json"));

    artifactoryMockServer.getWireMockServer()
        .verify(1, anyRequestedFor(urlPathEqualTo(DefaultArtifactoryClient.CHECKSUM_SEARCH_PATH)));
    verify(mockDefaultApiComponentDetailsServiceV2).getComponentDetailsListFromHds(
        Collections.singletonList(identifier), AbstractApiComponentDetailsServiceV2.PURPOSE_EVALUATION);
  }

  @Test
  public void testMatch_FeatureDisabled() throws Exception {
    ComponentIdentifier identifier =
        ComponentIdentifier.createMavenCoordinates("g.org", "a", "1.1-SNAPSHOT", null, "jar");
    mockArtifactoryResponse();
    List<ComponentEvaluationData> mockResult = new ArrayList<>();
    ComponentEvaluationData componentEvaluationData = new ComponentEvaluationData();
    mockResult.add(componentEvaluationData);
    lenient().when(mockDefaultApiComponentDetailsServiceV2.getComponentDetailsListFromHds(anyList(),
        eq(AbstractApiComponentDetailsServiceV2.PURPOSE_EVALUATION))).thenReturn(mockResult);

    matcher.match(readJsonFile("match-sha256/bom.json"));

    artifactoryMockServer.getWireMockServer()
        .verify(0, anyRequestedFor(urlPathEqualTo(DefaultArtifactoryClient.CHECKSUM_SEARCH_PATH)));
    verify(mockDefaultApiComponentDetailsServiceV2, never()).getComponentDetailsListFromHds(
        Collections.singletonList(identifier), AbstractApiComponentDetailsServiceV2.PURPOSE_EVALUATION);
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
    ArtifactoryChecksumSearchResults mockResult = ArtifactoryChecksumSearchResults.create("invalid uri",
        "http://localhost/artifactory/api/storage/reponame/g/org/a/1.1-SNAPSHOT/a-1.1-SNAPSHOT.jar");
    artifactoryMockServer.mockSearchChecksum(ChecksumType.SHA256,
        "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941", mockResult);

    Map<ComponentIdentifier, ObjectNode> sha256Matches = matcher.identify(readJsonFile("match-proprietary/bom.json"));

    assertThat(sha256Matches).hasSize(1).containsOnlyKeys(identifier);
    artifactoryMockServer.getWireMockServer()
        .verify(1, anyRequestedFor(urlPathEqualTo(DefaultArtifactoryClient.CHECKSUM_SEARCH_PATH)));
  }

  @Test
  public void testIdentify_NoRecognizablResults() throws Exception {
    ArtifactoryChecksumSearchResults mockResult = ArtifactoryChecksumSearchResults.create("invalid uri");
    artifactoryMockServer.mockSearchChecksum(ChecksumType.SHA256,
        "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941", mockResult);

    Map<ComponentIdentifier, ObjectNode> sha256Matches = matcher.identify(readJsonFile("match-proprietary/bom.json"));

    assertThat(sha256Matches).hasSize(0);
    artifactoryMockServer.getWireMockServer()
        .verify(1, anyRequestedFor(urlPathEqualTo(DefaultArtifactoryClient.CHECKSUM_SEARCH_PATH)));
  }

  @Test
  public void testIdentify_MinimalUri() throws Exception {
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", null, "jar");
    ArtifactoryChecksumSearchResults mockResult =
        ArtifactoryChecksumSearchResults.create("http://localhost/artifactory/api/storage/r/g/a/v/x.jar");
    artifactoryMockServer.mockSearchChecksum(ChecksumType.SHA256,
        "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941", mockResult);

    Map<ComponentIdentifier, ObjectNode> sha256Matches = matcher.identify(readJsonFile("match-proprietary/bom.json"));

    assertThat(sha256Matches).hasSize(1).containsOnlyKeys(identifier);
    artifactoryMockServer.getWireMockServer()
        .verify(1, anyRequestedFor(urlPathEqualTo(DefaultArtifactoryClient.CHECKSUM_SEARCH_PATH)));
  }

  @Test
  public void testIdentify_UriWithEmptyNamespaceSegment() throws Exception {
    testIdentify_HasNoResult("http://localhost/artifactory/api/storage/r/%20/a/v/x.jar");
  }

  @Test
  public void testIdentify_UriWithEmptyNameSegment() throws Exception {
    testIdentify_HasNoResult("http://localhost/artifactory/api/storage/r/g/%20/v/x.jar");
  }

  @Test
  public void testIdentify_UriWithEmptyVersionSegment() throws Exception {
    testIdentify_HasNoResult("http://localhost/artifactory/api/storage/r/g/a/%20/x.jar");
  }

  @Test
  public void testIdentify_UriWithEmptyFilenameSegment() throws Exception {
    testIdentify_HasNoResult("http://localhost/artifactory/api/storage/r/g/a/v/%20");
  }

  private void testIdentify_HasNoResult(String uri) throws Exception {
    ArtifactoryChecksumSearchResults mockResult = ArtifactoryChecksumSearchResults.create(uri);
    artifactoryMockServer.mockSearchChecksum(ChecksumType.SHA256,
        "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941", mockResult);

    Map<ComponentIdentifier, ObjectNode> sha256Matches = matcher.identify(readJsonFile("match-proprietary/bom.json"));

    assertThat(sha256Matches).isEmpty();
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

  @Test
  public void testGetEvaluationByIdentifier() {
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2");
    List<ComponentIdentifier> componentIdentifiers = Arrays.asList(componentIdentifier1, componentIdentifier2);
    List<ComponentEvaluationData> mockResult = new ArrayList<>();
    ComponentEvaluationData componentEvaluationData1 = new ComponentEvaluationData();
    ComponentEvaluationData componentEvaluationData2 = new ComponentEvaluationData();
    componentEvaluationData2.requestIndex = 1;
    mockResult.add(componentEvaluationData2);
    mockResult.add(componentEvaluationData1);
    when(mockDefaultApiComponentDetailsServiceV2.getComponentDetailsListFromHds(anyList(),
        eq(AbstractApiComponentDetailsServiceV2.PURPOSE_EVALUATION))).thenReturn(mockResult);

    Map<ComponentIdentifier, ComponentEvaluationData> evaluationByIdentifier =
        matcher.getEvaluationByIdentifier(componentIdentifiers);

    verify(mockDefaultApiComponentDetailsServiceV2).getComponentDetailsListFromHds(componentIdentifiers,
        AbstractApiComponentDetailsServiceV2.PURPOSE_EVALUATION);
    assertThat(evaluationByIdentifier).hasSize(2);
    assertThat(evaluationByIdentifier).containsEntry(componentIdentifier1, componentEvaluationData1);
    assertThat(evaluationByIdentifier).containsEntry(componentIdentifier2, componentEvaluationData2);
  }

  @Test
  public void testGetEvaluationByIdentifier_NoComponents() {
    when(mockDefaultApiComponentDetailsServiceV2.getComponentDetailsListFromHds(anyList(),
        eq(AbstractApiComponentDetailsServiceV2.PURPOSE_EVALUATION))).thenCallRealMethod();

    Map<ComponentIdentifier, ComponentEvaluationData> evaluationByIdentifier =
        matcher.getEvaluationByIdentifier(Collections.emptyList());

    assertThat(evaluationByIdentifier).isEmpty();
  }

  @Test
  public void testGetEvaluationByIdentifier_Error() {
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2");
    List<ComponentIdentifier> componentIdentifiers = Arrays.asList(componentIdentifier1, componentIdentifier2);
    when(mockDefaultApiComponentDetailsServiceV2.getComponentDetailsListFromHds(anyList(),
        eq(AbstractApiComponentDetailsServiceV2.PURPOSE_EVALUATION))).thenThrow(new BadGatewayException("Error"));

    Map<ComponentIdentifier, ComponentEvaluationData> evaluationByIdentifier =
        matcher.getEvaluationByIdentifier(componentIdentifiers);

    verify(mockDefaultApiComponentDetailsServiceV2).getComponentDetailsListFromHds(componentIdentifiers,
        AbstractApiComponentDetailsServiceV2.PURPOSE_EVALUATION);
    assertThat(evaluationByIdentifier).isEmpty();
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
