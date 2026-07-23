/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeatureTestSupport;
import com.sonatype.insight.brain.search.index.SearchIndexClient;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the JAX-RS surface of {@link GlobalSearchResource} — flag gating, manual
 * query-string validation, the read-context authorization gate, and delegation to
 * {@link ResultsService} for {@code /rest/search/results}.
 *
 * <p>
 * Anonymous callers never reach the resource: the Shiro requireAuth filter rejects them with 401
 * upstream, so there is no null-principal path to test here — an authenticated caller with no
 * readable context is the 403 case.
 */
@RunWith(MockitoJUnitRunner.class)
public class GlobalSearchResourceTest
{
  @Mock
  private ResultsService resultsService;

  @Mock
  private SearchIndexClient searchIndexClient;

  private GlobalSearchResource underTest;

  @Before
  public void setUp() {
    SystemConfigurationPropertyFeatureTestSupport.install();
    underTest = new GlobalSearchResource(
        resultsService, org.mockito.Mockito.mock(SuggestService.class), searchIndexClient);
  }

  @After
  public void tearDown() {
    SystemConfigurationPropertyFeatureTestSupport.uninstall();
  }

  /** Authenticated caller with READ on a single org context. */
  private void stubAuthorizedCaller() {
    when(searchIndexClient.getCurrentUserContextIdsWithReadPermission()).thenReturn(Set.of("org-1"));
  }

  @Test
  public void results_flagOff_throwsNotFoundAndNeverDelegates() {
    // GLOBAL_SEARCH default is OFF (enabledWhenAbsent=false, no property row written).
    assertThatThrownBy(() -> underTest.getResults("q", "APPLICATION", 1, 25, null, null, null))
        .isInstanceOf(NotFoundException.class);
    verify(resultsService, never()).search(any());
  }

  @Test
  public void results_flagOff_missingQuery_returns404NotBadRequest() {
    // The flag gate must run before input validation so a flag-off endpoint cannot leak its existence
    // via a 400 response.
    assertThatThrownBy(() -> underTest.getResults(null, "APPLICATION", 1, 25, null, null, null))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void results_flagOff_oversizeQuery_returns404NotBadRequest() {
    String oversize = "x".repeat(GlobalSearchResource.MAX_QUERY_LENGTH + 1);
    assertThatThrownBy(() -> underTest.getResults(oversize, "APPLICATION", 1, 25, null, null, null))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void results_flagOff_invalidTab_returns404NotBadRequest() {
    assertThatThrownBy(() -> underTest.getResults("q", "NOT_A_REAL_TAB", 1, 25, null, null, null))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void results_flagOff_negativePage_returns404NotBadRequest() {
    assertThatThrownBy(() -> underTest.getResults("q", "APPLICATION", -1, 25, null, null, null))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void results_flagOn_missingQuery_returns400() {
    SystemConfigurationPropertyFeature.GLOBAL_SEARCH.setEnabled(true);
    stubAuthorizedCaller();
    assertThatThrownBy(() -> underTest.getResults(null, "APPLICATION", 1, 25, null, null, null))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void results_flagOn_invalidTab_returns400_withoutEchoingInput() {
    SystemConfigurationPropertyFeature.GLOBAL_SEARCH.setEnabled(true);
    stubAuthorizedCaller();
    assertThatThrownBy(() -> underTest.getResults("q", "NOT_A_REAL_TAB", 1, 25, null, null, null))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("unknown tab")
        .hasMessageNotContaining("NOT_A_REAL_TAB");
  }

  @Test
  public void results_flagOn_oversizeTab_returns400_withoutEchoingInput() {
    SystemConfigurationPropertyFeature.GLOBAL_SEARCH.setEnabled(true);
    stubAuthorizedCaller();
    String oversize = "A".repeat(GlobalSearchResource.MAX_TAB_LENGTH + 1);
    assertThatThrownBy(() -> underTest.getResults("q", oversize, 1, 25, null, null, null))
        .isInstanceOf(BadRequestException.class)
        .hasMessageNotContaining(oversize);
    verify(resultsService, never()).search(any());
  }

  @Test
  public void results_flagOn_oversizeSort_returns400_withoutEchoingInput() {
    SystemConfigurationPropertyFeature.GLOBAL_SEARCH.setEnabled(true);
    stubAuthorizedCaller();
    String oversize = "A".repeat(GlobalSearchResource.MAX_SORT_LENGTH + 1);
    assertThatThrownBy(() -> underTest.getResults("q", "APPLICATION", 1, 25, oversize, null, null))
        .isInstanceOf(BadRequestException.class)
        .hasMessageNotContaining(oversize);
    verify(resultsService, never()).search(any());
  }

  @Test
  public void results_flagOn_withControlChars_returns400() {
    // Control-char validation must reject newline, tab, and embedded NUL. Every one of these
    // slipping through would allow header injection or garbled query logs.
    SystemConfigurationPropertyFeature.GLOBAL_SEARCH.setEnabled(true);
    stubAuthorizedCaller();
    assertThatThrownBy(() -> underTest.getResults("alpha\nbeta", "APPLICATION", 1, 25, null, null, null))
        .isInstanceOf(BadRequestException.class);
    assertThatThrownBy(() -> underTest.getResults("alpha\u0000beta", "APPLICATION", 1, 25, null, null, null))
        .isInstanceOf(BadRequestException.class);
    verify(resultsService, never()).search(any());
  }

  @Test
  public void results_flagOn_validInput_delegatesToService() {
    SystemConfigurationPropertyFeature.GLOBAL_SEARCH.setEnabled(true);
    stubAuthorizedCaller();
    ResultsResponse expected = new ResultsResponse(Tab.APPLICATION, 1, 25, 0L, List.of(), null);
    when(resultsService.search(any())).thenReturn(expected);

    ResultsResponse actual =
        (ResultsResponse) underTest.getResults("q", "APPLICATION", 1, 25, null, null, null).getEntity();
    assertThat(actual).isSameAs(expected);
  }

  @Test
  public void results_flagOn_scopedUserWithOrgGrant_isAuthorized() {
    // Non-admin user with READ on {org-1, app-1} (no global sentinel) passes the read-context gate;
    // the service layer applies per-principal row filtering downstream.
    SystemConfigurationPropertyFeature.GLOBAL_SEARCH.setEnabled(true);
    when(searchIndexClient.getCurrentUserContextIdsWithReadPermission()).thenReturn(Set.of("org-1", "app-1"));
    ResultsResponse expected = new ResultsResponse(Tab.APPLICATION, 1, 25, 0L, List.of(), null);
    when(resultsService.search(any())).thenReturn(expected);

    ResultsResponse actual =
        (ResultsResponse) underTest.getResults("q", "APPLICATION", 1, 25, null, null, null).getEntity();

    assertThat(actual).isSameAs(expected);
    verify(resultsService).search(any());
  }

  @Test
  public void results_warnings_flowThroughXSearchWarningsHeader() {
    // Compiler warnings from the AST pipeline surface both inline (in the JSON body) and via the
    // X-Search-Warnings response header, ASCII-encoded so the header write cannot crash on
    // non-ASCII characters (em-dashes, accented field names, etc.).
    SystemConfigurationPropertyFeature.GLOBAL_SEARCH.setEnabled(true);
    stubAuthorizedCaller();
    ResultsResponse expected = new ResultsResponse(
        Tab.APPLICATION, 1, 25, 0L, List.of(), null,
        List.of("Unknown filter \"bogus\" \u2014 ignored."));
    when(resultsService.search(any())).thenReturn(expected);

    jakarta.ws.rs.core.Response resp = underTest.getResults("q", "APPLICATION", 1, 25, null, null, null);

    String header = resp.getHeaderString("X-Search-Warnings");
    assertThat(header).as("header must be present when body carries warnings").isNotNull();
    // ASCII-encoded: em-dash (U+2014) must be \u2014 in the header.
    assertThat(header).contains("\\u2014");
    assertThat(header).contains("Unknown filter");
    for (int i = 0; i < header.length(); i++) {
      assertThat((int) header.charAt(i)).isLessThan(0x80);
    }
  }

  @Test
  public void results_noWarnings_omitsXSearchWarningsHeader() {
    SystemConfigurationPropertyFeature.GLOBAL_SEARCH.setEnabled(true);
    stubAuthorizedCaller();
    ResultsResponse expected = new ResultsResponse(Tab.APPLICATION, 1, 25, 0L, List.of(), null);
    when(resultsService.search(any())).thenReturn(expected);

    jakarta.ws.rs.core.Response resp = underTest.getResults("q", "APPLICATION", 1, 25, null, null, null);

    assertThat(resp.getHeaderString("X-Search-Warnings")).isNull();
  }

  @Test
  public void results_flagOn_userWithNoReadGrants_throwsForbiddenAndNeverDelegates() {
    SystemConfigurationPropertyFeature.GLOBAL_SEARCH.setEnabled(true);
    when(searchIndexClient.getCurrentUserContextIdsWithReadPermission()).thenReturn(Set.of());

    assertThatThrownBy(() -> underTest.getResults("q", "APPLICATION", 1, 25, null, null, null))
        .isInstanceOf(ForbiddenException.class);
    verify(resultsService, never()).search(any());
  }

  @Test
  public void resourcePath_isUnderRest() {
    // /rest/* is the internal UI-backing convention; /api/v2/* is the public surface and is
    // deliberately untouched here so the legacy /api/v2/search/advanced endpoint keeps working.
    assertThat(GlobalSearchResource.RESOURCE_PATH).startsWith("rest/");
  }

  @Test
  public void resultsRequest_ctor_rejectsPageBelow1() {
    assertThatThrownBy(() -> new ResultsRequest("q", Tab.APPLICATION, 0, 25, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("page must be >= 1");
  }

  @Test
  public void resultsRequest_ctor_rejectsPageSizeAtOrBelow0() {
    assertThatThrownBy(() -> new ResultsRequest("q", Tab.APPLICATION, 1, 0, null, null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new ResultsRequest("q", Tab.APPLICATION, 1, -1, null, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void resultsRequest_ctor_rejectsPageSizeAboveMax() {
    assertThatThrownBy(() -> new ResultsRequest("q", Tab.APPLICATION, 1, ResultsRequest.MAX_PAGE_SIZE + 1,
        null, null))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void resultsRequest_offsetCalculation_isOneIndexed() {
    ResultsRequest p1 = new ResultsRequest("q", Tab.APPLICATION, 1, 25, null, null);
    ResultsRequest p2 = new ResultsRequest("q", Tab.APPLICATION, 2, 25, null, null);
    ResultsRequest p3 = new ResultsRequest("q", Tab.APPLICATION, 3, 10, null, null);

    assertThat(p1.offset()).isEqualTo(0L);
    assertThat(p2.offset()).isEqualTo(25L);
    assertThat(p3.offset()).isEqualTo(20L);
  }

  @Test
  public void resultsRequest_offsetWithLargePage_doesNotOverflow() {
    // (Integer.MAX_VALUE - 1) * 25 overflows int but must be representable as long.
    ResultsRequest huge = new ResultsRequest("q", Tab.APPLICATION, Integer.MAX_VALUE, 25, null, null);
    long expected = (long) (Integer.MAX_VALUE - 1) * 25L;
    assertThat(huge.offset()).isEqualTo(expected);
    assertThat(huge.offset()).isPositive();
  }

  @Test
  public void resultsRequest_usesCursorOnlyWhenSearchAfterPresent() {
    ResultsRequest withoutCursor = new ResultsRequest("q", Tab.APPLICATION, 1, 25, null, null);
    ResultsRequest blankCursor = new ResultsRequest("q", Tab.APPLICATION, 1, 25, null, "  ");
    ResultsRequest withCursor = new ResultsRequest("q", Tab.APPLICATION, 1, 25, null, "abc");

    assertThat(withoutCursor.usesCursor()).isFalse();
    assertThat(blankCursor.usesCursor()).isFalse();
    assertThat(withCursor.usesCursor()).isTrue();
  }
}
