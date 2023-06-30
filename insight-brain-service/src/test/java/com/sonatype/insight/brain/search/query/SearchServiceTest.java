/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.query;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.core.StreamingOutput;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.report.ReportTestUtils;
import com.sonatype.insight.brain.search.docs.DocumentBuilder.ItemType;
import com.sonatype.insight.brain.search.index.IndexService;
import com.sonatype.insight.brain.search.index.VulnerabilityDescriptionFetcher;
import com.sonatype.insight.brain.search.results.GroupingByDTO;
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

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;

import static com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryCollector.TOTAL_SEARCHES;
import static com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryCollector.TOTAL_SEARCHES_BY_FIELD_NAME;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

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

  @Inject
  private ApiConfigurationService configurationService;

  @Mock
  private VulnerabilityDescriptionFetcher vulnerabilityDescriptionFetcher;

  @Override
  public void configure(Binder binder) {
    lenient().when(vulnerabilityDescriptionFetcher.getVulnerabilityDescription(anyString())).thenReturn("");
    binder.bind(VulnerabilityDescriptionFetcher.class).toInstance(vulnerabilityDescriptionFetcher);
    super.configure(binder);
  }

  @Test
  public void testSearchIndex_NoSearchIndexDirectory() {
    assertThatExceptionOfType(ConflictException.class).isThrownBy(() -> searchService.searchIndex("query", 1, 1, false))
        .withMessageContaining("Index does not exist or is unreadable, please (re)create your index.");
  }

  @Test
  public void testSearchIndex_EmptySearchIndexDirectory() throws Exception {
    Files.createDirectories(insightWork.getSearchIndexDir().toPath());
    assertThatExceptionOfType(ConflictException.class).isThrownBy(() -> searchService.searchIndex("query", 1, 1, false))
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
    searchService.searchIndex("organizationName:org1 itemType:it2", 1, 0, true);
    searchService.searchIndex("itemType:it1", 1, 0, false);
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
    searchService.searchIndex("itemType:it1", 10, 1, false);
    TelemetryData telemetryData = collectSearchTelemetry();

    assertThat(telemetryData).isNull();
  }

  @Test
  public void testSearchIndex_TelemetryInvalidFieldNameCaptured() throws Exception {
    indexService.createSearchIndex();

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> searchService.searchIndex("invalidFieldName:value", 1, 0, false));

    TelemetryData telemetryData = collectSearchTelemetry();

    @SuppressWarnings("unchecked")
    List<SearchCount> actualSearchCounts =
        (List<SearchCount>) telemetryData.getAttributes().get(TOTAL_SEARCHES_BY_FIELD_NAME);
    List<SearchCount> expectedSearchCounts =
        Collections.singletonList(new SearchCount("invalidFieldName", 1));

    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.ADVANCED_SEARCH);
    assertThat(telemetryData.getAttributes()).containsOnlyKeys(TOTAL_SEARCHES, TOTAL_SEARCHES_BY_FIELD_NAME);
    assertThat(actualSearchCounts).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedSearchCounts);
    assertThat(telemetryData.getAttributes().get(TOTAL_SEARCHES)).isEqualTo(1L);
  }

  @Test
  public void testSearchIndex_TelemetryDuplicateFieldNamesInQueryAreIgnored() throws Exception {
    indexService.createSearchIndex();
    searchService.searchIndex("itemType:it1 itemType:it2", 1, 0, true);
    TelemetryData telemetryData = collectSearchTelemetry();

    @SuppressWarnings("unchecked")
    List<SearchCount> actualSearchCounts =
        (List<SearchCount>) telemetryData.getAttributes().get(TOTAL_SEARCHES_BY_FIELD_NAME);
    List<SearchCount> expectedSearchCounts = Collections.singletonList(new SearchCount("itemType", 1));

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
    SearchResultDTO searchResultDTO = searchService.searchIndex("itemType:APPLICATION", 20, 0, false);

    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(1);

    List<String> applicationIds = searchResultDTO.groupingByDTOS.stream()
        .flatMap(groupingByDTO -> groupingByDTO.searchResultItemDTOS.stream())
        .map(searchResultItemDTO -> searchResultItemDTO.applicationId)
        .collect(toList());
    assertThat(applicationIds).containsOnly(application.getId());
  }

  @Test
  public void testSearchIndex_AllPermittedApplicationsReturned() throws IOException {
    Role nonGlobalReadRole = tempEntity.newRole(false, Permission.READ);

    Application application = tempEntity.newApplicationWithParent();
    Application anotherApplication = tempEntity.newApplicationWithParent();

    UserPrincipal userPrincipal = (UserPrincipal) subject.getPrincipal();
    tempEntity.newMembershipMapping(application.getId(), nonGlobalReadRole.getId(), userPrincipal.getUsername());
    tempEntity.newMembershipMapping(anotherApplication.getId(), nonGlobalReadRole.getId(), userPrincipal.getUsername());

    indexService.createSearchIndex();
    SearchResultDTO searchResultDTO = searchService.searchIndex("itemType:APPLICATION", 20, 0, false);

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

    SearchResultDTO searchResultDTO = searchService.searchIndex("itemType:ORGANIZATION", 20, 0, false);
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(1);

    List<String> organizationIds = searchResultDTO.groupingByDTOS.stream()
        .flatMap(groupingByDTO -> groupingByDTO.searchResultItemDTOS.stream())
        .map(searchResultItemDTO -> searchResultItemDTO.organizationId)
        .collect(toList());
    assertThat(organizationIds).containsOnly(organization.getId());

    searchResultDTO = searchService.searchIndex("itemType:APPLICATION", 20, 0, false);

    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(2);
    List<String> applicationIds = searchResultDTO.groupingByDTOS.stream()
        .flatMap(groupingByDTO -> groupingByDTO.searchResultItemDTOS.stream())
        .map(searchResultItemDTO -> searchResultItemDTO.applicationId)
        .collect(toList());
    assertThat(applicationIds).containsOnly(organizationApplication1.getId(), organizationApplication2.getId());
  }

  @Test
  public void testSearchIndex_ApplicationsReturnedForGlobalContextPermission() throws IOException {
    testSearchIndex_ShouldReturnAll(tempEntity.newRole(true, Permission.READ), MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void testSearchIndex_ApplicationsReturnedForRootOrganizationContextPermission() throws IOException {
    testSearchIndex_ShouldReturnAll(tempEntity.newRole(false, Permission.READ), Organization.ROOT_ORGANIZATION_ID);
  }

  private void testSearchIndex_ShouldReturnAll(Role role, String contextId) throws IOException {
    Organization org1 = tempEntity.newOrganization();
    Organization org2 = tempEntity.newOrganization();

    tempEntity.newApplication(org1.getId());
    tempEntity.newApplication(org1.getId());

    tempEntity.newApplication(org2.getId());
    tempEntity.newApplication(org2.getId());

    UserPrincipal userPrincipal = (UserPrincipal) subject.getPrincipal();

    tempEntity.newMembershipMapping(contextId, role.getId(), userPrincipal.getUsername());

    indexService.createSearchIndex();

    SearchResultDTO searchResultDTO = searchService.searchIndex("itemType:ORGANIZATION", 20, 0, false);
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(3); // 2 orgs + the Root org = 3

    searchResultDTO = searchService.searchIndex("itemType:APPLICATION", 20, 0, false);
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(4);  // 4 applications owned by 2 organizations
  }

  @Test
  public void testSearchIndex_ApplicationsReturnedForOrganizationContextPermission() throws IOException {
    Role role = tempEntity.newRole(false, Permission.READ);

    Organization org1 = tempEntity.newOrganization();
    Organization org2 = tempEntity.newOrganization();

    Application app1 = tempEntity.newApplication(org1.getId());
    Application app2 = tempEntity.newApplication(org1.getId());

    tempEntity.newApplication(org2.getId());
    tempEntity.newApplication(org2.getId());

    UserPrincipal userPrincipal = (UserPrincipal) subject.getPrincipal();

    tempEntity.newMembershipMapping(org1.getId(), role.getId(), userPrincipal.getUsername());

    indexService.createSearchIndex();

    SearchResultDTO searchResultDTO = searchService.searchIndex("itemType:ORGANIZATION", 20, 0, false);

    List<String> organizationIds = searchResultDTO.groupingByDTOS.stream()
        .flatMap(groupingByDTO -> groupingByDTO.searchResultItemDTOS.stream())
        .map(searchResultItemDTO -> searchResultItemDTO.organizationId)
        .collect(toList());
    assertThat(organizationIds).containsExactly(org1.getId());

    searchResultDTO = searchService.searchIndex("itemType:APPLICATION", 20, 0, false);

    List<String> applicationIds = searchResultDTO.groupingByDTOS.stream()
        .flatMap(groupingByDTO -> groupingByDTO.searchResultItemDTOS.stream())
        .map(searchResultItemDTO -> searchResultItemDTO.applicationId)
        .collect(toList());
    assertThat(applicationIds).containsExactlyInAnyOrder(app1.getId(), app2.getId());
  }

  @Test
  public void testSearchIndex_PoliciesReturnedForOrganizationContextPermission() throws IOException {
    Role role = tempEntity.newRole(false, Permission.READ);

    Organization org1 = tempEntity.newOrganization();
    Organization org2 = tempEntity.newOrganization();

    Application app1 = tempEntity.newApplication(org1.getId());
    tempEntity.newApplication(org1.getId());

    Application app3 = tempEntity.newApplication(org2.getId());
    tempEntity.newApplication(org2.getId());

    Policy policyOrg1 = tempEntity.newPolicy(org1);
    Policy policyApp1 = tempEntity.newPolicy(app1);
    tempEntity.newPolicy(org2);
    tempEntity.newPolicy(app3);

    UserPrincipal userPrincipal = (UserPrincipal) subject.getPrincipal();

    tempEntity.newMembershipMapping(org1.getId(), role.getId(), userPrincipal.getUsername());

    indexService.createSearchIndex();

    SearchResultDTO searchResultDTO = searchService.searchIndex("itemType:POLICY", 20, 0, false);

    List<String> policyIds = searchResultDTO.groupingByDTOS.stream()
        .flatMap(groupingByDTO -> groupingByDTO.searchResultItemDTOS.stream())
        .map(searchResultItemDTO -> searchResultItemDTO.policyId)
        .collect(toList());
    assertThat(policyIds).containsExactlyInAnyOrder(policyOrg1.getId(), policyApp1.getId());
  }

  @Test
  public void testSearchIndex_MaxAdvancedSearchClauseCountLimitExceeded() throws IOException {
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

      configurationService.setConfigurationNoAuthz(SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT, 2);
      configurationService.applyConfigurationToClients(SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT);
      indexService.createSearchIndex();

      assertThatExceptionOfType(BadRequestException.class)
          .isThrownBy(() -> searchService.searchIndex("itemType:APPLICATION", 1, 0, false))
          .withMessage("Error performing search due to too many clauses. " +
              "Please try narrowing down the query as much as possible " +
              "and consider updating Advanced Search configuration to support larger queries.");
    }
    finally {
      configurationService.deleteConfigurationNoAuthz(SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT);
      configurationService.applyConfigurationToClients(SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT);
    }
  }

  @Test
  public void testSearchIndex_ExportAdvancedSearch() throws Exception {

    Role role = tempEntity.newRole(false, Permission.READ);

    Organization org1 = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplication(org1.getId());
    Policy policyOrg1 = tempEntity.newPolicy(org1);
    Tag tag = tempEntity.newTag(org1.getId(), "Free Apps", "Free apps for customers", Color.light_blue);
    Label label = tempEntity.newLabel(org1.getId(), "My new label");
    tempEntity.newComponentLabel(org1.getId(), label.getId());

    newAppReport(app1.getId(), Stage.ID_RELEASE, "report-id",
        "/IndexSearchingTest/nonVulnerableComponents");

    UserPrincipal userPrincipal = (UserPrincipal) subject.getPrincipal();

    tempEntity.newMembershipMapping(org1.getId(), role.getId(), userPrincipal.getUsername());

    lenient().when(vulnerabilityDescriptionFetcher.getVulnerabilityDescription(anyString())).thenReturn("");

    indexService.createSearchIndex();

    SearchResultDTO searchResultDTO = searchService.searchIndex("itemType:*", 100, 0, true, true);

    List<SearchResultItemDTO> results = searchResultDTO.groupingByDTOS.stream()
        .flatMap(groupingByDTO -> groupingByDTO.searchResultItemDTOS.stream())
        .collect(toList());
    assertThat(results.stream().map(e -> e.itemType).collect(toList()))
        .containsExactlyInAnyOrderElementsOf(Arrays.stream(ItemType.values()).map(Enum::name).collect(toList()));

    StreamingOutput stream = (StreamingOutput) searchService.exportSearch("itemType:*", true).getEntity();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    stream.write(baos);
    List<String> export = Arrays.stream(baos.toString().split("\n")).collect(Collectors.toList());

    assertThat(export).hasSize(8);
    assertThat(export.size() - 1).isEqualTo(results.size());
    assertThat(export.get(0).split(",")).hasSize(16);

    Map<String, List<List<String>>> items = export.stream().skip(1)
        .map(s -> Arrays.stream(s.split(",")).collect(toList()))
        .collect(groupingBy(l -> l.get(0)));

    assertThat(items.get(ItemType.ORGANIZATION.name()).get(0).get(1)).isEqualTo(org1.getName());
    assertThat(items.get(ItemType.APPLICATION.name()).get(0).get(3)).isEqualTo(app1.getName());
    assertThat(items.get(ItemType.APPLICATION_CATEGORY.name()).get(0).get(5)).isEqualTo(tag.getName());
    assertThat(items.get(ItemType.COMPONENT_LABEL.name()).get(0).get(7)).isEqualTo(label.getLabel());
    assertThat(items.get(ItemType.POLICY.name()).get(0).get(9)).isEqualTo(policyOrg1.getName());

    List<SearchResultItemDTO> components = results.stream()
        .filter(sri -> sri.itemType.equals(ItemType.SECURITY_VULNERABILITY.name()) ||
            sri.itemType.equals(ItemType.NON_VULNERABLE_COMPONENT.name()))
        .collect(toList());

    assertThat(items.get(ItemType.SECURITY_VULNERABILITY.name()).get(0).get(12)).isEqualTo(
        components.get(0).componentName);
    assertThat(items.get(ItemType.NON_VULNERABLE_COMPONENT.name()).get(0).get(12)).isEqualTo(
        components.get(1).componentName);

    try {
      configurationService.setConfigurationNoAuthz(SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER,
          ";");
      configurationService.applyConfigurationToClients(
          SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER);
      stream = (StreamingOutput) searchService.exportSearch("itemType:*", true).getEntity();
      baos = new ByteArrayOutputStream();
      stream.write(baos);
      export = Arrays.stream(baos.toString().split("\n")).collect(Collectors.toList());
      assertThat(export.get(0).split(";")).hasSize(16);
    }
    finally {
      configurationService.deleteConfigurationNoAuthz(SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER);
      configurationService.applyConfigurationToClients(
          SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER);
    }
  }

  @Test
  public void testSearchIndex_ImprovedResultGrouping() throws Exception {
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

    SearchResultDTO searchResultDTO = searchService.searchIndex("vulnerabilitySeverity:[7 TO 8]", 10, 0, false);

    // without improved grouping vulnerabilities for "CVE-2022-25857" would have appeared partially in
    // the page 1 results and partially in the page 3 results

    // with improved grouping all 4 vulnerabilities for "CVE-2022-25857" appear in the page 1 results
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(39);
    Optional<GroupingByDTO> optionalGroupingByDTO =
        searchResultDTO.groupingByDTOS.stream().filter(g -> g.groupBy.equals("CVE-2022-25857")).findFirst();
    assertThat(optionalGroupingByDTO).isPresent();
    assertThat(optionalGroupingByDTO.get().searchResultItemDTOS).hasSize(4);
  }

  private PolicyEvaluation newAppReport(String appId, String stageId, String reportId, String reportResourceName)
      throws Exception
  {
    PolicyEvaluation policyEval = tempEntity.newPolicyEvaluation(appId, stageId, reportId);
    ReportTestUtils.createReportFile(policyEval.getApplicationId(), policyEval.getScanId(),
        ReportTestUtils.zipReportDir(reportResourceName, tempDir), insightWork);
    return policyEval;
  }
}
