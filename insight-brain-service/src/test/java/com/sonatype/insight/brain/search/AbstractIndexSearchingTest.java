/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.PENDING;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanResultDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanTicketDTO;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.api.v2.service.ApiSbomService;
import com.sonatype.insight.brain.api.v2.service.ApiThirdPartyScanService;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.SearchIndexChangeDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.SearchIndexChange.ChangeType;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.product.license.ProductMode;
import com.sonatype.insight.brain.report.ReportTestUtils;
import com.sonatype.insight.brain.search.index.AbstractSearchIndexClient;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.IndexService;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.index.VulnerabilityDescriptionFetcher;
import com.sonatype.insight.brain.search.lucene.DocumentBuilderHelper;
import com.sonatype.insight.brain.search.query.SearchService;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.HdsMockServerRule;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import jakarta.inject.Inject;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

/**
 * End-to-end tests of indexing and searching to check queries return the desired results.
 */
public abstract class AbstractIndexSearchingTest
    extends AbstractComponentTest
{
  @Inject
  private IndexService indexService;

  @Inject
  private SearchService searchService;

  @Inject
  private SearchIndexClient searchIndexClient;

  @Inject
  private ApiConfigurationService configurationService;

  @Inject
  private ApiSbomService apiSbomService;

  @Inject
  private ApiThirdPartyScanService apiThirdPartyScanService;

  @Inject
  private ApplicationDAO applicationDAO;

  private OrganizationDAO spyOrganizationDAO;

  private PolicyEvaluationDAO spyPolicyEvaluationDAO;

  @Inject
  private LabelDAO labelDAO;

  @Inject
  private PolicyDAO policyDAO;

  @Inject
  private TagDAO tagDAO;

  @Inject
  private SearchIndexChangeDAO searchIndexChangeDAO;

  @Inject
  private PolicyViolationDAO policyViolationDAO;

  @Inject
  private InsightWork insightWork;

  @Mock
  private VulnerabilityDescriptionFetcher vulnerabilityDescriptionFetcher;

  @Mock
  private TelemetrySender telemetrySenderMock;

  @Inject
  private DocumentBuilderHelper documentBuilderHelper;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  @Rule
  public HdsMockServerRule hdsMockServer = new HdsMockServerRule();

  @Before
  public void setUpSearchOverrides() {
    spyOrganizationDAO = spy(daoFactory.createOrganizationDAO());
    applyBeanFieldOverride(OwnerDAO.class, "orgDAO", spyOrganizationDAO);
    spyPolicyEvaluationDAO = spy(daoFactory.createPolicyEvaluationDAO());
    applyBeanFieldOverride(DocumentBuilderHelper.class, "policyEvaluationDAO", spyPolicyEvaluationDAO);
    lenient().when(vulnerabilityDescriptionFetcher.getVulnerabilityDescription(anyString())).thenReturn("");
  }

  @Override
  protected void setUpTestLicenseThreatGroups() {
    // Skip default Root Org LTGs — policy violation tests create their own isolated LTGs
  }

  @Before
  public void before() {
    UserPrincipal userPrincipal = (UserPrincipal) subject.getPrincipal();
    Role role = tempEntity.newRole(true, Permission.READ);
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), userPrincipal.getUsername());
    systemConfigurationPropertyDAO
        .update(new SystemConfigurationProperty(SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED, "true"));

    setHdsUrl();
  }

  private void index() {
    indexService.createSearchIndex();
  }

  private void indexChanges() {
    searchIndexClient.updateIndex();
  }

  private void setHdsUrl() {
    configurationService.setConfigurationInDatabaseNoAuthz(SystemConfigurationProperty.HDS_URL,
        hdsMockServer.getHttpUrl());
    configurationService.applyConfigurationToClients(SystemConfigurationProperty.HDS_URL);
  }

  private List<SearchResultItemDTO> search(String query, boolean allComponents) throws Exception {
    return searchService.searchIndex(query, Integer.MAX_VALUE, 1, allComponents, null, null).groupingByDTOS.stream()
        .map(groupDTO -> groupDTO.searchResultItemDTOS)
        .flatMap(List::stream)
        .collect(toList());
  }

  private List<SearchResultItemDTO> search(String query) throws Exception {
    return search(query, false);
  }

  private List<SearchResultItemDTO> search(FieldIdentifier fieldIdentifier, String fieldValue) throws Exception {
    return search(fieldIdentifier + ":" + fieldValue, false);
  }

  private List<SearchResultItemDTO> searchInAllComponents(
      FieldIdentifier fieldIdentifier,
      String fieldValue) throws Exception
  {
    return search(fieldIdentifier + ":" + fieldValue, true);
  }

  private List<SearchResultItemDTO> sbomManagerSearch(String query, boolean allComponents) throws Exception {
    return searchService.searchIndex(query, Integer.MAX_VALUE, 1, allComponents, ProductMode.SBOM_MANAGER,
        null).groupingByDTOS.stream()
            .map(groupDTO -> groupDTO.searchResultItemDTOS)
            .flatMap(List::stream)
            .collect(toList());
  }

  private List<SearchResultItemDTO> sbomManagerSearch(
      FieldIdentifier fieldIdentifier,
      String fieldValue) throws Exception
  {
    return sbomManagerSearch(fieldIdentifier + ":" + fieldValue, false);
  }

  private List<SearchResultItemDTO> sbomManagerSearchInAllComponents(
      FieldIdentifier fieldIdentifier,
      String fieldValue) throws Exception
  {
    return sbomManagerSearch(fieldIdentifier + ":" + fieldValue, true);
  }

  private PolicyEvaluation newAppReport(String stageId, String reportId) throws Exception {
    return newAppReport(tempEntity.newApplicationWithParent().getId(), stageId, reportId);
  }

  private PolicyEvaluation newAppReport(String appId, String stageId, String reportId) throws Exception {
    PolicyEvaluation policyEval = tempEntity.newPolicyEvaluation(appId, stageId, reportId);
    ReportTestUtils.createReportFile(policyEval.getOwnerId(), policyEval.getScanId(),
        ReportTestUtils.zipReportDir("/IndexSearchingTest/report", tempDir), insightWork);
    return policyEval;
  }

  private PolicyEvaluation newAppReport(
      String appId,
      String stageId,
      String reportId,
      String reportResourceName) throws Exception
  {
    PolicyEvaluation policyEval = tempEntity.newPolicyEvaluation(appId, stageId, reportId);
    ReportTestUtils.createReportFile(policyEval.getOwnerId(), policyEval.getScanId(),
        ReportTestUtils.zipReportDir(reportResourceName, tempDir), insightWork);
    return policyEval;
  }

  private void assertOrganization(SearchResultItemDTO result, Organization organization) {
    assertThat(result.itemType).isEqualTo(ItemType.ORGANIZATION.name());
    assertOrganizationData(result, organization);
  }

  private void assertOrganizationData(SearchResultItemDTO result, Organization organization) {
    assertThat(result.organizationId).isEqualTo(organization.getId());
    assertThat(result.organizationName).isEqualTo(organization.getName());
  }

  private void assertApplication(SearchResultItemDTO result, Application application, Organization organization) {
    assertThat(result.itemType).isEqualTo(ItemType.APPLICATION.name());
    assertApplicationData(result, application);
    assertOrganizationData(result, organization);
  }

  private void assertApplicationData(SearchResultItemDTO result, Application application) {
    assertThat(result.applicationId).isEqualTo(application.getId());
    assertThat(result.applicationPublicId).isEqualTo(application.getPublicId());
    assertThat(result.applicationName).isEqualTo(application.getName());
  }

  private void assertSbom(
      SearchResultItemDTO result,
      Application application,
      Organization organization,
      String version,
      String sbomSpecification)
  {
    assertThat(result.itemType).isEqualTo(ItemType.SBOM_METADATA.name());
    assertSbomDataData(result, version, sbomSpecification);
    assertApplicationData(result, application);
    assertOrganizationData(result, organization);
  }

  private void assertSbomDataData(SearchResultItemDTO result, String version, String sbomSpecification) {
    assertThat(result.applicationVersion).isEqualTo(version);
    assertThat(result.sbomSpecification).isEqualTo(sbomSpecification);
  }

  private void assertApplicationCategory(SearchResultItemDTO result, Tag tag, Organization organization) {
    assertThat(result.itemType).isEqualTo(ItemType.APPLICATION_CATEGORY.name());
    assertApplicationCategoryData(result, tag);
    assertOrganizationData(result, organization);
  }

  private void assertApplicationCategoryData(SearchResultItemDTO result, Tag tag) {
    assertThat(result.applicationCategoryId).isEqualTo(tag.getId());
    assertThat(result.applicationCategoryName).isEqualTo(tag.getName());
    assertThat(result.applicationCategoryColor).isEqualTo(tag.getColor().toValue());
    assertThat(result.applicationCategoryDescription).isEqualTo(tag.getDescription());
  }

  private void assertComponentLabel(SearchResultItemDTO result, Label label, Owner owner) {
    assertThat(result.itemType).isEqualTo(ItemType.COMPONENT_LABEL.name());
    assertComponentLabelData(result, label);
    assertOwnerData(result, owner);
  }

  private void assertOwnerData(SearchResultItemDTO result, Owner owner) {
    if (owner instanceof Organization) {
      assertOrganizationData(result, (Organization) owner);
    }
    else if (owner instanceof Application) {
      assertApplicationData(result, (Application) owner);
    }
    else {
      throw new IllegalArgumentException("unsupported owner type " + owner);
    }
  }

  private void assertComponentLabelData(SearchResultItemDTO result, Label label) {
    assertThat(result.componentLabelId).isEqualTo(label.getId());
    assertThat(result.componentLabelName).isEqualTo(label.getLabel());
    assertThat(result.componentLabelColor).isEqualTo(label.getColor().toValue());
    assertThat(result.componentLabelDescription).isEqualTo(Optional.ofNullable(label.getDescription()).orElse(""));
  }

  private void assertPolicy(SearchResultItemDTO result, Policy policy, Owner owner) {
    assertThat(result.itemType).isEqualTo(ItemType.POLICY.name());
    assertPolicyData(result, policy);
    assertOwnerData(result, owner);
  }

  private void assertPolicyData(SearchResultItemDTO result, Policy policy) {
    assertThat(result.policyId).isEqualTo(policy.getId());
    assertThat(result.policyName).isEqualTo(policy.getName());
    assertThat(result.policyThreatLevel).isEqualTo(policy.getThreatLevel());
    assertThat(result.policyThreatCategory).isEqualTo(policy.getThreatCategory().getName());
  }

  private void assertVulnerability(
      SearchResultItemDTO result,
      SecurityVulnerability vulnerability,
      String vulnerabilityDescription,
      String componentHash,
      ComponentIdentifier componentIdentifier,
      PolicyEvaluation evaluation)
  {
    assertThat(result.itemType).isEqualTo(ItemType.SECURITY_VULNERABILITY.name());
    assertThat(result.vulnerabilityId).isEqualTo(vulnerability.getRefId());
    assertThat(result.vulnerabilityStatus).isEqualTo(vulnerability.getStatus().getName());
    assertThat(result.vulnerabilityDescription).isEqualTo(vulnerabilityDescription);
    assertThat(result.reportId).isEqualTo(evaluation.getScanId());
    assertThat(result.componentHash).isEqualTo(componentHash);
    assertThat(result.componentIdentifier.toComponentIdentifier()).isEqualTo(componentIdentifier);
    assertThat(result.componentName).isEqualTo(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString());
    assertThat(result.policyEvaluationStage).isEqualTo(StageTypes.getById(evaluation.getStageTypeId()).getName());
    assertApplicationData(result, applicationDAO.getById(evaluation.getOwnerId()));
  }

  private void assertSbomVulnerability(
      SearchResultItemDTO result,
      SecurityVulnerability vulnerability,
      String vulnerabilityDescription,
      String componentHash,
      ComponentIdentifier componentIdentifier,
      Application application,
      Organization org,
      String version,
      String sbomSpecification)
  {
    assertThat(result.itemType).isEqualTo(ItemType.SECURITY_VULNERABILITY.name());
    assertThat(result.vulnerabilityId).isEqualTo(vulnerability.getRefId());
    assertThat(result.vulnerabilityStatus).isNull();
    assertThat(result.vulnerabilityDescription).isEqualTo(vulnerabilityDescription);
    assertThat(result.reportId).isNull();
    assertThat(result.componentHash).isEqualTo(componentHash);
    assertThat(result.componentIdentifier.toComponentIdentifier()).isEqualTo(componentIdentifier);
    assertThat(result.componentName).isEqualTo(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString());
    assertThat(result.policyEvaluationStage).isNull();
    assertSbomDataData(result, version, sbomSpecification);
    assertApplicationData(result, application);
    assertOrganizationData(result, org);
  }

  private void assertSbomNonVulnerableComponent(
      SearchResultItemDTO result,
      String componentHash,
      ComponentIdentifier componentIdentifier,
      Application application,
      Organization org,
      String version,
      String sbomSpecification)
  {
    assertThat(result.itemType).isEqualTo(ItemType.NON_VULNERABLE_COMPONENT.name());
    assertThat(result.vulnerabilityId).isNull();
    assertThat(result.vulnerabilityStatus).isNull();
    assertThat(result.vulnerabilityDescription).isNull();
    assertThat(result.reportId).isNull();
    assertThat(result.componentHash).isEqualTo(componentHash);
    assertThat(result.componentIdentifier.toComponentIdentifier()).isEqualTo(componentIdentifier);
    assertThat(result.componentName).isEqualTo(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString());
    assertThat(result.policyEvaluationStage).isNull();
    assertSbomDataData(result, version, sbomSpecification);
    assertApplicationData(result, application);
    assertOrganizationData(result, org);
  }

  @Test
  public void testResultFields_Organization() throws Exception {
    Organization org = tempEntity.newOrganization();
    index();
    List<SearchResultItemDTO> results = search(FieldIdentifier.ORGANIZATION_ID, org.getId());
    assertThat(results).hasSize(1);
    assertOrganization(results.get(0), org);
  }

  @Test
  public void testResultFields_Application() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    index();
    List<SearchResultItemDTO> results = search(FieldIdentifier.APPLICATION_ID, app.getId());
    assertThat(results).hasSize(1);
    assertApplication(results.get(0), app, org);
  }

  @Test
  public void testResultFields_SbomMetadata() throws Exception {
    String appVersion = "1.2.3";
    String sbomSpec = "spdx";
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    tempEntity.newSbomEvaluation(app, appVersion, sbomSpec,
        new PackageUrlIdentifier("pkg:maven/com.h2database/h2@1.4.200?type=jar"),
        "12345deadbeef", false, PENDING);
    index();
    List<SearchResultItemDTO> results = sbomManagerSearch(FieldIdentifier.APPLICATION_VERSION, appVersion);
    assertThat(results).hasSize(1);
    assertSbom(results.get(0), app, org, appVersion, sbomSpec);
  }

  @Test
  public void testResultFields_ApplicationCategory() throws Exception {
    Organization org = tempEntity.newOrganization();
    Tag tag = tempEntity.newTag(org.getId());
    index();
    List<SearchResultItemDTO> results = search(FieldIdentifier.APPLICATION_CATEGORY_ID, tag.getId());
    assertThat(results).hasSize(1);
    assertApplicationCategory(results.get(0), tag, org);
  }

  @Test
  public void testResultFields_ComponentLabel_OrgLevel() throws Exception {
    Organization org = tempEntity.newOrganization();
    Label label = tempEntity.newLabel(org.getId());
    index();
    List<SearchResultItemDTO> results = search(FieldIdentifier.COMPONENT_LABEL_ID, label.getId());
    assertThat(results).hasSize(1);
    assertComponentLabel(results.get(0), label, org);
  }

  @Test
  public void testResultFields_ComponentLabel_AppLevel() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Label label = tempEntity.newLabel(app.getId());
    index();
    List<SearchResultItemDTO> results = search(FieldIdentifier.COMPONENT_LABEL_ID, label.getId());
    assertThat(results).hasSize(1);
    assertComponentLabel(results.get(0), label, app);
  }

  @Test
  public void testResultFields_Policy_OrgLevel() throws Exception {
    Organization org = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(org);
    index();
    List<SearchResultItemDTO> results = search(FieldIdentifier.POLICY_ID, policy.getId());
    assertThat(results).hasSize(1);
    assertPolicy(results.get(0), policy, org);
  }

  @Test
  public void testResultFields_Policy_AppLevel() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app);
    index();
    List<SearchResultItemDTO> results = search(FieldIdentifier.POLICY_ID, policy.getId());
    assertThat(results).hasSize(1);
    assertPolicy(results.get(0), policy, app);
  }

  @Test
  public void testResultFields_Vulnerability() throws Exception {
    String vulnDescription = "Remote Code Execution, you may panic now";
    when(vulnerabilityDescriptionFetcher.getVulnerabilityDescription(anyString())).thenReturn(vulnDescription);
    PolicyEvaluation evaluation = newAppReport(Stage.ID_OPERATE, "report1234567890abcdef");
    index();
    List<SearchResultItemDTO> results = search(FieldIdentifier.VULNERABILITY_ID, "CVE-8765-1234");
    assertThat(results).hasSize(1);
    assertVulnerability(results.get(0),
        new SecurityVulnerability("cve", "CVE-8765-1234", 4.3f, SecurityVulnerabilityOverrideStatus.ACKNOWLEDGED),
        vulnDescription, "1234567890abcdeABCDE", ComponentIdentifier.createNugetCoordinates("Search.Test", "1.2.3"),
        evaluation);
  }

  @Test
  public void testResultFields_Sbom_Vulnerability() throws Exception {
    String appVersion = "1.2.3";
    String sbomSpecification = "spdx";

    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    tempEntity.newSbomEvaluation(app, appVersion, sbomSpecification,
        new PackageUrlIdentifier("pkg:maven/com.h2database/h2@1.4.200?type=jar"), "12345", "deadbeef", true,
        PENDING);

    index();
    List<SearchResultItemDTO> results = sbomManagerSearch(FieldIdentifier.VULNERABILITY_ID, "someRefId");
    assertThat(results).hasSize(1);
    assertSbomVulnerability(results.get(0),
        new SecurityVulnerability("someVulSource", "someRefId", 5.5f, SecurityVulnerabilityOverrideStatus.OPEN),
        "someDescription", "12345",
        ComponentIdentifier.createMavenCoordinates("com.h2database", "h2", "1.4.200", null, "jar"),
        app, org, appVersion, sbomSpecification);
  }

  @Test
  public void testResultFields_Sbom_NonVulnerableComponent() throws Exception {
    String appVersion = "1.2.3";
    String sbomSpecification = "spdx";

    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    tempEntity.newSbomEvaluation(app, appVersion, sbomSpecification,
        new PackageUrlIdentifier("pkg:maven/com.h2database/h2@1.4.200?type=jar"), "12345", "deadbeef", false,
        PENDING);

    index();
    List<SearchResultItemDTO> results = sbomManagerSearchInAllComponents(FieldIdentifier.COMPONENT_HASH, "12345");
    assertThat(results).hasSize(1);
    assertSbomNonVulnerableComponent(results.get(0), "12345",
        ComponentIdentifier.createMavenCoordinates("com.h2database", "h2", "1.4.200", null, "jar"),
        app, org, appVersion, sbomSpecification);
  }

  @Test
  public void testSearchByField_UnknownField() {
    index();
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> search("unknownField:foobar"))
        .withMessage("The search query contains invalid field names: [unknownField]");
  }

  @Test
  public void testSearchByField_DefaultField() throws Exception {
    String vulnId = "CVE-8765-1234";
    newAppReport(Stage.ID_OPERATE, "report1234567890abcdef");
    index();
    assertThat(search("CvE-8765-1234")).extracting(dto -> dto.vulnerabilityId).containsExactlyInAnyOrder(vulnId);
  }

  @Test
  public void testSearchByField_ItemType() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplication(org.getId());
    Application app2 = tempEntity.newApplication(org.getId());
    tempEntity.newTag(org.getId());
    index();
    assertThat(search(FieldIdentifier.ITEM_TYPE, "APPLication")).extracting(dto -> dto.applicationId)
        .containsExactlyInAnyOrder(app1.getId(), app2.getId());
  }

  @Test
  public void testSearchByField_OrganizationId() throws Exception {
    // NOTE: the explicit entity id uses both upper and lower case characters to verify normalization
    Organization org = tempEntity.newOrganizationWithSpecificId("2FAB4462f587401299ac3728ee21addc", "Search Test");
    tempEntity.newOrganization();
    index();
    assertThat(search(FieldIdentifier.ORGANIZATION_ID, org.getId().toLowerCase(Locale.ROOT)))
        .extracting(dto -> dto.organizationId)
        .containsExactlyInAnyOrder(org.getId());
    assertThat(search(FieldIdentifier.ORGANIZATION_ID, org.getId().toUpperCase(Locale.ROOT)))
        .extracting(dto -> dto.organizationId)
        .containsExactlyInAnyOrder(org.getId());
  }

  @Test
  public void testSearchByField_OrganizationName() throws Exception {
    Organization org = tempEntity.newOrganization("Search Test");
    tempEntity.newOrganization("Search Test 2");
    index();
    assertThat(search(FieldIdentifier.ORGANIZATION_NAME, "\"search TEST\"")).extracting(dto -> dto.organizationId)
        .containsExactlyInAnyOrder(org.getId());
    assertThat(search(FieldIdentifier.ORGANIZATION_NAME, "seaRCH")).isEmpty();
  }

  @Test
  public void testSearchByField_ApplicationId() throws Exception {
    // NOTE: the explicit entity id uses both upper and lower case characters to verify normalization
    Application app = tempEntity.newApplicationWithSpecificId("2FAB4462f587401299ac3728ee21addc", "Search Test",
        "search-test", tempEntity.newApplicationWithParent().getOrganizationId());
    index();
    assertThat(search(FieldIdentifier.APPLICATION_ID, app.getId().toLowerCase(Locale.ROOT)))
        .extracting(dto -> dto.applicationId)
        .containsExactlyInAnyOrder(app.getId());
    assertThat(search(FieldIdentifier.APPLICATION_ID, app.getId().toUpperCase(Locale.ROOT)))
        .extracting(dto -> dto.applicationId)
        .containsExactlyInAnyOrder(app.getId());
  }

  @Test
  public void testSearchByField_ApplicationName() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test-id-1", "Search Test");
    tempEntity.newApplicationWithParent("test-id-2", "Search Test 2");
    index();
    assertThat(search(FieldIdentifier.APPLICATION_NAME, "\"search TEST\"")).extracting(dto -> dto.applicationId)
        .containsExactlyInAnyOrder(app.getId());
    assertThat(search(FieldIdentifier.APPLICATION_NAME, "seaRCH")).isEmpty();
  }

  @Test
  public void testSearchByField_ApplicationPublicId() throws Exception {
    Application app = tempEntity.newApplicationWithParent("a_SEARCH-test", "App Name 1");
    tempEntity.newApplicationWithParent("a_search-test2", "App Name 2");
    index();
    assertThat(search(FieldIdentifier.APPLICATION_PUBLIC_ID, "A_search-TEST")).extracting(dto -> dto.applicationId)
        .containsExactlyInAnyOrder(app.getId());
    assertThat(search(FieldIdentifier.APPLICATION_PUBLIC_ID, "seaRCH")).isEmpty();
  }

  @Test
  public void testSearchByField_ApplicationVersion() throws Exception {
    Application app = tempEntity.newApplicationWithParent("a_SEARCH-test", "App Name 1");
    tempEntity.newApplicationWithParent("a_search-test2", "App Name 2");
    tempEntity.newSbomEvaluation(app, "1.2.3", "spdx",
        new PackageUrlIdentifier("pkg:maven/com.h2database/h2@1.4.200?type=jar"), "12345deadbeef", true,
        PENDING);
    index();

    // not SBOM Mgr mode; this should not be returned
    assertThat(search(FieldIdentifier.APPLICATION_VERSION, "1.2.3")).isEmpty();
  }

  @Test
  public void testSearchByField_Sbom_ApplicationVersion() throws Exception {
    Application app = tempEntity.newApplicationWithParent("a_SEARCH-test", "App Name 1");
    tempEntity.newSbomEvaluation(app, "1.2.3", "spdx",
        new PackageUrlIdentifier("pkg:maven/com.h2database/h2@1.4.200?type=jar"), "12345deadbeef", true,
        PENDING);
    index();

    assertThat(sbomManagerSearch(FieldIdentifier.APPLICATION_VERSION, "1.2.3")).satisfiesExactlyInAnyOrder(
        result1 -> {
          assertThat(result1.itemType).isEqualTo("SBOM_METADATA");
          assertThat(result1.applicationName).isEqualTo("App Name 1");
          assertThat(result1.applicationVersion).isEqualTo("1.2.3");
          assertThat(result1.sbomSpecification).isEqualTo("spdx");
        },
        result2 -> {
          assertThat(result2.itemType).isEqualTo("SECURITY_VULNERABILITY");
          assertThat(result2.applicationName).isEqualTo("App Name 1");
          assertThat(result2.applicationVersion).isEqualTo("1.2.3");
          assertThat(result2.sbomSpecification).isEqualTo("spdx");
          assertThat(result2.vulnerabilityId).isEqualTo("someRefId");
          assertThat(result2.vulnerabilityDescription).isEqualTo("someDescription");
          assertThat(result2.componentName).isEqualTo("com.h2database : h2 : 1.4.200");
        });
  }

  @Test
  public void testSearchByField_Sbom_SbomSpecification() throws Exception {
    Application app = tempEntity.newApplicationWithParent("a_SEARCH-test", "App Name 1");
    tempEntity.newSbomEvaluation(app, "1.2.3", "spdx",
        new PackageUrlIdentifier("pkg:maven/com.h2database/h2@1.4.200?type=jar"), "12345deadbeef", true,
        PENDING);
    index();

    assertThat(sbomManagerSearch(FieldIdentifier.SBOM_SPECIFICATION, "spdx")).satisfiesExactlyInAnyOrder(
        result1 -> {
          assertThat(result1.itemType).isEqualTo("SBOM_METADATA");
          assertThat(result1.applicationName).isEqualTo("App Name 1");
          assertThat(result1.applicationVersion).isEqualTo("1.2.3");
          assertThat(result1.sbomSpecification).isEqualTo("spdx");
        },
        result2 -> {
          assertThat(result2.itemType).isEqualTo("SECURITY_VULNERABILITY");
          assertThat(result2.applicationName).isEqualTo("App Name 1");
          assertThat(result2.applicationVersion).isEqualTo("1.2.3");
          assertThat(result2.sbomSpecification).isEqualTo("spdx");
          assertThat(result2.vulnerabilityId).isEqualTo("someRefId");
          assertThat(result2.vulnerabilityDescription).isEqualTo("someDescription");
          assertThat(result2.componentName).isEqualTo("com.h2database : h2 : 1.4.200");
        });
  }

  @Test
  public void testSearchByField_ApplicationCategoryId() throws Exception {
    Tag tag = tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID);
    index();
    assertThat(search(FieldIdentifier.APPLICATION_CATEGORY_ID, tag.getId().toLowerCase(Locale.ROOT)))
        .extracting(dto -> dto.applicationCategoryId)
        .containsExactlyInAnyOrder(tag.getId());
    assertThat(search(FieldIdentifier.APPLICATION_CATEGORY_ID, tag.getId().toUpperCase(Locale.ROOT)))
        .extracting(dto -> dto.applicationCategoryId)
        .containsExactlyInAnyOrder(tag.getId());
  }

  @Test
  public void testSearchByField_ApplicationCategoryName() throws Exception {
    Tag tag = tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID, "Search Test");
    tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID, "Search Test 2");
    index();
    assertThat(search(FieldIdentifier.APPLICATION_CATEGORY_NAME, "\"search TEST\""))
        .extracting(dto -> dto.applicationCategoryId)
        .containsExactlyInAnyOrder(tag.getId());
    assertThat(search(FieldIdentifier.APPLICATION_CATEGORY_NAME, "seaRCH")).isEmpty();
  }

  @Test
  public void testSearchByField_ApplicationCategoryColor() throws Exception {
    Tag tag = tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID, "Search Test", Color.dark_red);
    tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID, "Search Test 2", Color.dark_blue);
    index();
    assertThat(search(FieldIdentifier.APPLICATION_CATEGORY_COLOR, "DARK-red"))
        .extracting(dto -> dto.applicationCategoryId)
        .containsExactlyInAnyOrder(tag.getId());
    assertThat(search(FieldIdentifier.APPLICATION_CATEGORY_COLOR, "daRK")).isEmpty();
  }

  @Test
  public void testSearchByField_ApplicationCategoryDescription() throws Exception {
    Tag tag1 = tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID, "Category 1", "Search Test", Color.dark_red);
    Tag tag2 = tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID, "Category 2", "Search Testing 2", Color.dark_blue);
    index();
    assertThat(search(FieldIdentifier.APPLICATION_CATEGORY_DESCRIPTION, "\"search TEST\""))
        .extracting(dto -> dto.applicationCategoryId)
        .containsExactlyInAnyOrder(tag1.getId());
    assertThat(search(FieldIdentifier.APPLICATION_CATEGORY_DESCRIPTION, "seaRCH"))
        .extracting(dto -> dto.applicationCategoryId)
        .containsExactlyInAnyOrder(tag1.getId(), tag2.getId());
  }

  @Test
  public void testSearchByField_ComponentLabelId() throws Exception {
    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);
    index();
    assertThat(search(FieldIdentifier.COMPONENT_LABEL_ID, label.getId().toLowerCase(Locale.ROOT)))
        .extracting(dto -> dto.componentLabelId)
        .containsExactlyInAnyOrder(label.getId());
    assertThat(search(FieldIdentifier.COMPONENT_LABEL_ID, label.getId().toUpperCase(Locale.ROOT)))
        .extracting(dto -> dto.componentLabelId)
        .containsExactlyInAnyOrder(label.getId());
  }

  @Test
  public void testSearchByField_ComponentLabelName() throws Exception {
    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID, "Search Test");
    tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID, "Search Test 2");
    index();
    assertThat(search(FieldIdentifier.COMPONENT_LABEL_NAME, "\"search TEST\"")).extracting(dto -> dto.componentLabelId)
        .containsExactlyInAnyOrder(label.getId());
    assertThat(search(FieldIdentifier.COMPONENT_LABEL_NAME, "seaRCH")).isEmpty();
  }

  @Test
  public void testSearchByField_ComponentLabelColor() throws Exception {
    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID, "Search Test", Color.dark_red);
    tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID, "Search Test 2", Color.dark_blue);
    index();
    assertThat(search(FieldIdentifier.COMPONENT_LABEL_COLOR, "DARK-red")).extracting(dto -> dto.componentLabelId)
        .containsExactlyInAnyOrder(label.getId());
    assertThat(search(FieldIdentifier.COMPONENT_LABEL_COLOR, "daRK")).isEmpty();
  }

  @Test
  public void testSearchByField_ComponentLabelDescription() throws Exception {
    Label label1 = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID, "Category 1", "Search Test", Color.dark_red);
    Label label2 =
        tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID, "Category 2", "Search Testing 2", Color.dark_blue);
    index();
    assertThat(search(FieldIdentifier.COMPONENT_LABEL_DESCRIPTION, "\"search TEST\""))
        .extracting(dto -> dto.componentLabelId)
        .containsExactlyInAnyOrder(label1.getId());
    assertThat(search(FieldIdentifier.COMPONENT_LABEL_DESCRIPTION, "seaRCH")).extracting(dto -> dto.componentLabelId)
        .containsExactlyInAnyOrder(label1.getId(), label2.getId());
  }

  @Test
  public void testSearchByField_PolicyId() throws Exception {
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    index();
    assertThat(search(FieldIdentifier.POLICY_ID, policy.getId().toLowerCase(Locale.ROOT)))
        .extracting(dto -> dto.policyId)
        .containsExactlyInAnyOrder(policy.getId());
    assertThat(search(FieldIdentifier.POLICY_ID, policy.getId().toUpperCase(Locale.ROOT)))
        .extracting(dto -> dto.policyId)
        .containsExactlyInAnyOrder(policy.getId());
  }

  @Test
  public void testSearchByField_PolicyName() throws Exception {
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Search Test");
    tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Search Test 2");
    index();
    assertThat(search(FieldIdentifier.POLICY_NAME, "\"search TEST\"")).extracting(dto -> dto.policyId)
        .containsExactlyInAnyOrder(policy.getId());
    assertThat(search(FieldIdentifier.POLICY_NAME, "seaRCH")).isEmpty();
  }

  @Test
  public void testSearchByField_PolicyThreatCategory() throws Exception {
    Organization org = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(org, 5, LogicalOperator.AND,
        new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    tempEntity.newPolicy(org, 5, LogicalOperator.AND,
        new Condition(CoordinatesConditionType.ID, "match", "maven:foobar"));
    index();
    assertThat(search(FieldIdentifier.POLICY_THREAT_CATEGORY, "SECurity")).extracting(dto -> dto.policyId)
        .containsExactlyInAnyOrder(policy.getId());
  }

  @Test
  public void testSearchByField_PolicyThreatLevel() throws Exception {
    Policy policy1 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy 1", 1);
    Policy policy2 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy 2", 10);
    index();
    assertThat(search(FieldIdentifier.POLICY_THREAT_LEVEL, "1")).extracting(dto -> dto.policyId)
        .containsExactlyInAnyOrder(policy1.getId());
    assertThat(search(FieldIdentifier.POLICY_THREAT_LEVEL, "[9 TO 10]")).extracting(dto -> dto.policyId)
        .containsExactlyInAnyOrder(policy2.getId());
  }

  @Test
  public void testSearchByField_PolicyEvaluationStage() throws Exception {
    PolicyEvaluation eval1 = newAppReport(Stage.ID_RELEASE, "report-1");
    PolicyEvaluation eval2 = newAppReport(Stage.ID_STAGE_RELEASE, "report-2");
    index();
    assertThat(search(FieldIdentifier.POLICY_EVALUATION_STAGE, "relEASE")).extracting(dto -> dto.reportId)
        .containsOnly(eval1.getScanId());
    assertThat(search(FieldIdentifier.POLICY_EVALUATION_STAGE, "STAGE-release")).extracting(dto -> dto.reportId)
        .containsOnly(eval2.getScanId());
    assertThat(search(FieldIdentifier.POLICY_EVALUATION_STAGE, "STAGE")).isEmpty();
  }

  @Test
  public void testSearchByField_ReportId() throws Exception {
    PolicyEvaluation eval = newAppReport(Stage.ID_RELEASE, "lower-AND-UPPER-case-id");
    newAppReport(Stage.ID_STAGE_RELEASE, "report-2");
    index();
    assertThat(search(FieldIdentifier.REPORT_ID, "LOWer-and-UPPer-case-ID")).extracting(dto -> dto.reportId)
        .containsOnly(eval.getScanId());
  }

  @Test
  public void testSearchByField_ComponentHash() throws Exception {
    newAppReport(Stage.ID_RELEASE, "report-id");
    index();
    assertThat(search(FieldIdentifier.COMPONENT_HASH, "1234567890aBcDeAbCdE")).extracting(dto -> dto.componentHash)
        .containsExactlyInAnyOrder("1234567890abcdeABCDE");
  }

  @Test
  public void testSearchByField_ComponentFormat() throws Exception {
    newAppReport(Stage.ID_RELEASE, "report-id");
    index();
    assertThat(search(FieldIdentifier.COMPONENT_FORMAT, "nuGET")).extracting(dto -> dto.componentHash)
        .containsExactlyInAnyOrder("1234567890abcdeABCDE");
  }

  @Test
  public void testSearchByField_ComponentName() throws Exception {
    newAppReport(Stage.ID_RELEASE, "report-id");
    index();
    assertThat(search(FieldIdentifier.COMPONENT_NAME, "\"search.TEST 1.2.3\"")).extracting(dto -> dto.componentHash)
        .containsExactlyInAnyOrder("1234567890abcdeABCDE");
  }

  @Test
  public void testSearchByField_AllComponentsByName() throws Exception {
    newAppReport(tempEntity.newApplicationWithParent().getId(), Stage.ID_RELEASE, "report-id",
        "/IndexSearchingTest/nonVulnerableComponents");
    index();
    assertThat(searchInAllComponents(FieldIdentifier.COMPONENT_NAME, "*artifact*"))
        .extracting(dto -> dto.componentHash)
        .containsExactlyInAnyOrder("hashComponent1", "hashComponent2");
    assertThat(searchInAllComponents(FieldIdentifier.COMPONENT_NAME, "*artifact1*"))
        .extracting(dto -> dto.componentHash)
        .containsExactlyInAnyOrder("hashComponent1");
    assertThat(searchInAllComponents(FieldIdentifier.COMPONENT_NAME, "*artifact2*"))
        .extracting(dto -> dto.componentHash)
        .containsExactlyInAnyOrder("hashComponent2");
  }

  @Test
  public void testSearchByField_OnlyVulnerableComponentsByName() throws Exception {
    newAppReport(tempEntity.newApplicationWithParent().getId(), Stage.ID_RELEASE, "report-id",
        "/IndexSearchingTest/nonVulnerableComponents");
    index();
    assertThat(search(FieldIdentifier.COMPONENT_NAME, "*artifact*"))
        .extracting(dto -> dto.componentHash)
        .containsExactlyInAnyOrder("hashComponent1");
    assertThat(search(FieldIdentifier.COMPONENT_NAME, "*artifact2*"))
        .extracting(dto -> dto.componentHash)
        .isEmpty();
    assertThat(searchInAllComponents(FieldIdentifier.COMPONENT_NAME, "*artifact1*"))
        .extracting(dto -> dto.componentHash)
        .containsExactlyInAnyOrder("hashComponent1");
  }

  @Test
  public void testSearchByField_ComponentCoordinate() throws Exception {
    newAppReport(Stage.ID_RELEASE, "report-id");
    index();
    assertThat(search(FieldIdentifier.COMPONENT_COORDINATE + "PackageId:search.TEST"))
        .extracting(dto -> dto.componentHash)
        .containsExactlyInAnyOrder("1234567890abcdeABCDE");
    assertThat(search(FieldIdentifier.COMPONENT_COORDINATE + "Version:1.2.3")).extracting(dto -> dto.componentHash)
        .containsExactlyInAnyOrder("1234567890abcdeABCDE");
  }

  @Test
  public void testSearchByField_VulnerabilityId() throws Exception {
    newAppReport(Stage.ID_RELEASE, "report-id");
    index();
    assertThat(search(FieldIdentifier.VULNERABILITY_ID, "cvE-8765-1234")).extracting(dto -> dto.vulnerabilityId)
        .containsOnly("CVE-8765-1234");
    assertThat(search(FieldIdentifier.VULNERABILITY_ID, "cvE-8765")).isEmpty();
  }

  @Test
  public void testSearchByField_VulnerabilitySeverity() throws Exception {
    newAppReport(Stage.ID_RELEASE, "report-id");
    index();
    assertThat(search(FieldIdentifier.VULNERABILITY_SEVERITY, "4.5")).extracting(dto -> dto.vulnerabilityId)
        .containsOnly("CVE-8765-1234");
    assertThat(search(FieldIdentifier.VULNERABILITY_SEVERITY, "[5 TO 10]")).extracting(dto -> dto.vulnerabilityId)
        .containsOnly("sonatype-8765-1234");
  }

  @Test
  public void testSearchByField_VulnerabilityStatus() throws Exception {
    newAppReport(Stage.ID_RELEASE, "report-id");
    index();
    assertThat(search(FieldIdentifier.VULNERABILITY_STATUS, "acKNOWledged")).extracting(dto -> dto.vulnerabilityId)
        .containsOnly("CVE-8765-1234");
  }

  @Test
  public void testSearchByField_VulnerabilityDescription() throws Exception {
    String vulnDescription = "Cross-Site Scripting (XSS)";
    when(vulnerabilityDescriptionFetcher.getVulnerabilityDescription("CVE-8765-1234")).thenReturn(vulnDescription);
    newAppReport(Stage.ID_RELEASE, "report-id");
    index();
    assertThat(search(FieldIdentifier.VULNERABILITY_DESCRIPTION, "xSs")).extracting(dto -> dto.vulnerabilityId)
        .containsOnly("CVE-8765-1234");
    assertThat(search(FieldIdentifier.VULNERABILITY_DESCRIPTION, "\"cross-site scripting\""))
        .extracting(dto -> dto.vulnerabilityId)
        .containsOnly("CVE-8765-1234");
  }

  @Test
  public void testBoosting() throws Exception {
    Application app1 = tempEntity.newApplicationWithParent();
    Application app2 = tempEntity.newApplicationWithParent();
    index();
    List<SearchResultItemDTO> results = search(FieldIdentifier.APPLICATION_ID + ":" + app1.getId() + "^2 "
        + FieldIdentifier.APPLICATION_ID + ":" + app2.getId());
    assertThat(results).extracting(dto -> dto.applicationId).containsExactly(app1.getId(), app2.getId());
    results = search(FieldIdentifier.APPLICATION_ID + ":" + app1.getId() + " "
        + FieldIdentifier.APPLICATION_ID + ":" + app2.getId() + "^2");
    assertThat(results).extracting(dto -> dto.applicationId).containsExactly(app2.getId(), app1.getId());
  }

  @Test
  public void testLeadingWildcard_SingleCharacter() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test-id-1", "appName");
    tempEntity.newApplicationWithParent("test-id-2", "ppName");
    index();
    assertThat(search(FieldIdentifier.APPLICATION_NAME, "?ppName")).extracting(dto -> dto.applicationId)
        .containsExactlyInAnyOrder(app.getId());
    assertThat(search(FieldIdentifier.APPLICATION_NAME, "\\?ppName")).isEmpty();
  }

  @Test
  public void testTrailingWildcard_SingleCharacter() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test-id-1", "appName");
    tempEntity.newApplicationWithParent("test-id-2", "appNam");
    index();
    assertThat(search(FieldIdentifier.APPLICATION_NAME, "appNam?")).extracting(dto -> dto.applicationId)
        .containsExactlyInAnyOrder(app.getId());
    assertThat(search(FieldIdentifier.APPLICATION_NAME, "appNam\\?")).isEmpty();
  }

  @Test
  public void testLeadingWildcard_MultipleCharacters() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test-id-1", "appName1");
    tempEntity.newApplicationWithParent("test-id-2", "appName2");
    index();
    assertThat(search(FieldIdentifier.APPLICATION_NAME, "*1")).extracting(dto -> dto.applicationId)
        .containsExactlyInAnyOrder(app.getId());
    assertThat(search(FieldIdentifier.APPLICATION_NAME, "\\*1")).isEmpty();
  }

  @Test
  public void testTrailingWildcard_MultipleCharacters() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test-id-1", "app1Name");
    tempEntity.newApplicationWithParent("test-id-2", "app2Name");
    index();
    assertThat(search(FieldIdentifier.APPLICATION_NAME, "app1*")).extracting(dto -> dto.applicationId)
        .containsExactlyInAnyOrder(app.getId());
    assertThat(search(FieldIdentifier.APPLICATION_NAME, "app1\\*")).isEmpty();
  }

  @Test
  public void testSearchByDefaultFieldAndOther() throws Exception {
    String appId = newAppReport(Stage.ID_BUILD, "report-0").getOwnerId();
    newAppReport(Stage.ID_BUILD, "report-1");
    index();
    assertThat(search("CVE-8765-1234 AND " + FieldIdentifier.APPLICATION_ID + ":" + appId))
        .extracting(dto -> dto.reportId)
        .containsExactlyInAnyOrder("report-0");
  }

  @Test
  public void testSearchByFieldsFromDifferentDocumentTypes() throws Exception {
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    Tag tag = tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID);
    index();
    assertThat(search(FieldIdentifier.POLICY_ID + ":" + policy.getId() + " " + FieldIdentifier.APPLICATION_CATEGORY_ID
        + ":" + tag.getId())).extracting(dto -> dto.itemType)
            .containsExactlyInAnyOrder(ItemType.POLICY.name(),
                ItemType.APPLICATION_CATEGORY.name());
  }

  @Test
  public void testIncrementalUpdate_NewPolicyEvaluation() throws Exception {
    index();

    PolicyEvaluation eval = newAppReport(Stage.ID_BUILD, "old-report-id");
    indexChanges();
    assertThat(search(FieldIdentifier.POLICY_EVALUATION_STAGE, eval.getStageTypeId())).extracting(dto -> dto.reportId)
        .containsOnly(eval.getScanId());

    PolicyEvaluation newEval1 = newAppReport(eval.getOwnerId(), eval.getStageTypeId(), "new-report-id-1");
    PolicyEvaluation newEval2 = newAppReport(eval.getOwnerId(), Stage.ID_RELEASE, "new-report-id-2");
    indexChanges();
    assertThat(search(FieldIdentifier.POLICY_EVALUATION_STAGE, newEval1.getStageTypeId()))
        .extracting(dto -> dto.reportId)
        .containsOnly(newEval1.getScanId());
    assertThat(search(FieldIdentifier.POLICY_EVALUATION_STAGE, newEval2.getStageTypeId()))
        .extracting(dto -> dto.reportId)
        .containsOnly(newEval2.getScanId());
  }

  @Test
  public void testIncrementalUpdate_Application() throws Exception {
    index();

    // Add application
    Application app = tempEntity.newApplicationWithParent();
    indexChanges();
    List<SearchResultItemDTO> searchResults = search(FieldIdentifier.APPLICATION_NAME, app.getName());
    assertThat(searchResults).hasSize(1);
    assertApplicationData(searchResults.get(0), app);

    // Add app related entities and re-index everything.
    // The docs for these new entities should be updated when the app is updated.
    tempEntity.newLabel(app.getId());
    tempEntity.newPolicy(app);
    newAppReport(app.getId(), Stage.ID_BUILD, "testReportId");
    index();
    searchResults = search(FieldIdentifier.APPLICATION_NAME, app.getName());
    // There should be 5 results: app, label, policy, 2 SVs
    assertThat(searchResults).hasSize(5);
    searchResults.forEach(searchResult -> assertApplicationData(searchResult, app));

    // Update application
    String oldAppName = app.getName();
    String oldAppPublicId = app.getPublicId();
    app.setName("NewAppName");
    app.setPublicId("NewAppPublicId");
    applicationDAO.update(app);
    indexChanges();
    // Verify the new values are in the index
    searchResults = search(FieldIdentifier.APPLICATION_NAME, app.getName());
    assertThat(searchResults).hasSize(5);
    searchResults.forEach(searchResult -> assertApplicationData(searchResult, app));
    // Verify the old values were removed from the index
    assertThat(search(FieldIdentifier.APPLICATION_NAME, oldAppName)).isEmpty();
    assertThat(search(FieldIdentifier.APPLICATION_PUBLIC_ID, oldAppPublicId)).isEmpty();

    // Delete application
    applicationDAO.delete(app);
    indexChanges();
    assertThat(search(FieldIdentifier.APPLICATION_ID, app.getId())).isEmpty();
    assertThat(search(FieldIdentifier.APPLICATION_NAME, app.getName())).isEmpty();
    assertThat(search(FieldIdentifier.APPLICATION_PUBLIC_ID, app.getPublicId())).isEmpty();
  }

  @Test
  public void testIncrementalUpdate_Sbom() throws Exception {
    Path reportZipPath = Paths.get(getClass().getResource("/IndexSearchingTest/sbom/report.zip").toURI());
    hdsMockServer.respondWith("{ \"scanId\": \"hds-scan-id\" }").atUri("/rest/application/analysis");
    hdsMockServer.respondWith(Files.readAllBytes(reportZipPath)).atUri("/rest/application/analysis/hds-scan-id");
    hdsMockServer.respondWith("{}")
        .atUri("/rest/vulnerability/details/json/CVE-2018-7489");
    hdsMockServer.respondWith("{}")
        .atUri("/rest/vulnerability/details/json/CVE-2020-25649");
    hdsMockServer.respondWith("{}")
        .atUri("/rest/vulnerability/details/json/CVE-2020-36518");
    hdsMockServer.respondWith("{}")
        .atUri("/rest/vulnerability/details/json/CVE-2022-42003");
    hdsMockServer.respondWith("{}")
        .atUri("/rest/vulnerability/details/json/CVE-2022-42004");
    hdsMockServer.respondWith("{}")
        .atUri("/rest/vulnerability/details/json/sonatype-2020-1579");

    Organization org = tempEntity.newOrganization("org");
    Application app = tempEntity.newApplication("app", org.getId());
    InputStream binaryUploadInputStream = getClass().getResourceAsStream("/IndexSearchingTest/sbom/vuln-bom.xml");

    index();

    ApiThirdPartyScanTicketDTO scanTicket = (ApiThirdPartyScanTicketDTO) apiSbomService
        .importSbom(app.getId(), binaryUploadInputStream, "file.txt", true, "", null, false)
        .getEntity();

    // Wait for the import processing to complete
    int totalWait = 0;
    String scanRequestId = scanTicket.statusUrl.substring(scanTicket.statusUrl.lastIndexOf('/') + 1);
    ApiThirdPartyScanResultDTO scanResult = await().pollInSameThread()
        .atMost(30, TimeUnit.SECONDS)
        .pollInterval(Duration.ofSeconds(1))
        .until(() -> {
          try {
            return apiThirdPartyScanService.getScanStatus(app.getId(), scanRequestId);
          }
          catch (NotFoundException e) {
            return null;
          }
        }, notNullValue());

    assertThat(scanResult.errorMessage).isBlank();
    assertThat(scanResult.isError).isFalse();
    assertThat((long) totalWait).isLessThanOrEqualTo(Duration.ofSeconds(30).toMillis());
    assertThat(searchIndexChangeDAO.getAll()).filteredOn(change -> change.getChangeType() == ChangeType.SBOM)
        .isNotEmpty();

    indexChanges();

    List<SearchResultItemDTO> results = sbomManagerSearch(FieldIdentifier.ITEM_TYPE, "*");
    assertThat(results).satisfiesExactlyInAnyOrder(
        organization -> {
          assertThat(organization.itemType).isEqualTo("ORGANIZATION");
          assertThat(organization.organizationName).isEqualTo("Root Organization");
        },
        organization -> {
          assertThat(organization.itemType).isEqualTo("ORGANIZATION");
          assertThat(organization.organizationName).isEqualTo("org");
        },
        application -> {
          assertThat(application.itemType).isEqualTo("APPLICATION");
          assertThat(application.organizationName).isEqualTo("org");
          assertThat(application.applicationPublicId).isEqualTo("app");
        },
        sbom -> {
          assertThat(sbom.itemType).isEqualTo("SBOM_METADATA");
          assertThat(sbom.organizationName).isEqualTo("org");
          assertThat(sbom.applicationPublicId).isEqualTo("app");
          assertThat(sbom.sbomSpecification).isEqualTo("CycloneDx");
          assertThat(sbom.applicationVersion).isNotBlank();
        },
        sbomVuln -> {
          assertThat(sbomVuln.itemType).isEqualTo("SECURITY_VULNERABILITY");
          assertThat(sbomVuln.organizationName).isEqualTo("org");
          assertThat(sbomVuln.applicationPublicId).isEqualTo("app");
          assertThat(sbomVuln.applicationVersion).isNotBlank();
          assertThat(sbomVuln.sbomSpecification).isEqualTo("CycloneDx");
          assertThat(sbomVuln.vulnerabilityId).isEqualTo("SNYK-JAVA-COMFASTERXMLJACKSONCORE-32111");
          assertThat(sbomVuln.componentName).isEqualTo("com.fasterxml.jackson.core : jackson-databind : 2.9.4");
        },
        hdsVuln1 -> {
          assertThat(hdsVuln1.itemType).isEqualTo("SECURITY_VULNERABILITY");
          assertThat(hdsVuln1.organizationName).isEqualTo("org");
          assertThat(hdsVuln1.applicationPublicId).isEqualTo("app");
          assertThat(hdsVuln1.applicationVersion).isNotBlank();
          assertThat(hdsVuln1.sbomSpecification).isEqualTo("CycloneDx");
          assertThat(hdsVuln1.vulnerabilityId).isEqualTo("CVE-2018-7489");
          assertThat(hdsVuln1.componentName).isEqualTo("com.fasterxml.jackson.core : jackson-databind : 2.9.4");
        },
        hdsVuln2 -> {
          assertThat(hdsVuln2.itemType).isEqualTo("SECURITY_VULNERABILITY");
          assertThat(hdsVuln2.organizationName).isEqualTo("org");
          assertThat(hdsVuln2.applicationPublicId).isEqualTo("app");
          assertThat(hdsVuln2.applicationVersion).isNotBlank();
          assertThat(hdsVuln2.sbomSpecification).isEqualTo("CycloneDx");
          assertThat(hdsVuln2.vulnerabilityId).isEqualTo("CVE-2020-25649");
          assertThat(hdsVuln2.componentName).isEqualTo("com.fasterxml.jackson.core : jackson-databind : 2.9.4");
        },
        hdsVuln3 -> {
          assertThat(hdsVuln3.itemType).isEqualTo("SECURITY_VULNERABILITY");
          assertThat(hdsVuln3.organizationName).isEqualTo("org");
          assertThat(hdsVuln3.applicationPublicId).isEqualTo("app");
          assertThat(hdsVuln3.applicationVersion).isNotBlank();
          assertThat(hdsVuln3.sbomSpecification).isEqualTo("CycloneDx");
          assertThat(hdsVuln3.vulnerabilityId).isEqualTo("CVE-2020-36518");
          assertThat(hdsVuln3.componentName).isEqualTo("com.fasterxml.jackson.core : jackson-databind : 2.9.4");
        },
        hdsVuln4 -> {
          assertThat(hdsVuln4.itemType).isEqualTo("SECURITY_VULNERABILITY");
          assertThat(hdsVuln4.organizationName).isEqualTo("org");
          assertThat(hdsVuln4.applicationPublicId).isEqualTo("app");
          assertThat(hdsVuln4.applicationVersion).isNotBlank();
          assertThat(hdsVuln4.sbomSpecification).isEqualTo("CycloneDx");
          assertThat(hdsVuln4.vulnerabilityId).isEqualTo("CVE-2022-42003");
          assertThat(hdsVuln4.componentName).isEqualTo("com.fasterxml.jackson.core : jackson-databind : 2.9.4");
        },
        hdsVuln5 -> {
          assertThat(hdsVuln5.itemType).isEqualTo("SECURITY_VULNERABILITY");
          assertThat(hdsVuln5.organizationName).isEqualTo("org");
          assertThat(hdsVuln5.applicationPublicId).isEqualTo("app");
          assertThat(hdsVuln5.applicationVersion).isNotBlank();
          assertThat(hdsVuln5.sbomSpecification).isEqualTo("CycloneDx");
          assertThat(hdsVuln5.vulnerabilityId).isEqualTo("CVE-2022-42004");
          assertThat(hdsVuln5.componentName).isEqualTo("com.fasterxml.jackson.core : jackson-databind : 2.9.4");
        },
        hdsVuln6 -> {
          assertThat(hdsVuln6.itemType).isEqualTo("SECURITY_VULNERABILITY");
          assertThat(hdsVuln6.organizationName).isEqualTo("org");
          assertThat(hdsVuln6.applicationPublicId).isEqualTo("app");
          assertThat(hdsVuln6.applicationVersion).isNotBlank();
          assertThat(hdsVuln6.sbomSpecification).isEqualTo("CycloneDx");
          assertThat(hdsVuln6.vulnerabilityId).isEqualTo("sonatype-2020-1579");
          assertThat(hdsVuln6.componentName).isEqualTo("prismjs : 1.27.0");
        },
        legalViolation1 -> {
          assertThat(legalViolation1.itemType).isEqualTo("LEGAL_VIOLATION");
          assertThat(legalViolation1.organizationName).isEqualTo("org");
          assertThat(legalViolation1.applicationPublicId).isEqualTo("app");
          assertThat(legalViolation1.componentName).isEqualTo("com.fasterxml.jackson.core : jackson-databind : 2.9.4");
          assertThat(legalViolation1.componentEffectiveLicenseId).isEqualTo("Apache-2.0");
        },
        legalViolation2 -> {
          assertThat(legalViolation2.itemType).isEqualTo("LEGAL_VIOLATION");
          assertThat(legalViolation2.organizationName).isEqualTo("org");
          assertThat(legalViolation2.applicationPublicId).isEqualTo("app");
          assertThat(legalViolation2.componentName).isEqualTo("prismjs : 1.27.0");
          assertThat(legalViolation2.componentEffectiveLicenseId).isEqualTo("MIT");
        },
        legalViolation3 -> {
          assertThat(legalViolation3.itemType).isEqualTo("LEGAL_VIOLATION");
          assertThat(legalViolation3.organizationName).isEqualTo("org");
          assertThat(legalViolation3.applicationPublicId).isEqualTo("app");
          assertThat(legalViolation3.componentName).isEqualTo("prismjs : 1.27.0");
          assertThat(legalViolation3.componentEffectiveLicenseId).isEqualTo("Not-Supported");
        });
  }

  @Test
  public void testIncrementalUpdate_Organization() throws Exception {
    index();

    // Add organization
    Organization org = tempEntity.newOrganization("TestOrgName");
    indexChanges();
    List<SearchResultItemDTO> searchResults = search(FieldIdentifier.ORGANIZATION_NAME, org.getName());
    assertThat(searchResults).hasSize(1);
    assertOrganizationData(searchResults.get(0), org);

    // Add org related entities and re-index everything.
    // The docs for these new entities should be updated when the org is updated.
    Application app = tempEntity.newApplication(org.getId());
    tempEntity.newTag(org.getId());
    tempEntity.newLabel(org.getId());
    tempEntity.newPolicy(org);
    index();
    searchResults = search(FieldIdentifier.ORGANIZATION_NAME, org.getName());
    // There should be 5 results: org, app category, label, policy
    assertThat(searchResults).hasSize(5);
    searchResults.forEach(searchResult -> assertOrganizationData(searchResult, org));

    // Update organization
    String oldOrgName = org.getName();
    org.setName("NewOrgName");
    spyOrganizationDAO.update(org);
    indexChanges();
    // Verify the new values are in the index
    searchResults = search(FieldIdentifier.ORGANIZATION_NAME, org.getName());
    assertThat(searchResults).hasSize(5);
    searchResults.forEach(searchResult -> assertOrganizationData(searchResult, org));
    // Verify the old values were removed from the index
    assertThat(search(FieldIdentifier.ORGANIZATION_NAME, oldOrgName)).isEmpty();

    // Delete organization
    applicationDAO.delete(app);
    spyOrganizationDAO.delete(org);
    indexChanges();
    assertThat(search(FieldIdentifier.ORGANIZATION_ID, org.getId())).isEmpty();
    assertThat(search(FieldIdentifier.ORGANIZATION_NAME, org.getName())).isEmpty();
  }

  @Test
  public void testUpdate_OrganizationNameChange() throws Exception {
    index();

    Organization org = tempEntity.newOrganization("TestOrgName");
    Application app = tempEntity.newApplication(org.getId());
    newAppReport(app.getId(), StageTypes.BUILD.getId(), "");

    indexChanges();
    List<SearchResultItemDTO> searchResults = search(FieldIdentifier.ORGANIZATION_NAME, org.getName());
    assertThat(searchResults).hasSize(4);

    Set<String> searchResultItemTypes =
        searchResults.stream().map(searchResultItemDTO -> searchResultItemDTO.itemType).collect(Collectors.toSet());
    assertThat(searchResultItemTypes).containsExactlyInAnyOrder(ItemType.ORGANIZATION.name(),
        ItemType.APPLICATION.name(), ItemType.SECURITY_VULNERABILITY.name());

    org.setName("NewOrgName");
    spyOrganizationDAO.update(org);
    indexChanges();

    searchResults = search(FieldIdentifier.ORGANIZATION_NAME, org.getName());
    assertThat(searchResults).hasSize(4);
    searchResultItemTypes =
        searchResults.stream().map(searchResultItemDTO -> searchResultItemDTO.itemType).collect(Collectors.toSet());
    assertThat(searchResultItemTypes).containsExactlyInAnyOrder(ItemType.ORGANIZATION.name(),
        ItemType.APPLICATION.name(), ItemType.SECURITY_VULNERABILITY.name());
  }

  @Test
  public void testUpdate_OrganizationNameChange_OrganizationHierarchy() throws Exception {
    // org hierarchy for this test: foo -> bar -> baz
    index();

    Organization fooOrg = tempEntity.newOrganization("foo");
    Application fooApp = tempEntity.newApplication(fooOrg.getId());

    Organization barOrg = tempEntity.newOrganization("bar", fooOrg);
    Application barApp = tempEntity.newApplication(barOrg.getId());

    Organization bazOrg = tempEntity.newOrganization("baz", barOrg);
    Application bazApp = tempEntity.newApplication(bazOrg.getId());

    newAppReport(fooApp.getId(), StageTypes.BUILD.getId(), "");
    newAppReport(barApp.getId(), StageTypes.BUILD.getId(), "");
    newAppReport(bazApp.getId(), StageTypes.BUILD.getId(), "");

    indexChanges();

    List<SearchResultItemDTO> searchResults;

    searchResults = search(FieldIdentifier.ORGANIZATION_NAME, fooOrg.getName());
    assertThat(searchResults).hasSize(8);
    searchResults = search(FieldIdentifier.ORGANIZATION_NAME, barOrg.getName());
    assertThat(searchResults).hasSize(6);
    searchResults = search(FieldIdentifier.ORGANIZATION_NAME, bazOrg.getName());
    assertThat(searchResults).hasSize(4);

    barOrg.setName("bar-new-name");
    spyOrganizationDAO.update(barOrg);
    indexChanges();

    searchResults = search(FieldIdentifier.ORGANIZATION_NAME, fooOrg.getName());
    assertThat(searchResults).hasSize(8);
    searchResults = search(FieldIdentifier.ORGANIZATION_NAME, barOrg.getName());
    assertThat(searchResults).hasSize(6);
    searchResults = search(FieldIdentifier.ORGANIZATION_NAME, bazOrg.getName());
    assertThat(searchResults).hasSize(4);
  }

  @Test
  public void testIncrementalUpdate_Label() throws Exception {
    index();
    String labelName = "labelName";
    String labelDescription = "labelDescription";
    Color labelColor = Color.dark_blue;

    // Add label
    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID, labelName, labelDescription, labelColor);
    indexChanges();
    List<SearchResultItemDTO> searchResults = search(FieldIdentifier.COMPONENT_LABEL_NAME, label.getLabel());
    assertThat(searchResults).hasSize(1);
    assertComponentLabelData(searchResults.get(0), label);

    // Update label
    String oldLabelName = label.getLabel();
    Color oldLabelColor = label.getColor();
    String oldLabelDescription = label.getDescription();
    label.setLabel("NewLabelName");
    label.setDescription("NewLabelDescription");
    label.setColor(Color.dark_green);
    labelDAO.update(label);
    indexChanges();
    // Verify the new values are in the index
    searchResults = search(FieldIdentifier.COMPONENT_LABEL_NAME, label.getLabel());
    assertThat(searchResults).hasSize(1);
    assertComponentLabelData(searchResults.get(0), label);
    // Verify the old values were removed from the index
    assertThat(search(FieldIdentifier.COMPONENT_LABEL_NAME, oldLabelName)).isEmpty();
    assertThat(search(FieldIdentifier.COMPONENT_LABEL_COLOR, oldLabelColor.toValue())).isEmpty();
    assertThat(search(FieldIdentifier.COMPONENT_LABEL_DESCRIPTION, oldLabelDescription)).isEmpty();

    // Delete label
    labelDAO.delete(label);
    indexChanges();
    assertThat(search(FieldIdentifier.COMPONENT_LABEL_ID, label.getId())).isEmpty();
    assertThat(search(FieldIdentifier.COMPONENT_LABEL_NAME, label.getLabel())).isEmpty();
    assertThat(search(FieldIdentifier.COMPONENT_LABEL_DESCRIPTION, label.getDescription())).isEmpty();
    assertThat(search(FieldIdentifier.COMPONENT_LABEL_COLOR, label.getColor().toValue())).isEmpty();
  }

  @Test
  public void testIncrementalUpdate_Policy() throws Exception {
    index();

    // Add policy
    String policyName = "policyName";
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, policyName);

    indexChanges();
    List<SearchResultItemDTO> searchResults = search(FieldIdentifier.POLICY_NAME, policyName);
    assertThat(searchResults).hasSize(1);
    assertPolicyData(searchResults.get(0), policy);

    // Update policy
    policy.setName("NewPolicyName");
    policyDAO.update(policy);
    indexChanges();
    // Verify the new values are in the index
    searchResults = search(FieldIdentifier.POLICY_NAME, policy.getName());
    assertThat(searchResults).hasSize(1);
    assertPolicyData(searchResults.get(0), policy);
    // Verify the old values were removed from the index
    assertThat(search(FieldIdentifier.POLICY_NAME, policyName)).isEmpty();

    // Delete policy
    policyDAO.delete(policy);
    indexChanges();
    assertThat(search(FieldIdentifier.POLICY_ID, policy.getId())).isEmpty();
    assertThat(search(FieldIdentifier.POLICY_NAME, policy.getName())).isEmpty();
  }

  @Test
  public void testIncrementalUpdate_Tag() throws Exception {
    index();
    String tagName = "tagName";
    String tagDescription = "tagDescription";
    Color tagColor = Color.dark_blue;

    // Add application category
    Tag tag = tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID, tagName, tagDescription, tagColor);
    indexChanges();
    List<SearchResultItemDTO> searchResults = search(FieldIdentifier.APPLICATION_CATEGORY_NAME, tag.getName());
    assertThat(searchResults).hasSize(1);
    assertApplicationCategoryData(searchResults.get(0), tag);

    // Update application category
    String oldTagName = tag.getName();
    Color oldTagColor = tag.getColor();
    String oldTagDescription = tag.getDescription();
    tag.setName("NewTagName");
    tag.setDescription("NewTagDescription");
    tag.setColor(Color.dark_green);
    tagDAO.update(tag);
    indexChanges();
    // Verify the new values are in the index
    searchResults = search(FieldIdentifier.APPLICATION_CATEGORY_NAME, tag.getName());
    assertThat(searchResults).hasSize(1);
    assertApplicationCategoryData(searchResults.get(0), tag);
    // Verify the old values were removed from the index
    assertThat(search(FieldIdentifier.APPLICATION_CATEGORY_NAME, oldTagName)).isEmpty();
    assertThat(search(FieldIdentifier.APPLICATION_CATEGORY_COLOR, oldTagColor.toValue())).isEmpty();
    assertThat(search(FieldIdentifier.APPLICATION_CATEGORY_DESCRIPTION, oldTagDescription)).isEmpty();

    // Delete application category
    tagDAO.delete(tag);
    indexChanges();
    assertThat(search(FieldIdentifier.APPLICATION_CATEGORY_ID, tag.getId())).isEmpty();
    assertThat(search(FieldIdentifier.APPLICATION_CATEGORY_NAME, tag.getName())).isEmpty();
    assertThat(search(FieldIdentifier.APPLICATION_CATEGORY_DESCRIPTION, tag.getDescription())).isEmpty();
    assertThat(search(FieldIdentifier.APPLICATION_CATEGORY_COLOR, tag.getColor().toValue())).isEmpty();
  }

  // --- Policy Violation & Legal Violation test helpers ---

  private PolicyEvaluation setupPolicyViolationTestData(Application app) throws Exception {
    String orgId = app.getOrganizationId();
    PolicyEvaluation evaluation = newAppReport(app.getId(), Stage.ID_BUILD, "pvtReport001",
        "/IndexSearchingTest/policyViolationReport");

    Policy pLegalCopyleft = tempEntity.newPolicy(orgId, "Legal - Copyleft");
    Policy pSecurityCritical = tempEntity.newPolicy(orgId, "Security - Critical");
    Policy pQualityStandards = tempEntity.newPolicy(orgId, "Quality - Standards");
    Policy pLegalNonStandard = tempEntity.newPolicy(orgId, "Legal - Non-Standard");

    tempEntity.newPolicyViolation(evaluation, pLegalCopyleft, 8, PolicyThreatCategory.LICENSE,
        ComponentIdentifier.createMavenCoordinates("com.lodash", "lodash", "4.17.15"),
        "hashPvtLodash000000", null);
    tempEntity.newPolicyViolation(evaluation, pSecurityCritical, 10, PolicyThreatCategory.SECURITY,
        ComponentIdentifier.createMavenCoordinates("org.apache.logging", "log4j-core", "2.14.0"),
        "hashPvtLog4j00000000", null);

    PolicyWaiver waiver = tempEntity.newWaiver(pQualityStandards.getId(), orgId);
    tempEntity.newWaivedPolicyViolation(evaluation, pQualityStandards, 3, PolicyThreatCategory.QUALITY,
        ComponentIdentifier.createMavenCoordinates("net.busybox", "busybox", "1.33"),
        "hashPvtBusybox000000", waiver);

    AutoPolicyWaiver autoWaiver = tempEntity.newAutoPolicyWaiver(orgId);
    PolicyWaiver waiverForAuto = tempEntity.newWaiver(pLegalNonStandard.getId(), orgId);
    PolicyViolation pvAutoWaived = tempEntity.newWaivedPolicyViolation(
        evaluation, pLegalNonStandard, 6, PolicyThreatCategory.LICENSE,
        ComponentIdentifier.createMavenCoordinates("org.openssl", "openssl", "3.0"),
        "hashPvtOpenssl00000", waiverForAuto);
    pvAutoWaived.setAutoPolicyWaiverId(autoWaiver.getId());
    try (TransactionContext tx = policyViolationDAO.createTransactionContext()) {
      tx.begin();
      policyViolationDAO.update(tx, pvAutoWaived);
      tx.commit();
    }

    tempEntity.newLicenseThreatGroup(orgId, "Copyleft", 9, "GPL-3.0", "GPL-2.0");
    tempEntity.newLicenseThreatGroup(orgId, "Permissive", 1, "MIT", "Apache-2.0");
    tempEntity.newLicenseThreatGroup(orgId, "Non-Standard", 5, "OpenSSL");

    return evaluation;
  }

  private void assertPolicyViolationFields(
      SearchResultItemDTO result,
      String expectedPolicyName,
      String expectedThreatCategory,
      int expectedThreatLevel,
      String expectedWaiverStatus)
  {
    assertThat(result.itemType).isEqualTo(ItemType.POLICY_VIOLATION.name());
    assertThat(result.policyViolationPolicyName).isEqualTo(expectedPolicyName);
    assertThat(result.policyViolationThreatCategory).isEqualTo(expectedThreatCategory);
    assertThat(result.policyViolationThreatLevel).isEqualTo(expectedThreatLevel);
    assertThat(result.policyViolationWaiverStatus).isEqualTo(expectedWaiverStatus);
  }

  private void assertLegalViolationFields(
      SearchResultItemDTO result,
      String expectedLicenseId,
      String expectedLicenseName,
      String expectedThreatGroupName,
      int expectedThreatLevel)
  {
    assertThat(result.itemType).isEqualTo(ItemType.LEGAL_VIOLATION.name());
    assertThat(result.componentEffectiveLicenseId).isEqualTo(expectedLicenseId);
    assertThat(result.componentEffectiveLicenseName).isEqualTo(expectedLicenseName);
    assertThat(result.componentLicenseThreatGroupName).isEqualTo(expectedThreatGroupName);
    assertThat(result.componentLicenseThreatLevel).isEqualTo(expectedThreatLevel);
  }

  // --- IT-1: Search by policy violation policy name ---
  @Test
  public void testSearchByField_PolicyViolationPolicyName() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    setupPolicyViolationTestData(app);
    index();

    List<SearchResultItemDTO> results = search(FieldIdentifier.POLICY_VIOLATION_POLICY_NAME,
        "\"Legal - Copyleft\"");
    assertThat(results).hasSize(1);
    assertPolicyViolationFields(results.get(0), "Legal - Copyleft", "license", 8, "Active");
    assertThat(results.get(0).componentHash).isEqualTo("hashPvtLodash000000");
    assertThat(results.get(0).organizationId).isEqualTo(org.getId());
    assertThat(results.get(0).applicationId).isEqualTo(app.getId());
    assertThat(results.get(0).policyEvaluationStage).isNotNull();
  }

  // --- IT-2: Search by policy violation threat category ---
  @Test
  public void testSearchByField_PolicyViolationThreatCategory() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    setupPolicyViolationTestData(app);
    index();

    List<SearchResultItemDTO> results = search(FieldIdentifier.POLICY_VIOLATION_THREAT_CATEGORY,
        "\"license\"");
    assertThat(results).hasSize(2);
    assertThat(results).extracting(r -> r.componentHash)
        .containsExactlyInAnyOrder("hashPvtLodash000000", "hashPvtOpenssl00000");
  }

  // --- IT-3: Search by policy violation threat level range ---
  @Test
  public void testSearchByField_PolicyViolationThreatLevel_RangeQuery() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    setupPolicyViolationTestData(app);
    index();

    List<SearchResultItemDTO> results = search("policyViolationThreatLevel:[7 TO 10]");
    assertThat(results).hasSize(2);
    assertThat(results).extracting(r -> r.componentHash)
        .containsExactlyInAnyOrder("hashPvtLodash000000", "hashPvtLog4j00000000");
  }

  // --- IT-4: Search by waiver status Active ---
  @Test
  public void testSearchByField_PolicyViolationWaiverStatus_Active() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    setupPolicyViolationTestData(app);
    index();

    List<SearchResultItemDTO> results = search(FieldIdentifier.POLICY_VIOLATION_WAIVER_STATUS,
        "\"Active\"");
    assertThat(results).hasSize(2);
    assertThat(results).extracting(r -> r.componentHash)
        .containsExactlyInAnyOrder("hashPvtLodash000000", "hashPvtLog4j00000000");
  }

  // --- IT-5: Search by waiver status Waived ---
  @Test
  public void testSearchByField_PolicyViolationWaiverStatus_Waived() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    setupPolicyViolationTestData(app);
    index();

    List<SearchResultItemDTO> results = search(FieldIdentifier.POLICY_VIOLATION_WAIVER_STATUS,
        "\"Waived\"");
    assertThat(results).hasSize(1);
    assertThat(results.get(0).componentHash).isEqualTo("hashPvtBusybox000000");
  }

  // --- IT-6: Search by waiver status AutoWaived ---
  @Test
  public void testSearchByField_PolicyViolationWaiverStatus_AutoWaived() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    setupPolicyViolationTestData(app);
    index();

    List<SearchResultItemDTO> results = search(FieldIdentifier.POLICY_VIOLATION_WAIVER_STATUS,
        "\"AutoWaived\"");
    assertThat(results).hasSize(1);
    assertThat(results.get(0).componentHash).isEqualTo("hashPvtOpenssl00000");
  }

  // --- IT-7: Combine violation search with component name ---
  @Test
  public void testSearchByField_PolicyViolation_CombinedWithComponentName() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    setupPolicyViolationTestData(app);
    index();

    List<SearchResultItemDTO> results = search(
        "policyViolationThreatCategory:\"license\" AND componentName:*lodash*");
    assertThat(results).hasSize(1);
    assertThat(results.get(0).componentHash).isEqualTo("hashPvtLodash000000");
  }

  // --- IT-8: Combine violation search with application name ---
  @Test
  public void testSearchByField_PolicyViolation_CombinedWithApplicationName() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    setupPolicyViolationTestData(app);
    index();

    List<SearchResultItemDTO> results = search(
        "policyViolationThreatCategory:\"security\" AND applicationName:\"" + app.getName() + "\"");
    assertThat(results).hasSize(1);
    assertThat(results.get(0).componentHash).isEqualTo("hashPvtLog4j00000000");
  }

  // --- IT-9: Non-existent policy returns empty ---
  @Test
  public void testSearchByField_PolicyViolation_NonExistentPolicy() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    setupPolicyViolationTestData(app);
    index();

    List<SearchResultItemDTO> results = search(FieldIdentifier.POLICY_VIOLATION_POLICY_NAME,
        "\"Non-Existent Policy\"");
    assertThat(results).isEmpty();
  }

  // --- IT-10: Search by effective license ID ---
  @Test
  public void testSearchByField_EffectiveLicenseId() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    setupPolicyViolationTestData(app);
    index();

    List<SearchResultItemDTO> results = search(FieldIdentifier.COMPONENT_EFFECTIVE_LICENSE_ID,
        "\"GPL-3.0\"");
    assertThat(results).hasSize(1);
    assertThat(results.get(0).componentHash).isEqualTo("hashPvtLinux00000000");
    assertThat(results.get(0).itemType).isEqualTo(ItemType.LEGAL_VIOLATION.name());
  }

  // --- IT-11: Search by effective license name ---
  @Test
  public void testSearchByField_EffectiveLicenseName() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    setupPolicyViolationTestData(app);
    index();

    List<SearchResultItemDTO> results = search(FieldIdentifier.COMPONENT_EFFECTIVE_LICENSE_NAME,
        "\"OpenSSL\"");
    assertThat(results).hasSize(1);
    assertThat(results.get(0).componentHash).isEqualTo("hashPvtOpenssl00000");
  }

  // --- IT-12: Search by license threat group name ---
  @Test
  public void testSearchByField_LicenseThreatGroupName() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    setupPolicyViolationTestData(app);
    index();

    List<SearchResultItemDTO> results = search(FieldIdentifier.COMPONENT_LICENSE_THREAT_GROUP_NAME,
        "\"Copyleft\"");
    assertThat(results).hasSize(2);
    assertThat(results).extracting(r -> r.componentHash)
        .containsExactlyInAnyOrder("hashPvtLinux00000000", "hashPvtBusybox000000");
  }

  // --- IT-13: Search by high license threat level range ---
  @Test
  public void testSearchByField_LicenseThreatLevel_HighRange() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    setupPolicyViolationTestData(app);
    index();

    List<SearchResultItemDTO> results = search("componentLicenseThreatLevel:[7 TO 10]");
    assertThat(results).hasSize(2);
    assertThat(results).extracting(r -> r.componentHash)
        .containsExactlyInAnyOrder("hashPvtLinux00000000", "hashPvtBusybox000000");
  }

  // --- IT-14: Search by low license threat level range ---
  @Test
  public void testSearchByField_LicenseThreatLevel_LowRange() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    setupPolicyViolationTestData(app);
    index();

    List<SearchResultItemDTO> results = search("componentLicenseThreatLevel:[0 TO 2]");
    assertThat(results).hasSize(4);
    assertThat(results).extracting(r -> r.componentHash)
        .containsExactlyInAnyOrder("hashPvtLog4j00000000", "hashPvtLodash000000",
            "hashPvtDual00000000", "hashPvtDual00000000");
  }

  // --- IT-15: Combine license search with component name ---
  @Test
  public void testSearchByField_LegalViolation_CombinedWithComponentName() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    setupPolicyViolationTestData(app);
    index();

    List<SearchResultItemDTO> results = search(
        "componentLicenseThreatGroupName:\"Copyleft\" AND componentName:*linux*");
    assertThat(results).hasSize(1);
    assertThat(results.get(0).componentHash).isEqualTo("hashPvtLinux00000000");
  }

  // --- IT-16: Multiple licenses produce multiple documents ---
  @Test
  public void testSearchByField_LegalViolation_MultipleLicenses() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    setupPolicyViolationTestData(app);
    index();

    List<SearchResultItemDTO> results = search(
        "componentName:*dual-licensed* AND itemType:LEGAL_VIOLATION");
    assertThat(results).hasSize(2);
    assertThat(results).extracting(r -> r.componentEffectiveLicenseId)
        .containsExactlyInAnyOrder("MIT", "Apache-2.0");
  }

  // --- IT-17: Component with no licenses produces no LEGAL_VIOLATION docs ---
  @Test
  public void testSearchByField_LegalViolation_NoLicenses() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    setupPolicyViolationTestData(app);
    index();

    List<SearchResultItemDTO> results = search(
        "componentName:\"unknown-comp\" AND itemType:LEGAL_VIOLATION");
    assertThat(results).isEmpty();
  }

  // --- IT-18: Verify all fields on a LEGAL_VIOLATION result ---
  @Test
  public void testResultFields_LegalViolation() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    setupPolicyViolationTestData(app);
    index();

    List<SearchResultItemDTO> results = search(FieldIdentifier.COMPONENT_EFFECTIVE_LICENSE_ID,
        "\"MIT\"");
    assertThat(results).isNotEmpty();
    SearchResultItemDTO mitResult = results.stream()
        .filter(r -> "hashPvtLodash000000".equals(r.componentHash))
        .findFirst()
        .orElseThrow();
    assertLegalViolationFields(mitResult, "MIT", "MIT", "Permissive", 1);
    assertThat(mitResult.organizationId).isEqualTo(org.getId());
    assertThat(mitResult.organizationName).isEqualTo(org.getName());
    assertThat(mitResult.applicationId).isEqualTo(app.getId());
    assertThat(mitResult.applicationName).isEqualTo(app.getName());
    assertThat(mitResult.applicationPublicId).isEqualTo(app.getPublicId());
    assertThat(mitResult.componentHash).isEqualTo("hashPvtLodash000000");
    assertThat(mitResult.componentName).isNotNull();
    assertThat(mitResult.policyEvaluationStage).isNotNull();
  }

  // --- IT-19: Cross-field query (violation + vulnerability) ---
  @Test
  public void testSearchByFieldsFromDifferentDocumentTypes_ViolationAndVulnerability() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    setupPolicyViolationTestData(app);
    index();

    List<SearchResultItemDTO> pvResults = search(
        "policyViolationThreatCategory:\"security\"");
    assertThat(pvResults).extracting(r -> r.itemType)
        .containsOnly(ItemType.POLICY_VIOLATION.name());
    assertThat(pvResults).hasSize(1);

    List<SearchResultItemDTO> svResults = search(
        "vulnerabilitySeverity:[9 TO 10]");
    assertThat(svResults).extracting(r -> r.itemType)
        .containsOnly(ItemType.SECURITY_VULNERABILITY.name());
    assertThat(svResults).hasSize(1);
  }

  // --- IT-20: allComponents=false does not exclude new types ---
  @Test
  public void testSearchByField_AllComponentsFalse_DoesNotExcludeNewTypes() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    setupPolicyViolationTestData(app);
    index();

    List<SearchResultItemDTO> pvResults = search(
        "policyViolationThreatCategory:\"license\"", false);
    assertThat(pvResults).isNotEmpty();
    assertThat(pvResults).extracting(r -> r.itemType)
        .containsOnly(ItemType.POLICY_VIOLATION.name());

    List<SearchResultItemDTO> lvResults = search(
        "componentLicenseThreatGroupName:\"Copyleft\"", false);
    assertThat(lvResults).isNotEmpty();
    assertThat(lvResults).extracting(r -> r.itemType)
        .containsOnly(ItemType.LEGAL_VIOLATION.name());
  }

  // --- IT-21: Search by itemType to filter to specific document types ---
  @Test
  public void testSearchByField_ItemType_PolicyViolationAndLegalViolation() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    setupPolicyViolationTestData(app);
    index();

    List<SearchResultItemDTO> pvResults = search(FieldIdentifier.ITEM_TYPE, "POLICY_VIOLATION");
    assertThat(pvResults).isNotEmpty();
    assertThat(pvResults).allSatisfy(
        r -> assertThat(r.itemType).isEqualTo(ItemType.POLICY_VIOLATION.name()));
    assertThat(pvResults).hasSize(4);

    List<SearchResultItemDTO> lvResults = search(FieldIdentifier.ITEM_TYPE, "LEGAL_VIOLATION");
    assertThat(lvResults).isNotEmpty();
    assertThat(lvResults).allSatisfy(
        r -> assertThat(r.itemType).isEqualTo(ItemType.LEGAL_VIOLATION.name()));
    assertThat(lvResults).hasSize(7);
  }

  // --- IT-26: Incremental update rebuilds policy violation docs ---
  @Test
  public void testIncrementalUpdate_PolicyEvaluation_RebuildsPolicyViolationDocs() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation evaluation = setupPolicyViolationTestData(app);
    index();

    List<SearchResultItemDTO> initialResults = search(FieldIdentifier.ITEM_TYPE, "POLICY_VIOLATION");
    assertThat(initialResults).hasSize(4);

    Policy extraPolicy = tempEntity.newPolicy(org.getId(), "Extra - Policy");
    tempEntity.newPolicyViolation(evaluation, extraPolicy, 7, PolicyThreatCategory.OTHER,
        ComponentIdentifier.createMavenCoordinates("com.dual", "dual-licensed", "1.0"),
        "hashPvtDual00000000", null);

    try (TransactionContext tx = searchIndexChangeDAO.createTransactionContext()) {
      tx.begin();
      searchIndexChangeDAO.insert(tx,
          new SearchIndexChange(ChangeType.LAST_POLICY_EVALUATION, app.getId() + ":" + Stage.ID_BUILD));
      tx.commit();
    }
    indexChanges();

    List<SearchResultItemDTO> updatedResults = search(FieldIdentifier.ITEM_TYPE, "POLICY_VIOLATION");
    assertThat(updatedResults).hasSize(5);
  }

  // --- IT-27: Incremental update rebuilds legal violation docs ---
  @Test
  public void testIncrementalUpdate_PolicyEvaluation_RebuildsLegalViolationDocs() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation evaluation = setupPolicyViolationTestData(app);
    index();

    List<SearchResultItemDTO> initialResults = search(FieldIdentifier.ITEM_TYPE, "LEGAL_VIOLATION");
    int initialCount = initialResults.size();
    assertThat(initialCount).isGreaterThan(0);

    try (TransactionContext tx = searchIndexChangeDAO.createTransactionContext()) {
      tx.begin();
      searchIndexChangeDAO.insert(tx,
          new SearchIndexChange(ChangeType.LAST_POLICY_EVALUATION, app.getId() + ":" + Stage.ID_BUILD));
      tx.commit();
    }
    indexChanges();

    List<SearchResultItemDTO> updatedResults = search(FieldIdentifier.ITEM_TYPE, "LEGAL_VIOLATION");
    assertThat(updatedResults).hasSize(initialCount);
  }

  // --- IT-28: Incremental update does not affect SECURITY_VULNERABILITY docs ---
  @Test
  public void testIncrementalUpdate_PolicyEvaluation_ExistingDocsUnaffected() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation evaluation = setupPolicyViolationTestData(app);
    index();

    List<SearchResultItemDTO> svBefore = search(FieldIdentifier.ITEM_TYPE, "SECURITY_VULNERABILITY");
    int svCount = svBefore.size();

    try (TransactionContext tx = searchIndexChangeDAO.createTransactionContext()) {
      tx.begin();
      searchIndexChangeDAO.insert(tx,
          new SearchIndexChange(ChangeType.LAST_POLICY_EVALUATION, app.getId() + ":" + Stage.ID_BUILD));
      tx.commit();
    }
    indexChanges();

    List<SearchResultItemDTO> svAfter = search(FieldIdentifier.ITEM_TYPE, "SECURITY_VULNERABILITY");
    assertThat(svAfter).hasSize(svCount);
  }

  // --- IT-29: Full re-index includes all document types ---
  @Test
  public void testFullReindex_IncludesNewDocumentTypes() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    setupPolicyViolationTestData(app);
    index();

    List<SearchResultItemDTO> pvResults = search(FieldIdentifier.ITEM_TYPE, "POLICY_VIOLATION");
    assertThat(pvResults).isNotEmpty();

    List<SearchResultItemDTO> lvResults = search(FieldIdentifier.ITEM_TYPE, "LEGAL_VIOLATION");
    assertThat(lvResults).isNotEmpty();

    List<SearchResultItemDTO> svResults = search(FieldIdentifier.ITEM_TYPE, "SECURITY_VULNERABILITY");
    assertThat(svResults).isNotEmpty();
  }

  @BeforeClass
  public static void beforeClass() {
    System.setProperty("AdvancedSearch.createSearchIndex", "2");
    System.setProperty("AdvancedSearch.createSearchIndex.eval", "2");
    System.setProperty("AdvancedSearch.createSearchIndex.component", "2");
  }

  @AfterClass
  public static void afterClass() {
    System.clearProperty("AdvancedSearch.createSearchIndex");
    System.clearProperty("AdvancedSearch.createSearchIndex.eval");
    System.clearProperty("AdvancedSearch.createSearchIndex.component");
  }

  @Test
  public void testIndex_ThreadsAreLimited() throws Exception {
    String vulnDescription = "Remote Code Execution, you may panic now";
    when(vulnerabilityDescriptionFetcher.getVulnerabilityDescription(anyString())).thenReturn(vulnDescription);
    newAppReport(Stage.ID_BUILD, "report1");
    newAppReport(Stage.ID_BUILD, "report2");
    newAppReport(Stage.ID_BUILD, "report3");
    Set<Thread> threads = new HashSet<>();
    doAnswer(invocationOnMock -> {
      Thread.sleep(200);
      threads.add(Thread.currentThread());
      return invocationOnMock.callRealMethod();
    }).when(spyPolicyEvaluationDAO).getLastByOwnerIdAndStageId(anyString(), anyString());

    index();

    assertThat(threads).isNotEmpty();
    assertThat(threads).extracting(Thread::getName)
        .allSatisfy(name -> {
          assertThat(name).doesNotStartWith("ForkJoinPool.commonPool-worker");
          assertThat(name).doesNotEndWith("2");
        });
  }

  @Test
  public void testIndex_ExecutorsAreAddedToShutdownHandler() throws Exception {
    newAppReport(Stage.ID_RELEASE, "report-id");
    index();
    verify(mockShutdownHandler).add(((AbstractSearchIndexClient) searchIndexClient).getIndexingExecutor());
    verify(mockShutdownHandler).add(documentBuilderHelper.getEvalExecutor());
    verify(mockShutdownHandler).add(documentBuilderHelper.getComponentExecutor());
  }
}
