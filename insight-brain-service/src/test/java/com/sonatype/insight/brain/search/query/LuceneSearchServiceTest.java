/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.query;

import java.nio.file.Files;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class LuceneSearchServiceTest
    extends AbstractSearchServiceTest
{
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

  // TODO: the order this test assumes seems to be lucene specific - consider making a generic version if possible
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

    // Try all results on one page
    // There are 4 results for CVE-2022-25857 but only 2 of these are sequential and should be grouped
    SearchResultDTO searchResultDTO =
        searchService.searchIndex("vulnerabilitySeverity:[7 TO 8]", Integer.MAX_VALUE, 0, false, null, null);
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(39);
    assertThat(searchResultDTO.groupingByDTOS).hasSize(38);

    // First result (page 1)
    assertThat(searchResultDTO.groupingByDTOS.get(1).groupBy).isEqualTo("CVE-2022-25857");
    assertThat(searchResultDTO.groupingByDTOS.get(1).searchResultItemDTOS).hasSize(1);
    assertThat(searchResultDTO.groupingByDTOS.get(1).searchResultItemDTOS.get(0).resultIndex).isEqualTo(2);

    // Second result (page 1)
    assertThat(searchResultDTO.groupingByDTOS.get(16).groupBy).isEqualTo("CVE-2022-25857");
    assertThat(searchResultDTO.groupingByDTOS.get(16).searchResultItemDTOS).hasSize(1);
    assertThat(searchResultDTO.groupingByDTOS.get(16).searchResultItemDTOS.get(0).resultIndex).isEqualTo(17);

    // Third and fourth results (page 1)
    assertThat(searchResultDTO.groupingByDTOS.get(35).groupBy).isEqualTo("CVE-2022-25857");
    assertThat(searchResultDTO.groupingByDTOS.get(35).searchResultItemDTOS).hasSize(2);
    assertThat(searchResultDTO.groupingByDTOS.get(35).searchResultItemDTOS.get(0).resultIndex).isEqualTo(36);
    assertThat(searchResultDTO.groupingByDTOS.get(35).searchResultItemDTOS.get(1).resultIndex).isEqualTo(37);

    // Try splitting a group across pages
    // There are 4 results for CVE-2022-25857 but the 2 sequential results are split across pages and so not grouped
    searchResultDTO = searchService.searchIndex("vulnerabilitySeverity:[7 TO 8]", 36, 0, false, null, null);
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(39);
    assertThat(searchResultDTO.groupingByDTOS).hasSize(36);

    // First result (page 1)
    assertThat(searchResultDTO.groupingByDTOS.get(1).groupBy).isEqualTo("CVE-2022-25857");
    assertThat(searchResultDTO.groupingByDTOS.get(1).searchResultItemDTOS).hasSize(1);
    assertThat(searchResultDTO.groupingByDTOS.get(1).searchResultItemDTOS.get(0).resultIndex).isEqualTo(2);

    // Second result (page 1)
    assertThat(searchResultDTO.groupingByDTOS.get(16).groupBy).isEqualTo("CVE-2022-25857");
    assertThat(searchResultDTO.groupingByDTOS.get(16).searchResultItemDTOS).hasSize(1);
    assertThat(searchResultDTO.groupingByDTOS.get(16).searchResultItemDTOS.get(0).resultIndex).isEqualTo(17);

    // Third result (page 1)
    assertThat(searchResultDTO.groupingByDTOS.get(35).groupBy).isEqualTo("CVE-2022-25857");
    assertThat(searchResultDTO.groupingByDTOS.get(35).searchResultItemDTOS).hasSize(1);
    assertThat(searchResultDTO.groupingByDTOS.get(35).searchResultItemDTOS.get(0).resultIndex).isEqualTo(36);

    searchResultDTO = searchService.searchIndex("vulnerabilitySeverity:[7 TO 8]", 36, 2, false, null, null);
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(39);
    assertThat(searchResultDTO.groupingByDTOS).hasSize(3);

    // Fourth result (page 2)
    assertThat(searchResultDTO.groupingByDTOS.get(0).groupBy).isEqualTo("CVE-2022-25857");
    assertThat(searchResultDTO.groupingByDTOS.get(0).searchResultItemDTOS).hasSize(1);
    assertThat(searchResultDTO.groupingByDTOS.get(0).searchResultItemDTOS.get(0).resultIndex).isEqualTo(37);

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
}
