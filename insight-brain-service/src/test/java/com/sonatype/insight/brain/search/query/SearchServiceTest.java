/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.query;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.search.index.IndexService;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryCollector;
import com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryMetrics.SearchCount;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.Test;

import static com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryCollector.TOTAL_SEARCHES;
import static com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryCollector.TOTAL_SEARCHES_BY_FIELD_NAME;
import static java.util.stream.Collectors.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class SearchServiceTest
    extends AbstractComponentTest
{
  @Inject
  private SearchService searchService;

  @Inject
  private IndexService indexService;

  @Inject
  private InsightWork insightWork;

  @Inject
  private AdvancedSearchTelemetryCollector advancedSearchTelemetryCollector;

  @Test
  public void testSearchIndex_NoSearchIndexDirectory() {
    assertThatExceptionOfType(ConflictException.class).isThrownBy(() -> searchService.searchIndex("query", 1, 1))
        .withMessageContaining("Index does not exist or is unreadable, please (re)create your index.");
  }

  @Test
  public void testSearchIndex_EmptySearchIndexDirectory() throws Exception {
    Files.createDirectories(insightWork.getSearchIndexDir().toPath());
    assertThatExceptionOfType(ConflictException.class).isThrownBy(() -> searchService.searchIndex("query", 1, 1))
        .withMessageContaining("Index does not exist or is unreadable, please (re)create your index.");
  }

  private TelemetryData collectSearchTelemetry() {
    return advancedSearchTelemetryCollector.collectAllData().stream()
        .filter(telemetryData -> TelemetryPurpose.ADVANCED_SEARCH.equals(telemetryData.getPurpose())).findAny()
        .orElse(null);
  }

  @Test
  public void testSearchIndex_Telemetry() throws Exception {
    indexService.createSearchIndex();
    searchService.searchIndex("organizationName:org1 itemType:it2", 1, 0);
    searchService.searchIndex("itemType:it1", 1, 0);
    TelemetryData telemetryData = collectSearchTelemetry();

    @SuppressWarnings("unchecked")
    List<SearchCount> actualSearchCounts =
        (List<SearchCount>) telemetryData.getAttributes().get(TOTAL_SEARCHES_BY_FIELD_NAME);
    List<SearchCount> expectedSearchCounts =
        Arrays.asList(new SearchCount("organizationName", 1), new SearchCount("itemType", 2));

    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.ADVANCED_SEARCH);
    assertThat(telemetryData.getAttributes()).containsOnlyKeys(TOTAL_SEARCHES, TOTAL_SEARCHES_BY_FIELD_NAME);
    assertThat(actualSearchCounts).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedSearchCounts);
    assertThat(telemetryData.getAttributes().get(TOTAL_SEARCHES)).isEqualTo(2L);
  }

  @Test
  public void testSearchIndex_TelemetryNotAddedWhenPagingThroughResults() throws Exception {
    indexService.createSearchIndex();
    searchService.searchIndex("itemType:it1", 10, 1);
    TelemetryData telemetryData = collectSearchTelemetry();

    assertThat(telemetryData).isNull();
  }

  @Test
  public void testSearchIndex_TelemetryInvalidFieldNameCaptured() throws Exception {
    indexService.createSearchIndex();

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      searchService.searchIndex("invalidFieldName:value", 1, 0);
    });

    TelemetryData telemetryData = collectSearchTelemetry();

    @SuppressWarnings("unchecked")
    List<SearchCount> actualSearchCounts =
        (List<SearchCount>) telemetryData.getAttributes().get(TOTAL_SEARCHES_BY_FIELD_NAME);
    List<SearchCount> expectedSearchCounts =
        Arrays.asList(new SearchCount("invalidFieldName", 1));

    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.ADVANCED_SEARCH);
    assertThat(telemetryData.getAttributes()).containsOnlyKeys(TOTAL_SEARCHES, TOTAL_SEARCHES_BY_FIELD_NAME);
    assertThat(actualSearchCounts).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedSearchCounts);
    assertThat(telemetryData.getAttributes().get(TOTAL_SEARCHES)).isEqualTo(1L);
  }

  @Test
  public void testSearchIndex_TelemetryDuplicateFieldNamesInQueryAreIgnored() throws Exception {
    indexService.createSearchIndex();
    searchService.searchIndex("itemType:it1 itemType:it2", 1, 0);
    TelemetryData telemetryData = collectSearchTelemetry();

    @SuppressWarnings("unchecked")
    List<SearchCount> actualSearchCounts =
        (List<SearchCount>) telemetryData.getAttributes().get(TOTAL_SEARCHES_BY_FIELD_NAME);
    List<SearchCount> expectedSearchCounts = Arrays.asList(new SearchCount("itemType", 1));

    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.ADVANCED_SEARCH);
    assertThat(telemetryData.getAttributes()).containsOnlyKeys(TOTAL_SEARCHES, TOTAL_SEARCHES_BY_FIELD_NAME);
    assertThat(actualSearchCounts).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedSearchCounts);
    assertThat(telemetryData.getAttributes().get(TOTAL_SEARCHES)).isEqualTo(1L);
  }

  @Test
  public void testSearchIndex_RestrictedApplicationNotReturnedItemTypeSearch() throws IOException {
    Role nonGlobalReadRole = tempEntity.newRole(false, Permission.READ);

    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newApplicationWithParent();

    UserPrincipal userPrincipal = (UserPrincipal) subject.getPrincipal();
    tempEntity.newMembershipMapping(application.getId(), nonGlobalReadRole.getId(), userPrincipal.getUsername());

    indexService.createSearchIndex();
    SearchResultDTO searchResultDTO = searchService.searchIndex("itemType:APPLICATION", 20, 0);

    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(1);

    List<String> applicationIds = searchResultDTO.groupingByDTOS.stream()
        .flatMap(groupingByDTO -> groupingByDTO.searchResultItemDTOS.stream())
        .map(searchResultItemDTO -> searchResultItemDTO.applicationId)
        .collect(toList());
    assertThat(applicationIds).containsOnly(application.getId()); }

  @Test
  public void testSearchIndex_AllPermittedApplicationsReturned() throws IOException {
    Role nonGlobalReadRole = tempEntity.newRole(false, Permission.READ);

    Application application = tempEntity.newApplicationWithParent();
    Application anotherApplication = tempEntity.newApplicationWithParent();

    UserPrincipal userPrincipal = (UserPrincipal) subject.getPrincipal();
    tempEntity.newMembershipMapping(application.getId(), nonGlobalReadRole.getId(), userPrincipal.getUsername());
    tempEntity.newMembershipMapping(anotherApplication.getId(), nonGlobalReadRole.getId(), userPrincipal.getUsername());

    indexService.createSearchIndex();
    SearchResultDTO searchResultDTO = searchService.searchIndex("itemType:APPLICATION", 20, 0);

    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(2);

    List<String> applicationIds = searchResultDTO.groupingByDTOS.stream()
        .flatMap(groupingByDTO -> groupingByDTO.searchResultItemDTOS.stream())
        .map(searchResultItemDTO -> searchResultItemDTO.applicationId)
        .collect(toList());
    assertThat(applicationIds).containsOnly(application.getId(), anotherApplication.getId());
  }

  @Test
  public void testSearchIndex_ApplicationsOfOrganizationReturned() throws IOException {
    Role nonGlobalReadRole = tempEntity.newRole(false, Permission.READ);

    Organization organization = tempEntity.newOrganization();
    Organization restrictedOrganization = tempEntity.newOrganization();

    Application organizationApplication1 = tempEntity.newApplication(organization.getId());
    Application organizationApplication2 = tempEntity.newApplication(organization.getId());

    tempEntity.newApplication(restrictedOrganization.getId());
    tempEntity.newApplication(restrictedOrganization.getId());

    UserPrincipal userPrincipal = (UserPrincipal) subject.getPrincipal();
    tempEntity.newMembershipMapping(organization.getId(), nonGlobalReadRole.getId(), userPrincipal.getUsername());

    indexService.createSearchIndex();

    SearchResultDTO searchResultDTO = searchService.searchIndex("itemType:ORGANIZATION", 20, 0);
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(1);

    List<String> organizationIds = searchResultDTO.groupingByDTOS.stream()
        .flatMap(groupingByDTO -> groupingByDTO.searchResultItemDTOS.stream())
        .map(searchResultItemDTO -> searchResultItemDTO.organizationId)
        .collect(toList());
    assertThat(organizationIds).containsOnly(organization.getId());

    searchResultDTO = searchService.searchIndex("itemType:APPLICATION", 20, 0);

    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(2);
    List<String> applicationIds = searchResultDTO.groupingByDTOS.stream()
        .flatMap(groupingByDTO -> groupingByDTO.searchResultItemDTOS.stream())
        .map(searchResultItemDTO -> searchResultItemDTO.applicationId)
        .collect(toList());
    assertThat(applicationIds).containsOnly(organizationApplication1.getId(), organizationApplication2.getId());
  }

  @Test
  public void testSearchIndex_ApplicationsReturnedForGlobalContextPermission() throws IOException {
    Role role = tempEntity.newRole(true, Permission.READ);

    Organization org1 = tempEntity.newOrganization();
    Organization org2 = tempEntity.newOrganization();

    tempEntity.newApplication(org1.getId());
    tempEntity.newApplication(org1.getId());

    tempEntity.newApplication(org2.getId());
    tempEntity.newApplication(org2.getId());

    UserPrincipal userPrincipal = (UserPrincipal) subject.getPrincipal();

    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), userPrincipal.getUsername());

    indexService.createSearchIndex();

    SearchResultDTO searchResultDTO = searchService.searchIndex("itemType:ORGANIZATION", 20, 0);
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(3); // 2 orgs + the Root org = 3

    searchResultDTO = searchService.searchIndex("itemType:APPLICATION", 20, 0);
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(4);  // 4 applications owned by 2 organizations
  }
}
