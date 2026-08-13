/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.search.global.catalog.CatalogSuggestRequest;
import com.sonatype.insight.brain.search.global.catalog.CatalogSuggestResult;
import com.sonatype.insight.brain.search.global.catalog.GlobalSearchSuggestCatalogClient;
import com.sonatype.insight.brain.security.CurrentUser;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SuggestServiceImpl}: the static total-cap helper, the per-type fetch-limit
 * contract, and single-source dispatch. End-to-end orchestration is covered by
 * {@code SuggestEndpointTest}.
 */
public class SuggestServiceImplTest
{
  @Test
  public void bestMatchPlusPerSectionRows_sumIsCappedAt10() {
    List<SuggestGroup> raw = List.of(
        group(SuggestItemType.VULNERABILITY, 3),
        group(SuggestItemType.COMPONENT, 3),
        group(SuggestItemType.APPLICATION, 3),
        group(SuggestItemType.VIOLATION, 3),
        group(SuggestItemType.WAIVER, 3));
    SuggestRow bestMatch = row(SuggestItemType.APPLICATION, "best-app", "Best App");

    List<SuggestGroup> capped = SuggestServiceImpl.applyTotalCap(raw, bestMatch);

    int totalRows = capped.stream().mapToInt(g -> g.results().size()).sum();
    assertThat(totalRows + 1 /* BEST MATCH */).isEqualTo(10);
  }

  @Test
  public void noBestMatch_capAllows10GroupRows() {
    List<SuggestGroup> raw = List.of(
        group(SuggestItemType.VULNERABILITY, 3),
        group(SuggestItemType.COMPONENT, 3),
        group(SuggestItemType.APPLICATION, 3),
        group(SuggestItemType.VIOLATION, 3),
        group(SuggestItemType.WAIVER, 3));

    List<SuggestGroup> capped = SuggestServiceImpl.applyTotalCap(raw, null);

    int totalRows = capped.stream().mapToInt(g -> g.results().size()).sum();
    assertThat(totalRows).isEqualTo(10);
  }

  @Test
  public void underCap_groupsPassThroughUnchanged() {
    List<SuggestGroup> raw = List.of(
        group(SuggestItemType.APPLICATION, 1),
        group(SuggestItemType.COMPONENT, 1));

    List<SuggestGroup> capped = SuggestServiceImpl.applyTotalCap(raw, null);

    // Content equality (not reference identity): capped() copies on the under-limit pass-through path.
    assertThat(capped).usingRecursiveComparison().isEqualTo(raw);
  }

  @Test
  public void iqLocalLegIsAskedForPerTypeLimitPlusLookahead() {
    // The service must ask IQ-local for the expanded window so BEST MATCH resolution can see rows
    // sitting beyond the visible perTypeLimit slice.
    RecordingIqLocal iq = new RecordingIqLocal();
    GlobalSearchSuggestCatalogClient catalog = mock(GlobalSearchSuggestCatalogClient.class);

    SuggestServiceImpl service = new SuggestServiceImpl(iq, catalog, new BestMatchResolver(), stubCurrentUser(),
        new PerUserRateLimiter(Integer.MAX_VALUE));
    service.suggest("alpha", SearchSource.LOCAL);

    assertThat(iq.lastPerTypeLimit)
        .isEqualTo(SuggestServiceImpl.DEFAULT_PER_TYPE_LIMIT + SuggestServiceImpl.BEST_MATCH_LOOKAHEAD);
  }

  @Test
  public void localSource_neverConsultsCatalog() {
    RecordingIqLocal iq = new RecordingIqLocal();
    GlobalSearchSuggestCatalogClient catalog = mock(GlobalSearchSuggestCatalogClient.class);

    SuggestServiceImpl service = new SuggestServiceImpl(iq, catalog, new BestMatchResolver(), stubCurrentUser(),
        new PerUserRateLimiter(Integer.MAX_VALUE));
    SuggestResponse response = service.suggest("alpha", SearchSource.LOCAL);

    verifyNoInteractions(catalog);
    // Catalog not consulted on the local path: catalogAvailable is null (omitted from JSON), not false.
    assertThat(response.catalogAvailable()).isNull();
  }

  @Test
  public void catalogSource_unentitled_returnsEmpty_catalogAvailableFalse_noHdsCall() {
    RecordingIqLocal iq = new RecordingIqLocal();
    GlobalSearchSuggestCatalogClient catalog = mock(GlobalSearchSuggestCatalogClient.class);
    when(catalog.isEnabled()).thenReturn(false);

    SuggestServiceImpl service = new SuggestServiceImpl(iq, catalog, new BestMatchResolver(), stubCurrentUser(),
        new PerUserRateLimiter(Integer.MAX_VALUE));
    SuggestResponse response = service.suggest("alpha", SearchSource.CATALOG);

    assertThat(response.catalogAvailable()).isFalse();
    // Only COMPONENT + VULNERABILITY groups under catalog source.
    assertThat(response.groups()).extracting(SuggestGroup::type)
        .containsExactly(SuggestItemType.VULNERABILITY, SuggestItemType.COMPONENT);
    assertThat(response.groups()).allMatch(g -> g.results().isEmpty());
    verify(catalog, never()).suggest(any(CatalogSuggestRequest.class));
  }

  @Test
  public void catalogSource_catalogUnavailable_groupsTaggedCatalog_catalogAvailableFalse() {
    RecordingIqLocal iq = new RecordingIqLocal();
    GlobalSearchSuggestCatalogClient catalog = mock(GlobalSearchSuggestCatalogClient.class);
    when(catalog.isEnabled()).thenReturn(true);
    when(catalog.suggest(any(CatalogSuggestRequest.class))).thenReturn(CatalogSuggestResult.unavailable());

    SuggestServiceImpl service = new SuggestServiceImpl(iq, catalog, new BestMatchResolver(), stubCurrentUser(),
        new PerUserRateLimiter(Integer.MAX_VALUE));
    SuggestResponse response = service.suggest("alpha", SearchSource.CATALOG);

    assertThat(response.catalogAvailable()).isFalse();
    assertThat(response.groups()).allMatch(g -> g.results().isEmpty());
    assertThat(response.groups()).extracting(SuggestGroup::source)
        .containsOnly(SearchSource.CATALOG);
  }

  @Test
  public void catalogLeg_isAskedForLookaheadWindowAcrossBothTypes() {
    // I5/M1: the single mixed HDS limit must cover the per-type cap PLUS the BEST MATCH look-ahead for
    // EACH catalog type, so an exact match past the visible slice can still promote and a lopsided
    // component/vuln mix still leaves enough of each type to fill both caps.
    RecordingIqLocal iq = new RecordingIqLocal();
    GlobalSearchSuggestCatalogClient catalog = mock(GlobalSearchSuggestCatalogClient.class);
    when(catalog.isEnabled()).thenReturn(true);
    when(catalog.suggest(any(CatalogSuggestRequest.class))).thenReturn(CatalogSuggestResult.available(List.of()));

    SuggestServiceImpl service = new SuggestServiceImpl(iq, catalog, new BestMatchResolver(), stubCurrentUser(),
        new PerUserRateLimiter(Integer.MAX_VALUE));
    service.suggest("alpha", SearchSource.CATALOG);

    ArgumentCaptor<CatalogSuggestRequest> captor = ArgumentCaptor.forClass(CatalogSuggestRequest.class);
    verify(catalog).suggest(captor.capture());
    int expected = (SuggestServiceImpl.DEFAULT_PER_TYPE_LIMIT + SuggestServiceImpl.BEST_MATCH_LOOKAHEAD)
        * SuggestServiceImpl.CATALOG_TYPES.size();
    assertThat(captor.getValue().limit()).isEqualTo(expected);
  }

  @Test
  public void exhaustedRateLimiter_propagates429() {
    // M9: hold the single permit for the same user so the next acquire throws RateLimitedException
    // (HTTP 429), independent of the real per-endpoint suggest cap.
    RecordingIqLocal iq = new RecordingIqLocal();
    GlobalSearchSuggestCatalogClient catalog = mock(GlobalSearchSuggestCatalogClient.class);
    PerUserRateLimiter limiter = new PerUserRateLimiter(1);

    SuggestServiceImpl service =
        new SuggestServiceImpl(iq, catalog, new BestMatchResolver(), stubCurrentUser(), limiter);
    try (PerUserRateLimiter.Permit held = limiter.acquire("tester")) {
      assertThat(held).isNotNull();
      assertThatThrownBy(() -> service.suggest("alpha", SearchSource.LOCAL))
          .isInstanceOf(RateLimitedException.class);
    }
  }

  private static SuggestGroup group(final SuggestItemType type, final int count) {
    List<SuggestRow> rows = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      rows.add(row(type, type.name() + "-" + i, "Title " + i));
    }
    return new SuggestGroup(type, SearchSource.LOCAL, rows);
  }

  private static SuggestRow row(final SuggestItemType type, final String id, final String title) {
    return new SuggestRow(id, type, SearchSource.LOCAL, title, "", null);
  }

  private static CurrentUser stubCurrentUser() {
    CurrentUser cu = mock(CurrentUser.class);
    when(cu.getUsernameOrSystem()).thenReturn("tester");
    UserPrincipal principal = mock(UserPrincipal.class);
    when(cu.getUserPrincipal()).thenReturn(principal);
    return cu;
  }

  /** Records the per-type limit the service asked for and returns no rows. */
  private static final class RecordingIqLocal
      implements GlobalSearchSuggestIqLocalClient
  {
    volatile int lastPerTypeLimit = -1;

    @Override
    public List<SuggestRow> suggest(
        final String query,
        final List<SuggestItemType> types,
        final int perTypeLimit,
        final UserPrincipal principal)
    {
      this.lastPerTypeLimit = perTypeLimit;
      return List.of();
    }
  }
}
