/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.query;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.StreamingOutput;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.dataaccess.search.SearchIndexManager;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.SearchIndexChange;
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
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductMode;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.report.ReportTestUtils;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.search.index.IndexService;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.index.VulnerabilityDescriptionFetcher;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryCollector;
import com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryMetrics.SearchCount;
import com.sonatype.insight.error.exception.BadRequestException;
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

import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.PENDING;
import static com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryCollector.TOTAL_SEARCHES;
import static com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryCollector.TOTAL_SEARCHES_BY_FIELD_NAME;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractSearchServiceTest
    extends AbstractComponentTest
{
  @Inject
  protected SearchService searchService;

  @Inject
  protected IndexService indexService;

  @Inject
  protected InsightWork insightWork;

  @Inject
  private AdvancedSearchTelemetryCollector advancedSearchTelemetryCollector;

  @Inject
  protected ApiConfigurationService configurationService;

  @Mock
  private VulnerabilityDescriptionFetcher vulnerabilityDescriptionFetcher;

  @Inject
  private TestProductLicense testProductLicense;

  @Inject
  private ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  @Inject
  private SearchIndexManager searchIndexManager;

  @Inject
  private SearchIndexClient searchIndexClient;

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
        .isThrownBy(() -> searchService.searchIndex("query", 1, 1, false, null, null))
        .withMessage("advanced-search-configuration feature is disabled.");
  }

  private TelemetryData collectSearchTelemetry() {
    return advancedSearchTelemetryCollector.collectAllData().stream()
        .filter(telemetryData -> TelemetryPurpose.ADVANCED_SEARCH.equals(telemetryData.getPurpose())).findAny()
        .orElse(null);
  }

  @Test
  public void testSearchIndex_Telemetry() throws Exception {
    indexService.createSearchIndex();
    searchService.searchIndex("organizationName:org1 itemType:it2", 1, 0, true, null, null);
    searchService.searchIndex("itemType:it1", 1, 0, false, null, null);
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
    searchService.searchIndex("itemType:it1", 10, 1, false, null, null);
    TelemetryData telemetryData = collectSearchTelemetry();

    assertThat(telemetryData).isNull();
  }

  @Test
  public void testSearchIndex_TelemetryInvalidFieldNameCaptured() throws Exception {
    indexService.createSearchIndex();

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> searchService.searchIndex("invalidFieldName:value", 1, 0, false, null, null));

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
    searchService.searchIndex("itemType:it1 itemType:it2", 1, 0, true, null, null);
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
  public void testSearchIndex_RestrictedApplicationNotReturnedItemTypeSearch() {
    Role nonGlobalReadRole = tempEntity.newRole(false, Permission.READ);

    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newApplicationWithParent();

    UserPrincipal userPrincipal = (UserPrincipal) subject.getPrincipal();
    tempEntity.newMembershipMapping(application.getId(), nonGlobalReadRole.getId(), userPrincipal.getUsername());

    indexService.createSearchIndex();
    SearchResultDTO searchResultDTO = searchService.searchIndex("itemType:APPLICATION", 20, 0, false, null, null);

    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(1);

    List<String> applicationIds = searchResultDTO.groupingByDTOS.stream()
        .flatMap(groupingByDTO -> groupingByDTO.searchResultItemDTOS.stream())
        .map(searchResultItemDTO -> searchResultItemDTO.applicationId)
        .collect(toList());
    assertThat(applicationIds).containsOnly(application.getId());
  }

  @Test
  public void testSearchIndex_AllPermittedApplicationsReturned() {
    when(subject.getPrincipal()).thenReturn(new UserPrincipal("uSeRnAmEiıIİ", "Test User", InternalRealm.ID));
    Role nonGlobalReadRole = tempEntity.newRole(false, Permission.READ);

    Application application1 = tempEntity.newApplicationWithParent();
    Application application2 = tempEntity.newApplicationWithParent();
    Application application3 = tempEntity.newApplicationWithParent();
    Application application4 = tempEntity.newApplicationWithParent();

    UserPrincipal userPrincipal = (UserPrincipal) subject.getPrincipal();
    tempEntity.newMembershipMapping(application1.getId(), nonGlobalReadRole.getId(), userPrincipal.getUsername());
    tempEntity.newMembershipMapping(application2.getId(), nonGlobalReadRole.getId(), userPrincipal.getUsername());
    tempEntity.newMembershipMapping(application3.getId(), nonGlobalReadRole.getId(), "USERNAMEIIIİ");
    tempEntity.newMembershipMapping(application4.getId(), nonGlobalReadRole.getId(), "usernameiıii̇");

    indexService.createSearchIndex();
    SearchResultDTO searchResultDTO = searchService.searchIndex("itemType:APPLICATION", 20, 0, false, null, null);

    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(4);

    List<String> applicationIds = searchResultDTO.groupingByDTOS.stream()
        .flatMap(groupingByDTO -> groupingByDTO.searchResultItemDTOS.stream())
        .map(searchResultItemDTO -> searchResultItemDTO.applicationId)
        .collect(toList());
    assertThat(applicationIds).containsOnly(application1.getId(), application2.getId(), application3.getId(),
        application4.getId());
  }

  @Test
  public void testSearchIndex_ApplicationsOfOrganizationReturned() {
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

    SearchResultDTO searchResultDTO = searchService.searchIndex("itemType:ORGANIZATION", 20, 0, false, null, null);
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(1);

    List<String> organizationIds = searchResultDTO.groupingByDTOS.stream()
        .flatMap(groupingByDTO -> groupingByDTO.searchResultItemDTOS.stream())
        .map(searchResultItemDTO -> searchResultItemDTO.organizationId)
        .collect(toList());
    assertThat(organizationIds).containsOnly(organization.getId());

    searchResultDTO = searchService.searchIndex("itemType:APPLICATION", 20, 0, false, null, null);

    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(2);
    List<String> applicationIds = searchResultDTO.groupingByDTOS.stream()
        .flatMap(groupingByDTO -> groupingByDTO.searchResultItemDTOS.stream())
        .map(searchResultItemDTO -> searchResultItemDTO.applicationId)
        .collect(toList());
    assertThat(applicationIds).containsOnly(organizationApplication1.getId(), organizationApplication2.getId());
  }

  @Test
  public void testSearchIndex_ApplicationsReturnedForGlobalContextPermission() {
    testSearchIndex_ShouldReturnAll(tempEntity.newRole(true, Permission.READ), MembershipMapping.GLOBAL_CONTEXT_ID,
        "uSeRnAmEiıIİ");
  }

  @Test
  public void testSearchIndex_MemberNameLowercase_ApplicationsReturnedForGlobalContextPermission() {
    testSearchIndex_ShouldReturnAll(tempEntity.newRole(true, Permission.READ), MembershipMapping.GLOBAL_CONTEXT_ID,
        "USERNAMEIIIİ");
  }

  @Test
  public void testSearchIndex_MemberNameUppercase_ApplicationsReturnedForGlobalContextPermission() {
    testSearchIndex_ShouldReturnAll(tempEntity.newRole(true, Permission.READ), MembershipMapping.GLOBAL_CONTEXT_ID,
        "usernameiıii̇");
  }

  @Test
  public void testSearchIndex_ApplicationsReturnedForRootOrganizationContextPermission() {
    testSearchIndex_ShouldReturnAll(tempEntity.newRole(false, Permission.READ), Organization.ROOT_ORGANIZATION_ID,
        "uSeRnAmEiıIİ");
  }

  @Test
  public void testSearchIndex_MemberNameLowercase_ApplicationsReturnedForRootOrganizationContextPermission() {
    testSearchIndex_ShouldReturnAll(tempEntity.newRole(false, Permission.READ), Organization.ROOT_ORGANIZATION_ID,
        "USERNAMEIIIİ");
  }

  @Test
  public void testSearchIndex_MemberNameUppercase_ApplicationsReturnedForRootOrganizationContextPermission() {
    testSearchIndex_ShouldReturnAll(tempEntity.newRole(false, Permission.READ), Organization.ROOT_ORGANIZATION_ID,
        "usernameiıii̇");
  }

  private void testSearchIndex_ShouldReturnAll(
      Role role,
      String contextId,
      String memberName)
  {
    when(subject.getPrincipal()).thenReturn(new UserPrincipal("uSeRnAmEiıIİ", "Test User", InternalRealm.ID));
    Organization org1 = tempEntity.newOrganization();
    Organization org2 = tempEntity.newOrganization();

    tempEntity.newApplication(org1.getId());
    tempEntity.newApplication(org1.getId());

    tempEntity.newApplication(org2.getId());
    tempEntity.newApplication(org2.getId());

    tempEntity.newMembershipMapping(contextId, role.getId(), memberName);

    indexService.createSearchIndex();

    SearchResultDTO searchResultDTO = searchService.searchIndex("itemType:ORGANIZATION", 20, 0, false, null, null);
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(3); // 2 orgs + the Root org = 3

    searchResultDTO = searchService.searchIndex("itemType:APPLICATION", 20, 0, false, null, null);
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(4);  // 4 applications owned by 2 organizations
  }

  @Test
  public void testSearchIndex_ApplicationsReturnedForOrganizationContextPermission() {
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

    SearchResultDTO searchResultDTO = searchService.searchIndex("itemType:ORGANIZATION", 20, 0, false, null, null);

    List<String> organizationIds = searchResultDTO.groupingByDTOS.stream()
        .flatMap(groupingByDTO -> groupingByDTO.searchResultItemDTOS.stream())
        .map(searchResultItemDTO -> searchResultItemDTO.organizationId)
        .collect(toList());
    assertThat(organizationIds).containsExactly(org1.getId());

    searchResultDTO = searchService.searchIndex("itemType:APPLICATION", 20, 0, false, null, null);

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
        searchService.searchIndex("CVE-2022-25857 AND organizationName:org-01", 10, 0, true, null, null);
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(3);

    searchResultDTO = searchService.searchIndex("organizationName:org-02", 10, 0, true, null, null);
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(0);  // insufficient permissions

    tempEntity.newMembershipMapping(org2.getId(), role.getId(), userPrincipal.getUsername());
    searchResultDTO = searchService.searchIndex("CVE-2022-25857 AND organizationName:org-02", 10, 0, true, null, null);
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(1);  // sufficient permissions

    searchResultDTO = searchService.searchIndex("CVE-2022-25857", 10, 0, true, null, null);
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
        searchService.searchIndex("CVE-2022-25857 AND organizationName:parent-organization", 10, 0, true, null, null);
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(3);

    searchResultDTO =
        searchService.searchIndex("CVE-2022-25857 AND organizationName:child-organization", 10, 0, true, null, null);
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
        searchService.searchIndex("CVE-2022-25857", 10, 0, true, null, null);
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(1);

    tempEntity.newMembershipMapping(org2.getId(), role.getId(), userPrincipal.getUsername());
    searchResultDTO = searchService.searchIndex("CVE-2022-25857", 10, 0, true, null, null);
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(2);  // insufficient permissions

    tempEntity.newMembershipMapping(org1.getId(), role.getId(), userPrincipal.getUsername());
    searchResultDTO = searchService.searchIndex("CVE-2022-25857", 10, 0, true, null, null);
    assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(5);  // sufficient permissions
  }

  @Test
  public void testSearchIndex_PoliciesReturnedForOrganizationContextPermission() {
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

    SearchResultDTO searchResultDTO = searchService.searchIndex("itemType:POLICY", 20, 0, false, null, null);

    List<String> policyIds = searchResultDTO.groupingByDTOS.stream()
        .flatMap(groupingByDTO -> groupingByDTO.searchResultItemDTOS.stream())
        .map(searchResultItemDTO -> searchResultItemDTO.policyId)
        .collect(toList());
    assertThat(policyIds).containsExactlyInAnyOrder(policyOrg1.getId(), policyApp1.getId());
  }

  @Test
  public void testExportSearch_AdvancedSearchConfigurationDisabled() {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.setEnabled(false);

    assertThatExceptionOfType(NotAuthorizedException.class)
        .isThrownBy(() -> searchService.exportSearch("itemType:*", Integer.MAX_VALUE, 1, true, null, null,
            mock(HttpServletResponse.class)))
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

    SearchResultDTO searchResultDTO = searchService.searchIndex("itemType:*", 100, 0, true, null, null);

    List<SearchResultItemDTO> results = searchResultDTO.groupingByDTOS.stream()
        .flatMap(groupingByDTO -> groupingByDTO.searchResultItemDTOS.stream())
        .toList();

    List<String> actualItemTypes = results.stream().map(e -> e.itemType).collect(toList());
    List<String> expectedItemTypes = Arrays.stream(ItemType.values()).map(Enum::name).collect(toList());

    // TODO temporary until SBOM Advanced Search is fully implemented
    expectedItemTypes.remove("SBOM_METADATA");
    assertThat(actualItemTypes).containsExactlyInAnyOrderElementsOf(expectedItemTypes);

    StreamingOutput stream =
        (StreamingOutput) searchService.exportSearch("itemType:*", Integer.MAX_VALUE, 1, true, null, null,
            mock(HttpServletResponse.class)).getEntity();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    stream.write(baos);
    List<String> export = Arrays.stream(baos.toString().split("\n")).collect(Collectors.toList());

    assertThat(export).hasSize(8);
    assertThat(export.size() - 1).isEqualTo(results.size());
    assertThat(export.get(0).split(",")).hasSize(16);

    Map<String, List<List<String>>> items = export.stream().skip(1)
        .map(s -> Arrays.stream(s.split(",")).collect(toList()))
        .collect(groupingBy(l -> l.get(0)));
    items.values().forEach(list ->
        list.sort(Comparator.comparing(Object::toString))
    );

    assertThat(items.get(ItemType.ORGANIZATION.name()).get(0).get(1)).isEqualTo(org1.getName());
    assertThat(items.get(ItemType.APPLICATION.name()).get(0).get(3)).isEqualTo(app1.getName());
    assertThat(items.get(ItemType.APPLICATION_CATEGORY.name()).get(0).get(5)).isEqualTo(tag.getName());
    assertThat(items.get(ItemType.COMPONENT_LABEL.name()).get(0).get(7)).isEqualTo(label.getLabel());
    assertThat(items.get(ItemType.POLICY.name()).get(0).get(9)).isEqualTo(policyOrg1.getName());

    List<SearchResultItemDTO> components = results.stream()
        .filter(sri -> sri.itemType.equals(ItemType.SECURITY_VULNERABILITY.name()) ||
            sri.itemType.equals(ItemType.NON_VULNERABLE_COMPONENT.name()))
        .toList();
    components = new ArrayList<>(components);
    components.sort(Comparator.comparing(c -> c.componentName));

    assertThat(items.get(ItemType.SECURITY_VULNERABILITY.name()).get(0).get(12)).isEqualTo(
        components.get(0).componentName);
    assertThat(items.get(ItemType.SECURITY_VULNERABILITY.name()).get(0).get(1)).isEqualTo(org1.getName());
    assertThat(items.get(ItemType.SECURITY_VULNERABILITY.name()).get(0).get(2))
        .isEqualTo("ui/links/organization/" + org1.getId() + "/management");
    assertThat(items.get(ItemType.NON_VULNERABLE_COMPONENT.name()).get(0).get(12)).isEqualTo(
        components.get(1).componentName);

    try {
      configurationService.setConfigurationInDatabaseNoAuthz(
          SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER, ";");
      configurationService.applyConfigurationToClients(
          SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER);
      stream = (StreamingOutput) searchService.exportSearch("itemType:*", Integer.MAX_VALUE, 1, true, null, null,
          mock(HttpServletResponse.class)).getEntity();
      baos = new ByteArrayOutputStream();
      stream.write(baos);
      export = Arrays.stream(baos.toString().split("\n")).collect(Collectors.toList());
      assertThat(export.get(0).split(";")).hasSize(16);
    }
    finally {
      configurationService.deleteConfigurationInDatabaseNoAuthz(
          SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER);
      configurationService.applyConfigurationToClients(
          SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER);
    }
  }

  @Test
  public void testSearchIndex_SbomManagerMode_MissingLicensedFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.SBOM_MANAGER);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> searchService.searchIndex("itemType:*", 100, 0, true, ProductMode.SBOM_MANAGER, null))
        .withMessageContaining("The SBOM Manager feature is not supported by your license.");
  }

  @Test
  public void testSearchIndex_SbomManagerFeature_DefaultModeNotSupported() {
    testProductLicense.setProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> searchService.searchIndex("itemType:*", 100, 0, true, null, null))
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
        true,
        PENDING);
    tempEntity.newSbomEvaluation(app,
        "1.1",
        SbomSpecification.SPDX.toString(),
        PackageUrlIdentifier.fromComponentIdentifier(ComponentIdentifier.createAnameCoordinates("n", null, "v2")),
        "someScanId2",
        false,
        PENDING);
    tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID); // Should not be returned
    tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID); // Should not be returned
    tempEntity.newPolicy(); // Should not be returned
    newAppReport(app.getId(), Stage.ID_BUILD, "someScanId3", "/SearchServiceTest/report-3");
    indexService.createSearchIndex();
    assertThat(searchService.searchIndex("applicationCategoryId:*", 100, 0, true,
        ProductMode.SBOM_MANAGER, null).groupingByDTOS).isEmpty();
    assertThat(searchService.searchIndex("componentLabelId:*", 100, 0, true,
        ProductMode.SBOM_MANAGER, null).groupingByDTOS).isEmpty();
    assertThat(searchService.searchIndex("policyId:*", 100, 0, true,
        ProductMode.SBOM_MANAGER, null).groupingByDTOS).isEmpty();

    SearchResultDTO searchResultDTO =
        searchService.searchIndex("itemType:*", 100, 0, true, ProductMode.SBOM_MANAGER, null);

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
        true,
        PENDING);
    tempEntity.newSbomEvaluation(app,
        "1.1",
        SbomSpecification.SPDX.toString(),
        PackageUrlIdentifier.fromComponentIdentifier(ComponentIdentifier.createAnameCoordinates("n", null, "v2")),
        "someScanId2",
        false,
        PENDING);
    newAppReport(app.getId(), Stage.ID_BUILD, "someScanId3", "/SearchServiceTest/report-3");
    Tag tag = tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID);
    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);
    Policy policy = tempEntity.newPolicy();
    indexService.createSearchIndex();
    assertThat(searchService.searchIndex("applicationVersion:*", 100, 0, true, null, null).groupingByDTOS).isEmpty();

    SearchResultDTO searchResultDTO = searchService.searchIndex("itemType:*", 100, 0, true, null, null);

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
        true,
        PENDING);
    tempEntity.newSbomEvaluation(app,
        "1.1",
        SbomSpecification.SPDX.toString(),
        PackageUrlIdentifier.fromComponentIdentifier(ComponentIdentifier.createAnameCoordinates("n", null, "v2")),
        "someScanId2",
        false,
        PENDING);
    tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID); // Should not be returned
    tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID); // Should not be returned
    tempEntity.newPolicy(); // Should not be returned
    newAppReport(app.getId(), Stage.ID_BUILD, "someScanId3", "/SearchServiceTest/report-3");
    indexService.createSearchIndex();
    SearchResultDTO searchResultDTO =
        searchService.searchIndex("itemType:*", 100, 0, true, ProductMode.SBOM_MANAGER, null);
    List<SearchResultItemDTO> results = searchResultDTO.groupingByDTOS.stream()
        .flatMap(groupingByDTO -> groupingByDTO.searchResultItemDTOS.stream())
        .toList();

    StreamingOutput stream =
        (StreamingOutput) searchService.exportSearch("itemType:*", Integer.MAX_VALUE, 1, true, ProductMode.SBOM_MANAGER,
            null, mock(HttpServletResponse.class)).getEntity();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    stream.write(baos);
    List<String> rows = Arrays.asList(baos.toString().split("\n"));
    Map<String, List<List<String>>> items = rows.stream()
        .skip(1)
        .map(s -> Arrays.stream(s.split(",")).collect(toList()))
        .collect(groupingBy(l -> l.get(0)));
    items.values().forEach(list ->
        list.sort(Comparator.comparing(Object::toString))
    );
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
        true,
        PENDING);
    tempEntity.newSbomEvaluation(app,
        "1.1",
        SbomSpecification.SPDX.toString(),
        PackageUrlIdentifier.fromComponentIdentifier(ComponentIdentifier.createAnameCoordinates("n", null, "v2")),
        "someScanId2",
        false,
        PENDING);
    Tag tag = tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID);
    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);
    Policy policy = tempEntity.newPolicy();
    newAppReport(app.getId(), Stage.ID_BUILD, "someScanId3", "/SearchServiceTest/report-3");
    indexService.createSearchIndex();
    SearchResultDTO searchResultDTO = searchService.searchIndex("itemType:*", 100, 0, true, null, null);
    List<SearchResultItemDTO> results = searchResultDTO.groupingByDTOS.stream()
        .flatMap(groupingByDTO -> groupingByDTO.searchResultItemDTOS.stream())
        .toList();

    StreamingOutput stream =
        (StreamingOutput) searchService.exportSearch("itemType:*", Integer.MAX_VALUE, 1, true, null, null,
            mock(HttpServletResponse.class)).getEntity();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    stream.write(baos);
    List<String> rows = Arrays.asList(baos.toString().split("\n"));
    Map<String, List<List<String>>> items = rows.stream()
        .skip(1)
        .map(s -> Arrays.stream(s.split(",")).collect(toList()))
        .collect(groupingBy(l -> l.get(0)));
    items.values().forEach(list ->
        list.sort(Comparator.comparing(Object::toString))
    );
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
  public void testSearchIndex_TooManyBooleanClauses() {
    try {
      UserPrincipal userPrincipal = (UserPrincipal) subject.getPrincipal();
      Role role = tempEntity.newRole(false, Permission.READ);
      Organization org1 = tempEntity.newOrganization();
      Organization org2 = tempEntity.newOrganization();
      Organization org3 = tempEntity.newOrganization();
      tempEntity.newMembershipMapping(org1.getId(), role.getId(), userPrincipal.getUsername());
      tempEntity.newMembershipMapping(org2.getId(), role.getId(), userPrincipal.getUsername());
      tempEntity.newMembershipMapping(org3.getId(), role.getId(), userPrincipal.getUsername());
      indexService.createSearchIndex();

      // Lower the max clause count, we should get an error
      configurationService.setConfigurationInDatabaseNoAuthz(
          Map.of(SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT, 2));
      configurationService.applyConfigurationToClients(SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT);
      assertThatExceptionOfType(BadRequestException.class)
          .isThrownBy(() -> searchService.searchIndex("itemType:ORGANIZATION", 10, 0, false, null, null))
          .withMessageContaining("Error performing search due to too many clauses");

      // Raise the max clause count back up, we should not get an error
      configurationService.setConfigurationInDatabaseNoAuthz(
          Map.of(SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT, 10));
      configurationService.applyConfigurationToClients(SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT);

      SearchResultDTO searchResultDTO = searchService.searchIndex("itemType:ORGANIZATION", 10, 0, false, null, null);

      assertThat(searchResultDTO.totalNumberOfHits).isEqualTo(3);
      assertThat(searchResultDTO.groupingByDTOS).hasSize(3);
      assertThat(searchResultDTO.groupingByDTOS)
          .extracting(g -> g.searchResultItemDTOS.get(0).organizationId)
          .containsExactlyInAnyOrder(org1.getId(), org2.getId(), org3.getId());
    }
    finally {
      configurationService.deleteConfigurationInDatabaseNoAuthz(
          Set.of(SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT));
      configurationService.applyConfigurationToClients(SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT);
    }
  }

  @Test
  public void testSearchIndex_ApplicationVersionBadCharacters() {
    // Setup the user
    UserPrincipal userPrincipal = (UserPrincipal) subject.getPrincipal();
    Role role = tempEntity.newRole(false, Permission.READ);
    tempEntity.newMembershipMapping(Organization.ROOT_ORGANIZATION_ID, role.getId(), userPrincipal.getUsername());
    Application application = tempEntity.newApplicationWithParent();
    indexService.createSearchIndex();

    // Create the SBOM
    ThirdPartyFile file = tempEntity.newThirdPartyFile("CycloneDX-bom.xml");
    String applicationVersion = "}-13.7.59d072-20250926-8";
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(file.getId(), application.getId(), ACTIVE, file.getFilename());
    sbomMetadata.setSbomVersion(applicationVersion);
    thirdPartySbomMetadataDAO.update(sbomMetadata);

    // Manually insert the search index change for the SBOM with characters that need escaping
    SearchIndexChange searchIndexChange = thirdPartySbomMetadataDAO.newSearchIndexChange(sbomMetadata);
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.setEnabled(true);
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_ENABLED.setEnabled(true);
    searchIndexManager.insert(searchIndexChange);

    // Check it shouldn't be indexed until the update is processed
    SearchResultDTO searchResultDTO =
        searchService.searchIndex("itemType:SBOM_METADATA", 100, 0, true, ProductMode.SBOM_MANAGER, null);
    assertThat(searchResultDTO).isNotNull();
    assertThat(searchResultDTO.groupingByDTOS).isEmpty();

    searchIndexClient.updateIndex();

    // Check it exists after the update
    searchResultDTO =
        searchService.searchIndex("itemType:SBOM_METADATA", 100, 0, true, ProductMode.SBOM_MANAGER, null);
    assertThat(searchResultDTO).isNotNull();
    assertThat(searchResultDTO.groupingByDTOS).hasSize(1);
    assertThat(searchResultDTO.groupingByDTOS.get(0)).isNotNull();
    assertThat(searchResultDTO.groupingByDTOS.get(0).searchResultItemDTOS).hasSize(1);
    assertThat(searchResultDTO.groupingByDTOS.get(0).searchResultItemDTOS.get(0)).isNotNull();
    assertThat(searchResultDTO.groupingByDTOS.get(0).searchResultItemDTOS.get(0).applicationId).isEqualTo(
        application.getId());
    assertThat(searchResultDTO.groupingByDTOS.get(0).searchResultItemDTOS.get(0).applicationVersion).isEqualTo(
        applicationVersion);
  }

  protected PolicyEvaluation newAppReport(String appId, String stageId, String reportId, String reportResourceName)
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
