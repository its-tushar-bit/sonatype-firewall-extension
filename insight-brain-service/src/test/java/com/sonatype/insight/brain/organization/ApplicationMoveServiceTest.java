/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiMoveApplicationResponseDTOV2;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.tag.ApplicationTagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.LicenseThreatGroupValueType;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class ApplicationMoveServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApplicationMoveService applicationMoveService;

  @Inject
  private ApplicationDAO applicationDAO;

  @Inject
  private ApplicationTagDAO applicationTagDAO;

  @Inject
  private PolicyDAO policyDAO;

  @Inject
  private PolicyMonitoringDAO policyMonitoringDAO;

  @Inject
  private PolicyViolationDAO policyViolationDAO;

  @Inject
  private PolicyWaiverDAO policyWaiverDAO;

  @Inject
  private ComponentLabelDAO componentLabelDAO;

  @Inject
  private LabelDAO labelDAO;

  @Inject
  private LicenseThreatGroupDAO ltgDAO;

  @Inject
  private LicenseOverrideDAO licenseOverrideDAO;

  @Inject
  private MembershipMappingDAO membershipMappingDAO;

  @Inject
  private IdUtils idUtils;

  private Organization newOrg;

  private Organization oldOrg;

  private Application app;

  @Before
  public void setupOrgHierarchy() {
    newOrg = tempEntity.newOrganization("New Org");
    oldOrg = tempEntity.newOrganization("Old Org");
    app = tempEntity.newApplication(oldOrg.getId());
  }

  @Test
  public void testGetDestinationOrganizations_SortedByName() {
    Organization orgA = tempEntity.newOrganization("Org A");
    Organization orgC = tempEntity.newOrganization("Org C");
    Organization orgE = tempEntity.newOrganization("Org E");
    Organization orgB = tempEntity.newOrganization("Org B");
    Organization orgD = tempEntity.newOrganization("Org D");

    assertThat(applicationMoveService.getDestinationOrganizations(app.getId())).extracting(Organization::getName)
        .containsExactly(newOrg.getName(), orgA.getName(), orgB.getName(), orgC.getName(), orgD.getName(),
            orgE.getName());
  }

  private void assertIssues(ApplicationMoveException e, String... issues) {
    ApiMoveApplicationResponseDTOV2 actual = (ApiMoveApplicationResponseDTOV2) e.getResponse().getEntity();
    assertThat(actual.errors).containsExactlyInAnyOrder(issues);
  }

  private String tagIssue(String format, Tag tag, Owner owner) {
    return String.format(format, tag.getName(), owner.getName());
  }

  private String policyIssue(String format, Policy policy, Owner owner) {
    return String.format(format, policy.getName(), owner.getName());
  }

  private String labelIssue(String format, Label label, Owner owner) {
    return String.format(format, label.getLabel(), owner.getName());
  }

  private String ltgIssue(String format, LicenseThreatGroup ltg, Owner owner) {
    return String.format(format, ltg.getName(), owner.getName());
  }

  private Policy newPolicy(Owner owner, LicenseThreatGroup ltg) {
    Constraint constraint = new Constraint(null, "LTG Constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(LicenseThreatGroupConditionType.ID, "is", ltg.getId()));
    constraint.addCondition(new Condition(LicenseThreatGroupConditionType.ID, "is",
        LicenseThreatGroupValueType.UNASSIGNED_LICENSE_THREAT_GROUP_ID));
    Policy policy = new Policy(null, "Test Policy");
    policy.setOwnerId(owner.getId());
    policy.addConstraint(constraint);
    return tempEntity.newPolicy(policy);
  }

  private Policy newPolicy(Owner owner, Label label) {
    Constraint constraint = new Constraint(null, "Label Constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(LabelConditionType.ID, "is", label.getId()));
    Policy policy = new Policy(null, "Test Policy");
    policy.setOwnerId(owner.getId());
    policy.addConstraint(constraint);
    return tempEntity.newPolicy(policy);
  }

  private Policy createPolicyWithOverrides(String name, String ownerId, String overridingOwnerId) {
    Policy policy = tempEntity.newPolicy(ownerId, name);

    policy.setPolicyActionsOverrideAllowed(true);
    Map<String, String> actionsOverrides = new LinkedHashMap<>();
    actionsOverrides.put("stage-release", "fail");
    actionsOverrides.put("release", "fail");
    actionsOverrides.put("build", "warn");

    policy.setPolicyNotificationsOverrideAllowed(true);
    Notifications notificationsOverride = new Notifications();
    notificationsOverride.add(new UserNotification("user@domain", BuildStageType.ID));

    String internalOwnerId = idUtils.getInternalOwnerId(OwnerType.APPLICATION, overridingOwnerId);
    policy.addPolicyActionsOverride(internalOwnerId, actionsOverrides);
    policy.addPolicyNotificationsOverride(internalOwnerId, notificationsOverride);
    policyDAO.update(policy);
    return policy;
  }

  @Test
  public void testMoveApplication_RootOrganizationAsDestination() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> applicationMoveService.moveApplication(app.getId(), Organization.ROOT_ORGANIZATION_ID))
        .withMessage("Applications cannot be moved to the root organization.");
  }

  @Test
  public void testMoveApplication_SameOrganization() {
    String appId = app.getId();
    String organizationId = app.getOrganizationId();

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> applicationMoveService.moveApplication(appId, organizationId))
        .withMessage("The destination organization must be different from the current organization");
  }

  @Test
  public void testMoveApplication_TagAppliedToApplicationUnmatchedInNewParentOrg() {
    Tag tag = tempEntity.newTag(oldOrg.getId());
    tempEntity.newApplicationTag(app.getId(), tag.getId());

    assertThatExceptionOfType(ApplicationMoveException.class)
        .isThrownBy(() -> applicationMoveService.moveApplication(app.getId(), newOrg.getId()))
        .satisfies(e -> assertIssues(e, tagIssue(ApplicationMoveService.TAG_MISSING_MSG, tag, oldOrg)));
  }

  @Test
  public void testMoveApplication_TagNotAppliedToApplicationUnmatchedInNewParentOrg() {
    tempEntity.newTag(oldOrg.getId());

    assertThat(applicationMoveService.moveApplication(app.getId(), newOrg.getId()).warnings).isEmpty();
    assertThat(applicationDAO.getById(app.getId()).getOrganizationId()).isEqualTo(newOrg.getId());
  }

  @Test
  public void testMoveApplication_ApplicableOrgPolicyMissingInNewParentOrg() {
    Policy inheritedPolicy = tempEntity.newPolicy(oldOrg.getId(), "Inherited Policy");
    Policy taggedPolicy = tempEntity.newPolicy(oldOrg.getId(), "Tagged Policy");
    Tag oldTag = tempEntity.newTag(oldOrg.getId(), "Matched Tag");
    tempEntity.newPolicyTag(taggedPolicy.getId(), oldTag.getId());
    tempEntity.newApplicationTag(app.getId(), oldTag.getId());
    tempEntity.newTag(newOrg.getId(), "matchedtag");
    Tag otherTag = tempEntity.newTag(newOrg.getId(), "othertag");
    Policy policy3 = tempEntity.newPolicy(newOrg.getId(), "taggedpolicy");
    tempEntity.newPolicyTag(policy3.getId(), otherTag.getId());

    assertThatExceptionOfType(ApplicationMoveException.class)
        .isThrownBy(() -> applicationMoveService.moveApplication(app.getId(), newOrg.getId()))
        .satisfies(e -> assertIssues(e, policyIssue(ApplicationMoveService.POLICY_MISSING_MSG, inheritedPolicy, oldOrg),
            policyIssue(ApplicationMoveService.TAG_MISMATCH_MSG, taggedPolicy, oldOrg)));
  }

  @Test
  public void testMoveApplication_NonApplicableOrgPolicyMissingInNewParentOrg() {
    Policy inheritedPolicy = tempEntity.newPolicy(oldOrg);
    tempEntity.newPolicyTag(inheritedPolicy.getId(), tempEntity.newTag(oldOrg.getId()).getId());

    assertThat(applicationMoveService.moveApplication(app.getId(), newOrg.getId()).warnings).isEmpty();
    assertThat(applicationDAO.getById(app.getId()).getOrganizationId()).isEqualTo(newOrg.getId());
  }

  @Test
  public void testMoveApplication_ApplicableOrgPolicyMatchedByNewParentOrg() {
    Tag oldTag = tempEntity.newTag(oldOrg.getId(), "Matched Tag");
    Policy oldPolicy = tempEntity.newPolicy(oldOrg.getId(), "Matched Policy");
    tempEntity.newPolicyTag(oldPolicy.getId(), oldTag.getId());
    ApplicationTag appTag = tempEntity.newApplicationTag(app.getId(), oldTag.getId());
    Tag newTag = tempEntity.newTag(newOrg.getId(), "matchedtag");
    Policy newPolicy = tempEntity.newPolicy(newOrg.getId(), "matchedpolicy");
    tempEntity.newPolicyTag(newPolicy.getId(), newTag.getId());

    assertThat(applicationMoveService.moveApplication(app.getId(), newOrg.getId()).warnings).isEmpty();
    assertThat(applicationDAO.getById(app.getId()).getOrganizationId()).isEqualTo(newOrg.getId());
    appTag = applicationTagDAO.getById(appTag.getId());
    assertThat(appTag.getTagId()).isEqualTo(newTag.getId());
  }

  @Test
  public void testMoveApplication_OldOrgPolicyMatchedByNewOrgPolicy() {
    Policy oldPolicy = tempEntity.newPolicy(oldOrg.getId(), "Matched Policy");
    Policy newPolicy = tempEntity.newPolicy(newOrg.getId(), "matchedpolicy");

    assertThat(applicationMoveService.moveApplication(app.getId(), newOrg.getId()).warnings).isEmpty();
    assertThat(applicationDAO.getById(app.getId()).getOrganizationId()).isEqualTo(newOrg.getId());
    oldPolicy = policyDAO.getById(oldPolicy.getId());
    assertThat(oldPolicy.getOwnerId()).isEqualTo(oldOrg.getId());
    newPolicy = policyDAO.getById(newPolicy.getId());
    assertThat(newPolicy.getOwnerId()).isEqualTo(newOrg.getId());
  }

  @Test
  public void testMoveApplication_OldAppPolicyMatchedByNewOrgPolicy() {
    Policy oldPolicy = tempEntity.newPolicy(app.getId(), "Matched Policy");
    Policy newPolicy = tempEntity.newPolicy(newOrg.getId(), "matchedpolicy");

    assertThat(applicationMoveService.moveApplication(app.getId(), newOrg.getId()).warnings).isEmpty();
    assertThat(applicationDAO.getById(app.getId()).getOrganizationId()).isEqualTo(newOrg.getId());
    oldPolicy = policyDAO.getById(oldPolicy.getId());
    assertThat(oldPolicy).isNull();
    newPolicy = policyDAO.getById(newPolicy.getId());
    assertThat(newPolicy.getOwnerId()).isEqualTo(newOrg.getId());
  }

  @Test
  public void testMoveApplication_OldAppPolicyUnmatchedByNewOrgPolicy() {
    Policy oldPolicy = tempEntity.newPolicy(app);

    assertThat(applicationMoveService.moveApplication(app.getId(), newOrg.getId()).warnings).isEmpty();
    assertThat(applicationDAO.getById(app.getId()).getOrganizationId()).isEqualTo(newOrg.getId());
    oldPolicy = policyDAO.getById(oldPolicy.getId());
    assertThat(oldPolicy).isNotNull();
  }

  @Test
  public void testMoveApplication_OldAppPolicyClashesWithNonApplicableOrgPolicy() {
    Policy oldPolicy = tempEntity.newPolicy(app);
    Policy newPolicy = tempEntity.newPolicy(newOrg.getId(), oldPolicy.getName());
    tempEntity.newPolicyTag(newPolicy.getId(), tempEntity.newTag(newOrg.getId()).getId());

    assertThatExceptionOfType(ApplicationMoveException.class)
        .isThrownBy(() -> applicationMoveService.moveApplication(app.getId(), newOrg.getId()))
        .satisfies(e -> assertIssues(e, policyIssue(ApplicationMoveService.TAG_MISMATCH_2_MSG, oldPolicy, app)));
  }

  @Test
  public void testMoveApplication_PolicyViolations() {
    Policy oldPolicy = tempEntity.newPolicy(app.getId(), "Matched Policy");
    Policy newPolicy = tempEntity.newPolicy(newOrg.getId(), "matchedpolicy");
    PolicyEvaluation appEval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scanId");
    PolicyViolation appViolation = tempEntity.newPolicyViolation(appEval, oldPolicy);

    assertThat(applicationMoveService.moveApplication(app.getId(), newOrg.getId()).warnings).isEmpty();
    assertThat(applicationDAO.getById(app.getId()).getOrganizationId()).isEqualTo(newOrg.getId());
    appViolation = policyViolationDAO.getById(appViolation.getId());
    assertThat(appViolation.getPolicyId()).isEqualTo(newPolicy.getId());
  }

  @Test
  public void testMoveApplication_AppPolicyWaiver() {
    Policy oldPolicy = tempEntity.newPolicy(app);
    Policy newPolicy = tempEntity.newPolicy(newOrg.getId(), oldPolicy.getName());
    PolicyWaiver waiver = tempEntity.newWaiver(oldPolicy.getId(), app.getId());

    assertThat(applicationMoveService.moveApplication(app.getId(), newOrg.getId()).warnings).isEmpty();
    assertThat(applicationDAO.getById(app.getId()).getOrganizationId()).isEqualTo(newOrg.getId());
    waiver = policyWaiverDAO.getById(waiver.getId());
    assertThat(waiver.getPolicyId()).isEqualTo(newPolicy.getId());
  }

  @Test
  public void testMoveApplication_OldOrgPolicyWaiverMatchedByNewOrgPolicyWaiver() {
    Policy oldPolicy = tempEntity.newPolicy(oldOrg);
    Policy newPolicy = tempEntity.newPolicy(newOrg.getId(), oldPolicy.getName());
    PolicyWaiver oldWaiver = tempEntity.newWaiver("hash", oldPolicy.getId(), oldOrg.getId());
    tempEntity.newWaiver(newPolicy.getId(), newOrg.getId());

    assertThat(applicationMoveService.moveApplication(app.getId(), newOrg.getId()).warnings).isEmpty();
    assertThat(applicationDAO.getById(app.getId()).getOrganizationId()).isEqualTo(newOrg.getId());
    oldWaiver = policyWaiverDAO.getById(oldWaiver.getId());
    assertThat(oldWaiver.getPolicyId()).isEqualTo(oldPolicy.getId());
    assertThat(policyWaiverDAO.getActiveByOwnerId(app.getId())).isEmpty();
  }

  @Test
  public void testMoveApplication_OldOrgPolicyWaiverNotMatchedByNewOrgPolicyWaivers() {
    Policy oldPolicy = tempEntity.newPolicy(oldOrg);
    Policy newPolicy = tempEntity.newPolicy(newOrg.getId(), oldPolicy.getName());
    PolicyWaiver oldWaiver = tempEntity.newWaiver("hash", oldPolicy.getId(), oldOrg.getId());
    tempEntity.newWaiver("other-hash", newPolicy.getId(), newOrg.getId());

    assertThat(applicationMoveService.moveApplication(app.getId(), newOrg.getId()).warnings)
        .containsExactlyInAnyOrder(String.format(ApplicationMoveService.POLICY_WAIVERS_LOST_MSG, 1));
    assertThat(applicationDAO.getById(app.getId()).getOrganizationId()).isEqualTo(newOrg.getId());
    oldWaiver = policyWaiverDAO.getById(oldWaiver.getId());
    assertThat(oldWaiver.getPolicyId()).isEqualTo(oldPolicy.getId());
    assertThat(policyWaiverDAO.getActiveByOwnerId(app.getId())).isEmpty();
  }

  @Test
  public void testMoveApplication_InheritedPolicyMonitoringMatchedByNewParent() {
    tempEntity.newPolicyMonitoring(oldOrg.getId(), Stage.ID_RELEASE);
    tempEntity.newPolicyMonitoring(newOrg.getId(), Stage.ID_RELEASE);

    assertThat(applicationMoveService.moveApplication(app.getId(), newOrg.getId()).warnings).isEmpty();
    assertThat(applicationDAO.getById(app.getId()).getOrganizationId()).isEqualTo(newOrg.getId());
    PolicyMonitoring appMonitoring = policyMonitoringDAO.getByOwnerIdAndStageTypeId(app.getId(), Stage.ID_RELEASE);
    assertThat(appMonitoring).isNull();
  }

  @Test
  public void testMoveApplication_InheritedPolicyMonitoringNotMatchedByNewParent() {
    tempEntity.newPolicyMonitoring(oldOrg.getId(), Stage.ID_RELEASE);
    tempEntity.newPolicyMonitoring(newOrg.getId(), Stage.ID_OPERATE);

    assertThat(applicationMoveService.moveApplication(app.getId(), newOrg.getId()).warnings)
        .containsExactlyInAnyOrder(ApplicationMoveService.POLICY_MONITORING_DIFFERENT_MSG);
    assertThat(applicationDAO.getById(app.getId()).getOrganizationId()).isEqualTo(newOrg.getId());
    PolicyMonitoring appMonitoring = policyMonitoringDAO.getByOwnerIdAndStageTypeId(app.getId(), Stage.ID_OPERATE);
    assertThat(appMonitoring).isNull();
  }

  @Test
  public void testMoveApplication_InheritedPolicyMonitoringMissingInNewParent() {
    tempEntity.newPolicyMonitoring(oldOrg.getId(), Stage.ID_RELEASE);

    assertThat(applicationMoveService.moveApplication(app.getId(), newOrg.getId()).warnings)
        .containsExactlyInAnyOrder(ApplicationMoveService.POLICY_MONITORING_MISSING_MSG);
    assertThat(applicationDAO.getById(app.getId()).getOrganizationId()).isEqualTo(newOrg.getId());
    PolicyMonitoring appMonitoring = policyMonitoringDAO.getByOwnerIdAndStageTypeId(app.getId(), Stage.ID_RELEASE);
    assertThat(appMonitoring).isNull();
  }

  @Test
  public void testMoveApplication_OwnerRoleAlreadyAssignedToApp() {
    membershipMappingDAO.insert(new MembershipMapping(app.getId(), Role.OWNER_ROLE_ID, USERNAME, MemberType.USER));

    assertThat(applicationMoveService.moveApplication(app.getId(), newOrg.getId()).warnings).isEmpty();
    assertThat(applicationDAO.getById(app.getId()).getOrganizationId()).isEqualTo(newOrg.getId());
    assertThat(membershipMappingDAO.getByContextId(app.getId())).hasSize(1);
  }

  @Test
  public void testMoveApplication_OwnerRoleAlreadyInheritedFromNewParent() {
    membershipMappingDAO.insert(new MembershipMapping(newOrg.getId(), Role.OWNER_ROLE_ID, USERNAME, MemberType.USER));

    assertThat(applicationMoveService.moveApplication(app.getId(), newOrg.getId()).warnings).isEmpty();
    assertThat(applicationDAO.getById(app.getId()).getOrganizationId()).isEqualTo(newOrg.getId());
    assertThat(membershipMappingDAO.getByContextId(app.getId())).isEmpty();
  }

  @Test
  public void testMoveApplication_OwnerRoleNotAlreadyInheritedFromNewParent() {
    assertThat(applicationMoveService.moveApplication(app.getId(), newOrg.getId()).warnings).isEmpty();
    assertThat(applicationDAO.getById(app.getId()).getOrganizationId()).isEqualTo(newOrg.getId());
    assertThat(membershipMappingDAO.getByContextId(app.getId())).hasSize(1);
    MembershipMapping membershipMapping = membershipMappingDAO.getByContextId(app.getId()).get(0);
    assertThat(membershipMapping.getRoleId()).isEqualTo(Role.OWNER_ROLE_ID);
    assertThat(membershipMapping.getMemberName()).isEqualTo(USERNAME);
    assertThat(membershipMapping.getMemberType()).isEqualTo(MemberType.USER);
  }

  @Test
  public void testMoveApplication_UsedOrgLtgMissingInNewParentOrg() {
    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(oldOrg.getId());
    newPolicy(app, ltg);

    assertThatExceptionOfType(ApplicationMoveException.class)
        .isThrownBy(() -> applicationMoveService.moveApplication(app.getId(), newOrg.getId()))
        .satisfies(e -> assertIssues(e, ltgIssue(ApplicationMoveService.LTG_MISSING_MSG, ltg, oldOrg)));
  }

  @Test
  public void testMoveApplication_UnusedOrgLtgMissingInNewParentOrg() {
    tempEntity.newLicenseThreatGroup(oldOrg.getId());

    assertThat(applicationMoveService.moveApplication(app.getId(), newOrg.getId()).warnings).isEmpty();
    assertThat(applicationDAO.getById(app.getId()).getOrganizationId()).isEqualTo(newOrg.getId());
  }

  @Test
  public void testMoveApplication_OldOrgLtgMatchedByNewOrgLtg() {
    LicenseThreatGroup oldLtg = tempEntity.newLicenseThreatGroup(oldOrg.getId(), "Matched LTG", 5);
    Policy policy = newPolicy(app, oldLtg);
    LicenseThreatGroup newLtg = tempEntity.newLicenseThreatGroup(newOrg.getId(), "matchedltg", 4);

    assertThat(applicationMoveService.moveApplication(app.getId(), newOrg.getId()).warnings).isEmpty();
    assertThat(applicationDAO.getById(app.getId()).getOrganizationId()).isEqualTo(newOrg.getId());
    policy = policyDAO.getById(policy.getId());
    assertThat(policy.getConstraints().get(0).getConditions().get(0).getValue()).isEqualTo(newLtg.getId());
  }

  @Test
  public void testMoveApplication_OldAppLtgMatchedByNewOrgLtg() {
    LicenseThreatGroup oldLtg = tempEntity.newLicenseThreatGroup(app.getId(), "Matched LTG", 5);
    Policy policy = newPolicy(app, oldLtg);
    LicenseThreatGroup newLtg = tempEntity.newLicenseThreatGroup(newOrg.getId(), "matchedltg", 4);

    assertThat(applicationMoveService.moveApplication(app.getId(), newOrg.getId()).warnings).isEmpty();
    assertThat(applicationDAO.getById(app.getId()).getOrganizationId()).isEqualTo(newOrg.getId());
    policy = policyDAO.getById(policy.getId());
    assertThat(policy.getConstraints().get(0).getConditions().get(0).getValue()).isEqualTo(newLtg.getId());
    oldLtg = ltgDAO.getById(oldLtg.getId());
    assertThat(oldLtg).isNull();
  }

  @Test
  public void testMoveApplication_OldAppLtgUnmatchedByNewOrgLtgs() {
    LicenseThreatGroup oldLtg = tempEntity.newLicenseThreatGroup(app.getId());
    Policy policy = newPolicy(app, oldLtg);

    assertThat(applicationMoveService.moveApplication(app.getId(), newOrg.getId()).warnings).isEmpty();
    assertThat(applicationDAO.getById(app.getId()).getOrganizationId()).isEqualTo(newOrg.getId());
    policy = policyDAO.getById(policy.getId());
    assertThat(policy.getConstraints().get(0).getConditions().get(0).getValue()).isEqualTo(oldLtg.getId());
    oldLtg = ltgDAO.getById(oldLtg.getId());
    assertThat(oldLtg).isNotNull();
  }

  @Test
  public void testMoveApplication_UsedOrgLabelMissingInNewParentOrg() {
    Label labelUsedByPolicy = tempEntity.newLabel(oldOrg.getId(), "used-by-policy");
    newPolicy(app, labelUsedByPolicy);
    Label labelUsedByComponentLabel = tempEntity.newLabel(oldOrg.getId(), "used-by-component-label");
    tempEntity.newComponentLabel(app.getId(), labelUsedByComponentLabel.getId());

    assertThatExceptionOfType(ApplicationMoveException.class)
        .isThrownBy(() -> applicationMoveService.moveApplication(app.getId(), newOrg.getId()))
        .satisfies(e -> assertIssues(e, labelIssue(ApplicationMoveService.LABEL_MISSING_MSG, labelUsedByPolicy, oldOrg),
            labelIssue(ApplicationMoveService.LABEL_MISSING_MSG, labelUsedByComponentLabel, oldOrg)));
  }

  @Test
  public void testMoveApplication_UnusedOrgLabelMissingInNewParentOrg() {
    tempEntity.newLabel(oldOrg.getId());

    assertThat(applicationMoveService.moveApplication(app.getId(), newOrg.getId()).warnings).isEmpty();
    assertThat(applicationDAO.getById(app.getId()).getOrganizationId()).isEqualTo(newOrg.getId());
  }

  @Test
  public void testMoveApplication_OldOrgLabelMatchedByNewOrgLabel() {
    Label oldLabel = tempEntity.newLabel(oldOrg.getId(), "Matched Label");
    Policy policy = newPolicy(app, oldLabel);
    Label newLabel = tempEntity.newLabel(newOrg.getId(), "matched label");

    assertThat(applicationMoveService.moveApplication(app.getId(), newOrg.getId()).warnings).isEmpty();
    assertThat(applicationDAO.getById(app.getId()).getOrganizationId()).isEqualTo(newOrg.getId());
    policy = policyDAO.getById(policy.getId());
    assertThat(policy.getConstraints().get(0).getConditions().get(0).getValue()).isEqualTo(newLabel.getId());
  }

  @Test
  public void testMoveApplication_OldAppLabelMatchedByNewOrgLabel() {
    Label oldLabel = tempEntity.newLabel(app.getId(), "Matched Label");
    Policy policy = newPolicy(app, oldLabel);
    Label newLabel = tempEntity.newLabel(newOrg.getId(), "matched label");

    assertThat(applicationMoveService.moveApplication(app.getId(), newOrg.getId()).warnings).isEmpty();
    assertThat(applicationDAO.getById(app.getId()).getOrganizationId()).isEqualTo(newOrg.getId());
    policy = policyDAO.getById(policy.getId());
    assertThat(policy.getConstraints().get(0).getConditions().get(0).getValue()).isEqualTo(newLabel.getId());
    oldLabel = labelDAO.getById(oldLabel.getId());
    assertThat(oldLabel).isNull();
  }

  @Test
  public void testMoveApplication_OldAppLabelUnmatchedByNewOrgLabels() {
    Label oldLabel = tempEntity.newLabel(app.getId());
    Policy policy = newPolicy(app, oldLabel);

    assertThat(applicationMoveService.moveApplication(app.getId(), newOrg.getId()).warnings).isEmpty();
    assertThat(applicationDAO.getById(app.getId()).getOrganizationId()).isEqualTo(newOrg.getId());
    policy = policyDAO.getById(policy.getId());
    assertThat(policy.getConstraints().get(0).getConditions().get(0).getValue()).isEqualTo(oldLabel.getId());
    oldLabel = labelDAO.getById(oldLabel.getId());
    assertThat(oldLabel).isNotNull();
  }

  @Test
  public void testMoveApplication_AppComponentLabel() {
    Label oldLabel = tempEntity.newLabel(oldOrg.getId(), "Matched Label");
    Label newLabel = tempEntity.newLabel(newOrg.getId(), oldLabel.getLabel());
    ComponentLabel componentLabel = tempEntity.newComponentLabel(app.getId(), oldLabel.getId());

    assertThat(applicationMoveService.moveApplication(app.getId(), newOrg.getId()).warnings).isEmpty();
    assertThat(applicationDAO.getById(app.getId()).getOrganizationId()).isEqualTo(newOrg.getId());
    componentLabel = componentLabelDAO.getById(componentLabel.getId());
    assertThat(componentLabel.getLabelId()).isEqualTo(newLabel.getId());
  }

  @Test
  public void testMoveApplication_OldOrgComponentLabelMatchedByNewOrgComponentLabel() {
    Label oldLabel = tempEntity.newLabel(oldOrg.getId(), "Matched Label");
    Label newLabel = tempEntity.newLabel(newOrg.getId(), oldLabel.getLabel());
    ComponentLabel oldComponentLabel = tempEntity.newComponentLabel(oldOrg.getId(), oldLabel.getId());
    tempEntity.newComponentLabel(newOrg.getId(), newLabel.getId(), oldComponentLabel.getHash());

    assertThat(applicationMoveService.moveApplication(app.getId(), newOrg.getId()).warnings).isEmpty();
    assertThat(applicationDAO.getById(app.getId()).getOrganizationId()).isEqualTo(newOrg.getId());
    oldComponentLabel = componentLabelDAO.getById(oldComponentLabel.getId());
    assertThat(oldComponentLabel.getLabelId()).isEqualTo(oldLabel.getId());
    assertThat(componentLabelDAO.getByOwnerId(app.getId())).isEmpty();
  }

  @Test
  public void testMoveApplication_OldOrgComponentLabelNotMatchedByNewOrgComponentLabels() {
    Label oldLabel = tempEntity.newLabel(oldOrg.getId(), "Matched Label");
    tempEntity.newLabel(newOrg.getId(), oldLabel.getLabel());
    ComponentLabel oldComponentLabel = tempEntity.newComponentLabel(oldOrg.getId(), oldLabel.getId());

    assertThat(applicationMoveService.moveApplication(app.getId(), newOrg.getId()).warnings)
        .containsExactlyInAnyOrder(String.format(ApplicationMoveService.COMPONENT_LABELS_LOST_MSG, 1));
    assertThat(applicationDAO.getById(app.getId()).getOrganizationId()).isEqualTo(newOrg.getId());
    oldComponentLabel = componentLabelDAO.getById(oldComponentLabel.getId());
    assertThat(oldComponentLabel.getLabelId()).isEqualTo(oldLabel.getId());
    assertThat(componentLabelDAO.getByOwnerId(app.getId())).isEmpty();
  }

  @Test
  public void testMoveApplication_OldOrgLicenseOverrideMatchedByNewOrgLicenseOverride() {
    tempEntity.newLicenseOverride(oldOrg.getId(), ComponentIdentifier.createMavenCoordinates("g", "a", "1"),
        LicenseOverrideStatus.SELECTED, "Apache-2.0");
    tempEntity.newLicenseOverride(oldOrg.getId(), ComponentIdentifier.createMavenCoordinates("g", "a", "2", "c", "e"),
        LicenseOverrideStatus.ACKNOWLEDGED, (String) null);
    tempEntity.newLicenseOverride(newOrg.getId(), ComponentIdentifier.createMavenCoordinates("g", "a", "1"),
        LicenseOverrideStatus.SELECTED, "Apache-2.0");
    tempEntity.newLicenseOverride(newOrg.getId(), ComponentIdentifier.createMavenCoordinates("g", "a", "2"),
        LicenseOverrideStatus.ACKNOWLEDGED, (String) null);

    assertThat(applicationMoveService.moveApplication(app.getId(), newOrg.getId()).warnings).isEmpty();
    assertThat(applicationDAO.getById(app.getId()).getOrganizationId()).isEqualTo(newOrg.getId());
    assertThat(licenseOverrideDAO.getByOwnerId(app.getId())).isEmpty();
  }

  @Test
  public void testMoveApplication_OldOrgLicenseOverrideNotMatchedByNewOrgLicenseOverrides() {
    LicenseOverride oldOverride1 = tempEntity.newLicenseOverride(oldOrg.getId(),
        ComponentIdentifier.createMavenCoordinates("g", "a", "1"), LicenseOverrideStatus.SELECTED, "Apache-2.0");
    LicenseOverride oldOverride2 = tempEntity.newLicenseOverride(oldOrg.getId(),
        ComponentIdentifier.createMavenCoordinates("g", "a", "2", "c", "e"), LicenseOverrideStatus.OPEN, (String) null);
    LicenseOverride oldOverride3 = tempEntity.newLicenseOverride(oldOrg.getId(),
        ComponentIdentifier.createMavenCoordinates("g", "a", "3"), LicenseOverrideStatus.OVERRIDDEN, "MIT");
    tempEntity.newLicenseOverride(newOrg.getId(), ComponentIdentifier.createMavenCoordinates("g", "a", "1", "c", "e"),
        oldOverride1.getStatus(), oldOverride1.getLicenseIds(), "identifier-mismatch");
    tempEntity.newLicenseOverride(newOrg.getId(), oldOverride2.getComponentIdentifier(),
        LicenseOverrideStatus.CONFIRMED, oldOverride2.getLicenseIds(), "status-mismatch");
    tempEntity.newLicenseOverride(newOrg.getId(), oldOverride3.getComponentIdentifier(), oldOverride3.getStatus(),
        "GPL-2.0", "license-mismatch");

    assertThat(applicationMoveService.moveApplication(app.getId(), newOrg.getId()).warnings)
        .containsExactlyInAnyOrder(String.format(ApplicationMoveService.LICENSE_OVERRIDES_LOST_MSG, 3));
    assertThat(applicationDAO.getById(app.getId()).getOrganizationId()).isEqualTo(newOrg.getId());
    assertThat(licenseOverrideDAO.getByOwnerId(app.getId())).isEmpty();
  }

  @Test
  public void testMoveApplication_OldPolicyOverridesAreRemoved() {
    Application app = tempEntity.newApplication(oldOrg.getId());
    Policy oldOrgPolicy = createPolicyWithOverrides(
        "Policy with overrides", oldOrg.getId(), app.getId());
    Policy rootOrgPolicy = createPolicyWithOverrides(
        "Policy with overrides 2", Organization.ROOT_ORGANIZATION_ID, app.getId());
    createPolicyWithOverrides("Policy with overrides", newOrg.getId(), app.getId());
    applicationMoveService.moveApplication(app.getId(), newOrg.getId());

    oldOrgPolicy = policyDAO.getById(oldOrgPolicy.getId());
    rootOrgPolicy = policyDAO.getById(rootOrgPolicy.getId());

    assertThat(oldOrgPolicy.getPolicyActionsOverrides().get(app.getId())).isNull();
    assertThat(rootOrgPolicy.getPolicyActionsOverrides().get(app.getId())).hasSize(3);

    assertThat(oldOrgPolicy.getPolicyNotificationsOverrides().get(app.getId())).isNull();
    assertThat(rootOrgPolicy.getPolicyNotificationsOverrides().get(app.getId()).getAllNotifications()).hasSize(1);
  }
}
