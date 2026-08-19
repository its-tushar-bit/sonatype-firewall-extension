/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.service.ApiRepositoryConnectionService;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryConnectionDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.repository.RepositoryFormat;
import com.sonatype.insight.brain.repository.client.RepositoryClientFactory;
import com.sonatype.insight.brain.repository.client.RepositoryClientFactory.RepositoryClientBuilder;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.telemetry.model.TelemetryData;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static com.sonatype.insight.brain.repository.RepositoryQueryService.INNERSOURCE_REPOSITORY_FORMAT_KEY;
import static com.sonatype.insight.brain.repository.RepositoryQueryService.INNERSOURCE_REPOSITORY_QUERY_COUNT_KEY;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsTenant;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsNewTenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ComponentH2Test
public class RepositoryQueryServiceTest
    extends AbstractComponentH2Test
{
  private RepositoryQueryService repositoryQueryService;

  @Inject
  private RepositoryConnectionDAO dao;

  @Inject
  private ApiRepositoryConnectionService repositoryConnectionService;

  @Inject
  private OrganizationDAO organizationDAO;

  @Inject
  private ApplicationDAO applicationDAO;

  @Mock
  private PasswordHandler passwordHandler;

  @Mock
  private RepositoryClientFactory clientFactory;

  @Mock
  private RepositoryClientBuilder mockBuilder;

  @Mock
  private RepositoryClient mockClient;

  @BeforeEach
  public void before() {
    repositoryQueryService = new RepositoryQueryService(clientFactory, passwordHandler, dao,
        repositoryConnectionService);
  }

  @AfterEach
  public void after() {
    Organization rootOrganization = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    rootOrganization.setAllowRepositoryConnectionOverride(true);
    rootOrganization.setRepositoryConnectionEnabled(true);
    organizationDAO.update(rootOrganization);
  }

  @Test
  public void testGetAllVersions_Maven() throws Exception {
    RepositoryQueryService.REPOSITORY_QUERY_COUNT_PER_FORMAT.get().clear();

    // given
    Application app = getApplicationWithConnectionsEnabled();
    tempEntity.newRepositoryConnection(app.getId(), "baseUrl", RepositoryFormat.MAVEN, "user", "pass".toCharArray());
    tempEntity.newRepositoryConnection(app.getId(), "baseUrl2", RepositoryFormat.GENERIC, "user", "pass".toCharArray());
    tempEntity.newRepositoryConnection(app.getId(), "baseUrl3", RepositoryFormat.NPM, "user2", "pass2".toCharArray());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "n1", "1.2.0", "", "jar");
    when(clientFactory.create()).thenReturn(mockBuilder);
    when(mockBuilder.forNexus3(eq("baseUrl"), eq("user"), any())).thenReturn(mockClient);
    Map<String, String> params =
        ImmutableMap.of("group", "g1", "name", "n1", "maven.extension", "jar", "maven.classifier", "");
    ProxyRepositoryComponentResult c1 =
        new ProxyRepositoryComponentResult(identifier.createAlternativeVersion("1.1.0"), "c1sha1");
    ProxyRepositoryComponentResult c2 =
        new ProxyRepositoryComponentResult(identifier.createAlternativeVersion("1.2.0"), "c2sha1");
    ProxyRepositoryComponentResult c3 =
        new ProxyRepositoryComponentResult(identifier.createAlternativeVersion("1.3.0"), "c3sha1");
    List<ProxyRepositoryComponentResult> components = Arrays.asList(c1, c2, c3);
    RepositoryAllVersionsResponse mockResults = new RepositoryAllVersionsResponse(components);
    when(mockClient.getAllVersions(params)).thenReturn(mockResults);

    // when
    Pair<RepositoryAllVersionsResponse, RepositorySourceResponseDTO> results =
        repositoryQueryService.getAllVersions(identifier, app);

    // then
    assertThat(results.getLeft().getComponents()).hasSize(3).containsExactly(c1, c2, c3);
    List<TelemetryData> telemetryData = repositoryQueryService.collectAllData();
    assertThat(telemetryData).hasSize(1);
    assertThat(telemetryData.get(0).getAttributes()).hasSize(2)
        .containsEntry(INNERSOURCE_REPOSITORY_FORMAT_KEY, "maven")
        .containsEntry(INNERSOURCE_REPOSITORY_QUERY_COUNT_KEY, 1);
  }

  @Test
  public void testGetAllVersions_Telemetry() throws Exception {
    RepositoryQueryService.REPOSITORY_QUERY_COUNT_PER_FORMAT.get().clear();
    // given
    Application app = getApplicationWithConnectionsEnabled();
    tempEntity.newRepositoryConnection(app.getId(), "baseUrl", RepositoryFormat.GENERIC, "user", "pass".toCharArray());
    ComponentIdentifier maven = ComponentIdentifier.createMavenCoordinates("g1", "n1", "1.2.0", "", "jar");
    ComponentIdentifier npm1 = ComponentIdentifier.createNpmCoordinates("p1", "1.2.0");
    ComponentIdentifier npm2 = ComponentIdentifier.createNpmCoordinates("p2", "2.2.0");
    when(clientFactory.create()).thenReturn(mockBuilder);
    when(mockBuilder.forNexus3(eq("baseUrl"), eq("user"), any())).thenReturn(mockClient);
    doReturn(new RepositoryAllVersionsResponse(Collections.emptyList())).when(mockClient).getAllVersions(anyMap());

    // when
    repositoryQueryService.getAllVersions(maven, app);
    repositoryQueryService.getAllVersions(npm1, app);
    repositoryQueryService.getAllVersions(npm2, app);

    // then
    List<TelemetryData> telemetryData = repositoryQueryService.collectAllData();
    assertThat(telemetryData).hasSize(2);
    assertThat(telemetryData.get(0).getAttributes()).hasSize(2)
        .containsEntry(INNERSOURCE_REPOSITORY_FORMAT_KEY, "maven")
        .containsEntry(INNERSOURCE_REPOSITORY_QUERY_COUNT_KEY, 1);
    assertThat(telemetryData.get(1).getAttributes()).hasSize(2)
        .containsEntry(INNERSOURCE_REPOSITORY_FORMAT_KEY, "npm")
        .containsEntry(INNERSOURCE_REPOSITORY_QUERY_COUNT_KEY, 2);
  }

  @Test
  public void testGetAllVersions_Inherited_Org() throws Exception {
    // given
    Organization org = tempEntity.newOrganization();
    org.setRepositoryConnectionEnabled(true);
    organizationDAO.update(org);
    Application app = tempEntity.newApplication(org.getId());
    app.setRepositoryConnectionEnabled(null);
    applicationDAO.update(app);

    tempEntity.newRepositoryConnection(org.getId(), "baseUrl", RepositoryFormat.MAVEN, "user", "pass".toCharArray());
    tempEntity.newRepositoryConnection(org.getId(), "baseUrl2", RepositoryFormat.GENERIC, "user", "pass".toCharArray());
    tempEntity.newRepositoryConnection(org.getId(), "baseUrl3", RepositoryFormat.NPM, "user2", "pass2".toCharArray());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "n1", "1.2.0", "", "jar");
    when(clientFactory.create()).thenReturn(mockBuilder);
    when(mockBuilder.forNexus3(eq("baseUrl"), eq("user"), any())).thenReturn(mockClient);
    Map<String, String> params =
        ImmutableMap.of("group", "g1", "name", "n1", "maven.extension", "jar", "maven.classifier", "");
    ProxyRepositoryComponentResult c1 =
        new ProxyRepositoryComponentResult(identifier.createAlternativeVersion("1.1.0"), "c1sha1");
    ProxyRepositoryComponentResult c2 =
        new ProxyRepositoryComponentResult(identifier.createAlternativeVersion("1.2.0"), "c2sha1");
    ProxyRepositoryComponentResult c3 =
        new ProxyRepositoryComponentResult(identifier.createAlternativeVersion("1.3.0"), "c3sha1");
    List<ProxyRepositoryComponentResult> components = Arrays.asList(c1, c2, c3);
    RepositoryAllVersionsResponse mockResults = new RepositoryAllVersionsResponse(components);
    when(mockClient.getAllVersions(params)).thenReturn(mockResults);

    // when
    Pair<RepositoryAllVersionsResponse, RepositorySourceResponseDTO> results =
        repositoryQueryService.getAllVersions(identifier, app);

    // then
    assertThat(results.getLeft().getComponents()).hasSize(3).containsExactly(c1, c2, c3);
  }

  @Test
  public void testGetAllVersions_Inherited_Org_Disabled() throws Exception {
    Organization org = tempEntity.newOrganization();
    org.setRepositoryConnectionEnabled(false);
    organizationDAO.update(org);
    Application app = tempEntity.newApplication(org.getId());
    app.setRepositoryConnectionEnabled(null);
    applicationDAO.update(app);

    tempEntity.newRepositoryConnection(org.getId(), "baseUrl", RepositoryFormat.MAVEN, "user", "pass".toCharArray());
    tempEntity.newRepositoryConnection(org.getId(), "baseUrl2", RepositoryFormat.GENERIC, "user", "pass".toCharArray());
    tempEntity.newRepositoryConnection(org.getId(), "baseUrl3", RepositoryFormat.NPM, "user2", "pass2".toCharArray());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "n1", "1.2.0", "", "jar");
    lenient().when(clientFactory.create()).thenReturn(mockBuilder);
    lenient().when(mockBuilder.forNexus3(eq("baseUrl"), eq("user"), any())).thenReturn(mockClient);
    Map<String, String> params =
        ImmutableMap.of("group", "g1", "name", "n1", "maven.extension", "jar", "maven.classifier", "");
    ProxyRepositoryComponentResult c1 =
        new ProxyRepositoryComponentResult(identifier.createAlternativeVersion("1.1.0"), "c1sha1");
    ProxyRepositoryComponentResult c2 =
        new ProxyRepositoryComponentResult(identifier.createAlternativeVersion("1.2.0"), "c2sha1");
    ProxyRepositoryComponentResult c3 =
        new ProxyRepositoryComponentResult(identifier.createAlternativeVersion("1.3.0"), "c3sha1");
    List<ProxyRepositoryComponentResult> components = Arrays.asList(c1, c2, c3);
    RepositoryAllVersionsResponse mockResults = new RepositoryAllVersionsResponse(components);
    lenient().when(mockClient.getAllVersions(params)).thenReturn(mockResults);

    Pair<RepositoryAllVersionsResponse, RepositorySourceResponseDTO> results =
        repositoryQueryService.getAllVersions(identifier, app);

    assertThat(results.getLeft().getComponents()).isEmpty();
  }

  @Test
  public void testGetAllVersions_Inherited_Org_Disabled_Override() throws Exception {
    Organization org = tempEntity.newOrganization();
    org.setRepositoryConnectionEnabled(false);
    org.setAllowRepositoryConnectionOverride(false);
    organizationDAO.update(org);
    Application app = tempEntity.newApplication(org.getId());
    app.setRepositoryConnectionEnabled(true);
    applicationDAO.update(app);

    tempEntity.newRepositoryConnection(app.getId(), "baseUrl", RepositoryFormat.MAVEN, "user", "pass".toCharArray());
    tempEntity.newRepositoryConnection(app.getId(), "baseUrl2", RepositoryFormat.GENERIC, "user", "pass".toCharArray());
    tempEntity.newRepositoryConnection(app.getId(), "baseUrl3", RepositoryFormat.NPM, "user2", "pass2".toCharArray());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "n1", "1.2.0", "", "jar");
    lenient().when(clientFactory.create()).thenReturn(mockBuilder);
    lenient().when(mockBuilder.forNexus3(eq("baseUrl"), eq("user"), any())).thenReturn(mockClient);
    Map<String, String> params =
        ImmutableMap.of("group", "g1", "name", "n1", "maven.extension", "jar", "maven.classifier", "");
    ProxyRepositoryComponentResult c1 =
        new ProxyRepositoryComponentResult(identifier.createAlternativeVersion("1.1.0"), "c1sha1");
    ProxyRepositoryComponentResult c2 =
        new ProxyRepositoryComponentResult(identifier.createAlternativeVersion("1.2.0"), "c2sha1");
    ProxyRepositoryComponentResult c3 =
        new ProxyRepositoryComponentResult(identifier.createAlternativeVersion("1.3.0"), "c3sha1");
    List<ProxyRepositoryComponentResult> components = Arrays.asList(c1, c2, c3);
    RepositoryAllVersionsResponse mockResults = new RepositoryAllVersionsResponse(components);
    lenient().when(mockClient.getAllVersions(params)).thenReturn(mockResults);

    Pair<RepositoryAllVersionsResponse, RepositorySourceResponseDTO> results =
        repositoryQueryService.getAllVersions(identifier, app);

    assertThat(results.getLeft().getComponents()).isEmpty();
  }

  @Test
  public void testGetAllVersions_Maven_OnlyGeneric() throws Exception {
    // given
    Application app = getApplicationWithConnectionsEnabled();
    tempEntity.newRepositoryConnection(app.getId(), "baseUrl", RepositoryFormat.GENERIC, "user", "pass".toCharArray());
    tempEntity.newRepositoryConnection(app.getId(), "baseUrl2", RepositoryFormat.NPM, "user2", "pass2".toCharArray());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "n1", "1.2.0", "", "jar");
    when(clientFactory.create()).thenReturn(mockBuilder);
    when(mockBuilder.forNexus3(eq("baseUrl"), eq("user"), any())).thenReturn(mockClient);
    Map<String, String> params =
        ImmutableMap.of("group", "g1", "name", "n1", "maven.extension", "jar", "maven.classifier", "");
    ProxyRepositoryComponentResult c1 =
        new ProxyRepositoryComponentResult(identifier.createAlternativeVersion("1.1.0"), "c1sha1");
    ProxyRepositoryComponentResult c2 =
        new ProxyRepositoryComponentResult(identifier.createAlternativeVersion("1.2.0"), "c2sha1");
    ProxyRepositoryComponentResult c3 =
        new ProxyRepositoryComponentResult(identifier.createAlternativeVersion("1.3.0"), "c3sha1");
    List<ProxyRepositoryComponentResult> components = Arrays.asList(c1, c2, c3);
    RepositoryAllVersionsResponse mockResults = new RepositoryAllVersionsResponse(components);
    when(mockClient.getAllVersions(params)).thenReturn(mockResults);

    // when
    Pair<RepositoryAllVersionsResponse, RepositorySourceResponseDTO> results =
        repositoryQueryService.getAllVersions(identifier, app);

    // then
    assertThat(results.getLeft().getComponents()).hasSize(3).containsExactly(c1, c2, c3);
  }

  @Test
  public void testGetAllVersions_Maven_DoesNotMix() {
    // given
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    tempEntity.newRepositoryConnection(org.getId(), "baseUrl1", RepositoryFormat.MAVEN, "user", "pass".toCharArray());
    tempEntity.newRepositoryConnection(app.getId(), "baseUrl2", RepositoryFormat.NPM, "user2", "pass2".toCharArray());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "n1", "1.2.0", "", "jar");

    // when
    Pair<RepositoryAllVersionsResponse, RepositorySourceResponseDTO> results =
        repositoryQueryService.getAllVersions(identifier, app);

    // then
    assertThat(results.getLeft().getComponents()).isEmpty();
  }

  @Test
  public void testGetAllVersions_Npm() throws Exception {
    // given
    Application app = getApplicationWithConnectionsEnabled();
    tempEntity.newRepositoryConnection(app.getId(), "baseUrl", RepositoryFormat.NPM, "user", "pass".toCharArray());
    tempEntity.newRepositoryConnection(app.getId(), "baseUrl2", RepositoryFormat.GENERIC, "user", "pass".toCharArray());
    tempEntity.newRepositoryConnection(app.getId(), "baseUrl3", RepositoryFormat.MAVEN, "user2", "pass2".toCharArray());
    ComponentIdentifier identifier = ComponentIdentifier.createNpmCoordinates("p1", "1.2.0");
    when(clientFactory.create()).thenReturn(mockBuilder);
    when(mockBuilder.forNexus3(eq("baseUrl"), eq("user"), any())).thenReturn(mockClient);
    Map<String, String> params = ImmutableMap.of("name", "p1");
    ProxyRepositoryComponentResult c1 =
        new ProxyRepositoryComponentResult(identifier.createAlternativeVersion("1.1.0"), "c1sha1");
    ProxyRepositoryComponentResult c2 =
        new ProxyRepositoryComponentResult(identifier.createAlternativeVersion("1.2.0"), "c2sha1");
    ProxyRepositoryComponentResult c3 =
        new ProxyRepositoryComponentResult(identifier.createAlternativeVersion("1.3.0"), "c3sha1");
    List<ProxyRepositoryComponentResult> components = Arrays.asList(c1, c2, c3);
    RepositoryAllVersionsResponse mockResults = new RepositoryAllVersionsResponse(components);
    when(mockClient.getAllVersions(params)).thenReturn(mockResults);

    // when
    Pair<RepositoryAllVersionsResponse, RepositorySourceResponseDTO> results =
        repositoryQueryService.getAllVersions(identifier, app);

    // then
    assertThat(results.getLeft().getComponents()).hasSize(3).containsExactly(c1, c2, c3);
  }

  @Test
  public void testGetAllVersions_Npm_OnlyGeneric() throws Exception {
    // given
    Application app = getApplicationWithConnectionsEnabled();
    tempEntity.newRepositoryConnection(app.getId(), "baseUrl", RepositoryFormat.GENERIC, "user", "pass".toCharArray());
    tempEntity.newRepositoryConnection(app.getId(), "baseUrl2", RepositoryFormat.MAVEN, "user2", "pass2".toCharArray());
    ComponentIdentifier identifier = ComponentIdentifier.createNpmCoordinates("p1", "1.2.0");
    when(clientFactory.create()).thenReturn(mockBuilder);
    when(mockBuilder.forNexus3(eq("baseUrl"), eq("user"), any())).thenReturn(mockClient);
    Map<String, String> params = ImmutableMap.of("name", "p1");
    ProxyRepositoryComponentResult c1 =
        new ProxyRepositoryComponentResult(identifier.createAlternativeVersion("1.1.0"), "c1sha1");
    ProxyRepositoryComponentResult c2 =
        new ProxyRepositoryComponentResult(identifier.createAlternativeVersion("1.2.0"), "c2sha1");
    ProxyRepositoryComponentResult c3 =
        new ProxyRepositoryComponentResult(identifier.createAlternativeVersion("1.3.0"), "c3sha1");
    List<ProxyRepositoryComponentResult> components = Arrays.asList(c1, c2, c3);
    RepositoryAllVersionsResponse mockResults = new RepositoryAllVersionsResponse(components);
    when(mockClient.getAllVersions(params)).thenReturn(mockResults);

    // when
    Pair<RepositoryAllVersionsResponse, RepositorySourceResponseDTO> results =
        repositoryQueryService.getAllVersions(identifier, app);

    // then
    assertThat(results.getLeft().getComponents()).hasSize(3).containsExactly(c1, c2, c3);
    assertThat(results.getRight().source).isEqualTo("baseUrl");
    assertThat(results.getRight().sourceMessage).isNull();
  }

  @Test
  public void testGetAllVersions_Npm_DoesNotMix() {
    // given
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    tempEntity.newRepositoryConnection(org.getId(), "baseUrl1", RepositoryFormat.NPM, "user", "pass".toCharArray());
    tempEntity.newRepositoryConnection(app.getId(), "baseUrl2", RepositoryFormat.MAVEN, "user2", "pass2".toCharArray());
    ComponentIdentifier identifier = ComponentIdentifier.createNpmCoordinates("p1", "1.2.0");

    // when
    Pair<RepositoryAllVersionsResponse, RepositorySourceResponseDTO> results =
        repositoryQueryService.getAllVersions(identifier, app);

    // then
    assertThat(results.getLeft().getComponents()).isEmpty();
    assertThat(results.getRight()).isNull();
  }

  @Test
  public void testGetAllVersions_unsupportedFormat() {
    // given
    Application app = tempEntity.newApplicationWithParent();
    Map<String, String> coords = ImmutableMap.of("name", "n1", "version", "1.1.0");
    ComponentIdentifier identifier = new ComponentIdentifier("unknown", coords);

    // when
    Pair<RepositoryAllVersionsResponse, RepositorySourceResponseDTO> results =
        repositoryQueryService.getAllVersions(identifier, app);

    // then
    assertThat(results.getLeft().getComponents()).isEmpty();
  }

  @Test
  public void testGetAllVersions_noApplicableConnections() {
    // given
    Application app = tempEntity.newApplicationWithParent();
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "n1", "1.2.0", "", "jar");

    // when
    Pair<RepositoryAllVersionsResponse, RepositorySourceResponseDTO> results =
        repositoryQueryService.getAllVersions(identifier, app);

    // then
    assertThat(results.getLeft().getComponents()).isEmpty();
    assertThat(results.getRight()).isNull();
  }

  @Test
  public void testGetAllVersions_RepositoryApiError() throws Exception {
    // given
    Application app = getApplicationWithConnectionsEnabled();
    tempEntity.newRepositoryConnection(app.getId(), "baseUrl", "user", "pass".toCharArray());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "n1", "1.2.0", "", "jar");
    Map<String, String> params =
        ImmutableMap.of("group", "g1", "name", "n1", "maven.extension", "jar", "maven.classifier", "");
    when(clientFactory.create()).thenReturn(mockBuilder);
    when(mockBuilder.forNexus3(eq("baseUrl"), eq("user"), any())).thenReturn(mockClient);
    when(mockClient.getAllVersions(params)).thenThrow(new IOException("error"));

    // when
    Pair<RepositoryAllVersionsResponse, RepositorySourceResponseDTO> results =
        repositoryQueryService.getAllVersions(identifier, app);

    // then
    assertThat(results.getLeft().getComponents()).isEmpty();
    assertThat(results.getRight().source).isEqualTo("baseUrl");
    assertThat(results.getRight().sourceMessage).isEqualTo(
        "Could not retrieve data from InnerSource repository. Check your repository configuration.");
  }

  @Test
  public void testGetAllVersions_NoResults() throws Exception {
    // given
    Application app = getApplicationWithConnectionsEnabled();
    tempEntity.newRepositoryConnection(app.getId(), "baseUrl", null, null);
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "n1", "1.2.0", "", "jar");
    Map<String, String> params =
        ImmutableMap.of("group", "g1", "name", "n1", "maven.extension", "jar", "maven.classifier", "");
    when(clientFactory.create()).thenReturn(mockBuilder);
    when(mockBuilder.forNexus3(eq("baseUrl"), any(), any())).thenReturn(mockClient);
    when(mockClient.getAllVersions(params)).thenReturn(new RepositoryAllVersionsResponse(Collections.emptyList()));

    // when
    Pair<RepositoryAllVersionsResponse, RepositorySourceResponseDTO> results =
        repositoryQueryService.getAllVersions(identifier, app);

    // then
    assertThat(results.getLeft().getComponents()).isEmpty();
    assertThat(results.getRight().source).isEqualTo("baseUrl");
    assertThat(results.getRight().sourceMessage).isEqualTo(
        "No component versions returned from InnerSource repository. This may be due to insufficient privileges.");
  }

  @Test
  public void testGetAllVersions_Maven_MissingGroupId() {
    testGetAllVersions_BadRequestException_Maven(ComponentIdentifier.MAVEN_GROUP_ID, true);
  }

  @Test
  public void testGetAllVersions_Maven_MissingArtifactId() {
    testGetAllVersions_BadRequestException_Maven(ComponentIdentifier.MAVEN_ARTIFACT_ID, true);
  }

  @Test
  public void testGetAllVersions_Maven_MissingVersion() {
    testGetAllVersions_BadRequestException_Maven(ComponentIdentifier.VERSION, false);
  }

  @Test
  public void testGetAllVersions_Maven_MissingClassifier() {
    testGetAllVersions_BadRequestException_Maven(ComponentIdentifier.MAVEN_CLASSIFIER, false);
  }

  @Test
  public void testGetAllVersions_Maven_MissingExtension() {
    testGetAllVersions_BadRequestException_Maven(ComponentIdentifier.MAVEN_EXTENSION, false);
  }

  @Test
  public void testGetAllVersions_Npm_MissingPackageId() {
    testGetAllVersions_BadRequestException_Npm(ComponentIdentifier.NPM_PACKAGE_ID, true);
  }

  @Test
  public void testGetAllVersions_Npm_MissingVersion() {
    testGetAllVersions_BadRequestException_Npm(ComponentIdentifier.VERSION, false);
  }

  @Test
  public void testIsClusterTelemetry() {
    assertThat(repositoryQueryService.isClusterTelemetry()).isFalse();
  }

  private void testGetAllVersions_BadRequestException_Npm(
      String missingCoordinate,
      boolean expectBadRequestException)
  {
    testGetAllVersions_BadRequestException(ComponentIdentifier.createNpmCoordinates("p", "v"),
        missingCoordinate, expectBadRequestException);
  }

  @Test
  public void testShouldNotLeakDataBetweenTenants_whenMultiTenantMode() throws Exception {
    RepositoryQueryService.REPOSITORY_QUERY_COUNT_PER_FORMAT.get().clear();
    // given
    Application app = getApplicationWithConnectionsEnabled();
    tempEntity.newRepositoryConnection(app.getId(), "baseUrl", RepositoryFormat.GENERIC, "user", "pass".toCharArray());
    ComponentIdentifier maven = ComponentIdentifier.createMavenCoordinates("g1", "n1", "1.2.0", "", "jar");
    ComponentIdentifier npm1 = ComponentIdentifier.createNpmCoordinates("p1", "1.2.0");
    ComponentIdentifier npm2 = ComponentIdentifier.createNpmCoordinates("p2", "2.2.0");
    when(clientFactory.create()).thenReturn(mockBuilder);
    when(mockBuilder.forNexus3(eq("baseUrl"), eq("user"), any())).thenReturn(mockClient);
    doReturn(new RepositoryAllVersionsResponse(Collections.emptyList())).when(mockClient).getAllVersions(anyMap());

    Tenant tenant1 = testAsNewTenant(testName, t1 -> {
      // when
      repositoryQueryService.getAllVersions(maven, app);
      repositoryQueryService.getAllVersions(npm1, app);
    });

    Tenant tenant2 = testAsNewTenant(testName, t2 -> {
      repositoryQueryService.getAllVersions(npm1, app);
      repositoryQueryService.getAllVersions(npm2, app);
    });

    testAsTenant(tenant1, t1 -> {
      // then
      List<TelemetryData> telemetryData = repositoryQueryService.collectAllData();
      assertThat(telemetryData).hasSize(2);
      assertThat(telemetryData.get(0).getAttributes()).hasSize(2)
          .containsEntry(INNERSOURCE_REPOSITORY_FORMAT_KEY, "maven")
          .containsEntry(INNERSOURCE_REPOSITORY_QUERY_COUNT_KEY, 1);
      assertThat(telemetryData.get(1).getAttributes()).hasSize(2)
          .containsEntry(INNERSOURCE_REPOSITORY_FORMAT_KEY, "npm")
          .containsEntry(INNERSOURCE_REPOSITORY_QUERY_COUNT_KEY, 1);
    });

    testAsTenant(tenant2, t2 -> {
      // then
      List<TelemetryData> telemetryData = repositoryQueryService.collectAllData();
      assertThat(telemetryData).hasSize(1);
      assertThat(telemetryData.get(0).getAttributes()).hasSize(2)
          .containsEntry(INNERSOURCE_REPOSITORY_FORMAT_KEY, "npm")
          .containsEntry(INNERSOURCE_REPOSITORY_QUERY_COUNT_KEY, 2);
    });
  }

  private void testGetAllVersions_BadRequestException_Maven(
      String missingCoordinate,
      boolean expectBadRequestException)
  {
    testGetAllVersions_BadRequestException(ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"),
        missingCoordinate, expectBadRequestException);
  }

  private void testGetAllVersions_BadRequestException(
      ComponentIdentifier completeComponentIdentifier,
      String missingCoordinate,
      boolean expectBadRequestException)
  {
    Application app = tempEntity.newApplicationWithParent();
    Map<String, String> coordinates = new HashMap<>(completeComponentIdentifier.getCoordinates());
    coordinates.remove(missingCoordinate);
    ComponentIdentifier componentIdentifier =
        new ComponentIdentifier(completeComponentIdentifier.getFormat(), coordinates);

    if (expectBadRequestException) {
      assertThatExceptionOfType(BadRequestException.class).isThrownBy(
          () -> repositoryQueryService.getAllVersions(componentIdentifier, app));
    }
    else {
      assertThat(repositoryQueryService.getAllVersions(componentIdentifier, app)).isNotNull();
    }
  }

  private Application getApplicationWithConnectionsEnabled() {
    Application app = tempEntity.newApplicationWithParent();
    app.setRepositoryConnectionEnabled(true);
    applicationDAO.update(app);
    return app;
  }
}
