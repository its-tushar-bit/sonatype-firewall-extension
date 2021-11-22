/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryConnectionDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.repository.client.RepositoryClientFactory;
import com.sonatype.insight.brain.repository.client.RepositoryClientFactory.RepositoryClientBuilder;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class RepositoryQueryServiceTest
    extends AbstractComponentTest
{
  private RepositoryQueryService repositoryQueryService;

  @Inject
  private RepositoryConnectionDAO dao;

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
    repositoryQueryService = new RepositoryQueryService(clientFactory, passwordHandler, dao);
  }

  @Test
  public void testGetAllVersions_Maven() throws Exception {
    //given
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newRepositoryConnection(app.getId(), "baseUrl", "user", "pass".toCharArray());
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
    RepositoryAllVersionsResponse results = repositoryQueryService.getAllVersions(identifier, app.getId());

    //then
    assertThat(results.getComponents()).hasSize(3).containsExactly(c1, c2, c3);
  }

  @Test
  public void testGetAllVersions_Npm() throws Exception {
    //given
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newRepositoryConnection(app.getId(), "baseUrl", "user", "pass".toCharArray());
    ComponentIdentifier identifier = ComponentIdentifier.createNpmCoordinates("p1", "1.2.0");
    when(clientFactory.create()).thenReturn(mockBuilder);
    when(mockBuilder.forNexus3(eq("baseUrl"), eq("user"), any())).thenReturn(mockClient);
    Map<String, String> params = ImmutableMap.of( "name", "p1");
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
    RepositoryAllVersionsResponse results = repositoryQueryService.getAllVersions(identifier, app.getId());

    //then
    assertThat(results.getComponents()).hasSize(3).containsExactly(c1, c2, c3);
  }

  @Test
  public void testGetAllVersions_unsupportedFormat() {
    //given
    Application app = tempEntity.newApplicationWithParent();
    Map<String, String> coords = ImmutableMap.of("name", "n1", "version", "1.1.0");
    ComponentIdentifier identifier = new ComponentIdentifier("unknown", coords);

    //when
    RepositoryAllVersionsResponse results = repositoryQueryService.getAllVersions(identifier, app.getId());

    //then
    assertThat(results.getComponents()).isEmpty();
  }

  @Test
  public void testGetAllVersions_noApplicableConnections() {
    //given
    Application app = tempEntity.newApplicationWithParent();
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "n1", "1.2.0", "", "jar");

    //when
    RepositoryAllVersionsResponse results = repositoryQueryService.getAllVersions(identifier, app.getId());

    //then
    assertThat(results.getComponents()).isEmpty();
  }

  @Test
  public void testGetAllVersions_RepositoryApiError() throws Exception {
    //given
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newRepositoryConnection(app.getId(), "baseUrl", "user", "pass".toCharArray());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "n1", "1.2.0", "", "jar");
    Map<String, String> params =
        ImmutableMap.of("group", "g1", "name", "n1", "maven.extension", "jar", "maven.classifier", "");
    when(clientFactory.create()).thenReturn(mockBuilder);
    when(mockBuilder.forNexus3(eq("baseUrl"), eq("user"), any())).thenReturn(mockClient);
    when(mockClient.getAllVersions(params)).thenThrow(new IOException("error"));

    //when
    RepositoryAllVersionsResponse results = repositoryQueryService.getAllVersions(identifier, app.getId());

    //then
    assertThat(results.getComponents()).isEmpty();
  }
}
