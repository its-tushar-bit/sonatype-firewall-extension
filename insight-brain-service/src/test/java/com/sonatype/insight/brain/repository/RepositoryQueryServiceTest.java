/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

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
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class RepositoryQueryServiceTest
    extends AbstractComponentTest
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

  @Before
  public void before() {
    repositoryQueryService = new RepositoryQueryService(clientFactory, passwordHandler, dao,
        repositoryConnectionService);
  }

  @After
  public void after() {
    Organization rootOrganization = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    rootOrganization.setAllowRepositoryConnectionOverride(true);
    rootOrganization.setRepositoryConnectionEnabled(true);
    organizationDAO.update(rootOrganization);
  }

  @Test
  public void testGetAllVersions_Maven() throws Exception {
    //given
    Application app = getApplicationWithConnectionsEnabled();
    tempEntity.newRepositoryConnection(app.getId(), "baseUrl", RepositoryFormat.MAVEN, "user", "pass".toCharArray());
    tempEntity.newRepositoryConnection(app.getId(), "baseUrl2", RepositoryFormat.GENERIC, "user", "pass".toCharArray());
    tempEntity.newRepositoryConnection(app.getId(), "baseUrl3", RepositoryFormat.NPM, "user2", "pass2".toCharArray());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "n1", "1.2.0", "", "jar");
    when(clientFactory.create()).thenReturn(mockBuilder);
    when(mockBuilder.forNexus3(eq("baseUrl"), eq("user"), any())).thenReturn(mockClient);
    Map<String, String> params =
        ImmutableMap.of("group", "g1", "name", "n1", "maven.extension", "jar", "maven.classifier", "");
    RepositoryComponentResult c1 =
        new RepositoryComponentResult(identifier.createAlternativeVersion("1.1.0"), "c1sha1");
    RepositoryComponentResult c2 =
        new RepositoryComponentResult(identifier.createAlternativeVersion("1.2.0"), "c2sha1");
    RepositoryComponentResult c3 =
        new RepositoryComponentResult(identifier.createAlternativeVersion("1.3.0"), "c3sha1");
    List<RepositoryComponentResult> components = Arrays.asList(c1, c2, c3);
    RepositoryAllVersionsResponse mockResults = new RepositoryAllVersionsResponse(components);
    when(mockClient.getAllVersions(params)).thenReturn(mockResults);

    //when
    Pair<RepositoryAllVersionsResponse, RepositorySourceResponseDTO> results =
        repositoryQueryService.getAllVersions(identifier, app);

    //then
    assertThat(results.getLeft().getComponents()).hasSize(3).containsExactly(c1, c2, c3);
  }

  @Test
  public void testGetAllVersions_Inherited_Org() throws Exception {
    //given
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
    RepositoryComponentResult c1 =
        new RepositoryComponentResult(identifier.createAlternativeVersion("1.1.0"), "c1sha1");
    RepositoryComponentResult c2 =
        new RepositoryComponentResult(identifier.createAlternativeVersion("1.2.0"), "c2sha1");
    RepositoryComponentResult c3 =
        new RepositoryComponentResult(identifier.createAlternativeVersion("1.3.0"), "c3sha1");
    List<RepositoryComponentResult> components = Arrays.asList(c1, c2, c3);
    RepositoryAllVersionsResponse mockResults = new RepositoryAllVersionsResponse(components);
    when(mockClient.getAllVersions(params)).thenReturn(mockResults);

    //when
    Pair<RepositoryAllVersionsResponse, RepositorySourceResponseDTO> results =
        repositoryQueryService.getAllVersions(identifier, app);

    //then
    assertThat(results.getLeft().getComponents()).hasSize(3).containsExactly(c1, c2, c3);
  }

  @Test
  public void testGetAllVersions_Maven_OnlyGeneric() throws Exception {
    //given
    Application app = getApplicationWithConnectionsEnabled();
    tempEntity.newRepositoryConnection(app.getId(), "baseUrl", RepositoryFormat.GENERIC, "user", "pass".toCharArray());
    tempEntity.newRepositoryConnection(app.getId(), "baseUrl2", RepositoryFormat.NPM, "user2", "pass2".toCharArray());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "n1", "1.2.0", "", "jar");
    when(clientFactory.create()).thenReturn(mockBuilder);
    when(mockBuilder.forNexus3(eq("baseUrl"), eq("user"), any())).thenReturn(mockClient);
    Map<String, String> params =
        ImmutableMap.of("group", "g1", "name", "n1", "maven.extension", "jar", "maven.classifier", "");
    RepositoryComponentResult c1 =
        new RepositoryComponentResult(identifier.createAlternativeVersion("1.1.0"), "c1sha1");
    RepositoryComponentResult c2 =
        new RepositoryComponentResult(identifier.createAlternativeVersion("1.2.0"), "c2sha1");
    RepositoryComponentResult c3 =
        new RepositoryComponentResult(identifier.createAlternativeVersion("1.3.0"), "c3sha1");
    List<RepositoryComponentResult> components = Arrays.asList(c1, c2, c3);
    RepositoryAllVersionsResponse mockResults = new RepositoryAllVersionsResponse(components);
    when(mockClient.getAllVersions(params)).thenReturn(mockResults);

    //when
    Pair<RepositoryAllVersionsResponse, RepositorySourceResponseDTO> results =
        repositoryQueryService.getAllVersions(identifier, app);

    //then
    assertThat(results.getLeft().getComponents()).hasSize(3).containsExactly(c1, c2, c3);
  }

  @Test
  public void testGetAllVersions_Maven_DoesNotMix() {
    //given
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    tempEntity.newRepositoryConnection(org.getId(), "baseUrl1", RepositoryFormat.MAVEN, "user", "pass".toCharArray());
    tempEntity.newRepositoryConnection(app.getId(), "baseUrl2", RepositoryFormat.NPM, "user2", "pass2".toCharArray());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "n1", "1.2.0", "", "jar");

    //when
    Pair<RepositoryAllVersionsResponse, RepositorySourceResponseDTO> results =
        repositoryQueryService.getAllVersions(identifier, app);

    //then
    assertThat(results.getLeft().getComponents()).isEmpty();
  }

  @Test
  public void testGetAllVersions_Npm() throws Exception {
    //given
    Application app = getApplicationWithConnectionsEnabled();
    tempEntity.newRepositoryConnection(app.getId(), "baseUrl", RepositoryFormat.NPM, "user", "pass".toCharArray());
    tempEntity.newRepositoryConnection(app.getId(), "baseUrl2", RepositoryFormat.GENERIC, "user", "pass".toCharArray());
    tempEntity.newRepositoryConnection(app.getId(), "baseUrl3", RepositoryFormat.MAVEN, "user2", "pass2".toCharArray());
    ComponentIdentifier identifier = ComponentIdentifier.createNpmCoordinates("p1", "1.2.0");
    when(clientFactory.create()).thenReturn(mockBuilder);
    when(mockBuilder.forNexus3(eq("baseUrl"), eq("user"), any())).thenReturn(mockClient);
    Map<String, String> params = ImmutableMap.of("name", "p1");
    RepositoryComponentResult c1 =
        new RepositoryComponentResult(identifier.createAlternativeVersion("1.1.0"), "c1sha1");
    RepositoryComponentResult c2 =
        new RepositoryComponentResult(identifier.createAlternativeVersion("1.2.0"), "c2sha1");
    RepositoryComponentResult c3 =
        new RepositoryComponentResult(identifier.createAlternativeVersion("1.3.0"), "c3sha1");
    List<RepositoryComponentResult> components = Arrays.asList(c1, c2, c3);
    RepositoryAllVersionsResponse mockResults = new RepositoryAllVersionsResponse(components);
    when(mockClient.getAllVersions(params)).thenReturn(mockResults);

    //when
    Pair<RepositoryAllVersionsResponse, RepositorySourceResponseDTO> results =
        repositoryQueryService.getAllVersions(identifier, app);

    //then
    assertThat(results.getLeft().getComponents()).hasSize(3).containsExactly(c1, c2, c3);
  }

  @Test
  public void testGetAllVersions_Npm_OnlyGeneric() throws Exception {
    //given
    Application app = getApplicationWithConnectionsEnabled();
    tempEntity.newRepositoryConnection(app.getId(), "baseUrl", RepositoryFormat.GENERIC, "user", "pass".toCharArray());
    tempEntity.newRepositoryConnection(app.getId(), "baseUrl2", RepositoryFormat.MAVEN, "user2", "pass2".toCharArray());
    ComponentIdentifier identifier = ComponentIdentifier.createNpmCoordinates("p1", "1.2.0");
    when(clientFactory.create()).thenReturn(mockBuilder);
    when(mockBuilder.forNexus3(eq("baseUrl"), eq("user"), any())).thenReturn(mockClient);
    Map<String, String> params = ImmutableMap.of("name", "p1");
    RepositoryComponentResult c1 =
        new RepositoryComponentResult(identifier.createAlternativeVersion("1.1.0"), "c1sha1");
    RepositoryComponentResult c2 =
        new RepositoryComponentResult(identifier.createAlternativeVersion("1.2.0"), "c2sha1");
    RepositoryComponentResult c3 =
        new RepositoryComponentResult(identifier.createAlternativeVersion("1.3.0"), "c3sha1");
    List<RepositoryComponentResult> components = Arrays.asList(c1, c2, c3);
    RepositoryAllVersionsResponse mockResults = new RepositoryAllVersionsResponse(components);
    when(mockClient.getAllVersions(params)).thenReturn(mockResults);

    //when
    Pair<RepositoryAllVersionsResponse, RepositorySourceResponseDTO> results =
        repositoryQueryService.getAllVersions(identifier, app);

    //then
    assertThat(results.getLeft().getComponents()).hasSize(3).containsExactly(c1, c2, c3);
    assertThat(results.getRight().source).isEqualTo("baseUrl");
    assertThat(results.getRight().sourceError).isNull();
  }

  @Test
  public void testGetAllVersions_Npm_DoesNotMix() {
    //given
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    tempEntity.newRepositoryConnection(org.getId(), "baseUrl1", RepositoryFormat.NPM, "user", "pass".toCharArray());
    tempEntity.newRepositoryConnection(app.getId(), "baseUrl2", RepositoryFormat.MAVEN, "user2", "pass2".toCharArray());
    ComponentIdentifier identifier = ComponentIdentifier.createNpmCoordinates("p1", "1.2.0");

    //when
    Pair<RepositoryAllVersionsResponse, RepositorySourceResponseDTO> results =
        repositoryQueryService.getAllVersions(identifier, app);

    //then
    assertThat(results.getLeft().getComponents()).isEmpty();
    assertThat(results.getRight()).isNull();
  }

  @Test
  public void testGetAllVersions_unsupportedFormat() {
    //given
    Application app = tempEntity.newApplicationWithParent();
    Map<String, String> coords = ImmutableMap.of("name", "n1", "version", "1.1.0");
    ComponentIdentifier identifier = new ComponentIdentifier("unknown", coords);

    //when
    Pair<RepositoryAllVersionsResponse, RepositorySourceResponseDTO> results =
        repositoryQueryService.getAllVersions(identifier, app);

    //then
    assertThat(results.getLeft().getComponents()).isEmpty();
  }

  @Test
  public void testGetAllVersions_noApplicableConnections() {
    //given
    Application app = tempEntity.newApplicationWithParent();
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "n1", "1.2.0", "", "jar");

    //when
    Pair<RepositoryAllVersionsResponse, RepositorySourceResponseDTO> results =
        repositoryQueryService.getAllVersions(identifier, app);

    //then
    assertThat(results.getLeft().getComponents()).isEmpty();
    assertThat(results.getRight()).isNull();
  }

  @Test
  public void testGetAllVersions_RepositoryApiError() throws Exception {
    //given
    Application app = getApplicationWithConnectionsEnabled();
    tempEntity.newRepositoryConnection(app.getId(), "baseUrl", "user", "pass".toCharArray());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "n1", "1.2.0", "", "jar");
    Map<String, String> params =
        ImmutableMap.of("group", "g1", "name", "n1", "maven.extension", "jar", "maven.classifier", "");
    when(clientFactory.create()).thenReturn(mockBuilder);
    when(mockBuilder.forNexus3(eq("baseUrl"), eq("user"), any())).thenReturn(mockClient);
    when(mockClient.getAllVersions(params)).thenThrow(new IOException("error"));

    //when
    Pair<RepositoryAllVersionsResponse, RepositorySourceResponseDTO> results =
        repositoryQueryService.getAllVersions(identifier, app);

    //then
    assertThat(results.getLeft().getComponents()).isEmpty();
    assertThat(results.getRight().source).isEqualTo("baseUrl");
    assertThat(results.getRight().sourceError).isEqualTo(
        "unable to retrieve component versions from repository manager: baseUrl");
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

  private void testGetAllVersions_BadRequestException_Npm(
      String missingCoordinate,
      boolean expectBadRequestException)
  {
    testGetAllVersions_BadRequestException(ComponentIdentifier.createNpmCoordinates("p", "v"),
        missingCoordinate, expectBadRequestException);
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
