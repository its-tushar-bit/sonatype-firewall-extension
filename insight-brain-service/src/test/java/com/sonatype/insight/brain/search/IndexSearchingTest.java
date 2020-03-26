/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.report.ReportTestUtils;
import com.sonatype.insight.brain.search.docs.DocumentBuilder.FieldIdentifier;
import com.sonatype.insight.brain.search.docs.DocumentBuilder.ItemType;
import com.sonatype.insight.brain.search.index.IndexService;
import com.sonatype.insight.brain.search.index.VulnerabilityDescriptionFetcher;
import com.sonatype.insight.brain.search.query.SearchService;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * End-to-end tests of indexing and searching to check queries return the desired results.
 */
public class IndexSearchingTest
    extends AbstractComponentTest
{
  @Inject
  private IndexService indexService;

  @Inject
  private SearchService searchService;

  @Inject
  private InsightWork insightWork;

  @Mock
  private VulnerabilityDescriptionFetcher vulnerabilityDescriptionFetcher;

  @Mock
  private TelemetrySender telemetrySenderMock;

  @Override
  public void configure(Binder binder) {
    lenient().when(vulnerabilityDescriptionFetcher.getVulnerabilityDescription(anyString())).thenReturn("");
    binder.bind(VulnerabilityDescriptionFetcher.class).toInstance(vulnerabilityDescriptionFetcher);
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);
    super.configure(binder);
  }

  private void index() throws Exception {
    indexService.createSearchIndex();
  }

  private List<SearchResultItemDTO> search(String query) throws Exception {
    return searchService.searchIndex(query, Integer.MAX_VALUE, 1).groupingByDTOS.stream()
        .map(groupDTO -> groupDTO.searchResultItemDTOS).flatMap(List::stream).collect(toList());
  }

  private List<SearchResultItemDTO> search(FieldIdentifier fieldIdentifier, String fieldValue) throws Exception {
    return search(fieldIdentifier + ":" + fieldValue);
  }

  private PolicyEvaluation newAppReport(String stageId, String reportId) throws Exception {
    PolicyEvaluation policyEval =
        tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), stageId, reportId);
    ReportTestUtils.createReportFile(policyEval.getApplicationId(), policyEval.getScanId(),
        ReportTestUtils.zipReportDir("/IndexSearchingTest/report", tempDir), insightWork);
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
    assertApplicationData(result, new ApplicationDAO().getById(evaluation.getApplicationId()));
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
  public void testSearchByField_UnknownField() throws Exception {
    index();
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      search("unknownField:foobar");
    }).withMessage("The search query contains invalid field names: [unknownField]");
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
        .extracting(dto -> dto.organizationId).containsExactlyInAnyOrder(org.getId());
    assertThat(search(FieldIdentifier.ORGANIZATION_ID, org.getId().toUpperCase(Locale.ROOT)))
        .extracting(dto -> dto.organizationId).containsExactlyInAnyOrder(org.getId());
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
        .extracting(dto -> dto.applicationId).containsExactlyInAnyOrder(app.getId());
    assertThat(search(FieldIdentifier.APPLICATION_ID, app.getId().toUpperCase(Locale.ROOT)))
        .extracting(dto -> dto.applicationId).containsExactlyInAnyOrder(app.getId());
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
  public void testSearchByField_ApplicationCategoryId() throws Exception {
    Tag tag = tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID);
    index();
    assertThat(search(FieldIdentifier.APPLICATION_CATEGORY_ID, tag.getId().toLowerCase(Locale.ROOT)))
        .extracting(dto -> dto.applicationCategoryId).containsExactlyInAnyOrder(tag.getId());
    assertThat(search(FieldIdentifier.APPLICATION_CATEGORY_ID, tag.getId().toUpperCase(Locale.ROOT)))
        .extracting(dto -> dto.applicationCategoryId).containsExactlyInAnyOrder(tag.getId());
  }

  @Test
  public void testSearchByField_ApplicationCategoryName() throws Exception {
    Tag tag = tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID, "Search Test");
    tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID, "Search Test 2");
    index();
    assertThat(search(FieldIdentifier.APPLICATION_CATEGORY_NAME, "\"search TEST\""))
        .extracting(dto -> dto.applicationCategoryId).containsExactlyInAnyOrder(tag.getId());
    assertThat(search(FieldIdentifier.APPLICATION_CATEGORY_NAME, "seaRCH")).isEmpty();
  }

  @Test
  public void testSearchByField_ApplicationCategoryColor() throws Exception {
    Tag tag = tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID, "Search Test", Color.dark_red);
    tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID, "Search Test 2", Color.dark_blue);
    index();
    assertThat(search(FieldIdentifier.APPLICATION_CATEGORY_COLOR, "DARK-red"))
        .extracting(dto -> dto.applicationCategoryId).containsExactlyInAnyOrder(tag.getId());
    assertThat(search(FieldIdentifier.APPLICATION_CATEGORY_COLOR, "daRK")).isEmpty();
  }

  @Test
  public void testSearchByField_ApplicationCategoryDescription() throws Exception {
    Tag tag1 = tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID, "Category 1", "Search Test", Color.dark_red);
    Tag tag2 = tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID, "Category 2", "Search Testing 2", Color.dark_blue);
    index();
    assertThat(search(FieldIdentifier.APPLICATION_CATEGORY_DESCRIPTION, "\"search TEST\""))
        .extracting(dto -> dto.applicationCategoryId).containsExactlyInAnyOrder(tag1.getId());
    assertThat(search(FieldIdentifier.APPLICATION_CATEGORY_DESCRIPTION, "seaRCH"))
        .extracting(dto -> dto.applicationCategoryId).containsExactlyInAnyOrder(tag1.getId(), tag2.getId());
  }

  @Test
  public void testSearchByField_ComponentLabelId() throws Exception {
    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);
    index();
    assertThat(search(FieldIdentifier.COMPONENT_LABEL_ID, label.getId().toLowerCase(Locale.ROOT)))
        .extracting(dto -> dto.componentLabelId).containsExactlyInAnyOrder(label.getId());
    assertThat(search(FieldIdentifier.COMPONENT_LABEL_ID, label.getId().toUpperCase(Locale.ROOT)))
        .extracting(dto -> dto.componentLabelId).containsExactlyInAnyOrder(label.getId());
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
        .extracting(dto -> dto.componentLabelId).containsExactlyInAnyOrder(label1.getId());
    assertThat(search(FieldIdentifier.COMPONENT_LABEL_DESCRIPTION, "seaRCH")).extracting(dto -> dto.componentLabelId)
        .containsExactlyInAnyOrder(label1.getId(), label2.getId());
  }

  @Test
  public void testSearchByField_PolicyId() throws Exception {
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    index();
    assertThat(search(FieldIdentifier.POLICY_ID, policy.getId().toLowerCase(Locale.ROOT)))
        .extracting(dto -> dto.policyId).containsExactlyInAnyOrder(policy.getId());
    assertThat(search(FieldIdentifier.POLICY_ID, policy.getId().toUpperCase(Locale.ROOT)))
        .extracting(dto -> dto.policyId).containsExactlyInAnyOrder(policy.getId());
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
    assertThat(search(FieldIdentifier.POLICY_EVALUATION_STAGE, "\"STAGE release\"")).extracting(dto -> dto.reportId)
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
  public void testSearchByField_ComponentCoordinate() throws Exception {
    newAppReport(Stage.ID_RELEASE, "report-id");
    index();
    assertThat(search(FieldIdentifier.COMPONENT_COORDINATE + "PackageId:search.TEST"))
        .extracting(dto -> dto.componentHash).containsExactlyInAnyOrder("1234567890abcdeABCDE");
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
        .extracting(dto -> dto.vulnerabilityId).containsOnly("CVE-8765-1234");
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
    String appId = newAppReport(Stage.ID_BUILD, "report-0").getApplicationId();
    newAppReport(Stage.ID_BUILD, "report-1");
    index();
    assertThat(search("CVE-8765-1234 AND " + FieldIdentifier.APPLICATION_ID + ":" + appId))
        .extracting(dto -> dto.reportId).containsExactlyInAnyOrder("report-0");
  }

  @Test
  public void testSearchByFieldsFromDifferentDocumentTypes() throws Exception {
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    Tag tag = tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID);
    index();
    assertThat(search(FieldIdentifier.POLICY_ID + ":" + policy.getId() + " " + FieldIdentifier.APPLICATION_CATEGORY_ID
        + ":" + tag.getId())).extracting(dto -> dto.itemType).containsExactlyInAnyOrder(ItemType.POLICY.name(),
            ItemType.APPLICATION_CATEGORY.name());
  }
}
