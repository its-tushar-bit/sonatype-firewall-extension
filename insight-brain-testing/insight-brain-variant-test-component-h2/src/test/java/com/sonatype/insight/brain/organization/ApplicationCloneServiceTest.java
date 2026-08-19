/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProprietaryConfigDAO;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.tag.ApplicationTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.PolicyTagDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.ProprietaryConfig;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.conditions.AgeInDaysConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupLevelConditionType;
import com.sonatype.insight.brain.model.policy.facts.TriggerLabel;
import com.sonatype.insight.brain.model.policy.facts.TriggerLicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.facts.TriggerLicenseThreatGroupWithThreatLevel;
import com.sonatype.insight.brain.model.policy.notifications.JiraNotification;
import com.sonatype.insight.brain.model.policy.notifications.RoleNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.policy.notifications.WebhookNotification;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverride;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.telemetry.OwnerMaintenanceTelemetry;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.error.exception.PaymentRequiredException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.nexus.scm.SourceControlProvider;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

@ComponentH2Test
public class ApplicationCloneServiceTest
    extends AbstractComponentH2Test
{
  @Inject
  private ApplicationDAO applicationDAO;

  @Inject
  private LabelDAO labelDAO;

  @Inject
  private RoleDAO roleDAO;

  @Inject
  private ComponentLabelDAO componentLabelDAO;

  @Inject
  private LicenseThreatGroupDAO licenseThreatGroupDAO;

  @Inject
  private LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO;

  @Inject
  private LicenseOverrideDAO licenseOverrideDAO;

  @Inject
  private SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO;

  @Inject
  private MembershipMappingDAO membershipMappingDAO;

  @Inject
  private PolicyMonitoringDAO policyMonitoringDAO;

  @Inject
  private ApplicationTagDAO applicationTagDAO;

  @Inject
  private ProprietaryConfigDAO proprietaryConfigDAO;

  @Inject
  private SourceControlDAO sourceControlDAO;

  @Inject
  private PolicyDAO policyDAO;

  @Inject
  private PolicyTagDAO policyTagDAO;

  @Inject
  private PolicyWaiverDAO policyWaiverDAO;

  @Inject
  private ApplicationCloneService appCloneService;

  @Inject
  private TestProductLicense testProductLicense;

  @Mock
  private TelemetrySender telemetrySenderMock;

  private Application sourceApp;

  @BeforeEach
  public void before() {
    sourceApp = tempEntity.newApplicationWithParent();
  }

  @Test
  public void testCloneApplication_SourceApplicationDoesNotExist() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> appCloneService.cloneApplication("AppDoesNotExistId", "clonedAppName", "clonedAppPublicId"))
        .withMessage("Application with ID AppDoesNotExistId does not exist.");
  }

  @Test
  public void testCloneApplication_Application() {
    String clonedAppName = "clonedAppName";
    String clonedAppPublicId = "clonedAppPublicId";
    String contactUsername = "clonecontactuser";

    tempEntity.newUser(contactUsername);
    sourceApp.setContactInternalName(contactUsername);

    // The application cloning is supposed to disable legacy violation for the cloned app.
    // So we set it to true in the source application in order to verify
    // that is not copied to the cloned application.
    sourceApp.setLegacyViolationEnabled(true);
    applicationDAO.update(sourceApp);

    ApiApplicationDTO clonedAppDTO =
        appCloneService.cloneApplication(sourceApp.getId(), clonedAppName, clonedAppPublicId);

    // Assert the returned app DTO.
    assertThat(clonedAppDTO.organizationId).isEqualTo(sourceApp.getOrganizationId());
    assertThat(clonedAppDTO.name).isEqualTo(clonedAppName);
    assertThat(clonedAppDTO.publicId).isEqualTo(clonedAppPublicId);
    assertThat(clonedAppDTO.contactUserName).isEqualTo(contactUsername);
    assertThat(clonedAppDTO.applicationTags).isEmpty();

    // Assert the app stored in the db.
    Application clonedApp = applicationDAO.getByIdNotNull(clonedAppDTO.id);
    assertThat(clonedApp.getOrganizationId()).isEqualTo(sourceApp.getOrganizationId());
    assertThat(clonedApp.getName()).isEqualTo(clonedAppName);
    assertThat(clonedApp.getPublicId()).isEqualTo(clonedAppPublicId);
    assertThat(clonedApp.getContactInternalName()).isEqualTo(contactUsername);
    assertThat(clonedApp.isLegacyViolationEnabled()).isFalse();

    final ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock).send(telemetryDataArgumentCaptor.capture());
    final TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();

    OwnerMaintenanceTelemetry ownerMaintenanceTelemetryData =
        (OwnerMaintenanceTelemetry) telemetryData.getAttributes()
            .get(OwnerMaintenanceTelemetry.OWNER_MAINTENANCE_TELEMETRY);
    assertThat(ownerMaintenanceTelemetryData).isNotNull();

    assertThat(ownerMaintenanceTelemetryData.getOwnerId()).isEqualTo(clonedAppDTO.id);
    assertThat(ownerMaintenanceTelemetryData.getOwnerName()).isEqualTo(clonedAppDTO.name);
    assertThat(ownerMaintenanceTelemetryData.getOwnerMaintenanceType()).isEqualTo(OwnerMaintenanceTelemetry.TYPE_ADD);
  }

  @Test
  public void testCloneApplication_DuplicateApplicationName() {
    String clonedAppName = "clonedAppName";
    tempEntity.newApplicationWithParent("appPublicId", clonedAppName);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> appCloneService.cloneApplication(sourceApp.getId(), clonedAppName, "clonedAppPublicId"))
        .withMessage("An application with name '" + clonedAppName + "' already exists.");
  }

  @Test
  public void testCloneApplication_DuplicateApplicationPublicId() {
    String clonedAppPublicId = "clonedAppPublicId";
    tempEntity.newApplicationWithParent(clonedAppPublicId, "aAppName");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName", clonedAppPublicId))
        .withMessage("An application with public ID '" + clonedAppPublicId + "' already exists.");
  }

  @Test
  public void testCloneApplication_Label_AppLabel() {
    Label sourceLabel = tempEntity.newLabel(sourceApp.getId());
    ComponentLabel sourceComponentLabel =
        tempEntity.newComponentLabel(sourceApp.getId(), sourceLabel.getId(), "testhash");

    ApiApplicationDTO clonedAppDTO =
        appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName", "clonedAppPublicId");

    List<Label> clonedLabels = labelDAO.getByOwnerId(clonedAppDTO.id);
    assertThat(clonedLabels).hasSize(1);
    Label clonedLabel = clonedLabels.get(0);
    assertThat(clonedLabel.getId()).isNotEqualTo(sourceLabel.getId());
    assertThat(clonedLabel.getLabel()).isEqualTo(sourceLabel.getLabel());
    assertThat(clonedLabel.getDescription()).isEqualTo(sourceLabel.getDescription());
    assertThat(clonedLabel.getColor()).isEqualTo(sourceLabel.getColor());

    List<ComponentLabel> clonedComponentLabels = componentLabelDAO.getByOwnerId(clonedAppDTO.id);
    assertThat(clonedComponentLabels).hasSize(1);
    ComponentLabel clonedComponentLabel = clonedComponentLabels.get(0);
    assertThat(clonedComponentLabel.getId()).isNotEqualTo(sourceComponentLabel.getId());
    assertThat(clonedComponentLabel.getLabelId()).isEqualTo(clonedLabel.getId());
    assertThat(clonedComponentLabel.getHash()).isEqualTo(sourceComponentLabel.getHash());

    // Assert the source objects were cloned, not moved.
    assertThat(labelDAO.getById(sourceLabel.getId())).isNotNull();
    assertThat(componentLabelDAO.getById(sourceComponentLabel.getId())).isNotNull();
  }

  @Test
  public void testCloneApplication_Label_OrgLabel() {
    Label label = tempEntity.newLabel(sourceApp.getOrganizationId());
    ComponentLabel sourceComponentLabel =
        tempEntity.newComponentLabel(sourceApp.getId(), label.getId(), "testhash");

    ApiApplicationDTO clonedAppDTO =
        appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName", "clonedAppPublicId");

    List<Label> clonedLabels = labelDAO.getByOwnerId(clonedAppDTO.id);
    assertThat(clonedLabels).isEmpty();

    List<ComponentLabel> clonedComponentLabels = componentLabelDAO.getByOwnerId(clonedAppDTO.id);
    assertThat(clonedComponentLabels).hasSize(1);
    ComponentLabel clonedComponentLabel = clonedComponentLabels.get(0);
    assertThat(clonedComponentLabel.getId()).isNotEqualTo(sourceComponentLabel.getId());
    assertThat(clonedComponentLabel.getLabelId()).isEqualTo(label.getId());
    assertThat(clonedComponentLabel.getHash()).isEqualTo(sourceComponentLabel.getHash());

    // Assert the source objects were cloned, not moved.
    assertThat(labelDAO.getById(label.getId())).isNotNull();
    assertThat(componentLabelDAO.getById(sourceComponentLabel.getId())).isNotNull();
  }

  @Test
  public void testCloneApplication_LicenseThreatGroup() {
    LicenseThreatGroup sourceLicenseThreatGroup = tempEntity.newLicenseThreatGroup(sourceApp.getId());
    LicenseThreatGroupLicense sourceLicenseThreatGroupLicense =
        tempEntity.newLicenseThreatGroupLicense(sourceApp.getId(), sourceLicenseThreatGroup.getId());

    ApiApplicationDTO clonedAppDTO =
        appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName", "clonedAppPublicId");

    List<LicenseThreatGroup> clonedLicenseThreatGroups = licenseThreatGroupDAO.getByOwnerId(clonedAppDTO.id);
    assertThat(clonedLicenseThreatGroups).hasSize(1);
    LicenseThreatGroup clonedLicenseThreatGroup = clonedLicenseThreatGroups.get(0);
    assertThat(clonedLicenseThreatGroup.getId()).isNotEqualTo(sourceLicenseThreatGroup.getId());
    assertThat(clonedLicenseThreatGroup.getName()).isEqualTo(sourceLicenseThreatGroup.getName());
    assertThat(clonedLicenseThreatGroup.getThreatLevel()).isEqualTo(sourceLicenseThreatGroup.getThreatLevel());

    List<LicenseThreatGroupLicense> clonedLicenseThreatGroupLicenses =
        licenseThreatGroupLicenseDAO.getByOwnerId(clonedAppDTO.id);
    assertThat(clonedLicenseThreatGroupLicenses).hasSize(1);
    LicenseThreatGroupLicense clonedLicenseThreatGroupLicense = clonedLicenseThreatGroupLicenses.get(0);
    assertThat(clonedLicenseThreatGroupLicense.getId()).isNotEqualTo(sourceLicenseThreatGroupLicense.getId());
    assertThat(clonedLicenseThreatGroupLicense.getLicenseThreatGroupId()).isEqualTo(clonedLicenseThreatGroup.getId());
    assertThat(clonedLicenseThreatGroupLicense.getLicenseId())
        .isEqualTo(sourceLicenseThreatGroupLicense.getLicenseId());

    // Assert the source objects were cloned, not moved.
    assertThat(licenseThreatGroupDAO.getById(sourceLicenseThreatGroup.getId())).isNotNull();
    assertThat(licenseThreatGroupLicenseDAO.getById(sourceLicenseThreatGroupLicense.getId())).isNotNull();
  }

  @Test
  public void testCloneApplication_LicenseOverride() {
    LicenseOverride sourceLicenseOverride = tempEntity.newLicenseOverride(sourceApp.getId(),
        ComponentIdentifier.createNpmCoordinates("packageId", "version"), LicenseOverrideStatus.OVERRIDDEN,
        "Apache-2.0");

    ApiApplicationDTO clonedAppDTO =
        appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName", "clonedAppPublicId");

    List<LicenseOverride> clonedLicenseOverrides = licenseOverrideDAO.getByOwnerId(clonedAppDTO.id);
    assertThat(clonedLicenseOverrides).hasSize(1);
    LicenseOverride clonedLicenseOverride = clonedLicenseOverrides.get(0);
    assertThat(clonedLicenseOverride.getId()).isNotEqualTo(sourceLicenseOverride.getId());
    assertThat(clonedLicenseOverride.getStatus()).isEqualTo(sourceLicenseOverride.getStatus());
    assertThat(clonedLicenseOverride.getComponentIdentifier())
        .isEqualTo(sourceLicenseOverride.getComponentIdentifier());
    assertThat(clonedLicenseOverride.getLicenseIds()).isEqualTo(sourceLicenseOverride.getLicenseIds());
    assertThat(clonedLicenseOverride.getComment()).isEqualTo(sourceLicenseOverride.getComment());

    // Assert the source objects were cloned, not moved.
    assertThat(licenseOverrideDAO.getById(sourceLicenseOverride.getId())).isNotNull();
  }

  @Test
  public void testCloneApplication_SecurityVulnerabilityOverride() {
    SecurityVulnerabilityOverride sourceSecurityVulnerabilityOverride =
        tempEntity.newSecurityVulnerabilityOverride(sourceApp.getId(), "hash", "source", "referenceId",
            SecurityVulnerabilityOverrideStatus.NOT_APPLICABLE, "comment");

    ApiApplicationDTO clonedAppDTO =
        appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName", "clonedAppPublicId");

    List<SecurityVulnerabilityOverride> clonedSecurityVulnerabilityOverrides =
        securityVulnerabilityOverrideDAO.getByOwnerId(clonedAppDTO.id);
    assertThat(clonedSecurityVulnerabilityOverrides).hasSize(1);
    SecurityVulnerabilityOverride clonedSecurityVulnerabilityOverride = clonedSecurityVulnerabilityOverrides.get(0);
    assertThat(clonedSecurityVulnerabilityOverride.getId()).isNotEqualTo(sourceSecurityVulnerabilityOverride.getId());
    assertThat(clonedSecurityVulnerabilityOverride.getStatus())
        .isEqualTo(sourceSecurityVulnerabilityOverride.getStatus());
    assertThat(clonedSecurityVulnerabilityOverride.getHash()).isEqualTo(sourceSecurityVulnerabilityOverride.getHash());
    assertThat(clonedSecurityVulnerabilityOverride.getSource())
        .isEqualTo(sourceSecurityVulnerabilityOverride.getSource());
    assertThat(clonedSecurityVulnerabilityOverride.getReferenceId())
        .isEqualTo(sourceSecurityVulnerabilityOverride.getReferenceId());
    assertThat(clonedSecurityVulnerabilityOverride.getComment())
        .isEqualTo(sourceSecurityVulnerabilityOverride.getComment());

    // Assert the source objects were cloned, not moved.
    assertThat(securityVulnerabilityOverrideDAO.getById(sourceSecurityVulnerabilityOverride.getId())).isNotNull();
  }

  @Test
  public void testCloneApplication_MembershipMapping() {
    MembershipMapping sourceMembershipMapping =
        tempEntity.newMembershipMapping(sourceApp.getId(), Role.DEVELOPER_ROLE_ID, "username");

    ApiApplicationDTO clonedAppDTO =
        appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName", "clonedAppPublicId");

    List<MembershipMapping> clonedMembershipMappings = membershipMappingDAO.getByContextId(clonedAppDTO.id);
    assertThat(clonedMembershipMappings).hasSize(1);
    MembershipMapping clonedMembershipMapping = clonedMembershipMappings.get(0);
    assertThat(clonedMembershipMapping.getId()).isNotEqualTo(sourceMembershipMapping.getId());
    assertThat(clonedMembershipMapping.getRoleId()).isEqualTo(sourceMembershipMapping.getRoleId());
    assertThat(clonedMembershipMapping.getMemberName()).isEqualTo(sourceMembershipMapping.getMemberName());
    assertThat(clonedMembershipMapping.getMemberType()).isEqualTo(sourceMembershipMapping.getMemberType());

    // Assert the source objects were cloned, not moved.
    assertThat(membershipMappingDAO.getById(sourceMembershipMapping.getId())).isNotNull();
  }

  @Test
  public void testCloneApplication_PolicyMonitoring() {
    PolicyMonitoring sourcePolicyMonitoring =
        tempEntity.newPolicyMonitoring(sourceApp.getId(), StageTypes.BUILD.getId());

    ApiApplicationDTO clonedAppDTO =
        appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName", "clonedAppPublicId");

    PolicyMonitoring clonedPolicyMonitoring = policyMonitoringDAO.getByOwnerIdAndStageTypeId(clonedAppDTO.id,
        StageTypes.BUILD.getId());
    assertThat(clonedPolicyMonitoring.getId()).isNotEqualTo(sourcePolicyMonitoring.getId());
    assertThat(clonedPolicyMonitoring.getStageTypeId()).isEqualTo(sourcePolicyMonitoring.getStageTypeId());

    // Assert the source objects were cloned, not moved.
    assertThat(policyMonitoringDAO.getById(sourcePolicyMonitoring.getId())).isNotNull();
  }

  @Test
  public void testCloneApplication_ApplicationTag() {
    Tag tag = tempEntity.newTag(sourceApp.getOrganizationId());
    ApplicationTag sourceAppTag = tempEntity.newApplicationTag(sourceApp.getId(), tag.getId());

    ApiApplicationDTO clonedAppDTO =
        appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName", "clonedAppPublicId");

    List<ApplicationTag> clonedAppTags = applicationTagDAO.getByApplicationId(clonedAppDTO.id);
    assertThat(clonedAppTags).hasSize(1);
    ApplicationTag clonedAppTag = clonedAppTags.get(0);
    assertThat(clonedAppTag.getId()).isNotEqualTo(sourceAppTag.getId());
    assertThat(clonedAppTag.getTagId()).isEqualTo(sourceAppTag.getTagId());

    // Assert the source objects were cloned, not moved.
    assertThat(applicationTagDAO.getById(sourceAppTag.getId())).isNotNull();
  }

  @Test
  public void testCloneApplication_ProprietaryConfig() {
    ProprietaryConfig sourceProprietaryConfig = tempEntity.newProprietaryConfig(sourceApp.getId(),
        Collections.singletonList("proprietarypackage"), Collections.singletonList("proprietaryregex"));

    ApiApplicationDTO clonedAppDTO =
        appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName", "clonedAppPublicId");

    ProprietaryConfig clonedProprietaryConfig = proprietaryConfigDAO.getByOwnerId(clonedAppDTO.id);
    assertThat(clonedProprietaryConfig.getId()).isNotEqualTo(sourceProprietaryConfig.getId());
    assertThat(clonedProprietaryConfig.getPackages()).isEqualTo(sourceProprietaryConfig.getPackages());
    assertThat(clonedProprietaryConfig.getRegexes()).isEqualTo(sourceProprietaryConfig.getRegexes());

    // Assert the source objects were cloned, not moved.
    assertThat(proprietaryConfigDAO.getById(sourceProprietaryConfig.getId())).isNotNull();
  }

  @Test
  public void testCloneApplication_SourceControl() {
    // The application cloning is supposed to disable pull requests for the cloned app.
    // So we set it to true in the source application in order to verify
    // that is not copied to the cloned application.
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB, true, true, "master");
    SourceControl sourceSourceControl =
        tempEntity.newSourceControl(sourceApp.getId(), "https://example.com/organization/project", null, "token",
            null, true /* enablePullRequests */, true /* enableStatusChecks */, "baseBranch", null);

    ApiApplicationDTO clonedAppDTO =
        appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName", "clonedAppPublicId");

    SourceControl clonedSourceControl = sourceControlDAO.getByOwnerId(clonedAppDTO.id);
    assertThat(clonedSourceControl.getId()).isNotEqualTo(sourceSourceControl.getId());
    assertThat(clonedSourceControl.getRepositoryUrl()).isEqualTo(sourceSourceControl.getRepositoryUrl());
    assertThat(clonedSourceControl.getUsername()).isEqualTo(sourceSourceControl.getUsername());
    assertThat(clonedSourceControl.getToken()).isEqualTo(sourceSourceControl.getToken());
    assertThat(clonedSourceControl.getProvider()).isEqualTo(sourceSourceControl.getProvider());
    assertThat(clonedSourceControl.getBaseBranch()).isEqualTo(sourceSourceControl.getBaseBranch());
    assertThat(clonedSourceControl.getRemediationPullRequestsEnabled()).isFalse();
    assertThat(clonedSourceControl.getStatusChecksEnabled()).isEqualTo(sourceSourceControl.getStatusChecksEnabled());

    // Assert the source objects were cloned, not moved.
    assertThat(sourceControlDAO.getById(sourceSourceControl.getId())).isNotNull();
  }

  @Test
  public void testCloneApplication_Policy() {
    Webhook webhook =
        tempEntity.newWebhook("http://example.com", Collections.singleton(WebhookEventType.APPLICATION_EVALUATION));
    Policy sourcePolicy = tempEntity.newPolicy(sourceApp.getId());
    sourcePolicy.setAction(BuildStageType.ID, Action.ID_FAIL);
    sourcePolicy.getNotifications().add(new UserNotification("jhondoe@example.com", BuildStageType.ID));
    Role role = roleDAO.getById(Role.DEVELOPER_ROLE_ID);
    sourcePolicy.getNotifications().add(new RoleNotification(role.getId(), role.getName(), BuildStageType.ID));
    sourcePolicy.getNotifications().add(new JiraNotification("projectKey", 1, BuildStageType.ID));
    sourcePolicy.getNotifications().add(new WebhookNotification(webhook.getId(), BuildStageType.ID));
    policyDAO.update(sourcePolicy);
    Constraint sourceConstraint = sourcePolicy.getConstraints().get(0);
    Condition sourceCondition = sourceConstraint.getConditions().get(0);

    ApiApplicationDTO clonedAppDTO =
        appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName", "clonedAppPublicId");

    List<Policy> clonedPolicies = policyDAO.getByOwnerId(clonedAppDTO.id);
    assertThat(clonedPolicies).hasSize(1);
    Policy clonedPolicy = clonedPolicies.get(0);
    assertThat(clonedPolicy.getId()).isNotEqualTo(sourcePolicy.getId());
    assertThat(clonedPolicy.getName()).isEqualTo(sourcePolicy.getName());
    assertThat(clonedPolicy.getThreatLevel()).isEqualTo(sourcePolicy.getThreatLevel());
    assertThat(clonedPolicy.isLegacyViolationAllowed())
        .isEqualTo(sourcePolicy.isLegacyViolationAllowed());
    assertThat(clonedPolicy.getActions()).isEqualTo(sourcePolicy.getActions());
    assertThat(clonedPolicy.getNotifications()).isEqualTo(sourcePolicy.getNotifications());
    Constraint clonedConstraint = clonedPolicy.getConstraints().get(0);
    assertThat(clonedConstraint.getId()).isNotEqualTo(sourceConstraint.getId());
    assertThat(clonedConstraint.getName()).isEqualTo(sourceConstraint.getName());
    assertThat(clonedConstraint.getOperator()).isEqualTo(sourceConstraint.getOperator());
    Condition clonedCondition = clonedConstraint.getConditions().get(0);
    assertThat(clonedCondition.getConditionTypeId()).isEqualTo(sourceCondition.getConditionTypeId());
    assertThat(clonedCondition.getOperator()).isEqualTo(sourceCondition.getOperator());
    assertThat(clonedCondition.getValue()).isEqualTo(sourceCondition.getValue());
    assertThat(clonedPolicy.getDroolsCode()).contains(clonedConstraint.getId());
    assertThat(clonedPolicy.getDroolsCode()).doesNotContain(sourceConstraint.getId());

    // Assert the source objects were cloned, not moved.
    assertThat(policyDAO.getById(sourcePolicy.getId())).isNotNull();
  }

  @Test
  public void testCloneApplication_Policy_WithLabelCondition_AppLabel() {
    testCloneApplication_Policy_WithLabelCondition(sourceApp.getId());
  }

  @Test
  public void testCloneApplication_Policy_WithLabelCondition_OrgLabel() {
    testCloneApplication_Policy_WithLabelCondition(sourceApp.getOrganizationId());
  }

  private void testCloneApplication_Policy_WithLabelCondition(String labelOwnerId) {
    Label sourceLabel = tempEntity.newLabel(labelOwnerId);
    Policy sourcePolicy = tempEntity.newPolicy(sourceApp, 5, LogicalOperator.AND,
        new Condition(LabelConditionType.ID, "is", sourceLabel.getId()));

    ApiApplicationDTO clonedAppDTO =
        appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName", "clonedAppPublicId");

    Policy clonedPolicy = policyDAO.getByOwnerId(clonedAppDTO.id).get(0);
    Condition clonedPolicyCondition = clonedPolicy.getConstraints().get(0).getConditions().get(0);
    if (labelOwnerId.equals(sourceApp.getId())) {
      // The label is owned by the cloned app, so the condition must refer to the cloned label.
      Label clonedLabel = labelDAO.getByOwnerId(clonedAppDTO.id).get(0);
      assertThat(clonedPolicyCondition.getValue()).isEqualTo(clonedLabel.getId());
    }
    else {
      // The label is owned by the parent org, so the condition must refer to the org label (i.e. cloned verbatim).
      assertThat(clonedPolicyCondition.getValue()).isEqualTo(sourceLabel.getId());
    }

    // Assert that the source policy condition has not changed.
    sourcePolicy = policyDAO.getById(sourcePolicy.getId());
    Condition sourcePolicyCondition = sourcePolicy.getConstraints().get(0).getConditions().get(0);
    assertThat(sourcePolicyCondition.getValue()).isEqualTo(sourceLabel.getId());
  }

  @Test
  public void testCloneApplication_Policy_WithLicenseThreatGroupCondition_AppLicenseThreatGroup() {
    testCloneApplication_Policy_WithLicenseThreatGroupCondition(sourceApp.getId());
  }

  @Test
  public void testCloneApplication_Policy_WithLicenseThreatGroupCondition_OrgLicenseThreatGroup() {
    testCloneApplication_Policy_WithLicenseThreatGroupCondition(sourceApp.getOrganizationId());
  }

  private void testCloneApplication_Policy_WithLicenseThreatGroupCondition(String licenseThreatGroupOwnerId) {
    LicenseThreatGroup sourceLicenseThreatGroup = tempEntity.newLicenseThreatGroup(licenseThreatGroupOwnerId);
    Policy sourcePolicy = tempEntity.newPolicy(sourceApp, 5, LogicalOperator.AND,
        new Condition(LicenseThreatGroupConditionType.ID, "is", sourceLicenseThreatGroup.getId()));

    ApiApplicationDTO clonedAppDTO =
        appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName", "clonedAppPublicId");

    Policy clonedPolicy = policyDAO.getByOwnerId(clonedAppDTO.id).get(0);
    Condition clonedPolicyCondition = clonedPolicy.getConstraints().get(0).getConditions().get(0);
    if (licenseThreatGroupOwnerId.equals(sourceApp.getId())) {
      // The LTG is owned by the cloned app, so the condition must refer to the cloned LTG.
      LicenseThreatGroup clonedLicenseThreatGroup = licenseThreatGroupDAO.getByOwnerId(clonedAppDTO.id).get(0);
      assertThat(clonedPolicyCondition.getValue()).isEqualTo(clonedLicenseThreatGroup.getId());
    }
    else {
      // The LTG is owned by the parent org, so the condition must refer to the org LTG (i.e. cloned verbatim).
      assertThat(clonedPolicyCondition.getValue()).isEqualTo(sourceLicenseThreatGroup.getId());
    }

    // Assert that the source policy condition has not changed.
    sourcePolicy = policyDAO.getById(sourcePolicy.getId());
    Condition sourcePolicyCondition = sourcePolicy.getConstraints().get(0).getConditions().get(0);
    assertThat(sourcePolicyCondition.getValue()).isEqualTo(sourceLicenseThreatGroup.getId());
  }

  @Test
  public void testCloneApplication_PolicyTag() {
    Tag tag = tempEntity.newTag(sourceApp.getOrganizationId());
    Policy sourcePolicy = tempEntity.newPolicy(sourceApp.getId());
    PolicyTag sourcePolicyTag = tempEntity.newPolicyTag(sourcePolicy.getId(), tag.getId());

    ApiApplicationDTO clonedAppDTO =
        appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName", "clonedAppPublicId");

    Policy clonedPolicy = policyDAO.getByOwnerIdAndName(clonedAppDTO.id, sourcePolicy.getName());
    List<PolicyTag> clonedPolicyTags = policyTagDAO.getByPolicyId(clonedPolicy.getId());
    assertThat(clonedPolicyTags).hasSize(1);
    PolicyTag clonedPolicyTag = clonedPolicyTags.get(0);
    assertThat(clonedPolicyTag.getId()).isNotEqualTo(sourcePolicyTag.getId());
    assertThat(clonedPolicyTag.getTagId()).isEqualTo(sourcePolicyTag.getTagId());

    // Assert the source objects were cloned, not moved.
    assertThat(policyTagDAO.getById(sourcePolicyTag.getId())).isNotNull();
  }

  @Test
  public void testCloneApplication_PolicyWaiver_WithExpiry() {
    DateTime now = DateTime.now();
    Policy sourcePolicy = tempEntity.newPolicy(sourceApp.getId());
    PolicyWaiver sourcePolicyWaiver = tempEntity.newWaiver("hash1", sourcePolicy.getId(), sourceApp.getId(), "comment");
    PolicyWaiver expiringPolicyWaiver = tempEntity.newWaiver("hash2", sourcePolicy.getId(), sourceApp.getId(),
        null, "comment", now.toDate(), now.plusHours(1).toDate());
    PolicyWaiver expiredPolicyWaiver = tempEntity.newWaiver("hash3", sourcePolicy.getId(), sourceApp.getId(),
        null, "comment", now.toDate(), now.toDate());

    ApiApplicationDTO clonedAppDTO =
        appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName", "clonedAppPublicId");

    Policy clonedPolicy = policyDAO.getByOwnerIdAndName(clonedAppDTO.id, sourcePolicy.getName());
    List<PolicyWaiver> clonedPolicyWaivers = policyWaiverDAO.getByOwnerId(clonedAppDTO.id);
    assertThat(clonedPolicyWaivers).hasSize(3);
    PolicyWaiver clonedPolicyWaiver = clonedPolicyWaivers.get(0);
    assertThat(clonedPolicyWaiver.getId()).isNotEqualTo(sourcePolicyWaiver.getId());
    assertThat(clonedPolicyWaiver.getPolicyId()).isEqualTo(clonedPolicy.getId());
    assertThat(clonedPolicyWaiver.getHash()).isEqualTo(sourcePolicyWaiver.getHash());
    assertThat(clonedPolicyWaiver.getComment()).isEqualTo(sourcePolicyWaiver.getComment());
    assertThat(clonedPolicyWaiver.getCreateTime()).isEqualTo(sourcePolicyWaiver.getCreateTime());
    assertThat(clonedPolicyWaiver.getExpiryTime()).isEqualTo(sourcePolicyWaiver.getExpiryTime());
    assertThat(clonedPolicyWaiver.getConstraintFacts()).isNull();
    clonedPolicyWaiver = clonedPolicyWaivers.get(1);
    assertThat(clonedPolicyWaiver.getId()).isNotEqualTo(expiringPolicyWaiver.getId());
    assertThat(clonedPolicyWaiver.getPolicyId()).isEqualTo(clonedPolicy.getId());
    assertThat(clonedPolicyWaiver.getHash()).isEqualTo(expiringPolicyWaiver.getHash());
    assertThat(clonedPolicyWaiver.getComment()).isEqualTo(expiringPolicyWaiver.getComment());
    assertThat(clonedPolicyWaiver.getCreateTime()).isEqualTo(expiringPolicyWaiver.getCreateTime());
    assertThat(clonedPolicyWaiver.getExpiryTime()).isEqualTo(expiringPolicyWaiver.getExpiryTime());
    assertThat(clonedPolicyWaiver.getConstraintFacts()).isNull();
    clonedPolicyWaiver = clonedPolicyWaivers.get(2);
    assertThat(clonedPolicyWaiver.getId()).isNotEqualTo(expiredPolicyWaiver.getId());
    assertThat(clonedPolicyWaiver.getPolicyId()).isEqualTo(clonedPolicy.getId());
    assertThat(clonedPolicyWaiver.getHash()).isEqualTo(expiredPolicyWaiver.getHash());
    assertThat(clonedPolicyWaiver.getComment()).isEqualTo(expiredPolicyWaiver.getComment());
    assertThat(clonedPolicyWaiver.getCreateTime()).isEqualTo(expiredPolicyWaiver.getCreateTime());
    assertThat(clonedPolicyWaiver.getExpiryTime()).isEqualTo(expiredPolicyWaiver.getExpiryTime());
    assertThat(clonedPolicyWaiver.getConstraintFacts()).isNull();

    // Assert the source objects were cloned, not moved.
    assertThat(policyWaiverDAO.getById(sourcePolicyWaiver.getId())).isNotNull();
  }

  @Test
  public void testCloneApplication_PolicyWaiver_WithoutConstraintFacts() {
    Policy sourcePolicy = tempEntity.newPolicy(sourceApp.getId());
    PolicyWaiver sourcePolicyWaiver = tempEntity.newWaiver("hash", sourcePolicy.getId(), sourceApp.getId(), "comment");

    ApiApplicationDTO clonedAppDTO =
        appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName", "clonedAppPublicId");

    Policy clonedPolicy = policyDAO.getByOwnerIdAndName(clonedAppDTO.id, sourcePolicy.getName());
    List<PolicyWaiver> clonedPolicyWaivers = policyWaiverDAO.getByOwnerId(clonedAppDTO.id);
    assertThat(clonedPolicyWaivers).hasSize(1);
    PolicyWaiver clonedPolicyWaiver = clonedPolicyWaivers.get(0);
    assertThat(clonedPolicyWaiver.getId()).isNotEqualTo(sourcePolicyWaiver.getId());
    assertThat(clonedPolicyWaiver.getPolicyId()).isEqualTo(clonedPolicy.getId());
    assertThat(clonedPolicyWaiver.getHash()).isEqualTo(sourcePolicyWaiver.getHash());
    assertThat(clonedPolicyWaiver.getComment()).isEqualTo(sourcePolicyWaiver.getComment());
    assertThat(clonedPolicyWaiver.getCreateTime()).isEqualTo(sourcePolicyWaiver.getCreateTime());
    assertThat(clonedPolicyWaiver.getConstraintFacts()).isNull();

    // Assert the source objects were cloned, not moved.
    assertThat(policyWaiverDAO.getById(sourcePolicyWaiver.getId())).isNotNull();
  }

  @Test
  public void testCloneApplication_PolicyWaiver_WithConstraintFact_WithoutConditionFacts_AppPolicy() {
    testCloneApplication_PolicyWaiver_WithConstraintFact_WithoutConditionFacts(sourceApp.getId());
  }

  @Test
  public void testCloneApplication_PolicyWaiver_WithConstraintFact_WithoutConditionFacts_OrgPolicy() {
    testCloneApplication_PolicyWaiver_WithConstraintFact_WithoutConditionFacts(sourceApp.getOrganizationId());
  }

  private void testCloneApplication_PolicyWaiver_WithConstraintFact_WithoutConditionFacts(String policyOwnerId) {
    Policy sourcePolicy = tempEntity.newPolicy(policyOwnerId);
    Constraint sourceConstraint = sourcePolicy.getConstraints().get(0);
    ConstraintFact sourceConstraintFact =
        new ConstraintFact(sourceConstraint.getId(), sourceConstraint.getName(), sourceConstraint.getOperator().name());
    PolicyWaiver sourcePolicyWaiver = tempEntity.newWaiver("hash", sourcePolicy.getId(), sourceApp.getId(),
        Collections.singletonList(sourceConstraintFact), "comment");

    ApiApplicationDTO clonedAppDTO =
        appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName", "clonedAppPublicId");

    List<PolicyWaiver> clonedPolicyWaivers = policyWaiverDAO.getByOwnerId(clonedAppDTO.id);
    assertThat(clonedPolicyWaivers).hasSize(1);
    PolicyWaiver clonedPolicyWaiver = clonedPolicyWaivers.get(0);
    ConstraintFact clonedConstraintFact = clonedPolicyWaiver.getConstraintFacts().get(0);
    if (policyOwnerId.equals(sourceApp.getId())) {
      // The policy is owned by the cloned app, so the constraint fact must refer to the cloned policy.
      Policy clonedPolicy = policyDAO.getByOwnerIdAndName(clonedAppDTO.id, sourcePolicy.getName());
      Constraint clonedConstraint = clonedPolicy.getConstraints().get(0);
      assertThat(clonedConstraintFact.getConstraintId()).isEqualTo(clonedConstraint.getId());
    }
    else {
      // The policy is owned by the parent org, so the constraint fact must refer to the org policy (i.e. cloned
      // verbatim).
      assertThat(clonedConstraintFact.getConstraintId()).isEqualTo(sourceConstraint.getId());
    }
    assertThat(clonedConstraintFact.getConstraintName()).isEqualTo(sourceConstraintFact.getConstraintName());
    assertThat(clonedConstraintFact.getOperatorName()).isEqualTo(sourceConstraintFact.getOperatorName());
    assertThat(clonedConstraintFact.getConditionFacts()).isEmpty();

    // Assert the source objects were cloned, not moved.
    assertThat(policyWaiverDAO.getById(sourcePolicyWaiver.getId())).isNotNull();
  }

  @Test
  public void testCloneApplication_PolicyWaiver_WithConstraintFact_LabelConditionType_AppLabel() {
    testCloneApplication_PolicyWaiver_WithConstraintFact_LabelConditionType(sourceApp.getId());
  }

  @Test
  public void testCloneApplication_PolicyWaiver_WithConstraintFact_LabelConditionType_OrgLabel() {
    testCloneApplication_PolicyWaiver_WithConstraintFact_LabelConditionType(sourceApp.getOrganizationId());
  }

  private void testCloneApplication_PolicyWaiver_WithConstraintFact_LabelConditionType(String labelOwnerId) {
    Label sourceLabel = tempEntity.newLabel(labelOwnerId);
    Condition sourceCondition = new Condition(LabelConditionType.ID, "is", sourceLabel.getId());
    Policy sourcePolicy = tempEntity.newPolicy(sourceApp, 5, LogicalOperator.AND, sourceCondition);

    Constraint sourceConstraint = sourcePolicy.getConstraints().get(0);
    ConstraintFact sourceConstraintFact =
        new ConstraintFact(sourceConstraint.getId(), sourceConstraint.getName(), sourceConstraint.getOperator().name());
    ConditionFact sourceConditionFact = createConditionFact(sourceCondition, new TriggerLabel(sourceLabel.getId()));
    sourceConstraintFact.addConditionFact(sourceConditionFact);
    PolicyWaiver sourcePolicyWaiver = tempEntity.newWaiver("hash", sourcePolicy.getId(), sourceApp.getId(),
        Collections.singletonList(sourceConstraintFact), "comment");

    ApiApplicationDTO clonedAppDTO =
        appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName", "clonedAppPublicId");

    Policy clonedPolicy = policyDAO.getByOwnerIdAndName(clonedAppDTO.id, sourcePolicy.getName());
    List<PolicyWaiver> clonedPolicyWaivers = policyWaiverDAO.getByOwnerId(clonedAppDTO.id);
    assertThat(clonedPolicyWaivers).hasSize(1);
    PolicyWaiver clonedPolicyWaiver = clonedPolicyWaivers.get(0);
    assertThat(clonedPolicy.getConstraints()).hasSize(1);
    ConstraintFact clonedConstraintFact = clonedPolicyWaiver.getConstraintFacts().get(0);
    assertThat(clonedConstraintFact.getConditionFacts()).hasSize(1);
    ConditionFact clonedConditionFact = clonedConstraintFact.getConditionFacts().get(0);
    assertThat(clonedConditionFact.getConditionIndex()).isEqualTo(sourceCondition.getConditionIndex());
    assertThat(clonedConditionFact.getConditionTypeId()).isEqualTo(sourceCondition.getConditionTypeId());
    assertThat(clonedConditionFact.getReason()).isEqualTo(sourceConditionFact.getReason());
    assertThat(clonedConditionFact.getSummary()).isEqualTo(sourceConditionFact.getSummary());
    if (labelOwnerId.equals(sourceApp.getId())) {
      // The label is owned by the cloned app, so the condition fact must refer to the cloned label.
      Label clonedLabel = labelDAO.getByOwnerId(clonedAppDTO.id).get(0);
      assertThat(clonedConditionFact.getTriggerJson())
          .isEqualTo(sourceConditionFact.getTriggerJson().replace(sourceLabel.getId(), clonedLabel.getId()));
    }
    else {
      // The label is owned by the parent org, so the condition fact must refer to the org label (i.e. cloned verbatim).
      assertThat(clonedConditionFact.getTriggerJson()).isEqualTo(sourceConditionFact.getTriggerJson());
    }

    // Assert the source objects were cloned, not moved.
    assertThat(policyWaiverDAO.getById(sourcePolicyWaiver.getId())).isNotNull();
  }

  @Test
  public void testCloneApplication_PolicyWaiver_WithConstraintFact_LicenseThreatGroupConditionType_AppLTG() {
    testCloneApplication_PolicyWaiver_WithConstraintFact_LicenseThreatGroupConditionType(sourceApp.getId());
  }

  @Test
  public void testCloneApplication_PolicyWaiver_WithConstraintFact_LicenseThreatGroupConditionType_OrgLTG() {
    testCloneApplication_PolicyWaiver_WithConstraintFact_LicenseThreatGroupConditionType(sourceApp.getOrganizationId());
  }

  private void testCloneApplication_PolicyWaiver_WithConstraintFact_LicenseThreatGroupConditionType(
      String licenseThreatGroupOwnerId)
  {
    LicenseThreatGroup sourceLicenseThreatGroup = tempEntity.newLicenseThreatGroup(licenseThreatGroupOwnerId);
    Condition sourceCondition =
        new Condition(LicenseThreatGroupConditionType.ID, "is", sourceLicenseThreatGroup.getId());
    Policy sourcePolicy = tempEntity.newPolicy(sourceApp, 5, LogicalOperator.AND, sourceCondition);

    Constraint sourceConstraint = sourcePolicy.getConstraints().get(0);
    ConstraintFact sourceConstraintFact =
        new ConstraintFact(sourceConstraint.getId(), sourceConstraint.getName(), sourceConstraint.getOperator().name());
    ConditionFact sourceConditionFact =
        createConditionFact(sourceCondition, new TriggerLicenseThreatGroup(sourceLicenseThreatGroup.getId()));
    sourceConstraintFact.addConditionFact(sourceConditionFact);
    PolicyWaiver sourcePolicyWaiver = tempEntity.newWaiver("hash", sourcePolicy.getId(), sourceApp.getId(),
        Collections.singletonList(sourceConstraintFact), "comment");

    ApiApplicationDTO clonedAppDTO =
        appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName", "clonedAppPublicId");

    Policy clonedPolicy = policyDAO.getByOwnerIdAndName(clonedAppDTO.id, sourcePolicy.getName());
    List<PolicyWaiver> clonedPolicyWaivers = policyWaiverDAO.getByOwnerId(clonedAppDTO.id);
    assertThat(clonedPolicyWaivers).hasSize(1);
    PolicyWaiver clonedPolicyWaiver = clonedPolicyWaivers.get(0);
    assertThat(clonedPolicy.getConstraints()).hasSize(1);
    ConstraintFact clonedConstraintFact = clonedPolicyWaiver.getConstraintFacts().get(0);
    assertThat(clonedConstraintFact.getConditionFacts()).hasSize(1);
    ConditionFact clonedConditionFact = clonedConstraintFact.getConditionFacts().get(0);
    assertThat(clonedConditionFact.getConditionIndex()).isEqualTo(sourceCondition.getConditionIndex());
    assertThat(clonedConditionFact.getConditionTypeId()).isEqualTo(sourceCondition.getConditionTypeId());
    assertThat(clonedConditionFact.getReason()).isEqualTo(sourceConditionFact.getReason());
    assertThat(clonedConditionFact.getSummary()).isEqualTo(sourceConditionFact.getSummary());
    if (licenseThreatGroupOwnerId.equals(sourceApp.getId())) {
      // The LTG is owned by the cloned app, so the condition fact must refer to the cloned LTG.
      LicenseThreatGroup clonedLicenseThreatGroup = licenseThreatGroupDAO.getByOwnerId(clonedAppDTO.id).get(0);
      assertThat(clonedConditionFact.getTriggerJson()).isEqualTo(sourceConditionFact.getTriggerJson()
          .replace(sourceLicenseThreatGroup.getId(), clonedLicenseThreatGroup.getId()));
    }
    else {
      // The LTG is owned by the parent org, so the condition fact must refer to the org LTG (i.e. cloned verbatim).
      assertThat(clonedConditionFact.getTriggerJson()).isEqualTo(sourceConditionFact.getTriggerJson());
    }

    // Assert the source objects were cloned, not moved.
    assertThat(policyWaiverDAO.getById(sourcePolicyWaiver.getId())).isNotNull();
  }

  @Test
  public void testCloneApplication_PolicyWaiver_WithConstraintFact_LicenseThreatGroupLevelConditionType_AppLTG() {
    testCloneApplication_PolicyWaiver_WithConstraintFact_LicenseThreatGroupLevelConditionType(sourceApp.getId());
  }

  @Test
  public void testCloneApplication_PolicyWaiver_WithConstraintFact_LicenseThreatGroupLevelConditionType_OrgLTG() {
    testCloneApplication_PolicyWaiver_WithConstraintFact_LicenseThreatGroupLevelConditionType(
        sourceApp.getOrganizationId());
  }

  private void testCloneApplication_PolicyWaiver_WithConstraintFact_LicenseThreatGroupLevelConditionType(
      String licenseThreatGroupOwnerId)
  {
    LicenseThreatGroup sourceLicenseThreatGroup = tempEntity.newLicenseThreatGroup(licenseThreatGroupOwnerId);
    Condition sourceCondition = new Condition(LicenseThreatGroupLevelConditionType.ID, "<=", "9");
    Policy sourcePolicy = tempEntity.newPolicy(sourceApp, 5, LogicalOperator.AND, sourceCondition);

    Constraint sourceConstraint = sourcePolicy.getConstraints().get(0);
    ConstraintFact sourceConstraintFact =
        new ConstraintFact(sourceConstraint.getId(), sourceConstraint.getName(), sourceConstraint.getOperator().name());
    ConditionFact sourceConditionFact =
        createConditionFact(sourceCondition, new TriggerLicenseThreatGroupWithThreatLevel(sourceLicenseThreatGroup));
    sourceConstraintFact.addConditionFact(sourceConditionFact);
    PolicyWaiver sourcePolicyWaiver = tempEntity.newWaiver("hash", sourcePolicy.getId(), sourceApp.getId(),
        Collections.singletonList(sourceConstraintFact), "comment");

    ApiApplicationDTO clonedAppDTO =
        appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName", "clonedAppPublicId");

    Policy clonedPolicy = policyDAO.getByOwnerIdAndName(clonedAppDTO.id, sourcePolicy.getName());
    List<PolicyWaiver> clonedPolicyWaivers = policyWaiverDAO.getByOwnerId(clonedAppDTO.id);
    assertThat(clonedPolicyWaivers).hasSize(1);
    PolicyWaiver clonedPolicyWaiver = clonedPolicyWaivers.get(0);
    assertThat(clonedPolicy.getConstraints()).hasSize(1);
    ConstraintFact clonedConstraintFact = clonedPolicyWaiver.getConstraintFacts().get(0);
    assertThat(clonedConstraintFact.getConditionFacts()).hasSize(1);
    ConditionFact clonedConditionFact = clonedConstraintFact.getConditionFacts().get(0);
    assertThat(clonedConditionFact.getConditionIndex()).isEqualTo(sourceCondition.getConditionIndex());
    assertThat(clonedConditionFact.getConditionTypeId()).isEqualTo(sourceCondition.getConditionTypeId());
    assertThat(clonedConditionFact.getReason()).isEqualTo(sourceConditionFact.getReason());
    assertThat(clonedConditionFact.getSummary()).isEqualTo(sourceConditionFact.getSummary());
    if (licenseThreatGroupOwnerId.equals(sourceApp.getId())) {
      // The LTG is owned by the cloned app, so the condition fact must refer to the cloned LTG.
      LicenseThreatGroup clonedLicenseThreatGroup = licenseThreatGroupDAO.getByOwnerId(clonedAppDTO.id).get(0);
      assertThat(clonedConditionFact.getTriggerJson()).isEqualTo(sourceConditionFact.getTriggerJson()
          .replace(sourceLicenseThreatGroup.getId(), clonedLicenseThreatGroup.getId()));
    }
    else {
      // The LTG is owned by the parent org, so the condition fact must refer to the org LTG (i.e. cloned verbatim).
      assertThat(clonedConditionFact.getTriggerJson()).isEqualTo(sourceConditionFact.getTriggerJson());
    }

    // Assert the source objects were cloned, not moved.
    assertThat(policyWaiverDAO.getById(sourcePolicyWaiver.getId())).isNotNull();
  }

  @Test
  public void testCloneApplication_PolicyWaiver_WithTwoConstraintFacts() {
    Condition sourceCondition1 = new Condition(AgeInDaysConditionType.ID, "older than", "1");
    Constraint sourceConstraint1 = new Constraint(null /* id */, "Test Constraint 1", LogicalOperator.AND);
    sourceConstraint1.addCondition(sourceCondition1);
    Condition sourceCondition2 = new Condition(AgeInDaysConditionType.ID, "younger than", "10");
    Constraint sourceConstraint2 = new Constraint(null /* id */, "Test Constraint 2", LogicalOperator.AND);
    sourceConstraint2.addCondition(sourceCondition2);
    Policy sourcePolicy = new Policy(null /* id */, "Test Policy");
    sourcePolicy.setOwnerId(sourceApp.getId());
    sourcePolicy.setThreatLevel(5);
    sourcePolicy.addConstraint(sourceConstraint1);
    sourcePolicy.addConstraint(sourceConstraint2);
    sourcePolicy = tempEntity.newPolicy(sourcePolicy);

    ConstraintFact sourceConstraintFact1 = new ConstraintFact(sourceConstraint1.getId(), sourceConstraint1.getName(),
        sourceConstraint1.getOperator().name());
    ConditionFact sourceConditionFact1 = createConditionFact(sourceCondition1, null);
    sourceConstraintFact1.addConditionFact(sourceConditionFact1);
    ConstraintFact sourceConstraintFact2 = new ConstraintFact(sourceConstraint2.getId(), sourceConstraint2.getName(),
        sourceConstraint2.getOperator().name());
    ConditionFact sourceConditionFact2 = createConditionFact(sourceCondition2, null);
    sourceConstraintFact2.addConditionFact(sourceConditionFact2);
    PolicyWaiver sourcePolicyWaiver = tempEntity.newWaiver("hash", sourcePolicy.getId(), sourceApp.getId(),
        Arrays.asList(sourceConstraintFact1, sourceConstraintFact2), "comment");

    ApiApplicationDTO clonedAppDTO =
        appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName", "clonedAppPublicId");

    Policy clonedPolicy = policyDAO.getByOwnerIdAndName(clonedAppDTO.id, sourcePolicy.getName());
    List<PolicyWaiver> clonedPolicyWaivers = policyWaiverDAO.getByOwnerId(clonedAppDTO.id);
    assertThat(clonedPolicyWaivers).hasSize(1);
    PolicyWaiver clonedPolicyWaiver = clonedPolicyWaivers.get(0);
    assertThat(clonedPolicy.getConstraints()).hasSize(2);

    ConstraintFact clonedConstraintFact1 = clonedPolicyWaiver.getConstraintFacts().get(0);
    assertThat(clonedConstraintFact1.getConditionFacts()).hasSize(1);
    ConditionFact clonedConditionFact1 = clonedConstraintFact1.getConditionFacts().get(0);
    assertThat(clonedConditionFact1.getConditionIndex()).isEqualTo(sourceCondition1.getConditionIndex());
    assertThat(clonedConditionFact1.getConditionTypeId()).isEqualTo(sourceCondition1.getConditionTypeId());
    assertThat(clonedConditionFact1.getReason()).isEqualTo(sourceConditionFact1.getReason());
    assertThat(clonedConditionFact1.getSummary()).isEqualTo(sourceConditionFact1.getSummary());

    ConstraintFact clonedConstraintFact2 = clonedPolicyWaiver.getConstraintFacts().get(1);
    assertThat(clonedConstraintFact2.getConditionFacts()).hasSize(1);
    ConditionFact clonedConditionFact2 = clonedConstraintFact2.getConditionFacts().get(0);
    assertThat(clonedConditionFact2.getConditionIndex()).isEqualTo(sourceCondition2.getConditionIndex());
    assertThat(clonedConditionFact2.getConditionTypeId()).isEqualTo(sourceCondition2.getConditionTypeId());
    assertThat(clonedConditionFact2.getReason()).isEqualTo(sourceConditionFact2.getReason());
    assertThat(clonedConditionFact2.getSummary()).isEqualTo(sourceConditionFact2.getSummary());

    // Assert the source objects were cloned, not moved.
    assertThat(policyWaiverDAO.getById(sourcePolicyWaiver.getId())).isNotNull();
  }

  private ConditionFact createConditionFact(Condition condition, Object conditionTrigger) {
    ConditionFact conditionFact =
        new ConditionFact(condition.getConditionTypeId(), condition.getConditionIndex(), "summary", "reason");
    conditionFact.setTriggerJson(JsonUtils.writeUnformatted(conditionTrigger));
    return conditionFact;
  }

  @Test
  public void testCloneApplication_PolicyWaiver_WithObsoleteConstraintFact() {
    Policy sourcePolicy = tempEntity.newPolicy(sourceApp.getId());
    PolicyWaiver sourcePolicyWaiver = tempEntity.newWaiver("hash", sourcePolicy.getId(), sourceApp.getId(),
        Collections.singletonList(new ConstraintFact("invalidConstraintId", "constraintName", "operatorName")),
        "comment");

    ApiApplicationDTO clonedAppDTO =
        appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName", "clonedAppPublicId");

    List<PolicyWaiver> clonedPolicyWaivers = policyWaiverDAO.getByOwnerId(clonedAppDTO.id);
    assertThat(clonedPolicyWaivers).isEmpty();

    // Assert the source objects were cloned, not moved.
    assertThat(policyWaiverDAO.getById(sourcePolicyWaiver.getId())).isNotNull();
  }

  @Test
  public void testCloneApplication_ApplicationLicenseLimit() {
    testProductLicense.setMaxApplications(2);

    appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName1", "clonedAppPublicId1");
    assertThatExceptionOfType(PaymentRequiredException.class)
        .isThrownBy(() -> appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName2", "clonedAppPublicId2"));

    assertThat(applicationDAO.getByPublicId("clonedAppPublicId1")).isNotNull();
    assertThat(applicationDAO.getByPublicId("clonedAppPublicId2")).isNull();
  }
}
