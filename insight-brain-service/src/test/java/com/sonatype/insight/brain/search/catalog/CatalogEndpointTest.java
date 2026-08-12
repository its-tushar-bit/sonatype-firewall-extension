/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.guide.api.dto.ComponentDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentSearchResponse;
import com.sonatype.insight.brain.guide.core.SearchApiClient;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeatureTestSupport;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.search.global.GlobalSearchRequest;
import com.sonatype.insight.brain.search.global.GlobalSearchResult;
import com.sonatype.insight.brain.search.global.IqLocalSearchService;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.security.PermissionService;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Exercises the resource layer's authz gate and JAX-RS status mapping (403/404/400 via
 * {@link ErrorResponseGenerator}). Anonymous callers are rejected upstream by the Shiro requireAuth
 * filter with 401 before they reach this resource, so there is no null-principal path to drive here;
 * that 401 contract is pinned by the full-stack REST harness integration tests. Service
 * behaviour (row mapping, warnings, paging, license 404) is covered by {@link CatalogServiceTest}.
 */
public class CatalogEndpointTest
{
  private SearchApiClient searchApiClient;

  private CurrentUser currentUser;

  private PermissionService permissionService;

  private CatalogResource resource;

  @Before
  public void setUp() {
    SystemConfigurationPropertyFeatureTestSupport.install();
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    SearchIndexClient searchIndexClient = mock(SearchIndexClient.class);
    when(searchIndexClient.isSearchPreviewEnabled()).thenReturn(true);
    when(searchIndexClient.getCurrentUserContextIdsWithReadPermission()).thenReturn(Set.of());
    when(searchIndexClient.buildAllowedContextIdsFilter(any())).thenReturn(null);
    when(searchIndexClient.wrapWithPermissionFilter(any(), any())).thenAnswer(inv -> inv.getArgument(0));
    when(searchIndexClient.buildPermittedQuery(any())).thenCallRealMethod();
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenReturn(new GlobalSearchResult(List.of(), 0, List.of()));

    searchApiClient = mock(SearchApiClient.class);
    IqLocalSearchService iq = new IqLocalSearchService(searchIndexClient);
    CatalogService service = new CatalogService(iq, searchApiClient, searchIndexClient);

    currentUser = mock(CurrentUser.class);
    permissionService = mock(PermissionService.class);
    grantRead("org-1");
    resource = new CatalogResource(service, permissionService, currentUser);
  }

  @After
  public void tearDown() {
    SystemConfigurationPropertyFeatureTestSupport.uninstall();
  }

  @Test
  public void catalogAvailable_returnsMappedRows() {
    GuideComponentDocument doc = new GuideComponentDocument(
        "npm", null, null, "react", "18.0.0", null, List.of(), List.of(), true, 90, 1.0,
        null, false, null, null);
    when(searchApiClient.searchCatalogComponents(any()))
        .thenReturn(new GuideComponentSearchResponse(List.<ComponentDocument>of(doc), 1, 0, 25, null));
    CatalogResponse response = resource.search(
        new CatalogRequest("COMPONENT", "catalog", Map.of(), 1, 25, null, null, false));
    assertThat(response.catalogAvailable()).isTrue();
    assertThat(response.rows()).hasSize(1);
  }

  @Test
  public void authenticatedWithNoReadContext_mapsTo403() {
    grantNoRead();
    assertMappedStatus(new CatalogRequest("COMPONENT", "local", Map.of(), 1, 25, null, null, false), 403);
  }

  @Test
  public void flagOff_mapsTo404() {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);
    assertMappedStatus(new CatalogRequest("COMPONENT", "local", Map.of(), 1, 25, null, null, false), 404);
  }

  @Test
  public void unknownSource_mapsTo400() {
    assertMappedStatus(new CatalogRequest("COMPONENT", "bogus", Map.of(), 1, 25, null, null, false), 400);
  }

  @Test
  public void unknownEntityType_mapsTo400() {
    assertMappedStatus(new CatalogRequest("BOGUS", "catalog", Map.of(), 1, 25, null, null, false), 400);
  }

  @Test
  public void emptyBody_mapsTo400() {
    assertMappedStatus(null, 400);
  }

  private void assertMappedStatus(final CatalogRequest request, final int expectedStatus) {
    try {
      resource.search(request);
      throw new AssertionError("expected the request to throw");
    }
    catch (RuntimeException e) {
      int status = new ErrorResponseGenerator().mapExceptionAndLog(e).getStatusCode();
      assertThat(status).isEqualTo(expectedStatus);
    }
  }

  private void grantRead(final String contextId) {
    UserPrincipal principal = mock(UserPrincipal.class);
    when(currentUser.getUserPrincipal()).thenReturn(principal);
    when(permissionService.getContextIdsForUserWithPermission(any(UserPrincipal.class), eq(Permission.READ)))
        .thenReturn(Set.of(contextId));
  }

  private void grantNoRead() {
    UserPrincipal principal = mock(UserPrincipal.class);
    when(currentUser.getUserPrincipal()).thenReturn(principal);
    when(permissionService.getContextIdsForUserWithPermission(any(UserPrincipal.class), eq(Permission.READ)))
        .thenReturn(Set.of());
  }
}
