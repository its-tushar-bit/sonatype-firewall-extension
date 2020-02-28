/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import java.util.List;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.report.ReportTestUtils;
import com.sonatype.insight.brain.search.docs.DocumentBuilder.FieldIdentifier;
import com.sonatype.insight.brain.search.docs.DocumentBuilder.ItemType;
import com.sonatype.insight.brain.search.index.IndexService;
import com.sonatype.insight.brain.search.index.VulnerabilityDescriptionFetcher;
import com.sonatype.insight.brain.search.query.SearchService;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * End-to-end tests of indexing and auto-completion to check for desired search suggestions.
 */
public class IndexSuggestionTest
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

  private List<String> autoComplete(String query) throws Exception {
    return searchService.autoCompleteSearchQuery(query).searchResultItems;
  }

  private String field(FieldIdentifier fieldName, String value) {
    return field(fieldName.label, value);
  }

  private String field(String fieldName, String value) {
    return fieldName + ':' + value;
  }

  @Test
  public void testField_ItemType() throws Exception {
    tempEntity.newTag(tempEntity.newOrganization().getId());
    index();
    assertThat(autoComplete("application_"))
        .containsExactlyInAnyOrder(field(FieldIdentifier.ITEM_TYPE, ItemType.APPLICATION_CATEGORY.name()));
  }

  @Test
  public void testField_OrganizationId() throws Exception {
    Organization org = tempEntity.newOrganization("The AutoComplete Test");
    index();
    assertThat(autoComplete(org.getId().substring(0, 10)))
        .containsExactlyInAnyOrder(field(FieldIdentifier.ORGANIZATION_ID, org.getId()));
  }

  @Test
  public void testField_OrganizationName() throws Exception {
    Organization org = tempEntity.newOrganization("The AutoComplete Test");
    index();
    assertThat(autoComplete("autocom"))
        .containsExactlyInAnyOrder(field(FieldIdentifier.ORGANIZATION_NAME, org.getName()));
  }

  @Test
  public void testField_ApplicationId() throws Exception {
    Application app = tempEntity.newApplicationWithParent("public-id");
    index();
    assertThat(autoComplete(app.getId().substring(0, 10)))
        .containsExactlyInAnyOrder(field(FieldIdentifier.APPLICATION_ID, app.getId()));
  }

  @Test
  public void testField_ApplicationPublicId() throws Exception {
    Application app = tempEntity.newApplicationWithParent("The-AutoComplete-Test", "Some Test Name");
    index();
    assertThat(autoComplete("autocom"))
        .containsExactlyInAnyOrder(field(FieldIdentifier.APPLICATION_PUBLIC_ID, app.getPublicId()));
  }

  @Test
  public void testField_ApplicationName() throws Exception {
    Application app = tempEntity.newApplicationWithParent("public-id", "The AutoComplete Test", "Org Name");
    index();
    assertThat(autoComplete("autocom"))
        .containsExactlyInAnyOrder(field(FieldIdentifier.APPLICATION_NAME, app.getName()));
  }

  @Test
  public void testField_PolicyId() throws Exception {
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    index();
    assertThat(autoComplete(policy.getId().substring(0, 10)))
        .containsExactlyInAnyOrder(field(FieldIdentifier.POLICY_ID, policy.getId()));
  }

  @Test
  public void testField_PolicyName() throws Exception {
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "The AutoComplete Test");
    index();
    assertThat(autoComplete("autocom")).containsExactlyInAnyOrder(field(FieldIdentifier.POLICY_NAME, policy.getName()));
  }

  @Test
  public void testField_PolicyThreatCategory() throws Exception {
    Policy policy = tempEntity.newPolicy();
    index();
    assertThat(autoComplete(policy.getThreatCategory().getName().substring(0, 4)))
        .containsExactlyInAnyOrder(field(FieldIdentifier.POLICY_THREAT_CATEGORY, policy.getThreatCategory().getName()));
  }

  @Test
  public void testField_PolicyThreatLevel() throws Exception {
    tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Test Policy", 10);
    index();
    // "10" could also be a prefix of some entity id, hence not asserting only one suggestion
    assertThat(autoComplete("10")).contains(field(FieldIdentifier.POLICY_THREAT_LEVEL, "10"));
  }

  @Test
  public void testField_TagId() throws Exception {
    Tag tag = tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID);
    index();
    assertThat(autoComplete(tag.getId().substring(0, 10)))
        .containsExactlyInAnyOrder(field(FieldIdentifier.APPLICATION_CATEGORY_ID, tag.getId()));
  }

  @Test
  public void testField_TagName() throws Exception {
    Tag tag = tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID, "The AutoComplete Test");
    index();
    assertThat(autoComplete("autocom"))
        .containsExactlyInAnyOrder(field(FieldIdentifier.APPLICATION_CATEGORY_NAME, tag.getName()));
  }

  @Test
  public void testField_TagColor() throws Exception {
    Tag tag = tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID, "The AutoComplete Test", Color.dark_purple);
    index();
    assertThat(autoComplete("dark"))
        .containsExactlyInAnyOrder(field(FieldIdentifier.APPLICATION_CATEGORY_COLOR, tag.getColor().toValue()));
  }

  @Test
  public void testField_TagDescription() throws Exception {
    tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID, "Test", "The AutoComplete Test", Color.dark_red);
    index();
    assertThat(autoComplete("autocom")).isEmpty();
  }

  @Test
  public void testField_LabelId() throws Exception {
    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);
    index();
    assertThat(autoComplete(label.getId().substring(0, 10)))
        .containsExactlyInAnyOrder(field(FieldIdentifier.COMPONENT_LABEL_ID, label.getId()));
  }

  @Test
  public void testField_LabelName() throws Exception {
    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID, "The AutoComplete Test");
    index();
    assertThat(autoComplete("autocom"))
        .containsExactlyInAnyOrder(field(FieldIdentifier.COMPONENT_LABEL_NAME, label.getLabel()));
  }

  @Test
  public void testField_LabelColor() throws Exception {
    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID, "The AutoComplete Test", Color.dark_purple);
    index();
    assertThat(autoComplete("dark"))
        .containsExactlyInAnyOrder(field(FieldIdentifier.COMPONENT_LABEL_COLOR, label.getColor().toValue()));
  }

  @Test
  public void testField_LabelDescription() throws Exception {
    tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID, "Test", "The AutoComplete Test", Color.dark_red);
    index();
    assertThat(autoComplete("autocom")).isEmpty();
  }

  private PolicyEvaluation newAppReport() throws Exception {
    PolicyEvaluation policyEval = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(),
        Stage.ID_OPERATE, "report1234567890abcdef");
    ReportTestUtils.createReportFile(policyEval.getApplicationId(), policyEval.getScanId(),
        ReportTestUtils.zipReportDir("/IndexSuggestionTest/report", tempDir), insightWork);
    return policyEval;
  }

  @Test
  public void testField_PolicyEvaluationStage() throws Exception {
    newAppReport();
    index();
    assertThat(autoComplete("opera"))
        .containsExactlyInAnyOrder(field(FieldIdentifier.POLICY_EVALUATION_STAGE, StageTypes.OPERATE.getName()));
  }

  @Test
  public void testField_ReportId() throws Exception {
    String reportId = newAppReport().getScanId();
    index();
    assertThat(autoComplete(reportId.substring(0, 10)))
        .containsExactlyInAnyOrder(field(FieldIdentifier.REPORT_ID, reportId));
  }

  @Test
  public void testField_ComponentHash() throws Exception {
    newAppReport();
    index();
    assertThat(autoComplete("1234567890")) // hash with only digits
        .containsExactlyInAnyOrder(field(FieldIdentifier.COMPONENT_HASH, "12345678901234567890"));
    assertThat(autoComplete("abcdefFED")) // hash with only letters
        .containsExactlyInAnyOrder(field(FieldIdentifier.COMPONENT_HASH, "abcdefFEDCBAabcdefFE"));
  }

  @Test
  public void testField_ComponentName() throws Exception {
    newAppReport();
    index();
    assertThat(autoComplete("autocom")).containsExactlyInAnyOrder(
        field(FieldIdentifier.COMPONENT_NAME, "AutoComplete.Test 1.2.3"),
        field(FieldIdentifier.COMPONENT_COORDINATE + "PackageId", "AutoComplete.Test"));
  }

  @Test
  public void testField_ComponentFormat() throws Exception {
    newAppReport();
    index();
    assertThat(autoComplete("nug")).containsExactlyInAnyOrder(field(FieldIdentifier.COMPONENT_FORMAT, "nuget"));
  }

  @Test
  public void testField_ComponentCoordinate() throws Exception {
    newAppReport();
    index();
    assertThat(autoComplete("jar"))
        .containsExactlyInAnyOrder(field(FieldIdentifier.COMPONENT_COORDINATE + "Extension", "jar"));
  }

  @Test
  public void testField_VulnerabilityId() throws Exception {
    newAppReport();
    index();
    assertThat(autoComplete("CVE")).containsExactlyInAnyOrder(field(FieldIdentifier.VULNERABILITY_ID, "CVE-8765-1234"));
  }

  @Test
  public void testField_VulnerabilityStatus() throws Exception {
    newAppReport();
    index();
    assertThat(autoComplete("ackno"))
        .containsExactlyInAnyOrder(field(FieldIdentifier.VULNERABILITY_STATUS, "Acknowledged"));
  }

  @Test
  public void testField_VulnerabilityDescription() throws Exception {
    when(vulnerabilityDescriptionFetcher.getVulnerabilityDescription(anyString()))
        .thenReturn("Some cross-site scripting issue.");
    newAppReport();
    index();
    assertThat(autoComplete("script")).isEmpty();
  }

  @Test
  public void testNoStopWordFilter() throws Exception {
    Organization org = tempEntity.newOrganization("Thermal");
    index();
    assertThat(autoComplete("the")) // English stop word
        .containsExactlyInAnyOrder(field(FieldIdentifier.ORGANIZATION_NAME, org.getName()));
  }
}
