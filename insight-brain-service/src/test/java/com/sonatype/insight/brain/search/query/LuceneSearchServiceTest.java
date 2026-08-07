/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.query;

import java.io.IOException;
import java.nio.file.Files;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.lucene.LuceneIndexWriterOwner;
import com.sonatype.insight.brain.search.lucene.LuceneSearchIndexClient;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryCollector;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;
import jakarta.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;

import static org.apache.commons.io.FileUtils.deleteDirectory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
@ContextConfiguration(classes = LuceneSearchServiceTest.LuceneSearchServiceTestConfiguration.class)
public class LuceneSearchServiceTest
    extends AbstractSearchServiceTest
{
  @Inject
  private AdvancedSearchTelemetryCollector advancedSearchTelemetryCollector;

  @Inject
  private LuceneIndexWriterOwner luceneIndexWriterOwner;

  @TestConfiguration
  static class LuceneSearchServiceTestConfiguration
  {
    @Bean
    @Primary
    SearchIndexClient searchIndexClient(final LuceneSearchIndexClient luceneSearchIndexClient) {
      return luceneSearchIndexClient;
    }
  }

  @Override
  protected void grantDefaultTestUserAllPermissions() {
    tempEntity.newUser(USERNAME);
  }

  @Before
  public void resetLuceneSearchFixture() throws IOException {
    advancedSearchTelemetryCollector.collectAllData();
    luceneIndexWriterOwner.deregister();
    readableContextAuthzCache.bumpEpoch();

    if (insightWork.getSearchIndexDir().exists()) {
      deleteDirectory(insightWork.getSearchIndexDir());
    }
  }

  @Test
  public void testSearchIndex_MaxAdvancedSearchClauseCountLimitExceeded() {
    try {
      Role nonGlobalReadRole = tempEntity.newRole(false, Permission.READ);

      Application application = tempEntity.newApplicationWithParent();
      Application anotherApplication = tempEntity.newApplicationWithParent();
      Application oneMoreApplication = tempEntity.newApplicationWithParent();

      UserPrincipal userPrincipal = (UserPrincipal) subject.getPrincipal();
      tempEntity.newMembershipMapping(application.getId(), nonGlobalReadRole.getId(), userPrincipal.getUsername());
      tempEntity.newMembershipMapping(anotherApplication.getId(), nonGlobalReadRole.getId(),
          userPrincipal.getUsername());
      tempEntity.newMembershipMapping(oneMoreApplication.getId(), nonGlobalReadRole.getId(),
          userPrincipal.getUsername());

      configurationService.setConfigurationInDatabaseNoAuthz(
          SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT, 2);
      configurationService.applyConfigurationToClients(SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT);
      indexService.createSearchIndex();

      assertThatExceptionOfType(BadRequestException.class)
          .isThrownBy(() -> searchService.searchIndex("itemType:APPLICATION", 1, 0, false, null, null))
          .withMessage("Error performing search due to too many clauses. " +
              "Please try narrowing down the query as much as possible " +
              "and consider updating Advanced Search configuration to support larger queries.");
    }
    finally {
      configurationService.deleteConfigurationInDatabaseNoAuthz(
          SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT);
      configurationService.applyConfigurationToClients(SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT);
    }
  }

  @Test
  public void testSearchIndex_NoSearchIndexDirectory() {
    assertThatExceptionOfType(ConflictException.class).isThrownBy(
        () -> searchService.searchIndex("query", 1, 1, false, null, null))
        .withMessageContaining("Search index not found. The Advanced Search index is unavailable or has not " +
            "been created yet. Re-indexing is required before results can be returned.");
  }

  @Test
  public void testSearchIndex_EmptySearchIndexDirectory() throws Exception {
    Files.createDirectories(insightWork.getSearchIndexDir().toPath());
    assertThatExceptionOfType(ConflictException.class).isThrownBy(
        () -> searchService.searchIndex("query", 1, 1, false, null, null))
        .withMessageContaining("Search index not found. The Advanced Search index is unavailable or has not " +
            "been created yet. Re-indexing is required before results can be returned.");
  }

  @Test
  public void testSearchIndex_GroupsSequentialResultsIfPossible() throws Exception {
    Role role = tempEntity.newRole(false, Permission.READ);

    Organization org1 = tempEntity.newOrganization("org-01");
    Application app1 = tempEntity.newApplication("app-01", org1.getId());
    // there are 3 vulnerabilities for "CVE-2022-25857" in the report below
    newAppReport(app1.getId(), Stage.ID_RELEASE, "report-1", "/SearchServiceTest/report-1");

    Application app2 = tempEntity.newApplication("app-02", org1.getId());
    // there is 1 vulnerability for "CVE-2022-25857" in the report below
    newAppReport(app2.getId(), Stage.ID_RELEASE, "report-2", "/SearchServiceTest/report-2");

    UserPrincipal userPrincipal = (UserPrincipal) subject.getPrincipal();
    tempEntity.newMembershipMapping(org1.getId(), role.getId(), userPrincipal.getUsername());

    indexService.createSearchIndex();

    // Range query invariants (order-agnostic): 4 of the 39 hits are CVE-2022-25857. Their positions
    // in the result set depend on Lucene scoring and are not asserted. Any grouping with more than
    // one item must be a sequential-index run of same-groupBy items — that is the sequential-grouping
    // contract the algorithm is expected to honour.
    SearchResultDTO searchResultDTO =
        searchService.searchIndex("vulnerabilitySeverity:[7 TO 8]", Integer.MAX_VALUE, 0, false, null, null);
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(39);
    assertGroupingInvariants(searchResultDTO, 39);
    assertThat(countGroupedItemsByGroupBy(searchResultDTO, "CVE-2022-25857")).isEqualTo(4);

    // If we search specifically for CVE-2022-25857, then all results should be grouped (unless split across pages)
    // since no matter the order they will have the same groupBy key
    searchResultDTO = searchService.searchIndex("CVE-2022-25857", Integer.MAX_VALUE, 0, false, null, null);
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(4);
    assertThat(searchResultDTO.groupingByDTOS).hasSize(1);

    // First, second, third, and fourth results (page 1)
    assertThat(searchResultDTO.groupingByDTOS.get(0).groupBy).isEqualTo("CVE-2022-25857");
    assertThat(searchResultDTO.groupingByDTOS.get(0).searchResultItemDTOS).hasSize(4);
    assertThat(searchResultDTO.groupingByDTOS.get(0).searchResultItemDTOS.get(0).resultIndex).isEqualTo(1);
    assertThat(searchResultDTO.groupingByDTOS.get(0).searchResultItemDTOS.get(1).resultIndex).isEqualTo(2);
    assertThat(searchResultDTO.groupingByDTOS.get(0).searchResultItemDTOS.get(2).resultIndex).isEqualTo(3);
    assertThat(searchResultDTO.groupingByDTOS.get(0).searchResultItemDTOS.get(3).resultIndex).isEqualTo(4);

    // Try splitting across pages
    searchResultDTO = searchService.searchIndex("CVE-2022-25857", 2, 0, false, null, null);
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(4);
    assertThat(searchResultDTO.groupingByDTOS).hasSize(1);

    // First and second results (page 1)
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(4);
    assertThat(searchResultDTO.groupingByDTOS).hasSize(1);
    assertThat(searchResultDTO.groupingByDTOS.get(0).groupBy).isEqualTo("CVE-2022-25857");
    assertThat(searchResultDTO.groupingByDTOS.get(0).searchResultItemDTOS).hasSize(2);
    assertThat(searchResultDTO.groupingByDTOS.get(0).searchResultItemDTOS.get(0).resultIndex).isEqualTo(1);
    assertThat(searchResultDTO.groupingByDTOS.get(0).searchResultItemDTOS.get(1).resultIndex).isEqualTo(2);

    searchResultDTO = searchService.searchIndex("CVE-2022-25857", 2, 2, false, null, null);
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(4);
    assertThat(searchResultDTO.groupingByDTOS).hasSize(1);

    // Third and fourth results (page 2)
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(4);
    assertThat(searchResultDTO.groupingByDTOS).hasSize(1);
    assertThat(searchResultDTO.groupingByDTOS.get(0).groupBy).isEqualTo("CVE-2022-25857");
    assertThat(searchResultDTO.groupingByDTOS.get(0).searchResultItemDTOS).hasSize(2);
    assertThat(searchResultDTO.groupingByDTOS.get(0).searchResultItemDTOS.get(0).resultIndex).isEqualTo(3);
    assertThat(searchResultDTO.groupingByDTOS.get(0).searchResultItemDTOS.get(1).resultIndex).isEqualTo(4);
  }

  /**
   * Verifies the sequential-grouping algorithm's contract in a way that does not depend on the
   * underlying Lucene result ordering:
   * <ul>
   * <li>items across all groupings sum to {@code expectedTotalItems};</li>
   * <li>each grouping's items form a contiguous run of {@code resultIndex} values;</li>
   * <li>consecutive groupings tile the result set — the last {@code resultIndex} of one grouping is
   * exactly one less than the first of the next;</li>
   * <li>consecutive groupings have <em>different</em> {@code groupBy} values, i.e. a new grouping is
   * started if and only if the previous item's {@code groupBy} differs from the current item's.
   * This is the "collapse consecutive duplicates" contract in
   * {@link com.sonatype.insight.brain.search.index.AbstractSearchIndexClient#groupDocuments}: it
   * would fail both if two adjacent same-{@code groupBy} items were placed in separate groupings
   * (under-merge) and if two adjacent different-{@code groupBy} items were placed in the same
   * grouping (over-merge).</li>
   * </ul>
   */
  private static void assertGroupingInvariants(final SearchResultDTO result, final int expectedTotalItems) {
    int totalItems = result.groupingByDTOS.stream()
        .mapToInt(g -> g.searchResultItemDTOS.size())
        .sum();
    assertThat(totalItems).isEqualTo(expectedTotalItems);

    Integer expectedNextResultIndex = null;
    String previousGroupBy = null;
    for (var grouping : result.groupingByDTOS) {
      var items = grouping.searchResultItemDTOS;
      assertThat(items).isNotEmpty();

      for (int i = 1; i < items.size(); i++) {
        assertThat(items.get(i).resultIndex)
            .as("items within a grouping must have contiguous resultIndex values")
            .isEqualTo(items.get(i - 1).resultIndex + 1);
      }

      if (expectedNextResultIndex != null) {
        assertThat(items.get(0).resultIndex)
            .as("consecutive groupings must tile the result set with no gap")
            .isEqualTo(expectedNextResultIndex);
        assertThat(grouping.groupBy)
            .as("consecutive groupings must have different groupBy values — otherwise the "
                + "algorithm failed to merge adjacent same-key items")
            .isNotEqualTo(previousGroupBy);
      }
      expectedNextResultIndex = items.get(items.size() - 1).resultIndex + 1;
      previousGroupBy = grouping.groupBy;
    }
  }

  private static int countGroupedItemsByGroupBy(final SearchResultDTO result, final String groupBy) {
    return result.groupingByDTOS.stream()
        .filter(g -> groupBy.equals(g.groupBy))
        .mapToInt(g -> g.searchResultItemDTOS.size())
        .sum();
  }
}
