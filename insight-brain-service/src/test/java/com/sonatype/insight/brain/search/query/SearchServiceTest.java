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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.ws.rs.core.StreamingOutput;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductMode;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.report.ReportTestUtils;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.search.docs.DocumentBuilder.ItemType;
import com.sonatype.insight.brain.search.index.IndexService;
import com.sonatype.insight.brain.search.index.VulnerabilityDescriptionFetcher;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryCollector;
import com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryMetrics.SearchCount;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.error.exception.NotAuthorizedException;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.inject.Binder;
import org.apache.commons.lang.StringUtils;
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

  @Inject
  private TestProductLicense testProductLicense;

  @Override
  public void configure(Binder binder) {
    lenient().when(vulnerabilityDescriptionFetcher.getVulnerabilityDescription(anyString())).thenReturn("");
    binder.bind(VulnerabilityDescriptionFetcher.class).toInstance(vulnerabilityDescriptionFetcher);
    super.configure(binder);
  }

  @Test
  public void testSearchIndex_AdvancedSearchConfigurationDisabled() {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.setEnabled(false);

    assertThatExceptionOfType(NotAuthorizedException.class)
        .isThrownBy(() -> searchService.searchIndex("query", 1, 1, false, null))
        .withMessage("advanced-search-configuration feature is disabled.");
  }

  @Test
  public void testSearchIndex_NoSearchIndexDirectory() {
    assertThatExceptionOfType(ConflictException.class).isThrownBy(
        () -> searchService.searchIndex("query", 1, 1, false, null))
        .withMessageContaining("Index does not exist or is unreadable, please (re)create your index.");
  }

  @Test
  public void testSearchIndex_EmptySearchIndexDirectory() throws Exception {
    Files.createDirectories(insightWork.getSearchIndexDir().toPath());
    assertThatExceptionOfType(ConflictException.class).isThrownBy(
        () -> searchService.searchIndex("query", 1, 1, false, null))
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
    searchService.searchIndex("organizationName:org1 itemType:it2", 1, 0, true, null);
    searchService.searchIndex("itemType:it1", 1, 0, false, null);
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
    searchService.searchIndex("itemType:it1", 10, 1, false, null);
    TelemetryData telemetryData = collectSearchTelemetry();

    assertThat(telemetryData).isNull();
  }

  @Test
  public void testSearchIndex_TelemetryInvalidFieldNameCaptured() throws Exception {
    indexService.createSearchIndex();

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> searchService.searchIndex("invalidFieldName:value", 1, 0, false, null));

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
    searchService.searchIndex("itemType:it1 itemType:it2", 1, 0, true, null);
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
    SearchResultDTO searchResultDTO = searchService.searchIndex("itemType:APPLICATION", 20, 0, false, null);

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
    SearchResultDTO searchResultDTO = searchService.searchIndex("itemType:APPLICATION", 20, 0, false, null);

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

    SearchResultDTO searchResultDTO = searchService.searchIndex("itemType:ORGANIZATION", 20, 0, false, null);
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(1);

    List<String> organizationIds = searchResultDTO.groupingByDTOS.stream()
        .flatMap(groupingByDTO -> groupingByDTO.searchResultItemDTOS.stream())
        .map(searchResultItemDTO -> searchResultItemDTO.organizationId)
        .collect(toList());
    assertThat(organizationIds).containsOnly(organization.getId());

    searchResultDTO = searchService.searchIndex("itemType:APPLICATION", 20, 0, false, null);

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

    SearchResultDTO searchResultDTO = searchService.searchIndex("itemType:ORGANIZATION", 20, 0, false, null);
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(3); // 2 orgs + the Root org = 3

    searchResultDTO = searchService.searchIndex("itemType:APPLICATION", 20, 0, false, null);
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

    SearchResultDTO searchResultDTO = searchService.searchIndex("itemType:ORGANIZATION", 20, 0, false, null);

    List<String> organizationIds = searchResultDTO.groupingByDTOS.stream()
        .flatMap(groupingByDTO -> groupingByDTO.searchResultItemDTOS.stream())
        .map(searchResultItemDTO -> searchResultItemDTO.organizationId)
        .collect(toList());
    assertThat(organizationIds).containsExactly(org1.getId());

    searchResultDTO = searchService.searchIndex("itemType:APPLICATION", 20, 0, false, null);

    List<String> applicationIds = searchResultDTO.groupingByDTOS.stream()
        .flatMap(groupingByDTO -> groupingByDTO.searchResultItemDTOS.stream())
        .map(searchResultItemDTO -> searchResultItemDTO.applicationId)
        .collect(toList());
    assertThat(applicationIds).containsExactlyInAnyOrder(app1.getId(), app2.getId());
  }

  @Test
  public void testSearchIndex_SearchVulnerabilityAndOrganization() throws Exception {
    Role role = tempEntity.newRole(false, Permission.READ);

    Organization org1 = tempEntity.newOrganization("org-01");
    Organization org2 = tempEntity.newOrganization("org-02");

    Application app1 = tempEntity.newApplication("app-01", org1.getId());
    newAppReport(app1.getId(), Stage.ID_RELEASE, "report-1", "/SearchServiceTest/report-1");

    Application app2 = tempEntity.newApplication("app-02", org2.getId());
    newAppReport(app2.getId(), Stage.ID_RELEASE, "report-2", "/SearchServiceTest/report-2");

    UserPrincipal userPrincipal = (UserPrincipal) subject.getPrincipal();
    tempEntity.newMembershipMapping(org1.getId(), role.getId(), userPrincipal.getUsername());

    indexService.createSearchIndex();

    SearchResultDTO searchResultDTO =
        searchService.searchIndex("CVE-2022-25857 AND organizationName:org-01", 10, 0, true, null);
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(3);

    searchResultDTO = searchService.searchIndex("organizationName:org-02", 10, 0, true, null);
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(0);  // insufficient permissions

    tempEntity.newMembershipMapping(org2.getId(), role.getId(), userPrincipal.getUsername());
    searchResultDTO = searchService.searchIndex("CVE-2022-25857 AND organizationName:org-02", 10, 0, true, null);
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(1);  // sufficient permissions

    searchResultDTO = searchService.searchIndex("CVE-2022-25857", 10, 0, true, null);
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(4);  // no org filter
  }

  @Test
  public void testSearchIndex_SearchVulnerabilityAndParentOrganization() throws Exception {
    Role role = tempEntity.newRole(false, Permission.READ);

    Organization parentOrg = tempEntity.newOrganization("parent-organization");
    Organization childOrg = tempEntity.newOrganization("child-organization");

    Application app1 = tempEntity.newApplication("app-01", parentOrg.getId());
    newAppReport(app1.getId(), Stage.ID_RELEASE, "report-1", "/SearchServiceTest/report-1");

    Application app2 = tempEntity.newApplication("app-02", childOrg.getId());
    newAppReport(app2.getId(), Stage.ID_RELEASE, "report-2", "/SearchServiceTest/report-2");

    UserPrincipal userPrincipal = (UserPrincipal) subject.getPrincipal();
    tempEntity.newMembershipMapping(parentOrg.getId(), role.getId(), userPrincipal.getUsername());
    tempEntity.newMembershipMapping(childOrg.getId(), role.getId(), userPrincipal.getUsername());

    indexService.createSearchIndex();

    SearchResultDTO searchResultDTO =
        searchService.searchIndex("CVE-2022-25857 AND organizationName:parent-organization", 10, 0, true, null);
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(3);

    searchResultDTO =
        searchService.searchIndex("CVE-2022-25857 AND organizationName:child-organization", 10, 0, true, null);
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(1);
  }

  @Test
  public void testSearchIndex_SearchVulnerabilityAndOrganizationNLevelContextPermission() throws Exception {
    Role role = tempEntity.newRole(false, Permission.READ);

    Organization org1 = tempEntity.newOrganization("org-01");
    Organization org2 = tempEntity.newOrganization("org-02", org1);
    Organization org3 = tempEntity.newOrganization("org-03", org2);

    Application app1 = tempEntity.newApplication("app-01", org1.getId());
    newAppReport(app1.getId(), Stage.ID_RELEASE, "report-1", "/SearchServiceTest/report-1");

    Application app2 = tempEntity.newApplication("app-02", org2.getId());
    newAppReport(app2.getId(), Stage.ID_RELEASE, "report-2", "/SearchServiceTest/report-2");

    Application app3 = tempEntity.newApplication("app-03", org3.getId());
    newAppReport(app3.getId(), Stage.ID_RELEASE, "report-2", "/SearchServiceTest/report-2");

    UserPrincipal userPrincipal = (UserPrincipal) subject.getPrincipal();
    tempEntity.newMembershipMapping(org3.getId(), role.getId(), userPrincipal.getUsername());

    indexService.createSearchIndex();

    SearchResultDTO searchResultDTO =
        searchService.searchIndex("CVE-2022-25857", 10, 0, true, null);
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(1);

    tempEntity.newMembershipMapping(org2.getId(), role.getId(), userPrincipal.getUsername());
    searchResultDTO = searchService.searchIndex("CVE-2022-25857", 10, 0, true, null);
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(2);  // insufficient permissions

    tempEntity.newMembershipMapping(org1.getId(), role.getId(), userPrincipal.getUsername());
    searchResultDTO = searchService.searchIndex("CVE-2022-25857", 10, 0, true, null);
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(5);  // sufficient permissions
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

    SearchResultDTO searchResultDTO = searchService.searchIndex("itemType:POLICY", 20, 0, false, null);

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
          .isThrownBy(() -> searchService.searchIndex("itemType:APPLICATION", 1, 0, false, null))
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
  public void testExportSearch_AdvancedSearchConfigurationDisabled() {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.setEnabled(false);

    assertThatExceptionOfType(NotAuthorizedException.class)
        .isThrownBy(() -> searchService.exportSearch("itemType:*", true, null))
        .withMessage("advanced-search-configuration feature is disabled.");
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

    SearchResultDTO searchResultDTO = searchService.searchIndex("itemType:*", 100, 0, true, true, null);

    List<SearchResultItemDTO> results = searchResultDTO.groupingByDTOS.stream()
        .flatMap(groupingByDTO -> groupingByDTO.searchResultItemDTOS.stream())
        .collect(toList());

    List<String> actualItemTypes = results.stream().map(e -> e.itemType).collect(toList());
    List<String> expectedItemTypes = Arrays.stream(ItemType.values()).map(Enum::name).collect(toList());

    // TODO temporary until SBOM Advanced Search is fully implemented
    expectedItemTypes.remove("SBOM_METADATA");
    assertThat(actualItemTypes).containsExactlyInAnyOrderElementsOf(expectedItemTypes);

    StreamingOutput stream = (StreamingOutput) searchService.exportSearch("itemType:*", true, null).getEntity();
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
    assertThat(items.get(ItemType.SECURITY_VULNERABILITY.name()).get(0).get(1)).isEqualTo(org1.getName());
    assertThat(items.get(ItemType.SECURITY_VULNERABILITY.name()).get(0).get(2))
        .isEqualTo("ui/links/organization/" + org1.getId() + "/management");
    assertThat(items.get(ItemType.NON_VULNERABLE_COMPONENT.name()).get(0).get(12)).isEqualTo(
        components.get(1).componentName);

    try {
      configurationService.setConfigurationNoAuthz(SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER,
          ";");
      configurationService.applyConfigurationToClients(
          SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER);
      stream = (StreamingOutput) searchService.exportSearch("itemType:*", true, null).getEntity();
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
  public void testSearchIndex_SbomManagerMode_MissingLicensedFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.SBOM_MANAGER);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> searchService.searchIndex("itemType:*", 100, 0, true, true, ProductMode.SBOM_MANAGER))
        .withMessageContaining("The SBOM Manager feature is not supported by your license.");
  }

  @Test
  public void testSearchIndex_SbomManagerFeature_DefaultModeNotSupported() {
    testProductLicense.setProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> searchService.searchIndex("itemType:*", 100, 0, true, true, null))
        .withMessageContaining("Only SBOM Manager mode is supported by your license.");
  }

  @Test
  public void testSearchIndex_SbomManagerMode() throws Exception {
    UserPrincipal userPrincipal = (UserPrincipal) subject.getPrincipal();
    Role role = tempEntity.newRole(false, Permission.READ);
    tempEntity.newMembershipMapping(Organization.ROOT_ORGANIZATION_ID, role.getId(), userPrincipal.getUsername());
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    tempEntity.newSbomEvaluation(app,
        "1.0",
        SbomSpecification.CYCLONEDX.toString(),
        PackageUrlIdentifier.fromComponentIdentifier(ComponentIdentifier.createAnameCoordinates("n", null, "v1")),
        "someScanId1",
        true);
    tempEntity.newSbomEvaluation(app,
        "1.1",
        SbomSpecification.SPDX.toString(),
        PackageUrlIdentifier.fromComponentIdentifier(ComponentIdentifier.createAnameCoordinates("n", null, "v2")),
        "someScanId2",
        false);
    tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID); // Should not be returned
    tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID); // Should not be returned
    tempEntity.newPolicy(); // Should not be returned
    newAppReport(app.getId(), Stage.ID_BUILD, "someScanId3", "/SearchServiceTest/report-3");
    indexService.createSearchIndex();
    assertThat(searchService.searchIndex("applicationCategoryId:*", 100, 0, true, true,
        ProductMode.SBOM_MANAGER).groupingByDTOS).isEmpty();
    assertThat(searchService.searchIndex("componentLabelId:*", 100, 0, true, true,
        ProductMode.SBOM_MANAGER).groupingByDTOS).isEmpty();
    assertThat(searchService.searchIndex("policyId:*", 100, 0, true, true,
        ProductMode.SBOM_MANAGER).groupingByDTOS).isEmpty();

    SearchResultDTO searchResultDTO =
        searchService.searchIndex("itemType:*", 100, 0, true, true, ProductMode.SBOM_MANAGER);

    List<SearchResultItemDTO> results = searchResultDTO.groupingByDTOS.stream()
        .flatMap(groupingByDTO -> groupingByDTO.searchResultItemDTOS.stream())
        .collect(toList());
    assertThat(results).hasSize(7);
    assertThat(find(results, ItemType.ORGANIZATION,
        s -> Organization.ROOT_ORGANIZATION_ID.equals(s.organizationId))).hasSize(1);
    assertThat(find(results, ItemType.ORGANIZATION, s -> org.getId().equals(s.organizationId))).hasSize(1);
    assertThat(find(results, ItemType.APPLICATION, s -> app.getId().equals(s.applicationId))).hasSize(1);
    assertThat(find(results, ItemType.SBOM_METADATA, s -> "1.0".equals(s.applicationVersion))).hasSize(1);
    assertThat(find(results, ItemType.SBOM_METADATA, s -> "1.1".equals(s.applicationVersion))).hasSize(1);
    List<SearchResultItemDTO> securityVulnerabilities =
        find(results, ItemType.SECURITY_VULNERABILITY, s -> "n v1".equals(s.componentName));
    assertThat(securityVulnerabilities).hasSize(1);
    SearchResultItemDTO securityVulnerability = securityVulnerabilities.get(0);
    assertThat(securityVulnerability.applicationVersion).isEqualTo("1.0");
    assertThat(securityVulnerability.sbomSpecification).isEqualTo(SbomSpecification.CYCLONEDX.toString());
    assertThat(securityVulnerability.policyEvaluationStage).isNull();
    assertThat(securityVulnerability.reportId).isNull();
    List<SearchResultItemDTO> nonVulnerableComponents =
        find(results, ItemType.NON_VULNERABLE_COMPONENT, s -> "n v2".equals(s.componentName));
    assertThat(nonVulnerableComponents).hasSize(1);
    SearchResultItemDTO nonVulnerableComponent = nonVulnerableComponents.get(0);
    assertThat(nonVulnerableComponent.applicationVersion).isEqualTo("1.1");
    assertThat(nonVulnerableComponent.sbomSpecification).isEqualTo(SbomSpecification.SPDX.toString());
    assertThat(nonVulnerableComponent.policyEvaluationStage).isNull();
    assertThat(nonVulnerableComponent.reportId).isNull();
    assertThat(find(results, ItemType.SECURITY_VULNERABILITY, s -> "someScanId3".equals(s.reportId))).isEmpty();
    assertThat(find(results, ItemType.NON_VULNERABLE_COMPONENT, s -> "someScanId3".equals(s.reportId))).isEmpty();
  }

  @Test
  public void testSearchIndex_DefaultMode() throws Exception {
    UserPrincipal userPrincipal = (UserPrincipal) subject.getPrincipal();
    Role role = tempEntity.newRole(false, Permission.READ);
    tempEntity.newMembershipMapping(Organization.ROOT_ORGANIZATION_ID, role.getId(), userPrincipal.getUsername());
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    tempEntity.newSbomEvaluation(app,
        "1.0",
        SbomSpecification.CYCLONEDX.toString(),
        PackageUrlIdentifier.fromComponentIdentifier(ComponentIdentifier.createAnameCoordinates("n", null, "v1")),
        "someScanId1",
        true);
    tempEntity.newSbomEvaluation(app,
        "1.1",
        SbomSpecification.SPDX.toString(),
        PackageUrlIdentifier.fromComponentIdentifier(ComponentIdentifier.createAnameCoordinates("n", null, "v2")),
        "someScanId2",
        false);
    newAppReport(app.getId(), Stage.ID_BUILD, "someScanId3", "/SearchServiceTest/report-3");
    Tag tag = tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID);
    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);
    Policy policy = tempEntity.newPolicy();
    indexService.createSearchIndex();
    assertThat(searchService.searchIndex("applicationVersion:*", 100, 0, true, true, null).groupingByDTOS).isEmpty();

    SearchResultDTO searchResultDTO = searchService.searchIndex("itemType:*", 100, 0, true, true, null);

    List<SearchResultItemDTO> results = searchResultDTO.groupingByDTOS.stream()
        .flatMap(groupingByDTO -> groupingByDTO.searchResultItemDTOS.stream())
        .collect(toList());
    assertThat(results).hasSize(9);
    assertThat(find(results, ItemType.ORGANIZATION,
        s -> Organization.ROOT_ORGANIZATION_ID.equals(s.organizationId))).hasSize(1);
    assertThat(find(results, ItemType.ORGANIZATION, s -> org.getId().equals(s.organizationId))).hasSize(1);
    assertThat(find(results, ItemType.APPLICATION, s -> app.getId().equals(s.applicationId))).hasSize(1);
    assertThat(find(results, ItemType.APPLICATION_CATEGORY, s -> tag.getId().equals(s.applicationCategoryId))).hasSize(
        1);
    assertThat(find(results, ItemType.COMPONENT_LABEL, s -> label.getId().equals(s.componentLabelId))).hasSize(1);
    assertThat(find(results, ItemType.POLICY, s -> policy.getId().equals(s.policyId))).hasSize(1);
    assertThat(find(results, ItemType.SECURITY_VULNERABILITY, s -> "n v1".equals(s.componentName))).isEmpty();
    assertThat(
        find(results, ItemType.SECURITY_VULNERABILITY, s -> StringUtils.isNotBlank(s.applicationVersion))).isEmpty();
    assertThat(find(results, ItemType.NON_VULNERABLE_COMPONENT, s -> "n v2".equals(s.componentName))).isEmpty();
    assertThat(
        find(results, ItemType.NON_VULNERABLE_COMPONENT, s -> StringUtils.isNotBlank(s.applicationVersion))).isEmpty();
    assertThat(find(results, ItemType.SECURITY_VULNERABILITY, s -> "someScanId3".equals(s.reportId))).hasSize(2);
    assertThat(find(results, ItemType.NON_VULNERABLE_COMPONENT, s -> "someScanId3".equals(s.reportId))).hasSize(1);
  }

  @Test
  public void testSearchIndex_ExportAdvancedSearch_SbomManagerMode() throws Exception {
    UserPrincipal userPrincipal = (UserPrincipal) subject.getPrincipal();
    Role role = tempEntity.newRole(false, Permission.READ);
    tempEntity.newMembershipMapping(Organization.ROOT_ORGANIZATION_ID, role.getId(), userPrincipal.getUsername());
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    tempEntity.newSbomEvaluation(app,
        "1.0",
        SbomSpecification.CYCLONEDX.toString(),
        PackageUrlIdentifier.fromComponentIdentifier(ComponentIdentifier.createAnameCoordinates("n", null, "v1")),
        "someScanId1",
        true);
    tempEntity.newSbomEvaluation(app,
        "1.1",
        SbomSpecification.SPDX.toString(),
        PackageUrlIdentifier.fromComponentIdentifier(ComponentIdentifier.createAnameCoordinates("n", null, "v2")),
        "someScanId2",
        false);
    tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID); // Should not be returned
    tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID); // Should not be returned
    tempEntity.newPolicy(); // Should not be returned
    newAppReport(app.getId(), Stage.ID_BUILD, "someScanId3", "/SearchServiceTest/report-3");
    indexService.createSearchIndex();
    SearchResultDTO searchResultDTO =
        searchService.searchIndex("itemType:*", 100, 0, true, true, ProductMode.SBOM_MANAGER);
    List<SearchResultItemDTO> results = searchResultDTO.groupingByDTOS.stream()
        .flatMap(groupingByDTO -> groupingByDTO.searchResultItemDTOS.stream())
        .collect(toList());

    StreamingOutput stream =
        (StreamingOutput) searchService.exportSearch("itemType:*", true, ProductMode.SBOM_MANAGER).getEntity();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    stream.write(baos);
    List<String> rows = Arrays.asList(baos.toString().split("\n"));
    Map<String, List<List<String>>> items = rows.stream()
        .skip(1)
        .map(s -> Arrays.stream(s.split(",")).collect(toList()))
        .collect(groupingBy(l -> l.get(0)));
    assertThat(rows).hasSize(8);
    assertThat(rows.size() - 1).isEqualTo(results.size());
    assertThat(items).hasSize(5);
    assertThat(items.get(ItemType.ORGANIZATION.name()).get(0).get(1)).isEqualTo("Root Organization");
    assertThat(items.get(ItemType.ORGANIZATION.name()).get(0).get(2)).contains(Organization.ROOT_ORGANIZATION_ID);
    assertThat(items.get(ItemType.ORGANIZATION.name()).get(1).get(1)).isEqualTo(org.getName());
    assertThat(items.get(ItemType.ORGANIZATION.name()).get(1).get(2)).contains(org.getId());
    assertThat(items.get(ItemType.APPLICATION.name()).get(0).get(1)).isEqualTo(org.getName());
    assertThat(items.get(ItemType.APPLICATION.name()).get(0).get(2)).contains(org.getId());
    assertThat(items.get(ItemType.APPLICATION.name()).get(0).get(3)).isEqualTo(app.getName());
    assertThat(items.get(ItemType.APPLICATION.name()).get(0).get(4)).contains(app.getPublicId());
    assertThat(items.get(ItemType.SECURITY_VULNERABILITY.name()).get(0).get(1)).isEqualTo(org.getName());
    assertThat(items.get(ItemType.SECURITY_VULNERABILITY.name()).get(0).get(2)).contains(org.getId());
    assertThat(items.get(ItemType.SECURITY_VULNERABILITY.name()).get(0).get(3)).isEqualTo(app.getName());
    assertThat(items.get(ItemType.SECURITY_VULNERABILITY.name()).get(0).get(4)).contains(app.getPublicId());
    assertThat(items.get(ItemType.SECURITY_VULNERABILITY.name()).get(0).get(10)).isEqualTo("n v1");
    assertThat(items.get(ItemType.SECURITY_VULNERABILITY.name()).get(0).get(13)).contains("1.0");
    assertThat(items.get(ItemType.SECURITY_VULNERABILITY.name()).get(0).get(14)).contains("CycloneDx");
    assertThat(items.get(ItemType.NON_VULNERABLE_COMPONENT.name()).get(0).get(1)).isEqualTo(org.getName());
    assertThat(items.get(ItemType.NON_VULNERABLE_COMPONENT.name()).get(0).get(2)).contains(org.getId());
    assertThat(items.get(ItemType.NON_VULNERABLE_COMPONENT.name()).get(0).get(3)).isEqualTo(app.getName());
    assertThat(items.get(ItemType.NON_VULNERABLE_COMPONENT.name()).get(0).get(4)).contains(app.getPublicId());
    assertThat(items.get(ItemType.NON_VULNERABLE_COMPONENT.name()).get(0).get(10)).isEqualTo("n v2");
    assertThat(items.get(ItemType.NON_VULNERABLE_COMPONENT.name()).get(0).get(13)).contains("1.1");
    assertThat(items.get(ItemType.NON_VULNERABLE_COMPONENT.name()).get(0).get(14)).contains("SPDX");
    assertThat(items.get(ItemType.SBOM_METADATA.name()).get(0).get(3)).isEqualTo(app.getName());
    assertThat(items.get(ItemType.SBOM_METADATA.name()).get(0).get(4)).contains(app.getPublicId());
    assertThat(items.get(ItemType.SBOM_METADATA.name()).get(0).get(13)).contains("1.0");
    assertThat(items.get(ItemType.SBOM_METADATA.name()).get(0).get(14)).contains("CycloneDx");
    assertThat(items.get(ItemType.SBOM_METADATA.name()).get(1).get(3)).isEqualTo(app.getName());
    assertThat(items.get(ItemType.SBOM_METADATA.name()).get(1).get(4)).contains(app.getPublicId());
    assertThat(items.get(ItemType.SBOM_METADATA.name()).get(1).get(13)).contains("1.1");
    assertThat(items.get(ItemType.SBOM_METADATA.name()).get(1).get(14)).contains("SPDX");
  }

  @Test
  public void testSearchIndex_ExportAdvancedSearch_DefaultMode() throws Exception {
    UserPrincipal userPrincipal = (UserPrincipal) subject.getPrincipal();
    Role role = tempEntity.newRole(false, Permission.READ);
    tempEntity.newMembershipMapping(Organization.ROOT_ORGANIZATION_ID, role.getId(), userPrincipal.getUsername());
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    tempEntity.newSbomEvaluation(app,
        "1.0",
        SbomSpecification.CYCLONEDX.toString(),
        PackageUrlIdentifier.fromComponentIdentifier(ComponentIdentifier.createAnameCoordinates("n", null, "v1")),
        "someScanId1",
        true);
    tempEntity.newSbomEvaluation(app,
        "1.1",
        SbomSpecification.SPDX.toString(),
        PackageUrlIdentifier.fromComponentIdentifier(ComponentIdentifier.createAnameCoordinates("n", null, "v2")),
        "someScanId2",
        false);
    Tag tag = tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID);
    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);
    Policy policy = tempEntity.newPolicy();
    newAppReport(app.getId(), Stage.ID_BUILD, "someScanId3", "/SearchServiceTest/report-3");
    indexService.createSearchIndex();
    SearchResultDTO searchResultDTO = searchService.searchIndex("itemType:*", 100, 0, true, true, null);
    List<SearchResultItemDTO> results = searchResultDTO.groupingByDTOS.stream()
        .flatMap(groupingByDTO -> groupingByDTO.searchResultItemDTOS.stream())
        .collect(toList());

    StreamingOutput stream =
        (StreamingOutput) searchService.exportSearch("itemType:*", true, null).getEntity();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    stream.write(baos);
    List<String> rows = Arrays.asList(baos.toString().split("\n"));
    Map<String, List<List<String>>> items = rows.stream()
        .skip(1)
        .map(s -> Arrays.stream(s.split(",")).collect(toList()))
        .collect(groupingBy(l -> l.get(0)));
    assertThat(rows).hasSize(10);
    assertThat(rows.size() - 1).isEqualTo(results.size());
    assertThat(items).hasSize(7);
    assertThat(items.get(ItemType.ORGANIZATION.name()).get(0).get(1)).isEqualTo("Root Organization");
    assertThat(items.get(ItemType.ORGANIZATION.name()).get(1).get(1)).isEqualTo(org.getName());
    assertThat(items.get(ItemType.APPLICATION.name()).get(0).get(3)).isEqualTo(app.getName());
    assertThat(items.get(ItemType.APPLICATION_CATEGORY.name()).get(0).get(5)).isEqualTo(tag.getName());
    assertThat(items.get(ItemType.COMPONENT_LABEL.name()).get(0).get(7)).isEqualTo(label.getLabel());
    assertThat(items.get(ItemType.POLICY.name()).get(0).get(9)).isEqualTo(policy.getName());
    assertThat(items.get(ItemType.SECURITY_VULNERABILITY.name()).get(0).get(12)).isEqualTo(
        "tomcat : tomcat-util : 5.5.23");
    assertThat(items.get(ItemType.SECURITY_VULNERABILITY.name()).get(0).get(14)).isEqualTo("ui/links/vln/36079");
    assertThat(items.get(ItemType.SECURITY_VULNERABILITY.name()).get(1).get(12)).isEqualTo(
        "tomcat : tomcat-util : 5.5.23");
    assertThat(items.get(ItemType.SECURITY_VULNERABILITY.name()).get(1).get(14)).isEqualTo("ui/links/vln/62054");
    assertThat(items.get(ItemType.NON_VULNERABLE_COMPONENT.name()).get(0).get(12)).isEqualTo(
        "org.springframework.boot : spring-boot-actuator-autoconfigure : 2.4.3");
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

    // Try all results on one page
    // There are 4 results for CVE-2022-25857 but only 2 of these are sequential and should be grouped
    SearchResultDTO searchResultDTO =
        searchService.searchIndex("vulnerabilitySeverity:[7 TO 8]", Integer.MAX_VALUE, 0, false, null);
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
    searchResultDTO = searchService.searchIndex("vulnerabilitySeverity:[7 TO 8]", 36, 0, false, null);
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

    searchResultDTO = searchService.searchIndex("vulnerabilitySeverity:[7 TO 8]", 36, 2, false, null);
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(39);
    assertThat(searchResultDTO.groupingByDTOS).hasSize(3);

    // Fourth result (page 2)
    assertThat(searchResultDTO.groupingByDTOS.get(0).groupBy).isEqualTo("CVE-2022-25857");
    assertThat(searchResultDTO.groupingByDTOS.get(0).searchResultItemDTOS).hasSize(1);
    assertThat(searchResultDTO.groupingByDTOS.get(0).searchResultItemDTOS.get(0).resultIndex).isEqualTo(37);

    // If we search specifically for CVE-2022-25857, then all results should be grouped (unless split across pages)
    // since no matter the order they will have the same groupBy key
    searchResultDTO = searchService.searchIndex("CVE-2022-25857", Integer.MAX_VALUE, 0, false, null);
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
    searchResultDTO = searchService.searchIndex("CVE-2022-25857", 2, 0, false, null);
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(4);
    assertThat(searchResultDTO.groupingByDTOS).hasSize(1);

    // First and second results (page 1)
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(4);
    assertThat(searchResultDTO.groupingByDTOS).hasSize(1);
    assertThat(searchResultDTO.groupingByDTOS.get(0).groupBy).isEqualTo("CVE-2022-25857");
    assertThat(searchResultDTO.groupingByDTOS.get(0).searchResultItemDTOS).hasSize(2);
    assertThat(searchResultDTO.groupingByDTOS.get(0).searchResultItemDTOS.get(0).resultIndex).isEqualTo(1);
    assertThat(searchResultDTO.groupingByDTOS.get(0).searchResultItemDTOS.get(1).resultIndex).isEqualTo(2);

    searchResultDTO = searchService.searchIndex("CVE-2022-25857", 2, 2, false, null);
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

  private PolicyEvaluation newAppReport(String appId, String stageId, String reportId, String reportResourceName)
      throws Exception
  {
    PolicyEvaluation policyEval = tempEntity.newPolicyEvaluation(appId, stageId, reportId);
    ReportTestUtils.createReportFile(policyEval.getApplicationId(), policyEval.getScanId(),
        ReportTestUtils.zipReportDir(reportResourceName, tempDir), insightWork);
    return policyEval;
  }

  private List<SearchResultItemDTO> find(
      Collection<SearchResultItemDTO> searchResultDTOS,
      ItemType itemType,
      Predicate<SearchResultItemDTO> searchResultDTOPredicate)
  {
    return find(searchResultDTOS, s -> itemType.name().equals(s.itemType) && searchResultDTOPredicate.test(s));
  }

  private List<SearchResultItemDTO> find(
      Collection<SearchResultItemDTO> searchResultDTOS,
      Predicate<SearchResultItemDTO> searchResultDTOPredicate)
  {
    return searchResultDTOS.stream().filter(searchResultDTOPredicate).collect(toList());
  }
}
