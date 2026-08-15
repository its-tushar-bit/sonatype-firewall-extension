/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.audit.ApplicationCategoryAuditDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
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
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.notifications.JiraNotification;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.RoleNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.test.LogOutput;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.policy.PolicyResource.NOTIFICATIONS_PATH;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kept in the {@code com.sonatype.insight.brain.policy} package to access
 * {@link PolicyResource#NOTIFICATIONS_PATH}, which is package-private; reproduces the {@code AbstractAuditTest} /
 * {@code AbstractPolicyImportAuditTest} scaffolding (log capture + unauthorized-user helper + policy/label/tag/
 * license-threat-group fixture helpers) that the legacy {@code PolicyResourceAuditTest} inherited from its base
 * classes.
 */
@IqH2Test
class IqH2PolicyResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private PolicyDAO policyDAO;

  private RoleDAO roleDAO;

  private LicenseThreatGroupDAO licenseThreatGroupDAO;

  private PolicyWaiverDAO policyWaiverDAO;

  private TagDAO tagDAO;

  private LabelDAO labelDAO;

  private Organization organization;

  private Organization rootOrganization;

  private User unauthorizedUser;

  private final TestLogOutput logOutput = new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  private static final String LONG_LABEL_NAME = "thisNameIsTooLong________________________________51";

  @BeforeEach
  void before() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();

    roleDAO = ctx.lookup(RoleDAO.class);
    policyDAO = ctx.lookup(PolicyDAO.class);

    licenseThreatGroupDAO = ctx.lookup(LicenseThreatGroupDAO.class);
    policyWaiverDAO = ctx.lookup(PolicyWaiverDAO.class);
    tagDAO = ctx.lookup(TagDAO.class);
    labelDAO = ctx.lookup(LabelDAO.class);

    organization = ctx.tempEntity().newOrganization();
    rootOrganization = ctx.lookup(OrganizationDAO.class).getById(Organization.ROOT_ORGANIZATION_ID);
  }

  @AfterEach
  void after() {
    logOutput.tearDown();
  }

  @Override
  public LogOutput getLogOutput() {
    return logOutput;
  }

  @Override
  public String getUnauthorizedUsername() {
    return unauthorizedUser.getUsername();
  }

  @Override
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  private Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUser);
  }

  // --- folded from AbstractPolicyImportAuditTest -------------------------------------------------

  private Policy policy() {
    Policy policy = new Policy();
    policy.setId(TemporaryEntity.uuid());
    policy.setName(TemporaryEntity.uuid());
    policy.setPolicyActionsOverrideAllowed(true);
    Constraint constraint = new Constraint();
    constraint.setName("constraintName");
    Condition condition = new Condition(ConditionTypes.MatchStateConditionType.getId(), "is", "exact");
    constraint.setConditions(Collections.singletonList(condition));
    policy.setConstraints(Collections.singletonList(constraint));
    return policy;
  }

  private Policy aComplexPolicy() {
    Policy policy = policy();
    policy.setConstraints(new ArrayList<>(Arrays.asList(
        constraint("c1", LogicalOperator.AND,
            condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"),
            condition(ConditionTypes.MatchStateConditionType.getId(), "is", "exact")),
        constraint("c2", LogicalOperator.OR,
            condition(AgeInDaysConditionType.ID, "older than", "1"),
            condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "7")))));
    policy.setAction(Stage.ID_BUILD, WarnActionType.ID);
    policy.setAction(Stage.ID_RELEASE, FailActionType.ID);
    Role role = roleDAO.getById(Role.DEVELOPER_ROLE_ID);
    policy.setNotifications(new Notifications(
        new UserNotification("name@email.com", Stage.ID_BUILD, Stage.ID_STAGE_RELEASE, Stage.ID_OPERATE),
        new RoleNotification(role.getId(), role.getName(), Stage.ID_BUILD),
        new JiraNotification("p1", 123L, Stage.ID_DEVELOP)));
    return policy;
  }

  private Label label() {
    Label label = new Label();
    label.setId(TemporaryEntity.uuid());
    label.setLabel(TemporaryEntity.uuid());
    return label;
  }

  private LicenseThreatGroup licenseThreatGroup() {
    LicenseThreatGroup licenseThreatGroup = new LicenseThreatGroup();
    licenseThreatGroup.setId(TemporaryEntity.uuid());
    licenseThreatGroup.setName(TemporaryEntity.uuid());
    return licenseThreatGroup;
  }

  private Tag tag() {
    Tag tag = new Tag();
    tag.setId(TemporaryEntity.uuid());
    tag.setName(TemporaryEntity.uuid());
    tag.setDescription("tagDescription");
    return tag;
  }

  private void assertPolicyImportData(
      AuditDTO auditDTO,
      Integer policyCount,
      Integer componentLabelCount,
      Integer licenseThreatGroupCount,
      Integer applicationCategoryCount)
  {
    assertCustomData(auditDTO, "policyCount", policyCount);
    assertCustomData(auditDTO, "componentLabelCount", componentLabelCount);
    assertCustomData(auditDTO, "licenseThreatGroupCount", licenseThreatGroupCount);
    assertCustomData(auditDTO, "applicationCategoryCount", applicationCategoryCount);
  }

  private void assertPolicyOverrideData(
      final AuditDTO auditDTO,
      final Policy policy,
      final String overridingOwnerId,
      boolean actionsOverrideAdded,
      boolean notificationsOverrideAdded)
  {
    String auditedPolicyId = (String) auditDTO.data.get("policyId");
    String auditedOverridingOwnerId = (String) auditDTO.data.get("overridingOwnerId");

    assertThat(auditedPolicyId).isNotNull();
    assertThat(auditedOverridingOwnerId).isNotNull().isEqualTo(overridingOwnerId);
    assertThat(auditedPolicyId).isEqualTo(policy.getId());
    assertThat(policyDAO.getById(auditedPolicyId)).isNotNull();
    if (actionsOverrideAdded) {
      assertThat(auditDTO.data.get("actionsOverride")).isNotNull();
    }
    if (notificationsOverrideAdded) {
      assertThat(auditDTO.data.get("notificationsOverride")).isNotNull();
    }
  }

  private void assertImportedPolicies(
      final List<Policy> policies,
      String organizationId,
      String organizationName,
      String username)
  {
    List<AuditDTO> auditLogs = assertAuditLogs(AuditEvent.IMPORT_POLICY, policies.size(), null, username);
    for (int i = 0; i < policies.size(); i++) {
      assertOrganizationData(auditLogs.get(i), organizationId, organizationName);
      assertPolicyData(auditLogs.get(i), policies.get(i), false);
    }
  }

  private Condition condition(final String conditionTypeId, final String operator, final String value) {
    return new Condition(conditionTypeId, operator, value);
  }

  private Constraint constraint(final String name, final LogicalOperator logicalOperator, Condition... conditions) {
    Constraint constraint = new Constraint(TemporaryEntity.uuid(), name, logicalOperator);
    constraint.setConditions(Arrays.asList(conditions));
    return constraint;
  }

  // --- tests --------------------------------------------------------------------------------------

  @Test
  void testImportPolicies() throws Exception {
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Arrays.asList(policy(), policy());
    policyExportResult.labels = Arrays.asList(label(), label(), label());
    policyExportResult.licenseThreatGroups = Collections.singletonList(licenseThreatGroup());
    policyExportResult.tags = Arrays.asList(tag(), tag(), tag(), tag());

    policyResourceRequest(organization).path("import")
        .part("file", "file", policyExportResult)
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.IMPORT, null);
    assertOrganizationData(auditDTO, organization);
    assertPolicyImportData(auditDTO, 2, 3, 1, 4);
  }

  @Test
  void testImportPolicies_Unauthorized() throws Exception {
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Collections.singletonList(policy());

    policyResourceRequest(organization).with(unauthorizedUser())
        .path("import")
        .part("file", "file", policyExportResult)
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.IMPORT, "unauthorized");
    assertOrganizationData(auditDTO, organization);
    assertPolicyImportData(auditDTO, null, null, null, null);
  }

  @Test
  void testImportPolicies_LogDeletedLicenseThreatGroups() throws Exception {
    Application application = ctx.tempEntity().newApplication(organization.getId());
    LicenseThreatGroup organizationLTG = ctx.tempEntity().newLicenseThreatGroup(organization.getId());
    LicenseThreatGroup applicationLTG = ctx.tempEntity().newLicenseThreatGroup(application.getId());
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Collections.singletonList(policy());

    policyResourceRequest(organization).path("import").part("file", "file", policyExportResult).post();

    List<AuditDTO> auditDTOs = assertAuditLogs(AuditEvent.DELETE_LICENSE_THREAT_GROUP, 2, null);
    assertApplicationData(auditDTOs.get(0), application);
    assertLicenseThreatGroupData(auditDTOs.get(0), applicationLTG, (String[]) null);
    assertOrganizationData(auditDTOs.get(1), organization);
    assertLicenseThreatGroupData(auditDTOs.get(1), organizationLTG, (String[]) null);
  }

  @Test
  void testImportPolicies_DontLogDeletedLicenseThreatGroupsIfTransactionFails() throws Exception {
    LicenseThreatGroup ltg = ctx.tempEntity().newLicenseThreatGroup(organization.getId());
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Collections.singletonList(policy());
    policyExportResult.labels = Collections.singletonList(new Label(organization.getId(), LONG_LABEL_NAME));

    policyResourceRequest(organization).path("import").part("file", "file", policyExportResult).post();

    assertAuditLog(AuditEvent.IMPORT, "bad-request");
    assertThat(awaitLogEntries(AuditEvent.DELETE_LICENSE_THREAT_GROUP, 0)).isEmpty();
    assertThat(licenseThreatGroupDAO.getById(ltg.getId())).isNotNull();
  }

  @Test
  void testImportPolicies_LogImportedLicenseThreatGroups() throws Exception {
    LicenseThreatGroup ltg = new LicenseThreatGroup(organization.getId(), "Test LTG", 6);
    ltg.setId(TemporaryEntity.uuid());
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Collections.singletonList(policy());
    policyExportResult.licenseThreatGroups = Collections.singletonList(ltg);
    policyExportResult.licenseThreatGroupLicenses = Arrays.asList(
        new LicenseThreatGroupLicense(null, ltg.getId(), "Apache-UNSPECIFIED"),
        new LicenseThreatGroupLicense(null, ltg.getId(), "PUBLIC-DOMAIN"));

    policyResourceRequest(organization).path("import").part("file", "file", policyExportResult).post();

    assertAuditLog(AuditEvent.IMPORT, null);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.IMPORT_LICENSE_THREAT_GROUP, null);
    assertOrganizationData(auditDTO, organization);
    ltg.setId(null);
    assertLicenseThreatGroupData(auditDTO, ltg, (String[]) null);
    auditDTO = assertAuditLog(AuditEvent.CONFIGURE_LICENSE_THREAT_GROUP_LICENSES, null);
    assertLicenseThreatGroupData(auditDTO, ltg, "Apache", "Public Domain");
  }

  @Test
  void testImportPolicies_DontLogInheritedLicenseThreatGroups() throws Exception {
    LicenseThreatGroup inheritedLTG = ctx.tempEntity().newLicenseThreatGroup(rootOrganization.getId());
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Collections.singletonList(policy());
    policyExportResult.licenseThreatGroups =
        Collections.singletonList(new LicenseThreatGroup(null, inheritedLTG.getName(), 6));

    policyResourceRequest(organization).path("import").part("file", "file", policyExportResult).post();

    assertAuditLog(AuditEvent.IMPORT, null);
    assertThat(awaitLogEntries(AuditEvent.IMPORT_LICENSE_THREAT_GROUP, 0)).isEmpty();
    assertThat(awaitLogEntries(AuditEvent.CONFIGURE_LICENSE_THREAT_GROUP_LICENSES, 0)).isEmpty();
  }

  @Test
  void testImportPolicies_DontLogImportedLicenseThreatGroupsIfTransactionFails() throws Exception {
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Collections.singletonList(policy());
    policyExportResult.licenseThreatGroups = Arrays.asList(new LicenseThreatGroup(null, "Test LTG", 6),
        new LicenseThreatGroup(null, "Test LTG", 6));

    policyResourceRequest(organization).path("import").part("file", "file", policyExportResult).post();

    assertAuditLog(AuditEvent.IMPORT, "bad-request");
    assertThat(awaitLogEntries(AuditEvent.IMPORT_LICENSE_THREAT_GROUP, 0)).isEmpty();
    assertThat(awaitLogEntries(AuditEvent.CONFIGURE_LICENSE_THREAT_GROUP_LICENSES, 0)).isEmpty();
  }

  @Test
  void testImportPolicies_DeletesExistingPolicyWaivers() throws Exception {
    Policy policy = ctx.tempEntity().newPolicy();
    Application application = ctx.tempEntity().newApplication(organization.getId());
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
  void testImportPolicies_DoesNotDeleteExistingPolicyWaivers_BadRequest() throws Exception {
    Policy policy = ctx.tempEntity().newPolicy();
    PolicyWaiver policyWaiver = savePolicyWaiver(policy.getId(), organization.getId());
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Collections.singletonList(policy());
    policyExportResult.labels = Collections
        .singletonList(new Label(organization.getId(), LONG_LABEL_NAME, "description", Color.yellow));

    policyResourceRequest(rootOrganization).path("import").part("file", "file", policyExportResult).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.IMPORT, "bad-request");
    assertOrganizationData(auditDTO, Organization.ROOT_ORGANIZATION_ID, "Root Organization");
    assertPolicyImportData(auditDTO, 1, 1, 0, 0);
    assertThat(policyWaiverDAO.getById(policyWaiver.getId())).isNotNull();
    assertThat(awaitLogEntries(AuditEvent.DELETE_WAIVER, 0)).isEmpty();
  }

  @Test
  void testImportPolicies_ImportNewLabel() throws Exception {
    Label label = new Label(organization.getId(), "labelName", "labelDescription", Color.dark_blue);
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Collections.singletonList(policy());
    policyExportResult.labels = Collections.singletonList(label);
    policyResourceRequest(organization).path("import").part("file", "file", policyExportResult).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.IMPORT_LABEL, null);
    assertOrganizationData(auditDTO, organization);
    assertLabelData(auditDTO, label);
  }

  @Test
  void testImportPolicies_ImportExistingLabel() throws Exception {
    Label existingLabel =
        ctx.tempEntity().newLabel(organization.getId(), "labelName", "labelDescription", Color.dark_blue);
    Label importedLabel = new Label(organization.getId(), existingLabel.getLabel(), "newLabelDescription",
        Color.dark_red);
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Collections.singletonList(policy());
    policyExportResult.labels = Collections.singletonList(importedLabel);

    policyResourceRequest(organization).path("import").part("file", "file", policyExportResult).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.IMPORT_LABEL, null);
    assertOrganizationData(auditDTO, organization);
    assertLabelData(auditDTO, importedLabel);
    assertCustomData(auditDTO, "labelId", existingLabel.getId());
  }

  @Test
  void testImportPolicies_ImportLongLabel_BadRequest() throws Exception {
    Label label = new Label(organization.getId(), LONG_LABEL_NAME, "labelDescription", Color.dark_blue);
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Collections.singletonList(policy());
    policyExportResult.labels = Collections.singletonList(label);
    policyResourceRequest(organization).path("import").part("file", "file", policyExportResult).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.IMPORT, "bad-request");
    assertOrganizationData(auditDTO, organization);
    assertThat(awaitLogEntries(AuditEvent.IMPORT_LABEL, 0)).isEmpty();
  }

  private HttpRequest policyResourceRequest(Owner owner) {
    return ctx.restRequest().path(PolicyResource.RESOURCE_PATH).parameter(owner.getType(), owner.getPublicId());
  }

  @Test
  void testImportPolicies_ImportsApplicationCategories() throws Exception {
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Collections.singletonList(policy());
    policyExportResult.tags = Arrays.asList(tag(), tag());
    ctx.tempEntity()
        .newTag(organization.getId(), policyExportResult.tags.get(0).getName(), "oldDescription", Color.yellow);

    policyResourceRequest(organization).path("import").part("file", "file", policyExportResult).post();

    List<AuditDTO> auditDTOs = assertAuditLogs(AuditEvent.IMPORT_APPLICATION_CATEGORY, 2, null);
    auditDTOs.forEach(auditDTO -> assertOrganizationData(auditDTO, organization));
    assertTagData(auditDTOs.get(0), policyExportResult.tags.get(0));
    assertTagData(auditDTOs.get(1), policyExportResult.tags.get(1));
  }

  @Test
  void testImportPolicies_DoesNotImportApplicationCategories_BadRequest() throws Exception {
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Collections.singletonList(policy());
    policyExportResult.tags = Arrays.asList(tag(), tag());
    policyExportResult.tags.get(1).setName("thisNameIsTooLong__________________________________________61");

    policyResourceRequest(organization).path("import").part("file", "file", policyExportResult).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.IMPORT, "bad-request");
    assertOrganizationData(auditDTO, organization);
    assertThat(tagDAO.getByOrganizationId(organization.getId())).isEmpty();
    assertThat(awaitLogEntries(AuditEvent.IMPORT_APPLICATION_CATEGORY, 0)).isEmpty();
  }

  @Test
  void testAddPolicy_Application() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    Policy policy = aComplexPolicy();
    HttpResponse response = policyResourceRequest(app).body(policy).post();
    ctx.assertResponseStatus(200, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_POLICY, null);
    assertApplicationData(auditDTO, app);
    assertPolicyData(auditDTO, policy, false);
  }

  @Test
  void testAddPolicy_Organization() throws Exception {
    Policy policy = aComplexPolicy();
    HttpResponse response = policyResourceRequest(organization).body(policy).post();
    ctx.assertResponseStatus(200, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_POLICY, null);
    assertOrganizationData(auditDTO, organization);
    assertPolicyData(auditDTO, policy, false);
  }

  @Test
  void testAddPolicy_RepositoryContainer() throws Exception {
    Policy policy = aComplexPolicy();

    HttpResponse response = policyResourceRequest(RepositoryContainer.SINGLETON).body(policy).post();
    ctx.assertResponseStatus(200, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_POLICY, null);
    assertRepositoryContainerData(auditDTO);
    assertPolicyData(auditDTO, policy, false);
  }

  @Test
  void testAddPolicy_RepositoryManager() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Policy policy = aComplexPolicy();

    HttpResponse response = policyResourceRequest(repositoryManager).body(policy).post();
    ctx.assertResponseStatus(200, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_POLICY, null);
    assertRepositoryManagerData(auditDTO, repositoryManager);
    assertPolicyData(auditDTO, policy, false);
  }

  @Test
  void testAddPolicy_Repository() throws Exception {
    Repository repository = ctx.tempEntity().newRepository();
    Policy policy = aComplexPolicy();

    HttpResponse response =
        ctx.restRequest()
            .path(PolicyResource.RESOURCE_PATH)
            .parameter(repository.getType(), repository.getId())
            .body(policy)
            .post();
    ctx.assertResponseStatus(200, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_POLICY, null);
    assertCustomData(auditDTO, "repositoryId", repository.getId());
    assertPolicyData(auditDTO, policy, false);
  }

  @Test
  void testAddPolicy_Unauthorized() throws Exception {
    Policy policy = policy();
    policyResourceRequest(organization).with(unauthorizedUser()).body(policy).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_POLICY, "unauthorized");
    assertOrganizationData(auditDTO, organization);
  }

  @Test
  void testUpdatePolicy_Application() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    String existingPolicyId = ctx.tempEntity().newPolicy(app.getId(), TemporaryEntity.uuid()).getId();
    Policy policy = aComplexPolicy();
    policy.setId(existingPolicyId);

    policyResourceRequest(app).body(policy).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_POLICY, null);
    assertApplicationData(auditDTO, app);
    assertPolicyData(auditDTO, policy, false);
  }

  @Test
  void testUpdatePolicy_Organization() throws Exception {
    String existingPolicyId = ctx.tempEntity().newPolicy(organization.getId(), TemporaryEntity.uuid()).getId();
    Policy policy = aComplexPolicy();
    policy.setId(existingPolicyId);

    policyResourceRequest(organization).body(policy).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_POLICY, null);
    assertOrganizationData(auditDTO, organization);
    assertPolicyData(auditDTO, policy, false);
  }

  @Test
  void testUpdatePolicy_RepositoryContainer() throws Exception {
    String existingPolicyId =
        ctx.tempEntity().newPolicy(RepositoryContainer.REPOSITORY_CONTAINER_ID, TemporaryEntity.uuid()).getId();
    Policy policy = aComplexPolicy();
    policy.setId(existingPolicyId);

    policyResourceRequest(RepositoryContainer.SINGLETON).body(policy).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_POLICY, null);
    assertRepositoryContainerData(auditDTO);
    assertPolicyData(auditDTO, policy, false);
  }

  @Test
  void testUpdatePolicy_RepositoryManager() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    String existingPolicyId = ctx.tempEntity().newPolicy(repositoryManager.getId(), TemporaryEntity.uuid()).getId();

    Policy policy = aComplexPolicy();
    policy.setId(existingPolicyId);

    policyResourceRequest(repositoryManager).body(policy).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_POLICY, null);
    assertRepositoryManagerData(auditDTO, repositoryManager);
    assertPolicyData(auditDTO, policy, false);
  }

  @Test
  void testUpdatePolicy_Repository() throws Exception {
    Repository repository = ctx.tempEntity().newRepository();
    String existingPolicyId = ctx.tempEntity().newPolicy(repository.getId(), TemporaryEntity.uuid()).getId();

    Policy policy = aComplexPolicy();
    policy.setId(existingPolicyId);

    ctx.restRequest()
        .path(PolicyResource.RESOURCE_PATH)
        .parameter(repository.getType(), repository.getId())
        .body(policy)
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_POLICY, null);
    assertCustomData(auditDTO, "repositoryId", repository.getId());
    assertPolicyData(auditDTO, policy, false);
  }

  @Test
  void testUpdatePolicy_Unauthorized() throws Exception {
    String existingPolicyId = ctx.tempEntity().newPolicy(organization.getId(), UUID.randomUUID().toString()).getId();
    Policy policy = aComplexPolicy();
    policy.setId(existingPolicyId);

    policyResourceRequest(organization).with(unauthorizedUser()).body(policy).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_POLICY, "unauthorized");
    assertOrganizationData(auditDTO, organization);
  }

  @Test
  void updatePolicyNotifications_Application() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    Policy existingPolicy = ctx.tempEntity().newPolicy(app.getId(), TemporaryEntity.uuid());
    Policy policy = aComplexPolicy();
    policy.setId(existingPolicy.getId());

    policyResourceRequest(app).path(NOTIFICATIONS_PATH).body(policy).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_POLICY, null);
    assertApplicationData(auditDTO, app);

    Policy expectedPolicy = existingPolicy;
    expectedPolicy.setPolicyNotificationsOverrideAllowed(policy.isPolicyNotificationsOverrideAllowed());
    expectedPolicy.setPolicyNotificationsOverrides(policy.getPolicyNotificationsOverrides());
    expectedPolicy.setNotifications(policy.getNotifications());

    assertPolicyData(auditDTO, expectedPolicy, false);
  }

  @Test
  void updatePolicyNotifications_Organization() throws Exception {
    Policy existingPolicy = ctx.tempEntity().newPolicy(organization.getId(), TemporaryEntity.uuid());
    Policy policy = aComplexPolicy();
    policy.setId(existingPolicy.getId());

    policyResourceRequest(organization).path(NOTIFICATIONS_PATH).body(policy).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_POLICY, null);
    assertOrganizationData(auditDTO, organization);

    Policy expectedPolicy = existingPolicy;
    expectedPolicy.setPolicyNotificationsOverrideAllowed(policy.isPolicyNotificationsOverrideAllowed());
    expectedPolicy.setPolicyNotificationsOverrides(policy.getPolicyNotificationsOverrides());
    expectedPolicy.setNotifications(policy.getNotifications());

    assertPolicyData(auditDTO, expectedPolicy, false);
  }

  @Test
  void testUpdatePolicyNotifications_RepositoryContainer() throws Exception {
    Policy existingPolicy =
        ctx.tempEntity().newPolicy(RepositoryContainer.REPOSITORY_CONTAINER_ID, TemporaryEntity.uuid());
    Policy policy = aComplexPolicy();
    policy.setId(existingPolicy.getId());

    policyResourceRequest(RepositoryContainer.SINGLETON).path(NOTIFICATIONS_PATH).body(policy).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_POLICY, null);
    assertRepositoryContainerData(auditDTO);

    Policy expectedPolicy = existingPolicy;
    expectedPolicy.setPolicyNotificationsOverrideAllowed(policy.isPolicyNotificationsOverrideAllowed());
    expectedPolicy.setPolicyNotificationsOverrides(policy.getPolicyNotificationsOverrides());
    expectedPolicy.setNotifications(policy.getNotifications());

    assertPolicyData(auditDTO, expectedPolicy, false);
  }

  @Test
  void testUpdatePolicyNotifications_RepositoryManager() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Policy existingPolicy = ctx.tempEntity().newPolicy(repositoryManager.getId(), TemporaryEntity.uuid());

    Policy policy = aComplexPolicy();
    policy.setId(existingPolicy.getId());

    policyResourceRequest(repositoryManager).path(NOTIFICATIONS_PATH).body(policy).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_POLICY, null);
    assertRepositoryManagerData(auditDTO, repositoryManager);

    Policy expectedPolicy = existingPolicy;
    expectedPolicy.setPolicyNotificationsOverrideAllowed(policy.isPolicyNotificationsOverrideAllowed());
    expectedPolicy.setPolicyNotificationsOverrides(policy.getPolicyNotificationsOverrides());
    expectedPolicy.setNotifications(policy.getNotifications());

    assertPolicyData(auditDTO, expectedPolicy, false);
  }

  @Test
  void testUpdatePolicyNotifications_Repository() throws Exception {
    Repository repository = ctx.tempEntity().newRepository();
    Policy existingPolicy = ctx.tempEntity().newPolicy(repository.getId(), TemporaryEntity.uuid());

    Policy policy = aComplexPolicy();
    policy.setId(existingPolicy.getId());

    ctx.restRequest()
        .path(PolicyResource.RESOURCE_PATH)
        .path(NOTIFICATIONS_PATH)
        .parameter(repository.getType(), repository.getId())
        .body(policy)
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_POLICY, null);
    assertCustomData(auditDTO, "repositoryId", repository.getId());

    Policy expectedPolicy = existingPolicy;
    expectedPolicy.setPolicyNotificationsOverrideAllowed(policy.isPolicyNotificationsOverrideAllowed());
    expectedPolicy.setPolicyNotificationsOverrides(policy.getPolicyNotificationsOverrides());
    expectedPolicy.setNotifications(policy.getNotifications());

    assertPolicyData(auditDTO, expectedPolicy, false);
  }

  @Test
  void updatePolicyNotifications_Unauthorized() throws Exception {
    String existingPolicyId = ctx.tempEntity().newPolicy(organization.getId(), UUID.randomUUID().toString()).getId();
    Policy policy = aComplexPolicy();
    policy.setId(existingPolicyId);

    policyResourceRequest(organization).path(PolicyResource.NOTIFICATIONS_PATH)
        .with(unauthorizedUser())
        .body(policy)
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_POLICY, "unauthorized");
    assertOrganizationData(auditDTO, organization);
  }

  @Test
  void testDeletePolicy_Application() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    Policy policy = aComplexPolicy();
    policy.setOwnerId(app.getId());
    ctx.tempEntity().newPolicy(policy);

    policyResourceRequest(app).path(policy.getId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_POLICY, null);
    assertApplicationData(auditDTO, app);
    assertPolicyData(auditDTO, policy, true);
  }

  @Test
  void testDeletePolicy_Organization() throws Exception {
    Policy policy = aComplexPolicy();
    policy.setOwnerId(organization.getId());
    ctx.tempEntity().newPolicy(policy);

    policyResourceRequest(organization).path(policy.getId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_POLICY, null);
    assertOrganizationData(auditDTO, organization);
    assertPolicyData(auditDTO, policy, true);
  }

  @Test
  void testDeletePolicy_RepositoryContainer() throws Exception {
    Policy policy = aComplexPolicy();
    policy.setOwnerId(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    ctx.tempEntity().newPolicy(policy);

    policyResourceRequest(RepositoryContainer.SINGLETON).path(policy.getId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_POLICY, null);
    assertRepositoryContainerData(auditDTO);
    assertPolicyData(auditDTO, policy, true);
  }

  @Test
  void testDeletePolicy_RepositoryManager() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Policy policy = aComplexPolicy();
    policy.setOwnerId(repositoryManager.getId());
    ctx.tempEntity().newPolicy(policy);

    policyResourceRequest(repositoryManager).path(policy.getId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_POLICY, null);
    assertRepositoryManagerData(auditDTO, repositoryManager);
    assertPolicyData(auditDTO, policy, true);

  }

  @Test
  void testDeletePolicy_Repository() throws Exception {
    Repository repository = ctx.tempEntity().newRepository();
    Policy policy = aComplexPolicy();
    policy.setOwnerId(repository.getId());
    ctx.tempEntity().newPolicy(policy);

    ctx.restRequest()
        .path(PolicyResource.RESOURCE_PATH)
        .parameter(repository.getType(), repository.getId())
        .path(policy.getId())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_POLICY, null);
    assertCustomData(auditDTO, "repositoryId", repository.getId());
    assertPolicyData(auditDTO, policy, true);
  }

  @Test
  void testDeletePolicy_Unauthorized() throws Exception {
    String policyId = ctx.tempEntity().newPolicy(organization.getId(), "aPolicy").getId();

    policyResourceRequest(organization).path(policyId).with(unauthorizedUser()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_POLICY, "unauthorized");
    assertOrganizationData(auditDTO, organization);
  }

  @Test
  void testUpdateOverrides_Add_ForActions_Application() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    Policy policy = ctx.tempEntity().newPolicy();
    policy.setPolicyActionsOverrideAllowed(true);
    policyDAO.update(policy);

    Map<String, String> actionsOverride = new LinkedHashMap<>();
    actionsOverride.put("stage-release", "fail");
    actionsOverride.put("release", "fail");
    actionsOverride.put("build", "warn");
    policyResourceRequest(app).path(policy.getId(), "overrides")
        .body(new PolicyOverridesDTO(actionsOverride))
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_OVERRIDES, null);
    assertApplicationData(auditDTO, app);
    assertPolicyOverrideData(auditDTO, policy, app.getPublicId(), true, false);
  }

  @Test
  void testUpdateOverrides_Add_ForNotifications_Application() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    Policy policy = ctx.tempEntity().newPolicy();
    policy.setPolicyNotificationsOverrideAllowed(true);
    policyDAO.update(policy);

    Notifications notificationsOverride = new Notifications();
    notificationsOverride.add(new UserNotification("app@domain.com", BuildStageType.ID));
    policyResourceRequest(app).path(policy.getId(), "overrides")
        .body(new PolicyOverridesDTO(notificationsOverride))
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_OVERRIDES, null);
    assertApplicationData(auditDTO, app);
    assertPolicyOverrideData(auditDTO, policy, app.getPublicId(), false, true);
  }

  @Test
  void testUpdateOverrides_Add_ForActions_Organization() throws Exception {
    // Root Organization is the owner
    Policy policy = ctx.tempEntity().newPolicy();
    policy.setPolicyActionsOverrideAllowed(true);
    policyDAO.update(policy);

    policyResourceRequest(organization).path(policy.getId(), "overrides")
        .body(new PolicyOverridesDTO(new LinkedHashMap<>()))
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_OVERRIDES, null);
    assertOrganizationData(auditDTO, organization);
    assertPolicyOverrideData(auditDTO, policy, organization.getId(), true, false);
  }

  @Test
  void testUpdateOverrides_Add_ForNotifications_Organization() throws Exception {
    // Root Organization is the owner
    Policy policy = ctx.tempEntity().newPolicy();
    policy.setPolicyNotificationsOverrideAllowed(true);
    policyDAO.update(policy);

    policyResourceRequest(organization).path(policy.getId(), "overrides")
        .body(new PolicyOverridesDTO(new Notifications()))
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_OVERRIDES, null);
    assertOrganizationData(auditDTO, organization);
    assertPolicyOverrideData(auditDTO, policy, organization.getId(), false, true);
  }

  @Test
  void testUpdateOverrides_Add_Unauthorized() throws Exception {
    Policy policy = ctx.tempEntity().newPolicy();

    policyResourceRequest(organization)
        .path(policy.getId(), "overrides")
        .body(new PolicyOverridesDTO())
        .with(unauthorizedUser())
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_OVERRIDES, "unauthorized");
    assertOrganizationData(auditDTO, organization);
  }

  private JsonNode createDeleteBody(boolean deleteActionsOverride, boolean deleteNotificationsOverride) {
    ObjectNode json = JsonUtils.asTree(new PolicyOverridesDTO());
    if (!deleteActionsOverride) {
      json.remove("actions");
    }
    if (!deleteNotificationsOverride) {
      json.remove("notifications");
    }
    return json;
  }

  @Test
  void testUpdateOverrides_Delete_ForActions_Application() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    Policy policy = ctx.tempEntity().newPolicy();

    policyResourceRequest(app).path(policy.getId(), "overrides").body(createDeleteBody(true, false)).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_OVERRIDES, null);
    assertApplicationData(auditDTO, app);
    assertPolicyOverrideData(auditDTO, policy, app.getPublicId(), false, false);
  }

  @Test
  void testUpdateOverrides_Delete_ForNotifications_Application() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    Policy policy = ctx.tempEntity().newPolicy();

    policyResourceRequest(app).path(policy.getId(), "overrides").body(createDeleteBody(false, true)).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_OVERRIDES, null);
    assertApplicationData(auditDTO, app);
    assertPolicyOverrideData(auditDTO, policy, app.getPublicId(), false, false);
  }

  @Test
  void testUpdateOverrides_Delete_ForActions_Organization() throws Exception {
    Policy policy = ctx.tempEntity().newPolicy();

    policyResourceRequest(organization).path(policy.getId(), "overrides").body(createDeleteBody(true, false)).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_OVERRIDES, null);
    assertOrganizationData(auditDTO, organization);
    assertPolicyOverrideData(auditDTO, policy, organization.getId(), false, false);
  }

  @Test
  void testUpdateOverrides_Delete_ForNotifications_Organization() throws Exception {
    Policy policy = ctx.tempEntity().newPolicy();

    policyResourceRequest(organization).path(policy.getId(), "overrides").body(createDeleteBody(false, true)).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_OVERRIDES, null);
    assertOrganizationData(auditDTO, organization);
    assertPolicyOverrideData(auditDTO, policy, organization.getId(), false, false);
  }

  @Test
  void testUpdateOverrides_Delete_ForActions_Unauthorized() throws Exception {
    Policy policy = ctx.tempEntity().newPolicy();

    policyResourceRequest(organization).path(policy.getId(), "overrides")
        .body(createDeleteBody(true, false))
        .with(unauthorizedUser())
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_OVERRIDES, "unauthorized");
    assertOrganizationData(auditDTO, organization);
  }

  @Test
  void testUpdateOverrides_Delete_ForNotifications_Unauthorized() throws Exception {
    Policy policy = ctx.tempEntity().newPolicy();

    policyResourceRequest(organization).path(policy.getId(), "overrides")
        .body(createDeleteBody(false, true))
        .with(unauthorizedUser())
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_OVERRIDES, "unauthorized");
    assertOrganizationData(auditDTO, organization);
  }

  @Test
  void testImportPolicies_ImportNewPolicies() throws Exception {
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Arrays.asList(aComplexPolicy(), policy());
    policyResourceRequest(organization).path("import").part("file", "file", policyExportResult).post();

    assertImportedPolicies(policyExportResult.policies, organization.getId(), organization.getName(), null);
  }

  @Test
  void testImportPolicies_DeleteExistingPolicies() throws Exception {
    Application application = ctx.tempEntity().newApplication(organization.getId());
    // NOTE: The deleted policies specifically refer to LTGs which are also deleted during import
    LicenseThreatGroup orgLTG = ctx.tempEntity().newLicenseThreatGroup(organization.getId());
    LicenseThreatGroup appLTG = ctx.tempEntity().newLicenseThreatGroup(application.getId());
    Policy appPolicy = aComplexPolicy();
    appPolicy.setOwnerId(application.getId());
    appPolicy.addConstraint(constraint("Licensed", LogicalOperator.OR,
        condition(LicenseThreatGroupConditionType.ID, "is", appLTG.getId())));
    ctx.tempEntity().newPolicy(appPolicy);
    List<ConstraintDTO> appPolicyConstraints = ConstraintDTO.transcribe(appPolicy.getConstraints());
    Policy orgPolicy = aComplexPolicy();
    orgPolicy.setOwnerId(organization.getId());
    orgPolicy.addConstraint(constraint("Licensed", LogicalOperator.OR,
        condition(LicenseThreatGroupConditionType.ID, "is not", orgLTG.getId())));
    ctx.tempEntity().newPolicy(orgPolicy);
    List<ConstraintDTO> orgPolicyConstraints = ConstraintDTO.transcribe(orgPolicy.getConstraints());

    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Collections.singletonList(policy());
    policyResourceRequest(organization).path("import").part("file", "file", policyExportResult).post();

    List<AuditDTO> auditDTOs = awaitLogEntries(AuditEvent.DELETE_POLICY, 2);
    assertDeletedPolicyOnImport(appPolicy, appPolicyConstraints, auditDTOs, application);
    assertDeletedPolicyOnImport(orgPolicy, orgPolicyConstraints, auditDTOs, organization);
  }

  @Test
  void testImportPolicies_DontLogDeletedPoliciesIfTransactionFails() throws Exception {
    Policy policy = ctx.tempEntity().newPolicy(organization);

    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Collections.singletonList(policy());
    policyExportResult.labels = Collections.singletonList(new Label(organization.getId(), LONG_LABEL_NAME));
    policyResourceRequest(organization).path("import").part("file", "file", policyExportResult).post();

    assertAuditLog(AuditEvent.IMPORT, "bad-request");
    assertThat(awaitLogEntries(AuditEvent.DELETE_POLICY, 0)).isEmpty();
    assertThat(policyDAO.getById(policy.getId())).isNotNull();
  }

  @Test
  void testImportPolicies_PolicyTags_InheritMatchingCategory() throws Exception {
    Tag existingTag = ctx.tempEntity().newTag(organization.getId(), TemporaryEntity.uuid());
    Tag newTag = new Tag(organization.getId(), TemporaryEntity.uuid(), "desc2");

    PolicyExportResult policyExportResult = new PolicyExportResult();
    Policy policy = policy();
    policyExportResult.policies = Collections.singletonList(policy);
    policyExportResult.tags = Arrays.asList(existingTag, newTag);
    policyExportResult.policyTags = Arrays
        .asList(new PolicyTag(policy.getId(), existingTag.getId()), new PolicyTag(policy.getId(), newTag.getId()));
    policyResourceRequest(organization).path("import").part("file", "file", policyExportResult).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_POLICY_INHERITANCE, null);
    assertOrganizationData(auditDTO, organization);
    assertPolicyTagAuditData(auditDTO, policy, "matching-application-category");
    assertAuditedTags(auditDTO, policyExportResult.tags);
  }

  @Test
  void testImportPolicies_PolicyTags_InheritAll() throws Exception {
    PolicyExportResult policyExportResult = new PolicyExportResult();
    Policy policy = policy();
    policyExportResult.policies = Collections.singletonList(policy);

    policyResourceRequest(organization).path("import").part("file", "file", policyExportResult).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_POLICY_INHERITANCE, null);
    assertOrganizationData(auditDTO, organization);
    assertPolicyTagAuditData(auditDTO, policy, "all-children");
  }

  @Test
  void testImportPolicies_DontLogPolicyTagsIfTransactionFails() throws Exception {
    PolicyExportResult policyExportResult = new PolicyExportResult();
    Policy policy1 = policy();
    Policy invalidPolicy = policy();
    invalidPolicy.setName(policy1.getName());
    policyExportResult.policies = Arrays.asList(policy1, invalidPolicy);
    Tag existingTag = ctx.tempEntity().newTag(organization.getId(), TemporaryEntity.uuid());
    policyExportResult.policyTags = Collections.singletonList(new PolicyTag(policy1.getId(), existingTag.getId()));

    policyResourceRequest(organization).path("import").part("file", "file", policyExportResult).post();

    assertAuditLog(AuditEvent.IMPORT, "bad-request");
    assertThat(awaitLogEntries(AuditEvent.CONFIGURE_POLICY_INHERITANCE, 0)).isEmpty();
  }

  private void assertAuditedTags(final AuditDTO auditDTO, final List<Tag> tags) {
    List<ApplicationCategoryAuditDTO> auditedTags =
        ((Collection<?>) auditDTO.data.get("applicationCategories")).stream()
            .map(p -> JSON.convertValue(p, ApplicationCategoryAuditDTO.class))
            .collect(Collectors.toList());
    TagDAO tagDAO = this.tagDAO;

    assertThat(auditedTags).hasSameSizeAs(tags);
    for (int i = 0; i < tags.size(); i++) {
      assertThat(tagDAO.getById(auditedTags.get(i).applicationCategoryId)).isNotNull();
      assertThat(auditedTags.get(i).applicationCategoryName).isEqualTo(tags.get(i).getName());
    }
  }

  private void assertPolicyTagAuditData(
      final AuditDTO auditDTO,
      final Policy policy,
      final String inheritanceScope)
  {
    PolicyDAO policyDAO = this.policyDAO;
    assertThat(policyDAO.getById((String) auditDTO.data.get("policyId"))).isNotNull();
    assertCustomData(auditDTO, "policyName", policy.getName());
    assertCustomData(auditDTO, "inheritanceScope", inheritanceScope);
  }

  private void assertDeletedPolicyOnImport(
      final Policy policy,
      List<ConstraintDTO> constraints,
      List<AuditDTO> auditLogs,
      Owner owner)
  {
    AuditDTO foundDTO = auditLogs.stream()
        .filter(auditDTO -> auditDTO.data.get("policyId").equals(policy.getId()))
        .findFirst()
        .get();
    assertStandardData(foundDTO, AuditEvent.DELETE_POLICY, null);
    if (owner.getType().equals(OwnerType.APPLICATION)) {
      assertApplicationData(foundDTO, (Application) owner);
    }
    else {
      assertOrganizationData(foundDTO, (Organization) owner);
    }
    assertPolicyData(foundDTO, policy, true, constraints);
  }

  private PolicyWaiver savePolicyWaiver(String policyId, String ownerId) {
    return ctx.tempEntity().newWaiver("hash", policyId, ownerId, constraintFacts(), "comment");
  }

  private List<ConstraintFact> constraintFacts() {
    return Arrays.asList(
        constraintFact("constraintName1", conditionFact("summary1", "reason1"), conditionFact("summary2", "reason1"),
            conditionFact("summary3", "reason2")),
        constraintFact("constraintName2", conditionFact("summary1", "reason1"), conditionFact("summary2", "reason2")));
  }

  private ConstraintFact constraintFact(String constraintName, ConditionFact... conditionFacts) {
    return new ConstraintFact("constraintId", constraintName, "operatorName", conditionFacts);
  }

  private ConditionFact conditionFact(String summary, String reason) {
    return new ConditionFact("conditionTypeId", 0, summary, reason);
  }

  private void assertLicenseThreatGroupData(AuditDTO auditDTO, LicenseThreatGroup ltg, String... licenseNames) {
    if (ltg.getId() != null) {
      assertCustomData(auditDTO, "licenseThreatGroupId", ltg.getId());
    }
    else {
      assertThat(licenseThreatGroupDAO.getById((String) auditDTO.data.get("licenseThreatGroupId"))).isNotNull();
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
    String labelId = (String) auditDTO.data.get("labelId");
    assertThat(labelDAO.getById(labelId)).isNotNull();
    assertCustomData(auditDTO, "labelName", label.getLabel());
    assertCustomData(auditDTO, "labelDescription", label.getDescription());
    assertCustomData(auditDTO, "labelColor", label.getColor().toValue());
  }

  private void assertTagData(AuditDTO auditDTO, Tag tag) {
    Tag savedTag = tagDAO.getById((String) auditDTO.data.get("applicationCategoryId"));
    assertThat(savedTag).isNotNull();
    assertThat(savedTag.getName()).isEqualTo(tag.getName());
    assertThat(savedTag.getDescription()).isEqualTo(tag.getDescription());
    assertThat(savedTag.getColor()).isEqualTo(tag.getColor());
    assertCustomData(auditDTO, "applicationCategoryName", savedTag.getName());
    assertCustomData(auditDTO, "applicationCategoryDescription", savedTag.getDescription());
    assertCustomData(auditDTO, "applicationCategoryColor", savedTag.getColor().toValue());
  }

  private static final class TestLogOutput
      extends LogOutput
  {
    TestLogOutput(String... loggerNames) {
      super(loggerNames);
    }

    void tearDown() {
      after();
    }
  }
}
