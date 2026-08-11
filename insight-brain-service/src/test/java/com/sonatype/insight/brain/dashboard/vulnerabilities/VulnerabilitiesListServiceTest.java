/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.vulnerabilities;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.RankedGroup;
import com.sonatype.insight.brain.search.index.RankedGroupsResult;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.results.GroupingByDTO;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.utils.CvssV3Severity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VulnerabilitiesListServiceTest
{
  @Mock
  private SearchIndexClient searchIndexClient;

  @Mock
  private VulnerabilitiesListRequestValidator requestValidator;

  @Mock
  private VulnerabilitiesCatalogListService catalogListService;

  @Mock
  private VulnerabilitiesListScopeFacetsBuilder scopeFacetsBuilder;

  @Mock
  private OrganizationDAO organizationDAO;

  @Mock
  private Configuration configuration;

  private VulnerabilitiesListService service() {
    return new VulnerabilitiesListService(
        searchIndexClient,
        new VulnerabilitiesListIndexQueryBuilder(
            new DashboardIndexDimensionQueryBuilder(organizationDAO, configuration)),
        requestValidator,
        catalogListService,
        scopeFacetsBuilder,
        configuration);
  }

  @Test
  public void highestSeverityVulnerabilityBeyondTheOldSampleWindowAppearsOnPageZero() {
    // A CVSS 10.0 vulnerability whose documents sort far past any bounded document walk. Ranking
    // comes from the index primitive, so page zero carries it regardless of document position.
    stubRankedGroups(
        rankedGroup("cve-buried-critical", 10.0f),
        rankedGroup("cve-common-medium", 5.0f));

    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.page = 0;
    request.pageSize = 25;

    VulnerabilitiesListResponseDTO response = service().listVulnerabilities(request);

    assertThat(response.vulnerabilities).extracting(row -> row.vulnerabilityId)
        .containsExactly("cve-buried-critical", "cve-common-medium");
    assertThat(response.total).isEqualTo(2L);
    assertThat(response.totalExact).isTrue();
  }

  @Test
  public void rankDepthIsClampedAndPagingStopsAtTheBound() {
    stubRankedGroups(rankedGroup("cve-a", 9.0f), rankedGroup("cve-b", 8.0f));

    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.page = 500;
    request.pageSize = 100;

    VulnerabilitiesListResponseDTO response = service().listVulnerabilities(request);

    assertThat(response.vulnerabilities).isEmpty();
    assertThat(response.hasNextPage).isFalse();

    ArgumentCaptor<Integer> limit = ArgumentCaptor.forClass(Integer.class);
    verify(searchIndexClient).rankGroupsByMaxMetric(
        anyString(), anyString(), anyString(), limit.capture(), anyBoolean(), anyMap());
    assertThat(limit.getValue()).isEqualTo(VulnerabilitiesListService.MAX_RANK_DEPTH);
  }

  @Test
  public void severityBandsIncludingUnscoredAndOutOfRangeSumToTotal() {
    stubRankedGroups(
        rankedGroup("cve-critical", 9.5f),
        rankedGroup("cve-out-of-range", 11.0f),
        rankedGroup("cve-zero", 0.0f),
        rankedGroup("cve-unscored", null));

    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.includeFacets = true;

    VulnerabilitiesListResponseDTO response = service().listVulnerabilities(request);

    long banded = response.facets.severities.values().stream().mapToLong(Long::longValue).sum();
    assertThat(banded).isEqualTo(response.total);
    assertThat(response.facets.severities).containsEntry("critical", 1L);
    // Zero-scored, unscored, and out-of-range vulnerabilities all resolve to none.
    assertThat(response.facets.severities).containsEntry("none", 3L);
    assertThat(response.facets.severities).doesNotContainKey("unknown");
  }

  @Test
  public void ecosystemFacetCoversTheEstateAndDoesNotChangeWhenPaging() {
    stubRankedGroups(rankedGroup("cve-a", 9.0f), rankedGroup("cve-b", 8.0f));

    VulnerabilitiesListRequestDTO firstPage = new VulnerabilitiesListRequestDTO();
    firstPage.page = 0;
    firstPage.includeFacets = true;
    VulnerabilitiesListRequestDTO secondPage = new VulnerabilitiesListRequestDTO();
    secondPage.page = 1;
    secondPage.includeFacets = true;

    VulnerabilitiesListService service = service();
    VulnerabilitiesListResponseDTO first = service.listVulnerabilities(firstPage);
    VulnerabilitiesListResponseDTO second = service.listVulnerabilities(secondPage);

    // Counts come from an estate-wide aggregation, so they are exact on an exact backend and do not
    // shrink to whatever the current page happened to hydrate.
    assertThat(first.facets.ecosystemsExact).isTrue();
    assertThat(first.facets.ecosystems).containsEntry("maven", 2L);
    assertThat(second.vulnerabilities).isEmpty();
    assertThat(second.facets.ecosystems).isEqualTo(first.facets.ecosystems);
  }

  @Test
  public void severityFacetKeepsUnselectedBandsWhenASeverityIsFiltered() {
    // The rail renders every band, so counting them against a query that already carries the
    // severity selection would report the unselected bands as zero and strand the user on their
    // current selection. They come from a pass with that dimension dropped instead.
    Map<String, Long> narrowedBands = zeroedBands();
    narrowedBands.put("critical", 1L);
    Map<String, Long> estateBands = zeroedBands();
    estateBands.put("critical", 1L);
    estateBands.put("high", 7L);
    RankedGroupsResult narrowed = new RankedGroupsResult(
        List.of(rankedGroup("cve-critical", 9.5f)), 1L, true, narrowedBands, 0L);
    RankedGroupsResult estate = new RankedGroupsResult(List.of(), 8L, true, estateBands, 0L);
    when(searchIndexClient.rankGroupsByMaxMetric(
        anyString(), anyString(), anyString(), anyInt(), anyBoolean(), anyMap()))
            .thenAnswer(invocation -> carriesSeverityClause(invocation.getArgument(0))
                ? narrowed
                : estate);
    when(searchIndexClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenReturn(resultOf(vulnerabilityItem("cve-critical")));

    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.severities = Set.of("critical");
    request.includeFacets = true;

    VulnerabilitiesListResponseDTO response = service().listVulnerabilities(request);

    assertThat(response.facets.severities).containsEntry("high", 7L);
    assertThat(response.facets.severities).containsEntry("critical", 1L);
    // The rows and the total still reflect the narrowed selection.
    assertThat(response.vulnerabilities).extracting(row -> row.vulnerabilityId)
        .containsExactly("cve-critical");
    assertThat(response.total).isEqualTo(1L);
  }

  @Test
  public void ecosystemFacetKeepsUnselectedFormatsWhenAnEcosystemIsFiltered() {
    stubRankedGroups(rankedGroup("cve-a", 9.0f));
    stubEcosystemCounts(Map.of("maven", 3L, "npm", 5L));

    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.ecosystems = Set.of("maven");
    request.includeFacets = true;

    VulnerabilitiesListResponseDTO response = service().listVulnerabilities(request);

    assertThat(response.facets.ecosystems).containsEntry("npm", 5L);

    // The aggregation must run without the ecosystem selection, or npm could never appear in it.
    ArgumentCaptor<String> facetQuery = ArgumentCaptor.forClass(String.class);
    verify(searchIndexClient).countDistinctGroupedBy(
        facetQuery.capture(),
        eq(FieldIdentifier.COMPONENT_FORMAT.label),
        anyString(),
        anyCollection());
    assertThat(facetQuery.getValue()).doesNotContain(FieldIdentifier.COMPONENT_FORMAT.label);
  }

  /** True when the query narrows the severity field, which is what the band pass must drop. */
  private static boolean carriesSeverityClause(final String query) {
    return query.contains(FieldIdentifier.VULNERABILITY_SEVERITY.label);
  }

  @Test
  public void hasNextPageFollowsTheRankedSetRatherThanAnEstimatedTotal() {
    // OpenSearch estimates the distinct total, and an overshoot would advertise a page that comes
    // back empty. Ranking one group past the page answers it exactly instead.
    List<RankedGroup> groups = new ArrayList<>();
    for (int i = 0; i < 26; i++) {
      groups.add(rankedGroup("cve-" + i, 9.0f));
    }
    when(searchIndexClient.rankGroupsByMaxMetric(
        anyString(), anyString(), anyString(), anyInt(), anyBoolean(), anyMap()))
            .thenReturn(new RankedGroupsResult(groups, 4_200L, false, zeroedBands(), 26L));

    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.page = 0;
    request.pageSize = 25;
    request.includeFacets = false;

    assertThat(service().listVulnerabilities(request).hasNextPage).isTrue();

    when(searchIndexClient.rankGroupsByMaxMetric(
        anyString(), anyString(), anyString(), anyInt(), anyBoolean(), anyMap()))
            .thenReturn(new RankedGroupsResult(
                groups.subList(0, 25), 4_200L, false, zeroedBands(), 25L));

    assertThat(service().listVulnerabilities(request).hasNextPage).isFalse();
  }

  @Test
  public void rowsCarryHydratedDetailKeyedByTheLowerCasedRankedId() {
    // The ranking primitive lower-cases group values while documents keep their display casing, so
    // hydrated detail is only found if the lookup bridges the two. The row must then report the
    // document's casing, not the lower-cased key it was matched on.
    Map<String, Long> bands = zeroedBands();
    bands.put("critical", 1L);
    when(searchIndexClient.rankGroupsByMaxMetric(
        anyString(), anyString(), anyString(), anyInt(), anyBoolean(), anyMap()))
            .thenReturn(new RankedGroupsResult(
                List.of(rankedGroup("cve-2021-44228", 10.0f)), 1L, true, bands, 0L));
    when(searchIndexClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenReturn(resultOf(vulnerabilityItem("CVE-2021-44228")));
    stubApplicationCounts(Map.of("cve-2021-44228", 1L));

    VulnerabilitiesListResponseDTO response = service().listVulnerabilities(new VulnerabilitiesListRequestDTO());

    assertThat(response.vulnerabilities).hasSize(1);
    VulnerabilityRowDTO row = response.vulnerabilities.get(0);
    assertThat(row.vulnerabilityId).isEqualTo("CVE-2021-44228");
    assertThat(row.title).isEqualTo("Title for CVE-2021-44228");
    assertThat(row.ecosystem).isEqualTo("maven");
    assertThat(row.cvssScore).isEqualTo(10.0f);
    assertThat(row.severity).isEqualTo("critical");
    assertThat(row.applicationCount).isEqualTo(1);
    assertThat(row.applicationCountExact).isTrue();
  }

  @Test
  public void totalIsReportedInexactWhenTheBackendEstimatesTheDistinctCount() {
    when(searchIndexClient.rankGroupsByMaxMetric(
        anyString(), anyString(), anyString(), anyInt(), anyBoolean(), anyMap()))
            .thenReturn(new RankedGroupsResult(List.of(), 4_200L, false, zeroedBands(), 0L));

    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.includeFacets = false;

    VulnerabilitiesListResponseDTO response = service().listVulnerabilities(request);

    assertThat(response.total).isEqualTo(4_200L);
    assertThat(response.totalExact).isFalse();
    assertThat(response.vulnerabilities).isEmpty();
  }

  @Test
  public void applicationCountCoversTheWholeEstateRatherThanTheHydratedDocuments() {
    // The hydration walk reads one representative document per id, so a vulnerability in thousands of
    // applications would read as 1 if the count came from that walk. It comes from an aggregation over
    // every matching document instead, and inherits that aggregation's exactness.
    Map<String, Long> bands = zeroedBands();
    bands.put("critical", 1L);
    when(searchIndexClient.rankGroupsByMaxMetric(
        anyString(), anyString(), anyString(), anyInt(), anyBoolean(), anyMap()))
            .thenReturn(new RankedGroupsResult(
                List.of(rankedGroup("cve-2021-44228", 10.0f)), 1L, false, bands, 0L));
    when(searchIndexClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenReturn(resultOf(vulnerabilityItem("cve-2021-44228")));
    when(searchIndexClient.countDistinctGroupedBy(anyString(), anyString(), anyString(), anyCollection()))
        .thenReturn(Map.of("cve-2021-44228", 4_812L));

    VulnerabilitiesListResponseDTO response = service().listVulnerabilities(new VulnerabilitiesListRequestDTO());

    VulnerabilityRowDTO row = response.vulnerabilities.get(0);
    assertThat(row.applicationCount).isEqualTo(4_812);
    assertThat(row.applicationCountExact).isFalse();
  }

  @Test
  public void hydrationSplitsIdsToFitTheConfiguredClauseBudget() {
    // Every hydrated id becomes a boolean clause, and Lucene enforces maxAdvancedSearchClauseCount
    // against the whole query. That property is administrator-set and tenant-set on MTIQ, so a fixed
    // batch would put every page load over the ceiling for anyone who lowered it.
    int budget = 20;
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(budget);
    List<RankedGroup> groups = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      groups.add(rankedGroup(String.format("cve-2026-%05d", i), 9.0f));
    }
    Map<String, Long> bands = zeroedBands();
    bands.put("critical", (long) groups.size());
    when(searchIndexClient.rankGroupsByMaxMetric(
        anyString(), anyString(), anyString(), anyInt(), anyBoolean(), anyMap()))
            .thenReturn(new RankedGroupsResult(groups, groups.size(), true, bands, 0L));
    // A short page ends each batch's walk after one read, so the captured queries are exactly the
    // batches rather than the batches times their paging.
    when(searchIndexClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenReturn(resultOf(vulnerabilityItem("cve-2026-00000")));

    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.pageSize = 100;
    request.includeFacets = false;

    service().listVulnerabilities(request);

    ArgumentCaptor<String> queries = ArgumentCaptor.forClass(String.class);
    verify(searchIndexClient, atLeastOnce()).searchIndex(
        queries.capture(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList());

    int total = 0;
    for (String query : queries.getAllValues()) {
      int ids = countIdTerms(query);
      assertThat(ids).isLessThanOrEqualTo(budget);
      total += ids;
    }
    // Every page id is still hydrated, just spread across reads — this also proves the count above
    // is really seeing ids rather than passing on an empty match.
    assertThat(queries.getAllValues()).hasSizeGreaterThan(1);
    assertThat(total).isEqualTo(groups.size());
  }

  @Test
  public void applicationCountsAreReadAgainstTheRestrictedQueryNotTheWholeEstate() {
    // The Lucene collector behind this count loads a document's stored fields before it can see
    // whether the group was asked for, so the query decides the cost: handed the base query it pays
    // that for every document in the estate on every page load. It has to carry the same id
    // restriction the hydration walk uses, and be split the same way to stay under the clause budget.
    int budget = 20;
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(budget);
    List<RankedGroup> groups = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      groups.add(rankedGroup(String.format("cve-2026-%05d", i), 9.0f));
    }
    Map<String, Long> bands = zeroedBands();
    bands.put("critical", (long) groups.size());
    when(searchIndexClient.rankGroupsByMaxMetric(
        anyString(), anyString(), anyString(), anyInt(), anyBoolean(), anyMap()))
            .thenReturn(new RankedGroupsResult(groups, groups.size(), true, bands, 0L));
    when(searchIndexClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenReturn(resultOf(vulnerabilityItem("cve-2026-00000")));

    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.pageSize = 100;
    request.includeFacets = false;

    service().listVulnerabilities(request);

    ArgumentCaptor<String> countQueries = ArgumentCaptor.forClass(String.class);
    verify(searchIndexClient, atLeastOnce()).countDistinctGroupedBy(
        countQueries.capture(),
        eq(FieldIdentifier.VULNERABILITY_ID.label),
        eq(FieldIdentifier.APPLICATION_PUBLIC_ID.label),
        anyCollection());

    int total = 0;
    for (String query : countQueries.getAllValues()) {
      int ids = countIdTerms(query);
      assertThat(ids).isPositive().isLessThanOrEqualTo(budget);
      total += ids;
    }
    // Every page id is still counted, just spread across reads.
    assertThat(total).isEqualTo(groups.size());
  }

  /**
   * Ids in the hydration clause, which the builder renders as {@code vulnerabilityId:(id1 id2 …)}.
   * Counts the {@code cve} prefix rather than the whole id so the tally cannot be thrown off by the
   * escaping the builder applies to each term.
   */
  private static int countIdTerms(final String query) {
    int count = 0;
    Matcher matcher = Pattern.compile("cve").matcher(query);
    while (matcher.find()) {
      count++;
    }
    return count;
  }

  @Test
  public void severityBandsCarryTheExactnessOfTheReadThatProducedThem() {
    // The bands are distinct counts from the same read as the total, so on a backend that counts by
    // estimation they are estimates too. Without a flag of their own the rail renders them as exact.
    Map<String, Long> bands = zeroedBands();
    bands.put("critical", 1L);
    when(searchIndexClient.rankGroupsByMaxMetric(
        anyString(), anyString(), anyString(), anyInt(), anyBoolean(), anyMap()))
            .thenReturn(new RankedGroupsResult(
                List.of(rankedGroup("cve-2021-44228", 10.0f)), 1L, false, bands, 0L));
    when(searchIndexClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenReturn(resultOf(vulnerabilityItem("cve-2021-44228")));
    stubEcosystemCounts(Map.of("maven", 1L));

    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.includeFacets = true;

    VulnerabilitiesListResponseDTO response = service().listVulnerabilities(request);

    assertThat(response.facets.severitiesExact).isFalse();
  }

  @Test
  public void anUnhydratedRowStillRendersItsIdInDisplayCasing() {
    // The ranked id arrives lower-cased and a hydrated document normally supplies the real casing. A
    // page whose ids exhaust the hydration budget has no such document, and the id reaches the user
    // exactly as rendered here.
    stubRankedGroups(rankedGroup("cve-2021-44228", 10.0f));
    when(searchIndexClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenReturn(resultOf());

    VulnerabilitiesListResponseDTO response = service().listVulnerabilities(new VulnerabilitiesListRequestDTO());

    VulnerabilityRowDTO row = response.vulnerabilities.get(0);
    assertThat(row.title).isNull();
    assertThat(row.vulnerabilityId).isEqualTo("CVE-2021-44228");
  }

  private void stubRankedGroups(final RankedGroup... groups) {
    List<RankedGroup> ranked = List.of(groups);
    Map<String, Long> bandCounts = zeroedBands();
    long unbanded = 0;
    for (RankedGroup group : ranked) {
      String band = bandOf(group.metricValue());
      if (band == null) {
        unbanded++;
      }
      else {
        bandCounts.merge(band, 1L, Long::sum);
      }
    }
    when(searchIndexClient.rankGroupsByMaxMetric(
        anyString(), anyString(), anyString(), anyInt(), anyBoolean(), anyMap()))
            .thenReturn(new RankedGroupsResult(ranked, ranked.size(), true, bandCounts, unbanded));

    List<SearchResultItemDTO> items = new ArrayList<>(ranked.size());
    for (RankedGroup group : ranked) {
      items.add(vulnerabilityItem(group.groupValue()));
    }
    when(searchIndexClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenReturn(resultOf(items.toArray(new SearchResultItemDTO[0])));

    Map<String, Long> applicationCounts = new LinkedHashMap<>();
    ranked.forEach(group -> applicationCounts.put(group.groupValue(), 1L));
    stubApplicationCounts(applicationCounts);
    stubEcosystemCounts(Map.of("maven", (long) ranked.size()));
  }

  private void stubApplicationCounts(final Map<String, Long> countsByVulnerabilityId) {
    when(searchIndexClient.countDistinctGroupedBy(
        anyString(), eq(FieldIdentifier.VULNERABILITY_ID.label), anyString(), anyCollection()))
            .thenReturn(countsByVulnerabilityId);
  }

  private void stubEcosystemCounts(final Map<String, Long> countsByFormat) {
    when(searchIndexClient.countDistinctGroupedBy(
        anyString(), eq(FieldIdentifier.COMPONENT_FORMAT.label), anyString(), anyCollection()))
            .thenReturn(countsByFormat);
  }

  private static RankedGroup rankedGroup(final String vulnerabilityId, final Float cvssScore) {
    return new RankedGroup(vulnerabilityId, cvssScore);
  }

  private static Map<String, Long> zeroedBands() {
    Map<String, Long> bands = new LinkedHashMap<>();
    CvssV3Severity.halfOpenScoreBands().keySet().forEach(band -> bands.put(band, 0L));
    return bands;
  }

  private static String bandOf(final Float cvssScore) {
    if (cvssScore == null) {
      return null;
    }
    for (Map.Entry<String, float[]> band : CvssV3Severity.halfOpenScoreBands().entrySet()) {
      if (cvssScore >= band.getValue()[0] && cvssScore < band.getValue()[1]) {
        return band.getKey();
      }
    }
    return null;
  }

  private static SearchResultDTO resultOf(final SearchResultItemDTO... items) {
    GroupingByDTO group = new GroupingByDTO();
    group.searchResultItemDTOS = List.of(items);
    SearchResultDTO result = new SearchResultDTO();
    result.groupingByDTOS = List.of(group);
    return result;
  }

  private static SearchResultItemDTO vulnerabilityItem(final String vulnerabilityId) {
    SearchResultItemDTO item = new SearchResultItemDTO();
    item.itemType = ItemType.SECURITY_VULNERABILITY.name();
    item.vulnerabilityId = vulnerabilityId;
    item.vulnerabilityDescription = "Title for " + vulnerabilityId;
    item.applicationPublicId = "app-" + vulnerabilityId;
    ApiComponentIdentifierDTOV2 identifier = new ApiComponentIdentifierDTOV2();
    identifier.setFormat("maven");
    item.componentIdentifier = identifier;
    return item;
  }
}
