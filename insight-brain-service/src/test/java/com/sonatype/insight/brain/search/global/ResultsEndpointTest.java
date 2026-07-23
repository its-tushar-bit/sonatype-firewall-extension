/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import com.sonatype.insight.brain.search.global.catalog.GlobalSearchResultsCatalogClient;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import java.util.Set;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeatureTestSupport;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit-level wiring test for {@code /rest/search/results}: real {@link ResultsService} + real
 * {@link GlobalSearchResource} + registered exception mappers, invoked by calling the resource methods
 * directly (no servlet container). Verifies flag gating, stale-cursor mapping, and filter validation.
 * The framework-level path (Shiro filter, JAX-RS binding, @Provider mapper dispatch) is covered by
 * {@link ResultsResourceAuthzTest}.
 */
public class ResultsEndpointTest
{
  private FakeGlobalSearchResultsIqLocalClient iq;

  private FakeCatalogClient catalog;

  private GlobalSearchResource resource;

  private ResultsService service;

  @Before
  public void setUp() {
    SystemConfigurationPropertyFeatureTestSupport.install();
    SystemConfigurationPropertyFeature.GLOBAL_SEARCH.setEnabled(true);

    iq = new FakeGlobalSearchResultsIqLocalClient();
    catalog = new FakeCatalogClient();
    service = new ResultsService(iq, catalog);
    // Default: authenticated caller with a scoped READ grant so the resource's runtime
    // verifyReadOnAnyContext() gate passes and the test focuses on flag / cursor / filter behaviour.
    SearchIndexClient searchIndexClient = org.mockito.Mockito.mock(SearchIndexClient.class);
    org.mockito.Mockito.when(searchIndexClient.getCurrentUserContextIdsWithReadPermission())
        .thenReturn(Set.of("org-1"));
    resource = new GlobalSearchResource(service, searchIndexClient);
  }

  @After
  public void tearDown() {
    SystemConfigurationPropertyFeatureTestSupport.uninstall();
  }

  @Test
  public void flagOff_returns404() {
    SystemConfigurationPropertyFeature.GLOBAL_SEARCH.setEnabled(false);
    assertThatThrownBy(() -> resource.getResults("q", "APPLICATION", 1, 25, null, null, null))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void staleCursor_isMappedTo410WithRetryHeader() {
    // Build an outer ALL-tab cursor and bump the generation so decode fails.
    Map<Tab, AllTabCursor.SectionCursor> sections = new EnumMap<>(Tab.class);
    sections.put(Tab.COMPONENT, AllTabCursor.SectionCursor.nonExhausted(null, 1));
    String encoded = new AllTabCursor(null, 25, sections).encode();
    String saved = GlobalSearchCursor.currentGenerationToken();
    try {
      GlobalSearchCursor.bumpGenerationToken("g-rotated-after-reindex");

      StaleCursorException thrown = null;
      try {
        resource.getResults("q", "ALL", 1, 25, null, encoded, null);
      }
      catch (StaleCursorException e) {
        thrown = e;
      }
      assertThat(thrown).isNotNull();

      // Verify the mapper produces the 410 + retry header shape.
      Response response = new StaleCursorExceptionMapper().toResponse(thrown);
      assertThat(response.getStatus()).isEqualTo(410);
      assertThat(response.getHeaderString(StaleCursorExceptionMapper.RETRY_HINT_HEADER))
          .isEqualTo(StaleCursorExceptionMapper.RETRY_HINT_VALUE);
    }
    finally {
      GlobalSearchCursor.bumpGenerationToken(saved);
    }
  }

  @Test
  public void unknownSort_isMappedTo400WithGenericBody() {
    FilterValidationException thrown = null;
    try {
      resource.getResults("q", "COMPONENT", 1, 25, "noSuchSort", null, null);
    }
    catch (FilterValidationException e) {
      thrown = e;
    }
    assertThat(thrown).isNotNull();

    Response response = new FilterValidationExceptionMapper().toResponse(thrown);
    assertThat(response.getStatus()).isEqualTo(400);
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getEntity();
    assertThat(body).containsEntry("code", "SORT_NOT_ALLOWED");
    assertThat(body).containsEntry("message", FilterValidationException.Code.SORT_NOT_ALLOWED.clientMessage());
    // The response body must not echo the caller-supplied sort key or the allowlist.
    assertThat(body.get("message").toString()).doesNotContain("noSuchSort");
    assertThat(body.get("message").toString()).doesNotContain("Allowed");
  }

  @Test
  public void validRequest_delegatesToService_iqLocalTab() {
    iq.registerRow(Tab.APPLICATION,
        ResultRow.builder().type("APPLICATION").source(SearchSource.LOCAL.value()).id("a1").title("App 1").build());
    ResultsResponse response =
        (ResultsResponse) resource.getResults("q", "APPLICATION", 1, 25, null, null, null).getEntity();
    assertThat(response).isNotNull();
    assertThat(response.getResults()).hasSize(1);
  }

  private static final class FakeGlobalSearchResultsIqLocalClient
      implements GlobalSearchResultsIqLocalClient
  {
    private final Map<Tab, List<ResultRow>> byTab = new EnumMap<>(Tab.class);

    void registerRow(Tab tab, ResultRow row) {
      byTab.computeIfAbsent(tab, t -> new java.util.ArrayList<>()).add(row);
    }

    @Override
    public java.util.Optional<SectionResult> searchNative(ResultsRequest request) {
      List<ResultRow> rows = byTab.getOrDefault(request.getTab(), List.of());
      return java.util.Optional.of(new SectionResult(request.getTab(), rows, rows.size(), null, true));
    }
  }

  private static final class FakeCatalogClient
      implements GlobalSearchResultsCatalogClient
  {
    @Override
    public java.util.Optional<SectionResult> searchResults(ResultsRequest request) {
      return java.util.Optional.empty();
    }

    @Override
    public boolean isEnabled() {
      return false;
    }
  }
}
