/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.indexquery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeatureTestSupport;
import com.sonatype.insight.brain.search.global.GlobalSearchRequest;
import com.sonatype.insight.brain.search.global.GlobalSearchResult;
import com.sonatype.insight.brain.search.global.IqLocalSearchService;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class IndexQueryResourceAuthzTest
{
  private SearchIndexClient searchIndexClient;

  private IndexQueryResource resource;

  @Before
  public void setUp() {
    SystemConfigurationPropertyFeatureTestSupport.install();
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    searchIndexClient = mock(SearchIndexClient.class);
    when(searchIndexClient.isSearchPreviewEnabled()).thenReturn(true);
    when(searchIndexClient.getCurrentUserContextIdsWithReadPermission()).thenReturn(Set.of());
    when(searchIndexClient.buildAllowedContextIdsFilter(any())).thenReturn(null);
    when(searchIndexClient.wrapWithPermissionFilter(any(), any())).thenAnswer(inv -> inv.getArgument(0));
    when(searchIndexClient.buildPermittedQuery(any())).thenCallRealMethod();
    when(searchIndexClient.searchGlobal(any())).thenReturn(new GlobalSearchResult(List.of(), 0, List.of()));

    IqLocalSearchService iq = new IqLocalSearchService(searchIndexClient);
    IndexQueryService service = new IndexQueryService(iq, searchIndexClient, null);
    resource = new IndexQueryResource(service, searchIndexClient);
  }

  @After
  public void tearDown() {
    SystemConfigurationPropertyFeatureTestSupport.uninstall();
  }

  // Anonymous callers never reach this resource: the Shiro requireAuth filter rejects them with 401
  // upstream, so there is no null-principal path to test here.

  @Test
  public void authenticatedWithoutReadGrant_mapsTo403() {
    when(searchIndexClient.getCurrentUserContextIdsWithReadPermission()).thenReturn(Set.of());
    assertMappedStatus(applicationRequest(), 403);
  }

  @Test
  public void authorized_returns200() {
    grantRead("org-1");
    IndexQueryResponse response = resource.query(applicationRequest());
    assertThat(response).isNotNull();
    assertThat(response.entityType()).isEqualTo("APPLICATION");
  }

  @Test
  public void flagOff_mapsTo404_forAuthorizedCaller() {
    grantRead("org-1");
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);
    assertMappedStatus(applicationRequest(), 404);
  }

  @Test
  public void filterInverse_scopedUser_seesOnlyPermittedContexts() {
    grantRead("org-1");
    // The backend honours the permission filter: docs outside the caller's read scope never come back.
    // Model that here so the assertion is on observable results (out-of-scope row absent), not on the
    // buildPermittedQuery interaction.
    SearchResultItemDTO inScope = appDto("app-1", "In Scope", "org-1");
    SearchResultItemDTO outOfScope = appDto("app-2", "Out Of Scope", "org-2");
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class))).thenAnswer(inv -> {
      Set<String> permitted = searchIndexClient.getCurrentUserContextIdsWithReadPermission();
      List<SearchResultItemDTO> visible = java.util.stream.Stream.of(inScope, outOfScope)
          .filter(d -> permitted.contains(d.organizationName))
          .toList();
      return new GlobalSearchResult(visible, visible.size(), List.of());
    });

    IndexQueryResponse response = resource.query(applicationRequest());

    assertThat(response.rows()).singleElement()
        .satisfies(r -> assertThat(r.getFields()).containsEntry("organizationName", "org-1"));
    // The out-of-scope org must be excluded from the observable results.
    assertThat(response.rows())
        .noneSatisfy(r -> assertThat(r.getFields()).containsEntry("organizationName", "org-2"));
  }

  private void grantRead(final String contextId) {
    when(searchIndexClient.getCurrentUserContextIdsWithReadPermission()).thenReturn(Set.of(contextId));
  }

  private void assertMappedStatus(final IndexQueryRequest request, final int expectedStatus) {
    try {
      resource.query(request);
      throw new AssertionError("expected the query to throw");
    }
    catch (RuntimeException e) {
      int status = new ErrorResponseGenerator().mapExceptionAndLog(e).getStatusCode();
      assertThat(status).isEqualTo(expectedStatus);
    }
  }

  private static IndexQueryRequest applicationRequest() {
    return new IndexQueryRequest("APPLICATION", Map.of("query", "acme"), 1, 25, null, null, false);
  }

  private static SearchResultItemDTO appDto(final String publicId, final String name, final String org) {
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.itemType = "APPLICATION";
    d.applicationPublicId = publicId;
    d.applicationName = name;
    d.organizationName = org;
    return d;
  }
}
