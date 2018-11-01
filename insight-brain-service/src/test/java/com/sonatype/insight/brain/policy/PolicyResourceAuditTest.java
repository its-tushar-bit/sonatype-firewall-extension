/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.actions.WarnActionType;
import com.sonatype.insight.brain.model.policy.conditions.AgeInDaysConditionType;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.notifications.JiraNotification;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.RoleNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.tag.Tag;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class PolicyResourceAuditTest
    extends AbstractPolicyImportAuditTest
{
  private Organization organization;

  private Organization rootOrganization;

  private static final String LONG_LABEL_NAME = "thisNameIsTooLong________________________________51";

  @Before
  public void before() {
    organization = tempEntity.newOrganization();
    rootOrganization = new OrganizationDAO().getById(Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testImportPolicies() throws Exception {
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Arrays.asList(policy(), policy());
    policyExportResult.labels = Arrays.asList(label(), label(), label());
    policyExportResult.licenseThreatGroups = Collections.singletonList(licenseThreatGroup());
    policyExportResult.tags = Arrays.asList(tag(), tag(), tag(), tag());

    policyResourceRequest(organization).path("import").part("file", "file", policyExportResult)
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.IMPORT, null);
    assertOrganizationData(auditDTO, organization);
    assertPolicyImportData(auditDTO, 2, 3, 1, 4);
  }

  @Test
  public void testImportPolicies_Unauthorized() throws Exception {
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Collections.singletonList(policy());

    policyResourceRequest(organization).with(unauthorizedUser()).path("import")
        .part("file", "file", policyExportResult).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.IMPORT, "unauthorized");
    assertOrganizationData(auditDTO, organization);
    assertPolicyImportData(auditDTO, null, null, null, null);
  }

  @Test
  public void testImportPolicies_LogDeletedLicenseThreatGroups() throws Exception {
    Application application = tempEntity.newApplication(organization.getId());
    LicenseThreatGroup organizationLTG = tempEntity.newLicenseThreatGroup(organization.getId());
    LicenseThreatGroup applicationLTG = tempEntity.newLicenseThreatGroup(application.getId());
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Arrays.asList(policy());

    restRequest(organization).path("import").part("file", "file", policyExportResult).post();

    List<AuditDTO> auditDTOs = assertAuditLogs(AuditEvent.DELETE_LICENSE_THREAT_GROUP, 2, null);
    assertApplicationData(auditDTOs.get(0), application);
    assertLicenseThreatGroupData(auditDTOs.get(0), applicationLTG, (String[]) null);
    assertOrganizationData(auditDTOs.get(1), organization);
    assertLicenseThreatGroupData(auditDTOs.get(1), organizationLTG, (String[]) null);
  }

  @Test
  public void testImportPolicies_DontLogDeletedLicenseThreatGroupsIfTransactionFails() throws Exception {
    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(organization.getId());
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Arrays.asList(policy());
    policyExportResult.labels = Arrays.asList(new Label(organization.getId(), LONG_LABEL_NAME));

    restRequest(organization).path("import").part("file", "file", policyExportResult).post();

    assertAuditLog(AuditEvent.IMPORT, "bad-request");
    assertThat(awaitLogEntries(AuditEvent.DELETE_LICENSE_THREAT_GROUP, 0), empty());
    assertThat(new LicenseThreatGroupDAO().getById(ltg.getId()), is(notNullValue()));
  }

  @Test
  public void testImportPolicies_LogImportedLicenseThreatGroups() throws Exception {
    LicenseThreatGroup ltg = new LicenseThreatGroup(organization.getId(), "Test LTG", 6);
    ltg.setId(tempEntity.uuid());
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Arrays.asList(policy());
    policyExportResult.licenseThreatGroups = Arrays.asList(ltg);
    policyExportResult.licenseThreatGroupLicenses = Arrays.asList(
        new LicenseThreatGroupLicense(null, ltg.getId(), "Apache-UNSPECIFIED"),
        new LicenseThreatGroupLicense(null, ltg.getId(), "PUBLIC-DOMAIN"));

    restRequest(organization).path("import").part("file", "file", policyExportResult).post();

    assertAuditLog(AuditEvent.IMPORT, null);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.IMPORT_LICENSE_THREAT_GROUP, null);
    assertOrganizationData(auditDTO, organization);
    ltg.setId(null);
    assertLicenseThreatGroupData(auditDTO, ltg, (String[]) null);
    auditDTO = assertAuditLog(AuditEvent.CONFIGURE_LICENSE_THREAT_GROUP_LICENSES, null);
    assertLicenseThreatGroupData(auditDTO, ltg, "Apache", "Public Domain");
  }

  @Test
  public void testImportPolicies_DontLogInheritedLicenseThreatGroups() throws Exception {
    LicenseThreatGroup inheritedLTG = tempEntity.newLicenseThreatGroup(rootOrganization.getId());
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Arrays.asList(policy());
    policyExportResult.licenseThreatGroups = Arrays.asList(new LicenseThreatGroup(null, inheritedLTG.getName(), 6));

    restRequest(organization).path("import").part("file", "file", policyExportResult).post();

    assertAuditLog(AuditEvent.IMPORT, null);
    assertThat(awaitLogEntries(AuditEvent.IMPORT_LICENSE_THREAT_GROUP, 0), empty());
    assertThat(awaitLogEntries(AuditEvent.CONFIGURE_LICENSE_THREAT_GROUP_LICENSES, 0), empty());
  }

  @Test
  public void testImportPolicies_DontLogImportedLicenseThreatGroupsIfTransactionFails() throws Exception {
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Arrays.asList(policy());
    policyExportResult.licenseThreatGroups = Arrays.asList(new LicenseThreatGroup(null, "Test LTG", 6),
        new LicenseThreatGroup(null, "Test LTG", 6));

    restRequest(organization).path("import").part("file", "file", policyExportResult).post();

    assertAuditLog(AuditEvent.IMPORT, "bad-request");
    assertThat(awaitLogEntries(AuditEvent.IMPORT_LICENSE_THREAT_GROUP, 0), empty());
    assertThat(awaitLogEntries(AuditEvent.CONFIGURE_LICENSE_THREAT_GROUP_LICENSES, 0), empty());
  }

  @Test
  public void testImportPolicies_DeletesExistingPolicyWaivers() throws Exception {
    Policy policy = tempEntity.newPolicy();
    Application application = tempEntity.newApplication(organization.getId());
    PolicyWaiver rootOrganizationPolicyWaiver = savePolicyWaiver(policy.getId(), Organization.ROOT_ORGANIZATION_ID);
    PolicyWaiver organizationPolicyWaiver = savePolicyWaiver(policy.getId(), organization.getId());
    PolicyWaiver applicationPolicyWaiver = savePolicyWaiver(policy.getId(), application.getId());
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Collections.singletonList(policy());

    policyResourceRequest(rootOrganization).path("import").part("file", "file", policyExportResult).post();

    List<AuditDTO> auditDTOs = assertAuditLogs(AuditEvent.DELETE_WAIVER, 3, null);
    assertApplicationData(auditDTOs.get(0), application);
    assertDeletePolicyWaiverData(auditDTOs.get(0), policy, applicationPolicyWaiver);
    assertOrganizationData(auditDTOs.get(1), organization);
    assertDeletePolicyWaiverData(auditDTOs.get(1), policy, organizationPolicyWaiver);
    assertOrganizationData(auditDTOs.get(2), Organization.ROOT_ORGANIZATION_ID, "Root Organization");
    assertDeletePolicyWaiverData(auditDTOs.get(2), policy, rootOrganizationPolicyWaiver);
  }

  @Test
  public void testImportPolicies_DoesNotDeleteExistingPolicyWaivers_BadRequest() throws Exception {
    Policy policy = tempEntity.newPolicy();
    PolicyWaiver policyWaiver = savePolicyWaiver(policy.getId(), organization.getId());
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Collections.singletonList(policy());
    policyExportResult.labels = Collections
        .singletonList(new Label(organization.getId(), LONG_LABEL_NAME, "description", Color.yellow));

    policyResourceRequest(rootOrganization).path("import").part("file", "file", policyExportResult).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.IMPORT, "bad-request");
    assertOrganizationData(auditDTO, Organization.ROOT_ORGANIZATION_ID, "Root Organization");
    assertPolicyImportData(auditDTO, 1, 1, 0, 0);
    assertThat(new PolicyWaiverDAO().getById(policyWaiver.getId()), is(notNullValue()));
    assertThat(awaitLogEntries(AuditEvent.DELETE_WAIVER, 0), empty());
  }

  @Test
  public void testImportPolicies_ImportNewLabel() throws Exception {
    Label label = new Label(organization.getId(), "labelName", "labelDescription", Color.dark_blue);
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Arrays.asList(policy());
    policyExportResult.labels = Arrays.asList(label);
    policyResourceRequest(organization).path("import").part("file", "file", policyExportResult).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.IMPORT_LABEL, null);
    assertOrganizationData(auditDTO, organization);
    assertLabelData(auditDTO, label);
  }

  @Test
  public void testImportPolicies_ImportExistingLabel() throws Exception {
    Label existingLabel = tempEntity.newLabel(organization.getId(), "labelName", "labelDescription", Color.dark_blue);
    Label importedLabel = new Label(organization.getId(), existingLabel.getLabel(), "newLabelDescription",
        Color.dark_red);
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Arrays.asList(policy());
    policyExportResult.labels = Arrays.asList(importedLabel);

    policyResourceRequest(organization).path("import").part("file", "file", policyExportResult).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.IMPORT_LABEL, null);
    assertOrganizationData(auditDTO, organization);
    assertLabelData(auditDTO, importedLabel);
    assertCustomData(auditDTO, "labelId", existingLabel.getId());
  }

  @Test
  public void testImportPolicies_ImportLongLabel_BadRequest() throws Exception {
    Label label = new Label(organization.getId(), LONG_LABEL_NAME, "labelDescription", Color.dark_blue);
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Arrays.asList(policy());
    policyExportResult.labels = Arrays.asList(label);
    policyResourceRequest(organization).path("import").part("file", "file", policyExportResult).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.IMPORT, "bad-request");
    assertOrganizationData(auditDTO, organization);
    assertThat(awaitLogEntries(AuditEvent.IMPORT_LABEL, 0), empty());
  }

  private HttpRequest policyResourceRequest(Owner owner) {
    return restRequest().path(PolicyResource.RESOURCE_PATH).parameter(owner.getType(), owner.getPublicId());
  }

  @Test
  public void testImportPolicies_ImportsApplicationCategories() throws Exception {
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Collections.singletonList(policy());
    policyExportResult.tags = Arrays.asList(tag(), tag());
    tempEntity.newTag(organization.getId(), policyExportResult.tags.get(0).getName(), "oldDescription", Color.yellow);

    policyResourceRequest(organization).path("import").part("file", "file", policyExportResult).post();

    List<AuditDTO> auditDTOs = assertAuditLogs(AuditEvent.IMPORT_APPLICATION_CATEGORY, 2, null);
    auditDTOs.forEach(auditDTO -> assertOrganizationData(auditDTO, organization));
    assertTagData(auditDTOs.get(0), policyExportResult.tags.get(0));
    assertTagData(auditDTOs.get(1), policyExportResult.tags.get(1));
  }

  @Test
  public void testImportPolicies_DoesNotImportApplicationCategories_BadRequest() throws Exception {
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Collections.singletonList(policy());
    policyExportResult.tags = Arrays.asList(tag(), tag());
    policyExportResult.tags.get(1).setName("thisNameIsTooLong__________________________________________61");

    policyResourceRequest(organization).path("import").part("file", "file", policyExportResult).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.IMPORT, "bad-request");
    assertOrganizationData(auditDTO, organization);
    assertThat(new TagDAO().getByOrganizationId(organization.getId()), empty());
    assertThat(awaitLogEntries(AuditEvent.IMPORT_APPLICATION_CATEGORY, 0), empty());
  }

  @Test
  public void testAddPolicy_Application() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = aComplexPolicy();
    policyResourceRequest(app).body(policy).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_POLICY, null);
    assertApplicationData(auditDTO, app);
    assertPolicyData(auditDTO, policy);
  }

  @Test
  public void testAddPolicy_Organization() throws Exception {
    Policy policy = aComplexPolicy();
    policyResourceRequest(organization).body(policy).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_POLICY, null);
    assertOrganizationData(auditDTO, organization);
    assertPolicyData(auditDTO, policy);
  }

  @Test
  public void testAddPolicy_Unauthorized() throws Exception {
    Policy policy = policy();
    policyResourceRequest(organization).with(unauthorizedUser()).body(policy).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_POLICY, "unauthorized");
    assertOrganizationData(auditDTO, organization);
  }

  @Test
  public void testUpdatePolicy_Application() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    String existingPolicyId = tempEntity.newPolicy(app.getId(), tempEntity.uuid()).getId();
    Policy policy = aComplexPolicy();
    policy.setId(existingPolicyId);

    policyResourceRequest(app).body(policy).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_POLICY, null);
    assertApplicationData(auditDTO, app);
    assertPolicyData(auditDTO, policy);
  }

  @Test
  public void testUpdatePolicy_Organization() throws Exception {
    String existingPolicyId = tempEntity.newPolicy(organization.getId(), tempEntity.uuid()).getId();
    Policy policy = aComplexPolicy();
    policy.setId(existingPolicyId);

    policyResourceRequest(organization).body(policy).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_POLICY, null);
    assertOrganizationData(auditDTO, organization);
    assertPolicyData(auditDTO, policy);
  }

  @Test
  public void testUpdatePolicy_Unauthorized() throws Exception {
    String existingPolicyId = tempEntity.newPolicy(organization.getId(), UUID.randomUUID().toString()).getId();
    Policy policy = aComplexPolicy();
    policy.setId(existingPolicyId);

    policyResourceRequest(organization).with(unauthorizedUser()).body(policy).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_POLICY, "unauthorized");
    assertOrganizationData(auditDTO, organization);
  }

  private Policy aComplexPolicy() {
    Policy policy = policy();
    policy.setConstraints(Arrays.asList(
        constraint("c1", LogicalOperator.AND,
            condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"),
            condition(ConditionTypes.MatchStateConditionType.getId(), "is", "exact")),
        constraint("c2", LogicalOperator.OR,
            condition(AgeInDaysConditionType.ID, "older than", "1"),
            condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "7"))));
    policy.setAction(Stage.ID_BUILD, WarnActionType.ID);
    policy.setAction(Stage.ID_RELEASE, FailActionType.ID);
    policy.setNotifications(new Notifications(
        new UserNotification("name@email.com", Stage.ID_BUILD, Stage.ID_STAGE_RELEASE, Stage.ID_OPERATE),
        new RoleNotification(Role.DEVELOPER_ROLE_ID, Stage.ID_BUILD),
        new JiraNotification("p1", 123L, Stage.ID_DEVELOP)
    ));
    return policy;
  }

  private void assertPolicyData(final AuditDTO auditDTO, final Policy policy) {
    assertCustomData(auditDTO, "policyThreatLevel", policy.getThreatLevel());
    assertCustomData(auditDTO, "policyGrandfatheringMode",
        policy.isPolicyViolationGrandfatheringAllowed() ? "allow" : "disallow");
    assertCustomObject(auditDTO, "policyConstraints", ConstraintDTO.transcribe(policy.getConstraints()));
    assertCustomObject(auditDTO, "actions", ActionDTO.transcribe(policy.getActions()));
    assertCustomObject(auditDTO, "notifications", NotificationDTO.transcribe(policy.getNotifications()));
  }

  private PolicyWaiver savePolicyWaiver(String policyId, String ownerId) {
    return tempEntity.newWaiver("hash", policyId, ownerId, constraintFacts(), "comment");
  }

  private List<ConstraintFact> constraintFacts() {
    return Arrays.asList(
        constraintFact("constraintName1", conditionFact("summary1", "reason1"), conditionFact("summary2", "reason1"),
            conditionFact("summary3", "reason2")),
        constraintFact("constraintName2", conditionFact("summary1", "reason1"), conditionFact("summary2", "reason2")));
  }

  private ConstraintFact constraintFact(String constraintName, ConditionFact... conditionFacts) {
    return new ConstraintFact("constraintId", constraintName, "operatorName").with(conditionFacts);
  }

  private ConditionFact conditionFact(String summary, String reason) {
    return new ConditionFact("conditionTypeId", 0, summary, reason);
  }

  private void assertLicenseThreatGroupData(AuditDTO auditDTO, LicenseThreatGroup ltg, String... licenseNames) {
    if (ltg.getId() != null) {
      assertCustomData(auditDTO, "licenseThreatGroupId", ltg.getId());
    }
    else {
      assertThat(new LicenseThreatGroupDAO().getById((String) auditDTO.data.get("licenseThreatGroupId")),
          is(notNullValue()));
    }
    assertCustomData(auditDTO, "licenseThreatGroupName", ltg.getName());
    if (licenseNames == null) {
      assertCustomData(auditDTO, "licenseThreatGroupThreatLevel", ltg.getThreatLevel());
    }
    else {
      assertCustomData(auditDTO, "licenseNames", Arrays.asList(licenseNames));
    }
  }

  private void assertDeletePolicyWaiverData(AuditDTO auditDTO, Policy policy, PolicyWaiver policyWaiver) {
    assertCustomData(auditDTO, "policyId", policy.getId());
    assertCustomData(auditDTO, "policyName", policy.getName());
    assertCustomData(auditDTO, "policyWaiverId", policyWaiver.getId());
    assertCustomData(auditDTO, "comment", null);
    assertCustomData(auditDTO, "componentHash", policyWaiver.getHash());
    if (policyWaiver.getConstraintFacts() == null) {
      assertCustomData(auditDTO, "policyConstraints", null);
    }
    else {
      assertCustomObject(auditDTO, "policyConstraints",
          policyWaiver.getConstraintFacts().stream().map(ConstraintFactDTO::new).collect(Collectors.toList()));
    }
  }

  private void assertLabelData(final AuditDTO auditDTO, final Label label) {
    LabelDAO labelDAO = new LabelDAO();
    String labelId = (String) auditDTO.data.get("labelId");
    assertThat(labelDAO.getById(labelId), is(notNullValue()));
    assertCustomData(auditDTO, "labelName", label.getLabel());
    assertCustomData(auditDTO, "labelDescription", label.getDescription());
    assertCustomData(auditDTO, "labelColor", label.getColor().toValue());
  }

  private void assertTagData(AuditDTO auditDTO, Tag tag) {
    Tag savedTag = new TagDAO().getById((String) auditDTO.data.get("applicationCategoryId"));
    assertThat(savedTag, notNullValue());
    assertThat(savedTag.getName(), is(tag.getName()));
    assertThat(savedTag.getDescription(), is(tag.getDescription()));
    assertThat(savedTag.getColor(), is(tag.getColor()));
    assertCustomData(auditDTO, "applicationCategoryName", savedTag.getName());
    assertCustomData(auditDTO, "applicationCategoryDescription", savedTag.getDescription());
    assertCustomData(auditDTO, "applicationCategoryColor", savedTag.getColor().toValue());
  }

  private Condition condition(final String conditionTypeId, final String operator, final String value) {
    return new Condition(conditionTypeId, operator, value);
  }

  private Constraint constraint(final String name, final LogicalOperator logicalOperator, Condition... conditions) {
    Constraint constraint = new Constraint(UUID.randomUUID().toString(), name, logicalOperator);
    constraint.setConditions(Arrays.asList(conditions));
    return constraint;
  }
}
