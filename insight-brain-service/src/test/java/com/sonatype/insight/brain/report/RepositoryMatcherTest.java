/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.AnalysisSource;
import com.sonatype.clm.dto.model.component.AnalysisType;
import com.sonatype.clm.dto.model.component.AnalyzerFeatures;
import com.sonatype.clm.dto.model.component.ComponentCategory;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.HygieneRating;
import com.sonatype.clm.dto.model.component.IntegrityRating;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.api.v2.service.ApiComponentDetailsServiceV2;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.artifactory.ArtifactoryClient;
import com.sonatype.insight.brain.artifactory.ArtifactoryMockServerRule;
import com.sonatype.insight.brain.artifactory.client.ArtifactoryChecksumSearchResults;
import com.sonatype.insight.brain.artifactory.client.ChecksumType;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.component.RepositoryIdentifiedComponentCache;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.artifactory.ArtifactoryConnectionDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoader;
import com.sonatype.insight.brain.dataaccess.component.RepositoryIdentifiedComponentDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.artifactory.ArtifactoryConnection;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.component.RepositoryIdentifiedComponent;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightMail;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.lqa.LqaFormat;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.test.LogOutput;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class RepositoryMatcherTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(RepositoryMatcher.class);

  @Rule
  public ArtifactoryMockServerRule artifactoryMockServer = new ArtifactoryMockServerRule();

  @Inject
  private PasswordHandler passwordHandler;

  @Inject
  private RepositoryMatcher matcher;

  @Inject
  private RepositoryIdentifiedComponentDAO repositoryIdentifiedComponentDAO;

  @Inject
  private ArtifactoryConnectionDAO artifactoryConnectionDAO;

  @Inject
  private RepositoryIdentifiedComponentCache repositoryIdentifiedComponentCache;

  @Inject
  private OrganizationDAO organizationDAO;

  @Inject
  private ApplicationDAO applicationDAO;

  @Inject
  private ApiConfigurationService apiConfigurationService;

  @Mock
  private ApiComponentDetailsServiceV2 mockApiComponentDetailsServiceV2;

  @Mock
  private InsightMail mockInsightMail;

  private ArtifactoryConnection artifactoryConnection;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private Application application;

  @Before
  public void before() {
    resetMutableBfsConfiguration();

    Organization rootOrg = organizationDAO.getByIdNotNull(Organization.ROOT_ORGANIZATION_ID);
    rootOrg.setArtifactoryConnectionEnabled(true);
    organizationDAO.update(rootOrg);

    application = tempEntity.newApplicationWithParent();
    artifactoryMockServer.setPath("/artifactory");
    artifactoryConnection = tempEntity.newArtifactoryConnection(Organization.ROOT_ORGANIZATION_ID,
        artifactoryMockServer.getUrl(), null, null);
  }

  @After
  public void after() {
    resetMutableBfsConfiguration();

    Organization rootOrg = organizationDAO.getByIdNotNull(Organization.ROOT_ORGANIZATION_ID);
    rootOrg.setArtifactoryConnectionEnabled(null);
    organizationDAO.update(rootOrg);

    repositoryIdentifiedComponentDAO.getAll().forEach(repositoryIdentifiedComponentDAO::delete);
    repositoryIdentifiedComponentCache.getLoadingCache().invalidateAll();
  }

  private void resetMutableBfsConfiguration() {
    SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.setEnabled(false);
    apiConfigurationService.deleteConfigurationNoAuthz(new HashSet<>(Arrays.asList(
        SystemConfigurationProperty.BFS_REPOSITORIES,
        SystemConfigurationProperty.BFS_COMPONENT_QUERY_LIMIT,
        SystemConfigurationProperty.BFS_ARTIFACTORY_AQL_BATCH_SIZE,
        SystemConfigurationProperty.BFS_ARTIFACTORY_EXPIRED_TOKEN_EMAIL,
        SystemConfigurationProperty.BASE_URL)));
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
    when(mockApiComponentDetailsServiceV2.getComponentDetailsListFromHds(anyList(),
        eq(ApiComponentDetailsServiceV2.PURPOSE_EVALUATION))).thenReturn(mockResult);
    ObjectNode bomJson = (ObjectNode) readJsonFile("match-sha256/bom.json");
    ObjectNode dataJson = objectMapper.createObjectNode();
    ObjectNode summaryJson = objectMapper.createObjectNode();
    ObjectNode licensesJson = createObjectNodeWithAaData();
    ObjectNode securityJson = createObjectNodeWithAaData();
    RepositoryMatcher spyRepositoryMatcher = spy(matcher);

    Set<ComponentIdentifier> match =
        spyRepositoryMatcher.match(application, bomJson, dataJson, summaryJson, licensesJson, securityJson);

    assertThat(match).containsExactly(identifier);
    ArgumentCaptor<ArtifactoryConnection> connectionArgumentCaptor =
        ArgumentCaptor.forClass(ArtifactoryConnection.class);
    verify(spyRepositoryMatcher).identify(connectionArgumentCaptor.capture(), eq(bomJson));
    assertThat(connectionArgumentCaptor.getValue().getId()).isEqualTo(artifactoryConnection.getId());
    artifactoryMockServer.getWireMockServer()
        .verify(1, anyRequestedFor(
            urlPathEqualTo(artifactoryMockServer.getRelativePath(ArtifactoryClient.CHECKSUM_SEARCH_PATH))));
    verify(spyRepositoryMatcher).getEvaluationByIdentifier(Collections.singletonList(identifier));
    verify(mockApiComponentDetailsServiceV2).getComponentDetailsListFromHds(
        Collections.singletonList(identifier), ApiComponentDetailsServiceV2.PURPOSE_EVALUATION);
    verify(spyRepositoryMatcher).updateJsonFiles(eq(application), eq(bomJson), eq(dataJson), eq(summaryJson),
        eq(licensesJson), eq(securityJson), any(), any());
    assertThat(logOutput).atDebugLevel().contains("Artifactory search for 1 checksum(s) resulted in 1 match(es).");
    assertThat(logOutput).atErrorLevel().isEmpty();
  }

  @Test
  public void testMatch_ExpiredToken() throws Exception {
    testMatch_ExpiredToken("Token failed verification: expired", "username@domain", true);
  }

  @Test
  public void testMatch_NotExpiredTokenError() throws Exception {
    testMatch_ExpiredToken("error", "username@domain", false);
  }

  @Test
  public void testMatch_ExpiredToken_NoEmail() throws Exception {
    testMatch_ExpiredToken("Token failed verification: expired", null, false);
  }

  private void testMatch_ExpiredToken(String error, String email, boolean assertEmailSent) throws Exception {
    SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.setEnabled(true);
    apiConfigurationService.applyConfigurationToClients(SystemConfigurationProperty.BFS_REPOSITORIES);
    if (email != null) {
      apiConfigurationService.setConfigurationInDatabaseNoAuthz(
          SystemConfigurationProperty.BFS_ARTIFACTORY_EXPIRED_TOKEN_EMAIL, email);
      apiConfigurationService.applyConfigurationToClients(
          SystemConfigurationProperty.BFS_ARTIFACTORY_EXPIRED_TOKEN_EMAIL);
    }
    String baseUrl = "http://baseUrl/";
    apiConfigurationService.setConfigurationInDatabaseNoAuthz(SystemConfigurationProperty.BASE_URL, baseUrl);
    apiConfigurationService.applyConfigurationToClients(SystemConfigurationProperty.BASE_URL);
    artifactoryConnectionDAO.delete(artifactoryConnection);
    artifactoryConnection = tempEntity.newArtifactoryConnection(Organization.ROOT_ORGANIZATION_ID,
        artifactoryMockServer.getUrl(), "username", passwordHandler.encryptPassword("password".toCharArray()));
    artifactoryMockServer.mockSearchByChecksumsUsingAQLError(
        artifactoryConnection.getUsername(),
        passwordHandler.decryptPassword(artifactoryConnection.getPassword()),
        ChecksumType.SHA256,
        Collections.singleton("eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941"),
        401,
        error);
    ObjectNode bomJson = (ObjectNode) readJsonFile("match-sha256/bom.json");
    ObjectNode dataJson = objectMapper.createObjectNode();
    ObjectNode summaryJson = objectMapper.createObjectNode();
    ObjectNode licensesJson = createObjectNodeWithAaData();
    ObjectNode securityJson = createObjectNodeWithAaData();

    Set<ComponentIdentifier> match =
        matcher.match(application, bomJson, dataJson, summaryJson, licensesJson, securityJson);

    assertThat(match).isEmpty();
    if (assertEmailSent) {
      verify(mockInsightMail).sendHtml(email, RepositoryMatcher.BFS_ARTIFACTORY_EXPIRED_TOKEN_SUBJECT,
          String.format(RepositoryMatcher.BFS_ARTIFACTORY_EXPIRED_TOKEN_BODY, baseUrl,
              Organization.ROOT_ORGANIZATION_ID));
    }
    else {
      verifyNoInteractions(mockInsightMail);
    }
  }

  @Test
  public void testMatch_FeatureDisabled() throws Exception {
    ComponentIdentifier identifier =
        ComponentIdentifier.createMavenCoordinates("g.org", "a", "1.1-SNAPSHOT", null, "jar");
    mockArtifactoryResponse();
    List<ComponentEvaluationData> mockResult = new ArrayList<>();
    ComponentEvaluationData componentEvaluationData = new ComponentEvaluationData();
    mockResult.add(componentEvaluationData);
    lenient().when(mockApiComponentDetailsServiceV2.getComponentDetailsListFromHds(anyList(),
        eq(ApiComponentDetailsServiceV2.PURPOSE_EVALUATION))).thenReturn(mockResult);

    matcher.match(application, readJsonFile("match-sha256/bom.json"), objectMapper.createObjectNode(),
        objectMapper.createObjectNode(), createObjectNodeWithAaData(), createObjectNodeWithAaData());

    artifactoryMockServer.getWireMockServer()
        .verify(0, anyRequestedFor(urlPathEqualTo(ArtifactoryClient.CHECKSUM_SEARCH_PATH)));
    verify(mockApiComponentDetailsServiceV2, never()).getComponentDetailsListFromHds(
        Collections.singletonList(identifier), ApiComponentDetailsServiceV2.PURPOSE_EVALUATION);
  }

  @Test
  public void testIdentify_FilterOnlySha256() throws Exception {
    ComponentIdentifier identifier =
        ComponentIdentifier.createMavenCoordinates("g.org", "a", "1.1-SNAPSHOT", null, "jar");
    mockArtifactoryResponse();

    Map<ComponentIdentifier, ObjectNode> sha256Matches = matcher.identify(artifactoryConnection,
        readJsonFile("match-sha256/bom.json"));

    assertThat(sha256Matches).hasSize(1).containsOnlyKeys(identifier);
    artifactoryMockServer.getWireMockServer()
        .verify(1, anyRequestedFor(
            urlPathEqualTo(artifactoryMockServer.getRelativePath(ArtifactoryClient.CHECKSUM_SEARCH_PATH))));
    assertThat(logOutput).atDebugLevel().contains("Artifactory search for 1 checksum(s) resulted in 1 match(es).");
  }

  @Test
  public void testIdentify_Sbom() throws Exception {
    SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.setEnabled(true);

    ComponentIdentifier id1 =
        ComponentIdentifier.createMavenCoordinates("g.org", "a", "1.1-SNAPSHOT", null, "jar");

    ArtifactoryChecksumSearchResults mockResult = new ArtifactoryChecksumSearchResults(); // empty result
    artifactoryMockServer.mockSearchChecksum(ChecksumType.SHA256,
        "44ba611acde81de4319b2c4412d3379c74527bf4f433d78f89b213e08f7e6419", mockResult);
    artifactoryMockServer.mockSearchChecksum(ChecksumType.SHA256,
        "44ba611acde81de4319b2c4412d3379c74527bf4f433d78f89b213e08f7e6416", mockResult);
    mockArtifactoryResponse();

    List<ComponentEvaluationData> mockResultHds = new ArrayList<>();

    ComponentEvaluationData exact = new ComponentEvaluationData();
    exact.hash = "05431145264b6ae31a85";
    exact.componentIdentifier = id1;
    exact.matchState = "exact";
    exact.declaredLicenses = Collections.singleton(new License("Not-Declared", "Not Declared"));
    exact.observedLicenses = Collections.singleton(new License("Apache-2.0", "Apache-2.0"));
    exact.securityVulnerabilities = Collections.emptyList();
    exact.componentCategories = Collections.singletonList(new ComponentCategory(63, "Data Transport"));
    exact.relativePopularity = 100;
    exact.catalogDate = 1135095471000L;
    mockResultHds.add(exact);

    when(mockApiComponentDetailsServiceV2.getComponentDetailsListFromHds(anyList(),
        eq(ApiComponentDetailsServiceV2.PURPOSE_EVALUATION))).thenReturn(mockResultHds);

    ObjectNode bomJson = (ObjectNode) readJsonFile("sbom/bom.json");
    ObjectNode securityJson = (ObjectNode) readJsonFile("sbom/security.json");
    ObjectNode licenseJson = (ObjectNode) readJsonFile("sbom/licenses.json");
    ObjectNode summaryJson = (ObjectNode) readJsonFile("sbom/summary.json");
    ObjectNode dataJson = (ObjectNode) readJsonFile("sbom/data.json");

    Set<ComponentIdentifier> identified =
        matcher.match(application, bomJson, dataJson, summaryJson, licenseJson, securityJson);
    assertThat(identified).hasSize(1);

    artifactoryMockServer.getWireMockServer()
        .verify(3, anyRequestedFor(
            urlPathEqualTo(artifactoryMockServer.getRelativePath(ArtifactoryClient.CHECKSUM_SEARCH_PATH))));

    ObjectNode expectedBomJson = (ObjectNode) readJsonFile("sbom/outcome/bom.json");
    ObjectNode expectedSecurityJson = (ObjectNode) readJsonFile("sbom/outcome/security.json");
    ObjectNode expectedLicenseJson = (ObjectNode) readJsonFile("sbom/outcome/licenses.json");
    ObjectNode expectedSummaryJson = (ObjectNode) readJsonFile("sbom/outcome/summary.json");
    ObjectNode expectedDataJson = (ObjectNode) readJsonFile("sbom/outcome/data.json");

    assertThat(bomJson).isEqualTo(expectedBomJson);
    assertThat(securityJson).isEqualTo(expectedSecurityJson);
    assertThat(licenseJson).isEqualTo(expectedLicenseJson);
    assertThat(summaryJson).isEqualTo(expectedSummaryJson);
    assertThat(dataJson).isEqualTo(expectedDataJson);
  }

  @Test
  public void testIdentify_FilterCorrectMatchState() throws Exception {
    ComponentIdentifier id1 =
        ComponentIdentifier.createMavenCoordinates("g.org", "a", "1.1-SNAPSHOT", null, "jar");
    mockArtifactoryResponse();
    ComponentIdentifier id2 =
        ComponentIdentifier.createMavenCoordinates("g2.org", "a2", "5.0", null, "jar");
    mockArtifactoryResponse("8c7ac48903b2a4382561321e5c25913960007a65d0f5e4a167a80d41984092c9",
        "http://localhost/artifactory/api/storage/reponame2/g2/org/a2/5.0/a2-5.0.jar");

    Map<ComponentIdentifier, ObjectNode> sha256Matches = matcher.identify(artifactoryConnection,
        readJsonFile("match-state/bom.json"));

    assertThat(sha256Matches).hasSize(2).containsOnlyKeys(id1, id2);
    assertThat(logOutput).atDebugLevel().contains("Artifactory search for 2 checksum(s) resulted in 2 match(es).");
  }

  @Test
  public void testIdentify_FilterCorrectExtension() throws Exception {
    ComponentIdentifier identifier =
        ComponentIdentifier.createMavenCoordinates("g.org", "a", "1.1-SNAPSHOT", null, "jar");
    mockArtifactoryResponse();

    Map<ComponentIdentifier, ObjectNode> sha256Matches = matcher.identify(artifactoryConnection,
        readJsonFile("match-extension/bom.json"));

    assertThat(sha256Matches).hasSize(1).containsOnlyKeys(identifier);
    artifactoryMockServer.getWireMockServer()
        .verify(1, anyRequestedFor(
            urlPathEqualTo(artifactoryMockServer.getRelativePath(ArtifactoryClient.CHECKSUM_SEARCH_PATH))));
  }

  @Test
  public void testIdentify_FilterNonProprietary() throws Exception {
    ComponentIdentifier identifier =
        ComponentIdentifier.createMavenCoordinates("g.org", "a", "1.1-SNAPSHOT", null, "jar");
    mockArtifactoryResponse();

    Map<ComponentIdentifier, ObjectNode> sha256Matches = matcher.identify(artifactoryConnection,
        readJsonFile("match-proprietary/bom.json"));

    assertThat(sha256Matches).hasSize(1).containsOnlyKeys(identifier);
    artifactoryMockServer.getWireMockServer()
        .verify(1, anyRequestedFor(
            urlPathEqualTo(artifactoryMockServer.getRelativePath(ArtifactoryClient.CHECKSUM_SEARCH_PATH))));
  }

  @Test
  public void testIdentify_HandleMultipleUris() throws Exception {
    ComponentIdentifier identifier =
        ComponentIdentifier.createMavenCoordinates("g.org", "a", "1.1-SNAPSHOT", null, "jar");
    ArtifactoryChecksumSearchResults mockResult = ArtifactoryChecksumSearchResults.create("invalid uri",
        "http://localhost/artifactory/api/storage/reponame/g/org/a/1.1-SNAPSHOT/a-1.1-SNAPSHOT.jar");
    artifactoryMockServer.mockSearchChecksum(ChecksumType.SHA256,
        "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941", mockResult);

    Map<ComponentIdentifier, ObjectNode> sha256Matches = matcher.identify(artifactoryConnection,
        readJsonFile("match-proprietary/bom.json"));

    assertThat(sha256Matches).hasSize(1).containsOnlyKeys(identifier);
    artifactoryMockServer.getWireMockServer()
        .verify(1, anyRequestedFor(
            urlPathEqualTo(artifactoryMockServer.getRelativePath(ArtifactoryClient.CHECKSUM_SEARCH_PATH))));
  }

  @Test
  public void testIdentify_NoRecognizableResults() throws Exception {
    ArtifactoryChecksumSearchResults mockResult = ArtifactoryChecksumSearchResults.create("invalid uri");
    artifactoryMockServer.mockSearchChecksum(ChecksumType.SHA256,
        "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941", mockResult);

    Map<ComponentIdentifier, ObjectNode> sha256Matches = matcher.identify(artifactoryConnection,
        readJsonFile("match-proprietary/bom.json"));

    assertThat(sha256Matches).hasSize(0);
    artifactoryMockServer.getWireMockServer()
        .verify(1, anyRequestedFor(
            urlPathEqualTo(artifactoryMockServer.getRelativePath(ArtifactoryClient.CHECKSUM_SEARCH_PATH))));
  }

  @Test
  public void testIdentify_MinimalUri() throws Exception {
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", null, "jar");
    ArtifactoryChecksumSearchResults mockResult =
        ArtifactoryChecksumSearchResults.create("http://localhost/artifactory/api/storage/r/g/a/v/x.jar");
    String sha256 = "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941";
    artifactoryMockServer.mockSearchChecksum(ChecksumType.SHA256, sha256, mockResult);
    assertThat(repositoryIdentifiedComponentCache.get(sha256)).isNull();
    assertThat(repositoryIdentifiedComponentDAO.getByHash(sha256)).isNull();
    Date date = new Date();

    Map<ComponentIdentifier, ObjectNode> sha256Matches =
        matcher.identify(artifactoryConnection, readJsonFile("match-proprietary/bom.json"));

    assertThat(sha256Matches).hasSize(1).containsOnlyKeys(identifier);
    artifactoryMockServer.getWireMockServer()
        .verify(1, anyRequestedFor(
            urlPathEqualTo(artifactoryMockServer.getRelativePath(ArtifactoryClient.CHECKSUM_SEARCH_PATH))));
    assertThat(repositoryIdentifiedComponentCache.get(sha256)).isEqualTo(identifier);
    RepositoryIdentifiedComponent repositoryIdentifiedComponent = repositoryIdentifiedComponentDAO.getByHash(sha256);
    assertThat(repositoryIdentifiedComponent).isNotNull();
    assertThat(repositoryIdentifiedComponent.getComponentIdentifier()).isEqualTo(identifier);
    assertThat(repositoryIdentifiedComponent.getCreateTime()).isAfterOrEqualTo(date);
    assertThat(repositoryIdentifiedComponent.getLastAccessTime()).isEqualTo(
        repositoryIdentifiedComponent.getCreateTime());
  }

  @Test
  public void testIdentify_FromCache() throws Exception {
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", null, "jar");
    repositoryIdentifiedComponentCache.put("eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941",
        identifier);

    Map<ComponentIdentifier, ObjectNode> sha256Matches =
        matcher.identify(artifactoryConnection, readJsonFile("match-proprietary/bom.json"));

    assertThat(sha256Matches).hasSize(1).containsOnlyKeys(identifier);
    artifactoryMockServer.getWireMockServer()
        .verify(0, anyRequestedFor(urlPathEqualTo(ArtifactoryClient.CHECKSUM_SEARCH_PATH)));
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

    Map<ComponentIdentifier, ObjectNode> sha256Matches = matcher.identify(artifactoryConnection,
        readJsonFile("match-proprietary/bom.json"));

    assertThat(sha256Matches).isEmpty();
    artifactoryMockServer.getWireMockServer()
        .verify(1, anyRequestedFor(
            urlPathEqualTo(artifactoryMockServer.getRelativePath(ArtifactoryClient.CHECKSUM_SEARCH_PATH))));
  }

  @Test
  public void testIdentify_ArtifactoryError() throws Exception {
    artifactoryMockServer.mockSearchChecksumError(ChecksumType.SHA256,
        "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941", 502);

    Map<ComponentIdentifier, ObjectNode> sha256Matches = matcher.identify(artifactoryConnection,
        readJsonFile("match-state/bom.json"));

    assertThat(sha256Matches).hasSize(0);
    // although 2 matching components, only 1 search is attempted due to connection error
    artifactoryMockServer.getWireMockServer()
        .verify(1, anyRequestedFor(
            urlPathEqualTo(artifactoryMockServer.getRelativePath(ArtifactoryClient.CHECKSUM_SEARCH_PATH))));
  }

  @Test
  public void testIdentify_NoArtifactoryMatches() throws Exception {
    ArtifactoryChecksumSearchResults mockResult = new ArtifactoryChecksumSearchResults(); // empty result
    artifactoryMockServer.mockSearchChecksum(ChecksumType.SHA256,
        "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941", mockResult);

    Map<ComponentIdentifier, ObjectNode> sha256Matches = matcher.identify(artifactoryConnection,
        readJsonFile("match-proprietary/bom.json"));

    assertThat(sha256Matches).hasSize(0);
  }

  @Test
  public void testIdentify_NoBomNodesToMatch() throws Exception {
    Map<ComponentIdentifier, ObjectNode> sha256Matches = matcher.identify(artifactoryConnection,
        readJsonFile("no-match/bom.json"));
    assertThat(sha256Matches).hasSize(0);
  }

  @Test
  public void testIdentify_NoConfiguredArtifactoryConnections() throws Exception {
    artifactoryConnectionDAO.delete(artifactoryConnection);
    Map<ComponentIdentifier, ObjectNode> sha256Matches = matcher.identify(artifactoryConnection,
        readJsonFile("match-proprietary/bom.json"));

    assertThat(sha256Matches).hasSize(0);
  }

  @Test
  public void testIdentify_ArtifactoryConfig_Disabled() throws Exception {
    application.setArtifactoryConnectionEnabled(false);
    applicationDAO.update(application);

    Map<ComponentIdentifier, ObjectNode> sha256Matches = matcher.identify(artifactoryConnection,
        readJsonFile("match-sha256/bom.json"));

    assertThat(sha256Matches).isEmpty();
  }

  @Test
  public void testIdentify_ArtifactoryConfig_From_Application() throws Exception {
    Organization org = organizationDAO.getByIdNotNull(Organization.ROOT_ORGANIZATION_ID);
    org.setArtifactoryConnectionEnabled(false);
    organizationDAO.update(org);
    artifactoryConnectionDAO.delete(artifactoryConnection);
    artifactoryConnection =
        tempEntity.newArtifactoryConnection(application.getId(), artifactoryMockServer.getUrl(), null, null);
    application.setArtifactoryConnectionEnabled(true);
    applicationDAO.update(application);

    mockArtifactoryResponse();

    Map<ComponentIdentifier, ObjectNode> sha256Matches = matcher.identify(artifactoryConnection,
        readJsonFile("match-sha256/bom.json"));

    assertThat(sha256Matches).hasSize(1);
    assertThat(logOutput).atDebugLevel().contains("Artifactory search for 1 checksum(s) resulted in 1 match(es).");
  }

  @Test
  public void testIdentify_ArtifactoryConfig_No_Results() throws Exception {
    matcher.identify(artifactoryConnection, readJsonFile("match-sha256/bom.json"));
    assertThat(logOutput).atErrorLevel().contains("Checksum search error for repository connection uri");
  }

  @Test
  public void testIdentify_ArtifactoryConfig_Inherited_From_Parent_Org() throws Exception {
    artifactoryConnectionDAO.delete(artifactoryConnection);
    artifactoryConnection = tempEntity.newArtifactoryConnection(application.getParentOwnerId(),
        artifactoryMockServer.getUrl(), null, null);
    Organization org = organizationDAO.getByIdNotNull(application.getParentOwnerId());
    org.setArtifactoryConnectionEnabled(true);
    organizationDAO.update(org);
    mockArtifactoryResponse();

    Map<ComponentIdentifier, ObjectNode> sha256Matches = matcher.identify(artifactoryConnection,
        readJsonFile("match-sha256/bom.json"));

    assertThat(sha256Matches).hasSize(1);
  }

  @Test
  public void testIdentify_ArtifactoryConfig_Inherited_Org_Disabled() throws Exception {
    artifactoryConnection = tempEntity.newArtifactoryConnection(application.getParentOwnerId(),
        artifactoryMockServer.getUrl(), null, null);
    Organization org = organizationDAO.getByIdNotNull(application.getParentOwnerId());
    org.setArtifactoryConnectionEnabled(false);
    organizationDAO.update(org);
    application.setArtifactoryConnectionEnabled(null);
    applicationDAO.update(application);

    Map<ComponentIdentifier, ObjectNode> sha256Matches = matcher.identify(artifactoryConnection,
        readJsonFile("match-sha256/bom.json"));

    assertThat(sha256Matches).isEmpty();
  }

  @Test
  public void testIdentify_ArtifactoryConfig_Inherited_Org_Disabled_Override() throws Exception {
    artifactoryConnection = tempEntity.newArtifactoryConnection(application.getParentOwnerId(),
        artifactoryMockServer.getUrl(), null, null);
    Organization org = organizationDAO.getByIdNotNull(application.getParentOwnerId());
    org.setArtifactoryConnectionEnabled(false);
    org.setAllowArtifactoryConnectionOverride(false);
    organizationDAO.update(org);
    application.setArtifactoryConnectionEnabled(true);
    applicationDAO.update(application);

    Map<ComponentIdentifier, ObjectNode> sha256Matches = matcher.identify(artifactoryConnection,
        readJsonFile("match-sha256/bom.json"));

    assertThat(sha256Matches).isEmpty();
  }

  @Test
  public void testIdentify_Aql() throws Exception {
    String sha256a = "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941";
    String sha256b = "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f942";
    Set<String> repos = new HashSet<>(Arrays.asList("repo1", "repo2"));
    apiConfigurationService.setConfigurationInDatabaseNoAuthz(
        SystemConfigurationProperty.BFS_REPOSITORIES, String.join(",", repos));
    apiConfigurationService.applyConfigurationToClients(SystemConfigurationProperty.BFS_REPOSITORIES);
    artifactoryConnectionDAO.delete(artifactoryConnection);
    artifactoryConnection = tempEntity.newArtifactoryConnection(Organization.ROOT_ORGANIZATION_ID,
        artifactoryMockServer.getUrl(),
        "artifactoryUser",
        passwordHandler.encryptPassword("password".toCharArray()));
    artifactoryMockServer.mockSearchByChecksumsUsingAQL(
        artifactoryConnection.getUsername(),
        passwordHandler.decryptPassword(artifactoryConnection.getPassword()),
        ChecksumType.SHA256,
        new HashSet<>(Arrays.asList(sha256a, sha256b)),
        repos,
        ArtifactoryMockServerRule.createAQLResult(ChecksumType.SHA256, sha256a, "r1", "g1/a1/v1", "x1.jar"),
        ArtifactoryMockServerRule.createAQLResult(ChecksumType.SHA256, sha256a, "r2", "g2/a2/v2", "x1.jar"),
        ArtifactoryMockServerRule.createAQLResult(ChecksumType.SHA256, sha256b, "r3", "g3/a3/v3", "x2.jar"),
        ArtifactoryMockServerRule.createAQLResult(ChecksumType.SHA256, sha256b, "r4", "g4/a4/v4", "x2.jar"));
    assertThat(repositoryIdentifiedComponentCache.get(sha256a)).isNull();
    assertThat(repositoryIdentifiedComponentDAO.getByHash(sha256a)).isNull();
    assertThat(repositoryIdentifiedComponentCache.get(sha256b)).isNull();
    assertThat(repositoryIdentifiedComponentDAO.getByHash(sha256b)).isNull();
    Date date = new Date();

    Map<ComponentIdentifier, ObjectNode> sha256Matches =
        matcher.identify(artifactoryConnection, readJsonFile("match-multiple/bom.json"));

    ComponentIdentifier componentIdentifier1 =
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar");
    ComponentIdentifier componentIdentifier2 =
        ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3", null, "jar");
    assertThat(sha256Matches).containsOnlyKeys(componentIdentifier1, componentIdentifier2);
    artifactoryMockServer.getWireMockServer()
        .verify(1, anyRequestedFor(
            urlPathEqualTo(artifactoryMockServer.getRelativePath(ArtifactoryClient.AQL_SEARCH_PATH))));
    assertStored(date, sha256a, componentIdentifier1);
    assertStored(date, sha256b, componentIdentifier2);
    assertThat(logOutput).atDebugLevel().contains("Artifactory search for 2 checksum(s) resulted in 2 match(es).");
  }

  @Test
  public void testIdentify_Aql_withComponentLimit() throws Exception {
    Set<String> repos = new HashSet<>(Arrays.asList("repo1", "repo2"));
    apiConfigurationService.setConfigurationInDatabaseNoAuthz(
        SystemConfigurationProperty.BFS_REPOSITORIES, String.join(",", repos));
    apiConfigurationService.applyConfigurationToClients(SystemConfigurationProperty.BFS_REPOSITORIES);
    apiConfigurationService.setConfigurationInDatabaseNoAuthz(SystemConfigurationProperty.BFS_COMPONENT_QUERY_LIMIT, 1);
    apiConfigurationService.applyConfigurationToClients(SystemConfigurationProperty.BFS_COMPONENT_QUERY_LIMIT);
    String sha256a = "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941";
    artifactoryConnectionDAO.delete(artifactoryConnection);
    artifactoryConnection = tempEntity.newArtifactoryConnection(Organization.ROOT_ORGANIZATION_ID,
        artifactoryMockServer.getUrl(),
        "artifactoryUser",
        passwordHandler.encryptPassword("password1".toCharArray()));
    artifactoryMockServer.mockSearchByChecksumsUsingAQL(
        artifactoryConnection.getUsername(),
        passwordHandler.decryptPassword(artifactoryConnection.getPassword()),
        ChecksumType.SHA256,
        Collections.singleton(sha256a),
        repos,
        ArtifactoryMockServerRule.createAQLResult(ChecksumType.SHA256, sha256a, "r1", "g1/a1/v1", "x1.jar"),
        ArtifactoryMockServerRule.createAQLResult(ChecksumType.SHA256, sha256a, "r2", "g2/a2/v2", "x1.jar"));

    Map<ComponentIdentifier, ObjectNode> sha256Matches =
        matcher.identify(artifactoryConnection, readJsonFile("match-multiple/bom.json"));

    ComponentIdentifier componentIdentifier1 =
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar");
    assertThat(sha256Matches).containsOnlyKeys(componentIdentifier1);
    artifactoryMockServer.getWireMockServer()
        .verify(1, anyRequestedFor(
            urlPathEqualTo(artifactoryMockServer.getRelativePath(ArtifactoryClient.AQL_SEARCH_PATH))));
    assertThat(logOutput).atDebugLevel()
        .contains("Artifactory search, limited to 1 queries, for 2 checksum(s), resulted in 1 match(es).");
  }

  @Test
  public void testIdentify_checksum_withComponentLimit() throws Exception {
    apiConfigurationService.setConfigurationInDatabaseNoAuthz(SystemConfigurationProperty.BFS_COMPONENT_QUERY_LIMIT, 1);
    apiConfigurationService.applyConfigurationToClients(SystemConfigurationProperty.BFS_COMPONENT_QUERY_LIMIT);
    artifactoryConnectionDAO.delete(artifactoryConnection);
    artifactoryConnection =
        tempEntity.newArtifactoryConnection(Organization.ROOT_ORGANIZATION_ID, artifactoryMockServer.getUrl(), null,
            null);
    mockArtifactoryResponse();

    Map<ComponentIdentifier, ObjectNode> sha256Matches =
        matcher.identify(artifactoryConnection, readJsonFile("match-multiple/bom.json"));

    ComponentIdentifier componentIdentifier1 =
        ComponentIdentifier.createMavenCoordinates("g.org", "a", "1.1-SNAPSHOT", null, "jar");
    assertThat(sha256Matches).containsOnlyKeys(componentIdentifier1);
    artifactoryMockServer.getWireMockServer()
        .verify(1, anyRequestedFor(
            urlPathEqualTo(artifactoryMockServer.getRelativePath(ArtifactoryClient.CHECKSUM_SEARCH_PATH))));
    assertThat(logOutput).atDebugLevel()
        .contains("Artifactory search, limited to 1 queries, for 2 checksum(s), resulted in 1 match(es).");
  }

  @Test
  public void testIdentify_withComponentLimit_zero() throws Exception {
    apiConfigurationService.setConfigurationInDatabaseNoAuthz(SystemConfigurationProperty.BFS_COMPONENT_QUERY_LIMIT, 0);
    apiConfigurationService.applyConfigurationToClients(SystemConfigurationProperty.BFS_COMPONENT_QUERY_LIMIT);

    Map<ComponentIdentifier, ObjectNode> sha256Matches =
        matcher.identify(artifactoryConnection, readJsonFile("match-multiple/bom.json"));

    assertThat(sha256Matches).isEmpty();
    artifactoryMockServer.getWireMockServer().verify(0, anyRequestedFor(anyUrl()));
    assertThat(logOutput).atDebugLevel()
        .contains("Artifactory search, limited to 0 queries, for 2 checksum(s), resulted in 0 match(es).");
  }

  private void assertStored(Date dateBeforeCached, String sha256, ComponentIdentifier componentIdentifier) {
    assertThat(repositoryIdentifiedComponentCache.get(sha256)).isEqualTo(componentIdentifier);
    RepositoryIdentifiedComponent repositoryIdentifiedComponent = repositoryIdentifiedComponentDAO.getByHash(sha256);
    assertThat(repositoryIdentifiedComponent).isNotNull();
    assertThat(repositoryIdentifiedComponent.getComponentIdentifier()).isEqualTo(componentIdentifier);
    assertThat(repositoryIdentifiedComponent.getCreateTime()).isAfterOrEqualTo(dateBeforeCached);
    assertThat(repositoryIdentifiedComponent.getLastAccessTime()).isEqualTo(
        repositoryIdentifiedComponent.getCreateTime());
  }

  @Test
  public void testIdentify_Aql_withRepositoryList() throws Exception {
    Set<String> repos = new HashSet<>(Arrays.asList("repo1", "repo2"));
    apiConfigurationService.setConfigurationInDatabaseNoAuthz(
        SystemConfigurationProperty.BFS_REPOSITORIES, String.join(",", repos));
    apiConfigurationService.applyConfigurationToClients(SystemConfigurationProperty.BFS_REPOSITORIES);
    String sha256a = "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941";
    String sha256b = "eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f942";
    artifactoryConnectionDAO.delete(artifactoryConnection);
    artifactoryConnection = tempEntity.newArtifactoryConnection(Organization.ROOT_ORGANIZATION_ID,
        artifactoryMockServer.getUrl(),
        "artifactoryUser",
        passwordHandler.encryptPassword("password1".toCharArray()));
    artifactoryMockServer.mockSearchByChecksumsUsingAQL(
        artifactoryConnection.getUsername(),
        passwordHandler.decryptPassword(artifactoryConnection.getPassword()),
        ChecksumType.SHA256,
        new HashSet<>(Arrays.asList(sha256a, sha256b)),
        repos,
        ArtifactoryMockServerRule.createAQLResult(ChecksumType.SHA256, sha256a, "repo1", "g1/a1/v1", "x1.jar"),
        ArtifactoryMockServerRule.createAQLResult(ChecksumType.SHA256, sha256b, "repo2", "g2/a2/v2", "x2.jar"));

    Map<ComponentIdentifier, ObjectNode> sha256Matches =
        matcher.identify(artifactoryConnection, readJsonFile("match-multiple/bom.json"));

    ComponentIdentifier componentIdentifier1 =
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar");
    ComponentIdentifier componentIdentifier2 =
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", null, "jar");
    assertThat(sha256Matches).containsOnlyKeys(componentIdentifier1, componentIdentifier2);
  }

  @Test
  public void testIdentify_Checksum_withRepositoryList() throws Exception {
    String repos = "repo1,repo2";
    apiConfigurationService.setConfigurationInDatabaseNoAuthz(SystemConfigurationProperty.BFS_REPOSITORIES, repos);
    apiConfigurationService.applyConfigurationToClients(SystemConfigurationProperty.BFS_REPOSITORIES);
    artifactoryConnectionDAO.delete(artifactoryConnection);
    artifactoryConnection =
        tempEntity.newArtifactoryConnection(Organization.ROOT_ORGANIZATION_ID, artifactoryMockServer.getUrl(), null,
            null);
    mockArtifactoryResponse();

    Map<ComponentIdentifier, ObjectNode> sha256Matches =
        matcher.identify(artifactoryConnection, readJsonFile("match-multiple/bom.json"));

    ComponentIdentifier componentIdentifier1 =
        ComponentIdentifier.createMavenCoordinates("g.org", "a", "1.1-SNAPSHOT", null, "jar");
    assertThat(sha256Matches).containsOnlyKeys(componentIdentifier1);
  }

  @Test
  public void testResolveComponentIdentifierFromUri_maven() {
    ComponentIdentifier identifier = RepositoryMatcher.resolveComponentIdentifierFromUri("http://localhost/" +
        "artifactory/api/storage/reponame/org/apache/struts/struts2-core/2.3.4/struts2-core-2.3.4.jar");
    ComponentIdentifier expectedId =
        ComponentIdentifier.createMavenCoordinates("org.apache.struts", "struts2-core", "2.3.4", null, "jar");
    assertThat(identifier).isEqualTo(expectedId);
  }

  @Test
  public void testResolveComponentIdentifierFromUri_InvalidUri() {
    ComponentIdentifier identifier = RepositoryMatcher.resolveComponentIdentifierFromUri("not a uri");
    assertThat(identifier).isNull();
  }

  @Test
  public void testResolveComponentIdentifierFromUri_InvalidUri_MissingRequiredCoordinates() {
    ComponentIdentifier identifier = RepositoryMatcher.resolveComponentIdentifierFromUri(
        "http://localhost/artifactory/api/storage/reponame/1.1-SNAPSHOT/a-1.1-SNAPSHOT.jar");
    assertThat(identifier).isNull();
  }

  @Test
  public void testResolveComponentIdentifierFromUri_InvalidUri_MissingExtension() {
    ComponentIdentifier identifier = RepositoryMatcher.resolveComponentIdentifierFromUri(
        "http://localhost/artifactory/api/storage/reponame/g/org/a/1.1-SNAPSHOT/a-1/");
    assertThat(identifier).isNull();
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
    when(mockApiComponentDetailsServiceV2.getComponentDetailsListFromHds(anyList(),
        eq(ApiComponentDetailsServiceV2.PURPOSE_EVALUATION))).thenReturn(mockResult);

    Map<ComponentIdentifier, ComponentEvaluationData> evaluationByIdentifier =
        matcher.getEvaluationByIdentifier(componentIdentifiers);

    verify(mockApiComponentDetailsServiceV2).getComponentDetailsListFromHds(componentIdentifiers,
        ApiComponentDetailsServiceV2.PURPOSE_EVALUATION);
    assertThat(evaluationByIdentifier).hasSize(2);
    assertThat(evaluationByIdentifier).containsEntry(componentIdentifier1, componentEvaluationData1);
    assertThat(evaluationByIdentifier).containsEntry(componentIdentifier2, componentEvaluationData2);
  }

  @Test
  public void testGetEvaluationByIdentifier_NoComponents() {
    when(mockApiComponentDetailsServiceV2.getComponentDetailsListFromHds(anyList(),
        eq(ApiComponentDetailsServiceV2.PURPOSE_EVALUATION))).thenCallRealMethod();

    Map<ComponentIdentifier, ComponentEvaluationData> evaluationByIdentifier =
        matcher.getEvaluationByIdentifier(Collections.emptyList());

    assertThat(evaluationByIdentifier).isEmpty();
  }

  @Test
  public void testGetEvaluationByIdentifier_Error() {
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2");
    List<ComponentIdentifier> componentIdentifiers = Arrays.asList(componentIdentifier1, componentIdentifier2);
    when(mockApiComponentDetailsServiceV2.getComponentDetailsListFromHds(anyList(),
        eq(ApiComponentDetailsServiceV2.PURPOSE_EVALUATION))).thenThrow(new BadGatewayException("Error"));

    Map<ComponentIdentifier, ComponentEvaluationData> evaluationByIdentifier =
        matcher.getEvaluationByIdentifier(componentIdentifiers);

    verify(mockApiComponentDetailsServiceV2).getComponentDetailsListFromHds(componentIdentifiers,
        ApiComponentDetailsServiceV2.PURPOSE_EVALUATION);
    assertThat(evaluationByIdentifier).isEmpty();
  }

  @Test
  public void testUpdateComponentIdentifier_MavenComponentIdentifier() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    ObjectNode objectNode = objectMapper.createObjectNode();

    RepositoryMatcher.updateComponentIdentifier(objectNode, componentIdentifier);

    assertThat(objectNode.get(ComponentIdentifierAdapter.COMPONENT_IDENTIFIER)).isEqualTo(
        RepositoryMatcher.convert(componentIdentifier));
    assertThat(objectNode.get(ComponentIdentifierAdapter.PURL_IDENTIFIER).asText()).isEqualTo(
        PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).getPackageUrl());
    assertThat(objectNode.get(ComponentIdentifier.MAVEN_GROUP_ID).asText()).isEqualTo(
        componentIdentifier.get(ComponentIdentifier.MAVEN_GROUP_ID));
    assertThat(objectNode.get(ComponentIdentifier.MAVEN_ARTIFACT_ID).asText()).isEqualTo(
        componentIdentifier.get(ComponentIdentifier.MAVEN_ARTIFACT_ID));
    assertThat(objectNode.get(ComponentIdentifier.VERSION).asText()).isEqualTo(
        componentIdentifier.get(ComponentIdentifier.VERSION));
    assertThat(objectNode.get(ComponentIdentifier.MAVEN_CLASSIFIER).asText()).isEqualTo(
        componentIdentifier.get(ComponentIdentifier.MAVEN_CLASSIFIER));
    assertThat(objectNode.get(ComponentIdentifier.MAVEN_EXTENSION).asText()).isEqualTo(
        componentIdentifier.get(ComponentIdentifier.MAVEN_EXTENSION));
    assertThat(objectNode.get(ComponentLoader.DISPLAY_NAME_FIELD)).isEqualTo(
        JsonUtils.asTree(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier)));
  }

  @Test
  public void testUpdateComponentIdentifier_NotMavenComponentIdentifier() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("p", "v");
    ObjectNode objectNode = objectMapper.createObjectNode();

    RepositoryMatcher.updateComponentIdentifier(objectNode, componentIdentifier);

    assertThat(objectNode.get(ComponentIdentifierAdapter.COMPONENT_IDENTIFIER)).isEqualTo(
        RepositoryMatcher.convert(componentIdentifier));
    assertThat(objectNode.get(ComponentIdentifierAdapter.PURL_IDENTIFIER).asText()).isEqualTo(
        PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).getPackageUrl());
    assertThat(objectNode.has(ComponentIdentifier.MAVEN_GROUP_ID)).isFalse();
    assertThat(objectNode.has(ComponentIdentifier.MAVEN_ARTIFACT_ID)).isFalse();
    assertThat(objectNode.has(ComponentIdentifier.VERSION)).isFalse();
    assertThat(objectNode.has(ComponentIdentifier.MAVEN_CLASSIFIER)).isFalse();
    assertThat(objectNode.has(ComponentIdentifier.MAVEN_EXTENSION)).isFalse();
    assertThat(objectNode.get(ComponentLoader.DISPLAY_NAME_FIELD)).isEqualTo(
        JsonUtils.asTree(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier)));
  }

  @Test
  public void testUpdateBomJson_ComponentIdentifier() {
    ObjectNode bomJson = objectMapper.createObjectNode();
    ArrayNode aaData = bomJson.putArray("aaData");
    ObjectNode bomNode = aaData.addObject();
    bomNode.put(RepositoryMatcher.FIELD_HASH, "hash");
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");

    try (MockedStatic<RepositoryMatcher> repositoryMatcher = Mockito.mockStatic(RepositoryMatcher.class,
        CALLS_REAL_METHODS))
    {
      RepositoryMatcher.updateBomJson(bomJson, componentIdentifier, bomNode, false, new ComponentEvaluationData(),
          false);

      repositoryMatcher.verify(() -> RepositoryMatcher.updateComponentIdentifier(any(), eq(componentIdentifier)));
    }
  }

  @Test
  public void testUpdateBomJson_Proprietary() {
    ObjectNode bomJson = objectMapper.createObjectNode();
    ArrayNode aaData = bomJson.putArray("aaData");
    ObjectNode bomNode = aaData.addObject();
    bomNode.put(RepositoryMatcher.FIELD_HASH, "hash");

    RepositoryMatcher.updateBomJson(bomJson, ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"),
        bomNode, true, new ComponentEvaluationData(), false);

    assertThat(aaData).hasSize(1);
    bomNode = (ObjectNode) aaData.get(0);
    assertThat(bomNode.get(RepositoryMatcher.FIELD_PROPRIETARY).asBoolean()).isTrue();
  }

  @Test
  public void testUpdateBomJson_NotProprietary() {
    ObjectNode bomJson = objectMapper.createObjectNode();
    ArrayNode aaData = bomJson.putArray("aaData");
    ObjectNode bomNode = aaData.addObject();
    bomNode.put(RepositoryMatcher.FIELD_HASH, "hash");

    RepositoryMatcher.updateBomJson(bomJson, ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"),
        bomNode, false, new ComponentEvaluationData(), false);

    assertThat(aaData).hasSize(1);
    bomNode = (ObjectNode) aaData.get(0);
    assertThat(bomNode.get(RepositoryMatcher.FIELD_PROPRIETARY).asBoolean()).isFalse();
  }

  @Test
  public void testUpdateBomJson_Evaluation_NullFields() {
    ObjectNode bomJson = objectMapper.createObjectNode();
    ArrayNode aaData = bomJson.putArray("aaData");
    ObjectNode oldBomNode = aaData.addObject();
    oldBomNode.put(RepositoryMatcher.FIELD_FILENAMES, "filenames");
    oldBomNode.put(RepositoryMatcher.FIELD_PATHNAMES, "pathnames");
    oldBomNode.put(RepositoryMatcher.FIELD_AGGREGATE_FILES, "aggregateFiles");
    oldBomNode.put(RepositoryMatcher.FIELD_SCAN_ERROR, "scanError");
    oldBomNode.put(RepositoryMatcher.FIELD_HASH, "hash");
    oldBomNode.put(RepositoryMatcher.FIELD_SHA256, "sha256");
    oldBomNode.put(RepositoryMatcher.FIELD_LAST_MODIFIED_TIME, "lastModifiedTime");
    oldBomNode.put(RepositoryMatcher.FIELD_LAST_MODIFIED_ENTRY_TIME, "lastModifiedEntryTime");
    oldBomNode.put(RepositoryMatcher.FIELD_WEBSITE, "website");
    ComponentEvaluationData evaluation = new ComponentEvaluationData();
    evaluation.hash = "hash";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");

    RepositoryMatcher.updateBomJson(bomJson, componentIdentifier, oldBomNode, false, evaluation, false);

    assertThat(aaData).hasSize(1);
    ObjectNode bomNode = (ObjectNode) aaData.get(0);
    assertThat(bomNode.get(ComponentIdentifierAdapter.COMPONENT_IDENTIFIER)).isEqualTo(
        JsonUtils.asTree(componentIdentifier));
    assertThat(bomNode.get(ComponentIdentifierAdapter.PURL_IDENTIFIER).asText()).isEqualTo(
        PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).getPackageUrl());
    assertThat(bomNode.get(RepositoryMatcher.FIELD_FILENAMES)).isEqualTo(
        oldBomNode.get(RepositoryMatcher.FIELD_FILENAMES));
    assertThat(bomNode.get(RepositoryMatcher.FIELD_PATHNAMES)).isEqualTo(
        oldBomNode.get(RepositoryMatcher.FIELD_PATHNAMES));
    assertThat(bomNode.get(RepositoryMatcher.FIELD_AGGREGATE_FILES)).isEqualTo(
        oldBomNode.get(RepositoryMatcher.FIELD_AGGREGATE_FILES));
    assertThat(bomNode.get(RepositoryMatcher.FIELD_SCAN_ERROR)).isEqualTo(
        oldBomNode.get(RepositoryMatcher.FIELD_SCAN_ERROR));
    assertThat(bomNode.get(RepositoryMatcher.FIELD_HASH).asText()).isEqualTo("hash");
    assertThat(bomNode.get(RepositoryMatcher.FIELD_SHA256)).isEqualTo(oldBomNode.get(RepositoryMatcher.FIELD_SHA256));
    assertThat(bomNode.get(RepositoryMatcher.FIELD_LAST_MODIFIED_TIME)).isEqualTo(
        oldBomNode.get(RepositoryMatcher.FIELD_LAST_MODIFIED_TIME));
    assertThat(bomNode.get(RepositoryMatcher.FIELD_LAST_MODIFIED_ENTRY_TIME)).isEqualTo(
        oldBomNode.get(RepositoryMatcher.FIELD_LAST_MODIFIED_ENTRY_TIME));
    assertThat(bomNode.get(RepositoryMatcher.FIELD_WEBSITE)).isEqualTo(oldBomNode.get(RepositoryMatcher.FIELD_WEBSITE));
    assertThat(bomNode.get(RepositoryMatcher.FIELD_MATCH_STATE).asText()).isEqualTo(MatchState.EXACT.getId());
    assertThat(bomNode.get(RepositoryMatcher.FIELD_IDENTIFICATION_SOURCE).asText())
        .isEqualTo(IdentificationSource.SONATYPE_EXTERNAL_REPO.getId());
    assertThat(bomNode.get(RepositoryMatcher.FIELD_RELATIVE_POPULARITY)).isEqualTo(NullNode.getInstance());
    assertThat(bomNode.get(RepositoryMatcher.FIELD_CREATE_TIME)).isEqualTo(NullNode.getInstance());
    assertThat(bomNode.get(RepositoryMatcher.FIELD_COMPONENT_CATEGORIES)).isEmpty();
    assertThat(bomNode.get(RepositoryMatcher.FIELD_HYGIENE_RATING)).isEqualTo(NullNode.getInstance());
    assertThat(bomNode.get(RepositoryMatcher.FIELD_ANALYZER_FEATURES)).isEqualTo(RepositoryMatcher.convert(
        new AnalyzerFeatures(AnalysisSource.SDS, AnalysisType.COORDINATE, RepositoryMatcher.CLI_SCAN_CLIENT, true, true,
            true)));
    assertThat(bomNode.get(RepositoryMatcher.FIELD_INTEGRITY_RATING)).isEqualTo(NullNode.getInstance());
  }

  @Test
  public void testUpdateBomJson_Evaluation() {
    ObjectNode bomJson = objectMapper.createObjectNode();
    ArrayNode aaData = bomJson.putArray("aaData");
    ObjectNode bomNode = aaData.addObject();
    bomNode.put(RepositoryMatcher.FIELD_HASH, "hash");
    String scanClient = "someScanClient";
    bomNode.set(RepositoryMatcher.FIELD_ANALYZER_FEATURES,
        JsonUtils.asTree(new AnalyzerFeatures(null, null, scanClient, false, false, false)));
    ComponentEvaluationData evaluation = new ComponentEvaluationData();
    evaluation.relativePopularity = 5;
    evaluation.catalogDate = 10L;
    evaluation.componentCategories = Arrays.asList(new ComponentCategory(1, "path1"),
        new ComponentCategory(2, "path2"));
    evaluation.hygieneRating = new HygieneRating(1, "label");
    evaluation.analyzerFeatures = new AnalyzerFeatures(null, null, null, false, false, false);
    evaluation.integrityRating = new IntegrityRating(1, "label");

    RepositoryMatcher.updateBomJson(bomJson, ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"),
        bomNode, false, evaluation, false);

    assertThat(aaData).hasSize(1);
    bomNode = (ObjectNode) aaData.get(0);
    assertThat(bomNode.get(RepositoryMatcher.FIELD_MATCH_STATE).asText()).isEqualTo(MatchState.EXACT.getId());
    assertThat(bomNode.get(RepositoryMatcher.FIELD_IDENTIFICATION_SOURCE).asText())
        .isEqualTo(IdentificationSource.SONATYPE_EXTERNAL_REPO.getId());
    assertThat(bomNode.get(RepositoryMatcher.FIELD_RELATIVE_POPULARITY).asInt()).isEqualTo(
        evaluation.relativePopularity);
    assertThat(bomNode.get(RepositoryMatcher.FIELD_CREATE_TIME).asLong()).isEqualTo(evaluation.catalogDate);
    assertThat(bomNode.get(RepositoryMatcher.FIELD_COMPONENT_CATEGORIES)).isEqualTo(
        RepositoryMatcher.convert(evaluation.componentCategories));
    assertThat(bomNode.get(RepositoryMatcher.FIELD_HYGIENE_RATING)).isEqualTo(
        RepositoryMatcher.convert(evaluation.hygieneRating));
    assertThat(bomNode.get(RepositoryMatcher.FIELD_ANALYZER_FEATURES)).isEqualTo(RepositoryMatcher.convert(
        new AnalyzerFeatures(AnalysisSource.SDS, AnalysisType.COORDINATE, scanClient, true, true, true)));
    assertThat(bomNode.get(RepositoryMatcher.FIELD_INTEGRITY_RATING)).isEqualTo(
        RepositoryMatcher.convert(evaluation.integrityRating));
  }

  @Test
  public void testMatch_AllScenarios() throws IOException {
    SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.setEnabled(true);

    ObjectNode bomJson = (ObjectNode) readJsonFile("match/bom.json");
    ObjectNode securityJson = (ObjectNode) readJsonFile("match/security.json");
    ObjectNode licenseJson = (ObjectNode) readJsonFile("match/licenses.json");
    ObjectNode summaryJson = (ObjectNode) readJsonFile("match/summary.json");
    ObjectNode dataJson = (ObjectNode) readJsonFile("match/data.json");

    ComponentIdentifier ci1 =
        new PackageUrlIdentifier("pkg:maven/spring-web/spring-web@1.0?type=jar").toComponentIdentifier();
    ComponentIdentifier ci2 =
        new PackageUrlIdentifier("pkg:maven/activeio/activeio@1.0?type=jar").toComponentIdentifier();

    List<ComponentEvaluationData> evaluation = new ArrayList<>();

    ComponentEvaluationData unknown = new ComponentEvaluationData();
    unknown.matchState = "unknown";
    unknown.requestIndex = 1;
    unknown.declaredLicenses = Collections.emptySet();
    unknown.observedLicenses = Collections.emptySet();
    unknown.securityVulnerabilities = Collections.emptyList();
    evaluation.add(unknown);

    ComponentEvaluationData exact = new ComponentEvaluationData();
    exact.hash = "05431145264b6ae31a85";
    exact.componentIdentifier = ci2;
    exact.matchState = "exact";
    exact.requestIndex = 0;
    exact.declaredLicenses = Collections.singleton(new License("Not-Declared", "Not Declared"));
    exact.observedLicenses = Collections.singleton(new License("Apache-2.0", "Apache-2.0"));
    exact.securityVulnerabilities = Collections.emptyList();
    exact.componentCategories = Collections.singletonList(new ComponentCategory(63, "Data Transport"));
    exact.relativePopularity = 100;
    exact.catalogDate = 1135095471000L;
    evaluation.add(exact);

    when(mockApiComponentDetailsServiceV2.getComponentDetailsListFromHds(Arrays.asList(ci2, ci1),
        ApiComponentDetailsServiceV2.PURPOSE_EVALUATION)).thenReturn(evaluation);

    repositoryIdentifiedComponentCache.put("70fda5d53b25ec6535178cb557d46de37575d336e89ae1ea080e12c67d10811f", ci1);
    repositoryIdentifiedComponentCache.put("44ba611acde81de4319b2c4412d3379c74527bf4f433d78f89b213e08f7e6418", ci2);

    Set<ComponentIdentifier> identified =
        matcher.match(application, bomJson, dataJson, summaryJson, licenseJson, securityJson);
    assertThat(identified).hasSize(2);

    ObjectNode expectedBomJson = (ObjectNode) readJsonFile("match/outcome/bom.json");
    ObjectNode expectedSecurityJson = (ObjectNode) readJsonFile("match/outcome/security.json");
    ObjectNode expectedLicenseJson = (ObjectNode) readJsonFile("match/outcome/licenses.json");
    ObjectNode expectedSummaryJson = (ObjectNode) readJsonFile("match/outcome/summary.json");
    ObjectNode expectedDataJson = (ObjectNode) readJsonFile("match/outcome/data.json");

    assertThat(bomJson).isEqualTo(expectedBomJson);
    assertThat(securityJson).isEqualTo(expectedSecurityJson);
    assertThat(licenseJson).isEqualTo(expectedLicenseJson);
    assertThat(summaryJson).isEqualTo(expectedSummaryJson);
    assertThat(dataJson).isEqualTo(expectedDataJson);
  }

  @Test
  public void testUpdateLicensesJson_ComponentIdentifier() {
    String hash = "hash";
    ObjectNode licensesNode = objectMapper.createObjectNode();
    ArrayNode arrayNode = licensesNode.putArray("aaData");
    ComponentEvaluationData evaluation = new ComponentEvaluationData();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");

    try (MockedStatic<RepositoryMatcher> repositoryMatcher = Mockito.mockStatic(RepositoryMatcher.class,
        CALLS_REAL_METHODS))
    {
      RepositoryMatcher.updateLicensesJson(licensesNode, componentIdentifier, hash, false, evaluation, false);

      assertThat(arrayNode).hasSize(1);
      repositoryMatcher.verify(
          () -> RepositoryMatcher.updateComponentIdentifier((ObjectNode) arrayNode.get(0), componentIdentifier));
    }
  }

  @Test
  public void testUpdateLicensesJson_ExistingNode() {
    String hash = "hash";
    ObjectNode licensesNode = objectMapper.createObjectNode();
    ArrayNode licenseNodes = licensesNode.putArray("aaData");
    licenseNodes.addObject();
    ObjectNode licenseNode2 = licenseNodes.addObject();
    licenseNode2.put(RepositoryMatcher.FIELD_HASH, hash);
    ObjectNode licenseNode3 = licenseNodes.addObject();
    licenseNode3.put(RepositoryMatcher.FIELD_HASH, "otherHash");

    RepositoryMatcher.updateLicensesJson(licensesNode,
        ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"), hash, false,
        new ComponentEvaluationData(), false);

    assertThat(getNodeByHash(licenseNodes, hash)).isNotEqualTo(licenseNode2);
  }

  @Test
  public void testUpdateLicensesJson_NoExistingNode() {
    String hash = "hash";
    ObjectNode licensesNode = objectMapper.createObjectNode();
    ArrayNode licenseNodes = licensesNode.putArray("aaData");
    licenseNodes.addObject();
    ObjectNode licenseNode2 = licenseNodes.addObject();
    licenseNode2.put(RepositoryMatcher.FIELD_HASH, "otherHash");

    ComponentEvaluationData evaluation = new ComponentEvaluationData();
    evaluation.hash = hash;

    RepositoryMatcher.updateLicensesJson(licensesNode,
        ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"), hash, false,
        evaluation, false);

    assertThat(getNodeByHash(licenseNodes, hash)).isNotNull();
  }

  @Test
  public void testUpdateLicensesJson_Evaluation_NotProprietary() {
    String hash = "hash";
    ObjectNode licensesNode = objectMapper.createObjectNode();
    ArrayNode arrayNode = licensesNode.putArray("aaData");
    ComponentEvaluationData evaluation = new ComponentEvaluationData();
    evaluation.hash = hash;
    RepositoryMatcher.updateLicensesJson(licensesNode,
        ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"), hash, false, evaluation, false);

    ObjectNode licenseNode = (ObjectNode) getNodeByHash(arrayNode, hash);
    assertThat(licenseNode).isNotNull();
    assertThat(licenseNode.get(RepositoryMatcher.FIELD_PROPRIETARY).asBoolean()).isFalse();
  }

  @Test
  public void testUpdateLicensesJson_Evaluation_Proprietary() {
    String hash = "hash";
    ObjectNode licensesNode = objectMapper.createObjectNode();
    ArrayNode arrayNode = licensesNode.putArray("aaData");
    ComponentEvaluationData evaluation = new ComponentEvaluationData();
    evaluation.hash = hash;

    RepositoryMatcher.updateLicensesJson(licensesNode,
        ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"), hash, true, evaluation, false);

    ObjectNode licenseNode = (ObjectNode) getNodeByHash(arrayNode, hash);
    assertThat(licenseNode).isNotNull();
    assertThat(licenseNode.get(RepositoryMatcher.FIELD_PROPRIETARY).asBoolean()).isTrue();
  }

  @Test
  public void testUpdateLicensesJson_Evaluation_NullFields() {
    String hash = "hash";
    ObjectNode licensesNode = objectMapper.createObjectNode();
    ArrayNode arrayNode = licensesNode.putArray("aaData");
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    ComponentEvaluationData evaluation = new ComponentEvaluationData();
    evaluation.hash = hash;

    RepositoryMatcher.updateLicensesJson(licensesNode, componentIdentifier, hash, false, evaluation, false);

    ObjectNode licenseNode = (ObjectNode) getNodeByHash(arrayNode, hash);
    assertThat(licenseNode).isNotNull();
    assertThat(licenseNode.get(RepositoryMatcher.FIELD_DECLARED_LICENSES)).isEqualTo(
        JsonUtils.asTree(Collections.singleton(RepositoryMatcher.NOT_SUPPORTED_LICENSE_NAME)));
    assertThat(licenseNode.get(RepositoryMatcher.FIELD_OBSERVED_LICENSES)).isEqualTo(
        JsonUtils.asTree(Collections.singleton(RepositoryMatcher.NOT_SUPPORTED_LICENSE_NAME)));
    assertThat(licenseNode.get(RepositoryMatcher.FIELD_EFFECTIVE_LICENSES)).isEqualTo(
        JsonUtils.asTree(Collections.singleton(RepositoryMatcher.NOT_SUPPORTED_LICENSE_NAME)));
    assertThat(licenseNode.get(RepositoryMatcher.FIELD_MATCH_STATE).asText()).isEqualTo(MatchState.EXACT.getId());
    assertThat(licenseNode.get(RepositoryMatcher.FIELD_MATCHED_BY_COORDINATES).asBoolean()).isTrue();
    assertThat(licenseNode.get(RepositoryMatcher.FIELD_CATALOG_DATE)).isEqualTo(NullNode.getInstance());
    assertThat(licenseNode.get(ComponentLoader.DISPLAY_NAME_FIELD)).isEqualTo(
        JsonUtils.asTree(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier)));
  }

  @Test
  public void testUpdateLicensesJson_Evaluation() {
    String hash = "hash";
    ObjectNode licensesNode = objectMapper.createObjectNode();
    ArrayNode arrayNode = licensesNode.putArray("aaData");
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    ComponentEvaluationData evaluation = new ComponentEvaluationData();
    evaluation.hash = hash;
    evaluation.declaredLicenses = new LinkedHashSet<>(Arrays.asList(
        new License("id1", "d1"),
        new License("id2", "d2")));
    evaluation.observedLicenses = new LinkedHashSet<>(Arrays.asList(
        new License("id3", "o1"),
        new License("id4", "o2")));
    evaluation.catalogDate = 7L;

    RepositoryMatcher.updateLicensesJson(licensesNode, componentIdentifier, hash, false, evaluation, false);

    ObjectNode licenseNode = (ObjectNode) getNodeByHash(arrayNode, hash);
    assertThat(licenseNode).isNotNull();
    assertThat(licenseNode.get(RepositoryMatcher.FIELD_DECLARED_LICENSES)).isEqualTo(
        JsonUtils.asTree(Arrays.asList("d1", "d2")));
    assertThat(licenseNode.get(RepositoryMatcher.FIELD_OBSERVED_LICENSES)).isEqualTo(
        JsonUtils.asTree(Arrays.asList("o1", "o2")));
    assertThat(licenseNode.get(RepositoryMatcher.FIELD_EFFECTIVE_LICENSES)).isEqualTo(
        JsonUtils.asTree(Arrays.asList("d1", "d2", "o1", "o2")));
    assertThat(licenseNode.get(RepositoryMatcher.FIELD_MATCH_STATE).asText()).isEqualTo(MatchState.EXACT.getId());
    assertThat(licenseNode.get(RepositoryMatcher.FIELD_MATCHED_BY_COORDINATES).asBoolean()).isTrue();
    assertThat(licenseNode.get(RepositoryMatcher.FIELD_CATALOG_DATE).asLong()).isEqualTo(evaluation.catalogDate);
    assertThat(licenseNode.get(ComponentLoader.DISPLAY_NAME_FIELD)).isEqualTo(
        JsonUtils.asTree(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier)));
  }

  @Test
  public void testUpdateSecurityJson_ComponentIdentifier() {
    String hash = "hash";
    ObjectNode securityNode = objectMapper.createObjectNode();
    ArrayNode arrayNode = securityNode.putArray("aaData");
    ComponentEvaluationData evaluation = new ComponentEvaluationData();
    evaluation.hash = hash;
    SecurityVulnerability securityVulnerability1 = new SecurityVulnerability();
    securityVulnerability1.setRefId("1");
    SecurityVulnerability securityVulnerability2 = new SecurityVulnerability();
    securityVulnerability2.setRefId("2");
    evaluation.securityVulnerabilities = Arrays.asList(securityVulnerability1, securityVulnerability2);
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");

    try (MockedStatic<RepositoryMatcher> repositoryMatcher = Mockito.mockStatic(RepositoryMatcher.class,
        CALLS_REAL_METHODS))
    {
      RepositoryMatcher.updateSecurityJson(securityNode, componentIdentifier, hash, false, evaluation, false);

      assertThat(arrayNode).hasSize(2);
      repositoryMatcher.verify(
          () -> RepositoryMatcher.updateComponentIdentifier((ObjectNode) arrayNode.get(0), componentIdentifier));
      repositoryMatcher.verify(
          () -> RepositoryMatcher.updateComponentIdentifier((ObjectNode) arrayNode.get(1), componentIdentifier));
    }
  }

  @Test
  public void testUpdateSecurityJson_ExistingNodes() {
    String hash = "hash";
    ObjectNode securityNode = objectMapper.createObjectNode();
    ArrayNode arrayNode = securityNode.putArray("aaData");
    arrayNode.addObject();
    ObjectNode securityNode2 = arrayNode.addObject();
    securityNode2.put(RepositoryMatcher.FIELD_HASH, hash);
    ObjectNode securityNode3 = arrayNode.addObject();
    securityNode3.put(RepositoryMatcher.FIELD_HASH, hash);
    ObjectNode securityNode4 = arrayNode.addObject();
    securityNode4.put(RepositoryMatcher.FIELD_HASH, "otherHash");

    RepositoryMatcher.updateSecurityJson(securityNode,
        ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"), hash, false,
        new ComponentEvaluationData(), false);

    assertThat(getNodesByHash(arrayNode, hash)).isEmpty();
  }

  @Test
  public void testUpdateSecurityJson_NoExistingNodes() {
    String hash = "hash";
    ObjectNode securityNode = objectMapper.createObjectNode();
    ArrayNode arrayNode = securityNode.putArray("aaData");
    arrayNode.addObject();
    ObjectNode securityNode2 = arrayNode.addObject();
    securityNode2.put(RepositoryMatcher.FIELD_HASH, "otherHash");

    RepositoryMatcher.updateSecurityJson(securityNode,
        ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"), hash, false,
        new ComponentEvaluationData(), false);

    assertThat(getNodesByHash(arrayNode, hash)).isEmpty();
  }

  @Test
  public void testUpdateSecurityJson_NoSecurityVulnerabilities() {
    String hash = "hash";
    ObjectNode securityNode = objectMapper.createObjectNode();
    ArrayNode arrayNode = securityNode.putArray("aaData");
    ComponentEvaluationData evaluation = new ComponentEvaluationData();
    evaluation.hash = hash;

    RepositoryMatcher.updateSecurityJson(securityNode, null, hash, false, evaluation, false);

    assertThat(getNodesByHash(arrayNode, hash)).isEmpty();
  }

  @Test
  public void testUpdateSecurityJson_NotProprietary() {
    String hash = "hash";
    ObjectNode securityNode = objectMapper.createObjectNode();
    ArrayNode arrayNode = securityNode.putArray("aaData");
    ComponentEvaluationData evaluation = new ComponentEvaluationData();
    evaluation.hash = hash;
    evaluation.securityVulnerabilities = Collections.singletonList(new SecurityVulnerability());

    RepositoryMatcher.updateSecurityJson(securityNode,
        ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"), hash, false, evaluation, false);

    List<JsonNode> nodes = getNodesByHash(arrayNode, hash);
    assertThat(nodes).hasSize(1);
    assertThat(nodes.get(0).get(RepositoryMatcher.FIELD_PROPRIETARY).asBoolean()).isFalse();
  }

  @Test
  public void testUpdateSecurityJson_Proprietary() {
    String hash = "hash";
    ObjectNode securityNode = objectMapper.createObjectNode();
    ArrayNode arrayNode = securityNode.putArray("aaData");
    ComponentEvaluationData evaluation = new ComponentEvaluationData();
    evaluation.hash = hash;
    evaluation.securityVulnerabilities = Collections.singletonList(new SecurityVulnerability());

    RepositoryMatcher.updateSecurityJson(securityNode,
        ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"), hash, true, evaluation, false);

    List<JsonNode> nodes = getNodesByHash(arrayNode, hash);
    assertThat(nodes).hasSize(1);
    assertThat(nodes.get(0).get(RepositoryMatcher.FIELD_PROPRIETARY).asBoolean()).isTrue();
  }

  @Test
  public void testUpdateSecurityJson() {
    String hash = "hash";
    ObjectNode securityNode = objectMapper.createObjectNode();
    ArrayNode arrayNode = securityNode.putArray("aaData");
    ComponentEvaluationData evaluation = new ComponentEvaluationData();
    evaluation.hash = hash;
    SecurityVulnerability securityVulnerability1 = new SecurityVulnerability();
    SecurityVulnerability securityVulnerability2 = new SecurityVulnerability();
    securityVulnerability2.setUrl("url");
    securityVulnerability2.setRefId("refId");
    securityVulnerability2.setSource("source");
    securityVulnerability2.setSeverity(9f);
    securityVulnerability2.setCwe("11");
    securityVulnerability2.setCvssVector("test");
    securityVulnerability2.setCvssVectorSource("testSource");
    securityVulnerability2.setVulnerabilityCategories(Arrays.asList("c1", "c2"));
    evaluation.securityVulnerabilities = Arrays.asList(securityVulnerability1, securityVulnerability2);

    RepositoryMatcher.updateSecurityJson(securityNode,
        ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"), hash, false, evaluation, false);

    List<JsonNode> nodes = getNodesByHash(arrayNode, hash);
    assertThat(nodes).hasSize(2);
    for (JsonNode node : nodes) {
      assertThat(node.get(RepositoryMatcher.FIELD_MATCH_STATE).asText()).isEqualTo(MatchState.EXACT.getId());
    }
    assertThat(nodes.get(0).get(RepositoryMatcher.FIELD_URL)).isEqualTo(NullNode.getInstance());
    assertThat(nodes.get(0).get(RepositoryMatcher.FIELD_REFERENCE)).isEqualTo(NullNode.getInstance());
    assertThat(nodes.get(0).get(RepositoryMatcher.FIELD_SOURCE)).isEqualTo(NullNode.getInstance());
    assertThat(nodes.get(0).get(RepositoryMatcher.FIELD_SCORE)).isEqualTo(NullNode.getInstance());
    assertThat(nodes.get(0).get(RepositoryMatcher.FIELD_VULNERABILITY_CATEGORIES)).isEqualTo(NullNode.getInstance());
    assertThat(nodes.get(0).get(RepositoryMatcher.FIELD_CWE)).isEqualTo(NullNode.getInstance());
    assertThat(nodes.get(0).get(RepositoryMatcher.FIELD_VECTOR_SOURCE)).isEqualTo(NullNode.getInstance());
    assertThat(nodes.get(0).get(RepositoryMatcher.FIELD_VECTOR_STRING)).isEqualTo(NullNode.getInstance());

    assertThat(nodes.get(1).get(RepositoryMatcher.FIELD_URL).asText()).isEqualTo(securityVulnerability2.getUrl());
    assertThat(nodes.get(1).get(RepositoryMatcher.FIELD_REFERENCE).asText()).isEqualTo(
        securityVulnerability2.getRefId());
    assertThat(nodes.get(1).get(RepositoryMatcher.FIELD_SOURCE).asText()).isEqualTo(securityVulnerability2.getSource());
    assertThat(nodes.get(1).get(RepositoryMatcher.FIELD_SCORE).asDouble()).isEqualTo(
        (double) securityVulnerability2.getSeverity());
    assertThat(nodes.get(1).get(RepositoryMatcher.FIELD_VULNERABILITY_CATEGORIES)).isEqualTo(
        JsonUtils.asTree(securityVulnerability2.getVulnerabilityCategories()));
    assertThat(nodes.get(1).get(RepositoryMatcher.FIELD_CWE).asText()).isEqualTo(securityVulnerability2.getCwe());
    assertThat(nodes.get(1).get(RepositoryMatcher.FIELD_VECTOR_SOURCE).asText())
        .isEqualTo(securityVulnerability2.getCvssVectorSource());
    assertThat(nodes.get(1).get(RepositoryMatcher.FIELD_VECTOR_STRING).asText())
        .isEqualTo(securityVulnerability2.getCvssVector());
  }

  private JsonNode getNodeByHash(ArrayNode arrayNode, String hash) {
    for (JsonNode node : arrayNode) {
      if (node.path(RepositoryMatcher.FIELD_HASH).asText().equals(hash)) {
        return node;
      }
    }
    return null;
  }

  private List<JsonNode> getNodesByHash(ArrayNode arrayNode, String hash) {
    List<JsonNode> nodes = new ArrayList<>();
    for (JsonNode node : arrayNode) {
      if (node.path(RepositoryMatcher.FIELD_HASH).asText().equals(hash)) {
        nodes.add(node);
      }
    }
    return nodes;
  }

  @Test
  public void testUpdateDataJson_NoExistingData() {
    ObjectNode dataJson = objectMapper.createObjectNode();

    RepositoryMatcher.updateDataJson(dataJson, 3, 7);

    assertThat(dataJson.get(RepositoryMatcher.FIELD_PARTIALLY_MATCHED_COMPONENT_COUNT).asInt()).isZero();
    assertThat(dataJson.get(RepositoryMatcher.FIELD_EXACTLY_MATCHED_COMPONENT_COUNT).asInt()).isEqualTo(10);
    assertThat(dataJson.get(RepositoryMatcher.FIELD_KNOWN_ARTIFACT_COUNT).asInt()).isEqualTo(10);
  }

  @Test
  public void testUpdateDataJson() {
    ObjectNode dataJson = objectMapper.createObjectNode();
    dataJson.put(RepositoryMatcher.FIELD_PARTIALLY_MATCHED_COMPONENT_COUNT, 8);
    dataJson.put(RepositoryMatcher.FIELD_EXACTLY_MATCHED_COMPONENT_COUNT, 2);
    dataJson.put(RepositoryMatcher.FIELD_KNOWN_ARTIFACT_COUNT, 10);

    RepositoryMatcher.updateDataJson(dataJson, 3, 7);

    assertThat(dataJson.get(RepositoryMatcher.FIELD_PARTIALLY_MATCHED_COMPONENT_COUNT).asInt()).isEqualTo(1);
    assertThat(dataJson.get(RepositoryMatcher.FIELD_EXACTLY_MATCHED_COMPONENT_COUNT).asInt()).isEqualTo(12);
    assertThat(dataJson.get(RepositoryMatcher.FIELD_KNOWN_ARTIFACT_COUNT).asInt()).isEqualTo(13);
  }

  @Test
  public void testUpdateSummaryJson_NoExistingData() {
    ObjectNode summaryJson = objectMapper.createObjectNode();

    RepositoryMatcher.updateSummaryJson(summaryJson, 3, 7);

    assertThat(summaryJson.get(RepositoryMatcher.FIELD_KNOWN_ARTIFACT_COUNT).asInt()).isEqualTo(10);
  }

  @Test
  public void testUpdateSummaryJson() {
    ObjectNode summaryJson = objectMapper.createObjectNode();
    summaryJson.put(RepositoryMatcher.FIELD_KNOWN_ARTIFACT_COUNT, 10);

    RepositoryMatcher.updateSummaryJson(summaryJson, 3, 7);

    assertThat(summaryJson.get(RepositoryMatcher.FIELD_KNOWN_ARTIFACT_COUNT).asInt()).isEqualTo(13);
  }

  @Test
  public void testUpdateJsonFiles_NoNewKnown() {
    Application application = tempEntity.newApplicationWithParent();
    Map<ComponentIdentifier, ObjectNode> bomNodeByIdentifier = new HashMap<>();
    ObjectNode bomJson = objectMapper.createObjectNode();
    ObjectNode dataJson = objectMapper.createObjectNode();
    ObjectNode summaryJson = objectMapper.createObjectNode();
    Map<ComponentIdentifier, ComponentEvaluationData> evaluationByIdentifier = new HashMap<>();

    matcher.updateJsonFiles(application, bomJson, dataJson, summaryJson, null, null,
        bomNodeByIdentifier, evaluationByIdentifier);

    assertThat(dataJson.size()).isZero();
    assertThat(summaryJson.size()).isZero();
  }

  @Test
  public void testUpdateJsonFiles() {
    String hash = "hash";
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newProprietaryConfig(application.getId(), Collections.singletonList("g1"), Collections.emptyList());
    Map<ComponentIdentifier, ObjectNode> bomNodeByIdentifier = new HashMap<>();
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2");
    bomNodeByIdentifier.put(componentIdentifier1, createObjectNode(hash, MatchState.UNKNOWN));
    bomNodeByIdentifier.put(componentIdentifier2, createObjectNode(hash, MatchState.SIMILAR));
    ObjectNode bomJson = objectMapper.createObjectNode();
    bomJson.putArray("aaData");
    ObjectNode dataJson = objectMapper.createObjectNode();
    ObjectNode summaryJson = objectMapper.createObjectNode();
    Map<ComponentIdentifier, ComponentEvaluationData> evaluationByIdentifier = new HashMap<>();

    ComponentEvaluationData evaluation = new ComponentEvaluationData();
    evaluation.matchState = "exact";
    evaluationByIdentifier.put(componentIdentifier1, evaluation);
    evaluationByIdentifier.put(componentIdentifier2, evaluation);
    ObjectNode licensesJson = objectMapper.createObjectNode();
    licensesJson.putArray("aaData");
    ObjectNode securityJson = objectMapper.createObjectNode();
    securityJson.putArray("aaData");

    try (MockedStatic<RepositoryMatcher> mockedRepositoryMatcher = Mockito.mockStatic(RepositoryMatcher.class,
        CALLS_REAL_METHODS))
    {
      Set<ComponentIdentifier> componentIdentifiers =
          matcher.updateJsonFiles(application, bomJson, dataJson, summaryJson, licensesJson,
              securityJson, bomNodeByIdentifier, evaluationByIdentifier);

      assertThat(componentIdentifiers).containsExactlyInAnyOrder(componentIdentifier1, componentIdentifier2);
      assertThat(summaryJson.get(RepositoryMatcher.FIELD_KNOWN_ARTIFACT_COUNT).asInt()).isEqualTo(2);
      mockedRepositoryMatcher.verify(
          () -> RepositoryMatcher.updateBomJson(any(), eq(componentIdentifier1), any(), eq(true), any(), eq(false)));
      mockedRepositoryMatcher.verify(
          () -> RepositoryMatcher.updateLicensesJson(any(), eq(componentIdentifier1), anyString(),
              eq(true), any(), eq(false)));
      mockedRepositoryMatcher.verify(
          () -> RepositoryMatcher.updateSecurityJson(any(), eq(componentIdentifier1), anyString(),
              eq(true), any(), eq(false)));
      mockedRepositoryMatcher.verify(
          () -> RepositoryMatcher.updateBomJson(any(), eq(componentIdentifier2), any(), eq(false), any(), eq(false)));
      mockedRepositoryMatcher.verify(
          () -> RepositoryMatcher.updateLicensesJson(any(), eq(componentIdentifier2), anyString(),
              eq(false), any(), eq(false)));
      mockedRepositoryMatcher.verify(
          () -> RepositoryMatcher.updateSecurityJson(any(), eq(componentIdentifier2), anyString(),
              eq(false), any(), eq(false)));
      mockedRepositoryMatcher.verify(() -> RepositoryMatcher.updateDataJson(any(), anyInt(), anyInt()));
      mockedRepositoryMatcher.verify(() -> RepositoryMatcher.updateSummaryJson(any(), anyInt(), anyInt()));
    }
  }

  @Test
  public void testUpdateJsonFiles_ExternalRepository() {
    String hash = "hash";
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newProprietaryConfig(application.getId(), Collections.singletonList("g1"), Collections.emptyList());
    Map<ComponentIdentifier, ObjectNode> bomNodeByIdentifier = new HashMap<>();
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2");

    bomNodeByIdentifier.put(componentIdentifier1, createObjectNode(hash, MatchState.UNKNOWN));
    bomNodeByIdentifier.put(componentIdentifier2, createObjectNode(hash, MatchState.SIMILAR));
    ObjectNode bomJson = objectMapper.createObjectNode();
    bomJson.putArray("aaData");
    ObjectNode dataJson = objectMapper.createObjectNode();
    ObjectNode summaryJson = objectMapper.createObjectNode();
    Map<ComponentIdentifier, ComponentEvaluationData> evaluationByIdentifier = new HashMap<>();
    ComponentEvaluationData evaluationData = new ComponentEvaluationData();
    evaluationData.matchState = "unknown";
    evaluationByIdentifier.put(componentIdentifier1, evaluationData);
    evaluationByIdentifier.put(componentIdentifier2, evaluationData);
    ObjectNode licensesJson = objectMapper.createObjectNode();
    licensesJson.putArray("aaData");
    ObjectNode securityJson = objectMapper.createObjectNode();
    securityJson.putArray("aaData");

    try (MockedStatic<RepositoryMatcher> mockedRepositoryMatcher = Mockito.mockStatic(RepositoryMatcher.class,
        CALLS_REAL_METHODS))
    {
      Set<ComponentIdentifier> componentIdentifiers =
          matcher.updateJsonFiles(application, bomJson, dataJson, summaryJson, licensesJson,
              securityJson, bomNodeByIdentifier, evaluationByIdentifier);

      assertThat(componentIdentifiers).containsExactlyInAnyOrder(componentIdentifier1, componentIdentifier2);
      assertThat(summaryJson.get(RepositoryMatcher.FIELD_KNOWN_ARTIFACT_COUNT).asInt()).isEqualTo(2);
      mockedRepositoryMatcher.verify(
          () -> RepositoryMatcher.updateBomJson(any(), eq(componentIdentifier1), any(), eq(true), any(), eq(true)));
      mockedRepositoryMatcher.verify(
          () -> RepositoryMatcher.updateLicensesJson(any(), eq(componentIdentifier1), anyString(),
              eq(true), any(), eq(true)));
      mockedRepositoryMatcher.verify(
          () -> RepositoryMatcher.updateSecurityJson(any(), eq(componentIdentifier1), anyString(),
              eq(true), any(), eq(true)));
      mockedRepositoryMatcher.verify(
          () -> RepositoryMatcher.updateBomJson(any(), eq(componentIdentifier2), any(), eq(false), any(), eq(true)));
      mockedRepositoryMatcher.verify(
          () -> RepositoryMatcher.updateLicensesJson(any(), eq(componentIdentifier2), anyString(),
              eq(false), any(), eq(true)));
      mockedRepositoryMatcher.verify(
          () -> RepositoryMatcher.updateSecurityJson(any(), eq(componentIdentifier2), anyString(),
              eq(false), any(), eq(true)));
      mockedRepositoryMatcher.verify(() -> RepositoryMatcher.updateDataJson(any(), anyInt(), anyInt()));
      mockedRepositoryMatcher.verify(() -> RepositoryMatcher.updateSummaryJson(any(), anyInt(), anyInt()));
    }
  }

  @Test
  public void testConvert_Null() {
    assertThat(RepositoryMatcher.convert(null)).isEqualTo(NullNode.getInstance());
  }

  @Test
  public void testConvert() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    assertThat(RepositoryMatcher.convert(componentIdentifier)).isEqualTo(JsonUtils.asTree(componentIdentifier));
  }

  @Test
  public void testCreateAnalyzerFeatures_Supported_NoLicense() {
    String scanClient = "someScanClient";
    for (String format : ComponentIdentifier.NO_LICENSE_FORMATS) {
      assertThat(RepositoryMatcher.createAnalyzerFeatures(format, scanClient)).usingRecursiveComparison()
          .isEqualTo(
              new AnalyzerFeatures(AnalysisSource.SDS, AnalysisType.COORDINATE, scanClient, false, true, true));
    }
  }

  @Test
  public void testCreateAnalyzerFeatures_Supported_WithLicense() {
    String scanClient = "someScanClient";
    Set<String> formats = new HashSet<>(ComponentIdentifier.getFormatsSupportedByHds());
    ComponentIdentifier.NO_LICENSE_FORMATS.forEach(formats::remove);
    for (String format : formats) {
      assertThat(RepositoryMatcher.createAnalyzerFeatures(format, scanClient)).usingRecursiveComparison()
          .isEqualTo(
              new AnalyzerFeatures(AnalysisSource.SDS, AnalysisType.COORDINATE, scanClient, true, true, true));
    }
  }

  @Test
  public void testCreateAnalyzerFeatures_Lqa() {
    String scanClient = "someScanClient";
    Set<String> formats = new HashSet<>();
    for (LqaFormat value : LqaFormat.values()) {
      formats.add(value.format);
    }
    ComponentIdentifier.getFormatsSupportedByHds().forEach(formats::remove);
    for (String format : formats) {
      assertThat(RepositoryMatcher.createAnalyzerFeatures(format, scanClient)).usingRecursiveComparison()
          .isEqualTo(new AnalyzerFeatures(AnalysisSource.SDS, AnalysisType.COORDINATE, scanClient, false, false, true));
    }
  }

  @Test
  public void testCreateAnalyzerFeatures_Unknown() {
    assertThat(RepositoryMatcher.createAnalyzerFeatures("unknown", null)).isNull();
  }

  private ObjectNode createObjectNode(String hash, MatchState matchState) {
    ObjectNode objectNode = objectMapper.createObjectNode();
    objectNode.put(RepositoryMatcher.FIELD_HASH, hash);
    objectNode.put(RepositoryMatcher.FIELD_MATCH_STATE, matchState == null ? null : matchState.getId());
    return objectNode;
  }

  private void mockArtifactoryResponse() {
    mockArtifactoryResponse("eba07aa1954b30c10b2a562bed89ba077555fdbf3a40e2edc672a055aa40f941",
        "http://localhost/artifactory/api/storage/reponame/g/org/a/1.1-SNAPSHOT/a-1.1-SNAPSHOT.jar");
  }

  private void mockArtifactoryResponse(String sha256, String... uris) {
    artifactoryMockServer.mockSearchChecksum(ChecksumType.SHA256, sha256,
        ArtifactoryChecksumSearchResults.create(uris));
  }

  private JsonNode readJsonFile(String path) throws IOException {
    return objectMapper.readTree(getClass().getResource("/RepositoryMatcherTest/" + path));
  }

  private ObjectNode createObjectNodeWithAaData() {
    ObjectNode result = objectMapper.createObjectNode();
    result.putArray("aaData");
    return result;
  }
}
