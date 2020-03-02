/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
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

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
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

  @Override
  public void configure(Binder binder) {
    lenient().when(vulnerabilityDescriptionFetcher.getVulnerabilityDescription(anyString())).thenReturn("");
    binder.bind(VulnerabilityDescriptionFetcher.class).toInstance(vulnerabilityDescriptionFetcher);
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

  private PolicyEvaluation newAppReport() throws Exception {
    PolicyEvaluation policyEval = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(),
        Stage.ID_OPERATE, "report1234567890abcdef");
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
    PolicyEvaluation evaluation = newAppReport();
    index();
    List<SearchResultItemDTO> results = search(FieldIdentifier.VULNERABILITY_ID, "CVE-8765-1234");
    assertThat(results).hasSize(1);
    assertVulnerability(results.get(0),
        new SecurityVulnerability("cve", "CVE-8765-1234", 4.3f, SecurityVulnerabilityOverrideStatus.ACKNOWLEDGED),
        vulnDescription, "12345678901234567890", ComponentIdentifier.createNugetCoordinates("Search.Test", "1.2.3"),
        evaluation);
  }
}
